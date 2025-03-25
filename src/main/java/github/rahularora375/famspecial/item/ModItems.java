package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModItems {
    public static final Item PUNJABI_LASSI = registerItem("punjabi_lassi", new DrinkableItem(new Item.Settings().food(ModFoodComponents.PUNJABI_LASSI)) {
        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            if(Screen.hasShiftDown()) {
                tooltip.add(Text.translatable("tooltip.famspecial.punjabi_lassi.shift_down"));
            } else {
                tooltip.add(Text.translatable("tooltip.famspecial.punjabi_lassi"));
            }

            super.appendTooltip(stack, context, tooltip, type);
        }
    });

    public static final Item RADHE_RADHE_GURU_JI = registerItem("radhe_radhe_guru_ji", new DrinkableItem(new Item.Settings().food(ModFoodComponents.RADHE_RADHE_GURU_JI)) {
        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            if(Screen.hasShiftDown()) {
                tooltip.add(Text.translatable("tooltip.famspecial.radhe_radhe_guru_ji.shift_down"));
            } else {
                tooltip.add(Text.translatable("tooltip.famspecial.radhe_radhe_guru_ji"));
            }

            super.appendTooltip(stack, context, tooltip, type);
        }
    });


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(FamSpecial.MOD_ID, name), item);
    }

    public static void registerModItems() {
        FamSpecial.LOGGER.info("Registering Mod Items for " + FamSpecial.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(fabricItemGroupEntries -> {
            fabricItemGroupEntries.add(PUNJABI_LASSI);
            fabricItemGroupEntries.add(RADHE_RADHE_GURU_JI);
        });
    }
}
