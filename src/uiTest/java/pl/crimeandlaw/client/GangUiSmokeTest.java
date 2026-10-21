package pl.crimeandlaw.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import pl.crimeandlaw.gang.GangDirectory;
import pl.crimeandlaw.gang.GangView;

/** Real Minecraft renderer, fixture data, no server. Enabled only by -PuiSmoke. */
@EventBusSubscriber(modid="crimeandlaw", value=Dist.CLIENT)
public final class GangUiSmokeTest {
    private static final UUID PLAYER=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Path OUT=Path.of("../build/ui-smoke");
    private static int frames,stage=-1;
    private static int waitFrames;
    private static int startupFrames;
    private static GangDirectory directory;
    private static final UUID FRIEND=new UUID(0,2);
    private static GangScreen screen;
    private static final StringBuilder REPORT=new StringBuilder();

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
        if(!Boolean.getBoolean("crimeandlaw.uiSmoke"))return;
        Minecraft mc=Minecraft.getInstance();
        try {
            if(stage<0) {
                if(++startupFrames>1800)throw new AssertionError("Client startup did not reach the title screen: "+mc.screen);
                if(mc.getOverlay()==null&&mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding){mc.options.onboardingAccessibilityFinished();onboarding.onClose();return;}
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                Files.createDirectories(OUT);
                mc.options.guiScale().set(2);mc.resizeDisplay();
                fixture(false);screen=new GangScreen();mc.setScreen(screen);
                stage=0;frames=0;return;
            }
            if(stage==12&&!(mc.screen instanceof LogoCropScreen)){
                if(++waitFrames>600)throw new AssertionError("Crop dialog did not open: "+ClientState.message);return;
            }
            if(++frames<24)return;
            frames=0;
            switch(stage++) {
                case 0 -> {
                    checkCreation(mc);capture(mc,"01-create-scale2");
                    mc.options.guiScale().set(4);mc.resizeDisplay();
                }
                case 1 -> {
                    checkCreation(mc);capture(mc,"02-create-scale4");
                    var l=GangLayout.of(screen.width,screen.height);
                    int left=(l.canvasWidth()-l.panelWidth())/2,top=(l.canvasHeight()-l.panelHeight())/2;
                    screen.mouseClicked((left+l.panelWidth()*0.75)*l.scale(),(top+20)*l.scale(),0);
                    ClientState.message="";
                    require(screen.children().stream().anyMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Szukaj")),"Find tab click");
                }
                case 2 -> {capture(mc,"03-find");fixture(true);screen.updated();}
                case 3 -> {
                    capture(mc,"04-gang");checkOverview(mc);checkCloseButton(mc);
                    require(widget(screen,"Rangi").visible,"Ranks navigation restored");
                    require(widget(screen,"Ustawienia").getY()<widget(screen,"Opuść gang").getY(),"Settings above leave");
                    require(widget(screen,"Ustawienia").getY()>widget(screen,"Mapa").getY()+100,"Settings in footer");
                    selectTab(1);click(mc,"Gracz2");
                }
                case 4 -> {
                    capture(mc,"05-members");
                    require(screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Usuń z gangu")),"Members tab is read only");
                    // Page two must show only its six rows, not the owner/viewer extras.
                    fixturePage(PLAYER,1);screen.updated();selectTab(1);
                    require(screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().startsWith("Gracz")).count()==6,"Member page contains six exact rows");
                }
                case 5 -> {capture(mc,"06-member-page2");selectTab(3);}
                case 6 -> {capture(mc,"07-settings");click(mc,"Zarządzanie");}
                case 7 -> {capture(mc,"08-applications");click(mc,"Skład gangu");click(mc,"Gracz7");}
                case 8 -> {
                    require(widget(screen,"Usuń z gangu").active,"Leader member actions available");
                    capture(mc,"09-management");click(mc,"Wygląd");click(mc,"Zmień kolor tła…");
                }
                case 9 -> {
                    require(mc.screen instanceof ColorPickerScreen,"Color dialog opened");
                    ColorPickerScreen dialog=(ColorPickerScreen)mc.screen;
                    EditBox hex=(EditBox)dialog.children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow();
                    hex.setValue("FF0000");
                }
                case 10 -> {
                    capture(mc,"10-color-gradients");checkGradients(mc);checkCloseButton(mc);
                    ColorPickerScreen dialog=(ColorPickerScreen)mc.screen;double scale=dialog.layout.scale();
                    AbstractWidget slider=widget(mc.screen,"Krycie:");
                    dialog.mouseClicked((slider.getX()+38)*scale,(slider.getY()+86)*scale,0);dialog.mouseReleased(0,0,0);
                    // Vertical direction and keyboard adjustment are independently exercised.
                    AbstractWidget bright=widget(mc.screen,"Jasność:");
                    dialog.mouseClicked((bright.getX()+38)*scale,(bright.getY()+151)*scale,0);dialog.mouseReleased(0,0,0);
                    require(bright.getMessage().getString().endsWith("0%"),"Brightness bottom is zero");
                    dialog.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_HOME,0,0);
                    require(bright.getMessage().getString().endsWith("100%"),"Brightness Home is full");
                    dialog.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN,0,0);
                    require(bright.getMessage().getString().endsWith("99%"),"Brightness keyboard adjustment");
                    dialog.mouseClicked((bright.getX()+38)*scale,(bright.getY()+80)*scale,0);
                    dialog.mouseDragged((bright.getX()+38)*scale,(bright.getY()+21)*scale,0,0,-59*scale);dialog.mouseReleased(0,0,0);
                    require(bright.getMessage().getString().endsWith("100%"),"Brightness drag reaches top");
                    click(mc,"Zastosuj");
                    require(screen.currentTheme().opacity()>110&&screen.currentTheme().opacity()<145,"Vertical opacity changed preview");
                }
                case 11 -> {
                    capture(mc,"11-appearance-preview");checkCleanHints();click(mc,"Logo");
                    var cases=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("crimeandlaw.logoFixtures")))).getAsJsonObject();
                    for(var entry:cases.entrySet()){
                        Path raster=OUT.resolve("fixture-"+entry.getKey()+".png").toAbsolutePath();
                        Files.write(raster,java.util.Base64.getDecoder().decode(entry.getValue().getAsString()));
                        var decoded=java.util.concurrent.CompletableFuture.supplyAsync(()->{
                            try{return LogoImages.read(raster);}catch(java.io.IOException e){throw new RuntimeException(e);}
                        }).join();
                        require(decoded.getWidth()==16,"Bundled reader in mod class loader: "+entry.getKey());
                        require(!LogoImages.encode(decoded,new LogoCrop(decoded.getWidth(),decoded.getHeight())).isBlank(),"Normalize "+entry.getKey());
                    }
                    Path svg=OUT.resolve("logo-vector-fixture.png").toAbsolutePath();
                    Files.writeString(svg,"<svg xmlns='http://www.w3.org/2000/svg' width='512' height='256'><defs><linearGradient id='g'><stop stop-color='#b82355'/><stop offset='1' stop-color='#568ac8'/></linearGradient></defs><rect width='512' height='256' fill='url(#g)'/></svg>");
                    screen.onFilesDrop(java.util.List.of(svg));
                }
                case 12 -> {
                    checkLogoTexture(mc,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("crimeandlaw","logo_crop_preview"),512,256);
                    capture(mc,"12-logo-crop");LogoCropScreen dialog=(LogoCropScreen)mc.screen;double scale=dialog.layout.scale();
                    double x=(dialog.left+110)*scale,y=(dialog.top+145)*scale;
                    require(dialog.mouseClicked(x,y,0),"Crop click");dialog.mouseDragged(x+30*scale,y+10*scale,0,30*scale,10*scale);
                    dialog.mouseReleased(x+30*scale,y+10*scale,0);dialog.mouseScrolled(x,y,0,2);click(mc,"Zastosuj logo");
                    var field=GangScreen.class.getDeclaredField("draftLogo");field.setAccessible(true);String encoded=(String)field.get(screen);
                    require(!encoded.isBlank()&&!pl.crimeandlaw.gang.GangLogo.validate(encoded).isBlank(),"Dropped image cropped and encoded");
                    var legacy=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    String old=java.util.Base64.getEncoder().encodeToString(LogoImages.png(legacy));
                    checkLogoTexture(mc,LogoTexture.get(old),64,64);
                    // Detailed PNGs also exercise stream decoding beyond the old
                    // LWJGL stack allocation and the former network string limits.
                    var noise=new java.awt.image.BufferedImage(512,512,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    var random=new java.util.Random(21);
                    for(int row=0;row<512;row++)for(int col=0;col<512;col++)noise.setRGB(col,row,random.nextInt()|0xFF000000);
                    String large=LogoImages.encode(noise,new LogoCrop(512,512));
                    require(large.length()>28000,"Detailed logo exceeds previous network cap");
                    checkLogoTexture(mc,LogoTexture.get(large),512,512);
                    checkLogoTexture(mc,LogoTexture.get(encoded),512,512);
                    directory.logo(PLAYER,encoded);
                    REPORT.append("PASS logo: 512x512 saved texture, legacy 64x64, detailed PNG stream decoding, linear min/mag filters, smooth crop preview.\n");
                }
                case 13 -> {
                    capture(mc,"13-logo-preview");fixturePage(FRIEND,0);screen.updated();selectTab(3);click(mc,"Wygląd");
                    require(screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Zarządzanie")),"Management hidden from ordinary member");
                    require(!widget(screen,"Zapisz dla gangu").active,"Member cannot save shared appearance");
                    require(!ClientState.state.has("requestCount"),"Private requests not sent to member");
                }
                case 14 -> {
                    capture(mc,"14-member-settings");
                    directory.of(PLAYER).name="W".repeat(32);fixturePage(PLAYER,0);screen.updated();selectTab(0);
                    mc.options.guiScale().set(2);mc.resizeDisplay();
                }
                case 15 -> {
                    capture(mc,"15-overview-logo-long-name-scale2");checkOverview(mc);checkCloseButton(mc);
                    directory.of(PLAYER).name="Czarne Wilki";fixturePage(PLAYER,0);
                    screen=new GangScreen();mc.setScreen(screen);screen.updated();selectTab(4);
                }
                case 16 -> {
                    capture(mc,"16-register");
                    var dates=GangScreen.class.getDeclaredField("historyRows");dates.setAccessible(true);
                    require(dates.get(screen).toString().contains("21 października 2026 o godzinie"),"Register uses a full Polish date, year and time");
                    require(ClientState.state.getAsJsonArray("history").size()==100,"Register receives a bounded page of real events");
                    require(widget(screen,"›").active,"History next page is available");
                    require(screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Transport")),"Transport has no menu entry");
                    var l=GangLayout.of(screen.width,screen.height);ScrollPosition position=historyPosition();
                    require(position.maximum()>0,"History page has scrollable content");
                    int footerY=widget(screen,"›").getY();
                    require(footerY<widget(screen,"Przewijanie rejestru").getY(),"Pagination is in the header");
                    require(widget(screen,"›").getX()-widget(screen,"‹").getX()==26,"Arrow buttons are adjacent");
                    screen.mouseScrolled(400*l.scale(),180*l.scale(),0,-10000);
                    require(position.offset()==position.maximum(),"Wheel reaches the last event");
                    require(widget(screen,"›").getY()==footerY&&widget(screen,"›").visible,"Pagination stays fixed while scrolling");
                    var body=GangScreen.class.getDeclaredField("bodyScroll");body.setAccessible(true);require(body.getInt(screen)==0,"Header is outside the scroll offset");
                }
                case 17 -> {
                    capture(mc,"17-register-scrolled");
                    var l=GangLayout.of(screen.width,screen.height);var bar=widget(screen,"Przewijanie rejestru");var position=historyPosition();
                    double x=(bar.getX()+5)*l.scale(),from=(bar.getY()+position.thumbTop()+position.thumbHeight()/2.0)*l.scale();
                    double to=(bar.getY()+position.thumbHeight()/2.0)*l.scale();
                    require(screen.mouseClicked(x,from,0),"Scrollbar click");screen.mouseDragged(x,to,0,0,to-from);screen.mouseReleased(x,to,0);
                    require(position.offset()==0,"Scrollbar drag reaches start");
                    screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_END,0,0);require(position.offset()==position.maximum(),"Scrollbar End key");
                    screen.mouseScrolled(x,110*l.scale(),0,1);require(position.offset()==position.maximum(),"Wheel on header does not move entries");
                    click(mc,"›");require(position.offset()==0,"Changing page resets scroll immediately");
                    fixturePage(PLAYER,0,1);screen.updated();
                    require(ClientState.number("historyPage",-1)==1,"Second history page loaded");
                    require(historyPosition().offset()==0&&widget(screen,"Przewijanie rejestru").active,"Second page with remaining events scrolls correctly");
                }
                case 18 -> {
                    capture(mc,"18-register-second-page");fixturePage(PLAYER,0,0);screen.updated();
                    var l=GangLayout.of(screen.width,screen.height);screen.mouseScrolled(400*l.scale(),180*l.scale(),0,-1);
                    require(historyPosition().offset()==24,"Single wheel step");
                    mc.options.guiScale().set(4);mc.resizeDisplay();require(historyPosition().offset()==24,"GUI resize preserves scroll");
                }
                case 19 -> {
                    capture(mc,"19-register-scale4");selectTab(5);
                    var field=GangScreen.class.getDeclaredField("tab");field.setAccessible(true);
                    require(field.getInt(screen)==0,"Disabled transport route falls back to overview");
                }
                case 20 -> {
                    capture(mc,"20-hidden-transport-fallback");
                    for(String name:new String[]{"Premie","Saldo gangu","Czarna lista","Cele"}){
                        var entry=widget(screen,name);require(entry.visible&&entry.active,"Visible category "+name);click(mc,name);
                    }
                    selectTab(pl.crimeandlaw.gang.GangCategory.ROLES.tabId());
                }
                case 21 -> {
                    capture(mc,"21-ranks");checkCleanHints();require(widget(screen,"+ Nowa ranga").active,"Owner can create a rank");
                    click(mc,"+ Nowa ranga");
                    require(mc.screen instanceof NewRoleScreen,"New rank opens a separate dialog");
                    require(!widget(mc.screen,"Zapisz rangę").active,"Blank rank cannot be saved");
                    var edit=mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();
                    edit.setValue("Rekruter");click(mc,"[ ] Zapraszanie / zgłoszenia");click(mc,"[ ] Wygląd i logo gangu");
                    require(widget(mc.screen,"Zapisz rangę").active,"Rank form can be saved");
                }
                case 22 -> {
                    capture(mc,"22-create-rank");checkCloseButton(mc);
                    var name=mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->((EditBox)c).getValue()).findFirst().orElseThrow();
                    var rights=NewRoleScreen.class.getDeclaredField("permissions");rights.setAccessible(true);
                    @SuppressWarnings("unchecked") var selected=java.util.Set.copyOf((java.util.Set<GangDirectory.Permission>)rights.get(mc.screen));
                    require(name.equals("Rekruter")&&selected.equals(java.util.Set.of(GangDirectory.Permission.INVITE,GangDirectory.Permission.MANAGE_THEME)),"Rank dialog retains typed name and selected permissions");
                    click(mc,"Zapisz rangę");
                    require(mc.screen==screen,"Saving the rank returns to the ranks list");
                    // Fixture acknowledgements use the same domain operations as
                    // the server; this graphical run has no network connection.
                    directory.role(PLAYER,"",name,selected);
                    fixturePage(PLAYER,0);screen.updated();selectTab(pl.crimeandlaw.gang.GangCategory.ROLES.tabId());
                    click(mc,"≡  Rekruter");
                }
                case 23 -> {
                    capture(mc,"23-custom-rank");
                    var row=widget(screen,"≡  Rekruter");var leader=widget(screen,"≡  Lider");var l=GangLayout.of(screen.width,screen.height);
                    double x=(row.getX()+20)*l.scale(),from=(row.getY()+8)*l.scale(),to=(leader.getY()+1)*l.scale();
                    require(screen.mouseClicked(x,from,0),"Rank drag starts");screen.mouseDragged(x,to,0,0,to-from);
                    var moved=GangScreen.class.getDeclaredField("roleMoved");moved.setAccessible(true);require(moved.getBoolean(screen),"Rank drag threshold exceeded");
                    screen.mouseReleased(x,to,0);
                    var g=directory.of(PLAYER);String rank=g.roles.values().stream().filter(r->r.name.equals("Rekruter")).findFirst().orElseThrow().id;
                    directory.reorderRoles(PLAYER,java.util.List.of(rank,"leader","member"));
                    fixturePage(PLAYER,0);screen.updated();selectTab(pl.crimeandlaw.gang.GangCategory.ROLES.tabId());
                }
                case 24 -> {
                    capture(mc,"24-rank-order");require(widget(screen,"≡  Rekruter").getY()<widget(screen,"≡  Lider").getY(),"Custom rank is displayed above leader");
                    fixturePage(FRIEND,0);screen.updated();selectTab(pl.crimeandlaw.gang.GangCategory.ROLES.tabId());
                    require(!widget(screen,"+ Nowa ranga").active&&!widget(screen,"Zapisz rangę").active,"Ordinary member cannot edit ranks");
                    String rank=directory.of(PLAYER).roles.values().stream().filter(r->r.name.equals("Rekruter")).findFirst().orElseThrow().id;
                    directory.assign(PLAYER,FRIEND,rank);fixturePage(FRIEND,0);screen.updated();selectTab(3);click(mc,"Wygląd");
                    require(widget(screen,"Zapisz dla gangu").active,"Delegated appearance permission enables shared appearance");
                    require(ClientState.state.has("requestCount"),"Recruiter receives applications");
                    click(mc,"Zarządzanie");click(mc,"Zgłoszenia:");click(mc,"Kandydat14");
                    require(widget(screen,"Przyjmij").active,"Recruiter can accept applications");
                }
                case 25 -> {
                    capture(mc,"25-delegated-management");click(mc,"Logo");checkCleanLogoForm();
                    require(screen.children().stream().noneMatch(c->c instanceof EditBox),"Logo settings have no path input");
                    require(widget(screen,"Wybierz obraz…").active,"Delegated logo picker remains available");
                }
                case 26 -> {
                    capture(mc,"26-clean-logo");
                    fixturePage(PLAYER,0);screen.updated();selectTab(pl.crimeandlaw.gang.GangCategory.ROLES.tabId());click(mc,"+ Nowa ranga");
                    var edit=mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();
                    edit.setValue("Bez zapisu");click(mc,"[ ] Usuwanie członków");
                    mc.options.guiScale().set(2);mc.resizeDisplay();
                }
                case 27 -> {
                    capture(mc,"27-rank-dialog-scale2");checkCloseButton(mc);
                    var edit=mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();
                    require(edit.getValue().equals("Bez zapisu")&&widget(mc.screen,"[x] Usuwanie członków").active,"Rank draft survives a GUI resize");
                    int count=directory.of(PLAYER).roles.size();
                    mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0);
                    require(mc.screen==screen&&directory.of(PLAYER).roles.size()==count,"Escape cancels the rank without changes");
                    click(mc,"+ Nowa ranga");click(mc,"Zamknij");
                    require(mc.screen==screen&&directory.of(PLAYER).roles.size()==count,"Close icon cancels the rank without changes");
                    click(mc,"+ Nowa ranga");click(mc,"Anuluj");
                    require(mc.screen==screen&&directory.of(PLAYER).roles.size()==count,"Cancel returns without creating a rank");
                    terrainFixture();selectTab(2);
                }
                case 28 -> {
                    capture(mc,"28-terrain-map-scale2");
                    var panel=widget(screen,"Mapa terenu");var l=GangLayout.of(screen.width,screen.height);
                    require(panel.visible,"Terrain panel is visible without body scrolling");
                    screen.mouseClicked((panel.getX()+4*28+14)*l.scale(),(panel.getY()+2*28+14)*l.scale(),0);
                    var sx=GangScreen.class.getDeclaredField("terrainX");sx.setAccessible(true);var sz=GangScreen.class.getDeclaredField("terrainZ");sz.setAccessible(true);
                    require(sx.getInt(screen)==1&&sz.getInt(screen)==-1,"Scaled click selects the correct chunk");
                    screen.mouseScrolled((panel.getX()+250)*l.scale(),(panel.getY()+80)*l.scale(),0,-100);
                    var scroll=GangScreen.class.getDeclaredField("terrainScroll");scroll.setAccessible(true);
                    require(((ScrollPosition)scroll.get(screen)).offset()>0,"Long terrain details scroll independently");
                    mc.options.guiScale().set(4);mc.resizeDisplay();
                }
                case 29 -> {
                    capture(mc,"29-terrain-map-scale4");
                    var sx=GangScreen.class.getDeclaredField("terrainX");sx.setAccessible(true);require(sx.getInt(screen)==1,"GUI scale preserves selected chunk");
                    var panel=widget(screen,"Mapa terenu");var l=GangLayout.of(screen.width,screen.height);
                    screen.mouseClicked((panel.getX()+14)*l.scale(),(panel.getY()+14)*l.scale(),0);
                }
                case 30 -> {
                    capture(mc,"30-terrain-unavailable");
                    var sx=GangScreen.class.getDeclaredField("terrainX");sx.setAccessible(true);require(sx.getInt(screen)==-3,"Unavailable cell remains inspectable");
                    REPORT.append("PASS: actual Minecraft renderer, 30 captures, real terrain-summary grid with fixture data, scaled chunk selection, scrolling terrain inspector, map selection across GUI resize and unavailable cells; new-rank dialog, save and cancellation, role dragging, register, full Polish dates, colors and logo import.\n");
                    Files.writeString(OUT.resolve("result.txt"),REPORT.toString());System.out.println("CRIME_UI_SMOKE_PASS");mc.stop();
                }
            }
        } catch(Throwable error) {
            error.printStackTrace();
            try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"FAIL: "+error);}catch(Exception ignored){}
            stage=1000;mc.stop();
        }
    }
    private static void capture(Minecraft mc,String name)throws Exception {
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            require((image.getPixelRGBA(1600,850)&0xFFFFFF)!=0,"Panel background after scale/tab change: "+name);
            if(mc.screen instanceof GangScreen&&ClientState.gang()!=null){
                require((image.getPixelRGBA(600,200)&0xFFFFFF)!=0,"Header background after tab change: "+name);
                require((image.getPixelRGBA(250,500)&0xFFFFFF)!=0,"Sidebar background after tab change: "+name);
                require((image.getPixelRGBA(900,150)&0xFFFFFF)!=0,"Gang name frame after tab change: "+name);
            }
            if(mc.screen instanceof MenuDialog dialog){
                AbstractWidget cancel=widget(mc.screen,"Anuluj");double scale=dialog.layout.scale()*mc.getWindow().getGuiScale();int bright=0;
                for(int y=(int)((cancel.getY()+3)*scale);y<(cancel.getY()+cancel.getHeight()-3)*scale;y++)
                    for(int x=(int)((cancel.getX()+4)*scale);x<(cancel.getX()+cancel.getWidth()-4)*scale;x++){
                        int pixel=image.getPixelRGBA(x,y);if((pixel&255)>170&&(pixel>>8&255)>170&&(pixel>>16&255)>170)bright++;
                    }
                require(bright>20,"Cancel label rendered: "+name);
            }
            image.writeToFile(OUT.resolve(name+".png"));
        }
        REPORT.append(name).append(" — ").append(mc.getWindow().getWidth()).append('x').append(mc.getWindow().getHeight())
                .append(", GUI ").append(mc.getWindow().getGuiScale()).append('\n');
    }
    private static void checkCreation(Minecraft mc)throws ReflectiveOperationException {
        var l=GangLayout.of(screen.width,screen.height);
        var boxes=screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).toList();
        require(boxes.size()==1,"Only the name field remains; no logo path field");checkCleanLogoForm();EditBox name=boxes.getFirst();
        require(screen.mouseClicked((name.getX()+10)*l.scale(),(name.getY()+8)*l.scale(),0),"Scaled text field click");
        name.setValue("");screen.charTyped('T',0);screen.charTyped('e',0);screen.charTyped('s',0);screen.charTyped('t',0);
        require(name.getValue().equals("Test"),"Typing after scaled click");
        var create=screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Utwórz gang"))
                .map(c->(AbstractWidget)c).findFirst().orElseThrow();
        require(create.visible,"Create action visible without scrolling");
        var tabs=screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getY()==(l.canvasHeight()-l.panelHeight())/2+1)
                .map(c->(AbstractWidget)c).toList();
        require(tabs.size()==2&&tabs.get(0).getWidth()==tabs.get(1).getWidth()&&tabs.get(0).getY()==tabs.get(1).getY(),"Two equal horizontal tabs");
        REPORT.append("PASS creation, scale ").append(mc.getWindow().getGuiScale()).append('\n');
    }
    private static void checkOverview(Minecraft mc){
        var layout=GangLayout.of(screen.width,screen.height);double scale=layout.scale()*mc.getWindow().getGuiScale();
        int left=(layout.canvasWidth()-layout.panelWidth())/2,top=(layout.canvasHeight()-layout.panelHeight())/2;
        int cy=top+55,cx=left+116,cw=layout.panelWidth()-130,accent=screen.currentTheme().border();
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            for(int x:new int[]{left+110,left+layout.panelWidth()/2,left+layout.panelWidth()-3}){
                int rgba=image.getPixelRGBA((int)((x+.5)*scale),(int)((cy+110.5)*scale));
                int rgb=(rgba&255)<<16|(rgba&0xFF00)|(rgba>>16&255);
                require(rgb==accent,"Divider spans the full content pane");
            }
            // Each third must contain bright text below the section heading.
            for(int column=0;column<3;column++){
                int bright=0;
                for(int y=(int)((cy+158)*scale);y<(cy+210)*scale;y++)for(int x=(int)((cx+cw*column/3+8)*scale);x<(cx+cw*(column+1)/3-8)*scale;x++){
                    int rgba=image.getPixelRGBA(x,y);if((rgba&255)>145&&(rgba>>8&255)>145&&(rgba>>16&255)>145)bright++;
                }
                require(bright>40,"Information column "+column+" contains visible text");
            }
        }
    }
    private static void checkCleanLogoForm()throws ReflectiveOperationException {
        var field=GangScreen.class.getDeclaredField("labels");field.setAccessible(true);String labels=field.get(screen).toString();
        require(!labels.contains("ścieżk")&&!labels.contains("PNG /")&&!labels.contains("opcjonalne")&&!labels.contains("później"),"Removed path, format and optional-logo notices");
        require(screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Wczytaj i dopasuj")),"Manual path loading button removed");
    }
    private static void checkCleanHints()throws ReflectiveOperationException {
        var field=GangScreen.class.getDeclaredField("labels");field.setAccessible(true);String labels=field.get(screen).toString();
        require(!labels.contains("Przeciągaj rangi")&&!labels.contains("Pozycja na liście")&&!labels.contains("Zobacz podgląd"),"Removed rank-order and appearance instructions");
    }
    private static void checkCloseButton(Minecraft mc){
        var close=widget(mc.screen,"Zamknij");require(close.getWidth()==close.getHeight(),"Close button is square");
        double scale=GangLayout.of(mc.screen.width,mc.screen.height).scale()*mc.getWindow().getGuiScale();
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            for(int y=0;y<8;y++)for(int x=0;x<8;x++){
                int a=image.getPixelRGBA((int)((close.getX()+6+x+.5)*scale),(int)((close.getY()+6+y+.5)*scale));
                int b=image.getPixelRGBA((int)((close.getX()+13-x+.5)*scale),(int)((close.getY()+6+y+.5)*scale));
                require(a==b,"Close icon is horizontally symmetric");
                boolean bright=(a&255)>180&&(a>>8&255)>180&&(a>>16&255)>180;
                require(bright==(x==y||x==7-y),"Close icon has two centered, equal diagonals");
            }
        }
    }
    private static void checkLogoTexture(Minecraft mc,net.minecraft.resources.ResourceLocation id,int width,int height){
        require(id!=null,"Logo texture registered");
        var texture=mc.getTextureManager().getTexture(id);
        require(texture instanceof net.minecraft.client.renderer.texture.DynamicTexture,"Dynamic logo texture");
        var pixels=((net.minecraft.client.renderer.texture.DynamicTexture)texture).getPixels();
        require(pixels!=null&&pixels.getWidth()==width&&pixels.getHeight()==height,"Stored texture resolution "+width+"x"+height);
        texture.bind();
        require(org.lwjgl.opengl.GL11.glGetTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER)==org.lwjgl.opengl.GL11.GL_LINEAR,"Linear logo minification");
        require(org.lwjgl.opengl.GL11.glGetTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER)==org.lwjgl.opengl.GL11.GL_LINEAR,"Linear logo magnification");
    }
    private static void selectTab(int tab)throws Exception {
        var field=GangScreen.class.getDeclaredField("tab");field.setAccessible(true);field.setInt(screen,tab);screen.updated();
    }
    private static AbstractWidget widget(Screen screen,String prefix){
        var exact=screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals(prefix)).map(c->(AbstractWidget)c).findFirst();
        if(exact.isPresent())return exact.get();
        return screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().startsWith(prefix))
                .map(c->(AbstractWidget)c).findFirst().orElseThrow(()->new AssertionError("Missing widget: "+prefix));
    }
    private static void click(Minecraft mc,String prefix){
        AbstractWidget button=widget(mc.screen,prefix);var l=GangLayout.of(mc.screen.width,mc.screen.height);
        require(mc.screen.mouseClicked((button.getX()+button.getWidth()/2.0)*l.scale(),(button.getY()+button.getHeight()/2.0)*l.scale(),0),"Click "+prefix);
    }
    private static void fixture(boolean joined) {
        JsonObject state=new JsonObject();state.addProperty("player",PLAYER.toString());state.addProperty("page",0);state.addProperty("pages",1);
        JsonArray rows=new JsonArray();
        for(String name:new String[]{"Czarne Wilki","Bordowa Ekipa","Nocna Zmiana"}) {
            JsonObject row=new JsonObject();row.addProperty("id",UUID.randomUUID().toString());row.addProperty("name",name);
            row.addProperty("members",3);row.addProperty("requested",false);rows.add(row);
        }
        state.add("gangs",rows);
        if(joined) {
            if(directory==null){
                directory=new GangDirectory();var gang=directory.create(PLAYER,"Gracz","Czarne Wilki","");
                for(int i=2;i<=13;i++){UUID friend=new UUID(0,i);directory.request(friend,"Gracz"+i,gang.id);directory.accept(PLAYER,friend);}
                for(int i=0;i<50;i++){
                    UUID visitor=new UUID(1000,i+1);directory.request(visitor,"Gość"+i,gang.id);directory.accept(PLAYER,visitor);directory.leave(visitor);
                }
                // Fixed date makes the full month/year/time rendering reproducible.
                var when=java.time.LocalDateTime.of(2026,10,21,18,15).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                for(int i=0;i<gang.history.size();i++)gang.history.set(i,new GangDirectory.Event(when+i*60000,gang.history.get(i).text()));
                for(int i=14;i<=21;i++)directory.request(new UUID(0,i),"Kandydat"+i,gang.id);
            }
            JsonObject snapshot=GangView.snapshot(directory.of(PLAYER),PLAYER,0,0);
            for(var entry:snapshot.entrySet())state.add(entry.getKey(),entry.getValue());
        }
        ClientState.state=state;ClientState.message="";
    }
    private static ScrollPosition historyPosition()throws Exception {
        var field=GangScreen.class.getDeclaredField("historyScroll");field.setAccessible(true);return (ScrollPosition)field.get(screen);
    }
    private static void fixturePage(UUID player,int page){fixturePage(player,page,0);}
    private static void fixturePage(UUID player,int page,int historyPage){
        JsonObject state=GangView.snapshot(directory.of(PLAYER),player,page,0,historyPage);
        state.addProperty("player",player.toString());state.addProperty("message","");
        state.add("gangs",new JsonArray());state.addProperty("page",0);state.addProperty("pages",1);
        ClientState.state=state;ClientState.message="";
    }
    private static void terrainFixture(){
        var atlas=new pl.crimeandlaw.territory.TerrainAtlas();
        for(int z=-2;z<=3;z++)for(int x=-3;x<=3;x++){
            if(x==-3&&z==-2)continue;
            var key=new pl.crimeandlaw.territory.TerrainKey("minecraft:overworld",x,z);
            var surface=x< -1?pl.crimeandlaw.territory.TerrainProfile.Surface.WATER:x>1?pl.crimeandlaw.territory.TerrainProfile.Surface.ROCK:pl.crimeandlaw.territory.TerrainProfile.Surface.SOIL;
            var sample=new pl.crimeandlaw.territory.TerrainProfile.Sample(64,surface,x==0,"minecraft:plains");
            var templates=(x==0&&z==0||x==1&&z==-1)?java.util.List.of("minecraft:village/plains/houses/plains_weaponsmith_1","minecraft:village/plains/houses/plains_small_farm_1","minecraft:village/plains/houses/plains_library_1"):java.util.List.<String>of();
            atlas.remember(pl.crimeandlaw.territory.TerrainProfile.analyse(key,java.util.Collections.nCopies(16,sample),!templates.isEmpty(),templates,10));
        }
        TerrainClient.view=pl.crimeandlaw.territory.TerrainView.snapshot(atlas,"minecraft:overworld",0,0,key->key.z()!=-3,id->"Czarne Wilki");
    }
    private static void checkGradients(Minecraft mc){
        ColorPickerScreen d=(ColorPickerScreen)mc.screen;double scale=d.layout.scale()*mc.getWindow().getGuiScale();
        AbstractWidget bright=widget(d,"Jasność:"),sat=widget(d,"Nasycenie:");
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            int top=image.getPixelRGBA((int)((bright.getX()+38)*scale),(int)((bright.getY()+29)*scale));
            int bottom=image.getPixelRGBA((int)((bright.getX()+38)*scale),(int)((bright.getY()+144)*scale));
            require((top&255)>200&&(top>>8&255)<30&&(top>>16&255)<30,"Brightness top shows red");
            require((bottom&255)<30&&(bottom>>8&255)<10&&(bottom>>16&255)<10,"Brightness bottom shows black");
            int gray=image.getPixelRGBA((int)((sat.getX()+38)*scale),(int)((sat.getY()+144)*scale));
            require((gray>>8&255)>200&&(gray>>16&255)>200,"Saturation bottom shows pale neutral color");
        }
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
