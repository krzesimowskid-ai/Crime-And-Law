package pl.crimeandlaw.gang;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.crimeandlaw.client.LogoCrop;
import pl.crimeandlaw.client.LogoImages;
import static org.junit.jupiter.api.Assertions.*;

class HighResolutionImagesTest {
    @TempDir Path folder;
    @Test void wideOriginalKeepsOnePixelDetailsThroughImportAndSavedBanner()throws Exception{
        var source=new BufferedImage(4096,1024,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<1024;y++)for(int x=0;x<4096;x++)source.setRGB(x,y,((x+y)&1)==0?0xFF142538:0xFFF1DACB);
        Path file=folder.resolve("detailed.png");ImageIO.write(source,"png",file.toFile());
        var decoded=LogoImages.read(file);assertEquals(4096,decoded.getWidth());assertEquals(1024,decoded.getHeight());
        var preview=LogoImages.preview(decoded);assertEquals(2048,preview.getWidth());assertEquals(512,preview.getHeight());
        var saved=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(LogoImages.encode(decoded,new LogoCrop(4096,1024,4),true))));
        assertArrayEquals(source.getRGB(0,0,4096,1024,null,0,4096),saved.getRGB(0,0,4096,1024,null,0,4096));
    }
    @Test void longBannerSourceStaysDetailedWhileSquareSourcesRespectPixelBudget()throws Exception{
        Path wide=folder.resolve("wide.svg"),square=folder.resolve("square.svg");
        Files.writeString(wide,"<svg xmlns='http://www.w3.org/2000/svg' width='8192' height='1024'><rect width='8192' height='1024' fill='red'/></svg>");
        var image=LogoImages.read(wide);assertEquals(8192,image.getWidth());assertEquals(1024,image.getHeight());
        Files.writeString(square,"<svg xmlns='http://www.w3.org/2000/svg' width='8192' height='8192'><rect width='8192' height='8192' fill='blue'/></svg>");
        image=LogoImages.read(square);assertEquals(4096,image.getWidth());assertEquals(4096,image.getHeight());
        assertTrue((long)image.getWidth()*image.getHeight()<=LogoImages.MAX_SOURCE_PIXELS);
    }
}
