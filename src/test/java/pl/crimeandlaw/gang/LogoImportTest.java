package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.crimeandlaw.client.LogoImages;
import pl.crimeandlaw.client.LogoCrop;
import static org.junit.jupiter.api.Assertions.*;

class LogoImportTest {
    @TempDir Path folder;
    @Test void bundledCodecsReadRealFormatsWithMisleadingNames()throws Exception {
        try(var source=getClass().getResourceAsStream("/logo-formats.json")){
            var cases=JsonParser.parseReader(new InputStreamReader(source)).getAsJsonObject();
            for(var entry:cases.entrySet()){
                Path file=folder.resolve(entry.getKey()+"-żółć.png");
                Files.write(file,Base64.getDecoder().decode(entry.getValue().getAsString()));
                BufferedImage image=LogoImages.read(file);
                assertEquals(16,image.getWidth(),entry.getKey());assertEquals(16,image.getHeight(),entry.getKey());
                int rgb=image.getRGB(8,8);assertEquals(255,rgb>>>24,entry.getKey());
                assertEquals(40,(rgb>>16)&255,4,entry.getKey());assertEquals(120,(rgb>>8)&255,4,entry.getKey());assertEquals(200,rgb&255,4,entry.getKey());
            }
        }
    }
    @Test void svgLoadsByContentAndPreservesAspectAndTransparency()throws Exception {
        Path file=folder.resolve("vector.png");Files.writeString(file,"""
            <?xml version="1.0"?>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 6000 3000">
              <rect width="3000" height="3000" fill="#2878c8"/>
            </svg>
            """);
        BufferedImage image=LogoImages.read(file);assertTrue(image.getWidth()>4096);assertEquals(image.getWidth()/2,image.getHeight());assertTrue((long)image.getWidth()*image.getHeight()<=LogoImages.MAX_SOURCE_PIXELS);
        assertEquals(0xFF2878C8,image.getRGB(100,100));assertEquals(0,image.getRGB(image.getWidth()-100,100)>>>24);
    }
    @Test void pngWithSvgTextMetadataRemainsARaster()throws Exception {
        var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);image.setRGB(8,8,0xFF2878C8);
        var writer=ImageIO.getImageWritersByFormatName("png").next();var param=writer.getDefaultWriteParam();
        var metadata=writer.getDefaultImageMetadata(javax.imageio.ImageTypeSpecifier.createFromRenderedImage(image),param);
        var root=new javax.imageio.metadata.IIOMetadataNode("javax_imageio_png_1.0");
        var text=new javax.imageio.metadata.IIOMetadataNode("tEXt");var entry=new javax.imageio.metadata.IIOMetadataNode("tEXtEntry");
        entry.setAttribute("keyword","Description");entry.setAttribute("value","Source <svg width='16' height='16'>");text.appendChild(entry);root.appendChild(text);metadata.mergeTree("javax_imageio_png_1.0",root);
        Path file=folder.resolve("metadata.png");
        try(var output=new javax.imageio.stream.MemoryCacheImageOutputStream(Files.newOutputStream(file))){writer.setOutput(output);writer.write(null,new javax.imageio.IIOImage(image,null,metadata),param);}finally{writer.dispose();}
        assertEquals(0xFF2878C8,LogoImages.read(file).getRGB(8,8));
    }
    @Test void svgDoesNotLoadExternalResources()throws Exception {
        Path other=folder.resolve("other.svg");Files.writeString(other,"<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'><rect width='16' height='16' fill='red'/></svg>");
        Path file=folder.resolve("main.svg");Files.writeString(file,"<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'><image href='"+other.toUri()+"' width='16' height='16'/></svg>");
        assertEquals(0,LogoImages.read(file).getRGB(8,8)>>>24);
    }
    @Test void codecsWorkOnAsyncThreadWithUnrelatedContextLoader()throws Exception {
        var previous=Thread.currentThread().getContextClassLoader();
        try{
            Thread.currentThread().setContextClassLoader(new ClassLoader(null){});
            try(var input=getClass().getResourceAsStream("/logo-formats.json")){
                var json=JsonParser.parseReader(new InputStreamReader(input)).getAsJsonObject();Path file=folder.resolve("test.data");
                Files.write(file,Base64.getDecoder().decode(json.get("WEBP").getAsString()));assertEquals(16,LogoImages.read(file).getWidth());
            }
        }finally{Thread.currentThread().setContextClassLoader(previous);}
    }
    @Test void veryWideRasterIsDownsampledBeforePreview()throws Exception {
        Path file=folder.resolve("panorama.png");ImageIO.write(new BufferedImage(20000,4,BufferedImage.TYPE_INT_RGB),"png",file.toFile());
        var image=LogoImages.read(file);assertTrue(image.getWidth()<=LogoImages.SOURCE_SIDE);assertTrue(image.getHeight()>=1);
    }
    @Test void pngColorModelsAndFileNamesDoNotRestrictImport()throws Exception {
        int[] types={BufferedImage.TYPE_INT_RGB,BufferedImage.TYPE_INT_ARGB,BufferedImage.TYPE_BYTE_GRAY,
            BufferedImage.TYPE_USHORT_GRAY,BufferedImage.TYPE_BYTE_BINARY,BufferedImage.TYPE_BYTE_INDEXED};
        String[] names={"flaga ZSRR.png","flaga Izraela.png","logo gangu.png","żółć 漢字.bin","bez rozszerzenia","obraz.JPG"};
        for(int i=0;i<types.length;i++){
            var source=new BufferedImage(256,128,types[i]);
            var graphics=source.createGraphics();
            try{graphics.setColor(new java.awt.Color(40,120,200));graphics.fillRect(0,0,256,128);}finally{graphics.dispose();}
            Path file=folder.resolve(names[i]);assertTrue(ImageIO.write(source,"png",file.toFile()));
            var loaded=LogoImages.read(file);assertEquals(256,loaded.getWidth());assertEquals(128,loaded.getHeight());
            assertEquals(source.getRGB(100,64),loaded.getRGB(100,64),names[i]);
            assertFalse(LogoImages.encode(loaded,new LogoCrop(256,128)).isBlank(),names[i]);
        }
    }
    @Test void failingReaderDoesNotPreventAnotherReaderFromLoadingTheImage()throws Exception {
        var source=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);source.setRGB(8,8,0xFF2878C8);
        Path file=folder.resolve("flag.png");ImageIO.write(source,"png",file.toFile());
        // Initialize bundled providers before placing a deliberately incompatible
        // PNG reader first, as may happen with overlapping ImageIO plugins.
        LogoImages.read(file);
        IIORegistry registry=IIORegistry.getDefaultInstance();var broken=new FailingPngSpi();
        var providers=new ArrayList<ImageReaderSpi>();registry.getServiceProviders(ImageReaderSpi.class,true).forEachRemaining(providers::add);
        registry.registerServiceProvider(broken,ImageReaderSpi.class);
        for(var provider:providers)registry.setOrdering(ImageReaderSpi.class,broken,provider);
        try{
            for(boolean runtime:new boolean[]{false,true}){
                broken.runtime=runtime;
                assertEquals(0xFF2878C8,LogoImages.read(file).getRGB(8,8));
            }
            assertEquals(2,broken.attempts);assertEquals(2,broken.disposals);
        }finally{registry.deregisterServiceProvider(broken,ImageReaderSpi.class);}
    }
    @Test void rejectsCorruptEmptyAndOverBudgetFilesClearly()throws Exception {
        Path file=folder.resolve("broken.png");Files.write(file,new byte[0]);
        assertTrue(assertThrows(IOException.class,()->LogoImages.read(file)).getMessage().contains("Nie można odczytać"));
        try(var large=new RandomAccessFile(file.toFile(),"rw")){large.setLength((long)LogoImages.MAX_FILE_BYTES+1);}
        assertTrue(assertThrows(IOException.class,()->LogoImages.read(file)).getMessage().contains("64 MB"));
    }
    private static final class FailingPngSpi extends ImageReaderSpi {
        boolean runtime;int attempts,disposals;
        FailingPngSpi(){super("Test","1",new String[]{"png"},new String[]{"png"},new String[]{"image/png"},
            FailingPngReader.class.getName(),new Class<?>[]{ImageInputStream.class},null,false,null,null,null,null,false,null,null,null,null);}
        @Override public boolean canDecodeInput(Object source)throws IOException {
            if(!(source instanceof ImageInputStream input))return false;
            long position=input.getStreamPosition();
            try{return input.readInt()==0x89504E47;}finally{input.seek(position);}
        }
        @Override public ImageReader createReaderInstance(Object extension){return new FailingPngReader(this);}
        @Override public String getDescription(Locale locale){return "Deliberately incompatible PNG reader";}
    }
    private static final class FailingPngReader extends ImageReader {
        private final FailingPngSpi provider;
        FailingPngReader(FailingPngSpi provider){super(provider);this.provider=provider;}
        @Override public int getNumImages(boolean allowSearch){return 1;}
        @Override public int getWidth(int index){return 16;}
        @Override public int getHeight(int index){return 16;}
        @Override public Iterator<ImageTypeSpecifier> getImageTypes(int index){return List.of(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)).iterator();}
        @Override public IIOMetadata getStreamMetadata(){return null;}
        @Override public IIOMetadata getImageMetadata(int index){return null;}
        @Override public BufferedImage read(int index,ImageReadParam param)throws IOException {
            provider.attempts++;
            var stream=(ImageInputStream)getInput();stream.seek(stream.length()<0?8:stream.length());stream.flushBefore(stream.getStreamPosition());
            if(provider.runtime)throw new IllegalArgumentException("Unsupported PNG variant");
            throw new IOException("Unsupported PNG variant");
        }
        @Override public void dispose(){provider.disposals++;super.dispose();}
    }
}
