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
        BufferedImage image=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);Random random=new Random(22);
        for(int y=0;y<512;y++)for(int x=0;x<512;x++)image.setRGB(x,y,random.nextInt()|0xFF000000);
        String logo=LogoImages.encode(image,new LogoCrop(512,512));
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
        GangDirectory restored=GangDirectory.fromJson(directory.toJson());
        assertEquals(logo,restored.of(owner).logo);
        String snapshot=GangView.snapshot(restored.of(owner),owner,0,0).toString();
        var download=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            GangNetwork.State.CODEC.encode(download,new GangNetwork.State(snapshot));
            JsonObject state=JsonParser.parseString(GangNetwork.State.CODEC.decode(download).json()).getAsJsonObject();
            assertFalse(download.isReadable());assertEquals("Żółty Gang",state.getAsJsonObject("gang").get("name").getAsString());
            BufferedImage displayed=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(state.getAsJsonObject("gang").get("logo").getAsString())));
            assertEquals(512,displayed.getWidth());assertEquals(512,displayed.getHeight());
            assertEquals(image.getRGB(128,128),displayed.getRGB(128,128));
        }finally{download.release();}
    }
    @Test void packetCodecsStillRejectOverBudgetStrings(){
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            assertThrows(RuntimeException.class,()->GangNetwork.Action.CODEC.encode(buffer,new GangNetwork.Action("a".repeat(GangNetwork.ACTION_MAX_JSON+1))));
            buffer.clear();
            assertThrows(RuntimeException.class,()->GangNetwork.State.CODEC.encode(buffer,new GangNetwork.State("a".repeat(GangNetwork.STATE_MAX_JSON+1))));
        }finally{buffer.release();}
    }
}
