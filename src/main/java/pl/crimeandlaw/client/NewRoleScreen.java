package pl.crimeandlaw.client;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import pl.crimeandlaw.gang.GangDirectory.Permission;

/** A new rank stays a local draft until Save; closing never changes the gang. */
final class NewRoleScreen extends MenuDialog {
    private final PermissionList.State catalogState=new PermissionList.State();
    private final BiConsumer<String,Set<Permission>> accepted;
    private final EnumSet<Permission> permissions=EnumSet.noneOf(Permission.class);
    private String draftName="";
    private EditBox name;
    private FlatButton save;
    NewRoleScreen(GangScreen parent,BiConsumer<String,Set<Permission>> accepted){
        super(parent,"Nowa ranga");this.accepted=accepted;panelWidth=460;panelHeight=356;
    }
    @Override protected void build(){
        name=addRenderableWidget(new EditBox(font,left+22,top+61,panelWidth-44,18,Component.literal("Nazwa rangi")));
        name.setMaxLength(24);name.setValue(draftName);name.setResponder(value->{draftName=value;updateSave();});
        var catalog=new PermissionList(left+22,top+140,panelWidth-58,162,catalogState,permissions,()->true,this::theme);
        button(left+22,top+110,150,catalogState.group.label+" ▸",b->{catalogState.nextGroup();b.setMessage(Component.literal(catalogState.group.label+" ▸"));catalog.filter();});
        var search=addRenderableWidget(new EditBox(font,left+180,top+110,panelWidth-202,20,Component.literal("Szukaj uprawnienia")));
        search.setMaxLength(64);search.setValue(catalogState.query);search.setHint(Component.literal("Szukaj uprawnienia…"));
        search.setResponder(v->{catalogState.query=v;catalogState.scroll.reset();catalog.filter();});addRenderableWidget(catalog);
        catalog.scrollbar(addRenderableWidget(new RegisterScrollBar(left+panelWidth-32,top+140,162,catalogState.scroll,this::theme,"Przewijanie uprawnień")));
        button(left+18,top+panelHeight-35,90,"Anuluj",b->onClose());
        save=button(left+panelWidth-138,top+panelHeight-35,120,"Zapisz rangę",b->{
            accepted.accept(draftName.strip(),EnumSet.copyOf(permissions));minecraft.setScreen(parent);
        });
        updateSave();setInitialFocus(name);
    }
    private String permissionText(Permission permission,String text){return (permissions.contains(permission)?"[x] ":"[ ] ")+text;}
    private void updateSave(){if(save!=null)save.active=draftName.strip().length()>=2;}
    @Override protected void paint(GuiGraphics g,int x,int y,float delta){
        g.drawString(font,"Nazwa rangi",left+22,top+44,0xFFAAAAB7,false);
        g.drawString(font,"Uprawnienia",left+22,top+93,0xFFAAAAB7,false);
    }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers){
        if((key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER||key==org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)
                &&getFocused()==name&&save!=null&&save.active){save.onPress();return true;}
        return super.keyPressed(key,scanCode,modifiers);
    }
}
