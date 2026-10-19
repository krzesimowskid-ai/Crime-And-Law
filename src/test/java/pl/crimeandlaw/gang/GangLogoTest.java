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
    @Test void acceptsSmallPngAndEmptyDefault()throws Exception{
        assertFalse(GangLogo.validate(png(64,64)).isBlank());assertEquals("",GangLogo.validate(""));
    }
    @Test void rejectsMalformedAndOversizedImages()throws Exception{
        assertThrows(IllegalArgumentException.class,()->GangLogo.validate("not an image"));
        String large=png(65,64);assertThrows(IllegalArgumentException.class,()->GangLogo.validate(large));
        assertThrows(IllegalArgumentException.class,()->GangLogo.validate("a".repeat(23000)));
    }
}
