package com.mushokumagic.weather.world;

/** Pure hazard-selection, evolution, and force calculations for localized severe weather. */
public final class SevereWeatherModel {
    public static final SpawnChances DEFAULT_SPAWN_CHANCES = new SpawnChances(
            1.0, 0.16, 0.22, 0.055, 0.025, 0.58, 0.22);

    private SevereWeatherModel() {
    }

    public static Kind chooseKind(
            RegionalWeatherModel.WeatherState weather,
            float biomeTemperature,
            boolean biomeHasPrecipitation,
            double roll) {
        return chooseKind(weather, biomeTemperature, biomeHasPrecipitation, roll, DEFAULT_SPAWN_CHANCES);
    }

    public static Kind chooseKind(
            RegionalWeatherModel.WeatherState weather,
            float biomeTemperature,
            boolean biomeHasPrecipitation,
            double roll,
            SpawnChances chances) {
        if (weather == null || !Double.isFinite(roll)) {
            return Kind.NONE;
        }
        SpawnChances safeChances = chances == null ? DEFAULT_SPAWN_CHANCES : chances;
        double chance = clamp(roll, 0.0, 1.0);
        double temperature = weather.temperature();

        if (weather.thunderstorm() && weather.precipitationIntensity() >= 0.70) {
            if (temperature >= 0.82
                    && weather.pressure() <= 0.50
                    && weather.humidity() >= 0.76
                    && weather.windStrength() >= 0.55
                    && chance < safeChances.threshold(safeChances.cycloneChance())) {
                return Kind.HURRICANE;
            }
            if (temperature > 0.30
                    && weather.humidity() >= 0.58
                    && weather.windStrength() >= 0.62
                    && chance < safeChances.threshold(safeChances.tornadoChance())) {
                return Kind.TORNADO;
            }

            double rotationSupport = clamp(
                    0.35
                            + Math.max(0.0, weather.windStrength() - 0.45) * 0.80
                            + Math.max(0.0, weather.humidity() - 0.65) * 0.35
                            + Math.max(0.0, 0.62 - weather.pressure()) * 0.25,
                    0.40,
                    1.45);
            if (temperature >= 0.25
                    && weather.humidity() >= 0.66
                    && weather.windStrength() >= 0.52
                    && chance < safeChances.threshold(safeChances.supercellChance() * rotationSupport)) {
                return Kind.SUPERCELL;
            }
            double gustSupport = clamp(0.60 + weather.windStrength() * 0.55, 0.60, 1.10);
            if (temperature > 0.15
                    && weather.humidity() >= 0.50
                    && weather.windStrength() >= 0.60
                    && chance < safeChances.threshold(safeChances.squallChance() * gustSupport)) {
                return Kind.SQUALL;
            }
            if (temperature >= 0.12
                    && temperature <= 0.42
                    && chance < safeChances.threshold(safeChances.hailChance())) {
                return Kind.HAIL;
            }
        }

        if (!biomeHasPrecipitation
                && biomeTemperature >= 1.0f
                && temperature >= 0.9
                && weather.humidity() <= 0.30
                && weather.windStrength() >= 0.70
                && chance < safeChances.threshold(safeChances.sandstormChance())) {
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
            case SUPERCELL -> clamp(
                    0.38 + weather.precipitationIntensity() * 0.24 + weather.humidity() * 0.16
                            + weather.windStrength() * 0.18 + Math.max(0.0, 0.58 - weather.pressure()) * 0.16,
                    0.40,
                    1.0);
            case SQUALL -> clamp(
                    0.36 + weather.precipitationIntensity() * 0.24 + weather.windStrength() * 0.30
                            + weather.humidity() * 0.12,
                    0.40,
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

    /** Optional, bounded mid-life intensification used by configurable severe systems. */
    public static double strengtheningMultiplier(long ageTicks, long durationTicks, double boost) {
        if (durationTicks <= 0L || ageTicks < 0L || ageTicks >= durationTicks || boost <= 0.0) {
            return 1.0;
        }
        double progress = clamp((double)ageTicks / durationTicks, 0.0, 1.0);
        double growth = smoothStep(0.12, 0.48, progress);
        double weakening = 1.0 - smoothStep(0.68, 0.94, progress);
        return 1.0 + clamp(boost, 0.0, 0.75) * growth * weakening;
    }

    public static DevelopmentStage developmentStage(long ageTicks, long durationTicks) {
        if (durationTicks <= 0L || ageTicks < 0L || ageTicks >= durationTicks) {
            return DevelopmentStage.DISSIPATED;
        }
        double progress = (double)ageTicks / durationTicks;
        if (progress < 0.16) {
            return DevelopmentStage.FORMING;
        }
        if (progress < 0.78) {
            return DevelopmentStage.MATURE;
        }
        return DevelopmentStage.DISSIPATING;
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
            case SUPERCELL -> {
                double windX = -nz * 0.72 - nx * 0.20 + tx * 0.14;
                double windZ = nx * 0.72 - nz * 0.20 + tz * 0.14;
                double length = Math.hypot(windX, windZ);
                if (length > 1.0E-8) {
                    windX /= length;
                    windZ /= length;
                }
                double radial = clamp(radialInfluence, 0.0, 1.0);
                yield new Forces(
                        windX,
                        windZ,
                        clamp(0.42 + safeIntensity * 0.82, 0.0, 1.25),
                        clamp(0.12 + safeIntensity * 0.46 + radial * 0.08, 0.0, 0.90),
                        clamp(0.62 + safeIntensity * 0.32, 0.0, 1.0),
                        safeIntensity > 0.92 && radial > 0.58,
                        safeIntensity * 0.18);
            }
            case SQUALL -> {
                double windX = tx * 0.92 - nz * 0.20;
                double windZ = tz * 0.92 + nx * 0.20;
                double length = Math.hypot(windX, windZ);
                if (length > 1.0E-8) {
                    windX /= length;
                    windZ /= length;
                }
                yield new Forces(
                        windX,
                        windZ,
                        clamp(0.55 + safeIntensity * 0.78, 0.0, 1.25),
                        0.03 + safeIntensity * 0.10,
                        clamp(0.58 + safeIntensity * 0.38, 0.0, 1.0),
                        false,
                        0.0);
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

    private static double smooth(double value) {
        double amount = clamp(value, 0.0, 1.0);
        return amount * amount * (3.0 - 2.0 * amount);
    }

    private static double smoothStep(double lower, double upper, double value) {
        double amount = clamp((value - lower) / (upper - lower), 0.0, 1.0);
        return smooth(amount);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public enum Kind {
        NONE,
        SUPERCELL,
        SQUALL,
        HURRICANE,
        HAIL,
        TORNADO,
        SANDSTORM
    }

    public enum DevelopmentStage {
        FORMING,
        MATURE,
        DISSIPATING,
        DISSIPATED
    }

    public record SpawnChances(
            double globalMultiplier,
            double supercellChance,
            double squallChance,
            double tornadoChance,
            double cycloneChance,
            double hailChance,
            double sandstormChance) {
        public SpawnChances {
            globalMultiplier = clamp(globalMultiplier, 0.0, 5.0);
            supercellChance = clamp(supercellChance, 0.0, 1.0);
            squallChance = clamp(squallChance, 0.0, 1.0);
            tornadoChance = clamp(tornadoChance, 0.0, 1.0);
            cycloneChance = clamp(cycloneChance, 0.0, 1.0);
            hailChance = clamp(hailChance, 0.0, 1.0);
            sandstormChance = clamp(sandstormChance, 0.0, 1.0);
        }

        public double threshold(double configuredChance) {
            return clamp(configuredChance * this.globalMultiplier, 0.0, 1.0);
        }
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
