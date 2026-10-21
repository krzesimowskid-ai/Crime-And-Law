package pl.crimeandlaw.client;

import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import pl.crimeandlaw.gang.GangDirectory.Theme;
import pl.crimeandlaw.territory.*;

/** Continuous surface map, optional chunk overlay and a separate terrain inspector. */
final class TerrainPanel extends AbstractWidget {
    private record Line(FormattedCharSequence text,int y,int color){}
    private final TerrainView view;
    private final int selectedX,selectedZ,radius;
    private final boolean grid;
    private final Supplier<Theme> theme;
    private final Consumer<TerrainView.Cell> selected;
    private final ScrollPosition scroll;
    private final List<Line> lines=new ArrayList<>();
    private final TerrainView.Cell[] cells=new TerrainView.Cell[TerrainView.SIDE*TerrainView.SIDE];
    static final int GRID=196,DETAILS=GRID+16;
    private int textHeight;
    TerrainPanel(int x,int y,int width,int height,TerrainView view,int selectedX,int selectedZ,int radius,boolean grid,Supplier<Theme> theme,ScrollPosition scroll,Consumer<TerrainView.Cell> selected){
        super(x,y,width,height,Component.literal("Mapa terenu"));this.view=view;this.selectedX=selectedX;this.selectedZ=selectedZ;this.radius=radius;this.grid=grid;this.theme=theme;this.scroll=scroll;this.selected=selected;
        if(view!=null)for(var cell:view.cells()){
            int dx=cell.x()-view.centerX()+TerrainView.RADIUS,dz=cell.z()-view.centerZ()+TerrainView.RADIUS;
            if(dx>=0&&dx<TerrainView.SIDE&&dz>=0&&dz<TerrainView.SIDE)cells[dz*TerrainView.SIDE+dx]=cell;
        }
        TerrainMapTexture.update(view);buildDetails();scroll.range(height-12,textHeight);
    }
    private TerrainView.Cell cell(int x,int z){
        if(view==null)return null;int dx=x-view.centerX()+TerrainView.RADIUS,dz=z-view.centerZ()+TerrainView.RADIUS;
        return dx>=0&&dx<TerrainView.SIDE&&dz>=0&&dz<TerrainView.SIDE?cells[dz*TerrainView.SIDE+dx]:null;
    }
    private void line(String text,int color){
        var font=Minecraft.getInstance().font;
        for(var part:font.split(Component.literal(text),width-DETAILS-18)){lines.add(new Line(part,textHeight,color));textHeight+=11;}
        textHeight+=4;
    }
    private void buildDetails(){
        if(view==null){line("Pobieranie mapy…",0xFFE9E8EC);return;}
        line("Chunk "+selectedX+", "+selectedZ,0xFFEFE9F3);
        line("X: "+selectedX*16+"…"+(selectedX*16+15)+"  Z: "+selectedZ*16+"…"+(selectedZ*16+15),0xFFAAA3B3);
        var cell=cell(selectedX,selectedZ);
        if(cell==null||cell.status()!=TerrainView.Status.READY){
            line(cell!=null&&cell.status()==TerrainView.Status.PENDING?"Trwa rozpoznawanie pierwotnego terenu i struktur…":"Brak rozpoznania. Podejdź bliżej, aby wczytać teren.",0xFFAAA3B3);return;
        }
        line(cell.owner()==null?"Teren neutralny":"Gang: "+cell.ownerName(),0xFFC7B7C1);
        line("Wysokość: "+cell.minHeight()+"…"+cell.maxHeight(),0xFFAAA3B3);
        line("Biom: "+cell.biome().replace("minecraft:",""),0xFFAAA3B3);
        line(cell.source()==TerrainProfile.Source.GENERATED_SNAPSHOT?"Źródło cech: zapis z generowania":"Źródło cech: rekonstrukcja generatora",0xFFAAA3B3);
        line("Stałe cechy terenu",0xFFEFE9F3);
        if(cell.traits().isEmpty())line("Brak wyróżnionych cech.",0xFFAAA3B3);
        for(var trait:TerrainProfile.Trait.values())if(cell.traits().contains(trait))line("• "+trait.label(),0xFFD8D2DE);
        line("Premie",0xFFEFE9F3);
        line("Brak przyznanych premii. Same cechy terenu nie zapewniają bonusów.",0xFFAAA3B3);
        line("Wymagania poszczególnych premii oraz przejmowanie terenów będą dodawane osobno.",0xFF8E869A);
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
        var font=Minecraft.getInstance().font;int x=getX(),y=getY();Theme t=theme.get();
        g.fill(x-1,y-1,x+GRID+1,y+GRID+1,0xFF17191F);
        g.fill(x,y,x+GRID,y+GRID,0xFF29262B);TerrainMapTexture.draw(g,x,y,GRID,radius);
        if(view!=null){
            int side=radius*2+1;double cellSize=GRID/(double)side;
            // Overlays have no gaps and never replace the underlying terrain image.
            for(int row=0;row<side;row++)for(int col=0;col<side;col++){
                var cell=cell(view.centerX()-radius+col,view.centerZ()-radius+row);if(cell==null)continue;
                int px=x+(int)Math.round(col*cellSize),py=y+(int)Math.round(row*cellSize);
                int w=(int)Math.round((col+1)*cellSize)-(px-x),h=(int)Math.round((row+1)*cellSize)-(py-y);
                if(cell.owner()!=null){
                    var gang=ClientState.gang();int color=gang!=null&&cell.owner().equals(gang.id)?0x3062BD86:0x30BD6A70;
                    g.fill(px,py,px+w,py+h,color);
                }
                if(grid){g.fill(px,py,px+w,py+1,0x55303339);g.fill(px,py,px+1,py+h,0x55303339);}
                if(cell.x()==selectedX&&cell.z()==selectedZ){g.renderOutline(px,py,w,h,0xDDFFFFFF);g.renderOutline(px+1,py+1,w-2,h-2,t.borderArgb());}
            }
            double blocks=side*16.0;
            int px=x+(int)Math.round((view.playerX()-(view.centerX()-radius)*16.0)*GRID/blocks);
            int py=y+(int)Math.round((view.playerZ()-(view.centerZ()-radius)*16.0)*GRID/blocks);
            g.pose().pushPose();g.pose().translate(px,py,0);g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(view.yaw()));
            g.fill(-3,-3,4,3,0xEE17191F);g.fill(-2,-2,3,3,0xFFF5EFF4);g.fill(-1,3,2,5,0xFFF5EFF4);g.fill(0,5,1,6,0xFFF5EFF4);g.pose().popPose();
            g.fill(x+4,y+4,x+17,y+17,0xAA17191F);g.drawString(font,"N",x+8,y+7,0xFFEFE9F3,false);
            int bar=(int)Math.round(32*GRID/blocks);
            g.fill(x+5,y+GRID-18,x+bar+13,y+GRID-4,0xAA17191F);
            g.fill(x+9,y+GRID-8,x+bar+9,y+GRID-7,0xFFEFE9F3);
            g.drawString(font,"32 m",x+9,y+GRID-17,0xFFEFE9F3,false);
            g.drawString(font,"X: "+(int)Math.floor(view.playerX())+"  Z: "+(int)Math.floor(view.playerZ()),x,y+GRID+9,0xFFAAA3B3,false);
            g.drawString(font,"Zasięg: "+(int)blocks+" × "+(int)blocks+" bloków",x,y+GRID+23,0xFFAAA3B3,false);
        }else g.drawCenteredString(font,"Pobieranie mapy…",x+GRID/2,y+GRID/2,0xFFAAA3B3);
        g.fill(x+DETAILS-8,y,x+DETAILS-7,y+height,0x44000000|t.border());
        scissor(g,x+DETAILS,y+2,x+width-10,y+height-4);
        for(Line line:lines)g.drawString(font,line.text(),x+DETAILS,y+6+line.y()-(int)scroll.offset(),line.color(),false);
        g.disableScissor();
        if(scroll.maximum()>0){
            g.fill(x+width-5,y+2,x+width-2,y+height-4,0x44262934);
            int thumb=Math.max(18,(height-6)*(height-12)/Math.max(1,textHeight));
            int py=y+2+(int)((height-6-thumb)*scroll.offset()/scroll.maximum());
            g.fill(x+width-5,py,x+width-2,py+thumb,t.borderArgb());
        }
    }
    private void scissor(GuiGraphics g,int x1,int y1,int x2,int y2){
        float scale=GangLayout.of(Minecraft.getInstance().screen.width,Minecraft.getInstance().screen.height).scale();
        g.pose().pushPose();g.pose().last().pose().identity();
        g.enableScissor((int)Math.floor(x1*scale),(int)Math.floor(y1*scale),(int)Math.ceil(x2*scale),(int)Math.ceil(y2*scale));g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button!=0||!isMouseOver(x,y)||view==null)return false;
        if(x<getX()+GRID&&y<getY()+GRID){
            int side=radius*2+1;
            var cell=cell(view.centerX()-radius+(int)((x-getX())*side/GRID),view.centerZ()-radius+(int)((y-getY())*side/GRID));
            if(cell!=null){selected.accept(cell);return true;}
        }
        return x>=getX()+DETAILS;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(x>=getX()+DETAILS&&isMouseOver(x,y)){scroll.scroll(-vertical*24);return true;}return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,getMessage());}
}
