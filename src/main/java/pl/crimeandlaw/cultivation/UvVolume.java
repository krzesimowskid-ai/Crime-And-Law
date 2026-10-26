package pl.crimeandlaw.cultivation;
/** Direct, downward area light from the underside of the UV fitting. */
public final class UvVolume {
 public static final int CENTER=10,SIZE=CENTER*2+1,MAX_LIGHTS=8;
 public static final double SOURCE_X=CENTER+.5,SOURCE_Y=CENTER+.545,SOURCE_Z=CENTER+.5,RANGE=7;
 private static final double[][] EMITTERS={{0,0},{-.18,-.18},{.18,-.18},{-.18,.18},{.18,.18}};
 private UvVolume(){}
 public static int index(int x,int y,int z){return (y*SIZE+z)*SIZE+x;}
 public static float sample(float[] v,double x,double y,double z){
  if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||x<0||y<0||z<0||x>SIZE||y>=SOURCE_Y||z>SIZE)return 0;
  x=Math.clamp(x-.5,0,SIZE-1.001);y=Math.clamp(y-.5,0,SIZE-1.001);z=Math.clamp(z-.5,0,SIZE-1.001);int ix=(int)x,iy=(int)y,iz=(int)z;double fx=x-ix,fy=y-iy,fz=z-iz,result=0;
  for(int dy=0;dy<=1;dy++)for(int dz=0;dz<=1;dz++)for(int dx=0;dx<=1;dx++)result+=v[index(ix+dx,iy+dy,iz+dz)]*(dx==0?1-fx:fx)*(dy==0?1-fy:fy)*(dz==0?1-fz:fz);
  return (float)result;
 }
 public static float[] bake(boolean[] solid){
  if(solid.length!=SIZE*SIZE*SIZE)throw new IllegalArgumentException("Invalid UV volume");var result=new float[solid.length];if(solid[index(CENTER,CENTER,CENTER)])return result;
  for(int y=0;y<SIZE;y++)for(int z=0;z<SIZE;z++)for(int x=0;x<SIZE;x++){
   int i=index(x,y,z);if(solid[i])continue;double px=x+.5,py=y+.5,pz=z+.5;float power=irradiance(px,py,pz);if(power<=0)continue;int visible=0;
   for(var e:EMITTERS)if(clear(solid,SOURCE_X+e[0],SOURCE_Y,SOURCE_Z+e[1],px,py,pz))visible++;result[i]=power*visible/EMITTERS.length;
  }return result;
 }
 public static float irradiance(double x,double y,double z){
  double down=SOURCE_Y-y;if(!(down>0)||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))return 0;
  double dx=Math.max(0,Math.abs(x-SOURCE_X)-.28),dz=Math.max(0,Math.abs(z-SOURCE_Z)-.24),d=Math.sqrt(dx*dx+down*down+dz*dz);if(d>=RANGE)return 0;
  double edge=Math.clamp((down/d-.30)/(.82-.30),0,1),fade=1-d/RANGE;return (float)(edge*edge*(3-2*edge)*fade*fade);
 }
 private static boolean clear(boolean[] s,double sx,double sy,double sz,double x,double y,double z){
  double dx=x-sx,dy=y-sy,dz=z-sz;int steps=Math.max(1,(int)Math.ceil(Math.sqrt(dx*dx+dy*dy+dz*dz)/.18));
  for(int n=1;n<steps;n++){double t=n/(double)steps;if(s[index((int)Math.floor(sx+dx*t),(int)Math.floor(sy+dy*t),(int)Math.floor(sz+dz*t))])return false;}return true;
 }
}
