package pl.crimeandlaw.territory;

import java.util.*;

/** Small client cache of server-supplied tiles; never generates missing terrain. */
public final class TerrainViewCache {
    public static final int LIMIT=1024;
    private String dimension="";
    private final Map<TerrainKey,TerrainView.Cell> cells=new LinkedHashMap<>(128,.75f,true);
    public TerrainView remember(TerrainView incoming){
        if(!dimension.equals(incoming.dimension())){clear();dimension=incoming.dimension();}
        List<TerrainView.Cell> merged=new ArrayList<>();
        for(var cell:incoming.cells()){
            if(Math.abs((long)cell.x())>TerrainViewport.LIMIT||Math.abs((long)cell.z())>TerrainViewport.LIMIT){merged.add(cell);continue;}
            var key=new TerrainKey(dimension,cell.x(),cell.z());var previous=cells.get(key);
            if(cell.mapPixels().isEmpty()&&previous!=null&&!previous.mapPixels().isEmpty())cell=new TerrainView.Cell(
                cell.x(),cell.z(),cell.status(),cell.minHeight(),cell.maxHeight(),cell.samples(),cell.buildingPieces(),
                cell.biome(),cell.traits(),cell.source(),cell.owner(),cell.ownerName(),previous.mapPixels());
            if(!cell.mapPixels().isEmpty()||cell.status()==TerrainView.Status.READY)cells.put(key,cell);
            merged.add(cell);
        }
        while(cells.size()>LIMIT)cells.remove(cells.keySet().iterator().next());
        return new TerrainView(incoming.dimension(),incoming.centerX(),incoming.centerZ(),incoming.radius(),incoming.playerX(),incoming.playerZ(),incoming.yaw(),merged);
    }
    public TerrainView viewport(TerrainView current,TerrainViewport target){
        if(!dimension.equals(current.dimension()))remember(current);
        List<TerrainView.Cell> visible=new ArrayList<>();
        for(int z=-TerrainView.RADIUS;z<=TerrainView.RADIUS;z++)for(int x=-TerrainView.RADIUS;x<=TerrainView.RADIUS;x++){
            int cx=target.x()+x,cz=target.z()+z;
            var cell=Math.abs((long)cx)>TerrainViewport.LIMIT||Math.abs((long)cz)>TerrainViewport.LIMIT?null:cells.get(new TerrainKey(dimension,cx,cz));
            visible.add(cell==null?new TerrainView.Cell(cx,cz,TerrainView.Status.UNAVAILABLE,0,0,0,0,"",Set.of(),null,null,"",""):cell);
        }
        return new TerrainView(dimension,target.x(),target.z(),TerrainView.RADIUS,current.playerX(),current.playerZ(),current.yaw(),visible);
    }
    public int size(){return cells.size();}
    public void clear(){cells.clear();dimension="";}
}
