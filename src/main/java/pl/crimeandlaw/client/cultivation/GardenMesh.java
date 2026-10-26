package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import pl.crimeandlaw.cultivation.*;

/** Hollow stepped pot, sewn sacks, steel watering can and bolted two-block rack. */
public final class GardenMesh {
    private GardenMesh(){}
    private static int tint=-1;
    public static void box(PoseStack p,MultiBufferSource b,ResourceLocation material,double x0,double y0,double z0,double x1,double y1,double z1,int light,int color){
        var v=b.getBuffer(RenderType.entityCutoutNoCull(material));
        Vec3 a=new Vec3(x0,y0,z0),c=new Vec3(x1,y1,z1);
        quad(p,v,new Vec3(x0,y0,z0),new Vec3(x0,y1,z0),new Vec3(x1,y1,z0),new Vec3(x1,y0,z0),light,color);
        quad(p,v,new Vec3(x1,y0,z1),new Vec3(x1,y1,z1),new Vec3(x0,y1,z1),new Vec3(x0,y0,z1),light,color);
        quad(p,v,new Vec3(x0,y0,z1),new Vec3(x0,y1,z1),new Vec3(x0,y1,z0),new Vec3(x0,y0,z0),light,color);
        quad(p,v,new Vec3(x1,y0,z0),new Vec3(x1,y1,z0),new Vec3(x1,y1,z1),new Vec3(x1,y0,z1),light,color);
        quad(p,v,new Vec3(x0,y1,z0),new Vec3(x0,y1,z1),new Vec3(x1,y1,z1),new Vec3(x1,y1,z0),light,color);
        quad(p,v,new Vec3(x0,y0,z1),new Vec3(x0,y0,z0),new Vec3(x1,y0,z0),new Vec3(x1,y0,z1),light,color);
    }
    private static void quad(PoseStack p,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int l,int col){PixelSprite.quad(p,v,a,b,c,d,0,0,1,1,l,UvLighting.multiply(col,tint));}
    private static Vec3 ring(double r,double y,int i){double a=Math.PI/8+i*Math.PI/4;return new Vec3(.5+Math.cos(a)*r,y,.5+Math.sin(a)*r);}
    private static void band(PoseStack p,MultiBufferSource b,double r0,double y0,double r1,double y1,int l,int c){var v=b.getBuffer(RenderType.entityCutoutNoCull(GardenMaterials.PLASTIC));for(int i=0;i<8;i++)quad(p,v,ring(r0,y0,i),ring(r1,y1,i),ring(r1,y1,i+1),ring(r0,y0,i+1),l,c);}
    public static void pot(PoseStack p,MultiBufferSource b,float fill,boolean wet,int light){pot(p,b,fill,wet,light,0);}
    public static void pot(PoseStack p,MultiBufferSource b,float fill,boolean wet,int light,float hole){
        band(p,b,.235,.025,.25,.07,light,0xFFB8C1C7);band(p,b,.25,.07,.35,.41,light,-1);
        band(p,b,.35,.40,.375,.415,light,0xFFB9C2C8);band(p,b,.375,.415,.375,.48,light,-1);band(p,b,.375,.48,.318,.48,light,0xFFD2DADE);
        band(p,b,.318,.48,.310,.43,light,0xFFB6BFC4);band(p,b,.310,.43,.215,.055,light,0xFF9BA6AD);
        var out=b.getBuffer(RenderType.entityCutoutNoCull(GardenMaterials.PLASTIC));for(int i=0;i<8;i++)quad(p,out,new Vec3(.5,.035,.5),ring(.228,.035,i),ring(.228,.035,i+1),new Vec3(.5,.035,.5),light,0xFF929DA5);
        if(fill>0){double y=PreparationPhysics.soilY(fill),radius=PreparationPhysics.soilRadius(fill);var v=b.getBuffer(RenderType.entityCutoutNoCull(GardenMaterials.SOIL));
            // Concentric rings match the tapered cavity at every fill height.
            for(int i=0;i<8;i++){Vec3 a=ring(radius,y,i),c=ring(radius,y,i+1);Vec3 aa=ring(.068,y-hole*.033,i),cc=ring(.068,y-hole*.033,i+1);soilFace(p,v,aa,cc,c,a,radius,light,wet?0xFFB6B0A6:-1);soilFace(p,v,new Vec3(.5,y-hole*.04,.5),cc,aa,new Vec3(.5,y-hole*.04,.5),radius,light,wet?0xFFADA598:0xFFC3B49B);}
        }
    }
    private static void soilFace(PoseStack p,VertexConsumer out,Vec3 a,Vec3 b,Vec3 c,Vec3 d,double r,int light,int color){
        var normal=b.subtract(a).cross(c.subtract(a)).normalize();color=UvLighting.multiply(color,tint);
        for(var v:new Vec3[]{a,b,c,d})out.addVertex(p.last().pose(),(float)v.x,(float)v.y,(float)v.z).setColor(color).setUv((float)((v.x-.5)/(2*r)+.5),(float)((v.z-.5)/(2*r)+.5)).setLight(light).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(p.last(),(float)normal.x,(float)normal.y,(float)normal.z);
    }
    public static void frame(PoseStack p,MultiBufferSource b,boolean lamp,int light){
        for(double x:new double[]{.07,.87})for(double z:new double[]{.07,.87}){box(p,b,GardenMaterials.STEEL,x,0,z,x+.06,1.96,z+.06,light,0xFFFFFFFF);for(double y:new double[]{.08,.92,1.86})box(p,b,GardenMaterials.STEEL,x-.009,y,z-.009,x+.069,y+.038,z+.069,light,0xFF59666C);}
        for(double y:new double[]{.04,1.89}){box(p,b,GardenMaterials.STEEL,.07,y,.07,.93,y+.055,.13,light,-1);box(p,b,GardenMaterials.STEEL,.07,y,.87,.93,y+.055,.93,light,-1);box(p,b,GardenMaterials.STEEL,.07,y,.13,.13,y+.055,.87,light,-1);box(p,b,GardenMaterials.STEEL,.87,y,.13,.93,y+.055,.87,light,-1);}
        box(p,b,GardenMaterials.STEEL,.16,.075,.16,.84,.13,.84,light,0xFF809094);
        if(lamp){box(p,b,GardenMaterials.STEEL,.095,1.875,.455,.905,1.945,.545,light,-1);for(double x:new double[]{.255,.695}){box(p,b,GardenMaterials.STEEL,x-.025,1.85,.425,x+.065,1.97,.575,light,0xFFB6C7C8);box(p,b,GardenMaterials.STEEL,x-.01,1.65,.45,x+.05,1.73,.55,light,-1);}for(double x:new double[]{.27,.70})box(p,b,GardenMaterials.STEEL,x,1.64,.48,x+.025,1.9,.505,light,0xFF5F6A73);p.pushPose();p.translate(0,1.31,0);lamp(p,b,light);p.popPose();}
    }
    public static void lamp(PoseStack p,MultiBufferSource b,int light){
        box(p,b,GardenMaterials.STEEL,.17,.30,.18,.83,.37,.82,light,0xFFD4DDCF);box(p,b,GardenMaterials.STEEL,.22,.27,.22,.78,.30,.78,light,0xFF29313E);
        for(int i=0;i<4;i++){double z=.28+i*.13;box(p,b,GardenMaterials.STEEL,.21,.23,z-.025,.26,.29,z+.06,light,0xFF88978E);box(p,b,GardenMaterials.STEEL,.74,.23,z-.025,.79,.29,z+.06,light,0xFF88978E);box(p,b,GardenMaterials.UV,.26,.235,z,.74,.275,z+.035,LightTexture.FULL_BRIGHT,-1);}
        box(p,b,GardenMaterials.STEEL,.41,.37,.44,.59,.43,.56,light,0xFF566267);
    }
    public static void bag(PoseStack p,MultiBufferSource b,int capacity,boolean packet,boolean open,int light){bag(p,b,capacity,packet,open?1:0,light);}
    public static void bag(PoseStack p,MultiBufferSource b,int capacity,boolean packet,float cut,int light){
        var mat=packet?GardenMaterials.PACK:capacity==1?GardenMaterials.BAG1:capacity==2?GardenMaterials.BAG2:GardenMaterials.BAG3;
        double[][] bands={{.28,.08,.35,.72,.65},{.22,.16,.30,.78,.70},{.185,.33,.275,.815,.725},{.205,.65,.29,.795,.71},{.26,.78,.345,.74,.655}};
        var v=b.getBuffer(RenderType.entityCutoutNoCull(mat));
        for(int j=0;j<bands.length-1;j++){var a=bands[j];var c=bands[j+1];
            // Continuous UV coordinates across the shaped body, instead of restarting the fabric on every band.
            bagFace(p,v,new Vec3(a[0],a[1],a[2]),new Vec3(c[0],c[1],c[2]),new Vec3(c[3],c[1],c[2]),new Vec3(a[3],a[1],a[2]),a[1],c[1],light,-1);
            bagFace(p,v,new Vec3(a[3],a[1],a[4]),new Vec3(c[3],c[1],c[4]),new Vec3(c[0],c[1],c[4]),new Vec3(a[0],a[1],a[4]),a[1],c[1],light,-1);
            bagFace(p,v,new Vec3(a[0],a[1],a[4]),new Vec3(c[0],c[1],c[4]),new Vec3(c[0],c[1],c[2]),new Vec3(a[0],a[1],a[2]),a[1],c[1],light,0xFFD0D8CB);
            bagFace(p,v,new Vec3(a[3],a[1],a[2]),new Vec3(c[3],c[1],c[2]),new Vec3(c[3],c[1],c[4]),new Vec3(a[3],a[1],a[4]),a[1],c[1],light,0xFFD0D8CB);
        }
        quad(p,v,new Vec3(.28,.08,.35),new Vec3(.72,.08,.35),new Vec3(.72,.08,.65),new Vec3(.28,.08,.65),light,0xFFBDD0BC);
        // Attached mouth and folded lips. No rotating ribbon protrudes from the side of an opened bag.
        box(p,b,mat,.26,.78,.345,.74,.835,.365,light,-1);
        box(p,b,mat,.26,.78,.635,.74,.835,.655,light,-1);
        box(p,b,mat,.26,.78,.365,.275,.835,.635,light,0xFFD0D8CB);
        box(p,b,mat,.725,.78,.365,.74,.835,.635,light,0xFFD0D8CB);
        double opened=Math.clamp(cut,0,1),edge=.275+opened*.45;
        if(opened<1)box(p,b,mat,edge,.818,.365,.725,.835,.635,light,-1);
        if(opened>0)box(p,b,GardenMaterials.SOIL,.275,.805,.365,edge,.811,.635,light,-1);
        // Labels follow the actual sloping fabric, including the shoulder and lower taper.
        var label=packet?GardenMaterials.PACK_LABEL:capacity==1?GardenMaterials.LABEL1:capacity==2?GardenMaterials.LABEL2:GardenMaterials.LABEL3;
        for(int j=0;j<bands.length-1;j++){
            var a=bands[j];var c=bands[j+1];double low=Math.max(.20,a[1]),high=Math.min(.69,c[1]);
            if(high<=low)continue;
            var out=b.getBuffer(RenderType.entityCutoutNoCull(label));
            for(boolean back:new boolean[]{false,true}){
                var lo0=bagLabel(a,c,low,back,false);var lo1=bagLabel(a,c,low,back,true);
                var hi0=bagLabel(a,c,high,back,false);var hi1=bagLabel(a,c,high,back,true);
                PixelSprite.quad(p,out,lo0,hi0,hi1,lo1,0,(float)((.69-low)/.49),1,(float)((.69-high)/.49),light,UvLighting.multiply(-1,tint));
            }
        }
    }
    private static void bagFace(PoseStack p,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,double low,double high,int light,int color){
        PixelSprite.quad(p,v,a,b,c,d,0,(float)(1-low),1,(float)(1-high),light,UvLighting.multiply(color,tint));
    }
    private static Vec3 bagLabel(double[] a,double[] b,double y,boolean back,boolean right){
        double t=(y-a[1])/(b[1]-a[1]),left=a[0]+(b[0]-a[0])*t,r=a[3]+(b[3]-a[3])*t;
        double u=right?.91:.09;if(back)u=1-u;
        int face=back?4:2;double z=a[face]+(b[face]-a[face])*t+(back?.0008:-.0008);
        return new Vec3(left+(r-left)*u,y,z);
    }
    public static void seed(PoseStack p,MultiBufferSource b,int light){
        var out=b.getBuffer(RenderType.entityCutoutNoCull(GardenMaterials.SEED));double[] x={-.045,-.029,.008,.032,.044},ry={.001,.014,.018,.010,.001},rz={.001,.020,.024,.013,.001};
        for(int k=0;k<4;k++)for(int i=0;i<8;i++){double a=i*Math.PI/4,c=(i+1)*Math.PI/4;quad(p,out,new Vec3(x[k],Math.cos(a)*ry[k],Math.sin(a)*rz[k]),new Vec3(x[k+1],Math.cos(a)*ry[k+1],Math.sin(a)*rz[k+1]),new Vec3(x[k+1],Math.cos(c)*ry[k+1],Math.sin(c)*rz[k+1]),new Vec3(x[k],Math.cos(c)*ry[k],Math.sin(c)*rz[k]),light,i<4?0xFFE8D7B0:0xFFB9A078);}
        var seam=b.getBuffer(RenderType.entityCutoutNoCull(GardenMaterials.WHITE));quad(p,seam,new Vec3(-.028,.0148,-.0012),new Vec3(.008,.0188,-.0012),new Vec3(.008,.0188,.0012),new Vec3(-.028,.0148,.0012),light,0xFFDDCAA1);
    }
    public static void waterStream(PoseStack p,MultiBufferSource b,PreparationPhysics.Stream stream,double age,int light){
        if(!stream.flowing())return;boolean hits=PreparationPhysics.reachesSoil(stream,1);double end=PreparationPhysics.flight(stream.source(),stream.velocity(),hits?PreparationPhysics.TOP:.015);if(end<0)return;double start=Math.max(0,age-.055);end=Math.min(end,age);if(end<=start)return;
        var out=b.getBuffer(RenderType.entityTranslucent(GardenMaterials.WATER));double r=.0018;
        for(int i=0;i<4;i++){var a=PreparationPhysics.at(stream,start+(end-start)*i/4);var c=PreparationPhysics.at(stream,start+(end-start)*(i+1)/4);quad(p,out,new Vec3(a.x()-r,a.y(),a.z()),new Vec3(c.x()-r,c.y(),c.z()),new Vec3(c.x()+r,c.y(),c.z()),new Vec3(a.x()+r,a.y(),a.z()),light,0xAAB9E7F5);quad(p,out,new Vec3(a.x(),a.y(),a.z()-r),new Vec3(c.x(),c.y(),c.z()-r),new Vec3(c.x(),c.y(),c.z()+r),new Vec3(a.x(),a.y(),a.z()+r),light,0x88B9E7F5);}
    }
    public static void landingGuide(PoseStack p,MultiBufferSource b,double x,double z,boolean valid,int light){
        var out=b.getBuffer(RenderType.entityTranslucent(GardenMaterials.WHITE));double r=valid?.047:.037;int col=valid?0xDADBC080:0x7793968E;
        for(int i=0;i<32;i++){double a=i*Math.PI/16,c=(i+1)*Math.PI/16;quad(p,out,guidePoint(x,z,a,r),guidePoint(x,z,c,r),guidePoint(x,z,c,r-.003),guidePoint(x,z,a,r-.003),light,col);}
    }
    private static Vec3 guidePoint(double x,double z,double angle,double r){double xx=x+Math.cos(angle)*r,zz=z+Math.sin(angle)*r;return new Vec3(xx,PreparationPhysics.seedBedY(xx,zz)+.0015,zz);}
    public static void knife(PoseStack p,MultiBufferSource b,int light){box(p,b,GardenMaterials.PACK,-.12,-.025,-.025,0,.025,.025,light,-1);box(p,b,GardenMaterials.STEEL,0,-.010,-.012,.19,.010,.012,light,0xFFE5EEE8);}
    public static void glove(PoseStack p,MultiBufferSource b,int light){
        box(p,b,GardenMaterials.GLOVE,-.044,.002,-.024,.044,.030,.046,light,-1);box(p,b,GardenMaterials.GLOVE,-.041,.005,.043,.041,.026,.069,light,0xFF6A7779);
        for(int i=0;i<4;i++){double x=-.032+i*.022,end=-.097+(i==0?.014:i==3?.021:0);finger(p,b,x,end,light);}
        p.pushPose();p.translate(.036,.006,.012);p.mulPose(Axis.YP.rotationDegrees(-40));box(p,b,GardenMaterials.GLOVE,0,0,-.048,.018,.018,.010,light,0xFFE7E5D9);p.popPose();
    }
    private static void finger(PoseStack p,MultiBufferSource b,double x,double end,int light){
        var out=b.getBuffer(RenderType.entityCutoutNoCull(GardenMaterials.GLOVE));double[] zs={-.018,end+.014,end},rs={.010,.009,.002};
        for(int k=0;k<2;k++)for(int i=0;i<8;i++){double a=i*Math.PI/4,c=(i+1)*Math.PI/4;quad(p,out,new Vec3(x+Math.cos(a)*rs[k],.015+Math.sin(a)*rs[k],zs[k]),new Vec3(x+Math.cos(a)*rs[k+1],.015+Math.sin(a)*rs[k+1],zs[k+1]),new Vec3(x+Math.cos(c)*rs[k+1],.015+Math.sin(c)*rs[k+1],zs[k+1]),new Vec3(x+Math.cos(c)*rs[k],.015+Math.sin(c)*rs[k],zs[k]),light,-1);}
    }
    public static void can(PoseStack p,MultiBufferSource b,int water,int light){
        box(p,b,GardenMaterials.STEEL,.20,.12,.29,.66,.57,.71,light,-1);box(p,b,GardenMaterials.STEEL,.17,.12,.26,.69,.17,.74,light,0xFF829995);box(p,b,GardenMaterials.STEEL,.18,.54,.27,.68,.59,.73,light,0xFFD9DFCC);
        box(p,b,GardenMaterials.STEEL,.25,.59,.34,.56,.62,.66,light,0xFF4B6564);box(p,b,GardenMaterials.WATER,.30,.612,.39,.51,.616,.61,light,water>0?0xFFFFFFFF:0xFF26332D);
        box(p,b,GardenMaterials.STEEL,.13,.57,.44,.19,.82,.56,light,-1);box(p,b,GardenMaterials.STEEL,.13,.78,.44,.62,.84,.56,light,-1);box(p,b,GardenMaterials.STEEL,.56,.57,.44,.62,.82,.56,light,-1);
        p.pushPose();p.translate(.61,.26,.44);p.mulPose(Axis.ZP.rotationDegrees(36));box(p,b,GardenMaterials.STEEL,0,-.04,0,.43,.05,.12,light,0xFFB7CFC3);box(p,b,GardenMaterials.STEEL,.4,-.08,-.05,.46,.10,.17,light,0xFFD0DFCF);for(int i=0;i<3;i++)for(int j=0;j<3;j++)box(p,b,GardenMaterials.WHITE,.461,-.05+i*.055,-.02+j*.055,.464,-.027+i*.055,.003+j*.055,light,0xFF354D52);p.popPose();
        for(int i=0;i<3;i++)box(p,b,GardenMaterials.STEEL,.28+i*.09,.22,.282,.30+i*.09,.48,.29,light,0xFFAABFB4);
    }
    public static void station(PoseStack p,MultiBufferSource b,boolean rack,boolean hasPot,boolean lamp,float fill,int growth,int mask,int total,int water,int light){
        if(rack)frame(p,b,lamp,light);p.pushPose();if(rack)p.translate(0,.13,0);
        int old=tint;tint=-1;try{if(hasPot){pot(p,b,fill,water>0,light);if(growth>=0){p.translate(.11,.39,.11);p.scale(.78f,.86f,.78f);AureliaMesh.draw(p,b,Math.clamp(growth*4/CultivationRules.GROWTH_TICKS,0,4),mask,-1,0,light,total>0?total:6,tint);}}}finally{tint=old;}
        p.popPose();
    }
    public static boolean modelItem(ItemStack s){return s.is(CultivationContent.POT_ITEM.get())||s.is(CultivationContent.FRAME_ITEM.get())||s.getItem() instanceof GardenSupplyItem;}
    public static void item(PoseStack p,MultiBufferSource b,ItemStack s,int light){
        if(s.is(CultivationContent.POT_ITEM.get()))pot(p,b,0,false,light);
        else if(s.is(CultivationContent.FRAME_ITEM.get())){p.pushPose();p.translate(.25,0,.25);p.scale(.5f,.5f,.5f);frame(p,b,false,light);p.popPose();}
        else if(s.getItem() instanceof GardenSupplyItem item)switch(item.kind){case SOIL->bag(p,b,item.capacity,false,item.opened(s),light);case PACK->bag(p,b,5,true,false,light);case WATER->can(p,b,item.contents(s),light);case LAMP->lamp(p,b,light);case SEEDLING->{pot(p,b,1,true,light);p.pushPose();p.translate(.2,.39,.2);p.scale(.6f,.6f,.6f);AureliaMesh.draw(p,b,1,63,-1,0,light,6);p.popPose();}}
    }
}
