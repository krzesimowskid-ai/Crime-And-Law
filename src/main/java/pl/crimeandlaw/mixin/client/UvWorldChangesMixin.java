package pl.crimeandlaw.mixin.client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class)
public abstract class UvWorldChangesMixin {
    private void crimeandlaw$opacityChanged(BlockPos pos,BlockState oldState,BlockState newState){var level=(ClientLevel)(Object)this;if((oldState.getLightBlock(level,pos)>=15)!=(newState.getLightBlock(level,pos)>=15))pl.crimeandlaw.client.cultivation.UvLighting.changed(level,pos);}
    @Inject(method="setBlocksDirty",at=@At("HEAD")) private void crimeandlaw$dirty(BlockPos pos,BlockState oldState,BlockState newState,CallbackInfo ci){crimeandlaw$opacityChanged(pos,oldState,newState);}
    @Inject(method="sendBlockUpdated",at=@At("HEAD")) private void crimeandlaw$updated(BlockPos pos,BlockState oldState,BlockState newState,int flags,CallbackInfo ci){crimeandlaw$opacityChanged(pos,oldState,newState);}
}
