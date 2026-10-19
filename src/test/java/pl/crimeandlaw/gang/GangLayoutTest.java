package pl.crimeandlaw.gang;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.client.GangLayout;
class GangLayoutTest {
    @Test void sameProportionsAcrossMinecraftGuiScales(){
        for(int scale:new int[]{1,2,3,4,6}){
            var l=GangLayout.of(1920/scale,1080/scale);
            assertEquals(1536,l.panelWidth()*l.scale()*scale,3);
            assertEquals(864,l.panelHeight()*l.scale()*scale,3);
            assertEquals(21.6,9*l.scale()*scale,0.2);
        }
    }
    @Test void pointerCoordinatesFollowRenderedWidgets(){
        for(int[] size:new int[][]{{320,180},{480,270},{640,360},{960,540},{400,400}}){
            var l=GangLayout.of(size[0],size[1]);
            int left=(l.canvasWidth()-l.panelWidth())/2,top=(l.canvasHeight()-l.panelHeight())/2;
            double buttonX=left+20+60,buttonY=top+62+158+9;
            assertEquals(buttonX,l.local(buttonX*l.scale()),0.001);
            assertEquals(buttonY,l.local(buttonY*l.scale()),0.001);
            assertTrue(top+62+158+19<top+l.panelHeight()-24,"Create action visible without scrolling");
            assertTrue(l.panelWidth()*l.scale()<size[0]);
            assertTrue(l.panelHeight()*l.scale()<size[1]);
        }
    }
}
