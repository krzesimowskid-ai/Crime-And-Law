package pl.crimeandlaw.cultivation;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.*;

public final class GrowFrameBlock extends PlanterBlock {
    public static final MapCodec<GrowFrameBlock> CODEC=simpleCodec(GrowFrameBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty LAMP=BooleanProperty.create("lamp");
    public GrowFrameBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(HALF,DoubleBlockHalf.LOWER).setValue(LAMP,false));}
    @Override protected MapCodec<GrowFrameBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(HALF,LAMP);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(HALF)==DoubleBlockHalf.LOWER?new Planter(p,s):null;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var p=c.getClickedPos();return p.getY()<c.getLevel().getMaxBuildHeight()-1&&c.getLevel().getBlockState(p.above()).canBeReplaced(c)?defaultBlockState():null;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){l.setBlock(p.above(),defaultBlockState().setValue(HALF,DoubleBlockHalf.UPPER),3);}
    @Override protected BlockState updateShape(BlockState s,Direction dir,BlockState other,LevelAccessor l,BlockPos p,BlockPos np){boolean lower=s.getValue(HALF)==DoubleBlockHalf.LOWER;if(dir==(lower?Direction.UP:Direction.DOWN)&&(!other.is(this)||other.getValue(HALF)==s.getValue(HALF)))return Blocks.AIR.defaultBlockState();return super.updateShape(s,dir,other,l,p,np);}
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide&&player.isCreative()&&s.getValue(HALF)==DoubleBlockHalf.UPPER&&l.getBlockState(p.below()).is(this))l.setBlock(p.below(),Blocks.AIR.defaultBlockState(),35);return super.playerWillDestroy(l,p,s,player);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.block();}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.or(box(1,0,1,2,16,2),box(14,0,1,15,16,2),box(1,0,14,2,16,15),box(14,0,14,15,16,15),s.getValue(HALF)==DoubleBlockHalf.LOWER?box(1,0,1,15,2,15):box(1,14,1,15,16,15));}
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder b){
        if(s.getValue(HALF)!=DoubleBlockHalf.LOWER)return List.of();var result=new ArrayList<ItemStack>();result.add(new ItemStack(CultivationContent.FRAME_ITEM.get()));
        if(b.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Planter p){if(p.hasPot())result.add(new ItemStack(CultivationContent.POT_ITEM.get()));if(p.hasLamp())result.add(new ItemStack(CultivationContent.UV_LAMP.get()));}
        return result;
    }
}
