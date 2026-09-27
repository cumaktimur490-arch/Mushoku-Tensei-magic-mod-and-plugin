package com.mushokumagic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mushokumagic.spell.PhraseParser;
import com.mushokumagic.spell.SpellRegistry;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MagicConfigAnimeSpellPackTest {
    @Test
    void freshDefaultsIncludeAnimeSpellsAndAliases() {
        MagicConfig config = new MagicConfig();

        assertNotNull(spell(config, "water_cannon"));
        assertNotNull(spell(config, "cumulonimbus"));
        assertTrue(spell(config, "stone_ball").phrases.contains("stone cannon"));
        assertTrue(spell(config, "swamp").phrases.contains("quagmire"));
    }

    @Test
    void migratesLegacySpellListsAndKeepsUserRemovals() throws ReflectiveOperationException {
        MagicConfig config = new MagicConfig();
        config.spells = new ArrayList<>(List.of(
                new MagicConfig.SpellDef("stone_ball", "earth", 25.0, 15, 1.0, 1.0, List.of("stone ball")),
                new MagicConfig.SpellDef("swamp", "earth", 200.0, 60, 1.0, 10.0, List.of("swamp")),
                new MagicConfig.SpellDef("explosive_fireball", "fire", 160.0, 40, 6.0, 4.0, List.of("explosive fireball"))));
        config.spellPackVersion = 0;

        normalize(config);

        MagicConfig.SpellDef waterCannon = spell(config, "water_cannon");
        MagicConfig.SpellDef cumulonimbus = spell(config, "cumulonimbus");
        assertNotNull(waterCannon);
        assertNotNull(cumulonimbus);
        assertTrue(waterCannon.phrases.contains("water cannon"));
        assertTrue(cumulonimbus.phrases.contains("cumulonimbus"));
        assertTrue(spell(config, "stone_ball").phrases.contains("stone cannon"));
        assertTrue(spell(config, "explosive_fireball").phrases.contains("nuclear explosion"));
        assertEquals(1, config.spellPackVersion);

        SpellRegistry.rebuild(config.spells);
        assertEquals("water_cannon", PhraseParser.parse("Water Cannon").spell().id());
        assertEquals("cumulonimbus", PhraseParser.parse("Cumulonimbus").spell().id());
        assertEquals("stone_ball", PhraseParser.parse("Stone Cannon").spell().id());
        assertEquals("swamp", PhraseParser.parse("Quagmire").spell().id());

        config.spells.remove(cumulonimbus);
        normalize(config);
        assertFalse(config.spells.stream().anyMatch(definition -> "cumulonimbus".equals(definition.id)));
    }

    private static MagicConfig.SpellDef spell(MagicConfig config, String id) {
        return config.spells.stream().filter(definition -> id.equals(definition.id)).findFirst().orElse(null);
    }

    private static void normalize(MagicConfig config) throws ReflectiveOperationException {
        Method normalize = MagicConfig.class.getDeclaredMethod("normalize");
        normalize.setAccessible(true);
        normalize.invoke(config);
    }
}
