package dev.terrarealis.kernel.geo;

import java.util.Arrays;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;
import dev.terrarealis.kernel.math.Spline;

/**
 * Continental-scale hypsometry.
 *
 * <p>Two problems with naive {@code fbm > 0 ? land : water} terrain, both solved here:
 *
 * <p><b>1. Wrong shape of the elevation distribution.</b> Earth's hypsographic curve is strongly
 * bimodal: ~71% of the surface sits around -3.7 km (abyssal plains) and the continental mode is near
 * +0.8 km, with a narrow shelf and a steep continental slope between them. Summed Perlin noise is
 * gaussian and unimodal, which is why procedural maps look like "wrinkled oatmeal" — everything is
 * mid-elevation and coastlines are fractal mush. We fix this by (a) standardising the noise against
 * its <i>own measured</i> mean and sigma, (b) applying a bimodalising power transform, and (c)
 * shifting the distribution so the measured fraction below sea level equals
 * {@link GenParams#oceanFraction} exactly.
 *
 * <p><b>2. No shelf geometry.</b> The continentalness value is then pushed through a monotone cubic
 * spline whose knots encode real margin morphology: wide shallow shelf, sharp shelf break, steep
 * continental slope, flat abyssal plain. Because the spline is C1 and monotone, hillshading shows no
 * terraces and coastlines never invert.
 */
public final class Continents {

    /** continentalness (-1 deep ocean .. +1 highest summit) -> normalised elevation (-1 .. +1). */
    private static final double[] HYPSO_KNOTS = {
            -1.00, -1.000,
            -0.62, -0.680,
            -0.34, -0.360,
            -0.16, -0.130,
            -0.06, -0.035,
            -0.015, -0.006,
            0.000, 0.000,
            0.015, 0.010,
            0.060, 0.030,
            0.160, 0.070,
            0.320, 0.140,
            0.500, 0.260,
            0.660, 0.430,
            0.800, 0.640,
            0.900, 0.830,
            1.000, 1.000,
    };

    private final GenParams p;
    private final Noise field;
    private final Noise blocks;
    private final Noise inselberg;
    private final Noise aridProxy;
    private final Spline hypso;
    private final double invScale;
    private final double mean;
    private final double invSigma;
    private final double offset;

    public Continents(long seed, GenParams params) {
        this.p = params;
        this.field = new Noise(seed ^ 0x2F6E2B1L);
        this.blocks = new Noise(seed ^ 0x6B1A5E2CL);
        this.inselberg = new Noise(seed ^ 0x4D0C7A93L);
        this.aridProxy = new Noise(seed ^ 0x7E5B21C4L);
        this.hypso = Spline.of(HYPSO_KNOTS);
        this.invScale = 1.0 / Math.max(128.0, params.continentScale);

        // Measure the noise distribution once so the sea level offset is exact rather than guessed.
        int n = 96;
        double[] sample = new double[n * n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double v = raw((i - n / 2) * 37.0, (j - n / 2) * 37.0);
                sample[i * n + j] = v;
                sum += v;
            }
        }
        double m = sum / sample.length;
        double var = 0;
        for (double v : sample) {
            var += (v - m) * (v - m);
        }
        double sd = Math.sqrt(var / sample.length);
        this.mean = m;
        this.invSigma = sd < 1e-6 ? 1.0 : 1.0 / sd;

