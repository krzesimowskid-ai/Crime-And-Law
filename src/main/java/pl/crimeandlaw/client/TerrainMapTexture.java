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
    private TerrainMapTexture(){}
    static void update(TerrainView view){
        if(view==null){clear();return;}
        List<String> next=new ArrayList<>();next.add(view.dimension()+"|"+view.centerX()+","+view.centerZ());
        for(var cell:view.cells())next.add(cell.x()+","+cell.z()+":"+cell.mapPixels());
        if(valid&&next.equals(signature))return;
        int side=TerrainView.SIDE*16;
        NativeImage image=new NativeImage(side,side,false);
        // Dark fog is used only for missing pixels, never an invented landscape.
        for(int z=0;z<side;z++)for(int x=0;x<side;x++)image.setPixelRGBA(x,z,((x/8+z/8)&1)==0?0xFF292626:0xFF2D2A2A);
        for(var cell:view.cells()){
            if(cell.mapPixels().isEmpty())continue;
            int dx=cell.x()-view.centerX()+TerrainView.RADIUS,dz=cell.z()-view.centerZ()+TerrainView.RADIUS;
            if(dx<0||dz<0||dx>=TerrainView.SIDE||dz>=TerrainView.SIDE)continue;
            byte[] colors=new TerrainMapTile(cell.mapPixels()).colors();
            for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                int color=MapColor.getColorFromPackedId(colors[z*16+x]);
                if((color>>>24)!=0)image.setPixelRGBA(dx*16+x,dz*16+z,color);
            }
        }
        clear();var texture=new DynamicTexture(image);texture.setFilter(false,false);
        Minecraft.getInstance().getTextureManager().register(ID,texture);signature=List.copyOf(next);valid=true;
    }
    static void draw(GuiGraphics g,int x,int y,int size,int radius){
        if(!valid)return;int offset=(TerrainView.RADIUS-radius)*16,source=(radius*2+1)*16,side=TerrainView.SIDE*16;
        g.blit(ID,x,y,size,size,(float)offset,(float)offset,source,source,side,side);
    }
    static void clear(){if(valid)Minecraft.getInstance().getTextureManager().release(ID);valid=false;signature=List.of();}
}
