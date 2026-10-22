package pl.crimeandlaw.network;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.Varint21LengthFieldPrepender;

class MessageChunksTest {
    @Test void detailedMessagesSurviveRealFrameEncodingWithoutOversizedPackets(){
        String message="x".repeat(4_000_000)+"Zażółć gęślą jaźń 💎".repeat(1000);
        var assembler=new MessageChunks.Assembler();String complete=null;
        for(var part:MessageChunks.split(message,6_000_000)){
            var raw=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
            GangNetwork.ActionChunk.CODEC.encode(raw,new GangNetwork.ActionChunk(part));
            assertTrue(raw.readableBytes()<32_767);
            var copy=new RegistryFriendlyByteBuf(raw.copy(),RegistryAccess.EMPTY);GangNetwork.ActionChunk decoded;
            try{decoded=GangNetwork.ActionChunk.CODEC.decode(copy);}finally{copy.release();}
            assertArrayEquals(part.data(),decoded.part().data());
            var channel=new EmbeddedChannel(new Varint21LengthFieldPrepender());
            try{assertTrue(channel.writeOutbound(raw));ByteBuf frame=channel.readOutbound();try{assertTrue(frame.readableBytes()<32_767);}finally{frame.release();}}
            finally{channel.finishAndReleaseAll();}
            complete=assembler.accept(decoded.part(),6_000_000,part.offset());
        }
        assertEquals(message,complete);
    }
    @Test void invalidOrderOrTransferIdentityNeverDeliversPartialData(){
        var parts=MessageChunks.split("a".repeat(MessageChunks.PART_BYTES*2+5),100_000);var assembler=new MessageChunks.Assembler();
        assertNull(assembler.accept(parts.get(0),100_000,1));
        assertThrows(IllegalArgumentException.class,()->assembler.accept(parts.get(2),100_000,2));
        assertNull(assembler.accept(parts.get(0),100_000,3));
        var second=parts.get(1);var wrong=new MessageChunks.Part(UUID.randomUUID(),second.length(),second.offset(),second.data());
        assertThrows(IllegalArgumentException.class,()->assembler.accept(wrong,100_000,4));
        String result=null;for(var part:parts)result=assembler.accept(part,100_000,5);assertEquals("a".repeat(MessageChunks.PART_BYTES*2+5),result);
    }
    @Test void sizeLimitsAreCheckedBeforeGrowingBuffersAndMalformedUtf8IsRejected(){
        var assembler=new MessageChunks.Assembler();
        assertThrows(IllegalArgumentException.class,()->assembler.accept(new MessageChunks.Part(UUID.randomUUID(),1_000_001,0,new byte[]{1}),1_000_000,0));
        assertThrows(IllegalArgumentException.class,()->new MessageChunks.Part(UUID.randomUUID(),20,Integer.MAX_VALUE,new byte[]{1}));
        assertThrows(IllegalArgumentException.class,()->new MessageChunks.Part(UUID.randomUUID(),MessageChunks.PART_BYTES+1,0,new byte[MessageChunks.PART_BYTES+1]));
        assertThrows(IllegalArgumentException.class,()->assembler.accept(new MessageChunks.Part(UUID.randomUUID(),1,0,new byte[]{(byte)0xFF}),100,1));
        assertEquals("OK",assembler.accept(MessageChunks.split("OK",100).getFirst(),100,2));
    }
    @Test void incompleteTransfersExpireAndFreshUploadsCanResume(){
        var parts=MessageChunks.split("a".repeat(30_000),40_000);var assembler=new MessageChunks.Assembler();
        assertNull(assembler.accept(parts.getFirst(),40_000,0));assertTrue(assembler.expire(MessageChunks.IDLE_NANOS+1));
        assertThrows(IllegalArgumentException.class,()->assembler.accept(parts.getLast(),40_000,MessageChunks.IDLE_NANOS+2));
        assertNull(assembler.accept(parts.getFirst(),40_000,MessageChunks.IDLE_NANOS+3));
        assertEquals("a".repeat(30_000),assembler.accept(parts.getLast(),40_000,MessageChunks.IDLE_NANOS+4));
    }
    @Test void differentConnectionsCannotMixTheirTransferBuffers(){
        var one=new MessageChunks.Assembler();var two=new MessageChunks.Assembler();
        var first=MessageChunks.split("A".repeat(40_000),50_000);var second=MessageChunks.split("B".repeat(40_000),50_000);
        assertNull(one.accept(first.getFirst(),50_000,0));assertNull(two.accept(second.getFirst(),50_000,0));
        assertEquals("A".repeat(40_000),one.accept(first.getLast(),50_000,1));assertEquals("B".repeat(40_000),two.accept(second.getLast(),50_000,1));
    }
}