        // Standardise, then bimodalise, then shift so oceanFraction of the area falls below zero.
        for (int i = 0; i < sample.length; i++) {
            sample[i] = bimodalise(Interp.clamp((sample[i] - m) * invSigma * 0.52, -1.0, 1.0));
        }
        Arrays.sort(sample);
        int idx = Interp.clamp((int) Math.round(params.oceanFraction * sample.length), 0, sample.length - 1);
        this.offset = sample[idx];
    }

    private double raw(double nx, double nz) {
        return field.warpedFbm2(nx, nz, p.continentOctaves, p.continentWarp, 3, 2.35);
    }

    /** Pushes a symmetric distribution towards its extremes: more ocean floor, more continent. */
    private static double bimodalise(double u) {
        return Math.signum(u) * Math.pow(Math.abs(u), 0.80);
    }

    /**
     * Continentalness in [-1, 1]; 0 is exactly the coastline in a distributional sense.
     * Cheap enough to call per block (one warped fBm).
     */
    public double continentalness(double x, double z) {
        double u = Interp.clamp((raw(x * invScale, z * invScale) - mean) * invSigma * 0.52, -1.0, 1.0);
        return Interp.clamp(bimodalise(u) - offset, -1.0, 1.0);
    }

    /** Continentalness mapped to a normalised elevation, -1 (deepest) .. +1 (highest). */
    public double normalisedElevation(double continentalness) {
        return hypso.eval(Interp.clamp(continentalness * p.shelfSharpness, -1.0, 1.0));
    }

    /**
     * Pre-orogeny bedrock elevation, in block Y. Everything above {@code seaLevel} is land; the
     * {@code arc}/{@code trench}/{@code rift} terms are the tectonic first-order corrections.
     */
    public double tectonicBaseHeight(int x, int z, Tectonics.Setting t, Noise relief) {
        double cont = continentalness(x, z);
        double norm = normalisedElevation(cont);
        double y;
        if (norm < 0) {
            y = p.seaLevel + norm * p.maxDepth;
        } else {
            // Continentalness alone gives the *cratonic* hypsometry: shelves, plains, shields and
            // tablelands, i.e. almost everything below a kilometre. Orogenic belts, extensional ranges
            // and volcanoes add the rest, exactly as they do on Earth, where mean land elevation is
            // ~840 m and everything above ~2 km is tectonically young.
            y = p.seaLevel + Math.pow(norm, 1.15) * p.landRelief * p.reliefGain;
        }

        double land = Interp.smoothstep(-0.02, 0.06, cont);

        // Orogenic thickening: belts are ridged and follow the belt azimuth so they are linear.
        double along = Math.cos(t.beltAzimuth);
        double across = Math.sin(t.beltAzimuth);
        double rotX = (x * along + z * across) / p.mountainScale;
        double rotZ = (-x * across + z * along) / p.mountainScale;
        double ridged = relief.warpedRidged2(rotX, rotZ, p.mountainOctaves, 0.55, 3, 2.6);
        ridged = Math.max(0.0, ridged);
        double belt = Math.pow(Interp.clamp(t.arc, 0.0, 1.0), 0.8) * land;
        y += Math.pow(ridged, 1.45) * p.maxRelief * 1.25 * belt;
        // Broad orogenic plateau behind the belt: convergence thickens the whole crust, not just the
        // ridge crest. This is what makes a range read as a range from 40 km away.
        y += Interp.smoothstep(0.10, 0.55, t.arc) * 34.0 * land;

        // Volcanic arc cones sit inboard of the trench on a convergent margin.
        y += t.arc * t.arc * 12.0 * land * Interp.smoothstep(0.2, 0.9, ridged);

        // Subduction trench: only where the boundary is offshore.
        double ocean = 1.0 - land;
        y -= t.trenchDepth() * p.maxDepth * 0.55 * ocean;

        // Divergent boundary: rift valley on land, mid-ocean ridge under water.
        double riftSign = cont > 0.0 ? -1.0 : 1.0;
        y += riftSign * t.rift * (cont > 0.0 ? 22.0 : 16.0);

        // Transform/fault lineaments: straight grabens and pressure ridges.
        double lineament = relief.ridged2(x / (p.mountainScale * 0.42) + 61.0,
                z / (p.mountainScale * 0.42) - 33.0, 3, 2.0, 0.5);
        y += t.fault * lineament * 9.0;

        // ------------------------------------------------- basin & range
        // Extensional provinces do not make gaussian hills: they make parallel, linear, tilted fault
        // blocks with a steep range-front scarp on one side and a gently dipping back slope on the
        // other, and flat sediment-filled valleys between them. This is the Peninsular Ranges of
        // Anza-Borrego and the sky islands of the Sonoran, and it is the single most recognisable
        // landform of the arid American West.
        if (t.extension > 0.02) {
            double az = t.beltAzimuth + Math.PI * 0.5; // ranges strike perpendicular to extension
            double cosA = Math.cos(az);
            double sinA = Math.sin(az);
            double brAcross = (x * cosA + z * sinA) / p.basinRangeScale;
            double brAlong = (-x * sinA + z * cosA) / (p.basinRangeScale * 3.2);
            // Slow along-strike modulation: ranges die out, branch and step, they are not wallpaper.
            double envelope = 0.45 + 0.55 * (0.5 + 0.5 * blocks.fbm2(brAlong * 1.7 + 12.0,
                    brAcross * 0.18 - 7.0, 3, 2.0, 0.5));
            double phase = brAcross * Math.PI * 2.0
                    + blocks.perlin2(brAlong * 2.3, brAcross * 0.55) * 1.15;
            double f = phase / (Math.PI * 2.0);
            double frac = f - Math.floor(f);
            // Asymmetric sawtooth: a scarp on one face, a tilted back slope on the other.
            double tilt = p.blockTilt;
            double knee = 0.5 - 0.30 * tilt;
            double profile;
            if (frac < knee) {
                profile = Interp.smoothstep(0.0, knee, frac);
            } else {
                profile = 1.0 - Interp.smoothstep(knee, 1.0, frac) * (1.0 - tilt * 0.55);
            }
            double amp = p.basinRangeAmplitude * t.extension * envelope * reliefGainSafe();
            y += (profile - 0.42) * amp;
            // Valley fill: grabens are sediment starved of relief but not flat-flat; a little texture.
            double valley = 1.0 - Interp.smoothstep(0.05, 0.55, profile);
            y -= valley * amp * 0.10;
        }

        // ----------------------------------------------------- inselbergs
        // Isolated rock domes standing above an eroded plain: bornhardts and inselbergs. They need an
        // arid or seasonally arid climate (no soil to hide them) and hard crystalline rock, so they are
        // gated on a latitude-and-continentality aridity proxy.
        if (p.inselbergs) {
            double aridity = aridityProxyAt(x, z, cont);
            if (aridity > 0.25) {
                double inv = 1.0 / Math.max(600.0, p.inselbergSpacing);
                Noise.Worley w = inselberg.worley2(x * inv, z * inv, 0.5);
                long id = dev.terrarealis.kernel.math.Hash.hash(inselberg.seed(), w.cellX, w.cellZ);
                if (dev.terrarealis.kernel.math.Hash.unit(id) < 0.42) {
                    double radius = p.inselbergRadius
                            * (0.55 + 0.9 * dev.terrarealis.kernel.math.Hash.unit(
                                    dev.terrarealis.kernel.math.Hash.mix64(id ^ 0x9E37L)));
                    double d = w.f1 / inv; // distance to the dome centre, in blocks
                    if (d < radius) {
                        double u = 1.0 - (d / radius);
                        // Dome profile: steep-sided, rounded top. u^1.4 keeps the shoulder convex.
                        double dome = Math.pow(u, 1.4) * (0.72 + 0.5 * u);
                        double h = p.inselbergHeight * aridity
                                * dev.terrarealis.kernel.math.Hash.unit(
                                        dev.terrarealis.kernel.math.Hash.mix64(id ^ 0x51EDL));
                        y += dome * h * land;
                    }
                }
            }
        }

        // Non-orogenic hills: the "old, worn down" landscape between the belts.
        double hills = relief.fbm2(x / p.hillScale + 5.5, z / p.hillScale - 8.25, p.hillOctaves, 2.0, 0.5);
        double hillMask = (1.0 - 0.72 * t.arc) * Interp.smoothstep(0.0, 0.30, cont)
                * (1.0 - Interp.smoothstep(0.55, 0.95, cont));
        y += hills * 12.0 * hillMask;

        // Plateaus / tablelands: flat-topped where continentalness is high but orogeny is low.
        double plateau = Interp.smoothstep(0.24, 0.46, cont) * (1.0 - t.arc);
        y += plateau * 24.0;

        // Pre-erosion detail. Most block-scale roughness is added *after* erosion instead, otherwise
        // the erosion passes simply plane it off.
        double detail = relief.fbm2(x / p.detailScale, z / p.detailScale, p.detailOctaves, 2.0, 0.5);
        y += detail * 3.2 * (0.25 + 0.75 * (1.0 - land) + 0.5 * land);

        return Interp.clamp(y, p.minY + 6, p.maxY - 6);
    }

    /** Exposed for the biome/climate models: raw continentalness is a good aridity proxy. */
    public GenParams params() {
        return p;
    }

    private double reliefGainSafe() {
        return p.reliefGain <= 0 ? 1.0 : Math.min(p.reliefGain, 2.5);
    }

    /**
     * Cheap analytic aridity estimate, 0..1, for features that must be decided without a climate tile
     * (inselbergs, dune supply). Uses the zonal precipitation curve, the latitude, continentality and a
     * large-scale wobble — it deliberately ignores orography, which the real climate model handles.
     */
    public double aridityProxyAt(double x, double z, double continentalness) {
        double lat = Math.abs(Climate.latitudeAt(z, p));
        double zonalP = Climate.zonalPrecipMm(lat);
        double zonalT = Climate.zonalTempC(lat);
        double cont = Climate.continentality(continentalness);
        double supply = zonalP * Interp.lerp(1.0, 0.42, cont);
        double wobble = 0.55 + 0.45 * (0.5 + 0.5 * aridProxy.fbm2(x / p.precipNoiseScale + 33.0,
                z / p.precipNoiseScale - 19.0, 3, 2.0, 0.5));
        supply *= wobble;
        double pet = Climate.petMm(zonalT);
        double ai = pet / Math.max(40.0, supply);
        // AI 0.5 humid -> 0, AI 2 arid -> ~0.7, AI 10 hyperarid -> 1
        return Interp.clamp((ai - 0.62) / 5.5, 0.0, 1.0);
    }

    public Spline hypsometry() {
        return hypso;
    }
}
