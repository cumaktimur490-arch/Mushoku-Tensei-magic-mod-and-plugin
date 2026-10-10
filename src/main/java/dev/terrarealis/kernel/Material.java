package dev.terrarealis.kernel;

import java.util.HashMap;
import java.util.Map;

/**
 * The generator's block vocabulary.
 *
 * <p>The kernel never mentions Minecraft classes: it reasons about <i>lithologies and soils</i>.
 * The Fabric layer ({@code MaterialBlocks}) maps each constant onto a vanilla {@code BlockState}.
 * Keeping the two apart is what makes the kernel unit-testable and renderable headless — and it also
 * means a resource pack or another mod can re-target any material without touching the maths.
 *
 * <p>No custom blocks are registered: "ultra-realistic" here means realistic <i>geometry, drainage,
 * climate and stratigraphy</i>, and vanilla already ships 40+ plausible lithologies. Adding new
 * blocks would force every user to install a resource pack for zero gain in realism.
 */
public enum Material {

    // ------------------------------------------------------------- void/fluid
    AIR("air", Cat.VOID, 0.0, false, 0x00000000),
    WATER("water", Cat.FLUID, 0.0, false, 0x3F76E4FF),
    LAVA("lava", Cat.FLUID, 0.0, false, 0xCFFF6A00),

    // ------------------------------------------------------------------ crust
    BEDROCK("bedrock", Cat.ROCK, 1.0, false, 0x353535),

    STONE("stone", Cat.ROCK, 0.72, false, 0x7D7D7D),
    GRANITE("granite", Cat.ROCK, 0.80, false, 0x9A6A5A),
    DIORITE("diorite", Cat.ROCK, 0.78, false, 0xBDBDBD),
    ANDESITE("andesite", Cat.ROCK, 0.74, false, 0x88888A),
    DEEPSLATE("deepslate", Cat.ROCK, 0.85, false, 0x515154),
    TUFF("tuff", Cat.ROCK, 0.58, false, 0x6C6D66),
    CALCITE("calcite", Cat.ROCK, 0.52, true, 0xE2DCD5),
    DRIPSTONE_BLOCK("dripstone_block", Cat.ROCK, 0.62, true, 0x8C6A54),
    BASALT("basalt", Cat.ROCK, 0.70, false, 0x53535A),
    SMOOTH_BASALT("smooth_basalt", Cat.ROCK, 0.76, false, 0x48484E),
    BLACKSTONE("blackstone", Cat.ROCK, 0.74, false, 0x2F2933),
    OBSIDIAN("obsidian", Cat.ROCK, 0.95, false, 0x150B27),
    MAGMA_BLOCK("magma_block", Cat.ROCK, 0.60, false, 0x9E4A21),
    QUARTZ_BLOCK("quartz_block", Cat.ROCK, 0.70, false, 0xEBE7E0),
    AMETHYST_BLOCK("amethyst_block", Cat.ROCK, 0.66, false, 0x8B62BE),
    SCULK("sculk", Cat.ROCK, 0.40, false, 0x0E1B20),

    // ------------------------------------------------------- sedimentary rock
    SANDSTONE("sandstone", Cat.SEDIMENT, 0.45, false, 0xD9CFA2),
    SMOOTH_SANDSTONE("smooth_sandstone", Cat.SEDIMENT, 0.50, false, 0xE0D6AA),
    RED_SANDSTONE("red_sandstone", Cat.SEDIMENT, 0.45, false, 0xB2602A),

    // ------------------------------------------------------- unconsolidated
    GRAVEL("gravel", Cat.SEDIMENT, 0.20, true, 0x857B7B),
    SAND("sand", Cat.SEDIMENT, 0.15, true, 0xDBD3A0),
    RED_SAND("red_sand", Cat.SEDIMENT, 0.15, true, 0xBE6621),
    CLAY("clay", Cat.SEDIMENT, 0.18, false, 0xA1A6B3),
    MUD("mud", Cat.SEDIMENT, 0.12, false, 0x4A4C50),
    PACKED_MUD("packed_mud", Cat.SEDIMENT, 0.35, false, 0x8A6A4A),
    COBBLESTONE("cobblestone", Cat.SEDIMENT, 0.55, false, 0x7F7F7F),
    MOSSY_COBBLESTONE("mossy_cobblestone", Cat.SEDIMENT, 0.52, false, 0x6E8A5C),

    // ------------------------------------------------------------------ soils
    GRASS_BLOCK("grass_block", Cat.SOIL, 0.22, false, 0x79A74E),
    DIRT("dirt", Cat.SOIL, 0.22, false, 0x8B6244),
    COARSE_DIRT("coarse_dirt", Cat.SOIL, 0.24, false, 0x7A5C3E),
    ROOTED_DIRT("rooted_dirt", Cat.SOIL, 0.22, false, 0x8E6A4C),
    PODZOL("podzol", Cat.SOIL, 0.22, false, 0x6B4A26),
    MYCELIUM("mycelium", Cat.SOIL, 0.22, false, 0x8B7A92),
    MOSS_BLOCK("moss_block", Cat.SOIL, 0.20, false, 0x6B8F3C),
    TERRACOTTA("terracotta", Cat.SOIL, 0.42, false, 0x9E6146),
    ORANGE_TERRACOTTA("orange_terracotta", Cat.SOIL, 0.42, false, 0xA2542A),
    RED_TERRACOTTA("red_terracotta", Cat.SOIL, 0.42, false, 0x8E4330),
    WHITE_TERRACOTTA("white_terracotta", Cat.SOIL, 0.42, false, 0xD2B2A1),

