package pl.crimeandlaw.mixin.client;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatScreen.class)
public interface ChatScreenAccess {
    @Accessor("initial") String crimeandlaw$initial();
    @Accessor("commandSuggestions") CommandSuggestions crimeandlaw$suggestions();
    @Accessor("commandSuggestions") void crimeandlaw$suggestions(CommandSuggestions value);
}
