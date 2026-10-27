package pl.crimeandlaw.client.cultivation;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import pl.crimeandlaw.cultivation.LeafMotion;

/** Rigid leaf motion and removal only after its entire silhouette leaves the bottom of the view. */
public final class LeafFall {
    private LeafFall(){}
    public static Matrix4f transform(AureliaMesh.Leaf leaf,LeafMotion.Motion motion,double seconds){
        var pivot=leaf.point(.48f,0);float t=(float)seconds;
        return new Matrix4f().translate((float)motion.x(t),(float)motion.y(t),(float)motion.z(t))
            .translate((float)pivot.x,(float)pivot.y,(float)pivot.z)
            .rotateX((float)(motion.spinX()*t+Math.sin(t*6+motion.phase())*.20*t))
            .rotateY((float)(motion.spinZ()*.45*t)).rotateZ((float)(motion.spinZ()*t))
            .translate((float)-pivot.x,(float)-pivot.y,(float)-pivot.z);
    }
    public static boolean below(Matrix4f camera,Matrix4f motion,AureliaMesh.Leaf leaf,float bottom){
        var projected=new Matrix4f(camera).mul(motion);
        for(float t:new float[]{0,.42f,.72f,1})for(float side:new float[]{-1,1})for(float d:new float[]{-.007f,.007f}){
            var p=leaf.point(t,side);var v=projected.transformPosition(new Vector3f((float)p.x,(float)p.y+d,(float)p.z));
            if(v.y<=bottom+2)return false;
        }
        return true;
    }
}
