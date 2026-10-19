package pl.crimeandlaw.client;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.GangDirectory.Theme;

public final class ValueSlider extends AbstractSliderButton {
    private final String label;
    private final DoubleConsumer change;
    private final Supplier<Theme> theme;
    private DoubleFunction<String> format=v->Math.round(v*100)+"%";
    public ValueSlider(int x,int y,int width,String label,double value,Supplier<Theme> theme,DoubleConsumer change){
        super(x,y,width,27,Component.empty(),value);this.label=label;this.theme=theme;this.change=change;updateMessage();
    }
    public void normalized(double value){this.value=Math.clamp(value,0,1);updateMessage();}
    public ValueSlider display(DoubleFunction<String> format){this.format=format;updateMessage();return this;}
    @Override protected void updateMessage(){if(label!=null&&format!=null)setMessage(Component.literal(label+": "+format.apply(value)));}
    @Override protected void applyValue(){change.accept(value);}
    @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta){
        int x=getX(),y=getY(),accent=theme.get().borderArgb();
        g.drawString(Minecraft.getInstance().font,getMessage(),x,y,0xFFE6E3EA,false);
        g.fill(x,y+17,x+width,y+21,0xFF30303B);
        int knob=x+(int)Math.round(value*(width-5));g.fill(x,y+17,knob+3,y+21,accent);
        g.fill(knob,y+14,knob+5,y+24,isHoveredOrFocused()?0xFFF0EAF0:accent);
    }
}
