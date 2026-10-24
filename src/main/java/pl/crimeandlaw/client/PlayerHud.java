package pl.crimeandlaw.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.EventPriority;
import pl.crimeandlaw.gang.GangFinances;

@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class PlayerHud {
    private PlayerHud(){}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null||mc.options.hideGui)return;
        var g=event.getGuiGraphics();var hud=PlayerHudLayout.of(g.guiWidth(),g.guiHeight());var layout=hud.canvas();int x=hud.mapX(),y=hud.mapY();
        g.pose().pushPose();g.pose().scale(layout.scale(),layout.scale(),1);
        try{ClientMinimap.draw(g,x,y,132,92,layout.scale(),event.getPartialTick().getGameTimeDeltaPartialTick(false));money(g,hud.moneyX(),hud.moneyY(),false);money(g,hud.moneyX(),hud.moneyY()+43,true);}
        finally{g.pose().popPose();}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void chat(RenderGuiLayerEvent.Pre event){
        var mc=Minecraft.getInstance();if(!event.getName().equals(VanillaGuiLayers.CHAT)||mc.player==null||mc.screen!=null||mc.options.hideGui)return;
        var g=event.getGuiGraphics();int shift=PlayerHudLayout.of(g.guiWidth(),g.guiHeight()).chatShift();if(shift==0)return;
        event.setCanceled(true);g.pose().pushPose();try{g.pose().translate(0,shift,0);event.getLayer().render(g,event.getPartialTick());}finally{g.pose().popPose();}
    }
    private static void money(GuiGraphics g,int x,int y,boolean bank){
        var font=Minecraft.getInstance().font;int accent=bank?0xFF97BDD8:0xFF9BD7A8;
        g.fillGradient(x,y,x+148,y+35,0xE31B242D,0xD3121820);g.fill(x,y,x+2,y+35,accent);
        MoneyIcons.draw(g,x+7,y+7,23,bank);g.drawString(font,bank?"BANK":"GOTÓWKA",x+37,y+5,0xFFAEBEC6,false);
        String value=HudMoney.ready?GangFinances.format(bank?HudMoney.bank:HudMoney.cash):"—";g.drawString(font,value,x+37,y+20,0xFFF0F5F3,true);
    }
}
