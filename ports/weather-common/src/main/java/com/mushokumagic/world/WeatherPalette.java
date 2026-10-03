package com.mushokumagic.world;

import net.minecraft.class_2390;
import net.minecraft.class_2394;
import org.joml.Vector3f;

/** Neutral, storm-oriented colors owned by the weather mod (not the spell palette). */
public final class WeatherPalette {
    private WeatherPalette() {
    }

    public static class_2394 dust(int color, float scale) {
        float safeScale = Float.isFinite(scale) ? Math.max(0.01f, Math.min(4.0f, scale)) : 0.8f;
        float red = ((color >>> 16) & 0xFF) / 255.0f;
        float green = ((color >>> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        return new class_2390(new Vector3f(red, green, blue), safeScale);
    }

    public static class_2394 lightningCore(float scale) {
        return dust(0xF3FCFF, scale);
    }
}
