package pl.crimeandlaw.client.cultivation;
import java.nio.file.*;
import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import pl.crimeandlaw.cultivation.*;

@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class GardenClientSmoke {
    private static final String ROLE=System.getProperty("crimeandlaw.gardenRole","");
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.gardenOutput","build/garden-smoke"));
    private static final BlockPos A=new BlockPos(0,80,0);
    private static final int[] OFFERS={6,5,1,9,7,8,10},ORDER={7,2,5,0,6,1,4,3};
    private static final int[][] CASES={{1920,1080,2},{1920,1080,4},{855,481,2},{640,480,2}};
    private static java.util.concurrent.CompletableFuture<Void> reload;private static int gesture;private static boolean waterMoved;private static pl.crimeandlaw.cultivation.PreparationPhysics.Pose parked;
    private static int rimContacts,wallContacts;private static Object trackedFall;private static boolean fallMiddle,fallLower;
    private static int step=-1,frames,bought,cuts,round,waitFrames;private static boolean done,buying;private static long start,pressed;private static long expectedCash;private static boolean shopRecovered;
    @SubscribeEvent public static void frame(RenderFrameEvent.Post e){if(ROLE.isEmpty()||done)return;var mc=Minecraft.getInstance();try{
        if(start==0){start=System.nanoTime();Files.createDirectories(OUT);}check(System.nanoTime()-start<440_000_000_000L,"Client timeout "+ROLE+" step="+step);
        if(mc.getOverlay()==null&&mc.screen instanceof AccessibilityOnboardingScreen a){mc.options.onboardingAccessibilityFinished();a.onClose();return;}
        if(ROLE.equals("visual")){visual(mc);return;}if(++frames%3!=0)return;if(frames%30==0&&mc.screen instanceof PreparationScreen z)Files.writeString(OUT.resolve("debug-client.txt"),"step="+step+" op="+z.state().operation()+" work="+z.state().work()+" pose="+z.toolPose()+" grabbed="+field(z,"grabbed")+" pending="+field(z,"pending"));
        if(step<0){if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getToasts().clear();mc.options.guiScale().set(2);mc.options.framerateLimit().set(45);mc.resizeDisplay();ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25598"),new ServerData("Garden QA","127.0.0.1:25598",ServerData.Type.OTHER),false,null);step=0;return;}
        if(mc.player==null||mc.level==null)return;
        if(mc.screen instanceof PreparationScreen prep&&(prep.state().operation()==PotNetwork.POUR||prep.state().operation()==PotNetwork.WATER)){
            double fill=((Number)field(prep,"visualFill")).doubleValue();
            for(var part:(java.util.List<?>)field(prep,"particles")){
                boolean water=(boolean)field(part,"water"),stopped=(boolean)field(part,"stopped");
                var stream=new PreparationPhysics.Stream((PreparationPhysics.Point)field(part,"start"),(PreparationPhysics.Point)field(part,"velocity"),true);
                var point=PreparationPhysics.at(stream,((Number)field(part,"age")).doubleValue());
                if(!water)check(!PreparationPhysics.insideBag(prep.toolPose(),point,.007),"Rendered soil particle never penetrates rotating bag");
                var surface=PotCollision.surface(point,fill,water?.0018:.006);
                if(!stopped)check(surface==null,"Moving particle cannot cross pot material");
                if(surface==PotCollision.Surface.RIM)rimContacts++;if(surface==PotCollision.Surface.WALL)wallContacts++;
            }
        }
        if(ROLE.equals("rival")){
            if(step==0&&Files.exists(OUT.resolve("owner-open.ready"))&&mc.screen==null&&mc.player.tickCount>40){PotNetwork.open(A);pressed=System.nanoTime();step=1;return;}
            if(step==1&&System.nanoTime()-pressed>2_000_000_000L){check(!(mc.screen instanceof PreparationScreen),"Rival cannot open owner's locked pot");Files.writeString(OUT.resolve("rival-attempt.sent"),"actual packet sent");Files.writeString(OUT.resolve("rival.PASS"),"Second TCP player denied same preparation session\n");done=true;}return;
        }
        switch(step){
            case 0->{if(mc.screen!=null||mc.getOverlay()!=null||mc.player.tickCount<40||!Files.exists(OUT.resolve("server.ready")))return;var entities=mc.level.entitiesForRendering();for(var entity:entities)if(entity instanceof GardenMerchant m){mc.gameMode.interact(mc.player,m,InteractionHand.MAIN_HAND);step=1;return;}}
            case 1->{if(!(mc.screen instanceof GardenShopScreen s))return;if(bought==0&&!buying)capture(mc,"shop");
                if(buying){if(s.state().cash()!=expectedCash)return;check(s.state().cash()==expectedCash,"Purchase debits exact price");buying=false;bought++;}
                if(bought==OFFERS.length){capture(mc,"purchased");s.onClose();step=2;return;}
                if(bought==1&&!shopRecovered){set(s,"pending",s.state().quote());set(s,"awaitingSince",System.nanoTime());pressed=System.nanoTime();expectedCash=s.state().cash();step=24;return;}
                int offer=OFFERS[bought];int targetScroll=Math.max(0,offer-5);int current=(int)field(s,"scroll");while(current!=targetScroll){s.mouseScrolled(100,100,0,current<targetScroll?-1:1);current=(int)field(s,"scroll");}
                click(s,100,84+(offer-targetScroll)*62+20);var before=s.state();expectedCash=before.cash()-GardenCatalog.OFFERS.get(offer).price();click(s,650,390);if(bought==0)ShopNetwork.request(before,ShopNetwork.BUY,offer);buying=true;
            }
            case 2->{if(mc.screen!=null)return;select(mc,CultivationContent.FRAME_ITEM.get());mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(.5,80,.5),Direction.UP,A.below(),false));step=3;}
            case 3->{if(!mc.level.getBlockState(A).is(CultivationContent.FRAME.get()))return;select(mc,CultivationContent.POT_ITEM.get());useBlock(mc,A);step=4;}
            case 4->{var p=Planter.at(mc.level,A);if(p==null||!p.hasPot())return;capture(mc,"night-before-uv");select(mc,CultivationContent.UV_LAMP.get());useBlock(mc,A.above());step=5;}
            case 5->{var p=Planter.at(mc.level,A);if(p==null||!p.hasLamp())return;select(mc,CultivationContent.WATERING_CAN.get());look(mc,new Vec3(-1.5,80.5,1.5));mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);step=6;}
            case 6->{if(CultivationContent.WATERING_CAN.get().contents(mc.player.getMainHandItem())!=400)return;look(mc,new Vec3(.5,80.5,.5));if(!(mc.hitResult instanceof BlockHitResult h)||!Planter.base(mc.level,h.getBlockPos()).equals(A))return;if(UvLighting.activeLights()==0||UvLighting.renderedFrames()<3)return;capture(mc,"night-purple-uv");check(purpleFloor(mc),"UV colors floor below fitting in Fancy");Files.writeString(OUT.resolve("uv-second.request"),"add second lamp");step=49;}
            case 7->{if(!(mc.screen instanceof PreparationScreen s))return;Files.writeString(OUT.resolve("owner-open.ready"),"opened");set(s,"pending",s.state().revision());set(s,"awaitingSince",System.nanoTime());pressed=System.nanoTime();step=23;}
            case 23->{var s=(PreparationScreen)mc.screen;if(field(s,"pending")!=null)return;check(System.nanoTime()-pressed>=3_000_000_000L,"Prep recovery waits three seconds");check(s.state().stage()==0&&mc.player.getInventory().countItem(CultivationContent.SOIL_LARGE.get())==1,"Recovery doesn't use soil");Files.writeString(OUT.resolve("preparation-recovery.PASS"),"Unprocessed request recovers after three seconds through cancellation/state refresh, no materials spent\n");round=0;configure(mc,round);waitFrames=0;step=8;}
            case 24->{var s=(GardenShopScreen)mc.screen;if(field(s,"pending")!=null)return;check(System.nanoTime()-pressed>=3_000_000_000L,"Shop recovery waits three seconds");check(s.state().cash()==expectedCash&&mc.player.getInventory().countItem(CultivationContent.POT_ITEM.get())==1,"Shop recovery doesn't repeat purchase");Files.writeString(OUT.resolve("shop-recovery.PASS"),"Quote refreshed after three seconds without replaying purchase or changing funds\n");shopRecovered=true;step=1;}
            case 8->{if(++waitFrames<7)return;var s=(PreparationScreen)mc.screen;check(s.state().stage()==0,"Preparation starts empty");capture(mc,"responsive-"+round);if(++round<CASES.length){configure(mc,round);waitFrames=0;return;}configure(mc,0);step=9;}
            case 9->{if(!Files.exists(OUT.resolve("rival.PASS")))return;var s=(PreparationScreen)mc.screen;capture(mc,"empty-black-pot");click(s,740,291);gesture=0;pressed=System.nanoTime();step=25;}
            case 25->{var s=(PreparationScreen)mc.screen;if(s.state().operation()!=PotNetwork.CUT)return;if(System.nanoTime()-pressed<450_000_000L){check(((Number)field(s,"cut")).floatValue()==0,"Fresh sealed bag cannot open without cutting gesture");capture(mc,"sealed-bag-before-cut");return;}if(gesture==6)capture(mc,"half-cut-bag");if(seam(s)){capture(mc,"bag-cut");step=26;}}
            case 26->{var s=(PreparationScreen)mc.screen;if(s.state().operation()!=PotNetwork.POUR)return;grabTool(s);parked=s.toolPose();pressed=System.nanoTime();step=54;}
            case 10->{var s=(PreparationScreen)mc.screen;if(s.toolPose().roll()<3.10)return;s.keyReleased(GLFW.GLFW_KEY_A,0,0);dragTool(s,.5+.34*Math.cos(Math.PI/8),1,.5);pressed=System.nanoTime();step=57;}
            case 57->{var s=(PreparationScreen)mc.screen;if(System.nanoTime()-pressed<800_000_000L)return;check(rimContacts>0,"Soil visibly collides with upper rim");capture(mc,"soil-stops-at-rim");dragTool(s,.5+.27*Math.cos(Math.PI/8),1,.5);pressed=System.nanoTime();step=58;}
            case 58->{var s=(PreparationScreen)mc.screen;if(System.nanoTime()-pressed<600_000_000L)return;check(wallContacts>0,"Soil visibly collides with tapered inner wall");capture(mc,"soil-stops-at-wall");dragTool(s,.5,1,.5);Files.writeString(OUT.resolve("pot-collision.PASS"),"Actual rim and wall contacts; every moving soil/water particle checked against solid pot throughout GUI operations");capture(mc,"tilted-bag-stream");step=11;}
            case 11->{var s=(PreparationScreen)mc.screen;if(s.state().work()<300)return;s.mouseReleased(0,0,0);parked=s.toolPose();pressed=System.nanoTime();step=12;}
            case 12->{if(System.nanoTime()-pressed<1_100_000_000L)return;var s=(PreparationScreen)mc.screen;check(parked.equals(s.toolPose()),"Released soil bag floats in place");check(s.state().stage()==0&&s.state().work()<500,"Released bag pauses filling without spending material");capture(mc,"bag-floating-paused");grabTool(s);step=30;}
            case 30->{var s=(PreparationScreen)mc.screen;if(s.state().stage()!=1)return;capture(mc,"soil-contained-full");click(s,740,291);gesture=0;step=13;}
            case 13->{var s=(PreparationScreen)mc.screen;if(s.state().operation()!=PotNetwork.PLANT)return;if(seam(s))step=31;}
            case 31->{var s=(PreparationScreen)mc.screen;if(!(boolean)field(s,"packetOpened"))return;check(mc.player.getInventory().countItem(CultivationContent.SEED_PACKET.get())==1,"Packet held until seed is actually covered");seedDrop(s,.99,.5);pressed=System.nanoTime();step=32;}
            case 32->{if(System.nanoTime()-pressed<2_100_000_000L)return;var s=(PreparationScreen)mc.screen;check(s.state().operation()==PotNetwork.PLANT&&mc.player.getInventory().countItem(CultivationContent.SEED_PACKET.get())==1,"Missed falling seed costs no packet or seed");capture(mc,"seed-missed-recovered");seedDrop(s,.5,.5);step=14;}
            case 14->{var s=(PreparationScreen)mc.screen;if(s.state().operation()!=PotNetwork.COVER||(boolean)field(s,"falling"))return;capture(mc,"seed-in-hole");var point=project(s,new PreparationPhysics.Point(.40,PreparationPhysics.TOP,.5));s.mouseClicked(point.x*s.scale,point.y*s.scale,0);gesture=0;step=33;}
            case 33->{var s=(PreparationScreen)mc.screen;if(s.state().stage()==2){capture(mc,"seed-covered");check(mc.player.getInventory().countItem(CultivationContent.SEEDS.get())==4&&mc.player.getInventory().countItem(CultivationContent.SEED_PACKET.get())==0,"Covering spends one seed and returns four from packet");click(s,740,291);step=15;return;}var pt=project(s,new PreparationPhysics.Point(.5+Math.sin(gesture*.55)*.095,PreparationPhysics.TOP,.5));s.mouseDragged(pt.x*s.scale,pt.y*s.scale,0,0,0);if(++gesture==12)capture(mc,"covering-seed");}
            case 15->{var s=(PreparationScreen)mc.screen;if(s.state().operation()!=PotNetwork.WATER)return;grabTool(s);dragTool(s,.5,1,.5);parked=s.toolPose();pressed=System.nanoTime();step=55;}
            case 34->{var s=(PreparationScreen)mc.screen;
                if(!waterStartCaptured&&PreparationPhysics.stream(s.toolPose(),true,true).flowing()){
                    for(var part:(java.util.List<?>)field(s,"particles"))if((boolean)field(part,"water")){
                        double age=((Number)field(part,"age")).doubleValue();
                        var stream=new PreparationPhysics.Stream((PreparationPhysics.Point)field(part,"start"),(PreparationPhysics.Point)field(part,"velocity"),true);
                        check(age<PreparationPhysics.flight(stream.source(),stream.velocity(),PreparationPhysics.TOP),"Initial water front has not instantly reached soil");
                        capture(mc,"water-first-moving-front");waterStartCaptured=true;break;
                    }
                }
                if(s.toolPose().roll()>-Math.PI/2)return;s.keyReleased(GLFW.GLFW_KEY_D,0,0);step=35;}
            case 35->{var s=(PreparationScreen)mc.screen;if(s.state().operation()==PotNetwork.WATER&&s.state().work()>200&&!waterMoved){var before=PreparationPhysics.stream(s.toolPose(),true,true).source();dragTool(s,.45,1,.5);var after=PreparationPhysics.stream(s.toolPose(),true,true).source();check(waterStartCaptured,"Water starts progressively at rose");check(Math.abs(after.x()-before.x()+.05)<.002,"Actual mouse movement carries water emitter with spout");waterMoved=true;capture(mc,"water-follows-can");}if(s.state().stage()==3){capture(mc,"watered");step=16;}}
            case 16->{var s=(PreparationScreen)mc.screen;if(s.state().stage()!=3)return;capture(mc,"planted-watered");Files.writeString(OUT.resolve("grow.ready"),"planted and watered");select(mc,CultivationContent.SHEARS.get());pressed=System.nanoTime();step=17;}
            case 17->{var s=(PreparationScreen)mc.screen;if(s.state().stage()!=4||System.nanoTime()-pressed<1_000_000_000L)return;capture(mc,"mature-eight-leaves");s.onClose();mc.options.hideGui=true;uvPose(mc,.5,80.15,2.1,new Vec3(.5,80.95,.5));pressed=System.nanoTime();step=61;}
            case 61->{if(mc.screen instanceof PreparationScreen prep){prep.onClose();pressed=System.nanoTime();return;}if(mc.screen!=null||System.nanoTime()-pressed<1_200_000_000L||!mc.levelRenderer.hasRenderedAllSections())return;capture(mc,"mature-crop-upper-leaves-uv");mc.options.hideGui=false;PotNetwork.open(A);step=18;}
            case 18->{if(!(mc.screen instanceof HarvestScreen s))return;var state=(CultivationNetwork.State)field(s,"state");check(state.total()==8,"UV crop displays eight actual leaves");capture(mc,"harvest-eight");step=19;}
            case 19->{var s=(HarvestScreen)mc.screen;var p=marker(s,ORDER[cuts]);float scale=((Number)field(s,"scale")).floatValue();s.mouseClicked(p.x*scale,p.y*scale,0);pressed=System.nanoTime();step=20;}
            case 20->{if(System.nanoTime()-pressed<1_000_000_000L)return;var s=(HarvestScreen)mc.screen;var p=marker(s,ORDER[cuts]);float scale=((Number)field(s,"scale")).floatValue();s.mouseReleased(p.x*scale,p.y*scale,0);step=21;}
            case 21->{int n=mc.player.getInventory().countItem(CultivationContent.LEAF.get());if(n==cuts)return;check(n==cuts+1,"One reward per real snip");var s=(HarvestScreen)mc.screen;var state=(CultivationNetwork.State)field(s,"state");CultivationNetwork.cut(state.token(),ORDER[cuts],1);cuts++;check(mc.player.getMainHandItem().getDamageValue()==cuts,"One wear per snip");capture(mc,"cut-"+cuts);pressed=System.nanoTime();step=44;}
            case 54,55->{if(System.nanoTime()-pressed<450_000_000L)return;var s=(PreparationScreen)mc.screen;check(parked.equals(s.toolPose()),"Holding a tool without keys never tilts automatically");if(step==54){s.keyPressed(GLFW.GLFW_KEY_A,0,0);pressed=System.nanoTime();step=40;}else{s.keyPressed(GLFW.GLFW_KEY_D,0,0);step=34;}}
            case 40,41,42,43->{if(System.nanoTime()-pressed<220_000_000L)return;var s=(PreparationScreen)mc.screen;int[] keys={GLFW.GLFW_KEY_A,GLFW.GLFW_KEY_D,GLFW.GLFW_KEY_W,GLFW.GLFW_KEY_S};s.keyReleased(keys[step-40],0,0);var now=s.toolPose();check(now.x()==parked.x()&&now.y()==parked.y()&&now.z()==parked.z(),"WASD rotates rather than translating tool");check(switch(step){case 40->now.roll()>parked.roll()+.15;case 41->now.roll()<parked.roll()-.15;case 42->now.pitch()<parked.pitch()-.15;default->now.pitch()>parked.pitch()+.15;},"WASD changes expected rotation: "+keys[step-40]);if(step==43){dragTool(s,.5,1,.5);s.keyPressed(GLFW.GLFW_KEY_A,0,0);Files.writeString(OUT.resolve("wasd.PASS"),"Actual A/D roll and W/S pitch, fixed position, no automatic bag/can tilt; mouse moves tool");step=10;}else{parked=s.toolPose();s.keyPressed(keys[step-39],0,0);pressed=System.nanoTime();step++;}}
            case 44->{if(System.nanoTime()-pressed<330_000_000L)return;capture(mc,"falling-leaf-"+cuts);if(cuts==1){trackedFall=((java.util.List<?>)field(mc.screen,"falling")).getFirst();step=60;}else step=cuts==8?22:19;}
            case 60->{var s=(HarvestScreen)mc.screen;double elapsed=(System.nanoTime()-(long)field(trackedFall,"started"))/1e9;
                var leaf=AureliaMesh.leaf((int)field(trackedFall,"leaf"),(int)field(trackedFall,"total"));var motion=LeafFall.transform(leaf,(LeafMotion.Motion)field(trackedFall,"motion"),elapsed);
                float bottom=s.height/((Number)field(s,"scale")).floatValue();boolean below=LeafFall.below((org.joml.Matrix4f)field(s,"camera"),motion,leaf,bottom);
                var list=(java.util.List<?>)field(s,"falling");
                if(list.isEmpty()){check(below,"Leaf removed only after entire silhouette leaves Minecraft viewport");Files.writeString(OUT.resolve("leaf-fall.PASS"),"Real snip: rigid falling leaf continues past panel/old floor, removed only wholly below Minecraft viewport");step=19;return;}
                if(elapsed>.9&&!fallMiddle){capture(mc,"fall-past-old-floor");fallMiddle=true;}
                if(elapsed>1.3&&!fallLower){capture(mc,"fall-towards-viewport-bottom");fallLower=true;}
            }
            case 49->{if(UvLighting.activeLights()!=2||!mc.levelRenderer.hasRenderedAllSections())return;if(uvMotion(mc,"fancy")){Files.writeString(OUT.resolve("uv-second-remove.request"),"remove");step=50;}}
            case 50->{if(UvLighting.activeLights()!=1)return;uvFrame=0;step=51;}
            case 51->{mc.options.hideGui=true;uvPose(mc,.5,80,3.1,new Vec3(.5,84,0));if(++uvFrame<12)return;uvCeiling=uvPixel(mc);capture(mc,"uv-ceiling-on");Files.writeString(OUT.resolve("uv-off.request"),"off");uvFrame=0;step=52;}
            case 52->{if(UvLighting.activeLights()!=0)return;uvPose(mc,.5,80,3.1,new Vec3(.5,84,0));if(++uvFrame<12)return;var off=uvPixel(mc);for(int i=0;i<3;i++)check(Math.abs(uvCeiling[i]-off[i])<3,"No UV above fitting");capture(mc,"uv-ceiling-off");mc.options.hideGui=false;uvPose(mc,.5,80,3.1,new Vec3(.5,80.5,.5));step=45;}
            case 53->{if(uvMotion(mc,"fabulous")){mc.options.graphicsMode().set(GraphicsStatus.FANCY);mc.levelRenderer.allChanged();mc.options.hideGui=false;uvPose(mc,.5,80,3.1,new Vec3(.5,80.5,.5));Files.writeString(OUT.resolve("uv.PASS"),"Downward light, ceiling unchanged, GL linear filtering, two lamps and moving camera in Fancy/Fabulous, removal and reload pass");KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z));step=7;}}
            case 45->{if(Planter.at(mc.level,A).hasLamp()||UvLighting.activeLights()!=0)return;check(!purpleFloor(mc),"Removing UV clears world color");capture(mc,"uv-removed-room-normal");Files.writeString(OUT.resolve("uv-on.request"),"restore lamp");step=46;}
            case 46->{if(!Planter.at(mc.level,A).hasLamp()||UvLighting.activeLights()==0)return;reload=mc.reloadResourcePacks();step=47;}
            case 47->{if(reload==null||!reload.isDone()||mc.getOverlay()!=null||UvLighting.activeLights()==0)return;reload.join();check(purpleFloor(mc),"UV world lighting survives resource reload");capture(mc,"uv-after-resource-reload");mc.options.graphicsMode().set(GraphicsStatus.FABULOUS);mc.levelRenderer.allChanged();pressed=System.nanoTime();step=48;}
            case 48->{if(System.nanoTime()-pressed<2_000_000_000L||UvLighting.activeLights()==0||!mc.levelRenderer.hasRenderedAllSections())return;check(purpleFloor(mc),"UV colors floor below fitting in Fabulous");capture(mc,"uv-fabulous");uvFrame=0;step=53;}
            case 22->{var s=(HarvestScreen)mc.screen;var state=(CultivationNetwork.State)field(s,"state");if(state.active())return;check(Planter.at(mc.level,A)!=null&&!Planter.at(mc.level,A).plant.soil(),"No automatic regrowth or soil refund");capture(mc,"cycle-complete");s.onClose();Files.writeString(OUT.resolve("owner.PASS"),"Actual shop and replay, night UV, 3D seam cutting, LPM grab, manual WASD rotation without automatic tilt, whole-mouth soil stream, floating pause, aimed filling, packet opening, falling missed/recovered seed, soil covering, moving water emitter, exact supplies, 8 free-order snips pass\n");done=true;}
        }
    }catch(Throwable t){t.printStackTrace();done=true;try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(ROLE+".FAIL"),t+" step="+step);}catch(Exception ignored){}mc.stop();}}
    private static void click(GardenScreen s,int x,int y){check(s.mouseClicked((s.left+x)*s.scale,(s.top+y)*s.scale,0),"Clickable UI control "+x+","+y);s.mouseReleased((s.left+x)*s.scale,(s.top+y)*s.scale,0);}
    private static Vector3f project(PreparationScreen s,PreparationPhysics.Point p)throws Exception{var matrix=(org.joml.Matrix4f)field(s,"camera");return matrix.transformPosition(new Vector3f((float)p.x(),(float)p.y(),(float)p.z()));}
    private static boolean seam(PreparationScreen s)throws Exception{var a=s.seamStart();var b=s.seamEnd();if(gesture==0){s.mouseClicked(a.x*s.scale,a.y*s.scale,0);gesture++;return false;}double t=Math.min(1,gesture/12.);double x=(a.x+(b.x-a.x)*t)*s.scale,y=(a.y+(b.y-a.y)*t)*s.scale;move(s,x,y);s.mouseDragged(x,y,0,0,0);gesture++;if(t<1)return false;s.mouseReleased(x,y,0);return true;}
    private static void grabTool(PreparationScreen s)throws Exception{var p=s.toolPose();var v=project(s,new PreparationPhysics.Point(p.x(),p.y(),p.z()));move(s,v.x*s.scale,v.y*s.scale);check(s.mouseClicked(v.x*s.scale,v.y*s.scale,0),"Grab actual 3D tool");}
    private static void dragTool(PreparationScreen s,double x,double y,double z)throws Exception{var v=project(s,new PreparationPhysics.Point(x,y,z));move(s,v.x*s.scale,v.y*s.scale);s.mouseDragged(v.x*s.scale,v.y*s.scale,0,0,0);}
    private static void seedDrop(PreparationScreen s,double x,double z)throws Exception{var seed=(PreparationPhysics.Point)field(s,"seed");var p=project(s,seed);check(s.mouseClicked(p.x*s.scale,p.y*s.scale,0),"Grab seed");p=project(s,new PreparationPhysics.Point(x,PreparationPhysics.TOP,z));move(s,p.x*s.scale,p.y*s.scale);s.mouseDragged(p.x*s.scale,p.y*s.scale,0,0,0);s.mouseReleased(p.x*s.scale,p.y*s.scale,0);}
    private static void move(PreparationScreen s,double x,double y)throws Exception{var mc=Minecraft.getInstance();var move=MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);move.setAccessible(true);move.invoke(mc.mouseHandler,mc.getWindow().getWindow(),x*mc.getWindow().getGuiScale(),y*mc.getWindow().getGuiScale());}
    private static void select(Minecraft mc,Item item){for(int i=0;i<9;i++)if(mc.player.getInventory().getItem(i).is(item)){mc.player.getInventory().selected=i;return;}throw new AssertionError("Missing hotbar item "+item);}
    private static void useBlock(Minecraft mc,BlockPos p){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(p.getX()+.5,p.getY()+.5,p.getZ()+1),Direction.SOUTH,p,false));}
    private static void look(Minecraft mc,Vec3 p){var d=p.subtract(mc.player.getEyePosition());mc.player.setYRot((float)(Math.toDegrees(Math.atan2(d.z,d.x))-90));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}
    private static void configure(Minecraft mc,int i){mc.getToasts().clear();int[] c=CASES[i];GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),c[0],c[1]);mc.options.guiScale().set(c[2]);mc.resizeDisplay();}
    @SuppressWarnings("unchecked")private static Vector3f marker(HarvestScreen s,int id)throws Exception{return ((Map<Integer,Vector3f>)field(s,"markers")).get(id);}
    private static Object field(Object o,String n)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(n);}
    private static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
    private static boolean waterStartCaptured;
    private static int uvFrame;private static double[] uvMin,uvMax,uvCeiling;
    private static void uvPose(Minecraft mc,double x,double y,double z,Vec3 target){var p=mc.player;p.setPos(x,y,z);p.xo=x;p.yo=y;p.zo=z;var d=target.subtract(p.getEyePosition());p.setYRot((float)(Math.toDegrees(Math.atan2(d.z,d.x))-90));p.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));p.yRotO=p.getYRot();p.xRotO=p.getXRot();}
    private static double[] uvPixel(Minecraft mc)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){double[] c=new double[3];for(int y=-5;y<=5;y++)for(int x=-5;x<=5;x++){int p=image.getPixelRGBA(image.getWidth()/2+x,image.getHeight()/2+y);c[0]+=p&255;c[1]+=p>>>8&255;c[2]+=p>>>16&255;}for(int i=0;i<3;i++)c[i]/=121;return c;}}
    private static boolean uvMotion(Minecraft mc,String mode)throws Exception{
        mc.options.hideGui=true;if(uvFrame==0){uvMin=new double[]{255,255,255};uvMax=new double[3];}
        var f=UvLighting.class.getDeclaredField("atlas");f.setAccessible(true);var atlas=(net.minecraft.client.renderer.texture.DynamicTexture)f.get(null);int previous=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);atlas.bind();check(org.lwjgl.opengl.GL11.glGetTexParameteri(3553,10241)==9729&&org.lwjgl.opengl.GL11.glGetTexParameteri(3553,10240)==9729,"Linear UV filtering survives uploads");com.mojang.blaze3d.platform.GlStateManager._bindTexture(previous);
        if(uvFrame>15){var c=uvPixel(mc);for(int i=0;i<3;i++){uvMin[i]=Math.min(uvMin[i],c[i]);uvMax[i]=Math.max(uvMax[i],c[i]);}}
        double t=uvFrame/120.0*Math.PI*2;uvPose(mc,.5+1.2*Math.sin(t),80,3.3+.1*Math.cos(t),new Vec3(.5,80.003,2.25));if(uvFrame%30==0)capture(mc,"uv-motion-"+mode+"-"+uvFrame);if(++uvFrame<135)return false;
        Files.writeString(OUT.resolve("uv-stability-"+mode+".json"),new com.google.gson.Gson().toJson(Map.of("minimum",uvMin,"maximum",uvMax,"samples",119,"filter",9729)));for(int i=0;i<3;i++)check(uvMax[i]-uvMin[i]<8,"Stable UV brightness under camera motion "+mode+" channel "+i+" range="+(uvMax[i]-uvMin[i]));uvFrame=0;return true;
    }
    private static boolean purpleFloor(Minecraft mc)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){long r=0,g=0,b=0;for(int y=image.getHeight()*73/100;y<image.getHeight()*81/100;y+=3)for(int x=image.getWidth()*42/100;x<image.getWidth()*58/100;x+=3){int c=image.getPixelRGBA(x,y);r+=c&255;g+=c>>>8&255;b+=c>>>16&255;}return r>g*1.2&&b>g*1.5;}}
    private static void capture(Minecraft mc,String n)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(ROLE+"-"+n+".png"));}}
    private static void check(boolean ok,String n){if(!ok)throw new AssertionError(n);}
    private static void visual(Minecraft mc)throws Exception{
        if(step<0){if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;var rr=new PlanterRenderer(null);var rack=new Planter(A,CultivationContent.FRAME.get().defaultBlockState());check(rr.getRenderBoundingBox(rack).getYsize()==2,"Frame culling covers both blocks");check(!rr.shouldRenderOffScreen(rack),"Frame does not disable frustum culling for whole farm");configure(mc,0);mc.setScreen(new Gallery());step=0;frames=0;return;}
        if(++frames<35)return;capture(mc,"new-equipment-gallery");Files.writeString(OUT.resolve("visual.PASS"),"Custom 3D equipment gallery rendered in actual client\n");done=true;mc.stop();
    }
    private static final class Gallery extends Screen {
        Gallery(){super(net.minecraft.network.chat.Component.literal("Wyposażenie uprawy"));}
        @Override public void render(net.minecraft.client.gui.GuiGraphics g,int mx,int my,float delta){g.fill(0,0,width,height,0xFF14252A);g.drawCenteredString(font,"WŁASNE PIKSELOWE MODELE 3D / WYPOSAŻENIE UPRAWY",width/2,18,0xFFE6E7C3);var items=List.of(CultivationContent.POT_ITEM.get(),CultivationContent.FRAME_ITEM.get(),CultivationContent.SOIL_SMALL.get(),CultivationContent.SOIL_MEDIUM.get(),CultivationContent.SOIL_LARGE.get(),CultivationContent.SEED_PACKET.get(),CultivationContent.SEEDLING.get(),CultivationContent.WATERING_CAN.get(),CultivationContent.UV_LAMP.get(),CultivationContent.SHEARS.get());for(int i=0;i<items.size();i++){int x=width/10+(i%5)*width/5,y=90+(i/5)*220;var stack=new ItemStack(items.get(i));g.pose().pushPose();g.pose().translate(x-40,y,200);g.pose().scale(5,5,5);g.renderItem(stack,0,0);g.pose().popPose();g.renderItem(stack,x-8,y+110);g.drawCenteredString(font,font.plainSubstrByWidth(stack.getHoverName().getString(),width/5-10),x,y+143,0xFFBDD1B2);}}
        @Override public void renderBackground(net.minecraft.client.gui.GuiGraphics g,int x,int y,float delta){}
    }
}
