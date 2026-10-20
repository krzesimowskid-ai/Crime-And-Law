package pl.crimeandlaw.client;

import com.google.gson.*;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.*;
import java.nio.file.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.*;
import pl.crimeandlaw.gang.GangDirectory.*;

public final class GangScreen extends Screen {
    private static final List<GangCategory> TABS=GangCategory.navigation();
    private final List<Label> labels=new ArrayList<>();
    private record Label(int x,int y,String value,int color,int width){}
    private int left,top,w,h,cx,cy,cw,tab,memberPage,listPage,bodyScroll,maxBodyScroll;
    private GangLayout layout=GangLayout.of(800,450);
    private boolean find,requests=true;
    private String query="",draftName="",file="",draftLogo="",selectedMember="";
    private String background="15161C",border="A3425D",lastGang="";
    private int opacity=235;
    private int settingsSection;
    private boolean previewTheme;
    private long logoRequest;
    public GangScreen(){super(Component.literal("Crime And Law"));}
    private Gang gang(){return ClientState.gang();}
    private boolean isLeader(){Gang g=gang();return g!=null&&g.owner.equals(ClientState.player());}
    private Theme theme(){if(previewTheme)return draftTheme();Gang g=gang();if(g==null)return Theme.DEFAULT;Theme local=PersonalTheme.get(g.id.toString());return local==null?g.theme:local;}
    Theme currentTheme(){return theme();}
    public void updated(){
        Gang g=gang();String id=g==null?"":g.id.toString();
        if(!id.equals(lastGang)){
            lastGang=id;tab=0;selectedMember="";memberPage=0;previewTheme=false;
            Theme t=theme();background=String.format("%06X",t.background());border=String.format("%06X",t.border());opacity=t.opacity();
            draftLogo="";
        }
        rebuildWidgets();
    }
    @Override protected void init(){
        layout=GangLayout.of(width,height);w=layout.panelWidth();h=layout.panelHeight();
        left=(layout.canvasWidth()-w)/2;top=(layout.canvasHeight()-h)/2;
        labels.clear();
        Gang g=gang();
        if(g!=null)tab=GangCategory.visibleOrOverview(tab).tabId();
        if(g==null){
            cx=left+20;cy=top+62;cw=w-40;
            int half=(w-2)/2;
            addRenderableWidget(new FlatButton(left+1,top+1,half,41,"Stwórz gang",b->{find=false;bodyScroll=0;rebuildWidgets();},this::theme,!find,true));
            addRenderableWidget(new FlatButton(left+1+half,top+1,half,41,"Znajdź gang",b->{find=true;memberPage=0;bodyScroll=0;ClientState.refresh(query,0);rebuildWidgets();},this::theme,find,true));
            if(find)buildFind();else buildCreate();
        }else{
            cx=left+116;cy=top+55;cw=w-130;
            button(left+w-25,top+10,16,"×",b->onClose());
            for(int i=0;i<TABS.size();i++){
                final GangCategory category=TABS.get(i);final int chosen=category.tabId();button(left+10,cy+i*24,96,category.label(),b->{tab=chosen;memberPage=0;bodyScroll=0;rebuildWidgets();},tab==chosen);
            }
            button(left+10,top+h-75,96,"Ustawienia",b->{tab=3;bodyScroll=0;rebuildWidgets();},tab==3);
            String leaveMessage=ClientState.memberCount()==1?"Ostatni członek opuszcza gang — gang zostanie usunięty.":isLeader()?"Lider musi najpierw przekazać przywództwo.":"Czy chcesz opuścić ten gang?";
            button(left+10,top+h-48,96,"Opuść gang",b->confirm("Opuścić gang?",leaveMessage,()->send("leave",new JsonObject())));
            switch(tab){case 1->buildMembers();case 2->buildMap();case 3->buildSettings();case 4->buildRegister();}
        }
        int bottom=cy;
        for(var child:children())if(child instanceof AbstractWidget widget && widget.getX()>=cx && widget.getY()>=cy)bottom=Math.max(bottom,widget.getY()+widget.getHeight());
        for(Label line:labels)bottom=Math.max(bottom,line.y()+10);
        maxBodyScroll=Math.max(0,bottom-(top+h-26));bodyScroll=Math.clamp(bodyScroll,0,maxBodyScroll);
        for(var child:children())if(child instanceof AbstractWidget widget && widget.getX()>=cx && widget.getY()>=cy){
            widget.setY(widget.getY()-bodyScroll);widget.visible=widget.getY()>=cy && widget.getY()+widget.getHeight()<=top+h-24;
        }
    }
    private void label(int x,int y,String text){label(x,y,text,0xFFE9E8EC);}
    private void label(int x,int y,String text,int color){labels.add(new Label(x,y,text,color,Math.max(10,left+w-12-x)));}
    private FlatButton button(int x,int y,int width,String text,Button.OnPress action){return button(x,y,width,text,action,false);}
    private FlatButton button(int x,int y,int width,String text,Button.OnPress action,boolean selected){
        return addRenderableWidget(new FlatButton(x,y,width,19,text,action,this::theme,selected));
    }
    private EditBox field(int x,int y,int width,int max,String value,java.util.function.Consumer<String> change){
        EditBox box=new EditBox(font,x,y,width,18,Component.literal("Pole tekstowe"));box.setMaxLength(max);box.setValue(value);box.setResponder(change);return addRenderableWidget(box);
    }
    private void send(String action,JsonObject args){ClientState.action(action,args);}
    private void target(String action,String id){JsonObject a=new JsonObject();a.addProperty("target",id);send(action,a);}
    private void confirm(String title,String description,Runnable action){
        minecraft.setScreen(new ConfirmScreen(yes->{minecraft.setScreen(this);if(yes)action.run();},Component.literal(title),Component.literal(description)));
    }
    private void buildCreate(){
        label(cx,cy,"Stwórz swój gang");label(cx,cy+23,"Nazwa (3–32 znaki)",0xFFAAAAB7);
        field(cx,cy+37,Math.min(cw-170,380),32,draftName,v->draftName=v);
        logoControls(cx,cy+73,Math.min(cw-170,400),true);
        button(cx,cy+202,190,"Utwórz gang",b->{
            JsonObject a=new JsonObject();a.addProperty("name",draftName);a.addProperty("logo",draftLogo);send("create",a);
        });
        label(cx,cy+234,"Logo jest opcjonalne. Możesz zmienić je później.",0xFFAAAAB7);
    }
    private void logoControls(int x,int y,int width,boolean creating){
        label(x,y,"Logo z komputera",0xFFAAAAB7);
        button(x,y+18,Math.min(160,width/2-4),"Wybierz obraz…",b->chooseLogoFile()).active=creating||isLeader();
        label(x,y+48,"lub wklej ścieżkę do obrazu:",0xFFAAAAB7);
        field(x,y+62,width,1024,file,v->file=v);
        button(x,y+88,Math.min(140,width),"Wczytaj i dopasuj",b->loadLogo()).active=creating||isLeader();
        label(x,y+115,"PNG / JPG / GIF / BMP • możesz też przeciągnąć plik na okno gry.",0xFFAAAAB7);
    }
    private void buildFind(){
        label(cx,cy,"Znajdź gang");int searchWidth=Math.max(50,cw-82);
        field(cx,cy+20,searchWidth,32,query,v->query=v);
        button(cx+searchWidth+6,cy+20,76,"Szukaj",b->{listPage=0;ClientState.refresh(query,0);});
        JsonArray rows=ClientState.state.has("gangs")?ClientState.state.getAsJsonArray("gangs"):new JsonArray();
        int available=Math.max(1,(h-145)/23);int start=memberPage*available;
        if(rows.isEmpty())label(cx,cy+52,"Nie znaleziono gangów.",0xFFAAAAB7);
        for(int i=start;i<Math.min(rows.size(),start+available);i++){
            JsonObject row=rows.get(i).getAsJsonObject();int y=cy+48+(i-start)*23;
            label(cx,y+5,font.plainSubstrByWidth(row.get("name").getAsString()+"  ("+row.get("members").getAsInt()+")",cw-94));
            boolean pending=row.get("requested").getAsBoolean();
            button(cx+cw-88,y,88,pending?"Wysłano":"Zgłoś się",b->{JsonObject a=new JsonObject();a.addProperty("id",row.get("id").getAsString());send("request",a);}).active=!pending;
        }
        int page=ClientState.state.has("page")?ClientState.state.get("page").getAsInt():0;
        int pages=ClientState.state.has("pages")?ClientState.state.get("pages").getAsInt():1;
        int y=top+h-48;
        button(cx,y,42,"‹",b->{if(memberPage>0){memberPage--;rebuildWidgets();}else if(page>0){memberPage=0;ClientState.refresh(query,page-1);}}).active=memberPage>0||page>0;
        label(cx+50,y+5,"Strona "+(page+1)+" / "+pages);
        button(cx+cw-42,y,42,"›",b->{if((memberPage+1)*available<rows.size()){memberPage++;rebuildWidgets();}else if(page+1<pages){memberPage=0;ClientState.refresh(query,page+1);}}).active=(memberPage+1)*available<rows.size()||page+1<pages;
    }
    private void buildMembers(){
        label(cx,cy,"Członkowie: "+ClientState.memberCount());
        buildPeople(false,false,cy+28);
    }
    private void buildPeople(boolean applications,boolean manage,int y){
        Gang g=gang();int listWidth=Math.min(180,(cw-12)/2),rx=cx+listWidth+16;
        List<UUID> ids=new ArrayList<>();
        if(applications)ids.addAll(g.requests.keySet());
        else if(ClientState.state.has("memberIds"))for(JsonElement id:ClientState.state.getAsJsonArray("memberIds"))ids.add(UUID.fromString(id.getAsString()));
        for(int i=0;i<ids.size();i++){
            UUID id=ids.get(i);Member member=g.members.get(id);
            String name=applications?g.requests.get(id):member==null?"":member.name;
            button(cx,y+i*23,listWidth,font.plainSubstrByWidth(name,listWidth-12),b->{selectedMember=id.toString();rebuildWidgets();},selectedMember.equals(id.toString()));
        }
        if(ids.isEmpty())label(cx,y+6,applications?"Brak zgłoszeń.":"Brak członków.",0xFFAAAAB7);
        if(!selectedMember.isBlank()&&ids.stream().anyMatch(id->id.toString().equals(selectedMember))){
            UUID selected=UUID.fromString(selectedMember);
            if(applications){
                label(rx,y+5,g.requests.get(selected));
                if(manage){
                    button(rx,y+28,cw-listWidth-16,"Przyjmij",b->target("accept",selectedMember));
                    button(rx,y+52,cw-listWidth-16,"Odrzuć",b->target("reject",selectedMember));
                }
            }else{
                Member member=g.members.get(selected);label(rx,y+5,member.name);
                label(rx,y+23,g.owner.equals(selected)?"Lider gangu":"Członek gangu",0xFFAAAAB7);
                if(manage){
                    button(rx,y+51,cw-listWidth-16,"Usuń z gangu",b->confirm("Usunąć członka?",member.name,()->target("kick",selectedMember))).active=!g.owner.equals(selected);
                    button(rx,y+75,cw-listWidth-16,"Przekaż przywództwo",b->confirm("Przekazać przywództwo?","Nowy lider: "+member.name,()->target("transfer",selectedMember))).active=!g.owner.equals(selected);
                }
            }
        }
        String key=applications?"request":"member";int page=ClientState.number(key+"Page",0),pages=ClientState.number(key+"Pages",1);
        int bottom=top+h-48;
        button(cx,bottom,42,"‹",b->{selectedMember="";ClientState.listPage(applications,page-1);}).active=page>0;
        label(cx+50,bottom+5,"Strona "+(page+1)+" / "+pages);
        button(cx+listWidth-42,bottom,42,"›",b->{selectedMember="";ClientState.listPage(applications,page+1);}).active=page+1<pages;
    }
    private void buildRegister(){
        label(cx,cy,"Rejestr gangu");
        JsonArray events=ClientState.state.has("history")?ClientState.state.getAsJsonArray("history"):new JsonArray();
        var format=java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(java.time.ZoneId.systemDefault());
        if(events.isEmpty())label(cx,cy+30,"Brak wpisów. Nowe wydarzenia pojawią się tutaj.",0xFFAAAAB7);
        for(int i=0;i<events.size();i++){
            JsonObject event=events.get(i).getAsJsonObject();int y=cy+28+i*25;
            String date=format.format(java.time.Instant.ofEpochMilli(event.get("timestamp").getAsLong()));
            label(cx,y+4,date,0xFF8E869A);
            String text=event.get("text").getAsString(),first=font.plainSubstrByWidth(text,cw-94);
            label(cx+90,y,first);
            if(first.length()<text.length())label(cx+90,y+11,text.substring(first.length()).strip());
        }
        int page=ClientState.number("historyPage",0),pages=ClientState.number("historyPages",1),y=top+h-48;
        button(cx,y,42,"‹",b->ClientState.historyPage(page-1)).active=page>0;
        label(cx+50,y+5,"Strona "+(page+1)+" / "+pages);
        button(cx+cw-42,y,42,"›",b->ClientState.historyPage(page+1)).active=page+1<pages;
    }
    private void buildMap(){
        label(cx,cy,"Mapa — kolejny etap");label(cx,cy+26,"Tu pojawią się chunki i tereny gangów.",0xFFAAAAB7);
        label(cx,cy+47,"Planowane: ręczne oznaczenia prywatne i wspólne.",0xFFAAAAB7);
        label(cx,cy+68,"W tej wersji nie ma jeszcze przejmowania ziemi.",0xFFAAAAB7);
    }
    private Theme draftTheme(){return new Theme(Integer.parseInt(background.replace("#",""),16),Integer.parseInt(border.replace("#",""),16),opacity);}
    private void buildSettings(){
        if(!isLeader()&&settingsSection==2)settingsSection=0;
        button(cx,cy,110,"Wygląd",b->{settingsSection=0;bodyScroll=0;rebuildWidgets();},settingsSection==0);
        button(cx+118,cy,110,"Logo",b->{settingsSection=1;bodyScroll=0;rebuildWidgets();},settingsSection==1);
        if(isLeader())button(cx+236,cy,122,"Zarządzanie",b->{settingsSection=2;bodyScroll=0;selectedMember="";rebuildWidgets();},settingsSection==2);
        int y=cy+42,half=(cw-12)/2;
        if(settingsSection==0){
            label(cx,y,"Kolory i przezroczystość");
            label(cx,y+20,"Zobacz podgląd, a potem wybierz, komu zapisać wygląd.",0xFFAAAAB7);
            button(cx,y+48,half,"Zmień kolor tła…",b->openColor(true));
            button(cx+half+12,y+48,half,"Zmień obramówkę…",b->openColor(false));
            label(cx,y+80,"Krycie tła: "+Math.round(opacity/255.0*100)+"%",0xFFAAAAB7);
            label(cx+half+12,y+80,"Kolor akcentów menu",0xFFAAAAB7);
            label(cx,y+113,"Zapis wyglądu");
            button(cx,y+134,half,"Zapisz tylko u mnie",b->{
                if(PersonalTheme.set(gang().id.toString(),draftTheme())){previewTheme=false;ClientState.message="Zapisano osobisty wygląd.";}
            });
            button(cx+half+12,y+134,half,"Zapisz dla gangu",b->{
                Theme t=draftTheme();JsonObject a=new JsonObject();a.addProperty("background",t.background());a.addProperty("border",t.border());a.addProperty("opacity",t.opacity());send("theme",a);
            }).active=isLeader();
            button(cx,y+161,cw,"Używaj wyglądu gangu",b->{
                if(PersonalTheme.set(gang().id.toString(),null)){previewTheme=false;Theme t=gang().theme;background=String.format("%06X",t.background());border=String.format("%06X",t.border());opacity=t.opacity();rebuildWidgets();}
            });
        }else if(settingsSection==1){
            logoControls(cx,y,Math.min(cw-160,350),false);
            button(cx,y+151,Math.min(cw,230),"Zapisz logo gangu",b->{JsonObject a=new JsonObject();a.addProperty("logo",draftLogo);send("logo",a);}).active=isLeader()&&!draftLogo.isBlank();
            label(cx,y+183,"Zmiana logo będzie widoczna dla całego gangu.",0xFFAAAAB7);
        }else{
            button(cx,cy+34,120,"Zgłoszenia: "+ClientState.number("requestCount",0),b->{requests=true;selectedMember="";rebuildWidgets();},requests);
            button(cx+128,cy+34,120,"Skład gangu",b->{requests=false;selectedMember="";rebuildWidgets();},!requests);
            button(cx+cw-130,cy+34,130,"Odśwież dane",b->ClientState.refresh(query,listPage));
            buildPeople(requests,true,cy+65);
        }
    }
    private void openColor(boolean backgroundColor){
        Theme t=draftTheme();minecraft.setScreen(new ColorPickerScreen(this,backgroundColor?"Kolor i krycie tła":"Kolor obramówki",backgroundColor?t.background():t.border(),backgroundColor?opacity:255,backgroundColor,(rgb,alpha)->{
            if(backgroundColor){background=String.format("%06X",rgb);opacity=alpha;}else border=String.format("%06X",rgb);
            previewTheme=true;ClientState.message="Podgląd wyglądu. Wybierz sposób zapisu.";
        }));
    }
    private void chooseLogoFile(){
        try(MemoryStack stack=MemoryStack.stackPush()){
            var filters=stack.pointers(stack.UTF8("*.png"),stack.UTF8("*.jpg"),stack.UTF8("*.jpeg"),stack.UTF8("*.gif"),stack.UTF8("*.bmp"));
            String path=TinyFileDialogs.tinyfd_openFileDialog("Wybierz logo gangu",null,filters,"Obrazy PNG / JPG / GIF / BMP",false);
            if(path!=null){file=path;loadLogo();}
        }catch(Exception e){ClientState.message="Nie można otworzyć okna wyboru. Przeciągnij obraz albo wklej jego ścieżkę.";}
    }
    @Override public void onFilesDrop(List<Path> files){
        if(files.size()!=1){ClientState.message="Przeciągnij jeden obraz naraz.";return;}
        if(gang()!=null&&!isLeader()){ClientState.message="Nie masz uprawnień do zmiany logo gangu.";return;}
        file=files.getFirst().toString();if(gang()!=null){tab=3;settingsSection=1;}else find=false;bodyScroll=0;rebuildWidgets();loadLogo();
    }
    private void loadLogo(){
        if(gang()!=null&&!isLeader()){ClientState.message="Nie masz uprawnień do zmiany logo gangu.";return;}
        final long request=++logoRequest;final String path=file.strip().replaceAll("^\"|\"$","");
        if(path.isBlank()){ClientState.message="Wybierz obraz, przeciągnij plik albo wklej ścieżkę.";return;}
        ClientState.message="Wczytywanie obrazu…";
        java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try{return LogoImages.read(Path.of(path));}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}
        }).whenComplete((image,error)->Minecraft.getInstance().execute(()->{
            if(request!=logoRequest||minecraft.screen!=this)return;
            if(error!=null){
                Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();
                ClientState.message="Nie wczytano logo: "+(cause.getMessage()==null?cause.getClass().getSimpleName():cause.getMessage());rebuildWidgets();
            }else{
                ClientState.message="";minecraft.setScreen(new LogoCropScreen(this,image,Path.of(path).getFileName().toString(),encoded->{
                    draftLogo=encoded;ClientState.message="Logo przygotowane. "+(gang()==null?"Utwórz gang, aby je zapisać.":"Kliknij Zapisz logo gangu.");rebuildWidgets();
                }));
            }
        }));
    }
    // Vanilla Screen.render invokes renderBackground before widgets. The old custom
    // labels were drawn before that call and were blurred by the background pass.
    @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float delta){}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta){
        // A flat screen must not inherit depth from a previous GUI frame/resize.
        // Vanilla's background effect used to reset that state implicitly.
        g.flush();RenderSystem.depthMask(true);RenderSystem.clear(256,Minecraft.ON_OSX);RenderSystem.disableDepthTest();
        g.pose().pushPose();g.pose().scale(layout.scale(),layout.scale(),1);
        try{paint(g,(int)layout.local(mouseX),(int)layout.local(mouseY),delta);}
        finally{g.pose().popPose();}
    }
    private void clip(GuiGraphics g,int x1,int y1,int x2,int y2){
        // Absolute GUI coordinates work both with and without pose-aware scissoring.
        g.pose().pushPose();g.pose().last().pose().identity();
        g.enableScissor((int)Math.floor(x1*layout.scale()),(int)Math.floor(y1*layout.scale()),
                (int)Math.ceil(x2*layout.scale()),(int)Math.ceil(y2*layout.scale()));
        g.pose().popPose();
    }
    private void paint(GuiGraphics g,int mouseX,int mouseY,float delta){
        g.fill(0,0,layout.canvasWidth(),layout.canvasHeight(),0x55000000);
        g.fill(left+3,top+3,left+w+3,top+h+3,0x44000000);
        Theme t=theme();g.fill(left,top,left+w,top+h,t.backgroundArgb());
        int header=(t.opacity()<<24)|mix(t.background(),t.border(),0.18f);
        g.fillGradient(left+1,top+1,left+w-1,top+42,header,t.backgroundArgb());
        g.renderOutline(left,top,w,h,t.borderArgb());g.fill(left+1,top+42,left+w-1,top+43,t.borderArgb());
        for(int offset:new int[]{0,w-14}){
            g.fill(left+offset,top,left+offset+14,top+2,t.borderArgb());
            g.fill(left+offset,top+h-2,left+offset+14,top+h,t.borderArgb());
        }
        Gang gang=gang();String logo=!draftLogo.isBlank()?draftLogo:gang==null?"":gang.logo;
        var texture=LogoTexture.get(logo);
        if(gang!=null){
            g.fill(left+3,top+45,left+107,top+h-24,0x16000000);
            g.fill(left+108,top+43,left+109,top+h-22,t.borderArgb());
            g.fill(left+46,top+7,left+w-35,top+36,0x28000000);
            g.fill(left+46,top+7,left+48,top+36,t.borderArgb());
            if(texture!=null)g.blit(texture,left+10,top+8,0,0,28,28,28,28);
            else{g.renderOutline(left+10,top+8,28,28,t.borderArgb());g.drawCenteredString(font,"G",left+24,top+18,t.borderArgb());}
            g.drawString(font,font.plainSubstrByWidth(gang.name,w-106),left+54,top+10,0xFFF0EDF1,false);
            String subtitle=isLeader()?"Lider gangu":"Członek gangu";
            g.drawString(font,font.plainSubstrByWidth(subtitle,w-106),left+54,top+25,0xFFAAAAB7,false);
        }
        if(gang==null&&!find || gang!=null&&tab==3&&settingsSection==1){
            int previewX=cx+cw-140,previewY=cy+(gang==null?24:55);
            g.fill(previewX,previewY,previewX+140,previewY+148,0x20000000);g.renderOutline(previewX,previewY,140,148,0x66000000|t.border());
            g.drawCenteredString(font,"PODGLĄD LOGO",previewX+70,previewY+12,0xFFAAA7B6);
            if(texture!=null){g.pose().pushPose();g.pose().translate(previewX+22,previewY+32,0);g.pose().scale(1.5f,1.5f,1);g.blit(texture,0,0,0,0,64,64,64,64);g.pose().popPose();}
            else{g.renderOutline(previewX+38,previewY+48,64,64,t.borderArgb());g.drawCenteredString(font,"+",previewX+70,previewY+74,t.borderArgb());}
        }
        if(gang!=null&&tab==3&&settingsSection==0){
            int half=(cw-12)/2,y=cy+42;
            g.fill(cx,y+70,cx+half,y+75,(opacity<<24)|Integer.parseInt(background,16));
            g.fill(cx+half+12,y+70,cx+cw,y+75,0xFF000000|Integer.parseInt(border,16));
            g.fill(cx,y+104,cx+cw,y+105,0x44000000|t.border());
        }
        if(gang!=null&&tab==0)drawGangOverview(g,gang,texture);
        clip(g,cx,cy,cx+cw,top+h-24);
        for(Label line:labels)g.drawString(font,font.plainSubstrByWidth(line.value(),line.width()),line.x(),line.y()-bodyScroll,line.color(),false);
        g.disableScissor();
        super.render(g,mouseX,mouseY,delta);
        if(gang==null)g.fill(left+w/2,top+1,left+w/2+1,top+42,t.borderArgb());
        String message=ClientState.message;g.drawString(font,font.plainSubstrByWidth(message,w-20),left+10,top+h-14,0xFFD4C4CC,false);
        if(!message.isBlank()&&mouseY>=top+h-22&&mouseY<top+h && font.width(message)>w-20){
            var wrapped=font.split(Component.literal(message),w-28);int tipY=top+h-28-wrapped.size()*11;
            g.fill(left+6,tipY-4,left+w-6,top+h-24,0xFF181820);
            for(int i=0;i<wrapped.size();i++)g.drawString(font,wrapped.get(i),left+12,tipY+i*11,0xFFE9E8EC,false);
        }
    }
    private void drawGangOverview(GuiGraphics g,Gang gang,net.minecraft.resources.ResourceLocation logo){
        int center=cx+cw/2,logoY=cy,accent=theme().borderArgb();
        if(logo!=null)g.blit(logo,center-32,logoY,0,0,64,64,64,64);
        else{
            g.fill(center-32,logoY,center+32,logoY+64,0x18000000);
            g.renderOutline(center-32,logoY,64,64,accent);
            centeredText(g,"G",center,logoY+12,4.5f,accent);
        }
        var lines=font.split(Component.literal(gang.name),cw-48);
        for(int i=0;i<lines.size();i++)g.drawCenteredString(font,lines.get(i),center,cy+80+i*11,0xFFF2EDF3);
        // The identity area spans the content pane, matching the user's sketch.
        g.fill(left+109,cy+110,left+w-1,cy+111,accent);
        g.drawCenteredString(font,"Informacje o gangu",center,cy+126,0xFFE8E3ED);
        int gap=14,tileWidth=(cw-24-gap*2)/3;
        int x=cx+(cw-(tileWidth*3+gap*2))/2,y=cy+152;
        Member leader=gang.members.get(gang.owner);
        informationTile(g,x,y,tileWidth,"Członkowie",Integer.toString(ClientState.memberCount()),"");
        informationTile(g,x+tileWidth+gap,y,tileWidth,"Lider gangu",leader==null?"—":leader.name,"");
        informationTile(g,x+2*(tileWidth+gap),y,tileWidth,"Poziom gangu","—","Wkrótce");
    }
    private void informationTile(GuiGraphics g,int x,int y,int width,String label,String value,String detail){
        int center=x+width/2,accent=theme().border();
        g.fill(x,y,x+width,y+64,0x18000000);
        g.renderOutline(x,y,width,64,0x55000000|accent);
        g.drawCenteredString(font,label,center,y+10,0xFFAAA3B3);
        float scale=1.35f;
        String fitted=font.plainSubstrByWidth(value,(int)((width-16)/scale));
        centeredText(g,fitted,center,y+29,scale,0xFFEFE9F3);
        if(!detail.isEmpty())g.drawCenteredString(font,detail,center,y+49,0xFF8E869A);
    }
    private void centeredText(GuiGraphics g,String text,int center,int y,float scale,int color){
        g.pose().pushPose();
        g.pose().translate(center-font.width(text)*scale/2,y,0);g.pose().scale(scale,scale,1);
        g.drawString(font,text,0,0,color,false);g.pose().popPose();
    }
    private static int mix(int a,int b,float ratio){int r=(int)((a>>16&255)*(1-ratio)+(b>>16&255)*ratio),g=(int)((a>>8&255)*(1-ratio)+(b>>8&255)*ratio),blue=(int)((a&255)*(1-ratio)+(b&255)*ratio);return r<<16|g<<8|blue;}
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(layout.local(x),layout.local(y),button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(layout.local(x),layout.local(y),button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        return super.mouseDragged(layout.local(x),layout.local(y),button,layout.local(dx),layout.local(dy));
    }
    @Override public void mouseMoved(double x,double y){super.mouseMoved(layout.local(x),layout.local(y));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        x=layout.local(x);y=layout.local(y);
        if(x>=cx && x<cx+cw && y>=cy && y<top+h-22){
            bodyScroll=Math.clamp(bodyScroll-(int)(vertical*18),0,maxBodyScroll);rebuildWidgets();return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean isPauseScreen(){return false;}
}

