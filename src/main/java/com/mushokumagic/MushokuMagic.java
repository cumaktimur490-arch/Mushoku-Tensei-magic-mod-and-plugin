/*
 * Decompiled with CFR.
 */
package com.mushokumagic;

import com.mushokumagic.command.MagicCommands;
import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.effect.MagicEffects;
import com.mushokumagic.event.MagicEvents;
import com.mushokumagic.item.MagicItems;
import com.mushokumagic.mana.ManaData;
import com.mushokumagic.spell.SpellRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.class_2960;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MushokuMagic
implements ModInitializer {
    public static final String MOD_ID = "mushoku_magic";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"mushoku_magic");

    public static class_2960 id(String path) {
        return class_2960.method_60655((String)MOD_ID, (String)path);
    }

    public void onInitialize() {
        MagicConfig.load();
        ManaData.init();
        MagicEffects.init();
        MagicItems.init();
        MagicEvents.init();
        MagicCommands.init();
        LOGGER.info("Mushoku Tensei: Magic \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d. \u0417\u0430\u043a\u043b\u0438\u043d\u0430\u043d\u0438\u0439 \u0432 \u043a\u043e\u043d\u0444\u0438\u0433\u0435: {}", (Object)SpellRegistry.spells().size());
    }
}
