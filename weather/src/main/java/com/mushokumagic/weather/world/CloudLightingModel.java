package com.mushokumagic.weather.world;

/** Small CPU-side sun/sky model for day, night, dawn, and dusk cloud light. */
final class CloudLightingModel {
    private static final double DAY_TICKS = 24_000.0;
    private static final double TWO_PI = Math.PI * 2.0;

    private CloudLightingModel() {
    }

    static Lighting atTime(double dayTime) {
        double dayCycle = dayTime % DAY_TICKS;
        if (dayCycle < 0.0) {
            dayCycle += DAY_TICKS;
        }
        double sunAngle = dayCycle / DAY_TICKS * TWO_PI;
        double sunElevation = Math.sin(sunAngle);
        float daylight = (float)(0.20 + 0.80 * Math.max(0.0, sunElevation));
        float twilight = (float)Math.max(0.0, 1.0 - Math.abs(sunElevation) / 0.34);
        return new Lighting(
                daylight,
                twilight,
                (float)Math.cos(sunAngle),
                (float)sunElevation,
                0.0f);
    }

    record Lighting(float daylight, float twilight, float sunX, float sunY, float sunZ) {
    }
}
