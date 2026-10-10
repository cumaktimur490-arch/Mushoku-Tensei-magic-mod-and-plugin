package dev.terrarealis.kernel.region;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU cache of {@link RegionTile}s.
 *
 * <p>A tile costs tens of milliseconds to simulate and covers 64 chunks, so the cache is the difference
 * between a generator that runs at 200 chunks per second and one that runs at three. Minecraft asks for
 * chunks in roughly row-major order with a modest amount of back-tracking, which an LRU handles well.
 *
 * <p>Builds are synchronised per key so two threads that both need the same tile do not both pay for it;
 * the map itself is guarded for all mutations.
 */
public final class TileCache {

    private final RegionTile.Source source;
    private final LinkedHashMap<Long, RegionTile> map;
    private final Object buildLock = new Object();
    private long hits;
    private long misses;

    public TileCache(RegionTile.Source source, int capacity) {
        this.source = source;
        final int cap = Math.max(8, capacity);
        this.map = new LinkedHashMap<>(cap + 1, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, RegionTile> eldest) {
                return size() > cap;
            }
        };
    }

    public RegionTile tile(int tileX, int tileZ) {
        long key = ((long) tileX << 32) ^ (tileZ & 0xFFFFFFFFL);
        synchronized (map) {
            RegionTile t = map.get(key);
            if (t != null) {
                hits++;
                return t;
            }
            misses++;
        }
        RegionTile built;
        synchronized (buildLock) {
            // Another thread may have finished it while we waited for the build lock.
            synchronized (map) {
                RegionTile t = map.get(key);
                if (t != null) {
                    return t;
                }
            }
            built = new RegionTile(source, tileX, tileZ);
            synchronized (map) {
                map.put(key, built);
            }
        }
        return built;
    }

    /** Tile index for a block coordinate. */
    public int tileIndex(int blockCoord) {
        return Math.floorDiv(blockCoord, source.params().tileCells * source.params().erosionCellSize);
    }

    public synchronized long[] stats() {
        return new long[] {hits, misses, map.size()};
    }

    public synchronized void clear() {
        map.clear();
        hits = 0;
        misses = 0;
    }
}
