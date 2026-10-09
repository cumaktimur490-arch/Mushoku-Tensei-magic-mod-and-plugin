package dev.terrarealis.kernel.geo;

import java.util.PriorityQueue;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.math.Hash;
import dev.terrarealis.kernel.math.Interp;

/**
 * Landscape evolution on a raster: drainage extraction, fluvial incision, sediment transport,
 * hillslope failure, isostatic rebound and channel migration.
 *
 * <p>This is the stage that turns "a noise field that looks like terrain" into terrain. Without it you
 * get the two unmistakable tells of procedural landscapes: drainage that does not form a tree, and
 * valleys that are the same shape as the ridges between them. Real valleys are cut by water flowing
 * downhill through a hierarchy of tributaries, and real hillslopes are rounded off by mass wasting at
 * the angle of repose. Both are simulated here.
 *
 * <h2>Model</h2>
 * <ol>
 *   <li><b>Depression filling</b> — priority-flood (Barnes et al. 2014). Every cell gets a guaranteed
 *       downhill path to the grid edge or to a lake spill point. The difference between the filled and
 *       original surface is exactly the set of endorheic basins, i.e. lakes.</li>
 *   <li><b>Flow routing</b> — D8 steepest descent with diagonal length correction, then flow
 *       accumulation by processing cells in descending height order. Drainage area is what decides
 *       whether a cell is a rill, a stream, a river or a flood plain, via hydraulic-geometry scaling.</li>
 *   <li><b>Fluvial incision</b> — stream-power law {@code E = K A^m S^n}. Incision is capped so a cell
 *       can never cut below its receiver, which is what makes the network stable instead of oscillating.</li>
 *   <li><b>Sediment transport</b> — carrying capacity {@code C = Kc A^m S^n}; where capacity drops
 *       below the incoming load the excess is deposited. This is what builds alluvial fans at range
 *       fronts, flood plains in gradients below the equilibrium slope, and deltas at river mouths.</li>
 *   <li><b>Hillslope diffusion / talus</b> — any slope steeper than the angle of repose of the local
 *       rock collapses, moving material to the neighbours. Produces convex-summit / concave-footslope
 *       profiles, scree aprons below cliffs, and rounded hilltops.</li>
 *   <li><b>Isostatic rebound</b> — Airy isostasy: removing crust lets the remainder float higher.
 *       Applied as a fraction of the total denudation, so deeply incised ranges stand tall instead of
 *       being planed flat (real orogens reach exactly this dynamic equilibrium).</li>
 *   <li><b>Channel migration</b> — in low-gradient reaches, helicoidal flow erodes the outer bank and
 *       deposits a point bar on the inner bank. Meanders grow, necks narrow, and abandoned channels
 *       survive as oxbow scars.</li>
 * </ol>
 *
 * <p>The grid includes a halo so that drainage entering a tile from outside is accounted for; only the
 * interior is ever read back. See {@code docs/PIPELINE.md} for the seam analysis.
 */
public final class Erosion {

    /** Diagnostic: print min/max after every stage of {@link #erode} and {@link #hydrology}. */
    public static boolean DEBUG = false;

    private static void trace(String stage, Grid g) {
        if (!DEBUG) {
            return;
        }
        double mn = Double.POSITIVE_INFINITY;
        double mx = Double.NEGATIVE_INFINITY;
        for (double v : g.height) {
            mn = Math.min(mn, v);
            mx = Math.max(mx, v);
        }
        System.out.printf("    %-18s min %10.2f max %10.2f%n", stage, mn, mx);
    }


    /** D8 neighbour offsets. Package private so {@link Glaciation} can share the stencil. */
    static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    static final int[] DZ = {0, 0, 1, -1, 1, -1, 1, -1};
    static final double[] DIST = {1, 1, 1, 1, Math.sqrt(2), Math.sqrt(2), Math.sqrt(2), Math.sqrt(2)};

    /** All the per-cell results of the simulation. */
    public static final class Grid {
        public final int w;
        public final int halo;
        public final int interior;
        public final GenParams params;

        /** Uneroded uplift surface, blocks. */
        public final double[] base;
        /** Final surface after erosion, blocks. */
        public final double[] height;
        /** Depression-filled surface (used to find lakes). */
        public final double[] filled;
        /** Flow accumulation in cells (1 = the cell itself). */
        public final double[] acc;
        /** D8 flow direction index, or -1 for a sink/outlet. */
        public final int[] dir;
        /** Longest downslope gradient, degrees. */
        public final float[] slope;
        /** Downslope aspect, radians, 0 = +x (east), PI/2 = +z (south). */
        public final float[] aspect;
        /** Profile curvature: positive convex (summits), negative concave (valleys). */
        public final float[] curvature;
        /** Net deposition in blocks since the uplift surface (>0 = aggradation). */
        public final float[] deposition;
        /** 0..1 channel strength, splatted with the channel half-width. */
        public final float[] channel;
        /** Channel depth in blocks below the pre-carve surface. */
        public final float[] channelDepth;
        /** Standing water level in blocks, or {@link #NO_WATER}. */
        public final double[] waterLevel;
        /** Approximated water table level in blocks. */
        public final double[] waterTable;
        /** Distance to the nearest ocean cell, in blocks (bounded by the grid). */
        public final float[] oceanDistance;
        /** 0..1 delta / river-mouth deposit. */
        public final float[] delta;
        /** 0..1 active glaciation flux, for U-valley carving. */
        public final float[] glacier;
        /** Total isostatic rebound applied, blocks. */
        public final float[] denudation;
        /** 0 humid .. 1 hyperarid. Set by the climate stage before erosion runs. */
        public final float[] aridity;
        /** 0 hard crystalline rock .. 1 soft unconsolidated sediment. */
        public final float[] softness;
        /** 0..1 alluvial fan / bajada apron. */
        public final float[] fan;
        /** 0..1 playa (dry lake) floor. */
        public final float[] playa;
        /** 0..1 active lava flow. */
        public final float[] lavaFlow;
        /** 0..1 aeolian sand supply available for dune building. */
        public final float[] sandSupply;
        /** 0..1 karst index of the bedrock at this cell. */
        public final float[] karst;
        /** 0..1 — a swallow hole (ponor) or the dry reach downstream of one. */
        public final float[] swallow;
        /** Per-cell flow-accumulation needed before a channel forms. */
        public final double[] channelThreshold;
        /** Reused by the talus pass so worldgen does not allocate per cell. */
        double[] talusScratch;

