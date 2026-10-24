package pl.crimeandlaw.territory;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class TerrainMarkersTest {
    private final UUID owner=new UUID(25,1),member=new UUID(25,2),outsider=new UUID(25,3),gang=new UUID(25,9),other=new UUID(25,10);
    private final TerrainKey key=new TerrainKey("minecraft:overworld",2,3);
    private final TerrainViewport view=new TerrainViewport(2,3);
    @Test void privateAndGangNotesCoexistWithoutLeakingToOtherPlayersOrGangs(){
        var marks=new TerrainMarkers();marks.set(key,owner,gang,false,0xFF112233);marks.set(key,owner,gang,true,0x445566);
        assertEquals(2,marks.visible(owner,gang,key.dimension(),view).size());
        assertEquals(1,marks.visible(member,gang,key.dimension(),view).size());
        assertTrue(marks.visible(outsider,other,key.dimension(),view).isEmpty());
        assertTrue(marks.visible(owner,gang,"minecraft:the_nether",view).isEmpty());
        assertTrue(marks.visible(owner,gang,key.dimension(),new TerrainViewport(200,3)).isEmpty());
        assertEquals(0x112233,marks.visible(owner,null,key.dimension(),view).getFirst().color());
        var restored=TerrainMarkers.fromJson(marks.toJson());assertEquals(marks.visible(owner,gang,key.dimension(),view),restored.visible(owner,gang,key.dimension(),view));
        restored.remove(key,member,gang,false);assertEquals(2,restored.visible(owner,gang,key.dimension(),view).size());
        restored.remove(key,member,gang,true);assertEquals(1,restored.visible(owner,gang,key.dimension(),view).size());
    }
    @Test void notesAreBoundedButAnExistingNoteCanBeRecoloredAtTheLimit(){
        var marks=new TerrainMarkers();for(int i=0;i<TerrainMarkers.PER_SCOPE;i++)marks.set(new TerrainKey(key.dimension(),i,3),owner,gang,false,i);
        assertThrows(IllegalArgumentException.class,()->marks.set(new TerrainKey(key.dimension(),500,3),owner,gang,false,0));
        marks.set(key,owner,gang,false,0xABCDEF);assertEquals(0xABCDEF,marks.visible(owner,gang,key.dimension(),view).stream().filter(m->m.key().equals(key)).findFirst().orElseThrow().color());
        assertThrows(IllegalArgumentException.class,()->marks.set(key,owner,null,true,0));
    }
}
