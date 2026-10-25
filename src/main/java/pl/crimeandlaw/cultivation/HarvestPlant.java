package pl.crimeandlaw.cultivation;
import java.util.UUID;
public interface HarvestPlant {
    UUID identity(); int leaves(); int totalLeaves(); boolean ready(); boolean cut(int leaf); void finishHarvest();
}
