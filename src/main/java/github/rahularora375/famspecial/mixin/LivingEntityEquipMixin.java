package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.item.ArmorEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Instant re-dispatch of the full ArmorEffects bonus pass on armor slot
// change. Counterpart to ServerPlayNetworkHandlerMixin (which handles hotbar
// slot-select for mainhand-driven effects). Fires on any equipment change
// route vanilla funnels through LivingEntity#onEquipStack: inventory drag,
// armor-key hotswap, drop, creative swap, /item replace, dispenser-equip,
// ItemStackMixin's broken-piece re-equip.
//
// Design note: rather than hand-rolling per-effect strip checks, we call
// ArmorEffects.refreshBonusesFor(player) — the same method the 80-tick
// tick loop runs. This handles BOTH directions in one call:
//   - APPLY: if the new equipment satisfies a bonus trigger that previously
//     failed, the effect is added immediately (no 4-second swap-in delay
//     for effect-gated gameplay — Sun's Protection, Messmer's Flame Aegis,
//     Messmer's Venom propagation, Asgardian's Flight armor half).
//   - STRIP: if the new equipment no longer satisfies a previously-active
//     bonus, the MOD_MANAGED diff in refreshBonusesFor removes the effect
//     on the spot.
//
// The 80-tick tick loop remains as the eventual-consistency safety net for
// paths that don't funnel through onEquipStack. TAIL-inject so vanilla's
// own equip-side bookkeeping (attribute rebuild, advancements, sound)
// completes before we read the post-change equipment snapshot.
@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipMixin {

    @Inject(method = "onEquipStack", at = @At("TAIL"))
    private void famspecial$refreshBonusesOnEquipChange(
            EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return;
        if (slot != EquipmentSlot.HEAD
                && slot != EquipmentSlot.CHEST
                && slot != EquipmentSlot.LEGS
                && slot != EquipmentSlot.FEET) return;

        ArmorEffects.refreshBonusesFor(player);
    }
}
