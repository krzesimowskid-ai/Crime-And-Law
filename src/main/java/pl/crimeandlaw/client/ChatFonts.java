package pl.crimeandlaw.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import pl.crimeandlaw.mixin.client.FontAccess;

final class ChatFonts {
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("crimeandlaw","chat");
    private static Font source,font;
    private ChatFonts(){}
    static Font get(){
        Font next=Minecraft.getInstance().font;
        if(font==null||source!=next){source=next;var fonts=((FontAccess)(Object)next).crimeandlaw$fonts();font=new Font(id->fonts.apply(id.equals(Style.DEFAULT_FONT)?ID:id),false);}
        return font;
    }
}
