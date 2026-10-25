package pl.crimeandlaw.client.cultivation;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import pl.crimeandlaw.client.FlatButton;
import pl.crimeandlaw.cultivation.*;
import pl.crimeandlaw.gang.GangFinances;
public final class GardenShopScreen extends GardenScreen {
    private ShopNetwork.State state;private UUID pending;private long awaitingSince;private int selected,scroll;private FlatButton buy;
    public GardenShopScreen(ShopNetwork.State s){super("Ogrodnik");state=s;}
    public UUID token(){return state.session();}public ShopNetwork.State state(){return state;}
    public void receive(ShopNetwork.State s){if(pending!=null&&!pending.equals(s.quote()))pending=null;state=s;}
    @Override protected void init(){super.init();buy=button(535,380,300,"Kup za gotówkę",()->{if(pending==null){pending=state.quote();awaitingSince=System.nanoTime();ShopNetwork.request(state,ShopNetwork.BUY,selected);}});button(535,462,300,"Zamknij sklep",this::onClose);}
    @Override public void tick(){if(pending!=null&&System.nanoTime()-awaitingSince>3_000_000_000L){awaitingSince=System.nanoTime();ShopNetwork.request(state,ShopNetwork.RECOVER,-1);}}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        background(g,"O G R O D N I K","NASIONA  /  PODŁOŻE  /  WYPOSAŻENIE");int x=(int)left,y=(int)top;
        label(g,"GOTÓWKA  "+GangFinances.format(state.cash()),535,82,GREEN);
        for(int i=0;i<6;i++){int index=scroll+i;if(index>=GardenCatalog.OFFERS.size())break;var offer=GardenCatalog.OFFERS.get(index);int yy=y+84+i*62;boolean hover=mx/scale>=x+28&&mx/scale<x+460&&my/scale>=yy&&my/scale<yy+57;g.fill(x+28,yy,x+460,yy+57,index==selected?0xFF314C3C:hover?0xFF263D34:0x7020312D);if(index==selected)g.fill(x+28,yy,x+30,yy+57,0xFFBED78F);g.renderItem(offer.stack(),x+42,yy+18);label(g,offer.stack().getHoverName().getString(),72,yy-y+10,INK);label(g,offer.detail(),72,yy-y+32,MUTED);String price=GangFinances.format(offer.price());label(g,price,447-font.width(text(price)),yy-y+10,GREEN);}
        int track=y+84,bar=372*6/GardenCatalog.OFFERS.size();g.fill(x+469,track,x+472,track+372,0xFF253A33);int sy=track+(372-bar)*scroll/(GardenCatalog.OFFERS.size()-6);g.fill(x+469,sy,x+472,sy+bar,0xFF8BA579);label(g,"Przewiń listę kółkiem myszy",28,479,MUTED);
        var offer=GardenCatalog.OFFERS.get(selected);g.flush();RenderSystem.enableDepthTest();RenderSystem.depthMask(true);var buffers=minecraft.renderBuffers().bufferSource();g.pose().pushPose();g.pose().translate(x+680,y+272,180);g.pose().scale(155,-155,155);g.pose().mulPose(Axis.XP.rotation(.30f));g.pose().mulPose(Axis.YP.rotation(-.52f));g.pose().translate(-.5,0,-.5);new CultivationItemRenderer().renderByItem(offer.stack(),net.minecraft.world.item.ItemDisplayContext.GUI,g.pose(),buffers,LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);buffers.endBatch();g.pose().popPose();RenderSystem.clear(256,Minecraft.ON_OSX);RenderSystem.disableDepthTest();
        wrapped(g,offer.stack().getHoverName().getString(),535,306,300,INK,2);wrapped(g,offer.detail(),535,342,300,MUTED,2);
        buy.active=pending==null&&state.active()&&state.cash()>=offer.price();buy.setMessage(net.minecraft.network.chat.Component.literal(pending!=null?"Potwierdzanie zakupu…":"Kup • "+GangFinances.format(offer.price())));
        wrapped(g,state.message(),535,421,300,GREEN,2);end(g,mx,my,delta);
    }
    @Override public boolean mouseClicked(double x,double y,int b){if(super.mouseClicked(x,y,b))return true;double lx=x/scale-left,ly=y/scale-top;if(b==0&&lx>=28&&lx<460&&ly>=84&&ly<456){selected=Math.min(GardenCatalog.OFFERS.size()-1,scroll+(int)((ly-84)/62));return true;}return false;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll=Math.clamp(scroll+(vertical>0?-1:1),0,GardenCatalog.OFFERS.size()-6);return true;}
    @Override public void removed(){if(state.active()&&minecraft.getConnection()!=null)ShopNetwork.request(state,ShopNetwork.CLOSE,-1);}
}
