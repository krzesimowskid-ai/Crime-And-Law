package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;

/** A 32px material and its stepped silhouette. Source art never enters the block atlas. */
public final class PixelSprite {
    public static final int SIZE=32;
    public static final PixelSprite LEAF=new PixelSprite("aurelia_leaf",new int[]{0x17341D,0x24492B,0x365A32,0x456D38,0x578341,0x70964C,0x91AF63,0xB3C87D,0x9E9A42,0xBEAE51,0xDCC66B,0xE9D88A});
    public static final PixelSprite SHEARS=new PixelSprite("pruning_shears",new int[]{0x082C32,0x10444B,0x185E64,0x207C81,0x3B9B9A,0x3D2926,0x68402E,0x945D3F,0xC48458,0xEBAC7A,0x324251,0x536475,0x7C8A9B,0xABB5C0,0xD7DDE0,0xF3F0DE});
    public static final PixelSprite SEEDS=new PixelSprite("aurelia_seeds",new int[]{0x38271B,0x5C381D,0x805021,0xA56A27,0xC58A32,0xDDA33A,0xEDB94A,0xF8D16B,0xFFE6A0,0x5F6946,0x818A5B,0xA7AC77});
    public static final ResourceLocation STEM=location("aurelia_stem");
    private final ResourceLocation texture;
    private final int[] palette;
    private boolean[] pixels=new boolean[SIZE*SIZE];
    private List<Edge> edges=List.of();
    private record Edge(float u0,float v0,float u1,float v1,float sampleU,float sampleV){}
    @FunctionalInterface public interface Surface {Vec3 point(float u,float v,float depth);}
    private PixelSprite(String name,int[] palette){texture=location(name);this.palette=palette;}
    private static ResourceLocation location(String name){return ResourceLocation.fromNamespaceAndPath("crimeandlaw","textures/cultivation/"+name+".png");}
    public ResourceLocation texture(){return texture;}
    public boolean opaque(int x,int y){return x>=0&&y>=0&&x<SIZE&&y<SIZE&&pixels[y*SIZE+x];}
    public int edgeCount(){return edges.size();}
    public static void reloadAll(ResourceManager manager){
        try{for(var sprite:List.of(LEAF,SHEARS,SEEDS))sprite.reload(manager);}catch(IOException error){throw new java.io.UncheckedIOException(error);}
        var stem=new NativeImage(8,8,false);
        int[] green={0x435E30,0x58753B,0x6A8646,0x819B52};
        for(int y=0;y<8;y++)for(int x=0;x<8;x++)stem.setPixelRGBA(x,y,abgr(green[(x+(y/3)%2)%4]));
        register(STEM,stem);
    }
    private void reload(ResourceManager manager)throws IOException{
        var image=new NativeImage(SIZE,SIZE,false);boolean[] mask=new boolean[SIZE*SIZE];
        try(var in=manager.open(texture);var source=NativeImage.read(in)){
            for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
                int sample=source.getPixelRGBA(Math.min(source.getWidth()-1,(int)((x+.5)*source.getWidth()/SIZE)),Math.min(source.getHeight()-1,(int)((y+.5)*source.getHeight()/SIZE)));
                boolean solid=(sample>>>24)>=128;mask[y*SIZE+x]=solid;
                image.setPixelRGBA(x,y,solid?abgr(nearest(sample)):0);
            }
        }
        pixels=mask;edges=outline();register(texture,image);
    }
    private int nearest(int abgr){
        int r=abgr&255,g=abgr>>>8&255,b=abgr>>>16&255,best=palette[0],distance=Integer.MAX_VALUE;
        for(int rgb:palette){int dr=r-(rgb>>>16&255),dg=g-(rgb>>>8&255),db=b-(rgb&255),d=dr*dr+dg*dg+db*db;if(d<distance){distance=d;best=rgb;}}
        return best;
    }
    private static int abgr(int rgb){return 0xFF000000|(rgb&255)<<16|(rgb&0xFF00)|(rgb>>>16&255);}
    private static void register(ResourceLocation id,NativeImage image){
        var texture=new DynamicTexture(image){@Override public void setFilter(boolean blur,boolean mipmap){super.setFilter(false,false);}};
        Minecraft.getInstance().getTextureManager().register(id,texture);texture.setFilter(false,false);
    }
    private List<Edge> outline(){
        var result=new ArrayList<Edge>();
        // Merge collinear pixel edges: genuine thickness without a cube for every pixel.
        for(int y=0;y<SIZE;y++)for(int direction:new int[]{-1,1}){
            int start=-1;for(int x=0;x<=SIZE;x++){
                boolean edge=opaque(x,y)&&!opaque(x,y+direction);
                if(edge&&start<0)start=x;
                if(!edge&&start>=0){float v=(y+(direction>0?1:0))/(float)SIZE;result.add(new Edge((direction<0?x:start)/(float)SIZE,v,(direction<0?start:x)/(float)SIZE,v,(start+.5f)/SIZE,(y+.5f)/SIZE));start=-1;}
            }
        }
        for(int x=0;x<SIZE;x++)for(int direction:new int[]{-1,1}){
            int start=-1;for(int y=0;y<=SIZE;y++){
                boolean edge=opaque(x,y)&&!opaque(x+direction,y);
                if(edge&&start<0)start=y;
                if(!edge&&start>=0){float u=(x+(direction>0?1:0))/(float)SIZE;result.add(new Edge(u,(direction>0?y:start)/(float)SIZE,u,(direction>0?start:y)/(float)SIZE,(x+.5f)/SIZE,(start+.5f)/SIZE));start=-1;}
            }
        }
        return List.copyOf(result);
    }
    public void draw(PoseStack pose,VertexConsumer out,Surface shape,float halfThickness,int color,int light,boolean bent){
        float[] rows=bent?new float[]{0,.28f,.58f,1}:new float[]{0,1};
        float[] cols=bent?new float[]{0,.5f,1}:new float[]{0,1};
        for(int row=0;row<rows.length-1;row++)for(int col=0;col<cols.length-1;col++)for(int face:new int[]{1,-1}){
            float u0=cols[col],u1=cols[col+1],v0=rows[row],v1=rows[row+1],d=face*halfThickness;
            if(face>0)quad(pose,out,shape.point(u0,v0,d),shape.point(u0,v1,d),shape.point(u1,v1,d),shape.point(u1,v0,d),u0,v0,u1,v1,light,color);
            else quad(pose,out,shape.point(u1,v0,d),shape.point(u1,v1,d),shape.point(u0,v1,d),shape.point(u0,v0,d),u1,v0,u0,v1,light,shade(color,.83f));
        }
        for(var e:edges)quad(pose,out,shape.point(e.u0,e.v0,halfThickness),shape.point(e.u0,e.v0,-halfThickness),shape.point(e.u1,e.v1,-halfThickness),shape.point(e.u1,e.v1,halfThickness),e.sampleU,e.sampleV,e.sampleU,e.sampleV,light,shade(color,.69f));
    }
    private static int shade(int color,float factor){return color&0xFF000000|(int)((color>>>16&255)*factor)<<16|(int)((color>>>8&255)*factor)<<8|(int)((color&255)*factor);}
    public static void quad(PoseStack pose,VertexConsumer out,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float u0,float v0,float u1,float v1,int light,int color){
        Vec3 normal=b.subtract(a).cross(c.subtract(a)).normalize();
        vertex(pose,out,a,u0,v0,normal,light,color);vertex(pose,out,b,u0,v1,normal,light,color);vertex(pose,out,c,u1,v1,normal,light,color);vertex(pose,out,d,u1,v0,normal,light,color);
    }
    private static void vertex(PoseStack pose,VertexConsumer out,Vec3 p,float u,float v,Vec3 n,int light,int color){out.addVertex(pose.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(color).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),(float)n.x,(float)n.y,(float)n.z);}
}
