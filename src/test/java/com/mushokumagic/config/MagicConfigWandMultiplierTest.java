package com.mushokumagic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MagicConfigWandMultiplierTest {
    @Test
    void defaultsUseTheRequestedStaffMultipliers() {
        MagicConfig config = new MagicConfig();

        assertEquals(2.0, config.wandMultiplier(1), 0.0);
        assertEquals(15.0, config.wandMultiplier(2), 0.0);
        assertEquals(50.0, config.wandMultiplier(3), 0.0);
    }

    @Test
    void upgradesAnUnmodifiedLegacyConfig() throws ReflectiveOperationException {
        MagicConfig config = new MagicConfig();
        config.wandMultipliers = new LinkedHashMap<>(Map.of("1", 1.2, "2", 1.5, "3", 2.0));

        normalize(config);

        assertEquals(2.0, config.wandMultiplier(1), 0.0);
        assertEquals(15.0, config.wandMultiplier(2), 0.0);
        assertEquals(50.0, config.wandMultiplier(3), 0.0);
    }

    @Test
    void retainsCustomValuesAndFillsMissingTiers() throws ReflectiveOperationException {
        MagicConfig config = new MagicConfig();
        config.wandMultipliers = new LinkedHashMap<>(Map.of("2", 7.5));

        normalize(config);

        assertEquals(2.0, config.wandMultiplier(1), 0.0);
        assertEquals(7.5, config.wandMultiplier(2), 0.0);
        assertEquals(50.0, config.wandMultiplier(3), 0.0);
    }

    private static void normalize(MagicConfig config) throws ReflectiveOperationException {
        Method normalize = MagicConfig.class.getDeclaredMethod("normalize");
        normalize.setAccessible(true);
        normalize.invoke(config);
    }
}
