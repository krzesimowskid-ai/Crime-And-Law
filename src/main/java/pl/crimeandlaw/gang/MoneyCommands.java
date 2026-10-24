package pl.crimeandlaw.gang;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import pl.crimeandlaw.network.GangNetwork;

@EventBusSubscriber(modid="crimeandlaw")
public final class MoneyCommands {
    private MoneyCommands(){}
    private static boolean admin(CommandSourceStack source){
        return source.hasPermission(2)||source.getEntity() instanceof ServerPlayer player&&source.getServer().isSingleplayerOwner(player.getGameProfile());
    }
    private static int credit(CommandSourceStack source,ServerPlayer player,GangFinances.Account account,long amount){
        var saved=GangSavedData.get(player.server);try{saved.directory.finances.credit(player.getUUID(),account,amount);saved.setDirty();GangNetwork.sendMoney(player,true);GangNetwork.sendState(player,"");source.sendSuccess(()->Component.literal("Dodano "+GangFinances.format(amount)+" · "+(account==GangFinances.Account.CASH?"Gotówka":"Bank")+" · "+player.getGameProfile().getName()),true);return 1;}
        catch(IllegalArgumentException error){source.sendFailure(Component.literal(error.getMessage()));return 0;}
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("crimeandlaw").then(Commands.literal("pieniadze")
            .executes(c->{
                var player=c.getSource().getPlayerOrException();long amount=GangSavedData.get(player.server).directory.finances.wallet(player.getUUID());
                long cash=GangSavedData.get(player.server).directory.finances.cash(player.getUUID());
                c.getSource().sendSuccess(()->Component.literal("Gotówka: "+GangFinances.format(cash)+" | Bank: "+GangFinances.format(amount)),false);return 1;
            })
            .then(Commands.literal("dodaj").requires(MoneyCommands::admin)
                .then(Commands.argument("gracz",EntityArgument.player())
                    .then(Commands.literal("gotowka").then(Commands.argument("kwota",LongArgumentType.longArg(1,GangFinances.MAX_BALANCE)).executes(c->credit(c.getSource(),EntityArgument.getPlayer(c,"gracz"),GangFinances.Account.CASH,LongArgumentType.getLong(c,"kwota")))))
                    .then(Commands.literal("bank").then(Commands.argument("kwota",LongArgumentType.longArg(1,GangFinances.MAX_BALANCE)).executes(c->credit(c.getSource(),EntityArgument.getPlayer(c,"gracz"),GangFinances.Account.BANK,LongArgumentType.getLong(c,"kwota")))))
                    .then(Commands.argument("kwota",LongArgumentType.longArg(1,GangFinances.MAX_BALANCE))
                        .executes(c->{
                            var player=EntityArgument.getPlayer(c,"gracz");var saved=GangSavedData.get(player.server);
                            try{
                                long amount=LongArgumentType.getLong(c,"kwota");saved.directory.finances.credit(player.getUUID(),amount);saved.setDirty();
                                GangNetwork.sendState(player,"");c.getSource().sendSuccess(()->Component.literal("Dodano "+GangFinances.format(amount)+" na konto gracza "+player.getGameProfile().getName()+"."),true);return 1;
                            }catch(IllegalArgumentException error){c.getSource().sendFailure(Component.literal(error.getMessage()));return 0;}
                        }))))));
    }
}
