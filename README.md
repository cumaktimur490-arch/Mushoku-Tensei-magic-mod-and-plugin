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

Version 2.0.1 is built as `build/libs/mushoku-magic-2.0.1.jar`; its Fabric metadata version is generated from `mod_version` in `gradle.properties`. The three supplied wand textures are alpha-trimmed but not resized, preserving their original pixel detail at 103×123, 242×604, and 117×85 pixels. Minecraft's displayed sharpness still depends on the in-game render size and filtering. The test suite checks the staff defaults and legacy-config migration. GitHub Actions runs the same build and tests on pushes and pull requests. Download the release jar from the repository's [Releases](https://github.com/cumaktimur490-arch/Mushoku-Tensei-magic-mod-and-plugin/releases) page.
