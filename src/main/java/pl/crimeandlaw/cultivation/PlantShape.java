package pl.crimeandlaw.cultivation;

import java.util.*;

/** A permanent, cosmetic growth plan derived from the existing persisted plant identity. */
public final class PlantShape {
    public static final double HALF_THICKNESS=.0075;
    public record Point(double x,double y,double z){}
    public record Leaf(int node,double height,double angle,double length,double width,double rise,double arch,double petiole,double roll,double cup,double droop){
        public double bladeLength(){return length-petiole;}
        public double stalkRise(){return petiole*.35;}
        public double hump(){return arch+droop/4;}
        public double lift(double t){return stalkRise()+rise*t+4*arch*t*(1-t)-droop*t*t;}
        public Point point(double t,double side){
            double c=Math.cos(angle),s=Math.sin(angle),along=petiole+bladeLength()*t,across=side*width/2*Math.cos(roll);
            return new Point(c*along-s*across,height+lift(t)+side*width/2*Math.sin(roll)-cup*side*side*Math.sin(Math.PI*t),s*along+c*across);
        }
        public double minY(){return height+stalkRise()+Math.min(0,rise-droop)-cup-Math.abs(width/2*Math.sin(roll))-HALF_THICKNESS;}
        public double maxY(){return height+stalkRise()+Math.max(0,rise-droop)+hump()+Math.abs(width/2*Math.sin(roll))+HALF_THICKNESS;}
    }
    // The first three harvestable leaves span the plant; later growth fills the gaps.
    private static final int[] NODES={0,2,7,4,6,1,5,3};
    private record Vector(double x,double y,double z){
        Vector cross(Vector b){return new Vector(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x);}
        double dot(Vector b){return x*b.x+y*b.y+z*b.z;}
    }
    private record Envelope(Vector center,Vector[] edges){}
    private PlantShape(){}
    public static long seed(UUID identity){return identity.getMostSignificantBits()^Long.rotateLeft(identity.getLeastSignificantBits(),23);}
    public static List<Leaf> create(long seed){
        long mixed=(seed^(seed>>>30))*0xBF58476D1CE4E5B9L;mixed=(mixed^(mixed>>>27))*0x94D049BB133111EBL;mixed^=mixed>>>31;
        var random=new Random(mixed);double phase=random.nextDouble()*Math.PI*2;
        // Lower nodes carry opposite pairs; upper leaves alternate around the stem.
        double lower=.215+random.nextDouble()*.015,middle=.395+random.nextDouble()*.015;
        double[] heights={lower,lower+.004,middle,middle+.004,.555+random.nextDouble()*.012,.66+random.nextDouble()*.012,.765+random.nextDouble()*.012,.86+random.nextDouble()*.012};
        // First try the full inclination range. A bounded reduction guarantees a
        // separated layout even for an unusually crowded deterministic seed.
        for(int pass=0;pass<9;pass++){
            var leaves=new ArrayList<Leaf>(8);double strength=Math.pow(.8,pass);
            for(int id=0;id<8;id++){
                int node=NODES[id];double taper=1-node*.22/7;
                double length=(.445+random.nextDouble()*.045)*taper,width=(.36+random.nextDouble()*.05)*taper,petiole=.07+random.nextDouble()*.025;
                double azimuth=phase+(node<4?(node/2)*Math.PI/2+(node%2)*Math.PI:Math.PI+(node-4)*2.399963229728653);
                for(int attempt=0;attempt<192;attempt++){
                    double angle=azimuth+(random.nextDouble()-.5)*.36,roll=(random.nextDouble()-.5)*.30;
                    double arch=(.010+random.nextDouble()*.008)*strength,cup=(.014+random.nextDouble()*.009)*strength,droop=(node<4?.018:.008)+random.nextDouble()*.008;
                    double rise=(node<2?-.025-random.nextDouble()*.025:node<4?.01+random.nextDouble()*.02:.045+random.nextDouble()*.025)*strength;
                    rise=Math.max(.13-heights[node]-petiole*.35+droop+cup+Math.abs(width/2*Math.sin(roll)),rise);
                    var candidate=new Leaf(node,heights[node],angle,length,width,rise,arch,petiole,roll,cup,droop);
                    if(leaves.stream().noneMatch(other->overlap(candidate,other))){leaves.add(candidate);break;}
                }
                if(leaves.size()!=id+1)break;
            }
            if(leaves.size()==8)return List.copyOf(leaves);
        }
        throw new IllegalStateException("Cannot separate plant leaves");
    }
    private static Envelope envelope(Leaf leaf){
        double c=Math.cos(leaf.angle),s=Math.sin(leaf.angle),along=leaf.petiole+leaf.bladeLength()/2;
        return new Envelope(new Vector(c*along,leaf.height+leaf.stalkRise()+(leaf.rise-leaf.droop)/2+(leaf.hump()-leaf.cup)/2,s*along),new Vector[]{
            new Vector(c*leaf.bladeLength()/2,(leaf.rise-leaf.droop)/2,s*leaf.bladeLength()/2),new Vector(-s*leaf.width/2*Math.cos(leaf.roll),leaf.width/2*Math.sin(leaf.roll),c*leaf.width/2*Math.cos(leaf.roll)),new Vector(0,(leaf.hump()+leaf.cup)/2+HALF_THICKNESS,0)});
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
