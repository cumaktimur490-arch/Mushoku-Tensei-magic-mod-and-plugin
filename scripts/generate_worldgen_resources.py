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
        "vegetation_profile": "plains",
        "vegetation_overlay": [
            "mushoku_worldgen:groundcover_grass_lush",
            "mushoku_worldgen:groundcover_tall_grass",
            "mushoku_worldgen:steppe_bloom_patches",
        ],
        "vegetation": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:trees_plains",
            "minecraft:flower_plains",
            "minecraft:patch_grass_plain",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
            "mushoku_worldgen:golden_wheat_fields",
        ],
        "vegetation_fabric": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_bush",
            "minecraft:trees_plains",
            "minecraft:flower_plains",
            "minecraft:patch_grass_plain",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_pumpkin",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_firefly_bush_near_water",
            "mushoku_worldgen:golden_wheat_fields",
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
        "vegetation_profile": "meadow",
        "vegetation_overlay": [
            "mushoku_worldgen:groundcover_grass_lush",
            "mushoku_worldgen:groundcover_tall_grass_lush",
            "mushoku_worldgen:meadow_bloom_patches",
        ],
        "vegetation": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_grass_plain",
            "minecraft:flower_meadow",
            "minecraft:trees_meadow",
        ],
        "vegetation_fabric": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_grass_meadow",
            "minecraft:flower_meadow",
            "minecraft:trees_meadow",
            "minecraft:wildflowers_meadow",
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
        "vegetation_profile": "forest",
        "vegetation_overlay": [
            "mushoku_worldgen:forest_groundcover_grass",
            "mushoku_worldgen:forest_tall_grass",
            "mushoku_worldgen:forest_fern_sprigs",
            "mushoku_worldgen:forest_bloom_patches",
        ],
        "vegetation": [
            "minecraft:glow_lichen",
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
            "minecraft:glow_lichen",
            "minecraft:forest_flowers",
            "minecraft:trees_birch_and_oak_leaf_litter",
            "minecraft:patch_bush",
            "minecraft:flower_default",
            "minecraft:patch_grass_forest",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_pumpkin",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_firefly_bush_near_water",
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
        "vegetation_profile": "old_growth_pine_taiga",
        "vegetation_overlay": [
            "mushoku_worldgen:highland_groundcover_grass",
            "mushoku_worldgen:highland_tall_grass",
            "mushoku_worldgen:highland_fern_sprigs",
            "mushoku_worldgen:highland_bloom_patches",
        ],
        "vegetation": [
            "minecraft:glow_lichen",
            "minecraft:patch_large_fern",
            "minecraft:trees_old_growth_pine_taiga",
            "minecraft:flower_default",
            "minecraft:patch_grass_taiga",
            "minecraft:patch_dead_bush",
            "minecraft:brown_mushroom_old_growth",
            "minecraft:red_mushroom_old_growth",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
            "minecraft:patch_berry_common",
        ],
        "vegetation_fabric": [
            "minecraft:glow_lichen",
            "minecraft:patch_large_fern",
            "minecraft:trees_old_growth_pine_taiga",
            "minecraft:flower_default",
            "minecraft:patch_grass_taiga",
            "minecraft:patch_dead_bush",
            "minecraft:brown_mushroom_old_growth",
            "minecraft:red_mushroom_old_growth",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_pumpkin",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_firefly_bush_near_water",
            "minecraft:patch_berry_common",
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
        "vegetation_profile": "grove",
        "vegetation_overlay": [
            "mushoku_worldgen:alpine_groundcover_grass",
            "mushoku_worldgen:alpine_bloom_patches",
        ],
        "vegetation": [
            "minecraft:glow_lichen",
            "minecraft:trees_grove",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
        ],
        "vegetation_fabric": [
            "minecraft:glow_lichen",
            "minecraft:trees_grove",
            "minecraft:patch_pumpkin",
        ],
        "emerald_ore": True,
    },
}

CUSTOM_BIOME_IDS = [f"mushoku_worldgen:{name}" for name in BIOMES]
BIOME_CATEGORY_TAGS = {
    "is_forest": ["mushoku_worldgen:whispering_forest"],
    "is_hill": ["mushoku_worldgen:emerald_highlands"],
    "is_mountain": ["mushoku_worldgen:skyreach_mountains"],
    "is_taiga": ["mushoku_worldgen:emerald_highlands"],
}
STRUCTURE_BIOME_TAGS = {
    "village_plains": [
        "mushoku_worldgen:golden_steppe",
        "mushoku_worldgen:riverside_meadow",
        "mushoku_worldgen:whispering_forest",
    ],
    "village_snowy": ["mushoku_worldgen:skyreach_mountains"],
    "village_taiga": ["mushoku_worldgen:emerald_highlands"],
    "pillager_outpost": list(CUSTOM_BIOME_IDS),
}
CUSTOM_NOISE_SETTINGS_ID = "mushoku_worldgen:mushoku_overworld"
CUSTOM_DENSITY_FUNCTION_IDS = (
    "mushoku_worldgen:macro_continents",
    "mushoku_worldgen:landmass_density",
    "mushoku_worldgen:tectonic_relief",
)
GOLDEN_WHEAT_FIELDS_ID = "mushoku_worldgen:golden_wheat_fields"
GOLDEN_WHEAT_FIELD_CONFIG_ID = "mushoku_worldgen:golden_wheat_field"
RIPE_WHEAT_ID = "mushoku_worldgen:ripe_wheat"
TALL_GRASS_PATCH_ID = "mushoku_worldgen:tall_grass_clump"
FERN_SPRIG_PATCH_ID = "mushoku_worldgen:fern_sprig_patch"
VEGETATION_PLACED_FEATURE_IDS = {
    "groundcover_grass_lush": "mushoku_worldgen:groundcover_grass_lush",
    "groundcover_tall_grass": "mushoku_worldgen:groundcover_tall_grass",
    "groundcover_tall_grass_lush": "mushoku_worldgen:groundcover_tall_grass_lush",
    "steppe_bloom_patches": "mushoku_worldgen:steppe_bloom_patches",
    "meadow_bloom_patches": "mushoku_worldgen:meadow_bloom_patches",
    "forest_groundcover_grass": "mushoku_worldgen:forest_groundcover_grass",
    "forest_tall_grass": "mushoku_worldgen:forest_tall_grass",
    "forest_fern_sprigs": "mushoku_worldgen:forest_fern_sprigs",
    "forest_bloom_patches": "mushoku_worldgen:forest_bloom_patches",
    "highland_groundcover_grass": "mushoku_worldgen:highland_groundcover_grass",
    "highland_tall_grass": "mushoku_worldgen:highland_tall_grass",
    "highland_fern_sprigs": "mushoku_worldgen:highland_fern_sprigs",
    "highland_bloom_patches": "mushoku_worldgen:highland_bloom_patches",
    "alpine_groundcover_grass": "mushoku_worldgen:alpine_groundcover_grass",
    "alpine_bloom_patches": "mushoku_worldgen:alpine_bloom_patches",
}
VEGETATION_PLACED_FEATURE_PATHS = {
    feature_id: f"worldgen/placed_feature/{feature_id.split(':', 1)[1]}.json"
    for feature_id in VEGETATION_PLACED_FEATURE_IDS.values()
}
VEGETATION_CONFIGURED_FEATURE_PATHS = {
    TALL_GRASS_PATCH_ID: "worldgen/configured_feature/tall_grass_clump.json",
    FERN_SPRIG_PATCH_ID: "worldgen/configured_feature/fern_sprig_patch.json",
}
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


