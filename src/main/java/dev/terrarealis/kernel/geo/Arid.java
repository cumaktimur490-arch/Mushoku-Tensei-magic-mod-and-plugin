package dev.terrarealis.kernel.geo;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.math.Hash;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;

/**
 * Landforms that only exist where there is almost no water and almost no vegetation.
 *
 * <p>An arid landscape is not a drier version of a humid one; it is a different system. Rainfall is
 * episodic and violent, there is no root mat to hold slopes together, and there is no through-flow —
 * so the landscape is built by two things: <b>sheet floods that incise</b> and <b>the same floods
 * dumping everything they carry the instant the gradient drops</b>. That produces the forms this file
 * implements.
 *
 * <ul>
 *   <li><b>Alluvial fans and bajadas.</b> Where a range-front canyon debouches onto a basin floor the
 *       flow spreads, competence collapses, and a cone of gravel is built at about 3 degrees. Adjacent
 *       fans coalesce into a bajada — the smooth, gently tilted apron that rings every range in
 *       Anza-Borrego and the Sonoran, and the reason desert mountains look like they are standing in a
 *       skirt rather than in a valley.</li>
 *   <li><b>Badlands.</b> Handled by the aridity-aware erosion regime in {@link Erosion}: on bare soft
 *       sediment the channel-initiation threshold collapses, so the surface is cut into a dense lattice
 *       of gullies with knife-edge divides. No soil, no creep, no rounding.</li>
 *   <li><b>Playas.</b> Endorheic basins plane flat and crust over with evaporites.</li>
 *   <li><b>Dunes.</b> Aeolian transport is modelled with the real geometry: a shallow stoss slope of
 *       about 12 degrees and a slip face at the angle of repose of dry sand, about 33 degrees, which
 *       fixes the height-to-wavelength ratio at roughly 1:6. Dunes are oriented perpendicular to the
 *       formative wind, so they rotate with the wind field instead of being random bumps.</li>
 *   <li><b>Riparian corridors.</b> Desert rivers are lined with green even when the interfluves are
 *       bare, because the water table reaches the surface in the channel. That green line is the most
 *       recognisable feature of aerial desert photography, so it gets an explicit flag.</li>
 * </ul>
 */
public final class Arid {

    /** Angle of repose of dry sand, degrees — sets the slip-face gradient. */
    private static final double SLIP_FACE_DEG = 33.0;
    /** Stoss (windward) slope of a transverse dune, degrees. */
    private static final double STOSS_DEG = 11.5;
    /** Fan surface gradient, degrees. Real fans run 2-6 degrees. */
    private static final double FAN_SLOPE_DEG = 3.2;

    private Arid() {}

