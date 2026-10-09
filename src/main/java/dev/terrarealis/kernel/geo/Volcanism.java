package dev.terrarealis.kernel.geo;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.math.Hash;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;

/**
 * Volcanic edifices and lava flows.
 *
 * <p>Two scales, because real volcanic provinces have both:
 *
 * <ul>
 *   <li><b>Stratovolcanoes</b> at hotspots and above subduction zones. Concave-up profile (steep near
 *       the summit, gentle at the base), a summit crater with a raised rim, an occasional collapse
 *       caldera, and <i>radial</i> drainage — the spokes that make a volcano read as a volcano from any
 *       angle.</li>
 *   <li><b>Monogenetic cone fields</b> — dozens of small cinder cones scattered over a volcanic
 *       province, each with its own crater. This is the Pinacate field in the Sonoran and the kind of
 *       terrain that gives Death Stranding its ash-desert backdrops.</li>
 *   <li><b>Lava flows</b> that follow the pre-existing drainage downhill and, crucially, <i>fill</i> it:
 *       a flow imposes a minimum gradient, so valleys downstream of a vent become flat floored and the
 *       ridges between them become inverted relief once the softer surroundings erode away.</li>
 * </ul>
 */
public final class Volcanism {

    /** What one column owes to volcanism. */
    public static final class Edifice {
        /** Added elevation, blocks. */
        public double height;
        /** 0..1 proximity to a summit vent (crater floor). */
        public double vent;
        /** 0..1 inside a crater or caldera depression. */
        public double crater;
        /** 0..1 on a crater/summit rim. */
        public double rim;
        /** 0 stratovolcano, 1 cinder cone. */
        public double coneKind;
        /** 0..1 strength of the radial spoke pattern. */
        public double radial;
        /** 0..1 — fresh volcanic rock at the surface. */
        public double fresh;

        public void clear() {
            height = 0;
            vent = 0;
            crater = 0;
            rim = 0;
            coneKind = 0;
            radial = 0;
            fresh = 0;
        }
    }

    private final GenParams p;
    private final Noise strato;
    private final Noise cones;
    private final Noise field;
    private final Noise surface;
    private final double stratoInv;
    private final double coneInv;

    public Volcanism(long seed, GenParams params) {
        this.p = params;
        this.strato = new Noise(seed ^ 0x1A2B3C4DL);
        this.cones = new Noise(seed ^ 0x5E6F7A8BL);
        this.field = new Noise(seed ^ 0x2D4F6183L);
        this.surface = new Noise(seed ^ 0x7C1E90A2L);
        this.stratoInv = 1.0 / Math.max(1200.0, params.volcanoSpacing);
        this.coneInv = 1.0 / Math.max(400.0, params.cinderConeSpacing);
    }

    public boolean enabled() {
        return p.volcanism && p.volcanoSpacing > 0;
    }

