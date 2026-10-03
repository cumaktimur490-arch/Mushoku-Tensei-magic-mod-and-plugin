package com.mushokumagic.config;

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
    }
}
