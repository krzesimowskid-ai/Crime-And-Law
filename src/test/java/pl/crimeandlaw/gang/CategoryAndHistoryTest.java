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
        for(int i=0;i<10030;i++)d.theme(owner,new GangDirectory.Theme(i,0,200));
        assertEquals(10000,g.history.size());JsonObject view=GangView.snapshot(g,owner,0,0,0);
        assertEquals(100,view.getAsJsonArray("history").size());assertEquals(100,view.get("historyPages").getAsInt());
        assertEquals(g.history.getLast().text(),view.getAsJsonArray("history").get(0).getAsJsonObject().get("text").getAsString());
        assertEquals(99,GangView.snapshot(g,owner,0,0,Integer.MAX_VALUE).get("historyPage").getAsInt());
    }
}
