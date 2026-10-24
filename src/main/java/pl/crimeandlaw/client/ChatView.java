package pl.crimeandlaw.client;

import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.ChatVisiblity;
import pl.crimeandlaw.mixin.client.ChatComponentAccess;

/** Presentation of Minecraft's authoritative chat history, including its deletion/signature state. */
public final class ChatView {
    private record Row(GuiMessage message,int part,FormattedCharSequence text,String time){}
    private static final ChatScroll SCROLL=new ChatScroll();
    private static final ChatFade FADE=new ChatFade();
    private static List<GuiMessage> source=List.of();
    private static List<Row> rows=List.of();
    private static final IdentityHashMap<GuiMessage,String> times=new IdentityHashMap<>();
    private static int wrappedWidth=-1;private static boolean colored;
    private ChatView(){}
    public static void received(GuiMessage message){
        if(times.size()>=128){var live=Collections.newSetFromMap(new IdentityHashMap<GuiMessage,Boolean>());live.addAll(((ChatComponentAccess)Minecraft.getInstance().gui.getChat()).crimeandlaw$messages());times.keySet().retainAll(live);}
        times.put(message,HudClock.INSTANCE.display().time());
    }
    public static ChatLayout layout(boolean focused){var mc=Minecraft.getInstance();return ChatLayout.of(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),focused,mc.options.chatWidth().get(),mc.options.chatScale().get(),mc.options.chatLineSpacing().get(),mc.options.chatHeightFocused().get());}
    public static boolean hidden(){return Minecraft.getInstance().options.chatVisibility().get()==ChatVisiblity.HIDDEN;}
    private static Component display(Component value){
        if(value.getContents() instanceof TranslatableContents translated&&translated.getKey().equals("chat.type.text")&&translated.getArgs().length==2&&translated.getArgs()[0] instanceof Component name&&translated.getArgs()[1] instanceof Component text)
            return Component.empty().append(name.copy()).append(": ").append(text.copy()).withStyle(value.getStyle());
        return value;
    }
    private static void prepare(ChatComponent chat,ChatLayout layout){
        var current=((ChatComponentAccess)chat).crimeandlaw$messages();boolean colors=Minecraft.getInstance().options.chatColors().get();
        boolean same=current.size()==source.size();if(same)for(int i=0;i<current.size();i++)if(current.get(i)!=source.get(i)){same=false;break;}
        int wrap=layout.width()-52;
        if(!same||wrap!=wrappedWidth||colors!=colored){
            Row anchor=SCROLL.offset()>0&&SCROLL.end()>0&&!rows.isEmpty()?rows.get(SCROLL.end()-1):null;
            var next=new ArrayList<Row>();var font=ChatFonts.get();var live=Collections.newSetFromMap(new IdentityHashMap<GuiMessage,Boolean>());live.addAll(current);times.keySet().retainAll(live);
            for(int i=current.size()-1;i>=0;i--){var message=current.get(i);String time=times.computeIfAbsent(message,key->HudClock.INSTANCE.display().time());
                var lines=net.minecraft.client.gui.components.ComponentRenderUtils.wrapComponents(display(message.content()),wrap,font);
                for(int part=0;part<lines.size();part++)next.add(new Row(message,part,lines.get(part),part==0?time:""));
            }
            if(next.size()>800)next.subList(0,next.size()-800).clear();rows=List.copyOf(next);source=new ArrayList<>(current);wrappedWidth=wrap;colored=colors;
            SCROLL.bounds(rows.size(),layout.lines());
            if(anchor!=null)for(int i=0;i<rows.size();i++){var row=rows.get(i);if(row.message()==anchor.message()&&row.part()==anchor.part()){SCROLL.set(rows.size()-i-1);break;}}
        }
        SCROLL.bounds(rows.size(),layout.lines());
    }
    public static void render(GuiGraphics g,ChatComponent chat,int tick,boolean focused){
        var mc=Minecraft.getInstance();if(hidden()||mc.player==null||mc.screen!=null&&!(mc.screen instanceof ChatScreen)||mc.screen instanceof ChatScreen&&!focused)return;
        var l=layout(focused);prepare(chat,l);double opacity=(mc.options.chatOpacity().get()*.9+.1)*FADE.opacity(focused,System.nanoTime());
        if(!focused&&rows.isEmpty())return;var font=ChatFonts.get();int alpha=(int)(255*opacity);
        g.pose().pushPose();g.pose().scale(l.scale(),l.scale(),1);
        try{
            HudShapes.rounded(g,l.x()-4,l.y()-4,l.width()+8,l.header()+l.historyHeight()+8,5,ChatFade.tint(0xA4101820,opacity*mc.options.textBackgroundOpacity().get()));g.flush();
            g.drawString(font,"CZAT",l.x()+5,l.y()+3,ChatFade.tint(0xFFBBC7CF,opacity),false);g.drawString(font,"⚙",l.x()+l.width()-15,l.y()+2,ChatFade.tint(0xFFD4DDE3,opacity),false);
            for(int i=SCROLL.start();i<SCROLL.end();i++){
                var row=rows.get(i);int y=l.historyY()+(i-SCROLL.start())*l.lineHeight();
                g.drawString(font,row.time(),l.x()+4,y+2,Math.max(4,alpha*3/5)<<24|0xB9C3CB,false);
                if(row.message().tag()!=null)g.fill(l.x(),y+2,l.x()+1,y+11,Math.max(4,alpha/3)<<24|row.message().tag().indicatorColor()&0xFFFFFF);
                g.drawString(font,row.text(),l.x()+43,y+2,alpha<<24|0xFFFFFF,true);
            }
            if(SCROLL.maximum()>0){int track=l.historyHeight(),thumb=Math.max(12,track*l.lines()/Math.max(1,rows.size()));int top=l.historyY()+(track-thumb)*(SCROLL.maximum()-SCROLL.offset())/SCROLL.maximum();
                g.fill(l.x()+l.width()+3,l.historyY(),l.x()+l.width()+5,l.historyY()+track,ChatFade.tint(0x383E4D59,opacity));g.fill(l.x()+l.width()+3,top,l.x()+l.width()+5,top+thumb,ChatFade.tint(0xC4B4C6D1,opacity));}
            if(focused&&mc.getChatListener().queueSize()>0)g.drawString(font,"Oczekujące: "+mc.getChatListener().queueSize()+" · kliknij tutaj",l.x()+3,l.inputY()+l.inputHeight()+5,0xFFE7D69D,true);
        }finally{g.pose().popPose();}
    }
    public static Style styleAt(double screenX,double screenY){
        if(hidden()||!(Minecraft.getInstance().screen instanceof ChatScreen))return null;var l=layout(true);var chat=Minecraft.getInstance().gui.getChat();prepare(chat,l);if(!l.inHistory(screenX,screenY))return null;
        int index=SCROLL.start()+(int)((l.local(screenY)-l.historyY())/l.lineHeight());if(index>=SCROLL.end())return null;
        double x=l.local(screenX)-l.x()-43;if(x<0)return null;return ChatFonts.get().getSplitter().componentStyleAtWidth(rows.get(index).text(),(int)x);
    }
    public static GuiMessageTag tagAt(double screenX,double screenY){
        var l=layout(true);if(hidden()||!(Minecraft.getInstance().screen instanceof ChatScreen)||!l.inHistory(screenX,screenY)||l.local(screenX)>l.x()+3)return null;prepare(Minecraft.getInstance().gui.getChat(),l);
        int index=SCROLL.start()+(int)((l.local(screenY)-l.historyY())/l.lineHeight());return index<SCROLL.end()?rows.get(index).message().tag():null;
    }
    public static boolean queueClicked(double sx,double sy){var l=layout(true);if(!(Minecraft.getInstance().screen instanceof ChatScreen)||hidden()||Minecraft.getInstance().getChatListener().queueSize()==0||l.local(sx)<l.x()||l.local(sx)>l.x()+l.width()||l.local(sy)<l.inputY()+l.inputHeight()+3||l.local(sy)>l.inputY()+l.inputHeight()+18)return false;Minecraft.getInstance().getChatListener().acceptNextDelayedMessage();return true;}
    public static boolean scrollbarClicked(double sx,double sy){var l=layout(true);if(SCROLL.maximum()==0||l.local(sx)<l.x()+l.width()||l.local(sx)>l.x()+l.width()+9||l.local(sy)<l.historyY()||l.local(sy)>l.historyY()+l.historyHeight())return false;dragScrollbar(sy);return true;}
    public static void dragScrollbar(double sy){var l=layout(true);SCROLL.set((int)Math.round((1-Math.clamp((l.local(sy)-l.historyY())/l.historyHeight(),0,1))*SCROLL.maximum()));}
    public static void scroll(int amount){prepare(Minecraft.getInstance().gui.getChat(),layout(true));SCROLL.scroll(amount);}
    public static int scrollOffset(){return SCROLL.offset();}
    public static void resetScroll(){SCROLL.reset();}
    public static void invalidate(){wrappedWidth=-1;}
    public static void clear(){source=List.of();rows=List.of();times.clear();wrappedWidth=-1;SCROLL.bounds(0,1);SCROLL.reset();FADE.reset();}
}
