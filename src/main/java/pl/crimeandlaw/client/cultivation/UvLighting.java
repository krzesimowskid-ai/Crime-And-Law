package pl.crimeandlaw.client.cultivation;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import pl.crimeandlaw.cultivation.*;

/** Colored irradiance is supplied directly to lightmapped materials, before texture and fog. */
@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class UvLighting {
    private static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath("crimeandlaw","dynamic/uv_volume");
    private record Field(long stamp,boolean[] solid,float[] values){}
    private static final Map<BlockPos,Field> CACHE=new HashMap<>();
    private static final Matrix4f inverseView=new Matrix4f();
    private static final float[][] lights=new float[UvVolume.MAX_LIGHTS][4];
    private static List<Planter> lamps=List.of();private static ClientLevel world;private static long lastScan=Long.MIN_VALUE;private static DynamicTexture atlas;private static boolean dirty=true,worldPass,handPass;
    private static int renderedFrames,lastLightCount;private static float handLight;
    private UvLighting(){}
    public static int multiply(int a,int b){return (a>>>24)*(b>>>24)/255<<24|((a>>>16&255)*(b>>>16&255)/255)<<16|((a>>>8&255)*(b>>>8&255)/255)<<8|(a&255)*(b&255)/255;}
    public static int activeLights(){return lastLightCount;}public static int renderedFrames(){return renderedFrames;}
    @EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent e){release();}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){release();}
    private static void release(){if(atlas!=null){Minecraft.getInstance().getTextureManager().release(ATLAS);atlas=null;}CACHE.clear();lamps=List.of();world=null;lastScan=Long.MIN_VALUE;dirty=true;lastLightCount=0;worldPass=false;handPass=false;}
    private static void collect(Minecraft mc){
        if(world!=mc.level){release();world=mc.level;}long now=world.getGameTime();if(now-lastScan>=0&&now-lastScan<5)return;lastScan=now;
        var camera=mc.gameRenderer.getMainCamera().getPosition();int cx=net.minecraft.util.Mth.floor(camera.x)>>4,cz=net.minecraft.util.Mth.floor(camera.z)>>4;var found=new ArrayList<Planter>();
        for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++){var chunk=world.getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);if(chunk instanceof LevelChunk c)for(var be:c.getBlockEntities().values())if(be instanceof Planter p&&p.hasLamp()&&p.getBlockPos().distToCenterSqr(camera)<420)found.add(p);}
        found.sort(Comparator.comparingDouble(p->p.getBlockPos().distToCenterSqr(camera)));if(found.size()>UvVolume.MAX_LIGHTS)found.subList(UvVolume.MAX_LIGHTS,found.size()).clear();found.sort(Comparator.comparingLong(p->p.getBlockPos().asLong()));
        if(!found.equals(lamps)){lamps=List.copyOf(found);dirty=true;}var positions=new HashSet<BlockPos>();for(var p:lamps)positions.add(p.getBlockPos());CACHE.keySet().retainAll(positions);
    }
    private static BlockPos origin(Planter p){return p.getBlockPos().offset(-UvVolume.CENTER,1-UvVolume.CENTER,-UvVolume.CENTER);}
    private static void volumes(){
        int size=UvVolume.SIZE;for(var p:lamps){var pos=p.getBlockPos();long stamp=(world.getGameTime()+Math.floorMod(pos.asLong(),5))/5;var old=CACHE.get(pos);if(old!=null&&old.stamp==stamp)continue;
            var base=origin(p);var solid=new boolean[size*size*size];var at=new BlockPos.MutableBlockPos();
            for(int y=0;y<size;y++)for(int z=0;z<size;z++)for(int x=0;x<size;x++){at.set(base.getX()+x,base.getY()+y,base.getZ()+z);solid[UvVolume.index(x,y,z)]=!world.hasChunkAt(at)||world.getBlockState(at).getLightBlock(world,at)>=15;}
            if(old!=null&&Arrays.equals(old.solid,solid)){CACHE.put(pos,new Field(stamp,old.solid,old.values));continue;}
            CACHE.put(pos,new Field(stamp,solid,UvVolume.bake(solid)));dirty=true;
        }
        if(atlas==null){atlas=new DynamicTexture(size*size,size*UvVolume.MAX_LIGHTS,false);Minecraft.getInstance().getTextureManager().register(ATLAS,atlas);atlas.setFilter(true,false);dirty=true;}
        if(!dirty)return;var image=Objects.requireNonNull(atlas.getPixels());image.fillRect(0,0,image.getWidth(),image.getHeight(),0xFF000000);
        for(int i=0;i<lamps.size();i++){var values=CACHE.get(lamps.get(i).getBlockPos()).values;for(int y=0;y<size;y++)for(int z=0;z<size;z++)for(int x=0;x<size;x++){int v=Math.clamp(Math.round(values[UvVolume.index(x,y,z)]*255),0,255);image.setPixelRGBA(y*size+x,i*size+z,0xFF000000|v*0x010101);}}
        // NativeImage.upload resets GL filtering to NEAREST. Restore linear filtering afterwards.
        atlas.upload();atlas.setFilter(true,false);dirty=false;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY)return;worldPass=false;handPass=false;var mc=Minecraft.getInstance();if(mc.level==null)return;collect(mc);
        var live=lamps.stream().filter(p->!p.isRemoved()&&p.hasLamp()).toList();if(!live.equals(lamps)){lamps=live;dirty=true;}
        lastLightCount=lamps.size();handLight=0;if(lamps.isEmpty())return;volumes();inverseView.set(e.getModelViewMatrix()).invert();var eye=e.getCamera().getPosition();
        var direction=e.getCamera().getLookVector();var hand=eye.add(direction.x()*.45,direction.y()*.45-.4,direction.z()*.45);
        for(int i=0;i<lamps.size();i++){var lamp=lamps.get(i);var o=origin(lamp);handLight=Math.max(handLight,UvVolume.sample(CACHE.get(lamp.getBlockPos()).values,hand.x-o.getX(),hand.y-o.getY(),hand.z-o.getZ()));lights[i][0]=(float)(o.getX()-eye.x);lights[i][1]=(float)(o.getY()-eye.y);lights[i][2]=(float)(o.getZ()-eye.z);lights[i][3]=i;}
        worldPass=true;renderedFrames++;
    }
    public static void beginHands(){handPass=worldPass;}
    public static void endHands(){handPass=false;}
    public static void finishWorld(){worldPass=false;handPass=false;}
    public static void bind(ShaderInstance shader){
        if(shader.getUniform("CalUvCount")==null)return;
        int count=worldPass&&atlas!=null&&world==Minecraft.getInstance().level?lastLightCount:0;
        shader.safeGetUniform("CalUvCount").set(count);if(count==0)return;
        shader.setSampler("CalUvVolume",atlas.getId());shader.safeGetUniform("CalUvInverseView").set(inverseView);shader.safeGetUniform("CalUvHand").set(handPass?1:0);shader.safeGetUniform("CalUvHandLight").set(handLight);shader.safeGetUniform("CalUvGamma").set(Minecraft.getInstance().options.gamma().get().floatValue());
        for(int i=0;i<count;i++){var a=lights[i];shader.safeGetUniform("CalUvLight"+i).set(a[0],a[1],a[2],a[3]);}
    }
}
