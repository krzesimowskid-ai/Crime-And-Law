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

/** A crisp pixel material and its stepped silhouette. Source art never enters the block atlas. */
public final class PixelSprite {
    public static final int SIZE=32;
    public static final int CURVE_ROWS=8,CURVE_COLUMNS=4;
    public static final PixelSprite LEAF=new PixelSprite("aurelia_leaf",64,new int[]{0x15391E,0x204A26,0x2E5E2D,0x3D7135,0x4D843F,0x62964C,0x7BA95C,0x92B76D});
    public static final PixelSprite SHEARS=new PixelSprite("pruning_shears",new int[]{0x082C32,0x10444B,0x185E64,0x207C81,0x3B9B9A,0x3D2926,0x68402E,0x945D3F,0xC48458,0xEBAC7A,0x324251,0x536475,0x7C8A9B,0xABB5C0,0xD7DDE0,0xF3F0DE});
    public static final PixelSprite SEEDS=new PixelSprite("aurelia_seeds",new int[]{0x38271B,0x5C381D,0x805021,0xA56A27,0xC58A32,0xDDA33A,0xEDB94A,0xF8D16B,0xFFE6A0,0x5F6946,0x818A5B,0xA7AC77});
    public static final ResourceLocation STEM=location("aurelia_stem");
    private final ResourceLocation texture;
    private final int[] palette;
    private final int size;
    private boolean[] pixels=new boolean[SIZE*SIZE];
    private List<Edge> edges=List.of();
    private record Edge(float u0,float v0,float u1,float v1,float sampleU,float sampleV){}
    @FunctionalInterface public interface Surface {Vec3 point(float u,float v,float depth);}
    private PixelSprite(String name,int[] palette){this(name,SIZE,palette);}
    private PixelSprite(String name,int size,int[] palette){texture=location(name);this.palette=palette;this.size=size;pixels=new boolean[size*size];}
    private static ResourceLocation location(String name){return ResourceLocation.fromNamespaceAndPath("crimeandlaw","textures/cultivation/"+name+".png");}
    public ResourceLocation texture(){return texture;}
    public int resolution(){return size;}
    public boolean opaque(int x,int y){return x>=0&&y>=0&&x<size&&y<size&&pixels[y*size+x];}
    public int edgeCount(){return edges.size();}
    public static void reloadAll(ResourceManager manager){
        try{for(var sprite:List.of(LEAF,SHEARS,SEEDS))sprite.reload(manager);}catch(IOException error){throw new java.io.UncheckedIOException(error);}
        var stem=new NativeImage(8,8,false);
        int[] green={0x365B2E,0x406834,0x4B743A,0x568040};
        for(int y=0;y<8;y++)for(int x=0;x<8;x++)stem.setPixelRGBA(x,y,abgr(green[Math.min(3,(x%4)+(y%5==0?1:0))]));
        register(STEM,stem);
    }
    private void reload(ResourceManager manager)throws IOException{
        var image=new NativeImage(size,size,false);boolean[] mask=new boolean[size*size];
        try(var in=manager.open(texture);var source=NativeImage.read(in)){
            int x0=0,y0=0,x1=source.getWidth()-1,y1=source.getHeight()-1;
            if(this==LEAF){
                x0=source.getWidth();y0=source.getHeight();x1=y1=-1;
                for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if((source.getPixelRGBA(x,y)>>>24)>=128){x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);}
                if(x1<0)throw new IOException("Empty Aurelia leaf sprite");
            }
            for(int y=0;y<size;y++)for(int x=0;x<size;x++){
                int sample=source.getPixelRGBA(Math.min(x1,x0+(int)((x+.5)*(x1-x0+1)/size)),Math.min(y1,y0+(int)((y+.5)*(y1-y0+1)/size)));
                boolean solid=(sample>>>24)>=128;mask[y*size+x]=solid;
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
        for(int y=0;y<size;y++)for(int direction:new int[]{-1,1}){
            int start=-1;for(int x=0;x<=size;x++){
                boolean edge=opaque(x,y)&&!opaque(x,y+direction);
                if(edge&&start<0)start=x;
                if(!edge&&start>=0){float v=(y+(direction>0?1:0))/(float)size;result.add(new Edge((direction<0?x:start)/(float)size,v,(direction<0?start:x)/(float)size,v,(start+.5f)/size,(y+.5f)/size));start=-1;}
            }
        }
        for(int x=0;x<size;x++)for(int direction:new int[]{-1,1}){
            int start=-1;for(int y=0;y<=size;y++){
                boolean edge=opaque(x,y)&&!opaque(x+direction,y);
                if(edge&&start<0)start=y;
                if(!edge&&start>=0){float u=(x+(direction>0?1:0))/(float)size;result.add(new Edge(u,(direction>0?y:start)/(float)size,u,(direction>0?start:y)/(float)size,(x+.5f)/size,(start+.5f)/size));start=-1;}
            }
        }
        return List.copyOf(result);
    }
    public void draw(PoseStack pose,VertexConsumer out,Surface shape,float halfThickness,int color,int light,boolean bent){
        int rows=bent?CURVE_ROWS:1,cols=bent?CURVE_COLUMNS:1;
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++)for(int face:new int[]{1,-1}){
            float u0=col/(float)cols,u1=(col+1)/(float)cols,v0=row/(float)rows,v1=(row+1)/(float)rows,d=face*halfThickness;
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
