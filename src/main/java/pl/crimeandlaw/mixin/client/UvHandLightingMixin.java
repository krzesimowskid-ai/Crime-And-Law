package pl.crimeandlaw.mixin.client;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class UvHandLightingMixin {
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void crimeandlaw$uvHands(DeltaTracker delta,CallbackInfo ci){pl.crimeandlaw.client.cultivation.UvLighting.afterHands();}
}
