package pl.crimeandlaw.cultivation;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid="crimeandlaw")
public final class PotController {
    private record Key(String dimension,BlockPos pos){}
    private static final class Session {
        final UUID token=UUID.randomUUID(); UUID revision=UUID.randomUUID();final Key key;final Planter planter;
        long activity,began;int operation,slot=-1;ItemStack expected=ItemStack.EMPTY;String message="Przygotuj doniczkę.";
        Session(Key k,Planter p,long now){key=k;planter=p;activity=now;}
        void reset(){operation=0;slot=-1;expected=ItemStack.EMPTY;}
    }
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<Key,ServerPlayer> LOCKS=new HashMap<>();
    private static final Map<ServerPlayer,long[]> RATES=new WeakHashMap<>();
    private static Key key(ServerPlayer p,BlockPos pos){return new Key(p.serverLevel().dimension().location().toString(),pos.immutable());}
    public static boolean reserved(ServerPlayer p,BlockPos pos){return LOCKS.containsKey(key(p,pos));}
    private static boolean allow(ServerPlayer p){long t=System.nanoTime()/1_000_000_000;var r=RATES.computeIfAbsent(p,x->new long[]{t,0});if(r[0]!=t){r[0]=t;r[1]=0;}return ++r[1]<=16;}
    public static String problem(ServerPlayer p,BlockPos pos){
        if(p.isDeadOrDying()||p.isSpectator())return "Nie możesz teraz przygotowywać doniczki.";
        if(!p.serverLevel().hasChunkAt(pos)||Planter.at(p.serverLevel(),pos)==null)return "Stanowisko nie jest dostępne.";
        if(p.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))>25)return "Podejdź bliżej doniczki.";
        if(!p.serverLevel().mayInteract(p,pos))return "Nie możesz użyć stanowiska w tym miejscu.";
        var hit=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),Vec3.atCenterOf(pos),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        if(!Planter.base(p.serverLevel(),hit.getBlockPos()).equals(pos))return "Stanowisko musi być widoczne.";
        return "";
    }
    private static void notice(ServerPlayer p,String msg){p.displayClientMessage(net.minecraft.network.chat.Component.literal(msg),true);}
    public static void interact(ServerPlayer p,BlockPos hit,InteractionHand hand){
        if(!allow(p))return;var pos=Planter.base(p.serverLevel(),hit);String err=problem(p,pos);if(!err.isEmpty()){notice(p,err);return;}
        if(HarvestController.reserved(p,pos)||reserved(p,pos)){notice(p,"Stanowisko jest już używane. Zamknij jego okno przed montażem.");return;}
        if(!HarvestController.interactionAllowed(p,hit)||!problem(p,pos).isEmpty())return;
        var pot=Planter.at(p.serverLevel(),pos);if(pot==null)return;var stack=p.getItemInHand(hand);
        boolean upper=!hit.equals(pos);
        if(pot.frame()){
            if(!upper&&stack.is(CultivationContent.POT_ITEM.get())&&!pot.hasPot()){if(pot.installPot()){stack.shrink(1);p.inventoryMenu.broadcastChanges();}return;}
            if(upper&&stack.is(CultivationContent.UV_LAMP.get())&&!pot.hasLamp()){if(pot.installLamp()){stack.shrink(1);p.inventoryMenu.broadcastChanges();}return;}
            if(p.isShiftKeyDown()&&stack.isEmpty()){
                if(upper&&pot.removeLamp())InventoryExchange.giveOrDrop(p,new ItemStack(CultivationContent.UV_LAMP.get()));
                else if(!upper&&pot.removePot())InventoryExchange.giveOrDrop(p,new ItemStack(CultivationContent.POT_ITEM.get()));
                else notice(p,"Doniczkę można wyjąć dopiero po zużyciu podłoża.");return;
            }
        }
        openChecked(p,pos,pot);
    }
    public static void open(ServerPlayer p,BlockPos hit){
        if(!allow(p))return;var pos=Planter.base(p.serverLevel(),hit);String err=problem(p,pos);if(!err.isEmpty()){notice(p,err);return;}
        if(!HarvestController.interactionAllowed(p,hit)||!problem(p,pos).isEmpty())return;var pot=Planter.at(p.serverLevel(),pos);if(pot!=null)openChecked(p,pos,pot);
    }
    private static void openChecked(ServerPlayer p,BlockPos pos,Planter pot){
        if(!pot.hasPot()){notice(p,"Włóż doniczkę do dolnej części stelaża.");return;}
        if(pot.ready()){if(reserved(p,pos)){notice(p,"Stanowisko jest już używane.");return;}HarvestController.open(p,pos);return;}
        var key=key(p,pos);var previous=SESSIONS.get(p);
        if(previous!=null&&previous.key.equals(key)&&previous.planter==pot){send(p,previous,true);return;}
        if(HarvestController.reserved(p,pos)||LOCKS.containsKey(key)||SESSIONS.size()>=64){notice(p,"Stanowisko zajęte. Spróbuj ponownie później.");return;}
        close(p);var s=new Session(key,pot,p.serverLevel().getGameTime());SESSIONS.put(p,s);LOCKS.put(key,p);send(p,s,true);
    }
    private static boolean valid(ServerPlayer p,Session s){return s.key.dimension.equals(p.serverLevel().dimension().location().toString())&&problem(p,s.key.pos).isEmpty()&&Planter.at(p.serverLevel(),s.key.pos)==s.planter;}
    public static void act(ServerPlayer p,PotNetwork.Action a){
        var s=SESSIONS.get(p);if(s==null||!s.token.equals(a.token()))return;
        if(a.action()==PotNetwork.CLOSE){close(p);return;}if(!allow(p))return;
        if(!valid(p,s)){s.message="Przygotowanie przerwane. Zapisano zatwierdzone czynności.";send(p,s,false);close(p);return;}
        s.activity=p.serverLevel().getGameTime();
        if(a.action()==PotNetwork.RECOVER){s.reset();s.revision=UUID.randomUUID();s.message="Stan odświeżony. Możesz kontynuować.";send(p,s,true);return;}
        if(!s.revision.equals(a.revision())){send(p,s,true);return;}
        s.revision=UUID.randomUUID(); // retire even rejected mutations, so delayed packets cannot repeat later
        if(!HarvestController.interactionAllowed(p,s.key.pos)||!valid(p,s)){s.reset();s.message="Czynność zablokowana lub stanowisko zmienione.";send(p,s,false);close(p);return;}
        var pot=s.planter;var plant=pot.plant;
        if(a.action()==PotNetwork.CANCEL){s.reset();s.message="Przerwano. Materiały zachowano.";send(p,s,true);return;}
        if(a.action()==PotNetwork.HARVEST){s.message="Przejście do zbioru.";send(p,s,false);close(p);HarvestController.open(p,s.key.pos);return;}
        if(a.action()==PotNetwork.FINISH){
            int operation=s.operation,slot=s.slot;long held=p.serverLevel().getGameTime()-s.began;var expected=s.expected.copy();s.reset();
            if(!CultivationRules.validHold(operation,held)||!InventoryExchange.sourceSlot(slot)||!ItemStack.matches(p.getInventory().getItem(slot),expected)){s.message="Nie ukończono czynności lub zmieniono przedmiot.";send(p,s,true);return;}
            ItemStack replacement;int water=0;
            if(operation==PotNetwork.POUR&&!plant.soil()&&expected.getItem() instanceof GardenSupplyItem item&&item.kind==GardenSupplyItem.Kind.SOIL&&item.contents(expected)>0)replacement=item.usedSoil(expected);
            else if(operation==PotNetwork.WATER&&plant.soil()&&expected.getItem() instanceof GardenSupplyItem item&&item.kind==GardenSupplyItem.Kind.WATER&&(water=Math.min(item.contents(expected),CultivationRules.WATER_CAPACITY-plant.water()))>0)replacement=item.withContents(expected,item.contents(expected)-water);
            else {s.message="Czynność nie jest już potrzebna.";send(p,s,true);return;}
            var plan=InventoryExchange.prepare(p,slot,expected,replacement,ItemStack.EMPTY);
            if(plan==null||!plan.commit()){s.message="Ekwipunek się zmienił. Spróbuj ponownie.";send(p,s,true);return;}
            if(operation==PotNetwork.POUR)plant.fillSoil();else plant.addWater(water);
            pot.changed();s.message=operation==PotNetwork.POUR?"Ziemia wsypana. Wybierz nasiono lub sadzonkę.":"Doniczka podlana.";
            p.serverLevel().playSound(null,s.key.pos,operation==PotNetwork.POUR?SoundEvents.GRAVEL_PLACE:SoundEvents.BUCKET_EMPTY,SoundSource.BLOCKS,.65f,1);send(p,s,true);return;
        }
        if(s.operation!=0){s.message="Najpierw zakończ lub anuluj trwającą czynność.";send(p,s,true);return;}
        if(!InventoryExchange.sourceSlot(a.slot())){s.message="Wybierz przedmiot z ekwipunku.";send(p,s,true);return;}
        var stack=p.getInventory().getItem(a.slot());
        if(a.action()==PotNetwork.POUR||a.action()==PotNetwork.WATER){
            boolean ok=stack.getItem() instanceof GardenSupplyItem item&&item.contents(stack)>0&&(a.action()==PotNetwork.POUR?item.kind==GardenSupplyItem.Kind.SOIL&&!plant.soil():item.kind==GardenSupplyItem.Kind.WATER&&plant.soil()&&plant.water()<100);
            if(ok){s.operation=a.action();s.slot=a.slot();s.expected=stack.copy();s.began=p.serverLevel().getGameTime();s.message=s.operation==PotNetwork.POUR?"Kieruj ziemię do otworu doniczki.":"Kieruj wodę do doniczki.";}
            else s.message="Brak odpowiedniego materiału lub doniczka jest już pełna.";
        }else if(a.action()==PotNetwork.UNPACK){s.message=unpack(p,a.slot())?"Woreczek otwarty. Masz 5 luźnych nasion.":"Brak woreczka lub miejsca na nasiona.";}
        else if(a.action()==PotNetwork.PLANT){
            boolean seedling=stack.is(CultivationContent.SEEDLING.get());
            if(plant.soil()&&!plant.planted()&&(stack.is(CultivationContent.SEEDS.get())||seedling)){
                var replacement=stack.copy();replacement.shrink(1);var plan=InventoryExchange.prepare(p,a.slot(),stack,replacement,ItemStack.EMPTY);
                if(plan!=null&&plan.commit()){plant.plant(seedling);pot.changed();s.message="Roślina zasadzona. Podlej ją konewką.";}
            }else s.message="Najpierw wsyp ziemię i wybierz nasiono lub sadzonkę.";
        }
        send(p,s,true);
    }
    private static boolean unpack(ServerPlayer p,int slot){var stack=p.getInventory().getItem(slot);if(!stack.is(CultivationContent.SEED_PACKET.get()))return false;var replacement=stack.copy();replacement.shrink(1);var plan=InventoryExchange.prepare(p,slot,stack,replacement,new ItemStack(CultivationContent.SEEDS.get(),CultivationRules.PACK_SEEDS));return plan!=null&&plan.commit();}
    public static void openPacket(ServerPlayer p,int slot){if(allow(p)&&!p.isSpectator()&&!p.isDeadOrDying()&&InventoryExchange.sourceSlot(slot))notice(p,unpack(p,slot)?"Rozpakowano 5 nasion.":"Zrób miejsce w ekwipunku na nasiona.");}
    private static void send(ServerPlayer p,Session s,boolean active){var pot=s.planter;var plant=pot.plant;PotNetwork.send(p,new PotNetwork.State(s.key.pos,s.token,s.revision,plant.stage(),plant.growth(),plant.water(),plant.leaves(),plant.total(),pot.frame(),pot.hasPot(),pot.hasLamp(),pot.lit(),active,s.operation,s.message));}
    private static void close(ServerPlayer p){var s=SESSIONS.remove(p);if(s!=null)LOCKS.remove(s.key,p);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){if(e.getServer().getTickCount()%20!=0)return;for(var p:new ArrayList<>(SESSIONS.keySet())){var s=SESSIONS.get(p);if(p.server!=e.getServer())continue;if(p.hasDisconnected()){close(p);continue;}if(!valid(p,s)||p.serverLevel().getGameTime()-s.activity>1800){s.message="Przygotowanie przerwane. Zatwierdzony postęp zachowano.";send(p,s,false);close(p);}else send(p,s,true);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){close(p);RATES.remove(p);}}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){SESSIONS.keySet().removeIf(p->p.server==e.getServer());LOCKS.entrySet().removeIf(x->x.getValue().server==e.getServer());RATES.keySet().removeIf(p->p.server==e.getServer());}
}
