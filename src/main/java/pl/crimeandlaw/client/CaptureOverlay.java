package pl.crimeandlaw.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class CaptureOverlay {
    private CaptureOverlay(){}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();var b=CaptureClient.ownBattle();
        if(mc.player==null||mc.screen!=null||mc.options.hideGui||b==null)return;
        var g=event.getGuiGraphics();int width=Math.min(280,g.guiWidth()-20),x=(g.guiWidth()-width)/2,y=12;
        var gang=ClientState.gang();int color=gang==null?0xA3425D:gang.mapColor;
        g.fill(x,y,x+width,y+40,0xDD181A23);g.fill(x,y,x+width,y+2,0xFF000000|color);
        String phase=switch(b.get("phase").getAsString()){case "CLAIMING"->"Przejmowanie terenu";case "PREPARING"->"Przygotowanie do walki";default->"Walka o teren";};
        g.drawCenteredString(mc.font,phase+" · "+CaptureClient.duration(b.get("remaining").getAsLong()),x+width/2,y+8,0xFFF0EBF1);
        String status=b.get("eliminated").getAsBoolean()?"Wyeliminowano — poczekaj na koniec captu":!b.get("present").getAsBoolean()?"Wróć do strefy "+b.get("x")+", "+b.get("z"):
            b.get("holding").getAsLong()>0?"Utrzymanie: "+b.get("holding")+" / "+b.get("holdRequired")+" s":"Chunk "+b.get("x")+", "+b.get("z");
        g.drawCenteredString(mc.font,mc.font.plainSubstrByWidth(status,width-12),x+width/2,y+24,0xFFBDB5C7);
    }
    @SubscribeEvent public static void name(RenderNameTagEvent event){
        if(!(event.getEntity() instanceof Player target))return;
        var battle=CaptureClient.ownBattle();var gang=ClientState.gang();if(battle==null||gang==null)return;
        if(!target.level().dimension().location().toString().equals(battle.get("dimension").getAsString()))return;
        for(var element:battle.getAsJsonArray("participants")){
            var row=element.getAsJsonObject();if(!row.get("id").getAsString().equals(target.getUUID().toString())||row.get("eliminated").getAsBoolean()||!row.get("present").getAsBoolean())continue;
            boolean ally=row.get("gang").getAsString().equals(gang.id.toString());
            var prefix=Component.literal((ally?"◆ ":"⚔ ")+row.get("name").getAsString()+" | ").withStyle(s->s.withColor(row.get("color").getAsInt()));
            event.setContent(prefix.append(event.getContent()));return;
        }
    }
}
