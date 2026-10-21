package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.gang.GangDirectory.*;

class GangViewTest {
    private final UUID owner=new UUID(1,0),viewer=new UUID(1,43);
    private Gang largeGang(){
        Gang g=new Gang();g.id=UUID.randomUUID();g.owner=owner;g.name="Duży Gang";g.logo="A".repeat(27000);
        for(int i=0;i<10000;i++)g.members.put(new UUID(1,i),new Member(new UUID(1,i),"Member"+i));
        for(int i=0;i<5000;i++)g.requests.put(new UUID(2,i),"Applicant"+i);
        return g;
    }
    @Test void networkSnapshotIsBoundedAndShowsTrueMemberCount(){
        Gang gang=largeGang();gang.logo="A".repeat(GangLogo.MAX_ENCODED_LENGTH);
        for(int i=0;i<100;i++)gang.history.add(new Event(i,"W".repeat(256)));
        JsonObject view=GangView.snapshot(gang,owner,0,0);
        assertEquals(10000,view.get("memberCount").getAsInt());assertEquals(1667,view.get("memberPages").getAsInt());
        assertEquals(6,view.getAsJsonArray("memberIds").size());assertEquals(6,view.getAsJsonObject("gang").getAsJsonObject("requests").size());
        assertTrue(view.toString().length()<pl.crimeandlaw.network.GangNetwork.STATE_MAX_JSON,"Maximum logo, membership and history page fit the state packet");
    }
    @Test void everyMemberAppearsOnceAcrossPagesAndOwnerAndViewerRemainAvailable(){
        Gang g=largeGang();Set<String> seen=new HashSet<>();
        for(int p=0;p<1667;p++){
            JsonObject view=GangView.snapshot(g,viewer,p,0),members=view.getAsJsonObject("gang").getAsJsonObject("members");
            assertTrue(members.has(owner.toString()));assertTrue(members.has(viewer.toString()));assertTrue(members.size()<=8);
            for(JsonElement id:view.getAsJsonArray("memberIds"))assertTrue(seen.add(id.getAsString()));
        }
        assertEquals(10000,seen.size());
    }
    @Test void applicationsAreVisibleOnlyToLeader(){
        JsonObject view=GangView.snapshot(largeGang(),viewer,0,0);
        assertFalse(view.getAsJsonObject("gang").has("requests"));assertFalse(view.has("requestCount"));
        Gang decoded=new Gson().fromJson(view.get("gang"),Gang.class);assertTrue(decoded.requests.isEmpty());
    }
    @Test void clampsOutdatedPagesAfterMembershipOrApplicationsChange(){
        Gang g=largeGang();JsonObject high=GangView.snapshot(g,owner,Integer.MAX_VALUE,Integer.MAX_VALUE);
        assertEquals(1666,high.get("memberPage").getAsInt());assertEquals(833,high.get("requestPage").getAsInt());
        g.members.clear();g.members.put(owner,new Member(owner,"Leader"));g.requests.clear();
        JsonObject small=GangView.snapshot(g,owner,1666,833);
        assertEquals(0,small.get("memberPage").getAsInt());assertEquals(0,small.get("requestPage").getAsInt());
        assertEquals(1,small.get("memberPages").getAsInt());assertEquals(1,small.get("requestPages").getAsInt());
        assertEquals(0,GangView.snapshot(g,owner,-5,-5).get("memberPage").getAsInt());
    }
    @Test void outsidersCannotReadMemberSnapshots(){
        assertThrows(IllegalArgumentException.class,()->GangView.snapshot(largeGang(),new UUID(3,0),0,0));
    }
}
