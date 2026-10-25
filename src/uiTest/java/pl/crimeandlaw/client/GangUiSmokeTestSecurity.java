package pl.crimeandlaw.client;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.glfw.GLFW;
import pl.crimeandlaw.gang.*;
import pl.crimeandlaw.gang.GangDirectory.Permission;

/** Actual renderer and pointer checks for delegated rights and correlated modal responses. */
@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class GangUiSmokeTestSecurity {
    private static final UUID OWNER=new UUID(39,1),MEMBER=new UUID(39,2);
    private static final int[][] CASES={{1920,1080,2,1},{1920,1080,4,0},{855,481,2,1},{640,480,2,0}};
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.securityUiOutput","build/security-ui"));
    private static final List<String> REPORT=new ArrayList<>();
    private static GangScreen parent;
    private static int round,step,frames,startup;
    private static boolean begun,done;
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(!Boolean.getBoolean("crimeandlaw.securityUiSmoke")||done)return;
        var mc=Minecraft.getInstance();
        try{
            if(!begun){
                if(++startup>2400)throw new AssertionError("Startup timeout");
                if(mc.getOverlay()==null&&mc.screen instanceof AccessibilityOnboardingScreen onboarding){mc.options.onboardingAccessibilityFinished();onboarding.onClose();return;}
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                Files.createDirectories(OUT);begun=true;configure(mc);return;
            }
            if(++frames<24){if(frames==9&&step==0){mc.options.guiScale().set(CASES[round][2]);mc.resizeDisplay();}return;}frames=0;
            boolean owner=CASES[round][3]==1;
            switch(step++){
                case 0->{
                    var c=CASES[round];require(mc.getWindow().getWidth()==c[0]&&mc.getWindow().getHeight()==c[1],"Actual framebuffer size");
                    require(mc.getWindow().getGuiScale()==Math.min(c[2],Math.min(c[0]/320,c[1]/240)),"Actual GUI scale");
                    var d=new GangDirectory();var gang=d.create(OWNER,"Lider","Kontrola menu","");d.request(MEMBER,"Kadrowy",gang.id);d.accept(OWNER,MEMBER);
                    d.role(OWNER,"member","Kadry",Set.of(Permission.MANAGE_ROLES,Permission.ASSIGN_ROLES,Permission.INVITE,Permission.MANAGE_BLACKLIST));
                    d.role(OWNER,"","Skarbnik",Set.of(Permission.WITHDRAW_FUNDS));
                    state(d,owner?OWNER:MEMBER);parent=new GangScreen();mc.setScreen(parent);parent.updated();click(mc,"Rangi");
                }
                case 1->{
                    search(mc,"Wypłata środków");toggle(mc);require(roleDraft().contains(Permission.WITHDRAW_FUNDS)==owner,"Unowned financial checkbox cannot be enabled");if(owner)toggle(mc);
                    search(mc,"Zapraszanie");toggle(mc);require(!roleDraft().contains(Permission.INVITE),"Owned right can be disabled");toggle(mc);require(roleDraft().contains(Permission.INVITE),"Owned right can be enabled");
                    click(mc,"≡  Skarbnik");search(mc,"Wypłata środków");
                    require(widget(mc,"Zapisz rangę").active==owner,"Protected-role save follows authority");
                    toggle(mc);require(roleDraft().contains(Permission.WITHDRAW_FUNDS)!=owner,"Protected-role checkbox cannot be changed by personnel");if(owner)toggle(mc);
                }
                case 2->{capture(mc,"protected-role");click(mc,"+ Nowa ranga");var name=mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();name.setValue("Nowa ranga");
                    search(mc,"Wypłata środków");toggle(mc);require(newDraft(mc).contains(Permission.WITHDRAW_FUNDS)==owner,"New-role picker respects grant ceiling");
                    search(mc,"Zapraszanie");toggle(mc);require(newDraft(mc).contains(Permission.INVITE),"New-role picker still allows owned rights");
                }
                case 3->{
                    capture(mc,"new-role");click(mc,"Anuluj");var dialog=new BlacklistAddScreen(parent);mc.setScreen(dialog);
                    var pending=BlacklistAddScreen.class.getDeclaredField("pending");pending.setAccessible(true);pending.setBoolean(dialog,true);
                    var id=BlacklistAddScreen.class.getDeclaredField("submittedId");id.setAccessible(true);String operation=UUID.randomUUID().toString();id.set(dialog,operation);
                    ClientState.message="Zapisano.";ClientState.state.remove("operationId");dialog.updated();require(mc.screen==dialog&&pending.getBoolean(dialog),"Unrelated success cannot close pending blacklist dialog");
                    ClientState.state.addProperty("operationId",UUID.randomUUID().toString());dialog.updated();require(mc.screen==dialog&&pending.getBoolean(dialog),"Another operation cannot close this dialog");
                    ClientState.state.addProperty("operationId",operation);dialog.updated();require(mc.screen==parent&&!pending.getBoolean(dialog),"Matching response closes completed dialog");
                    var old=new BlacklistAddScreen(parent);mc.setScreen(old);UUID viewer=owner?OWNER:MEMBER;var d=new GangDirectory();d.create(viewer,"Inny lider","Nowy gang","");state(d,viewer);old.updated();require(mc.screen==parent,"Changing gangs closes old blacklist form");
                    int[] c=CASES[round];REPORT.add("PASS "+c[0]+"x"+c[1]+" GUI "+mc.getWindow().getGuiScale()+" "+(owner?"owner":"personnel")+": physical permission toggles; protected-role save; new role; correlated responses; gang change");
                    if(++round==CASES.length){Files.write(OUT.resolve("result.txt"),REPORT);System.out.println("CRIME_SECURITY_UI_PASS");done=true;mc.stop();}else{step=0;configure(mc);}
                }
            }
        }catch(Throwable e){e.printStackTrace();done=true;try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("FAIL.txt"),e.toString());}catch(Exception ignored){}mc.stop();}
    }
    private static void configure(Minecraft mc){var c=CASES[round];GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),c[0],c[1]);frames=0;}
    private static void state(GangDirectory directory,UUID viewer){var value=GangView.snapshot(directory.of(viewer),viewer,0,0);value.addProperty("player",viewer.toString());ClientState.state=value;ClientState.message="";}
    private static AbstractWidget widget(Minecraft mc,String text){return mc.screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals(text)).map(c->(AbstractWidget)c).findFirst().orElseThrow(()->new AssertionError("Missing widget "+text));}
    private static void click(Minecraft mc,String text){var w=widget(mc,text);require(w.active&&w.visible,"Reachable "+text);pointer(mc,w.getX()+w.getWidth()/2.0,w.getY()+w.getHeight()/2.0);}
    private static void pointer(Minecraft mc,double x,double y){var layout=GangLayout.of(mc.screen.width,mc.screen.height);require(mc.screen.mouseClicked(x*layout.scale(),y*layout.scale(),0),"Physical pointer click");mc.screen.mouseReleased(x*layout.scale(),y*layout.scale(),0);}
    private static void search(Minecraft mc,String text){var fields=mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();require(fields.size()==2,"Role name and permission search");fields.get(1).setValue(text);}
    private static void toggle(Minecraft mc){var catalog=widget(mc,"Lista uprawnień");pointer(mc,catalog.getX()+catalog.getWidth()-15,catalog.getY()+11);}
    @SuppressWarnings("unchecked") private static Set<Permission> roleDraft()throws Exception{var f=GangScreen.class.getDeclaredField("rolePermissions");f.setAccessible(true);return (Set<Permission>)f.get(parent);}
    @SuppressWarnings("unchecked") private static Set<Permission> newDraft(Minecraft mc)throws Exception{var f=NewRoleScreen.class.getDeclaredField("permissions");f.setAccessible(true);return (Set<Permission>)f.get(mc.screen);}
    private static void capture(Minecraft mc,String name)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(round+"-"+name+".png"));}}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
