package com.mushokumagic.spell;

/** Smooth phase curves for the charge, release, and impact choreography of a spell. */
final class SpellVisualMotion {
    static final double RELEASE_START = 0.68;

    private SpellVisualMotion() {
    }

    static double releaseProgress(double castProgress) {
        double normalized = (SpellVisualMotion.clamp01(castProgress) - RELEASE_START) / (1.0 - RELEASE_START);
        return SpellVisualMotion.clamp01(normalized);
    }

    static double chargePulse(double chargeProgress) {
        return Math.sin(Math.PI * SpellVisualMotion.clamp01(chargeProgress));
    }

    static double easeInOut(double progress) {
        double amount = SpellVisualMotion.clamp01(progress);
        return amount * amount * (3.0 - 2.0 * amount);
    }

    static double easeOut(double progress) {
        double amount = SpellVisualMotion.clamp01(progress);
        return 1.0 - (1.0 - amount) * (1.0 - amount);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
