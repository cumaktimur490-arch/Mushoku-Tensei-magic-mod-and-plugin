package dev.terrarealis.kernel.math;

/**
 * Stateless coherent-noise library written for this generator.
 *
 * <p>Why not reuse Minecraft's {@code ImprovedNoise}? Three reasons:
 * <ol>
 *   <li>the kernel has to run headless (unit tests, map previews, the {@code /terra} exporter) with
 *       no Minecraft on the classpath;</li>
 *   <li>vanilla noise is driven by a permutation table sized for one octave set — realistic terrain
 *       needs independently seeded octaves so that "continent" and "boulder" detail never alias
 *       onto each other;</li>
 *   <li>we need Worley/cellular noise (plate cells, forest clumping, karst pits) which vanilla
 *       simply does not have.</li>
 * </ol>
 *
 * <p>Instances are immutable and therefore safe to share between worldgen threads.
 */
public final class Noise {

    // 2-D gradient set: 4 axis aligned + 4 diagonals.
    private static final double[] GX2 = {1, -1, 0, 0, 1, -1, 1, -1};
    private static final double[] GZ2 = {0, 0, 1, -1, 1, 1, -1, -1};

    // 3-D gradient set: the 12 edge midpoints of a cube (Perlin's classic choice).
    private static final int[][] G3 = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
    };

    /** Empirical peak magnitude of 3-D Perlin noise with the gradient set above; used to normalise. */
    private static final double PERLIN3_NORM = 1.0 / 0.866;
    private static final double PERLIN2_NORM = 1.0 / 0.7071;

    private final long seed;

    public Noise(long seed) {
        this.seed = Hash.mix64(seed);
    }

    public Noise withSalt(long salt) {
        return new Noise(seed ^ (salt * Hash.GOLDEN));
    }

    public long seed() {
        return seed;
    }

    // ---------------------------------------------------------------- Perlin

    public double perlin2(double x, double z) {
        int xi = fastFloor(x);
        int zi = fastFloor(z);
        double fx = x - xi;
        double fz = z - zi;
        double u = Interp.quintic(fx);
        double v = Interp.quintic(fz);

        double n00 = grad2(xi, zi, fx, fz);
        double n10 = grad2(xi + 1, zi, fx - 1, fz);
        double n01 = grad2(xi, zi + 1, fx, fz - 1);
        double n11 = grad2(xi + 1, zi + 1, fx - 1, fz - 1);

        double nx0 = Interp.lerp(u, n00, n10);
        double nx1 = Interp.lerp(u, n01, n11);
        return Interp.lerp(v, nx0, nx1) * PERLIN2_NORM;
    }

    private double grad2(int cx, int cz, double dx, double dz) {
        int h = (int) (Hash.hash(seed, cx, cz) & 7);
        return GX2[h] * dx + GZ2[h] * dz;
    }

    public double perlin3(double x, double y, double z) {
        int xi = fastFloor(x);
        int yi = fastFloor(y);
        int zi = fastFloor(z);
        double fx = x - xi;
        double fy = y - yi;
        double fz = z - zi;
        double u = Interp.quintic(fx);
        double v = Interp.quintic(fy);
        double w = Interp.quintic(fz);

        double n000 = grad3(xi, yi, zi, fx, fy, fz);
        double n100 = grad3(xi + 1, yi, zi, fx - 1, fy, fz);
        double n010 = grad3(xi, yi + 1, zi, fx, fy - 1, fz);
        double n110 = grad3(xi + 1, yi + 1, zi, fx - 1, fy - 1, fz);
        double n001 = grad3(xi, yi, zi + 1, fx, fy, fz - 1);
        double n101 = grad3(xi + 1, yi, zi + 1, fx - 1, fy, fz - 1);
        double n011 = grad3(xi, yi + 1, zi + 1, fx, fy - 1, fz - 1);
        double n111 = grad3(xi + 1, yi + 1, zi + 1, fx - 1, fy - 1, fz - 1);

        double x00 = Interp.lerp(u, n000, n100);
        double x10 = Interp.lerp(u, n010, n110);
        double x01 = Interp.lerp(u, n001, n101);
        double x11 = Interp.lerp(u, n011, n111);
        return Interp.lerp(w, Interp.lerp(v, x00, x10), Interp.lerp(v, x01, x11)) * PERLIN3_NORM;
    }

    private double grad3(int cx, int cy, int cz, double dx, double dy, double dz) {
        int[] g = G3[(int) (Hash.hash(seed, cx, cy, cz) & 15) % 12];
        return g[0] * dx + g[1] * dy + g[2] * dz;
    }

    // --------------------------------------------------------------- fractal

    /** Fractional Brownian motion. Returns roughly [-1, 1]. */
    public double fbm2(double x, double z, int octaves, double lacunarity, double gain) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double fx = x;
        double fz = z;
        for (int i = 0; i < octaves; i++) {
            sum += perlin2(fx, fz) * amp;
            norm += amp;
            fx *= lacunarity;
            fz *= lacunarity;
            amp *= gain;
        }
        return norm == 0 ? 0 : sum / norm;
    }

    public double fbm3(double x, double y, double z, int octaves, double lacunarity, double gain) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double fx = x;
        double fy = y;
        double fz = z;
        for (int i = 0; i < octaves; i++) {
            sum += perlin3(fx, fy, fz) * amp;
            norm += amp;
            fx *= lacunarity;
            fy *= lacunarity;
            fz *= lacunarity;
            amp *= gain;
        }
        return norm == 0 ? 0 : sum / norm;
    }

    /**
     * Ridged multifractal: {@code 1 - |n|}, squared, so ridges stay sharp and valleys flatten.
     * This is the single most important noise for mountain belts — real orogens are ridges and
     * intermontane basins, not gaussian blobs.
     */
    public double ridged2(double x, double z, int octaves, double lacunarity, double gain) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double weight = 1;
        double fx = x;
        double fz = z;
        for (int i = 0; i < octaves; i++) {
            double n = 1.0 - Math.abs(perlin2(fx, fz));
            n *= n;
            // Weight successive octaves by the previous one: keeps ridges coherent instead of dusty.
            n *= weight;
            weight = Interp.clamp(n * 2.0, 0.0, 1.0);
            sum += n * amp;
            norm += amp;
            fx *= lacunarity;
            fz *= lacunarity;
            amp *= gain;
        }
        return norm == 0 ? 0 : (sum / norm) * 2.0 - 1.0;
    }

    /** Billow noise — useful for cloud-like and dune-like fields. */
    public double billow2(double x, double z, int octaves, double lacunarity, double gain) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double fx = x;
        double fz = z;
        for (int i = 0; i < octaves; i++) {
            sum += (Math.abs(perlin2(fx, fz)) * 2.0 - 1.0) * amp;
            norm += amp;
            fx *= lacunarity;
            fz *= lacunarity;
            amp *= gain;
        }
        return norm == 0 ? 0 : sum / norm;
    }

    // --------------------------------------------------------------- cellular

    /** Result of a 2-D Worley probe: the two nearest feature cells and their distances. */
    public static final class Worley {
        public final double f1;
        public final double f2;
        /** Coordinates of the nearest cell (the one that "owns" this point). */
        public final int cellX;
        public final int cellZ;
        /** Coordinates of the second nearest cell — the neighbour across the boundary. */
        public final int cell2X;
        public final int cell2Z;

        Worley(double f1, double f2, int cellX, int cellZ, int cell2X, int cell2Z) {
            this.f1 = f1;
            this.f2 = f2;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.cell2X = cell2X;
            this.cell2Z = cell2Z;
        }

        /** {@code f2 - f1}: 0 exactly on a cell boundary, large deep inside a cell. */
        public double border() {
            return f2 - f1;
        }
    }

    /**
     * 2-D Worley (cellular) noise. One feature point per cell, jittered by {@code jitter}.
     * Distances are returned in cell units, so {@code f1} is in [0, ~1.5].
     */
    public Worley worley2(double x, double z, double jitter) {
        int xi = fastFloor(x);
        int zi = fastFloor(z);
        double best1 = Double.MAX_VALUE;
        double best2 = Double.MAX_VALUE;
        int bx = xi;
        int bz = zi;
        int b2x = xi;
        int b2z = zi;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = xi + dx;
                int cz = zi + dz;
                long h = Hash.hash(seed, cx, cz);
                double px = cx + (Hash.unit(h) - 0.5) * 2.0 * jitter;
                double pz = cz + (Hash.unit(Hash.mix64(h ^ 0x5DEECE66DL)) - 0.5) * 2.0 * jitter;
                double ddx = px - x;
                double ddz = pz - z;
                double d = ddx * ddx + ddz * ddz;
                if (d < best1) {
                    best2 = best1;
                    b2x = bx;
                    b2z = bz;
                    best1 = d;
                    bx = cx;
                    bz = cz;
                } else if (d < best2) {
                    best2 = d;
                    b2x = cx;
                    b2z = cz;
                }
            }
        }
        return new Worley(Math.sqrt(best1),
                Math.sqrt(best2 == Double.MAX_VALUE ? best1 : best2),
                bx, bz, b2x, b2z);
    }

    // ------------------------------------------------------------ domain warp

    /**
     * Two-pass domain warp. Warping the *coordinates* rather than summing noises is what turns
     * "blobby" continents into something with embayments, peninsulas and curved coastlines.
     */
    public double warpedFbm2(double x, double z, int octaves, double warpStrength,
                             int warpOctaves, double warpFreqScale) {
        double qx = fbm2(x * warpFreqScale + 11.3, z * warpFreqScale + 27.7, warpOctaves, 2.0, 0.5);
        double qz = fbm2(x * warpFreqScale + 91.7, z * warpFreqScale + 3.1, warpOctaves, 2.0, 0.5);
        double wx = x + qx * warpStrength;
        double wz = z + qz * warpStrength;
        return fbm2(wx, wz, octaves, 2.0, 0.5);
    }

    public double warpedRidged2(double x, double z, int octaves, double warpStrength,
                                int warpOctaves, double warpFreqScale) {
        double qx = fbm2(x * warpFreqScale + 41.9, z * warpFreqScale + 63.2, warpOctaves, 2.0, 0.5);
        double qz = fbm2(x * warpFreqScale + 17.4, z * warpFreqScale + 88.8, warpOctaves, 2.0, 0.5);
        return ridged2(x + qx * warpStrength, z + qz * warpStrength, octaves, 2.0, 0.5);
    }

    // ---------------------------------------------------------------- helpers

    public static int fastFloor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }
}
