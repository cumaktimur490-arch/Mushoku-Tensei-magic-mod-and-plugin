package com.mushokumagic.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.class_1309;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_2680;
import net.minecraft.class_2374;
import net.minecraft.server.MinecraftServer;

/** Applies local precipitation and wind to exposed creatures and elemental spells. */
public final class WeatherPhysics {
    private static final int PHYSICS_INTERVAL_TICKS = 20;
    private static final int MAX_WIND_PULSES_PER_WORLD = 24;
    private static final double ENTITY_SCAN_RADIUS = 16.0;
    private static final Map<class_3218, List<WindPulse>> WIND_PULSES = new IdentityHashMap<>();
    private static final Map<class_1309, Double> WETNESS = new WeakHashMap<>();

    private WeatherPhysics() {
    }

    public static Conditions sampleAt(class_3218 level, class_243 position) {
        RegionalWeatherModel.WeatherState regional = RegionalWeatherManager.sampleAt(
                level,
                position.method_10216(),
                position.method_10214(),
                position.method_10215());
        LocalStormManager.StormWeather storm = LocalStormManager.weatherAt(
                level,
                position.method_10216(),
                position.method_10215());
        SevereWeatherManager.WeatherSample severe = SevereWeatherManager.weatherAt(
                level,
                position.method_10216(),
                position.method_10214(),
                position.method_10215());
        class_2338 blockPos = class_2338.method_49638((class_2374)position);
        boolean exposed = level.method_8311(blockPos);
        double localStormInfluence = exposed ? storm.intensity() : 0.0;
        double severeInfluence = exposed ? severe.intensity() : 0.0;
        double stormInfluence = Math.max(localStormInfluence, severeInfluence);
        double regionalWindStrength = exposed ? regional.windStrength() : 0.0;
        double stormForceX = storm.windX() * storm.windStrength() * localStormInfluence
                + severe.windX() * severe.windStrength() * severeInfluence;
        double stormForceZ = storm.windZ() * storm.windStrength() * localStormInfluence
                + severe.windZ() * severe.windStrength() * severeInfluence;
        double stormForceLength = Math.hypot(stormForceX, stormForceZ);
        double stormWindX = stormForceLength > 1.0E-8 ? stormForceX / stormForceLength : 1.0;
        double stormWindZ = stormForceLength > 1.0E-8 ? stormForceZ / stormForceLength : 0.0;
        double stormWindStrength = stormInfluence > 1.0E-8
                ? Math.min(1.25, stormForceLength / stormInfluence)
                : 0.0;
        double stormLift = storm.verticalLift() * localStormInfluence + severe.verticalLift();
        PulseInfluence pulse = WeatherPhysics.pulseAt(level, position);
        double lift = (exposed
                ? regional.cloudCover() * regional.humidity() * 0.16 + stormLift
                : 0.0) + pulse.verticalLift();
        WeatherPhysicsModel.Wind wind = WeatherPhysicsModel.combineWind(
                regional.windX(),
                regional.windZ(),
                regionalWindStrength,
                stormWindX,
                stormWindZ,
                exposed ? stormWindStrength : 0.0,
                stormInfluence,
                pulse.x(),
                pulse.z(),
                lift);
        double precipitation = exposed
                ? Math.max(regional.precipitationIntensity(),
                        Math.max(storm.intensity(), severe.precipitationIntensity()))
                : 0.0;
        double humidity = Math.max(regional.humidity(),
                0.72 * Math.max(storm.intensity(), severe.intensity()));
        boolean hailing = exposed && severe.hailing();
        boolean snowing = precipitation > 0.02
                && !hailing
                && (regional.precipitation() == RegionalWeatherModel.Precipitation.SNOW
                        || regional.temperature() <= 0.15);
        return new Conditions(
                wind.x(),
                wind.z(),
                wind.strength(),
                wind.verticalLift(),
                humidity,
                precipitation,
                regional.temperature(),
                stormInfluence,
                exposed ? severe.tornadoIntensity() : 0.0,
                snowing,
                hailing,
                exposed && (regional.thunderstorm() || storm.thunderstorm()
                        || severe.kind() == SevereWeatherModel.Kind.TORNADO
                        || severe.kind() == SevereWeatherModel.Kind.HAIL
                        || severe.kind() == SevereWeatherModel.Kind.HURRICANE));
    }

