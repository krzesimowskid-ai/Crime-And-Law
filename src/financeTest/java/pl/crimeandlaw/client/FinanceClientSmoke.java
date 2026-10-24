package pl.crimeandlaw.client;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import pl.crimeandlaw.network.GangNetwork;

@EventBusSubscriber(modid="crimeandlaw",value=Dist.CLIENT)
public final class FinanceClientSmoke {
    private static final String ROLE=System.getProperty("crimeandlaw.financeRole","");
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.financeOutput","build/finance-smoke"));
    private static final UUID MEMBER=id("Member29"),FRIEND=id("Friend29");
    private static long start;private static int frames,step=-1;private static boolean done;
    private static JsonObject previous;private static String pending="",paidToken="";
    private static UUID id(String name){return UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(ROLE.isEmpty()||done)return;var mc=Minecraft.getInstance();
        try{
            if(start==0){start=System.nanoTime();Files.createDirectories(OUT);}
            check(System.nanoTime()-start<360_000_000_000L,"Client timeout stage "+step+" / "+ClientState.message);
            if(++frames%6!=0)return;
            if(!pending.isEmpty()){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(ROLE+"-"+pending+".png"));if(pending.equals("hud-clean-scale2"))checkIcons(mc,image);}pending="";}
            if(mc.screen instanceof AccessibilityOnboardingScreen onboarding){mc.options.onboardingAccessibilityFinished();onboarding.onClose();return;}
            if(mc.screen instanceof DisconnectedScreen)throw new AssertionError("Disconnected at stage "+step);
            if(step<0){
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                mc.options.guiScale().set(2);mc.options.framerateLimit().set(30);mc.resizeDisplay();
                ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25595"),new ServerData("Finance integration","127.0.0.1:25595",ServerData.Type.OTHER),false,null);step=0;return;
            }
            if(mc.player==null||mc.level==null)return;
            if(step==0){mc.setScreen(new GangScreen());previous=ClientState.state;ClientState.refresh("",0);step=1;return;}
            if(ClientState.gang()==null||ClientState.state==previous&&step==1)return;
            if(ROLE.equals("owner"))owner(mc);else member(mc);
        }catch(Throwable error){error.printStackTrace();done=true;try{Files.writeString(OUT.resolve(ROLE+".FAIL"),error+" stage="+step+" message="+ClientState.message);}catch(Exception ignored){}mc.stop();}
    }
    private static void owner(Minecraft mc)throws Exception{
        switch(step){
            case 1->{press(mc,"Saldo gangu");step=2;}
            case 2->{if(!kind("balance"))return;press(mc,"Wpłać");fields(mc,"1000","Wpłata z panelu");pending="deposit-dialog";step=3;}
            case 3->{press(mc,"Wpłać");step=4;}
            case 4->{if(FinanceClient.money("walletBalance")!=19000)return;check(FinanceClient.money("gangBalance")==81000,"Deposit authoritative");pending="balance";step=5;}
            case 5->{press(mc,"Wypłać");fields(mc,"500","Wypłata z panelu");step=6;}
            case 6->{press(mc,"Wypłać");step=7;}
            case 7->{if(FinanceClient.money("walletBalance")!=19500)return;check(FinanceClient.money("gangBalance")==80500,"Withdrawal authoritative");press(mc,"Premie");step=8;}
            case 8->{if(!kind("bonuses"))return;press(mc,"Wypłać premię");step=9;}
            case 9->{if(!FinanceClient.data().has("candidates"))return;press(mc,"Member29");fields(mc,"","250","Za capt");pending="single-bonus";step=10;}
            case 10->{press(mc,"Wypłać");step=11;}
            case 11->{if(FinanceClient.money("gangBalance")!=80250)return;press(mc,"Premia masowa");step=12;}
            case 12->{press(mc,"Member29");press(mc,"Friend29");fields(mc,"","300","Premia zespołu");paidToken=FinanceClient.token();pending="mass-bonus";step=13;}
            case 13->{press(mc,"Wypłać");step=14;}
            case 14->{if(FinanceClient.money("gangBalance")!=79650)return;pending="bonuses";var replay=payments(paidToken,300);previous=ClientState.state;GangNetwork.send(replay);step=15;}
            case 15->{if(ClientState.state==previous)return;check(ClientState.message.contains("Odśwież")&&FinanceClient.money("gangBalance")==79650,"Replay rejected");badWithdraw(1_000_000_000_000L);step=16;}
            case 16->{if(ClientState.state==previous)return;check(FinanceClient.money("gangBalance")==79650,"Overdraft rejected");badWithdraw(-1);step=17;}
            case 17->{if(ClientState.state==previous)return;check(FinanceClient.money("gangBalance")==79650,"Negative amount rejected");badWithdraw(0.5);step=18;}
            case 18->{if(ClientState.state==previous)return;check(FinanceClient.money("gangBalance")==79650,"Fraction rejected");var request=payments(FinanceClient.token(),1);request.getAsJsonArray("payments").add(request.getAsJsonArray("payments").get(0).deepCopy());previous=ClientState.state;GangNetwork.send(request);step=19;}
            case 19->{if(ClientState.state==previous)return;check(ClientState.message.contains("więcej niż raz")&&FinanceClient.money("gangBalance")==79650,"Duplicate recipient rejected");Files.writeString(OUT.resolve("owner-payments.PASS"),"Owner UI and raw invalid packet checks pass\n");step=20;}
            case 20->{if(!Files.exists(OUT.resolve("member.PASS")))return;ClientState.message="";mc.setScreen(null);pending="hud-scale2";step=21;}
            case 21->{check(HudMoney.ready&&HudMoney.cash==1235&&HudMoney.bank==19500,"HUD receives actual independent accounts");check(ClientMinimap.cacheSize()>0&&ClientMinimap.cacheSize()<=256,"Minimap samples loaded terrain within its cache bound");mc.options.guiScale().set(4);mc.resizeDisplay();pending="hud-scale4";step=22;}
            case 22->{mc.setScreen(new GangScreen());previous=ClientState.state;ClientState.refresh("",0);step=23;}
            case 23->{if(ClientState.state==previous)return;press(mc,"Czarna lista");step=24;}
            case 24->{if(!ClientState.state.has("blacklist"))return;press(mc,"Dodaj do listy");fields(mc,"Friend29","Test blokady");pending="blacklist-add";step=25;}
            case 25->{press(mc,"Dodaj do listy");step=26;}
            case 26->{if(ClientState.memberCount()!=2)return;var data=ClientState.state.getAsJsonObject("blacklist");check(data.getAsJsonArray("entries").size()==1,"Blacklist operation removes a current member");pending="blacklist-list";step=27;}
            case 27->{var table=mc.screen.children().stream().filter(BlacklistTable.class::isInstance).map(BlacklistTable.class::cast).findFirst().orElseThrow();table.mouseClicked(table.getX()+table.getWidth()-26,table.getY()+35,0);check(mc.screen instanceof ConfirmScreen,"Removing the blacklist entry opens confirmation");press(mc,"Yes");step=28;}
            case 28->{if(!ClientState.state.getAsJsonObject("blacklist").getAsJsonArray("entries").isEmpty())return;press(mc,"Historia");step=29;}
            case 29->{var data=ClientState.state.getAsJsonObject("blacklist");if(!data.get("history").getAsBoolean())return;check(data.getAsJsonArray("entries").size()==2,"Blacklist history shows add and remove");pending="blacklist-history";step=30;}
            case 30->{press(mc,"Cele");pending="weekly-goals";step=31;}
            case 31->{check(ClientState.state.getAsJsonObject("goals").getAsJsonArray("cards").size()==3,"Three current-mechanic goals are visible");mc.setScreen(null);HudMoney.receive(new GangNetwork.Money(1_000_000_000_000L,1_000_000_000_000L));pending="hud-max-values";step=32;}
            case 32->{HudMoney.receive(new GangNetwork.Money(148840,69420));look(mc,180);pending="hud-clean-north";step=33;}
            case 33->{look(mc,90);pending="hud-clean-west";step=34;}
            case 34->{look(mc,0);pending="hud-clean-south";step=35;}
            case 35->{look(mc,-90);pending="hud-clean-east";step=36;}
            case 36->{mc.options.guiScale().set(2);mc.resizeDisplay();pending="hud-clean-scale2";step=37;}
            case 37->{mc.options.hideGui=true;pending="hud-hidden-f1";step=38;}
            case 38->{mc.options.hideGui=false;mc.setScreen(new ChatScreen(""));pending="hud-open-chat";step=39;}
            case 39->{mc.setScreen(null);HudMoney.ready=false;pending="hud-loading-balances";step=40;}
            case 40->{HudMoney.receive(new GangNetwork.Money(1235,19500));Files.writeString(OUT.resolve("owner.PASS"),"Financial TCP checks; actual cash/bank HUD at GUI 2/4; synthetic client-only representative and maximum values; four compass headings; F1, open chat, loading state; live minimap, blacklist, goals.\n");done=true;mc.stop();}
        }
    }
    private static void look(Minecraft mc,float yaw){mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(38);mc.player.xRotO=38;}
    private static void checkIcons(Minecraft mc,com.mojang.blaze3d.platform.NativeImage image){
        var layout=PlayerHudLayout.of(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
        double scale=layout.canvas().scale()*image.getWidth()/mc.getWindow().getGuiScaledWidth();
        for(int row=0;row<2;row++){int count=0,x=(int)(layout.moneyX()*scale),y=(int)((layout.moneyY()+24*row)*scale);
            for(int dy=0;dy<22*scale;dy++)for(int dx=0;dx<22*scale;dx++){int c=image.getPixelRGBA(x+dx,y+dy),r=c&255,g=c>>>8&255,b=c>>>16&255;if(g>160&&b>150&&r>130)count++;}
            check(count>80,"Actual rendered "+(row==0?"banknote":"credit card")+" must contain visible vector pixels: "+count);
        }
    }
    private static void member(Minecraft mc)throws Exception{
        switch(step){
            case 1->{press(mc,"Premie");step=2;}
            case 2->{if(!kind("bonuses"))return;check(!button(mc,"Wypłać premię").active&&!button(mc,"Premia masowa").active,"Missing bonus permission disables controls");pending="disabled-bonuses";step=3;}
            case 3->{if(!Files.exists(OUT.resolve("owner-payments.PASS")))return;check(FinanceClient.money("walletBalance")==550&&FinanceClient.money("gangBalance")==79650,"Member receives individual and mass bonuses");var request=payments(FinanceClient.token(),1);request.getAsJsonArray("payments").remove(0);previous=ClientState.state;GangNetwork.send(request);step=4;}
            case 4->{if(ClientState.state==previous)return;check(ClientState.message.contains("uprawnień")&&FinanceClient.money("gangBalance")==79650,"Forged bonus request denied");press(mc,"Saldo gangu");step=5;}
            case 5->{if(!kind("balance"))return;check(button(mc,"Wpłać").active&&!button(mc,"Wypłać").active,"Deposit and withdrawal permissions are separate");press(mc,"Wpłać");fields(mc,"50","Zwrot do gangu");pending="member-deposit";step=6;}
            case 6->{press(mc,"Wpłać");step=7;}
            case 7->{if(FinanceClient.money("walletBalance")!=500)return;check(FinanceClient.money("gangBalance")==79700,"Member deposit authoritative");Files.writeString(OUT.resolve("member-death.ready"),"ready");step=8;}
            case 8->{if(!mc.player.isDeadOrDying())return;mc.player.respawn();mc.setScreen(null);step=9;}
            case 9->{if(mc.player.isDeadOrDying())return;check(HudMoney.cash==567&&HudMoney.bank==500,"Actual death and respawn preserve both balances");Files.writeString(OUT.resolve("member.PASS"),"Permissions, received bonuses, authorized bank deposit, actual death/respawn and cash/bank synchronization.\n");done=true;mc.stop();}
        }
    }
    private static void badWithdraw(Number amount){var request=new JsonObject();request.addProperty("action","finance_withdraw");request.addProperty("amount",amount);request.addProperty("comment","");request.addProperty("financeToken",FinanceClient.token());previous=ClientState.state;GangNetwork.send(request);}
    private static JsonObject payments(String token,long amount){
        var request=new JsonObject();request.addProperty("action","finance_pay");request.addProperty("financeToken",token);request.addProperty("comment","Premia zespołu");var rows=new JsonArray();
        for(UUID id:List.of(MEMBER,FRIEND)){var row=new JsonObject();row.addProperty("recipient",id.toString());row.addProperty("amount",amount);rows.add(row);}request.add("payments",rows);return request;
    }
    private static boolean kind(String kind){return FinanceClient.data().has("kind")&&kind.equals(FinanceClient.data().get("kind").getAsString());}
    private static Button button(Minecraft mc,String text){return mc.screen.children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().contains(text)).map(c->(Button)c).findFirst().orElseThrow(()->new AssertionError("Missing button "+text+" stage="+step+" screen="+mc.screen));}
    private static void press(Minecraft mc,String text){var button=button(mc,text);check(button.active,"Disabled button "+text+" stage="+step);button.onPress();}
    private static void fields(Minecraft mc,String... values){var fields=mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();check(fields.size()==values.length,"Field count");for(int i=0;i<values.length;i++)fields.get(i).setValue(values[i]);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