    /**
     * @param arc      0..1 volcanic-arc strength from {@link Tectonics.Setting#arc}
     * @param rift     0..1 divergence (rift volcanism)
     */
    public void sample(int x, int z, double arc, double rift, Edifice out) {
        out.clear();
        if (!enabled()) {
            return;
        }
        // A large-scale "is this a volcanic province at all" field so cones cluster instead of
        // carpeting the entire map.
        double province = Interp.smoothstep(0.02, 0.52,
                field.fbm2(x / 9000.0 + 17.0, z / 9000.0 - 31.0, 3, 2.0, 0.5));
        province = Interp.clamp(province * 0.75 + arc * 0.75 + rift * 0.35, 0.0, 1.0);
        if (province < 0.05) {
            return;
        }

        // ------------------------------------------------------- stratovolcano
        Noise.Worley w = strato.worley2(x * stratoInv, z * stratoInv, 0.5);
        long id = Hash.hash(strato.seed(), w.cellX, w.cellZ);
        double active = Hash.unit(id) < 0.5 ? 1.0 : 0.0;
        double dist = w.f1 / stratoInv;
        if (active > 0 && province > 0.12) {
            double radius = p.stratovolcanoRadius
                    * (0.62 + 0.75 * Hash.unit(Hash.mix64(id ^ 0x2545F491L)));
            double height = p.stratovolcanoHeight
                    * (0.65 + 0.6 * Hash.unit(Hash.mix64(id ^ 0x9E3779B9L))) * province;
            if (dist < radius) {
                double u = 1.0 - dist / radius;
                // Concave-up cone: exponent < 1 keeps the summit steep and the apron gentle.
                double cone = Math.pow(u, 0.70);
                // Radial spokes: real volcanoes are fluted by their own drainage.
                double angle = Math.atan2(z - w.cellZ / stratoInv, x - w.cellX / stratoInv);
                int spokes = 7 + Hash.range(id, 9);
                double radial = 0.5 + 0.5 * Math.sin(angle * spokes
                        + Hash.unit(Hash.mix64(id ^ 0x6C62272EL)) * 6.283);
                double spokeBand = Interp.smoothstep(0.10, 0.35, u) * (1.0 - Interp.smoothstep(0.62, 0.95, u));
                double h = height * cone * (1.0 - 0.16 * radial * spokeBand);
                // Surface texture so the cone is not a perfect solid of revolution.
                h *= 0.94 + 0.12 * (0.5 + 0.5 * surface.fbm2(x / 130.0, z / 130.0, 3, 2.0, 0.5));

                double craterR = radius * 0.115;
                double caldera = Hash.unit(Hash.mix64(id ^ 0xBF58476DL));
                if (caldera < 0.28 && dist < radius * 0.30) {
                    // Collapse caldera: flat floor, steep inner wall, resurgent rim.
                    double cr = radius * 0.30;
                    double cu = 1.0 - dist / cr;
                    h = height * cone * 0.62 * Interp.smoothstep(0.0, 1.0, cu);
                    out.crater = Interp.clamp(1.35 * cu, 0.0, 1.0);
                    out.rim = Interp.smoothstep(0.62, 0.88, cu) * (1.0 - Interp.smoothstep(0.88, 1.0, cu));
                    h += out.rim * height * 0.16;
                } else if (dist < craterR * 1.9) {
                    double cu = 1.0 - dist / (craterR * 1.9);
                    out.crater = Interp.clamp(cu * 1.4, 0.0, 1.0);
                    out.rim = Interp.smoothstep(0.35, 0.75, cu) * (1.0 - Interp.smoothstep(0.80, 1.0, cu));
                    h -= out.crater * height * 0.30;
                    h += out.rim * height * 0.10;
                }
                out.vent = Interp.clamp(1.0 - dist / (craterR * 2.4), 0.0, 1.0) * active;
                out.radial = radial * spokeBand;
                out.height = Math.max(0.0, h);
                out.coneKind = 0.0;
                out.fresh = Interp.clamp(province * (0.35 + 0.65 * out.vent), 0.0, 1.0);
            }
        }

        // ---------------------------------------------------------- cinder cones
        if (province < 0.16) {
            return;
        }
        Noise.Worley c = cones.worley2(x * coneInv, z * coneInv, 0.55);
        long cid = Hash.hash(cones.seed(), c.cellX, c.cellZ);
        if (Hash.unit(cid) > 0.46 * province + 0.10) {
            return;
        }
        double cdist = c.f1 / coneInv;
        double cr = (16.0 + 34.0 * Hash.unit(Hash.mix64(cid ^ 0x51ED270BL))) * (0.6 + 0.8 * province);
        if (cdist >= cr) {
            return;
        }
        double ch = p.cinderConeHeight * (0.55 + 0.9 * Hash.unit(Hash.mix64(cid ^ 0x3C6EF372L)));
        double u = 1.0 - cdist / cr;
        // Scoria cones are straight-sided and stand at the angle of repose of loose tephra.
        double cone = Math.pow(u, 0.86);
        double craterR = cr * (0.24 + 0.14 * Hash.unit(Hash.mix64(cid ^ 0x2E1B2138L)));
        double h = ch * cone;
        if (cdist < craterR * 1.6) {
            double cu = 1.0 - cdist / (craterR * 1.6);
            double crater = Interp.clamp(cu * 1.5, 0.0, 1.0);
            h -= crater * ch * 0.55;
            out.crater = Math.max(out.crater, crater);
            out.vent = Math.max(out.vent, Interp.clamp(1.0 - cdist / craterR, 0.0, 1.0));
        }
        // Breach: many cinder cones have a gap where a lava flow broke out.
        double angle = Math.atan2(z - c.cellZ / coneInv, x - c.cellX / coneInv);
        double breach = Interp.smoothstep(0.30, 0.02,
                Math.abs(Interp.wrapAngle(angle - Hash.unit(Hash.mix64(cid)) * 6.283 + Math.PI) - Math.PI));
        if (Hash.unit(Hash.mix64(cid ^ 0x77A61D2FL)) < 0.55) {
            h *= 1.0 - 0.75 * breach * Interp.smoothstep(cr * 0.35, cr, cdist);
        }
        if (h > out.height) {
            out.height = h;
            out.coneKind = 1.0;
            out.radial = 0.0;
        } else {
            out.height = Math.max(out.height, h * 0.85);
        }
        out.fresh = Math.max(out.fresh, Interp.clamp(province * 0.85, 0.0, 1.0));
    }

