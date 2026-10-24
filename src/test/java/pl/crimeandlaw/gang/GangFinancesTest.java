package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.gang.GangDirectory.Permission;
import pl.crimeandlaw.network.GangNetwork;

class GangFinancesTest {
    private final UUID owner=new UUID(29,1),member=new UUID(29,2),friend=new UUID(29,3),outsider=new UUID(29,4);
    private GangDirectory directory(){
        var d=new GangDirectory();var gang=d.create(owner,"Owner","Finanse testowe","");
        d.request(member,"Member",gang.id);d.accept(owner,member);d.request(friend,"Friend",gang.id);d.accept(owner,friend);return d;
    }
    private GangDirectory funded(){var d=directory();d.finances.credit(owner,10000);d.finances.deposit(d,owner,8000,"Kapitał");return d;}
    @Test void transfersConserveMoneyAndHaveSeparateImmutableHistory(){
        var d=funded();var gang=d.of(owner);int events=gang.history.size();
        d.finances.withdraw(d,owner,1000,"Zwrot");d.finances.pay(d,owner,Map.of(member,2500L,friend,500L),"Za capt");
        assertEquals(3000,d.finances.wallet(owner));assertEquals(2500,d.finances.wallet(member));assertEquals(500,d.finances.wallet(friend));assertEquals(4000,d.finances.balance(gang.id));
        assertEquals(events,gang.history.size());assertEquals(4,d.finances.history(gang.id).size());
        assertThrows(UnsupportedOperationException.class,()->d.finances.history(gang.id).clear());
        assertEquals(d.toJson(),GangDirectory.fromJson(d.toJson()).toJson());
    }
    @Test void eachFinancialPermissionIsEnforcedIndependently(){
        var d=funded();String before=d.toJson();
        assertThrows(IllegalArgumentException.class,()->d.finances.deposit(d,member,1,""));
        assertThrows(IllegalArgumentException.class,()->d.finances.withdraw(d,member,1,""));
        assertThrows(IllegalArgumentException.class,()->d.finances.pay(d,member,Map.of(friend,1L),""));assertEquals(before,d.toJson());
        d.role(owner,"member","Członek",Set.of(Permission.DEPOSIT_FUNDS));d.finances.credit(member,10);d.finances.deposit(d,member,10,"");
        assertThrows(IllegalArgumentException.class,()->d.finances.withdraw(d,member,1,""));
        d.role(owner,"member","Członek",Set.of(Permission.PAY_BONUSES));d.finances.pay(d,member,Map.of(owner,1L),"");
        assertFalse(GangDirectory.allowed(d.of(member),member,Permission.WITHDRAW_FUNDS));
    }
    @Test void invalidAmountsNeverMutateAccounts(){
        var d=funded();String before=d.toJson();
        for(long amount:new long[]{0,-1,Long.MIN_VALUE,GangFinances.MAX_BALANCE+1,Long.MAX_VALUE}){
            assertThrows(IllegalArgumentException.class,()->d.finances.deposit(d,owner,amount,""));
            assertThrows(IllegalArgumentException.class,()->d.finances.withdraw(d,owner,amount,""));
            assertThrows(IllegalArgumentException.class,()->d.finances.pay(d,owner,Map.of(member,amount),""));
        }
        assertThrows(IllegalArgumentException.class,()->d.finances.deposit(d,owner,2001,""));
        assertThrows(IllegalArgumentException.class,()->d.finances.withdraw(d,owner,8001,""));assertEquals(before,d.toJson());
    }
    @Test void aBadLastRecipientRejectsTheEntireBatch(){
        var d=funded();var payments=new LinkedHashMap<UUID,Long>();payments.put(member,100L);payments.put(outsider,100L);String before=d.toJson();
        assertThrows(IllegalArgumentException.class,()->d.finances.pay(d,owner,payments,""));assertEquals(before,d.toJson());
        payments.remove(outsider);payments.put(friend,8000L);assertThrows(IllegalArgumentException.class,()->d.finances.pay(d,owner,payments,""));assertEquals(before,d.toJson());
        payments.put(friend,0L);assertThrows(IllegalArgumentException.class,()->d.finances.pay(d,owner,payments,""));assertEquals(before,d.toJson());
        assertThrows(IllegalArgumentException.class,()->d.finances.pay(d,owner,Map.of(owner,1L),""));assertEquals(before,d.toJson());
    }
    @Test void walletOverflowAndBatchTotalOverflowAreRejectedBeforeDebit(){
        var d=funded();d.finances.credit(friend,GangFinances.MAX_BALANCE);String before=d.toJson();
        assertThrows(IllegalArgumentException.class,()->d.finances.pay(d,owner,Map.of(member,100L,friend,1L),""));
        assertThrows(IllegalArgumentException.class,()->d.finances.credit(friend,1));assertEquals(before,d.toJson());
        var clean=funded();String baseline=clean.toJson();
        assertThrows(IllegalArgumentException.class,()->clean.finances.pay(clean,owner,Map.of(member,GangFinances.MAX_BALANCE,friend,1L),""));assertEquals(baseline,clean.toJson());
    }
    @Test void recipientsMustStillBeMembersWhenThePayoutExecutes(){
        var d=funded();d.kick(owner,member);d.create(member,"Member","Drugi gang","");String before=d.toJson();
        assertThrows(IllegalArgumentException.class,()->d.finances.pay(d,owner,Map.of(member,1L),""));assertEquals(before,d.toJson());
    }
    @Test void personalMoneySurvivesLeavingAndGangDeletionRequiresEmptyBalance(){
        var d=funded();d.finances.pay(d,owner,Map.of(member,100L),"");d.leave(member);assertEquals(100,d.finances.wallet(member));
        d.leave(friend);String before=d.toJson();assertThrows(IllegalArgumentException.class,()->d.leave(owner));assertEquals(before,d.toJson());
        d.finances.withdraw(d,owner,7900,"");d.leave(owner);var restored=GangDirectory.fromJson(d.toJson());
        assertNull(restored.of(owner));assertEquals(9900,restored.finances.wallet(owner));assertEquals(100,restored.finances.wallet(member));
    }
    @Test void filteringPaginatesAllMatchesOnTheServerAndKeepsWalletsPrivate(){
        var d=funded();for(int i=0;i<205;i++)d.finances.pay(d,owner,Map.of(member,1L),i%2==0?"Capt":"Drop");
        var page=FinanceView.snapshot(d,owner,new FinanceView.Query("bonuses",1,"capt","comment","Member",0));
        assertEquals(103,page.get("count").getAsInt());assertEquals(2,page.get("pages").getAsInt());assertEquals(3,page.getAsJsonArray("entries").size());
        assertEquals(1,page.getAsJsonArray("candidates").size());assertFalse(page.toString().contains("wallet"));
        assertThrows(IllegalArgumentException.class,()->FinanceView.snapshot(d,outsider,FinanceView.Query.DEFAULT));
        var withoutRights=FinanceView.snapshot(d,member,FinanceView.Query.DEFAULT);assertFalse(withoutRights.has("candidates"));
    }
    @Test void historyKeepsTheLastTenThousandWithoutChangingBalances(){
        var d=funded();for(int i=0;i<10005;i++){d.finances.withdraw(d,owner,1,"");d.finances.deposit(d,owner,1,"");}
        assertEquals(10000,d.finances.history(d.of(owner).id).size());assertEquals(8000,d.finances.balance(d.of(owner).id));
        assertEquals(2000,d.finances.wallet(owner));GangDirectory.fromJson(d.toJson());
    }
    @Test void oldOutfitRightsAreRemovedAndTheRestOfTheSaveIsPreserved(){
        var d=directory();var json=JsonParser.parseString(d.toJson()).getAsJsonObject();json.addProperty("schemaVersion",2);json.remove("finances");
        var rights=json.getAsJsonObject("gangs").getAsJsonObject(d.of(owner).id.toString()).getAsJsonObject("roles").getAsJsonObject("member").getAsJsonArray("permissions");
        rights.add("EDIT_OUTFITS");rights.add("CREATE_OUTFITS");rights.add("INVITE");
        var loaded=GangDirectory.fromJson(json.toString());assertTrue(loaded.needsMigration());assertEquals(3,loaded.of(owner).members.size());
        assertTrue(GangDirectory.allowed(loaded.of(member),member,Permission.INVITE));assertFalse(loaded.toJson().contains("OUTFITS"));assertEquals(0,loaded.finances.wallet(owner));
        rights.add("UNKNOWN_PERMISSION");assertThrows(IllegalArgumentException.class,()->GangDirectory.fromJson(json.toString()));
    }
    @Test void corruptFinancialSavesAreRejected(){
        var d=funded();var json=JsonParser.parseString(d.toJson()).getAsJsonObject();json.getAsJsonObject("finances").getAsJsonObject("wallets").addProperty(owner.toString(),-1);
        String negative=json.toString();assertThrows(IllegalArgumentException.class,()->GangDirectory.fromJson(negative));
        json=JsonParser.parseString(d.toJson()).getAsJsonObject();json.add("finances",JsonNull.INSTANCE);String text=json.toString();
        assertThrows(IllegalArgumentException.class,()->GangDirectory.fromJson(text));
    }
    @Test void networkAmountsCannotBeStringsFractionsOrOverflow(){
        for(String amount:List.of("\"100\"","null","true","{}","[]","0","-1","0.5","1000000000001","9223372036854775808")){
            var object=JsonParser.parseString("{\"amount\":"+amount+"}").getAsJsonObject();assertThrows(IllegalArgumentException.class,()->GangNetwork.exactAmount(object,"amount"),amount);
        }
        assertEquals(123,GangNetwork.exactAmount(JsonParser.parseString("{\"amount\":123}").getAsJsonObject(),"amount"));
    }
    @Test void commentsCannotInjectNewLinesOrFormatting(){
        var d=funded();String before=d.toJson();
        for(String comment:List.of("x".repeat(129),"abc\ndef","\u00a7cFake"))assertThrows(IllegalArgumentException.class,()->d.finances.withdraw(d,owner,1,comment));
        assertEquals(before,d.toJson());
        assertEquals(1200,GangFinances.parseInput("1 200"));assertEquals("$ 1 200",GangFinances.format(1200));
        for(String input:List.of("-1","0","1.5","1e3","$100"))assertThrows(IllegalArgumentException.class,()->GangFinances.parseInput(input));
    }
}
