package com.mushokumagic.world;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.spell.MagicPalette;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2374;
import net.minecraft.class_2394;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

/**
 * Creates short-lived, moving severe-weather systems only in loaded areas near players.
 * Their particle fields and forces are local; this never changes dimension-wide weather.
 */
public final class SevereWeatherManager {
    private static final long SPAWN_CHECK_INTERVAL = 600L;
    private static final long PARTICLE_INTERVAL = 5L;
    private static final long BLOCK_CHECK_INTERVAL = 40L;
    private static final long EVENT_CELL_COOLDOWN = 12_000L;
    private static final int MAX_SYSTEMS_PER_WORLD = 4;
    private static final double MIN_PLAYER_DISTANCE = 320.0;
    private static final double PARTICLE_VIEW_PADDING = 96.0;
    private static final class_2394 CLOUD = MagicPalette.dust(0xCFD9E0, 1.45f);
    private static final class_2394 STORM_CLOUD = MagicPalette.dust(0x3D4856, 1.75f);
    private static final class_2394 TORNADO_DUST = MagicPalette.dust(0x78828B, 1.12f);
    private static final class_2394 HAIL_GRAIN = MagicPalette.dust(0xEAF5FF, 0.82f);
    private static final class_2394 SAND_DUST = MagicPalette.dust(0xC9AD7A, 1.15f);
    private static final Map<class_3218, WorldState> WORLDS = new IdentityHashMap<>();

    private SevereWeatherManager() {
    }

    public static void tick(MinecraftServer server) {
        if (!MagicConfig.get().regionalWeatherEnabled || !MagicConfig.get().severeWeatherEnabled) {
            SevereWeatherManager.clear();
            return;
        }

        List<class_3222> players = server.method_3760().method_14571();
        for (class_3218 level : server.method_3738()) {
            long now = level.method_8510();
            WorldState state = WORLDS.computeIfAbsent(level, ignored -> new WorldState());
            state.systems.removeIf(system -> now >= system.endTick);
            if (now % SPAWN_CHECK_INTERVAL == 0L) {
                SevereWeatherManager.pruneCooldowns(state, now);
                for (class_3222 player : players) {
                    if (player.method_51469() == level) {
                        SevereWeatherManager.tryFormSystem(level, state, player, now);
                    }
                }
            }
            if (now % PARTICLE_INTERVAL != 0L) {
                continue;
            }

            for (SystemCell system : state.systems) {
                system.moveTo(now);
                if (!SevereWeatherManager.hasNearbyPlayer(players, level, system)) {
                    continue;
                }
                SevereWeatherManager.emitWeather(level, system, now);
                if (MagicConfig.get().weatherBlockDamage
                        && (system.kind == SevereWeatherModel.Kind.TORNADO
                                || system.kind == SevereWeatherModel.Kind.HURRICANE)
                        && now % BLOCK_CHECK_INTERVAL == 0L) {
                    SevereWeatherManager.damageFragileBlock(level, system);
                }
            }
        }
    }

    /** Returns the strongest nearby moving hazard at a point, without loading chunks. */
    public static WeatherSample weatherAt(class_3218 level, double x, double y, double z) {
        WorldState state = WORLDS.get(level);
        if (state == null || state.systems.isEmpty()) {
            return WeatherSample.NONE;
        }

        long now = level.method_8510();
        WeatherSample strongest = WeatherSample.NONE;
        for (SystemCell system : state.systems) {
            double verticalDistance = Math.abs(y - system.baseY);
            if (system.kind == SevereWeatherModel.Kind.TORNADO && verticalDistance > system.height + 6.0) {
                continue;
            }
            double dx = x - system.x;
            double dz = z - system.z;
            double distance = Math.hypot(dx, dz);
            double radial = SevereWeatherModel.radialInfluence(distance, system.radius);
            if (radial <= 0.0) {
                continue;
            }
            double life = SevereWeatherModel.lifecycleStrength(now - system.startTick, system.duration());
            double intensity = system.baseIntensity * life * radial;
            if (intensity <= strongest.intensity() || intensity < 0.025) {
                continue;
            }
            SevereWeatherModel.Forces forces = SevereWeatherModel.forces(
                    system.kind,
                    intensity,
                    radial,
                    dx,
                    dz,
                    system.travelX,
                    system.travelZ);
            strongest = new WeatherSample(
                    system.kind,
                    intensity,
                    forces.windX(),
                    forces.windZ(),
                    forces.windStrength(),
                    forces.verticalLift(),
                    forces.precipitationIntensity(),
                    forces.hailing(),
                    forces.tornadoIntensity());
        }
        return strongest;
    }

