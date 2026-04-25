package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        if (left.isEmpty() || right.isEmpty()) {
            return;
        }
        if (left.getItem() != Items.DIAMOND_CHESTPLATE) {
            return;
        }
        if (!MERGE_ELIGIBLE_SETS.contains(left.get(ModComponents.SET_ID))) {
            return;
        }
        if (!Boolean.TRUE.equals(left.get(ModComponents.IS_FAMSPECIAL_GEAR))) {
            return;
        }
        if (!right.isOf(Items.ELYTRA)) {
            return;
        }
        if (right.get(ModComponents.SET_ID) != null) {
            return;
        }

        ItemStack merged = famspecial$buildMergedElytra(left, right);
        this.output.setStack(0, merged);
        FamSpecial.LOGGER.info("Merged {} chestplate into elytra; result SET_ID={}",
                left.get(ModComponents.SET_ID), merged.get(ModComponents.SET_ID));
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
}
