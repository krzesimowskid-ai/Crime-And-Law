package pl.crimeandlaw.cultivation;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AureliaPlant extends BlockEntity implements HarvestPlant {
    private int leaves=63;
    private UUID identity=UUID.randomUUID();
    public AureliaPlant(BlockPos pos,BlockState state){super(CultivationContent.PLANT.get(),pos,state);}
    public int leaves(){return leaves;}
    public UUID identity(){return identity;}
    public boolean cut(int leaf){
        if(!PruningRules.available(leaves,leaf))return false;
        leaves=PruningRules.remove(leaves,leaf);setChanged();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
        return true;
    }
    public int totalLeaves(){return 6;}
    public boolean ready(){return getBlockState().getValue(AureliaCrop.GROWTH)==4&&leaves!=0;}
    public void finishHarvest(){if(level!=null)level.removeBlock(worldPosition,false);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("leaves",leaves);tag.putUUID("plant",identity);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);leaves=tag.contains("leaves")?tag.getInt("leaves")&63:63;
        if(tag.hasUUID("plant"))identity=tag.getUUID("plant");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
