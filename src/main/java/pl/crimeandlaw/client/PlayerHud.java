package pl.crimeandlaw.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
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
    static final Style HUD_FONT=Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("crimeandlaw","hud"));
    private PlayerHud(){}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null||mc.options.hideGui)return;
        var g=event.getGuiGraphics();var hud=PlayerHudLayout.of(g.guiWidth(),g.guiHeight());var layout=hud.canvas();int x=hud.mapX(),y=hud.mapY();
        g.pose().pushPose();g.pose().scale(layout.scale(),layout.scale(),1);
        try{ClientMinimap.draw(g,x,y,PlayerHudLayout.MAP_WIDTH,PlayerHudLayout.MAP_HEIGHT,layout.scale(),event.getPartialTick().getGameTimeDeltaPartialTick(false));money(g,hud.moneyX(),hud.moneyY(),false);money(g,hud.moneyX(),hud.moneyY()+24,true);}
        finally{g.pose().popPose();}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void chat(RenderGuiLayerEvent.Pre event){
        var mc=Minecraft.getInstance();if(!event.getName().equals(VanillaGuiLayers.CHAT)||mc.player==null||mc.screen!=null||mc.options.hideGui)return;
        var g=event.getGuiGraphics();int shift=PlayerHudLayout.of(g.guiWidth(),g.guiHeight()).chatShift();if(shift==0)return;
        event.setCanceled(true);g.pose().pushPose();try{g.pose().translate(0,shift,0);event.getLayer().render(g,event.getPartialTick());}finally{g.pose().popPose();}
    }
    private static void money(GuiGraphics g,int x,int y,boolean bank){
        var font=Minecraft.getInstance().font;
        String value=HudMoney.ready?GangFinances.format(bank?HudMoney.bank:HudMoney.cash):"—";
        var text=Component.literal(value).setStyle(HUD_FONT);float textScale=Math.min(1.08f,118f/Math.max(1,font.width(text)));
        // One icon and amount per row, with a text shadow instead of opaque dashboard cards.
        MoneyIcons.draw(g,x,y,22,bank);g.flush();
        g.pose().pushPose();g.pose().translate(x+28,y+4,0);g.pose().scale(textScale,textScale,1);
        g.drawString(font,text,0,1,0xFF10171B,false);g.drawString(font,text,0,0,bank?0xFFDFEBF5:0xFFB9E8B5,false);g.pose().popPose();
    }
}
