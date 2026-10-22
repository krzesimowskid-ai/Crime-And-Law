package pl.crimeandlaw.client;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.GangDirectory.Theme;

final class PlayerHomeButton extends FlatButton {
    PlayerHomeButton(int x,int y,Runnable action,Supplier<Theme> theme,boolean following){
        super(x,y,22,19,"Wróć do gracza",b->action.run(),theme,following);
        strongSelection();setTooltip(Tooltip.create(Component.literal("Wróć do gracza")));
    }
    @Override protected void renderLabel(GuiGraphics g,int x,int y){
        int c=active?0xFFE9E8EC:0xFF74747F;
        int center=x+width/2;
        g.fill(center-2,y+3,center+2,y+7,c);
        g.fill(center-4,y+8,center+4,y+10,c);g.fill(center-3,y+10,center+3,y+12,c);
        g.fill(center-3,y+12,center-1,y+16,c);g.fill(center+1,y+12,center+3,y+16,c);
    }
}
