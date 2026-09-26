/*
 * Decompiled with CFR.
 */
package com.mushokumagic.spell;

import com.mushokumagic.MushokuMagic;
import com.mushokumagic.mana.ManaManager;
import com.mushokumagic.spell.CastParams;
import com.mushokumagic.spell.Spell;
import com.mushokumagic.spell.SpellCasting;
import com.mushokumagic.util.Msg;
import com.mushokumagic.world.MagicHitTracker;
import com.mushokumagic.world.TemporaryBlocks;
import java.util.List;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_1922;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2374;
import net.minecraft.class_238;
import net.minecraft.class_2382;
import net.minecraft.class_239;
import net.minecraft.class_2394;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_2769;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_4538;
import net.minecraft.class_4770;
import net.minecraft.class_6089;

public final class SpellEffects {
    private SpellEffects() {
    }

    public static void execute(class_3222 caster, Spell spell, CastParams params) {
        class_3218 class_32182 = caster.method_51469();
        if (!(class_32182 instanceof class_3218)) {
            return;
        }
        class_3218 level = class_32182;
        double power = ManaManager.powerMultiplier(caster) * params.wordPower() * params.damageMultiplier();
        String string = spell.id();
        int n = -1;
        switch (string.hashCode()) {
            case -286757122: {
                if (!string.equals("fire_bolt")) break;
                n = 0;
                break;
            }
            case 102970646: {
                if (!string.equals("light")) break;
                n = 1;
                break;
            }
            case 396918863: {
                if (!string.equals("explosive_fireball")) break;
                n = 2;
                break;
            }
            case 1960874023: {
                if (!string.equals("water_ball")) break;
                n = 3;
                break;
            }
            case -414135101: {
                if (!string.equals("ice_needle")) break;
                n = 4;
                break;
            }
            case 1961499634: {
                if (!string.equals("water_wall")) break;
                n = 5;
                break;
            }
            case 1666334105: {
                if (!string.equals("stone_ball")) break;
                n = 6;
                break;
            }
            case 1666959716: {
                if (!string.equals("stone_wall")) break;
                n = 7;
                break;
            }
            case 109846752: {
                if (!string.equals("swamp")) break;
                n = 8;
                break;
            }
            case 3184591: {
                if (!string.equals("gust")) break;
                n = 9;
                break;
            }
            case -233942042: {
                if (!string.equals("updraft")) break;
                n = 10;
                break;
            }
            case 99050123: {
                if (!string.equals("haste")) break;
                n = 11;
                break;
            }
            case 0x11F7F717: {
                if (!string.equals("heal_basic")) break;
                n = 12;
                break;
            }
            case 1259647182: {
                if (!string.equals("heal_strong")) break;
                n = 13;
                break;
            }
            case 9862790: {
                if (!string.equals("heal_full")) break;
                n = 14;
                break;
            }
            case 1668727941: {
                if (!string.equals("repair_item")) break;
                n = 15;
            }
        }
        switch (n) {
            case 0: {
                SpellEffects.fireBolt(caster, level, spell, params, power);
                break;
            }
            case 1: {
                SpellEffects.light(caster, level, spell, params, power);
                break;
            }
            case 2: {
                SpellEffects.explosiveFireball(caster, level, spell, params, power);
                break;
            }
            case 3: {
                SpellEffects.waterBall(caster, level, spell, params, power);
                break;
            }
            case 4: {
                SpellEffects.iceNeedle(caster, level, spell, params, power);
                break;
            }
            case 5: {
                SpellEffects.waterWall(caster, level, spell, params, power);
                break;
            }
            case 6: {
                SpellEffects.stoneBall(caster, level, spell, params, power);
                break;
            }
            case 7: {
                SpellEffects.stoneWall(caster, level, spell, params, power);
                break;
            }
            case 8: {
                SpellEffects.swamp(caster, level, spell, params, power);
                break;
            }
            case 9: {
                SpellEffects.gust(caster, level, spell, params, power);
                break;
            }
            case 10: {
                SpellEffects.updraft(caster, level, params);
                break;
            }
            case 11: {
                SpellEffects.haste(caster, level, spell, params, power);
                break;
            }
            case 12: {
                SpellEffects.healBasic(caster, level, spell, params, power);
                break;
            }
            case 13: {
                SpellEffects.healStrong(caster, level, spell, params, power);
                break;
            }
            case 14: {
                SpellEffects.healFull(caster, level, spell, params, power);
                break;
            }
            case 15: {
                SpellEffects.repair(caster, level, spell, params, power);
                break;
            }
            default: {
                MushokuMagic.LOGGER.warn("\u041d\u0435\u0442 \u0440\u0435\u0430\u043b\u0438\u0437\u0430\u0446\u0438\u0438 \u0434\u043b\u044f \u0437\u0430\u043a\u043b\u0438\u043d\u0430\u043d\u0438\u044f {}", spell.id());
            }
        }
    }

