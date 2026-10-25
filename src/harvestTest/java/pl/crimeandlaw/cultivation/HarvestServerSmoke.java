package pl.crimeandlaw.cultivation;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Two real clients plus hostile direct calls on the server thread; disposable world only. */
@EventBusSubscriber(modid="crimeandlaw")
public final class HarvestServerSmoke {
    private static final Path OUT=Path.of(System.getProperty("crimeandlaw.harvestOutput","build/harvest-smoke"));
    private static final BlockPos A=new BlockPos(0,80,0),B=new BlockPos(2,80,0);
    private static boolean ready,done;private static int stage;private static long began,at;
    private static CultivationNetwork.Cut replay;private static UUID token;private static int target;
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("crimeandlaw.harvestSmoke"))return;began=System.nanoTime();
        try{
            Files.createDirectories(OUT);var server=event.getServer();
            if(Boolean.getBoolean("crimeandlaw.harvestRestart")){
                var p=(AureliaPlant)server.overworld().getBlockEntity(B);check(p!=null&&PruningRules.count(p.leaves())==5,"Partial leaf mask survives real disk restart");
                check(server.overworld().getBlockState(A).getValue(AureliaCrop.GROWTH)==1,"Young regrowth stage survives restart");
                var file=server.getWorldPath(LevelResource.ROOT).resolve("playerdata").resolve(id("HarvestOwner")+".dat");
                var nbt=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());int leaves=0,wear=0;
                for(var item:nbt.getList("Inventory",Tag.TAG_COMPOUND)){var stack=ItemStack.parseOptional(server.registryAccess(),(CompoundTag)item);if(stack.is(CultivationContent.LEAF.get()))leaves+=stack.getCount();if(stack.is(CultivationContent.SHEARS.get()))wear=stack.getDamageValue();}
                check(leaves==7&&wear==7,"Exact inventory rewards and tool wear survive player-data restart");
                Files.writeString(OUT.resolve("restart.PASS"),"Actual disk reload retains partial crop leaves, regrowth stage, exactly seven harvested leaves and seven durability wear.\n");done=true;server.halt(false);
            }
        }catch(Throwable e){fail(event.getServer(),e);}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(!Boolean.getBoolean("crimeandlaw.harvestSmoke")||done)return;var server=event.getServer();
        try{
            check(System.nanoTime()-began<260_000_000_000L,"Harvest integration timed out stage "+stage);
            var owner=server.getPlayerList().getPlayer(id("HarvestOwner"));var rival=server.getPlayerList().getPlayer(id("HarvestRival"));if(owner==null||rival==null)return;
            var level=server.overworld();
            if(!ready){
                level.setChunkForced(0,0,true);level.getChunk(0,0);
                pl.crimeandlaw.territory.CaptureSavedData.get(server).rules=pl.crimeandlaw.territory.CaptureSavedData.get(server).rules.realTime(false);
                level.setDayTime(6000);server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,server);
                for(int x=-4;x<=6;x++)for(int z=-4;z<=5;z++){level.setBlock(new BlockPos(x,79,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);for(int y=80;y<=83;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
                plant(server,A,4);plant(server,B,4);
                for(var p:List.of(owner,rival)){p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.setNoGravity(true);p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(CultivationContent.SHEARS.get()));}
                owner.connection.teleport(.5,80,3.1,180,25);rival.connection.teleport(2.5,80,3.1,180,25);
                check(level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("crimeandlaw","aurelia_seeds")).isPresent(),"Seed recipe decoded");
                check(level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("crimeandlaw","pruning_shears")).isPresent(),"Special shears recipe decoded");
                Files.writeString(OUT.resolve("server.ready"),"Garden and two actual players ready");ready=true;return;
            }
            if(!Files.exists(OUT.resolve("owner.PASS"))||!Files.exists(OUT.resolve("rival.PASS")))return;
            switch(stage){
                case 0->{
                    check(count(owner)==6&&owner.getMainHandItem().getDamageValue()==6,"Six network snips conserve items and durability");
                    owner.connection.teleport(2.5,80,3.1,180,25);
                    var tool=owner.getMainHandItem();owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.SHEARS));check(HarvestController.problem(owner,B).contains("specjalne"),"Ordinary shears cannot bypass special tool");owner.setItemSlot(EquipmentSlot.MAINHAND,tool);
                    level.setBlock(new BlockPos(2,80,1),Blocks.STONE.defaultBlockState(),3);check(HarvestController.problem(owner,B).contains("ścianę"),"Harvest through a wall is denied");level.setBlock(new BlockPos(2,80,1),Blocks.AIR.defaultBlockState(),3);
                    check(!HarvestController.problem(owner,B.offset(2000,0,0)).isEmpty(),"Far and unloaded target denied");
                    java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock> veto=e->{if(e.getPos().equals(B))e.setCanceled(true);};
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(veto);
                    try{HarvestController.open(owner,B);check(session(owner)==null,"Protection event can veto custom-key harvest");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(veto);}
                    HarvestController.open(owner,B);var s=session(owner);token=(UUID)field(s,"token");target=(int)field(s,"target");
                    HarvestController.cut(rival,new CultivationNetwork.Cut(token,target,0));HarvestController.cut(rival,new CultivationNetwork.Cut(token,target,1));
                    HarvestController.cut(owner,new CultivationNetwork.Cut(UUID.randomUUID(),target,1));
                    HarvestController.cut(owner,new CultivationNetwork.Cut(token,(target+1)%6,1));
                    HarvestController.cut(owner,new CultivationNetwork.Cut(token,target,0));HarvestController.cut(owner,new CultivationNetwork.Cut(token,target,1));
                    check(count(owner)==6&&((AureliaPlant)level.getBlockEntity(B)).leaves()==63,"Foreign/stale nonce, wrong leaf and premature release award nothing");
                    at=level.getGameTime();stage=1;
                }
                case 1->{if(level.getGameTime()-at<25)return;HarvestController.cut(owner,new CultivationNetwork.Cut(token,target,0));at=level.getGameTime();stage=2;}
                case 2->{if(level.getGameTime()-at<34)return;HarvestController.cut(owner,new CultivationNetwork.Cut(token,target,1));check(count(owner)==6,"Overlong hold does not award a leaf");at=level.getGameTime();stage=3;}
                case 3->{if(level.getGameTime()-at<22)return;HarvestController.cut(owner,new CultivationNetwork.Cut(token,target,0));at=level.getGameTime();stage=4;}
                case 4->{
                    if(level.getGameTime()-at<20)return;
                    replay=new CultivationNetwork.Cut(token,target,1);HarvestController.cut(owner,replay);HarvestController.cut(owner,replay);
                    check(count(owner)==7&&owner.getMainHandItem().getDamageValue()==7,"Replayed final packet cannot double the reward or tool wear");
                    check(PruningRules.count(((AureliaPlant)level.getBlockEntity(B)).leaves())==5,"Only one exact leaf disappears");
                    HarvestController.cut(owner,new CultivationNetwork.Cut(token,-1,2));HarvestController.cut(owner,replay);check(count(owner)==7,"Closed-session packet cannot reward again");
                    var original=(AureliaPlant)level.getBlockEntity(B);var nbt=original.saveWithoutMetadata(server.registryAccess());var restored=new AureliaPlant(B,original.getBlockState());restored.loadAdditional(nbt,server.registryAccess());check(original.identity().equals(restored.identity())&&original.leaves()==restored.leaves(),"NBT retains exact plant identity and partial mask");
                    // A full creative inventory must drop the reward rather than discard it.
                    rival.setGameMode(GameType.CREATIVE);for(int i=0;i<rival.getInventory().items.size();i++)rival.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
                    var give=HarvestController.class.getDeclaredMethod("give",ServerPlayer.class,ItemStack.class);give.setAccessible(true);var reward=new ItemStack(CultivationContent.LEAF.get());give.invoke(null,rival,reward);
                    check(rival.getInventory().countItem(CultivationContent.LEAF.get())==0,"Full creative inventory is not mutated by an impossible insertion");
                    var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,rival.getBoundingBox().inflate(3),e->e.getItem().is(CultivationContent.LEAF.get()));check(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Overflow reward survives as exactly one ground item");drops.forEach(net.minecraft.world.entity.Entity::discard);
                    Files.writeString(OUT.resolve("security.PASS"),"Server-thread checks: recipes, special-tool gate, wall/reach checks, foreign/stale token, wrong leaf, early/late timing, replay, closure, partial NBT, full creative inventory conservation.\n");
                    server.saveEverything(true,true,true);Files.writeString(OUT.resolve("server.PASS"),"Actual two-client harvest, reservation isolation, six GUI cuts and hostile packet checks passed.\n");done=true;server.halt(false);
                }
            }
        }catch(Throwable e){fail(server,e);}
    }
    private static void plant(MinecraftServer server,BlockPos pos,int age){server.overworld().setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);server.overworld().setBlock(pos,CultivationContent.AURELIA.get().getStateForAge(age),3);}
    private static UUID id(String name){return UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static int count(ServerPlayer player){return player.getInventory().countItem(CultivationContent.LEAF.get());}
    private static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static Object session(ServerPlayer p)throws Exception{var f=HarvestController.class.getDeclaredField("SESSIONS");f.setAccessible(true);return ((Map<?,?>)f.get(null)).get(p);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void fail(MinecraftServer server,Throwable e){e.printStackTrace();done=true;try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("server.FAIL"),e.toString()+" stage="+stage);}catch(Exception ignored){}server.halt(false);}
}
