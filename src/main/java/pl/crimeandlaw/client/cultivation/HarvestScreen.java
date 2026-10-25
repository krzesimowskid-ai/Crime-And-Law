package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Lighting;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import pl.crimeandlaw.cultivation.*;

/** Real mesh camera, projected cut points, server-timed snips and detached leaf animation. */
public final class HarvestScreen extends Screen {
    private static final ResourceLocation FONT=ResourceLocation.fromNamespaceAndPath("crimeandlaw","chat");
    private CultivationNetwork.State state;
    private float scale,left,top,yaw=.55f,zoom=1;
    private Matrix4f camera=new Matrix4f();
    private final long opened=System.nanoTime();
    private long pressed;
    private long awaitingSince;
    private boolean holding,awaiting,rotating;
    private double dragX;
    private int heldLeaf=-1,hoveredLeaf=-1,collected;
    private String status;
    private record Falling(int leaf,long started){}
    private final List<Falling> falling=new ArrayList<>();
    private final Map<Integer,Vector3f> markers=new HashMap<>();
    private Button exit;
    public HarvestScreen(CultivationNetwork.State state){super(Component.literal("Zbiór aurelii"));this.state=state;status=state.message();}
    public UUID token(){return state.token();}
    public void receive(CultivationNetwork.State next){
        int removed=state.leaves()&~next.leaves();
        for(int id=0;id<6;id++)if((removed&(1<<id))!=0){falling.add(new Falling(id,System.nanoTime()));collected++;}
        state=next;status=next.message();
        if(!next.holding()){awaiting=false;if(!holding)heldLeaf=-1;}
        if(!next.active()){holding=false;awaiting=false;}
        if(exit!=null)exit.setMessage(Component.literal(next.active()?"Przerwij zbiór":"Wróć do gry"));
    }
    public void interrupted(String message){holding=awaiting=false;status=message;state=new CultivationNetwork.State(state.pos(),state.token(),state.leaves(),-1,false,false,message);}
    @Override public void tick(){
        if(awaiting&&System.nanoTime()-awaitingSince>3_000_000_000L){
            awaitingSince=System.nanoTime();status="Odświeżanie stanu cięcia…";
            // Cancel/recover only: never resend a cut or request another reward.
            CultivationNetwork.cut(state.token(),heldLeaf,3);
        }
    }
    @Override protected void init(){
        scale=Math.min(1,Math.min(width/920f,height/560f));left=(width/scale-900)/2;top=(height/scale-520)/2;
        exit=addRenderableWidget(new pl.crimeandlaw.client.FlatButton((int)(left+630),(int)(top+462),240,25,state.active()?"Przerwij zbiór":"Wróć do gry",b->onClose(),()->new pl.crimeandlaw.gang.GangDirectory.Theme(0x14231F,0x71965B,240),false).primary());
    }
    private static Component text(String value){return Component.literal(value).withStyle(s->s.withFont(FONT));}
    private void label(GuiGraphics g,String value,int x,int y,int color){g.drawString(font,text(value),x,y,color,false);}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float delta){}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        g.fill(0,0,width,height,0xBC070F10);
        int x=(int)left,y=(int)top,mxLocal=(int)(mx/scale),myLocal=(int)(my/scale);
        g.pose().pushPose();g.pose().scale(scale,scale,1);
        try{
            g.fill(x+4,y+6,x+904,y+526,0x70000000);g.fillGradient(x,y,x+900,y+520,0xFC172B2B,0xFC101B22);
            g.renderOutline(x,y,900,520,0xFF496F66);g.fill(x,y,x+900,y+2,0xFFB4CE8A);
            g.fill(x+599,y+67,x+600,y+490,0x443C786B);
            label(g,"A U R E L I A",x+29,y+21,0xFFF2E8CA);
            label(g,"PRECYZYJNY ZBIÓR LIŚCI",x+29,y+43,0xFF9CB6AA);
            String counter="POZOSTAŁO  "+PruningRules.count(state.leaves())+" / 6";
            label(g,counter,x+900-font.width(text(counter))-30,y+30,0xFFCDDDAC);
            // Subtle stage pedestal and ground shadow, then a true 3D mesh.
            for(int i=0;i<18;i++)g.fill(x+140+i*5,y+420+i/3,x+445-i*5,y+443-i/3,0x07000000);
            g.fillGradient(x+44,y+414,x+557,y+447,0x001F4238,0x203F6853);
            drawPlant(g,mxLocal,myLocal);
            g.flush();RenderSystem.depthMask(true);RenderSystem.clear(256,Minecraft.ON_OSX);RenderSystem.disableDepthTest();
            int target=holding||awaiting?heldLeaf:hoveredLeaf;
            for(var e:markers.entrySet()){
                var p=e.getValue();int px=Math.round(p.x),py=Math.round(p.y);boolean chosen=e.getKey()==target;
                if(chosen&&state.active()){
                    int r=holding?13:11+(int)(Math.sin(time()*3)*1.5);ring(g,px,py,r,0xFFCAE89A);ring(g,px,py,r+3,0x665DAD80);
                    g.fill(px-2,py-2,px+3,py+3,0xFFF0F7C5);g.fill(px+r+4,py,px+r+28,py+1,0xAACAE89A);
                    label(g,holding?"TNIESZ TEN LIŚĆ":"WYBRANY LIŚĆ",px+r+31,py-6,0xFFE1EDC8);
                }else if(state.active()){g.fill(px-2,py-2,px+2,py+2,0x8892B7A3);}
            }
            label(g,"Przeciągnij tło lub PPM, aby obrócić • kółko: zbliżenie",x+32,y+478,0xFF8BA59F);
            label(g,"01  WYBIERZ DOWOLNY LIŚĆ",x+630,y+86,0xFFCEE2B5);
            label(g,"Ty decydujesz o kolejności zbioru.",x+630,y+111,0xFFAEC2B8);
            label(g,"02  PRZYTRZYMAJ LPM",x+630,y+152,0xFFCEE2B5);
            label(g,"Kliknij liść lub znacznik u nasady.",x+630,y+177,0xFFAEC2B8);
            label(g,"03  PUŚĆ W ZIELONYM POLU",x+630,y+218,0xFFCEE2B5);
            label(g,"Spokojne cięcie zachowuje cały liść.",x+630,y+243,0xFFAEC2B8);
            progress(g,x+630,y+280,240);
            label(g,"ZEBRANO W TEJ SESJI",x+630,y+339,0xFF90A99D);
            label(g,Integer.toString(collected)+" liści",x+630,y+363,0xFFF0E8CA);
            g.fill(x+629,y+398,x+871,y+444,0x7021332E);
            var lines=font.split(text(status),222);for(int line=0;line<Math.min(2,lines.size());line++)g.drawString(font,lines.get(line),x+640,y+407+line*17,state.active()?0xFFCEE0C5:0xFFE9D7A4,false);
            // Flush leaf geometry before buttons/items so depth never hides controls.
            g.flush();RenderSystem.depthMask(true);RenderSystem.clear(256,Minecraft.ON_OSX);
            super.render(g,mxLocal,myLocal,delta);
            if(state.active()&&mxLocal<x+600&&mxLocal>x&&myLocal>y+70&&myLocal<y+465){
                g.pose().pushPose();g.pose().translate(mxLocal+9,myLocal+8,300);g.pose().scale(1.8f,1.8f,1.8f);
                if(holding)g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float)Math.sin(time()*19)*5));
                g.renderItem(new net.minecraft.world.item.ItemStack(CultivationContent.SHEARS.get()),0,0);g.pose().popPose();
            }
        }finally{g.pose().popPose();}
    }
    private float time(){return (System.nanoTime()-opened)/1_000_000_000f;}
    private void drawPlant(GuiGraphics g,int mouseX,int mouseY){
        camera=new Matrix4f().translate(left+298,top+431,170).scale(363*zoom,-363*zoom,363*zoom)
            .rotateX(.23f).rotateY(yaw).translate(-.5f,0,-.5f);
        markers.clear();for(int i=0;i<6;i++)if(PruningRules.available(state.leaves(),i)){
            var p=AureliaMesh.leaf(i).marker();markers.put(i,camera.transformPosition(new Vector3f((float)p.x,(float)p.y,(float)p.z)));
        }
        hoveredLeaf=state.active()?pick(mouseX,mouseY):-1;
        g.flush();RenderSystem.enableDepthTest();RenderSystem.depthMask(true);Lighting.setupFor3DItems();
        var buffers=minecraft.renderBuffers().bufferSource();
        g.pose().pushPose();g.pose().mulPose(camera);
        try{
            AureliaMesh.draw(g.pose(),buffers,4,state.leaves(),state.active()?(holding||awaiting?heldLeaf:hoveredLeaf):-1,time(),LightTexture.FULL_BRIGHT);
            var it=falling.iterator();while(it.hasNext()){
                var f=it.next();float t=(System.nanoTime()-f.started)/1_000_000_000f;if(t>1.4){it.remove();continue;}
                g.pose().pushPose();g.pose().translate(.14*t,-.30*t*t,.12*t);g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(t*48));
                AureliaMesh.drawLeaf(g.pose(),buffers.getBuffer(RenderType.entityCutoutNoCull(AureliaMesh.LEAF)),AureliaMesh.leaf(f.leaf),LightTexture.FULL_BRIGHT,0xFFFFFFFF,time(),false);g.pose().popPose();
            }
            buffers.endBatch();
        }finally{g.pose().popPose();RenderSystem.disableDepthTest();Lighting.setupFor3DItems();}
    }
    private void progress(GuiGraphics g,int x,int y,int w){
        g.fill(x,y,x+w,y+12,0xFF0B1517);
        int start=(int)(w*PruningRules.MIN_HOLD/40f),end=(int)(w*PruningRules.MAX_HOLD/40f);
        g.fill(x+start,y+1,x+end,y+11,0xFF476C48);g.fill(x+w/2-1,y+1,x+w/2+1,y+11,0xFFB6D68D);
        float held=holding?(System.nanoTime()-pressed)/50_000_000f:0;
        int p=x+(int)(Math.min(held,40)/40*w);
        if(holding){g.fill(p-1,y-4,p+2,y+16,held>PruningRules.MAX_HOLD?0xFFE49A83:0xFFF4E9C5);}
        label(g,awaiting?"Potwierdzanie cięcia…":holding?"PUŚĆ W ZIELONYM POLU":"Przytrzymaj przy znaczniku",x,y+25,holding?0xFFDCF3AD:0xFF94AA9F);
    }
    private static void ring(GuiGraphics g,int x,int y,int radius,int color){
        for(int i=0;i<48;i++){double a=i*Math.PI/24;int px=x+(int)Math.round(Math.cos(a)*radius),py=y+(int)Math.round(Math.sin(a)*radius);g.fill(px,py,px+2,py+2,color);}
    }
    private Vector3f project(AureliaMesh.Leaf leaf,float u,float v){var p=leaf.point(1-v,u*2-1);return camera.transformPosition(new Vector3f((float)p.x,(float)p.y,(float)p.z));}
    private int pick(double x,double y){
        int result=-1;double best=22*22;
        for(var e:markers.entrySet()){var p=e.getValue();double d=(p.x-x)*(p.x-x)+(p.y-y)*(p.y-y);if(d<best){best=d;result=e.getKey();}}
        if(result>=0)return result;
        float nearest=-Float.MAX_VALUE;float[] rows={0,.28f,.58f,1};
        for(int id=0;id<6;id++)if(PruningRules.available(state.leaves(),id)){
            var leaf=AureliaMesh.leaf(id);
            for(int row=0;row<3;row++)for(int col=0;col<2;col++){
                float u0=col*.5f,u1=u0+.5f,v0=rows[row],v1=rows[row+1];
                var a=project(leaf,u0,v0);var b=project(leaf,u0,v1);var c=project(leaf,u1,v1);var d=project(leaf,u1,v0);
                float z=hit(x,y,a,b,c,u0,v0,u0,v1,u1,v1);if(z>nearest){nearest=z;result=id;}
                z=hit(x,y,a,c,d,u0,v0,u1,v1,u1,v0);if(z>nearest){nearest=z;result=id;}
            }
        }
        return result;
    }
    private static float hit(double x,double y,Vector3f a,Vector3f b,Vector3f c,float au,float av,float bu,float bv,float cu,float cv){
        double denominator=(b.y-c.y)*(a.x-c.x)+(c.x-b.x)*(a.y-c.y);if(Math.abs(denominator)<.01)return -Float.MAX_VALUE;
        double wa=((b.y-c.y)*(x-c.x)+(c.x-b.x)*(y-c.y))/denominator,wb=((c.y-a.y)*(x-c.x)+(a.x-c.x)*(y-c.y))/denominator,wc=1-wa-wb;
        if(wa<0||wb<0||wc<0)return -Float.MAX_VALUE;
        int u=(int)((wa*au+wb*bu+wc*cu)*PixelSprite.SIZE),v=(int)((wa*av+wb*bv+wc*cv)*PixelSprite.SIZE);
        return PixelSprite.LEAF.opaque(u,v)?(float)(wa*a.z+wb*b.z+wc*c.z):-Float.MAX_VALUE;
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        x/=scale;y/=scale;
        if(super.mouseClicked(x,y,button))return true;
        if(x<left||x>left+596||y<top+68||y>top+463)return false;
        if(button==0&&state.active()&&!holding&&!awaiting&&pick(x,y)>=0){
            heldLeaf=pick(x,y);holding=true;pressed=System.nanoTime();CultivationNetwork.cut(state.token(),heldLeaf,0);return true;
        }
        if(button==0||button==1){rotating=true;dragX=x;return true;}return false;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(rotating){yaw+=(float)(dx/scale*.008);return true;}return super.mouseDragged(x/scale,y/scale,button,dx/scale,dy/scale);}
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&holding){holding=false;awaiting=true;awaitingSince=System.nanoTime();CultivationNetwork.cut(state.token(),heldLeaf,pick(x/scale,y/scale)==heldLeaf?1:3);return true;}
        if(rotating){rotating=false;return true;}return super.mouseReleased(x/scale,y/scale,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x/scale<left+600){zoom=Math.clamp(zoom+(float)vertical*.06f,.78f,1.18f);return true;}return super.mouseScrolled(x/scale,y/scale,horizontal,vertical);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT){yaw+=key==GLFW.GLFW_KEY_LEFT?-.16f:.16f;return true;}return super.keyPressed(key,scan,mods);}
    @Override public void onClose(){minecraft.setScreen(null);}
    @Override public void removed(){if(state.active()&&minecraft.getConnection()!=null&&minecraft.getConnection().hasChannel(CultivationNetwork.Cut.TYPE))CultivationNetwork.cut(state.token(),-1,2);}
    @Override public boolean isPauseScreen(){return false;}
}
