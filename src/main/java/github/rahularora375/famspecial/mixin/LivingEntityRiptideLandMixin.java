package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.effect.ModStatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Asgardian's Flight (Thor 5/5) post-riptide swing-input fix. Vanilla
// LivingEntity#tickRiptide only clears riptideTicks on (a) hitting a
// LivingEntity in the spin path or (b) horizontalCollision — never on a
// pure vertical landing. So a straight-up/straight-down Mjolnir launch
// keeps USING_RIPTIDE_FLAG (and EntityPose.SPIN_ATTACK) set for the full
// 20-tick riptide window after touchdown, which suppresses the player's
// swing input — the next left-click is silently eaten until the timer
// elapses. This mixin sets riptideTicks=0 at HEAD when the player is on
// ground and currently has ASGARDIANS_FLIGHT, so the same-tick tail of
// tickRiptide (which gates the FLAG clear on riptideTicks <= 0) releases
// the spin-attack pose immediately and the next swing lands.
//
// Gated on ASGARDIANS_FLIGHT to avoid affecting vanilla tridents and the
// Poseidon dry-land riptide (TridentItemMixin) — Poseidon wearers never
// carry that effect, so their existing horizontalCollision-only clear
// behavior is preserved.
@Mixin(LivingEntity.class)
public abstract class LivingEntityRiptideLandMixin {

    @Shadow
    protected int riptideTicks;

    @Inject(
            method = "tickRiptide(Lnet/minecraft/util/math/Box;Lnet/minecraft/util/math/Box;)V",
            at = @At("HEAD")
    )
    private void famspecial$clearRiptideOnLanding(Box currentBox, Box nextBox, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!player.hasStatusEffect(ModStatusEffects.ASGARDIANS_FLIGHT)) return;
        if (!player.isOnGround()) return;
        this.riptideTicks = 0;
    }
}
