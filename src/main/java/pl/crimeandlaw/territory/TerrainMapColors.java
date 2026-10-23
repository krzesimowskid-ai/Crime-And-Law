package pl.crimeandlaw.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.MapColor;

/** The same surface palette for live chunks and immutable saved-chunk snapshots. */
final class TerrainMapColors {
    @FunctionalInterface interface Heights {int at(int x,int z,int fallback);}
    private TerrainMapColors(){}
    static byte sample(BlockGetter blocks,int x,int y,int z,int min,Heights heights){
        if(y<min)return 0;
        var pos=new BlockPos.MutableBlockPos(x,y,z);var state=blocks.getBlockState(pos);var color=state.getMapColor(blocks,pos);
        while(y>min&&color==MapColor.NONE){state=blocks.getBlockState(pos.setY(--y));color=state.getMapColor(blocks,pos);}
        if(color==MapColor.NONE)return 0;
        MapColor.Brightness brightness;
        if(color==MapColor.WATER){
            int depth=0;while(depth<12&&y-depth>min&&!blocks.getBlockState(pos.setY(y-depth)).getFluidState().isEmpty())depth++;
            brightness=depth<3?MapColor.Brightness.HIGH:depth>7?MapColor.Brightness.LOW:MapColor.Brightness.NORMAL;
        }else{
            int north=heights.at(x,z-1,y),west=heights.at(x-1,z,y);
            double slope=(y-north)*0.65+(y-west)*0.35;
            brightness=slope>0.6?MapColor.Brightness.HIGH:slope< -0.6?MapColor.Brightness.LOW:MapColor.Brightness.NORMAL;
        }
        return color.getPackedId(brightness);
    }
}
