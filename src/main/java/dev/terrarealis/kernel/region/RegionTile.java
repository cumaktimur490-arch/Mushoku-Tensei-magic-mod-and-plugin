package dev.terrarealis.kernel.region;

import dev.terrarealis.kernel.Column;
import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.geo.Arid;
import dev.terrarealis.kernel.geo.Climate;
import dev.terrarealis.kernel.geo.Erosion;
import dev.terrarealis.kernel.geo.Geology;
import dev.terrarealis.kernel.geo.Glaciation;
import dev.terrarealis.kernel.geo.Karst;
import dev.terrarealis.kernel.geo.Tectonics;
import dev.terrarealis.kernel.geo.Volcanism;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;

/**
 * One simulated landscape tile: the erosion grid plus the climate field that drove it.
 *
 * <p>Erosion, glaciation, volcanism and karst are all <i>non-local</i> — the shape of a valley depends
 * on the whole catchment above it — so they cannot be evaluated per column the way noise can. They are
 * run on a tile that is larger than the area it is responsible for: a {@code tileCells} interior plus a
 * {@code haloCells} border. Only the interior is ever read back.
 *
 * <p>The halo is what buys correctness at the seams. Drainage that crosses a tile boundary is already
 * accumulated by the time the interior is reached, so a river does not restart at every tile edge.
 * Tile-based erosion can never be perfectly seamless — a catchment that begins outside the halo is
 * only partly seen — so the property that is actually guaranteed, and actually tested by
 * {@code KernelTests}, is statistical: the mean height difference across a seam stays around a block
 * and the 99th percentile stays in single digits, which is far below the scale at which a player can
 * perceive a discontinuity.
 *
 * <p>Climate is evaluated on a coarser sub-lattice whose step divides the tile size exactly, so the
 * sub-lattices of neighbouring tiles coincide and bilinear interpolation is continuous across the
 * boundary by construction.
 */
public final class RegionTile {

    /** Climate is sampled every this many erosion cells. Must divide {@code tileCells}. */
    public static final int CLIMATE_STEP = 8;
    /** Lithology and the uplift field are sampled every this many erosion cells. */
    public static final int PROVINCE_STEP = 2;
    /** Set true (e.g. from the diagnostic tool) to print min/max height after every pass. */
    public static boolean DEBUG = false;

    private static void trace(String stage, Erosion.Grid g) {
        if (!DEBUG) {
            return;
        }
        double mn = Double.POSITIVE_INFINITY;
        double mx = Double.NEGATIVE_INFINITY;
        for (double v : g.height) {
            mn = Math.min(mn, v);
            mx = Math.max(mx, v);
        }
        System.out.printf("  %-22s min %10.2f max %10.2f%n", stage, mn, mx);
    }

    /** Number of upwind march steps for the orographic model. */
    private static final int MARCH_STEPS = 22;
    private static final double MARCH_STEP_BLOCKS = 190.0;
    private static final double MARCH_DECAY = 0.895;

    public final int tileX;
    public final int tileZ;
    public final GenParams params;
    public final Erosion.Grid grid;
    public final long seed;

    private final int cw;
    private final float[] cTempBase;
    private final float[] cPrecip;
    private final float[] cAridity;
    private final float[] cContinentality;
    private final float[] cWindX;
    private final float[] cWindZ;
    private final float[] cSnowLineM;
    private final float[] cLatitude;
    private final float[] cGlacial;

    /** World block coordinate of the interior origin. */
    public final int originX;
    public final int originZ;

    private final int cell;

