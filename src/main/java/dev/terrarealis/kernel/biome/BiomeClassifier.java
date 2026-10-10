package dev.terrarealis.kernel.biome;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.Rock;
import dev.terrarealis.kernel.geo.Climate;
import dev.terrarealis.kernel.geo.Geology;
import dev.terrarealis.kernel.math.Hash;
import dev.terrarealis.kernel.math.Interp;

/**
 * Biome assignment from climate, altitude, drainage and substrate.
 *
 * <p>Vanilla picks biomes from a six-dimensional "climate" space whose axes (temperature, humidity,
 * continentalness, erosion, depth, weirdness) are independent noises. The result is that a jungle can
 * border an ice field, deserts appear at 60 degrees north, and mountains are decorated with the biome
 * that happened to win the noise lottery. None of that happens on Earth, and the reason it does not is
 * that biomes are the <i>output</i> of a physical system, not an input.
 *
 * <p>Here the order of causality is respected:
 * <pre>
 *   latitude + elevation + orography  ->  temperature, precipitation
 *   temperature, precipitation        ->  aridity index (UNEP PET/P)
 *   aridity + temperature             ->  Whittaker life zone
 *   altitude relative to treeline/snowline -> altitudinal belt
 *   substrate (soil taxon, lithology) ->  local override (dune, playa, bog, badland, till)
 *   drainage (channel, water table)   ->  riparian and wetland override
 * </pre>
 *
 * <p>Where several biomes are equally valid for the same climate — birchwood versus mixed forest
 * versus deciduous forest in a humid temperate zone — the choice is made with a large-scale hash of the
 * coordinates. That gives a <i>mosaic</i>: patchy, contiguous, and reproducible for the same seed, which
 * is how real forest composition varies with soil and disturbance history.
 */
public final class BiomeClassifier {

    /** Everything the classifier needs about one location. */
    public static final class Env {
        public int x;
        public int z;
        public double latitude;
        /** Terrain surface Y, blocks. */
        public int surfaceY;
        /** Standing water surface Y, or {@link Integer#MIN_VALUE}. */
        public int waterY = Integer.MIN_VALUE;
        /** Positive when the ground is submerged, in blocks. */
        public double waterDepth;
        public boolean marine;
        public boolean river;
        public boolean lake;
        public double tempC;
        public double precipMm;
        public double aridity;
        public double sri;
        public double elevationM;
        public double snowLineM;
        public double treeLineM;
        public double slopeDeg;
        public double glacier;
        public double outcrop;
        public double channel;
        public double delta;
        public double volcano;
        public double karst;
        public double continentalness;
        public double oceanDistanceBlocks;
        public boolean permafrost;
        public double windExposure;
        /** 0..1, how close the water table is to the surface. */
        public double saturation;
        public Rock rock;
        public Geology.SoilKind soil;
        public GenParams params;
    }

    private BiomeClassifier() {}

    /** Altitude of the closed-forest limit, metres a.s.l. */
    public static double treeLineMeters(double latitude, double precipMm, double windExposure) {
        double base = 4350.0 - 62.0 * Interp.clamp(Math.abs(latitude), 0.0, 75.0);
        // Drought and wind both depress the treeline; waterlogging depresses it too.
        double dry = Interp.clamp((precipMm - 400.0) * 0.35, -700.0, 250.0);
        return Math.max(30.0, base + dry - windExposure * 260.0);
    }

