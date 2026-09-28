# Local weather physics and spell interactions

This simulation is server-side and local to exposed, loaded areas near players. It does not turn on dimension-wide vanilla rain. Natural regional fronts use the mod's existing biome-, temperature-, pressure-, and wind-aware model; Cumulonimbus adds a cyclonic storm field that remains within its 20×20-chunk sector.

## Physical effects

- Exposed living entities accumulate temporary wetness during rain. Rain fills the wetness meter gradually; warmer air dries it faster, while snow wets more slowly.
- Heavy rain extinguishes burning creatures and uncovered fire blocks near active players. Sheltered entities and fire are not treated as exposed to rain.
- Strong exposed wind gently pushes nearby creatures; storm conditions combine with regional wind, humidity, precipitation, and convective lift. Storm intensity fades smoothly near the sector edge.
- Weather physics only processes loaded areas around active players. It does not wet blocks, fill cauldrons, accelerate crop growth, or change global weather.

## Spell interactions

- Wind and updraft fields influence free-flight Fire Bolt, Explosive Fireball, Water Ball, Water Cannon, Ice Needle, and Stone Ball trajectories. Lighter spells drift more; stone resists drift. Horizontal drift is capped at four blocks and vertical lift at two, and wind cannot extend a cast beyond its scaled maximum range.
- Gust and Updraft leave a brief, radius-limited air current that can affect subsequent spell paths. These fields fade in and out and are removed when the server stops.
- Rain and accumulated wetness modestly reduce Fire Bolt damage, sharply shorten its burn duration, and suppress fire-spell ignition. Heavy rain also dampens explosive fire ignition without disabling the configured block-destruction setting.
- Humid/rainy air modestly strengthens Water Cannon hits; low temperatures favor Ice Needle. These elemental adjustments are capped and multiply, rather than replace, the existing staff and spell scaling.

The effects are deliberately bounded so weather adds tactical feedback without overwhelming the mod's existing ×2, ×15, and ×50 staff multipliers.
