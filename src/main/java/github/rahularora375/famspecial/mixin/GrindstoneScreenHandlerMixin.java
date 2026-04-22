package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.component.ModComponents;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Grindstone would otherwise strip our fixed Unbreaking/Mending off Sage's
// Grace (and disenchant any future HEALS_TARGET weapons). After vanilla
// builds the grind output, overwrite it with EMPTY whenever an input slot
// carries the flag — simpler than HEAD-cancelling since the content-update
// broadcast already ran with whatever state we leave behind.
@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin {
    @Shadow
    @Final
    Inventory input;

    @Shadow
    @Final
    private Inventory result;

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void famspecial$blockHealsTarget(CallbackInfo ci) {
        if (hasHealsTarget(this.input.getStack(0)) || hasHealsTarget(this.input.getStack(1))) {
            this.result.setStack(0, ItemStack.EMPTY);
        }
    }

    private static boolean hasHealsTarget(ItemStack stack) {
        return !stack.isEmpty() && Boolean.TRUE.equals(stack.get(ModComponents.HEALS_TARGET));
    }
}