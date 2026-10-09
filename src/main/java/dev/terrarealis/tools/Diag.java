package dev.terrarealis.tools;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.TerraKernel;
import dev.terrarealis.kernel.geo.Erosion;
import dev.terrarealis.kernel.geo.Glaciation;
import dev.terrarealis.kernel.region.RegionTile;

public final class Diag {
    public static void main(String[] a) {
        GenParams p = GenParams.defaults();
        p.sanitise();
        dev.terrarealis.kernel.region.RegionTile.DEBUG = true;
        dev.terrarealis.kernel.geo.Erosion.DEBUG = true;
        TerraKernel k = new TerraKernel(0x5EED1234L, p);
        int tx = Math.floorDiv(-10020, 128);
        int tz = Math.floorDiv(-26000, 128);
        System.out.println("tile " + tx + " " + tz);
        Erosion.Grid g = Erosion.createGrid(p, tx, tz, k::upliftHeight);
        System.out.printf("base      min %.1f max %.1f%n", min(g.base), max(g.base));
        System.out.printf("height    min %.1f max %.1f%n", min(g.height), max(g.height));
        // climate + softness like the tile does, then passes
        RegionTile t = new RegionTile(k, tx, tz);
        Erosion.Grid gg = t.grid;
        System.out.printf("final     min %.1f max %.1f%n", min(gg.height), max(gg.height));
        double amin = Double.POSITIVE_INFINITY, amax = 0, chmax = 0, slmax = 0;
        int nCh = 0;
        for (int i = 0; i < gg.acc.length; i++) {
            amin = Math.min(amin, gg.acc[i]);
            amax = Math.max(amax, gg.acc[i]);
            chmax = Math.max(chmax, gg.channel[i]);
            slmax = Math.max(slmax, gg.slope[i]);
            if (gg.channel[i] > 0.15) {
                nCh++;
            }
        }
        System.out.printf("acc min %.1f max %.1f ; channel max %.2f cells>0.15 %d ; slope max %.1f%n",
                amin, amax, chmax, nCh, slmax);
        double[] w = gg.waterLevel;
        double wmin = Double.POSITIVE_INFINITY, wmax = Double.NEGATIVE_INFINITY;
        for (double v : w) {
            if (v > Erosion.Grid.NO_WATER / 2) {
                wmin = Math.min(wmin, v);
                wmax = Math.max(wmax, v);
            }
        }
        System.out.printf("waterLvl  min %.1f max %.1f%n", wmin, wmax);
    }

    static double min(double[] a) {
        double m = Double.POSITIVE_INFINITY;
        for (double v : a) {
            m = Math.min(m, v);
        }
        return m;
    }

    static double max(double[] a) {
        double m = Double.NEGATIVE_INFINITY;
        for (double v : a) {
            m = Math.max(m, v);
        }
        return m;
    }
}
