package dev.terrarealis.tools;

import dev.terrarealis.kernel.Column;
import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.Stratigrapher;
import dev.terrarealis.kernel.TerraKernel;
import dev.terrarealis.kernel.region.RegionTile;

/** Micro-benchmarks. Not a test: prints timings so the cost of each stage is visible. */
public final class Bench {
    public static void main(String[] a) {
        GenParams p = GenParams.defaults();
        p.sanitise();
        TerraKernel k = new TerraKernel(0x5EED1234L, p);
        Column c = new Column();

        for (int i = 0; i < 3000; i++) {
            k.column(i % 120, (i * 7) % 120, c);
        }
        long t2 = System.nanoTime();
        int T = 30;
        for (int i = 0; i < T; i++) {
            new RegionTile(k, 60 + i * 4, 20 + i * 3);
        }
        long t3 = System.nanoTime();
        System.out.printf("tile build      : %.1f ms each%n", (t3 - t2) / 1e6 / T);

        // Columns inside one already-built tile: this is the real per-chunk cost.
        k.column(64, 64, c);
        long t4 = System.nanoTime();
        int N = 60000;
        for (int i = 0; i < N; i++) {
            k.column((i * 3) % 128, (i * 5) % 128, c);
        }
        long t5 = System.nanoTime();
        double perCol = (t5 - t4) / 1000.0 / N;
        System.out.printf("column (cached) : %.2f us  -> %.2f ms/chunk%n", perCol, perCol * 256 / 1000.0);

        long t6 = System.nanoTime();
        int M = 20000;
        for (int i = 0; i < M; i++) {
            k.column((i * 3) % 128, (i * 5) % 128, c);
            Stratigrapher.decideSurface(c, p);
        }
        long t7 = System.nanoTime();
        System.out.printf("column+surface  : %.2f us%n", (t7 - t6) / 1000.0 / M);

        long t8 = System.nanoTime();
        int Q = 200000;
        for (int i = 0; i < Q; i++) {
            k.upliftHeight((i * 17) % 40000 - 20000, (i * 29) % 40000 - 20000);
        }
        long t9 = System.nanoTime();
        System.out.printf("upliftHeight    : %.2f us%n", (t9 - t8) / 1000.0 / Q);

        TerraKernel.LatticeCaves lc = new TerraKernel.LatticeCaves(k);
        Column[] cols = new Column[256];
        for (int i = 0; i < 256; i++) {
            cols[i] = new Column();
            k.column(1000 + (i % 16), 1000 + (i / 16), cols[i]);
        }
        long t10 = System.nanoTime();
        int R = 200;
        for (int i = 0; i < R; i++) {
            lc.prepare(1000, 1000, cols, p.minY, p.maxY);
        }
        long t11 = System.nanoTime();
        System.out.printf("cave lattice    : %.2f ms/chunk%n", (t11 - t10) / 1e6 / R);
    }
}
