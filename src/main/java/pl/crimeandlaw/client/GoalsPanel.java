package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

final class GoalsPanel extends AbstractWidget {
    private final JsonObject data;
    private final long received=System.nanoTime();
    GoalsPanel(int x,int y,int width,int height,JsonObject data){super(x,y,width,height,Component.literal("Tygodniowe cele"));this.data=data;}
    long remaining(){return data.has("endsAt")?Math.max(0,data.get("endsAt").getAsLong()-data.get("now").getAsLong()-(System.nanoTime()-received)/1_000_000):0;}
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
        var font=Minecraft.getInstance().font;int x=getX(),y=getY();long seconds=remaining()/1000;
        String timer="Do końca tygodnia: "+seconds/86400+" d "+seconds/3600%24+" h "+seconds/60%60+" min";
        g.drawString(font,timer,x,y,0xFFCCBAC7,false);g.drawString(font,"Postęp bez nagród",x+width-font.width("Postęp bez nagród"),y,0xFFAAA3B3,false);
        if(!data.has("cards")){g.drawString(font,"Pobieranie postępu…",x,y+30,0xFFAAA3B3,false);return;}
        int index=0;
        for(var item:data.getAsJsonArray("cards")){
            var card=item.getAsJsonObject();int top=y+22+index++*73,current=card.get("current").getAsInt(),target=card.get("target").getAsInt();boolean done=current>=target;
            g.fill(x,top,x+width,top+67,done?0x353A7954:0x28000000);g.renderOutline(x,top,width,67,done?0x8872BB8D:0x443B3541);
            g.drawString(font,card.get("name").getAsString(),x+10,top+7,0xFFEFE9F1,false);String count=current+" / "+target+(done?"  ✓":"");g.drawString(font,count,x+width-font.width(count)-11,top+7,done?0xFF9ED8B0:0xFFE4C799,false);
            var lines=font.split(Component.literal(card.get("description").getAsString()),width-22);for(int n=0;n<Math.min(2,lines.size());n++)g.drawString(font,lines.get(n),x+10,top+23+n*11,0xFFAAA3B3,false);
            g.fill(x+10,top+54,x+width-10,top+58,0xAA101218);g.fill(x+10,top+54,x+10+(width-20)*Math.min(current,target)/target,top+58,done?0xFF87C99F:0xFFD4AD6C);
        }
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
}
