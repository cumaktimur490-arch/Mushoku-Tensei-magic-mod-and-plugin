package com.mushokumagic.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MagicScalingTest {
    @Test
    void tierThreeStaffCreatesWideFireAreaAndLongerRange() {
        double power = 50.0;
        double fireBoltRadius = MagicScaling.radius(2.5, power, 1.0);

        assertTrue(fireBoltRadius * 2.0 >= 50.0);
        assertTrue(MagicScaling.range(48.0, power) >= 50.0);
        assertEquals(87.2, MagicScaling.range(48.0, power), 0.0001);
    }

    @Test
    void normalPowerKeepsBaseGeometry() {
        assertEquals(2.5, MagicScaling.radius(2.5, 1.0, 1.0), 0.0);
        assertEquals(48.0, MagicScaling.range(48.0, 1.0), 0.0);
    }

    @Test
    void areaAndRangeRemainBoundedForExtremeConfigValues() {
        assertEquals(32.0, MagicScaling.radius(1000.0, 1_000_000.0, 20.0), 0.0);
        assertEquals(128.0, MagicScaling.range(48.0, 1_000_000.0), 0.0);
        assertEquals(12, MagicScaling.intensity(1_000_000.0));
        assertEquals(1000.0, MagicScaling.clampPower(1_000_000.0), 0.0);
        assertEquals(1000.0, MagicScaling.clampPower(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(0.1, MagicScaling.clampPower(Double.NEGATIVE_INFINITY), 0.0);
        assertEquals(1.0, MagicScaling.clampPower(Double.NaN), 0.0);
    }
}
