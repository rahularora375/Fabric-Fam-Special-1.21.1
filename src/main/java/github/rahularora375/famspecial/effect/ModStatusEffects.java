package github.rahularora375.famspecial.effect;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.item.ArmorEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class ModStatusEffects {
    // Fire Serpent 4/4 set bonus: reduces incoming fire damage by 80%. The
    // reduction itself lives in LivingEntityMixin — this class only registers
    // the effect so we have a RegistryEntry to check against. Color int matches
    // vanilla fire_resistance so the potion-particle tint reads identically to
    // a Fire Resistance potion; the icon PNG shipped at
    // assets/famspecial/textures/mob_effect/messmers_flame_aegis.png is a byte-
    // for-byte copy of vanilla's fire_resistance.png (extracted at build-time
    // from the Fabric Loom cache). Display name: "Messmer's Flame Aegis".
    public static final RegistryEntry<StatusEffect> MESSMERS_FLAME_AEGIS = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "messmers_flame_aegis"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 16750848) {}
    );

    // Fire Serpent set bonus vector. Dual-purpose:
    //   1. Aura: applied by ArmorEffects to the wearer while 4/4 is worn, so
    //      the player sees the icon in the HUD as a visual "empowered" cue.
    //      applyUpdateEffect no-ops on a wearer — the fullSet check short-
    //      circuits damage ticks for them. Icon + particles still render.
    //   2. Offense: applied by AttackHandlers on every melee hit while 4/4 is
    //      worn. Targets that AREN'T wearing Fire Serpent fall through to the
    //      damage tick — 1.0 magic damage every 25 ticks at amp 0, stops at
    //      1 HP — mirroring vanilla PoisonStatusEffect byte-for-byte.
    //
    // The reason we don't just apply StatusEffects.POISON is vanilla
    // immunities: spiders, undead, witches, the Wither, and the Ender Dragon
    // all short-circuit canHaveStatusEffect with a direct equality check
    // against StatusEffects.POISON. A distinct registry entry here bypasses
    // every one of those checks. Color matches vanilla poison green so
    // particles read identically to a Poison potion.
    public static final RegistryEntry<StatusEffect> MESSMERS_VENOM = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "messmers_venom"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x4E9331) {
                @Override
                public boolean canApplyUpdateEffect(int duration, int amplifier) {
                    int i = 25 >> amplifier;
                    return i <= 0 || duration % i == 0;
                }

                @Override
                public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
                    // Wearer immunity: the 4/4 aura is cosmetic for the player
                    // who has it. Damage only ticks on entities that don't
                    // themselves have the set equipped.
                    if (entity instanceof PlayerEntity p && ArmorEffects.hasFullSet(p, "fire_serpent")) {
                        return true;
                    }
                    if (entity.getHealth() > 1.0F) {
                        entity.damage(world, entity.getDamageSources().magic(), 1.0F);
                    }
                    return true;
                }
            }
    );

    // Tireless Lunge HUD marker for Fire Serpent's Wrath. Purely cosmetic —
    // the actual gameplay (Lunge's exhaustion cost is skipped) lives in
    // ApplyExhaustionEnchantmentEffectMixin and is gated on the spear's
    // NO_LUNGE_HUNGER component. This badge tells the player "yes, lunging
    // with this spear won't eat hunger." Color is Fire Serpent blazing-orange
    // (#FF6600) to match the set's name-prefix palette. Icon at
    // assets/famspecial/textures/mob_effect/tireless_lunge.png is a copy of
    // vanilla saturation.png — the most on-theme "hunger isn't depleting"
    // badge.
    public static final RegistryEntry<StatusEffect> TIRELESS_LUNGE = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "tireless_lunge"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xFF6600) {}
    );

    // Night Strength HUD marker for the Obsidian Dagger. Purely cosmetic —
    // the actual gameplay (day/night attack-damage swap on the held stack)
    // lives in ArmorEffects and is driven by the NIGHT_STRENGTH component on
    // the main-hand stack plus the night/timeless-dimension gate. This badge
    // just tells the player "yes, the dagger is in its strong form right
    // now." Color is a dark indigo (#4B0082) to read as "Mistborn at night"
    // rather than as vanilla Strength (red). Icon at
    // assets/famspecial/textures/mob_effect/night_strength.png is a copy of
    // vanilla strength.png.
    public static final RegistryEntry<StatusEffect> NIGHT_STRENGTH = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "night_strength"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x4B0082) {}
    );

    // Sage's Grace: Pacifist theme HUD badge advertising the sword's heal-on-
    // hit mechanic. Purely cosmetic — the actual heal-target gameplay lives in
    // AttackHandlers' AttackEntityCallback, gated on the same HEALS_TARGET
    // mainhand flag this badge uses via ArmorEffects' sages_grace_mainhand
    // bonus, so icon + gameplay share one source of truth. Tooltip renders
    // "Sage's Grace" via the lang file. Icon at
    // assets/famspecial/textures/mob_effect/sages_grace.png is a copy of
    // vanilla item/golden_apple.png — the most on-theme "gentle heal" badge.
    // Color 0x9FE59F is a soft Pacifist-palette green.
    public static final RegistryEntry<StatusEffect> SAGES_GRACE = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "sages_grace"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x9FE59F) {}
    );

    // Empowered Riptide HUD marker for the Poseidon 4/4 set. Purely cosmetic
    // — the actual gameplay (Riptide fires on dry land) is gated in
    // TridentItemMixin via ArmorEffects.hasFullSet(player, "poseidon") and
    // doesn't read this effect at all. The badge lets the wearer see at a
    // glance that the set bonus is live. Color is Poseidon deep-sky-blue
    // (#00BFFF) so particles match the set's name-prefix palette. Icon at
    // assets/famspecial/textures/mob_effect/empowered_riptide.png is a copy
    // of vanilla conduit_power.png — the most on-theme aquatic badge.
    public static final RegistryEntry<StatusEffect> EMPOWERED_RIPTIDE = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "empowered_riptide"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x00BFFF) {}
    );

    // Healer's Vision HUD marker for Sage's Crown. Purely cosmetic status-
    // effect badge — the actual gameplay (floating HP bars above other players
    // in view) lives in HealthOverlay and is driven by the SHOWS_ENTITY_HP
    // helmet flag, not by this effect. We register the effect purely so the
    // wearer sees an icon in the top-right HUD confirming the cue is on, the
    // same way Messmer's Flame Aegis advertises its fire-resist aura. Color matches
    // the Pacifist electric-cyan palette (name prefix #00D4FF) so particles
    // read as "pacifist set" rather than a generic potion tint. Icon at
    // assets/famspecial/textures/mob_effect/healers_vision.png is a copy of
    // vanilla glowing.png — thematically on-point for "see entity state".
    public static final RegistryEntry<StatusEffect> HEALERS_VISION = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "healers_vision"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x00D4FF) {}
    );

    // Stormlight HUD marker + particle aura for the Knight Radiant Shard set.
    // Gameplay-bearing only insofar as applyUpdateEffect spawns End Rod
    // particles around the wearer — the durability-regen mechanic it advertises
    // lives in ArmorEffects' 243-tick block, driven by the REGENS_DURABILITY
    // flag, not by this effect. Color 0xA0D2FF matches the Knight Radiant
    // NAME_PREFIX palette so the HUD badge border tints accordingly. Icon at
    // assets/famspecial/textures/mob_effect/stormlight.png is a copy of vanilla
    // conduit_power.png — the most on-theme "radiant aura" badge.
    public static final RegistryEntry<StatusEffect> STORMLIGHT = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "stormlight"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xA0D2FF) {
                @Override
                public boolean canApplyUpdateEffect(int duration, int amplifier) {
                    // Throttle — spawn particles every 8 ticks (~2.5 times/second)
                    // for a slow, breath-like cadence rather than a steady stream.
                    return duration % 8 == 0;
                }

                @Override
                public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
                    // Lore-accurate Stormlight: faint glowing white wisps drifting
                    // off the wearer like breath in cold air. Snowflake particles
                    // are small, pale, and drift gently — the subtlest "radiant
                    // mist" option vanilla offers. Count stays low (2 per burst)
                    // and the upward drift speed is near zero so they hang in
                    // place rather than streaming visibly.
                    double x = entity.getX();
                    double y = entity.getY() + entity.getHeight() * 0.5;
                    double z = entity.getZ();
                    world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 2, 0.25, 0.4, 0.25, 0.005);
                    return true;
                }
            }
    );

    // Radiant Might: Knight Radiant 4/4 set bonus. Real attribute-bearing
    // effect — +2 ATTACK_DAMAGE (two-thirds of vanilla Strength I's +3.0).
    // Applied by ArmorEffects' knight_radiant_full_set bonus while all four
    // Shard pieces are worn. The attribute modifier chained on registration
    // means vanilla StatusEffect handles application/removal automatically
    // during the effect's active window — no applyUpdateEffect override
    // needed. Color 0xE0F0FF matches the Knight Radiant NAME_PIECE palette
    // (near-white-blue bold accent). Icon at
    // assets/famspecial/textures/mob_effect/radiant_might.png is a copy of
    // vanilla strength.png.
    public static final RegistryEntry<StatusEffect> RADIANT_MIGHT = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "radiant_might"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xE0F0FF) {}
                    .addAttributeModifier(
                            EntityAttributes.ATTACK_DAMAGE,
                            Identifier.of(FamSpecial.MOD_ID, "effect.radiant_might"),
                            2.0,
                            EntityAttributeModifier.Operation.ADD_VALUE)
    );

    // Shardbearing HUD marker for Oathbringer: purely cosmetic — the actual
    // gameplay (+5% current-HP bonus damage on melee hits, bypassing armor/
    // protection/resistance) lives in LivingEntityMixin and is driven by the
    // GRANTS_SHARDBEARING component on the attacker's main-hand stack. This
    // badge tells the player "yes, my hits are chipping extra HP right now."
    // Color 0xE0F0FF matches the Knight Radiant NAME_PIECE palette. Icon at
    // assets/famspecial/textures/mob_effect/shardbearing.png is a copy of
    // vanilla item/prismarine_shard.png — literally a shard, pale-blue tone
    // on-theme for the effect name + Knight Radiant palette. Extracted from
    // the Loom client jar.
    public static final RegistryEntry<StatusEffect> SHARDBEARING = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "shardbearing"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xE0F0FF) {}
    );

    // Rotten Muscle: Necromancer 4/4 set bonus HUD badge + particle aura.
    // Advertises the Zombie-Reinforcements charge state — applied by
    // ArmorEffects' bonus only while the full set is worn AND the summon is
    // off cooldown. On every hit that triggers a summon the cooldown flips
    // "on" for 5 minutes, at which point the ArmorEffects trigger drops and
    // the effect naturally times out in one refresh cycle (within 20s). When
    // the cooldown ends the badge re-applies, visually telling the wearer
    // "the summon is charged again." Color 0x8B0000 matches the Necromancer
    // LORE_ACCENT palette. Particles emit soul flame every 8 ticks while
    // active. Icon copied byte-for-byte from vanilla mob_effect/wither.png.
    public static final RegistryEntry<StatusEffect> ROTTEN_MUSCLE = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "rotten_muscle"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x8B0000) {
                @Override
                public boolean canApplyUpdateEffect(int duration, int amplifier) {
                    return duration % 10 == 0;
                }

                @Override
                public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
                    double x = entity.getX();
                    double y = entity.getY() + entity.getHeight() * 0.5;
                    double z = entity.getZ();
                    world.spawnParticles(ParticleTypes.SOUL, x, y, z, 1, 0.3, 0.5, 0.3, 0.005);
                    return true;
                }
            }
    );

    // Undead Resistance: Necromancer chest piece (Smooth Criminal's Vestment)
    // HUD badge + particle aura. Advertises the 60% undead-damage reduction.
    // The reduction itself lives in LivingEntityMixin, gated on the chest's
    // GRANTS_UNDEAD_RESISTANCE component; this effect only exists so the
    // wearer sees the icon and feels the "shroud" via smoke wisps. Color
    // 0x6B8E5A matches the Necromancer NAME_PREFIX palette (muted green).
    // Icon copied byte-for-byte from vanilla mob_effect/resistance.png.
    public static final RegistryEntry<StatusEffect> UNDEAD_RESISTANCE = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "undead_resistance"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x6B8E5A) {
                @Override
                public boolean canApplyUpdateEffect(int duration, int amplifier) {
                    return duration % 12 == 0;
                }

                @Override
                public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
                    double x = entity.getX();
                    double y = entity.getY() + entity.getHeight() * 0.4;
                    double z = entity.getZ();
                    world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.3, 0.5, 0.3, 0.002);
                    return true;
                }
            }
    );

    // Shadi Buff: Just Hit Bro (Esh-Endra-Navesh pickaxe) HUD badge. Purely
    // cosmetic — advertises the BONUS_DIAMOND_CHANCE roll mechanic, which itself
    // lives in BlockBreakHandler driven by the same flag on the main-hand stack.
    // Pops on swap to the pickaxe, drops on swap away via MOD_MANAGED's diff.
    // Color 0xE8D171 matches the Esh-Endra-Navesh NAME_PIECE palette (gold).
    // Icon at assets/famspecial/textures/mob_effect/shadi_buff.png is vanilla
    // item/diamond.png (16x16) padded with a 1px transparent border to match
    // the 18x18 mob_effect convention.
    public static final RegistryEntry<StatusEffect> SHADI_BUFF = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "shadi_buff"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xE8D171) {}
    );

    // Wither Touch: Thriller's Edge (Necromancer axe) HUD badge + particle
    // aura. Advertises the Wither-II-on-hit gameplay, which itself lives in
    // AttackHandlers' ALLOW_DAMAGE handler gated on the mainhand stack's
    // APPLIES_WITHER_ON_HIT flag. This effect only fires while the axe is
    // actually in the main hand — so the ash-like smoke puffing off the
    // weapon tells the player "your next hit will wither them." Color
    // 0xA8C49A matches the Necromancer NAME_PIECE palette (lighter green).
    // Icon copied byte-for-byte from vanilla mob_effect/wither.png (skull sprite).
    public static final RegistryEntry<StatusEffect> WITHER_TOUCH = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "wither_touch"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xA8C49A) {
                @Override
                public boolean canApplyUpdateEffect(int duration, int amplifier) {
                    return duration % 6 == 0;
                }

                @Override
                public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
                    double x = entity.getX();
                    double y = entity.getY() + entity.getHeight() * 0.6;
                    double z = entity.getZ();
                    world.spawnParticles(ParticleTypes.ASH, x, y, z, 2, 0.3, 0.4, 0.3, 0.01);
                    return true;
                }
            }
    );

    // Emperor's Divide: HUD badge + gameplay marker for the Sun Disc Spear
    // (Shurima). Gameplay lives in LivingEntityKnockbackMixin: when the
    // attacker has this effect, the target's KNOCKBACK_RESISTANCE attribute
    // is bypassed so knockback always lands full strength. Applied and
    // removed on a per-tick fast-path in ArmorEffects driven by the
    // IGNORES_KB_RESISTANCE main-hand flag — guarantees <=1-tick (50 ms)
    // removal on weapon swap. Color 0xC8A84B matches the Shurima NAME_PREFIX
    // palette (Worn Gold). Icon copied byte-for-byte from vanilla strength.png.
    public static final RegistryEntry<StatusEffect> EMPERORS_DIVIDE = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "emperors_divide"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xC8A84B) {}
    );

    // Sun's Protection: HUD badge + gameplay marker for the Sun Emperor's
    // Crown (Shurima helmet). Gameplay lives in LivingEntityMixin — while
    // the wearer has this effect, incoming damage is scaled to 0.8x (= Resistance I).
    // Applied by ArmorEffects while the helmet is worn AND the player is in
    // BiomeKeys.DESERT. Included in MOD_MANAGED so it drops within one 4-s
    // cycle of the biome predicate flipping (player walks out of desert);
    // helmet removal is additionally caught instantly by LivingEntityEquipMixin.
    // Color 0xE8922A matches the Shurima LORE_ACCENT palette (Warm Orange).
    // Icon copied byte-for-byte from vanilla resistance.png.
    public static final RegistryEntry<StatusEffect> SUNS_PROTECTION = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "suns_protection"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xE8922A) {}
    );

    // Shuriman Endurance: HUD badge + gameplay marker for the Shurima 4/4
    // set. Gameplay lives in PlayerEntityExhaustionMixin — while the player
    // has this effect, all addExhaustion calls are cancelled at HEAD so no
    // saturation loss accrues from any source. Applied by ArmorEffects with
    // a 4-minute (4800-tick) duration and an apply-once gate (refresh only
    // if remaining < 4720), so once earned it persists anywhere the player
    // goes — matches the "beacon effect" framing. Not added to MOD_MANAGED;
    // decays naturally when the timer expires. Color 0xB8A070 matches the
    // Shurima LORE_BASE palette (Dusty Parchment). Icon copied byte-for-byte
    // from vanilla saturation.png.
    public static final RegistryEntry<StatusEffect> SHURIMAN_ENDURANCE = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "shuriman_endurance"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xB8A070) {}
    );

    // God of Thunder: cosmetic HUD badge for Mjolnir. Advertises the lightning-
    // on-hit roll — actual gameplay lives in ThorEffects' AFTER_DAMAGE handler,
    // gated on the attacker's mainhand carrying LIGHTNING_ON_HIT. Effect and
    // handler share the same flag via ArmorEffects' god_of_thunder_mainhand
    // bonus so icon + mechanic can't drift. Color 0x60C8FF matches the Thor
    // LORE_ACCENT palette (lightning cyan). Icon at
    // assets/famspecial/textures/mob_effect/god_of_thunder.png is a copy of
    // vanilla strength.png.
    public static final RegistryEntry<StatusEffect> GOD_OF_THUNDER = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "god_of_thunder"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x60C8FF) {}
    );

    // Storm's Awakening: cosmetic HUD badge for Thunderhelm. Advertises the
    // ready-to-fire state of the thunderstorm kill-trigger — applied by
    // ArmorEffects' storms_awakening_helmet_ready bonus only while the helmet
    // is worn AND the per-player cooldown (ThorEffects.isStormCooldown) has
    // elapsed. Pops visible when the ability is armed, disappears during the
    // 48000-tick (2 Minecraft days) cooldown window that follows a successful
    // trigger, then re-appears once cleared. Color 0x6858B8 matches the Thor
    // LORE_BASE palette (deep storm purple). Icon at
    // assets/famspecial/textures/mob_effect/storms_awakening.png is a copy of
    // vanilla bad_omen.png.
    public static final RegistryEntry<StatusEffect> STORMS_AWAKENING = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "storms_awakening"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x6858B8) {}
    );

    // Asgardian's Flight: Thor 5/5 gameplay gate for Mjolnir's right-click
    // riptide launch. Applied by ArmorEffects' asgardians_flight_full_thor_set_and_mjolnir
    // bonus only while all four Thor armor pieces are worn AND the main hand
    // carries THOR_MACE. Consumed by MjolnirMaceItem#use and
    // MjolnirMaceItem#onStoppedUsing — the riptide launch is short-circuited
    // unless this effect is active. Stripping is event-driven: the
    // LivingEntityEquipMixin handler drops it when any Thor armor slot flips;
    // the ServerPlayNetworkHandlerMixin handler drops it when the mainhand
    // stops carrying THOR_MACE. MOD_MANAGED membership is the 4-s-cycle safety
    // net for routes neither hook sees (inventory drag of mace, item break,
    // /item replace). Color 0x60C8FF matches the Thor LORE_ACCENT palette.
    // Icon at assets/famspecial/textures/mob_effect/asgardians_flight.png is a
    // byte-for-byte copy of vanilla item/elytra.png.
    public static final RegistryEntry<StatusEffect> ASGARDIANS_FLIGHT = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FamSpecial.MOD_ID, "asgardians_flight"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x60C8FF) {}
    );

    public static void register() {
        FamSpecial.LOGGER.info("Registering status effects for {}", FamSpecial.MOD_ID);
    }
}
