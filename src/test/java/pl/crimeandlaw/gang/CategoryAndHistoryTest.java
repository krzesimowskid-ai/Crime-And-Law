package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CategoryAndHistoryTest {
    @Test void onlyTransportIsDisabledAndCannotBeOpened(){
        assertEquals(List.of(GangCategory.GANG,GangCategory.MEMBERS,GangCategory.ROLES,GangCategory.REGISTER,GangCategory.TERRITORY,GangCategory.BONUSES,GangCategory.BALANCE,GangCategory.BLACKLIST,GangCategory.GOALS),GangCategory.navigation());
        assertEquals(List.of(GangCategory.TRANSPORT),Arrays.stream(GangCategory.values()).filter(c->!c.enabled()).toList());
        assertEquals(GangCategory.GANG,GangCategory.visibleOrOverview(GangCategory.TRANSPORT.tabId()));
        assertEquals(GangCategory.REGISTER,GangCategory.visibleOrOverview(4));
        assertEquals(GangCategory.GANG,GangCategory.visibleOrOverview(Integer.MAX_VALUE));
    }
    @Test void recordsOnlySuccessfulPublicEventsAndPreservesThemInSave(){
        UUID leader=UUID.randomUUID(),member=UUID.randomUUID(),applicant=UUID.randomUUID();var d=new GangDirectory();var g=d.create(leader,"Lider","Testowy Gang","");
        d.request(member,"Członek",g.id);d.accept(leader,member);int count=g.history.size();
        d.request(applicant,"TajnyKandydat",g.id);d.reject(leader,applicant);
        assertThrows(IllegalArgumentException.class,()->d.kick(member,leader));assertEquals(count,g.history.size());
        assertTrue(g.history.stream().noneMatch(e->e.text().contains("TajnyKandydat")));
        d.transfer(leader,member);d.leave(leader);
        var restored=GangDirectory.fromJson(d.toJson());assertEquals(g.history,restored.of(member).history);
        assertTrue(g.history.getLast().text().contains("opuszcza"));
    }
    @Test void migratesOldSaveAndBoundsAndPagesHistory(){
        UUID owner=UUID.randomUUID();var d=new GangDirectory();var g=d.create(owner,"Lider","Testowy Gang","");
        JsonObject old=JsonParser.parseString(d.toJson()).getAsJsonObject();old.getAsJsonObject("gangs").getAsJsonObject(g.id.toString()).remove("history");
        assertTrue(GangDirectory.fromJson(old.toString()).of(owner).history.isEmpty());
        UUID member=UUID.randomUUID();
        for(int i=0;i<5015;i++){d.request(member,"Gracz"+i,g.id);d.accept(owner,member);d.leave(member);}
        assertEquals(10000,g.history.size());JsonObject view=GangView.snapshot(g,owner,0,0,0);
        assertEquals(100,view.getAsJsonArray("history").size());assertEquals(100,view.get("historyPages").getAsInt());
        assertEquals(g.history.getLast().text(),view.getAsJsonArray("history").get(0).getAsJsonObject().get("text").getAsString());
        assertEquals(99,GangView.snapshot(g,owner,0,0,Integer.MAX_VALUE).get("historyPage").getAsInt());
    }
    @Test void configurationDoesNotFillTheRegisterButMemberActionsStillDo(){
        UUID owner=UUID.randomUUID(),member=UUID.randomUUID();var d=new GangDirectory();var g=d.create(owner,"Lider","Testowy Gang","");
        int initial=g.history.size();
        d.theme(owner,new GangDirectory.Theme(123,456,200));d.logo(owner,"new-logo");
        d.role(owner,"","Oficer",Set.of(GangDirectory.Permission.INVITE));
        String role=g.roles.values().stream().filter(r->r.name.equals("Oficer")).findFirst().orElseThrow().id;
        d.role(owner,role,"Rekruter",Set.of(GangDirectory.Permission.INVITE));
        d.reorderRoles(owner,List.of(role,"member","leader"));
        assertEquals(initial,g.history.size());
        d.request(member,"Gracz",g.id);d.accept(owner,member);d.assign(owner,member,role);d.kick(owner,member);
        assertEquals(initial+3,g.history.size());
        assertTrue(g.history.get(initial).text().contains("dołącza"));
        assertTrue(g.history.get(initial+1).text().contains("zmienia rangę gracza"));
        assertTrue(g.history.getLast().text().contains("usuwa gracza"));
        assertEquals(g.history,GangDirectory.fromJson(d.toJson()).of(owner).history);
    }
}
