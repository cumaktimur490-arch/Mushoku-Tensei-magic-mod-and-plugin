package com.mushokumagic.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SevereWeatherModelTest {
    @Test
    void warmHumidThunderstormsCanFormMovingTornadoes() {
        RegionalWeatherModel.WeatherState severe = weather(0.28, 0.80, 0.86, 0.78, 0.82, true);

        assertEquals(
                SevereWeatherModel.Kind.TORNADO,
                SevereWeatherModel.chooseKind(severe, 0.8f, true, 0.02));
        assertEquals(
                SevereWeatherModel.Kind.NONE,
                SevereWeatherModel.chooseKind(severe, 0.8f, true, 0.8));
        assertTrue(SevereWeatherModel.initialIntensity(SevereWeatherModel.Kind.TORNADO, severe) > 0.45);
    }

    @Test
    void warmLowPressureThunderstormsCanFormLargeCyclones() {
        RegionalWeatherModel.WeatherState tropicalStorm = weather(0.24, 0.9, 0.95, 0.98, 0.88, true);

        assertEquals(
                SevereWeatherModel.Kind.HURRICANE,
                SevereWeatherModel.chooseKind(tropicalStorm, 1.0f, true, 0.01));
        SevereWeatherModel.Forces forces = SevereWeatherModel.forces(
                SevereWeatherModel.Kind.HURRICANE, 0.85, 0.8, 8.0, 0.0, 0.0, 1.0);
        assertTrue(forces.windZ() > 0.8);
        assertTrue(forces.precipitationIntensity() > 0.9);
    }

    @Test
    void coldThunderstormsCanProduceHail() {
        RegionalWeatherModel.WeatherState storm = weather(0.35, 0.83, 0.9, 0.3, 0.8, true);

        assertEquals(
                SevereWeatherModel.Kind.HAIL,
                SevereWeatherModel.chooseKind(storm, 0.45f, true, 0.4));
        SevereWeatherModel.Forces hail = SevereWeatherModel.forces(
                SevereWeatherModel.Kind.HAIL, 0.8, 0.7, 4.0, 3.0, 1.0, 0.0);
        assertTrue(hail.hailing());
        assertTrue(hail.precipitationIntensity() > 0.8);
    }

    @Test
    void hotDryBiomesCanProduceDustStormsButWetBiomesCannot() {
        RegionalWeatherModel.WeatherState dryWind = weather(0.7, 0.12, 0.0, 1.2, 0.88, false);

        assertEquals(
                SevereWeatherModel.Kind.SANDSTORM,
                SevereWeatherModel.chooseKind(dryWind, 1.3f, false, 0.1));
        assertEquals(
                SevereWeatherModel.Kind.NONE,
                SevereWeatherModel.chooseKind(dryWind, 1.3f, true, 0.1));
        assertTrue(SevereWeatherModel.forces(
                SevereWeatherModel.Kind.SANDSTORM, 0.8, 0.5, 3.0, 4.0, 1.0, 0.0).windStrength() > 0.9);
    }

    @Test
    void stormLifeAndRadiusFadeSmoothly() {
        assertEquals(0.0, SevereWeatherModel.lifecycleStrength(0L, 2_000L), 0.0);
        assertEquals(1.0, SevereWeatherModel.lifecycleStrength(1_000L, 2_000L), 0.0);
        assertTrue(SevereWeatherModel.lifecycleStrength(1_900L, 2_000L) < 1.0);
        assertEquals(0.0, SevereWeatherModel.lifecycleStrength(2_000L, 2_000L), 0.0);
        assertEquals(1.0, SevereWeatherModel.radialInfluence(0.0, 40.0), 0.0);
        assertTrue(SevereWeatherModel.radialInfluence(20.0, 40.0) > 0.0);
        assertEquals(0.0, SevereWeatherModel.radialInfluence(40.0, 40.0), 0.0);
    }

    @Test
    void tornadoFlowSwirlsAndLiftsWithinItsCore() {
        SevereWeatherModel.Forces core = SevereWeatherModel.forces(
                SevereWeatherModel.Kind.TORNADO, 0.9, 1.0, 5.0, 0.0, 0.0, 1.0);

        assertTrue(core.windZ() > 0.0);
        assertTrue(core.verticalLift() > 0.6);
        assertTrue(core.tornadoIntensity() > 0.8);
        assertTrue(core.windStrength() <= 1.25);
    }

    private static RegionalWeatherModel.WeatherState weather(
            double pressure,
            double humidity,
            double precipitation,
            double temperature,
            double windStrength,
            boolean thunderstorm) {
        RegionalWeatherModel.Precipitation kind = precipitation > 0.0
                ? RegionalWeatherModel.Precipitation.RAIN
                : RegionalWeatherModel.Precipitation.NONE;
        return new RegionalWeatherModel.WeatherState(
                pressure,
                humidity,
                0.9,
                precipitation,
                temperature,
                1.0,
                0.0,
                windStrength,
                kind,
                thunderstorm);
    }
}
