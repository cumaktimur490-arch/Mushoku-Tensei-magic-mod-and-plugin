package dev.terrarealis.kernel.biome;

import java.util.HashMap;
import java.util.Map;

/**
 * The generator's biome vocabulary.
 *
 * <p>Every entry maps onto a <b>vanilla</b> biome. That is a deliberate decision, not a shortcut:
 * vanilla biomes already ship correct grass and foliage colours, spawn tables, placed features (so a
 * classified forest actually grows trees), structure weights and music. Registering 45 new biomes would
 * mean 45 empty biomes with no trees, and force every player to install a datapack or resource pack.
 * The realism Terra Realis adds is in <i>where</i> biomes occur — climate, drainage, altitude and
 * substrate decide — which is precisely the part vanilla gets wrong.
 *
 * <p>{@link #vanillaId} is resolved through the biome registry at runtime with a fallback, so an id
 * that does not exist on a given Minecraft version degrades gracefully instead of crashing the world.
 */
public enum BiomeKind {

    // ------------------------------------------------------------------ marine
    DEEP_OCEAN("deep_ocean", "Deep ocean", 0x1B3C7A, Zone.ABYSSAL),
    OCEAN("ocean", "Ocean", 0x2C5DB0, Zone.SHELF),
    COLD_OCEAN("cold_ocean", "Cold ocean", 0x2A4E86, Zone.SHELF),
    DEEP_COLD_OCEAN("deep_cold_ocean", "Deep cold ocean", 0x1B3560, Zone.ABYSSAL),
    FROZEN_OCEAN("frozen_ocean", "Frozen ocean", 0x6E9BC4, Zone.POLAR),
    DEEP_FROZEN_OCEAN("deep_frozen_ocean", "Deep frozen ocean", 0x3E6C99, Zone.POLAR),
    WARM_OCEAN("warm_ocean", "Warm ocean", 0x3AA7D6, Zone.SHELF),
    LUKEWARM_OCEAN("lukewarm_ocean", "Lukewarm ocean", 0x2E86C0, Zone.SHELF),
    DEEP_LUKEWARM_OCEAN("deep_lukewarm_ocean", "Deep lukewarm ocean", 0x1F5C8C, Zone.ABYSSAL),

    // ------------------------------------------------------------------ fluvial
    RIVER("river", "River", 0x3B6FD4, Zone.RIVERINE),
    FROZEN_RIVER("frozen_river", "Frozen river", 0x86B4E8, Zone.RIVERINE),
    ESTUARY("river", "Estuary", 0x4E7FB0, Zone.RIVERINE),

    // ------------------------------------------------------------------ coastal
    BEACH("beach", "Beach", 0xE0D8A8, Zone.COASTAL),
    SNOWY_BEACH("snowy_beach", "Snowy beach", 0xE8EEF2, Zone.COASTAL),
    STONY_SHORE("stony_shore", "Stony shore", 0x8C93A0, Zone.COASTAL),
    MANGROVE("mangrove_swamp", "Mangrove", 0x6E8C4A, Zone.COASTAL),
    TIDAL_FLAT("beach", "Tidal flat", 0xBFBBA8, Zone.COASTAL),

    // ------------------------------------------------------------------ lowland
    TROPICAL_RAINFOREST("jungle", "Tropical rainforest", 0x2F8F2A, Zone.LOWLAND),
    TROPICAL_MONSOON("bamboo_jungle", "Monsoon forest", 0x4FA03A, Zone.LOWLAND),
    TROPICAL_SEASONAL("sparse_jungle", "Seasonal tropical woodland", 0x6FA845, Zone.LOWLAND),
    SAVANNA("savanna", "Savanna", 0xB5C458, Zone.LOWLAND),
    SAVANNA_PLATEAU("savanna_plateau", "Savanna plateau", 0xA8B750, Zone.LOWLAND),
    DRY_SHRUBLAND("savanna", "Dry shrubland", 0xB0A862, Zone.LOWLAND),
    DESERT("desert", "Desert", 0xE8DE93, Zone.LOWLAND),
    COASTAL_DESERT("desert", "Coastal desert", 0xE2D68E, Zone.COASTAL),
    COLD_DESERT("windswept_gravelly_hills", "Cold stony desert", 0xA9A292, Zone.LOWLAND),
    BADLANDS("badlands", "Badlands", 0xC8794A, Zone.LOWLAND),
    ERODED_BADLANDS("eroded_badlands", "Eroded badlands", 0xB4643C, Zone.LOWLAND),
    WOODED_BADLANDS("wooded_badlands", "Wooded badlands", 0xA97A4A, Zone.LOWLAND),
    PLAYA("badlands", "Saline playa", 0xD8D2C0, Zone.LOWLAND),

    TEMPERATE_GRASSLAND("plains", "Temperate grassland", 0x8FBF60, Zone.LOWLAND),
    STEPPE("plains", "Steppe", 0x9DBE6A, Zone.LOWLAND),
    FLOWERING_MEADOW("sunflower_plains", "Flowering meadow", 0xA8CE5E, Zone.LOWLAND),
    TEMPERATE_DECIDUOUS("forest", "Temperate deciduous forest", 0x4F8F44, Zone.LOWLAND),
    TEMPERATE_MIXED("flower_forest", "Temperate mixed forest", 0x5C9B4E, Zone.LOWLAND),
    BIRCHWOOD("birch_forest", "Birchwood", 0x77A85E, Zone.LOWLAND),
    OLD_GROWTH_BIRCH("old_growth_birch_forest", "Old-growth birch", 0x6A9C55, Zone.LOWLAND),
    DARK_CONIFEROUS("dark_forest", "Dark coniferous forest", 0x3A6B33, Zone.LOWLAND),
    MEDITERRANEAN("flower_forest", "Mediterranean woodland", 0x7FA85A, Zone.LOWLAND),
    CHERRY_GROVE("cherry_grove", "Cherry grove", 0xE0A8C0, Zone.LOWLAND),

