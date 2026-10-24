package pl.crimeandlaw.gang;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.client.PlayerHudLayout;
import pl.crimeandlaw.client.MinimapStyle;

class PlayerHudLayoutTest {
    @Test void radarAndMoneyFitAndDoNotCoverVanillaHealthOrHotbar(){
        for(int[] nativeGui:new int[][]{{960,540},{640,360},{480,270},{800,450},{534,300},{400,225},{320,240},{344,258}}){
            var layout=PlayerHudLayout.of(nativeGui[0],nativeGui[1]);double scale=layout.canvas().scale();
            assertTrue(layout.mapX()*scale>=0);assertTrue((layout.mapY()+PlayerHudLayout.MAP_HEIGHT+2)*scale<nativeGui[1]);
            assertTrue(layout.moneyY()*scale>=0);assertTrue((layout.moneyX()+PlayerHudLayout.MONEY_WIDTH)*scale<nativeGui[0]);
            if((layout.moneyX()+PlayerHudLayout.MONEY_WIDTH)*scale>nativeGui[0]/2.0-111)assertTrue((layout.moneyY()+PlayerHudLayout.MONEY_HEIGHT)*scale<=nativeGui[1]-59.9);
            assertTrue(nativeGui[1]-40+layout.chatShift()<=Math.min(layout.moneyY(),layout.mapY())*scale-9);
        }
    }
    @Test void northFollowsTheEdgeInsteadOfCrossingTheMap(){
        int x=14,y=200,w=136,h=86;float px=x+w/2f,py=y+h*.58f;
        for(int angle=-720;angle<=720;angle++){
            var n=MinimapStyle.north(x,y,w,h,px,py,angle);
            assertTrue(n.x()>=x+7.999&&n.x()<=x+w-7.999);assertTrue(n.y()>=y+7.999&&n.y()<=y+h-7.999);
            assertTrue(Math.min(Math.min(Math.abs(n.x()-x-8),Math.abs(n.x()-x-w+8)),Math.min(Math.abs(n.y()-y-8),Math.abs(n.y()-y-h+8)))<.001);
        }
        assertEquals(y+8,MinimapStyle.north(x,y,w,h,px,py,0).y(),.001);
        assertEquals(x+w-8,MinimapStyle.north(x,y,w,h,px,py,90).x(),.001);
        assertEquals(y+h-8,MinimapStyle.north(x,y,w,h,px,py,180).y(),.001);
        assertEquals(x+8,MinimapStyle.north(x,y,w,h,px,py,270).x(),.001);
    }
}
