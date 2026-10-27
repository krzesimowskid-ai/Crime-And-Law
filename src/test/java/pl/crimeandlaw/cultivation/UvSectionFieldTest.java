package pl.crimeandlaw.cultivation;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UvSectionFieldTest {
 private static float[] light(){return UvVolume.bake(new boolean[21*21*21]);}
 private static UvSectionField.Source source(int x,int y,int z,float[] values){return new UvSectionField.Source(x-10,y-9,z-10,y+1,values);}
 @Test void cameraIndependentFieldRetainsMoreThanEightSeparatedLamps(){var v=light();var sources=new ArrayList<UvSectionField.Source>();for(int i=0;i<20;i++)sources.add(source(i*32,0,0,v));var field=UvSectionField.compose(sources);for(int i=0;i<20;i++)assertTrue(field.sample(i*32+.5,.5,.5)>.2,"Source "+i);}
 @Test void samplesStayContinuousAcrossNegativeAndPositiveChunkBorders(){var v=light();for(int center:new int[]{-17,-1,15,31}){var f=UvSectionField.compose(List.of(source(center,0,center,v)));for(double dx:new double[]{-.51,.49,.51,1.01})for(double dy:new double[]{-1.2,.5,1.2}){double x=center+dx,z=center+.51;assertEquals(UvVolume.sample(v,x-(center-10),dy+9,z-(center-10)),f.sample(x,dy,z),.004);assertEquals(f.sample(x,dy,z),packed(f,x,dy,z),.00001);}}}
 @Test void downwardPlaneWorksAlsoBelowWorldZero(){var v=light();var f=UvSectionField.compose(List.of(source(0,-60,0,v)));assertTrue(f.sample(.5,-59.1,.5)>.2);assertEquals(0,f.sample(.5,-58.455,.5));assertEquals(0,packed(f,.5,-58.4,.5));}
 @Test void compositionHasStableSlotsAndTexelsWithoutCameraSorting(){var v=light();var a=source(-24,0,-16,v);var b=source(24,0,16,v);var one=UvSectionField.compose(List.of(a,b));var two=UvSectionField.compose(List.of(b,a));assertArrayEquals(one.atlasPixels(),two.atlasPixels());assertArrayEquals(one.indexPixels(),two.indexPixels());}
 @Test void emptyFieldAndOutsideLookupsAreDark(){var f=UvSectionField.compose(List.of());assertEquals(0,f.count);assertEquals(0,f.sample(1,2,3));assertEquals(0,packed(f,1000,0,0));}
 // Independent emulation of the shader's packed hash table and padded atlas addressing.
 private static float packed(UvSectionField f,double x,double y,double z){
  double qx=x-f.originX,qy=y-f.originY,qz=z-f.originZ;int sx=(int)Math.floor(qx/16),sy=(int)Math.floor(qy/16),sz=(int)Math.floor(qz/16),slot=-1;
  int hash=sx*73856093^sy*19349663^sz*83492791,at=hash&(f.capacity-1);var table=f.indexPixels();
  for(int i=0;i<f.probes;i++){
   int a=table[at*3],b=table[at*3+1],c=table[at*3+2],value=c>>>8;if(value==0)return 0;
   if((a&0xffffff)==sx&&((a>>>24)|((b&65535)<<8))==sy&&((b>>>16)|((c&255)<<16))==sz){slot=value-1;break;}at=(at+1)&(f.capacity-1);
  }if(slot<0)return 0;
  double lx=qx-sx*16-.5,ly=qy-sy*16-.5,lz=qz-sz*16-.5;int bx=(int)Math.floor(lx)+1,by=(int)Math.floor(ly)+1,bz=(int)Math.floor(lz)+1;double fx=lx-Math.floor(lx),fy=ly-Math.floor(ly),fz=lz-Math.floor(lz),sum=0;
  for(int dy=0;dy<=1;dy++)for(int dz=0;dz<=1;dz++)for(int dx=0;dx<=1;dx++){
   int px=(slot%f.columns)*324+(by+dy)*18+bx+dx,pz=(slot/f.columns)*18+bz+dz,value=f.atlasPixels()[pz*f.atlasWidth+px],ceiling=value>>>8;
   if(qy>=ceiling+.545)continue;sum+=(value&255)/255.0*(dx==0?1-fx:fx)*(dy==0?1-fy:fy)*(dz==0?1-fz:fz);
  }return (float)sum;
 }
}
