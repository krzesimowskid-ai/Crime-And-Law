package pl.crimeandlaw.territory;

import java.nio.charset.StandardCharsets;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class TerrainSavedData extends SavedData {
    public final TerrainAtlas atlas;
    private TerrainSavedData(){this(new TerrainAtlas());}
    private TerrainSavedData(TerrainAtlas atlas){this.atlas=atlas;}
    public static TerrainSavedData get(ServerLevel level){
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(TerrainSavedData::new,TerrainSavedData::load),"crimeandlaw_terrain");
    }
    private static TerrainSavedData load(CompoundTag tag,HolderLookup.Provider provider){
        return new TerrainSavedData(TerrainAtlas.fromJson(new String(tag.getByteArray("atlas"),StandardCharsets.UTF_8)));
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        tag.putInt("version",1);tag.putByteArray("atlas",atlas.toJson().getBytes(StandardCharsets.UTF_8));return tag;
    }
}
