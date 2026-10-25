package pl.crimeandlaw.cultivation;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** All reservations, cuts, tool wear and rewards happen on the server thread. */
@EventBusSubscriber(modid="crimeandlaw")
public final class HarvestController {
    private record Key(String dimension,BlockPos pos){}
    private static final class Session {
        final UUID token=UUID.randomUUID(),plant;final Key key;final long opened;
        int target;long began=-1;
        Session(Key key,AureliaPlant plant,long tick,int choice){this.key=key;this.plant=plant.identity();opened=tick;target=PruningRules.target(plant.leaves(),choice);}
    }
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<Key,ServerPlayer> LOCKS=new HashMap<>();
    private static final Map<ServerPlayer,long[]> RATES=new WeakHashMap<>();
    private HarvestController(){}
    public static String problem(ServerPlayer player,BlockPos pos){
        if(player.isDeadOrDying()||player.isSpectator())return "Nie możesz teraz zbierać roślin.";
        if(player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))>25)return "Podejdź bliżej rośliny.";
        if(!player.serverLevel().hasChunkAt(pos))return "Roślina nie jest dostępna.";
        if(!player.serverLevel().mayInteract(player,pos))return "Nie możesz zbierać roślin w tym miejscu.";
        var sight=player.serverLevel().clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),Vec3.atCenterOf(pos),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        if(!sight.getBlockPos().equals(pos))return "Roślina musi być widoczna — nie można zbierać przez ścianę.";
        var state=player.serverLevel().getBlockState(pos);
        if(!state.is(CultivationContent.AURELIA.get())||state.getValue(AureliaCrop.GROWTH)<4)return "Roślina musi najpierw dojrzeć.";
        if(!player.getMainHandItem().is(CultivationContent.SHEARS.get()))return "Weź specjalne nożyce do przycinania do głównej ręki.";
        return "";
    }
    public static void open(ServerPlayer player,BlockPos pos){
        if(!allow(player))return;
        pos=pos.immutable();String error=problem(player,pos);
        if(!error.isEmpty()){message(player,pos,error);return;}
        if(!interactionAllowed(player,pos)){message(player,pos,"Zbiór w tym miejscu został zablokowany.");return;}
        error=problem(player,pos);if(!error.isEmpty()){message(player,pos,error);return;}
        Key key=new Key(player.serverLevel().dimension().location().toString(),pos);
        var previous=SESSIONS.get(player);
        if(previous!=null&&previous.key.equals(key)){send(player,previous,true,"");return;}
        if(LOCKS.containsKey(key)&&LOCKS.get(key)!=player){message(player,pos,"Tę roślinę przycina już inny gracz.");return;}
        if(SESSIONS.size()>=64){message(player,pos,"Spróbuj ponownie za chwilę.");return;}
        close(player);var plant=(AureliaPlant)player.serverLevel().getBlockEntity(pos);
        if(plant==null||plant.leaves()==0){message(player,pos,"Ta roślina została już zebrana.");return;}
        var session=new Session(key,plant,player.serverLevel().getGameTime(),player.getRandom().nextInt());SESSIONS.put(player,session);LOCKS.put(key,player);
        send(player,session,true,"Wybierz oznaczony liść i przytrzymaj przy jego nasadzie.");
    }
    public static void cut(ServerPlayer player,CultivationNetwork.Cut packet){
        var session=SESSIONS.get(player);if(session==null||!session.token.equals(packet.token()))return;
        if(packet.phase()==2){close(player);return;}
        if(!allow(player))return;
        String error=problem(player,session.key.pos);
        if(!session.key.dimension.equals(player.serverLevel().dimension().location().toString()))error="Zmieniono świat — zbiór przerwany.";
        if(!error.isEmpty()){send(player,session,false,error);close(player);return;}
        var plant=player.serverLevel().getBlockEntity(session.key.pos) instanceof AureliaPlant p?p:null;
        if(plant==null||!plant.identity().equals(session.plant)){
            send(player,session,false,error.isEmpty()?"Roślina została zmieniona.":error);close(player);return;
        }
        if(packet.phase()==3){session.began=-1;send(player,session,true,"Cięcie anulowane. Spróbuj ponownie.");return;}
        if(packet.leaf()!=session.target||!PruningRules.available(plant.leaves(),packet.leaf()))return;
        long now=player.serverLevel().getGameTime();
        if(packet.phase()==0){if(session.began<0){session.began=now;send(player,session,true,"Puść przycisk w zielonym polu.");}return;}
        if(packet.phase()!=1||session.began<0)return;
        long held=now-session.began;session.began=-1;
        if(!PruningRules.precise(held)){send(player,session,true,held<PruningRules.MIN_HOLD?"Za szybko. Przytrzymaj dłużej.":"Za długo. Spróbuj łagodniejszego cięcia.");return;}
        if(!interactionAllowed(player,session.key.pos)||player.serverLevel().getBlockEntity(session.key.pos)!=plant||!problem(player,session.key.pos).isEmpty()){
            send(player,session,false,"Zbiór został zablokowany lub roślina się zmieniła.");close(player);return;
        }
        if(!plant.cut(packet.leaf()))return;
        give(player,new ItemStack(CultivationContent.LEAF.get()));
        player.getMainHandItem().hurtAndBreak(1,player,EquipmentSlot.MAINHAND);
        player.serverLevel().playSound(null,session.key.pos,SoundEvents.SHEEP_SHEAR,SoundSource.BLOCKS,.65f,1.1f+player.getRandom().nextFloat()*.18f);
        if(plant.leaves()==0){
            send(player,session,false,"Zbiór ukończony. Roślina odrośnie.");
            close(player);plant.regrow();player.serverLevel().setBlock(session.key.pos,plant.getBlockState().setValue(AureliaCrop.GROWTH,1),3);return;
        }
        session.target=PruningRules.target(plant.leaves(),player.getRandom().nextInt());
        if(!player.getMainHandItem().is(CultivationContent.SHEARS.get())){send(player,session,false,"Nożyce się zużyły. Pozostałe liście zachowano.");close(player);return;}
        send(player,session,true,"Precyzyjne cięcie! Zebrano liść.");
    }
    private static void give(ServerPlayer player,ItemStack reward){
        // Inventory.add discards creative overflow; only use it when the one leaf fits.
        var inv=player.getInventory();boolean room=inv.items.stream().anyMatch(s->s.isEmpty()||ItemStack.isSameItemSameComponents(s,reward)&&s.getCount()<Math.min(s.getMaxStackSize(),inv.getMaxStackSize()));
        room|=inv.offhand.stream().anyMatch(s->!s.isEmpty()&&ItemStack.isSameItemSameComponents(s,reward)&&s.getCount()<Math.min(s.getMaxStackSize(),inv.getMaxStackSize()));
        if(room)inv.add(reward);if(!reward.isEmpty())player.drop(reward,false);player.inventoryMenu.broadcastChanges();
    }
    private static void message(ServerPlayer player,BlockPos pos,String text){CultivationNetwork.send(player,new CultivationNetwork.State(pos,new UUID(0,0),0,-1,false,false,text));}
    private static void send(ServerPlayer player,Session s,boolean active,String message){
        int mask=s.key.dimension.equals(player.serverLevel().dimension().location().toString())&&player.serverLevel().hasChunkAt(s.key.pos)&&player.serverLevel().getBlockEntity(s.key.pos) instanceof AureliaPlant p?p.leaves():0;
        CultivationNetwork.send(player,new CultivationNetwork.State(s.key.pos,s.token,mask,s.target,s.began>=0,active,message));
    }
    private static void close(ServerPlayer player){var s=SESSIONS.remove(player);if(s!=null)LOCKS.remove(s.key,player);}
    private static boolean interactionAllowed(ServerPlayer player,BlockPos pos){
        var event=net.neoforged.neoforge.common.CommonHooks.onRightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
        return !event.isCanceled()&&event.getUseBlock()!=net.neoforged.neoforge.common.util.TriState.FALSE;
    }
    private static boolean allow(ServerPlayer player){long second=System.nanoTime()/1_000_000_000;var rate=RATES.computeIfAbsent(player,p->new long[]{second,0});if(rate[0]!=second){rate[0]=second;rate[1]=0;}return ++rate[1]<=16;}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){close(p);RATES.remove(p);}}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(event.getServer().getTickCount()%10!=0)return;
        for(var p:new ArrayList<>(SESSIONS.keySet()))if(p.server==event.getServer()){
            var s=SESSIONS.get(p);if(s==null)continue;
            if(p.hasDisconnected()){close(p);continue;}
            if(!s.key.dimension.equals(p.serverLevel().dimension().location().toString())||p.serverLevel().getGameTime()-s.opened>1800||!problem(p,s.key.pos).isEmpty()){
                message(p,s.key.pos,"Zbiór przerwany. Możesz wrócić do pozostałych liści.");close(p);
            }
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){for(var p:new ArrayList<>(SESSIONS.keySet()))if(p.server==event.getServer())close(p);RATES.keySet().removeIf(p->p.server==event.getServer());}
}
