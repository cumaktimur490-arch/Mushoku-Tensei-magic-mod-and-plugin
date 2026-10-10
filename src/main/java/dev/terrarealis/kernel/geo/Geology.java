package dev.terrarealis.kernel.geo;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.Material;
import dev.terrarealis.kernel.Rock;
import dev.terrarealis.kernel.math.Hash;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;

/**
 * Lithology, structural geology, pedology and metallogeny.
 *
 * <p><b>Lithology.</b> Which rock you are standing on is not random: it is a function of tectonic
 * setting. Oceanic crust is mafic, continental shields are granitic and gneissic, foreland basins
 * accumulate clastics and carbonates, arcs are volcanic, and glaciated terrain is buried in drift.
 * The province field combines the tectonic setting with a large-scale basin noise so that provinces
 * are contiguous over tens of kilometres.
 *
 * <p><b>Structure.</b> Sedimentary and metamorphic provinces are bedded. Bedding planes are given a
 * strike and a dip taken from a fold field: dip is {@code maxDip * sin(foldPhase)}, which produces
 * alternating dip directions and therefore real anticlines and synclines. In the block builder the
 * strata index is evaluated along the tilted bedding normal, so a cliff face shows <i>inclined</i>
 * layers, and a dip slope forms a cuesta on one side and an escarpment on the other.
 *
 * <p><b>Pedology.</b> Soil is not "3 blocks of dirt". Soil kind follows climate and parent material
 * (laterite in the wet tropics, podzol in boreal forests, aridisol and caliche in deserts, andosol on
 * volcanic ash, histosol in waterlogged ground, till under glaciers) and soil <i>thickness</i>
 * follows slope: steep slopes are stripped to bedrock, footslopes and valley floors accumulate. That
 * single rule removes the biggest tell of procedural terrain — uniform soil everywhere.
 *
 * <p><b>Metallogeny.</b> Ore is placed by province and depth rather than uniformly: coal in black
 * shale and clastic basins, copper in mafic volcanics and porphyry granites, gold in granitic veins
 * and as placer deposits in river gravels, diamonds only in deep ultramafic pipes, lead-zinc
 * analogues in carbonates.
 */
public final class Geology {

    /** Soil taxon, loosely after the World Reference Base. */
    public enum SoilKind {
        /** Bare bedrock, no mantle. */
        LITHOSOL(0.0, Material.STONE),
        /** Thin, immature soil on a steep slope. */
        RANKER(1.0, Material.COARSE_DIRT),
        /** Temperate deciduous forest soil. */
        BROWN_EARTH(1.0, Material.DIRT),
        /** Coniferous/boreal, leached, acidic. */
        PODZOL(1.0, Material.PODZOL),
        /** Wet tropical, iron-rich, deep red. */
        LATERITE(1.0, Material.RED_TERRACOTTA),
        /** Arid, pale, often with a caliche horizon. */
        ARIDISOL(1.0, Material.SAND),
        /** Saline lake bed / playa. */
        SOLONCHAK(1.0, Material.WHITE_TERRACOTTA),
        /** Organic, waterlogged. */
        HISTOSOL(1.0, Material.MUD),
        /** Volcanic ash soil, dark and fertile. */
        ANDOSOL(1.0, Material.COARSE_DIRT),
        /** Wind-blown silt. */
        LOESS(1.0, Material.DIRT),
        /** Glacial till, unsorted, bouldery. */
        TILL(1.0, Material.GRAVEL),
        /** River/alluvial sediment. */
        ALLUVIUM(1.0, Material.GRAVEL),
        /** Aeolian sand. */
        DUNE(1.0, Material.SAND),
        /** Permafrost/organic tundra mat. */
        GELISOL(1.0, Material.MOSS_BLOCK);

        /** Multiplier on the climate-derived soil thickness. */
        public final double thicknessGain;
        /** Default surface material before the biome overlay decides grass/sand/snow. */
        public final Material subsoil;

        SoilKind(double thicknessGain, Material subsoil) {
            this.thicknessGain = thicknessGain;
            this.subsoil = subsoil;
        }
    }

