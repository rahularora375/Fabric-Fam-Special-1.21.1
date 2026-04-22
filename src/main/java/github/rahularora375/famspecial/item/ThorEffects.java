package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Thor / God of Thunder theme — gameplay hooks. Three mechanics, each gated
// on a different source so they compose independently:
//
//   1. Lightning on Hit (Mjolnir): AFTER_DAMAGE hook. On every successful
//      melee hit from a player whose main-hand carries LIGHTNING_ON_HIT,
//      roll 20% (clear weather) or 100% (thundering) to spawn a vanilla
//      LIGHTNING_BOLT on the victim. Summoner/victim sanity checks mirror
//      NecromancerSummon.
//
//   2. Storm's Awakening (Thunderhelm): AFTER_DEATH hook. On any kill the
//      wearer performs, if the world is NOT already thundering, roll 8%
//      to start a thunderstorm via ServerWorld#setWeather. Per-player
//      24000-tick (one Minecraft day) cooldown. CRITICAL: if it's already
//      thundering the kill is skipped entirely — no roll, no cooldown
//      burn. Cooldown only starts on a *successful* trigger. Matches the
//      "you brought the storm, nature took over" framing.
//
//   3. Mjolnir's Riptide (5/5 + mainhand): UseItemCallback on right-click.
//      When the held Mjolnir + all 4 Thor armor pieces are equipped,
//      launch the player with a Level 3 riptide equivalent — no water,
//      no rain, no enchant required. Mirrors the Poseidon 4/4 dry-land
//      riptide pattern conceptually but uses a direct useRiptide call
//      rather than a mixin on MaceItem (MaceItem doesn't declare 'use'
//      or 'onStoppedUsing' — those are inherited from Item, so a
//      @Mixin(MaceItem.class) on those methods would fail to link).
//      ActionResult.SUCCESS on the callback cancels vanilla right-click
//      behavior for the mace so there's no double-fire.
public final class ThorEffects {
    // Storm's Awakening: flat 8% roll per kill, with a 24000-tick (one Minecraft
    // day) per-player cooldown after a successful trigger.
    // TESTING VALUE — restore before release
    private static final float STORM_TRIGGER_CHANCE = 1.00f;
    private static final long STORM_COOLDOWN_TICKS = 24000L;
    // Thunderstorm duration on trigger: 4800 ticks (~4 minutes real-time).
    // clearDuration = 0 (don't schedule clear weather afterward; vanilla's
    // weather system will tick its own follow-up clear). raining = true,
    // thundering = true — identical to /weather thunder.
    private static final int STORM_THUNDER_DURATION_TICKS = 4800;

    // Lightning on Hit: 20% roll in clear weather, 100% in a thunderstorm
    // (the latter matches the Mjolnir fantasy — "in a storm, every hit
    // crackles"). Rolled in AFTER_DAMAGE so the victim has already absorbed
    // the base swing's damage before the lightning strike is added.
    private static final float LIGHTNING_CHANCE_CLEAR = 0.20f;
    private static final float LIGHTNING_CHANCE_THUNDERING = 1.00f;

    // Per-player last-trigger tick for Storm's Awakening. Mirrors the
    // NecromancerSummon cooldown map pattern.
    private static final Map<UUID, Long> lastStormTriggerTick = new ConcurrentHashMap<>();

    private ThorEffects() {}

    // Public helper — read by ArmorEffects' storms_awakening_helmet_ready
    // bonus so the cosmetic HUD badge only appears while the ability is
    // armed (i.e. NOT within the cooldown window that follows a successful
    // trigger). Matches the NecromancerSummon.isOnCooldown shape 1:1 so the
    // two badge-ready gates feel identical to the armor-effects dispatcher.
    public static boolean isStormCooldown(PlayerEntity player, long worldTime) {
        Long last = lastStormTriggerTick.get(player.getUuid());
        return last != null && worldTime - last < STORM_COOLDOWN_TICKS;
    }

    public static void register() {
        // Mechanic 1: Lightning on Hit (Mjolnir).
        ServerLivingEntityEvents.AFTER_DAMAGE.register(ThorEffects::onAfterDamage);

        // Mechanic 2: Storm's Awakening (Thunderhelm kill-triggered storm).
        ServerLivingEntityEvents.AFTER_DEATH.register(ThorEffects::onAfterDeath);

        // Mechanic 3: Mjolnir's Riptide (5/5 right-click launch) — owned by
        // the MjolnirMaceItem subclass (use/getUseAction/getMaxUseTime/
        // onStoppedUsing). No registration needed here.

        FamSpecial.LOGGER.info("Registering Thor effects for {}", FamSpecial.MOD_ID);
    }