def ore_features(spec: dict[str, Any], legacy: bool) -> list[str]:
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
    if not legacy:
        features.insert(features.index("minecraft:ore_diamond_large"), "minecraft:ore_diamond_medium")
    if spec.get("emerald_ore"):
        features.append("minecraft:ore_emerald")
    return features


def biome_features(spec: dict[str, Any], legacy: bool) -> list[list[str]]:
    vegetation = list(
        spec["vegetation"] if legacy else spec.get("vegetation_fabric", spec["vegetation"])
    )
    overlay = list(spec.get("vegetation_overlay", []))
    if GOLDEN_WHEAT_FIELDS_ID in vegetation:
        # Keep the selected steppe-only crop feature last so fields remain the
        # dominant ground-cover accent where their patches are generated.
        wheat_index = vegetation.index(GOLDEN_WHEAT_FIELDS_ID)
        vegetation[wheat_index:wheat_index] = overlay
    elif spec["vegetation_profile"] in {"forest", "old_growth_pine_taiga"}:
        # Lay undergrowth before the canopy is placed so surface-height sampling
        # reaches the forest floor rather than the tops of leaves.
        if spec["vegetation_profile"] == "forest":
            tree_feature = (
                "minecraft:trees_birch_and_oak"
                if legacy
                else "minecraft:trees_birch_and_oak_leaf_litter"
            )
        else:
            tree_feature = "minecraft:trees_old_growth_pine_taiga"
        tree_index = vegetation.index(tree_feature)
        vegetation[tree_index:tree_index] = overlay
    else:
        vegetation.extend(overlay)
    return [
        [],
        ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
        ["minecraft:amethyst_geode"],
        ["minecraft:monster_room", "minecraft:monster_room_deep"],
        [],
        [],
        ore_features(spec, legacy),
        [],
        ["minecraft:spring_water", "minecraft:spring_lava"],
        vegetation,
        ["minecraft:freeze_top_layer"],
    ]


# FeatureSorter converts the order inside each decoration step into ordering
# constraints. Preserve the relative order of shared vanilla features while
# allowing standalone Mushoku features; the merged graph below still rejects
# cross-biome cycles such as reversing deep_dark's tall-grass/tree constraints.
VANILLA_VEGETATION_ORDER: dict[str, dict[str, list[str]]] = {
    "legacy": {
        "plains": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:trees_plains",
            "minecraft:flower_plains",
            "minecraft:patch_grass_plain",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
        ],
        "deep_dark": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:trees_plains",
            "minecraft:flower_plains",
            "minecraft:patch_grass_plain",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
        ],
        "meadow": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_grass_plain",
            "minecraft:flower_meadow",
            "minecraft:trees_meadow",
        ],
        "forest": [
            "minecraft:glow_lichen",
            "minecraft:forest_flowers",
            "minecraft:trees_birch_and_oak",
            "minecraft:flower_default",
            "minecraft:patch_grass_forest",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
        ],
        "old_growth_pine_taiga": [
            "minecraft:glow_lichen",
            "minecraft:patch_large_fern",
            "minecraft:trees_old_growth_pine_taiga",
            "minecraft:flower_default",
            "minecraft:patch_grass_taiga",
            "minecraft:patch_dead_bush",
            "minecraft:brown_mushroom_old_growth",
            "minecraft:red_mushroom_old_growth",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
            "minecraft:patch_berry_common",
        ],
        "grove": [
            "minecraft:glow_lichen",
            "minecraft:trees_grove",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_pumpkin",
        ],
    },
    "fabric": {
        "plains": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_bush",
            "minecraft:trees_plains",
            "minecraft:flower_plains",
            "minecraft:patch_grass_plain",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_pumpkin",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_firefly_bush_near_water",
        ],
        "deep_dark": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:trees_plains",
            "minecraft:flower_plains",
            "minecraft:patch_grass_plain",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_pumpkin",
        ],
        "meadow": [
            "minecraft:glow_lichen",
            "minecraft:patch_tall_grass_2",
            "minecraft:patch_grass_meadow",
            "minecraft:flower_meadow",
            "minecraft:trees_meadow",
            "minecraft:wildflowers_meadow",
        ],
        "forest": [
            "minecraft:glow_lichen",
            "minecraft:forest_flowers",
            "minecraft:trees_birch_and_oak_leaf_litter",
            "minecraft:patch_bush",
            "minecraft:flower_default",
            "minecraft:patch_grass_forest",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_pumpkin",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_firefly_bush_near_water",
        ],
        "old_growth_pine_taiga": [
            "minecraft:glow_lichen",
            "minecraft:patch_large_fern",
            "minecraft:trees_old_growth_pine_taiga",
            "minecraft:flower_default",
            "minecraft:patch_grass_taiga",
            "minecraft:patch_dead_bush",
            "minecraft:brown_mushroom_old_growth",
            "minecraft:red_mushroom_old_growth",
            "minecraft:brown_mushroom_normal",
            "minecraft:red_mushroom_normal",
            "minecraft:patch_pumpkin",
            "minecraft:patch_sugar_cane",
            "minecraft:patch_firefly_bush_near_water",
            "minecraft:patch_berry_common",
        ],
        "grove": [
            "minecraft:glow_lichen",
            "minecraft:trees_grove",
            "minecraft:patch_pumpkin",
        ],
    },
}


