package pl.crimeandlaw.client;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pl.crimeandlaw.gang.GangDirectory.Theme;

final class MemberRow extends FlatButton {
    private final String name,role;
    private final boolean online;
    MemberRow(int x,int y,int width,String name,String role,boolean online,OnPress action,Supplier<Theme> theme,boolean selected){
        super(x,y,width,30,name,action,theme,selected);this.name=name;this.role=role;this.online=online;
    }
    @Override protected void renderLabel(GuiGraphics g,int x,int y){
        var font=Minecraft.getInstance().font;
        g.drawString(font,font.plainSubstrByWidth(name,width-26),x+8,y+5,0xFFE9E8EC,false);
        g.drawString(font,font.plainSubstrByWidth(role,width-26),x+8,y+17,0xFFAAA3B3,false);
        g.fill(x+width-13,y+12,x+width-8,y+17,online?0xFF72BC8C:0xFF77727F);
    }
}
