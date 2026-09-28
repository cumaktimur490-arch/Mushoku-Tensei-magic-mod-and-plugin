/*
 * Decompiled with CFR.
 */
package com.mushokumagic.spell;

import com.mushokumagic.config.MagicConfig;
import com.mushokumagic.spell.Spell;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PhraseParser {
    private static final Map<String, Spell> PHRASES = new HashMap();

    private PhraseParser() {
    }

    public static void rebuild(List<Spell> spells) {
        PHRASES.clear();
        for (Spell spell : spells) {
            for (String phrase : spell.phrases()) {
                String normalized = PhraseParser.normalize(phrase);
                if (normalized.isEmpty()) continue;
                PHRASES.put(normalized, spell);
            }
        }
    }

    public static String normalize(String input) {
        String lower = input.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435');
        StringBuilder builder = new StringBuilder(lower.length());
        boolean pendingSpace = false;
        for (int i = 0; i < lower.length(); ++i) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit((char)c)) {
                if (pendingSpace && builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(c);
                pendingSpace = false;
                continue;
            }
            pendingSpace = true;
        }
        return builder.toString();
    }

    public static Result parse(String rawMessage) {
        String text = PhraseParser.normalize(rawMessage);
        if (text.isEmpty()) {
            return null;
        }
        ArrayList<String> configuredModifiers = new ArrayList<>();
        Map<String, MagicConfig.KeywordDef> keywords = MagicConfig.get().keywords;
        if (keywords != null) {
            for (MagicConfig.KeywordDef keyword : keywords.values()) {
                if (keyword == null || keyword.words == null) continue;
                for (String configuredModifier : keyword.words) {
                    if (configuredModifier == null) continue;
                    String modifier = PhraseParser.normalize(configuredModifier);
                    if (!modifier.isEmpty() && !configuredModifiers.contains(modifier)) {
                        configuredModifiers.add(modifier);
                    }
                }
            }
        }
        Spell bestSpell = null;
        String bestPhrase = null;
        int bestModifierLength = 0;
        for (Map.Entry entry : PHRASES.entrySet()) {
            String phrase = (String)entry.getKey();
            if (PhraseParser.startsWithPhrase(text, phrase)
                    && (bestPhrase == null || phrase.length() > bestPhrase.length())) {
                bestPhrase = phrase;
                bestSpell = (Spell)entry.getValue();
                bestModifierLength = 0;
            }
            for (String modifier : configuredModifiers) {
                if (!text.startsWith(modifier + " ")) continue;
                String remaining = text.substring(modifier.length() + 1);
                if (!PhraseParser.startsWithPhrase(remaining, phrase)
                        || bestPhrase != null && phrase.length() <= bestPhrase.length()) continue;
                bestPhrase = phrase;
                bestSpell = (Spell)entry.getValue();
                bestModifierLength = modifier.length();
            }
        }
        if (bestSpell == null || bestPhrase == null) {
            return null;
        }
        int phraseStart = bestModifierLength == 0 ? 0 : bestModifierLength + 1;
        int phraseEnd = phraseStart + bestPhrase.length();
        ArrayList<String> extraWords = new ArrayList<>();
        if (bestModifierLength > 0) {
            for (String word : text.substring(0, bestModifierLength).split(" ")) {
                if (!word.isBlank()) extraWords.add(word);
            }
        }
        if (text.length() > phraseEnd) {
            String rest = text.substring(phraseEnd + 1).trim();
            if (!rest.isEmpty()) {
                for (String word : rest.split(" ")) {
                    if (!word.isBlank()) extraWords.add(word);
                }
            }
        }
        return new Result(bestSpell, extraWords);
    }

    private static boolean startsWithPhrase(String text, String phrase) {
        return text.equals(phrase) || text.startsWith(phrase + " ");
    }

    public record Result(Spell spell, List<String> extraWords) {
    }
}
