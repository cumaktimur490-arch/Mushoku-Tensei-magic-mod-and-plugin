# Mushoku: Vast Lands

`Mushoku Tensei: Worldgen` is a standalone mod with a selectable **Mushoku: Vast Lands** world preset. It does not replace the default `minecraft:normal` preset or change the standard Overworld, so existing worlds and new vanilla worlds keep their original generation. The Nether and End remain vanilla in the new preset as well.

## World palette

The preset gives the Overworld five custom surface biomes:

- **Golden Steppe** — warm, open grasslands with a muted gold-green palette.
- **Riverside Meadow** — broad flowered meadows and lowlands.
- **Whispering Forest** — oak and birch woodland.
- **Emerald Highlands** — cool, evergreen highlands.
- **Skyreach Mountains** — cold, rugged uplands with sparse alpine vegetation.

The custom palette replaces many vanilla surface climates inside this preset; deserts, jungles, swamps, savannas, and badlands are intentionally not selected. Vanilla oceans, beaches, rivers, and cave biomes are retained. The custom biomes are added to compatible vanilla structure tags so plains-style villages and pillager outposts can use them.

This first data-driven version keeps Minecraft's built-in Overworld noise settings and continentalness field, then maps broad climate regions to the Mushoku palette. That keeps world terrain and vanilla worlds isolated and compatible; exact continent-size and mountain-profile tuning remains preliminary until the preset has been explored in-game. No claim of visual worldgen QA is made yet.

## Choosing the preset

Create a new world and select **Mushoku: Vast Lands** from the world-type/preset selector. Install only the artifact that matches the loader and Minecraft version:

| Loader | Minecraft | Artifact |
| --- | --- | --- |
| Fabric | 1.21.11 | `mushoku-worldgen-<version>.jar` |
| Forge | 1.20.1 | `mushoku-worldgen-forge-1.20.1-<version>.jar` |
| NeoForge | 1.20.1 | `mushoku-worldgen-neoforge-1.20.1-<version>.jar` |

The mod has no dependency on Mushoku Magic or Mushoku Weather. The Fabric 1.21.11 and native Forge/NeoForge 1.20.1 artifacts contain version-appropriate worldgen data and resource-pack metadata.

World presets only affect newly created worlds. Do not switch an existing save to a different generator; create a fresh world for a clean biome layout.
