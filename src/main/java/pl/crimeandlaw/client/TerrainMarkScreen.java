package pl.crimeandlaw.client;

import net.minecraft.client.gui.GuiGraphics;

/** Scope is chosen before color; both kinds of note may coexist on one chunk. */
final class TerrainMarkScreen extends MenuDialog {
    private final int x,z;
    TerrainMarkScreen(GangScreen parent,int x,int z){super(parent,"Zaznacz teren");this.x=x;this.z=z;panelHeight=180;}
    private Integer color(boolean shared){return CaptureClient.marks(x,z).stream().filter(m->m.get("shared").getAsBoolean()==shared).map(m->m.get("color").getAsInt()).findFirst().orElse(null);}
    private void choose(boolean shared){
        Integer color=color(shared);minecraft.setScreen(new ColorPickerScreen(parent,shared?"Oznaczenie dla gangu":"Moje oznaczenie",color==null?0xD6A34C:color,255,false,(rgb,alpha)->CaptureClient.mark(x,z,shared,rgb)));
    }
    @Override protected void build(){
        button(left+20,top+73,330,"Tylko dla mnie — wybierz kolor",b->choose(false));
        button(left+360,top+73,140,"Usuń moje",b->{CaptureClient.mark(x,z,false,null);onClose();}).active=color(false)!=null;
        button(left+20,top+109,330,"Dla mojego gangu — wybierz kolor",b->choose(true));
        button(left+360,top+109,140,"Usuń gangowe",b->{CaptureClient.mark(x,z,true,null);onClose();}).active=color(true)!=null;
    }
    @Override protected void paint(GuiGraphics g,int mx,int my,float delta){g.drawString(font,"Chunk "+x+", "+z,left+20,top+48,0xFFBBB5C3,false);}
}
