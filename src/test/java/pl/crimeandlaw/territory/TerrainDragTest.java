package pl.crimeandlaw.territory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrainDragTest {
    @Test void repeatedGesturesResumeFromTheFractionalWorldPosition(){
        for(int radius=2;radius<=5;radius++){
            double size=196.0/(radius*2+1);var drag=new TerrainDrag();
            drag.begin(-2.37,5.23,80,90,radius,196);drag.move(80+.41*size,90-.36*size);drag.finish();
            assertEquals(-2.78,drag.x(),1e-9);assertEquals(5.59,drag.z(),1e-9);
            drag.begin(drag.x(),drag.z(),80,90,radius,196);drag.move(80-.19*size,90+.43*size);drag.finish();
            assertEquals(-2.59,drag.x(),1e-9);assertEquals(5.16,drag.z(),1e-9);
            assertNotEquals(Math.rint(drag.x()),drag.x());assertFalse(drag.active());
        }
    }
    @Test void repeatedReversalsKeepTheWorldPointUnderTheHeldMouseAtEveryZoom(){
        for(int radius=2;radius<=5;radius++){
            var drag=new TerrainDrag();var origin=new TerrainViewport(-1,-2);double pixels=196.0/(radius*2+1);
            drag.begin(origin,80,90,radius,196);
            for(double[] delta:new double[][]{{63,7},{-81,54},{10,-92},{111,40},{-22,-16},{0,0}}){
                drag.move(80+delta[0],90+delta[1]);var camera=drag.viewport();
                assertEquals(origin.x(),camera.x()-drag.offsetX(camera.x())/pixels+delta[0]/pixels,1e-9);
                assertEquals(origin.z(),camera.z()-drag.offsetY(camera.z())/pixels+delta[1]/pixels,1e-9);
                assertTrue(Math.abs(drag.offsetX(camera.x()))<=pixels/2+1e-8);
                assertTrue(Math.abs(drag.offsetY(camera.z()))<=pixels/2+1e-8);
            }
            assertEquals(origin,drag.viewport());assertTrue(drag.moved());drag.finish();assertFalse(drag.active());
        }
    }
    @Test void aClickStaysAClickButDraggingBackDoesNotSelectAChunk(){
        var drag=new TerrainDrag();drag.begin(new TerrainViewport(3,-7),100,100,5,196);
        drag.move(102,101);assertFalse(drag.moved());assertEquals(new TerrainViewport(3,-7),drag.viewport());
        drag.move(130,100);drag.move(100,100);assertTrue(drag.moved());assertEquals(new TerrainViewport(3,-7),drag.viewport());
    }
    @Test void draggingAtWorldCornersClampsTheCameraWithoutLeavingHugePreviewOffsets(){
        int limit=TerrainViewport.LIMIT;
        for(int x:new int[]{-limit,limit})for(int z:new int[]{-limit,limit}){
            var drag=new TerrainDrag();drag.begin(new TerrainViewport(x,z),0,0,5,196);
            drag.move(-x*1e5,-z*1e5);assertEquals(new TerrainViewport(x,z),drag.viewport());
            assertEquals(0,drag.offsetX(x));assertEquals(0,drag.offsetY(z));
        }
    }
}
