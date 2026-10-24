package pl.crimeandlaw.territory;

import com.google.gson.Gson;
import java.util.*;

/** Explicit notes, never automatically discovered hidden territory. */
public final class TerrainMarkers {
    public static final int PER_SCOPE=256,MAX_TOTAL=32768;
    private static final Gson JSON=new Gson();
    public record Mark(TerrainKey key,UUID scope,boolean shared,int color){
        public Mark {Objects.requireNonNull(key);Objects.requireNonNull(scope);color&=0xFFFFFF;}
        String id(){return (shared?"g":"p")+scope+"|"+key.storageKey();}
    }
    private final Map<String,Mark> marks=new LinkedHashMap<>();
    public void set(TerrainKey key,UUID player,UUID gang,boolean shared,int color){
        UUID scope=shared?gang:player;if(scope==null)throw new IllegalArgumentException("Najpierw dołącz do gangu.");
        var mark=new Mark(key,scope,shared,color);
        if(!marks.containsKey(mark.id())&&(marks.size()>=MAX_TOTAL||marks.values().stream().filter(m->m.shared==shared&&m.scope.equals(scope)).count()>=PER_SCOPE))
            throw new IllegalArgumentException("Osiągnięto limit 256 oznaczeń dla tego właściciela.");
        marks.put(mark.id(),mark);
    }
    public void remove(TerrainKey key,UUID player,UUID gang,boolean shared){
        UUID scope=shared?gang:player;if(scope!=null)marks.remove(new Mark(key,scope,shared,0).id());
    }
    public List<Mark> visible(UUID player,UUID gang,String dimension,TerrainViewport viewport){
        return marks.values().stream().filter(m->m.shared?m.scope.equals(gang):m.scope.equals(player))
            .filter(m->m.key.dimension().equals(dimension)&&Math.abs((long)m.key.x()-viewport.x())<=TerrainView.BUFFER_RADIUS&&Math.abs((long)m.key.z()-viewport.z())<=TerrainView.BUFFER_RADIUS).toList();
    }
    public String toJson(){return JSON.toJson(this);}
    public static TerrainMarkers fromJson(String value){
        var result=JSON.fromJson(value,TerrainMarkers.class);
        if(result==null||result.marks==null||result.marks.size()>MAX_TOTAL)throw new IllegalArgumentException("Nieprawidłowe oznaczenia mapy.");
        Map<String,Integer> counts=new HashMap<>();
        for(var entry:result.marks.entrySet()){
            var mark=entry.getValue();if(mark==null||!entry.getKey().equals(mark.id())||counts.merge((mark.shared?"g":"p")+mark.scope,1,Integer::sum)>PER_SCOPE)
                throw new IllegalArgumentException("Nieprawidłowe oznaczenie mapy.");
        }return result;
    }
}
