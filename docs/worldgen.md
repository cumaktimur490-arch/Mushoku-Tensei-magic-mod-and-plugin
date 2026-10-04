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

The five Mushoku biomes now add a second, biome-specific vegetation layer on top of their vanilla-style feature sets. It uses compact patches of short and tall grass, fern sprigs beneath the forest/highland canopy, and occasional supplemental flower clusters. Riverside Meadow gets the fullest grass-and-flower cover; Golden Steppe remains open and keeps its wheat fields as the final accent; Whispering Forest and Emerald Highlands gain fern-rich floors; Skyreach Mountains keeps a lighter alpine grass-and-bloom edge. These placed features are added only to the custom Mushoku biomes, with matching resources for Fabric 1.21.11 and Forge/NeoForge 1.20.1.

The supplied YouTube timestamp was not available for frame-by-frame inspection in this workspace, so this pass targets the visible grassy, flower-dotted hillside direction without claiming an exact recreation. Fresh-chunk in-game visual and performance QA have not been performed.

## Broad landmasses and blended relief

Only the Mushoku preset's Overworld uses `mushoku_worldgen:mushoku_overworld`. Its low-frequency continentalness field, with a smaller coast-detail octave, still drives both terrain density and the broad biome layout. The underlying vanilla `sloped_cheese` terrain profile, aquifers, ore veins, and 3D cave-density router remain the foundation, preserving the large continents already introduced without replacing Minecraft's terrain model.

This beta adds a restrained Tectonic/TerraForged-inspired layer rather than copying either mod's algorithms. Inland areas receive gentle plateau uplift; the vanilla erosion and ridge fields guide where the stronger, narrow mountain uplift appears. A feathered density carve around the river-like ridge band gives river corridors softer, lower approaches, while a small broad erosion term varies the surrounding forms. These are density adjustments, not direct block-height measurements, and may need tuning after playtesting.

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

For the supplied [Moog's Voyager Structures 5.1.3-1.20](https://github.com/Moog-s-Mods/MoogsVoyagerStructures/releases/tag/5.1.3-1.20), Worldgen also extends the mod's own append-only biome tags:

| MVS biome tag | Mushoku biome(s) |
| --- | --- |
| `mvs:is_plains` | Golden Steppe, Riverside Meadow |
| `mvs:is_floral` | Riverside Meadow |
| `mvs:is_forest`, `mvs:is_birch_forest` | Whispering Forest |
| `mvs:is_taiga` | Emerald Highlands |
| `mvs:is_mountain`, `mvs:is_snowy` | Skyreach Mountains |
| `mvs:is_overworld`, `mvs:is_on_land_overworld` | All five custom biomes |

The native Forge/NeoForge 1.20.1 artifacts also append Golden Steppe and Riverside Meadow to `forge:is_plains`, Emerald Highlands to `forge:is_coniferous`, and Skyreach Mountains to `forge:is_mountain` and `forge:is_snowy`. This gives MVS's optional Forge tag references and other Forge tag-based features the appropriate custom biomes without requiring MVS in the mod metadata. Every tag uses `"replace": false`; only the custom Mushoku IDs are added. Structure mods may consequently consider their matching structures in these new biomes; their own rarity settings still control placement.

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
