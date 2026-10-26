package pl.crimeandlaw.mixin.client;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ShaderInstance.class)
public abstract class UvShaderMixin {
    @Inject(method="apply",at=@At("HEAD"))
    private void crimeandlaw$uvLightmap(CallbackInfo ci){pl.crimeandlaw.client.cultivation.UvLighting.bind((ShaderInstance)(Object)this);}
}
