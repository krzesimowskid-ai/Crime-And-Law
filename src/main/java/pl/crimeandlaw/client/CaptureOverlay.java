package pl.crimeandlaw.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
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
        if(mc.player==null||mc.screen!=null&&!(mc.screen instanceof ChatScreen)||mc.options.hideGui||b==null)return;
        var g=event.getGuiGraphics();var layout=CaptureHudLayout.of(g.guiWidth(),ChatView.hidden()?null:ChatView.layout(false));
        int width=layout.width(),x=layout.x(),y=layout.y();
        var gang=ClientState.gang();int color=gang==null?0xA3425D:gang.mapColor;
        g.fill(x,y,x+width,y+layout.height(),0xDD181A23);g.fill(x,y,x+width,y+2,0xFF000000|color);
        String phase=switch(b.get("phase").getAsString()){case "CLAIMING"->"Przejmowanie terenu";case "PREPARING"->"Przygotowanie do walki";default->"Walka o teren";};
        fitted(g,mc.font,phase+" · "+CaptureClient.duration(b.get("remaining").getAsLong()),x,y+8,width,0xFFF0EBF1);
        String status=b.get("eliminated").getAsBoolean()?"Wyeliminowano — poczekaj na koniec captu":!b.get("present").getAsBoolean()?"Wróć do strefy "+b.get("x")+", "+b.get("z"):
            b.get("holding").getAsLong()>0?"Utrzymanie: "+b.get("holding")+" / "+b.get("holdRequired")+" s":"Chunk "+b.get("x")+", "+b.get("z");
        fitted(g,mc.font,status,x,y+24,width,0xFFBDB5C7);
    }
    private static void fitted(GuiGraphics g,Font font,String text,int x,int y,int width,int color){
        float scale=Math.min(1,Math.max(1,width-12)/(float)Math.max(1,font.width(text)));
        g.pose().pushPose();
        try{g.pose().translate(x+width/2f,y+(1-scale)*font.lineHeight/2,0);g.pose().scale(scale,scale,1);g.drawCenteredString(font,text,0,0,color);}
        finally{g.pose().popPose();}
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
