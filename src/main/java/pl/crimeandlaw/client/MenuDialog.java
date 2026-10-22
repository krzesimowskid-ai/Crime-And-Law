package pl.crimeandlaw.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.GangDirectory.Theme;

abstract class MenuDialog extends Screen {
    protected final GangScreen parent;
    protected GangLayout layout;
    protected int left,top,panelWidth=520,panelHeight=340;
    protected MenuDialog(GangScreen parent,String title){super(Component.literal(title));this.parent=parent;}
    protected Theme theme(){return parent.currentTheme();}
    @Override protected void init(){
        layout=GangLayout.of(width,height);left=(layout.canvasWidth()-panelWidth)/2;top=(layout.canvasHeight()-panelHeight)/2;
        parent.resize(minecraft,width,height);build();
        addRenderableWidget(new CloseButton(left+panelWidth-28,top+8,b->onClose(),this::theme));
    }
    protected abstract void build();
    protected abstract void paint(GuiGraphics g,int x,int y,float delta);
    @Override public void renderBackground(GuiGraphics g,int x,int y,float delta){}
    @Override public void render(GuiGraphics g,int x,int y,float delta){
        parent.render(g,-100,-100,delta);g.flush();RenderSystem.depthMask(true);RenderSystem.clear(256,Minecraft.ON_OSX);
        g.pose().pushPose();g.pose().scale(layout.scale(),layout.scale(),1);
        try {
            g.fill(0,0,layout.canvasWidth(),layout.canvasHeight(),0xAA000000);
            g.fill(left,top,left+panelWidth,top+panelHeight,0xFF181A23);
            g.fillGradient(left,top,left+panelWidth,top+36,0xFF29212F,0xFF191B24);
            g.renderOutline(left,top,panelWidth,panelHeight,theme().borderArgb());
            g.drawString(font,title,left+16,top+13,0xFFF0EDF2,false);
            int mx=(int)layout.local(x),my=(int)layout.local(y);paint(g,mx,my,delta);
            // Preview blits and text must not occlude the dialog's controls.
            g.flush();RenderSystem.depthMask(true);RenderSystem.clear(256,Minecraft.ON_OSX);
            super.render(g,mx,my,delta);
        }finally{g.pose().popPose();}
    }
    protected FlatButton button(int x,int y,int w,String text,net.minecraft.client.gui.components.Button.OnPress action){
        return addRenderableWidget(new FlatButton(x,y,w,21,text,action,this::theme,false).centered());
    }
    protected void scissor(GuiGraphics g,int x,int y,int w,int h){
        g.enableScissor((int)(x*layout.scale()),(int)(y*layout.scale()),(int)Math.ceil((x+w)*layout.scale()),(int)Math.ceil((y+h)*layout.scale()));
    }
    @Override public boolean mouseClicked(double x,double y,int button){return click(layout.local(x),layout.local(y),button);}
    protected boolean click(double x,double y,int button){return super.mouseClicked(x,y,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return release(layout.local(x),layout.local(y),button);}
    protected boolean release(double x,double y,int button){return super.mouseReleased(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return drag(layout.local(x),layout.local(y),button,layout.local(dx),layout.local(dy));}
    protected boolean drag(double x,double y,int button,double dx,double dy){return super.mouseDragged(x,y,button,dx,dy);}
    @Override public void mouseMoved(double x,double y){super.mouseMoved(layout.local(x),layout.local(y));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return scroll(layout.local(x),layout.local(y),horizontal,vertical);}
    protected boolean scroll(double x,double y,double horizontal,double vertical){return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
