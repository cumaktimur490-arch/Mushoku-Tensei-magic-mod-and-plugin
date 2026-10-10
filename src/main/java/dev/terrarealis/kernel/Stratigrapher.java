package dev.terrarealis.kernel;

import dev.terrarealis.kernel.geo.Geology;
import dev.terrarealis.kernel.math.Hash;
import dev.terrarealis.kernel.math.Interp;

/**
 * Turns a {@link Column} into a stack of {@link Material}s.
 *
 * <p>This is the layer that decides what the world is actually <i>made of</i>, and it is where the
 * difference between a heightmap mod and a geology mod shows. A heightmap mod puts grass on everything
 * above sea level. This puts the material that the rock, the climate, the landform and the position in
 * the stratigraphic column say belongs there:
 *
 * <pre>
 *   snow / ice
 *   ─────────────── surface
 *   soil horizon          (thickness from climate x parent material x slope)
 *   regolith              (chemically weathered parent rock)
 *   bedrock, bedded       (alternating lithologies on the dip of the fold)
 *   ore shoots            (zoned by depth and host rock)
 *   deepslate transition
 *   bedrock floor
 * </pre>
 *
 * <p>Overlaid on that stack are the surface states that override the soil entirely: playa crust, dune
 * sand, alluvial-fan gravel, lava-flow basalt, painted badland strata, desert pavement, and the green
 * riparian strip along a desert watercourse.
 */
public final class Stratigrapher {

    /** Asks whether a lattice point is void. Implementations: direct and interpolated. */
    public interface CaveSampler {
        boolean voidAt(int x, int y, int z, Column col);
    }

    /** No caves at all — used for heightmap previews and for cross-sections where voids are noise. */
    public static final CaveSampler NO_CAVES = (x, y, z, col) -> false;

    private Stratigrapher() {}

