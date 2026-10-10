package com.mushokumagic.spell;

import net.minecraft.class_243;

/** Smooth phase curves and geometry for spell charge, release, and impact choreography. */
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

    /** Returns an offset in the plane perpendicular to the normalized travel direction. */
    static class_243 ribbonOffset(class_243 direction, double angle, double radius) {
        double dx = direction.method_10216();
        double dy = direction.method_10214();
        double dz = direction.method_10215();
        double directionLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (directionLength < 1.0E-6 || radius <= 0.0) {
            return new class_243(0.0, 0.0, 0.0);
        }
        dx /= directionLength;
        dy /= directionLength;
        dz /= directionLength;
        double referenceX = Math.abs(dy) > 0.9 ? 1.0 : 0.0;
        double referenceY = Math.abs(dy) > 0.9 ? 0.0 : 1.0;
        double sideX = -dz * referenceY;
        double sideY = dz * referenceX;
        double sideZ = dx * referenceY - dy * referenceX;
        double sideLength = Math.sqrt(sideX * sideX + sideY * sideY + sideZ * sideZ);
        sideX /= sideLength;
        sideY /= sideLength;
        sideZ /= sideLength;
        double upX = dy * sideZ - dz * sideY;
        double upY = dz * sideX - dx * sideZ;
        double upZ = dx * sideY - dy * sideX;
        double cosine = Math.cos(angle) * radius;
        double sine = Math.sin(angle) * radius;
        return new class_243(sideX * cosine + upX * sine,
                sideY * cosine + upY * sine,
                sideZ * cosine + upZ * sine);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
