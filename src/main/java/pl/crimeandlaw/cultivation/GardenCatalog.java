package pl.crimeandlaw.cultivation;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
public final class GardenCatalog {
    public record Offer(Supplier<? extends Item> item,int price,String detail){public ItemStack stack(){return new ItemStack(item.get());}}
    public static final List<Offer> OFFERS=List.of(
        new Offer(CultivationContent.SEEDS,60,"Jedna roślina • jeden cykl"),new Offer(CultivationContent.SEED_PACKET,270,"Woreczek 5 luźnych nasion"),new Offer(CultivationContent.SEEDLING,90,"Początkowy wzrost 20%"),
        new Offer(CultivationContent.SOIL_SMALL,20,"1 pełne napełnienie"),new Offer(CultivationContent.SOIL_MEDIUM,38,"2 pełne napełnienia"),new Offer(CultivationContent.SOIL_LARGE,54,"3 pełne napełnienia"),
        new Offer(CultivationContent.POT_ITEM,100,"Samodzielna lub w stelażu"),new Offer(CultivationContent.FRAME_ITEM,250,"Stalowa rama • 2 bloki"),new Offer(CultivationContent.UV_LAMP,400,"Światło dla jednej doniczki"),
        new Offer(CultivationContent.WATERING_CAN,100,"400 wody • sprzedawana pusta"),new Offer(CultivationContent.SHEARS,80,"Precyzyjny zbiór liści"));
    private GardenCatalog(){}
}