    /**
     * Lava flows on the erosion grid.
     *
     * <p>A flow leaves the vent and follows the pre-flow drainage downhill, but unlike water it has a
     * yield strength: it stops moving below a minimum gradient. Implementing that as
     * {@code surface = max(surface, previous - minGradient * step)} is what makes flows fill valleys,
     * dam lakes, and eventually stand out as inverted relief.
     */
    public static void lavaFlows(Erosion.Grid g, Volcanism v, Tectonics tectonics) {
        GenParams p = g.params;
        if (!p.lavaFlows || v == null || !v.enabled()) {
            return;
        }
        int w = g.w;
        int cell = p.erosionCellSize;
        double minGradient = Math.tan(Math.toRadians(1.6)) * cell;
        int originX = -g.halo * cell;
        int originZ = originX;
        Edifice e = new Edifice();
        Tectonics.Setting t = new Tectonics.Setting();

        for (int cz = 3; cz < w - 3; cz++) {
            for (int cx = 3; cx < w - 3; cx++) {
                int bx = originX + cx * cell;
                int bz = originZ + cz * cell;
                tectonics.sampleInto(bx, bz, t);
                v.sample(bx, bz, t.arc, t.rift, e);
                if (e.vent < 0.55) {
                    continue;
                }
                int i = cz * w + cx;
                if (g.lavaFlow[i] > 0.9) {
                    continue;
                }
                // Follow the drainage downstream, filling to the minimum gradient.
                double level = g.height[i] + p.lavaLevelVent;
                int cur = i;
                int steps = (int) p.lavaFlowLength;
                for (int s = 0; s < steps; s++) {
                    int d = g.dir[cur];
                    if (d < 0) {
                        break;
                    }
                    int ncx = (cur % w) + Erosion.DX[d];
                    int ncz = (cur / w) + Erosion.DZ[d];
                    if (ncx < 2 || ncz < 2 || ncx >= w - 2 || ncz >= w - 2) {
                        break;
                    }
                    int n = ncz * w + ncx;
                    level -= minGradient * Erosion.DIST[d];
                    if (g.height[n] > level) {
                        // Flow is thicker than the valley: it climbs the far side and stops.
                        level = g.height[n] - minGradient * 0.5;
                        if (level <= g.height[cur]) {
                            break;
                        }
                    }
                    double thickness = Interp.clamp(level - g.height[n], 0.0, 34.0);
                    double fade = 1.0 - Interp.smoothstep(steps * 0.45, steps, s);
                    if (thickness > 0.35 && fade > 0.05) {
                        g.height[n] = Interp.lerp(0.85, g.height[n], g.height[n] + thickness);
                        g.lavaFlow[n] = (float) Math.max(g.lavaFlow[n], fade);
                        // Lava floods and buries anything standing in the valley, including lakes.
                        if (g.waterLevel[n] != Erosion.Grid.NO_WATER && g.waterLevel[n] < g.height[n]) {
                            g.waterLevel[n] = Erosion.Grid.NO_WATER;
                        }
                        // The flow spreads laterally, one cell either side, thinning as it goes.
                        int lx = ncx - Erosion.DZ[d];
                        int lz = ncz + Erosion.DX[d];
                        if (lx > 1 && lz > 1 && lx < w - 2 && lz < w - 2) {
                            int side = lz * w + lx;
                            double st = thickness * 0.42;
                            if (g.height[side] < level && st > 0.4) {
                                g.height[side] += st * 0.7;
                                g.lavaFlow[side] = (float) Math.max(g.lavaFlow[side], fade * 0.7f);
                            }
                            int rx = ncx + Erosion.DZ[d];
                            int rz = ncz - Erosion.DX[d];
                            int side2 = rz * w + rx;
                            if (rx > 1 && rz > 1 && rx < w - 2 && rz < w - 2
                                    && g.height[side2] < level && st > 0.4) {
                                g.height[side2] += st * 0.7;
                                g.lavaFlow[side2] = (float) Math.max(g.lavaFlow[side2], fade * 0.7f);
                            }
                        }
                    }
                    cur = n;
                    if (g.lavaFlow[cur] > 0.98 && s > steps * 0.5) {
                        break;
                    }
                }
            }
        }
    }

}
