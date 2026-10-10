package dev.terrarealis.kernel.geo;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.math.Hash;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;

/**
 * Plate tectonics, simplified to what a heightfield generator can actually use.
 *
 * <p>The crust is partitioned into plates by a jittered Voronoi (Worley) tessellation at
 * {@link GenParams#plateScale}. Each plate carries a rigid velocity vector derived from a large
 * scale mantle-flow field, so neighbouring plates move coherently and predictably. From that we get
 * the three things that actually control real topography:
 *
 * <ul>
 *   <li><b>Convergence</b> — the component of relative plate velocity normal to the shared boundary.
 *       Positive convergence builds mountain belts (Himalaya, Andes); the belt is <i>linear</i>,
 *       which is the single biggest visual difference from vanilla's blobby "mountains" biome.</li>
 *   <li><b>Divergence</b> — negative convergence opens rifts on continents (East African Rift) and
 *       mid-ocean ridges under water.</li>
 *   <li><b>Transform</b> — large tangential relative motion with little convergence produces fault
 *       lineaments: straight valleys, offset ridges and the fracture networks that later become
 *       fissure caves.</li>
 * </ul>
 *
 * <p>Subduction asymmetry is modelled with the classic rule: at a convergent margin the oceanic side
 * is dragged down into a trench, while the overriding side is thickened and volcanised into an arc.
 * Hotspots are an independent Poisson field — they do not sit on boundaries, exactly like Hawaii.
 */
public final class Tectonics {

    /** Half width of an orogenic belt, in plate-cell units. */
    private static final double BELT_WIDTH = 0.30;
    /** Half width of a fault lineament, in plate-cell units. */
    private static final double FAULT_WIDTH = 0.075;

    private final GenParams p;
    private final Noise plate;
    private final Noise hotspot;
    private final Noise mantleFlow;
    private final Noise activeMargin;
    private final Noise extensionProvince;
    private final double cellInv;
    private final double hotspotInv;

    public Tectonics(long seed, GenParams params) {
        this.p = params;
        this.plate = new Noise(seed ^ 0x7E4A7C15L);
        this.hotspot = new Noise(seed ^ 0x2545F491L);
        this.mantleFlow = new Noise(seed ^ 0x6C62272EL);
        this.activeMargin = new Noise(seed ^ 0x1B56C4E9L);
        this.extensionProvince = new Noise(seed ^ 0x5A17C3DEL);
        this.cellInv = 1.0 / Math.max(64.0, params.plateScale);
        this.hotspotInv = 1.0 / Math.max(400.0, params.volcanoSpacing);
    }

    /** Everything a caller needs about the tectonic setting of one column. */
    public static final class Setting {
        /** -1 (divergent) .. +1 (convergent). */
        public double convergence;
        /** 0 far from any plate boundary, 1 exactly on one. */
        public double boundary;
        /** 0..1, narrow: the fault/lineament trace itself. */
        public double fault;
        /** 0..1, offshore expression of a convergent margin: the trench. */
        public double trench;
        /** 0..1, onshore expression of a convergent margin: the volcanic arc. */
        public double arc;
        /** 0..1, divergent ridge: rift valley on land, mid-ocean ridge under water. */
        public double rift;
        /**
         * 0..1, crustal extension. Drives Basin and Range fault-block terrain: horsts, grabens and the
         * steep range-front scarps with bajadas at their feet.
         */
        public double extension;
        /** 0..1, distance-weighted hotspot influence. */
        public double hotspot;
        /** Plate identifier, stable per plate. Useful for province coherence. */
        public long plateId;

        /**
         * Offshore expression of subduction. Trenches only form where an oceanic plate is consumed,
         * so this is gated on the boundary being sharp and convergent.
         */
        public double trenchDepth() {
            return Interp.clamp(convergence * boundary * 1.15, 0.0, 1.0);
        }
        /** Dip direction of the belt, radians; drives strike-aligned ridges. */
        public double beltAzimuth;
    }

    private final Setting scratch = new Setting();

    /**
     * Not thread safe (reuses a scratch object). Callers on worldgen threads must use
     * {@link #sampleInto(int, int, Setting)} with their own {@link Setting}, or {@link #sample}
     * which allocates.
     */
    public Setting sample(int x, int z) {
        Setting s = new Setting();
        sampleInto(x, z, s);
        return s;
    }

