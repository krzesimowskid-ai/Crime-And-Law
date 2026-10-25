package pl.crimeandlaw;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import pl.crimeandlaw.network.GangNetwork;

@Mod(CrimeAndLaw.ID)
public final class CrimeAndLaw {
    public static final String ID="crimeandlaw";
    public CrimeAndLaw(IEventBus bus){bus.addListener(GangNetwork::register);pl.crimeandlaw.territory.TerrainOrigins.TYPES.register(bus);pl.crimeandlaw.cultivation.CultivationContent.register(bus);}
}