def is_ordered_subsequence(actual: list[str], reference: list[str]) -> bool:
    next_index = 0
    for feature in actual:
        try:
            next_index = reference.index(feature, next_index) + 1
        except ValueError:
            return False
    return True


def reference_feature_stages(profile: str, legacy: bool) -> list[list[str]]:
    version = "legacy" if legacy else "fabric"
    if profile not in VANILLA_VEGETATION_ORDER[version]:
        raise ValueError(f"Unknown vanilla feature-order profile: {profile}")

    vanilla_ores = ore_features(
        {"emerald_ore": profile in {"meadow", "grove"}}, legacy
    )
    springs = ["minecraft:spring_water", "minecraft:spring_lava"]
    if profile == "grove":
        springs.append("minecraft:spring_lava_frozen")
    stage_two = ["minecraft:amethyst_geode"]
    stage_seven: list[str] = []
    if profile == "old_growth_pine_taiga":
        stage_two.append("minecraft:forest_rock")
    if profile in {"meadow", "grove"}:
        stage_seven.append("minecraft:ore_infested")

    return [
        [],
        ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
        stage_two,
        ["minecraft:monster_room", "minecraft:monster_room_deep"],
        [],
        [],
        vanilla_ores,
        stage_seven,
        springs,
        VANILLA_VEGETATION_ORDER[version][profile],
        ["minecraft:freeze_top_layer"],
    ]


def check_feature_ordering(legacy: bool) -> None:
    version = "Minecraft 1.20.1" if legacy else "Minecraft 1.21.11"
    profile_names = list(VANILLA_VEGETATION_ORDER["legacy" if legacy else "fabric"])
    profile_names.remove("deep_dark")
    profile_sequences = {
        profile: reference_feature_stages(profile, legacy)
        for profile in profile_names
    }
    deep_dark_vegetation = VANILLA_VEGETATION_ORDER[
        "legacy" if legacy else "fabric"
    ]["deep_dark"]

    for biome_name, spec in BIOMES.items():
        profile = spec["vegetation_profile"]
        actual = biome_features(spec, legacy)
        expected = profile_sequences[profile]
        for stage, feature_list in enumerate(actual):
            vanilla_features = set(expected[stage])
            shared_features = [
                feature for feature in feature_list if feature in vanilla_features
            ]
            if not is_ordered_subsequence(shared_features, expected[stage]):
                raise ValueError(
                    f"{version} {biome_name} vanilla features in decoration step {stage} "
                    f"are not ordered like vanilla {profile}: {feature_list}"
                )
        if biome_name == "golden_steppe":
            deep_dark_features = set(deep_dark_vegetation)
            shared_with_deep_dark = [
                feature for feature in actual[9] if feature in deep_dark_features
            ]
            if not is_ordered_subsequence(shared_with_deep_dark, deep_dark_vegetation):
                raise ValueError(
                    f"{version} Golden Steppe vegetation conflicts with minecraft:deep_dark"
                )

    # FeatureSorter also merges every biome's same-stage sequence into a graph.
    # Check the selected vanilla templates together with all custom lists so a
    # future edit cannot introduce a cross-biome cycle unnoticed by JSON checks.
    for stage in range(11):
        graph: dict[str, set[str]] = {}
        indegree: dict[str, int] = {}
        sources: dict[tuple[str, str], str] = {}
        sequences: list[tuple[str, list[str]]] = []
        for profile, stages in profile_sequences.items():
            sequences.append((f"minecraft:{profile}", stages[stage]))
        sequences.append(("minecraft:deep_dark", deep_dark_vegetation if stage == 9 else []))
        for biome_name, spec in BIOMES.items():
            sequences.append(
                (f"mushoku_worldgen:{biome_name}", biome_features(spec, legacy)[stage])
            )

        for source, sequence in sequences:
            for feature in sequence:
                graph.setdefault(feature, set())
                indegree.setdefault(feature, 0)
            for before, after in zip(sequence, sequence[1:]):
                if after not in graph[before]:
                    graph[before].add(after)
                    indegree[after] = indegree.get(after, 0) + 1
                sources[(before, after)] = source

        ready = [feature for feature, degree in indegree.items() if degree == 0]
        visited = 0
        while ready:
            feature = ready.pop()
            visited += 1
            for following in graph[feature]:
                indegree[following] -= 1
                if indegree[following] == 0:
                    ready.append(following)
        if visited != len(indegree):
            cyclic = sorted(feature for feature, degree in indegree.items() if degree > 0)
            raise ValueError(
                f"{version} decoration step {stage} has a feature-order cycle: {cyclic}; "
                f"last constraints={list(sources.items())[-8:]}"
            )


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


def density_add(argument1: Any, argument2: Any) -> dict[str, Any]:
    return {"type": "minecraft:add", "argument1": argument1, "argument2": argument2}


def density_mul(argument1: Any, argument2: Any) -> dict[str, Any]:
    return {"type": "minecraft:mul", "argument1": argument1, "argument2": argument2}


def density_abs(argument: Any) -> dict[str, Any]:
    return {"type": "minecraft:abs", "argument": argument}


def density_clamp(argument: Any, minimum: float, maximum: float) -> dict[str, Any]:
    return {
        "type": "minecraft:clamp",
        "input": argument,
        "min": minimum,
        "max": maximum,
    }


def density_min(argument1: Any, argument2: Any) -> dict[str, Any]:
    return {"type": "minecraft:min", "argument1": argument1, "argument2": argument2}


def density_max(argument1: Any, argument2: Any) -> dict[str, Any]:
    return {"type": "minecraft:max", "argument1": argument1, "argument2": argument2}


def density_noise(noise: str, xz_scale: float, y_scale: float) -> dict[str, Any]:
    return {
        "type": "minecraft:noise",
        "noise": noise,
        "xz_scale": xz_scale,
        "y_scale": y_scale,
    }


def density_shifted_noise(noise: str, xz_scale: float) -> dict[str, Any]:
    return {
        "type": "minecraft:shifted_noise",
        "noise": noise,
        "shift_x": "minecraft:shift_x",
        "shift_y": 0.0,
        "shift_z": "minecraft:shift_z",
        "xz_scale": xz_scale,
        "y_scale": 0.0,
    }


def macro_continents_function() -> dict[str, Any]:
    """Low-frequency landmass field, with a smaller coast-detail octave."""
    broad_noise = density_shifted_noise("minecraft:continentalness", 0.055)
    coast_detail = density_mul(
        0.18, density_shifted_noise("minecraft:continentalness", 0.14)
    )
    return {
        "type": "minecraft:flat_cache",
        "argument": {
            "type": "minecraft:clamp",
            "input": density_add(broad_noise, coast_detail),
            "min": -1.0,
            "max": 1.0,
        },
    }


