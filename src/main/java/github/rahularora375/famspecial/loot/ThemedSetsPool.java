package github.rahularora375.famspecial.loot;

import github.rahularora375.famspecial.item.entries.EshEndraNaveshItems;
import github.rahularora375.famspecial.item.entries.FireSerpentItems;
import github.rahularora375.famspecial.item.entries.KnightRadiantItems;
import github.rahularora375.famspecial.item.entries.MistbornItems;
import github.rahularora375.famspecial.item.entries.NecromancerItems;
import github.rahularora375.famspecial.item.entries.PacifistItems;
import github.rahularora375.famspecial.item.entries.PoseidonItems;
import github.rahularora375.famspecial.item.entries.RaidersLegacyItems;
import github.rahularora375.famspecial.item.entries.ShurimaItems;
import github.rahularora375.famspecial.item.entries.ThorItems;
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

import java.util.Optional;
import java.util.Set;

// Sibling to LegendaryPool / MapsPool. Injects the 50 themed-set items
// (Mistborn, Pacifist, Poseidon, Fire Serpent, Necromancer, Knight Radiant,
// Esh-Endra-Navesh, Shurima, Thor, Raider's Legacy — every theme except
// OldFam, which already drops out of LegendaryPool) into vanilla structure
// chests. Each rolled stack is a fresh ItemStack#copy() of the same template
// the creative tab builds, so SET_ID, IS_FAMSPECIAL_GEAR, INDESTRUCTIBLE,
// AttributeModifiers, custom MaceItem/CrossbowItem subclasses, pre-stamped
// Riptide III on Mjolnir, and every other DataComponent survives the loot
// pipeline intact (vanilla ItemEntry+SetEnchantmentsLootFunction would
// preserve only enchantments).
//
// Tier mapping is identical to LegendaryPool. Per-tier empty-slot weights
// are different — themed sets are roughly half as common as legendaries.
// Each item entry has weight 10; the Mending book has weight 100; the
// per-tier empty entries balance the chest drop rates.
public final class ThemedSetsPool {
    private ThemedSetsPool() {}

    // === Per-item weights — every themed item is weight 10. Grouped by
    // theme so it's obvious at a glance which constants belong together. ===

    // Mistborn
    private static final int W_ATIUM_DAGGER       = 10;
    private static final int W_MISTBORN_VEIL      = 10;
    private static final int W_MISTBORN_CLOAK     = 10;
    private static final int W_MISTBORN_WRAP      = 10;
    private static final int W_MISTBORN_DRIFT     = 10;

    // Pacifist
    private static final int W_SAGES_GRACE        = 10;
    private static final int W_SAGES_CROWN        = 10;
    private static final int W_SON_OF_THORS       = 10;
    private static final int W_SAGES_WRAP         = 10;
    private static final int W_VIKINGS_FAREWELL   = 10;

    // Poseidon
    private static final int W_TRIDENT_OF_OLYMPUS = 10;
    private static final int W_LORELAIS_CROWN     = 10;
    private static final int W_IRONBORN_PLATE     = 10;
    private static final int W_HIGHSTORMS_BIND    = 10;
    private static final int W_PHINNS_TRUDGE      = 10;

    // Fire Serpent
    private static final int W_FIRE_SERPENTS_WRATH = 10;
    private static final int W_IMPALERS_CROWN      = 10;
    private static final int W_SERPENTS_EMBRACE    = 10;
    private static final int W_KINDLINGS_WRAP      = 10;
    private static final int W_KINDLINGS_PATH      = 10;

    // Necromancer
    private static final int W_THRILLERS_EDGE      = 10;
    private static final int W_FEDORA_OF_DAMNED    = 10;
    private static final int W_SMOOTH_VESTMENT     = 10;
    private static final int W_BILLIE_JEANS        = 10;
    private static final int W_MOONWALKERS_CURSE   = 10;

    // Knight Radiant
    private static final int W_OATHBRINGER         = 10;
    private static final int W_SHARD_HELMET        = 10;
    private static final int W_SHARD_PLATE         = 10;
    private static final int W_SHARD_LEGGINGS      = 10;
    private static final int W_SHARD_BOOTS         = 10;

