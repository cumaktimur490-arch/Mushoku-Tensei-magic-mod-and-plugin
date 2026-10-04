# Mushoku Tensei: Magic, Weather, and Worldgen

Recovered Fabric mod source for Minecraft 1.21.11. The Java classes were decompiled from the supplied `mushoku-magic-1.0.0 (1).jar` with CFR 0.152. The original jar is retained as the reconstruction reference. Resource files under `src/main/resources` were copied from it byte-for-byte; the jar's intermediary mapping namespace is retained so the restored sources match the original binary.

## Separate mods and dependency

Weather is now an independent mod: **Mushoku Tensei: Weather** (`mushoku_weather`). It runs without the Magic mod and owns regional weather, severe storms, weather configuration, local weather commands, and the local rendering/network state. **Mushoku Tensei: Magic** (`mushoku_magic`) has a required dependency on Weather because spells use its weather API. Weather does not depend on Magic. Install both jars when playing with Magic; the Weather jar can also be used by itself.

The repository builds Fabric 1.21.11 mods at the root and native Forge/NeoForge 1.20.1 mods under `ports/`. For each game/loader combination, the Weather and Magic jars are separate and must match that same loader. **Mushoku Tensei: Worldgen** (`mushoku_worldgen`) is a third, standalone mod that adds the selectable **Mushoku: Vast Lands** preset with broad continents, a restrained Tectonic/TerraForged-inspired relief blend, and extensive ripe-wheat fields in Golden Steppe; the ordinary vanilla Overworld and other dimensions are unchanged. It has no dependency on Magic or Weather. See [worldgen.md](docs/worldgen.md) for the biome palette, wheat fields, feature-order crash fix, terrain blend, and beta QA caveat.

## Staff power

| Staff tier | Power multiplier |
| --- | ---: |
| Tier 1 | ×2 |
| Tier 2 | ×15 |
| Tier 3 | ×50 |

These are the defaults written to `config/mushoku_magic.json`. An existing configuration using the previous unmodified defaults is upgraded automatically; custom multiplier values are preserved.

## Build Fabric 1.21.11

Building requires JDK 25 because the recovered Fabric Loom version runs on Java 25. Both artifacts compile with `--release 21`, so they remain compatible with Minecraft's Java 21 minimum. The Gradle wrapper downloads the pinned Gradle version.

```sh
./gradlew :weather:build :worldgen:build build
```

The outputs include `build/libs/mushoku-magic-2.5.4.jar`, `weather/build/libs/mushoku-weather-1.1.3.jar`, and `worldgen/build/libs/mushoku-worldgen-0.1.3-beta.1.jar` (versions are configured in the root `gradle.properties`). Worldgen is optional; install it to select the Mushoku preset during new-world creation. Install the Weather jar for regional weather and both Weather and Magic jars to enable Magic. The Magic metadata enforces the Weather dependency.

The three supplied wand textures are alpha-trimmed but not resized, preserving their original pixel detail at 103×123, 242×604, and 117×85 pixels.

## Native Minecraft 1.20.1 loader builds

The existing Fabric 1.21.11 build is retained alongside native Forge and NeoForge 1.20.1 builds. They target Java 17 bytecode and do not use Connector.

```sh
./gradlew -p ports :weather-forge:build :weather-neoforge:build :worldgen-forge:build :worldgen-neoforge:build :forge:build :neoforge:build
```

For each loader, install both matching jars to use Magic: `mushoku-weather-<loader>-1.20.1-1.1.3.jar` and `mushoku-magic-<loader>-1.20.1-2.5.4.jar`. Weather works without the Magic jar. The optional standalone worldgen jars are `mushoku-worldgen-forge-1.20.1-0.1.3-beta.1.jar` and `mushoku-worldgen-neoforge-1.20.1-0.1.3-beta.1.jar`. Forge alone lists Embeddium and Luxium as optional client dependencies; neither is required. The weather mod does not patch Luxium's renderer. See [ports/README.md](ports/README.md) for build and loader details.

## Weather mod and magic integration

The complete meteorology simulation is in the separate [Mushoku Tensei: Weather project](weather/README.md). It replaces dimension-wide vanilla weather with spatially localized conditions, moving storm systems, rotating supercells, squall lines, cyclones, tornadoes, hail, sandstorms, wind, local precipitation rendering, and `/magicweather` controls. Its settings live in `config/mushoku_weather.json`; existing weather settings are migrated from `mushoku_magic.json` when this file is first created.

Magic uses the required Weather API to keep its established spell interactions: rain and target wetness reduce Fire Bolt damage and burn duration, precipitation suppresses fire-spell ignition, humid air can strengthen water magic, cold air favors ice, and wind bends lighter spell trajectories. Gust and Updraft spells create short-lived local currents. Cumulonimbus starts a fading local storm in its 20×20-chunk sector; it never changes the server's dimension-wide weather. See [weather-physics.md](docs/weather-physics.md) for interaction bounds and [anime-magic-visual-style.md](docs/anime-magic-visual-style.md) for spell-color references.

