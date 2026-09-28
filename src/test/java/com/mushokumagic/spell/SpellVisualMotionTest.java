package com.mushokumagic.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
