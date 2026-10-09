package dev.terrarealis.tools;

import dev.terrarealis.kernel.Column;
import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.Material;
import dev.terrarealis.kernel.Stratigrapher;
import dev.terrarealis.kernel.TerraKernel;
import dev.terrarealis.kernel.biome.BiomeKind;
import dev.terrarealis.kernel.geo.Erosion;
import dev.terrarealis.kernel.region.RegionTile;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * The kernel test-suite.
 *
 * <p>A plain {@code main}, no JUnit, because the point of the kernel is that it builds and runs with
 * nothing but a JDK. Every assertion is a property the generator must hold for <i>any</i> seed, so a
 * pass here means the algorithms are sound rather than that one screenshot looked nice.
 *
 * <p>Run with {@code tools/build-offline.sh test}.
 */
public final class KernelTests {

    private static int failures;
    private static int checks;

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.decode(args[0]) : 0x5EED1234L;
        GenParams p = GenParams.defaults();
        p.sanitise();
        TerraKernel k = new TerraKernel(seed, p);

        determinism(k, p);
        seamContinuity(k, p);
        hypsometry(k, p);
        drainage(k, p);
        aridLandforms(k, p);
        columnIntegrity(k, p);
        caves(k, p);
        biomes(k, p);
        performance(k, p);

