package com.mushokumagic.client;

import com.mushokumagic.weather.MushokuWeather;
import com.mushokumagic.world.ClientStormWeather;
import net.minecraft.class_310;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only renderer hook for weather snapshots received from the server. */
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
}
