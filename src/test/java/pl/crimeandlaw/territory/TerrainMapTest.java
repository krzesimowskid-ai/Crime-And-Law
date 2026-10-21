package pl.crimeandlaw.territory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrainMapTest {
    @Test void all256SurfacePixelsSurviveEncodingAndCannotBeModifiedThroughAnArray(){
        byte[] colors=new byte[256];for(int i=0;i<256;i++)colors[i]=(byte)i;
        var tile=TerrainMapTile.of(colors);assertEquals(344,tile.pixels().length());assertArrayEquals(colors,tile.colors());
        colors[19]=0;assertEquals(19,tile.colors()[19]);var decoded=tile.colors();decoded[19]=0;assertEquals(19,tile.colors()[19]);
    }
    @Test void invalidOrPartialMapTilesAreRejected(){
        assertThrows(IllegalArgumentException.class,()->TerrainMapTile.of(new byte[255]));
        assertThrows(IllegalArgumentException.class,()->new TerrainMapTile(""));
        assertThrows(IllegalArgumentException.class,()->new TerrainMapTile("?".repeat(344)));
        assertThrows(IllegalArgumentException.class,()->new TerrainMapTile(Base64.getEncoder().encodeToString(new byte[257])));
    }
    @Test void mapIsIndependentOfOriginalFeaturesAndDoesNotInventBonuses(){
        var key=new TerrainKey("minecraft:overworld",-2,3);byte[] colors=new byte[256];colors[23]=17;
        var tile=TerrainMapTile.of(colors);
        var view=TerrainView.snapshot(new TerrainAtlas(),key.dimension(),-2,3,k->true,id->"Gang",k->k.equals(key)?tile:null).withPosition(-17.25,53.75,91);
        var cell=view.cells().stream().filter(c->c.x()==-2&&c.z()==3).findFirst().orElseThrow();
        assertEquals(TerrainView.Status.PENDING,cell.status());assertEquals(tile.pixels(),cell.mapPixels());assertTrue(cell.traits().isEmpty());
        assertEquals(-17.25,view.playerX());assertEquals(53.75,view.playerZ());assertEquals(91,view.yaw());
        assertEquals(1,view.cells().stream().filter(c->!c.mapPixels().isEmpty()).count());
    }
    @Test void mapPixelsPersistByDimensionAndChunkAndCanRefreshWithoutChangingOriginalProfile(){
        var key=new TerrainKey("minecraft:overworld",-1,-2);var other=new TerrainKey("minecraft:the_nether",-1,-2);
        var sample=new TerrainProfile.Sample(64,TerrainProfile.Surface.SOIL,false,"minecraft:plains");
        var profile=TerrainProfile.analyse(key,Collections.nCopies(16,sample),false,List.of(),100);
        var atlas=new TerrainAtlas();atlas.remember(profile);CompoundTag legacy=new CompoundTag();
        legacy.putByteArray("atlas",atlas.toJson().getBytes(StandardCharsets.UTF_8));legacy.putInt("version",1);
        var saved=TerrainSavedData.load(legacy,null);assertNull(saved.map(key));
        byte[] colors=new byte[256];Arrays.fill(colors,(byte)17);var before=TerrainMapTile.of(colors);
        saved.rememberMap(key,before);colors[23]=81;var after=TerrainMapTile.of(colors);saved.rememberMap(key,after);saved.rememberMap(other,before);
        var loaded=TerrainSavedData.load(saved.save(new CompoundTag(),null),null);
        assertEquals(after,loaded.map(key));assertEquals(before,loaded.map(other));assertEquals(profile,loaded.atlas.get(key));assertEquals(1,loaded.atlas.size());
    }
}
