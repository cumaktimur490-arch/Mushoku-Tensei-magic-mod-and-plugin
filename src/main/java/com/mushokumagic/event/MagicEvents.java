/*
 * Decompiled with CFR.
 */
package com.mushokumagic.event;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.item.WandItem;
import com.mushokumagic.mana.ManaData;
import com.mushokumagic.mana.ManaManager;
import com.mushokumagic.spell.CastManager;
import com.mushokumagic.util.Msg;
import com.mushokumagic.world.LocalStormManager;
import com.mushokumagic.world.MagicHitTracker;
import com.mushokumagic.world.TemporaryBlocks;
import java.util.Locale;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
import net.minecraft.class_8111;
import net.minecraft.server.MinecraftServer;

public final class MagicEvents {
    private MagicEvents() {
    }

    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> !CastManager.handleChat(sender, message.method_44862()));
        ServerLivingEntityEvents.AFTER_DEATH.register(MagicEvents::onDeath);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> MagicEvents.onJoin(handler.method_32311()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CastManager.cancel(handler.method_32311()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> CastManager.cancel(newPlayer));
        UseItemCallback.EVENT.register(MagicEvents::onUseItem);
        ServerTickEvents.END_SERVER_TICK.register(MagicEvents::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            TemporaryBlocks.restoreAll(server);
            LocalStormManager.clear();
        });
    }

    private static void onJoin(class_3222 player) {
        ManaData data = ManaData.of((class_1309)player);
        if (data.getMana() <= 0.0 && !data.isOverloaded()) {
            ManaManager.refill(player);
        }
        if (!MagicConfig.get().announceOnJoin) {
            return;
        }
        MagicConfig.RankDef rank = ManaManager.rankOf(data);
        Msg.chat(player, (class_2561)Msg.t("mushoku_magic.msg.join", Msg.rankName(rank.id), String.format((Locale)Locale.ROOT, (String)"x%.2f", (Object[])new Object[]{rank.multiplier})));
        if (data.isOverloaded()) {
            Msg.chat(player, (class_2561)Msg.t("mushoku_magic.msg.overload_active", data.getOverloadTicks() / 20));
        }
    }

    private static class_1269 onUseItem(class_1657 player, class_1937 world, class_1268 hand) {
        if (!(player instanceof class_3222)) {
            return class_1269.field_5811;
        }
        class_3222 serverPlayer = (class_3222)player;
        class_1799 stack = player.method_5998(hand);
        if (!(stack.method_7909() instanceof WandItem)) {
            return class_1269.field_5811;
        }
        if (!MagicConfig.get().wandRightClickCasts) {
            return class_1269.field_5811;
        }
        if (player.method_7357().method_7904(stack)) {
            return class_1269.field_5811;
        }
        if (!CastManager.castLast(serverPlayer)) {
            return class_1269.field_5811;
        }
        int cooldownTicks = (int)Math.round((double)(MagicConfig.get().wandRightClickCooldownSeconds * 20.0));
        player.method_7357().method_62835(stack, Math.max((int)0, (int)cooldownTicks));
        return class_1269.field_5812;
    }

    private static void onServerTick(MinecraftServer server) {
        for (class_3218 level : server.method_3738()) {
            TemporaryBlocks.tick(level);
        }
        LocalStormManager.tick(server);
        for (class_3222 player : server.method_3760().method_14571()) {
            ManaManager.tickPlayer(player);
        }
        CastManager.tick(server);
        long gameTime = server.method_30002().method_75260();
        if (gameTime % 20L == 0L) {
            MagicHitTracker.prune(gameTime);
        }
    }

    private static void onDeath(class_1309 entity, class_1282 source) {
        class_1937 class_19372 = entity.method_73183();
        if (!(class_19372 instanceof class_3218)) {
            return;
        }
        class_3218 level = (class_3218)class_19372;
        if (entity instanceof class_3222) {
            return;
        }
        class_3222 killer = MagicEvents.resolveMagicKiller(source);
        if (killer == null) {
            return;
        }
        MagicHitTracker.Hit hit = MagicHitTracker.get(entity);
        if (hit == null || hit.player() == null || !hit.player().equals(killer.method_5667())) {
            return;
        }
        if (level.method_75260() - hit.tick() > 200L) {
            return;
        }
        MagicHitTracker.clear(entity);
        ManaManager.awardSpellKill(killer, entity);
    }

    private static class_3222 resolveMagicKiller(class_1282 source) {
        class_1297 direct = source.method_5526();
        class_1297 cause = source.method_5529();
        class_3222 player = null;
        if (direct instanceof class_3222) {
            class_3222 directPlayer;
            player = directPlayer = (class_3222)direct;
        } else if (cause instanceof class_3222) {
            class_3222 causePlayer;
            player = causePlayer = (class_3222)cause;
        }
        if (player == null) {
            return null;
        }
        boolean magical = source.method_49708(class_8111.field_42349) || source.method_49708(class_8111.field_42329) || source.method_49708(class_8111.field_42331) || source.method_49708(class_8111.field_42332);
        return magical ? player : null;
    }
}
