package com.mushokumagic.item;

import com.mushokumagic.MushokuMagic;
import net.minecraft.class_1792;
import net.minecraft.class_7706;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Item registrations used by both 1.20.1 loader builds. */
public final class MagicItems {
    public static final DeferredRegister<class_1792> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MushokuMagic.MOD_ID);

    private static final RegistryObject<class_1792> MAGIC_STONE_1 = register("magic_stone_1", new class_1792(new class_1792.class_1793().method_7889(16)));
    private static final RegistryObject<class_1792> MAGIC_STONE_2 = register("magic_stone_2", new class_1792(new class_1792.class_1793().method_7889(16)));
    private static final RegistryObject<class_1792> MAGIC_STONE_3 = register("magic_stone_3", new class_1792(new class_1792.class_1793().method_7889(16)));
    private static final RegistryObject<class_1792> ENCHANTED_STICK_1 = register("enchanted_stick_1", new class_1792(new class_1792.class_1793().method_7889(16)));
    private static final RegistryObject<class_1792> ENCHANTED_STICK_2 = register("enchanted_stick_2", new class_1792(new class_1792.class_1793().method_7889(16)));
    private static final RegistryObject<class_1792> ENCHANTED_STICK_3 = register("enchanted_stick_3", new class_1792(new class_1792.class_1793().method_7889(16)));
    private static final RegistryObject<class_1792> WAND_1 = register("wand_1", new WandItem(1, new class_1792.class_1793().method_7889(1)));
    private static final RegistryObject<class_1792> WAND_2 = register("wand_2", new WandItem(2, new class_1792.class_1793().method_7889(1)));
    private static final RegistryObject<class_1792> WAND_3 = register("wand_3", new WandItem(3, new class_1792.class_1793().method_7889(1)));

    private MagicItems() {
    }

    private static RegistryObject<class_1792> register(String path, class_1792 item) {
        return ITEMS.register(path, () -> item);
    }

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(class_7706.field_41062)) {
            return;
        }
        event.accept(MAGIC_STONE_1.get());
        event.accept(MAGIC_STONE_2.get());
        event.accept(MAGIC_STONE_3.get());
        event.accept(ENCHANTED_STICK_1.get());
        event.accept(ENCHANTED_STICK_2.get());
        event.accept(ENCHANTED_STICK_3.get());
        event.accept(WAND_1.get());
        event.accept(WAND_2.get());
        event.accept(WAND_3.get());
    }

    public static class_1792 wandForTier(int tier) {
        return switch (tier) {
            case 1 -> WAND_1.get();
            case 2 -> WAND_2.get();
            case 3 -> WAND_3.get();
            default -> null;
        };
    }
}
