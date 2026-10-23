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
    private static final pl.crimeandlaw.territory.TerrainViewCache cache=new pl.crimeandlaw.territory.TerrainViewCache();
    private TerrainClient(){}
    static void request(){
        pending=true;flush();
    }
    static void tick(){
        // Complete visible/prefetched loaded tiles promptly instead of waiting two seconds.
        if(view!=null&&view.cells().stream().anyMatch(c->c.mapPixels().isEmpty()&&c.status()!=TerrainView.Status.UNAVAILABLE))pending=true;
        flush();
    }
    static boolean following(){return target==null;}
    static void pan(int dx,int dz){
        if(target==null&&view==null)return;
        var base=target==null?new TerrainViewport(view.centerX(),view.centerZ()):target;
        target=base.move(dx,dz);
        if(view!=null){cache.remember(view);view=cache.viewport(view,target,TerrainView.BUFFER_RADIUS);updated();}
        request();
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
        var player=Minecraft.getInstance().player;
        if(player!=null){
            if(!next.dimension().equals(player.level().dimension().location().toString()))return;
        }
        // A reply for an earlier camera position still contains useful neighboring tiles.
        next=cache.remember(next);
        TerrainViewport wanted=target!=null?target:player==null?new TerrainViewport(next.centerX(),next.centerZ()):new TerrainViewport(player.chunkPosition().x,player.chunkPosition().z);
        if(next.centerX()!=wanted.x()||next.centerZ()!=wanted.z())next=cache.viewport(next,wanted,TerrainView.BUFFER_RADIUS);
        if(next.equals(view))return;view=next;updated();
    }
    private static void updated(){if(Minecraft.getInstance().screen instanceof GangScreen screen)screen.terrainUpdated();}
    static void clear(){view=null;target=null;pending=false;requested=0;cache.clear();TerrainMapTexture.clear();}
}
