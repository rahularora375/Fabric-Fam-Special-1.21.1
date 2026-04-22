package github.rahularora375.famspecial.client;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import github.rahularora375.famspecial.net.VersionHandshake;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

public class FamSpecialClient implements ClientModInitializer {
    // Mistborn palette. Gold italic is the "active" state — the underlying
    // server-side bonus is currently firing for the local player. Partial /
    // inactive states use base + silver so the gold stands out on activation.
    private static final Style LORE_BASE = Style.EMPTY
            .withColor(TextColor.fromRgb(0x9AA5C4)).withItalic(true);
    private static final Style LORE_SILVER = Style.EMPTY
            .withColor(TextColor.fromRgb(0xD4DCE8)).withItalic(true);
    private static final Style LORE_ACTIVE = Style.EMPTY
            .withColor(TextColor.fromRgb(0xFFD76A)).withItalic(true);

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            String setId = stack.get(ModComponents.SET_ID);
            if ("mistborn".equals(setId)) {
                // Attribute values render natively in the vanilla attributes block.
                int count = countEquippedWithSetId("mistborn");
                boolean night = isNightOrTimeless();
                boolean full = count == 4;
                lines.add(buildSetLine("Mistborn Set", count, full && night));
                lines.add(grantLine("Grants ", "Night Strength", full && night));
                lines.add(grantLine("Grants ", "Night Speed", full && night));
            }
            if ("pacifist".equals(setId)) {
                // Attribute values render natively in the vanilla attributes block. No time gate on any Pacifist grant.
                int count = countEquippedWithSetId("pacifist");
                boolean active = count == 4;
                lines.add(buildSetLine("Pacifist Set", count, active));
                lines.add(grantLine("Grants ", "Regeneration", active));
            }
            if ("poseidon".equals(setId)) {
                // No time gate. +1 heart per piece renders natively in attributes.
                // Empowered Riptide is the 4/4 set bonus — always considered
                // active when the full set is worn, since the player can't
                // trigger riptide without a riptide trident in hand anyway.
                int count = countEquippedWithSetId("poseidon");
                boolean full = count == 4;
                lines.add(buildSetLine("Poseidon Set", count, full));
                lines.add(grantLine("Grants ", "Empowered Riptide", full));
            }
            if ("knight_radiant".equals(setId)) {
                // No time gate. 4/4 bonus is Radiant Might (+1 ATTACK_DAMAGE
                // via the effect's chained attribute modifier). Stormlight is
                // an any-piece aura and is surfaced on its own line below.
                int count = countEquippedWithSetId("knight_radiant");
                boolean full = count == 4;
                lines.add(buildSetLine("Knight Radiant Set", count, full));
                lines.add(grantLine("Grants ", "Radiant Might", full));
            }
            if ("fire_serpent".equals(setId)) {
                // No time gate. +0.5 heart and +0.0025 move speed per piece
                // render natively in attributes. 4/4 bonus is Messmer's Venom
                // — applied as a cosmetic aura to the wearer and per melee hit
                // to targets (bypasses vanilla poison immunities, bare-hands
                // capable). Messmer's Flame Aegis is a helmet-alone bonus and is
                // surfaced on its own line below.
                int count = countEquippedWithSetId("fire_serpent");
                boolean full = count == 4;
                lines.add(buildSetLine("Fire Serpent Set", count, full));
                lines.add(grantLine("Grants ", "Messmer's Venom", full));
            }
            if ("esh_endra_navesh".equals(setId)) {
                // No time gate on the Haste set bonus. +0.5 heart per piece
                // renders natively in the attributes block. Bad Omen lives on
                // the helmet alone and surfaces on its own grant line below.
                int count = countEquippedWithSetId("esh_endra_navesh");
                boolean full = count == 4;
                lines.add(buildSetLine("Esh-Endra-Navesh Set", count, full));
                lines.add(grantLine("Grants ", "Haste", full));
            }
            if ("shurima".equals(setId)) {
                // Shurima Set: +0.5 heart, +0.0025 move speed, +0.075 KB
                // resistance per piece render natively in the attributes block.
                // 4/4 bonus is Shuriman Endurance (saturation lock) gated on
                // any desert-family biome (desert / badlands / eroded badlands)
                // — surfaced as the "Grants" line, gold when 4/4 is worn AND
                // the player is currently in one of those biomes.
                int count = countEquippedWithSetId("shurima");
                boolean full = count == 4;
                boolean inDesert = isInDesert();
                lines.add(buildSetLine("Shurima Set", count, full && inDesert));
                lines.add(grantLine("Grants ", "Shuriman Endurance", full && inDesert));
            }
            if ("thor".equals(setId)) {
                // Thor Set: vanilla diamond base (no per-piece attribute
                // modifiers). 5/5 gate (4 armor + Mjolnir in main hand)
                // unlocks the right-click riptide launch inside
                // MjolnirMaceItem, gated by the ASGARDIANS_FLIGHT effect.
                // The set line counts Mjolnir as the 5th piece so the line
                // reads (N/5) — so 4/5 means "full armor, no mace" (silver),
                // 5/5 means "full armor + mace" (gold).
                int count = countThorPieces();
                boolean full5 = count == 5;
                lines.add(buildSetLine("Thor Set", count, 5, full5));
                boolean flightActive = hasEffect(ModStatusEffects.ASGARDIANS_FLIGHT);
                lines.add(grantLine("Grants ", "Asgardian's Flight", flightActive));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.THOR_MACE))) {
                // Mjolnir-specific set line: Mjolnir is the 5th piece of the
                // Thor set. Parallel to the armor branch above so the mace
                // tooltip also advertises 5/5 progress. Gold only when
                // ASGARDIANS_FLIGHT is actually active on the player (i.e.
                // armor + mace simultaneously equipped). The "Grants God of
                // Thunder" line is emitted by the LIGHTNING_ON_HIT branch
                // below — we don't duplicate it here.
                int count = countThorPieces();
                boolean full5 = count == 5;
                lines.add(buildSetLine("Thor Set", count, 5, full5));
                boolean flightActive = hasEffect(ModStatusEffects.ASGARDIANS_FLIGHT);
                lines.add(grantLine("Grants ", "Asgardian's Flight", flightActive));
            }
            if ("necromancer".equals(setId)) {
                // No time gate. +0.5 heart per piece renders natively in
                // attributes. 4/4 bonus is Rotten Muscle — the summon charge
                // state. Rotten Muscle only reads "active" (gold) when the
                // summon is READY; during cooldown the Grants line carries
                // a crimson "MM:SS" suffix read off NECROMANCER_COOLDOWN_END
                // (stamped server-side when the summon fires), so the
                // player can see remaining time next to the effect name.
                int count = countEquippedWithSetId("necromancer");
                boolean full = count == 4;
                long cdRemaining = necromancerCooldownRemaining(stack);
                lines.add(buildSetLine("Necromancer Set", count, full));
                lines.add(grantLineWithCooldown("Grants ", "Rotten Muscle", full && cdRemaining <= 0L, cdRemaining));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.GRANTS_NIGHT_VISION))) {
                // Night Vision fires whenever the helmet is worn — no time gate.
                boolean active = isWornWithFlag(EquipmentSlot.HEAD, ModComponents.GRANTS_NIGHT_VISION);
                lines.add(grantLine("Grants ", "Night Vision", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.GRANTS_WATER_BREATHING))) {
                // Water Breathing fires whenever the helmet is worn — no time gate.
                boolean active = isWornWithFlag(EquipmentSlot.HEAD, ModComponents.GRANTS_WATER_BREATHING);
                lines.add(grantLine("Grants ", "Water Breathing", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.GRANTS_OMINOUS))) {
                // Bad Omen fires only when the helmet is worn at night (or in
                // Nether/End) — mirrors the server-side gate in ArmorEffects
                // (ominous_helmet bonus), which reuses the Mistborn night gate.
                boolean active = isWornWithFlag(EquipmentSlot.HEAD, ModComponents.GRANTS_OMINOUS)
                        && isNightOrTimeless();
                lines.add(grantLine("Grants ", "Bad Omen", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.GRANTS_MESSMERS_FLAME))) {
                // Messmer's Flame Aegis fires whenever the chestplate is worn — no time gate.
                boolean active = isWornWithFlag(EquipmentSlot.CHEST, ModComponents.GRANTS_MESSMERS_FLAME);
                lines.add(grantLine("Grants ", "Messmer's Flame Aegis", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.SHOWS_ENTITY_HP))) {
                // Health Vision fires whenever the helmet is worn — no time gate.
                boolean active = isWornWithFlag(EquipmentSlot.HEAD, ModComponents.SHOWS_ENTITY_HP);
                lines.add(grantLine("Grants ", "Healer's Vision", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.NIGHT_STRENGTH))) {
                // Night Strength fires only when held in the MAIN hand at night
                // or in Nether/End — mirrors the server-side gate in ArmorEffects.
                boolean active = isMainHandWithFlag(ModComponents.NIGHT_STRENGTH) && isNightOrTimeless();
                lines.add(grantLine("Grants ", "Nightfall's Might", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.HEALS_TARGET))) {
                // Sage's Grace fires whenever the stack is in the main hand
                // — mirrors AttackHandlers gate on same flag.
                boolean active = isMainHandWithFlag(ModComponents.HEALS_TARGET);
                lines.add(grantLine("Grants ", "Sage's Grace", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.NO_LUNGE_HUNGER))) {
                // Tireless Lunge fires whenever the stack is in the main hand
                // — mirrors the stack-flag gate in ApplyExhaustionEnchantmentEffectMixin.
                boolean active = isMainHandWithFlag(ModComponents.NO_LUNGE_HUNGER);
                lines.add(grantLine("Grants ", "Tireless Lunge", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.REGENS_DURABILITY))) {
                // Stormlight fires whenever this piece is worn in its own slot
                // — mirrors the per-slot gate in ArmorEffects' stormlight_any_piece
                // bonus. Each Shard piece lights up independently; any one
                // equipped piece is enough to trigger the aura server-side.
                boolean active = isEquippedSomewhereWithFlag(ModComponents.REGENS_DURABILITY);
                lines.add(grantLine("Grants ", "Stormlight", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.GRANTS_SHARDBEARING))) {
                // Shardbearing fires whenever Oathbringer is in the main hand
                // — mirrors the server-side gate in ArmorEffects' shardbearing_mainhand
                // bonus (HUD badge) and LivingEntityMixin's modifyAppliedDamage
                // handler (+5% current-HP chip past all mitigation). Purely
                // cosmetic status-effect badge; the chip is mixin-driven.
                boolean active = isMainHandWithFlag(ModComponents.GRANTS_SHARDBEARING);
                lines.add(grantLine("Grants ", "Shardbearing", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.GRANTS_UNDEAD_RESISTANCE))) {
                // Undead Resistance fires whenever the chestplate (Smooth
                // Criminal's Vestment) is worn — no time gate. Mirrors the
                // server-side gate in LivingEntityMixin's
                // famspecial$undeadResistance handler, which reads the same
                // flag off the CHEST slot.
                boolean active = isWornWithFlag(EquipmentSlot.CHEST, ModComponents.GRANTS_UNDEAD_RESISTANCE);
                lines.add(grantLine("Grants ", "Undead Resistance", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.APPLIES_WITHER_ON_HIT))) {
                // Wither Touch fires whenever the stack is in the main hand
                // — mirrors the weapon-stack gate in AttackHandlers'
                // ALLOW_DAMAGE handler (reads the same flag off
                // source.getWeaponStack()).
                boolean active = isMainHandWithFlag(ModComponents.APPLIES_WITHER_ON_HIT);
                lines.add(grantLine("Grants ", "Wither Touch", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.GRANTS_SUNS_PROTECTION))) {
                // Sun's Protection fires when the helmet is worn AND the
                // player is in any desert-family biome (desert / badlands /
                // eroded badlands) — mirrors the server gate in ArmorEffects.
                // Matches the other desert-gated tooltip; state flips in
                // sync with the server gate via the MOD_MANAGED diff pass.
                boolean active = isWornWithFlag(EquipmentSlot.HEAD, ModComponents.GRANTS_SUNS_PROTECTION)
                        && isInDesert();
                lines.add(grantLine("Grants ", "Sun's Protection", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.IGNORES_KB_RESISTANCE))) {
                // Emperor's Divide fires whenever the stack is in the main hand
                // — the per-tick fast-path in ArmorEffects applies it every
                // tick the spear is held and strips it the tick after swap.
                boolean active = isMainHandWithFlag(ModComponents.IGNORES_KB_RESISTANCE);
                lines.add(grantLine("Grants ", "Emperor's Divide", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.LIGHTNING_ON_HIT))) {
                // God of Thunder fires whenever Mjolnir is in the main hand —
                // mirrors the server-side gate in ThorEffects.onAfterDamage
                // (reads LIGHTNING_ON_HIT off the attacker's main-hand stack).
                // The lightning roll is weather-aware (20% clear / 100% storm)
                // but the tooltip just reflects "weapon is active."
                boolean active = isMainHandWithFlag(ModComponents.LIGHTNING_ON_HIT);
                lines.add(grantLine("Grants ", "God of Thunder", active));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.TRIGGERS_STORM_AWAKENING))) {
                // Storm's Awakening fires whenever Thunderhelm is worn in the
                // HEAD slot — mirrors the server-side gate in
                // ThorEffects.onAfterDeath. The Grants line carries a crimson
                // MM:SS suffix read off STORM_COOLDOWN_END (stamped server-
                // side when the storm triggers), so the tooltip shows
                // remaining cooldown while it ticks down. Mirrors the
                // Rotten Muscle pattern above.
                long cdRemaining = stormCooldownRemaining(stack);
                boolean active = isWornWithFlag(EquipmentSlot.HEAD, ModComponents.TRIGGERS_STORM_AWAKENING)
                        && cdRemaining <= 0L;
                lines.add(grantLineWithCooldown("Grants ", "Storm's Awakening", active, cdRemaining));
            }
            if (Boolean.TRUE.equals(stack.get(ModComponents.BONUS_DIAMOND_CHANCE))) {
                // Shadi Buff fires while the pickaxe is in the main hand —
                // mirrors the BlockBreakHandler gate (bonus-diamond roll) and
                // the ArmorEffects shadi_buff_mainhand HUD bonus, which both
                // key off the same flag. Respects Fortune and Silk Touch.
                boolean active = isMainHandWithFlag(ModComponents.BONUS_DIAMOND_CHANCE);
                lines.add(grantLine("Grants ", "Shadi Buff", active));
            }
        });

        HealthOverlay.register();
        VersionHandshake.registerClient();

        FamSpecial.LOGGER.info("Initialized client for {} v{}", FamSpecial.MOD_ID, FamSpecial.MOD_VERSION);
    }

    private static Text grantLine(String prefix, String keyword, boolean active) {
        if (active) {
            return Text.literal(prefix + keyword).setStyle(LORE_ACTIVE);
        }
        return Text.literal(prefix).setStyle(LORE_BASE)
                .append(Text.literal(keyword).setStyle(LORE_SILVER));
    }

    // Muted crimson to match the Necromancer lore-accent palette. Italic so
    // it blends into the surrounding tooltip italics rather than looking like
    // a stat number.
    private static final Style LORE_COOLDOWN = Style.EMPTY
            .withColor(TextColor.fromRgb(0x8B0000)).withItalic(true);

    // Returns the number of ticks left on the Necromancer summon cooldown for
    // the given stack, or 0 if no cooldown is stamped or it has already
    // expired. Reads NECROMANCER_COOLDOWN_END (an absolute world tick) and
    // compares against the client world's current time.
    private static long necromancerCooldownRemaining(ItemStack stack) {
        Long end = stack.get(ModComponents.NECROMANCER_COOLDOWN_END);
        if (end == null) return 0L;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return 0L;
        long remaining = end - world.getTime();
        return Math.max(0L, remaining);
    }

    // Same shape as necromancerCooldownRemaining, reading the Thunderhelm-
    // stamped STORM_COOLDOWN_END for Storm's Awakening.
    private static long stormCooldownRemaining(ItemStack stack) {
        Long end = stack.get(ModComponents.STORM_COOLDOWN_END);
        if (end == null) return 0L;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return 0L;
        long remaining = end - world.getTime();
        return Math.max(0L, remaining);
    }

    private static String formatCooldown(long ticks) {
        long seconds = (ticks + 19L) / 20L;
        long mins = seconds / 60L;
        long secs = seconds % 60L;
        return String.format("%d:%02d", mins, secs);
    }

    // Same as grantLine but appends " · MM:SS" in crimson italic when
    // cooldownTicks > 0 so the Grants line doubles as the cooldown readout.
    private static Text grantLineWithCooldown(String prefix, String keyword, boolean active, long cooldownTicks) {
        Text base = grantLine(prefix, keyword, active);
        if (cooldownTicks <= 0L) return base;
        return Text.empty().append(base)
                .append(Text.literal(" · " + formatCooldown(cooldownTicks)).setStyle(LORE_COOLDOWN));
    }

    private static Text buildSetLine(String setName, int count, boolean active) {
        return buildSetLine(setName, count, 4, active);
    }

    // Max-aware overload — used by the Thor 5/5 set (4 armor + Mjolnir) so
    // the denominator reads "/5" instead of "/4". Existing 4-slot sets keep
    // calling the base method which delegates here with max=4 so no call
    // sites need to change.
    private static Text buildSetLine(String setName, int count, int max, boolean active) {
        if (active) {
            return Text.literal("Part of the " + setName + " (" + max + "/" + max + ")").setStyle(LORE_ACTIVE);
        }
        if (count > 0) {
            return Text.literal("Part of the ").setStyle(LORE_BASE)
                    .append(Text.literal(setName).setStyle(LORE_SILVER))
                    .append(Text.literal(" (" + count + "/" + max + ")").setStyle(LORE_BASE));
        }
        return Text.literal("Part of the ").setStyle(LORE_BASE)
                .append(Text.literal(setName).setStyle(LORE_SILVER));
    }

    // Counts Thor pieces the local player currently holds: 1 per worn armor
    // slot whose SET_ID is "thor", plus 1 if the main-hand stack carries the
    // THOR_MACE flag. Max is 5. Used by the Thor set-line in both the armor
    // tooltip and the Mjolnir tooltip so 5/5 requires all four armor pieces
    // AND Mjolnir in the main hand — matching the ASGARDIANS_FLIGHT gate.
    private static int countThorPieces() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return 0;
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if ("thor".equals(player.getEquippedStack(slot).get(ModComponents.SET_ID))) count++;
        }
        if (Boolean.TRUE.equals(player.getEquippedStack(EquipmentSlot.MAINHAND).get(ModComponents.THOR_MACE))) {
            count++;
        }
        return count;
    }

    // Client-side check whether the local player has the given status effect
    // active. Reads ClientPlayerEntity#hasStatusEffect which tracks effects
    // via the S2C effect packets vanilla already sends to the owning client.
    private static boolean hasEffect(RegistryEntry<StatusEffect> effect) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        return player.hasStatusEffect(effect);
    }

    private static int countEquippedWithSetId(String setId) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return 0;
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (setId.equals(player.getEquippedStack(slot).get(ModComponents.SET_ID))) count++;
        }
        return count;
    }

    private static boolean isWornWithFlag(EquipmentSlot slot, ComponentType<Boolean> flag) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        return Boolean.TRUE.equals(player.getEquippedStack(slot).get(flag));
    }

    private static boolean isMainHandWithFlag(ComponentType<Boolean> flag) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        return Boolean.TRUE.equals(player.getEquippedStack(EquipmentSlot.MAINHAND).get(flag));
    }

    // Mirrors the server-side stormlight_any_piece gate: true if any of the
    // four armor slots carries the flag. Used by the Stormlight tooltip so
    // every Shard piece's "Grants Stormlight" line lights up whenever the
    // server aura would fire — i.e. any Shard piece is worn.
    private static boolean isEquippedSomewhereWithFlag(ComponentType<Boolean> flag) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (Boolean.TRUE.equals(player.getEquippedStack(slot).get(flag))) return true;
        }
        return false;
    }

    // Mirrors the server-side biome cache in ArmorEffects: true iff the local
    // player's current block position is in any desert-family biome (classic
    // desert, badlands, or eroded badlands). Used by the Shurima tooltip
    // lines (Sun's Protection, Shuriman Endurance) so their gold/silver
    // state flips in sync with the server gate. Method name kept as
    // isInDesert to match call sites; semantic is now "desert-family."
    private static boolean isInDesert() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (player == null || world == null) return false;
        var biome = world.getBiome(player.getBlockPos());
        return biome.matchesKey(BiomeKeys.DESERT)
                || biome.matchesKey(BiomeKeys.BADLANDS)
                || biome.matchesKey(BiomeKeys.ERODED_BADLANDS);
    }

    // Mirrors the server-side gate in ArmorEffects: Nether/End always count as
    // "dark", otherwise check whether the current world's time is past dusk.
    private static boolean isNightOrTimeless() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return false;
        RegistryKey<World> dim = world.getRegistryKey();
        if (dim == World.NETHER || dim == World.END) return true;
        return (world.getTimeOfDay() % 24000) >= 12000;
    }

}