    public RegionTile(Source src, int tileX, int tileZ) {
        this.tileX = tileX;
        this.tileZ = tileZ;
        this.params = src.params();
        this.seed = src.seed();
        this.cell = params.erosionCellSize;
        this.originX = tileX * params.tileCells * cell;
        this.originZ = tileZ * params.tileCells * cell;

        // ---------------------------------------------------- uplift surface
        // The uplift field's finest component is the ~26 block detail octave, so sampling it every two
        // erosion cells (4 blocks) and interpolating loses nothing visible while cutting the most
        // expensive stage of the build by a factor of four.
        this.grid = createGridCoarse(params, tileX, tileZ, src);
        int w = grid.w;

        // ------------------------------------------------------- climate field
        int full = w / CLIMATE_STEP + 2;
        this.cw = full;
        this.cTempBase = new float[full * full];
        this.cPrecip = new float[full * full];
        this.cAridity = new float[full * full];
        this.cContinentality = new float[full * full];
        this.cWindX = new float[full * full];
        this.cWindZ = new float[full * full];
        this.cSnowLineM = new float[full * full];
        this.cLatitude = new float[full * full];
        this.cGlacial = new float[full * full];

        int gridOriginX = originX - params.haloCells * cell;
        int gridOriginZ = originZ - params.haloCells * cell;
        Climate.HeightSampler sampler = src::upliftHeightD;
        Climate.Orography oro = new Climate.Orography();
        Noise wind = src.windNoise();
        double[] wv = new double[2];

        for (int cz = 0; cz < full; cz++) {
            for (int cx = 0; cx < full; cx++) {
                int bx = gridOriginX + cx * CLIMATE_STEP * cell;
                int bz = gridOriginZ + cz * CLIMATE_STEP * cell;
                int i = cz * full + cx;

                double lat = Climate.latitudeAt(bz, params);
                double absLat = Math.abs(lat);
                double cont = src.continentalness(bx, bz);
                double continental = Climate.continentality(cont);

                Climate.windVector(lat, bx, bz, params, wind, wv);
                cWindX[i] = (float) wv[0];
                cWindZ[i] = (float) wv[1];
                cLatitude[i] = (float) lat;
                cContinentality[i] = (float) continental;

                Climate.marchUpwind(bx, bz, wv[0], wv[1], MARCH_STEP_BLOCKS, MARCH_STEPS,
                        MARCH_DECAY, sampler, params, oro);

                double zonalP = Climate.zonalPrecipMm(lat);
                // Moisture supply decays inland; the ocean is the only real source.
                double supply = Interp.lerp(1.0, 0.40, continental);
                // Regional wobble so that two places at the same latitude are not identical.
                double wobble = 0.72 + 0.56 * (0.5 + 0.5 * src.precipNoise().fbm2(
                        bx / params.precipNoiseScale + 51.0, bz / params.precipNoiseScale - 29.0,
                        3, 2.0, 0.5));
                double precip = params.basePrecipMm * (zonalP / 2450.0) * supply * wobble
                        * oro.shadowFactor * oro.enhancement;
                precip = Interp.clamp(precip, 18.0, 6500.0);
                cPrecip[i] = (float) precip;

                double tempBase = Climate.zonalTempC(lat);
                // Continental interiors swing hotter in summer and colder in winter; the annual mean
                // drifts up slightly because land heats faster than ocean.
                tempBase += continental * 1.6;
                tempBase += 2.2 * (0.5 + 0.5 * src.precipNoise().fbm2(
                        bx / (params.precipNoiseScale * 2.2) - 71.0,
                        bz / (params.precipNoiseScale * 2.2) + 13.0, 2, 2.0, 0.5)) - 1.1;
                // Warm ocean currents: coastal west sides of continents at low latitude stay mild.
                tempBase += (1.0 - continental) * (absLat < 35.0 ? 1.2 : -0.6);
                cTempBase[i] = (float) tempBase;

                cAridity[i] = (float) Climate.aridityIndex(tempBase, precip);
                double snowLine = Climate.snowLineMeters(absLat, precip);
                cSnowLineM[i] = (float) snowLine;

                // Active glaciation: cold enough, wet enough to accumulate, and far enough poleward.
                double glacial = Interp.smoothstep(params.glacialLatitude - 4.0,
                        params.glacialLatitude + 12.0, absLat);
                glacial *= Interp.smoothstep(120.0, 500.0, precip);
                cGlacial[i] = (float) Interp.clamp(glacial, 0.0, 1.0);
            }
        }

        // ------------------------------------------- per-cell derived fields
        double[] snowLineBlocks = new double[w * w];
        double[] precipCells = new double[w * w];
        double[] glacialCells = new double[w * w];
        double[] karstCells = new double[w * w];
        Geology.Site site = new Geology.Site();
        Tectonics.Setting tec = new Tectonics.Setting();
        int gridOriginCellX = -params.haloCells;

        // Lithology is a province-scale field (kilometres), so it too is sampled on the coarse lattice
        // and interpolated down to the erosion grid.
        int ps = PROVINCE_STEP;
        int pw = w / ps + 1;
        float[] pSoft = new float[pw * pw];
        float[] pKarst = new float[pw * pw];
        for (int cz = 0; cz < pw; cz++) {
            for (int cx = 0; cx < pw; cx++) {
                int gcx = Math.min(cx * ps, w - 1);
                int gcz = Math.min(cz * ps, w - 1);
                int gi = gcz * w + gcx;
                int bx = gridOriginX + gcx * cell;
                int bz = gridOriginZ + gcz * cell;
                double base = grid.base[gi];
                double tempSea = sampleBilinear(cTempBase, full, gcx, gcz, CLIMATE_STEP);
                double precip = sampleBilinear(cPrecip, full, gcx, gcz, CLIMATE_STEP);
                double aridity = sampleBilinear(cAridity, full, gcx, gcz, CLIMATE_STEP);
                double snowLineM = sampleBilinear(cSnowLineM, full, gcx, gcz, CLIMATE_STEP);
                double glacial = sampleBilinear(cGlacial, full, gcx, gcz, CLIMATE_STEP);
                double elevM = (base - params.seaLevel) * params.metersPerBlock;
                double tempC = tempSea - params.lapseRateCPerKm * elevM / 1000.0;
                double gl = glacial
                        * Interp.smoothstep(snowLineM - 700.0, snowLineM - 150.0, elevM);
                src.tectonics().sampleInto(bx, bz, tec);
                double cont = src.continentalness(bx, bz);
                src.geology().site(bx, bz, cont, tec, 0.0, tempC, aridity, precip, gl, 0.0, 0.0, site);
                pSoft[cz * pw + cx] = (float) Interp.clamp(1.0 - site.rock.primary.hardness, 0.0, 1.0);
                pKarst[cz * pw + cx] = (float) site.karst;
            }
        }

        for (int cz = 0; cz < w; cz++) {
            for (int cx = 0; cx < w; cx++) {
                int i = cz * w + cx;
                double base = grid.base[i];

                double tempSea = sampleBilinear(cTempBase, full, cx, cz, CLIMATE_STEP);
                double precip = sampleBilinear(cPrecip, full, cx, cz, CLIMATE_STEP);
                double aridity = sampleBilinear(cAridity, full, cx, cz, CLIMATE_STEP);
                double snowLineM = sampleBilinear(cSnowLineM, full, cx, cz, CLIMATE_STEP);
                double glacial = sampleBilinear(cGlacial, full, cx, cz, CLIMATE_STEP);
                double continental = sampleBilinear(cContinentality, full, cx, cz, CLIMATE_STEP);

                double elevM = (base - params.seaLevel) * params.metersPerBlock;

                precipCells[i] = precip;
                glacialCells[i] = glacial * Interp.smoothstep(snowLineM - 700.0, snowLineM - 150.0, elevM);
                snowLineBlocks[i] = params.seaLevel + snowLineM / params.metersPerBlock;

                grid.softness[i] = (float) sampleCoarse(pSoft, pw, cx, cz, ps);
                grid.karst[i] = (float) sampleCoarse(pKarst, pw, cx, cz, ps);
                karstCells[i] = grid.karst[i];
                // Aridity 0..1 from the UNEP index, for the erosion regime switch.
                double ar = Interp.clamp((aridity - 0.55) / 3.2, 0.0, 1.0);
                grid.aridity[i] = (float) Math.max(ar,
                        Interp.smoothstep(0.45, 0.95, continental) * 0.35);
            }
        }

        // ------------------------------------------------------------ passes
        trace("createGrid", grid);
        Erosion.erode(grid);
        trace("erode", grid);
        if (params.glaciation) {
            Glaciation.apply(grid, snowLineBlocks, precipCells, 1.0);
        }
        trace("glaciation", grid);
        Volcanism.lavaFlows(grid, src.volcanism(), src.tectonics());
        trace("lavaFlows", grid);
        Karst.apply(grid, karstCells, precipCells, src.coneNoise(), seed ^ 0x4A571L, params);
        trace("karst", grid);
        Arid.alluvialFans(grid);
        trace("alluvialFans", grid);
        Arid.sandSupply(grid);
        Erosion.hydrology(grid);
        trace("hydrology", grid);

        this.precipAtCells = precipCells;
        this.glacialAtCells = glacialCells;
    }

