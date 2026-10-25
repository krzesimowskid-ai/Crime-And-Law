package pl.crimeandlaw.cultivation;

/** Bounded cosmetic motion only; rewards remain in HarvestController. */
public final class LeafMotion {
    public record Motion(double vx,double vz,double lift,double spinX,double spinZ,double phase){
        public double x(double t){return vx*t+Math.sin(t*5+phase)*.018*t;}
        public double y(double t){return lift*t-.58*t*t;}
        public double z(double t){return vz*t+Math.cos(t*4+phase)*.025*t;}
    }
    private LeafMotion(){}
    public static Motion cut(int id,long variation){
        var r=new java.util.Random(variation^((long)id*0x9E3779B97F4A7C15L));
        double a=id*2.399963229728653+r.nextDouble()*.75,speed=.16+r.nextDouble()*.16;
        return new Motion(Math.cos(a)*speed,Math.sin(a)*speed,.045+r.nextDouble()*.11,(r.nextBoolean()?1:-1)*(1.0+r.nextDouble()*2),(r.nextBoolean()?1:-1)*(.8+r.nextDouble()*2),r.nextDouble()*Math.PI*2);
    }
}
