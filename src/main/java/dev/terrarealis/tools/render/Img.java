package dev.terrarealis.tools.render;

/** An RGB image buffer plus the handful of drawing operations the preview tool needs. */
public final class Img {

    public final int w;
    public final int h;
    public final int[] px;

    public Img(int w, int h) {
        this.w = w;
        this.h = h;
        this.px = new int[w * h];
    }

    public void set(int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < w && y < h) {
            px[y * w + x] = rgb;
        }
    }

    public int get(int x, int y) {
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (x >= w) {
            x = w - 1;
        }
        if (y >= h) {
            y = h - 1;
        }
        return px[y * w + x];
    }

    public void fill(int rgb) {
        java.util.Arrays.fill(px, rgb);
    }

    public void hline(int x0, int x1, int y, int rgb) {
        for (int x = Math.max(0, x0); x <= Math.min(w - 1, x1); x++) {
            px[y * w + x] = rgb;
        }
    }

    public void vline(int x, int y0, int y1, int rgb) {
        for (int y = Math.max(0, y0); y <= Math.min(h - 1, y1); y++) {
            px[y * w + x] = rgb;
        }
    }

    public void rect(int x, int y, int rw, int rh, int rgb) {
        hline(x, x + rw - 1, y, rgb);
        hline(x, x + rw - 1, y + rh - 1, rgb);
        vline(x, y, y + rh - 1, rgb);
        vline(x + rw - 1, y, y + rh - 1, rgb);
    }

    public void fillRect(int x, int y, int rw, int rh, int rgb) {
        for (int j = y; j < y + rh; j++) {
            hline(x, x + rw - 1, j, rgb);
        }
    }

    /** Blend {@code rgb} over the existing pixel with the given alpha. */
    public void blend(int x, int y, int rgb, double a) {
        if (x < 0 || y < 0 || x >= w || y >= h) {
            return;
        }
        int dst = px[y * w + x];
        int r = (int) (((rgb >> 16) & 0xFF) * a + ((dst >> 16) & 0xFF) * (1 - a));
        int g = (int) (((rgb >> 8) & 0xFF) * a + ((dst >> 8) & 0xFF) * (1 - a));
        int b = (int) ((rgb & 0xFF) * a + (dst & 0xFF) * (1 - a));
        px[y * w + x] = (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------- 5x7 glyphs

    /** Draws {@code text} at (x, y) in a fixed 5x7 bitmap font. Returns the width drawn. */
    public int text(int x, int y, String text, int rgb, int scale) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                cx += 6 * scale;
                continue;
            }
            int[] glyph = glyph(ch);
            for (int gy = 0; gy < 7; gy++) {
                for (int gx = 0; gx < 5; gx++) {
                    if (glyph[gy * 5 + gx] != 0) {
                        fillRect(cx + gx * scale, y + gy * scale, scale, scale, rgb);
                    }
                }
            }
            cx += 6 * scale;
        }
        return cx - x;
    }

    /** A tiny built-in font covering the ASCII the labels the preview needs. */
    private static final String[] FONT_ROWS = {
        "A", "011101000110001111111000110001",
        "B", "111101000110001111101000111110",
        "C", "011101000110000100001000101110",
        "D", "111101000110001100011000111110",
        "E", "111111000011110100001000011111",
        "F", "111111000011110100001000010000",
        "G", "011101000110000101111000101110",
        "H", "100011000111111100011000110001",
        "I", "111110010000100001000010011111",
        "J", "001110001000010000101001001110",
        "K", "100011001011100110101000110001",
        "L", "100001000010000100001000011111",
        "M", "100011101110101100011000110001",
        "N", "100011100110101100111000110001",
        "O", "011101000110001100011000101110",
        "P", "111101000110001111101000010000",
        "Q", "011101000110001100011010101101",
        "R", "111101000110001111101001010001",
        "S", "011111000001110000011000111110",
        "T", "111110010000100001000010000100",
        "U", "100011000110001100011000101110",
        "V", "100011000110001100010101000100",
        "W", "100011000110001101011101110001",
        "X", "100011000101110011101000110001",
        "Y", "100011000101110001000010000100",
        "Z", "111110000100010001000100011111",
        "0", "011101000110011101011100101110",
        "1", "001000110000100001000010001110",
        "2", "011101000100001000100010011111",
        "3", "111110001000110000101000111110",
        "4", "100011000111111000010000100001",
        "5", "111111000011110000011000111110",
        "6", "001100100010000111101000101110",
        "7", "111110000100010001000100010000",
        "8", "011101000110001011101000101110",
        "9", "011101000110001011110001001100",
        ".", "000000000000000000000110001100",
        ",", "000000000000000000000110001100",
        "-", "000000000000000111000000000000",
        "+", "000000010001110001000000000000",
        ":", "000000110001100000000110001100",
        "/", "000010000100010001000100010000",
        "(", "000100010001000010000100000010",
        ")", "010000010000010000100010001000",
        "=", "000000000011111000001111100000",
        "%", "110011100100010010011001100011",
        "?", "011101000100001000100000000100",
        "!", "001000010000100001000010000000",
        "_", "000000000000000000000000011111",
        " ", "000000000000000000000000000000",
    };

    private static final java.util.Map<Character, int[]> FONT = buildFont();

    private static int[] glyph(char ch) {
        int[] g = FONT.get(Character.toUpperCase(ch));
        if (g == null) {
            g = FONT.get('?');
        }
        return g;
    }

    private static java.util.Map<Character, int[]> buildFont() {
        java.util.Map<Character, int[]> m = new java.util.HashMap<>();
        String[] rows = FONT_ROWS;
        for (int i = 0; i < rows.length; i += 2) {
            String key = rows[i];
            String bits = rows[i + 1];
            int[] g = new int[35];
            for (int b = 0; b < 35 && b < bits.length(); b++) {
                g[b] = bits.charAt(b) == '1' ? 1 : 0;
            }
            m.put(key.charAt(0), g);
        }
        return m;
    }

}
