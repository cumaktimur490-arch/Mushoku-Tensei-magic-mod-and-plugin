package com.mushokumagic.weather.api;

import com.mushokumagic.weather.world.LocalStormManager;
import com.mushokumagic.weather.world.WeatherPhysics;
import net.minecraft.class_1309;
import net.minecraft.class_243;
import net.minecraft.class_3218;

/** Stable integration surface for mods that consume local weather conditions. */
public final class WeatherApi {
    private WeatherApi() {
    }

    public static void startCumulonimbus(class_3218 level, class_243 center, int durationTicks) {
        LocalStormManager.start(level, center, durationTicks);
    }

    public static class_243 driftProjectileAimPoint(
            class_3218 level,
            class_243 origin,
            class_243 target,
            String spellId,
            double maxRange) {
        return WeatherPhysics.driftProjectileAimPoint(level, origin, target, spellId, maxRange);
    }

    public static double elementalPowerMultiplier(
            class_3218 level,
            class_243 position,
            String element,
            class_1309 target) {
        return WeatherPhysics.elementalPowerMultiplier(level, position, element, target);
    }

    public static double fireDurationMultiplier(class_3218 level, class_243 position, class_1309 target) {
        return WeatherPhysics.fireDurationMultiplier(level, position, target);
    }

    public static double ignitionMultiplier(class_3218 level, class_243 position) {
        return WeatherPhysics.ignitionMultiplier(level, position);
    }

    public static void addGust(
            class_3218 level,
            class_243 center,
            double directionX,
            double directionZ,
            double strength,
            double radius,
            int durationTicks) {
        WeatherPhysics.addGust(level, center, directionX, directionZ, strength, radius, durationTicks);
    }

    public static void addUpdraft(
            class_3218 level,
            class_243 center,
            double strength,
            double radius,
            int durationTicks) {
        WeatherPhysics.addUpdraft(level, center, strength, radius, durationTicks);
    }
}
