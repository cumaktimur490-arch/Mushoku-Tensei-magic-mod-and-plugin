/*
 * Decompiled with CFR.
 */
package com.mushokumagic.spell;

public record CastParams(double wordBonus, double radiusMultiplier, double damageMultiplier, double castTimeMultiplier, boolean explosion, boolean silent) {
    public static CastParams none() {
        return new CastParams(0.0, 1.0, 1.0, 1.0, false, false);
    }

    public double wordPower() {
        return 1.0 + this.wordBonus;
    }
}
