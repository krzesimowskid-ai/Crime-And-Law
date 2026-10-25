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
    private static final int[] FREE_ORDER={5,1,3,0,4,2};
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
                    if(mc.screen!=null||mc.getOverlay()!=null||mc.player.tickCount<40)return;
                    if(!Files.exists(OUT.resolve("server.ready"))||!mc.level.getBlockState(POS).is(CultivationContent.AURELIA.get()))return;
                    if(!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||!hit.getBlockPos().equals(POS))return;
                    capture(mc,"world-prompt");KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z));step=1;
                }
                case 1->{if(!(mc.screen instanceof HarvestScreen))return;capture(mc,"initial-3d");Files.writeString(OUT.resolve("owner-open.ready"),"ready");step=2;}
                case 2->{if(!Files.exists(OUT.resolve("rival.PASS")))return;var screen=(HarvestScreen)mc.screen;var s=state(screen);require(s.active(),"Owner reservation survived rival attempt");
                    if(!recovered&&cuts==0){
                        if(recoveryStarted==0){
                            recoveryStarted=System.nanoTime();set(screen,"awaiting",true);set(screen,"heldLeaf",5);set(screen,"awaitingSince",recoveryStarted);return;
                        }
                        if((boolean)field(screen,"awaiting"))return;
                        require(System.nanoTime()-recoveryStarted>=3_000_000_000L,"Recovery waits three real seconds");
                        require(mc.player.getInventory().countItem(CultivationContent.LEAF.get())==0&&mc.player.getMainHandItem().getDamageValue()==0,"Recovery never resends a cut or gives a reward");
                        Files.writeString(OUT.resolve("recovery.PASS"),"Real client waits three seconds for a simulated unprocessed cut, requests cancellation/state recovery and remains at zero rewards and zero wear.\n");recovered=true;
                    }
                    if(cuts==0){screen.mouseDragged(300,250,1,0,0);capture(mc,"ready-to-cut");}
                    require(s.target()==-1,"Idle server does not assign a leaf");
                    var p=marker(screen,FREE_ORDER[cuts]);float scale=number(screen,"scale");screen.mouseClicked(p.x*scale,p.y*scale,0);pressed=System.nanoTime();step=3;
                }
                case 3->{
                    if(System.nanoTime()-pressed<980_000_000L)return;
                    var screen=(HarvestScreen)mc.screen;var s=state(screen);require(s.target()==FREE_ORDER[cuts]&&s.holding(),"Server binds the leaf chosen by the player");var p=marker(screen,FREE_ORDER[cuts]);float scale=number(screen,"scale");
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
                    capture(mc,"completed");screen.onClose();Files.writeString(OUT.resolve("owner.PASS"),"Z prompt opens real 3D game; six freely ordered (5,1,3,0,4,2) physical hold/release snips over TCP, one leaf and one tool wear per cut, exhausted crop regrows.\n");done=true;
                }
            }
        }catch(Throwable e){e.printStackTrace();done=true;try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(ROLE+".FAIL"),e.toString()+" step="+step);}catch(Exception ignored){}mc.stop();}
    }
    private static void visual(Minecraft mc)throws Exception{
        if(step<0){if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;configure(mc);step=0;frames=0;return;}
        if(++frames<30)return;frames=0;
        if(step==3){capture(mc,"pixel-item-gallery");Files.writeString(OUT.resolve("visual.PASS"),"Four responsive 3D camera cases; six free leaf targets; exact 32px limited-palette sprites; nearest min/mag filters; solid stepped outlines; custom item gallery.\n");done=true;mc.stop();return;}
        if(step==0){
            mc.options.guiScale().set(CASES[round][2]);mc.resizeDisplay();mc.setScreen(new HarvestScreen(new CultivationNetwork.State(POS,UUID.randomUUID(),63,-1,false,true,"Wybierz dowolny liść. Kolejność zależy od Ciebie.")));step=1;return;
        }
        if(step==1){
            var s=(HarvestScreen)mc.screen;require(markers(s).size()==6,"All six projected leaf markers exist");
            var unique=new HashSet<String>();for(var p:markers(s).values())unique.add(Math.round(p.x)+":"+Math.round(p.y));require(unique.size()==6,"3D leaves have distinct cut points");
            for(var sprite:List.of(PixelSprite.LEAF,PixelSprite.SEEDS,PixelSprite.SHEARS)){
                var texture=mc.getTextureManager().getTexture(sprite.texture());texture.setFilter(true,true);texture.bind();
                require(org.lwjgl.opengl.GL11.glGetTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER)==org.lwjgl.opengl.GL11.GL_NEAREST,"Render layers preserve nearest pixel minification");
                require(org.lwjgl.opengl.GL11.glGetTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER)==org.lwjgl.opengl.GL11.GL_NEAREST,"Pixel magnification is nearest");
                var pixels=((net.minecraft.client.renderer.texture.DynamicTexture)texture).getPixels();require(pixels.getWidth()==32&&pixels.getHeight()==32,"Logical texture resolution is exactly 32px");
                Set<Integer> colors=new HashSet<>();for(int y=0;y<32;y++)for(int x=0;x<32;x++){int color=pixels.getPixelRGBA(x,y);if((color>>>24)!=0)colors.add(color);}
                require(colors.size()<=16&&colors.size()>5,"Own texture has a limited pixel palette");require(sprite.edgeCount()>20&&sprite.edgeCount()<300,"Merged silhouette has solid stepped side walls");
            }
            var pick=HarvestScreen.class.getDeclaredMethod("pick",double.class,double.class);pick.setAccessible(true);
            for(var entry:markers(s).entrySet())require((int)pick.invoke(s,(double)entry.getValue().x,(double)entry.getValue().y)==entry.getKey(),"Every leaf marker can be selected freely");
            capture(mc,"visual-"+round);s.mouseClicked(140,200,1);s.mouseDragged(220,200,1,80,0);s.mouseReleased(220,200,1);s.mouseScrolled(220,200,0,1);step=2;return;
        }
        capture(mc,"visual-rotated-"+round);
        if(++round<CASES.length){mc.setScreen(new TitleScreen());configure(mc);step=0;}else{GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1920,1080);mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new ItemGallery());step=3;}
    }
    private static final class ItemGallery extends Screen {
        ItemGallery(){super(net.minecraft.network.chat.Component.literal("Pixel item gallery"));}
        @Override public void render(net.minecraft.client.gui.GuiGraphics g,int x,int y,float delta){
            g.fill(0,0,width,height,0xFF18272A);g.drawCenteredString(font,"AURELIA / WLASNE TEKSTURY 32 PX / MODELE Z GRUBOSCIA",width/2,24,0xFFE5EAD2);
            var items=List.of(CultivationContent.SHEARS.get(),CultivationContent.SEEDS.get(),CultivationContent.LEAF.get());
            for(int i=0;i<3;i++){
                int cx=width/6+i*width/3;
                g.pose().pushPose();g.pose().translate(cx-72,90,100);g.pose().scale(9,9,9);g.renderItem(new net.minecraft.world.item.ItemStack(items.get(i)),0,0);g.pose().popPose();
                g.flush();com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
                g.pose().pushPose();g.pose().translate(cx,340,200);g.pose().scale(125,-125,125);g.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(65));g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-15));g.pose().translate(-.5,-.5,-.5);
                var buffers=Minecraft.getInstance().renderBuffers().bufferSource();new CultivationItemRenderer().renderByItem(new net.minecraft.world.item.ItemStack(items.get(i)),net.minecraft.world.item.ItemDisplayContext.GUI,g.pose(),buffers,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,0);buffers.endBatch();g.pose().popPose();
                com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                g.renderItem(new net.minecraft.world.item.ItemStack(items.get(i)),cx-8,455);
                g.drawCenteredString(font,items.get(i).getDescription().getString(),cx,490,0xFFB7D3BA);
            }
        }
        @Override public void renderBackground(net.minecraft.client.gui.GuiGraphics g,int x,int y,float delta){}
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
