package dev.terrarealis.kernel.geo;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.Rock;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;

/**
 * Subterranean voids.
 *
 * <p>Vanilla's cave generator is three independent 3-D noise fields with fixed thresholds. It looks
 * like swiss cheese because it is swiss cheese: cave density does not depend on what the rock is, how
 * deep it is, or where the water table sits. Real caves are strongly conditioned by all three.
 *
 * <p>Terra Realis evaluates five distinct void systems and combines them:
 * <ol>
 *   <li><b>Bedding-plane caves</b> — voids develop along specific beds, so a cave system in tilted
 *       strata is a set of inclined tiers that follow the dip of the rock. This is what makes an
 *       underground passage look like it is <i>in</i> something.</li>
 *   <li><b>Karst conduits</b> — dissolution along joints, orders of magnitude more void in soluble
 *       rock than in granite. Scaled by the same karst index that drives dolines and ponors, so surface
 *       and subsurface karst agree.</li>
 *   <li><b>Fissure caves</b> — narrow, straight, steeply dipping slots along fault traces and dyke
 *       swarms. Driven by the tectonic fracture field, so they line up with the lineaments visible on
 *       the surface.</li>
 *   <li><b>Massive-cavern systems</b> — the classic 3-D "cheese" voids, but restricted to depth and
 *       suppressed in hard crystalline rock, which in reality joint-blocks rather than dissolves.</li>
 *   <li><b>Lava tubes</b> — long, low, remarkably straight tunnels on volcanic flanks, formed when the
 *       crust of a flow insulates the still-molten interior and then drains out. Only near vents.</li>
 * </ol>
 *
 * <p>Everything below the water table is flooded, and everything below the magma level is lava-filled,
 * so a cave system has a dry upper tier, a water table level you can hear, and a flooded lower tier —
 * the structure that real cavers actually navigate.
 */
public final class Caves {

    /** Per-column context the void field needs; reused, never allocated per block. */
    public static final class Ctx {
        public int surfaceY;
        public int waterTableY;
        public int lavaLevel;
        public int minY;
        public double karst;
        public double fracture;
        public double volcano;
        public double dip;
        public double strike;
        public double strataSpacing;
        public boolean bedded;
        public double hardness;
    }

    private final GenParams p;
    private final Noise cheese;
    private final Noise spaghettiA;
    private final Noise spaghettiB;
    private final Noise fissure;
    private final Noise tube;
    private final Noise joint;

    public Caves(long seed, GenParams params) {
        this.p = params;
        this.cheese = new Noise(seed ^ 0x1C4E3L);
        this.spaghettiA = new Noise(seed ^ 0x5FA9E712L);
        this.spaghettiB = new Noise(seed ^ 0x2B7D44C8L);
        this.fissure = new Noise(seed ^ 0x6A1F90D3L);
        this.tube = new Noise(seed ^ 0x3E8C5A71L);
        this.joint = new Noise(seed ^ 0x71B4D2E9L);
    }

