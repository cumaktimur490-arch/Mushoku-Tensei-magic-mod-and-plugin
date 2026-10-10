#!/usr/bin/env python3
"""Adapt the Terra Realis v3.0.0 checkout to Minecraft 1.21.11 without replacing vanilla Default."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

GENERATOR = Path("src/main/java/dev/terrarealis/fabric/worldgen/RealisChunkGenerator.java")
BIOME_SOURCE = Path("src/main/java/dev/terrarealis/fabric/worldgen/RealisBiomeSource.java")
MATERIAL_BLOCKS = Path("src/main/java/dev/terrarealis/fabric/worldgen/MaterialBlocks.java")
COMMAND = Path("src/main/java/dev/terrarealis/fabric/command/RealisCommand.java")
METADATA = Path("src/main/resources/fabric.mod.json")
VANILLA_PRESET = Path("src/main/resources/data/minecraft/worldgen/world_preset/normal.json")
REALIS_PRESET = Path("src/main/resources/data/terra_realis/worldgen/world_preset/realis.json")


def replace_exact(source: str, old: str, new: str, expected_count: int, path: Path) -> str:
    count = source.count(old)
    if count != expected_count:
        raise ValueError(f"Expected {expected_count} occurrences of {old!r} in {path}, found {count}")
    return source.replace(old, new)


def patch(source_root: Path) -> None:
    paths = (GENERATOR, BIOME_SOURCE, MATERIAL_BLOCKS, COMMAND, METADATA, VANILLA_PRESET, REALIS_PRESET)
    for relative_path in paths:
        path = source_root / relative_path
        if not path.is_file():
            raise ValueError(f"Terra Realis v3.0.0 source is missing {path}")

    generator_path = source_root / GENERATOR
    source = generator_path.read_text(encoding="utf-8")
    for old, new, count in (
        (
            "net.minecraft.world.level.levelgen.chunk.",
            "net.minecraft.world.level.chunk.",
            2,
        ),
        (
            "net.minecraft.world.level.levelgen.Blender",
            "net.minecraft.world.level.levelgen.blending.Blender",
            1,
        ),
        (
            "net.minecraft.world.level.levelgen.NoiseColumn",
            "net.minecraft.world.level.NoiseColumn",
            1,
        ),
        (
            "net.minecraft.world.level.levelgen.structure.StructureManager",
            "net.minecraft.world.level.StructureManager",
            1,
        ),
        ("chunk.setBlockState(cursor, snow, false)", "chunk.setBlockState(cursor, snow, 0)", 2),
        ("chunk.setBlockState(cursor, state, false)", "chunk.setBlockState(cursor, state, 0)", 1),
    ):
        source = replace_exact(source, old, new, count, generator_path)
    generator_path.write_text(source, encoding="utf-8")

    biome_source_path = source_root / BIOME_SOURCE
    source = biome_source_path.read_text(encoding="utf-8")
    source = replace_exact(
        source,
        "getter.get(key).orElse(this.fallback)",
        "getter.get(key).map(holder -> (Holder<Biome>) holder).orElse(this.fallback)",
        1,
        biome_source_path,
    )
    biome_source_path.write_text(source, encoding="utf-8")

    material_blocks_path = source_root / MATERIAL_BLOCKS
    source = material_blocks_path.read_text(encoding="utf-8")
    source = replace_exact(source, "Blocks.SNOW_LAYER", "Blocks.SNOW", 2, material_blocks_path)
    material_blocks_path.write_text(source, encoding="utf-8")

    command_path = source_root / COMMAND
    source = command_path.read_text(encoding="utf-8")
    source = replace_exact(
        source,
        "import net.minecraft.network.chat.Component;",
        "import net.minecraft.network.chat.Component;\nimport net.minecraft.server.permissions.Permissions;",
        1,
        command_path,
    )
    source = replace_exact(
        source,
        ".requires(s -> s.hasPermission(2))",
        ".requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))",
        1,
        command_path,
    )
    command_path.write_text(source, encoding="utf-8")

    metadata_path = source_root / METADATA
    source = metadata_path.read_text(encoding="utf-8")
    source = replace_exact(
        source,
        '"note": "The generator overrides the vanilla Default world preset via data/minecraft/worldgen/world_preset/normal.json. Delete that one file to restore vanilla terrain."',
        '"note": "The generator is available as a separate Terra Realis preset at data/terra_realis/worldgen/world_preset/realis.json. The vanilla Default world preset is unchanged."',
        1,
        metadata_path,
    )
    metadata_path.write_text(source, encoding="utf-8")

    vanilla_preset_path = source_root / VANILLA_PRESET
    vanilla_preset = json.loads(vanilla_preset_path.read_text(encoding="utf-8"))
    generator_type = (
        vanilla_preset.get("dimensions", {})
        .get("minecraft:overworld", {})
        .get("generator", {})
        .get("type")
    )
    if generator_type != "terra_realis:realis":
        raise ValueError(f"Refusing to remove unexpected vanilla preset override in {vanilla_preset_path}")

    realis_preset_path = source_root / REALIS_PRESET
    realis_preset = json.loads(realis_preset_path.read_text(encoding="utf-8"))
    realis_generator_type = (
        realis_preset.get("dimensions", {})
        .get("minecraft:overworld", {})
        .get("generator", {})
        .get("type")
    )
    if realis_generator_type != "terra_realis:realis":
        raise ValueError(f"Terra Realis preset is missing its generator in {realis_preset_path}")

    vanilla_preset_path.unlink()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_root", type=Path, help="checkout directory for the v3.0.0 source")
    args = parser.parse_args()
    try:
        patch(args.source_root)
    except (OSError, json.JSONDecodeError, ValueError) as error:
        parser.error(str(error))
    print(f"Applied Minecraft 1.21.11 API fixes and preserved the vanilla Default preset in {args.source_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
