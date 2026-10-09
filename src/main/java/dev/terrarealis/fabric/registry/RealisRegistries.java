package dev.terrarealis.fabric.registry;

import dev.terrarealis.fabric.worldgen.RealisBiomeSource;
import dev.terrarealis.fabric.worldgen.RealisChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Registration of the two codec-bearing entry points: the chunk generator and the biome source.
 *
 * <p>Both are registered under the {@code terra_realis} namespace and referenced by the world presets in
 * {@code data/}, which is what makes the generator reachable from the world-creation screen without a
 * single mixin or reflection hack.
 */
public final class RealisRegistries {

    public static final Identifier CHUNK_GENERATOR_ID =
            Identifier.fromNamespaceAndPath("terra_realis", "realis");
    public static final Identifier BIOME_SOURCE_ID =
            Identifier.fromNamespaceAndPath("terra_realis", "realis");

    private RealisRegistries() {}

    public static void register() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, CHUNK_GENERATOR_ID,
                RealisChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.BIOME_SOURCE, BIOME_SOURCE_ID,
                RealisBiomeSource.CODEC);
    }
}
