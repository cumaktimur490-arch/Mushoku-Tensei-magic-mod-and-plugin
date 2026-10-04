# AAA Particles integration

The Forge and NeoForge 1.20.1 Magic artifacts have an optional client-side integration with [AAA Particles](https://modrinth.com/mod/En8uHTOK). It plays Effekseer effects for fire, water/ice, and earth spell bursts. The existing vanilla particles remain enabled as the fallback and are still sent to players without AAA Particles; neither the dedicated server nor players who do not use AAA Particles need the library installed.

The integration sends only a small, range-limited effect message from the logical server to nearby clients. The client then calls AAA Particles through a guarded compatibility bridge. The bridge has no compile-time or bundled dependency on AAA Particles and safely does nothing when the library/API is absent. AAA Particles is detected by its mod id `aaa_particles`; use a release that supports Minecraft 1.20.1 (for example, AAA Particles 2.2.3 for Forge/NeoForge).

## Loader support

| Magic artifact | AAA Particles integration |
| --- | --- |
| Forge 1.20.1 | Supported when AAA Particles is installed client-side |
| NeoForge 1.20.1 | Supported when AAA Particles is installed client-side |
| Fabric 1.21.11 | Not advertised as supported: AAA Particles has no Fabric build for Minecraft 1.21.11 in its published compatibility list. Vanilla spell particles remain the fallback. |

This is an additive effect layer, not a replacement for Minecraft's particles. The distributed Effekseer assets are used under the MIT license; the license is included in the Magic JAR at `META-INF/licenses/AAA-Particles-World-MIT.txt`.

## Third-party effect assets

The included `explosion` and `explosion_mini` Effekseer assets are from [ChloePrime/AAA-Particles-World](https://github.com/ChloePrime/AAA-Particles-World), source commit [`dda9af0`](https://github.com/ChloePrime/AAA-Particles-World/commit/dda9af0afc60f03ce9228bc1b0246d3e1eccc5d3), copyright ChloePrime, MIT licensed. They are packaged under `assets/mushoku_magic/effeks/` and are only activated when the optional AAA Particles API is available. The effect metadata preloads the three spell-burst definitions at resource reload to avoid loading their Effekseer assets on the first cast.

No in-game visual or performance QA has been performed for this beta integration; automated CI verifies the builds and resource packaging, not the final rendered result on a player's graphics driver or mod list.
