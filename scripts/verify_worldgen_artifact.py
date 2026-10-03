#!/usr/bin/env python3
"""Verify loader metadata and data resources in a Mushoku Worldgen jar."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from zipfile import BadZipFile, ZipFile

CUSTOM_BIOMES = (
    "golden_steppe",
    "riverside_meadow",
    "whispering_forest",
    "emerald_highlands",
    "skyreach_mountains",
)
RETAINED_VANILLA_BIOMES = {
    "minecraft:deep_ocean",
    "minecraft:ocean",
    "minecraft:beach",
    "minecraft:frozen_river",
    "minecraft:river",
    "minecraft:lush_caves",
    "minecraft:dripstone_caves",
    "minecraft:deep_dark",
}
PRESET = "data/mushoku_worldgen/worldgen/world_preset/mushoku_world.json"
NORMAL_TAG = "data/minecraft/tags/worldgen/world_preset/normal.json"


def load_json(jar: ZipFile, path: str) -> dict:
    try:
        return json.loads(jar.read(path))
    except (KeyError, json.JSONDecodeError) as error:
        raise ValueError(f"Missing or invalid JSON resource {path}: {error}") from error


def verify(jar_path: Path, loader: str) -> None:
    if not jar_path.is_file():
        raise ValueError(f"Worldgen artifact does not exist: {jar_path}")

    try:
        with ZipFile(jar_path) as jar:
            names = set(jar.namelist())
            expected_pack_format = 75 if loader == "fabric" else 15
            required = {
                "pack.mcmeta",
                PRESET,
                NORMAL_TAG,
                "data/minecraft/tags/worldgen/biome/is_overworld.json",
                "data/minecraft/tags/worldgen/biome/has_structure/village_plains.json",
                "data/minecraft/tags/worldgen/biome/has_structure/pillager_outpost.json",
                "assets/mushoku_worldgen/lang/en_us.json",
                "assets/mushoku_worldgen/lang/ru_ru.json",
            }
            required.update(
                f"data/mushoku_worldgen/worldgen/biome/{biome}.json"
                for biome in CUSTOM_BIOMES
            )
            missing = sorted(required - names)
            if missing:
                raise ValueError(f"{jar_path} is missing resources: {missing}")

            if loader == "fabric":
                if "META-INF/mods.toml" in names:
                    raise ValueError(f"{jar_path} incorrectly includes native loader metadata")
                metadata = load_json(jar, "fabric.mod.json")
                if metadata.get("id") != "mushoku_worldgen":
                    raise ValueError(f"{jar_path} has the wrong Fabric mod id")
                dependencies = metadata.get("depends", {})
                if "mushoku_magic" in dependencies or "mushoku_weather" in dependencies:
                    raise ValueError("Worldgen must be standalone from Magic and Weather")
                if "com/mushokumagic/worldgen/MushokuWorldgen.class" not in names:
                    raise ValueError(f"{jar_path} is missing its Fabric entrypoint")
            else:
                if "fabric.mod.json" in names:
                    raise ValueError(f"{jar_path} incorrectly includes Fabric metadata")
                metadata = jar.read("META-INF/mods.toml").decode("utf-8")
                if 'modId="mushoku_worldgen"' not in metadata:
                    raise ValueError(f"{jar_path} has the wrong native mod id")
                if 'modId="mushoku_magic"' in metadata or 'modId="mushoku_weather"' in metadata:
                    raise ValueError("Worldgen must not depend on Magic or Weather")
                if 'versionRange="[1.20.1,1.20.2)"' not in metadata:
                    raise ValueError(f"{jar_path} does not target Minecraft 1.20.1")
                if "com/mushokumagic/worldgen/MushokuWorldgen.class" not in names:
                    raise ValueError(f"{jar_path} is missing its native @Mod entrypoint")

            pack = load_json(jar, "pack.mcmeta").get("pack", {})
            if pack.get("pack_format") != expected_pack_format:
                raise ValueError(
                    f"{jar_path} has pack format {pack.get('pack_format')}; "
                    f"expected {expected_pack_format} for {loader}"
                )

            preset = load_json(jar, PRESET)
            dimensions = preset.get("dimensions", {})
            if set(dimensions) != {
                "minecraft:overworld",
                "minecraft:the_nether",
                "minecraft:the_end",
            }:
                raise ValueError(f"{jar_path} has an incomplete dimension set")
            overworld = dimensions["minecraft:overworld"].get("generator", {})
            if overworld.get("settings") != "minecraft:overworld":
                raise ValueError(f"{jar_path} would alter the vanilla Overworld noise settings")
            entries = overworld.get("biome_source", {}).get("biomes", [])
            biome_ids = {entry.get("biome") for entry in entries}
            expected_biomes = {
                f"mushoku_worldgen:{biome}" for biome in CUSTOM_BIOMES
            } | RETAINED_VANILLA_BIOMES
            if biome_ids != expected_biomes:
                raise ValueError(
                    f"{jar_path} has an unexpected biome palette: "
                    f"missing={sorted(expected_biomes - biome_ids)}, "
                    f"extra={sorted(biome_ids - expected_biomes)}"
                )
            for biome in CUSTOM_BIOMES:
                biome_id = f"mushoku_worldgen:{biome}"
                if biome_id not in biome_ids:
                    raise ValueError(f"{jar_path} preset does not include {biome_id}")
                definition = load_json(
                    jar,
                    f"data/mushoku_worldgen/worldgen/biome/{biome}.json",
                )
                if len(definition.get("features", [])) != 11:
                    raise ValueError(f"{jar_path} biome {biome_id} has invalid feature stages")

            normal_tag = load_json(jar, NORMAL_TAG)
            if normal_tag.get("replace", False):
                raise ValueError(f"{jar_path} replaces rather than extends normal presets")
            if "mushoku_worldgen:mushoku_world" not in normal_tag.get("values", []):
                raise ValueError(f"{jar_path} preset is not discoverable from normal worlds")
    except BadZipFile as error:
        raise ValueError(f"Not a valid jar: {jar_path}: {error}") from error


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--loader", choices=("fabric", "forge", "neoforge"), required=True)
    parser.add_argument("jar", type=Path)
    args = parser.parse_args()
    try:
        verify(args.jar, args.loader)
    except (OSError, UnicodeDecodeError, ValueError) as error:
        print(error, file=sys.stderr)
        return 1
    print(f"Verified {args.loader} Mushoku Worldgen jar: {args.jar}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
