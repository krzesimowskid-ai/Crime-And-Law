package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import pl.crimeandlaw.gang.GangDirectory;
import pl.crimeandlaw.gang.GangDirectory.Permission;

/** Two real clients, real TCP packets and UI actions; opt-in, excluded from the JAR. */
@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class GangMultiplayerSmoke {
    private static final String ROLE=System.getProperty("crimeandlaw.networkSmokeRole","");
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.networkSmokeOutput","../build/network-smoke"));
    private static final String NAME=System.getProperty("crimeandlaw.networkSmokeGang","Test interfejsu net16a");
    private static long started,poll;
    private static int frames,step=-1;
    private static String expectedLogo="";
    private static String capturePending="";
    private static boolean finished;

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(ROLE.isEmpty()||finished)return;
        Minecraft mc=Minecraft.getInstance();
        try{
            if(started==0){started=System.nanoTime();Files.createDirectories(OUT);}
            require(System.nanoTime()-started<240_000_000_000L,"Timed out at step "+step+" screen="+mc.screen);
            if(++frames%6!=0)return;
            if(!capturePending.isEmpty()){
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(capturePending));}
                capturePending="";
            }
            if(mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding){mc.options.onboardingAccessibilityFinished();onboarding.onClose();return;}
            if(mc.screen instanceof DisconnectedScreen)throw new AssertionError("Disconnected at step "+step);
            if(step<0){
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25597"),
                    new ServerData("Isolated Crime And Law test","127.0.0.1:25597",ServerData.Type.OTHER),false,null);
                next("connect");return;
            }
            if(mc.player==null||mc.level==null)return;
            if(step==0){mc.setScreen(new GangScreen());ClientState.refresh("",0);next("request authoritative state");return;}
            if(!ClientState.state.has("player"))return;
            if(ROLE.equals("owner"))owner(mc);else member(mc);
        }catch(Throwable error){
            error.printStackTrace();finished=true;
            try{Files.writeString(OUT.resolve(ROLE+".FAIL"),error.toString());}catch(Exception ignored){}
            mc.stop();
        }
    }
    private static void owner(Minecraft mc)throws Exception{
        var gang=ClientState.gang();
        switch(step){
            case 1 -> {
                require(gang==null,"Fresh owner has no gang");nameField(mc).setValue(NAME);press(mc,"Utwórz gang");next("create via panel");
            }
            case 2 -> {if(gang==null)return;require(gang.owner.equals(mc.player.getUUID()),"Server assigned the owner");next("wait for applicant");}
            case 3 -> {
                if(gang.requests.isEmpty())return;
                press(mc,"Ustawienia");press(mc,"Zarządzanie");press(mc,"Zgłoszenia: ");press(mc,"Member16");press(mc,"Przyjmij");next("accept via panel");
            }
            case 4 -> {if(ClientState.memberCount()!=2)return;next("wait for member permission checks");}
            case 5 -> {
                if(!Files.exists(OUT.resolve("member-initial.PASS")))return;
                press(mc,"Rangi");press(mc,"+ Nowa ranga");nameField(mc).setValue("Stylista");
                press(mc,"[ ] Wygląd i logo gangu");press(mc,"Zapisz rangę");next("create role via dialog");
            }
            case 6 -> {
                if(gang.roles.values().stream().noneMatch(r->r.name.equals("Stylista")))return;
                press(mc,"Ustawienia");press(mc,"Zarządzanie");press(mc,"Skład gangu");press(mc,"Member16");press(mc,"Zmień rangę");next("assign role via panel");
            }
            case 7 -> {
                var member=gang.members.values().stream().filter(m->m.name.equals("Member16")).findFirst().orElseThrow();
                var role=gang.roles.get(member.role);if(role==null||!role.name.equals("Stylista"))return;
                BufferedImage image=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);var random=new Random(1616);
                for(int y=0;y<512;y++)for(int x=0;x<512;x++)image.setRGB(x,y,random.nextInt()|0xFF000000);
                Path file=OUT.resolve("large-logo-fixture.png");ImageIO.write(image,"png",file.toFile());
                ((GangScreen)mc.screen).onFilesDrop(List.of(file));next("load detailed logo through drop handler");
            }
            case 8 -> {if(!(mc.screen instanceof LogoCropScreen))return;press(mc,"Zastosuj logo");next("apply crop");}
            case 9 -> {
                if(!(mc.screen instanceof GangScreen))return;
                var field=GangScreen.class.getDeclaredField("draftLogo");field.setAccessible(true);expectedLogo=(String)field.get(mc.screen);
                require(expectedLogo.length()>28_000,"Logo exceeds the old string cap");press(mc,"Zapisz logo gangu");next("save large logo over TCP");
            }
            case 10 -> {
                if(!gang.logo.equals(expectedLogo))return;
                Files.writeString(OUT.resolve("logo.sha256"),hash(gang.logo));
                press(mc,"Wygląd");press(mc,"Zmień kolor tła…");nameField(mc).setValue("253047");press(mc,"Zastosuj");press(mc,"Zapisz dla gangu");next("save shared theme via dialog");
            }
            case 11 -> {if(gang.theme.background()!=0x253047)return;press(mc,"Gang");capture(mc,"owner-overview.png");next("wait for member final checks");}
            case 12 -> {
                if(!Files.exists(OUT.resolve("member.PASS")))return;
                require(gang.owner.equals(mc.player.getUUID())&&ClientState.memberCount()==2,"Unauthorized member operation did not change membership");
                pass(mc,"Owner created a gang, accepted the other client, created/assigned a role, saved a detailed 512x512 logo and shared theme through UI actions; logo chars="+gang.logo.length());
            }
        }
    }
    private static void member(Minecraft mc)throws Exception{
        var gang=ClientState.gang();
        switch(step){
            case 1 -> {
                require(gang==null,"Fresh applicant has no gang");press(mc,"Znajdź gang");next("find gang");
            }
            case 2 -> {
                var rows=ClientState.state.getAsJsonArray("gangs");
                boolean found=false;for(var row:rows)if(row.getAsJsonObject().get("name").getAsString().equals(NAME))found=true;
                if(!found){if(System.nanoTime()-poll>2_000_000_000L){poll=System.nanoTime();ClientState.refresh(NAME,0);}return;}
                press(mc,"Zgłoś się");next("apply via panel");
            }
            case 3 -> {
                if(gang==null)return;
                require(ClientState.memberCount()==2&&!ClientState.state.has("requestCount"),"Ordinary member receives no private applications");
                press(mc,"Rangi");require(!widget(mc,"+ Nowa ranga").active,"Ordinary member cannot create ranks");
                press(mc,"Ustawienia");require(!widget(mc,"Zapisz dla gangu").active,"Ordinary member cannot save shared appearance");
                require(mc.screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Zarządzanie")),"Management is hidden");
                capture(mc,"member-before-permission.png");JsonObject args=new JsonObject();args.addProperty("background",0x00FF00);args.addProperty("border",0xFF0000);args.addProperty("opacity",255);
                ClientState.action("theme",args);next("attempt unauthorized shared theme");
            }
            case 4 -> {
                if(ClientState.message.isBlank()||ClientState.message.equals("Zapisano."))return;
                require(gang.theme.background()==GangDirectory.Theme.DEFAULT.background(),"Server rejects unauthorized appearance changes");
                Files.writeString(OUT.resolve("member-initial.PASS"),"Ordinary member UI, private data and server authorization checked.\n");next("wait for delegated permission");
            }
            case 5 -> {
                if(!GangDirectory.allowed(gang,mc.player.getUUID(),Permission.MANAGE_THEME))return;
                require(!GangDirectory.allowed(gang,mc.player.getUUID(),Permission.MANAGE_ROLES)&&!ClientState.state.has("requestCount"),"Appearance permission grants no rank/application access");
                press(mc,"Ustawienia");press(mc,"Wygląd");require(widget(mc,"Zapisz dla gangu").active,"Delegated appearance permission updates the UI");next("wait for shared logo and theme");
            }
            case 6 -> {
                if(gang.logo.length()<=28_000||gang.theme.background()!=0x253047||!Files.exists(OUT.resolve("logo.sha256")))return;
                require(hash(gang.logo).equals(Files.readString(OUT.resolve("logo.sha256"))),"Both clients received identical detailed logo data");
                var location=LogoTexture.get(gang.logo);var texture=(net.minecraft.client.renderer.texture.DynamicTexture)mc.getTextureManager().getTexture(location);
                require(texture.getPixels().getWidth()==512&&texture.getPixels().getHeight()==512,"Remote logo renders at saved resolution");
                press(mc,"Gang");capture(mc,"member-overview.png");
                JsonObject args=new JsonObject();args.addProperty("target",gang.owner.toString());ClientState.action("kick",args);next("attempt unauthorized membership change");
            }
            case 7 -> {
                if(ClientState.message.isBlank()||ClientState.message.equals("Zapisano."))return;
                require(ClientState.memberCount()==2&&gang.members.containsKey(gang.owner),"Server rejects unauthorized membership change");
                pass(mc,"Member joined via the panel, received delegated permission, identical detailed logo and shared theme; rejected unauthorized theme/kick; private applications remained hidden.");
            }
        }
    }
    private static EditBox nameField(Minecraft mc){return mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();}
    private static AbstractWidget widget(Minecraft mc,String text){
        var exact=mc.screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals(text)).map(c->(AbstractWidget)c).findFirst();
        return exact.orElseGet(()->mc.screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().startsWith(text)).map(c->(AbstractWidget)c).findFirst().orElseThrow(()->new AssertionError("Missing control: "+text)));
    }
    private static void press(Minecraft mc,String text){
        var w=widget(mc,text);require(w.active&&w.visible,"Control available: "+text);var l=GangLayout.of(mc.screen.width,mc.screen.height);
        double x=(w.getX()+w.getWidth()/2.0)*l.scale(),y=(w.getY()+w.getHeight()/2.0)*l.scale();
        require(mc.screen.mouseClicked(x,y,0),"Click handled: "+text);mc.screen.mouseReleased(x,y,0);
    }
    private static void capture(Minecraft mc,String filename){capturePending=filename;}
    private static String hash(String value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
    private static void next(String text)throws Exception{
        step++;Files.writeString(OUT.resolve(ROLE+"-steps.txt"),step+" "+text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        System.out.println("CRIME_NETWORK_"+ROLE.toUpperCase(Locale.ROOT)+"_STEP "+step+" "+text);
    }
    private static void pass(Minecraft mc,String text)throws Exception{
        Files.writeString(OUT.resolve(ROLE+".PASS"),text+"\n");finished=true;System.out.println("CRIME_NETWORK_"+ROLE.toUpperCase(Locale.ROOT)+"_PASS");mc.stop();
    }
    private static void require(boolean value,String text){if(!value)throw new AssertionError(text);}
}
