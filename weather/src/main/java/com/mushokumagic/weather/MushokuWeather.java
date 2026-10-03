package com.mushokumagic.weather;

import com.mushokumagic.weather.command.WeatherCommands;
import com.mushokumagic.weather.config.WeatherConfig;
import com.mushokumagic.weather.world.LocalWeatherNetwork;
import com.mushokumagic.weather.world.WeatherEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.class_2960;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fabric entrypoint for the standalone weather mod. */
public final class MushokuWeather implements ModInitializer {
    public static final String MOD_ID = "mushoku_weather";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static class_2960 id(String path) {
        return class_2960.method_60655(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        WeatherConfig.load();
        LocalWeatherNetwork.register();
        WeatherEvents.init();
        WeatherCommands.init();
        LOGGER.info("Mushoku Tensei: Weather initialized independently of the magic mod.");
    }
}
