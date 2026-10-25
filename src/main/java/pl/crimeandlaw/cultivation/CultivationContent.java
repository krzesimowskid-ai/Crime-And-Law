package pl.crimeandlaw.cultivation;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
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
    public static final DeferredItem<Item> SEEDS=ITEMS.register("aurelia_seeds",()->new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAF=ITEMS.register("aurelia_leaf",()->new Item(new Item.Properties()));
    public static final DeferredItem<ShearsItem> SHEARS=ITEMS.register("pruning_shears",()->new ShearsItem(new Item.Properties().durability(192)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AureliaPlant>> PLANT=ENTITIES.register("aurelia",()->BlockEntityType.Builder.of(AureliaPlant::new,AURELIA.get()).build(null));
    public static final DeferredBlock<PlanterBlock> POT=BLOCKS.register("planter",()->new PlanterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.2f).noOcclusion().sound(SoundType.DECORATED_POT).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<GrowFrameBlock> FRAME=BLOCKS.register("grow_frame",()->new GrowFrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2).noOcclusion().sound(SoundType.METAL).pushReaction(PushReaction.BLOCK).lightLevel(s->s.getValue(GrowFrameBlock.LAMP)&&s.getValue(GrowFrameBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER?10:0)));
    public static final DeferredItem<BlockItem> POT_ITEM=ITEMS.register("planter",()->new BlockItem(POT.get(),new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BlockItem> FRAME_ITEM=ITEMS.register("grow_frame",()->new BlockItem(FRAME.get(),new Item.Properties().stacksTo(16)));
    public static final DeferredItem<GardenSupplyItem> SOIL_SMALL=supply("soil_bag_small",GardenSupplyItem.Kind.SOIL,1,16),SOIL_MEDIUM=supply("soil_bag_medium",GardenSupplyItem.Kind.SOIL,2,1),SOIL_LARGE=supply("soil_bag_large",GardenSupplyItem.Kind.SOIL,3,1);
    public static final DeferredItem<GardenSupplyItem> SEED_PACKET=supply("aurelia_seed_packet",GardenSupplyItem.Kind.PACK,5,64),SEEDLING=supply("aurelia_seedling",GardenSupplyItem.Kind.SEEDLING,1,16),WATERING_CAN=supply("watering_can",GardenSupplyItem.Kind.WATER,400,1),UV_LAMP=supply("uv_lamp",GardenSupplyItem.Kind.LAMP,1,16);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<Planter>> PLANTER=ENTITIES.register("planter",()->BlockEntityType.Builder.of(Planter::new,POT.get(),FRAME.get()).build(null));
    public static final DeferredRegister<EntityType<?>> MOBS=DeferredRegister.create(Registries.ENTITY_TYPE,CrimeAndLaw.ID);
    public static final DeferredHolder<EntityType<?>,EntityType<GardenMerchant>> MERCHANT=MOBS.register("garden_merchant",()->EntityType.Builder.of(GardenMerchant::new,MobCategory.MISC).sized(.6f,1.95f).clientTrackingRange(8).build("crimeandlaw:garden_merchant"));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> MERCHANT_EGG=ITEMS.register("garden_merchant_spawn_egg",()->new net.neoforged.neoforge.common.DeferredSpawnEggItem(MERCHANT,0x496A48,0xC9AE7B,new Item.Properties()));
    private static DeferredItem<GardenSupplyItem> supply(String name,GardenSupplyItem.Kind kind,int capacity,int size){return ITEMS.register(name,()->new GardenSupplyItem(kind,capacity,new Item.Properties().stacksTo(size)));}
    private CultivationContent(){}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MOBS.register(bus);bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->e.put(MERCHANT.get(),Villager.createAttributes().build()));bus.addListener(PotNetwork::register);bus.addListener(ShopNetwork::register);bus.addListener(CultivationNetwork::register);bus.addListener(CultivationContent::creative);}
    private static void creative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event){
        if(event.getTabKey()==CreativeModeTabs.NATURAL_BLOCKS){event.accept(SEEDS);event.accept(LEAF);event.accept(SEED_PACKET);event.accept(SEEDLING);event.accept(SOIL_SMALL);event.accept(SOIL_MEDIUM);event.accept(SOIL_LARGE);event.accept(POT_ITEM);}
        if(event.getTabKey()==CreativeModeTabs.TOOLS_AND_UTILITIES){event.accept(SHEARS);event.accept(WATERING_CAN);event.accept(FRAME_ITEM);event.accept(UV_LAMP);}
        if(event.getTabKey()==CreativeModeTabs.SPAWN_EGGS)event.accept(MERCHANT_EGG);
    }
}
