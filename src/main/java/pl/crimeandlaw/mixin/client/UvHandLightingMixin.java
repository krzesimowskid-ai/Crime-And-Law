package pl.crimeandlaw.mixin.client;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class UvHandLightingMixin {
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void crimeandlaw$uvFinish(DeltaTracker delta,CallbackInfo ci){pl.crimeandlaw.client.cultivation.UvLighting.finishWorld();}
    @Inject(method="renderItemInHand",at=@At("HEAD"))
    private void crimeandlaw$uvBeginHands(Camera camera,float tick,Matrix4f projection,CallbackInfo ci){pl.crimeandlaw.client.cultivation.UvLighting.beginHands();}
    @Inject(method="renderItemInHand",at=@At("RETURN"))
    private void crimeandlaw$uvEndHands(Camera camera,float tick,Matrix4f projection,CallbackInfo ci){pl.crimeandlaw.client.cultivation.UvLighting.endHands();}
    @ModifyVariable(method="reloadShaders",at=@At("HEAD"),argsOnly=true)
    private ResourceProvider crimeandlaw$uvResources(ResourceProvider original){return pl.crimeandlaw.client.cultivation.UvShaderResources.wrap(original);}
}
