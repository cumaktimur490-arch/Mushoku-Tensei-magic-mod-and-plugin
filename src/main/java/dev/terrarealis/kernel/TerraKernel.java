package dev.terrarealis.kernel;

import dev.terrarealis.kernel.biome.BiomeClassifier;
import dev.terrarealis.kernel.biome.BiomeKind;
import dev.terrarealis.kernel.geo.Arid;
import dev.terrarealis.kernel.geo.Caves;
import dev.terrarealis.kernel.geo.Climate;
import dev.terrarealis.kernel.geo.Continents;
import dev.terrarealis.kernel.geo.Erosion;
import dev.terrarealis.kernel.geo.Geology;
import dev.terrarealis.kernel.geo.Tectonics;
import dev.terrarealis.kernel.geo.Volcanism;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;
import dev.terrarealis.kernel.region.RegionTile;
import dev.terrarealis.kernel.region.TileCache;

/**
 * The generator.
 *
 * <p>Everything in the mod is reachable from here and nothing in here knows Minecraft exists. That is
 * deliberate: the whole pipeline can be compiled, run and rendered to PNG without a Minecraft jar,
 * which is the only way to iterate on a landscape generator at any speed.
 *
 * <h2>Two elevation fields, and why</h2>
 *
 * <p><b>Analytic</b> ({@link #upliftHeight}) — continents, plate tectonics, orogenic belts, basin and
 * range fault blocks, inselbergs and volcanic edifices. Cheap, cache-free, valid at any coordinate with
 * no neighbours. It is the <i>uplift</i> surface: what the landscape would look like if nothing ever
 * eroded it.
 *
 * <p><b>Simulated</b> ({@link RegionTile}) — the analytic field, then hydrologically conditioned
 * drainage, stream-power incision, transport-limited deposition, hillslope failure at the angle of
 * repose, isostatic rebound, glaciation, lava flows, karst dissolution and alluvial fans. This is what
 * the player actually walks on.
 *
 * <p>{@code getBaseHeight} and {@code getBaseColumn} — the two hooks Minecraft uses for structure
 * placement and the F3 "surface" readout — are answered from the analytic field, because they must not
 * trigger a tile simulation. The difference is bounded and is asserted in {@code KernelTests}.
 *
 * <h2>Thread safety</h2>
 *
 * <p>All noise fields are immutable and stateless, so they are shared freely. The mutable scratch
 * objects (tectonic setting, volcanic edifice, geology site, climate envelope) are per-thread, and the
 * tile cache is synchronised. {@code TerraKernel} is safe to call from every worldgen thread at once.
 */
public final class TerraKernel implements RegionTile.Source {

    private final long seed;
    private final GenParams p;
    private final Continents continents;
    private final Tectonics tectonics;
    private final Volcanism volcanism;
    private final Geology geology;
    private final Caves caves;
    private final TileCache tiles;

    private final Noise relief;
    private final Noise detail;
    private final Noise micro;
    private final Noise wind;
    private final Noise precip;
    private final Noise cone;
    private final Noise aeolian;

    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    /** Per-thread reusable temporaries. */
    private static final class Scratch {
        final Tectonics.Setting setting = new Tectonics.Setting();
        final Volcanism.Edifice edifice = new Volcanism.Edifice();
        final BiomeClassifier.Env env = new BiomeClassifier.Env();
        final RegionTile.TileSample sample = new RegionTile.TileSample();
        final Arid.Dunes dunes = new Arid.Dunes();
        final double[] windVec = new double[2];
    }

    public TerraKernel(long seed, GenParams params) {
        this.seed = seed;
        this.p = params;
        this.continents = new Continents(seed, params);
        this.tectonics = new Tectonics(seed, params);
        this.volcanism = new Volcanism(seed, params);
        this.geology = new Geology(seed, params);
        this.caves = new Caves(seed, params);
        this.tiles = new TileCache(this, params.tileCacheSize);

        this.relief = new Noise(seed ^ 0x45D9F3BL);
        this.detail = new Noise(seed ^ 0x27D4EB2FL);
        this.micro = new Noise(seed ^ 0x165667B1L);
        this.wind = new Noise(seed ^ 0x1B56C4E9L);
        this.precip = new Noise(seed ^ 0x9E3779B1L);
        this.cone = new Noise(seed ^ 0x4A571B3L);
        this.aeolian = new Noise(seed ^ 0x68E31DA4L);
    }

