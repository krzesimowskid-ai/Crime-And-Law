package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import pl.crimeandlaw.gang.*;
import pl.crimeandlaw.gang.GangDirectory.Theme;

/** Regression checks against the actual client/configuration and modal lifecycle. */
@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class GangUiSmokeTestLifecycle {
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.reviewUiOutput","build/review-ui"));
    private static final UUID VIEWER=new UUID(40,1);
    private static final List<String> REPORT=new ArrayList<>();
    private static final List<String> FAILURES=new ArrayList<>();
    private static boolean done;
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(!Boolean.getBoolean("crimeandlaw.reviewUiSmoke")||done)return;
        var mc=Minecraft.getInstance();
        if(mc.getOverlay()==null&&mc.screen instanceof AccessibilityOnboardingScreen onboarding){mc.options.onboardingAccessibilityFinished();onboarding.onClose();return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
        done=true;
        run("Failed theme save leaves the effective preference unchanged",()->themeFailure(mc));
        run("New-rank draft is discarded after gang change",()->newRank(mc));
        run("Color draft survives same-gang refresh and closes on gang change",()->color(mc));
        run("Confirmation cannot act on a different gang",()->confirmation(mc));
        try{
            Files.createDirectories(OUT);
            Files.write(OUT.resolve("result.txt"),REPORT);
            if(!FAILURES.isEmpty())Files.write(OUT.resolve("FAIL.txt"),FAILURES);
        }catch(Exception e){e.printStackTrace();}
        mc.stop();
    }
    @FunctionalInterface private interface Check {void run()throws Exception;}
    private static void run(String title,Check check){
        try{check.run();REPORT.add("PASS "+title);}
        catch(Throwable e){e.printStackTrace();REPORT.add("FAIL "+title+": "+e);FAILURES.add(title+": "+e);}
    }
    private static void themeFailure(Minecraft mc)throws Exception{
        var field=PersonalTheme.class.getDeclaredField("FILE");field.setAccessible(true);Path file=(Path)field.get(null);
        require(file.toAbsolutePath().normalize().startsWith(mc.gameDirectory.toPath().toAbsolutePath().normalize()),"Only the disposable client's configuration may be touched");
        String key="review-040-theme";Theme original=PersonalTheme.get(key);
        byte[] backup=Files.exists(file)?Files.readAllBytes(file):null;
        Files.createDirectories(file.getParent());Files.deleteIfExists(file);
        Files.createDirectory(file);Path marker=file.resolve("owned-test-marker");Files.writeString(marker,"Force a real replacement failure, including under root.");
        try{
            require(!PersonalTheme.set(key,new Theme(0x123456,0x654321,191)),"Blocked destination must reject the save");
            require(Objects.equals(PersonalTheme.get(key),original),"Failed persistence changed the in-memory theme");
        }finally{
            Files.deleteIfExists(marker);Files.deleteIfExists(file);
            require(PersonalTheme.set(key,original),"Restore in-memory preference");
            if(backup==null)Files.deleteIfExists(file);else Files.write(file,backup);
            Files.deleteIfExists(file.resolveSibling(file.getFileName()+".tmp"));
        }
    }
    private static JsonObject state(String name){
        var directory=new GangDirectory();directory.create(VIEWER,"Tester",name,"");
        var state=GangView.snapshot(directory.of(VIEWER),VIEWER,0,0);state.addProperty("player",VIEWER.toString());state.addProperty("message","");return state;
    }
    private static GangScreen parent(Minecraft mc,String name){
        ClientState.receive(state(name).toString());var parent=new GangScreen();mc.setScreen(parent);parent.updated();return parent;
    }
    private static void newRank(Minecraft mc){
        var parent=parent(mc,"Pierwszy gang");AtomicInteger applied=new AtomicInteger();
        var dialog=new NewRoleScreen(parent,(name,permissions)->applied.incrementAndGet());mc.setScreen(dialog);
        ClientState.receive(state("Drugi gang").toString());
        require(mc.screen==parent,"Old rank editor remains open after the authoritative gang changes");
        require(applied.get()==0,"Stale rank draft was applied");
    }
    private static void color(Minecraft mc)throws Exception{
        var parent=parent(mc,"Kolory pierwsze");String same=ClientState.state.toString();AtomicInteger applied=new AtomicInteger();
        var dialog=new ColorPickerScreen(parent,"Kolor gangu",0xA3425D,255,false,(rgb,alpha)->applied.incrementAndGet());mc.setScreen(dialog);
        ClientState.receive(same);require(mc.screen==dialog,"Unrelated refresh discarded an active color draft");
        ClientState.receive(state("Kolory drugie").toString());
        require(mc.screen==parent,"Old color editor remains open after the authoritative gang changes");
        var registered=ColorPickerScreen.class.getDeclaredField("registered");registered.setAccessible(true);
        require(!registered.getBoolean(dialog)&&applied.get()==0,"Stale color draft or GPU texture was retained");
    }
    private static void confirmation(Minecraft mc)throws Exception{
        var parent=parent(mc,"Potwierdzenie pierwsze");AtomicInteger actions=new AtomicInteger();
        var method=GangScreen.class.getDeclaredMethod("confirm",String.class,String.class,Runnable.class);method.setAccessible(true);
        method.invoke(parent,"Opuścić gang?","Test",(Runnable)actions::incrementAndGet);
        require(mc.screen instanceof ConfirmScreen,"Native confirmation opened");
        ClientState.receive(state("Potwierdzenie drugie").toString());
        mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).findFirst().orElseThrow().onPress();
        require(actions.get()==0&&mc.screen==parent,"Old confirmation executed against the new current gang");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
