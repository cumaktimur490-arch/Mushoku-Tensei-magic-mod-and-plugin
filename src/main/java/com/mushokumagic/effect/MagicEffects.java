/*
 * Decompiled with CFR.
 */
package com.mushokumagic.effect;

import com.mushokumagic.MushokuMagic;
import com.mushokumagic.effect.ManaOverloadEffect;
import net.minecraft.class_1291;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_6880;
import net.minecraft.class_7923;

public final class MagicEffects {
    public static class_6880<class_1291> MANA_OVERLOAD;

    private MagicEffects() {
    }

    public static void init() {
        MANA_OVERLOAD = class_2378.method_47985((class_2378)class_7923.field_41174, (class_2960)MushokuMagic.id("mana_overload"), (new ManaOverloadEffect()));
    }
}
