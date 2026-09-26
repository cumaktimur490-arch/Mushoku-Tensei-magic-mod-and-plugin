/*
 * Decompiled with CFR.
 */
package com.mushokumagic.spell;

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
                PHRASES.put((Object)normalized, (Object)spell);
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
        String rest;
        String text = PhraseParser.normalize(rawMessage);
        if (text.isEmpty()) {
            return null;
        }
        Spell bestSpell = null;
        String bestPhrase = null;
        for (Map.Entry entry : PHRASES.entrySet()) {
            String phrase = (String)entry.getKey();
            boolean matches = text.equals((Object)phrase) || text.startsWith(phrase + " ");
            if (!matches || bestPhrase != null && phrase.length() <= bestPhrase.length()) continue;
            bestPhrase = phrase;
            bestSpell = (Spell)((Object)entry.getValue());
        }
        if (bestSpell == null || bestPhrase == null) {
            return null;
        }
        ArrayList extraWords = new ArrayList();
        if (text.length() > bestPhrase.length() && !(rest = text.substring(bestPhrase.length() + 1).trim()).isEmpty()) {
            for (String word : rest.split(" ")) {
                if (word.isBlank()) continue;
                extraWords.add((Object)word);
            }
        }
        return new Result(bestSpell, (List<String>)extraWords);
    }

    public record Result(Spell spell, List<String> extraWords) {
    }
}
