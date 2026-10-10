package dev.terrarealis.kernel;

import dev.terrarealis.kernel.math.Hash;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every tunable knob of the generator, with physically motivated defaults.
 *
 * <p><b>Scale model.</b> Minecraft cannot represent real topography at 1:1 — the whole build height is
 * 384 blocks while sea floor to summit on Earth spans ~20 km. Terra Realis therefore declares an
 * explicit map scale, {@link #metersPerBlock}, and derives every physical quantity (lapse rate, talus
 * angle, catchment area, wind fetch, dune wavelength) from it. Defaults give:
 * <pre>
 *   1 block     = 12 m            (1 chunk = 192 m; a 1024x1024 preview = 12.3 km across)
 *   sea level   = y 63            = 0 m a.s.l.
 *   build limit = y 320           = +3 084 m a.s.l.  (alpine and nival zones are reachable)
 *   world floor = y -64           = -1 524 m         (shelf and abyssal plain, compressed)
 * </pre>
 * Ocean depth is deliberately compressed relative to the vertical scale; a true-to-scale abyssal plain
 * would be 300 blocks deep and swallow the world height. Everything that depends on a <i>ratio</i>
 * (slope, aspect, rain shadow, snow line, talus) stays consistent because horizontal and vertical use
 * the same number.
 *
 * <p><b>Design target.</b> The defaults are tuned for the "arid-dramatic" look: Basin and Range
 * fault-block mountains rising out of bajadas, dissected badlands with slot canyons, playa basins,
 * dune fields, volcanic cones and lava flows, and sky islands where a desert floor climbs to pine
 * forest within a few kilometres. That is Anza-Borrego, the Sonoran, and the Death Stranding palette —
 * while the humid half of the climate model still produces the soft green valleys and big lakes of a
 * Mushoku Tensei landscape.
 */
public final class GenParams {

    // ------------------------------------------------------------------ world
    public double metersPerBlock = 12.0;
    public int seaLevel = 63;
    public int minY = -64;
    public int maxY = 320;
    public int deepslateLevel = 0;
    public int bedrockLayers = 5;
    public int lavaLevel = -54;

    // ------------------------------------------------------------ continents
    public double continentScale = 5200.0;
    public int continentOctaves = 5;
    public double continentWarp = 0.65;
    /** Target fraction of the map below sea level. Earth is 0.71; gameplay wants less. */
    public double oceanFraction = 0.40;
    public double reliefGain = 1.0;
    /** Highest peak above sea level, in blocks (~2 900 m at the default scale). */
    public double maxRelief = 210.0;
    /** Cratonic relief budget in blocks: how high continentalness alone can lift the land. */
    public double landRelief = 62.0;
    public double maxDepth = 58.0;
    public double shelfSharpness = 1.0;

    // ------------------------------------------------------------- tectonics
    public double plateScale = 3000.0;
    public double orogenyGain = 1.0;
    public double faultiness = 1.0;
    public double volcanoSpacing = 16000.0;
    /** Wavelength of the "extended province" field: how big a Basin and Range region is. */
    public double extensionProvinceScale = 13000.0;
    /** -1..1; positive makes extensional provinces cover more of the map. */
    public double extensionBias = 0.12;

    // ------------------------------------------------- basin & range / sky islands
    /** Distance between adjacent fault-block ranges, blocks (~17 km). */
    public double basinRangeScale = 1450.0;
    /** Relief of one tilted fault block, blocks (~1 300 m — the Santa Catalinas are ~1 800 m). */
    public double basinRangeAmplitude = 150.0;
    /** 0..1 — how asymmetric the blocks are: 0 symmetric ridges, 1 fully tilted fault blocks. */
    public double blockTilt = 0.72;

    // ---------------------------------------------------------------- relief
    public double mountainScale = 620.0;
    public int mountainOctaves = 5;
    public double hillScale = 150.0;
    public int hillOctaves = 4;
    public double detailScale = 26.0;
    public int detailOctaves = 3;
    /** Roughness added after erosion, scaled by local slope. */
    public double microRelief = 3.0;

