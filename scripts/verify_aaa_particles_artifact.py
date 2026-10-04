#!/usr/bin/env python3
"""Validate optional AAA Particles resources and metadata in native Magic jars."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from zipfile import ZipFile

EFFECT_ROOT = "assets/mushoku_magic/effeks"
EFFECTS = {
    f"{EFFECT_ROOT}/explosion/main.efkefc": 8.0,
    f"{EFFECT_ROOT}/explosion_mini/blue.efkefc": 4.0,
    f"{EFFECT_ROOT}/explosion_mini/yellow.efkefc": 4.0,
}
LICENSE_PATH = "META-INF/licenses/AAA-Particles-World-MIT.txt"
OPTIONAL_DEPENDENCY = re.compile(
    r'\[\[dependencies\.mushoku_magic\]\]\s*'
    r'modId="aaa_particles"\s*'
    r'mandatory=false\s*'
    r'versionRange="\[[^\]]+\)"\s*'
    r'ordering="AFTER"\s*'
    r'side="CLIENT"',
    re.MULTILINE,
)


def verify(path: Path, loader: str) -> None:
    if not path.is_file():
        raise SystemExit(f"Missing {loader} Magic jar: {path}")
    with ZipFile(path) as jar:
        names = set(jar.namelist())
        metadata = jar.read("META-INF/mods.toml").decode("utf-8")
        if not OPTIONAL_DEPENDENCY.search(metadata):
            raise SystemExit(f"{path} does not declare AAA Particles as optional client-only")
        if "mod/chloeprime/aaaparticles/" in "\n".join(names):
            raise SystemExit(f"{path} must not bundle the AAA Particles API")
        if LICENSE_PATH not in names:
            raise SystemExit(f"{path} is missing the third-party effect asset license")
        for effect_path, intrinsic_size in EFFECTS.items():
            if effect_path not in names:
                raise SystemExit(f"{path} is missing Effekseer resource {effect_path}")
            if not jar.read(effect_path).startswith(b"EFKE"):
                raise SystemExit(f"{path} has an invalid Effekseer binary header: {effect_path}")
            metadata_path = effect_path + ".mcmeta"
            if metadata_path not in names:
                raise SystemExit(f"{path} is missing effect metadata {metadata_path}")
            metadata_json = json.loads(jar.read(metadata_path))
            if metadata_json.get("preload") is not True or metadata_json.get("size") != intrinsic_size:
                raise SystemExit(f"{path} has invalid/preload-incompatible metadata in {metadata_path}")
        for relative_resource in (
            "explosion/Texture/fire_tex.png",
            "explosion/Texture/smoke_tex.png",
            "explosion/Model/rock1.efkmodel",
            "explosion/01_NextSoft01/Texture/Fog2001.png",
            "explosion_mini/Texture/ColorNoise.png",
            "explosion_mini/Material/mat_eff_hit_himatsu.efkmat",
            "explosion_mini/Material/mat_eff_yugami.efkmatd",
        ):
            resource = f"{EFFECT_ROOT}/{relative_resource}"
            if resource not in names:
                raise SystemExit(f"{path} is missing Effekseer dependency {resource}")
    print(f"Verified optional AAA Particles resources in {loader} jar {path}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--forge", type=Path, required=True)
    parser.add_argument("--neoforge", type=Path, required=True)
    args = parser.parse_args()
    verify(args.forge, "Forge")
    verify(args.neoforge, "NeoForge")


if __name__ == "__main__":
    main()
