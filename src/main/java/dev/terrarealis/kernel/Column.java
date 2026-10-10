package dev.terrarealis.kernel;

import dev.terrarealis.kernel.biome.BiomeKind;
import dev.terrarealis.kernel.geo.Caves;
import dev.terrarealis.kernel.geo.Geology;

/**
 * Everything the block placer needs about one 1-block-wide column.
 *
 * <p>Instances are pooled and reused: filling a chunk asks for 256 of these, several times a second,
 * on several threads. No allocation happens in the hot path.
 */
public final class Column {

    // ------------------------------------------------------------ geometry
    /** Y of the topmost solid block. */
    public int surfaceY;
    /** Y of the standing water surface, or {@link #NO_WATER}. */
    public int waterY = NO_WATER;
    public static final int NO_WATER = Integer.MIN_VALUE;
    /** Y of the water table, or {@link #NO_WATER}. */
    public int waterTableY = NO_WATER;
    /** Blocks of water above the ground, 0 if dry land. */
    public double waterDepth;
    /** True when the standing water is marine rather than a river or lake. */
    public boolean marine;
    public boolean river;
    public boolean lake;
    public double playa;
    /** 0..1 — a swallow hole or the dry reach below one. */
    public double swallow;

    // --------------------------------------------------------------- climate
    public double latitude;
    public double tempC;
    public double precipMm;
    public double aridity;
    public double sri;
    public double elevationM;
    public double snowLineM;
    public double treeLineM;
    public boolean permafrost;
    public double windX;
    public double windZ;
    public double windExposure;

    // ------------------------------------------------------------ morphometry
    public double slopeDeg;
    public double aspect;
    public double curvature;
    public double deposition;
    public double channel;
    /** Upstream drainage area in erosion cells. */
    public double acc;
    public double channelDepth;
    public double fan;
    public double delta;
    public double glacier;
    public double lavaFlow;
    public double sandSupply;
    public double duneHeight;
    public double duneSlipFace;
    public double oceanDistance;
    public double saturation;
    public double continentalness;

    // --------------------------------------------------------------- geology
    public Rock rock;
    public Geology.SoilKind soil;
    public double soilThickness;
    public double regolith;
    public double outcrop;
    public double karst;
    public double fracture;
    /** 0..1 — how weak the bedrock is (1 = unconsolidated sediment, 0 = fresh crystalline). */
    public double softness;
    public double dip;
    public double strike;
    public double strataSpacing;
    public double pavement;
    public double painted;
    public double boulders;
    public double riparian;
    public double volcanoVent;
    public double volcanoFresh;

    // ----------------------------------------------------------------- result
    /** Full lithological site; reused, never re-allocated. */
    public final Geology.Site site = new Geology.Site();
    /** Cave evaluation context derived from {@link #site} and the column geometry. */
    public final Caves.Ctx caveCtx = new Caves.Ctx();

    public BiomeKind biome;
    public BiomeKind subterraneanBiome;
    /** Top block. */
    public Material surface;
    /** Second block. */
    public Material filler;
    /** Third and deeper soil block. */
    public Material subsoil;
    /** Snow layers to place above {@link #surfaceY}, 0..8. */
    public int snowLayers;
    /** Ice thickness on standing water, blocks. */
    public int iceThickness;

    public void reset() {
        surfaceY = 0;
        waterY = NO_WATER;
        waterTableY = NO_WATER;
        waterDepth = 0;
        marine = false;
        river = false;
        lake = false;
        playa = 0;
        swallow = 0;
        latitude = 0;
        tempC = 0;
        precipMm = 0;
        aridity = 0;
        sri = 1;
        elevationM = 0;
        snowLineM = 0;
        treeLineM = 0;
        permafrost = false;
        windX = 1;
        windZ = 0;
        windExposure = 0;
        slopeDeg = 0;
        aspect = 0;
        curvature = 0;
        deposition = 0;
        channel = 0;
        acc = 0;
        channelDepth = 0;
        fan = 0;
        delta = 0;
        glacier = 0;
        lavaFlow = 0;
        sandSupply = 0;
        duneHeight = 0;
        duneSlipFace = 0;
        oceanDistance = 0;
        saturation = 0;
        continentalness = 0;
        rock = Rock.GRANITIC;
        site.rock = Rock.GRANITIC;
        soil = Geology.SoilKind.BROWN_EARTH;
        soilThickness = 3;
        regolith = 3;
        outcrop = 0;
        karst = 0;
        fracture = 0;
        softness = 0.5;
        dip = 0;
        strike = 0;
        strataSpacing = 6;
        pavement = 0;
        painted = 0;
        boulders = 0;
        riparian = 0;
        volcanoVent = 0;
        volcanoFresh = 0;
        biome = BiomeKind.TEMPERATE_GRASSLAND;
        subterraneanBiome = null;
        surface = Material.GRASS_BLOCK;
        filler = Material.DIRT;
        subsoil = Material.STONE;
        snowLayers = 0;
        iceThickness = 0;
    }

    public boolean underwater() {
        return waterY != NO_WATER && waterY > surfaceY;
    }
}