    // --------------------------------------------------------------- erosion
    public int erosionCellSize = 2;
    public int tileCells = 64;
    public int haloCells = 48;
    public int hydraulicIterations = 6;
    public int thermalIterations = 4;
    public double fluvialK = 0.60;
    public double fluvialAreaExp = 0.5;
    public double fluvialSlopeExp = 1.0;
    public double depositionK = 0.30;
    public double talusAngleDeg = 34.0;
    public double sedimentTalusDeg = 27.0;
    public int riverThreshold = 900;
    public int majorRiverThreshold = 4000;
    public int lakeMinCells = 5;
    /** 0..1 — how much denudation is returned as Airy isostatic rebound. */
    public double isostasyGain = 0.24;

    // --------------------------------------------------------------- climate
    public double blocksPerDegreeLatitude = 8000.0;
    public double latitudeAtZ0 = 0.0;
    public double equatorTempC = 27.0;
    public double lapseRateCPerKm = 6.5;
    public double basePrecipMm = 2200.0;
    public double orographicK = 0.62;
    public double continentalityScale = 4200.0;
    public double windNoiseScale = 7000.0;
    public double polarLatitude = 66.0;
    public double glacialLatitude = 55.0;
    /** Wobble of the orographic march, so rain shadows bend instead of striping. */
    public double precipNoiseScale = 3100.0;

    // --------------------------------------------------------------- geology
    public double provinceScale = 3400.0;
    public double structuralScale = 1100.0;
    public double maxDipDeg = 16.0;
    public double strataSpacing = 6.0;
    public double soilBaseBlocks = 3.4;
    public boolean geologicalOres = true;
    public double karstStrength = 1.0;
    public double caveDensity = 1.0;
    public int caveCellSize = 4;
    /** Painted strata: how strongly badlands alternate between coloured beds. */
    public double strataContrast = 1.0;

    // ----------------------------------------------------- arid landforms
    public boolean badlands = true;
    public double badlandGain = 1.0;
    public boolean alluvialFans = true;
    public double fanGain = 1.0;
    public boolean dunes = true;
    /** Transverse dune spacing, blocks (~550 m — large dune fields run 100-500 m). */
    public double duneWavelength = 46.0;
    public double duneHeight = 8.0;
    public boolean inselbergs = true;
    public double inselbergSpacing = 5200.0;
    public double inselbergRadius = 150.0;
    public double inselbergHeight = 130.0;
    public boolean playaCrust = true;
    /** Green riparian corridor along desert watercourses: width in blocks. */
    public boolean riparianCorridors = true;
    public double riparianWidth = 30.0;

    // ------------------------------------------------------------- volcanism
    public boolean volcanism = true;
    public double stratovolcanoHeight = 205.0;
    public double stratovolcanoRadius = 340.0;
    public double cinderConeSpacing = 2900.0;
    public double cinderConeHeight = 30.0;
    public boolean lavaFlows = true;
    public double lavaFlowLength = 70.0;
    public int lavaLevelVent = 6;

    // ---------------------------------------------------------------- water
    public boolean wetlands = true;
    public boolean deltas = true;
    public boolean meanders = true;
    public boolean glaciation = true;
    public double beachGain = 1.0;

    // ------------------------------------------------------------ performance
    public int tileCacheSize = 128;
    public boolean diskCache = true;

    public static GenParams defaults() {
        return new GenParams();
    }

