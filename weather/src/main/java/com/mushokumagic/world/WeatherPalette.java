package com.mushokumagic.world;

import net.minecraft.class_2390;
import net.minecraft.class_2394;

/** Neutral, storm-oriented colors owned by the weather mod (not the spell palette). */
public final class WeatherPalette {
    private WeatherPalette() {
    }

    public static class_2394 dust(int color, float scale) {
        float safeScale = Float.isFinite(scale) ? Math.max(0.01f, Math.min(4.0f, scale)) : 0.8f;
        return new class_2390(color, safeScale);
    }

    public static class_2394 lightningCore(float scale) {
        return dust(0xF3FCFF, scale);
    }
}
