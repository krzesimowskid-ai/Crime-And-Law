package pl.crimeandlaw.client;

/** Keep one radar/counter cluster; wrap the counters directly above the radar when needed. */
public record PlayerHudLayout(GangLayout canvas,int mapX,int mapY,int moneyX,int moneyY) {
    public static final int MAP_WIDTH=136,MAP_HEIGHT=86,MONEY_WIDTH=106,MONEY_HEIGHT=28,CLOCK_HEIGHT=30,ROW_STEP=14,ICON_SIZE=14;
    public static PlayerHudLayout of(int width,int height){
        return of(width,height,MONEY_WIDTH);
    }
    public static PlayerHudLayout of(int width,int height,float contentWidth){
        var canvas=GangLayout.of(width,height);float scale=canvas.scale();int mapX=6,mapY=canvas.canvasHeight()-MAP_HEIGHT-6,moneyX=mapX+MAP_WIDTH+10,moneyY=mapY+MAP_HEIGHT-MONEY_HEIGHT;
        float span=Math.clamp(contentWidth,40,MONEY_WIDTH);
        // The vanilla hotbar and health start 91 native GUI units left of center.
        // Keep one extra unit for shadows instead of reserving an unrelated 60-unit height.
        if((moneyX+span)*scale>width/2.0-92){moneyX=mapX;moneyY=mapY-MONEY_HEIGHT-4;}
        return new PlayerHudLayout(canvas,mapX,mapY,moneyX,moneyY);
    }
    public int clockY(){return moneyY-CLOCK_HEIGHT;}
}
