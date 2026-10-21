package pl.crimeandlaw.network;

import com.google.gson.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.crimeandlaw.gang.*;
import pl.crimeandlaw.gang.GangDirectory.*;

public final class GangNetwork {
    public static final int ACTION_MAX_JSON=GangLogo.MAX_ENCODED_LENGTH+1024;
    public static final int STATE_MAX_JSON=GangLogo.MAX_ENCODED_LENGTH+100_000;
    private static final Gson JSON=new Gson();
    public static Consumer<String> clientReceiver=ignored->{};
    public static Consumer<String> terrainReceiver=ignored->{};
    private static final Map<ServerPlayer,View> views=new WeakHashMap<>();
    private static final Map<ServerPlayer,Rate> rates=new WeakHashMap<>();
    private record View(String query,int page,int memberPage,int requestPage,int historyPage){}
    private static final class Rate {long second;int requests;}
    public record Action(String json) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("crimeandlaw","action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=new StreamCodec<>(){
            public Action decode(RegistryFriendlyByteBuf b){return new Action(b.readUtf(ACTION_MAX_JSON));}
            public void encode(RegistryFriendlyByteBuf b,Action p){b.writeUtf(p.json,ACTION_MAX_JSON);}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record State(String json) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("crimeandlaw","state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=new StreamCodec<>(){
            public State decode(RegistryFriendlyByteBuf b){return new State(b.readUtf(STATE_MAX_JSON));}
            public void encode(RegistryFriendlyByteBuf b,State p){b.writeUtf(p.json,STATE_MAX_JSON);}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Terrain(String json) implements CustomPacketPayload {
        public static final Type<Terrain> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("crimeandlaw","terrain"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Terrain> CODEC=new StreamCodec<>(){
            public Terrain decode(RegistryFriendlyByteBuf b){return new Terrain(b.readUtf(pl.crimeandlaw.territory.TerrainView.MAX_JSON));}
            public void encode(RegistryFriendlyByteBuf b,Terrain p){b.writeUtf(p.json,pl.crimeandlaw.territory.TerrainView.MAX_JSON);}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("7");
        registrar.playToServer(Action.TYPE,Action.CODEC,GangNetwork::handle);
        registrar.playToClient(State.TYPE,State.CODEC,(p,c)->clientReceiver.accept(p.json));
        registrar.playToClient(Terrain.TYPE,Terrain.CODEC,(p,c)->terrainReceiver.accept(p.json));
    }
    public static void send(JsonObject action){PacketDistributor.sendToServer(new Action(JSON.toJson(action)));}
    private static void handle(Action packet,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        Rate rate=rates.computeIfAbsent(player,p->new Rate());long second=System.nanoTime()/1_000_000_000L;
        if(rate.second!=second){rate.second=second;rate.requests=0;}
        if(++rate.requests>12)return;
        GangSavedData saved=GangSavedData.get(player.server);GangDirectory d=saved.directory;UUID actor=player.getUUID();
        try{
            JsonObject a=JsonParser.parseString(packet.json).getAsJsonObject();String action=text(a,"action");
            if(action.equals("terrain_view")){
                if(d.of(actor)==null)throw new IllegalArgumentException("Najpierw dołącz do gangu.");
                PacketDistributor.sendToPlayer(player,new Terrain(JSON.toJson(pl.crimeandlaw.territory.TerrainAnalysis.view(player))));return;
            }
            if(action.equals("refresh")){
                String query=text(a,"query");if(query.length()>32)query=query.substring(0,32);
                View old=views.getOrDefault(player,new View("",0,0,0,0));
                views.put(player,new View(query,Math.max(0,integer(a,"page",0)),old.memberPage,old.requestPage,old.historyPage));
                sendState(player,"");return;
            }
            if(action.equals("list_page")){
                View old=views.getOrDefault(player,new View("",0,0,0,0));
                views.put(player,new View(old.query,old.page,Math.max(0,integer(a,"memberPage",old.memberPage)),Math.max(0,integer(a,"requestPage",old.requestPage)),Math.max(0,integer(a,"historyPage",old.historyPage))));
                sendState(player,"");return;
            }
            switch(action){
                case "create" -> d.create(actor,player.getGameProfile().getName(),text(a,"name"),GangLogo.validate(text(a,"logo")));
                case "request" -> d.request(actor,player.getGameProfile().getName(),uuid(a,"id"));
                case "accept" -> d.accept(actor,uuid(a,"target"));
                case "reject" -> d.reject(actor,uuid(a,"target"));
                case "leave" -> d.leave(actor);
                case "kick" -> d.kick(actor,uuid(a,"target"));
                case "transfer" -> d.transfer(actor,uuid(a,"target"));
                case "role" -> {
                    Set<Permission> permissions=EnumSet.noneOf(Permission.class);
                    for(Permission p:Permission.values())if(a.has(p.name())&&a.get(p.name()).getAsBoolean())permissions.add(p);
                    d.role(actor,text(a,"role"),text(a,"name"),permissions);
                }
                case "assign" -> d.assign(actor,uuid(a,"target"),text(a,"role"));
                case "reorder_roles" -> {
                    List<String> order=new ArrayList<>();for(JsonElement id:a.getAsJsonArray("order"))order.add(id.getAsString());d.reorderRoles(actor,order);
                }
                case "theme" -> d.theme(actor,new Theme(integer(a,"background",Theme.DEFAULT.background()),integer(a,"border",Theme.DEFAULT.border()),integer(a,"opacity",235)));
                case "logo" -> d.logo(actor,GangLogo.validate(text(a,"logo")));
                default -> throw new IllegalArgumentException("Nieznana czynność.");
            }
            saved.setDirty();
            // Every viewer receives authoritative membership/permissions, including kicked members.
            for(ServerPlayer online:player.server.getPlayerList().getPlayers()){
                if(online==player || views.containsKey(online))sendState(online,online==player?"Zapisano.":"");
            }
        }catch(IllegalArgumentException exception){sendState(player,exception.getMessage()==null?"Nieprawidłowe dane.":exception.getMessage());}
        catch(RuntimeException exception){sendState(player,"Nie udało się wykonać czynności. Sprawdź dane i spróbuj ponownie.");}
    }
    public static void sendState(ServerPlayer player,String message){
        GangDirectory d=GangSavedData.get(player.server).directory;
        View view=views.computeIfAbsent(player,p->new View("",0,0,0,0));
        JsonObject result=new JsonObject();result.addProperty("player",player.getUUID().toString());result.addProperty("message",message);
        Gang current=d.of(player.getUUID());
        if(current!=null){
            JsonObject snapshot=GangView.snapshot(current,player.getUUID(),view.memberPage,view.requestPage,view.historyPage);
            for(var entry:snapshot.entrySet())result.add(entry.getKey(),entry.getValue());
        }
        List<Gang> matches=d.gangs().stream().filter(g->g.name.toLowerCase(Locale.ROOT).contains(view.query.toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(g->g.name.toLowerCase(Locale.ROOT))).toList();
        int page=Math.min(view.page,Math.max(0,(matches.size()-1)/12));
        JsonArray rows=new JsonArray();
        for(Gang g:matches.stream().skip(page*12L).limit(12).toList()){
            JsonObject row=new JsonObject();row.addProperty("id",g.id.toString());row.addProperty("name",g.name);
            row.addProperty("members",g.members.size());row.addProperty("requested",g.requests.containsKey(player.getUUID()));rows.add(row);
        }
        result.add("gangs",rows);result.addProperty("page",page);result.addProperty("pages",Math.max(1,(matches.size()+11)/12));
        PacketDistributor.sendToPlayer(player,new State(JSON.toJson(result)));
    }
    private static String text(JsonObject a,String key){return a.has(key)&&!a.get(key).isJsonNull()?a.get(key).getAsString():"";}
    private static int integer(JsonObject a,String key,int fallback){return a.has(key)?a.get(key).getAsInt():fallback;}
    private static UUID uuid(JsonObject a,String key){return UUID.fromString(text(a,key));}
}
