package pl.crimeandlaw.territory;

import com.google.gson.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.crimeandlaw.gang.*;
import pl.crimeandlaw.gang.GangDirectory.*;
import pl.crimeandlaw.network.GangNetwork;

@EventBusSubscriber(modid="crimeandlaw")
public final class CaptureController {
    private record Watch(TerrainKey key,TerrainViewport viewport,long tick){}
    private static final Map<ServerPlayer,Watch> watchers=new WeakHashMap<>();
    private static final Map<ServerPlayer,Boolean> audience=new WeakHashMap<>();
    private static final Map<MinecraftServer,Long> lastTick=new WeakHashMap<>();
    private static final Gson JSON=new Gson();
    private CaptureController(){}
    public static void watch(ServerPlayer player,TerrainKey key,TerrainViewport viewport){watchers.put(player,new Watch(key,viewport,player.server.getTickCount()));send(player,key,"");}
    public static void marker(ServerPlayer player,TerrainKey key,boolean shared,Integer color){
        var data=CaptureSavedData.get(player.server);var gang=GangSavedData.get(player.server).directory.of(player.getUUID());
        require(gang!=null,"Najpierw dołącz do gangu.");
        if(color==null)data.markers.remove(key,player.getUUID(),gang.id,shared);
        else data.markers.set(key,player.getUUID(),gang.id,shared,color);
        data.setDirty();send(player,key,color==null?"Usunięto oznaczenie.":"Zapisano oznaczenie.");
    }
    public static void action(ServerPlayer player,String action,TerrainKey key){
        var saved=CaptureSavedData.get(player.server);var directory=GangSavedData.get(player.server).directory;var gang=directory.of(player.getUUID());
        require(gang!=null,"Najpierw dołącz do gangu.");long now=System.currentTimeMillis();
        if(action.equals("capture_unlock")){
            require(directory.isLeader(gang,player.getUUID()),"Tylko lider aktywuje Kontrolę terytoriów.");
            saved.board.unlock(gang.id);saved.setDirty();send(player,key,"Aktywowano Kontrolę terytoriów.");return;
        }
        require(GangDirectory.allowed(gang,player.getUUID(),action.equals("capture_start")?Permission.START_TERRITORY_ATTACK:Permission.JOIN_TERRITORY_ATTACK),"Nie masz uprawnienia do tej czynności.");
        require(present(player,key),"Stań w wybranym chunku w trybie przetrwania lub przygody.");
        var terrain=TerrainSavedData.get(player.serverLevel());require(terrain.atlas.get(key)!=null,"Najpierw poczekaj na rozpoznanie terenu.");
        if(action.equals("capture_start")){
            UUID owner=terrain.atlas.owner(key);
            require(owner==null||directory.get(owner)!=null,"Ten teren ma właściciela, którego atak nie jest teraz dostępny.");
            var participants=eligible(player,gang,key,saved.board);
            require(participants.size()>=saved.rules.minimumPlayers(),"W strefie jest za mało uprawnionych uczestników twojego gangu.");
            require(owner==null||!owner.equals(gang.id),"Ten teren należy już do twojego gangu.");
            require(firstOrAdjacent(terrain.atlas,gang.id,key),"Wybierz teren sąsiadujący bokiem z twoim terytorium w tym wymiarze.");
            saved.board.start(key,gang.id,owner,player.getUUID(),saved.rules,now);
            for(var participant:participants)if(!participant.getUUID().equals(player.getUUID()))saved.board.join(key,gang.id,participant.getUUID(),now);
        }else if(action.equals("capture_join")){
            var battle=saved.board.at(key);require(battle!=null,"Nie ma tu aktywnego captu.");
            var participants=eligible(player,gang,key,saved.board).stream().limit(CaptureBoard.MAX_PLAYERS-battle.fighters.size()).toList();
            boolean newTeam=!battle.teams().contains(gang.id);
            if(newTeam)require(participants.size()>=battle.rules.minimumPlayers(),"W strefie jest za mało uprawnionych uczestników twojego gangu.");
            saved.board.join(key,gang.id,player.getUUID(),now);
            if(newTeam)for(var participant:participants)if(!participant.getUUID().equals(player.getUUID()))saved.board.join(key,gang.id,participant.getUUID(),now);
        }
        else throw new IllegalArgumentException("Nieznana czynność captu.");
        saved.setDirty();notifyTeams(player.server,saved.board.at(key),"Zaktualizowano capt przy "+key.x()+", "+key.z()+".");
        send(player,key,"Zapisano udział w capcie.");
    }
    private static List<ServerPlayer> eligible(ServerPlayer actor,Gang gang,TerrainKey key,CaptureBoard board){
        return actor.server.getPlayerList().getPlayers().stream().filter(p->present(p,key)&&gang.members.containsKey(p.getUUID())&&!board.registered(p.getUUID()))
            .filter(p->p==actor||GangDirectory.allowed(gang,p.getUUID(),Permission.JOIN_TERRITORY_ATTACK)).sorted(Comparator.comparing(p->p!=actor)).limit(CaptureBoard.MAX_PLAYERS).toList();
    }
    public static boolean firstOrAdjacent(TerrainAtlas atlas,UUID gang,TerrainKey key){
        if(!atlas.hasLand(gang,key.dimension()))return true;
        for(int[] delta:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
            long x=(long)key.x()+delta[0],z=(long)key.z()+delta[1];if(Math.abs(x)>TerrainViewport.LIMIT||Math.abs(z)>TerrainViewport.LIMIT)continue;
            if(gang.equals(atlas.owner(new TerrainKey(key.dimension(),(int)x,(int)z))))return true;
        }return false;
    }
    public static boolean present(ServerPlayer player,TerrainKey key){
        return player!=null&&!player.isDeadOrDying()&&!player.isSpectator()&&!player.isCreative()
            &&player.serverLevel().dimension().location().toString().equals(key.dimension())
            &&player.chunkPosition().x==key.x()&&player.chunkPosition().z==key.z();
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();var saved=CaptureSavedData.get(server);var board=saved.board;long now=System.currentTimeMillis();
        if(board.recover(now))saved.setDirty();clock(server,saved,Instant.ofEpochMilli(now));
        if(server.getTickCount()%5!=0)return;
        long nano=System.nanoTime(),previous=lastTick.getOrDefault(server,nano);lastTick.put(server,nano);
        var directory=GangSavedData.get(server).directory;Map<UUID,CaptureBoard.Person> people=new HashMap<>();
        for(var battle:board.battles())for(var fighter:battle.fighters.values()){
            var player=server.getPlayerList().getPlayer(fighter.player);var gang=directory.get(fighter.gang);
            boolean member=gang!=null&&gang.members.containsKey(fighter.player);
            people.put(fighter.player,new CaptureBoard.Person(member?fighter.gang:new UUID(0,0),member&&present(player,battle.key)));
        }
        Set<UUID> existing=new HashSet<>();for(var gang:directory.gangs())existing.add(gang.id);
        boolean busy=!board.battles().isEmpty();
        var results=board.tick(now,(nano-previous)/1_000_000,people,existing);
        for(var result:results){
            var level=server.getAllLevels().iterator();net.minecraft.server.level.ServerLevel target=null;
            while(level.hasNext()){var next=level.next();if(next.dimension().location().toString().equals(result.key().dimension())){target=next;break;}}
            if(result.winner()!=null&&target!=null){
                var terrain=TerrainSavedData.get(target);terrain.atlas.setOwner(result.key(),result.winner());terrain.setDirty();
            }
            for(var gangId:result.gangs()){
                var gang=directory.get(gangId);if(gang==null)continue;
                directory.addEvent(gang,result.reason()+" Chunk "+result.key().x()+", "+result.key().z()+".");
                for(var id:gang.members.keySet()){var player=server.getPlayerList().getPlayer(id);if(player!=null)player.sendSystemMessage(Component.literal(result.reason()));}
            }
            GangSavedData.get(server).setDirty();
        }
        if(busy||!results.isEmpty())saved.setDirty();
        if(server.getTickCount()%20!=0)return;
        board.cleanup(existing);
        watchers.entrySet().removeIf(e->e.getKey().server==server&&(server.getTickCount()-e.getValue().tick()>100||e.getKey().hasDisconnected()||!e.getKey().serverLevel().dimension().location().toString().equals(e.getValue().key().dimension())));
        Set<ServerPlayer> sent=new HashSet<>();
        for(var e:watchers.entrySet())if(e.getKey().server==server){send(e.getKey(),e.getValue().key(),"");sent.add(e.getKey());}
        for(var b:board.battles())for(var f:b.fighters.values()){var player=server.getPlayerList().getPlayer(f.player);if(player!=null&&sent.add(player))send(player,b.key,"");}
        for(var player:new ArrayList<>(audience.keySet()))if(player.server==server&&!sent.contains(player)){
            if(!player.hasDisconnected())send(player,TerrainKey.at(player.serverLevel().dimension().location().toString(),player.getBlockX(),player.getBlockZ()),"");
            audience.remove(player);
        }
        sent.forEach(p->audience.put(p,true));
    }
    private static void clock(MinecraftServer server,CaptureSavedData saved,Instant now){
        var rule=server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        if(saved.rules.realTime()){
            if(!saved.daylightRemembered){saved.previousDaylight=rule.get();saved.daylightRemembered=true;saved.setDirty();}
            if(rule.get())rule.set(false,server);
            long time=saved.rules.minecraftTime(now);for(var level:server.getAllLevels())if(level.dimensionType().hasSkyLight())level.setDayTime(time);
        }else if(saved.daylightRemembered){rule.set(saved.previousDaylight,server);saved.daylightRemembered=false;saved.setDirty();}
    }
    public static JsonObject snapshot(ServerPlayer player,TerrainKey key,String message){
        long now=System.currentTimeMillis();var saved=CaptureSavedData.get(player.server);var directory=GangSavedData.get(player.server).directory;
        var gang=directory.of(player.getUUID());var atlas=TerrainSavedData.get(player.serverLevel()).atlas;
        JsonObject out=new JsonObject();out.addProperty("message",message);out.addProperty("serverNow",now);
        out.addProperty("dimension",key.dimension());out.addProperty("x",key.x());out.addProperty("z",key.z());
        out.addProperty("zone",saved.rules.zone());out.addProperty("time",Instant.ofEpochMilli(now).atZone(ZoneId.of(saved.rules.zone())).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        out.addProperty("opens",saved.rules.opens());out.addProperty("closes",saved.rules.closes());out.addProperty("open",saved.rules.open(Instant.ofEpochMilli(now)));
        out.addProperty("minimum",saved.rules.minimumPlayers());out.addProperty("unlocked",gang!=null&&saved.board.unlocked(gang.id));
        out.addProperty("cooldown",gang==null?0:saved.board.cooldown(gang.id,now));out.addProperty("protection",saved.board.protection(key,now));
        out.addProperty("present",present(player,key));out.addProperty("adjacent",gang!=null&&firstOrAdjacent(atlas,gang.id,key));
        UUID owner=atlas.owner(key);if(owner!=null){out.addProperty("owner",owner.toString());out.addProperty("ownerName",directory.get(owner)==null?"Nieznany gang":directory.get(owner).name);}
        out.addProperty("recognized",atlas.get(key)!=null);
        var battle=saved.board.at(key);if(battle!=null)out.add("battle",battleJson(player,battle,directory));
        var own=gang==null?null:saved.board.forGang(gang.id);
        if(own!=null&&own.fighters.containsKey(player.getUUID()))out.add("ownBattle",battleJson(player,own,directory));
        out.addProperty("busy",own!=null||saved.board.registered(player.getUUID()));
        out.addProperty("eligible",gang==null?0:eligible(player,gang,key,saved.board).size());
        JsonArray markers=new JsonArray();var watch=watchers.get(player);var viewport=watch==null?new TerrainViewport(key.x(),key.z()):watch.viewport();
        for(var mark:saved.markers.visible(player.getUUID(),gang==null?null:gang.id,key.dimension(),viewport)){
            JsonObject row=new JsonObject();row.addProperty("x",mark.key().x());row.addProperty("z",mark.key().z());row.addProperty("shared",mark.shared());row.addProperty("color",mark.color());markers.add(row);
        }out.add("markers",markers);
        JsonArray events=new JsonArray();for(var b:saved.board.battles()){
            JsonObject entry=new JsonObject();entry.addProperty("dimension",b.key.dimension());entry.addProperty("x",b.key.x());entry.addProperty("z",b.key.z());entry.addProperty("phase",b.phase.name());events.add(entry);
        }out.add("events",events);
        return out;
    }
    private static JsonObject battleJson(ServerPlayer viewer,CaptureBoard.Battle b,GangDirectory directory){
        JsonObject out=new JsonObject();out.addProperty("id",b.id.toString());out.addProperty("phase",b.phase.name());out.addProperty("remaining",b.remainingSeconds());
        out.addProperty("dimension",b.key.dimension());out.addProperty("x",b.key.x());out.addProperty("z",b.key.z());
        var fighter=b.fighters.get(viewer.getUUID());out.addProperty("joined",fighter!=null);out.addProperty("eliminated",fighter!=null&&fighter.eliminated);
        out.addProperty("minimum",b.rules.minimumPlayers());out.addProperty("present",present(viewer,b.key));
        out.addProperty("holding",b.holdingElapsed/1000);out.addProperty("holdRequired",b.rules.holdSeconds());
        JsonArray people=new JsonArray();for(var f:b.fighters.values()){
            var gang=directory.get(f.gang);if(gang==null)continue;JsonObject row=new JsonObject();row.addProperty("id",f.player.toString());
            row.addProperty("gang",f.gang.toString());row.addProperty("name",gang.name);row.addProperty("color",gang.mapColor);
            row.addProperty("eliminated",f.eliminated);row.addProperty("present",present(viewer.server.getPlayerList().getPlayer(f.player),b.key));people.add(row);
        }out.add("participants",people);return out;
    }
    private static void send(ServerPlayer player,TerrainKey key,String message){
        if(!player.serverLevel().dimension().location().toString().equals(key.dimension()))key=TerrainKey.at(player.serverLevel().dimension().location().toString(),player.getBlockX(),player.getBlockZ());
        PacketDistributor.sendToPlayer(player,new GangNetwork.Capture(JSON.toJson(snapshot(player,key,message))));
    }
    public static void respond(ServerPlayer player,TerrainKey key,String message){send(player,key,message==null?"Nieprawidłowe dane captu.":message);}
    private static void notifyTeams(MinecraftServer server,CaptureBoard.Battle b,String text){
        var directory=GangSavedData.get(server).directory;Set<UUID> teams=new HashSet<>(b.teams());if(b.previousOwner!=null)teams.add(b.previousOwner);
        for(UUID team:teams){var gang=directory.get(team);if(gang!=null)for(UUID id:gang.members.keySet()){var player=server.getPlayerList().getPlayer(id);if(player!=null)player.sendSystemMessage(Component.literal(text));}}
    }
    @SubscribeEvent public static void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer player)CaptureSavedData.get(player.server).board.eliminate(player.getUUID());}
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event){
        if(!(event.getEntity() instanceof ServerPlayer victim)||!(event.getSource().getEntity() instanceof ServerPlayer attacker))return;
        for(var b:CaptureSavedData.get(victim.server).board.battles()){
            var v=b.fighters.get(victim.getUUID());var a=b.fighters.get(attacker.getUUID());
            if(v==null&&a==null||!present(victim,b.key)&&!present(attacker,b.key))continue;
            if(b.phase!=CaptureBoard.Phase.FIGHTING||v==null||a==null||v.eliminated||a.eliminated||a.gang.equals(v.gang))event.setCanceled(true);
            return;
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){lastTick.remove(event.getServer());watchers.entrySet().removeIf(e->e.getKey().server==event.getServer());audience.keySet().removeIf(p->p.server==event.getServer());}
    private static void require(boolean condition,String text){if(!condition)throw new IllegalArgumentException(text);}
}