    public static BiomeKind classify(Env e) {
        // ------------------------------------------------------------ volcanic
        if (e.volcano > 0.55) {
            return e.elevationM > 850.0 ? BiomeKind.VOLCANIC_SUMMIT : BiomeKind.VOLCANIC_FLANK;
        }

        // --------------------------------------------------------------- ice
        if (e.glacier > 0.32 && e.elevationM > e.snowLineM - 120.0) {
            if (Math.abs(e.latitude) > 72.0) {
                return BiomeKind.ICE_CAP;
            }
            return e.slopeDeg > 34.0 ? BiomeKind.JAGGED_PEAKS : BiomeKind.FROZEN_PEAKS;
        }

        // ------------------------------------------------------------- marine
        if (e.marine && e.waterDepth > 0.5) {
            return ocean(e);
        }

        // -------------------------------------------------------- fluvial/lacustrine
        if (e.river || e.lake) {
            if (e.tempC < -1.0) {
                return BiomeKind.FROZEN_RIVER;
            }
            if (e.delta > 0.28 && e.oceanDistanceBlocks < 500.0) {
                return BiomeKind.ESTUARY;
            }
            if (e.lake && e.slopeDeg < 1.5 && e.channel < 0.2) {
                return e.precipMm > 900.0 ? BiomeKind.FEN : BiomeKind.RIVER;
            }
            return BiomeKind.RIVER;
        }

        // ------------------------------------------------------------ coastal
        if (Math.abs(e.surfaceY - e.params.seaLevel) <= 4 && e.oceanDistanceBlocks < 48.0) {
            if (e.slopeDeg > 24.0 || e.outcrop > 0.65) {
                return BiomeKind.STONY_SHORE;
            }
            if (e.delta > 0.30) {
                return BiomeKind.TIDAL_FLAT;
            }
            if (e.tempC < 0.5) {
                return BiomeKind.SNOWY_BEACH;
            }
            if (e.tempC > 19.0 && e.precipMm > 1150.0 && e.slopeDeg < 4.0
                    && Interp.smoothstep(0.5, 1.0, e.saturation) > 0.4) {
                return BiomeKind.MANGROVE;
            }
            // No mushroom fields: they are a fantasy biome and the single biggest source of
            // giant mushrooms in vanilla decoration. Real coastlines get beaches, flats and marshes.
            return BiomeKind.BEACH;
        }

        // --------------------------------------------------- altitudinal belts
        if (e.elevationM > e.snowLineM) {
            if (e.slopeDeg > 38.0) {
                return BiomeKind.JAGGED_PEAKS;
            }
            if (e.slopeDeg > 22.0) {
                return BiomeKind.ROCKY_PEAKS;
            }
            return BiomeKind.FROZEN_PEAKS;
        }
        if (e.elevationM > e.treeLineM) {
            if (e.slopeDeg > 33.0) {
                return BiomeKind.SCREE_SLOPE;
            }
            if (e.outcrop > 0.55) {
                return BiomeKind.ROCKY_PEAKS;
            }
            if (e.aridity > Climate.AI_ARID) {
                return BiomeKind.WINDSWEPT_GRAVEL;
            }
            return BiomeKind.ALPINE_TUNDRA;
        }
        if (e.elevationM > e.treeLineM - 480.0) {
            if (e.slopeDeg > 30.0) {
                return e.precipMm > 500.0 ? BiomeKind.WINDSWEPT_FOREST : BiomeKind.WINDSWEPT_GRAVEL;
            }
            if (e.aridity > Climate.AI_SEMIARID) {
                return BiomeKind.MONTANE_MEADOW;
            }
            if (e.precipMm > 420.0 && e.tempC > 1.0) {
                return BiomeKind.MONTANE_FOREST;
            }
            return BiomeKind.MONTANE_MEADOW;
        }

        // ------------------------------------------------------- drainage overrides
        if (e.saturation > 0.82 && e.slopeDeg < 3.0 && e.precipMm > 380.0) {
            if (e.tempC < 6.0) {
                return BiomeKind.PEAT_BOG;
            }
            return e.tempC > 17.0 ? BiomeKind.WETLAND : BiomeKind.FEN;
        }
        if (e.channel > 0.12 && e.soil == Geology.SoilKind.ALLUVIUM) {
            return e.tempC < 4.0 ? BiomeKind.FOREST_TUNDRA : BiomeKind.FLOODPLAIN;
        }

        // ---------------------------------------------------------- substrate
        BiomeKind substrate = substrateBiome(e);
        if (substrate != null) {
            return substrate;
        }

        // --------------------------------------------------------- Whittaker
        return whittaker(e);
    }

