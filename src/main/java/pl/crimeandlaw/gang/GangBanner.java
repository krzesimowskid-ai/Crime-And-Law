package pl.crimeandlaw.gang;

/** Shared optional identity banner. Opacity applies to either the image or solid color. */
public record GangBanner(String image,int color,int opacity) {
    public static final int WIDTH=4096,HEIGHT=1024;
    public static final GangBanner DEFAULT=new GangBanner("",0x15161C,0);
    public GangBanner {
        if(image==null||image.length()>GangLogo.MAX_ENCODED_LENGTH)throw new IllegalArgumentException("Nieprawidłowy baner.");
        color&=0xFFFFFF;opacity=Math.clamp(opacity,0,255);
    }
    public static String validateImage(String image){return GangLogo.validateImage(image,WIDTH,HEIGHT,"Baner");}
}