def tectonic_relief_function() -> dict[str, Any]:
    """Add restrained inland uplifts and softly carved river corridors.

    The vanilla erosion and ridge router fields keep this blended into
    Minecraft's existing terrain instead of replacing its terrain model.
    """
    continents = CUSTOM_DENSITY_FUNCTION_IDS[0]
    inland = density_clamp(
        density_mul(1.4, density_add(continents, -0.08)), 0.0, 1.0
    )
    erosion = "minecraft:overworld/erosion"
    ruggedness = density_clamp(density_add(0.25, density_mul(-1.0, erosion)), 0.0, 1.0)
    ridge_signal = density_abs("minecraft:overworld/ridges")
    ridge_uplift = density_clamp(density_add(ridge_signal, -0.45), 0.0, 0.55)

    highland_mask = density_mul(inland, ruggedness)
    highland_uplift = density_mul(
        highland_mask, density_add(0.08, density_mul(0.22, ridge_uplift))
    )
    river_corridor = density_clamp(
        density_add(0.08, density_mul(-1.0, ridge_signal)), 0.0, 0.08
    )
    river_carving = density_mul(-2.0, river_corridor)
    broad_erosion = density_mul(0.04, erosion)

    return {
        "type": "minecraft:flat_cache",
        "argument": density_add(
            highland_uplift, density_add(river_carving, broad_erosion)
        ),
    }


def landmass_density_function() -> dict[str, Any]:
    """Keep vanilla caves while blending in continents and gentle relief."""
    return density_add(
        "minecraft:overworld/sloped_cheese",
        density_add(
            density_mul(0.5, CUSTOM_DENSITY_FUNCTION_IDS[0]),
            CUSTOM_DENSITY_FUNCTION_IDS[2],
        ),
    )


def range_choice(
    input_value: Any,
    *,
    minimum: float,
    maximum: float,
    when_in_range: Any,
    when_out_of_range: Any,
) -> dict[str, Any]:
    return {
        "type": "minecraft:range_choice",
        "input": input_value,
        "min_inclusive": minimum,
        "max_exclusive": maximum,
        "when_in_range": when_in_range,
        "when_out_of_range": when_out_of_range,
    }


def vanilla_cave_aware_final_density() -> dict[str, Any]:
    """Vanilla 1.20/1.21 cave router, using the adjusted terrain density."""
    base_density = CUSTOM_DENSITY_FUNCTION_IDS[1]
    cave_layer = density_mul(
        4.0,
        {
            "type": "minecraft:square",
            "argument": density_noise("minecraft:cave_layer", 1.0, 8.0),
        },
    )
    cave_cheese = {
        "type": "minecraft:clamp",
        "input": density_add(
            0.27, density_noise("minecraft:cave_cheese", 1.0, 2.0 / 3.0)
        ),
        "min": -1.0,
        "max": 1.0,
    }
    terrain_mask = {
        "type": "minecraft:clamp",
        "input": density_add(1.5, density_mul(-0.64, base_density)),
        "min": 0.0,
        "max": 0.5,
    }
    caves_near_surface = density_add(cave_layer, density_add(cave_cheese, terrain_mask))
    cave_entrances = density_min(
        caves_near_surface, "minecraft:overworld/caves/entrances"
    )
    spaghetti = density_add(
        "minecraft:overworld/caves/spaghetti_2d",
        "minecraft:overworld/caves/spaghetti_roughness_function",
    )
    pillars = range_choice(
        "minecraft:overworld/caves/pillars",
        minimum=-1_000_000.0,
        maximum=0.03,
        when_in_range=-1_000_000.0,
        when_out_of_range="minecraft:overworld/caves/pillars",
    )
    cave_density = density_max(density_min(cave_entrances, spaghetti), pillars)
    surface_or_caves = range_choice(
        base_density,
        minimum=-1_000_000.0,
        maximum=1.5625,
        when_in_range=density_min(
            base_density,
            density_mul(5.0, "minecraft:overworld/caves/entrances"),
        ),
        when_out_of_range=cave_density,
    )

    bottom_slide = {
        "type": "minecraft:y_clamped_gradient",
        "from_value": 0.0,
        "from_y": -64,
        "to_value": 1.0,
        "to_y": -40,
    }
    top_slide = {
        "type": "minecraft:y_clamped_gradient",
        "from_value": 1.0,
        "from_y": 240,
        "to_value": 0.0,
        "to_y": 256,
    }
    slid_density = density_add(
        0.1171875,
        density_mul(
            bottom_slide,
            density_add(
                -0.1171875,
                density_add(
                    -0.078125,
                    density_mul(top_slide, density_add(0.078125, surface_or_caves)),
                ),
            ),
        ),
    )
    vanilla_terrain_with_caves = {
        "type": "minecraft:squeeze",
        "argument": density_mul(
            0.64,
            {
                "type": "minecraft:interpolated",
                "argument": {
                    "type": "minecraft:blend_density",
                    "argument": slid_density,
                },
            },
        ),
    }
    return density_min(vanilla_terrain_with_caves, "minecraft:overworld/caves/noodle")


def surface_condition(condition: dict[str, Any], rule: dict[str, Any]) -> dict[str, Any]:
    return {"type": "minecraft:condition", "if_true": condition, "then_run": rule}


def surface_block(block: str, properties: dict[str, str] | None = None) -> dict[str, Any]:
    result_state: dict[str, Any] = {"Name": block}
    if properties:
        result_state["Properties"] = properties
    return {"type": "minecraft:block", "result_state": result_state}


def surface_biome_rule(biomes: list[str], block: str) -> dict[str, Any]:
    return surface_condition(
        {"type": "minecraft:biome", "biome_is": biomes}, surface_block(block)
    )


