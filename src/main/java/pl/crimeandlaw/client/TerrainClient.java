package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import pl.crimeandlaw.territory.TerrainView;

/** Separate terrain updates avoid retransmitting the gang's large logo. */
final class TerrainClient {
    static TerrainView view;
    private static long requested;
    private TerrainClient(){}
    static void request(){
        if(Minecraft.getInstance().player==null||Minecraft.getInstance().getConnection()==null)return;
        if(view!=null&&!view.dimension().equals(Minecraft.getInstance().player.level().dimension().location().toString()))clear();
        long now=System.nanoTime();if(now-requested<1_500_000_000L)return;requested=now;
        ClientState.action("terrain_view",new JsonObject());
    }
    static void receive(String json){
        TerrainView next=ClientState.JSON.fromJson(json,TerrainView.class);
        if(next.equals(view))return;view=next;
        if(Minecraft.getInstance().screen instanceof GangScreen screen)screen.terrainUpdated();
    }
    static void clear(){view=null;requested=0;TerrainMapTexture.clear();}
}