    private final double[] precipAtCells;
    private final double[] glacialAtCells;

    /** Everything {@link RegionTile} needs from the kernel, so the tile can be built without cycles. */
    public interface Source {
        long seed();

        GenParams params();

        double upliftHeight(int x, int z);

        default double upliftHeightD(double x, double z) {
            return upliftHeight((int) Math.round(x), (int) Math.round(z));
        }

        double continentalness(double x, double z);

        Tectonics tectonics();

        Volcanism volcanism();

        Geology geology();

        Noise windNoise();

        Noise precipNoise();

        Noise coneNoise();
    }

    /**
     * Builds the erosion grid from the analytic uplift field, sampling it on a coarse lattice and
     * interpolating. The uplift surface has no structure below about 8 blocks, so a 4-block lattice is
     * exact to within a fraction of a centimetre of relief.
     */
    private static Erosion.Grid createGridCoarse(GenParams params, int tileX, int tileZ, Source src) {
        int w = params.gridWidth();
        int halo = params.haloCells;
        int cell = params.erosionCellSize;
        int originX = tileX * params.tileCells - halo;
        int originZ = tileZ * params.tileCells - halo;
        int ps = PROVINCE_STEP;
        int lw = w / ps + 2;
        double[] lat = new double[lw * lw];
        for (int lz = 0; lz < lw; lz++) {
            for (int lx = 0; lx < lw; lx++) {
                int bx = (originX + lx * ps) * cell;
                int bz = (originZ + lz * ps) * cell;
                lat[lz * lw + lx] = src.upliftHeight(bx, bz);
            }
        }
        double[] arr = new double[w * w];
        for (int cz = 0; cz < w; cz++) {
            int z0 = cz / ps;
            double fz = (cz - z0 * ps) / (double) ps;
            if (z0 > lw - 2) {
                z0 = lw - 2;
                fz = 1.0;
            }
            for (int cx = 0; cx < w; cx++) {
                int x0 = cx / ps;
                double fx = (cx - x0 * ps) / (double) ps;
                if (x0 > lw - 2) {
                    x0 = lw - 2;
                    fx = 1.0;
                }
                arr[cz * w + cx] = Interp.bilerp(fx, fz,
                        lat[z0 * lw + x0], lat[z0 * lw + x0 + 1],
                        lat[(z0 + 1) * lw + x0], lat[(z0 + 1) * lw + x0 + 1]);
            }
        }
        return Erosion.gridFromBase(params, tileX, tileZ, arr);
    }

