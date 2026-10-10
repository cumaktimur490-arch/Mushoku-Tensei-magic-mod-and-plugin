package dev.terrarealis.fabric;

import dev.terrarealis.fabric.command.RealisCommand;
import dev.terrarealis.fabric.registry.RealisRegistries;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terra Realis — ultra-realistic terrain for Minecraft 1.21.11.
 *
 * <p>Entry point. Registers the chunk generator and biome source codecs and the
 * {@code /terra_realis} command. All of the actual work lives in the Minecraft-free
 * {@code dev.terrarealis.kernel} package, which is compiled, unit-tested and rendered to PNG by
 * {@code tools/build-offline.sh} without a Minecraft jar.
 */
public class TerraRealisMod implements ModInitializer {

    public static final String MOD_ID = "terra_realis";
    public static final Logger LOGGER = LoggerFactory.getLogger("terra_realis");

    @Override
    public void onInitialize() {
        RealisRegistries.register();
        RealisCommand.register();
        LOGGER.info("Terra Realis initialised: generator terra_realis:realis, "
                + "world preset override data/minecraft/worldgen/world_preset/normal.json");
    }
}
