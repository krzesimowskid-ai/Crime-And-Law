package pl.crimeandlaw.territory;

import com.mojang.brigadier.arguments.*;
import java.time.*;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid="crimeandlaw")
public final class WorldTimeCommands {
    public static final List<String> KEYS=List.of("otwarcie","zamkniecie","przejecie","przygotowanie","utrzymanie","walka","nieobecnosc","cooldown","ochrona","uczestnicy");
    private WorldTimeCommands(){}
    private static boolean allowed(CommandSourceStack source){
        return source.hasPermission(2)||source.getEntity() instanceof ServerPlayer player&&source.getServer().isSingleplayerOwner(player.getGameProfile());
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("crimeandlaw")
            .then(Commands.literal("czas").requires(WorldTimeCommands::allowed)
                .executes(c->show(c.getSource()))
                .then(Commands.argument("strefa",StringArgumentType.greedyString())
                    .suggests((c,b)->SharedSuggestionProvider.suggest(zones(),b))
                    .executes(c->setZone(c.getSource(),StringArgumentType.getString(c,"strefa")))))
            .then(Commands.literal("capt").requires(WorldTimeCommands::allowed)
                .executes(c->show(c.getSource()))
                .then(Commands.argument("parametr",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(KEYS,b))
                    .then(Commands.argument("wartosc",IntegerArgumentType.integer(0,2592000))
                        .executes(c->change(c.getSource(),StringArgumentType.getString(c,"parametr"),IntegerArgumentType.getInteger(c,"wartosc")))))));
    }
    private static List<String> zones(){var result=new ArrayList<>(ZoneId.getAvailableZoneIds());result.add("off");Collections.sort(result);return result;}
    private static int show(CommandSourceStack source){
        var r=CaptureSavedData.get(source.getServer()).rules;
        source.sendSuccess(()->Component.literal("Strefa: "+r.zone()+" | "+ZonedDateTime.now(ZoneId.of(r.zone())).toLocalTime().withNano(0)+" | Doba 24 h: "+r.realTime()+" | Capty: "+r.opens()+":00–"+r.closes()+":00 | Przejęcie: "+r.captureSeconds()+" s | Cooldown: "+r.cooldownSeconds()+" s | Ochrona: "+r.protectionSeconds()+" s | Uczestnicy: "+r.minimumPlayers()),false);return 1;
    }
    private static int setZone(CommandSourceStack source,String zone){
        var data=CaptureSavedData.get(source.getServer());
        try{data.rules=zone.equals("off")?data.rules.realTime(false):data.rules.zone(zone.strip());data.setDirty();return show(source);}
        catch(DateTimeException error){source.sendFailure(Component.literal("Nieznana strefa czasowa. Wybierz podpowiedź klawiszem Tab."));return 0;}
    }
    private static int change(CommandSourceStack source,String key,int value){
        try{if(!KEYS.contains(key))throw new IllegalArgumentException();var data=CaptureSavedData.get(source.getServer());data.rules=data.rules.change(key,value);data.setDirty();return show(source);}
        catch(IllegalArgumentException error){source.sendFailure(Component.literal("Nieprawidłowy parametr lub wartość. Czasy podaj w sekundach; godziny 0–24."));return 0;}
    }
}
