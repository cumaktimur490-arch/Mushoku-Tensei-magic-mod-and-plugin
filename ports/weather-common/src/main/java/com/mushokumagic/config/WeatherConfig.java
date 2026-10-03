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

/** Settings owned by the standalone weather mod, independent of magic progression. */
public final class WeatherConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static WeatherConfig instance = new WeatherConfig();

    public boolean regionalWeatherEnabled = true;
    public boolean severeWeatherEnabled = true;
    public boolean weatherBlockDamage = false;

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
            defaults.regionalWeatherEnabled = readBoolean(json, "regionalWeatherEnabled", defaults.regionalWeatherEnabled);
            defaults.severeWeatherEnabled = readBoolean(json, "severeWeatherEnabled", defaults.severeWeatherEnabled);
            defaults.weatherBlockDamage = readBoolean(json, "weatherBlockDamage", defaults.weatherBlockDamage);
        } catch (Exception exception) {
            MushokuWeather.LOGGER.error("Failed to read Mushoku Weather configuration; using safe defaults", exception);
        }
        return defaults;
    }

    private static WeatherConfig readLegacyMagicConfig(Path weatherPath) {
        WeatherConfig defaults = new WeatherConfig();
        Path legacyPath = weatherPath.resolveSibling("mushoku_magic.json");
        if (!Files.exists(legacyPath)) {
            return defaults;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(legacyPath, StandardCharsets.UTF_8)).getAsJsonObject();
            defaults.regionalWeatherEnabled = readBoolean(json, "regionalWeatherEnabled", defaults.regionalWeatherEnabled);
            defaults.severeWeatherEnabled = readBoolean(json, "severeWeatherEnabled", defaults.severeWeatherEnabled);
            defaults.weatherBlockDamage = readBoolean(json, "weatherBlockDamage", defaults.weatherBlockDamage);
            MushokuWeather.LOGGER.info("Migrated weather settings from mushoku_magic.json to mushoku_weather.json");
        } catch (Exception exception) {
            MushokuWeather.LOGGER.error("Failed to migrate legacy weather settings; using defaults", exception);
        }
        return defaults;
    }

    private static boolean readBoolean(JsonObject json, String key, boolean fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean()
                : fallback;
    }

    private static Path configPath() {
        return Path.of("config").resolve("mushoku_weather.json");
    }
}
