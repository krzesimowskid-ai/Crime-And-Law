package pl.crimeandlaw.cultivation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
public final class GardenMerchant extends Villager {
    public GardenMerchant(EntityType<? extends Villager> type,Level level){super(type,level);setNoAi(true);setPersistenceRequired();setInvulnerable(true);setCustomName(Component.literal("Ogrodnik"));setCustomNameVisible(true);setVillagerData(new VillagerData(VillagerType.PLAINS,VillagerProfession.FARMER,2));}
    @Override public boolean isPushable(){return false;}
    @Override public InteractionResult mobInteract(Player p,InteractionHand hand){if(p instanceof net.minecraft.server.level.ServerPlayer sp)ShopController.open(sp,this);return InteractionResult.sidedSuccess(level().isClientSide);}
}
