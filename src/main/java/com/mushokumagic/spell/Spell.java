/*
 * Decompiled with CFR.
 */
package com.mushokumagic.spell;

import java.util.List;

public record Spell(String id, String element, double cost, int castTicks, double power, double radius, List<String> phrases) {
    public String nameKey() {
        return "mushoku_magic.spell." + this.id;
    }

    public double castSeconds() {
        return (double)this.castTicks / 20.0;
    }
}
