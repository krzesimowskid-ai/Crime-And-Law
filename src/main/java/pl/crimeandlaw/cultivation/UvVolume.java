package pl.crimeandlaw.cultivation;

/** Small, immutable local irradiance volume. No chunk loading or Minecraft types. */
public final class UvVolume {
    public static final int SIZE=13,MAX_LIGHTS=8;
    public static final double RADIUS=6.1;
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
        var result=new float[solid.length];
        for(int y=0;y<SIZE;y++)for(int z=0;z<SIZE;z++)for(int x=0;x<SIZE;x++){
            int i=index(x,y,z);if(solid[i])continue;double dx=x-6,dy=y+.5-6.56,dz=z-6,d=Math.sqrt(dx*dx+dy*dy+dz*dz);if(d>=RADIUS)continue;
            boolean blocked=false;int steps=Math.max(1,(int)Math.ceil(d/.22));
            for(int n=1;n<steps;n++){double t=n/(double)steps;int xx=(int)Math.floor(6.5+dx*t),yy=(int)Math.floor(6.56+dy*t),zz=(int)Math.floor(6.5+dz*t);if(solid[index(xx,yy,zz)]){blocked=true;break;}}
            if(!blocked){double edge=Math.clamp((d-3.1)/(RADIUS-3.1),0,1);result[i]=(float)((1-edge*edge*(3-2*edge))/(1+.025*d*d));}
        }return result;
    }
}
