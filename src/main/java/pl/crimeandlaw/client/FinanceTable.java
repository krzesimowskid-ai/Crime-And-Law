package pl.crimeandlaw.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.GangDirectory.Theme;
import pl.crimeandlaw.gang.GangFinances;
import pl.crimeandlaw.gang.GangFinances.*;

/** Fixed table header; one page scrolls without moving search, buttons or pagination. */
final class FinanceTable extends AbstractWidget {
    private static final Gson JSON=new Gson();
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd.MM.uuuu"),TIME=DateTimeFormatter.ofPattern("HH:mm");
    private final List<Entry> entries=new ArrayList<>();
    private final boolean bonuses;
    private final ScrollPosition scroll;
    private final Supplier<Theme> theme;
    private final float scale;
    FinanceTable(int x,int y,int width,int height,JsonArray rows,boolean bonuses,ScrollPosition scroll,Supplier<Theme> theme,float scale){
        super(x,y,width,height,Component.literal(bonuses?"Historia premii":"Historia finansowa"));
        for(var row:rows)entries.add(JSON.fromJson(row,Entry.class));
        this.bonuses=bonuses;this.scroll=scroll;this.theme=theme;this.scale=scale;scroll.range(height-22,entries.size()*34);
    }
    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta){
        var font=Minecraft.getInstance().font;int x=getX(),y=getY(),usable=width-20,first=usable*35/100,second=usable*27/100,amount=usable*17/100;
        int c1=x+5,c2=x+first,c3=c2+second,c4=c3+amount;
        g.fill(x,y,x+width,y+19,0x33000000);int accent=theme.get().borderArgb();
        g.drawString(font,bonuses?"Nadawca / odbiorca":"Gracz / transakcja",c1,y+5,accent,false);
        g.drawString(font,"Komentarz",c2,y+5,accent,false);g.drawString(font,"Kwota",c3,y+5,accent,false);g.drawString(font,"Data",c4,y+5,accent,false);
        g.pose().pushPose();g.pose().last().pose().identity();
        g.enableScissor((int)(x*scale),(int)((y+21)*scale),(int)Math.ceil((x+width-13)*scale),(int)Math.ceil((y+height)*scale));g.pose().popPose();
        if(entries.isEmpty())g.drawString(font,"Brak transakcji dla wybranego filtra.",x+6,y+34,0xFFAAA3B3,false);
        for(int i=0;i<entries.size();i++){
            int rowY=y+22+i*34-(int)Math.round(scroll.offset());if(rowY+34<=y+21||rowY>=y+height)continue;
            Entry e=entries.get(i);if(i%2==0)g.fill(x,rowY,x+width-14,rowY+33,0x16000000);
            g.drawString(font,font.plainSubstrByWidth(e.actorName(),first-10),c1,rowY+4,0xFFE9E8EC,false);
            String detail=bonuses?"→ "+e.recipientName():e.kind().label;
            g.drawString(font,font.plainSubstrByWidth(detail,first-10),c1,rowY+17,0xFFAAA3B3,false);
            var comments=font.split(Component.literal(e.comment().isBlank()?"—":e.comment()),second-7);
            for(int line=0;line<Math.min(2,comments.size());line++)g.drawString(font,comments.get(line),c2,rowY+4+line*12,0xFFC9C5CF,false);
            String money=(bonuses?"":e.kind()==Kind.DEPOSIT?"+":"−")+GangFinances.format(e.amount());
            g.drawString(font,font.plainSubstrByWidth(money,amount-5),c3,rowY+4,e.kind()==Kind.DEPOSIT?0xFF82CBA8:0xFFE9C98B,false);
            var date=Instant.ofEpochMilli(e.timestamp()).atZone(ZoneId.systemDefault());
            g.drawString(font,DATE.format(date),c4,rowY+4,0xFFCBC5D0,false);g.drawString(font,TIME.format(date),c4,rowY+17,0xFFAAA3B3,false);
            g.fill(x,rowY+33,x+width-14,rowY+34,0x22000000|theme.get().border());
        }
        g.disableScissor();
        if(isMouseOver(mouseX,mouseY)&&mouseY>=y+22){
            int index=(int)((mouseY-y-22+scroll.offset())/34);
            if(index>=0&&index<entries.size()){
                Entry e=entries.get(index);String detail=e.actorName()+" ("+e.actorRole()+")"+(e.kind()==Kind.BONUS?" → "+e.recipientName()+" ("+e.recipientRole()+")":"")
                    +"\n"+e.kind().label+": "+GangFinances.format(e.amount())+(e.comment().isBlank()?"":"\n"+e.comment());
                g.renderTooltip(font,font.split(Component.literal(detail),300),mouseX,mouseY);
            }
        }
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(isMouseOver(x,y)){scroll.scroll(-vertical*28);return true;}return false;}
    @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
}
