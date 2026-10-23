package pl.crimeandlaw.gang;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import javax.imageio.ImageIO;
import com.google.gson.*;
import pl.crimeandlaw.client.*;
import pl.crimeandlaw.gang.GangDirectory.Permission;

class GangBannerTest {
    @Test void wideCropClampsAtEverySourceEdgeWithoutStretching(){
        for(int[] size:new int[][]{{80,640},{640,80},{256,256}}){
            var crop=new LogoCrop(size[0],size[1],4);crop.zoom(8);crop.pan(1e8,-1e8);
            assertEquals(4,crop.side()/crop.cropHeight(),1e-9);
            assertTrue(crop.x()>=0&&crop.y()>=0&&crop.x()+crop.side()<=size[0]+1e-8&&crop.y()+crop.cropHeight()<=size[1]+1e-8);
        }
    }
    @Test void bannerFitPreservesSquareImageWithTransparentMargins()throws Exception{
        var image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();g.setColor(java.awt.Color.GREEN);g.fillRect(0,0,64,64);g.dispose();
        var crop=new LogoCrop(64,64,4);crop.fit(true);
        var result=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(LogoImages.encode(image,crop,true))));
        assertEquals(GangBanner.WIDTH,result.getWidth());assertEquals(GangBanner.HEIGHT,result.getHeight());assertEquals(0,result.getRGB(0,result.getHeight()/2)>>>24);assertEquals(0xFF00FF00,result.getRGB(result.getWidth()/2,result.getHeight()/2));
    }
    @Test void bannerAndLogoHaveIndependentDimensionValidation()throws Exception{
        String wide=Base64.getEncoder().encodeToString(LogoImages.png(new BufferedImage(4096,1024,BufferedImage.TYPE_INT_ARGB)));
        assertFalse(GangBanner.validateImage(wide).isEmpty());assertThrows(IllegalArgumentException.class,()->GangLogo.validate(wide));
        String tall=Base64.getEncoder().encodeToString(LogoImages.png(new BufferedImage(512,GangBanner.HEIGHT+1,BufferedImage.TYPE_INT_ARGB)));
        assertThrows(IllegalArgumentException.class,()->GangBanner.validateImage(tall));
        assertThrows(IllegalArgumentException.class,()->GangBanner.validateImage("not base64"));
    }
    @Test void onlyAppearancePermissionAllowsBannerChangesAndBadImagesAreAtomic(){
        var d=new GangDirectory();var owner=new UUID(17,1);var member=new UUID(17,2);var gang=d.create(owner,"Owner","Banner Test","");d.request(member,"Member",gang.id);d.accept(owner,member);
        assertThrows(IllegalArgumentException.class,()->d.banner(member,new GangBanner("",0x123456,200)));
        d.role(owner,"member","Członek",Set.of(Permission.MANAGE_THEME));d.banner(member,new GangBanner("",0x123456,200));
        assertEquals(0x123456,gang.banner.color());assertThrows(IllegalArgumentException.class,()->d.banner(member,new GangBanner("bad",0,255)));assertEquals(0x123456,gang.banner.color());
    }
    @Test void oldSaveMigratesWithoutInventingJoinDatesOrChangingLogo(){
        var d=new GangDirectory();var owner=new UUID(17,1);var gang=d.create(owner,"Owner","Old Save","");
        var json=JsonParser.parseString(d.toJson()).getAsJsonObject();var saved=json.getAsJsonObject("gangs").getAsJsonObject(gang.id.toString());saved.remove("banner");saved.remove("history");
        var member=saved.getAsJsonObject("members").getAsJsonObject(owner.toString());member.remove("joinedAt");member.remove("textures");member.remove("textureSignature");
        var restored=GangDirectory.fromJson(json.toString()).of(owner);assertEquals(GangBanner.DEFAULT,restored.banner);assertEquals(0,restored.members.get(owner).joinedAt);assertEquals("",restored.members.get(owner).textures);assertEquals("",restored.logo);
    }
    @Test void memberJoinDateProfileAndBannerSurviveStorage(){
        var d=new GangDirectory();var owner=new UUID(17,1);var g=d.create(owner,"Owner","Saved Profile","");assertTrue(g.members.get(owner).joinedAt>0);
        assertTrue(d.rememberProfile(owner,"Renamed","public texture data","signature"));d.banner(owner,new GangBanner("",0xB33566,180));
        var restored=GangDirectory.fromJson(d.toJson()).of(owner);assertEquals(g.banner,restored.banner);assertEquals(g.members.get(owner).joinedAt,restored.members.get(owner).joinedAt);assertEquals("public texture data",restored.members.get(owner).textures);
    }
    @Test void onlineTotalsDoNotSendTheWholeGangOrOtherPlayers(){
        var d=new GangDirectory();var owner=new UUID(17,1);var gang=d.create(owner,"Owner","Large Gang","");var online=new HashSet<UUID>();online.add(owner);
        for(int i=2;i<=30;i++){var id=new UUID(17,i);d.request(id,"Member"+i,gang.id);d.accept(owner,id);online.add(id);}online.add(new UUID(123,123));
        var result=GangView.snapshot(gang,owner,0,0,0,online);assertEquals(30,result.get("onlineCount").getAsInt());assertEquals(6,result.getAsJsonArray("onlineMemberIds").size());assertEquals(6,result.getAsJsonObject("gang").getAsJsonObject("members").size());
    }
}
