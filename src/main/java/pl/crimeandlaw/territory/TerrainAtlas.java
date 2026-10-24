package pl.crimeandlaw.territory;

import com.google.gson.Gson;
import java.util.*;

/** Server authority and persistent, first-write-only terrain descriptions. */
public final class TerrainAtlas {
    private static final Gson JSON=new Gson();
    private final Map<String,TerrainProfile> profiles=new LinkedHashMap<>();
    private final Map<String,UUID> owners=new LinkedHashMap<>();
    public TerrainProfile get(TerrainKey key){return profiles.get(key.storageKey());}
    public int size(){return profiles.size();}
    public boolean remember(TerrainProfile profile){return profiles.putIfAbsent(profile.key().storageKey(),profile)==null;}
    public UUID owner(TerrainKey key){return owners.get(key.storageKey());}
    public boolean hasLand(UUID gang,String dimension){return owners.entrySet().stream().anyMatch(e->e.getKey().startsWith(dimension+"|")&&e.getValue().equals(gang));}
    public boolean neighbours(UUID gang,TerrainKey key,boolean diagonals){
        if(gang==null)return false;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            if(dx==0&&dz==0||!diagonals&&Math.abs(dx)+Math.abs(dz)!=1)continue;
            long x=(long)key.x()+dx,z=(long)key.z()+dz;
            if(Math.abs(x)>TerrainViewport.LIMIT||Math.abs(z)>TerrainViewport.LIMIT)continue;
            if(gang.equals(owner(new TerrainKey(key.dimension(),(int)x,(int)z))))return true;
        }return false;
    }
    public boolean releaseMissingOwners(Set<UUID> existing){return owners.values().removeIf(id->!existing.contains(id));}
    /** Only the authoritative capture controller may change ownership. */
    public void setOwner(TerrainKey key,UUID gang){
        if(get(key)==null)throw new IllegalArgumentException("Teren nie został jeszcze rozpoznany.");
        if(gang==null)owners.remove(key.storageKey());else owners.put(key.storageKey(),gang);
    }
    /** Descriptive traits of owned land. These are NOT qualified or awarded bonuses. */
    public Set<TerrainProfile.Trait> traitsAt(TerrainKey key,UUID gang){
        TerrainProfile profile=get(key);
        return profile!=null&&gang!=null&&gang.equals(owner(key))?profile.traits():Set.of();
    }
    public String toJson(){return JSON.toJson(this);}
    public static TerrainAtlas fromJson(String json){
        TerrainAtlas atlas=JSON.fromJson(json,TerrainAtlas.class);
        if(atlas==null||atlas.profiles==null||atlas.owners==null)throw new IllegalArgumentException("Nieprawidłowy zapis terenów.");
        for(var entry:atlas.profiles.entrySet())if(entry.getValue()==null||!entry.getKey().equals(entry.getValue().key().storageKey()))throw new IllegalArgumentException("Nieprawidłowe położenie zapisanego terenu.");
        for(var entry:atlas.owners.entrySet())if(entry.getValue()==null||!atlas.profiles.containsKey(entry.getKey()))throw new IllegalArgumentException("Nieprawidłowa własność terenu.");
        return atlas;
    }
}
