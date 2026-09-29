package com.mushokumagic.world;

/** Pure hazard-selection and lifecycle calculations for localized severe weather. */
public final class SevereWeatherModel {
    private SevereWeatherModel() {
    }

    public static Kind chooseKind(
            RegionalWeatherModel.WeatherState weather,
            float biomeTemperature,
            boolean biomeHasPrecipitation,
            double roll) {
        if (weather == null || !Double.isFinite(roll)) {
            return Kind.NONE;
        }
        double chance = clamp(roll, 0.0, 1.0);
        double temperature = weather.temperature();

        if (weather.thunderstorm() && weather.precipitationIntensity() >= 0.72) {
            if (temperature >= 0.82
                    && weather.pressure() <= 0.50
                    && weather.humidity() >= 0.76
                    && weather.windStrength() >= 0.55
                    && chance < 0.025) {
                return Kind.HURRICANE;
            }
            if (temperature >= 0.12 && temperature <= 0.42 && chance < 0.58) {
                return Kind.HAIL;
            }
            if (temperature > 0.30
                    && weather.humidity() >= 0.58
                    && weather.windStrength() >= 0.62
                    && chance < 0.055) {
                return Kind.TORNADO;
            }
        }

        if (!biomeHasPrecipitation
                && biomeTemperature >= 1.0f
                && temperature >= 0.9
                && weather.humidity() <= 0.30
                && weather.windStrength() >= 0.70
                && chance < 0.22) {
            return Kind.SANDSTORM;
        }
        return Kind.NONE;
    }

    public static double initialIntensity(Kind kind, RegionalWeatherModel.WeatherState weather) {
        if (kind == null || kind == Kind.NONE || weather == null) {
            return 0.0;
        }
        return switch (kind) {
            case HURRICANE -> clamp(
                    0.46 + Math.max(0.0, 0.72 - weather.pressure()) * 0.34 + weather.windStrength() * 0.22,
                    0.45,
                    1.0);
            case TORNADO -> clamp(
                    0.48 + Math.max(0.0, weather.windStrength() - 0.55) * 0.7
                            + Math.max(0.0, 0.72 - weather.pressure()) * 0.3,
                    0.45,
                    1.0);
            case HAIL -> clamp(
                    0.42 + weather.precipitationIntensity() * 0.42 + weather.windStrength() * 0.12,
                    0.45,
                    1.0);
            case SANDSTORM -> clamp(
                    0.42 + Math.max(0.0, weather.windStrength() - 0.6) * 0.9,
                    0.4,
                    0.95);
            case NONE -> 0.0;
        };
    }

    /** Smooth formation, mature, and dissipation phases over the system's lifetime. */
    public static double lifecycleStrength(long ageTicks, long durationTicks) {
        if (durationTicks <= 0L || ageTicks < 0L || ageTicks >= durationTicks) {
            return 0.0;
        }
        double progress = (double)ageTicks / (double)durationTicks;
        if (progress < 0.16) {
            return smooth(progress / 0.16);
        }
        if (progress > 0.78) {
            return 1.0 - smooth((progress - 0.78) / 0.22);
        }
        return 1.0;
    }

    /** Smooth falloff from the storm core to its finite outer radius. */
    public static double radialInfluence(double distance, double radius) {
        if (!Double.isFinite(distance) || !Double.isFinite(radius) || radius <= 0.0 || distance >= radius) {
            return 0.0;
        }
        return smooth(1.0 - Math.max(0.0, distance) / radius);
    }

