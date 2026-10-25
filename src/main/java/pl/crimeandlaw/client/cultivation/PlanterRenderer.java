package pl.crimeandlaw.client.cultivation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import pl.crimeandlaw.cultivation.*;
public final class PlanterRenderer implements BlockEntityRenderer<Planter> {
    public PlanterRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(Planter pot,float delta,PoseStack pose,MultiBufferSource buffers,int light,int overlay){var s=pot.plant;GardenMesh.station(pose,buffers,pot.frame(),pot.hasPot(),pot.hasLamp(),s.soil()?1:0,s.planted()?s.growth():-1,s.mature()?s.leaves():63,s.total(),s.water(),light);}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(Planter p){return new net.minecraft.world.phys.AABB(p.getBlockPos()).expandTowards(0,p.frame()?1:.4,0);}
}
