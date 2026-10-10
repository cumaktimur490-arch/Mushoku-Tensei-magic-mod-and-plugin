package com.mushokumagic.weather.world;

/** Small CPU-side phase model for day, night, dawn, and dusk cloud light. */
final class CloudLightingModel {
    private CloudLightingModel() {
    }

    static Lighting atTime(double dayTime) {
        double dayCycle = dayTime % 24_000.0;
        if (dayCycle < 0.0) {
            dayCycle += 24_000.0;
        }
        double sunElevation = Math.sin(dayCycle / 24_000.0 * Math.PI * 2.0);
        float daylight = (float)(0.20 + 0.80 * Math.max(0.0, sunElevation));
        float twilight = (float)Math.max(0.0, 1.0 - Math.abs(sunElevation) / 0.34);
        return new Lighting(daylight, twilight);
    }

    record Lighting(float daylight, float twilight) {
    }
}
