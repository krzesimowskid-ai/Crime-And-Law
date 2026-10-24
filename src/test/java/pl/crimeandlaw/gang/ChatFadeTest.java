package pl.crimeandlaw.gang;

import org.junit.jupiter.api.Test;
import pl.crimeandlaw.client.ChatFade;
import static org.junit.jupiter.api.Assertions.*;

class ChatFadeTest {
    @Test void idleMessagesStayVisibleAfterLongPeriods(){
        var fade=new ChatFade();
        for(long now:new long[]{0,1_000_000_000L,12_000_000_000L,3_600_000_000_000L})assertEquals(ChatFade.IDLE,fade.opacity(false,now),.0001);
        assertTrue(ChatFade.IDLE>.5f);
    }
    @Test void focusAndBlurFadeTheWholePanelWithoutAnOpacityJump(){
        var fade=new ChatFade();long t=1_000_000_000L;
        assertEquals(ChatFade.IDLE,fade.opacity(true,t),.0001);
        float halfway=fade.opacity(true,t+100_000_000L);assertTrue(halfway>ChatFade.IDLE&&halfway<1);
        assertEquals(1,fade.opacity(true,t+200_000_000L),.0001);
        assertEquals(1,fade.opacity(false,t+300_000_000L),.0001);
        float closing=fade.opacity(false,t+400_000_000L);assertEquals(halfway,closing,.0001);
        assertEquals(closing,fade.opacity(true,t+400_000_000L),.0001);
        assertEquals(1,fade.opacity(true,t+600_000_000L),.0001);
        fade.reset();assertEquals(ChatFade.IDLE,fade.opacity(false,t+5_000_000_000L),.0001);
    }
    @Test void fadingPreservesColorsAndRespectsConfiguredOpacity(){
        assertEquals(0x8099AAFF,ChatFade.tint(0xFF99AAFF,.5));
        assertEquals(0x0099AAFF,ChatFade.tint(0xFF99AAFF,-1));
        assertEquals(0xFF99AAFF,ChatFade.tint(0xFF99AAFF,2));
        assertEquals(0x3099AAFF,ChatFade.tint(0x5299AAFF,ChatFade.IDLE));
    }
}
