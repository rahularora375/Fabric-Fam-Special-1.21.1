package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.component.ModComponents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.ForgingSlotsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Anvil would otherwise let players merge enchants onto Sage's Grace (or any
// future HEALS_TARGET item) beyond the fixed Unbreaking/Mending loadout.
// Post-vanilla-update, blank the output whenever either input carries the
// flag. Renames are also blocked — fine, custom names are part of the
// item identity.
//
// Additionally: if the left input carries BLOCKS_MENDING and the right input
// (sacrifice — either an enchanted item or an enchanted book) contains the
// Mending enchant, blank the output so Mending can never land on a flagged
// stack via the anvil. Other enchants on the sacrifice still merge normally
// when the left input isn't BLOCKS_MENDING-flagged.
//
// Extends ForgingScreenHandler (not a @Shadow) because `input` and `output`
// are declared on the parent class — in dev mappings, Mixin can't @Shadow
// inherited fields. The constructor is never invoked; Mixin strips it.
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {
    private AnvilScreenHandlerMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
                                    ScreenHandlerContext context, ForgingSlotsManager slots) {
        super(type, syncId, playerInventory, context, slots);
    }

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void famspecial$blockModifications(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);
        if (hasHealsTarget(left) || hasHealsTarget(right)) {
            this.output.setStack(0, ItemStack.EMPTY);
            return;
        }
        if (blocksMending(left) && stackHasMending(right)) {
            this.output.setStack(0, ItemStack.EMPTY);
        }
    }

    private static boolean hasHealsTarget(ItemStack stack) {
        return !stack.isEmpty() && Boolean.TRUE.equals(stack.get(ModComponents.HEALS_TARGET));
    }

    private static boolean blocksMending(ItemStack stack) {
        return !stack.isEmpty() && Boolean.TRUE.equals(stack.get(ModComponents.BLOCKS_MENDING));
    }

    // True if the stack carries Mending as either a regular enchant
    // (ENCHANTMENTS component) or a stored enchanted-book entry
    // (STORED_ENCHANTMENTS component). Both paths use matchesKey on the
    // registry entry so the lookup is direct.
    private static boolean stackHasMending(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (containsMending(stack.getEnchantments())) return true;
        ItemEnchantmentsComponent stored = stack.getOrDefault(
                DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        return containsMending(stored);
    }

    private static boolean containsMending(ItemEnchantmentsComponent enchantments) {
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(Enchantments.MENDING)) return true;
        }
        return false;
    }
}
