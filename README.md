# Mushoku Tensei: Magic

Recovered Fabric mod source for Minecraft 1.21.11. The Java classes were decompiled from the supplied `mushoku-magic-1.0.0 (1).jar` with CFR 0.152. The original jar is retained as the reconstruction reference. Resource files under `src/main/resources` were copied from it byte-for-byte; the jar's intermediary mapping namespace is retained so the restored sources match the original binary.

## Staff power

| Staff tier | Power multiplier |
| --- | ---: |
| Tier 1 | ×2 |
| Tier 2 | ×15 |
| Tier 3 | ×50 |

These are the defaults written to `config/mushoku_magic.json`. An existing configuration using the previous unmodified defaults is upgraded automatically; custom multiplier values are preserved.

## Build

Building requires JDK 25 because the recovered Fabric Loom version runs on Java 25. The project compiles with `--release 21`, so the resulting mod remains compatible with Minecraft's Java 21 minimum. The Gradle wrapper downloads the pinned Gradle version.

```sh
./gradlew clean build
```

Version 2.2.5 is built as `build/libs/mushoku-magic-2.2.5.jar`; its Fabric metadata version is generated from `mod_version` in `gradle.properties`. The three supplied wand textures are alpha-trimmed but not resized, preserving their original pixel detail at 103×123, 242×604, and 117×85 pixels.

## Regional meteorology

The Fabric mod adds a player-local weather system rather than relying on Minecraft's dimension-wide rain. Slowly moving pressure and humidity fields blend across space, while biome precipitation, biome temperature, elevation, a 96-game-day seasonal cycle, daily temperature, and wind determine each region's conditions. Nearby regions can therefore be clear, overcast, rainy, snowy, or show localized lightning at the same time, with dust gusts possible in hot, dry biomes. Weather particles are emitted only around active players, and vanilla world-wide rain and thunder are kept clear while the feature is enabled. Set `regionalWeatherEnabled` to `false` in `config/mushoku_magic.json` to return to vanilla weather. Regional rain and snow are visual particle effects; they do not wet blocks, fill cauldrons, or apply crop-weather simulation.

## Anime spell phrases

The spell list recognizes **Water Cannon** (a high-pressure water beam), **Cumulonimbus** (a targeted storm that slows creatures and extinguishes nearby fire), and **Earth Hedgehog** (a ring of rising stone spikes that damages, slows, and lifts nearby creatures). Cumulonimbus now sustains a fading-in/fading-out local rain, cloud, and visual-lightning effect over a 20×20 chunk sector for 60–120 seconds. Vanilla rain is dimension-wide, so the spell simulates localized weather with particles instead of changing the world's global weather or applying rain physics. Existing spells also recognize aliases such as **Stone Cannon**, **Quagmire**, **Icicle Lance**, **Exodus Flame**, and **Nuclear Explosion**. These names map to the mod's existing or adapted Minecraft mechanics; they are gameplay interpretations, not claims of frame-perfect spell simulation. Russian spellbook names are localized too: Cumulonimbus appears as «Кумуло Нимбус» and accepts that spaced phrase in chat. Versioned config migrations add new spells and aliases to existing `mushoku_magic.json` files without restoring them if a player later removes them.

## Wand-powered magic

Wands multiply spell damage and amplify range, area, and visual effects. Spell trails and impact bursts use layered anime-inspired colors: white-hot orange-red fire with a restrained violet overcharge accent, cyan-blue water, frost-white ice, stone-and-ochre earth, pale-cyan wind, and green-gold healing. See [the visual reference notes](docs/anime-magic-visual-style.md). The area and cast range are bounded to keep extreme custom multipliers manageable. At the base rank with no extra-word bonuses, the ×50 staff lets the default fire bolt reach about 87 blocks and affect an approximately 52×52-block area; the explosive fireball can affect up to about 64×64 blocks. Add `big`, `large`, or `huge` (or Russian forms such as «большой», «огромный», «большого шара», or «большого размера») before or after a fire or water spell phrase to create a much larger particle burst without changing damage or gameplay area. Existing custom keyword definitions are retained when this modifier is added to older configs. Fire spells set fire and explosions can damage terrain by default. Set `fireSpellsModifyBlocks` to `false` in `config/mushoku_magic.json` to disable those world changes while keeping the spell visuals and entity damage; an administrator can apply the change with `/magicadmin reload` (or restart the server). Large explosions can substantially alter the world.

Minecraft's displayed sharpness still depends on the in-game render size and filtering. The test suite checks the ×2/×15/×50 staff defaults, wand, spell-pack, and keyword config migrations, anime phrase aliases, large-modifier grammar, localized storm-sector bounds, regional-weather determinism and climate rules, and spell scaling. GitHub Actions runs the same build and tests on pushes and pull requests. Download the release jar from the repository's [Releases](https://github.com/cumaktimur490-arch/Mushoku-Tensei-magic-mod-and-plugin/releases) page.
