package github.rahularora375.famspecial.item.entries;

import github.rahularora375.famspecial.item.ModItemGroups;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class OldFamItems {
    private OldFamItems() {}

    public static void addWeapons(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(ModItemGroups.buildItem(Items.DIAMOND_SWORD,
                Text.literal("DEMACIAAAAAAAA !!!"),
                List.of(Text.literal("By this blade, Demacia's enemies shall fall")),
                enchants,
                ModItemGroups.e(Enchantments.SHARPNESS, 7),
                ModItemGroups.e(Enchantments.UNBREAKING, 3),
                ModItemGroups.e(Enchantments.LOOTING, 5)));

        entries.add(ModItemGroups.buildItem(Items.DIAMOND_SWORD,
                Text.literal("Blade of the Ruined King").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)),
                List.of(Text.literal("Wielded by a king lost in agony").setStyle(Style.EMPTY.withColor(Formatting.WHITE)),
                        Text.literal("It cuts deeper with every heartbreak").setStyle(Style.EMPTY.withColor(Formatting.WHITE))),
                enchants,
                ModItemGroups.e(Enchantments.SMITE, 7),
                ModItemGroups.e(Enchantments.LOOTING, 5)));

        entries.add(ModItemGroups.buildItem(Items.DIAMOND_AXE,
                Text.literal("Leviathan Axe"),
                List.of(Text.literal("Forged in the icy depths of Jötunheim,"),
                        Text.literal("this axe carries the fury of a fallen god")),
                enchants,
                ModItemGroups.e(Enchantments.EFFICIENCY, 7),
                ModItemGroups.e(Enchantments.SHARPNESS, 7)));

        entries.add(ModItemGroups.buildItem(Items.BOW,
                Text.literal("Ashe's Bow").setStyle(Style.EMPTY.withColor(Formatting.AQUA)),
                List.of(Text.literal("Ashe ka bow hai bhai")),
                enchants,
                ModItemGroups.e(Enchantments.POWER, 7),
                ModItemGroups.e(Enchantments.MENDING, 1),
                ModItemGroups.e(Enchantments.INFINITY, 1)));
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(ModItemGroups.buildItem(Items.DIAMOND_PICKAXE,
                Text.literal("Hextech Drill"),
                List.of(Text.literal("Once a tool of Piltover's hextech miners,"),
                        Text.literal("now corrupted by Zaun's chemtech")),
                enchants,
                ModItemGroups.e(Enchantments.EFFICIENCY, 7),
                ModItemGroups.e(Enchantments.UNBREAKING, 3)));

        entries.add(ModItemGroups.buildItem(Items.DIAMOND_SHOVEL,
                Text.literal("Raftwreck Digger"),
                List.of(Text.literal("Once used by Raft survivors to unearth"),
                        Text.literal("lost treasures, it still carries"),
                        Text.literal("the scent of salt and despair")),
                enchants,
                ModItemGroups.e(Enchantments.EFFICIENCY, 7),
                ModItemGroups.e(Enchantments.UNBREAKING, 5)));

        entries.add(ModItemGroups.buildItem(Items.DIAMOND_HOE,
                Text.literal("Hoe Hoe Hoe"),
                List.of(Text.literal("Bihari Santa ka asli jugaadu hathiyaar"),
                        Text.literal("Kheton mein bhi chale aur dilon pe bhi vaar!")),
                enchants,
                ModItemGroups.e(Enchantments.EFFICIENCY, 6),
                ModItemGroups.e(Enchantments.UNBREAKING, 4),
                ModItemGroups.e(Enchantments.SILK_TOUCH, 1)));
    }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        // Head → chest → legs → feet, matching vanilla inventory slot order.
        entries.add(ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                Text.literal("Vox's Resonance Crown"),
                List.of(Text.literal("Harmonic shards, resonating with"),
                        Text.literal("echoes of forgotten songs")),
                enchants,
                ModItemGroups.e(Enchantments.PROTECTION, 6),
                ModItemGroups.e(Enchantments.AQUA_AFFINITY, 1),
                ModItemGroups.e(Enchantments.RESPIRATION, 4)));

        entries.add(ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                Text.literal("Brimstone's Surgeplate"),
                List.of(Text.literal("Open up the sky!")),
                enchants,
                ModItemGroups.e(Enchantments.PROTECTION, 4),
                ModItemGroups.e(Enchantments.FIRE_PROTECTION, 4)));

        entries.add(ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                Text.literal("Stormguard"),
                List.of(Text.literal("I am the stormguard"),
                        Text.literal("I am your misfortune and your despair"),
                        Text.literal("Halcyon shimmers in my veins")),
                enchants,
                ModItemGroups.e(Enchantments.PROTECTION, 6),
                ModItemGroups.e(Enchantments.UNBREAKING, 5),
                ModItemGroups.e(Enchantments.THORNS, 5)));

        entries.add(ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                Text.literal("Clutch God Leggings"),
                List.of(Text.literal("Crazyy..."),
                        Text.literal("Kya Aunty hai Jesus"),
                        Text.literal("Ye hi to time hai nashe karne ka")),
                enchants,
                ModItemGroups.e(Enchantments.SWIFT_SNEAK, 4),
                ModItemGroups.e(Enchantments.PROTECTION, 6)));

        entries.add(ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                Text.literal("Sundar Ki Chutiya Chappal").setStyle(Style.EMPTY.withColor(Formatting.AQUA)),
                List.of(Text.literal("The only shoes that run ").setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
                        Text.literal("faster than their owner").setStyle(Style.EMPTY.withColor(Formatting.YELLOW))),
                enchants,
                ModItemGroups.e(Enchantments.PROTECTION, 6),
                ModItemGroups.e(Enchantments.SOUL_SPEED, 8),
                ModItemGroups.e(Enchantments.DEPTH_STRIDER, 4),
                ModItemGroups.e(Enchantments.VANISHING_CURSE, 1)));

        entries.add(ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                Text.literal("Lavanya's Shackles"),
                List.of(Text.literal("When she walks, gravity kneels")),
                enchants,
                ModItemGroups.e(Enchantments.PROTECTION, 6),
                ModItemGroups.e(Enchantments.FEATHER_FALLING, 6)));
    }
}
