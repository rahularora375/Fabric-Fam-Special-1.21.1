package github.rahularora375.famspecial.loot;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.EmptyEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetEnchantmentsLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.Set;

import static github.rahularora375.famspecial.loot.LootPoolHelpers.ench;
import static github.rahularora375.famspecial.loot.LootPoolHelpers.namedItem;

public final class LegendaryPool {
    private LegendaryPool() {}

    private enum LootTier {
        CRAZY(640),
        S(1440),
        A(1840),
        F(3040);

        final int emptyWeight;

        LootTier(int emptyWeight) {
            this.emptyWeight = emptyWeight;
        }
    }

    // Crazy Tier — rarest (ominous trials, ancient city, bastion treasure)
    private static final Set<RegistryKey<LootTable>> CRAZY_TIER = Set.of(
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE_CHEST,
            LootTables.ANCIENT_CITY_CHEST,
            LootTables.ANCIENT_CITY_ICE_BOX_CHEST,
            LootTables.BASTION_TREASURE_CHEST
    );

    // S Tier — hard (desert/jungle temples, mansion, buried treasure, trial chambers unique, bastions)
    private static final Set<RegistryKey<LootTable>> S_TIER = Set.of(
            LootTables.DESERT_PYRAMID_CHEST,
            LootTables.JUNGLE_TEMPLE_CHEST,
            LootTables.WOODLAND_MANSION_CHEST,
            LootTables.BURIED_TREASURE_CHEST,
            LootTables.TRIAL_CHAMBERS_REWARD_UNIQUE_CHEST,
            LootTables.BASTION_BRIDGE_CHEST,
            LootTables.BASTION_HOGLIN_STABLE_CHEST,
            LootTables.BASTION_OTHER_CHEST
    );

    // A Tier — moderate (end city, pillager outpost, underwater ruins, shipwreck treasure, ominous rare)
    private static final Set<RegistryKey<LootTable>> A_TIER = Set.of(
            LootTables.END_CITY_TREASURE_CHEST,
            LootTables.PILLAGER_OUTPOST_CHEST,
            LootTables.UNDERWATER_RUIN_BIG_CHEST,
            LootTables.UNDERWATER_RUIN_SMALL_CHEST,
            LootTables.SHIPWRECK_TREASURE_CHEST,
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE_CHEST
    );

    // F Tier — common/easy structures
    private static final Set<RegistryKey<LootTable>> F_TIER = Set.of(
            LootTables.ABANDONED_MINESHAFT_CHEST,
            LootTables.SIMPLE_DUNGEON_CHEST,
            LootTables.SHIPWRECK_MAP_CHEST,
            LootTables.SHIPWRECK_SUPPLY_CHEST,
            LootTables.NETHER_BRIDGE_CHEST,
            LootTables.IGLOO_CHEST_CHEST,
            LootTables.STRONGHOLD_CORRIDOR_CHEST,
            LootTables.STRONGHOLD_CROSSING_CHEST,
            LootTables.STRONGHOLD_LIBRARY_CHEST,
            LootTables.TRIAL_CHAMBERS_REWARD_RARE_CHEST
    );

    public static Optional<LootPool> buildFor(
            RegistryKey<LootTable> key,
            RegistryWrapper<Enchantment> enchants) {
        LootTier tier = getTier(key);
        if (tier == null) return Optional.empty();
        return Optional.of(buildPool(tier, enchants));
    }

    private static LootTier getTier(RegistryKey<LootTable> key) {
        if (CRAZY_TIER.contains(key)) return LootTier.CRAZY;
        if (S_TIER.contains(key)) return LootTier.S;
        if (A_TIER.contains(key)) return LootTier.A;
        if (F_TIER.contains(key)) return LootTier.F;
        return null;
    }

