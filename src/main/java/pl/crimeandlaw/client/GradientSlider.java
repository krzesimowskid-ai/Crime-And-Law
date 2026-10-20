package pl.crimeandlaw.client;

import java.util.function.DoubleConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Vertical color channel: 100% at the top, 0% at the bottom. */
public final class GradientSlider extends AbstractWidget {
    private final String label;
    private final IntSupplier topColor,bottomColor;
    private final DoubleConsumer change;
    private final boolean checkerboard;
    private double value;
    public GradientSlider(int x,int y,String label,double value,IntSupplier topColor,IntSupplier bottomColor,boolean checkerboard,DoubleConsumer change){
        super(x,y,76,176,Component.empty());this.label=label;this.topColor=topColor;this.bottomColor=bottomColor;
        this.checkerboard=checkerboard;this.change=change;normalized(value);
    }
    public void normalized(double value){this.value=Math.clamp(value,0,1);setMessage(Component.literal(label+": "+Math.round(this.value*100)+"%"));}
    private void choose(double y){setValue(1-(y-(getY()+21))/130.0);}
    private void setValue(double next){normalized(next);change.accept(value);}
    @Override public void onClick(double x,double y,int button){choose(y);}
    @Override protected void onDrag(double x,double y,double dx,double dy){choose(y);}
    @Override public boolean keyPressed(int key,int scanCode,int modifiers){
        if(!active||!isFocused())return false;
        switch(key){
            case GLFW.GLFW_KEY_UP,GLFW.GLFW_KEY_RIGHT -> setValue(value+0.01);
            case GLFW.GLFW_KEY_DOWN,GLFW.GLFW_KEY_LEFT -> setValue(value-0.01);
            case GLFW.GLFW_KEY_HOME -> setValue(1);
            case GLFW.GLFW_KEY_END -> setValue(0);
            default -> {return super.keyPressed(key,scanCode,modifiers);}
        }
        return true;
    }
    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta){
        int x=getX()+27,y=getY()+21,w=22,h=130;var font=Minecraft.getInstance().font;
        g.drawCenteredString(font,label,getX()+width/2,getY()+3,0xFFBDB6C5);
        if(checkerboard)for(int row=0;row<h;row+=6)for(int col=0;col<w;col+=6)
            g.fill(x+col,y+row,x+Math.min(w,col+6),y+Math.min(h,row+6),(row/6+col/6)%2==0?0xFF62616B:0xFF32313C);
        g.fillGradient(x,y,x+w,y+h,topColor.getAsInt(),bottomColor.getAsInt());
        g.renderOutline(x-1,y-1,w+2,h+2,isHoveredOrFocused()?0xFFCDC4D4:0xFF696172);
        int mark=y+(int)Math.round((1-value)*h);
        g.fill(x-5,mark-2,x+w+5,mark+3,0xFF121018);
        g.fill(x-4,mark-1,x+w+4,mark+2,0xFFF1ECF4);
        g.drawCenteredString(font,Math.round(value*100)+"%",getX()+width/2,getY()+161,0xFFEAE5EF);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
}