    // ------------------------------------------------------------ accessors

    public long seed() {
        return seed;
    }

    @Override
    public GenParams params() {
        return p;
    }

    public Continents continents() {
        return continents;
    }

    @Override
    public Tectonics tectonics() {
        return tectonics;
    }

    @Override
    public Volcanism volcanism() {
        return volcanism;
    }

    @Override
    public Geology geology() {
        return geology;
    }

    public Caves caves() {
        return caves;
    }

    @Override
    public Noise windNoise() {
        return wind;
    }

    @Override
    public Noise precipNoise() {
        return precip;
    }

    @Override
    public Noise coneNoise() {
        return cone;
    }

    public Noise aeolianNoise() {
        return aeolian;
    }

    public TileCache tileCache() {
        return tiles;
    }

    public int tileBlocks() {
        return p.tileCells * p.erosionCellSize;
    }

    // ------------------------------------------------------ analytic surface

    @Override
    public double continentalness(double x, double z) {
        return continents.continentalness(x, z);
    }

    /**
     * Pre-erosion uplift surface in blocks. Never allocates.
     *
     * <p>This is the field that the climate model marches upwind through, that the erosion grid starts
     * from, and that answers {@code getBaseHeight}.
     */
    @Override
    public double upliftHeight(int x, int z) {
        Scratch s = SCRATCH.get();
        tectonics.sampleInto(x, z, s.setting);
        double y = continents.tectonicBaseHeight(x, z, s.setting, relief);
        volcanism.sample(x, z, s.setting.arc, s.setting.rift, s.edifice);
        y += s.edifice.height;
        return Interp.clamp(y, p.minY + 3, p.maxY - 6);
    }

    public double upliftHeight(int x, int z, Volcanism.Edifice edificeOut) {
        Scratch s = SCRATCH.get();
        tectonics.sampleInto(x, z, s.setting);
        double y = continents.tectonicBaseHeight(x, z, s.setting, relief);
        volcanism.sample(x, z, s.setting.arc, s.setting.rift, s.edifice);
        edificeOut.height = s.edifice.height;
        edificeOut.vent = s.edifice.vent;
        edificeOut.crater = s.edifice.crater;
        edificeOut.rim = s.edifice.rim;
        edificeOut.coneKind = s.edifice.coneKind;
        edificeOut.radial = s.edifice.radial;
        edificeOut.fresh = s.edifice.fresh;
        y += s.edifice.height;
        return Interp.clamp(y, p.minY + 3, p.maxY - 6);
    }

    // ------------------------------------------------------------- tile access

    public RegionTile tileFor(int blockX, int blockZ) {
        return tiles.tile(tiles.tileIndex(blockX), tiles.tileIndex(blockZ));
    }

    // -------------------------------------------------------------- the column

