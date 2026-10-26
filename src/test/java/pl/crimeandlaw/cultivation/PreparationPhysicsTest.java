package pl.crimeandlaw.cultivation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.cultivation.PreparationPhysics.*;
class PreparationPhysicsTest {
    @Test void bagEmitterStaysAtRealOpeningAfterRotation(){var p=new Pose(.5,1,.5,Math.PI,0);var s=PreparationPhysics.stream(p,false,true);assertEquals(.5,s.source().x(),1e-8);assertEquals(1-.37*.75,s.source().y(),1e-8);assertTrue(s.flowing());}
    @Test void uprightOrReleasedToolsDoNotPour(){for(boolean water:new boolean[]{true,false}){assertFalse(PreparationPhysics.stream(new Pose(.5,1,.5,0,0),water,true).flowing());assertFalse(PreparationPhysics.stream(new Pose(.5,1,.5,water?-Math.PI/2:Math.PI,0),water,false).flowing());}}
    @Test void centeredBagReachesEveryFillHeight(){for(int i=0;i<=100;i++)assertTrue(PreparationPhysics.reachesSoil(PreparationPhysics.stream(new Pose(.5,1,.5,Math.PI,0),false,true),i/100.),"fill "+i);}
    @Test void tiltedCanFeedsTheActualSpout(){var p=new Pose(.5,1,.5,-Math.PI/2,0);var s=PreparationPhysics.stream(p,true,true);var rose=PreparationPhysics.rose(1,1);assertEquals(.5+(rose.y()-.45)*.75,s.source().x(),1e-8);assertEquals(1-(rose.x()-.5)*.75,s.source().y(),1e-8);assertTrue(PreparationPhysics.reachesSoil(s,1));}
    @Test void movingToolMovesEmitterAndMissingPotDoesNotCount(){var a=PreparationPhysics.stream(new Pose(.5,1,.5,Math.PI,0),false,true);var b=PreparationPhysics.stream(new Pose(1.3,1,.5,Math.PI,0),false,true);assertEquals(.8,b.source().x()-a.source().x(),1e-8);assertFalse(PreparationPhysics.reachesSoil(b,0));assertFalse(PreparationPhysics.reachesSoil(b,1));}
    @Test void sidePitchChangesTheFlightInDepth(){var a=PreparationPhysics.stream(new Pose(.5,1,.5,Math.PI,0),false,true);var b=PreparationPhysics.stream(new Pose(.5,1,.5,Math.PI,.6),false,true);assertNotEquals(a.source().z(),b.source().z());assertNotEquals(a.velocity().z(),b.velocity().z());}
    @Test void surfaceAlwaysFitsInsideTaperedCavity(){double last=0;for(int i=0;i<=1000;i++){double f=i/1000.,y=PreparationPhysics.soilY(f),r=PreparationPhysics.soilRadius(f);assertTrue(y>=.065&&y<=.385);assertTrue(r>=last);assertTrue(r<.215+.095*(y-.055)/.375-.008);last=r;}}
    @Test void seedFallsAccordingToGravityAndHitsAtPredictedTime(){var source=new Point(.5,.83,.5);double t=PreparationPhysics.flight(source,new Point(0,0,0),.345);var p=PreparationPhysics.at(new Stream(source,new Point(0,0,0),true),t);assertEquals(.345,p.y(),1e-8);assertTrue(t>.5&&t<.7);assertTrue(PreparationPhysics.inside(p.x(),p.z(),.17));}
    @Test void nonFiniteOrDistantControlPointsCannotBeAccepted(){for(double bad:new double[]{Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,100,-100})assertFalse(PreparationPhysics.bounded(new Pose(bad,1,.5,0,0)));assertFalse(PreparationPhysics.bounded(new Pose(.5,100,.5,0,0)));assertFalse(PreparationPhysics.bounded(new Pose(.5,1,.5,100,0)));}
    @Test void floatWireAnglesKeepValidPiRotation(){assertTrue(PreparationPhysics.bounded(new Pose(.5,1,.5,(float)Math.PI,0)));}
    @Test void octagonRejectsSquareCorners(){assertTrue(PreparationPhysics.inside(.5,.5,.3));assertFalse(PreparationPhysics.inside(.5+.3,.5+.3,.3));assertFalse(PreparationPhysics.inside(.5,.5+.3,.3));}

