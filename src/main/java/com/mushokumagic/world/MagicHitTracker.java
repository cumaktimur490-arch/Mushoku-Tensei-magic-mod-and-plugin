/*
 * Decompiled with CFR.
 */
package com.mushokumagic.world;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1309;
import net.minecraft.class_3222;

public final class MagicHitTracker {
    private static final Map<UUID, Hit> HITS = new HashMap();

    private MagicHitTracker() {
    }

    public static void mark(class_1309 entity, class_3222 player, long tick) {
        HITS.put((Object)entity.method_5667(), (Object)new Hit(player.method_5667(), tick));
    }

    public static Hit get(class_1309 entity) {
        return (Hit)((Object)HITS.get((Object)entity.method_5667()));
    }

    public static void clear(class_1309 entity) {
        HITS.remove((Object)entity.method_5667());
    }

    public static void prune(long now) {
        Iterator iterator = HITS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - ((Hit)((Object)((Map.Entry)iterator.next()).getValue())).tick() <= 600L) continue;
            iterator.remove();
        }
    }

    public record Hit(UUID player, long tick) {
    }
}