    /** Per-column geology, computed once and reused for the whole column. */
    public static final class Site {
        public Rock rock = Rock.GRANITIC;
        public SoilKind soil = SoilKind.BROWN_EARTH;
        /** Bedding dip in radians (signed: the sign flips across a fold axis). */
        public double dip;
        /** Bedding strike azimuth in radians. */
        public double strike;
        /** Spacing of strata in blocks for this province. */
        public double strataSpacing = 6.0;
        /** Soil mantle thickness in blocks. */
        public double soilThickness = 3.0;
        /** Depth of the weathered regolith below the soil, in blocks. */
        public double regolith = 3.0;
        /** 0..1 — how karstic (dissolution-prone) this column is. */
        public double karst;
        /** 0..1 — fracture density, drives fissure caves and cliff jointing. */
        public double fracture;
        /** 0..1 — how much of the surface is bedrock outcrop rather than soil. */
        public double outcrop;
    }

    private final GenParams p;
    private final Noise province;
    private final Noise basin;
    private final Noise foldPhase;
    private final Noise strikeField;
    private final Noise fractureField;
    private final Noise soilNoise;

    public Geology(long seed, GenParams params) {
        this.p = params;
        this.province = new Noise(seed ^ 0x51ED270BL);
        this.basin = new Noise(seed ^ 0x3C6EF372L);
        this.foldPhase = new Noise(seed ^ 0x2E1B2138L);
        this.strikeField = new Noise(seed ^ 0x77A61D2FL);
        this.fractureField = new Noise(seed ^ 0x1F123BB5L);
        this.soilNoise = new Noise(seed ^ 0x4C9E1F8AL);
    }

    /**
     * Classifies one column.
     *
     * @param continentalness -1..1 from {@link Continents}
     * @param tectonic        plate setting
     * @param slopeDegrees    post-erosion surface slope
     * @param temperature     mean annual, degrees C
     * @param aridity         UNEP aridity index (PET/P)
     * @param precipitation   mm/yr
     * @param glacial         0..1 active glaciation
     * @param riverChannel    0..1 channel strength at this column
     * @param depositional    0..1 net sediment deposition
     */
    public void site(int x, int z, double continentalness, Tectonics.Setting tectonic,
                     double slopeDegrees, double temperature, double aridity, double precipitation,
                     double glacial, double riverChannel, double depositional, Site out) {

        double nx = x / p.provinceScale;
        double nz = z / p.provinceScale;
        double prov = province.fbm2(nx + 3.1, nz - 7.7, 4, 2.0, 0.5);
        double bas = basin.fbm2(nx * 2.4 - 11.0, nz * 2.4 + 5.0, 3, 2.0, 0.5);

        out.rock = classifyRock(continentalness, tectonic, prov, bas, temperature, aridity, glacial);

        // ---------------------------------------------------------- structure
        double foldGain = 0.22 + 0.78 * tectonic.arc;
        double phase = foldPhase.fbm2(x / p.structuralScale + 2.3, z / p.structuralScale - 4.9,
                3, 2.0, 0.5) * Math.PI * 2.0;
        out.dip = Math.toRadians(p.maxDipDeg * foldGain * Math.sin(phase));
        out.strike = strikeField.perlin2(x / (p.structuralScale * 1.7), z / (p.structuralScale * 1.7)) * Math.PI;
        out.strataSpacing = p.strataSpacing * (0.6 + 0.9 * Hash.unit(
                Hash.hash(province.seed(), x >> 8, z >> 8)));

        // Folding concentrates fractures at hinge zones and along faults.
        double frac = 0.5 + 0.5 * fractureField.ridged2(x / (p.structuralScale * 0.45),
                z / (p.structuralScale * 0.45), 3, 2.0, 0.5);
        out.fracture = Interp.clamp(frac * 0.55 + tectonic.fault * 0.85 + tectonic.arc * 0.2, 0.0, 1.0);

        // Karst needs soluble rock AND enough water to dissolve it.
        double soluble = out.rock.soluble ? 1.0 : 0.25;
        double wet = Interp.smoothstep(350.0, 1100.0, precipitation) * (1.0 - Interp.smoothstep(0.4, 2.0, aridity));
        out.karst = Interp.clamp(p.karstStrength * soluble * (0.25 + 0.75 * wet) * (0.6 + 0.4 * frac), 0.0, 1.0);

        // ------------------------------------------------------------- soils
        double soilJitter = 0.75 + 0.5 * soilNoise.perlin2(x / 90.0, z / 90.0);
        out.soil = classifySoil(out.rock, temperature, aridity, precipitation, glacial,
                riverChannel, depositional, continentalness);

        // Thickness: climate and parent-material weatherability build it, slope strips it.
        double build = p.soilBaseBlocks * (0.35 + out.rock.weatherability)
                * Interp.smoothstep(150.0, 900.0, precipitation)
                * (0.55 + 0.45 * Interp.smoothstep(-6.0, 14.0, temperature))
                * soilJitter;
        double strip = 1.0 - Interp.smoothstep(8.0, 38.0, slopeDegrees);
        double valleyBonus = 1.0 + 2.2 * depositional + 1.4 * riverChannel;
        out.soilThickness = Interp.clamp(build * strip * valleyBonus * out.soil.thicknessGain, 0.0, 9.0);
        if (out.soil == SoilKind.LITHOSOL) {
            out.soilThickness = Math.min(out.soilThickness, 0.6);
        }

        // Regolith: chemically weathered rock below the soil. Deep in warm wet climates, absent on
        // fresh glacial or arid surfaces.
        out.regolith = Interp.clamp(out.soilThickness * 0.9
                + 3.0 * out.rock.weatherability * Interp.smoothstep(300.0, 1200.0, precipitation)
                * Interp.smoothstep(-2.0, 16.0, temperature) * (1.0 - 0.8 * glacial), 0.4, 14.0);

        // Bedrock outcrop: steep slopes, hard rock, thin soil.
        out.outcrop = Interp.clamp(Interp.smoothstep(24.0, 44.0, slopeDegrees)
                * (0.45 + 0.55 * out.rock.primary.hardness)
                * (1.0 - Interp.smoothstep(0.4, 2.2, out.soilThickness))
                + Interp.smoothstep(52.0, 70.0, slopeDegrees), 0.0, 1.0);
        if (out.soil == SoilKind.ALLUVIUM || out.soil == SoilKind.DUNE) {
            out.outcrop *= 0.15;
        }
    }

