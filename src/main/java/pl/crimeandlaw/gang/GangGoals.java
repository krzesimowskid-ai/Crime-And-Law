package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Weekly progress only: no claim endpoint and no currency or item rewards. */
public final class GangGoals {
    private static final int MAX_EVENTS=10000;
    private static final class Week {
        long start;
        Set<UUID> recruited=new HashSet<>(),captures=new HashSet<>(),wins=new HashSet<>();
        Week(long start){this.start=start;}
    }
    private transient boolean dirty;
    public boolean takeDirty(){boolean result=dirty;dirty=false;return result;}
    private Map<UUID,Week> gangs=new LinkedHashMap<>();
    private Week week(UUID gang,Instant now,ZoneId zone){
        long start=now.atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay();
        Week current=gangs.get(gang);if(current==null||start>current.start){current=new Week(start);gangs.put(gang,current);dirty=true;}return current;
    }
    public void recruit(UUID gang,UUID player,Instant now,ZoneId zone){var week=week(gang,now,zone);if(week.recruited.size()<MAX_EVENTS)dirty|=week.recruited.add(player);}
    public void capture(UUID event,UUID winner,Collection<UUID> participants,Instant now,ZoneId zone){
        if(winner==null)return;
        for(UUID gang:participants){var week=week(gang,now,zone);if(week.captures.size()<MAX_EVENTS)dirty|=week.captures.add(event);if(gang.equals(winner)&&week.wins.size()<MAX_EVENTS)dirty|=week.wins.add(event);}
    }
    public void forget(UUID gang){gangs.remove(gang);}
    public JsonObject snapshot(UUID gang,Instant now,ZoneId zone){
        var week=week(gang,now,zone);var result=new JsonObject();result.addProperty("now",now.toEpochMilli());result.addProperty("endsAt",LocalDate.ofEpochDay(week.start).plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli());
        var cards=new JsonArray();cards.add(card("recruits","Nowicjusze","Przyjmij nowych graczy do gangu. Każdy gracz liczy się raz w tygodniu.",week.recruited.size(),5));
        cards.add(card("captures","Wspólna akcja","Weź udział w zakończonych captach. Anulowane akcje nie zwiększają postępu.",week.captures.size(),5));
        cards.add(card("wins","Kontrola terenu","Wygraj capty i przejmij teren dla swojego gangu.",week.wins.size(),3));result.add("cards",cards);return result;
    }
    private static JsonObject card(String id,String name,String description,int current,int target){var card=new JsonObject();card.addProperty("id",id);card.addProperty("name",name);card.addProperty("description",description);card.addProperty("current",Math.min(current,target));card.addProperty("target",target);return card;}
    void validate(){
        if(gangs==null)throw new IllegalArgumentException("Uszkodzony zapis celów.");
        for(var entry:gangs.entrySet()){
            Week w=entry.getValue();if(entry.getKey()==null||w==null||w.recruited==null||w.captures==null||w.wins==null||w.recruited.size()>MAX_EVENTS||w.captures.size()>MAX_EVENTS||w.wins.size()>MAX_EVENTS||w.recruited.contains(null)||w.captures.contains(null)||w.wins.contains(null)||!w.captures.containsAll(w.wins))throw new IllegalArgumentException("Uszkodzony postęp celów.");
            try{LocalDate.ofEpochDay(w.start);}catch(DateTimeException e){throw new IllegalArgumentException("Uszkodzony tydzień celów.");}
        }
    }
}
