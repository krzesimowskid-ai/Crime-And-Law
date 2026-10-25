package pl.crimeandlaw.cultivation;

/** Server timing and leaf ownership; no reward amount ever comes from a client. */
public final class PruningRules {
    public static final int LEAVES=6,FULL_MASK=(1<<LEAVES)-1,MIN_HOLD=12,IDEAL_HOLD=20,MAX_HOLD=29;
    private PruningRules(){}
    public static boolean available(int mask,int leaf){return leaf>=0&&leaf<LEAVES&&(mask&(1<<leaf))!=0;}
    public static int remove(int mask,int leaf){if(!available(mask,leaf))throw new IllegalArgumentException("Ten liść został już zebrany.");return mask&~(1<<leaf);}
    public static boolean precise(long held){return held>=MIN_HOLD&&held<=MAX_HOLD;}
    public static int count(int mask){return Integer.bitCount(mask&FULL_MASK);}
    public static int target(int mask,int choice){
        int count=count(mask);if(count==0)return -1;int wanted=Math.floorMod(choice,count);
        for(int i=0;i<LEAVES;i++)if(available(mask,i)&&wanted--==0)return i;
        throw new IllegalStateException();
    }
}
