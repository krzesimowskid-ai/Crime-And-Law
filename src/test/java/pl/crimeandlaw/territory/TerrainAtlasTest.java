package pl.crimeandlaw.territory;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.territory.TerrainProfile.*;

class TerrainAtlasTest {
    private TerrainProfile profile(TerrainKey key,Surface surface,boolean forest,List<String> templates){
        List<Sample> samples=new ArrayList<>();for(int i=0;i<16;i++)samples.add(new Sample(64,surface,forest,"minecraft:plains"));
        return TerrainProfile.analyse(key,samples,!templates.isEmpty(),templates,100);
    }
    @Test void physicalLandscapeAndGeneratedBuildingsHaveIndependentTraits(){
        var p=profile(new TerrainKey("minecraft:overworld",0,0),Surface.SOIL,true,List.of("minecraft:village/plains/houses/plains_weaponsmith_1"));
        assertEquals(Set.of(Trait.CULTIVABLE,Trait.WOODED,Trait.VILLAGE,Trait.WORKSHOP),p.traits());
        var farm=profile(p.key(),Surface.SOIL,false,List.of("minecraft:village/taiga/houses/taiga_large_farm_1"));
        assertTrue(farm.traits().contains(Trait.FARM));assertFalse(farm.traits().contains(Trait.WORKSHOP));
        var bare=profile(p.key(),Surface.ROCK,false,List.of());assertEquals(Set.of(Trait.ROCKY),bare.traits());
        // An inventory/workstation item name is not a generated village template.
        assertTrue(TerrainTemplates.traits("minecraft:smithing_table").isEmpty());
    }
    @Test void descriptionDistinguishesReliefAndWaterWithoutAwardingBonuses(){
        List<Sample> s=new ArrayList<>();for(int i=0;i<16;i++)s.add(new Sample(50+i*2,i==0?Surface.WATER:Surface.SOIL,false,"minecraft:meadow"));
        var p=TerrainProfile.analyse(new TerrainKey("minecraft:overworld",1,1),s,false,List.of(),100);
        assertTrue(p.traits().containsAll(Set.of(Trait.WATER,Trait.HILLY)));assertFalse(p.traits().contains(Trait.CULTIVABLE));
    }
    @Test void firstObservationRemainsPermanentAcrossEditsSaveReloadAndOwnershipChanges(){
        var atlas=new TerrainAtlas();var key=new TerrainKey("minecraft:overworld",2,-3);
        var original=profile(key,Surface.SOIL,true,List.of("minecraft:village/plains/houses/plains_small_farm_1"));
        assertTrue(atlas.remember(original));assertFalse(atlas.remember(profile(key,Surface.ROCK,false,List.of())));
        UUID gang=UUID.randomUUID(),other=UUID.randomUUID();atlas.setOwner(key,gang);
        var restored=TerrainAtlas.fromJson(atlas.toJson());assertEquals(original,restored.get(key));
        restored.setOwner(key,other);assertEquals(original,restored.get(key));assertEquals(other,restored.owner(key));
        assertFalse(restored.remember(profile(key,Surface.WATER,false,List.of())));assertEquals(original,restored.get(key));
    }
    @Test void ownedTraitsRequireExactOwnerChunkAndDimensionForTheEntireColumn(){
        var atlas=new TerrainAtlas();UUID gang=UUID.randomUUID();var key=TerrainKey.at("minecraft:overworld",-1,-17);
        assertEquals(new TerrainKey("minecraft:overworld",-1,-2),key);
        atlas.remember(profile(key,Surface.SOIL,false,List.of()));atlas.setOwner(key,gang);
        assertTrue(atlas.traitsAt(key,gang).contains(Trait.CULTIVABLE));
        assertTrue(atlas.traitsAt(key,UUID.randomUUID()).isEmpty());assertTrue(atlas.traitsAt(key,null).isEmpty());
        assertTrue(atlas.traitsAt(new TerrainKey("minecraft:the_nether",-1,-2),gang).isEmpty());
        assertTrue(atlas.traitsAt(new TerrainKey("minecraft:overworld",0,-2),gang).isEmpty());
        atlas.setOwner(key,null);assertTrue(atlas.traitsAt(key,gang).isEmpty());
    }
    @Test void evenAFragmentOfAVillageBuildingCountsInEachIntersectedChunk(){
        assertTrue(TerrainTemplates.intersectsChunk(15,-2,18,5,0,0));
        assertTrue(TerrainTemplates.intersectsChunk(15,-2,18,5,1,0));
        assertTrue(TerrainTemplates.intersectsChunk(15,-2,18,5,0,-1));
        assertFalse(TerrainTemplates.intersectsChunk(15,-2,18,5,-1,0));
        assertFalse(TerrainTemplates.intersectsChunk(15,-2,18,5,2,0));
    }
    @Test void summaryIsBoundedAndDistinguishesWaitingFromUnloaded(){
        var atlas=new TerrainAtlas();var key=new TerrainKey("minecraft:overworld",0,0);atlas.remember(profile(key,Surface.SOIL,false,List.of()));
        var view=TerrainView.snapshot(atlas,key.dimension(),0,0,k->k.x()==1,id->"Gang");
        assertEquals(121,view.cells().size());
        assertEquals(TerrainView.Status.READY,view.cells().stream().filter(c->c.x()==0&&c.z()==0).findFirst().orElseThrow().status());
        assertEquals(TerrainView.Status.PENDING,view.cells().stream().filter(c->c.x()==1&&c.z()==0).findFirst().orElseThrow().status());
        assertEquals(TerrainView.Status.UNAVAILABLE,view.cells().stream().filter(c->c.x()==2&&c.z()==0).findFirst().orElseThrow().status());
        String json=new Gson().toJson(view);assertTrue(json.length()<TerrainView.MAX_JSON);assertFalse(json.contains("villagePieces"));
    }
    @Test void rejectsCorruptPersistenceAndKeepsCollectionsImmutable(){
        var atlas=new TerrainAtlas();var p=profile(new TerrainKey("minecraft:overworld",0,0),Surface.SOIL,false,List.of());atlas.remember(p);
        assertThrows(UnsupportedOperationException.class,()->p.traits().add(Trait.FARM));
        assertThrows(UnsupportedOperationException.class,()->p.surfaces().put(Surface.ROCK,16));
        JsonObject broken=JsonParser.parseString(atlas.toJson()).getAsJsonObject();
        var values=broken.getAsJsonObject("profiles");var original=values.remove(p.key().storageKey());values.add("minecraft:overworld|1,0",original);
        assertThrows(IllegalArgumentException.class,()->TerrainAtlas.fromJson(broken.toString()));
        assertThrows(IllegalArgumentException.class,()->atlas.setOwner(new TerrainKey("minecraft:overworld",1,1),UUID.randomUUID()));
    }
    @Test void mapAtTheWorldBorderKeepsItsBoundedShapeWithoutInvalidChunkKeys(){
        var view=TerrainView.snapshot(new TerrainAtlas(),"minecraft:overworld",1_875_000,-1_875_000,k->true,id->"Gang");
        assertEquals(121,view.cells().size());
        assertTrue(view.cells().stream().filter(c->c.x()>1_875_000||c.z()< -1_875_000).allMatch(c->c.status()==TerrainView.Status.UNAVAILABLE));
    }
}
