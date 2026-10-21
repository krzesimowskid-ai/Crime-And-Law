package pl.crimeandlaw.territory;

/** One complete chunk column, distinguished by dimension. */
public record TerrainKey(String dimension,int x,int z) {
    public TerrainKey {
        if(dimension==null||dimension.length()>128||!dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")
            ||Math.abs((long)x)>1_875_000||Math.abs((long)z)>1_875_000)throw new IllegalArgumentException("Nieprawidłowe położenie terenu.");
    }
    public static TerrainKey at(String dimension,int blockX,int blockZ){return new TerrainKey(dimension,Math.floorDiv(blockX,16),Math.floorDiv(blockZ,16));}
    public String storageKey(){return dimension+"|"+x+","+z;}
}