        System.out.printf(Locale.ROOT, "%n%d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            System.exit(1);
        }
        System.out.println("KERNEL OK");
    }

    // ------------------------------------------------------------------ checks

    private static void determinism(TerraKernel k, GenParams p) {
        Column a = new Column();
        Column b = new Column();
        boolean same = true;
        for (int i = 0; i < 4000; i++) {
            int x = -12000 + i * 7;
            int z = 4000 - i * 5;
            k.column(x, z, a);
            k.column(x, z, b);
            if (a.surfaceY != b.surfaceY || a.biome != b.biome || a.surface != b.surface) {
                same = false;
                break;
            }
        }
        check("determinism: same seed, same column", same);

        TerraKernel other = new TerraKernel(seed(k) ^ 0xDEADBEEFL, p);
        Column c = new Column();
        int differing = 0;
        for (int i = 0; i < 2000; i++) {
            int x = (i * 11) % 900;
            int z = (i * 13) % 900;
            k.column(x, z, a);
            other.column(x, z, c);
            if (a.surfaceY != c.surfaceY) {
                differing++;
            }
        }
        check("determinism: different seed, different world", differing > 1500);
    }

    private static long seed(TerraKernel k) {
        return k.seed();
    }

    private static void seamContinuity(TerraKernel k, GenParams p) {
        // Two tiles that share an edge must agree on the height along it, to within the interpolation
        // error of the halo. This is the property that makes a tiled erosion model usable at all.
        int tileBlocks = k.tileBlocks();
        RegionTile t0 = k.tileFor(0, 0);
        RegionTile t1 = k.tileFor(1, 0);
        RegionTile.TileSample s0 = new RegionTile.TileSample();
        RegionTile.TileSample s1 = new RegionTile.TileSample();
        // The last interior column of tile 0 (block tileBlocks-1) against the first interior column
        // of tile 1 (block tileBlocks): adjacent ground, simulated by two different tiles.
        double[] diff = new double[tileBlocks];
        for (int i = 0; i < tileBlocks; i++) {
            t0.sample(tileBlocks - 1, i, s0);
            t1.sample(tileBlocks, i, s1);
            diff[i] = Math.abs(s0.height - s1.height);
        }
        double[] sorted = diff.clone();
        java.util.Arrays.sort(sorted);
        double mean = 0;
        for (double d : diff) {
            mean += d;
        }
        mean /= diff.length;
        double p99 = sorted[(int) (diff.length * 0.99)];
        double worst = sorted[diff.length - 1];
        check(String.format(Locale.ROOT,
                "seam continuity: mean |dh| %.2f blocks (< 1.5), p99 %.2f (< 6), max %.2f",
                mean, p99, worst), mean < 1.5 && p99 < 6.0);
    }

    private static void hypsometry(TerraKernel k, GenParams p) {
        int n = 0;
        int ocean = 0;
        int land = 0;
        int high = 0;
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int z = -60000; z <= 60000; z += 750) {
            for (int x = -60000; x <= 60000; x += 750) {
                double h = k.upliftHeight(x, z);
                min = Math.min(min, h);
                max = Math.max(max, h);
                n++;
                if (h < p.seaLevel) {
                    ocean++;
                } else {
                    land++;
                    double em = (h - p.seaLevel) * p.metersPerBlock;
                    sum += em;
                    if (em > 1500) {
                        high++;
                    }
                }
            }
        }
        double oceanFraction = ocean / (double) n;
        double meanLand = sum / land;
        check("hypsometry: uplift within build limits ("
                + (int) min + ".." + (int) max + " in " + p.minY + ".." + p.maxY + ")",
                min >= p.minY && max <= p.maxY);
        check("hypsometry: ocean fraction " + String.format(Locale.ROOT, "%.3f", oceanFraction)
                + " within 0.08 of target " + p.oceanFraction,
                Math.abs(oceanFraction - p.oceanFraction) < 0.08);
        check("hypsometry: mean land elevation " + (int) meanLand
                + " m is Earth-like (200..1400)", meanLand > 200 && meanLand < 1400);
        check("hypsometry: mountains above 1500 m exist ("
                + String.format(Locale.ROOT, "%.1f", 100.0 * high / n) + "%)", high > n * 0.005);
    }

    private static void drainage(TerraKernel k, GenParams p) {
        // In a humid region the hydrology must produce a connected channel network with water in it.
        RegionTile t = k.tileFor(findHumidTile(k), 3);
        Erosion.Grid g = t.grid;
        int channels = 0;
        int water = 0;
        double maxAcc = 0;
        for (int i = 0; i < g.acc.length; i++) {
            maxAcc = Math.max(maxAcc, g.acc[i]);
            if (g.channel[i] > 0.3) {
                channels++;
            }
            if (g.waterLevel[i] != Erosion.Grid.NO_WATER) {
                water++;
            }
        }
        double frac = channels / (double) g.acc.length;
        check("drainage: humid tile has a channel network ("
                + String.format(Locale.ROOT, "%.2f", frac * 100) + "% of cells)",
                frac > 0.004 && frac < 0.30);
        check("drainage: accumulation concentrates (max acc " + (int) maxAcc + " > 800 cells)",
                maxAcc > 800);
        check("drainage: perennial water exists in a humid tile (" + water + " cells)", water > 20);
    }

    private static int findHumidTile(TerraKernel k) {
        for (int tx = -40; tx < 40; tx++) {
            int x = tx * k.tileBlocks() + 64;
            double cont = k.continentalness(x, 200);
            double arid = k.continents().aridityProxyAt(x, 200, cont);
            if (cont > 0.1 && arid < 0.15) {
                return tx;
            }
        }
        return 2;
    }

    private static void aridLandforms(TerraKernel k, GenParams p) {
        // Find an arid tile and require that the desert systems actually fire.
        int tx = -1;
        int tz = -1;
        outer:
        for (int z = -220; z < 220; z += 4) {
            for (int x = -220; x < 220; x += 4) {
                int bx = x * k.tileBlocks() + 64;
                int bz = z * k.tileBlocks() + 64;
                double cont = k.continentalness(bx, bz);
                double arid = k.continents().aridityProxyAt(bx, bz, cont);
                double sum = 0;
                for (int d = 0; d < 8; d++) {
                    double a = d * Math.PI / 4.0;
                    double dh = k.upliftHeight((int) (bx + Math.cos(a) * 620),
                            (int) (bz + Math.sin(a) * 620)) - k.upliftHeight(bx, bz);
                    sum += dh * dh;
                }
                double relief = Math.sqrt(sum / 8.0);
                if (cont > 0.10 && arid > 0.55 && relief > 8.0) {
                    tx = x;
                    tz = z;
                    break outer;
                }
            }
        }
        check("arid: located a desert basin-and-range tile", tx >= 0);
        if (tx < 0) {
            return;
        }
        RegionTile t = k.tileFor(tx, tz);
        Erosion.Grid g = t.grid;
        Column col = new Column();
        int painted = 0;
        int fans = 0;
        int dunes = 0;
        int playas = 0;
        int badlandSlope = 0;
        for (int i = 0; i < 400; i++) {
            int bx = tx * k.tileBlocks() + (i % 20) * 6;
            int bz = tz * k.tileBlocks() + (i / 20) * 6;
            k.column(bx, bz, col);
            Stratigrapher.decideSurface(col, p);
            if (col.painted > 0.3) {
                painted++;
            }
            if (col.fan > 0.4) {
                fans++;
            }
            if (col.duneHeight > 0.8) {
                dunes++;
            }
            if (col.playa > 0.35) {
                playas++;
            }
            if (col.slopeDeg > 25) {
                badlandSlope++;
            }
        }
        int any = painted + fans + dunes + playas;
        check("arid: desert landforms present (painted " + painted + ", fans " + fans + ", dunes "
                + dunes + ", playas " + playas + ")", any > 4);
        check("arid: dissected relief present (steep cells " + badlandSlope + ")", badlandSlope > 0);
        check("arid: tile heights finite",
                Double.isFinite(g.height[0]) && Double.isFinite(g.height[g.height.length - 1]));
    }

    private static void columnIntegrity(TerraKernel k, GenParams p) {
        Material[] stack = new Material[p.maxY - p.minY + 1];
        Column col = new Column();
        boolean ok = true;
        boolean waterOk = true;
        boolean bedrockOk = true;
        for (int i = 0; i < 600; i++) {
            int x = -2000 + (i * 67) % 4000;
            int z = 1000 - (i * 41) % 4000;
            k.column(x, z, col);
            Stratigrapher.fill(k, col, x, z, stack, Stratigrapher.NO_CAVES);
            if (col.surfaceY < p.minY || col.surfaceY > p.maxY) {
                ok = false;
            }
            Material top = stack[col.surfaceY - p.minY];
            if (top == Material.AIR || top == Material.WATER) {
                ok = false;
            }
            for (int b = 0; b < 3; b++) {
                if (stack[b] != Material.BEDROCK) {
                    bedrockOk = false;
                }
            }
            boolean seenWater = false;
            for (int y = col.surfaceY + 1; y <= p.maxY; y++) {
                Material m = stack[y - p.minY];
                if (m == Material.WATER) {
                    seenWater = true;
                } else if (seenWater && m != Material.ICE && m != Material.PACKED_ICE
                        && m != Material.SNOW_LAYER && m != Material.AIR) {
                    waterOk = false;
                }
            }
            if (seenWater != (col.waterY != Column.NO_WATER)) {
                waterOk = false;
            }
        }
        check("column: surface is solid ground and within build limits", ok);
        check("column: bedrock floor present", bedrockOk);
        check("column: water sits above the surface and only where declared", waterOk);
    }

    private static void caves(TerraKernel k, GenParams p) {
        Column col = new Column();
        TerraKernel.DirectCaves caves = k.new DirectCaves();
        int voids = 0;
        int total = 0;
        for (int i = 0; i < 120; i++) {
            int x = -1500 + (i * 151) % 3000;
            int z = 800 - (i * 97) % 3000;
            k.column(x, z, col);
            for (int y = col.surfaceY - 8; y > p.minY + 8; y -= 3) {
                total++;
                if (caves.voidAt(x, y, z, col)) {
                    voids++;
                }
            }
        }
        double frac = voids / (double) total;
        check("caves: void fraction " + String.format(Locale.ROOT, "%.3f", frac)
                + " within 0.002..0.15", frac > 0.002 && frac < 0.15);
    }

    private static void biomes(TerraKernel k, GenParams p) {
        // Six contiguous windows in different latitude bands: contiguous so that the tile cache is
        // warm, spread so that the biome spectrum is real rather than one climate zone.
        int[] ocean = {-6000, -6000};
        scan:
        for (int z = -60000; z <= 60000; z += 2000) {
            for (int x = -60000; x <= 60000; x += 2000) {
                if (k.continentalness(x, z) < -0.30) {
                    ocean = new int[] {x, z};
                    break scan;
                }
            }
        }
        int[][] windows = {
                {-3000, -3000}, {2000, -1500}, {-1000, 3000},
                {4000, 2500}, {-4500, 6000}, {3000, -5000}, ocean,
        };
        Column col = new Column();
        Set<BiomeKind> seen = EnumSet.noneOf(BiomeKind.class);
        boolean desertSeen = false;
        boolean oceanSeen = false;
        boolean alpineSeen = false;
        int samples = 0;
        for (int[] wnd : windows) {
            for (int a = 0; a < 40; a++) {
                for (int b = 0; b < 40; b++) {
                    int x = wnd[0] + a * 50;
                    int z = wnd[1] + b * 50;
                    k.column(x, z, col);
                    samples++;
                    seen.add(col.biome);
                    switch (col.biome.zone) {
                        case ABYSSAL, SHELF -> oceanSeen = true;
                        case ALPINE, MONTANE -> alpineSeen = true;
                        default -> { }
                    }
                    if (col.biome == BiomeKind.DESERT || col.biome == BiomeKind.COASTAL_DESERT
                            || col.biome == BiomeKind.BADLANDS || col.biome == BiomeKind.ERODED_BADLANDS) {
                        desertSeen = true;
                    }
                }
            }
        }
        check("biomes: " + seen.size() + " distinct biomes over " + samples
                + " samples in 6 climate windows (> 14)", seen.size() > 14);
        check("biomes: ocean biomes present", oceanSeen);
        check("biomes: montane/alpine biomes present", alpineSeen);
        check("biomes: desert biomes present", desertSeen);
    }

    private static void performance(TerraKernel k, GenParams p) {
        Column col = new Column();
        for (int i = 0; i < 4000; i++) {
            k.column(i % 128, (i * 3) % 128, col);
        }
        long t0 = System.nanoTime();
        int n = 20000;
        for (int i = 0; i < n; i++) {
            k.column((i * 3) % 128, (i * 5) % 128, col);
        }
        double us = (System.nanoTime() - t0) / 1000.0 / n;
        check("performance: column " + String.format(Locale.ROOT, "%.2f", us)
                + " us (< 12 us, i.e. < 3 ms/chunk)", us < 12.0);

        long t1 = System.nanoTime();
        int tiles = 12;
        for (int i = 0; i < tiles; i++) {
            new RegionTile(k, 200 + i * 5, 90 + i * 3);
        }
        double ms = (System.nanoTime() - t1) / 1e6 / tiles;
        int chunksPerTile = (p.tileCells * p.erosionCellSize / 16)
                * (p.tileCells * p.erosionCellSize / 16);
        check("performance: tile " + String.format(Locale.ROOT, "%.1f", ms) + " ms for "
                + chunksPerTile + " chunks (< 6 ms/chunk)", ms / chunksPerTile < 6.0);
    }

    // ------------------------------------------------------------------ helper

    private static void check(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf(Locale.ROOT, "  [%s] %s%n", ok ? "PASS" : "FAIL", what);
    }
}
