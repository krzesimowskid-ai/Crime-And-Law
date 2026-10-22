package pl.crimeandlaw.client;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import pl.crimeandlaw.gang.GangDirectory.*;

/** Anchored, read-only member portrait. Skin loading stays asynchronous and cached. */
final class MemberCard extends AbstractWidget {
    private final Member member;
    private final String role,date;
    private final boolean online;
    private final Supplier<Theme> theme;
    private final Supplier<PlayerSkin> skin;
    private final Runnable close;
    MemberCard(int rowX,int rowY,int rowWidth,int minX,int minY,int maxX,int maxY,Member member,String role,boolean online,Supplier<Theme> theme,Runnable close){
        super(0,0,252,151,Component.literal("Profil członka: "+member.name));
        setX(Math.clamp(rowX+rowWidth+10+width<=maxX?rowX+rowWidth+10:rowX-width-10,minX,maxX-width));
        setY(Math.clamp(rowY,minY,maxY-height));
        this.member=member;this.role=role;this.online=online;this.theme=theme;this.close=close;
        date=member.joinedAt==0?"Data nie została zapisana":DateTimeFormatter.ofPattern("d MMMM uuuu, HH:mm",Locale.forLanguageTag("pl-PL"))
            .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(member.joinedAt));
        var mc=Minecraft.getInstance();var info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(member.id);
        if(info!=null)skin=info::getSkin;
        else{
            var profile=new GameProfile(member.id,member.name);
            if(member.textures!=null&&!member.textures.isEmpty())profile.getProperties().put("textures",new Property("textures",member.textures,member.textureSignature==null||member.textureSignature.isEmpty()?null:member.textureSignature));
            skin=mc.getSkinManager().lookupInsecure(profile);
        }
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
        int x=getX(),y=getY(),accent=theme.get().borderArgb();var font=Minecraft.getInstance().font;
        g.fill(x+3,y+3,x+width+3,y+height+3,0x66000000);g.fill(x,y,x+width,y+height,0xFF191A22);
        g.fillGradient(x+1,y+1,x+width-1,y+22,0xFF302431,0xFF191A22);g.renderOutline(x,y,width,height,accent);
        g.drawString(font,"Członek gangu",x+10,y+8,0xFFAAA3B3,false);
        for(int i=0;i<7;i++){g.fill(x+width-16+i,y+8+i,x+width-15+i,y+9+i,0xFFE9E8EC);g.fill(x+width-10-i,y+8+i,x+width-9-i,y+9+i,0xFFE9E8EC);}
        g.fill(x+10,y+28,x+88,y+106,0xFF111219);g.renderOutline(x+10,y+28,78,78,0x554D4654);
        g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        portrait(g,skin.get(),x+13,y+34);com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        g.drawString(font,font.plainSubstrByWidth(member.name,width-110),x+100,y+33,0xFFF2EDF3,false);
        g.drawString(font,"Ranga",x+100,y+51,0xFFAAA3B3,false);
        g.drawString(font,font.plainSubstrByWidth(role,width-110),x+100,y+64,0xFFE9E8EC,false);
        g.fill(x+100,y+85,x+105,y+90,online?0xFF72BC8C:0xFF77727F);
        g.drawString(font,online?"Online":"Offline",x+111,y+84,0xFFAAA3B3,false);
        g.drawString(font,member.joinedAtFromHistory?"Dołączył — według rejestru":"Dołączył do gangu",x+12,y+115,0xFFAAA3B3,false);
        g.drawString(font,font.plainSubstrByWidth(date,width-24),x+12,y+130,0xFFE9E8EC,false);
    }
    private static void part(GuiGraphics g,ResourceLocation texture,int x,int y,int u,int v,int w,int h){part(g,texture,x,y,u,v,w,h,false);}
    private static void part(GuiGraphics g,ResourceLocation texture,int x,int y,int u,int v,int w,int h,boolean outer){
        int pad=outer?1:0;g.blit(texture,x-pad,y-pad,w*3+pad*2,h*3+pad*2,(float)u,(float)v,w,h,64,64);
    }
    private static void portrait(GuiGraphics g,PlayerSkin skin,int x,int y){
        var texture=skin.texture();boolean slim=skin.model()==PlayerSkin.Model.SLIM;int arm=slim?3:4;
        part(g,texture,x+24,y,8,8,8,8);
        part(g,texture,x+24,y+24,20,20,8,12);
        part(g,texture,x+24-arm*3,y+24,44,20,arm,12);
        part(g,texture,x+48,y+24,36,52,arm,12);
        // Hat, jacket and both sleeves are distinct alpha-blended outer layers.
        // A one-pixel rim keeps their silhouette visible at portrait scale.
        part(g,texture,x+24,y+24,20,36,8,12,true);
        part(g,texture,x+24-arm*3,y+24,44,36,arm,12,true);
        part(g,texture,x+48,y+24,52,52,arm,12,true);
        part(g,texture,x+24,y,40,8,8,8,true);
    }
    @Override public void onClick(double x,double y,int button){if(x>=getX()+width-22&&y<getY()+24)close.run();}
    @Override protected void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,Component.literal(member.name+", "+role+", "+(online?"online":"offline")));}
}
