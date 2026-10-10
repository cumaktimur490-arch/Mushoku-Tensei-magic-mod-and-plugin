package dev.terrarealis.kernel.geo;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;
import dev.terrarealis.kernel.math.Spline;

/**
 * Climate model: latitude, elevation, continentality and — the part almost every terrain generator
 * skips — <b>orographic precipitation</b>.
 *
 * <p>Air masses pick up moisture over the ocean, and dump it on the first range they hit. The leeward
 * side is therefore a rain shadow. This one mechanism is why the Atacama, the Patagonian steppe, the
 * Great Basin and the Gobi exist, and it is why a mountain range in a realistic generator must have a
 * wet green side and a dry brown side. Terra Realis computes it by marching upwind from every sample
 * point and accumulating the upslope the air has already been forced over.
 *
 * <p>The zonal curves below are the real ones: ITCZ maximum near the equator, the subtropical dry
 * belt around 20-30 degrees (Hadley cell descent), a mid-latitude maximum where the westerlies and
 * storm tracks sit, and a polar desert beyond 70 degrees.
 */
public final class Climate {

    /** Mean annual temperature by absolute latitude, degrees C (ocean-level reference). */
    private static final Spline ZONAL_TEMP = Spline.of(
            0.0, 27.0,
            10.0, 26.5,
            20.0, 23.5,
            30.0, 18.0,
            40.0, 12.5,
            50.0, 5.5,
            60.0, -1.5,
            70.0, -11.0,
            80.0, -21.0,
            90.0, -30.0);

    /** Mean annual precipitation by absolute latitude, mm, at a windward coast. */
    private static final Spline ZONAL_PRECIP = Spline.of(
            0.0, 2450.0,
            7.0, 2250.0,
            14.0, 1400.0,
            21.0, 620.0,
            27.0, 210.0,
            33.0, 520.0,
            42.0, 950.0,
            52.0, 1050.0,
            60.0, 700.0,
            70.0, 330.0,
            80.0, 180.0,
            90.0, 110.0);

    /** UNEP aridity index thresholds (PET / P). */
    public static final double AI_HYPERARID = 10.0;
    public static final double AI_ARID = 2.0;
    public static final double AI_SEMIARID = 0.65;
    public static final double AI_DRY_SUBHUMID = 0.5;

    private Climate() {}

    public static double zonalTempC(double absLatitude) {
        return ZONAL_TEMP.eval(Interp.clamp(Math.abs(absLatitude), 0.0, 90.0));
    }

    public static double zonalPrecipMm(double absLatitude) {
        return ZONAL_PRECIP.eval(Interp.clamp(Math.abs(absLatitude), 0.0, 90.0));
    }

    /** Latitude in degrees for a world Z coordinate, honouring the configured map scale. */
    public static double latitudeAt(double z, GenParams p) {
        return p.latitudeAtZ0 + z / p.blocksPerDegreeLatitude;
    }

    /** Potential evapotranspiration, mm/yr. A linearised Thornthwaite: good enough for classification. */
    public static double petMm(double tempC) {
        return Math.max(60.0, 100.0 + tempC * 22.0);
    }

    /** UNEP aridity index: PET / P. >2 arid, 0.5-0.75 dry sub-humid, <0.5 humid. */
    public static double aridityIndex(double tempC, double precipMm) {
        return petMm(tempC) / Math.max(20.0, precipMm);
    }

    /**
     * Equilibrium line altitude of glaciers, metres a.s.l.
     *
     * <p>Falls with latitude (~88 m per degree) and falls further where precipitation is high, because
     * accumulation outweighs ablation. Rises in a rain shadow — which is exactly why the Andes are
     * glacier-covered at 20S while the leeward Patagonian steppe is not.
     */
    public static double snowLineMeters(double absLatitude, double precipMm) {
        double base = 5300.0 - 88.0 * Interp.clamp(absLatitude, 0.0, 80.0);
        double wet = Interp.clamp((precipMm - 900.0) * 0.42, -950.0, 700.0);
        return Math.max(-400.0, base - wet);
    }