    private static void onAfterDamage(LivingEntity victim, DamageSource source,
                                      float baseDamage, float damageTaken, boolean blocked) {
        // Attacker must be a player whose mainhand carries LIGHTNING_ON_HIT.
        // Gate on mainhand only — off-hand or inventory copies don't trigger
        // the roll. Mirrors the Wither-on-hit gating pattern in AttackHandlers
        // (main-hand component check before any branching work).
        Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof PlayerEntity player)) return;
        if (victim == attackerEntity) return;
        if (!victim.isAlive()) return; // don't spawn lightning on a corpse
        if (!Boolean.TRUE.equals(player.getMainHandStack().get(ModComponents.LIGHTNING_ON_HIT))) return;
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;

        float chance = world.isThundering() ? LIGHTNING_CHANCE_THUNDERING : LIGHTNING_CHANCE_CLEAR;
        if (world.random.nextFloat() >= chance) return;

        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.refreshPositionAfterTeleport(victim.getX(), victim.getY(), victim.getZ());
        if (player instanceof ServerPlayerEntity sp) {
            bolt.setChanneler(sp);
        }
        world.spawnEntity(bolt);
    }

    private static void onAfterDeath(LivingEntity entity, DamageSource source) {
        // Attribute the kill to the attacker player. We use source.getAttacker()
        // rather than entity.getAttacker() because the damage source already
        // resolved the logical attacker (projectile shooter, etc.) at the point
        // the death event fires.
        Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof ServerPlayerEntity player)) return;
        if (entity == attackerEntity) return;

        // Helmet gate — the kill only counts if the player is wearing
        // Thunderhelm. Read the HEAD slot directly rather than the full-set
        // check since Storm's Awakening is a piece-alone mechanic.
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        if (!Boolean.TRUE.equals(helmet.get(ModComponents.TRIGGERS_STORM_AWAKENING))) return;

        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;

        // CRITICAL: if the world is already thundering, skip the kill entirely.
        // No roll, no cooldown burn. The ability's framing is "my kill summoned
        // the storm" — not "I tried to summon during a storm and missed my
        // one-shot." Only a *successful* fresh-storm trigger arms the cooldown.
        if (world.isThundering()) return;

        // Per-player cooldown gate. If within cooldown window, skip silently —
        // the cosmetic STORMS_AWAKENING HUD badge is already hidden by
        // ArmorEffects while this helper returns true, so the player can see
        // "ability not ready."
        if (isStormCooldown(player, world.getTime())) return;

        // Flat 8% roll.
        if (world.random.nextFloat() >= STORM_TRIGGER_CHANCE) return;

        // Trigger the storm. setWeather(clearDuration, weatherDuration, raining,
        // thundering) — we pass clearDuration = 0 so vanilla's internal weather
        // cycle handles the follow-up clear, and weatherDuration =
        // STORM_THUNDER_DURATION_TICKS (4800 ticks ≈ 4 min real-time) for the
        // storm itself. Both raining + thundering must be true for a proper
        // thunderstorm (thundering implies rain, but vanilla's check requires
        // both flags explicitly).
        //
        // Audible "the storm answered" cue: play the vanilla lightning-bolt
        // thunderclap at volume 10000 centered on the player. Vanilla volume
        // caps at 1.0 per-attenuation-step, but a value > 1 extends the
        // audible radius (volume-to-range scaling in SoundSystem), so this
        // reaches every player in the world rather than only ones standing
        // next to the triggerer. Plays BEFORE setWeather so the sky-flash
        // animation lands right on the crack.
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 10000.0F, 0.8F);
        world.setWeather(0, STORM_THUNDER_DURATION_TICKS, true, true);

        // Arm cooldown only on successful trigger. lastStormTriggerTick is
        // read by isStormCooldown which ArmorEffects uses to gate the cosmetic
        // badge on the next refresh cycle.
        long now = world.getTime();
        lastStormTriggerTick.put(player.getUuid(), now);
        // Stamp absolute cooldown-end tick on the worn Thunderhelm so the
        // client tooltip can render the MM:SS countdown. Server keeps using
        // its own per-UUID map as the source of truth; the stamp is purely
        // for display. Mirrors NecromancerSummon.stampCooldownOnWornSet.
        helmet.set(ModComponents.STORM_COOLDOWN_END, now + STORM_COOLDOWN_TICKS);
    }

    // Private helper — true only when the player has all four Thor armor
    // pieces equipped. Matches ArmorEffects.hasFullSet shape exactly, but
    // inlined here so ThorEffects stays self-contained (and the 5/5 gate
    // composes with the Mjolnir mainhand check cleanly without a second
    // indirection through ArmorEffects). Public helper
    // ArmorEffects.hasFullSet(player, "thor") is also available if other
    // call sites need it later.
    public static boolean hasFullThorSet(PlayerEntity player) {
        return ArmorEffects.hasFullSet(player, "thor");
    }

    // Lifecycle cleanup is intentionally omitted — the cooldown map uses
    // UUIDs and stays tiny (1 long per player who has ever triggered). The
    // next hit after reconnect/respawn re-gates through isStormCooldown
    // which correctly returns false once the timer elapses naturally. Matches
    // NecromancerSummon's approach (it also leaves lastSummonTick alone across
    // death/disconnect).
}
