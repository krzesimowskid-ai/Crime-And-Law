package pl.crimeandlaw.cultivation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.ItemLike;

/** A fictional perennial crop; ordinary breaking yields seeds, never a bypass harvest. */
public final class AureliaCrop extends CropBlock implements EntityBlock {
    public static final com.mojang.serialization.MapCodec<AureliaCrop> CODEC=simpleCodec(AureliaCrop::new);
    public static final IntegerProperty GROWTH=IntegerProperty.create("age",0,4);
    private static final VoxelShape[] SHAPES={Block.box(5,0,5,11,4,11),Block.box(3,0,3,13,7,13),Block.box(2,0,2,14,10,14),Block.box(1,0,1,15,13,15),Block.box(1,0,1,15,16,15)};
    public AureliaCrop(BlockBehaviour.Properties properties){super(properties);}
    @Override public com.mojang.serialization.MapCodec<AureliaCrop> codec(){return CODEC;}
    @Override public IntegerProperty getAgeProperty(){return GROWTH;}
    @Override public int getMaxAge(){return 4;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(GROWTH);}
    @Override protected ItemLike getBaseSeedId(){return CultivationContent.SEEDS.get();}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPES[getAge(state)];}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AureliaPlant(pos,state);}
}
