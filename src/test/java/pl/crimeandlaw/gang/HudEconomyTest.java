package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.crimeandlaw.gang.GangDirectory.Permission;
import pl.crimeandlaw.gang.GangFinances.Account;

class HudEconomyTest {
    private final UUID owner=new UUID(30,1),member=new UUID(30,2),other=new UUID(30,3);
    private GangDirectory directory(){var d=new GangDirectory();var g=d.create(owner,"Owner","Audit Gang","");d.request(member,"Member",g.id);d.accept(owner,member);return d;}
    @Test void oldPersonalMoneyMigratesToBankOnlyAndNeverToCash(){
        var d=directory();d.finances.credit(owner,12345);var json=JsonParser.parseString(d.toJson()).getAsJsonObject();json.addProperty("schemaVersion",3);json.getAsJsonObject("finances").remove("cash");json.remove("goals");json.remove("profiles");json.getAsJsonObject("gangs").getAsJsonObject(d.of(owner).id.toString()).remove("blacklist");
        var restored=GangDirectory.fromJson(json.toString());assertTrue(restored.needsMigration());assertEquals(12345,restored.finances.bank(owner));assertEquals(0,restored.finances.cash(owner));
        restored=GangDirectory.fromJson(restored.toJson());assertEquals(12345,restored.finances.bank(owner));assertEquals(0,restored.finances.cash(owner));assertFalse(restored.needsMigration());
    }
    @Test void personalAccountsAreIndependentAndGangTransactionsOnlyUseBank(){
        var d=directory();d.finances.credit(owner,Account.CASH,999);d.finances.credit(owner,Account.BANK,1000);d.finances.deposit(d,owner,500,"");d.finances.pay(d,owner,Map.of(member,100L),"");d.finances.withdraw(d,owner,100,"");
        assertEquals(999,d.finances.cash(owner));assertEquals(600,d.finances.bank(owner));assertEquals(0,d.finances.cash(member));assertEquals(100,d.finances.bank(member));assertEquals(300,d.finances.balance(d.of(owner).id));
        var restored=GangDirectory.fromJson(d.toJson());assertEquals(999,restored.finances.cash(owner));assertEquals(600,restored.finances.bank(owner));
    }
    @Test void fractionalAndOversizeSavedBalancesAreRejectedRatherThanTruncated(){
        for(String value:List.of("0.5","-1","9223372036854775808","\"100\"","null")){
            var d=directory();d.finances.credit(owner,1);var json=JsonParser.parseString(d.toJson()).getAsJsonObject();json.getAsJsonObject("finances").getAsJsonObject("wallets").add(owner.toString(),JsonParser.parseString(value));
            assertThrows(IllegalArgumentException.class,()->GangDirectory.fromJson(json.toString()),value);
        }
    }
    @Test void accountOverflowRejectsTheWholeGrantAndDoesNotTouchTheOtherAccount(){
        var d=directory();d.finances.credit(owner,Account.CASH,GangFinances.MAX_BALANCE);d.finances.credit(owner,Account.BANK,GangFinances.MAX_BALANCE);String before=d.toJson();
        for(Account account:Account.values())assertThrows(IllegalArgumentException.class,()->d.finances.credit(owner,account,1));assertEquals(before,d.toJson());
    }
    @Test void tenThousandRandomTransfersCannotCreateMoney(){
        var random=new Random(300026);var d=new GangDirectory();var people=new ArrayList<UUID>();
        for(int g=0;g<4;g++){
            UUID lead=new UUID(30,100+g*10);var gang=d.create(lead,"Leader"+g,"Gang "+g,"");people.add(lead);d.finances.credit(lead,10000);d.finances.credit(lead,Account.CASH,1000);d.finances.deposit(d,lead,5000,"");
            d.role(lead,"member","Member",Set.of(Permission.DEPOSIT_FUNDS,Permission.WITHDRAW_FUNDS,Permission.PAY_BONUSES));
            for(int i=1;i<=4;i++){UUID id=new UUID(30,100+g*10+i);d.request(id,"Player"+g+"_"+i,gang.id);d.accept(lead,id);people.add(id);d.finances.credit(id,10000);d.finances.credit(id,Account.CASH,1000);}
        }
        long expected=220000;
        for(int i=0;i<10000;i++){
            UUID actor=people.get(random.nextInt(people.size()));long amount=random.nextInt(2200)-200;
            try{switch(random.nextInt(3)){case 0->d.finances.deposit(d,actor,amount,"");case 1->d.finances.withdraw(d,actor,amount,"");default->d.finances.pay(d,actor,Map.of(people.get(random.nextInt(people.size())),amount),"");}}
            catch(IllegalArgumentException expectedError){}
            long total=0;for(UUID id:people){assertTrue(d.finances.bank(id)>=0);total+=d.finances.bank(id)+d.finances.cash(id);}for(var g:d.gangs()){assertTrue(d.finances.balance(g.id)>=0);total+=d.finances.balance(g.id);}assertEquals(expected,total,"operation "+i);
            if(i%1000==0)d=GangDirectory.fromJson(d.toJson());
        }
    }
    @Test void blacklistRemovesMembersBlocksPendingRequestsAndSurvivesRenames(){
        var d=directory();var gang=d.of(owner);d.finances.credit(member,100);d.blacklistAdd(owner,member,"Powód");assertNull(d.of(member));assertTrue(gang.blacklist.contains(member));assertEquals(100,d.finances.bank(member));
        d.renameSeenPlayer(member,"NewName");assertThrows(IllegalArgumentException.class,()->d.request(member,"NewName",gang.id));
        var restored=GangDirectory.fromJson(d.toJson());assertTrue(restored.of(owner).blacklist.contains(member));assertEquals(member,restored.resolvePlayer("NewName"));
        d.blacklistRemove(owner,member);d.request(member,"NewName",gang.id);d.accept(owner,member);assertEquals(2,gang.blacklist.history().size());
    }
    @Test void blacklistPermissionsCannotBeUsedToBypassMemberRemovalOrBanOwner(){
        var d=directory();var gang=d.of(owner);d.request(other,"Other",gang.id);d.accept(owner,other);d.role(owner,"member","Member",Set.of(Permission.MANAGE_BLACKLIST));String before=d.toJson();
        assertThrows(IllegalArgumentException.class,()->d.blacklistAdd(member,other,""));assertThrows(IllegalArgumentException.class,()->d.blacklistAdd(member,owner,""));assertThrows(IllegalArgumentException.class,()->d.blacklistAdd(owner,owner,""));assertEquals(before,d.toJson());
    }
    @Test void addingBlacklistCancelsAnOutstandingApplication(){
        var d=directory();var gang=d.of(owner);d.request(other,"Other",gang.id);d.blacklistAdd(owner,other,"");assertFalse(gang.requests.containsKey(other));assertThrows(IllegalArgumentException.class,()->d.accept(owner,other));assertThrows(IllegalArgumentException.class,()->d.request(other,"Other",gang.id));
    }
    @Test void weeklyGoalsDeduplicateEventsResetAtMondayAndNeverChangeMoney(){
        var d=directory();UUID gang=d.of(owner).id;var zone=ZoneId.of("Europe/Warsaw");var now=Instant.parse("2026-10-24T12:00:00Z");UUID event=new UUID(30,1000);String finances=new Gson().toJson(d.finances);
        for(int i=0;i<5;i++){d.goals.recruit(gang,member,now,zone);d.goals.capture(event,gang,List.of(gang),now,zone);}
        var cards=d.goals.snapshot(gang,now,zone).getAsJsonArray("cards");for(var card:cards)assertEquals(1,card.getAsJsonObject().get("current").getAsInt());assertEquals(finances,new Gson().toJson(d.finances));
        var restored=GangDirectory.fromJson(d.toJson());assertEquals(cards,restored.goals.snapshot(gang,now,zone).getAsJsonArray("cards"));
        var next=Instant.parse("2026-10-26T00:00:00Z");for(var card:restored.goals.snapshot(gang,next,zone).getAsJsonArray("cards"))assertEquals(0,card.getAsJsonObject().get("current").getAsInt());
        restored.goals.recruit(gang,other,next,zone);assertEquals(1,restored.goals.snapshot(gang,now,zone).getAsJsonArray("cards").get(0).getAsJsonObject().get("current").getAsInt(),"Clock rollback cannot repeatedly reset completed goals");
    }
}
