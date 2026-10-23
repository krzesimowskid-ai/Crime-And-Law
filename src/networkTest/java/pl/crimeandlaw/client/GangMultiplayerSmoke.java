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
    private static String expectedLogo="",expectedBanner="";
    private static JsonObject lastState;
    private static String capturePending="";
    private static boolean finished;
    private static TerrainPanel mapPanel;
    private static int mapStep,mapOriginX,mapOriginZ,mapReplies,replyBefore,selectedMapX,selectedMapZ;
    private static boolean awaitingMap;
    private static double mapPointerX,mapPointerY;
    private static final double[][] MAP_PATH={{.4,0},{1.7,.5},{-1.6,1.7},{1.8,-1.7},{.3,.2},{0,0}};

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
                press(mc,"Wygląd i logo gangu");press(mc,"Zapisz rangę");next("create role via dialog");
            }
            case 6 -> {
                if(gang.roles.values().stream().noneMatch(r->r.name.equals("Stylista")))return;
                press(mc,"Ustawienia");press(mc,"Zarządzanie");press(mc,"Skład gangu");press(mc,"Member16");press(mc,"Zmień rangę");next("assign role via panel");
            }
            case 7 -> {
                var member=gang.members.values().stream().filter(m->m.name.equals("Member16")).findFirst().orElseThrow();
                var role=gang.roles.get(member.role);if(role==null||!role.name.equals("Stylista"))return;
                BufferedImage image=new BufferedImage(2048,2048,BufferedImage.TYPE_INT_ARGB);var random=new Random(1616);
                for(int y=0;y<2048;y++)for(int x=0;x<2048;x++)image.setRGB(x,y,random.nextInt()|0xFF000000);
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
            case 11 -> {
                if(gang.theme.background()!=0x253047)return;
                press(mc,"Baner");BufferedImage image=new BufferedImage(4096,951,BufferedImage.TYPE_INT_ARGB);var random=new Random(1717);
                for(int y=0;y<951;y++)for(int x=0;x<4096;x++)image.setRGB(x,y,random.nextInt()|0xFF000000);
                Path file=OUT.resolve("large-banner-fixture.png");ImageIO.write(image,"png",file.toFile());
                ((GangScreen)mc.screen).onFilesDrop(List.of(file));next("import wide banner");
            }
            case 12 -> {if(!(mc.screen instanceof LogoCropScreen))return;press(mc,"Zastosuj baner");next("apply wide crop");}
            case 13 -> {
                if(!(mc.screen instanceof GangScreen))return;
                var field=GangScreen.class.getDeclaredField("draftBanner");field.setAccessible(true);
                expectedBanner=((pl.crimeandlaw.gang.GangBanner)field.get(mc.screen)).image();require(expectedBanner.length()>1_000_000,"Detailed banner exercises a large simultaneous asset state");
                press(mc,"Zapisz baner gangu");next("save banner beside detailed logo over TCP");
            }
            case 14 -> {
                if(!gang.banner.image().equals(expectedBanner))return;
                Files.writeString(OUT.resolve("banner.sha256"),hash(gang.banner.image()));press(mc,"Gang");capture(mc,"owner-overview.png");next("wait for member final checks");
            }
            case 15 -> {
                if(!Files.exists(OUT.resolve("member.PASS")))return;
                require(gang.owner.equals(mc.player.getUUID())&&ClientState.memberCount()==2,"Unauthorized member operation did not change membership");
                pl.crimeandlaw.network.GangNetwork.terrainReceiver=json->{mapReplies++;TerrainClient.receive(json);};
                press(mc,"Mapa");next("wait for full prefetched real world map");
            }
            case 16 -> {
                if(!mappedBuffer())return;
                mapOriginX=TerrainClient.view.centerX();mapOriginZ=TerrainClient.view.centerZ();
                mapPanel=(TerrainPanel)widget(mc,"Mapa terenu");var l=GangLayout.of(mc.screen.width,mc.screen.height);
                var x=GangScreen.class.getDeclaredField("terrainX");x.setAccessible(true);selectedMapX=x.getInt(mc.screen);
                var z=GangScreen.class.getDeclaredField("terrainZ");z.setAccessible(true);selectedMapZ=z.getInt(mc.screen);
                mapPointerX=(mapPanel.getX()+98)*l.scale();mapPointerY=(mapPanel.getY()+98)*l.scale();
                require(mc.screen.mouseClicked(mapPointerX,mapPointerY,0),"Start actual held map drag");
                next("hold mouse while panning and receiving real server replies");
            }
            case 17 -> {
                require(widget(mc,"Mapa terenu")==mapPanel&&mapPanel.dragging(),"Real packets do not replace the focused map during a held drag");
                if(awaitingMap){
                    if(mapReplies<=replyBefore||!mappedBuffer())return;
                    checkMapCanvas(mc,mapPanel);capture(mc,"owner-map-held-"+mapStep+".png");mapStep++;awaitingMap=false;
                }
                if(mapStep==MAP_PATH.length){mc.screen.mouseReleased(mapPointerX,mapPointerY,0);next("release map after repeated reversals");return;}
                var l=GangLayout.of(mc.screen.width,mc.screen.height);double cell=TerrainPanel.GRID/11.0;
                double dx=MAP_PATH[mapStep][0]*cell,dy=MAP_PATH[mapStep][1]*cell;
                require(mc.screen.mouseDragged(mapPointerX+dx*l.scale(),mapPointerY+dy*l.scale(),0,dx*l.scale(),dy*l.scale()),"Real held camera motion handled");
                require(TerrainClient.view.centerX()==mapOriginX+Math.round(-MAP_PATH[mapStep][0])&&TerrainClient.view.centerZ()==mapOriginZ+Math.round(-MAP_PATH[mapStep][1]),"Held camera rebases before release");
                replyBefore=mapReplies;TerrainClient.request();awaitingMap=true;
            }
            case 18 -> {
                require(!((TerrainPanel)widget(mc,"Mapa terenu")).dragging(),"Release ends the held camera preview");
                require(TerrainClient.view.centerX()==mapOriginX&&TerrainClient.view.centerZ()==mapOriginZ,"Actual multi-direction drag returns to its original chunk");
                var x=GangScreen.class.getDeclaredField("terrainX");x.setAccessible(true);var z=GangScreen.class.getDeclaredField("terrainZ");z.setAccessible(true);
                require(x.getInt(mc.screen)==selectedMapX&&z.getInt(mc.screen)==selectedMapZ,"Dragging and real replies preserve the separate selection");
                checkMapCanvas(mc,(TerrainPanel)widget(mc,"Mapa terenu"));
                pass(mc,"Owner membership/permissions and identical detailed images passed; logo chars="+gang.logo.length()+", banner chars="+gang.banner.image().length()+". Actual loaded-world map: full prefetched buffer, six held direction changes, real TCP replies without losing focus, no clipping pixels, stable selection and release/return; replies="+mapReplies);
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
                require(ClientState.number("onlineCount",0)==2&&gang.members.get(mc.player.getUUID()).joinedAt>0,"Online totals and actual membership date received");
                press(mc,"Baner");require(!widget(mc,"Zapisz baner gangu").active,"Ordinary member cannot save banners");
                lastState=ClientState.state;JsonObject args=new JsonObject();args.addProperty("image","");args.addProperty("color",0xFF0000);args.addProperty("opacity",255);ClientState.action("banner",args);next("attempt unauthorized banner");
            }
            case 5 -> {
                if(ClientState.state==lastState)return;
                require(gang.banner.equals(pl.crimeandlaw.gang.GangBanner.DEFAULT),"Server rejected unauthorized banner changes");
                Files.writeString(OUT.resolve("member-initial.PASS"),"Ordinary member UI, private data, theme/banner authorization, online count and join date checked.\n");next("wait for delegated permission");
            }
            case 6 -> {
                if(!GangDirectory.allowed(gang,mc.player.getUUID(),Permission.MANAGE_THEME))return;
                require(!GangDirectory.allowed(gang,mc.player.getUUID(),Permission.MANAGE_ROLES)&&!ClientState.state.has("requestCount"),"Appearance permission grants no rank/application access");
                press(mc,"Ustawienia");press(mc,"Wygląd");require(widget(mc,"Zapisz dla gangu").active,"Delegated appearance permission updates the UI");next("wait for shared logo and theme");
            }
            case 7 -> {
                if(gang.logo.length()<=28_000||gang.banner.image().length()<=1_000_000||gang.theme.background()!=0x253047||!Files.exists(OUT.resolve("logo.sha256"))||!Files.exists(OUT.resolve("banner.sha256")))return;
                require(hash(gang.banner.image()).equals(Files.readString(OUT.resolve("banner.sha256"))),"Both clients received the same detailed wide banner");
                var bannerTexture=(net.minecraft.client.renderer.texture.DynamicTexture)mc.getTextureManager().getTexture(BannerTexture.get(gang.banner.image()));
                require(bannerTexture.getPixels().getWidth()==4096&&bannerTexture.getPixels().getHeight()==951,"Remote banner has its full independent resolution");
                require(hash(gang.logo).equals(Files.readString(OUT.resolve("logo.sha256"))),"Both clients received identical detailed logo data");
                var location=LogoTexture.get(gang.logo);var texture=(net.minecraft.client.renderer.texture.DynamicTexture)mc.getTextureManager().getTexture(location);
                require(texture.getPixels().getWidth()==2048&&texture.getPixels().getHeight()==2048,"Remote logo renders at saved resolution");
                press(mc,"Gang");capture(mc,"member-overview.png");
                JsonObject args=new JsonObject();args.addProperty("target",gang.owner.toString());ClientState.action("kick",args);next("attempt unauthorized membership change");
            }
            case 8 -> {
                if(ClientState.message.isBlank()||ClientState.message.equals("Zapisano."))return;
                require(ClientState.memberCount()==2&&gang.members.containsKey(gang.owner),"Server rejects unauthorized membership change");
                pass(mc,"Member joined via the panel, received delegated permission, identical detailed logo/banner and shared theme; rejected unauthorized theme/banner/kick; private applications remained hidden.");
            }
        }
    }
    private static boolean mappedBuffer(){return TerrainClient.view!=null&&TerrainClient.view.cells().size()==225&&TerrainClient.view.cells().stream().allMatch(c->!c.mapPixels().isEmpty());}
    private static void checkMapCanvas(Minecraft mc,TerrainPanel panel){
        double scale=GangLayout.of(mc.screen.width,mc.screen.height).scale()*mc.getWindow().getGuiScale();
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            for(int y=2;y<TerrainPanel.GRID-2;y+=4)for(int x=2;x<TerrainPanel.GRID-2;x+=4){
                if(x<19&&y<19||x<82&&y>TerrainPanel.GRID-21)continue;
                int pixel=image.getPixelRGBA((int)((panel.getX()+x+.5)*scale),(int)((panel.getY()+y+.5)*scale));
                require(pixel!=0xFF2B2629&&pixel!=0xFF292626&&pixel!=0xFF2D2A2A,"Loaded world covers the map at "+x+","+y);
            }
        }
    }
    private static EditBox nameField(Minecraft mc){return mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();}
    private static AbstractWidget widget(Minecraft mc,String text){
        var exact=mc.screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals(text)).map(c->(AbstractWidget)c).findFirst();
        return exact.orElseGet(()->mc.screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().startsWith(text)).map(c->(AbstractWidget)c).findFirst().orElseThrow(()->new AssertionError("Missing control: "+text)));
    }
    private static void press(Minecraft mc,String text){
        for(var permission:Permission.values())if(permission.label.equals(text)){
            var list=widget(mc,"Lista uprawnień");var layout=GangLayout.of(mc.screen.width,mc.screen.height);
            double x=(list.getX()+20)*layout.scale(),y=(list.getY()+permission.ordinal()*PermissionList.ROW+10)*layout.scale();
            require(mc.screen.mouseClicked(x,y,0),"Permission click: "+text);mc.screen.mouseReleased(x,y,0);return;
        }
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
