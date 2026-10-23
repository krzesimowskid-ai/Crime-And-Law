package pl.crimeandlaw.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;
import pl.crimeandlaw.territory.*;

/** A single reusable raster: not hundreds of GUI rectangles or textures per frame. */
final class TerrainMapTexture {
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("crimeandlaw","terrain_map");
    private static List<String> signature=List.of();
    private static boolean valid;
    private static int imageSide,bufferRadius;
    private static final LinkedHashMap<String,int[]> decoded=new LinkedHashMap<>(512,.75f,true);
    private TerrainMapTexture(){}
    static void update(TerrainView view){
        if(view==null){clear();return;}
        List<String> next=new ArrayList<>();next.add(view.dimension()+"|"+view.centerX()+","+view.centerZ()+"|"+view.radius());
        for(var cell:view.cells())next.add(cell.x()+","+cell.z()+":"+cell.mapPixels());
        if(valid&&next.equals(signature))return;
        int cells=view.radius()*2+1,side=cells*16;
        NativeImage image=new NativeImage(side,side,false);
        // Dark fog is used only for missing pixels, never an invented landscape.
        for(int z=0;z<side;z++)for(int x=0;x<side;x++)image.setPixelRGBA(x,z,((x/8+z/8)&1)==0?0xFF292626:0xFF2D2A2A);
        for(var cell:view.cells()){
            if(cell.mapPixels().isEmpty())continue;
            int dx=cell.x()-view.centerX()+view.radius(),dz=cell.z()-view.centerZ()+view.radius();
            if(dx<0||dz<0||dx>=cells||dz>=cells)continue;
            int[] colors=colors(cell.mapPixels());
            for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                int color=colors[z*16+x];
                if((color>>>24)!=0)image.setPixelRGBA(dx*16+x,dz*16+z,color);
            }
        }
        releaseTexture();var texture=new DynamicTexture(image);texture.setFilter(false,false);
        Minecraft.getInstance().getTextureManager().register(ID,texture);signature=List.copyOf(next);imageSide=side;bufferRadius=view.radius();valid=true;
    }
    private static int[] colors(String pixels){
        var result=decoded.get(pixels);if(result!=null)return result;
        byte[] packed=new TerrainMapTile(pixels).colors();result=new int[256];
        for(int i=0;i<256;i++)result[i]=MapColor.getColorFromPackedId(packed[i]);
        decoded.put(pixels,result);while(decoded.size()>1024)decoded.pollFirstEntry();return result;
    }
    static void draw(GuiGraphics g,int x,int y,int size,int radius){
        if(!valid)return;
        double scale=size/((radius*2+1)*16.0),margin=(bufferRadius-radius)*16*scale;
        // Draw the prefetched border as well as the visible square. Translating a
        // held gesture must not expose the edge of an old, viewport-sized quad.
        g.pose().pushPose();g.pose().translate(x-margin,y-margin,0);g.pose().scale((float)scale,(float)scale,1);
        g.blit(ID,0,0,0f,0f,imageSide,imageSide,imageSide,imageSide);g.pose().popPose();
    }
    private static void releaseTexture(){if(valid)Minecraft.getInstance().getTextureManager().release(ID);valid=false;signature=List.of();imageSide=bufferRadius=0;}
    static void clear(){releaseTexture();decoded.clear();}
}
