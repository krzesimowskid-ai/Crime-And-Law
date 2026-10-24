package pl.crimeandlaw.territory;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CaptureBoardTest {
    private final UUID a=new UUID(25,1),b=new UUID(25,2),c=new UUID(25,3),p=new UUID(25,11),q=new UUID(25,12),r=new UUID(25,13);
    private final TerrainKey key=new TerrainKey("minecraft:overworld",-2,3);
    private final Set<UUID> gangs=Set.of(a,b,c);
    private final CaptureRules rules=new CaptureRules("UTC",true,0,24,5,2,2,9,3,60,120,1);
    private final long start=Instant.parse("2026-10-24T12:00:00Z").toEpochMilli();
    private long now=start;
    private CaptureBoard board(){var board=new CaptureBoard();gangs.forEach(board::unlock);return board;}
    private Map<UUID,CaptureBoard.Person> people(UUID... ids){
        var result=new HashMap<UUID,CaptureBoard.Person>();for(var id:ids)result.put(id,new CaptureBoard.Person(id.equals(p)?a:id.equals(q)?b:c,true));return result;
    }
    private List<CaptureBoard.Result> seconds(CaptureBoard board,int count,Map<UUID,CaptureBoard.Person> people){
        List<CaptureBoard.Result> results=new ArrayList<>();for(int i=0;i<count;i++){now+=1000;results.addAll(board.tick(now,1000,people,gangs));}return results;
    }
    @Test void neutralCaptureRequiresActualPresenceAndPreservesPartialProgress(){
        var board=board();board.start(key,a,null,p,rules,now);
        assertFalse(board.recover(now),"A fresh board must never cancel a newly started event");
        seconds(board,2,people(p));seconds(board,2,Map.of());assertEquals(2000,board.at(key).progress);
        assertTrue(seconds(board,2,people(p)).isEmpty());var results=seconds(board,1,people(p));
        assertEquals(a,results.getFirst().winner());assertNull(board.at(key));
        assertEquals(60_000,board.cooldown(a,now));assertEquals(120_000,board.protection(key,now));
    }
    @Test void leavingAndLoggingOutNeverAwardLand(){
        var board=board();board.start(key,a,null,p,rules,now);
        var result=seconds(board,3,Map.of()).getFirst();assertNull(result.winner());assertEquals(0,board.protection(key,now));
        assertTrue(board.cooldown(a,now)>0);
    }
    @Test void aRivalSwitchesToPreparationThenCombatInsteadOfFreeClaim(){
        var board=board();board.start(key,a,null,p,rules,now);seconds(board,4,people(p));board.join(key,b,q,now);
        assertEquals(CaptureBoard.Phase.PREPARING,board.at(key).phase);
        seconds(board,2,people(p,q));assertEquals(CaptureBoard.Phase.FIGHTING,board.at(key).phase);
        seconds(board,3,people(p,q));assertNotNull(board.at(key));
        board.eliminate(q);assertTrue(seconds(board,1,people(p)).isEmpty());
        assertEquals(a,seconds(board,1,people(p)).getFirst().winner());
        assertTrue(board.cooldown(b,now)>0);
    }
    @Test void defenderHasTheWholePreparationPeriodToJoin(){
        var longer=new CaptureRules("UTC",true,0,24,5,6,2,9,3,60,120,1);
        var board=board();board.start(key,a,b,p,longer,now);seconds(board,5,people(p));board.join(key,b,q,now);
        assertFalse(board.at(key).removedTeams.contains(b));seconds(board,1,people(p,q));
        board.eliminate(p);assertEquals(b,seconds(board,2,people(q)).getFirst().winner());
    }
    @Test void aFightCanEndInADrawAndRetainsItsPreviousOwner(){
        var board=board();board.start(key,a,b,p,rules,now);board.join(key,b,q,now);seconds(board,2,people(p,q));
        var result=seconds(board,9,people(p,q)).getFirst();assertNull(result.winner());assertEquals(b,result.previous());
    }
    @Test void threeGangsAndADeadPlayerCannotBypassElimination(){
        var board=board();board.start(key,a,null,p,rules,now);board.join(key,b,q,now);board.join(key,c,r,now);
        board.eliminate(q);assertThrows(IllegalArgumentException.class,()->board.join(key,b,q,now));
        seconds(board,2,people(p,q,r));board.eliminate(r);assertEquals(a,seconds(board,2,people(p)).getFirst().winner());
    }
    @Test void changingGangOrDimensionDoesNotCountAsPresence(){
        var board=board();board.start(key,a,null,p,rules,now);
        var changed=Map.of(p,new CaptureBoard.Person(b,true));assertNull(seconds(board,3,changed).getFirst().winner());
        board=board();board.start(key,a,null,p,rules,now);
        assertNull(seconds(board,3,Map.of(p,new CaptureBoard.Person(a,false))).getFirst().winner());
    }
    @Test void aSurvivorCanWinEvenWhenTheStartingMinimumWasTwo(){
        var two=new CaptureRules("UTC",true,0,24,5,2,2,9,3,60,120,2);
        var board=board();board.start(key,a,b,p,two,now);board.join(key,b,q,now);
        seconds(board,2,people(p,q));board.eliminate(q);assertEquals(a,seconds(board,2,people(p)).getFirst().winner());
    }
    @Test void hugeElapsedTimeAndOfflineRestartNeverSkipTheCapture(){
        var board=board();board.start(key,a,null,p,rules,now);
        assertTrue(board.tick(now+86400000,86400000,people(p),gangs).isEmpty());assertEquals(1000,board.at(key).progress);
        var restored=CaptureBoard.fromJson(board.toJson());assertTrue(restored.recover(now));assertNull(restored.at(key));
        assertTrue(restored.unlocked(a));assertEquals(60_000,restored.cooldown(a,now));assertFalse(restored.recover(now+1));
        var again=CaptureBoard.fromJson(restored.toJson());assertFalse(again.recover(now+2));assertEquals(59_998,again.cooldown(a,now+2));
    }
    @Test void immunityCooldownBusyGangAndLateEntriesAreEnforced(){
        var board=board();board.start(key,a,null,p,rules,now);
        assertThrows(IllegalArgumentException.class,()->board.start(new TerrainKey(key.dimension(),-1,3),a,null,q,rules,now));
        assertThrows(IllegalArgumentException.class,()->board.start(new TerrainKey(key.dimension(),-1,3),b,null,p,rules,now));
        seconds(board,5,people(p));
        assertThrows(IllegalArgumentException.class,()->board.start(key,b,a,q,rules,now));
        assertThrows(IllegalArgumentException.class,()->board.start(new TerrainKey(key.dimension(),-1,3),a,null,p,rules,now));
        now+=121_000;board.start(key,b,a,q,rules,now);board.join(key,a,p,now);seconds(board,2,people(p,q));
        assertThrows(IllegalArgumentException.class,()->board.join(key,c,r,now));
    }
    @Test void unfinishedSaveRejectsCorruptParticipantsInsteadOfSilentlyAwardingLand(){
        var board=board();board.start(key,a,null,p,rules,now);
        var json=JsonParser.parseString(board.toJson()).getAsJsonObject();
        json.getAsJsonObject("active").getAsJsonObject(key.storageKey()).add("fighters",null);
        assertThrows(IllegalArgumentException.class,()->CaptureBoard.fromJson(json.toString()));
    }
    @Test void windowAndClockUseTheConfiguredTimezoneIncludingDaylightSaving(){
        var warsaw=rules.zone("Europe/Warsaw").change("otwarcie",16).change("zamkniecie",22);
        assertFalse(warsaw.open(Instant.parse("2026-10-24T13:59:59Z")));
        assertTrue(warsaw.open(Instant.parse("2026-10-24T14:00:00Z")));
        assertFalse(warsaw.open(Instant.parse("2026-10-24T20:00:00Z")));
        assertFalse(warsaw.open(Instant.parse("2026-10-25T14:59:59Z")));
        assertTrue(warsaw.open(Instant.parse("2026-10-25T15:00:00Z")));
        var night=rules.change("otwarcie",22).change("zamkniecie",4);
        assertTrue(night.open(Instant.parse("2026-10-24T23:00:00Z")));assertTrue(night.open(Instant.parse("2026-10-25T03:00:00Z")));
        assertFalse(night.open(Instant.parse("2026-10-25T04:00:00Z")));
    }
    @Test void sunUsesTwentyFourHoursAndCrossesMidnightContinuously(){
        assertEquals(0,Math.floorMod(rules.minecraftTime(Instant.parse("2026-10-24T06:00:00Z")),24000));
        assertEquals(6000,Math.floorMod(rules.minecraftTime(Instant.parse("2026-10-24T12:00:00Z")),24000));
        assertEquals(18000,Math.floorMod(rules.minecraftTime(Instant.parse("2026-10-24T00:00:00Z")),24000));
        assertEquals(24000,rules.minecraftTime(Instant.parse("2026-10-25T12:00:00Z"))-rules.minecraftTime(Instant.parse("2026-10-24T12:00:00Z")));
        assertEquals(1,rules.minecraftTime(Instant.parse("2026-10-25T00:00:01Z"))-rules.minecraftTime(Instant.parse("2026-10-24T23:59:59Z")));
        assertThrows(IllegalArgumentException.class,()->rules.change("utrzymanie",10));
        assertThrows(IllegalArgumentException.class,()->rules.change("nieistniejacy",1));
        assertThrows(DateTimeException.class,()->rules.zone("Nieistniejaca/Strefa"));
    }
}
