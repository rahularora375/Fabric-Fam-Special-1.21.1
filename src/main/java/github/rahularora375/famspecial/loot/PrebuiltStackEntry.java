package github.rahularora375.famspecial.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import github.rahularora375.famspecial.FamSpecial;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Supplier;

// Custom LootPoolEntry that emits a fully-baked ItemStack supplied by a
// callback. Each roll calls supplier.get() to produce a fresh stack template
// and then ItemStack#copy()s it into the loot stream so two rolls of the same
// entry don't share component instances. This is how the ThemedSetsPool
// preserves all of the custom DataComponents (SET_ID, IS_FAMSPECIAL_GEAR,
// INDESTRUCTIBLE, BLOCKS_MENDING, BOUNTY_HUNTER, STORM_COOLDOWN_END,
// AttributeModifiers, custom MaceItem/CrossbowItem instances, pre-stamped
// Riptide III on Mjolnir, etc.) — vanilla ItemEntry+SetEnchantmentsLootFunction
// only knows about enchantments, not arbitrary components.
//
// Codec only serializes weight/quality/conditions/functions — the supplier
// itself isn't serialized. This is deliberate: this entry type is only used
// for code-built pools injected via LootTableEvents.MODIFY at runtime; it is
// not intended to round-trip through datapack JSON.
public class PrebuiltStackEntry extends LeafEntry {
    public static final MapCodec<PrebuiltStackEntry> CODEC = RecordCodecBuilder.mapCodec(
            instance -> addLeafFields(instance).apply(instance,
                    (weight, quality, conditions, functions) ->
                            new PrebuiltStackEntry(() -> ItemStack.EMPTY, weight, quality, conditions, functions))
    );

    public static LootPoolEntryType TYPE;

    public static void register() {
        TYPE = Registry.register(
                Registries.LOOT_POOL_ENTRY_TYPE,
                Identifier.of(FamSpecial.MOD_ID, "prebuilt_stack"),
                new LootPoolEntryType(CODEC));
    }

    private final Supplier<ItemStack> stackSupplier;

    private PrebuiltStackEntry(Supplier<ItemStack> stackSupplier, int weight, int quality,
                               List<LootCondition> conditions, List<LootFunction> functions) {
        super(weight, quality, conditions, functions);
        this.stackSupplier = stackSupplier;
    }

    @Override
    public LootPoolEntryType getType() {
        return TYPE;
    }

    @Override
    protected void generateLoot(java.util.function.Consumer<ItemStack> lootConsumer, LootContext context) {
        ItemStack template = stackSupplier.get();
        if (template == null || template.isEmpty()) return;
        lootConsumer.accept(template.copy());
    }

    public static LeafEntry.Builder<?> builder(Supplier<ItemStack> stackSupplier, int weight) {
        LeafEntry.Builder<?> b = LeafEntry.builder(
                (weight2, quality, conditions, functions) ->
                        new PrebuiltStackEntry(stackSupplier, weight2, quality, conditions, functions));
        return b.weight(weight);
    }
}