    public GenParams copy() {
        GenParams p = new GenParams();
        p.metersPerBlock = metersPerBlock;
        p.seaLevel = seaLevel;
        p.minY = minY;
        p.maxY = maxY;
        p.deepslateLevel = deepslateLevel;
        p.bedrockLayers = bedrockLayers;
        p.lavaLevel = lavaLevel;
        p.continentScale = continentScale;
        p.continentOctaves = continentOctaves;
        p.continentWarp = continentWarp;
        p.oceanFraction = oceanFraction;
        p.reliefGain = reliefGain;
        p.maxRelief = maxRelief;
        p.landRelief = landRelief;
        p.maxDepth = maxDepth;
        p.shelfSharpness = shelfSharpness;
        p.plateScale = plateScale;
        p.orogenyGain = orogenyGain;
        p.faultiness = faultiness;
        p.volcanoSpacing = volcanoSpacing;
        p.extensionProvinceScale = extensionProvinceScale;
        p.extensionBias = extensionBias;
        p.basinRangeScale = basinRangeScale;
        p.basinRangeAmplitude = basinRangeAmplitude;
        p.blockTilt = blockTilt;
        p.mountainScale = mountainScale;
        p.mountainOctaves = mountainOctaves;
        p.hillScale = hillScale;
        p.hillOctaves = hillOctaves;
        p.detailScale = detailScale;
        p.detailOctaves = detailOctaves;
        p.microRelief = microRelief;
        p.erosionCellSize = erosionCellSize;
        p.tileCells = tileCells;
        p.haloCells = haloCells;
        p.hydraulicIterations = hydraulicIterations;
        p.thermalIterations = thermalIterations;
        p.fluvialK = fluvialK;
        p.fluvialAreaExp = fluvialAreaExp;
        p.fluvialSlopeExp = fluvialSlopeExp;
        p.depositionK = depositionK;
        p.talusAngleDeg = talusAngleDeg;
        p.sedimentTalusDeg = sedimentTalusDeg;
        p.riverThreshold = riverThreshold;
        p.majorRiverThreshold = majorRiverThreshold;
        p.lakeMinCells = lakeMinCells;
        p.isostasyGain = isostasyGain;
        p.blocksPerDegreeLatitude = blocksPerDegreeLatitude;
        p.latitudeAtZ0 = latitudeAtZ0;
        p.equatorTempC = equatorTempC;
        p.lapseRateCPerKm = lapseRateCPerKm;
        p.basePrecipMm = basePrecipMm;
        p.orographicK = orographicK;
        p.continentalityScale = continentalityScale;
        p.windNoiseScale = windNoiseScale;
        p.polarLatitude = polarLatitude;
        p.glacialLatitude = glacialLatitude;
        p.precipNoiseScale = precipNoiseScale;
        p.provinceScale = provinceScale;
        p.structuralScale = structuralScale;
        p.maxDipDeg = maxDipDeg;
        p.strataSpacing = strataSpacing;
        p.soilBaseBlocks = soilBaseBlocks;
        p.geologicalOres = geologicalOres;
        p.karstStrength = karstStrength;
        p.caveDensity = caveDensity;
        p.caveCellSize = caveCellSize;
        p.strataContrast = strataContrast;
        p.badlands = badlands;
        p.badlandGain = badlandGain;
        p.alluvialFans = alluvialFans;
        p.fanGain = fanGain;
        p.dunes = dunes;
        p.duneWavelength = duneWavelength;
        p.duneHeight = duneHeight;
        p.inselbergs = inselbergs;
        p.inselbergSpacing = inselbergSpacing;
        p.inselbergRadius = inselbergRadius;
        p.inselbergHeight = inselbergHeight;
        p.playaCrust = playaCrust;
        p.riparianCorridors = riparianCorridors;
        p.riparianWidth = riparianWidth;
        p.volcanism = volcanism;
        p.stratovolcanoHeight = stratovolcanoHeight;
        p.stratovolcanoRadius = stratovolcanoRadius;
        p.cinderConeSpacing = cinderConeSpacing;
        p.cinderConeHeight = cinderConeHeight;
        p.lavaFlows = lavaFlows;
        p.lavaFlowLength = lavaFlowLength;
        p.lavaLevelVent = lavaLevelVent;
        p.wetlands = wetlands;
        p.deltas = deltas;
        p.meanders = meanders;
        p.glaciation = glaciation;
        p.beachGain = beachGain;
        p.tileCacheSize = tileCacheSize;
        p.diskCache = diskCache;
        return p;
    }

