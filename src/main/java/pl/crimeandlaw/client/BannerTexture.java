package pl.crimeandlaw.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import pl.crimeandlaw.gang.GangBanner;

public final class BannerTexture {
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("crimeandlaw","gang_banner");
    private static String current="";private static boolean valid;
    private static int width,height;
    public static ResourceLocation get(String logo){
        if(logo==null || logo.isEmpty()){clear();return null;}
        if(logo.equals(current)){current=logo;return valid?ID:null;}
        clear();current=logo;
        if(logo.length()>pl.crimeandlaw.gang.GangBanner.MAX_ENCODED_LENGTH)return null;
        // Stream decoding avoids allocating a detailed PNG on LWJGL's small stack.
        try(var input=new ByteArrayInputStream(Base64.getDecoder().decode(logo))){
            NativeImage image=NativeImage.read(input);
            if(image.getWidth()>GangBanner.READ_WIDTH||image.getHeight()>GangBanner.READ_HEIGHT){image.close();return null;}
            double scale=Math.min(1,Math.min(GangBanner.WIDTH/(double)image.getWidth(),GangBanner.HEIGHT/(double)image.getHeight()));
            if(scale<1){
                var reduced=new NativeImage(Math.max(1,(int)Math.round(image.getWidth()*scale)),Math.max(1,(int)Math.round(image.getHeight()*scale)),false);
                try{image.resizeSubRectTo(0,0,image.getWidth(),image.getHeight(),reduced);}
                catch(Throwable error){reduced.close();throw error;}
                finally{image.close();}
                image=reduced;
            }
            width=image.getWidth();height=image.getHeight();
            var texture=SmoothImageTexture.create(image);
            Minecraft.getInstance().getTextureManager().register(ID,texture);valid=true;return ID;}
        catch(Exception exception){return null;}
    }
    public static void draw(GuiGraphics graphics,int x,int y,int drawWidth,int drawHeight,int opacity){
        if(!valid||opacity<=0)return;
        float ratio=drawWidth/(float)drawHeight;
        int sw=Math.min(width,Math.max(1,Math.round(height*ratio))),sh=Math.min(height,Math.max(1,Math.round(width/ratio)));
        graphics.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.setColor(1,1,1,opacity/255f);
        graphics.blit(ID,x,y,drawWidth,drawHeight,(width-sw)/2f,(height-sh)/2f,sw,sh,width,height);
        graphics.setColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    public static void clear(){if(valid)Minecraft.getInstance().getTextureManager().release(ID);valid=false;current="";width=height=0;}
}
