package com.mushokumagic.world;

/**
 * Deterministic, smoothly varying weather fields used by the mod's regional weather simulation.
 * The broad and local pressure systems drift over time instead of changing at chunk borders.
 */
public final class RegionalWeatherModel {
    public static final int YEAR_LENGTH_DAYS = 96;

    private static final double TICKS_PER_DAY = 24_000.0;
    private static final double TWO_PI = Math.PI * 2.0;
    private static final long SYNOPTIC_SALT = 0x4F1BBCDCBFA54001L;
    private static final long MESOSCALE_SALT = 0x369DEA0F31A53F85L;
    private static final long LOCAL_SALT = 0x2545F4914F6CDD1DL;
    private static final long MOISTURE_SALT = 0x2C1B3C6D1A5E9B7DL;
    private static final long WIND_X_SALT = 0x1B03738712FAD5C9L;
    private static final long WIND_Z_SALT = 0x6A09E667F3BCC909L;
    private static final long THUNDER_SALT = 0x3C6EF372FE94F82AL;

    private RegionalWeatherModel() {
    }

    public static WeatherState sample(
            long worldSeed,
            double x,
            double y,
            double z,
            long worldTime,
            float biomeTemperature,
            boolean biomeHasPrecipitation) {
        double days = (double)worldTime / TICKS_PER_DAY;

        // Large fronts move slowly; smaller pressure systems add local variation.
        double synoptic = RegionalWeatherModel.noise(
                worldSeed ^ SYNOPTIC_SALT,
                x + days * 72.0,
                z - days * 46.0,
                1_680.0);
        double mesoscale = RegionalWeatherModel.noise(
                worldSeed ^ MESOSCALE_SALT,
                x - days * 118.0,
                z + days * 79.0,
                760.0);
        double local = RegionalWeatherModel.noise(
                worldSeed ^ LOCAL_SALT,
                x + days * 165.0,
                z + days * 93.0,
                360.0);
        double front = synoptic * 0.58 + mesoscale * 0.27 + local * 0.15;
        double moistureNoise = RegionalWeatherModel.noise(
                worldSeed ^ MOISTURE_SALT,
                x - days * 41.0,
                z + days * 57.0,
                1_320.0);

        double humidityBase = biomeHasPrecipitation ? 0.52 : 0.12;
        double dryAir = Math.max(0.0, (double)biomeTemperature - 0.85) * 0.18;
        double humidity = RegionalWeatherModel.clamp(humidityBase + front * 0.17 + moistureNoise * 0.20 - dryAir, 0.0, 1.0);
        double pressure = RegionalWeatherModel.clamp(0.52 - synoptic * 0.27 - mesoscale * 0.14 - local * 0.08, 0.0, 1.0);
        double cloudCover = RegionalWeatherModel.clamp(
                0.10 + humidity * 0.28 + (1.0 - pressure) * 0.52 + local * 0.04,
                0.0,
                1.0);
        double precipitationPotential = humidity * 0.56 + (1.0 - pressure) * 0.44;
        double precipitationIntensity = biomeHasPrecipitation
                ? RegionalWeatherModel.smoothStep(0.61, 0.79, precipitationPotential)
                : 0.0;

        double yearPhase = RegionalWeatherModel.positiveModulo(days, YEAR_LENGTH_DAYS) / YEAR_LENGTH_DAYS;
        double dayPhase = RegionalWeatherModel.positiveModulo((double)worldTime, TICKS_PER_DAY) / TICKS_PER_DAY;
        double seasonalTemperature = 0.24 * Math.sin(TWO_PI * yearPhase - Math.PI / 2.0);
        double dailyTemperature = 0.035 * Math.sin(TWO_PI * dayPhase - Math.PI / 2.0);
        double altitudeCooling = Math.max(0.0, y - 64.0) * 0.0018;
        double temperature = biomeTemperature + seasonalTemperature + dailyTemperature - altitudeCooling;

        double windX = RegionalWeatherModel.noise(
                worldSeed ^ WIND_X_SALT,
                x + days * 138.0,
                z - days * 96.0,
                720.0);
        double windZ = RegionalWeatherModel.noise(
                worldSeed ^ WIND_Z_SALT,
                x - days * 103.0,
                z + days * 151.0,
                720.0);
        double windLength = Math.hypot(windX, windZ);
        if (windLength < 1.0E-6) {
            windX = 1.0;
            windZ = 0.0;
            windLength = 1.0;
        }
        windX /= windLength;
        windZ /= windLength;
        double windStrength = RegionalWeatherModel.clamp(
                0.18 + Math.min(1.0, windLength) * 0.48 + (1.0 - pressure) * 0.28,
                0.12,
                1.0);

        Precipitation precipitation = Precipitation.NONE;
        if (precipitationIntensity > 0.02) {
            precipitation = temperature <= 0.15 ? Precipitation.SNOW : Precipitation.RAIN;
        }
        double thunderPotential = RegionalWeatherModel.noise(
                worldSeed ^ THUNDER_SALT,
                x + days * 211.0,
                z - days * 167.0,
                520.0);
        boolean thunderstorm = precipitation == Precipitation.RAIN
                && precipitationIntensity >= 0.76
                && temperature > 0.08
                && thunderPotential > 0.35;

        return new WeatherState(
                pressure,
                humidity,
                cloudCover,
                precipitationIntensity,
                temperature,
                windX,
                windZ,
                windStrength,
                precipitation,
                thunderstorm);
    }