    /**
     * Decides the surface horizon: top block, second block, subsoil, snow cover and ice thickness.
     *
     * <p>The order of the tests matters and follows the real hierarchy of controls: an active volcanic
     * surface overrides everything, a water body overrides the soil, an arid landform overrides the
     * biome default, and only then does the soil taxon decide.
     */
    public static void decideSurface(Column col, GenParams p) {
        Material top = Material.GRASS_BLOCK;
        Material filler = Material.DIRT;
        Material sub = col.soil.subsoil;

        double arid = col.aridity;

        // ------------------------------------------------- volcanic surfaces
        if (col.volcanoVent > 0.55) {
            top = Material.MAGMA_BLOCK;
            filler = Material.OBSIDIAN;
            sub = Material.BASALT;
        } else if (col.lavaFlow > 0.30) {
            // Fresh flow crust over older, weathered flow. Columnar jointing shows as smooth basalt.
            double fresh = Interp.clamp(col.lavaFlow, 0.0, 1.0);
            top = fresh > 0.72 ? Material.BASALT : Material.SMOOTH_BASALT;
            filler = Material.BASALT;
            sub = Material.BLACKSTONE;
        } else if (col.volcanoFresh > 0.45) {
            top = Material.GRAVEL;
            filler = Material.TUFF;
            sub = Material.BASALT;
            if (col.biome == dev.terrarealis.kernel.biome.BiomeKind.VOLCANIC_SUMMIT) {
                top = Material.BASALT;
            }
        }

        // ---------------------------------------------------------- underwater
        if (col.underwater() && top == Material.GRASS_BLOCK) {
            double depth = col.waterDepth;
            if (col.marine) {
                if (depth > 26.0) {
                    top = col.rock == Rock.CARBONATE ? Material.CALCITE : Material.CLAY;
                    filler = Material.CLAY;
                    sub = Material.STONE;
                } else if (depth > 6.0) {
                    top = Material.SAND;
                    filler = Material.SANDSTONE;
                    sub = Material.STONE;
                } else {
                    top = Material.GRAVEL;
                    filler = Material.SAND;
                    sub = Material.SANDSTONE;
                }
                if (col.slopeDeg > 26.0 && depth < 30.0) {
                    top = col.rock.primary;
                    filler = col.rock.primary;
                }
            } else if (col.river || col.channel > 0.20) {
                top = col.slopeDeg > 20.0 ? col.rock.primary : Material.GRAVEL;
                filler = Material.GRAVEL;
                sub = Material.SANDSTONE;
            } else {
                // Lake bed: fine sediment, mud near the shore.
                top = depth < 2.5 ? Material.MUD : Material.CLAY;
                filler = Material.CLAY;
                sub = Material.PACKED_MUD;
            }
        }

        // ------------------------------------------------------------ playas
        if (col.playa > 0.35 && !col.underwater()) {
            // Evaporite crust over lake mud. Real playas are white, cracked and hard as concrete.
            top = Material.WHITE_TERRACOTTA;
            filler = Material.PACKED_MUD;
            sub = Material.CLAY;
        }

        // ------------------------------------------------------------- dunes
        if (col.duneHeight > 0.75 && col.sandSupply > 0.22 && !col.underwater()) {
            boolean red = col.rock == Rock.CLASTIC || col.rock == Rock.SHALE || arid > 0.72;
            top = red ? Material.RED_SAND : Material.SAND;
            filler = red ? Material.RED_SANDSTONE : Material.SANDSTONE;
            sub = filler;
        }

        // ---------------------------------------------------- alluvial fans
        if (col.fan > 0.42 && !col.underwater() && col.duneHeight < 0.75) {
            // Coarse, poorly sorted apron: gravel near the apex, sand at the toe.
            double coarse = Interp.clamp(col.fan * (0.4 + 0.6 * Interp.smoothstep(6.0, 22.0, col.slopeDeg)),
                    0.0, 1.0);
            top = coarse > 0.55 ? Material.GRAVEL : Material.SAND;
            filler = Material.GRAVEL;
            sub = Material.COBBLESTONE;
            if (arid > 0.6) {
                top = coarse > 0.55 ? Material.GRAVEL : Material.SAND;
                filler = Material.SANDSTONE;
            }
        }

        // --------------------------------------------------- deltas/floodplain
        if (col.delta > 0.30 && !col.underwater()) {
            top = Material.GRAVEL;
            filler = Material.SAND;
            sub = Material.MUD;
            if (col.saturation > 0.75) {
                top = Material.MUD;
                filler = Material.CLAY;
            }
        }

        // ------------------------------------------------- painted badlands
        if (col.painted > 0.30 && !col.underwater() && col.duneHeight < 0.75) {
            top = paintedMaterial(col, 0);
            filler = paintedMaterial(col, 1);
            sub = paintedMaterial(col, 3);
        }

        // -------------------------------------------------- desert pavement
        if (col.pavement > 0.55 && top == Material.GRASS_BLOCK) {
            top = col.boulders > 0.45 ? Material.COBBLESTONE : Material.GRAVEL;
            filler = Material.COARSE_DIRT;
            sub = Material.SANDSTONE;
        }

        // ------------------------------------------------------ soil default
        if (top == Material.GRASS_BLOCK) {
            switch (col.soil) {
                case LITHOSOL -> {
                    top = col.rock.primary;
                    filler = col.rock.secondary;
                    sub = col.rock.primary;
                }
                case RANKER -> {
                    top = Material.COARSE_DIRT;
                    filler = col.rock.primary;
                    sub = col.rock.primary;
                }
                case PODZOL -> {
                    top = Material.PODZOL;
                    filler = Material.DIRT;
                    sub = Material.COARSE_DIRT;
                }
                case LATERITE -> {
                    top = Material.GRASS_BLOCK;
                    filler = Material.RED_TERRACOTTA;
                    sub = Material.ORANGE_TERRACOTTA;
                }
                case ARIDISOL -> {
                    top = arid > 0.55 ? Material.SAND : Material.COARSE_DIRT;
                    filler = Material.SAND;
                    sub = Material.SMOOTH_SANDSTONE;
                }
                case SOLONCHAK -> {
                    top = Material.WHITE_TERRACOTTA;
                    filler = Material.PACKED_MUD;
                    sub = Material.CLAY;
                }
                case HISTOSOL -> {
                    top = Material.MOSS_BLOCK;
                    filler = Material.MUD;
                    sub = Material.PACKED_MUD;
                }
                case ANDOSOL -> {
                    top = Material.COARSE_DIRT;
                    filler = Material.ROOTED_DIRT;
                    sub = Material.TUFF;
                }
                case LOESS -> {
                    top = Material.GRASS_BLOCK;
                    filler = Material.DIRT;
                    sub = Material.PACKED_MUD;
                }
                case TILL -> {
                    top = col.boulders > 0.4 ? Material.COBBLESTONE : Material.GRAVEL;
                    filler = Material.GRAVEL;
                    sub = Material.COBBLESTONE;
                }
                case ALLUVIUM -> {
                    top = Material.GRAVEL;
                    filler = Material.SAND;
                    sub = Material.CLAY;
                }
                case DUNE -> {
                    top = Material.SAND;
                    filler = Material.SANDSTONE;
                    sub = Material.SANDSTONE;
                }
                case GELISOL -> {
                    top = Material.MOSS_BLOCK;
                    filler = Material.COARSE_DIRT;
                    sub = Material.PACKED_ICE;
                }
                default -> {
                    top = Material.GRASS_BLOCK;
                    filler = Material.DIRT;
                    sub = Material.STONE;
                }
            }
        }

        // -------------------------------------------------------- riparian
        if (col.riparian > 0.45 && !col.underwater() && col.playa < 0.35) {
            // The green line through the desert. Only where the water table really reaches the surface.
            top = Material.GRASS_BLOCK;
            filler = Material.DIRT;
            sub = Material.GRAVEL;
        }

        // --------------------------------------------------- beaches/shores
        if (!col.underwater() && col.oceanDistance < 34.0
                && Math.abs(col.surfaceY - p.seaLevel) < 5.0 && col.delta < 0.3) {
            double coarse = Interp.smoothstep(9.0, 22.0, col.slopeDeg);
            if (col.tempC < 1.0) {
                top = Material.SNOW_BLOCK;
                filler = Material.GRAVEL;
            } else if (col.rock == Rock.CARBONATE && col.tempC > 17.0) {
                top = Material.SAND;
                filler = Material.SANDSTONE;
            } else {
                top = coarse > 0.55 ? Material.GRAVEL : Material.SAND;
                filler = coarse > 0.55 ? Material.COBBLESTONE : Material.SANDSTONE;
            }
            sub = Material.STONE;
        }

        // ----------------------------------------------------- rock outcrop
        if (col.outcrop > 0.62 && !col.underwater()) {
            top = col.rock.primary;
            filler = col.rock.primary;
            sub = col.rock.secondary;
            if (col.boulders > 0.5) {
                top = col.rock == Rock.GRANITIC || col.rock == Rock.GNEISSIC
                        ? Material.COBBLESTONE : col.rock.primary;
            }
        }

        // ------------------------------------------------------- snow / ice
        col.snowLayers = 0;
        col.iceThickness = 0;
        double snowFrac = snowCover(col);
        if (snowFrac > 0.05 && !col.underwater() && top != Material.SNOW_BLOCK) {
            col.snowLayers = (int) Math.round(snowFrac * 8.0);
            if (col.snowLayers > 0 && col.snowLayers < 3 && top == Material.GRASS_BLOCK) {
                // A dusting does not hide the ground; a deep cover does.
                col.snowLayers = Math.max(1, col.snowLayers);
            }
            if (col.glacier > 0.55 || snowFrac > 0.88) {
                top = Material.SNOW_BLOCK;
                filler = Material.PACKED_ICE;
                sub = Material.BLUE_ICE;
                col.snowLayers = 0;
            }
        }
        if (col.underwater() && col.tempC < -1.0) {
            col.iceThickness = (int) Interp.clamp((-col.tempC) * 0.22, 1.0, 5.0);
            if (col.glacier > 0.4) {
                col.iceThickness = (int) Interp.clamp(col.iceThickness + 2, 1, 8);
            }
        }

        col.surface = top;
        col.filler = filler;
        col.subsoil = sub;
    }

