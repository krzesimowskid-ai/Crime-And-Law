package pl.crimeandlaw.territory;

import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.crimeandlaw.CrimeAndLaw;

/** Opt-in disposable dedicated-server integration test; never in a release. */
@EventBusSubscriber(modid=CrimeAndLaw.ID)
public final class TerrainServerSmoke {
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.serverSmokeOutput","../build/server-smoke"));
    private static final Gson JSON=new Gson();
    private static TerrainKey target;
    private static TerrainKey legacy;
    private static TerrainProfile original;
    private static TerrainProfile.Trait expected;
    private static boolean done,restart;
    private static int waited;
    private static boolean changedMap;
    private static TerrainKey cameraOnly;
    private static TerrainKey diskOnly;
    private static java.util.concurrent.CompletableFuture<Void> diskWrite;
    private static boolean coldDiskMap;
    private static String findTemplate(Tag value){
        if(value instanceof CompoundTag tag){
            String location=tag.getString("location");if(!location.isBlank())return location;
            for(String key:tag.getAllKeys()){String found=findTemplate(tag.get(key));if(found!=null)return found;}
        }else if(value instanceof ListTag list)for(Tag tag:list){String found=findTemplate(tag);if(found!=null)return found;}
        return null;
    }
    @SubscribeEvent public static void start(ServerStartedEvent event){
        if(!Boolean.getBoolean("crimeandlaw.serverSmoke"))return;
        var server=event.getServer();var level=server.overworld();
        try{
            Files.createDirectories(OUT);restart=Files.exists(OUT.resolve("target.json"));
            System.out.println("TERRAIN_SMOKE_RESTART="+restart+" output="+OUT);
            if(restart){
                var meta=JsonParser.parseString(Files.readString(OUT.resolve("target.json"))).getAsJsonObject();
                target=JSON.fromJson(meta.get("key"),TerrainKey.class);original=JSON.fromJson(meta.get("original"),TerrainProfile.class);
                expected=TerrainProfile.Trait.valueOf(meta.get("trait").getAsString());
                var chunk=level.getChunk(target.x(),target.z());
                require(original.equals(TerrainOrigins.read(chunk)),"Generation snapshot survives a real server restart");
                require(TerrainSavedData.get(level).map(target)!=null,"Surface raster survives an actual server restart");
                require((TerrainSavedData.get(level).map(target).colors()[17]&255)>>2==net.minecraft.world.level.material.MapColor.GOLD.id,"Updated map pixel is persisted before any rescan");
            }else{
                var structures=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(StructureTags.VILLAGE).orElseThrow();
                var found=level.getChunkSource().getGenerator().findNearestMapStructure(level,structures,BlockPos.ZERO,32,false);
                require(found!=null,"Generated village located");
                ChunkPos village=new ChunkPos(found.getFirst());level.getChunk(village.x,village.z);
                var starts=level.structureManager().startsForStructure(village,s->s==found.getSecond().value());
                outer:for(var start:starts)for(var piece:start.getPieces()){
                    String template=findTemplate(piece.createTag(StructurePieceSerializationContext.fromLevel(level)).get("pool_element"));
                    if(template==null)continue;var traits=TerrainTemplates.traits(template);
                    if(!traits.contains(TerrainProfile.Trait.FARM)&&!traits.contains(TerrainProfile.Trait.WORKSHOP))continue;
                    var pos=piece.getBoundingBox().getCenter();target=TerrainKey.at(level.dimension().location().toString(),pos.getX(),pos.getZ());
                    expected=traits.contains(TerrainProfile.Trait.FARM)?TerrainProfile.Trait.FARM:TerrainProfile.Trait.WORKSHOP;break outer;
                }
                require(target!=null,"Village contains a generated farm/workshop template");
                var chunk=level.getChunk(target.x(),target.z());original=TerrainOrigins.read(chunk);
                require(original!=null&&original.source()==TerrainProfile.Source.GENERATED_SNAPSHOT,"Origin captured at real generation");
                JsonObject meta=new JsonObject();meta.add("key",JSON.toJsonTree(target));meta.add("original",JSON.toJsonTree(original));meta.addProperty("trait",expected.name());
                Files.writeString(OUT.resolve("target.json"),meta.toString());
                // Change the live chunk before analysing it: the stored snapshot
                // and generated structure templates must remain authoritative.
                for(int dz=2;dz<16;dz+=4)for(int dx=2;dx<16;dx+=4){
                    int x=target.x()*16+dx,z=target.z()*16+dz;
                    level.setBlock(new BlockPos(x,level.getMaxBuildHeight()-6,z),Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
                }
            }
            // A headless server has no player to keep the test village loaded.
            level.setChunkForced(target.x(),target.z(),true);
            var targetChunk=level.getChunk(target.x(),target.z());
            for(var references:targetChunk.getAllReferences().values())for(long pos:references){
                level.setChunkForced(ChunkPos.getX(pos),ChunkPos.getZ(pos),true);level.getChunk(ChunkPos.getX(pos),ChunkPos.getZ(pos));
            }
            legacy=new TerrainKey(target.dimension(),target.x()+1,target.z());level.setChunkForced(legacy.x(),legacy.z(),true);
            var oldChunk=level.getChunk(legacy.x(),legacy.z());
            if(!restart){
                // Simulate a chunk saved before this mod captured origins.
                oldChunk.removeData(TerrainOrigins.ORIGINAL);oldChunk.setUnsaved(true);
                for(int dz=2;dz<16;dz+=4)for(int dx=2;dx<16;dx+=4)level.setBlock(new BlockPos(legacy.x()*16+dx,level.getMaxBuildHeight()-10,legacy.z()*16+dz),Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
                // Non-4x4-sample columns and both sides of a chunk seam must be drawn.
                for(int x:new int[]{target.x()*16+1,target.x()*16+15,legacy.x()*16})level.setBlock(new BlockPos(x,level.getMaxBuildHeight()-4,target.z()*16+1),Blocks.RED_CONCRETE.defaultBlockState(),3);
            }
            // Force-loading is limited to this disposable fixture, never the map implementation.
            for(int dz=-3;dz<=3;dz++)for(int dx=-3;dx<=3;dx++){
                level.setChunkForced(target.x()+dx,target.z()+dz,true);level.getChunk(target.x()+dx,target.z()+dz);
            }
            System.out.println("TERRAIN_SMOKE_QUEUED "+target);
            TerrainAnalysis.enqueue(level,target.x(),target.z());
        }catch(Throwable error){fail(server,error);}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(!Boolean.getBoolean("crimeandlaw.serverSmoke")||done||target==null)return;
        var server=event.getServer();var level=server.overworld();
        try{
            TerrainAnalysis.enqueue(level,target.x(),target.z());var saved=TerrainSavedData.get(level);var profile=saved.atlas.get(target);
            TerrainCartography.enqueue(level,target.x(),target.z());
            var reconstructed=saved.atlas.get(legacy);
            boolean mapped=true;for(int dz=-3;dz<=3;dz++)for(int dx=-3;dx<=3;dx++)if(saved.map(new TerrainKey(target.dimension(),target.x()+dx,target.z()+dz))==null)mapped=false;
            if(profile==null||reconstructed==null||!mapped){if(++waited>1200)throw new AssertionError("Terrain/map job did not complete");return;}
            require(reconstructed.source()==TerrainProfile.Source.GENERATOR_RECONSTRUCTION,"Old chunk uses explicitly marked generator reconstruction");
            require(reconstructed.maxHeight()<level.getMaxBuildHeight()-10,"Player towers are excluded from old-chunk reconstruction");
            require(profile.traits().contains(TerrainProfile.Trait.VILLAGE)&&profile.traits().contains(expected),"Real generated village building was recognised");
            require(profile.surfaces().equals(original.surfaces())&&profile.minHeight()==original.minHeight()&&profile.maxHeight()==original.maxHeight(),"Player block edits cannot change original terrain measurements");
            require(!profile.villagePieces().isEmpty(),"Generated template evidence retained");
            byte[] colors=saved.map(target).colors(),next=saved.map(legacy).colors();
            require((colors[2+2*16]&255)>>2==net.minecraft.world.level.material.MapColor.DIAMOND.id,"Actual roof block color is drawn, not a chunk-wide biome color");
            require((colors[15+16]&255)>>2==net.minecraft.world.level.material.MapColor.COLOR_RED.id&&(next[16]&255)>>2==net.minecraft.world.level.material.MapColor.COLOR_RED.id,"Both sides of a chunk border retain their actual pixels");
            if(!restart&&!changedMap){
                require((colors[17]&255)>>2==net.minecraft.world.level.material.MapColor.COLOR_RED.id,"Columns outside the old 16-sample grid are sampled too");
                level.setBlock(new BlockPos(target.x()*16+1,level.getMaxBuildHeight()-4,target.z()*16+1),Blocks.GOLD_BLOCK.defaultBlockState(),3);changedMap=true;return;
            }
            if((colors[17]&255)>>2!=net.minecraft.world.level.material.MapColor.GOLD.id){if(++waited>1600)throw new AssertionError("Live map did not refresh after editing a block");return;}
            var owner=new java.util.UUID(21,14);saved.atlas.setOwner(target,owner);saved.setDirty();
            require(saved.atlas.traitsAt(target,owner).contains(expected),"Owned land exposes descriptive traits without awarding bonuses");
            require(saved.atlas.traitsAt(new TerrainKey(target.dimension(),target.x()+1,target.z()),owner).isEmpty(),"No spillover to the adjacent chunk");
            CompoundTag tag=saved.save(new CompoundTag(),level.registryAccess());
            var restored=TerrainAtlas.fromJson(new String(tag.getByteArray("atlas"),java.nio.charset.StandardCharsets.UTF_8));
            require(profile.equals(restored.get(target)),"Actual SavedData NBT roundtrip preserves the profile");
            var view=TerrainView.snapshot(saved.atlas,target.dimension(),target.x(),target.z(),key->level.getChunkSource().getChunkNow(key.x(),key.z())!=null,id->"Gang testowy",saved::map);
            Files.writeString(OUT.resolve("view.json"),JSON.toJson(view));
            var viewer=new net.minecraft.server.level.ServerPlayer(server,level,
                new com.mojang.authlib.GameProfile(new java.util.UUID(16,16),"MapViewer"),
                net.minecraft.server.level.ClientInformation.createDefault());
            viewer.setPos(target.x()*16+8,80,target.z()*16+8);
            var remote=new TerrainKey(target.dimension(),400_000,-400_000);
            saved.rememberMap(remote,saved.map(target));
            int loaded=level.getChunkSource().getLoadedChunksCount();
            var remoteView=TerrainAnalysis.view(viewer,new TerrainViewport(remote.x(),remote.z()));
            require(remoteView.centerX()==remote.x()&&remoteView.centerZ()==remote.z(),"Server respects a bounded remote viewport");
            require(remoteView.cells().stream().anyMatch(c->c.x()==remote.x()&&c.z()==remote.z()&&c.mapPixels().equals(saved.map(target).pixels())),"Remote viewport reads saved pixels");
            require(level.getChunkSource().getChunkNow(remote.x(),remote.z())==null&&level.getChunkSource().getLoadedChunksCount()==loaded,"Panning does not load or generate remote chunks");
            require(remoteView.playerX()==viewer.getX()&&remoteView.playerZ()==viewer.getZ(),"Remote viewport keeps the real player position");
            for(int edgeX:new int[]{-TerrainViewport.LIMIT,TerrainViewport.LIMIT})for(int edgeZ:new int[]{-TerrainViewport.LIMIT,TerrainViewport.LIMIT}){
                var edgeView=TerrainAnalysis.view(viewer,new TerrainViewport(edgeX,edgeZ));
                require(edgeView.centerX()==edgeX&&edgeView.centerZ()==edgeZ&&edgeView.cells().size()==225,"Views at all four world corners remain valid");
            }
            require(level.getChunkSource().getLoadedChunksCount()==loaded,"World-corner views do not load terrain");
            if(cameraOnly==null){
                cameraOnly=new TerrainKey(target.dimension(),target.x()+8,target.z());
                // Only this disposable fixture generates/holds the extra chunk.
                // The implementation must sample it from the camera while the player stays eight chunks away.
                level.setChunkForced(cameraOnly.x(),cameraOnly.z(),true);level.getChunk(cameraOnly.x(),cameraOnly.z());
                if(!restart)require(saved.map(cameraOnly)==null,"Camera-only chunk was not previously mapped around the player");
            }
            int beforeCamera=level.getChunkSource().getLoadedChunksCount();
            var cameraView=TerrainAnalysis.view(viewer,new TerrainViewport(cameraOnly.x(),cameraOnly.z()));
            require(beforeCamera==level.getChunkSource().getLoadedChunksCount(),"Camera request creates no chunk tickets or generation");
            if(saved.map(cameraOnly)==null){if(++waited>1600)throw new AssertionError("Loaded camera-only chunk was never mapped");return;}
            require(cameraView.cells().stream().anyMatch(c->c.x()==cameraOnly.x()&&c.z()==cameraOnly.z()&&!c.mapPixels().isEmpty()),"Viewport fills already loaded terrain outside the player's old mapping radius");
            require(cameraView.radius()==TerrainView.BUFFER_RADIUS,"Camera reply includes the prefetched border");
            if(diskOnly==null){
                diskOnly=new TerrainKey(target.dimension(),target.x()+32,target.z()+32);
                if(restart)diskWrite=java.util.concurrent.CompletableFuture.completedFuture(null);
                else {
                    // Put real serialized FULL chunk data at an unloaded test location.
                    // No live chunk is created there, and the restart must read its region file cold.
                    var nbt=net.minecraft.world.level.chunk.storage.ChunkSerializer.write(level,level.getChunk(target.x(),target.z()));
                    nbt.putInt("xPos",diskOnly.x());nbt.putInt("zPos",diskOnly.z());
                    diskWrite=level.getChunkSource().chunkMap.write(new ChunkPos(diskOnly.x(),diskOnly.z()),nbt);
                }
            }
            if(!diskWrite.isDone())return;diskWrite.join();
            if(!coldDiskMap){
                var field=TerrainSavedData.class.getDeclaredField("maps");field.setAccessible(true);((java.util.Map<?,?>)field.get(saved)).remove(diskOnly);coldDiskMap=true;
            }
            require(level.getChunkSource().getChunkNow(diskOnly.x(),diskOnly.z())==null,"Disk fixture stays unloaded");
            int beforeDisk=level.getChunkSource().getLoadedChunksCount();
            var diskView=TerrainAnalysis.view(viewer,new TerrainViewport(diskOnly.x(),diskOnly.z()));
            require(level.getChunkSource().getLoadedChunksCount()==beforeDisk,"Saved-region map request does not load any live chunks");
            if(saved.map(diskOnly)==null){if(++waited>1600)throw new AssertionError("Saved full chunk did not produce a map tile");return;}
            require((saved.map(diskOnly).colors()[17]&255)>>2==net.minecraft.world.level.material.MapColor.GOLD.id,"Saved palette and heightmap preserve the real gold surface column");
            require(diskView.cells().stream().anyMatch(c->c.x()==diskOnly.x()&&c.z()==diskOnly.z()&&!c.mapPixels().isEmpty()),"Unloaded generated terrain is visible through the actual viewport");
            require(level.getChunkSource().getChunkNow(diskOnly.x(),diskOnly.z())==null,"Disk mapping creates no chunk ticket");
            var farRegion=net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("region/r.12500.-12500.mca");
            require(!Files.exists(farRegion),"Browsing missing distant terrain does not create an empty region file");
            String result=(restart?"RESTART":"FIRST_RUN")+" PASS: real village "+target+", generated "+expected+", original traits, old-chunk reconstruction, 49 surface tiles, all-column colors, seam/live update, remote/world-corner views without generation; camera-only loaded chunk outside player radius; real FULL chunk NBT palette/heightmap read at an unloaded location without tickets, cold region-file read after restart, no empty distant region file.\n";
            Files.writeString(OUT.resolve("result.txt"),result,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            System.out.println("CRIME_TERRAIN_SERVER_PASS "+result);done=true;server.halt(false);
        }catch(Throwable error){fail(server,error);}
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void fail(net.minecraft.server.MinecraftServer server,Throwable error){
        error.printStackTrace();done=true;
        try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"FAIL: "+error+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}
        server.halt(false);
    }
}
