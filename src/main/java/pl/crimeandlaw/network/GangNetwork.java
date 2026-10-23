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
    public static final int STATE_MAX_JSON=2*GangLogo.MAX_ENCODED_LENGTH+200_000;
    private static final Gson JSON=new Gson();
    public static Consumer<String> clientReceiver=ignored->{};
    public static Consumer<String> terrainReceiver=ignored->{};
    private static final Map<ServerPlayer,View> views=new WeakHashMap<>();
    private static final Map<ServerPlayer,Rate> rates=new WeakHashMap<>();
    public static final int ACTION_MAX_BYTES=ACTION_MAX_JSON*3,STATE_MAX_BYTES=STATE_MAX_JSON*3;
    private static final Map<ServerPlayer,MessageChunks.Assembler> uploads=new WeakHashMap<>();
    private static final MessageChunks.Assembler download=new MessageChunks.Assembler();
    public static void expireUploads(){long now=System.nanoTime();uploads.entrySet().removeIf(entry->entry.getValue().expire(now));}
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
    private static MessageChunks.Part readPart(RegistryFriendlyByteBuf b){return new MessageChunks.Part(b.readUUID(),b.readVarInt(),b.readVarInt(),b.readByteArray(MessageChunks.PART_BYTES));}
    private static void writePart(RegistryFriendlyByteBuf b,MessageChunks.Part part){b.writeUUID(part.id());b.writeVarInt(part.length());b.writeVarInt(part.offset());b.writeByteArray(part.data());}
    public record ActionChunk(MessageChunks.Part part) implements CustomPacketPayload {
        public static final Type<ActionChunk> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("crimeandlaw","action_chunk"));
        public static final StreamCodec<RegistryFriendlyByteBuf,ActionChunk> CODEC=new StreamCodec<>(){
            public ActionChunk decode(RegistryFriendlyByteBuf b){return new ActionChunk(readPart(b));}
            public void encode(RegistryFriendlyByteBuf b,ActionChunk packet){writePart(b,packet.part());}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record StateChunk(MessageChunks.Part part) implements CustomPacketPayload {
        public static final Type<StateChunk> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("crimeandlaw","state_chunk"));
        public static final StreamCodec<RegistryFriendlyByteBuf,StateChunk> CODEC=new StreamCodec<>(){
            public StateChunk decode(RegistryFriendlyByteBuf b){return new StateChunk(readPart(b));}
            public void encode(RegistryFriendlyByteBuf b,StateChunk packet){writePart(b,packet.part());}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("12");
        registrar.playToServer(Action.TYPE,Action.CODEC,GangNetwork::handle);
        registrar.playToClient(State.TYPE,State.CODEC,(p,c)->clientReceiver.accept(p.json));
        registrar.playToClient(Terrain.TYPE,Terrain.CODEC,(p,c)->terrainReceiver.accept(p.json));
        registrar.playToServer(ActionChunk.TYPE,ActionChunk.CODEC,GangNetwork::upload);
        registrar.playToClient(StateChunk.TYPE,StateChunk.CODEC,(p,c)->{
            String complete=download.accept(p.part(),STATE_MAX_BYTES,System.nanoTime());
            if(complete!=null){if(complete.length()>STATE_MAX_JSON)throw new IllegalArgumentException("Wiadomość przekracza limit.");clientReceiver.accept(complete);}
        });
    }
    public static void clearClientTransfers(){download.clear();}
    public static void send(JsonObject action){
        String json=JSON.toJson(action);
        if(json.length()>MessageChunks.PART_BYTES/3){for(var part:MessageChunks.split(json,ACTION_MAX_BYTES))PacketDistributor.sendToServer(new ActionChunk(part));}
        else PacketDistributor.sendToServer(new Action(json));
    }
    private static void upload(ActionChunk packet,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        MessageChunks.Assembler assembler;
        if(packet.part().offset()==0){
            if(!allow(player)){uploads.remove(player);return;}
            assembler=uploads.computeIfAbsent(player,p->new MessageChunks.Assembler());
        }else{
            assembler=uploads.get(player);if(assembler==null)return;
        }
        try{
            String complete=assembler.accept(packet.part(),ACTION_MAX_BYTES,System.nanoTime());
            if(complete!=null){if(complete.length()>ACTION_MAX_JSON)throw new IllegalArgumentException("Wiadomość przekracza limit.");handleAction(new Action(complete),context);}
        }catch(IllegalArgumentException error){uploads.remove(player);sendState(player,error.getMessage());}
    }
    private static boolean allow(ServerPlayer player){
        Rate rate=rates.computeIfAbsent(player,p->new Rate());long second=System.nanoTime()/1_000_000_000L;
        if(rate.second!=second){rate.second=second;rate.requests=0;}
        return ++rate.requests<=12;
    }
    private static void handle(Action packet,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        if(allow(player))handleAction(packet,context);
    }
    private static void handleAction(Action packet,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        GangSavedData saved=GangSavedData.get(player.server);GangDirectory d=saved.directory;UUID actor=player.getUUID();
        try{
            JsonObject a=JsonParser.parseString(packet.json).getAsJsonObject();String action=text(a,"action");
            if(action.equals("terrain_view")){
                if(d.of(actor)==null)throw new IllegalArgumentException("Najpierw dołącz do gangu.");
                boolean remote=a.has("centerX")||a.has("centerZ");
                var viewport=remote?new pl.crimeandlaw.territory.TerrainViewport(exactInteger(a,"centerX"),exactInteger(a,"centerZ")):
                    new pl.crimeandlaw.territory.TerrainViewport(player.chunkPosition().x,player.chunkPosition().z);
                PacketDistributor.sendToPlayer(player,new Terrain(JSON.toJson(pl.crimeandlaw.territory.TerrainAnalysis.view(player,viewport))));return;
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
                case "banner" -> d.banner(actor,new GangBanner(text(a,"image"),integer(a,"color",0x15161C),integer(a,"opacity",255)));
                case "logo" -> d.logo(actor,GangLogo.validate(text(a,"logo")));
                default -> throw new IllegalArgumentException("Nieznana czynność.");
            }
            rememberProfile(player);
            if(action.equals("accept")){var accepted=player.server.getPlayerList().getPlayer(uuid(a,"target"));if(accepted!=null)rememberProfile(accepted);}
            saved.setDirty();
            // Every viewer receives authoritative membership/permissions, including kicked members.
            for(ServerPlayer online:player.server.getPlayerList().getPlayers()){
                if(online==player || views.containsKey(online))sendState(online,online==player?"Zapisano.":"");
            }
        }catch(IllegalArgumentException exception){sendState(player,exception.getMessage()==null?"Nieprawidłowe dane.":exception.getMessage());}
        catch(RuntimeException exception){sendState(player,"Nie udało się wykonać czynności. Sprawdź dane i spróbuj ponownie.");}
    }
    public static void sendState(ServerPlayer player,String message){
        sendState(player,message,null);
    }
    private static void sendState(ServerPlayer player,String message,UUID disconnected){
        GangDirectory d=GangSavedData.get(player.server).directory;
        View view=views.computeIfAbsent(player,p->new View("",0,0,0,0));
        JsonObject result=new JsonObject();result.addProperty("player",player.getUUID().toString());result.addProperty("message",message);
        Gang current=d.of(player.getUUID());
        if(current!=null){
            JsonObject snapshot=GangView.snapshot(current,player.getUUID(),view.memberPage,view.requestPage,view.historyPage,
                player.server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).filter(id->!id.equals(disconnected)).collect(java.util.stream.Collectors.toSet()));
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
        String json=JSON.toJson(result);
        if(json.length()>MessageChunks.PART_BYTES/3){for(var part:MessageChunks.split(json,STATE_MAX_BYTES))PacketDistributor.sendToPlayer(player,new StateChunk(part));}
        else PacketDistributor.sendToPlayer(player,new State(json));
    }
    public static void rememberProfile(ServerPlayer player){
        var property=player.getGameProfile().getProperties().get("textures").stream().findFirst().orElse(null);
        var saved=GangSavedData.get(player.server);
        if(saved.directory.rememberProfile(player.getUUID(),player.getGameProfile().getName(),property==null?"":property.value(),property==null||property.signature()==null?"":property.signature()))saved.setDirty();
    }
    public static void presenceChanged(ServerPlayer player,boolean joined){
        if(!joined){uploads.remove(player);rates.remove(player);views.remove(player);}
        if(joined)rememberProfile(player);
        var gang=GangSavedData.get(player.server).directory.of(player.getUUID());if(gang==null)return;
        for(var viewer:player.server.getPlayerList().getPlayers())if(viewer!=player&&gang.members.containsKey(viewer.getUUID())&&views.containsKey(viewer))sendState(viewer,"",joined?null:player.getUUID());
    }
    private static String text(JsonObject a,String key){return a.has(key)&&!a.get(key).isJsonNull()?a.get(key).getAsString():"";}
    private static int integer(JsonObject a,String key,int fallback){return a.has(key)?a.get(key).getAsInt():fallback;}
    private static int exactInteger(JsonObject a,String key){
        if(!a.has(key)||!a.get(key).isJsonPrimitive()||!a.getAsJsonPrimitive(key).isNumber())
            throw new IllegalArgumentException("Nieprawidłowa pozycja mapy.");
        try{return a.get(key).getAsBigDecimal().intValueExact();}
        catch(ArithmeticException exception){throw new IllegalArgumentException("Nieprawidłowa pozycja mapy.");}
    }
    private static UUID uuid(JsonObject a,String key){return UUID.fromString(text(a,key));}
}