## Anime spell phrases

The spell list recognizes **Water Cannon** (a high-pressure water beam), **Cumulonimbus** (a targeted storm that slows creatures and extinguishes nearby fire), and **Earth Hedgehog** (a ring of rising stone spikes that damages, slows, and lifts nearby creatures). Cast visuals gather into a rotating elemental focus before the projectile launches; Water Cannon grows into a coherent stream, while Cumulonimbus and Earth Hedgehog form around their target instead of using the same straight-line trail. Cumulonimbus asks the standalone Weather mod to sustain a fading-in/fading-out 20×20-chunk storm for 60–120 seconds. The Weather server synchronizes the active sector and cast altitude to clients; shader-raymarched 3D clouds, local rain, and thunder are presented in that sector, and the previous local weather values are restored on exit. The server's dimension-wide weather is never toggled. Vanilla block precipitation ticks such as filling cauldrons or growing crops are not simulated. Severe storms and regional cloud systems are rendered as GPU volumetric density fields with distinct supercell, squall, cyclone, tornado, and overcast structures; particles serve only as auxiliary rain, hail, wind, lightning, and dust effects.

Existing spells also recognize aliases such as **Stone Cannon**, **Quagmire**, **Icicle Lance**, **Exodus Flame**, and **Nuclear Explosion**. These names map to the mod's existing or adapted Minecraft mechanics; they are gameplay interpretations, not claims of frame-perfect spell simulation. Russian spellbook names are localized too: Cumulonimbus appears as «Кумуло Нимбус» and accepts that spaced phrase in chat. Versioned config migrations add new spells and aliases to existing `mushoku_magic.json` files without restoring them if a player later removes them.

## Wand-powered magic

Wands multiply spell damage and amplify range, area, and visual effects. Spell trails and impact bursts use layered anime-inspired colors: white-hot orange-red fire with a restrained violet overcharge accent, cyan-blue water, frost-white ice, stone-and-ochre earth, pale-cyan wind, and green-gold healing. The area and cast range are bounded to keep extreme custom multipliers manageable. At the base rank with no extra-word bonuses, the ×50 staff lets the default fire bolt reach about 87 blocks and affect an approximately 52×52-block area; the explosive fireball can affect up to about 64×64 blocks. Add `big`, `large`, or `huge` (or Russian forms such as «большой», «огромный», «большого шара», or «большого размера») before or after a fire or water spell phrase to create a much larger particle burst without changing damage or gameplay area. Existing custom keyword definitions are retained when this modifier is added to older configs. Fire spells set fire and explosions can damage terrain by default. Set `fireSpellsModifyBlocks` to `false` in `config/mushoku_magic.json` to disable those world changes while keeping the spell visuals and entity damage; an administrator can apply the change with `/magicadmin reload` (or restart the server). Large explosions can substantially alter the world.

## Optional AAA Particles spell effects

The native Forge and NeoForge Magic builds for Minecraft 1.20.1 can optionally use [AAA Particles](https://modrinth.com/mod/En8uHTOK) to play Effekseer fire, blue-water/ice, and earth/gold burst effects. AAA Particles is a client-side optional dependency: install it only on clients that want the enhanced bursts; the server does not need it. Standard Mushoku spell particles remain active as the fallback, and the integration can be disabled with `aaaParticlesSpellEffects: false` in `config/mushoku_magic.json`.

The root Fabric build targets Minecraft 1.21.11, for which AAA Particles does not currently publish a compatible Fabric build, so that artifact does not advertise the integration. See [aaa-particles.md](docs/aaa-particles.md) for the loader matrix and the third-party Effekseer asset license.

**Beta QA caveat:** the integration is build/resource-verified only. It has not been visually or performance-tested in-game with AAA Particles, graphics drivers, or other client mods; treat the rendered size, timing, and GPU cost as unverified until tested in Minecraft.

Minecraft's displayed sharpness still depends on the in-game render size and filtering. The test suite checks the ×2/×15/×50 staff defaults, wand and config migrations, anime phrase aliases, large-modifier grammar, localized storm-sector bounds, regional-weather determinism and climate rules, supercell/squall/cyclone/tornado/hail/sandstorm conditions and lifecycle, smoke-vortex rotation/convergence/updraft, weather wetness and spell interactions, staged spell-visual timing, and spell scaling. GitHub Actions builds Magic, Weather, and Worldgen for Fabric, Forge, and NeoForge; it runs the Magic and Weather test suites, validates generated/packaged Worldgen data and metadata, and checks staff hand transforms in the packaged Magic jars. Download the release jars from the repository's [Releases](https://github.com/cumaktimur490-arch/Mushoku-Tensei-magic-mod-and-plugin/releases) page.
