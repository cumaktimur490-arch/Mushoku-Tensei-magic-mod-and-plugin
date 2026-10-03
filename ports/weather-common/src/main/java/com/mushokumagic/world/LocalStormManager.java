package com.mushokumagic.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_243;
import net.minecraft.class_2394;
import net.minecraft.class_2398;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

/** Simulates localized Cumulonimbus weather without changing a world's global weather. */
public final class LocalStormManager {
    private static final int TICK_INTERVAL = 5;
    private static final int MAX_STORMS_PER_WORLD = 8;
    private static final long FADE_TICKS = 100L;
    private static final long MIN_LIGHTNING_DELAY = 240L;
    private static final long LIGHTNING_DELAY_VARIANCE = 360L;
    private static final Map<class_3218, List<Storm>> STORMS = new IdentityHashMap<>();
    private static final Set<class_3218> DIRTY_CLIENT_SNAPSHOTS = Collections.newSetFromMap(new IdentityHashMap<>());

    private LocalStormManager() {
    }

    public static void start(class_3218 level, class_243 center, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        long now = level.method_8510();
        long endTick = now + durationTicks;
        double baseY = center.method_10214();
        StormSector sector = StormSector.centeredAt(center.method_10216(), center.method_10215());
        List<Storm> storms = STORMS.computeIfAbsent(level, ignored -> new ArrayList<>());
        for (Storm storm : storms) {
            if (!storm.sector.equals(sector)) {
                continue;
            }
            boolean changed = false;
            if (endTick > storm.endTick) {
                storm.endTick = endTick;
                changed = true;
            }
            if (Math.abs(storm.baseY - baseY) > 1.0) {
                storm.baseY = baseY;
                changed = true;
            }
            if (changed) {
                DIRTY_CLIENT_SNAPSHOTS.add(level);
            }
            return;
        }
        if (storms.size() >= MAX_STORMS_PER_WORLD) {
            storms.remove(0);
        }
        storms.add(new Storm(sector, baseY, now, endTick, now + LocalStormManager.lightningDelay(level)));
        DIRTY_CLIENT_SNAPSHOTS.add(level);
    }

    /** Sends the active sector snapshot after a player joins or changes dimensions. */
    public static void syncToPlayer(class_3222 player) {
        if (player == null) {
            return;
        }
        class_3218 level = player.method_51469();
        LocalWeatherNetwork.syncToPlayer(player, level, LocalStormManager.snapshots(level));
    }

    public static List<StormSnapshot> snapshots(class_3218 level) {
        List<Storm> storms = STORMS.get(level);
        if (storms == null || storms.isEmpty()) {
            return List.of();
        }
        ArrayList<StormSnapshot> snapshots = new ArrayList<>(storms.size());
        for (Storm storm : storms) {
            snapshots.add(new StormSnapshot(
                    storm.sector.minChunkX(),
                    storm.sector.minChunkZ(),
                    storm.sector.widthChunks(),
                    storm.baseY,
                    storm.startTick,
                    storm.endTick));
        }
        return List.copyOf(snapshots);
    }

