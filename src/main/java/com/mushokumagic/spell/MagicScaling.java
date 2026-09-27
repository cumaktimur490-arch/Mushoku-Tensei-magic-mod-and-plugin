package com.mushokumagic.spell;

/** Shared, bounded scaling for wand-powered spell geometry and visuals. */
public final class MagicScaling {
    private static final double AREA_EXPONENT = 0.6;
    private static final double MAX_AREA_MULTIPLIER = 12.0;
    private static final double MAX_AREA_RADIUS = 32.0;
    private static final double MAX_CAST_RANGE = 128.0;

    private MagicScaling() {
    }

    /**
     * Scales area more slowly than damage: a 50x wand produces about a 10.5x
     * radius, which is roughly a 52-block-wide fire-bolt impact at its default
     * radius, without allowing custom configs to create unbounded areas.
     */
    public static double clampPower(double power) {
        return Double.isNaN(power) ? 1.0 : Math.max(0.1, Math.min(1000.0, power));
    }

    public static double areaMultiplier(double power) {
        double safePower = Double.isNaN(power) ? 1.0 : Math.max(1.0, power);
        return Math.min(MAX_AREA_MULTIPLIER, Math.pow(safePower, AREA_EXPONENT));
    }

    public static double radius(double baseRadius, double power, double modifier) {
        if (!Double.isFinite(baseRadius) || !Double.isFinite(modifier)) {
            return 0.0;
        }
        double scaled = Math.max(0.0, baseRadius)
                * areaMultiplier(power)
                * Math.max(0.0, modifier);
        return Math.min(MAX_AREA_RADIUS, scaled);
    }

    /** Adds range gradually, reaching about 87 blocks with a 50x wand. */
    public static double range(double baseRange, double power) {
        double safeBase = Double.isFinite(baseRange) ? Math.max(0.0, baseRange) : 0.0;
        double safePower = Double.isNaN(power) ? 1.0 : Math.max(1.0, power);
        return Math.min(MAX_CAST_RANGE, safeBase + Math.max(0.0, safePower - 1.0) * 0.8);
    }

    public static int intensity(double power) {
        return Math.max(1, Math.min(12, (int) Math.ceil(areaMultiplier(power))));
    }
}
