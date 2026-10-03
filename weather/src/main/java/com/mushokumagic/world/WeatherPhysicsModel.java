package com.mushokumagic.world;

/** Pure calculations for bounded, local weather forces and spell interactions. */
public final class WeatherPhysicsModel {
    private static final double MAX_WIND_SPEED = 1.25;
    private static final double MAX_HORIZONTAL_DRIFT = 4.0;
    private static final double MAX_VERTICAL_DRIFT = 2.0;

    private WeatherPhysicsModel() {
    }

    /** Fades storm intensity over the outer 18% of its sector without a hard border. */
    public static double sectorEdgeFalloff(double normalizedDistance) {
        double distance = WeatherPhysicsModel.clamp(normalizedDistance, 0.0, 1.0);
        if (distance <= 0.82) {
            return 1.0;
        }
        double amount = (distance - 0.82) / 0.18;
        return 1.0 - amount * amount * (3.0 - 2.0 * amount);
    }

    public static Wind combineWind(
            double regionalX,
            double regionalZ,
            double regionalStrength,
            double stormX,
            double stormZ,
            double stormStrength,
            double stormInfluence,
            double gustX,
            double gustZ,
            double verticalLift) {
        double stormWeight = WeatherPhysicsModel.clamp(stormInfluence, 0.0, 1.0);
        double regionalWeight = 1.0 - stormWeight * 0.55;
        double x = regionalX * Math.max(0.0, regionalStrength) * regionalWeight
                + stormX * Math.max(0.0, stormStrength) * stormWeight
                + gustX;
        double z = regionalZ * Math.max(0.0, regionalStrength) * regionalWeight
                + stormZ * Math.max(0.0, stormStrength) * stormWeight
                + gustZ;
        double magnitude = Math.hypot(x, z);
        if (magnitude < 1.0E-8) {
            return new Wind(1.0, 0.0, 0.0, WeatherPhysicsModel.clamp(verticalLift, 0.0, MAX_WIND_SPEED));
        }
        return new Wind(
                x / magnitude,
                z / magnitude,
                Math.min(MAX_WIND_SPEED, magnitude),
                WeatherPhysicsModel.clamp(verticalLift, 0.0, MAX_WIND_SPEED));
    }

    /** Moisture builds gradually in exposed rain rather than switching on instantly. */
    public static double wetnessAfterRain(double wetness, double precipitation, int elapsedTicks) {
        double safeWetness = WeatherPhysicsModel.clamp(wetness, 0.0, 1.0);
        double rain = WeatherPhysicsModel.clamp(precipitation, 0.0, 1.0);
        return WeatherPhysicsModel.clamp(safeWetness + rain * Math.max(0, elapsedTicks) / 240.0, 0.0, 1.0);
    }

    /** Warm, dry air removes moisture faster than cold air. */
    public static double wetnessAfterDrying(double wetness, double temperature, int elapsedTicks) {
        double safeWetness = WeatherPhysicsModel.clamp(wetness, 0.0, 1.0);
        double warmth = Math.max(0.0, temperature - 0.3);
        double dryingPerTick = 0.00035 + warmth * 0.00045;
        return WeatherPhysicsModel.clamp(safeWetness - dryingPerTick * Math.max(0, elapsedTicks), 0.0, 1.0);
    }

    public static double fireDamageMultiplier(double precipitation, double wetness) {
        return WeatherPhysicsModel.clamp(
                1.0 - WeatherPhysicsModel.clamp(precipitation, 0.0, 1.0) * 0.14
                        - WeatherPhysicsModel.clamp(wetness, 0.0, 1.0) * 0.12,
                0.70,
                1.0);
    }

    public static double fireDurationMultiplier(double precipitation, double wetness) {
        return WeatherPhysicsModel.clamp(
                1.0 - WeatherPhysicsModel.clamp(precipitation, 0.0, 1.0) * 0.75
                        - WeatherPhysicsModel.clamp(wetness, 0.0, 1.0) * 0.65,
                0.05,
                1.0);
    }

    public static double ignitionMultiplier(double precipitation, double wetness) {
        return WeatherPhysicsModel.clamp(
                1.0 - WeatherPhysicsModel.clamp(precipitation, 0.0, 1.0) * 0.88
                        - WeatherPhysicsModel.clamp(wetness, 0.0, 1.0) * 0.4,
                0.0,
                1.0);
    }

    /**
     * Small, bounded elemental modifiers let the same ambient conditions affect magic
     * without replacing wand power or the spell's configured damage.
     */
    public static double elementalPowerMultiplier(
            String element,
            double humidity,
            double precipitation,
            double temperature,
            double windStrength,
            double targetWetness) {
        double wetAir = WeatherPhysicsModel.clamp(humidity, 0.0, 1.0);
        double rain = WeatherPhysicsModel.clamp(precipitation, 0.0, 1.0);
        double wetTarget = WeatherPhysicsModel.clamp(targetWetness, 0.0, 1.0);
        double wind = WeatherPhysicsModel.clamp(windStrength, 0.0, MAX_WIND_SPEED);
        double scale = switch (element == null ? "" : element) {
            case "fire" -> WeatherPhysicsModel.fireDamageMultiplier(rain, wetTarget);
            case "water" -> 1.0 + wetAir * 0.05 + rain * 0.07;
            case "ice" -> 1.0 + Math.max(0.0, 0.2 - temperature) * 0.12;
            case "wind" -> 1.0 + wind * 0.08;
            default -> 1.0;
        };
        return WeatherPhysicsModel.clamp(scale, 0.70, 1.15);
    }

    public static Drift projectileDrift(
            double distance,
            String spellId,
            double windX,
            double windZ,
            double windStrength,
            double verticalLift) {
        double response = WeatherPhysicsModel.projectileWindResponse(spellId);
        if (response <= 0.0 || !Double.isFinite(distance) || distance <= 0.0) {
            return new Drift(0.0, 0.0, 0.0);
        }
        double horizontalDistance = Math.min(
                MAX_HORIZONTAL_DRIFT,
                Math.min(128.0, distance) * WeatherPhysicsModel.clamp(windStrength, 0.0, MAX_WIND_SPEED) * response);
        double directionLength = Math.hypot(windX, windZ);
        double dx = directionLength > 1.0E-8 ? windX / directionLength * horizontalDistance : 0.0;
        double dz = directionLength > 1.0E-8 ? windZ / directionLength * horizontalDistance : 0.0;
        double dy = WeatherPhysicsModel.clamp(verticalLift, 0.0, MAX_WIND_SPEED)
                * Math.min(128.0, distance) * response * 0.55;
        dy = WeatherPhysicsModel.clamp(dy, 0.0, MAX_VERTICAL_DRIFT);
        return new Drift(dx, dy, dz);
    }

    public static double projectileWindResponse(String spellId) {
        if (spellId == null) {
            return 0.0;
        }
        return switch (spellId) {
            case "fire_bolt" -> 0.045;
            case "water_ball" -> 0.036;
            case "ice_needle" -> 0.032;
            case "explosive_fireball" -> 0.024;
            case "water_cannon" -> 0.018;
            case "stone_ball" -> 0.009;
            default -> 0.0;
        };
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public record Wind(double x, double z, double strength, double verticalLift) {
    }

    public record Drift(double x, double y, double z) {
    }
}
