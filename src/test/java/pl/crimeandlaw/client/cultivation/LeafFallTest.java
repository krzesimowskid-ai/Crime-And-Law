package pl.crimeandlaw.client.cultivation;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import pl.crimeandlaw.cultivation.LeafMotion;
import static org.junit.jupiter.api.Assertions.*;

class LeafFallTest {
    @Test void theCutBeginsAtTheSamePositionAndKeepsTheLeafRigid(){
        var leaf=AureliaMesh.leaf(7,8);var motion=LeafMotion.cut(7,42);
        var a=leaf.point(.1f,-.5f);var b=leaf.point(.9f,.5f);
        var first=LeafFall.transform(leaf,motion,0);var later=LeafFall.transform(leaf,motion,1.4);
        var aa=new Vector3f((float)a.x,(float)a.y,(float)a.z);var bb=new Vector3f((float)b.x,(float)b.y,(float)b.z);
        assertTrue(first.transformPosition(new Vector3f(aa)).distance(aa)<1e-6);
        assertEquals(aa.distance(bb),later.transformPosition(new Vector3f(aa)).distance(later.transformPosition(new Vector3f(bb))),1e-6);
    }
    @Test void leavesHaveAShallowProfileInsteadOfSharpFolds(){
        var leaf=AureliaMesh.leaf(0,8);
        double lastSlope=0;
        for(int i=1;i<=100;i++){
            var a=leaf.point((i-1)/100f,0);var b=leaf.point(i/100f,0);
            double slope=(b.y-a.y)/Math.hypot(b.x-a.x,b.z-a.z);
            assertTrue(Math.abs(slope)<.14);if(i>1)assertTrue(Math.abs(slope-lastSlope)<.20);
            assertTrue(Math.abs(b.y-leaf.base().y)<.02);lastSlope=slope;
        }
    }
    @Test void fallingContinuesPastTheOldFloorUntilTheWholeLeafLeavesTheView(){
        var camera=new Matrix4f().translate(300,430,0).scale(363,-363,363).rotateX(.23f).rotateY(.55f).translate(-.5f,0,-.5f);
        for(int id=0;id<8;id++){
            var leaf=AureliaMesh.leaf(id,8);var motion=LeafMotion.cut(id,42);
            assertFalse(LeafFall.below(camera,LeafFall.transform(leaf,motion,0),leaf,540));
            assertTrue(motion.y(1.8)<motion.y(1.4));
            assertTrue(LeafFall.below(camera,LeafFall.transform(leaf,motion,4),leaf,540));
        }
    }
}
