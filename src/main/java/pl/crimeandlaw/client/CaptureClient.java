package pl.crimeandlaw.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;

/** Small event updates are independent of surface rasters and large gang graphics. */
final class CaptureClient {
    static JsonObject state=new JsonObject();
    private static int selectedX,selectedZ;
    private static boolean selected;
    private static long requested;
    private static String dimension="";
    private static final Map<Long,List<JsonObject>> markers=new HashMap<>();
    private static final Set<Long> events=new HashSet<>();
    private CaptureClient(){}
    static void select(int x,int z){
        var player=Minecraft.getInstance().player;if(player==null)return;
        String current=player.level().dimension().location().toString();
        boolean changed=!selected||selectedX!=x||selectedZ!=z||!dimension.equals(current);
        selected=true;selectedX=x;selectedZ=z;dimension=current;if(changed){requested=0;request();}
    }
    static void request(){
        long now=System.nanoTime();if(!selected||now-requested<500_000_000L)return;requested=now;
        action("capture_view");
    }
    static void action(String name){
        JsonObject args=point(selectedX,selectedZ);
        if(name.equals("capture_view")){args.addProperty("centerX",(int)Math.round(TerrainClient.cameraX()));args.addProperty("centerZ",(int)Math.round(TerrainClient.cameraZ()));}
        ClientState.action(name,args);
    }
    private static JsonObject point(int x,int z){var args=new JsonObject();args.addProperty("x",x);args.addProperty("z",z);return args;}
    static void mark(int x,int z,boolean shared,Integer color){
        var args=point(x,z);args.addProperty("shared",shared);if(color!=null)args.addProperty("color",color);
        ClientState.action(color==null?"marker_remove":"marker_set",args);
    }
    static void receive(String value){
        var next=JsonParser.parseString(value).getAsJsonObject();var player=Minecraft.getInstance().player;
        if(player==null||!next.get("dimension").getAsString().equals(player.level().dimension().location().toString()))return;
        if(next.has("message")&&!next.get("message").getAsString().isEmpty())ClientState.message=next.get("message").getAsString();
        state=next;markers.clear();events.clear();
        if(next.has("markers"))for(var element:next.getAsJsonArray("markers")){var row=element.getAsJsonObject();markers.computeIfAbsent(position(row.get("x").getAsInt(),row.get("z").getAsInt()),k->new ArrayList<>()).add(row);}
        if(next.has("events"))for(var element:next.getAsJsonArray("events")){var row=element.getAsJsonObject();if(row.get("dimension").getAsString().equals(player.level().dimension().location().toString()))events.add(position(row.get("x").getAsInt(),row.get("z").getAsInt()));}
        if(Minecraft.getInstance().screen instanceof GangScreen screen)screen.captureUpdated();
    }
    private static long position(int x,int z){return (long)x<<32|z&0xFFFFFFFFL;}
    static boolean currentDimension(){var player=Minecraft.getInstance().player;return player!=null&&state.has("dimension")&&state.get("dimension").getAsString().equals(player.level().dimension().location().toString());}
    static List<JsonObject> marks(int x,int z){return currentDimension()?markers.getOrDefault(position(x,z),List.of()):List.of();}
    static boolean event(int x,int z){return currentDimension()&&events.contains(position(x,z));}
    static JsonObject ownBattle(){return currentDimension()&&state.has("ownBattle")?state.getAsJsonObject("ownBattle"):null;}
    static boolean matches(int x,int z){return currentDimension()&&state.has("x")&&state.get("x").getAsInt()==x&&state.get("z").getAsInt()==z;}
    static boolean flag(String key){return state.has(key)&&state.get(key).getAsBoolean();}
    static long number(String key){return state.has(key)?state.get(key).getAsLong():0;}
    static String text(String key){return state.has(key)?state.get(key).getAsString():"";}
    static String phase(){return !state.has("battle")?"":switch(state.getAsJsonObject("battle").get("phase").getAsString()){
        case "CLAIMING"->"Przejmowanie";case "PREPARING"->"Przygotowanie PvP";default->"Walka PvP";};}
    static String duration(long seconds){return String.format(Locale.ROOT,"%d:%02d",Math.max(0,seconds)/60,Math.max(0,seconds)%60);}
    static void clear(){state=new JsonObject();selected=false;requested=0;dimension="";markers.clear();events.clear();}
}
