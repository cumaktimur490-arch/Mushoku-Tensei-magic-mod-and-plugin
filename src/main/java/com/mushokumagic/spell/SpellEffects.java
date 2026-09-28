/*
 * Decompiled with CFR.
 */
package com.mushokumagic.spell;

import com.mushokumagic.MushokuMagic;
import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.mana.ManaManager;
import com.mushokumagic.spell.CastParams;
import com.mushokumagic.spell.Spell;
import com.mushokumagic.spell.SpellCasting;
import com.mushokumagic.util.Msg;
import com.mushokumagic.world.LocalStormManager;
import com.mushokumagic.world.MagicHitTracker;
import com.mushokumagic.world.TemporaryBlocks;
import com.mushokumagic.world.WeatherPhysics;
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
        double power = MagicScaling.clampPower(ManaManager.powerMultiplier(caster) * params.wordPower() * params.damageMultiplier());
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
                break;
            }
            case -1062012635: {
                if (!string.equals("water_cannon")) break;
                n = 16;
                break;
            }
            case 1363602955: {
                if (!string.equals("cumulonimbus")) break;
                n = 17;
                break;
            }
            case 1726806096: {
                if (!string.equals("earth_hedgehog")) break;
                n = 18;
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
                SpellEffects.updraft(caster, level, params, power);
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
            case 16: {
                SpellEffects.waterCannon(caster, level, spell, params, power);
                break;
            }
            case 17: {
                SpellEffects.cumulonimbus(caster, level, spell, params, power);
                break;
            }
            case 18: {
                SpellEffects.earthHedgehog(caster, level, spell, params, power);
                break;
            }
            default: {
                MushokuMagic.LOGGER.warn("\u041d\u0435\u0442 \u0440\u0435\u0430\u043b\u0438\u0437\u0430\u0446\u0438\u0438 \u0434\u043b\u044f \u0437\u0430\u043a\u043b\u0438\u043d\u0430\u043d\u0438\u044f {}", spell.id());
            }
        }
    }

    private static void fireBolt(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.weatherAdjustedAimPoint(caster, MagicScaling.range(48.0, power), "fire_bolt");
        SpellEffects.impact(caster, level, point, spell.radius(), spell.power() * power, 5.0 * power, params, power);
    }

    private static void explosiveFireball(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.weatherAdjustedAimPoint(caster, MagicScaling.range(48.0, power), "explosive_fireball");
        double radius = MagicScaling.radius(spell.radius(), power, params.radiusMultiplier());
        List<class_1309> targets = level.method_18467(class_1309.class, SpellEffects.boxAround(point, radius));
        for (class_1309 target : targets) {
            if (target == caster || target.method_73189().method_1025(point) > radius * radius) continue;
            MagicHitTracker.mark(target, caster, level.method_75260());
        }
        boolean modifyBlocks = MagicConfig.get().fireSpellsModifyBlocks;
        boolean allowFire = modifyBlocks && WeatherPhysics.ignitionMultiplier(level, point) >= 0.2;
        level.method_8537((class_1297)caster, point.method_10216(), point.method_10214(), point.method_10215(), (float)radius, allowFire, modifyBlocks ? class_1937.class_7867.field_40889 : class_1937.class_7867.field_40888);
        if (modifyBlocks) {
            SpellEffects.igniteAround(level, point, SpellEffects.fireAttemptCount(radius), radius);
        }
        SpellEffects.fireBurst(level, point, radius, power, params);
        if (!params.silent()) {
            float volume = (float)Math.min(2.5, 1.0 + Math.log1p(power) * 0.2);
            float pitch = (float)Math.max(0.55, 1.05 - MagicScaling.intensity(power) * 0.035);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), (class_3414)class_3417.field_15152.comp_349(), class_3419.field_15248, volume, pitch);
        }
    }

    private static void light(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_239 hit = caster.method_5745(MagicScaling.range(8.0, power), 0.0f, false);
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
            SpellEffects.coloredBurst(level, new class_243(pos.method_10263() + 0.5, pos.method_10264() + 0.5, pos.method_10260() + 0.5), "light", 1.0, power, false);
            level.method_43128(null, (double)pos.method_10263() + 0.5, (double)pos.method_10264() + 0.5, (double)pos.method_10260() + 0.5, class_3417.field_26980, class_3419.field_15248, 0.7f, 1.4f);
        }
    }

    private static void waterBall(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.weatherAdjustedAimPoint(caster, MagicScaling.range(32.0, power), "water_ball");
        double radius = Math.max(1.0, MagicScaling.radius(spell.radius(), power, params.radiusMultiplier()));
        SpellEffects.extinguish(level, point, radius);
        if (!params.silent()) {
            SpellEffects.waterBurst(level, point, radius, power, params.largeVisuals());
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_14810, class_3419.field_15248, 1.0f, 1.0f);
        }
    }

    private static void waterCannon(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 start = caster.method_33571();
        class_243 point = SpellCasting.weatherAdjustedAimPoint(caster, MagicScaling.range(64.0, power), "water_cannon");
        class_243 segment = point.method_1020(start);
        double lengthSquared = segment.method_1027();
        if (lengthSquared < 1.0E-6) return;
        double radius = Math.max(0.5, MagicScaling.radius(spell.radius(), power, params.radiusMultiplier()));
        class_238 corridor = new class_238(
                Math.min(start.method_10216(), point.method_10216()) - radius,
                Math.min(start.method_10214(), point.method_10214()) - radius,
                Math.min(start.method_10215(), point.method_10215()) - radius,
                Math.max(start.method_10216(), point.method_10216()) + radius,
                Math.max(start.method_10214(), point.method_10214()) + radius,
                Math.max(start.method_10215(), point.method_10215()) + radius);
        int maxTargets = 8 + MagicScaling.intensity(power) * 4;
        int affected = 0;
        for (class_1309 target : level.method_18467(class_1309.class, corridor)) {
            if (target == caster) continue;
            class_243 fromStart = target.method_73189().method_1020(start);
            double progress = Math.max(0.0, Math.min(1.0, fromStart.method_1026(segment) / lengthSquared));
            class_243 closest = start.method_1019(segment.method_1021(progress));
            if (target.method_73189().method_1025(closest) > radius * radius) continue;
            double waterPower = WeatherPhysics.elementalPowerMultiplier(level, target.method_73189(), "water", target);
            SpellEffects.magicDamage(caster, level, target, spell.power() * power * waterPower, 0.0);
            class_243 push = segment.method_1029().method_1021(Math.min(2.25, 0.9 + Math.sqrt(power) * 0.12));
            target.method_5762(push.field_1352, Math.min(0.8, 0.2 + power * 0.01), push.field_1350);
            target.field_6037 = true;
            if (++affected >= maxTargets) break;
        }
        if (!params.silent()) {
            int intensity = MagicScaling.intensity(power);
            double length = Math.sqrt(lengthSquared);
            int samples = Math.max(5, Math.min(20, (int)Math.ceil(length / 4.0)));
            for (int i = 0; i <= samples; ++i) {
                class_243 sample = start.method_1019(segment.method_1021((double)i / (double)samples));
                double spread = Math.min(0.6, 0.12 + intensity * 0.025) * (params.largeVisuals() ? 1.7 : 1.0);
                level.method_65096(MagicPalette.core("water", 0.95f), sample.method_10216(), sample.method_10214(), sample.method_10215(), 3 + intensity / 2 + (params.largeVisuals() ? 2 : 0), spread, spread, spread, 0.025);
                if (i % 2 == 0) {
                    level.method_65096((class_2394)class_2398.field_11202, sample.method_10216(), sample.method_10214(), sample.method_10215(), 1 + intensity / 4 + (params.largeVisuals() ? 2 : 0), spread * 1.5, spread, spread * 1.5, 0.035);
                }
            }
            SpellEffects.waterBurst(level, point, radius, power, params.largeVisuals());
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_14843, class_3419.field_15248, 1.2f, 0.75f);
        }
    }

    private static void iceNeedle(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.weatherAdjustedAimPoint(caster, MagicScaling.range(32.0, power), "ice_needle");
        double radius = Math.max(2.0, MagicScaling.radius(spell.radius(), power, params.radiusMultiplier()));
        int maxTargets = MagicScaling.intensity(power);
        int affected = 0;
        for (class_1309 target : level.method_18467(class_1309.class, SpellEffects.boxAround(point, radius))) {
            if (target == caster || target.method_73189().method_1025(point) > radius * radius) continue;
            double coldPower = WeatherPhysics.elementalPowerMultiplier(level, target.method_73189(), "ice", target);
            SpellEffects.magicDamage(caster, level, target, 2.0 * power * coldPower, 0.0);
            target.method_6092(new class_1293(class_1294.field_5909, (int)Math.max((long)20L, (long)Math.round((double)(40.0 * power * coldPower))), 0));
            MagicHitTracker.mark(target, caster, level.method_75260());
            if (++affected >= maxTargets) break;
        }
        if (!params.silent()) {
            SpellEffects.iceBurst(level, point, radius, power);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_15081, class_3419.field_15248, 0.6f, 1.6f);
        }
    }

    private static void waterWall(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        int duration = (int)Math.min(12000.0, Math.max(40.0, Math.round(100.0 * power)));
        double sizePower = MagicScaling.areaMultiplier(power) * Math.sqrt(Math.max(0.1, params.radiusMultiplier()));
        int halfSize = Math.max(1, Math.min(6, (int)Math.ceil(sizePower / 2.0)));
        int height = Math.max(2, Math.min(5, 2 + MagicScaling.intensity(power) / 4));
        int placed = 0;
        class_2338 center = caster.method_24515();
        for (int dx = -halfSize; dx <= halfSize; ++dx) {
            for (int dz = -halfSize; dz <= halfSize; ++dz) {
                if (Math.abs(dx) != halfSize && Math.abs(dz) != halfSize) continue;
                for (int dy = 0; dy < height; ++dy) {
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
            SpellEffects.waterBurst(level, new class_243(center.method_10263() + 0.5, center.method_10264() + 0.5, center.method_10260() + 0.5), halfSize, power, params.largeVisuals());
            level.method_43128(null, (double)center.method_10263() + 0.5, (double)center.method_10264() + 0.5, (double)center.method_10260() + 0.5, class_3417.field_14843, class_3419.field_15248, 1.0f, 1.2f);
        }
    }

    private static void cumulonimbus(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.aimPoint(caster, MagicScaling.range(64.0, power));
        double radius = Math.max(3.0, MagicScaling.radius(spell.radius(), power, params.radiusMultiplier()));
        int duration = (int)Math.min(1200.0, Math.max(40.0, Math.round(100.0 * power)));
        int stormDuration = (int)Math.min(2400.0, Math.max(1200.0, Math.round(600.0 * power)));
        int affected = 0;
        for (class_1309 target : level.method_18467(class_1309.class, SpellEffects.boxAround(point, radius))) {
            if (target == caster || target.method_73189().method_1025(point) > radius * radius) continue;
            target.method_6092(new class_1293(class_1294.field_5909, duration, 0));
            ++affected;
        }
        SpellEffects.extinguish(level, point, radius);
        if (!params.silent()) {
            LocalStormManager.start(level, point, stormDuration);
            int intensity = MagicScaling.intensity(power);
            double cloudRadius = Math.min(24.0, Math.max(4.0, radius * 0.75));
            if (params.largeVisuals()) cloudRadius = Math.min(36.0, cloudRadius * 1.65);
            class_243 cloud = point.method_1019(new class_243(0.0, Math.max(5.0, Math.min(12.0, radius * 0.25)), 0.0));
            int cloudCount = Math.min(params.largeVisuals() ? 360 : 260, 50 + intensity * 16 + (params.largeVisuals() ? 80 : 0));
            int rainCount = Math.min(params.largeVisuals() ? 600 : 420, 80 + intensity * 22 + (params.largeVisuals() ? 140 : 0));
            SpellEffects.coloredBurst(level, cloud, "water", Math.min(8.0, cloudRadius * 0.4), power, false, params.largeVisuals());
            level.method_65096((class_2394)class_2398.field_11204, cloud.method_10216(), cloud.method_10214(), cloud.method_10215(), cloudCount, cloudRadius, 2.5, cloudRadius, 0.015);
            level.method_65096(MagicPalette.body("water", 0.7f), cloud.method_10216(), cloud.method_10214(), cloud.method_10215(), 18 + intensity * 5 + (params.largeVisuals() ? 30 : 0), cloudRadius * 0.7, 1.5, cloudRadius * 0.7, 0.01);
            level.method_65096((class_2394)class_2398.field_11242, cloud.method_10216(), cloud.method_10214(), cloud.method_10215(), rainCount, cloudRadius, 1.5, cloudRadius, 0.12);
            level.method_65096((class_2394)class_2398.field_11202, point.method_10216(), point.method_10214(), point.method_10215(), 28 + intensity * 8 + (params.largeVisuals() ? 48 : 0), radius * 0.55, 0.4, radius * 0.55, 0.04);
            SpellEffects.waterBurst(level, point, Math.min(6.0, radius * 0.25), power, params.largeVisuals());
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_14843, class_3419.field_15248, 1.4f, 0.65f);
        }
        Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.cumulonimbus", affected));
    }

    private static void earthHedgehog(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.aimPoint(caster, MagicScaling.range(32.0, power));
        double radius = Math.max(1.0, MagicScaling.radius(spell.radius(), power, params.radiusMultiplier()));
        int intensity = MagicScaling.intensity(power);
        int maxTargets = 24 + intensity * 8;
        int affected = 0;
        int slowTicks = (int)Math.min(200.0, Math.max(20.0, Math.round(40.0 * power)));
        for (class_1309 target : level.method_18467(class_1309.class, SpellEffects.boxAround(point, radius))) {
            if (target == caster || target.method_73189().method_1025(point) > radius * radius) continue;
            SpellEffects.magicDamage(caster, level, target, spell.power() * power, 0.0);
            target.method_6092(new class_1293(class_1294.field_5909, slowTicks, 0));
            target.method_5762(0.0, Math.min(0.9, 0.35 + Math.sqrt(power) * 0.08), 0.0);
            target.field_6037 = true;
            if (++affected >= maxTargets) break;
        }
        if (!params.silent()) {
            int spikeCount = 6 + Math.min(6, intensity / 2);
            int spikeLevels = 2 + intensity / 4;
            double footprint = Math.max(1.0, radius * 0.8);
            double spikeHeight = 1.6 + intensity * 0.3;
            for (int spike = 0; spike < spikeCount; ++spike) {
                double angle = Math.PI * 2.0 * spike / spikeCount;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                for (int step = 1; step <= spikeLevels; ++step) {
                    double progress = (double)step / spikeLevels;
                    double distance = footprint * (0.45 + 0.55 * progress);
                    double x = point.method_10216() + dx * distance;
                    double y = point.method_10214() + progress * spikeHeight;
                    double z = point.method_10215() + dz * distance;
                    double spread = 0.08 + progress * 0.05;
                    level.method_65096(MagicPalette.core("earth", 0.85f + intensity * 0.035f), x, y, z, 2 + intensity / 3, spread, spread, spread, 0.025);
                    level.method_65096((class_2394)class_2398.field_11205, x, y, z, 1 + intensity / 6, spread * 0.8, spread, spread * 0.8, 0.055);
                }
            }
            SpellEffects.earthBurst(level, point, radius, power);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_14658, class_3419.field_15248, 1.1f, 0.8f);
        }
        Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.earth_hedgehog", affected));
    }

    private static void stoneBall(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        class_243 point = SpellCasting.weatherAdjustedAimPoint(caster, MagicScaling.range(32.0, power), "stone_ball");
        double radius = Math.max(1.0, MagicScaling.radius(spell.radius(), power, params.radiusMultiplier()));
        for (class_1309 target : level.method_18467(class_1309.class, SpellEffects.boxAround(point, radius))) {
            if (target == caster || target.method_73189().method_1025(point) > radius * radius) continue;
            SpellEffects.magicDamage(caster, level, target, spell.power() * power, 0.0);
        }
        if (!params.silent()) {
            SpellEffects.earthBurst(level, point, radius, power);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_14658, class_3419.field_15248, 0.8f, 1.0f);
        }
    }

    private static void stoneWall(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        int duration = (int)Math.min(12000.0, Math.max(40.0, Math.round(200.0 * power)));
        class_243 look = caster.method_5720();
        class_243 forward = new class_243(look.field_1352, 0.0, look.field_1350);
        if (forward.method_1027() < 1.0E-4) {
            forward = new class_243(0.0, 0.0, 1.0);
        }
        forward = forward.method_1029();
        class_243 side = new class_243(-forward.field_1350, 0.0, forward.field_1352);
        class_2338 base = class_2338.method_49638((class_2374)caster.method_73189().method_1019(forward.method_1021(2.0)));
        int halfWidth = Math.max(2, Math.min(10, (int)Math.ceil(MagicScaling.areaMultiplier(power) * 0.75)));
        int wallHeight = Math.max(3, Math.min(8, 2 + MagicScaling.intensity(power) / 2));
        int placed = 0;
        for (int offset = -halfWidth; offset <= halfWidth; ++offset) {
            for (int height = 0; height < wallHeight; ++height) {
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
            SpellEffects.earthBurst(level, new class_243(base.method_10263() + 0.5, base.method_10264() + 0.5, base.method_10260() + 0.5), halfWidth, power);
            level.method_43128(null, (double)base.method_10263() + 0.5, (double)base.method_10264() + 0.5, (double)base.method_10260() + 0.5, class_3417.field_14574, class_3419.field_15248, 1.0f, 0.8f);
        }
    }

    private static void swamp(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        double radius = Math.max(3.0, MagicScaling.radius(spell.radius(), power, params.radiusMultiplier()));
        class_243 look = caster.method_5720();
        int duration = (int)Math.min(12000.0, Math.max(20.0, Math.round(120.0 * power)));
        int affected = 0;
        for (class_1309 target : level.method_18467(class_1309.class, caster.method_5829().method_1014(radius))) {
            class_243 toTarget;
            if (target == caster || (toTarget = target.method_73189().method_1020(caster.method_73189())).method_1027() > radius || toTarget.method_1027() > 1.0 && toTarget.method_1029().method_1026(look) < 0.2) continue;
            target.method_6092(new class_1293(class_1294.field_5909, duration, 2));
            ++affected;
        }
        Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.swamp", affected));
        if (!params.silent()) {
            int particles = Math.min(180, 48 + MagicScaling.intensity(power) * 12);
            SpellEffects.coloredBurst(level, new class_243(caster.method_23317(), caster.method_23318() + 0.2, caster.method_23321()), "earth", radius, power, false);
            level.method_65096((class_2394)class_2398.field_11233, caster.method_23317(), caster.method_23318() + 0.2, caster.method_23321(), particles, radius / 2.0, 0.3, radius / 2.0, 0.01);
            level.method_43128(null, caster.method_23317(), caster.method_23318(), caster.method_23321(), class_3417.field_14788, class_3419.field_15248, 0.8f, 0.6f);
        }
    }

    private static void gust(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        double radius = Math.max(2.0, MagicScaling.radius(spell.radius(), power, params.radiusMultiplier()));
        double pushStrength = Math.min(2.75, 0.75 + Math.sqrt(Math.max(1.0, power)) * 0.15)
                * params.damageMultiplier()
                * WeatherPhysics.elementalPowerMultiplier(level, caster.method_73189(), "wind", caster);
        class_243 horizontalWind = new class_243(caster.method_5720().field_1352, 0.0, caster.method_5720().field_1350);
        if (horizontalWind.method_1027() < 1.0E-4) {
            horizontalWind = new class_243(0.0, 0.0, 1.0);
        } else {
            horizontalWind = horizontalWind.method_1029();
        }
        WeatherPhysics.addGust(
                level,
                caster.method_73189(),
                horizontalWind.field_1352,
                horizontalWind.field_1350,
                Math.min(1.1, 0.25 + Math.sqrt(Math.max(1.0, power)) * 0.045),
                Math.min(24.0, Math.max(4.0, radius * 1.5)),
                100);
        int affected = 0;
        for (class_1309 target : level.method_18467(class_1309.class, caster.method_5829().method_1014(radius))) {
            if (target == caster) continue;
            class_243 push = target.method_73189().method_1020(caster.method_73189());
            double distance = push.method_1027();
            if (distance > radius) continue;
            if (distance > 0.001) {
                push = push.method_1029().method_1021(pushStrength);
                target.method_5762(push.field_1352, Math.min(1.5, 0.25 + pushStrength * 0.25), push.field_1350);
                target.field_6037 = true;
            }
            target.method_5646();
            target.method_20803(0);
            ++affected;
        }
        SpellEffects.extinguish(level, caster.method_73189(), radius + 2.0);
        if (!params.silent()) {
            int particles = Math.min(180, 40 + MagicScaling.intensity(power) * 12);
            SpellEffects.coloredBurst(level, new class_243(caster.method_23317(), caster.method_23318() + 1.0, caster.method_23321()), "wind", radius, power, false);
            level.method_65096((class_2394)class_2398.field_47494, caster.method_23317(), caster.method_23318() + 1.0, caster.method_23321(), Math.max(1, particles / 3), radius / 2.0, 0.8, radius / 2.0, 0.08);
            level.method_65096((class_2394)class_2398.field_11204, caster.method_23317(), caster.method_23318() + 1.0, caster.method_23321(), particles, radius / 2.0, 0.5, radius / 2.0, 0.05);
            level.method_43128(null, caster.method_23317(), caster.method_23318(), caster.method_23321(), (class_3414)class_3417.field_49049.comp_349(), class_3419.field_15248, 1.0f, 1.2f);
        }
        Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.gust", affected));
    }

    private static void updraft(class_3222 caster, class_3218 level, CastParams params, double power) {
        double lift = Math.min(2.5, (0.75 + Math.sqrt(Math.max(1.0, power)) * 0.17)
                * WeatherPhysics.elementalPowerMultiplier(level, caster.method_73189(), "wind", caster));
        WeatherPhysics.addUpdraft(
                level,
                caster.method_73189(),
                Math.min(1.1, 0.25 + Math.sqrt(Math.max(1.0, power)) * 0.045),
                Math.min(24.0, Math.max(4.0, 4.0 + Math.sqrt(Math.max(1.0, power)) * 2.0)),
                100);
        caster.method_5762(0.0, lift, 0.0);
        caster.field_6037 = true;
        caster.method_6016(class_1294.field_5909);
        if (!params.silent()) {
            int intensity = MagicScaling.intensity(power);
            SpellEffects.coloredBurst(level, new class_243(caster.method_23317(), caster.method_23318() + 0.6, caster.method_23321()), "wind", 1.5, power, false);
            level.method_65096((class_2394)class_2398.field_47494, caster.method_23317(), caster.method_23318(), caster.method_23321(), 4 + intensity, 0.5, 0.2, 0.5, 0.16);
            level.method_65096((class_2394)class_2398.field_11207, caster.method_23317(), caster.method_23318(), caster.method_23321(), 2 + intensity, 0.35, 0.5, 0.35, 0.08);
        }
    }

    private static void haste(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        int duration = (int)Math.max((long)20L, (long)Math.round((double)(40.0 * power)));
        caster.method_6092(new class_1293(class_1294.field_5904, duration, 2));
        if (!params.silent()) {
            int intensity = MagicScaling.intensity(power);
            SpellEffects.coloredBurst(level, new class_243(caster.method_23317(), caster.method_23318() + 0.5, caster.method_23321()), "wind", 1.5, power, false);
            level.method_65096((class_2394)class_2398.field_11204, caster.method_23317(), caster.method_23318() + 0.5, caster.method_23321(), 20 + intensity * 6, 0.5, 0.3, 0.5, 0.1);
            level.method_65096((class_2394)class_2398.field_11207, caster.method_23317(), caster.method_23318() + 0.5, caster.method_23321(), intensity * 2, 0.4, 0.5, 0.4, 0.06);
        }
    }

    private static void healBasic(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        caster.method_6092(new class_1293(class_1294.field_5924, (int)Math.max((long)20L, (long)Math.round((double)(100.0 * power))), 0));
        SpellEffects.healVisual(caster, level, params, power);
    }

    private static void healStrong(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        caster.method_6092(new class_1293(class_1294.field_5924, (int)Math.max((long)20L, (long)Math.round((double)(200.0 * power))), 0));
        caster.method_6092(new class_1293(class_1294.field_5907, (int)Math.max((long)20L, (long)Math.round((double)(60.0 * power))), 0));
        SpellEffects.healVisual(caster, level, params, power);
    }

    private static void healFull(class_3222 caster, class_3218 level, Spell spell, CastParams params, double power) {
        caster.method_6092(new class_1293(class_1294.field_5924, (int)Math.max((long)20L, (long)Math.round((double)(300.0 * power))), 0));
        caster.method_6092(new class_1293(class_1294.field_5907, (int)Math.max((long)20L, (long)Math.round((double)(100.0 * power))), 0));
        caster.method_6025((float)(2.0 * power));
        SpellEffects.healVisual(caster, level, params, power);
    }

    private static void healVisual(class_3222 caster, class_3218 level, CastParams params, double power) {
        if (params.silent()) {
            return;
        }
        int intensity = MagicScaling.intensity(power);
        double x = caster.method_23317();
        double y = caster.method_23318() + 1.0;
        double z = caster.method_23321();
        SpellEffects.coloredBurst(level, new class_243(x, y, z), "healing", 1.5, power, false);
        level.method_65096((class_2394)class_2398.field_11201, x, y, z, 8 + intensity * 5, 0.6, 0.8, 0.6, 0.04);
        level.method_65096((class_2394)class_2398.field_11211, x, y, z, 4 + intensity * 3, 0.5, 0.7, 0.5, 0.03);
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
            int intensity = MagicScaling.intensity(power);
            SpellEffects.coloredBurst(level, new class_243(caster.method_23317(), caster.method_23318() + 1.0, caster.method_23321()), "repair", 1.5, power, false);
            level.method_65096((class_2394)class_2398.field_11211, caster.method_23317(), caster.method_23318() + 1.0, caster.method_23321(), 12 + intensity * 5, 0.5, 0.5, 0.5, 0.04);
            level.method_65096((class_2394)class_2398.field_11207, caster.method_23317(), caster.method_23318() + 1.0, caster.method_23321(), 6 + intensity * 3, 0.4, 0.5, 0.4, 0.05);
            level.method_43128(null, caster.method_23317(), caster.method_23318(), caster.method_23321(), class_3417.field_14559, class_3419.field_15248, 0.7f, 1.4f);
        }
        Msg.actionBar(caster, (class_2561)Msg.t("mushoku_magic.msg.repair_done", new Object[0]));
    }

    private static boolean isRepairable(class_1799 stack) {
        return !stack.method_7960() && stack.method_7963() && stack.method_7986();
    }

    private static void coloredBurst(class_3218 level, class_243 center, String element, double radius, double power, boolean overcharged) {
        SpellEffects.coloredBurst(level, center, element, radius, power, overcharged, false);
    }

    private static void coloredBurst(class_3218 level, class_243 center, String element, double radius, double power, boolean overcharged, boolean largeVisuals) {
        int intensity = MagicScaling.intensity(power);
        int particleBoost = largeVisuals ? Math.min(48, 16 + intensity * 3) : 0;
        float particleScale = largeVisuals ? 1.35f : 1.0f;
        double maxSpread = largeVisuals ? 32.0 : 16.0;
        double spread = Math.max(0.25, Math.min(maxSpread, radius * 0.5));
        double x = center.method_10216();
        double y = center.method_10214();
        double z = center.method_10215();
        level.method_65096(MagicPalette.core(element, Math.min(1.7f, (0.8f + intensity * 0.05f) * particleScale)), x, y, z, 8 + intensity + particleBoost / 3, spread, spread * 0.55, spread, 0.035);
        level.method_65096(MagicPalette.body(element, Math.min(1.6f, (0.7f + intensity * 0.045f) * particleScale)), x, y, z, 12 + intensity * 2 + particleBoost, spread * 0.85, spread * 0.65, spread * 0.85, 0.045);
        level.method_65096(MagicPalette.edge(element, 0.7f * particleScale), x, y, z, 6 + intensity + particleBoost / 2, spread, spread * 0.8, spread, 0.025);
        if ("fire".equals(element) && overcharged) {
            level.method_65096(MagicPalette.surge(element, 0.85f, true), x, y, z, 6 + intensity + particleBoost / 2, spread * 0.6, spread * 0.5, spread * 0.6, 0.035);
        }
    }

    private static void fireBurst(class_3218 level, class_243 center, double radius, double power, CastParams params) {
        if (params.silent()) return;
        int intensity = MagicScaling.intensity(power);
        double visualRadius = params.largeVisuals() ? Math.min(64.0, radius * 4.0) : radius;
        int particleBoost = params.largeVisuals() ? Math.min(96, 32 + intensity * 5) : 0;
        double spread = Math.max(0.35, Math.min(32.0, visualRadius * 0.5));
        double verticalSpread = Math.max(0.8, Math.min(32.0, visualRadius * 0.5));
        double x = center.method_10216();
        double y = center.method_10214();
        double z = center.method_10215();
        SpellEffects.coloredBurst(level, center, "fire", visualRadius, power, power >= 15.0 || params.explosion(), params.largeVisuals());
        level.method_65096((class_2394)class_2398.field_11221, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        level.method_65096((class_2394)class_2398.field_11236, x, y, z, 2 + intensity, spread * 0.4, verticalSpread * 0.5, spread * 0.4, 0.05);
        level.method_65096((class_2394)class_2398.field_11240, x, y, z, 32 + intensity * 14 + particleBoost, spread, verticalSpread, spread, 0.08);
        level.method_65096((class_2394)class_2398.field_22246, x, y, z, 16 + intensity * 7 + particleBoost / 2, spread * 0.8, verticalSpread * 1.2, spread * 0.8, 0.06);
        level.method_65096((class_2394)class_2398.field_11237, x, y, z, 16 + intensity * 7 + particleBoost / 2, spread * 0.75, verticalSpread, spread * 0.75, 0.025);
        level.method_65096((class_2394)class_2398.field_11207, x, y, z, 8 + intensity * 5 + particleBoost / 3, spread, verticalSpread * 1.4, spread, 0.06);
    }

    private static void waterBurst(class_3218 level, class_243 center, double radius, double power) {
        SpellEffects.waterBurst(level, center, radius, power, false);
    }

    private static void waterBurst(class_3218 level, class_243 center, double radius, double power, boolean largeVisuals) {
        int intensity = MagicScaling.intensity(power);
        double visualRadius = largeVisuals ? Math.min(64.0, radius * 4.0) : radius;
        int particleBoost = largeVisuals ? Math.min(120, 36 + intensity * 7) : 0;
        double spread = Math.max(0.35, Math.min(32.0, visualRadius * 0.5));
        double x = center.method_10216();
        double y = center.method_10214();
        double z = center.method_10215();
        SpellEffects.coloredBurst(level, center, "water", visualRadius, power, false, largeVisuals);
        level.method_65096((class_2394)class_2398.field_11202, x, y, z, 32 + intensity * 12 + particleBoost, spread, 1.0 + (largeVisuals ? 1.0 : 0.0), spread, 0.12);
        level.method_65096((class_2394)class_2398.field_11247, x, y, z, 12 + intensity * 7 + particleBoost / 2, spread * 0.8, 1.4, spread * 0.8, 0.05);
        level.method_65096((class_2394)class_2398.field_11207, x, y, z, 6 + intensity * 4 + particleBoost / 3, spread, 1.2, spread, 0.04);
    }

    private static void iceBurst(class_3218 level, class_243 center, double radius, double power) {
        int intensity = MagicScaling.intensity(power);
        double spread = Math.max(0.25, Math.min(16.0, radius * 0.5));
        double x = center.method_10216();
        double y = center.method_10214();
        double z = center.method_10215();
        SpellEffects.coloredBurst(level, center, "ice", radius, power, false);
        level.method_65096((class_2394)class_2398.field_28013, x, y, z, 18 + intensity * 10, spread, spread, spread, 0.035);
        level.method_65096((class_2394)class_2398.field_11207, x, y, z, 8 + intensity * 5, spread * 1.2, spread * 1.2, spread * 1.2, 0.04);
        level.method_65096((class_2394)class_2398.field_11237, x, y, z, 4 + intensity * 3, spread, spread * 0.5, spread, 0.015);
    }

    private static void earthBurst(class_3218 level, class_243 center, double radius, double power) {
        int intensity = MagicScaling.intensity(power);
        double spread = Math.max(0.25, Math.min(16.0, radius * 0.5));
        double x = center.method_10216();
        double y = center.method_10214();
        double z = center.method_10215();
        SpellEffects.coloredBurst(level, center, "earth", radius, power, false);
        level.method_65096((class_2394)class_2398.field_11205, x, y, z, 24 + intensity * 10, spread, spread, spread, 0.08);
        level.method_65096((class_2394)class_2398.field_11237, x, y, z, 12 + intensity * 5, spread, spread * 0.6, spread, 0.04);
        level.method_65096((class_2394)class_2398.field_11207, x, y, z, 6 + intensity * 4, spread, spread, spread, 0.05);
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

    private static void impact(class_3222 caster, class_3218 level, class_243 point, double radius, double damage, double fireSeconds, CastParams params, double power) {
        double finalRadius = MagicScaling.radius(radius, power, params.radiusMultiplier());
        List<class_1309> targets = level.method_18467(class_1309.class, SpellEffects.boxAround(point, finalRadius));
        if (params.explosion()) {
            for (class_1309 target : targets) {
                if (target == caster || target.method_73189().method_1025(point) > finalRadius * finalRadius) continue;
                MagicHitTracker.mark(target, caster, level.method_75260());
            }
            boolean modifyBlocks = MagicConfig.get().fireSpellsModifyBlocks;
            level.method_8537((class_1297)caster, point.method_10216(), point.method_10214(), point.method_10215(), (float)finalRadius, modifyBlocks, modifyBlocks ? class_1937.class_7867.field_40889 : class_1937.class_7867.field_40888);
        } else {
            for (class_1309 target : targets) {
                if (target == caster || target.method_73189().method_1025(point) > finalRadius * finalRadius) continue;
                class_243 targetPosition = target.method_73189();
                double weatherDamage = WeatherPhysics.elementalPowerMultiplier(level, targetPosition, "fire", target);
                double wetFireDuration = fireSeconds > 0.0
                        ? fireSeconds * WeatherPhysics.fireDurationMultiplier(level, targetPosition, target)
                        : 0.0;
                SpellEffects.magicDamage(caster, level, target, damage * weatherDamage, wetFireDuration);
            }
        }
        if (MagicConfig.get().fireSpellsModifyBlocks) {
            SpellEffects.igniteAround(level, point, SpellEffects.fireAttemptCount(finalRadius), finalRadius);
        }
        SpellEffects.fireBurst(level, point, finalRadius, power, params);
        if (!params.silent()) {
            float volume = (float)Math.min(2.0, 0.8 + Math.log1p(power) * 0.16);
            level.method_43128(null, point.method_10216(), point.method_10214(), point.method_10215(), class_3417.field_15013, class_3419.field_15248, volume, 0.9f);
        }
    }

    private static int fireAttemptCount(double radius) {
        return Math.min(192, Math.max(3, (int)Math.ceil(radius * radius * 0.12)));
    }

    private static void igniteAround(class_3218 level, class_243 center, int attempts, double radius) {
        double ignitionScale = WeatherPhysics.ignitionMultiplier(level, center);
        attempts = (int)Math.round(attempts * ignitionScale);
        if (radius <= 0.0 || attempts <= 0) {
            return;
        }
        int startY = (int)Math.floor(center.method_10214());
        for (int i = 0; i < attempts; ++i) {
            double angle = level.field_9229.method_43058() * Math.PI * 2.0;
            double distance = Math.sqrt(level.field_9229.method_43058()) * radius;
            int x = (int)Math.floor(center.method_10216() + Math.cos(angle) * distance);
            int z = (int)Math.floor(center.method_10215() + Math.sin(angle) * distance);
            for (int drop = 0; drop <= 16; ++drop) {
                class_2338 pos = class_2338.method_49637((double)x, (double)(startY - drop), (double)z);
                class_2680 state = level.method_8320(pos);
                if (!state.method_45474()) continue;
                class_2680 fire = class_4770.method_24416((class_1922)level, (class_2338)pos);
                if (!fire.method_26184((class_4538)level, pos)) continue;
                level.method_8501(pos, fire);
                break;
            }
        }
    }

    private static void extinguish(class_3218 level, class_243 center, double radius) {
        class_2338 origin = class_2338.method_49638((class_2374)center);
        // Keep the cubic block scan bounded; entity effects still use the full spell radius below.
        double blockRadius = Math.min(12.0, Math.max(0.0, radius));
        int r = (int)Math.ceil(blockRadius);
        for (class_2338 pos : class_2338.method_10097((class_2338)origin.method_10069(-r, -r, -r), (class_2338)origin.method_10069(r, r, r))) {
            class_2680 state;
            if (origin.method_10262((class_2382)pos) > blockRadius * blockRadius || !(state = level.method_8320(pos)).method_27852(class_2246.field_10036) && !state.method_27852(class_2246.field_22089)) continue;
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
