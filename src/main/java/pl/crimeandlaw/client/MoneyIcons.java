package pl.crimeandlaw.client;

import net.minecraft.client.gui.GuiGraphics;

/** Compact, original banknote and card silhouettes; fractional batched vector geometry. */
final class MoneyIcons {
    private MoneyIcons(){}
    static void draw(GuiGraphics g,int x,int y,int size,boolean bank){
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size/128f,size/128f,1);
        if(bank){
            rounded(g,8,29,112,76,12,0x90060B10);rounded(g,5,22,112,76,11,0xFFE2EDF4);rounded(g,11,28,100,64,6,0xFF5484A2);
            rounded(g,11,40,100,14,0,0xFF253E52);rounded(g,24,66,21,15,3,0xFFECD5A0);
            rounded(g,31,66,2.5f,15,0,0xFFB39A64);rounded(g,38,66,2.5f,15,0,0xFFB39A64);rounded(g,24,72,21,2,0,0xFFB39A64);
            rounded(g,58,69,36,4,2,0xFFEAF2F6);rounded(g,58,79,24,3,1.5f,0xFFB1CEDF);
            rounded(g,89,32,14,2.5f,1,0xFFA7C8DE);
        }else{
            rounded(g,12,32,109,71,9,0x90060B10);rounded(g,11,28,109,69,8,0xFF47765E);
            rounded(g,3,20,110,69,8,0xFFDDEEDD);rounded(g,9,26,98,57,4,0xFF6CA67B);
            rounded(g,16,32,84,45,3,0xFFB7D7B9);rounded(g,19,35,78,39,2,0xFF6CA67B);
            ellipse(g,58,54,17,23,0xFFDEEFDB);ellipse(g,58,54,11,17,0xFF659F75);
            rounded(g,56,40,4,28,2,0xFFDEEFDB);rounded(g,49,45,16,4,2,0xFFDEEFDB);rounded(g,51,59,16,4,2,0xFFDEEFDB);
            rounded(g,20,49,12,4,2,0xFFDEEFDB);rounded(g,84,57,12,4,2,0xFFDEEFDB);
        }
        g.pose().popPose();
    }
    private static void ellipse(GuiGraphics g,float cx,float cy,float rx,float ry,int color){HudShapes.ellipse(g,cx,cy,rx,ry,color);}
    private static void rounded(GuiGraphics g,float x,float y,float w,float h,float radius,int color){HudShapes.rounded(g,x,y,w,h,radius,color);}
}
