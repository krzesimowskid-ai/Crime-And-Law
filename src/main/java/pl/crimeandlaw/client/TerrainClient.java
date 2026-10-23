package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import pl.crimeandlaw.territory.TerrainView;
import pl.crimeandlaw.territory.TerrainViewport;
import pl.crimeandlaw.territory.TerrainRequestGate;

/** Separate terrain updates avoid retransmitting the gang's large logo. */
final class TerrainClient {
    static TerrainView view;
    private static TerrainViewport target;
    private static double cameraX,cameraZ;
    private static int leadX,leadZ;
    private static boolean pending,prefetchPending;
    private static final TerrainRequestGate gate=new TerrainRequestGate();
    private static final pl.crimeandlaw.territory.TerrainViewCache cache=new pl.crimeandlaw.territory.TerrainViewCache();
    private TerrainClient(){}
    static void request(){
        pending=true;flush();
    }
    static void tick(){
        if(view==null||prefetchPending||view.cells().stream().anyMatch(c->Math.abs(c.x()-view.centerX())<=TerrainView.RADIUS+2&&Math.abs(c.z()-view.centerZ())<=TerrainView.RADIUS+2&&c.mapPixels().isEmpty()&&c.status()!=TerrainView.Status.UNAVAILABLE)
            ||gate.awaiting()!=0&&gate.ready(System.nanoTime()))pending=true;
        flush();
    }
    static boolean following(){return target==null;}
    static double cameraX(){return target!=null?cameraX:view==null?0:view.centerX();}
    static double cameraZ(){return target!=null?cameraZ:view==null?0:view.centerZ();}
    static void pan(int dx,int dz){
        if(target==null&&view==null)return;
        if(!moveTo(cameraX()+dx,cameraZ()+dz))updated();
    }
    static boolean moveTo(double x,double z){
        if(view==null||!Double.isFinite(x)||!Double.isFinite(z))return false;
        double dx=x-cameraX(),dz=z-cameraZ(),distance=Math.max(Math.abs(dx),Math.abs(dz));
        cameraX=Math.clamp(x,-TerrainViewport.LIMIT,TerrainViewport.LIMIT);
        cameraZ=Math.clamp(z,-TerrainViewport.LIMIT,TerrainViewport.LIMIT);
        var next=new TerrainViewport((int)Math.round(cameraX),(int)Math.round(cameraZ));
        boolean rebase=!next.equals(target);target=next;
        if(rebase){
            if(distance>0){leadX=(int)Math.round(dx/distance*TerrainView.MAX_LEAD);leadZ=(int)Math.round(dz/distance*TerrainView.MAX_LEAD);}
            cache.remember(view);view=cache.viewport(view,target,TerrainView.BUFFER_RADIUS);updated();request();
        }
        return rebase;
    }
    static void home(){
        target=null;cameraX=cameraZ=0;leadX=leadZ=0;
        if(view!=null){
            var player=Minecraft.getInstance().player;
            double x=player==null?view.playerX():player.getX(),z=player==null?view.playerZ():player.getZ();
            var point=new TerrainViewport((int)Math.clamp(Math.floor(x/16),-TerrainViewport.LIMIT,TerrainViewport.LIMIT),(int)Math.clamp(Math.floor(z/16),-TerrainViewport.LIMIT,TerrainViewport.LIMIT));
            cache.remember(view);cache.protect(point);view=cache.viewport(view.withPosition(x,z,player==null?view.yaw():player.getYRot()),point,TerrainView.BUFFER_RADIUS);
        }
        updated();request();
    }
    private static void flush(){
        if(Minecraft.getInstance().player==null||Minecraft.getInstance().getConnection()==null)return;
        if(view!=null&&!view.dimension().equals(Minecraft.getInstance().player.level().dimension().location().toString())){clear();pending=true;}
        if(!pending)return;
        int id=gate.start(System.nanoTime());if(id==0)return;pending=false;
        JsonObject args=new JsonObject();args.addProperty("requestId",id);
        if(target!=null){args.addProperty("centerX",target.x());args.addProperty("centerZ",target.z());}
        args.addProperty("leadX",target==null?0:leadX);args.addProperty("leadZ",target==null?0:leadZ);
        ClientState.action("terrain_view",args);
    }
    static void receive(String json){
        TerrainView next=ClientState.JSON.fromJson(json,TerrainView.class);
        var player=Minecraft.getInstance().player;
        if(player!=null){
            if(!next.dimension().equals(player.level().dimension().location().toString()))return;
        }
        boolean acknowledged=gate.acknowledge(next.requestId());int id=next.requestId();next=next.withRequestId(0);
        // A reply for an earlier camera position still contains useful neighboring tiles.
        next=cache.remember(next);
        cache.protect(new TerrainViewport((int)Math.clamp(Math.floor(next.playerX()/16),-TerrainViewport.LIMIT,TerrainViewport.LIMIT),(int)Math.clamp(Math.floor(next.playerZ()/16),-TerrainViewport.LIMIT,TerrainViewport.LIMIT)));
        if(acknowledged||id==0)prefetchPending=next.cells().stream().anyMatch(c->c.mapPixels().isEmpty()&&c.status()==TerrainView.Status.PENDING);
        TerrainViewport wanted=target!=null?target:player==null?new TerrainViewport(next.centerX(),next.centerZ()):new TerrainViewport(player.chunkPosition().x,player.chunkPosition().z);
        if(next.centerX()!=wanted.x()||next.centerZ()!=wanted.z())next=cache.viewport(next,wanted,TerrainView.BUFFER_RADIUS);
        if(!next.equals(view)){view=next;updated();}flush();
    }
    private static void updated(){if(Minecraft.getInstance().screen instanceof GangScreen screen)screen.terrainUpdated();}
    static void clear(){view=null;target=null;cameraX=cameraZ=0;leadX=leadZ=0;pending=prefetchPending=false;gate.clear();cache.clear();TerrainMapTexture.clear();}
}