    private static void syncWorld(class_3218 level, List<class_3222> players) {
        List<StormSnapshot> snapshots = LocalStormManager.snapshots(level);
        for (class_3222 player : players) {
            if (player.method_51469() == level) {
                LocalWeatherNetwork.syncToPlayer(player, level, snapshots);
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (STORMS.isEmpty()) {
            return;
        }
        List<class_3222> players = server.method_3760().method_14571();
        Iterator<Map.Entry<class_3218, List<Storm>>> worlds = STORMS.entrySet().iterator();
        while (worlds.hasNext()) {
            Map.Entry<class_3218, List<Storm>> entry = worlds.next();
            class_3218 level = entry.getKey();
            List<Storm> storms = entry.getValue();
            long now = level.method_8510();
            int previousStormCount = storms.size();
            storms.removeIf(storm -> now >= storm.endTick);
            boolean dirtySnapshot = DIRTY_CLIENT_SNAPSHOTS.remove(level);
            boolean snapshotChanged = storms.size() != previousStormCount || dirtySnapshot;
            if (snapshotChanged) {
                LocalStormManager.syncWorld(level, players);
            }
            if (storms.isEmpty()) {
                worlds.remove();
                DIRTY_CLIENT_SNAPSHOTS.remove(level);
                continue;
            }
            if (now % TICK_INTERVAL != 0L) {
                continue;
            }

            Map<Storm, Set<WeatherCell>> emittedCells = new IdentityHashMap<>();
            for (class_3222 player : players) {
                if (player.method_51469() != level) {
                    continue;
                }
                class_243 position = player.method_19538();
                Storm activeStorm = LocalStormManager.findStorm(storms, position);
                if (activeStorm == null) {
                    continue;
                }
                WeatherCell cell = WeatherCell.from(position);
                Set<WeatherCell> cells = emittedCells.computeIfAbsent(activeStorm, ignored -> new HashSet<>());
                if (cells.add(cell)) {
                    LocalStormManager.spawnWeather(level, cell);
                }
            }

            for (Storm storm : storms) {
                if (now < storm.nextLightningTick) {
                    continue;
                }
                class_3222 observer = LocalStormManager.firstPlayerInStorm(players, level, storm.sector);
                if (observer == null) {
                    storm.nextLightningTick = now + TICK_INTERVAL * 4L;
                    continue;
                }
                LocalStormManager.spawnLightning(level, observer);
                storm.nextLightningTick = now + LocalStormManager.lightningDelay(level);
            }
        }
    }

    public static void clear() {
        STORMS.clear();
        DIRTY_CLIENT_SNAPSHOTS.clear();
    }

    /** Samples the storm's cyclonic wind and convective lift inside its fixed 20x20 chunk sector. */
    public static StormWeather weatherAt(class_3218 level, double x, double z) {
        List<Storm> storms = STORMS.get(level);
        if (storms == null || storms.isEmpty()) {
            return StormWeather.NONE;
        }
        long now = level.method_8510();
        StormWeather strongest = StormWeather.NONE;
        for (Storm storm : storms) {
            if (!storm.sector.contains(x, z)) {
                continue;
            }
            double halfExtent = storm.sector.widthChunks() * 8.0;
            double centerX = (storm.sector.minChunkX() + storm.sector.widthChunks() * 0.5) * 16.0;
            double centerZ = (storm.sector.minChunkZ() + storm.sector.widthChunks() * 0.5) * 16.0;
            double normalizedDistance = Math.max(Math.abs(x - centerX), Math.abs(z - centerZ)) / halfExtent;
            double strength = storm.strengthAt(now)
                    * WeatherPhysicsModel.sectorEdgeFalloff(normalizedDistance);
            if (strength <= strongest.intensity()) {
                continue;
            }

            double offsetX = x - centerX;
            double offsetZ = z - centerZ;
            double distance = Math.hypot(offsetX, offsetZ);
            if (distance < 1.0E-6) {
                long orientation = ((long)storm.sector.minChunkX() * 31L + storm.sector.minChunkZ()) & 3L;
                offsetX = orientation == 0L || orientation == 2L ? 1.0 : 0.0;
                offsetZ = orientation == 1L || orientation == 3L ? 1.0 : 0.0;
                distance = 1.0;
            }
            // A rotating storm has tangential flow with a modest inward component.
            double windX = (-offsetZ / distance) * 0.85 - (offsetX / distance) * 0.15;
            double windZ = (offsetX / distance) * 0.85 - (offsetZ / distance) * 0.15;
            double windLength = Math.hypot(windX, windZ);
            strongest = new StormWeather(
                    strength,
                    windX / windLength,
                    windZ / windLength,
                    0.25 + strength * 0.75,
                    0.12 + strength * 0.46,
                    strength >= 0.72);
        }
        return strongest;
    }

    private static Storm findStorm(List<Storm> storms, class_243 position) {
        for (Storm storm : storms) {
            if (storm.sector.contains(position.method_10216(), position.method_10215())) {
                return storm;
            }
        }
        return null;
    }

    private static class_3222 firstPlayerInStorm(List<class_3222> players, class_3218 level, StormSector sector) {
        for (class_3222 player : players) {
            if (player.method_51469() != level) {
                continue;
            }
            class_243 position = player.method_19538();
            if (sector.contains(position.method_10216(), position.method_10215())) {
                return player;
            }
        }
        return null;
    }

    private static void spawnWeather(class_3218 level, WeatherCell cell) {
        double x = cell.centerX();
        double z = cell.centerZ();
        StormWeather flow = LocalStormManager.weatherAt(level, x, z);
        double driftX = flow.windX() * flow.windStrength() * 3.0;
        double driftZ = flow.windZ() * flow.windStrength() * 3.0;
        if (flow.intensity() > 0.04) {
            class_2394 rain = (class_2394)class_2398.field_11242;
            double rainY = cell.minY() + 26.0;
            level.method_14199(rain,
                    x + driftX, rainY, z + driftZ,
                    18 + (int)Math.round(44.0 * flow.intensity()),
                    10.0 + flow.intensity() * 5.0, 10.0,
                    10.0 + flow.intensity() * 5.0,
                    0.075 + flow.windStrength() * 0.07);
            WeatherVisuals.emitDirectionalPrecipitation(
                    level, rain, x + driftX, rainY - 1.0, z + driftZ,
                    4 + (int)Math.round(flow.intensity() * 5.0),
                    8.0 + flow.intensity() * 3.0, 8.0,
                    flow.windX(), flow.windZ(), flow.windStrength(), false);
        }
        if (flow.windStrength() >= 0.5) {
            WeatherVisuals.emitWindThreads(
                    level,
                    x + driftX,
                    cell.minY() + 10.0,
                    z + driftZ,
                    4 + (int)Math.round(flow.windStrength() * 3.0),
                    14.0,
                    5.0,
                    flow.windX(),
                    flow.windZ(),
                    flow.windStrength());
        }
    }

    private static void spawnLightning(class_3218 level, class_3222 observer) {
        class_243 position = observer.method_19538();
        double x = position.method_10216() + LocalStormManager.randomOffset(level, 8.0);
        double z = position.method_10215() + LocalStormManager.randomOffset(level, 8.0);
        WeatherVisuals.emitLightning(level, x, z, position.method_10214(), 24.0);
    }

    private static double randomOffset(class_3218 level, double radius) {
        return (level.field_9229.method_43058() * 2.0 - 1.0) * radius;
    }

    private static long lightningDelay(class_3218 level) {
        return MIN_LIGHTNING_DELAY + (long)(level.field_9229.method_43058() * LIGHTNING_DELAY_VARIANCE);
    }

    private record WeatherCell(int cellX, int cellY, int cellZ) {
        private static WeatherCell from(class_243 position) {
            int blockX = (int)Math.floor(position.method_10216());
            int blockY = (int)Math.floor(position.method_10214());
            int blockZ = (int)Math.floor(position.method_10215());
            return new WeatherCell(
                    Math.floorDiv(blockX, 16),
                    Math.floorDiv(blockY, 16),
                    Math.floorDiv(blockZ, 16));
        }

        private int centerX() {
            return this.cellX * 16 + 8;
        }

        private int minY() {
            return this.cellY * 16;
        }

        private int centerZ() {
            return this.cellZ * 16 + 8;
        }
    }

    public record StormSnapshot(
            int minChunkX,
            int minChunkZ,
            int widthChunks,
            double baseY,
            long startTick,
            long endTick) {
    }

    public record StormWeather(
            double intensity,
            double windX,
            double windZ,
            double windStrength,
            double verticalLift,
            boolean thunderstorm) {
        private static final StormWeather NONE = new StormWeather(0.0, 1.0, 0.0, 0.0, 0.0, false);
    }

    private static final class Storm {
        private final StormSector sector;
        private double baseY;
        private final long startTick;
        private long endTick;
        private long nextLightningTick;

        private Storm(StormSector sector, double baseY, long startTick, long endTick, long nextLightningTick) {
            this.sector = sector;
            this.baseY = baseY;
            this.startTick = startTick;
            this.endTick = endTick;
            this.nextLightningTick = nextLightningTick;
        }

        private double strengthAt(long now) {
            double fadeIn = Math.min(1.0, Math.max(0.0, (double)(now - this.startTick) / FADE_TICKS));
            double fadeOut = Math.min(1.0, Math.max(0.0, (double)(this.endTick - now) / FADE_TICKS));
            return Math.min(fadeIn, fadeOut);
        }
    }
}
