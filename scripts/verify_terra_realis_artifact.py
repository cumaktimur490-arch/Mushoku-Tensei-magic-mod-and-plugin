#!/usr/bin/env python3
"""Verify the complete Fabric mod jar built from the Terra Realis v3.0.0 tag."""

from __future__ import annotations

import argparse
import json
import struct
import sys
from pathlib import Path
from zipfile import BadZipFile, ZipFile

EXPECTED_VERSION = "3.0.0"
EXPECTED_ID = "terra_realis"
VANILLA_DEFAULT_PRESET = "data/minecraft/worldgen/world_preset/normal.json"
REALIS_PRESET = "data/terra_realis/worldgen/world_preset/realis.json"
REQUIRED_RESOURCES = (
    REALIS_PRESET,
    "assets/terra_realis/lang/en_us.json",
)
REQUIRED_CLASSES = (
    "dev/terrarealis/fabric/TerraRealisMod.class",
    "dev/terrarealis/fabric/worldgen/RealisChunkGenerator.class",
    "dev/terrarealis/fabric/worldgen/RealisBiomeSource.class",
    "dev/terrarealis/kernel/TerraKernel.class",
)


def verify(jar_path: Path) -> None:
    if not jar_path.is_file():
        raise ValueError(f"Terra Realis mod jar does not exist: {jar_path}")

    try:
        with ZipFile(jar_path) as jar:
            names = set(jar.namelist())
            if "fabric.mod.json" not in names:
                raise ValueError(f"{jar_path} is missing Fabric mod metadata")
            try:
                metadata = json.loads(jar.read("fabric.mod.json"))
            except json.JSONDecodeError as error:
                raise ValueError(f"{jar_path} has invalid Fabric metadata: {error}") from error

            if metadata.get("id") != EXPECTED_ID or metadata.get("version") != EXPECTED_VERSION:
                raise ValueError(f"{jar_path} has the wrong mod id or version")
            dependencies = metadata.get("depends", {})
            if not {
                "fabricloader",
                "minecraft",
                "java",
                "fabric-api",
            }.issubset(dependencies):
                raise ValueError(f"{jar_path} is missing a required Fabric dependency")
            if dependencies.get("minecraft") != "~1.21.11":
                raise ValueError(f"{jar_path} does not target Minecraft 1.21.11")

            if VANILLA_DEFAULT_PRESET in names:
                raise ValueError(
                    f"{jar_path} overrides vanilla Default via {VANILLA_DEFAULT_PRESET}; "
                    "the standard Overworld must remain unchanged"
                )

            missing = [
                path
                for path in (*REQUIRED_RESOURCES, *REQUIRED_CLASSES)
                if path not in names
            ]
            if missing:
                raise ValueError(f"{jar_path} is incomplete; missing: {', '.join(missing)}")

            realis = json.loads(jar.read(REALIS_PRESET))
            generator = (
                realis.get("dimensions", {})
                .get("minecraft:overworld", {})
                .get("generator", {})
            )
            if generator.get("type") != "terra_realis:realis":
                raise ValueError(f"{jar_path} does not expose Terra Realis as a separate world preset")

            mod_class = jar.read(REQUIRED_CLASSES[0])
            if len(mod_class) < 8 or struct.unpack(">H", mod_class[6:8])[0] != 65:
                raise ValueError(f"{jar_path} is not compiled for Java 21 bytecode")
    except (BadZipFile, KeyError, json.JSONDecodeError) as error:
        raise ValueError(f"Unable to inspect Terra Realis jar {jar_path}: {error}") from error


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path, help="Terra Realis Fabric jar built from v3.0.0")
    args = parser.parse_args()
    try:
        verify(args.jar)
    except ValueError as error:
        print(f"Terra Realis artifact verification failed: {error}", file=sys.stderr)
        return 1
    print(f"Verified complete Terra Realis Fabric 1.21.11 jar with vanilla Default preserved: {args.jar}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
