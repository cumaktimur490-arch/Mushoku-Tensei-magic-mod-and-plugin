package com.mushokumagic.client;

import com.mushokumagic.world.ClientStormWeather;
import com.mushokumagic.world.VolumetricCloudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_638;

@Environment(EnvType.CLIENT)
public final class MushokuWeatherClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLocalWeatherNetwork.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientStormWeather.tick());
        WorldRenderEvents.LAST.register(context -> {
            class_310 client = class_310.method_1551();
            class_638 world = client.field_1687;
            if (world == null) {
                return;
            }
            class_243 camera = context.camera().method_19326();
            VolumetricCloudRenderer.render(
                    world.method_27983().toString(),
                    camera.method_10216(),
                    camera.method_10214(),
                    camera.method_10215(),
                    world.method_75260(),
                    context.tickDelta());
        });
    }
}
