package dev.terrarealis.tools;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.TerraKernel;
import dev.terrarealis.kernel.geo.Tectonics;
import dev.terrarealis.kernel.geo.Volcanism;

public final class Diag3 {
    public static void main(String[] a) {
        GenParams p = GenParams.defaults();
        p.sanitise();
        TerraKernel k = new TerraKernel(0x5EED1234L, p);
        Tectonics.Setting t = new Tectonics.Setting();
        Volcanism.Edifice e = new Volcanism.Edifice();
        int n = 0;
        double extSum = 0, extMax = 0, arcSum = 0, hMin = 1e9, hMax = -1e9, hSum = 0;
        double insel = 0;
        int[] hist = new int[12];
        for (int z = -40000; z <= 40000; z += 1600) {
            for (int x = -40000; x <= 40000; x += 1600) {
                k.tectonics().sampleInto(x, z, t);
                double h = k.upliftHeight(x, z, e);
                extSum += t.extension;
                extMax = Math.max(extMax, t.extension);
                arcSum += t.arc;
                hMin = Math.min(hMin, h);
                hMax = Math.max(hMax, h);
                hSum += h;
                double em = (h - p.seaLevel) * p.metersPerBlock;
                int b = (int) Math.max(0, Math.min(11, Math.floor((em + 1600) / 500.0)));
                hist[b]++;
                n++;
            }
        }
        System.out.printf("n=%d  extension mean %.3f max %.3f | arc mean %.3f%n", n, extSum / n, extMax, arcSum / n);
        System.out.printf("uplift blocks min %.0f max %.0f mean %.0f%n", hMin, hMax, hSum / n);
        StringBuilder sb = new StringBuilder("elev hist (m, -1600..4400 by 500): ");
        for (int h2 : hist) {
            sb.append(String.format("%.2f ", h2 / (double) n));
        }
        System.out.println(sb);
        // fraction of land above 1000 m, above 2000 m
        int land = 0, hi = 0, hi2 = 0;
        for (int z = -40000; z <= 40000; z += 800) {
            for (int x = -40000; x <= 40000; x += 800) {
                double h = k.upliftHeight(x, z);
                if (h > p.seaLevel) {
                    land++;
                    double em = (h - p.seaLevel) * p.metersPerBlock;
                    if (em > 1000) {
                        hi++;
                    }
                    if (em > 2000) {
                        hi2++;
                    }
                }
            }
        }
        System.out.printf("land>sea %.1f%%  >1000m %.1f%%  >2000m %.1f%%%n",
                100.0 * land / 10201, 100.0 * hi / 10201, 100.0 * hi2 / 10201);
    }
}
