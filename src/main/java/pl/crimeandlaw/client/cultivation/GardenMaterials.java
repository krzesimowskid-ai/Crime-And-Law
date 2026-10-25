package pl.crimeandlaw.client.cultivation;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Original 16px palette materials for geometric models. Never samples vanilla blocks. */
public final class GardenMaterials {
    public static final ResourceLocation CLAY=id("clay"),STEEL=id("steel"),SOIL=id("soil"),CLOTH=id("cloth"),PACK=id("pack"),WHITE=id("white"),WATER=id("water"),UV=id("uv");
    private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath("crimeandlaw","dynamic/garden/"+s);}
    public static void reload(){
        make(CLAY,new int[]{0x92502F,0xAB623B,0xBF774C,0xD59465},0);make(STEEL,new int[]{0x52636B,0x7B8D93,0xA7B4B5,0xD8DDCD},1);
        make(SOIL,new int[]{0x29231C,0x3A3022,0x51402A,0x6A5335},2);make(CLOTH,new int[]{0x857247,0xAA945B,0xCCB878,0xE7D695},3);
        make(PACK,new int[]{0x284C3C,0x3F7050,0x63895A,0xBACA84},3);make(WHITE,new int[]{0xFFFFFF},0);make(WATER,new int[]{0x347F9A,0x4DACC4,0x84D7DC},1);make(UV,new int[]{0x9D6FE8,0xCEAFFA,0xEFDBFF},1);
    }
    private static void make(ResourceLocation id,int[] palette,int style){var image=new NativeImage(16,16,false);for(int y=0;y<16;y++)for(int x=0;x<16;x++){
        int k=switch(style){case 1->x/4;case 2->(x*13+y*7+x*y)%11;case 3->x%5==0?1:2+((x*7+y*3)%13==0?1:0);default->(y/4+(x*7+y*3)%3);};int rgb=palette[Math.floorMod(k,palette.length)];image.setPixelRGBA(x,y,0xFF000000|(rgb&255)<<16|(rgb&0xFF00)|(rgb>>>16&255));}
        var texture=new DynamicTexture(image){@Override public void setFilter(boolean blur,boolean mip){super.setFilter(false,false);}};Minecraft.getInstance().getTextureManager().register(id,texture);texture.setFilter(false,false);
    }
}