    // ------------------------------------------------------------------- JSON

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("world", section(
                "meters_per_block", metersPerBlock, "sea_level", seaLevel, "min_y", minY,
                "max_y", maxY, "deepslate_level", deepslateLevel, "bedrock_layers", bedrockLayers,
                "lava_level", lavaLevel));
        m.put("continents", section(
                "scale", continentScale, "octaves", continentOctaves, "warp", continentWarp,
                "ocean_fraction", oceanFraction, "relief_gain", reliefGain, "max_relief", maxRelief,
                "land_relief", landRelief,
                "max_depth", maxDepth, "shelf_sharpness", shelfSharpness));
        m.put("tectonics", section(
                "plate_scale", plateScale, "orogeny_gain", orogenyGain, "faultiness", faultiness,
                "volcano_spacing", volcanoSpacing, "extension_province_scale", extensionProvinceScale,
                "extension_bias", extensionBias));
        m.put("basin_and_range", section(
                "scale", basinRangeScale, "amplitude", basinRangeAmplitude, "block_tilt", blockTilt));
        m.put("relief", section(
                "mountain_scale", mountainScale, "mountain_octaves", mountainOctaves,
                "hill_scale", hillScale, "hill_octaves", hillOctaves, "detail_scale", detailScale,
                "detail_octaves", detailOctaves, "micro_relief", microRelief));
        m.put("erosion", section(
                "cell_size", erosionCellSize, "tile_cells", tileCells, "halo_cells", haloCells,
                "hydraulic_iterations", hydraulicIterations, "thermal_iterations", thermalIterations,
                "fluvial_k", fluvialK, "fluvial_area_exp", fluvialAreaExp,
                "fluvial_slope_exp", fluvialSlopeExp, "deposition_k", depositionK,
                "talus_angle_deg", talusAngleDeg, "sediment_talus_deg", sedimentTalusDeg,
                "river_threshold", riverThreshold, "major_river_threshold", majorRiverThreshold,
                "lake_min_cells", lakeMinCells, "isostasy_gain", isostasyGain));
        m.put("climate", section(
                "blocks_per_degree_latitude", blocksPerDegreeLatitude, "latitude_at_z0", latitudeAtZ0,
                "equator_temp_c", equatorTempC, "lapse_rate_c_per_km", lapseRateCPerKm,
                "base_precip_mm", basePrecipMm, "orographic_k", orographicK,
                "continentality_scale", continentalityScale, "wind_noise_scale", windNoiseScale,
                "polar_latitude", polarLatitude, "glacial_latitude", glacialLatitude,
                "precip_noise_scale", precipNoiseScale));
        m.put("geology", section(
                "province_scale", provinceScale, "structural_scale", structuralScale,
                "max_dip_deg", maxDipDeg, "strata_spacing", strataSpacing,
                "soil_base_blocks", soilBaseBlocks, "geological_ores", geologicalOres,
                "karst_strength", karstStrength, "cave_density", caveDensity,
                "cave_cell_size", caveCellSize, "strata_contrast", strataContrast));
        m.put("arid", section(
                "badlands", badlands, "badland_gain", badlandGain, "alluvial_fans", alluvialFans,
                "fan_gain", fanGain, "dunes", dunes, "dune_wavelength", duneWavelength,
                "dune_height", duneHeight, "inselbergs", inselbergs,
                "inselberg_spacing", inselbergSpacing, "inselberg_radius", inselbergRadius,
                "inselberg_height", inselbergHeight, "playa_crust", playaCrust,
                "riparian_corridors", riparianCorridors, "riparian_width", riparianWidth));
        m.put("volcanism", section(
                "enabled", volcanism, "stratovolcano_height", stratovolcanoHeight,
                "stratovolcano_radius", stratovolcanoRadius, "cinder_cone_spacing", cinderConeSpacing,
                "cinder_cone_height", cinderConeHeight, "lava_flows", lavaFlows,
                "lava_flow_length", lavaFlowLength, "lava_level_vent", lavaLevelVent));
        m.put("water", section(
                "wetlands", wetlands, "deltas", deltas, "meanders", meanders,
                "glaciation", glaciation, "beach_gain", beachGain));
        m.put("performance", section(
                "tile_cache_size", tileCacheSize, "disk_cache", diskCache));
        return m;
    }

    /** Tiny varargs helper: key, value, key, value, ... */
    private static Map<String, Object> section(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    public static GenParams fromJson(String text) {
        GenParams p = new GenParams();
        p.apply(MiniJson.parseObject(text));
        return p;
    }

    @SuppressWarnings("unchecked")
    public void apply(Map<String, Object> root) {
        if (root == null) {
            return;
        }
        Map<String, Object> s;
        if ((s = obj(root, "world")) != null) {
            metersPerBlock = d(s, "meters_per_block", metersPerBlock);
            seaLevel = i(s, "sea_level", seaLevel);
            minY = i(s, "min_y", minY);
            maxY = i(s, "max_y", maxY);
            deepslateLevel = i(s, "deepslate_level", deepslateLevel);
            bedrockLayers = i(s, "bedrock_layers", bedrockLayers);
            lavaLevel = i(s, "lava_level", lavaLevel);
        }
        if ((s = obj(root, "continents")) != null) {
            continentScale = d(s, "scale", continentScale);
            continentOctaves = i(s, "octaves", continentOctaves);
            continentWarp = d(s, "warp", continentWarp);
            oceanFraction = d(s, "ocean_fraction", oceanFraction);
            reliefGain = d(s, "relief_gain", reliefGain);
            maxRelief = d(s, "max_relief", maxRelief);
            landRelief = d(s, "land_relief", landRelief);
            maxDepth = d(s, "max_depth", maxDepth);
            shelfSharpness = d(s, "shelf_sharpness", shelfSharpness);
        }
        if ((s = obj(root, "tectonics")) != null) {
            plateScale = d(s, "plate_scale", plateScale);
            orogenyGain = d(s, "orogeny_gain", orogenyGain);
            faultiness = d(s, "faultiness", faultiness);
            volcanoSpacing = d(s, "volcano_spacing", volcanoSpacing);
            extensionProvinceScale = d(s, "extension_province_scale", extensionProvinceScale);
            extensionBias = d(s, "extension_bias", extensionBias);
        }
        if ((s = obj(root, "basin_and_range")) != null) {
            basinRangeScale = d(s, "scale", basinRangeScale);
            basinRangeAmplitude = d(s, "amplitude", basinRangeAmplitude);
            blockTilt = d(s, "block_tilt", blockTilt);
        }
        if ((s = obj(root, "relief")) != null) {
            mountainScale = d(s, "mountain_scale", mountainScale);
            mountainOctaves = i(s, "mountain_octaves", mountainOctaves);
            hillScale = d(s, "hill_scale", hillScale);
            hillOctaves = i(s, "hill_octaves", hillOctaves);
            detailScale = d(s, "detail_scale", detailScale);
            detailOctaves = i(s, "detail_octaves", detailOctaves);
            microRelief = d(s, "micro_relief", microRelief);
        }
        if ((s = obj(root, "erosion")) != null) {
            erosionCellSize = i(s, "cell_size", erosionCellSize);
            tileCells = i(s, "tile_cells", tileCells);
            haloCells = i(s, "halo_cells", haloCells);
            hydraulicIterations = i(s, "hydraulic_iterations", hydraulicIterations);
            thermalIterations = i(s, "thermal_iterations", thermalIterations);
            fluvialK = d(s, "fluvial_k", fluvialK);
            fluvialAreaExp = d(s, "fluvial_area_exp", fluvialAreaExp);
            fluvialSlopeExp = d(s, "fluvial_slope_exp", fluvialSlopeExp);
            depositionK = d(s, "deposition_k", depositionK);
            talusAngleDeg = d(s, "talus_angle_deg", talusAngleDeg);
            sedimentTalusDeg = d(s, "sediment_talus_deg", sedimentTalusDeg);
            riverThreshold = i(s, "river_threshold", riverThreshold);
            majorRiverThreshold = i(s, "major_river_threshold", majorRiverThreshold);
            lakeMinCells = i(s, "lake_min_cells", lakeMinCells);
            isostasyGain = d(s, "isostasy_gain", isostasyGain);
        }
        if ((s = obj(root, "climate")) != null) {
            blocksPerDegreeLatitude = d(s, "blocks_per_degree_latitude", blocksPerDegreeLatitude);
            latitudeAtZ0 = d(s, "latitude_at_z0", latitudeAtZ0);
            equatorTempC = d(s, "equator_temp_c", equatorTempC);
            lapseRateCPerKm = d(s, "lapse_rate_c_per_km", lapseRateCPerKm);
            basePrecipMm = d(s, "base_precip_mm", basePrecipMm);
            orographicK = d(s, "orographic_k", orographicK);
            continentalityScale = d(s, "continentality_scale", continentalityScale);
            windNoiseScale = d(s, "wind_noise_scale", windNoiseScale);
            polarLatitude = d(s, "polar_latitude", polarLatitude);
            glacialLatitude = d(s, "glacial_latitude", glacialLatitude);
            precipNoiseScale = d(s, "precip_noise_scale", precipNoiseScale);
        }
        if ((s = obj(root, "geology")) != null) {
            provinceScale = d(s, "province_scale", provinceScale);
            structuralScale = d(s, "structural_scale", structuralScale);
            maxDipDeg = d(s, "max_dip_deg", maxDipDeg);
            strataSpacing = d(s, "strata_spacing", strataSpacing);
            soilBaseBlocks = d(s, "soil_base_blocks", soilBaseBlocks);
            geologicalOres = b(s, "geological_ores", geologicalOres);
            karstStrength = d(s, "karst_strength", karstStrength);
            caveDensity = d(s, "cave_density", caveDensity);
            caveCellSize = i(s, "cave_cell_size", caveCellSize);
            strataContrast = d(s, "strata_contrast", strataContrast);
        }
        if ((s = obj(root, "arid")) != null) {
            badlands = b(s, "badlands", badlands);
            badlandGain = d(s, "badland_gain", badlandGain);
            alluvialFans = b(s, "alluvial_fans", alluvialFans);
            fanGain = d(s, "fan_gain", fanGain);
            dunes = b(s, "dunes", dunes);
            duneWavelength = d(s, "dune_wavelength", duneWavelength);
            duneHeight = d(s, "dune_height", duneHeight);
            inselbergs = b(s, "inselbergs", inselbergs);
            inselbergSpacing = d(s, "inselberg_spacing", inselbergSpacing);
            inselbergRadius = d(s, "inselberg_radius", inselbergRadius);
            inselbergHeight = d(s, "inselberg_height", inselbergHeight);
            playaCrust = b(s, "playa_crust", playaCrust);
            riparianCorridors = b(s, "riparian_corridors", riparianCorridors);
            riparianWidth = d(s, "riparian_width", riparianWidth);
        }
        if ((s = obj(root, "volcanism")) != null) {
            volcanism = b(s, "enabled", volcanism);
            stratovolcanoHeight = d(s, "stratovolcano_height", stratovolcanoHeight);
            stratovolcanoRadius = d(s, "stratovolcano_radius", stratovolcanoRadius);
            cinderConeSpacing = d(s, "cinder_cone_spacing", cinderConeSpacing);
            cinderConeHeight = d(s, "cinder_cone_height", cinderConeHeight);
            lavaFlows = b(s, "lava_flows", lavaFlows);
            lavaFlowLength = d(s, "lava_flow_length", lavaFlowLength);
            lavaLevelVent = i(s, "lava_level_vent", lavaLevelVent);
        }
        if ((s = obj(root, "water")) != null) {
            wetlands = b(s, "wetlands", wetlands);
            deltas = b(s, "deltas", deltas);
            meanders = b(s, "meanders", meanders);
            glaciation = b(s, "glaciation", glaciation);
            beachGain = d(s, "beach_gain", beachGain);
        }
        if ((s = obj(root, "performance")) != null) {
            tileCacheSize = i(s, "tile_cache_size", tileCacheSize);
            diskCache = b(s, "disk_cache", diskCache);
        }
        sanitise();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> obj(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    private static double d(Map<String, Object> m, String k, double fb) {
        return MiniJson.getDouble(m, k, fb);
    }

    private static int i(Map<String, Object> m, String k, int fb) {
        return MiniJson.getInt(m, k, fb);
    }

    private static boolean b(Map<String, Object> m, String k, boolean fb) {
        return MiniJson.getBool(m, k, fb);
    }

    /** Clamps user edits into ranges that cannot break the maths. */
    public void sanitise() {
        metersPerBlock = clampD(metersPerBlock, 0.25, 64.0);
        erosionCellSize = clampI(erosionCellSize, 1, 8);
        tileCells = clampI(tileCells, 16, 256);
        haloCells = clampI(haloCells, 8, 256);
        hydraulicIterations = clampI(hydraulicIterations, 0, 16);
        thermalIterations = clampI(thermalIterations, 0, 16);
        continentOctaves = clampI(continentOctaves, 1, 10);
        mountainOctaves = clampI(mountainOctaves, 1, 10);
        hillOctaves = clampI(hillOctaves, 1, 10);
        detailOctaves = clampI(detailOctaves, 1, 8);
        riverThreshold = Math.max(8, riverThreshold);
        majorRiverThreshold = Math.max(riverThreshold + 1, majorRiverThreshold);
        lakeMinCells = Math.max(1, lakeMinCells);
        caveCellSize = clampI(caveCellSize, 1, 16);
        oceanFraction = clampD(oceanFraction, 0.0, 0.95);
        talusAngleDeg = clampD(talusAngleDeg, 5.0, 80.0);
        sedimentTalusDeg = clampD(sedimentTalusDeg, 5.0, talusAngleDeg);
        blocksPerDegreeLatitude = Math.max(64.0, blocksPerDegreeLatitude);
        maxY = clampI(maxY, seaLevel + 16, 2032);
        minY = Math.min(seaLevel - 16, minY);
        bedrockLayers = clampI(bedrockLayers, 0, 16);
        basinRangeScale = clampD(basinRangeScale, 200.0, 20000.0);
        basinRangeAmplitude = clampD(basinRangeAmplitude, 0.0, maxRelief);
        landRelief = clampD(landRelief, 4.0, maxRelief);
        blockTilt = clampD(blockTilt, 0.0, 1.0);
        isostasyGain = clampD(isostasyGain, 0.0, 0.6);
        duneWavelength = clampD(duneWavelength, 8.0, 400.0);
        duneHeight = clampD(duneHeight, 0.0, 40.0);
        inselbergRadius = clampD(inselbergRadius, 16.0, 900.0);
        cinderConeSpacing = clampD(cinderConeSpacing, 400.0, 60000.0);
        lavaLevel = Math.max(minY + 2, lavaLevel);
        extensionBias = clampD(extensionBias, -1.0, 1.0);
    }

    private static int clampI(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static double clampD(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /** Stable fingerprint used to invalidate cached erosion tiles. */
    public long fingerprint() {
        long h = 0x243F6A8885A308D3L;
        h = mix(h, Double.doubleToLongBits(metersPerBlock));
        h = mix(h, seaLevel);
        h = mix(h, Double.doubleToLongBits(continentScale));
        h = mix(h, Double.doubleToLongBits(continentWarp));
        h = mix(h, Double.doubleToLongBits(oceanFraction));
        h = mix(h, Double.doubleToLongBits(maxRelief));
        h = mix(h, Double.doubleToLongBits(landRelief));
        h = mix(h, Double.doubleToLongBits(maxDepth));
        h = mix(h, Double.doubleToLongBits(plateScale));
        h = mix(h, Double.doubleToLongBits(orogenyGain));
        h = mix(h, Double.doubleToLongBits(extensionBias));
        h = mix(h, Double.doubleToLongBits(basinRangeScale));
        h = mix(h, Double.doubleToLongBits(basinRangeAmplitude));
        h = mix(h, Double.doubleToLongBits(blockTilt));
        h = mix(h, Double.doubleToLongBits(mountainScale));
        h = mix(h, Double.doubleToLongBits(hillScale));
        h = mix(h, erosionCellSize);
        h = mix(h, tileCells);
        h = mix(h, haloCells);
        h = mix(h, hydraulicIterations);
        h = mix(h, thermalIterations);
        h = mix(h, Double.doubleToLongBits(fluvialK));
        h = mix(h, Double.doubleToLongBits(depositionK));
        h = mix(h, Double.doubleToLongBits(talusAngleDeg));
        h = mix(h, riverThreshold);
        h = mix(h, Double.doubleToLongBits(isostasyGain));
        h = mix(h, Double.doubleToLongBits(blocksPerDegreeLatitude));
        h = mix(h, Double.doubleToLongBits(orographicK));
        h = mix(h, Double.doubleToLongBits(provinceScale));
        h = mix(h, Double.doubleToLongBits(caveDensity));
        h = mix(h, caveCellSize);
        h = mix(h, Double.doubleToLongBits(badlandGain));
        h = mix(h, Double.doubleToLongBits(fanGain));
        h = mix(h, Double.doubleToLongBits(duneWavelength));
        h = mix(h, Double.doubleToLongBits(duneHeight));
        h = mix(h, Double.doubleToLongBits(inselbergSpacing));
        h = mix(h, Double.doubleToLongBits(inselbergHeight));
        h = mix(h, Double.doubleToLongBits(stratovolcanoHeight));
        h = mix(h, Double.doubleToLongBits(cinderConeSpacing));
        h = mix(h, Double.doubleToLongBits(lavaFlowLength));
        return Hash.mix64(h);
    }

    private static long mix(long h, long v) {
        return Hash.mix64(h ^ (v * Hash.GOLDEN));
    }

    /** Interior blocks covered by one erosion tile. */
    public int tileBlocks() {
        return tileCells * erosionCellSize;
    }

    /** Full simulated width including both halos. */
    public int gridWidth() {
        return tileCells + 2 * haloCells;
    }
}
