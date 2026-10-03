package com.mushokumagic.worldgen;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fabric entrypoint for the standalone, data-driven Mushoku world preset. */
public final class MushokuWorldgen implements ModInitializer {
    public static final String MOD_ID = "mushoku_worldgen";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Mushoku: Vast Lands world preset loaded independently of Magic and Weather.");
    }
}
