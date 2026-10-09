package dev.terrarealis.kernel.geo;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.math.Hash;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;

/**
 * Karst: landscape built by dissolution rather than by mechanical erosion.
 *
 * <p>Carbonate terrain does not behave like any other rock. Water slightly acidified by soil CO2
 * dissolves it, and the drainage goes <i>underground</i>, so the surface is left with a set of forms
 * that cannot be produced by fluvial processes at all:
 *
 * <ul>
 *   <li><b>Dolines</b> — closed, funnel-shaped depressions, tens of metres across, that pock the
 *       surface at densities of hundreds per square kilometre. Where they merge they become
 *       <b>uvalas</b>.</li>
 *   <li><b>Cone and cockpit karst</b> — in the wet tropics dissolution is so aggressive that what
 *       survives between the depressions is a field of steep conical hills. This is Guilin, Ha Long and
 *       the Mengione of fantasy art, and it is included because it is one of the most striking
 *       landscapes on the planet.</li>
 *   <li><b>Ponors</b> — the point where a surface stream reaches soluble rock and simply disappears.
 *       Downstream of a ponor the channel is dry, which is deeply counter-intuitive to a player who has
 *       only ever seen vanilla rivers, and completely correct.</li>
 *   <li><b>Dry valleys</b> — the abandoned courses of streams that used to flow on the surface before
 *       the drainage was captured underground. They survive because dissolution is slower than the
 *       incision that cut them.</li>
 * </ul>
 */
public final class Karst {

    private Karst() {}

