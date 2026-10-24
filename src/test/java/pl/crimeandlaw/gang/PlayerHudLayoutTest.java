package pl.crimeandlaw.gang;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.client.PlayerHudLayout;

class PlayerHudLayoutTest {
    @Test void radarAndMoneyFitAndDoNotCoverVanillaHealthOrHotbar(){
        for(int[] nativeGui:new int[][]{{960,540},{640,360},{480,270},{800,450},{534,300},{400,225},{320,240},{344,258}}){
            var layout=PlayerHudLayout.of(nativeGui[0],nativeGui[1]);double scale=layout.canvas().scale();
            assertTrue(layout.mapX()*scale>=0);assertTrue((layout.mapY()+95)*scale<nativeGui[1]);
            assertTrue(layout.moneyY()*scale>=0);assertTrue((layout.moneyX()+148)*scale<nativeGui[0]);
            if((layout.moneyX()+148)*scale>nativeGui[0]/2.0-111)assertTrue((layout.moneyY()+78)*scale<=nativeGui[1]-59.9);
            assertTrue(nativeGui[1]-40+layout.chatShift()<=Math.min(layout.moneyY(),layout.mapY())*scale-9);
        }
    }
}
