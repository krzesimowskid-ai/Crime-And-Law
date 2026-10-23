package pl.crimeandlaw.gang;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public final class GangLogo {
    public static final int SIDE=2048,MAX_BYTES=17_000_000;
    public static final int MAX_ENCODED_LENGTH=((MAX_BYTES+2)/3)*4;
    private GangLogo(){}
    public static String validate(String encoded){
        return validateImage(encoded,SIDE,SIDE,"Logo");
    }
    static String validateImage(String encoded,int maxWidth,int maxHeight,String label){
        return validateImage(encoded,maxWidth,maxHeight,MAX_BYTES,label);
    }
    static String validateImage(String encoded,int maxWidth,int maxHeight,int maxBytes,String label){
        return validateImage(encoded,maxWidth,maxHeight,maxBytes,label,maxWidth,maxHeight);
    }
    static String validateImage(String encoded,int maxWidth,int maxHeight,int maxBytes,String label,int outputWidth,int outputHeight){
        if(encoded==null || encoded.isEmpty())return "";
        if(encoded.length()>((maxBytes+2)/3)*4)throw new IllegalArgumentException("Przekroczono limit rozmiaru obrazu.");
        try{
            byte[] bytes=Base64.getDecoder().decode(encoded);
            if(bytes.length>maxBytes)throw new IOException();
            try(ImageInputStream in=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
                var readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw new IOException();
                var reader=readers.next();
                try{
                    reader.setInput(in);int w=reader.getWidth(0),h=reader.getHeight(0);
                    if(!reader.getFormatName().equalsIgnoreCase("png") || w<1 || h<1 || w>maxWidth || h>maxHeight)throw new IOException();
                    BufferedImage image=reader.read(0);ByteArrayOutputStream out=new ByteArrayOutputStream();
                    double scale=Math.min(1,Math.min(outputWidth/(double)w,outputHeight/(double)h));
                    if(scale<1){
                        var reduced=new BufferedImage(Math.max(1,(int)Math.round(w*scale)),Math.max(1,(int)Math.round(h*scale)),BufferedImage.TYPE_INT_ARGB);
                        var graphics=reduced.createGraphics();
                        try{graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(image,0,0,reduced.getWidth(),reduced.getHeight(),null);}
                        finally{graphics.dispose();}
                        image=reduced;
                    }
                    try(var output=new MemoryCacheImageOutputStream(out)){ImageIO.write(image,"png",output);}
                    if(out.size()>maxBytes)throw new IOException();
                    return Base64.getEncoder().encodeToString(out.toByteArray());
                }finally{reader.dispose();}
            }
        }catch(Exception exception){throw new IllegalArgumentException(label+" musi być poprawnym PNG do "+maxWidth+"×"+maxHeight+" i "+maxBytes/1_000_000+" MB.");}
    }
}
