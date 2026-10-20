package pl.crimeandlaw.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ColorPickerScreen extends MenuDialog {
    private static final ResourceLocation WHEEL=ResourceLocation.fromNamespaceAndPath("crimeandlaw","color_wheel");
    private final BiConsumer<Integer,Integer> accepted;
    private final boolean transparency;
    private final int originalRgb,originalAlpha;
    private double hue,saturation,value,alpha;
    private boolean registered,dragging,syncing;
    private int wx,wy;
    private final int diameter=168;
    private EditBox hex,red,green,blue;
    private GradientSlider brightness,sat,opacity;
    private FlatButton apply;
    public ColorPickerScreen(GangScreen parent,String title,int rgb,int alpha,boolean transparency,BiConsumer<Integer,Integer> accepted){
        super(parent,title);this.accepted=accepted;this.transparency=transparency;originalRgb=rgb;originalAlpha=alpha;
        double[] hsv=ColorValues.hsv(rgb);hue=hsv[0];saturation=hsv[1];value=hsv[2];this.alpha=alpha/255.0;
    }
    private int rgb(){return ColorValues.rgb(hue,saturation,value);}
    @Override protected void build(){
        wx=left+30;wy=top+54;
        if(!registered){
            NativeImage image=new NativeImage(diameter,diameter,false);
            for(int y=0;y<diameter;y++)for(int x=0;x<diameter;x++){
                double dx=(x+0.5-diameter/2.0)/(diameter/2.0),dy=(y+0.5-diameter/2.0)/(diameter/2.0),distance=Math.sqrt(dx*dx+dy*dy);
                int color=ColorValues.rgb(Math.atan2(dy,dx)/(2*Math.PI),Math.min(1,distance),1);
                int rgba=distance<=1?0xFF000000|(color&255)<<16|(color&0xFF00)|(color>>16&255):0;
                image.setPixelRGBA(x,y,rgba);
            }
            minecraft.getTextureManager().register(WHEEL,new DynamicTexture(image));registered=true;
        }
        hex=field(left+24,top+254,120,7);hex.setResponder(s->{if(syncing)return;try{String v=s.replace("#","");if(v.length()==6){setRgb(Integer.parseInt(v,16));sync(false);}}catch(NumberFormatException ignored){}});
        red=field(left+170,top+254,56,3);green=field(left+236,top+254,56,3);blue=field(left+302,top+254,56,3);
        for(EditBox box:new EditBox[]{red,green,blue})box.setResponder(s->{if(syncing)return;try{
            int r=Integer.parseInt(red.getValue()),g=Integer.parseInt(green.getValue()),b=Integer.parseInt(blue.getValue());
            if(r>=0&&r<=255&&g>=0&&g<=255&&b>=0&&b<=255){setRgb(r<<16|g<<8|b);sync(false);}
        }catch(NumberFormatException ignored){}});
        brightness=addRenderableWidget(new GradientSlider(left+230,top+49,"Jasność",value,()->0xFF000000|ColorValues.rgb(hue,saturation,1),()->0xFF000000,false,v->{value=v;sync(true);}));
        sat=addRenderableWidget(new GradientSlider(left+312,top+49,"Nasycenie",saturation,()->0xFF000000|ColorValues.rgb(hue,1,value),()->0xFF000000|ColorValues.rgb(hue,0,value),false,v->{saturation=v;sync(true);}));
        if(transparency)opacity=addRenderableWidget(new GradientSlider(left+394,top+49,"Krycie",alpha,()->0xFF000000|rgb(),this::rgb,true,v->{alpha=v;sync(true);}));
        button(left+18,top+panelHeight-35,90,"Anuluj",b->onClose());
        button(left+116,top+panelHeight-35,100,"Przywróć",b->{setRgb(originalRgb);alpha=originalAlpha/255.0;sync(true);});
        apply=button(left+panelWidth-138,top+panelHeight-35,120,"Zastosuj",b->{
            try{String text=hex.getValue().replace("#","");if(text.length()!=6)return;int rgb=Integer.parseInt(text,16);
                accepted.accept(rgb,(int)Math.round(alpha*255));minecraft.setScreen(parent);
            }catch(NumberFormatException ignored){}
        });sync(true);
    }
    private EditBox field(int x,int y,int w,int max){EditBox box=new EditBox(font,x,y,w,19,Component.literal("Kolor"));box.setMaxLength(max);return addRenderableWidget(box);}
    private void setRgb(int rgb){double[] hsv=ColorValues.hsv(rgb);if(hsv[1]>0)hue=hsv[0];saturation=hsv[1];value=hsv[2];}
    private void sync(boolean fields){
        syncing=true;int color=rgb();
        if(fields){hex.setValue(String.format("%06X",color));red.setValue(""+(color>>16&255));green.setValue(""+(color>>8&255));blue.setValue(""+(color&255));}
        else { // Keep the field currently being typed; synchronize the others on focus loss.
            if(!hex.isFocused())hex.setValue(String.format("%06X",color));
            if(!red.isFocused())red.setValue(""+(color>>16&255));if(!green.isFocused())green.setValue(""+(color>>8&255));if(!blue.isFocused())blue.setValue(""+(color&255));
        }
        if(brightness!=null)brightness.normalized(value);if(sat!=null)sat.normalized(saturation);if(opacity!=null)opacity.normalized(alpha);syncing=false;
    }
    @Override protected void paint(GuiGraphics g,int x,int y,float delta){
        boolean valid=hex.getValue().replace("#","").matches("[0-9a-fA-F]{6}");
        for(EditBox box:new EditBox[]{red,green,blue})try{int channel=Integer.parseInt(box.getValue());valid&=channel>=0&&channel<=255;}catch(NumberFormatException e){valid=false;}
        apply.active=valid;
        g.blit(WHEEL,wx,wy,0,0,diameter,diameter,diameter,diameter);
        int dotX=wx+diameter/2+(int)(Math.cos(hue*2*Math.PI)*saturation*(diameter/2-2)),dotY=wy+diameter/2+(int)(Math.sin(hue*2*Math.PI)*saturation*(diameter/2-2));
        g.renderOutline(dotX-3,dotY-3,7,7,0xFF111118);g.renderOutline(dotX-2,dotY-2,5,5,0xFFFFFFFF);
        g.drawString(font,"HEX",left+24,top+240,0xFFAAA7B6,false);
        g.drawString(font,"R",left+170,top+240,0xFFAAA7B6,false);g.drawString(font,"G",left+236,top+240,0xFFAAA7B6,false);g.drawString(font,"B",left+302,top+240,0xFFAAA7B6,false);
        g.drawString(font,"Podgląd",left+386,top+240,0xFFAAA7B6,false);
        for(int row=0;row<20;row+=5)for(int col=0;col<110;col+=5)
            g.fill(left+386+col,top+254+row,left+391+col,top+259+row,(row/5+col/5)%2==0?0xFF62616B:0xFF32313C);
        g.fill(left+386,top+254,left+496,top+274,((int)Math.round(alpha*255)<<24)|rgb());
        g.renderOutline(left+386,top+254,110,20,0xFF696172);
        if(!valid)g.drawString(font,"Wpisz 6 cyfr HEX i wartości RGB od 0 do 255.",left+24,top+282,0xFFFF9292,false);
    }
    private boolean wheel(double x,double y){double dx=x-(wx+diameter/2.0),dy=y-(wy+diameter/2.0);return dx*dx+dy*dy<=diameter*diameter/4.0;}
    private void choose(double x,double y){double dx=x-(wx+diameter/2.0),dy=y-(wy+diameter/2.0);hue=Math.atan2(dy,dx)/(2*Math.PI);if(hue<0)hue+=1;saturation=Math.min(1,Math.hypot(dx,dy)/(diameter/2));sync(true);}
    @Override protected boolean click(double x,double y,int button){if(button==0&&wheel(x,y)){dragging=true;choose(x,y);return true;}return super.click(x,y,button);}
    @Override protected boolean drag(double x,double y,int button,double dx,double dy){if(dragging){choose(x,y);return true;}return super.drag(x,y,button,dx,dy);}
    @Override protected boolean release(double x,double y,int button){if(dragging){dragging=false;return true;}return super.release(x,y,button);}
    @Override public void removed(){if(registered){minecraft.getTextureManager().release(WHEEL);registered=false;}}
}
