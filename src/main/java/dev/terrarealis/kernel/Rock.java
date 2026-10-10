package dev.terrarealis.kernel;

/**
 * Lithological provinces.
 *
 * <p>A province is a volume of crust with a single geological history. It decides:
 * <ul>
 *   <li>which {@link Material} forms the bedrock and what its strata look like;</li>
 *   <li>how fast it weathers, and therefore how thick the soil mantle is;</li>
 *   <li>whether it is soluble (karst), fractured (fissure caves) or massive (blocky joints);</li>
 *   <li>which ore assemblage it hosts — this is the <b>metallogeny</b> model, and it is the reason a
 *       player finds coal in shale basins and copper in mafic rocks instead of uniformly everywhere.</li>
 * </ul>
 */
public enum Rock {

    /** Continental shield: granite/gneiss batholiths. Old, hard, low relief, poor soils. */
    GRANITIC("granitic shield", Material.GRANITE, Material.STONE, 0.30, false,
            OreSet.GRANITIC),
    /** High-grade metamorphic basement, often deeply incised. */
    GNEISSIC("metamorphic basement", Material.ANDESITE, Material.DIORITE, 0.26, false,
            OreSet.METAMORPHIC),
    /** Volcanic arc / oceanic crust: basalt, tuff, blackstone. Fertile, dark soils. */
    MAFIC("mafic volcanics", Material.BASALT, Material.TUFF, 0.44, false,
            OreSet.MAFIC),
    /** Deep plutonic / high-pressure crust, mostly below y 0. */
    ULTRAMAFIC("ultramafic pluton", Material.BLACKSTONE, Material.DEEPSLATE, 0.34, false,
            OreSet.ULTRAMAFIC),
    /** Marine carbonates: limestone and dolomite. The karst province. */
    CARBONATE("carbonate platform", Material.CALCITE, Material.DRIPSTONE_BLOCK, 0.55, true,
            OreSet.CARBONATE),
    /** Clastic basin: sandstone, siltstone, conglomerate. Aquifers and cuestas. */
    CLASTIC("clastic basin", Material.SANDSTONE, Material.TUFF, 0.50, false,
            OreSet.CLASTIC),
    /** Organic-rich shales: coal measures. */
    SHALE("black shale basin", Material.TUFF, Material.STONE, 0.62, false,
            OreSet.SHALE),
    /** Arid evaporites and red beds. */
    EVAPORITE("evaporite / red beds", Material.RED_SANDSTONE, Material.TERRACOTTA, 0.58, false,
            OreSet.EVAPORITE),
    /** Unconsolidated Cenozoic cover: gravels, sands, loess. */
    COVER("sedimentary cover", Material.GRAVEL, Material.SAND, 0.70, false,
            OreSet.COVER),
    /** Active volcanic edifice. */
    VOLCANIC("active volcano", Material.SMOOTH_BASALT, Material.MAGMA_BLOCK, 0.36, false,
            OreSet.VOLCANIC),
    /** Glacial drift: till, outwash, erratics. */
    DRIFT("glacial drift", Material.GRAVEL, Material.COBBLESTONE, 0.75, false,
            OreSet.COVER),
    /** Deep crust below the deepslate line: everything becomes deepslate-hosted. */
    DEEP("deep crust", Material.DEEPSLATE, Material.DEEPSLATE, 0.22, false,
            OreSet.DEEP);

    /** Which ore assemblage the province can host, and with what relative enrichment. */
    public enum OreSet {
        /** Granites: Sn-W-vein style -> gold + copper + emerald in greisen contacts. */
        GRANITIC,
        METAMORPHIC,
        /** VMS / mafic: copper, iron, some gold. */
        MAFIC,
        /** Chromite-PGE analogue: ancient debris + iron. */
        ULTRAMAFIC,
        /** Carbonates: MVT lead-zinc analogue -> redstone + iron; caves dominate instead. */
        CARBONATE,
        CLASTIC,
        /** Coal measures: coal, some iron. */
        SHALE,
        EVAPORITE,
        /** Placer deposits: gold in gravels. */
        COVER,
        VOLCANIC,
        DEEP
    }

    public final String label;
    /** The dominant bedrock material. */
    public final Material primary;
    /** The subordinate material interleaved with the primary one along bedding planes. */
    public final Material secondary;
    /** Weathering rate: higher means thicker regolith and soil for the same slope and climate. */
    public final double weatherability;
    public final boolean soluble;
    public final OreSet ores;

    Rock(String label, Material primary, Material secondary, double weatherability,
         boolean soluble, OreSet ores) {
        this.label = label;
        this.primary = primary;
        this.secondary = secondary;
        this.weatherability = weatherability;
        this.soluble = soluble;
        this.ores = ores;
    }

    /** Whether this province is expected to be layered (sedimentary/metamorphic) rather than massive. */
    public boolean bedded() {
        switch (this) {
            case CARBONATE:
            case CLASTIC:
            case SHALE:
            case EVAPORITE:
            case GNEISSIC:
            case COVER:
                return true;
            default:
                return false;
        }
    }

    /** Resistance to fluvial incision, 0..1, used to modulate the stream-power K. */
    public double erodibility() {
        return 1.0 - Math.min(0.95, primary.hardness * 0.7 + weatherability * -0.25 + 0.35);
    }
}
