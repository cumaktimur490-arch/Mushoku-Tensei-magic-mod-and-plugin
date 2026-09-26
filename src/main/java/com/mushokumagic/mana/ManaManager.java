/*
 * Decompiled with CFR.
 */
package com.mushokumagic.mana;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.effect.MagicEffects;
import com.mushokumagic.item.WandItem;
import com.mushokumagic.mana.ManaData;
import com.mushokumagic.spell.CastManager;
import com.mushokumagic.util.Msg;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_7923;

public final class ManaManager {
    private ManaManager() {
    }

    public static void tickPlayer(class_3222 player) {
        ManaData data = ManaData.of((class_1309)player);
        MagicConfig config = MagicConfig.get();
        if (data.isAdminMode()) {
            if (data.isOverloaded()) {
                ManaManager.clearOverload(player);
            }
            if (data.getMana() < data.getMaxMana()) {
                data.setMana(data.getMaxMana());
            }
            return;
        }
        if (data.isOverloaded()) {
            data.setOverloadTicks(data.getOverloadTicks() - 1);
            if (data.getOverloadTicks() % 20 == 0) {
                ManaManager.reapplyOverloadEffects(player, data);
            }
            if (data.getOverloadTicks() == 0) {
                Msg.actionBar(player, (class_2561)Msg.t("mushoku_magic.msg.overload_end", new Object[0]));
            }
            return;
        }
        if (data.getMana() < data.getMaxMana() && player.method_51469().method_75260() % ((long)config.regenIntervalSeconds * 20L) == 0L) {
            data.setMana(data.getMana() + config.regenAmount);
        }
    }

    private static void reapplyOverloadEffects(class_3222 player, ManaData data) {
        int duration = data.getOverloadTicks();
        if (!player.method_6059(MagicEffects.MANA_OVERLOAD)) {
            player.method_6092(new class_1293(MagicEffects.MANA_OVERLOAD, duration, 0, false, true));
        }
        if (!player.method_6059(class_1294.field_38092)) {
            player.method_6092(new class_1293(class_1294.field_38092, duration, 0, false, true));
        }
        if (!player.method_6059(class_1294.field_5911)) {
            player.method_6092(new class_1293(class_1294.field_5911, duration, 0, false, true));
        }
    }

    public static void triggerOverload(class_3222 player) {
        ManaData data = ManaData.of((class_1309)player);
        MagicConfig config = MagicConfig.get();
        data.setMana(0.0);
        data.addMaxMana(config.overloadMaxBonus);
        data.setOverloadTicks(config.overloadSeconds * 20);
        ManaManager.reapplyOverloadEffects(player, data);
        Msg.chat(player, (class_2561)Msg.t("mushoku_magic.msg.overload_start", config.overloadMaxBonus, config.overloadSeconds / 60));
    }

    public static boolean spend(class_3222 player, double cost) {
        ManaData data = ManaData.of((class_1309)player);
        if (data.isAdminMode()) {
            return true;
        }
        if (data.getMana() + 1.0E-4 < cost) {
            Msg.actionBar(player, (class_2561)Msg.t("mushoku_magic.msg.not_enough_mana", new Object[0]));
            return false;
        }
        data.setMana(data.getMana() - cost);
        if (data.getMana() <= 1.0E-4) {
            ManaManager.triggerOverload(player);
        }
        return true;
    }

    public static double xpThreshold(ManaData data) {
        MagicConfig config = MagicConfig.get();
        return config.xpThresholdBase + Math.max((double)0.0, (double)(data.getMaxMana() - config.startMaxMana)) * config.xpThresholdPerMaxMana;
    }

    public static void awardSpellKill(class_3222 player, class_1309 killed) {
        MagicConfig config = MagicConfig.get();
        ManaData data = ManaData.of((class_1309)player);
        if (ManaManager.isBoss(killed)) {
            data.addMaxMana(config.bossMaxManaBonus);
            Msg.chat(player, (class_2561)Msg.t("mushoku_magic.msg.boss_bonus", config.bossMaxManaBonus, (int)data.getMaxMana()));
            return;
        }
        data.addXp(config.killXp);
        boolean leveled = false;
        while (data.getXp() >= ManaManager.xpThreshold(data)) {
            data.addXp(-ManaManager.xpThreshold(data));
            data.addMaxMana(10.0);
            leveled = true;
        }
        if (leveled) {
            Msg.chat(player, (class_2561)Msg.t("mushoku_magic.msg.xp_levelup", (int)data.getMaxMana(), ManaManager.rankOf((ManaData)data).id));
        } else {
            Msg.actionBar(player, (class_2561)Msg.t("mushoku_magic.msg.xp_gain", config.killXp));
        }
    }

