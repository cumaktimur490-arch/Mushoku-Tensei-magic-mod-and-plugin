package com.mushokumagic.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeatherPhysicsModelTest {
    @Test
    void stormWindCombinesSmoothlyAndRemainsBounded() {
        WeatherPhysicsModel.Wind calm = WeatherPhysicsModel.combineWind(
                1.0, 0.0, 0.4,
                0.0, 1.0, 0.8,
                0.0,
                0.0, 0.0,
                0.0);
        WeatherPhysicsModel.Wind storm = WeatherPhysicsModel.combineWind(
                1.0, 0.0, 0.4,
                0.0, 1.0, 0.8,
                1.0,
                0.0, 0.0,
                0.6);

        assertEquals(1.0, calm.x(), 1.0E-12);
        assertEquals(0.4, calm.strength(), 1.0E-12);
        assertTrue(storm.z() > storm.x());
        assertEquals(0.6, storm.verticalLift(), 0.0);
        assertTrue(storm.strength() <= 1.25);
    }

    @Test
    void stormIntensityFadesAtTheSectorEdgeWithoutASeam() {
        assertEquals(1.0, WeatherPhysicsModel.sectorEdgeFalloff(0.5), 0.0);
        assertEquals(1.0, WeatherPhysicsModel.sectorEdgeFalloff(0.82), 0.0);
        assertEquals(0.5, WeatherPhysicsModel.sectorEdgeFalloff(0.91), 1.0E-12);
        assertEquals(0.0, WeatherPhysicsModel.sectorEdgeFalloff(1.0), 0.0);
    }

    @Test
    void exposedRainBuildsWetnessAndWarmAirDriesItGradually() {
        double wet = WeatherPhysicsModel.wetnessAfterRain(0.0, 1.0, 120);
        double dryCold = WeatherPhysicsModel.wetnessAfterDrying(wet, 0.0, 20);
        double dryWarm = WeatherPhysicsModel.wetnessAfterDrying(wet, 1.2, 20);

        assertEquals(0.5, wet, 0.0);
        assertTrue(dryCold < wet);
        assertTrue(dryWarm < dryCold);
        assertEquals(1.0, WeatherPhysicsModel.wetnessAfterRain(0.95, 1.0, 240), 0.0);
        assertEquals(0.0, WeatherPhysicsModel.wetnessAfterDrying(0.0, 2.0, 20), 0.0);
    }

    @Test
    void weatherChangesElementalPowerAndDampensFireSpreadWithoutBreakingBaseScale() {
        double clearFire = WeatherPhysicsModel.elementalPowerMultiplier("fire", 0.2, 0.0, 0.6, 0.2, 0.0);
        double stormFire = WeatherPhysicsModel.elementalPowerMultiplier("fire", 0.9, 1.0, 0.6, 1.0, 1.0);
        double stormWater = WeatherPhysicsModel.elementalPowerMultiplier("water", 0.9, 1.0, 0.6, 1.0, 0.0);
        double coldIce = WeatherPhysicsModel.elementalPowerMultiplier("ice", 0.4, 0.0, -0.5, 0.2, 0.0);

        assertEquals(1.0, clearFire, 0.0);
        assertTrue(stormFire < clearFire);
        assertTrue(stormWater > 1.0);
        assertTrue(coldIce > 1.0);
        assertTrue(stormFire >= 0.70);
        assertTrue(stormWater <= 1.15);
        assertTrue(WeatherPhysicsModel.fireDurationMultiplier(1.0, 1.0) < 0.1);
        assertTrue(WeatherPhysicsModel.ignitionMultiplier(1.0, 1.0) < 0.01);
    }

    @Test
    void lighterProjectilesDriftMoreAndCalmAirDoesNotDeflectThem() {
        WeatherPhysicsModel.Drift calm = WeatherPhysicsModel.projectileDrift(64.0, "fire_bolt", 1.0, 0.0, 0.0, 0.0);
        WeatherPhysicsModel.Drift fire = WeatherPhysicsModel.projectileDrift(64.0, "fire_bolt", 1.0, 0.0, 1.0, 0.0);
        WeatherPhysicsModel.Drift stone = WeatherPhysicsModel.projectileDrift(64.0, "stone_ball", 1.0, 0.0, 1.0, 0.0);
        WeatherPhysicsModel.Drift updraft = WeatherPhysicsModel.projectileDrift(64.0, "fire_bolt", 0.0, 1.0, 0.0, 0.5);

        assertEquals(0.0, calm.x(), 0.0);
        assertEquals(0.0, calm.z(), 0.0);
        assertTrue(fire.x() > stone.x());
        assertTrue(fire.x() <= 4.0);
        assertTrue(updraft.y() > 0.0);
        assertEquals(0.0, WeatherPhysicsModel.projectileWindResponse("earth_hedgehog"), 0.0);
    }
}
