package com.mushokumagic.weather.client;

import com.mushokumagic.weather.world.ClientStormWeather;
import com.mushokumagic.weather.world.LocalWeatherNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Registers the clientbound local-storm snapshot receiver. */
public final class ClientLocalWeatherNetwork {
    private ClientLocalWeatherNetwork() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                LocalWeatherNetwork.StormSnapshotMessage.TYPE,
                (message, context) -> context.client().execute(
                        () -> ClientStormWeather.applySnapshot(
                                message.dimension(),
                                message.localStorms(),
                                message.severeStorms(),
                                message.regional())));
    }
}