    /**
     * Fills {@code out} with everything known about the column at {@code (x, z)}.
     *
     * <p>This is the single entry point the block placer and the biome source share, which is what keeps
     * the two consistent: a biome can never disagree with the terrain it is sitting on, because both are
     * derived from the same column.
     */
    public void column(int x, int z, Column out) {
        out.reset();
        Scratch s = SCRATCH.get();
        RegionTile tile = tileFor(x, z);
        RegionTile.TileSample ts = s.sample;
        tile.sample(x, z, ts);

        out.continentalness = continents.continentalness(x, z);

        // --------------------------------------------------------- climate
        double elevM = (ts.height - p.seaLevel) * p.metersPerBlock;
        double tempC = ts.tempBase - p.lapseRateCPerKm * elevM / 1000.0;
        double precip = ts.precipMm;
        double aridityAI = ts.aridityIndex;
        double aridity = Interp.clamp((aridityAI - 0.55) / 3.2, 0.0, 1.0);
        aridity = Math.max(aridity, Interp.smoothstep(0.45, 0.95, ts.continentality) * 0.30);
        out.latitude = ts.latitude;
        out.tempC = tempC;
        out.precipMm = precip;
        out.aridity = aridity;
        out.elevationM = elevM;
        out.snowLineM = ts.snowLineM;
        out.sri = Climate.solarIndex(out.latitude, Math.toRadians(ts.slope), ts.aspect);
        out.aridity = aridity;

        // ------------------------------------------------------ morphometry
        out.slopeDeg = ts.slope;
        out.aspect = ts.aspect;
        out.curvature = ts.curvature;
        out.deposition = ts.deposition;
        out.channel = ts.channel;
        out.acc = ts.acc;
        out.channelDepth = ts.channelDepth;
        out.fan = ts.fan;
        out.playa = ts.playa;
        out.lavaFlow = ts.lavaFlow;
        out.sandSupply = ts.sandSupply;
        out.delta = ts.delta;
        out.glacier = ts.glacier;
        out.karst = ts.karst;
        out.swallow = ts.swallow;
        out.oceanDistance = ts.oceanDistance;
        out.aridity = aridity;

        // Wind exposure: high, steep and facing into the prevailing wind.
        double dot = Math.cos(ts.aspect) * ts.windX + Math.sin(ts.aspect) * ts.windZ;
        out.windX = ts.windX;
        out.windZ = ts.windZ;
        out.windExposure = Interp.clamp(
                0.42 * Interp.smoothstep(220.0, 1500.0, elevM)
                        + 0.30 * Interp.smoothstep(13.0, 42.0, ts.slope)
                        + 0.28 * Math.max(0.0, -dot), 0.0, 1.0);
        out.treeLineM = BiomeClassifier.treeLineMeters(Math.abs(ts.latitude), precip, out.windExposure);

        // ---------------------------------------------------------- geology
        tectonics.sampleInto(x, z, s.setting);
        double glacialPotential = ts.glacialPotential
                * Interp.smoothstep(ts.snowLineM - 900.0, ts.snowLineM - 120.0, elevM);
        double depositional = Interp.clamp(ts.deposition / 6.0, 0.0, 1.0);
        geology.site(x, z, out.continentalness, s.setting, ts.slope, tempC, aridityAI, precip,
                glacialPotential, ts.channel, depositional, out.site);
        out.rock = out.site.rock;
        out.soil = out.site.soil;
        out.soilThickness = out.site.soilThickness;
        out.regolith = out.site.regolith;
        out.outcrop = out.site.outcrop;
        out.karst = out.site.karst;
        out.fracture = out.site.fracture;
        out.softness = ts.softness;
        out.dip = out.site.dip;
        out.strike = out.site.strike;
        out.strataSpacing = out.site.strataSpacing;

        // ------------------------------------------------- volcanic surface
        volcanism.sample(x, z, s.setting.arc, s.setting.rift, s.edifice);
        out.volcanoVent = s.edifice.vent;
        out.volcanoFresh = Math.max(s.edifice.fresh, ts.lavaFlow);

        // -------------------------------------------------- final surface Y
        double rough = Interp.clamp(
                0.22
                        + Interp.smoothstep(6.0, 34.0, ts.slope) * 0.85
                        + aridity * out.site.rock.primary.hardness * 0.30
                        + (1.0 - Interp.smoothstep(0.5, 2.5, out.soilThickness)) * 0.45, 0.0, 1.9);
        double microRelief = p.microRelief * rough;
        double microVal = micro.fbm2(x / p.detailScale, z / p.detailScale, p.detailOctaves,
                2.0, 0.5) * microRelief;
        // Bedding-parallel ridges: in badlands the micro relief aligns with the strata, which is what
        // turns a smooth slope into the ribbed surface of a painted canyon wall.
        double painted = Arid.paintedStrata(aridity, ts.slope, out.outcrop,
                out.site.rock.bedded(), ts.softness, p);
        out.painted = painted;
        if (painted > 0.05) {
            double u = (x * Math.cos(out.strike) + z * Math.sin(out.strike)) / 7.5;
            double rib = 0.5 + 0.5 * detail.ridged2(u, (x * Math.sin(out.strike)
                    - z * Math.cos(out.strike)) / 26.0, 2, 2.0, 0.5);
            microVal += painted * 2.6 * (rib - 0.5);
        }

        double surface = ts.height + microVal;

        // Wave-cut notch at the waterline on steep coasts.
        double waterDepthAt = ts.waterLevel == Erosion.Grid.NO_WATER ? 0.0 : ts.waterLevel - surface;
        double notch = caves.seaCaveNotch(x, z, (int) Math.floor(surface), ts.slope,
                Math.max(0.0, waterDepthAt), p);
        surface -= notch;

        out.surfaceY = (int) Math.floor(surface);
        out.surfaceY = Interp.clamp(out.surfaceY, p.minY + 1, p.maxY - 12);

        // ------------------------------------------------------------- water
        int declaredWater = ts.waterLevel == Erosion.Grid.NO_WATER
                ? Column.NO_WATER : (int) Math.floor(ts.waterLevel);
        if (declaredWater != Column.NO_WATER && declaredWater > out.surfaceY) {
            out.waterY = declaredWater;
            out.waterDepth = ts.waterLevel - surface;
            out.marine = out.continentalness < 0.02 && ts.oceanDistance > 24;
            out.river = !out.marine && ts.channel > 0.18;
            out.lake = !out.marine && !out.river;
        } else if (out.surfaceY < p.seaLevel
                && ts.oceanDistance * p.erosionCellSize < 240.0) {
            // Below sea level, no local water body, and within a stone's throw of open ocean: that is
            // the sea itself, which is the base level and not a lake the hydrology has to discover.
            // The distance gate matters: a closed basin 100 m under sea level in the middle of a
            // continent (Salton Trough, Turfan, the Dead Sea) is endorheic, dry and often full of
            // dunes - calling it "marine" would flood it and delete its desert.
            out.waterY = p.seaLevel;
            out.waterDepth = p.seaLevel - surface;
            out.marine = true;
            out.river = false;
            out.lake = false;
        }
        if (ts.waterTable != Erosion.Grid.NO_WATER) {
            out.waterTableY = (int) Math.floor(ts.waterTable);
        }
        out.saturation = Interp.clamp(
                Math.max(out.waterTableY == Column.NO_WATER ? 0.0
                                : 1.0 - (out.surfaceY - out.waterTableY) / 7.0,
                        out.waterY == Column.NO_WATER ? 0.0 : 1.0), 0.0, 1.0);

        // -------------------------------------------------------- aeolian
        // Aeolian transport is a subaerial process: no dunes under the sea. The gate is "marine", not
        // "below sea level", because endorheic desert basins sit below sea level and carry some of the
        // finest dune fields on Earth (the Imperial and Cuatro Cienegas sands).
        if (!out.marine) {
            Arid.dunes(x, z, ts.windX, ts.windZ, ts.sandSupply, aridity, ts.slope, p, aeolian, s.dunes);
            out.duneHeight = s.dunes.height;
            out.duneSlipFace = s.dunes.slipFace;
            if (out.duneHeight > 0.05) {
                out.surfaceY = Interp.clamp(out.surfaceY + (int) Math.round(out.duneHeight),
                        p.minY + 1, p.maxY - 12);
            }
        }
        out.playa = ts.playa;
        out.permafrost = tempC < -3.5 && glacialPotential < 0.6;

        // ------------------------------------------------------- riparian
        out.riparian = Arid.riparian(aridity, ts.channel, out.saturation, ts.slope, p);
        out.pavement = Arid.pavement(aridity, out.soilThickness, ts.slope);
        out.boulders = Arid.boulderiness(x, z, ts.slope, aridity, out.site.rock.primary.hardness);

        // ------------------------------------------------------------ biome
        BiomeClassifier.Env e = s.env;
        e.x = x;
        e.z = z;
        e.latitude = out.latitude;
        e.surfaceY = out.surfaceY;
        e.waterY = out.waterY;
        e.waterDepth = out.waterDepth;
        e.marine = out.marine;
        e.river = out.river;
        e.lake = out.lake;
        e.tempC = out.tempC;
        e.precipMm = out.precipMm;
        e.aridity = aridityAI;
        e.sri = out.sri;
        e.elevationM = out.elevationM;
        e.snowLineM = out.snowLineM;
        e.treeLineM = out.treeLineM;
        e.slopeDeg = out.slopeDeg;
        e.glacier = out.glacier;
        e.outcrop = out.outcrop;
        e.channel = out.channel;
        e.delta = out.delta;
        e.volcano = out.volcanoFresh;
        e.karst = out.karst;
        e.continentalness = out.continentalness;
        e.oceanDistanceBlocks = out.oceanDistance;
        e.permafrost = out.permafrost;
        e.windExposure = out.windExposure;
        e.saturation = out.saturation;
        e.rock = out.rock;
        e.soil = out.soil;
        e.params = p;
        out.biome = BiomeClassifier.classify(e);
        // Riparian override: a green corridor through an otherwise bare desert.
        boolean actuallyWet = out.waterY != Column.NO_WATER || out.saturation > 0.72
                || out.channel > 0.72;
        if (out.riparian > 0.45 && actuallyWet
                && out.biome != BiomeKind.RIVER && out.biome != BiomeKind.FROZEN_RIVER) {
            out.biome = out.tempC < 1.0 ? BiomeKind.FROZEN_RIVER : BiomeKind.RIVER;
        }

        // -------------------------------------------------------- cave context
        Caves.Ctx c = out.caveCtx;
        c.surfaceY = out.surfaceY;
        c.waterTableY = out.waterTableY == Column.NO_WATER ? out.surfaceY - 1 : out.waterTableY;
        c.lavaLevel = p.lavaLevel;
        c.minY = p.minY;
        c.karst = out.karst;
        c.fracture = out.fracture;
        c.volcano = out.volcanoFresh;
        c.dip = out.dip;
        c.strike = out.strike;
        c.strataSpacing = out.strataSpacing;
        c.bedded = out.rock.bedded();
        c.hardness = out.rock.primary.hardness;
    }

