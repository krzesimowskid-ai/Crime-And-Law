package pl.crimeandlaw.cultivation;

import java.util.List;
import java.util.ArrayList;

/** Shared local-space geometry: the mesh, stream emitter and server target use the same transforms. */
public final class PreparationPhysics {
    public static final double SCALE=.75, GRAVITY=2.8, RIM=.48, BOTTOM=.065, TOP=.385;
    public record Point(double x,double y,double z){
        public Point add(Point p){return new Point(x+p.x,y+p.y,z+p.z);}public Point scale(double n){return new Point(x*n,y*n,z*n);}
    }
    public record Pose(double x,double y,double z,double roll,double pitch){
        public Point point(Point local){return new Point(x,y,z).add(rotate(new Point(local.x-.5,local.y-.45,local.z-.5).scale(SCALE),roll,pitch));}
    }
    public record Stream(Point source,Point velocity,boolean flowing){}
    private PreparationPhysics(){}
    public static Point rotate(Point p,double roll,double pitch){double c=Math.cos(roll),s=Math.sin(roll),x=c*p.x-s*p.y,y=s*p.x+c*p.y;return new Point(x,Math.cos(pitch)*y-Math.sin(pitch)*p.z,Math.sin(pitch)*y+Math.cos(pitch)*p.z);}
    public static double soilY(double fill){return BOTTOM+(TOP-BOTTOM)*Math.clamp(fill,0,1);}
    public static double soilRadius(double fill){return .2035+.0811*Math.clamp(fill,0,1);}
    public static boolean inside(double x,double z,double radius){x=Math.abs(x-.5);z=Math.abs(z-.5);return Math.max(x,z)<=radius*.9238795&&x+z<=radius*1.306563;}
    public static boolean bounded(Pose p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z)&&Double.isFinite(p.roll)&&Double.isFinite(p.pitch)&&p.x>=-.45&&p.x<=1.45&&p.z>=-.45&&p.z<=1.45&&p.y>=.60&&p.y<=1.55&&Math.abs(p.roll)<=Math.PI+1e-5&&Math.abs(p.pitch)<=Math.PI+1e-5;}
    public static Stream stream(Pose pose,boolean water,boolean held){
        var local=water?rose(1,1):new Point(.5,.82,.5);
        var normal=rotate(water?new Point(.809,.588,0):new Point(0,1,0),pose.roll,pose.pitch);
        return new Stream(pose.point(local),normal.scale(water?.46:.20),held&&normal.y<(water?-.12:-.30));
    }
    /** Nine holes are transformed with the actual rose, rather than a detached central emitter. */
    public static List<Stream> streams(Pose pose,boolean water,boolean held){
        if(!water){
            var list=new ArrayList<Stream>(15);var middle=stream(pose,false,held);
            for(int row=0;row<5;row++)for(int column=0;column<3;column++)
                list.add(new Stream(pose.point(bagOpening(row,column)),middle.velocity,middle.flowing));
            return list;
        }
        var list=new ArrayList<Stream>(9);var middle=stream(pose,true,held);
        for(int i=0;i<3;i++)for(int j=0;j<3;j++){
            var spread=rotate(new Point(-.588*(i-1)*.019,.809*(i-1)*.019,(j-1)*.019),pose.roll,pose.pitch);
            list.add(new Stream(pose.point(rose(i,j)),middle.velocity.add(spread),middle.flowing));
        }return list;
    }
    public static Point rose(int row,int column){return new Point(.61,.26,.44).add(rotate(new Point(.4625,-.0385+row*.055,-.0085+column*.055),Math.toRadians(36),0));}
    /** The whole cut opening, inset from its attached folded lips. */
    public static Point bagOpening(int row,int column){return new Point(.28+row*.11,.82,.38+column*.12);}
    /** Mouse moves the held tool; WASD changes its orientation, with no automatic tilt. */
    public static Pose tilt(Pose p,double right,double forward,double seconds){
        double length=Math.hypot(right,forward);if(length>1){right/=length;forward/=length;}
        double amount=1.9*Math.clamp(seconds,0,.12);
        return new Pose(p.x,p.y,p.z,Math.clamp(p.roll-right*amount,-Math.PI,Math.PI),Math.clamp(p.pitch-forward*amount,-Math.PI,Math.PI));
    }
    public static double hitFraction(Pose pose,boolean water,boolean held,double fill){var jets=streams(pose,water,held);return jets.stream().filter(s->reachesSoil(s,fill)).count()/(double)jets.size();}
    /** Camera-local horizontal translation; independent of the model's pouring rotation. */
    public static Pose move(Pose p,double right,double forward,double yaw,double seconds){
        double length=Math.hypot(right,forward);if(length>1){right/=length;forward/=length;}
        double d=.46*Math.clamp(seconds,0,.12),x=p.x+d*(Math.cos(yaw)*right+Math.sin(yaw)*forward),z=p.z+d*(Math.sin(yaw)*right-Math.cos(yaw)*forward);
        return new Pose(Math.clamp(x,-.40,1.40),p.y,Math.clamp(z,-.40,1.40),p.roll,p.pitch);
    }
    public static double flight(Point p,Point velocity,double ground){double h=p.y-ground;if(h<0||!Double.isFinite(h))return -1;return (velocity.y+Math.sqrt(velocity.y*velocity.y+2*GRAVITY*h))/GRAVITY;}
    public static Point at(Stream s,double seconds){return s.source.add(s.velocity.scale(seconds)).add(new Point(0,-.5*GRAVITY*seconds*seconds,0));}
    public static boolean reachesSoil(Stream s,double fill){
        if(!s.flowing||s.source.y<RIM)return false;
        double rimTime=flight(s.source,s.velocity,RIM);if(rimTime<0)return false;var rim=at(s,rimTime);if(!inside(rim.x,rim.z,.310))return false;
        double t=flight(s.source,s.velocity,soilY(fill));var hit=at(s,t);return t>=0&&inside(hit.x,hit.z,soilRadius(fill));
    }
}
