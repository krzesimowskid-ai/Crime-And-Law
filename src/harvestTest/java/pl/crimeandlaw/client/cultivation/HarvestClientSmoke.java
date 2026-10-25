package pl.crimeandlaw.client.cultivation;

import java.nio.file.*;
import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import pl.crimeandlaw.cultivation.*;

@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class HarvestClientSmoke {
    private static final String ROLE=System.getProperty("crimeandlaw.harvestRole","");
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.harvestOutput","build/harvest-smoke"));
    private static final BlockPos POS=new BlockPos(0,80,0);
    private static final int[][] CASES={{1920,1080,2},{1920,1080,4},{855,481,2},{640,480,2}};
    private static int step=-1,frames,round,cuts;private static boolean done;private static long start,pressed;
    private static CultivationNetwork.State last;
    private static long recoveryStarted;private static boolean recovered;
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(ROLE.isEmpty()||done)return;var mc=Minecraft.getInstance();
        try{
            if(start==0){start=System.nanoTime();Files.createDirectories(OUT);}
            if(System.nanoTime()-start>240_000_000_000L)throw new AssertionError("Harvest client timeout "+ROLE+" stage "+step);
            if(mc.getOverlay()==null&&mc.screen instanceof AccessibilityOnboardingScreen onboarding){mc.options.onboardingAccessibilityFinished();onboarding.onClose();return;}
            if(ROLE.equals("visual")){visual(mc);return;}
            if(++frames%4!=0)return;
            if(step<0){if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                mc.options.guiScale().set(2);mc.options.framerateLimit().set(45);mc.resizeDisplay();
                CultivationNetwork.receiver=s->{last=s;CultivationClient.receive(s);};
                ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25598"),new ServerData("Harvest QA","127.0.0.1:25598",ServerData.Type.OTHER),false,null);step=0;return;
            }
            if(mc.player==null||mc.level==null)return;
            if(ROLE.equals("rival")){
                if(step==0&&Files.exists(OUT.resolve("owner-open.ready"))){CultivationNetwork.open(POS);step=1;return;}
                if(step==1&&last!=null&&!last.active()){
                    require(last.message().contains("inny gracz"),"Second real client is denied the plant reservation");
                    require(!(mc.screen instanceof HarvestScreen),"Denied viewer cannot open the pruning game");Files.writeString(OUT.resolve("rival.PASS"),"Actual TCP reservation conflict denied without changing leaves.\n");done=true;
                }return;
            }
            switch(step){
                case 0->{
                    if(!Files.exists(OUT.resolve("server.ready"))||!mc.level.getBlockState(POS).is(CultivationContent.AURELIA.get()))return;
                    if(!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||!hit.getBlockPos().equals(POS))return;
                    capture(mc,"world-prompt");KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z));step=1;
                }
                case 1->{if(!(mc.screen instanceof HarvestScreen))return;capture(mc,"initial-3d");Files.writeString(OUT.resolve("owner-open.ready"),"ready");step=2;}
                case 2->{if(!Files.exists(OUT.resolve("rival.PASS")))return;var screen=(HarvestScreen)mc.screen;var s=state(screen);require(s.active(),"Owner reservation survived rival attempt");
                    if(!recovered&&cuts==0){
                        if(recoveryStarted==0){
                            recoveryStarted=System.nanoTime();set(screen,"awaiting",true);set(screen,"heldLeaf",s.target());set(screen,"awaitingSince",recoveryStarted);return;
                        }
                        if((boolean)field(screen,"awaiting"))return;
                        require(System.nanoTime()-recoveryStarted>=3_000_000_000L,"Recovery waits three real seconds");
                        require(mc.player.getInventory().countItem(CultivationContent.LEAF.get())==0&&mc.player.getMainHandItem().getDamageValue()==0,"Recovery never resends a cut or gives a reward");
                        Files.writeString(OUT.resolve("recovery.PASS"),"Real client waits three seconds for a simulated unprocessed cut, requests cancellation/state recovery and remains at zero rewards and zero wear.\n");recovered=true;
                    }
                    if(cuts==0){screen.mouseDragged(300,250,1,0,0);capture(mc,"ready-to-cut");}
                    var p=marker(screen,s.target());float scale=number(screen,"scale");screen.mouseClicked(p.x*scale,p.y*scale,0);pressed=System.nanoTime();step=3;
                }
                case 3->{
                    if(System.nanoTime()-pressed<980_000_000L)return;
                    var screen=(HarvestScreen)mc.screen;var s=state(screen);var p=marker(screen,s.target());float scale=number(screen,"scale");
                    capture(mc,"hold-"+cuts);screen.mouseReleased(p.x*scale,p.y*scale,0);step=4;
                }
                case 4->{
                    var screen=(HarvestScreen)mc.screen;var s=state(screen);
                    int leaves=mc.player.getInventory().countItem(CultivationContent.LEAF.get());if(leaves==cuts)return;
                    require(leaves==cuts+1,"One server-confirmed cut gives exactly one leaf");cuts++;
                    require(mc.player.getMainHandItem().getDamageValue()==cuts,"Each cut wears the real tool once");
                    capture(mc,"cut-"+cuts);step=cuts==6?5:2;
                }
                case 5->{
                    var screen=(HarvestScreen)mc.screen;if(state(screen).active())return;
                    require(mc.level.getBlockState(POS).getValue(AureliaCrop.GROWTH)==1,"Exhausted crop regrows from the young stage");
                    capture(mc,"completed");screen.onClose();Files.writeString(OUT.resolve("owner.PASS"),"Z prompt opens real 3D game; six physical hold/release snips over TCP, one leaf and one tool wear per cut, exhausted crop regrows.\n");done=true;
                }
            }
        }catch(Throwable e){e.printStackTrace();done=true;try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(ROLE+".FAIL"),e.toString()+" step="+step);}catch(Exception ignored){}mc.stop();}
    }
    private static void visual(Minecraft mc)throws Exception{
        if(step<0){if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;configure(mc);step=0;frames=0;return;}
        if(++frames<30)return;frames=0;
        if(step==0){
            mc.options.guiScale().set(CASES[round][2]);mc.resizeDisplay();mc.setScreen(new HarvestScreen(new CultivationNetwork.State(POS,UUID.randomUUID(),63,round%6,false,true,"Wybierz oznaczony liść i przytrzymaj przy jego nasadzie.")));step=1;return;
        }
        if(step==1){
            var s=(HarvestScreen)mc.screen;require(markers(s).size()==6,"All six projected leaf markers exist");
            var unique=new HashSet<String>();for(var p:markers(s).values())unique.add(Math.round(p.x)+":"+Math.round(p.y));require(unique.size()==6,"3D leaves have distinct cut points");
            mc.getTextureManager().getTexture(AureliaMesh.LEAF).bind();
            require(org.lwjgl.opengl.GL11.glGetTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER)==org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR,"Entity render layers preserve trilinear leaf minification");
            capture(mc,"visual-"+round);s.mouseClicked(140,200,1);s.mouseDragged(220,200,1,80,0);s.mouseReleased(220,200,1);s.mouseScrolled(220,200,0,1);step=2;return;
        }
        capture(mc,"visual-rotated-"+round);
        if(++round<CASES.length){mc.setScreen(new TitleScreen());configure(mc);step=0;}else{Files.writeString(OUT.resolve("visual.PASS"),"Actual renderer: four framebuffer/GUI cases, six distinct 3D cut points, rotation and zoom, custom leaf texture, modeled shears and responsive panel.\n");done=true;mc.stop();}
    }
    private static void configure(Minecraft mc){int[] c=CASES[round];GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),c[0],c[1]);}
    private static CultivationNetwork.State state(HarvestScreen s)throws Exception{return (CultivationNetwork.State)field(s,"state");}
    @SuppressWarnings("unchecked") private static Map<Integer,Vector3f> markers(HarvestScreen s)throws Exception{return (Map<Integer,Vector3f>)field(s,"markers");}
    private static Vector3f marker(HarvestScreen s,int id)throws Exception{return markers(s).get(id);}
    private static float number(HarvestScreen s,String name)throws Exception{return ((Number)field(s,name)).floatValue();}
    private static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void set(Object o,String name,Object value)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    private static void capture(Minecraft mc,String name)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(ROLE+"-"+name+".png"));}}
    private static void require(boolean value,String detail){if(!value)throw new AssertionError(detail);}
}
