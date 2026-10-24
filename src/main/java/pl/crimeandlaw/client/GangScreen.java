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
    private static final java.time.format.DateTimeFormatter HISTORY_DATE=java.time.format.DateTimeFormatter.ofPattern("d MMMM uuuu 'o godzinie' HH:mm",Locale.forLanguageTag("pl-PL"));
    private final List<Label> labels=new ArrayList<>();
    private record Label(int x,int y,String value,int color,int width){}
    private int left,top,w,h,cx,cy,cw,tab,memberPage,listPage,bodyScroll,maxBodyScroll;
    private GangLayout layout=GangLayout.of(800,450);
    private boolean find,requests=true;
    private String query="",draftName="",draftLogo="",selectedMember="",selectedRole="member",roleName="";
    private String background="15161C",border="A3425D",lastGang="";
    private int opacity=235;
    private int settingsSection;
    private boolean previewTheme;
    private long logoRequest;
    private String draggedRole="";
    private double dragStartY,dragY;
    private boolean roleMoved;
    private final Map<String,FlatButton> roleButtons=new LinkedHashMap<>();
    private final Map<FlatButton,Integer> listRows=new LinkedHashMap<>();
    private final List<DirectoryRow> directoryRows=new ArrayList<>();
    private record DirectoryRow(String name,int y){}
    private final ScrollPosition directoryScroll=new ScrollPosition(),roleScroll=new ScrollPosition();
    private final PermissionList.State permissionState=new PermissionList.State();
    private ScrollPosition activeList;
    private int listTop,listBottom,lastDirectoryPage=-1;
    private String roleDraftFor="";
    private FlatButton createGangButton,saveRoleButton;
    private EnumSet<Permission> rolePermissions=EnumSet.noneOf(Permission.class);
    private final ScrollPosition historyScroll=new ScrollPosition();
    private final List<HistoryRow> historyRows=new ArrayList<>();
    private record HistoryRow(String date,List<net.minecraft.util.FormattedCharSequence> lines,int top,int height){}
    private int historyTop,historyBottom,lastHistoryPage=-1;
    private final ScrollPosition terrainScroll=new ScrollPosition();
    private String terrainCenter="";
    private int terrainX,terrainZ,terrainTicks;
    private int terrainRadius=pl.crimeandlaw.territory.TerrainView.RADIUS;
    private boolean terrainGrid;
    private MemberCard memberCard;
    private GangBanner draftBanner;
    private pl.crimeandlaw.territory.TerrainView.Cell terrainSelection;
    private TerrainPanel terrainPanel;
    private long roleScrollTime;


    public GangScreen(){super(Component.literal("Crime And Law"));}
    public void terrainUpdated(){if(tab==2){if(terrainPanel!=null&&terrainPanel.dragging())terrainPanel.update(TerrainClient.view);else rebuildWidgets();}}
    public void captureUpdated(){if(tab==2){if(terrainPanel!=null&&terrainPanel.dragging())terrainPanel.update(TerrainClient.view);else rebuildWidgets();}}
    @Override public void tick(){super.tick();if(gang()!=null&&tab==2){TerrainClient.tick();if(++terrainTicks%20==0)CaptureClient.request();if(terrainTicks%40==0)TerrainClient.request();}}
    private void scrollDraggedRole(){
        if(draggedRole.isEmpty()||!roleMoved){roleScrollTime=0;return;}
        long now=System.nanoTime();double seconds=roleScrollTime==0?0:Math.min(.05,(now-roleScrollTime)/1_000_000_000.0);roleScrollTime=now;
        if(dragY<listTop+12)roleScroll.scroll(-140*seconds);else if(dragY>listBottom-12)roleScroll.scroll(140*seconds);
    }
    private GangBanner banner(){return draftBanner!=null?draftBanner:gang()==null?GangBanner.DEFAULT:gang().banner;}

    private Gang gang(){return ClientState.gang();}
    private boolean isLeader(){Gang g=gang();return g!=null&&g.owner.equals(ClientState.player());}
    private boolean can(Permission permission){return GangDirectory.allowed(gang(),ClientState.player(),permission);}
    private boolean canManage(){return can(Permission.INVITE)||can(Permission.MANAGE_MEMBERS)||can(Permission.ASSIGN_ROLES);}
    double bannerAspect(){return (w-110)/123.0;}
    int bannerColor(){return banner().color();}
    private Theme theme(){if(previewTheme)return draftTheme();Gang g=gang();if(g==null)return Theme.DEFAULT;Theme local=PersonalTheme.get(g.id.toString());return local==null?g.theme:local;}
    Theme currentTheme(){return theme();}
    public void updated(){
        Gang g=gang();String id=g==null?"":g.id.toString();
        if(!id.equals(lastGang)){
            lastGang=id;tab=0;selectedMember="";memberPage=0;previewTheme=false;lastHistoryPage=-1;historyScroll.reset();
            Theme t=theme();background=String.format("%06X",t.background());border=String.format("%06X",t.border());opacity=t.opacity();
            draftLogo="";selectedRole="member";roleName="";rolePermissions.clear();draggedRole="";roleMoved=false;
            roleDraftFor="";roleScroll.reset();directoryScroll.reset();draftBanner=null;terrainCenter="";terrainSelection=null;
        }
        if(g!=null&&draftBanner!=null&&draftBanner.equals(g.banner))draftBanner=null;
        if(g!=null&&!draftLogo.isEmpty()&&draftLogo.equals(g.logo))draftLogo="";
        rebuildWidgets();
    }
    @Override protected void init(){
        layout=GangLayout.of(width,height);w=layout.panelWidth();h=layout.panelHeight();
        left=(layout.canvasWidth()-w)/2;top=(layout.canvasHeight()-h)/2;
        labels.clear();historyRows.clear();roleButtons.clear();listRows.clear();directoryRows.clear();activeList=null;
        createGangButton=null;saveRoleButton=null;memberCard=null;
        Gang g=gang();
        if(!ClientState.ready()){addRenderableWidget(new CloseButton(left+w-30,top+11,b->onClose(),this::theme));return;}
        if(g!=null)tab=GangCategory.visibleOrOverview(tab).tabId();
        if(g==null){
            cx=left+20;cy=top+62;cw=w-40;
            int half=(w-2)/2;
            addRenderableWidget(new FlatButton(left+1,top+1,half,41,"Stwórz gang",b->{find=false;bodyScroll=0;rebuildWidgets();},this::theme,!find,true));
            addRenderableWidget(new FlatButton(left+1+half,top+1,half,41,"Znajdź gang",b->{find=true;memberPage=0;bodyScroll=0;directoryScroll.reset();ClientState.refresh(query,0);rebuildWidgets();},this::theme,find,true));
            addRenderableWidget(new CloseButton(left+w-30,top+11,b->onClose(),this::theme));
            if(find)buildFind();else buildCreate();
        }else{
            cx=left+116;cy=top+55;cw=w-130;
            addRenderableWidget(new CloseButton(left+w-30,top+11,b->onClose(),this::theme));
            for(int i=0;i<TABS.size();i++){
                final GangCategory category=TABS.get(i);final int chosen=category.tabId();button(left+10,cy+i*24,96,category==GangCategory.BALANCE?"Saldo gangu":category.label(),b->{tab=chosen;memberPage=0;bodyScroll=0;historyScroll.reset();rebuildWidgets();},tab==chosen);
            }
            button(left+10,top+h-75,96,"Ustawienia",b->{tab=3;bodyScroll=0;rebuildWidgets();},tab==3);
            String leaveMessage=ClientState.memberCount()==1?"Ostatni członek opuszcza gang — gang zostanie usunięty.":isLeader()?"Lider musi najpierw przekazać przywództwo.":"Czy chcesz opuścić ten gang?";
            button(left+10,top+h-48,96,"Opuść gang",b->confirm("Opuścić gang?",leaveMessage,()->send("leave",new JsonObject())));
            switch(tab){case 1->buildMembers();case 2->buildMap();case 3->buildSettings();case 4->buildRegister();case 6,7,8,9->buildPlannedCategory();case 10->buildRoles();}
        }
        if(activeList!=null){positionListRows();maxBodyScroll=0;bodyScroll=0;return;}
        if(g!=null&&tab==4){maxBodyScroll=0;bodyScroll=0;return;}
        int bottom=cy;
        for(var child:children())if(child instanceof AbstractWidget widget && widget.getX()>=cx && widget.getY()>=cy)bottom=Math.max(bottom,widget.getY()+widget.getHeight());
        for(Label line:labels)bottom=Math.max(bottom,line.y()+10);
        maxBodyScroll=Math.max(0,bottom-(top+h-26));bodyScroll=Math.clamp(bodyScroll,0,maxBodyScroll);
        for(var child:children())if(child instanceof AbstractWidget widget && widget.getX()>=cx && widget.getY()>=cy){
            widget.setY(widget.getY()-bodyScroll);widget.visible=widget.getY()>=cy && widget.getY()+widget.getHeight()<=top+h-24&&(!(widget instanceof RegisterScrollBar)||widget.active);
        }
    }
    private void label(int x,int y,String text){label(x,y,text,0xFFE9E8EC);}
    private void label(int x,int y,String text,int color){labels.add(new Label(x,y,text,color,Math.max(10,left+w-12-x)));}
    private FlatButton button(int x,int y,int width,String text,Button.OnPress action){return button(x,y,width,text,action,false);}
    private FlatButton button(int x,int y,int width,String text,Button.OnPress action,boolean selected){
        var widget=addRenderableWidget(new FlatButton(x,y,width,19,text,action,this::theme,selected));
        if(Set.of("‹","›","−","+","Odśwież","Odśwież dane","Zapisz rangę","Zapisz logo gangu","Przyjmij","Odrzuć","Zmień rangę","Usuń z gangu","Przekaż przywództwo","Zapisz tylko u mnie","Zapisz dla gangu","Używaj wyglądu gangu").contains(text))widget.centered();
        return widget;
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
        int x=cx+22,formWidth=cw-216;
        label(x,cy+16,"Stwórz swój gang");label(x,cy+45,"Nazwa (3–32 znaki)",0xFFAAAAB7);
        field(x,cy+62,formWidth,32,draftName,v->{draftName=v;if(createGangButton!=null)createGangButton.active=validName(draftName,3);});
        logoControls(x,cy+100,formWidth,true);
        createGangButton=addRenderableWidget(new FlatButton(x,cy+174,formWidth,28,"Utwórz gang",b->{
            JsonObject args=new JsonObject();args.addProperty("name",draftName);args.addProperty("logo",draftLogo);send("create",args);
        },this::theme,false).primary());
        createGangButton.active=validName(draftName,3);
    }
    private static boolean validName(String value,int minimum){return value.strip().replaceAll("\\s+"," ").length()>=minimum;}
    private void logoControls(int x,int y,int width,boolean creating){
        label(x,y,"Logo z komputera",0xFFAAAAB7);
        button(x,y+18,Math.min(240,width),"Wybierz obraz…",b->chooseLogoFile()).centered().active=creating||can(Permission.MANAGE_THEME);
    }
    private void buildFind(){
        label(cx,cy,"Znajdź gang");int searchWidth=Math.max(50,cw-82);
        field(cx,cy+20,searchWidth,32,query,v->query=v);
        button(cx+searchWidth+6,cy+20,76,"Szukaj",b->search());
        JsonArray rows=ClientState.state.has("gangs")?ClientState.state.getAsJsonArray("gangs"):new JsonArray();
        int page=ClientState.number("page",0),pages=ClientState.number("pages",1);
        if(lastDirectoryPage!=page){lastDirectoryPage=page;directoryScroll.reset();}
        activeList=directoryScroll;listTop=cy+48;listBottom=top+h-62;
        directoryScroll.range(listBottom-listTop,Math.max(0,rows.size()*23-4));
        if(rows.isEmpty())label(cx,cy+52,"Nie znaleziono gangów.",0xFFAAAAB7);
        for(int i=0;i<rows.size();i++){
            JsonObject row=rows.get(i).getAsJsonObject();int y=listTop+i*23;
            directoryRows.add(new DirectoryRow(row.get("name").getAsString()+"  ("+row.get("members").getAsInt()+")",y));
            boolean pending=row.get("requested").getAsBoolean();
            var request=button(cx+cw-104,y,88,pending?"Wysłano":"Zgłoś się",b->{JsonObject a=new JsonObject();a.addProperty("id",row.get("id").getAsString());send("request",a);});
            request.active=!pending;listRows.put(request,y);
        }
        addRenderableWidget(new RegisterScrollBar(cx+cw-10,listTop,listBottom-listTop,directoryScroll,this::theme,"Przewijanie gangów"));
        int y=top+h-48;
        button(cx,y,42,"‹",b->{directoryScroll.reset();listPage=page-1;ClientState.refresh(query,page-1);}).active=page>0;
        label(cx+50,y+5,"Strona "+(page+1)+" / "+pages);
        button(cx+cw-42,y,42,"›",b->{directoryScroll.reset();listPage=page+1;ClientState.refresh(query,page+1);}).active=page+1<pages;
    }
    private void search(){directoryScroll.reset();listPage=0;memberPage=0;ClientState.refresh(query,0);}
    private void positionListRows(){
        if(activeList==null)return;
        for(var entry:listRows.entrySet()){
            var row=entry.getKey();row.setY(entry.getValue()-(int)Math.round(activeList.offset()));
            row.visible=row.getY()>=listTop&&row.getY()+row.getHeight()<=listBottom;
        }
    }
    private void drawDirectory(GuiGraphics g){
        for(var row:directoryRows){
            int y=row.y()-(int)Math.round(directoryScroll.offset());
            if(y<listTop||y+19>listBottom)continue;
            g.fill(cx,y,cx+cw-16,y+19,0x16000000);
            g.drawString(font,font.plainSubstrByWidth(row.name(),cw-112),cx+5,y+5,0xFFE9E8EC,false);
        }
    }
    private void buildRoles(){
        Gang g=gang();label(cx,cy,"Rangi i uprawnienia");int listWidth=Math.min(150,(cw-12)/2),rx=cx+listWidth+12;
        activeList=roleScroll;listTop=cy+26;listBottom=top+h-76;
        roleScroll.range(listBottom-listTop,Math.max(0,g.roles.size()*22-3));
        int index=0;for(Role r:g.roles.values()){
            int y=listTop+index++*22;
            var row=button(cx,y,listWidth-12,"≡  "+r.name,b->{selectedRole=r.id;roleDraftFor=r.id;roleName=r.name;rolePermissions=r.permissions.isEmpty()?EnumSet.noneOf(Permission.class):EnumSet.copyOf(r.permissions);rebuildWidgets();},selectedRole.equals(r.id));
            roleButtons.put(r.id,row);listRows.put(row,y);
        }
        addRenderableWidget(new RegisterScrollBar(cx+listWidth-10,listTop,listBottom-listTop,roleScroll,this::theme,"Przewijanie rang"));
        button(cx,top+h-48,listWidth,"+ Nowa ranga",b->minecraft.setScreen(new NewRoleScreen(this,(name,permissions)->{
            JsonObject a=new JsonObject();a.addProperty("role","");a.addProperty("name",name);
            for(Permission p:Permission.values())a.addProperty(p.name(),permissions.contains(p));send("role",a);
        }))).active=can(Permission.MANAGE_ROLES)&&g.roles.size()<GangDirectory.MAX_ROLES;
        Role current=g.roles.get(selectedRole);
        if(!selectedRole.equals(roleDraftFor)&&current!=null){roleDraftFor=selectedRole;roleName=current.name;rolePermissions=current.permissions.isEmpty()?EnumSet.noneOf(Permission.class):EnumSet.copyOf(current.permissions);}
        field(rx,cy+26,cw-listWidth-12,24,roleName,v->{roleName=v;if(saveRoleButton!=null)saveRoleButton.active=can(Permission.MANAGE_ROLES)&&!selectedRole.equals("leader")&&validName(v,2);}).active=can(Permission.MANAGE_ROLES)&&!selectedRole.equals("leader");
        int filterY=cy+52,editorWidth=cw-listWidth-12,groupWidth=Math.min(112,editorWidth/2);
        var catalog=new PermissionList(rx,cy+78,editorWidth-14,top+h-78-(cy+78),permissionState,rolePermissions,
            ()->can(Permission.MANAGE_ROLES)&&!selectedRole.equals("leader"),this::theme);
        var group=button(rx,filterY,groupWidth,permissionState.group.label+" ▸",b->{permissionState.nextGroup();b.setMessage(Component.literal(permissionState.group.label+" ▸"));catalog.filter();});
        group.setTooltip(Tooltip.create(Component.literal("Zmień grupę uprawnień")));
        field(rx+groupWidth+6,filterY,editorWidth-groupWidth-6,64,permissionState.query,v->{permissionState.query=v;permissionState.scroll.reset();catalog.filter();}).setHint(Component.literal("Szukaj…"));
        addRenderableWidget(catalog);
        catalog.scrollbar(addRenderableWidget(new RegisterScrollBar(rx+editorWidth-10,cy+78,catalog.getHeight(),permissionState.scroll,this::theme,"Przewijanie uprawnień")));
        saveRoleButton=button(rx,top+h-48,editorWidth,"Zapisz rangę",b->{JsonObject a=new JsonObject();a.addProperty("role",selectedRole);a.addProperty("name",roleName);
            for(Permission p:Permission.values())a.addProperty(p.name(),p.available()&&rolePermissions.contains(p));send("role",a);
        });saveRoleButton.active=can(Permission.MANAGE_ROLES)&&!selectedRole.equals("leader")&&validName(roleName,2);
    }

    private void buildMembers(){
        label(cx,cy,"Członkowie: "+ClientState.number("onlineCount",0)+" online / "+ClientState.memberCount());
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
            if(!manage&&!applications&&member!=null){
                Role role=g.roles.get(member.role);
                addRenderableWidget(new MemberRow(cx,y+i*34,listWidth,name,role==null?"Członek":role.name,online(id),b->{
                    selectedMember=selectedMember.equals(id.toString())?"":id.toString();rebuildWidgets();
                },this::theme,selectedMember.equals(id.toString())));
            }else button(cx,y+i*23,listWidth,font.plainSubstrByWidth(name,listWidth-12),b->{selectedMember=id.toString();rebuildWidgets();},selectedMember.equals(id.toString()));
        }
        if(ids.isEmpty())label(cx,y+6,applications?"Brak zgłoszeń.":"Brak członków.",0xFFAAAAB7);
        if(!selectedMember.isBlank()&&ids.stream().anyMatch(id->id.toString().equals(selectedMember))){
            UUID selected=UUID.fromString(selectedMember);
            if(applications){
                label(rx,y+5,g.requests.get(selected));
                if(manage){
                    button(rx,y+28,cw-listWidth-16,"Przyjmij",b->target("accept",selectedMember)).active=can(Permission.INVITE);
                    button(rx,y+52,cw-listWidth-16,"Odrzuć",b->target("reject",selectedMember)).active=can(Permission.INVITE);
                }
            }else{
                Member member=g.members.get(selected);Role assigned=g.roles.get(member.role);
                if(!manage){
                    int index=ids.indexOf(selected);
                    memberCard=addRenderableWidget(new MemberCard(cx,y+index*34,listWidth,cx,cy+24,cx+cw,top+h-56,member,assigned==null?"Członek":assigned.name,online(selected),this::theme,()->{selectedMember="";rebuildWidgets();}));
                }else{
                    label(rx,y+5,member.name);label(rx,y+23,"Ranga: "+(assigned==null?"Członek":assigned.name),0xFFAAAAB7);
                    button(rx,y+49,cw-listWidth-16,"Zmień rangę",b->{
                        List<Role> roles=g.roles.values().stream().filter(r->!r.id.equals("leader")).toList();
                        int index=0;for(int i=0;i<roles.size();i++)if(roles.get(i).id.equals(member.role))index=i;
                        JsonObject a=new JsonObject();a.addProperty("target",selectedMember);a.addProperty("role",roles.get((index+1)%roles.size()).id);send("assign",a);
                    }).active=can(Permission.ASSIGN_ROLES)&&!g.owner.equals(selected);
                    button(rx,y+73,cw-listWidth-16,"Usuń z gangu",b->confirm("Usunąć członka?",member.name,()->target("kick",selectedMember))).active=can(Permission.MANAGE_MEMBERS)&&!g.owner.equals(selected)&&!selected.equals(ClientState.player());
                    button(rx,y+97,cw-listWidth-16,"Przekaż przywództwo",b->confirm("Przekazać przywództwo?","Nowy lider: "+member.name,()->target("transfer",selectedMember))).active=isLeader()&&!g.owner.equals(selected);
                }
            }
        }
        String key=applications?"request":"member";int page=ClientState.number(key+"Page",0),pages=ClientState.number(key+"Pages",1);
        int bottom=top+h-48;
        button(cx,bottom,42,"‹",b->{selectedMember="";ClientState.listPage(applications,page-1);}).active=page>0;
        label(cx+50,bottom+5,"Strona "+(page+1)+" / "+pages);
        button(cx+listWidth-42,bottom,42,"›",b->{selectedMember="";ClientState.listPage(applications,page+1);}).active=page+1<pages;
    }
    private boolean online(UUID id){
        if(ClientState.state.has("onlineMemberIds"))for(var value:ClientState.state.getAsJsonArray("onlineMemberIds"))if(value.getAsString().equals(id.toString()))return true;
        return false;
    }
    private void buildRegister(){
        bodyScroll=0;label(cx,cy,"Rejestr gangu");
        int page=ClientState.number("historyPage",0),pages=ClientState.number("historyPages",1);
        if(lastHistoryPage!=page){lastHistoryPage=page;historyScroll.reset();}
        historyTop=cy+26;historyBottom=top+h-16;
        JsonArray events=ClientState.state.has("history")?ClientState.state.getAsJsonArray("history"):new JsonArray();
        var format=HISTORY_DATE.withZone(java.time.ZoneId.systemDefault());
        int offset=0;
        for(JsonElement item:events){
            JsonObject event=item.getAsJsonObject();
            String date=format.format(java.time.Instant.ofEpochMilli(event.get("timestamp").getAsLong()));
            var lines=font.split(Component.literal(event.get("text").getAsString()),cw-24);
            int rowHeight=lines.size()*11+30;
            historyRows.add(new HistoryRow(date,lines,offset,rowHeight));offset+=rowHeight;
        }
        historyScroll.range(historyBottom-historyTop,offset);
        addRenderableWidget(new RegisterScrollBar(cx+cw-10,historyTop,historyBottom-historyTop,historyScroll,this::theme));
        String pageText="Strona "+(page+1)+" / "+pages;
        int pageX=cx+cw-font.width(pageText),arrowsX=pageX-54;
        button(arrowsX,cy-5,23,"‹",b->{historyScroll.reset();ClientState.historyPage(page-1);}).active=page>0;
        button(arrowsX+26,cy-5,23,"›",b->{historyScroll.reset();ClientState.historyPage(page+1);}).active=page+1<pages;
        label(pageX,cy,pageText);
    }
    private void drawRegister(GuiGraphics g){
        g.fill(cx,cy+18,cx+cw,cy+19,0x55000000|theme().border());
        clip(g,cx,historyTop,cx+cw-16,historyBottom);
        if(historyRows.isEmpty())g.drawString(font,"Brak wpisów. Nowe wydarzenia pojawią się tutaj.",cx,historyTop+8,0xFFAAAAB7,false);
        for(HistoryRow row:historyRows){
            int y=historyTop+row.top()-(int)Math.round(historyScroll.offset());
            if(y+row.height()<=historyTop||y>=historyBottom)continue;
            g.drawString(font,row.date(),cx,y+7,0xFFAAA3B3,false);
            for(int i=0;i<row.lines().size();i++)g.drawString(font,row.lines().get(i),cx,y+21+i*11,0xFFE9E8EC,false);
            g.fill(cx,y+row.height()-1,cx+cw-20,y+row.height(),0x22000000|theme().border());
        }
        g.disableScissor();
    }
    private void buildPlannedCategory(){
        label(cx,cy,GangCategory.visibleOrOverview(tab).label());
    }
    private void buildMap(){
        label(cx,cy,"Mapa świata");var view=TerrainClient.view;if(view==null)TerrainClient.request();
        if(view!=null){
            if(!view.dimension().equals(terrainCenter)){terrainCenter=view.dimension();terrainX=view.centerX();terrainZ=view.centerZ();terrainSelection=null;terrainScroll.reset();}
            for(var cell:view.cells())if(cell.x()==terrainX&&cell.z()==terrainZ)terrainSelection=cell;
        }
        button(cx+cw-102,cy-5,102,"Odśwież",b->TerrainClient.request());
        addRenderableWidget(new PlayerHomeButton(cx+84,cy-5,TerrainClient::home,this::theme,TerrainClient.following()));
        button(cx+114,cy-5,66,"Siatka",b->{terrainGrid=!terrainGrid;rebuildWidgets();},terrainGrid).centered().strongSelection();
        button(cx+186,cy-5,22,"−",b->{terrainRadius=Math.min(pl.crimeandlaw.territory.TerrainView.RADIUS,terrainRadius+1);rebuildWidgets();}).active=terrainRadius<pl.crimeandlaw.territory.TerrainView.RADIUS;
        button(cx+212,cy-5,22,"+",b->{terrainRadius=Math.max(2,terrainRadius-1);rebuildWidgets();}).active=terrainRadius>2;
        button(cx+242,cy-5,82,"Zaznacz",b->minecraft.setScreen(new TerrainMarkScreen(this,terrainX,terrainZ))).centered();
        CaptureClient.select(terrainX,terrainZ);
        int mapTop=cy+25,mapHeight=top+h-76-mapTop;
        terrainPanel=addRenderableWidget(new TerrainPanel(cx,mapTop,cw,mapHeight,view,terrainX,terrainZ,terrainSelection,terrainRadius,terrainGrid,this::theme,terrainScroll,
            cell->{terrainX=cell.x();terrainZ=cell.z();terrainSelection=cell;terrainScroll.reset();rebuildWidgets();},
            direction->{terrainRadius=Math.clamp(terrainRadius+direction,2,pl.crimeandlaw.territory.TerrainView.RADIUS);rebuildWidgets();}));
        addRenderableWidget(new RegisterScrollBar(cx+cw-10,mapTop+6,mapHeight-12,terrainScroll,this::theme,"Przewijanie opisu terenu"));
        int bottom=top+h-68;boolean current=CaptureClient.matches(terrainX,terrainZ);
        if(current){
            labels.add(new Label(cx,bottom+24,CaptureClient.text("time")+" · "+CaptureClient.text("zone"),0xFFAAA3B3,TerrainPanel.GRID));
            labels.add(new Label(cx,bottom+36,"Capty "+CaptureClient.number("opens")+":00–"+CaptureClient.number("closes")+":00 · min. "+CaptureClient.number("minimum"),0xFFAAA3B3,TerrainPanel.GRID));
        }
        int right=cx+TerrainPanel.DETAILS,space=cw-TerrainPanel.DETAILS;
        var battle=current&&CaptureClient.state.has("battle")?CaptureClient.state.getAsJsonObject("battle"):null;
        if(battle!=null){
            boolean joined=battle.get("joined").getAsBoolean(),eliminated=battle.get("eliminated").getAsBoolean(),closed=battle.get("phase").getAsString().equals("FIGHTING");
            button(right,bottom,space,eliminated?"Wyeliminowano":joined?"Uczestniczysz w capcie":closed?"Zapisy zakończone":"Dołącz do captu",b->CaptureClient.action("capture_join")).centered().active=!joined&&!closed&&CaptureClient.flag("present")&&can(Permission.JOIN_TERRITORY_ATTACK);
        }else if(!CaptureClient.flag("unlocked")){
            button(right,bottom,space,"Aktywuj kontrolę terytoriów",b->CaptureClient.action("capture_unlock")).centered().active=current&&isLeader();
        }else{
            button(right,bottom,space,"Rozpocznij przejęcie",b->confirm("Rozpocząć capt?","Chunk "+terrainX+", "+terrainZ+". Przejmowanie wymaga obecności w strefie.",()->CaptureClient.action("capture_start"))).centered().active=current&&CaptureClient.flag("open")&&CaptureClient.flag("present")&&CaptureClient.flag("adjacent")&&CaptureClient.flag("recognized")&&!CaptureClient.flag("busy")&&CaptureClient.number("eligible")>=CaptureClient.number("minimum")&&CaptureClient.number("cooldown")==0&&CaptureClient.number("protection")==0&&can(Permission.START_TERRITORY_ATTACK)&&!gang().id.toString().equals(CaptureClient.text("owner"));
        }
        if(battle!=null)label(right,bottom+25,CaptureClient.phase()+": "+CaptureClient.duration(battle.get("remaining").getAsLong()),0xFFE9E8EC);
        else if(current&&CaptureClient.number("cooldown")>0)label(right,bottom+25,"Cooldown: "+CaptureClient.duration(CaptureClient.number("cooldown")/1000),0xFFAAA3B3);
        else if(current&&CaptureClient.number("protection")>0)label(right,bottom+25,"Ochrona: "+CaptureClient.duration(CaptureClient.number("protection")/1000),0xFFAAA3B3);
        else if(current){
            String reason=!CaptureClient.flag("unlocked")?"Aktywacja jest obecnie bezpłatna.":!can(Permission.START_TERRITORY_ATTACK)?"Brak uprawnienia do rozpoczęcia captu.":CaptureClient.flag("busy")?"Twój gang uczestniczy już w capcie.":!CaptureClient.flag("open")?"Poza godzinami rozpoczynania captów.":!CaptureClient.flag("recognized")?"Poczekaj na rozpoznanie terenu.":gang().id.toString().equals(CaptureClient.text("owner"))?"To już jest teren twojego gangu.":!CaptureClient.flag("adjacent")?"Wybierz teren obok własnego terytorium.":!CaptureClient.flag("present")?"Stań w wybranym chunku.":CaptureClient.number("eligible")<CaptureClient.number("minimum")?"Za mało uprawnionych graczy w strefie.":"Przejęcie neutralnego terenu jest bezpłatne.";
            label(right,bottom+25,reason,0xFFAAA3B3);
        }
    }
    private Theme draftTheme(){return new Theme(Integer.parseInt(background.replace("#",""),16),Integer.parseInt(border.replace("#",""),16),opacity);}
    private void buildSettings(){
        if(!canManage()&&settingsSection==2)settingsSection=0;
        button(cx,cy,110,"Wygląd",b->{settingsSection=0;bodyScroll=0;rebuildWidgets();},settingsSection!=2);
        if(canManage())button(cx+116,cy,122,"Zarządzanie",b->{settingsSection=2;bodyScroll=0;selectedMember="";rebuildWidgets();},settingsSection==2);
        if(settingsSection!=2){
            button(cx,cy+27,110,"Kolory menu",b->{settingsSection=0;bodyScroll=0;rebuildWidgets();},settingsSection==0);
            button(cx+116,cy+27,94,"Logo",b->{settingsSection=1;bodyScroll=0;rebuildWidgets();},settingsSection==1);
            button(cx+216,cy+27,94,"Baner",b->{settingsSection=3;bodyScroll=0;rebuildWidgets();},settingsSection==3);
        }
        int y=cy+68,half=(cw-12)/2;
        if(settingsSection==0){
            label(cx,y,"Kolory i przezroczystość");
            button(cx,y+28,half,"Zmień kolor tła…",b->openColor(true));
            button(cx+half+12,y+28,half,"Zmień obramówkę…",b->openColor(false));
            label(cx,y+60,"Krycie tła: "+Math.round(opacity/255.0*100)+"%",0xFFAAAAB7);
            label(cx+half+12,y+60,"Kolor akcentów menu",0xFFAAAAB7);
            label(cx,y+93,"Zapis wyglądu");
            button(cx,y+114,half,"Zapisz tylko u mnie",b->{
                if(PersonalTheme.set(gang().id.toString(),draftTheme())){previewTheme=false;ClientState.message="Zapisano osobisty wygląd.";}
            });
            button(cx+half+12,y+114,half,"Zapisz dla gangu",b->{
                Theme t=draftTheme();JsonObject a=new JsonObject();a.addProperty("background",t.background());a.addProperty("border",t.border());a.addProperty("opacity",t.opacity());send("theme",a);
            }).active=can(Permission.MANAGE_THEME);
            button(cx,y+141,cw,"Używaj wyglądu gangu",b->{
                if(PersonalTheme.set(gang().id.toString(),null)){previewTheme=false;Theme t=gang().theme;background=String.format("%06X",t.background());border=String.format("%06X",t.border());opacity=t.opacity();rebuildWidgets();}
            });
            button(cx,y+168,cw,"Kolor gangu na mapie i w capcie…",b->minecraft.setScreen(new ColorPickerScreen(this,"Kolor gangu",gang().mapColor,255,false,(color,alpha)->{JsonObject a=new JsonObject();a.addProperty("color",color);send("map_color",a);}))).centered().active=can(Permission.MANAGE_THEME);
        }else if(settingsSection==1){
            logoControls(cx,y,Math.min(cw-160,350),false);
            button(cx,y+56,Math.min(cw,230),"Zapisz logo gangu",b->{JsonObject a=new JsonObject();a.addProperty("logo",draftLogo);send("logo",a);}).active=can(Permission.MANAGE_THEME)&&!draftLogo.isBlank();
            label(cx,y+88,"Zmiana logo będzie widoczna dla całego gangu.",0xFFAAAAB7);
        }else if(settingsSection==3){
            label(cx,y,"Baner gangu");
            button(cx,y+24,half,"Jednolity kolor",b->{var current=banner();draftBanner=new GangBanner("",current.color(),current.opacity()==0?255:current.opacity());rebuildWidgets();},banner().image().isEmpty()).active=can(Permission.MANAGE_THEME);
            button(cx+half+12,y+24,half,"Wybierz obraz baneru…",b->chooseImageFile(true)).centered().active=can(Permission.MANAGE_THEME);
            button(cx,y+51,half,"Zmień kolor baneru…",b->openBannerColor()).centered().active=can(Permission.MANAGE_THEME);
            button(cx+half+12,y+51,half,"Usuń baner",b->{draftBanner=GangBanner.DEFAULT;rebuildWidgets();}).centered().active=can(Permission.MANAGE_THEME);
            button(cx,y+184,cw,"Zapisz baner gangu",b->{var value=banner();JsonObject args=new JsonObject();args.addProperty("image",value.image());args.addProperty("color",value.color());args.addProperty("opacity",value.opacity());send("banner",args);}).centered().active=can(Permission.MANAGE_THEME)&&draftBanner!=null;
        }else{
            if(!can(Permission.INVITE))requests=false;
            button(cx,cy+34,120,"Zgłoszenia: "+ClientState.number("requestCount",0),b->{requests=true;selectedMember="";rebuildWidgets();},requests).active=can(Permission.INVITE);
            button(cx+128,cy+34,120,"Skład gangu",b->{requests=false;selectedMember="";rebuildWidgets();},!requests);
            button(cx+cw-130,cy+34,130,"Odśwież dane",b->ClientState.refresh(query,listPage));
            buildPeople(requests,true,cy+65);
        }
    }
    private void openColor(boolean backgroundColor){
        Theme t=draftTheme();minecraft.setScreen(new ColorPickerScreen(this,backgroundColor?"Kolor i krycie tła":"Kolor obramówki",backgroundColor?t.background():t.border(),backgroundColor?opacity:255,backgroundColor,(rgb,alpha)->{
            if(backgroundColor){background=String.format("%06X",rgb);opacity=alpha;}else border=String.format("%06X",rgb);
            previewTheme=true;ClientState.message="";
        }));
    }
    private void openBannerColor(){
        var original=banner();minecraft.setScreen(new ColorPickerScreen(this,"Kolor i krycie baneru",original.color(),original.opacity(),true,(rgb,alpha)->{
            draftBanner=new GangBanner(original.image(),rgb,alpha);ClientState.message="";
        }));
    }
    private void chooseLogoFile(){chooseImageFile(false);}
    private void chooseImageFile(boolean banner){
        try(MemoryStack stack=MemoryStack.stackPush()){
            var filters=stack.pointers(stack.UTF8("*.png"),stack.UTF8("*.jpg"),stack.UTF8("*.jpeg"),stack.UTF8("*.gif"),stack.UTF8("*.bmp"),
                stack.UTF8("*.webp"),stack.UTF8("*.svg"),stack.UTF8("*.tif"),stack.UTF8("*.tiff"),stack.UTF8("*.ico"),stack.UTF8("*.icns"),
                stack.UTF8("*.psd"),stack.UTF8("*.tga"),stack.UTF8("*.pcx"),stack.UTF8("*.pnm"),stack.UTF8("*.ppm"),stack.UTF8("*.pgm"),stack.UTF8("*.pbm"),stack.UTF8("*"));
            String path=TinyFileDialogs.tinyfd_openFileDialog(banner?"Wybierz baner gangu":"Wybierz logo gangu",null,filters,"Obrazy (format rozpoznawany z pliku)",false);
            if(path!=null)loadImage(Path.of(path),banner);
        }catch(Exception e){ClientState.message="Nie można otworzyć okna wyboru. Przeciągnij obraz na okno gry.";}
    }
    @Override public void onFilesDrop(List<Path> files){
        if(files.size()!=1){ClientState.message="Przeciągnij jeden obraz naraz.";return;}
        if(gang()!=null&&!can(Permission.MANAGE_THEME)){ClientState.message="Nie masz uprawnień do zmiany logo gangu.";return;}
        boolean banner=gang()!=null&&tab==3&&settingsSection==3;
        if(gang()!=null){tab=3;settingsSection=banner?3:1;}else find=false;bodyScroll=0;rebuildWidgets();loadImage(files.getFirst(),banner);
    }
    private void loadImage(Path path,boolean banner){
        if(gang()!=null&&!can(Permission.MANAGE_THEME)){ClientState.message="Nie masz uprawnień do zmiany logo gangu.";return;}
        final long request=++logoRequest;
        ClientState.message="Wczytywanie obrazu…";
        java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try{return LogoImages.read(path);}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}
        }).whenComplete((image,error)->Minecraft.getInstance().execute(()->{
            if(request!=logoRequest||minecraft.screen!=this)return;
            if(error!=null){
                Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();
                ClientState.message=(banner?"Nie wczytano baneru: ":"Nie wczytano logo: ")+(cause.getMessage()==null?cause.getClass().getSimpleName():cause.getMessage());rebuildWidgets();
            }else{
                ClientState.message="";minecraft.setScreen(new LogoCropScreen(this,image,path.getFileName().toString(),encoded->{
                    if(banner){var value=banner();draftBanner=new GangBanner(encoded,value.color(),value.opacity()==0?255:value.opacity());ClientState.message="";}
                    else{draftLogo=encoded;ClientState.message="Logo przygotowane. "+(gang()==null?"Utwórz gang, aby je zapisać.":"Kliknij Zapisz logo gangu.");}
                    rebuildWidgets();
                },banner));
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
        scrollDraggedRole();positionListRows();
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
        if(!ClientState.ready()){
            g.drawCenteredString(font,"Crime And Law",left+w/2,top+16,0xFFF0EDF1);
            String status=ClientState.message.isBlank()?"Pobieranie danych gangu…":ClientState.message;
            g.drawCenteredString(font,font.plainSubstrByWidth(status,w-40),left+w/2,top+h/2,0xFFAAAAB7);
            super.render(g,mouseX,mouseY,delta);return;
        }
        Gang gang=gang();String logo=!draftLogo.isBlank()?draftLogo:gang==null?"":gang.logo;
        var texture=LogoTexture.get(logo);
        if(gang!=null){
            g.fill(left+3,top+45,left+107,top+h-24,0x16000000);
            g.fill(left+108,top+43,left+109,top+h-22,t.borderArgb());
            g.fill(left+46,top+7,left+w-35,top+36,0x28000000);
            g.fill(left+46,top+7,left+48,top+36,t.borderArgb());
            if(texture!=null)LogoTexture.draw(g,left+10,top+8,28,28);
            else{g.renderOutline(left+10,top+8,28,28,t.borderArgb());g.drawCenteredString(font,"G",left+24,top+18,t.borderArgb());}
            g.drawString(font,font.plainSubstrByWidth(gang.name,w-106),left+54,top+10,0xFFF0EDF1,false);
            Member me=gang.members.get(ClientState.player());Role myRole=me==null?null:gang.roles.get(me.role);
            String subtitle="Ranga: "+(myRole==null?"Członek":myRole.name);
            g.drawString(font,font.plainSubstrByWidth(subtitle,w-106),left+54,top+25,0xFFAAAAB7,false);
        }
        if(gang==null&&!find){
            g.fill(cx+6,cy+2,cx+cw-6,cy+232,0x22000000);g.renderOutline(cx+6,cy+2,cw-12,230,0x66000000|t.border());
            g.fill(cx+22,cy+33,cx+cw-22,cy+34,0x44000000|t.border());
        }
        if(gang==null&&!find || gang!=null&&tab==3&&settingsSection==1){
            int previewX=cx+cw-(gang==null?172:140),previewY=cy+(gang==null?44:81);
            g.fill(previewX,previewY,previewX+140,previewY+148,0x20000000);g.renderOutline(previewX,previewY,140,148,0x66000000|t.border());
            g.drawCenteredString(font,"PODGLĄD LOGO",previewX+70,previewY+12,0xFFAAA7B6);
            if(texture!=null)LogoTexture.draw(g,previewX+22,previewY+32,96,96);
            else{g.renderOutline(previewX+38,previewY+48,64,64,t.borderArgb());g.drawCenteredString(font,"+",previewX+70,previewY+74,t.borderArgb());}
        }
        if(gang!=null&&tab==3&&settingsSection==0){
            int half=(cw-12)/2,y=cy+68;
            g.fill(cx,y+50,cx+half,y+55,(opacity<<24)|Integer.parseInt(background,16));
            g.fill(cx+half+12,y+50,cx+cw,y+55,0xFF000000|Integer.parseInt(border,16));
            g.fill(cx,y+84,cx+cw,y+85,0x44000000|t.border());
        }
        if(gang!=null&&tab==3&&settingsSection==3){
            int bw=Math.min(cw,420),bh=(int)Math.round(bw/bannerAspect()),bx=cx+(cw-bw)/2;
            drawBanner(g,banner(),bx,cy+144,bw,bh);g.renderOutline(bx,cy+144,bw,bh,0x66000000|t.border());
        }
        if(gang!=null&&tab==0)drawGangOverview(g,gang,texture);
        if(gang!=null&&tab==4)drawRegister(g);
        if(gang==null&&find)drawDirectory(g);
        if(gang!=null&&(tab==6||tab==7||tab==8||tab==9)){
            int y=cy+30,height=126;
            g.fill(cx,y,cx+cw,y+height,0x18000000);g.renderOutline(cx,y,cw,height,0x44000000|t.border());
            g.fill(cx+cw/2-18,y+28,cx+cw/2+18,y+30,t.borderArgb());
            g.drawCenteredString(font,"W przygotowaniu",cx+cw/2,y+55,0xFFE9E8EC);
        }
        clip(g,cx,cy,cx+cw,top+h-24);
        for(Label line:labels)g.drawString(font,font.plainSubstrByWidth(line.value(),line.width()),line.x(),line.y()-bodyScroll,line.color(),false);
        g.disableScissor();
        super.render(g,mouseX,mouseY,delta);
        if(!draggedRole.isEmpty()&&roleMoved){
            int listWidth=Math.min(150,(cw-12)/2),drop=roleDropIndex(dragY);
            int lineY=Math.clamp(listTop+drop*22-(int)roleScroll.offset(),listTop,listBottom-1);
            g.fill(cx,lineY,cx+listWidth,lineY+2,t.borderArgb());
            int ghostY=(int)Math.clamp(dragY-9,listTop,listBottom-20);
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
    private void drawGangOverview(GuiGraphics g,Gang gang,net.minecraft.resources.ResourceLocation logo){
        // Identity content shares the full banner's axis, including its wider side margins.
        int center=left+109+(w-110)/2,logoY=cy,accent=theme().borderArgb();
        drawBanner(g,banner(),left+109,top+43,w-110,123);
        if(logo!=null)LogoTexture.draw(g,center-32,logoY,64,64);
        else{
            g.fill(center-32,logoY,center+32,logoY+64,0x18000000);
            g.renderOutline(center-32,logoY,64,64,accent);
            centeredText(g,"G",center,logoY+12,4.5f,accent);
        }
        int nameWidth=Math.min(cw-36,font.width(gang.name)+28);
        g.fill(center-nameWidth/2,cy+75,center+(nameWidth+1)/2,cy+100,0xDC181A22);
        g.renderOutline(center-nameWidth/2,cy+75,nameWidth,25,accent);
        g.drawCenteredString(font,font.plainSubstrByWidth(gang.name,nameWidth-18),center,cy+84,0xFFF2EDF3);
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
    private void drawBanner(GuiGraphics g,GangBanner banner,int x,int y,int width,int height){
        g.fill(x,y,x+width,y+height,0x22000000);
        g.fill(x,y,x+width,y+height,(banner.opacity()<<24)|banner.color());
        if(!banner.image().isEmpty()&&BannerTexture.get(banner.image())!=null)BannerTexture.draw(g,x,y,width,height,banner.opacity());
        g.fillGradient(x,y,x+width,y+height,0x00000000,0x44000000);g.flush();
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
    private int roleDropIndex(double y){return Math.clamp((int)Math.floor((y+roleScroll.offset()-listTop)/22),0,gang().roles.size()-1);}
    @Override public boolean mouseClicked(double x,double y,int button){
        positionListRows();
        x=layout.local(x);y=layout.local(y);
        if(memberCard!=null&&!memberCard.isMouseOver(x,y)){selectedMember="";rebuildWidgets();}
        String role="";
        if(button==0&&tab==GangCategory.ROLES.tabId()&&can(Permission.MANAGE_ROLES))for(var entry:roleButtons.entrySet()){
            if(entry.getValue().visible&&entry.getValue().isMouseOver(x,y)){role=entry.getKey();break;}
        }
        boolean result=super.mouseClicked(x,y,button);
        if(!role.isEmpty()){draggedRole=role;dragY=dragStartY=y;roleMoved=false;roleScrollTime=0;}return result;
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&!draggedRole.isEmpty()){
            if(roleMoved&&gang()!=null&&can(Permission.MANAGE_ROLES)){
                List<String> order=new ArrayList<>(gang().roles.keySet());int target=roleDropIndex(layout.local(y));
                order.remove(draggedRole);order.add(Math.min(target,order.size()),draggedRole);
                JsonObject args=new JsonObject();JsonArray ids=new JsonArray();order.forEach(ids::add);args.add("order",ids);send("reorder_roles",args);
            }
            draggedRole="";roleMoved=false;roleScrollTime=0;
        }
        return super.mouseReleased(layout.local(x),layout.local(y),button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0&&!draggedRole.isEmpty()){
            dragY=layout.local(y);roleMoved|=Math.abs(dragY-dragStartY)>3;
            scrollDraggedRole();positionListRows();
            return true;
        }
        return super.mouseDragged(layout.local(x),layout.local(y),button,layout.local(dx),layout.local(dy));
    }
    @Override public void mouseMoved(double x,double y){super.mouseMoved(layout.local(x),layout.local(y));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        x=layout.local(x);y=layout.local(y);
        if(activeList!=null&&x>=cx&&x<cx+(gang()==null?cw:Math.min(150,(cw-12)/2))&&y>=listTop&&y<listBottom){
            activeList.scroll(-vertical*23);positionListRows();return true;
        }
        if(gang()!=null&&tab==2)return super.mouseScrolled(x,y,horizontal,vertical);
        if(gang()!=null&&tab==4){
            if(x>=cx&&x<cx+cw&&y>=historyTop&&y<historyBottom){historyScroll.scroll(-vertical*24);return true;}
            return super.mouseScrolled(x,y,horizontal,vertical);
        }
        if(maxBodyScroll>0&&x>=cx && x<cx+cw && y>=cy && y<top+h-22){
            bodyScroll=Math.clamp(bodyScroll-(int)(vertical*18),0,maxBodyScroll);rebuildWidgets();return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers){
        if(memberCard!=null&&key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){selectedMember="";rebuildWidgets();return true;}
        if(gang()!=null&&tab==2&&!(getFocused() instanceof RegisterScrollBar)){
            switch(key){
                case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT -> TerrainClient.pan(-terrainRadius,0);
                case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT -> TerrainClient.pan(terrainRadius,0);
                case org.lwjgl.glfw.GLFW.GLFW_KEY_UP -> TerrainClient.pan(0,-terrainRadius);
                case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN -> TerrainClient.pan(0,terrainRadius);
                case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME -> TerrainClient.home();
                default -> {return super.keyPressed(key,scanCode,modifiers);}
            }
            return true;
        }
        if(ClientState.ready()&&gang()==null&&getFocused() instanceof EditBox&&(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER||key==org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)){
            if(find){search();return true;}
            if(createGangButton!=null&&createGangButton.active){createGangButton.onPress();return true;}
        }
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override public boolean isPauseScreen(){return false;}
}

