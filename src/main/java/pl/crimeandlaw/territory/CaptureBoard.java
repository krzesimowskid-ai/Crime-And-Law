package pl.crimeandlaw.territory;

import com.google.gson.Gson;
import java.time.Instant;
import java.util.*;

/** Deterministic capture state machine. The adapter supplies authoritative presence. */
public final class CaptureBoard {
    public static final int MAX_EVENTS=32,MAX_TEAMS=8,MAX_PLAYERS=64;
    private static final Gson JSON=new Gson();
    public enum Phase { CLAIMING, PREPARING, FIGHTING }
    public record Person(UUID gang,boolean present){}
    public record Result(TerrainKey key,UUID event,UUID previous,UUID winner,Set<UUID> gangs,String reason){}
    public static final class Fighter {
        public UUID player,gang;public boolean eliminated;
        Fighter(UUID player,UUID gang){this.player=player;this.gang=gang;}
    }
    public static final class Battle {
        public UUID id=UUID.randomUUID(),attacker,previousOwner,holding;
        public TerrainKey key;
        public Phase phase=Phase.CLAIMING;
        public long progress,phaseElapsed,holdingElapsed;
        public CaptureRules rules;
        public Map<UUID,Fighter> fighters=new LinkedHashMap<>();
        public Map<UUID,Long> absences=new LinkedHashMap<>();
        public Set<UUID> removedTeams=new LinkedHashSet<>();
        Battle(TerrainKey key,UUID gang,UUID owner,CaptureRules rules){this.key=key;attacker=gang;previousOwner=owner;this.rules=rules;absences.put(gang,0L);}
        public Set<UUID> teams(){return Set.copyOf(absences.keySet());}
        public long remainingSeconds(){return switch(phase){case CLAIMING->Math.max(0,(rules.captureSeconds()*1000L-progress+999)/1000);case PREPARING->Math.max(0,(rules.preparationSeconds()*1000L-phaseElapsed+999)/1000);case FIGHTING->Math.max(0,(rules.battleSeconds()*1000L-phaseElapsed+999)/1000);};}
    }
    private Map<String,Battle> active=new LinkedHashMap<>();
    private Map<UUID,Long> cooldowns=new LinkedHashMap<>();
    private Map<String,Long> protections=new LinkedHashMap<>();
    private Set<UUID> unlocked=new LinkedHashSet<>();
    private transient boolean recovered=true;
    public Collection<Battle> battles(){return Collections.unmodifiableCollection(active.values());}
    public Battle at(TerrainKey key){return active.get(key.storageKey());}
    public Battle forGang(UUID gang){return active.values().stream().filter(b->b.absences.containsKey(gang)).findFirst().orElse(null);}
    public boolean registered(UUID player){return active.values().stream().anyMatch(b->b.fighters.containsKey(player));}
    public long cooldown(UUID gang,long now){return Math.max(0,cooldowns.getOrDefault(gang,0L)-now);}
    public long protection(TerrainKey key,long now){return Math.max(0,protections.getOrDefault(key.storageKey(),0L)-now);}
    public boolean unlocked(UUID gang){return unlocked.contains(gang);}
    public boolean unlock(UUID gang){return unlocked.add(Objects.requireNonNull(gang));}
    public Battle start(TerrainKey key,UUID gang,UUID owner,UUID player,CaptureRules rules,long now){
        Objects.requireNonNull(key);Objects.requireNonNull(gang);Objects.requireNonNull(player);Objects.requireNonNull(rules);
        require(unlocked(gang),"Lider musi aktywować gałąź Kontrola terytoriów.");
        require(rules.open(Instant.ofEpochMilli(now)),"Ataki są teraz zamknięte. Sprawdź godziny serwera.");
        require(!gang.equals(owner),"Ten teren należy już do twojego gangu.");
        require(at(key)==null,"Na tym terenie trwa już wydarzenie.");
        require(forGang(gang)==null,"Twój gang uczestniczy już w capcie.");
        require(!registered(player),"Uczestniczysz już w innym capcie.");
        require(cooldown(gang,now)==0,"Twój gang czeka na zakończenie cooldownu.");
        require(protection(key,now)==0,"Teren jest chroniony po poprzednim przejęciu.");
        require(active.size()<MAX_EVENTS,"Osiągnięto limit równoczesnych captów. Spróbuj później.");
        var battle=new Battle(key,gang,owner,rules);battle.fighters.put(player,new Fighter(player,gang));
        // An occupied chunk has a real defending side. An absent owner never becomes an NPC.
        if(owner!=null){require(forGang(owner)==null,"Właściciel terenu uczestniczy już w innym capcie.");battle.absences.put(owner,0L);battle.phase=Phase.PREPARING;}
        active.put(key.storageKey(),battle);return battle;
    }
    public void join(TerrainKey key,UUID gang,UUID player,long now){
        var b=at(key);require(b!=null,"Nie ma aktywnego captu na tym terenie.");
        require(b.phase!=Phase.FIGHTING,"Zapisy do tej walki zostały zakończone.");
        require(!b.removedTeams.contains(gang),"Ten gang odpadł już z wydarzenia.");
        var previous=b.fighters.get(player);require(previous==null||!previous.eliminated,"Po eliminacji nie można wrócić do tego captu.");
        if(previous!=null){require(previous.gang.equals(gang),"Nie można zmienić strony w trakcie captu.");return;}
        require(!registered(player),"Uczestniczysz już w innym capcie.");
        require(b.fighters.size()<MAX_PLAYERS,"Lista uczestników captu jest pełna.");
        if(!b.absences.containsKey(gang)){
            require(unlocked(gang),"Lider musi aktywować gałąź Kontrola terytoriów.");
            require(cooldown(gang,now)==0,"Twój gang czeka na zakończenie cooldownu.");
            require(forGang(gang)==null,"Twój gang uczestniczy już w innym capcie.");
            require(b.absences.size()<MAX_TEAMS,"Osiągnięto limit gangów w jednym capcie.");b.absences.put(gang,0L);
        }
        b.fighters.put(player,new Fighter(player,gang));
        if(b.phase==Phase.CLAIMING&&!b.attacker.equals(gang)){b.phase=Phase.PREPARING;b.phaseElapsed=0;b.holding=null;b.holdingElapsed=0;}
    }
    public void eliminate(UUID player){for(var b:active.values()){var f=b.fighters.get(player);if(f!=null)f.eliminated=true;}}
    public List<Result> tick(long now,long elapsed,Map<UUID,Person> people,Set<UUID> existingGangs){
        elapsed=Math.clamp(elapsed,0,1000);List<Result> results=new ArrayList<>();
        var iterator=active.values().iterator();
        while(iterator.hasNext()){
            var b=iterator.next();
            Map<UUID,Integer> present=new HashMap<>();
            for(var f:b.fighters.values()){
                var person=people.get(f.player);
                if(person!=null&&!person.gang().equals(f.gang))f.eliminated=true;
                if(!f.eliminated&&person!=null&&person.present()&&f.gang.equals(person.gang())&&existingGangs.contains(f.gang))present.merge(f.gang,1,Integer::sum);
            }
            for(var gang:b.absences.keySet()){
                if(b.removedTeams.contains(gang))continue;
                int required=b.phase==Phase.CLAIMING?b.rules.minimumPlayers():1;
                if(b.phase==Phase.PREPARING||present.getOrDefault(gang,0)>=required)b.absences.put(gang,0L);
                else b.absences.put(gang,b.absences.get(gang)+elapsed);
                if(!existingGangs.contains(gang)||b.absences.get(gang)>=b.rules.absenceSeconds()*1000L){
                    b.removedTeams.add(gang);for(var f:b.fighters.values())if(f.gang.equals(gang))f.eliminated=true;
                }
            }
            Result result=null;
            if(b.phase==Phase.CLAIMING){
                if(b.removedTeams.contains(b.attacker))result=finish(b,null,now,"Przejęcie anulowane: brak obecnych uczestników.");
                else if(present.getOrDefault(b.attacker,0)>=b.rules.minimumPlayers()){
                    b.progress+=elapsed;
                    if(b.progress>=b.rules.captureSeconds()*1000L)result=finish(b,b.attacker,now,"Zakończono przejęcie terenu.");
                }
            }else if(b.phase==Phase.PREPARING){
                b.phaseElapsed+=elapsed;
                if(b.phaseElapsed>=b.rules.preparationSeconds()*1000L){b.phase=Phase.FIGHTING;b.phaseElapsed=0;}
            }else{
                b.phaseElapsed+=elapsed;
                List<UUID> alive=b.absences.keySet().stream().filter(g->!b.removedTeams.contains(g))
                    .filter(g->b.fighters.values().stream().anyMatch(f->f.gang.equals(g)&&!f.eliminated)).toList();
                if(alive.isEmpty())result=finish(b,null,now,"Capt zakończony remisem: brak uczestników.");
                else if(alive.size()==1&&present.getOrDefault(alive.getFirst(),0)>=1){
                    UUID gang=alive.getFirst();if(!gang.equals(b.holding)){b.holding=gang;b.holdingElapsed=0;}
                    b.holdingElapsed+=elapsed;
                    if(b.holdingElapsed>=b.rules.holdSeconds()*1000L)result=finish(b,gang,now,"Zwycięstwo w walce o teren.");
                }else {b.holding=null;b.holdingElapsed=0;}
                if(result==null&&b.phaseElapsed>=b.rules.battleSeconds()*1000L)result=finish(b,null,now,"Capt zakończony remisem: upłynął czas walki.");
            }
            if(result!=null){results.add(result);iterator.remove();}
        }
        cooldowns.values().removeIf(until->until<=now);protections.values().removeIf(until->until<=now);
        return results;
    }
    private Result finish(Battle b,UUID winner,long now,String reason){
        for(UUID gang:b.absences.keySet())cooldowns.put(gang,Math.max(cooldowns.getOrDefault(gang,0L),now+b.rules.cooldownSeconds()*1000L));
        if(winner!=null)protections.put(b.key.storageKey(),now+b.rules.protectionSeconds()*1000L);
        return new Result(b.key,b.id,b.previousOwner,winner,b.teams(),reason);
    }
    public String toJson(){return JSON.toJson(this);}
    public void cleanup(Set<UUID> existing){unlocked.retainAll(existing);cooldowns.keySet().removeIf(id->!existing.contains(id));}
    public static CaptureBoard fromJson(String value){
        CaptureBoard board=JSON.fromJson(value,CaptureBoard.class);
        require(board!=null&&board.active!=null&&board.cooldowns!=null&&board.protections!=null&&board.unlocked!=null,"Nieprawidłowy zapis captów.");
        require(board.active.size()<=MAX_EVENTS&&board.cooldowns.size()<=4096&&board.unlocked.size()<=4096,"Zapis captów przekracza limit.");
        require(board.cooldowns.entrySet().stream().allMatch(e->e.getKey()!=null&&e.getValue()!=null&&e.getValue()>=0)
            &&board.protections.entrySet().stream().allMatch(e->e.getKey()!=null&&e.getValue()!=null&&e.getValue()>=0),"Nieprawidłowe czasy captów.");
        require(board.unlocked.stream().allMatch(Objects::nonNull),"Nieprawidłowa gałąź kontroli terytoriów.");
        Set<UUID> teams=new HashSet<>(),players=new HashSet<>();
        for(var entry:board.active.entrySet()){
            var b=entry.getValue();
            require(b!=null&&b.id!=null&&b.phase!=null&&b.rules!=null&&b.key!=null&&b.attacker!=null&&b.absences!=null
                &&b.absences.size()>=1&&b.absences.size()<=MAX_TEAMS&&b.absences.containsKey(b.attacker)
                &&b.fighters!=null&&b.fighters.size()<=MAX_PLAYERS&&b.removedTeams!=null
                &&b.absences.keySet().containsAll(b.removedTeams)&&entry.getKey().equals(b.key.storageKey())
                &&b.progress>=0&&b.phaseElapsed>=0&&b.holdingElapsed>=0,"Uszkodzony capt.");
            for(var team:b.absences.entrySet())require(team.getKey()!=null&&team.getValue()!=null&&team.getValue()>=0&&teams.add(team.getKey()),"Gang w wielu captach.");
            for(var fighter:b.fighters.entrySet()){
                var f=fighter.getValue();require(f!=null&&f.player!=null&&f.gang!=null&&f.player.equals(fighter.getKey())
                    &&b.absences.containsKey(f.gang)&&players.add(f.player),"Nieprawidłowy uczestnik captu.");
            }
        }
        board.recovered=false;
        return board;
    }
    /** Restart cancels unfinished fights; never awards a chunk for offline time. */
    public boolean recover(long now){
        if(recovered)return false;recovered=true;if(active.isEmpty())return false;
        for(var b:active.values())finish(b,null,now,"Capt przerwany restartem serwera.");active.clear();return true;
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalArgumentException(message);}
}
