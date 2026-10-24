package pl.crimeandlaw.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import pl.crimeandlaw.client.ChatView;

@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {
    @Inject(method="addMessageToQueue",at=@At("TAIL"))
    private void crimeandlaw$received(net.minecraft.client.GuiMessage message,CallbackInfo callback){ChatView.received(message);}
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void crimeandlaw$render(GuiGraphics graphics,int tick,int mouseX,int mouseY,boolean focused,CallbackInfo callback){ChatView.render(graphics,(ChatComponent)(Object)this,tick,focused);callback.cancel();}
    @Inject(method="scrollChat",at=@At("HEAD"),cancellable=true)
    private void crimeandlaw$scroll(int amount,CallbackInfo callback){if(Minecraft.getInstance().screen instanceof ChatScreen){ChatView.scroll(amount);callback.cancel();}}
    @Inject(method="resetChatScroll",at=@At("TAIL"))
    private void crimeandlaw$reset(CallbackInfo callback){ChatView.resetScroll();}
    @Inject(method="rescaleChat",at=@At("TAIL"))
    private void crimeandlaw$rescale(CallbackInfo callback){ChatView.invalidate();}
    @Inject(method="clearMessages",at=@At("TAIL"))
    private void crimeandlaw$clear(boolean history,CallbackInfo callback){ChatView.clear();}
    @Inject(method="getLinesPerPage",at=@At("HEAD"),cancellable=true)
    private void crimeandlaw$page(CallbackInfoReturnable<Integer> callback){if(Minecraft.getInstance().screen instanceof ChatScreen)callback.setReturnValue(ChatView.layout(true).lines());}
    @Inject(method="getClickedComponentStyleAt",at=@At("HEAD"),cancellable=true)
    private void crimeandlaw$style(double x,double y,CallbackInfoReturnable<Style> callback){callback.setReturnValue(ChatView.styleAt(x,y));}
    @Inject(method="getMessageTagAt",at=@At("HEAD"),cancellable=true)
    private void crimeandlaw$tag(double x,double y,CallbackInfoReturnable<GuiMessageTag> callback){callback.setReturnValue(ChatView.tagAt(x,y));}
    @Inject(method="handleChatQueueClicked",at=@At("HEAD"),cancellable=true)
    private void crimeandlaw$queue(double x,double y,CallbackInfoReturnable<Boolean> callback){callback.setReturnValue(ChatView.queueClicked(x,y));}
}