    /**
     * Prevailing wind as the direction the air <i>travels towards</i>, world space (+x east, +z south).
     *
     * <p>Zonal cells — trades, westerlies, polar easterlies — mirrored across the equator, plus a slow
     * noise wobble so that rain shadows bend instead of forming perfect stripes.
     */
    public static void windVector(double latitude, double x, double z, GenParams p, Noise wind, double[] out) {
        double lat = latitude;
        double abs = Math.abs(lat);
        boolean north = lat >= 0;
        // Travel direction in (east, south) components for the northern hemisphere.
        double ex;
        double sz;
        if (abs < 30.0) {
            // Trades: NE -> SW.
            double t = Interp.smoothstep(0.0, 30.0, abs);
            ex = -0.75;
            sz = 0.55 + 0.20 * t;
        } else if (abs < 60.0) {
            // Westerlies: SW -> NE, the strongest cell.
            double t = Interp.smoothstep(30.0, 60.0, abs);
            ex = 0.92;
            sz = -(0.42 + 0.18 * t);
        } else {
            // Polar easterlies: NE -> SW, weak.
            ex = -0.62;
            sz = 0.45;
        }
        if (!north) {
            sz = -sz;
        }
        double az = Math.atan2(sz, ex);
        az += wind.perlin2(x / p.windNoiseScale, z / p.windNoiseScale) * 0.62;
        az += wind.perlin2(x / (p.windNoiseScale * 0.31) + 40.0, z / (p.windNoiseScale * 0.31) - 12.0) * 0.22;
        out[0] = Math.cos(az);
        out[1] = Math.sin(az);
    }

    /**
     * Solar radiation index: 1.0 on flat ground, higher on equator-facing slopes, lower on pole-facing
     * ones. Drives the north/south asymmetry of vegetation, snow retention and soil development that
     * makes real hillsides look different on each side.
     */
    public static double solarIndex(double latitude, double slopeRadians, double aspectRadians) {
        double equatorward = latitude >= 0 ? Math.PI : 0.0; // south-facing in the northern hemisphere
        double cosIncidence = Math.cos(slopeRadians) * Math.cos(Math.toRadians(Math.abs(latitude)) * 0.55)
                + Math.sin(slopeRadians) * Math.cos(aspectRadians - equatorward)
                        * Math.sin(Math.toRadians(Math.abs(latitude)) * 0.55 + 0.35);
        return Interp.clamp(0.55 + cosIncidence * 0.62, 0.2, 1.75);
    }

    /** Continentality 0 (coast) .. 1 (deep interior), from the continentalness field. */
    public static double continentality(double continentalness) {
        return Interp.smoothstep(0.015, 0.62, continentalness);
    }

    /**
     * Upslope the prevailing wind has already been forced over, in metres, plus the local orographic
     * gradient. Filled by {@link #marchUpwind}.
     */
    public static final class Orography {
        public double upslopeMeters;
        public double localSlope;
        public double shadowFactor = 1.0;
        public double enhancement = 1.0;
    }

    /**
     * Marches upwind from {@code (x, z)} sampling {@code heights} and accumulates forced ascent.
     *
     * <p>The accumulator decays geometrically, which gives the shadow a finite memory of roughly
     * {@code step / (1 - decay)} blocks. Without decay a single peak would shadow the whole map;
     * with too much decay no shadow forms at all.
     *
     * @param step    march step in blocks (should be >> the detail wavelength, ~128-256)
     * @param steps   number of march steps
     * @param decay   per-step memory decay, e.g. 0.90
     */
    public static void marchUpwind(double x, double z, double windX, double windZ,
                                   double step, int steps, double decay,
                                   HeightSampler heights, GenParams p, Orography out) {
        double acc = 0.0;
        double prev = heights.heightAt(x, z);
        double cx = x;
        double cz = z;
        double maxLocal = 0.0;
        for (int i = 0; i < steps; i++) {
            cx -= windX * step;
            cz -= windZ * step;
            double h = heights.heightAt(cx, cz);
            // We walk upwind, so a DROP in h along the walk means the air rose coming towards us.
            double rise = prev - h;
            if (rise > 0) {
                acc = acc * decay + rise;
                double rate = rise / step;
                if (rate > maxLocal) {
                    maxLocal = rate;
                }
            } else {
                acc *= decay * 0.94;
            }
            prev = h;
        }
        out.upslopeMeters = acc * p.metersPerBlock;
        out.localSlope = maxLocal;
        out.shadowFactor = Math.exp(-p.orographicK * out.upslopeMeters / 1000.0);
        out.enhancement = 1.0 + 0.95 * Interp.smoothstep(0.004, 0.055, maxLocal);
    }

    /** Abstraction so the climate model can be fed by the analytic field or a cached array. */
    public interface HeightSampler {
        double heightAt(double x, double z);
    }
}
