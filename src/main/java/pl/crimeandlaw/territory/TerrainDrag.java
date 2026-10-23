package pl.crimeandlaw.territory;

/** A continuous world-space camera; a server reply never resets a held gesture. */
public final class TerrainDrag {
    private boolean active,moved;
    private double pointerX,pointerY,x,z,originX,originZ,cellSize;
    public void begin(TerrainViewport origin,double pointerX,double pointerY,int radius,int pixels){
        begin(origin.x(),origin.z(),pointerX,pointerY,radius,pixels);
    }
    public void begin(double originX,double originZ,double pointerX,double pointerY,int radius,int pixels){
        if(radius<1||pixels<1||!Double.isFinite(originX)||!Double.isFinite(originZ))throw new IllegalArgumentException("Nieprawidłowy rozmiar mapy.");
        active=true;moved=false;this.pointerX=pointerX;this.pointerY=pointerY;
        x=this.originX=Math.clamp(originX,-TerrainViewport.LIMIT,TerrainViewport.LIMIT);
        z=this.originZ=Math.clamp(originZ,-TerrainViewport.LIMIT,TerrainViewport.LIMIT);cellSize=pixels/(double)(radius*2+1);
    }
    public void move(double pointerX,double pointerY){
        if(!active||!Double.isFinite(pointerX)||!Double.isFinite(pointerY))return;
        double dx=pointerX-this.pointerX,dy=pointerY-this.pointerY;
        moved|=Math.hypot(dx,dy)>3;
        if(moved){x=Math.clamp(originX-dx/cellSize,-TerrainViewport.LIMIT,TerrainViewport.LIMIT);z=Math.clamp(originZ-dy/cellSize,-TerrainViewport.LIMIT,TerrainViewport.LIMIT);}
    }
    public TerrainViewport viewport(){return new TerrainViewport((int)Math.round(x),(int)Math.round(z));}
    public double x(){return x;}
    public double z(){return z;}
    public double offsetX(int centerX){return (centerX-x)*cellSize;}
    public double offsetY(int centerZ){return (centerZ-z)*cellSize;}
    public boolean active(){return active;}
    public boolean moved(){return moved;}
    public void finish(){active=false;}
}