    /**
     * Fraction of the year-equivalent snow cover, 0..1, from temperature, wind exposure and aspect.
     *
     * <p>Snow does not lie evenly. It blows off ridges and cornices on lee slopes, and it survives on
     * north-facing ground long after south-facing ground has melted out. Both effects are included
     * because both are visible from a distance: a snowy mountain range with bare ridgelines and snow
     * filling the shaded cirques is one of the most immediately readable landforms there is.
     */
    public static double snowCover(Column col) {
        double t = col.tempC;
        double base = Interp.smoothstep(2.5, -6.0, t);
        // Wind strips snow from exposed ground.
        base *= 1.0 - 0.55 * col.windExposure;
        // Aspect: pole-facing slopes keep snow. In the northern hemisphere that is north (-z).
        double poleward = col.latitude >= 0 ? -Math.cos(col.aspect) : Math.cos(col.aspect);
        base *= 1.0 + 0.30 * poleward * Interp.smoothstep(6.0, 26.0, col.slopeDeg);
        // Above the snow line everything stays white regardless.
        double aboveLine = Interp.smoothstep(col.snowLineM - 260.0, col.snowLineM + 40.0, col.elevationM);
        base = Math.max(base, aboveLine);
        if (col.glacier > 0.45) {
            base = Math.max(base, 0.85);
        }
        return Interp.clamp(base, 0.0, 1.0);
    }

