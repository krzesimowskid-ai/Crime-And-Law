package pl.crimeandlaw.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/** Loaded client chunks only. Bounded sampling and one texture shared by all frames. */
final class ClientMinimap {
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("crimeandlaw","minimap");
    private static final int RADIUS=5,SIDE=(RADIUS*2+1)*16,MAX_TILES=256;
    private record Offset(int x,int z){}
    private record Tile(int[] pixels,long updated){}
    private static final List<Offset> OFFSETS=new ArrayList<>();
    static {for(int z=-RADIUS;z<=RADIUS;z++)for(int x=-RADIUS;x<=RADIUS;x++)OFFSETS.add(new Offset(x,z));OFFSETS.sort(Comparator.comparingInt(o->o.x*o.x+o.z*o.z));}
    private static final LinkedHashMap<Long,Tile> tiles=new LinkedHashMap<>(256,.75f,true);
    private static final BlockPos.MutableBlockPos POS=new BlockPos.MutableBlockPos();
    private static ClientLevel level;
    private static DynamicTexture texture;
    private static Job job;
    private static long ticks,lastUpload;
    private static int centerX,centerZ,imageCenterX,imageCenterZ;
    private static boolean dirty;
    private static final class Job {final int x,z;final int[] pixels=new int[256];int cursor;Job(int x,int z){this.x=x;this.z=z;}}
    private ClientMinimap(){}
    private static long key(int x,int z){return (long)x<<32|z&0xFFFFFFFFL;}
    static void tick(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(level!=mc.level){clear();level=mc.level;}
        ticks++;int px=mc.player.chunkPosition().x,pz=mc.player.chunkPosition().z;
        if(texture==null||centerX!=px||centerZ!=pz){centerX=px;centerZ=pz;dirty=true;}
        if(job!=null&&(Math.abs(job.x-px)>RADIUS+1||Math.abs(job.z-pz)>RADIUS+1))job=null;
        long deadline=System.nanoTime()+2_000_000L;int budget=512;
        while(budget>0&&System.nanoTime()<deadline){
            if(job==null){
                for(var offset:OFFSETS){int x=px+offset.x,z=pz+offset.z;if(!level.getChunkSource().hasChunk(x,z))continue;Tile tile=tiles.get(key(x,z));if(tile==null||ticks-tile.updated()>120){job=new Job(x,z);break;}}
                if(job==null)break;
            }
            var chunk=level.getChunkSource().getChunk(job.x,job.z,ChunkStatus.FULL,false);
            if(chunk==null){job=null;break;}
            while(job.cursor<256&&budget-->0&&System.nanoTime()<deadline){
                int x=job.cursor&15,z=job.cursor>>4;job.pixels[job.cursor++]=sample(chunk,job.x*16+x,job.z*16+z,x,z);
            }
            if(job.cursor==256){tiles.put(key(job.x,job.z),new Tile(job.pixels,ticks));while(tiles.size()>MAX_TILES)tiles.pollFirstEntry();job=null;dirty=true;}
        }
        if(dirty&&(texture==null||ticks-lastUpload>=5)){upload();lastUpload=ticks;dirty=false;}
    }
    private static int sample(LevelChunk chunk,int wx,int wz,int x,int z){
        int y=Math.clamp(chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)+1,level.getMinBuildHeight(),level.getMaxBuildHeight()-1);
        MapColor color=MapColor.NONE;
        for(int depth=0;depth<48&&y>=level.getMinBuildHeight();depth++,y--){POS.set(wx,y,wz);color=chunk.getBlockState(POS).getMapColor(level,POS);if(color!=MapColor.NONE)break;}
        if(color==MapColor.NONE)return MinimapStyle.FOG;
        int west=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,Math.max(0,x-1),z),east=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,Math.min(15,x+1),z);
        var shade=west>east?MapColor.Brightness.LOW:west<east?MapColor.Brightness.HIGH:MapColor.Brightness.NORMAL;
        return MinimapStyle.terrain(MapColor.getColorFromPackedId(color.getPackedId(shade)),color==MapColor.WATER);
    }
    private static void upload(){
        if(texture==null){texture=new DynamicTexture(new NativeImage(SIDE,SIDE,false));Minecraft.getInstance().getTextureManager().register(ID,texture);}
        var image=texture.getPixels();if(image==null)return;
        for(int z=0;z<SIDE;z++)for(int x=0;x<SIDE;x++)image.setPixelRGBA(x,z,MinimapStyle.FOG);
        for(int z=-RADIUS;z<=RADIUS;z++)for(int x=-RADIUS;x<=RADIUS;x++){
            var tile=tiles.get(key(centerX+x,centerZ+z));if(tile==null)continue;
            for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++)image.setPixelRGBA((x+RADIUS)*16+dx,(z+RADIUS)*16+dz,tile.pixels()[dz*16+dx]);
        }
        texture.upload();texture.setFilter(true,false);imageCenterX=centerX;imageCenterZ=centerZ;
    }
    static void draw(GuiGraphics g,int x,int y,int width,int height,float scale,float partial){
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return;
        g.renderOutline(x-2,y-2,width+4,height+4,0x70080C10);g.renderOutline(x-1,y-1,width+2,height+2,0x60838D94);
        g.pose().pushPose();g.pose().last().pose().identity();g.enableScissor((int)(x*scale),(int)(y*scale),(int)Math.ceil((x+width)*scale),(int)Math.ceil((y+height)*scale));g.pose().popPose();
        g.fill(x,y,x+width,y+height,0x180C1218);
        double px=player.xo+(player.getX()-player.xo)*partial,pz=player.zo+(player.getZ()-player.zo)*partial;
        float yaw=net.minecraft.util.Mth.rotLerp(partial,player.yRotO,player.getYRot());float angle=180-yaw;
        float mx=x+width/2f,my=y+height*.58f,blocks=width/112f;
        if(texture!=null){
            g.pose().pushPose();g.pose().translate(mx,my,0);g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));g.pose().scale(blocks,blocks,1);
            g.pose().translate(-px+(imageCenterX-RADIUS)*16,-pz+(imageCenterZ-RADIUS)*16,0);
            g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            try{g.blit(ID,0,0,0f,0f,SIDE,SIDE,SIDE,SIDE);}finally{RenderSystem.disableBlend();g.pose().popPose();}
        }
        g.disableScissor();
        var north=MinimapStyle.north(x,y,width,height,mx,my,angle);
        HudShapes.ellipse(g,north.x(),north.y(),6.5f,6.5f,0xC0182027);g.flush();
        var label=Component.literal("N").setStyle(PlayerHud.HUD_FONT);
        g.drawString(mc.font,label.getVisualOrderText(),north.x()-mc.font.width(label)/2f,north.y()-4,0xFFF2F5F7,false);
        g.pose().pushPose();g.pose().translate(mx,my,0);
        HudShapes.triangle(g,0,-7,-6,6,0,3,0xF0091015);HudShapes.triangle(g,0,-7,0,3,6,6,0xF0091015);
        HudShapes.triangle(g,0,-5,-3.8f,3.7f,0,1.6f,0xFFFFFFFF);HudShapes.triangle(g,0,-5,0,1.6f,3.8f,3.7f,0xFFD7E2E8);
        g.pose().popPose();g.flush();
    }
    static int cacheSize(){return tiles.size();}
    static void clear(){if(texture!=null){Minecraft.getInstance().getTextureManager().release(ID);texture=null;}level=null;job=null;tiles.clear();ticks=lastUpload=0;dirty=false;}
}
