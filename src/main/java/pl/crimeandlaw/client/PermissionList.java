package pl.crimeandlaw.client;

import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.*;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.GangDirectory.*;

/** A bounded, searchable catalog; planned rights can never be toggled. */
final class PermissionList extends AbstractWidget {
    static final int ROW=26;
    static final class State {
        final ScrollPosition scroll=new ScrollPosition();
        String query="";PermissionGroup group=PermissionGroup.ALL;
        PermissionGroup nextGroup(){group=PermissionGroup.values()[(group.ordinal()+1)%PermissionGroup.values().length];scroll.reset();return group;}
    }
    private final State state;
    private final Set<Permission> values;
    private final BooleanSupplier editable;
    private final Supplier<Theme> theme;
    private final List<Permission> filtered=new ArrayList<>();
    private final Map<Permission,Tooltip> tooltips=new EnumMap<>(Permission.class);
    private int selected;
    private RegisterScrollBar scrollbar;
    private Predicate<Permission> grantable=p->true;
    PermissionList grantable(Predicate<Permission> predicate){grantable=predicate;return this;}
    void scrollbar(RegisterScrollBar scrollbar){this.scrollbar=scrollbar;updateScrollbar();}
    private void updateScrollbar(){if(scrollbar!=null){scrollbar.active=state.scroll.maximum()>0;scrollbar.visible=scrollbar.active;}}
    PermissionList(int x,int y,int width,int height,State state,Set<Permission> values,BooleanSupplier editable,Supplier<Theme> theme){
        super(x,y,width,height,Component.literal("Lista uprawnień"));this.state=state;this.values=values;this.editable=editable;this.theme=theme;
        for(var permission:Permission.values())tooltips.put(permission,Tooltip.create(Component.literal(permission.label+(permission.available()?"":" — W przygotowaniu"))));
        filter();
    }
    void filter(){
        filtered.clear();String query=state.query.strip().toLowerCase(Locale.ROOT);
        for(var p:Permission.values())if((state.group==PermissionGroup.ALL||p.group==state.group)&&p.label.toLowerCase(Locale.ROOT).contains(query))filtered.add(p);
        selected=Math.clamp(selected,0,Math.max(0,filtered.size()-1));state.scroll.range(height,filtered.size()*ROW);
        updateScrollbar();
    }
    private int index(double y){return (int)((y-getY()+state.scroll.offset())/ROW);}
    private boolean enabled(Permission p){return p.available()&&editable.getAsBoolean()&&grantable.test(p);}
    @Override public void onClick(double x,double y,int button){int i=index(y);if(i>=0&&i<filtered.size()){selected=i;toggle();}}
    private void toggle(){if(filtered.isEmpty())return;var p=filtered.get(selected);if(enabled(p)&&!values.remove(p))values.add(p);}
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
        var font=Minecraft.getInstance().font;
        var layout=GangLayout.of(Minecraft.getInstance().screen.width,Minecraft.getInstance().screen.height);
        g.pose().pushPose();g.pose().last().pose().identity();
        g.enableScissor((int)Math.floor(getX()*layout.scale()),(int)Math.floor(getY()*layout.scale()),(int)Math.ceil((getX()+width)*layout.scale()),(int)Math.ceil((getY()+height)*layout.scale()));g.pose().popPose();
        Tooltip tooltip=null;
        for(int i=0;i<filtered.size();i++){
            var p=filtered.get(i);int x=getX(),y=getY()+i*ROW-(int)state.scroll.offset();if(y+ROW<=getY()||y>=getY()+height)continue;
            boolean hover=mx>=x&&mx<x+width&&my>=Math.max(y,getY())&&my<Math.min(y+23,getY()+height);
            boolean enabled=enabled(p),checked=p.available()&&values.contains(p);
            g.fill(x,y,x+width,y+23,hover?0xFF292330:0x33101016);
            if(isFocused()&&i==selected)g.renderOutline(x,y,width,23,theme.get().borderArgb());
            int available=width-(p.available()?43:85);String label=p.label;
            if(font.width(label)>available)label=font.plainSubstrByWidth(label,available-font.width("…"))+"…";
            g.drawString(font,label,x+6,y+7,enabled?0xFFE9E8EC:0xFF8B8491,false);
            if(!p.available())g.drawString(font,"Wkrótce",x+width-78,y+8,0xFF756E7E,false);
            int sx=x+width-29,sy=y+7;g.fill(sx,sy,sx+23,sy+10,checked?theme.get().borderArgb():0xFF484551);
            int knob=sx+(checked?14:2);g.fill(knob,sy+2,knob+7,sy+8,enabled?0xFFF2EDF3:0xFF77717D);
            if(hover)tooltip=p.available()&&!grantable.test(p)?Tooltip.create(Component.literal(p.label+" — Nie możesz nadać uprawnienia, którego nie posiadasz.")):tooltips.get(p);
        }
        if(filtered.isEmpty())g.drawString(font,"Brak pasujących uprawnień.",getX()+6,getY()+8,0xFFAAA3B3,false);
        g.disableScissor();setTooltip(tooltip);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(!isMouseOver(x,y))return false;state.scroll.scroll(-vertical*ROW);return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==264||key==265){selected=Math.clamp(selected+(key==264?1:-1),0,Math.max(0,filtered.size()-1));double y=selected*ROW;if(y<state.scroll.offset())state.scroll.scroll(y-state.scroll.offset());else if(y+ROW>state.scroll.offset()+height)state.scroll.scroll(y+ROW-height-state.scroll.offset());return true;}
        if(key==32||key==257||key==335){toggle();return true;}return super.keyPressed(key,scan,modifiers);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput out){
        var p=filtered.isEmpty()?null:filtered.get(selected);out.add(NarratedElementType.TITLE,Component.literal(p==null?"Brak uprawnień":p.label+(!p.available()?", w przygotowaniu":values.contains(p)?", włączone":", wyłączone")));
    }
}
