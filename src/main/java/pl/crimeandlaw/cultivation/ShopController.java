package pl.crimeandlaw.cultivation;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import pl.crimeandlaw.gang.*;
import pl.crimeandlaw.network.GangNetwork;

@EventBusSubscriber(modid="crimeandlaw")
public final class ShopController {
    private static final class Session {final UUID id=UUID.randomUUID(),merchant;final String dimension;UUID quote=UUID.randomUUID();long activity;Session(ServerPlayer p,GardenMerchant m){merchant=m.getUUID();dimension=p.serverLevel().dimension().location().toString();activity=p.serverLevel().getGameTime();}}
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();private static final Map<ServerPlayer,long[]> RATES=new WeakHashMap<>();
    private static boolean allow(ServerPlayer p){long t=System.nanoTime()/1_000_000_000;var r=RATES.computeIfAbsent(p,x->new long[]{t,0});if(r[0]!=t){r[0]=t;r[1]=0;}return ++r[1]<=16;}
    private static GardenMerchant merchant(ServerPlayer p,Session s){return s.dimension.equals(p.serverLevel().dimension().location().toString())&&p.serverLevel().getEntity(s.merchant) instanceof GardenMerchant m?m:null;}
    private static boolean valid(ServerPlayer p,GardenMerchant m){return !p.isDeadOrDying()&&!p.isSpectator()&&m!=null&&m.isAlive()&&m.distanceToSqr(p)<=25&&p.hasLineOfSight(m);}
    public static void open(ServerPlayer p,GardenMerchant m){if(!allow(p)||!valid(p,m)||SESSIONS.size()>=64)return;var s=new Session(p,m);SESSIONS.put(p,s);send(p,s,true,"Wybierz potrzebne materiały. Płatność gotówką.");}
    public static void request(ServerPlayer p,ShopNetwork.Request r){
        var s=SESSIONS.get(p);if(s==null||!s.id.equals(r.session()))return;if(r.action()==ShopNetwork.CLOSE){SESSIONS.remove(p);return;}if(!allow(p))return;
        if(!valid(p,merchant(p,s))){send(p,s,false,"Zakupy przerwane. Podejdź do sprzedawcy.");SESSIONS.remove(p);return;}
        s.activity=p.serverLevel().getGameTime();
        if(r.action()==ShopNetwork.RECOVER){s.quote=UUID.randomUUID();send(p,s,true,"Stan zakupów odświeżony.");return;}
        if(r.action()!=ShopNetwork.BUY||!s.quote.equals(r.quote())){send(p,s,true,"Stan zakupów odświeżony.");return;}
        s.quote=UUID.randomUUID(); // one debit and one item per quote, including delayed/replayed packets
        if(r.offer()<0||r.offer()>=GardenCatalog.OFFERS.size()){send(p,s,true,"Nieprawidłowy produkt.");return;}
        var offer=GardenCatalog.OFFERS.get(r.offer());var plan=InventoryExchange.prepare(p,-1,ItemStack.EMPTY,ItemStack.EMPTY,offer.stack());
        if(plan==null){send(p,s,true,"Zrób miejsce w ekwipunku przed zakupem.");return;}
        var saved=GangSavedData.get(p.server);var funds=saved.directory.finances;
        try{funds.spend(p.getUUID(),GangFinances.Account.CASH,offer.price());}
        catch(IllegalArgumentException e){send(p,s,true,e.getMessage());return;}
        if(!plan.commit()){funds.credit(p.getUUID(),GangFinances.Account.CASH,offer.price());send(p,s,true,"Ekwipunek się zmienił. Zakup anulowany.");return;}
        saved.setDirty();GangNetwork.sendMoney(p,true);send(p,s,true,"Kupiono: "+offer.stack().getHoverName().getString()+".");
    }
    private static void send(ServerPlayer p,Session s,boolean active,String text){var m=merchant(p,s);ShopNetwork.send(p,new ShopNetwork.State(s.id,s.quote,m==null?-1:m.getId(),GangSavedData.get(p.server).directory.finances.cash(p.getUUID()),active,text));}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){if(e.getServer().getTickCount()%20!=0)return;for(var p:new ArrayList<>(SESSIONS.keySet()))if(p.server==e.getServer()){var s=SESSIONS.get(p);if(p.hasDisconnected())SESSIONS.remove(p);else if(!valid(p,merchant(p,s))||p.serverLevel().getGameTime()-s.activity>1800){send(p,s,false,"Sklep zamknięty. Możesz otworzyć go ponownie.");SESSIONS.remove(p);}}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){SESSIONS.remove(p);RATES.remove(p);}}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){SESSIONS.keySet().removeIf(p->p.server==e.getServer());RATES.keySet().removeIf(p->p.server==e.getServer());}
}