    private Rock classifyRock(double continentalness, Tectonics.Setting t, double prov, double bas,
                              double temperature, double aridity, double glacial) {
        // Oceanic crust.
        if (continentalness < -0.05) {
            if (t.rift > 0.45) {
                return Rock.MAFIC; // fresh mid-ocean ridge basalt
            }
            return prov > 0.22 ? Rock.COVER : Rock.MAFIC; // abyssal pelagic cover over old crust
        }

        // Volcanic arc / hotspot edifice wins everywhere it is active.
        if (t.hotspot > 0.28) {
            return Rock.VOLCANIC;
        }
        if (t.arc > 0.55 && prov > -0.15) {
            return bas > 0.1 ? Rock.MAFIC : Rock.VOLCANIC;
        }

        // Glaciated terrain is mantled in drift regardless of what is underneath.
        if (glacial > 0.35 && prov < 0.35) {
            return Rock.DRIFT;
        }

        // Arid interior basins: soft, bedded, brightly coloured sediment. This is the rock that makes
        // badlands at all — a granite desert pavements over, a sandstone-and-shale desert dissects into
        // painted gullies. Getting the lithology right is what separates Borrego from a grey plain.
        if (aridity > 1.15 && continentalness > 0.05) {
            if (bas > 0.22) {
                return Rock.EVAPORITE;
            }
            if (bas > -0.18) {
                return Rock.CLASTIC;
            }
            return prov > 0.0 ? Rock.SHALE : Rock.CLASTIC;
        }

        // Deep craton: old, hard, low-relief granite and gneiss.
        double craton = Interp.smoothstep(0.18, 0.62, continentalness) * (1.0 - t.arc) * (1.0 - t.boundary * 0.6);
        if (craton > 0.42) {
            return prov > 0.05 ? Rock.GRANITIC : Rock.GNEISSIC;
        }

        // Foreland / platform basins: carbonates and clastics alternate on the basin noise.
        if (continentalness > -0.02) {
            if (bas > 0.30) {
                return Rock.CARBONATE;
            }
            if (bas > -0.05) {
                return temperature > 12.0 && aridity < 1.1 ? Rock.CARBONATE : Rock.CLASTIC;
            }
            if (bas > -0.38) {
                return Rock.CLASTIC;
            }
            return prov > 0.1 ? Rock.SHALE : Rock.CLASTIC;
        }

        // Margin: mixed.
        return bas > 0.0 ? Rock.CLASTIC : Rock.MAFIC;
    }

