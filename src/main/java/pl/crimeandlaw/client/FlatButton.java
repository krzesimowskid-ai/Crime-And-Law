package pl.crimeandlaw.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;
import pl.crimeandlaw.gang.GangDirectory.Theme;

public final class FlatButton extends Button {
    private final Supplier<Theme> theme;
    private final boolean selected;
    private final boolean centered;
    private String tooltipText="";
    public FlatButton(int x,int y,int w,int h,String text,OnPress action,Supplier<Theme> theme,boolean selected){
        this(x,y,w,h,text,action,theme,selected,false);
    }
    public FlatButton(int x,int y,int w,int h,String text,OnPress action,Supplier<Theme> theme,boolean selected,boolean centered){
        super(x,y,w,h,Component.literal(text),action,DEFAULT_NARRATION);this.theme=theme;this.selected=selected;this.centered=centered;
    }
    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        Theme t=theme.get();int x=getX(),y=getY();
        boolean highlight=selected||active&&isHoveredOrFocused();
        g.fill(x,y,x+width,y+height,highlight?0x55000000|t.border():0x40262934);
        if(!centered)g.renderOutline(x,y,width,height,highlight?0x99000000|t.border():0x553E3C48);
        if(selected){
            if(centered)g.fill(x,y+height-2,x+width,y+height,t.borderArgb());
            else g.fill(x,y,x+2,y+height,t.borderArgb());
        }
        var font=Minecraft.getInstance().font;
        String full=getMessage().getString();boolean clipped=font.width(full)>width-10;
        String label=clipped?font.plainSubstrByWidth(full,Math.max(0,width-10-font.width("…")))+"…":full;
        String nextTooltip=clipped?full:"";
        if(!nextTooltip.equals(tooltipText)){tooltipText=nextTooltip;setTooltip(clipped?Tooltip.create(getMessage()):null);}
        g.drawString(font,label,centered?x+(width-font.width(label))/2:x+5,y+(height-8)/2,active?0xFFE9E8EC:0xFF74747F,false);
    }
}