    /**
     * Builds alluvial fans at range fronts and lets them coalesce into bajadas.
     *
     * <p>A fan is detected where the flow leaves a steep reach and enters a shallow one with a
     * substantial catchment behind it. The deposit is a cone whose apex is at the canyon mouth and
     * whose surface gradient is fixed at about 3 degrees; because the cone is built by raising the
     * existing surface rather than replacing it, neighbouring fans merge into a continuous apron.
     */
    public static void alluvialFans(Erosion.Grid g) {
        GenParams p = g.params;
        if (!p.alluvialFans) {
            return;
        }
        int w = g.w;
        double cell = p.erosionCellSize;
        double fanGradient = Math.tan(Math.toRadians(FAN_SLOPE_DEG)) * cell;
        double[] add = new double[w * w];
        float[] mask = new float[w * w];

        for (int cz = 2; cz < w - 2; cz++) {
            for (int cx = 2; cx < w - 2; cx++) {
                int i = cz * w + cx;
                double arid = g.aridity[i];
                if (arid < 0.28) {
                    continue;
                }
                int d = g.dir[i];
                if (d < 0) {
                    continue;
                }
                int jx = cx + Erosion.DX[d];
                int jz = cz + Erosion.DZ[d];
                int j = jz * w + jx;
                if (jx < 1 || jz < 1 || jx >= w - 1 || jz >= w - 1) {
                    continue;
                }
                // Range front: steep above, shallow below, with a real catchment.
                if (g.slope[i] < 9.0 || g.slope[j] > 7.0) {
                    continue;
                }
                if (g.acc[j] < g.channelThreshold[j] * 1.5) {
                    continue;
                }
                if (g.waterLevel[j] != Erosion.Grid.NO_WATER && g.playa[j] <= 0) {
                    continue; // a standing lake absorbs the fan instead
                }
                double t = Math.log(Math.max(2.0, g.acc[j]) / Math.max(8.0, g.channelThreshold[j]))
                        / Math.log(2.0);
                double radius = Interp.clamp(2.0 + 1.55 * t, 2.0, 15.0) * (0.7 + 0.6 * arid);
                double apex = g.height[j];
                int r = (int) Math.ceil(radius);
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        int nx = jx + dx;
                        int nz = jz + dz;
                        if (nx < 1 || nz < 1 || nx >= w - 1 || nz >= w - 1) {
                            continue;
                        }
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > radius) {
                            continue;
                        }
                        int k = nz * w + nx;
                        // Cone surface, blurred at the toe so the fan feathers into the basin floor.
                        double cone = apex - dist * fanGradient;
                        double toe = 1.0 - Interp.smoothstep(radius * 0.55, radius, dist);
                        double target = Interp.lerp(g.height[k], cone, toe);
                        if (target > g.height[k] + add[k]) {
                            add[k] = target - g.height[k];
                            mask[k] = (float) Math.max(mask[k], toe * Interp.smoothstep(0.28, 0.75, arid));
                        }
                    }
                }
            }
        }
        for (int i = 0; i < add.length; i++) {
            if (add[i] > 0) {
                g.height[i] += add[i] * p.fanGain;
                g.deposition[i] += (float) (add[i] * p.fanGain);
                g.fan[i] = (float) Math.max(g.fan[i], mask[i] * p.fanGain);
                // Fans are permeable gravel: water disappears into them and re-emerges at the toe.
                g.sandSupply[i] = (float) Math.max(g.sandSupply[i], mask[i] * 0.45f);
            }
        }
    }

    /**
     * Aeolian sand supply.
     *
     * <p>Dunes only form where there is loose sand <i>and</i> the wind is strong <i>and</i> the ground
     * is dry and bare. Real sand sources are playas (the classic source of desert dune fields), river
     * beds that dry out seasonally, coastal strands and disintegrating sandstone. Supply is therefore
     * accumulated from those sources and decays with distance downwind.
     */
    public static void sandSupply(Erosion.Grid g) {
        int w = g.w;
        int sea = g.params.seaLevel;
        for (int cz = 1; cz < w - 1; cz++) {
            for (int cx = 1; cx < w - 1; cx++) {
                int i = cz * w + cx;
                double supply = g.sandSupply[i];
                // Playa floors are the single biggest dune-field source on Earth.
                supply = Math.max(supply, g.playa[i] * 0.95);
                // Dry, sandy, gently sloping ground above the water table.
                double dry = Interp.smoothstep(0.42, 0.85, g.aridity[i]);
                double flat = 1.0 - Interp.smoothstep(6.0, 18.0, g.slope[i]);
                double aboveWater = g.waterTable[i] == Erosion.Grid.NO_WATER
                        || g.height[i] - g.waterTable[i] > 2.5 ? 1.0 : 0.15;
                supply = Math.max(supply, dry * flat * aboveWater * 0.55);
                // Coastal strand.
                if (g.oceanDistance[i] < 6 * g.params.erosionCellSize
                        && Math.abs(g.height[i] - sea) < 6.0) {
                    supply = Math.max(supply, 0.75 * flat);
                }
                g.sandSupply[i] = (float) Interp.clamp(supply, 0.0, 1.0);
            }
        }
    }

    /** Result of a dune probe for one column. */
    public static final class Dunes {
        public double height;
        public double slipFace;
    }

    /**
     * Transverse and barchanoid dune relief for one column.
     *
     * <p>The crest line is perpendicular to the formative wind, so the dune field rotates with the
     * latitude-dependent wind vector — trade-wind dunes and westerly dunes in the same world run at
     * different angles, which is correct and which players read as "this looks planned".
     *
     * @param windX   unit wind travel direction, +x
     * @param windZ   unit wind travel direction, +z
     * @param supply  0..1 available loose sand
     * @param aridity 0..1
     */
    public static void dunes(double x, double z, double windX, double windZ, double supply,
                             double aridity, double slopeDeg, GenParams p, Noise aeolian, Dunes out) {
        out.height = 0.0;
        out.slipFace = 0.0;
        if (!p.dunes || supply < 0.18 || aridity < 0.30 || slopeDeg > 9.0) {
            return;
        }
        double wavelength = p.duneWavelength * (0.75 + 0.5 * supply);
        // Height follows from the two real slope angles and the wavelength: L = H/tan(stoss) + H/tan(slip).
        double geom = 1.0 / Math.tan(Math.toRadians(STOSS_DEG)) + 1.0 / Math.tan(Math.toRadians(SLIP_FACE_DEG));
        double height = Math.min(p.duneHeight, wavelength / geom) * Interp.smoothstep(0.18, 0.75, supply);
        if (height < 0.6) {
            return;
        }

        double u = (x * windX + z * windZ) / wavelength;      // along wind
        double v = (-x * windZ + z * windX) / wavelength;      // across wind

        // Barchanoid: the crest line wanders and the amplitude is modulated across wind, so straight
        // transverse ridges break up into crescents and patches of interdune flat.
        double crestWander = aeolian.fbm2(v * 0.55 + 4.1, u * 0.21 - 9.3, 3, 2.0, 0.5);
        double patch = Interp.smoothstep(-0.25, 0.45,
                aeolian.fbm2(x / (wavelength * 9.0) + 61.0, z / (wavelength * 9.0) - 23.0, 3, 2.0, 0.5));
        if (patch < 0.02) {
            return;
        }

        double phase = u + crestWander * 0.42;
        double f = phase - Math.floor(phase);
        double stoss = 1.0 / (1.0 + Math.tan(Math.toRadians(STOSS_DEG)) / Math.tan(Math.toRadians(SLIP_FACE_DEG)));
        double profile;
        if (f < stoss) {
            // Shallow windward ramp.
            profile = f / stoss;
        } else {
            // Slip face at the angle of repose.
            profile = 1.0 - (f - stoss) / (1.0 - stoss);
        }
        // Round the crest slightly; real crests are sharp but not mathematical.
        profile = profile * (0.86 + 0.14 * Interp.smoothstep(0.05, 0.55, profile));
        out.height = height * profile * patch;
        out.slipFace = (f >= stoss && f < stoss + 0.22) ? Interp.smoothstep(stoss, stoss + 0.10, f) : 0.0;
    }

    /**
     * 0..1 — is this column inside a desert riparian corridor?
     *
     * <p>Where the water table comes to within a couple of blocks of the surface along a channel in an
     * arid region, the vegetation is completely different from the surrounding interfluve. Getting this
     * right is what makes a desert read as a desert instead of as a brown plain with a blue line in it.
     */
    public static double riparian(double aridity, double channel, double saturation,
                                  double slopeDeg, GenParams p) {
        if (!p.riparianCorridors || aridity < 0.30) {
            return 0.0;
        }
        // Only the channel core and its immediate bank: a riparian corridor is tens of metres wide,
        // not kilometres. The channel field is splatted over several cells, so the threshold sits high.
        double byChannel = Interp.smoothstep(0.72, 0.97, channel);
        double byWater = Interp.smoothstep(0.70, 0.98, saturation);
        double flat = 1.0 - Interp.smoothstep(10.0, 22.0, slopeDeg);
        return Interp.clamp(Math.max(byChannel, byWater * 0.85) * (0.35 + 0.65 * flat) * aridity, 0.0, 1.0);
    }

    /**
     * Desert pavement / rock varnish flag: the interfluve surface in hyperarid terrain is a lag deposit
     * of coarse gravel, not soil. Used by the column builder to pick the surface material.
     */
    public static double pavement(double aridity, double soilThickness, double slopeDeg) {
        return Interp.clamp(Interp.smoothstep(0.55, 0.95, aridity)
                * (1.0 - Interp.smoothstep(0.4, 1.6, soilThickness))
                * (1.0 - Interp.smoothstep(3.0, 14.0, slopeDeg)), 0.0, 1.0);
    }

    /**
     * Painted-strata index for badlands.
     *
     * <p>The reason places like Painted Canyon and the Calcite Mine look the way they do is that
     * alternately coloured beds are exposed on steep, freshly cut slopes. This returns 0..1 for "show
     * the bedding colours here", which requires bare steep ground on soft, bedded sediment.
     */
    public static double paintedStrata(double aridity, double slopeDeg, double outcrop,
                                       boolean bedded, double softness, GenParams p) {
        if (!p.badlands) {
            return 0.0;
        }
        double steep = Interp.smoothstep(14.0, 34.0, slopeDeg);
        return Interp.clamp(p.badlandGain * p.strataContrast * aridity * steep
                * (bedded ? 1.0 : 0.45) * (0.35 + 0.65 * softness)
                * (0.45 + 0.55 * outcrop), 0.0, 1.0);
    }

    /** Deterministic 0..1 for scree/boulder coverage on a bare slope. */
    public static double boulderiness(int x, int z, double slopeDeg, double aridity, double hardness) {
        double n = 0.5 + 0.5 * Hash.signed(Hash.hash(0xB0A1DE2L, x >> 2, z >> 2));
        double steep = Interp.smoothstep(20.0, 40.0, slopeDeg);
        double dry = Interp.smoothstep(0.30, 0.80, aridity);
        return Interp.clamp((0.35 + 0.65 * n) * steep * (0.4 + 0.6 * dry) * (0.5 + 0.5 * hardness), 0.0, 1.0);
    }
}
