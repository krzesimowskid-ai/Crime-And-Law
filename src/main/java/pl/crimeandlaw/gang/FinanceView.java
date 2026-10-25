package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.util.*;
import pl.crimeandlaw.gang.GangFinances.*;

/** Bounded, server-filtered ledger and recipient directory; never other players' wallets. */
public final class FinanceView {
    private static final Gson JSON=new Gson();
    public static final int PAGE_SIZE=100,CANDIDATE_PAGE_SIZE=6;
    public static final List<String> FILTERS=List.of("all","actor","recipient","type","comment");
    public record Query(String kind,int page,String search,String filter,String candidateQuery,int candidatePage){
        public static final Query DEFAULT=new Query("balance",0,"","all","",0);
        public Query {
            if(!Set.of("balance","bonuses").contains(kind)||!FILTERS.contains(filter)||search==null||search.length()>64||candidateQuery==null||candidateQuery.length()>32)
                throw new IllegalArgumentException("Nieprawidłowy filtr finansów.");
            page=Math.max(0,page);candidatePage=Math.max(0,candidatePage);
        }
        public Query update(JsonObject request){
            return new Query(string(request,"kind",kind),number(request,"financePage",page),string(request,"search",search),string(request,"filter",filter),string(request,"candidateQuery",candidateQuery),number(request,"candidatePage",candidatePage));
        }
    }
    private FinanceView(){}
    public static JsonObject snapshot(GangDirectory directory,UUID viewer,Query query){
        var gang=directory.of(viewer);if(gang==null)throw new IllegalArgumentException("Najpierw dołącz do gangu.");
        String search=query.search().strip().toLowerCase(Locale.ROOT);
        var entries=directory.finances.history(gang.id).stream().filter(e->!query.kind().equals("bonuses")||e.kind()==Kind.BONUS)
            .filter(e->matches(e,query.filter(),search)).toList();
        int page=Math.clamp(query.page(),0,Math.max(0,(entries.size()-1)/PAGE_SIZE));
        JsonArray rows=new JsonArray();
        for(int i=page*PAGE_SIZE;i<Math.min(entries.size(),(page+1)*PAGE_SIZE);i++)rows.add(JSON.toJsonTree(entries.get(entries.size()-1-i)));
        var result=new JsonObject();result.addProperty("kind",query.kind());result.addProperty("page",page);
        result.addProperty("pages",Math.max(1,(entries.size()+PAGE_SIZE-1)/PAGE_SIZE));result.addProperty("count",entries.size());result.add("entries",rows);
        if(GangDirectory.allowed(gang,viewer,GangDirectory.Permission.PAY_BONUSES)){
            String names=query.candidateQuery().strip().toLowerCase(Locale.ROOT);
            var members=gang.members.values().stream().filter(m->!m.id.equals(viewer)&&m.name.toLowerCase(Locale.ROOT).contains(names))
                .sorted(Comparator.comparing((GangDirectory.Member m)->m.name.toLowerCase(Locale.ROOT)).thenComparing(m->m.id)).toList();
            int cp=Math.clamp(query.candidatePage(),0,Math.max(0,(members.size()-1)/CANDIDATE_PAGE_SIZE));
            var candidates=new JsonArray();
            for(var member:members.stream().skip((long)cp*CANDIDATE_PAGE_SIZE).limit(CANDIDATE_PAGE_SIZE).toList()){
                var row=new JsonObject();row.addProperty("id",member.id.toString());row.addProperty("name",member.name);row.addProperty("role",gang.roles.get(member.role).name);candidates.add(row);
            }
            result.add("candidates",candidates);result.addProperty("candidatePage",cp);result.addProperty("candidatePages",Math.max(1,(members.size()+CANDIDATE_PAGE_SIZE-1)/CANDIDATE_PAGE_SIZE));
        }
        return result;
    }
    private static boolean matches(Entry e,String filter,String query){
        if(query.isEmpty())return true;
        String value=switch(filter){case "actor"->e.actorName();case "recipient"->e.recipientName();case "type"->e.kind().label;case "comment"->e.comment();
            default->e.actorName()+" "+e.recipientName()+" "+e.actorRole()+" "+e.recipientRole()+" "+e.kind().label+" "+e.comment()+" "+e.amount();};
        return value.toLowerCase(Locale.ROOT).contains(query);
    }
    private static String string(JsonObject request,String key,String fallback){return pl.crimeandlaw.network.ActionFields.text(request,key,fallback);}
    private static int number(JsonObject request,String key,int fallback){
        return pl.crimeandlaw.network.ActionFields.integer(request,key,fallback);
    }
}
