package pl.crimeandlaw.territory;

/** One active map download; rapid camera updates replace the pending destination. */
public final class TerrainRequestGate {
    public static final long INTERVAL_NANOS=150_000_000L,TIMEOUT_NANOS=2_000_000_000L;
    private long sent;
    private int sequence,awaiting;
    private boolean started;
    public boolean ready(long now){return (!started||now-sent>=INTERVAL_NANOS)&&(awaiting==0||now-sent>=TIMEOUT_NANOS);}
    public int start(long now){
        if(!ready(now))return 0;
        sequence=sequence==Integer.MAX_VALUE?1:sequence+1;awaiting=sequence;sent=now;started=true;return awaiting;
    }
    public boolean acknowledge(int id){if(id<=0||id!=awaiting)return false;awaiting=0;return true;}
    public int awaiting(){return awaiting;}
    public void clear(){sent=0;awaiting=0;started=false;}
}
