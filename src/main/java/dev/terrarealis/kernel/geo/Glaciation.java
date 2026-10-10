package dev.terrarealis.kernel.geo;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.math.Interp;

/**
 * Glacial modification of an already-eroded landscape.
 *
 * <p>Ice is a different eroder than water. It does not follow the steepest descent of a single cell, it
 * is thick, it plugs its own valley, and it abrades the whole cross-section instead of cutting a V. The
 * consequences are the most recognisable landforms on Earth and none of them can be produced by fluvial
 * erosion:
 *
 * <ul>
 *   <li><b>U-shaped valleys</b> — the flux-carrying reach is deepened and, crucially, <i>widened</i> by
 *       lateral abrasion, so the valley walls are steep and straight and the floor is flat.</li>
 *   <li><b>Cirques and aretes</b> — accumulation zones at the head of a glacier overdeepen into
 *       armchair hollows; two cirques back-to-back leave a knife-edge ridge between them.</li>
 *   <li><b>Terminal and lateral moraines</b> — everything the glacier carried is dumped at the snout as
 *       a ridge, not as a fan, because ice transport stops abruptly rather than grading out.</li>
 *   <li><b>Fjords</b> — a glacial valley whose floor was cut below sea level and is then drowned. These
 *       appear automatically: no special case, just overdeepening plus a coastline.</li>
 *   <li><b>Glacial drift</b> — the surface mantle becomes unsorted till with erratics, handled in
 *       {@link Geology} rather than here.</li>
 * </ul>
 *
 * <p>Applied after fluvial erosion and before channels are carved, so meltwater streams still organise
 * themselves over the glacial topography the way they do in reality.
 */
public final class Glaciation {

    private Glaciation() {}

