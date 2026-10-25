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
        long activity,began,inputAt,inputSecond;int inputCount;int operation,slot=-1,sequence=-1;double work,coverX,coverZ;boolean held,coverStarted;PreparationPhysics.Pose pose=new PreparationPhysics.Pose(.8,1,.5,0,0);ItemStack expected=ItemStack.EMPTY;String message="Przygotuj doniczkę.";
        Session(Key k,Planter p,long now){key=k;planter=p;activity=now;}
        void reset(){operation=0;slot=-1;expected=ItemStack.EMPTY;work=0;held=false;sequence=-1;coverStarted=false;}
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
    public static void control(ServerPlayer p,PotNetwork.Control c){
        var s=SESSIONS.get(p);if(s==null||!s.token.equals(c.token())||!s.revision.equals(c.revision())||c.sequence()<0||c.sequence()<=s.sequence)return;
        long second=System.nanoTime()/1_000_000_000L;if(s.inputSecond!=second){s.inputSecond=second;s.inputCount=0;}if(++s.inputCount>24)return;
        if(s.operation!=PotNetwork.POUR&&s.operation!=PotNetwork.WATER&&s.operation!=PotNetwork.PLANT&&s.operation!=PotNetwork.COVER)return;
        var pose=new PreparationPhysics.Pose(c.x(),c.y(),c.z(),c.roll(),c.pitch());
        if(s.operation==PotNetwork.COVER){
            if(!Float.isFinite(c.x())||!Float.isFinite(c.y())||!Float.isFinite(c.z())||Math.abs(c.y()-PreparationPhysics.TOP)>.05||!PreparationPhysics.inside(c.x(),c.z(),.25))return;
        }else if(!PreparationPhysics.bounded(pose))return;
        if(!valid(p,s)||!ItemStack.matches(p.getInventory().getItem(s.slot),s.expected))return;
        long now=p.serverLevel().getGameTime();
        if(now==s.inputAt&&c.held()==s.held)return; // at most one accepted sample per simulation tick
        if(s.operation==PotNetwork.COVER&&now>=s.began){
            if(c.held()&&s.coverStarted){double d=Math.hypot(c.x()-s.coverX,c.z()-s.coverZ);if(d<=.22&&Math.hypot(c.x()-.5,c.z()-.5)<.20)s.work=Math.min(1000,s.work+d/.48*1000);}
            s.coverX=c.x();s.coverZ=c.z();s.coverStarted=c.held();
        }
        s.pose=pose;s.held=c.held();s.sequence=c.sequence();s.inputAt=now;s.activity=now;
    }
    private static boolean unchanged(ServerPlayer p,Session s){return InventoryExchange.sourceSlot(s.slot)&&ItemStack.matches(p.getInventory().getItem(s.slot),s.expected);}
    public static void act(ServerPlayer p,PotNetwork.Action a){
        var s=SESSIONS.get(p);if(s==null||!s.token.equals(a.token()))return;
        if(a.action()==PotNetwork.CLOSE){close(p);return;}if(!allow(p))return;
        if(!valid(p,s)){s.message="Przygotowanie przerwane. Zapisano zatwierdzone czynności.";send(p,s,false);close(p);return;}
        s.activity=p.serverLevel().getGameTime();
        if(a.action()==PotNetwork.RECOVER){s.reset();s.revision=UUID.randomUUID();s.message="Stan odświeżony. Możesz kontynuować.";send(p,s,true);return;}
        if(!s.revision.equals(a.revision())){send(p,s,true);return;}s.revision=UUID.randomUUID();
        if(!HarvestController.interactionAllowed(p,s.key.pos)||!valid(p,s)){s.reset();s.message="Czynność zablokowana lub stanowisko zmienione.";send(p,s,false);close(p);return;}
        var pot=s.planter;var plant=pot.plant;
        if(a.action()==PotNetwork.CANCEL){s.reset();s.message="Przerwano. Materiały zachowano.";send(p,s,true);return;}
        if(a.action()==PotNetwork.HARVEST){s.message="Przejście do zbioru.";send(p,s,false);close(p);HarvestController.open(p,s.key.pos);return;}
        if(a.action()==PotNetwork.DROP){
            if(s.operation!=PotNetwork.PLANT||!unchanged(p,s)){s.reset();s.message="Zmieniono nasiono. Wybierz je ponownie.";send(p,s,true);return;}
            if(p.serverLevel().getGameTime()-s.inputAt>8||s.held||!PreparationPhysics.inside(s.pose.x(),s.pose.z(),.17)){
                s.message="Nasiono minęło dołek. Podnieś je i spróbuj ponownie.";send(p,s,true);return;
            }
            double t=PreparationPhysics.flight(new PreparationPhysics.Point(s.pose.x(),s.pose.y(),s.pose.z()),new PreparationPhysics.Point(0,0,0),PreparationPhysics.TOP-.035);
            if(t<0||t>2){send(p,s,true);return;}
            s.operation=PotNetwork.COVER;s.began=p.serverLevel().getGameTime()+(long)Math.ceil(t*20);s.work=0;s.held=false;s.coverStarted=false;s.message="Nasiono opada. Przysuń ziemię nad dołek LPM.";send(p,s,true);return;
        }
        if(a.action()==PotNetwork.FINISH){
            int op=s.operation,slot=s.slot;double work=s.work;long held=p.serverLevel().getGameTime()-s.began;var expected=s.expected.copy();
            boolean source=unchanged(p,s);s.reset();
            if(!source||!CultivationRules.validHold(op,held)||op!=PotNetwork.CUT&&work<999){s.message="Nie ukończono gestu lub zmieniono przedmiot. Materiały zachowano.";send(p,s,true);return;}
            ItemStack replacement,reward=ItemStack.EMPTY;int water=0;boolean seedling=expected.is(CultivationContent.SEEDLING.get());
            if(op==PotNetwork.CUT&&expected.getItem() instanceof GardenSupplyItem item&&item.kind==GardenSupplyItem.Kind.SOIL&&!item.opened(expected)){
                var opened=item.openSoil(expected);replacement=expected.copy();replacement.shrink(1);if(replacement.isEmpty())replacement=opened;else reward=opened;
            }else if(op==PotNetwork.POUR&&!plant.soil()&&expected.getItem() instanceof GardenSupplyItem item&&item.kind==GardenSupplyItem.Kind.SOIL&&item.opened(expected)&&item.contents(expected)>0)replacement=item.usedSoil(expected);
            else if(op==PotNetwork.WATER&&plant.soil()&&expected.getItem() instanceof GardenSupplyItem item&&item.kind==GardenSupplyItem.Kind.WATER&&(water=Math.min(item.contents(expected),100-plant.water()))>0)replacement=item.withContents(expected,item.contents(expected)-water);
            else if(op==PotNetwork.COVER&&plant.soil()&&!plant.planted()&&(expected.is(CultivationContent.SEEDS.get())||seedling||expected.is(CultivationContent.SEED_PACKET.get()))){replacement=expected.copy();replacement.shrink(1);if(expected.is(CultivationContent.SEED_PACKET.get()))reward=new ItemStack(CultivationContent.SEEDS.get(),CultivationRules.PACK_SEEDS-1);}
            else{s.message="Czynność nie jest już potrzebna.";send(p,s,true);return;}
            var plan=InventoryExchange.prepare(p,slot,expected,replacement,reward);
            if(plan==null||!plan.commit()){s.message="Zrób miejsce w ekwipunku. Materiały zachowano.";send(p,s,true);return;}
            if(op==PotNetwork.CUT){s.message="Worek rozcięty. Złap go LPM. WASD przesuwa, Q/E koryguje przechył.";}
            else if(op==PotNetwork.POUR){plant.fillSoil();s.message="Doniczka pełna. Przygotuj nasiono.";}
            else if(op==PotNetwork.COVER){plant.plant(seedling);s.message="Nasiono przysypane. Podlej konewką.";}
            else{plant.addWater(water);s.message="Doniczka podlana.";}
            pot.changed();p.serverLevel().playSound(null,s.key.pos,op==PotNetwork.WATER?SoundEvents.BUCKET_EMPTY:SoundEvents.GRAVEL_PLACE,SoundSource.BLOCKS,.60f,1);send(p,s,true);return;
        }
        if(s.operation!=0){s.message="Najpierw zakończ lub anuluj trwającą czynność.";send(p,s,true);return;}
        if(!InventoryExchange.sourceSlot(a.slot())){s.message="Wybierz przedmiot z ekwipunku.";send(p,s,true);return;}
        var stack=p.getInventory().getItem(a.slot());boolean ok=false;
        if(a.action()==PotNetwork.CUT||a.action()==PotNetwork.POUR||a.action()==PotNetwork.WATER){
            ok=stack.getItem() instanceof GardenSupplyItem item&&item.contents(stack)>0&&(a.action()==PotNetwork.WATER?item.kind==GardenSupplyItem.Kind.WATER&&plant.soil()&&plant.water()<100:item.kind==GardenSupplyItem.Kind.SOIL&&!plant.soil()&&(a.action()==PotNetwork.CUT?!item.opened(stack):item.opened(stack)));
        }else if(a.action()==PotNetwork.UNPACK){s.message=unpack(p,a.slot())?"Woreczek otwarty. Masz 5 luźnych nasion.":"Brak woreczka lub miejsca na nasiona.";send(p,s,true);return;}
        else if(a.action()==PotNetwork.PLANT){ok=plant.soil()&&!plant.planted()&&(stack.is(CultivationContent.SEEDS.get())||stack.is(CultivationContent.SEEDLING.get())||stack.is(CultivationContent.SEED_PACKET.get()));
            if(ok&&stack.is(CultivationContent.SEED_PACKET.get())){var rest=stack.copy();rest.shrink(1);ok=InventoryExchange.prepare(p,a.slot(),stack,rest,new ItemStack(CultivationContent.SEEDS.get(),4))!=null;}
        }
        if(ok){s.reset();s.operation=a.action();s.slot=a.slot();s.expected=stack.copy();s.began=p.serverLevel().getGameTime();s.inputAt=0;s.message=s.operation==PotNetwork.CUT?"Przeciągnij ostrze wzdłuż oznaczonego szwu.":s.operation==PotNetwork.PLANT?"Podnieś nasiono LPM i puść nad dołkiem.":"Trzymaj przedmiot LPM. WASD przesuwa, Q/E koryguje przechył.";}
        else s.message="Brak odpowiedniego materiału, miejsca lub stanowisko jest już gotowe.";
        send(p,s,true);
    }
    private static boolean unpack(ServerPlayer p,int slot){var stack=p.getInventory().getItem(slot);if(!stack.is(CultivationContent.SEED_PACKET.get()))return false;var replacement=stack.copy();replacement.shrink(1);var plan=InventoryExchange.prepare(p,slot,stack,replacement,new ItemStack(CultivationContent.SEEDS.get(),CultivationRules.PACK_SEEDS));return plan!=null&&plan.commit();}
    public static void openPacket(ServerPlayer p,int slot){if(allow(p)&&!p.isSpectator()&&!p.isDeadOrDying()&&InventoryExchange.sourceSlot(slot))notice(p,unpack(p,slot)?"Rozpakowano 5 nasion.":"Zrób miejsce w ekwipunku na nasiona.");}
    private static void send(ServerPlayer p,Session s,boolean active){var pot=s.planter;var plant=pot.plant;PotNetwork.send(p,new PotNetwork.State(s.key.pos,s.token,s.revision,plant.stage(),plant.growth(),plant.water(),plant.leaves(),plant.total(),pot.frame(),pot.hasPot(),pot.hasLamp(),pot.lit(),active,s.operation,(int)s.work,s.slot,s.message));}
    private static void close(ServerPlayer p){var s=SESSIONS.remove(p);if(s!=null)LOCKS.remove(s.key,p);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        long now=e.getServer().getTickCount();for(var p:new ArrayList<>(SESSIONS.keySet())){var s=SESSIONS.get(p);if(p.server!=e.getServer())continue;
            if(p.hasDisconnected()){close(p);continue;}
            if(now%5==0&&(!valid(p,s)||p.serverLevel().getGameTime()-s.activity>1800)){s.message="Przygotowanie przerwane. Zatwierdzony postęp zachowano.";send(p,s,false);close(p);continue;}
            if((s.operation==PotNetwork.POUR||s.operation==PotNetwork.WATER)&&p.serverLevel().getGameTime()-s.inputAt<=8&&s.held&&unchanged(p,s)){
                boolean water=s.operation==PotNetwork.WATER;double fraction=PreparationPhysics.hitFraction(s.pose,water,true,water?1:s.work/1000);
                s.work=Math.min(1000,s.work+fraction*1000.0/(water?30:60));
            }
            if(now%5==0)send(p,s,true);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){close(p);RATES.remove(p);}}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){SESSIONS.keySet().removeIf(p->p.server==e.getServer());LOCKS.entrySet().removeIf(x->x.getValue().server==e.getServer());RATES.keySet().removeIf(p->p.server==e.getServer());}
}
