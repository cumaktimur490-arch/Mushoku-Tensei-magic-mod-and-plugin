/*
 * Decompiled with CFR.
 */
package com.mushokumagic.item;

import net.minecraft.class_1792;

public class WandItem
extends class_1792 {
    private final int tier;

    public WandItem(int tier, class_1792.class_1793 properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return this.tier;
    }
}
