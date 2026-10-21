package pl.crimeandlaw.gang;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.gang.GangDirectory.*;

class GangDirectoryTest {
    private final UUID leader=UUID.randomUUID(),member=UUID.randomUUID(),stranger=UUID.randomUUID();
    private GangDirectory setup(){var d=new GangDirectory();d.create(leader,"Lider","Testowy Gang","");return d;}
    private void join(GangDirectory d,UUID id,String name){d.request(id,name,d.of(leader).id);d.accept(leader,id);}
    @Test void preventsDuplicateMembershipAndNames(){
        var d=setup();assertThrows(IllegalArgumentException.class,()->d.create(leader,"Lider","Drugi Gang",""));
        assertThrows(IllegalArgumentException.class,()->d.create(member,"Gracz","testowy gang",""));
    }
    @Test void joiningRequiresApprovalAndRejectsForgedLeaderActions(){
        var d=setup();d.request(member,"Gracz",d.of(leader).id);assertNull(d.of(member));
        assertThrows(IllegalArgumentException.class,()->d.accept(stranger,member));
        d.accept(leader,member);assertSame(d.of(leader),d.of(member));
        d.request(stranger,"Nowy",d.of(leader).id);
        assertThrows(IllegalArgumentException.class,()->d.accept(member,stranger));
        assertThrows(IllegalArgumentException.class,()->d.reject(member,stranger));
        assertThrows(IllegalArgumentException.class,()->d.theme(member,Theme.DEFAULT));
        assertThrows(IllegalArgumentException.class,()->d.logo(member,""));
        assertThrows(IllegalArgumentException.class,()->d.kick(member,leader));
        assertThrows(IllegalArgumentException.class,()->d.transfer(member,stranger));
    }
    @Test void leaderCannotLeaveGroupOrBeKickedUntilTransfer(){
        var d=setup();join(d,member,"Gracz");
        assertThrows(IllegalArgumentException.class,()->d.leave(leader));
        assertThrows(IllegalArgumentException.class,()->d.kick(leader,leader));
        d.transfer(leader,member);
        assertFalse(d.isLeader(d.of(leader),leader));assertTrue(d.isLeader(d.of(member),member));
        assertThrows(IllegalArgumentException.class,()->d.theme(leader,Theme.DEFAULT));
        d.leave(leader);assertNull(d.of(leader));assertEquals(member,d.of(member).owner);
    }
    @Test void applicationCannotJoinTwoGangsAndIsClearedOnAcceptance(){
        var d=setup();d.create(stranger,"Inny","Drugi Gang","");
        d.request(member,"Gracz",d.of(leader).id);d.request(member,"Gracz",d.of(stranger).id);
        d.accept(leader,member);assertFalse(d.of(stranger).requests.containsKey(member));
        assertThrows(IllegalArgumentException.class,()->d.request(member,"Gracz",d.of(stranger).id));
    }
    @Test void survivesJsonRoundTripIncludingMembersRequestsLogoAndTheme(){
        var d=setup();join(d,member,"Gracz");d.theme(leader,new Theme(0x123456,0xABCDEF,105));
        d.request(stranger,"Nowy",d.of(leader).id);d.logo(leader,"retained-logo");
        var restored=GangDirectory.fromJson(d.toJson());assertEquals(d.toJson(),restored.toJson());
        restored.accept(leader,stranger);assertTrue(restored.isLeader(restored.of(leader),leader));
        assertEquals(105,restored.of(leader).theme.opacity());assertEquals("retained-logo",restored.of(leader).logo);
    }
    @Test void restoresLegacyRanksAndTheirExplicitPermissions(){
        var d=setup();join(d,member,"Gracz");d.request(stranger,"Nowy",d.of(leader).id);
        d.theme(leader,new Theme(0x123456,0xABCDEF,105));d.logo(leader,"old-logo");
        JsonObject old=JsonParser.parseString(d.toJson()).getAsJsonObject();
        JsonObject gang=old.getAsJsonObject("gangs").getAsJsonObject(d.of(leader).id.toString());
        JsonObject roles=new JsonObject(),recruiter=new JsonObject();recruiter.addProperty("id","recruiter");recruiter.addProperty("name","Rekruter");
        JsonArray rights=new JsonArray();rights.add("INVITE");rights.add("MANAGE_THEME");recruiter.add("permissions",rights);roles.add("recruiter",recruiter);gang.add("roles",roles);
        gang.getAsJsonObject("members").getAsJsonObject(member.toString()).addProperty("role","recruiter");
        var restored=GangDirectory.fromJson(old.toString());
        assertEquals("recruiter",restored.of(member).members.get(member).role);
        assertEquals(Set.of(Permission.INVITE,Permission.MANAGE_THEME),restored.of(member).roles.get("recruiter").permissions);
        assertEquals(leader,restored.of(member).owner);assertEquals("old-logo",restored.of(member).logo);
        restored.accept(member,stranger);assertSame(restored.of(leader),restored.of(stranger));
    }
    @Test void gangSizeAndApplicationCountHaveNoGameplayCapAndSurviveSave(){
        var d=setup();UUID id=d.of(leader).id;
        for(int i=1;i<=1000;i++){UUID player=new UUID(1,i);d.request(player,"Player"+i,id);d.accept(leader,player);}
        for(int i=1;i<=2000;i++)d.request(new UUID(2,i),"Applicant"+i,id);
        assertEquals(1001,d.of(leader).members.size());assertEquals(2000,d.of(leader).requests.size());
        var restored=GangDirectory.fromJson(d.toJson());assertEquals(1001,restored.of(leader).members.size());assertEquals(2000,restored.of(leader).requests.size());
    }
    @Test void rejectsCorruptedMembershipInSave(){
        var d=setup();JsonObject json=JsonParser.parseString(d.toJson()).getAsJsonObject();
        JsonObject gang=json.getAsJsonObject("gangs").getAsJsonObject(d.of(leader).id.toString());gang.addProperty("owner",stranger.toString());
        assertThrows(IllegalArgumentException.class,()->GangDirectory.fromJson(json.toString()));
    }
    @Test void lastMemberLeavingDeletesGang(){var d=setup();d.leave(leader);assertTrue(d.gangs().isEmpty());}
    @Test void themeClampsOpacityAndMasksRgb(){
        Theme t=new Theme(0xFF112233,0xFF445566,999);assertEquals(255,t.opacity());assertEquals(0x112233,t.background());
        assertEquals(0,new Theme(0,0,-10).opacity());
    }
    @Test void rejectsNamesThatCouldBreakLayout(){
        for(String name:List.of("ab","x".repeat(33),"<script>","Naz\u0000wa"))
            assertThrows(IllegalArgumentException.class,()->new GangDirectory().create(leader,"Lider",name,""));
    }
}