    /**
     * @param snowLine per-cell equilibrium line altitude in blocks (not metres)
     * @param precip   per-cell annual precipitation in mm; more snow means thicker ice
     */
    public static void apply(Erosion.Grid g, double[] snowLine, double[] precip, double glacialGain) {
        GenParams p = g.params;
        int w = g.w;
        int n = w * w;
        double[] ice = new double[n];
        double[] flux = new double[n];

        // --- accumulation ---------------------------------------------------
        for (int i = 0; i < n; i++) {
            double above = g.height[i] - snowLine[i];
            if (above <= 0) {
                continue;
            }
            // Ice thickness grows with how far above the ELA we are, and with snowfall.
            double supply = Interp.smoothstep(150.0, 1400.0, precip[i]);
            ice[i] = Math.min(140.0, above * (0.10 + 0.30 * supply)) * glacialGain;
        }

        // --- ice flux along the drainage network ----------------------------
        // Ice follows the pre-glacial valleys, which is why glacial troughs are inherited from fluvial
        // ones and why fjords are always dendritic rather than straight.
        long[] order = new long[n];
        for (int i = 0; i < n; i++) {
            long q = (long) Math.round(g.height[i] * 64.0);
            order[i] = ((2_000_000L - q) << 21) | (i & 0x1FFFFFL);
        }
        java.util.Arrays.sort(order);
        System.arraycopy(ice, 0, flux, 0, n);
        for (long k : order) {
            int i = (int) (k & 0x1FFFFFL);
            int d = g.dir[i];
            if (d < 0 || flux[i] <= 0.01) {
                continue;
            }
            int cx = i % w;
            int cz = i / w;
            int nx = cx + Erosion.DX[d];
            int nz = cz + Erosion.DZ[d];
            if (nx < 0 || nz < 0 || nx >= w || nz >= w) {
                continue;
            }
            int j = nz * w + nx;
            // Ablation as the ice descends into warmer air.
            flux[j] += flux[i] * 0.94;
        }

        double carveThreshold = 22.0;
        // --- U-valley carving: deepen --------------------------------------
        for (int cz = 1; cz < w - 1; cz++) {
            for (int cx = 1; cx < w - 1; cx++) {
                int i = cz * w + cx;
                if (flux[i] < carveThreshold) {
                    continue;
                }
                double t = Math.log(flux[i] / carveThreshold) / Math.log(2.0);
                double deepen = Interp.clamp(2.0 + 3.4 * t, 2.0, 26.0) * glacialGain;
                // Cirques: overdeepening where the flux is concentrated on a steep headwall.
                if (g.slope[i] > 18.0 && flux[i] > carveThreshold * 1.6) {
                    deepen *= 1.55;
                }
                g.height[i] -= deepen;
                g.denudation[i] += (float) deepen;
                g.glacier[i] = (float) Interp.clamp(t * 0.32, 0.0, 1.0);
            }
        }

        // --- U-valley carving: widen laterally ------------------------------
        // Straight, steep trough walls come from smoothing perpendicular to flow, not along it.
        double[] widened = g.height.clone();
        for (int cz = 2; cz < w - 2; cz++) {
            for (int cx = 2; cx < w - 2; cx++) {
                int i = cz * w + cx;
                if (g.glacier[i] <= 0.02f) {
                    continue;
                }
                int d = g.dir[i];
                if (d < 0) {
                    continue;
                }
                // Lateral (cross-valley) direction.
                int lx = -Erosion.DZ[d];
                int lz = Erosion.DX[d];
                double strength = Interp.clamp(g.glacier[i] * 1.6, 0.0, 0.55);
                int reach = 1 + (int) (g.glacier[i] * 2.2);
                double sum = g.height[i];
                int count = 1;
                for (int s = 1; s <= reach; s++) {
                    int ax = cx + lx * s;
                    int az = cz + lz * s;
                    int bx = cx - lx * s;
                    int bz = cz - lz * s;
                    if (ax >= 0 && az >= 0 && ax < w && az < w) {
                        sum += g.height[az * w + ax];
                        count++;
                    }
                    if (bx >= 0 && bz >= 0 && bx < w && bz < w) {
                        sum += g.height[bz * w + bx];
                        count++;
                    }
                }
                double mean = sum / count;
                widened[i] = Interp.lerp(strength, g.height[i], Math.min(mean, g.height[i]));
                // The abraded debris goes onto the valley sides: lateral moraines.
                double debris = (g.height[i] - widened[i]) * 0.55;
                if (debris > 0) {
                    for (int s = reach; s <= reach + 1; s++) {
                        depositAt(widened, w, cx + lx * s, cz + lz * s, debris * 0.35);
                        depositAt(widened, w, cx - lx * s, cz - lz * s, debris * 0.35);
                    }
                }
            }
        }
        System.arraycopy(widened, 0, g.height, 0, n);

        // --- terminal moraines ---------------------------------------------
        for (int cz = 2; cz < w - 2; cz++) {
            for (int cx = 2; cx < w - 2; cx++) {
                int i = cz * w + cx;
                if (flux[i] < carveThreshold) {
                    continue;
                }
                int d = g.dir[i];
                if (d < 0) {
                    continue;
                }
                int j = (cz + Erosion.DZ[d]) * w + (cx + Erosion.DX[d]);
                if (j < 0 || j >= n) {
                    continue;
                }
                // The snout: high flux here, none downstream. Everything carried is dropped in a ridge.
                if (flux[j] < carveThreshold) {
                    double load = Interp.clamp(flux[i] * 0.028, 0.0, 7.0) * glacialGain;
                    g.height[j] += load;
                    g.deposition[j] += (float) load;
                    // Arc the moraine across the valley mouth.
                    int lx = -Erosion.DZ[d];
                    int lz = Erosion.DX[d];
                    for (int s = 1; s <= 2; s++) {
                        g.height[idx(w, cx + Erosion.DX[d] + lx * s, cz + Erosion.DZ[d] + lz * s)] += load * 0.6;
                        g.height[idx(w, cx + Erosion.DX[d] - lx * s, cz + Erosion.DZ[d] - lz * s)] += load * 0.6;
                    }
                }
            }
        }
    }

    private static void depositAt(double[] h, int w, int cx, int cz, double amount) {
        if (cx < 1 || cz < 1 || cx >= w - 1 || cz >= w - 1) {
            return;
        }
        h[cz * w + cx] += amount;
    }

    private static int idx(int w, int cx, int cz) {
        return Interp.clamp(cz, 0, w - 1) * w + Interp.clamp(cx, 0, w - 1);
    }
}
