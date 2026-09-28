package com.mushokumagic.client;

import com.mushokumagic.MushokuMagic;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Forge/NeoForge 1.20.1 key bindings for the spell and mana commands. */
@Mod.EventBusSubscriber(modid = MushokuMagic.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MushokuMagicClient {
    private static class_304 keySpells;
    private static class_304 keyRepeat;
    private static class_304 keyStatus;

    private MushokuMagicClient() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        String category = "key.categories.mushoku_magic";
        keySpells = new class_304("key.mushoku_magic.spells", class_3675.class_307.field_1668, GLFW.GLFW_KEY_K, category);
        keyRepeat = new class_304("key.mushoku_magic.repeat", class_3675.class_307.field_1668, GLFW.GLFW_KEY_R, category);
        keyStatus = new class_304("key.mushoku_magic.status", class_3675.class_307.field_1668, GLFW.GLFW_KEY_M, category);
        event.register(keySpells);
        event.register(keyRepeat);
        event.register(keyStatus);
    }

    @Mod.EventBusSubscriber(modid = MushokuMagic.MOD_ID, value = Dist.CLIENT)
    public static final class ClientTicks {
        private ClientTicks() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || keySpells == null) {
                return;
            }
            class_310 client = class_310.method_1551();
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
        }
    }
}
