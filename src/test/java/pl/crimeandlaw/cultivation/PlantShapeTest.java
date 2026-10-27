package pl.crimeandlaw.cultivation;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlantShapeTest {
 @Test void identityKeepsTheSameShapeAcrossRestoresAndHarvestSessions(){var a=new PlanterState();a.fillSoil();a.plant(false);long seed=PlantShape.seed(a.identity());var b=new PlanterState();b.restore(a.data());assertEquals(seed,PlantShape.seed(b.identity()));assertEquals(PlantShape.create(seed),PlantShape.create(PlantShape.seed(b.identity())));}
 @Test void differentPlantsVaryAngleHeightLengthAndWidth(){var a=PlantShape.create(1);var b=PlantShape.create(2);assertNotEquals(a,b);for(int i=0;i<8;i++){assertNotEquals(a.get(i).angle(),b.get(i).angle());assertNotEquals(a.get(i).height(),b.get(i).height());assertNotEquals(a.get(i).length(),b.get(i).length());assertNotEquals(a.get(i).width(),b.get(i).width());}}
 @Test void allLeafSurfacesStaySeparatedForManyPlants(){for(int seed=0;seed<2000;seed++){var shape=PlantShape.create(seed);assertEquals(8,shape.stream().map(PlantShape.Leaf::node).distinct().count());for(int i=0;i<8;i++)for(int j=i+1;j<8;j++)assertSeparated(shape.get(i),shape.get(j),seed);}}
 @Test void firstThreeLeavesSpanThePlantAndLaterLeavesDoNotMoveThem(){var s=PlantShape.create(42);assertTrue(s.get(0).height()<.25);assertTrue(s.get(2).height()>.77);for(var l:s){assertTrue(l.length()>.29&&l.length()<.47);for(double t=0;t<=1;t+=.01)assertTrue(l.height()+l.lift(t)>=l.minY()&&l.height()+l.lift(t)<=l.maxY());}}
 @Test void nearbySeedNumbersStillProduceVisiblyDifferentOrientations(){var quadrants=new int[4];for(int i=1;i<=100;i++){double angle=PlantShape.create(i).get(0).angle();int q=Math.floorMod((int)Math.floor(angle/(Math.PI/2)),4);quadrants[q]++;}for(int n:quadrants)assertTrue(n>=12,"Seeds must not bunch into one phase quadrant");}
 @Test void evenThreeLeafPlantsMixUprightLevelAndLoweredWholeLeaves(){for(int seed=0;seed<2000;seed++){var leaves=PlantShape.create(seed).subList(0,3);assertTrue(leaves.stream().anyMatch(l->l.rise()/l.length()>.14),"Upright leaf at seed "+seed);assertTrue(leaves.stream().anyMatch(l->l.rise()/l.length()<-.14),"Lowered leaf at seed "+seed);assertTrue(leaves.stream().anyMatch(l->Math.abs(l.rise()/l.length())<.051),"Level leaf at seed "+seed);}}
 @Test void leavesStayAboveThePotRimAndBelowTheUvFitting(){for(int seed=0;seed<2000;seed++)for(var leaf:PlantShape.create(seed)){assertTrue(.39+.86*leaf.minY()>.485);assertTrue(.39+.86*leaf.maxY()<1.5);assertTrue(Math.abs(leaf.rise()/leaf.length())<.421);}}

 // Independent check: clip projected patches in XZ, then compare their affine
 // surface heights at every intersection vertex, including both solid skins.
 private record Point(double x,double z){}
 private static Point point(PlantShape.Leaf l,double t,double side){double c=Math.cos(l.angle()),s=Math.sin(l.angle());return new Point(c*l.length()*t-s*side*l.width()/2,s*l.length()*t+c*side*l.width()/2);}
 private static double y(PlantShape.Leaf l,Point p){double c=Math.cos(l.angle()),s=Math.sin(l.angle()),t=(p.x*c+p.z*s)/l.length(),side=2*(-p.x*s+p.z*c)/l.width();return l.height()+l.lift(Math.clamp(t,0,1))-.004*Math.abs(side);}
 private static double cross(Point a,Point b,Point p){return (b.x-a.x)*(p.z-a.z)-(b.z-a.z)*(p.x-a.x);}
 private static List<Point> clip(List<Point> polygon,List<Point> border){
  for(int i=0;i<border.size()&&!polygon.isEmpty();i++){
   var a=border.get(i);var b=border.get((i+1)%border.size());var out=new ArrayList<Point>();var previous=polygon.getLast();double before=cross(a,b,previous);
   for(var current:polygon){double after=cross(a,b,current);if((before>=0)!=(after>=0)){double t=before/(before-after);out.add(new Point(previous.x+(current.x-previous.x)*t,previous.z+(current.z-previous.z)*t));}if(after>=0)out.add(current);previous=current;before=after;}polygon=out;
  }return polygon;
 }
 private static List<List<Point>> patches(PlantShape.Leaf l){var out=new ArrayList<List<Point>>();double[] t={0,.42,.72,1};for(int i=0;i<3;i++)for(int side=-1;side<1;side++)out.add(List.of(point(l,t[i],side),point(l,t[i+1],side),point(l,t[i+1],side+1),point(l,t[i],side+1)));return out;}
 private static void assertSeparated(PlantShape.Leaf a,PlantShape.Leaf b,int seed){
  if(a.maxY()+.005<b.minY()||b.maxY()+.005<a.minY())return;
  for(var pa:patches(a))for(var pb:patches(b)){var intersection=clip(pa,pb);if(intersection.isEmpty())continue;double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;for(var p:intersection){double gap=y(a,p)-y(b,p);min=Math.min(min,gap);max=Math.max(max,gap);}assertTrue(min>.018||max<-.018,"Solid leaf patches intersect at seed "+seed);}
 }
}
