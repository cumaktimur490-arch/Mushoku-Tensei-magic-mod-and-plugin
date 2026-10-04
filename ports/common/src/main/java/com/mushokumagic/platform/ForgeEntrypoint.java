package com.mushokumagic.platform;

import com.mushokumagic.MushokuMagic;
import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.effect.MagicEffects;
import com.mushokumagic.event.MagicEvents;
import com.mushokumagic.item.MagicItems;
import com.mushokumagic.mana.ManaData;
import com.mushokumagic.command.MagicCommands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Shared FML bootstrap: Forge and NeoForge 1.20.1 expose the same legacy API. */
@Mod(MushokuMagic.MOD_ID)
public final class ForgeEntrypoint {
    public ForgeEntrypoint() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        MagicItems.ITEMS.register(modBus);
        MagicEffects.EFFECTS.register(modBus);
        modBus.addListener(MagicItems::addToCreativeTab);
        MagicFxNetwork.register();
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandlers());
        MinecraftForge.EVENT_BUS.register(this);

        MagicConfig.load();
        ManaData.init();
        MushokuMagic.LOGGER.info("Mushoku Tensei: Magic initialized for Forge-compatible 1.20.1.");
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        MagicCommands.register(event.getDispatcher());
    }
}
