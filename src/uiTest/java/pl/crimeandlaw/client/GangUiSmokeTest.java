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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import pl.crimeandlaw.gang.GangDirectory;

/** Real Minecraft renderer, fixture data, no server. Enabled only by -PuiSmoke. */
@EventBusSubscriber(modid="crimeandlaw", value=Dist.CLIENT)
public final class GangUiSmokeTest {
    private static final UUID PLAYER=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Path OUT=Path.of("../build/ui-smoke");
    private static int frames,stage=-1;
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
                case 3 -> {capture(mc,"04-gang");selectTab(1);}
                case 4 -> {capture(mc,"05-members");selectTab(2);}
                case 5 -> {capture(mc,"06-ranks");selectTab(4);}
                case 6 -> {
                    capture(mc,"07-settings");REPORT.append("PASS: actual Minecraft renderer, 7 screens, GUI scales 2 and 4, mouse/typing/tab checks.\n");
                    Files.writeString(OUT.resolve("result.txt"),REPORT.toString());
                    System.out.println("CRIME_UI_SMOKE_PASS");mc.stop();
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
    private static void selectTab(int tab)throws Exception {
        var field=GangScreen.class.getDeclaredField("tab");field.setAccessible(true);field.setInt(screen,tab);screen.updated();
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
            GangDirectory directory=new GangDirectory();var gang=directory.create(PLAYER,"Gracz","Czarne Wilki","");
            UUID friend=UUID.fromString("00000000-0000-0000-0000-000000000002");directory.request(friend,"DrugiGracz",gang.id);directory.accept(PLAYER,friend);
            state.add("gang",ClientState.JSON.toJsonTree(gang));
        }
        ClientState.state=state;ClientState.message="";
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
