package pl.crimeandlaw.territory;

import java.time.*;

/** Game rules owned by the server, never values supplied by an attacking client. */
public record CaptureRules(String zone,boolean realTime,int opens,int closes,int captureSeconds,
        int preparationSeconds,int holdSeconds,int battleSeconds,int absenceSeconds,
        int cooldownSeconds,int protectionSeconds,int minimumPlayers) {
    public static CaptureRules defaults(){return new CaptureRules(ZoneId.systemDefault().getId(),true,16,22,300,60,30,900,30,3600,86400,1);}
    public CaptureRules {
        ZoneId.of(zone);
        if(opens<0||opens>23||closes<0||closes>24||captureSeconds<1||captureSeconds>86400
            ||preparationSeconds<1||preparationSeconds>3600||holdSeconds<1||holdSeconds>3600
            ||battleSeconds<holdSeconds||battleSeconds>86400||absenceSeconds<1||absenceSeconds>3600
            ||cooldownSeconds<0||cooldownSeconds>604800||protectionSeconds<0||protectionSeconds>2592000
            ||minimumPlayers<1||minimumPlayers>CaptureBoard.MAX_PLAYERS)
            throw new IllegalArgumentException("Nieprawidłowe zasady captów.");
    }
    public boolean open(Instant now){
        int hour=now.atZone(ZoneId.of(zone)).getHour();
        if(opens==closes)return true;
        return opens<closes?hour>=opens&&hour<closes:hour>=opens||hour<closes;
    }
    public CaptureRules zone(String value){return new CaptureRules(value,true,opens,closes,captureSeconds,preparationSeconds,holdSeconds,battleSeconds,absenceSeconds,cooldownSeconds,protectionSeconds,minimumPlayers);}
    public CaptureRules realTime(boolean value){return new CaptureRules(zone,value,opens,closes,captureSeconds,preparationSeconds,holdSeconds,battleSeconds,absenceSeconds,cooldownSeconds,protectionSeconds,minimumPlayers);}
    public CaptureRules change(String key,int value){
        if(!java.util.Set.of("otwarcie","zamkniecie","przejecie","przygotowanie","utrzymanie","walka","nieobecnosc","cooldown","ochrona","uczestnicy").contains(key))
            throw new IllegalArgumentException("Nieznany parametr captów.");
        return new CaptureRules(zone,realTime,key.equals("otwarcie")?value:opens,key.equals("zamkniecie")?value:closes,
            key.equals("przejecie")?value:captureSeconds,key.equals("przygotowanie")?value:preparationSeconds,
            key.equals("utrzymanie")?value:holdSeconds,key.equals("walka")?value:battleSeconds,
            key.equals("nieobecnosc")?value:absenceSeconds,key.equals("cooldown")?value:cooldownSeconds,
            key.equals("ochrona")?value:protectionSeconds,key.equals("uczestnicy")?value:minimumPlayers);
    }
    /** Minecraft's dawn is tick zero; only the sun clock is slowed, never game ticks. */
    public long minecraftTime(Instant now){
        var local=now.atZone(ZoneId.of(zone));
        return local.toLocalDate().toEpochDay()*24000+18000+local.toLocalTime().toNanoOfDay()*24000/86_400_000_000_000L;
    }
}
