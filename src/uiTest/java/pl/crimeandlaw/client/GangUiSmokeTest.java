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
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.uiSmokeOutput","../build/ui-smoke"));
    private static int frames,stage=-1;
    private static int waitFrames;
    private static int startupFrames;
    private static GangDirectory directory;
    private static final UUID FRIEND=new UUID(0,2);
    private static GangScreen screen;
    private static pl.crimeandlaw.territory.TerrainView unshiftedMap;
    private static int chosenX,chosenZ;
    private static long roleEdgeDeadline;
    private static TerrainPanel heldMap;
    private static int mapRound,mapStep,heldSelectionX,heldSelectionZ;
    private static double mapPointerX,mapPointerY;
    private static final double[][] MAP_PATH={{1.7,-1.4},{-2.1,2.2},{.35,.45},{2.4,1.1},{-2,-2},{0,0}};
    private static final StringBuilder REPORT=new StringBuilder();
    private static int headerStep=-1;

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
        if(!Boolean.getBoolean("crimeandlaw.uiSmoke"))return;
        Minecraft mc=Minecraft.getInstance();
        try {
            if(Boolean.getBoolean("crimeandlaw.headerAlignmentOnly")){checkHeaderAlignmentRun(mc);return;}
            if(stage==-2){
                if(++frames<24)return;capture(mc,"00-loading");
                require(screen.children().stream().noneMatch(c->c instanceof EditBox||c instanceof AbstractWidget w&&java.util.Set.of("Stwórz gang","Znajdź gang").contains(w.getMessage().getString())),"No creation flash before the authoritative snapshot");
                ClientState.refresh("",0);require(ClientState.message.contains("na serwerze"),"Loading state retains a useful missing-server-mod message");
                fixture(false);screen.updated();stage=0;frames=0;return;
            }
            if(stage<0) {
                if(++startupFrames>1800)throw new AssertionError("Client startup did not reach the title screen: "+mc.screen);
                if(mc.getOverlay()==null&&mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding){mc.options.onboardingAccessibilityFinished();onboarding.onClose();return;}
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                Files.createDirectories(OUT);
                mc.options.guiScale().set(2);mc.resizeDisplay();
                ClientState.state=new JsonObject();screen=new GangScreen();mc.setScreen(screen);
                stage=-2;frames=0;return;
            }
            if((stage==12||stage==50)&&!(mc.screen instanceof LogoCropScreen)){
                if(++waitFrames>600)throw new AssertionError("Crop dialog did not open: "+ClientState.message);return;
            }
            if((stage==13||stage==51)&&mc.screen instanceof LogoCropScreen){
                if(++waitFrames>1200)throw new AssertionError("Asynchronous image encoding did not finish");return;
            }
            if(stage==38&&System.nanoTime()<roleEdgeDeadline){
                var field=GangScreen.class.getDeclaredField("roleScroll");field.setAccessible(true);
                if(((ScrollPosition)field.get(screen)).offset()>0)return;
            }
            if(++frames<24)return;
            frames=0;
            switch(stage++) {
                case 0 -> {
                    checkCreation(mc);checkCloseButton(mc);capture(mc,"01-create-scale2");
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
                }
                case 13 -> {
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
                    checkLogoTexture(mc,LogoTexture.get(large),2048,2048);
                    checkLogoTexture(mc,LogoTexture.get(encoded),2048,2048);
                    directory.logo(PLAYER,encoded);
                    REPORT.append("PASS logo: 2048x2048 saved texture, legacy 64x64, detailed PNG stream decoding, trilinear minification/linear magnification filters, smooth crop preview.\n");
                    capture(mc,"13-logo-preview");fixturePage(FRIEND,0);screen.updated();selectTab(3);click(mc,"Wygląd");
                    require(screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Zarządzanie")),"Management hidden from ordinary member");
                    require(!widget(screen,"Zapisz dla gangu").active,"Member cannot save shared appearance");
                    require(!ClientState.state.has("requestCount"),"Private requests not sent to member");
                }
                case 14 -> {
                    capture(mc,"14-member-settings");
                    var savedLogo=GangScreen.class.getDeclaredField("draftLogo");savedLogo.setAccessible(true);require(((String)savedLogo.get(screen)).isEmpty(),"Acknowledged logo draft no longer shadows future shared updates");
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
                    edit.setValue("Rekruter");click(mc,"Zapraszanie / zgłoszenia");click(mc,"Wygląd i logo gangu");
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
                    capture(mc,"25-delegated-management");click(mc,"Wygląd");click(mc,"Logo");checkCleanLogoForm();
                    require(screen.children().stream().noneMatch(c->c instanceof EditBox),"Logo settings have no path input");
                    require(widget(screen,"Wybierz obraz…").active,"Delegated logo picker remains available");
                }
                case 26 -> {
                    capture(mc,"26-clean-logo");
                    fixturePage(PLAYER,0);screen.updated();selectTab(pl.crimeandlaw.gang.GangCategory.ROLES.tabId());click(mc,"+ Nowa ranga");
                    var edit=mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();
                    edit.setValue("Bez zapisu");click(mc,"Usuwanie członków");
                    mc.options.guiScale().set(2);mc.resizeDisplay();
                }
                case 27 -> {
                    capture(mc,"27-rank-dialog-scale2");checkCloseButton(mc);
                    var edit=mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();
                    require(edit.getValue().equals("Bez zapisu")&&((java.util.Set<?>)rankPermissions(mc.screen)).contains(GangDirectory.Permission.MANAGE_MEMBERS),"Rank draft survives a GUI resize");
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
                    screen.mouseClicked((panel.getX()+6.5*196/11)*l.scale(),(panel.getY()+4.5*196/11)*l.scale(),0);
                    screen.mouseReleased((panel.getX()+6.5*196/11)*l.scale(),(panel.getY()+4.5*196/11)*l.scale(),0);
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
                    screen.mouseClicked((panel.getX()+0.5*196/11)*l.scale(),(panel.getY()+0.5*196/11)*l.scale(),0);
                    screen.mouseReleased((panel.getX()+0.5*196/11)*l.scale(),(panel.getY()+0.5*196/11)*l.scale(),0);
                }
                case 30 -> {
                    capture(mc,"30-terrain-unavailable");
                    var sx=GangScreen.class.getDeclaredField("terrainX");sx.setAccessible(true);require(sx.getInt(screen)==-5,"Unavailable cell remains inspectable");
                    click(mc,"Siatka");click(mc,"+");click(mc,"+");
                }
                case 31 -> {
                    capture(mc,"31-terrain-zoom-and-grid");
                    var radius=GangScreen.class.getDeclaredField("terrainRadius");radius.setAccessible(true);require(radius.getInt(screen)==3,"Zoom changes the visible geographical area");
                    click(mc,"−");click(mc,"−");click(mc,"Siatka");
                    var actual=Path.of(System.getProperty("crimeandlaw.actualTerrainView","../build/server-smoke-terrain16a/view.json"));
                    require(Files.exists(actual),"Actual world surface from the dedicated-server test is available");
                    TerrainClient.receive(Files.readString(actual));selectMapCenter();
                }
                case 32 -> {
                    capture(mc,"32-actual-village-map-scale4");checkMapTexture(mc);
                    mc.options.guiScale().set(2);mc.resizeDisplay();
                }
                case 33 -> {
                    capture(mc,"33-actual-village-map-scale2");checkMapTexture(mc);
                    fixture(false);JsonArray rows=ClientState.state.getAsJsonArray("gangs");rows.remove(2);rows.remove(1);rows.remove(0);
                    for(int i=0;i<12;i++){
                        JsonObject row=new JsonObject();row.addProperty("id",new UUID(16,i).toString());row.addProperty("name","Gang testowy "+(i+1));
                        row.addProperty("members",i+1);row.addProperty("requested",i==0);rows.add(row);
                    }
                    ClientState.state.addProperty("pages",3);screen=new GangScreen();mc.setScreen(screen);click(mc,"Znajdź gang");ClientState.message="";
                }
                case 34 -> {
                    capture(mc,"34-find-twelve-gangs");
                    var field=screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();field.setValue("Wilki");
                    var bar=widget(screen,"Przewijanie gangów");require(bar.active,"All twelve server results share one scrollable page");
                    int footer=widget(screen,"›").getY();var l=GangLayout.of(screen.width,screen.height);
                    screen.mouseScrolled((bar.getX()-80)*l.scale(),(bar.getY()+50)*l.scale(),0,-100);
                    require(screen.children().contains(field)&&field.getValue().equals("Wilki"),"Scrolling does not rebuild or clear the search field");
                    require(widget(screen,"›").getY()==footer,"Directory pagination stays fixed");
                    var position=GangScreen.class.getDeclaredField("directoryScroll");position.setAccessible(true);
                    require(((ScrollPosition)position.get(screen)).offset()>0,"Directory reaches its final rows");
                }
                case 35 -> {
                    capture(mc,"35-find-scrolled");
                    var gang=directory.of(PLAYER);
                    while(gang.roles.size()<GangDirectory.MAX_ROLES)directory.role(PLAYER,"","Ranga"+gang.roles.size(),java.util.Set.of());
                    fixturePage(PLAYER,0);screen.updated();selectTab(pl.crimeandlaw.gang.GangCategory.ROLES.tabId());
                    require(widget(screen,"+ Nowa ranga").visible&&!widget(screen,"+ Nowa ranga").active,"Create rank action remains visible at the rank limit");
                    var field=screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();field.setValue("");
                    require(!widget(screen,"Zapisz rangę").active,"An empty rank name cannot be saved");
                    var bar=widget(screen,"Przewijanie rang");require(bar.active,"Twelve ranks have an independent scrollbar");
                    var l=GangLayout.of(screen.width,screen.height);int saveY=widget(screen,"Zapisz rangę").getY(),newY=widget(screen,"+ Nowa ranga").getY();
                    screen.mouseScrolled((bar.getX()-30)*l.scale(),(bar.getY()+50)*l.scale(),0,-100);
                    require(widget(screen,"Zapisz rangę").getY()==saveY&&widget(screen,"+ Nowa ranga").getY()==newY,"Rank editor and create action do not scroll with the list");
                    require(screen.children().contains(field)&&field.getValue().isEmpty(),"Rank list scrolling preserves the draft field");
                }
                case 36 -> {
                    capture(mc,"36-twelve-ranks-scrolled");mc.options.guiScale().set(4);mc.resizeDisplay();
                    var field=screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();
                    require(field.getValue().isEmpty()&&!widget(screen,"Zapisz rangę").active,"Resizing preserves an intentionally empty rank draft");
                }
                case 37 -> {
                    capture(mc,"37-rank-draft-resize");
                    var row=widget(screen,"≡  "+new java.util.ArrayList<>(directory.of(PLAYER).roles.values()).getLast().name);require(row.visible,"Final rank is reachable");
                    var bar=widget(screen,"Przewijanie rang");var l=GangLayout.of(screen.width,screen.height);
                    double x=(row.getX()+20)*l.scale(),from=(row.getY()+8)*l.scale(),to=(bar.getY()+2)*l.scale();
                    screen.mouseClicked(x,from,0);
                    screen.mouseDragged(x,to,0,0,to-from);
                    // No more mouse movement: rendering must continue scrolling by elapsed time.
                    // Use elapsed time, not 24 frames, which may be only a few milliseconds on a fast renderer.
                    roleEdgeDeadline=System.nanoTime()+3_000_000_000L;

                }
                case 38 -> {
                    var position=GangScreen.class.getDeclaredField("roleScroll");position.setAccessible(true);
                    require(((ScrollPosition)position.get(screen)).offset()==0,"Holding a dragged rank at the edge continues scrolling without movement");
                    capture(mc,"38-rank-drag-autoscroll");var row=widget(screen,"Przewijanie rang");var l=GangLayout.of(screen.width,screen.height);
                    screen.mouseReleased((row.getX()-50)*l.scale(),(row.getY()+2)*l.scale(),0);ClientState.message="";selectTab(6);
                }
                case 39 -> {capture(mc,"39-premie-panel");selectTab(7);}
                case 40 -> {capture(mc,"40-balance-panel");selectTab(8);}
                case 41 -> {capture(mc,"41-blacklist-panel");selectTab(9);}
                case 42 -> {capture(mc,"42-goals-panel");TerrainClient.home();terrainFixture();selectTab(2);}
                case 43 -> {
                    capture(mc,"43-map-before-pan");var panel=widget(screen,"Mapa terenu");var l=GangLayout.of(screen.width,screen.height);
                    screen.mouseScrolled((panel.getX()+90)*l.scale(),(panel.getY()+90)*l.scale(),0,1);
                    var radius=GangScreen.class.getDeclaredField("terrainRadius");radius.setAccessible(true);require(radius.getInt(screen)==4,"Wheel over map changes zoom");
                    unshiftedMap=TerrainClient.view;panel=widget(screen,"Mapa terenu");
                    var sx=GangScreen.class.getDeclaredField("terrainX");sx.setAccessible(true);chosenX=sx.getInt(screen);
                    var sz=GangScreen.class.getDeclaredField("terrainZ");sz.setAccessible(true);chosenZ=sz.getInt(screen);
                    double x=(panel.getX()+90)*l.scale(),y=(panel.getY()+90)*l.scale();
                    require(screen.mouseClicked(x,y,0),"Map drag starts");
                    require(screen.mouseDragged(x+48*l.scale(),y+24*l.scale(),0,48*l.scale(),24*l.scale()),"Map drag is handled");
                }
                case 44 -> {
                    capture(mc,"44-map-drag-preview");var panel=widget(screen,"Mapa terenu");var l=GangLayout.of(screen.width,screen.height);
                    screen.mouseReleased((panel.getX()+138)*l.scale(),(panel.getY()+114)*l.scale(),0);
                    var targetField=TerrainClient.class.getDeclaredField("target");targetField.setAccessible(true);
                    var target=(pl.crimeandlaw.territory.TerrainViewport)targetField.get(null);
                    require(target.equals(new pl.crimeandlaw.territory.TerrainViewport(-2,-1)),"Drag converts to the requested chunk viewport");
                    TerrainClient.receive(ClientState.JSON.toJson(unshiftedMap));require(TerrainClient.view.centerX()==target.x()&&TerrainClient.view.centerZ()==target.z(),"Stale response does not replace the immediate cached viewport");
                    JsonObject shifted=ClientState.JSON.toJsonTree(unshiftedMap).getAsJsonObject();
                    shifted.addProperty("centerX",target.x());shifted.addProperty("centerZ",target.z());
                    for(var item:shifted.getAsJsonArray("cells")){
                        var cell=item.getAsJsonObject();cell.addProperty("x",cell.get("x").getAsInt()+target.x());cell.addProperty("z",cell.get("z").getAsInt()+target.z());
                    }
                    shifted.addProperty("playerX",1000000);TerrainClient.receive(shifted.toString());
                    require(TerrainClient.view.centerX()==-2&&!TerrainClient.following(),"Matching panned view is accepted");
                    var sx=GangScreen.class.getDeclaredField("terrainX");sx.setAccessible(true);var sz=GangScreen.class.getDeclaredField("terrainZ");sz.setAccessible(true);
                    require(sx.getInt(screen)==chosenX&&sz.getInt(screen)==chosenZ,"Panning preserves the independently selected chunk");
                }
                case 45 -> {
                    capture(mc,"45-map-panned-player-outside");click(mc,"Wróć do gracza");require(TerrainClient.following(),"Player button restores follow mode");
                    TerrainClient.receive(ClientState.JSON.toJson(unshiftedMap));screen.setFocused(null);screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT,0,0);require(!TerrainClient.following(),"Keyboard arrow moves the viewport");
                    require(screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&java.util.Set.of("‹","›","↑","↓","Gracz").contains(w.getMessage().getString())),"No bottom map arrows or text player button");
                    var icon=widget(screen,"Wróć do gracza");require(icon.getY()==widget(screen,"Siatka").getY(),"Player icon is in the map header");
                    for(String name:new String[]{"−","+","Odśwież"})require(((FlatButton)widget(screen,name)).isTextCentered(),"Centered map action "+name);
                    screen.setFocused(null);screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_HOME,0,0);require(TerrainClient.following(),"Home restores follow mode");
                    TerrainClient.receive(ClientState.JSON.toJson(unshiftedMap));
                }
                case 46 -> {
                    capture(mc,"46-map-return-to-player");
                    TerrainClient.receive(Files.readString(Path.of(System.getProperty("crimeandlaw.actualTerrainView","../build/server-smoke-terrain16a/view.json"))));
                    selectMapCenter();mc.options.guiScale().set(2);mc.resizeDisplay();
                }
                case 47 -> {
                    capture(mc,"47-actual-map-navigation-final");checkMapTexture(mc);
                    var bar=widget(screen,"Przewijanie opisu terenu");require(bar.active||!bar.visible,"A short terrain inspector hides its unused scrollbar");
                    selectTab(3);click(mc,"Baner");click(mc,"Jednolity kolor");click(mc,"Zmień kolor baneru…");
                    var hex=mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();hex.setValue("416B80");click(mc,"Zastosuj");
                }
                case 48 -> {
                    capture(mc,"48-banner-color-settings");var field=GangScreen.class.getDeclaredField("draftBanner");field.setAccessible(true);
                    var banner=(pl.crimeandlaw.gang.GangBanner)field.get(screen);require(banner.color()==0x416B80&&banner.opacity()==255,"Banner color wheel changes the solid preview");
                    require(((FlatButton)widget(screen,"Zapisz baner gangu")).isTextCentered(),"Banner save label is centered");
                    directory.banner(PLAYER,banner);fixturePage(PLAYER,0);screen.updated();selectTab(0);
                }
                case 49 -> {
                    capture(mc,"49-banner-solid-overview");checkBannerEdges(mc);selectTab(3);click(mc,"Baner");
                    var image=new java.awt.image.BufferedImage(1024,256,java.awt.image.BufferedImage.TYPE_INT_ARGB);var draw=image.createGraphics();
                    draw.setPaint(new java.awt.GradientPaint(0,0,new java.awt.Color(0x552E59),1024,256,new java.awt.Color(0x347898)));draw.fillRect(0,0,1024,256);
                    draw.setColor(new java.awt.Color(255,255,255,80));for(int x=0;x<1024;x+=64)draw.fillRect(x,0,2,256);draw.dispose();
                    Path file=OUT.resolve("wide-banner-fixture.png").toAbsolutePath();javax.imageio.ImageIO.write(image,"png",file.toFile());screen.onFilesDrop(java.util.List.of(file));
                }
                case 50 -> {
                    capture(mc,"50-banner-wide-crop");require(mc.screen instanceof LogoCropScreen,"Banner gets its own crop dialog");
                    require(widget(mc.screen,"Wypełnij baner").visible,"Wide crop mode is explicit");click(mc,"Zastosuj baner");
                }
                case 51 -> {
                    capture(mc,"51-banner-image-settings");var field=GangScreen.class.getDeclaredField("draftBanner");field.setAccessible(true);
                    var banner=(pl.crimeandlaw.gang.GangBanner)field.get(screen);
                    var decoded=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(banner.image())));
                    require(decoded.getWidth()==4096&&decoded.getHeight()==951,"Banner keeps its independent detailed strip texture");
                    directory.banner(PLAYER,banner);fixturePage(PLAYER,0);screen.updated();selectTab(0);
                }
                case 52 -> {
                    capture(mc,"52-banner-image-overview");checkLogoTexture(mc,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("crimeandlaw","gang_banner"),4096,951);
                    fixture(false);screen=new GangScreen();mc.setScreen(screen);
                    var name=screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();name.setValue("Test");
                    screen.setFocused(widget(screen,"Zamknij"));screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);
                    require(mc.screen!=screen,"Enter activates the focused close button instead of submitting the form");mc.setScreen(screen);
                    name=screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow();screen.setFocused(name);screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);
                    require(!ClientState.message.isBlank(),"Enter in the name field submits the form");
                }
                case 53 -> {
                    capture(mc,"53-create-primary-action");require(((FlatButton)widget(screen,"Utwórz gang")).isTextCentered(),"Create action label is centered");
                    fixturePage(PLAYER,0);screen.updated();selectTab(1);click(mc,"Gracz2");
                    var card=widget(screen,"Profil członka:");var l=GangLayout.of(screen.width,screen.height);
                    require(card.getX()>=0&&card.getX()+card.getWidth()<=l.canvasWidth()&&card.getY()+card.getHeight()<=l.canvasHeight(),"Member portrait card fits inside the screen");
                }
                case 54 -> {
                    capture(mc,"54-member-skin-card");screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0);
                    require(mc.screen==screen&&screen.children().stream().noneMatch(c->c instanceof MemberCard),"Escape closes the member card first");
                    click(mc,"Gracz2");click(mc,"Gang");require(screen.children().stream().noneMatch(c->c instanceof MemberCard),"Clicking outside closes the member card and activates navigation");
                    selectTab(3);click(mc,"Baner");
                }
                case 55 -> {
                    capture(mc,"55-banner-final-settings");
                    checkDragRoundTrip(mc);
                }
                case 56 -> {
                    capture(mc,"56-map-roundtrip");
                    directory=new GangDirectory();directory.create(PLAYER,"Gracz","Katalog praw","");fixturePage(PLAYER,0);screen.updated();
                    selectTab(pl.crimeandlaw.gang.GangCategory.ROLES.tabId());click(mc,"+ Nowa ranga");
                    var boxes=mc.screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).toList();boxes.getFirst().setValue("Planowane");boxes.get(1).setValue("rajdu");
                }
                case 57 -> {
                    capture(mc,"57-planned-permissions");
                    require(!widget(mc.screen,"Przewijanie uprawnień").visible,"Filtered short permission list hides its scrollbar");
                    var catalog=widget(mc.screen,"Lista uprawnień");var l=GangLayout.of(mc.screen.width,mc.screen.height);
                    mc.screen.mouseClicked((catalog.getX()+20)*l.scale(),(catalog.getY()+12)*l.scale(),0);
                    mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE,0,0);
                    require(((java.util.Set<?>)rankPermissions(mc.screen)).isEmpty(),"Planned permissions reject mouse and keyboard changes");
                    click(mc,"Anuluj");mc.setScreen(new SkinLayerProbe(mc));
                }
                case 58 -> {
                    capture(mc,"58-skin-outer-layers");checkSkinLayers(mc);mc.screen.onClose();mc.setScreen(new ImageQualityProbe());
                }
                case 59 -> {
                    capture(mc,"59-high-resolution-minification");checkMinification(mc);
                    var image=new java.awt.image.BufferedImage(800,400,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    mc.setScreen(new LogoCropScreen(screen,image,"cancel-probe.png",encoded->{throw new AssertionError("Cancelled encoding changed the logo");}));
                    click(mc,"Zastosuj logo");require(!widget(mc.screen,"Zastosuj logo").active,"Encoding cannot be started twice");
                    require(!widget(mc.screen,"Przybliżenie").active,"Crop is frozen while the snapshot is encoded");click(mc,"Anuluj");
                }
                case 60 -> {
                    require(mc.screen==screen,"Cancelled asynchronous encoding leaves the parent open");
                    TerrainClient.clear();
                    for(int z:new int[]{-7,0,7})for(int x:new int[]{-7,0,7})TerrainClient.receive(ClientState.JSON.toJson(wideMap(x,z,0)));
                    TerrainClient.receive(ClientState.JSON.toJson(wideMap(0,0,0)));mapRound=0;startHeldMap(mc);
                }
                case 61 -> {
                    require(widget(screen,"Mapa terenu")==heldMap&&heldMap.dragging(),"The same focused map survives camera changes and server replies");
                    checkMapCanvas(mc,heldMap);
                    if(mapStep>0)capture(mc,"60-map-held-r"+mapRound+"-step"+mapStep);
                    var before=TerrainClient.view;
                    // Delayed response from another viewport, containing a newer player heading.
                    TerrainClient.receive(ClientState.JSON.toJson(wideMap(-7,0,mapStep*7+mapRound+1)));
                    require(TerrainClient.view.centerX()==before.centerX()&&TerrainClient.view.centerZ()==before.centerZ(),"A delayed neighboring response cannot move the held camera");
                    require(widget(screen,"Mapa terenu")==heldMap&&heldMap.dragging(),"Receiving new data keeps the gesture and focus");
                    if(mapStep<MAP_PATH.length){
                        var l=GangLayout.of(screen.width,screen.height);var field=GangScreen.class.getDeclaredField("terrainRadius");field.setAccessible(true);
                        double cell=TerrainPanel.GRID/(double)(field.getInt(screen)*2+1);
                        double dx=MAP_PATH[mapStep][0]*cell,dy=MAP_PATH[mapStep][1]*cell;
                        require(screen.mouseDragged(mapPointerX+dx*l.scale(),mapPointerY+dy*l.scale(),0,dx*l.scale(),dy*l.scale()),"Continuous held drag handled");
                        require(TerrainClient.view.centerX()==Math.round(-MAP_PATH[mapStep][0])&&TerrainClient.view.centerZ()==Math.round(-MAP_PATH[mapStep][1]),"Camera moves while the mouse is held");
                        mapStep++;stage=61;
                    }else screen.mouseReleased(mapPointerX,mapPointerY,0);
                }
                case 62 -> {
                    checkMapCanvas(mc,(TerrainPanel)widget(screen,"Mapa terenu"));
                    var x=GangScreen.class.getDeclaredField("terrainX");x.setAccessible(true);var z=GangScreen.class.getDeclaredField("terrainZ");z.setAccessible(true);
                    require(x.getInt(screen)==heldSelectionX&&z.getInt(screen)==heldSelectionZ,"Neither panning nor replies select a different chunk");
                    require(TerrainClient.view.centerX()==0&&TerrainClient.view.centerZ()==0,"Full multi-direction loop returns to its world origin");
                    if(++mapRound<3){startHeldMap(mc);stage=61;}
                    else {
                        REPORT.append("PASS held map pan: three zoom levels, eighteen direction/reversal steps, delayed neighboring replies during a held gesture, stable focus/selection, no background or fog pixels across fully known visible terrain, and round-trip release.\n");
                    REPORT.append("PASS high-resolution images: actual 2048-square/4096-wide textures, full GPU mip chains, neutral checkerboard minification without coarse aliasing, asynchronous apply and cancellation.\n");
                    REPORT.append("PASS: actual Minecraft renderer; earlier map, list, roles, register and logo checks retained; centered actions, accented creation form, member skin card/dismissal, banner color/image/crop, Enter focus, stationary rank-drag autoscroll, persistent chunk selection and top player icon; loading gate, full strip edges, cached drag round-trip, searchable locked future permissions; actual alpha-blended hat, jacket and both sleeves for wide and slim skins.\n");
                    Files.writeString(OUT.resolve("result.txt"),REPORT.toString());System.out.println("CRIME_UI_SMOKE_PASS");mc.stop();
                    }
                }
            }
        } catch(Throwable error) {
            error.printStackTrace();
            try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"FAIL: "+error);}catch(Exception ignored){}
            stage=1000;mc.stop();
        }
    }
    /** A narrow, zoomed-out banner exposes differences between the three rendered axes. */
    private static void checkHeaderAlignmentRun(Minecraft mc)throws Exception {
        if(headerStep<0){
            if(++startupFrames>1800)throw new AssertionError("Header client did not reach the title screen");
            if(mc.getOverlay()==null&&mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding){mc.options.onboardingAccessibilityFinished();onboarding.onClose();return;}
            if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
            Files.createDirectories(OUT);directory=new GangDirectory();directory.create(PLAYER,"Gracz","ZSRR","");
            var logo=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var paint=logo.createGraphics();paint.setColor(java.awt.Color.RED);paint.fillRect(0,0,64,64);paint.dispose();
            directory.of(PLAYER).logo=java.util.Base64.getEncoder().encodeToString(LogoImages.png(logo));
            fixturePage(PLAYER,0);mc.options.guiScale().set(2);mc.resizeDisplay();screen=new GangScreen();mc.setScreen(screen);
            headerBanner(false);headerStep=0;frames=0;return;
        }
        if(++frames<24)return;frames=0;
        capture(mc,"header-alignment-"+headerStep);
        var layout=GangLayout.of(screen.width,screen.height);double scale=layout.scale()*mc.getWindow().getGuiScale();
        int left=(layout.canvasWidth()-layout.panelWidth())/2,top=(layout.canvasHeight()-layout.panelHeight())/2;
        int from=(int)Math.ceil((left+109)*scale),to=(int)((left+layout.panelWidth()-1)*scale),cy=top+55;
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            double banner=headerPixelCenter(image,from,to,(int)((cy+69)*scale),0xFFFFFF);
            double logo=headerPixelCenter(image,from,to,(int)((cy+24)*scale),0xFF0000);
            double name=headerPixelCenter(image,from,to,(int)((cy+76)*scale),screen.currentTheme().border());
            double strip=(left+109+(layout.panelWidth()-110)/2.0)*scale;
            require(Math.abs(banner-strip)<=1.1,"The zoomed-out source stays centered in the full banner strip");
            require(Math.abs(logo-banner)<=1.1,"Logo aligns with the banner center: logo="+logo+", banner="+banner);
            require(Math.abs(name-banner)<=1.1,"Name frame aligns with the banner center: frame="+name+", banner="+banner);
            REPORT.append("PASS header ").append(headerStep).append(": banner=").append(banner).append(", logo=").append(logo).append(", name=").append(name).append(" pixels\n");
        }
        switch(headerStep++){
            case 0->{mc.options.guiScale().set(4);mc.resizeDisplay();}
            case 1->headerBanner(true);
            case 2->{mc.options.guiScale().set(2);mc.resizeDisplay();}
            case 3->{REPORT.append("PASS: actual Minecraft framebuffer; full-image and further zoomed-out portrait banner, GUI scales 2 and 4, shared banner/logo/name axis within 1.1 physical pixels.\n");Files.writeString(OUT.resolve("result.txt"),REPORT.toString());System.out.println("CRIME_HEADER_ALIGNMENT_PASS");mc.stop();}
        }
    }
    private static void headerBanner(boolean zoomOut)throws Exception {
        var source=new java.awt.image.BufferedImage(240,300,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var paint=source.createGraphics();paint.setColor(java.awt.Color.WHITE);paint.fillRect(0,0,240,300);paint.dispose();
        var crop=new LogoCrop(240,300,screen.bannerAspect());crop.fit(true);if(zoomOut)crop.zoom(crop.zoom()*.65);
        directory.of(PLAYER).banner=new pl.crimeandlaw.gang.GangBanner(LogoImages.encode(source,crop,true),0x15161C,255);
        fixturePage(PLAYER,0);screen.updated();
    }
    private static double headerPixelCenter(com.mojang.blaze3d.platform.NativeImage image,int from,int to,int y,int rgb){
        int first=-1,last=-1;
        for(int x=from;x<to;x++){
            int rgba=image.getPixelRGBA(x,y),actual=(rgba&255)<<16|(rgba&0xFF00)|(rgba>>16&255);
            // The menu presentation can shade the white fixture vertically; its edges stay neutral.
            boolean neutral=rgb==0xFFFFFF&&(actual>>16&255)>170&&(actual>>16&255)==(actual>>8&255)&&(actual>>8&255)==(actual&255);
            if(actual==rgb||neutral){if(first<0)first=x;last=x;}
        }
        require(first>=0&&last>first,"Header fixture color is visible: "+Integer.toHexString(rgb));
        return (first+last+1)/2.0;
    }
    private static pl.crimeandlaw.territory.TerrainView wideMap(int x,int z,float yaw){
        return pl.crimeandlaw.territory.TerrainView.snapshot(new pl.crimeandlaw.territory.TerrainAtlas(),"minecraft:overworld",x,z,key->true,id->"",key->{
            byte[] colors=new byte[256];
            var color=((key.x()+key.z())&1)==0?net.minecraft.world.level.material.MapColor.GRASS:net.minecraft.world.level.material.MapColor.SAND;
            java.util.Arrays.fill(colors,color.getPackedId(net.minecraft.world.level.material.MapColor.Brightness.NORMAL));
            return pl.crimeandlaw.territory.TerrainMapTile.of(colors);
        },pl.crimeandlaw.territory.TerrainView.BUFFER_RADIUS).withPosition(8,8,yaw);
    }
    private static void startHeldMap(Minecraft mc)throws Exception{
        TerrainClient.pan(-TerrainClient.view.centerX(),-TerrainClient.view.centerZ());
        var radius=GangScreen.class.getDeclaredField("terrainRadius");radius.setAccessible(true);radius.setInt(screen,new int[]{5,3,2}[mapRound]);
        selectTab(2);heldMap=(TerrainPanel)widget(screen,"Mapa terenu");
        var x=GangScreen.class.getDeclaredField("terrainX");x.setAccessible(true);heldSelectionX=x.getInt(screen);
        var z=GangScreen.class.getDeclaredField("terrainZ");z.setAccessible(true);heldSelectionZ=z.getInt(screen);
        var l=GangLayout.of(screen.width,screen.height);mapPointerX=(heldMap.getX()+98)*l.scale();mapPointerY=(heldMap.getY()+98)*l.scale();
        require(screen.mouseClicked(mapPointerX,mapPointerY,0),"Start held map gesture");mapStep=0;
    }
    private static void checkMapCanvas(Minecraft mc,TerrainPanel panel){
        double scale=GangLayout.of(mc.screen.width,mc.screen.height).scale()*mc.getWindow().getGuiScale();
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            for(int y=2;y<TerrainPanel.GRID-2;y+=4)for(int x=2;x<TerrainPanel.GRID-2;x+=4){
                if(x<19&&y<19||x<82&&y>TerrainPanel.GRID-21)continue;
                int pixel=image.getPixelRGBA((int)((panel.getX()+x+.5)*scale),(int)((panel.getY()+y+.5)*scale));
                require(pixel!=0xFF2B2629&&pixel!=0xFF292626&&pixel!=0xFF2D2A2A,"Known map pixels cover every visible edge during a held drag at "+x+","+y);
            }
        }
    }
    private static final class ImageQualityProbe extends Screen {
        ImageQualityProbe()throws Exception{
            super(net.minecraft.network.chat.Component.literal("Próba szczegółów grafiki"));
            for(boolean banner:new boolean[]{false,true}){
                int width=banner?4096:2048,height=banner?951:2048;
                var image=new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<height;y++)for(int x=0;x<width;x++)image.setRGB(x,y,((x+y)&1)==0?0xFF000000:0xFFFFFFFF);
                String png=java.util.Base64.getEncoder().encodeToString(LogoImages.png(image));
                if(banner)BannerTexture.get(png);else LogoTexture.get(png);
            }
        }
        @Override public void render(net.minecraft.client.gui.GuiGraphics g,int mx,int my,float delta){
            g.fill(0,0,width,height,0xFF181A22);var l=GangLayout.of(width,height);g.pose().pushPose();g.pose().scale(l.scale(),l.scale(),1);
            LogoTexture.draw(g,80,100,137,137);BannerTexture.draw(g,250,100,360,84,255);g.pose().popPose();
        }
        @Override public void removed(){LogoTexture.clear();BannerTexture.clear();}
    }
    private static void checkMinification(Minecraft mc){
        double scale=GangLayout.of(mc.screen.width,mc.screen.height).scale()*mc.getWindow().getGuiScale();
        checkLogoTexture(mc,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("crimeandlaw","gang_logo"),2048,2048);
        checkLogoTexture(mc,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("crimeandlaw","gang_banner"),4096,951);
        for(int[] shape:new int[][]{{2048,2048},{4096,951}}){
            var texture=mc.getTextureManager().getTexture(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("crimeandlaw",shape[0]==2048?"gang_logo":"gang_banner"));texture.bind();
            for(int level=0;level<=31-Integer.numberOfLeadingZeros(shape[0]);level++){
                require(org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,level,org.lwjgl.opengl.GL11.GL_TEXTURE_WIDTH)==Math.max(1,shape[0]>>level),"Every mip level is allocated");
                require(org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,level,org.lwjgl.opengl.GL11.GL_TEXTURE_HEIGHT)==Math.max(1,shape[1]>>level),"Non-square mip level is complete");
            }
        }
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            for(int[] rect:new int[][]{{80,100,137,137},{250,100,360,84}})
                for(int y=rect[1]+10;y<rect[1]+rect[3]-10;y+=7)for(int x=rect[0]+10;x<rect[0]+rect[2]-10;x+=7){
                    int pixel=image.getPixelRGBA((int)(x*scale),(int)(y*scale));
                    for(int shift:new int[]{0,8,16}){int channel=pixel>>shift&255;require(channel>=110&&channel<=145,"Fine source checkerboard averages to stable gray instead of a coarse pattern");}
                }
        }
    }
    private static final class SkinLayerProbe extends Screen {
        private final net.minecraft.resources.ResourceLocation texture=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("crimeandlaw","layer_probe");
        SkinLayerProbe(Minecraft mc)throws Exception{
            super(net.minecraft.network.chat.Component.literal("Próba warstw skina"));
            var image=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);var draw=image.createGraphics();
            draw.setColor(java.awt.Color.BLUE);draw.fillRect(8,8,8,8);draw.fillRect(20,20,8,12);draw.fillRect(44,20,4,12);draw.fillRect(36,52,4,12);
            draw.setColor(new java.awt.Color(255,0,0,128));draw.fillRect(40,8,8,8);
            draw.setColor(java.awt.Color.GREEN);draw.fillRect(20,36,8,12);draw.setColor(java.awt.Color.YELLOW);draw.fillRect(44,36,4,12);
            draw.setColor(java.awt.Color.MAGENTA);draw.fillRect(52,52,4,12);draw.dispose();
            mc.getTextureManager().register(texture,new net.minecraft.client.renderer.texture.DynamicTexture(com.mojang.blaze3d.platform.NativeImage.read(new java.io.ByteArrayInputStream(LogoImages.png(image)))));
        }
        @Override public void render(net.minecraft.client.gui.GuiGraphics g,int mx,int my,float delta){
            g.fill(0,0,width,height,0xFF181A22);var l=GangLayout.of(width,height);g.pose().pushPose();g.pose().scale(l.scale(),l.scale(),1);
            g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            try{
                var method=MemberCard.class.getDeclaredMethod("portrait",net.minecraft.client.gui.GuiGraphics.class,net.minecraft.client.resources.PlayerSkin.class,int.class,int.class);method.setAccessible(true);
                for(var model:net.minecraft.client.resources.PlayerSkin.Model.values())method.invoke(null,g,new net.minecraft.client.resources.PlayerSkin(texture,null,null,null,model,false),100+model.ordinal()*200,100);
            }catch(Exception error){throw new RuntimeException(error);}
            g.flush();com.mojang.blaze3d.systems.RenderSystem.disableBlend();g.pose().popPose();
        }
        @Override public void onClose(){Minecraft.getInstance().setScreen(screen);}
        @Override public void removed(){Minecraft.getInstance().getTextureManager().release(texture);}
    }
    private static void checkSkinLayers(Minecraft mc){
        double scale=GangLayout.of(mc.screen.width,mc.screen.height).scale()*mc.getWindow().getGuiScale();
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            for(int origin:new int[]{100,300}){
                int hat=image.getPixelRGBA((int)((origin+36)*scale),(int)(112*scale));
                require((hat&255)>100&&(hat>>16&255)>100&&(hat>>8&255)<30,"Semitransparent hat blends above the base head");
                int jacket=image.getPixelRGBA((int)((origin+36)*scale),(int)(142*scale));require((jacket>>8&255)>240,"Second torso layer is visible");
                int right=image.getPixelRGBA((int)((origin+20)*scale),(int)(142*scale));require((right&255)>240&&(right>>8&255)>240,"Right outer sleeve is visible");
                int left=image.getPixelRGBA((int)((origin+52)*scale),(int)(142*scale));require((left&255)>240&&(left>>16&255)>240,"Left outer sleeve is visible");
            }
        }
    }
    private static void capture(Minecraft mc,String name)throws Exception {
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            require((image.getPixelRGBA(image.getWidth()*1600/1920,image.getHeight()*850/1080)&0xFFFFFF)!=0,"Panel background after scale/tab change: "+name);
            if(mc.screen instanceof GangScreen&&ClientState.gang()!=null){
                require((image.getPixelRGBA(image.getWidth()*600/1920,image.getHeight()*200/1080)&0xFFFFFF)!=0,"Header background after tab change: "+name);
                require((image.getPixelRGBA(image.getWidth()*250/1920,image.getHeight()*500/1080)&0xFFFFFF)!=0,"Sidebar background after tab change: "+name);
                require((image.getPixelRGBA(image.getWidth()*900/1920,image.getHeight()*150/1080)&0xFFFFFF)!=0,"Gang name frame after tab change: "+name);
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
    private static void selectMapCenter(){
        var panel=widget(screen,"Mapa terenu");var l=GangLayout.of(screen.width,screen.height);
        double x=(panel.getX()+TerrainPanel.GRID/2.0)*l.scale(),y=(panel.getY()+TerrainPanel.GRID/2.0)*l.scale();
        screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);
    }
    private static void checkCreation(Minecraft mc)throws ReflectiveOperationException {
        var l=GangLayout.of(screen.width,screen.height);
        var boxes=screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).toList();
        require(boxes.size()==1,"Only the name field remains; no logo path field");checkCleanLogoForm();EditBox name=boxes.getFirst();
        require(screen.mouseClicked((name.getX()+10)*l.scale(),(name.getY()+8)*l.scale(),0),"Scaled text field click");
        name.setValue("");screen.charTyped('T',0);screen.charTyped('e',0);screen.charTyped('s',0);screen.charTyped('t',0);
        require(name.getValue().equals("Test"),"Typing after scaled click");
        name.setValue("");require(!widget(screen,"Utwórz gang").active,"Blank gang name cannot be submitted");
        name.setValue("   ");require(!widget(screen,"Utwórz gang").active,"Whitespace gang name cannot be submitted");
        name.setValue("Test");require(widget(screen,"Utwórz gang").active,"Valid gang name enables create action");
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
        require(org.lwjgl.opengl.GL11.glGetTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER)==org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR,"Trilinear logo minification");
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
        for(var permission:GangDirectory.Permission.values())if(permission.label.equals(prefix)){
            var catalog=widget(mc.screen,"Lista uprawnień");var l=GangLayout.of(mc.screen.width,mc.screen.height);
            require(mc.screen.mouseClicked((catalog.getX()+20)*l.scale(),(catalog.getY()+permission.ordinal()*PermissionList.ROW+10)*l.scale(),0),"Click permission "+prefix);return;
        }
        AbstractWidget button=widget(mc.screen,prefix);var l=GangLayout.of(mc.screen.width,mc.screen.height);
        require(mc.screen.mouseClicked((button.getX()+button.getWidth()/2.0)*l.scale(),(button.getY()+button.getHeight()/2.0)*l.scale(),0),"Click "+prefix);
    }
    private static Object rankPermissions(Screen screen)throws Exception {
        var field=NewRoleScreen.class.getDeclaredField("permissions");field.setAccessible(true);return field.get(screen);
    }
    private static void checkBannerEdges(Minecraft mc){
        var l=GangLayout.of(screen.width,screen.height);double scale=l.scale()*mc.getWindow().getGuiScale();int left=(l.canvasWidth()-l.panelWidth())/2,top=(l.canvasHeight()-l.panelHeight())/2;
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            int reference=image.getPixelRGBA((int)((left+250.5)*scale),(int)((top+43.5)*scale));
            for(int x:new int[]{left+109,left+110,left+l.panelWidth()-3,left+l.panelWidth()-2}){
                int value=image.getPixelRGBA((int)((x+.5)*scale),(int)((top+43.5)*scale));require(value==reference,"Banner reaches the full identity strip width, without padding gaps");
            }
        }
    }
    private static void checkDragRoundTrip(Minecraft mc)throws Exception {
        TerrainClient.clear();terrainFixture();selectTab(2);var original=TerrainClient.view;var l=GangLayout.of(screen.width,screen.height);
        for(int delta:new int[]{64,-64}){
            var panel=widget(screen,"Mapa terenu");double x=(panel.getX()+90)*l.scale(),y=(panel.getY()+90)*l.scale();
            screen.mouseClicked(x,y,0);screen.mouseDragged(x+delta*l.scale(),y,0,delta*l.scale(),0);screen.mouseReleased(x+delta*l.scale(),y,0);
            panel=widget(screen,"Mapa terenu");var field=TerrainPanel.class.getDeclaredField("dragX");field.setAccessible(true);require(field.getDouble(panel)==0,"No stale drag translation remains after release");
        }
        require(TerrainClient.view.centerX()==original.centerX()&&TerrainClient.view.centerZ()==original.centerZ(),"Immediate reverse drag returns to the original viewport");
        for(var cell:original.cells())if(!cell.mapPixels().isEmpty()){
            var actual=TerrainClient.view.cells().stream().filter(c->c.x()==cell.x()&&c.z()==cell.z()).findFirst().orElseThrow();
            require(actual.mapPixels().equals(cell.mapPixels()),"Immediate pan out and back retains every known pixel without any server reply");
        }
        TerrainClient.receive(ClientState.JSON.toJson(original));
        var panel=widget(screen,"Mapa terenu");var field=TerrainPanel.class.getDeclaredField("dragX");field.setAccessible(true);require(field.getDouble(panel)==0,"Identical server response cannot leave the map clipped");
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
        JsonObject state=GangView.snapshot(directory.of(PLAYER),player,page,0,historyPage,java.util.Set.of(PLAYER,FRIEND));
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
        byte[] pixels=new byte[256];
        java.util.Map<pl.crimeandlaw.territory.TerrainKey,pl.crimeandlaw.territory.TerrainMapTile> tiles=new java.util.HashMap<>();
        for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++){
            for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
                int wx=x*16+dx,wz=z*16+dz;
                var color=Math.abs(wx-14*Math.sin(wz/18.0))<5?net.minecraft.world.level.material.MapColor.WATER:wx>22?net.minecraft.world.level.material.MapColor.STONE:net.minecraft.world.level.material.MapColor.GRASS;
                pixels[dz*16+dx]=color.getPackedId(net.minecraft.world.level.material.MapColor.Brightness.NORMAL);
            }
            tiles.put(new pl.crimeandlaw.territory.TerrainKey("minecraft:overworld",x,z),pl.crimeandlaw.territory.TerrainMapTile.of(pixels));
        }
        TerrainClient.view=pl.crimeandlaw.territory.TerrainView.snapshot(atlas,"minecraft:overworld",0,0,key->key.z()!=-3,id->"Czarne Wilki",tiles::get);
        // An intentionally long synthetic inspector tests scrolling even though
        // the production empty-bonus copy is now shorter. Real village captures
        // below still use unmodified data from the dedicated server.
        var v=TerrainClient.view;var cells=new java.util.ArrayList<pl.crimeandlaw.territory.TerrainView.Cell>();
        for(var c:v.cells())cells.add(c.x()==1&&c.z()==-1?new pl.crimeandlaw.territory.TerrainView.Cell(
            c.x(),c.z(),c.status(),c.minHeight(),c.maxHeight(),c.samples(),c.buildingPieces(),c.biome(),
            java.util.EnumSet.allOf(pl.crimeandlaw.territory.TerrainProfile.Trait.class),c.source(),c.owner(),c.ownerName(),c.mapPixels()):c);
        TerrainClient.view=new pl.crimeandlaw.territory.TerrainView(v.dimension(),v.centerX(),v.centerZ(),v.radius(),v.playerX(),v.playerZ(),v.yaw(),cells);
    }
    private static void checkMapTexture(Minecraft mc){
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("crimeandlaw","terrain_map");
        var texture=(net.minecraft.client.renderer.texture.DynamicTexture)mc.getTextureManager().getTexture(id);
        var image=texture.getPixels();require(image.getWidth()==176&&image.getHeight()==176,"Map texture retains one pixel per world block");
        var view=TerrainClient.view;int count=0;
        for(var cell:view.cells()){
            if(cell.mapPixels().isEmpty())continue;
            int dx=(cell.x()-view.centerX()+5)*16,dz=(cell.z()-view.centerZ()+5)*16;
            byte[] colors=new pl.crimeandlaw.territory.TerrainMapTile(cell.mapPixels()).colors();
            for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                int expected=net.minecraft.world.level.material.MapColor.getColorFromPackedId(colors[z*16+x]);
                if((expected>>>24)!=0)require(image.getPixelRGBA(dx+x,dz+z)==expected,"Texture position, channels and real map color preserved");
            }count++;
        }
        require(count>=49,"Real world map has at least 49 captured chunks");
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