    WETLAND("swamp", "Wetland", 0x5A7050, Zone.LOWLAND),
    PEAT_BOG("swamp", "Peat bog", 0x4A5F45, Zone.LOWLAND),
    FLOODPLAIN("swamp", "Floodplain", 0x6A8055, Zone.RIVERINE),
    FEN("swamp", "Fen", 0x607A52, Zone.LOWLAND),
    MUSHROOM_BANK("mushroom_fields", "Mushroom bank", 0xA878B0, Zone.COASTAL),

    // ------------------------------------------------------------------ boreal
    BOREAL_FOREST("taiga", "Boreal forest", 0x4A7A5C, Zone.BOREAL),
    BOREAL_OLD_GROWTH("old_growth_spruce_taiga", "Old-growth spruce taiga", 0x3C6B4E, Zone.BOREAL),
    BOREAL_PINE("old_growth_pine_taiga", "Old-growth pine taiga", 0x5A7A50, Zone.BOREAL),
    SNOWY_TAIGA("snowy_taiga", "Snowy taiga", 0x6E8C86, Zone.BOREAL),
    FOREST_TUNDRA("snowy_taiga", "Forest tundra", 0x7C9088, Zone.BOREAL),
    TUNDRA("snowy_plains", "Tundra", 0xC8D6DC, Zone.POLAR),
    POLAR_DESERT("snowy_plains", "Polar desert", 0xDDE6EA, Zone.POLAR),
    ICE_CAP("ice_spikes", "Ice cap", 0xEAF4FA, Zone.POLAR),
    PERMAFROST_GROUND("snowy_plains", "Permafrost ground", 0xC0CED6, Zone.POLAR),

    // ------------------------------------------------------------------ montane
    MONTANE_MEADOW("meadow", "Montane meadow", 0x86BE6A, Zone.MONTANE),
    MONTANE_FOREST("grove", "Montane forest", 0x4E7A5A, Zone.MONTANE),
    SUBALPINE("snowy_slopes", "Subalpine", 0xA8BCC4, Zone.MONTANE),
    ALPINE_TUNDRA("snowy_slopes", "Alpine tundra", 0xB4C6CC, Zone.ALPINE),
    WINDSWEPT_HILLS("windswept_hills", "Windswept hills", 0x8C9C86, Zone.MONTANE),
    WINDSWEPT_FOREST("windswept_forest", "Windswept forest", 0x6A8A6A, Zone.MONTANE),
    WINDSWEPT_GRAVEL("windswept_gravelly_hills", "Windswept gravel", 0x98A09A, Zone.MONTANE),
    SCREE_SLOPE("windswept_gravelly_hills", "Scree slope", 0x9A9A96, Zone.MONTANE),
    ROCKY_PEAKS("stony_peaks", "Rocky peaks", 0x9EA49C, Zone.ALPINE),
    JAGGED_PEAKS("jagged_peaks", "Jagged peaks", 0xB8C4CC, Zone.ALPINE),
    FROZEN_PEAKS("frozen_peaks", "Frozen peaks", 0xD6E4EE, Zone.ALPINE),
    VOLCANIC_FLANK("windswept_gravelly_hills", "Volcanic flank", 0x6A6268, Zone.MONTANE),
    VOLCANIC_SUMMIT("stony_peaks", "Volcanic summit", 0x5A5258, Zone.ALPINE),

    // ------------------------------------------------------------------ subterranean
    CAVERN("dripstone_caves", "Cavern", 0x6A5A4A, Zone.SUBTERRANEAN),
    LUSH_CAVE("lush_caves", "Lush cave", 0x4A7A3A, Zone.SUBTERRANEAN),
    DEEP_DARK("deep_dark", "Deep dark", 0x141C22, Zone.SUBTERRANEAN);

    /** Coarse zone used for map legends and for ordering altitudinal belts. */
    public enum Zone { ABYSSAL, SHELF, COASTAL, RIVERINE, LOWLAND, BOREAL, POLAR, MONTANE, ALPINE, SUBTERRANEAN }

    private static final Map<String, BiomeKind> BY_VANILLA = new HashMap<>();

    static {
        for (BiomeKind k : values()) {
            BY_VANILLA.putIfAbsent(k.vanillaId, k);
        }
    }

    /** Vanilla biome id <i>path</i>, in the {@code minecraft} namespace. */
    public final String vanillaId;
    public final String label;
    /** Preview map colour, 0xRRGGBB. */
    public final int rgb;
    public final Zone zone;

    BiomeKind(String vanillaId, String label, int rgb, Zone zone) {
        this.vanillaId = vanillaId;
        this.label = label;
        this.rgb = rgb;
        this.zone = zone;
    }

    public String qualifiedId() {
        return "minecraft:" + vanillaId;
    }

    public boolean isAquatic() {
        return zone == Zone.ABYSSAL || zone == Zone.SHELF || zone == Zone.RIVERINE
                || this == FROZEN_OCEAN || this == DEEP_FROZEN_OCEAN;
    }

    public boolean isSubterranean() {
        return zone == Zone.SUBTERRANEAN;
    }

    public static BiomeKind byVanillaId(String id) {
        return BY_VANILLA.getOrDefault(id, TEMPERATE_GRASSLAND);
    }
}
