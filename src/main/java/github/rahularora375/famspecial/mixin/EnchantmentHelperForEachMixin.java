package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.component.ModComponents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Broken-state enchant-dispatch gate for INDESTRUCTIBLE stacks.
//
// Sibling to ItemStackEnchantmentsMixin: that one overrides
// ItemStack#getEnchantments() so tooltips and the anvil/grindstone screens
// see "no enchants" on a broken piece. But the vanilla effect-dispatch
// pipeline (Frost Walker, Soul Speed, Thorns, Protection, Efficiency,
// Unbreaking, etc.) does NOT route through getEnchantments() — it routes
// through EnchantmentHelper.forEachEnchantment(...). Both overloads read
// the ENCHANTMENTS component directly off the stack (via
// stack.getOrDefault(...) and stack.get(...) respectively), bypassing the
// getEnchantments() wrapper entirely.
//
// Concretely the dispatch chain for location-based effects is:
//   EnchantmentHelper.applyLocationBasedEffects(ServerWorld, LivingEntity)
//     -> forEachEnchantment(LivingEntity, ContextAwareConsumer)
//       -> for each EquipmentSlot:
//            forEachEnchantment(ItemStack, EquipmentSlot, LivingEntity,
//                               ContextAwareConsumer)  [** reads stack.get(ENCHANTMENTS) **]
// And similarly for onTick, isInvulnerableTo, getProtectionAmount, etc.
// The stack-only variant forEachEnchantment(ItemStack, Consumer) is the
// other entry point — used by getDamage / modifyKnockback / getItemDamage /
// getProjectileCount / etc. — and it reads via stack.getOrDefault(ENCHANTMENTS).
//
// By HEAD-cancelling both forEachEnchantment(ItemStack, ...) overloads when
// the stack is flagged INDESTRUCTIBLE and sitting at its durability cap, we
// guarantee that *no* enchant effect — location, tick, damage, protection,
// anything — fires for a broken piece. Stormlight + the 243-tick durability
// regen are untouched: both read the REGENS_DURABILITY component directly
// (stack.get(ModComponents.REGENS_DURABILITY)), which is a custom data
// component lookup, not an enchant lookup.
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperForEachMixin {

    // Stack-only variant. Hit by: getDamage, modifyKnockback, getItemDamage,
    // getProjectileCount, getFishingLuckBonus, getTridentReturnAcceleration,
    // and the other static EnchantmentHelper math helpers.
    @Inject(
            method = "forEachEnchantment(Lnet/minecraft/item/ItemStack;Lnet/minecraft/enchantment/EnchantmentHelper$Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void famspecial$gateBrokenForEachStack(
            ItemStack stack,
            EnchantmentHelper.Consumer consumer,
            CallbackInfo ci
    ) {
        if (famspecial$isBrokenIndestructible(stack)) {
            ci.cancel();
        }
    }

    // Slot+entity variant. Hit by: applyLocationBasedEffects (Frost Walker,
    // Soul Speed), removeLocationBasedEffects, onTick, isInvulnerableTo,
    // getProtectionAmount, onTargetDamaged, onHitBlock, etc.  The
    // LivingEntity-wide forEachEnchantment(LivingEntity, ContextAwareConsumer)
    // overload iterates the four EquipmentSlots and funnels into this one, so
    // gating here covers the entity-wide dispatch transitively.
    @Inject(
            method = "forEachEnchantment(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/enchantment/EnchantmentHelper$ContextAwareConsumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void famspecial$gateBrokenForEachSlot(
            ItemStack stack,
            EquipmentSlot slot,
            LivingEntity entity,
            EnchantmentHelper.ContextAwareConsumer consumer,
            CallbackInfo ci
    ) {
        if (famspecial$isBrokenIndestructible(stack)) {
            ci.cancel();
        }
    }

    private static boolean famspecial$isBrokenIndestructible(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!Boolean.TRUE.equals(stack.get(ModComponents.INDESTRUCTIBLE))) return false;
        if (!stack.isDamageable()) return false;
        return stack.getDamage() >= stack.getMaxDamage() - 1;
    }
}
