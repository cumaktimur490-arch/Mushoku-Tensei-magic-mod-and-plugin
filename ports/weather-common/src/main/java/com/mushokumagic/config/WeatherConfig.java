package com.mushokumagic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mushokumagic.weather.MushokuWeather;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server settings for the standalone, locally simulated weather system. */
public final class WeatherConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static WeatherConfig instance = new WeatherConfig();

    /** Replace dimension-wide vanilla weather with spatially localized weather. */
    public boolean regionalWeatherEnabled = true;
    /** Enable naturally forming and operator-started severe storm systems. */
    public boolean severeWeatherEnabled = true;
    /** Allow tornadoes/cyclones to remove blocks; disabled by default. */
    public boolean weatherBlockDamage = false;
    /** Render true shader-based 3D cloud volumes on clients. */
    public boolean volumetricCloudsEnabled = true;
    /** Raymarch quality: 1 performance, 2 balanced, 3 detailed. */
    public int volumetricCloudQuality = 2;
    /** Maximum distance at which volumetric weather is rendered, in blocks. */
    public int cloudRenderDistance = 768;

    /** Multiplies all natural severe-weather spawn chances. */
    public double severeWeatherSpawnMultiplier = 1.0;
    public double supercellSpawnChance = 0.16;
    public double squallSpawnChance = 0.22;
    public double tornadoSpawnChance = 0.055;
    public double cycloneSpawnChance = 0.025;
    public double hailSpawnChance = 0.58;
    public double sandstormSpawnChance = 0.22;
    /** Scales storm footprints without changing the 20-by-20 Cumulonimbus sector. */
    public double stormSizeMultiplier = 1.0;
    /** Scales peak force intensity before per-hazard safety caps. */
    public double stormStrengthMultiplier = 1.0;
    /** Chance that a newly formed severe system intensifies during its mature phase. */
    public double stormStrengtheningChance = 0.35;
    public int maxConcurrentStorms = 4;
    /** Hard cap on blocks one enabled destructive storm may remove. */
    public int maxBlocksPerStorm = 64;
    /** Highest block hardness affected when weatherBlockDamage is enabled. */
    public double maxDamageableBlockHardness = 2.0;

    public static WeatherConfig get() {
        return instance;
    }

    public static void load() {
        Path path = configPath();
        WeatherConfig loaded = Files.exists(path) ? read(path) : readLegacyMagicConfig(path);
        instance = loaded;
        save();
    }

    public static void reload() {
        load();
    }

    public static void save() {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            MushokuWeather.LOGGER.error("Failed to save Mushoku Weather configuration", exception);
        }
    }

    private static WeatherConfig read(Path path) {
        WeatherConfig defaults = new WeatherConfig();
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            defaults.readValues(json);
        } catch (Exception exception) {
            MushokuWeather.LOGGER.error("Failed to read Mushoku Weather configuration; using safe defaults", exception);
        }
        return defaults;
    }

    /** Imports legacy weather toggles on the first launch after the Magic/Weather split. */
    private static WeatherConfig readLegacyMagicConfig(Path weatherPath) {
        WeatherConfig defaults = new WeatherConfig();
        Path legacyPath = weatherPath.resolveSibling("mushoku_magic.json");
        if (!Files.exists(legacyPath)) {
            return defaults;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(legacyPath, StandardCharsets.UTF_8)).getAsJsonObject();
            defaults.readValues(json);
            MushokuWeather.LOGGER.info("Migrated weather settings from mushoku_magic.json to mushoku_weather.json");
        } catch (Exception exception) {
            MushokuWeather.LOGGER.error("Failed to migrate legacy weather settings; using defaults", exception);
        }
        return defaults;
    }

    private void readValues(JsonObject json) {
        this.regionalWeatherEnabled = readBoolean(json, "regionalWeatherEnabled", this.regionalWeatherEnabled);
        this.severeWeatherEnabled = readBoolean(json, "severeWeatherEnabled", this.severeWeatherEnabled);
        this.weatherBlockDamage = readBoolean(json, "weatherBlockDamage", this.weatherBlockDamage);
        this.volumetricCloudsEnabled = readBoolean(json, "volumetricCloudsEnabled", this.volumetricCloudsEnabled);
        this.volumetricCloudQuality = readInt(json, "volumetricCloudQuality", this.volumetricCloudQuality, 1, 3);
        this.cloudRenderDistance = readInt(json, "cloudRenderDistance", this.cloudRenderDistance, 128, 2048);
        this.severeWeatherSpawnMultiplier = readDouble(
                json, "severeWeatherSpawnMultiplier", this.severeWeatherSpawnMultiplier, 0.0, 5.0);
        this.supercellSpawnChance = readDouble(json, "supercellSpawnChance", this.supercellSpawnChance, 0.0, 1.0);
        this.squallSpawnChance = readDouble(json, "squallSpawnChance", this.squallSpawnChance, 0.0, 1.0);
        this.tornadoSpawnChance = readDouble(json, "tornadoSpawnChance", this.tornadoSpawnChance, 0.0, 1.0);
        this.cycloneSpawnChance = readDouble(json, "cycloneSpawnChance", this.cycloneSpawnChance, 0.0, 1.0);
        this.hailSpawnChance = readDouble(json, "hailSpawnChance", this.hailSpawnChance, 0.0, 1.0);
        this.sandstormSpawnChance = readDouble(json, "sandstormSpawnChance", this.sandstormSpawnChance, 0.0, 1.0);
        this.stormSizeMultiplier = readDouble(json, "stormSizeMultiplier", this.stormSizeMultiplier, 0.5, 2.5);
        this.stormStrengthMultiplier = readDouble(json, "stormStrengthMultiplier", this.stormStrengthMultiplier, 0.5, 1.5);
        this.stormStrengtheningChance = readDouble(
                json, "stormStrengtheningChance", this.stormStrengtheningChance, 0.0, 1.0);
        this.maxConcurrentStorms = readInt(json, "maxConcurrentStorms", this.maxConcurrentStorms, 1, 10);
        this.maxBlocksPerStorm = readInt(json, "maxBlocksPerStorm", this.maxBlocksPerStorm, 0, 512);
        this.maxDamageableBlockHardness = readDouble(
                json, "maxDamageableBlockHardness", this.maxDamageableBlockHardness, 0.0, 3.0);
    }

    private static boolean readBoolean(JsonObject json, String key, boolean fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean()
                : fallback;
    }

    private static double readDouble(JsonObject json, String key, double fallback, double min, double max) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        double parsed = value.getAsDouble();
        return Double.isFinite(parsed) ? Math.max(min, Math.min(max, parsed)) : fallback;
    }

    private static int readInt(JsonObject json, String key, int fallback, int min, int max) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        try {
            int parsed = value.getAsInt();
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Path configPath() {
        return Path.of("config").resolve("mushoku_weather.json");
    }
}
