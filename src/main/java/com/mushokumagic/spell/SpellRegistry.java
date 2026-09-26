/*
 * Decompiled with CFR.
 */
package com.mushokumagic.spell;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.spell.PhraseParser;
import com.mushokumagic.spell.Spell;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SpellRegistry {
    private static List<Spell> spells = List.of();

    private SpellRegistry() {
    }

    public static void rebuild(List<MagicConfig.SpellDef> definitions) {
        ArrayList built = new ArrayList();
        for (MagicConfig.SpellDef definition : definitions) {
            built.add((Object)new Spell(definition.id, definition.element, definition.cost, Math.max((int)0, (int)definition.castTicks), definition.power, definition.radius, (List<String>)List.copyOf(definition.phrases)));
        }
        spells = List.copyOf((Collection)built);
        PhraseParser.rebuild(spells);
    }

    public static List<Spell> spells() {
        return spells;
    }

    public static Spell byId(String id) {
        for (Spell spell : spells) {
            if (!spell.id().equals((Object)id)) continue;
            return spell;
        }
        return null;
    }
}
