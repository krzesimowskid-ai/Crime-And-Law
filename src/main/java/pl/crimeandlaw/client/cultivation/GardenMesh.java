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
    private static void quad(PoseStack p,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int l,int col){PixelSprite.quad(p,v,a,b,c,d,0,0,1,1,l,col);}
    private static Vec3 ring(double r,double y,int i){double a=Math.PI/8+i*Math.PI/4;return new Vec3(.5+Math.cos(a)*r,y,.5+Math.sin(a)*r);}
    private static void band(PoseStack p,MultiBufferSource b,double r0,double y0,double r1,double y1,int l,int c){var v=b.getBuffer(RenderType.entityCutoutNoCull(GardenMaterials.CLAY));for(int i=0;i<8;i++)quad(p,v,ring(r0,y0,i),ring(r1,y1,i),ring(r1,y1,i+1),ring(r0,y0,i+1),l,c);}
    public static void pot(PoseStack p,MultiBufferSource b,float fill,boolean wet,int light){
        band(p,b,.235,.025,.25,.07,light,0xFFE1B28E);band(p,b,.25,.07,.35,.41,light,0xFFFFFFFF);
        band(p,b,.35,.395,.385,.405,light,0xFFB97E52);band(p,b,.385,.405,.385,.47,light,0xFFFFFFFF);band(p,b,.385,.47,.322,.47,light,0xFFFFDCAD);
        band(p,b,.322,.47,.322,.40,light,0xFFBB8861);band(p,b,.322,.40,.22,.055,light,0xFF835C43);
        box(p,b,GardenMaterials.CLAY,.28,.025,.28,.72,.055,.72,light,0xFF9E6949);
        if(fill>0){double y=.09+.30*Math.clamp(fill,0,1);var v=b.getBuffer(RenderType.entityCutoutNoCull(GardenMaterials.SOIL));for(int i=0;i<8;i++)quad(p,v,new Vec3(.5,y,.5),ring(.307,y,i),ring(.307,y,i+1),new Vec3(.5,y,.5),light,wet?0xFF8D9CA1:0xFFFFFFFF);
            for(int i=0;i<9;i++){double x=.35+(i%3)*.12,z=.35+(i/3)*.12;box(p,b,GardenMaterials.SOIL,x,y,z,x+.035,y+.018,z+.035,light,wet?0xFF687883:0xFFD9C4A0);}}
    }
    public static void frame(PoseStack p,MultiBufferSource b,boolean lamp,int light){
        for(double x:new double[]{.07,.87})for(double z:new double[]{.07,.87}){box(p,b,GardenMaterials.STEEL,x,0,z,x+.06,1.96,z+.06,light,0xFFFFFFFF);for(double y:new double[]{.08,.92,1.86})box(p,b,GardenMaterials.STEEL,x-.009,y,z-.009,x+.069,y+.038,z+.069,light,0xFF59666C);}
        for(double y:new double[]{.04,1.89}){box(p,b,GardenMaterials.STEEL,.07,y,.07,.93,y+.055,.13,light,-1);box(p,b,GardenMaterials.STEEL,.07,y,.87,.93,y+.055,.93,light,-1);box(p,b,GardenMaterials.STEEL,.07,y,.13,.13,y+.055,.87,light,-1);box(p,b,GardenMaterials.STEEL,.87,y,.13,.93,y+.055,.87,light,-1);}
        box(p,b,GardenMaterials.STEEL,.16,.075,.16,.84,.13,.84,light,0xFF809094);
        if(lamp){for(double x:new double[]{.27,.70})box(p,b,GardenMaterials.STEEL,x,1.64,.48,x+.025,1.9,.505,light,0xFF5F6A73);p.pushPose();p.translate(0,1.31,0);lamp(p,b,light);p.popPose();}
    }
    public static void lamp(PoseStack p,MultiBufferSource b,int light){
        box(p,b,GardenMaterials.STEEL,.17,.30,.18,.83,.37,.82,light,0xFFD4DDCF);box(p,b,GardenMaterials.STEEL,.22,.27,.22,.78,.30,.78,light,0xFF29313E);
        for(int i=0;i<4;i++){double z=.28+i*.13;box(p,b,GardenMaterials.STEEL,.21,.23,z-.025,.26,.29,z+.06,light,0xFF88978E);box(p,b,GardenMaterials.STEEL,.74,.23,z-.025,.79,.29,z+.06,light,0xFF88978E);box(p,b,GardenMaterials.UV,.26,.235,z,.74,.275,z+.035,LightTexture.FULL_BRIGHT,-1);}
        box(p,b,GardenMaterials.STEEL,.41,.37,.44,.59,.43,.56,light,0xFF566267);
    }
    public static void bag(PoseStack p,MultiBufferSource b,int capacity,boolean packet,boolean open,int light){
        var mat=packet?GardenMaterials.PACK:GardenMaterials.CLOTH;
        box(p,b,mat,.20,.08,.30,.80,.64,.70,light,-1);box(p,b,mat,.235,.64,.33,.765,.73,.67,light,0xFFE1DAB6);
        box(p,b,mat,.28,.73,.36,.72,.79,.64,light,-1);box(p,b,mat,.195,.1,.295,.23,.63,.705,light,0xFFD9C494);box(p,b,mat,.77,.1,.295,.805,.63,.705,light,0xFFD9C494);
        if(open){box(p,b,GardenMaterials.SOIL,.30,.77,.38,.70,.79,.62,light,0xFF665A45);box(p,b,mat,.25,.79,.35,.75,.82,.38,light,-1);box(p,b,mat,.25,.79,.62,.75,.82,.65,light,-1);}
        else box(p,b,mat,.265,.79,.35,.735,.83,.65,light,0xFFE8DDB9);
        box(p,b,GardenMaterials.WHITE,.30,.28,.292,.70,.56,.299,light,packet?0xFFDFE7C7:0xFF3D5B46);
        if(packet)PixelSprite.SEEDS.draw(p,b.getBuffer(RenderType.entityCutoutNoCull(PixelSprite.SEEDS.texture())),(u,v,d)->new Vec3(.37+u*.26,.32+(1-v)*.20,.288-d),.002f,-1,light,false);
        else { // embossed native-pixel capacity numeral
            String[] digit=capacity==1?new String[]{"010","110","010","010","111"}:capacity==2?new String[]{"111","001","111","100","111"}:new String[]{"111","001","111","001","111"};
            for(int y=0;y<5;y++)for(int x=0;x<3;x++)if(digit[y].charAt(x)=='1')box(p,b,GardenMaterials.WHITE,.445+x*.04,.49-y*.038,.279,.478+x*.04,.52-y*.038,.29,light,0xFFECDFC1);
        }
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
        if(hasPot){pot(p,b,fill,water>0,light);if(growth>=0){p.translate(.11,.39,.11);p.scale(.78f,.86f,.78f);AureliaMesh.draw(p,b,Math.clamp(growth*4/CultivationRules.GROWTH_TICKS,0,4),mask,-1,0,light,total>0?total:6);}}
        p.popPose();
    }
    public static boolean modelItem(ItemStack s){return s.is(CultivationContent.POT_ITEM.get())||s.is(CultivationContent.FRAME_ITEM.get())||s.getItem() instanceof GardenSupplyItem;}
    public static void item(PoseStack p,MultiBufferSource b,ItemStack s,int light){
        if(s.is(CultivationContent.POT_ITEM.get()))pot(p,b,0,false,light);
        else if(s.is(CultivationContent.FRAME_ITEM.get())){p.pushPose();p.translate(.25,0,.25);p.scale(.5f,.5f,.5f);frame(p,b,false,light);p.popPose();}
        else if(s.getItem() instanceof GardenSupplyItem item)switch(item.kind){case SOIL->bag(p,b,item.capacity,false,false,light);case PACK->bag(p,b,5,true,false,light);case WATER->can(p,b,item.contents(s),light);case LAMP->lamp(p,b,light);case SEEDLING->{pot(p,b,1,true,light);p.pushPose();p.translate(.2,.39,.2);p.scale(.6f,.6f,.6f);AureliaMesh.draw(p,b,1,63,-1,0,light,6);p.popPose();}}
    }
}
