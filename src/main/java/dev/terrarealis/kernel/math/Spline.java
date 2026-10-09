package dev.terrarealis.kernel.math;

/**
 * Monotone piecewise-cubic spline (Fritsch–Carlson).
 *
 * <p>Vanilla's {@code Spline} uses linear interpolation between fixed points plus hand authored
 * derivatives. Monotone cubic gets us two things that matter a lot for realism:
 * <ul>
 *   <li><b>C1 continuity</b> — no kinks at the control points, so hillshaded terrain does not show
 *       terraces where the hypsometric curve changes slope;</li>
 *   <li><b>monotonicity</b> — no overshoot, so an elevation curve that should never go backwards
 *       cannot produce floating islands or inverted coastlines.</li>
 * </ul>
 */
public final class Spline {

    private final double[] xs;
    private final double[] ys;
    private final double[] ms; // tangents

    private Spline(double[] xs, double[] ys, double[] ms) {
        this.xs = xs;
        this.ys = ys;
        this.ms = ms;
    }

    /**
     * @param knots alternating {@code x0, y0, x1, y1, ...}; x must be strictly increasing.
     */
    public static Spline of(double... knots) {
        if (knots.length < 4 || (knots.length & 1) != 0) {
            throw new IllegalArgumentException("need an even number of >= 4 knot values");
        }
        int n = knots.length / 2;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = knots[i * 2];
            ys[i] = knots[i * 2 + 1];
            if (i > 0 && xs[i] <= xs[i - 1]) {
                throw new IllegalArgumentException("knot x values must increase: " + xs[i - 1] + " -> " + xs[i]);
            }
        }
        return new Spline(xs, ys, tangents(xs, ys));
    }

    public static Spline of(double[] xs, double[] ys) {
        if (xs.length != ys.length || xs.length < 2) {
            throw new IllegalArgumentException("bad spline arrays");
        }
        return new Spline(xs.clone(), ys.clone(), tangents(xs, ys));
    }

    /** Fritsch–Carlson tangents: guarantees no overshoot between knots. */
    private static double[] tangents(double[] xs, double[] ys) {
        int n = xs.length;
        double[] d = new double[n - 1];
        double[] h = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            h[i] = xs[i + 1] - xs[i];
            d[i] = (ys[i + 1] - ys[i]) / h[i];
        }
        double[] m = new double[n];
        m[0] = d[0];
        m[n - 1] = d[n - 2];
        for (int i = 1; i < n - 1; i++) {
            if (d[i - 1] * d[i] <= 0) {
                m[i] = 0; // local extremum: flat tangent keeps monotonicity
            } else {
                // Harmonic mean weighted by segment lengths.
                double w1 = 2 * h[i] + h[i - 1];
                double w2 = h[i] + 2 * h[i - 1];
                m[i] = (w1 + w2) / (w1 / d[i - 1] + w2 / d[i]);
            }
        }
        // Final Fritsch–Carlson correction against residual overshoot.
        for (int i = 0; i < n - 1; i++) {
            if (d[i] == 0) {
                m[i] = 0;
                m[i + 1] = 0;
            } else {
                double a = m[i] / d[i];
                double b = m[i + 1] / d[i];
                double s = a * a + b * b;
                if (s > 9) {
                    double t = 3 / Math.sqrt(s);
                    m[i] = t * a * d[i];
                    m[i + 1] = t * b * d[i];
                }
            }
        }
        return m;
    }

    public double eval(double x) {
        int n = xs.length;
        if (x <= xs[0]) {
            return ys[0];
        }
        if (x >= xs[n - 1]) {
            return ys[n - 1];
        }
        int i = search(x);
        double h = xs[i + 1] - xs[i];
        double t = (x - xs[i]) / h;
        double t2 = t * t;
        double t3 = t2 * t;
        double h00 = 2 * t3 - 3 * t2 + 1;
        double h10 = t3 - 2 * t2 + t;
        double h01 = -2 * t3 + 3 * t2;
        double h11 = t3 - t2;
        return h00 * ys[i] + h10 * h * ms[i] + h01 * ys[i + 1] + h11 * h * ms[i + 1];
    }

    /** d(eval)/dx — used to modulate slope-sensitive effects (cliffs, scree). */
    public double derivative(double x) {
        int n = xs.length;
        if (x <= xs[0] || x >= xs[n - 1]) {
            return 0;
        }
        int i = search(x);
        double h = xs[i + 1] - xs[i];
        double t = (x - xs[i]) / h;
        double t2 = t * t;
        double d00 = 6 * t2 - 6 * t;
        double d10 = 3 * t2 - 4 * t + 1;
        double d01 = -6 * t2 + 6 * t;
        double d11 = 3 * t2 - 2 * t;
        return (d00 * ys[i] + d01 * ys[i + 1]) / h + d10 * ms[i] + d11 * ms[i + 1];
    }

    private int search(double x) {
        int lo = 0;
        int hi = xs.length - 2;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (xs[mid] <= x) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    public double minX() {
        return xs[0];
    }

    public double maxX() {
        return xs[xs.length - 1];
    }
}
