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
    public boolean fit(){return fit;}
    public void fit(boolean fit){this.fit=fit;}
    public void zoom(double value){zoom=Math.clamp(value,1,8);clamp();}
    public void pan(double dx,double dy){centerX+=dx;centerY+=dy;clamp();}
    private void clamp(){double half=side()/2,halfY=cropHeight()/2;centerX=Math.clamp(centerX,half,width-half);centerY=Math.clamp(centerY,halfY,height-halfY);}
}
