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
    private static GangDirectory directory;
    private static final UUID FRIEND=new UUID(0,2);
    private static GangScreen screen;
    private static final StringBuilder REPORT=new StringBuilder();

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
        if(!Boolean.getBoolean("crimeandlaw.uiSmoke"))return;
        Minecraft mc=Minecraft.getInstance();
        try {
            if(stage<0) {
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
                    capture(mc,"04-gang");checkOverview(mc);
                    require(screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Rangi")),"Ranks removed");
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
                    capture(mc,"10-color-gradients");checkGradients(mc);
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
                    capture(mc,"11-appearance-preview");click(mc,"Logo");
                    var image=new java.awt.image.BufferedImage(512,256,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<256;y++)for(int x=0;x<512;x++)image.setRGB(x,y,0xFF000000|(x/2)<<16|(y<<8)|150);
                    Path png=OUT.resolve("logo-fixture.png").toAbsolutePath();Files.write(png,LogoImages.png(image));
                    screen.onFilesDrop(java.util.List.of(png));
                }
                case 12 -> {
                    capture(mc,"12-logo-crop");LogoCropScreen dialog=(LogoCropScreen)mc.screen;double scale=dialog.layout.scale();
                    double x=(dialog.left+110)*scale,y=(dialog.top+145)*scale;
                    require(dialog.mouseClicked(x,y,0),"Crop click");dialog.mouseDragged(x+30*scale,y+10*scale,0,30*scale,10*scale);
                    dialog.mouseReleased(x+30*scale,y+10*scale,0);dialog.mouseScrolled(x,y,0,2);click(mc,"Zastosuj logo");
                    var field=GangScreen.class.getDeclaredField("draftLogo");field.setAccessible(true);String encoded=(String)field.get(screen);
                    require(!encoded.isBlank()&&!pl.crimeandlaw.gang.GangLogo.validate(encoded).isBlank(),"Dropped image cropped and encoded");
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
                    capture(mc,"15-overview-logo-long-name-scale2");checkOverview(mc);
                    directory.of(PLAYER).name="Czarne Wilki";fixturePage(PLAYER,0);
                    screen=new GangScreen();mc.setScreen(screen);screen.updated();selectTab(4);
                }
                case 16 -> {
                    capture(mc,"16-register");
                    require(ClientState.state.getAsJsonArray("history").size()==8,"Register receives a bounded page of real events");
                    require(widget(screen,"›").active,"History next page is available");
                    require(screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("Transport")),"Transport has no menu entry");
                    var l=GangLayout.of(screen.width,screen.height);ScrollPosition position=historyPosition();
                    require(position.maximum()>0,"History page has scrollable content");
                    int footerY=widget(screen,"›").getY();
                    screen.mouseScrolled(400*l.scale(),180*l.scale(),0,-30);
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
                    require(historyPosition().offset()==0&&!widget(screen,"Przewijanie rejestru").active,"Short second page needs no scrolling");
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
                    REPORT.append("PASS: actual Minecraft renderer, 20 captures, register wheel/drag/keyboard scrolling, fixed header and pagination, page reset and GUI resize, visible register, hidden transport gate, GUI scales 2/4, typing, wide identity header with divider and three information columns, long gang name and uploaded logo, paged members, leader management, ordinary member restrictions, vertical gradient pixels, keyboard/drag color input, opacity, file-drop handler and cropping.\n");
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
    private static void checkCreation(Minecraft mc) {
        var l=GangLayout.of(screen.width,screen.height);
        var boxes=screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).toList();
        require(boxes.size()==2,"Name/logo fields");EditBox name=boxes.getFirst();
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
    private static void selectTab(int tab)throws Exception {
        var field=GangScreen.class.getDeclaredField("tab");field.setAccessible(true);field.setInt(screen,tab);screen.updated();
    }
    private static AbstractWidget widget(Screen screen,String prefix){
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
