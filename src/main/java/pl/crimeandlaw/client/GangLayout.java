package pl.crimeandlaw.client;

/** Logical canvas keeps this custom menu independent of Minecraft's GUI scale. */
public record GangLayout(float scale,int canvasWidth,int canvasHeight,int panelWidth,int panelHeight) {
    public static GangLayout of(int width,int height){
        float scale=Math.max(0.01f,Math.min(Math.max(1,width)/800f,Math.max(1,height)/450f));
        int cw=Math.round(Math.max(1,width)/scale),ch=Math.round(Math.max(1,height)/scale);
        return new GangLayout(scale,cw,ch,Math.min(640,cw-40),Math.min(360,ch-40));
    }
    public double local(double coordinate){return coordinate/scale;}
}
