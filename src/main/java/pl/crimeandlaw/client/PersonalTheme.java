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
        get(gang);if(theme==null)themes.remove(gang);else themes.put(gang,theme);
        try{Files.createDirectories(FILE.getParent());Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(new Data(themes)));Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);return true;}
        catch(Exception exception){ClientState.message="Nie udało się zapisać osobistego motywu.";return false;}
    }
}
