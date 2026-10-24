package pl.crimeandlaw.client;

/** A focus transition for the whole panel; idle chat never fades out with message age. */
public final class ChatFade {
    public static final float IDLE=.58f;
    private static final long DURATION=200_000_000L;
    private float from=IDLE,target=IDLE;
    private long changedAt;
    public float opacity(boolean focused,long now){
        float value=value(now),next=focused?1:IDLE;
        if(next!=target){from=value;target=next;changedAt=now;}
        return value;
    }
    private float value(long now){
        double t=Math.clamp((now-changedAt)/(double)DURATION,0,1);
        return from+(target-from)*(float)t;
    }
    public void reset(){from=target=IDLE;changedAt=0;}
    public static int tint(int color,double opacity){return (int)Math.round((color>>>24)*Math.clamp(opacity,0,1))<<24|color&0xFFFFFF;}
}
