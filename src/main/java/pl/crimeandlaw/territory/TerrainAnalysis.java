package pl.crimeandlaw.territory;

import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.crimeandlaw.CrimeAndLaw;
import pl.crimeandlaw.gang.GangSavedData;
import pl.crimeandlaw.territory.TerrainProfile.*;

/** Demand-driven server work; never generates chunks to fill a map. */
@EventBusSubscriber(modid=CrimeAndLaw.ID)
public final class TerrainAnalysis {
    private static final Map<MinecraftServer,Queue> QUEUES=new WeakHashMap<>();
    private static final class Queue {final LinkedHashMap<TerrainKey,Job> jobs=new LinkedHashMap<>();final Map<TerrainKey,Long> failed=new HashMap<>();}
    private static final class Job {
        final ServerLevel level;final TerrainKey key;final List<Sample> samples=new ArrayList<>();
        TerrainProfile base;long seen,nextTry;
        Job(ServerLevel level,TerrainKey key,long tick){this.level=level;this.key=key;seen=tick;}
    }
    private record Buildings(boolean village,List<String> templates){}
    private TerrainAnalysis(){}
    public static TerrainView view(ServerPlayer player){
        return view(player,new TerrainViewport(player.chunkPosition().x,player.chunkPosition().z));
    }
    public static TerrainView view(ServerPlayer player,TerrainViewport viewport){
        // Sample already loaded chunks at the camera, not only at the player.
        // Both queues use getChunkNow: moving the camera creates no chunk tickets.
        ServerLevel level=player.serverLevel();enqueue(level,viewport.x(),viewport.z());
        TerrainCartography.enqueue(level,viewport.x(),viewport.z(),TerrainView.BUFFER_RADIUS);
        var gangs=GangSavedData.get(player.server).directory;
        return TerrainView.snapshot(TerrainSavedData.get(level).atlas,level.dimension().location().toString(),viewport.x(),viewport.z(),
            key->level.getChunkSource().getChunkNow(key.x(),key.z())!=null||TerrainCartography.pending(level,key),
            id->{var gang=gangs.get(id);return gang==null?null:gang.name;},TerrainSavedData.get(level)::map,TerrainView.BUFFER_RADIUS)
            .withPosition(player.getX(),player.getZ(),player.getYRot());
    }
    public static void enqueue(ServerLevel level,int x,int z){
        Queue queue=QUEUES.computeIfAbsent(level.getServer(),s->new Queue());var atlas=TerrainSavedData.get(level).atlas;long tick=level.getServer().getTickCount();
        // Nearer loaded chunks around the requested viewport finish first.
        for(int r=0;r<=TerrainView.RADIUS;r++)for(int dz=-r;dz<=r;dz++)for(int dx=-r;dx<=r;dx++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            if(Math.abs((long)x+dx)>1_875_000||Math.abs((long)z+dz)>1_875_000)continue;
            TerrainKey key=new TerrainKey(level.dimension().location().toString(),x+dx,z+dz);
            if(atlas.get(key)!=null||level.getChunkSource().getChunkNow(key.x(),key.z())==null)continue;
            if(queue.failed.getOrDefault(key,0L)>tick)continue;
            Job job=queue.jobs.get(key);
            if(job!=null)job.seen=tick;else if(queue.jobs.size()<128)queue.jobs.put(key,new Job(level,key,tick));
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(!event.hasTime())return;
        Queue queue=QUEUES.get(event.getServer());if(queue==null)return;
        long tick=event.getServer().getTickCount(),end=System.nanoTime()+2_000_000;
        queue.failed.entrySet().removeIf(e->e.getValue()<=tick);
        for(int work=0;work<2&&!queue.jobs.isEmpty()&&System.nanoTime()<end;work++){
            var entry=queue.jobs.firstEntry();Job job=entry.getValue();queue.jobs.remove(entry.getKey());
            if(tick-job.seen>200||job.level.getChunkSource().getChunkNow(job.key.x(),job.key.z())==null)continue;
            if(job.nextTry>tick){queue.jobs.put(entry.getKey(),job);continue;}
            try{
                if(!step(job)){
                    if(job.nextTry>tick)queue.jobs.put(entry.getKey(),job);else queue.jobs.putFirst(entry.getKey(),job);
                }
            }catch(RuntimeException error){
                queue.failed.put(job.key,tick+600);
                LogUtils.getLogger().warn("Could not analyse generated terrain at {}",job.key,error);
            }
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){QUEUES.remove(event.getServer());}
    private static boolean step(Job job){
        var chunk=job.level.getChunkSource().getChunkNow(job.key.x(),job.key.z());if(chunk==null)return true;
        var saved=TerrainSavedData.get(job.level);if(saved.atlas.get(job.key)!=null)return true;
        if(job.base==null){
            job.base=TerrainOrigins.read(chunk);
            if(job.base==null){
                job.samples.add(reconstructSample(job.level,job.key,job.samples.size()));
                if(job.samples.size()<16)return false;
                job.base=TerrainProfile.analyse(job.key,job.samples,false,List.of(),System.currentTimeMillis()).reconstructed();
            }
        }
        Buildings buildings=buildings(job.level,chunk);
        if(buildings==null){job.nextTry=job.level.getServer().getTickCount()+20;return false;}
        if(saved.atlas.remember(job.base.withBuildings(buildings.village(),buildings.templates())))saved.setDirty();
        return true;
    }
    /** Older chunks lack a pre-edit snapshot. Use the generator, never live player blocks.
     * Surface cover is inferred from original biomes; the UI labels this reconstruction. */
    private static Sample reconstructSample(ServerLevel level,TerrainKey key,int index){
        int x=key.x()*16+2+(index%4)*4,z=key.z()*16+2+(index/4)*4;
        var generator=level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
        int y=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,level,random)-1;
        var biome=generator.getBiomeSource().getNoiseBiome(x>>2,y>>2,z>>2,random.sampler());
        String id=biome.unwrapKey().orElseThrow().location().toString();
        var state=generator.getBaseColumn(x,z,level,random).getBlock(y);Surface surface=TerrainOrigins.surface(state);
        if(surface==Surface.ROCK&&biome.is(BiomeTags.IS_OVERWORLD)){
            if(id.contains("desert")||id.contains("beach")||id.contains("badlands"))surface=Surface.SAND;
            else if(id.contains("stony")||id.contains("jagged")||id.contains("frozen_peaks"))surface=Surface.ROCK;
            else surface=biome.value().getBaseTemperature()<0.15f?Surface.SNOW:Surface.SOIL;
        }
        return new Sample(y,surface,biome.is(BiomeTags.IS_FOREST)||biome.is(BiomeTags.IS_TAIGA)||biome.is(BiomeTags.IS_JUNGLE),id);
    }
    private static Buildings buildings(ServerLevel level,LevelChunk chunk){
        var registry=level.registryAccess().registryOrThrow(Registries.STRUCTURE);Set<String> templates=new TreeSet<>();boolean village=false;
        for(var reference:chunk.getAllReferences().entrySet()){
            var holder=registry.getHolder(registry.getId(reference.getKey()));
            if(holder.isEmpty()||!holder.get().is(StructureTags.VILLAGE))continue;
            for(long pos:reference.getValue()){
                // false requests no generation/tickets. If a referenced start is
                // unavailable, defer instead of permanently storing missing buildings.
                ChunkAccess startChunk=level.getChunkSource().getChunk(ChunkPos.getX(pos),ChunkPos.getZ(pos),ChunkStatus.STRUCTURE_STARTS,false);
                if(startChunk==null)return null;
                var start=startChunk.getStartForStructure(reference.getKey());if(start==null||!start.isValid())continue;
                for(var piece:start.getPieces()){
                    var box=piece.getBoundingBox();
                    if(!TerrainTemplates.intersectsChunk(box.minX(),box.minZ(),box.maxX(),box.maxZ(),chunk.getPos().x,chunk.getPos().z))continue;
                    village=true;
                    if(piece instanceof PoolElementStructurePiece)collect(piece.createTag(StructurePieceSerializationContext.fromLevel(level)).get("pool_element"),templates,0);
                }
            }
        }
        return new Buildings(village,new ArrayList<>(templates));
    }
    private static void collect(Tag value,Set<String> templates,int depth){
        if(value==null||depth>8)return;
        if(value instanceof CompoundTag tag){
            if(tag.contains("location",Tag.TAG_STRING)){
                String location=tag.getString("location");if(location.contains(":village/"))templates.add(location);
            }
            for(String key:tag.getAllKeys())collect(tag.get(key),templates,depth+1);
        }else if(value instanceof ListTag list)for(Tag tag:list)collect(tag,templates,depth+1);
    }
}
