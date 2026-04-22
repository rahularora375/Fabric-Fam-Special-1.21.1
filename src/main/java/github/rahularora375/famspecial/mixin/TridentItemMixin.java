package github.rahularora375.famspecial.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import github.rahularora375.famspecial.item.ArmorEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Riptide bypass for the Poseidon set. Vanilla TridentItem refuses to launch
// riptide unless the player is in water or rain (two call sites: `use` gates
// the start-hold, `onStoppedUsing` gates the release). When the wielder has
// all four SET_ID="poseidon" pieces equipped AND is wielding the trident in
// the main hand, short-circuit the vanilla water/rain check to return true
// so riptide fires anywhere — this is the Poseidon 4/4 set bonus. Off-hand
// use falls back to vanilla behavior (still needs water/rain).
//
// INVOKE target owner is PlayerEntity (not Entity) — javac bakes the
// compile-time static receiver type into the INVOKEVIRTUAL instruction, and
// both vanilla call sites use a PlayerEntity-typed reference.
@Mixin(TridentItem.class)
public abstract class TridentItemMixin {

    @Redirect(
            method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z")
    )
    private boolean famspecial$riptideOverrideOnUse(PlayerEntity self, @Local(argsOnly = true) Hand hand) {
        if (self.isTouchingWaterOrRain()) return true;
        if (hand != Hand.MAIN_HAND) return false;
        return ArmorEffects.hasFullSet(self, "poseidon");
    }

    @Redirect(
            method = "onStoppedUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z")
    )
    private boolean famspecial$riptideOverrideOnStopped(PlayerEntity self) {
        if (self.isTouchingWaterOrRain()) return true;
        if (self.getActiveHand() != Hand.MAIN_HAND) return false;
        return ArmorEffects.hasFullSet(self, "poseidon");
    }
}
