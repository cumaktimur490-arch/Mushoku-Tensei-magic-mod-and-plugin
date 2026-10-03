package com.mushokumagic.weather.world;

import net.minecraft.server.MinecraftServer;

/** Weather lifecycle shared by native Forge and NeoForge 1.20.1. */
public final class WeatherEvents {
    private WeatherEvents() {
    }

    public static void onServerTick(MinecraftServer server) {
        RegionalWeatherManager.tick(server);
        LocalStormManager.tick(server);
        SevereWeatherManager.tick(server);
        WeatherPhysics.tick(server);
        LocalWeatherNetwork.syncServerSnapshots(server);
    }

    public static void clear(MinecraftServer server) {
        RegionalWeatherManager.clear();
        LocalStormManager.clear();
        SevereWeatherManager.clear();
        WeatherPhysics.clear();
    }
}
