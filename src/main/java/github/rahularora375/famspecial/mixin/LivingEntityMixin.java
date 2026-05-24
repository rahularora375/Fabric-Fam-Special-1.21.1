package github.rahularora375.famspecial.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import github.rahularora375.famspecial.item.TechnobladeSave;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
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

    // Shardbearing chip magnitude: 10% of the target's max HP, added on
    // top of the post-mitigation damage. No PvE/PvP split — the chip is
    // uniform against players and mobs alike.
    private static final float OATHBRINGER_HP_DAMAGE_FRACTION = 0.10F;

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

    // Shardbearing max-HP chip (Oathbringer gameplay): when the attacker is
    // a LivingEntity holding a stack flagged GRANTS_SHARDBEARING in their main
    // hand, add OATHBRINGER_HP_DAMAGE_FRACTION (10%) of the target's max HP
    // as bonus damage. Hooks modifyAppliedDamage rather than applyArmorToDamage
    // so the bonus lands AFTER vanilla armor/protection/resistance math — it
    // bypasses mitigation the same way the previous armor-pierce did, but
    // always contributes something on unarmored mobs (where piercing had
    // nothing to pierce). self.getMaxHealth() is the target's max HP, so the
    // chip magnitude is constant per target regardless of remaining HP.
    // Non-attacker damage (lava/fall/etc.) and non-LivingEntity attackers
    // short-circuit. Projectile/indirect hits only chip if the attacker is
    // STILL holding Oathbringer at impact time — the convention used
    // elsewhere in this repo.
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
        return original + self.getMaxHealth() * OATHBRINGER_HP_DAMAGE_FRACTION;
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

    @ModifyReturnValue(
            method = "modifyAppliedDamage",
            at = @At("RETURN")
    )
    private float famspecial$technobladeFallImmunity(float original, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity)) return original;
        if (!self.hasStatusEffect(ModStatusEffects.TECHNOBLADE_FEATHERWEIGHT)) return original;
        if (!source.isOf(DamageTypes.FALL)) return original;
        return 0.0f;
    }

    // Technoblade totem-save: when an incoming hit would otherwise drop the
    // wearer to 0 HP, consume the Raider 4/4 30-min cooldown and clamp damage
    // so the wearer survives at 1 HP with the vanilla totem effect bundle.
    // DECLARATION ORDER MATTERS: this @ModifyReturnValue MUST be declared
    // AFTER famspecial$technobladeFallImmunity. Mixin chains return-value
    // injectors in declaration order, so this injector receives the
    // post-fall-zero return value — which means a fatal fall has already
    // been zeroed out by the time we test (original >= totalHP) and the
    // save won't burn its cooldown on a fall the previous injector already
    // negated.
    @ModifyReturnValue(method = "modifyAppliedDamage", at = @At("RETURN"))
    private float famspecial$technobladeTotemSave(float original, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return original;
        float totalHP = self.getHealth() + self.getAbsorptionAmount();
        if (original < totalHP) return original;
        if (!TechnobladeSave.tryConsume(player, source)) return original;
        return totalHP - 1.0f;
    }
}
