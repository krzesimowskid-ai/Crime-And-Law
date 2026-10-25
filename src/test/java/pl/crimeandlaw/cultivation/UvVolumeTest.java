package pl.crimeandlaw.cultivation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UvVolumeTest {
    private static boolean[] empty(){return new boolean[13*13*13];}
    @Test void illuminationReachesWallsAndCeilingHeight(){var v=UvVolume.bake(empty());assertTrue(v[UvVolume.index(3,6,6)]>.5);assertTrue(v[UvVolume.index(6,9,6)]>.5);assertTrue(v[UvVolume.index(6,4,6)]>.5);}
    @Test void aSolidWallBlocksTheOtherRoom(){var blocks=empty();for(int y=0;y<13;y++)for(int z=0;z<13;z++)blocks[UvVolume.index(8,y,z)]=true;var v=UvVolume.bake(blocks);assertEquals(0,v[UvVolume.index(10,6,6)]);assertTrue(v[UvVolume.index(7,6,6)]>0);}
    @Test void removingTheWallRestoresLight(){var blocks=empty();blocks[UvVolume.index(7,6,6)]=true;assertEquals(0,UvVolume.bake(blocks)[UvVolume.index(9,6,6)]);blocks[UvVolume.index(7,6,6)]=false;assertTrue(UvVolume.bake(blocks)[UvVolume.index(9,6,6)]>0);}
    @Test void volumeIsFiniteAndBounded(){for(float v:UvVolume.bake(empty()))assertTrue(Float.isFinite(v)&&v>=0&&v<=1);assertEquals(0,UvVolume.bake(empty())[UvVolume.index(0,0,0)]);assertThrows(IllegalArgumentException.class,()->UvVolume.bake(new boolean[1]));}
    @Test void samplingCellCentersPreservesTheirValue(){var v=UvVolume.bake(empty());assertEquals(v[UvVolume.index(6,6,6)],UvVolume.sample(v,6.5,6.5,6.5));}
    @Test void trilinearSampleBlendsAllEightNeighbors(){var v=new float[13*13*13];v[UvVolume.index(7,7,7)]=1;assertEquals(.125,UvVolume.sample(v,7,7,7),1e-9);assertEquals(.5,UvVolume.sample(v,7.5,7.5,7),1e-9);}
    @Test void outOfRangeAndInvalidSampleCoordinatesStayDark(){var v=UvVolume.bake(empty());for(double x:new double[]{-1,14,Double.NaN,Double.POSITIVE_INFINITY})assertEquals(0,UvVolume.sample(v,x,6,6));}
}
