package pl.crimeandlaw.client;

/** Rendering-independent radar palette and compass geometry. Colors use NativeImage's ABGR. */
public final class MinimapStyle {
    public static final int OPACITY=176,FOG=0x58474440;
    private MinimapStyle(){}
    public record Point(float x,float y){}
    public static int terrain(int abgr){
        // Transparency is independent of the terrain palette. Keep every original RGB channel.
        return OPACITY<<24|abgr&0x00FFFFFF;
    }
    public static Point north(float x,float y,int width,int height,float playerX,float playerY,float angle){
        double radians=Math.toRadians(angle),dx=Math.sin(radians),dy=-Math.cos(radians);
        double tx=Math.abs(dx)<1e-8?Double.POSITIVE_INFINITY:((dx>0?x+width-8:x+8)-playerX)/dx;
        double ty=Math.abs(dy)<1e-8?Double.POSITIVE_INFINITY:((dy>0?y+height-8:y+8)-playerY)/dy;
        double t=Math.min(tx,ty);return new Point((float)(playerX+dx*t),(float)(playerY+dy*t));
    }
}
