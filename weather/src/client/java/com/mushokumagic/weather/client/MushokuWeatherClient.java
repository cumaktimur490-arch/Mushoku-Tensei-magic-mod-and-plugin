package com.mushokumagic.weather.client;

import com.mushokumagic.weather.world.ClientStormWeather;
import com.mushokumagic.weather.world.VolumetricCloudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_638;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class MushokuWeatherClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLocalWeatherNetwork.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientStormWeather.tick());
        WorldRenderEvents.END_MAIN.register(context -> {
            class_310 client = class_310.method_1551();
            class_638 world = client.field_1687;
            if (world == null) {
                return;
            }

            var cameraState = context.worldState().field_63082;
            if (!cameraState.field_63079) {
                return;
            }
            class_243 camera = cameraState.field_63078;
            float partialTick = client.method_61966().method_60637(false);
            int fovDegrees = (Integer)client.field_1690.method_41808().method_41753();
            Matrix4f projectionMatrix = context.gameRenderer().method_22973((float)fovDegrees);

            VolumetricCloudRenderer.render(
                    world.method_27983().toString(),
                    camera.method_10216(),
                    camera.method_10214(),
                    camera.method_10215(),
                    world.method_75260(),
                    partialTick,
                    projectionMatrix);
        });
    }
}
