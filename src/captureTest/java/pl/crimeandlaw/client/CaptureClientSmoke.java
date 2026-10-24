package pl.crimeandlaw.client;

import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Real TCP clients exercise the actual map controls and capture payloads. */
@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class CaptureClientSmoke {
    private static final String ROLE=System.getProperty("crimeandlaw.captureRole","");
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.captureOutput","build/capture-smoke"));
    private static int frames,step=-1,wait;private static long started;private static boolean done;private static String pending="";
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(ROLE.isEmpty()||done)return;var mc=Minecraft.getInstance();
        try{
            if(started==0){started=System.nanoTime();Files.createDirectories(OUT);}
            if(System.nanoTime()-started>300_000_000_000L)throw new AssertionError("Client timeout stage "+step+" / "+ClientState.message);
            if(++frames%6!=0)return;
            if(!pending.isEmpty()){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(ROLE+"-"+pending+".png"));}pending="";}
            if(mc.screen instanceof AccessibilityOnboardingScreen onboarding){mc.options.onboardingAccessibilityFinished();onboarding.onClose();return;}
            if(mc.screen instanceof DisconnectedScreen)throw new AssertionError("Client disconnected at stage "+step);
            if(step<0){
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                mc.options.guiScale().set(2);mc.options.framerateLimit().set(60);mc.resizeDisplay();
                ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25596"),new ServerData("Capture integration","127.0.0.1:25596",ServerData.Type.OTHER),false,null);step=0;return;
            }
            if(mc.player==null||mc.level==null)return;
            if(step==0){
                if(ClientState.gang()==null){ClientState.refresh("",0);return;}
                mc.setScreen(new GangScreen());click(mc,"Mapa");step=1;return;
            }
            if(step==1){
                if(TerrainClient.view==null||!CaptureClient.matches(0,0))return;
                if(ROLE.equals("owner")){click(mc,"Zaznacz");click(mc,"Tylko dla mnie — wybierz kolor");pending="color-wheel";step=2;}
                else{Files.writeString(OUT.resolve("rival.ready"),"Ready at the actual capture map");step=5;}
                return;
            }
            if(step==2){
                check(mc.screen instanceof ColorPickerScreen,"Manual mark opens the real color wheel");click(mc,"Zastosuj");step=3;return;
            }
            if(step==3){
                if(CaptureClient.marks(0,0).isEmpty())return;
                check(CaptureClient.marks(0,0).getFirst().get("shared").getAsBoolean()==false,"Private marker roundtrips over TCP");
                if(!Files.exists(OUT.resolve("rival.ready")))return;
                pending="map-before-capture";click(mc,"Aktywuj kontrolę terytoriów");step=4;return;
            }
            if(step==4){
                if(!CaptureClient.flag("unlocked")||!CaptureClient.flag("recognized"))return;
                click(mc,"Rozpocznij przejęcie");check(mc.screen instanceof ConfirmScreen,"Starting capture requires reviewing the chosen chunk");click(mc,"Yes");step=5;return;
            }
            if(step==5){
                if(!CaptureClient.state.has("battle"))return;
                if(ROLE.equals("rival")){
                    if(!CaptureClient.flag("unlocked")){click(mc,"Aktywuj kontrolę terytoriów");return;}
                    check(CaptureClient.marks(0,0).isEmpty(),"Other gang never receives the owner's private marker");
                    check(CaptureClient.state.getAsJsonObject("battle").get("joinAllowed").getAsBoolean(),"Leader with a diagonal territory can counterattack");
                    click(mc,"Dołącz do captu");check(mc.screen instanceof ConfirmScreen,"Rival leader explicitly confirms the attack");click(mc,"Yes");
                }step=6;return;
            }
            if(step==6){
                var b=CaptureClient.ownBattle();if(b==null||b.getAsJsonArray("participants").size()<2)return;
                pending="map-contested";step=7;wait=0;return;
            }
            if(step==7){if(++wait<8)return;mc.setScreen(null);pending="capture-hud";step=8;return;}
            if(step==8){
                if(ROLE.equals("rival")&&mc.player.isDeadOrDying()){mc.player.respawn();mc.setScreen(null);step=9;return;}
                if(ROLE.equals("owner")&&CaptureClient.ownBattle()==null){mc.setScreen(new GangScreen());click(mc,"Mapa");TerrainClient.request();step=10;return;}
                return;
            }
            if(step==9){if(CaptureClient.ownBattle()!=null)return;pass(mc,"Real rival joined, died, respawned, remained eliminated and received end-of-capture cleanup.");return;}
            if(step==10){
                if(TerrainClient.view.cells().stream().noneMatch(cell->cell.x()==0&&cell.z()==0&&ClientState.gang().id.equals(cell.owner())))return;
                pending="map-winner";mc.player.connection.sendCommand("crimeandlaw lupy");step=11;return;
            }
            if(step==11){
                if(mc.player.getInventory().countItem(Items.DIAMOND)!=11)return;
                if(!pending.isEmpty())return;
                pass(mc,"Real owner: scoped marker, activation/start/confirm, PvP roster, HUD, ownership refresh, death loot and depot claim over TCP passed.");
            }
        }catch(Throwable e){e.printStackTrace();done=true;try{Files.writeString(OUT.resolve(ROLE+".FAIL"),e.toString()+" stage="+step+" message="+ClientState.message);}catch(Exception ignored){}mc.stop();}
    }
    private static void click(Minecraft mc,String text){
        var screen=mc.screen;var widget=screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals(text)).map(c->(AbstractWidget)c).findFirst().orElseThrow(()->new AssertionError("Missing button "+text+" on "+screen));
        check(widget.active&&widget.visible,"Button available: "+text);double scale=screen instanceof GangScreen?GangLayout.of(screen.width,screen.height).scale():screen instanceof MenuDialog dialog?dialog.layout.scale():1;
        double x=(widget.getX()+widget.getWidth()/2.0)*scale,y=(widget.getY()+widget.getHeight()/2.0)*scale;
        screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);
    }
    private static void check(boolean condition,String text){if(!condition)throw new AssertionError(text);}
    private static void pass(Minecraft mc,String text)throws Exception{Files.writeString(OUT.resolve(ROLE+".PASS"),text+"\n");done=true;mc.stop();}
}
