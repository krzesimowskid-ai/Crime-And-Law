package pl.crimeandlaw.client;

import net.minecraft.client.gui.GuiGraphics;

/** Original vector silhouettes on a 128-unit canvas; no font-glyph or image dependency. */
final class MoneyIcons {
    private MoneyIcons(){}
    static void draw(GuiGraphics g,int x,int y,int size,boolean bank){
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size/128f,size/128f,1);
        if(bank){
            rounded(g,9,27,114,81,10,0xCC000000);rounded(g,5,21,114,81,10,0xFFDAE8F0);rounded(g,10,26,104,71,7,0xFF365C79);
            g.fill(11,39,113,52,0xFF162F44);rounded(g,22,62,25,19,3,0xFFEBD29B);
            g.fill(29,63,31,80,0xFF9D8050);g.fill(38,63,40,80,0xFF9D8050);g.fill(23,70,46,72,0xFF9D8050);
            rounded(g,63,67,37,5,2,0xFFE3EDF3);rounded(g,63,78,24,4,2,0xFF8CB0C9);
        }else{
            rounded(g,13,30,108,73,7,0xCC000000);rounded(g,9,25,109,73,7,0xFF335F48);rounded(g,3,18,110,73,7,0xFFD8EDDC);rounded(g,8,23,100,63,4,0xFF57966D);
            g.renderOutline(14,29,88,51,0xFFD1E5CB);ellipse(g,58,54,17,24,0xFFD1E5CB);ellipse(g,58,54,11,19,0xFF43805B);
            g.fill(55,36,60,73,0xFFD1E5CB);g.fill(48,43,67,48,0xFFD1E5CB);g.fill(48,60,67,65,0xFFD1E5CB);
            rounded(g,22,46,12,5,2,0xFFD1E5CB);rounded(g,82,58,12,5,2,0xFFD1E5CB);
        }
        g.pose().popPose();
    }
    private static void ellipse(GuiGraphics g,int cx,int cy,int rx,int ry,int color){for(int y=-ry;y<=ry;y++){int w=(int)Math.round(rx*Math.sqrt(Math.max(0,1-y*y/(double)(ry*ry))));g.fill(cx-w,cy+y,cx+w+1,cy+y+1,color);}}
    private static void rounded(GuiGraphics g,int x,int y,int w,int h,int radius,int color){
        g.fill(x+radius,y,x+w-radius,y+h,color);g.fill(x,y+radius,x+w,y+h-radius,color);
        for(int row=0;row<radius;row++){int inset=(int)Math.ceil(radius-Math.sqrt(radius*radius-Math.pow(radius-row-.5,2)));g.fill(x+inset,y+row,x+w-inset,y+row+1,color);g.fill(x+inset,y+h-row-1,x+w-inset,y+h-row,color);}
    }
}
