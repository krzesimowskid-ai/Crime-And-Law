package pl.crimeandlaw.gang;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.gang.GangDirectory.*;

class GangDirectoryTest {
    private final UUID leader=UUID.randomUUID(),member=UUID.randomUUID(),stranger=UUID.randomUUID();
    private GangDirectory setup(){var d=new GangDirectory();d.create(leader,"Lider","Testowy Gang","");return d;}
    @Test void preventsDuplicateMembershipAndNames(){
        var d=setup();assertThrows(IllegalArgumentException.class,()->d.create(leader,"Lider","Drugi Gang",""));
        assertThrows(IllegalArgumentException.class,()->d.create(member,"Gracz","testowy gang",""));
    }
    @Test void joiningRequiresApprovalAndRejectsForgedOwnerActions(){
        var d=setup();d.request(member,"Gracz",d.of(leader).id);assertNull(d.of(member));
        assertThrows(IllegalArgumentException.class,()->d.accept(stranger,member));
        d.accept(leader,member);assertSame(d.of(leader),d.of(member));
        assertThrows(IllegalArgumentException.class,()->d.theme(member,Theme.DEFAULT));
        assertThrows(IllegalArgumentException.class,()->d.role(member,"","Szef",EnumSet.allOf(Permission.class)));
    }
    @Test void leaderCannotLeaveGroupOrBeKickedUntilTransfer(){
        var d=setup();d.request(member,"Gracz",d.of(leader).id);d.accept(leader,member);
        assertThrows(IllegalArgumentException.class,()->d.leave(leader));
        assertThrows(IllegalArgumentException.class,()->d.kick(leader,leader));
        assertThrows(IllegalArgumentException.class,()->d.assign(leader,member,"leader"));
        d.transfer(leader,member);d.leave(leader);assertNull(d.of(leader));assertEquals(member,d.of(member).owner);
    }
    @Test void assignedRolesAreEnforced(){
        var d=setup();d.request(member,"Gracz",d.of(leader).id);d.accept(leader,member);
        d.role(leader,"","Rekruter",Set.of(Permission.INVITE));
        Role role=d.of(leader).roles.values().stream().filter(r->r.name.equals("Rekruter")).findFirst().orElseThrow();
        d.assign(leader,member,role.id);d.request(stranger,"Nowy",d.of(leader).id);d.accept(member,stranger);
        assertNotNull(d.of(stranger));assertThrows(IllegalArgumentException.class,()->d.kick(member,stranger));
    }
    @Test void applicationCannotJoinTwoGangsAndIsClearedOnAcceptance(){
        var d=setup();d.create(stranger,"Inny","Drugi Gang","");
        d.request(member,"Gracz",d.of(leader).id);d.request(member,"Gracz",d.of(stranger).id);
        d.accept(leader,member);assertFalse(d.of(stranger).requests.containsKey(member));
        assertThrows(IllegalArgumentException.class,()->d.request(member,"Gracz",d.of(stranger).id));
    }
    @Test void survivesJsonRoundTripIncludingPermissionsAndTheme(){
        var d=setup();d.theme(leader,new Theme(0x123456,0xABCDEF,105));d.request(member,"Gracz",d.of(leader).id);
        var restored=GangDirectory.fromJson(d.toJson());
        assertEquals(d.toJson(),restored.toJson());restored.accept(leader,member);
        assertTrue(restored.allowed(restored.of(leader),leader,Permission.MANAGE_THEME));
        assertEquals(105,restored.of(leader).theme.opacity());
    }
    @Test void lastMemberLeavingDeletesGang(){var d=setup();d.leave(leader);assertTrue(d.gangs().isEmpty());}
    @Test void themeClampsOpacityAndMasksRgb(){
        Theme t=new Theme(0xFF112233,0xFF445566,999);assertEquals(255,t.opacity());assertEquals(0x112233,t.background());
        assertEquals(0,new Theme(0,0,-10).opacity());
    }
    @Test void rejectsNamesThatCouldBreakLayout(){
        var d=new GangDirectory();assertThrows(IllegalArgumentException.class,()->d.create(leader,"Lider","§kHidden",""));
        assertThrows(IllegalArgumentException.class,()->d.create(leader,"Lider","a".repeat(33),""));
    }
    @Test void independentWorldsHaveIndependentGangs(){
        var a=setup();var b=new GangDirectory();assertNull(b.of(leader));assertEquals(1,a.gangs().size());
    }
    @Test void visualRoleOrderCanPutUnprivilegedRoleAboveLeaderAndSurvivesSave(){
        var d=setup();d.role(leader,"","Nowa Ranga",Set.of());var g=d.of(leader);
        String custom=g.roles.values().stream().filter(r->r.name.equals("Nowa Ranga")).findFirst().orElseThrow().id;
        d.reorderRoles(leader,List.of(custom,"member","leader"));
        assertEquals(List.of(custom,"member","leader"),new ArrayList<>(g.roles.keySet()));
        d.request(member,"Gracz",g.id);d.accept(leader,member);d.assign(leader,member,custom);
        assertFalse(d.allowed(g,member,Permission.MANAGE_ROLES));assertEquals(leader,g.owner);
        var restored=GangDirectory.fromJson(d.toJson());assertEquals(List.of(custom,"member","leader"),new ArrayList<>(restored.of(leader).roles.keySet()));
    }
    @Test void roleReorderingChecksPermissionAndExactPermutation(){
        var d=setup();var g=d.of(leader);d.request(member,"Gracz",g.id);d.accept(leader,member);
        assertThrows(IllegalArgumentException.class,()->d.reorderRoles(member,List.of("member","leader")));
        for(var invalid:List.of(List.of("member"),List.of("member","member"),List.of("member","unknown")))assertThrows(IllegalArgumentException.class,()->d.reorderRoles(leader,invalid));
        assertEquals(List.of("leader","member"),new ArrayList<>(g.roles.keySet()));
    }
}
