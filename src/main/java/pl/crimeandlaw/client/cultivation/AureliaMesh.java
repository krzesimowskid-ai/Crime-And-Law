package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Angular, thick pixel leaves and square stems, shared by the world and harvest camera. */
public final class AureliaMesh {
    public static final ResourceLocation LEAF=PixelSprite.LEAF.texture();
    public record Leaf(int id,Vec3 base,float angle,float length,float width){
        public Vec3 point(float t,float side){
            double d=length*t,c=Math.cos(angle),s=Math.sin(angle);
            double lift=t<.42?t*.16:t<.72?.0672-(t-.42)*.10:.0372-(t-.72)*.28;
            return base.add(c*d-s*side*width/2,lift-.019*Math.abs(side),s*d+c*side*width/2);
        }
        public Vec3 marker(){return point(.25f,0);}
    }
    public static Leaf leaf(int id){return leaf(id,8);}
    public static Leaf leaf(int id,int total){
        int tier=id/2;int tiers=Math.max(1,(total+1)/2);float angle=(float)(tier*1.09+(id%2)*Math.PI+.22);
        return new Leaf(id,new Vec3(.5,.36+tier*.46/Math.max(1,tiers-1),.5),angle,.48f-tier*.035f,.38f-tier*.025f);
    }
    private AureliaMesh(){}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int age,int mask,int target,float time,int light){draw(pose,buffers,age,mask,target,time,light,8);}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int age,int mask,int target,float time,int light,int total){
        float growth=.24f+age*.19f;
        pose.pushPose();pose.translate(.5,0,.5);pose.scale(growth,growth,growth);pose.translate(-.5,0,-.5);
        var stem=buffers.getBuffer(RenderType.entityCutoutNoCull(PixelSprite.STEM));
        stem(pose,stem,new Vec3(.5,.025,.5),new Vec3(.5,.87,.5),.021f,light);
        for(int id=0;id<8;id++)if((mask&(1<<id))!=0){var leaf=leaf(id,total);stem(pose,stem,leaf.base,leaf.point(.25f,0),.008f,light);}
        var leaves=buffers.getBuffer(RenderType.entityCutoutNoCull(LEAF));
        for(int id=0;id<8;id++)if((mask&(1<<id))!=0)drawLeaf(pose,leaves,leaf(id,total),light,id==target?0xFFFFFFCE:0xFFFFFFFF,time,id==target);
        pose.popPose();
    }
    public static void drawLeaf(PoseStack pose,VertexConsumer out,Leaf leaf,int light,int color,float time,boolean selected){
        PixelSprite.LEAF.draw(pose,out,(u,v,d)->leaf.point(1-v,u*2-1).add(0,d,0),.0065f,color,light,true);
    }
    private static void stem(PoseStack pose,VertexConsumer out,Vec3 a,Vec3 b,float radius,int light){
        Vec3 direction=b.subtract(a).normalize(),u=direction.cross(new Vec3(0,0,1)).normalize();if(u.lengthSqr()<.1)u=new Vec3(1,0,0);Vec3 v=direction.cross(u).normalize();
        Vec3[] corners=new Vec3[4];for(int i=0;i<4;i++){double p=Math.PI/4+i*Math.PI/2;corners[i]=u.scale(Math.cos(p)*radius).add(v.scale(Math.sin(p)*radius));}
        for(int i=0;i<4;i++)PixelSprite.quad(pose,out,a.add(corners[i]),b.add(corners[i]),b.add(corners[(i+1)%4]),a.add(corners[(i+1)%4]),0,0,1,1,light,0xFFFFFFFF);
        PixelSprite.quad(pose,out,b.add(corners[0]),b.add(corners[1]),b.add(corners[2]),b.add(corners[3]),0,0,1,1,light,0xFFFFFFFF);
    }
}
