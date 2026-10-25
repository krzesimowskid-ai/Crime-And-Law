package pl.crimeandlaw.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.GangFinances;
import pl.crimeandlaw.network.GangNetwork;

abstract class FinanceResponseScreen extends MenuDialog {
    protected boolean pending;
    private String submittedToken="";
    private long awaitingSince;
    private final java.util.UUID gangId;
    FinanceResponseScreen(GangScreen parent,String title){super(parent,title);gangId=ClientState.gang().id;}
    protected EditBox field(int x,int y,int width,int max,String value,java.util.function.Consumer<String> changed){
        var field=new EditBox(font,x,y,width,20,Component.literal("Wartość"));field.setMaxLength(max);field.setValue(value);field.setResponder(changed);addRenderableWidget(field);return field;
    }
    protected long parsed(String value){try{return GangFinances.parseInput(value);}catch(IllegalArgumentException e){return 0;}}
    protected void submit(String action,JsonObject args){
        if(pending)return;
        if(minecraft.getConnection()==null||!minecraft.getConnection().hasChannel(GangNetwork.Action.TYPE)){ClientState.message="Brak połączenia z serwerem Crime And Law.";return;}
        submittedToken=FinanceClient.token();args.addProperty("financeToken",submittedToken);pending=true;awaitingSince=System.nanoTime();
        ClientState.action(action,args);rebuildWidgets();
    }
    @Override public void tick(){
        super.tick();
        if(pending&&System.nanoTime()-awaitingSince>=5_000_000_000L){
            awaitingSince=System.nanoTime();var request=new JsonObject();request.addProperty("financeToken",submittedToken);
            // Recover the authoritative result and invalidate an unprocessed request; never resend money.
            ClientState.action("finance_recover",request);
        }
    }
    public void updated(){
        var gang=ClientState.gang();
        if(gang==null||!gang.id.equals(gangId)){onClose();parent.updated();return;}
        if(pending&&!submittedToken.equals(FinanceClient.token())){
            pending=false;if(ClientState.message.equals("Zapisano.")){onClose();parent.updated();return;}
        }
        rebuildWidgets();
    }
}
