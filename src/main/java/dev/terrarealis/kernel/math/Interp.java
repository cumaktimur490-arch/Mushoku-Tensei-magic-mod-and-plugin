package dev.terrarealis.kernel.math;

/** Scalar helpers used everywhere in the kernel. */
public final class Interp {

    private Interp() {}

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /** Ken Perlin's improved fade curve: zero 1st and 2nd derivative at the knots. */
    public static double quintic(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    public static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** Normalised inverse: {@code remap(v, a, b)} is 0 at a and 1 at b, clamped. */
    public static double remap(double v, double a, double b) {
        if (b == a) {
            return 0.0;
        }
        return clamp((v - a) / (b - a), 0.0, 1.0);
    }

    public static double inverseLerpUnclamped(double v, double a, double b) {
        return (b == a) ? 0.0 : (v - a) / (b - a);
    }

    /** Bilinear interpolation of a unit cell from corner values. */
    public static double bilerp(double fx, double fz, double v00, double v10, double v01, double v11) {
        double a = lerp(fx, v00, v10);
        double b = lerp(fx, v01, v11);
        return lerp(fz, a, b);
    }

    /** Trilinear interpolation used to up-sample coarse erosion grids back to block resolution. */
    public static double trilerp(double fx, double fy, double fz,
                                 double v000, double v100, double v010, double v110,
                                 double v001, double v101, double v011, double v111) {
        double x00 = lerp(fx, v000, v100);
        double x10 = lerp(fx, v010, v110);
        double x01 = lerp(fx, v001, v101);
        double x11 = lerp(fx, v011, v111);
        return lerp(fz, lerp(fy, x00, x10), lerp(fy, x01, x11));
    }

    /** Signed angle-preserving wrap of a direction into [0, TAU). */
    public static double wrapAngle(double radians) {
        double tau = Math.PI * 2.0;
        double r = radians % tau;
        return r < 0 ? r + tau : r;
    }
}