    /**
     * Void density at one lattice point, 0..1. The caller thresholds this after trilinear
     * interpolation to block resolution, exactly the way vanilla evaluates its own cave noise on a
     * coarse cell and interpolates — which keeps the cost per chunk in the single-digit milliseconds.
     */
    public double density(int x, int y, int z, Ctx c) {
        int depth = c.surfaceY - y;
        if (depth < 4 || y <= c.minY + 4) {
            return 0.0;
        }
        double depthF = Interp.smoothstep(4.0, 34.0, depth);
        // Hard crystalline rock joint-blocks instead of dissolving: far less void.
        double rockGate = Interp.lerp(1.0, 0.30, Interp.clamp(c.hardness, 0.0, 1.0));

        double v = 0.0;

        // ------------------------------------------------------ massive caverns
        double scale = 1.0 / (38.0 * Math.max(0.35, p.caveCellSize / 4.0));
        double cheese = this.cheese.fbm3(x * scale, y * scale * 1.35, z * scale, 2, 2.0, 0.5);
        double thr = 0.60 - 0.30 * depthF - 0.20 * c.karst;
        v += Interp.smoothstep(thr, thr + 0.16, cheese) * 0.95 * rockGate;

        // --------------------------------------------------- winding passages
        // Two 2-D sheets whose intersection is a curve in 3-D: a tunnel. Cheap and very effective.
        double s1 = spaghettiA.perlin2(x / 84.0 + y * 0.019, y / 24.0 - z * 0.004);
        double s2 = spaghettiB.perlin2(z / 84.0 - y * 0.019, y / 24.0 + x * 0.004);
        double tunnelW = 0.055 + 0.055 * c.karst + 0.02 * depthF;
        if (Math.abs(s1) < tunnelW && Math.abs(s2) < tunnelW) {
            double a = 1.0 - Math.abs(s1) / tunnelW;
            double b = 1.0 - Math.abs(s2) / tunnelW;
            v += Math.min(a, b) * 0.95 * rockGate * Interp.lerp(0.55, 1.0, depthF);
        }

        // ------------------------------------------------- bedding-plane caves
        if (c.bedded) {
            double u = x * Math.cos(c.strike) + z * Math.sin(c.strike) + y * Math.tan(c.dip);
            double band = u / Math.max(2.0, c.strataSpacing);
            double frac = band - Math.floor(band);
            // Voids preferentially follow one particular bed and its partings.
            double onBed = Math.exp(-Math.pow((frac - 0.5) / 0.16, 2.0));
            double parting = Math.exp(-Math.pow(frac / 0.06, 2.0))
                    + Math.exp(-Math.pow((frac - 1.0) / 0.06, 2.0));
            double bedding = (onBed * 0.55 + parting * 0.85) * (0.25 + 0.75 * c.karst);
            double beddingNoise = 0.5 + 0.5 * joint.perlin3(x / 46.0, y / 12.0, z / 46.0);
            v += bedding * beddingNoise * Interp.lerp(0.35, 1.0, depthF);
        }

        // ------------------------------------------------------- fissure caves
        if (c.fracture > 0.22) {
            double f = fissure.ridged2(x / 150.0 + y * 0.0035, z / 150.0 - y * 0.0035, 2, 2.0, 0.5);
            double slot = Interp.smoothstep(0.72, 0.94, f) * c.fracture;
            // Fissures are near-vertical: fade them out with depth below the fracture zone.
            v += slot * 0.85 * Interp.smoothstep(6.0, 26.0, depth) * (1.0 - Interp.smoothstep(150.0, 260.0, depth));
        }

        // ---------------------------------------------------------- lava tubes
        if (c.volcano > 0.12) {
            double t1 = tube.perlin2(x / 120.0 + y * 0.031, y / 15.0);
            double t2 = tube.perlin2(z / 120.0 - y * 0.031, y / 15.0 + 40.0);
            double tw = 0.045;
            if (Math.abs(t1) < tw && Math.abs(t2) < tw && depth < 70.0) {
                double a = 1.0 - Math.abs(t1) / tw;
                double b = 1.0 - Math.abs(t2) / tw;
                v += Math.min(a, b) * c.volcano * 0.9
                        * (1.0 - Interp.smoothstep(24.0, 72.0, depth));
            }
        }

        return Interp.clamp(v * p.caveDensity, 0.0, 1.4);
    }

    /**
     * Threshold above which the lattice point becomes void. Rises with proximity to the surface so
     * caves do not punch holes in the ground everywhere.
     */
    public double threshold(int depthBelowSurface, double karst) {
        return Interp.lerp(1.02, 0.62, Interp.smoothstep(5.0, 46.0, depthBelowSurface))
                - 0.14 * karst;
    }

    /**
     * Sea caves and wave-cut notches: a cliff base at the waterline gets undercut.
     * Returns the number of blocks to remove from the base of the column, 0 if none.
     */
    public double seaCaveNotch(int x, int z, int surfaceY, double slopeDeg, double waterDepth,
                               GenParams p) {
        if (waterDepth <= 0.2 || waterDepth > 14.0 || slopeDeg < 18.0) {
            return 0.0;
        }
        if (surfaceY < p.seaLevel - 2 || surfaceY > p.seaLevel + 12) {
            return 0.0;
        }
        double n = joint.fbm2(x / 42.0, z / 42.0, 3, 2.0, 0.5);
        double notch = Interp.smoothstep(0.05, 0.55, n);
        return notch * (2.0 + 5.0 * Interp.smoothstep(20.0, 45.0, slopeDeg));
    }

    /** Whether the host rock of a {@link Rock} province is expected to be bedded. */
    public static boolean bedded(Rock rock) {
        return rock.bedded();
    }
}
