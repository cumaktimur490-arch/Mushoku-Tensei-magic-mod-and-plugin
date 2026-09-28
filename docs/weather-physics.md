# Local weather physics and severe weather

The server simulates climate and hazards only in loaded areas near players. It does not enable dimension-wide vanilla rain. Broad regional fronts continue to use the mod's biome-, temperature-, pressure-, and wind-aware climate model; particles are emitted around players rather than across the whole dimension.

## Regional weather and moving hazards

- Exposed living entities accumulate temporary wetness in rain and dry over time; snow and hail wet them more slowly. Heavy precipitation extinguishes exposed flames and can put out uncovered fire blocks.
- Strong regional wind nudges exposed creatures. Tornado cores add a rotating, inward-flowing wind field and a vertical updraft that can lift nearby creatures.
- Warm, humid low-pressure storms can form large moving cyclones, strong thunderstorms can produce localized tornadoes, cool thunderstorms can produce hail, and hot, dry biomes can form drifting sandstorms. Systems pass through smooth forming, mature, and dissipating phases, then expire. Their weather fields and particles are local and do not load chunks.
- Severe weather creates stacked cloud and precipitation particle layers, hail grains, blown dust, and a rotating funnel. This is a lightweight particle-based rendering effect, not a custom volumetric-cloud renderer or a full atmospheric fluid solver.
- `severeWeatherEnabled` controls natural cyclones, tornadoes, hailstorms, and sandstorms. `weatherBlockDamage` defaults to `false`; when enabled, tornadoes and cyclones may remove at most 24 fragile blocks per storm system near their path. Weather visuals and entity effects still work with block damage disabled.
- Cumulonimbus remains a separate player-cast storm limited to its 20×20-chunk sector. Severe weather never changes dimension-wide weather. Set `regionalWeatherEnabled` to `false` to use vanilla weather instead.
- While regional weather is enabled, the manager intentionally holds vanilla's global weather clear. Operators should use `/magicweather <clear|cloudy|rain|thunder|snow> [seconds]` to set an explicit local override (default 120 seconds, 160-block radius), or `/magicweather status` to inspect the area. `/magicweather hail`, `tornado`, `cyclone`, and `sandstorm` start moving local hazards; these require `severeWeatherEnabled=true`. `clear` also removes nearby severe systems. This is the working in-game control; vanilla `/weather` only controls the world after regional weather is disabled.
- The simulation does not wet blocks, fill cauldrons, or apply crop-weather ticks.

## Spell interactions

- Wind and updraft fields influence free-flight Fire Bolt, Explosive Fireball, Water Ball, Water Cannon, Ice Needle, and Stone Ball trajectories. Lighter spells drift more; stone resists drift. Horizontal drift is capped at four blocks and vertical lift at two, and wind cannot extend a cast beyond its scaled maximum range.
- Gust and Updraft leave a brief, radius-limited air current that can affect subsequent spell paths. These fields fade in and out and are removed when the server stops.
- Rain and accumulated wetness modestly reduce Fire Bolt damage, sharply shorten its burn duration, and suppress fire-spell ignition. Heavy rain also dampens explosive fire ignition without disabling the configured block-destruction setting.
- Humid/rainy air modestly strengthens Water Cannon hits; low temperatures favor Ice Needle. These elemental adjustments are capped and multiply, rather than replace, the existing staff and spell scaling.

The effects are deliberately bounded around loaded player areas so weather adds visible and tactical feedback without overwhelming the mod's ×2, ×15, and ×50 staff multipliers or creating unbounded server work.
