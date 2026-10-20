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
    private static JsonObject cachedState;
    private static GangDirectory.Gang cachedGang;
    public static GangDirectory.Gang gang(){
        if(cachedState!=state){cachedState=state;cachedGang=state.has("gang")?JSON.fromJson(state.get("gang"),GangDirectory.Gang.class):null;}
        return cachedGang;
    }
    public static UUID player(){
        var player=Minecraft.getInstance().player;
        return player!=null?player.getUUID():state.has("player")?UUID.fromString(state.get("player").getAsString()):new UUID(0,0);
    }
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
    public static int number(String key,int fallback){return state.has(key)?state.get(key).getAsInt():fallback;}
    public static int memberCount(){var g=gang();return number("memberCount",g==null?0:g.members.size());}
    public static void listPage(boolean applications,int page){JsonObject a=new JsonObject();a.addProperty(applications?"requestPage":"memberPage",page);action("list_page",a);}
}

