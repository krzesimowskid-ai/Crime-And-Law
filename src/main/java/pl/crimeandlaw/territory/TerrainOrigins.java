package pl.crimeandlaw.territory;

import com.google.gson.Gson;
import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.registries.*;
import pl.crimeandlaw.CrimeAndLaw;
import pl.crimeandlaw.territory.TerrainProfile.*;

/** Capture chunk-local evidence at first generation, before players can alter it. */
@EventBusSubscriber(modid=CrimeAndLaw.ID)
public final class TerrainOrigins {
    private static final Gson JSON=new Gson();
    public static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,CrimeAndLaw.ID);
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<String>> ORIGINAL=TYPES.register("original_terrain",()->AttachmentType.builder(()->"").serialize(Codec.STRING).build());
    private TerrainOrigins(){}
    @SubscribeEvent public static void generated(ChunkEvent.Load event){
        if(!event.isNewChunk()||!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return;
        ChunkAccess chunk=event.getChunk();if(chunk.hasData(ORIGINAL))return;
        // Only the chunk argument is read here: level/chunk loading inside this
        // event can deadlock, so cross-chunk structure resolution runs later.
        String dimension=level.dimension().location().toString();
        TerrainKey key=new TerrainKey(dimension,chunk.getPos().x,chunk.getPos().z);
        List<Sample> samples=new ArrayList<>();
        for(int dz=2;dz<16;dz+=4)for(int dx=2;dx<16;dx+=4){
            int x=chunk.getPos().getMinBlockX()+dx,z=chunk.getPos().getMinBlockZ()+dz;
            int y=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,dx,dz);BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos(x,y,z);
            BlockState state=chunk.getBlockState(pos);boolean trees=false;
            while(y>chunk.getMinBuildHeight()&&(state.is(BlockTags.LEAVES)||state.is(BlockTags.LOGS)||state.canBeReplaced()&&state.getFluidState().isEmpty())){
                trees|=state.is(BlockTags.LEAVES)||state.is(BlockTags.LOGS);state=chunk.getBlockState(pos.setY(--y));
            }
            var biome=chunk.getNoiseBiome(x>>2,y>>2,z>>2);
            String id=biome.unwrapKey().orElseThrow().location().toString();
            boolean wooded=trees||biome.is(BiomeTags.IS_FOREST)||biome.is(BiomeTags.IS_TAIGA)||biome.is(BiomeTags.IS_JUNGLE);
            samples.add(new Sample(y,surface(state),wooded,id));
        }
        chunk.setData(ORIGINAL,JSON.toJson(TerrainProfile.analyse(key,samples,false,List.of(),System.currentTimeMillis())));chunk.setUnsaved(true);
    }
    public static Surface surface(BlockState state){
        if(state.getFluidState().is(FluidTags.WATER)||state.is(Blocks.ICE)||state.is(Blocks.PACKED_ICE))return Surface.WATER;
        if(state.is(BlockTags.DIRT)||state.is(Blocks.FARMLAND)||state.is(Blocks.MUD))return Surface.SOIL;
        if(state.is(BlockTags.SAND)||state.is(Blocks.GRAVEL))return Surface.SAND;
        if(state.is(Blocks.SNOW)||state.is(Blocks.SNOW_BLOCK)||state.is(Blocks.POWDER_SNOW))return Surface.SNOW;
        if(state.is(BlockTags.BASE_STONE_OVERWORLD)||state.is(BlockTags.BASE_STONE_NETHER)||state.is(Blocks.END_STONE))return Surface.ROCK;
        return Surface.OTHER;
    }
    public static TerrainProfile read(ChunkAccess chunk){
        if(!chunk.hasData(ORIGINAL))return null;
        String value=chunk.getData(ORIGINAL);return value.isBlank()?null:JSON.fromJson(value,TerrainProfile.class);
    }
}
