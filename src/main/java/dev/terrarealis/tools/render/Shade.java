package dev.terrarealis.tools.render;

import dev.terrarealis.kernel.math.Interp;

/** Shaded-relief and hypsometric tinting — the part that makes a height field look like a landscape. */
public final class Shade {

    private Shade() {}

    /**
     * Lambertian hillshade.
     *
     * <p>Two lights: a strong key light from the north-west at 45 degrees, which is the convention for
     * topographic maps because it puts the shadows where a reader expects them, and a weak cool fill from
     * the opposite side so lee slopes do not go completely black.
     *
     * @param cell   ground distance between samples, in the same units as {@code zFactor * height}
     * @return 0..1 per pixel
     */
    public static double[] hillshade(double[] h, int w, int hh, double cell, double zFactor) {
        double[] out = new double[w * hh];
        double azKey = Math.toRadians(315.0);
        double altKey = Math.toRadians(42.0);
        double azFill = Math.toRadians(135.0);
        double altFill = Math.toRadians(24.0);
        double kx = Math.cos(azKey) * Math.cos(altKey);
        double ky = Math.sin(azKey) * Math.cos(altKey);
        double kz = Math.sin(altKey);
        double fx = Math.cos(azFill) * Math.cos(altFill);
        double fy = Math.sin(azFill) * Math.cos(altFill);
        double fz = Math.sin(altFill);
        for (int y = 0; y < hh; y++) {
            int ym = Math.max(0, y - 1);
            int yp = Math.min(hh - 1, y + 1);
            for (int x = 0; x < w; x++) {
                int xm = Math.max(0, x - 1);
                int xp = Math.min(w - 1, x + 1);
                // Horn's method on the 3x3 neighbourhood a b c / d e f / g h i.
                double a = h[ym * w + xm];
                double b = h[ym * w + x];
                double c = h[ym * w + xp];
                double d = h[y * w + xm];
                double f = h[y * w + xp];
                double g = h[yp * w + xm];
                double hh2 = h[yp * w + x];
                double i = h[yp * w + xp];
                double dzdx = ((c + 2 * f + i) - (a + 2 * d + g)) / (8.0 * cell) * zFactor;
                double dzdy = ((g + 2 * hh2 + i) - (a + 2 * b + c)) / (8.0 * cell) * zFactor;
                double len = Math.sqrt(dzdx * dzdx + dzdy * dzdy + 1.0);
                double nx = -dzdx / len;
                double ny = -dzdy / len;
                double nz = 1.0 / len;
                double key = nx * kx + ny * ky + nz * kz;
                double fill = nx * fx + ny * fy + nz * fz;
                out[y * w + x] = Interp.clamp(
                        0.98 * Math.max(0.0, key) + 0.30 * Math.max(0.0, fill) + 0.26 * nz, 0.0, 1.4);
            }
        }
        return out;
    }

    /** Slope in degrees from the same stencil. */
    public static double[] slopeDegrees(double[] h, int w, int hh, double cell, double zFactor) {
        double[] out = new double[w * hh];
        for (int y = 0; y < hh; y++) {
            int ym = Math.max(0, y - 1);
            int yp = Math.min(hh - 1, y + 1);
            for (int x = 0; x < w; x++) {
                int xm = Math.max(0, x - 1);
                int xp = Math.min(w - 1, x + 1);
                double a = h[ym * w + xm];
                double b = h[ym * w + x];
                double c = h[ym * w + xp];
                double d = h[y * w + xm];
                double f = h[y * w + xp];
                double g = h[yp * w + xm];
                double hh2 = h[yp * w + x];
                double i = h[yp * w + xp];
                double dzdx = ((c + 2 * f + i) - (a + 2 * d + g)) / (8.0 * cell);
                double dzdy = ((g + 2 * hh2 + i) - (a + 2 * b + c)) / (8.0 * cell);
                out[y * w + x] = Math.toDegrees(Math.atan(
                        Math.sqrt(dzdx * dzdx + dzdy * dzdy) * zFactor));
            }
        }
        return out;
    }

