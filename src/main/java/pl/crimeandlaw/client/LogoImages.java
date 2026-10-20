package pl.crimeandlaw.client;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.Set;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import pl.crimeandlaw.gang.GangLogo;

/** Decoding, preview downsampling and final encoding run outside rendering. */
public final class LogoImages {
    private LogoImages(){}
    public static BufferedImage read(Path path)throws IOException {
        if(!Files.isRegularFile(path))throw new IOException("Nie znaleziono pliku. Sprawdź ścieżkę i nazwę obrazu.");
        if(Files.size(path)>20_000_000)throw new IOException("Obraz przekracza 20 MB. Zapisz mniejszy plik PNG, JPG, GIF lub BMP.");
        try(var raw=Files.newInputStream(path);var in=new MemoryCacheImageInputStream(raw)){
            var readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())throw new IOException("Nie można odczytać obrazu. Wybierz prawdziwy plik PNG, JPG, GIF lub BMP, a nie sam zmieniony format nazwy.");
            var reader=readers.next();
            try {
                reader.setInput(in);String format=reader.getFormatName();
                if(!Set.of("png","jpeg","gif","bmp").contains(format.toLowerCase(Locale.ROOT)))throw new IOException("Ten format nie jest obsługiwany. Wybierz PNG, JPG, GIF lub BMP.");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1||height<1||width>16384||height>16384||(long)width*height>64_000_000)throw new IOException("Obraz ma zbyt duże wymiary. Zmniejsz go do maksymalnie 64 milionów pikseli.");
                var param=reader.getDefaultReadParam();int step=Math.max(1,(Math.max(width,height)+1023)/1024);
                param.setSourceSubsampling(step,step,0,0);return reader.read(0,param);
            }finally{reader.dispose();}
        }
    }
    public static byte[] png(BufferedImage image)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(var output=new MemoryCacheImageOutputStream(out)){if(!ImageIO.write(image,"png",output))throw new IOException("Nie można zapisać obrazu PNG.");}
        return out.toByteArray();
    }
    public static String encode(BufferedImage image,LogoCrop crop)throws IOException {
        BufferedImage target=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if(crop.fit()){
                double scale=Math.min(64.0/image.getWidth(),64.0/image.getHeight());
                int w=Math.max(1,(int)Math.round(image.getWidth()*scale)),h=Math.max(1,(int)Math.round(image.getHeight()*scale));
                g.drawImage(image,(64-w)/2,(64-h)/2,w,h,null);
            }else{
                int x1=(int)Math.floor(crop.x()),y1=(int)Math.floor(crop.y());
                int x2=Math.min(image.getWidth(),(int)Math.ceil(crop.x()+crop.side())),y2=Math.min(image.getHeight(),(int)Math.ceil(crop.y()+crop.side()));
                g.drawImage(image,0,0,64,64,x1,y1,x2,y2,null);
            }
        }finally{g.dispose();}
        return GangLogo.validate(Base64.getEncoder().encodeToString(png(target)));
    }
}
