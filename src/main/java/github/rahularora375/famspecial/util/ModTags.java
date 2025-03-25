package github.rahularora375.famspecial.util;

import github.rahularora375.famspecial.FamSpecial;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> FAM_SPECIAL_FOOD_ITEMS = createTag("fam_special_food_items");

        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(FamSpecial.MOD_ID, name));
        }
    }
}