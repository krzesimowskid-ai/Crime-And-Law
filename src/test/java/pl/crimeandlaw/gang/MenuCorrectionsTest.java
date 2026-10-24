package pl.crimeandlaw.gang;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import javax.imageio.ImageIO;
import pl.crimeandlaw.client.*;
import pl.crimeandlaw.gang.GangDirectory.*;

class MenuCorrectionsTest {
    private final UUID owner=new UUID(18,1),member=new UUID(18,2);
    @Test void zoomOutCanShowEveryCharacterAndMoveItInsideTheBanner()throws Exception{
        BufferedImage image=new BufferedImage(1800,900,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();g.setColor(java.awt.Color.GREEN);g.fillRect(0,0,1800,900);
        g.setColor(java.awt.Color.RED);g.fillRect(0,0,120,900);g.setColor(java.awt.Color.BLUE);g.fillRect(1680,0,120,900);g.dispose();
        var crop=new LogoCrop(1800,900,530/123.0);crop.fit(true);
        assertTrue(crop.zoom()<1);assertTrue(crop.x()<0);
        var saved=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(LogoImages.encode(image,crop,true))));
        assertEquals(2048,saved.getWidth());assertEquals(475,saved.getHeight());
        boolean red=false,blue=false;for(int x=0;x<saved.getWidth();x++){int rgb=saved.getRGB(x,saved.getHeight()/2);red|=rgb==0xFFFF0000;blue|=rgb==0xFF0000FF;}
        assertTrue(red&&blue,"Both extreme characters survive fitting into the actual banner strip");
        crop.zoom(crop.zoom()/2);double before=crop.x();crop.pan(-100,0);assertTrue(crop.x()<before);assertTrue(crop.side()>image.getWidth());
        double zoom=crop.zoom();crop.normalizedZoom(crop.normalizedZoom());assertEquals(zoom,crop.zoom(),1e-9);
    }
    @Test void legacyMembershipDatesAreRecoveredOnlyFromMatchingRecordedEvents(){
        var d=new GangDirectory();var gang=d.create(owner,"Owner","Old Membership","");d.request(member,"Member",gang.id);d.accept(owner,member);
        var json=JsonParser.parseString(d.toJson()).getAsJsonObject();var saved=json.getAsJsonObject("gangs").getAsJsonObject(gang.id.toString());
        saved.getAsJsonObject("members").entrySet().forEach(e->e.getValue().getAsJsonObject().remove("joinedAt"));
        var restored=GangDirectory.fromJson(json.toString());assertTrue(restored.needsMigration());
        assertEquals(gang.history.getFirst().timestamp(),restored.of(owner).members.get(owner).joinedAt);
        assertEquals(gang.history.getLast().timestamp(),restored.of(owner).members.get(member).joinedAt);
        assertTrue(restored.of(owner).members.get(member).joinedAtFromHistory);
        saved.remove("history");restored=GangDirectory.fromJson(json.toString());assertEquals(0,restored.of(owner).members.get(member).joinedAt);
    }
    @Test void oldRankEditorsRetainAssignmentWhileNewPermissionsCanBeSeparate(){
        var d=new GangDirectory();var gang=d.create(owner,"Owner","Old Permissions","");d.request(member,"Member",gang.id);d.accept(owner,member);
        d.role(owner,"member","Członek",Set.of(Permission.MANAGE_ROLES));
        var json=JsonParser.parseString(d.toJson()).getAsJsonObject();json.remove("schemaVersion");
        var restored=GangDirectory.fromJson(json.toString());assertTrue(GangDirectory.allowed(restored.of(member),member,Permission.ASSIGN_ROLES));
        d.role(owner,"member","Członek",Set.of(Permission.ASSIGN_ROLES));
        assertTrue(GangDirectory.allowed(gang,member,Permission.ASSIGN_ROLES));assertFalse(GangDirectory.allowed(gang,member,Permission.MANAGE_ROLES));
        d.assign(member,member,"member");assertThrows(IllegalArgumentException.class,()->d.role(member,"","New Rank",Set.of()));
    }
    @Test void plannedCatalogRightsAreRejectedEvenForTheLeaderWithoutPartialChanges(){
        var d=new GangDirectory();var gang=d.create(owner,"Owner","Planned Rights","");String before=d.toJson();
        assertEquals(23,Permission.values().length);assertEquals(11,Permission.activePermissions().size());
        for(var p:Permission.values())if(!p.available()){
            assertFalse(GangDirectory.allowed(gang,owner,p));
            assertThrows(IllegalArgumentException.class,()->d.role(owner,"member","Członek",Set.of(p)));
            assertEquals(before,d.toJson());
        }
    }
}
