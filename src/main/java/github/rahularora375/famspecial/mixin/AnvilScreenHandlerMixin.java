package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.item.FortuneGloryItem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Anvil merge feature only — the chestplate→elytra identity merge. No other
// anvil gating: themed gear behaves like vanilla for rename, repair-combine,
// and enchant-merge operations.
//
// Extends ForgingScreenHandler (not a @Shadow) because `input` and `output`
// are declared on the parent class — in dev mappings, Mixin can't @Shadow
// inherited fields. The constructor is never invoked; Mixin strips it.
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {
    // Set IDs whose diamond chestplate is allowed to merge onto a vanilla elytra
    // via the anvil path below. OldFam is intentionally excluded — its pieces
    // don't carry SET_ID and aren't part of this feature. Any SET_ID not in
    // this allowlist is rejected, so future themes opt in explicitly.
    private static final Set<String> MERGE_ELIGIBLE_SETS = Set.of(
            "mistborn",
            "pacifist",
            "poseidon",
            "fire_serpent",
            "necromancer",
            "knight_radiant",
            "esh_endra_navesh",
            "shurima",
            "thor",
            "raider"
    );

    // Fortune & Glory is built with Quick Charge IV baked in
    // (see RaidersLegacyItems#buildFortuneAndGlory). The anvil-merge path below
    // enforces this as a floor: even if both inputs lack QC entirely, or one
    // has a lower-level QC, the merged result is forced to QC IV.
    private static final int FG_BAKED_QUICK_CHARGE_LEVEL = 4;

    @Shadow @Final private Property levelCost;
    @Shadow private String newItemName;

    private AnvilScreenHandlerMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
                                    ScreenHandlerContext context, ForgingSlotsManager slots) {
        super(type, syncId, playerInventory, context, slots);
    }

    // Merge path: any themed diamond chestplate whose SET_ID is in
    // MERGE_ELIGIBLE_SETS (slot 0) + a vanilla elytra (slot 1) → a vanilla
    // elytra stack carrying the chestplate's identity, so the player wears an
    // elytra that still completes the themed set and drives its bonuses.
    // Covers 9 themes (Mistborn, Pacifist, Poseidon, FireSerpent, Necromancer,
    // KnightRadiant, EshEndraNavesh, Shurima, Thor); OldFam is excluded because
    // its pieces don't carry SET_ID. Mending and Unbreaking are preserved from
    // the source chestplate and elytra (max level wins for duplicates); all
    // other enchants are stripped. Rename field is honored when non-blank.
    //
    // Hardened against NBT forgery: the stack must also carry the
    // IS_FAMSPECIAL_GEAR identity flag, which is only stamped by
    // ModItemGroups — a plain diamond chestplate with a hand-stamped SET_ID
    // is rejected. The durability-lifecycle flags (INDESTRUCTIBLE,
    // REGENS_DURABILITY, BLOCKS_MENDING) and the chestplate's base armor /
    // armor-toughness attribute modifiers are also filtered out downstream in
    // buildMergedElytra to prevent an infinite-flight exploit.
    //
    // HEAD + cancellable because vanilla's updateResult early-returns mid-method
    // for incompatible item pairs (chestplate + elytra aren't same-item and
    // elytra isn't a repair material), and @At("TAIL") only fires at the last
    // RETURN instruction — so an early return bypasses TAIL. Running at HEAD
    // intercepts before that path.
    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void famspecial$mergeThemedChestplateElytra(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);
        if (left.isEmpty() || right.isEmpty()) return;
        if (left.getItem() != Items.DIAMOND_CHESTPLATE) return;
        String leftSetId = left.get(ModComponents.SET_ID);
        if (leftSetId == null || !MERGE_ELIGIBLE_SETS.contains(leftSetId)) return;
        if (!Boolean.TRUE.equals(left.get(ModComponents.IS_FAMSPECIAL_GEAR))) return;
        if (!right.isOf(Items.ELYTRA)) return;
        if (right.get(ModComponents.SET_ID) != null) return;

        ItemStack merged = famspecial$buildMergedElytra(left, right);
        this.output.setStack(0, merged);
        FamSpecial.LOGGER.info("Merged {} chestplate into elytra; result SET_ID={}",
                leftSetId, merged.get(ModComponents.SET_ID));
        this.levelCost.set(4);
        this.sendContentUpdates();
        ci.cancel();
    }

    private ItemStack famspecial$buildMergedElytra(ItemStack chestplate, ItemStack elytra) {
        ItemStack out = new ItemStack(Items.ELYTRA);

        var lore = chestplate.get(DataComponentTypes.LORE);
        if (lore != null) out.set(DataComponentTypes.LORE, lore);

        // Copy custom attribute modifiers (MAX_HEALTH, MOVEMENT_SPEED,
        // KNOCKBACK_RESISTANCE, etc.) but strip the chestplate's base ARMOR and
        // ARMOR_TOUGHNESS — wearing this as an elytra intentionally trades
        // armor points for flight, so we don't want the diamond baseline
        // leaking onto the elytra slot.
        AttributeModifiersComponent sourceAttrs = chestplate.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (sourceAttrs != null) {
            AttributeModifiersComponent.Builder b = AttributeModifiersComponent.builder();
            for (AttributeModifiersComponent.Entry entry : sourceAttrs.modifiers()) {
                if (entry.attribute().equals(EntityAttributes.ARMOR)) continue;
                if (entry.attribute().equals(EntityAttributes.ARMOR_TOUGHNESS)) continue;
                b.add(entry.attribute(), entry.modifier(), entry.slot());
            }
            out.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, b.build());
        }

        if (this.newItemName != null && !this.newItemName.isBlank()) {
            out.set(DataComponentTypes.CUSTOM_NAME, Text.literal(this.newItemName));
        } else {
            var name = chestplate.get(DataComponentTypes.CUSTOM_NAME);
            if (name != null) out.set(DataComponentTypes.CUSTOM_NAME, name);
        }

        // Merge Unbreaking/Mending from both inputs, taking the max level when
        // both contribute the same enchant. All other enchants are stripped.
        ItemEnchantmentsComponent.Builder builder =
                new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        mergeAllowedEnchants(builder, chestplate.getEnchantments());
        mergeAllowedEnchants(builder, elytra.getEnchantments());
        EnchantmentHelper.set(out, builder.build());

        ModComponents.copyModComponentsForElytra(chestplate, out);

        return out;
    }

    // Copies only Unbreaking / Mending entries from `source` into `builder`,
    // using Builder#add which keeps the max level when the same enchant is
    // contributed by both chestplate and elytra.
    private static void mergeAllowedEnchants(ItemEnchantmentsComponent.Builder builder,
                                             ItemEnchantmentsComponent source) {
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : source.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> key = entry.getKey();
            if (!key.matchesKey(Enchantments.MENDING) && !key.matchesKey(Enchantments.UNBREAKING)) continue;
            builder.add(key, entry.getIntValue());
        }
    }

    // Anvil merge path for the custom crossbow Fortune & Glory.
    //
    // Vanilla updateResult bails out for an F&G + F&G or F&G + vanilla
    // crossbow combine because the two stacks aren't the same Item and the
    // right side isn't a registered repair material. We intercept at HEAD so
    // the player can repair / re-enchant F&G without losing its custom
    // components (CRUSADERS_VOLLEY, IS_FAMSPECIAL_GEAR, custom name, lore).
    //
    // Allowed pairs:
    //   - F&G + F&G              → result F&G
    //   - F&G + vanilla CROSSBOW → result F&G
    //   - vanilla CROSSBOW + F&G → result F&G
    // Anything else (vanilla CROSSBOW + vanilla CROSSBOW, modded crossbows,
    // books, dirt, ...) falls through to vanilla logic by returning early
    // WITHOUT ci.cancel(). The scope is intentionally vanilla-only: we use
    // an exact Items.CROSSBOW check rather than `instanceof CrossbowItem` so
    // modded crossbows go through their own merge path.
    //
    // The result starts as `fgSource.copy()` so all F&G data components
    // survive. Durability and enchantment merges follow vanilla math. The
    // enchantment union runs through Enchantment#isAcceptableItem against the
    // result stack, which naturally rejects Multishot — the
    // famspecial:multishot_eligible tag (Multishot.supported_items) only
    // contains minecraft:crossbow, and F&G isn't in the tag. A post-pass also
    // prunes the seed-from-left entries to catch Multishot-on-left + F&G-on-
    // right. Quick Charge is force-floored to IV so the F&G baked-in level is
    // never lost on a merge.
    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void famspecial$mergeFortuneAndGlory(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);
        if (left.isEmpty() || right.isEmpty()) return;

        boolean leftIsFg = left.getItem() instanceof FortuneGloryItem;
        boolean rightIsFg = right.getItem() instanceof FortuneGloryItem;
        // At least one side must be F&G; otherwise this is not our path.
        if (!leftIsFg && !rightIsFg) return;
        // Each side must be either F&G or an exact vanilla crossbow.
        boolean leftIsVanillaXbow = left.isOf(Items.CROSSBOW);
        boolean rightIsVanillaXbow = right.isOf(Items.CROSSBOW);
        if (!(leftIsFg || leftIsVanillaXbow)) return;
        if (!(rightIsFg || rightIsVanillaXbow)) return;

        ItemStack fgSource = leftIsFg ? left : right;
        ItemStack result = fgSource.copy();

        int cost = 0;
        int renameCost = 0;

        long priorWorkSum = (long) left.getOrDefault(DataComponentTypes.REPAIR_COST, 0)
                + (long) right.getOrDefault(DataComponentTypes.REPAIR_COST, 0);

        // Durability merge — vanilla math: right contributes its remaining
        // durability plus 12% of result's max damage as a "repair bonus".
        int resultMaxDmg = result.getMaxDamage();
        int leftRem = left.getMaxDamage() - left.getDamage();
        int rightRem = right.getMaxDamage() - right.getDamage();
        int repairBonus = rightRem + resultMaxDmg * 12 / 100;
        int totalRepair = leftRem + repairBonus;
        int newDamage = Math.max(0, resultMaxDmg - totalRepair);
        if (newDamage < result.getDamage()) {
            result.setDamage(newDamage);
            cost += 2;
        }

        // Enchantment union — seed from left, fold right's entries in with
        // vanilla's same-enchant-bumps-level rule.
        ItemEnchantmentsComponent leftEnch = EnchantmentHelper.getEnchantments(left);
        ItemEnchantmentsComponent rightEnch = EnchantmentHelper.getEnchantments(right);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(leftEnch);

        // Pre-pass: prune seed-from-left entries that aren't acceptable on
        // the result. Must run BEFORE the right-merge loop, otherwise an
        // ineligible left-seed entry (e.g. Multishot from a vanilla crossbow
        // in slot 0) would still be in the builder during the canBeCombined
        // checks below — and would cause a mutually-exclusive right-side
        // enchant (Piercing vs Multishot) to be falsely rejected. Builder.set
        // with level <= 0 removes the entry in 1.21.11 mappings.
        List<RegistryEntry<Enchantment>> toRemove = new ArrayList<>();
        for (RegistryEntry<Enchantment> key : builder.getEnchantments()) {
            if (!key.value().isAcceptableItem(result)) toRemove.add(key);
        }
        for (RegistryEntry<Enchantment> key : toRemove) builder.set(key, 0);

        boolean appliedAny = false;
        boolean rejectedAny = false;
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : rightEnch.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> key = entry.getKey();
            int existing = builder.getLevel(key);
            int incoming = entry.getIntValue();
            int merged = (existing == incoming) ? incoming + 1 : Math.max(incoming, existing);
            Enchantment ench = key.value();

            // Eligibility on the result stack — this is what prunes Multishot:
            // F&G isn't in the multishot_eligible tag.
            boolean acceptable = ench.isAcceptableItem(result);
            // Also enforce pairwise compatibility against everything already
            // in the builder. Skip the matching-key case because canBeCombined
            // returns false when first.equals(second) — that's the merge
            // target, not an exclusivity collision.
            if (acceptable) {
                for (RegistryEntry<Enchantment> existingKey : builder.getEnchantments()) {
                    if (!existingKey.equals(key) && !Enchantment.canBeCombined(key, existingKey)) {
                        acceptable = false;
                        break;
                    }
                }
            }
            if (!acceptable) {
                rejectedAny = true;
                continue;
            }

            appliedAny = true;
            if (merged > ench.getMaxLevel()) merged = ench.getMaxLevel();
            builder.set(key, merged);
            cost += ench.getAnvilCost() * merged;
        }

        // Quick Charge IV floor — F&G is built with QC IV baked in
        // (RaidersLegacyItems#buildFortuneAndGlory). Source the QC
        // RegistryEntry from fgSource's enchantments to avoid a registry
        // lookup. If neither input had QC at all, nothing to source from —
        // accept the loss rather than fabricating an entry.
        RegistryEntry<Enchantment> qcEntry = null;
        for (RegistryEntry<Enchantment> e : builder.getEnchantments()) {
            if (e.matchesKey(Enchantments.QUICK_CHARGE)) {
                qcEntry = e;
                break;
            }
        }
        if (qcEntry == null) {
            for (RegistryEntry<Enchantment> e : EnchantmentHelper.getEnchantments(fgSource).getEnchantments()) {
                if (e.matchesKey(Enchantments.QUICK_CHARGE)) {
                    builder.set(e, FG_BAKED_QUICK_CHARGE_LEVEL);
                    break;
                }
            }
        } else if (builder.getLevel(qcEntry) < FG_BAKED_QUICK_CHARGE_LEVEL) {
            builder.set(qcEntry, FG_BAKED_QUICK_CHARGE_LEVEL);
        }

        // Rename — vanilla's three branches.
        if (this.newItemName != null && !this.newItemName.isBlank()) {
            if (!this.newItemName.equals(left.getName().getString())) {
                renameCost = 1;
                cost += renameCost;
                result.set(DataComponentTypes.CUSTOM_NAME, Text.literal(this.newItemName));
            }
        } else if (left.contains(DataComponentTypes.CUSTOM_NAME)) {
            renameCost = 1;
            cost += renameCost;
            result.remove(DataComponentTypes.CUSTOM_NAME);
        }

        // All-rejected branch: every right-side enchant got rejected and
        // nothing else moved the cost. Treat as a no-op rather than charging
        // the player for nothing.
        if (rejectedAny && !appliedAny && cost == 0) {
            this.output.setStack(0, ItemStack.EMPTY);
            this.levelCost.set(0);
            this.sendContentUpdates();
            ci.cancel();
            return;
        }

        EnchantmentHelper.set(result, builder.build());

        // No-op branch — nothing changed at all.
        if (cost <= 0) {
            this.output.setStack(0, ItemStack.EMPTY);
            this.levelCost.set(0);
            this.sendContentUpdates();
            ci.cancel();
            return;
        }

        int finalCost = (int) MathHelper.clamp(priorWorkSum + cost, 0L, (long) Integer.MAX_VALUE);

        // 40-level "Too Expensive!" cap (vanilla parity). Mirror vanilla's
        // canTakeOutput gate: in survival, hand the player an empty output
        // slot but keep the level cost on display so they see why.
        if (finalCost >= 40 && !this.player.isInCreativeMode()) {
            this.output.setStack(0, ItemStack.EMPTY);
            this.levelCost.set(finalCost);
            this.sendContentUpdates();
            ci.cancel();
            return;
        }

        // RepairCost bump — vanilla's 2*max+1 — but skip for pure-rename
        // (renameCost == cost && renameCost > 0) so a rename doesn't keep
        // doubling the prior-work penalty.
        int newRC = Math.max(
                left.getOrDefault(DataComponentTypes.REPAIR_COST, 0),
                right.getOrDefault(DataComponentTypes.REPAIR_COST, 0));
        boolean pureRename = renameCost > 0 && renameCost == cost;
        if (!pureRename) {
            newRC = newRC * 2 + 1;
        }
        result.set(DataComponentTypes.REPAIR_COST, newRC);

        this.output.setStack(0, result);
        this.levelCost.set(finalCost);
        this.sendContentUpdates();
        ci.cancel();
    }
}