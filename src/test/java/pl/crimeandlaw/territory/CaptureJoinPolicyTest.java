package pl.crimeandlaw.territory;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import pl.crimeandlaw.gang.GangDirectory;
import pl.crimeandlaw.gang.GangDirectory.Permission;

class CaptureJoinPolicyTest {
    private final UUID owner=new UUID(26,1),rival=new UUID(26,2),member=new UUID(26,3);
    private final TerrainKey key=new TerrainKey("minecraft:overworld",0,0);
    private final GangDirectory directory=new GangDirectory();
    private final GangDirectory.Gang a=directory.create(owner,"Owner","Pierwszy","");
    private final GangDirectory.Gang b=directory.create(rival,"Rival","Sąsiedni","");
    private final CaptureBoard board=new CaptureBoard();
    private final TerrainAtlas atlas=new TerrainAtlas();
    private CaptureBoard.Battle battle(UUID previousOwner){
        board.unlock(a.id);board.unlock(b.id);return board.start(key,a.id,previousOwner,owner,CaptureRules.defaults().change("otwarcie",0).change("zamkniecie",24),0);
    }
    private void land(TerrainKey key,UUID gang){
        atlas.remember(TerrainProfile.analyse(key,List.of(new TerrainProfile.Sample(64,TerrainProfile.Surface.SOIL,false,"minecraft:plains")),false,List.of(),1));atlas.setOwner(key,gang);
    }
    @Test void allEightImmediateNeighboursPermitTheRivalLeader(){
        var battle=battle(null);
        for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
            if(x==0&&z==0)continue;
            var neighbour=new TerrainKey(key.dimension(),x,z);land(neighbour,b.id);
            assertEquals("",CaptureJoinPolicy.problem(battle,b,rival,atlas),"Offset "+x+", "+z);
            assertEquals(Math.abs(x)+Math.abs(z)==1,atlas.neighbours(b.id,key,false));
            atlas.setOwner(neighbour,null);
        }
    }
    @Test void emptyLandDistantLandAndTheSelectedChunkItselfAreNotABorder(){
        var battle=battle(null);assertFalse(CaptureJoinPolicy.problem(battle,b,rival,atlas).isEmpty());
        land(new TerrainKey(key.dimension(),2,0),b.id);land(key,b.id);
        assertFalse(CaptureJoinPolicy.problem(battle,b,rival,atlas).isEmpty());
        assertFalse(atlas.neighbours(b.id,key,true));
    }
    @Test void anotherGangOrDimensionDoesNotQualify(){
        var battle=battle(null);land(new TerrainKey(key.dimension(),1,1),a.id);land(new TerrainKey("minecraft:the_nether",1,1),b.id);
        assertFalse(CaptureJoinPolicy.problem(battle,b,rival,atlas).isEmpty());
    }
    @Test void theRealLeaderDecidesEvenIfAMemberHasParticipationPermission(){
        var battle=battle(null);land(new TerrainKey(key.dimension(),1,1),b.id);
        directory.request(member,"Member",b.id);directory.accept(rival,member);directory.role(rival,"member","Atakujący",Set.of(Permission.JOIN_TERRITORY_ATTACK));
        assertTrue(GangDirectory.allowed(b,member,Permission.JOIN_TERRITORY_ATTACK));
        assertTrue(CaptureJoinPolicy.problem(battle,b,member,atlas).contains("lider"));
        assertEquals("",CaptureJoinPolicy.problem(battle,b,rival,atlas));
    }
    @Test void afterTheLeaderEntersTeammatesCanJoinWithoutRepeatingTheDecision(){
        var battle=battle(null);land(new TerrainKey(key.dimension(),-1,1),b.id);board.join(key,b.id,rival,0);
        assertFalse(CaptureJoinPolicy.challenge(battle,b));assertEquals("",CaptureJoinPolicy.problem(battle,b,member,atlas));
        assertEquals("",CaptureJoinPolicy.problem(battle,a,owner,atlas),"The original attacking team needs no extra border");
    }
    @Test void theOwnerCanStillDefendItsOnlyOccupiedChunk(){
        var battle=battle(b.id);assertEquals("",CaptureJoinPolicy.problem(battle,b,rival,atlas));
        assertFalse(CaptureJoinPolicy.challenge(battle,b));
    }
    @Test void checkingWorldBorderNeighboursDoesNotConstructAnInvalidChunk(){
        for(int sign:new int[]{-1,1}){
            var corner=new TerrainKey(key.dimension(),sign*TerrainViewport.LIMIT,sign*TerrainViewport.LIMIT);
            land(new TerrainKey(key.dimension(),corner.x()-sign,corner.z()-sign),b.id);
            assertTrue(atlas.neighbours(b.id,corner,true));assertFalse(atlas.neighbours(b.id,corner,false));
        }
    }
    @Test void anAdmissionPreviewIsNonMutatingAndSharesTheEngineRejection(){
        battle(null);String before=board.toJson();
        assertEquals("",board.joinProblem(key,b.id,rival,0));assertEquals(before,board.toJson());
        board.join(key,b.id,rival,0);board.eliminate(rival);before=board.toJson();String problem=board.joinProblem(key,b.id,rival,0);
        assertFalse(problem.isEmpty());assertEquals(problem,assertThrows(IllegalArgumentException.class,()->board.join(key,b.id,rival,0)).getMessage());
        assertEquals(before,board.toJson());
    }
}
