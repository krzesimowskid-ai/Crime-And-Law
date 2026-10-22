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
    static final String[] RIGHTS={"Zapraszanie / zgłoszenia","Usuwanie członków","Zarządzanie rangami","Wygląd i logo gangu"};
    private final BiConsumer<String,Set<Permission>> accepted;
    private final EnumSet<Permission> permissions=EnumSet.noneOf(Permission.class);
    private String draftName="";
    private EditBox name;
    private FlatButton save;
    NewRoleScreen(GangScreen parent,BiConsumer<String,Set<Permission>> accepted){
        super(parent,"Nowa ranga");this.accepted=accepted;panelWidth=420;panelHeight=282;
    }
    @Override protected void build(){
        name=addRenderableWidget(new EditBox(font,left+22,top+61,panelWidth-44,18,Component.literal("Nazwa rangi")));
        name.setMaxLength(24);name.setValue(draftName);name.setResponder(value->{draftName=value;updateSave();});
        int row=0;
        for(Permission permission:Permission.values()){
            String text=RIGHTS[row];
            button(left+22,top+110+row++*24,panelWidth-44,permissionText(permission,text),b->{
                if(!permissions.remove(permission))permissions.add(permission);
                b.setMessage(Component.literal(permissionText(permission,text)));
            });
        }
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
