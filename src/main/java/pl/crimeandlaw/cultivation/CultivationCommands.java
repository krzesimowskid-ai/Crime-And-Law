package pl.crimeandlaw.cultivation;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
@EventBusSubscriber(modid="crimeandlaw")
public final class CultivationCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("crimeandlaw").then(Commands.literal("uprawy").requires(s->s.hasPermission(2)||s.getEntity() instanceof ServerPlayer p&&s.getServer().isSingleplayerOwner(p.getGameProfile()))
        .then(Commands.literal("sprzedawca").executes(c->{var s=c.getSource();var m=CultivationContent.MERCHANT.get().create(s.getLevel());if(m==null)return 0;var pos=s.getPosition().add(s.getRotation().y==0?2:0,0,2);m.moveTo(pos.x,pos.y,pos.z,s.getRotation().y+180,0);s.getLevel().addFreshEntity(m);s.sendSuccess(()->Component.literal("Ustawiono sprzedawcę wyposażenia."),true);return 1;}))
        .then(Commands.literal("dojrzej").executes(c->{var p=c.getSource().getPlayerOrException();if(p.pick(5,0,false) instanceof BlockHitResult hit){var pot=Planter.at(p.serverLevel(),hit.getBlockPos());if(pot!=null&&pot.plant.planted()&&!pot.plant.mature()&&!PotController.reserved(p,pot.getBlockPos())){var d=pot.plant.data();pot.plant.restore(new PlanterState.Data(true,true,CultivationRules.GROWTH_TICKS-1,d.initial(),pot.hasLamp()?CultivationRules.GROWTH_TICKS-1-d.initial():d.uv(),100,0,0,d.identity()));pot.plant.step(true,pot.hasLamp(),p.getRandom()::nextInt);pot.changed();return 1;}}c.getSource().sendFailure(Component.literal("Spójrz na zasadzoną roślinę w doniczce. To polecenie testowe administratora."));return 0;}))));}
}
