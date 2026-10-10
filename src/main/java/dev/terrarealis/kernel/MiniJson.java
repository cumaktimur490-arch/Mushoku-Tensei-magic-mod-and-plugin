package dev.terrarealis.kernel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A ~200 line dependency-free JSON reader/writer.
 *
 * <p>The kernel must not depend on Gson (or anything else from Minecraft) so that it can be unit
 * tested and rendered headless. Gson is fine inside the Fabric layer, but the config object lives in
 * the kernel, so it gets a tiny parser of its own. Numbers are always {@link Double}; that is enough
 * for a tuning file.
 */
public final class MiniJson {

    private final String s;
    private int i;

    private MiniJson(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        MiniJson p = new MiniJson(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) {
            throw new IllegalArgumentException("trailing content at " + p.i);
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object o = parse(text);
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("expected a JSON object");
        }
        return (Map<String, Object>) o;
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                i++;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') {
                while (i < s.length() && s.charAt(i) != '\n') {
                    i++;
                }
            } else {
                break;
            }
        }
    }

    private Object value() {
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': expect("true"); return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null"); return null;
            default: return number();
        }
    }

    private void expect(String lit) {
        if (!s.startsWith(lit, i)) {
            throw new IllegalArgumentException("expected '" + lit + "' at " + i);
        }
        i += lit.length();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; // {
        ws();
        if (s.charAt(i) == '}') {
            i++;
            return m;
        }
        while (true) {
            ws();
            String k = string();
            ws();
            if (s.charAt(i) != ':') {
                throw new IllegalArgumentException("expected ':' at " + i);
            }
            i++;
            ws();
            m.put(k, value());
            ws();
            char c = s.charAt(i++);
            if (c == '}') {
                return m;
            }
            if (c != ',') {
                throw new IllegalArgumentException("expected ',' or '}' at " + (i - 1));
            }
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++; // [
        ws();
        if (s.charAt(i) == ']') {
            i++;
            return l;
        }
        while (true) {
            ws();
            l.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ']') {
                return l;
            }
            if (c != ',') {
                throw new IllegalArgumentException("expected ',' or ']' at " + (i - 1));
            }
        }
    }

    private String string() {
        if (s.charAt(i) != '"') {
            throw new IllegalArgumentException("expected '\"' at " + i);
        }
        i++;
        StringBuilder b = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') {
                return b.toString();
            }
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': b.append('\n'); break;
                    case 't': b.append('\t'); break;
                    case 'r': b.append('\r'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                    default: b.append(e);
                }
            } else {
                b.append(c);
            }
        }
    }

    private Double number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        return Double.parseDouble(s.substring(start, i));
    }

    // ---------------------------------------------------------------- writing

    public static String write(Object value) {
        StringBuilder b = new StringBuilder();
        writeValue(b, value, 0);
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder b, Object v, int depth) {
        if (v == null) {
            b.append("null");
        } else if (v instanceof String) {
            writeString(b, (String) v);
        } else if (v instanceof Boolean) {
            b.append(v);
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                b.append((long) d);
            } else {
                b.append(d);
            }
        } else if (v instanceof Number) {
            b.append(v);
        } else if (v instanceof Map) {
            b.append("{\n");
            Map<String, Object> m = (Map<String, Object>) v;
            int n = 0;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                pad(b, depth + 1);
                writeString(b, e.getKey());
                b.append(": ");
                writeValue(b, e.getValue(), depth + 1);
                if (++n < m.size()) {
                    b.append(',');
                }
                b.append('\n');
            }
            pad(b, depth);
            b.append('}');
        } else if (v instanceof List) {
            List<Object> l = (List<Object>) v;
            if (l.isEmpty()) {
                b.append("[]");
                return;
            }
            b.append("[\n");
            for (int k = 0; k < l.size(); k++) {
                pad(b, depth + 1);
                writeValue(b, l.get(k), depth + 1);
                if (k + 1 < l.size()) {
                    b.append(',');
                }
                b.append('\n');
            }
            pad(b, depth);
            b.append(']');
        } else {
            writeString(b, String.valueOf(v));
        }
    }

    private static void pad(StringBuilder b, int depth) {
        for (int i = 0; i < depth; i++) {
            b.append("  ");
        }
    }

    private static void writeString(StringBuilder b, String s) {
        b.append('"');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        b.append('"');
    }

    // --------------------------------------------------------------- accessors

    public static double getDouble(Map<String, Object> m, String key, double fallback) {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : fallback;
    }

    public static int getInt(Map<String, Object> m, String key, int fallback) {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).intValue() : fallback;
    }

    public static boolean getBool(Map<String, Object> m, String key, boolean fallback) {
        Object v = m.get(key);
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    public static String getString(Map<String, Object> m, String key, String fallback) {
        Object v = m.get(key);
        return v instanceof String ? (String) v : fallback;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getObject(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }
}