    private SoilKind classifySoil(Rock rock, double temperature, double aridity, double precipitation,
                                  double glacial, double riverChannel, double depositional,
                                  double continentalness) {
        if (riverChannel > 0.35 || depositional > 0.55) {
            return SoilKind.ALLUVIUM;
        }
        if (glacial > 0.45) {
            return SoilKind.TILL;
        }
        if (aridity > Climate.AI_ARID) {
            // Playa floors in endorheic basins are saline; the rest is aridisol or dune sand.
            if (depositional > 0.25 && continentalness > 0.05 && precipitation < 260.0) {
                return SoilKind.SOLONCHAK;
            }
            return precipitation < 160.0 ? SoilKind.DUNE : SoilKind.ARIDISOL;
        }
        if (depositional > 0.30 && precipitation > 700.0 && temperature > 4.0) {
            return SoilKind.HISTOSOL;
        }
        if (rock == Rock.VOLCANIC || rock == Rock.MAFIC && precipitation > 900.0) {
            return SoilKind.ANDOSOL;
        }
        if (temperature < -3.0) {
            return SoilKind.GELISOL;
        }
        if (temperature < 6.0) {
            return SoilKind.PODZOL;
        }
        if (temperature > 17.0 && precipitation > 1300.0) {
            return SoilKind.LATERITE;
        }
        if (precipitation < 480.0 && aridity > Climate.AI_SEMIARID) {
            return SoilKind.LOESS;
        }
        return SoilKind.BROWN_EARTH;
    }

    // ------------------------------------------------------------- stratigraphy

    /**
     * Bedrock material at a point, honouring bedding and the deepslate transition.
     *
     * @param depthBelowSurface how far below the pre-erosion surface, in blocks
     */
    public Material bedrockAt(Site site, int x, int y, int z, double depthBelowSurface, int deepslateLevel) {
        Material m = site.rock.primary;
        if (site.rock.bedded()) {
            // Position along the tilted bedding normal.
            double u = x * Math.cos(site.strike) + z * Math.sin(site.strike) + y * Math.tan(site.dip);
            // Fold limbs interleave the subordinate lithology.
            double band = u / Math.max(1.5, site.strataSpacing);
            double frac = band - Math.floor(band);
            int layer = (int) Math.floor(band);
            boolean subordinate = (layer & 1) == 1;
            // Thin partings every few beds read as real stratification on a cliff face.
            boolean parting = frac < 0.10 || frac > 0.94;
            if (subordinate || parting) {
                m = site.rock.secondary;
            }
        }
        if (y < deepslateLevel - 1 && m.cat == Material.Cat.ROCK && m != Material.DEEPSLATE) {
            double fade = Interp.smoothstep(deepslateLevel + 4, deepslateLevel - 22, y);
            if (fade > 0.55) {
                m = Material.DEEPSLATE;
            } else if (fade > 0.2 && (Hash.hash(991, x, y, z) & 3) == 0) {
                m = Material.DEEPSLATE;
            }
        }
        return m;
    }

    // ------------------------------------------------------------- metallogeny

    /**
     * Ore substitution. Returns {@code null} when the host rock stays as it is.
     *
     * @param depthBelowSurface blocks below the (eroded) surface; controls the vertical zonation
     *                          that real deposits show
     * @param nearChannel       0..1 proximity to an active channel, for placer deposits
     */
    public Material oreAt(Site site, int x, int y, int z, double depthBelowSurface, double nearChannel,
                          boolean deepslate) {
        if (!p.geologicalOres) {
            return null;
        }
        long h = Hash.hash(0x0BE5L ^ 0x5EEDL, x, y, z);
        double r = Hash.unit(h);
        double r2 = Hash.unit(Hash.mix64(h ^ 0x9E37L));
        Rock.OreSet set = site.rock.ores;

        // Veins are clustered, not per-block: gate on a coarse 3-D hash so ores arrive in shoots.
        int vx = x >> 2;
        int vy = y >> 2;
        int vz = z >> 2;
        double vein = Hash.unit(Hash.hash(0xE1E1L, vx, vy, vz));
        if (vein > 0.10) {
            // Outside a vein nucleus only the rarest, most dispersed deposits appear.
            r *= 0.12;
        }

        Material m = pickOre(set, r, r2, y, depthBelowSurface, site, nearChannel, vein);
        if (m == null) {
            return null;
        }
        return deepslate ? deepslateVariant(m) : m;
    }

