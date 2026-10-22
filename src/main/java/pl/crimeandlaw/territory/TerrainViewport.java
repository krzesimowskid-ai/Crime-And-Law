package pl.crimeandlaw.territory;

/** A map location selects saved data, never a request to generate or load land. */
public record TerrainViewport(int x,int z) {
    public static final int LIMIT=1_875_000;
    public TerrainViewport {
        if(Math.abs((long)x)>LIMIT||Math.abs((long)z)>LIMIT)
            throw new IllegalArgumentException("Wybrany obszar jest poza granicą świata.");
    }
    public TerrainViewport move(long dx,long dz){
        // Clamp the displacement first, so even an untrusted long cannot overflow.
        return new TerrainViewport((int)Math.clamp(x+Math.clamp(dx,-2L*LIMIT,2L*LIMIT),-LIMIT,LIMIT),
            (int)Math.clamp(z+Math.clamp(dz,-2L*LIMIT,2L*LIMIT),-LIMIT,LIMIT));
    }
    public boolean near(int chunkX,int chunkZ,int radius){
        return Math.abs((long)x-chunkX)<=radius&&Math.abs((long)z-chunkZ)<=radius;
    }
}
