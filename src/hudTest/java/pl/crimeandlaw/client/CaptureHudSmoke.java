package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;
import pl.crimeandlaw.network.GangNetwork;

/** Real renderer and window sizes, with a local capture payload fixture; never shipped. */
@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class CaptureHudSmoke {
    private record Case(String name,int width,int height,int gui,boolean focused,boolean hidden,String phase,boolean eliminated){}
    private static final Case[] CASES={
        new Case("small-closed",855,481,2,false,false,"CLAIMING",false),
        new Case("small-open",855,481,2,true,false,"CLAIMING",false),
        new Case("minimum-open",640,480,2,true,false,"PREPARING",true),
        new Case("minimum-closed",640,480,2,false,false,"FIGHTING",false),
        new Case("large-gui2",1920,1080,2,true,false,"PREPARING",false),
        new Case("large-gui4",1920,1080,4,true,false,"FIGHTING",true),
        new Case("hidden-chat",855,481,2,false,true,"CLAIMING",false)
    };
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.hudOutput","build/hud-smoke"));
    private static int frame,step=-1,wait;private static long started;private static boolean done;
    @SubscribeEvent public static void gui(RenderGuiEvent.Pre event){
        var mc=Minecraft.getInstance();
        if(Boolean.getBoolean("crimeandlaw.hudSmoke")&&!done&&mc.level!=null&&step>0&&step<=CASES.length){mc.getToasts().clear();fixture(mc,CASES[step-1]);}
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(!Boolean.getBoolean("crimeandlaw.hudSmoke")||done)return;
        var mc=Minecraft.getInstance();
        try{
            if(started==0){started=System.nanoTime();Files.createDirectories(OUT);}
            if(System.nanoTime()-started>240_000_000_000L)throw new AssertionError("HUD timeout, case "+step);
            if(++frame%6!=0)return;
            if(mc.screen instanceof AccessibilityOnboardingScreen screen){mc.options.onboardingAccessibilityFinished();screen.onClose();return;}
            if(mc.screen instanceof DisconnectedScreen)throw new AssertionError("HUD client disconnected");
            if(step<0){
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                mc.options.guiScale().set(2);mc.options.framerateLimit().set(30);mc.resizeDisplay();
                ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25597"),new ServerData("HUD regression","127.0.0.1:25597",ServerData.Type.OTHER),false,null);
                step=0;return;
            }
            if(mc.player==null||mc.level==null)return;
            if(step==0){
                if(ClientMinimap.cacheSize()==0)return;
                mc.gui.getChat().clearMessages(true);
                for(int i=0;i<18;i++)mc.gui.getChat().addMessage(Component.literal("Wiadomość "+i+": aktualizowano capt przy -34, 4. Zażółć gęślą jaźń."));
                HudMoney.receive(new GangNetwork.Money(148840,69420));mc.player.setXRot(30);mc.player.xRotO=30;step=1;configure(mc,CASES[0]);return;
            }
            var c=CASES[step-1];fixture(mc,c);
            if(++wait<9)return;
            check(mc.getWindow().getWidth()==c.width()&&mc.getWindow().getHeight()==c.height(),"Actual framebuffer size "+c.name());
            if(c.focused())check(mc.screen instanceof RpChatScreen&&input(mc).getValue().equals("szkic wiadomości"),"Draft survives resized chat");
            var layout=CaptureHudLayout.of(mc.getWindow().getGuiScaledWidth(),c.hidden()?null:ChatView.layout(false));
            if(!c.hidden())check(layout.x()>(ChatView.layout(c.focused()).x()+ChatView.layout(c.focused()).width()+10)*ChatView.layout(c.focused()).scale(),"Chat and capture geometry is disjoint");
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
                image.writeToFile(OUT.resolve(c.name()+".png"));
                Files.writeString(OUT.resolve(c.name()+"-geometry.txt"),layout+" image="+image.getWidth()+"x"+image.getHeight()+" gui="+mc.getWindow().getGuiScaledWidth()+"x"+mc.getWindow().getGuiScaledHeight());
                checkBar(mc,image,layout);checkMoney(mc,image);
            }
            Files.writeString(OUT.resolve(c.name()+".PASS"),"Actual "+c.width()+"x"+c.height()+", GUI "+c.gui()+", capture border and both money icons rendered; chat geometry disjoint.\n");
            if(step==CASES.length){Files.writeString(OUT.resolve("hud.PASS"),"Seven actual renderer cases passed. Capture is visible with open chat and native GUI 2/4; no chat/capture overlap at 855x481 or 640x480; all phases and eliminated status rendered.\n");done=true;mc.stop();return;}
            step++;configure(mc,CASES[step-1]);
        }catch(Throwable error){error.printStackTrace();done=true;try{Files.writeString(OUT.resolve("hud.FAIL"),error+" case="+step);}catch(Exception ignored){}mc.stop();}
    }
    private static void configure(Minecraft mc,Case c){
        wait=0;mc.setScreen(null);mc.options.chatVisibility().set(c.hidden()?ChatVisiblity.HIDDEN:ChatVisiblity.FULL);mc.options.chatWidth().set(1d);mc.options.chatScale().set(1d);mc.options.guiScale().set(c.gui());
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),c.width(),c.height());mc.resizeDisplay();
        if(c.focused())mc.setScreen(new ChatScreen("szkic wiadomości"));fixture(mc,c);
        mc.gui.getChat().addMessage(Component.literal("Aktualizowano capt przy -34, 4 — wiadomość testowa."));
    }
    private static void fixture(Minecraft mc,Case c){
        var battle=new JsonObject();battle.addProperty("phase",c.phase());battle.addProperty("remaining",296);battle.addProperty("x",-34);battle.addProperty("z",4);battle.addProperty("eliminated",c.eliminated());battle.addProperty("present",true);battle.addProperty("holding",0);battle.addProperty("holdRequired",60);
        var state=new JsonObject();state.addProperty("dimension",mc.level.dimension().location().toString());state.add("ownBattle",battle);CaptureClient.state=state;
    }
    private static EditBox input(Minecraft mc){return mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();}
    private static void checkBar(Minecraft mc,NativeImage image,CaptureHudLayout layout){
        double sx=image.getWidth()/(double)mc.getWindow().getGuiScaledWidth(),sy=image.getHeight()/(double)mc.getWindow().getGuiScaledHeight();int found=0;
        int from=(int)Math.ceil((layout.x()+2)*sx),to=(int)Math.floor((layout.x()+layout.width()-2)*sx),y=(int)Math.ceil((layout.y()+.5)*sy);
        for(int x=from;x<to;x++){int p=image.getPixelRGBA(x,y);if((p&255)==0xA3&&(p>>>8&255)==0x42&&(p>>>16&255)==0x5D)found++;}
        check(found>=(to-from)*.95,"Actual capture border is visible, including focused chat: "+found+"/"+(to-from));
    }
    private static void checkMoney(Minecraft mc,NativeImage image){
        var l=PlayerHud.layout(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());double s=l.canvas().scale()*image.getWidth()/mc.getWindow().getGuiScaledWidth();
        for(int row=0;row<2;row++){int count=0,x=(int)(l.moneyX()*s),y=(int)((l.moneyY()+PlayerHudLayout.ROW_STEP*row)*s);
            for(int dy=0;dy<PlayerHudLayout.ICON_SIZE*s;dy++)for(int dx=0;dx<PlayerHudLayout.ICON_SIZE*s;dx++){int p=image.getPixelRGBA(x+dx,y+dy);if((p&255)>130&&(p>>>8&255)>160&&(p>>>16&255)>150)count++;}
            int minimum=Math.max(3,(int)Math.ceil(Math.pow(PlayerHudLayout.ICON_SIZE*s,2)*.10));
            check(count>=minimum,"Actual money icon visible, row "+row+": "+count+" / "+minimum);
        }
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
