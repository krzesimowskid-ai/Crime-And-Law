package pl.crimeandlaw.mixin;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.crimeandlaw.territory.CaptureLoot;
import pl.crimeandlaw.territory.CaptureSavedData;

/** A participant's drops must never absorb unrelated ground items before collection. */
@Mixin(ItemEntity.class)
abstract class CaptureDropMixin {
    @Inject(method="tryToMerge",at=@At("HEAD"),cancellable=true)
    private void crimeandlaw$separateCaptureDrops(ItemEntity other,CallbackInfo callback){
        var self=(ItemEntity)(Object)this;if(!(self.level() instanceof ServerLevel level))return;
        UUID a=self.getPersistentData().hasUUID(CaptureLoot.EVENT)?self.getPersistentData().getUUID(CaptureLoot.EVENT):null;
        UUID b=other.getPersistentData().hasUUID(CaptureLoot.EVENT)?other.getPersistentData().getUUID(CaptureLoot.EVENT):null;
        if(Objects.equals(a,b)||a==null&&b==null)return;
        if(CaptureSavedData.get(level.getServer()).board.battles().stream().anyMatch(event->event.id.equals(a)||event.id.equals(b)))callback.cancel();
    }
}
