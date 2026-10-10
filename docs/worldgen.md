# Mushoku: Vast Lands

`Mushoku Tensei: Worldgen` is a standalone mod with a selectable **Mushoku: Vast Lands** world preset. It does not replace the default `minecraft:normal` preset or change the standard Overworld, so existing worlds and new vanilla worlds keep their original generation. The Nether and End remain vanilla in this preset as well.

## What is generated

The preset provides five custom surface biomes:

- **Golden Steppe** — warm, open grasslands with a muted gold-green palette.
- **Riverside Meadow** — broad flowered meadows and lowlands.
- **Whispering Forest** — oak and birch woodland.
- **Emerald Highlands** — cool, evergreen highlands.
- **Skyreach Mountains** — cold, rugged uplands with sparse alpine vegetation.

The custom palette replaces many vanilla surface climates inside this preset; deserts, jungles, swamps, savannas, and badlands are intentionally not selected. Vanilla oceans, beaches, rivers, and cave biomes are retained. Mushoku biomes are appended to compatible vanilla biome-category and structure tags, so tag-driven integrations can recognize the forest, taiga, hill, mountain, and village roles without replacing any vanilla tag contents.

## Golden wheat fields

Golden Steppe now generates broad, irregular fields of ripe wheat on moist farmland, with a large patch attempted in each steppe chunk. Field edges follow the terrain and existing flowers and trees are left as natural breaks, so the crop does not turn every biome into one flat farm. Other biomes keep their own meadow, forest, and mountain character. Wheat fields generate only in newly created chunks of the Mushoku preset.

## Natural ground cover

The five Mushoku biomes add a second, biome-specific vegetation layer on top of their vanilla-style feature sets. This update doubles the patch attempts for ground grass, tall grass, and fern understory in Whispering Forest and Emerald Highlands, and makes their supplemental blooms slightly more frequent. Riverside Meadow keeps the fullest grass-and-flower cover; Golden Steppe remains open and keeps its wheat fields as the final accent; Skyreach Mountains retains a lighter alpine edge. These changes apply only to the custom Mushoku biomes, with matching resources for Fabric 1.21.11 and Forge/NeoForge 1.20.1. More vegetation means more feature-placement work in those two biomes; performance still needs fresh-world testing.

The supplied YouTube timestamp was not available for frame-by-frame inspection in this workspace, so this pass targets the visible grassy, flower-dotted hillside direction without claiming an exact recreation. Fresh-chunk in-game visual and performance QA have not been performed.

## Broad landmasses and blended relief

Only the Mushoku preset's Overworld uses `mushoku_worldgen:mushoku_overworld`. Its low-frequency continentalness field still drives both terrain density and the broad biome layout; the smaller coast-detail octave has been reduced from 0.18 to 0.10 to avoid choppy shorelines. The underlying vanilla `sloped_cheese` terrain profile, aquifers, ore veins, and 3D cave-density router remain the foundation, preserving the large continents already introduced without replacing Minecraft's terrain model.

This beta softens the Tectonic/TerraForged-inspired blend without copying either mod's algorithms. Inland, erosion, ridge, and river-corridor masks now use cubic smoothstep easing, removing abrupt changes at their thresholds. The plateau/ridge uplift is reduced, river carving is shallower (maximum added density -0.055 instead of -0.16), and the broad erosion term is halved. The large-scale continent field, vanilla caves/aquifers, biome palette, and standard terrain foundation are retained. These are density adjustments, not direct block-height measurements, and may need further tuning after playtesting.

The ordinary vanilla Overworld still uses `minecraft:overworld` noise settings and is not modified by this preset. The coefficients are experimental: no fresh-world in-game visual or performance QA has been performed, so the map shape and the appearance of the new relief are not claimed as verified.

## Worldgen crash fix

The 1.20.1 crash was a `Feature order cycle` between Golden Steppe and vanilla Deep Dark. Golden Steppe had placed `trees_plains` and `flower_plains` before `patch_tall_grass_2`, while Deep Dark requires tall grass before those shared features. The feature lists are now kept in vanilla-compatible order for both target versions. The resource generator also checks all custom decoration-stage lists against their vanilla templates and constructs an ordering graph to reject cycles before packaging.

This automated preflight is not a substitute for launching the game and generating chunks. A fresh Forge 1.20.1 world still needs in-game verification; that runtime path has not been confirmed visually or performance-tested in this workspace.

## Modpack integration and generation cost

The 1.20.1 Forge/NeoForge and Fabric 1.21.11 data packs use the same append-only compatibility mapping:

| Vanilla biome tag | Mushoku biome(s) | Purpose |
| --- | --- | --- |
| `minecraft:is_forest` | Whispering Forest | Forest-aware features and structures |
| `minecraft:is_taiga`, `minecraft:is_hill` | Emerald Highlands | Taiga/hill integrations, including tag-based tree and structure packs |
| `minecraft:is_mountain` | Skyreach Mountains | Mountain-aware structures and features |
| `minecraft:has_structure/village_plains` | Golden Steppe, Riverside Meadow, Whispering Forest | Plains-style villages |
| `minecraft:has_structure/village_taiga` | Emerald Highlands | Taiga villages |
| `minecraft:has_structure/village_snowy` | Skyreach Mountains | Snowy villages |
| `minecraft:has_structure/pillager_outpost` | All five custom biomes | Pillager outposts |

