package pl.crimeandlaw.cultivation;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CultivationNetwork {
    public static Consumer<State> receiver=s->{};
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("crimeandlaw",path));}
    public record Open(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Open> TYPE=CultivationNetwork.type("harvest_open");
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->b.writeBlockPos(p.pos),b->new Open(b.readBlockPos()));
        @Override public Type<Open> type(){return TYPE;}
    }
    public record Cut(UUID token,int leaf,int phase) implements CustomPacketPayload {
        public static final Type<Cut> TYPE=CultivationNetwork.type("harvest_cut");
        public static final StreamCodec<RegistryFriendlyByteBuf,Cut> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeVarInt(p.leaf);b.writeVarInt(p.phase);},b->new Cut(b.readUUID(),b.readVarInt(),b.readVarInt()));
        @Override public Type<Cut> type(){return TYPE;}
    }
    public record State(BlockPos pos,UUID token,int leaves,int total,int target,boolean holding,boolean active,String message) implements CustomPacketPayload {
        public State(BlockPos p,UUID t,int l,int target,boolean h,boolean a,String m){this(p,t,l,6,target,h,a,m);}
        public static final Type<State> TYPE=CultivationNetwork.type("harvest_state");
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeUUID(p.token);b.writeVarInt(p.leaves);b.writeVarInt(p.total);b.writeVarInt(p.target);b.writeBoolean(p.holding);b.writeBoolean(p.active);b.writeUtf(p.message,160);},b->new State(b.readBlockPos(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readUtf(160)));
        @Override public Type<State> type(){return TYPE;}
    }
    private CultivationNetwork(){}
    public static void register(RegisterPayloadHandlersEvent event){
        var r=event.registrar("25");
        r.playToServer(Open.TYPE,Open.CODEC,(p,c)->{if(c.player() instanceof ServerPlayer player)HarvestController.open(player,p.pos);});
        r.playToServer(Cut.TYPE,Cut.CODEC,(p,c)->{if(c.player() instanceof ServerPlayer player)HarvestController.cut(player,p);});
        r.playToClient(State.TYPE,State.CODEC,(p,c)->receiver.accept(p));
    }
    public static void send(ServerPlayer player,State state){PacketDistributor.sendToPlayer(player,state);}
    public static void open(BlockPos pos){PacketDistributor.sendToServer(new Open(pos));}
    public static void cut(UUID token,int leaf,int phase){PacketDistributor.sendToServer(new Cut(token,leaf,phase));}
}
