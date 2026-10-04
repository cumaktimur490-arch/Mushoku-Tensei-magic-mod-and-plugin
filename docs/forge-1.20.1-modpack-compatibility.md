# Forge 1.20.1 modpack compatibility notes

This is a static compatibility review of the Forge 1.20.1 mod list supplied for the Mushoku mods. No complete third-party mod folder, crash log, in-game world, or profiler capture was available, so this is not a claim that the entire pack has passed runtime testing.

## Mushoku artifacts

Use the native Forge 1.20.1 artifacts together: Mushoku Weather runs by itself; Mushoku Magic requires the matching Weather artifact; Mushoku Worldgen is optional and adds the **Mushoku: Vast Lands** preset. These native jars do not need Sinytra Connector. The preset is opt-in and leaves the ordinary `minecraft:normal` Overworld untouched.

The Worldgen data now appends each custom biome to only its semantically appropriate vanilla category/structure tags. Forest, taiga, hill, mountain and village tags let tag-driven mods and structure datapacks recognize the new biomes. The mapping is checked in both source resources and packaged Fabric/Forge/NeoForge artifacts. This helps integrations that use those vanilla tags; it cannot guarantee that every Dynamic Trees species pack or every MVS structure has a rule for every custom biome.

## Performance and rendering observations

- The supplied pack already contains ModernFix, FerriteCore, Embeddium, Embeddium Extra, EntityCulling and Distant Horizons. Worldgen does not bundle another renderer or duplicate performance library. Existing density-field overlays use Minecraft's flat/2D cache for their horizontal terrain signals; no measured chunk-time improvement is claimed without a profiler run.
- Dynamic Trees, Dynamic Trees for Quark, Quark and [Moog's Voyager Structures](https://modrinth.com/datapack/mvs-moogs-voyager-structure-config-pack) are not hard dependencies of Worldgen. Forest/taiga/hill/mountain tags improve tag-based matching. Their tree replacement, feature selection and structure frequency still depend on the actual addon/config data and need a fresh-world test.
- Distant Horizons' initial LOD build and structure-heavy terrain can increase CPU and disk work. Start with a modest LOD distance, allow generation to catch up, and raise it only after observing server tick time and client memory. On a dedicated server, pre-generate gradually rather than requesting a huge radius at once.
- [Particle Rain](https://modrinth.com/project/nrikgvxm) replaces vanilla precipitation rendering and also adds wind, haze and dust effects. Mushoku Weather supplies local storm state and precipitation inside bounded sectors. These layers may overlap visually or increase particle cost; if that happens, disable the duplicate precipitation/haze effects in `/particlerain` while keeping the Mushoku Weather simulation enabled. This combination still needs in-game QA.
- Fog Overrides and Luxium operate in the client rendering stack, not in biome/noise generation. Luxium `2.8.0-pre-alpha` is an experimental renderer in this pack; if clouds, fog or rain look incorrect, first test without Luxium and other fog overrides. Mushoku Weather does not patch Luxium's renderer.
- The listed [`dynamiclights-v1.9-mc1.17-1.21.9-mod.jar`](https://modrinth.com/datapack/dynamic-lights/version/Yyh6uR59) filename matches a Modrinth artifact whose published loader list is Fabric, NeoForge and Quilt, not native Forge. The pack also includes Connector, which may be intentionally used for Fabric compatibility, but this is not equivalent to a native Forge build. Check the installed jar's loader metadata and launch log; replace it with a Forge-native build if Connector cannot load it cleanly or if client-side dynamic lighting does not work.
- The [`Emoji Type 2.2.3+1.20.4`](https://modrinth.com/mod/emoji-type/version/bOTB1mD7) and [`InvMoveCompats 0.5.0+1.20.4-forge`](https://github.com/PieKing1215/InvMoveCompats/releases) suffixes alone do not prove a version mismatch: their published Forge builds cover the 1.20.0–1.20.4 range, which includes 1.20.1. Keep the Forge-specific files, not Fabric/NeoForge variants.

## What to test in Minecraft

1. Launch Forge 1.20.1 with the three matching Mushoku jars and the listed pack; save the full `latest.log` if ModLauncher reports a missing dependency, failed mixin, or rejected mod loader.
2. Create a **new** Mushoku: Vast Lands world. Check that Golden Steppe, Riverside Meadow, Whispering Forest, Emerald Highlands and Skyreach Mountains all appear, then confirm villages/outposts and third-party structures in newly generated chunks.
3. In a fresh chunk, inspect vegetation under forest canopies and check whether Dynamic Trees for Quark replaces the intended tree features without duplicate vanilla/Dynamic Trees trees.
4. With `regionalWeatherEnabled=true`, compare Particle Rain enabled and disabled around a local storm boundary; watch for duplicate rain, fog, wind and sand effects.
5. Record chunk-generation and client frame-time before/after lowering Distant Horizons LOD distance. No in-game visual, compatibility, memory or performance QA has been performed for this profile yet.

The exact tag membership and jar contents are statically verified by `scripts/generate_worldgen_resources.py --check` and `scripts/verify_worldgen_artifact.py`; these checks do not launch Minecraft or substitute for the steps above.