    /** Biome at an arbitrary point, for the biome source. */
    public BiomeKind biomeAt(int x, int y, int z, Column scratch) {
        column(x, z, scratch);
        if (y >= scratch.surfaceY) {
            return scratch.biome;
        }
        Scratch s = SCRATCH.get();
        return BiomeClassifier.subterranean(s.env, y);
    }

    /**
     * Direct cave evaluation, no lattice. Cheap enough for a cross-section, too slow for a whole chunk;
     * the Minecraft layer builds a {@link LatticeCaves} instead.
     */
    public final class DirectCaves implements Stratigrapher.CaveSampler {
        @Override
        public boolean voidAt(int x, int y, int z, Column col) {
            double d = caves.density(x, y, z, col.caveCtx);
            return d > caves.threshold(col.surfaceY - y, col.karst);
        }
    }

    /**
     * Cave evaluation through a coarse 3-D lattice with trilinear interpolation.
     *
     * <p>This is the same trick vanilla uses for its density grid, and it is what keeps cave generation
     * affordable: the void field is smooth at the scale of the lattice, so evaluating it every
     * {@code caveCellSize} blocks and interpolating is visually indistinguishable from evaluating it per
     * block at a fraction of the cost.
     *
     * <p>Not thread safe — build one per chunk on the generating thread and reuse it.
     */
    public static final class LatticeCaves implements Stratigrapher.CaveSampler {
        private final TerraKernel k;
        private final float[] grid;
        private final int step;
        private final Caves.Ctx ctx = new Caves.Ctx();
        private int baseX;
        private int baseY;
        private int baseZ;
        private int nx;
        private int ny;
        private int nz;

