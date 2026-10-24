package pl.crimeandlaw.gang;

import java.util.*;
import pl.crimeandlaw.gang.GangDirectory.*;

/** Whole in-game dollars. All validation precedes mutation on the server thread. */
public final class GangFinances {
    public static final long MAX_BALANCE=1_000_000_000_000L;
    public static final int MAX_RECIPIENTS=100,MAX_COMMENT=128;
    public enum Kind {
        DEPOSIT("Wpłata"),WITHDRAWAL("Wypłata"),BONUS("Premia");
        public final String label;Kind(String label){this.label=label;}
    }
    public record Entry(long timestamp,Kind kind,UUID actor,String actorName,String actorRole,
                        UUID recipient,String recipientName,String recipientRole,long amount,String comment){}
    private Map<UUID,Long> wallets=new LinkedHashMap<>();
    private Map<UUID,Long> balances=new LinkedHashMap<>();
    private Map<UUID,List<Entry>> history=new LinkedHashMap<>();

    public long wallet(UUID player){return wallets.getOrDefault(player,0L);}
    public long balance(UUID gang){return balances.getOrDefault(gang,0L);}
    public List<Entry> history(UUID gang){return Collections.unmodifiableList(history.getOrDefault(gang,List.of()));}
    public static String format(long amount){return "$ "+String.format(Locale.ROOT,"%,d",amount).replace(',',' ');}
    public static long parseInput(String text){
        if(text==null)throw new IllegalArgumentException("Podaj kwotę.");
        String digits=text.replace(" ","").replace("\u00a0","");
        require(digits.matches("[0-9]{1,13}"),"Podaj dodatnią, całkowitą kwotę w dolarach.");
        try{long amount=Long.parseLong(digits);amount(amount);return amount;}
        catch(NumberFormatException error){throw new IllegalArgumentException("Nieprawidłowa kwota.");}
    }
    public void credit(UUID player,long amount){
        Objects.requireNonNull(player);amount(amount);capacity(wallet(player),amount);wallets.put(player,wallet(player)+amount);
    }
    public void deposit(GangDirectory directory,UUID actor,long amount,String comment){
        Gang gang=authorized(directory,actor,Permission.DEPOSIT_FUNDS);amount(amount);comment=comment(comment);
        require(wallet(actor)>=amount,"Nie masz wystarczających środków na koncie.");capacity(balance(gang.id),amount);
        wallets.put(actor,wallet(actor)-amount);balances.put(gang.id,balance(gang.id)+amount);
        record(gang,Kind.DEPOSIT,actor,actor,amount,comment);
    }
    public void withdraw(GangDirectory directory,UUID actor,long amount,String comment){
        Gang gang=authorized(directory,actor,Permission.WITHDRAW_FUNDS);amount(amount);comment=comment(comment);
        require(balance(gang.id)>=amount,"Gang nie ma wystarczających środków.");capacity(wallet(actor),amount);
        balances.put(gang.id,balance(gang.id)-amount);wallets.put(actor,wallet(actor)+amount);
        record(gang,Kind.WITHDRAWAL,actor,actor,amount,comment);
    }
    public void pay(GangDirectory directory,UUID actor,Map<UUID,Long> payments,String comment){
        Gang gang=authorized(directory,actor,Permission.PAY_BONUSES);comment=comment(comment);
        require(payments!=null&&!payments.isEmpty()&&payments.size()<=MAX_RECIPIENTS,"Wybierz od 1 do 100 odbiorców.");
        long total=0;
        for(var payment:payments.entrySet()){
            UUID recipient=payment.getKey();Long amount=payment.getValue();
            require(recipient!=null&&gang.members.containsKey(recipient),"Każdy odbiorca musi należeć do Twojego gangu.");
            require(!actor.equals(recipient),"Nie możesz wypłacić premii samemu sobie.");
            require(amount!=null,"Podaj kwotę premii.");amount(amount);capacity(wallet(recipient),amount);
            capacity(total,amount);total+=amount;
        }
        require(balance(gang.id)>=total,"Gang nie ma środków na całą wypłatę premii.");
        balances.put(gang.id,balance(gang.id)-total);
        for(var payment:payments.entrySet()){
            wallets.put(payment.getKey(),wallet(payment.getKey())+payment.getValue());
            record(gang,Kind.BONUS,actor,payment.getKey(),payment.getValue(),comment);
        }
    }
    public void forgetEmptyGang(UUID gang){require(balance(gang)==0,"Najpierw wypłać pozostałe środki gangu.");balances.remove(gang);history.remove(gang);}
    private void record(Gang gang,Kind kind,UUID actor,UUID recipient,long amount,String comment){
        var sender=gang.members.get(actor);var target=gang.members.get(recipient);
        var entries=history.computeIfAbsent(gang.id,id->new ArrayList<>());
        entries.add(new Entry(System.currentTimeMillis(),kind,actor,sender.name,gang.roles.get(sender.role).name,
            recipient,target.name,gang.roles.get(target.role).name,amount,comment));
        if(entries.size()>GangDirectory.HISTORY_LIMIT)entries.subList(0,entries.size()-GangDirectory.HISTORY_LIMIT).clear();
    }
    private static Gang authorized(GangDirectory directory,UUID actor,Permission permission){
        Gang gang=directory.of(actor);require(GangDirectory.allowed(gang,actor,permission),"Nie masz uprawnień do tej czynności.");return gang;
    }
    private static String comment(String value){
        require(value!=null,"Nieprawidłowy komentarz.");value=value.strip();
        require(value.length()<=MAX_COMMENT&&value.codePoints().noneMatch(c->Character.isISOControl(c)||c==0x00a7),"Komentarz może mieć do 128 znaków, bez znaków sterujących.");return value;
    }
    public static void amount(long amount){require(amount>0&&amount<=MAX_BALANCE,"Kwota musi wynosić od 1 do "+format(MAX_BALANCE)+".");}
    private static void capacity(long current,long added){require(current>=0&&current<=MAX_BALANCE&&added<=MAX_BALANCE-current,"Przekroczono maksymalne saldo konta.");}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
    void validate(){
        require(wallets!=null&&balances!=null&&history!=null,"Uszkodzony zapis finansów.");
        for(var map:List.of(wallets,balances))for(var e:map.entrySet())
            require(e.getKey()!=null&&e.getValue()!=null&&e.getValue()>=0&&e.getValue()<=MAX_BALANCE,"Uszkodzone saldo konta.");
        for(var ledger:history.entrySet()){
            require(ledger.getKey()!=null&&ledger.getValue()!=null&&ledger.getValue().size()<=GangDirectory.HISTORY_LIMIT,"Uszkodzona historia finansowa.");
            for(var e:ledger.getValue()){
                require(e!=null&&e.timestamp()>=0&&e.kind()!=null&&e.actor()!=null&&e.recipient()!=null,"Uszkodzona transakcja.");
                require(e.actorName()!=null&&e.actorName().length()<=32&&e.recipientName()!=null&&e.recipientName().length()<=32
                    &&e.actorRole()!=null&&e.actorRole().length()<=24&&e.recipientRole()!=null&&e.recipientRole().length()<=24,"Uszkodzone dane transakcji.");
                amount(e.amount());comment(e.comment());
            }
        }
    }
}
