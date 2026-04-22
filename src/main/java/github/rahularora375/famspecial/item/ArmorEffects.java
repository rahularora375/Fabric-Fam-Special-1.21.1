package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import github.rahularora375.famspecial.item.entries.MistbornItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class ArmorEffects {
    // Refresh bonuses every 4s with 20s duration — re-ups before the sub-10s
    // vanilla blink so effects stay solid while worn and drop fast on unequip.
    private static final int REFRESH_INTERVAL_TICKS = 80;
    private static final int EFFECT_DURATION_TICKS = 400;
    // Passive durability regen cadence for stacks flagged REGENS_DURABILITY:
    // +1 point every 243 ticks (~12s) across all armor slots.
    private static final int DURABILITY_REGEN_INTERVAL_TICKS = 243;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    // Per-player set of mod-managed effects applied on the previous refresh
    // pass. Used by the bonus-dispatch loop to detect trigger drops: any effect
    // that was present last tick but isn't this tick gets explicitly
    // removeStatusEffect'd so the HUD icon disappears immediately instead of
    // lingering the full 20s refresh window.
    private static final Map<UUID, Set<RegistryEntry<StatusEffect>>> appliedThisMod = new ConcurrentHashMap<>();

    // Effects registered by this mod. Only these are candidates for explicit-
    // remove when their trigger drops — vanilla effects (NIGHT_VISION, STRENGTH,
    // SPEED, REGENERATION, WATER_BREATHING) are left to decay naturally so we
    // don't strip a beacon or potion source the player legitimately has
    // elsewhere. A few mod-registered effects (NIGHT_STRENGTH, RADIANT_MIGHT,
    // SUNS_PROTECTION) are also intentionally absent from this set: they used
    // to be diff-stripped but were moved to natural decay so the 400-tick
    // duration rides out after unequip. The 80-tick refresh still re-ups them
    // while the trigger matches; once the trigger flips, the effect fades over
    // its remaining duration rather than snapping off.
    private static final Set<RegistryEntry<StatusEffect>> MOD_MANAGED = Set.of(
            ModStatusEffects.MESSMERS_FLAME_AEGIS,
            ModStatusEffects.MESSMERS_VENOM,
            ModStatusEffects.HEALERS_VISION,
            // Sage's Grace (Pacifist sword HEALS_TARGET mainhand): cosmetic
            // HUD badge. Diff-removed within one 4-s cycle on swap away;
            // instant strip on hotbar scroll handled by
            // ServerPlayNetworkHandlerMixin via refreshBonusesFor.
            ModStatusEffects.SAGES_GRACE,
            ModStatusEffects.EMPOWERED_RIPTIDE,
            ModStatusEffects.TIRELESS_LUNGE,
            ModStatusEffects.STORMLIGHT,
            ModStatusEffects.SHARDBEARING,
            ModStatusEffects.ROTTEN_MUSCLE,
            ModStatusEffects.UNDEAD_RESISTANCE,
            ModStatusEffects.WITHER_TOUCH,
            ModStatusEffects.SHADI_BUFF,
            // Emperor's Divide (Shurima Sun Disc Spear): applied via the regular
            // 80-tick BONUSES pass while IGNORES_KB_RESISTANCE is true on the
            // mainhand. Listed here so the diff-remove path strips it within
            // one 4-second cycle when the predicate flips false (inventory
            // drag, drop, item break). The common scroll/hotbar-key swap case
            // is handled instantly by ServerPlayNetworkHandlerMixin.
            ModStatusEffects.EMPERORS_DIVIDE,
            // God of Thunder (Mjolnir mainhand): cosmetic HUD badge driving the
            // LIGHTNING_ON_HIT roll in ThorEffects.AFTER_DAMAGE. Diff-removed
            // within one 4-second cycle when the mainhand flag drops
            // (inventory drag / drop / item break). Instant removal on
            // scroll / hotbar-key swap is handled by
            // ServerPlayNetworkHandlerMixin.
            ModStatusEffects.GOD_OF_THUNDER,
            // Storm's Awakening (Thunderhelm ready-state badge): cosmetic HUD
            // badge visible only while the helmet is worn AND the per-player
            // ThorEffects storm cooldown is clear. Diff-removed one 4-second
            // cycle after the helmet is unequipped or after the cooldown
            // arms on a successful trigger.
            ModStatusEffects.STORMS_AWAKENING,
            // Asgardian's Flight (Thor 5/5: 4 armor + Mjolnir mainhand): gates
            // Mjolnir's right-click riptide launch in MjolnirMaceItem. Event-
            // stripped by LivingEntityEquipMixin (armor change) and
            // ServerPlayNetworkHandlerMixin (hotbar swap); MOD_MANAGED here is
            // the 4-s-cycle safety net for inventory-drag/drop/item-break
            // paths that neither hook catches.
            ModStatusEffects.ASGARDIANS_FLIGHT
    );

    private record Effect(RegistryEntry<StatusEffect> type, int amplifier) {}

    private record Bonus(String id, Predicate<TickContext> trigger, List<Effect> effects) {
        static Bonus effects(String id, Predicate<TickContext> trigger, Effect... effects) {
            return new Bonus(id, trigger, List.of(effects));
        }
    }

    private record TickContext(
            ServerPlayerEntity player,
            ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots,
            ItemStack mainHand, ItemStack offHand,
            boolean isNight, RegistryKey<World> dimension,
            boolean inDesert
    ) {
        boolean hasFullSet(String setId) {
            return setId.equals(helmet.get(ModComponents.SET_ID))
                    && setId.equals(chest.get(ModComponents.SET_ID))
                    && setId.equals(legs.get(ModComponents.SET_ID))
                    && setId.equals(boots.get(ModComponents.SET_ID));
        }

        // Nether and End have no real day/night cycle, so time-gated bonuses
        // treat these dimensions as always "dark enough" to apply.
        boolean inTimelessDimension() {
            return dimension == World.NETHER || dimension == World.END;
        }
    }

    // Add new status-effect bonuses here. Each entry carries a trigger predicate
    // evaluated per player per tick plus one or more status effects re-applied
    // (20s duration, 4s refresh) while the trigger matches.
    private static final List<Bonus> BONUSES = List.of(
            Bonus.effects("night_vision_helmet",
                    ctx -> Boolean.TRUE.equals(ctx.helmet().get(ModComponents.GRANTS_NIGHT_VISION)),
                    new Effect(StatusEffects.NIGHT_VISION, 0)),

            // Mistborn Strength + Speed are night-gated — the hidden, time-gated
            // payoff the player discovers by watching the tooltip flip to gold
            // at night.
            Bonus.effects("mistborn_set_night",
                    ctx -> (ctx.isNight() || ctx.inTimelessDimension()) && ctx.hasFullSet("mistborn"),
                    new Effect(StatusEffects.STRENGTH, 0),
                    new Effect(StatusEffects.SPEED, 0)),

            Bonus.effects("pacifist_full_set",
                    ctx -> ctx.hasFullSet("pacifist"),
                    new Effect(StatusEffects.REGENERATION, 0)),

            Bonus.effects("water_breathing_helmet",
                    ctx -> Boolean.TRUE.equals(ctx.helmet().get(ModComponents.GRANTS_WATER_BREATHING)),
                    new Effect(StatusEffects.WATER_BREATHING, 0)),

            // Messmer's Flame Aegis is a chestplate-alone bonus (Serpent's
            // Embrace carries GRANTS_MESSMERS_FLAME). 80% fire damage reduction
            // via LivingEntityMixin.
            Bonus.effects("messmers_flame_aegis_chest",
                    ctx -> Boolean.TRUE.equals(ctx.chest().get(ModComponents.GRANTS_MESSMERS_FLAME)),
                    new Effect(ModStatusEffects.MESSMERS_FLAME_AEGIS, 0)),

            // Sage's Crown HUD badge: purely cosmetic Healer's Vision status
            // effect so the wearer sees a top-right icon confirming the floating
            // HP bars are active. The actual rendering lives client-side in
            // HealthOverlay, driven by the same SHOWS_ENTITY_HP helmet flag —
            // effect and overlay share one trigger so they can't drift.
            Bonus.effects("healers_vision_helmet",
                    ctx -> Boolean.TRUE.equals(ctx.helmet().get(ModComponents.SHOWS_ENTITY_HP)),
                    new Effect(ModStatusEffects.HEALERS_VISION, 0)),

            // Fire Serpent 4/4 aura: Messmer's Venom on the wearer, purely
            // cosmetic (MESSMERS_VENOM.applyUpdateEffect short-circuits damage
            // for anyone wearing the full set). Shows the icon top-right as a
            // "your melee hits poison" cue. The actual on-hit application to
            // targets lives in AttackHandlers.
            Bonus.effects("fire_serpent_full_set",
                    ctx -> ctx.hasFullSet("fire_serpent"),
                    new Effect(ModStatusEffects.MESSMERS_VENOM, 0)),

            // Poseidon 4/4 aura: Empowered Riptide HUD badge. Cosmetic only —
            // the dry-land riptide itself is gated in TridentItemMixin by a
            // direct hasFullSet("poseidon") check, not by this effect.
            Bonus.effects("poseidon_full_set",
                    ctx -> ctx.hasFullSet("poseidon"),
                    new Effect(ModStatusEffects.EMPOWERED_RIPTIDE, 0)),

            // Obsidian Dagger HUD badge: appears only when the dagger is in
            // the main hand AND it's night (or Nether/End) — matches exactly
            // the gate the attribute-modifier swap below uses, so icon and
            // damage-number flip share one trigger and can't drift.
            Bonus.effects("night_strength_mainhand",
                    ctx -> Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.NIGHT_STRENGTH))
                            && (ctx.isNight() || ctx.inTimelessDimension()),
                    new Effect(ModStatusEffects.NIGHT_STRENGTH, 0)),

            // Fire Serpent's Wrath HUD badge: appears while the spear is in
            // the main hand. Mirrors the ApplyExhaustionEnchantmentEffectMixin
            // gate on the same NO_LUNGE_HUNGER stack flag so the icon and the
            // actual exhaustion-skip share one trigger. Pops as soon as the
            // player swaps to the spear, drops on swap away — no time gate.
            Bonus.effects("tireless_lunge_mainhand",
                    ctx -> Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.NO_LUNGE_HUNGER)),
                    new Effect(ModStatusEffects.TIRELESS_LUNGE, 0)),

            // Sage's Grace HUD badge: appears while Sage's Grace sword is in the
            // main hand. Cosmetic — the actual heal-on-hit lives in
            // AttackHandlers.AttackEntityCallback, gated on the same HEALS_TARGET
            // flag this trigger reads, so badge + gameplay share one source of
            // truth. Particles suppressed via the showParticles branch in
            // refreshBonusesFor (no ambient swirl — the sword already spawns its
            // own on-hit particles).
            Bonus.effects("sages_grace_mainhand",
                    ctx -> Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.HEALS_TARGET)),
                    new Effect(ModStatusEffects.SAGES_GRACE, 0)),

            // Emperor's Divide (Shurima Sun Disc Spear): advertises the KB-
            // resistance bypass while the spear is in the main hand. Gameplay
            // lives in LivingEntityKnockbackMixin, gated on the same
            // IGNORES_KB_RESISTANCE mainhand flag this trigger reads — icon
            // and bypass share one source of truth. Applied on the 80-tick
            // BONUSES cadence (fallback for drag/drop/break removal); instant
            // removal on scroll/hotbar-key swap is handled by
            // ServerPlayNetworkHandlerMixin. No time gate.
            Bonus.effects("emperors_divide_mainhand",
                    ctx -> Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.IGNORES_KB_RESISTANCE)),
                    new Effect(ModStatusEffects.EMPERORS_DIVIDE, 0)),

            // Knight Radiant Stormlight aura: any equipped armor slot flagged
            // REGENS_DURABILITY lights up the wearer with End Rod particles via
            // STORMLIGHT.applyUpdateEffect. A la carte — matches the 243-tick
            // durability-regen block's per-slot gate, not set-gated. Cosmetic
            // only (the regen itself lives in the 243-tick block above).
            Bonus.effects("stormlight_any_piece",
                    ctx -> Boolean.TRUE.equals(ctx.helmet().get(ModComponents.REGENS_DURABILITY))
                            || Boolean.TRUE.equals(ctx.chest().get(ModComponents.REGENS_DURABILITY))
                            || Boolean.TRUE.equals(ctx.legs().get(ModComponents.REGENS_DURABILITY))
                            || Boolean.TRUE.equals(ctx.boots().get(ModComponents.REGENS_DURABILITY)),
                    new Effect(ModStatusEffects.STORMLIGHT, 0)),

            // Oathbringer HUD badge: appears while the sword is in the main
            // hand. Shardbearing is cosmetic — the actual gameplay (+5%
            // current-HP bonus damage on the wearer's melee hits, past all
            // mitigation) lives in LivingEntityMixin, keyed off the same
            // GRANTS_SHARDBEARING mainhand flag this trigger reads, so icon
            // and HP-chip share one gate and can't drift. No time gate.
            Bonus.effects("shardbearing_mainhand",
                    ctx -> Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.GRANTS_SHARDBEARING)),
                    new Effect(ModStatusEffects.SHARDBEARING, 0)),

            // Knight Radiant 4/4 set bonus: applies Radiant Might (+1
            // ATTACK_DAMAGE via the effect's chained attribute modifier — a
            // third of vanilla Strength I). No time gate. Distinct from the
            // per-piece Stormlight aura above, which fires off any single
            // Shard piece.
            Bonus.effects("knight_radiant_full_set",
                    ctx -> ctx.hasFullSet("knight_radiant"),
                    new Effect(ModStatusEffects.RADIANT_MIGHT, 0)),

            // Necromancer 4/4 charge indicator: Rotten Muscle. Only active
            // while full set is worn AND the summon is off cooldown — so the
            // badge disappears during the 5-minute cooldown window and returns
            // when the summon is charged again. isOnCooldown reads the same
            // lastSummonTick map that drives the summon gate, so icon and
            // gameplay share one source of truth.
            Bonus.effects("rotten_muscle_ready",
                    ctx -> ctx.hasFullSet("necromancer")
                            && !NecromancerSummon.isOnCooldown(ctx.player(),
                                    ctx.player().getEntityWorld().getTime()),
                    new Effect(ModStatusEffects.ROTTEN_MUSCLE, 0)),

            // Undead Resistance: Necromancer chest piece (Smooth Criminal's
            // Vestment). Advertises the 60% undead-damage reduction gated in
            // LivingEntityMixin on the same GRANTS_UNDEAD_RESISTANCE flag this
            // trigger reads. Piece-alone bonus — doesn't require the full set.
            Bonus.effects("undead_resistance_chest",
                    ctx -> Boolean.TRUE.equals(ctx.chest().get(ModComponents.GRANTS_UNDEAD_RESISTANCE)),
                    new Effect(ModStatusEffects.UNDEAD_RESISTANCE, 0)),

            // Wither Touch: Thriller's Edge (Necromancer axe). Mirrors the
            // AttackHandlers ALLOW_DAMAGE branch on the same APPLIES_WITHER_ON_HIT
            // mainhand flag, so badge + gameplay share one source of truth.
            // No time gate — pops on swap to axe, drops on swap away.
            Bonus.effects("wither_touch_mainhand",
                    ctx -> Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.APPLIES_WITHER_ON_HIT)),
                    new Effect(ModStatusEffects.WITHER_TOUCH, 0)),

            // Esh-Endra-Navesh helmet (Time Dekhlo Helmet): Bad Omen while worn
            // at night (6 PM → 6 AM) or anytime in Nether/End. Shares the
            // Mistborn/Obsidian Dagger night gate. Vanilla effect — left out of
            // MOD_MANAGED so ominous-bottle / beacon sources decay naturally
            // rather than getting stripped on unequip.
            Bonus.effects("ominous_helmet",
                    ctx -> Boolean.TRUE.equals(ctx.helmet().get(ModComponents.GRANTS_OMINOUS))
                            && (ctx.isNight() || ctx.inTimelessDimension()),
                    new Effect(StatusEffects.BAD_OMEN, 0)),

            // Esh-Endra-Navesh 4/4 set bonus: Haste I. Same "vanilla effect,
            // natural decay" convention as Mistborn Strength/Speed.
            Bonus.effects("esh_endra_navesh_full_set",
                    ctx -> ctx.hasFullSet("esh_endra_navesh"),
                    new Effect(StatusEffects.HASTE, 0)),

            // Just Hit Bro pickaxe HUD badge: Shadi Buff. Mirrors the
            // BONUS_DIAMOND_CHANCE mainhand gate used by BlockBreakHandler, so
            // badge + gameplay share one flag. Pops on swap to pickaxe, drops
            // on swap away via MOD_MANAGED's diff.
            Bonus.effects("shadi_buff_mainhand",
                    ctx -> Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.BONUS_DIAMOND_CHANCE)),
                    new Effect(ModStatusEffects.SHADI_BUFF, 0)),

            // Sun's Protection (Shurima helmet + desert): applies the
            // SUNS_PROTECTION effect while the helmet is worn AND the player
            // is in BiomeKeys.DESERT. The actual 20% damage reduction lives in
            // LivingEntityMixin, gated on the effect's presence. Now in
            // MOD_MANAGED: helmet removal is caught instantly by
            // LivingEntityEquipMixin, biome leave is caught on the next 4-s
            // MOD_MANAGED diff pass — so the effect no longer outlives its
            // triggers by ~20 s of natural decay as in earlier versions.
            Bonus.effects("suns_protection_helmet_desert",
                    ctx -> Boolean.TRUE.equals(ctx.helmet().get(ModComponents.GRANTS_SUNS_PROTECTION))
                            && ctx.inDesert(),
                    new Effect(ModStatusEffects.SUNS_PROTECTION, 0)),

            // Shurima 4/4 set + desert: grants SHURIMAN_ENDURANCE (cancels all
            // addExhaustion calls via PlayerEntityExhaustionMixin). Applied
            // with a 4-minute apply-once duration by a dedicated branch in the
            // dispatch loop below — once earned, it persists anywhere the
            // player goes for ~4 minutes, beacon-style. Not in MOD_MANAGED;
            // decays naturally when its timer expires.
            Bonus.effects("shuriman_endurance_full_set_desert",
                    ctx -> ctx.hasFullSet("shurima") && ctx.inDesert(),
                    new Effect(ModStatusEffects.SHURIMAN_ENDURANCE, 0)),

            // God of Thunder (Mjolnir mainhand): HUD badge advertising the
            // LIGHTNING_ON_HIT roll. Mirrors the ThorEffects.AFTER_DAMAGE gate
            // on the same mainhand flag so badge + mechanic share one source
            // of truth. Pops on swap to Mjolnir, drops on swap away via
            // MOD_MANAGED's diff (or the ServerPlayNetworkHandlerMixin fast
            // path on hotbar scroll).
            Bonus.effects("god_of_thunder_mainhand",
                    ctx -> Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.LIGHTNING_ON_HIT)),
                    new Effect(ModStatusEffects.GOD_OF_THUNDER, 0)),

            // Storm's Awakening (Thunderhelm ready state): HUD badge visible
            // only while the helmet is worn AND the per-player storm cooldown
            // is clear. Cosmetic — advertises "ability is armed, next kill may
            // summon the storm." Disappears during the 48000-tick (2 Minecraft
            // days) cooldown window that follows a successful trigger, then
            // re-appears once cleared. ThorEffects.isStormCooldown is the
            // single source of truth for the cooldown state; this bonus's
            // trigger queries it live so badge + gameplay can't drift.
            Bonus.effects("storms_awakening_helmet_ready",
                    ctx -> Boolean.TRUE.equals(ctx.helmet().get(ModComponents.TRIGGERS_STORM_AWAKENING))
                            && !ThorEffects.isStormCooldown(ctx.player(),
                                    ctx.player().getEntityWorld().getTime()),
                    new Effect(ModStatusEffects.STORMS_AWAKENING, 0)),

            // Asgardian's Flight (Thor 5/5): gameplay gate for Mjolnir's
            // right-click riptide launch. Trigger is full Thor armor set AND
            // mainhand THOR_MACE — the only custom 5/5 gate in the mod.
            // MjolnirMaceItem#use and MjolnirMaceItem#onStoppedUsing read this
            // effect directly; the riptide launch short-circuits unless it's
            // present. Event-stripped by LivingEntityEquipMixin on armor
            // change and ServerPlayNetworkHandlerMixin on hotbar swap.
            Bonus.effects("asgardians_flight_full_thor_set_and_mjolnir",
                    ctx -> ctx.hasFullSet("thor")
                            && Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.THOR_MACE)),
                    new Effect(ModStatusEffects.ASGARDIANS_FLIGHT, 0))
    );

    // Shuriman Endurance is applied once for ~4 minutes (4800 ticks) and only
    // refreshed when the remaining duration drops below this threshold (i.e.
    // the effect is within one 80-tick refresh cycle of expiring). Keeps it
    // from being re-applied every 4s while the trigger matches — so once
    // earned in the desert, the player can leave and the effect rides out its
    // full duration without being clobbered back to the 400-tick default.
    private static final int SHURIMAN_ENDURANCE_DURATION_TICKS = 4800;
    private static final int SHURIMAN_ENDURANCE_REFRESH_THRESHOLD_TICKS = 4720;

    // Public helper used by non-tick callers (e.g. TridentItemMixin, client
    // tooltips) to check whether a player currently has all four armor slots
    // filled with pieces tagged with the given SET_ID. Mirrors TickContext's
    // local check but reads live off the player instead of pre-fetched stacks.
    public static boolean hasFullSet(PlayerEntity player, String setId) {
        return setId.equals(player.getEquippedStack(EquipmentSlot.HEAD).get(ModComponents.SET_ID))
                && setId.equals(player.getEquippedStack(EquipmentSlot.CHEST).get(ModComponents.SET_ID))
                && setId.equals(player.getEquippedStack(EquipmentSlot.LEGS).get(ModComponents.SET_ID))
                && setId.equals(player.getEquippedStack(EquipmentSlot.FEET).get(ModComponents.SET_ID));
    }

    // Instant re-dispatch for a single player: builds the TickContext, walks
    // BONUSES applying matching effects, diff-removes any MOD_MANAGED effect
    // whose trigger flipped, and runs the Obsidian Dagger attribute swap.
    // Called every 80 ticks from the tick loop (the eventual-consistency
    // safety net) AND on every equipment / hotbar swap via the instant-apply
    // mixins so swap-in latency for effect-gated gameplay (Emperor's Divide,
    // Messmer's Flame Aegis, Sun's Protection, Messmer's Venom propagation,
    // Asgardian's Flight, Shuriman Endurance) drops to zero. Idempotent —
    // safe to run multiple times per tick; same-duration re-application is
    // a no-op and the diff is last-write-wins on appliedThisMod.
    public static void refreshBonusesFor(ServerPlayerEntity player) {
        var server = player.getEntityWorld().getServer();
        if (server == null) return;
        boolean isNight = (server.getOverworld().getTimeOfDay() % 24000) >= 12000;

        // Arid-biome flag for the Shurima bonuses (Sun's Protection helmet
        // gate, Shuriman Endurance 4/4 gate). True in the classic sandy desert
        // plus the two terracotta badlands biomes — the "desert-family" arid
        // set. Variable kept as inDesert to match the existing TickContext
        // field; semantic meaning is "is in a desert-family biome."
        var biome = player.getEntityWorld().getBiome(player.getBlockPos());
        boolean inDesert = biome.matchesKey(BiomeKeys.DESERT)
                || biome.matchesKey(BiomeKeys.BADLANDS)
                || biome.matchesKey(BiomeKeys.ERODED_BADLANDS);

        TickContext ctx = new TickContext(
                player,
                player.getEquippedStack(EquipmentSlot.HEAD),
                player.getEquippedStack(EquipmentSlot.CHEST),
                player.getEquippedStack(EquipmentSlot.LEGS),
                player.getEquippedStack(EquipmentSlot.FEET),
                player.getEquippedStack(EquipmentSlot.MAINHAND),
                player.getEquippedStack(EquipmentSlot.OFFHAND),
                isNight,
                player.getEntityWorld().getRegistryKey(),
                inDesert
        );

        Set<RegistryEntry<StatusEffect>> newlyActive = new HashSet<>();
        for (Bonus bonus : BONUSES) {
            if (!bonus.trigger().test(ctx)) continue;
            for (Effect effect : bonus.effects()) {
                // Shuriman Endurance is applied once with a 4-minute
                // duration and refreshed only when remaining drops
                // below the threshold. Skips re-application while the
                // player already has a fresh stack of the effect so
                // leaving the desert doesn't clobber the timer.
                if (effect.type() == ModStatusEffects.SHURIMAN_ENDURANCE) {
                    StatusEffectInstance existing = player.getStatusEffect(effect.type());
                    if (existing != null
                            && existing.getDuration() > SHURIMAN_ENDURANCE_REFRESH_THRESHOLD_TICKS) {
                        newlyActive.add(effect.type());
                        continue;
                    }
                    player.addStatusEffect(new StatusEffectInstance(
                            effect.type(),
                            SHURIMAN_ENDURANCE_DURATION_TICKS,
                            effect.amplifier(),
                            false, true, true
                    ));
                    newlyActive.add(effect.type());
                    continue;
                }

                // Rotten Muscle and Wither Touch suppress the colored
                // swirl — their applyUpdateEffect spawns themed vanilla
                // particles (Soul wisps / Ash) instead, so the player
                // reads the necro aesthetic rather than a generic orb.
                // Sage's Grace also suppresses the swirl — the Pacifist
                // sword already spawns its own on-hit heal particles, so
                // the ambient badge swirl would double up.
                boolean showParticles = effect.type() != ModStatusEffects.ROTTEN_MUSCLE
                        && effect.type() != ModStatusEffects.WITHER_TOUCH
                        && effect.type() != ModStatusEffects.SAGES_GRACE;
                player.addStatusEffect(new StatusEffectInstance(
                        effect.type(),
                        EFFECT_DURATION_TICKS,
                        effect.amplifier(),
                        false,  // not ambient
                        showParticles,
                        true    // show icon
                ));
                newlyActive.add(effect.type());
            }
        }

        // Instant-removal for mod-managed effects whose trigger just
        // dropped: compare last tick's set to this tick's set, and for
        // every mod-managed effect present last tick but absent now,
        // strip it so the HUD icon disappears on the next 80-tick pass
        // instead of lingering the full 20s duration. Vanilla effects
        // (NIGHT_VISION, STRENGTH, SPEED, REGENERATION, WATER_BREATHING)
        // are left to decay naturally so we don't strip a beacon or
        // potion source the player legitimately has elsewhere.
        UUID uuid = player.getUuid();
        Set<RegistryEntry<StatusEffect>> previouslyActive =
                appliedThisMod.getOrDefault(uuid, Set.of());
        for (RegistryEntry<StatusEffect> prev : previouslyActive) {
            if (!newlyActive.contains(prev) && MOD_MANAGED.contains(prev)) {
                player.removeStatusEffect(prev);
            }
        }
        Set<RegistryEntry<StatusEffect>> toStore = new HashSet<>();
        for (RegistryEntry<StatusEffect> e : newlyActive) {
            if (MOD_MANAGED.contains(e)) toStore.add(e);
        }
        appliedThisMod.put(uuid, toStore);

        // Main-hand-only: if the item held in the main hand carries the
        // NIGHT_STRENGTH marker, swap its AttributeModifiers between the
        // day and night forms so the tooltip numbers themselves change.
        // Off-hand and inventory copies are untouched.
        if (Boolean.TRUE.equals(ctx.mainHand().get(ModComponents.NIGHT_STRENGTH))) {
            boolean nightActive = ctx.isNight() || ctx.inTimelessDimension();
            AttributeModifiersComponent desired = nightActive
                    ? MistbornItems.DAGGER_NIGHT_MODIFIERS
                    : MistbornItems.DAGGER_DAY_MODIFIERS;
            AttributeModifiersComponent current =
                    ctx.mainHand().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (!desired.equals(current)) {
                ctx.mainHand().set(DataComponentTypes.ATTRIBUTE_MODIFIERS, desired);
            }
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Passive durability regen runs on its own cadence independent of
            // the bonus-refresh cadence — every 243 ticks, each armor slot
            // flagged REGENS_DURABILITY heals one point of damage. Cheap:
            // one mod/get per armor slot per online player per 12s.
            if (server.getTicks() % DURABILITY_REGEN_INTERVAL_TICKS == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    for (EquipmentSlot slot : ARMOR_SLOTS) {
                        ItemStack stack = player.getEquippedStack(slot);
                        if (!Boolean.TRUE.equals(stack.get(ModComponents.REGENS_DURABILITY))) continue;
                        if (stack.getDamage() > 0) {
                            // Track the broken-state transition across the regen tick.
                            // While a piece sits at maxDamage - 1 it's "broken" (no
                            // armor, no toughness, no enchants — gated by the attribute
                            // + enchantment mixins). The tick that heals it back to
                            // maxDamage - 2 flips it back to usable, but the entity's
                            // AttributeContainer still has the gated (empty) modifiers
                            // cached. Force an equipStack on that slot so vanilla's
                            // onEquipStack path re-runs applyAttributeModifiers — the
                            // mixin now lets the entries through again, restoring armor
                            // + toughness + custom +max health.
                            boolean wasBroken = stack.isDamageable()
                                    && stack.getDamage() >= stack.getMaxDamage() - 1;
                            stack.setDamage(stack.getDamage() - 1);
                            if (wasBroken
                                    && Boolean.TRUE.equals(stack.get(ModComponents.INDESTRUCTIBLE))) {
                                player.equipStack(slot, stack);
                            }
                        }
                    }
                }
            }

            if (server.getTicks() % REFRESH_INTERVAL_TICKS != 0) return;

            // Slow-path safety net: every 80 ticks re-dispatch for every
            // online player. The instant-apply mixins (LivingEntityEquipMixin,
            // ServerPlayNetworkHandlerMixin) call refreshBonusesFor on swap
            // events so gameplay latency is zero; this loop remains as the
            // eventual-consistency pass for anything those hooks miss
            // (dispenser equip, item break, /item replace, etc.).
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                refreshBonusesFor(player);
            }
        });

        // Lifecycle cleanup: drop per-player tracking on disconnect and death
        // so the map doesn't grow unbounded and a dead/reconnected player
        // doesn't carry stale state into their next tick pass.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                appliedThisMod.remove(handler.getPlayer().getUuid()));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity p) {
                appliedThisMod.remove(p.getUuid());
            }
        });

        FamSpecial.LOGGER.info("Registering armor effects for {}", FamSpecial.MOD_ID);
    }
}