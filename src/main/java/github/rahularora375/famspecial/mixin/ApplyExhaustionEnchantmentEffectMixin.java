package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.component.ModComponents;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.entity.ApplyExhaustionEnchantmentEffect;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Suppresses Lunge's POST_PIERCING_ATTACK exhaustion tick for weapons flagged
// with NO_LUNGE_HUNGER (currently Fire Serpent's Wrath). Vanilla lunge.json
// wires ApplyExhaustionEnchantmentEffect to the piercing path; we short-circuit
// on the context stack flag so the dash doesn't cost food while other lunge
// effects (damage, knockback) still apply.
@Mixin(ApplyExhaustionEnchantmentEffect.class)
public abstract class ApplyExhaustionEnchantmentEffectMixin {

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void famspecial$skipExhaustionForFlaggedStack(
            ServerWorld world, int level, EnchantmentEffectContext context,
            Entity user, Vec3d pos, CallbackInfo ci) {
        if (Boolean.TRUE.equals(context.stack().get(ModComponents.NO_LUNGE_HUNGER))) {
            ci.cancel();
        }
    }
}
