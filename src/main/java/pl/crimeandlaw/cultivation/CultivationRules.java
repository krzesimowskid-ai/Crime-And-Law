package pl.crimeandlaw.cultivation;

/** Game balance for fictional Aurelia; active, loaded growth ticks only. */
public final class CultivationRules {
    public static final int GROWTH_TICKS=18000, SEEDLING_START=3600, WATER_CAPACITY=100, CAN_CAPACITY=400;
    public static final int WATER_INTERVAL=180, POUR_MIN=60, POUR_MAX=1800, WATER_MIN=30, WATER_MAX=1800, PACK_SEEDS=5;
    private CultivationRules(){}
    public static boolean uvEligible(int exposed,int elapsed){return elapsed>0 && (long)exposed*5>=(long)elapsed*4;}
    public static int yield(boolean uv,int roll){return uv?4+Math.floorMod(roll,5):3+Math.floorMod(roll,4);}
    public static boolean validHold(int action,long held){return action==9?held>=8&&held<=1800:action==11?held>=12&&held<=1800:action==1?held>=POUR_MIN&&held<=POUR_MAX:action==2&&held>=WATER_MIN&&held<=WATER_MAX;}
}
