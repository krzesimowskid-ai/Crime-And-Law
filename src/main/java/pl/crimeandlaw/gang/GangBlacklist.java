package pl.crimeandlaw.gang;

import java.util.*;

/** Per-gang exclusion uses immutable player UUIDs, independently of display names. */
public final class GangBlacklist {
    public record Entry(UUID player,String name,UUID actor,String actorName,String reason,long timestamp){}
    public record Change(Entry entry,boolean added,UUID actor,String actorName,long timestamp){}
    private Map<UUID,Entry> entries=new LinkedHashMap<>();
    private List<Change> history=new ArrayList<>();
    public boolean contains(UUID player){return entries.containsKey(player);}
    public Collection<Entry> entries(){return Collections.unmodifiableCollection(entries.values());}
    public List<Change> history(){return Collections.unmodifiableList(history);}
    void add(UUID player,String name,UUID actor,String actorName,String reason){
        require(!contains(player),"Ten gracz jest już na czarnej liście.");
        require(entries.size()<10000,"Czarna lista ma już 10 000 wpisów.");
        checkText(name,32);checkText(actorName,32);reason=reason(reason);
        var entry=new Entry(player,name,actor,actorName,reason,System.currentTimeMillis());entries.put(player,entry);
        record(new Change(entry,true,actor,actorName,entry.timestamp()));
    }
    void remove(UUID player,UUID actor,String actorName){
        require(contains(player),"Nie ma takiego wpisu na czarnej liście.");checkText(actorName,32);
        var entry=entries.remove(player);record(new Change(entry,false,actor,actorName,System.currentTimeMillis()));
    }
    private void record(Change event){history.add(event);if(history.size()>10000)history.subList(0,history.size()-10000).clear();}
    public static String reason(String text){require(text!=null,"Nieprawidłowy powód.");text=text.strip();checkText(text,128);return text;}
    private static void checkText(String value,int max){require(value!=null&&value.length()<=max&&value.codePoints().noneMatch(c->Character.isISOControl(c)||c==0x00a7),"Nieprawidłowy tekst lub zbyt długi powód.");}
    void validate(){
        require(entries!=null&&entries.size()<=10000&&history!=null&&history.size()<=10000,"Uszkodzona czarna lista.");
        for(var e:entries.entrySet()){validate(e.getValue());require(e.getKey().equals(e.getValue().player()),"Uszkodzony identyfikator czarnej listy.");}
        for(var change:history){require(change!=null&&change.actor()!=null&&change.timestamp()>=0,"Uszkodzona historia czarnej listy.");checkText(change.actorName(),32);validate(change.entry());}
    }
    private static void validate(Entry e){require(e!=null&&e.player()!=null&&e.actor()!=null&&e.timestamp()>=0,"Uszkodzony wpis czarnej listy.");checkText(e.name(),32);checkText(e.actorName(),32);reason(e.reason());}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
}
