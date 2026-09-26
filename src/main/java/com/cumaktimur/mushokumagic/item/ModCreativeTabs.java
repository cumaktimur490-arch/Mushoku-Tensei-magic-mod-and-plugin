package com.cumaktimur.mushokumagic.item;

import com.cumaktimur.mushokumagic.MushokuMagicMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MushokuMagicMod.MODID);

    public static final RegistryObject<CreativeModeTab> MUSHOKU_MAGIC_TAB = CREATIVE_MODE_TABS.register("mushoku_magic_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.AQUA_HARTIA_STAFF.get()))
                    .title(Component.translatable("creativetab.mushoku_magic_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        // Посохи - основной контент мода
                        pOutput.accept(ModItems.WAND.get());
                        pOutput.accept(ModItems.MIGURD_STAFF.get());
                        pOutput.accept(ModItems.AQUA_HARTIA_STAFF.get());
                        
                        // Материалы
                        pOutput.accept(ModItems.MANA_CRYSTAL.get());
                        pOutput.accept(ModItems.MIGURD_CRYSTAL.get());
                        pOutput.accept(ModItems.AQUA_CRYSTAL.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
