package pl.crimeandlaw.territory;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FluidState;

/** Read only full, already generated chunk NBT. No ChunkSerializer.read, entities,
 * lighting, chunk tickets, world generation, or mutation of live world state. */
final class TerrainDiskMap {
    private static final Codec<PalettedContainer<BlockState>> STATES=PalettedContainer.codecRW(
        Block.BLOCK_STATE_REGISTRY,BlockState.CODEC,PalettedContainer.Strategy.SECTION_STATES,Blocks.AIR.defaultBlockState());
    private TerrainDiskMap(){}
    static TerrainMapTile raster(CompoundTag nbt,TerrainKey key,int minHeight,int height){
        var status=ChunkStatus.byName(nbt.getString("Status"));
        if(status==null||!status.isOrAfter(ChunkStatus.FULL)||nbt.getInt("xPos")!=key.x()||nbt.getInt("zPos")!=key.z())return null;
        var source=new Snapshot(key,minHeight,height);
        for(Tag tag:nbt.getList("sections",Tag.TAG_COMPOUND)){
            var section=(CompoundTag)tag;int index=section.getByte("Y")-(minHeight>>4);
            if(index>=0&&index<source.sections.length&&section.contains("block_states",Tag.TAG_COMPOUND))
                source.sections[index]=STATES.parse(NbtOps.INSTANCE,section.getCompound("block_states")).getOrThrow();
        }
        long[] heights=nbt.getCompound("Heightmaps").getLongArray("WORLD_SURFACE");
        if(heights.length>0){
            var storage=new SimpleBitStorage(Mth.ceillog2(height+1),256,heights);
            for(int i=0;i<256;i++)source.heights[i]=minHeight+storage.get(i)-1;
        }else{
            var pos=new BlockPos.MutableBlockPos();
            for(int i=0;i<256;i++){
                int x=key.x()*16+(i&15),z=key.z()*16+(i>>4),y=minHeight+height-1;
                while(y>=minHeight&&source.getBlockState(pos.set(x,y,z)).isAir())y--;
                source.heights[i]=y;
            }
        }
        byte[] colors=new byte[256];
        for(int i=0;i<256;i++)colors[i]=TerrainMapColors.sample(source,key.x()*16+(i&15),source.heights[i],key.z()*16+(i>>4),minHeight,source::height);
        return TerrainMapTile.of(colors);
    }
    private static final class Snapshot implements BlockGetter {
        final TerrainKey key;final int min,span;final int[] heights=new int[256];
        final PalettedContainer<BlockState>[] sections;
        @SuppressWarnings("unchecked") Snapshot(TerrainKey key,int min,int span){this.key=key;this.min=min;this.span=span;sections=(PalettedContainer<BlockState>[])new PalettedContainer<?>[(span+15)/16];}
        int height(int x,int z,int fallback){return (x>>4)==key.x()&&(z>>4)==key.z()?heights[(z&15)*16+(x&15)]:fallback;}
        @Override public BlockState getBlockState(BlockPos pos){
            int section=(pos.getY()-min)>>4;
            if((pos.getX()>>4)!=key.x()||(pos.getZ()>>4)!=key.z()||section<0||section>=sections.length||sections[section]==null)return Blocks.AIR.defaultBlockState();
            return sections[section].get(pos.getX()&15,pos.getY()&15,pos.getZ()&15);
        }
        @Override public FluidState getFluidState(BlockPos pos){return getBlockState(pos).getFluidState();}
        @Override public BlockEntity getBlockEntity(BlockPos pos){return null;}
        @Override public int getHeight(){return span;}
        @Override public int getMinBuildHeight(){return min;}
    }
}