    /**
     * Painted-strata material at a given depth below the surface.
     *
     * <p>Colour comes from the bedding band index, so a canyon wall shows the same alternation as the
     * real Triassic and Tertiary sediment of the Borrego badlands: red ochre, buff, white and grey in
     * beds a few metres thick, with thin darker partings.
     */
    public static Material paintedMaterial(Column col, int depthBelowSurface) {
        // Band index along the dip direction, sampled at the surface so the whole wall agrees.
        double band = depthBelowSurface / Math.max(1.5, col.strataSpacing * 0.42);
        int idx = (int) Math.floor(band);
        double frac = band - idx;
        long h = Hash.hash(0xBAD1A4D5L, idx, (int) Math.floor(col.strike * 1000.0),
                col.rock.ordinal());
        double r = Hash.unit(h);
        Material m;
        if (r < 0.24) {
            m = Material.RED_TERRACOTTA;
        } else if (r < 0.42) {
            m = Material.ORANGE_TERRACOTTA;
        } else if (r < 0.58) {
            m = Material.TERRACOTTA;
        } else if (r < 0.70) {
            m = Material.WHITE_TERRACOTTA;
        } else if (r < 0.80) {
            m = Material.RED_SANDSTONE;
        } else if (r < 0.90) {
            m = Material.SANDSTONE;
        } else {
            m = Material.PACKED_MUD;
        }
        // Thin partings read as real bedding on a cliff face.
        if (frac < 0.10 || frac > 0.93) {
            m = col.rock == Rock.SHALE ? Material.PACKED_MUD : Material.SMOOTH_SANDSTONE;
        }
        return m;
    }

    /** Chemically weathered parent rock: the regolith horizon. */
    public static Material weathered(Rock rock, double wetness, double aridity) {
        switch (rock) {
            case CARBONATE -> {
                return wetness > 0.6 ? Material.CLAY : Material.CALCITE;
            }
            case EVAPORITE -> {
                return Material.SMOOTH_SANDSTONE;
            }
            case VOLCANIC -> {
                return Material.TUFF;
            }
            case SHALE, CLASTIC -> {
                return aridity > 0.6 ? Material.SANDSTONE : Material.COBBLESTONE;
            }
            case DRIFT, COVER -> {
                return Material.GRAVEL;
            }
            case DEEP -> {
                return Material.DEEPSLATE;
            }
            default -> {
                return wetness > 0.72 ? Material.MOSSY_COBBLESTONE : Material.COBBLESTONE;
            }
        }
    }

