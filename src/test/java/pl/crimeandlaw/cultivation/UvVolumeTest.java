package pl.crimeandlaw.cultivation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UvVolumeTest {
    private static final int S=UvVolume.SIZE,C=UvVolume.CENTER;
    private static boolean[] empty(){return new boolean[S*S*S];}
    @Test void fittingIlluminatesDownwardOnly(){var v=UvVolume.bake(empty());assertTrue(v[UvVolume.index(C,C-2,C)]>.3);assertEquals(0,v[UvVolume.index(C,C+2,C)]);assertEquals(0,v[UvVolume.index(C+3,C,C)]);}
    @Test void beamHasASoftEdgeAndLosesStrengthWithDistance(){double x=UvVolume.SOURCE_X,y=UvVolume.SOURCE_Y,z=UvVolume.SOURCE_Z;assertTrue(UvVolume.irradiance(x,y-1,z)>UvVolume.irradiance(x,y-3,z));assertTrue(UvVolume.irradiance(x+1,y-1,z)>UvVolume.irradiance(x+2,y-1,z));assertTrue(UvVolume.irradiance(x+2,y-1,z)>UvVolume.irradiance(x+3,y-1,z));assertEquals(0,UvVolume.irradiance(x+5,y-1,z));}
    @Test void opaqueShelfBlocksTheBeam(){var blocks=empty();for(int z=C-2;z<=C+2;z++)for(int x=C-2;x<=C+2;x++)blocks[UvVolume.index(x,C-1,z)]=true;assertEquals(0,UvVolume.bake(blocks)[UvVolume.index(C,C-3,C)]);assertTrue(UvVolume.bake(empty())[UvVolume.index(C,C-3,C)]>0);}
    @Test void solidWallBlocksTheOtherRoom(){var blocks=empty();for(int y=0;y<S;y++)for(int x=0;x<S;x++)blocks[UvVolume.index(x,y,C+1)]=true;assertEquals(0,UvVolume.bake(blocks)[UvVolume.index(C,C-3,C+3)]);for(int y=C-2;y<=C;y++)blocks[UvVolume.index(C,y,C+1)]=false;assertTrue(UvVolume.bake(blocks)[UvVolume.index(C,C-3,C+3)]>0);}
    @Test void blockedSourceProducesNoLight(){var blocks=empty();blocks[UvVolume.index(C,C,C)]=true;for(float n:UvVolume.bake(blocks))assertEquals(0,n);}
    @Test void refreshDoesNotChangeStationaryLight(){var blocks=empty();var a=UvVolume.bake(blocks);assertArrayEquals(a,UvVolume.bake(blocks));blocks[UvVolume.index(C,C+3,C)]=true;assertArrayEquals(a,UvVolume.bake(blocks));}
    @Test void samplingAboveTheFittingIsDark(){var v=UvVolume.bake(empty());assertEquals(0,UvVolume.sample(v,C+.5,UvVolume.SOURCE_Y,C+.5));}
    @Test void volumeIsFiniteAndBounded(){for(float v:UvVolume.bake(empty()))assertTrue(Float.isFinite(v)&&v>=0&&v<=1);assertEquals(0,UvVolume.bake(empty())[UvVolume.index(0,0,0)]);assertThrows(IllegalArgumentException.class,()->UvVolume.bake(new boolean[1]));}
    @Test void samplingCellCentersPreservesTheirValue(){var v=UvVolume.bake(empty());assertEquals(v[UvVolume.index(C,C,C)],UvVolume.sample(v,C+.5,C+.5,C+.5));}
    @Test void trilinearSampleBlendsAllEightNeighbors(){var v=emptyFloats();v[UvVolume.index(C,C,C)]=1;assertEquals(.125,UvVolume.sample(v,C,C,C),1e-9);assertEquals(.5,UvVolume.sample(v,C+.5,C+.5,C),1e-9);}
    private static float[] emptyFloats(){return new float[S*S*S];}
    @Test void outOfRangeAndInvalidSampleCoordinatesStayDark(){var v=UvVolume.bake(empty());for(double x:new double[]{-1,S+1,Double.NaN,Double.POSITIVE_INFINITY})assertEquals(0,UvVolume.sample(v,x,C,C));}
}
