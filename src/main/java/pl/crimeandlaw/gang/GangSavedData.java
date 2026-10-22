package pl.crimeandlaw.gang;

import java.nio.charset.StandardCharsets;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class GangSavedData extends SavedData {
    public final GangDirectory directory;
    private GangSavedData(){directory=new GangDirectory();}
    private GangSavedData(GangDirectory directory){this.directory=directory;if(directory.needsMigration())setDirty();}
    public static GangSavedData get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(GangSavedData::new,GangSavedData::load),"crimeandlaw_gangs");
    }
    private static GangSavedData load(CompoundTag tag,HolderLookup.Provider provider){
        return new GangSavedData(GangDirectory.fromJson(new String(tag.getByteArray("directory"),StandardCharsets.UTF_8)));
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        tag.putInt("version",1);tag.putByteArray("directory",directory.toJson().getBytes(StandardCharsets.UTF_8));return tag;
    }
}
