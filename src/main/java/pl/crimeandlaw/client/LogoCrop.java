package pl.crimeandlaw.client;

/** Image coordinates stay clamped to the source independently of UI scaling. */
public final class LogoCrop {
    public final int width,height;
    private double centerX,centerY,zoom=1;
    private boolean fit;
    public LogoCrop(int width,int height){if(width<1||height<1)throw new IllegalArgumentException("Pusty obraz.");this.width=width;this.height=height;reset();}
    public void reset(){centerX=width/2.0;centerY=height/2.0;zoom=1;fit=false;}
    public double side(){return Math.min(width,height)/zoom;}
    public double x(){return centerX-side()/2;}
    public double y(){return centerY-side()/2;}
    public double zoom(){return zoom;}
    public boolean fit(){return fit;}
    public void fit(boolean fit){this.fit=fit;}
    public void zoom(double value){zoom=Math.clamp(value,1,8);clamp();}
    public void pan(double dx,double dy){centerX+=dx;centerY+=dy;clamp();}
    private void clamp(){double half=side()/2;centerX=Math.clamp(centerX,half,width-half);centerY=Math.clamp(centerY,half,height-half);}
}
