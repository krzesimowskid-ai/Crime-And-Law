package pl.crimeandlaw.territory;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.*;

class TerrainViewCacheTest {
    private TerrainView view(String dimension,int x,int z,boolean mapped){
        byte[] colors=new byte[256];Arrays.fill(colors,(byte)45);var tile=TerrainMapTile.of(colors);
        return TerrainView.snapshot(new TerrainAtlas(),dimension,x,z,key->false,id->"",key->mapped?tile:null);
    }
    @Test void panningOutAndBackRetainsEveryKnownPixelBeforeAnyServerReply(){
        var cache=new TerrainViewCache();var initial=cache.remember(view("minecraft:overworld",0,0,true));
        var shifted=cache.viewport(initial,new TerrainViewport(-4,2));
        assertTrue(shifted.cells().stream().anyMatch(c->c.mapPixels().isEmpty()),"Unknown borders remain unknown");
        var back=cache.viewport(shifted,new TerrainViewport(0,0));
        assertEquals(initial.cells(),back.cells());assertEquals(121,cache.size());
        var partial=cache.remember(view("minecraft:overworld",0,0,false));
        assertEquals(initial.cells(),partial.cells(),"An incomplete refresh must not erase known tiles");
    }
    @Test void cacheIsBoundedAndDimensionsNeverShareTiles(){
        var cache=new TerrainViewCache();for(int x=0;x<200;x+=8)cache.remember(view("minecraft:overworld",x,0,true));
        assertEquals(TerrainViewCache.LIMIT,cache.size());
        var nether=cache.remember(view("minecraft:the_nether",0,0,false));assertEquals(0,cache.size());
        assertTrue(nether.cells().stream().allMatch(c->c.mapPixels().isEmpty()));
    }
    @Test void worldCornersDoNotCreateInvalidCacheKeys(){
        var cache=new TerrainViewCache();int edge=TerrainViewport.LIMIT;
        for(int x:new int[]{-edge,edge})for(int z:new int[]{-edge,edge}){
            var corner=cache.remember(view("minecraft:overworld",x,z,true));
            var result=cache.viewport(corner,new TerrainViewport(x,z));assertEquals(121,result.cells().size());
        }
    }
    @Test void neighboringRepliesFillTheCurrentViewAndAllReversalsKeepKnownTiles(){
        var cache=new TerrainViewCache();var initial=cache.remember(view("minecraft:overworld",0,0,true));
        for(int z:new int[]{-6,0,6})for(int x:new int[]{-6,0,6})cache.remember(view("minecraft:overworld",x,z,true));
        for(int[] point:new int[][]{{4,3},{-3,-4},{5,-2},{-5,4},{0,0}}){
            var camera=cache.viewport(initial,new TerrainViewport(point[0],point[1]),TerrainView.BUFFER_RADIUS);
            assertTrue(camera.cells().stream().filter(c->Math.abs(c.x()-point[0])<=5&&Math.abs(c.z()-point[1])<=5).allMatch(c->!c.mapPixels().isEmpty()));
            cache.remember(view("minecraft:overworld",point[0],point[1],false));
        }
    }
}
