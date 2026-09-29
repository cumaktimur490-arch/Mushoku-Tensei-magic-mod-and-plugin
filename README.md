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

The Fabric artifact version is generated from `mod_version` in `gradle.properties` (currently 2.3.4). The three supplied wand textures are alpha-trimmed but not resized, preserving their original pixel detail at 103×123, 242×604, and 117×85 pixels.

## Native Minecraft 1.20.1 loader ports

The existing Fabric 1.21.11 build is retained alongside separate, native Minecraft 1.20.1 builds under `ports/forge` and `ports/neoforge`; both ports target Java 17 bytecode and share gameplay code while using each loader's own dependency and mod metadata. Build them from the repository root with:

```sh
./gradlew -p ports :forge:build :neoforge:build
```

The resulting jars are `ports/forge/build/libs/mushoku-magic-forge-1.20.1-2.3.4.jar` and `ports/neoforge/build/libs/mushoku-magic-neoforge-1.20.1-2.3.4.jar`. Install only the jar matching the loader in an instance. The Forge artifact declares Embeddium and Luxium as optional client dependencies; the mod does not require either to load. Local Cumulonimbus regions are synchronized from the server and use Minecraft's rain renderer only on clients inside the 20×20-chunk sector; leaving the sector restores the client's prior rain state. This never toggles dimension-wide server weather. The ports do not patch Luxium's renderer, whose project page notes that its rain and thunder rendering is not ready yet.

## Regional meteorology

The Fabric mod adds a player-local weather system rather than relying on Minecraft's dimension-wide rain. Slowly moving pressure and humidity fields blend across space, while biome precipitation, biome temperature, elevation, a 96-game-day seasonal cycle, daily temperature, and wind determine each region's conditions. Nearby regions can therefore be clear, overcast, rainy, snowy, or show localized lightning at the same time. When `severeWeatherEnabled` is on, warm low-pressure storms can grow into moving cyclones and tornadoes, cool thunderstorms can produce hail, and hot arid biomes can develop drifting sandstorms. The systems grow, mature, and fade rather than switching on instantly; layered cloud, rain, hail, dust, and rotating funnel particles are emitted only near players. Weather particles and physical effects are simulated only around active players; vanilla world-wide rain and thunder are kept clear while the feature is enabled. Exposed creatures gradually become wet in rain and dry over time, heavy rain extinguishes exposed fires, and cold regions turn storm precipitation into snow. Set `regionalWeatherEnabled` to `false` in `config/mushoku_magic.json` to return to vanilla weather, or `severeWeatherEnabled` to `false` there to disable natural cyclones, tornadoes, hailstorms, and sandstorms. Severe weather only removes very fragile blocks when `weatherBlockDamage` is explicitly enabled; it is `false` by default, and all weather visuals remain active. The simulation does not wet blocks, fill cauldrons, or apply crop-weather ticks. Strong exposed winds gently push nearby creatures and bend light spell trajectories, while heavy projectiles resist drift; the Gust and Updraft spells create short-lived local air currents. Rain and accumulated target wetness reduce Fire Bolt damage and shorten its burn effect; precipitation also suppresses fire-spell ignition. Humid air slightly strengthens water-beam hits, while cold air favors Ice Needle. Elemental adjustments are bounded and retain the configured ×2/×15/×50 wand scaling. Cumulonimbus keeps these effects inside its 20×20-chunk sector. See [weather-physics.md](docs/weather-physics.md) for the interaction details and bounds.

### Manual local weather control

With `regionalWeatherEnabled=true`, the mod deliberately keeps vanilla's dimension-wide rain and thunder clear so one command cannot change the entire world. Operators can set weather around themselves with `/magicweather clear`, `/magicweather cloudy`, `/magicweather rain`, `/magicweather thunder`, or `/magicweather snow`; append a duration in seconds (10–3600) to override the 120-second default. `/magicweather hail`, `/magicweather tornado`, `/magicweather cyclone`, and `/magicweather sandstorm` start the corresponding moving local hazard. Each command affects a 160-block radius around the operator; `/magicweather status` reports an active override. Severe hazards require `severeWeatherEnabled=true`. If regional weather is disabled, use vanilla `/weather` instead.