        public static final double NO_WATER = -1e9;

        Grid(int w, int halo, GenParams params, double[] base) {
            this.w = w;
            this.halo = halo;
            this.interior = w - 2 * halo;
            this.params = params;
            int n = w * w;
            this.base = base;
            this.height = base.clone();
            this.filled = new double[n];
            this.acc = new double[n];
            this.dir = new int[n];
            this.slope = new float[n];
            this.aspect = new float[n];
            this.curvature = new float[n];
            this.deposition = new float[n];
            this.channel = new float[n];
            this.channelDepth = new float[n];
            this.waterLevel = new double[n];
            this.waterTable = new double[n];
            this.oceanDistance = new float[n];
            this.delta = new float[n];
            this.glacier = new float[n];
            this.denudation = new float[n];
            this.aridity = new float[n];
            this.softness = new float[n];
            this.fan = new float[n];
            this.playa = new float[n];
            this.lavaFlow = new float[n];
            this.sandSupply = new float[n];
            this.karst = new float[n];
            this.swallow = new float[n];
            this.channelThreshold = new double[n];
            this.talusScratch = new double[n];
            java.util.Arrays.fill(this.channelThreshold, params.riverThreshold);
            java.util.Arrays.fill(waterLevel, NO_WATER);
            java.util.Arrays.fill(waterTable, NO_WATER);
        }

        public int index(int cx, int cz) {
            return cz * w + cx;
        }

        public boolean inBounds(int cx, int cz) {
            return cx >= 0 && cz >= 0 && cx < w && cz < w;
        }
    }

    private Erosion() {}

    public static Grid createGrid(GenParams p, int tileX, int tileZ, HeightAt base) {
        int w = p.gridWidth();
        int halo = p.haloCells;
        int cell = p.erosionCellSize;
        double[] arr = new double[w * w];
        int originX = tileX * p.tileCells - halo;
        int originZ = tileZ * p.tileCells - halo;
        for (int cz = 0; cz < w; cz++) {
            for (int cx = 0; cx < w; cx++) {
                int bx = (originX + cx) * cell;
                int bz = (originZ + cz) * cell;
                arr[cz * w + cx] = base.heightAt(bx, bz);
            }
        }
        return new Grid(w, halo, p, arr);
    }

    /**
     * Wraps a pre-computed base-height array as a grid. Used when the caller wants to sample the uplift
     * field on a coarse lattice and interpolate, which is four times cheaper than sampling it per cell.
     */
    public static Grid gridFromBase(GenParams p, int tileX, int tileZ, double[] base) {
        return new Grid(p.gridWidth(), p.haloCells, p, base);
    }

    /** Supplies the uplift surface; implemented by {@code TerraKernel}. */
    public interface HeightAt {
        double heightAt(int x, int z);
    }

    // ------------------------------------------------------------------ driver

    /**
     * Denudation stage: drainage extraction, fluvial incision, sediment transport, hillslope failure
     * and isostatic rebound. Leaves the surface in a state where landform-specific passes (glacial,
     * volcanic, aeolian) can still modify it.
     */
    public static void erode(Grid g) {
        GenParams p = g.params;
        priorityFlood(g);
        computeFlow(g);
        computeAccumulation(g);

        double cellMeters = p.erosionCellSize * p.metersPerBlock;
        double talusRock = Math.tan(Math.toRadians(p.talusAngleDeg)) * p.erosionCellSize;
        double talusSoft = Math.tan(Math.toRadians(p.sedimentTalusDeg)) * p.erosionCellSize;
        double[] erodibility = erodibilityField(g);
        trace("flood/flow/acc", g);

        for (int it = 0; it < p.hydraulicIterations; it++) {
            if (it > 0) {
                computeFlow(g);
                computeAccumulation(g);
            }
            fluvialPass(g, erodibility, cellMeters, it);
            trace("fluvial " + it, g);
            talusPass(g, talusRock, talusSoft, erodibility);
            trace("talus " + it, g);
        }
        for (int it = 0; it < p.thermalIterations; it++) {
            talusPass(g, talusRock, talusSoft, erodibility);
            trace("thermal " + it, g);
        }

        isostaticRebound(g);
        trace("isostasy", g);
        priorityFlood(g);
        computeFlow(g);
        computeAccumulation(g);
    }

