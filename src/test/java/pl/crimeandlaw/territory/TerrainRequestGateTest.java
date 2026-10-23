package pl.crimeandlaw.territory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrainRequestGateTest {
    @Test void aBurstOfCameraUpdatesKeepsOnlyOneDownloadAndAnOldReplyCannotReleaseANewerOne(){
        var gate=new TerrainRequestGate();int first=gate.start(0);assertTrue(first>0);
        for(int i=1;i<10000;i++)assertEquals(0,gate.start(i));
        assertFalse(gate.acknowledge(first+1));assertEquals(first,gate.awaiting());
        assertTrue(gate.acknowledge(first));assertEquals(0,gate.start(TerrainRequestGate.INTERVAL_NANOS-1));
        int second=gate.start(TerrainRequestGate.INTERVAL_NANOS);assertNotEquals(first,second);
        assertFalse(gate.acknowledge(first));assertEquals(second,gate.awaiting());assertTrue(gate.acknowledge(second));
    }
    @Test void aLostResponseTimesOutAndDisconnectAllowsAnImmediateFreshRequest(){
        var gate=new TerrainRequestGate();int first=gate.start(13);
        assertEquals(0,gate.start(13+TerrainRequestGate.TIMEOUT_NANOS-1));
        int retry=gate.start(13+TerrainRequestGate.TIMEOUT_NANOS);assertTrue(retry>first);
        assertFalse(gate.acknowledge(first));gate.clear();assertTrue(gate.ready(1));assertTrue(gate.start(1)>0);
    }
}
