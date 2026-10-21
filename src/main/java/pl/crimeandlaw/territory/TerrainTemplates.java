package pl.crimeandlaw.territory;

import java.util.*;
import pl.crimeandlaw.territory.TerrainProfile.Trait;

/** Uses generated jigsaw template identifiers, never player-placed workstation blocks. */
public final class TerrainTemplates {
    private TerrainTemplates(){}
    public static Set<Trait> traits(String template){
        Set<Trait> result=EnumSet.noneOf(Trait.class);
        if(template==null||!template.contains(":village/"))return result;
        result.add(Trait.VILLAGE);String name=template.substring(template.lastIndexOf('/')+1);
        if(name.contains("farm"))result.add(Trait.FARM);
        if(name.contains("weaponsmith")||name.contains("tool_smith")||name.contains("toolsmith")||name.contains("armorer")||name.contains("smith"))result.add(Trait.WORKSHOP);
        if(name.contains("animal_pen")||name.contains("shepherd")||name.contains("butcher"))result.add(Trait.ANIMAL_PEN);
        if(name.contains("fisher"))result.add(Trait.FISHERY);
        if(name.contains("library"))result.add(Trait.LIBRARY);
        if(name.contains("tannery"))result.add(Trait.TANNERY);
        return result;
    }
    public static boolean intersectsChunk(int minX,int minZ,int maxX,int maxZ,int chunkX,int chunkZ){
        long x=(long)chunkX*16,z=(long)chunkZ*16;
        return minX<=x+15&&maxX>=x&&minZ<=z+15&&maxZ>=z;
    }
}
