package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.util.*;
import pl.crimeandlaw.gang.GangDirectory.*;

/** Bounded network views allow large gangs without a gameplay member cap. */
public final class GangView {
    public static final int PAGE_SIZE=6,HISTORY_PAGE_SIZE=100;
    private static final Gson JSON=new Gson();
    private GangView(){}
    private static int page(int requested,int size){return Math.clamp(requested,0,Math.max(0,(size-1)/PAGE_SIZE));}
    public static JsonObject snapshot(Gang source,UUID viewer,int memberPage,int requestPage){
        return snapshot(source,viewer,memberPage,requestPage,0);
    }
    public static JsonObject snapshot(Gang source,UUID viewer,int memberPage,int requestPage,int historyPage){
        return snapshot(source,viewer,memberPage,requestPage,historyPage,Set.of());
    }
    public static JsonObject snapshot(Gang source,UUID viewer,int memberPage,int requestPage,int historyPage,Set<UUID> online){
        if(!source.members.containsKey(viewer))throw new IllegalArgumentException("Nie należysz do tego gangu.");
        JsonObject result=new JsonObject(),gang=new JsonObject();
        gang.addProperty("id",source.id.toString());gang.addProperty("owner",source.owner.toString());
        gang.addProperty("name",source.name);gang.addProperty("logo",source.logo);gang.add("theme",JSON.toJsonTree(source.theme));
        gang.addProperty("mapColor",source.mapColor);
        gang.add("roles",JSON.toJsonTree(source.roles));gang.add("banner",JSON.toJsonTree(source.banner));
        int mp=page(memberPage,source.members.size());Map<UUID,Member> members=new LinkedHashMap<>();JsonArray ids=new JsonArray();
        source.members.entrySet().stream().skip((long)mp*PAGE_SIZE).limit(PAGE_SIZE).forEach(e->{members.put(e.getKey(),e.getValue());ids.add(e.getKey().toString());});
        members.put(source.owner,source.members.get(source.owner));members.put(viewer,source.members.get(viewer));
        gang.add("members",JSON.toJsonTree(members));result.add("memberIds",ids);
        JsonArray onlineIds=new JsonArray();for(UUID id:members.keySet())if(online.contains(id))onlineIds.add(id.toString());
        result.add("onlineMemberIds",onlineIds);result.addProperty("onlineCount",online.stream().filter(source.members::containsKey).count());
        result.addProperty("memberCount",source.members.size());result.addProperty("memberPage",mp);
        result.addProperty("memberPages",Math.max(1,(source.members.size()+PAGE_SIZE-1)/PAGE_SIZE));
        if(GangDirectory.allowed(source,viewer,Permission.INVITE)){
            int rp=page(requestPage,source.requests.size());Map<UUID,String> requests=new LinkedHashMap<>();
            source.requests.entrySet().stream().skip((long)rp*PAGE_SIZE).limit(PAGE_SIZE).forEach(e->requests.put(e.getKey(),e.getValue()));
            gang.add("requests",JSON.toJsonTree(requests));result.addProperty("requestCount",source.requests.size());
            result.addProperty("requestPage",rp);result.addProperty("requestPages",Math.max(1,(source.requests.size()+PAGE_SIZE-1)/PAGE_SIZE));
        }
        int hp=Math.clamp(historyPage,0,Math.max(0,(source.history.size()-1)/HISTORY_PAGE_SIZE));
        JsonArray events=new JsonArray();
        for(int i=hp*HISTORY_PAGE_SIZE;i<Math.min(source.history.size(),(hp+1)*HISTORY_PAGE_SIZE);i++)events.add(JSON.toJsonTree(source.history.get(source.history.size()-1-i)));
        result.add("history",events);result.addProperty("historyPage",hp);result.addProperty("historyPages",Math.max(1,(source.history.size()+HISTORY_PAGE_SIZE-1)/HISTORY_PAGE_SIZE));
        result.addProperty("historyCount",source.history.size());
        result.add("gang",gang);return result;
    }
}
