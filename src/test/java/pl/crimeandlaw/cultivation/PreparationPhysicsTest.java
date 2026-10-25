package pl.crimeandlaw.cultivation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.cultivation.PreparationPhysics.*;
class PreparationPhysicsTest {
    @Test void bagEmitterStaysAtRealOpeningAfterRotation(){var p=new Pose(.5,1,.5,Math.PI,0);var s=PreparationPhysics.stream(p,false,true);assertEquals(.5,s.source().x(),1e-8);assertEquals(1-.37*.75,s.source().y(),1e-8);assertTrue(s.flowing());}
    @Test void uprightOrReleasedToolsDoNotPour(){for(boolean water:new boolean[]{true,false}){assertFalse(PreparationPhysics.stream(new Pose(.5,1,.5,0,0),water,true).flowing());assertFalse(PreparationPhysics.stream(new Pose(.5,1,.5,water?-Math.PI/2:Math.PI,0),water,false).flowing());}}
    @Test void centeredBagReachesEveryFillHeight(){for(int i=0;i<=100;i++)assertTrue(PreparationPhysics.reachesSoil(PreparationPhysics.stream(new Pose(.5,1,.5,Math.PI,0),false,true),i/100.),"fill "+i);}
    @Test void tiltedCanFeedsTheActualSpout(){var p=new Pose(.5,1,.5,-Math.PI/2,0);var s=PreparationPhysics.stream(p,true,true);assertEquals(.5+.09*.75,s.source().x(),1e-8);assertEquals(1-.476*.75,s.source().y(),1e-8);assertTrue(PreparationPhysics.reachesSoil(s,1));}
    @Test void movingToolMovesEmitterAndMissingPotDoesNotCount(){var a=PreparationPhysics.stream(new Pose(.5,1,.5,Math.PI,0),false,true);var b=PreparationPhysics.stream(new Pose(1.3,1,.5,Math.PI,0),false,true);assertEquals(.8,b.source().x()-a.source().x(),1e-8);assertFalse(PreparationPhysics.reachesSoil(b,0));assertFalse(PreparationPhysics.reachesSoil(b,1));}
    @Test void sidePitchChangesTheFlightInDepth(){var a=PreparationPhysics.stream(new Pose(.5,1,.5,Math.PI,0),false,true);var b=PreparationPhysics.stream(new Pose(.5,1,.5,Math.PI,.6),false,true);assertNotEquals(a.source().z(),b.source().z());assertNotEquals(a.velocity().z(),b.velocity().z());}
    @Test void surfaceAlwaysFitsInsideTaperedCavity(){double last=0;for(int i=0;i<=1000;i++){double f=i/1000.,y=PreparationPhysics.soilY(f),r=PreparationPhysics.soilRadius(f);assertTrue(y>=.065&&y<=.385);assertTrue(r>=last);assertTrue(r<.215+.095*(y-.055)/.375-.008);last=r;}}
    @Test void seedFallsAccordingToGravityAndHitsAtPredictedTime(){var source=new Point(.5,.83,.5);double t=PreparationPhysics.flight(source,new Point(0,0,0),.345);var p=PreparationPhysics.at(new Stream(source,new Point(0,0,0),true),t);assertEquals(.345,p.y(),1e-8);assertTrue(t>.5&&t<.7);assertTrue(PreparationPhysics.inside(p.x(),p.z(),.17));}
    @Test void nonFiniteOrDistantControlPointsCannotBeAccepted(){for(double bad:new double[]{Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,100,-100})assertFalse(PreparationPhysics.bounded(new Pose(bad,1,.5,0,0)));assertFalse(PreparationPhysics.bounded(new Pose(.5,100,.5,0,0)));assertFalse(PreparationPhysics.bounded(new Pose(.5,1,.5,100,0)));}
    @Test void floatWireAnglesKeepValidPiRotation(){assertTrue(PreparationPhysics.bounded(new Pose(.5,1,.5,(float)Math.PI,0)));}
    @Test void octagonRejectsSquareCorners(){assertTrue(PreparationPhysics.inside(.5,.5,.3));assertFalse(PreparationPhysics.inside(.5+.3,.5+.3,.3));assertFalse(PreparationPhysics.inside(.5,.5+.3,.3));}
}
