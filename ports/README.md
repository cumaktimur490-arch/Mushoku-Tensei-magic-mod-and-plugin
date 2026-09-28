# Minecraft 1.20.1 loader ports

This subproject builds two standalone mod jars from the shared gameplay sources:

- `forge` — native Forge 1.20.1 (`1.20.1-47.4.10`)
- `neoforge` — native NeoForge 1.20.1 (`1.20.1-47.1.106`)

The existing Fabric 1.21.11 project remains at the repository root and is not replaced by these ports. The two 1.20.1 outputs are compiled separately against their loader dependencies; they are not Fabric jars and do not use Sinytra Connector. They target Java 17 bytecode. A JDK 21 or newer is needed to run the pinned Loom plugin; the CI build uses JDK 25.

## Build

From the repository root:

```sh
./gradlew -p ports :forge:build :neoforge:build
```

Artifacts are written to `forge/build/libs/` and `neoforge/build/libs/` beneath this directory. Run the shared JUnit suite with either loader's `test` task, for example:

```sh
./gradlew -p ports :forge:test :neoforge:test
```

## Luxium

Only the Forge artifact declares [Luxium: Let there be light](https://www.curseforge.com/minecraft/mc-mods/luxium) and Embeddium as optional client-side dependencies. Neither is required for the magic mod itself. The port does not patch Luxium's renderer or call an unpublished Luxium API: spell and local-weather visuals continue to use Minecraft's particle system. Luxium's project page currently requires Embeddium and says that its rain and thunder rendering is not ready, so the port does not claim Luxium-specific weather rendering or visual enhancements.

Install only the artifact for the loader used by the Minecraft instance. Do not put both port jars in the same `mods` directory.
