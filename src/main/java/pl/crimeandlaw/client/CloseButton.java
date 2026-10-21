package pl.crimeandlaw.client;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.GangDirectory.Theme;

/** Square close control with a centered, symmetric icon, independent of the font. */
final class CloseButton extends Button {
    private final Supplier<Theme> theme;
    CloseButton(int x,int y,OnPress action,Supplier<Theme> theme){
        super(x,y,20,20,Component.literal("Zamknij"),action,DEFAULT_NARRATION);this.theme=theme;
    }
    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta){
        Theme t=theme.get();int x=getX(),y=getY();
        g.fill(x,y,x+width,y+height,isHoveredOrFocused()?0x55000000|t.border():0x40262934);
        g.renderOutline(x,y,width,height,isHoveredOrFocused()?0x99000000|t.border():0x553E3C48);
        int color=active?0xFFE9E8EC:0xFF74747F;
        for(int i=0;i<8;i++){
            g.fill(x+6+i,y+6+i,x+7+i,y+7+i,color);
            g.fill(x+13-i,y+6+i,x+14-i,y+7+i,color);
        }
    }
}
