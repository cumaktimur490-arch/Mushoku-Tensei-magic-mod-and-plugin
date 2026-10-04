# Mushoku: Vast Lands

`Mushoku Tensei: Worldgen` is a standalone mod with a selectable **Mushoku: Vast Lands** world preset. It does not replace the default `minecraft:normal` preset or change the standard Overworld, so existing worlds and new vanilla worlds keep their original generation. The Nether and End remain vanilla in this preset as well.

## What is generated

The preset provides five custom surface biomes:

- **Golden Steppe** — warm, open grasslands with a muted gold-green palette.
- **Riverside Meadow** — broad flowered meadows and lowlands.
- **Whispering Forest** — oak and birch woodland.
- **Emerald Highlands** — cool, evergreen highlands.
- **Skyreach Mountains** — cold, rugged uplands with sparse alpine vegetation.

The custom palette replaces many vanilla surface climates inside this preset; deserts, jungles, swamps, savannas, and badlands are intentionally not selected. Vanilla oceans, beaches, rivers, and cave biomes are retained. The custom biomes are added to compatible vanilla structure tags so plains-style villages and pillager outposts can use them.

## Golden wheat fields

Golden Steppe now generates broad, irregular fields of ripe wheat on moist farmland, with a large patch attempted in each steppe chunk. Field edges follow the terrain and existing flowers and trees are left as natural breaks, so the crop does not turn every biome into one flat farm. Other biomes keep their own meadow, forest, and mountain character. Wheat fields generate only in newly created chunks of the Mushoku preset.

## Broad landmasses and blended relief

Only the Mushoku preset's Overworld uses `mushoku_worldgen:mushoku_overworld`. Its low-frequency continentalness field, with a smaller coast-detail octave, still drives both terrain density and the broad biome layout. The underlying vanilla `sloped_cheese` terrain profile, aquifers, ore veins, and 3D cave-density router remain the foundation, preserving the large continents already introduced without replacing Minecraft's terrain model.

This beta adds a restrained Tectonic/TerraForged-inspired layer rather than copying either mod's algorithms. Inland areas receive gentle plateau uplift; the vanilla erosion and ridge fields guide where the stronger, narrow mountain uplift appears. A feathered density carve around the river-like ridge band gives river corridors softer, lower approaches, while a small broad erosion term varies the surrounding forms. These are density adjustments, not direct block-height measurements, and may need tuning after playtesting.

The ordinary vanilla Overworld still uses `minecraft:overworld` noise settings and is not modified by this preset. The coefficients are experimental: no fresh-world in-game visual or performance QA has been performed, so the map shape and the appearance of the new relief are not claimed as verified.

## Worldgen crash fix

The 1.20.1 crash was a `Feature order cycle` between Golden Steppe and vanilla Deep Dark. Golden Steppe had placed `trees_plains` and `flower_plains` before `patch_tall_grass_2`, while Deep Dark requires tall grass before those shared features. The feature lists are now kept in vanilla-compatible order for both target versions. The resource generator also checks all custom decoration-stage lists against their vanilla templates and constructs an ordering graph to reject cycles before packaging.

This automated preflight is not a substitute for launching the game and generating chunks. A fresh Forge 1.20.1 world still needs in-game verification; that runtime path has not been confirmed visually or performance-tested in this workspace.

## Choosing the preset

Create a **new** world and select **Mushoku: Vast Lands** from the world-type/preset selector. Install only the artifact that matches the loader and Minecraft version:

| Loader | Minecraft | Artifact |
| --- | --- | --- |
| Fabric | 1.21.11 | `mushoku-worldgen-<version>.jar` |
| Forge | 1.20.1 | `mushoku-worldgen-forge-1.20.1-<version>.jar` |
| NeoForge | 1.20.1 | `mushoku-worldgen-neoforge-1.20.1-<version>.jar` |

The mod has no dependency on Mushoku Magic or Mushoku Weather. The Fabric 1.21.11 and native Forge/NeoForge 1.20.1 artifacts contain version-appropriate worldgen data and resource-pack metadata.

World presets affect newly generated chunks. For a clean test of the terrain blend and the fixed feature order, create a fresh world with the Mushoku preset rather than reusing the world that crashed during first generation.
