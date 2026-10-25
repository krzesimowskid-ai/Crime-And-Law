package pl.crimeandlaw.cultivation;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class ShopNetwork {
    public static final int BUY=0,RECOVER=1,CLOSE=2;public static Consumer<State> receiver=s->{};
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String s){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("crimeandlaw",s));}
    public record Request(UUID session,UUID quote,int action,int offer) implements CustomPacketPayload {
        public static final Type<Request> TYPE=ShopNetwork.type("garden_buy");
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.session);b.writeUUID(p.quote);b.writeVarInt(p.action);b.writeVarInt(p.offer);},b->new Request(b.readUUID(),b.readUUID(),b.readVarInt(),b.readVarInt()));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record State(UUID session,UUID quote,int merchant,long cash,boolean active,String message) implements CustomPacketPayload {
        public static final Type<State> TYPE=ShopNetwork.type("garden_shop");
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.session);b.writeUUID(p.quote);b.writeVarInt(p.merchant);b.writeLong(p.cash);b.writeBoolean(p.active);b.writeUtf(p.message,200);},b->new State(b.readUUID(),b.readUUID(),b.readVarInt(),b.readLong(),b.readBoolean(),b.readUtf(200)));
        @Override public Type<State> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("23");r.playToServer(Request.TYPE,Request.CODEC,(p,c)->{if(c.player() instanceof ServerPlayer s)ShopController.request(s,p);});r.playToClient(State.TYPE,State.CODEC,(p,c)->receiver.accept(p));}
    public static void request(State s,int action,int offer){PacketDistributor.sendToServer(new Request(s.session,s.quote,action,offer));}
    public static void send(ServerPlayer p,State s){PacketDistributor.sendToPlayer(p,s);}
}
