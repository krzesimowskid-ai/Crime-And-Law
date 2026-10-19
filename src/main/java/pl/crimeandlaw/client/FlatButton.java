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
    public FlatButton(int x,int y,int w,int h,String text,OnPress action,Supplier<Theme> theme,boolean selected){
        super(x,y,w,h,Component.literal(text),action,DEFAULT_NARRATION);this.theme=theme;this.selected=selected;
        if(Minecraft.getInstance().font.width(text)>w-10)setTooltip(Tooltip.create(Component.literal(text)));
    }
    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        Theme t=theme.get();int x=getX(),y=getY();
        g.fill(x,y,x+width,y+height,selected||isHoveredOrFocused()?0x55000000|t.border():0x55101016);
        if(selected)g.fill(x,y,x+2,y+height,t.borderArgb());
        var font=Minecraft.getInstance().font;
        String label=font.plainSubstrByWidth(getMessage().getString(),Math.max(0,width-10));
        g.drawString(font,label,x+5,y+(height-8)/2,active?0xFFE9E8EC:0xFF74747F,false);
    }
}
