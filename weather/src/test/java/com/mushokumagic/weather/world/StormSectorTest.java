package com.mushokumagic.weather.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StormSectorTest {
    @Test
    void cumulonimbusSectorCoversExactlyTwentyByTwentyChunks() {
        StormSector sector = StormSector.centeredAt(0.0, 0.0);

        assertEquals(StormSector.CUMULONIMBUS_WIDTH_CHUNKS, sector.widthChunks());
        assertTrue(sector.contains(0.0, 0.0));
        assertTrue(sector.containsChunk(sector.minChunkX(), sector.minChunkZ()));
        assertTrue(sector.containsChunk(sector.minChunkX() + 19, sector.minChunkZ() + 19));
        assertFalse(sector.containsChunk(sector.minChunkX() + 20, sector.minChunkZ()));
        assertFalse(sector.containsChunk(sector.minChunkX(), sector.minChunkZ() + 20));
        assertFalse(sector.contains(sector.minChunkX() * 16.0 - 0.01, 0.0));
    }

    @Test
    void sectorAlignmentHandlesNegativeWorldCoordinates() {
        StormSector sector = StormSector.centeredAt(-0.5, -16.0);

        assertTrue(sector.contains(-0.5, -16.0));
        assertTrue(sector.containsChunk(-1, -1));
        assertFalse(sector.contains(-176.01, -16.0));
        assertFalse(sector.contains(-0.5, 144.0));
    }
}
