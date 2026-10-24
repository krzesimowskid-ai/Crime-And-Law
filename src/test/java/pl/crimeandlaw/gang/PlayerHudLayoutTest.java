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
            assertTrue(layout.clockY()*scale>=0);assertTrue((layout.moneyX()+PlayerHudLayout.MONEY_WIDTH)*scale<nativeGui[0]);
            assertTrue((layout.moneyX()+PlayerHudLayout.MONEY_WIDTH)*scale<=nativeGui[0]/2.0-91,"Counters stay to the left of the native hotbar");
            assertTrue(layout.moneyX()==layout.mapX()||layout.moneyX()==layout.mapX()+PlayerHudLayout.MAP_WIDTH+10);
            assertEquals(6,layout.mapX());assertEquals(6,layout.canvas().canvasHeight()-layout.mapY()-PlayerHudLayout.MAP_HEIGHT);
        }
    }
    @Test void compactNormalAmountsCanStayBesideTheMapAtLargeGuiScale(){
        var compact=PlayerHudLayout.of(480,270,68);
        assertEquals(compact.mapY()+PlayerHudLayout.MAP_HEIGHT,compact.moneyY()+PlayerHudLayout.MONEY_HEIGHT);
        var maximum=PlayerHudLayout.of(480,270,PlayerHudLayout.MONEY_WIDTH);
        assertTrue(maximum.moneyY()<compact.moneyY());
        assertEquals(maximum.mapX(),maximum.moneyX());
        assertEquals(maximum.mapY()-4,maximum.moneyY()+PlayerHudLayout.MONEY_HEIGHT);
    }
    @Test void theUsersSmallWindowKeepsOrdinaryAmountsBesideTheMap(){
        for(int[] size:new int[][]{{428,241},{427,240},{480,270}}){
            var hud=PlayerHudLayout.of(size[0],size[1],72);
            assertEquals(hud.mapX()+PlayerHudLayout.MAP_WIDTH+10,hud.moneyX());
            assertEquals(hud.mapY()+PlayerHudLayout.MAP_HEIGHT,hud.moneyY()+PlayerHudLayout.MONEY_HEIGHT);
        }
    }
    @Test void narrowWindowsAndLongAmountsStayAttachedToTheMap(){
        for(int[] size:new int[][]{{320,240},{344,258},{400,225},{428,241},{480,270}})
            for(float width:new float[]{40,72,106}){
                var hud=PlayerHudLayout.of(size[0],size[1],width);
                assertTrue(hud.clockY()>0);
                if(hud.moneyX()==hud.mapX())assertEquals(hud.mapY()-4,hud.moneyY()+PlayerHudLayout.MONEY_HEIGHT);
                else assertEquals(hud.mapY()+PlayerHudLayout.MAP_HEIGHT,hud.moneyY()+PlayerHudLayout.MONEY_HEIGHT);
                assertTrue((hud.moneyX()+width)*hud.canvas().scale()<=size[0]/2.0-91);
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
