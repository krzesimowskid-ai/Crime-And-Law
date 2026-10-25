package pl.crimeandlaw.cultivation;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public class PlanterBlock extends BaseEntityBlock {
    public static final MapCodec<PlanterBlock> CODEC=simpleCodec(PlanterBlock::new);
    public PlanterBlock(Properties p){super(p);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Planter(p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(2,0,2,14,8,14);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,CultivationContent.PLANTER.get(),Planter::serverTick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){if(player instanceof net.minecraft.server.level.ServerPlayer sp)PotController.interact(sp,p,h);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(player instanceof net.minecraft.server.level.ServerPlayer sp)PotController.interact(sp,p,InteractionHand.MAIN_HAND);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder b){return List.of(new ItemStack(CultivationContent.POT_ITEM.get()));}
}
