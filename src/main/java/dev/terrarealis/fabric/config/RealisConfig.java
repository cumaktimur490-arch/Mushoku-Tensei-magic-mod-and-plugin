package dev.terrarealis.fabric.config;

import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.MiniJson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * World-generation parameters, loaded from {@code config/terra_realis.json}.
 *
 * <p>The file is written with every default on first load, so a server owner can open it, change
 * {@code ocean_fraction} or turn dunes off, and restart. The whole file round-trips through
 * {@link GenParams#toJson()} / {@link GenParams#fromJson(String)}, and the generator's codec stores a
 * fingerprint of the parameters so a world created with one parameter set can detect a change.
 */
public final class RealisConfig {

    private static volatile GenParams cached;

    private RealisConfig() {}

    public static GenParams get() {
        GenParams p = cached;
        if (p != null) {
            return p;
        }
        synchronized (RealisConfig.class) {
            if (cached != null) {
                return cached;
            }
            cached = load();
            return cached;
        }
    }

    /** Forces a reload; used by the {@code /terra_realis reload} command. */
    public static void invalidate() {
        cached = null;
    }

    private static GenParams load() {
        GenParams p = GenParams.defaults();
        Path file = FabricLoader.getInstance().getConfigDir().resolve("terra_realis.json");
        try {
            if (Files.exists(file)) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                Map<String, Object> root = MiniJson.parseObject(text);
                if (root != null && !root.isEmpty()) {
                    p.apply(root);
                }
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, pretty(p), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[terra_realis] could not read " + file + ": " + e
                    + " - using defaults");
        }
        p.sanitise();
        return p;
    }

    private static String pretty(GenParams p) {
        StringBuilder sb = new StringBuilder("{\n");
        Map<String, Object> m = p.toJson();
        int i = 0;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            sb.append("  \"").append(e.getKey()).append("\": ");
            Object v = e.getValue();
            if (v instanceof String s) {
                sb.append('"').append(s).append('"');
            } else if (v instanceof Boolean || v instanceof Number) {
                sb.append(v);
            } else if (v instanceof Map<?, ?> inner) {
                sb.append("{\n");
                int j = 0;
                for (Map.Entry<?, ?> ie : inner.entrySet()) {
                    sb.append("    \"").append(ie.getKey()).append("\": ");
                    Object iv = ie.getValue();
                    sb.append(iv instanceof String ? "\"" + iv + "\"" : String.valueOf(iv));
                    sb.append(j++ < inner.size() - 1 ? ",\n" : "\n");
                }
                sb.append("  }");
            } else {
                sb.append(String.valueOf(v));
            }
            sb.append(i++ < m.size() - 1 ? ",\n" : "\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
