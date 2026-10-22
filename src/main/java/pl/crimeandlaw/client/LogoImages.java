package pl.crimeandlaw.client;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.*;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.xml.stream.XMLInputFactory;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.view.ViewBox;
import pl.crimeandlaw.gang.GangLogo;

/** Content-neutral decoding, preview downsampling and final encoding outside rendering. */
public final class LogoImages {
    public static final String FORMATS="PNG / JPG / GIF / BMP / WebP / SVG / TIFF / ICO / ICNS / PSD / TGA / PCX / PNM";
    public static final int MAX_FILE_BYTES=64_000_000,PREVIEW_SIDE=1024;
    private static final Set<IIORegistry> REGISTRIES=Collections.newSetFromMap(new WeakHashMap<>());
    private LogoImages(){}
    private static synchronized void registerReaders(){
        IIORegistry registry=IIORegistry.getDefaultInstance();
        if(!REGISTRIES.add(registry))return;
        // Explicit providers also work when NeoForge's module loader cannot discover
        // ImageIO service descriptors from the worker thread's context loader.
        for(ImageReaderSpi spi:List.of(
            new com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi(),
            new com.twelvemonkeys.imageio.plugins.bmp.BMPImageReaderSpi(),
            new com.twelvemonkeys.imageio.plugins.bmp.ICOImageReaderSpi(),
            new com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi(),
            new com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReaderSpi(),
            new com.twelvemonkeys.imageio.plugins.psd.PSDImageReaderSpi(),
            new com.twelvemonkeys.imageio.plugins.tga.TGAImageReaderSpi(),
            new com.twelvemonkeys.imageio.plugins.pcx.PCXImageReaderSpi(),
            new com.twelvemonkeys.imageio.plugins.pnm.PNMImageReaderSpi(),
            new com.twelvemonkeys.imageio.plugins.icns.ICNSImageReaderSpi()))registry.registerServiceProvider(spi,ImageReaderSpi.class);
    }
    public static BufferedImage read(Path path)throws IOException {
        if(!Files.isRegularFile(path))throw new IOException("Nie znaleziono pliku. Sprawdź ścieżkę i nazwę obrazu.");
        if(Files.size(path)>MAX_FILE_BYTES)throw new IOException("Obraz przekracza limit 64 MB.");
        byte[] bytes;
        try(var raw=Files.newInputStream(path)){bytes=raw.readNBytes(MAX_FILE_BYTES+1);}
        if(bytes.length>MAX_FILE_BYTES)throw new IOException("Obraz przekracza limit 64 MB.");
        if(looksLikeSvg(bytes))return readSvg(bytes);
        registerReaders();
        try(var in=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())throw new IOException("Nie można odczytać obrazu: nieznany format lub uszkodzony plik. Obsługiwane: "+FORMATS+".");
            boolean dimensionsRejected=false;
            // Several providers can recognize the same format. A failure in one
            // must not reject a valid file that another provider can decode.
            while(readers.hasNext()){
                var reader=readers.next();
                // Keep detection and each decode on separate streams. Readers may
                // seek/flush their input before failing; the next one starts clean.
                try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
                    reader.setInput(input);
                    int width=reader.getWidth(0),height=reader.getHeight(0);
                    if(width<1||height<1)continue;
                    if(width>100_000||height>100_000||(long)width*height>1_000_000_000L){dimensionsRejected=true;continue;}
                    var param=reader.getDefaultReadParam();int step=Math.max(1,(Math.max(width,height)+PREVIEW_SIDE-1)/PREVIEW_SIDE);
                    param.setSourceSubsampling(step,step,0,0);
                    BufferedImage image=reader.read(0,param);
                    if(image!=null)return image;
                }catch(IOException|RuntimeException error){
                    System.getLogger(LogoImages.class.getName()).log(System.Logger.Level.DEBUG,
                        "Czytnik logo "+reader.getClass().getSimpleName()+" nie odczytał pliku.",error);
                }finally{reader.dispose();}
            }
            if(dimensionsRejected)throw new IOException("Obraz przekracza limit wymiarów: bok 100 000 px lub miliard pikseli.");
            throw new IOException("Nie można odczytać obrazu: dostępne czytniki nie obsługują tego wariantu albo plik jest uszkodzony. Obsługiwane: "+FORMATS+".");
        }
    }
    private static boolean looksLikeSvg(byte[] bytes){
        String prefix=new String(bytes,0,Math.min(bytes.length,65536),StandardCharsets.UTF_8).replace("\u0000","").replace("\uFEFF","").stripLeading();
        return prefix.startsWith("<")&&java.util.regex.Pattern.compile("<(?:(?:[A-Za-z_][\\w.-]*):)?svg(?:\\s|>)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(prefix).find();
    }
    private static BufferedImage readSvg(byte[] bytes)throws IOException {
        if(bytes.length>8_000_000)throw new IOException("Obraz SVG przekracza limit 8 MB.");
        XMLInputFactory xml=XMLInputFactory.newDefaultFactory();
        xml.setProperty(XMLInputFactory.SUPPORT_DTD,false);
        xml.setProperty("javax.xml.stream.isSupportingExternalEntities",false);
        var document=new SVGLoader().load(()->xml.createXMLEventReader(new ByteArrayInputStream(bytes)),null,
            LoaderContext.builder().externalResourcePolicy(ResourcePolicy.DENY_ALL).build());
        if(document==null)throw new IOException("Nie można odczytać obrazu SVG: niepoprawny dokument.");
        double w=document.size().width,h=document.size().height;
        if(!Double.isFinite(w)||!Double.isFinite(h)||w<=0||h<=0)throw new IOException("Obraz SVG nie ma poprawnych wymiarów lub viewBox.");
        double scale=Math.min(1,PREVIEW_SIDE/Math.max(w,h));
        int width=Math.max(1,(int)Math.round(w*scale)),height=Math.max(1,(int)Math.round(h*scale));
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        try{document.render(null,g,new ViewBox(0,0,width,height));}finally{g.dispose();}
        return image;
    }
    public static byte[] png(BufferedImage image)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(var output=new MemoryCacheImageOutputStream(out)){if(!ImageIO.write(image,"png",output))throw new IOException("Nie można zapisać obrazu PNG.");}
        return out.toByteArray();
    }
    public static String encode(BufferedImage image,LogoCrop crop)throws IOException {
        return encode(image,crop,false);
    }
    public static String encode(BufferedImage image,LogoCrop crop,boolean banner)throws IOException {
        int outWidth=banner?pl.crimeandlaw.gang.GangBanner.WIDTH:GangLogo.SIDE;
        int outHeight=banner?pl.crimeandlaw.gang.GangBanner.HEIGHT:GangLogo.SIDE;
        BufferedImage target=new BufferedImage(outWidth,outHeight,BufferedImage.TYPE_INT_ARGB);var g=target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if(crop.fit()){
                double scale=Math.min((double)outWidth/image.getWidth(),(double)outHeight/image.getHeight());
                int w=Math.max(1,(int)Math.round(image.getWidth()*scale)),h=Math.max(1,(int)Math.round(image.getHeight()*scale));
                g.drawImage(image,(outWidth-w)/2,(outHeight-h)/2,w,h,null);
            }else{
                int x1=(int)Math.floor(crop.x()),y1=(int)Math.floor(crop.y());
                int x2=Math.min(image.getWidth(),(int)Math.ceil(crop.x()+crop.side())),y2=Math.min(image.getHeight(),(int)Math.ceil(crop.y()+crop.cropHeight()));
                g.drawImage(image,0,0,outWidth,outHeight,x1,y1,x2,y2,null);
            }
        }finally{g.dispose();}
        String encoded=Base64.getEncoder().encodeToString(png(target));
        return banner?pl.crimeandlaw.gang.GangBanner.validateImage(encoded):GangLogo.validate(encoded);
    }
}