    /**
     * Which ocean biome, from depth and temperature. Warm/shallow shelves grow coral; cold, nutrient
     * rich shelves grow kelp — vanilla already implements both as biome-specific features, so getting
     * the classification right is what puts reefs in the tropics instead of everywhere.
     */
    private static BiomeKind ocean(Env e) {
        double depth = e.waterDepth * e.params.metersPerBlock;
        boolean polar = e.tempC < -1.2 || Math.abs(e.latitude) > e.params.polarLatitude - 6.0;
        boolean deep = depth > 260.0;
        if (polar) {
            return deep ? BiomeKind.DEEP_FROZEN_OCEAN : BiomeKind.FROZEN_OCEAN;
        }
        if (e.tempC > 20.5 && depth < 130.0) {
            return BiomeKind.WARM_OCEAN;
        }
        if (e.tempC > 12.0) {
            return deep ? BiomeKind.DEEP_LUKEWARM_OCEAN : BiomeKind.LUKEWARM_OCEAN;
        }
        if (e.tempC > 4.0) {
            return deep ? BiomeKind.DEEP_OCEAN : BiomeKind.OCEAN;
        }
        return deep ? BiomeKind.DEEP_COLD_OCEAN : BiomeKind.COLD_OCEAN;
    }

    private static BiomeKind substrateBiome(Env e) {
        switch (e.soil) {
            case DUNE:
                return e.oceanDistanceBlocks < 520.0 && e.precipMm < 200.0
                        ? BiomeKind.COASTAL_DESERT : BiomeKind.DESERT;
            case SOLONCHAK:
                return BiomeKind.PLAYA;
            case HISTOSOL:
                return e.tempC < 7.0 ? BiomeKind.PEAT_BOG : BiomeKind.WETLAND;
            case TILL:
                if (e.tempC < -6.0) {
                    return BiomeKind.POLAR_DESERT;
                }
                return e.tempC < 3.0 ? BiomeKind.TUNDRA : BiomeKind.PERMAFROST_GROUND;
            case GELISOL:
                return e.permafrost ? BiomeKind.PERMAFROST_GROUND : BiomeKind.TUNDRA;
            default:
                break;
        }
        // Badlands: arid, unconsolidated, steeply dissected. The classic combination is an evaporite or
        // clastic province that has been cut by ephemeral streams with no vegetation to protect it.
        if (e.aridity > 1.55 && (e.rock == Rock.EVAPORITE || e.rock == Rock.CLASTIC || e.rock == Rock.SHALE)) {
            if (e.precipMm > 480.0 && e.slopeDeg < 18.0) {
                return BiomeKind.WOODED_BADLANDS;
            }
            if (e.slopeDeg > 24.0) {
                return BiomeKind.ERODED_BADLANDS;
            }
            return BiomeKind.BADLANDS;
        }
        if (e.outcrop > 0.72 && e.slopeDeg > 20.0) {
            return e.tempC < 2.0 ? BiomeKind.SCREE_SLOPE : BiomeKind.WINDSWEPT_GRAVEL;
        }
        return null;
    }

    private static BiomeKind whittaker(Env e) {
        double ai = e.aridity;
        double t = e.tempC;
        double p = e.precipMm;

        if (ai > Climate.AI_ARID) {
            if (ai > Climate.AI_HYPERARID) {
                return t > 10.0 ? BiomeKind.DESERT : BiomeKind.COLD_DESERT;
            }
            if (t > 16.0) {
                return p > 240.0 ? BiomeKind.DRY_SHRUBLAND : BiomeKind.DESERT;
            }
            if (t > 4.0) {
                return p > 330.0 ? BiomeKind.STEPPE : BiomeKind.COLD_DESERT;
            }
            return BiomeKind.COLD_DESERT;
        }

        if (ai > Climate.AI_SEMIARID) {
            if (t > 19.0) {
                return e.elevationM > 320.0 ? BiomeKind.SAVANNA_PLATEAU : BiomeKind.SAVANNA;
            }
            if (t > 8.0) {
                return p > 520.0 ? BiomeKind.FLOWERING_MEADOW : BiomeKind.STEPPE;
            }
            if (t > 1.0) {
                return BiomeKind.STEPPE;
            }
            return BiomeKind.FOREST_TUNDRA;
        }

        if (ai > Climate.AI_DRY_SUBHUMID) {
            if (t > 20.0) {
                return BiomeKind.TROPICAL_SEASONAL;
            }
            if (t > 11.0) {
                // Mediterranean-type: high solar index, warm, at the dry end of the forest band.
                return e.sri > 1.12 && p < 760.0 ? BiomeKind.MEDITERRANEAN : temperateForest(e);
            }
            if (t > 3.0) {
                return borealForest(e);
            }
            if (t > -3.0) {
                return BiomeKind.SNOWY_TAIGA;
            }
            return BiomeKind.FOREST_TUNDRA;
        }

        // Humid.
        if (t > 23.5 && p > 1750.0) {
            return BiomeKind.TROPICAL_RAINFOREST;
        }
        if (t > 19.5) {
            return BiomeKind.TROPICAL_MONSOON;
        }
        if (t > 12.0) {
            return temperateForest(e);
        }
        if (t > 6.0) {
            // Dark forest is deliberately never emitted: vanilla decorates it with huge mushrooms.
            // The humid cool-temperate slot goes to old-growth spruce instead - giant trees, no fungi.
            return p > 1250.0 ? BiomeKind.BOREAL_OLD_GROWTH : temperateForest(e);
        }
        if (t > 1.0) {
            return borealForest(e);
        }
        if (t > -4.0) {
            return BiomeKind.SNOWY_TAIGA;
        }
        if (t > -12.0) {
            return BiomeKind.TUNDRA;
        }
        return p < 260.0 ? BiomeKind.POLAR_DESERT : BiomeKind.TUNDRA;
    }

