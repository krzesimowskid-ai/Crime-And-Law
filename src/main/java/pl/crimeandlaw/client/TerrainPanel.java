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
    private TerrainView view;
    private final TerrainView.Cell knownSelection;
    private final int selectedX,selectedZ,radius;
    private final boolean grid;
    private final Supplier<Theme> theme;
    private final Consumer<TerrainView.Cell> selected;
    private final IntConsumer zoom;
    private final ScrollPosition scroll;
    private final List<Line> lines=new ArrayList<>();
    private TerrainView.Cell[] cells=new TerrainView.Cell[0];
    static final int GRID=196,DETAILS=GRID+16;
    private int textHeight;
    private final TerrainDrag drag=new TerrainDrag();
    private double dragX,dragY;
    TerrainPanel(int x,int y,int width,int height,TerrainView view,int selectedX,int selectedZ,TerrainView.Cell knownSelection,int radius,boolean grid,Supplier<Theme> theme,ScrollPosition scroll,Consumer<TerrainView.Cell> selected,IntConsumer zoom){
        super(x,y,width,height,Component.literal("Mapa terenu"));this.view=view;this.knownSelection=knownSelection;this.selectedX=selectedX;this.selectedZ=selectedZ;this.radius=radius;this.grid=grid;this.theme=theme;this.scroll=scroll;this.selected=selected;this.zoom=zoom;
        update(view);
    }
    boolean dragging(){return drag.active();}
    void update(TerrainView next){
        view=next;int side=view==null?0:view.radius()*2+1;cells=new TerrainView.Cell[side*side];
        if(view!=null)for(var cell:view.cells()){
            int dx=cell.x()-view.centerX()+view.radius(),dz=cell.z()-view.centerZ()+view.radius();
            if(dx>=0&&dx<side&&dz>=0&&dz<side)cells[dz*side+dx]=cell;
        }
        updateOffsets();
        TerrainMapTexture.update(view);lines.clear();textHeight=0;buildDetails();scroll.range(height-12,textHeight);
    }
    private void updateOffsets(){
        double size=GRID/(double)(radius*2+1);
        dragX=view==null?0:(view.centerX()-TerrainClient.cameraX())*size;
        dragY=view==null?0:(view.centerZ()-TerrainClient.cameraZ())*size;
    }
    private TerrainView.Cell cell(int x,int z){
        if(view==null)return null;int side=view.radius()*2+1,dx=x-view.centerX()+view.radius(),dz=z-view.centerZ()+view.radius();
        return dx>=0&&dx<side&&dz>=0&&dz<side?cells[dz*side+dx]:null;
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
        var cell=cell(selectedX,selectedZ);if(cell==null&&knownSelection!=null&&knownSelection.x()==selectedX&&knownSelection.z()==selectedZ)cell=knownSelection;
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
        line("Bonusy terenu",0xFFEFE9F3);
        line("Brak przyznanych bonusów.",0xFFAAA3B3);
        if(CaptureClient.matches(selectedX,selectedZ)){
            if(!CaptureClient.text("ownerName").isEmpty())line("Właściciel: "+CaptureClient.text("ownerName"),0xFFC7B7C1);
            if(CaptureClient.state.has("battle")){
                line(CaptureClient.phase(),0xFFEFE9F3);
                line("Pozostało: "+CaptureClient.duration(CaptureClient.state.getAsJsonObject("battle").get("remaining").getAsLong()),0xFFE9E8EC);
                var participants=CaptureClient.state.getAsJsonObject("battle").getAsJsonArray("participants");
                long active=participants.asList().stream().filter(p->!p.getAsJsonObject().get("eliminated").getAsBoolean()&&p.getAsJsonObject().get("present").getAsBoolean()).count();
                line("Obecni uczestnicy: "+active,0xFFAAA3B3);
            }
        }
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
        updateOffsets();
        var font=Minecraft.getInstance().font;int x=getX(),y=getY();Theme t=theme.get();
        g.fill(x-1,y-1,x+GRID+1,y+GRID+1,0xFF17191F);
        g.fill(x,y,x+GRID,y+GRID,0xFF29262B);
        scissor(g,x,y,x+GRID,y+GRID);g.pose().pushPose();g.pose().translate(dragX,dragY,0);
        TerrainMapTexture.draw(g,x,y,GRID,radius);
        if(view!=null){
            int side=radius*2+1;double cellSize=GRID/(double)side;
            // Overlays have no gaps and never replace the underlying terrain image.
            for(int row=-1;row<=side;row++)for(int col=-1;col<=side;col++){
                var cell=cell(view.centerX()-radius+col,view.centerZ()-radius+row);if(cell==null)continue;
                int px=x+(int)Math.round(col*cellSize),py=y+(int)Math.round(row*cellSize);
                int w=(int)Math.round((col+1)*cellSize)-(px-x),h=(int)Math.round((row+1)*cellSize)-(py-y);
                if(cell.owner()!=null){
                    var gang=ClientState.gang();int color=gang!=null&&cell.owner().equals(gang.id)?0x30000000|gang.mapColor:0x30BD6A70;
                    g.fill(px,py,px+w,py+h,color);
                }
                for(var mark:CaptureClient.marks(cell.x(),cell.z())){
                    int color=mark.get("color").getAsInt(),inset=mark.get("shared").getAsBoolean()?0:Math.min(2,w/4);
                    g.fill(px+inset,py+inset,px+w-inset,py+h-inset,0x50000000|color);
                    g.renderOutline(px+inset,py+inset,w-inset*2,h-inset*2,0xCC000000|color);
                }
                if(CaptureClient.event(cell.x(),cell.z()))g.renderOutline(px+1,py+1,Math.max(1,w-2),Math.max(1,h-2),0xFFFFCC71);
                if(grid){g.fill(px,py,px+w,py+1,0x55303339);g.fill(px,py,px+1,py+h,0x55303339);}
                if(cell.x()==selectedX&&cell.z()==selectedZ){g.renderOutline(px,py,w,h,0xDDFFFFFF);g.renderOutline(px+1,py+1,w-2,h-2,t.borderArgb());}
            }
            double blocks=side*16.0;
            int px=x+(int)Math.round((view.playerX()-(view.centerX()-radius)*16.0)*GRID/blocks);
            int py=y+(int)Math.round((view.playerZ()-(view.centerZ()-radius)*16.0)*GRID/blocks);
            if(px>=x-8&&px<=x+GRID+8&&py>=y-8&&py<=y+GRID+8){
                g.pose().pushPose();g.pose().translate(px,py,0);g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(view.yaw()));
                g.fill(-3,-3,4,3,0xEE17191F);g.fill(-2,-2,3,3,0xFFF5EFF4);g.fill(-1,3,2,5,0xFFF5EFF4);g.fill(0,5,1,6,0xFFF5EFF4);g.pose().popPose();
            }
            g.pose().popPose();g.disableScissor();
            g.fill(x+4,y+4,x+17,y+17,0xAA17191F);g.drawString(font,"N",x+8,y+7,0xFFEFE9F3,false);
            int bar=(int)Math.round(32*GRID/blocks);
            g.fill(x+5,y+GRID-18,x+bar+13,y+GRID-4,0xAA17191F);
            g.fill(x+9,y+GRID-8,x+bar+9,y+GRID-7,0xFFEFE9F3);
            g.drawString(font,"32 m",x+9,y+GRID-17,0xFFEFE9F3,false);
            g.drawString(font,"Chunk: "+view.centerX()+", "+view.centerZ(),x,y+GRID+9,0xFFAAA3B3,false);
            g.drawString(font,"Obszar: "+(int)blocks+" × "+(int)blocks+" bloków",x,y+GRID+23,0xFFAAA3B3,false);
        }else {g.pose().popPose();g.disableScissor();g.drawCenteredString(font,"Pobieranie mapy…",x+GRID/2,y+GRID/2,0xFFAAA3B3);}
        g.fill(x+DETAILS-8,y,x+DETAILS-7,y+height,0x44000000|t.border());
        scissor(g,x+DETAILS,y+2,x+width-12,y+height-4);
        for(Line line:lines)g.drawString(font,line.text(),x+DETAILS,y+6+line.y()-(int)scroll.offset(),line.color(),false);
        g.disableScissor();
    }
    private void scissor(GuiGraphics g,int x1,int y1,int x2,int y2){
        float scale=GangLayout.of(Minecraft.getInstance().screen.width,Minecraft.getInstance().screen.height).scale();
        g.pose().pushPose();g.pose().last().pose().identity();
        g.enableScissor((int)Math.floor(x1*scale),(int)Math.floor(y1*scale),(int)Math.ceil(x2*scale),(int)Math.ceil(y2*scale));g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button!=0||!isMouseOver(x,y)||view==null)return false;
        if(x<getX()+GRID&&y<getY()+GRID){
            updateOffsets();drag.begin(TerrainClient.cameraX(),TerrainClient.cameraZ(),x,y,radius,GRID);return true;
        }
        return x>=getX()+DETAILS&&x<getX()+width-10;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button!=0||!drag.active())return false;
        drag.move(x,y);
        if(drag.moved()){
            TerrainClient.moveTo(drag.x(),drag.z());updateOffsets();
        }
        return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button!=0||!drag.active())return false;
        drag.finish();
        updateOffsets();
        // The continuous camera outlives the gesture, including its fractional part.
        if(drag.moved()){TerrainClient.pan(0,0);return true;}
        if(x>=getX()&&x<getX()+GRID&&y>=getY()&&y<getY()+GRID){
            int side=radius*2+1;
            var cell=cell(view.centerX()-radius+(int)Math.floor((x-getX()-dragX)*side/GRID),view.centerZ()-radius+(int)Math.floor((y-getY()-dragY)*side/GRID));
            if(cell!=null)selected.accept(cell);
        }
        return true;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(!isMouseOver(x,y))return false;
        if(x>=getX()+DETAILS){scroll.scroll(-vertical*24);return true;}
        if(x<getX()+GRID&&y<getY()+GRID&&vertical!=0){zoom.accept(vertical>0?-1:1);return true;}
        return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,getMessage());}
}
