package com.mushokumagic.weather.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mushokumagic.weather.config.WeatherConfig;
import com.mushokumagic.weather.MushokuWeather;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Ray-marched, GPU-rendered local storm volumes. Clouds are density fields in a
 * fragment shader, not stacks of cloud particles; the server sends only compact
 * storm state and this class builds the visible atmosphere on each client.
 */
public final class VolumetricCloudRenderer {
    private static final String VERTEX_RESOURCE = "/assets/mushoku_weather/shaders/volume_cloud.vert";
    private static final String FRAGMENT_RESOURCE = "/assets/mushoku_weather/shaders/volume_cloud.frag";
    private static final String CLOUD_NOISE_RESOURCE = "/assets/mushoku_weather/textures/cloud_noise.png";
    private static final int CLOUD_TEXTURE_UNIT = GL13.GL_TEXTURE3;
    private static final int MAX_RENDERED_VOLUMES = 6;
    private static final int MAX_SNAPSHOT_DIMENSIONS = 8;
    private static final double LOCAL_FADE_TICKS = 100.0;
    private static final double MAX_EXTRAPOLATION_TICKS = 40.0;
    private static final Map<String, SnapshotSet> SNAPSHOTS = new HashMap<>();
    private static final float[] UNIT_CUBE = createUnitCube();
    private static int program;
    private static int vertexArray;
    private static int vertexBuffer;
    private static int cloudNoiseTexture;
    private static int cloudNoiseLocation;
    private static int stepsLocation;
    private static int centerLocation;
    private static int halfSizeLocation;
    private static int windLocation;
    private static int kindLocation;
    private static int intensityLocation;
    private static int phaseLocation;
    private static int timeLocation;
    private static int daylightLocation;
    private static int twilightLocation;
    private static int lightningLocation;
    private static int viewProjectionLocation;
    private static boolean disabledAfterFailure;
    private static boolean configDisableReported;
    private static boolean missingSnapshotReported;
    private static boolean emptyVolumesReported;
    private static boolean activeRenderReported;

    private VolumetricCloudRenderer() {
    }

    public static void updateSnapshots(
            String dimension,
            List<LocalStormManager.StormSnapshot> localStorms,
            List<SevereWeatherManager.StormSnapshot> severeStorms,
            RegionalWeatherManager.RegionalSnapshot regional) {
        if (dimension == null || dimension.isBlank()) {
            return;
        }
        boolean isEmpty = (localStorms == null || localStorms.isEmpty())
                && (severeStorms == null || severeStorms.isEmpty())
                && (regional == null || regional.cloudCover() < 0.18);
        if (isEmpty) {
            SNAPSHOTS.remove(dimension);
        } else {
            SNAPSHOTS.put(dimension, new SnapshotSet(
                    localStorms == null ? List.of() : List.copyOf(localStorms),
                    severeStorms == null ? List.of() : List.copyOf(severeStorms),
                    regional));
        }
        while (SNAPSHOTS.size() > MAX_SNAPSHOT_DIMENSIONS) {
            SNAPSHOTS.remove(SNAPSHOTS.keySet().iterator().next());
        }
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }

