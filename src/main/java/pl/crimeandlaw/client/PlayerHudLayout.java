package pl.crimeandlaw.client;

/** Money stays beside the radar while respecting the actual vanilla HUD scale. */
public record PlayerHudLayout(GangLayout canvas,int mapX,int mapY,int moneyX,int moneyY,int chatShift) {
    public static PlayerHudLayout of(int width,int height){
        var canvas=GangLayout.of(width,height);float scale=canvas.scale();int mapX=14,mapY=canvas.canvasHeight()-112,moneyX=mapX+148,moneyY=mapY+9;
        if((moneyX+148)*scale>width/2.0-111&&moneyX*scale<width/2.0+111)moneyY=Math.min(moneyY,(int)Math.floor((height-60)/scale)-78);
        moneyY=Math.max(12,moneyY);int highest=(int)Math.floor(Math.min(mapY-3,moneyY)*scale);
        return new PlayerHudLayout(canvas,mapX,mapY,moneyX,moneyY,-Math.max(0,height-highest-30));
    }
}
