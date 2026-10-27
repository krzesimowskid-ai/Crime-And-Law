package pl.crimeandlaw.cultivation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.cultivation.PreparationPhysics.*;

class PotCollisionTest {
    private static Stream drop(double x,double z){return new Stream(new Point(x,1,z),new Point(0,-.2,0),true);}
    @Test void centeredSoilStopsOnTheGrowingSurfaceRatherThanBelowThePot(){
        for(double fill:new double[]{0,.2,.7,1}){
            var hit=PotCollision.trace(drop(.5,.5),0,2,fill,.006);
            assertNotNull(hit);assertEquals(PotCollision.Surface.SOIL,hit.surface());
            assertEquals(PreparationPhysics.soilY(fill)+.006,hit.point().y(),.0001);
        }
    }
    @Test void materialAimedAtTheRimDoesNotTunnelAcrossItDuringASlowFrame(){
        double x=.5+.34*Math.cos(Math.PI/8);
        for(double radius:new double[]{.0018,.006}){
            var hit=PotCollision.trace(drop(x,.5),0,1,0,radius);
            assertEquals(PotCollision.Surface.RIM,hit.surface());assertTrue(hit.point().y()>=.48);
        }
    }
    @Test void aJetEnteringTheMouthCanHitTheInnerWallBeforeReachingTheBottom(){
        var stream=new Stream(new Point(.5,1,.5),new Point(.34,-.2,0),true);
        var hit=PotCollision.trace(stream,0,1,0,.002);
        assertEquals(PotCollision.Surface.WALL,hit.surface());assertTrue(hit.point().y()>.08);
        assertEquals(PotCollision.innerRadius(hit.point().y())-.002/Math.cos(Math.PI/8),PotCollision.radius(hit.point()),.0001);
    }
    @Test void anOutsideJetCannotEnterThePotThroughItsSide(){
        var stream=new Stream(new Point(1,.35,.5),new Point(-.8,0,0),true);
        var hit=PotCollision.trace(stream,0,.5,0,.002);
        assertEquals(PotCollision.Surface.WALL,hit.surface());assertTrue(hit.point().x()>.7);
    }
    @Test void aMissFallsOutsideWhileTheInsideAboveSoilIsHollow(){
        assertNull(PotCollision.surface(new Point(.5,.25,.5),0,.006));
        var hit=PotCollision.trace(drop(1.1,.5),0,2,0,.006);
        assertEquals(PotCollision.Surface.FLOOR,hit.surface());assertEquals(.021,hit.point().y(),.0001);
    }
    @Test void substepsAgreeWithASingleLongStepForWallRimAndSoil(){
        for(var stream:new Stream[]{drop(.5,.5),drop(.82,.5),new Stream(new Point(.5,1,.5),new Point(.34,-.2,0),true)}){
            var whole=PotCollision.trace(stream,0,1,0,.002);PotCollision.Contact small=null;
            for(double t=0;t<1&&small==null;t+=.016)small=PotCollision.trace(stream,t,t+.016,0,.002);
            assertNotNull(small);assertEquals(whole.surface(),small.surface());assertEquals(whole.time(),small.time(),.0001);
        }
    }
}
