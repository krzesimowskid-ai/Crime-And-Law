package pl.crimeandlaw.client.cultivation;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;
import pl.crimeandlaw.cultivation.*;

@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class CultivationClient {
    public static final KeyMapping HARVEST=new KeyMapping("key.crimeandlaw.harvest",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_Z,"key.categories.crimeandlaw");
    private static long requested;
    private CultivationClient(){}
    public static void receive(CultivationNetwork.State state){
        var mc=Minecraft.getInstance();
        if(mc.screen instanceof HarvestScreen screen&&screen.token().equals(state.token()))screen.receive(state);
        else if(state.active()&&mc.player!=null&&mc.screen==null)mc.setScreen(new HarvestScreen(state));
        else if(!state.message().isEmpty()&&mc.player!=null){
            if(mc.screen instanceof HarvestScreen screen)screen.interrupted(state.message());
            else mc.player.displayClientMessage(Component.literal(state.message()),true);
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        while(HARVEST.consumeClick())if(mc.player!=null&&mc.screen==null&&mc.hitResult instanceof BlockHitResult hit&&mc.level!=null&&mc.level.getBlockState(hit.getBlockPos()).is(CultivationContent.AURELIA.get())){
            if(System.nanoTime()-requested<500_000_000L)continue;requested=System.nanoTime();
            CultivationNetwork.open(hit.getBlockPos());
        }
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null||mc.options.hideGui||!(mc.hitResult instanceof BlockHitResult hit))return;
        var state=mc.level.getBlockState(hit.getBlockPos());if(!state.is(CultivationContent.AURELIA.get()))return;
        int age=state.getValue(AureliaCrop.GROWTH);boolean ready=age==4,tool=mc.player.getMainHandItem().is(CultivationContent.SHEARS.get());
        GuiGraphics g=event.getGuiGraphics();int w=224,x=(g.guiWidth()-w)/2,y=g.guiHeight()/2+36;
        g.fill(x,y,x+w,y+40,0xE014211E);g.renderOutline(x,y,w,40,0xFF608775);g.fill(x,y,x+2,y+40,0xFFBBDC86);
        String title=ready?"AURELIA · gotowa do zbioru":"AURELIA · wzrost "+age*25+"%";
        g.drawString(mc.font,title,x+10,y+7,0xFFEAF0DF,false);
        String hint=ready?(tool?HARVEST.getTranslatedKeyMessage().getString()+" — zbierz liście":"Wymagane specjalne nożyce do przycinania"):"Potrzebuje światła i nawodnionej ziemi";
        g.drawString(mc.font,mc.font.plainSubstrByWidth(hint,w-20),x+10,y+23,ready&&tool?0xFFBEDE8B:0xFFB0C0B5,false);
    }
}
