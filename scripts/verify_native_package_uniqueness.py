#!/usr/bin/env python3
"""Fail when paired native mod jars contain classes in the same Java package."""

from __future__ import annotations

import argparse
import glob
import sys
from pathlib import Path
from zipfile import BadZipFile, ZipFile


def resolve_jar(pattern: str, label: str) -> Path:
    matches = sorted(
        Path(match)
        for match in glob.glob(pattern, recursive=True)
        if Path(match).is_file() and not Path(match).stem.endswith("-sources")
    )
    if len(matches) != 1:
        raise SystemExit(f"Expected exactly one {label} jar matching {pattern!r}, found {matches}")
    return matches[0]


def class_packages(jar_path: Path) -> set[str]:
    packages: set[str] = set()
    try:
        with ZipFile(jar_path) as jar:
            for entry in jar.namelist():
                if not entry.endswith(".class"):
                    continue
                parts = entry.split("/")
                if len(parts) >= 4 and parts[0:2] == ["META-INF", "versions"] and parts[2].isdigit():
                    entry = "/".join(parts[3:])
                if entry == "module-info.class" or entry.endswith("/module-info.class"):
                    continue
                package_path = entry.rpartition("/")[0]
                packages.add(package_path.replace("/", ".") if package_path else "<default package>")
    except BadZipFile as error:
        raise SystemExit(f"Not a valid jar: {jar_path}: {error}") from error
    if not packages:
        raise SystemExit(f"No Java class packages found in {jar_path}")
    return packages


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--pair",
        nargs=3,
        action="append",
        required=True,
        metavar=("LOADER", "MAGIC_JAR_GLOB", "WEATHER_JAR_GLOB"),
        help="check the class-package sets of one loader's Magic and Weather jars",
    )
    args = parser.parse_args()

    for loader, magic_pattern, weather_pattern in args.pair:
        magic_jar = resolve_jar(magic_pattern, f"{loader} Magic")
        weather_jar = resolve_jar(weather_pattern, f"{loader} Weather")
        magic_packages = class_packages(magic_jar)
        weather_packages = class_packages(weather_jar)
        duplicates = sorted(magic_packages & weather_packages)
        if duplicates:
            joined = ", ".join(duplicates)
            raise SystemExit(
                f"{loader} Magic and Weather jars split Java packages: {joined}\n"
                f"  Magic: {magic_jar}\n"
                f"  Weather: {weather_jar}"
            )
        print(
            f"Verified {loader}: {len(magic_packages)} Magic packages and "
            f"{len(weather_packages)} Weather packages are disjoint"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
