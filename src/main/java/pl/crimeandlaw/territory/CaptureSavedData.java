package pl.crimeandlaw.territory;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-scoped rules and ongoing captures; protection is independent of server restarts. */
public final class CaptureSavedData extends SavedData {
    private static final Gson JSON=new Gson();
    public CaptureRules rules=CaptureRules.defaults();
    public CaptureBoard board=new CaptureBoard();
    public TerrainMarkers markers=new TerrainMarkers();
    public boolean daylightRemembered,previousDaylight=true;
    private CaptureSavedData(){}
    public static CaptureSavedData get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(CaptureSavedData::new,CaptureSavedData::load),"crimeandlaw_captures");
    }
    static CaptureSavedData load(CompoundTag tag,HolderLookup.Provider provider){
        var data=new CaptureSavedData();
        if(tag.contains("rules"))data.rules=JSON.fromJson(new String(tag.getByteArray("rules"),StandardCharsets.UTF_8),CaptureRules.class);
        if(data.rules==null)throw new IllegalArgumentException("Uszkodzone ustawienia czasu świata.");
        if(tag.contains("board"))data.board=CaptureBoard.fromJson(new String(tag.getByteArray("board"),StandardCharsets.UTF_8));
        if(tag.contains("markers"))data.markers=TerrainMarkers.fromJson(new String(tag.getByteArray("markers"),StandardCharsets.UTF_8));
        data.daylightRemembered=tag.getBoolean("daylightRemembered");data.previousDaylight=!tag.contains("previousDaylight")||tag.getBoolean("previousDaylight");
        if(data.board.recover(System.currentTimeMillis()))data.setDirty();return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        tag.putInt("version",1);tag.putByteArray("rules",JSON.toJson(rules).getBytes(StandardCharsets.UTF_8));
        tag.putByteArray("board",board.toJson().getBytes(StandardCharsets.UTF_8));
        tag.putByteArray("markers",markers.toJson().getBytes(StandardCharsets.UTF_8));
        tag.putBoolean("daylightRemembered",daylightRemembered);tag.putBoolean("previousDaylight",previousDaylight);return tag;
    }
}
