package pl.crimeandlaw.gang;

import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Disposable dedicated server with two actual TCP clients, then a disk restart. */
@EventBusSubscriber(modid="crimeandlaw")
public final class FinanceServerSmoke {
    private static final UUID OWNER=id("Owner29"),MEMBER=id("Member29"),FRIEND=id("Friend29");
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.financeOutput","build/finance-smoke"));
    private static long start;private static boolean done,deathDone,commandsDone;
    private static UUID id(String name){return UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("crimeandlaw.financeSmoke"))return;
        try{
            Files.createDirectories(OUT);start=System.nanoTime();var d=GangSavedData.get(event.getServer()).directory;
            if(Boolean.getBoolean("crimeandlaw.financeRestart")){
                verify(d);check(!d.needsMigration(),"Current schema reloads without another migration");
                Files.writeString(OUT.resolve("restart.PASS"),"Actual world reload preserves accounts, ledger and rank permissions.\n");done=true;event.getServer().halt(false);return;
            }
            var world=event.getServer().overworld();int ground=world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,0,0)-1;
            for(int x=-48;x<=48;x++)for(int z=-30;z<=30;z++){
                var block=z>=9&&z<=16?net.minecraft.world.level.block.Blocks.BLUE_CONCRETE:z>=-9&&z<=-2?(z==-6&&x%6<3?net.minecraft.world.level.block.Blocks.YELLOW_CONCRETE:net.minecraft.world.level.block.Blocks.GRAY_CONCRETE):x>=22&&x<=35&&z<=-14&&z>=-26?net.minecraft.world.level.block.Blocks.RED_TERRACOTTA:null;
                if(block!=null)world.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,ground,z),block.defaultBlockState());
            }
            var gang=d.create(OWNER,"Owner29","Finanse 029","");
            d.request(MEMBER,"Member29",gang.id);d.accept(OWNER,MEMBER);d.request(FRIEND,"Friend29",gang.id);d.accept(OWNER,FRIEND);
            d.role(OWNER,"member","Członek",Set.of(GangDirectory.Permission.DEPOSIT_FUNDS));
            d.finances.credit(OWNER,GangFinances.Account.CASH,1234);d.finances.credit(MEMBER,GangFinances.Account.CASH,567);
            d.goals.recruit(gang.id,FRIEND,java.time.Instant.now(),java.time.ZoneId.of("Europe/Warsaw"));
            d.finances.credit(OWNER,100000);d.finances.deposit(d,OWNER,80000,"Kapitał testowy");GangSavedData.get(event.getServer()).setDirty();
            Files.writeString(OUT.resolve("server.ready"),"ready");
        }catch(Throwable error){fail(event.getServer(),error);}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(!Boolean.getBoolean("crimeandlaw.financeSmoke")||done||start==0)return;
        try{
            check(System.nanoTime()-start<420_000_000_000L,"Financial smoke timed out");
            var owner=event.getServer().getPlayerList().getPlayer(OWNER);var member=event.getServer().getPlayerList().getPlayer(MEMBER);
            if(!commandsDone&&owner!=null&&member!=null){
                check(event.getServer().getCommands().getDispatcher().execute("crimeandlaw pieniadze dodaj Owner29 gotowka 1",event.getServer().createCommandSourceStack())==1,"Console grants cash through the real command");
                boolean denied=false;try{event.getServer().getCommands().getDispatcher().execute("crimeandlaw pieniadze dodaj Member29 bank 999",member.createCommandSourceStack().withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
                check(denied,"Non-operator cannot grant money");owner.teleportTo(0.5,owner.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,0,0),0.5);commandsDone=true;
            }
            if(!deathDone&&member!=null&&Files.exists(OUT.resolve("member-death.ready"))){member.kill();deathDone=true;}
            if(!Files.exists(OUT.resolve("owner.PASS"))||!Files.exists(OUT.resolve("member.PASS")))return;
            var saved=GangSavedData.get(event.getServer());verify(saved.directory);saved.setDirty();event.getServer().saveEverything(true,true,true);
            Files.writeString(OUT.resolve("server.PASS"),"TCP payouts, denied operations, replay rejection and conserved bank/treasury total of $100000 plus $1802 cash verified.\n");done=true;event.getServer().halt(false);
        }catch(Throwable error){fail(event.getServer(),error);}
    }
    private static void verify(GangDirectory d){
        var g=d.of(OWNER);check(g!=null&&g.members.size()==2,"Membership persists after blacklist removes the offline recipient");
        check(d.finances.cash(OWNER)==1235&&d.finances.cash(MEMBER)==567,"Independent cash survives death and restart");
        check(g.blacklist.entries().isEmpty()&&g.blacklist.history().size()==2,"Blacklist add/remove and history persist");
        check(d.finances.wallet(OWNER)==19500,"Owner account");check(d.finances.wallet(MEMBER)==500,"Member account");
        check(d.finances.wallet(FRIEND)==300,"Offline recipient account");check(d.finances.balance(g.id)==79700,"Gang account");
        check(d.finances.history(g.id).size()==7,"Only successful entries are in the ledger");check(g.history.size()==4,"Public register includes the blacklist removal event");
        check(GangDirectory.allowed(g,MEMBER,GangDirectory.Permission.DEPOSIT_FUNDS)&&!GangDirectory.allowed(g,MEMBER,GangDirectory.Permission.PAY_BONUSES),"Separate rank permissions persist");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void fail(MinecraftServer server,Throwable error){done=true;error.printStackTrace();try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("server.FAIL"),error.toString());}catch(Exception ignored){}server.halt(false);}
}
