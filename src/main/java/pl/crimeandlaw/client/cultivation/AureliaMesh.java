package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import pl.crimeandlaw.cultivation.PlantShape;

/** Angular, thick pixel leaves and square stems, shared by the world and harvest camera. */
public final class AureliaMesh {
    public static final ResourceLocation LEAF=PixelSprite.LEAF.texture();
    public record Leaf(int id,Vec3 base,float angle,float length,float width,float rise,float arch,float petiole,float roll,float cup,float droop){
        public Vec3 point(float t,float side){
            double d=petiole+(length-petiole)*t,c=Math.cos(angle),s=Math.sin(angle),across=side*width/2*Math.cos(roll);
            double lift=petiole*.35+rise*t+4*arch*t*(1-t)-droop*t*t+side*width/2*Math.sin(roll)-cup*side*side*Math.sin(Math.PI*t);
            return base.add(c*d-s*across,lift,s*d+c*across);
        }
        public Vec3 marker(){return point(.12f,0);}
        public double minY(){return base.y+petiole*.35+Math.min(0,rise-droop)-cup-Math.abs(width/2*Math.sin(roll))-PlantShape.HALF_THICKNESS;}
        public double maxY(){return base.y+petiole*.35+Math.max(0,rise-droop)+arch+droop/4+Math.abs(width/2*Math.sin(roll))+PlantShape.HALF_THICKNESS;}
    }
    private static final Map<Long,List<Leaf>> SHAPES=new LinkedHashMap<>(256,.75f,true){protected boolean removeEldestEntry(Map.Entry<Long,List<Leaf>> e){return size()>256;}};
    public static Leaf leaf(int id){return leaf(id,8,0);}
    public static Leaf leaf(int id,int total){return leaf(id,total,0);}
    public static Leaf leaf(int id,int total,long seed){
        return SHAPES.computeIfAbsent(seed,k->{var list=new ArrayList<Leaf>(8);int index=0;for(var l:PlantShape.create(k))list.add(new Leaf(index++,new Vec3(.5,l.height(),.5),(float)l.angle(),(float)l.length(),(float)l.width(),(float)l.rise(),(float)l.arch(),(float)l.petiole(),(float)l.roll(),(float)l.cup(),(float)l.droop()));return List.copyOf(list);}).get(id);
    }
    private AureliaMesh(){}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int age,int mask,int target,float time,int light){draw(pose,buffers,age,mask,target,time,light,8);}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int age,int mask,int target,float time,int light,int total){draw(pose,buffers,age,mask,target,time,light,total,-1);}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int age,int mask,int target,float time,int light,int total,int tint){draw(pose,buffers,age,mask,target,time,light,total,tint,0);}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int age,int mask,int target,float time,int light,int total,int tint,long seed){
        float growth=age==4?1:.24f+age*.19f;
        pose.pushPose();pose.translate(.5,0,.5);pose.scale(growth,growth,growth);pose.translate(-.5,0,-.5);
        var stem=buffers.getBuffer(RenderType.entityCutoutNoCull(PixelSprite.STEM));
        for(int i=0;i<4;i++)stem(pose,stem,new Vec3(.5,.025+i*.225,.5),new Vec3(.5,.025+(i+1)*.225,.5),.016f-i*.0022f,light,tint);
        for(int id=0;id<8;id++)if((mask&(1<<id))!=0){var leaf=leaf(id,total,seed);var tip=leaf.point(0,0);var mid=leaf.base.lerp(tip,.5).add(0,.006,0);stem(pose,stem,leaf.base,mid,.0065f,light,tint);stem(pose,stem,mid,tip,.0055f,light,tint);}
        var leaves=buffers.getBuffer(RenderType.entityCutoutNoCull(LEAF));
        for(int id=0;id<8;id++)if((mask&(1<<id))!=0)drawLeaf(pose,leaves,leaf(id,total,seed),light,id==target?0xFFFFFFCE:tint,time,id==target);
        pose.popPose();
    }
    public static void drawLeaf(PoseStack pose,VertexConsumer out,Leaf leaf,int light,int color,float time,boolean selected){
        PixelSprite.LEAF.draw(pose,out,(u,v,d)->leaf.point(1-v,u*2-1).add(0,d,0),(float)PlantShape.HALF_THICKNESS,color,light,true);
    }
    private static void stem(PoseStack pose,VertexConsumer out,Vec3 a,Vec3 b,float radius,int light,int tint){
        Vec3 direction=b.subtract(a).normalize(),u=direction.cross(new Vec3(0,0,1)).normalize();if(u.lengthSqr()<.1)u=new Vec3(1,0,0);Vec3 v=direction.cross(u).normalize();
        Vec3[] corners=new Vec3[4];for(int i=0;i<4;i++){double p=Math.PI/4+i*Math.PI/2;corners[i]=u.scale(Math.cos(p)*radius).add(v.scale(Math.sin(p)*radius));}
        for(int i=0;i<4;i++)PixelSprite.quad(pose,out,a.add(corners[i]),b.add(corners[i]),b.add(corners[(i+1)%4]),a.add(corners[(i+1)%4]),0,0,1,1,light,tint);
        PixelSprite.quad(pose,out,b.add(corners[0]),b.add(corners[1]),b.add(corners[2]),b.add(corners[3]),0,0,1,1,light,tint);
    }
}
