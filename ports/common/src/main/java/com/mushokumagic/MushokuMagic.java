package com.mushokumagic;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.spell.SpellRegistry;
import net.minecraft.class_2960;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-neutral bootstrap shared by the Forge and NeoForge 1.20.1 artifacts. */
public final class MushokuMagic {
    public static final String MOD_ID = "mushoku_magic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private MushokuMagic() {
    }

    public static class_2960 id(String path) {
        return new class_2960(MOD_ID, path);
    }

    public static void initialize() {
        MagicConfig.load();
        LOGGER.info("Mushoku Tensei: Magic loaded. Spells in config: {}", SpellRegistry.spells().size());
    }
}
