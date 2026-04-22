package github.rahularora375.famspecial.component;

import com.mojang.serialization.Codec;
import github.rahularora375.famspecial.FamSpecial;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModComponents {
    public static final ComponentType<Boolean> GRANTS_NIGHT_VISION = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "grants_night_vision"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Marks a piece as belonging to a named armor set. When all four equipped
    // pieces share the same SET_ID, ArmorEffects applies that set's bonus.
    public static final ComponentType<String> SET_ID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "set_id"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    // Marks a weapon as granting Strength I to its wielder while held at night
    // (or in Nether/End). Used by the Obsidian Dagger to restore full damage
    // from its half-damage default — matched in ArmorEffects.
    public static final ComponentType<Boolean> NIGHT_STRENGTH = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "night_strength"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Marks a weapon as healing its target on hit instead of dealing damage.
    // Handled in AttackHandlers: vanilla damage is cancelled and the target
    // (if LivingEntity) is healed by a fixed amount.
    public static final ComponentType<Boolean> HEALS_TARGET = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "heals_target"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Helmet flag granting Water Breathing while worn — mirrors the Night
    // Vision pattern. Bonus fires from ArmorEffects, driven by this flag on
    // the HEAD slot.
    public static final ComponentType<Boolean> GRANTS_WATER_BREATHING = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "grants_water_breathing"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Helmet flag granting Messmer's Flame Aegis (80% fire damage reduction
    // via LivingEntityMixin) while worn. Mirrors the Night Vision / Water
    // Breathing pattern — bonus fires from ArmorEffects, driven by this flag
    // on HEAD. Lives on Impaler's Crown; decoupled from the 4/4 set gate so
    // the flame resist can be worn a la carte.
    public static final ComponentType<Boolean> GRANTS_MESSMERS_FLAME = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "grants_messmers_flame"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Helmet flag: while worn, the client renders a floating HP bar above
    // every living entity in view (color-graded red/yellow/green by HP %).
    // Purely client-side cosmetic — no server effect. Wired in HealthOverlay
    // which reads this flag off the player's HEAD slot.
    public static final ComponentType<Boolean> SHOWS_ENTITY_HP = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "shows_entity_hp"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Weapon flag: suppresses the exhaustion cost that vanilla's Lunge enchant
    // normally applies via ApplyExhaustionEnchantmentEffect. Used by Fire
    // Serpent's Wrath so the spear can lunge freely without eating hunger.
    // Checked in ApplyExhaustionEnchantmentEffectMixin against the context's
    // stack — any enchant effect that reuses that class will also short-
    // circuit for a flagged stack, which is the intended generalization.
    public static final ComponentType<Boolean> NO_LUNGE_HUNGER = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "no_lunge_hunger"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Elytra-style durability cap: flagged stacks stop taking damage at
    // (maxDamage - 1) and never break. Enforced in ItemStackMixin on the
    // core damage(int, ServerWorld, ServerPlayerEntity, Consumer<Item>)
    // overload that every damage path funnels through.
    public static final ComponentType<Boolean> INDESTRUCTIBLE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "indestructible"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Passive durability regeneration while equipped: flagged stacks heal
    // one point of durability every 243 ticks. Handled by ArmorEffects'
    // 243-tick cadence block.
    public static final ComponentType<Boolean> REGENS_DURABILITY = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "regens_durability"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Mending-lockout flag. Blocks Mending from being applied to the stack
    // via anvil (AnvilScreenHandlerMixin blanks the output if the sacrifice
    // carries Mending) or enchanting table (EnchantmentHelperMixin filters
    // Mending out of the possible-entries roll).
    public static final ComponentType<Boolean> BLOCKS_MENDING = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "blocks_mending"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Main-hand weapon flag: grants Shardbearing (+5% current-HP bonus damage
    // on the wearer's melee hits, bypassing armor/protection/resistance) while
    // held. Lives on Oathbringer, the Knight Radiant signature sword. Two
    // consumers read this flag off the mainhand stack: ArmorEffects'
    // shardbearing_mainhand Bonus (applies the cosmetic SHARDBEARING HUD
    // badge) and LivingEntityMixin's modifyAppliedDamage ModifyReturnValue
    // (adds the current-HP chip onto final post-mitigation damage).
    public static final ComponentType<Boolean> GRANTS_SHARDBEARING = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "grants_shardbearing"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Chestplate flag granting 60% damage reduction from undead attackers
    // (melee + ranged). Enforced in LivingEntityMixin's modifyAppliedDamage
    // ModifyReturnValue, gated on the CHEST slot and on the attacker's type
    // being in EntityTypeTags.UNDEAD. source.getAttacker() attributes arrow
    // hits back to the shooter, so ranged undead (skeletons, strays) are
    // covered by the same branch.
    public static final ComponentType<Boolean> GRANTS_UNDEAD_RESISTANCE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "grants_undead_resistance"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Main-hand weapon flag: applies Wither II for 4s to the target on any
    // successful melee/projectile hit. Handled additively in AttackHandlers'
    // ALLOW_DAMAGE handler alongside the Messmer's Venom branch. Lives on
    // Thriller's Edge.
    public static final ComponentType<Boolean> APPLIES_WITHER_ON_HIT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "applies_wither_on_hit"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Per-stack Necromancer summon cooldown: absolute world-tick at which the
    // 5-min cooldown expires. Stamped on all 4 worn Necromancer pieces when
    // the summon fires in NecromancerSummon. Client reads this to render the
    // MM:SS countdown in the armor tooltip; server ignores it and keeps using
    // its own per-UUID map. Stays stamped after the cooldown expires (the
    // tooltip just hides once currentTime >= value).
    public static final ComponentType<Long> NECROMANCER_COOLDOWN_END = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "necromancer_cooldown_end"),
            ComponentType.<Long>builder().codec(Codec.LONG).build()
    );

    // Main-hand pickaxe flag: when set, every successful block break additionally
    // rolls vanilla's blocks/diamond_ore loot table with the same pickaxe as the
    // tool, at BONUS_DIAMOND_ROLL_CHANCE probability. Respects Fortune and Silk
    // Touch naturally because the vanilla table is used verbatim. Handled by
    // BlockBreakHandler, which gates on the player's main-hand stack.
    public static final ComponentType<Boolean> BONUS_DIAMOND_CHANCE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "bonus_diamond_chance"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Helmet flag granting Bad Omen ("Ominous Potion effect") while worn
    // between 8 PM and 6 AM (timeOfDay >= 14000, or always in Nether/End).
    // Applied by ArmorEffects driven by this flag on the HEAD slot. Lives on
    // the Esh-Endra-Navesh helmet.
    public static final ComponentType<Boolean> GRANTS_OMINOUS = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "grants_ominous"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Main-hand weapon flag: while the stack is held in the main hand, the
    // wielder is granted EMPERORS_DIVIDE. The effect itself causes
    // LivingEntityKnockbackMixin to bypass the target's KNOCKBACK_RESISTANCE
    // attribute so a hit always lands full-strength knockback. Lives on the
    // Shurima Sun Disc Spear. Applied by a per-tick fast-path in ArmorEffects
    // so the effect appears within 1 tick on swap in and drops within 1 tick
    // on swap out (tied to the weapon; won't carry over to other items).
    public static final ComponentType<Boolean> IGNORES_KB_RESISTANCE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "ignores_kb_resistance"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Helmet flag granting SUNS_PROTECTION (Resistance I ~ 20% damage reduction
    // via LivingEntityMixin's modifyAppliedDamage) while worn AND the player is
    // in a DESERT biome. Applied by ArmorEffects, gated on the HEAD slot and
    // the biome cache. Included in MOD_MANAGED: helmet removal is caught
    // instantly by LivingEntityEquipMixin; biome leave drops the effect within
    // one 4-s MOD_MANAGED diff pass. Lives on the Sun Emperor's Crown.
    public static final ComponentType<Boolean> GRANTS_SUNS_PROTECTION = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "grants_suns_protection"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Main-hand weapon flag: while held, every successful melee hit rolls a
    // chance (20% clear, 100% thundering) to strike the victim with a vanilla
    // LIGHTNING_BOLT. Lives on Mjolnir (the Thor mace). Handled in ThorEffects
    // via ServerLivingEntityEvents.AFTER_DAMAGE, gated on the attacker's main-
    // hand carrying this flag. Also drives the cosmetic GOD_OF_THUNDER HUD badge
    // via ArmorEffects' god_of_thunder_mainhand bonus so icon + mechanic share
    // one source of truth.
    public static final ComponentType<Boolean> LIGHTNING_ON_HIT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "lightning_on_hit"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Helmet flag: on any kill the wearer performs, roll a flat 8% chance to
    // start a thunderstorm — but ONLY when the world is not already thundering.
    // If a thunderstorm is already active the kill is ignored entirely (no roll,
    // no cooldown burn). Per-player 2-day cooldown only starts on a successful
    // trigger. Lives on Thunderhelm (the Thor helmet). Handled in ThorEffects
    // via ServerLivingEntityEvents.AFTER_DEATH. Drives the cosmetic
    // STORMS_AWAKENING HUD badge (visible only when off cooldown) via
    // ArmorEffects' storms_awakening_helmet_ready bonus.
    public static final ComponentType<Boolean> TRIGGERS_STORM_AWAKENING = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "triggers_storm_awakening"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    // Per-stack Storm's Awakening cooldown: absolute world-tick at which the
    // cooldown expires. Stamped on the worn Thunderhelm when Storm's Awakening
    // triggers. Client reads this to render the MM:SS countdown in the helmet
    // tooltip; server ignores it and keeps using its own per-UUID map. Stays
    // stamped after the cooldown expires (the tooltip just hides once
    // currentTime >= value). Mirrors NECROMANCER_COOLDOWN_END.
    public static final ComponentType<Long> STORM_COOLDOWN_END = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "storm_cooldown_end"),
            ComponentType.<Long>builder().codec(Codec.LONG).build()
    );

    // Main-hand weapon flag: marker identifying Mjolnir (the Thor mace)
    // specifically — not any MaceItem instance. Used by the 5/5 helper
    // (ArmorEffects.hasFullSetWithMjolnir) and by ThorEffects' right-click
    // riptide launch to confirm the held weapon is Mjolnir before engaging
    // the Thor-set bonus. Distinct from LIGHTNING_ON_HIT so that one component
    // could in theory be re-used on other Thor-themed weapons without making
    // them all count as Mjolnir for the 5/5 gate.
    public static final ComponentType<Boolean> THOR_MACE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FamSpecial.MOD_ID, "thor_mace"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    public static void register() {
        FamSpecial.LOGGER.info("Registering components for {}", FamSpecial.MOD_ID);
    }
}