    /**
     * Bends only free-flight spells. The same result is used by the cast visuals and
     * the impact code so the visible projectile follows the weather-adjusted trajectory.
     */
    public static class_243 driftProjectileAimPoint(
            class_3218 level,
            class_243 origin,
            class_243 target,
            String spellId,
            double maxRange) {
        if (WeatherPhysicsModel.projectileWindResponse(spellId) <= 0.0) {
            return target;
        }
        class_243 segment = target.method_1020(origin);
        double distance = Math.sqrt(segment.method_1027());
        if (distance < 1.0E-6) {
            return target;
        }
        int samples = Math.max(2, Math.min(6, (int)Math.ceil(distance / 24.0)));
        double windX = 0.0;
        double windZ = 0.0;
        double verticalLift = 0.0;
        double totalWeight = 0.0;
        for (int sampleIndex = 0; sampleIndex <= samples; ++sampleIndex) {
            double amount = (double)sampleIndex / (double)samples;
            class_243 samplePosition = origin.method_1019(segment.method_1021(amount));
            Conditions conditions = WeatherPhysics.sampleAt(level, samplePosition);
            double weight = sampleIndex == 0 || sampleIndex == samples ? 0.5 : 1.0;
            windX += conditions.windX() * conditions.windStrength() * weight;
            windZ += conditions.windZ() * conditions.windStrength() * weight;
            verticalLift += conditions.verticalLift() * weight;
            totalWeight += weight;
        }
        windX /= totalWeight;
        windZ /= totalWeight;
        verticalLift /= totalWeight;
        double windStrength = Math.hypot(windX, windZ);
        WeatherPhysicsModel.Drift drift = WeatherPhysicsModel.projectileDrift(
                distance,
                spellId,
                windX,
                windZ,
                windStrength,
                verticalLift);
        class_243 adjusted = target.method_1019(new class_243(drift.x(), drift.y(), drift.z()));
        double range = Double.isFinite(maxRange) ? Math.max(0.0, maxRange) : distance;
        class_243 adjustedSegment = adjusted.method_1020(origin);
        double adjustedDistance = Math.sqrt(adjustedSegment.method_1027());
        if (adjustedDistance > range && range > 0.0) {
            adjusted = origin.method_1019(adjustedSegment.method_1021(range / adjustedDistance));
        }
        return adjusted;
    }

    public static double elementalPowerMultiplier(
            class_3218 level,
            class_243 position,
            String element,
            class_1309 target) {
        Conditions conditions = WeatherPhysics.sampleAt(level, position);
        double wetness = target == null ? 0.0 : WeatherPhysics.wetnessOf(target);
        return WeatherPhysicsModel.elementalPowerMultiplier(
                element,
                conditions.humidity(),
                conditions.precipitationIntensity(),
                conditions.temperature(),
                conditions.windStrength(),
                wetness);
    }

    public static double fireDurationMultiplier(class_3218 level, class_243 position, class_1309 target) {
        Conditions conditions = WeatherPhysics.sampleAt(level, position);
        double wetness = target == null ? 0.0 : WeatherPhysics.wetnessOf(target);
        return WeatherPhysicsModel.fireDurationMultiplier(conditions.precipitationIntensity(), wetness);
    }

    public static double ignitionMultiplier(class_3218 level, class_243 position) {
        Conditions conditions = WeatherPhysics.sampleAt(level, position);
        return WeatherPhysicsModel.ignitionMultiplier(conditions.precipitationIntensity(), 0.0);
    }

    public static double wetnessOf(class_1309 entity) {
        return WETNESS.getOrDefault(entity, 0.0);
    }

    public static void addGust(
            class_3218 level,
            class_243 center,
            double directionX,
            double directionZ,
            double strength,
            double radius,
            int durationTicks) {
        WeatherPhysics.addPulse(level, center, directionX, directionZ, strength, 0.0, radius, durationTicks);
    }

    public static void addUpdraft(
            class_3218 level,
            class_243 center,
            double strength,
            double radius,
            int durationTicks) {
        WeatherPhysics.addPulse(level, center, 0.0, 0.0, 0.0, strength, radius, durationTicks);
    }

