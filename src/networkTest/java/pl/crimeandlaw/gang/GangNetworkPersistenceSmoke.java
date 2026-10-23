package pl.crimeandlaw.gang;

import java.nio.file.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Reads the world saved by two real clients after restarting the dedicated server. */
@EventBusSubscriber(modid="crimeandlaw")
public final class GangNetworkPersistenceSmoke {
    private static int peakJobs,peakReads,peakViewers;
    @SubscribeEvent public static void budgets(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        String path=System.getProperty("crimeandlaw.networkSmokeOutput");
        if(path==null||Boolean.getBoolean("crimeandlaw.networkSmokePersistence"))return;
        try{
            var queues=pl.crimeandlaw.territory.TerrainCartography.class.getDeclaredField("QUEUES");queues.setAccessible(true);
            var queue=((java.util.Map<?,?>)queues.get(null)).get(event.getServer());if(queue==null)return;
            var jobs=queue.getClass().getDeclaredField("jobs");jobs.setAccessible(true);
            var flights=queue.getClass().getDeclaredField("flights");flights.setAccessible(true);
            var demand=queue.getClass().getDeclaredField("demand");demand.setAccessible(true);
            int jobCount=((java.util.Map<?,?>)jobs.get(queue)).size(),readCount=((java.util.List<?>)flights.get(queue)).size();
            int viewerCount=((pl.crimeandlaw.territory.TerrainDemand)demand.get(queue)).size();
            if(jobCount>pl.crimeandlaw.territory.TerrainDemand.MAX_JOBS||readCount>pl.crimeandlaw.territory.TerrainCartography.MAX_IN_FLIGHT||viewerCount>pl.crimeandlaw.territory.TerrainDemand.MAX_VIEWERS)throw new AssertionError("Map work bounds exceeded");
            peakJobs=Math.max(peakJobs,jobCount);peakReads=Math.max(peakReads,readCount);peakViewers=Math.max(peakViewers,viewerCount);
            if(event.getServer().getTickCount()%20==0)Files.writeString(Path.of(path,"budget.txt"),"peak jobs="+peakJobs+", actual read slots="+peakReads+", viewers="+peakViewers+"\n");
        }catch(Throwable error){
            error.printStackTrace();try{Files.createDirectories(Path.of(path));Files.writeString(Path.of(path,"budget.FAIL"),error.toString());}catch(Exception ignored){}
            event.getServer().halt(false);
        }
    }
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("crimeandlaw.networkSmokePersistence"))return;
        Path out=Path.of(System.getProperty("crimeandlaw.networkSmokeOutput"));
        try{
            var directory=GangSavedData.get(event.getServer()).directory;
            String name=System.getProperty("crimeandlaw.networkSmokeGang");
            var gang=directory.gangs().stream().filter(g->g.name.equals(name)).findFirst().orElseThrow();
            if(gang.members.size()!=2||!gang.members.get(gang.owner).name.equals("Owner16"))throw new AssertionError("Membership/owner not persisted");
            var member=gang.members.entrySet().stream().filter(e->e.getValue().name.equals("Member16")).findFirst().orElseThrow();
            if(!gang.roles.get(member.getValue().role).name.equals("Stylista")||!GangDirectory.allowed(gang,member.getKey(),GangDirectory.Permission.MANAGE_THEME))throw new AssertionError("Assigned role/permission not persisted");
            if(gang.theme.background()!=0x253047||gang.logo.length()<=28000||GangLogo.validate(gang.logo).isEmpty())throw new AssertionError("Shared theme/detailed logo not persisted");
            if(gang.banner.image().length()<=1_000_000||GangBanner.validateImage(gang.banner.image()).isEmpty()||gang.members.values().stream().anyMatch(m->m.joinedAt<=0))throw new AssertionError("Banner/join dates not persisted");
            Files.createDirectories(out);Files.writeString(out.resolve("restart.PASS"),"Dedicated-server restart retained two members, owner, assigned role/permissions, shared theme, detailed logo/banner and join dates.\n");
            System.out.println("CRIME_NETWORK_RESTART_PASS");
        }catch(Throwable error){
            error.printStackTrace();try{Files.createDirectories(out);Files.writeString(out.resolve("restart.FAIL"),error.toString());}catch(Exception ignored){}
        }finally{event.getServer().halt(false);}
    }
}
