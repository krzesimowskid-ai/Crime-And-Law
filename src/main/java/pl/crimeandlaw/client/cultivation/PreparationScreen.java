package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import pl.crimeandlaw.client.FlatButton;
import pl.crimeandlaw.cultivation.*;

public final class PreparationScreen extends GardenScreen {
    private PotNetwork.State state;private FlatButton previous,next,action,exit;private int selectedSlot=-1;
    private UUID pending;private long awaitingSince,lastFrame=System.nanoTime(),started;private boolean holding;private float progress,yaw=.50f;
    private double aimX,aimY;private Vector3f opening=new Vector3f();private String message;
    public PreparationScreen(PotNetwork.State s){super("Przygotowanie doniczki");state=s;message=s.message();}
    public UUID token(){return state.token();} public PotNetwork.State state(){return state;}
    public void receive(PotNetwork.State s){
        if(pending!=null&&!pending.equals(s.revision()))pending=null;
        if(state.stage()!=s.stage()){selectedSlot=-1;progress=0;}
        state=s;message=s.message();if(!s.active()||s.operation()==0&&!holding)progress=0;
    }
    private int mode(){return state.stage()==0?PotNetwork.POUR:state.stage()==1?PotNetwork.PLANT:state.stage()==4?PotNetwork.HARVEST:PotNetwork.WATER;}
    private List<Integer> sources(){var list=new ArrayList<Integer>();if(minecraft.player==null)return list;for(int i=0;i<=40;i++)if(InventoryExchange.sourceSlot(i)){
        var s=minecraft.player.getInventory().getItem(i);boolean ok=switch(mode()){
            case PotNetwork.POUR->s.getItem() instanceof GardenSupplyItem it&&it.kind==GardenSupplyItem.Kind.SOIL&&it.contents(s)>0;
            case PotNetwork.PLANT->s.is(CultivationContent.SEEDS.get())||s.is(CultivationContent.SEEDLING.get())||s.is(CultivationContent.SEED_PACKET.get());
            case PotNetwork.WATER->s.getItem() instanceof GardenSupplyItem it&&it.kind==GardenSupplyItem.Kind.WATER&&it.contents(s)>0;
            default->false;};if(ok)list.add(i);
        }return list;
    }
    private ItemStack selected(){var list=sources();if(!list.contains(selectedSlot))selectedSlot=list.isEmpty()?-1:list.getFirst();return selectedSlot<0?ItemStack.EMPTY:minecraft.player.getInventory().getItem(selectedSlot);}
    private void cycle(int d){if(pending!=null||holding)return;var list=sources();if(!list.isEmpty()){int i=list.indexOf(selectedSlot);selectedSlot=list.get(Math.floorMod(i+d,list.size()));}}
    private void send(int action){if(pending!=null||!state.active())return;pending=state.revision();awaitingSince=System.nanoTime();PotNetwork.action(state,action,selectedSlot);}
    @Override protected void init(){super.init();previous=button(628,197,30,"‹",()->cycle(-1));next=button(840,197,30,"›",()->cycle(1));action=button(628,280,242,"Wybierz",()->{var stack=selected();send(stack.is(CultivationContent.SEED_PACKET.get())?PotNetwork.UNPACK:mode());});exit=button(628,462,242,"Zamknij",this::onClose);}
    @Override public void tick(){if(pending!=null&&System.nanoTime()-awaitingSince>3_000_000_000L){awaitingSince=System.nanoTime();PotNetwork.action(state,PotNetwork.RECOVER,-1);holding=false;progress=0;message="Odświeżanie stanu…";}selected();}
    private boolean aimed(double x,double y){return Math.abs(x-opening.x)<82&&Math.abs(y-opening.y)<34;}
    private void bar(GuiGraphics g,int y,float amount,String name){int x=(int)left+628,yy=(int)top+y;g.fill(x,yy,x+242,yy+9,0xFF0B1517);g.fill(x,yy,x+(int)(242*Math.clamp(amount,0,1)),yy+9,0xFF779663);label(g,name,628,y+17,MUTED);}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        long now=System.nanoTime();float dt=Math.min(.1f,(now-lastFrame)/1e9f);lastFrame=now;aimX=mx/scale;aimY=my/scale;
        if(holding&&aimed(aimX,aimY)&&pending==null)progress=Math.min(1,progress+dt/(mode()==PotNetwork.POUR?3.5f:1.9f));
        background(g,"P R Z Y G O T O W A N I E","DONICZKA  /  AURELIA");int x=(int)left,y=(int)top;
        label(g,state.frame()?state.lamp()?"STELAŻ · UV WŁĄCZONE":"STELAŻ · BEZ LAMPY":"ZWYKŁE ŚWIATŁO",628,82,GREEN);
        String[] stages={"01  WSYP ZIEMIĘ","02  POSADŹ ROŚLINĘ","03  PODLEJ ROŚLINĘ","04  WZROST ROŚLINY","05  GOTOWA DO ZBIORU"};label(g,stages[Math.clamp(state.stage(),0,4)],628,115,INK);
        wrapped(g,state.stage()==0?"Jedno użycie worka napełnia całą doniczkę.":state.stage()==1?"Wybierz nasiono, sadzonkę lub otwórz woreczek.":state.stage()==4?"Zbierz każdy liść w dowolnej kolejności.":"Wzrost wymaga wody oraz światła. UV działa tylko na tę doniczkę.",628,143,242,MUTED,3);
        var stack=selected();previous.active=next.active=pending==null&&!holding&&sources().size()>1;
        String chosen=stack.isEmpty()?mode()==PotNetwork.HARVEST?"Wymagane nożyce":"Brak materiału w ekwipunku":stack.getHoverName().getString();
        wrapped(g,chosen,667,199,166,GREEN,2);if(!stack.isEmpty()){g.renderItem(stack,x+632,y+237);String contents=stack.getItem() instanceof GardenSupplyItem it?it.kind==GardenSupplyItem.Kind.WATER?"Woda: "+it.contents(stack)+" / "+it.capacity:it.kind==GardenSupplyItem.Kind.SOIL?"Napełnienia: "+it.contents(stack)+" / "+it.capacity:"Ilość: "+stack.getCount():"Ilość: "+stack.getCount();label(g,contents,657,241,MUTED);}
        boolean clickAction=mode()==PotNetwork.PLANT||mode()==PotNetwork.HARVEST;
        action.visible=clickAction;action.active=pending==null&&state.active()&&(mode()==PotNetwork.HARVEST||!stack.isEmpty());action.setMessage(net.minecraft.network.chat.Component.literal(mode()==PotNetwork.HARVEST?"Przejdź do zbioru":stack.is(CultivationContent.SEED_PACKET.get())?"Otwórz woreczek":"Posadź jedną roślinę"));
        if(!clickAction){bar(g,282,progress,holding?progress>=1?"GOTOWE • PUŚĆ LPM":aimed(aimX,aimY)?"TRZYMAJ LPM NAD DONICZKĄ":"SKIERUJ STRUMIEŃ DO OTWORU":"Przytrzymaj LPM nad doniczką");}
        bar(g,334,state.growth()/(float)CultivationRules.GROWTH_TICKS,"Wzrost "+state.growth()*100/CultivationRules.GROWTH_TICKS+"%  •  woda "+state.water()+" / 100");
        label(g,state.lit()?"ŚWIATŁO: DOSTĘPNE":"ŚWIATŁO: BRAK • WZROST WSTRZYMANY",628,375,state.lit()?GREEN:0xFFE5B391);
        g.fill(x+628,y+398,x+871,y+447,0x7021332E);wrapped(g,pending!=null?"Czekam na potwierdzenie…":message,639,406,220,GREEN,2);
        drawStage(g,stack,dt);
        g.flush();RenderSystem.depthMask(true);RenderSystem.clear(256,Minecraft.ON_OSX);RenderSystem.disableDepthTest();
        label(g,state.stage()<2?"Celuj w otwór • trzymaj i puść LPM po napełnieniu":"Zatwierdzone czynności są zachowywane po zamknięciu",29,475,MUTED);
        label(g,"PPM i przeciągnięcie: obrót",29,494,MUTED);
        end(g,mx,my,delta);
    }
    private void drawStage(GuiGraphics g,ItemStack stack,float dt){
        var camera=new Matrix4f().translate(left+294,top+421,180).scale(390,-390,390).rotateX(.38f).rotateY(yaw).translate(-.5f,0,-.5f);
        opening=camera.transformPosition(new Vector3f(.5f,.47f,.5f));float fill=state.stage()>0?1:progress*.97f;
        g.flush();RenderSystem.enableDepthTest();RenderSystem.depthMask(true);Lighting.setupFor3DItems();var buffers=minecraft.renderBuffers().bufferSource();
        g.pose().pushPose();g.pose().mulPose(camera);
        GardenMesh.station(g.pose(),buffers,false,true,false,fill,state.stage()>=2?state.growth():-1,state.total()>0?state.leaves():63,state.total(),state.water(),LightTexture.FULL_BRIGHT);
        // Settling grains and droplets are small 3D cubes, projected with the same camera.
        if(holding&&aimed(aimX,aimY)){float t=(System.nanoTime()-started)/1e9f;for(int i=0;i<22;i++){float f=(t*1.7f+i*.137f)%1;double xx=.5+Math.sin(i*9.4)*.065,zz=.5+Math.cos(i*3.7)*.065,yy=.48+.38*(1-f);GardenMesh.box(g.pose(),buffers,mode()==PotNetwork.POUR?GardenMaterials.SOIL:GardenMaterials.WATER,xx,yy,zz,xx+.018,yy+.028,zz+.018,LightTexture.FULL_BRIGHT,-1);}}
        g.pose().popPose();buffers.endBatch();
        if(!stack.isEmpty()&&(mode()==PotNetwork.POUR||mode()==PotNetwork.WATER)){
            double tx=holding?Math.clamp(aimX,opening.x-170,opening.x+170):opening.x+75,ty=holding?Math.clamp(aimY,opening.y-60,opening.y+55):opening.y;
            g.pose().pushPose();g.pose().translate(tx+52,ty-86,330);g.pose().scale(150,-150,150);g.pose().mulPose(Axis.XP.rotation(.30f));g.pose().mulPose(Axis.YP.rotation(-.40f));g.pose().mulPose(Axis.ZP.rotation(holding?-.66f:-.12f));g.pose().translate(-.5,0,-.5);
            if(stack.getItem() instanceof GardenSupplyItem it){if(it.kind==GardenSupplyItem.Kind.SOIL)GardenMesh.bag(g.pose(),buffers,it.capacity,false,holding,LightTexture.FULL_BRIGHT);else GardenMesh.can(g.pose(),buffers,it.contents(stack),LightTexture.FULL_BRIGHT);}
            buffers.endBatch();g.pose().popPose();
        }
        RenderSystem.disableDepthTest();
        if(state.stage()<2||mode()==PotNetwork.WATER){int px=(int)opening.x,py=(int)opening.y;g.renderOutline(px-82,py-34,164,68,holding&&aimed(aimX,aimY)?0xFFBFE291:0x665F987B);}
    }
    private boolean rotating;
    @Override public boolean mouseClicked(double x,double y,int b){if(super.mouseClicked(x,y,b))return true;double lx=x/scale,ly=y/scale;
        if(b==1&&lx<left+590&&ly>top+70){rotating=true;return true;}
        if(b==0&&(mode()==PotNetwork.POUR||mode()==PotNetwork.WATER)&&pending==null&&!holding&&!selected().isEmpty()&&aimed(lx,ly)&&state.water()<100){holding=true;progress=0;started=System.nanoTime();send(mode());return true;}return false;
    }
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){if(rotating){yaw+=(float)(dx/scale*.008);return true;}return holding||super.mouseDragged(x,y,b,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int b){if(rotating){rotating=false;return true;}if(b==0&&holding){holding=false;if(pending==null)send(progress>=1&&aimed(x/scale,y/scale)?PotNetwork.FINISH:PotNetwork.CANCEL);else PotNetwork.action(state,PotNetwork.RECOVER,-1);return true;}return super.mouseReleased(x,y,b);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x/scale>left+600){cycle(vertical>0?-1:1);return true;}return false;}
    @Override public void removed(){if(state.active()&&minecraft.getConnection()!=null)PotNetwork.action(state,PotNetwork.CLOSE,-1);}
}
