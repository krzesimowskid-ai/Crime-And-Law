package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import pl.crimeandlaw.cultivation.Planter;
import pl.crimeandlaw.cultivation.PreparationPhysics;

/** Bounded local illumination. Collision/occlusion snapshots are cached, not raycast every frame. */
public final class UvLighting {
    public static final int TINT=0xFFD29AFF;
    private record Patch(double x0,double y,double z0,double x1,double z1){}
    private record Cached(long stamp,List<Patch> patches){}
    private static final Map<Planter,Cached> CACHE=new WeakHashMap<>();
    private UvLighting(){}
    public static int multiply(int a,int b){return (a>>>24)*(b>>>24)/255<<24|((a>>>16&255)*(b>>>16&255)/255)<<16|((a>>>8&255)*(b>>>8&255)/255)<<8|(a&255)*(b&255)/255;}
    public static float intensity(double x,double y,double z){double drop=1.56-y;if(drop<=0||drop>2.9)return 0;double radius=Math.max(.22,drop*.86),r=Math.hypot(x-.5,z-.5)/radius;return (float)(Math.max(0,1-r*r)*Math.max(0,1-drop/3.2));}
    private static List<Patch> surfaces(Planter pot){
        var level=pot.getLevel();var base=pot.getBlockPos();long stamp=(level.getGameTime()+Math.floorMod(base.asLong(),10))/10;var cached=CACHE.get(pot);if(cached!=null&&cached.stamp==stamp)return cached.patches;
        var list=new ArrayList<Patch>();var source=new Vec3(base.getX()+.5,base.getY()+1.56,base.getZ()+.5);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=-1;dy<=0;dy++){
            BlockPos block=base.offset(dx,dy,dz);if(!level.hasChunkAt(block))continue;var shape=level.getBlockState(block).getCollisionShape(level,block);if(shape.isEmpty())continue;
            for(var bounds:shape.toAabbs()){
                double y=dy+bounds.maxY+.004;if(intensity(dx+.5,y,dz+.5)<=0)continue;
                var center=new Vec3(block.getX()+(bounds.minX+bounds.maxX)/2,base.getY()+y-.002,block.getZ()+(bounds.minZ+bounds.maxZ)/2);
                var hit=level.clip(new ClipContext(source,center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,CollisionContext.empty()));if(hit.getType()==HitResult.Type.BLOCK&&!hit.getBlockPos().equals(block))continue;
                list.add(new Patch(dx+bounds.minX,y,dz+bounds.minZ,dx+bounds.maxX,dz+bounds.maxZ));
            }
        }
        var result=List.copyOf(list);CACHE.put(pot,new Cached(stamp,result));return result;
    }
    public static void spill(PoseStack pose,MultiBufferSource buffers,Planter pot){
        if(pot.getLevel()==null||!pot.hasLamp())return;var out=buffers.getBuffer(RenderType.lightning());
        for(var b:surfaces(pot))for(int x=0;x<4;x++)for(int z=0;z<4;z++){
            double x0=b.x0+(b.x1-b.x0)*x/4,x1=b.x0+(b.x1-b.x0)*(x+1)/4,z0=b.z0+(b.z1-b.z0)*z/4,z1=b.z0+(b.z1-b.z0)*(z+1)/4;
            vertex(pose,out,x0,b.y,z0);vertex(pose,out,x0,b.y,z1);vertex(pose,out,x1,b.y,z1);vertex(pose,out,x1,b.y,z0);
        }
        if(pot.plant.soil()){
            pose.pushPose();pose.translate(0,.13,0);double y=PreparationPhysics.TOP+.006;
            for(int i=0;i<16;i++){double a=i*Math.PI/8,b=(i+1)*Math.PI/8,r=.28;color(pose,out,.5,y,.5,.22f);color(pose,out,.5+Math.cos(b)*r,y,.5+Math.sin(b)*r,0);color(pose,out,.5+Math.cos(a)*r,y,.5+Math.sin(a)*r,0);color(pose,out,.5,y,.5,.22f);}pose.popPose();
        }
    }
    private static void vertex(PoseStack p,VertexConsumer out,double x,double y,double z){color(p,out,x,y,z,intensity(x,y,z)*.70f);}
    private static void color(PoseStack p,VertexConsumer out,double x,double y,double z,float a){out.addVertex(p.last().pose(),(float)x,(float)y,(float)z).setColor(.62f,.12f,1,Math.clamp(a,0,.55f));}
}