    private static double sampleCoarse(float[] arr, int pw, int cx, int cz, int step) {
        double gx = cx / (double) step;
        double gz = cz / (double) step;
        int x0 = Interp.clamp((int) Math.floor(gx), 0, pw - 2);
        int z0 = Interp.clamp((int) Math.floor(gz), 0, pw - 2);
        return Interp.bilerp(Interp.clamp(gx - x0, 0.0, 1.0), Interp.clamp(gz - z0, 0.0, 1.0),
                arr[z0 * pw + x0], arr[z0 * pw + x0 + 1],
                arr[(z0 + 1) * pw + x0], arr[(z0 + 1) * pw + x0 + 1]);
    }

    private static double sampleBilinear(float[] arr, int w, int cx, int cz, int step) {
        double gx = (double) cx / step;
        double gz = (double) cz / step;
        int x0 = Interp.clamp((int) Math.floor(gx), 0, w - 2);
        int z0 = Interp.clamp((int) Math.floor(gz), 0, w - 2);
        double fx = Interp.clamp(gx - x0, 0.0, 1.0);
        double fz = Interp.clamp(gz - z0, 0.0, 1.0);
        return Interp.bilerp(fx, fz,
                arr[z0 * w + x0], arr[z0 * w + x0 + 1],
                arr[(z0 + 1) * w + x0], arr[(z0 + 1) * w + x0 + 1]);
    }

