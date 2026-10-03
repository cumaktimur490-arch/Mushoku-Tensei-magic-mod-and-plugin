package com.mushokumagic.weather.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mushokumagic.weather.config.WeatherConfig;
import com.mushokumagic.weather.world.RegionalWeatherManager;
import com.mushokumagic.weather.world.RegionalWeatherModel;
import com.mushokumagic.weather.world.SevereWeatherManager;
import com.mushokumagic.weather.world.SevereWeatherModel;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.class_12099;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_3218;
import net.minecraft.class_3222;

/** Operator controls for the standalone local-weather simulation. */
public final class WeatherCommands {
    private static final double COMMAND_RADIUS = 160.0;
    private static final int DEFAULT_DURATION_SECONDS = 120;

    private WeatherCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register((CommandDispatcher<class_2168>)dispatcher));
    }

    public static void register(CommandDispatcher<class_2168> dispatcher) {
        LiteralArgumentBuilder<class_2168> command = class_2170.method_9247("magicweather")
                .requires(WeatherCommands::isAdmin)
                .executes(WeatherCommands::weatherHelp);
        command.then(class_2170.method_9247("status").executes(WeatherCommands::weatherStatus));
        command.then(class_2170.method_9247("reload").executes(WeatherCommands::reload));

        RequiredArgumentBuilder<class_2168, String> preset = class_2170.method_9244("preset", StringArgumentType.word());
        preset.suggests(WeatherCommands::suggestWeather);
        preset.executes(context -> WeatherCommands.setWeather(context, DEFAULT_DURATION_SECONDS));
        preset.then(class_2170.method_9244("seconds", IntegerArgumentType.integer(10, 3600))
                .executes(WeatherCommands::setWeatherWithDuration));
        command.then(preset);
        dispatcher.register(command);
    }

    private static boolean isAdmin(class_2168 source) {
        return source.method_75037().hasPermission(class_12099.field_63210);
    }

    private static int weatherHelp(CommandContext<class_2168> context) {
        context.getSource().method_9226(
                () -> text("Погода локальная; vanilla /weather не переключает весь мир, когда региональная система включена."),
                false);
        context.getSource().method_9226(
                () -> text("Использование: /magicweather <clear|cloudy|rain|thunder|snow|hail|squall|supercell|tornado|cyclone|sandstorm> [секунды], /magicweather status или /magicweather reload."),
                false);
        return 1;
    }

    private static int weatherStatus(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_2168 source = context.getSource();
        if (!WeatherConfig.get().regionalWeatherEnabled) {
            source.method_9226(() -> text("Региональная погода отключена; используется ванильная погода мира."), false);
            return 1;
        }
        class_3222 player = source.method_9207();
        if (!(player.method_51469() instanceof class_3218 level)) {
            source.method_9213(text("Локальную погоду можно проверить только в игровом мире."));
            return 0;
        }
        class_243 position = player.method_73189();
        RegionalWeatherManager.ManualWeatherStatus status = RegionalWeatherManager.manualWeatherStatusAt(
                level, position.method_10216(), position.method_10215());
        if (status == null) {
            source.method_9226(
                    () -> text("Здесь нет активного ручного приказа; условия задаются региональной симуляцией."),
                    false);
            return 1;
        }
        int seconds = (status.remainingTicks() + 19) / 20;
        source.method_9226(
                () -> text("Локальная погода: " + status.description() + ", ещё примерно " + seconds + " сек."),
                false);
        return 1;
    }

    private static int reload(CommandContext<class_2168> context) {
        WeatherConfig.reload();
        context.getSource().method_9226(
                () -> text("Настройки Mushoku Weather перечитаны из config/mushoku_weather.json."),
                true);
        return 1;
    }

    private static int setWeatherWithDuration(CommandContext<class_2168> context) throws CommandSyntaxException {
        return setWeather(context, IntegerArgumentType.getInteger(context, "seconds"));
    }

    private static int setWeather(CommandContext<class_2168> context, int seconds) throws CommandSyntaxException {
        class_2168 source = context.getSource();
        WeatherConfig config = WeatherConfig.get();
        if (!config.regionalWeatherEnabled) {
            source.method_9213(text(
                    "Региональная погода отключена. Включите regionalWeatherEnabled в config/mushoku_weather.json и выполните /magicweather reload."));
            return 0;
        }

        String requested = StringArgumentType.getString(context, "preset").toLowerCase(Locale.ROOT);
        RegionalWeatherModel.ManualPreset preset;
        SevereWeatherModel.Kind hazard = null;
        String displayName;
        switch (requested) {
            case "clear", "sunny" -> {
                preset = RegionalWeatherModel.ManualPreset.CLEAR;
                displayName = "ясно";
            }
            case "cloudy" -> {
                preset = RegionalWeatherModel.ManualPreset.CLOUDY;
                displayName = "облачно";
            }
            case "rain" -> {
                preset = RegionalWeatherModel.ManualPreset.RAIN;
                displayName = "дождь";
            }
            case "thunder", "storm" -> {
                preset = RegionalWeatherModel.ManualPreset.THUNDER;
                displayName = "гроза";
            }
            case "snow" -> {
                preset = RegionalWeatherModel.ManualPreset.SNOW;
                displayName = "снег";
            }
            case "hail" -> {
                preset = RegionalWeatherModel.ManualPreset.THUNDER;
                hazard = SevereWeatherModel.Kind.HAIL;
                displayName = "град";
            }
            case "squall" -> {
                preset = RegionalWeatherModel.ManualPreset.RAIN;
                hazard = SevereWeatherModel.Kind.SQUALL;
                displayName = "шквал";
            }
            case "supercell" -> {
                preset = RegionalWeatherModel.ManualPreset.THUNDER;
                hazard = SevereWeatherModel.Kind.SUPERCELL;
                displayName = "суперячейка";
            }
            case "tornado" -> {
                preset = RegionalWeatherModel.ManualPreset.THUNDER;
                hazard = SevereWeatherModel.Kind.TORNADO;
                displayName = "торнадо";
            }
            case "cyclone", "hurricane" -> {
                preset = RegionalWeatherModel.ManualPreset.THUNDER;
                hazard = SevereWeatherModel.Kind.HURRICANE;
                displayName = "циклон";
            }
            case "sandstorm" -> {
                preset = RegionalWeatherModel.ManualPreset.CLOUDY;
                hazard = SevereWeatherModel.Kind.SANDSTORM;
                displayName = "песчаная буря";
            }
            default -> {
                source.method_9213(text("Неизвестный тип. Введите /magicweather для списка вариантов."));
                return 0;
            }
        }
        if (hazard != null && !config.severeWeatherEnabled) {
            source.method_9213(text("Опасные погодные системы отключены (severeWeatherEnabled=false)."));
            return 0;
        }

        class_3222 player = source.method_9207();
        if (!(player.method_51469() instanceof class_3218 level)) {
            source.method_9213(text("Локальную погоду можно задать только в игровом мире."));
            return 0;
        }
        class_243 position = player.method_73189();
        double x = position.method_10216();
        double z = position.method_10215();
        int durationTicks = seconds * 20;
        RegionalWeatherManager.setManualWeather(
                level, x, z, preset, displayName, COMMAND_RADIUS, durationTicks);
        if (hazard != null && !SevereWeatherManager.startManual(level, position, hazard, durationTicks)) {
            source.method_9213(text("Не удалось запустить локальную погодную систему."));
            return 0;
        }
        if (preset == RegionalWeatherModel.ManualPreset.CLEAR) {
            SevereWeatherManager.clearNear(level, x, z, COMMAND_RADIUS);
        }
        source.method_9226(
                () -> text("Установлена локальная погода «" + displayName + "» в радиусе 160 блоков на " + seconds + " сек."),
                true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestWeather(
            CommandContext<class_2168> context,
            SuggestionsBuilder builder) {
        for (String preset : List.of("clear", "cloudy", "rain", "thunder", "snow", "hail", "squall", "supercell", "tornado", "cyclone", "sandstorm")) {
            builder.suggest(preset);
        }
        return builder.buildFuture();
    }

    private static class_2561 text(String message) {
        return class_2561.method_43470(message);
    }
}
