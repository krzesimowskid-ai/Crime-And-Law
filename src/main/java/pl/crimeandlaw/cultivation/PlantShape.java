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
    private record Vector(double x,double y,double z){
        Vector cross(Vector b){return new Vector(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x);}
        double dot(Vector b){return x*b.x+y*b.y+z*b.z;}
    }
    private record Envelope(Vector center,Vector[] edges){}
    private PlantShape(){}
    public static long seed(UUID identity){return identity.getMostSignificantBits()^Long.rotateLeft(identity.getLeastSignificantBits(),23);}
    public static List<Leaf> create(long seed){
        long mixed=(seed^(seed>>>30))*0xBF58476D1CE4E5B9L;mixed=(mixed^(mixed>>>27))*0x94D049BB133111EBL;mixed^=mixed>>>31;
        var random=new Random(mixed);double phase=random.nextDouble()*Math.PI*2,bottom=.22+random.nextDouble()*.02,span=.56+random.nextDouble()*.04;
        double[] heights=new double[8],gaps=new double[7];double sum=0;for(int i=0;i<7;i++){gaps[i]=.9+random.nextDouble()*.2;sum+=gaps[i];}
        heights[0]=bottom;for(int i=1;i<8;i++)heights[i]=heights[i-1]+span*gaps[i-1]/sum;
        var modes=new ArrayList<>(List.of(-1,0,1));Collections.shuffle(modes,random);
        // First try the full inclination range. A bounded reduction guarantees a
        // separated layout even for an unusually crowded deterministic seed.
        for(int pass=0;pass<9;pass++){
            var leaves=new ArrayList<Leaf>(8);double strength=Math.pow(.7,pass);
            for(int id=0;id<8;id++){
                int node=NODES[id],mode=modes.get(id%3);double taper=1-node*.12/7;
                double length=(.34+random.nextDouble()*.12)*taper,width=(.23+random.nextDouble()*.09)*taper;
                for(int attempt=0;attempt<192;attempt++){
                    double angle=phase+node*2.399963229728653+(attempt<8?(random.nextDouble()-.5)*1.1:random.nextDouble()*Math.PI*2);
                    double slope=mode==0?(random.nextDouble()-.5)*.10:mode*(.18+random.nextDouble()*.24);
                    double rise=Math.max(.126-heights[node],length*slope*strength);
                    var candidate=new Leaf(node,heights[node],angle,length,width,rise,.004+random.nextDouble()*.008);
                    if(leaves.stream().noneMatch(other->overlap(candidate,other))){leaves.add(candidate);break;}
                }
                if(leaves.size()!=id+1)break;
            }
            if(leaves.size()==8)return List.copyOf(leaves);
        }
        throw new IllegalStateException("Cannot separate plant leaves");
    }
    private static Envelope envelope(Leaf leaf){
        double c=Math.cos(leaf.angle),s=Math.sin(leaf.angle),bend=(leaf.arch-.004)/2;
        return new Envelope(new Vector(c*leaf.length/2,leaf.height+leaf.rise/2+bend,s*leaf.length/2),new Vector[]{
            new Vector(c*leaf.length/2,leaf.rise/2,s*leaf.length/2),new Vector(-s*leaf.width/2,0,c*leaf.width/2),new Vector(0,(leaf.arch+.004)/2+.0065,0)});
    }
    /** Separating-axis test of conservative sheared prisms, including thickness. */
    private static boolean overlap(Leaf a,Leaf b){
        if(a.maxY()+.006<b.minY()||b.maxY()+.006<a.minY())return false;
        var ea=envelope(a);var eb=envelope(b);var delta=new Vector(eb.center.x-ea.center.x,eb.center.y-ea.center.y,eb.center.z-ea.center.z);
        for(int i=0;i<3;i++){
            if(separates(ea.edges[(i+1)%3].cross(ea.edges[(i+2)%3]),delta,ea,eb)||separates(eb.edges[(i+1)%3].cross(eb.edges[(i+2)%3]),delta,ea,eb))return false;
            for(int j=0;j<3;j++)if(separates(ea.edges[i].cross(eb.edges[j]),delta,ea,eb))return false;
        }return true;
    }
    private static boolean separates(Vector axis,Vector delta,Envelope a,Envelope b){
        double squared=axis.dot(axis);if(squared<1e-20)return false;
        double radius=.006*Math.sqrt(squared);for(var edge:a.edges)radius+=Math.abs(axis.dot(edge));for(var edge:b.edges)radius+=Math.abs(axis.dot(edge));
        return Math.abs(axis.dot(delta))>radius;
    }
}
