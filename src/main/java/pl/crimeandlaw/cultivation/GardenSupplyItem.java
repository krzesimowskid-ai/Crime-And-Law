package pl.crimeandlaw.cultivation;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.tags.FluidTags;

/** Stored contents, deliberately independent of repairable damage. */
public final class GardenSupplyItem extends Item {
    public enum Kind { SOIL, PACK, WATER, SEEDLING, LAMP }
    public final Kind kind; public final int capacity;
    public GardenSupplyItem(Kind kind,int capacity,Properties props){super(props);this.kind=kind;this.capacity=capacity;}
    public int contents(ItemStack stack){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();String key=kind==Kind.WATER?"water":"uses";return tag.contains(key)?Math.clamp(tag.getInt(key),0,capacity):kind==Kind.WATER?0:capacity;}
    public ItemStack withContents(ItemStack stack,int n){var copy=stack.copy();var tag=copy.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt(kind==Kind.WATER?"water":"uses",Math.clamp(n,0,capacity));copy.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return copy;}
    public ItemStack usedSoil(ItemStack stack){int left=contents(stack)-1;if(left<=0){var c=stack.copy();c.shrink(1);return c;}return withContents(stack,left);}
    @Override public boolean isBarVisible(ItemStack stack){return (kind==Kind.WATER||kind==Kind.SOIL&&capacity>1)&&contents(stack)<capacity;}
    @Override public int getBarWidth(ItemStack stack){return Math.round(13f*contents(stack)/Math.max(1,capacity));}
    @Override public int getBarColor(ItemStack stack){return kind==Kind.WATER?0x63BEE0:0xC6AC77;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        if(kind==Kind.SOIL)lines.add(Component.literal("Pozostało pełnych napełnień: "+contents(stack)+" / "+capacity));
        if(kind==Kind.PACK)lines.add(Component.literal("5 nasion • PPM, aby rozpakować"));
        if(kind==Kind.WATER){lines.add(Component.literal("Woda: "+contents(stack)+" / "+capacity));lines.add(Component.literal("Napełnij PPM przy źródle wody"));}
        if(kind==Kind.LAMP)lines.add(Component.literal("Montowana w górnym bloku stelaża • jedna doniczka"));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(kind==Kind.PACK){if(player instanceof net.minecraft.server.level.ServerPlayer sp)PotController.openPacket(sp,hand==InteractionHand.MAIN_HAND?player.getInventory().selected:40);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
        if(kind==Kind.WATER){var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);if(hit instanceof BlockHitResult b&&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&level.getFluidState(b.getBlockPos()).is(FluidTags.WATER)&&level.getFluidState(b.getBlockPos()).isSource()&&player.getEyePosition().distanceToSqr(hit.getLocation())<=25&&level.mayInteract(player,b.getBlockPos())){
            if(player instanceof net.minecraft.server.level.ServerPlayer sp&&!sp.isSpectator()){var expected=stack.copy();if(!HarvestController.interactionAllowed(sp,b.getBlockPos())||!ItemStack.matches(expected,player.getItemInHand(hand))||!level.getFluidState(b.getBlockPos()).isSource()||!level.getFluidState(b.getBlockPos()).is(FluidTags.WATER))return InteractionResultHolder.fail(player.getItemInHand(hand));player.setItemInHand(hand,withContents(expected,capacity));sp.inventoryMenu.broadcastChanges();sp.displayClientMessage(Component.literal("Konewka napełniona: "+capacity+" / "+capacity),true);}
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }}
        return super.use(level,player,hand);
    }
}
