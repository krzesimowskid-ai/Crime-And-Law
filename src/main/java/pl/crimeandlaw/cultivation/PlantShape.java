package pl.crimeandlaw.cultivation;

import java.util.*;

/** A permanent, cosmetic growth plan derived from the existing persisted plant identity. */
public final class PlantShape {
    public record Leaf(int node,double height,double angle,double length,double width,double rise,double arch){
        public double lift(double t){double bend=t<.42?t/.42:t<.72?1-(t-.42)*(.45/.30):.55-(t-.72)*(.55/.28);return rise*t+arch*bend;}
        public double minY(){return height+Math.min(0,rise)-.004-.0065;}
        public double maxY(){return height+Math.max(0,rise)+arch+.0065;}
    }
    // The first three harvestable leaves span the plant; later growth fills the gaps.
    private static final int[] NODES={0,4,7,2,6,1,5,3};
    private PlantShape(){}
    public static long seed(UUID identity){return identity.getMostSignificantBits()^Long.rotateLeft(identity.getLeastSignificantBits(),23);}
    public static List<Leaf> create(long seed){
        long mixed=(seed^(seed>>>30))*0xBF58476D1CE4E5B9L;mixed=(mixed^(mixed>>>27))*0x94D049BB133111EBL;mixed^=mixed>>>31;
        var random=new Random(mixed);double phase=random.nextDouble()*Math.PI*2,bottom=.22+random.nextDouble()*.02,span=.56+random.nextDouble()*.04;
        double[] heights=new double[8],gaps=new double[7];double sum=0;for(int i=0;i<7;i++){gaps[i]=.9+random.nextDouble()*.2;sum+=gaps[i];}
        heights[0]=bottom;for(int i=1;i<8;i++)heights[i]=heights[i-1]+span*gaps[i-1]/sum;
        var leaves=new ArrayList<Leaf>(8);
        for(int id=0;id<8;id++){
            int node=NODES[id];double taper=1-node*.12/7;
            leaves.add(new Leaf(node,heights[node],phase+node*2.399963229728653+(random.nextDouble()-.5)*1.1,(.34+random.nextDouble()*.12)*taper,(.23+random.nextDouble()*.09)*taper,-.006+random.nextDouble()*.014,.003+random.nextDouble()*.006));
        }return List.copyOf(leaves);
    }
}