    /** Called from the loader's world-render stage with the active projection matrix. */
    public static void render(
            String dimension,
            double cameraX,
            double cameraY,
            double cameraZ,
            long worldTime,
            long dayTime,
            float partialTick,
            Matrix4f projectionMatrix) {
        WeatherConfig config = WeatherConfig.get();
        if (dimension == null || disabledAfterFailure) {
            return;
        }
        if (!config.volumetricCloudsEnabled) {
            if (!configDisableReported) {
                MushokuWeather.LOGGER.warn(
                        "Volumetric weather clouds are disabled by config/mushoku_weather.json (volumetricCloudsEnabled=false).");
                configDisableReported = true;
            }
            return;
        }
        SnapshotSet snapshots = SNAPSHOTS.get(dimension);
        if (snapshots == null) {
            if (!missingSnapshotReported) {
                MushokuWeather.LOGGER.warn(
                        "No synchronized local weather snapshot is available for {}; volumetric clouds cannot be rendered yet.",
                        dimension);
                missingSnapshotReported = true;
            }
            return;
        }

        double partial = Math.max(0.0, Math.min(1.0, (double)partialTick));
        double frameTime = worldTime + partial;
        double daylightTime = dayTime + partial;
        List<CloudVolume> volumes = VolumetricCloudRenderer.collectVolumes(
                snapshots,
                cameraX,
                cameraY,
                cameraZ,
                frameTime,
                Math.max(128, Math.min(2048, config.cloudRenderDistance)));
        if (volumes.isEmpty()) {
            if (!emptyVolumesReported) {
                MushokuWeather.LOGGER.warn(
                        "The synchronized weather snapshot for {} contains no renderable cloud volume; regional cloud cover is {}%.",
                        dimension,
                        snapshots.regional == null ? "unknown" : Math.round(snapshots.regional.cloudCover() * 100.0));
                emptyVolumesReported = true;
            }
            return;
        }
        volumes.sort(Comparator.comparingDouble((CloudVolume volume) -> volume.distanceSquared));
        int quality = Math.max(1, Math.min(3, config.volumetricCloudQuality));
        int volumeBudget = Math.min(MAX_RENDERED_VOLUMES, quality * 2);
        if (volumes.size() > volumeBudget) {
            volumes = new ArrayList<>(volumes.subList(0, volumeBudget));
        }
        volumes.sort(Comparator.comparingDouble((CloudVolume volume) -> volume.distanceSquared).reversed());

        try {
            if (!VolumetricCloudRenderer.ensureProgram()) {
                return;
            }
            VolumetricCloudRenderer.draw(
                    volumes, cameraX, cameraY, cameraZ, frameTime, daylightTime, quality, projectionMatrix);
            if (!activeRenderReported) {
                MushokuWeather.LOGGER.info(
                        "Volumetric weather rendering is active in {} with {} cloud volume(s) at quality {}.",
                        dimension,
                        volumes.size(),
                        quality);
                activeRenderReported = true;
            }
        } catch (Throwable throwable) {
            disabledAfterFailure = true;
            MushokuWeather.LOGGER.error("Volumetric weather rendering failed; keeping the rest of the weather effects enabled", throwable);
        }
    }

