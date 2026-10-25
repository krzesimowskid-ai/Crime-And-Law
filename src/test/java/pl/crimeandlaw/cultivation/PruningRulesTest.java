package pl.crimeandlaw.cultivation;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruningRulesTest {
    @Test void aLeafCanOnlyBeRemovedOnce(){int mask=PruningRules.remove(63,2);assertEquals(5,PruningRules.count(mask));assertFalse(PruningRules.available(mask,2));assertThrows(IllegalArgumentException.class,()->PruningRules.remove(mask,2));}
    @Test void malformedIdsCannotWrapBitShifts(){for(int id:new int[]{-1,-32,Integer.MIN_VALUE,6,31,32,64,Integer.MAX_VALUE})assertFalse(PruningRules.available(63,id));}
    @Test void serverTimingHasExplicitBoundaries(){for(long t:new long[]{Long.MIN_VALUE,-1,0,11,30,Long.MAX_VALUE})assertFalse(PruningRules.precise(t));assertTrue(PruningRules.precise(12));assertTrue(PruningRules.precise(20));assertTrue(PruningRules.precise(29));}
    @Test void everyOneOfThe720LeafOrdersCanBeHarvestedExactlyOnce(){
        assertEquals(720,orders(PruningRules.FULL_MASK));
    }
    private int orders(int mask){if(mask==0)return 1;int orders=0;for(int id=0;id<6;id++)if(PruningRules.available(mask,id))orders+=orders(PruningRules.remove(mask,id));return orders;}
    @Test void tenThousandMixedRequestsNeverYieldMoreThanSixLeaves(){
        for(int seed=0;seed<100;seed++){var random=new Random(seed);int mask=63,awarded=0;
            for(int i=0;i<100;i++){int id=random.nextInt(12)-3;if(PruningRules.available(mask,id)){mask=PruningRules.remove(mask,id);awarded++;}}
            assertEquals(6,awarded);assertEquals(0,mask);
        }
    }
    @Test void partialMasksRetainExactRemainingLeaves(){for(int mask=0;mask<64;mask++){int count=0;for(int id=0;id<6;id++)if(PruningRules.available(mask,id))count++;assertEquals(count,PruningRules.count(mask));}}
}
