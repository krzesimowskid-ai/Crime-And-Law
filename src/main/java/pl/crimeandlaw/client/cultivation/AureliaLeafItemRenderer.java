package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class AureliaLeafItemRenderer extends BlockEntityWithoutLevelRenderer {
    public AureliaLeafItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        AureliaMesh.drawLeaf(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(AureliaMesh.LEAF)),new AureliaMesh.Leaf(0,new Vec3(.5,.5,.08),(float)Math.PI/2,.86f,.74f),light,0xFFFFFFFF,0,false);
    }
}
