package com.mushokumagic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mushokumagic.spell.PhraseParser;
import com.mushokumagic.spell.SpellRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MagicConfigAnimeSpellPackTest {
    @Test
    void freshDefaultsIncludeAnimeSpellsAndAliases() {
        MagicConfig config = new MagicConfig();

        assertNotNull(spell(config, "water_cannon"));
        assertNotNull(spell(config, "cumulonimbus"));
        assertTrue(spell(config, "cumulonimbus").phrases.contains("кумуло нимбус"));
        assertTrue(spell(config, "earth_hedgehog").phrases.contains("earth hedgehog"));
        assertTrue(spell(config, "stone_ball").phrases.contains("stone cannon"));
        assertTrue(spell(config, "swamp").phrases.contains("quagmire"));
    }

    @Test
    void everyDefaultSpellHasARussianDisplayName() throws IOException {
        try (InputStream stream = MagicConfigAnimeSpellPackTest.class.getResourceAsStream("/assets/mushoku_magic/lang/ru_ru.json")) {
            assertNotNull(stream);
            JsonObject translations = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (MagicConfig.SpellDef definition : new MagicConfig().spells) {
                String key = "mushoku_magic.spell." + definition.id;
                assertTrue(translations.has(key), "Missing Russian spell name: " + key);
                assertFalse(translations.get(key).getAsString().isBlank(), "Blank Russian spell name: " + key);
            }
            assertEquals("Кумуло Нимбус", translations.get("mushoku_magic.spell.cumulonimbus").getAsString());
        }
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
        assertNotNull(spell(config, "earth_hedgehog"));
        assertTrue(waterCannon.phrases.contains("water cannon"));
        assertTrue(cumulonimbus.phrases.contains("cumulonimbus"));
        assertTrue(cumulonimbus.phrases.contains("кумуло нимбус"));
        assertTrue(spell(config, "stone_ball").phrases.contains("stone cannon"));
        assertTrue(spell(config, "explosive_fireball").phrases.contains("nuclear explosion"));
        assertEquals(3, config.spellPackVersion);

        SpellRegistry.rebuild(config.spells);
        assertEquals("water_cannon", PhraseParser.parse("Water Cannon").spell().id());
        assertEquals("cumulonimbus", PhraseParser.parse("Cumulonimbus").spell().id());
        assertEquals("cumulonimbus", PhraseParser.parse("Кумуло Нимбус").spell().id());
        assertEquals("earth_hedgehog", PhraseParser.parse("Earth Hedgehog").spell().id());
        assertEquals("stone_ball", PhraseParser.parse("Stone Cannon").spell().id());
        assertEquals("swamp", PhraseParser.parse("Quagmire").spell().id());

        config.spells.remove(cumulonimbus);
        normalize(config);
        assertFalse(config.spells.stream().anyMatch(definition -> "cumulonimbus".equals(definition.id)));
    }

    @Test
    void addsEarthHedgehogToVersionTwoPointTwoConfigsOnlyOnce() throws ReflectiveOperationException {
        MagicConfig config = new MagicConfig();
        MagicConfig.SpellDef defaultSpell = spell(config, "earth_hedgehog");
        config.spells.remove(defaultSpell);
        config.spellPackVersion = 1;

        normalize(config);

        MagicConfig.SpellDef addedSpell = spell(config, "earth_hedgehog");
        assertNotNull(addedSpell);
        assertEquals(3, config.spellPackVersion);
        config.spells.remove(addedSpell);
        normalize(config);
        assertFalse(config.spells.stream().anyMatch(definition -> "earth_hedgehog".equals(definition.id)));
    }

    @Test
    void addsSpacedRussianCumulonimbusPhraseToVersionTwoPointTwoOneConfigsOnce() throws ReflectiveOperationException {
        MagicConfig config = new MagicConfig();
        MagicConfig.SpellDef cumulonimbus = spell(config, "cumulonimbus");
        cumulonimbus.phrases.remove("кумуло нимбус");
        config.spellPackVersion = 2;

        normalize(config);

        assertEquals(3, config.spellPackVersion);
        assertTrue(cumulonimbus.phrases.contains("кумуло нимбус"));
        SpellRegistry.rebuild(config.spells);
        assertEquals("cumulonimbus", PhraseParser.parse("Кумуло Нимбус").spell().id());

        cumulonimbus.phrases.remove("кумуло нимбус");
        normalize(config);
        assertFalse(cumulonimbus.phrases.contains("кумуло нимбус"));
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
