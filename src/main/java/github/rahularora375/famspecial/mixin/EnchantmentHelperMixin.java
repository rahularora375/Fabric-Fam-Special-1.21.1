package github.rahularora375.famspecial.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import github.rahularora375.famspecial.component.ModComponents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

// Blocks Mending from ever rolling onto a BLOCKS_MENDING stack via the
// enchanting table. getPossibleEntries is the single chokepoint that the
// table's roll (and EnchantmentHelper#enchantAndGetEntries) reads from, so
// filtering its return value is sufficient — no need to hook the table
// screen handler itself.
//
// The stack argument is the second parameter; MixinExtras' @Local pulls it
// out by type. Returning a filtered copy keeps the contract with vanilla
// (a mutable ArrayList) intact.
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @ModifyReturnValue(
            method = "getPossibleEntries(ILnet/minecraft/item/ItemStack;Ljava/util/stream/Stream;)Ljava/util/List;",
            at = @At("RETURN")
    )
    private static List<EnchantmentLevelEntry> famspecial$filterMending(
            List<EnchantmentLevelEntry> original,
            @Local(argsOnly = true) ItemStack stack
    ) {
        if (!Boolean.TRUE.equals(stack.get(ModComponents.BLOCKS_MENDING))) return original;
        original.removeIf(entry -> entry.enchantment().matchesKey(Enchantments.MENDING));
        return original;
    }
}
