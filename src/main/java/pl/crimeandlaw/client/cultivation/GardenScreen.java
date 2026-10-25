package pl.crimeandlaw.client.cultivation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import pl.crimeandlaw.client.FlatButton;
import pl.crimeandlaw.gang.GangDirectory;

/** The complete panel scales with small windows; long catalog content scrolls. */
abstract class GardenScreen extends Screen {
    protected float scale,left,top;
    protected static final int INK=0xFFF2E8CA,MUTED=0xFFA6BBB0,GREEN=0xFFC8DEA2;
    private static final ResourceLocation FONT=ResourceLocation.fromNamespaceAndPath("crimeandlaw","chat");
    protected GardenScreen(String title){super(Component.literal(title));}
    @Override protected void init(){scale=Math.min(1,Math.min(width/920f,height/550f));left=(width/scale-900)/2;top=(height/scale-520)/2;}
    protected Component text(String s){return Component.literal(s).withStyle(style->style.withFont(FONT));}
    protected void label(GuiGraphics g,String s,float x,float y,int c){g.drawString(font,text(s),Math.round(left+x),Math.round(top+y),c,false);}
    protected void wrapped(GuiGraphics g,String s,int x,int y,int w,int color,int lines){var parts=font.split(text(s),w);for(int i=0;i<Math.min(lines,parts.size());i++)g.drawString(font,parts.get(i),(int)left+x,(int)top+y+i*16,color,false);}
    protected FlatButton button(int x,int y,int w,String name,Runnable run){return addRenderableWidget(new FlatButton((int)left+x,(int)top+y,w,26,name,b->run.run(),()->new GangDirectory.Theme(0x14231F,0x71965B,240),false).primary());}
    protected void background(GuiGraphics g,String title,String subtitle){g.fill(0,0,width,height,0xBC070F10);g.pose().pushPose();g.pose().scale(scale,scale,1);int x=(int)left,y=(int)top;g.fill(x+4,y+6,x+904,y+526,0x70000000);g.fillGradient(x,y,x+900,y+520,0xFC172B2B,0xFC101B22);g.renderOutline(x,y,900,520,0xFF496F66);g.fill(x,y,x+900,y+2,0xFFB4CE8A);label(g,title,29,22,INK);label(g,subtitle,29,44,MUTED);}
    protected void end(GuiGraphics g,int mx,int my,float delta){super.render(g,(int)(mx/scale),(int)(my/scale),delta);g.pose().popPose();}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float delta){}
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(x/scale,y/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/scale,y/scale,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged(x/scale,y/scale,b,dx/scale,dy/scale);}
    @Override public void onClose(){minecraft.setScreen(null);}
    @Override public boolean isPauseScreen(){return false;}
}
