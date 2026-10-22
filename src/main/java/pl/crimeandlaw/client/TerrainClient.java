package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import pl.crimeandlaw.territory.TerrainView;
import pl.crimeandlaw.territory.TerrainViewport;

/** Separate terrain updates avoid retransmitting the gang's large logo. */
final class TerrainClient {
    static TerrainView view;
    private static long requested;
    private static TerrainViewport target;
    private static boolean pending;
    private TerrainClient(){}
    static void request(){
        pending=true;flush();
    }
    static void tick(){flush();}
    static boolean following(){return target==null;}
    static void pan(int dx,int dz){
        if(target==null&&view==null)return;
        var base=target==null?new TerrainViewport(view.centerX(),view.centerZ()):target;
        target=base.move(dx,dz);request();
    }
    static void home(){target=null;request();}
    private static void flush(){
        if(Minecraft.getInstance().player==null||Minecraft.getInstance().getConnection()==null)return;
        if(view!=null&&!view.dimension().equals(Minecraft.getInstance().player.level().dimension().location().toString())){clear();pending=true;}
        if(!pending)return;
        long now=System.nanoTime();if(now-requested<250_000_000L)return;requested=now;pending=false;
        JsonObject args=new JsonObject();
        if(target!=null){args.addProperty("centerX",target.x());args.addProperty("centerZ",target.z());}
        ClientState.action("terrain_view",args);
    }
    static void receive(String json){
        TerrainView next=ClientState.JSON.fromJson(json,TerrainView.class);
        if(target!=null&&(next.centerX()!=target.x()||next.centerZ()!=target.z()))return;
        var player=Minecraft.getInstance().player;
        if(player!=null){
            if(!next.dimension().equals(player.level().dimension().location().toString()))return;
            if(target==null&&(next.centerX()!=player.chunkPosition().x||next.centerZ()!=player.chunkPosition().z))return;
        }
        if(next.equals(view))return;view=next;
        if(Minecraft.getInstance().screen instanceof GangScreen screen)screen.terrainUpdated();
    }
    static void clear(){view=null;target=null;pending=false;requested=0;TerrainMapTexture.clear();}
}
