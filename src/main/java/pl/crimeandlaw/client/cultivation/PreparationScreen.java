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
import org.lwjgl.glfw.GLFW;
import pl.crimeandlaw.client.FlatButton;
import pl.crimeandlaw.cultivation.*;
import pl.crimeandlaw.cultivation.PreparationPhysics.*;

/** All tools and particles share a world-space camera and emitter transform. */
public final class PreparationScreen extends GardenScreen {
    private PotNetwork.State state;private FlatButton previous,next,action;private int selectedSlot=-1,sequence,ticks;
    private UUID pending;private long awaitingSince,lastFrame=System.nanoTime(),cutSince,seedFallSince,landedAt;
    private ItemStack source=ItemStack.EMPTY;
    private Pose tool=new Pose(.82,1.0,.50,0,0);
    private Matrix4f camera=new Matrix4f();private float yaw=.38f,cut,visualFill;
    private final Set<Integer> keys=new HashSet<>();private boolean grabbed,cutting,packetOpened,seedHeld,falling,seedOnSoil,covering,rotating,autoPour;
    private Point seed=new Point(.95,.83,.48),drop,hand=new Point(.5,PreparationPhysics.TOP,.5),grabOffset=new Point(0,0,0);
    private double mouseX,mouseY,spawnClock;
    private static final class Particle {Point start,velocity;double age;boolean water,spilled;Particle(Stream s,boolean w,double jitter){start=s.source().add(new Point(Math.sin(jitter)*(w?.0007:.004),0,Math.cos(jitter)*(w?.0007:.004)));velocity=s.velocity();water=w;}}
    private final List<Particle> particles=new ArrayList<>();
    public PreparationScreen(PotNetwork.State s){super("Przygotowanie doniczki");state=s;visualFill=s.stage()>0?1:0;}
    public UUID token(){return state.token();}public PotNetwork.State state(){return state;}
    public Pose toolPose(){return tool;}
    public void receive(PotNetwork.State s){
        int old=state.operation();boolean answered=pending!=null&&!pending.equals(s.revision());if(answered)pending=null;
        state=s;if(s.slot()>=0)selectedSlot=s.slot();
        if(old!=s.operation()){
            grabbed=cutting=covering=seedHeld=false;keys.clear();spawnClock=0;
            if(s.operation()==PotNetwork.CUT||s.operation()==PotNetwork.POUR||s.operation()==PotNetwork.WATER){source=selected().copy();tool=new Pose(s.operation()==PotNetwork.CUT?.60:.82,1.0,.50,0,0);cut=s.operation()==PotNetwork.CUT?0:1;}
            if(s.operation()==PotNetwork.PLANT){source=selected().copy();packetOpened=!source.is(CultivationContent.SEED_PACKET.get());seed=new Point(.95,.83,.48);falling=seedOnSoil=false;cut=0;}
            if(s.operation()==0){autoPour=old==PotNetwork.CUT;source=ItemStack.EMPTY;selectedSlot=-1;falling=seedOnSoil=false;cut=0;}
        }
    }
    private int mode(){return state.stage()==0?PotNetwork.POUR:state.stage()==1?PotNetwork.PLANT:state.stage()==4?PotNetwork.HARVEST:PotNetwork.WATER;}
    private List<Integer> sources(){var list=new ArrayList<Integer>();if(minecraft.player==null)return list;for(int i=0;i<=40;i++)if(InventoryExchange.sourceSlot(i)){
        var s=minecraft.player.getInventory().getItem(i);boolean ok=switch(mode()){
            case PotNetwork.POUR->s.getItem() instanceof GardenSupplyItem it&&it.kind==GardenSupplyItem.Kind.SOIL&&it.contents(s)>0;
            case PotNetwork.PLANT->s.is(CultivationContent.SEEDS.get())||s.is(CultivationContent.SEEDLING.get())||s.is(CultivationContent.SEED_PACKET.get());
            case PotNetwork.WATER->s.getItem() instanceof GardenSupplyItem it&&it.kind==GardenSupplyItem.Kind.WATER&&it.contents(s)>0;default->false;};if(ok)list.add(i);
        }return list;
    }
    private ItemStack selected(){if(state.operation()!=0&&minecraft.player!=null&&InventoryExchange.sourceSlot(selectedSlot))return minecraft.player.getInventory().getItem(selectedSlot);
        var list=sources();if(!list.contains(selectedSlot)){selectedSlot=list.isEmpty()?-1:list.getFirst();if(mode()==PotNetwork.POUR)for(int slot:list){var s=minecraft.player.getInventory().getItem(slot);if(((GardenSupplyItem)s.getItem()).opened(s)){selectedSlot=slot;break;}}}return selectedSlot<0?ItemStack.EMPTY:minecraft.player.getInventory().getItem(selectedSlot);}
    private void cycle(int d){if(pending!=null||state.operation()!=0)return;var list=sources();if(!list.isEmpty())selectedSlot=list.get(Math.floorMod(list.indexOf(selectedSlot)+d,list.size()));}
    private void send(int a){if(pending!=null||!state.active())return;pending=state.revision();awaitingSince=System.nanoTime();PotNetwork.action(state,a,selectedSlot);}
    private void start(){var s=selected();if(s.isEmpty())return;send(mode()==PotNetwork.POUR&&s.getItem() instanceof GardenSupplyItem it&&!it.opened(s)?PotNetwork.CUT:mode());}
    @Override protected void init(){super.init();previous=button(628,197,30,"‹",()->cycle(-1));next=button(840,197,30,"›",()->cycle(1));action=button(628,280,242,"Rozpocznij",()->{if(mode()==PotNetwork.HARVEST)send(PotNetwork.HARVEST);else start();});button(628,462,242,"Zamknij",this::onClose);}
    @Override public void tick(){
        ticks++;if(pending!=null&&System.nanoTime()-awaitingSince>3_000_000_000L){awaitingSince=System.nanoTime();PotNetwork.action(state,PotNetwork.RECOVER,-1);grabbed=seedHeld=covering=false;}
        selected();if(autoPour&&pending==null){autoPour=false;if(selected().getItem() instanceof GardenSupplyItem it&&it.opened(selected()))send(PotNetwork.POUR);}
        if(pending==null&&state.work()>=999&&(state.operation()==PotNetwork.POUR||state.operation()==PotNetwork.WATER||state.operation()==PotNetwork.COVER&&!falling&&System.nanoTime()-landedAt>700_000_000L))send(PotNetwork.FINISH);
        if(ticks%3==0&&pending==null)control();
        if(cut>=.99&&pending==null&&System.nanoTime()-cutSince>600_000_000L){if(state.operation()==PotNetwork.CUT)send(PotNetwork.FINISH);else if(state.operation()==PotNetwork.PLANT&&!packetOpened){packetOpened=true;cutting=false;}}
    }
    private void control(){if(minecraft.getConnection()==null)return;int op=state.operation();if(op==PotNetwork.POUR||op==PotNetwork.WATER)PotNetwork.control(state,++sequence,tool,grabbed);
        else if(op==PotNetwork.PLANT&&packetOpened)PotNetwork.control(state,++sequence,new Pose(seed.x(),Math.max(.60,seed.y()),seed.z(),0,0),seedHeld);
        else if(op==PotNetwork.COVER&&!falling)PotNetwork.control(state,++sequence,new Pose(hand.x(),PreparationPhysics.TOP,hand.z(),0,0),covering);
    }
    private Vector3f project(Point p){return camera.transformPosition(new Vector3f((float)p.x(),(float)p.y(),(float)p.z()));}
    private Point unproject(double x,double y,double plane){var inverse=new Matrix4f(camera).invert();var a=inverse.transformPosition(new Vector3f((float)x,(float)y,0));var b=inverse.transformPosition(new Vector3f((float)x,(float)y,1));double t=(plane-a.y)/(b.y-a.y);return new Point(a.x+(b.x-a.x)*t,plane,a.z+(b.z-a.z)*t);}
    private boolean toolHit(double x,double y){var p=project(new Point(tool.x(),tool.y(),tool.z()));return Math.abs(x-p.x)<85&&Math.abs(y-p.y)<90;}
    public Vector3f seamStart(){return project(cutPose().point(new Point(.275,.84,.365)));}
    public Vector3f seamEnd(){return project(cutPose().point(new Point(.725,.84,.365)));}
    private Pose cutPose(){return new Pose(.60,1.0,.50,0,0);}
    private void cutAt(double x,double y){var a=seamStart();var b=seamEnd();double dx=b.x-a.x,dy=b.y-a.y,l=dx*dx+dy*dy,u=((x-a.x)*dx+(y-a.y)*dy)/l,dist=Math.abs((x-a.x)*dy-(y-a.y)*dx)/Math.sqrt(l);if(dist<16&&u>=cut-.13&&u<=cut+.30)cut=Math.max(cut,(float)Math.clamp(u,0,1));}
    private void advance(double dt){
        double right=(keys.contains(GLFW.GLFW_KEY_D)?1:0)-(keys.contains(GLFW.GLFW_KEY_A)?1:0),forward=(keys.contains(GLFW.GLFW_KEY_W)?1:0)-(keys.contains(GLFW.GLFW_KEY_S)?1:0);
        if(grabbed)tool=PreparationPhysics.tilt(tool,right,forward,dt);
        if(seedHeld){var moved=PreparationPhysics.move(new Pose(seed.x(),.83,seed.z(),0,0),right,forward,yaw,dt);seed=new Point(moved.x(),.83,moved.z());}
        float target=state.stage()>0?1:state.work()/1000f;visualFill+=(target-visualFill)*Math.min(1,dt*12);
        int op=state.operation();boolean water=op==PotNetwork.WATER;
        if((op==PotNetwork.POUR||water)&&grabbed&&pending==null){var stream=PreparationPhysics.stream(tool,water,true);if(stream.flowing()){spawnClock+=dt*(water?23:24);while(spawnClock>=1&&particles.size()<480){spawnClock--;for(var jet:PreparationPhysics.streams(tool,water,true))particles.add(new Particle(jet,water,ticks+particles.size()*7.3));}}}
        for(var it=particles.iterator();it.hasNext();){var p=it.next();double before=p.age;p.age+=dt;var s=new Stream(p.start,p.velocity,true);var prev=PreparationPhysics.at(s,before);var now=PreparationPhysics.at(s,p.age);if(!p.water&&PreparationPhysics.insideBag(tool,now,.007)){it.remove();continue;}if(prev.y()>=PreparationPhysics.RIM&&now.y()<PreparationPhysics.RIM&&!PreparationPhysics.inside(now.x(),now.z(),.310))p.spilled=true;
            double surface=p.spilled?.015:PreparationPhysics.soilY(visualFill);if(p.age>2||now.y()<surface)it.remove();}
        if(falling){double t=(System.nanoTime()-seedFallSince)/1e9;double ground=PreparationPhysics.inside(drop.x(),drop.z(),.17)?PreparationPhysics.seedRestY():.025;seed=new Point(drop.x(),Math.max(ground,drop.y()-.5*PreparationPhysics.GRAVITY*t*t),drop.z());if(seed.y()<=ground){falling=false;landedAt=System.nanoTime();seedOnSoil=ground>.1;}}
        if(state.operation()==PotNetwork.PLANT&&!falling&&!seedHeld&&landedAt!=0&&!seedOnSoil&&System.nanoTime()-landedAt>650_000_000L){seed=new Point(.95,.83,.48);landedAt=0;}
    }
    private void bar(GuiGraphics g,int y,float n,String title){int x=(int)left+628,yy=(int)top+y;g.fill(x,yy,x+242,yy+9,0xFF0B1517);g.fill(x,yy,x+(int)(242*Math.clamp(n,0,1)),yy+9,0xFF779663);label(g,title,628,y+17,MUTED);}
    private String step(){return switch(state.operation()){case PotNetwork.CUT->"01  ROZETNIJ WOREK";case PotNetwork.POUR->"02  WSYP ZIEMIĘ";case PotNetwork.PLANT->packetOpened?"04  UPUŚĆ NASIONO":"03  OTWÓRZ WORECZEK";case PotNetwork.COVER->falling?"NASIONO OPADA":"05  ZASYP DOŁEK";case PotNetwork.WATER->"06  PODLEJ ROŚLINĘ";default->state.stage()==0?"PRZYGOTUJ WOREK ZIEMI":state.stage()==1?"PRZYGOTUJ NASIONO":state.stage()==4?"GOTOWA DO ZBIORU":"PODLEWANIE I WZROST";};}
    private String hint(){if(state.operation()==0&&mode()==PotNetwork.POUR&&selected().getItem() instanceof GardenSupplyItem it&&it.opened(selected()))return "Ten worek jest już rozcięty. Kolejne użycie nie wymaga otwierania. Wybierz nowy zamknięty worek, aby rozciąć zgrzew.";return switch(state.operation()){case PotNetwork.CUT->"Przeciągnij ostrze LPM wzdłuż złotego szwu od kropki do końca.";case PotNetwork.POUR,PotNetwork.WATER->"Trzymaj LPM i przesuwaj myszą. A/D przechyla na boki, W/S w przód i w tył. Sam ustaw kąt, aby sypać lub podlewać.";case PotNetwork.PLANT->packetOpened?"Chwyć nasiono LPM. Celuj kursorem w dołek. Obręcz pokazuje miejsce upadku; puść, aby upuścić nasiono.":"Rozsuń zamknięcie woreczka, przeciągając LPM po oznaczonym szwie.";case PotNetwork.COVER->falling?"Nasiono opada do dołka. Poczekaj, aż się zatrzyma.":"Nasiono jest w dołku. Przytrzymaj LPM na ziemi i przysuń ją nad nasiono.";default->"Wybierz materiał z ekwipunku i rozpocznij. Zatwierdzony postęp jest zachowywany.";};}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        long now=System.nanoTime();double dt=Math.min(.12,(now-lastFrame)/1e9);lastFrame=now;mouseX=mx/scale;mouseY=my/scale;advance(dt);
        background(g,"P R Z Y G O T O W A N I E","AURELIA / WŁASNE STANOWISKO");int x=(int)left,y=(int)top;
        label(g,state.lamp()?"UV · OŚWIETLENIE FIOLETOWE":state.frame()?"STELAŻ · BEZ LAMPY":"ZWYKŁE ŚWIATŁO",628,82,GREEN);label(g,step(),628,115,INK);wrapped(g,hint(),628,143,242,MUTED,4);
        var stack=selected();previous.active=next.active=pending==null&&state.operation()==0&&sources().size()>1;wrapped(g,stack.isEmpty()?state.stage()==4?"Wymagane nożyce":"Brak materiału":stack.getHoverName().getString(),667,199,165,GREEN,2);
        if(!stack.isEmpty()){g.renderItem(stack,x+632,y+237);String q=stack.getItem() instanceof GardenSupplyItem it?it.kind==GardenSupplyItem.Kind.WATER?"Woda: "+it.contents(stack)+" / "+it.capacity:it.kind==GardenSupplyItem.Kind.SOIL?"Napełnienia: "+it.contents(stack)+" / "+it.capacity:"Ilość: "+stack.getCount():"Ilość: "+stack.getCount();label(g,q,657,241,MUTED);}
        action.visible=state.operation()==0;action.active=pending==null&&(state.stage()==4||!stack.isEmpty());action.setMessage(net.minecraft.network.chat.Component.literal(state.stage()==4?"Przejdź do zbioru":mode()==PotNetwork.POUR?stack.getItem() instanceof GardenSupplyItem it&&it.opened(stack)?"Weź otwarty worek":"Rozetnij worek":mode()==PotNetwork.PLANT?"Przygotuj sadzenie":"Weź konewkę"));
        if(state.operation()!=0)bar(g,282,state.operation()==PotNetwork.CUT||state.operation()==PotNetwork.PLANT&&!packetOpened?cut:state.work()/1000f,pending!=null?"Potwierdzanie…":state.operation()==PotNetwork.PLANT?"Nasiono zużyje się po zasypaniu":"Postęp czynności");
        bar(g,334,state.growth()/(float)CultivationRules.GROWTH_TICKS,"Wzrost "+state.growth()*100/CultivationRules.GROWTH_TICKS+"% • woda "+state.water()+" / 100");label(g,state.lit()?"ŚWIATŁO: DOSTĘPNE":"BRAK ŚWIATŁA • WZROST WSTRZYMANY",628,375,state.lit()?GREEN:0xFFE5B391);
        g.fill(x+628,y+398,x+871,y+447,0x7021332E);wrapped(g,state.message(),639,406,220,GREEN,2);
        drawStage(g,stack);
        g.flush();RenderSystem.depthMask(true);RenderSystem.clear(256,Minecraft.ON_OSX);RenderSystem.disableDepthTest();
        if(state.operation()==PotNetwork.CUT||state.operation()==PotNetwork.PLANT&&!packetOpened){var a=seamStart();var b=seamEnd();for(int i=0;i<=40;i++){float t=i/40f;int xx=Math.round(a.x+(b.x-a.x)*t),yy=Math.round(a.y+(b.y-a.y)*t);g.fill(xx-1,yy-1,xx+2,yy+2,t<=cut?0xFFC5EB92:0xFFE2BF68);}g.fill((int)a.x-4,(int)a.y-4,(int)a.x+4,(int)a.y+4,0xFFEBE7BC);}
        label(g,"LPM + mysz: przesuwanie  •  WASD: obrót worka / konewki",29,475,MUTED);label(g,"PPM na tle: obrót widoku  •  Esc: zamknij",29,494,MUTED);end(g,mx,my,delta);
    }
    private void objectPose(com.mojang.blaze3d.vertex.PoseStack p,Pose tool){p.translate(tool.x(),tool.y(),tool.z());p.mulPose(Axis.XP.rotation((float)tool.pitch()));p.mulPose(Axis.ZP.rotation((float)tool.roll()));p.scale(.75f,.75f,.75f);p.translate(-.5,-.45,-.5);}
    private void drawStage(GuiGraphics g,ItemStack selected){
        float view=300-35*state.growth()/CultivationRules.GROWTH_TICKS;camera=new Matrix4f().translate(left+290,top+431,160).scale(view,-view,view).rotateX(.64f).rotateY(yaw).translate(-.5f,0,-.5f);
        g.flush();g.enableScissor((int)((left+12)*scale),(int)((top+70)*scale),(int)((left+600)*scale),(int)((top+458)*scale));RenderSystem.enableDepthTest();Lighting.setupFor3DItems();var buffers=minecraft.renderBuffers().bufferSource();var p=g.pose();p.pushPose();p.mulPose(camera);
        float hole=state.stage()==1?(state.operation()==PotNetwork.COVER?1-state.work()/1000f:1):0;
        GardenMesh.pot(p,buffers,visualFill,state.water()>0,LightTexture.FULL_BRIGHT,hole);
        if(state.stage()>=2){p.pushPose();p.translate(.11,PreparationPhysics.TOP,.11);p.scale(.78f,.86f,.78f);AureliaMesh.draw(p,buffers,Math.clamp(state.growth()*4/CultivationRules.GROWTH_TICKS,0,4),state.total()>0?state.leaves():63,-1,0,LightTexture.FULL_BRIGHT,state.total()>0?state.total():6);p.popPose();}
        int op=state.operation();ItemStack shown=source.isEmpty()?selected:source;
        if(op==PotNetwork.CUT||op==PotNetwork.POUR||op==PotNetwork.WATER||op==0&&state.stage()==0){p.pushPose();objectPose(p,op==PotNetwork.CUT?cutPose():tool);if(shown.getItem() instanceof GardenSupplyItem it){if(it.kind==GardenSupplyItem.Kind.WATER)GardenMesh.can(p,buffers,it.contents(shown),LightTexture.FULL_BRIGHT);else if(it.kind==GardenSupplyItem.Kind.SOIL)GardenMesh.bag(p,buffers,it.capacity,false,op==PotNetwork.CUT?cut:it.opened(shown)?1:0,LightTexture.FULL_BRIGHT);}p.popPose();}
        if(op==PotNetwork.PLANT&&!packetOpened){p.pushPose();objectPose(p,cutPose());GardenMesh.bag(p,buffers,5,true,cut,LightTexture.FULL_BRIGHT);p.popPose();}
        if(op==PotNetwork.PLANT&&packetOpened){GardenMesh.landingGuide(p,buffers,.5,.5,true,LightTexture.FULL_BRIGHT);if(seedHeld){boolean valid=PreparationPhysics.inside(seed.x(),seed.z(),.17);for(double y=PreparationPhysics.seedBedY(.5,.5)+.02;y<seed.y()-.025;y+=.045)GardenMesh.box(p,buffers,GardenMaterials.WHITE,seed.x()-.0015,y,seed.z()-.0015,seed.x()+.0015,y+.012,seed.z()+.0015,LightTexture.FULL_BRIGHT,valid?0xFFDBC080:0xFF999B91);}}
        if((op==PotNetwork.PLANT&&packetOpened)||op==PotNetwork.COVER){p.pushPose();p.translate(seed.x(),seed.y(),seed.z());if(falling){p.mulPose(Axis.YP.rotation((float)((System.nanoTime()-seedFallSince)/1e9*.9)));p.mulPose(Axis.ZP.rotation((float)Math.sin((System.nanoTime()-seedFallSince)/1e9*3)*.16f));}GardenMesh.seed(p,buffers,LightTexture.FULL_BRIGHT);p.popPose();}
        if(covering){p.pushPose();p.translate(hand.x(),hand.y()+.025,hand.z());GardenMesh.glove(p,buffers,LightTexture.FULL_BRIGHT);p.popPose();}
        if(op==PotNetwork.CUT||op==PotNetwork.PLANT&&!packetOpened){
            p.pushPose();objectPose(p,cutPose());p.translate(.275+.45*cut+.19,.838,.365);
            p.mulPose(Axis.YP.rotationDegrees(180));GardenMesh.knife(p,buffers,LightTexture.FULL_BRIGHT);p.popPose();
        }
        for(var part:particles){var stream=new Stream(part.start,part.velocity,true);
            if(part.water){GardenMesh.waterStream(p,buffers,stream,part.age,LightTexture.FULL_BRIGHT);continue;}
            var pos=PreparationPhysics.at(stream,part.age);double size=.006;
            GardenMesh.box(p,buffers,GardenMaterials.SOIL,pos.x()-size,pos.y()-size,pos.z()-size,pos.x()+size,pos.y()+size,pos.z()+size,LightTexture.FULL_BRIGHT,-1);
        }
        buffers.endBatch();p.popPose();RenderSystem.disableDepthTest();g.disableScissor();
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;double lx=x/scale,ly=y/scale;if(lx<left+10||lx>left+600||ly<top+70||ly>top+460)return false;
        if(button==1&&!grabbed&&!seedHeld){rotating=true;return true;}if(button!=0||pending!=null)return false;int op=state.operation();
        if(op==PotNetwork.CUT||op==PotNetwork.PLANT&&!packetOpened){var a=seamStart();if(Math.hypot(lx-a.x,ly-a.y)<25){cutting=true;cutSince=System.nanoTime();cutAt(lx,ly);return true;}}
        if(op==PotNetwork.POUR||op==PotNetwork.WATER){if(toolHit(lx,ly)){grabbed=true;var at=unproject(lx,ly,tool.y());grabOffset=new Point(tool.x()-at.x(),0,tool.z()-at.z());control();return true;}}
        if(op==PotNetwork.PLANT&&packetOpened&&!falling){var at=project(seed);if(Math.hypot(lx-at.x,ly-at.y)<25){seedHeld=true;seedOnSoil=false;landedAt=0;control();return true;}}
        if(op==PotNetwork.COVER&&!falling){var at=unproject(lx,ly,PreparationPhysics.TOP);if(PreparationPhysics.inside(at.x(),at.z(),.25)){hand=at;covering=true;control();return true;}}
        if(op==0&&mode()!=PotNetwork.HARVEST&&toolHit(lx,ly)){start();return true;}return false;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){double lx=x/scale,ly=y/scale;if(cutting){cutAt(lx,ly);return true;}if(grabbed){var at=unproject(lx,ly,tool.y());tool=new Pose(Math.clamp(at.x()+grabOffset.x(),-.40,1.40),tool.y(),Math.clamp(at.z()+grabOffset.z(),-.40,1.40),tool.roll(),tool.pitch());return true;}
        if(seedHeld){var at=unproject(lx,ly,PreparationPhysics.TOP);seed=aimSeed(at.x(),at.z());return true;}if(covering){var at=unproject(lx,ly,PreparationPhysics.TOP);if(PreparationPhysics.inside(at.x(),at.z(),.25))hand=at;return true;}if(rotating){yaw+=(float)(dx/scale*.006);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    private Point aimSeed(double x,double z){if(PreparationPhysics.inside(x,z,.17))return new Point(.5,.83,.5);return new Point(Math.clamp(x,-.35,1.35),.83,Math.clamp(z,-.35,1.35));}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==1)rotating=false;if(button==0){if(cutting){cutting=false;return true;}if(grabbed){grabbed=false;control();return true;}if(seedHeld){seedHeld=false;if(PreparationPhysics.inside(seed.x(),seed.z(),.17))seed=new Point(.5,.83,.5);control();drop=seed;falling=true;seedFallSince=System.nanoTime();send(PotNetwork.DROP);return true;}if(covering){covering=false;control();return true;}}return super.mouseReleased(x,y,button);}
    @Override public boolean keyPressed(int k,int scan,int mods){if(k==GLFW.GLFW_KEY_W||k==GLFW.GLFW_KEY_A||k==GLFW.GLFW_KEY_S||k==GLFW.GLFW_KEY_D){keys.add(k);return true;}return super.keyPressed(k,scan,mods);}
    @Override public boolean keyReleased(int k,int scan,int mods){if(keys.remove(k))return true;return super.keyReleased(k,scan,mods);}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(x/scale>left+600){cycle(v>0?-1:1);return true;}return false;}
    @Override public void removed(){if(state.active()&&minecraft.getConnection()!=null)PotNetwork.action(state,PotNetwork.CLOSE,-1);}
}
