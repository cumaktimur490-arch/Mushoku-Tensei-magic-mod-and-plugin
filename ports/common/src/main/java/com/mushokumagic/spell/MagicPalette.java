package com.mushokumagic.spell;

import net.minecraft.class_2390;
import net.minecraft.class_2394;

/**
 * Anime-inspired spell colors: warm, layered fire; bright water/ice highlights;
 * earthy stone; and soft green-gold healing light. No anime artwork is bundled.
 */
public final class MagicPalette {
    private static final Palette FIRE = new Palette(0xFFF3D1, 0xFFAA36, 0xE94732, 0x873FE0);
    private static final Palette WATER = new Palette(0xF3FCFF, 0x53DBF2, 0x2B79D8, 0x8BF3FF);
    private static final Palette ICE = new Palette(0xFFFFFF, 0xB7F2FF, 0x69C6ED, 0xE0FBFF);
    private static final Palette EARTH = new Palette(0xD8CBB9, 0xA98868, 0x62564A, 0xE4B966);
    private static final Palette WIND = new Palette(0xF4FFFF, 0xB5EDF4, 0x78C7D8, 0xC4FCFF);
    private static final Palette HEALING = new Palette(0xFFFBE7, 0xBFEAA1, 0x63B888, 0xFFD86E);
    private static final Palette REPAIR = new Palette(0xFFF5CA, 0xEBCB5A, 0xB58231, 0xA7DDA2);
    private static final Palette LIGHT = new Palette(0xFFFFEE, 0xFFE27D, 0xFFBD45, 0xFFF4C0);

    private MagicPalette() {
    }

    public static class_2394 core(String element, float scale) {
        return new class_2390(palette(element).core(), clampScale(scale));
    }

    public static class_2394 body(String element, float scale) {
        return new class_2390(palette(element).body(), clampScale(scale));
    }

    public static class_2394 edge(String element, float scale) {
        return new class_2390(palette(element).edge(), clampScale(scale));
    }

    /** Purple is reserved for overcharged fire, echoing the series' highest-tier fire effect. */
    public static class_2394 surge(String element, float scale, boolean overcharged) {
        Palette palette = palette(element);
        int color = "fire".equals(element) && overcharged ? palette.surge() : palette.edge();
        return new class_2390(color, clampScale(scale));
    }

    private static Palette palette(String element) {
        if (element == null) {
            return WIND;
        }
        return switch (element) {
            case "fire" -> FIRE;
            case "water" -> WATER;
            case "ice" -> ICE;
            case "earth" -> EARTH;
            case "healing" -> HEALING;
            case "repair" -> REPAIR;
            case "light" -> LIGHT;
            case "wind" -> WIND;
            default -> WIND;
        };
    }

    private static float clampScale(float scale) {
        return Float.isFinite(scale) ? Math.max(0.01f, Math.min(4.0f, scale)) : 0.8f;
    }

    private record Palette(int core, int body, int edge, int surge) {
    }
}
