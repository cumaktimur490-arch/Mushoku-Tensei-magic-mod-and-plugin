package com.mushokumagic.worldgen;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Forge-compatible entrypoint used by the native Forge and NeoForge 1.20.1 jars. */
@Mod(MushokuWorldgen.MOD_ID)
public final class MushokuWorldgen {
    public static final String MOD_ID = "mushoku_worldgen";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public MushokuWorldgen() {
        LOGGER.info("Mushoku: Vast Lands world preset loaded independently of Magic and Weather.");
    }
}
