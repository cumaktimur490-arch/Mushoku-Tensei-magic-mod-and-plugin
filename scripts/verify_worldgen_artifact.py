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
CUSTOM_NOISE_SETTINGS = "mushoku_worldgen:mushoku_overworld"
NOISE_SETTINGS = "data/mushoku_worldgen/worldgen/noise_settings/mushoku_overworld.json"
DENSITY_FUNCTIONS = {
    "macro_continents": "data/mushoku_worldgen/worldgen/density_function/macro_continents.json",
    "landmass_density": "data/mushoku_worldgen/worldgen/density_function/landmass_density.json",
    "tectonic_relief": "data/mushoku_worldgen/worldgen/density_function/tectonic_relief.json",
}
WHEAT_RESOURCES = {
    "ripe_wheat_configured": "data/mushoku_worldgen/worldgen/configured_feature/ripe_wheat.json",
    "golden_field_configured": "data/mushoku_worldgen/worldgen/configured_feature/golden_wheat_field.json",
    "ripe_wheat_placed": "data/mushoku_worldgen/worldgen/placed_feature/ripe_wheat.json",
    "golden_fields_placed": "data/mushoku_worldgen/worldgen/placed_feature/golden_wheat_fields.json",
}
VEGETATION_CONFIGURED_FEATURES = {
    "tall_grass_clump": "data/mushoku_worldgen/worldgen/configured_feature/tall_grass_clump.json",
    "fern_sprig_patch": "data/mushoku_worldgen/worldgen/configured_feature/fern_sprig_patch.json",
}
VEGETATION_PLACED_FEATURES = {
    "groundcover_grass_lush": ("minecraft:patch_grass", "minecraft:count", 2, "WORLD_SURFACE_WG"),
    "groundcover_tall_grass": ("mushoku_worldgen:tall_grass_clump", "minecraft:count", 1, "WORLD_SURFACE_WG"),
    "groundcover_tall_grass_lush": ("mushoku_worldgen:tall_grass_clump", "minecraft:count", 2, "WORLD_SURFACE_WG"),
    "steppe_bloom_patches": ("minecraft:flower_plain", "minecraft:rarity_filter", 8, "MOTION_BLOCKING"),
    "meadow_bloom_patches": ("minecraft:flower_meadow", "minecraft:rarity_filter", 4, "MOTION_BLOCKING"),
    "forest_groundcover_grass": ("minecraft:patch_grass", "minecraft:count", 1, "WORLD_SURFACE_WG"),
    "forest_tall_grass": ("mushoku_worldgen:tall_grass_clump", "minecraft:count", 1, "WORLD_SURFACE_WG"),
    "forest_fern_sprigs": ("mushoku_worldgen:fern_sprig_patch", "minecraft:count", 1, "WORLD_SURFACE_WG"),
    "forest_bloom_patches": ("minecraft:flower_default", "minecraft:rarity_filter", 10, "MOTION_BLOCKING"),
    "highland_groundcover_grass": ("minecraft:patch_grass", "minecraft:count", 1, "WORLD_SURFACE_WG"),
    "highland_tall_grass": ("mushoku_worldgen:tall_grass_clump", "minecraft:count", 1, "WORLD_SURFACE_WG"),
    "highland_fern_sprigs": ("mushoku_worldgen:fern_sprig_patch", "minecraft:count", 1, "WORLD_SURFACE_WG"),
    "highland_bloom_patches": ("minecraft:flower_default", "minecraft:rarity_filter", 8, "MOTION_BLOCKING"),
    "alpine_groundcover_grass": ("minecraft:patch_grass", "minecraft:count", 1, "WORLD_SURFACE_WG"),
    "alpine_bloom_patches": ("minecraft:flower_meadow", "minecraft:rarity_filter", 5, "MOTION_BLOCKING"),
}
VEGETATION_PLACED_FEATURE_PATHS = {
    name: f"data/mushoku_worldgen/worldgen/placed_feature/{name}.json"
    for name in VEGETATION_PLACED_FEATURES
}
VEGETATION_OVERLAYS_BY_BIOME = {
    "golden_steppe": {
        "mushoku_worldgen:groundcover_grass_lush",
        "mushoku_worldgen:groundcover_tall_grass",
        "mushoku_worldgen:steppe_bloom_patches",
    },
    "riverside_meadow": {
        "mushoku_worldgen:groundcover_grass_lush",
        "mushoku_worldgen:groundcover_tall_grass_lush",
        "mushoku_worldgen:meadow_bloom_patches",
    },
    "whispering_forest": {
        "mushoku_worldgen:forest_groundcover_grass",
        "mushoku_worldgen:forest_tall_grass",
        "mushoku_worldgen:forest_fern_sprigs",
        "mushoku_worldgen:forest_bloom_patches",
    },
    "emerald_highlands": {
        "mushoku_worldgen:highland_groundcover_grass",
        "mushoku_worldgen:highland_tall_grass",
        "mushoku_worldgen:highland_fern_sprigs",
        "mushoku_worldgen:highland_bloom_patches",
    },
    "skyreach_mountains": {
        "mushoku_worldgen:alpine_groundcover_grass",
        "mushoku_worldgen:alpine_bloom_patches",
    },
}
GOLDEN_WHEAT_FIELDS_ID = "mushoku_worldgen:golden_wheat_fields"
GOLDEN_WHEAT_FIELD_CONFIG_ID = "mushoku_worldgen:golden_wheat_field"
RIPE_WHEAT_ID = "mushoku_worldgen:ripe_wheat"
MACRO_CONTINENTS_ID = "mushoku_worldgen:macro_continents"
LANDMASS_DENSITY_ID = "mushoku_worldgen:landmass_density"
TECTONIC_RELIEF_ID = "mushoku_worldgen:tectonic_relief"


