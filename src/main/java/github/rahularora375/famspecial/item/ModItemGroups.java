package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup FAM_SPECIAL_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(FamSpecial.MOD_ID, "fam_special"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.PUNJABI_LASSI))
                    .displayName(Text.translatable("itemgroup.famspecial.fam_special_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.PUNJABI_LASSI);
                        entries.add(ModItems.RADHE_RADHE_GURU_JI);

                    }).build());


    public static void registerItemGroups() {
        FamSpecial.LOGGER.info("Registering Item Groups for " + FamSpecial.MOD_ID);
    }
}