## Anime spell phrases

The spell list recognizes **Water Cannon** (a high-pressure water beam), **Cumulonimbus** (a targeted storm that slows creatures and extinguishes nearby fire), and **Earth Hedgehog** (a ring of rising stone spikes that damages, slows, and lifts nearby creatures). Cast visuals now gather into a rotating elemental focus before the projectile launches; Water Cannon grows into a coherent stream, while Cumulonimbus and Earth Hedgehog form around their target instead of using the same straight-line trail. Cumulonimbus sustains a fading-in/fading-out storm over a 20×20-chunk sector for 60–120 seconds. The server syncs each active sector to modded clients; vanilla rain and thunder gradients are enabled only in the local client view while its player is inside that sector, then the previous values are restored. The server's dimension-wide weather is never toggled. WeatherPhysics also applies the existing local wetness, fire-extinguishing, and spell interactions in the storm; vanilla block precipitation ticks such as filling cauldrons or growing crops are not simulated. Clouds, wind threads, and lightning remain lightweight effects rather than a volumetric-cloud renderer. Existing spells also recognize aliases such as **Stone Cannon**, **Quagmire**, **Icicle Lance**, **Exodus Flame**, and **Nuclear Explosion**. These names map to the mod's existing or adapted Minecraft mechanics; they are gameplay interpretations, not claims of frame-perfect spell simulation. Russian spellbook names are localized too: Cumulonimbus appears as «Кумуло Нимбус» and accepts that spaced phrase in chat. Versioned config migrations add new spells and aliases to existing `mushoku_magic.json` files without restoring them if a player later removes them.

## Wand-powered magic

Wands multiply spell damage and amplify range, area, and visual effects. Spell trails and impact bursts use layered anime-inspired colors: white-hot orange-red fire with a restrained violet overcharge accent, cyan-blue water, frost-white ice, stone-and-ochre earth, pale-cyan wind, and green-gold healing. See [the visual reference notes](docs/anime-magic-visual-style.md). The area and cast range are bounded to keep extreme custom multipliers manageable. At the base rank with no extra-word bonuses, the ×50 staff lets the default fire bolt reach about 87 blocks and affect an approximately 52×52-block area; the explosive fireball can affect up to about 64×64 blocks. Add `big`, `large`, or `huge` (or Russian forms such as «большой», «огромный», «большого шара», or «большого размера») before or after a fire or water spell phrase to create a much larger particle burst without changing damage or gameplay area. Existing custom keyword definitions are retained when this modifier is added to older configs. Fire spells set fire and explosions can damage terrain by default. Set `fireSpellsModifyBlocks` to `false` in `config/mushoku_magic.json` to disable those world changes while keeping the spell visuals and entity damage; an administrator can apply the change with `/magicadmin reload` (or restart the server). Large explosions can substantially alter the world.

Minecraft's displayed sharpness still depends on the in-game render size and filtering. The test suite checks the ×2/×15/×50 staff defaults, wand, spell-pack, and keyword config migrations, anime phrase aliases, large-modifier grammar, localized storm-sector bounds, regional-weather determinism and climate rules, cyclone/tornado/hail/sandstorm conditions and lifecycle, smoke-vortex rotation/convergence/updraft, weather wetness and spell interactions, staged spell-visual timing, and spell scaling. GitHub Actions builds and tests all three loader targets, and verifies each staff model's left/right first- and third-person transforms in the packaged jars. Download the release jar from the repository's [Releases](https://github.com/cumaktimur490-arch/Mushoku-Tensei-magic-mod-and-plugin/releases) page.
