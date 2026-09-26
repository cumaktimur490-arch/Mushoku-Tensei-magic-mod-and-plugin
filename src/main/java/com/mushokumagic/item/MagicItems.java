/*
 * Decompiled with CFR.
 */
package com.mushokumagic.item;

import com.mushokumagic.MushokuMagic;
import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.item.WandItem;
import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.class_1792;
import net.minecraft.class_1935;
import net.minecraft.class_2378;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7706;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import net.minecraft.class_9290;
import net.minecraft.class_9334;

public final class MagicItems {
    public static final class_1792 MAGIC_STONE_1 = MagicItems.register("magic_stone_1", new class_1792.class_1793().method_7889(16));
    public static final class_1792 MAGIC_STONE_2 = MagicItems.register("magic_stone_2", new class_1792.class_1793().method_7889(16));
    public static final class_1792 MAGIC_STONE_3 = MagicItems.register("magic_stone_3", new class_1792.class_1793().method_7889(16));
    public static final class_1792 ENCHANTED_STICK_1 = MagicItems.register("enchanted_stick_1", new class_1792.class_1793().method_7889(16));
    public static final class_1792 ENCHANTED_STICK_2 = MagicItems.register("enchanted_stick_2", new class_1792.class_1793().method_7889(16));
    public static final class_1792 ENCHANTED_STICK_3 = MagicItems.register("enchanted_stick_3", new class_1792.class_1793().method_7889(16));
    public static final class_1792 WAND_1 = MagicItems.registerWand(1, "wand_1");
    public static final class_1792 WAND_2 = MagicItems.registerWand(2, "wand_2");
    public static final class_1792 WAND_3 = MagicItems.registerWand(3, "wand_3");

    private MagicItems() {
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent((class_5321)class_7706.field_41062).register((ItemGroupEvents.ModifyEntries) entries -> {
            entries.method_45421((class_1935)MAGIC_STONE_1);
            entries.method_45421((class_1935)MAGIC_STONE_2);
            entries.method_45421((class_1935)MAGIC_STONE_3);
            entries.method_45421((class_1935)ENCHANTED_STICK_1);
            entries.method_45421((class_1935)ENCHANTED_STICK_2);
            entries.method_45421((class_1935)ENCHANTED_STICK_3);
            entries.method_45421((class_1935)WAND_1);
            entries.method_45421((class_1935)WAND_2);
            entries.method_45421((class_1935)WAND_3);
        });
    }

    private static class_1792 register(String name, class_1792.class_1793 properties) {
        class_5321 key = class_5321.method_29179((class_5321)class_7924.field_41197, (class_2960)MushokuMagic.id(name));
        return (class_1792)class_2378.method_39197((class_2378)class_7923.field_41178, (class_5321)key, new class_1792(properties.method_63686(key)));
    }

    private static class_1792 registerWand(int tier, String name) {
        class_5321 key = class_5321.method_29179((class_5321)class_7924.field_41197, (class_2960)MushokuMagic.id(name));
        double multiplier = MagicConfig.get().wandMultiplier(tier);
        class_1792.class_1793 properties = new class_1792.class_1793().method_7889(1).method_63686(key).method_57349(class_9334.field_49632, new class_9290(List.of(class_2561.method_43469((String)"item.mushoku_magic.wand.lore", (Object[])new Object[]{multiplier}))));
        return (class_1792)class_2378.method_39197((class_2378)class_7923.field_41178, (class_5321)key, (new WandItem(tier, properties)));
    }

    public static class_1792 wandForTier(int tier) {
        return switch (tier) {
            case 1 -> WAND_1;
            case 2 -> WAND_2;
            case 3 -> WAND_3;
            default -> null;
        };
    }
}