    private static List<CloudVolume> collectVolumes(
            SnapshotSet snapshots,
            double cameraX,
            double cameraY,
            double cameraZ,
            double now,
            double renderDistance) {
        ArrayList<CloudVolume> volumes = new ArrayList<>();
        for (SevereWeatherManager.StormSnapshot storm : snapshots.severeStorms) {
            double age = now - storm.startTick();
            double duration = storm.endTick() - storm.startTick();
            if (duration <= 0.0 || age < 0.0 || age >= duration) {
                continue;
            }
            double fade = SevereWeatherModel.lifecycleStrength((long)age, (long)duration)
                    * SevereWeatherModel.strengtheningMultiplier((long)age, (long)duration, storm.strengtheningBoost());
            double intensity = clamp(storm.baseIntensity() * fade, 0.0, 1.35);
            if (intensity <= 0.025) {
                continue;
            }
            double extrapolation = clamp(now - storm.snapshotTick(), 0.0, MAX_EXTRAPOLATION_TICKS);
            double x = storm.x() + storm.travelX() * storm.travelSpeed() * extrapolation;
            double z = storm.z() + storm.travelZ() * storm.travelSpeed() * extrapolation;
            int renderKind = VolumetricCloudRenderer.shaderKind(storm.kind());
            double halfX = storm.radius() * 1.16;
            double halfZ = storm.radius() * 1.16;
            double halfY;
            double centerY;
            if (storm.kind() == SevereWeatherModel.Kind.TORNADO) {
                halfX = Math.max(16.0, storm.radius() * 0.72);
                halfZ = Math.max(16.0, storm.radius() * 0.72);
                halfY = Math.max(22.0, storm.height() * 0.58);
                centerY = storm.baseY() + storm.height() * 0.52;
            } else if (storm.kind() == SevereWeatherModel.Kind.SANDSTORM) {
                halfY = 25.0;
                centerY = storm.baseY() + 23.0;
            } else {
                halfY = storm.kind() == SevereWeatherModel.Kind.SQUALL ? 38.0 : 48.0;
                centerY = storm.baseY() + storm.height() + halfY * 0.78;
                if (storm.kind() == SevereWeatherModel.Kind.HURRICANE) {
                    halfX *= 1.14;
                    halfZ *= 1.14;
                    halfY = 56.0;
                    centerY = storm.baseY() + storm.height() + 44.0;
                }
            }
            VolumetricCloudRenderer.addIfVisible(
                    volumes, renderKind, x, centerY, z, halfX, halfY, halfZ,
                    intensity, storm.kind() != SevereWeatherModel.Kind.SANDSTORM,
                    storm.travelX(), storm.travelZ(), storm.phase(), now,
                    cameraX, cameraY, cameraZ, renderDistance);
        }

        for (LocalStormManager.StormSnapshot storm : snapshots.localStorms) {
            double age = now - storm.startTick();
            double duration = storm.endTick() - storm.startTick();
            if (duration <= 0.0 || age < 0.0 || age >= duration) {
                continue;
            }
            double centerX = (storm.minChunkX() + storm.widthChunks() * 0.5) * 16.0;
            double centerZ = (storm.minChunkZ() + storm.widthChunks() * 0.5) * 16.0;
            double halfExtent = storm.widthChunks() * 8.0;
            double edgeDistance = Math.max(Math.abs(cameraX - centerX), Math.abs(cameraZ - centerZ)) / halfExtent;
            if (edgeDistance > 1.38) {
                continue;
            }
            double fade = Math.min(clamp(age / LOCAL_FADE_TICKS), clamp((duration - age) / LOCAL_FADE_TICKS));
            double visibleEdge = 1.0 - smoothStep(0.86, 1.38, edgeDistance);
            double intensity = fade * visibleEdge;
            if (intensity <= 0.025) {
                continue;
            }
            double centerY = Math.max(-16.0, Math.min(320.0, storm.baseY() + 76.0));
            VolumetricCloudRenderer.addIfVisible(
                    volumes, 0, centerX, centerY, centerZ,
                    halfExtent * 1.08, 58.0, halfExtent * 1.08,
                    intensity, true, 0.32, 0.72, (storm.minChunkX() * 31.0 + storm.minChunkZ()) * 0.017,
                    now, cameraX, cameraY, cameraZ, renderDistance);
        }

        RegionalWeatherManager.RegionalSnapshot regional = snapshots.regional;
        if (regional != null && regional.cloudCover() >= 0.18) {
            double intensity = clamp((regional.cloudCover() - 0.12) / 0.78);
            intensity = Math.max(intensity, clamp(regional.precipitationIntensity() * 0.90));
            intensity = Math.max(0.16, intensity);
            if (regional.thunderstorm()) {
                intensity = Math.max(intensity, 0.88);
            }
            double centerY = Math.max(110.0, Math.min(248.0, regional.y() + 84.0));
            double halfExtent = 235.0 + regional.cloudCover() * 140.0;
            VolumetricCloudRenderer.addIfVisible(
                    volumes, 7, regional.x(), centerY, regional.z(),
                    halfExtent, 54.0, halfExtent,
                    intensity, regional.thunderstorm(), regional.windX(), regional.windZ(), now * 0.002,
                    now, cameraX, cameraY, cameraZ, renderDistance);

            if (regional.thunderstorm() || regional.precipitationIntensity() >= 0.68) {
                double convectiveIntensity = Math.max(0.68, intensity * 0.92);
                double stormY = Math.min(304.0, centerY + 38.0);
                double stormExtent = 188.0 + regional.precipitationIntensity() * 82.0;
                VolumetricCloudRenderer.addIfVisible(
                        volumes, 0, regional.x(), stormY, regional.z(),
                        stormExtent, 82.0, stormExtent,
                        convectiveIntensity, regional.thunderstorm(),
                        regional.windX(), regional.windZ(), now * 0.0013,
                        now, cameraX, cameraY, cameraZ, renderDistance);
            }
        }
        return volumes;
    }

