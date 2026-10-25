package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Curved, double-sided leaf surfaces shared by the world and the pruning camera. */
public final class AureliaMesh {
    public static final ResourceLocation LEAF=ResourceLocation.fromNamespaceAndPath("crimeandlaw","textures/plant/aurelia_leaf.png");
    private static final ResourceLocation STEM=ResourceLocation.withDefaultNamespace("textures/block/stripped_spruce_log.png");
    public record Leaf(int id,Vec3 base,float angle,float length,float width){
        public Vec3 point(float t,float side){
            double d=length*t,c=Math.cos(angle),s=Math.sin(angle),lift=.10*Math.sin(t*Math.PI)-.11*t*t;
            return base.add(c*d-s*side*width/2,lift-.045*Math.abs(side)*Math.sin(t*Math.PI),s*d+c*side*width/2);
        }
        public Vec3 marker(){return point(.39f,0);}
    }
    public static Leaf leaf(int id){
        int tier=id/2;float angle=(float)(tier*1.09+(id%2)*Math.PI+.22);
        return new Leaf(id,new Vec3(.5,.40+tier*.205,.5),angle,.48f-tier*.035f,.38f-tier*.025f);
    }
    private AureliaMesh(){}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int age,int mask,int target,float time,int light){
        float growth=.24f+age*.19f;
        pose.pushPose();pose.translate(.5,0,.5);pose.scale(growth,growth,growth);pose.translate(-.5,0,-.5);
        var stem=buffers.getBuffer(RenderType.entityCutoutNoCull(STEM));
        stem(pose,stem,new Vec3(.5,.025,.5),new Vec3(.5,.98,.5),.018f,light,0xFF7C9D63);
        for(int id=0;id<6;id++)if((mask&(1<<id))!=0){var leaf=leaf(id);stem(pose,stem,leaf.base,leaf.point(.25f,0),.006f,light,0xFF8BA87A);}
        var leaves=buffers.getBuffer(RenderType.entityCutoutNoCull(LEAF));
        for(int id=0;id<6;id++)if((mask&(1<<id))!=0)drawLeaf(pose,leaves,leaf(id),light,id==target?0xFFFFFFC6:0xFFFFFFFF,time,id==target);
        for(int id=0;id<4;id++)drawLeaf(pose,leaves,new Leaf(6+id,new Vec3(.5,.85+id*.033,.5),(float)(id*Math.PI/2+.5),.19f,.145f),light,0xFFC2EF9B,time,false);
        pose.popPose();
    }
    public static void drawLeaf(PoseStack pose,VertexConsumer out,Leaf leaf,int light,int color,float time,boolean selected){
        float sway=(float)Math.sin(time*.7+leaf.id)*.006f;
        pose.pushPose();pose.translate(0,sway,0);
        for(int row=0;row<10;row++)for(int col=0;col<2;col++){
            float t0=row/10f,t1=(row+1)/10f,s0=col-1,s1=col;
            vertex(pose,out,leaf.point(t0,s0),(s0+1)/2,1-t0,light,color);
            vertex(pose,out,leaf.point(t1,s0),(s0+1)/2,1-t1,light,color);
            vertex(pose,out,leaf.point(t1,s1),(s1+1)/2,1-t1,light,color);
            vertex(pose,out,leaf.point(t0,s1),(s1+1)/2,1-t0,light,color);
        }
        pose.popPose();
    }
    private static void stem(PoseStack pose,VertexConsumer out,Vec3 a,Vec3 b,float radius,int light,int color){
        Vec3 direction=b.subtract(a).normalize(),u=direction.cross(new Vec3(0,0,1)).normalize();if(u.lengthSqr()<.1)u=new Vec3(1,0,0);Vec3 v=direction.cross(u).normalize();
        for(int side=0;side<6;side++){
            double p=side*Math.PI/3,q=(side+1)*Math.PI/3;
            Vec3 x=u.scale(Math.cos(p)*radius).add(v.scale(Math.sin(p)*radius)),y=u.scale(Math.cos(q)*radius).add(v.scale(Math.sin(q)*radius));
            vertex(pose,out,a.add(x),0,0,light,color);vertex(pose,out,b.add(x),0,1,light,color);vertex(pose,out,b.add(y),1,1,light,color);vertex(pose,out,a.add(y),1,0,light,color);
        }
    }
    private static void vertex(PoseStack p,VertexConsumer v,Vec3 point,float u,float w,int light,int color){
        v.addVertex(p.last().pose(),(float)point.x,(float)point.y,(float)point.z).setColor(color).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p.last(),0,1,0);
    }
}
