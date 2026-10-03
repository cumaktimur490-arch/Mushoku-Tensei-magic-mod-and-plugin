package com.mushokumagic.weather;

import com.mushokumagic.command.WeatherCommands;
import com.mushokumagic.config.WeatherConfig;
import com.mushokumagic.world.LocalWeatherNetwork;
import com.mushokumagic.world.WeatherEventHandlers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Native Forge/NeoForge 1.20.1 entrypoint for the standalone weather mod. */
@Mod(MushokuWeather.MOD_ID)
public final class MushokuWeather {
    public static final String MOD_ID = "mushoku_weather";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public MushokuWeather() {
        MinecraftForge.EVENT_BUS.register(new WeatherEventHandlers());
        MinecraftForge.EVENT_BUS.register(this);
        WeatherConfig.load();
        LocalWeatherNetwork.register();
        LOGGER.info("Mushoku Tensei: Weather initialized independently of the magic mod.");
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        WeatherCommands.register(event.getDispatcher());
    }
}