    // Esh-Endra-Navesh
    private static final int W_JUST_HIT_BRO        = 10;
    private static final int W_TIME_DEKHLO_HELMET  = 10;
    private static final int W_BOMBARDIRO          = 10;
    private static final int W_KNEE_USE_PAPER      = 10;
    private static final int W_DHOOM_MACHALE       = 10;

    // Shurima
    private static final int W_SUN_DISC_SPEAR      = 10;
    private static final int W_SUN_EMPERORS_CROWN  = 10;
    private static final int W_MANTLE_OF_SHURIMA   = 10;
    private static final int W_SANDSTRIDER_WRAPS   = 10;
    private static final int W_DRIFTING_SANDS      = 10;

    // Thor
    private static final int W_MJOLNIR             = 10;
    private static final int W_THUNDER_HELM        = 10;
    private static final int W_ASGARDIAN_PLATE     = 10;
    private static final int W_WARRIORS_GREAVES    = 10;
    private static final int W_STORM_STRIDER       = 10;

    // Raider's Legacy
    private static final int W_FORTUNE_AND_GLORY   = 10;
    private static final int W_HAT_OF_THE_RAIDER   = 10;
    private static final int W_EXPLORERS_JACKET    = 10;
    private static final int W_TROUSERS_OF_TRAIL   = 10;
    private static final int W_THE_CLOSE_CALL      = 10;

    // Bonus utility
    private static final int W_MENDING_BOOK        = 100;

    // === Per-tier empty-slot weights — these balance the chest drop rates.
    // CRAZY chests drop themed gear most often (smallest empty), F chests
    // are nearly always empty (largest empty). Mirrors LegendaryPool's
    // per-tier shape but with substantially heavier empties because the
    // per-item weights here are 10 instead of LegendaryPool's mixed pattern. ===
    private enum LootTier {
        CRAZY(1900),
        S(4400),
        A(9400),
        F(19400);

        final int emptyWeight;

        LootTier(int emptyWeight) {
            this.emptyWeight = emptyWeight;
        }
    }

    // Tier sets — identical to LegendaryPool.java:42-83 (verbatim).
    private static final Set<RegistryKey<LootTable>> CRAZY_TIER = Set.of(
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE_CHEST,
            LootTables.ANCIENT_CITY_CHEST,
            LootTables.ANCIENT_CITY_ICE_BOX_CHEST,
            LootTables.BASTION_TREASURE_CHEST,
            LootTables.WOODLAND_MANSION_CHEST
    );

    private static final Set<RegistryKey<LootTable>> S_TIER = Set.of(
            LootTables.DESERT_PYRAMID_CHEST,
            LootTables.JUNGLE_TEMPLE_CHEST,
            LootTables.BURIED_TREASURE_CHEST,
            LootTables.TRIAL_CHAMBERS_REWARD_UNIQUE_CHEST,
            LootTables.BASTION_BRIDGE_CHEST,
            LootTables.BASTION_HOGLIN_STABLE_CHEST,
            LootTables.BASTION_OTHER_CHEST
    );

