package pl.crimeandlaw.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.Base64;

public final class LogoTexture {
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("crimeandlaw","gang_logo");
    private static String current="";private static boolean valid;
    public static ResourceLocation get(String logo){
        if(logo==null || logo.isEmpty()){clear();return null;}
        if(logo.equals(current))return valid?ID:null;
        clear();current=logo;
        try{NativeImage image=NativeImage.read(Base64.getDecoder().decode(logo));
            Minecraft.getInstance().getTextureManager().register(ID,new DynamicTexture(image));valid=true;return ID;}
        catch(Exception exception){return null;}
    }
    public static void clear(){if(valid)Minecraft.getInstance().getTextureManager().release(ID);valid=false;current="";}
}
