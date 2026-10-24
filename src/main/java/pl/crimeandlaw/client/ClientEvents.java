package pl.crimeandlaw.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;
import pl.crimeandlaw.network.GangNetwork;

public final class ClientEvents {
    public static final KeyMapping OPEN=new KeyMapping("key.crimeandlaw.gang",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,"key.categories.crimeandlaw");
    @EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){event.register(OPEN);}
        @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{GangNetwork.moneyReceiver=HudMoney::receive;GangNetwork.clientReceiver=ClientState::receive;GangNetwork.terrainReceiver=TerrainClient::receive;GangNetwork.captureReceiver=CaptureClient::receive;});}
    }
    @EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
    public static final class Gameplay {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event){
            Minecraft mc=Minecraft.getInstance();
            if(mc.player!=null&&mc.level!=null&&!mc.options.hideGui)ClientMinimap.tick();
            while(OPEN.consumeClick())if(mc.player!=null && mc.screen==null){
                if(!ClientState.ready())ClientState.message="Pobieranie danych…";
                mc.setScreen(new GangScreen());ClientState.refresh("",0);
            }
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){
            HudMoney.clear();ClientMinimap.clear();ClientState.state=new com.google.gson.JsonObject();ClientState.message="";GangNetwork.clearClientTransfers();LogoTexture.clear();BannerTexture.clear();TerrainClient.clear();CaptureClient.clear();
        }
    }
}
