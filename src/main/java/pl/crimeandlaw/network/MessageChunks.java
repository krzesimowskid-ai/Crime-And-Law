package pl.crimeandlaw.network;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

/** Bounded, ordered application messages below Minecraft's physical packet limit. */
public final class MessageChunks {
    public static final int PART_BYTES=24*1024;
    public static final long IDLE_NANOS=60_000_000_000L,MAX_NANOS=300_000_000_000L;
    public record Part(UUID id,int length,int offset,byte[] data){
        public Part {
            if(id==null||length<1||offset<0||data==null||data.length<1||data.length>PART_BYTES||offset>length-data.length)
                throw new IllegalArgumentException("Nieprawidłowa część wiadomości.");
        }
    }
    private MessageChunks(){}
    public static List<Part> split(String message,int limit){
        byte[] bytes=message.getBytes(StandardCharsets.UTF_8);
        if(bytes.length<1||bytes.length>limit)throw new IllegalArgumentException("Wiadomość przekracza limit.");
        var id=UUID.randomUUID();List<Part> parts=new ArrayList<>();
        for(int at=0;at<bytes.length;at+=PART_BYTES)parts.add(new Part(id,bytes.length,at,Arrays.copyOfRange(bytes,at,Math.min(bytes.length,at+PART_BYTES))));
        return parts;
    }
    public static final class Assembler {
        private UUID id;private int length;private long started,last;
        private ByteArrayOutputStream bytes;
        public void clear(){id=null;bytes=null;length=0;started=last=0;}
        public boolean expire(long now){if(bytes!=null&&(now-last>IDLE_NANOS||now-started>MAX_NANOS)){clear();return true;}return false;}
        public String accept(Part part,int limit,long now){
            try{
                expire(now);
                if(part.length()>limit)throw new IllegalArgumentException("Wiadomość przekracza limit.");
                if(part.offset()==0){
                    if(bytes!=null)throw new IllegalArgumentException("Poprzednia wiadomość nie została zakończona.");
                    id=part.id();length=part.length();started=now;bytes=new ByteArrayOutputStream(Math.min(length,PART_BYTES));
                }
                if(bytes==null||!id.equals(part.id())||length!=part.length()||bytes.size()!=part.offset())
                    throw new IllegalArgumentException("Nieprawidłowa kolejność części wiadomości.");
                bytes.writeBytes(part.data());last=now;
                if(bytes.size()<length)return null;
                byte[] complete=bytes.toByteArray();clear();
                return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(complete)).toString();
            }catch(CharacterCodingException exception){clear();throw new IllegalArgumentException("Nieprawidłowy tekst wiadomości.");}
            catch(RuntimeException exception){clear();throw exception;}
        }
    }
}
