package pl.crimeandlaw.cultivation;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.UUID;

public final class Planter extends BlockEntity implements HarvestPlant {
    public final PlanterState plant=new PlanterState(); private boolean pot,lamp;
    public Planter(BlockPos pos,BlockState state){super(CultivationContent.PLANTER.get(),pos,state);pot=state.is(CultivationContent.POT.get());}
    public boolean frame(){return getBlockState().is(CultivationContent.FRAME.get());}
    public boolean hasPot(){return pot;} public boolean hasLamp(){return frame()&&lamp;}
    public boolean naturalLight(){return level!=null&&level.getBrightness(LightLayer.SKY,worldPosition.above())-level.getSkyDarken()>=9;}
    public boolean lit(){return hasLamp()||naturalLight();}
    public boolean installPot(){if(!frame()||pot)return false;pot=true;changed();return true;}
    public boolean removePot(){if(!frame()||!pot||plant.soil())return false;pot=false;changed();return true;}
    public boolean installLamp(){if(!frame()||lamp)return false;lamp=true;syncLamp();changed();return true;}
    public boolean removeLamp(){if(!hasLamp())return false;lamp=false;syncLamp();changed();return true;}
    private void syncLamp(){if(level!=null)for(var pos:new BlockPos[]{worldPosition,worldPosition.above()}){var s=level.getBlockState(pos);if(s.is(CultivationContent.FRAME.get()))level.setBlock(pos,s.setValue(GrowFrameBlock.LAMP,lamp),3);}}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    public static void serverTick(Level level,BlockPos pos,BlockState state,Planter p){if(p.pot&&p.plant.step(p.lit(),p.hasLamp(),level.random::nextInt)){p.setChanged();if(level.getGameTime()%20==0||p.plant.mature())p.changed();}}
    @Override public UUID identity(){return plant.identity();} @Override public int leaves(){return plant.leaves();} @Override public int totalLeaves(){return plant.total();}
    @Override public boolean ready(){return pot&&plant.mature()&&plant.leaves()!=0;}
    @Override public boolean cut(int id){if(!plant.cut(id))return false;changed();return true;}
    @Override public void finishHarvest(){plant.finish();changed();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);var d=plant.data();tag.putBoolean("pot",pot);tag.putBoolean("lamp",lamp);tag.putBoolean("soil",d.soil());tag.putBoolean("planted",d.planted());tag.putInt("growth",d.growth());tag.putInt("initial",d.initial());tag.putInt("uv",d.uv());tag.putInt("water",d.water());tag.putInt("total",d.total());tag.putInt("leaves",d.leaves());tag.putUUID("identity",d.identity());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);pot=!frame()||tag.getBoolean("pot");lamp=frame()&&tag.getBoolean("lamp");plant.restore(new PlanterState.Data(pot&&tag.getBoolean("soil"),pot&&tag.getBoolean("planted"),tag.getInt("growth"),tag.getInt("initial"),tag.getInt("uv"),tag.getInt("water"),tag.getInt("total"),tag.getInt("leaves"),tag.hasUUID("identity")?tag.getUUID("identity"):null));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public static BlockPos base(Level level,BlockPos pos){if(level.hasChunkAt(pos)){var s=level.getBlockState(pos);if(s.is(CultivationContent.FRAME.get())&&s.getValue(GrowFrameBlock.HALF)==DoubleBlockHalf.UPPER)return pos.below();}return pos;}
    public static Planter at(Level level,BlockPos pos){pos=base(level,pos);return level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof Planter p?p:null;}
}
