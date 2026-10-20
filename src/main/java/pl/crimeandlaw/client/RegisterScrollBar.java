package pl.crimeandlaw.client;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import pl.crimeandlaw.gang.GangDirectory.Theme;

/** The only draggable control in the history viewport; page controls stay outside. */
public final class RegisterScrollBar extends AbstractWidget {
    private final ScrollPosition position;
    private final Supplier<Theme> theme;
    private double grab;
    public RegisterScrollBar(int x,int y,int height,ScrollPosition position,Supplier<Theme> theme){
        super(x,y,10,height,Component.literal("Przewijanie rejestru"));this.position=position;this.theme=theme;active=position.maximum()>0;
    }
    @Override public void onClick(double x,double y,int button){
        double local=y-getY(),top=position.thumbTop();
        grab=local>=top&&local<top+position.thumbHeight()?local-top:position.thumbHeight()/2.0;
        position.moveThumb(local-grab);
    }
    @Override protected void onDrag(double x,double y,double dx,double dy){position.moveThumb(y-getY()-grab);}
    @Override public boolean keyPressed(int key,int scanCode,int modifiers){
        if(!active||!isFocused())return false;
        switch(key){
            case GLFW.GLFW_KEY_UP -> position.scroll(-24);
            case GLFW.GLFW_KEY_DOWN -> position.scroll(24);
            case GLFW.GLFW_KEY_PAGE_UP -> position.scroll(-position.viewport());
            case GLFW.GLFW_KEY_PAGE_DOWN -> position.scroll(position.viewport());
            case GLFW.GLFW_KEY_HOME -> position.reset();
            case GLFW.GLFW_KEY_END -> position.set(position.maximum());
            default -> {return super.keyPressed(key,scanCode,modifiers);}
        }
        return true;
    }
    @Override protected void renderWidget(GuiGraphics g,int x,int y,float delta){
        int left=getX()+2,right=getX()+8,top=getY()+position.thumbTop(),accent=theme.get().border();
        g.fill(left,getY(),right,getY()+height,0x50312D3B);
        g.fill(left,top,right,top+position.thumbHeight(),active?(isHoveredOrFocused()?0xFF000000:0xBB000000)|accent:0x553E3948);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
}
