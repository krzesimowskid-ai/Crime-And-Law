package pl.crimeandlaw.client;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import pl.crimeandlaw.gang.GangDirectory;
import pl.crimeandlaw.network.GangNetwork;
import java.util.UUID;

public final class ClientState {
    public static final Gson JSON=new Gson();
    public static JsonObject state=new JsonObject();
    public static String message="";
    public static GangDirectory.Gang gang(){
        return state.has("gang")?JSON.fromJson(state.get("gang"),GangDirectory.Gang.class):null;
    }
    public static UUID player(){return Minecraft.getInstance().player.getUUID();}
    public static void receive(String data){
        state=JsonParser.parseString(data).getAsJsonObject();message=state.get("message").getAsString();
        if(Minecraft.getInstance().screen instanceof GangScreen screen)screen.updated();
    }
    public static void action(String action,JsonObject data){
        if(Minecraft.getInstance().getConnection()==null || !Minecraft.getInstance().getConnection().hasChannel(GangNetwork.Action.TYPE)){
            message="Crime And Law musi być zainstalowany również na serwerze.";return;
        }
        data.addProperty("action",action);GangNetwork.send(data);
    }
    public static void refresh(String query,int page){JsonObject a=new JsonObject();a.addProperty("query",query);a.addProperty("page",page);action("refresh",a);}
}
