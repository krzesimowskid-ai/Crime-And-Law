package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

final class BlacklistAddScreen extends MenuDialog {
    private String player="",reason="";
    private boolean pending;
    private FlatButton save;
    BlacklistAddScreen(GangScreen parent){super(parent,"Dodaj do czarnej listy");panelHeight=245;}
    @Override protected void build(){
        save=null;
        field(top+75,36,player,value->{player=value;if(save!=null)save.active=!pending&&!player.isBlank();});
        field(top+126,128,reason,value->reason=value);
        save=button(left+panelWidth-166,top+173,148,pending?"Wysyłanie…":"Dodaj do listy",b->{
            if(pending)return;pending=true;var args=new JsonObject();args.addProperty("player",player);args.addProperty("reason",reason);ClientState.action("blacklist_add",args);rebuildWidgets();
        });save.active=!pending&&!player.isBlank();button(left+18,top+173,100,"Anuluj",b->onClose());
    }
    private void field(int y,int max,String value,java.util.function.Consumer<String> changed){var field=new EditBox(font,left+18,y,panelWidth-36,20,Component.literal("Wartość"));field.setMaxLength(max);field.setValue(value);field.setResponder(changed);addRenderableWidget(field);}
    public void updated(){if(ClientState.gang()==null||pending&&ClientState.message.equals("Zapisano.")){onClose();parent.updated();return;}if(!ClientState.message.isBlank())pending=false;rebuildWidgets();}
    @Override protected void paint(GuiGraphics g,int x,int y,float delta){
        g.drawString(font,"Nick znany serwerowi lub UUID gracza",left+18,top+58,0xFFAAA3B3,false);g.drawString(font,"Powód (opcjonalnie)",left+18,top+109,0xFFAAA3B3,false);
        g.drawString(font,"Obecny członek zostanie usunięty z gangu.",left+18,top+154,0xFFAAA3B3,false);g.drawString(font,font.plainSubstrByWidth(ClientState.message,panelWidth-36),left+18,top+218,0xFFD8BBC8,false);
    }
}
