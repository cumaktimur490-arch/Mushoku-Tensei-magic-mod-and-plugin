package dev.terrarealis.tools;

import dev.terrarealis.kernel.Column;
import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.Material;
import dev.terrarealis.kernel.Rock;
import dev.terrarealis.kernel.Stratigrapher;
import dev.terrarealis.kernel.TerraKernel;
import dev.terrarealis.kernel.biome.BiomeKind;
import dev.terrarealis.kernel.geo.Volcanism;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.tools.render.Img;
import dev.terrarealis.tools.render.Png;
import dev.terrarealis.tools.render.Shade;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders the generator to PNG without Minecraft.
 *
 * <p>This is the acceptance harness. A landscape generator that cannot be looked at cannot be judged,
 * and it certainly cannot be tuned, so every landform the mod claims to produce has an image that shows
 * it: shaded relief, hypsometric tint, actual block colours, biome map, temperature, precipitation,
 * aridity, drainage, slope, lithology, geological cross-sections with caves, and zoomed plates on a
 * desert basin-and-range front, a volcanic province and an alpine range.
 *
 * <p>Usage: {@code PreviewMain [--seed N] [--out DIR] [--quick]}
 */
public final class PreviewMain {

    private static final double MPB = 12.0;

    public static void main(String[] args) throws Exception {
        long seed = 0x5EED1234L;
        Path out = Path.of("previews");
        boolean quick = false;
        String only = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seed" -> seed = Long.decode(args[++i]);
                case "--out" -> out = Path.of(args[++i]);
                case "--only" -> only = args[++i];
                case "--quick" -> quick = true;
                default -> { }
            }
        }
        Files.createDirectories(out);

        GenParams p = GenParams.defaults();
        p.sanitise();
        TerraKernel k = new TerraKernel(seed, p);

        System.out.printf(Locale.ROOT, "Terra Realis preview  seed=0x%X  tile=%d blocks  cell=%d%n",
                seed, k.tileBlocks(), p.erosionCellSize);

        long t0 = System.currentTimeMillis();
        Scout scout = new Scout(k);
        scout.run();
        System.out.printf(Locale.ROOT, "scout: desert=(%d,%d) volcano=(%d,%d) alpine=(%d,%d) coast=(%d,%d)%n",
                scout.desertX, scout.desertZ, scout.volcanoX, scout.volcanoZ,
                scout.alpineX, scout.alpineZ, scout.coastX, scout.coastZ);

        List<String> done = new ArrayList<>();

        // ------------------------------------------------------------------ global
        int gw = quick ? 640 : 900;
        int gh = quick ? 420 : 600;
        int gstep = quick ? 6 : 4;
        int gx = -gw * gstep / 2;
        int gz = -gh * gstep / 2;
        if (want(only, "global")) {
            Field f = Field.build(k, gx, gz, gw, gh, gstep, 2);
            System.out.printf(Locale.ROOT, "global field %dx%d @%d blocks: %d ms%n",
                    gw, gh, gstep, f.buildMillis);
            save(out, "01_hero_materials.png", materialMap(k, f, true));
            done.add("01_hero_materials.png");
            save(out, "02_hero_relief.png", hypsometricMap(k, f, true));
            save(out, "03_biomes.png", biomeMap(k, f, true));
            save(out, "04_temperature.png", scalarMap(k, f, "MEAN ANNUAL TEMPERATURE", f.tempC,
                    new double[] {-34, -18, -6, 2, 10, 18, 25, 31},
                    new int[] {0x1B2A6B, 0x2E6FB7, 0x57B4D8, 0xBFE8F2, 0xF7E9A0, 0xF0A44B, 0xD9541E, 0x8C1B0C},
                    "%.0f C", "%.0f C", "%.0f C", true));
            save(out, "05_precipitation.png", scalarMap(k, f, "ANNUAL PRECIPITATION", f.precipMm,
                    new double[] {0, 120, 300, 600, 1000, 1600, 2400, 3600},
                    new int[] {0xB99A63, 0xD9C08A, 0xE8DDAE, 0xBFD8A0, 0x7FB569, 0x3F8F55, 0x1F6B57, 0x14465C},
                    "0 mm", "%.0f mm", "%.0f mm", true));
            save(out, "06_aridity.png", scalarMap(k, f, "ARIDITY  (0 humid - 1 hyperarid)", f.aridity,
                    new double[] {0, 0.2, 0.4, 0.6, 0.8, 1.0},
                    new int[] {0x1F6B57, 0x6FA85C, 0xC9C46A, 0xE0A95C, 0xC9743A, 0x9C4A22},
                    "0.0", "0.5", "1.0", true));
            save(out, "07_drainage.png", drainageMap(k, f, true));
            save(out, "08_slope.png", scalarMap(k, f, "SLOPE", f.slope,
                    new double[] {0, 4, 9, 16, 25, 34, 45, 60},
                    new int[] {0x2E7D46, 0x7FB569, 0xD9D08A, 0xE0A95C, 0xC9743A, 0xA8452A, 0x6E2418, 0x3A1410},
                    "0 deg", "25 deg", "60 deg", true));
            save(out, "09_geology.png", geologyMap(k, f, true));
            save(out, "10_arid_landforms.png", aridMap(k, f, true));
            stats(f, out, "global");
        }

        // ------------------------------------------------------------- desert close
        if (want(only, "desert")) {
            int dw = quick ? 420 : 620;
            int dh = quick ? 420 : 620;
            int dstep = quick ? 2 : 1;
            int dx = scout.desertX - dw * dstep / 2;
            int dz = scout.desertZ - dh * dstep / 2;
            Field f = Field.build(k, dx, dz, dw, dh, dstep, 2);
            System.out.printf(Locale.ROOT, "desert field %dx%d @%d blocks: %d ms%n",
                    dw, dh, dstep, f.buildMillis);
            save(out, "20_desert_materials.png", materialMap(k, f, false));
            save(out, "21_desert_relief.png", hypsometricMap(k, f, false));
            save(out, "22_desert_biomes.png", biomeMap(k, f, false));
            save(out, "23_desert_drainage.png", drainageMap(k, f, false));
            stats(f, out, "desert");
        }

        // ----------------------------------------------------------- volcano close
        if (want(only, "volcano")) {
            int vw = quick ? 360 : 520;
            int vh = quick ? 360 : 520;
            int vstep = quick ? 3 : 2;
            int vx = scout.volcanoX - vw * vstep / 2;
            int vz = scout.volcanoZ - vh * vstep / 2;
            Field f = Field.build(k, vx, vz, vw, vh, vstep, 2);
            System.out.printf(Locale.ROOT, "volcano field %dx%d @%d blocks: %d ms%n",
                    vw, vh, vstep, f.buildMillis);
            save(out, "30_volcano_materials.png", materialMap(k, f, false));
            save(out, "31_volcano_relief.png", hypsometricMap(k, f, false));
        }

        // ------------------------------------------------------------ alpine close
        if (want(only, "alpine")) {
            int aw = quick ? 380 : 560;
            int ah = quick ? 380 : 560;
            int astep = quick ? 3 : 2;
            int ax = scout.alpineX - aw * astep / 2;
            int az = scout.alpineZ - ah * astep / 2;
            Field f = Field.build(k, ax, az, aw, ah, astep, 2);
            System.out.printf(Locale.ROOT, "alpine field %dx%d @%d blocks: %d ms%n",
                    aw, ah, astep, f.buildMillis);
            save(out, "40_alpine_materials.png", materialMap(k, f, false));
            save(out, "41_alpine_relief.png", hypsometricMap(k, f, false));
        }

        // ------------------------------------------------------------- coast close
        if (want(only, "coast")) {
            int cw = quick ? 340 : 480;
            int ch = quick ? 340 : 480;
            int cstep = quick ? 3 : 2;
            int cx = scout.coastX - cw * cstep / 2;
            int cz = scout.coastZ - ch * cstep / 2;
            Field f = Field.build(k, cx, cz, cw, ch, cstep, 2);
            save(out, "50_coast_materials.png", materialMap(k, f, false));
            save(out, "51_coast_relief.png", hypsometricMap(k, f, false));
        }

        // ------------------------------------------------------------- sections
        if (want(only, "sections")) {
            save(out, "60_section_desert.png",
                    section(k, scout.desertX - 900, scout.desertZ, scout.desertX + 900, scout.desertZ,
                            "CROSS-SECTION  DESERT BASIN AND RANGE", true));
            save(out, "61_section_alpine.png",
                    section(k, scout.alpineX - 1100, scout.alpineZ + 300,
                            scout.alpineX + 1100, scout.alpineZ - 300,
                            "CROSS-SECTION  ALPINE RANGE", true));
            save(out, "62_section_coast.png",
                    section(k, scout.coastX - 900, scout.coastZ - 250,
                            scout.coastX + 900, scout.coastZ + 250,
                            "CROSS-SECTION  COAST AND SHELF", true));
            save(out, "63_section_caves.png",
                    section(k, scout.desertX - 500, scout.desertZ + 200,
                            scout.desertX + 500, scout.desertZ - 200,
                            "CROSS-SECTION  SUBSURFACE AND CAVES", true));
        }

        System.out.printf(Locale.ROOT, "wrote %d images to %s in %d ms%n",
                done.size() + countPng(out), out.toAbsolutePath(), System.currentTimeMillis() - t0);
    }

    private static boolean want(String only, String group) {
        return only == null || only.equals(group);
    }

    private static int countPng(Path dir) throws Exception {
        try (var s = Files.list(dir)) {
            return (int) s.filter(x -> x.toString().endsWith(".png")).count();
        }
    }

    private static void save(Path dir, String name, Img img) throws Exception {
        Png.write(dir.resolve(name), img.w, img.h, img.px);
        System.out.println("  wrote " + name + "  (" + img.w + "x" + img.h + ")");
    }

    // ------------------------------------------------------------------ renders

    /** Real block colours under a sun. This is the closest thing to an aerial photograph. */
    private static Img materialMap(TerraKernel k, Field f, boolean global) {
        double cell = f.step * MPB;
        double[] shade = Shade.hillshade(f.surfaceY, f.w, f.h, cell, 1.0);
        Img img = new Img(f.w, f.h);
        for (int i = 0; i < f.w * f.h; i++) {
            Material m = f.top[i];
            int base = m == null ? Material.STONE.rgb : m.rgb;
            double waterY = f.waterY[i];
            if (!Double.isNaN(waterY) && waterY > f.surfaceY[i]) {
                double depth = waterY - f.surfaceY[i];
                // Water absorbs red first: shallow is turquoise, deep is navy.
                double t = Interp.smoothstep(0.5, 42.0, depth);
                base = Shade.lerpColor(0x4FB9C9, 0x0B2A54, t);
                base = Shade.lerpColor(base, 0x0A2038, Interp.smoothstep(40.0, 180.0, depth));
                img.px[i] = Shade.shade(base, 0.72 + 0.30 * shade[i], 1.02);
                continue;
            }
            // Slight elevation darkening so the image has atmospheric depth.
            double alt = Interp.clamp(1.06 - f.elevM[i] / 22000.0, 0.80, 1.10);
            img.px[i] = Shade.shade(base, shade[i] * alt, 0.96);
        }
        annotate(k, f, img, global, "TERRA REALIS  -  SURFACE MATERIALS");
        return img;
    }

    private static Img hypsometricMap(TerraKernel k, Field f, boolean global) {
        double cell = f.step * MPB;
        double[] shade = Shade.hillshade(f.surfaceY, f.w, f.h, cell, 1.0);
        Img img = new Img(f.w, f.h);
        for (int i = 0; i < f.w * f.h; i++) {
            double e = f.elevM[i];
            int base = Shade.hypsometric(e, f.aridity[i]);
            double waterY = f.waterY[i];
            if (!Double.isNaN(waterY) && waterY > f.surfaceY[i]) {
                base = Shade.hypsometric((waterY - k.params().seaLevel) * MPB
                        - (waterY - f.surfaceY[i]) * MPB, f.aridity[i]);
                base = Shade.lerpColor(base, 0x0E3460, Interp.smoothstep(2.0, 60.0, waterY - f.surfaceY[i]));
            } else if (e < 0.0) {
                // Dry land below sea level: a depression, not an ocean. Salt pan and pale ochre.
                base = Shade.lerpColor(base, 0xE8E0C4, Interp.smoothstep(0.0, -160.0, e) * 0.85);
            }
            img.px[i] = Shade.shade(base, 0.30 + 0.90 * shade[i], 0.94);
        }
        annotate(k, f, img, global, "TERRA REALIS  -  SHADED RELIEF");
        return img;
    }

    private static Img biomeMap(TerraKernel k, Field f, boolean global) {
        Img img = new Img(f.w, f.h);
        double cell = f.step * MPB;
        double[] shade = Shade.hillshade(f.surfaceY, f.w, f.h, cell, 1.0);
        for (int i = 0; i < f.w * f.h; i++) {
            BiomeKind b = f.biome[i];
            int base = b == null ? 0xFF00FF : b.rgb;
            img.px[i] = Shade.shade(base, 0.62 + 0.48 * shade[i], 1.0);
        }
        annotate(k, f, img, global, "TERRA REALIS  -  BIOMES");
        // Legend of the biomes actually present.
        Map<BiomeKind, Integer> counts = new EnumMap<>(BiomeKind.class);
        for (BiomeKind b : f.biome) {
            if (b != null) {
                counts.merge(b, 1, Integer::sum);
            }
        }
        List<Map.Entry<BiomeKind, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        int lx = img.w - 178;
        int ly = 34;
        Shade.panel(img, lx - 6, ly - 14, 182, Math.min(sorted.size(), 18) * 9 + 22);
        img.text(lx, ly - 10, "BIOME COVERAGE", 0xFFFFFF, 1);
        for (int i = 0; i < Math.min(sorted.size(), 18); i++) {
            BiomeKind b = sorted.get(i).getKey();
            double pct = 100.0 * sorted.get(i).getValue() / (f.w * (double) f.h);
            img.fillRect(lx, ly + i * 9 + 1, 7, 7, b.rgb);
            img.text(lx + 11, ly + i * 9, String.format(Locale.ROOT, "%-16s %4.1f%%",
                    truncate(b.label, 16), pct), 0xDDDDDD, 1);
        }
        return img;
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }

    private static Img scalarMap(TerraKernel k, Field f, String title, double[] data,
                                 double[] stops, int[] colors, String lo, String mid, String hi,
                                 boolean global) {
        Img img = new Img(f.w, f.h);
        double cell = f.step * MPB;
        double[] shade = Shade.hillshade(f.surfaceY, f.w, f.h, cell, 1.0);
        for (int i = 0; i < f.w * f.h; i++) {
            double v = data[i];
            int base = Shade.ramp(stops, colors, v);
            img.px[i] = Shade.shade(base, 0.72 + 0.42 * shade[i], 1.0);
        }
        annotate(k, f, img, global, "TERRA REALIS  -  " + title);
        Shade.legend(img, img.w - 118, img.h - 150, 14, 110, stops, colors, "", lo, mid, hi);
        return img;
    }

    /** Channels in blue over a grey shaded relief, plus accumulation as brightness. */
    private static Img drainageMap(TerraKernel k, Field f, boolean global) {
        double cell = f.step * MPB;
        double[] shade = Shade.hillshade(f.surfaceY, f.w, f.h, cell, 1.0);
        Img img = new Img(f.w, f.h);
        for (int i = 0; i < f.w * f.h; i++) {
            double land = Interp.clamp(0.16 + 0.34 * shade[i], 0.0, 1.0);
            int base = Shade.lerpColor(0x1B1F26, 0x9AA4B0, land);
            // Arid ground tints warm so the contrast with the blue network is obvious.
            base = Shade.lerpColor(base, 0x8C6A46, Interp.smoothstep(0.35, 0.95, f.aridity[i]));
            double ch = f.channel[i];
            double waterY = f.waterY[i];
            if (!Double.isNaN(waterY) && waterY > f.surfaceY[i]) {
                ch = Math.max(ch, 1.0);
            }
            if (ch > 0.02) {
                double t = Interp.smoothstep(0.02, 0.85, ch);
                base = Shade.lerpColor(base, 0x2E9BD6, t);
                base = Shade.lerpColor(base, 0xE8F6FF, Interp.smoothstep(0.72, 1.0, ch) * 0.55);
            }
            if (f.playa[i] > 0.3) {
                base = Shade.lerpColor(base, 0xE6E2D0, f.playa[i] * 0.8);
            }
            img.px[i] = base;
        }
        annotate(k, f, img, global, "TERRA REALIS  -  DRAINAGE, PLAYAS AND CHANNELS");
        return img;
    }

    private static Img geologyMap(TerraKernel k, Field f, boolean global) {
        double cell = f.step * MPB;
        double[] shade = Shade.hillshade(f.surfaceY, f.w, f.h, cell, 1.0);
        Img img = new Img(f.w, f.h);
        for (int i = 0; i < f.w * f.h; i++) {
            Rock r = f.rock[i];
            int base = r == null ? 0x808080 : r.primary.rgb;
            // Karst gets a cyan cast, volcanics a red cast: the two most useful overlays.
            base = Shade.lerpColor(base, 0x4FD3C0, Interp.smoothstep(0.30, 0.95, f.karst[i]) * 0.75);
            base = Shade.lerpColor(base, 0xE0431E, Interp.smoothstep(0.25, 0.95, f.volcano[i]) * 0.80);
            base = Shade.lerpColor(base, 0xD8C9A0, Interp.smoothstep(0.25, 0.90, f.outcrop[i]) * 0.35);
            img.px[i] = Shade.shade(base, 0.55 + 0.60 * shade[i], 1.0);
        }
        annotate(k, f, img, global, "TERRA REALIS  -  LITHOLOGY, KARST AND VOLCANICS");
        return img;
    }

    /** Where each arid landform is: badlands, fans, dunes, playas, riparian corridors. */
    private static Img aridMap(TerraKernel k, Field f, boolean global) {
        double cell = f.step * MPB;
        double[] shade = Shade.hillshade(f.surfaceY, f.w, f.h, cell, 1.0);
        Img img = new Img(f.w, f.h);
        for (int i = 0; i < f.w * f.h; i++) {
            int base = Shade.lerpColor(0x2A2620, 0x9C8C72, Interp.clamp(0.2 + 0.5 * shade[i], 0, 1));
            base = Shade.lerpColor(base, 0xE3B23C, Interp.smoothstep(0.20, 0.95, f.painted[i]));
            base = Shade.lerpColor(base, 0xD9762B, Interp.smoothstep(0.30, 0.95, f.fan[i]));
            base = Shade.lerpColor(base, 0xF2E4B0, Interp.smoothstep(0.60, 3.20, f.dune[i]));
            base = Shade.lerpColor(base, 0xF4F6F8, Interp.smoothstep(0.30, 0.90, f.playa[i]));
            base = Shade.lerpColor(base, 0x2FA84F, Interp.smoothstep(0.40, 0.95, f.riparian[i]));
            base = Shade.lerpColor(base, 0x2E9BD6, Interp.smoothstep(0.15, 0.80, f.channel[i]));
            img.px[i] = base;
        }
        annotate(k, f, img, global, "TERRA REALIS  -  ARID LANDFORMS");
        int lx = 12;
        int ly = img.h - 96;
        Shade.panel(img, lx - 6, ly - 14, 190, 92);
        String[] labels = {"PAINTED BADLANDS", "ALLUVIAL FAN / BAJADA", "DUNES", "PLAYA / SALT FLAT",
                "RIPARIAN CORRIDOR", "CHANNEL"};
        int[] cols = {0xE3B23C, 0xD9762B, 0xF2E4B0, 0xF4F6F8, 0x2FA84F, 0x2E9BD6};
        img.text(lx, ly - 10, "LEGEND", 0xFFFFFF, 1);
        for (int i = 0; i < labels.length; i++) {
            img.fillRect(lx, ly + i * 12 + 2, 8, 8, cols[i]);
            img.text(lx + 13, ly + i * 12, labels[i], 0xDDDDDD, 1);
        }
        return img;
    }

    // ------------------------------------------------------------- cross-section

    private static Img section(TerraKernel k, int x0, int z0, int x1, int z1, String title,
                               boolean withCaves) {
        int px = 900;
        int py = 430;
        GenParams p = k.params();
        int minY = p.minY;
        int maxY = p.maxY;
        Material[] buf = new Material[maxY - minY + 1];
        Material[][] cols = new Material[px][];
        double[] heights = new double[px];
        Stratigrapher.CaveSampler caves = withCaves ? k.new DirectCaves() : Stratigrapher.NO_CAVES;

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < px; i++) {
            double t = i / (double) (px - 1);
            int bx = (int) Math.round(Interp.lerp(t, x0, x1));
            int bz = (int) Math.round(Interp.lerp(t, z0, z1));
            Column col = new Column();
            k.column(bx, bz, col);
            heights[i] = col.surfaceY;
            Stratigrapher.fill(k, col, bx, bz, buf, caves);
            cols[i] = buf.clone();
        }
        System.out.printf(Locale.ROOT, "  section %s: %d ms%n", title, System.currentTimeMillis() - t0);

        Img img = new Img(px, py + 64);
        img.fill(0x0C1220);
        // Sky gradient.
        int skyTop = 0;
        int groundTop = 64;
        double vRange = maxY - minY;
        for (int y = 0; y < groundTop; y++) {
            img.hline(0, px - 1, y, Shade.lerpColor(0x16233C, 0x2E4C7A, y / (double) groundTop));
        }
        for (int i = 0; i < px; i++) {
            for (int j = 0; j < py; j++) {
                double v = maxY - (j / (double) (py - 1)) * vRange;
                int y = (int) Math.round(v);
                if (y < minY || y > maxY) {
                    continue;
                }
                Material m = cols[i][y - minY];
                if (m == Material.AIR) {
                    continue;
                }
                int rgb = m.rgb;
                double depthFade = Interp.clamp(1.0 - (maxY - y) / (vRange * 2.6), 0.42, 1.0);
                if (m == Material.WATER) {
                    rgb = Shade.lerpColor(0x4FB9C9, 0x0B2A54,
                            Interp.smoothstep(0.0, 40.0, heights[i] < y ? 0 : y - heights[i]));
                } else if (m == Material.LAVA) {
                    rgb = 0xE8541C;
                }
                img.set(i, groundTop + j, Shade.shade(rgb, depthFade, 1.0));
            }
        }
        // Sea level line.
        int seaY = groundTop + (int) Math.round((maxY - p.seaLevel) / vRange * (py - 1));
        for (int i = 0; i < px; i += 6) {
            img.hline(i, Math.min(px - 1, i + 3), seaY, 0x9FD8FF);
        }
        img.text(px - 96, seaY - 10, "SEA LEVEL 63", 0x9FD8FF, 1);
        img.text(8, groundTop - 12, title, 0xFFFFFF, 1);
        double km = Math.hypot(x1 - x0, z1 - z0) * MPB / 1000.0;
        img.text(8, groundTop - 26, String.format(Locale.ROOT,
                "%.1f km long   vertical 1:%.1f   %.1f m per pixel",
                km, (vRange / py) / (Math.hypot(x1 - x0, z1 - z0) / px), MPB * vRange / py), 0xA8B4C4, 1);
        // Y axis labels.
        for (int e = -1500; e <= 3000; e += 500) {
            int yy = groundTop + (int) Math.round((maxY - (p.seaLevel + e / MPB)) / vRange * (py - 1));
            if (yy > groundTop && yy < groundTop + py) {
                img.text(4, yy - 3, String.format(Locale.ROOT, "%5d m", e), 0x8090A8, 1);
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ chrome

    private static void annotate(TerraKernel k, Field f, Img img, boolean global, String title) {
        GenParams p = k.params();
        int wKm = (int) Math.round(f.w * f.step * MPB / 1000.0);
        int hKm = (int) Math.round(f.h * f.step * MPB / 1000.0);
        String sub = String.format(Locale.ROOT,
                "%d x %d km   %d blocks/pixel   seed 0x%X   1 block = %.0f m",
                wKm, hKm, f.step, k.seed(), MPB);
        int tw = Math.max(title.length(), sub.length()) * 6 + 20;
        Shade.panel(img, 6, 6, tw, 30);
        img.text(14, 11, title, 0xFFFFFF, 1);
        img.text(14, 22, sub, 0x9FB0C4, 1);
        // Scale bar.
        int barBlocks = niceBar(wKm * 1000.0 / MPB / f.w * 90.0);
        int barPx = (int) Math.round(barBlocks / (double) f.step);
        int bx = 14;
        int by = img.h - 22;
        Shade.panel(img, 6, by - 12, barPx + 74, 26);
        img.hline(bx, bx + barPx, by + 6, 0xFFFFFF);
        img.vline(bx, by + 2, by + 10, 0xFFFFFF);
        img.vline(bx + barPx, by + 2, by + 10, 0xFFFFFF);
        img.text(bx + barPx + 6, by + 2,
                String.format(Locale.ROOT, "%d m", (int) Math.round(barBlocks * MPB)), 0xFFFFFF, 1);
    }

    private static int niceBar(double blocks) {
        double[] nice = {16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
        for (double n : nice) {
            if (blocks <= n) {
                return (int) n;
            }
        }
        return 8192;
    }

    // ------------------------------------------------------------------- stats

    private static void stats(Field f, Path out, String tag) throws Exception {
        Map<BiomeKind, Integer> counts = new EnumMap<>(BiomeKind.class);
        int land = 0;
        int water = 0;
        double minE = Double.MAX_VALUE;
        double maxE = -Double.MAX_VALUE;
        double sumArid = 0;
        int aridCount = 0;
        int painted = 0;
        int fans = 0;
        int dunes = 0;
        int playas = 0;
        int riparian = 0;
        int lava = 0;
        int karst = 0;
        int channels = 0;
        int glacial = 0;
        for (int i = 0; i < f.w * f.h; i++) {
            counts.merge(f.biome[i], 1, Integer::sum);
            if (Double.isNaN(f.waterY[i])) {
                land++;
            } else {
                water++;
            }
            minE = Math.min(minE, f.elevM[i]);
            maxE = Math.max(maxE, f.elevM[i]);
            sumArid += f.aridity[i];
            if (f.aridity[i] > 0.55) {
                aridCount++;
            }
            if (f.painted[i] > 0.30) {
                painted++;
            }
            if (f.fan[i] > 0.40) {
                fans++;
            }
            if (f.dune[i] > 0.75) {
                dunes++;
            }
            if (f.playa[i] > 0.35) {
                playas++;
            }
            if (f.riparian[i] > 0.45) {
                riparian++;
            }
            if (f.lava[i] > 0.30) {
                lava++;
            }
            if (f.karst[i] > 0.45) {
                karst++;
            }
            if (f.channel[i] > 0.15) {
                channels++;
            }
            if (f.glacier[i] > 0.30) {
                glacial++;
            }
        }
        int n = f.w * f.h;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "# %s field  %dx%d px  %d blocks/px  %d ms  %d column calls%n",
                tag, f.w, f.h, f.step, f.buildMillis, n));
        sb.append(String.format(Locale.ROOT,
                "land %.1f%%  water %.1f%%  elevation %.0f m .. %.0f m  mean aridity %.2f  arid %.1f%%%n",
                100.0 * land / n, 100.0 * water / n, minE, maxE, sumArid / n, 100.0 * aridCount / n));
        sb.append(String.format(Locale.ROOT,
                "painted badlands %.2f%%  fans %.2f%%  dunes %.2f%%  playas %.2f%%  riparian %.2f%%%n",
                100.0 * painted / n, 100.0 * fans / n, 100.0 * dunes / n, 100.0 * playas / n,
                100.0 * riparian / n));
        sb.append(String.format(Locale.ROOT,
                "lava %.2f%%  karst %.2f%%  channels %.2f%%  glacier %.2f%%  biomes %d%n",
                100.0 * lava / n, 100.0 * karst / n, 100.0 * channels / n, 100.0 * glacial / n,
                counts.size()));
        List<Map.Entry<BiomeKind, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        for (Map.Entry<BiomeKind, Integer> e : sorted) {
            sb.append(String.format(Locale.ROOT, "  %-26s %6.2f%%%n", e.getKey().label,
                    100.0 * e.getValue() / n));
        }
        Files.writeString(out.resolve("stats_" + tag + ".txt"), sb.toString(), StandardCharsets.UTF_8);
        System.out.print(sb);
    }

    /** Finds the most photogenic place for each landform class. */
    static final class Scout {
        private final TerraKernel k;
        int desertX;
        int desertZ;
        int volcanoX;
        int volcanoZ;
        int alpineX;
        int alpineZ;
        int coastX;
        int coastZ;

        Scout(TerraKernel k) {
            this.k = k;
        }

        void run() {
            GenParams p = k.params();
            Volcanism.Edifice e = new Volcanism.Edifice();
            double bestDesert = -1;
            double bestVolcano = -1;
            double bestAlpine = -1;
            double bestCoast = -1;
            int span = 26000;
            int step = 340;
            double[] ring = new double[9];
            for (int z = -span; z <= span; z += step) {
                for (int x = -span; x <= span; x += step) {
                    double cont = k.continentalness(x, z);
                    if (cont < 0.02) {
                        continue;
                    }
                    double arid = k.continents().aridityProxyAt(x, z, cont);
                    double h = k.upliftHeight(x, z);
                    // Local relief: how much does the ground vary over ~600 m?
                    double sum = 0;
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4.0;
                        ring[i] = k.upliftHeight((int) (x + Math.cos(a) * 620), (int) (z + Math.sin(a) * 620));
                        sum += ring[i];
                    }
                    double mean = sum / 8.0;
                    double var = 0;
                    for (int i = 0; i < 8; i++) {
                        var += (ring[i] - mean) * (ring[i] - mean);
                    }
                    double relief = Math.sqrt(var / 8.0);

                    // Desert: arid, on land, with real relief (basin and range), not a volcano.
                    k.upliftHeight(x, z, e);
                    double ds = arid * arid * (0.35 + Interp.smoothstep(6.0, 42.0, relief)) * (1.0 - e.fresh);
                    if (ds > bestDesert) {
                        bestDesert = ds;
                        desertX = x;
                        desertZ = z;
                    }
                    if (e.fresh > bestVolcano) {
                        bestVolcano = e.fresh;
                        volcanoX = x;
                        volcanoZ = z;
                    }
                    double elevM = (h - p.seaLevel) * p.metersPerBlock;
                    if (elevM > bestAlpine) {
                        bestAlpine = elevM;
                        alpineX = x;
                        alpineZ = z;
                    }
                    // Coast: near sea level and near the continental edge.
                    double cs = (1.0 - Interp.smoothstep(0.0, 40.0, Math.abs(elevM)))
                            * Interp.smoothstep(0.30, 0.02, Math.abs(cont));
                    if (cs > bestCoast) {
                        bestCoast = cs;
                        coastX = x;
                        coastZ = z;
                    }
                }
            }
            if (bestVolcano <= 0) {
                volcanoX = desertX + 3000;
                volcanoZ = desertZ - 2000;
            }
            // Refine the alpine pick downward from the analytic maximum.
            alpineZ += 400;
        }
    }
}
