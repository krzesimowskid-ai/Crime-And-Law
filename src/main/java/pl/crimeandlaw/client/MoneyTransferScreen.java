package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import pl.crimeandlaw.gang.GangDirectory;
import pl.crimeandlaw.gang.GangDirectory.Permission;
import pl.crimeandlaw.gang.GangFinances;

final class MoneyTransferScreen extends FinanceResponseScreen {
    private final boolean deposit;
    private String amount="",comment="";
    private FlatButton pay;
    MoneyTransferScreen(GangScreen parent,boolean deposit){super(parent,deposit?"Wpłata na saldo gangu":"Wypłata z salda gangu");this.deposit=deposit;panelHeight=252;}
    private boolean allowed(){return GangDirectory.allowed(ClientState.gang(),ClientState.player(),deposit?Permission.DEPOSIT_FUNDS:Permission.WITHDRAW_FUNDS);}
    private void updateButton(){if(pay!=null)pay.active=!pending&&allowed()&&parsed(amount)>0&&parsed(amount)<=FinanceClient.money(deposit?"walletBalance":"gangBalance");}
    @Override protected void build(){
        pay=null;
        field(left+18,top+95,220,17,amount,value->{amount=value;updateButton();});
        field(left+18,top+143,panelWidth-36,128,comment,value->comment=value);
        pay=button(left+panelWidth-154,top+191,136,pending?"Wysyłanie…":deposit?"Wpłać":"Wypłać",b->{
            var args=new JsonObject();args.addProperty("amount",parsed(amount));args.addProperty("comment",comment);submit(deposit?"finance_deposit":"finance_withdraw",args);
        });
        button(left+18,top+191,100,"Anuluj",b->onClose());updateButton();
    }
    @Override protected void paint(GuiGraphics g,int x,int y,float delta){
        g.drawString(font,"Bank: "+GangFinances.format(FinanceClient.money("walletBalance")),left+18,top+49,0xFFE9E8EC,false);
        g.drawString(font,"Saldo gangu: "+GangFinances.format(FinanceClient.money("gangBalance")),left+270,top+49,0xFFE9E8EC,false);
        g.drawString(font,"Kwota ($)",left+18,top+80,0xFFAAA3B3,false);g.drawString(font,"Komentarz (opcjonalnie)",left+18,top+128,0xFFAAA3B3,false);
        String status=!allowed()?"Nie masz uprawnienia do tej czynności.":ClientState.message;
        if(allowed()&&parsed(amount)>FinanceClient.money(deposit?"walletBalance":"gangBalance"))status="Brak wystarczających środków.";
        g.drawString(font,font.plainSubstrByWidth(status,panelWidth-36),left+18,top+229,0xFFD8BBC8,false);
    }
}
