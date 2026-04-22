package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class AttackHandlers {
    // Sage's Grace tuning. 1 heart on a normal hit, double on a crit —
    // mirrors vanilla's 1.5× crit damage multiplier in spirit (rounded up
    // to 2× since we're healing on integer half-heart boundaries).
    private static final float HEAL_AMOUNT = 2.0f;
    private static final float CRIT_HEAL_AMOUNT = 4.0f;
    private static final int HEART_PARTICLE_COUNT = 8;
    // 3× vanilla sword wear (sword = 1/hit). With Mending + Unbreaking 5
    // this still feels effectively infinite on a mob farm, but swings
    // without XP exposure meaningfully chew durability.
    private static final int DURABILITY_COST = 3;

    // Fire Serpent 4/4 set-bonus payload: 14s of Messmer's Venom (amp 0 →
    // ticks every 25 game-ticks, ~11 ticks total if the duration runs out
    // uninterrupted). Every melee hit refreshes the duration.
    private static final int MESSMERS_VENOM_DURATION_TICKS = 14 * 20;

    // Thriller's Edge payload: 4s of Wither II. Amp 1 for Wither II.
    private static final int WITHER_ON_HIT_DURATION_TICKS = 4 * 20;
    private static final int WITHER_ON_HIT_AMPLIFIER = 1;

    public static void register() {
        // Messmer's Venom propagation. Fires for any successful damage a
        // player deals where the weapon is tagged #minecraft:spears — covers
        // melee stabs, lunge dashes, and thrown spear hits (DamageSource
        // carries the spear stack on all three paths). AttackEntityCallback
        // only fires on left-click melee through PlayerEntity.attack, so it
        // would miss lunge + thrown-spear hits; ALLOW_DAMAGE catches them all
        // because every damage path funnels through LivingEntity.damage. We
        // always return true — this isn't a cancellation, just a pre-damage
        // hook. Gating on the weapon (not the attacker's set) keeps the
        // mechanic tied to "you hit them with a spear while poisoned" rather
        // than any swing while the effect is up, and any future spear — modded
        // or vanilla — inherits the behavior for free.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
            Entity attacker = source.getAttacker();
            ItemStack weapon = source.getWeaponStack();
            if (attacker instanceof PlayerEntity player
                    && victim != attacker
                    && player.hasStatusEffect(ModStatusEffects.MESSMERS_VENOM)
                    && weapon != null
                    && weapon.isIn(ItemTags.SPEARS)) {
                victim.addStatusEffect(new StatusEffectInstance(
                        ModStatusEffects.MESSMERS_VENOM,
                        MESSMERS_VENOM_DURATION_TICKS, 0,
                        false, true, true), player);
            }

            // Thriller's Edge Wither-on-hit propagation. Additive — runs
            // alongside the venom branch, no mutual exclusion. Main-hand only:
            // we check the attacker's main-hand stack directly rather than
            // source.getWeaponStack(), so an off-hand flagged stack does not
            // propagate Wither. Matches the axe's AttributeModifierSlot.MAINHAND
            // gating on its attack-speed modifier — both only apply in main hand.
            if (attacker instanceof PlayerEntity witherPlayer
                    && victim != attacker
                    && Boolean.TRUE.equals(witherPlayer.getMainHandStack().get(ModComponents.APPLIES_WITHER_ON_HIT))) {
                victim.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WITHER,
                        WITHER_ON_HIT_DURATION_TICKS, WITHER_ON_HIT_AMPLIFIER,
                        false, true, true), witherPlayer);
            }
            return true;
        });

        // Fires on every player left-click against an entity, BEFORE vanilla
        // damage resolution. Returning SUCCESS cancels damage/knockback/sweep/
        // durability for this swing on both sides — which is also why sweep
        // never fires on Sage's Grace: vanilla's doSweepingAttack runs inside
        // PlayerEntity.attack after this callback, and SUCCESS short-circuits
        // the whole attack method. Sweep-hit entities don't re-trigger this
        // callback either, so nearby mobs are never healed.
        // Side effects (heal, sounds, particles) are server-only to avoid
        // desync and double-broadcast.
        AttackEntityCallback.EVENT.register((player, world, hand, target, hitResult) -> {
            // Main-hand only. Vanilla only ever passes MAIN_HAND here, but this
            // keeps the intent explicit against future / modded code paths.
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            if (!Boolean.TRUE.equals(player.getMainHandStack().get(ModComponents.HEALS_TARGET))) {
                return ActionResult.PASS;
            }

            // Replicate PlayerEntity.isCriticalHit (private in vanilla) + the
            // cooldown gate from PlayerEntity.attack: bl = g > 0.9F.
            float cooldownProgress = player.getAttackCooldownProgress(0.5F);
            boolean cooldownPassed = cooldownProgress > 0.9F;
            boolean isCrit = cooldownPassed
                    && player.fallDistance > 0.0F
                    && !player.isOnGround()
                    && !player.isClimbing()
                    && !player.isTouchingWater()
                    && !player.hasBlindnessEffect()
                    && !player.hasVehicle()
                    && target instanceof LivingEntity
                    && !player.isSprinting();

            float healAmount = isCrit ? CRIT_HEAL_AMOUNT : HEAL_AMOUNT;
            SoundEvent attackSound = isCrit
                    ? SoundEvents.ENTITY_PLAYER_ATTACK_CRIT
                    : (cooldownPassed ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG
                                      : SoundEvents.ENTITY_PLAYER_ATTACK_WEAK);

            if (world instanceof ServerWorld serverWorld && target instanceof LivingEntity living) {
                living.heal(healAmount);

                double x = living.getX();
                double y = living.getY() + living.getHeight() * 0.75;
                double z = living.getZ();
                serverWorld.spawnParticles(ParticleTypes.HEART,
                        x, y, z,
                        HEART_PARTICLE_COUNT,
                        0.4, 0.4, 0.4,
                        0.0);
                // Vanilla sword hit sound (STRONG/WEAK/CRIT) from the attacker's
                // position, matching PlayerEntity#playAttackSound.
                serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                        attackSound, player.getSoundCategory(), 1.0f, 1.0f);

                // Since we SUCCESS-out before vanilla can apply swing wear,
                // consume durability ourselves. Routed through ItemStack#damage
                // so Unbreaking / Mending / break events all behave normally.
                ItemStack weapon = player.getMainHandStack();
                weapon.damage(DURABILITY_COST, player, EquipmentSlot.MAINHAND);
            }

            // Reset the cooldown bar so the swing reads as committed and the
            // next crit requires the usual jump+wait rhythm. Runs on both
            // sides — client drives the HUD bar, server drives crit gating.
            player.resetTicksSinceLastAttack();
            return ActionResult.SUCCESS;
        });

        FamSpecial.LOGGER.info("Registering attack handlers for {}", FamSpecial.MOD_ID);
    }
}