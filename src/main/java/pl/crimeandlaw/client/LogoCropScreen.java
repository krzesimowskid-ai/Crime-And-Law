package pl.crimeandlaw.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class LogoCropScreen extends MenuDialog {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("crimeandlaw","logo_crop_preview");
    private final BufferedImage image;
    private final LogoCrop crop;
    private final Consumer<String> accepted;
    private final String filename;
    private int px,py;
    private final int size=220;
    private final int previewHeight;
    private final int background;
    private final boolean banner;
    private boolean dragging,registered;
    private String error="";
    private ValueSlider zoom;
    public LogoCropScreen(GangScreen parent,BufferedImage image,String filename,Consumer<String> accepted){
        this(parent,image,filename,accepted,false);
    }
    public LogoCropScreen(GangScreen parent,BufferedImage image,String filename,Consumer<String> accepted,boolean banner){
        super(parent,banner?"Dopasuj baner":"Dopasuj logo");this.banner=banner;previewHeight=banner?(int)Math.round(size/parent.bannerAspect()):size;background=banner?parent.bannerColor():0;
        this.image=image;this.crop=new LogoCrop(image.getWidth(),image.getHeight(),banner?parent.bannerAspect():1);this.filename=filename;this.accepted=accepted;
    }
    @Override protected void build(){
        px=left+18;py=top+(banner?116:52);
        if(!registered)try {
            // The byte[] overload allocates the entire PNG on a small LWJGL
            // stack. Stream decoding uses a growable buffer for larger previews.
            var texture=new DynamicTexture(NativeImage.read(new java.io.ByteArrayInputStream(LogoImages.png(image))));
            texture.setFilter(true,false);minecraft.getTextureManager().register(TEXTURE,texture);registered=true;
        }catch(Exception e){error="Nie można wyświetlić podglądu obrazu.";}
        int rx=left+258,rw=panelWidth-276;
        addRenderableWidget(new FlatButton(rx,top+92,rw,21,banner?"Wypełnij baner":"Wypełnij kwadrat",b->{crop.fit(false);rebuildWidgets();},this::theme,!crop.fit()));
        addRenderableWidget(new FlatButton(rx,top+118,rw,21,"Pokaż cały obraz",b->{crop.fit(true);rebuildWidgets();},this::theme,crop.fit()));
        zoom=addRenderableWidget(new ValueSlider(rx,top+158,rw,"Przybliżenie",crop.normalizedZoom(),this::theme,crop::normalizedZoom).display(v->String.format(java.util.Locale.ROOT,"%.2f×",crop.zoom())));
        zoom.active=true;
        button(rx,top+199,rw,"Wyśrodkuj / resetuj",b->{crop.reset();rebuildWidgets();});
        button(left+18,top+panelHeight-35,100,"Anuluj",b->onClose());
        button(left+panelWidth-164,top+panelHeight-35,146,banner?"Zastosuj baner":"Zastosuj logo",b->{
            try {String encoded=LogoImages.encode(image,crop,banner);minecraft.setScreen(parent);accepted.accept(encoded);}
            catch(Exception e){error="Nie udało się zapisać obrazu: "+e.getMessage();}
        }).active=registered;
    }
    @Override protected void paint(GuiGraphics g,int x,int y,float delta){
        for(int by=0;by<previewHeight;by+=10)for(int bx=0;bx<size;bx+=10)g.fill(px+bx,py+by,px+Math.min(bx+10,size),py+Math.min(by+10,previewHeight),((bx/10+by/10)&1)==0?0xFF30323B:0xFF23252D);
        if(banner)g.fill(px,py,px+size,py+previewHeight,0xFF000000|background);
        if(registered){
            scissor(g,px,py,size,previewHeight);g.pose().pushPose();
            double scale=size/crop.side();
            double ix=px-crop.x()*scale;
            double iy=py-crop.y()*scale;
            g.pose().translate(ix,iy,0);g.pose().scale((float)scale,(float)scale,1);
            g.blit(TEXTURE,0,0,0,0,image.getWidth(),image.getHeight(),image.getWidth(),image.getHeight());
            g.pose().popPose();g.disableScissor();
        }
        if(!crop.fit())for(int n=1;n<3;n++){
            g.fill(px+n*size/3,py,px+n*size/3+1,py+previewHeight,0x55FFFFFF);g.fill(px,py+n*previewHeight/3,px+size,py+n*previewHeight/3+1,0x55FFFFFF);
        }
        g.renderOutline(px,py,size,previewHeight,theme().borderArgb());
        int rx=left+258;
        g.drawString(font,font.plainSubstrByWidth(filename,panelWidth-276),rx,top+54,0xFFEAE7ED,false);
        g.drawString(font,crop.fit()?"Cały obraz z marginesami":banner?"Kadr dopasowany do pasa":"Kwadratowy kadr",rx,top+72,0xFFAAA7B6,false);
        g.drawString(font,"Przeciągnij obraz, aby ustawić kadr.",px,py+previewHeight+9,0xFFAAA7B6,false);
        g.drawString(font,"Kółko myszy zmienia przybliżenie.",px,py+previewHeight+21,0xFFAAA7B6,false);
        if(!error.isEmpty())g.drawString(font,font.plainSubstrByWidth(error,panelWidth-36),left+18,top+panelHeight-49,0xFFFF9292,false);
    }
    private boolean over(double x,double y){return x>=px&&x<px+size&&y>=py&&y<py+previewHeight;}
    @Override protected boolean click(double x,double y,int button){if(button==0&&over(x,y)){dragging=true;return true;}return super.click(x,y,button);}
    @Override protected boolean drag(double x,double y,int button,double dx,double dy){
        if(dragging){crop.pan(-dx*crop.side()/size,-dy*crop.cropHeight()/previewHeight);return true;}return super.drag(x,y,button,dx,dy);
    }
    @Override protected boolean release(double x,double y,int button){if(dragging){dragging=false;return true;}return super.release(x,y,button);}
    @Override protected boolean scroll(double x,double y,double horizontal,double vertical){
        if(over(x,y)){crop.zoom(crop.zoom()*Math.pow(1.15,vertical));zoom.normalized(crop.normalizedZoom());return true;}return super.scroll(x,y,horizontal,vertical);
    }
    @Override public void removed(){if(registered){minecraft.getTextureManager().release(TEXTURE);registered=false;}}
}
