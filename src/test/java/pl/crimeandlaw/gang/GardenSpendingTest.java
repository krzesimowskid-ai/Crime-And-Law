package pl.crimeandlaw.gang;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GardenSpendingTest {
    @Test void purchaseDebitsOnlySelectedPlayerAndAccount(){var f=new GangFinances();var a=UUID.randomUUID();var b=UUID.randomUUID();f.credit(a,GangFinances.Account.CASH,100);f.credit(a,GangFinances.Account.BANK,900);f.credit(b,GangFinances.Account.CASH,500);f.spend(a,GangFinances.Account.CASH,54);assertEquals(46,f.cash(a));assertEquals(900,f.bank(a));assertEquals(500,f.cash(b));f.spend(a,GangFinances.Account.BANK,100);assertEquals(800,f.bank(a));assertEquals(46,f.cash(a));}
    @Test void invalidAndUnaffordablePurchasesLeaveAllFundsUnchanged(){var f=new GangFinances();var a=UUID.randomUUID();f.credit(a,GangFinances.Account.CASH,100);for(long price:new long[]{-1,0,101,Long.MAX_VALUE}){assertThrows(IllegalArgumentException.class,()->f.spend(a,GangFinances.Account.CASH,price));assertEquals(100,f.cash(a));}assertThrows(NullPointerException.class,()->f.spend(a,null,1));assertEquals(100,f.cash(a));}
    @Test void repeatedSpendingCannotUnderflowOrConsumeBank(){var f=new GangFinances();var a=UUID.randomUUID();f.credit(a,GangFinances.Account.CASH,120);f.credit(a,GangFinances.Account.BANK,GangFinances.MAX_BALANCE);f.spend(a,GangFinances.Account.CASH,60);f.spend(a,GangFinances.Account.CASH,60);assertThrows(IllegalArgumentException.class,()->f.spend(a,GangFinances.Account.CASH,60));assertEquals(0,f.cash(a));assertEquals(GangFinances.MAX_BALANCE,f.bank(a));}
}
