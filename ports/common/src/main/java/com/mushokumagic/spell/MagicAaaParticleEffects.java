package com.mushokumagic.spell;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.platform.MagicFxNetwork;
import net.minecraft.class_243;
import net.minecraft.class_3218;

/** Chooses a lightweight Effekseer impact family; ordinary particles remain the fallback. */
public final class MagicAaaParticleEffects {
    private static final int FIRE = 0;
    private static final int BLUE = 1;
    private static final int GOLD = 2;

    private MagicAaaParticleEffects() {
    }

    public static void burst(class_3218 level, class_243 center, String element, double radius) {
        if (!MagicConfig.get().aaaParticlesSpellEffects || center == null || !Double.isFinite(radius)) {
            return;
        }

        int effectKind;
        double intrinsicSize;
        switch (element == null ? "" : element) {
            case "fire" -> {
                effectKind = FIRE;
                intrinsicSize = 8.0;
            }
            case "earth", "healing", "repair", "light" -> {
                effectKind = GOLD;
                intrinsicSize = 4.0;
            }
            default -> {
                effectKind = BLUE;
                intrinsicSize = 4.0;
            }
        }

        // Spell radius already includes wand power and the user's "big" modifier.
        // Capping effect scale bounds GPU work while leaving the vanilla fallback unchanged.
        float scale = (float) Math.max(0.2, Math.min(4.0, Math.max(0.0, radius) / intrinsicSize));
        MagicFxNetwork.spawn(level, effectKind,
                center.method_10216(), center.method_10214(), center.method_10215(), scale);
    }
}
