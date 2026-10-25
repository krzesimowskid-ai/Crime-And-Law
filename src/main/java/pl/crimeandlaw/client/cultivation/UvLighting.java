package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import pl.crimeandlaw.cultivation.*;

/** Depth-reconstructed world lighting, including walls/ceiling/entities. HUD is drawn later. */
@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class UvLighting {
    private static final ResourceLocation SHADER=ResourceLocation.fromNamespaceAndPath("crimeandlaw","uv_world"),ATLAS=ResourceLocation.fromNamespaceAndPath("crimeandlaw","dynamic/uv_volume");
    private record Field(long stamp,float[] values){}
    private static final Map<BlockPos,Field> CACHE=new HashMap<>();
    private static List<Planter> lamps=List.of();private static ClientLevel world;private static long lastScan=Long.MIN_VALUE;private static ShaderInstance shader;private static TextureTarget scene;private static DynamicTexture atlas;private static boolean dirty=true;
    private static int renderedFrames,lastLightCount;private static float handLight;
    private UvLighting(){}
    public static int multiply(int a,int b){return (a>>>24)*(b>>>24)/255<<24|((a>>>16&255)*(b>>>16&255)/255)<<16|((a>>>8&255)*(b>>>8&255)/255)<<8|(a&255)*(b&255)/255;}
    public static int activeLights(){return lastLightCount;}public static int renderedFrames(){return renderedFrames;}
    @EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent e)throws java.io.IOException{e.registerShader(new ShaderInstance(e.getResourceProvider(),SHADER,DefaultVertexFormat.POSITION),s->{release();shader=s;});}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){release();}
    private static void release(){if(scene!=null){scene.destroyBuffers();scene=null;}if(atlas!=null){Minecraft.getInstance().getTextureManager().release(ATLAS);atlas=null;}CACHE.clear();lamps=List.of();world=null;lastScan=Long.MIN_VALUE;dirty=true;lastLightCount=0;}
    private static void collect(Minecraft mc){
        if(world!=mc.level){release();world=mc.level;}long now=world.getGameTime();if(now-lastScan>=0&&now-lastScan<5)return;lastScan=now;
        var camera=mc.gameRenderer.getMainCamera().getPosition();int cx=net.minecraft.util.Mth.floor(camera.x)>>4,cz=net.minecraft.util.Mth.floor(camera.z)>>4;var found=new ArrayList<Planter>();
        for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++){var chunk=world.getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);if(chunk instanceof LevelChunk c)for(var be:c.getBlockEntities().values())if(be instanceof Planter p&&p.hasLamp()&&p.getBlockPos().distToCenterSqr(camera)<180)found.add(p);}
        found.sort(Comparator.comparingDouble(p->p.getBlockPos().distToCenterSqr(camera)));if(found.size()>UvVolume.MAX_LIGHTS)found.subList(UvVolume.MAX_LIGHTS,found.size()).clear();
        if(!found.equals(lamps)){lamps=List.copyOf(found);dirty=true;}var positions=new HashSet<BlockPos>();for(var p:lamps)positions.add(p.getBlockPos());CACHE.keySet().retainAll(positions);
    }
    private static BlockPos origin(Planter p){return p.getBlockPos().offset(-6,-5,-6);}
    private static void volumes(){
        int size=UvVolume.SIZE;for(var p:lamps){var pos=p.getBlockPos();long stamp=(world.getGameTime()+Math.floorMod(pos.asLong(),20))/20;var old=CACHE.get(pos);if(old!=null&&old.stamp==stamp)continue;
            var base=origin(p);var solid=new boolean[size*size*size];var at=new BlockPos.MutableBlockPos();
            for(int y=0;y<size;y++)for(int z=0;z<size;z++)for(int x=0;x<size;x++){at.set(base.getX()+x,base.getY()+y,base.getZ()+z);solid[UvVolume.index(x,y,z)]=!world.hasChunkAt(at)||world.getBlockState(at).isCollisionShapeFullBlock(world,at);}
            CACHE.put(pos,new Field(stamp,UvVolume.bake(solid)));dirty=true;
        }
        if(atlas==null){atlas=new DynamicTexture(size*size,size*UvVolume.MAX_LIGHTS,false);Minecraft.getInstance().getTextureManager().register(ATLAS,atlas);atlas.setFilter(true,false);dirty=true;}
        if(!dirty)return;var image=Objects.requireNonNull(atlas.getPixels());image.fillRect(0,0,image.getWidth(),image.getHeight(),0xFF000000);
        for(int i=0;i<lamps.size();i++){var values=CACHE.get(lamps.get(i).getBlockPos()).values;for(int y=0;y<size;y++)for(int z=0;z<size;z++)for(int x=0;x<size;x++){int v=Math.clamp(Math.round(values[UvVolume.index(x,y,z)]*255),0,255);image.setPixelRGBA(y*size+x,i*size+z,0xFF000000|v*0x010101);}}
        atlas.upload();dirty=false;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||shader==null)return;var mc=Minecraft.getInstance();if(mc.level==null)return;collect(mc);
        var live=lamps.stream().filter(p->!p.isRemoved()&&p.hasLamp()).toList();if(!live.equals(lamps)){lamps=live;dirty=true;}
        lastLightCount=lamps.size();handLight=0;if(lamps.isEmpty())return;volumes();var eye=e.getCamera().getPosition();for(var lamp:lamps){var o=origin(lamp);handLight+=UvVolume.sample(CACHE.get(lamp.getBlockPos()).values,eye.x-o.getX(),eye.y-o.getY(),eye.z-o.getZ());}handLight=Math.clamp(handLight*1.8f,0,1);effect(mc,e,false);
    }
    /** Called immediately after first-person rendering, before Minecraft clears its depth for HUD. */
    public static void afterHands(){var mc=Minecraft.getInstance();if(shader==null||world==null||mc.level!=world||lastLightCount==0||handLight<=0||!mc.options.getCameraType().isFirstPerson()||mc.options.hideGui)return;effect(mc,null,true);}
    private static void effect(Minecraft mc,RenderLevelStageEvent e,boolean hand){
        var main=mc.getMainRenderTarget();if(!main.useDepth)return;
        if(scene==null)scene=new TextureTarget(main.width,main.height,true,Minecraft.ON_OSX);else if(scene.width!=main.width||scene.height!=main.height)scene.resize(main.width,main.height,Minecraft.ON_OSX);if(main.isStencilEnabled()&&!scene.isStencilEnabled())scene.enableStencil();
        GlStateManager._glBindFramebuffer(36008,main.frameBufferId);GlStateManager._glBindFramebuffer(36009,scene.frameBufferId);GlStateManager._glBlitFrameBuffer(0,0,main.width,main.height,0,0,scene.width,scene.height,16384|256,9728);main.bindWrite(true);
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);var previous=RenderSystem.getShader();
        RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();RenderSystem.disableCull();
        try{
            shader.safeGetUniform("HandPass").set(hand?1:0);shader.safeGetUniform("HandLight").set(handLight);
            shader.setSampler("Scene",scene.getColorTextureId());shader.setSampler("SceneDepth",scene.getDepthTextureId());shader.setSampler("Volume",atlas.getId());
            if(!hand){shader.safeGetUniform("InverseProjection").set(new Matrix4f(e.getProjectionMatrix()).invert());shader.safeGetUniform("InverseView").set(new Matrix4f(e.getModelViewMatrix()).invert());shader.safeGetUniform("LightCount").set(lamps.size());
                var camera=e.getCamera().getPosition();for(int i=0;i<UvVolume.MAX_LIGHTS;i++){var o=i<lamps.size()?origin(lamps.get(i)):BlockPos.ZERO;shader.safeGetUniform("Light"+i).set((float)(o.getX()-camera.x),(float)(o.getY()-camera.y),(float)(o.getZ()-camera.z),(float)i);}}
            RenderSystem.setShader(()->shader);var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);b.addVertex(-1,-1,0);b.addVertex(1,-1,0);b.addVertex(1,1,0);b.addVertex(-1,1,0);BufferUploader.drawWithShader(b.buildOrThrow());renderedFrames++;
        }finally{RenderSystem.setShader(()->previous);RenderSystem.depthMask(true);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();}
    }
}