    // ------------------------------------------------------------------- ice
    SNOW_BLOCK("snow_block", Cat.ICE, 0.10, false, 0xF4FAFA),
    ICE("ice", Cat.ICE, 0.30, false, 0x91B8E8),
    PACKED_ICE("packed_ice", Cat.ICE, 0.45, false, 0x8AA9E8),
    BLUE_ICE("blue_ice", Cat.ICE, 0.55, false, 0x74A6F5),

    // --------------------------------------------------------- ores (hosted)
    COAL_ORE("coal_ore", Cat.ORE, 0.72, false, 0x6A6A6A),
    IRON_ORE("iron_ore", Cat.ORE, 0.72, false, 0x9A8171),
    COPPER_ORE("copper_ore", Cat.ORE, 0.72, false, 0x8E8169),
    GOLD_ORE("gold_ore", Cat.ORE, 0.72, false, 0xA99449),
    REDSTONE_ORE("redstone_ore", Cat.ORE, 0.72, false, 0x8E4A42),
    LAPIS_ORE("lapis_ore", Cat.ORE, 0.72, false, 0x5A6CA8),
    DIAMOND_ORE("diamond_ore", Cat.ORE, 0.72, false, 0x6FA6A0),
    EMERALD_ORE("emerald_ore", Cat.ORE, 0.74, false, 0x59A86E),
    RAW_IRON_BLOCK("raw_iron_block", Cat.ORE, 0.75, false, 0xA68C7C),
    RAW_COPPER_BLOCK("raw_copper_block", Cat.ORE, 0.75, false, 0x9E7C63),
    ANCIENT_DEBRIS("ancient_debris", Cat.ORE, 0.90, false, 0x65474B),

    DEEPSLATE_COAL_ORE("deepslate_coal_ore", Cat.ORE, 0.85, false, 0x4E4E50),
    DEEPSLATE_IRON_ORE("deepslate_iron_ore", Cat.ORE, 0.85, false, 0x7C6A5F),
    DEEPSLATE_COPPER_ORE("deepslate_copper_ore", Cat.ORE, 0.85, false, 0x71685A),
    DEEPSLATE_GOLD_ORE("deepslate_gold_ore", Cat.ORE, 0.85, false, 0x8E7C45),
    DEEPSLATE_REDSTONE_ORE("deepslate_redstone_ore", Cat.ORE, 0.85, false, 0x71393A),
    DEEPSLATE_LAPIS_ORE("deepslate_lapis_ore", Cat.ORE, 0.85, false, 0x4A5A88),
    DEEPSLATE_DIAMOND_ORE("deepslate_diamond_ore", Cat.ORE, 0.85, false, 0x57837F),
    DEEPSLATE_EMERALD_ORE("deepslate_emerald_ore", Cat.ORE, 0.85, false, 0x4A8A5E),

    GLOWSTONE("glowstone", Cat.SPECIAL, 0.40, false, 0xF7D17E),
    SNOW_LAYER("snow_layer", Cat.SPECIAL, 0.08, false, 0xF4FAFA);

    /** Coarse grouping used by the erosion, pedology and metallogeny models. */
    public enum Cat { VOID, FLUID, ROCK, SEDIMENT, SOIL, ICE, ORE, SPECIAL }

    private static final Map<String, Material> BY_ID = new HashMap<>();

    static {
        for (Material m : values()) {
            BY_ID.put(m.id, m);
        }
    }

    public static Material of(String id) {
        Material m = BY_ID.get(id);
        return m == null ? STONE : m;
    }

    /** Vanilla block id path, e.g. {@code "grass_block"}. */
    public final String id;
    public final Cat cat;
    /** 0 (mud) .. 1 (bedrock). Drives incision resistance and the angle of repose. */
    public final double hardness;
    /** Soluble rocks get karst: dolines, caves, sinking streams. */
    public final boolean soluble;
    /** 0xAARRGGBB used only by the headless preview renderer. */
    public final int rgb;

    Material(String id, Cat cat, double hardness, boolean soluble, int rgb) {
        this.id = id;
        this.cat = cat;
        this.hardness = hardness;
        this.soluble = soluble;
        this.rgb = rgb;
    }

    public boolean isAir() {
        return this == AIR;
    }

    public boolean isFluid() {
        return cat == Cat.FLUID;
    }

    public boolean isSolidGround() {
        return cat != Cat.VOID && cat != Cat.FLUID;
    }

    public boolean isRocky() {
        return cat == Cat.ROCK || cat == Cat.ORE;
    }

    public boolean isUnconsolidated() {
        return cat == Cat.SEDIMENT || cat == Cat.SOIL;
    }

    /** Angle of repose in degrees, derived from hardness — hard rock holds steeper slopes. */
    public double reposeDegrees() {
        switch (cat) {
            case SOIL:
            case SEDIMENT:
                return 26.0 + hardness * 24.0;
            case ICE:
                return 40.0;
            case ROCK:
            case ORE:
                return 32.0 + hardness * 26.0;
            default:
                return 90.0;
        }
    }
}
