package com.mushokumagic.item;

import com.mushokumagic.config.MagicConfig;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1836;
import net.minecraft.class_1937;
import net.minecraft.class_2561;

public class WandItem extends class_1792 {
    private final int tier;

    public WandItem(int tier, class_1792.class_1793 properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public void method_7851(class_1799 stack, class_1937 world, List<class_2561> tooltip, class_1836 context) {
        super.method_7851(stack, world, tooltip, context);
        String multiplier = String.format(Locale.ROOT, "%.0f", MagicConfig.get().wandMultiplier(tier));
        tooltip.add(class_2561.method_43469("item.mushoku_magic.wand.lore", multiplier));
    }
}
