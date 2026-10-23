package pl.crimeandlaw.territory;

import com.mojang.logging.LogUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.nio.file.Files;
import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.crimeandlaw.CrimeAndLaw;

/** Live cartography is separate from immutable generated terrain and from bonuses.
 * Samples live FULL chunks or existing saved FULL chunks; no chunk tickets. */
@EventBusSubscriber(modid=CrimeAndLaw.ID)
public final class TerrainCartography {
    private static final Map<MinecraftServer,Queue> QUEUES=new WeakHashMap<>();
    public static final int MAX_COLUMNS=2048,MAX_READS_PER_TICK=4,MAX_IN_FLIGHT=8;
    private static final class Queue {
        final LinkedHashMap<TerrainKey,Job> jobs=new LinkedHashMap<>();
        final Map<TerrainKey,Long> refreshed=new HashMap<>();
        final LinkedHashMap<TerrainKey,Long> absent=new LinkedHashMap<>();
        final TerrainDemand demand=new TerrainDemand();
        final List<Read> flights=new ArrayList<>();
    }
    private record Read(ServerLevel level,TerrainKey key,CompletableFuture<TerrainMapTile> future){}
    private static final class Job {
        final ServerLevel level;final TerrainKey key;final byte[] colors=new byte[256];
        int next,distance;long seen;final boolean missing;CompletableFuture<TerrainMapTile> disk;
        Job(ServerLevel level,TerrainKey key,long now,boolean missing){this.level=level;this.key=key;seen=now;this.missing=missing;}
    }
    private TerrainCartography(){}
    public static void enqueue(ServerLevel level,int x,int z){
        enqueue(level,x,z,TerrainView.RADIUS);
    }
    public static void enqueue(ServerLevel level,int x,int z,int radius){
        var point=new TerrainViewport(x,z);enqueue(level,new UUID(0,0),point,point,radius);
    }
    public static void enqueue(ServerLevel level,UUID viewer,TerrainViewport visible,TerrainViewport ahead,int radius){
        if(radius<1||radius>TerrainView.BUFFER_RADIUS)throw new IllegalArgumentException("Nieprawidłowy zasięg mapy.");
        var queue=QUEUES.computeIfAbsent(level.getServer(),ignored->new Queue());long tick=level.getServer().getTickCount();
        var saved=TerrainSavedData.get(level);
        String dimension=level.dimension().location().toString();
        finishReads(queue,tick);queue.demand.update(viewer,dimension,visible,ahead,radius,tick);prune(queue,tick);
        // Expiring bookkeeping is bounded even when many players explore a large world.
        queue.refreshed.entrySet().removeIf(e->tick-e.getValue()>200);
        queue.absent.entrySet().removeIf(e->e.getValue()<=tick);
        while(queue.absent.size()>2048)queue.absent.remove(queue.absent.keySet().iterator().next());
        // The visible camera comes first; the remaining rows are look-ahead work.
        for(int r=0;r<=radius+TerrainView.MAX_LEAD;r++)for(int dz=-r;dz<=r;dz++)for(int dx=-r;dx<=r;dx++){
            int x=visible.x(),z=visible.z();
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r||Math.abs((long)x+dx)>TerrainViewport.LIMIT||Math.abs((long)z+dz)>TerrainViewport.LIMIT||!ahead.near(x+dx,z+dz,radius))continue;
            var key=new TerrainKey(dimension,x+dx,z+dz);
            boolean loaded=level.getChunkSource().getChunkNow(key.x(),key.z())!=null;
            if(!loaded&&(saved.map(key)!=null||queue.absent.containsKey(key))||tick-queue.refreshed.getOrDefault(key,-100L)<100)continue;
            var job=queue.jobs.get(key);if(job!=null)job.seen=tick;
            else if(queue.jobs.size()<TerrainDemand.MAX_JOBS)queue.jobs.put(key,new Job(level,key,tick,saved.map(key)==null));
        }
    }
    private static void prune(Queue queue,long tick){
        queue.demand.expire(tick);var iterator=queue.jobs.values().iterator();
        while(iterator.hasNext()){
            var job=iterator.next();job.distance=queue.demand.distance(job.key);
            if(job.distance==Integer.MAX_VALUE||tick-job.seen>TerrainDemand.TTL){iterator.remove();releaseDisk(queue,job);}
        }
    }
    private static void finishReads(Queue queue,long tick){
        var iterator=queue.flights.iterator();
        while(iterator.hasNext()){
            var read=iterator.next();if(!read.future().isDone())continue;iterator.remove();
            var queued=queue.jobs.get(read.key());
            if(queued!=null&&queued.disk==read.future())queue.jobs.remove(read.key());
            try{
                var tile=read.future().join();
                if(tile==null)queue.absent.put(read.key(),tick+200);
                else {TerrainSavedData.get(read.level()).rememberMap(read.key(),tile);queue.refreshed.put(read.key(),tick);}
            }catch(RuntimeException error){
                queue.refreshed.put(read.key(),tick);LogUtils.getLogger().warn("Could not draw surface map at {}",read.key(),error);
            }
        }
    }
    public static boolean pending(ServerLevel level,TerrainKey key){var queue=QUEUES.get(level.getServer());return queue!=null&&queue.jobs.containsKey(key);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(!event.hasTime())return;var queue=QUEUES.get(event.getServer());if(queue==null)return;
        long tick=event.getServer().getTickCount(),end=System.nanoTime()+2_000_000;finishReads(queue,tick);prune(queue,tick);
        int startedReads=0,columns=0;
        // Global bounds survive camera cancellation: an actual read owns its slot
        // until completion, even when its old destination is no longer wanted.
        for(int step=0;step<64&&!queue.jobs.isEmpty()&&System.nanoTime()<end;step++){
            Map.Entry<TerrainKey,Job> chosen=null;int best=Integer.MAX_VALUE;
            for(var candidate:queue.jobs.entrySet()){
                var j=candidate.getValue();boolean loaded=j.level.getChunkSource().getChunkNow(j.key.x(),j.key.z())!=null;
                if(loaded&&columns>=MAX_COLUMNS)continue;
                if(!loaded&&j.disk!=null&&!j.disk.isDone())continue;
                if(!loaded&&j.disk==null&&(startedReads>=MAX_READS_PER_TICK||queue.flights.size()>=MAX_IN_FLIGHT))continue;
                int priority=(j.missing?(j.distance<=TerrainView.RADIUS+1?0:128):256)+j.distance*4+(loaded?0:j.disk!=null?1:2);
                if(priority<best){chosen=candidate;best=priority;if(best==0)break;}
            }
            if(chosen==null)break;
            var job=chosen.getValue();queue.jobs.remove(chosen.getKey());
            var chunk=job.level.getChunkSource().getChunkNow(job.key.x(),job.key.z());
            try{
                if(chunk==null){
                    if(job.disk==null){
                        var root=job.level.getServer().getWorldPath(LevelResource.ROOT);
                        var region=DimensionType.getStorageFolder(job.level.dimension(),root).resolve("region/r."+(job.key.x()>>5)+"."+(job.key.z()>>5)+".mca");
                        // Vanilla's storage reader can create an empty region file.
                        // Check existence first so browsing never creates world files.
                        if(!Files.isRegularFile(region)){queue.absent.put(job.key,tick+200);continue;}
                        int min=job.level.getMinBuildHeight(),height=job.level.getHeight();
                        job.disk=job.level.getChunkSource().chunkMap.read(new ChunkPos(job.key.x(),job.key.z()))
                            .thenApplyAsync(nbt->nbt.map(tag->TerrainDiskMap.raster(tag,job.key,min,height)).orElse(null),Util.backgroundExecutor());
                        queue.flights.add(new Read(job.level,job.key,job.disk));startedReads++;queue.jobs.put(job.key,job);continue;
                    }
                    var future=job.disk;job.disk=null;var tile=future.join();
                    if(tile==null)queue.absent.put(job.key,tick+200);
                    else {TerrainSavedData.get(job.level).rememberMap(job.key,tile);queue.refreshed.put(job.key,tick);}
                }else{
                    releaseDisk(queue,job);
                    int stop=Math.min(256,job.next+Math.min(64,MAX_COLUMNS-columns));
                    while(job.next<stop){job.colors[job.next]=sample(job.level,chunk,job.next%16,job.next/16);job.next++;columns++;}
                    if(job.next==256){TerrainSavedData.get(job.level).rememberMap(job.key,TerrainMapTile.of(job.colors));queue.refreshed.put(job.key,tick);}
                    else queue.jobs.putFirst(job.key,job);
                }
            }catch(RuntimeException error){
                releaseDisk(queue,job);queue.refreshed.put(job.key,tick);
                LogUtils.getLogger().warn("Could not draw surface map at {}",job.key,error);
            }
        }
    }
    private static void releaseDisk(Queue queue,Job job){job.disk=null;}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){var queue=QUEUES.remove(event.getServer());if(queue!=null)for(var job:queue.jobs.values())releaseDisk(queue,job);}
    static byte sample(ServerLevel level,LevelChunk chunk,int dx,int dz){
        int x=chunk.getPos().getMinBlockX()+dx,z=chunk.getPos().getMinBlockZ()+dz;
        return TerrainMapColors.sample(chunk,x,chunk.getHeight(Heightmap.Types.WORLD_SURFACE,dx,dz),z,chunk.getMinBuildHeight(),
            (nx,nz,fallback)->height(level,chunk,nx-chunk.getPos().getMinBlockX(),nz-chunk.getPos().getMinBlockZ(),fallback));
    }
    private static int height(ServerLevel level,LevelChunk chunk,int x,int z,int fallback){
        if(x>=0&&z>=0)return chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
        var neighbor=level.getChunkSource().getChunkNow(chunk.getPos().x+(x<0?-1:0),chunk.getPos().z+(z<0?-1:0));
        return neighbor==null?fallback:neighbor.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
    }
}