def surface_rule() -> dict[str, Any]:
    top_layer = surface_condition(
        {
            "type": "minecraft:stone_depth",
            "offset": 0,
            "surface_type": "floor",
            "add_surface_depth": False,
            "secondary_depth_range": 0,
        },
        {
            "type": "minecraft:sequence",
            "sequence": [
                surface_biome_rule(["minecraft:beach"], "minecraft:sand"),
                surface_biome_rule(["minecraft:ocean"], "minecraft:sand"),
                surface_biome_rule(["minecraft:deep_ocean"], "minecraft:gravel"),
                surface_biome_rule(
                    ["minecraft:river", "minecraft:frozen_river"], "minecraft:gravel"
                ),
                surface_biome_rule(
                    ["mushoku_worldgen:skyreach_mountains"], "minecraft:stone"
                ),
                surface_block("minecraft:grass_block", {"snowy": "false"}),
            ],
        },
    )
    dirt_layer = surface_condition(
        {
            "type": "minecraft:stone_depth",
            "offset": 0,
            "surface_type": "floor",
            "add_surface_depth": True,
            "secondary_depth_range": 0,
        },
        surface_block("minecraft:dirt"),
    )
    bedrock_floor = surface_condition(
        {
            "type": "minecraft:vertical_gradient",
            "random_name": "minecraft:bedrock_floor",
            "true_at_and_below": {"above_bottom": 0},
            "false_at_and_above": {"above_bottom": 5},
        },
        surface_block("minecraft:bedrock"),
    )
    natural_surface = surface_condition(
        {"type": "minecraft:above_preliminary_surface"},
        {"type": "minecraft:sequence", "sequence": [top_layer, dirt_layer]},
    )
    return {"type": "minecraft:sequence", "sequence": [bedrock_floor, natural_surface]}


