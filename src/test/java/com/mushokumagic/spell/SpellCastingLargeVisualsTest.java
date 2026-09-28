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

    @Test
    void visualMassTracksTheSpellTierWithoutChangingGameplayScaling() {
        Spell waterBall = new Spell("water_ball", "water", 10.0, 10, 10.0, 3.0, List.of());
        Spell fireBolt = new Spell("fire_bolt", "fire", 50.0, 40, 50.0, 4.0, List.of());
        Spell explosiveFireball = new Spell("explosive_fireball", "fire", 160.0, 40, 160.0, 6.0, List.of());

        assertEquals(0.8, SpellCasting.intrinsicVisualScale(waterBall), 0.0);
        assertEquals(1.0, SpellCasting.intrinsicVisualScale(fireBolt), 0.0);
        assertEquals(Math.sqrt(3.2), SpellCasting.intrinsicVisualScale(explosiveFireball), 1.0E-12);
        assertEquals(50.0, fireBolt.power(), 0.0);
        assertEquals(160.0, explosiveFireball.power(), 0.0);
    }

    @Test
    void recognizesLargeBallPhrasesBeforeOrAfterTheSpellName() {
        SpellRegistry.rebuild(new MagicConfig().spells);

        for (String command : List.of(
                "Water Ball big ball",
                "Водяной шар большого шара",
                "Водяной шар большого размера",
                "Огненный шар огромного размера",
                "Большой водяной шар",
                "Huge Fireball")) {
            PhraseParser.Result parsed = PhraseParser.parse(command);
            assertNotNull(parsed, "Large-ball wording should be accepted: " + command);

            CastParams params = SpellCasting.paramsFor(parsed.extraWords());
            assertTrue(params.largeVisuals(), "Large-ball wording should enable large visuals: " + command);
            assertEquals(0.0, params.wordBonus(), 0.0, "Modifier phrase must not grant a power bonus");
            assertEquals(1.0, params.radiusMultiplier(), 0.0, "Modifier phrase must not change gameplay area");
            assertEquals(1.0, params.damageMultiplier(), 0.0, "Modifier phrase must not change damage");
        }
    }
}