    /**
     * Writes the whole column, {@code minY..maxY}, into {@code out} (indexed {@code y - minY}).
     *
     * @param out must be at least {@code maxY - minY + 1} long
     */
    public static void fill(TerraKernel k, Column col, int x, int z, Material[] out, CaveSampler caves) {
        GenParams p = k.params();
        int minY = p.minY;
        int maxY = p.maxY;
        int n = maxY - minY + 1;
        for (int i = 0; i < n; i++) {
            out[i] = Material.AIR;
        }
        decideSurface(col, p);

        int surf = col.surfaceY;
        int soilTop = (int) Math.round(col.soilThickness);
        int regolithTop = soilTop + (int) Math.round(col.regolith);
        Geology.Site site = col.site;
        Geology geology = k.geology();
        double nearChannel = Interp.clamp(col.channel, 0.0, 1.0);

        // ------------------------------------------------------- bedrock floor
        for (int y = minY; y <= minY + p.bedrockLayers + 2 && y < n + minY; y++) {
            double fuzz = 1.0 - Interp.smoothstep(minY + p.bedrockLayers - 2,
                    minY + p.bedrockLayers + 2, y);
            boolean solid = y <= minY + p.bedrockLayers - 2
                    || Hash.unit(Hash.hash(0xBED20C4L, x, y, z)) < fuzz;
            out[y - minY] = solid ? Material.BEDROCK : Material.AIR;
        }

        // -------------------------------------------------------- rock column
        int top = Math.min(surf, maxY);
        for (int y = minY + p.bedrockLayers + 3; y <= top; y++) {
            int depth = surf - y;
            Material m;
            boolean paintedHere = false;
            if (depth < 0) {
                m = Material.AIR;
            } else if (depth == 0) {
                m = col.surface;
            } else if (depth < soilTop) {
                m = depth == 1 ? col.filler : col.subsoil;
            } else if (depth < regolithTop) {
                m = weathered(col.rock, col.saturation, col.aridity);
            } else {
                m = geology.bedrockAt(site, x, y, z, depth, p.deepslateLevel);
                Material ore = geology.oreAt(site, x, y, z, depth, nearChannel, y < p.deepslateLevel);
                if (ore != null) {
                    m = ore;
                }
            }

            // Painted strata continue a few blocks down so a cut bank shows bedding, not a veneer.
            if (col.painted > 0.30 && depth >= 0 && depth < Math.max(2, regolithTop + 6)
                    && m.cat == Material.Cat.ROCK && !col.underwater()) {
                double strength = col.painted * Interp.smoothstep(0.15, 0.65, col.outcrop + col.painted);
                if (strength > 0.35) {
                    m = paintedMaterial(col, depth);
                    paintedHere = true;
                }
            }

            // Caves. Never eat the surface soil unless the void is strong: that is what produces
            // sinkholes and swallow holes rather than a pockmarked lawn.
            if (m != Material.AIR && m != Material.BEDROCK && y > minY + p.bedrockLayers + 3) {
                boolean voidHere = paintedHere ? false : caves.voidAt(x, y, z, col);
                if (voidHere) {
                    if (y <= p.lavaLevel) {
                        m = Material.LAVA;
                    } else if (col.waterTableY != Column.NO_WATER && y <= col.waterTableY) {
                        m = Material.WATER;
                    } else {
                        m = Material.AIR;
                    }
                }
            }
            // A ponor is a hole in the ground at the surface.
            if (col.swallow > 0.55 && depth >= 0 && depth < 4 && col.channel > 0.15) {
                m = Material.AIR;
            }
            out[y - minY] = m;
        }

        // ---------------------------------------------------------- water body
        if (col.waterY != Column.NO_WATER) {
            int waterTop = Math.min(col.waterY, maxY);
            for (int y = surf + 1; y <= waterTop; y++) {
                if (y - minY >= 0 && y - minY < n) {
                    out[y - minY] = Material.WATER;
                }
            }
            for (int i = 0; i < col.iceThickness && waterTop - i > surf; i++) {
                int y = waterTop - i;
                if (y - minY >= 0 && y - minY < n) {
                    out[y - minY] = i == 0 && col.glacier > 0.4 ? Material.PACKED_ICE : Material.ICE;
                }
            }
        }

        // ------------------------------------------------------------ snow cap
        if (col.snowLayers > 0 && !col.underwater()) {
            for (int i = 1; i <= col.snowLayers; i++) {
                int y = surf + i;
                if (y - minY >= 0 && y - minY < n) {
                    out[y - minY] = i >= col.snowLayers && col.snowLayers <= 2
                            ? Material.SNOW_LAYER : Material.SNOW_BLOCK;
                }
            }
        }

        // ------------------------------------------------------ magma at vents
        if (col.volcanoVent > 0.72 && surf - minY >= 0 && surf - minY < n) {
            out[surf - minY] = Material.MAGMA_BLOCK;
            if (surf - 1 - minY >= 0) {
                out[surf - 1 - minY] = Material.OBSIDIAN;
            }
        }
    }
}
