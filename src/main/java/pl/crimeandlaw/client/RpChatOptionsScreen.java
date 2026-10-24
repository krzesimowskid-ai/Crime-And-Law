package pl.crimeandlaw.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;

/** Both focus states share one height, so this entry point exposes one height control. */
final class RpChatOptionsScreen extends ChatOptionsScreen {
    RpChatOptionsScreen(Screen parent,Options options){super(parent,options);}
    @Override protected void addOptions(){
        list.addSmall(new OptionInstance<?>[]{options.chatVisibility(),options.chatColors(),options.chatLinks(),
            options.chatLinksPrompt(),options.chatOpacity(),options.textBackgroundOpacity(),options.chatScale(),
            options.chatLineSpacing(),options.chatDelay(),options.chatWidth(),options.chatHeightFocused(),
            options.narrator(),options.autoSuggestions(),options.hideMatchedNames(),options.reducedDebugInfo(),options.onlyShowSecureChat()});
    }
}
