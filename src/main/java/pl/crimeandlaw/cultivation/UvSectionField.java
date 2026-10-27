package pl.crimeandlaw.cultivation;

import java.util.*;

/** Sparse lightmap sections. Composition and lookup do not depend on the camera or a lamp-count limit. */
public final class UvSectionField {
    public static final int EDGE=16,PAD=18,TILE_WIDTH=PAD*PAD,TILE_HEIGHT=PAD;
    public record Key(int x,int y,int z) implements Comparable<Key>{public int compareTo(Key b){int c=Integer.compare(x,b.x);if(c==0)c=Integer.compare(y,b.y);return c==0?Integer.compare(z,b.z):c;}}
    public record Source(int x,int y,int z,int ceiling,float[] values){}
    private record Section(float[] light,int[] ceiling){}
    private final Map<Key,Section> sections;
    public final int originX,originY,originZ,extentX,extentY,extentZ,columns,atlasWidth,atlasHeight,indexWidth,indexHeight,capacity,probes,count;
    private final int[] atlas,index;
    private UvSectionField(Map<Key,Section> sections,SortedSet<Key> keys){
        this.sections=sections;count=keys.size();
        originX=keys.stream().mapToInt(Key::x).min().orElse(0)*16;originY=keys.stream().mapToInt(Key::y).min().orElse(0)*16;originZ=keys.stream().mapToInt(Key::z).min().orElse(0)*16;
        extentX=keys.stream().mapToInt(Key::x).max().orElse(0)-originX/16;extentY=keys.stream().mapToInt(Key::y).max().orElse(0)-originY/16;extentZ=keys.stream().mapToInt(Key::z).max().orElse(0)-originZ/16;
        columns=Math.max(1,(int)Math.ceil(Math.sqrt(Math.max(1,count)*TILE_HEIGHT/(double)TILE_WIDTH)));
        atlasWidth=columns*TILE_WIDTH;atlasHeight=Math.max(1,(count+columns-1)/columns)*TILE_HEIGHT;atlas=new int[atlasWidth*atlasHeight];
        int cap=4;while(cap<Math.max(1,count)*4)cap*=2;capacity=cap;
        indexWidth=Math.min(1024,capacity*3);indexHeight=(capacity*3+indexWidth-1)/indexWidth;index=new int[indexWidth*indexHeight];
        int slot=0,maxProbe=1;
        for(var key:keys){
            int dx=key.x-originX/16,dy=key.y-originY/16,dz=key.z-originZ/16,at=hash(dx,dy,dz)&(capacity-1),probe=1;
            while(index[at*3+2]!=0){at=(at+1)&(capacity-1);probe++;}maxProbe=Math.max(maxProbe,probe);
            int value=slot+1;index[at*3]=pack(dx,dx>>>8,dx>>>16,dy);index[at*3+1]=pack(dy>>>8,dy>>>16,dz,dz>>>8);index[at*3+2]=pack(dz>>>16,value,value>>>8,value>>>16);
            int ox=(slot%columns)*TILE_WIDTH,oy=(slot/columns)*TILE_HEIGHT;
            for(int y=0;y<PAD;y++)for(int z=0;z<PAD;z++)for(int x=0;x<PAD;x++){
                int wx=key.x*16+x-1,wy=key.y*16+y-1,wz=key.z*16+z-1;
                var sec=sections.get(new Key(wx>>4,wy>>4,wz>>4));if(sec==null)continue;
                int i=cell(wx&15,wy&15,wz&15);float light=sec.light[i];if(light<=0)continue;
                int ceiling=sec.ceiling[i]-originY;
                atlas[(oy+z)*atlasWidth+ox+y*PAD+x]=pack(Math.clamp(Math.round(light*255),0,255),ceiling,ceiling>>>8,ceiling>>>16);
            }slot++;
        }probes=maxProbe;
    }
    public int[] atlasPixels(){return atlas;}public int[] indexPixels(){return index;}
    public static int hash(int x,int y,int z){return x*73856093^y*19349663^z*83492791;}
    private static int pack(int r,int g,int b,int a){return r&255|(g&255)<<8|(b&255)<<16|(a&255)<<24;}
    private static int cell(int x,int y,int z){return (y*16+z)*16+x;}
    public static UvSectionField compose(Collection<Source> sources){
        var sections=new HashMap<Key,Section>();var keys=new TreeSet<Key>();int s=UvVolume.SIZE;
        for(var source:sources){
            if(source.values.length!=s*s*s)throw new IllegalArgumentException("Invalid source volume");
            for(int y=0;y<s;y++)for(int z=0;z<s;z++)for(int x=0;x<s;x++){
                float v=source.values[UvVolume.index(x,y,z)];if(!Float.isFinite(v)||v<=0)continue;v=Math.min(1,v);
                int wx=source.x+x,wy=source.y+y,wz=source.z+z;
                var key=new Key(wx>>4,wy>>4,wz>>4);var sec=sections.computeIfAbsent(key,k->{var ceilings=new int[4096];Arrays.fill(ceilings,Integer.MIN_VALUE);return new Section(new float[4096],ceilings);});int i=cell(wx&15,wy&15,wz&15);
                sec.light[i]=Math.max(sec.light[i],v);sec.ceiling[i]=Math.max(sec.ceiling[i],source.ceiling);
                // Adjacent empty sections need the border samples too, to interpolate without seams.
                for(int dy:(wy&15)==0?new int[]{-1,0}:(wy&15)==15?new int[]{0,1}:new int[]{0})
                    for(int dz:(wz&15)==0?new int[]{-1,0}:(wz&15)==15?new int[]{0,1}:new int[]{0})
                        for(int dx:(wx&15)==0?new int[]{-1,0}:(wx&15)==15?new int[]{0,1}:new int[]{0})keys.add(new Key(key.x+dx,key.y+dy,key.z+dz));
            }
        }return new UvSectionField(sections,keys);
    }
    /** Same quantized samples and hard upper emission plane as the material shader. */
    public float sample(double x,double y,double z){
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))return 0;
        int bx=(int)Math.floor(x-.5),by=(int)Math.floor(y-.5),bz=(int)Math.floor(z-.5);double fx=x-.5-bx,fy=y-.5-by,fz=z-.5-bz,value=0;
        for(int dy=0;dy<=1;dy++)for(int dz=0;dz<=1;dz++)for(int dx=0;dx<=1;dx++){
            int xx=bx+dx,yy=by+dy,zz=bz+dz;var sec=sections.get(new Key(xx>>4,yy>>4,zz>>4));if(sec==null)continue;int i=cell(xx&15,yy&15,zz&15);
            if(y>=sec.ceiling[i]+.545)continue;
            value+=Math.round(sec.light[i]*255)/255.0*(dx==0?1-fx:fx)*(dy==0?1-fy:fy)*(dz==0?1-fz:fz);
        }return (float)value;
    }
}
