package com.mushokumagic.weather.client;

import com.mushokumagic.weather.MushokuWeather;
import com.mushokumagic.weather.world.ClientStormWeather;
import com.mushokumagic.weather.world.VolumetricCloudRenderer;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client weather presentation and volumetric GPU cloud rendering for Forge-family loaders. */
@Mod.EventBusSubscriber(modid = MushokuWeather.MOD_ID, value = Dist.CLIENT)
public final class MushokuWeatherClient {
    private MushokuWeatherClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientStormWeather.tick();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        class_310 client = class_310.method_1551();
        class_638 world = client.field_1687;
        if (world == null) {
            return;
        }
        class_243 camera = event.getCamera().method_19326();
        VolumetricCloudRenderer.render(
                world.method_27983().toString(),
                camera.method_10216(),
                camera.method_10214(),
                camera.method_10215(),
                world.method_8510(),
                event.getPartialTick());
    }
}
