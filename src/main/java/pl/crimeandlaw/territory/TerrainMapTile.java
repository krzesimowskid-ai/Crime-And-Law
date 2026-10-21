package pl.crimeandlaw.territory;

import java.util.Base64;

/** One actual surface column per pixel, in Minecraft's shaded map palette. */
public record TerrainMapTile(String pixels) {
    public static final int SIDE=16,PIXELS=SIDE*SIDE,ENCODED_LENGTH=344;
    public TerrainMapTile {
        if(pixels==null||pixels.length()!=ENCODED_LENGTH||Base64.getDecoder().decode(pixels).length!=PIXELS)
            throw new IllegalArgumentException("Nieprawidłowy fragment mapy.");
    }
    public static TerrainMapTile of(byte[] pixels){
        if(pixels==null||pixels.length!=PIXELS)throw new IllegalArgumentException("Niepełny fragment mapy.");
        return new TerrainMapTile(Base64.getEncoder().encodeToString(pixels));
    }
    public byte[] colors(){return Base64.getDecoder().decode(pixels);}
}
