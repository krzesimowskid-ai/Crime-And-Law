package pl.crimeandlaw.gang;

/** Shared optional identity banner. Opacity applies to either the image or solid color. */
public record GangBanner(String image,int color,int opacity) {
    public static final int WIDTH=2048,HEIGHT=512;
    // Retain the previous read/transport limits so existing 8K saves stay readable.
    public static final int READ_WIDTH=8192,READ_HEIGHT=2048,MAX_BYTES=68_000_000;
    public static final int MAX_ENCODED_LENGTH=((MAX_BYTES+2)/3)*4;
    public static final GangBanner DEFAULT=new GangBanner("",0x15161C,0);
    public GangBanner {
        if(image==null||image.length()>MAX_ENCODED_LENGTH)throw new IllegalArgumentException("Nieprawidłowy baner.");
        color&=0xFFFFFF;opacity=Math.clamp(opacity,0,255);
    }
    public static String validateImage(String image){return GangLogo.validateImage(image,READ_WIDTH,READ_HEIGHT,MAX_BYTES,"Baner",WIDTH,HEIGHT);}
    public static String validateUpload(String image){return GangLogo.validateImage(image,WIDTH,HEIGHT,GangLogo.MAX_BYTES,"Baner");}
}
