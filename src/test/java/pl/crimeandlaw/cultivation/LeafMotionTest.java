package pl.crimeandlaw.cultivation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LeafMotionTest {
    @Test void cutsFallInDifferentDirections(){var headings=new java.util.HashSet<Integer>();for(int i=0;i<8;i++){var m=LeafMotion.cut(i,42);headings.add((int)Math.floor((Math.atan2(m.vz(),m.vx())+Math.PI)/(Math.PI/2)));}assertEquals(4,headings.size());}
    @Test void gravityEventuallyOutweighsTheInitialLift(){for(int i=0;i<8;i++){var m=LeafMotion.cut(i,42);assertEquals(0,m.y(0));assertTrue(m.y(1.3)<-.6);assertNotEquals(0,m.spinX());assertNotEquals(0,m.spinZ());}}
    @Test void cosmeticVariationIsReproducibleAndIndependentOfRewards(){assertEquals(LeafMotion.cut(3,12),LeafMotion.cut(3,12));assertNotEquals(LeafMotion.cut(3,12),LeafMotion.cut(3,13));}
}
