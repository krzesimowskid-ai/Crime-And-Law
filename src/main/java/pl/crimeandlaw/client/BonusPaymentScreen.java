package pl.crimeandlaw.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import pl.crimeandlaw.gang.GangDirectory;
import pl.crimeandlaw.gang.GangDirectory.Permission;
import pl.crimeandlaw.gang.GangFinances;

/** A paged recipient picker handles offline members without sending an unbounded roster. */
final class BonusPaymentScreen extends FinanceResponseScreen {
    private final boolean multiple;
    private final Map<UUID,String> selected=new LinkedHashMap<>();
    private String amount="",comment="",query="";
    private FlatButton pay;
    BonusPaymentScreen(GangScreen parent,boolean multiple){super(parent,multiple?"Premia masowa":"Wypłać premię");this.multiple=multiple;}
    private boolean allowed(){return GangDirectory.allowed(ClientState.gang(),ClientState.player(),Permission.PAY_BONUSES);}
    private long total(){long value=parsed(amount);return value>0&&selected.size()<=GangFinances.MAX_BALANCE/value?value*selected.size():0;}
    private void updateButton(){if(pay!=null)pay.active=!pending&&allowed()&&!selected.isEmpty()&&total()>0&&total()<=FinanceClient.money("gangBalance");}
    @Override protected void build(){
        pay=null;
        field(left+16,top+54,192,32,query,value->query=value);
        button(left+214,top+54,64,"Szukaj",b->FinanceClient.candidates(query,0));
        var data=FinanceClient.data();var candidates=data.has("candidates")?data.getAsJsonArray("candidates"):new JsonArray();
        int index=0;
        for(var candidate:candidates){
            var row=candidate.getAsJsonObject();UUID id=UUID.fromString(row.get("id").getAsString());String name=row.get("name").getAsString();
            boolean chosen=selected.containsKey(id);
            var choice=button(left+16,top+85+index++*25,262,(chosen?"✓ ":"  ")+name+" · "+row.get("role").getAsString(),b->{
                if(selected.containsKey(id))selected.remove(id);else{if(!multiple)selected.clear();if(selected.size()<GangFinances.MAX_RECIPIENTS)selected.put(id,name);}rebuildWidgets();
            });choice.active=!pending;
        }
        int page=FinanceClient.number("candidatePage",0),pages=FinanceClient.number("candidatePages",1);
        button(left+16,top+242,32,"‹",b->FinanceClient.candidates(query,page-1)).active=!pending&&page>0;
        button(left+52,top+242,32,"›",b->FinanceClient.candidates(query,page+1)).active=!pending&&page+1<pages;
        field(left+302,top+83,200,17,amount,value->{amount=value;updateButton();});
        field(left+302,top+133,200,128,comment,value->comment=value);
        pay=button(left+302,top+242,200,pending?"Wysyłanie…":"Wypłać",b->{
            var args=new JsonObject();var payments=new JsonArray();
            for(UUID id:selected.keySet()){var row=new JsonObject();row.addProperty("recipient",id.toString());row.addProperty("amount",parsed(amount));payments.add(row);}
            args.add("payments",payments);args.addProperty("comment",comment);submit("finance_pay",args);
        });
        button(left+16,top+293,100,"Anuluj",b->onClose());updateButton();
    }
    @Override protected void paint(GuiGraphics g,int x,int y,float delta){
        g.drawString(font,"Odbiorcy",left+16,top+40,0xFFAAA3B3,false);
        g.drawString(font,"Kwota na osobę ($)",left+302,top+68,0xFFAAA3B3,false);g.drawString(font,"Komentarz",left+302,top+118,0xFFAAA3B3,false);
        g.drawString(font,"Wybrano: "+selected.size()+(multiple?" / 100":""),left+302,top+173,0xFFE9E8EC,false);
        g.drawString(font,"Razem: "+GangFinances.format(total()),left+302,top+194,0xFFE9C98B,false);
        g.drawString(font,"Saldo: "+GangFinances.format(FinanceClient.money("gangBalance")),left+302,top+214,0xFFAAA3B3,false);
        g.drawString(font,"Strona "+(FinanceClient.number("candidatePage",0)+1)+" / "+FinanceClient.number("candidatePages",1),left+96,top+249,0xFFAAA3B3,false);
        if(!FinanceClient.data().has("candidates")||FinanceClient.data().getAsJsonArray("candidates").isEmpty())g.drawString(font,"Brak pasujących członków.",left+20,top+100,0xFFAAA3B3,false);
        String message=!allowed()?"Nie masz uprawnienia do wypłacania premii.":!pending&&total()>FinanceClient.money("gangBalance")?"Brak środków na całą wypłatę.":ClientState.message;
        g.drawString(font,font.plainSubstrByWidth(message,panelWidth-36),left+18,top+278,0xFFD8BBC8,false);
        if(!selected.isEmpty())g.drawString(font,font.plainSubstrByWidth(String.join(", ",selected.values()),panelWidth-150),left+130,top+299,0xFFAAA3B3,false);
    }
}
