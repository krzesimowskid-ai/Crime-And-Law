package pl.crimeandlaw.gang;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class GangLogoTest {
    private String png(int w,int h)throws Exception{
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
    @Test void acceptsLegacyAndDetailedPngAndEmptyDefault()throws Exception{
        assertFalse(GangLogo.validate(png(64,64)).isBlank());
        assertFalse(GangLogo.validate(png(256,256)).isBlank());
        assertFalse(GangLogo.validate(png(512,512)).isBlank());assertEquals("",GangLogo.validate(""));
    }
    @Test void rejectsMalformedAndOversizedImages()throws Exception{
        assertThrows(IllegalArgumentException.class,()->GangLogo.validate("not an image"));
        String wide=png(513,512),tall=png(512,513);
        assertThrows(IllegalArgumentException.class,()->GangLogo.validate(wide));
        assertThrows(IllegalArgumentException.class,()->GangLogo.validate(tall));
        assertThrows(IllegalArgumentException.class,()->GangLogo.validate("a".repeat(GangLogo.MAX_ENCODED_LENGTH+1)));
    }
}
