package com.mushokumagic.world;

/** A square, chunk-aligned region used for localized Cumulonimbus weather. */
public record StormSector(int minChunkX, int minChunkZ, int widthChunks) {
    public static final int CUMULONIMBUS_WIDTH_CHUNKS = 20;
    private static final int BLOCKS_PER_CHUNK = 16;

    public StormSector {
        if (widthChunks <= 0) {
            throw new IllegalArgumentException("A storm sector must contain at least one chunk");
        }
    }

    public static StormSector centeredAt(double x, double z) {
        int blockX = (int)Math.floor(x);
        int blockZ = (int)Math.floor(z);
        int centerChunkX = Math.floorDiv(blockX, BLOCKS_PER_CHUNK);
        int centerChunkZ = Math.floorDiv(blockZ, BLOCKS_PER_CHUNK);
        int halfWidth = CUMULONIMBUS_WIDTH_CHUNKS / 2;
        return new StormSector(centerChunkX - halfWidth, centerChunkZ - halfWidth, CUMULONIMBUS_WIDTH_CHUNKS);
    }

    public boolean contains(double x, double z) {
        double minX = minChunkX * (double)BLOCKS_PER_CHUNK;
        double minZ = minChunkZ * (double)BLOCKS_PER_CHUNK;
        double maxX = (minChunkX + widthChunks) * (double)BLOCKS_PER_CHUNK;
        double maxZ = (minChunkZ + widthChunks) * (double)BLOCKS_PER_CHUNK;
        return x >= minX && x < maxX && z >= minZ && z < maxZ;
    }

    public boolean containsChunk(int chunkX, int chunkZ) {
        return chunkX >= minChunkX
                && chunkX < minChunkX + widthChunks
                && chunkZ >= minChunkZ
                && chunkZ < minChunkZ + widthChunks;
    }
}
