package pl.crimeandlaw.client.cultivation;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Matrix4f;
import pl.crimeandlaw.cultivation.*;

/** World lightmap for all loaded lamps, independent of player/freecam distance. */
@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class UvLighting {
    private static final ResourceLocation ATLAS=id("uv_volume"),INDEX=id("uv_index");
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("crimeandlaw","dynamic/"+name);}
    private static final Map<Long,LevelChunk> LOADED=new HashMap<>();
    private static final Map<BlockPos,float[]> CACHE=new HashMap<>();
    private static final Matrix4f inverseView=new Matrix4f();
    private static ClientLevel tracked,world;private static List<Planter> lamps=List.of();private static long lastScan=Long.MIN_VALUE;
    private static DynamicTexture atlas,index;private static UvSectionField field;private static boolean dirty=true,worldPass,handPass;
    private static int renderedFrames,lastLightCount;private static float handLight;
    private UvLighting(){}
    public static int multiply(int a,int b){return (a>>>24)*(b>>>24)/255<<24|((a>>>16&255)*(b>>>16&255)/255)<<16|((a>>>8&255)*(b>>>8&255)/255)<<8|(a&255)*(b&255)/255;}
    public static int activeLights(){return lastLightCount;}public static int renderedFrames(){return renderedFrames;}
    @EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {@SubscribeEvent public static void shaders(RegisterShadersEvent e){release();}}
    @SubscribeEvent public static void load(ChunkEvent.Load e){if(e.getLevel() instanceof ClientLevel level&&e.getChunk() instanceof LevelChunk chunk){if(tracked!=level){LOADED.clear();tracked=level;release();}LOADED.put(chunk.getPos().toLong(),chunk);invalidateAll();}}
    @SubscribeEvent public static void unload(ChunkEvent.Unload e){if(e.getLevel()==tracked){LOADED.remove(e.getChunk().getPos().toLong());invalidateAll();}}
    @SubscribeEvent public static void leave(LevelEvent.Unload e){if(e.getLevel()==tracked){LOADED.clear();tracked=null;release();}}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){LOADED.clear();tracked=null;release();}
    private static void invalidateAll(){CACHE.clear();dirty=true;lastScan=Long.MIN_VALUE;}
    public static void changed(ClientLevel level,BlockPos pos){if(level!=tracked)return;boolean removed=CACHE.keySet().removeIf(p->Math.abs(p.getX()-pos.getX())<=10&&Math.abs(p.getZ()-pos.getZ())<=10&&pos.getY()>=p.getY()-9&&pos.getY()<=p.getY()+11);if(removed)dirty=true;}
    private static void release(){var manager=Minecraft.getInstance().getTextureManager();if(atlas!=null){manager.release(ATLAS);atlas=null;}if(index!=null){manager.release(INDEX);index=null;}CACHE.clear();field=null;lamps=List.of();world=null;lastScan=Long.MIN_VALUE;dirty=true;lastLightCount=0;worldPass=handPass=false;}
    private static void collect(Minecraft mc){
        if(world!=mc.level){release();world=mc.level;}long now=world.getGameTime();if(now-lastScan>=0&&now-lastScan<5)return;lastScan=now;
        if(LOADED.entrySet().removeIf(e->{var c=e.getValue();return world.getChunkSource().getChunk(c.getPos().x,c.getPos().z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)!=c;}))invalidateAll();
        var found=new ArrayList<Planter>();for(var chunk:LOADED.values())for(var be:chunk.getBlockEntities().values())if(be instanceof Planter p&&!p.isRemoved()&&p.hasLamp())found.add(p);
        found.sort(Comparator.comparingLong(p->p.getBlockPos().asLong()));if(!found.equals(lamps)){lamps=List.copyOf(found);dirty=true;}
        var positions=new HashSet<BlockPos>();for(var p:lamps)positions.add(p.getBlockPos());CACHE.keySet().retainAll(positions);
    }
    private static BlockPos origin(Planter p){return p.getBlockPos().offset(-UvVolume.CENTER,1-UvVolume.CENTER,-UvVolume.CENTER);}
    private static DynamicTexture texture(DynamicTexture old,ResourceLocation name,int width,int height,int[] pixels){
        var manager=Minecraft.getInstance().getTextureManager();if(old==null||old.getPixels().getWidth()!=width||old.getPixels().getHeight()!=height){if(old!=null)manager.release(name);old=new DynamicTexture(width,height,false);manager.register(name,old);}
        var image=Objects.requireNonNull(old.getPixels());for(int y=0;y<height;y++)for(int x=0;x<width;x++)image.setPixelRGBA(x,y,pixels[y*width+x]);
        old.upload();old.setFilter(true,false);return old;
    }
    private static void volumes(){
        for(var lamp:lamps)if(!CACHE.containsKey(lamp.getBlockPos())){
            int size=UvVolume.SIZE;var base=origin(lamp);var solid=new boolean[size*size*size];var at=new BlockPos.MutableBlockPos();
            for(int y=0;y<size;y++)for(int z=0;z<size;z++)for(int x=0;x<size;x++){
                at.set(base.getX()+x,base.getY()+y,base.getZ()+z);var chunk=LOADED.get(ChunkPos.asLong(at.getX()>>4,at.getZ()>>4));
                solid[UvVolume.index(x,y,z)]=chunk==null||chunk.getBlockState(at).getLightBlock(world,at)>=15;
            }CACHE.put(lamp.getBlockPos(),UvVolume.bake(solid));dirty=true;
        }
        if(!dirty)return;var sources=new ArrayList<UvSectionField.Source>();for(var lamp:lamps){var o=origin(lamp);sources.add(new UvSectionField.Source(o.getX(),o.getY(),o.getZ(),lamp.getBlockPos().getY()+1,CACHE.get(lamp.getBlockPos())));}
        field=UvSectionField.compose(sources);atlas=texture(atlas,ATLAS,field.atlasWidth,field.atlasHeight,field.atlasPixels());index=texture(index,INDEX,field.indexWidth,field.indexHeight,field.indexPixels());dirty=false;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY)return;worldPass=handPass=false;var mc=Minecraft.getInstance();if(mc.level==null)return;collect(mc);
        var live=lamps.stream().filter(p->!p.isRemoved()&&p.hasLamp()).toList();if(!live.equals(lamps)){lamps=live;dirty=true;}
        lastLightCount=lamps.size();handLight=0;if(lamps.isEmpty())return;volumes();inverseView.set(e.getModelViewMatrix()).invert();var eye=e.getCamera().getPosition();var direction=e.getCamera().getLookVector();
        var hand=eye.add(direction.x()*.45,direction.y()*.45-.4,direction.z()*.45);handLight=field.sample(hand.x,hand.y,hand.z);worldPass=true;renderedFrames++;
    }
    public static void beginHands(){handPass=worldPass;}public static void endHands(){handPass=false;}public static void finishWorld(){worldPass=handPass=false;}
    public static void bind(ShaderInstance shader){
        if(shader.getUniform("CalUvCount")==null)return;boolean enabled=worldPass&&atlas!=null&&index!=null&&field!=null&&field.count>0&&world==Minecraft.getInstance().level;
        // Shaders retain sampler IDs. Clear disabled bindings before an atlas can be
        // replaced/released, including GUI rendering immediately after world rendering.
        shader.setSampler("CalUvVolume",enabled?atlas.getId():0);shader.setSampler("CalUvIndex",enabled?index.getId():0);
        shader.safeGetUniform("CalUvCount").set(enabled?1:0);if(!enabled)return;var eye=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        shader.safeGetUniform("CalUvInverseView").set(inverseView);shader.safeGetUniform("CalUvHand").set(handPass?1:0);shader.safeGetUniform("CalUvHandLight").set(handLight);shader.safeGetUniform("CalUvGamma").set(Minecraft.getInstance().options.gamma().get().floatValue());
        shader.safeGetUniform("CalUvOrigin").set((float)(field.originX-eye.x),(float)(field.originY-eye.y),(float)(field.originZ-eye.z));shader.safeGetUniform("CalUvExtent").set((float)field.extentX,(float)field.extentY,(float)field.extentZ);shader.safeGetUniform("CalUvColumns").set(field.columns);shader.safeGetUniform("CalUvIndexWidth").set(field.indexWidth);shader.safeGetUniform("CalUvCapacity").set(field.capacity);shader.safeGetUniform("CalUvProbes").set(field.probes);
    }
}
