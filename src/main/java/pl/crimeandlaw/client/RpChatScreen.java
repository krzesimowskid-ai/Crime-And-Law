package pl.crimeandlaw.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.mixin.client.ChatScreenAccess;

/** Native Minecraft send/history/signing, with a compact upper-left editor. */
public final class RpChatScreen extends ChatScreen {
    private boolean scrolling;
    public RpChatScreen(String initial){super(initial);}
    private CommandSuggestions suggestions(){return ((ChatScreenAccess)(Object)this).crimeandlaw$suggestions();}
    @Override protected void init(){
        String saved=input==null?null:input.getValue();super.init();if(saved==null)saved=input.getValue();removeWidget(input);
        var l=ChatView.layout(true);var font=ChatFonts.get();
        input=new EditBox(font,l.x()+9,l.inputY()+7,l.width()-80,12,Component.literal("Wiadomość"));input.setMaxLength(256);input.setBordered(false);input.setValue(saved);input.setCanLoseFocus(false);input.setTextColor(0xFFE9EEF2);input.setHint(Component.literal("Napisz wiadomość lub /komendę…"));addWidget(input);
        var suggestions=new CommandSuggestions(minecraft,this,input,font,false,false,1,7,false,0xE51B242E);suggestions.setAllowHiding(false);((ChatScreenAccess)(Object)this).crimeandlaw$suggestions(suggestions);
        input.setResponder(value->{suggestions.setAllowSuggestions(true);suggestions.updateCommandInfo();});suggestions.updateCommandInfo();
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        minecraft.gui.getChat().render(g,minecraft.gui.getGuiTicks(),mouseX,mouseY,true);var l=ChatView.layout(true);int mx=(int)l.local(mouseX),my=(int)l.local(mouseY);var font=ChatFonts.get();
        g.pose().pushPose();g.pose().scale(l.scale(),l.scale(),1);
        try{HudShapes.rounded(g,l.x()-3,l.inputY(),l.width()+6,l.inputHeight(),5,0xDA101820);g.flush();g.fill(l.x()-3,l.inputY()+5,l.x()-1,l.inputY()+l.inputHeight()-5,0xFF9CBCD0);
            input.render(g,mx,my,partial);g.pose().pushPose();g.pose().translate(l.x()+l.width()-58,l.inputY()+9,0);g.pose().scale(.72f,.72f,1);g.drawString(font,input.getValue().length()+"/256",0,0,0xFF7F919E,false);g.pose().popPose();
            int sx=l.x()+l.width()-12,sy=l.inputY()+12;HudShapes.triangle(g,sx-4,sy-5,sx+5,sy,sx-4,sy+5,input.getValue().isBlank()?0xFF54636D:0xFFBED7E8);g.flush();
            g.pose().pushPose();g.pose().translate(0,0,300);suggestions().render(g,mx,my);g.pose().popPose();
            var tag=ChatView.tagAt(mouseX,mouseY);var style=ChatView.styleAt(mouseX,mouseY);
            if(tag!=null&&tag.text()!=null)g.renderTooltip(font,font.split(tag.text(),250),mx,my);
            else if(style!=null&&style.getHoverEvent()!=null)g.renderComponentHoverEffect(font,style,mx,my);
        }finally{g.pose().popPose();}
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        var l=ChatView.layout(true);double mx=l.local(x),my=l.local(y);
        if(suggestions().mouseClicked(mx,my,button))return true;
        if(button==0){
            if(mx>=l.x()+l.width()-24&&mx<=l.x()+l.width()+2&&my>=l.inputY()&&my<=l.inputY()+l.inputHeight()){handleChatInput(input.getValue(),true);if(minecraft.screen==this)minecraft.setScreen(null);return true;}
            if(mx>=l.x()+l.width()-22&&mx<=l.x()+l.width()&&my>=l.y()&&my<=l.historyY()){minecraft.setScreen(new ChatOptionsScreen(this,minecraft.options));return true;}
            if(ChatView.scrollbarClicked(x,y)){scrolling=true;return true;}
            if(ChatView.queueClicked(x,y))return true;
            var style=ChatView.styleAt(x,y);if(style!=null&&handleComponentClicked(style))return true;
        }
        if(input.mouseClicked(mx,my,button)){setFocused(input);setDragging(button==0);return true;}return false;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(scrolling){ChatView.dragScrollbar(y);return true;}var l=ChatView.layout(true);return super.mouseDragged(l.local(x),l.local(y),button,l.local(dx),l.local(dy));}
    @Override public boolean mouseReleased(double x,double y,int button){scrolling=false;var l=ChatView.layout(true);return super.mouseReleased(l.local(x),l.local(y),button);}
}
