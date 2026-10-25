package pl.crimeandlaw.cultivation;

import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** The detailed image is rendered directly; it does not lower the vanilla atlas mip level. */
public final class AureliaLeafItem extends Item {
    public AureliaLeafItem(Properties properties){super(properties);}
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){
        consumer.accept(new IClientItemExtensions(){
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new pl.crimeandlaw.client.cultivation.AureliaLeafItemRenderer();return renderer;
            }
        });
    }
}