    public static Forces forces(
            Kind kind,
            double intensity,
            double radialInfluence,
            double offsetX,
            double offsetZ,
            double travelX,
            double travelZ) {
        double safeIntensity = clamp(intensity, 0.0, 1.0);
        double radialLength = Math.hypot(offsetX, offsetZ);
        double nx = radialLength > 1.0E-8 ? offsetX / radialLength : 0.0;
        double nz = radialLength > 1.0E-8 ? offsetZ / radialLength : 0.0;
        double travelLength = Math.hypot(travelX, travelZ);
        double tx = travelLength > 1.0E-8 ? travelX / travelLength : 1.0;
        double tz = travelLength > 1.0E-8 ? travelZ / travelLength : 0.0;

        return switch (kind == null ? Kind.NONE : kind) {
            case HURRICANE -> {
                double windX = -nz * 0.92 - nx * 0.08 + tx * 0.04;
                double windZ = nx * 0.92 - nz * 0.08 + tz * 0.04;
                double length = Math.hypot(windX, windZ);
                if (length > 1.0E-8) {
                    windX /= length;
                    windZ /= length;
                }
                yield new Forces(
                        windX,
                        windZ,
                        clamp(0.38 + safeIntensity * 0.86, 0.0, 1.25),
                        0.05 * safeIntensity,
                        0.65 + safeIntensity * 0.32,
                        false,
                        0.0);
            }
            case TORNADO -> {
                double windX = -nz * 0.82 - nx * 0.18 + tx * 0.08;
                double windZ = nx * 0.82 - nz * 0.18 + tz * 0.08;
                double length = Math.hypot(windX, windZ);
                if (length > 1.0E-8) {
                    windX /= length;
                    windZ /= length;
                }
                yield new Forces(
                        windX,
                        windZ,
                        clamp(0.28 + safeIntensity * 0.98, 0.0, 1.25),
                        clamp(safeIntensity * (0.16 + 0.58 * clamp(radialInfluence, 0.0, 1.0)), 0.0, 1.0),
                        0.55 * safeIntensity,
                        false,
                        safeIntensity);
            }
            case HAIL -> new Forces(
                    tx,
                    tz,
                    clamp(0.22 + safeIntensity * 0.72, 0.0, 1.25),
                    0.04 * safeIntensity,
                    clamp(0.60 + safeIntensity * 0.35, 0.0, 1.0),
                    true,
                    0.0);
            case SANDSTORM -> new Forces(
                    tx,
                    tz,
                    clamp(0.38 + safeIntensity * 0.78, 0.0, 1.25),
                    0.0,
                    0.0,
                    false,
                    0.0);
            case NONE -> Forces.CALM;
        };
    }

    private static double smooth(double value) {
        double amount = clamp(value, 0.0, 1.0);
        return amount * amount * (3.0 - 2.0 * amount);
    }

    /**
     * Initial smoke-particle velocity for a rising vortex. Tangential circulation
     * makes the funnel visibly rotate; inward flow gathers wisps into the core.
     */
    public static VortexFlow vortexFlow(
            double offsetX,
            double offsetZ,
            double travelX,
            double travelZ,
            double intensity,
            double updraft) {
        double safeIntensity = clamp(intensity, 0.0, 1.0);
        double radialLength = Math.hypot(offsetX, offsetZ);
        double nx = radialLength > 1.0E-8 ? offsetX / radialLength : 1.0;
        double nz = radialLength > 1.0E-8 ? offsetZ / radialLength : 0.0;
        double travelLength = Math.hypot(travelX, travelZ);
        double tx = travelLength > 1.0E-8 ? travelX / travelLength : 1.0;
        double tz = travelLength > 1.0E-8 ? travelZ / travelLength : 0.0;
        double tangential = 0.035 + safeIntensity * 0.085;
        double inward = 0.015 + safeIntensity * 0.035;
        double drift = 0.01 + safeIntensity * 0.025;
        double vertical = clamp(0.035 + safeIntensity * 0.075 + clamp(updraft, 0.0, 1.25) * 0.08, 0.02, 0.22);
        return new VortexFlow(
                -nz * tangential - nx * inward + tx * drift,
                vertical,
                nx * tangential - nz * inward + tz * drift);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public enum Kind {
        NONE,
        HURRICANE,
        HAIL,
        TORNADO,
        SANDSTORM
    }

    public record VortexFlow(double x, double y, double z) {
    }

    public record Forces(
            double windX,
            double windZ,
            double windStrength,
            double verticalLift,
            double precipitationIntensity,
            boolean hailing,
            double tornadoIntensity) {
        private static final Forces CALM = new Forces(1.0, 0.0, 0.0, 0.0, 0.0, false, 0.0);
    }
}
