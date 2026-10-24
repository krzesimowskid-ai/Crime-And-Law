package pl.crimeandlaw.territory;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.crimeandlaw.gang.*;
import pl.crimeandlaw.network.GangNetwork;

/** Disposable real dedicated server, then two real clients, then a disk restart. */
@EventBusSubscriber(modid="crimeandlaw")
public final class CaptureServerSmoke {
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.captureOutput","build/capture-smoke"));
    private static final UUID A=new UUID(25,101),B=new UUID(25,102),C=new UUID(25,103),D=new UUID(26,104);
    private static final TerrainKey KEY=new TerrainKey("minecraft:overworld",-30,40),NEXT=new TerrainKey(KEY.dimension(),-29,40),RESTART=new TerrainKey(KEY.dimension(),-30,41);
    private static final Map<UUID,FakePlayer> fakes=new HashMap<>();
    private static UUID ga,gb;private static int stage,wait,combatTicks;private static boolean done;
    private static ItemEntity unrelated;
    private static long began;
    private static final Set<UUID> prepared=new HashSet<>();
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("crimeandlaw.captureSmoke"))return;var server=event.getServer();began=System.nanoTime();
        try{
            Files.createDirectories(OUT);
            if(Boolean.getBoolean("crimeandlaw.captureRestart")){
                var meta=JsonParser.parseString(Files.readString(OUT.resolve("expected.json"))).getAsJsonObject();UUID gang=UUID.fromString(meta.get("gang").getAsString());
                var data=CaptureSavedData.get(server);var terrain=TerrainSavedData.get(server.overworld());
                check(gang.equals(terrain.atlas.owner(KEY)),"Ownership survives disk restart");
                check(data.board.protection(KEY,System.currentTimeMillis())>0,"Protection survives disk restart");
                check(data.board.at(RESTART)==null&&terrain.atlas.owner(RESTART)==null,"Unfinished event cancels, never awards offline progress");
                check(data.board.cooldown(gang,System.currentTimeMillis())>0,"Interrupted event has persisted cooldown");
                check(CaptureLoot.get(server).count(gang)==7,"Depot items and components survive disk restart");
                check(data.markers.visible(A,gang,KEY.dimension(),new TerrainViewport(KEY.x(),KEY.z())).size()==2,"Private/shared notes survive disk restart");
                check(data.rules.zone().equals("Europe/Warsaw")&&data.rules.realTime(),"Timezone and 24-hour clock survive restart");
                Files.writeString(OUT.resolve("restart.PASS"),"Ownership, protection, cancellation, cooldown, depot, scoped notes, timezone persist on disk.\n");
                done=true;server.halt(false);return;
            }
            var directory=GangSavedData.get(server).directory;
            ga=directory.create(A,"FakeOwner","Serwerowe A","").id;gb=directory.create(B,"FakeRival","Serwerowe B","").id;
            directory.request(C,"FakeMember",ga);directory.accept(A,C);GangSavedData.get(server).setDirty();
            directory.request(D,"FakeParticipant",gb);directory.accept(B,D);directory.role(B,"member","Atakujący",Set.of(GangDirectory.Permission.JOIN_TERRITORY_ATTACK));
            for(var id:List.of(A,B,C,D)){
                var fake=new FakePlayer(server.overworld(),new GameProfile(id,"Fake"+id.getLeastSignificantBits()));fake.setGameMode(GameType.SURVIVAL);fake.setPos(KEY.x()*16+8,100,KEY.z()*16+8);fakes.put(id,fake);register(server,fake,true);
            }
            for(var key:List.of(KEY,NEXT,RESTART))remember(server,key);
            server.overworld().setChunkForced(KEY.x(),KEY.z(),true);server.overworld().getChunk(KEY.x(),KEY.z());
            server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false,server);
            check(command(server,"crimeandlaw czas Europe/Warsaw")==1,"Console sets timezone without manual owner configuration");
            var before=CaptureSavedData.get(server).rules;check(command(server,"crimeandlaw czas Not/AZone")==0&&before.equals(CaptureSavedData.get(server).rules),"Invalid timezone is rejected atomically");
            boolean denied=false;try{server.getCommands().getDispatcher().execute("crimeandlaw czas UTC",fakes.get(C).createCommandSourceStack().withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
            check(denied,"Non-operator cannot change the server clock");
            System.out.println("CAPTURE_SMOKE_START");
        }catch(Throwable e){fail(server,e);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(ServerTickEvent.Post event){
        if(!Boolean.getBoolean("crimeandlaw.captureSmoke")||done)return;var server=event.getServer();
        try{
            check(System.nanoTime()-began<420_000_000_000L,"Capture smoke timeout at stage "+stage);
            var data=CaptureSavedData.get(server);var directory=GangSavedData.get(server).directory;var terrain=TerrainSavedData.get(server.overworld());
            if(stage<6){
                switch(stage){
                    case 0->{
                        if(++wait<10)return;
                        check(!server.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT),"Real time disables vanilla sun progression");
                        check(Math.abs(server.overworld().getDayTime()-data.rules.minecraftTime(java.time.Instant.now()))<=2,"Actual world sun follows timezone clock");
                        command(server,"crimeandlaw czas off");stage=1;
                    }
                    case 1->{
                        check(server.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT),"Turning clock off restores the original gamerule");
                        data.rules=new CaptureRules("UTC",false,0,24,3,2,2,20,3,8,3600,1);data.setDirty();
                        var a=fakes.get(A);CaptureController.action(a,"capture_unlock",KEY);CaptureController.action(fakes.get(B),"capture_unlock",KEY);
                        rejected(()->CaptureController.action(fakes.get(C),"capture_start",KEY),"Member without permission cannot start");
                        a.setGameMode(GameType.CREATIVE);rejected(()->CaptureController.action(a,"capture_start",KEY),"Creative presence is rejected");a.setGameMode(GameType.SURVIVAL);
                        a.setPos(KEY.x()*16+40,100,KEY.z()*16+8);rejected(()->CaptureController.action(a,"capture_start",KEY),"Wrong chunk is rejected");a.setPos(KEY.x()*16+8,100,KEY.z()*16+8);
                        CaptureController.action(a,"capture_start",KEY);
                        rejected(()->CaptureController.action(fakes.get(B),"capture_join",KEY),"A leader without bordering land cannot join neutral capture");
                        var border=new TerrainKey(KEY.dimension(),KEY.x()+1,KEY.z()+1);remember(server,border);terrain.atlas.setOwner(border,gb);terrain.setDirty();
                        check(CaptureController.snapshot(fakes.get(B),KEY,"").getAsJsonObject("battle").get("joinAllowed").getAsBoolean(),"A diagonal border admits the real leader");
                        rejected(()->CaptureController.action(fakes.get(D),"capture_join",KEY),"Even a permitted member cannot commit a new gang without its leader");
                        check(!CaptureController.snapshot(fakes.get(D),KEY,"").getAsJsonObject("battle").get("joinAllowed").getAsBoolean(),"The client receives the same leadership block");
                        check(data.board.at(KEY).teams().size()==1,"Rejected attempts never enroll the gang");
                        var tagged=item(server,KEY,31);tagged.getPersistentData().putUUID(CaptureLoot.EVENT,data.board.at(KEY).id);unrelated=item(server,KEY,5);
                        data.markers.set(KEY,A,ga,false,0x12ABCD);data.markers.set(KEY,A,ga,true,0xC84375);data.setDirty();
                        stage=2;
                    }
                    case 2->{
                        if(data.board.at(KEY)!=null)return;
                        check(ga.equals(terrain.atlas.owner(KEY)),"Authoritative neutral capture changes chunk owner");
                        check(CaptureLoot.get(server).count(ga)==31&&unrelated.isAlive()&&unrelated.getItem().getCount()==5,"Only event drops collected; unrelated identical items cannot merge into loot");
                        var inventory=fakes.get(A).getInventory();for(int i=0;i<inventory.items.size();i++)inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));
                        fakes.get(A).getAbilities().instabuild=true;
                        check(CaptureLoot.get(server).claim(fakes.get(A),ga)==0&&CaptureLoot.get(server).count(ga)==31,"A full creative inventory cannot destroy depot items");
                        inventory.setItem(0,new ItemStack(Items.DIAMOND,63));
                        check(CaptureLoot.get(server).claim(fakes.get(A),ga)==1&&CaptureLoot.get(server).count(ga)==30,"Only the portion that fits is claimed");
                        inventory.clearContent();fakes.get(A).getAbilities().instabuild=false;
                        check(CaptureLoot.get(server).claim(fakes.get(A),ga)==30&&CaptureLoot.get(server).count(ga)==0,"Remaining depot transfer conserves items");
                        var extra=item(server,KEY,7);var id=UUID.randomUUID();extra.getPersistentData().putUUID(CaptureLoot.EVENT,id);CaptureLoot.get(server).collect(server.overworld(),KEY,id,ga);
                        rejected(()->CaptureController.action(fakes.get(B),"capture_start",KEY),"Protection rejects new attack");
                        var saved=CaptureLoot.get(server).save(new CompoundTag(),server.registryAccess());check(CaptureLoot.load(saved,server.registryAccess()).count(ga)==7,"Real NBT retains the depot");
                        stage=3;
                    }
                    case 3->{
                        if(data.board.cooldown(ga,System.currentTimeMillis())>0)return;
                        for(var fake:fakes.values())fake.setPos(NEXT.x()*16+8,100,NEXT.z()*16+8);
                        CaptureController.action(fakes.get(B),"capture_start",NEXT);CaptureController.action(fakes.get(A),"capture_join",NEXT);stage=4;
                    }
                    case 4->{
                        if(data.board.at(NEXT).phase!=CaptureBoard.Phase.FIGHTING)return;
                        CaptureController.death(new LivingDeathEvent(fakes.get(A),server.overworld().damageSources().generic()));
                        rejected(()->CaptureController.action(fakes.get(A),"capture_join",NEXT),"Eliminated participant cannot return");stage=5;
                    }
                    case 5->{
                        if(data.board.at(NEXT)!=null)return;check(gb.equals(terrain.atlas.owner(NEXT)),"PvP winner gains territory");
                        for(var fake:fakes.values())register(server,fake,false);
                        data.rules=new CaptureRules("Europe/Warsaw",true,0,24,30,4,4,60,6,8,3600,1);data.setDirty();
                        Files.writeString(OUT.resolve("server.PASS"),"Actual dedicated server: commands/permission, clock/gamerule restoration, presence, neutral capture, PvP elimination, protection, loot isolation and NBT passed.\n");
                        stage=6;
                    }
                }return;
            }
            for(var player:server.getPlayerList().getPlayers())if(player.getGameProfile().getName().startsWith("Capture")&&prepared.add(player.getUUID())){
                var gang=directory.create(player.getUUID(),player.getGameProfile().getName(),player.getGameProfile().getName()+" Gang","");directory.mapColor(player.getUUID(),player.getGameProfile().getName().equals("CaptureOwner")?0xB84F7B:0x3BB8B3);
                if(player.getGameProfile().getName().equals("CaptureRival")){var border=new TerrainKey(KEY.dimension(),1,1);remember(server,border);terrain.atlas.setOwner(border,gang.id);terrain.setDirty();}
                double x=player.getGameProfile().getName().equals("CaptureOwner")?8:12,z=player.getGameProfile().getName().equals("CaptureOwner")?8:12;
                int y=server.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(int)x,(int)z)+1;
                player.setGameMode(GameType.SURVIVAL);player.setInvulnerable(true);player.setNoGravity(true);player.connection.teleport(x,y,z,-45,0);
                if(player.getGameProfile().getName().equals("CaptureRival"))player.getInventory().add(new ItemStack(Items.DIAMOND,11));
                GangSavedData.get(server).setDirty();GangNetwork.sendState(player,"");System.out.println("CAPTURE_REAL_PLAYER_READY "+player.getGameProfile().getName());
            }
            var battle=data.board.battles().stream().filter(b->b.key.x()==0&&b.key.z()==0).findFirst().orElse(null);
            if(battle!=null&&battle.phase==CaptureBoard.Phase.FIGHTING&&++combatTicks==25){
                var rival=server.getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().getName().equals("CaptureRival")).findFirst().orElseThrow();rival.setInvulnerable(false);rival.kill();
            }
            if(Files.exists(OUT.resolve("owner.PASS"))&&Files.exists(OUT.resolve("rival.PASS"))){
                data.rules=data.rules.change("cooldown",600);data.board.start(RESTART,ga,null,A,data.rules,System.currentTimeMillis());data.setDirty();
                var meta=new JsonObject();meta.addProperty("gang",ga.toString());Files.writeString(OUT.resolve("expected.json"),meta.toString());
                done=true;System.out.println("CAPTURE_TWO_CLIENTS_PASS");server.halt(false);
            }
        }catch(Throwable e){fail(server,e);}
    }
    private static int command(MinecraftServer server,String command)throws Exception{return server.getCommands().getDispatcher().execute(command,server.createCommandSourceStack());}
    private static ItemEntity item(MinecraftServer server,TerrainKey key,int count){var item=new ItemEntity(server.overworld(),key.x()*16+5,100,key.z()*16+5,new ItemStack(Items.DIAMOND,count));item.setNoGravity(true);item.setDeltaMovement(0,0,0);server.overworld().addFreshEntity(item);return item;}
    private static void remember(MinecraftServer server,TerrainKey key){TerrainSavedData.get(server.overworld()).atlas.remember(TerrainProfile.analyse(key,List.of(new TerrainProfile.Sample(70,TerrainProfile.Surface.SOIL,false,"minecraft:plains")),false,List.of(),1));TerrainSavedData.get(server.overworld()).setDirty();}
    @SuppressWarnings("unchecked") private static void register(MinecraftServer server,ServerPlayer player,boolean add)throws Exception{
        if(add){var channel=net.minecraft.network.Connection.class.getDeclaredField("channel");channel.setAccessible(true);channel.set(player.connection.getConnection(),new io.netty.channel.embedded.EmbeddedChannel());}
        var a=PlayerList.class.getDeclaredField("players");a.setAccessible(true);var list=(List<ServerPlayer>)a.get(server.getPlayerList());
        var b=PlayerList.class.getDeclaredField("playersByUUID");b.setAccessible(true);var map=(Map<UUID,ServerPlayer>)b.get(server.getPlayerList());
        if(add){list.add(player);map.put(player.getUUID(),player);}else{list.remove(player);map.remove(player.getUUID());}
    }
    private static void rejected(Runnable action,String detail){boolean failed=false;try{action.run();}catch(IllegalArgumentException expected){failed=true;}check(failed,detail);}
    private static void check(boolean condition,String detail){if(!condition)throw new AssertionError(detail);}
    private static void fail(MinecraftServer server,Throwable error){error.printStackTrace();done=true;try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("server.FAIL"),error.toString());}catch(Exception ignored){}server.halt(false);}
}