        public LatticeCaves(TerraKernel kernel) {
            this.k = kernel;
            this.step = Math.max(2, kernel.params().caveCellSize);
            int nxz = 16 / step + 3;
            int nyy = (kernel.params().maxY - kernel.params().minY) / step + 3;
            this.grid = new float[nxz * nxz * nyy];
        }

        /**
         * @param columns 16 x 16 columns of the chunk, indexed {@code columns[cz * 16 + cx]}
         */
        public void prepare(int chunkMinX, int chunkMinZ, Column[] columns, int minY, int maxY) {
            this.baseX = chunkMinX;
            this.baseZ = chunkMinZ;
            this.baseY = minY;
            this.nx = 16 / step + 1;
            this.nz = 16 / step + 1;
            this.ny = (maxY - minY) / step + 1;
            int sx = nx + 1;
            int strideY = ny + 1;
            for (int gz = 0; gz <= nz; gz++) {
                for (int gx = 0; gx <= nx; gx++) {
                    int bx = chunkMinX + gx * step;
                    int bz = chunkMinZ + gz * step;
                    Column col = columns[Interp.clamp(gz * step, 0, 15) * 16
                            + Interp.clamp(gx * step, 0, 15)];
                    int base = (gz * sx + gx) * strideY;
                    if (col == null) {
                        for (int gy = 0; gy <= ny; gy++) {
                            grid[base + gy] = -1f;
                        }
                        continue;
                    }
                    Caves.Ctx c = col.caveCtx;
                    ctx.surfaceY = c.surfaceY;
                    ctx.waterTableY = c.waterTableY;
                    ctx.lavaLevel = c.lavaLevel;
                    ctx.minY = c.minY;
                    ctx.karst = c.karst;
                    ctx.fracture = c.fracture;
                    ctx.volcano = c.volcano;
                    ctx.dip = c.dip;
                    ctx.strike = c.strike;
                    ctx.strataSpacing = c.strataSpacing;
                    ctx.bedded = c.bedded;
                    ctx.hardness = c.hardness;
                    for (int gy = 0; gy <= ny; gy++) {
                        int by = minY + gy * step;
                        if (by > ctx.surfaceY - 3) {
                            grid[base + gy] = -1f;
                            continue;
                        }
                        grid[base + gy] = (float) (k.caves.density(bx, by, bz, ctx)
                                - k.caves.threshold(ctx.surfaceY - by, ctx.karst));
                    }
                }
            }
        }

