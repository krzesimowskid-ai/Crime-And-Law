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
import net.minecraft.client.gui.screens.ChatScreen;
import pl.crimeandlaw.gang.GangFinances;

@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class PlayerHud {
    static final Style HUD_FONT=Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("crimeandlaw","hud"));
    private static final float MONEY_SCALE=.70f;
    private PlayerHud(){}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null&&!(mc.screen instanceof ChatScreen)||mc.options.hideGui)return;
        var g=event.getGuiGraphics();var hud=layout(g);var layout=hud.canvas();int x=hud.mapX(),y=hud.mapY();
        g.pose().pushPose();g.pose().scale(layout.scale(),layout.scale(),1);
        try{ClientMinimap.draw(g,x,y,PlayerHudLayout.MAP_WIDTH,PlayerHudLayout.MAP_HEIGHT,layout.scale(),event.getPartialTick().getGameTimeDeltaPartialTick(false));clock(g,hud.moneyX(),hud.clockY());money(g,hud.moneyX(),hud.moneyY(),false);money(g,hud.moneyX(),hud.moneyY()+PlayerHudLayout.ROW_STEP,true);}
        finally{g.pose().popPose();}
    }
    private static Component amount(boolean bank){return Component.literal(HudMoney.ready?GangFinances.format(bank?HudMoney.bank:HudMoney.cash):"—").setStyle(HUD_FONT);}
    static PlayerHudLayout layout(GuiGraphics g){
        return layout(g.guiWidth(),g.guiHeight());
    }
    static PlayerHudLayout layout(int width,int height){
        var font=Minecraft.getInstance().font;var time=HudClock.INSTANCE.display();
        float span=Math.max(20+Math.min(84,Math.max(font.width(amount(false)),font.width(amount(true)))*MONEY_SCALE),Math.max(31+font.width(Component.literal(time.date()).setStyle(HUD_FONT))*.57f,font.width(Component.literal(time.day()).setStyle(HUD_FONT))*.62f));
        return PlayerHudLayout.of(width,height,span);
    }
    private static void clock(GuiGraphics g,int x,int y){
        var time=HudClock.INSTANCE.display();text(g,time.time(),x,y,.74f,0xFFF2F4F6);text(g,time.date(),x+31,y+1,.57f,0xFFCDD4D9);text(g,time.day(),x,y+12,.62f,0xFFDDE3E7);
    }
    private static void text(GuiGraphics g,String value,int x,int y,float scale,int color){
        var text=Component.literal(value).setStyle(HUD_FONT);g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
        var font=Minecraft.getInstance().font;g.drawString(font,text,0,1,0xFF10171B,false);g.drawString(font,text,0,0,color,false);g.pose().popPose();
    }
    private static void money(GuiGraphics g,int x,int y,boolean bank){
        var font=Minecraft.getInstance().font;
        var text=amount(bank);float textScale=Math.min(MONEY_SCALE,84f/Math.max(1,font.width(text)));
        // One icon and amount per row, with a text shadow instead of opaque dashboard cards.
        MoneyIcons.draw(g,x,y,PlayerHudLayout.ICON_SIZE,bank);g.flush();
        g.pose().pushPose();g.pose().translate(x+20,y+2,0);g.pose().scale(textScale,textScale,1);
        g.drawString(font,text,0,1,0xFF10171B,false);g.drawString(font,text,0,0,bank?0xFFDFEBF5:0xFFB9E8B5,false);g.pose().popPose();
    }
}
