package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.component.ModComponents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

// Elytra-style durability cap. Every damage path in ItemStack
// (damage(int, LivingEntity, EquipmentSlot) for armor wear, damage(int, Hand)
// for weapon wear, etc.) funnels through the core overload below, so one
// HEAD-cancellable inject covers all of them. For stacks flagged
// INDESTRUCTIBLE we clamp the damage so getDamage() never exceeds
// getMaxDamage() - 1 — mirrors vanilla's willBreakNextUse() gate that
// elytra uses to stop gliding (and thus stop taking damage) one point
// before destruction.
//
// Behaviour per incoming amount (always cancels vanilla for INDESTRUCTIBLE
// stacks — we fully own the damage-bookkeeping path for them):
//   - already at or past (maxDamage - 1): no-op cancel.
//   - otherwise: setDamage to min(before + amount, cap) ourselves.
//
// Always-cancel is load-bearing for the break-sound fire. Vanilla emits the
// break sound via LivingEntity#sendEquipmentBreakStatus, which is gated on
// damage >= maxDamage — i.e. past the cap, which our clamp prevents from
// ever happening. If we let vanilla handle the sub-cap path, the common
// "incremental 1-point armor-wear tick from cap-1 to cap" transition would
// silently sit at the cap with no audible break. Handling setDamage
// ourselves lets us detect the before < cap → newDamage == cap transition
// regardless of whether the damage call arrived as a 1-point tick or as a
// single overflow hit, and fire famspecial$onBrokenTransition exactly once.
//
// On that fresh transition we (a) play ENTITY_ITEM_BREAK once at the player's
// position so the break is audible even though vanilla's path never fires,
// and (b) force an equipment refresh on whichever armor slot is currently
// holding this stack so the entity's cached AttributeContainer drops the
// broken piece's modifiers — see ItemStack#applyAttributeModifiers inject
// for the broken-state gate those modifiers are now filtered through.
//
// By the time this mixin runs, Unbreaking's damage reduction has already
// been applied upstream (calculateDamage runs in the caller overload
// LivingEntity#damageEquipment → ItemStack#damage(int, LivingEntity, slot)),
// so always-cancel doesn't lose any enchant behavior.
//
// The core method runs only on the server (ServerWorld is required), so
// client-side cosmetics don't need to duplicate this logic.
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(
            method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void famspecial$clampIndestructibleDamage(
            int amount,
            ServerWorld world,
            @Nullable ServerPlayerEntity player,
            Consumer<Item> breakCallback,
            CallbackInfo ci
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (!Boolean.TRUE.equals(self.get(ModComponents.INDESTRUCTIBLE))) return;
        if (!self.isDamageable()) return;

        int cap = self.getMaxDamage() - 1;
        int before = self.getDamage();
        if (before >= cap) {
            // Already at the cap — swallow the damage call entirely.
            ci.cancel();
            return;
        }
        int newDamage = Math.min(before + amount, cap);
        self.setDamage(newDamage);
        if (newDamage >= cap) {
            // Just crossed into the broken state — fire once.
            famspecial$onBrokenTransition(self, world, player);
        }
        ci.cancel();
    }

    // Fires once per piece when damage crosses from below-cap to at-cap.
    // Plays the vanilla break sound (since the break callback was skipped)
    // and forces an equipment refresh on the owning slot so the entity's
    // AttributeContainer drops the now-gated attribute modifiers.
    private static void famspecial$onBrokenTransition(
            ItemStack stack,
            ServerWorld world,
            @Nullable ServerPlayerEntity player
    ) {
        if (player == null) return;

        // Null source so the player themselves also hears it.
        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ITEM_BREAK,
                SoundCategory.PLAYERS,
                0.8f,
                0.8f + world.getRandom().nextFloat() * 0.4f
        );

        // The damage(...) call doesn't carry slot info directly, so walk the
        // four armor slots to find the one holding this stack instance.
        // Four reference checks is cheap. equipStack routes through
        // LivingEntity#onEquipStack, which recomputes the slot's attribute
        // modifiers via removeModifiers(old) / addTemporaryModifiers(new) —
        // with applyAttributeModifiers now HEAD-cancelling for broken
        // INDESTRUCTIBLE stacks, the re-add is a no-op and the cached
        // modifiers are cleared.
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            if (player.getEquippedStack(slot) == stack) {
                ((LivingEntity) player).equipStack(slot, stack);
                return;
            }
        }
    }
}
