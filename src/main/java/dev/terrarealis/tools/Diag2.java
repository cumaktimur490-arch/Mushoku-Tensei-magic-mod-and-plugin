package dev.terrarealis.tools;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.geo.Erosion;

public final class Diag2 {
    public static void main(String[] a) {
        GenParams p = GenParams.defaults();
        p.sanitise();
        // A tilted plane with a V valley down the middle: accumulation must concentrate in the valley.
        int w = p.gridWidth();
        double[] base = new double[w * w];
        for (int cz = 0; cz < w; cz++) {
            for (int cx = 0; cx < w; cx++) {
                double tilt = (w - cz) * 0.15;
                double valley = Math.abs(cx - w / 2.0) * 0.06;
                base[cz * w + cx] = 60 + tilt + valley;
            }
        }
        Erosion.Grid g = Erosion.gridFromBase(p, 0, 0, base);
        Erosion.erode(g);
        double amax = 0;
        int at = -1;
        for (int i = 0; i < g.acc.length; i++) {
            if (g.acc[i] > amax) {
                amax = g.acc[i];
                at = i;
            }
        }
        int dirNeg = 0;
        for (int d : g.dir) {
            if (d < 0) {
                dirNeg++;
            }
        }
        System.out.printf("synthetic: acc max %.0f at %d (cx=%d cz=%d), dir<0 count %d / %d%n",
                amax, at, at % w, at / w, dirNeg, g.dir.length);
        // print a small column of acc along the valley
        for (int cz = w - 6; cz < w - 1; cz++) {
            System.out.printf("  cz=%d acc=%.0f dir=%d filled-h=%.3f%n", cz,
                    g.acc[cz * w + w / 2], g.dir[cz * w + w / 2],
                    g.filled[cz * w + w / 2] - g.height[cz * w + w / 2]);
        }
    }
}
