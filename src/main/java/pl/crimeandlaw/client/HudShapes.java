package pl.crimeandlaw.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

/** Fractional vector geometry, batched in the ordinary GUI buffer. */
final class HudShapes {
    private HudShapes(){}
    static void triangle(GuiGraphics g,float ax,float ay,float bx,float by,float cx,float cy,int color){
        // RenderType.gui culls back faces. Accept either path winding in screen coordinates.
        if((bx-ax)*(cy-ay)-(by-ay)*(cx-ax)>0){float tx=bx,ty=by;bx=cx;by=cy;cx=tx;cy=ty;}
        var matrix=g.pose().last().pose();VertexConsumer vertices=g.bufferSource().getBuffer(RenderType.gui());
        vertices.addVertex(matrix,ax,ay,0).setColor(color);vertices.addVertex(matrix,bx,by,0).setColor(color);
        vertices.addVertex(matrix,cx,cy,0).setColor(color);vertices.addVertex(matrix,cx,cy,0).setColor(color);
    }
    static void ellipse(GuiGraphics g,float cx,float cy,float rx,float ry,int color){
        for(int i=0;i<32;i++){double a=i*Math.PI/16,b=(i+1)*Math.PI/16;triangle(g,cx,cy,cx+(float)Math.cos(a)*rx,cy+(float)Math.sin(a)*ry,cx+(float)Math.cos(b)*rx,cy+(float)Math.sin(b)*ry,color);}
    }
    static void rounded(GuiGraphics g,float x,float y,float w,float h,float radius,int color){
        float cx=x+w/2,cy=y+h/2,previousX=x+w-radius,previousY=y;
        for(int corner=0;corner<4;corner++)for(int step=0;step<=8;step++){
            double angle=(-90+corner*90+step*90/8.0)*Math.PI/180;
            float ox=corner<2?x+w-radius:x+radius,oy=corner==0||corner==3?y+radius:y+h-radius;
            float nx=ox+(float)Math.cos(angle)*radius,ny=oy+(float)Math.sin(angle)*radius;
            triangle(g,cx,cy,previousX,previousY,nx,ny,color);previousX=nx;previousY=ny;
        }
        triangle(g,cx,cy,previousX,previousY,x+w-radius,y,color);
    }
}
