package pl.crimeandlaw.mixin.client;

import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Font.class)
public interface FontAccess {
    @Accessor("fonts") Function<ResourceLocation,FontSet> crimeandlaw$fonts();
}
