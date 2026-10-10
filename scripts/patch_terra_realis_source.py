#!/usr/bin/env python3
"""Apply Minecraft 1.21.11 API compatibility fixes to the v3.0.0 source checkout."""

from __future__ import annotations

import argparse
from pathlib import Path

GENERATOR = Path("src/main/java/dev/terrarealis/fabric/worldgen/RealisChunkGenerator.java")
BIOME_SOURCE = Path("src/main/java/dev/terrarealis/fabric/worldgen/RealisBiomeSource.java")


def replace_exact(source: str, old: str, new: str, expected_count: int, path: Path) -> str:
    count = source.count(old)
    if count != expected_count:
        raise ValueError(f"Expected {expected_count} occurrences of {old!r} in {path}, found {count}")
    return source.replace(old, new)


def patch(source_root: Path) -> None:
    generator_path = source_root / GENERATOR
    biome_source_path = source_root / BIOME_SOURCE
    for path in (generator_path, biome_source_path):
        if not path.is_file():
            raise ValueError(f"Terra Realis v3.0.0 source is missing {path}")

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

    source = biome_source_path.read_text(encoding="utf-8")
    source = replace_exact(
        source,
        "getter.get(key).orElse(this.fallback)",
        "getter.get(key).map(holder -> (Holder<Biome>) holder).orElse(this.fallback)",
        1,
        biome_source_path,
    )
    biome_source_path.write_text(source, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_root", type=Path, help="checkout directory for the v3.0.0 source")
    args = parser.parse_args()
    try:
        patch(args.source_root)
    except ValueError as error:
        parser.error(str(error))
    print(f"Applied Minecraft 1.21.11 API compatibility fixes in {args.source_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
