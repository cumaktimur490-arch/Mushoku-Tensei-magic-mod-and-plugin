package com.mushokumagic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class MagicConfigKeywordMigrationTest {
    @Test
    void addsBigKeywordToLegacyConfigWithoutReplacingExistingKeywords() throws ReflectiveOperationException {
        MagicConfig config = new MagicConfig();
        config.keywords.remove("big");
        MagicConfig.KeywordDef customKeyword = new MagicConfig.KeywordDef(
                "custom", List.of("my custom word"), 1.25, 1.5, 0.75, false, false);
        config.keywords.put("custom", customKeyword);

        normalize(config);

        assertNotNull(config.keywords.get("big"));
        assertEquals("big", config.keywords.get("big").id);
        assertSame(customKeyword, config.keywords.get("custom"));
        assertTrue(config.keywords.containsKey("explosion"));
        assertTrue(config.keywords.containsKey("fast"));
        assertTrue(config.keywords.containsKey("silent"));
        assertTrue(config.keywords.get("big").words.containsAll(List.of("big", "large", "huge", "большой", "огромный", "большого шара", "большого размера")));
    }

    @Test
    void addsNewLargeBallPhrasesToPreviouslySavedBigKeywordWithoutResettingItsOptions() throws ReflectiveOperationException {
        MagicConfig config = new MagicConfig();
        MagicConfig.KeywordDef savedBigKeyword = new MagicConfig.KeywordDef(
                "big", List.of("user's large orb"), 1.0, 1.25, 0.9, false, false);
        config.keywords = new java.util.LinkedHashMap<>(java.util.Map.of("big", savedBigKeyword));

        normalize(config);

        assertTrue(savedBigKeyword.words.contains("user's large orb"));
        assertTrue(savedBigKeyword.words.contains("большого шара"));
        assertTrue(savedBigKeyword.words.contains("большого размера"));
        assertEquals(1.25, savedBigKeyword.damageMultiplier, 0.0);
        assertEquals(0.9, savedBigKeyword.castTimeMultiplier, 0.0);
    }

    private static void normalize(MagicConfig config) throws ReflectiveOperationException {
        Method normalize = MagicConfig.class.getDeclaredMethod("normalize");
        normalize.setAccessible(true);
        normalize.invoke(config);
    }
}
