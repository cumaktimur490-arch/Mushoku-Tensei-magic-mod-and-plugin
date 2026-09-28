package com.mushokumagic.world;

import com.mushokumagic.spell.MagicPalette;
import net.minecraft.class_2390;
import net.minecraft.class_2394;
import net.minecraft.class_2398;
import net.minecraft.class_3218;

/** Shared particle staging for layered local clouds, wind and directional precipitation. */
public final class WeatherVisuals {
    private static final class_2394 CLOUD_HIGHLIGHT = new class_2390(0xC5D3DD, 1.12f);
    private static final class_2394 CLOUD_BODY = new class_2390(0x758491, 1.42f);
    private static final class_2394 CLOUD_UNDERSIDE = new class_2390(0x35414F, 1.55f);
    private static final class_2394 WIND_MIST = new class_2390(0xA7C5D4, 0.38f);

    private WeatherVisuals() {
    }

    /** Draws three drifting cloud strata instead of a single random cloud puff. */
    public static void emitCloudDeck(
            class_3218 level,
            double x,
            double y,
            double z,
            double radius,
            double cover,
            double windX,
            double windZ,
            long tick,
            boolean storm) {
        double safeCover = clamp(cover, 0.0, 1.0);
        if (safeCover < 0.32) {
            return;
        }
        double windLength = Math.hypot(windX, windZ);
        double directionX = windLength > 1.0E-8 ? windX / windLength : 1.0;
        double directionZ = windLength > 1.0E-8 ? windZ / windLength : 0.0;
        double safeRadius = clamp(radius, 7.0, 96.0);
        double phase = tick * 0.0035;
        int density = 7 + (int)Math.round(safeCover * 9.0);

        for (int layer = 0; layer < 3; ++layer) {
            double layerOffset = layer - 1.0;
            double alongWind = layerOffset * safeRadius * 0.12
                    + Math.sin(phase + layer * 1.7) * safeRadius * 0.035;
            double crossWind = Math.cos(phase * 0.72 + layer * 1.35) * safeRadius * 0.045;
            double centerX = x + directionX * alongWind - directionZ * crossWind;
            double centerZ = z + directionZ * alongWind + directionX * crossWind;
            double layerY = y + layerOffset * 1.7;
            double layerSpread = safeRadius * (0.68 + layer * 0.045);
            int cloudPuffs = density + (layer == 1 ? 3 : 0);

            level.method_65096((class_2394)class_2398.field_11204,
                    centerX, layerY, centerZ,
                    cloudPuffs, layerSpread, 1.5, layerSpread, 0.0015);
            level.method_65096(CLOUD_HIGHLIGHT,
                    centerX - directionX * 1.8, layerY + 0.55, centerZ - directionZ * 1.8,
                    4 + density / 2, layerSpread * 0.76, 0.9, layerSpread * 0.76, 0.001);
            if (storm && layer < 2) {
                level.method_65096(CLOUD_BODY,
                        centerX + directionX * 1.2, layerY - 0.45, centerZ + directionZ * 1.2,
                        5 + density, layerSpread * 0.72, 0.85, layerSpread * 0.72, 0.0008);
            }
            if (storm && layer == 0) {
                level.method_65096(CLOUD_UNDERSIDE,
                        centerX, layerY - 1.35, centerZ,
                        4 + density, layerSpread * 0.62, 0.7, layerSpread * 0.62, 0.0005);
            }
        }
    }

    /** Builds a forked blue-white lightning bolt from connected particle segments. */
    public static void emitLightning(class_3218 level, double x, double z, double bottomY, double height) {
        double safeHeight = clamp(height, 12.0, 48.0);
        double topY = bottomY + safeHeight;
        int segments = Math.max(8, (int)Math.round(safeHeight / 2.1));
        double step = safeHeight / segments;
        double currentX = x;
        double currentZ = z;
        class_2394 boltCore = MagicPalette.core("water", 2.0f);
        class_2394 boltEdge = MagicPalette.core("water", 1.1f);

        for (int segment = 0; segment <= segments; ++segment) {
            if (segment > 0) {
                currentX += randomOffset(level, 0.92);
                currentZ += randomOffset(level, 0.92);
            }
            double y = topY - segment * step;
            level.method_65096(boltCore,
                    currentX, y, currentZ,
                    3, 0.16, step * 0.30, 0.16, 0.025);
            level.method_65096((class_2394)class_2398.field_11207,
                    currentX, y, currentZ,
                    1, 0.08, step * 0.24, 0.08, 0.018);
            if (segment % 3 == 1) {
                level.method_65096((class_2394)class_2398.field_29644,
                        currentX, y, currentZ,
                        1, 0.18, 0.24, 0.18, 0.02);
            }
            if (segment == segments / 3 || segment == (segments * 2) / 3) {
                WeatherVisuals.emitLightningBranch(level, currentX, y, currentZ, boltEdge, segment % 2 == 0 ? 1.0 : -1.0);
            }
        }

        level.method_65096(boltCore,
                currentX, bottomY + 0.7, currentZ,
                28, 2.5, 1.4, 2.5, 0.055);
        level.method_65096((class_2394)class_2398.field_11207,
                currentX, bottomY + 0.8, currentZ,
                14, 1.6, 0.9, 1.6, 0.04);
        level.method_65096((class_2394)class_2398.field_29644,
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
            level.method_65096(bolt,
                    branchX, branchY, branchZ,
                    2, 0.12, 0.3, 0.12, 0.018);
            if (point % 2 == 0) {
                level.method_65096((class_2394)class_2398.field_11207,
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
            level.method_65096(particle,
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
            level.method_65096(particle,
                    particleX, particleY, particleZ,
                    0, windX * speed, lift, windZ * speed, 1.0);
        }
    }

    private static double randomOffset(class_3218 level, double radius) {
        return (level.field_9229.method_43058() * 2.0 - 1.0) * radius;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