    public static void tick(MinecraftServer server) {
        List<class_3222> players = server.method_3760().method_14571();
        for (class_3218 level : server.method_3738()) {
            long now = level.method_75260();
            WeatherPhysics.prunePulses(level, now);
            if (now % PHYSICS_INTERVAL_TICKS != 0L) {
                continue;
            }
            WeatherPhysics.dryEntitiesIn(level, PHYSICS_INTERVAL_TICKS);
            Set<class_1309> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<WeatherCell> dousedCells = new HashSet<>();
            for (class_3222 player : players) {
                if (player.method_51469() != level) {
                    continue;
                }
                class_238 searchBox = player.method_5829().method_1014(ENTITY_SCAN_RADIUS);
                for (class_1309 entity : level.method_18467(class_1309.class, searchBox)) {
                    if (!visited.add(entity)) {
                        continue;
                    }
                    class_243 position = entity.method_73189();
                    class_2338 blockPos = class_2338.method_49638((class_2374)position);
                    if (!level.method_8311(blockPos)) {
                        continue;
                    }
                    Conditions conditions = WeatherPhysics.sampleAt(level, position);
                    WeatherPhysics.applyAirflow(entity, conditions);
                    if (conditions.precipitationIntensity() <= 0.02) {
                        continue;
                    }
                    double wetting = conditions.snowing() || conditions.hailing()
                            ? conditions.precipitationIntensity() * 0.35
                            : conditions.precipitationIntensity();
                    double wetness = WeatherPhysicsModel.wetnessAfterRain(
                            WeatherPhysics.wetnessOf(entity),
                            wetting,
                            PHYSICS_INTERVAL_TICKS);
                    WETNESS.put(entity, wetness);
                    if (!conditions.snowing()
                            && conditions.precipitationIntensity() >= 0.62
                            && wetness >= 0.25) {
                        entity.method_5646();
                        WeatherCell cell = WeatherCell.from(position);
                        if (dousedCells.add(cell)) {
                            WeatherPhysics.douseExposedFires(level, blockPos);
                        }
                    }
                }
            }
        }
    }

    public static void clear() {
        WIND_PULSES.clear();
        WETNESS.clear();
    }

    private static void addPulse(
            class_3218 level,
            class_243 center,
            double directionX,
            double directionZ,
            double horizontalStrength,
            double verticalStrength,
            double radius,
            int durationTicks) {
        if (durationTicks <= 0 || !Double.isFinite(radius) || radius <= 0.0) {
            return;
        }
        double length = Math.hypot(directionX, directionZ);
        if (length > 1.0E-8) {
            directionX /= length;
            directionZ /= length;
        }
        double safeHorizontal = WeatherPhysics.clamp(horizontalStrength, 0.0, 1.25);
        double safeVertical = WeatherPhysics.clamp(verticalStrength, 0.0, 1.25);
        if (safeHorizontal <= 0.0 && safeVertical <= 0.0) {
            return;
        }
        long now = level.method_75260();
        List<WindPulse> pulses = WIND_PULSES.computeIfAbsent(level, ignored -> new ArrayList<>());
        pulses.removeIf(pulse -> now >= pulse.endTick());
        if (pulses.size() >= MAX_WIND_PULSES_PER_WORLD) {
            pulses.remove(0);
        }
        pulses.add(new WindPulse(
                center.method_10216(),
                center.method_10215(),
                directionX,
                directionZ,
                safeHorizontal,
                safeVertical,
                Math.max(1.0, Math.min(64.0, radius)),
                now,
                now + durationTicks));
    }

    private static PulseInfluence pulseAt(class_3218 level, class_243 position) {
        List<WindPulse> pulses = WIND_PULSES.get(level);
        if (pulses == null || pulses.isEmpty()) {
            return PulseInfluence.NONE;
        }
        long now = level.method_75260();
        double x = position.method_10216();
        double z = position.method_10215();
        double gustX = 0.0;
        double gustZ = 0.0;
        double lift = 0.0;
        for (WindPulse pulse : pulses) {
            if (now < pulse.startTick() || now >= pulse.endTick()) {
                continue;
            }
            double distance = Math.hypot(x - pulse.x(), z - pulse.z());
            if (distance >= pulse.radius()) {
                continue;
            }
            double radial = 1.0 - distance / pulse.radius();
            double fadeIn = Math.min(1.0, (double)(now - pulse.startTick()) / 5.0);
            double fadeOut = Math.min(1.0, (double)(pulse.endTick() - now) / 20.0);
            double influence = radial * radial * Math.min(fadeIn, fadeOut);
            gustX += pulse.directionX() * pulse.horizontalStrength() * influence;
            gustZ += pulse.directionZ() * pulse.horizontalStrength() * influence;
            lift += pulse.verticalStrength() * influence;
        }
        double horizontalLength = Math.hypot(gustX, gustZ);
        if (horizontalLength > 1.25) {
            gustX *= 1.25 / horizontalLength;
            gustZ *= 1.25 / horizontalLength;
        }
        return new PulseInfluence(gustX, gustZ, Math.min(1.25, lift));
    }

