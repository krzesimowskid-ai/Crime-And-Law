package pl.crimeandlaw.cultivation;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlantShapeTest {
 @Test void identityKeepsTheSameShapeAcrossRestoresAndHarvestSessions(){var a=new PlanterState();a.fillSoil();a.plant(false);long seed=PlantShape.seed(a.identity());var b=new PlanterState();b.restore(a.data());assertEquals(seed,PlantShape.seed(b.identity()));assertEquals(PlantShape.create(seed),PlantShape.create(PlantShape.seed(b.identity())));}
 @Test void differentPlantsVaryAngleHeightLengthAndWidth(){var a=PlantShape.create(1);var b=PlantShape.create(2);assertNotEquals(a,b);for(int i=0;i<8;i++){assertNotEquals(a.get(i).angle(),b.get(i).angle());assertNotEquals(a.get(i).height(),b.get(i).height());assertNotEquals(a.get(i).length(),b.get(i).length());assertNotEquals(a.get(i).width(),b.get(i).width());}}
 @Test void allLeafSurfacesStaySeparatedForManyPlants(){for(int seed=0;seed<2000;seed++){var shape=PlantShape.create(seed);assertEquals(8,shape.stream().map(PlantShape.Leaf::node).distinct().count());for(int i=0;i<8;i++)for(int j=i+1;j<8;j++)assertSeparated(shape.get(i),shape.get(j),seed);}}
 @Test void firstThreeLeavesSpanThePlantAndLaterLeavesDoNotMoveThem(){var s=PlantShape.create(42);assertTrue(s.get(0).height()<.25);assertTrue(s.get(2).height()>.84);assertTrue(s.get(0).length()>s.get(2).length());assertTrue(s.get(0).width()>s.get(2).width());}
 @Test void nearbySeedNumbersStillProduceVisiblyDifferentOrientations(){var quadrants=new int[4];for(int i=1;i<=100;i++){double angle=PlantShape.create(i).get(0).angle();int q=Math.floorMod((int)Math.floor(angle/(Math.PI/2)),4);quadrants[q]++;}for(int n:quadrants)assertTrue(n>=12,"Seeds must not bunch into one phase quadrant");}
 @Test void lowerBladesSagWhileYoungUpperBladesRise(){for(int seed=0;seed<2000;seed++){var s=PlantShape.create(seed);assertTrue((s.get(0).rise()-s.get(0).droop())/s.get(0).bladeLength()<-.03);assertTrue(Math.abs((s.get(1).rise()-s.get(1).droop())/s.get(1).bladeLength())<.09);assertTrue((s.get(2).rise()-s.get(2).droop())/s.get(2).bladeLength()>.05);}}
 @Test void leavesStayAboveThePotRimAndBelowTheUvFitting(){for(int seed=0;seed<2000;seed++)for(var leaf:PlantShape.create(seed)){assertTrue(.39+.86*leaf.minY()>.485);assertTrue(.52+.86*leaf.maxY()<1.52);}}
 @Test void lowerNodesGrowInOppositePairs(){for(int seed=0;seed<2000;seed++){var s=PlantShape.create(seed);var byNode=new HashMap<Integer,PlantShape.Leaf>();for(var l:s)byNode.put(l.node(),l);for(int n:new int[]{0,2}){var a=byNode.get(n);var b=byNode.get(n+1);double diff=Math.abs(Math.IEEEremainder(a.angle()-b.angle(),Math.PI*2));assertTrue(Math.abs(diff-Math.PI)<.37);assertTrue(Math.abs(a.height()-b.height())<.006);}}}
 @Test void curvedBladeHasARealStalkAndStaysInsideItsConservativeEnvelope(){for(int seed=0;seed<200;seed++)for(var l:PlantShape.create(seed)){assertTrue(l.petiole()>=.07&&l.petiole()<.096);assertTrue(Math.hypot(l.point(0,0).x(),l.point(0,0).z())>.069);for(int i=0;i<=64;i++)for(int j=0;j<=8;j++){var p=l.point(i/64.,j/4.-1);assertTrue(p.y()-PlantShape.HALF_THICKNESS>=l.minY()-1e-10);assertTrue(p.y()+PlantShape.HALF_THICKNESS<=l.maxY()+1e-10);}}}

 // Independent validation: intersect horizontal footprints, then compare the
 // affine upper/lower prism planes at all vertices of the shared polygon.
 private record Point(double x,double z){}
 private static Point point(PlantShape.Leaf l,double t,double side){var p=l.point(t,side);return new Point(p.x(),p.z());}
 private static double plane(PlantShape.Leaf l,Point p,boolean upper){double c=Math.cos(l.angle()),s=Math.sin(l.angle()),t=(p.x*c+p.z*s-l.petiole())/l.bladeLength(),side=2*(-p.x*s+p.z*c)/(l.width()*Math.cos(l.roll()));return l.height()+l.stalkRise()+(l.rise()-l.droop())*t+side*l.width()/2*Math.sin(l.roll())+(upper?l.hump()+PlantShape.HALF_THICKNESS:-l.cup()-PlantShape.HALF_THICKNESS);}
 private static double cross(Point a,Point b,Point p){return (b.x-a.x)*(p.z-a.z)-(b.z-a.z)*(p.x-a.x);}
 private static List<Point> clip(List<Point> polygon,List<Point> border){
  for(int i=0;i<border.size()&&!polygon.isEmpty();i++){
   var a=border.get(i);var b=border.get((i+1)%border.size());var out=new ArrayList<Point>();var previous=polygon.getLast();double before=cross(a,b,previous);
   for(var current:polygon){double after=cross(a,b,current);if((before>=0)!=(after>=0)){double t=before/(before-after);out.add(new Point(previous.x+(current.x-previous.x)*t,previous.z+(current.z-previous.z)*t));}if(after>=0)out.add(current);previous=current;before=after;}polygon=out;
  }return polygon;
 }
 private static List<Point> footprint(PlantShape.Leaf l){return List.of(point(l,0,-1),point(l,1,-1),point(l,1,1),point(l,0,1));}
 private static void assertSeparated(PlantShape.Leaf a,PlantShape.Leaf b,int seed){
  var intersection=clip(footprint(a),footprint(b));if(intersection.isEmpty())return;double ab=Double.NEGATIVE_INFINITY,ba=Double.NEGATIVE_INFINITY;for(var p:intersection){ab=Math.max(ab,plane(a,p,true)-plane(b,p,false));ba=Math.max(ba,plane(b,p,true)-plane(a,p,false));}assertTrue(ab<-.005||ba<-.005,"Solid blade envelopes intersect at seed "+seed);
 }
}