    private static BiomeKind temperateForest(Env e) {
        double m = mixture(e, 31337, 5200);
        if (e.slopeDeg > 22.0) {
            return e.precipMm > 700.0 ? BiomeKind.WINDSWEPT_FOREST : BiomeKind.WINDSWEPT_HILLS;
        }
        if (e.precipMm > 1500.0 && e.tempC > 15.0 && m < 0.22) {
            return BiomeKind.CHERRY_GROVE;
        }
        if (m < 0.20) {
            return BiomeKind.BIRCHWOOD;
        }
        if (m < 0.30) {
            return BiomeKind.OLD_GROWTH_BIRCH;
        }
        if (m < 0.52) {
            return BiomeKind.TEMPERATE_MIXED;
        }
        if (m < 0.72) {
            return BiomeKind.TEMPERATE_DECIDUOUS;
        }
        if (m < 0.86 && e.precipMm > 900.0) {
            return BiomeKind.BOREAL_FOREST;
        }
        return BiomeKind.FLOWERING_MEADOW;
    }

    private static BiomeKind borealForest(Env e) {
        double m = mixture(e, 9091, 6100);
        if (e.slopeDeg > 26.0) {
            return BiomeKind.WINDSWEPT_FOREST;
        }
        if (m < 0.24) {
            return BiomeKind.BOREAL_PINE;
        }
        if (m < 0.58) {
            return BiomeKind.BOREAL_OLD_GROWTH;
        }
        if (m < 0.90) {
            return BiomeKind.BOREAL_FOREST;
        }
        return e.saturation > 0.6 ? BiomeKind.PEAT_BOG : BiomeKind.BOREAL_FOREST;
    }

    /**
     * Large-scale pseudo-random in [0,1), used to break ties between climatically equivalent biomes so
     * that forest composition forms a contiguous mosaic instead of a checkerboard.
     *
     * @param cell blocks — the mosaic wavelength
     */
    private static double mixture(Env e, int salt, double cell) {
        long h = Hash.hash(salt, (long) Math.floor(e.x / cell), (long) Math.floor(e.z / cell));
        return Hash.unit(h);
    }

    /**
     * Subterranean biome, chosen from depth below the surface plus the host lithology and climate.
     * Biomes are three-dimensional in modern Minecraft, so caves get their own: a karst cavern under an
     * arid plateau is not the same place as a lush cave under a rainforest, and the difference is
     * visible in the decorations vanilla places.
     */
    public static BiomeKind subterranean(Env e, int y) {
        int depth = e.surfaceY - y;
        if (depth < 12) {
            return null; // too shallow: the surface biome is still the right answer
        }
        // Only the plain cavern biome (vanilla dripstone caves) is emitted underground. Lush caves
        // and the deep dark are fantasy flora and sculk: neither has a counterpart in the real karst
        // and phreatic systems this kernel simulates, and neither is wanted here.
        return BiomeKind.CAVERN;
    }
}
