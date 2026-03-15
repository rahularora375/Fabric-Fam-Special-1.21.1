package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModItemGroups {
    public static final ItemGroup FAM_SPECIAL_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(FamSpecial.MOD_ID, "fam_special"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(Items.DIAMOND_SWORD))
                    .displayName(Text.translatable("itemgroup.famspecial.fam_special_items"))
                    .entries((displayContext, entries) -> {
                        RegistryWrapper<Enchantment> enchants = displayContext.lookup().getOrThrow(RegistryKeys.ENCHANTMENT);

                        // === Weapons ===
                        entries.add(buildItem(Items.DIAMOND_SWORD,
                                Text.literal("DEMACIAAAAAAAA !!!"),
                                List.of(Text.literal("By this blade, Demacia's enemies shall fall")),
                                enchants,
                                e(Enchantments.SHARPNESS, 7), e(Enchantments.UNBREAKING, 3), e(Enchantments.LOOTING, 5)));

                        entries.add(buildItem(Items.DIAMOND_SWORD,
                                Text.literal("Blade of the Ruined King").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)),
                                List.of(Text.literal("Wielded by a king lost in agony").setStyle(Style.EMPTY.withColor(Formatting.WHITE)),
                                        Text.literal("It cuts deeper with every heartbreak").setStyle(Style.EMPTY.withColor(Formatting.WHITE))),
                                enchants,
                                e(Enchantments.SMITE, 7), e(Enchantments.LOOTING, 5)));

                        entries.add(buildItem(Items.DIAMOND_AXE,
                                Text.literal("Leviathan Axe"),
                                List.of(Text.literal("Forged in the icy depths of J\u00f6tunheim,"),
                                        Text.literal("this axe carries the fury of a fallen god")),
                                enchants,
                                e(Enchantments.EFFICIENCY, 7), e(Enchantments.SHARPNESS, 7)));

                        entries.add(buildItem(Items.BOW,
                                Text.literal("Ashe's Bow").setStyle(Style.EMPTY.withColor(Formatting.AQUA)),
                                List.of(Text.literal("Ashe ka bow hai bhai")),
                                enchants,
                                e(Enchantments.POWER, 7), e(Enchantments.MENDING, 1), e(Enchantments.INFINITY, 1)));

                        // === Tools ===
                        entries.add(buildItem(Items.DIAMOND_HOE,
                                Text.literal("Hoe Hoe Hoe"),
                                List.of(Text.literal("Bihari Santa ka asli jugaadu hathiyaar"),
                                        Text.literal("Kheton mein bhi chale aur dilon pe bhi vaar!")),
                                enchants,
                                e(Enchantments.EFFICIENCY, 6), e(Enchantments.UNBREAKING, 4), e(Enchantments.SILK_TOUCH, 1)));

                        entries.add(buildItem(Items.DIAMOND_PICKAXE,
                                Text.literal("Hextech Drill"),
                                List.of(Text.literal("Once a tool of Piltover's hextech miners,"),
                                        Text.literal("now corrupted by Zaun's chemtech")),
                                enchants,
                                e(Enchantments.EFFICIENCY, 7), e(Enchantments.UNBREAKING, 3)));

                        entries.add(buildItem(Items.DIAMOND_SHOVEL,
                                Text.literal("Raftwreck Digger"),
                                List.of(Text.literal("Once used by Raft survivors to unearth"),
                                        Text.literal("lost treasures, it still carries"),
                                        Text.literal("the scent of salt and despair")),
                                enchants,
                                e(Enchantments.EFFICIENCY, 7), e(Enchantments.UNBREAKING, 5)));

                        // === Armor ===
                        entries.add(buildItem(Items.DIAMOND_CHESTPLATE,
                                Text.literal("Brimstone's Surgeplate"),
                                List.of(Text.literal("Open up the sky!")),
                                enchants,
                                e(Enchantments.PROTECTION, 4), e(Enchantments.FIRE_PROTECTION, 4)));

                        entries.add(buildItem(Items.DIAMOND_CHESTPLATE,
                                Text.literal("Stormguard"),
                                List.of(Text.literal("I am the stormguard"),
                                        Text.literal("I am your misfortune and your despair"),
                                        Text.literal("Halcyon shimmers in my veins")),
                                enchants,
                                e(Enchantments.PROTECTION, 6), e(Enchantments.UNBREAKING, 5), e(Enchantments.THORNS, 5)));

                        entries.add(buildItem(Items.DIAMOND_HELMET,
                                Text.literal("Vox's Resonance Crown"),
                                List.of(Text.literal("Harmonic shards, resonating with"),
                                        Text.literal("echoes of forgotten songs")),
                                enchants,
                                e(Enchantments.PROTECTION, 6), e(Enchantments.AQUA_AFFINITY, 1), e(Enchantments.RESPIRATION, 4)));

                        entries.add(buildItem(Items.DIAMOND_BOOTS,
                                Text.literal("Sundar Ki Chutiya Chappal").setStyle(Style.EMPTY.withColor(Formatting.AQUA)),
                                List.of(Text.literal("The only shoes that run ").setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
                                        Text.literal("faster than their owner").setStyle(Style.EMPTY.withColor(Formatting.YELLOW))),
                                enchants,
                                e(Enchantments.PROTECTION, 6), e(Enchantments.SOUL_SPEED, 8),
                                e(Enchantments.DEPTH_STRIDER, 4), e(Enchantments.VANISHING_CURSE, 1)));

                        entries.add(buildItem(Items.DIAMOND_BOOTS,
                                Text.literal("Lavanya's Shackles"),
                                List.of(Text.literal("When she walks, gravity kneels")),
                                enchants,
                                e(Enchantments.PROTECTION, 6), e(Enchantments.SWIFT_SNEAK, 4)));

                        entries.add(buildItem(Items.DIAMOND_LEGGINGS,
                                Text.literal("Clutch God Leggings"),
                                List.of(Text.literal("Crazyy..."),
                                        Text.literal("Kya Aunty hai Jesus"),
                                        Text.literal("Ye hi to time hai nashe karne ka")),
                                enchants,
                                e(Enchantments.PROTECTION, 6)));
                    }).build());

    private record EnchantEntry(RegistryKey<Enchantment> key, int level) {}

    private static EnchantEntry e(RegistryKey<Enchantment> key, int level) {
        return new EnchantEntry(key, level);
    }

    private static ItemStack buildItem(ItemConvertible item, Text name, List<Text> lore,
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
