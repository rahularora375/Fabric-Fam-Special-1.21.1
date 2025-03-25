package github.rahularora375.famspecial.datagen;

import github.rahularora375.famspecial.item.ModItems;
import github.rahularora375.famspecial.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(ModTags.Items.FAM_SPECIAL_FOOD_ITEMS)
                .add(ModItems.PUNJABI_LASSI)
                .add(ModItems.RADHE_RADHE_GURU_JI);
    }
}