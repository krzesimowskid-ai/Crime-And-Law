package pl.crimeandlaw.gang;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.crimeandlaw.client.*;
import static org.junit.jupiter.api.Assertions.*;

class ImageEditingTest {
    @TempDir Path folder;
    @Test void readsPngWithSpacesAndPolishCharacters()throws Exception {
        Path path=folder.resolve("moje logo żółte.PNG");BufferedImage image=new BufferedImage(120,80,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(10,20,0xFF1267AB);ImageIO.write(image,"png",path.toFile());
        BufferedImage loaded=LogoImages.read(path);assertEquals(120,loaded.getWidth());assertEquals(0xFF1267AB,loaded.getRGB(10,20));
    }
    @Test void readsJpegAndDownsamplesLargeSources()throws Exception {
        Path jpg=folder.resolve("photo.jpg");ImageIO.write(new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB),"jpeg",jpg.toFile());
        assertEquals(320,LogoImages.read(jpg).getWidth());
        Path png=folder.resolve("large.png");ImageIO.write(new BufferedImage(4097,32,BufferedImage.TYPE_INT_ARGB),"png",png.toFile());
        assertTrue(LogoImages.read(png).getWidth()<=LogoImages.PREVIEW_SIDE);
    }
    @Test void accepts256SquareAndWideOrTallImagesAcrossSupportedFormats()throws Exception {
        for(String format:List.of("png","jpeg","gif","bmp"))for(int[] size:new int[][]{{256,256},{640,160},{80,320}}){
            BufferedImage source=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);
            source.setRGB(size[0]/2,size[1]/2,0xFFFF0000);
            Path path=folder.resolve(format+"-"+size[0]+"x"+size[1]+"."+format);
            assertTrue(ImageIO.write(source,format,path.toFile()));BufferedImage loaded=LogoImages.read(path);
            assertEquals(size[0],loaded.getWidth());assertEquals(size[1],loaded.getHeight());
            LogoCrop crop=new LogoCrop(loaded.getWidth(),loaded.getHeight());
            assertFalse(GangLogo.validate(LogoImages.encode(loaded,crop)).isBlank());
            crop.fit(true);BufferedImage fitted=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(LogoImages.encode(loaded,crop))));
            assertEquals(GangLogo.SIDE,fitted.getWidth());assertEquals(GangLogo.SIDE,fitted.getHeight());
            if(size[0]!=size[1])assertEquals(0,fitted.getRGB(0,0)>>>24,"Fit preserves proportions with transparent margins");
        }
    }
    @Test void reportsMissingAndFakeImageFiles()throws Exception {
        assertTrue(assertThrows(IOException.class,()->LogoImages.read(folder.resolve("missing.png"))).getMessage().contains("Nie znaleziono"));
        Path fake=folder.resolve("fake.png");Files.writeString(fake,"not an image");
        assertTrue(assertThrows(IOException.class,()->LogoImages.read(fake)).getMessage().contains("Nie można odczytać"));
    }
    @Test void cropNeverEscapesImageAfterExtremePanAndZoom(){
        LogoCrop crop=new LogoCrop(800,200);crop.zoom(100);crop.pan(1e6,-1e6);
        assertEquals(8,crop.zoom());assertTrue(crop.x()>=0);assertTrue(crop.y()>=0);assertTrue(crop.x()+crop.side()<=800);assertTrue(crop.y()+crop.side()<=200);
        crop.reset();assertEquals(300,crop.x());assertEquals(0,crop.y());
    }
    @Test void encodesChosenCropAndPreservesFitTransparency()throws Exception {
        BufferedImage source=new BufferedImage(200,100,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<100;y++)for(int x=0;x<200;x++)source.setRGB(x,y,x<100?0xFFFF0000:0xFF0000FF);
        LogoCrop crop=new LogoCrop(200,100);crop.pan(-1000,0);
        BufferedImage selected=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(LogoImages.encode(source,crop))));
        assertEquals(0xFFFF0000,selected.getRGB(256,256));assertEquals(GangLogo.SIDE,selected.getWidth());
        crop.fit(true);BufferedImage fit=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(LogoImages.encode(source,crop))));
        assertEquals(0,fit.getRGB(256,0)>>>24);assertEquals(255,fit.getRGB(256,256)>>>24);
    }
    @Test void rgbHsvRoundTripAndGrayscale(){
        Random random=new Random(7);
        for(int i=0;i<2000;i++){int color=random.nextInt(0x1000000);double[] hsv=ColorValues.hsv(color);assertEquals(color,ColorValues.rgb(hsv[0],hsv[1],hsv[2]));}
        assertEquals(0xFFFFFF,ColorValues.rgb(0.7,0,1));assertEquals(0,ColorValues.rgb(0.3,1,0));assertEquals(0xFF0000,ColorValues.rgb(1,1,1));
    }
    @Test void acceptsWorstCaseRgbaThumbnailWithinNetworkBudget()throws Exception {
        BufferedImage noise=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);Random random=new Random(18);
        for(int y=0;y<512;y++)for(int x=0;x<512;x++)noise.setRGB(x,y,random.nextInt());
        String encoded=LogoImages.encode(noise,new LogoCrop(512,512));
        assertFalse(encoded.isBlank());assertTrue(Base64.getDecoder().decode(encoded).length<=GangLogo.MAX_BYTES);
        assertTrue(encoded.length()+512<pl.crimeandlaw.network.GangNetwork.ACTION_MAX_JSON,"Fits the action packet with metadata");
    }
    @Test void preservesFineDetailsThatWereLostAt64Pixels()throws Exception {
        BufferedImage source=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<1024;y++)for(int x=0;x<1024;x++)source.setRGB(x,y,((x+y)&1)==0?0xFF000000:0xFFFFFFFF);
        BufferedImage saved=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(LogoImages.encode(source,new LogoCrop(1024,1024)))));
        assertEquals(1024,saved.getWidth());assertEquals(1024,saved.getHeight());
        for(int y=0;y<1024;y++)for(int x=0;x<1024;x++)assertEquals(source.getRGB(x,y),saved.getRGB(x,y));
    }
}
