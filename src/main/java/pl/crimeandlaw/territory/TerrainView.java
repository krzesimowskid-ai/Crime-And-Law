package pl.crimeandlaw.territory;

import java.util.*;
import java.util.function.*;

/** Bounded summaries; raw structure NBT and full template lists stay on the server. */
public record TerrainView(String dimension,int centerX,int centerZ,int radius,List<Cell> cells) {
    public static final int RADIUS=3,MAX_JSON=65_000;
    public enum Status { READY, PENDING, UNAVAILABLE }
    public record Cell(int x,int z,Status status,int color,int minHeight,int maxHeight,
            int samples,int buildingPieces,String biome,Set<TerrainProfile.Trait> traits,TerrainProfile.Source source,UUID owner,String ownerName) {
        public Cell {traits=Set.copyOf(traits);}
    }
    public TerrainView {cells=List.copyOf(cells);}
    public static TerrainView snapshot(TerrainAtlas atlas,String dimension,int x,int z,Predicate<TerrainKey> loaded,Function<UUID,String> gangName){
        List<Cell> cells=new ArrayList<>();
        for(int dz=-RADIUS;dz<=RADIUS;dz++)for(int dx=-RADIUS;dx<=RADIUS;dx++){
            if(Math.abs((long)x+dx)>1_875_000||Math.abs((long)z+dz)>1_875_000){
                cells.add(new Cell(x+dx,z+dz,Status.UNAVAILABLE,0x292A33,0,0,0,0,"",Set.of(),null,null,""));continue;
            }
            TerrainKey key=new TerrainKey(dimension,x+dx,z+dz);TerrainProfile p=atlas.get(key);UUID owner=atlas.owner(key);
            cells.add(p==null?new Cell(key.x(),key.z(),loaded.test(key)?Status.PENDING:Status.UNAVAILABLE,0x292A33,0,0,0,0,"",Set.of(),null,null,""):
                new Cell(key.x(),key.z(),Status.READY,p.mapColor(),p.minHeight(),p.maxHeight(),p.sampleCount(),p.villagePieces().size(),
                    p.biomes().isEmpty()?"":p.biomes().getFirst(),p.traits(),p.source(),owner,owner==null?"":Objects.requireNonNullElse(gangName.apply(owner),"Nieznany gang")));
        }
        return new TerrainView(dimension,x,z,RADIUS,cells);
    }
}
