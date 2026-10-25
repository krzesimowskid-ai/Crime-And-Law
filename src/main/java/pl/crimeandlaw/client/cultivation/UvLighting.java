package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import pl.crimeandlaw.cultivation.Planter;

/** Local purple illumination on models and visible ground. Vanilla's logical light level stays separate. */
public final class UvLighting {
    public static final int TINT=0xFFD29AFF;
    private UvLighting(){}
    public static int multiply(int a,int b){return (a>>>24)*(b>>>24)/255<<24|((a>>>16&255)*(b>>>16&255)/255)<<16|((a>>>8&255)*(b>>>8&255)/255)<<8|(a&255)*(b&255)/255;}
    public static float intensity(double x,double y,double z){double drop=1.56-y;if(drop<=0||drop>2.9)return 0;double radius=Math.max(.22,drop*.86),r=Math.hypot(x-.5,z-.5)/radius;return (float)(Math.max(0,1-r*r)*Math.max(0,1-drop/3.2));}
    public static void spill(PoseStack pose,MultiBufferSource buffers,Planter pot){
        var level=pot.getLevel();if(level==null||!pot.hasLamp())return;var base=pot.getBlockPos();var source=new Vec3(base.getX()+.5,base.getY()+1.56,base.getZ()+.5);
        var out=buffers.getBuffer(RenderType.lightning());
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=-1;dy<=0;dy++){
            BlockPos block=base.offset(dx,dy,dz);if(!level.hasChunkAt(block))continue;var shape=level.getBlockState(block).getCollisionShape(level,block);if(shape.isEmpty())continue;
            for(var bounds:shape.toAabbs()){double y=dy+bounds.maxY+.004;
            var center=new Vec3(block.getX()+.5,base.getY()+y-.002,block.getZ()+.5);
            var hit=level.clip(new ClipContext(source,center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));if(hit.getType()==HitResult.Type.BLOCK&&!hit.getBlockPos().equals(block))continue;
            for(int x=0;x<4;x++)for(int z=0;z<4;z++){
                double x0=dx+bounds.minX+(bounds.maxX-bounds.minX)*x/4,x1=dx+bounds.minX+(bounds.maxX-bounds.minX)*(x+1)/4,z0=dz+bounds.minZ+(bounds.maxZ-bounds.minZ)*z/4,z1=dz+bounds.minZ+(bounds.maxZ-bounds.minZ)*(z+1)/4;
                vertex(pose,out,x0,y,z0);vertex(pose,out,x0,y,z1);vertex(pose,out,x1,y,z1);vertex(pose,out,x1,y,z0);
            }
        }
        }
        // Visible light on the upper soil surface of this pot, without a solid beam or white square.
        pose.pushPose();pose.translate(0,.13,0);double y=pl.crimeandlaw.cultivation.PreparationPhysics.TOP+.006;if(pot.plant.soil()){
            for(int i=0;i<16;i++){double a=i*Math.PI/8,b=(i+1)*Math.PI/8,r=.285;
                color(pose,out,.5,y,.5,.22f);color(pose,out,.5+Math.cos(b)*r,y,.5+Math.sin(b)*r,0);color(pose,out,.5+Math.cos(a)*r,y,.5+Math.sin(a)*r,0);color(pose,out,.5,y,.5,.22f);
            }
        }pose.popPose();
    }
    private static void vertex(PoseStack p,VertexConsumer out,double x,double y,double z){color(p,out,x,y,z,intensity(x,y,z)*.70f);}
    private static void color(PoseStack p,VertexConsumer out,double x,double y,double z,float a){out.addVertex(p.last().pose(),(float)x,(float)y,(float)z).setColor(.62f,.12f,1,Math.clamp(a,0,.55f));}
}
