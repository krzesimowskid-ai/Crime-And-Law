package pl.crimeandlaw.network;

import com.google.gson.*;
import io.netty.buffer.Unpooled;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import pl.crimeandlaw.client.*;
import pl.crimeandlaw.gang.*;
import static org.junit.jupiter.api.Assertions.*;

class GangNetworkTest {
    @Test void detailedLogoSurvivesActionStorageAndStateCodecs()throws Exception {
        BufferedImage image=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);Random random=new Random(22);
        for(int y=0;y<1024;y++)for(int x=0;x<1024;x++)image.setRGB(x,y,random.nextInt()|0xFF000000);
        String logo=LogoImages.encode(image,new LogoCrop(1024,1024));
        assertTrue(logo.length()>28000,"Exercises a payload exceeding the previous action limit");
        JsonObject action=new JsonObject();action.addProperty("action","create");action.addProperty("name","Żółty Gang");action.addProperty("logo",logo);
        var upload=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        String received;
        try{
            GangNetwork.Action.CODEC.encode(upload,new GangNetwork.Action(action.toString()));
            received=GangNetwork.Action.CODEC.decode(upload).json();assertFalse(upload.isReadable());
        }finally{upload.release();}
        JsonObject parsed=JsonParser.parseString(received).getAsJsonObject();UUID owner=new UUID(19,11);
        GangDirectory directory=new GangDirectory();directory.create(owner,"Lider",parsed.get("name").getAsString(),GangLogo.validate(parsed.get("logo").getAsString()));
        var wide=new BufferedImage(2048,512,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<512;y++)for(int x=0;x<2048;x++)wide.setRGB(x,y,random.nextInt()|0xFF000000);
        String banner=LogoImages.encode(wide,new LogoCrop(2048,512,4),true);
        directory.banner(owner,new GangBanner(banner,0x334455,180));
        GangDirectory restored=GangDirectory.fromJson(directory.toJson());
        assertEquals(logo,restored.of(owner).logo);
        String snapshot=GangView.snapshot(restored.of(owner),owner,0,0).toString();
        var download=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            GangNetwork.State.CODEC.encode(download,new GangNetwork.State(snapshot));
            JsonObject state=JsonParser.parseString(GangNetwork.State.CODEC.decode(download).json()).getAsJsonObject();
            assertFalse(download.isReadable());assertEquals("Żółty Gang",state.getAsJsonObject("gang").get("name").getAsString());
            BufferedImage displayed=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(state.getAsJsonObject("gang").get("logo").getAsString())));
            assertEquals(1024,displayed.getWidth());assertEquals(1024,displayed.getHeight());
            assertEquals(image.getRGB(128,128),displayed.getRGB(128,128));
            var receivedBanner=state.getAsJsonObject("gang").getAsJsonObject("banner");
            assertEquals(banner,receivedBanner.get("image").getAsString());assertEquals(180,receivedBanner.get("opacity").getAsInt());
            assertTrue(snapshot.length()>7_000_000,"Both detailed assets share the same bounded state packet");
        }finally{download.release();}
    }
    @Test void packetCodecsStillRejectOverBudgetStrings(){
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            assertThrows(RuntimeException.class,()->GangNetwork.Action.CODEC.encode(buffer,new GangNetwork.Action("a".repeat(GangNetwork.ACTION_MAX_JSON+1))));
            buffer.clear();
            assertThrows(RuntimeException.class,()->GangNetwork.State.CODEC.encode(buffer,new GangNetwork.State("a".repeat(GangNetwork.STATE_MAX_JSON+1))));
            buffer.clear();
            assertThrows(RuntimeException.class,()->GangNetwork.Terrain.CODEC.encode(buffer,new GangNetwork.Terrain("a".repeat(pl.crimeandlaw.territory.TerrainView.MAX_JSON+1))));
        }finally{buffer.release();}
    }
    @Test void terrainSummaryUsesItsOwnCodecWithoutSendingLogoData(){
        var atlas=new pl.crimeandlaw.territory.TerrainAtlas();
        var key=new pl.crimeandlaw.territory.TerrainKey("minecraft:overworld",-2,3);
        var sample=new pl.crimeandlaw.territory.TerrainProfile.Sample(64,pl.crimeandlaw.territory.TerrainProfile.Surface.SOIL,false,"minecraft:plains");
        atlas.remember(pl.crimeandlaw.territory.TerrainProfile.analyse(key,Collections.nCopies(16,sample),false,List.of(),10));
        var view=pl.crimeandlaw.territory.TerrainView.snapshot(atlas,key.dimension(),key.x(),key.z(),k->true,id->"Gang");
        String json=new Gson().toJson(view);var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            GangNetwork.Terrain.CODEC.encode(buffer,new GangNetwork.Terrain(json));
            var decoded=new Gson().fromJson(GangNetwork.Terrain.CODEC.decode(buffer).json(),pl.crimeandlaw.territory.TerrainView.class);
            assertEquals(view,decoded);assertFalse(buffer.isReadable());assertFalse(json.contains("logo"));
        }finally{buffer.release();}
    }
    @Test void fullSurfaceRasterWithAllTraitsAndOwnersFitsTerrainCodec(){
        var atlas=new pl.crimeandlaw.territory.TerrainAtlas();UUID owner=UUID.randomUUID();byte[] pixels=new byte[256];new Random(42).nextBytes(pixels);
        var tile=pl.crimeandlaw.territory.TerrainMapTile.of(pixels);
        for(int z=-5;z<=5;z++)for(int x=-5;x<=5;x++){
            var key=new pl.crimeandlaw.territory.TerrainKey("minecraft:overworld",x,z);
            atlas.remember(new pl.crimeandlaw.territory.TerrainProfile(key,1,16,1,256,Map.of(pl.crimeandlaw.territory.TerrainProfile.Surface.SOIL,16),16,List.of("minecraft:plains"),List.of(),EnumSet.allOf(pl.crimeandlaw.territory.TerrainProfile.Trait.class),pl.crimeandlaw.territory.TerrainProfile.Source.GENERATED_SNAPSHOT,1));atlas.setOwner(key,owner);
        }
        var view=pl.crimeandlaw.territory.TerrainView.snapshot(atlas,"minecraft:overworld",0,0,k->true,id->"Ż".repeat(32),key->tile);
        String json=new Gson().toJson(view);assertTrue(json.length()<pl.crimeandlaw.territory.TerrainView.MAX_JSON);assertEquals(121,view.cells().size());
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{GangNetwork.Terrain.CODEC.encode(buffer,new GangNetwork.Terrain(json));assertEquals(view,new Gson().fromJson(GangNetwork.Terrain.CODEC.decode(buffer).json(),pl.crimeandlaw.territory.TerrainView.class));assertFalse(buffer.isReadable());}
        finally{buffer.release();}
    }
}
