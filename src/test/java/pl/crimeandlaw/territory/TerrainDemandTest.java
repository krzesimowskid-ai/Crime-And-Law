package pl.crimeandlaw.territory;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrainDemandTest {
    @Test void fastPanningReplacesObsoleteWorkButRetainsAnotherPlayersVisibleArea(){
        var demand=new TerrainDemand();UUID first=new UUID(24,1),second=new UUID(24,2);
        demand.update(second,"minecraft:overworld",new TerrainViewport(0,0),new TerrainViewport(0,0),10,1);
        for(int i=1;i<=10000;i++)demand.update(first,"minecraft:overworld",new TerrainViewport(i,0),new TerrainViewport(i+3,0),10,2);
        assertEquals(2,demand.size());assertEquals(0,demand.distance(new TerrainKey("minecraft:overworld",0,0)));
        assertEquals(Integer.MAX_VALUE,demand.distance(new TerrainKey("minecraft:overworld",5000,0)));
        assertEquals(13,demand.distance(new TerrainKey("minecraft:overworld",10013,0)));
        assertEquals(Integer.MAX_VALUE,demand.distance(new TerrainKey("minecraft:the_nether",10000,0)));
    }
    @Test void viewerFloodsAndIdleViewsExpireWithoutGrowingBookkeeping(){
        var demand=new TerrainDemand();var point=new TerrainViewport(0,0);
        for(int i=0;i<1000;i++)demand.update(new UUID(24,i),"minecraft:overworld",point,point,10,0);
        assertEquals(TerrainDemand.MAX_VIEWERS,demand.size());demand.expire(TerrainDemand.TTL+1);assertEquals(0,demand.size());
        assertEquals(Integer.MAX_VALUE,demand.distance(new TerrainKey("minecraft:overworld",0,0)));
    }
}