    /** Morphometry, channel carving, deltas, playas and the water table. Run last. */
    public static void hydrology(Grid g) {
        deriveMorphometry(g);
        oceanDistance(g);
        channelThresholdField(g);
        carveChannels(g);
        deltas(g);
        playas(g);
        waterTableField(g);
    }

    /**
     * Per-cell channel-initiation threshold.
     *
     * <p>Drainage density is the strongest single control on whether a landscape reads as temperate or
     * as desert. In humid terrain vegetation armours slopes, so channels only start once a substantial
     * catchment exists. On bare, soft sediment — the Borrego badlands, the Sonoran arroyos — every
     * rill survives, so the threshold collapses and the surface is dissected into a dense, sharp
     * gully-and-ridge lattice. On bare <i>hard</i> rock the opposite happens: runoff sheets off the
     * desert pavement and only a few deep, boulder-floored arroyos survive.
     */
    private static void channelThresholdField(Grid g) {
        GenParams p = g.params;
        for (int i = 0; i < g.channelThreshold.length; i++) {
            double ar = g.aridity[i];
            double soft = g.softness[i];
            double badland = ar * soft;
            double pavement = ar * (1.0 - soft);
            double scale = Interp.lerp(1.0, 0.16, Interp.smoothstep(0.15, 0.95, badland))
                    * Interp.lerp(1.0, 5.0, Interp.smoothstep(0.2, 1.0, pavement))
                    * Interp.lerp(1.0, 0.75, Interp.smoothstep(0.0, 0.6, (1.0 - ar) * soft));
            g.channelThreshold[i] = Math.max(40.0, p.riverThreshold * scale);
        }
    }

    /**
     * Arid endorheic basins become playas: the floor is planed flat, the water is ephemeral, and the
     * surface crusts over with evaporites. This is what turns a desert basin from "a dip in the noise"
     * into a salt flat with alluvial fans ringing it.
     */
    private static void playas(Grid g) {
        int w = g.w;
        GenParams p = g.params;
        // Height of the basin sink each cell drains to: one ascending sweep, since a cell's sink is its
        // receiver's sink or itself.
        int n = w * w;
        double[] sinkH = new double[n];
        long[] order = new long[n];
        for (int i = 0; i < n; i++) {
            order[i] = ascendingKey(g.height[i], i);
            sinkH[i] = g.height[i];
        }
        java.util.Arrays.sort(order);
        for (long k : order) {
            int i = keyIndex(k);
            int d = g.dir[i];
            if (d >= 0) {
                int cx = i % w;
                int cz = i / w;
                int j = (cz + DZ[d]) * w + (cx + DX[d]);
                if (g.height[j] < g.height[i]) {
                    sinkH[i] = Math.min(sinkH[i], sinkH[j]);
                }
            }
        }
        for (int cz = 2; cz < w - 2; cz++) {
            for (int cx = 2; cx < w - 2; cx++) {
                int i = cz * w + cx;
                double aridity = g.aridity[i];
                if (aridity < 0.45f) {
                    continue;
                }
                if (g.oceanDistance[i] < 12 * p.erosionCellSize) {
                    continue;
                }
                double spill = g.filled[i] - g.height[i];
                if (spill < 0.35 && g.dir[i] >= 0) {
                    continue;
                }
                // An ephemeral desert lake is a water-balance feature, not a spill-point feature: it
                // reaches a depth of a few metres at most, scaled by how much catchment feeds it. That
                // is what keeps the playa to the basin floor instead of painting the whole basin white.
                double feed = Math.log(Math.max(2.0, g.acc[i]) / Math.max(8.0, g.channelThreshold[i]))
                        / Math.log(2.0);
                double lakeDepth = 1.1 + 1.9 * Interp.clamp(feed, 0.0, 5.0) / 5.0;
                double lakeLevel = Math.min(g.filled[i], sinkH[i] + lakeDepth);
                double aboveLake = g.height[i] - lakeLevel;
                if (aboveLake < 0.0) {
                    // Plane the floor flat: evaporite crusts and mud cracks do not tolerate relief.
                    g.height[i] = Interp.lerp(0.85, g.height[i], lakeLevel - 0.10);
                    g.playa[i] = (float) Interp.clamp(aridity * 1.3, 0.0, 1.0);
                    if (lakeLevel - 0.75 > g.height[i] + 0.5 && aridity < 0.93) {
                        g.waterLevel[i] = lakeLevel - 0.75;
                    } else {
                        g.waterLevel[i] = Grid.NO_WATER;
                    }
                } else if (aboveLake < 0.6) {
                    // The shoreline ring: still salt-affected, gently planed.
                    g.height[i] = Interp.lerp(0.35, g.height[i], lakeLevel + 0.05);
                    g.playa[i] = (float) Math.max(g.playa[i], aridity * 0.55);
                }
            }
        }
    }

    /**
     * A cheap per-cell rock-strength proxy.
     *
     * <p>The full lithological classification needs climate, which is not available until the tile is
     * built, so erosion uses only the large-scale province noise. Hardness varies smoothly enough that
     * the resulting differential erosion (hard ridges, soft valleys) is still visible and correct in
     * the right places.
     */
    private static double[] erodibilityField(Grid g) {
        int w = g.w;
        double[] e = new double[w * w];
        long seed = Hash.hash(0xE20DL ^ 0x1234L, 7, 13);
        double scale = 1.0 / 900.0;
        int originX = -g.halo * g.params.erosionCellSize;
        int originZ = originX;
        for (int cz = 0; cz < w; cz++) {
            for (int cx = 0; cx < w; cx++) {
                double n = valueNoise(seed, (originX + cx * g.params.erosionCellSize) * scale,
                        (originZ + cz * g.params.erosionCellSize) * scale);
                // 0 = very hard (slow), 1 = soft (fast).
                e[cz * w + cx] = Interp.clamp(0.55 + n * 0.85, 0.12, 1.35);
            }
        }
        return e;
    }