    public static void clear() {
        WORLDS.clear();
    }

    /** Creates an explicitly requested local hazard, bypassing natural-spawn odds. */
    public static boolean startManual(
            class_3218 level,
            class_243 center,
            SevereWeatherModel.Kind kind,
            int durationTicks) {
        if (!MagicConfig.get().severeWeatherEnabled
                || kind == null
                || kind == SevereWeatherModel.Kind.NONE
                || durationTicks <= 0) {
            return false;
        }
        RegionalWeatherModel.WeatherState climate = RegionalWeatherManager.sampleAt(
                level,
                center.method_10216(),
                center.method_10214(),
                center.method_10215());
        double windLength = Math.hypot(climate.windX(), climate.windZ());
        double travelX = windLength > 1.0E-8 ? climate.windX() / windLength : 1.0;
        double travelZ = windLength > 1.0E-8 ? climate.windZ() / windLength : 0.0;
        double radius = switch (kind) {
            case HURRICANE -> 144.0;
            case TORNADO -> 38.0;
            case HAIL -> 58.0;
            case SANDSTORM -> 72.0;
            case NONE -> 0.0;
        };
        double travelSpeed = switch (kind) {
            case HURRICANE -> 0.011;
            case TORNADO -> 0.034;
            case HAIL -> 0.018;
            case SANDSTORM -> 0.038;
            case NONE -> 0.0;
        };
        int height = kind == SevereWeatherModel.Kind.TORNADO ? 48 : 28;
        long now = level.method_8510();
        double offset = kind == SevereWeatherModel.Kind.TORNADO ? 26.0 : 18.0;
        if (kind == SevereWeatherModel.Kind.TORNADO || kind == SevereWeatherModel.Kind.HURRICANE) {
            offset += level.field_9229.method_43058() * 14.0;
        }
        WorldState state = WORLDS.computeIfAbsent(level, ignored -> new WorldState());
        if (state.systems.size() >= MAX_SYSTEMS_PER_WORLD) {
            state.systems.remove(0);
        }
        state.systems.add(new SystemCell(
                kind,
                center.method_10216() + travelX * offset,
                center.method_10214(),
                center.method_10215() + travelZ * offset,
                travelX,
                travelZ,
                travelSpeed,
                radius,
                height,
                Math.max(0.78, SevereWeatherModel.initialIntensity(kind, climate)),
                level.field_9229.method_43058() * Math.PI * 2.0,
                now,
                now + Math.min(72_000L, (long)durationTicks)));
        return true;
    }

    /** Removes nearby temporary hazards when an administrator clears the local weather. */
    public static void clearNear(class_3218 level, double x, double z, double radius) {
        WorldState state = WORLDS.get(level);
        if (state == null || state.systems.isEmpty()) {
            return;
        }
        state.systems.removeIf(system -> Math.hypot(system.x - x, system.z - z) <= radius + system.radius);
        if (state.systems.isEmpty()) {
            WORLDS.remove(level);
        }
    }

