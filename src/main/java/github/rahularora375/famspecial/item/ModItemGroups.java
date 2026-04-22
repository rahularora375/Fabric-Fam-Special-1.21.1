package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.item.entries.EshEndraNaveshItems;
import github.rahularora375.famspecial.item.entries.FireSerpentItems;
import github.rahularora375.famspecial.item.entries.KnightRadiantItems;
import github.rahularora375.famspecial.item.entries.MistbornItems;
import github.rahularora375.famspecial.item.entries.NecromancerItems;
import github.rahularora375.famspecial.item.entries.OldFamItems;
import github.rahularora375.famspecial.item.entries.PacifistItems;
import github.rahularora375.famspecial.item.entries.PoseidonItems;
import github.rahularora375.famspecial.item.entries.ShurimaItems;
import github.rahularora375.famspecial.item.entries.ThorItems;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

// Registration hub for the two "Fam Special" creative tabs (Gear + Armor).
// The actual item definitions live in per-theme files under item/entries/
// (OldFamItems, MistbornItems, ...), each exposing addWeapons/addTools/addArmor
// called from the .entries(...) lambdas below. Shared helpers for building
// styled item stacks also live here so every theme file in the entries/
// subpackage can reuse them.
public class ModItemGroups {
    public static final ItemGroup GEAR_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(FamSpecial.MOD_ID, "gear"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(Items.IRON_PICKAXE))
                    .displayName(Text.translatable("itemgroup.famspecial.gear"))
                    .entries((displayContext, entries) -> {
                        RegistryWrapper<Enchantment> enchants = displayContext.lookup().getOrThrow(RegistryKeys.ENCHANTMENT);

                        // All weapons first (every theme in order), then all tools.
                        OldFamItems.addWeapons(entries, enchants);
                        MistbornItems.addWeapons(entries, enchants);
                        PacifistItems.addWeapons(entries, enchants);
                        PoseidonItems.addWeapons(entries, enchants);
                        FireSerpentItems.addWeapons(entries, enchants);
                        NecromancerItems.addWeapons(entries, enchants);
                        KnightRadiantItems.addWeapons(entries, enchants);
                        EshEndraNaveshItems.addWeapons(entries, enchants);
                        ShurimaItems.addWeapons(entries, enchants);
                        ThorItems.addWeapons(entries, enchants);

                        OldFamItems.addTools(entries, enchants);
                        MistbornItems.addTools(entries, enchants);
                        PacifistItems.addTools(entries, enchants);
                        PoseidonItems.addTools(entries, enchants);
                        FireSerpentItems.addTools(entries, enchants);
                        NecromancerItems.addTools(entries, enchants);
                        KnightRadiantItems.addTools(entries, enchants);
                        EshEndraNaveshItems.addTools(entries, enchants);
                        ShurimaItems.addTools(entries, enchants);
                        ThorItems.addTools(entries, enchants);
                    }).build());

    public static final ItemGroup ARMOR_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(FamSpecial.MOD_ID, "armor"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(Items.IRON_CHESTPLATE))
                    .displayName(Text.translatable("itemgroup.famspecial.armor"))
                    .entries((displayContext, entries) -> {
                        RegistryWrapper<Enchantment> enchants = displayContext.lookup().getOrThrow(RegistryKeys.ENCHANTMENT);

                        OldFamItems.addArmor(entries, enchants);
                        MistbornItems.addArmor(entries, enchants);
                        PacifistItems.addArmor(entries, enchants);
                        PoseidonItems.addArmor(entries, enchants);
                        FireSerpentItems.addArmor(entries, enchants);
                        NecromancerItems.addArmor(entries, enchants);
                        KnightRadiantItems.addArmor(entries, enchants);
                        EshEndraNaveshItems.addArmor(entries, enchants);
                        ShurimaItems.addArmor(entries, enchants);
                        ThorItems.addArmor(entries, enchants);
                    }).build());

    // === Shared helpers (used by entries/*Items classes) ===
    // Public because the entries/ subpackage lives outside this package.

    // Builds a lore line with a base style plus mixed inline segments.
    // Pass a String to inherit the base style, or a pre-styled Text for an accent.
    public static Text loreLine(Style base, Object... segments) {
        MutableText root = Text.empty().setStyle(base);
        for (Object seg : segments) {
            if (seg instanceof String s) {
                root.append(s);
            } else if (seg instanceof Text t) {
                root.append(t);
            } else {
                throw new IllegalArgumentException("loreLine segment must be String or Text, got " + seg.getClass());
            }
        }
        return root;
    }

    public record EnchantEntry(RegistryKey<Enchantment> key, int level) {}

    public static EnchantEntry e(RegistryKey<Enchantment> key, int level) {
        return new EnchantEntry(key, level);
    }

    public static ItemStack buildItem(ItemConvertible item, Text name, List<Text> lore,
                                      RegistryWrapper<Enchantment> enchants, EnchantEntry... defs) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));

        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        for (EnchantEntry def : defs) {
            RegistryEntry<Enchantment> entry = enchants.getOrThrow(def.key());
            builder.set(entry, def.level());
        }
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());

        return stack;
    }

    public static void registerItemGroups() {
        FamSpecial.LOGGER.info("Registering Item Groups for {}", FamSpecial.MOD_ID);
    }
}
