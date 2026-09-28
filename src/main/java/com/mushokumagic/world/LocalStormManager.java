package com.mushokumagic.world;

import com.mushokumagic.spell.MagicPalette;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_2390;
import net.minecraft.class_2394;
import net.minecraft.class_2398;
import net.minecraft.class_243;
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
    private static final class_2394 STORM_CLOUD = new class_2390(0x59636E, 1.5f);
    private static final Map<class_3218, List<Storm>> STORMS = new IdentityHashMap<>();

    private LocalStormManager() {
    }

    public static void start(class_3218 level, class_243 center, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        long now = level.method_75260();
        long endTick = now + durationTicks;
        StormSector sector = StormSector.centeredAt(center.method_10216(), center.method_10215());
        List<Storm> storms = STORMS.computeIfAbsent(level, ignored -> new ArrayList<>());
        for (Storm storm : storms) {
            if (!storm.sector.equals(sector)) {
                continue;
            }
            storm.endTick = Math.max(storm.endTick, endTick);
            return;
        }
        if (storms.size() >= MAX_STORMS_PER_WORLD) {
            storms.remove(0);
        }
        storms.add(new Storm(sector, now, endTick, now + LocalStormManager.lightningDelay(level)));
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
            long now = level.method_75260();
            storms.removeIf(storm -> now >= storm.endTick);
            if (storms.isEmpty()) {
                worlds.remove();
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
                class_243 position = player.method_73189();
                Storm activeStorm = LocalStormManager.findStorm(storms, position);
                if (activeStorm == null) {
                    continue;
                }
                WeatherCell cell = WeatherCell.from(position);
                Set<WeatherCell> cells = emittedCells.computeIfAbsent(activeStorm, ignored -> new HashSet<>());
                if (cells.add(cell)) {
                    LocalStormManager.spawnWeather(level, cell, activeStorm, now);
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
            class_243 position = player.method_73189();
            if (sector.contains(position.method_10216(), position.method_10215())) {
                return player;
            }
        }
        return null;
    }

    private static void spawnWeather(class_3218 level, WeatherCell cell, Storm storm, long now) {
        double strength = storm.strengthAt(now);
        double x = cell.centerX();
        double rainY = cell.minY() + 24.0;
        double cloudY = cell.minY() + 27.0;
        double z = cell.centerZ();
        double windPhase = now * 0.018 + storm.sector.minChunkX() * 0.17 + storm.sector.minChunkZ() * 0.11;
        double driftX = Math.sin(windPhase) * 2.5;
        double driftZ = Math.cos(windPhase) * 2.5;
        double rainSpread = 15.0 + strength * 6.0;
        int rainCount = 12 + (int)Math.round(56.0 * strength);
        int cloudCount = 2 + (int)Math.round(8.0 * strength);
        int darkCloudCount = 4 + (int)Math.round(10.0 * strength);

        level.method_65096((class_2394)class_2398.field_11242,
                x + driftX, rainY, z + driftZ,
                rainCount, rainSpread, 8.0, rainSpread, 0.06);
        level.method_65096((class_2394)class_2398.field_11204,
                x + driftX, cloudY, z + driftZ,
                cloudCount, 22.0, 2.5, 22.0, 0.008);
        level.method_65096(LocalStormManager.STORM_CLOUD,
                x + driftX, cloudY, z + driftZ,
                darkCloudCount, 24.0, 3.0, 24.0, 0.004);
    }

    private static void spawnLightning(class_3218 level, class_3222 observer) {
        class_243 position = observer.method_73189();
        double x = position.method_10216() + LocalStormManager.randomOffset(level, 8.0);
        double z = position.method_10215() + LocalStormManager.randomOffset(level, 8.0);
        double topY = position.method_10214() + 24.0;
        for (int segment = 0; segment < 9; ++segment) {
            if (segment > 0) {
                x += LocalStormManager.randomOffset(level, 1.0);
                z += LocalStormManager.randomOffset(level, 1.0);
            }
            double y = topY - segment * 3.0;
            level.method_65096(MagicPalette.core("water", 1.5f),
                    x, y, z, 2, 0.45, 0.9, 0.45, 0.02);
            if (segment % 2 == 0) {
                level.method_65096((class_2394)class_2398.field_11207,
                        x, y, z, 1, 0.15, 0.7, 0.15, 0.01);
            }
        }
        level.method_65096(MagicPalette.core("water", 2.0f),
                x, topY - 7.0, z, 24, 3.0, 4.0, 3.0, 0.035);
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

    private static final class Storm {
        private final StormSector sector;
        private final long startTick;
        private long endTick;
        private long nextLightningTick;

        private Storm(StormSector sector, long startTick, long endTick, long nextLightningTick) {
            this.sector = sector;
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
