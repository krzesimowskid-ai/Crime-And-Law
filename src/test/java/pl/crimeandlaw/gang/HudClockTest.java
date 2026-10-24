package pl.crimeandlaw.gang;

import java.time.*;
import org.junit.jupiter.api.Test;
import pl.crimeandlaw.client.HudClock;
import pl.crimeandlaw.client.MinimapStyle;
import static org.junit.jupiter.api.Assertions.*;

class HudClockTest {
    @Test void dateAndDayFollowTheServerTimezoneAcrossMidnight(){
        var clock=new HudClock();long start=123_000_000L;
        clock.receiveAt(Instant.parse("2026-10-24T21:59:59Z").toEpochMilli(),"Europe/Warsaw",start);
        assertEquals(new HudClock.Display("23:59","24.10.2026","Sobota"),clock.displayAt(start));
        assertEquals(new HudClock.Display("00:00","25.10.2026","Niedziela"),clock.displayAt(start+2_000_000_000L));
    }
    @Test void autumnClockChangeAndResynchronizationUseServerData(){
        var clock=new HudClock();clock.receiveAt(Instant.parse("2026-10-25T00:59:59Z").toEpochMilli(),"Europe/Warsaw",0);
        assertEquals("02:59",clock.displayAt(0).time());assertEquals("02:00",clock.displayAt(2_000_000_000L).time());
        clock.receiveAt(Instant.parse("2026-10-25T01:00:01Z").toEpochMilli(),"UTC",3_000_000_000L);
        assertEquals("01:00",clock.displayAt(3_000_000_000L).time());
        assertThrows(DateTimeException.class,()->clock.receiveAt(0,"invalid-zone",0));
        clock.clear();assertFalse(clock.ready());assertEquals(new HudClock.Display("—","—","—"),clock.displayAt(Long.MAX_VALUE));
    }
    @Test void transparencyPreservesTheOriginalTerrainColors(){
        for(int color:new int[]{0xFF2040D0,0xFF318F25,0xFFB8D8E8,0xFF202020,0xFF7F1BCB}){
            int translucent=MinimapStyle.terrain(color);
            assertEquals(color&0xFFFFFF,translucent&0xFFFFFF);assertEquals(176,translucent>>>24);
        }
    }
}
