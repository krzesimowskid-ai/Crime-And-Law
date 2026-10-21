package pl.crimeandlaw.territory;

import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.crimeandlaw.CrimeAndLaw;

/** Live cartography is separate from immutable generated terrain and from bonuses.
 * Requests only sample FULL chunks already loaded by the server; no chunk tickets. */
@EventBusSubscriber(modid=CrimeAndLaw.ID)
public final class TerrainCartography {
    private static final Map<MinecraftServer,Queue> QUEUES=new WeakHashMap<>();
    private static final class Queue {
        final LinkedHashMap<TerrainKey,Job> jobs=new LinkedHashMap<>();
        final Map<TerrainKey,Long> refreshed=new HashMap<>();
    }
    private static final class Job {
        final ServerLevel level;final TerrainKey key;final byte[] colors=new byte[256];
        int next;long seen;
        Job(ServerLevel level,TerrainKey key,long now){this.level=level;this.key=key;seen=now;}
    }
    private TerrainCartography(){}
    public static void enqueue(ServerLevel level,int x,int z){
        var queue=QUEUES.computeIfAbsent(level.getServer(),ignored->new Queue());long tick=level.getServer().getTickCount();
        // Expiring bookkeeping is bounded even when many players explore a large world.
        queue.refreshed.entrySet().removeIf(e->tick-e.getValue()>200);
        for(int r=0;r<=TerrainView.RADIUS;r++)for(int dz=-r;dz<=r;dz++)for(int dx=-r;dx<=r;dx++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r||Math.abs((long)x+dx)>1_875_000||Math.abs((long)z+dz)>1_875_000)continue;
            var key=new TerrainKey(level.dimension().location().toString(),x+dx,z+dz);
            if(level.getChunkSource().getChunkNow(key.x(),key.z())==null||tick-queue.refreshed.getOrDefault(key,-100L)<100)continue;
            var job=queue.jobs.get(key);if(job!=null)job.seen=tick;
            else if(queue.jobs.size()<256)queue.jobs.put(key,new Job(level,key,tick));
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(!event.hasTime())return;var queue=QUEUES.get(event.getServer());if(queue==null)return;
        long tick=event.getServer().getTickCount(),end=System.nanoTime()+2_000_000;
        // At most 128 columns / tick, in 32-column steps, and a soft 2 ms budget.
        for(int step=0;step<4&&!queue.jobs.isEmpty()&&System.nanoTime()<end;step++){
            var entry=queue.jobs.firstEntry();var job=entry.getValue();queue.jobs.remove(entry.getKey());
            var chunk=job.level.getChunkSource().getChunkNow(job.key.x(),job.key.z());
            if(tick-job.seen>200||chunk==null)continue;
            try{
                int stop=Math.min(256,job.next+32);
                while(job.next<stop){job.colors[job.next]=sample(job.level,chunk,job.next%16,job.next/16);job.next++;}
                if(job.next==256){TerrainSavedData.get(job.level).rememberMap(job.key,TerrainMapTile.of(job.colors));queue.refreshed.put(job.key,tick);}
                else queue.jobs.putFirst(job.key,job);
            }catch(RuntimeException error){
                queue.refreshed.put(job.key,tick);LogUtils.getLogger().warn("Could not draw surface map at {}",job.key,error);
            }
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){QUEUES.remove(event.getServer());}
    static byte sample(ServerLevel level,LevelChunk chunk,int dx,int dz){
        int x=chunk.getPos().getMinBlockX()+dx,z=chunk.getPos().getMinBlockZ()+dz;
        int y=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,dx,dz),min=chunk.getMinBuildHeight();
        if(y<min)return 0;
        var pos=new BlockPos.MutableBlockPos(x,y,z);var state=chunk.getBlockState(pos);var color=state.getMapColor(chunk,pos);
        // Match map visibility: transparent blocks do not hide the ground below.
        while(y>min&&color==MapColor.NONE){state=chunk.getBlockState(pos.setY(--y));color=state.getMapColor(chunk,pos);}
        if(color==MapColor.NONE)return 0;
        MapColor.Brightness brightness;
        if(color==MapColor.WATER){
            int depth=0;while(depth<12&&y-depth>min&&!chunk.getBlockState(pos.setY(y-depth)).getFluidState().isEmpty())depth++;
            brightness=depth<3?MapColor.Brightness.HIGH:depth>7?MapColor.Brightness.LOW:MapColor.Brightness.NORMAL;
        }else{
            int north=height(level,chunk,dx,dz-1,y),west=height(level,chunk,dx-1,dz,y);
            double slope=(y-north)*0.65+(y-west)*0.35;
            brightness=slope>0.6?MapColor.Brightness.HIGH:slope< -0.6?MapColor.Brightness.LOW:MapColor.Brightness.NORMAL;
        }
        return color.getPackedId(brightness);
    }
    private static int height(ServerLevel level,LevelChunk chunk,int x,int z,int fallback){
        if(x>=0&&z>=0)return chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
        var neighbor=level.getChunkSource().getChunkNow(chunk.getPos().x+(x<0?-1:0),chunk.getPos().z+(z<0?-1:0));
        return neighbor==null?fallback:neighbor.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
    }
}
