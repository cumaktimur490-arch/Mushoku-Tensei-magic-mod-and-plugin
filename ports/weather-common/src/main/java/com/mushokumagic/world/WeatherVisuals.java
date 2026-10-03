package com.mushokumagic.world;

import net.minecraft.class_2394;
import net.minecraft.class_2398;
import net.minecraft.class_3218;

/** Shared secondary weather effects: lightning, wind and directional precipitation. */
public final class WeatherVisuals {
    private static final class_2394 WIND_MIST = WeatherPalette.dust(0xA7C5D4, 0.38f);

    private WeatherVisuals() {
    }

    /** Builds a forked blue-white lightning bolt from connected particle segments. */
    public static void emitLightning(class_3218 level, double x, double z, double bottomY, double height) {
        double safeHeight = clamp(height, 12.0, 48.0);
        double topY = bottomY + safeHeight;
        int segments = Math.max(8, (int)Math.round(safeHeight / 2.1));
        double step = safeHeight / segments;
        double currentX = x;
        double currentZ = z;
        class_2394 boltCore = WeatherPalette.lightningCore(2.0f);
        class_2394 boltEdge = WeatherPalette.lightningCore(1.1f);

        for (int segment = 0; segment <= segments; ++segment) {
            if (segment > 0) {
                currentX += randomOffset(level, 0.92);
                currentZ += randomOffset(level, 0.92);
            }
            double y = topY - segment * step;
            level.method_14199(boltCore,
                    currentX, y, currentZ,
                    3, 0.16, step * 0.30, 0.16, 0.025);
            level.method_14199((class_2394)class_2398.field_11207,
                    currentX, y, currentZ,
                    1, 0.08, step * 0.24, 0.08, 0.018);
            if (segment % 3 == 1) {
                level.method_14199((class_2394)class_2398.field_29644,
                        currentX, y, currentZ,
                        1, 0.18, 0.24, 0.18, 0.02);
            }
            if (segment == segments / 3 || segment == (segments * 2) / 3) {
                WeatherVisuals.emitLightningBranch(level, currentX, y, currentZ, boltEdge, segment % 2 == 0 ? 1.0 : -1.0);
            }
        }

        level.method_14199(boltCore,
                currentX, bottomY + 0.7, currentZ,
                28, 2.5, 1.4, 2.5, 0.055);
        level.method_14199((class_2394)class_2398.field_11207,
                currentX, bottomY + 0.8, currentZ,
                14, 1.6, 0.9, 1.6, 0.04);
        level.method_14199((class_2394)class_2398.field_29644,
                currentX, bottomY + 1.0, currentZ,
                8, 1.1, 0.8, 1.1, 0.035);
    }

    private static void emitLightningBranch(
            class_3218 level,
            double startX,
            double startY,
            double startZ,
            class_2394 bolt,
            double side) {
        double branchX = startX;
        double branchZ = startZ;
        for (int point = 1; point <= 4; ++point) {
            branchX += side * (0.5 + level.field_9229.method_43058() * 0.8)
                    + randomOffset(level, 0.45);
            branchZ += randomOffset(level, 0.65);
            double branchY = startY - point * 1.55;
            level.method_14199(bolt,
                    branchX, branchY, branchZ,
                    2, 0.12, 0.3, 0.12, 0.018);
            if (point % 2 == 0) {
                level.method_14199((class_2394)class_2398.field_11207,
                        branchX, branchY, branchZ,
                        1, 0.08, 0.22, 0.08, 0.012);
            }
        }
    }