    public void sampleInto(int x, int z, Setting s) {
        double px = x * cellInv;
        double pz = z * cellInv;
        Noise.Worley w = plate.worley2(px, pz, 0.42);

        // ---- plate velocities -------------------------------------------------
        double[] v1 = plateVelocity(w.cellX, w.cellZ);
        double[] v2 = plateVelocity(w.cell2X, w.cell2Z);
        s.plateId = Hash.hash(plate.seed(), w.cellX, w.cellZ);

        // Unit vector from plate 1 to plate 2 approximates the boundary normal.
        double[] c1 = plateCentroid(w.cellX, w.cellZ);
        double[] c2 = plateCentroid(w.cell2X, w.cell2Z);
        double nx = c2[0] - c1[0];
        double nz = c2[1] - c1[1];
        double nl = Math.sqrt(nx * nx + nz * nz);
        if (nl < 1e-9) {
            nx = 1;
            nz = 0;
            nl = 1;
        }
        nx /= nl;
        nz /= nl;
        s.beltAzimuth = Math.atan2(nz, nx);

        // Convergence = relative velocity projected on the boundary normal.
        double dvx = v1[0] - v2[0];
        double dvz = v1[1] - v2[1];
        double normal = dvx * nx + dvz * nz;
        double tangential = Math.abs(-dvx * nz + dvz * nx);
        s.convergence = Interp.clamp(normal * 1.6, -1.0, 1.0);

        // ---- proximity fields -------------------------------------------------
        // border() is 0 on the boundary and grows inward, in cell units.
        double bd = w.border();
        s.boundary = Math.exp(-bd / BELT_WIDTH);
        s.fault = Math.exp(-bd / FAULT_WIDTH) * p.faultiness;

        // Large scale modulation so not every boundary is an active orogen.
        double activity = Interp.smoothstep(-0.15, 0.45,
                activeMargin.fbm2(x * cellInv * 0.35 + 40.0, z * cellInv * 0.35 - 17.0, 3, 2.0, 0.5));
        activity = Interp.lerp(0.35, 1.0, activity);

        double conv = Math.max(0.0, s.convergence);
        double div = Math.max(0.0, -s.convergence);
        double transf = Math.max(0.0, 1.0 - Math.abs(s.convergence) * 3.0)
                * Interp.smoothstep(0.15, 0.75, tangential);

        s.arc = Interp.clamp(conv * s.boundary * activity * p.orogenyGain, 0.0, 1.0);
        s.rift = Interp.clamp(div * s.boundary * 1.25, 0.0, 1.0);
        // Regional extension: the Basin and Range is not a single rift but a whole province of the
        // crust being pulled apart, so it needs its own very large scale field.
        double province = Interp.smoothstep(-0.10 + p.extensionBias * 0.55,
                0.42 - p.extensionBias * 0.30,
                activeMargin.fbm2(x / p.extensionProvinceScale + 200.0,
                        z / p.extensionProvinceScale - 140.0, 3, 2.0, 0.5));
        s.extension = Interp.clamp(s.rift * 0.85 + province * 0.92, 0.0, 1.0);
        s.fault = Interp.clamp(s.fault * (0.35 + 0.65 * transf + 0.35 * conv), 0.0, 1.0);

        // ---- hotspots ---------------------------------------------------------
        if (p.volcanoSpacing > 0) {
            Noise.Worley hs = hotspot.worley2(x * hotspotInv, z * hotspotInv, 0.5);
            double d = hs.f1;
            double present = Hash.unit(Hash.hash(hotspot.seed(), hs.cellX, hs.cellZ)) < 0.55 ? 1.0 : 0.0;
            s.hotspot = present * Math.exp(-(d * d) / 0.010);
        } else {
            s.hotspot = 0.0;
        }
    }

    /** Rigid velocity of one plate, from a smooth mantle-flow field sampled at the plate centroid. */
    private double[] plateVelocity(int cellX, int cellZ) {
        double[] c = plateCentroid(cellX, cellZ);
        double fx = c[0] * 0.55;
        double fz = c[1] * 0.55;
        double vx = mantleFlow.fbm2(fx + 3.7, fz - 9.1, 3, 2.0, 0.5);
        double vz = mantleFlow.fbm2(fx - 21.3, fz + 14.8, 3, 2.0, 0.5);
        // Add a slow global rotation so plates do not all drift the same way.
        double spin = 0.18;
        double rx = -c[1] * spin;
        double rz = c[0] * spin;
        return new double[] {vx + rx, vz + rz};
    }

    /** Feature point of a plate cell (matches {@link Noise#worley2} jitter, amplitude 0.42). */
    private double[] plateCentroid(int cellX, int cellZ) {
        long h = Hash.hash(plate.seed(), cellX, cellZ);
        double jx = (Hash.unit(h) - 0.5) * 2.0 * 0.42;
        double jz = (Hash.unit(Hash.mix64(h ^ 0x5DEECE66DL)) - 0.5) * 2.0 * 0.42;
        return new double[] {cellX + jx, cellZ + jz};
    }
}
