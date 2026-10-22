package pl.crimeandlaw.client;

/** Image coordinates stay clamped to the source independently of UI scaling. */
public final class LogoCrop {
    public final int width,height;
    private double centerX,centerY,zoom=1;
    private boolean fit;
    private final double aspect;
    public LogoCrop(int width,int height){this(width,height,1);}
    public LogoCrop(int width,int height,double aspect){if(width<1||height<1||!Double.isFinite(aspect)||aspect<=0)throw new IllegalArgumentException("Pusty obraz.");this.width=width;this.height=height;this.aspect=aspect;reset();}
    public void reset(){centerX=width/2.0;centerY=height/2.0;zoom=1;fit=false;}
    public double side(){return Math.min(width,height*aspect)/zoom;}
    public double x(){return centerX-side()/2;}
    public double cropHeight(){return side()/aspect;}
    public double y(){return centerY-cropHeight()/2;}
    public double zoom(){return zoom;}
    public double aspect(){return aspect;}
    public double minimumZoom(){return Math.min(.1,Math.min(width,height*aspect)/Math.max(width,height*aspect));}
    public double normalizedZoom(){return Math.log(zoom/minimumZoom())/Math.log(8/minimumZoom());}
    public void normalizedZoom(double value){zoom(minimumZoom()*Math.pow(8/minimumZoom(),Math.clamp(value,0,1)));}
    public boolean fit(){return fit;}
    public void fit(boolean fit){reset();this.fit=fit;if(fit)zoom=Math.min(width,height*aspect)/Math.max(width,height*aspect);clamp();}
    public void zoom(double value){if(!Double.isFinite(value))return;zoom=Math.clamp(value,minimumZoom(),8);fit=false;clamp();}
    public void pan(double dx,double dy){centerX+=dx;centerY+=dy;clamp();}
    private static double clampCenter(double center,double source,double viewport){double half=viewport/2;return Math.clamp(center,Math.min(half,source-half),Math.max(half,source-half));}
    private void clamp(){centerX=clampCenter(centerX,width,side());centerY=clampCenter(centerY,height,cropHeight());}
}