    private static LootPool buildPool(LootTier tier, RegistryWrapper<Enchantment> enchants) {
        LootPool.Builder pool = LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .bonusRolls(ConstantLootNumberProvider.create(0));

        // === Weapons ===

        // DEMACIAAAAAAAA !!!
        pool.with(namedItem(Items.DIAMOND_SWORD, 10,
                Text.literal("DEMACIAAAAAAAA !!!"),
                new Text[]{Text.literal("By this blade, Demacia's enemies shall fall")},
                enchants,
                ench(Enchantments.SHARPNESS, 6, 7),
                ench(Enchantments.UNBREAKING, 3, 3),
                ench(Enchantments.LOOTING, 4, 5)));

        // Blade of the Ruined King
        pool.with(namedItem(Items.DIAMOND_SWORD, 10,
                Text.literal("Blade of the Ruined King").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)),
                new Text[]{
                        Text.literal("Wielded by a king lost in agony").setStyle(Style.EMPTY.withColor(Formatting.WHITE)),
                        Text.literal("It cuts deeper with every heartbreak").setStyle(Style.EMPTY.withColor(Formatting.WHITE))
                },
                enchants,
                ench(Enchantments.SMITE, 6, 7),
                ench(Enchantments.LOOTING, 4, 5)));

        // Leviathan Axe
        pool.with(namedItem(Items.DIAMOND_AXE, 10,
                Text.literal("Leviathan Axe"),
                new Text[]{
                        Text.literal("Forged in the icy depths of J\u00f6tunheim,"),
                        Text.literal("this axe carries the fury of a fallen god")
                },
                enchants,
                ench(Enchantments.EFFICIENCY, 6, 7),
                ench(Enchantments.SHARPNESS, 6, 7)));

        // Ashe's Bow
        pool.with(namedItem(Items.BOW, 10,
                Text.literal("Ashe's Bow").setStyle(Style.EMPTY.withColor(Formatting.AQUA)),
                new Text[]{Text.literal("Ashe ka bow hai bhai")},
                enchants,
                ench(Enchantments.POWER, 6, 7),
                ench(Enchantments.MENDING, 1, 1),
                ench(Enchantments.INFINITY, 1, 1)));

        // === Tools ===

        // Hoe Hoe Hoe
        pool.with(namedItem(Items.DIAMOND_HOE, 10,
                Text.literal("Hoe Hoe Hoe"),
                new Text[]{
                        Text.literal("Bihari Santa ka asli jugaadu hathiyaar"),
                        Text.literal("Kheton mein bhi chale aur dilon pe bhi vaar!")
                },
                enchants,
                ench(Enchantments.EFFICIENCY, 6, 6),
                ench(Enchantments.UNBREAKING, 4, 4),
                ench(Enchantments.SILK_TOUCH, 1, 1)));

        // Hextech Drill
        pool.with(namedItem(Items.DIAMOND_PICKAXE, 10,
                Text.literal("Hextech Drill"),
                new Text[]{
                        Text.literal("Once a tool of Piltover's hextech miners,"),
                        Text.literal("now corrupted by Zaun's chemtech")
                },
                enchants,
                ench(Enchantments.EFFICIENCY, 6, 7),
                ench(Enchantments.UNBREAKING, 3, 3)));

        // Raftwreck Digger
        pool.with(namedItem(Items.DIAMOND_SHOVEL, 10,
                Text.literal("Raftwreck Digger"),
                new Text[]{
                        Text.literal("Once used by Raft survivors to unearth"),
                        Text.literal("lost treasures, it still carries"),
                        Text.literal("the scent of salt and despair")
                },
                enchants,
                ench(Enchantments.EFFICIENCY, 6, 7),
                ench(Enchantments.UNBREAKING, 4, 5)));

        // === Armor ===

        // Brimstone's Surgeplate
        pool.with(namedItem(Items.DIAMOND_CHESTPLATE, 10,
                Text.literal("Brimstone's Surgeplate"),
                new Text[]{Text.literal("Open up the sky!")},
                enchants,
                ench(Enchantments.PROTECTION, 4, 4),
                ench(Enchantments.FIRE_PROTECTION, 4, 4)));

        // Stormguard
        pool.with(namedItem(Items.DIAMOND_CHESTPLATE, 10,
                Text.literal("Stormguard"),
                new Text[]{
                        Text.literal("I am the stormguard"),
                        Text.literal("I am your misfortune and your despair"),
                        Text.literal("Halcyon shimmers in my veins")
                },
                enchants,
                ench(Enchantments.PROTECTION, 5, 6),
                ench(Enchantments.UNBREAKING, 4, 5),
                ench(Enchantments.THORNS, 5, 5)));

        // Vox's Resonance Crown
        pool.with(namedItem(Items.DIAMOND_HELMET, 10,
                Text.literal("Vox's Resonance Crown"),
                new Text[]{
                        Text.literal("Harmonic shards, resonating with"),
                        Text.literal("echoes of forgotten songs")
                },
                enchants,
                ench(Enchantments.PROTECTION, 5, 6),
                ench(Enchantments.AQUA_AFFINITY, 1, 1),
                ench(Enchantments.RESPIRATION, 3, 4)));

        // Sundar Ki Chutiya Chappal
        pool.with(namedItem(Items.DIAMOND_BOOTS, 10,
                Text.literal("Sundar Ki Chutiya Chappal").setStyle(Style.EMPTY.withColor(Formatting.AQUA)),
                new Text[]{
                        Text.literal("The only shoes that run ").setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
                        Text.literal("faster than their owner").setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
                },
                enchants,
                ench(Enchantments.PROTECTION, 5, 6),
                ench(Enchantments.SOUL_SPEED, 5, 8),
                ench(Enchantments.DEPTH_STRIDER, 4, 4),
                ench(Enchantments.VANISHING_CURSE, 1, 1)));

        // Lavanya's Shackles
        pool.with(namedItem(Items.DIAMOND_BOOTS, 10,
                Text.literal("Lavanya's Shackles"),
                new Text[]{Text.literal("When she walks, gravity kneels")},
                enchants,
                ench(Enchantments.PROTECTION, 5, 6),
                ench(Enchantments.FEATHER_FALLING, 5, 6)));

        // Clutch God Leggings
        pool.with(namedItem(Items.DIAMOND_LEGGINGS, 10,
                Text.literal("Clutch God Leggings"),
                new Text[]{
                        Text.literal("Crazyy..."),
                        Text.literal("Kya Aunty hai Jesus"),
                        Text.literal("Ye hi to time hai nashe karne ka")
                },
                enchants,
                ench(Enchantments.SWIFT_SNEAK, 4, 4),
                ench(Enchantments.PROTECTION, 5, 6)));

        // === Mending Book ===
        pool.with(ItemEntry.builder(Items.BOOK).weight(30)
                .apply(new SetEnchantmentsLootFunction.Builder()
                        .enchantment(enchants.getOrThrow(Enchantments.MENDING),
                                ConstantLootNumberProvider.create(1))));

        // === Empty entry ===
        pool.with(EmptyEntry.builder().weight(tier.emptyWeight));

        return pool.build();
    }
}