    /** Adds a few wind-slanted rain or snow particles with explicit velocity. */
    public static void emitDirectionalPrecipitation(
            class_3218 level,
            class_2394 particle,
            double x,
            double y,
            double z,
            int count,
            double horizontalSpread,
            double verticalSpread,
            double windX,
            double windZ,
            double windStrength,
            boolean snow) {
        int safeCount = Math.max(0, Math.min(16, count));
        if (particle == null || safeCount == 0) {
            return;
        }
        double horizontalSpeed = snow ? 0.018 + windStrength * 0.055 : 0.035 + windStrength * 0.11;
        double verticalSpeed = snow ? -0.025 : -0.24;
        double velocityX = windX * horizontalSpeed;
        double velocityZ = windZ * horizontalSpeed;
        for (int index = 0; index < safeCount; ++index) {
            double particleX = x + randomOffset(level, horizontalSpread);
            double particleY = y + level.field_9229.method_43058() * verticalSpread;
            double particleZ = z + randomOffset(level, horizontalSpread);
            level.method_14199(particle,
                    particleX, particleY, particleZ,
                    0, velocityX, verticalSpeed, velocityZ, 1.0);
        }
    }

    /** Faint drifting threads make the wind direction readable without obscuring the scene. */
    public static void emitWindThreads(
            class_3218 level,
            double x,
            double y,
            double z,
            int count,
            double horizontalSpread,
            double verticalSpread,
            double windX,
            double windZ,
            double windStrength) {
        WeatherVisuals.emitDriftingParticles(
                level,
                WIND_MIST,
                x,
                y,
                z,
                count,
                horizontalSpread,
                verticalSpread,
                windX,
                windZ,
                windStrength);
    }

    /** Emits softly colored dust or ash threads that travel with the local wind. */
    public static void emitDriftingParticles(
            class_3218 level,
            class_2394 particle,
            double x,
            double y,
            double z,
            int count,
            double horizontalSpread,
            double verticalSpread,
            double windX,
            double windZ,
            double windStrength) {
        int safeCount = Math.max(0, Math.min(12, count));
        if (particle == null || safeCount == 0 || windStrength < 0.45) {
            return;
        }
        double speed = 0.025 + windStrength * 0.085;
        for (int index = 0; index < safeCount; ++index) {
            double particleX = x + randomOffset(level, horizontalSpread);
            double particleY = y + randomOffset(level, verticalSpread);
            double particleZ = z + randomOffset(level, horizontalSpread);
            double lift = (level.field_9229.method_43058() - 0.5) * 0.012;
            level.method_14199(particle,
                    particleX, particleY, particleZ,
                    0, windX * speed, lift, windZ * speed, 1.0);
        }
    }

    /** Seeds a bounded, updraft-driven vortex without pairwise particle interactions. */
    public static void emitVortexParticles(
            class_3218 level,
            class_2394 particle,
            double centerX,
            double baseY,
            double centerZ,
            int count,
            double radius,
            double height,
            double windX,
            double windZ,
            double intensity) {
        int safeCount = Math.max(0, Math.min(12, count));
        if (particle == null || safeCount == 0 || intensity < 0.15 || radius <= 0.0 || height <= 0.0) {
            return;
        }
        double strength = clamp(intensity, 0.0, 1.0);
        for (int index = 0; index < safeCount; ++index) {
            double angle = level.field_9229.method_43058() * Math.PI * 2.0;
            double radialFraction = 0.18 + 0.82 * Math.sqrt(level.field_9229.method_43058());
            double radialDistance = Math.max(0.5, radius * radialFraction);
            double heightFraction = level.field_9229.method_43058();
            double radialX = Math.cos(angle);
            double radialZ = Math.sin(angle);
            SevereWeatherModel.VortexFlow flow = SevereWeatherModel.vortexFlow(
                    radialX * radialDistance,
                    radialZ * radialDistance,
                    windX,
                    windZ,
                    strength,
                    1.0 - heightFraction * 0.58);
            double particleX = centerX + radialX * radialDistance;
            double particleY = baseY + heightFraction * height;
            double particleZ = centerZ + radialZ * radialDistance;
            level.method_14199(
                    particle,
                    particleX,
                    particleY,
                    particleZ,
                    0,
                    flow.x(),
                    flow.y(),
                    flow.z(),
                    1.0);
        }
    }

    private static double randomOffset(class_3218 level, double radius) {
        return (level.field_9229.method_43058() * 2.0 - 1.0) * radius;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