    // ------------------------------------------------------------- sampling

    /** Bilinearly samples every tile field at a block coordinate and writes it into {@code out}. */
    public void sample(int blockX, int blockZ, TileSample out) {
        int w = grid.w;
        double gx = (blockX - originX) / (double) cell + params.haloCells;
        double gz = (blockZ - originZ) / (double) cell + params.haloCells;
        int x0 = Interp.clamp((int) Math.floor(gx), 0, w - 2);
        int z0 = Interp.clamp((int) Math.floor(gz), 0, w - 2);
        double fx = gx - x0;
        double fz = gz - z0;
        int i00 = z0 * w + x0;
        int i10 = i00 + 1;
        int i01 = i00 + w;
        int i11 = i01 + 1;

        out.height = Interp.bilerp(fx, fz, grid.height[i00], grid.height[i10], grid.height[i01], grid.height[i11]);
        out.slope = bil(grid.slope, i00, i10, i01, i11, fx, fz);
        // Aspect is an angle: interpolate through its vector so 359deg and 1deg do not average to 180.
        double ax = bilCos(grid.aspect, i00, i10, i01, i11, fx, fz);
        double az = bilSin(grid.aspect, i00, i10, i01, i11, fx, fz);
        out.aspect = Math.atan2(az, ax);
        out.curvature = bil(grid.curvature, i00, i10, i01, i11, fx, fz);
        out.deposition = bil(grid.deposition, i00, i10, i01, i11, fx, fz);
        out.channel = bil(grid.channel, i00, i10, i01, i11, fx, fz);
        out.channelDepth = bil(grid.channelDepth, i00, i10, i01, i11, fx, fz);
        out.fan = bil(grid.fan, i00, i10, i01, i11, fx, fz);
        out.playa = bil(grid.playa, i00, i10, i01, i11, fx, fz);
        out.lavaFlow = bil(grid.lavaFlow, i00, i10, i01, i11, fx, fz);
        out.sandSupply = bil(grid.sandSupply, i00, i10, i01, i11, fx, fz);
        out.delta = bil(grid.delta, i00, i10, i01, i11, fx, fz);
        out.glacier = bil(grid.glacier, i00, i10, i01, i11, fx, fz);
        out.karst = bil(grid.karst, i00, i10, i01, i11, fx, fz);
        out.swallow = bil(grid.swallow, i00, i10, i01, i11, fx, fz);
        out.aridity = bil(grid.aridity, i00, i10, i01, i11, fx, fz);
        out.softness = bil(grid.softness, i00, i10, i01, i11, fx, fz);
        out.oceanDistance = bil(grid.oceanDistance, i00, i10, i01, i11, fx, fz) * cell;
        out.acc = bilD(grid.acc, i00, i10, i01, i11, fx, fz);

        // Water level: take the maximum of the four cells so a channel is never interpolated away.
        out.waterLevel = Grid.water(grid.waterLevel, i00, i10, i01, i11, fx, fz);
        out.waterTable = Grid.water(grid.waterTable, i00, i10, i01, i11, fx, fz);

        // Climate: bilinear over the coarse sub-lattice, expressed in grid-cell units.
        double ccx = gx / CLIMATE_STEP;
        double ccz = gz / CLIMATE_STEP;
        int cx0 = Interp.clamp((int) Math.floor(ccx), 0, cw - 2);
        int cz0 = Interp.clamp((int) Math.floor(ccz), 0, cw - 2);
        double cfx = ccx - cx0;
        double cfz = ccz - cz0;
        int c00 = cz0 * cw + cx0;
        int c10 = c00 + 1;
        int c01 = c00 + cw;
        int c11 = c01 + 1;
        out.tempBase = bil(cTempBase, c00, c10, c01, c11, cfx, cfz);
        out.precipMm = bil(cPrecip, c00, c10, c01, c11, cfx, cfz);
        out.aridityIndex = bil(cAridity, c00, c10, c01, c11, cfx, cfz);
        out.continentality = bil(cContinentality, c00, c10, c01, c11, cfx, cfz);
        out.snowLineM = bil(cSnowLineM, c00, c10, c01, c11, cfx, cfz);
        out.latitude = bil(cLatitude, c00, c10, c01, c11, cfx, cfz);
        out.glacialPotential = bil(cGlacial, c00, c10, c01, c11, cfx, cfz);
        out.windX = bil(cWindX, c00, c10, c01, c11, cfx, cfz);
        out.windZ = bil(cWindZ, c00, c10, c01, c11, cfx, cfz);
    }