    private static int shaderKind(SevereWeatherModel.Kind kind) {
        if (kind == null) {
            return 7;
        }
        return switch (kind) {
            case SUPERCELL -> 1;
            case SQUALL -> 2;
            case HURRICANE -> 3;
            case HAIL -> 4;
            case TORNADO -> 5;
            case SANDSTORM -> 6;
            case NONE -> 7;
        };
    }

    private static void addIfVisible(
            List<CloudVolume> output,
            int kind,
            double x,
            double y,
            double z,
            double halfX,
            double halfY,
            double halfZ,
            double intensity,
            boolean lightning,
            double windX,
            double windZ,
            double phase,
            double time,
            double cameraX,
            double cameraY,
            double cameraZ,
            double renderDistance) {
        double dx = x - cameraX;
        double dy = y - cameraY;
        double dz = z - cameraZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double distance = Math.sqrt(distanceSquared);
        double extent = Math.max(halfX, Math.max(halfY, halfZ));
        double reach = renderDistance + extent;
        if (distance > reach || intensity <= 0.01) {
            return;
        }
        double distanceFade = 1.0 - VolumetricCloudRenderer.smoothStep(renderDistance, reach, distance);
        double visibleIntensity = intensity * distanceFade;
        if (visibleIntensity <= 0.01) {
            return;
        }
        double windLength = Math.hypot(windX, windZ);
        if (windLength < 1.0E-8) {
            windX = 1.0;
            windZ = 0.0;
        } else {
            windX /= windLength;
            windZ /= windLength;
        }
        output.add(new CloudVolume(
                kind,
                (float)dx,
                (float)dy,
                (float)dz,
                (float)Math.max(8.0, halfX),
                (float)Math.max(8.0, halfY),
                (float)Math.max(8.0, halfZ),
                (float)clamp(visibleIntensity, 0.0, 1.35),
                lightning ? 1.0f : 0.0f,
                (float)windX,
                (float)windZ,
                (float)phase,
                (float)time,
                distanceSquared));
    }