    private static double noise(long seed, double x, double z, double scale) {
        double gridX = x / scale;
        double gridZ = z / scale;
        int x0 = (int)Math.floor(gridX);
        int z0 = (int)Math.floor(gridZ);
        double blendX = RegionalWeatherModel.smooth(gridX - x0);
        double blendZ = RegionalWeatherModel.smooth(gridZ - z0);
        double southWest = RegionalWeatherModel.lattice(seed, x0, z0);
        double southEast = RegionalWeatherModel.lattice(seed, x0 + 1, z0);
        double northWest = RegionalWeatherModel.lattice(seed, x0, z0 + 1);
        double northEast = RegionalWeatherModel.lattice(seed, x0 + 1, z0 + 1);
        double south = RegionalWeatherModel.lerp(southWest, southEast, blendX);
        double north = RegionalWeatherModel.lerp(northWest, northEast, blendX);
        return RegionalWeatherModel.lerp(south, north, blendZ);
    }

    private static double lattice(long seed, int x, int z) {
        long value = seed + (long)x * 0x9E3779B97F4A7C15L + (long)z * 0xC2B2AE3D27D4EB4FL;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (double)(value >>> 11) * 0x1.0p-52 - 1.0;
    }

    private static double smooth(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double smoothStep(double lower, double upper, double value) {
        double amount = RegionalWeatherModel.clamp((value - lower) / (upper - lower), 0.0, 1.0);
        return RegionalWeatherModel.smooth(amount);
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double positiveModulo(double value, double divisor) {
        return value - Math.floor(value / divisor) * divisor;
    }

    public enum Precipitation {
        NONE,
        RAIN,
        SNOW
    }

    public record WeatherState(
            double pressure,
            double humidity,
            double cloudCover,
            double precipitationIntensity,
            double temperature,
            double windX,
            double windZ,
            double windStrength,
            Precipitation precipitation,
            boolean thunderstorm) {
        public WeatherKind kind() {
            if (this.thunderstorm) {
                return WeatherKind.THUNDERSTORM;
            }
            if (this.precipitation == Precipitation.SNOW) {
                return WeatherKind.SNOW;
            }
            if (this.precipitation == Precipitation.RAIN) {
                return this.precipitationIntensity >= 0.68 ? WeatherKind.HEAVY_RAIN : WeatherKind.RAIN;
            }
            return this.cloudCover >= 0.62 ? WeatherKind.CLOUDY : WeatherKind.CLEAR;
        }
    }

    public enum WeatherKind {
        CLEAR,
        CLOUDY,
        RAIN,
        HEAVY_RAIN,
        SNOW,
        THUNDERSTORM
    }
}
