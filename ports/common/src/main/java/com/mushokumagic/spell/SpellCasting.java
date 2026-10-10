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
import com.mushokumagic.weather.api.WeatherApi;
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
    private static final int FORMATION_DOTS = 5;

    private SpellCasting() {
    }

    public static CastParams paramsFor(List<String> extraWords) {
        MagicConfig config = MagicConfig.get();
        int bonusWordCount = extraWords.size();
        double radius = 1.0;
        double damage = 1.0;
        double castTime = 1.0;
        boolean explosion = false;
        boolean silent = false;
        boolean largeVisuals = false;
        if (config.keywords != null) {
            for (MagicConfig.KeywordDef keyword : config.keywords.values()) {
                if (keyword == null || keyword.words == null) continue;
                int wordIndex = 0;
                while (wordIndex < extraWords.size()) {
                    String[] matchedWords = SpellCasting.findKeywordPhrase(keyword.words, extraWords, wordIndex);
                    if (matchedWords == null) {
                        ++wordIndex;
                        continue;
                    }
                    radius *= keyword.radiusMultiplier;
                    damage *= keyword.damageMultiplier;
                    castTime *= keyword.castTimeMultiplier;
                    explosion |= keyword.explosion;
                    silent |= keyword.silent;
                    if ("big".equals(keyword.id)) {
                        largeVisuals = true;
                        bonusWordCount -= matchedWords.length;
                    }
                    wordIndex += matchedWords.length;
                }
            }
        }
        double wordBonus = Math.min((double)(Math.max(0, bonusWordCount) * config.wordBonusPerWord), (double)config.maxWordBonus);
        return new CastParams(wordBonus, radius, damage, castTime, explosion, silent, largeVisuals);
    }

    private static String[] findKeywordPhrase(List<String> keywordPhrases, List<String> extraWords, int start) {
        String[] bestMatch = null;
        for (String keywordPhrase : keywordPhrases) {
            if (keywordPhrase == null) continue;
            String normalized = PhraseParser.normalize(keywordPhrase);
            if (normalized.isEmpty()) continue;
            String[] phraseWords = normalized.split(" ");
            if (phraseWords.length <= (bestMatch == null ? 0 : bestMatch.length)
                    || start + phraseWords.length > extraWords.size()) {
                continue;
            }
            boolean matches = true;
            for (int offset = 0; offset < phraseWords.length; ++offset) {
                if (!phraseWords[offset].equals(extraWords.get(start + offset))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                bestMatch = phraseWords;
            }
        }
        return bestMatch;
    }

    public static class_2394 particleFor(String element) {
        return switch (element) {
            case "water" -> class_2398.field_11202;
            case "ice" -> class_2398.field_28013;
            case "earth" -> class_2398.field_11205;
            case "wind" -> class_2398.field_11204;
            case "healing", "repair" -> class_2398.field_11201;
            case "light" -> class_2398.field_11207;
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

    public static class_243 weatherAdjustedAimPoint(class_3222 player, double range, String spellId) {
        class_243 origin = player.method_33571();
        class_243 target = SpellCasting.aimPoint(player, range);
        class_3218 level = player.method_51469();
        return WeatherApi.driftProjectileAimPoint(level, origin, target, spellId, range);
    }

    public static void drawTrail(class_3222 player, Spell spell, CastParams params, double progress) {
        class_3218 class_32182;
        if (params.silent() || !((class_32182 = player.method_51469()) instanceof class_3218)) {
            return;
        }
        class_3218 level = class_32182;
        double power = MagicScaling.clampPower(ManaManager.powerMultiplier(player) * params.wordPower() * params.damageMultiplier());
        double progressClamped = Math.max(0.0, Math.min(1.0, progress));
        int intensity = MagicScaling.intensity(power);
        String spellId = spell.id();
        String visualElement = "ice_needle".equals(spellId) ? "ice" : ("light".equals(spellId) ? "light" : spell.element());
        boolean largeWaterOrFire = params.largeVisuals() && ("water".equals(visualElement) || "fire".equals(visualElement));
        double visualScale = SpellCasting.intrinsicVisualScale(spell);
        class_243 from = player.method_33571();
        double baseRange = switch (spellId) {
            case "water_cannon", "cumulonimbus" -> 64.0;
            case "water_ball", "ice_needle", "stone_ball", "earth_hedgehog" -> 32.0;
            default -> 48.0;
        };
        double castRange = MagicScaling.range(baseRange, power);
        boolean projectile = SpellCasting.isProjectileSpell(spellId) || "water_cannon".equals(spellId);
        class_243 to = projectile
                ? SpellCasting.weatherAdjustedAimPoint(player, castRange, spellId)
                : SpellCasting.aimPoint(player, castRange);
        long now = level.method_8510();

        if (projectile) {
            class_243 focus = from.method_1019(player.method_5720().method_1021(0.62)).method_1019(new class_243(0.0, -0.28, 0.0));
            if (progressClamped < SpellVisualMotion.RELEASE_START) {
                SpellCasting.drawFocus(level, player, focus, visualElement, intensity,
                        progressClamped / SpellVisualMotion.RELEASE_START, power, visualScale, largeWaterOrFire, now);
            } else {
                double releaseProgress = SpellVisualMotion.releaseProgress(progressClamped);
                if ("water_cannon".equals(spellId)) {
                    SpellCasting.drawWaterBeam(level, from, to, intensity, releaseProgress, visualScale, largeWaterOrFire, now);
                } else {
                    SpellCasting.drawProjectile(level, from, to, visualElement, intensity, releaseProgress, power, visualScale, largeWaterOrFire, now);
                }
            }
            return;
        }

        boolean selfCentered = SpellCasting.isSelfCenteredSpell(spellId);
        boolean formingCloud = "cumulonimbus".equals(spellId);
        boolean groundFormation = "earth_hedgehog".equals(spellId);
        class_243 focus = selfCentered
                ? new class_243(player.method_23317(), player.method_23318() + 0.95, player.method_23321())
                : to;
        SpellCasting.drawTargetFormation(
                level,
                player,
                spell,
                focus,
                visualElement,
                intensity,
                progressClamped,
                power,
                visualScale,
                params.radiusMultiplier(),
                largeWaterOrFire,
                selfCentered,
                formingCloud,
                groundFormation,
                now);
    }

    static double intrinsicVisualScale(Spell spell) {
        double spellPower = Double.isFinite(spell.power()) ? Math.max(1.0, spell.power()) : 50.0;
        return Math.max(0.8, Math.min(2.0, Math.sqrt(spellPower / 50.0)));
    }

    private static boolean isProjectileSpell(String spellId) {
        return switch (spellId) {
            case "fire_bolt", "explosive_fireball", "water_ball", "ice_needle", "stone_ball" -> true;
            default -> false;
        };
    }

    private static boolean isSelfCenteredSpell(String spellId) {
        return switch (spellId) {
            case "water_wall", "stone_wall", "swamp", "gust", "updraft", "haste",
                    "heal_basic", "heal_strong", "heal_full", "repair_item" -> true;
            default -> false;
        };
    }

    private static void drawFocus(
            class_3218 level,
            class_3222 player,
            class_243 focus,
            String element,
            int intensity,
            double progress,
            double power,
            double visualScale,
            boolean largeVisuals,
            long now) {
        double charge = Math.max(0.0, Math.min(1.0, progress));
        double pulse = SpellVisualMotion.chargePulse(charge);
        double ringRadius = (0.12 + pulse * (largeVisuals ? 0.65 : 0.46) + charge * 0.12)
                * visualScale;
        double phase = SpellCasting.phase(player, now);
        double x = focus.method_10216();
        double y = focus.method_10214();
        double z = focus.method_10215();
        if (now % 2L == 0L) {
            for (int dot = 0; dot < FORMATION_DOTS; ++dot) {
                double angle = phase + Math.PI * 2.0 * dot / FORMATION_DOTS;
                double particleX = x + Math.cos(angle) * ringRadius;
                double particleY = y + Math.sin(angle * 2.0 + phase * 0.35) * 0.12;
                double particleZ = z + Math.sin(angle) * ringRadius;
                class_2394 particle = dot % 2 == 0
                        ? MagicPalette.body(element, (0.55f + (float)pulse * 0.35f) * (float)visualScale)
                        : MagicPalette.edge(element, (0.45f + (float)pulse * 0.25f) * (float)visualScale);
                level.method_14199(particle, particleX, particleY, particleZ, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        level.method_14199(
                MagicPalette.core(element, (float)((0.9 + pulse * 0.65 + intensity * 0.025) * visualScale)),
                x, y, z,
                1 + (largeVisuals ? 1 : 0),
                0.025 + pulse * 0.035, 0.025 + pulse * 0.035, 0.025 + pulse * 0.035,
                0.008);
        if (now % 4L == 0L) {
            level.method_14199(
                    SpellCasting.particleFor(element),
                    x, y, z,
                    1 + intensity / 6 + (largeVisuals ? 1 : 0),
                    0.04 + pulse * 0.03, 0.04 + pulse * 0.03, 0.04 + pulse * 0.03,
                    0.012);
        }
        if ("fire".equals(element) && power >= 15.0 && now % 2L == 0L) {
            level.method_14199(MagicPalette.surge(element, (0.55f + (float)pulse * 0.2f) * (float)visualScale, true),
                    x, y, z, 1, 0.08, 0.08, 0.08, 0.01);
        }
    }

    private static void drawProjectile(
            class_3218 level,
            class_243 from,
            class_243 to,
            String element,
            int intensity,
            double progress,
            double power,
            double visualScale,
            boolean largeVisuals,
            long now) {
        class_243 segment = to.method_1020(from);
        double distanceSquared = segment.method_1027();
        if (distanceSquared < 1.0E-6) {
            return;
        }
        double travel = SpellVisualMotion.easeInOut(progress);
        class_243 direction = segment.method_1029();
        class_243 head = from.method_1019(segment.method_1021(travel));
        double tailSpacing = Math.min(0.34 + intensity * 0.025, Math.sqrt(distanceSquared) * travel * 0.45);
        class_243 tail = head.method_1020(direction.method_1021(tailSpacing));
        double spread = (0.025 + intensity * 0.003) * visualScale * (largeVisuals ? 1.6 : 1.0);
        float sizeBoost = (float)(visualScale * (largeVisuals ? 1.25 : 1.0));

        level.method_14199(MagicPalette.core(element, (0.9f + intensity * 0.045f) * sizeBoost),
                head.method_10216(), head.method_10214(), head.method_10215(),
                2 + intensity / 4 + (largeVisuals ? 2 : 0), spread, spread, spread, 0.015);
        level.method_14199(MagicPalette.body(element, (0.62f + intensity * 0.035f) * sizeBoost),
                tail.method_10216(), tail.method_10214(), tail.method_10215(),
                1 + intensity / 5, spread * 0.8, spread * 0.8, spread * 0.8, 0.018);
        level.method_14199(SpellCasting.particleFor(element),
                head.method_10216(), head.method_10214(), head.method_10215(),
                1 + intensity / 6 + (largeVisuals ? 1 : 0), spread * 1.4, spread * 1.4, spread * 1.4, 0.025);
        SpellCasting.drawHelicalAccent(level, head, direction, element, intensity, visualScale,
                spread, progress, now, largeVisuals);
        if ("fire".equals(element) && power >= 15.0 && now % 2L == 0L) {
            level.method_14199(MagicPalette.surge(element, 0.65f, true),
                    tail.method_10216(), tail.method_10214(), tail.method_10215(),
                    1, spread * 1.5, spread * 1.5, spread * 1.5, 0.015);
        }
        if (intensity >= 4 && now % 4L == 0L) {
            level.method_14199((class_2394)class_2398.field_11207,
                    head.method_10216(), head.method_10214(), head.method_10215(),
                    1 + intensity / 6, spread * 1.5, spread * 1.5, spread * 1.5, 0.035);
        }
    }

    /** Adds two or three restrained colored strands around a moving projectile core. */
    private static void drawHelicalAccent(
            class_3218 level,
            class_243 anchor,
            class_243 direction,
            String element,
            int intensity,
            double visualScale,
            double width,
            double progress,
            long now,
            boolean largeVisuals) {
        if ((now & 1L) != 0L) {
            return;
        }
        double radius = Math.max(0.075, width * (largeVisuals ? 3.0 : 2.2));
        double phase = now * 0.42 + progress * Math.PI * 3.0;
        int strands = intensity >= 5 ? 3 : 2;
        for (int strand = 0; strand < strands; strand++) {
            double angle = phase + Math.PI * 2.0 * strand / strands;
            class_243 offset = SpellVisualMotion.ribbonOffset(direction, angle, radius);
            class_243 point = anchor.method_1019(offset);
            class_2394 particle = strand == 0
                    ? MagicPalette.edge(element,
                            (float)Math.min(1.25, (0.38 + intensity * 0.025) * visualScale))
                    : MagicPalette.body(element,
                            (float)Math.min(1.25, (0.32 + intensity * 0.02) * visualScale));
            level.method_14199(particle,
                    point.method_10216(), point.method_10214(), point.method_10215(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void drawWaterBeam(
            class_3218 level,
            class_243 from,
            class_243 to,
            int intensity,
            double progress,
            double visualScale,
            boolean largeVisuals,
            long now) {
        if (now % 2L != 0L) {
            return;
        }
        class_243 segment = to.method_1020(from);
        double distanceSquared = segment.method_1027();
        if (distanceSquared < 1.0E-6) {
            return;
        }
        double beamProgress = SpellVisualMotion.easeOut(progress);
        double beamLength = Math.sqrt(distanceSquared) * beamProgress;
        int samples = Math.max(1, Math.min(6, (int)Math.ceil(beamLength / 8.0)));
        double spread = (0.035 + intensity * 0.006) * visualScale * (largeVisuals ? 1.5 : 1.0);
        for (int sampleIndex = 0; sampleIndex <= samples; ++sampleIndex) {
            double amount = beamProgress * (double)sampleIndex / (double)samples;
            class_243 sample = from.method_1019(segment.method_1021(amount));
            level.method_14199(MagicPalette.core("water", Math.min(2.2f,
                    (0.9f + intensity * 0.035f) * (float)visualScale * (largeVisuals ? 1.25f : 1.0f))),
                    sample.method_10216(), sample.method_10214(), sample.method_10215(),
                    1 + intensity / 6, spread, spread, spread, 0.018);
            if (sampleIndex % 2 == 0) {
                class_243 accent = sample.method_1019(SpellVisualMotion.ribbonOffset(
                        segment.method_1029(), now * 0.32 + progress * Math.PI * 2.0 + sampleIndex,
                        Math.max(0.07, spread * (largeVisuals ? 2.7 : 1.9))));
                level.method_14199(MagicPalette.edge("water",
                        (float)Math.min(1.25, (0.62 + intensity * 0.02) * visualScale)),
                        accent.method_10216(), accent.method_10214(), accent.method_10215(),
                        1, 0.0, 0.0, 0.0, 0.0);
            }
            if (sampleIndex % 2 == 0) {
                level.method_14199((class_2394)class_2398.field_11202,
                        sample.method_10216(), sample.method_10214(), sample.method_10215(),
                        1, spread * 1.4, spread, spread * 1.4, 0.025);
            }
        }
    }

    private static void drawTargetFormation(
            class_3218 level,
            class_3222 player,
            Spell spell,
            class_243 focus,
            String element,
            int intensity,
            double progress,
            double power,
            double visualScale,
            double radiusMultiplier,
            boolean largeVisuals,
            boolean selfCentered,
            boolean formingCloud,
            boolean groundFormation,
            long now) {
        double charge = Math.max(0.0, Math.min(1.0, progress));
        double pulse = SpellVisualMotion.chargePulse(charge);
        double baseRadius = Math.max(0.25, MagicScaling.radius(spell.radius(), power, radiusMultiplier) * 0.22 * visualScale);
        double maxRadius = formingCloud ? Math.min(10.0, Math.max(1.5, baseRadius))
                : groundFormation ? Math.min(8.0, Math.max(1.0, baseRadius))
                : (largeVisuals ? 1.05 : 0.72) * visualScale;
        double ringRadius = selfCentered
                ? (0.25 + pulse * 0.45) * visualScale
                : 0.18 + charge * maxRadius;
        double verticalShift = formingCloud ? 5.0 + charge * 3.0 : (groundFormation ? 0.08 + charge * 0.45 : 0.0);
        double phase = SpellCasting.phase(player, now);
        double x = focus.method_10216();
        double y = focus.method_10214() + verticalShift;
        double z = focus.method_10215();

        if (now % 2L == 0L) {
            for (int dot = 0; dot < FORMATION_DOTS; ++dot) {
                double angle = phase + Math.PI * 2.0 * dot / FORMATION_DOTS;
                double particleY = y + Math.sin(angle * 2.0 + phase * 0.4) * (formingCloud ? 0.24 : 0.1);
                double particleX = x + Math.cos(angle) * ringRadius;
                double particleZ = z + Math.sin(angle) * ringRadius;
                if (formingCloud) {
                    level.method_14199((class_2394)class_2398.field_11204,
                            particleX, particleY, particleZ, 1, 0.0, 0.0, 0.0, 0.0);
                    level.method_14199(MagicPalette.body("water", (0.55f + (float)pulse * 0.3f) * (float)visualScale),
                            particleX, particleY, particleZ, 1, 0.015, 0.015, 0.015, 0.004);
                } else {
                    class_2394 particle = groundFormation && dot % 2 == 0
                            ? SpellCasting.particleFor("earth")
                            : MagicPalette.edge(element, (0.5f + (float)pulse * 0.25f) * (float)visualScale);
                    level.method_14199(particle, particleX, particleY, particleZ, 1, 0.01, 0.01, 0.01, 0.006);
                }
            }
        }

        level.method_14199(MagicPalette.core(element, (0.85f + (float)pulse * 0.55f) * (float)visualScale),
                x, y, z, 1 + intensity / 8, 0.04 + pulse * 0.06, 0.04 + pulse * 0.06, 0.04 + pulse * 0.06, 0.01);
        if (now % 4L == 0L) {
            level.method_14199(SpellCasting.particleFor(element),
                    x, y, z, 1 + intensity / 6, 0.06, 0.05, 0.06, 0.015);
        }
    }

    private static double phase(class_3222 player, long now) {
        long uuidPhase = player.method_5667().getLeastSignificantBits() & 0xFFFFL;
        return now * 0.22 + uuidPhase * 0.0004;
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
