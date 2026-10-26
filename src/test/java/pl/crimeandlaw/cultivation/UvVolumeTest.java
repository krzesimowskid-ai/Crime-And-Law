package pl.crimeandlaw.cultivation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UvVolumeTest {
    private static final int S=UvVolume.SIZE,C=UvVolume.CENTER;
    private static boolean[] empty(){return new boolean[S*S*S];}
    @Test void illuminationReachesWallsAndCeilingHeight(){var v=UvVolume.bake(empty());assertTrue(v[UvVolume.index(C-3,C,C)]>0);assertTrue(v[UvVolume.index(C,C+3,C)]>0);assertTrue(v[UvVolume.index(C,C-2,C)]>0);}
    @Test void aSolidWallBlocksTheOtherRoom(){var blocks=empty();for(int y=0;y<S;y++)for(int z=0;z<S;z++)blocks[UvVolume.index(C+2,y,z)]=true;var v=UvVolume.bake(blocks);assertEquals(0,v[UvVolume.index(C+4,C,C)]);assertTrue(v[UvVolume.index(C+1,C,C)]>0);}
    @Test void openingADoorLetsLightReachTheOtherRoom(){var blocks=empty();for(int y=0;y<S;y++)for(int z=0;z<S;z++)blocks[UvVolume.index(C+2,y,z)]=true;assertEquals(0,UvVolume.bake(blocks)[UvVolume.index(C+4,C,C)]);blocks[UvVolume.index(C+2,C,C)]=false;assertTrue(UvVolume.bake(blocks)[UvVolume.index(C+4,C,C)]>0);}
    @Test void lightTravelsAroundAnObstructionRatherThanUsingASpotlightRay(){var blocks=empty();blocks[UvVolume.index(C+1,C,C)]=true;var v=UvVolume.bake(blocks);assertTrue(v[UvVolume.index(C+2,C,C)]>0);assertTrue(v[UvVolume.index(C+2,C,C)]<v[UvVolume.index(C-2,C,C)]);}
    @Test void distanceCostsOneLevelPerBlockOnAllSixAxes(){var v=UvVolume.bake(empty());for(int d=1;d<UvVolume.LEVEL;d++){assertTrue(v[UvVolume.index(C+d,C,C)]<v[UvVolume.index(C+d-1,C,C)]);assertEquals(v[UvVolume.index(C+d,C,C)],v[UvVolume.index(C,C-d,C)]);assertEquals(v[UvVolume.index(C+d,C,C)],v[UvVolume.index(C,C,C+d)]);}assertEquals(0,v[UvVolume.index(C+UvVolume.LEVEL,C,C)]);}
    @Test void aBlockedSourceCannotIlluminateAnotherRoom(){var blocks=empty();blocks[UvVolume.index(C,C,C)]=true;for(float n:UvVolume.bake(blocks))assertEquals(0,n);}
    @Test void volumeIsFiniteAndBounded(){for(float v:UvVolume.bake(empty()))assertTrue(Float.isFinite(v)&&v>=0&&v<=1);assertEquals(0,UvVolume.bake(empty())[UvVolume.index(0,0,0)]);assertThrows(IllegalArgumentException.class,()->UvVolume.bake(new boolean[1]));}
    @Test void samplingCellCentersPreservesTheirValue(){var v=UvVolume.bake(empty());assertEquals(v[UvVolume.index(C,C,C)],UvVolume.sample(v,C+.5,C+.5,C+.5));}
    @Test void trilinearSampleBlendsAllEightNeighbors(){var v=emptyFloats();v[UvVolume.index(C,C,C)]=1;assertEquals(.125,UvVolume.sample(v,C,C,C),1e-9);assertEquals(.5,UvVolume.sample(v,C+.5,C+.5,C),1e-9);}
    private static float[] emptyFloats(){return new float[S*S*S];}
    @Test void outOfRangeAndInvalidSampleCoordinatesStayDark(){var v=UvVolume.bake(empty());for(double x:new double[]{-1,S+1,Double.NaN,Double.POSITIVE_INFINITY})assertEquals(0,UvVolume.sample(v,x,C,C));}
}
