/*
 * Decompiled with CFR.
 */
package com.mushokumagic.spell;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.mana.ManaData;
import com.mushokumagic.mana.ManaManager;
import com.mushokumagic.spell.CastParams;
import com.mushokumagic.spell.PhraseParser;
import com.mushokumagic.spell.Spell;
import com.mushokumagic.spell.SpellCasting;
import com.mushokumagic.spell.SpellEffects;
import com.mushokumagic.spell.SpellRegistry;
import com.mushokumagic.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1309;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

public final class CastManager {
    private static final Map<UUID, PendingCast> PENDING = new HashMap();

    private CastManager() {
    }

    public static boolean handleChat(class_3222 player, String rawMessage) {
        PhraseParser.Result result = PhraseParser.parse(rawMessage);
        if (result == null) {
            return false;
        }
        CastManager.attempt(player, result.spell(), result.extraWords());
        return true;
    }

    public static boolean attempt(class_3222 player, Spell spell, List<String> extraWords) {
        ManaData data = ManaData.of((class_1309)player);
        MagicConfig config = MagicConfig.get();
        CastParams params = SpellCasting.paramsFor(extraWords);
        if (!data.hasLearned(spell.id())) {
            boolean success;
            int attempts = data.getAttempts(spell.id());
            boolean guaranteed = attempts + 1 >= config.guaranteedAfterAttempts;
            boolean bl = success = guaranteed || player.method_6051().method_43058() >= config.failChance;
            if (!success) {
                data.addAttempt(spell.id());
                ManaManager.spend(player, spell.cost() * 0.5);
                int left = Math.max((int)0, (int)(config.guaranteedAfterAttempts - data.getAttempts(spell.id())));
                Msg.chat(player, (class_2561)Msg.t("mushoku_magic.msg.learn_fail", Msg.spellName(spell.id()), left));
                return true;
            }
            data.learn(spell.id());
            ManaManager.onSpellLearned(player, spell.id());
        }
        if (!data.isAdminMode() && data.getMana() + 1.0E-4 < spell.cost()) {
            Msg.chat(player, (class_2561)Msg.t("mushoku_magic.msg.not_enough_mana_chat", Msg.spellName(spell.id())));
            return true;
        }
        CastManager.start(player, spell, params);
        return true;
    }

    public static void start(class_3222 player, Spell spell, CastParams params) {
        int ticks = (int)Math.max((long)0L, (long)Math.round((double)((double)spell.castTicks() * params.castTimeMultiplier())));
        ManaData.of((class_1309)player).setLastSpell(spell.id());
        if (ticks <= 0) {
            CastManager.finish(player, spell, params);
            return;
        }
        PENDING.put(player.method_5667(), new PendingCast(spell, params, ticks, ticks));
        SpellCasting.announceCast(player, spell, params);
    }

    private static void finish(class_3222 player, Spell spell, CastParams params) {
        if (!ManaManager.spend(player, spell.cost())) {
            return;
        }
        SpellEffects.execute(player, spell, params);
    }

    public static void cancel(class_3222 player) {
        PENDING.remove(player.method_5667());
    }

    public static boolean isCasting(class_3222 player) {
        return PENDING.containsKey(player.method_5667());
    }

    public static boolean castLast(class_3222 player) {
        ManaData data = ManaData.of((class_1309)player);
        Spell spell = SpellRegistry.byId(data.getLastSpell());
        if (spell == null) {
            String id;
            Iterator iterator = data.getLearned().iterator();
            while (iterator.hasNext() && (spell = SpellRegistry.byId(id = (String)iterator.next())) == null) {
            }
        }
        if (spell == null && data.isAdminMode() && !SpellRegistry.spells().isEmpty()) {
            spell = (Spell)(SpellRegistry.spells().get(0));
        }
        if (spell == null) {
            Msg.actionBar(player, (class_2561)Msg.t("mushoku_magic.msg.no_last_spell", new Object[0]));
            return false;
        }
        if (CastManager.isCasting(player)) {
            return false;
        }
        CastManager.attempt(player, spell, List.<String>of());
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            class_3222 player = server.method_3760().method_14602((UUID)entry.getKey());
            PendingCast cast = (PendingCast)(entry.getValue());
            if (player == null || !player.method_5805()) {
                iterator.remove();
                continue;
            }
            int ticksLeft = cast.ticksLeft() - 1;
            SpellCasting.drawTrail(player, cast.spell(), cast.params(), 1.0 - (double)ticksLeft / (double)cast.totalTicks());
            if (ticksLeft > 0) {
                entry.setValue(new PendingCast(cast.spell(), cast.params(), ticksLeft, cast.totalTicks()));
                if (ticksLeft % 5 != 0) continue;
                Msg.actionBar(player, (class_2561)Msg.t("mushoku_magic.msg.casting", Msg.spellName(cast.spell().id()), SpellCasting.progressText(ticksLeft)));
                continue;
            }
            iterator.remove();
            CastManager.finish(player, cast.spell(), cast.params());
        }
    }

    public static List<Spell> learnedSpells(class_3222 player) {
        ManaData data = ManaData.of((class_1309)player);
        if (data.isAdminMode()) {
            return SpellRegistry.spells();
        }
        ArrayList list = new ArrayList();
        for (String id : data.getLearned()) {
            Spell spell = SpellRegistry.byId(id);
            if (spell == null) continue;
            list.add(spell);
        }
        return list;
    }

    private record PendingCast(Spell spell, CastParams params, int ticksLeft, int totalTicks) {
    }
}
