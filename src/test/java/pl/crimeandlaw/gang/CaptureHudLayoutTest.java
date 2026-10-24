package pl.crimeandlaw.gang;

import org.junit.jupiter.api.Test;
import pl.crimeandlaw.client.*;
import static org.junit.jupiter.api.Assertions.*;

class CaptureHudLayoutTest {
    @Test void captureAndChatNeverOverlapAcrossWindowSizesAndChatSettings(){
        for(int[] size:new int[][]{{428,241},{427,240},{320,240},{344,258},{480,270},{640,360},{960,540},{1920,1080}})
            for(boolean focused:new boolean[]{false,true})
                for(double width:new double[]{0,.5,1})
                    for(double scale:new double[]{.1,.5,1}){
                        var chat=ChatLayout.of(size[0],size[1],focused,width,scale,1);
                        var capture=CaptureHudLayout.of(size[0],chat);
                        double chatRight=(chat.x()+chat.width()+10)*chat.scale();
                        assertTrue(capture.x()>=chatRight+5.99,"Capture must clear the chat background and scrollbar");
                        assertTrue(capture.x()+capture.width()<=size[0]-10,"Capture stays inside the window");
                        assertTrue(capture.width()>=180,"The capture title needs a readable panel");
                        assertEquals(12,capture.y());assertEquals(40,capture.height());
                    }
    }
    @Test void theReportedSmallWindowShrinksThePanelToFitBesideChat(){
        var chat=ChatLayout.of(428,241,false,1,1,0);
        var panel=CaptureHudLayout.of(428,chat);
        assertTrue(panel.width()<280);assertTrue(panel.x()>(428-280)/2);
        var focused=ChatLayout.of(428,241,true,1,1,0);
        assertEquals(panel,CaptureHudLayout.of(428,focused),"Opening chat must not move the panel");
    }
    @Test void aCenteredPanelStaysCenteredWhenThereIsRoomOrChatIsHidden(){
        var chat=ChatLayout.of(1920,540,false,1,1,0);
        var panel=CaptureHudLayout.of(1920,chat);
        assertEquals((1920-280)/2,panel.x());assertEquals(280,panel.width());
        assertEquals(new CaptureHudLayout(74,12,280,40),CaptureHudLayout.of(428,null));
    }
}