    @Test void wateringRoseHasNineDistinctHoles(){var pose=new Pose(.5,1,.5,-Math.PI/2,0);var jets=PreparationPhysics.streams(pose,true,true);assertEquals(9,jets.size());assertEquals(9,jets.stream().map(Stream::source).distinct().count());for(int row=0;row<3;row++)for(int col=0;col<3;col++)assertEquals(pose.point(PreparationPhysics.rose(row,col)),jets.get(row*3+col).source());}
    @Test void everyCenteredWaterJetReachesSoil(){assertEquals(1,PreparationPhysics.hitFraction(new Pose(.5,1,.5,-Math.PI/2,0),true,true,1),1e-9);}
    @Test void waterJetsMoveAndRotateWithRose(){var pose=new Pose(.6,1.2,.7,-1.9,.3);var jets=PreparationPhysics.streams(pose,true,true);for(var jet:jets){assertTrue(jet.flowing());assertTrue(Double.isFinite(jet.source().x()));}assertEquals(0,PreparationPhysics.hitFraction(pose,true,false,1));}
    @Test void aPartialHitDoesNotCountAsNineFullJets(){boolean partial=false;for(double x=.5;x<1.2;x+=.005){double f=PreparationPhysics.hitFraction(new Pose(x,1,.5,-Math.PI/2,0),true,true,1);if(f>0&&f<1)partial=true;}assertTrue(partial);}
    @Test void leftAndRightAreScreenRelativeAtEveryYaw(){var p=new Pose(.5,1,.5,Math.PI,0);for(double yaw:new double[]{0,.38,1.8,-2.4}){var right=PreparationPhysics.move(p,1,0,yaw,.1);var left=PreparationPhysics.move(p,-1,0,yaw,.1);assertTrue(Math.cos(yaw)*(right.x()-p.x())+Math.sin(yaw)*(right.z()-p.z())>0);assertTrue(Math.cos(yaw)*(left.x()-p.x())+Math.sin(yaw)*(left.z()-p.z())<0);assertEquals(p.roll(),right.roll());}}
    @Test void forwardAndBackMoveInDepthWithoutChangingHeight(){var p=new Pose(.5,1,.5,-1,0);for(double yaw:new double[]{0,.38,1.8}){var a=PreparationPhysics.move(p,0,1,yaw,.1);var b=PreparationPhysics.move(p,0,-1,yaw,.1);assertTrue(-Math.sin(yaw)*(a.x()-p.x())+Math.cos(yaw)*(a.z()-p.z())<0);assertEquals(p.y(),a.y());assertTrue(-Math.sin(yaw)*(b.x()-p.x())+Math.cos(yaw)*(b.z()-p.z())>0);assertEquals(p.y(),b.y());}}
    @Test void diagonalMovementAndBoundsDoNotAccelerateTools(){var p=new Pose(.5,1,.5,0,0);var a=PreparationPhysics.move(p,1,1,0,.1);assertEquals(.046,Math.hypot(a.x()-.5,a.z()-.5),1e-9);var edge=PreparationPhysics.move(new Pose(1.4,1,-.4,0,0),1,1,0,.1);assertTrue(PreparationPhysics.bounded(edge));}

    @Test void wasdRotatesWithoutTranslatingOrAutomaticMotion(){
        var p=new Pose(.7,1,.6,0,0);
        assertEquals(p,PreparationPhysics.tilt(p,0,0,.1));
        var a=PreparationPhysics.tilt(p,-1,0,.1);var d=PreparationPhysics.tilt(p,1,0,.1);
        var w=PreparationPhysics.tilt(p,0,1,.1);var back=PreparationPhysics.tilt(p,0,-1,.1);
        assertTrue(a.roll()>0);assertTrue(d.roll()<0);assertTrue(w.pitch()<0);assertTrue(back.pitch()>0);
        for(var rotated:List.of(a,d,w,back)){assertEquals(p.x(),rotated.x());assertEquals(p.y(),rotated.y());assertEquals(p.z(),rotated.z());assertTrue(PreparationPhysics.bounded(rotated));}
        assertEquals(p,PreparationPhysics.tilt(a,1,0,.1));
    }
    @Test void manualRotationBoundsAndDiagonalSpeedStayValid(){
        var p=new Pose(.5,1,.5,0,0);var diagonal=PreparationPhysics.tilt(p,1,1,.1);
        assertEquals(.19,Math.hypot(diagonal.roll(),diagonal.pitch()),1e-9);
        for(int i=0;i<1000;i++)p=PreparationPhysics.tilt(p,-1,-1,.12);
        assertEquals(Math.PI,p.roll());assertEquals(Math.PI,p.pitch());assertTrue(PreparationPhysics.bounded(p));
    }
    @Test void soilFlowsAcrossEntireOpeningAndFollowsBothRotations(){
        var p=new Pose(.5,1,.5,Math.PI,.2);var jets=PreparationPhysics.streams(p,false,true);
        assertEquals(15,jets.size());assertEquals(15,jets.stream().map(Stream::source).distinct().count());
        for(int row=0;row<5;row++)for(int column=0;column<3;column++){
            var jet=jets.get(row*3+column);assertEquals(p.point(PreparationPhysics.bagOpening(row,column)),jet.source());assertTrue(jet.flowing());
        }
        assertTrue(jets.stream().mapToDouble(s->s.source().x()).max().orElseThrow()-jets.stream().mapToDouble(s->s.source().x()).min().orElseThrow()>.3);
    }
    @Test void centeredWideSoilStreamHitsAtEveryFillHeightAndStopsOnRelease(){
        var p=new Pose(.5,1,.5,Math.PI,0);
        for(int i=0;i<=100;i++)assertEquals(1,PreparationPhysics.hitFraction(p,false,true,i/100.),1e-9,"fill "+i);
        assertEquals(0,PreparationPhysics.hitFraction(p,false,false,1));
        assertTrue(PreparationPhysics.streams(new Pose(.5,1,.5,0,0),false,true).stream().noneMatch(Stream::flowing));
    }
    @Test void wideSoilStreamSpillOnlyCountsThePartInsidePot(){
        boolean partial=false;
        for(double x=.5;x<1.2;x+=.005){double f=PreparationPhysics.hitFraction(new Pose(x,1,.5,Math.PI,0),false,true,1);if(f>0&&f<1)partial=true;assertTrue(f>=0&&f<=1);}
        assertTrue(partial);assertEquals(0,PreparationPhysics.hitFraction(new Pose(1.3,1,.5,Math.PI,0),false,true,1));
    }
    @Test void forwardAndBackwardCanTurnBagFarEnoughToPour(){
        for(double direction:new double[]{-1,1}){
            var p=new Pose(.5,1,.5,0,0);for(int i=0;i<20;i++)p=PreparationPhysics.tilt(p,0,direction,.1);
            assertTrue(PreparationPhysics.bounded(p));assertTrue(PreparationPhysics.stream(p,false,true).flowing());
            assertEquals(1,PreparationPhysics.hitFraction(p,false,true,1),1e-9);
        }
    }
}
