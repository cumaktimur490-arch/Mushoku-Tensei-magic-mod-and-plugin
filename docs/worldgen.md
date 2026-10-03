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

## Broad landmasses

Unlike the previous biome-only version, this preset now points only its Overworld generator at `mushoku_worldgen:mushoku_overworld`. That isolated noise setting adds a low-frequency continentalness field (with a smaller coast-detail octave) to vanilla Overworld terrain density. The broad field is also used by the biome source, so land and Mushoku climate regions follow the same large-scale pattern. The vanilla terrain profile, aquifers, ore veins, and 3D cave-density router are retained as the base; only the macro landmass field and the compact surface rules are customized. The intended result is fewer, wider land regions with longer plains, forest belts, and mountain ranges rather than only larger patches of unchanged biomes.

The ordinary vanilla Overworld still uses `minecraft:overworld` noise settings and is not modified by this preset. The scale coefficients are a first-pass beta and have not yet had in-game visual or performance QA; no visual result is being claimed as verified.

## Worldgen crash fix

The 1.20.1 crash was a `Feature order cycle` between Golden Steppe and vanilla Deep Dark. Golden Steppe had placed `trees_plains` and `flower_plains` before `patch_tall_grass_2`, while Deep Dark requires tall grass before those shared features. The feature lists are now kept in vanilla-compatible order for both target versions. The resource generator also checks all custom decoration-stage lists against their vanilla templates and constructs an ordering graph to reject cycles before packaging.

This automated preflight is not a substitute for launching the game and generating chunks. The beta still needs in-game verification, including a fresh Forge 1.20.1 world, because that exact runtime path has not been visually or performance-tested in this workspace.

## Choosing the preset

Create a **new** world and select **Mushoku: Vast Lands** from the world-type/preset selector. Install only the artifact that matches the loader and Minecraft version:

| Loader | Minecraft | Artifact |
| --- | --- | --- |
| Fabric | 1.21.11 | `mushoku-worldgen-<version>.jar` |
| Forge | 1.20.1 | `mushoku-worldgen-forge-1.20.1-<version>.jar` |
| NeoForge | 1.20.1 | `mushoku-worldgen-neoforge-1.20.1-<version>.jar` |

The mod has no dependency on Mushoku Magic or Mushoku Weather. The Fabric 1.21.11 and native Forge/NeoForge 1.20.1 artifacts contain version-appropriate worldgen data and resource-pack metadata.

World presets affect newly generated chunks. For a clean test of the changed terrain and the fixed feature order, create a fresh world with the Mushoku preset rather than reusing the world that crashed during first generation.
