#!/usr/bin/env python3
"""Generate and verify the version-specific Mushoku World data resources."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
FABRIC_RESOURCES = ROOT / "worldgen/src/main/resources"
LEGACY_RESOURCES = ROOT / "ports/worldgen-common/src/main/resources"

BIOMES: dict[str, dict[str, Any]] = {
    "golden_steppe": {
        "name_en": "Golden Steppe",
        "name_ru": "Золотая степь",
        "temperature": 0.8,
        "downfall": 0.35,
        "grass_color": 0xB4AA54,
        "foliage_color": 0x9D9B4F,
        "water_color": 0x477EC2,
        "sky_color": 0x78A7FF,
        "fog_color": 0xD9E4E8,
        "vegetation": [
            "minecraft:trees_plains",
            "minecraft:flower_plains",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_grass_plain",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
        ],
    },
    "riverside_meadow": {
        "name_en": "Riverside Meadow",
        "name_ru": "Речные луга",
        "temperature": 0.55,
        "downfall": 0.75,
        "grass_color": 0x86B95B,
        "foliage_color": 0x6CA05C,
        "water_color": 0x4A84BF,
        "sky_color": 0x78A7FF,
        "fog_color": 0xD5E9E8,
        "vegetation": [
            "minecraft:flower_meadow",
            "minecraft:trees_meadow",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_grass_plain",
            "minecraft:patch_sugar_cane",
        ],
        "vegetation_fabric": [
            "minecraft:flower_meadow",
            "minecraft:trees_meadow",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_grass_meadow",
            "minecraft:wildflowers_meadow",
            "minecraft:patch_sugar_cane",
        ],
    },
    "whispering_forest": {
        "name_en": "Whispering Forest",
        "name_ru": "Шепчущий лес",
        "temperature": 0.7,
        "downfall": 0.8,
        "grass_color": 0x79A65A,
        "foliage_color": 0x4F8951,
        "water_color": 0x367B87,
        "sky_color": 0x72A9FF,
        "fog_color": 0xC8DDDA,
        "vegetation": [
            "minecraft:forest_flowers",
            "minecraft:trees_birch_and_oak",
            "minecraft:flower_default",
            "minecraft:patch_grass_forest",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
        ],
        "vegetation_fabric": [
            "minecraft:forest_flowers",
            "minecraft:trees_birch_and_oak_leaf_litter",
            "minecraft:flower_default",
            "minecraft:patch_grass_forest",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
        ],
    },
    "emerald_highlands": {
        "name_en": "Emerald Highlands",
        "name_ru": "Изумрудные нагорья",
        "temperature": 0.25,
        "downfall": 0.8,
        "grass_color": 0x73A265,
        "foliage_color": 0x4F7656,
        "water_color": 0x557F91,
        "sky_color": 0x77A3D0,
        "fog_color": 0xB8C9D7,
        "vegetation": [
            "minecraft:trees_old_growth_pine_taiga",
            "minecraft:patch_large_fern",
            "minecraft:flower_default",
            "minecraft:patch_grass_taiga",
            "minecraft:patch_berry_common",
            "minecraft:brown_mushroom_old_growth",
            "minecraft:red_mushroom_old_growth",
            "minecraft:patch_sugar_cane",
        ],
    },
    "skyreach_mountains": {
        "name_en": "Skyreach Mountains",
        "name_ru": "Небесные горы",
        "temperature": -0.35,
        "downfall": 0.85,
        "grass_color": 0x8AA79A,
        "foliage_color": 0x708E84,
        "water_color": 0x4A708A,
        "sky_color": 0x91B3DB,
        "fog_color": 0xC0CDDE,
        "vegetation": [
            "minecraft:trees_grove",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_grass_taiga",
            "minecraft:glow_lichen",
            "minecraft:patch_large_fern",
        ],
        "emerald_ore": True,
    },
}

CUSTOM_BIOME_IDS = [f"mushoku_worldgen:{name}" for name in BIOMES]
RETAINED_VANILLA_BIOME_IDS = [
    "minecraft:deep_ocean",
    "minecraft:ocean",
    "minecraft:beach",
    "minecraft:frozen_river",
    "minecraft:river",
    "minecraft:lush_caves",
    "minecraft:dripstone_caves",
    "minecraft:deep_dark",
]


def color(value: int) -> str:
    return f"#{value:06X}"


def spawners() -> dict[str, list[dict[str, Any]]]:
    def entry(entity: str, weight: int, minimum: int, maximum: int) -> dict[str, Any]:
        return {
            "type": f"minecraft:{entity}",
            "weight": weight,
            "minCount": minimum,
            "maxCount": maximum,
        }

    return {
        "ambient": [entry("bat", 10, 8, 8)],
        "axolotls": [],
        "creature": [
            entry("sheep", 12, 4, 4),
            entry("pig", 10, 4, 4),
            entry("chicken", 10, 4, 4),
            entry("cow", 8, 4, 4),
            entry("horse", 5, 2, 6),
        ],
        "misc": [],
        "monster": [
            entry("spider", 100, 4, 4),
            entry("zombie", 95, 4, 4),
            entry("zombie_villager", 5, 1, 1),
            entry("skeleton", 100, 4, 4),
            entry("creeper", 100, 4, 4),
            entry("slime", 100, 4, 4),
            entry("enderman", 10, 1, 4),
            entry("witch", 5, 1, 1),
        ],
        "underground_water_creature": [entry("glow_squid", 10, 4, 6)],
        "water_ambient": [],
        "water_creature": [],
    }


def ore_features(spec: dict[str, Any]) -> list[str]:
    features = [
        "minecraft:ore_dirt",
        "minecraft:ore_gravel",
        "minecraft:ore_granite_upper",
        "minecraft:ore_granite_lower",
        "minecraft:ore_diorite_upper",
        "minecraft:ore_diorite_lower",
        "minecraft:ore_andesite_upper",
        "minecraft:ore_andesite_lower",
        "minecraft:ore_tuff",
        "minecraft:ore_coal_upper",
        "minecraft:ore_coal_lower",
        "minecraft:ore_iron_upper",
        "minecraft:ore_iron_middle",
        "minecraft:ore_iron_small",
        "minecraft:ore_gold",
        "minecraft:ore_gold_lower",
        "minecraft:ore_redstone",
        "minecraft:ore_redstone_lower",
        "minecraft:ore_diamond",
        "minecraft:ore_diamond_large",
        "minecraft:ore_diamond_buried",
        "minecraft:ore_lapis",
        "minecraft:ore_lapis_buried",
        "minecraft:ore_copper",
        "minecraft:underwater_magma",
        "minecraft:disk_sand",
        "minecraft:disk_clay",
        "minecraft:disk_gravel",
    ]
    if spec.get("emerald_ore"):
        features.append("minecraft:ore_emerald")
    return features


def biome_features(spec: dict[str, Any], legacy: bool) -> list[list[str]]:
    vegetation = list(
        spec["vegetation"] if legacy else spec.get("vegetation_fabric", spec["vegetation"])
    )
    return [
        [],
        ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
        ["minecraft:amethyst_geode"],
        ["minecraft:monster_room", "minecraft:monster_room_deep"],
        [],
        [],
        ore_features(spec),
        [],
        ["minecraft:spring_water", "minecraft:spring_lava"],
        vegetation,
        ["minecraft:freeze_top_layer"],
    ]


def biome_definition(spec: dict[str, Any], legacy: bool) -> dict[str, Any]:
    definition: dict[str, Any] = {
        "has_precipitation": True,
        "temperature": spec["temperature"],
        "downfall": spec["downfall"],
        "effects": {
            "water_color": spec["water_color"] if legacy else color(spec["water_color"]),
            "foliage_color": spec["foliage_color"] if legacy else color(spec["foliage_color"]),
            "grass_color": spec["grass_color"] if legacy else color(spec["grass_color"]),
        },
        "features": biome_features(spec, legacy),
        "spawners": spawners(),
        "spawn_costs": {},
    }
    if legacy:
        definition["carvers"] = {
            "air": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]
        }
        definition["effects"].update(
            {
                "fog_color": spec["fog_color"],
                "sky_color": spec["sky_color"],
                "water_fog_color": 0x050533,
            }
        )
    else:
        definition["carvers"] = [
            "minecraft:cave",
            "minecraft:cave_extra_underground",
            "minecraft:canyon",
        ]
        definition["attributes"] = {
            "minecraft:visual/fog_color": color(spec["fog_color"]),
            "minecraft:visual/sky_color": color(spec["sky_color"]),
            "minecraft:visual/water_fog_color": "#050533",
        }
    return definition


def climate_point(
    biome: str,
    *,
    temperature: list[float] = [-1.0, 1.0],
    humidity: list[float] = [-1.0, 1.0],
    continentalness: list[float] = [-1.0, 1.0],
    erosion: list[float] = [-1.0, 1.0],
    weirdness: list[float] = [-1.0, 1.0],
    depth: list[float] = [0.0, 0.0],
    offset: float = 0.0,
) -> dict[str, Any]:
    return {
        "biome": biome,
        "parameters": {
            "temperature": temperature,
            "humidity": humidity,
            "continentalness": continentalness,
            "erosion": erosion,
            "weirdness": weirdness,
            "depth": depth,
            "offset": offset,
        },
    }


def world_preset() -> dict[str, Any]:
    biomes: list[dict[str, Any]] = [
        climate_point("minecraft:deep_ocean", continentalness=[-1.0, -0.455]),
        climate_point("minecraft:ocean", continentalness=[-0.455, -0.19]),
        climate_point("minecraft:beach", continentalness=[-0.19, -0.11]),
        climate_point(
            "mushoku_worldgen:riverside_meadow",
            temperature=[-0.15, 0.55],
            humidity=[0.15, 0.8],
            continentalness=[-0.11, 0.12],
        ),
        climate_point(
            "minecraft:frozen_river",
            temperature=[-1.0, -0.2],
            continentalness=[-0.11, 1.0],
            weirdness=[-0.08, 0.08],
        ),
        climate_point(
            "minecraft:river",
            temperature=[-0.2, 1.0],
            continentalness=[-0.11, 1.0],
            weirdness=[-0.08, 0.08],
        ),
        climate_point(
            "mushoku_worldgen:skyreach_mountains",
            temperature=[-1.0, 0.4],
            continentalness=[0.05, 1.0],
            erosion=[-1.0, -0.45],
            weirdness=[0.45, 1.0],
        ),
        climate_point(
            "mushoku_worldgen:emerald_highlands",
            temperature=[-1.0, 0.15],
            humidity=[0.0, 1.0],
            continentalness=[0.05, 1.0],
        ),
        climate_point(
            "mushoku_worldgen:whispering_forest",
            temperature=[-0.3, 0.65],
            humidity=[0.4, 1.0],
            continentalness=[0.05, 1.0],
        ),
        climate_point(
            "mushoku_worldgen:golden_steppe",
            temperature=[0.25, 1.0],
            humidity=[-1.0, 0.2],
            continentalness=[0.05, 1.0],
        ),
        climate_point(
            "minecraft:lush_caves",
            humidity=[0.5, 1.0],
            depth=[0.2, 0.9],
        ),
        climate_point(
            "minecraft:dripstone_caves",
            humidity=[-1.0, 0.5],
            depth=[0.2, 0.9],
        ),
        climate_point(
            "minecraft:deep_dark",
            erosion=[-1.0, -0.375],
            depth=[1.1, 1.1],
        ),
    ]
    return {
        "dimensions": {
            "minecraft:overworld": {
                "type": "minecraft:overworld",
                "generator": {
                    "type": "minecraft:noise",
                    "settings": "minecraft:overworld",
                    "biome_source": {
                        "type": "minecraft:multi_noise",
                        "biomes": biomes,
                    },
                },
            },
            "minecraft:the_nether": {
                "type": "minecraft:the_nether",
                "generator": {
                    "type": "minecraft:noise",
                    "settings": "minecraft:nether",
                    "biome_source": {
                        "type": "minecraft:multi_noise",
                        "preset": "minecraft:nether",
                    },
                },
            },
            "minecraft:the_end": {
                "type": "minecraft:the_end",
                "generator": {
                    "type": "minecraft:noise",
                    "settings": "minecraft:end",
                    "biome_source": {"type": "minecraft:the_end"},
                },
            },
        }
    }


def generated_files(resource_root: Path, legacy: bool) -> dict[Path, dict[str, Any]]:
    files: dict[Path, dict[str, Any]] = {}
    biome_root = resource_root / "data/mushoku_worldgen/worldgen/biome"
    for name, spec in BIOMES.items():
        files[biome_root / f"{name}.json"] = biome_definition(spec, legacy)

    files[
        resource_root / "data/mushoku_worldgen/worldgen/world_preset/mushoku_world.json"
    ] = world_preset()
    files[resource_root / "data/minecraft/tags/worldgen/world_preset/normal.json"] = {
        "replace": False,
        "values": ["mushoku_worldgen:mushoku_world"],
    }
    files[resource_root / "data/minecraft/tags/worldgen/biome/is_overworld.json"] = {
        "replace": False,
        "values": CUSTOM_BIOME_IDS,
    }
    files[
        resource_root
        / "data/minecraft/tags/worldgen/biome/has_structure/village_plains.json"
    ] = {
        "replace": False,
        "values": [
            "mushoku_worldgen:golden_steppe",
            "mushoku_worldgen:riverside_meadow",
            "mushoku_worldgen:whispering_forest",
        ],
    }
    files[
        resource_root
        / "data/minecraft/tags/worldgen/biome/has_structure/pillager_outpost.json"
    ] = {
        "replace": False,
        "values": [
            "mushoku_worldgen:golden_steppe",
            "mushoku_worldgen:riverside_meadow",
            "mushoku_worldgen:whispering_forest",
            "mushoku_worldgen:emerald_highlands",
            "mushoku_worldgen:skyreach_mountains",
        ],
    }

    files[resource_root / "assets/mushoku_worldgen/lang/en_us.json"] = {
        "generator.mushoku_worldgen.mushoku_world": "Mushoku: Vast Lands",
        **{
            f"biome.mushoku_worldgen.{key}": spec["name_en"]
            for key, spec in BIOMES.items()
        },
    }
    files[resource_root / "assets/mushoku_worldgen/lang/ru_ru.json"] = {
        "generator.mushoku_worldgen.mushoku_world": "Мушоку: Просторные земли",
        **{
            f"biome.mushoku_worldgen.{key}": spec["name_ru"]
            for key, spec in BIOMES.items()
        },
    }
    return files


def check_world_preset(preset: dict[str, Any], resource_root: Path) -> None:
    dimensions = preset.get("dimensions", {})
    expected_dimensions = {
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end",
    }
    if set(dimensions) != expected_dimensions:
        raise ValueError(f"World preset has unexpected dimensions: {sorted(dimensions)}")

    overworld = dimensions["minecraft:overworld"]["generator"]
    if overworld.get("settings") != "minecraft:overworld":
        raise ValueError("The custom preset must not replace the vanilla terrain noise settings")
    source = overworld.get("biome_source", {})
    if source.get("type") != "minecraft:multi_noise":
        raise ValueError("The custom preset must use the multi-noise biome source")

    entries = source.get("biomes", [])
    selected = {entry.get("biome") for entry in entries}
    expected_palette = set(CUSTOM_BIOME_IDS + RETAINED_VANILLA_BIOME_IDS)
    if selected != expected_palette:
        missing = sorted(expected_palette - selected)
        unexpected = sorted(selected - expected_palette)
        raise ValueError(f"Unexpected preset biome palette; missing={missing}, extra={unexpected}")
    for biome_id in CUSTOM_BIOME_IDS:
        biome_file = resource_root / "data" / biome_id.split(":", 1)[0] / "worldgen/biome" / f"{biome_id.split(':', 1)[1]}.json"
        if not biome_file.is_file():
            raise ValueError(f"Missing biome definition for {biome_id}: {biome_file}")


def render(value: dict[str, Any]) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2) + "\n"


def update(root: Path, check: bool) -> int:
    mismatches: list[str] = []
    for resource_root, legacy in ((FABRIC_RESOURCES, False), (LEGACY_RESOURCES, True)):
        for path, value in generated_files(resource_root, legacy).items():
            content = render(value)
            if check:
                if not path.is_file() or path.read_text(encoding="utf-8") != content:
                    mismatches.append(str(path.relative_to(root)))
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content, encoding="utf-8")
        preset_path = (
            resource_root / "data/mushoku_worldgen/worldgen/world_preset/mushoku_world.json"
        )
        try:
            check_world_preset(json.loads(preset_path.read_text(encoding="utf-8")), resource_root)
        except (OSError, json.JSONDecodeError, ValueError) as error:
            mismatches.append(f"{preset_path.relative_to(root)}: {error}")

    if mismatches:
        print("Worldgen resource verification failed:", file=sys.stderr)
        for path in mismatches:
            print(f"  {path}", file=sys.stderr)
        return 1
    action = "Verified" if check else "Generated"
    print(f"{action} {len(BIOMES)} custom biome definitions for Fabric and legacy ports.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="verify generated resources without changing files",
    )
    args = parser.parse_args()
    return update(ROOT, args.check)


if __name__ == "__main__":
    raise SystemExit(main())