    public double precipitation(int cellIndex) {
        return precipAtCells[cellIndex];
    }

    public double glacialPotential(int cellIndex) {
        return glacialAtCells[cellIndex];
    }

    private static double bil(float[] a, int i00, int i10, int i01, int i11, double fx, double fz) {
        return Interp.bilerp(fx, fz, a[i00], a[i10], a[i01], a[i11]);
    }

    private static double bilD(double[] a, int i00, int i10, int i01, int i11, double fx, double fz) {
        return Interp.bilerp(fx, fz, a[i00], a[i10], a[i01], a[i11]);
    }

    private static double bilCos(float[] a, int i00, int i10, int i01, int i11, double fx, double fz) {
        return Interp.bilerp(fx, fz, Math.cos(a[i00]), Math.cos(a[i10]), Math.cos(a[i01]), Math.cos(a[i11]));
    }

    private static double bilSin(float[] a, int i00, int i10, int i01, int i11, double fx, double fz) {
        return Interp.bilerp(fx, fz, Math.sin(a[i00]), Math.sin(a[i10]), Math.sin(a[i01]), Math.sin(a[i11]));
    }

    /** Handles the NO_WATER sentinel while interpolating. */
    private static final class Grid {
        static double water(double[] a, int i00, int i10, int i01, int i11, double fx, double fz) {
            double v00 = a[i00];
            double v10 = a[i10];
            double v01 = a[i01];
            double v11 = a[i11];
            if (v00 == Erosion.Grid.NO_WATER && v10 == Erosion.Grid.NO_WATER
                    && v01 == Erosion.Grid.NO_WATER && v11 == Erosion.Grid.NO_WATER) {
                return Erosion.Grid.NO_WATER;
            }
            // Replace missing corners with the lowest present value so the interpolation cannot invent
            // water where there is none, and cannot drag a channel's level down to the sentinel.
            double fallback = Erosion.Grid.NO_WATER;
            fallback = v00 != Erosion.Grid.NO_WATER ? v00 : fallback;
            fallback = v10 != Erosion.Grid.NO_WATER ? v10 : fallback;
            fallback = v01 != Erosion.Grid.NO_WATER ? v01 : fallback;
            fallback = v11 != Erosion.Grid.NO_WATER ? v11 : fallback;
            double a00 = v00 == Erosion.Grid.NO_WATER ? fallback : v00;
            double a10 = v10 == Erosion.Grid.NO_WATER ? fallback : v10;
            double a01 = v01 == Erosion.Grid.NO_WATER ? fallback : v01;
            double a11 = v11 == Erosion.Grid.NO_WATER ? fallback : v11;
            double bil = Interp.bilerp(fx, fz, a00, a10, a01, a11);
            // Only report water if at least one contributing cell is strong enough.
            double strongest = Math.max(Math.max(v00, v10), Math.max(v01, v11));
            return Math.min(bil, strongest);
        }
    }

    /** All the interpolated fields for one block column. */
    public static final class TileSample {
        public double height;
        public double slope;
        public double aspect;
        public double curvature;
        public double deposition;
        public double channel;
        public double channelDepth;
        public double fan;
        public double playa;
        public double lavaFlow;
        public double sandSupply;
        public double delta;
        public double glacier;
        public double karst;
        public double swallow;
        public double aridity;
        public double softness;
        public double oceanDistance;
        public double acc;
        public double waterLevel = Erosion.Grid.NO_WATER;
        public double waterTable = Erosion.Grid.NO_WATER;
        public double tempBase;
        public double precipMm;
        public double aridityIndex;
        public double continentality;
        public double snowLineM;
        public double latitude;
        public double glacialPotential;
        public double windX;
        public double windZ;
    }

}
