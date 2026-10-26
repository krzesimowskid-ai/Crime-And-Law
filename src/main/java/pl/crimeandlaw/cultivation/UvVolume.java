package pl.crimeandlaw.cultivation;

/** A separate colored block-light field. Six-neighbor propagation, one level lost per block. */
public final class UvVolume {
    public static final int LEVEL=10,CENTER=LEVEL,SIZE=LEVEL*2+1,MAX_LIGHTS=8;
    private UvVolume(){}
    public static int index(int x,int y,int z){return (y*SIZE+z)*SIZE+x;}
    public static float sample(float[] v,double x,double y,double z){
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||x<0||y<0||z<0||x>SIZE||y>SIZE||z>SIZE)return 0;
        x=Math.clamp(x-.5,0,SIZE-1.001);y=Math.clamp(y-.5,0,SIZE-1.001);z=Math.clamp(z-.5,0,SIZE-1.001);int ix=(int)x,iy=(int)y,iz=(int)z;double fx=x-ix,fy=y-iy,fz=z-iz,result=0;
        for(int dy=0;dy<=1;dy++)for(int dz=0;dz<=1;dz++)for(int dx=0;dx<=1;dx++)result+=v[index(ix+dx,iy+dy,iz+dz)]*(dx==0?1-fx:fx)*(dy==0?1-fy:fy)*(dz==0?1-fz:fz);
        return (float)result;
    }
    public static float[] bake(boolean[] solid){
        if(solid.length!=SIZE*SIZE*SIZE)throw new IllegalArgumentException("Invalid UV volume");
        var result=new float[solid.length];int source=index(CENTER,CENTER,CENTER);if(solid[source])return result;
        var levels=new byte[solid.length];var queue=new int[solid.length];int head=0,tail=0;levels[source]=LEVEL;queue[tail++]=source;
        while(head<tail){int at=queue[head++],next=levels[at]-1;if(next<=0)continue;int x=at%SIZE,z=at/SIZE%SIZE,y=at/(SIZE*SIZE);
            for(int side=0;side<6;side++){int xx=x+(side==0?1:side==1?-1:0),yy=y+(side==2?1:side==3?-1:0),zz=z+(side==4?1:side==5?-1:0);
                if(xx<0||yy<0||zz<0||xx>=SIZE||yy>=SIZE||zz>=SIZE)continue;int n=index(xx,yy,zz);if(solid[n]||levels[n]!=0)continue;levels[n]=(byte)next;queue[tail++]=n;}}
        for(int i=0;i<result.length;i++){float level=levels[i]/(float)LEVEL;result[i]=level/(4-3*level);}
        return result;
    }
}
