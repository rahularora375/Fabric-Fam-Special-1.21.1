package github.rahularora375.famspecial.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.rahularora375.famspecial.component.ModComponents;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Broken-state enchant gate for INDESTRUCTIBLE stacks. All enchant effect
// paths (damage, protection, efficiency, etc.) read through
// ItemStack#getEnchantments, so replacing its return value with the empty
// ItemEnchantmentsComponent.DEFAULT while the stack is at its durability cap
// zeros out every enchant benefit at the source.
//
// Side effect (intended): tooltips + anvil/grindstone screens will also see
// "no enchants" on a broken piece. Acceptable — the broken state is
// transient (regen heals it back within ~12s), and the visual signal is
// coherent with the gameplay ("this piece is dead right now").
@Mixin(ItemStack.class)
public abstract class ItemStackEnchantmentsMixin {

    @ModifyReturnValue(
            method = "getEnchantments()Lnet/minecraft/component/type/ItemEnchantmentsComponent;",
            at = @At("RETURN")
    )
    private ItemEnchantmentsComponent famspecial$gateBrokenEnchantments(ItemEnchantmentsComponent original) {
        ItemStack self = (ItemStack) (Object) this;
        if (!Boolean.TRUE.equals(self.get(ModComponents.INDESTRUCTIBLE))) return original;
        if (!self.isDamageable()) return original;
        if (self.getDamage() >= self.getMaxDamage() - 1) {
            return ItemEnchantmentsComponent.DEFAULT;
        }
        return original;
    }
}
