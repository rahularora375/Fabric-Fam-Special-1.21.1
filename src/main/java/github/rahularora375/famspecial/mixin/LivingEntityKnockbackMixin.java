package github.rahularora375.famspecial.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Emperor's Divide knockback-resistance bypass (Shurima Sun Disc Spear
// gameplay). Vanilla LivingEntity#takeKnockback(double, double, double) scales
// the incoming knockback strength by `1 - KNOCKBACK_RESISTANCE` on the
// receiver's attribute. If the attacker is a LivingEntity with
// EMPERORS_DIVIDE, return 0.0 from the attribute lookup so the multiplier
// becomes (1 - 0) = 1 and knockback lands at full strength regardless of
// how stiff the target is.
//
// Hook: @ModifyExpressionValue on the INVOKEVIRTUAL of getAttributeValue
// inside takeKnockback. Verified against yarn 1.21.11+build.4 — the only
// getAttributeValue call in takeKnockback reads KNOCKBACK_RESISTANCE.
@Mixin(LivingEntity.class)
public abstract class LivingEntityKnockbackMixin {

    @ModifyExpressionValue(
            method = "takeKnockback(DDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D")
    )
    private double famspecial$bypassKnockbackResistance(double original) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity attacker = self.getAttacker();
        if (attacker instanceof LivingEntity livingAttacker
                && livingAttacker.hasStatusEffect(ModStatusEffects.EMPERORS_DIVIDE)) {
            return 0.0;
        }
        return original;
    }
}
