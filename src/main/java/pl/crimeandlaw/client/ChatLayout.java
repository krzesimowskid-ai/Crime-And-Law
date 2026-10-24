package pl.crimeandlaw.client;

/** Chat uses a logical canvas so GUI 2/4 have the same physical text size and mouse targets. */
public record ChatLayout(float scale,int x,int y,int width,int lines,int lineHeight,int header) {
    public static ChatLayout of(int width,int height,boolean focused,double chatWidth,double chatScale,double lineSpacing){
        return of(width,height,focused,chatWidth,chatScale,lineSpacing,1);
    }
    public static ChatLayout of(int width,int height,boolean focused,double chatWidth,double chatScale,double lineSpacing,double chatHeight){
        float scale=GangLayout.of(width,height).scale()*.82f*(float)Math.clamp(chatScale,.1,1);
        int available=(int)(height/scale),line=14+(int)Math.round(Math.clamp(lineSpacing,0,1)*6);
        int wanted=(int)Math.round((20+Math.clamp(chatHeight,0,1)*160)/180*11);
        int lines=Math.max(2,Math.min(wanted,(int)(available*.34)/line));
        return new ChatLayout(scale,10,10,200+(int)Math.round(Math.clamp(chatWidth,0,1)*140),lines,line,18);
    }
    public int historyY(){return y+header;}
    public int historyHeight(){return lines*lineHeight;}
    public int inputY(){return historyY()+historyHeight()+8;}
    public int inputHeight(){return 24;}
    public double local(double coordinate){return coordinate/scale;}
    public boolean inHistory(double sx,double sy){double px=local(sx),py=local(sy);return px>=x&&px<x+width&&py>=historyY()&&py<historyY()+historyHeight();}
}
