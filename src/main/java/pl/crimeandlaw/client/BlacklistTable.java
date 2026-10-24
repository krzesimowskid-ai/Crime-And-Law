package pl.crimeandlaw.client;

import com.google.gson.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.GangDirectory.Theme;

final class BlacklistTable extends AbstractWidget {
    private final List<JsonObject> rows=new ArrayList<>();
    private final ScrollPosition scroll;
    private final Supplier<Theme> theme;
    private final Consumer<String> remove;
    private final float scale;
    private final boolean history;
    BlacklistTable(int x,int y,int w,int h,JsonArray entries,boolean history,ScrollPosition scroll,Supplier<Theme> theme,float scale,Consumer<String> remove){
        super(x,y,w,h,Component.literal("Czarna lista"));entries.forEach(e->rows.add(e.getAsJsonObject()));this.history=history;this.scroll=scroll;this.theme=theme;this.scale=scale;this.remove=remove;scroll.range(h-22,rows.size()*40);
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
        var font=Minecraft.getInstance().font;int x=getX(),y=getY(),body=y+22,first=width*32/100,date=x+width-112;
        g.fill(x,y,x+width,y+20,0x33000000);g.drawString(font,"Gracz / moderator",x+5,y+5,theme.get().borderArgb(),false);g.drawString(font,"Powód",x+first,y+5,theme.get().borderArgb(),false);g.drawString(font,"Data",date,y+5,theme.get().borderArgb(),false);
        g.pose().pushPose();g.pose().last().pose().identity();g.enableScissor((int)(x*scale),(int)(body*scale),(int)Math.ceil((x+width-13)*scale),(int)Math.ceil((y+height)*scale));g.pose().popPose();
        if(rows.isEmpty())g.drawString(font,"Brak wpisów dla wybranego filtra.",x+5,body+12,0xFFAAA3B3,false);
        for(int i=0;i<rows.size();i++){
            int rowY=body+i*40-(int)Math.round(scroll.offset());if(rowY+40<=body||rowY>=y+height)continue;var row=rows.get(i);
            if(i%2==0)g.fill(x,rowY,x+width-13,rowY+39,0x16000000);
            String title=(history?(row.get("added").getAsBoolean()?"+ ":"− "):"")+row.get("name").getAsString();
            g.drawString(font,font.plainSubstrByWidth(title,first-9),x+5,rowY+5,0xFFEAE6ED,false);g.drawString(font,font.plainSubstrByWidth(row.get("actorName").getAsString(),first-9),x+5,rowY+20,0xFFAAA3B3,false);
            var reason=font.split(Component.literal(row.get("reason").getAsString().isBlank()?"—":row.get("reason").getAsString()),date-x-first-7);
            for(int n=0;n<Math.min(2,reason.size());n++)g.drawString(font,reason.get(n),x+first,rowY+5+n*12,0xFFCDC6D0,false);
            var instant=Instant.ofEpochMilli(row.get("timestamp").getAsLong()).atZone(ZoneId.systemDefault());g.drawString(font,DateTimeFormatter.ofPattern("dd.MM.uuuu").format(instant),date,rowY+5,0xFFCBC5D0,false);g.drawString(font,DateTimeFormatter.ofPattern("HH:mm").format(instant),date,rowY+20,0xFFAAA3B3,false);
            if(remove!=null)g.drawString(font,"×",x+width-29,rowY+12,0xFFE0A7B9,false);g.fill(x,rowY+39,x+width-13,rowY+40,0x22000000|theme.get().border());
        }
        g.disableScissor();int index=(int)((my-body+scroll.offset())/40);
        if(isMouseOver(mx,my)&&my>=body&&index>=0&&index<rows.size()){
            var row=rows.get(index);String text=row.get("name").getAsString()+"\nUUID: "+row.get("player").getAsString()+"\n"+row.get("reason").getAsString()+(remove!=null?"\n× — usuń z czarnej listy":"");g.renderTooltip(font,font.split(Component.literal(text),300),mx,my);
        }
    }
    @Override public void onClick(double x,double y,int button){if(button!=0||remove==null||x<getX()+width-38||y<getY()+22)return;int index=(int)((y-getY()-22+scroll.offset())/40);if(index>=0&&index<rows.size())remove.accept(rows.get(index).get("player").getAsString());}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(isMouseOver(x,y)){scroll.scroll(-v*28);return true;}return false;}
    @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
}