    /**
     * @param karst   per-cell karst index, 0..1
     * @param precip  per-cell annual precipitation in mm — cone karst needs a wet tropics
     */
    public static void apply(Erosion.Grid g, double[] karst, double[] precip, Noise coneNoise,
                             long seed, GenParams p) {
        int w = g.w;
        int cell = p.erosionCellSize;
        int originX = g.originBlockX;
        int originZ = g.originBlockZ;

        // ------------------------------------------------------------- dolines
        for (int cz = 2; cz < w - 2; cz++) {
            for (int cx = 2; cx < w - 2; cx++) {
                int i = cz * w + cx;
                double k = karst[i];
                if (k < 0.30) {
                    continue;
                }
                int bx = originX + cx * cell;
                int bz = originZ + cz * cell;
                long h = Hash.hash(seed ^ 0x4A571L, bx >> 1, bz >> 1);
                // Doline density: roughly one per 150 m of ground in well-developed karst.
                double density = 0.028 * Interp.smoothstep(0.30, 0.85, k);
                if (Hash.unit(h) > density) {
                    continue;
                }
                double radius = (1.6 + 3.4 * Hash.unit(Hash.mix64(h ^ 0x9E3779B9L)))
                        * (0.6 + 0.8 * k);
                double depth = (1.6 + 6.5 * Hash.unit(Hash.mix64(h ^ 0x2545F491L)))
                        * (0.5 + 0.9 * k);
                int r = (int) Math.ceil(radius);
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        int nx = cx + dx;
                        int nz = cz + dz;
                        if (nx < 1 || nz < 1 || nx >= w - 1 || nz >= w - 1) {
                            continue;
                        }
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > radius) {
                            continue;
                        }
                        int j = nz * w + nx;
                        double u = 1.0 - dist / radius;
                        // Funnel: steep walls, flat-ish floor, slightly raised lip from the residue.
                        double bowl = Math.pow(u, 0.72);
                        double lip = Math.exp(-Math.pow((dist / radius - 0.92) / 0.16, 2.0)) * 0.18;
                        g.height[j] -= depth * bowl;
                        g.height[j] += depth * lip;
                        g.denudation[j] += (float) (depth * bowl);
                        // A doline floor is a sediment trap and often holds a seasonal pond.
                        g.deposition[j] += (float) (depth * bowl * 0.22);
                        if (g.waterLevel[j] == Erosion.Grid.NO_WATER
                                && g.aridity[j] < 0.55 && bowl > 0.75
                                && g.filled[j] - g.height[j] > 1.2) {
                            g.waterLevel[j] = g.height[j] + 0.85;
                        }
                    }
                }
            }
        }

        // --------------------------------------------------- cone / cockpit karst
        // Only in the wet tropics, only on strongly soluble rock: the classic tower-karst setting.
        double coneScale = 1.0 / 130.0;
        for (int cz = 1; cz < w - 1; cz++) {
            for (int cx = 1; cx < w - 1; cx++) {
                int i = cz * w + cx;
                if (karst[i] < 0.62 || precip[i] < 1150.0) {
                    continue;
                }
                int bx = originX + cx * cell;
                int bz = originZ + cz * cell;
                Noise.Worley cw = coneNoise.worley2(bx * coneScale, bz * coneScale, 0.45);
                long id = Hash.hash(coneNoise.seed(), cw.cellX, cw.cellZ);
                double radius = (34.0 + 44.0 * Hash.unit(Hash.mix64(id ^ 0x51ED270BL))) * coneScale
                        / coneScale; // in blocks
                double dist = cw.f1 / coneScale;
                if (dist >= radius) {
                    continue;
                }
                double u = 1.0 - dist / radius;
                // Steep conical residual hill, flat cockpit between them.
                double cone = Math.pow(u, 1.35);
                double strength = Interp.smoothstep(0.62, 0.95, karst[i])
                        * Interp.smoothstep(1150.0, 1900.0, precip[i]);
                double amp = (46.0 + 62.0 * Hash.unit(Hash.mix64(id ^ 0x3C6EF372L))) * strength;
                if (g.height[i] < p.seaLevel + 4) {
                    continue;
                }
                g.height[i] += cone * amp;
                g.denudation[i] -= (float) (cone * amp);
            }
        }

        // ------------------------------------------------------------- ponors
        // Where a channel crosses onto soluble rock the water goes underground and the channel below
        // that point is dry. Marked so the column builder stops placing water and cuts a swallow hole.
        for (int cz = 1; cz < w - 1; cz++) {
            for (int cx = 1; cx < w - 1; cx++) {
                int i = cz * w + cx;
                if (karst[i] < 0.45 || g.channel[i] < 0.25f) {
                    continue;
                }
                if (g.waterLevel[i] == Erosion.Grid.NO_WATER) {
                    continue;
                }
                // Only swallow where the local water table can actually take the water: not on a
                // coastal plain a metre above sea level.
                if (g.height[i] < p.seaLevel + 6) {
                    continue;
                }
                double chance = 0.05 * karst[i];
                if (Hash.unit(Hash.hash(seed ^ 0xF0A0E1L, i, cx)) < chance) {
                    g.swallow[i] = 1.0f;
                    g.waterLevel[i] = Erosion.Grid.NO_WATER;
                    // The stream cut down to the level where it was swallowed; leave a pit.
                    g.height[i] -= 1.6;
                }
            }
        }
        // Dry the reaches downstream of a swallow hole.
        for (int pass = 0; pass < 2; pass++) {
            for (int cz = 1; cz < w - 1; cz++) {
                for (int cx = 1; cx < w - 1; cx++) {
                    int i = cz * w + cx;
                    if (g.swallow[i] <= 0.5f) {
                        continue;
                    }
                    int d = g.dir[i];
                    if (d < 0) {
                        continue;
                    }
                    int j = (cz + Erosion.DZ[d]) * w + (cx + Erosion.DX[d]);
                    if (g.swallow[j] < 0.5f && g.aridity[j] < 0.9) {
                        g.swallow[j] = g.swallow[i] * 0.72f;
                        g.waterLevel[j] = Erosion.Grid.NO_WATER;
                    }
                }
            }
        }
    }

    /** Interpolated karst index used for the swallow-hole dryness gradient. */
    public static double dryValley(double swallow, double aridity) {
        return Interp.clamp(swallow * (0.4 + 0.6 * aridity), 0.0, 1.0);
    }
}
