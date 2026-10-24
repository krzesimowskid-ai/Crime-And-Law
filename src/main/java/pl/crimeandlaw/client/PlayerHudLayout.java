package pl.crimeandlaw.client;

/** Money stays beside the radar while respecting the actual vanilla HUD scale. */
public record PlayerHudLayout(GangLayout canvas,int mapX,int mapY,int moneyX,int moneyY,int chatShift) {
    public static final int MAP_WIDTH=136,MAP_HEIGHT=86,MONEY_WIDTH=148,MONEY_HEIGHT=46;
    public static PlayerHudLayout of(int width,int height){
        var canvas=GangLayout.of(width,height);float scale=canvas.scale();int mapX=14,mapY=canvas.canvasHeight()-104,moneyX=mapX+MAP_WIDTH+12,moneyY=mapY+MAP_HEIGHT-MONEY_HEIGHT;
        if((moneyX+MONEY_WIDTH)*scale>width/2.0-111&&moneyX*scale<width/2.0+111)moneyY=Math.min(moneyY,(int)Math.floor((height-60)/scale)-MONEY_HEIGHT);
        moneyY=Math.max(12,moneyY);int highest=(int)Math.floor(Math.min(mapY-3,moneyY)*scale);
        return new PlayerHudLayout(canvas,mapX,mapY,moneyX,moneyY,-Math.max(0,height-highest-30));
    }
}
