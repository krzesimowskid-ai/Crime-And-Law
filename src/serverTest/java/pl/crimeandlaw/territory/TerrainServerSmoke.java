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
                    level.setBlock(new BlockPos(x,original.maxHeight()+4,z),Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
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
            var reconstructed=saved.atlas.get(legacy);
            if(profile==null||reconstructed==null){if(++waited>1200)throw new AssertionError("Terrain job did not complete");return;}
            require(reconstructed.source()==TerrainProfile.Source.GENERATOR_RECONSTRUCTION,"Old chunk uses explicitly marked generator reconstruction");
            require(reconstructed.maxHeight()<level.getMaxBuildHeight()-10,"Player towers are excluded from old-chunk reconstruction");
            require(profile.traits().contains(TerrainProfile.Trait.VILLAGE)&&profile.traits().contains(expected),"Real generated village building was recognised");
            require(profile.surfaces().equals(original.surfaces())&&profile.minHeight()==original.minHeight()&&profile.maxHeight()==original.maxHeight(),"Player block edits cannot change original terrain measurements");
            require(!profile.villagePieces().isEmpty(),"Generated template evidence retained");
            var owner=new java.util.UUID(21,14);saved.atlas.setOwner(target,owner);saved.setDirty();
            require(saved.atlas.capabilitiesAt(target,owner).contains(expected),"Owning gang receives local capabilities");
            require(saved.atlas.capabilitiesAt(new TerrainKey(target.dimension(),target.x()+1,target.z()),owner).isEmpty(),"No spillover to the adjacent chunk");
            CompoundTag tag=saved.save(new CompoundTag(),level.registryAccess());
            var restored=TerrainAtlas.fromJson(new String(tag.getByteArray("atlas"),java.nio.charset.StandardCharsets.UTF_8));
            require(profile.equals(restored.get(target)),"Actual SavedData NBT roundtrip preserves the profile");
            String result=(restart?"RESTART":"FIRST_RUN")+" PASS: dedicated server, real village "+target+", generated "+expected+", origin attachment, edits ignored, marked old-chunk reconstruction ignoring player towers, local ownership and SavedData NBT.\n";
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
