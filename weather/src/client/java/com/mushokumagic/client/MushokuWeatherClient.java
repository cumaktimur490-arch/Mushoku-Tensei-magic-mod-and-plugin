package com.mushokumagic.client;

import com.mushokumagic.world.ClientStormWeather;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public final class MushokuWeatherClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLocalWeatherNetwork.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientStormWeather.tick());
    }
}
