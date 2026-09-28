package com.mushokumagic.world;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.spell.MagicPalette;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2374;
import net.minecraft.class_2390;
import net.minecraft.class_2394;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_5217;
import net.minecraft.server.MinecraftServer;

/**
 * Simulates biome-aware weather fronts around players without letting vanilla's
 * dimension-wide rain and thunder affect the whole world.
 */
public final class RegionalWeatherManager {
    private static final long PARTICLE_INTERVAL = 10L;
    private static final int VANILLA_CLEAR_DURATION_TICKS = 12_000;
    private static final long VANILLA_WEATHER_REFRESH_TICKS = 6_000L;
    private static final class_2394 OVERCAST_CLOUD = new class_2390(0x58626E, 1.25f);
    private static final class_2394 DRY_DUST = new class_2390(0xBDA477, 0.8f);
    private static final Map<class_3218, WorldState> WORLDS = new IdentityHashMap<>();

    private RegionalWeatherManager() {
    }

    public static void tick(MinecraftServer server) {
        Iterable<class_3218> levels = server.method_3738();
        if (!MagicConfig.get().regionalWeatherEnabled) {
            RegionalWeatherManager.releaseVanillaWeather();
            return;
        }

        List<class_3222> players = server.method_3760().method_14571();
        for (class_3218 level : levels) {
            long now = level.method_75260();
            WorldState worldState = WORLDS.computeIfAbsent(
                    level,
                    ignored -> new WorldState(ThreadLocalRandom.current().nextLong()));
            RegionalWeatherManager.keepVanillaWeatherClear(level, worldState, now);
            if (now % PARTICLE_INTERVAL != 0L) {
                continue;
            }

            Set<WeatherCell> emittedCells = new HashSet<>();
            for (class_3222 player : players) {
                if (player.method_51469() != level) {
                    continue;
                }
                class_243 position = player.method_73189();
                WeatherCell cell = WeatherCell.from(position);
                if (!emittedCells.add(cell)) {
                    continue;
                }
                RegionalWeatherManager.emitForCell(level, cell, now);
            }
        }
    }

    public static void clear() {
        RegionalWeatherManager.releaseVanillaWeather();
        WORLDS.clear();
    }

    /** Samples the same regional climate used by particles, or vanilla rain when the feature is disabled. */
    public static RegionalWeatherModel.WeatherState sampleAt(class_3218 level, double x, double y, double z) {
        class_2338 samplePos = class_2338.method_49637(Math.floor(x), Math.floor(y), Math.floor(z));
        class_1959 biome = (class_1959)level.method_23753(samplePos).comp_349();
        float biomeTemperature = biome.method_8712();
        boolean hasPrecipitation = biome.method_48163();
        WorldState state = WORLDS.computeIfAbsent(
                level,
                ignored -> new WorldState(ThreadLocalRandom.current().nextLong()));
        RegionalWeatherModel.WeatherState regional = RegionalWeatherModel.sample(
                state.seed,
                x,
                y,
                z,
                level.method_75260(),
                biomeTemperature,
                hasPrecipitation);
        if (MagicConfig.get().regionalWeatherEnabled) {
            return regional;
        }

        class_5217 properties = level.method_8401();
        boolean raining = properties.method_156();
        boolean thunder = properties.method_203();
        double precipitation = raining ? (thunder ? 0.9 : 0.72) : 0.0;
        RegionalWeatherModel.Precipitation kind = precipitation <= 0.0
                ? RegionalWeatherModel.Precipitation.NONE
                : regional.temperature() <= 0.15
                        ? RegionalWeatherModel.Precipitation.SNOW
                        : RegionalWeatherModel.Precipitation.RAIN;
        return new RegionalWeatherModel.WeatherState(
                regional.pressure(),
                raining ? 0.72 : 0.25,
                raining ? 0.9 : 0.12,
                precipitation,
                regional.temperature(),
                regional.windX(),
                regional.windZ(),
                raining ? (thunder ? 0.75 : 0.4) : 0.0,
                kind,
                thunder && kind == RegionalWeatherModel.Precipitation.RAIN);
    }

    private static void releaseVanillaWeather() {
        for (Map.Entry<class_3218, WorldState> entry : WORLDS.entrySet()) {
            if (entry.getValue().vanillaWeatherLocked) {
                RegionalWeatherManager.restoreVanillaWeather(entry.getKey());
                entry.getValue().vanillaWeatherLocked = false;
            }
        }
    }

    private static void keepVanillaWeatherClear(class_3218 level, WorldState state, long now) {
        class_5217 properties = level.method_8401();
        if (!state.vanillaWeatherLocked
                || now >= state.nextVanillaWeatherRefresh
                || properties.method_156()
                || properties.method_203()) {
            level.method_27910(VANILLA_CLEAR_DURATION_TICKS, 0, false, false);
            state.vanillaWeatherLocked = true;
            state.nextVanillaWeatherRefresh = now + VANILLA_WEATHER_REFRESH_TICKS;
        }
    }

    private static void restoreVanillaWeather(class_3218 level) {
        level.method_27910(0, 0, false, false);
    }

