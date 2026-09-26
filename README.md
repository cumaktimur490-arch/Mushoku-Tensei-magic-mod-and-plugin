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

Requires JDK 21. The Gradle wrapper downloads the pinned Gradle version.

```sh
./gradlew clean build
```

The mod jar is produced at `build/libs/mushoku-magic-1.0.0.jar`. The test suite checks the staff defaults and legacy-config migration. GitHub Actions runs the same build and tests on pushes and pull requests.