    private static void dryEntitiesIn(class_3218 level, int elapsedTicks) {
        Iterator<Map.Entry<class_1309, Double>> iterator = WETNESS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<class_1309, Double> entry = iterator.next();
            class_1309 entity = entry.getKey();
            if (entity == null || !(entity.method_73183() instanceof class_3218 entityLevel) || entityLevel != level) {
                continue;
            }
            double temperature = WeatherPhysics.sampleAt(level, entity.method_73189()).temperature();
            double dried = WeatherPhysicsModel.wetnessAfterDrying(entry.getValue(), temperature, elapsedTicks);
            if (dried <= 0.001) {
                iterator.remove();
            } else {
                entry.setValue(dried);
            }
        }
    }

    private static void applyAirflow(class_1309 entity, Conditions conditions) {
        double tornado = conditions.tornadoIntensity();
        if (conditions.windStrength() < 0.85 && tornado < 0.05) {
            return;
        }
        double playerFactor = entity instanceof class_3222
                ? (tornado > 0.05 ? 0.65 : conditions.stormIntensity() > 0.65 ? 0.55 : 0.2)
                : 1.0;
        double windImpulse = Math.max(0.0, conditions.windStrength() - 0.85) * 0.025;
        double stormGust = conditions.stormIntensity() * 0.045;
        double horizontalImpulse = Math.max(Math.max(windImpulse, stormGust), tornado * 0.14) * playerFactor;
        double verticalImpulse = Math.max(conditions.verticalLift() * 0.003, tornado * 0.22) * playerFactor;
        entity.method_5762(
                conditions.windX() * horizontalImpulse,
                verticalImpulse,
                conditions.windZ() * horizontalImpulse);
        entity.field_6037 = true;
    }

    private static void douseExposedFires(class_3218 level, class_2338 center) {
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dy = -2; dy <= 2; ++dy) {
                for (int dz = -2; dz <= 2; ++dz) {
                    class_2338 pos = center.method_10069(dx, dy, dz);
                    if (center.method_10262((net.minecraft.class_2382)pos) > 6.0 || !level.method_8311(pos)) {
                        continue;
                    }
                    class_2680 state = level.method_8320(pos);
                    if (state.method_27852(class_2246.field_10036) || state.method_27852(class_2246.field_22089)) {
                        level.method_8650(pos, false);
                    }
                }
            }
        }
    }

    private static void prunePulses(class_3218 level, long now) {
        List<WindPulse> pulses = WIND_PULSES.get(level);
        if (pulses == null) {
            return;
        }
        pulses.removeIf(pulse -> now >= pulse.endTick());
        if (pulses.isEmpty()) {
            WIND_PULSES.remove(level);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }

    public record Conditions(
            double windX,
            double windZ,
            double windStrength,
            double verticalLift,
            double humidity,
            double precipitationIntensity,
            double temperature,
            double stormIntensity,
            double tornadoIntensity,
            boolean snowing,
            boolean hailing,
            boolean thunderstorm) {
    }

    private record PulseInfluence(double x, double z, double verticalLift) {
        private static final PulseInfluence NONE = new PulseInfluence(0.0, 0.0, 0.0);
    }

    private record WindPulse(
            double x,
            double z,
            double directionX,
            double directionZ,
            double horizontalStrength,
            double verticalStrength,
            double radius,
            long startTick,
            long endTick) {
    }

    private record WeatherCell(int chunkX, int chunkZ) {
        private static WeatherCell from(class_243 position) {
            int x = (int)Math.floor(position.method_10216());
            int z = (int)Math.floor(position.method_10215());
            return new WeatherCell(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        }
    }
}
