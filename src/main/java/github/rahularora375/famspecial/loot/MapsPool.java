package github.rahularora375.famspecial.loot;

import github.rahularora375.famspecial.FamSpecial;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.EmptyEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ExplorationMapLootFunction;
import net.minecraft.loot.function.SetNameLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

import java.util.Optional;
import java.util.Set;

public final class MapsPool {
    private MapsPool() {}

    // Empty weights taken verbatim from the Map Items sheet. Crazy/S use 10M to
    // suppress map drops to ~0% while keeping entries structurally present;
    // A/F target 15%/10% map drop rates.
    private enum MapTier {
        CRAZY(5999940),
        S(5999940),
        A(340),
        F(540);

        final int emptyWeight;

        MapTier(int emptyWeight) {
            this.emptyWeight = emptyWeight;
        }
    }

    // Tier sets mirror LegendaryPool, minus non-overworld chests (Nether/End).
    // ExplorationMapLootFunction searches structures in the chest's dimension,
    // so Nether/End chests silently resolve to blank maps — commented out below.
    private static final Set<RegistryKey<LootTable>> CRAZY_TIER = Set.of(
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE_CHEST,
            LootTables.ANCIENT_CITY_CHEST,
            LootTables.ANCIENT_CITY_ICE_BOX_CHEST,
            LootTables.WOODLAND_MANSION_CHEST
            // LootTables.BASTION_TREASURE_CHEST        // Nether
    );

    private static final Set<RegistryKey<LootTable>> S_TIER = Set.of(
            LootTables.DESERT_PYRAMID_CHEST,
            LootTables.JUNGLE_TEMPLE_CHEST,
            LootTables.BURIED_TREASURE_CHEST,
            LootTables.TRIAL_CHAMBERS_REWARD_UNIQUE_CHEST
            // LootTables.BASTION_BRIDGE_CHEST,         // Nether
            // LootTables.BASTION_HOGLIN_STABLE_CHEST,  // Nether
            // LootTables.BASTION_OTHER_CHEST           // Nether
    );

    private static final Set<RegistryKey<LootTable>> A_TIER = Set.of(
            // LootTables.END_CITY_TREASURE_CHEST,      // End
            LootTables.PILLAGER_OUTPOST_CHEST,
            LootTables.UNDERWATER_RUIN_BIG_CHEST,
            LootTables.UNDERWATER_RUIN_SMALL_CHEST,
            LootTables.SHIPWRECK_TREASURE_CHEST,
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE_CHEST
    );

    private static final Set<RegistryKey<LootTable>> F_TIER = Set.of(
            LootTables.ABANDONED_MINESHAFT_CHEST,
            LootTables.SIMPLE_DUNGEON_CHEST,
            LootTables.SHIPWRECK_MAP_CHEST,
            LootTables.SHIPWRECK_SUPPLY_CHEST,
            // LootTables.NETHER_BRIDGE_CHEST,          // Nether
            LootTables.IGLOO_CHEST_CHEST,
            LootTables.STRONGHOLD_CORRIDOR_CHEST,
            LootTables.STRONGHOLD_CROSSING_CHEST,
            LootTables.STRONGHOLD_LIBRARY_CHEST,
            LootTables.TRIAL_CHAMBERS_REWARD_RARE_CHEST
    );

    private static final TagKey<Structure> ON_ANCIENT_CITY_MAPS = TagKey.of(
            RegistryKeys.STRUCTURE,
            Identifier.of(FamSpecial.MOD_ID, "on_ancient_city_maps"));

    public static Optional<LootPool> buildFor(RegistryKey<LootTable> key) {
        MapTier tier = getTier(key);
        if (tier == null) return Optional.empty();
        return Optional.of(buildPool(tier));
    }

    private static MapTier getTier(RegistryKey<LootTable> key) {
        if (CRAZY_TIER.contains(key)) return MapTier.CRAZY;
        if (S_TIER.contains(key)) return MapTier.S;
        if (A_TIER.contains(key)) return MapTier.A;
        if (F_TIER.contains(key)) return MapTier.F;
        return null;
    }

    private static LootPool buildPool(MapTier tier) {
        LootPool.Builder pool = LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .bonusRolls(ConstantLootNumberProvider.create(0));

        pool.with(ItemEntry.builder(Items.MAP).weight(20)
                .apply(SetNameLootFunction.builder(
                        Text.translatable("filled_map.jungle_temple"), SetNameLootFunction.Target.ITEM_NAME))
                .apply(ExplorationMapLootFunction.builder()
                        .withDestination(StructureTags.ON_JUNGLE_EXPLORER_MAPS)
                        .withDecoration(MapDecorationTypes.JUNGLE_TEMPLE)
                        .withZoom((byte) 1)
                        .searchRadius(100)
                        .withSkipExistingChunks(false)));

        pool.with(ItemEntry.builder(Items.MAP).weight(20)
                .apply(SetNameLootFunction.builder(
                        Text.literal("Ancient City Map"), SetNameLootFunction.Target.ITEM_NAME))
                .apply(ExplorationMapLootFunction.builder()
                        .withDestination(ON_ANCIENT_CITY_MAPS)
                        .withDecoration(MapDecorationTypes.RED_X)
                        .withZoom((byte) 1)
                        .searchRadius(100)
                        .withSkipExistingChunks(false)));

        pool.with(ItemEntry.builder(Items.MAP).weight(20)
                .apply(SetNameLootFunction.builder(
                        Text.translatable("filled_map.mansion"), SetNameLootFunction.Target.ITEM_NAME))
                .apply(ExplorationMapLootFunction.builder()
                        .withDestination(StructureTags.ON_WOODLAND_EXPLORER_MAPS)
                        .withDecoration(MapDecorationTypes.MANSION)
                        .withZoom((byte) 1)
                        .searchRadius(150)
                        .withSkipExistingChunks(false)));

        pool.with(EmptyEntry.builder().weight(tier.emptyWeight));

        return pool.build();
    }
}