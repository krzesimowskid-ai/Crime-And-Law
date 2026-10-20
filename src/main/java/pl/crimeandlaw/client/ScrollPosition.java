package pl.crimeandlaw.client;

/** Pixel-based viewport position; independent of GUI scaling and networking. */
public final class ScrollPosition {
    private int viewport=1,content;
    private double offset;
    public void range(int viewport,int content){this.viewport=Math.max(1,viewport);this.content=Math.max(0,content);set(offset);}
    public int viewport(){return viewport;}
    public double offset(){return offset;}
    public int maximum(){return Math.max(0,content-viewport);}
    public void reset(){offset=0;}
    public void set(double value){offset=Math.clamp(value,0,maximum());}
    public void scroll(double pixels){set(offset+pixels);}
    public int thumbHeight(){return maximum()==0?viewport:Math.min(viewport,Math.max(18,(int)Math.round((double)viewport*viewport/content)));}
    public int thumbTop(){return maximum()==0?0:(int)Math.round(offset/maximum()*(viewport-thumbHeight()));}
    public void moveThumb(double top){int travel=viewport-thumbHeight();set(travel<=0?0:Math.clamp(top/travel,0,1)*maximum());}
}
