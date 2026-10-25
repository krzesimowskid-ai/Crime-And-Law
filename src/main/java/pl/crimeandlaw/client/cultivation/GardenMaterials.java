package pl.crimeandlaw.client.cultivation;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Original 16px palette materials for geometric models. Never samples vanilla blocks. */
public final class GardenMaterials {
    public static final ResourceLocation CLAY=id("clay"),PLASTIC=id("plastic"),BAG1=id("bag1"),BAG2=id("bag2"),BAG3=id("bag3"),LABEL1=id("label1"),LABEL2=id("label2"),LABEL3=id("label3"),STEEL=id("steel"),SOIL=id("soil"),CLOTH=id("cloth"),PACK=id("pack"),WHITE=id("white"),WATER=id("water"),UV=id("uv");
    private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath("crimeandlaw","dynamic/garden/"+s);}
    public static void reload(){
        make(PLASTIC,new int[]{0x1B2024,0x232A2E,0x2C3439,0x363F44},3);
        make(BAG1,new int[]{0x234F42,0x2E6853,0x37745E,0x497F69},3);make(BAG2,new int[]{0x234551,0x2C5D6C,0x366F7E,0x4A8190},3);make(BAG3,new int[]{0x57373A,0x734B4C,0x825653,0x936A5E},3);label(LABEL1,1);label(LABEL2,2);label(LABEL3,3);
        make(CLAY,new int[]{0x92502F,0xAB623B,0xBF774C,0xD59465},0);make(STEEL,new int[]{0x52636B,0x7B8D93,0xA7B4B5,0xD8DDCD},1);
        make(SOIL,new int[]{0x29231C,0x3A3022,0x51402A,0x6A5335},2);make(CLOTH,new int[]{0x857247,0xAA945B,0xCCB878,0xE7D695},3);
        make(PACK,new int[]{0x284C3C,0x3F7050,0x63895A,0xBACA84},3);make(WHITE,new int[]{0xFFFFFF},0);make(WATER,new int[]{0x347F9A,0x4DACC4,0x84D7DC},1);make(UV,new int[]{0x9D6FE8,0xCEAFFA,0xEFDBFF},1);
    }
    private static void label(ResourceLocation id,int capacity){
        var im=new NativeImage(32,48,false);int[] colors={0xEFF0DC,0xE4E5CF,0xD7DBC4};
        for(int y=0;y<48;y++)for(int x=0;x<32;x++){int rgb=colors[(x*7+y*11)%29==0?2:0];if(y<5||y>42)rgb=capacity==1?0x2B6650:capacity==2?0x285B72:0x824D42;im.setPixelRGBA(x,y,abgr(rgb));}
        glyphs(im,"AGRO",8,7,0x35674E);glyphs(im,"ZIEMIA",4,16,0x263F35);
        for(int y=27;y<36;y++)for(int x=5;x<27;x++)if(y>31-Math.abs(x-16)/4&&(x+y)%3!=0)im.setPixelRGBA(x,y,abgr((x+y)%2==0?0x55412C:0x796044));
        glyphs(im,Integer.toString(capacity),14,37,0x285741);register(id,im);
    }
    private static void glyphs(NativeImage im,String word,int x,int y,int color){
        String alphabet="AGROZIEM123";String[] pixels={"010101111101101","111100101101111","110101110101101","111101101101111","111001010100111","111010010010111","111100110100111","101111111101101","010110010010111","111001111100111","111001111001111"};
        for(char c:word.toCharArray()){int at=alphabet.indexOf(c);if(at>=0)for(int yy=0;yy<5;yy++)for(int xx=0;xx<3;xx++)if(pixels[at].charAt(yy*3+xx)=='1')im.setPixelRGBA(x+xx,y+yy,abgr(color));x+=4;}
    }
    private static int abgr(int rgb){return 0xFF000000|(rgb&255)<<16|(rgb&0xFF00)|(rgb>>>16&255);}
    private static void register(ResourceLocation id,NativeImage image){var t=new DynamicTexture(image){@Override public void setFilter(boolean b,boolean m){super.setFilter(false,false);}};Minecraft.getInstance().getTextureManager().register(id,t);t.setFilter(false,false);}
    private static void make(ResourceLocation id,int[] palette,int style){var image=new NativeImage(16,16,false);for(int y=0;y<16;y++)for(int x=0;x<16;x++){
        int k=switch(style){case 1->x/4;case 2->(x*13+y*7+x*y)%11;case 3->x%5==0?1:2+((x*7+y*3)%13==0?1:0);default->(y/4+(x*7+y*3)%3);};int rgb=palette[Math.floorMod(k,palette.length)];image.setPixelRGBA(x,y,0xFF000000|(rgb&255)<<16|(rgb&0xFF00)|(rgb>>>16&255));}
        var texture=new DynamicTexture(image){@Override public void setFilter(boolean blur,boolean mip){super.setFilter(false,false);}};Minecraft.getInstance().getTextureManager().register(id,texture);texture.setFilter(false,false);
    }
}