    private static void tryFormSystem(class_3218 level, WorldState state, class_3222 player, long now) {
        class_243 position = player.method_19538();
        class_2338 blockPos = class_2338.method_49638((class_2374)position);
        if (!level.method_8311(blockPos)) {
            return;
        }
        class_1959 biome = (class_1959)level.method_23753(blockPos).comp_349();
        RegionalWeatherModel.WeatherState climate = RegionalWeatherManager.sampleAt(
                level,
                position.method_10216(),
                position.method_10214(),
                position.method_10215());
        double roll = level.field_9229.method_43058();
        SevereWeatherModel.Kind kind = SevereWeatherModel.chooseKind(
                climate,
                biome.method_8712(),
                biome.method_48163(),
                roll);
        if (kind == SevereWeatherModel.Kind.NONE) {
            return;
        }

        EventCell eventCell = EventCell.from(position);
        if (state.nextEventTick.getOrDefault(eventCell, 0L) > now
                || state.systems.size() >= MAX_SYSTEMS_PER_WORLD
                || SevereWeatherManager.hasNearbySystem(state.systems, position.method_10216(), position.method_10215())) {
            return;
        }

        double angle = level.field_9229.method_43058() * Math.PI * 2.0;
        double offset = 16.0 + level.field_9229.method_43058() * 24.0;
        double centerX = position.method_10216() + Math.cos(angle) * offset;
        double centerZ = position.method_10215() + Math.sin(angle) * offset;
        double duration = switch (kind) {
            case HURRICANE -> 12_000.0 + level.field_9229.method_43058() * 12_000.0;
            case TORNADO -> 3_200.0 + level.field_9229.method_43058() * 2_400.0;
            case HAIL -> 1_800.0 + level.field_9229.method_43058() * 1_800.0;
            case SANDSTORM -> 2_400.0 + level.field_9229.method_43058() * 2_800.0;
            case NONE -> 0.0;
        };
        double radius = switch (kind) {
            case HURRICANE -> 128.0 + level.field_9229.method_43058() * 64.0;
            case TORNADO -> 34.0 + level.field_9229.method_43058() * 10.0;
            case HAIL -> 48.0 + level.field_9229.method_43058() * 20.0;
            case SANDSTORM -> 60.0 + level.field_9229.method_43058() * 24.0;
            case NONE -> 0.0;
        };
        double travelSpeed = switch (kind) {
            case HURRICANE -> 0.008 + level.field_9229.method_43058() * 0.007;
            case TORNADO -> 0.026 + level.field_9229.method_43058() * 0.018;
            case HAIL -> 0.012 + level.field_9229.method_43058() * 0.012;
            case SANDSTORM -> 0.026 + level.field_9229.method_43058() * 0.024;
            case NONE -> 0.0;
        };
        double travelLength = Math.hypot(climate.windX(), climate.windZ());
        double travelX = travelLength > 1.0E-8 ? climate.windX() / travelLength : 1.0;
        double travelZ = travelLength > 1.0E-8 ? climate.windZ() / travelLength : 0.0;
        int height = switch (kind) {
            case TORNADO -> 30 + (int)Math.round(26.0 * SevereWeatherModel.initialIntensity(kind, climate));
            case HURRICANE -> 34;
            default -> 20;
        };
        SystemCell system = new SystemCell(
                kind,
                centerX,
                position.method_10214(),
                centerZ,
                travelX,
                travelZ,
                travelSpeed,
                radius,
                height,
                SevereWeatherModel.initialIntensity(kind, climate),
                level.field_9229.method_43058() * Math.PI * 2.0,
                now,
                now + (long)duration);
        state.systems.add(system);
        state.nextEventTick.put(eventCell, now + EVENT_CELL_COOLDOWN);
        if (state.systems.size() > MAX_SYSTEMS_PER_WORLD) {
            state.systems.remove(0);
        }
    }

