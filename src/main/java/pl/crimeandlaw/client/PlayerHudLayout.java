package pl.crimeandlaw.client;

/** Money stays beside the radar while respecting the actual vanilla HUD scale. */
public record PlayerHudLayout(GangLayout canvas,int mapX,int mapY,int moneyX,int moneyY,int chatShift) {
    public static final int MAP_WIDTH=136,MAP_HEIGHT=86,MONEY_WIDTH=106,MONEY_HEIGHT=28,CLOCK_HEIGHT=30,ROW_STEP=14,ICON_SIZE=14;
    public static PlayerHudLayout of(int width,int height){
        return of(width,height,MONEY_WIDTH);
    }
    public static PlayerHudLayout of(int width,int height,float contentWidth){
        var canvas=GangLayout.of(width,height);float scale=canvas.scale();int mapX=14,mapY=canvas.canvasHeight()-104,moneyX=mapX+MAP_WIDTH+10,moneyY=mapY+MAP_HEIGHT-MONEY_HEIGHT;
        float span=Math.clamp(contentWidth,40,MONEY_WIDTH);
        if((moneyX+span)*scale>width/2.0-96&&moneyX*scale<width/2.0+96)moneyY=Math.min(moneyY,(int)Math.floor((height-60)/scale)-MONEY_HEIGHT);
        moneyY=Math.max(12+CLOCK_HEIGHT,moneyY);int highest=(int)Math.floor(Math.min(mapY-3,moneyY-CLOCK_HEIGHT)*scale);
        return new PlayerHudLayout(canvas,mapX,mapY,moneyX,moneyY,-Math.max(0,height-highest-30));
    }
    public int clockY(){return moneyY-CLOCK_HEIGHT;}
}
