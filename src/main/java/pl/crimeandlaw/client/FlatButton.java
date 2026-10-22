package pl.crimeandlaw.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;
import pl.crimeandlaw.gang.GangDirectory.Theme;

public class FlatButton extends Button {
    private final Supplier<Theme> theme;
    private final boolean selected;
    private final boolean centered;
    private boolean centerText,primary,strongSelection;
    private String tooltipText="";
    public FlatButton(int x,int y,int w,int h,String text,OnPress action,Supplier<Theme> theme,boolean selected){
        this(x,y,w,h,text,action,theme,selected,false);
    }
    public FlatButton(int x,int y,int w,int h,String text,OnPress action,Supplier<Theme> theme,boolean selected,boolean centered){
        super(x,y,w,h,Component.literal(text),action,DEFAULT_NARRATION);this.theme=theme;this.selected=selected;this.centered=centered;this.centerText=centered;
    }
    public FlatButton centered(){centerText=true;return this;}
    public FlatButton primary(){primary=true;centerText=true;return this;}
    public FlatButton strongSelection(){strongSelection=true;return this;}
    public boolean isTextCentered(){return centerText;}
    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        Theme t=theme.get();int x=getX(),y=getY();
        boolean highlight=selected||active&&isHoveredOrFocused();
        g.fill(x,y,x+width,y+height,highlight?0x55000000|t.border():0x40262934);
        if(primary){
            int color=active?t.border():0x44414B;
            g.fillGradient(x+1,y+1,x+width-1,y+height-1,0xFF000000|color,0xFF000000|shade(color,.55f));
            if(active&&isHoveredOrFocused())g.fill(x+1,y+1,x+width-1,y+height-1,0x22FFFFFF);
            g.fill(x+2,y+2,x+width-2,y+3,active?0x44FFFFFF:0x226D6877);
        }
        if(!centered)g.renderOutline(x,y,width,height,highlight?0x99000000|t.border():0x553E3C48);
        if(selected){
            if(strongSelection){g.renderOutline(x,y,width,height,t.borderArgb());g.renderOutline(x+1,y+1,width-2,height-2,t.borderArgb());}
            else if(centered)g.fill(x,y+height-2,x+width,y+height,t.borderArgb());
            else g.fill(x,y,x+2,y+height,t.borderArgb());
        }
        renderLabel(g,x,y);
    }
    private static int shade(int color,float factor){return ((int)((color>>16&255)*factor)<<16)|((int)((color>>8&255)*factor)<<8)|(int)((color&255)*factor);}
    protected void renderLabel(GuiGraphics g,int x,int y){
        var font=Minecraft.getInstance().font;
        String full=getMessage().getString();boolean clipped=font.width(full)>width-10;
        String label=clipped?font.plainSubstrByWidth(full,Math.max(0,width-10-font.width("…")))+"…":full;
        String nextTooltip=clipped?full:"";
        if(!nextTooltip.equals(tooltipText)){tooltipText=nextTooltip;setTooltip(clipped?Tooltip.create(getMessage()):null);}
        g.drawString(font,label,centerText?x+(width-font.width(label))/2:x+5,y+(height-8)/2,active?0xFFE9E8EC:0xFF74747F,false);
    }
}

