package pl.crimeandlaw.territory;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TerrainViewportTest {
    @Test void panningUsesChunkCoordinatesIncludingNegativePositions(){
        assertEquals(new TerrainViewport(-2,7),new TerrainViewport(1,3).move(-3,4));
        assertTrue(new TerrainViewport(-5,5).near(0,0,5));
        assertFalse(new TerrainViewport(-6,5).near(0,0,5));
    }
    @Test void panningClampsAtBothWorldEdgesWithoutOverflow(){
        assertEquals(new TerrainViewport(TerrainViewport.LIMIT,-TerrainViewport.LIMIT),
            new TerrainViewport(0,0).move(Long.MAX_VALUE,Long.MIN_VALUE));
        assertEquals(new TerrainViewport(TerrainViewport.LIMIT,TerrainViewport.LIMIT),
            new TerrainViewport(TerrainViewport.LIMIT,TerrainViewport.LIMIT).move(1,1));
    }
    @Test void untrustedCentersOutsideTheWorldAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new TerrainViewport(Integer.MIN_VALUE,0));
        assertThrows(IllegalArgumentException.class,()->new TerrainViewport(0,TerrainViewport.LIMIT+1));
    }
}
