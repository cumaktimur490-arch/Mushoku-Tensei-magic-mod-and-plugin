package com.mushokumagic.world;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

/** Owns weather ticks and player synchronization independently of magic callbacks. */
public final class WeatherEvents {
    private WeatherEvents() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            class_3222 player = handler.method_32311();
            LocalStormManager.syncToPlayer(player);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
                (player, origin, destination) -> LocalStormManager.syncToPlayer(player));
        ServerTickEvents.END_SERVER_TICK.register(WeatherEvents::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(WeatherEvents::clear);
    }

    public static void onServerTick(MinecraftServer server) {
        RegionalWeatherManager.tick(server);
        LocalStormManager.tick(server);
        SevereWeatherManager.tick(server);
        WeatherPhysics.tick(server);
    }

    public static void clear(MinecraftServer server) {
        RegionalWeatherManager.clear();
        LocalStormManager.clear();
        SevereWeatherManager.clear();
        WeatherPhysics.clear();
    }
}
