/*
 * Decompiled with CFR.
 */
package com.mushokumagic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.item.MagicItems;
import com.mushokumagic.mana.ManaData;
import com.mushokumagic.mana.ManaManager;
import com.mushokumagic.spell.CastManager;
import com.mushokumagic.spell.Spell;
import com.mushokumagic.spell.SpellRegistry;
import com.mushokumagic.util.Msg;
import com.mushokumagic.world.RegionalWeatherManager;
import com.mushokumagic.world.RegionalWeatherModel;
import com.mushokumagic.world.SevereWeatherManager;
import com.mushokumagic.world.SevereWeatherModel;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_243;
import net.minecraft.class_2186;
import net.minecraft.class_2561;
import net.minecraft.class_3218;
import net.minecraft.class_3222;

public final class MagicCommands {
    private MagicCommands() {
    }

    private static boolean isAdmin(class_2168 source) {
        return source.method_9259(2);
    }

    public static void register(CommandDispatcher<class_2168> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247((String)"spells").executes(MagicCommands::listLearned)).then(((LiteralArgumentBuilder)class_2170.method_9247((String)"all").requires(MagicCommands::isAdmin)).executes(MagicCommands::listAll)));
        dispatcher.register((LiteralArgumentBuilder)class_2170.method_9247((String)"magiclevel").executes(MagicCommands::magicLevel));
        dispatcher.register((LiteralArgumentBuilder)class_2170.method_9247((String)"mana").executes(MagicCommands::manaStatus));
        LiteralArgumentBuilder<class_2168> weatherCommand = class_2170.method_9247("magicweather");
        weatherCommand.requires(MagicCommands::isAdmin);
        weatherCommand.executes(MagicCommands::weatherHelp);
        weatherCommand.then(class_2170.method_9247("status").executes(MagicCommands::weatherStatus));
        RequiredArgumentBuilder<class_2168, String> weatherPreset = class_2170.method_9244(
                "preset",
                StringArgumentType.word());
        weatherPreset.suggests(MagicCommands::suggestWeather);
        weatherPreset.executes(context -> MagicCommands.setWeather((CommandContext<class_2168>)context, 120));
        weatherPreset.then(class_2170.method_9244(
                "seconds",
                IntegerArgumentType.integer(10, 3600))
                .executes(MagicCommands::setWeatherWithDuration));
        weatherCommand.then(weatherPreset);
        dispatcher.register(weatherCommand);
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247((String)"cast").executes(MagicCommands::castLast)).then(class_2170.method_9247((String)"last").executes(MagicCommands::castLast))).then(class_2170.method_9244((String)"spell", (ArgumentType)StringArgumentType.word()).suggests(MagicCommands::suggestSpells).executes(MagicCommands::castNamed)));
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247((String)"wand").requires(MagicCommands::isAdmin)).executes(context -> MagicCommands.giveWand((CommandContext<class_2168>)context, 1, null))).then(((RequiredArgumentBuilder)class_2170.method_9244((String)"tier", (ArgumentType)IntegerArgumentType.integer((int)1, (int)3)).executes(context -> MagicCommands.giveWand((CommandContext<class_2168>)context, IntegerArgumentType.getInteger((CommandContext)context, (String)"tier"), null))).then(class_2170.method_9244((String)"player", (ArgumentType)class_2186.method_9305()).executes(context -> MagicCommands.giveWand((CommandContext<class_2168>)context, IntegerArgumentType.getInteger((CommandContext)context, (String)"tier"), class_2186.method_9315((CommandContext)context, (String)"player"))))));
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247((String)"magicadmin").requires(MagicCommands::isAdmin)).then(class_2170.method_9247((String)"reload").executes(MagicCommands::reloadConfig))).then(class_2170.method_9247((String)"reset").then(class_2170.method_9244((String)"player", (ArgumentType)class_2186.method_9305()).executes(MagicCommands::reset)))).then(class_2170.method_9247((String)"setmax").then(class_2170.method_9244((String)"player", (ArgumentType)class_2186.method_9305()).then(class_2170.method_9244((String)"value", (ArgumentType)IntegerArgumentType.integer((int)1, (int)100000)).executes(MagicCommands::setMax))))).then(class_2170.method_9247((String)"addxp").then(class_2170.method_9244((String)"player", (ArgumentType)class_2186.method_9305()).then(class_2170.method_9244((String)"value", (ArgumentType)IntegerArgumentType.integer((int)0, (int)100000)).executes(MagicCommands::addXp))))).then(class_2170.method_9247((String)"learn").then(class_2170.method_9244((String)"player", (ArgumentType)class_2186.method_9305()).then(class_2170.method_9244((String)"spell", (ArgumentType)StringArgumentType.word()).suggests(MagicCommands::suggestSpells).executes(MagicCommands::learnSpell))))).then(class_2170.method_9247((String)"clearoverload").then(class_2170.method_9244((String)"player", (ArgumentType)class_2186.method_9305()).executes(MagicCommands::clearOverload))));
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247((String)"admin").requires(MagicCommands::isAdmin)).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247((String)"magic").then(((LiteralArgumentBuilder)class_2170.method_9247((String)"all").executes(context -> MagicCommands.setAdminMode((CommandContext<class_2168>)context, null, true))).then(class_2170.method_9244((String)"player", (ArgumentType)class_2186.method_9305()).executes(context -> MagicCommands.setAdminMode((CommandContext<class_2168>)context, class_2186.method_9315((CommandContext)context, (String)"player"), true))))).then(((LiteralArgumentBuilder)class_2170.method_9247((String)"off").executes(context -> MagicCommands.setAdminMode((CommandContext<class_2168>)context, null, false))).then(class_2170.method_9244((String)"player", (ArgumentType)class_2186.method_9305()).executes(context -> MagicCommands.setAdminMode((CommandContext<class_2168>)context, class_2186.method_9315((CommandContext)context, (String)"player"), false))))).then(((LiteralArgumentBuilder)class_2170.method_9247((String)"status").executes(context -> MagicCommands.adminStatus((CommandContext<class_2168>)context, null))).then(class_2170.method_9244((String)"player", (ArgumentType)class_2186.method_9305()).executes(context -> MagicCommands.adminStatus((CommandContext<class_2168>)context, class_2186.method_9315((CommandContext)context, (String)"player")))))));
    }

    private static int listLearned(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 player = ((class_2168)context.getSource()).method_9207();
        return MagicCommands.showSpells((class_2168)context.getSource(), player, false);
    }

    private static int listAll(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 player = ((class_2168)context.getSource()).method_9207();
        return MagicCommands.showSpells((class_2168)context.getSource(), player, true);
    }

    private static int showSpells(class_2168 source, class_3222 player, boolean all) {
        ManaData data = ManaData.of((class_1309)player);
        int shown = 0;
        String headerKey = all ? "mushoku_magic.msg.spells_all_header" : (data.isAdminMode() ? "mushoku_magic.msg.spells_admin_header" : "mushoku_magic.msg.spells_header");
        source.method_9226(() -> Msg.t(headerKey, new Object[0]), false);
        for (Spell spell : SpellRegistry.spells()) {
            if (!all && !data.hasLearned(spell.id())) continue;
            String phrase = spell.phrases().isEmpty() ? spell.id() : (String)spell.phrases().get(0);
            boolean learned = data.hasLearned(spell.id());
            int attempts = data.getAttempts(spell.id());
            String status = learned ? "\u2713" : Msg.t("mushoku_magic.msg.spells_attempts", attempts, MagicConfig.get().guaranteedAfterAttempts).getString();
            String line = "- \u00ab" + phrase + "\u00bb \u2014 " + (int)spell.cost() + " " + Msg.t("mushoku_magic.unit.mana", new Object[0]).getString() + ", " + String.format((Locale)Locale.ROOT, (String)"%.1f", (Object[])new Object[]{spell.castSeconds()}) + " " + Msg.t("mushoku_magic.unit.seconds", new Object[0]).getString() + ", " + status;
            source.method_9226(() -> class_2561.method_43470((String)line), false);
            ++shown;
        }
        if (shown == 0) {
            source.method_9226(() -> Msg.t("mushoku_magic.msg.spells_empty", new Object[0]), false);
        }
        return shown;
    }

    private static int magicLevel(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 player = ((class_2168)context.getSource()).method_9207();
        ManaData data = ManaData.of((class_1309)player);
        MagicConfig.RankDef rank = ManaManager.rankOf(data);
        double threshold = ManaManager.xpThreshold(data);
        int progress = (int)Math.min((long)100L, (long)Math.round((double)(data.getXp() / threshold * 100.0)));
        double nextBonus = MagicCommands.nextRankMultiplier(rank);
        ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.magiclevel", Msg.rankName(rank.id), String.format((Locale)Locale.ROOT, (String)"x%.2f", (Object[])new Object[]{rank.multiplier}), progress), false);
        if (nextBonus > 0.0) {
            ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.magiclevel_next", String.format((Locale)Locale.ROOT, (String)"x%.2f", (Object[])new Object[]{nextBonus})), false);
        }
        return 1;
    }

    private static double nextRankMultiplier(MagicConfig.RankDef current) {
        MagicConfig config = MagicConfig.get();
        for (int i = 0; i < config.ranks.size(); ++i) {
            if (!((MagicConfig.RankDef)config.ranks.get((int)i)).id.equals(current.id) || i + 1 >= config.ranks.size()) continue;
            return ((MagicConfig.RankDef)config.ranks.get((int)(i + 1))).multiplier;
        }
        return 0.0;
    }

    private static int weatherHelp(CommandContext<class_2168> context) {
        ((class_2168)context.getSource()).method_9226(
                () -> Msg.literal("Погода в этом моде локальная; vanilla /weather при включённой региональной погоде не управляет локальными фронтами."),
                false);
        ((class_2168)context.getSource()).method_9226(
                () -> Msg.literal("Используйте: /magicweather <clear|cloudy|rain|thunder|snow|hail|tornado|cyclone|sandstorm> [секунды] (по умолчанию 120)."),
                false);
        return 1;
    }

    private static int weatherStatus(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_2168 source = (class_2168)context.getSource();
        if (!MagicConfig.get().regionalWeatherEnabled) {
            source.method_9226(
                    () -> Msg.literal("Региональная погода отключена; используется ванильная погода мира."),
                    false);
            return 1;
        }
        class_3222 player = source.method_9207();
        if (!(player.method_51469() instanceof class_3218 level)) {
            source.method_9213((class_2561)Msg.literal("Локальную погоду можно проверить только в игровом мире."));
            return 0;
        }
        class_243 position = player.method_19538();
        RegionalWeatherManager.ManualWeatherStatus status = RegionalWeatherManager.manualWeatherStatusAt(
                level,
                position.method_10216(),
                position.method_10215());
        if (status == null) {
            source.method_9226(
                    () -> Msg.literal("В этой точке нет ручного погодного приказа; здесь действует динамическая региональная погода."),
                    false);
            return 1;
        }
        int seconds = (status.remainingTicks() + 19) / 20;
        String weatherName = status.description();
        source.method_9226(
                () -> Msg.literal("Локальная погода: " + weatherName + ", ещё примерно " + seconds + " сек."),
                false);
        return 1;
    }

    private static int setWeatherWithDuration(CommandContext<class_2168> context) throws CommandSyntaxException {
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        return MagicCommands.setWeather(context, seconds);
    }

    private static int setWeather(CommandContext<class_2168> context, int seconds) throws CommandSyntaxException {
        class_2168 source = (class_2168)context.getSource();
        if (!MagicConfig.get().regionalWeatherEnabled) {
            source.method_9213((class_2561)Msg.literal(
                    "Региональная погода отключена. Включите regionalWeatherEnabled в config/mushoku_magic.json, затем выполните /magicadmin reload; иначе используйте vanilla /weather."));
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
                source.method_9213((class_2561)Msg.literal(
                        "Неизвестный тип погоды. Введите /magicweather для списка вариантов."));
                return 0;
            }
        }
        if (hazard != null && !MagicConfig.get().severeWeatherEnabled) {
            source.method_9213((class_2561)Msg.literal(
                    "Опасные погодные системы отключены (severeWeatherEnabled=false). Обычные осадки менять можно."));
            return 0;
        }

        class_3222 player = source.method_9207();
        if (!(player.method_51469() instanceof class_3218 level)) {
            source.method_9213((class_2561)Msg.literal("Локальную погоду можно задать только в игровом мире."));
            return 0;
        }
        class_243 position = player.method_19538();
        double x = position.method_10216();
        double z = position.method_10215();
        double radius = 160.0;
        int durationTicks = seconds * 20;
        RegionalWeatherManager.setManualWeather(level, x, z, preset, displayName, radius, durationTicks);
        if (hazard != null && !SevereWeatherManager.startManual(level, position, hazard, durationTicks)) {
            source.method_9213((class_2561)Msg.literal("Не удалось запустить локальную погодную систему."));
            return 0;
        }
        if (preset == RegionalWeatherModel.ManualPreset.CLEAR) {
            SevereWeatherManager.clearNear(level, x, z, radius);
        }
        source.method_9226(
                () -> Msg.literal("Установлена локальная погода «" + displayName + "» в радиусе 160 блоков на " + seconds + " сек."),
                true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestWeather(
            CommandContext<class_2168> context,
            SuggestionsBuilder builder) {
        for (String preset : List.of("clear", "cloudy", "rain", "thunder", "snow", "hail", "tornado", "cyclone", "sandstorm")) {
            builder.suggest(preset);
        }
        return builder.buildFuture();
    }

    private static int manaStatus(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 player = ((class_2168)context.getSource()).method_9207();
        ManaData data = ManaData.of((class_1309)player);
        if (data.isAdminMode()) {
            ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.mana_admin", (int)Math.round((double)data.getMaxMana())), false);
        } else if (data.isOverloaded()) {
            int seconds = data.getOverloadTicks() / 20;
            ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.mana_overloaded", String.format((Locale)Locale.ROOT, (String)"%d:%02d", (Object[])new Object[]{seconds / 60, seconds % 60})), false);
        } else {
            double percent = data.getMaxMana() <= 0.0 ? 0.0 : data.getMana() / data.getMaxMana() * 100.0;
            ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.mana_stable", ManaManager.hasWand((class_1657)player) ? Msg.t("mushoku_magic.msg.yes", new Object[0]) : Msg.t("mushoku_magic.msg.no", new Object[0]), (int)Math.round((double)percent)), false);
        }
        return 1;
    }

    private static int castLast(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 player = ((class_2168)context.getSource()).method_9207();
        CastManager.castLast(player);
        return 1;
    }

    private static int castNamed(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 player = ((class_2168)context.getSource()).method_9207();
        String id = StringArgumentType.getString(context, (String)"spell");
        Spell spell = SpellRegistry.byId(id);
        if (spell == null) {
            ((class_2168)context.getSource()).method_9213((class_2561)Msg.t("mushoku_magic.msg.unknown_spell", id));
            return 0;
        }
        if (!ManaData.of((class_1309)player).hasLearned(spell.id()) && !MagicCommands.isAdmin((class_2168)context.getSource())) {
            ((class_2168)context.getSource()).method_9213((class_2561)Msg.t("mushoku_magic.msg.not_learned", Msg.spellName(spell.id())));
            return 0;
        }
        CastManager.attempt(player, spell, List.<String>of());
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestSpells(CommandContext<class_2168> context, SuggestionsBuilder builder) {
        for (Spell spell : SpellRegistry.spells()) {
            builder.suggest(spell.id());
        }
        return builder.buildFuture();
    }

    private static int giveWand(CommandContext<class_2168> context, int tier, class_3222 target) throws CommandSyntaxException {
        class_1792 item = MagicItems.wandForTier(tier);
        if (item == null) {
            ((class_2168)context.getSource()).method_9213((class_2561)Msg.t("mushoku_magic.msg.unknown_wand", tier));
            return 0;
        }
        class_3222 receiver = target != null ? target : ((class_2168)context.getSource()).method_9207();
        class_1799 stack = new class_1799((class_1935)item);
        if (!receiver.method_31548().method_7394(stack)) {
            receiver.method_7328(stack, false);
        }
        ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.wand_given", receiver.method_5476(), tier), true);
        return 1;
    }

    private static int reloadConfig(CommandContext<class_2168> context) {
        MagicConfig.reload();
        ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.config_reloaded", SpellRegistry.spells().size()), true);
        return 1;
    }

    private static int reset(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 target = class_2186.method_9315(context, (String)"player");
        ManaManager.reset(target);
        ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.reset", target.method_5476()), true);
        return 1;
    }

    private static int setMax(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 target = class_2186.method_9315(context, (String)"player");
        int value = IntegerArgumentType.getInteger(context, (String)"value");
        ManaData data = ManaData.of((class_1309)target);
        data.setMaxMana(value);
        data.setMana(value);
        ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.setmax", target.method_5476()), true);
        return 1;
    }

    private static int addXp(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 target = class_2186.method_9315(context, (String)"player");
        int value = IntegerArgumentType.getInteger(context, (String)"value");
        ManaManager.refill(target);
        ManaData data = ManaData.of((class_1309)target);
        data.addXp(value);
        ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.addxp", value, target.method_5476()), true);
        return 1;
    }

    private static int learnSpell(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 target = class_2186.method_9315(context, (String)"player");
        String id = StringArgumentType.getString(context, (String)"spell");
        Spell spell = SpellRegistry.byId(id);
        if (spell == null) {
            ((class_2168)context.getSource()).method_9213((class_2561)Msg.t("mushoku_magic.msg.unknown_spell", id));
            return 0;
        }
        ManaData data = ManaData.of((class_1309)target);
        if (!data.hasLearned(spell.id())) {
            data.learn(spell.id());
            ManaManager.onSpellLearned(target, spell.id());
        }
        ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.learn_granted", Msg.spellName(spell.id()), target.method_5476()), true);
        return 1;
    }

    private static int clearOverload(CommandContext<class_2168> context) throws CommandSyntaxException {
        class_3222 target = class_2186.method_9315(context, (String)"player");
        ManaManager.clearOverload(target);
        ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.overload_cleared", target.method_5476()), true);
        return 1;
    }

    private static int setAdminMode(CommandContext<class_2168> context, class_3222 target, boolean enabled) throws CommandSyntaxException {
        class_3222 receiver = target != null ? target : ((class_2168)context.getSource()).method_9207();
        ManaManager.setAdminMode(receiver, enabled);
        int spells = SpellRegistry.spells().size();
        int learned = CastManager.learnedSpells(receiver).size();
        if (enabled) {
            ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.admin_on", spells), true);
        } else {
            ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.admin_off", learned), true);
        }
        class_3222 executor = ((class_2168)context.getSource()).method_44023();
        if (executor == null || !executor.method_5667().equals(receiver.method_5667())) {
            Msg.chat(receiver, (class_2561)Msg.t(enabled ? "mushoku_magic.msg.admin_on_target" : "mushoku_magic.msg.admin_off_target", new Object[0]));
        }
        return 1;
    }

    private static int adminStatus(CommandContext<class_2168> context, class_3222 target) throws CommandSyntaxException {
        class_3222 receiver = target != null ? target : ((class_2168)context.getSource()).method_9207();
        ManaData data = ManaData.of((class_1309)receiver);
        if (data.isAdminMode()) {
            ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.admin_status_on", receiver.method_5476(), SpellRegistry.spells().size()), false);
        } else {
            ((class_2168)context.getSource()).method_9226(() -> Msg.t("mushoku_magic.msg.admin_status_off", receiver.method_5476()), false);
        }
        return 1;
    }
}