    private static Material pickOre(Rock.OreSet set, double r, double r2, int y,
                                    double depth, Site site, double nearChannel, double vein) {
        switch (set) {
            case SHALE:
                // Coal measures: thick, shallow, laterally persistent seams.
                if (depth < 90 && y > -24 && r < 0.085 * veinGain(vein)) {
                    return Material.COAL_ORE;
                }
                if (depth < 140 && r2 < 0.014) {
                    return Material.IRON_ORE; // siderite bands
                }
                return null;
            case CLASTIC:
                if (depth < 70 && r < 0.030) {
                    return Material.COAL_ORE;
                }
                if (depth < 120 && r2 < 0.026) {
                    return Material.IRON_ORE;
                }
                if (nearChannel > 0.5 && r < 0.006) {
                    return Material.GOLD_ORE; // placer
                }
                return null;
            case CARBONATE:
                // Mississippi Valley type: redstone and iron in dissolution cavities.
                if (site.karst > 0.4 && depth < 110 && r < 0.034) {
                    return Material.REDSTONE_ORE;
                }
                if (depth < 150 && r2 < 0.020) {
                    return Material.IRON_ORE;
                }
                if (depth < 90 && r > 0.985) {
                    return Material.LAPIS_ORE;
                }
                return null;
            case MAFIC:
            case VOLCANIC:
                // VMS / magmatic sulphides: copper and iron, concentrated near the top of the intrusion.
                if (depth < 120 && r < 0.052 * veinGain(vein)) {
                    return Material.COPPER_ORE;
                }
                if (depth < 160 && r2 < 0.048) {
                    return Material.IRON_ORE;
                }
                if (depth < 60 && r > 0.9955) {
                    return Material.GOLD_ORE;
                }
                return null;
            case GRANITIC:
                // Porphyry/greisen: copper-moly analogue, gold veins, tin->copper, emerald in contacts.
                if (depth < 170 && r < 0.046 * veinGain(vein)) {
                    return Material.COPPER_ORE;
                }
                if (depth < 130 && r2 < 0.022) {
                    return Material.GOLD_ORE;
                }
                if (depth < 60 && r > 0.992) {
                    return Material.EMERALD_ORE;
                }
                if (depth < 200 && r2 > 0.996) {
                    return Material.RAW_COPPER_BLOCK;
                }
                return null;
            case METAMORPHIC:
                if (depth < 150 && r < 0.030) {
                    return Material.IRON_ORE;
                }
                if (depth < 90 && r2 < 0.014) {
                    return Material.GOLD_ORE;
                }
                if (depth < 70 && r > 0.994) {
                    return Material.EMERALD_ORE;
                }
                return null;
            case ULTRAMAFIC:
                if (depth > 60 && y < -10 && r < 0.055) {
                    return Material.IRON_ORE;
                }
                if (depth > 110 && y < -30 && r2 > 0.9972) {
                    return Material.DIAMOND_ORE; // kimberlite pipe
                }
                if (y < -45 && r > 0.9993) {
                    return Material.ANCIENT_DEBRIS;
                }
                return null;
            case EVAPORITE:
                if (depth < 80 && r < 0.020) {
                    return Material.LAPIS_ORE;
                }
                if (nearChannel > 0.4 && r2 < 0.004) {
                    return Material.GOLD_ORE;
                }
                return null;
            case COVER:
                // Placers and reworked material: gold and iron in gravels, mostly near water.
                if (nearChannel > 0.35 && r < 0.012) {
                    return Material.GOLD_ORE;
                }
                if (depth < 40 && r2 < 0.030) {
                    return Material.IRON_ORE;
                }
                return null;
            case DEEP:
            default:
                if (depth < 200 && r < 0.026) {
                    return Material.IRON_ORE;
                }
                if (y < -20 && r2 < 0.016) {
                    return Material.COPPER_ORE;
                }
                return null;
        }
    }

    private static double veinGain(double vein) {
        return 0.35 + vein * 2.2;
    }

    private static Material deepslateVariant(Material m) {
        switch (m) {
            case COAL_ORE: return Material.DEEPSLATE_COAL_ORE;
            case IRON_ORE: return Material.DEEPSLATE_IRON_ORE;
            case COPPER_ORE: return Material.DEEPSLATE_COPPER_ORE;
            case GOLD_ORE: return Material.DEEPSLATE_GOLD_ORE;
            case REDSTONE_ORE: return Material.DEEPSLATE_REDSTONE_ORE;
            case LAPIS_ORE: return Material.DEEPSLATE_LAPIS_ORE;
            case DIAMOND_ORE: return Material.DEEPSLATE_DIAMOND_ORE;
            case EMERALD_ORE: return Material.DEEPSLATE_EMERALD_ORE;
            default: return m;
        }
    }
}
