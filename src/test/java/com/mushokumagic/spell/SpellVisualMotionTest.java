package com.mushokumagic.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.class_243;
import org.junit.jupiter.api.Test;

class SpellVisualMotionTest {
    @Test
    void releaseProgressStaysAtTheCasterUntilTheChargePhaseFinishes() {
        assertEquals(0.0, SpellVisualMotion.releaseProgress(0.0), 0.0);
        assertEquals(0.0, SpellVisualMotion.releaseProgress(0.67), 0.0);
        assertEquals(0.0, SpellVisualMotion.releaseProgress(SpellVisualMotion.RELEASE_START), 0.0);
        assertEquals(0.5, SpellVisualMotion.releaseProgress(0.84), 1.0E-9);
        assertEquals(1.0, SpellVisualMotion.releaseProgress(1.0), 0.0);
        assertEquals(1.0, SpellVisualMotion.releaseProgress(2.0), 0.0);
    }

    @Test
    void easingCurvesStayBoundedAndMoveMonotonically() {
        double previousFlight = 0.0;
        double previousBeam = 0.0;
        for (int step = 0; step <= 100; ++step) {
            double progress = step / 100.0;
            double flight = SpellVisualMotion.easeInOut(progress);
            double beam = SpellVisualMotion.easeOut(progress);

            assertTrue(flight >= 0.0 && flight <= 1.0);
            assertTrue(beam >= 0.0 && beam <= 1.0);
            assertTrue(flight >= previousFlight);
            assertTrue(beam >= previousBeam);
            previousFlight = flight;
            previousBeam = beam;
        }
        assertEquals(0.0, SpellVisualMotion.easeInOut(0.0), 0.0);
        assertEquals(1.0, SpellVisualMotion.easeInOut(1.0), 0.0);
        assertEquals(0.0, SpellVisualMotion.easeOut(0.0), 0.0);
        assertEquals(1.0, SpellVisualMotion.easeOut(1.0), 0.0);
    }

    @Test
    void chargePulseStartsAndEndsQuietlyWithItsPeakMidCast() {
        assertEquals(0.0, SpellVisualMotion.chargePulse(0.0), 0.0);
        assertEquals(1.0, SpellVisualMotion.chargePulse(0.5), 1.0E-12);
        assertEquals(0.0, SpellVisualMotion.chargePulse(1.0), 1.0E-12);
        assertTrue(SpellVisualMotion.chargePulse(-1.0) >= 0.0);
        assertTrue(SpellVisualMotion.chargePulse(2.0) >= 0.0);
    }

    @Test
    void helixOffsetsStayPerpendicularAndKeepTheirRadiusForVerticalAndHorizontalTravel() {
        class_243[] directions = {
                new class_243(1.0, 0.0, 0.0),
                new class_243(0.0, 1.0, 0.0),
                new class_243(0.2, 0.3, 0.9)
        };
        for (class_243 direction : directions) {
            class_243 offset = SpellVisualMotion.ribbonOffset(direction, 0.73, 2.5);
            double dx = direction.method_10216();
            double dy = direction.method_10214();
            double dz = direction.method_10215();
            double directionLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double offsetX = offset.method_10216();
            double offsetY = offset.method_10214();
            double offsetZ = offset.method_10215();
            double offsetLength = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
            double dot = (dx * offsetX + dy * offsetY + dz * offsetZ) / directionLength;

            assertEquals(2.5, offsetLength, 1.0E-9);
            assertEquals(0.0, dot, 1.0E-9);
        }
    }
}
