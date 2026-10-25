package pl.crimeandlaw.cultivation;

import java.util.UUID;
import java.util.function.IntUnaryOperator;

/** No world/inventory dependency: one seed, one soil filling and one persisted roll per cycle. */
public final class PlanterState {
    public record Data(boolean soil,boolean planted,int growth,int initial,int uv,int water,int total,int leaves,UUID identity){}
    private boolean soil,planted; private int growth,initial,uv,water,total,leaves; private UUID identity=UUID.randomUUID();
    public Data data(){return new Data(soil,planted,growth,initial,uv,water,total,leaves,identity);}
    public void restore(Data d){
        soil=d.soil;planted=soil&&d.planted;growth=planted?Math.clamp(d.growth,0,CultivationRules.GROWTH_TICKS):0;
        initial=planted?Math.clamp(d.initial,0,Math.min(growth,CultivationRules.SEEDLING_START)):0;
        uv=planted?Math.clamp(d.uv,0,growth-initial):0;water=soil?Math.clamp(d.water,0,CultivationRules.WATER_CAPACITY):0;
        total=planted&&growth==CultivationRules.GROWTH_TICKS?Math.clamp(d.total,0,8):0;
        leaves=total>0?d.leaves&((1<<total)-1):0; identity=d.identity==null?UUID.randomUUID():d.identity;
        if(total>0&&leaves==0)finish();
        else if(planted&&growth==CultivationRules.GROWTH_TICKS&&total==0)growth--;
    }
    public boolean soil(){return soil;} public boolean planted(){return planted;} public int growth(){return growth;}
    public int water(){return water;} public int total(){return total;} public int leaves(){return leaves;}
    public UUID identity(){return identity;} public boolean mature(){return total>0;}
    public int stage(){return !soil?0:!planted?1:mature()?4:water==0?2:3;}
    public boolean fillSoil(){if(soil)return false;soil=true;return true;}
    public boolean plant(boolean seedling){if(!soil||planted)return false;planted=true;growth=initial=seedling?CultivationRules.SEEDLING_START:0;identity=UUID.randomUUID();return true;}
    public int addWater(int available){if(!soil||available<=0)return 0;int n=Math.min(available,CultivationRules.WATER_CAPACITY-water);water+=n;return n;}
    public boolean step(boolean lit,boolean lamp,IntUnaryOperator random){
        if(!planted||mature()||water<=0||!lit)return false;
        growth++;if(lamp)uv++;
        if((growth-initial)%CultivationRules.WATER_INTERVAL==0)water--;
        if(growth>=CultivationRules.GROWTH_TICKS){boolean bonus=CultivationRules.uvEligible(uv,growth-initial);total=CultivationRules.yield(bonus,random.applyAsInt(bonus?5:4));leaves=(1<<total)-1;}
        return true;
    }
    public boolean cut(int id){if(!mature()||!PruningRules.available(leaves,id))return false;leaves=PruningRules.remove(leaves,id);return true;}
    public boolean finish(){if(!mature()||leaves!=0)return false;soil=planted=false;growth=initial=uv=water=total=leaves=0;identity=UUID.randomUUID();return true;}
}
