package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.component.ModComponents;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GrindstoneScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Merged-elytra identity re-stamp only. All other grindstone operations are
// vanilla. When a themed elytra is disenchanted, vanilla grindstone drops our
// custom component bundle, so we re-stamp it onto the output here so
// SET_ID / IS_FAMSPECIAL_GEAR / BLOCKS_MENDING / REGENS_DURABILITY / etc.
// survive. copyModComponentsForElytra excludes INDESTRUCTIBLE by design.
@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin {
    @Shadow
    @Final
    Inventory input;

    @Shadow
    @Final
    private Inventory result;

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void famspecial$restampMergedElytra(CallbackInfo ci) {
        ItemStack input0 = this.input.getStack(0);
        ItemStack input1 = this.input.getStack(1);

        ItemStack result = this.result.getStack(0);
        if (result.isEmpty()) return;

        if (hasSetId(input0) && input0.isOf(Items.ELYTRA)) {
            ModComponents.copyModComponentsForElytra(input0, result);
        } else if (hasSetId(input1) && input1.isOf(Items.ELYTRA)) {
            ModComponents.copyModComponentsForElytra(input1, result);
        }
    }

    private static boolean hasSetId(ItemStack stack) {
        return !stack.isEmpty() && stack.get(ModComponents.SET_ID) != null;
    }
}
