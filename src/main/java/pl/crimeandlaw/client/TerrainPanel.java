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

/** Real server summaries in a chunk grid with an independently scrollable inspector. */
final class TerrainPanel extends AbstractWidget {
    private record Line(FormattedCharSequence text,int y,int color){}
    private final TerrainView view;
    private final int selectedX,selectedZ;
    private final Supplier<Theme> theme;
    private final Consumer<TerrainView.Cell> selected;
    private final ScrollPosition scroll;
    private final List<Line> lines=new ArrayList<>();
    private final TerrainView.Cell[] cells=new TerrainView.Cell[49];
    private final int readyCount;
    private static final int CELL=28,GRID=7*CELL,DETAILS=GRID+16;
    private int textHeight;
    TerrainPanel(int x,int y,int width,int height,TerrainView view,int selectedX,int selectedZ,Supplier<Theme> theme,ScrollPosition scroll,Consumer<TerrainView.Cell> selected){
        super(x,y,width,height,Component.literal("Mapa terenu"));this.view=view;this.selectedX=selectedX;this.selectedZ=selectedZ;this.theme=theme;this.scroll=scroll;this.selected=selected;
        int ready=0;if(view!=null)for(var cell:view.cells()){
            int dx=cell.x()-view.centerX()+3,dz=cell.z()-view.centerZ()+3;
            if(dx>=0&&dx<7&&dz>=0&&dz<7)cells[dz*7+dx]=cell;
            if(cell.status()==TerrainView.Status.READY)ready++;
        }
        readyCount=ready;
        buildDetails();scroll.range(height-12,textHeight);
    }
    private TerrainView.Cell cell(int x,int z){
        if(view==null)return null;int dx=x-view.centerX()+3,dz=z-view.centerZ()+3;
        return dx>=0&&dx<7&&dz>=0&&dz<7?cells[dz*7+dx]:null;
    }
    private void line(String text,int color){
        var font=Minecraft.getInstance().font;
        for(var part:font.split(Component.literal(text),width-DETAILS-18)){lines.add(new Line(part,textHeight,color));textHeight+=11;}
        textHeight+=4;
    }
    private void buildDetails(){
        if(view==null){line("Pobieranie terenu…",0xFFE9E8EC);return;}
        line("Chunk "+selectedX+", "+selectedZ,0xFFEFE9F3);
        line("X: "+selectedX*16+"…"+(selectedX*16+15)+"  Z: "+selectedZ*16+"…"+(selectedZ*16+15),0xFFAAA3B3);
        var cell=cell(selectedX,selectedZ);
        if(cell==null||cell.status()!=TerrainView.Status.READY){
            line(cell!=null&&cell.status()==TerrainView.Status.PENDING?"Trwa rozpoznawanie pierwotnego terenu i struktur…":"Brak rozpoznania. Podejdź bliżej, aby wczytać teren.",0xFFAAA3B3);return;
        }
        line(cell.owner()==null?"Teren neutralny":"Gang: "+cell.ownerName(),0xFFC7B7C1);
        line("Wysokość: "+cell.minHeight()+"…"+cell.maxHeight(),0xFFAAA3B3);
        line("Biom: "+cell.biome().replace("minecraft:",""),0xFFAAA3B3);
        line(cell.source()==TerrainProfile.Source.GENERATED_SNAPSHOT?"Źródło: zapis z generowania":"Źródło: rekonstrukcja generatora",0xFFAAA3B3);
        line("Stałe cechy terenu",0xFFEFE9F3);
        if(cell.traits().isEmpty())line("Brak wyróżnionych cech.",0xFFAAA3B3);
        for(var trait:TerrainProfile.Trait.values())if(cell.traits().contains(trait))line("• "+trait.label(),0xFFD8D2DE);
        line("Potencjał do rozwinięcia",0xFFEFE9F3);
        for(var trait:TerrainProfile.Trait.values())if(cell.traits().contains(trait))line(trait.potential(),0xFFAAA3B3);
        line("Mnożniki, receptury i przejmowanie terenów będą dodawane osobno.",0xFF8E869A);
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
        var font=Minecraft.getInstance().font;int x=getX(),y=getY();Theme t=theme.get();
        g.fill(x-1,y-1,x+GRID+1,y+GRID+1,0xFF17191F);
        for(int row=0;row<7;row++)for(int col=0;col<7;col++){
            int px=x+col*CELL,py=y+row*CELL;
            var cell=view==null?null:cell(view.centerX()-3+col,view.centerZ()-3+row);
            boolean ready=cell!=null&&cell.status()==TerrainView.Status.READY;
            g.fill(px+1,py+1,px+CELL-1,py+CELL-1,0xFF000000|(ready?cell.color():cell!=null&&cell.status()==TerrainView.Status.PENDING?0x34343D:0x22232B));
            if(ready&&cell.traits().contains(TerrainProfile.Trait.VILLAGE)){
                g.fill(px+18,py+4,px+24,py+10,0xFFDFD1AD);g.fill(px+20,py+2,px+22,py+4,0xFFDFD1AD);
            }
            if(cell!=null&&cell.owner()!=null){
                var gang=ClientState.gang();int color=gang!=null&&cell.owner().equals(gang.id)?0xFF62BD86:0xFFBD6A70;
                g.fill(px+2,py+CELL-5,px+CELL-2,py+CELL-2,color);
            }
            if(cell!=null&&cell.x()==selectedX&&cell.z()==selectedZ){g.renderOutline(px,py,CELL,CELL,t.borderArgb());g.renderOutline(px+1,py+1,CELL-2,CELL-2,t.borderArgb());}
        }
        if(view!=null){
            int px=x+3*CELL+CELL/2,py=y+3*CELL+CELL/2;
            g.fill(px-3,py-1,px+4,py+2,0xFFF5EFF4);g.fill(px-1,py-3,px+2,py+4,0xFFF5EFF4);
            g.drawString(font,"Pozycja: "+view.centerX()+", "+view.centerZ(),x,y+GRID+9,0xFFAAA3B3,false);
            g.drawString(font,"Rozpoznane: "+readyCount+" / 49",x,y+GRID+23,0xFFAAA3B3,false);
        }
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
            var cell=cell(view.centerX()-3+(int)(x-getX())/CELL,view.centerZ()-3+(int)(y-getY())/CELL);
            if(cell!=null){selected.accept(cell);return true;}
        }
        return x>=getX()+DETAILS;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(x>=getX()+DETAILS&&isMouseOver(x,y)){scroll.scroll(-vertical*24);return true;}return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,getMessage());}
}
