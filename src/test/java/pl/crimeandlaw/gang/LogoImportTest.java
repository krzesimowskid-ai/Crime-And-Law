package pl.crimeandlaw.gang;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.crimeandlaw.client.LogoImages;
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
        BufferedImage image=LogoImages.read(file);assertEquals(1024,image.getWidth());assertEquals(512,image.getHeight());
        assertEquals(0xFF2878C8,image.getRGB(100,100));assertEquals(0,image.getRGB(900,100)>>>24);
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
        var image=LogoImages.read(file);assertTrue(image.getWidth()<=1024);assertTrue(image.getHeight()>=1);
    }
    @Test void rejectsCorruptEmptyAndOverBudgetFilesClearly()throws Exception {
        Path file=folder.resolve("broken.png");Files.write(file,new byte[0]);
        assertTrue(assertThrows(IOException.class,()->LogoImages.read(file)).getMessage().contains("Nie można odczytać"));
        try(var large=new RandomAccessFile(file.toFile(),"rw")){large.setLength((long)LogoImages.MAX_FILE_BYTES+1);}
        assertTrue(assertThrows(IOException.class,()->LogoImages.read(file)).getMessage().contains("64 MB"));
    }
}
