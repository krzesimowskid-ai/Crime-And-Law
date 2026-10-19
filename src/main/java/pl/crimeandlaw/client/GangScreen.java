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
    private static final String[] TABS={"Gang","Członkowie","Rangi","Mapa","Ustawienia"};
    private static final String[] RIGHTS={"Zapraszanie / zgłoszenia","Usuwanie członków","Zarządzanie rangami","Wygląd i logo gangu"};
    private final List<Label> labels=new ArrayList<>();
    private record Label(int x,int y,String value,int color,int width){}
    private int left,top,w,h,cx,cy,cw,tab,memberPage,listPage,bodyScroll,maxBodyScroll;
    private GangLayout layout=GangLayout.of(800,450);
    private boolean find,requests;
    private String query="",draftName="",file="",draftLogo="",selectedMember="",selectedRole="member",roleName="";
    private String background="15161C",border="A3425D",lastGang="";
    private int opacity=235;
    private int settingsSection;
    private boolean previewTheme;
    private long logoRequest;
    private String draggedRole="";
    private double dragStartY,dragY;
    private boolean roleMoved;
    private final Map<String,FlatButton> roleButtons=new LinkedHashMap<>();
    private EnumSet<Permission> rolePermissions=EnumSet.noneOf(Permission.class);
    public GangScreen(){super(Component.literal("Crime And Law"));}
    private Gang gang(){return ClientState.gang();}
    private boolean can(Permission p){
        Gang g=gang();if(g==null)return false;if(g.owner.equals(ClientState.player()))return true;
        Member m=g.members.get(ClientState.player());Role role=m==null?null:g.roles.get(m.role);
        return role!=null && role.permissions.contains(p);
    }
    private Theme theme(){if(previewTheme)return draftTheme();Gang g=gang();if(g==null)return Theme.DEFAULT;Theme local=PersonalTheme.get(g.id.toString());return local==null?g.theme:local;}
    Theme currentTheme(){return theme();}
    public void updated(){
        Gang g=gang();String id=g==null?"":g.id.toString();
        if(!id.equals(lastGang)){
            lastGang=id;tab=0;selectedMember="";selectedRole="member";memberPage=0;roleName="";previewTheme=false;
            Theme t=theme();background=String.format("%06X",t.background());border=String.format("%06X",t.border());opacity=t.opacity();
            draftLogo="";
        }
        rebuildWidgets();
    }
    @Override protected void init(){
        layout=GangLayout.of(width,height);w=layout.panelWidth();h=layout.panelHeight();
        left=(layout.canvasWidth()-w)/2;top=(layout.canvasHeight()-h)/2;
        labels.clear();roleButtons.clear();
        Gang g=gang();
        if(g==null){
            cx=left+20;cy=top+62;cw=w-40;
            int half=(w-2)/2;
            addRenderableWidget(new FlatButton(left+1,top+1,half,41,"Stwórz gang",b->{find=false;bodyScroll=0;rebuildWidgets();},this::theme,!find,true));
            addRenderableWidget(new FlatButton(left+1+half,top+1,half,41,"Znajdź gang",b->{find=true;memberPage=0;bodyScroll=0;ClientState.refresh(query,0);rebuildWidgets();},this::theme,find,true));
            if(find)buildFind();else buildCreate();
        }else{
            cx=left+116;cy=top+55;cw=w-130;
            button(left+w-25,top+10,16,"×",b->onClose());
            for(int i=0;i<TABS.length;i++){
                final int chosen=i;button(left+10,cy+i*24,96,TABS[i],b->{tab=chosen;memberPage=0;bodyScroll=0;rebuildWidgets();},tab==i);
            }
            button(left+10,top+h-48,96,"Opuść gang",b->confirm("Opuścić gang?",g.members.size()==1?"Ostatni członek opuszcza gang — gang zostanie usunięty.":"Lider musi najpierw przekazać przywództwo.",()->send("leave",new JsonObject())));
            switch(tab){case 0->buildOverview();case 1->buildMembers();case 2->buildRoles();case 3->buildMap();case 4->buildSettings();}
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
        button(x,y+18,Math.min(160,width/2-4),"Wybierz obraz…",b->chooseLogoFile()).active=creating||can(Permission.MANAGE_THEME);
        label(x,y+48,"lub wklej ścieżkę do obrazu:",0xFFAAAAB7);
        field(x,y+62,width,1024,file,v->file=v);
        button(x,y+88,Math.min(140,width),"Wczytaj i dopasuj",b->loadLogo()).active=creating||can(Permission.MANAGE_THEME);
        label(x,y+115,"PNG / JPG • możesz też przeciągnąć plik na okno gry.",0xFFAAAAB7);
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
    private void buildOverview(){
        Gang g=gang();label(cx,cy,g.name);label(cx,cy+24,"Członkowie: "+g.members.size()+" / "+GangDirectory.MAX_MEMBERS);
        label(cx,cy+44,"Rangi: "+g.roles.size());
        Member leader=g.members.get(g.owner);label(cx,cy+64,"Lider: "+leader.name);
        label(cx,cy+98,"Zgłoszenia do gangu wymagają akceptacji.",0xFFAAAAB7);
        label(cx,cy+118,"Członkostwo i ustawienia zapisuje serwer.",0xFFAAAAB7);
        if(can(Permission.INVITE))button(cx,cy+148,Math.min(cw,210),"Zgłoszenia: "+g.requests.size(),b->{tab=1;requests=true;rebuildWidgets();});
        button(cx,cy+180,Math.min(cw,160),"Odśwież dane",b->ClientState.refresh(query,listPage));
    }
    private void buildMembers(){
        Gang g=gang();label(cx,cy,requests?"Zgłoszenia":"Członkowie");
        if(can(Permission.INVITE))button(cx+Math.max(0,cw-120),cy-3,120,requests?"Pokaż członków":"Pokaż zgłoszenia",b->{requests=!requests;selectedMember="";memberPage=0;rebuildWidgets();});
        int listWidth=Math.min(160,(cw-12)/2),rx=cx+listWidth+12;
        List<UUID> ids=new ArrayList<>(requests?g.requests.keySet():g.members.keySet());int visible=Math.max(1,(h-150)/23);
        for(int i=memberPage*visible;i<Math.min(ids.size(),(memberPage+1)*visible);i++){
            UUID id=ids.get(i);String name=requests?g.requests.get(id):g.members.get(id).name;int y=cy+28+(i-memberPage*visible)*23;
            button(cx,y,listWidth,name,b->{selectedMember=id.toString();rebuildWidgets();},selectedMember.equals(id.toString()));
        }
        if(ids.isEmpty())label(cx,cy+32,requests?"Brak zgłoszeń.":"Brak członków.",0xFFAAAAB7);
        if(!selectedMember.isBlank()){
            UUID selected=UUID.fromString(selectedMember);
            if(requests && g.requests.containsKey(selected)){
                label(rx,cy+32,g.requests.get(selected));
                button(rx,cy+56,cw-listWidth-12,"Przyjmij",b->target("accept",selectedMember)).active=can(Permission.INVITE);
                button(rx,cy+80,cw-listWidth-12,"Odrzuć",b->target("reject",selectedMember)).active=can(Permission.INVITE);
            }else if(!requests && g.members.containsKey(selected)){
                Member m=g.members.get(selected);label(rx,cy+32,m.name);label(rx,cy+50,"Ranga: "+g.roles.get(m.role).name,0xFFAAAAB7);
                List<Role> roles=new ArrayList<>(g.roles.values());
                button(rx,cy+74,cw-listWidth-12,"Zmień rangę ›",b->{
                    List<Role> assignable=roles.stream().filter(r->!r.id.equals("leader")).toList();
                    int index=0;for(int i=0;i<assignable.size();i++)if(assignable.get(i).id.equals(m.role))index=i;
                    JsonObject a=new JsonObject();a.addProperty("target",selectedMember);a.addProperty("role",assignable.get((index+1)%assignable.size()).id);send("assign",a);
                }).active=can(Permission.MANAGE_ROLES)&&!g.owner.equals(selected);
                button(rx,cy+100,cw-listWidth-12,"Usuń z gangu",b->confirm("Usunąć członka?",m.name,()->target("kick",selectedMember))).active=can(Permission.MANAGE_MEMBERS)&&!g.owner.equals(selected)&&!selected.equals(ClientState.player());
                button(rx,cy+126,cw-listWidth-12,"Przekaż przywództwo",b->confirm("Przekazać przywództwo?","Nowy lider: "+m.name,()->target("transfer",selectedMember))).active=g.owner.equals(ClientState.player())&&!g.owner.equals(selected);
            }
        }
        pager(visible,ids.size());
    }
    private void pager(int visible,int total){
        int y=top+h-48;button(cx,y,42,"‹",b->{memberPage--;rebuildWidgets();}).active=memberPage>0;
        label(cx+50,y+5,"Lista "+(memberPage+1)+" / "+Math.max(1,(total+visible-1)/visible));
        button(cx+cw-42,y,42,"›",b->{memberPage++;rebuildWidgets();}).active=(memberPage+1)*visible<total;
    }
    private void buildRoles(){
        Gang g=gang();label(cx,cy,"Rangi i uprawnienia");int listWidth=Math.min(150,(cw-12)/2),rx=cx+listWidth+12;
        int index=0;for(Role r:g.roles.values()){
            var row=button(cx,cy+26+index++*22,listWidth,"≡  "+r.name,b->{selectedRole=r.id;roleName=r.name;rolePermissions=r.permissions.isEmpty()?EnumSet.noneOf(Permission.class):EnumSet.copyOf(r.permissions);rebuildWidgets();},selectedRole.equals(r.id));
            roleButtons.put(r.id,row);
        }
        button(cx,Math.max(top+h-48,cy+30+g.roles.size()*22),listWidth,"+ Nowa ranga",b->{selectedRole="";roleName="";rolePermissions.clear();rebuildWidgets();}).active=can(Permission.MANAGE_ROLES)&&g.roles.size()<12;
        Role current=g.roles.get(selectedRole);
        if(roleName.isBlank()&&current!=null){roleName=current.name;rolePermissions=current.permissions.isEmpty()?EnumSet.noneOf(Permission.class):EnumSet.copyOf(current.permissions);}
        field(rx,cy+26,cw-listWidth-12,24,roleName,v->roleName=v);
        int row=0;for(Permission permission:Permission.values()){
            int position=row;button(rx,cy+54+row++*23,cw-listWidth-12,(rolePermissions.contains(permission)?"[x] ":"[ ] ")+RIGHTS[position],b->{
                if(!rolePermissions.remove(permission))rolePermissions.add(permission);rebuildWidgets();
            }).active=can(Permission.MANAGE_ROLES)&&!selectedRole.equals("leader");
        }
        button(rx,cy+154,cw-listWidth-12,"Zapisz rangę",b->{JsonObject a=new JsonObject();a.addProperty("role",selectedRole);a.addProperty("name",roleName);
            for(Permission p:Permission.values())a.addProperty(p.name(),rolePermissions.contains(p));send("role",a);
        }).active=can(Permission.MANAGE_ROLES)&&!selectedRole.equals("leader");
        label(rx,cy+188,"Przeciągaj rangi po lewej, aby je uporządkować.",0xFFAAAAB7);
        label(rx,cy+204,"Pozycja na liście nie zmienia uprawnień.",0xFFAAAAB7);
    }
    private void buildMap(){
        label(cx,cy,"Mapa — kolejny etap");label(cx,cy+26,"Tu pojawią się chunki i tereny gangów.",0xFFAAAAB7);
        label(cx,cy+47,"Planowane: ręczne oznaczenia prywatne i wspólne.",0xFFAAAAB7);
        label(cx,cy+68,"W tej wersji nie ma jeszcze przejmowania ziemi.",0xFFAAAAB7);
    }
    private Theme draftTheme(){return new Theme(Integer.parseInt(background.replace("#",""),16),Integer.parseInt(border.replace("#",""),16),opacity);}
    private void buildSettings(){
        button(cx,cy,110,"Wygląd",b->{settingsSection=0;bodyScroll=0;rebuildWidgets();},settingsSection==0);
        button(cx+118,cy,110,"Logo",b->{settingsSection=1;bodyScroll=0;rebuildWidgets();},settingsSection==1);
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
            }).active=can(Permission.MANAGE_THEME);
            button(cx,y+161,cw,"Używaj wyglądu gangu",b->{
                if(PersonalTheme.set(gang().id.toString(),null)){previewTheme=false;Theme t=gang().theme;background=String.format("%06X",t.background());border=String.format("%06X",t.border());opacity=t.opacity();rebuildWidgets();}
            });
        }else{
            logoControls(cx,y,Math.min(cw-160,350),false);
            button(cx,y+151,Math.min(cw,230),"Zapisz logo gangu",b->{JsonObject a=new JsonObject();a.addProperty("logo",draftLogo);send("logo",a);}).active=can(Permission.MANAGE_THEME)&&!draftLogo.isBlank();
            label(cx,y+183,"Zmiana logo będzie widoczna dla całego gangu.",0xFFAAAAB7);
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
            var filters=stack.pointers(stack.UTF8("*.png"),stack.UTF8("*.jpg"),stack.UTF8("*.jpeg"));
            String path=TinyFileDialogs.tinyfd_openFileDialog("Wybierz logo gangu",null,filters,"Obrazy PNG / JPG",false);
            if(path!=null){file=path;loadLogo();}
        }catch(Exception e){ClientState.message="Nie można otworzyć okna wyboru. Przeciągnij obraz albo wklej jego ścieżkę.";}
    }
    @Override public void onFilesDrop(List<Path> files){
        if(files.size()!=1){ClientState.message="Przeciągnij jeden obraz naraz.";return;}
        if(gang()!=null&&!can(Permission.MANAGE_THEME)){ClientState.message="Nie masz uprawnień do zmiany logo gangu.";return;}
        file=files.getFirst().toString();if(gang()!=null){tab=4;settingsSection=1;}else find=false;bodyScroll=0;rebuildWidgets();loadLogo();
    }
    private void loadLogo(){
        if(gang()!=null&&!can(Permission.MANAGE_THEME)){ClientState.message="Nie masz uprawnień do zmiany logo gangu.";return;}
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
        g.flush();RenderSystem.clear(256,Minecraft.ON_OSX);RenderSystem.disableDepthTest();
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
            Member me=gang.members.get(ClientState.player());String subtitle="Twoja ranga: "+gang.roles.get(me.role).name;
            g.drawString(font,font.plainSubstrByWidth(subtitle,w-106),left+54,top+25,0xFFAAAAB7,false);
        }
        if(gang==null&&!find || gang!=null&&tab==4&&settingsSection==1){
            int previewX=cx+cw-140,previewY=cy+(gang==null?24:55);
            g.fill(previewX,previewY,previewX+140,previewY+148,0x20000000);g.renderOutline(previewX,previewY,140,148,0x66000000|t.border());
            g.drawCenteredString(font,"PODGLĄD LOGO",previewX+70,previewY+12,0xFFAAA7B6);
            if(texture!=null){g.pose().pushPose();g.pose().translate(previewX+22,previewY+32,0);g.pose().scale(1.5f,1.5f,1);g.blit(texture,0,0,0,0,64,64,64,64);g.pose().popPose();}
            else{g.renderOutline(previewX+38,previewY+48,64,64,t.borderArgb());g.drawCenteredString(font,"+",previewX+70,previewY+74,t.borderArgb());}
        }
        if(gang!=null&&tab==4&&settingsSection==0){
            int half=(cw-12)/2,y=cy+42;
            g.fill(cx,y+70,cx+half,y+75,(opacity<<24)|Integer.parseInt(background,16));
            g.fill(cx+half+12,y+70,cx+cw,y+75,0xFF000000|Integer.parseInt(border,16));
            g.fill(cx,y+104,cx+cw,y+105,0x44000000|t.border());
        }
        clip(g,cx,cy,cx+cw,top+h-24);
        for(Label line:labels)g.drawString(font,font.plainSubstrByWidth(line.value(),line.width()),line.x(),line.y()-bodyScroll,line.color(),false);
        g.disableScissor();
        super.render(g,mouseX,mouseY,delta);
        if(!draggedRole.isEmpty()&&roleMoved){
            int listWidth=Math.min(150,(cw-12)/2),drop=roleDropIndex(dragY);
            int lineY=Math.clamp(cy+26+drop*22-bodyScroll,cy,top+h-26);
            g.fill(cx,lineY,cx+listWidth,lineY+2,t.borderArgb());
            int ghostY=(int)Math.clamp(dragY-9,cy,top+h-44);
            g.fill(cx+3,ghostY,cx+listWidth+3,ghostY+20,0xEE29232F);g.renderOutline(cx+3,ghostY,listWidth,20,t.borderArgb());
            Role role=gang.roles.get(draggedRole);if(role!=null)g.drawString(font,font.plainSubstrByWidth(role.name,listWidth-12),cx+9,ghostY+6,0xFFEAE7ED,false);
        }
        if(gang==null)g.fill(left+w/2,top+1,left+w/2+1,top+42,t.borderArgb());
        String message=ClientState.message;g.drawString(font,font.plainSubstrByWidth(message,w-20),left+10,top+h-14,0xFFD4C4CC,false);
        if(!message.isBlank()&&mouseY>=top+h-22&&mouseY<top+h && font.width(message)>w-20){
            var wrapped=font.split(Component.literal(message),w-28);int tipY=top+h-28-wrapped.size()*11;
            g.fill(left+6,tipY-4,left+w-6,top+h-24,0xFF181820);
            for(int i=0;i<wrapped.size();i++)g.drawString(font,wrapped.get(i),left+12,tipY+i*11,0xFFE9E8EC,false);
        }
    }
    private static int mix(int a,int b,float ratio){int r=(int)((a>>16&255)*(1-ratio)+(b>>16&255)*ratio),g=(int)((a>>8&255)*(1-ratio)+(b>>8&255)*ratio),blue=(int)((a&255)*(1-ratio)+(b&255)*ratio);return r<<16|g<<8|blue;}
    private int roleDropIndex(double y){return Math.clamp((int)Math.floor((y+bodyScroll-cy-26)/22),0,gang().roles.size()-1);}
    @Override public boolean mouseClicked(double x,double y,int button){
        x=layout.local(x);y=layout.local(y);
        String role="";
        if(button==0&&tab==2&&can(Permission.MANAGE_ROLES))for(var entry:roleButtons.entrySet()){
            if(entry.getValue().visible&&entry.getValue().isMouseOver(x,y)){role=entry.getKey();break;}
        }
        boolean result=super.mouseClicked(x,y,button);
        if(!role.isEmpty()){draggedRole=role;dragY=dragStartY=y;roleMoved=false;}return result;
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&!draggedRole.isEmpty()){
            if(roleMoved&&gang()!=null&&can(Permission.MANAGE_ROLES)){
                List<String> order=new ArrayList<>(gang().roles.keySet());int target=roleDropIndex(layout.local(y));
                order.remove(draggedRole);order.add(Math.min(target,order.size()),draggedRole);
                JsonObject args=new JsonObject();JsonArray ids=new JsonArray();order.forEach(ids::add);args.add("order",ids);send("reorder_roles",args);
            }
            draggedRole="";roleMoved=false;
        }
        return super.mouseReleased(layout.local(x),layout.local(y),button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0&&!draggedRole.isEmpty()){
            dragY=layout.local(y);roleMoved|=Math.abs(dragY-dragStartY)>3;
            if(dragY<cy+10&&bodyScroll>0){bodyScroll=Math.max(0,bodyScroll-8);rebuildWidgets();}
            else if(dragY>top+h-36&&bodyScroll<maxBodyScroll){bodyScroll=Math.min(maxBodyScroll,bodyScroll+8);rebuildWidgets();}
            return true;
        }
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

