package com.mushokumagic.weather.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeatherConfigTest {
    @Test
    void defaultsEnableRegionalAndSevereWeatherWithoutBlockDamage() {
        WeatherConfig config = new WeatherConfig();

        assertTrue(config.regionalWeatherEnabled);
        assertTrue(config.severeWeatherEnabled);
        assertFalse(config.weatherBlockDamage);
        assertTrue(config.volumetricCloudsEnabled);
        assertEquals(3, config.volumetricCloudQuality);
        assertEquals(768, config.cloudRenderDistance);
        assertEquals(1.0, config.severeWeatherSpawnMultiplier, 0.0);
        assertEquals(1.0, config.stormSizeMultiplier, 0.0);
        assertEquals(1.0, config.stormStrengthMultiplier, 0.0);
        assertEquals(4, config.maxConcurrentStorms);
        assertEquals(64, config.maxBlocksPerStorm);
    }

    @Test
    void spawnAndDestructionSettingsHaveExplicitSafeBounds() {
        WeatherConfig config = new WeatherConfig();

        assertTrue(config.supercellSpawnChance > 0.0);
        assertTrue(config.squallSpawnChance > 0.0);
        assertTrue(config.tornadoSpawnChance > 0.0);
        assertTrue(config.cycloneSpawnChance > 0.0);
        assertTrue(config.stormStrengtheningChance >= 0.0 && config.stormStrengtheningChance <= 1.0);
        assertTrue(config.maxDamageableBlockHardness <= 3.0);
        assertFalse(config.weatherBlockDamage);
    }
}
