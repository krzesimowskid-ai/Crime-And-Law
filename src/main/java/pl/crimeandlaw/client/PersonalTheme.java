package pl.crimeandlaw.client;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import net.neoforged.fml.loading.FMLPaths;
import pl.crimeandlaw.gang.GangDirectory.Theme;

public final class PersonalTheme {
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("crimeandlaw-appearance.json");
    private static final Gson JSON=new Gson();
    private static final Map<String,Theme> themes=new HashMap<>();
    private static boolean loaded;
    private record Data(Map<String,Theme> themes){}
    public static Theme get(String gang){
        if(!loaded){loaded=true;
            try{if(Files.exists(FILE)){Data d=JSON.fromJson(Files.readString(FILE),Data.class);if(d!=null && d.themes!=null)themes.putAll(d.themes);}}
            catch(Exception ignored){ClientState.message="Nie udało się odczytać osobistego motywu.";}
        }
        return themes.get(gang);
    }
    public static boolean set(String gang,Theme theme){
        get(gang);Map<String,Theme> next=new HashMap<>(themes);if(theme==null)next.remove(gang);else next.put(gang,theme);
        Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
        try{Files.createDirectories(FILE.getParent());Files.writeString(tmp,JSON.toJson(new Data(next)));
            try{Files.move(tmp,FILE,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException exception){Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);}
            themes.clear();themes.putAll(next);return true;}
        catch(Exception exception){ClientState.message="Nie udało się zapisać osobistego motywu.";return false;}
        finally{try{Files.deleteIfExists(tmp);}catch(Exception ignored){}}
    }
}