    private static final Set<RegistryKey<LootTable>> A_TIER = Set.of(
            LootTables.END_CITY_TREASURE_CHEST,
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

        // === Mistborn ===
        pool.with(PrebuiltStackEntry.builder(() -> MistbornItems.buildAtiumDagger(enchants),       W_ATIUM_DAGGER));
        pool.with(PrebuiltStackEntry.builder(() -> MistbornItems.buildMistbornVeil(enchants),      W_MISTBORN_VEIL));
        pool.with(PrebuiltStackEntry.builder(() -> MistbornItems.buildMistbornCloak(enchants),     W_MISTBORN_CLOAK));
        pool.with(PrebuiltStackEntry.builder(() -> MistbornItems.buildMistbornWrap(enchants),      W_MISTBORN_WRAP));
        pool.with(PrebuiltStackEntry.builder(() -> MistbornItems.buildMistbornDrift(enchants),     W_MISTBORN_DRIFT));

        // === Pacifist ===
        pool.with(PrebuiltStackEntry.builder(() -> PacifistItems.buildSagesGrace(enchants),        W_SAGES_GRACE));
        pool.with(PrebuiltStackEntry.builder(() -> PacifistItems.buildSagesCrown(enchants),        W_SAGES_CROWN));
        pool.with(PrebuiltStackEntry.builder(() -> PacifistItems.buildSonOfThors(enchants),        W_SON_OF_THORS));
        pool.with(PrebuiltStackEntry.builder(() -> PacifistItems.buildSagesWrap(enchants),         W_SAGES_WRAP));
        pool.with(PrebuiltStackEntry.builder(() -> PacifistItems.buildVikingsFarewell(enchants),   W_VIKINGS_FAREWELL));

        // === Poseidon ===
        pool.with(PrebuiltStackEntry.builder(() -> PoseidonItems.buildTridentOfOlympus(enchants),  W_TRIDENT_OF_OLYMPUS));
        pool.with(PrebuiltStackEntry.builder(() -> PoseidonItems.buildLorelaisCrown(enchants),     W_LORELAIS_CROWN));
        pool.with(PrebuiltStackEntry.builder(() -> PoseidonItems.buildIronbornPlate(enchants),     W_IRONBORN_PLATE));
        pool.with(PrebuiltStackEntry.builder(() -> PoseidonItems.buildHighstormsBind(enchants),    W_HIGHSTORMS_BIND));
        pool.with(PrebuiltStackEntry.builder(() -> PoseidonItems.buildPhinnsTrudge(enchants),      W_PHINNS_TRUDGE));

        // === Fire Serpent ===
        pool.with(PrebuiltStackEntry.builder(() -> FireSerpentItems.buildFireSerpentsWrath(enchants), W_FIRE_SERPENTS_WRATH));
        pool.with(PrebuiltStackEntry.builder(() -> FireSerpentItems.buildImpalersCrown(enchants),     W_IMPALERS_CROWN));
        pool.with(PrebuiltStackEntry.builder(() -> FireSerpentItems.buildSerpentsEmbrace(enchants),   W_SERPENTS_EMBRACE));
        pool.with(PrebuiltStackEntry.builder(() -> FireSerpentItems.buildKindlingsWrap(enchants),     W_KINDLINGS_WRAP));
        pool.with(PrebuiltStackEntry.builder(() -> FireSerpentItems.buildKindlingsPath(enchants),     W_KINDLINGS_PATH));

        // === Necromancer ===
        pool.with(PrebuiltStackEntry.builder(() -> NecromancerItems.buildThrillersEdge(enchants),         W_THRILLERS_EDGE));
        pool.with(PrebuiltStackEntry.builder(() -> NecromancerItems.buildFedoraOfTheDamned(enchants),     W_FEDORA_OF_DAMNED));
        pool.with(PrebuiltStackEntry.builder(() -> NecromancerItems.buildSmoothCriminalsVestment(enchants), W_SMOOTH_VESTMENT));
        pool.with(PrebuiltStackEntry.builder(() -> NecromancerItems.buildBillieJeans(enchants),           W_BILLIE_JEANS));
        pool.with(PrebuiltStackEntry.builder(() -> NecromancerItems.buildMoonwalkersCurse(enchants),      W_MOONWALKERS_CURSE));

        // === Knight Radiant ===
        pool.with(PrebuiltStackEntry.builder(() -> KnightRadiantItems.buildOathbringer(enchants),    W_OATHBRINGER));
        pool.with(PrebuiltStackEntry.builder(() -> KnightRadiantItems.buildShardHelmet(enchants),    W_SHARD_HELMET));
        pool.with(PrebuiltStackEntry.builder(() -> KnightRadiantItems.buildShardPlate(enchants),     W_SHARD_PLATE));
        pool.with(PrebuiltStackEntry.builder(() -> KnightRadiantItems.buildShardLeggings(enchants),  W_SHARD_LEGGINGS));
        pool.with(PrebuiltStackEntry.builder(() -> KnightRadiantItems.buildShardBoots(enchants),     W_SHARD_BOOTS));

        // === Esh-Endra-Navesh ===
        pool.with(PrebuiltStackEntry.builder(() -> EshEndraNaveshItems.buildJustHitBro(enchants),         W_JUST_HIT_BRO));
        pool.with(PrebuiltStackEntry.builder(() -> EshEndraNaveshItems.buildTimeDekhloHelmet(enchants),   W_TIME_DEKHLO_HELMET));
        pool.with(PrebuiltStackEntry.builder(() -> EshEndraNaveshItems.buildBombardiroCoccodrillo(enchants), W_BOMBARDIRO));
        pool.with(PrebuiltStackEntry.builder(() -> EshEndraNaveshItems.buildKneeUsePaper(enchants),       W_KNEE_USE_PAPER));
        pool.with(PrebuiltStackEntry.builder(() -> EshEndraNaveshItems.buildDhoomMachale(enchants),       W_DHOOM_MACHALE));

        // === Shurima ===
        pool.with(PrebuiltStackEntry.builder(() -> ShurimaItems.buildSunDiscSpear(enchants),       W_SUN_DISC_SPEAR));
        pool.with(PrebuiltStackEntry.builder(() -> ShurimaItems.buildSunEmperorsCrown(enchants),   W_SUN_EMPERORS_CROWN));
        pool.with(PrebuiltStackEntry.builder(() -> ShurimaItems.buildMantleOfShurima(enchants),    W_MANTLE_OF_SHURIMA));
        pool.with(PrebuiltStackEntry.builder(() -> ShurimaItems.buildSandstriderWraps(enchants),   W_SANDSTRIDER_WRAPS));
        pool.with(PrebuiltStackEntry.builder(() -> ShurimaItems.buildDriftingSands(enchants),      W_DRIFTING_SANDS));

        // === Thor ===
        pool.with(PrebuiltStackEntry.builder(() -> ThorItems.buildMjolnir(enchants),               W_MJOLNIR));
        pool.with(PrebuiltStackEntry.builder(() -> ThorItems.buildThunderHelm(enchants),           W_THUNDER_HELM));
        pool.with(PrebuiltStackEntry.builder(() -> ThorItems.buildAsgardianPlate(enchants),        W_ASGARDIAN_PLATE));
        pool.with(PrebuiltStackEntry.builder(() -> ThorItems.buildWarriorsGreaves(enchants),       W_WARRIORS_GREAVES));
        pool.with(PrebuiltStackEntry.builder(() -> ThorItems.buildStormStrider(enchants),          W_STORM_STRIDER));

        // === Raider's Legacy ===
        pool.with(PrebuiltStackEntry.builder(() -> RaidersLegacyItems.buildFortuneAndGlory(enchants),     W_FORTUNE_AND_GLORY));
        pool.with(PrebuiltStackEntry.builder(() -> RaidersLegacyItems.buildHatOfTheRaider(enchants),      W_HAT_OF_THE_RAIDER));
        pool.with(PrebuiltStackEntry.builder(() -> RaidersLegacyItems.buildExplorersJacket(enchants),     W_EXPLORERS_JACKET));
        pool.with(PrebuiltStackEntry.builder(() -> RaidersLegacyItems.buildTrousersOfTheTrail(enchants),  W_TROUSERS_OF_TRAIL));
        pool.with(PrebuiltStackEntry.builder(() -> RaidersLegacyItems.buildTheCloseCall(enchants),        W_THE_CLOSE_CALL));

        // === Mending book — same recipe LegendaryPool uses, just at a
        // different weight. ===
        pool.with(ItemEntry.builder(Items.BOOK).weight(W_MENDING_BOOK)
                .apply(new SetEnchantmentsLootFunction.Builder()
                        .enchantment(enchants.getOrThrow(Enchantments.MENDING),
                                ConstantLootNumberProvider.create(1))));

        // === Per-tier empty entry ===
        pool.with(EmptyEntry.builder().weight(tier.emptyWeight));

        return pool.build();
    }
}
