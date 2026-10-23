package pl.crimeandlaw.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import pl.crimeandlaw.gang.GangLogo;

public final class LogoTexture {
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("crimeandlaw","gang_logo");
    private static String current="";private static boolean valid;
    private static int width,height;
    public static ResourceLocation get(String logo){
        if(logo==null || logo.isEmpty()){clear();return null;}
        if(logo.equals(current)){current=logo;return valid?ID:null;}
        clear();current=logo;
        if(logo.length()>GangLogo.MAX_ENCODED_LENGTH)return null;
        // Stream decoding avoids allocating a detailed PNG on LWJGL's small stack.
        try(var input=new ByteArrayInputStream(Base64.getDecoder().decode(logo))){
            NativeImage image=NativeImage.read(input);
            if(image.getWidth()>GangLogo.SIDE||image.getHeight()>GangLogo.SIDE){image.close();return null;}
            width=image.getWidth();height=image.getHeight();
            var texture=SmoothImageTexture.create(image);
            Minecraft.getInstance().getTextureManager().register(ID,texture);valid=true;return ID;}
        catch(Exception exception){return null;}
    }
    public static void draw(GuiGraphics graphics,int x,int y,int drawWidth,int drawHeight){
        if(valid){
            graphics.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            graphics.blit(ID,x,y,drawWidth,drawHeight,0f,0f,width,height,width,height);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }
    public static void clear(){if(valid)Minecraft.getInstance().getTextureManager().release(ID);valid=false;current="";width=height=0;}
}
