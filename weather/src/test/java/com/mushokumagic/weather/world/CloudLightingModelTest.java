package com.mushokumagic.weather.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CloudLightingModelTest {
    @Test
    void mapsWorldTimeToDaylightAndSunsetPhases() {
        CloudLightingModel.Lighting dawn = CloudLightingModel.atTime(0.0);
        CloudLightingModel.Lighting noon = CloudLightingModel.atTime(6_000.0);
        CloudLightingModel.Lighting dusk = CloudLightingModel.atTime(12_000.0);
        CloudLightingModel.Lighting midnight = CloudLightingModel.atTime(18_000.0);

        assertEquals(1.0f, dawn.twilight(), 1.0E-6f);
        assertEquals(1.0f, dusk.twilight(), 1.0E-6f);
        assertEquals(1.0f, noon.daylight(), 1.0E-6f);
        assertEquals(0.0f, noon.twilight(), 1.0E-6f);
        assertEquals(0.20f, midnight.daylight(), 1.0E-6f);
        assertEquals(0.0f, midnight.twilight(), 1.0E-6f);
        assertTrue(noon.daylight() > midnight.daylight());
    }

    @Test
    void wrapsLongAndNegativeDayTimesIntoTheSameLightingCycle() {
        CloudLightingModel.Lighting morning = CloudLightingModel.atTime(1_500.0);
        CloudLightingModel.Lighting nextMorning = CloudLightingModel.atTime(25_500.0);
        CloudLightingModel.Lighting previousMorning = CloudLightingModel.atTime(-22_500.0);

        assertEquals(morning.daylight(), nextMorning.daylight(), 1.0E-6f);
        assertEquals(morning.twilight(), nextMorning.twilight(), 1.0E-6f);
        assertEquals(morning.daylight(), previousMorning.daylight(), 1.0E-6f);
        assertEquals(morning.twilight(), previousMorning.twilight(), 1.0E-6f);
    }
}
