package pl.crimeandlaw.gang;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public final class GangLogo {
    public static final int SIDE=512,MAX_BYTES=1_100_000;
    public static final int MAX_ENCODED_LENGTH=((MAX_BYTES+2)/3)*4;
    private GangLogo(){}
    public static String validate(String encoded){
        return validateImage(encoded,SIDE,SIDE,"Logo");
    }
    static String validateImage(String encoded,int maxWidth,int maxHeight,String label){
        if(encoded==null || encoded.isEmpty())return "";
        if(encoded.length()>MAX_ENCODED_LENGTH)throw new IllegalArgumentException(label+" jest zbyt duże.");
        try{
            byte[] bytes=Base64.getDecoder().decode(encoded);
            if(bytes.length>MAX_BYTES)throw new IOException();
            try(ImageInputStream in=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
                var readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw new IOException();
                var reader=readers.next();
                try{
                    reader.setInput(in);int w=reader.getWidth(0),h=reader.getHeight(0);
                    if(!reader.getFormatName().equalsIgnoreCase("png") || w<1 || h<1 || w>maxWidth || h>maxHeight)throw new IOException();
                    BufferedImage image=reader.read(0);ByteArrayOutputStream out=new ByteArrayOutputStream();
                    try(var output=new MemoryCacheImageOutputStream(out)){ImageIO.write(image,"png",output);}
                    if(out.size()>MAX_BYTES)throw new IOException();
                    return Base64.getEncoder().encodeToString(out.toByteArray());
                }finally{reader.dispose();}
            }
        }catch(Exception exception){throw new IllegalArgumentException(label+" musi być poprawnym PNG do "+maxWidth+"×"+maxHeight+" i 1,1 MB.");}
    }
}
