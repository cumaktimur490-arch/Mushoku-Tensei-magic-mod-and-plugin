package com.mushokumagic.effect;

import com.mushokumagic.MushokuMagic;
import net.minecraft.class_1291;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Deferred status-effect registry shared by the 1.20.1 loader builds. */
public final class MagicEffects {
    public static final DeferredRegister<class_1291> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MushokuMagic.MOD_ID);
    private static final RegistryObject<class_1291> MANA_OVERLOAD_ENTRY =
            EFFECTS.register("mana_overload", ManaOverloadEffect::new);

    private MagicEffects() {
    }

    public static class_1291 manaOverload() {
        return MANA_OVERLOAD_ENTRY.get();
    }
}
