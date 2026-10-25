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
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){event.register(OPEN);event.register(pl.crimeandlaw.client.cultivation.CultivationClient.HARVEST);}
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(pl.crimeandlaw.cultivation.CultivationContent.PLANT.get(),pl.crimeandlaw.client.cultivation.AureliaRenderer::new);}
        @SubscribeEvent public static void itemRenderers(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){
            event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
                private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
                @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new pl.crimeandlaw.client.cultivation.CultivationItemRenderer();return renderer;}
            },pl.crimeandlaw.cultivation.CultivationContent.LEAF.get(),pl.crimeandlaw.cultivation.CultivationContent.SEEDS.get(),pl.crimeandlaw.cultivation.CultivationContent.SHEARS.get());
        }
        @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->{
            pl.crimeandlaw.client.cultivation.PixelSprite.reloadAll(manager);
        });}
        @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{pl.crimeandlaw.cultivation.CultivationNetwork.receiver=pl.crimeandlaw.client.cultivation.CultivationClient::receive;GangNetwork.clockReceiver=p->HudClock.INSTANCE.receive(p.epochMillis(),p.zone());GangNetwork.moneyReceiver=HudMoney::receive;GangNetwork.clientReceiver=ClientState::receive;GangNetwork.terrainReceiver=TerrainClient::receive;GangNetwork.captureReceiver=CaptureClient::receive;});}
    }
    @EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
    public static final class Gameplay {
        @SubscribeEvent public static void screen(ScreenEvent.Opening event){if(event.getNewScreen()!=null&&event.getNewScreen().getClass()==net.minecraft.client.gui.screens.ChatScreen.class)event.setNewScreen(new RpChatScreen(((pl.crimeandlaw.mixin.client.ChatScreenAccess)event.getNewScreen()).crimeandlaw$initial()));}
        @SubscribeEvent public static void tick(ClientTickEvent.Post event){
            Minecraft mc=Minecraft.getInstance();
            if(mc.player!=null&&mc.level!=null&&!mc.options.hideGui)ClientMinimap.tick();
            while(OPEN.consumeClick())if(mc.player!=null && mc.screen==null){
                if(!ClientState.ready())ClientState.message="Pobieranie danych…";
                mc.setScreen(new GangScreen());ClientState.refresh("",0);
            }
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){
            ChatView.clear();HudClock.INSTANCE.clear();HudMoney.clear();ClientMinimap.clear();ClientState.state=new com.google.gson.JsonObject();ClientState.message="";GangNetwork.clearClientTransfers();LogoTexture.clear();BannerTexture.clear();TerrainClient.clear();CaptureClient.clear();
        }
    }
}
