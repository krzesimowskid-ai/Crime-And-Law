package pl.crimeandlaw.client;

import com.google.gson.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
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
    private boolean find,requests;
    private String query="",draftName="",file="",draftLogo="",selectedMember="",selectedRole="member",roleName="";
    private String background="15161C",border="A3425D",lastGang="";
    private int opacity=235;
    private EnumSet<Permission> rolePermissions=EnumSet.noneOf(Permission.class);
    public GangScreen(){super(Component.literal("Crime And Law"));}
    private Gang gang(){return ClientState.gang();}
    private boolean can(Permission p){
        Gang g=gang();if(g==null)return false;if(g.owner.equals(ClientState.player()))return true;
        Member m=g.members.get(ClientState.player());Role role=m==null?null:g.roles.get(m.role);
        return role!=null && role.permissions.contains(p);
    }
    private Theme theme(){Gang g=gang();if(g==null)return Theme.DEFAULT;Theme local=PersonalTheme.get(g.id.toString());return local==null?g.theme:local;}
    public void updated(){
        Gang g=gang();String id=g==null?"":g.id.toString();
        if(!id.equals(lastGang)){
            lastGang=id;tab=0;selectedMember="";selectedRole="member";memberPage=0;roleName="";
            Theme t=theme();background=String.format("%06X",t.background());border=String.format("%06X",t.border());opacity=t.opacity();
            draftLogo="";
        }
        rebuildWidgets();
    }
    @Override protected void init(){
        w=Math.min(620,width-20);h=Math.min(360,height-20);left=(width-w)/2;top=(height-h)/2;
        cx=left+116;cy=top+55;cw=w-130;
        labels.clear();if(width<340 || height<235)return;
        button(left+w-25,top+10,16,"×",b->onClose());
        Gang g=gang();
        if(g==null){
            button(left+10,cy,96,"Stwórz gang",b->{find=false;rebuildWidgets();},!find);
            button(left+10,cy+24,96,"Znajdź gang",b->{find=true;ClientState.refresh(query,0);rebuildWidgets();},find);
            if(find)buildFind();else buildCreate();
        }else{
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
        label(cx,cy,"Stwórz swój gang");label(cx,cy+22,"Nazwa (3–32 znaki)",0xFFAAAAB7);
        field(cx,cy+36,Math.min(cw,280),32,draftName,v->draftName=v);
        label(cx,cy+68,"Własne logo — ścieżka do pliku PNG",0xFFAAAAB7);
        field(cx,cy+82,cw,240,file,v->file=v);
        button(cx,cy+106,112,"Wczytaj PNG",b->loadLogo());
        label(cx,cy+135,"Logo zostanie dopasowane do 64×64.",0xFFAAAAB7);
        button(cx,cy+158,Math.min(cw,190),"Utwórz gang",b->{
            JsonObject a=new JsonObject();a.addProperty("name",draftName);a.addProperty("logo",draftLogo);send("create",a);
        });
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
            button(cx,cy+26+index++*22,listWidth,r.name,b->{selectedRole=r.id;roleName=r.name;rolePermissions=r.permissions.isEmpty()?EnumSet.noneOf(Permission.class):EnumSet.copyOf(r.permissions);rebuildWidgets();},selectedRole.equals(r.id));
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
    }
    private void buildMap(){
        label(cx,cy,"Mapa — kolejny etap");label(cx,cy+26,"Tu pojawią się chunki i tereny gangów.",0xFFAAAAB7);
        label(cx,cy+47,"Planowane: ręczne oznaczenia prywatne i wspólne.",0xFFAAAAB7);
        label(cx,cy+68,"W tej wersji nie ma jeszcze przejmowania ziemi.",0xFFAAAAB7);
    }
    private Theme draftTheme(){return new Theme(Integer.parseInt(background.replace("#",""),16),Integer.parseInt(border.replace("#",""),16),opacity);}
    private void buildSettings(){
        label(cx,cy,"Wygląd menu");int half=Math.max(60,(cw-10)/2);
        label(cx,cy+22,"Tło — HEX",0xFFAAAAB7);label(cx+half+10,cy+22,"Obramówka — HEX",0xFFAAAAB7);
        field(cx,cy+36,half,7,background,v->background=v);field(cx+half+10,cy+36,half,7,border,v->border=v);
        addRenderableWidget(new AbstractSliderButton(cx,cy+62,cw,18,Component.empty(),opacity/255.0){
            {updateMessage();}
            @Override protected void updateMessage(){setMessage(Component.literal("Krycie tła: "+Math.round(value*100)+"%"));}
            @Override protected void applyValue(){opacity=(int)Math.round(value*255);}
        });
        button(cx,cy+90,half,"Zapisz tylko u mnie",b->{try{if(PersonalTheme.set(gang().id.toString(),draftTheme()))ClientState.message="Zapisano osobisty wygląd.";}catch(Exception e){ClientState.message="Podaj kolor jako 6 znaków HEX, np. A3425D.";}});
        button(cx+half+10,cy+90,half,"Zapisz dla gangu",b->{try{Theme t=draftTheme();JsonObject a=new JsonObject();a.addProperty("background",t.background());a.addProperty("border",t.border());a.addProperty("opacity",t.opacity());send("theme",a);}catch(Exception e){ClientState.message="Nieprawidłowy kolor HEX.";}}).active=can(Permission.MANAGE_THEME);
        button(cx,cy+114,cw,"Używaj wyglądu gangu",b->{PersonalTheme.set(gang().id.toString(),null);Theme t=gang().theme;background=String.format("%06X",t.background());border=String.format("%06X",t.border());opacity=t.opacity();rebuildWidgets();});
        label(cx,cy+145,"Logo gangu — ścieżka do PNG",0xFFAAAAB7);
        field(cx,cy+159,cw,240,file,v->file=v);
        button(cx,cy+183,half,"Wczytaj PNG",b->loadLogo()).active=can(Permission.MANAGE_THEME);
        button(cx+half+10,cy+183,half,"Zapisz logo gangu",b->{JsonObject a=new JsonObject();a.addProperty("logo",draftLogo);send("logo",a);}).active=can(Permission.MANAGE_THEME)&&!draftLogo.isBlank();
    }
    private void loadLogo(){
        final String path=file;ClientState.message="Wczytywanie logo…";
        java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try{
                Path p=Path.of(path.replace("\"","").strip());if(Files.size(p)>5_000_000)throw new IOException("Plik przekracza 5 MB.");
                try(var in=ImageIO.createImageInputStream(p.toFile())){
                    var readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw new IOException("Nieprawidłowy obraz.");
                    var reader=readers.next();
                    try{reader.setInput(in);if(!reader.getFormatName().equalsIgnoreCase("png")||reader.getWidth(0)>4096||reader.getHeight(0)>4096)throw new IOException("Wybierz PNG do 4096×4096.");
                        BufferedImage original=reader.read(0);BufferedImage image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
                        var graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        double scale=Math.min(64.0/original.getWidth(),64.0/original.getHeight());int iw=Math.max(1,(int)(original.getWidth()*scale)),ih=Math.max(1,(int)(original.getHeight()*scale));
                        graphics.drawImage(original,(64-iw)/2,(64-ih)/2,iw,ih,null);graphics.dispose();
                        ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
                        return GangLogo.validate(Base64.getEncoder().encodeToString(out.toByteArray()));
                    }finally{reader.dispose();}
                }
            }catch(Exception e){throw new java.util.concurrent.CompletionException(e);}
        }).whenComplete((encoded,error)->Minecraft.getInstance().execute(()->{
            if(error!=null)ClientState.message="Nie wczytano logo. Wybierz istniejący plik PNG do 5 MB.";
            else{draftLogo=encoded;ClientState.message="Logo wczytane. Zapisz gang lub logo.";}
            if(minecraft.screen==this)rebuildWidgets();
        }));
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta){
        g.fill(0,0,width,height,0x55000000);
        if(width<340||height<235){g.drawCenteredString(font,"Zmniejsz skalę GUI w ustawieniach Minecrafta.",width/2,height/2,0xFFFFFFFF);return;}
        Theme t=theme();g.fill(left,top,left+w,top+h,t.backgroundArgb());
        g.renderOutline(left,top,w,h,t.borderArgb());g.fill(left+1,top+42,left+w-1,top+43,t.borderArgb());
        g.fill(left+108,top+43,left+109,top+h-22,t.borderArgb());
        Gang gang=gang();String logo=!draftLogo.isBlank()?draftLogo:gang==null?"":gang.logo;
        var texture=LogoTexture.get(logo);
        if(texture!=null)g.blit(texture,left+10,top+8,0,0,28,28,28,28);
        else{g.renderOutline(left+10,top+8,28,28,t.borderArgb());g.drawCenteredString(font,"G",left+24,top+18,t.borderArgb());}
        String name=gang==null?"Crime And Law":gang.name;
        g.drawString(font,font.plainSubstrByWidth(name,w-98),left+48,top+10,0xFFF0EDF1,false);
        String subtitle="Stwórz gang lub znajdź swoją ekipę";
        if(gang!=null){Member me=gang.members.get(ClientState.player());subtitle="Twoja ranga: "+gang.roles.get(me.role).name;}
        g.drawString(font,font.plainSubstrByWidth(subtitle,w-98),left+48,top+25,0xFFAAAAB7,false);
        g.enableScissor(cx,cy,cx+cw,top+h-24);
        for(Label line:labels)g.drawString(font,font.plainSubstrByWidth(line.value(),line.width()),line.x(),line.y()-bodyScroll,line.color(),false);
        g.disableScissor();
        super.render(g,mouseX,mouseY,delta);
        String message=ClientState.message;g.drawString(font,font.plainSubstrByWidth(message,w-20),left+10,top+h-14,0xFFD4C4CC,false);
        if(!message.isBlank()&&mouseY>=top+h-22&&mouseY<top+h)g.renderTooltip(font,Component.literal(message),mouseX,mouseY);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(x>=cx && x<cx+cw && y>=cy && y<top+h-22){
            bodyScroll=Math.clamp(bodyScroll-(int)(vertical*18),0,maxBodyScroll);rebuildWidgets();return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean isPauseScreen(){return false;}
}
