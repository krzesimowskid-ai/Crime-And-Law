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
        g.fill(x+9,y+3,x+13,y+7,c);g.fill(x+7,y+8,x+15,y+12,c);
        g.fill(x+9,y+12,x+11,y+16,c);g.fill(x+12,y+12,x+14,y+16,c);
    }
}
