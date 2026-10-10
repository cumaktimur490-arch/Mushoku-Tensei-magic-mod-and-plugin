#!/usr/bin/env python3
"""Apply the small Minecraft 1.21.11 API compatibility patch to the v3.0.0 source checkout."""

from __future__ import annotations

import argparse
from pathlib import Path

GENERATOR = Path("src/main/java/dev/terrarealis/fabric/worldgen/RealisChunkGenerator.java")


def patch(source_root: Path) -> None:
    path = source_root / GENERATOR
    if not path.is_file():
        raise ValueError(f"Terra Realis v3.0.0 source is missing {path}")

    source = path.read_text(encoding="utf-8")
    package_reference = "net.minecraft.world.level.levelgen.chunk."
    if source.count(package_reference) < 2:
        raise ValueError("Expected obsolete ChunkGenerator package references in v3.0.0 source")
    source = source.replace(package_reference, "net.minecraft.world.level.chunk.")

    replacements = (
        ("chunk.setBlockState(cursor, snow, false)", "chunk.setBlockState(cursor, snow, 0)", 2),
        ("chunk.setBlockState(cursor, state, false)", "chunk.setBlockState(cursor, state, 0)", 1),
    )
    for old, new, expected_count in replacements:
        actual_count = source.count(old)
        if actual_count != expected_count:
            raise ValueError(
                f"Expected {expected_count} occurrences of {old!r}, found {actual_count}"
            )
        source = source.replace(old, new)

    path.write_text(source, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_root", type=Path, help="checkout directory for the v3.0.0 source")
    args = parser.parse_args()
    try:
        patch(args.source_root)
    except ValueError as error:
        parser.error(str(error))
    print(f"Applied 1.21.11 chunk API compatibility fixes in {args.source_root / GENERATOR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
