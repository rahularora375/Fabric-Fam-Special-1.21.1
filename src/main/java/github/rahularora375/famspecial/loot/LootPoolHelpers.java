package github.rahularora375.famspecial.loot;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemConvertible;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.function.SetEnchantmentsLootFunction;
import net.minecraft.loot.function.SetLoreLootFunction;
import net.minecraft.loot.function.SetNameLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

final class LootPoolHelpers {
    private LootPoolHelpers() {}

    record EnchantDef(RegistryKey<Enchantment> key, float min, float max) {}

    static EnchantDef ench(RegistryKey<Enchantment> key, float min, float max) {
        return new EnchantDef(key, min, max);
    }

    static LeafEntry.Builder<?> namedItem(
            ItemConvertible item, int weight, Text name, Text[] lore,
            RegistryWrapper<Enchantment> enchants, EnchantDef... defs) {

        LeafEntry.Builder<?> entry = ItemEntry.builder(item).weight(weight);

        entry.apply(SetNameLootFunction.builder(name, SetNameLootFunction.Target.CUSTOM_NAME));

        SetLoreLootFunction.Builder loreBuilder = SetLoreLootFunction.builder();
        for (Text line : lore) {
            loreBuilder.lore(line);
        }
        entry.apply(loreBuilder);

        SetEnchantmentsLootFunction.Builder enchantBuilder = new SetEnchantmentsLootFunction.Builder();
        for (EnchantDef def : defs) {
            RegistryEntry<Enchantment> enchEntry = enchants.getOrThrow(def.key());
            LootNumberProvider provider = def.min() == def.max()
                    ? ConstantLootNumberProvider.create(def.min())
                    : UniformLootNumberProvider.create(def.min(), def.max());
            enchantBuilder.enchantment(enchEntry, provider);
        }
        entry.apply(enchantBuilder);

        return entry;
    }
}