        @Override
        public boolean voidAt(int x, int y, int z, Column col) {
            if (y > col.surfaceY - 4) {
                return false;
            }
            double fx = (x - baseX) / (double) step;
            double fy = (y - baseY) / (double) step;
            double fz = (z - baseZ) / (double) step;
            if (fx < 0 || fy < 0 || fz < 0) {
                return false;
            }
            int x0 = (int) fx;
            int y0 = (int) fy;
            int z0 = (int) fz;
            if (x0 >= nx || y0 >= ny || z0 >= nz) {
                return false;
            }
            int sx = nx + 1;
            int strideY = ny + 1;
            double v = Interp.trilerp(fx - x0, fy - y0, fz - z0,
                    grid[(z0 * sx + x0) * strideY + y0],
                    grid[(z0 * sx + x0 + 1) * strideY + y0],
                    grid[(z0 * sx + x0) * strideY + y0 + 1],
                    grid[(z0 * sx + x0 + 1) * strideY + y0 + 1],
                    grid[((z0 + 1) * sx + x0) * strideY + y0],
                    grid[((z0 + 1) * sx + x0 + 1) * strideY + y0],
                    grid[((z0 + 1) * sx + x0) * strideY + y0 + 1],
                    grid[((z0 + 1) * sx + x0 + 1) * strideY + y0 + 1]);
            return v > 0.0;
        }
    }
}
