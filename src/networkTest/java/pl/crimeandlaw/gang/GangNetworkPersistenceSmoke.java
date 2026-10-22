package pl.crimeandlaw.gang;

import java.nio.file.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Reads the world saved by two real clients after restarting the dedicated server. */
@EventBusSubscriber(modid="crimeandlaw")
public final class GangNetworkPersistenceSmoke {
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
