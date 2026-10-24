package pl.crimeandlaw.mixin.client;

import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import pl.crimeandlaw.client.RpChatScreen;

@Mixin(CommandSuggestions.class)
abstract class CommandSuggestionsMixin {
    @Shadow @Final private Screen screen;
    @Shadow @Final private EditBox input;
    @ModifyConstant(method={"showSuggestions","renderUsage"},constant=@Constant(intValue=72))
    private int crimeandlaw$belowEditor(int original){return screen instanceof RpChatScreen?input.getY()+input.getHeight()+8:original;}
}
