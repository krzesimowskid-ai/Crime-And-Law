package pl.crimeandlaw.territory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class TerrainSavedData extends SavedData {
    public final TerrainAtlas atlas;
    private final Map<TerrainKey,TerrainMapTile> maps=new LinkedHashMap<>();
    private TerrainSavedData(){this(new TerrainAtlas());}
    private TerrainSavedData(TerrainAtlas atlas){this.atlas=atlas;}
    public static TerrainSavedData get(ServerLevel level){
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(TerrainSavedData::new,TerrainSavedData::load),"crimeandlaw_terrain");
    }
    static TerrainSavedData load(CompoundTag tag,HolderLookup.Provider provider){
        var saved=new TerrainSavedData(TerrainAtlas.fromJson(new String(tag.getByteArray("atlas"),StandardCharsets.UTF_8)));
        for(Tag value:tag.getList("maps",Tag.TAG_COMPOUND)){
            var tile=(CompoundTag)value;
            saved.maps.put(new TerrainKey(tile.getString("dimension"),tile.getInt("x"),tile.getInt("z")),TerrainMapTile.of(tile.getByteArray("colors")));
        }
        return saved;
    }
    public TerrainMapTile map(TerrainKey key){return maps.get(key);}
    public void rememberMap(TerrainKey key,TerrainMapTile tile){if(!tile.equals(maps.put(key,tile)))setDirty();}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        tag.putInt("version",2);tag.putByteArray("atlas",atlas.toJson().getBytes(StandardCharsets.UTF_8));
        ListTag tiles=new ListTag();maps.forEach((key,tile)->{
            var entry=new CompoundTag();entry.putString("dimension",key.dimension());entry.putInt("x",key.x());entry.putInt("z",key.z());entry.putByteArray("colors",tile.colors());tiles.add(entry);
        });tag.put("maps",tiles);return tag;
    }
}