    private static void draw(
            List<CloudVolume> volumes,
            double cameraX,
            double cameraY,
            double cameraZ,
            double time,
            double dayTime,
            int requestedQuality,
            Matrix4f projectionMatrix) {
        Matrix4f viewRotation = new Matrix4f(RenderSystem.getModelViewMatrix()).setTranslation(0.0f, 0.0f, 0.0f);
        Matrix4f viewProjection = new Matrix4f(projectionMatrix).mul(viewRotation);
        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
        viewProjection.get(matrixBuffer);
        matrixBuffer.flip();

        GlState state = GlState.capture();
        try {
            GL20.glUseProgram(program);
            GL20.glUniformMatrix4fv(viewProjectionLocation, false, matrixBuffer);
            GL30.glBindVertexArray(vertexArray);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glBlendEquationSeparate(GL20.GL_FUNC_ADD, GL20.GL_FUNC_ADD);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_FRONT);
            GL11.glFrontFace(GL11.GL_CCW);
            GL13.glActiveTexture(CLOUD_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, cloudNoiseTexture);
            GL20.glUniform1i(cloudNoiseLocation, CLOUD_TEXTURE_UNIT - GL13.GL_TEXTURE0);
            CloudLightingModel.Lighting lighting = CloudLightingModel.atTime(dayTime);
            GL20.glUniform1f(daylightLocation, lighting.daylight());
            GL20.glUniform1f(twilightLocation, lighting.twilight());

            int steps = requestedQuality <= 1 ? 20 : requestedQuality == 2 ? 32 : 48;
            GL20.glUniform1i(stepsLocation, steps);
            for (CloudVolume volume : volumes) {
                GL20.glUniform3f(centerLocation, volume.centerX, volume.centerY, volume.centerZ);
                GL20.glUniform3f(halfSizeLocation, volume.halfX, volume.halfY, volume.halfZ);
                GL20.glUniform2f(windLocation, volume.windX, volume.windZ);
                GL20.glUniform1i(kindLocation, volume.kind);
                GL20.glUniform1f(intensityLocation, volume.intensity);
                GL20.glUniform1f(lightningLocation, volume.lightning);
                GL20.glUniform1f(phaseLocation, volume.phase);
                GL20.glUniform1f(timeLocation, (float)(time % 100_000.0));
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, UNIT_CUBE.length / 3);
            }
        } finally {
            state.restore();
        }
    }

    private static boolean ensureProgram() {
        if (program != 0) {
            return true;
        }
        try {
            int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, readResource(VERTEX_RESOURCE));
            int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, readResource(FRAGMENT_RESOURCE));
            int linkedProgram = GL20.glCreateProgram();
            GL20.glAttachShader(linkedProgram, vertexShader);
            GL20.glAttachShader(linkedProgram, fragmentShader);
            GL20.glBindAttribLocation(linkedProgram, 0, "Position");
            GL20.glLinkProgram(linkedProgram);
            if (GL20.glGetProgrami(linkedProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(linkedProgram, 4096);
                GL20.glDeleteProgram(linkedProgram);
                GL20.glDeleteShader(vertexShader);
                GL20.glDeleteShader(fragmentShader);
                throw new IllegalStateException("Cloud shader link failed: " + log);
            }
            GL20.glDetachShader(linkedProgram, vertexShader);
            GL20.glDetachShader(linkedProgram, fragmentShader);
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            program = linkedProgram;
            VolumetricCloudRenderer.createCubeBuffer();
            viewProjectionLocation = uniform("uViewProjection");
            centerLocation = uniform("uCenter");
            halfSizeLocation = uniform("uHalfSize");
            windLocation = uniform("uWind");
            kindLocation = uniform("uKind");
            stepsLocation = uniform("uSteps");
            intensityLocation = uniform("uIntensity");
            phaseLocation = uniform("uPhase");
            timeLocation = uniform("uTime");
            daylightLocation = uniform("uDaylight");
            twilightLocation = uniform("uTwilight");
            lightningLocation = uniform("uLightning");
            cloudNoiseLocation = uniform("uCloudNoise");
            cloudNoiseTexture = createCloudNoiseTexture();
            return true;
        } catch (Exception exception) {
            disabledAfterFailure = true;
            MushokuWeather.LOGGER.error("Could not initialize the volumetric weather shader", exception);
            return false;
        }
    }

    private static int uniform(String name) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) {
            throw new IllegalStateException("Cloud shader is missing uniform " + name);
        }
        return location;
    }

    private static int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Cloud shader compilation failed: " + log);
        }
        return shader;
    }

    private static String readResource(String resource) throws IOException {
        try (InputStream stream = VolumetricCloudRenderer.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing shader resource " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int createCloudNoiseTexture() throws IOException {
        BufferedImage image;
        try (InputStream stream = VolumetricCloudRenderer.class.getResourceAsStream(CLOUD_NOISE_RESOURCE)) {
            if (stream == null) {
                throw new IOException("Missing cloud-noise texture " + CLOUD_NOISE_RESOURCE);
            }
            image = ImageIO.read(stream);
        }
        if (image == null || image.getWidth() < 64 || image.getHeight() < 64) {
            throw new IOException("Invalid cloud-noise texture " + CLOUD_NOISE_RESOURCE);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 3);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int argb = image.getRGB(x, y);
                pixels.put((byte)(argb >> 16));
                pixels.put((byte)(argb >> 8));
                pixels.put((byte)argb);
            }
        }
        pixels.flip();
        image.flush();

        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(CLOUD_TEXTURE_UNIT);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int previousUnpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int texture = GL11.glGenTextures();
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, width, height, 0,
                    GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, pixels);
        } catch (RuntimeException exception) {
            GL11.glDeleteTextures(texture);
            throw exception;
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousUnpackAlignment);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            GL13.glActiveTexture(previousActiveTexture);
        }
        return texture;
    }

    private static void createCubeBuffer() {
        vertexArray = GL30.glGenVertexArrays();
        vertexBuffer = GL15.glGenBuffers();
        GL30.glBindVertexArray(vertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, UNIT_CUBE, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0L);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private static float[] createUnitCube() {
        return new float[] {
                // +Z face
                -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, 1, 1,
                // -Z face
                1, -1, -1, -1, -1, -1, -1, 1, -1, 1, -1, -1, -1, 1, -1, 1, 1, -1,
                // +X face
                1, -1, 1, 1, -1, -1, 1, 1, -1, 1, -1, 1, 1, 1, -1, 1, 1, 1,
                // -X face
                -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, -1, -1, -1, 1, 1, -1, 1, -1,
                // +Y face
                -1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, -1,
                // -Y face
                -1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, -1, 1, -1, 1, -1, -1, 1
        };
    }

    private static double smoothStep(double edge0, double edge1, double value) {
        double amount = clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return amount * amount * (3.0 - 2.0 * amount);
    }

    private static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }

    private static double clamp(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private record SnapshotSet(
            List<LocalStormManager.StormSnapshot> localStorms,
            List<SevereWeatherManager.StormSnapshot> severeStorms,
            RegionalWeatherManager.RegionalSnapshot regional) {
    }

    private static final class CloudVolume {
        private final int kind;
        private final float centerX;
        private final float centerY;
        private final float centerZ;
        private final float halfX;
        private final float halfY;
        private final float halfZ;
        private final float intensity;
        private final float lightning;
        private final float windX;
        private final float windZ;
        private final float phase;
        private final float time;
        private final double distanceSquared;

        private CloudVolume(
                int kind,
                float centerX,
                float centerY,
                float centerZ,
                float halfX,
                float halfY,
                float halfZ,
                float intensity,
                float lightning,
                float windX,
                float windZ,
                float phase,
                float time,
                double distanceSquared) {
            this.kind = kind;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.halfX = halfX;
            this.halfY = halfY;
            this.halfZ = halfZ;
            this.intensity = intensity;
            this.lightning = lightning;
            this.windX = windX;
            this.windZ = windZ;
            this.phase = phase;
            this.time = time;
            this.distanceSquared = distanceSquared;
        }
    }

    private static final class GlState {
        private final boolean blend;
        private final boolean depthTest;
        private final boolean cull;
        private final boolean depthMask;
        private final int depthFunction;
        private final int blendSourceRgb;
        private final int blendDestinationRgb;
        private final int blendSourceAlpha;
        private final int blendDestinationAlpha;
        private final int blendEquationRgb;
        private final int blendEquationAlpha;
        private final int cullFace;
        private final int frontFace;
        private final int program;
        private final int vertexArray;
        private final int arrayBuffer;
        private final int activeTexture;
        private final int cloudTextureBinding;

        private GlState() {
            this.blend = GL11.glIsEnabled(GL11.GL_BLEND);
            this.depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            this.cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            this.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            this.depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            this.blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            this.blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            this.blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            this.blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            this.blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
            this.blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
            this.cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
            this.frontFace = GL11.glGetInteger(GL11.GL_FRONT_FACE);
            this.program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            this.vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            this.arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            this.activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(CLOUD_TEXTURE_UNIT);
            this.cloudTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(this.activeTexture);
        }

        private static GlState capture() {
            return new GlState();
        }

        private void restore() {
            if (this.blend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
            if (this.depthTest) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (this.cull) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(this.depthMask);
            GL11.glDepthFunc(this.depthFunction);
            GL14.glBlendFuncSeparate(this.blendSourceRgb, this.blendDestinationRgb,
                    this.blendSourceAlpha, this.blendDestinationAlpha);
            GL20.glBlendEquationSeparate(this.blendEquationRgb, this.blendEquationAlpha);
            GL11.glCullFace(this.cullFace);
            GL11.glFrontFace(this.frontFace);
            GL20.glUseProgram(this.program);
            GL30.glBindVertexArray(this.vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.arrayBuffer);
            GL13.glActiveTexture(CLOUD_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.cloudTextureBinding);
            GL13.glActiveTexture(this.activeTexture);
        }
    }
}
