/*
 * Decompiled with CFR.
 */
package com.mushokumagic.client;

import com.mushokumagic.MushokuMagic;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_3675;

@Environment(value=EnvType.CLIENT)
public class MushokuMagicClient
implements ClientModInitializer {
    private static final class_304.class_11900 CATEGORY = class_304.class_11900.method_74698((class_2960)MushokuMagic.id("magic"));
    private static class_304 keySpells;
    private static class_304 keyRepeat;
    private static class_304 keyStatus;

    public void onInitializeClient() {
        keySpells = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.mushoku_magic.spells", class_3675.class_307.field_1668, 75, CATEGORY));
        keyRepeat = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.mushoku_magic.repeat", class_3675.class_307.field_1668, 82, CATEGORY));
        keyStatus = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.mushoku_magic.status", class_3675.class_307.field_1668, 77, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.field_1724 == null || client.method_1562() == null) {
                return;
            }
            while (keySpells.method_1436()) {
                client.field_1724.field_3944.method_45730("spells");
            }
            while (keyRepeat.method_1436()) {
                client.field_1724.field_3944.method_45730("cast last");
            }
            while (keyStatus.method_1436()) {
                client.field_1724.field_3944.method_45730("mana");
            }
        });
    }
}