    private static void emitForCell(class_3218 level, WeatherCell cell, long now) {
        WorldState worldState = WORLDS.computeIfAbsent(
                level,
                ignored -> new WorldState(ThreadLocalRandom.current().nextLong()));
        double sampleX = cell.centerX();
        double sampleY = cell.minY() + 8.0;
        double sampleZ = cell.centerZ();
        class_2338 samplePos = class_2338.method_49638((class_2374)new class_243(sampleX, sampleY, sampleZ));
        if (!level.method_8311(samplePos)) {
            return;
        }
        class_1959 biome = (class_1959)level.method_23753(samplePos).comp_349();
        float biomeTemperature = biome.method_8712();
        boolean hasPrecipitation = biome.method_48163();
        RegionalWeatherModel.WeatherState weather = RegionalWeatherManager.sampleAt(
                level,
                sampleX,
                sampleY,
                sampleZ);

        double driftX = weather.windX() * weather.windStrength() * 3.0;
        double driftZ = weather.windZ() * weather.windStrength() * 3.0;
        double x = sampleX + driftX;
        double z = sampleZ + driftZ;
        double rainY = cell.minY() + 24.0;
        double cloudY = cell.minY() + 27.0;

        if (hasPrecipitation && weather.cloudCover() >= 0.40) {
            int cloudCount = 2 + (int)Math.round(weather.cloudCover() * 6.0);
            double cloudSpread = 13.0 + weather.cloudCover() * 9.0;
            level.method_65096((class_2394)class_2398.field_11204,
                    x, cloudY, z,
                    cloudCount, cloudSpread, 2.5, cloudSpread, 0.006 + weather.windStrength() * 0.004);
            if (weather.cloudCover() >= 0.62) {
                int darkCloudCount = 2 + (int)Math.round((weather.cloudCover() - 0.5) * 14.0);
                level.method_65096(OVERCAST_CLOUD,
                        x, cloudY, z,
                        darkCloudCount, cloudSpread, 1.8, cloudSpread, 0.003);
            }
        }

        if (weather.precipitationIntensity() > 0.02) {
            class_2394 precipitation = weather.precipitation() == RegionalWeatherModel.Precipitation.SNOW
                    ? (class_2394)class_2398.field_28013
                    : (class_2394)class_2398.field_11242;
            int precipitationCount = 6 + (int)Math.round(42.0 * weather.precipitationIntensity());
            double spread = 9.0 + weather.precipitationIntensity() * 8.0;
            double speed = weather.precipitation() == RegionalWeatherModel.Precipitation.SNOW
                    ? 0.015 + weather.windStrength() * 0.025
                    : 0.035 + weather.windStrength() * 0.05;
            level.method_65096(precipitation,
                    x, rainY, z,
                    precipitationCount, spread, 7.0, spread, speed);
        } else if (!hasPrecipitation && biomeTemperature > 1.0f && weather.windStrength() >= 0.72) {
            int dustCount = 3 + (int)Math.round(weather.windStrength() * 5.0);
            level.method_65096(DRY_DUST,
                    x, cell.minY() + 10.0, z,
                    dustCount, 9.0, 3.0, 9.0, 0.006 + weather.windStrength() * 0.012);
        }

        if (weather.windStrength() >= 0.78 && now % 40L == 0L) {
            level.method_65096((class_2394)class_2398.field_50252,
                    x, cell.minY() + 12.0, z,
                    1, 5.0, 2.5, 5.0, 0.04);
        }

        if (weather.thunderstorm() && RegionalWeatherManager.isLightningTick(worldState.seed, cell, now)) {
            RegionalWeatherManager.spawnLightning(level, cell);
        }
    }

    private static boolean isLightningTick(long seed, WeatherCell cell, long now) {
        long hash = seed
                ^ (long)cell.cellX * 0x9E3779B97F4A7C15L
                ^ (long)cell.cellY * 0xC2B2AE3D27D4EB4FL
                ^ (long)cell.cellZ * 0x165667B19E3779F9L;
        hash ^= hash >>> 30;
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= hash >>> 27;
        long interval = 300L + Math.floorMod(hash, 360L);
        long phase = Math.floorMod(hash >>> 32, interval);
        return Math.floorMod(now + phase, interval) < PARTICLE_INTERVAL;
    }

    private static void spawnLightning(class_3218 level, WeatherCell cell) {
        double x = cell.centerX() + RegionalWeatherManager.randomOffset(level, 7.0);
        double z = cell.centerZ() + RegionalWeatherManager.randomOffset(level, 7.0);
        double topY = cell.minY() + 25.0;
        for (int segment = 0; segment < 9; ++segment) {
            if (segment > 0) {
                x += RegionalWeatherManager.randomOffset(level, 0.8);
                z += RegionalWeatherManager.randomOffset(level, 0.8);
            }
            double y = topY - segment * 3.0;
            level.method_65096(MagicPalette.core("water", 1.25f),
                    x, y, z, 2, 0.35, 0.7, 0.35, 0.02);
            if (segment % 2 == 0) {
                level.method_65096((class_2394)class_2398.field_11207,
                        x, y, z, 1, 0.12, 0.6, 0.12, 0.01);
            }
        }
        level.method_65096(MagicPalette.core("water", 1.8f),
                x, topY - 7.0, z, 18, 2.2, 3.5, 2.2, 0.03);
    }

    private static double randomOffset(class_3218 level, double radius) {
        return (level.field_9229.method_43058() * 2.0 - 1.0) * radius;
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

    private static final class WorldState {
        private final long seed;
        private long nextVanillaWeatherRefresh = Long.MIN_VALUE;
        private boolean vanillaWeatherLocked;

        private WorldState(long seed) {
            this.seed = seed;
        }
    }
}
