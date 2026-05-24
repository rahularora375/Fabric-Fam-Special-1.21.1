package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    // Fire Serpent 4/4 set-bonus payload: Messmer's Venom (amp 0 → ticks
    // every 25 game-ticks at 5.0 magic damage = 2.5 hearts, lethal).
    //
    // 124 ticks (6.2 s) is intentional. The canApplyUpdateEffect modulo
    // (`duration % 25 == 0`) fires whenever duration aligns to 25, so a
    // 100-tick (5 s) duration would tick on application — landing the
    // first hit inside the attack's 10-tick i-frame window and getting
    // partially absorbed (the 19-vs-20 damage variance). 124 is the
    // smallest non-25-multiple that still leaves room for 4 hits: the
    // duration decrements past 124 → 100 (1st hit) → 75 → 50 → 25 →
    // expires, so all four hits land cleanly outside the application's
    // i-frame window for a deterministic 20 HP total. Every fresh hit
    // refreshes the duration.
    private static final int MESSMERS_VENOM_DURATION_TICKS = 124;

    // Thriller's Edge payload: Wither IV, 8s vs mobs / 2s vs players; mob
    // kills via wither tick drop a wither rose. Amp 3 for Wither IV (amp 0
    // = I, 1 = II, 2 = III, 3 = IV). PvE/PvP duration split mirrors the
    // Messmer's Venom precedent above — heavy on mobs, restrained on
    // players to keep PvP non-degenerate.
    private static final int WITHER_ON_HIT_DURATION_TICKS_PVE = 8 * 20;
    private static final int WITHER_ON_HIT_DURATION_TICKS_PVP = 2 * 20;
    private static final int WITHER_ON_HIT_AMPLIFIER = 3;

    // UUIDs of mobs (never players) currently wither-tagged by Thriller's
    // Edge. Used by the AFTER_DEATH hook to drop a wither rose when a
    // tagged mob is killed by wither-tick damage. Skip players on apply so
    // PvP wither kills don't drop roses. Cleaned up on rose drop (one rose
    // per death) and by a 200-tick sweep against currently-loaded entities.
    private static final Set<UUID> WITHER_TAGGED_MOBS = ConcurrentHashMap.newKeySet();

    public static void register() {
        // Messmer's Venom propagation. Fires for any successful damage a
        // player wearing the full Fire Serpent set deals to anything other
        // than themselves — weapon-agnostic, so fists, sword, axe, bow/arrow,
        // snowball, anything carries the venom. ALLOW_DAMAGE catches every
        // path because every damage path funnels through LivingEntity.damage
        // (AttackEntityCallback would miss lunges and projectile hits). We
        // always return true — this isn't a cancellation, just a pre-damage
        // hook. Set membership is read indirectly through the aura
        // (MESSMERS_VENOM is granted by ArmorEffects' fire_serpent_full_set
        // bonus while 4/4 is worn), so the hasStatusEffect check IS the
        // full-set check.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
            Entity attacker = source.getAttacker();
            ItemStack weapon = source.getWeaponStack();
            if (attacker instanceof PlayerEntity player
                    && victim != attacker
                    && player.hasStatusEffect(ModStatusEffects.MESSMERS_VENOM)) {
                victim.addStatusEffect(new StatusEffectInstance(
                        ModStatusEffects.MESSMERS_VENOM,
                        MESSMERS_VENOM_DURATION_TICKS, 0,
                        false, true, true), player);
            }

            // Thriller's Edge Wither-on-hit propagation: Wither IV, 8s vs
            // mobs / 2s vs players. Mob kills via wither tick drop a wither
            // rose (see AFTER_DEATH hook below). Additive — runs alongside
            // the venom branch, no mutual exclusion. Main-hand only: we
            // check the attacker's main-hand stack directly rather than
            // source.getWeaponStack(), so an off-hand flagged stack does not
            // propagate Wither. Matches the axe's AttributeModifierSlot.MAINHAND
            // gating on its attack-speed modifier — both only apply in main hand.
            if (attacker instanceof PlayerEntity witherPlayer
                    && victim != attacker
                    && Boolean.TRUE.equals(witherPlayer.getMainHandStack().get(ModComponents.APPLIES_WITHER_ON_HIT))) {
                // PvE/PvP duration split mirrors Messmer's Venom: full
                // pressure on mobs (8 s), restrained on players (2 s).
                boolean victimIsPlayer = victim instanceof PlayerEntity;
                int duration = victimIsPlayer
                        ? WITHER_ON_HIT_DURATION_TICKS_PVP
                        : WITHER_ON_HIT_DURATION_TICKS_PVE;
                victim.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WITHER,
                        duration, WITHER_ON_HIT_AMPLIFIER,
                        false, true, true), witherPlayer);
                // Tag mob victims only — players never drop wither roses.
                if (!victimIsPlayer) {
                    WITHER_TAGGED_MOBS.add(victim.getUuid());
                }
            }
            return true;
        });

        // Thriller's Edge wither rose drop. Fires when a tagged mob dies
        // to wither-tick damage that originated from a hit we tagged.
        // Mirrors BountyHunterKills' Block.dropStack idiom for the drop.
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, damageSource) -> {
            if (!(victim.getEntityWorld() instanceof ServerWorld serverWorld)) return;
            if (victim instanceof PlayerEntity) return;
            if (!damageSource.isOf(DamageTypes.WITHER)) return;
            if (!WITHER_TAGGED_MOBS.remove(victim.getUuid())) return;
            Block.dropStack(serverWorld, victim.getBlockPos(), new ItemStack(Items.WITHER_ROSE));
        });

        // 200-tick (10 s) sweep — wither lasts 160 ticks max on mobs, so a
        // 10 s cadence is generous. Mirrors FortuneGloryItem's tick-handler
        // cleanup pattern (entity-still-loaded check). Despawn / chunk
        // unload paths never fire AFTER_DEATH, so without this the set
        // would leak.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getOverworld().getTime() % 200L != 0L) return;
            if (WITHER_TAGGED_MOBS.isEmpty()) return;
            WITHER_TAGGED_MOBS.removeIf(uuid -> {
                for (ServerWorld world : server.getWorlds()) {
                    if (world.getEntity(uuid) != null) return false;
                }
                return true;
            });
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