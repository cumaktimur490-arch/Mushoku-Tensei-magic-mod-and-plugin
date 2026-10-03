package com.mushokumagic.weather.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_5217;
import net.minecraft.class_638;

/**
 * Applies the server's local storm state to vanilla rain rendering only while the
 * client player is inside a Cumulonimbus sector. No server-wide weather is changed.
 */
@Environment(EnvType.CLIENT)
public final class ClientStormWeather {
    private static final long FADE_TICKS = 100L;
    private static final Map<String, List<LocalStormManager.StormSnapshot>> SNAPSHOTS = new HashMap<>();

    private static class_638 overriddenWorld;
    private static boolean savedRaining;
    private static float savedRainGradient;
    private static float savedThunderGradient;

    private ClientStormWeather() {
    }

    public static void applySnapshot(
            String dimension,
            List<LocalStormManager.StormSnapshot> storms,
            List<SevereWeatherManager.StormSnapshot> severeStorms,
            RegionalWeatherManager.RegionalSnapshot regional) {
        if (dimension == null || dimension.isBlank()) {
            return;
        }
        if (storms == null || storms.isEmpty()) {
            SNAPSHOTS.remove(dimension);
        } else {
            SNAPSHOTS.put(dimension, List.copyOf(storms));
        }
        VolumetricCloudRenderer.updateSnapshots(dimension, storms, severeStorms, regional);
        while (SNAPSHOTS.size() > 8) {
            SNAPSHOTS.remove(SNAPSHOTS.keySet().iterator().next());
        }
    }

    public static void tick() {
        class_310 client = class_310.method_1551();
        class_638 world = client.field_1687;
        if (world == null || client.field_1724 == null) {
            ClientStormWeather.restore();
            SNAPSHOTS.clear();
            VolumetricCloudRenderer.clear();
            return;
        }

        String dimension = world.method_27983().toString();
        List<LocalStormManager.StormSnapshot> storms = SNAPSHOTS.get(dimension);
        double intensity = ClientStormWeather.intensityAt(
                storms,
                client.field_1724.method_73189().method_10216(),
                client.field_1724.method_73189().method_10215(),
                world.method_75260());
        if (intensity <= 0.001) {
            ClientStormWeather.restore();
            return;
        }

        if (overriddenWorld != world) {
            ClientStormWeather.restore();
            overriddenWorld = world;
            class_5217 properties = world.method_8401();
            savedRaining = properties.method_156();
            savedRainGradient = world.method_8430(1.0f);
            savedThunderGradient = world.method_8478(1.0f);
        }

        class_5217 properties = world.method_8401();
        properties.method_157(true);
        world.method_8519((float)intensity);
        world.method_8496((float)Math.min(1.0, intensity * 0.82));
    }

    public static void clear() {
        SNAPSHOTS.clear();
        VolumetricCloudRenderer.clear();
        ClientStormWeather.restore();
    }

    private static double intensityAt(
            List<LocalStormManager.StormSnapshot> storms,
            double x,
            double z,
            long now) {
        if (storms == null || storms.isEmpty()) {
            return 0.0;
        }
        double strongest = 0.0;
        for (LocalStormManager.StormSnapshot storm : storms) {
            StormSector sector = new StormSector(storm.minChunkX(), storm.minChunkZ(), storm.widthChunks());
            if (!sector.contains(x, z) || now < storm.startTick() || now >= storm.endTick()) {
                continue;
            }
            double halfExtent = storm.widthChunks() * 8.0;
            double centerX = (storm.minChunkX() + storm.widthChunks() * 0.5) * 16.0;
            double centerZ = (storm.minChunkZ() + storm.widthChunks() * 0.5) * 16.0;
            double edgeDistance = Math.max(Math.abs(x - centerX), Math.abs(z - centerZ)) / halfExtent;
            double fadeIn = clamp((double)(now - storm.startTick()) / FADE_TICKS);
            double fadeOut = clamp((double)(storm.endTick() - now) / FADE_TICKS);
            double intensity = Math.min(fadeIn, fadeOut) * WeatherPhysicsModel.sectorEdgeFalloff(edgeDistance);
            strongest = Math.max(strongest, intensity);
        }
        return strongest;
    }

    private static void restore() {
        if (overriddenWorld == null) {
            return;
        }
        class_5217 properties = overriddenWorld.method_8401();
        properties.method_157(savedRaining);
        overriddenWorld.method_8519(savedRainGradient);
        overriddenWorld.method_8496(savedThunderGradient);
        overriddenWorld = null;
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }
}
