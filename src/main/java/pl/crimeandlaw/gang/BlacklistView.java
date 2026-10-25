package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.util.*;

public final class BlacklistView {
    private static final Gson JSON=new Gson();
    public record Query(boolean history,String search,int page){
        public static final Query DEFAULT=new Query(false,"",0);
        public Query {if(search==null||search.length()>64)throw new IllegalArgumentException("Zbyt długie wyszukiwanie.");page=Math.max(0,page);}
        public Query update(JsonObject request){return new Query(pl.crimeandlaw.network.ActionFields.flag(request,"history",history),pl.crimeandlaw.network.ActionFields.text(request,"search",search),pl.crimeandlaw.network.ActionFields.integer(request,"page",page));}
    }
    private BlacklistView(){}
    public static JsonObject snapshot(GangDirectory directory,UUID viewer,Query query){
        var gang=directory.of(viewer);if(gang==null)throw new IllegalArgumentException("Najpierw dołącz do gangu.");
        var rows=new ArrayList<JsonObject>();String search=query.search().toLowerCase(Locale.ROOT);
        if(query.history())for(var event:gang.blacklist.history()){
            var row=JSON.toJsonTree(event.entry()).getAsJsonObject();row.addProperty("added",event.added());row.addProperty("actorName",event.actorName());row.addProperty("timestamp",event.timestamp());rows.add(row);
        }else for(var entry:gang.blacklist.entries())rows.add(JSON.toJsonTree(entry).getAsJsonObject());
        rows.removeIf(row->!row.toString().toLowerCase(Locale.ROOT).contains(search));
        rows.sort(Comparator.comparingLong((JsonObject row)->row.get("timestamp").getAsLong()).reversed());
        int page=Math.clamp(query.page(),0,Math.max(0,(rows.size()-1)/100));var entries=new JsonArray();rows.stream().skip(page*100L).limit(100).forEach(entries::add);
        var result=new JsonObject();result.addProperty("history",query.history());result.addProperty("page",page);result.addProperty("pages",Math.max(1,(rows.size()+99)/100));result.addProperty("count",rows.size());result.add("entries",entries);return result;
    }
}
