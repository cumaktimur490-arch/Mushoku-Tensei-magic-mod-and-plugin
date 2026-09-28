package com.mushokumagic.event;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.item.WandItem;
import com.mushokumagic.mana.ManaData;
import com.mushokumagic.mana.ManaManager;
import com.mushokumagic.spell.CastManager;
import com.mushokumagic.util.Msg;
import com.mushokumagic.world.LocalStormManager;
import com.mushokumagic.world.MagicHitTracker;
import com.mushokumagic.world.RegionalWeatherManager;
import com.mushokumagic.world.SevereWeatherManager;
import com.mushokumagic.world.TemporaryBlocks;
import com.mushokumagic.world.WeatherPhysics;
import java.util.Locale;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1282;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

/** Shared gameplay callbacks invoked by the Forge-style event bridge on 1.20.1. */
public final class MagicEvents {
    private MagicEvents() {
    }

    public static void onJoin(class_3222 player) {
        ManaData data = ManaData.of(player);
        if (data.getMana() <= 0.0 && !data.isOverloaded()) {
            ManaManager.refill(player);
        }
        if (!MagicConfig.get().announceOnJoin) {
            return;
        }
        MagicConfig.RankDef rank = ManaManager.rankOf(data);
        Msg.chat(player, (class_2561) Msg.t("mushoku_magic.msg.join", Msg.rankName(rank.id),
                String.format(Locale.ROOT, "x%.2f", rank.multiplier)));
        if (data.isOverloaded()) {
            Msg.chat(player, (class_2561) Msg.t("mushoku_magic.msg.overload_active", data.getOverloadTicks() / 20));
        }
    }

    public static void onLogout(class_3222 player) {
        CastManager.cancel(player);
    }

    public static void onRespawn(class_3222 oldPlayer, class_3222 newPlayer) {
        ManaData.copyTo(oldPlayer, newPlayer);
        CastManager.cancel(newPlayer);
    }

    public static boolean onChat(class_3222 player, String message) {
        return CastManager.handleChat(player, message);
    }

    public static class_1269 onUseItem(class_1657 player, class_1937 world, class_1268 hand) {
        if (!(player instanceof class_3222 serverPlayer)) {
            return class_1269.field_5811;
        }
        class_1799 stack = player.method_5998(hand);
        if (!(stack.method_7909() instanceof WandItem)
                || !MagicConfig.get().wandRightClickCasts
                || player.method_7357().method_7904(stack.method_7909())
                || !CastManager.castLast(serverPlayer)) {
            return class_1269.field_5811;
        }
        int cooldownTicks = (int) Math.round(MagicConfig.get().wandRightClickCooldownSeconds * 20.0);
        player.method_7357().method_7906(stack.method_7909(), Math.max(0, cooldownTicks));
        return class_1269.field_5812;
    }

    public static void onServerTick(MinecraftServer server) {
        for (class_3218 level : server.method_3738()) {
            TemporaryBlocks.tick(level);
        }
        RegionalWeatherManager.tick(server);
        LocalStormManager.tick(server);
        SevereWeatherManager.tick(server);
        WeatherPhysics.tick(server);
        for (class_3222 player : server.method_3760().method_14571()) {
            ManaManager.tickPlayer(player);
        }
        CastManager.tick(server);
        long gameTime = server.method_30002().method_8510();
        if (gameTime % 20L == 0L) {
            MagicHitTracker.prune(gameTime);
        }
    }

    public static void onDeath(class_1309 entity, class_1282 source) {
        class_1937 level = entity.method_37908();
        if (!(level instanceof class_3218 serverLevel) || entity instanceof class_3222) {
            return;
        }
        class_3222 killer = resolveMagicKiller(source);
        if (killer == null) {
            return;
        }
        MagicHitTracker.Hit hit = MagicHitTracker.get(entity);
        if (hit == null || hit.player() == null || !hit.player().equals(killer.method_5667())) {
            return;
        }
        if (serverLevel.method_8510() - hit.tick() > 200L) {
            return;
        }
        MagicHitTracker.clear(entity);
        ManaManager.awardSpellKill(killer, entity);
    }

    public static void onServerStopping(MinecraftServer server) {
        TemporaryBlocks.restoreAll(server);
        RegionalWeatherManager.clear();
        LocalStormManager.clear();
        SevereWeatherManager.clear();
        WeatherPhysics.clear();
    }

    private static class_3222 resolveMagicKiller(class_1282 source) {
        class_1297 direct = source.method_5526();
        class_1297 causing = source.method_5529();
        if (direct instanceof class_3222 player) {
            return player;
        }
        if (causing instanceof class_3222 player) {
            return player;
        }
        return null;
    }
}
