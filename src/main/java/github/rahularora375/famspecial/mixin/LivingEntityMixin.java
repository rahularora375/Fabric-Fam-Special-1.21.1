package github.rahularora375.famspecial.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Messmer's Flame Aegis (Fire Serpent 4/4 set bonus): scales incoming fire
// damage to 20% of its vanilla value — acts like a Fire Protection enchantment
// layer, not a Fire Resistance potion. Runs after vanilla's
// resistance/armor/protection math inside modifyAppliedDamage, so the 0.2×
// multiplier is applied on top of whatever reductions the player already has.
//
// Vanilla Fire Resistance short-circuits fire damage to zero earlier in
// LivingEntity#damage (before modifyAppliedDamage is even called), so having
// both effects at once still results in zero damage — no interaction bug.
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyReturnValue(
            method = "modifyAppliedDamage",
            at = @At("RETURN")
    )
    private float famspecial$messmersFlameAegisReduction(float original, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.hasStatusEffect(ModStatusEffects.MESSMERS_FLAME_AEGIS)) return original;
        if (!source.isIn(DamageTypeTags.IS_FIRE)) return original;
        return original * 0.2f;
    }

    // Shardbearing current-HP chip (Oathbringer gameplay): when the attacker is
    // a LivingEntity holding a stack flagged GRANTS_SHARDBEARING in their main
    // hand, add 7% of the target's current HP as bonus damage. Hooks
    // modifyAppliedDamage rather than applyArmorToDamage so the bonus lands
    // AFTER vanilla armor/protection/resistance math — it bypasses mitigation
    // the same way the previous armor-pierce did, but always contributes
    // something on unarmored mobs (where piercing had nothing to pierce).
    // self.getHealth() at this point is the target's HP BEFORE the incoming
    // hit lands, so 7% reflects pre-hit current HP. Non-attacker damage
    // (lava/fall/etc.) and non-LivingEntity attackers short-circuit.
    // Projectile/indirect hits only chip if the attacker is STILL holding
    // Oathbringer at impact time — the convention used elsewhere in this repo.
    @ModifyReturnValue(
            method = "modifyAppliedDamage",
            at = @At("RETURN")
    )
    private float famspecial$shardbearingCurrentHpChip(float original, DamageSource source) {
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof LivingEntity livingAttacker)) return original;
        ItemStack mainHand = livingAttacker.getMainHandStack();
        if (!Boolean.TRUE.equals(mainHand.get(ModComponents.GRANTS_SHARDBEARING))) return original;
        LivingEntity self = (LivingEntity) (Object) this;
        return original + self.getHealth() * 0.07f;
    }

    // Smooth Criminal's Vestment (Necromancer chestplate) grants 60% damage
    // reduction from undead attackers. Fires on modifyAppliedDamage RETURN so
    // the 0.4× multiplier lands after vanilla armor/protection/resistance math
    // — stacks multiplicatively, matching the Messmer's Flame Aegis convention.
    // source.getAttacker() returns the shooter for arrow/projectile sources
    // (confirmed via DamageSources.arrow attribution), so skeleton arrows and
    // wither-skeleton melee are both covered by one branch — no separate
    // projectile path needed.
    @ModifyReturnValue(
            method = "modifyAppliedDamage",
            at = @At("RETURN")
    )
    private float famspecial$undeadResistance(float original, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack chest = self.getEquippedStack(EquipmentSlot.CHEST);
        if (!Boolean.TRUE.equals(chest.get(ModComponents.GRANTS_UNDEAD_RESISTANCE))) return original;
        Entity attacker = source.getAttacker();
        if (attacker == null) return original;
        if (!attacker.getType().isIn(EntityTypeTags.UNDEAD)) return original;
        return original * 0.4f;
    }

    // Sun's Protection (Shurima helmet + desert): while the wearer has the
    // SUNS_PROTECTION effect, incoming damage is scaled to 0.8× (= Resistance I).
    // Hooked on modifyAppliedDamage RETURN so the multiplier lands after
    // vanilla armor/protection/resistance math — stacks multiplicatively,
    // matching the Messmer's Flame Aegis / Undead Resistance convention.
    // The effect is applied by ArmorEffects and gated on the helmet flag
    // + DESERT biome; once applied it decays naturally over its 400-tick
    // duration after removal (not in MOD_MANAGED).
    @ModifyReturnValue(
            method = "modifyAppliedDamage",
            at = @At("RETURN")
    )
    private float famspecial$sunsProtection(float original, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.hasStatusEffect(ModStatusEffects.SUNS_PROTECTION)) return original;
        return original * 0.8f;
    }
}
