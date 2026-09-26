/*
 * Decompiled with CFR.
 */
package com.mushokumagic.util;

import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_5250;

public final class Msg {
    private Msg() {
    }

    public static class_5250 t(String key, Object ... args) {
        return class_2561.method_43469((String)key, (Object[])args);
    }

    public static class_5250 literal(String text) {
        return class_2561.method_43470((String)text);
    }

    public static void chat(class_3222 player, class_2561 component) {
        player.method_7353(component, false);
    }

    public static void actionBar(class_3222 player, class_2561 component) {
        player.method_7353(component, true);
    }

    public static class_5250 spellName(String spellId) {
        return class_2561.method_43471((String)("mushoku_magic.spell." + spellId));
    }

    public static class_5250 rankName(String rankId) {
        return class_2561.method_43471((String)("mushoku_magic.rank." + rankId));
    }
}
