package com.mushokumagic.weather.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegionalWeatherModelTest {
    @Test
    void weatherIsDeterministicForAWorldAndChangesAcrossMovingFronts() {
        RegionalWeatherModel.WeatherState first = RegionalWeatherModel.sample(
                0x1234ABCDL, 128.0, 70.0, -256.0, 42_000L, 0.8f, true);
        RegionalWeatherModel.WeatherState repeated = RegionalWeatherModel.sample(
                0x1234ABCDL, 128.0, 70.0, -256.0, 42_000L, 0.8f, true);
        RegionalWeatherModel.WeatherState otherRegion = RegionalWeatherModel.sample(
                0x1234ABCDL, 2_048.0, 70.0, 1_536.0, 42_000L, 0.8f, true);

        assertEquals(first, repeated);
        assertTrue(Math.abs(first.cloudCover() - otherRegion.cloudCover()) > 0.02
                || Math.abs(first.precipitationIntensity() - otherRegion.precipitationIntensity()) > 0.02);
    }

    @Test
    void frontsBlendSmoothlyInsteadOfChangingAtRegionBorders() {
        RegionalWeatherModel.WeatherState west = RegionalWeatherModel.sample(
                7L, 319.5, 72.0, 640.0, 1_234_567L, 0.8f, true);
        RegionalWeatherModel.WeatherState east = RegionalWeatherModel.sample(
                7L, 320.5, 72.0, 640.0, 1_234_567L, 0.8f, true);

        assertTrue(Math.abs(west.cloudCover() - east.cloudCover()) < 0.02);
        assertTrue(Math.abs(west.precipitationIntensity() - east.precipitationIntensity()) < 0.02);
        assertTrue(Math.abs(west.windStrength() - east.windStrength()) < 0.02);
    }

    @Test
    void dryBiomesNeverProduceRainOrSnow() {
        for (int x = -2_000; x <= 2_000; x += 250) {
            for (int z = -2_000; z <= 2_000; z += 250) {
                RegionalWeatherModel.WeatherState state = RegionalWeatherModel.sample(
                        99L, x, 80.0, z, 100_000L, 1.5f, false);
                assertEquals(0.0, state.precipitationIntensity(), 0.0);
                assertEquals(RegionalWeatherModel.Precipitation.NONE, state.precipitation());
                assertFalse(state.thunderstorm());
            }
        }
    }

    @Test
    void coldAndHighAltitudePrecipitationBecomesSnow() {
        boolean foundSnow = false;
        for (int x = -4_000; x <= 4_000 && !foundSnow; x += 240) {
            for (int z = -4_000; z <= 4_000; z += 240) {
                RegionalWeatherModel.WeatherState state = RegionalWeatherModel.sample(
                        123L, x, 180.0, z, 0L, 0.25f, true);
                if (state.precipitation() == RegionalWeatherModel.Precipitation.SNOW) {
                    foundSnow = true;
                    break;
                }
            }
        }
        assertTrue(foundSnow, "Cold regions should receive snow when a wet front passes");
    }

    @Test
    void warmWetLowPressureFrontsCanProduceThunderstorms() {
        RegionalWeatherModel.WeatherState storm = RegionalWeatherModel.sample(
                42L, -11_600.0, 80.0, 240.0, 0L, 0.8f, true);

        assertTrue(storm.thunderstorm());
        assertEquals(RegionalWeatherModel.WeatherKind.THUNDERSTORM, storm.kind());
        assertTrue(storm.precipitationIntensity() >= 0.76);
    }

    @Test
    void manualWeatherPresetsForceClearRainThunderAndSnowLocally() {
        RegionalWeatherModel.WeatherState base = RegionalWeatherModel.sample(
                55L, 128.0, 70.0, -256.0, 42_000L, 0.8f, true);
        RegionalWeatherModel.WeatherState clear = RegionalWeatherModel.applyPreset(
                base, RegionalWeatherModel.ManualPreset.CLEAR);
        RegionalWeatherModel.WeatherState rain = RegionalWeatherModel.applyPreset(
                base, RegionalWeatherModel.ManualPreset.RAIN);
        RegionalWeatherModel.WeatherState thunder = RegionalWeatherModel.applyPreset(
                base, RegionalWeatherModel.ManualPreset.THUNDER);
        RegionalWeatherModel.WeatherState snow = RegionalWeatherModel.applyPreset(
                base, RegionalWeatherModel.ManualPreset.SNOW);

        assertEquals(RegionalWeatherModel.Precipitation.NONE, clear.precipitation());
        assertEquals(0.0, clear.precipitationIntensity(), 0.0);
        assertEquals(RegionalWeatherModel.Precipitation.RAIN, rain.precipitation());
        assertTrue(rain.precipitationIntensity() > 0.7);
        assertTrue(thunder.thunderstorm());
        assertEquals(RegionalWeatherModel.Precipitation.SNOW, snow.precipitation());
        assertTrue(snow.temperature() <= 0.15);
    }

    @Test
    void seasonalTemperatureAndWindAreBounded() {
        RegionalWeatherModel.WeatherState winter = RegionalWeatherModel.sample(
                321L, 400.0, 80.0, -700.0, 0L, 0.8f, true);
        RegionalWeatherModel.WeatherState summer = RegionalWeatherModel.sample(
                321L, 400.0, 80.0, -700.0, 48L * 24_000L, 0.8f, true);

        assertTrue(summer.temperature() - winter.temperature() > 0.40);
        assertTrue(winter.windStrength() >= 0.12 && winter.windStrength() <= 1.0);
        assertEquals(1.0, Math.hypot(winter.windX(), winter.windZ()), 1.0E-9);
    }
}
