package dev.terrarealis.tools;

import dev.terrarealis.kernel.Column;
import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.TerraKernel;

public final class Diag5 {
    public static void main(String[] a) {
        GenParams p = GenParams.defaults();
        p.sanitise();
        TerraKernel k = new TerraKernel(0x5EED1234L, p);
        Column c = new Column();
        int x = Integer.parseInt(a[0]);
        for (int z = Integer.parseInt(a[1]); z <= Integer.parseInt(a[2]); z += 8) {
            k.column(x, z, c);
            System.out.printf("z=%6d tileZ=%4d surfY=%7d waterY=%7d elevM=%9.1f depth=%6.1f biome=%s%n",
                    z, Math.floorDiv(z, 128), c.surfaceY, 
                    c.waterY == Column.NO_WATER ? -9999 : c.waterY,
                    c.elevationM, c.waterDepth, c.biome.name());
        }
    }
}