    private static void fireBolt(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.aimPoint(caster, 48.0);
        SpellEffects.impact(caster, level, point, spell.radius(), spell.power() * power, 5.0 * power, params);
    }

    private static void explosiveFireball(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.aimPoint(caster, 48.0);
        double radius = spell.radius() * params.radiusMultiplier();
        List<class_1309> targets = level.method_18467(class_1309.class, SpellEffects.boxAround(point, radius));
        for (class_1309 target : targets) {
            if (target == caster) continue;
            MagicHitTracker.mark(target, caster, level.method_75260());
        }
        level.method_8537((class_1297)caster, point.method_10216(), point.method_10214(), point.method_10215(), (float)radius, false, class_1937.class_7867.field_40888);
        SpellEffects.igniteAround(level, point, 6, radius);
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_11221, point.method_10216(), point.method_10214(), point.method_10215(), 1, 0.0, 0.0, 0.0, 0.0);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), (class_3414)class_3417.field_15152.comp_349(), class_3419.field_15248, 1.0f, 1.0f);
        }
    }

    private static void light(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_239 hit = caster.method_5745(8.0, 0.0f, false);
        class_243 target = hit.method_17783() == class_239.class_240.field_1333 ? caster.method_73189() : hit.method_17784();
        class_2338 pos = class_2338.method_49638((class_2374)target);
        if (!level.method_8320(pos).method_45474()) {
            pos = pos.method_10084();
        }
        int durationTicks = (int)Math.max((long)20L, (long)Math.round((double)(40.0 * power)));
        boolean placed = TemporaryBlocks.place(level, pos, (class_2680)class_2246.field_31037.method_9564().method_11657((class_2769)class_6089.field_31187, (Comparable)Integer.valueOf((int)15)), durationTicks);
        if (!placed) {
            Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.no_place", new Object[0]));
            return;
        }
        if (!params.silent()) {
            level.method_43128(null, (double)pos.method_10263() + 0.5, (double)pos.method_10264() + 0.5, (double)pos.method_10260() + 0.5, class_3417.field_26980, class_3419.field_15248, 0.7f, 1.4f);
        }
    }

    private static void waterBall(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.aimPoint(caster, 32.0);
        double radius = Math.max((double)1.0, (double)(spell.radius() * params.radiusMultiplier()));
        SpellEffects.extinguish(level, point, radius);
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_11202, point.method_10216(), point.method_10214(), point.method_10215(), 40, 0.5, 0.5, 0.5, 0.1);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_14810, class_3419.field_15248, 1.0f, 1.0f);
        }
    }

    private static void iceNeedle(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.aimPoint(caster, 32.0);
        class_1309 target = SpellEffects.nearest(level, point, 2.0, caster);
        if (target != null) {
            SpellEffects.magicDamage(caster, level, target, 2.0 * power, 0.0);
            target.method_6092(new class_1293(class_1294.field_5909, (int)Math.max((long)20L, (long)Math.round((double)(40.0 * power))), 0));
            MagicHitTracker.mark(target, caster, level.method_75260());
        }
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_28013, point.method_10216(), point.method_10214(), point.method_10215(), 25, 0.25, 0.25, 0.25, 0.02);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_15081, class_3419.field_15248, 0.6f, 1.6f);
        }
    }

    private static void waterWall(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        int duration = (int)Math.max((long)40L, (long)Math.round((double)(100.0 * power)));
        int placed = 0;
        class_2338 center = caster.method_24515();
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = 0; dy <= 1; ++dy) {
                    class_2338 pos = center.method_10069(dx, dy, dz);
                    if (!TemporaryBlocks.place(level, pos, class_2246.field_10295.method_9564(), duration)) continue;
                    ++placed;
                }
            }
        }
        if (placed == 0) {
            Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.no_place", new Object[0]));
            return;
        }
        if (!params.silent()) {
            level.method_43128(null, (double)center.method_10263() + 0.5, (double)center.method_10264() + 0.5, (double)center.method_10260() + 0.5, class_3417.field_14843, class_3419.field_15248, 1.0f, 1.2f);
        }
    }

    private static void stoneBall(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.aimPoint(caster, 32.0);
        double radius = Math.max((double)1.0, (double)(spell.radius() * params.radiusMultiplier()));
        List<class_1309> targets = level.method_18467(class_1309.class, SpellEffects.boxAround(point, radius));
        for (class_1309 target : targets) {
            if (target == caster) continue;
            SpellEffects.magicDamage(caster, level, target, spell.power() * power, 0.0);
            break;
        }
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_11205, point.method_10216(), point.method_10214(), point.method_10215(), 20, 0.3, 0.3, 0.3, 0.05);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_14658, class_3419.field_15248, 0.8f, 1.0f);
        }
    }

    private static void stoneWall(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        int duration = (int)Math.max((long)40L, (long)Math.round((double)(200.0 * power)));
        class_243 look = caster.method_5720();
        class_243 forward = new class_243(look.field_1352, 0.0, look.field_1350);
        if (forward.method_1027() < 1.0E-4) {
            forward = new class_243(0.0, 0.0, 1.0);
        }
        forward = forward.method_1029();
        class_243 side = new class_243(-forward.field_1350, 0.0, forward.field_1352);
        class_2338 base = class_2338.method_49638((class_2374)caster.method_73189().method_1019(forward.method_1021(2.0)));
        int placed = 0;
        for (int offset = -2; offset <= 2; ++offset) {
            for (int height = 0; height < 3; ++height) {
                int dz;
                int dx = (int)Math.round((double)(side.field_1352 * (double)offset));
                class_2338 pos = base.method_10069(dx, height, dz = (int)Math.round((double)(side.field_1350 * (double)offset)));
                if (!TemporaryBlocks.place(level, pos, class_2246.field_10340.method_9564(), duration)) continue;
                ++placed;
            }
        }
        if (placed == 0) {
            Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.no_place", new Object[0]));
            return;
        }
        if (!params.silent()) {
            level.method_43128(null, (double)base.method_10263() + 0.5, (double)base.method_10264() + 0.5, (double)base.method_10260() + 0.5, class_3417.field_14574, class_3419.field_15248, 1.0f, 0.8f);
        }
    }

    private static void swamp(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        double radius = Math.max((double)3.0, (double)(spell.radius() * params.radiusMultiplier()));
        class_243 look = caster.method_5720();
        int duration = (int)Math.max((long)20L, (long)Math.round((double)(120.0 * power)));
        int affected = 0;
        for (class_1309 target : level.method_18467(class_1309.class, caster.method_5829().method_1014(radius))) {
            class_243 toTarget;
            if (target == caster || (toTarget = target.method_73189().method_1020(caster.method_73189())).method_1027() > 1.0 && toTarget.method_1029().method_1026(look) < 0.2) continue;
            target.method_6092(new class_1293(class_1294.field_5909, duration, 2));
            ++affected;
        }
        Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.swamp", affected));
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_11233, caster.method_23317(), caster.method_23318() + 0.2, caster.method_23321(), 60, radius / 2.0, 0.2, radius / 2.0, 0.0);
            level.method_43128(null, caster.method_23317(), caster.method_23318(), caster.method_23321(), class_3417.field_14788, class_3419.field_15248, 0.8f, 0.6f);
        }
    }

    private static void gust(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        double radius = Math.max((double)2.0, (double)(spell.radius() * params.radiusMultiplier()));
        int affected = 0;
        for (class_1309 target : level.method_18467(class_1309.class, caster.method_5829().method_1014(radius))) {
            if (target == caster) continue;
            class_243 push = target.method_73189().method_1020(caster.method_73189());
            if (push.method_1027() > 0.001) {
                push = push.method_1029().method_1021(0.9 * params.damageMultiplier());
                target.method_5762(push.field_1352, 0.45, push.field_1350);
                target.field_6037 = true;
            }
            target.method_5646();
            target.method_20803(0);
            ++affected;
        }
        SpellEffects.extinguish(level, caster.method_73189(), radius + 2.0);
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_11204, caster.method_23317(), caster.method_23318() + 1.0, caster.method_23321(), 40, radius / 2.0, 0.5, radius / 2.0, 0.05);
            level.method_43128(null, caster.method_23317(), caster.method_23318(), caster.method_23321(), (class_3414)class_3417.field_49049.comp_349(), class_3419.field_15248, 1.0f, 1.2f);
        }
        Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.gust", affected));
    }

    private static void updraft(class_3222 caster, class_3218 level, CastParams params) {
        caster.method_5762(0.0, 0.9, 0.0);
        caster.field_6037 = true;
        caster.method_6016(class_1294.field_5909);
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_11204, caster.method_23317(), caster.method_23318(), caster.method_23321(), 25, 0.3, 0.1, 0.3, 0.15);
        }
    }

    private static void haste(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        int duration = (int)Math.max((long)20L, (long)Math.round((double)(40.0 * power)));
        caster.method_6092(new class_1293(class_1294.field_5904, duration, 2));
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_11204, caster.method_23317(), caster.method_23318() + 0.5, caster.method_23321(), 20, 0.4, 0.2, 0.4, 0.1);
        }
    }

    private static void healBasic(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        caster.method_6092(new class_1293(class_1294.field_5924, (int)Math.max((long)20L, (long)Math.round((double)(100.0 * power))), 0));
        SpellEffects.healVisual(caster, level, params);
    }

    private static void healStrong(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        caster.method_6092(new class_1293(class_1294.field_5924, (int)Math.max((long)20L, (long)Math.round((double)(200.0 * power))), 0));
        caster.method_6092(new class_1293(class_1294.field_5907, (int)Math.max((long)20L, (long)Math.round((double)(60.0 * power))), 0));
        SpellEffects.healVisual(caster, level, params);
    }

    private static void healFull(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        caster.method_6092(new class_1293(class_1294.field_5924, (int)Math.max((long)20L, (long)Math.round((double)(300.0 * power))), 0));
        caster.method_6092(new class_1293(class_1294.field_5907, (int)Math.max((long)20L, (long)Math.round((double)(100.0 * power))), 0));
        caster.method_6025((float)(2.0 * power));
        SpellEffects.healVisual(caster, level, params);
    }

    private static void healVisual(class_3222 caster, class_3218 level, CastParams params) {
        if (params.silent()) {
            return;
        }
        level.method_65096((class_2394)class_2398.field_11201, caster.method_23317(), caster.method_23318() + 1.0, caster.method_23321(), 12, 0.4, 0.4, 0.4, 0.02);
        level.method_43128(null, caster.method_23317(), caster.method_23318(), caster.method_23321(), class_3417.field_26980, class_3419.field_15248, 0.8f, 1.6f);
    }

    private static void repair(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_1799 stack = caster.method_6047();
        if (!SpellEffects.isRepairable(stack)) {
            stack = caster.method_6079();
        }
        if (!SpellEffects.isRepairable(stack)) {
            Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.repair_nothing", new Object[0]));
            return;
        }
        int repairAmount = (int)Math.ceil((double)((double)stack.method_7936() * spell.power() * power));
        stack.method_7974(Math.max((int)0, (int)(stack.method_7919() - repairAmount)));
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_11211, caster.method_23317(), caster.method_23318() + 1.0, caster.method_23321(), 15, 0.4, 0.4, 0.4, 0.02);
            level.method_43128(null, caster.method_23317(), caster.method_23318(), caster.method_23321(), class_3417.field_14559, class_3419.field_15248, 0.7f, 1.4f);
        }
        Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.repair_done", new Object[0]));
    }

    private static boolean isRepairable(class_1799 stack) {
        return !stack.method_7960() && stack.method_7963() && stack.method_7986();
    }

    private static class_238 boxAround(class_243 center, double radius) {
        return new class_238(center.method_10216() - radius, center.method_10214() - radius, center.method_10215() - radius, center.method_10216() + radius, center.method_10214() + radius, center.method_10215() + radius);
    }

    private static void magicDamage(class_3222 caster, class_3218 level, class_1309 target, double amount, double fireSeconds) {
        if (target == caster) {
            return;
        }
        MagicHitTracker.mark(target, caster, level.method_75260());
        target.method_64397(level, level.method_48963().method_48815((class_1297)caster, (class_1297)caster), (float)amount);
        if (fireSeconds > 0.0) {
            target.method_20803((int)(fireSeconds * 20.0));
        }
    }

    private static void impact(class_3222 caster, class_3218 level, class_243 point, double radius, double damage, double fireSeconds, CastParams params) {
        double finalRadius = radius * params.radiusMultiplier();
        List<class_1309> targets = level.method_18467(class_1309.class, SpellEffects.boxAround(point, finalRadius));
        if (params.explosion()) {
            for (class_1309 target : targets) {
                if (target == caster) continue;
                MagicHitTracker.mark(target, caster, level.method_75260());
            }
            level.method_8537((class_1297)caster, point.method_10216(), point.method_10214(), point.method_10215(), (float)finalRadius, false, class_1937.class_7867.field_40888);
        } else {
            for (class_1309 target : targets) {
                SpellEffects.magicDamage(caster, level, target, damage, fireSeconds);
            }
        }
        SpellEffects.igniteAround(level, point, 3, finalRadius);
        if (!params.silent()) {
            level.method_65096((class_2394)class_2398.field_11240, point.method_10216(), point.method_10214(), point.method_10215(), 30, 0.4, 0.4, 0.4, 0.05);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_15013, class_3419.field_15248, 1.0f, 1.0f);
        }
    }

    private static void igniteAround(class_3218 level, class_243 center, int attempts, double radius) {
        for (int i = 0; i < attempts; ++i) {
            class_2680 fire;
            double z;
            double y;
            double x = center.method_10216() + (level.field_9229.method_43058() - 0.5) * radius * 2.0;
            class_2338 pos = class_2338.method_49637((double)x, (double)(y = center.method_10214() + (level.field_9229.method_43058() - 0.5) * radius), (double)(z = center.method_10215() + (level.field_9229.method_43058() - 0.5) * radius * 2.0));
            class_2680 state = level.method_8320(pos);
            if (!state.method_45474() || !(fire = class_4770.method_24416((class_1922)level, (class_2338)pos)).method_26184((class_4538)level, pos)) continue;
            level.method_8501(pos, fire);
        }
    }

    private static void extinguish(class_3218 level, class_243 center, double radius) {
        class_2338 origin = class_2338.method_49638((class_2374)center);
        int r = (int)Math.ceil((double)radius);
        for (class_2338 pos : class_2338.method_10097((class_2338)origin.method_10069(-r, -r, -r), (class_2338)origin.method_10069(r, r, r))) {
            class_2680 state;
            if (origin.method_10262((class_2382)pos) > radius * radius || !(state = level.method_8320(pos)).method_27852(class_2246.field_10036) && !state.method_27852(class_2246.field_22089)) continue;
            level.method_8650(pos, false);
        }
        for (class_1309 entity : level.method_18467(class_1309.class, SpellEffects.boxAround(center, radius))) {
            entity.method_5646();
            entity.method_20803(0);
        }
    }

    private static class_1309 nearest(class_3218 level, class_243 point, double radius, class_3222 caster) {
        class_1309 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (class_1309 entity : level.method_18467(class_1309.class, SpellEffects.boxAround(point, radius))) {
            double distance;
            if (entity == caster || !((distance = entity.method_73189().method_1025(point)) < bestDistance)) continue;
            bestDistance = distance;
            best = entity;
        }
        return best;
    }
}
