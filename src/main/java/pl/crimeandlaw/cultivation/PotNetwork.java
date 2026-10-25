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

public final class PotNetwork {
    public static final int CLOSE=0,POUR=1,WATER=2,FINISH=3,CANCEL=4,PLANT=5,UNPACK=6,HARVEST=7,RECOVER=8,CUT=9,DROP=10,COVER=11;
    public static Consumer<State> receiver=s->{};
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String s){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("crimeandlaw",s));}
    public record Open(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Open> TYPE=PotNetwork.type("pot_open");
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->b.writeBlockPos(p.pos),b->new Open(b.readBlockPos()));
        @Override public Type<Open> type(){return TYPE;}
    }
    public record Action(UUID token,UUID revision,int action,int slot) implements CustomPacketPayload {
        public static final Type<Action> TYPE=PotNetwork.type("pot_action");
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeUUID(p.revision);b.writeVarInt(p.action);b.writeVarInt(p.slot);},b->new Action(b.readUUID(),b.readUUID(),b.readVarInt(),b.readVarInt()));
        @Override public Type<Action> type(){return TYPE;}
    }
    public record State(BlockPos pos,UUID token,UUID revision,int stage,int growth,int water,int leaves,int total,boolean frame,boolean pot,boolean lamp,boolean lit,boolean active,int operation,int work,int slot,String message) implements CustomPacketPayload {
        public static final Type<State> TYPE=PotNetwork.type("pot_state");
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeUUID(p.token);b.writeUUID(p.revision);b.writeVarInt(p.stage);b.writeVarInt(p.growth);b.writeVarInt(p.water);b.writeVarInt(p.leaves);b.writeVarInt(p.total);b.writeBoolean(p.frame);b.writeBoolean(p.pot);b.writeBoolean(p.lamp);b.writeBoolean(p.lit);b.writeBoolean(p.active);b.writeVarInt(p.operation);b.writeVarInt(p.work);b.writeVarInt(p.slot);b.writeUtf(p.message,200);},b->new State(b.readBlockPos(),b.readUUID(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(200)));
        @Override public Type<State> type(){return TYPE;}
    }
    public record Control(UUID token,UUID revision,int sequence,float x,float y,float z,float roll,float pitch,boolean held) implements CustomPacketPayload {
        public static final Type<Control> TYPE=PotNetwork.type("pot_control");
        public static final StreamCodec<RegistryFriendlyByteBuf,Control> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeUUID(p.revision);b.writeVarInt(p.sequence);b.writeFloat(p.x);b.writeFloat(p.y);b.writeFloat(p.z);b.writeFloat(p.roll);b.writeFloat(p.pitch);b.writeBoolean(p.held);},b->new Control(b.readUUID(),b.readUUID(),b.readVarInt(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readBoolean()));
        @Override public Type<Control> type(){return TYPE;}
    }
    public static void control(State s,int seq,PreparationPhysics.Pose p,boolean held){PacketDistributor.sendToServer(new Control(s.token,s.revision,seq,(float)p.x(),(float)p.y(),(float)p.z(),(float)p.roll(),(float)p.pitch(),held));}
    public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("25");r.playToServer(Open.TYPE,Open.CODEC,(p,c)->{if(c.player() instanceof ServerPlayer s)PotController.open(s,p.pos);});r.playToServer(Action.TYPE,Action.CODEC,(p,c)->{if(c.player() instanceof ServerPlayer s)PotController.act(s,p);});r.playToServer(Control.TYPE,Control.CODEC,(p,c)->{if(c.player() instanceof ServerPlayer s)PotController.control(s,p);});r.playToClient(State.TYPE,State.CODEC,(p,c)->receiver.accept(p));}
    public static void open(BlockPos pos){PacketDistributor.sendToServer(new Open(pos));}
    public static void action(State s,int action,int slot){PacketDistributor.sendToServer(new Action(s.token,s.revision,action,slot));}
    public static void send(ServerPlayer p,State s){PacketDistributor.sendToPlayer(p,s);}
}
