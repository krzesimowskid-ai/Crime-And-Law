package pl.crimeandlaw.cultivation;

import pl.crimeandlaw.cultivation.PreparationPhysics.Point;
import pl.crimeandlaw.cultivation.PreparationPhysics.Stream;

/** Cosmetic particle contact against the hollow, octagonal pot, including its rim. */
public final class PotCollision {
    public enum Surface { SOIL, WALL, RIM, FLOOR }
    public record Contact(double time, Point point, Surface surface) {}
    private static final double COS=Math.cos(Math.PI/8);
    private PotCollision(){}
    /** Circumradius coordinate for the same octagon used by GardenMesh.ring. */
    public static double radius(Point p){
        double x=Math.abs(p.x()-.5),z=Math.abs(p.z()-.5);
        return Math.max(Math.max(x,z),(x+z)/Math.sqrt(2))/COS;
    }
    public static double innerRadius(double y){
        return y<=.43?.215+.095*Math.clamp((y-.055)/.375,0,1):.310+.008*Math.clamp((y-.43)/.05,0,1);
    }
    public static double outerRadius(double y){
        if(y<=.07)return .235+.015*Math.clamp((y-.025)/.045,0,1);
        if(y<=.40)return .25+.10*Math.clamp((y-.07)/.34,0,1);
        if(y<=.415)return .35+.025*Math.clamp((y-.40)/.015,0,1);
        return .375;
    }
    public static Surface surface(Point p,double fill,double particleRadius){
        if(p.y()<=.015+particleRadius)return Surface.FLOOR;
        if(p.y()<.025-particleRadius||p.y()>.48+particleRadius)return null;
        double r=radius(p),margin=particleRadius/COS;
        if(r>outerRadius(p.y())+margin)return null;
        if(p.y()<=.035+particleRadius)return Surface.FLOOR;
        if(r>=innerRadius(p.y())-margin)return p.y()>=.415-particleRadius?Surface.RIM:Surface.WALL;
        if(p.y()<=PreparationPhysics.soilY(fill)+particleRadius&&r<=PreparationPhysics.soilRadius(fill)+margin)return Surface.SOIL;
        return null;
    }
    /** Substeps prevent a slow frame from tunnelling across a thin wall or the upper rim. */
    public static Contact trace(Stream stream,double from,double to,double fill,double particleRadius){
        if(!Double.isFinite(from)||!Double.isFinite(to)||from<0||to<from)return null;
        double speed=Math.sqrt(stream.velocity().x()*stream.velocity().x()+stream.velocity().z()*stream.velocity().z())+Math.abs(stream.velocity().y())+PreparationPhysics.GRAVITY*to;
        int steps=Math.clamp((int)Math.ceil((to-from)*Math.max(1,speed)/.005),1,4096);
        double previous=from;
        for(int i=0;i<=steps;i++){
            double time=from+(to-from)*i/steps;Point point=PreparationPhysics.at(stream,time);
            Surface hit=surface(point,fill,particleRadius);
            if(hit!=null){
                double low=previous,high=time;
                for(int n=0;n<10&&high>low;n++){double mid=(low+high)/2;if(surface(PreparationPhysics.at(stream,mid),fill,particleRadius)==null)low=mid;else high=mid;}
                point=PreparationPhysics.at(stream,high);return new Contact(high,point,surface(point,fill,particleRadius));
            }
            previous=time;
        }
        return null;
    }
}
