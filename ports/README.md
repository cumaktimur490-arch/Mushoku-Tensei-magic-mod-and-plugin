# Native Minecraft 1.20.1 loader builds

The `ports/` Gradle build produces separate native Weather and Magic mods for both loaders:

- Forge 1.20.1 (`1.20.1-47.4.10`): `mushoku-weather-forge-1.20.1-1.1.3.jar` and `mushoku-magic-forge-1.20.1-2.5.3.jar`
- NeoForge 1.20.1 (`1.20.1-47.1.106`): `mushoku-weather-neoforge-1.20.1-1.1.3.jar` and `mushoku-magic-neoforge-1.20.1-2.5.3.jar`

Install the Weather jar by itself for standalone local weather. The Magic mod has a required Weather dependency, so install both matching jars to use Magic. Never mix the Forge and NeoForge jars or install a port jar in a Fabric instance. These are native builds and do not use Sinytra Connector. NeoForge 1.20.1 is its legacy Forge-compatible 47.1.x line, so Loom uses its Forge platform for that module while the dependency and published artifact remain native NeoForge.

Weather owns the regional simulation, local Cumulonimbus snapshots, rotating supercells, squalls and other severe storms, weather configuration, and `/magicweather` commands. It never toggles server-wide rain. Magic accesses Weather's API for Cumulonimbus and bounded spell/weather interactions; the dependency goes only from Magic to Weather. Local precipitation rendering is restored when a client leaves a Cumulonimbus sector. Forge and NeoForge 1.20.1 use the compatible Forge `SimpleChannel` API for storm snapshots.

Both mods target Java 17 bytecode. A JDK 21 or newer is needed to run the pinned Loom plugin; CI uses JDK 25.

## Build

From the repository root:

```sh
./gradlew -p ports :weather-forge:build :weather-neoforge:build :forge:build :neoforge:build
```

Artifacts are written to `ports/weather-forge/build/libs/`, `ports/weather-neoforge/build/libs/`, `ports/forge/build/libs/`, and `ports/neoforge/build/libs/`. Each standalone mod has its own test suite; run all four loader test tasks with:

```sh
./gradlew -p ports :weather-forge:test :weather-neoforge:test :forge:test :neoforge:test
```

## Luxium

Only the Forge **Magic** artifact declares [Luxium: Let there be light](https://www.curseforge.com/minecraft/mc-mods/luxium) and Embeddium as optional client-side dependencies. Neither is required. The standalone Weather artifact declares no Luxium dependency and does not patch its renderer or call an unpublished API. Local rain uses vanilla's renderer; Luxium's project page currently requires Embeddium and says rain and thunder rendering is not ready yet.

**Compatibility note:** Luxium may trigger Embeddium's `MixinTaintDetector` because Luxium modifies Embeddium's `SodiumWorldRenderer`. That diagnostic comes from the Luxium/Embeddium combination, not from a Mushoku mixin, and does not by itself indicate a failed launch. If you encounter rendering instability, test without Luxium and report it to its maintainers.
