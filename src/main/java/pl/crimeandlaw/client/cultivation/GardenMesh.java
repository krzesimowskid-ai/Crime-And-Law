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
            for(int i=0;i<8;i++){Vec3 a=ring(radius,y,i),c=ring(radius,y,i+1);Vec3 aa=ring(.055,y-hole*.033,i),cc=ring(.055,y-hole*.033,i+1);quad(p,v,aa,cc,c,a,light,wet?0xFFB0A2B7:-1);quad(p,v,new Vec3(.5,y-hole*.04,.5),cc,aa,new Vec3(.5,y-hole*.04,.5),light,wet?0xFF9C8CAA:0xFFDBD1B7);}
            for(int i=0;i<8;i++){double a=i*2.4,rr=radius*.65,x=.5+Math.cos(a)*rr,z=.5+Math.sin(a)*rr;box(p,b,GardenMaterials.SOIL,x,y,z,x+.018,y+.01,z+.018,light,wet?0xFF8D7D9D:0xFFC2AD86);}
        }
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
        for(int j=0;j<bands.length-1;j++){var a=bands[j];var c=bands[j+1];var v=b.getBuffer(RenderType.entityCutoutNoCull(mat));
            quad(p,v,new Vec3(a[0],a[1],a[2]),new Vec3(c[0],c[1],c[2]),new Vec3(c[3],c[1],c[2]),new Vec3(a[3],a[1],a[2]),light,-1);
            quad(p,v,new Vec3(a[3],a[1],a[4]),new Vec3(c[3],c[1],c[4]),new Vec3(c[0],c[1],c[4]),new Vec3(a[0],a[1],a[4]),light,-1);
            quad(p,v,new Vec3(a[0],a[1],a[4]),new Vec3(c[0],c[1],c[4]),new Vec3(c[0],c[1],c[2]),new Vec3(a[0],a[1],a[2]),light,0xFFD0D8CB);
            quad(p,v,new Vec3(a[3],a[1],a[2]),new Vec3(c[3],c[1],c[2]),new Vec3(c[3],c[1],c[4]),new Vec3(a[3],a[1],a[4]),light,0xFFD0D8CB);
        }
        box(p,b,mat,.27,.075,.35,.73,.11,.65,light,0xFFBDD0BC);
        box(p,b,mat,.25,.78,.33,.75,.83,.36,light,-1);box(p,b,mat,.25,.78,.64,.75,.83,.67,light,-1);
        if(cut<.99f){box(p,b,mat,.25+cut*.50,.805,.36,.75,.835,.64,light,0xFFDDE9CD);box(p,b,GardenMaterials.WHITE,.25+cut*.5,.836,.35,.75,.842,.37,light,0xFFE4C77D);}
        else {box(p,b,GardenMaterials.SOIL,.28,.79,.37,.72,.795,.63,light,-1);p.pushPose();p.translate(.25,.80,.35);p.mulPose(Axis.ZP.rotationDegrees(-65));box(p,b,mat,0,0,0,.50,.018,.025,light,-1);p.popPose();}
        if(!packet){var label=capacity==1?GardenMaterials.LABEL1:capacity==2?GardenMaterials.LABEL2:GardenMaterials.LABEL3;var v=b.getBuffer(RenderType.entityCutoutNoCull(label));
            PixelSprite.quad(p,v,new Vec3(.22,.69,.727),new Vec3(.22,.20,.727),new Vec3(.78,.20,.727),new Vec3(.78,.69,.727),0,0,1,1,light,-1);
            PixelSprite.quad(p,v,new Vec3(.78,.69,.273),new Vec3(.78,.20,.273),new Vec3(.22,.20,.273),new Vec3(.22,.69,.273),0,0,1,1,light,-1);
        }else {box(p,b,GardenMaterials.WHITE,.30,.26,.731,.70,.57,.735,light,0xFFDFE7C7);PixelSprite.SEEDS.draw(p,b.getBuffer(RenderType.entityCutoutNoCull(PixelSprite.SEEDS.texture())),(u,v,d)->new Vec3(.36+u*.28,.30+(1-v)*.22,.739+d),.002f,-1,light,false);}
    }
    public static void seed(PoseStack p,MultiBufferSource b,int light){box(p,b,GardenMaterials.CLOTH,-.025,-.035,-.018,.025,.035,.018,light,0xFFB07B35);box(p,b,GardenMaterials.CLOTH,-.018,-.045,-.013,.018,.045,.013,light,0xFFE8BB62);box(p,b,GardenMaterials.SOIL,-.004,-.035,-.019,.004,.035,.019,light,0xFF6D5432);}
    public static void knife(PoseStack p,MultiBufferSource b,int light){box(p,b,GardenMaterials.PACK,-.12,-.025,-.025,0,.025,.025,light,-1);box(p,b,GardenMaterials.STEEL,0,-.010,-.012,.19,.010,.012,light,0xFFE5EEE8);}
    public static void glove(PoseStack p,MultiBufferSource b,int light){box(p,b,GardenMaterials.PACK,-.045,0,-.055,.045,.025,.045,light,0xFFCDD5B7);for(int i=0;i<4;i++)box(p,b,GardenMaterials.PACK,-.045+i*.024,.005,-.10,-.026+i*.024,.022,-.05,light,0xFFB9C59F);}
    public static void can(PoseStack p,MultiBufferSource b,int water,int light){
        box(p,b,GardenMaterials.STEEL,.20,.12,.29,.66,.57,.71,light,-1);box(p,b,GardenMaterials.STEEL,.17,.12,.26,.69,.17,.74,light,0xFF829995);box(p,b,GardenMaterials.STEEL,.18,.54,.27,.68,.59,.73,light,0xFFD9DFCC);
        box(p,b,GardenMaterials.STEEL,.25,.59,.34,.56,.62,.66,light,0xFF4B6564);box(p,b,GardenMaterials.WATER,.30,.612,.39,.51,.616,.61,light,water>0?0xFFFFFFFF:0xFF26332D);
        box(p,b,GardenMaterials.STEEL,.13,.57,.44,.19,.82,.56,light,-1);box(p,b,GardenMaterials.STEEL,.13,.78,.44,.62,.84,.56,light,-1);box(p,b,GardenMaterials.STEEL,.56,.57,.44,.62,.82,.56,light,-1);
        p.pushPose();p.translate(.61,.26,.44);p.mulPose(Axis.ZP.rotationDegrees(36));box(p,b,GardenMaterials.STEEL,0,-.04,0,.43,.05,.12,light,0xFFB7CFC3);box(p,b,GardenMaterials.STEEL,.4,-.08,-.05,.46,.10,.17,light,0xFFD0DFCF);for(int i=0;i<3;i++)for(int j=0;j<3;j++)box(p,b,GardenMaterials.WHITE,.461,-.05+i*.055,-.02+j*.055,.464,-.027+i*.055,.003+j*.055,light,0xFF354D52);p.popPose();
        for(int i=0;i<3;i++)box(p,b,GardenMaterials.STEEL,.28+i*.09,.22,.282,.30+i*.09,.48,.29,light,0xFFAABFB4);
    }
    public static void station(PoseStack p,MultiBufferSource b,boolean rack,boolean hasPot,boolean lamp,float fill,int growth,int mask,int total,int water,int light){
        if(rack)frame(p,b,lamp,light);p.pushPose();if(rack)p.translate(0,.13,0);
        int old=tint;tint=rack&&lamp?UvLighting.TINT:-1;try{if(hasPot){if(lamp)light=LightTexture.FULL_BRIGHT;pot(p,b,fill,water>0,light);if(growth>=0){p.translate(.11,.39,.11);p.scale(.78f,.86f,.78f);AureliaMesh.draw(p,b,Math.clamp(growth*4/CultivationRules.GROWTH_TICKS,0,4),mask,-1,0,light,total>0?total:6,tint);}}}finally{tint=old;}
        p.popPose();
    }
    public static boolean modelItem(ItemStack s){return s.is(CultivationContent.POT_ITEM.get())||s.is(CultivationContent.FRAME_ITEM.get())||s.getItem() instanceof GardenSupplyItem;}
    public static void item(PoseStack p,MultiBufferSource b,ItemStack s,int light){
        if(s.is(CultivationContent.POT_ITEM.get()))pot(p,b,0,false,light);
        else if(s.is(CultivationContent.FRAME_ITEM.get())){p.pushPose();p.translate(.25,0,.25);p.scale(.5f,.5f,.5f);frame(p,b,false,light);p.popPose();}
        else if(s.getItem() instanceof GardenSupplyItem item)switch(item.kind){case SOIL->bag(p,b,item.capacity,false,false,light);case PACK->bag(p,b,5,true,false,light);case WATER->can(p,b,item.contents(s),light);case LAMP->lamp(p,b,light);case SEEDLING->{pot(p,b,1,true,light);p.pushPose();p.translate(.2,.39,.2);p.scale(.6f,.6f,.6f);AureliaMesh.draw(p,b,1,63,-1,0,light,6);p.popPose();}}
    }
}