    public static boolean isBoss(class_1309 entity) {
        class_2960 id = class_7923.field_41177.method_10221((Object)entity.method_5864());
        return id != null && MagicConfig.get().bosses.contains((Object)id.toString());
    }

    public static void onSpellLearned(class_3222 player, String spellId) {
        ManaData data = ManaData.of((class_1309)player);
        double bonus = MagicConfig.get().learnMaxManaBonus;
        data.addMaxMana(bonus);
        Msg.chat(player, (class_2561)Msg.t("mushoku_magic.msg.learned", spellId, bonus, (int)data.getMaxMana()));
    }

    public static MagicConfig.RankDef rankOf(ManaData data) {
        return ManaManager.rankOfMax(data.getMaxMana());
    }

    public static MagicConfig.RankDef rankOfMax(double maxMana) {
        MagicConfig config = MagicConfig.get();
        MagicConfig.RankDef result = (MagicConfig.RankDef)config.ranks.get(0);
        for (MagicConfig.RankDef rank : config.ranks) {
            boolean belowMax;
            boolean aboveMin = maxMana >= rank.min;
            boolean bl = belowMax = rank.max <= 0.0 || maxMana <= rank.max;
            if (aboveMin && belowMax) {
                return rank;
            }
            if (!aboveMin) continue;
            result = rank;
        }
        return result;
    }

    public static double rankMultiplier(class_3222 player) {
        return ManaManager.rankOf((ManaData)ManaData.of((class_1309)player)).multiplier;
    }

    public static double wandMultiplier(class_1657 player) {
        double main = ManaManager.wandMultiplierOf(player.method_6047());
        if (main > 0.0) {
            return main;
        }
        return ManaManager.wandMultiplierOf(player.method_6079());
    }

    private static double wandMultiplierOf(class_1799 stack) {
        class_1792 class_17922 = stack.method_7909();
        if (class_17922 instanceof WandItem) {
            WandItem wand = (WandItem)class_17922;
            return MagicConfig.get().wandMultiplier(wand.tier());
        }
        return 0.0;
    }

    public static double powerMultiplier(class_3222 player) {
        return ManaManager.rankMultiplier(player) * Math.max((double)1.0, (double)ManaManager.wandMultiplier((class_1657)player));
    }

    public static boolean hasWand(class_1657 player) {
        return ManaManager.wandMultiplier(player) > 0.0;
    }

    public static void reset(class_3222 player) {
        ManaData data = ManaData.of((class_1309)player);
        data.setMaxMana(MagicConfig.get().startMaxMana);
        data.setMana(data.getMaxMana());
        data.setXp(0.0);
        data.setOverloadTicks(0);
        data.getLearned().clear();
        data.getAttempts().clear();
        data.setLastSpell("");
    }

    public static void refill(class_3222 player) {
        ManaData data = ManaData.of((class_1309)player);
        data.setMana(data.getMaxMana());
    }

    public static void clearOverload(class_3222 player) {
        ManaData data = ManaData.of((class_1309)player);
        data.setOverloadTicks(0);
        player.method_6016(MagicEffects.MANA_OVERLOAD);
        player.method_6016(class_1294.field_38092);
        player.method_6016(class_1294.field_5911);
    }

    public static void setAdminMode(class_3222 player, boolean enabled) {
        ManaData data = ManaData.of((class_1309)player);
        data.setAdminMode(enabled);
        CastManager.cancel(player);
        if (enabled) {
            ManaManager.clearOverload(player);
            ManaManager.refill(player);
        }
    }

    public static boolean isOverloaded(class_3222 player) {
        return ManaData.of((class_1309)player).isOverloaded();
    }

    public static class_3218 levelOf(class_3222 player) {
        class_3218 serverLevel;
        class_3218 class_32182 = player.method_51469();
        return class_32182 instanceof class_3218 ? (serverLevel = class_32182) : null;
    }
}
