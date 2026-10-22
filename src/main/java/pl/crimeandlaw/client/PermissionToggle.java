package pl.crimeandlaw.client;

import java.util.function.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import pl.crimeandlaw.gang.GangDirectory.Theme;

/** Permission label stays left aligned; the switch has explicit state narration. */
final class PermissionToggle extends FlatButton {
    private final BooleanSupplier value;
    private final Supplier<Theme> theme;
    private final String label;
    PermissionToggle(int x,int y,int width,String label,BooleanSupplier value,Runnable toggle,Supplier<Theme> theme){
        super(x,y,width,21,label,b->toggle.run(),theme,false);this.label=label;this.value=value;this.theme=theme;
        setTooltip(Tooltip.create(Component.literal(label)));
    }
    @Override protected void renderLabel(GuiGraphics g,int x,int y){
        var font=Minecraft.getInstance().font;int color=active?0xFFE9E8EC:0xFF74747F;
        g.drawString(font,font.plainSubstrByWidth(label,width-45),x+7,y+6,color,false);
        int sx=x+width-32,sy=y+6;boolean checked=value.getAsBoolean();
        g.fill(sx,sy,sx+24,sy+10,checked?theme.get().borderArgb():0xFF484551);
        int knob=sx+(checked?15:2);g.fill(knob,sy+2,knob+7,sy+8,active?0xFFF2EDF3:0xFFAAA3B3);
    }
    @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage(){
        return Component.literal(label+": "+(value.getAsBoolean()?"włączone":"wyłączone"));
    }
}
