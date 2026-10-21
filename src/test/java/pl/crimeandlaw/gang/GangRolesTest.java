package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.gang.GangDirectory.*;

class GangRolesTest {
    private final UUID owner=new UUID(20,1),member=new UUID(20,2),applicant=new UUID(20,3);
    private GangDirectory setup(){
        var d=new GangDirectory();var g=d.create(owner,"Lider","Testowy Gang","");
        d.request(member,"Gracz",g.id);d.accept(owner,member);return d;
    }
    private String add(GangDirectory d,String name,Set<Permission> rights){
        Set<String> before=new HashSet<>(d.of(owner).roles.keySet());d.role(owner,"",name,rights);
        return d.of(owner).roles.keySet().stream().filter(k->!before.contains(k)).findFirst().orElseThrow();
    }
    @Test void createsEditsAssignsAndPersistsCustomRoles(){
        var d=setup();String rank=add(d,"Rekruter",Set.of(Permission.INVITE));d.assign(owner,member,rank);
        d.role(owner,rank,"Oficer",Set.of(Permission.INVITE,Permission.MANAGE_THEME));
        var restored=GangDirectory.fromJson(d.toJson());var g=restored.of(owner);
        assertEquals("Oficer",g.roles.get(rank).name);assertEquals(rank,g.members.get(member).role);
        assertEquals(Set.of(Permission.INVITE,Permission.MANAGE_THEME),g.roles.get(rank).permissions);
        assertTrue(g.history.stream().noneMatch(e->e.text().contains("tworzy rangę Rekruter")));
        assertTrue(g.history.stream().anyMatch(e->e.text().contains("zmienia rangę gracza Gracz")));
    }
    @Test void delegatesOnlyExplicitPermissionsAndProtectsOwnership(){
        var d=setup();String rank=add(d,"Rekruter",Set.of(Permission.INVITE,Permission.MANAGE_THEME));d.assign(owner,member,rank);
        d.request(applicant,"Kandydat",d.of(owner).id);d.accept(member,applicant);d.theme(member,Theme.DEFAULT);d.logo(member,"");
        assertThrows(IllegalArgumentException.class,()->d.kick(member,applicant));
        assertThrows(IllegalArgumentException.class,()->d.role(member,"","Nowa",Set.of()));
        assertThrows(IllegalArgumentException.class,()->d.transfer(member,applicant));
        assertEquals(owner,d.of(member).owner);
    }
    @Test void visualOrderingDoesNotGrantPermissionsEvenAboveLeader(){
        var d=setup();String rank=add(d,"Nowicjusz",Set.of());d.assign(owner,member,rank);
        d.reorderRoles(owner,List.of(rank,"member","leader"));
        assertEquals(List.of(rank,"member","leader"),new ArrayList<>(d.of(owner).roles.keySet()));
        assertFalse(GangDirectory.allowed(d.of(member),member,Permission.MANAGE_ROLES));
        assertThrows(IllegalArgumentException.class,()->d.reorderRoles(member,List.of("leader","member",rank)));
        assertEquals(owner,GangDirectory.fromJson(d.toJson()).of(member).owner);
    }
    @Test void validatesRoleUpdatesAndNeverPartiallyMutatesInvalidRequests(){
        var d=setup();String before=d.toJson();
        assertThrows(IllegalArgumentException.class,()->d.role(member,"","Nowa",Set.of()));
        assertThrows(IllegalArgumentException.class,()->d.role(owner,"leader","Szef",Set.of()));
        assertThrows(IllegalArgumentException.class,()->d.role(owner,"missing","Nowa",Set.of()));
        assertThrows(IllegalArgumentException.class,()->d.role(owner,"","X",Set.of()));
        assertThrows(IllegalArgumentException.class,()->d.role(owner,"","Nowa",null));
        assertThrows(IllegalArgumentException.class,()->d.reorderRoles(owner,List.of("leader","leader")));
        assertThrows(IllegalArgumentException.class,()->d.reorderRoles(owner,List.of("leader")));
        assertEquals(before,d.toJson());
    }
    @Test void protectsLeaderAssignmentAndTransferMovesReservedRole(){
        var d=setup();String rank=add(d,"Oficer",Set.of(Permission.MANAGE_ROLES));d.assign(owner,member,rank);
        assertThrows(IllegalArgumentException.class,()->d.assign(member,owner,"member"));
        assertThrows(IllegalArgumentException.class,()->d.assign(member,member,"leader"));
        assertThrows(IllegalArgumentException.class,()->d.assign(member,applicant,rank));
        assertThrows(IllegalArgumentException.class,()->d.assign(member,member,"missing"));
        d.transfer(owner,member);assertEquals("leader",d.of(member).members.get(member).role);
        assertEquals("member",d.of(member).members.get(owner).role);
        GangDirectory.fromJson(d.toJson());
    }
    @Test void migratesSavesWithoutRolesAndKeepsLogoMembershipAndHistory(){
        var d=setup();d.logo(owner,"retained-logo");var g=d.of(owner);
        JsonObject json=JsonParser.parseString(d.toJson()).getAsJsonObject();
        JsonObject saved=json.getAsJsonObject("gangs").getAsJsonObject(g.id.toString());saved.remove("roles");
        saved.getAsJsonObject("members").entrySet().forEach(e->e.getValue().getAsJsonObject().remove("role"));
        var restored=GangDirectory.fromJson(json.toString()).of(owner);
        assertEquals("leader",restored.members.get(owner).role);assertEquals("member",restored.members.get(member).role);
        assertEquals("retained-logo",restored.logo);assertEquals(g.history,restored.history);assertEquals(2,restored.roles.size());
    }
    @Test void applicationSnapshotsFollowInvitePermissionAndRevokeImmediately(){
        var d=setup();d.request(applicant,"Kandydat",d.of(owner).id);
        String rank=add(d,"Rekruter",Set.of(Permission.INVITE));d.assign(owner,member,rank);
        assertTrue(GangView.snapshot(d.of(owner),member,0,0).has("requestCount"));
        d.role(owner,rank,"Rekruter",Set.of());
        JsonObject snapshot=GangView.snapshot(d.of(owner),member,0,0);
        assertFalse(snapshot.has("requestCount"));assertFalse(snapshot.getAsJsonObject("gang").has("requests"));
        assertTrue(snapshot.getAsJsonObject("gang").getAsJsonObject("roles").has(rank));
    }
}
