package dev.terrarealis.fabric.worldgen;

import dev.terrarealis.kernel.Material;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The single place where the Minecraft-free kernel vocabulary meets real blocks.
 *
 * <p>The kernel reasons in terms of {@link Material}: a lithological and pedological category with a
 * hardness, a solubility and a colour. Minecraft needs {@link BlockState}. This map is the whole of the
 * coupling, which is what lets the kernel be compiled, unit-tested and rendered without a Minecraft jar
 * at all.
 *
 * <p>Every entry is a vanilla block. The mod adds no blocks, no items and no creative tab; it only
 * arranges vanilla matter in a way vanilla never does.
 */
public final class MaterialBlocks {

    private MaterialBlocks() {}

    public static BlockState state(Material m) {
        switch (m) {
            case AIR: return Blocks.AIR.defaultBlockState();
            case WATER: return Blocks.WATER.defaultBlockState();
            case LAVA: return Blocks.LAVA.defaultBlockState();
            case BEDROCK: return Blocks.BEDROCK.defaultBlockState();
            case STONE: return Blocks.STONE.defaultBlockState();
            case GRANITE: return Blocks.GRANITE.defaultBlockState();
            case DIORITE: return Blocks.DIORITE.defaultBlockState();
            case ANDESITE: return Blocks.ANDESITE.defaultBlockState();
            case DEEPSLATE: return Blocks.DEEPSLATE.defaultBlockState();
            case TUFF: return Blocks.TUFF.defaultBlockState();
            case CALCITE: return Blocks.CALCITE.defaultBlockState();
            case DRIPSTONE_BLOCK: return Blocks.DRIPSTONE_BLOCK.defaultBlockState();
            case BASALT: return Blocks.BASALT.defaultBlockState();
            case SMOOTH_BASALT: return Blocks.SMOOTH_BASALT.defaultBlockState();
            case BLACKSTONE: return Blocks.BLACKSTONE.defaultBlockState();
            case OBSIDIAN: return Blocks.OBSIDIAN.defaultBlockState();
            case MAGMA_BLOCK: return Blocks.MAGMA_BLOCK.defaultBlockState();
            case QUARTZ_BLOCK: return Blocks.QUARTZ_BLOCK.defaultBlockState();
            case AMETHYST_BLOCK: return Blocks.AMETHYST_BLOCK.defaultBlockState();
            case SCULK: return Blocks.SCULK.defaultBlockState();
            case SANDSTONE: return Blocks.SANDSTONE.defaultBlockState();
            case SMOOTH_SANDSTONE: return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
            case RED_SANDSTONE: return Blocks.RED_SANDSTONE.defaultBlockState();
            case GRAVEL: return Blocks.GRAVEL.defaultBlockState();
            case SAND: return Blocks.SAND.defaultBlockState();
            case RED_SAND: return Blocks.RED_SAND.defaultBlockState();
            case CLAY: return Blocks.CLAY.defaultBlockState();
            case MUD: return Blocks.MUD.defaultBlockState();
            case PACKED_MUD: return Blocks.PACKED_MUD.defaultBlockState();
            case COBBLESTONE: return Blocks.COBBLESTONE.defaultBlockState();
            case MOSSY_COBBLESTONE: return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case GRASS_BLOCK: return Blocks.GRASS_BLOCK.defaultBlockState();
            case DIRT: return Blocks.DIRT.defaultBlockState();
            case COARSE_DIRT: return Blocks.COARSE_DIRT.defaultBlockState();
            case ROOTED_DIRT: return Blocks.ROOTED_DIRT.defaultBlockState();
            case PODZOL: return Blocks.PODZOL.defaultBlockState();
            case MYCELIUM: return Blocks.MYCELIUM.defaultBlockState();
            case MOSS_BLOCK: return Blocks.MOSS_BLOCK.defaultBlockState();
            case TERRACOTTA: return Blocks.TERRACOTTA.defaultBlockState();
            case ORANGE_TERRACOTTA: return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            case RED_TERRACOTTA: return Blocks.RED_TERRACOTTA.defaultBlockState();
            case WHITE_TERRACOTTA: return Blocks.WHITE_TERRACOTTA.defaultBlockState();
            case SNOW_BLOCK: return Blocks.SNOW_BLOCK.defaultBlockState();
            case ICE: return Blocks.ICE.defaultBlockState();
            case PACKED_ICE: return Blocks.PACKED_ICE.defaultBlockState();
            case BLUE_ICE: return Blocks.BLUE_ICE.defaultBlockState();
            case COAL_ORE: return Blocks.COAL_ORE.defaultBlockState();
            case IRON_ORE: return Blocks.IRON_ORE.defaultBlockState();
            case COPPER_ORE: return Blocks.COPPER_ORE.defaultBlockState();
            case GOLD_ORE: return Blocks.GOLD_ORE.defaultBlockState();
            case REDSTONE_ORE: return Blocks.REDSTONE_ORE.defaultBlockState();
            case LAPIS_ORE: return Blocks.LAPIS_ORE.defaultBlockState();
            case DIAMOND_ORE: return Blocks.DIAMOND_ORE.defaultBlockState();
            case EMERALD_ORE: return Blocks.EMERALD_ORE.defaultBlockState();
            case RAW_IRON_BLOCK: return Blocks.RAW_IRON_BLOCK.defaultBlockState();
            case RAW_COPPER_BLOCK: return Blocks.RAW_COPPER_BLOCK.defaultBlockState();
            case ANCIENT_DEBRIS: return Blocks.ANCIENT_DEBRIS.defaultBlockState();
            case DEEPSLATE_COAL_ORE: return Blocks.DEEPSLATE_COAL_ORE.defaultBlockState();
            case DEEPSLATE_IRON_ORE: return Blocks.DEEPSLATE_IRON_ORE.defaultBlockState();
            case DEEPSLATE_COPPER_ORE: return Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState();
            case DEEPSLATE_GOLD_ORE: return Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState();
            case DEEPSLATE_REDSTONE_ORE: return Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
            case DEEPSLATE_LAPIS_ORE: return Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState();
            case DEEPSLATE_DIAMOND_ORE: return Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
            case DEEPSLATE_EMERALD_ORE: return Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState();
            case GLOWSTONE: return Blocks.GLOWSTONE.defaultBlockState();
            case SNOW_LAYER: return Blocks.SNOW_LAYER.defaultBlockState();
            default: return Blocks.STONE.defaultBlockState();
        }
    }

    /** Snow with the correct {@code layers} property, 1..8. */
    public static BlockState snowLayers(int layers) {
        int n = Math.max(1, Math.min(8, layers));
        return Blocks.SNOW_LAYER.defaultBlockState().setValue(SnowLayerBlock.LAYERS, n);
    }
}
