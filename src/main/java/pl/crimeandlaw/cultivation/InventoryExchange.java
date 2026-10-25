package pl.crimeandlaw.cultivation;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Copy/plan/commit, including offhand, with no Inventory.add creative-overflow shortcut. */
public final class InventoryExchange {
    public static boolean sourceSlot(int slot){return slot>=0&&slot<36||slot==40;}
    public static final class Prepared {
        private final ServerPlayer player; private final List<ItemStack> before,after;
        private Prepared(ServerPlayer p,List<ItemStack> before,List<ItemStack> after){player=p;this.before=before;this.after=after;}
        public boolean unchanged(){for(int i=0;i<before.size();i++)if(!ItemStack.matches(before.get(i),player.getInventory().getItem(i)))return false;return true;}
        public boolean commit(){if(!unchanged())return false;for(int i=0;i<after.size();i++)player.getInventory().setItem(i,after.get(i));player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();return true;}
    }
    private InventoryExchange(){}
    public static Prepared prepare(ServerPlayer p,int slot,ItemStack expected,ItemStack replacement,ItemStack reward){
        var before=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)before.add(p.getInventory().getItem(i).copy());
        var after=new ArrayList<ItemStack>();for(var s:before)after.add(s.copy());
        if(slot>=0){if(!sourceSlot(slot)||!ItemStack.matches(before.get(slot),expected))return null;after.set(slot,replacement.copy());}
        var rest=reward.copy();
        for(int i=0;i<after.size()&&!rest.isEmpty();i++)if(i<36||i==40){var s=after.get(i);if(!s.isEmpty()&&ItemStack.isSameItemSameComponents(s,rest)){int n=Math.min(rest.getCount(),Math.min(s.getMaxStackSize(),p.getInventory().getMaxStackSize())-s.getCount());if(n>0){s.grow(n);rest.shrink(n);}}}
        for(int i=0;i<36&&!rest.isEmpty();i++)if(after.get(i).isEmpty()){int n=Math.min(rest.getCount(),Math.min(rest.getMaxStackSize(),p.getInventory().getMaxStackSize()));after.set(i,rest.copyWithCount(n));rest.shrink(n);}
        return rest.isEmpty()?new Prepared(p,before,after):null;
    }
    public static void giveOrDrop(ServerPlayer p,ItemStack stack){var plan=prepare(p,-1,ItemStack.EMPTY,ItemStack.EMPTY,stack);if(plan==null||!plan.commit())p.drop(stack.copy(),false);}
}
