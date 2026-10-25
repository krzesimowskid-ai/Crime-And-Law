package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import pl.crimeandlaw.cultivation.CultivationContent;

/** Pixel silhouettes have front, back and solid stepped side walls in every item view. */
public final class CultivationItemRenderer extends BlockEntityWithoutLevelRenderer {
    public CultivationItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(GardenMesh.modelItem(stack)){GardenMesh.item(pose,buffers,stack,light);return;}
        var sprite=stack.is(CultivationContent.SHEARS.get())?PixelSprite.SHEARS:stack.is(CultivationContent.SEEDS.get())?PixelSprite.SEEDS:PixelSprite.LEAF;
        float thickness=sprite==PixelSprite.LEAF?.018f:sprite==PixelSprite.SHEARS?.043f:.055f;
        sprite.draw(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(sprite.texture())),(u,v,d)->new Vec3(.04+u*.92,.04+(1-v)*.92,.5+d),thickness,0xFFFFFFFF,light,false);
    }
}