    private static boolean hasNearbySystem(List<SystemCell> systems, double x, double z) {
        for (SystemCell system : systems) {
            if (Math.hypot(system.x - x, system.z - z) < MIN_PLAYER_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbyPlayer(List<class_3222> players, class_3218 level, SystemCell system) {
        double distanceLimit = system.radius + PARTICLE_VIEW_PADDING;
        double distanceSquared = distanceLimit * distanceLimit;
        for (class_3222 player : players) {
            if (player.method_51469() != level) {
                continue;
            }
            class_243 position = player.method_19538();
            double dx = position.method_10216() - system.x;
            double dz = position.method_10215() - system.z;
            if (dx * dx + dz * dz <= distanceSquared) {
                return true;
            }
        }
        return false;
    }

    private static void emitWeather(class_3218 level, SystemCell system, long now) {
        double lifecycle = SevereWeatherModel.lifecycleStrength(now - system.startTick, system.duration());
        double intensity = system.baseIntensity * lifecycle;
        if (intensity <= 0.02) {
            return;
        }
        double cloudY = system.baseY + system.height + 5.0;
        if (now % 10L == 0L) {
            SevereWeatherManager.emitCloudLayers(level, system, cloudY, intensity, now);
        }

        switch (system.kind) {
            case HURRICANE -> {
                class_2394 rain = (class_2394)class_2398.field_11242;
                level.method_14199(rain,
                        system.x, cloudY - 7.0, system.z,
                        56 + (int)Math.round(64.0 * intensity), system.radius * 0.78, 9.0, system.radius * 0.78, 0.13);
                WeatherVisuals.emitDirectionalPrecipitation(
                        level, rain, system.x, cloudY - 7.0, system.z,
                        8, system.radius * 0.58, 9.0,
                        system.travelX, system.travelZ, intensity, false);
                SevereWeatherManager.emitHurricaneBands(level, system, now, cloudY, intensity);
            }
            case TORNADO -> SevereWeatherManager.emitTornadoFunnel(level, system, now, intensity);
            case HAIL -> {
                class_2394 rain = (class_2394)class_2398.field_11242;
                level.method_14199(rain,
                        system.x, cloudY - 6.0, system.z,
                        32 + (int)Math.round(36.0 * intensity), system.radius * 0.55, 7.0, system.radius * 0.55, 0.12);
                WeatherVisuals.emitDirectionalPrecipitation(
                        level, rain, system.x, cloudY - 6.0, system.z,
                        6, system.radius * 0.45, 8.0,
                        system.travelX, system.travelZ, intensity, false);
                level.method_14199(HAIL_GRAIN,
                        system.x, cloudY - 7.0, system.z,
                        18 + (int)Math.round(34.0 * intensity), system.radius * 0.52, 8.0, system.radius * 0.52, 0.18);
            }
            case SANDSTORM -> {
                level.method_14199(SAND_DUST,
                        system.x, system.baseY + 5.0, system.z,
                        38 + (int)Math.round(48.0 * intensity), system.radius * 0.72, 5.0, system.radius * 0.72, 0.11);
                level.method_14199((class_2394)class_2398.field_11237,
                        system.x, system.baseY + 2.0, system.z,
                        6 + (int)Math.round(8.0 * intensity), system.radius * 0.58, 1.6, system.radius * 0.58, 0.045);
                level.method_14199(TORNADO_DUST,
                        system.x, system.baseY + 9.0, system.z,
                        10 + (int)Math.round(14.0 * intensity), system.radius * 0.64, 4.0, system.radius * 0.64, 0.055);
                WeatherVisuals.emitDriftingParticles(
                        level,
                        SAND_DUST,
                        system.x,
                        system.baseY + 6.0,
                        system.z,
                        7,
                        system.radius * 0.72,
                        6.0,
                        system.travelX,
                        system.travelZ,
                        intensity);
            }
            case NONE -> {
            }
        }
    }

    private static void emitCloudLayers(
            class_3218 level,
            SystemCell system,
            double cloudY,
            double intensity,
            long now) {
        double cloudRadius = system.radius * (system.kind == SevereWeatherModel.Kind.TORNADO ? 0.82 : 0.68);
        WeatherVisuals.emitCloudDeck(
                level,
                system.x,
                cloudY,
                system.z,
                cloudRadius,
                Math.min(1.0, 0.68 + intensity * 0.32),
                system.travelX,
                system.travelZ,
                now,
                true);
    }

    private static void emitHurricaneBands(
            class_3218 level,
            SystemCell system,
            long now,
            double cloudY,
            double intensity) {
        double rotation = now * 0.02 + system.phase;
        for (int band = 0; band < 4; ++band) {
            double ringRadius = system.radius * (0.24 + band * 0.165);
            double y = cloudY - 5.5 + band * 2.1;
            for (int segment = 0; segment < 12; ++segment) {
                double angle = rotation + band * 0.82 + segment * (Math.PI * 2.0 / 12.0);
                double x = system.x + Math.cos(angle) * ringRadius;
                double z = system.z + Math.sin(angle) * ringRadius;
                level.method_14199(STORM_CLOUD,
                        x, y, z,
                        1 + (int)Math.round(intensity * 2.0), 2.5, 1.45, 2.5, 0.006);
                if (segment % 3 == 0 && band % 2 == 0) {
                    SevereWeatherModel.VortexFlow flow = SevereWeatherModel.vortexFlow(
                            x - system.x, z - system.z,
                            system.travelX, system.travelZ,
                            intensity, 0.38);
                    level.method_14199((class_2394)class_2398.field_11237,
                            x, y, z,
                            0, flow.x(), flow.y(), flow.z(), 1.0);
                }
                if (segment % 3 != 1) {
                    level.method_14199(CLOUD, x, y + 0.55, z, 1, 2.6, 1.1, 2.6, 0.004);
                }
            }
        }
    }

    private static void emitTornadoFunnel(class_3218 level, SystemCell system, long now, double intensity) {
        double rotation = now * (0.075 + intensity * 0.035) + system.phase;
        for (int layer = 0; layer < 10; ++layer) {
            double progress = (double)layer / 9.0;
            double y = system.baseY + 1.5 + progress * system.height;
            double ringRadius = (1.5 + progress * 8.2) * (0.55 + intensity * 0.45);
            for (int segment = 0; segment < 7; ++segment) {
                double angle = rotation + layer * 0.58 + segment * (Math.PI * 2.0 / 7.0);
                double x = system.x + Math.cos(angle) * ringRadius;
                double z = system.z + Math.sin(angle) * ringRadius;
                level.method_14199(TORNADO_DUST,
                        x, y, z,
                        1, 0.42, 0.62, 0.42, 0.045 + intensity * 0.025);
                if ((segment + layer) % 2 == 0) {
                    SevereWeatherModel.VortexFlow flow = SevereWeatherModel.vortexFlow(
                            x - system.x, z - system.z,
                            system.travelX, system.travelZ,
                            intensity, 1.0 - progress * 0.24);
                    level.method_14199((class_2394)class_2398.field_11237,
                            x, y, z,
                            0, flow.x(), flow.y(), flow.z(), 1.0);
                    level.method_14199(CLOUD,
                            x, y + 0.55, z,
                            1, 0.62, 0.48, 0.62, 0.024);
                }
            }
        }
        if (now % 10L == 0L) {
            level.method_14199((class_2394)class_2398.field_11242,
                    system.x, system.baseY + system.height * 0.55, system.z,
                    26 + (int)Math.round(22.0 * intensity), system.radius * 0.58, system.height * 0.42, system.radius * 0.58, 0.11);
            level.method_14199(SAND_DUST,
                    system.x, system.baseY + 1.0, system.z,
                    18 + (int)Math.round(20.0 * intensity), system.radius * 0.44, 1.2, system.radius * 0.44, 0.1);
            level.method_14199((class_2394)class_2398.field_11237,
                    system.x, system.baseY + 1.2, system.z,
                    6 + (int)Math.round(10.0 * intensity), system.radius * 0.36, 0.8, system.radius * 0.36, 0.06);
        }
    }

    private static void damageFragileBlock(class_3218 level, SystemCell system) {
        if (system.damagedBlocks >= 24) {
            return;
        }
        for (int attempt = 0; attempt < 3; ++attempt) {
            double angle = level.field_9229.method_43058() * Math.PI * 2.0;
            double distance = Math.sqrt(level.field_9229.method_43058()) * system.radius * 0.45;
            int x = (int)Math.floor(system.x + Math.cos(angle) * distance);
            int z = (int)Math.floor(system.z + Math.sin(angle) * distance);
            int y = (int)Math.floor(system.baseY - 1.0 + level.field_9229.method_43058() * 3.0);
            class_2338 pos = class_2338.method_49637(x, y, z);
            class_2680 block = level.method_8320(pos);
            if (block.method_45474()) {
                continue;
            }
            float hardness = block.method_26214(level, pos);
            if (hardness < 0.0f || hardness > 0.3f) {
                continue;
            }
            level.method_8650(pos, false);
            system.damagedBlocks++;
            break;
        }
    }

    private static void pruneCooldowns(WorldState state, long now) {
        Iterator<Map.Entry<EventCell, Long>> iterator = state.nextEventTick.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
        if (state.nextEventTick.size() > 512) {
            state.nextEventTick.clear();
        }
    }

    public record WeatherSample(
            SevereWeatherModel.Kind kind,
            double intensity,
            double windX,
            double windZ,
            double windStrength,
            double verticalLift,
            double precipitationIntensity,
            boolean hailing,
            double tornadoIntensity) {
        private static final WeatherSample NONE = new WeatherSample(
                SevereWeatherModel.Kind.NONE, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, false, 0.0);
    }

    private static final class WorldState {
        private final List<SystemCell> systems = new ArrayList<>();
        private final Map<EventCell, Long> nextEventTick = new HashMap<>();
    }

    private static final class SystemCell {
        private final SevereWeatherModel.Kind kind;
        private final double travelX;
        private final double travelZ;
        private final double travelSpeed;
        private final double radius;
        private final int height;
        private final double baseIntensity;
        private final double phase;
        private final long startTick;
        private final long endTick;
        private double x;
        private double baseY;
        private double z;
        private long lastMoveTick;
        private int damagedBlocks;

        private SystemCell(
                SevereWeatherModel.Kind kind,
                double x,
                double baseY,
                double z,
                double travelX,
                double travelZ,
                double travelSpeed,
                double radius,
                int height,
                double baseIntensity,
                double phase,
                long startTick,
                long endTick) {
            this.kind = kind;
            this.x = x;
            this.baseY = baseY;
            this.z = z;
            this.travelX = travelX;
            this.travelZ = travelZ;
            this.travelSpeed = travelSpeed;
            this.radius = radius;
            this.height = height;
            this.baseIntensity = baseIntensity;
            this.phase = phase;
            this.startTick = startTick;
            this.endTick = endTick;
            this.lastMoveTick = startTick;
        }

        private long duration() {
            return this.endTick - this.startTick;
        }

        private void moveTo(long now) {
            long elapsed = now - this.lastMoveTick;
            if (elapsed <= 0L) {
                return;
            }
            this.x += this.travelX * this.travelSpeed * elapsed;
            this.z += this.travelZ * this.travelSpeed * elapsed;
            this.lastMoveTick = now;
        }
    }

    private record EventCell(int x, int z) {
        private static EventCell from(class_243 position) {
            int blockX = (int)Math.floor(position.method_10216());
            int blockZ = (int)Math.floor(position.method_10215());
            return new EventCell(Math.floorDiv(blockX, 320), Math.floorDiv(blockZ, 320));
        }
    }
}