    /**
     * Hypsometric tint.
     *
     * <p>A hand-built ramp from abyssal blue through shelf turquoise, beach sand, lowland green, olive,
     * ochre, rock grey and snow white. The exact knots matter less than the fact that the ramp is
     * non-linear: it spends most of its range in the 0-800 m band, which is where almost all of the
     * inhabited landscape sits, and compresses the alpine end.
     */
    private static final double[] HYPS_E = {
        -1600, -1100, -600, -220, -60, -8, 0, 4, 18, 60, 140, 260, 420, 620, 850,
        1150, 1500, 1900, 2350, 2900, 3600, 4600, 6000
    };
    private static final int[] HYPS_C = {
        0x0A1E3C, 0x10305C, 0x164A80, 0x1E6A9E, 0x2A8CBE, 0x57B4CE, 0xC8B98A, 0xD9C79A, 0xA8C07A,
        0x7FA85A, 0x6A9A4E, 0x8FA257, 0xB09A5C, 0xA98A62, 0x96795C,
        0x84695A, 0x75605A, 0x8A7C74, 0xA99E96, 0xC4BEB8, 0xD9D6D2, 0xECEDF0, 0xFFFFFF
    };

    public static int hypsometric(double elevationMeters, double aridity) {
        double e = elevationMeters;
        int i = 0;
        while (i < HYPS_E.length - 1 && HYPS_E[i + 1] < e) {
            i++;
        }
        if (i >= HYPS_E.length - 1) {
            return HYPS_C[HYPS_C.length - 1];
        }
        double t = Interp.clamp((e - HYPS_E[i]) / Math.max(1e-6, HYPS_E[i + 1] - HYPS_E[i]), 0.0, 1.0);
        int c0 = HYPS_C[i];
        int c1 = HYPS_C[i + 1];
        int rgb = lerpColor(c0, c1, t);
        if (aridity > 0.35 && e > -20.0) {
            // Dry ground loses its green: shift towards ochre/buff with aridity.
            int dry = lerpColor(0xB99A63, 0xD9BC86, Interp.clamp((aridity - 0.35) / 0.5, 0.0, 1.0));
            rgb = lerpColor(rgb, dry, Interp.smoothstep(0.35, 0.95, aridity) * 0.78);
        }
        return rgb;
    }

    public static int lerpColor(int a, int b, double t) {
        t = Interp.clamp(t, 0.0, 1.0);
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Multiplies a colour by a shade value and applies a gentle filmic curve so highlights roll off. */
    public static int shade(int rgb, double s, double gamma) {
        double r = Math.pow(Interp.clamp(((rgb >> 16) & 0xFF) / 255.0 * s, 0.0, 1.0), gamma);
        double g = Math.pow(Interp.clamp(((rgb >> 8) & 0xFF) / 255.0 * s, 0.0, 1.0), gamma);
        double b = Math.pow(Interp.clamp((rgb & 0xFF) / 255.0 * s, 0.0, 1.0), gamma);
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    /** A colour ramp for scalar fields, sampled from a list of stops. */
    public static int ramp(double[] stops, int[] colors, double v) {
        if (v <= stops[0]) {
            return colors[0];
        }
        for (int i = 0; i < stops.length - 1; i++) {
            if (v <= stops[i + 1]) {
                double t = (v - stops[i]) / Math.max(1e-9, stops[i + 1] - stops[i]);
                return lerpColor(colors[i], colors[i + 1], t);
            }
        }
        return colors[colors.length - 1];
    }

    /** Draws a vertical legend bar with min/max/mid labels. */
    public static void legend(Img img, int x, int y, int w, int h, double[] stops, int[] colors,
                              String label, String fmtLow, String fmtMid, String fmtHigh) {
        for (int j = 0; j < h; j++) {
            double v = stops[0] + (stops[stops.length - 1] - stops[0]) * (1.0 - j / (double) (h - 1));
            int c = ramp(stops, colors, v);
            img.hline(x, x + w - 1, y + j, c);
        }
        img.rect(x, y, w, h, 0x202020);
        img.text(x + w + 4, y - 2, label, 0xFFFFFF, 1);
        img.text(x + w + 4, y + 8, fmtHigh, 0xDDDDDD, 1);
        img.text(x + w + 4, y + h / 2 - 3, fmtMid, 0xDDDDDD, 1);
        img.text(x + w + 4, y + h - 8, fmtLow, 0xDDDDDD, 1);
    }

    /** A translucent dark panel behind a caption block. */
    public static void panel(Img img, int x, int y, int w, int h) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                img.blend(i, j, 0x0A0E14, 0.72);
            }
        }
        img.rect(x, y, w, h, 0x2A3340);
    }
}
