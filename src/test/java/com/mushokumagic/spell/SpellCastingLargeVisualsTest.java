package com.mushokumagic.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mushokumagic.config.MagicConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpellCastingLargeVisualsTest {
    @Test
    void recognizesEnglishAndRussianBigModifiersWithoutChangingSpellPower() {
        SpellRegistry.rebuild(new MagicConfig().spells);

        for (String modifier : List.of(
                "BIG", "large", "HuGe",
                "большой", "большая", "большое", "большие", "большим", "большими",
                "огромный", "огромная", "огромное", "огромными")) {
            PhraseParser.Result parsed = PhraseParser.parse("Water Ball " + modifier);
            assertNotNull(parsed, "Modifier should follow a spell phrase: " + modifier);
            assertEquals("water_ball", parsed.spell().id());

            CastParams params = SpellCasting.paramsFor(parsed.extraWords());
            assertTrue(params.largeVisuals(), "Modifier should enable larger visuals: " + modifier);
            assertEquals(0.0, params.wordBonus(), 0.0, "Visual-only modifier must not add spell power");
            assertEquals(1.0, params.radiusMultiplier(), 0.0, "Visual-only modifier must not change gameplay radius");
            assertEquals(1.0, params.damageMultiplier(), 0.0, "Visual-only modifier must not change damage");
            assertEquals(1.0, params.castTimeMultiplier(), 0.0);
        }
    }
}
