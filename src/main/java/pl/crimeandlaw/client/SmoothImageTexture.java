package pl.crimeandlaw.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/** Build the minification chain once, rather than sampling four source pixels per GUI pixel. */
public final class SmoothImageTexture {
    private SmoothImageTexture(){}
    public static DynamicTexture create(NativeImage image){
        DynamicTexture texture=new DynamicTexture(image);
        try{
            texture.bind();
            int levels=31-Integer.numberOfLeadingZeros(Math.max(image.getWidth(),image.getHeight()));
            // DynamicTexture normally allocates only level zero. Make every generated
            // level available, including for non-power-of-two banner dimensions.
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D,GL12.GL_TEXTURE_MAX_LEVEL,levels);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D,GL12.GL_TEXTURE_MIN_LOD,0f);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D,GL12.GL_TEXTURE_MAX_LOD,(float)levels);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            texture.setFilter(true,true);
            return texture;
        }catch(RuntimeException|Error error){texture.close();throw error;}
    }
}
