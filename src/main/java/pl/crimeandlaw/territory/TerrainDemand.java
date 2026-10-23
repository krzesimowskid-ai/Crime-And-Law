package pl.crimeandlaw.territory;

import java.util.*;

/** Bounded current viewing areas; obsolete camera positions do not retain work. */
public final class TerrainDemand {
    public static final int MAX_VIEWERS=64,MAX_JOBS=1024,TTL=100;
    private record Area(String dimension,TerrainViewport visible,TerrainViewport buffer,int radius,long tick){}
    private final LinkedHashMap<UUID,Area> areas=new LinkedHashMap<>();
    public void update(UUID viewer,String dimension,TerrainViewport visible,TerrainViewport buffer,int radius,long tick){
        if(radius<1||radius>TerrainView.BUFFER_RADIUS)throw new IllegalArgumentException("Nieprawidłowy zasięg mapy.");
        expire(tick);areas.remove(viewer);areas.put(viewer,new Area(dimension,visible,buffer,radius,tick));
        while(areas.size()>MAX_VIEWERS)areas.pollFirstEntry();
    }
    public void expire(long tick){areas.values().removeIf(a->tick-a.tick()>TTL);}
    public int distance(TerrainKey key){
        int best=Integer.MAX_VALUE;
        for(var area:areas.values())if(area.dimension().equals(key.dimension())&&area.buffer().near(key.x(),key.z(),area.radius())){
            int distance=(int)Math.max(Math.abs((long)key.x()-area.visible().x()),Math.abs((long)key.z()-area.visible().z()));
            best=Math.min(best,distance);
        }
        return best;
    }
    public int size(){return areas.size();}
}
