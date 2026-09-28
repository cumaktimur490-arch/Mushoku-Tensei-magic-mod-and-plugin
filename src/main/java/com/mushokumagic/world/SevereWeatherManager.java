package com.mushokumagic.world;

import com.mushokumagic.config.MagicConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2374;
import net.minecraft.class_2390;
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
    private static final class_2394 CLOUD = new class_2390(0xAEB7BF, 1.25f);
    private static final class_2394 STORM_CLOUD = new class_2390(0x5A626B, 1.45f);
    private static final class_2394 TORNADO_DUST = new class_2390(0x737A80, 0.85f);
    private static final class_2394 HAIL_GRAIN = new class_2390(0xEAF5FF, 0.55f);
    private static final class_2394 SAND_DUST = new class_2390(0xC9AD7A, 0.9f);
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
            long now = level.method_75260();
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

        long now = level.method_75260();
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

    private static void tryFormSystem(class_3218 level, WorldState state, class_3222 player, long now) {
        class_243 position = player.method_73189();
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
            class_243 position = player.method_73189();
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
        if (now % 20L == 0L) {
            SevereWeatherManager.emitCloudLayers(level, system, cloudY, intensity);
        }

        switch (system.kind) {
            case HURRICANE -> {
                level.method_65096((class_2394)class_2398.field_11242,
                        system.x, cloudY - 7.0, system.z,
                        56 + (int)Math.round(64.0 * intensity), system.radius * 0.78, 9.0, system.radius * 0.78, 0.13);
                SevereWeatherManager.emitHurricaneBands(level, system, now, cloudY, intensity);
            }
            case TORNADO -> SevereWeatherManager.emitTornadoFunnel(level, system, now, intensity);
            case HAIL -> {
                level.method_65096((class_2394)class_2398.field_11242,
                        system.x, cloudY - 6.0, system.z,
                        32 + (int)Math.round(36.0 * intensity), system.radius * 0.55, 7.0, system.radius * 0.55, 0.12);
                level.method_65096(HAIL_GRAIN,
                        system.x, cloudY - 7.0, system.z,
                        18 + (int)Math.round(34.0 * intensity), system.radius * 0.52, 8.0, system.radius * 0.52, 0.18);
            }
            case SANDSTORM -> {
                level.method_65096(SAND_DUST,
                        system.x, system.baseY + 5.0, system.z,
                        38 + (int)Math.round(48.0 * intensity), system.radius * 0.72, 5.0, system.radius * 0.72, 0.11);
                level.method_65096(TORNADO_DUST,
                        system.x, system.baseY + 9.0, system.z,
                        10 + (int)Math.round(14.0 * intensity), system.radius * 0.64, 4.0, system.radius * 0.64, 0.055);
            }
            case NONE -> {
            }
        }
    }

    private static void emitCloudLayers(class_3218 level, SystemCell system, double cloudY, double intensity) {
        double cloudRadius = system.radius * (system.kind == SevereWeatherModel.Kind.TORNADO ? 0.9 : 0.78);
        int cloudCount = 8 + (int)Math.round(12.0 * intensity);
        for (int layer = 0; layer < 3; ++layer) {
            double y = cloudY + layer * 2.2;
            level.method_65096(CLOUD,
                    system.x + system.travelX * layer * 2.0, y, system.z + system.travelZ * layer * 2.0,
                    cloudCount, cloudRadius, 1.5, cloudRadius, 0.004);
            level.method_65096(STORM_CLOUD,
                    system.x - system.travelX * layer * 1.5, y - 1.0, system.z - system.travelZ * layer * 1.5,
                    Math.max(4, cloudCount / 2), cloudRadius * 0.84, 1.3, cloudRadius * 0.84, 0.002);
        }
    }

    private static void emitHurricaneBands(
            class_3218 level,
            SystemCell system,
            long now,
            double cloudY,
            double intensity) {
        double rotation = now * 0.018 + system.phase;
        for (int band = 0; band < 4; ++band) {
            double ringRadius = system.radius * (0.28 + band * 0.15);
            double y = cloudY - 5.0 + band * 2.4;
            for (int segment = 0; segment < 8; ++segment) {
                double angle = rotation + band * 0.9 + segment * (Math.PI * 2.0 / 8.0);
                double x = system.x + Math.cos(angle) * ringRadius;
                double z = system.z + Math.sin(angle) * ringRadius;
                level.method_65096(STORM_CLOUD,
                        x, y, z,
                        1 + (int)Math.round(intensity * 2.0), 2.2, 1.3, 2.2, 0.005);
                if (segment % 2 == 0) {
                    level.method_65096(CLOUD, x, y + 0.5, z, 1, 3.0, 1.0, 3.0, 0.003);
                }
            }
        }
    }

    private static void emitTornadoFunnel(class_3218 level, SystemCell system, long now, double intensity) {
        double rotation = now * (0.075 + intensity * 0.035) + system.phase;
        for (int layer = 0; layer < 7; ++layer) {
            double progress = (double)layer / 6.0;
            double y = system.baseY + 2.0 + progress * system.height;
            double ringRadius = (1.6 + progress * 6.5) * (0.55 + intensity * 0.45);
            for (int segment = 0; segment < 5; ++segment) {
                double angle = rotation + layer * 0.72 + segment * (Math.PI * 2.0 / 5.0);
                double x = system.x + Math.cos(angle) * ringRadius;
                double z = system.z + Math.sin(angle) * ringRadius;
                level.method_65096(TORNADO_DUST,
                        x, y, z,
                        1, 0.35, 0.55, 0.35, 0.035 + intensity * 0.02);
                if (segment % 2 == 0) {
                    level.method_65096(CLOUD,
                            x, y + 0.5, z,
                            1, 0.55, 0.4, 0.55, 0.018);
                }
            }
        }
        if (now % 10L == 0L) {
            level.method_65096((class_2394)class_2398.field_11242,
                    system.x, system.baseY + system.height * 0.55, system.z,
                    26 + (int)Math.round(22.0 * intensity), system.radius * 0.58, system.height * 0.42, system.radius * 0.58, 0.09);
            level.method_65096(SAND_DUST,
                    system.x, system.baseY + 1.0, system.z,
                    12 + (int)Math.round(18.0 * intensity), system.radius * 0.4, 1.0, system.radius * 0.4, 0.08);
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
