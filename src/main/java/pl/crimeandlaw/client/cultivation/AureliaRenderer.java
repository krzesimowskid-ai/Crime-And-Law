package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import pl.crimeandlaw.cultivation.*;

public final class AureliaRenderer implements BlockEntityRenderer<AureliaPlant> {
    public AureliaRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(AureliaPlant plant,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        int age=plant.getBlockState().getValue(AureliaCrop.GROWTH);
        float time=plant.getLevel()==null?0:plant.getLevel().getGameTime()+partial;
        AureliaMesh.draw(pose,buffers,age,plant.leaves(),-1,time/20,light,6);
    }
}
