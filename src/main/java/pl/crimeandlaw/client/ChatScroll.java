package pl.crimeandlaw.client;

/** Bounded scroll offset measured backwards from the newest row. */
public final class ChatScroll {
    private int size,page=1,offset;
    public void bounds(int size,int page){this.size=Math.max(0,size);this.page=Math.max(1,page);set(offset);}
    public void set(int offset){this.offset=Math.clamp(offset,0,Math.max(0,size-page));}
    public void scroll(int amount){set((int)Math.clamp((long)offset+amount,0L,(long)Math.max(0,size-page)));}
    public void reset(){offset=0;}
    public int offset(){return offset;}
    public int end(){return Math.max(0,size-offset);}
    public int start(){return Math.max(0,end()-page);}
    public int maximum(){return Math.max(0,size-page);}
}
