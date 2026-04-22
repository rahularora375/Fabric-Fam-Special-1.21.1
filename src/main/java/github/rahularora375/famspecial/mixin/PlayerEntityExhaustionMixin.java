package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.effect.ModStatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Shuriman Endurance saturation-loss suppression (Shurima 4/4 set gameplay).
// Vanilla PlayerEntity#addExhaustion(float) funnels every saturation-drain
// path (sprint, jump, attack, regen-from-food, block break, mob damage, etc.)
// through this single method. When the player has SHURIMAN_ENDURANCE,
// cancel the call at HEAD so no exhaustion is added — the saturation bar
// never decreases. The effect is applied with a 4-minute apply-once duration
// by ArmorEffects so the buff persists anywhere the player goes once earned,
// beacon-style.
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityExhaustionMixin {

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void famspecial$skipExhaustionForShurimanEndurance(float exhaustion, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self.hasStatusEffect(ModStatusEffects.SHURIMAN_ENDURANCE)) {
            ci.cancel();
        }
    }
}
