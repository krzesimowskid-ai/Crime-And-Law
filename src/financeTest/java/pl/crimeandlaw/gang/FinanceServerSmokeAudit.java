package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.crimeandlaw.network.GangNetwork;
import pl.crimeandlaw.gang.GangDirectory.Permission;

/** Extra adversarial checks on the real server thread with both TCP clients connected. */
final class FinanceServerSmokeAudit {
    private static final UUID OWNER=id("Owner29"),MEMBER=id("Member29"),FRIEND=id("Friend29");
    private static int stage;
    private static CompletableFuture<Void> simultaneous;
    private static UUID id(String name){return UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    static boolean run(MinecraftServer server,Path out)throws Exception{
        check(server.isSameThread(),"Mutations stay on the server thread");
        var d=GangSavedData.get(server).directory;var owner=server.getPlayerList().getPlayer(OWNER);var member=server.getPlayerList().getPlayer(MEMBER);
        check(owner!=null&&member!=null,"Both real TCP sessions remain connected");var gang=d.of(OWNER);
        if(stage==0){
            var stolen=transaction("finance_withdraw",token(owner),1);stolen.addProperty("actor",OWNER.toString());action(member,stolen);
            check(d.finances.bank(MEMBER)==500&&d.finances.balance(gang.id)==79700,"Foreign session nonce and forged actor are rejected");
            action(member,transaction("finance_withdraw",token(member),1));check(d.finances.bank(MEMBER)==500,"Own nonce cannot bypass withdrawal permission");
            d.role(OWNER,"member","Kadry",Set.of(Permission.DEPOSIT_FUNDS,Permission.MANAGE_ROLES,Permission.ASSIGN_ROLES));
            Set<String> existing=new HashSet<>(gang.roles.keySet());d.role(OWNER,"","Finanse audytu",Set.of(Permission.WITHDRAW_FUNDS));
            String protectedRole=gang.roles.keySet().stream().filter(r->!existing.contains(r)).findFirst().orElseThrow();
            var grant=new JsonObject();grant.addProperty("action","role");grant.addProperty("role","member");grant.addProperty("name","Kadry");grant.addProperty("WITHDRAW_FUNDS",true);action(member,grant);
            check(!GangDirectory.allowed(gang,MEMBER,Permission.WITHDRAW_FUNDS),"Raw role edit cannot acquire financial rights");
            var assign=new JsonObject();assign.addProperty("action","assign");assign.addProperty("target",MEMBER.toString());assign.addProperty("role",protectedRole);action(member,assign);
            check(gang.members.get(MEMBER).role.equals("member"),"Raw self-assignment cannot acquire financial rights");
            gang.roles.remove(protectedRole);d.role(OWNER,"member","Członek",Set.of(Permission.DEPOSIT_FUNDS));
            String oldGangToken=token(member);d.leave(MEMBER);var temporary=d.create(MEMBER,"Member29","Przejściowy gang audytu","");
            action(member,transaction("finance_deposit",oldGangToken,1));
            check(d.finances.bank(MEMBER)==500&&d.finances.balance(temporary.id)==0,"An old gang token cannot charge a new gang");
            d.leave(MEMBER);d.request(MEMBER,"Member29",gang.id);d.accept(OWNER,MEMBER);GangNetwork.sendState(member,"");
            String paid=token(owner);var withdraw=transaction("finance_withdraw",paid,1);action(owner,withdraw);action(owner,withdraw);
            check(d.finances.bank(OWNER)==19501&&d.finances.balance(gang.id)==79699,"Same request executes once");
            action(owner,transaction("finance_deposit",token(owner),1));
            String unchanged=d.toJson();
            for(String json:List.of("{\"action\":"+"[".repeat(10000)+"\"refresh\""+"]".repeat(10000)+"}",
                    "{\"action\":\"finance_view\",\"search\":[[\"test\"]]}",
                    "{\"action\":\"blacklist_view\",\"history\":[[true]]}",
                    "{\"action\":\"theme\",\"background\":[[1]]}",
                    "{\"action\":\"reorder_roles\",\"order\":[[[\"member\"]]]}"))raw(owner,json);
            check(d.toJson().equals(unchanged),"Malformed and deep scalar fields neither crash nor mutate the world");
            JsonObject invalid=transaction("finance_pay",token(owner),1);invalid.remove("amount");var payments=new JsonArray();
            for(UUID recipient:List.of(MEMBER,UUID.randomUUID())){var row=new JsonObject();row.addProperty("recipient",recipient.toString());row.addProperty("amount",1);payments.add(row);}invalid.add("payments",payments);
            action(owner,invalid);check(d.finances.bank(MEMBER)==500&&d.finances.balance(gang.id)==79700,"Invalid last recipient rejects the entire batch");
            var a=transaction("finance_deposit",token(owner),1);var b=transaction("finance_deposit",token(member),1);
            // Separate producers queue both actions concurrently; the game thread commits them in order.
            var first=CompletableFuture.supplyAsync(()->server.submit(()->unchecked(owner,a))).thenCompose(f->f);
            var second=CompletableFuture.supplyAsync(()->server.submit(()->unchecked(member,b))).thenCompose(f->f);
            simultaneous=CompletableFuture.allOf(first,second);stage=1;return false;
        }
        if(stage==1){
            if(!simultaneous.isDone())return false;simultaneous.join();
            check(d.finances.bank(OWNER)==19499&&d.finances.bank(MEMBER)==499&&d.finances.balance(gang.id)==79702,"Concurrent independent deposits conserve money");
            action(owner,transaction("finance_withdraw",token(owner),1));
            var payment=transaction("finance_pay",token(owner),1);payment.remove("amount");var rows=new JsonArray();var row=new JsonObject();row.addProperty("recipient",MEMBER.toString());row.addProperty("amount",1);rows.add(row);payment.add("payments",rows);action(owner,payment);
            check(d.finances.bank(OWNER)+d.finances.bank(MEMBER)+d.finances.bank(FRIEND)+d.finances.balance(gang.id)==100000,"Final total stays at the initial 100000 dollars");
            check(d.finances.history(gang.id).size()==13&&gang.history.size()==6,"Only successful financial and membership operations enter history");
            Files.writeString(out.resolve("audit.PASS"),"Actual dedicated server thread and two connected TCP sessions: foreign/stale/replayed nonces; spoofed actor; role escalation; malformed/deep fields; atomic batch; concurrent queued deposits; unchanged total.\n");stage=2;
        }
        return true;
    }
    private static JsonObject transaction(String kind,String token,long amount){var result=new JsonObject();result.addProperty("action",kind);result.addProperty("financeToken",token);result.addProperty("amount",amount);result.addProperty("comment","");return result;}
    private static String token(ServerPlayer player)throws Exception{
        var f=GangNetwork.class.getDeclaredField("financeTokens");f.setAccessible(true);Object record=((Map<?,?>)f.get(null)).get(player);
        var value=record.getClass().getDeclaredMethod("value");value.setAccessible(true);return (String)value.invoke(record);
    }
    private static void action(ServerPlayer player,JsonObject request)throws Exception{raw(player,request.toString());}
    private static void raw(ServerPlayer player,String request)throws Exception{
        var method=GangNetwork.class.getDeclaredMethod("handleAction",GangNetwork.Action.class,IPayloadContext.class);method.setAccessible(true);
        IPayloadContext context=(IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),new Class<?>[]{IPayloadContext.class},(proxy,m,args)->{
            if(m.getName().equals("player"))return player;
            if(m.getName().equals("toString"))return "Finance audit context";
            throw new AssertionError("Unexpected context method: "+m.getName());
        });method.invoke(null,new GangNetwork.Action(request),context);
    }
    private static void unchecked(ServerPlayer player,JsonObject request){try{action(player,request);}catch(Exception e){throw new RuntimeException(e);}}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