def load_json(jar: ZipFile, path: str) -> dict:
    try:
        return json.loads(jar.read(path))
    except (KeyError, json.JSONDecodeError) as error:
        raise ValueError(f"Missing or invalid JSON resource {path}: {error}") from error


def contains_value(value: object, expected: object) -> bool:
    if value == expected:
        return True
    if isinstance(value, dict):
        return any(contains_value(child, expected) for child in value.values())
    if isinstance(value, list):
        return any(contains_value(child, expected) for child in value)
    return False


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
            required.add(NOISE_SETTINGS)
            required.update(DENSITY_FUNCTIONS.values())
            required.update(WHEAT_RESOURCES.values())
            required.update(VEGETATION_CONFIGURED_FEATURES.values())
            required.update(VEGETATION_PLACED_FEATURE_PATHS.values())
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
            if overworld.get("settings") != CUSTOM_NOISE_SETTINGS:
                raise ValueError(f"{jar_path} does not use isolated Mushoku terrain settings")
            noise_settings = load_json(jar, NOISE_SETTINGS)
            noise_router = noise_settings.get("noise_router", {})
            if noise_router.get("continents") != MACRO_CONTINENTS_ID:
                raise ValueError(f"{jar_path} is missing the broad continentalness router")
            if not contains_value(noise_router.get("final_density"), LANDMASS_DENSITY_ID):
                raise ValueError(f"{jar_path} final density does not use its continent overlay")
            if loader == "fabric":
                if noise_router.get("preliminary_surface_level", {}).get("type") != "minecraft:find_top_surface":
                    raise ValueError(f"{jar_path} has no modern preliminary-surface sampler")
            elif not {"initial_density_without_jaggedness", "lava"}.issubset(noise_router):
                raise ValueError(f"{jar_path} has an incomplete Forge/NeoForge aquifer router")
            if "surface_rule" not in noise_settings:
                raise ValueError(f"{jar_path} has no custom Overworld surface rule")
            density_documents = {
                density_id: load_json(jar, resource)
                for density_id, resource in DENSITY_FUNCTIONS.items()
            }
            if not contains_value(
                density_documents["landmass_density"], TECTONIC_RELIEF_ID
            ):
                raise ValueError(f"{jar_path} landmass overlay omits its terrain-relief blend")
            relief = density_documents["tectonic_relief"]
            if not all(
                contains_value(relief, field)
                for field in ("minecraft:overworld/erosion", "minecraft:overworld/ridges")
            ):
                raise ValueError(f"{jar_path} terrain relief omits erosion/ridge shaping")

            wheat = {
                name: load_json(jar, resource)
                for name, resource in WHEAT_RESOURCES.items()
            }
            ripe_wheat = wheat["ripe_wheat_configured"].get("config", {}).get(
                "to_place", {}
            ).get("state", {})
            field_config = wheat["golden_field_configured"].get("config", {})
            ground_state = field_config.get("ground_state", {}).get("state", {})
            if not (
                ripe_wheat.get("Name") == "minecraft:wheat"
                and ripe_wheat.get("Properties", {}).get("age") == "7"
                and wheat["golden_field_configured"].get("type") == "minecraft:vegetation_patch"
                and ground_state.get("Name") == "minecraft:farmland"
                and field_config.get("vegetation_feature") == RIPE_WHEAT_ID
                and field_config.get("vegetation_chance", 0.0) >= 0.7
                and field_config.get("xz_radius", {}).get("value", {}).get("min_inclusive", 0) >= 6
                and wheat["golden_fields_placed"].get("feature") == GOLDEN_WHEAT_FIELD_CONFIG_ID
            ):
                raise ValueError(f"{jar_path} wheat fields are not configured as dense mature crops")
            field_placements = wheat["golden_fields_placed"].get("placement", [])
            if not (
                any(item.get("type") == "minecraft:in_square" for item in field_placements)
                and any(
                    item.get("type") == "minecraft:heightmap"
                    and item.get("heightmap") == "WORLD_SURFACE_WG"
                    for item in field_placements
                )
                and any(item.get("type") == "minecraft:biome" for item in field_placements)
            ):
                raise ValueError(f"{jar_path} wheat-field placement is missing its surface spread")
            if not contains_value(
                wheat["ripe_wheat_placed"].get("placement", []), "minecraft:farmland"
            ):
                raise ValueError(f"{jar_path} mature wheat placement is missing its farmland filter")

            vegetation_configured = {
                name: load_json(jar, resource)
                for name, resource in VEGETATION_CONFIGURED_FEATURES.items()
            }
            expected_plant_patches = {
                "tall_grass_clump": (
                    {"Name": "minecraft:tall_grass", "Properties": {"half": "lower"}},
                    24,
                ),
                "fern_sprig_patch": ({"Name": "minecraft:fern"}, 16),
            }
            for name, (state, tries) in expected_plant_patches.items():
                configured = vegetation_configured[name]
                config = configured.get("config", {})
                placed_state = (
                    config.get("feature", {})
                    .get("feature", {})
                    .get("config", {})
                    .get("to_place", {})
                    .get("state", {})
                )
                if not (
                    configured.get("type") == "minecraft:random_patch"
                    and placed_state == state
                    and config.get("tries") == tries
                ):
                    raise ValueError(
                        f"{jar_path} vegetation feature {name} has an unexpected plant patch"
                    )

            vegetation_placed = {
                name: load_json(jar, resource)
                for name, resource in VEGETATION_PLACED_FEATURE_PATHS.items()
            }
            for name, (target, modifier, value, heightmap) in VEGETATION_PLACED_FEATURES.items():
                placed = vegetation_placed[name]
                placements = placed.get("placement", [])
                if not (
                    placed.get("feature") == target
                    and len(placements) == 4
                    and placements[0].get("type") == modifier
                    and placements[0].get("count", placements[0].get("chance")) == value
                    and placements[1].get("type") == "minecraft:in_square"
                    and placements[2].get("type") == "minecraft:heightmap"
                    and placements[2].get("heightmap") == heightmap
                    and placements[3].get("type") == "minecraft:biome"
                ):
                    raise ValueError(
                        f"{jar_path} vegetation placement {name} has an invalid target or spread"
                    )

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
                feature_stages = definition.get("features", [])
                if len(feature_stages) != 11:
                    raise ValueError(f"{jar_path} biome {biome_id} has invalid feature stages")
                vegetation = feature_stages[9]
                if (GOLDEN_WHEAT_FIELDS_ID in vegetation) != (biome == "golden_steppe"):
                    raise ValueError(f"{jar_path} wheat fields are not limited to Golden Steppe")
                custom_vegetation_ids = {
                    f"mushoku_worldgen:{name}"
                    for name in VEGETATION_PLACED_FEATURES
                }
                actual_overlay = custom_vegetation_ids.intersection(vegetation)
                expected_overlay = VEGETATION_OVERLAYS_BY_BIOME[biome]
                if actual_overlay != expected_overlay:
                    raise ValueError(
                        f"{jar_path} biome {biome_id} has an unexpected ground-cover overlay: "
                        f"missing={sorted(expected_overlay - actual_overlay)}, "
                        f"unexpected={sorted(actual_overlay - expected_overlay)}"
                    )
                if biome == "golden_steppe":
                    field_index = vegetation.index(GOLDEN_WHEAT_FIELDS_ID)
                    if max(vegetation.index(feature) for feature in expected_overlay) >= field_index:
                        raise ValueError(f"{jar_path} Golden Steppe wheat fields must follow ground cover")
                if biome in {"whispering_forest", "emerald_highlands"}:
                    if biome == "whispering_forest":
                        tree_feature = (
                            "minecraft:trees_birch_and_oak_leaf_litter"
                            if loader == "fabric"
                            else "minecraft:trees_birch_and_oak"
                        )
                    else:
                        tree_feature = "minecraft:trees_old_growth_pine_taiga"
                    if (
                        tree_feature not in vegetation
                        or max(vegetation.index(feature) for feature in expected_overlay)
                        >= vegetation.index(tree_feature)
                    ):
                        raise ValueError(
                            f"{jar_path} biome {biome_id} ground cover must precede its trees"
                        )
                if biome == "golden_steppe":
                    deep_dark_order = [
                        "minecraft:glow_lichen",
                        "minecraft:patch_tall_grass_2",
                        "minecraft:trees_plains",
                        "minecraft:flower_plains",
                        "minecraft:patch_grass_plain",
                        "minecraft:brown_mushroom_normal",
                        "minecraft:red_mushroom_normal",
                        "minecraft:patch_pumpkin",
                    ]
                    deep_dark_features = set(deep_dark_order)
                    shared = [
                        feature
                        for feature in feature_stages[9]
                        if feature in deep_dark_features
                    ]
                    if shared != deep_dark_order:
                        raise ValueError(
                            f"{jar_path} Golden Steppe has a feature-order conflict with minecraft:deep_dark"
                        )

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
