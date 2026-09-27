/*
 * Decompiled with CFR.
 */
package com.mushokumagic.spell;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.mana.ManaData;
import com.mushokumagic.mana.ManaManager;
import com.mushokumagic.spell.CastParams;
import com.mushokumagic.spell.Spell;
import com.mushokumagic.util.Msg;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_1309;
import net.minecraft.class_239;
import net.minecraft.class_2394;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_3218;
import net.minecraft.class_3222;

public final class SpellCasting {
    private SpellCasting() {
    }

    public static CastParams paramsFor(List<String> extraWords) {
        MagicConfig config = MagicConfig.get();
        double wordBonus = Math.min((double)((double)extraWords.size() * config.wordBonusPerWord), (double)config.maxWordBonus);
        double radius = 1.0;
        double damage = 1.0;
        double castTime = 1.0;
        boolean explosion = false;
        boolean silent = false;
        for (String word : extraWords) {
            for (MagicConfig.KeywordDef keyword : config.keywords.values()) {
                if (!keyword.words.contains(word)) continue;
                radius *= keyword.radiusMultiplier;
                damage *= keyword.damageMultiplier;
                castTime *= keyword.castTimeMultiplier;
                explosion |= keyword.explosion;
                silent |= keyword.silent;
            }
        }
        return new CastParams(wordBonus, radius, damage, castTime, explosion, silent);
    }

    public static class_2394 particleFor(String element) {
        String string = element;
        int n = -1;
        switch (string.hashCode()) {
            case 112903447: {
                if (!string.equals("water")) break;
                n = 0;
                break;
            }
            case 96278602: {
                if (!string.equals("earth")) break;
                n = 1;
                break;
            }
            case 3649544: {
                if (!string.equals("wind")) break;
                n = 2;
                break;
            }
            case 795549946: {
                if (!string.equals("healing")) break;
                n = 3;
                break;
            }
            case -934535283: {
                if (!string.equals("repair")) break;
                n = 4;
            }
        }
        return switch (n) {
            case 0 -> class_2398.field_11202;
            case 1 -> class_2398.field_11205;
            case 2 -> class_2398.field_11204;
            case 3, 4 -> class_2398.field_11201;
            default -> class_2398.field_11240;
        };
    }

    public static class_243 aimPoint(class_3222 player, double range) {
        class_239 hit = player.method_5745(range, 0.0f, false);
        if (hit.method_17783() == class_239.class_240.field_1333) {
            return player.method_33571().method_1019(player.method_5720().method_1021(range));
        }
        return hit.method_17784();
    }

    public static void drawTrail(class_3222 player, Spell spell, CastParams params, double progress) {
        class_3218 class_32182;
        if (params.silent() || !((class_32182 = player.method_51469()) instanceof class_3218)) {
            return;
        }
        class_3218 level = class_32182;
        double power = MagicScaling.clampPower(ManaManager.powerMultiplier(player) * params.wordPower() * params.damageMultiplier());
        double progressClamped = Math.max(0.0, Math.min(1.0, progress));
        class_243 from = player.method_33571();
        class_243 to = SpellCasting.aimPoint(player, MagicScaling.range(48.0, power));
        class_243 point = from.method_35590(to, progressClamped);
        int intensity = MagicScaling.intensity(power);
        int particleCount = 4 + Math.min(8, intensity);
        double spread = 0.02 + intensity * 0.004;
        level.method_65096(SpellCasting.particleFor(spell.element()), point.method_10216(), point.method_10214(), point.method_10215(), particleCount, spread, spread, spread, 0.01);
        long castTick = Math.round(progressClamped * Math.max(1, spell.castTicks()));
        if (intensity >= 4 && castTick % 4 == 0) {
            level.method_65096((class_2394)class_2398.field_11207, point.method_10216(), point.method_10214(), point.method_10215(), Math.min(6, intensity / 2), spread * 1.5, spread * 1.5, spread * 1.5, 0.04);
        }
    }

    public static String progressText(int ticksLeft) {
        return String.format((Locale)Locale.ROOT, (String)"%.1f", (Object[])new Object[]{(double)ticksLeft / 20.0});
    }

    public static void announceCast(class_3222 player, Spell spell, CastParams params) {
        Msg.actionBar(player, (class_2561)Msg.t("mushoku_magic.msg.casting", Msg.spellName(spell.id()), SpellCasting.progressText(spell.castTicks())));
    }

    public static boolean knows(class_3222 player, Spell spell) {
        return ManaData.of((class_1309)player).hasLearned(spell.id());
    }
}
