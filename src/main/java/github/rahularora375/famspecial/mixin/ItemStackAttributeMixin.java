package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.component.ModComponents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

// Broken-state attribute gate for INDESTRUCTIBLE stacks. When the stack is
// at its durability cap (getDamage() >= getMaxDamage() - 1), cancel the
// per-slot attribute iteration so the BiConsumer never sees the stack's
// ATTRIBUTE_MODIFIERS entries. LivingEntity#updateAttributes reads through
// this method; with the iteration cancelled, the slot contributes no armor,
// no toughness, and no custom modifiers — broken pieces behave as if unworn.
//
// Restored state flips back automatically the next time equipStack runs
// (see ItemStackMixin's broken-transition refresh + ArmorEffects' regen
// block) — nothing to do here on the recovery side.
@Mixin(ItemStack.class)
public abstract class ItemStackAttributeMixin {

    @Inject(
            method = "applyAttributeModifiers(Lnet/minecraft/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void famspecial$gateBrokenAttributes(
            EquipmentSlot slot,
            BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> consumer,
            CallbackInfo ci
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (!Boolean.TRUE.equals(self.get(ModComponents.INDESTRUCTIBLE))) return;
        if (!self.isDamageable()) return;
        if (self.getDamage() >= self.getMaxDamage() - 1) {
            ci.cancel();
        }
    }
}
