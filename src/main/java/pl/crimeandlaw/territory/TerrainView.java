package pl.crimeandlaw.territory;

import java.util.*;
import java.util.function.*;

/** Bounded summaries; raw structure NBT and full template lists stay on the server. */
public record TerrainView(String dimension,int centerX,int centerZ,int radius,double playerX,double playerZ,float yaw,List<Cell> cells,int requestId) {
    public static final int RADIUS=5,SIDE=RADIUS*2+1,BUFFER_RADIUS=RADIUS+5,MAX_JSON=768_000;
    public static final int MAX_LEAD=3;
    public TerrainView(String dimension,int centerX,int centerZ,int radius,double playerX,double playerZ,float yaw,List<Cell> cells){this(dimension,centerX,centerZ,radius,playerX,playerZ,yaw,cells,0);}
    public enum Status { READY, PENDING, UNAVAILABLE }
    public record Cell(int x,int z,Status status,int minHeight,int maxHeight,
            int samples,int buildingPieces,String biome,Set<TerrainProfile.Trait> traits,TerrainProfile.Source source,UUID owner,String ownerName,String mapPixels) {
        public Cell {traits=Set.copyOf(traits);if(mapPixels==null)mapPixels="";if(!mapPixels.isEmpty())new TerrainMapTile(mapPixels);}
    }
    public TerrainView {cells=List.copyOf(cells);}
    public TerrainView withPosition(double x,double z,float yaw){return new TerrainView(dimension,centerX,centerZ,radius,x,z,yaw,cells,requestId);}
    public TerrainView withRequestId(int id){return new TerrainView(dimension,centerX,centerZ,radius,playerX,playerZ,yaw,cells,id);}
    public static TerrainView snapshot(TerrainAtlas atlas,String dimension,int x,int z,Predicate<TerrainKey> loaded,Function<UUID,String> gangName){
        return snapshot(atlas,dimension,x,z,loaded,gangName,key->null);
    }
    public static TerrainView snapshot(TerrainAtlas atlas,String dimension,int x,int z,Predicate<TerrainKey> loaded,Function<UUID,String> gangName,Function<TerrainKey,TerrainMapTile> maps){
        return snapshot(atlas,dimension,x,z,loaded,gangName,maps,RADIUS);
    }
    public static TerrainView snapshot(TerrainAtlas atlas,String dimension,int x,int z,Predicate<TerrainKey> loaded,Function<UUID,String> gangName,Function<TerrainKey,TerrainMapTile> maps,int radius){
        if(radius<1||radius>BUFFER_RADIUS)throw new IllegalArgumentException("Nieprawidłowy zasięg mapy.");
        List<Cell> cells=new ArrayList<>();
        for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++){
            if(Math.abs((long)x+dx)>1_875_000||Math.abs((long)z+dz)>1_875_000){
                cells.add(new Cell(x+dx,z+dz,Status.UNAVAILABLE,0,0,0,0,"",Set.of(),null,null,"",""));continue;
            }
            TerrainKey key=new TerrainKey(dimension,x+dx,z+dz);TerrainProfile p=atlas.get(key);UUID owner=atlas.owner(key);
            TerrainMapTile tile=maps.apply(key);String pixels=tile==null?"":tile.pixels();
            cells.add(p==null?new Cell(key.x(),key.z(),loaded.test(key)?Status.PENDING:Status.UNAVAILABLE,0,0,0,0,"",Set.of(),null,null,"",pixels):
                new Cell(key.x(),key.z(),Status.READY,p.minHeight(),p.maxHeight(),p.sampleCount(),p.villagePieces().size(),
                    p.biomes().isEmpty()?"":p.biomes().getFirst(),p.traits(),p.source(),owner,owner==null?"":Objects.requireNonNullElse(gangName.apply(owner),"Nieznany gang"),pixels));
        }
        return new TerrainView(dimension,x,z,radius,x*16.0+8,z*16.0+8,0,cells);
    }
}
