package pl.crimeandlaw.gang;

import org.junit.jupiter.api.Test;
import pl.crimeandlaw.client.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatLayoutTest {
    @Test void chatAndInputStayAboveTheRadarAndFitAcrossGuiSizes(){
        for(int[] size:new int[][]{{960,540},{640,360},{480,270},{800,450},{534,300},{400,225},{320,240},{344,258}}){
            var chat=ChatLayout.of(size[0],size[1],true,1,1,0);var hud=PlayerHudLayout.of(size[0],size[1]);
            assertTrue(chat.x()*chat.scale()>0);assertTrue((chat.x()+chat.width()+10)*chat.scale()<size[0]);
            assertTrue((chat.inputY()+chat.inputHeight()+20)*chat.scale()<hud.clockY()*hud.canvas().scale());
            double mx=(chat.x()+50)*chat.scale(),my=(chat.historyY()+5)*chat.scale();assertTrue(chat.inHistory(mx,my));
            assertEquals(chat.x()+50,chat.local(mx),.001);assertFalse(chat.inHistory(mx,chat.inputY()*chat.scale()));
        }
    }
    @Test void scrollClampsAndRetainsItsPlaceWhenTheViewportChanges(){
        var scroll=new ChatScroll();scroll.bounds(100,11);assertEquals(89,scroll.start());assertEquals(100,scroll.end());
        scroll.scroll(7);assertEquals(7,scroll.offset());assertEquals(93,scroll.end());
        scroll.scroll(Integer.MAX_VALUE);assertEquals(0,scroll.start());assertEquals(89,scroll.offset());
        scroll.scroll(Integer.MIN_VALUE);assertEquals(0,scroll.offset());
        scroll.scroll(20);scroll.bounds(100,7);assertEquals(20,scroll.offset());assertEquals(73,scroll.start());
        scroll.bounds(3,11);assertEquals(0,scroll.offset());assertEquals(0,scroll.start());assertEquals(3,scroll.end());
    }
}
