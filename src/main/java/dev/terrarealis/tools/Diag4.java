package dev.terrarealis.tools;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.TerraKernel;
import dev.terrarealis.kernel.region.RegionTile;

/** Compares the same world block sampled from the tile that owns it (interior) and from a neighbour
 *  (halo). Large differences mean a pass is sensitive to tile boundaries. */
public final class Diag4 {
    public static void main(String[] a) {
        GenParams p = GenParams.defaults();
        p.sanitise();
        TerraKernel k = new TerraKernel(0x5EED1234L, p);
        int tb = k.tileBlocks();
        RegionTile own = k.tileFor(2, 2);
        RegionTile nb = k.tileFor(3, 2);
        RegionTile.TileSample s0 = new RegionTile.TileSample();
        RegionTile.TileSample s1 = new RegionTile.TileSample();
        double hMax = 0, slMax = 0, chMax = 0, plMax = 0, wlMax = 0, odMax = 0, saMax = 0;
        for (int bz = 0; bz < tb; bz += 2) {
            for (int bx = 0; bx < 24; bx += 2) {
                int wx = 2 * tb + bx;
                int wz = 2 * tb + bz;
                own.sample(wx, wz, s0);
                nb.sample(wx, wz, s1);
                hMax = Math.max(hMax, Math.abs(s0.height - s1.height));
                slMax = Math.max(slMax, Math.abs(s0.slope - s1.slope));
                chMax = Math.max(chMax, Math.abs(s0.channel - s1.channel));
                plMax = Math.max(plMax, Math.abs(s0.playa - s1.playa));
                saMax = Math.max(saMax, Math.abs(s0.sandSupply - s1.sandSupply));
                odMax = Math.max(odMax, Math.abs(s0.oceanDistance - s1.oceanDistance));
                if (s0.waterLevel != s1.waterLevel) {
                    wlMax = Math.max(wlMax, Math.abs(s0.waterLevel - s1.waterLevel));
                }
            }
        }
        System.out.printf("interior-vs-halo max |d|: height %.3f slope %.2f channel %.2f playa %.2f sand %.2f oceanDist %.1f waterLevel %.2f%n",
                hMax, slMax, chMax, plMax, saMax, odMax, wlMax);
    }
}
