#!/usr/bin/env python3
"""Package each release's loader-matched mod jars into a simple mods/ ZIP."""

from __future__ import annotations

import argparse
import re
import sys
import zipfile
from pathlib import Path

ZIP_TIMESTAMP = (2020, 1, 1, 0, 0, 0)


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def make_bundle(output: Path, loader: str, minecraft: str, tag: str, jars: tuple[Path, ...]) -> None:
    output.mkdir(parents=True, exist_ok=True)
    archive_path = output / f"mushoku-tensei-mods-{loader}-mc{minecraft}-{tag}.zip"
    expected_names = {"mods/"} | {f"mods/{jar.name}" for jar in jars}

    with zipfile.ZipFile(archive_path, "w") as archive:
        directory = zipfile.ZipInfo("mods/", ZIP_TIMESTAMP)
        directory.create_system = 3
        directory.external_attr = (0o40755 << 16) | 0x10
        archive.writestr(directory, b"")
        for jar in jars:
            if not jar.is_file():
                raise SystemExit(f"Missing release jar: {jar}")
            with zipfile.ZipFile(jar) as packaged_mod:
                broken_member = packaged_mod.testzip()
                if broken_member is not None:
                    raise SystemExit(f"Invalid jar {jar}: corrupt member {broken_member}")
            member = zipfile.ZipInfo(f"mods/{jar.name}", ZIP_TIMESTAMP)
            member.create_system = 3
            member.external_attr = 0o100644 << 16
            member.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(member, jar.read_bytes(), compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

    with zipfile.ZipFile(archive_path) as archive:
        actual_names = set(archive.namelist())
        if actual_names != expected_names:
            archive_path.unlink(missing_ok=True)
            raise SystemExit(f"Unexpected contents in {archive_path}: {sorted(actual_names)}")
        if archive.testzip() is not None:
            archive_path.unlink(missing_ok=True)
            raise SystemExit(f"Corrupt bundle: {archive_path}")
    print(f"Packaged {loader} {minecraft}: {archive_path} ({len(jars)} mods)")


def locate(assets: Path, filename: str) -> Path:
    matches = list(assets.rglob(filename))
    if len(matches) != 1:
        raise SystemExit(f"Expected one release asset named {filename}, found {matches}")
    return matches[0]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets-dir", type=Path, required=True, help="directory containing the nine release JARs")
    parser.add_argument("--output-dir", type=Path, required=True, help="directory where the three ZIPs are written")
    parser.add_argument("--tag", required=True, help="release tag, for example v2.5.9-beta.2")
    args = parser.parse_args()

    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", args.tag):
        raise SystemExit(f"Unsafe release tag for bundle filename: {args.tag}")
    if not args.assets_dir.is_dir():
        raise SystemExit(f"Release assets directory does not exist: {args.assets_dir}")

    fabric = read_properties(Path("gradle.properties"))
    ports = read_properties(Path("ports/gradle.properties"))
    expected_versions = (
        "mod_version", "weather_version", "worldgen_version",
    )
    port_versions = (
        "port_version", "weather_port_version", "worldgen_port_version",
    )
    for key in expected_versions:
        if key not in fabric:
            raise SystemExit(f"Missing {key} in gradle.properties")
    for key in port_versions:
        if key not in ports:
            raise SystemExit(f"Missing {key} in ports/gradle.properties")
    expected_tag = f"v{fabric['mod_version']}"
    if args.tag != expected_tag and not args.tag.startswith(expected_tag + "-"):
        raise SystemExit(f"Release tag {args.tag} does not match this source version {expected_tag}")

    fabric_jars = (
        locate(args.assets_dir, f"mushoku-magic-{fabric['mod_version']}.jar"),
        locate(args.assets_dir, f"mushoku-weather-{fabric['weather_version']}.jar"),
        locate(args.assets_dir, f"mushoku-worldgen-{fabric['worldgen_version']}.jar"),
    )
    forge_jars = (
        locate(args.assets_dir, f"mushoku-magic-forge-1.20.1-{ports['port_version']}.jar"),
        locate(args.assets_dir, f"mushoku-weather-forge-1.20.1-{ports['weather_port_version']}.jar"),
        locate(args.assets_dir, f"mushoku-worldgen-forge-1.20.1-{ports['worldgen_port_version']}.jar"),
    )
    neoforge_jars = (
        locate(args.assets_dir, f"mushoku-magic-neoforge-1.20.1-{ports['port_version']}.jar"),
        locate(args.assets_dir, f"mushoku-weather-neoforge-1.20.1-{ports['weather_port_version']}.jar"),
        locate(args.assets_dir, f"mushoku-worldgen-neoforge-1.20.1-{ports['worldgen_port_version']}.jar"),
    )

    make_bundle(args.output_dir, "fabric", "1.21.11", args.tag, fabric_jars)
    make_bundle(args.output_dir, "forge", "1.20.1", args.tag, forge_jars)
    make_bundle(args.output_dir, "neoforge", "1.20.1", args.tag, neoforge_jars)
    return 0


if __name__ == "__main__":
    sys.exit(main())
