package pl.crimeandlaw.cultivation;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlantShapeTest {
 @Test void identityKeepsTheSameShapeAcrossRestoresAndHarvestSessions(){var a=new PlanterState();a.fillSoil();a.plant(false);long seed=PlantShape.seed(a.identity());var b=new PlanterState();b.restore(a.data());assertEquals(seed,PlantShape.seed(b.identity()));assertEquals(PlantShape.create(seed),PlantShape.create(PlantShape.seed(b.identity())));}
 @Test void differentPlantsVaryAngleHeightLengthAndWidth(){var a=PlantShape.create(1);var b=PlantShape.create(2);assertNotEquals(a,b);for(int i=0;i<8;i++){assertNotEquals(a.get(i).angle(),b.get(i).angle());assertNotEquals(a.get(i).height(),b.get(i).height());assertNotEquals(a.get(i).length(),b.get(i).length());assertNotEquals(a.get(i).width(),b.get(i).width());}}
 @Test void allLeafSurfacesStaySeparatedForManyPlants(){for(int seed=0;seed<2000;seed++){var shape=PlantShape.create(seed);assertEquals(8,shape.stream().map(PlantShape.Leaf::node).distinct().count());for(int i=0;i<8;i++)for(int j=i+1;j<8;j++){var a=shape.get(i);var b=shape.get(j);assertTrue(a.maxY()+.01<b.minY()||b.maxY()+.01<a.minY(),"Leaf envelopes intersect at seed "+seed);}}}
 @Test void firstThreeLeavesSpanThePlantAndLaterLeavesDoNotMoveThem(){var s=PlantShape.create(42);assertTrue(s.get(0).height()<.25);assertTrue(s.get(2).height()>.77);for(var l:s){assertTrue(l.length()>.29&&l.length()<.47);for(double t=0;t<=1;t+=.01)assertTrue(l.height()+l.lift(t)>=l.minY()&&l.height()+l.lift(t)<=l.maxY());}}
 @Test void nearbySeedNumbersStillProduceVisiblyDifferentOrientations(){var quadrants=new int[4];for(int i=1;i<=100;i++){double angle=PlantShape.create(i).get(0).angle();int q=Math.floorMod((int)Math.floor(angle/(Math.PI/2)),4);quadrants[q]++;}for(int n:quadrants)assertTrue(n>=12,"Seeds must not bunch into one phase quadrant");}
}