Worldgen appends common `c:` biome categories with only the matching Mushoku biomes. It ships the older names (`c:in_overworld`, `c:plains`, `c:floral`, `c:forest`, `c:birch_forest`, `c:taiga`, `c:mountain`, `c:snowy`) alongside their `c:is_*` selector aliases, plus `c:tree_coniferous`, `c:tree_deciduous`, and dense/sparse vegetation tags. This covers both the older Fabric convention-tag names used around 1.20.1 and the newer `is_*` family used by later/cross-loader selectors, without a library dependency; vanilla tag contents are append-only.

For the supplied [Moog's Voyager Structures 5.1.3-1.20](https://github.com/Moog-s-Mods/MoogsVoyagerStructures/releases/tag/5.1.3-1.20), Worldgen also extends the mod's own append-only biome tags:

| MVS biome tag | Mushoku biome(s) |
| --- | --- |
| `mvs:is_plains` | Golden Steppe, Riverside Meadow |
| `mvs:is_floral` | Riverside Meadow |
| `mvs:is_forest`, `mvs:is_birch_forest` | Whispering Forest |
| `mvs:is_taiga` | Emerald Highlands |
| `mvs:is_mountain`, `mvs:is_snowy`, `mvs:snowy_biomes` | Skyreach Mountains |
| `mvs:is_overworld`, `mvs:is_on_land_overworld` | All five custom biomes |

I audited the 130 structure biome selectors in the MVS 5.1.3-1.20 source. Direct selectors include `mvs:is_overworld` in 58 structures, `mvs:is_plains` in 4, `mvs:is_floral` in 1, `mvs:is_birch_forest` in 6, `mvs:is_forest` in 2, `mvs:is_taiga` in 11, `mvs:is_mountain` in 1 and `mvs:is_snowy` in 5. `mvs:snowy_biomes` is selected by `snowy_fossil`; Skyreach Mountains is appended to that tag as well, so it is eligible on Fabric too, where Forge's optional `forge:is_snowy` entry is unavailable. This is a source-data eligibility check, not a claim that generated structures have been tested in-game.

The native Forge/NeoForge 1.20.1 artifacts also append Golden Steppe and Riverside Meadow to `forge:is_plains`; Emerald Highlands to `forge:is_coniferous`; the forest, meadow, and highlands to `forge:is_dense` and `forge:is_wet`; Emerald Highlands and Skyreach Mountains to `forge:is_cold` and `forge:is_slope`; Skyreach Mountains to `forge:is_mountain`, `forge:is_peak`, and `forge:is_snowy`; and Golden Steppe plus Skyreach Mountains to `forge:is_sparse`. These use existing Forge 1.20.1 biome-tag identifiers and help Forge tag-based features recognize the new biomes without requiring MVS in the mod metadata. Every tag uses `"replace": false`; only the custom Mushoku IDs are added. Structure mods may consequently consider their matching structures in these new biomes; their own rarity settings still control placement.

The standard Overworld has no Mushoku biomes in its generator, so it remains unchanged. These tags improve integrations that consult the same categories; they do not promise that every third-party tree pack or structure has a rule for every custom biome. The native Mushoku jars do not require Connector, Quark, Dynamic Trees, or MVS.

For a Forge 1.20.1 pack already running ModernFix and FerriteCore, Worldgen adds no second optimization library or renderer. Its vegetation layer uses a small number of count/rarity placements per custom biome; the potentially heavier costs are initial terrain generation, third-party structures, and Distant Horizons LOD generation. On an unknown CPU/GPU, start with a modest render/LOD distance and increase it only after testing; if pre-generating a large area, use a chunk pre-generator gradually and monitor server tick time. Adding `is_forest`, `is_taiga`, and other semantic tags can make tag-aware structures eligible in those Mushoku biomes, so structure density remains controlled by those mods' own configs.

Mushoku Weather applies local precipitation state inside its storm sector. Particle Rain replaces the vanilla precipitation renderer and also adds its own wind/haze/dust effects, so using both can create overlapping visuals or particle cost. Configure one precipitation layer at a time if duplication appears; keep the Mushoku Weather simulation enabled if you need localized storms. Fog Overrides and Luxium affect client rendering rather than terrain generation; for visual faults, test a clean Forge profile without the optional fog/renderer overlays before treating it as a Worldgen failure. This combination has not yet received in-game compatibility or performance QA.

## Choosing the preset

Create a **new** world and select **Mushoku: Vast Lands** from the world-type/preset selector. Install only the artifact that matches the loader and Minecraft version:

| Loader | Minecraft | Artifact |
| --- | --- | --- |
| Fabric | 1.21.11 | `mushoku-worldgen-<version>.jar` |
| Forge | 1.20.1 | `mushoku-worldgen-forge-1.20.1-<version>.jar` |
| NeoForge | 1.20.1 | `mushoku-worldgen-neoforge-1.20.1-<version>.jar` |

The mod has no dependency on Mushoku Magic or Mushoku Weather. The Fabric 1.21.11 and native Forge/NeoForge 1.20.1 artifacts contain version-appropriate worldgen data and resource-pack metadata.

World presets affect newly generated chunks. For a clean test of the terrain blend and the fixed feature order, create a fresh world with the Mushoku preset rather than reusing the world that crashed during first generation. See [forge-1.20.1-modpack-compatibility.md](forge-1.20.1-modpack-compatibility.md) for the mod-list observations and a focused runtime test checklist.