def noise_settings(legacy: bool) -> dict[str, Any]:
    aquifer_barrier = density_noise("minecraft:aquifer_barrier", 1.0, 0.5)
    router: dict[str, Any] = {
        "barrier": aquifer_barrier,
        "continents": CUSTOM_DENSITY_FUNCTION_IDS[0],
        "depth": "minecraft:overworld/depth",
        "erosion": "minecraft:overworld/erosion",
        "final_density": vanilla_cave_aware_final_density(),
        "fluid_level_floodedness": density_noise(
            "minecraft:aquifer_fluid_level_floodedness", 1.0, 0.67
        ),
        "fluid_level_spread": density_noise(
            "minecraft:aquifer_fluid_level_spread", 1.0, 0.7142857142857143
        ),
        "ridges": "minecraft:overworld/ridges",
        "temperature": density_shifted_noise("minecraft:temperature", 0.25),
        "vegetation": density_shifted_noise("minecraft:vegetation", 0.25),
        "vein_gap": density_noise("minecraft:ore_gap", 1.0, 1.0),
        "vein_ridged": density_add(
            -0.07999999821186066,
            density_max(
                {
                    "type": "minecraft:abs",
                    "argument": {
                        "type": "minecraft:interpolated",
                        "argument": range_choice(
                            "minecraft:y",
                            minimum=-60.0,
                            maximum=51.0,
                            when_in_range=density_noise("minecraft:ore_vein_a", 4.0, 4.0),
                            when_out_of_range=0.0,
                        ),
                    },
                },
                {
                    "type": "minecraft:abs",
                    "argument": {
                        "type": "minecraft:interpolated",
                        "argument": range_choice(
                            "minecraft:y",
                            minimum=-60.0,
                            maximum=51.0,
                            when_in_range=density_noise("minecraft:ore_vein_b", 4.0, 4.0),
                            when_out_of_range=0.0,
                        ),
                    },
                },
            ),
        ),
        "vein_toggle": {
            "type": "minecraft:interpolated",
            "argument": range_choice(
                "minecraft:y",
                minimum=-60.0,
                maximum=51.0,
                when_in_range=density_noise("minecraft:ore_veininess", 1.5, 1.5),
                when_out_of_range=0.0,
            ),
        },
    }
    if legacy:
        router["initial_density_without_jaggedness"] = CUSTOM_DENSITY_FUNCTION_IDS[1]
        router["lava"] = density_noise("minecraft:aquifer_lava", 1.0, 1.0)
    else:
        router["preliminary_surface_level"] = {
            "type": "minecraft:find_top_surface",
            "cell_height": 8,
            "density": CUSTOM_DENSITY_FUNCTION_IDS[1],
            "lower_bound": -64,
            "upper_bound": 320,
        }

    return {
        "aquifers_enabled": True,
        "default_block": {"Name": "minecraft:stone"},
        "default_fluid": {
            "Name": "minecraft:water",
            "Properties": {"level": "0"},
        },
        "disable_mob_generation": False,
        "legacy_random_source": False,
        "noise": {
            "height": 384,
            "min_y": -64,
            "size_horizontal": 1,
            "size_vertical": 2,
        },
        "noise_router": router,
        "ore_veins_enabled": True,
        "sea_level": 63,
        "spawn_target": [
            {
                "continentalness": [-0.11, 1.0],
                "depth": 0.0,
                "erosion": [-1.0, 1.0],
                "humidity": [-1.0, 1.0],
                "offset": 0.0,
                "temperature": [-1.0, 1.0],
                "weirdness": [-1.0, 1.0],
            }
        ],
        "surface_rule": surface_rule(),
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
                    "settings": CUSTOM_NOISE_SETTINGS_ID,
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


def ripe_wheat_configured_feature() -> dict[str, Any]:
    return {
        "type": "minecraft:simple_block",
        "config": {
            "to_place": {
                "type": "minecraft:simple_state_provider",
                "state": {
                    "Name": "minecraft:wheat",
                    "Properties": {"age": "7"},
                },
            }
        },
    }


def ripe_wheat_placed_feature() -> dict[str, Any]:
    return {
        "feature": RIPE_WHEAT_ID,
        "placement": [
            {
                "type": "minecraft:block_predicate_filter",
                "predicate": {
                    "type": "minecraft:all_of",
                    "predicates": [
                        {
                            "type": "minecraft:matching_blocks",
                            "blocks": ["minecraft:air"],
                        },
                        {
                            "type": "minecraft:matching_blocks",
                            "blocks": ["minecraft:farmland"],
                            "offset": [0, -1, 0],
                        },
                    ],
                },
            },
        ],
    }


def golden_wheat_field_configured_feature() -> dict[str, Any]:
    return {
        "type": "minecraft:vegetation_patch",
        "config": {
            "depth": 1,
            "extra_bottom_block_chance": 0.0,
            "extra_edge_column_chance": 0.5,
            "ground_state": {
                "type": "minecraft:simple_state_provider",
                "state": {
                    "Name": "minecraft:farmland",
                    "Properties": {"moisture": "7"},
                },
            },
            "replaceable": "#minecraft:dirt",
            "surface": "floor",
            "vegetation_chance": 0.82,
            "vegetation_feature": RIPE_WHEAT_ID,
            "vertical_range": 5,
            "xz_radius": {
                "type": "minecraft:uniform",
                "value": {"min_inclusive": 6, "max_inclusive": 9},
            },
        },
    }


def golden_wheat_fields_placed_feature() -> dict[str, Any]:
    return {
        "feature": GOLDEN_WHEAT_FIELD_CONFIG_ID,
        "placement": [
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
            {"type": "minecraft:biome"},
        ],
    }


def small_plant_patch_configured_feature(
    state: dict[str, Any], *, tries: int
) -> dict[str, Any]:
    return {
        "type": "minecraft:random_patch",
        "config": {
            "feature": {
                "feature": {
                    "type": "minecraft:simple_block",
                    "config": {
                        "to_place": {
                            "type": "minecraft:simple_state_provider",
                            "state": state,
                        }
                    },
                },
                "placement": [
                    {
                        "type": "minecraft:block_predicate_filter",
                        "predicate": {
                            "type": "minecraft:matching_blocks",
                            "blocks": "minecraft:air",
                        },
                    }
                ],
            },
            "tries": tries,
            "xz_spread": 7,
            "y_spread": 3,
        },
    }


def vegetation_groundcover_configured_features() -> dict[str, dict[str, Any]]:
    return {
        TALL_GRASS_PATCH_ID: small_plant_patch_configured_feature(
            {"Name": "minecraft:tall_grass", "Properties": {"half": "lower"}},
            tries=24,
        ),
        FERN_SPRIG_PATCH_ID: small_plant_patch_configured_feature(
            {"Name": "minecraft:fern"}, tries=16
        ),
    }


def vegetation_placed_feature(
    feature: str,
    *,
    count: int | None = None,
    rarity: int | None = None,
    heightmap: str = "WORLD_SURFACE_WG",
) -> dict[str, Any]:
    if (count is None) == (rarity is None):
        raise ValueError("Vegetation placement needs exactly one count or rarity")
    placement: list[dict[str, Any]] = []
    if count is not None:
        placement.append({"type": "minecraft:count", "count": count})
    else:
        placement.append({"type": "minecraft:rarity_filter", "chance": rarity})
    placement.extend(
        [
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": heightmap},
            {"type": "minecraft:biome"},
        ]
    )
    return {"feature": feature, "placement": placement}


def vegetation_groundcover_placed_features() -> dict[str, dict[str, Any]]:
    placed = VEGETATION_PLACED_FEATURE_IDS
    return {
        placed["groundcover_grass_lush"]: vegetation_placed_feature(
            "minecraft:patch_grass", count=2
        ),
        placed["groundcover_tall_grass"]: vegetation_placed_feature(
            TALL_GRASS_PATCH_ID, count=1
        ),
        placed["groundcover_tall_grass_lush"]: vegetation_placed_feature(
            TALL_GRASS_PATCH_ID, count=2
        ),
        placed["steppe_bloom_patches"]: vegetation_placed_feature(
            "minecraft:flower_plain", rarity=8, heightmap="MOTION_BLOCKING"
        ),
        placed["meadow_bloom_patches"]: vegetation_placed_feature(
            "minecraft:flower_meadow", rarity=4, heightmap="MOTION_BLOCKING"
        ),
        placed["forest_groundcover_grass"]: vegetation_placed_feature(
            "minecraft:patch_grass", count=1
        ),
        placed["forest_tall_grass"]: vegetation_placed_feature(
            TALL_GRASS_PATCH_ID, count=1
        ),
        placed["forest_fern_sprigs"]: vegetation_placed_feature(
            FERN_SPRIG_PATCH_ID, count=1
        ),
        placed["forest_bloom_patches"]: vegetation_placed_feature(
            "minecraft:flower_default", rarity=10, heightmap="MOTION_BLOCKING"
        ),
        placed["highland_groundcover_grass"]: vegetation_placed_feature(
            "minecraft:patch_grass", count=1
        ),
        placed["highland_tall_grass"]: vegetation_placed_feature(
            TALL_GRASS_PATCH_ID, count=1
        ),
        placed["highland_fern_sprigs"]: vegetation_placed_feature(
            FERN_SPRIG_PATCH_ID, count=1
        ),
        placed["highland_bloom_patches"]: vegetation_placed_feature(
            "minecraft:flower_default", rarity=8, heightmap="MOTION_BLOCKING"
        ),
        placed["alpine_groundcover_grass"]: vegetation_placed_feature(
            "minecraft:patch_grass", count=1
        ),
        placed["alpine_bloom_patches"]: vegetation_placed_feature(
            "minecraft:flower_meadow", rarity=5, heightmap="MOTION_BLOCKING"
        ),
    }


def generated_files(resource_root: Path, legacy: bool) -> dict[Path, dict[str, Any]]:
    files: dict[Path, dict[str, Any]] = {}
    biome_root = resource_root / "data/mushoku_worldgen/worldgen/biome"
    for name, spec in BIOMES.items():
        files[biome_root / f"{name}.json"] = biome_definition(spec, legacy)

    worldgen_root = resource_root / "data/mushoku_worldgen/worldgen"
    files[worldgen_root / "noise_settings/mushoku_overworld.json"] = noise_settings(legacy)
    files[worldgen_root / "density_function/macro_continents.json"] = macro_continents_function()
    files[worldgen_root / "density_function/landmass_density.json"] = landmass_density_function()
    files[worldgen_root / "density_function/tectonic_relief.json"] = tectonic_relief_function()
    files[worldgen_root / "configured_feature/ripe_wheat.json"] = ripe_wheat_configured_feature()
    files[worldgen_root / "configured_feature/golden_wheat_field.json"] = golden_wheat_field_configured_feature()
    files[worldgen_root / "placed_feature/ripe_wheat.json"] = ripe_wheat_placed_feature()
    files[worldgen_root / "placed_feature/golden_wheat_fields.json"] = golden_wheat_fields_placed_feature()
    configured_groundcover = vegetation_groundcover_configured_features()
    for feature_id, relative_path in VEGETATION_CONFIGURED_FEATURE_PATHS.items():
        files[resource_root / "data/mushoku_worldgen" / relative_path] = (
            configured_groundcover[feature_id]
        )
    placed_groundcover = vegetation_groundcover_placed_features()
    for feature_id, relative_path in VEGETATION_PLACED_FEATURE_PATHS.items():
        files[resource_root / "data/mushoku_worldgen" / relative_path] = (
            placed_groundcover[feature_id]
        )

    files[
        resource_root / "data/mushoku_worldgen/worldgen/world_preset/mushoku_world.json"
    ] = world_preset()
    files[resource_root / "data/minecraft/tags/worldgen/world_preset/normal.json"] = {
        "replace": False,
        "values": ["mushoku_worldgen:mushoku_world"],
    }
    biome_tag_root = resource_root / "data/minecraft/tags/worldgen/biome"
    files[biome_tag_root / "is_overworld.json"] = {
        "replace": False,
        "values": CUSTOM_BIOME_IDS,
    }
    for tag_name, biome_ids in BIOME_CATEGORY_TAGS.items():
        files[biome_tag_root / f"{tag_name}.json"] = {
            "replace": False,
            "values": biome_ids,
        }
    for tag_name, biome_ids in STRUCTURE_BIOME_TAGS.items():
        files[biome_tag_root / f"has_structure/{tag_name}.json"] = {
            "replace": False,
            "values": biome_ids,
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


def contains_value(value: Any, expected: Any) -> bool:
    if value == expected:
        return True
    if isinstance(value, dict):
        return any(contains_value(child, expected) for child in value.values())
    if isinstance(value, list):
        return any(contains_value(child, expected) for child in value)
    return False


def check_wheat_field_resources(resource_root: Path) -> None:
    feature_root = resource_root / "data/mushoku_worldgen/worldgen"
    paths = {
        "wheat": feature_root / "configured_feature/ripe_wheat.json",
        "field": feature_root / "configured_feature/golden_wheat_field.json",
        "wheat_placed": feature_root / "placed_feature/ripe_wheat.json",
        "field_placed": feature_root / "placed_feature/golden_wheat_fields.json",
    }
    documents: dict[str, dict[str, Any]] = {}
    for name, path in paths.items():
        if not path.is_file():
            raise ValueError(f"Missing wheat-field worldgen resource: {path}")
        try:
            documents[name] = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise ValueError(f"Invalid wheat-field resource {path}: {error}") from error

    wheat_state = (
        documents["wheat"].get("config", {}).get("to_place", {}).get("state", {})
    )
    if documents["wheat"].get("type") != "minecraft:simple_block" or not (
        wheat_state.get("Name") == "minecraft:wheat"
        and wheat_state.get("Properties", {}).get("age") == "7"
    ):
        raise ValueError("Golden Steppe fields must place fully grown wheat")

    field_config = documents["field"].get("config", {})
    farmland = field_config.get("ground_state", {}).get("state", {})
    if not (
        documents["field"].get("type") == "minecraft:vegetation_patch"
        and field_config.get("replaceable") == "#minecraft:dirt"
        and farmland.get("Name") == "minecraft:farmland"
        and farmland.get("Properties", {}).get("moisture") == "7"
        and field_config.get("vegetation_feature") == RIPE_WHEAT_ID
        and field_config.get("vegetation_chance", 0.0) >= 0.7
        and field_config.get("xz_radius", {}).get("type") == "minecraft:uniform"
        and field_config.get("xz_radius", {}).get("value", {}).get("min_inclusive", 0) >= 6
        and field_config.get("xz_radius", {}).get("value", {}).get("max_inclusive", 0) >= 9
    ):
        raise ValueError("Golden Steppe needs dense mature-wheat patches on moist farmland")

    if documents["field_placed"].get("feature") != GOLDEN_WHEAT_FIELD_CONFIG_ID:
        raise ValueError("Golden wheat placed feature points to the wrong configured feature")
    field_placements = documents["field_placed"].get("placement", [])
    if not all(
        any(placement.get("type") == placement_type for placement in field_placements)
        for placement_type in ("minecraft:in_square", "minecraft:heightmap", "minecraft:biome")
    ):
        raise ValueError("Golden wheat fields need surface-spread and biome placement filters")

    wheat_placed = documents["wheat_placed"]
    if wheat_placed.get("feature") != RIPE_WHEAT_ID or not contains_value(
        wheat_placed.get("placement", []), "minecraft:farmland"
    ):
        raise ValueError("Mature wheat placement must be limited to farmland")

    for biome_name in BIOMES:
        biome_path = resource_root / f"data/mushoku_worldgen/worldgen/biome/{biome_name}.json"
        try:
            biome = json.loads(biome_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise ValueError(f"Invalid biome resource {biome_path}: {error}") from error
        feature_stages = biome.get("features", [])
        if len(feature_stages) != 11:
            raise ValueError(f"Biome {biome_name} has invalid worldgen feature stages")
        vegetation = feature_stages[9]
        has_fields = GOLDEN_WHEAT_FIELDS_ID in vegetation
        if has_fields != (biome_name == "golden_steppe"):
            raise ValueError(
                "Golden wheat fields must be exclusive to the Golden Steppe biome"
            )


def check_vegetation_resources(resource_root: Path) -> None:
    feature_root = resource_root / "data/mushoku_worldgen/worldgen"
    configured = vegetation_groundcover_configured_features()
    expected_states = {
        TALL_GRASS_PATCH_ID: (
            {"Name": "minecraft:tall_grass", "Properties": {"half": "lower"}},
            24,
        ),
        FERN_SPRIG_PATCH_ID: ({"Name": "minecraft:fern"}, 16),
    }
    for feature_id, relative_path in VEGETATION_CONFIGURED_FEATURE_PATHS.items():
        path = resource_root / "data/mushoku_worldgen" / relative_path
        try:
            actual = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise ValueError(f"Invalid vegetation configured feature {path}: {error}") from error
        expected_state, expected_tries = expected_states[feature_id]
        config = actual.get("config", {})
        feature_state = (
            config.get("feature", {})
            .get("feature", {})
            .get("config", {})
            .get("to_place", {})
            .get("state", {})
        )
        if not (
            actual.get("type") == "minecraft:random_patch"
            and feature_state == expected_state
            and config.get("tries") == expected_tries
            and config.get("xz_spread") == 7
        ):
            raise ValueError(f"Vegetation patch {feature_id} has an unexpected plant or density")
        if actual != configured[feature_id]:
            raise ValueError(f"Vegetation patch {feature_id} differs from its generated definition")

    expected_placed = vegetation_groundcover_placed_features()
    for feature_id, relative_path in VEGETATION_PLACED_FEATURE_PATHS.items():
        path = resource_root / "data/mushoku_worldgen" / relative_path
        try:
            actual = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise ValueError(f"Invalid vegetation placed feature {path}: {error}") from error
        if actual != expected_placed[feature_id]:
            raise ValueError(f"Vegetation placement {feature_id} differs from its generated definition")

    all_overlay_ids = set(VEGETATION_PLACED_FEATURE_IDS.values())
    for biome_name, spec in BIOMES.items():
        path = resource_root / f"data/mushoku_worldgen/worldgen/biome/{biome_name}.json"
        try:
            biome = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise ValueError(f"Invalid vegetation biome resource {path}: {error}") from error
        feature_stages = biome.get("features", [])
        if len(feature_stages) != 11:
            raise ValueError(f"Biome {biome_name} has invalid vegetation feature stages")
        vegetation = feature_stages[9]
        actual_overlay = all_overlay_ids.intersection(vegetation)
        expected_overlay = set(spec.get("vegetation_overlay", []))
        if actual_overlay != expected_overlay:
            raise ValueError(
                f"Biome {biome_name} has the wrong Mushoku vegetation overlay: "
                f"missing={sorted(expected_overlay - actual_overlay)}, "
                f"unexpected={sorted(actual_overlay - expected_overlay)}"
            )
        if GOLDEN_WHEAT_FIELDS_ID in vegetation and not (
            max(vegetation.index(feature) for feature in expected_overlay)
            < vegetation.index(GOLDEN_WHEAT_FIELDS_ID)
        ):
            raise ValueError("Golden Steppe vegetation overlays must precede its wheat fields")
        if spec["vegetation_profile"] in {"forest", "old_growth_pine_taiga"}:
            if spec["vegetation_profile"] == "forest":
                tree_feature = (
                    "minecraft:trees_birch_and_oak"
                    if resource_root == LEGACY_RESOURCES
                    else "minecraft:trees_birch_and_oak_leaf_litter"
                )
            else:
                tree_feature = "minecraft:trees_old_growth_pine_taiga"
            if tree_feature not in vegetation or max(
                vegetation.index(feature) for feature in expected_overlay
            ) >= vegetation.index(tree_feature):
                raise ValueError(
                    f"Biome {biome_name} ground cover must be placed before its canopy feature"
                )


def check_biome_compatibility_tags(resource_root: Path) -> None:
    expected: dict[str, list[str]] = {
        "is_overworld": list(CUSTOM_BIOME_IDS),
        **BIOME_CATEGORY_TAGS,
        **{
            f"has_structure/{tag_name}": biome_ids
            for tag_name, biome_ids in STRUCTURE_BIOME_TAGS.items()
        },
    }
    tag_root = resource_root / "data/minecraft/tags/worldgen/biome"
    for tag_name, biome_ids in expected.items():
        path = tag_root / f"{tag_name}.json"
        try:
            tag = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise ValueError(f"Missing or invalid biome compatibility tag {path}: {error}") from error
        if tag.get("replace") is not False or tag.get("values") != biome_ids:
            raise ValueError(
                f"Biome compatibility tag {tag_name} must append only its intended Mushoku biomes"
            )


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
    if overworld.get("settings") != CUSTOM_NOISE_SETTINGS_ID:
        raise ValueError("Mushoku: Vast Lands must use its isolated custom noise settings")
    noise_settings_path = resource_root / "data/mushoku_worldgen/worldgen/noise_settings/mushoku_overworld.json"
    if not noise_settings_path.is_file():
        raise ValueError(f"Missing Mushoku terrain settings: {noise_settings_path}")
    try:
        settings = json.loads(noise_settings_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"Invalid Mushoku terrain settings: {error}") from error
    router = settings.get("noise_router", {})
    if router.get("continents") != CUSTOM_DENSITY_FUNCTION_IDS[0]:
        raise ValueError("Mushoku noise router is missing its broad continentalness field")
    if not contains_value(router.get("final_density"), CUSTOM_DENSITY_FUNCTION_IDS[1]):
        raise ValueError("Mushoku final density does not apply the broad landmass overlay")
    if "surface_rule" not in settings or settings.get("sea_level") != 63:
        raise ValueError("Mushoku noise settings are missing their Overworld surface configuration")
    try:
        pack_format = json.loads(
            (resource_root / "pack.mcmeta").read_text(encoding="utf-8")
        )["pack"]["pack_format"]
    except (OSError, json.JSONDecodeError, KeyError, TypeError) as error:
        raise ValueError(f"Unable to determine Worldgen pack version: {error}") from error
    if pack_format == 15 and not {"initial_density_without_jaggedness", "lava"}.issubset(router):
        raise ValueError("Legacy 1.20.1 noise router is missing its aquifer/surface fields")
    if pack_format == 75:
        preliminary = router.get("preliminary_surface_level", {})
        if preliminary.get("type") != "minecraft:find_top_surface":
            raise ValueError("Fabric noise router is missing its preliminary surface sampler")
    density_documents: dict[str, dict[str, Any]] = {}
    for density_function_id in CUSTOM_DENSITY_FUNCTION_IDS:
        namespace, name = density_function_id.split(":", 1)
        density_path = resource_root / f"data/{namespace}/worldgen/density_function/{name}.json"
        if not density_path.is_file():
            raise ValueError(f"Missing Mushoku density function: {density_path}")
        try:
            density_documents[density_function_id] = json.loads(
                density_path.read_text(encoding="utf-8")
            )
        except (OSError, json.JSONDecodeError) as error:
            raise ValueError(f"Invalid Mushoku density function {density_function_id}: {error}") from error

    if not contains_value(
        density_documents[CUSTOM_DENSITY_FUNCTION_IDS[1]], CUSTOM_DENSITY_FUNCTION_IDS[2]
    ):
        raise ValueError("Mushoku landmass overlay does not include its terrain-relief blend")
    relief = density_documents[CUSTOM_DENSITY_FUNCTION_IDS[2]]
    if not all(
        contains_value(relief, field)
        for field in ("minecraft:overworld/erosion", "minecraft:overworld/ridges")
    ):
        raise ValueError("Mushoku terrain relief is missing its erosion/ridge shaping fields")
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
    check_wheat_field_resources(resource_root)
    check_vegetation_resources(resource_root)
    check_biome_compatibility_tags(resource_root)


def render(value: dict[str, Any]) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2) + "\n"


def update(root: Path, check: bool) -> int:
    mismatches: list[str] = []
    for legacy in (False, True):
        try:
            check_feature_ordering(legacy)
        except ValueError as error:
            version = "ports/worldgen-common" if legacy else "worldgen"
            mismatches.append(f"{version}: {error}")

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