    private static double valueNoise(long seed, double x, double z) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        double fx = Interp.quintic(x - xi);
        double fz = Interp.quintic(z - zi);
        double a = Hash.signed(Hash.hash(seed, xi, zi));
        double b = Hash.signed(Hash.hash(seed, xi + 1, zi));
        double c = Hash.signed(Hash.hash(seed, xi, zi + 1));
        double d = Hash.signed(Hash.hash(seed, xi + 1, zi + 1));
        return Interp.bilerp(fx, fz, a, b, c, d);
    }

    // ----------------------------------------------------------- priority flood

    /**
     * Barnes et al. (2014) priority-flood. Fills every depression to its lowest spill point and
     * records, for each filled cell, the neighbour it drains towards.
     */
    private static void priorityFlood(Grid g) {
        int w = g.w;
        double[] h = g.height;
        double[] f = g.filled;
        boolean[] closed = new boolean[w * w];
        // (quantised height << 32) | index
        PriorityQueue<Long> open = new PriorityQueue<>();
        for (int cz = 0; cz < w; cz++) {
            for (int cx = 0; cx < w; cx++) {
                if (cx == 0 || cz == 0 || cx == w - 1 || cz == w - 1) {
                    int i = cz * w + cx;
                    f[i] = h[i];
                    closed[i] = true;
                    g.dir[i] = -1;
                    open.add(ascendingKey(f[i], i));
                }
            }
        }
        // Small epsilon avoids perfectly flat filled surfaces, which have no flow direction.
        double eps = 1e-4;
        while (!open.isEmpty()) {
            long k = open.poll();
            int i = keyIndex(k);
            int cx = i % w;
            int cz = i / w;
            for (int d = 0; d < 8; d++) {
                int nx = cx + DX[d];
                int nz = cz + DZ[d];
                if (nx < 0 || nz < 0 || nx >= w || nz >= w) {
                    continue;
                }
                int n = nz * w + nx;
                if (closed[n]) {
                    continue;
                }
                closed[n] = true;
                if (h[n] <= f[i]) {
                    // Depression: fill to the spill height plus epsilon so water can still move.
                    f[n] = f[i] + eps;
                } else {
                    f[n] = h[n];
                }
                open.add(ascendingKey(f[n], n));
            }
        }
    }

    private static int opposite(int d) {
        // Pairs: 0<->1, 2<->3, 4<->7, 5<->6
        switch (d) {
            case 0: return 1;
            case 1: return 0;
            case 2: return 3;
            case 3: return 2;
            case 4: return 7;
            case 5: return 6;
            case 6: return 5;
            case 7: return 4;
            default: return -1;
        }
    }

    private static final int INDEX_BITS = 21;
    private static final long INDEX_MASK = (1L << INDEX_BITS) - 1L;
    /** Heights are quantised to 1/4096 block; the offset keeps the rank positive for +-900 blocks. */
    private static final long RANK_OFFSET = 4_000_000L;

    /** Sort key ordering cells by <b>ascending</b> height (priority-flood frontier). */
    private static long ascendingKey(double height, int index) {
        long rank = RANK_OFFSET + (long) Math.round(height * 4096.0);
        return (rank << INDEX_BITS) | (index & INDEX_MASK);
    }

    /** Sort key ordering cells by <b>descending</b> height (drainage and incision sweeps). */
    private static long descendingKey(double height, int index) {
        long rank = RANK_OFFSET - (long) Math.round(height * 4096.0);
        return (rank << INDEX_BITS) | (index & INDEX_MASK);
    }

    private static int keyIndex(long key) {
        return (int) (key & INDEX_MASK);
    }

    // ------------------------------------------------------------------- flow

    private static void computeFlow(Grid g) {
        int w = g.w;
        double[] h = g.filled;
        for (int cz = 1; cz < w - 1; cz++) {
            for (int cx = 1; cx < w - 1; cx++) {
                int i = cz * w + cx;
                double best = 0;
                int bestDir = -1;
                for (int d = 0; d < 8; d++) {
                    int n = (cz + DZ[d]) * w + (cx + DX[d]);
                    double drop = (h[i] - h[n]) / DIST[d];
                    if (drop > best) {
                        best = drop;
                        bestDir = d;
                    }
                }
                g.dir[i] = bestDir;
            }
        }
    }

    /** Accumulates drainage area by walking cells from highest to lowest. */
    private static void computeAccumulation(Grid g) {
        int n = g.w * g.w;
        long[] order = new long[n];
        for (int i = 0; i < n; i++) {
            order[i] = descendingKey(g.filled[i], i);
        }
        java.util.Arrays.sort(order);
        java.util.Arrays.fill(g.acc, 1.0);
        for (long k : order) {
            int i = keyIndex(k);
            int d = g.dir[i];
            if (d >= 0) {
                int cx = i % g.w;
                int cz = i / g.w;
                int nx = cx + DX[d];
                int nz = cz + DZ[d];
                if (nx >= 0 && nz >= 0 && nx < g.w && nz < g.w) {
                    g.acc[nz * g.w + nx] += g.acc[i];
                }
            }
        }
    }

    // --------------------------------------------------------------- fluvial

    private static void fluvialPass(Grid g, double[] erodibility, double cellMeters, int iteration) {
        GenParams p = g.params;
        int w = g.w;
        int n = w * w;
        long[] order = new long[n];
        for (int i = 0; i < n; i++) {
            order[i] = descendingKey(g.height[i], i);
        }
        java.util.Arrays.sort(order);

        double[] load = new double[n];
        double dt = 1.0;
        // Slightly stronger incision on later passes: the network is by then organised, so cuts go
        // into the right places instead of being smeared over a still-random drainage pattern.
        double gain = 1.0 + 0.30 * iteration;
        double kf = p.fluvialK * gain;
        double seq = Math.tan(Math.toRadians(1.4)); // equilibrium slope below which deposition wins

        for (long k : order) {
            int i = keyIndex(k);
            int d = g.dir[i];
            if (d < 0) {
                continue;
            }
            int cx = i % w;
            int cz = i / w;
            int nx = cx + DX[d];
            int nz = cz + DZ[d];
            if (nx <= 0 || nz <= 0 || nx >= w - 1 || nz >= w - 1) {
                continue;
            }
            int j = nz * w + nx;
            double drop = g.height[i] - g.height[j];
            if (drop <= 0) {
                load[j] += load[i];
                continue;
            }
            double distMeters = DIST[d] * cellMeters;
            double s = drop * p.metersPerBlock / distMeters;
            double area = g.acc[i] / Math.max(8.0, g.channelThreshold[i]);
            if (area < 0.35) {
                load[j] += load[i];
                continue;
            }
            double ap = Math.pow(area, p.fluvialAreaExp);
            double sp = Math.pow(s, p.fluvialSlopeExp);

            // Arid, unvegetated, soft ground incises far faster: this is what cuts slot canyons and
            // badland gullies instead of rounded valleys.
            double ar = g.aridity[i];
            double soft = g.softness[i];
            double aridBoost = 1.0 + 3.1 * ar * soft + 0.55 * ar * (1.0 - soft);

            // Stream-power incision.
            double incision = kf * aridBoost * ap * sp * dt * erodibility[i] * p.erosionCellSize;
            // Never cut through the receiver: keeps the network a tree instead of a pit farm.
            double maxCut = drop * 0.45;
            if (incision > maxCut) {
                incision = maxCut;
            }

            // Capacity-limited deposition on low gradients.
            double deposition = 0.0;
            double ratio = s / seq;
            if (ratio < 1.0) {
                // Deposition is much more efficient in arid basins: ephemeral flows lose all their
                // competence the moment the gradient drops, which is how bajadas are built.
                double depGain = 1.0 + 3.4 * ar;
                deposition = p.depositionK * depGain * (load[i] + incision * 0.6) * (1.0 - ratio) * dt;
                deposition = Math.min(deposition, drop * 0.88);
            }

            double dh = deposition - incision;
            g.height[i] += dh;
            g.deposition[i] += (float) dh;
            load[j] += load[i] + incision - deposition;
        }
    }

    // ----------------------------------------------------------------- talus

    /** Jacobi hillslope-failure pass: material above the angle of repose moves to the neighbours. */
    private static void talusPass(Grid g, double talusRock, double talusSoft, double[] erodibility) {
        int w = g.w;
        double[] src = g.height;
        double[] dst = g.talusScratch;
        System.arraycopy(src, 0, dst, 0, src.length);
        double[] excess = new double[8];
        double creepBase = 0.011; // slow linear creep rounds summits even below the failure angle

        for (int cz = 1; cz < w - 1; cz++) {
            for (int cx = 1; cx < w - 1; cx++) {
                int i = cz * w + cx;
                double hi = src[i];
                double repose = Interp.lerp(Interp.clamp(erodibility[i], 0, 1.35), talusRock, talusSoft);
                // No soil and no roots means no bioturbation creep: desert ridges stay knife-edged.
                double creep = creepBase * (1.0 - 0.82 * g.aridity[i]);
                double excessTotal = 0.0;
                int active = 0;
                for (int d = 0; d < 8; d++) {
                    excess[d] = 0.0;
                    int nx = cx + DX[d];
                    int nz = cz + DZ[d];
                    int j = nz * w + nx;
                    double drop = hi - src[j];
                    double limit = repose * DIST[d];
                    if (drop > limit) {
                        excess[d] = (drop - limit) * 0.5;
                        excessTotal += excess[d];
                        active++;
                    }
                    // Linear creep: unconditional, tiny, keeps hillslopes from looking faceted. The
                    // symmetric neighbour applies the same transfer from its own side, so this is half
                    // of the intended creep coefficient.
                    double creepMove = (drop * creep) / DIST[d];
                    dst[i] -= creepMove;
                    dst[j] += creepMove;
                }
                if (excessTotal > 0) {
                    // Simultaneous relaxation over n active directions must not overshoot: moving the
                    // full excess on every direction at once inverts the spike and the Jacobi sweep then
                    // amplifies it each pass. The stable coefficient for n receivers is 2/(n+1).
                    double alpha = Math.min(0.85, 2.0 / (active + 1));
                    dst[i] -= excessTotal * alpha;
                    for (int d = 0; d < 8; d++) {
                        if (excess[d] > 0) {
                            int j = (cz + DZ[d]) * w + (cx + DX[d]);
                            dst[j] += excess[d] * alpha;
                            // The remainder is "lost" to compaction; without it talus cones grow
                            // forever and bury the valleys.
                        }
                    }
                }
            }
        }
        System.arraycopy(dst, 0, g.height, 0, dst.length);
    }

    // --------------------------------------------------------------- isostasy

    /**
     * Airy isostasy. Denudation unloads the crust and the remainder rebounds; the constant is the
     * crust/mantle density ratio (~0.28 for 2800/3300 kg m-3).
     */
    private static void isostaticRebound(Grid g) {
        double gain = 0.24;
        int sea = g.params.seaLevel;
        for (int i = 0; i < g.height.length; i++) {
            double removed = g.base[i] - g.height[i];
            if (removed > 0 && g.base[i] > sea) {
                double rebound = removed * gain;
                g.height[i] += rebound;
                g.denudation[i] += (float) rebound;
            }
        }
    }

    // ----------------------------------------------------------- morphometry

    private static void deriveMorphometry(Grid g) {
        int w = g.w;
        double cell = g.params.erosionCellSize * g.params.metersPerBlock;
        for (int cz = 1; cz < w - 1; cz++) {
            for (int cx = 1; cx < w - 1; cx++) {
                int i = cz * w + cx;
                double dzdx = (g.height[i + 1] - g.height[i - 1]) / (2.0 * cell) * g.params.metersPerBlock;
                double dzdy = (g.height[i + w] - g.height[i - w]) / (2.0 * cell) * g.params.metersPerBlock;
                double slopeRad = Math.atan(Math.sqrt(dzdx * dzdx + dzdy * dzdy));
                g.slope[i] = (float) Math.toDegrees(slopeRad);
                // Aspect points downhill: negate the gradient vector.
                g.aspect[i] = (float) Math.atan2(-dzdy, -dzdx);
                double d2zdx2 = (g.height[i + 1] - 2 * g.height[i] + g.height[i - 1]) / (cell * cell);
                double d2zdy2 = (g.height[i + w] - 2 * g.height[i] + g.height[i - w]) / (cell * cell);
                double d2zdxdy = (g.height[i + w + 1] - g.height[i + w - 1]
                        - g.height[i - w + 1] + g.height[i - w - 1]) / (4 * cell * cell);
                double p = dzdx;
                double q = dzdy;
                double denom = (1 + p * p + q * q);
                // Profile curvature: negative = concave (valley floor), positive = convex (ridge).
                double prof = -(p * p * d2zdx2 + 2 * p * q * d2zdxdy + q * q * d2zdy2)
                        / (denom * Math.sqrt(denom) + 1e-9);
                g.curvature[i] = (float) Interp.clamp(prof * 400.0, -1.0, 1.0);
            }
        }
    }

    /** Two-pass chamfer distance transform from ocean cells. */
    private static void oceanDistance(Grid g) {
        int w = g.w;
        float[] d = g.oceanDistance;
        float max = 1e9f;
        java.util.Arrays.fill(d, max);
        int sea = g.params.seaLevel;
        for (int i = 0; i < d.length; i++) {
            if (g.base[i] < sea - 1.0) {
                d[i] = 0;
            }
        }
        float step = g.params.erosionCellSize;
        float diag = step * 1.4142f;
        for (int cz = 1; cz < w; cz++) {
            for (int cx = 1; cx < w; cx++) {
                int i = cz * w + cx;
                d[i] = min3(d[i], d[i - 1] + step, d[i - w] + step, d[i - w - 1] + diag, d[i - w + 1] + diag);
            }
        }
        for (int cz = w - 2; cz >= 0; cz--) {
            for (int cx = w - 2; cx >= 0; cx--) {
                int i = cz * w + cx;
                d[i] = min3(d[i], d[i + 1] + step, d[i + w] + step, d[i + w + 1] + diag, d[i + w - 1] + diag);
            }
        }
    }

    private static float min3(float a, float b, float c, float d, float e) {
        float m = a < b ? a : b;
        if (c < m) {
            m = c;
        }
        if (d < m) {
            m = d;
        }
        return e < m ? e : m;
    }

    // --------------------------------------------------------------- channels

    /**
     * Turns the flow-accumulation skeleton into visible channels with hydraulic-geometry scaling.
     *
     * <p>Real river width and depth scale as power laws of discharge, {@code w ~ Q^0.5},
     * {@code d ~ Q^0.4}, and discharge scales with drainage area. We use that directly, then splat the
     * channel over its half-width so that bilinear interpolation to block resolution yields a smooth
     * water surface with proper banks rather than a one-cell scratch.
     */
    private static void carveChannels(Grid g) {
        GenParams p = g.params;
        int w = g.w;
        int sea = p.seaLevel;
        // 1. per-cell geometry
        float[] depth = new float[w * w];
        float[] halfWidth = new float[w * w];
        for (int cz = 1; cz < w - 1; cz++) {
            for (int cx = 1; cx < w - 1; cx++) {
                int i = cz * w + cx;
                double a = g.acc[i];
                if (a < g.channelThreshold[i]) {
                    continue;
                }
                double t = Math.log(a / g.channelThreshold[i]) / Math.log(2.0);
                double wd = Interp.clamp(0.55 + 0.52 * Math.pow(t, 0.85), 0.55, 2.4);
                double dp = Interp.clamp(0.9 + 0.85 * Math.pow(t, 0.72), 0.9, 8.0);
                // Steep bedrock reaches are narrow and deep (canyons); alluvial reaches are wide and shallow.
                double steep = Interp.smoothstep(6.0, 22.0, g.slope[i]);
                wd *= (1.0 - 0.42 * steep);
                dp *= (1.0 + 0.55 * steep);
                // Meanders widen the flood plain. Incised arid arroyos do not meander: they are
                // straight, steep-walled and cut into bedrock or indurated sediment.
                double arid = g.aridity[i];
                // Arid channels are narrow: an arroyo is a trench, not a flood plain.
                wd *= Interp.lerp(1.0, 0.5, arid);
                if (p.meanders && g.slope[i] < 4.0) {
                    wd *= Interp.lerp(1.22, 0.6, arid);
                }
                dp *= Interp.lerp(1.0, 1.85, arid); // slot-canyon effect
                halfWidth[i] = (float) wd;
                depth[i] = (float) dp;
            }
        }
        if (p.meanders) {
            meanderPass(g, depth);
        }
        // 2. splat
        int radius = 4;
        for (int cz = 1; cz < w - 1; cz++) {
            for (int cx = 1; cx < w - 1; cx++) {
                int i = cz * w + cx;
                if (depth[i] <= 0) {
                    continue;
                }
                double hw = halfWidth[i];
                int r = Interp.clamp((int) Math.ceil(hw) + 1, 1, radius);
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        int nx = cx + dx;
                        int nz = cz + dz;
                        if (nx < 0 || nz < 0 || nx >= w || nz >= w) {
                            continue;
                        }
                        int j = nz * w + nx;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        double s = 1.0 - Interp.smoothstep(hw * 0.55, hw + 0.65, dist);
                        if (s <= 0) {
                            continue;
                        }
                        if (s > g.channel[j]) {
                            g.channel[j] = (float) s;
                            g.channelDepth[j] = (float) (depth[i] * s);
                        }
                    }
                }
            }
        }
        // 3. carve + set water level
        for (int i = 0; i < g.height.length; i++) {
            double s = g.channel[i];
            if (s <= 0) {
                continue;
            }
            double cut = g.channelDepth[i];
            double before = g.height[i];
            g.height[i] = before - cut;
            // A desert arroyo is dry 350 days a year: only perennial channels carry standing water.
            // Perennial means humid climate, or a catchment so large that baseflow survives the dry
            // season. Everything else is an incised, empty trench — which is exactly what an arroyo is.
            double ar = g.aridity[i];
            boolean perennial = ar < 0.14 || g.acc[i] > g.channelThreshold[i] * 55.0;
            if (perennial) {
                double level = before - cut * 0.28;
                if (level > sea + 0.5 || before > sea) {
                    g.waterLevel[i] = Math.max(g.waterLevel[i], level);
                }
            }
        }
        // 3.5 re-flood: carving the channels created new depressions along every channel, and the
        // pre-carve fill level would read each of them as a lake. Only a fresh flood gives the real
        // closed basins.
        priorityFlood(g);

        // 4. lakes from the depression fill
        for (int i = 0; i < g.height.length; i++) {
            double lakeDepth = g.filled[i] - g.height[i];
            if (lakeDepth > 1.6 && g.channel[i] < 0.35
                    && (g.aridity[i] < 0.55 || g.acc[i] > 2600.0)) {
                g.waterLevel[i] = Math.max(g.waterLevel[i], g.filled[i] - 0.15);
            }
        }
    }

    /**
     * Channel migration by helicoidal flow.
     *
     * <p>Where a reach turns, the surface flow is deflected downwards by the centrifugal effect, so the
     * near-bed current crosses the channel towards the outer bank. That undercuts the outer bank and
     * dumps the eroded material as a point bar on the inner bank. Iterating this is all it takes for
     * straight alluvial channels to develop sinuosity, for necks to narrow, and for cut-off loops to be
     * left behind as oxbow scars.
     */
    private static void meanderPass(Grid g, float[] depth) {
        int w = g.w;
        double rate = 0.32;
        for (int pass = 0; pass < 3; pass++) {
            for (int cz = 2; cz < w - 2; cz++) {
                for (int cx = 2; cx < w - 2; cx++) {
                    int i = cz * w + cx;
                    if (depth[i] <= 0 || g.slope[i] > 5.0) {
                        continue;
                    }
                    int d = g.dir[i];
                    if (d < 0) {
                        continue;
                    }
                    int jx = cx + DX[d];
                    int jz = cz + DZ[d];
                    if (jx < 1 || jz < 1 || jx >= w - 1 || jz >= w - 1) {
                        continue;
                    }
                    int j = jz * w + jx;
                    // Turn of the thalweg between this cell and the next.
                    double a1 = Math.atan2(DZ[d], DX[d]);
                    double a0 = upstreamAspect(g, cx, cz);
                    double turn = Interp.wrapAngle(a0 - a1 + Math.PI);
                    if (turn > Math.PI) {
                        turn -= 2 * Math.PI;
                    }
                    if (Math.abs(turn) < 0.035) {
                        continue;
                    }
                    // Lateral unit vector, perpendicular to the downstream direction.
                    double lx = -Math.sin(a1);
                    double lz = Math.cos(a1);
                    // turn > 0 means the channel bends towards +lateral: the outer bank is -lateral.
                    double sgn = turn > 0 ? -1.0 : 1.0;
                    int ox = cx + (int) Math.round(lx * sgn);
                    int oz = cz + (int) Math.round(lz * sgn);
                    int inx = cx - (int) Math.round(lx * sgn);
                    int inz = cz - (int) Math.round(lz * sgn);
                    if (ox < 1 || oz < 1 || ox >= w - 1 || oz >= w - 1) {
                        continue;
                    }
                    if (inx < 1 || inz < 1 || inx >= w - 1 || inz >= w - 1) {
                        continue;
                    }
                    int outer = oz * w + ox;
                    int inner = inz * w + inx;
                    double move = Math.min(Math.abs(turn) * rate, 0.35) * (0.4 + depth[i] * 0.12);
                    // Only migrate across land, never into the sea or a lake.
                    if (g.waterLevel[outer] == Grid.NO_WATER) {
                        g.height[outer] -= move;
                        g.height[inner] += move * 0.85;
                        g.deposition[inner] += (float) (move * 0.85);
                        g.deposition[outer] -= (float) move;
                    }
                }
            }
        }
    }

    private static double upstreamAspect(Grid g, int cx, int cz) {
        int w = g.w;
        double best = -1e9;
        double ax = 1;
        double az = 0;
        for (int d = 0; d < 8; d++) {
            int n = (cz + DZ[d]) * w + (cx + DX[d]);
            if (g.dir[n] == opposite(d) && g.height[n] > best) {
                best = g.height[n];
                ax = DX[d];
                az = DZ[d];
            }
        }
        return Math.atan2(az, ax);
    }

    // ----------------------------------------------------------------- deltas

    /**
     * River mouths build deltas: where a high-discharge channel meets standing water the gradient
     * collapses to zero, capacity collapses with it, and the entire bed load is dropped. The deposit is
     * a shallow cone that pokes above sea level and splits the channel into distributaries.
     */
    private static void deltas(Grid g) {
        if (!g.params.deltas) {
            return;
        }
        GenParams p = g.params;
        int w = g.w;
        int sea = p.seaLevel;
        for (int cz = 2; cz < w - 2; cz++) {
            for (int cx = 2; cx < w - 2; cx++) {
                int i = cz * w + cx;
                if (g.acc[i] < p.majorRiverThreshold * 0.55) {
                    continue;
                }
                if (g.height[i] > sea + 3.0 || g.height[i] < sea - 6.0) {
                    continue;
                }
                // Must be a mouth: downstream is open water and the cell is near the coast.
                int d = g.dir[i];
                if (d < 0) {
                    continue;
                }
                int j = (cz + DZ[d]) * w + (cx + DX[d]);
                boolean mouth = g.base[j] < sea + 0.5 || g.oceanDistance[i] < 6 * p.erosionCellSize;
                if (!mouth) {
                    continue;
                }
                double t = Math.log(g.acc[i] / p.riverThreshold) / Math.log(2.0);
                double radius = Interp.clamp(2.0 + 0.85 * t, 2.0, 12.0);
                double crest = Interp.clamp(1.0 + 0.35 * t, 1.0, 4.0);
                int r = (int) Math.ceil(radius);
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        int nx = cx + dx;
                        int nz = cz + dz;
                        if (nx < 0 || nz < 0 || nx >= w || nz >= w) {
                            continue;
                        }
                        int k = nz * w + nx;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > radius) {
                            continue;
                        }
                        double s = 1.0 - Interp.smoothstep(radius * 0.25, radius, dist);
                        double target = sea + crest * s;
                        if (g.height[k] < target) {
                            g.height[k] = Interp.lerp(0.65, g.height[k], target);
                            g.delta[k] = (float) Math.max(g.delta[k], s);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------ water table

    /**
     * Propagates a water table outwards from every body of water.
     *
     * <p>The level drops with distance from the channel and can never exceed the local surface minus a
     * small offset. Where it comes within a block of the surface on flat ground you get a spring, a seep
     * or a wetland — the reason real valleys are boggy at the bottom of the slope and dry on top of it.
     * Gauss-Seidel sweeps in alternating directions converge in a handful of passes over the tile.
     */
    private static void waterTableField(Grid g) {
        int w = g.w;
        double[] wt = g.waterTable;
        java.util.Arrays.fill(wt, Grid.NO_WATER);
        for (int i = 0; i < wt.length; i++) {
            if (g.waterLevel[i] != Grid.NO_WATER) {
                wt[i] = g.waterLevel[i];
            }
        }
        double dropPerCell = 0.16;
        for (int pass = 0; pass < 14; pass++) {
            boolean forward = (pass & 1) == 0;
            for (int cz = 1; cz < w - 1; cz++) {
                int row = forward ? cz : w - 1 - cz;
                for (int k = 1; k < w - 1; k++) {
                    int cx = forward ? k : w - 1 - k;
                    int i = row * w + cx;
                    double best = wt[i];
                    for (int d = 0; d < 8; d++) {
                        int j = (row + DZ[d]) * w + (cx + DX[d]);
                        double cand = wt[j] - dropPerCell * DIST[d];
                        if (cand > best) {
                            best = cand;
                        }
                    }
                    double cap = g.height[i] - 0.15;
                    wt[i] = Math.min(best, cap);
                }
            }
        }
    }
}
