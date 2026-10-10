package dev.terrarealis.tools;

import dev.terrarealis.kernel.Column;
import dev.terrarealis.kernel.Material;
import dev.terrarealis.kernel.Stratigrapher;
import dev.terrarealis.kernel.TerraKernel;
import dev.terrarealis.kernel.biome.BiomeKind;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * One pass over a rectangular region of the world, collecting everything the preview images need.
 *
 * <p>All the layers are rendered from a single field build, because building the field is the expensive
 * part: it has to simulate every erosion tile the region touches. Rendering nine PNGs from one field
 * costs about the same as rendering one.
 */
public final class Field {

    public final int w;
    public final int h;
    public final int step;
    public final int x0;
    public final int z0;

    public final double[] surfaceY;
    public final double[] waterY;
    public final double[] elevM;
    public final double[] tempC;
    public final double[] precipMm;
    public final double[] aridity;
    public final double[] slope;
    public final double[] channel;
    public final double[] acc;
    public final double[] dune;
    public final double[] fan;
    public final double[] playa;
    public final double[] lava;
    public final double[] karst;
    public final double[] glacier;
    public final double[] painted;
    public final double[] riparian;
    public final double[] snowLineM;
    public final double[] latitude;
    public final double[] deposition;
    public final double[] saturation;
    public final double[] continentality;
    public final double[] oceanDistance;
    public final double[] volcano;
    public final double[] outcrop;
    public final double[] soilThickness;

    public final Material[] top;
    public final Material[] filler;
    public final BiomeKind[] biome;
    public final dev.terrarealis.kernel.Rock[] rock;

    public long columnCalls;
    public long buildMillis;

    private Field(int w, int h, int step, int x0, int z0) {
        this.w = w;
        this.h = h;
        this.step = step;
        this.x0 = x0;
        this.z0 = z0;
        int n = w * h;
        surfaceY = new double[n];
        waterY = new double[n];
        elevM = new double[n];
        tempC = new double[n];
        precipMm = new double[n];
        aridity = new double[n];
        slope = new double[n];
        channel = new double[n];
        acc = new double[n];
        dune = new double[n];
        fan = new double[n];
        playa = new double[n];
        lava = new double[n];
        karst = new double[n];
        glacier = new double[n];
        painted = new double[n];
        riparian = new double[n];
        snowLineM = new double[n];
        latitude = new double[n];
        deposition = new double[n];
        saturation = new double[n];
        continentality = new double[n];
        oceanDistance = new double[n];
        volcano = new double[n];
        outcrop = new double[n];
        soilThickness = new double[n];
        top = new Material[n];
        filler = new Material[n];
        biome = new BiomeKind[n];
        rock = new dev.terrarealis.kernel.Rock[n];
    }

    public double metersPerPixel() {
        return step * 12.0;
    }

    public static Field build(TerraKernel k, int x0, int z0, int w, int h, int step, int threads) {
        Field f = new Field(w, h, step, x0, z0);
        long t0 = System.nanoTime();
        int n = Math.max(1, Math.min(threads, Runtime.getRuntime().availableProcessors()));
        ExecutorService pool = Executors.newFixedThreadPool(n);
        for (int t = 0; t < n; t++) {
            final int tid = t;
            pool.submit(() -> {
                Column col = new Column();
                for (int pz = tid; pz < h; pz += n) {
                    for (int px = 0; px < w; px++) {
                        int bx = x0 + px * step;
                        int bz = z0 + pz * step;
                        k.column(bx, bz, col);
                        Stratigrapher.decideSurface(col, k.params());
                        int i = pz * w + px;
                        f.surfaceY[i] = col.surfaceY;
                        f.waterY[i] = col.waterY == Column.NO_WATER ? Double.NaN : col.waterY;
                        f.elevM[i] = col.elevationM;
                        f.tempC[i] = col.tempC;
                        f.precipMm[i] = col.precipMm;
                        f.aridity[i] = col.aridity;
                        f.slope[i] = col.slopeDeg;
                        f.channel[i] = col.channel;
                        f.acc[i] = col.acc;
                                        f.dune[i] = col.duneHeight;
                        f.fan[i] = col.fan;
                        f.playa[i] = col.playa;
                        f.lava[i] = col.lavaFlow;
                        f.karst[i] = col.karst;
                        f.glacier[i] = col.glacier;
                        f.painted[i] = col.painted;
                        f.riparian[i] = col.riparian;
                        f.snowLineM[i] = col.snowLineM;
                        f.latitude[i] = col.latitude;
                        f.deposition[i] = col.deposition;
                        f.saturation[i] = col.saturation;
                        f.continentality[i] = col.continentalness;
                        f.oceanDistance[i] = col.oceanDistance;
                        f.volcano[i] = col.volcanoFresh;
                        f.outcrop[i] = col.outcrop;
                        f.soilThickness[i] = col.soilThickness;
                        f.top[i] = col.surface;
                        f.filler[i] = col.filler;
                        f.biome[i] = col.biome;
                        f.rock[i] = col.rock;
                    }
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        f.buildMillis = (System.nanoTime() - t0) / 1_000_000L;
        f.columnCalls = (long) w * h;
        return f;
    }
}
