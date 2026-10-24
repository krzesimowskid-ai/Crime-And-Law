package pl.crimeandlaw.client;

import java.time.*;
import java.time.format.*;
import java.util.Locale;

/** Server wall time and timezone; interpolation uses a monotonic clock, never the client's date. */
public final class HudClock {
    public static final HudClock INSTANCE=new HudClock();
    private static final Locale PL=Locale.forLanguageTag("pl-PL");
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm",PL),DATE=DateTimeFormatter.ofPattern("dd.MM.uuuu",PL);
    private static final Display LOADING=new Display("—","—","—");
    private long epochMillis,anchorNanos,cachedMinute=Long.MIN_VALUE;
    private ZoneId zone;
    private Display cached=LOADING;
    public record Display(String time,String date,String day){}
    public void receive(long millis,String zone){receiveAt(millis,zone,System.nanoTime());}
    public void receiveAt(long millis,String zoneId,long nanos){
        ZoneId nextZone=ZoneId.of(zoneId);
        if(millis<0||millis>253_402_300_799_999L)throw new IllegalArgumentException("Nieprawidłowy czas serwera.");
        epochMillis=millis;anchorNanos=nanos;zone=nextZone;cachedMinute=Long.MIN_VALUE;
    }
    public Display display(){return displayAt(System.nanoTime());}
    public Display displayAt(long nanos){
        if(zone==null)return LOADING;
        long millis=epochMillis+Math.max(0,nanos-anchorNanos)/1_000_000L,minute=millis/60_000;
        if(minute!=cachedMinute){
            var date=Instant.ofEpochMilli(millis).atZone(zone);String day=date.getDayOfWeek().getDisplayName(TextStyle.FULL,PL);
            cached=new Display(TIME.format(date),DATE.format(date),day.substring(0,1).toUpperCase(PL)+day.substring(1));cachedMinute=minute;
        }
        return cached;
    }
    public boolean ready(){return zone!=null;}
    public String zoneId(){return zone==null?"":zone.getId();}
    public void clear(){zone=null;cached=LOADING;cachedMinute=Long.MIN_VALUE;epochMillis=anchorNanos=0;}
}
