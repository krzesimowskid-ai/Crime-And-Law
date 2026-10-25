package pl.crimeandlaw.cultivation;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import pl.crimeandlaw.CrimeAndLaw;

public final class CultivationContent {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(CrimeAndLaw.ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(CrimeAndLaw.ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,CrimeAndLaw.ID);
    public static final DeferredBlock<AureliaCrop> AURELIA=BLOCKS.register("aurelia",()->new AureliaCrop(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().randomTicks().sound(net.minecraft.world.level.block.SoundType.CROP)));
    public static final DeferredItem<ItemNameBlockItem> SEEDS=ITEMS.register("aurelia_seeds",()->new ItemNameBlockItem(AURELIA.get(),new Item.Properties()));
    public static final DeferredItem<Item> LEAF=ITEMS.register("aurelia_leaf",()->new Item(new Item.Properties()));
    public static final DeferredItem<ShearsItem> SHEARS=ITEMS.register("pruning_shears",()->new ShearsItem(new Item.Properties().durability(192)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AureliaPlant>> PLANT=ENTITIES.register("aurelia",()->BlockEntityType.Builder.of(AureliaPlant::new,AURELIA.get()).build(null));
    private CultivationContent(){}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);bus.addListener(CultivationNetwork::register);bus.addListener(CultivationContent::creative);}
    private static void creative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event){
        if(event.getTabKey()==CreativeModeTabs.NATURAL_BLOCKS){event.accept(SEEDS);event.accept(LEAF);}
        if(event.getTabKey()==CreativeModeTabs.TOOLS_AND_UTILITIES)event.accept(SHEARS);
    }
}
