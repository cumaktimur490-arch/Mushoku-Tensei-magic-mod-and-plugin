package com.mushokumagic.platform;

import com.mushokumagic.event.MagicEvents;
import com.mushokumagic.mana.ManaData;
import com.mushokumagic.spell.CastManager;
import net.minecraft.class_1269;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_3222;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

/** Adapts Forge/NeoForge's 1.20.1 event bus to the shared magic systems. */
public final class ForgeEventHandlers {
    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        if (MagicEvents.onChat(event.getPlayer(), event.getRawText())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof class_3222 player) {
            MagicEvents.onJoin(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof class_3222 player) {
            MagicEvents.onLogout(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        class_1309 oldPlayer = event.getOriginal();
        if (event.getEntity() instanceof class_3222 newPlayer) {
            ManaData.copyTo(oldPlayer, newPlayer);
            CastManager.cancel(newPlayer);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != LogicalSide.SERVER || !(event.getEntity() instanceof class_1657 player)) {
            return;
        }
        class_1937 world = (class_1937) event.getLevel();
        class_1269 result = MagicEvents.onUseItem(player, world, event.getHand());
        if (result == class_1269.field_5812) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        MagicEvents.onDeath(event.getEntity(), event.getSource());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MagicEvents.onServerTick(event.getServer());
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        MagicEvents.onServerStopping(event.getServer());
    }
}
