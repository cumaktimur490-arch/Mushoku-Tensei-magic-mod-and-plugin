/*
 * Decompiled with CFR.
 */
package com.mushokumagic.world;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3218;
import net.minecraft.class_5321;
import net.minecraft.server.MinecraftServer;

public final class TemporaryBlocks {
    private static final Map<Key, Entry> BLOCKS = new HashMap();

    private TemporaryBlocks() {
    }

    public static boolean place(class_3218 level, class_2338 pos, class_2680 state, int lifetimeTicks) {
        class_2338 immutable = pos.method_10062();
        class_2680 previous = level.method_8320(immutable);
        if (!previous.method_45474()) {
            return false;
        }
        Key key = new Key((class_5321<class_1937>)level.method_27983(), immutable);
        if (BLOCKS.containsKey(key)) {
            return false;
        }
        BLOCKS.put(key, new Entry(previous, state, level.method_75260() + (long)Math.max((int)1, (int)lifetimeTicks)));
        level.method_8652(immutable, state, 2);
        return true;
    }

    public static void tick(class_3218 level) {
        if (BLOCKS.isEmpty()) {
            return;
        }
        long now = level.method_75260();
        Iterator iterator = BLOCKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            if (!((Key)(entry.getKey())).dimension().equals(level.method_27983()) || now < ((Entry)(entry.getValue())).expireTick()) continue;
            TemporaryBlocks.restore(level, ((Key)(entry.getKey())).pos(), (Entry)(entry.getValue()));
            iterator.remove();
        }
    }

    private static void restore(class_3218 level, class_2338 pos, Entry entry) {
        if (level.method_8320(pos).equals(entry.placed())) {
            level.method_8652(pos, entry.previous(), 2);
        }
    }

    public static void restoreAll(MinecraftServer server) {
        for (Map.Entry entry : BLOCKS.entrySet()) {
            class_3218 level = server.method_3847(((Key)(entry.getKey())).dimension());
            if (level == null) continue;
            TemporaryBlocks.restore(level, ((Key)(entry.getKey())).pos(), (Entry)(entry.getValue()));
        }
        BLOCKS.clear();
    }

    private record Key(class_5321<class_1937> dimension, class_2338 pos) {
    }

    private record Entry(class_2680 previous, class_2680 placed, long expireTick) {
    }
}
