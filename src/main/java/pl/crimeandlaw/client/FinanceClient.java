package pl.crimeandlaw.client;

import com.google.gson.JsonObject;

final class FinanceClient {
    private FinanceClient(){}
    static JsonObject data(){return ClientState.state.has("finances")?ClientState.state.getAsJsonObject("finances"):new JsonObject();}
    static long money(String key){return ClientState.state.has(key)?ClientState.state.get(key).getAsLong():0;}
    static String token(){return ClientState.state.has("financeToken")?ClientState.state.get("financeToken").getAsString():"";}
    static int number(String key,int fallback){var data=data();return data.has(key)?data.get(key).getAsInt():fallback;}
    static void view(String kind,int page,String search,String filter){
        var request=new JsonObject();request.addProperty("kind",kind);request.addProperty("financePage",page);request.addProperty("search",search);request.addProperty("filter",filter);ClientState.action("finance_view",request);
    }
    static void candidates(String query,int page){
        var request=new JsonObject();request.addProperty("candidateQuery",query);request.addProperty("candidatePage",page);ClientState.action("finance_view",request);
    }
}
