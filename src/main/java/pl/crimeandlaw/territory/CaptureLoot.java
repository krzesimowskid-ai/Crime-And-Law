package pl.crimeandlaw.territory;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

/** Persistent gang depot until physical gang warehouses are introduced. */
public final class CaptureLoot extends SavedData {
    public static final String EVENT="crimeandlaw_capture_drop";
    public static final int MAX_STACKS=1024;
    private final Map<UUID,List<ItemStack>> stored=new LinkedHashMap<>();
    private CaptureLoot(){}
    public static CaptureLoot get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(CaptureLoot::new,CaptureLoot::load),"crimeandlaw_capture_loot");}
    static CaptureLoot load(CompoundTag tag,HolderLookup.Provider provider){
        var result=new CaptureLoot();var rows=tag.getList("gangs",Tag.TAG_COMPOUND);
        for(var entry:rows){var row=(CompoundTag)entry;var items=row.getList("items",Tag.TAG_COMPOUND);List<ItemStack> stacks=new ArrayList<>();
            for(var item:items){var stack=ItemStack.parseOptional(provider,(CompoundTag)item);if(!stack.isEmpty())stacks.add(stack);}
            if(stacks.size()>MAX_STACKS)throw new IllegalArgumentException("Depozyt łupów przekracza limit.");result.stored.put(row.getUUID("gang"),stacks);
        }return result;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        ListTag rows=new ListTag();stored.forEach((gang,stacks)->{
            var row=new CompoundTag();row.putUUID("gang",gang);var items=new ListTag();for(var stack:stacks)items.add(stack.saveOptional(provider));row.put("items",items);rows.add(row);
        });tag.put("gangs",rows);return tag;
    }
    public int count(UUID gang){return stored.getOrDefault(gang,List.of()).stream().mapToInt(ItemStack::getCount).sum();}
    public int collect(ServerLevel level,TerrainKey key,UUID event,UUID winner){
        var items=stored.computeIfAbsent(winner,id->new ArrayList<>());int count=0;
        var area=new AABB(key.x()*16.0,level.getMinBuildHeight()-64,key.z()*16.0,key.x()*16.0+16,level.getMaxBuildHeight()+64,key.z()*16.0+16);
        for(var drop:level.getEntitiesOfClass(ItemEntity.class,area,e->e.getPersistentData().hasUUID(EVENT)&&event.equals(e.getPersistentData().getUUID(EVENT)))){
            if(drop.getItem().isEmpty()||drop.isRemoved()||items.size()>=MAX_STACKS)continue;
            items.add(drop.getItem().copy());count+=drop.getItem().getCount();setDirty();drop.discard();
        }
        if(items.isEmpty())stored.remove(winner);return count;
    }
    public int claim(ServerPlayer player,UUID gang){
        var items=stored.get(gang);if(items==null)return 0;int claimed=0;
        for(var it=items.iterator();it.hasNext();){
            var stack=it.next();int before=stack.getCount();player.getInventory().add(stack);claimed+=before-stack.getCount();if(stack.isEmpty())it.remove();
        }
        if(items.isEmpty())stored.remove(gang);if(claimed>0){setDirty();player.inventoryMenu.broadcastChanges();}return claimed;
    }
}
