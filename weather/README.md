# Mushoku Tensei: Weather

`mushoku_weather` is the standalone weather mod in this repository. It does not require or load Mushoku Tensei: Magic; install the Magic mod only when you want its spell-to-weather interactions. Magic has a required dependency on this mod.

## Supported builds

- Fabric for Minecraft 1.21.11 (`weather/build.gradle`)
- Native Forge and NeoForge for Minecraft 1.20.1 (`ports/weather-forge` and `ports/weather-neoforge`)

These are separate loader/game builds. Do not install jars for different loaders or Minecraft versions together. Weather has no dependency on Magic, but Magic and Weather jars must both be present for the Magic mod to load.

## Weather simulation

The system keeps weather local instead of switching rain for an entire dimension. Regional weather blends moving pressure and humidity fields with biome precipitation, temperature, elevation, seasons, daily temperature, and wind. Nearby storm cells pass through formation, maturity, optional strengthening, and dissipation; they can develop into supercells, fast squall lines, cyclones, tornadoes, hailstorms, and drifting sandstorms. Their cloud bodies are real GPU ray-marched density volumes, with distinct anvil/wall-cloud supercells, shelf-cloud gust fronts, spiral cyclone bands, ground-reaching tornado funnels, and stratiform regional overcast. The volumetric renderer uses a quality setting and distance fade; rain, hail, wind, lightning, and near-ground dust remain lightweight secondary particle effects. Local wind and precipitation influence exposed creatures and fires. Severe wind effects only damage blocks when explicitly enabled; it is off by default and has per-storm block-count and hardness caps.

Cumulonimbus spells from the dependent Magic mod call the Weather API to create a fading local storm in exactly a 20×20-chunk sector at the cast altitude. The server synchronizes that region to clients; its volumetric cloud, rain, and thunder are presented only while the client is in the sector, and the local vanilla rain view is restored on exit. It never changes server-wide or dimension-wide weather. When regional weather is enabled, the system replaces vanilla weather changes and holds global rain clear; if disabled, vanilla `/weather` works normally.

## Configuration

Settings are stored in `config/mushoku_weather.json`:

- `regionalWeatherEnabled` — use the local weather simulation and keep vanilla dimension-wide weather clear; default `true`.
- `severeWeatherEnabled` — allow supercells, squalls, cyclones, tornadoes, hail, and sandstorms; default `true`.
- `volumetricCloudsEnabled` — enable GPU ray-marched storm and regional clouds; default `true`.
- `volumetricCloudQuality` — quality/performance preset `1` (up to 2 volumes), `2` (4), or `3` (6); default `2`.
- `cloudRenderDistance` — render distance in blocks (128–2048); cloud volumes fade at the edge.
- `severeWeatherSpawnMultiplier` and the per-hazard `*SpawnChance` values — tune natural storm frequency; all probabilities are bounded.
- `stormSizeMultiplier`, `stormStrengthMultiplier`, and `stormStrengtheningChance` — tune footprint, peak force, and mature-stage intensification.
- `maxConcurrentStorms` — maximum active natural/manual storm systems per world (hard cap 10).
- `weatherBlockDamage` — allow tornadoes and cyclones to remove blocks; default `false`. `maxBlocksPerStorm` and `maxDamageableBlockHardness` bound damage when enabled.

On first launch after the split, the mod imports the three legacy toggles (`regionalWeatherEnabled`, `severeWeatherEnabled`, and `weatherBlockDamage`) from an existing `config/mushoku_magic.json`, then writes the independent Weather config with defaults for the new storm controls. Edit the file and run `/magicweather reload` to reload it without restarting.

## Operator commands

With `regionalWeatherEnabled=true`, `/magicweather` affects a local 160-block radius and does not change other regions or the whole dimension:

- `/magicweather clear`, `cloudy`, `rain`, `thunder`, or `snow`
- `/magicweather hail`, `squall`, `supercell`, `tornado`, `cyclone`, or `sandstorm` for moving local hazards
- Add `10–3600` seconds to set a duration (default 120 seconds).
- `/magicweather status` reports a current manual override.
- `/magicweather reload` reloads `mushoku_weather.json`.

The commands are available from the Weather mod itself, without installing Magic. Operators need the normal permission level. Severe hazard commands require `severeWeatherEnabled=true`. If regional weather is disabled, use vanilla `/weather` instead.

## Build

Build the Fabric Weather jar from the repository root:

```sh
./gradlew :weather:build
```

Build the native Minecraft 1.20.1 ports:

```sh
./gradlew -p ports :weather-forge:build :weather-neoforge:build
```

The Fabric artifact version is `weather_version` in the root `gradle.properties`; native port versions are set by `weather_port_version` in `ports/gradle.properties`. Release bundles include a separate Weather jar for each supported loader.
