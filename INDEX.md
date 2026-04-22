# INDEX.md

Source-file inventory. Each row: path (relative to `src/main/`) → one-line purpose → topic file that documents it. Use this when you don't know which topic file to load.

## Entrypoints

| File | Purpose | Doc |
|---|---|---|
| `java/github/rahularora375/famspecial/FamSpecial.java` | Main `ModInitializer`; wires 12 subsystems in order. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/client/FamSpecialClient.java` | Client `ModInitializer`; tooltip callbacks + client-side registrations. | `SYSTEMS.md` |

## Components & effects

| File | Purpose | Doc |
|---|---|---|
| `java/github/rahularora375/famspecial/component/ModComponents.java` | Custom `DataComponentType` registry (all `GRANTS_*`, `SET_ID`, `INDESTRUCTIBLE`, etc.). | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/effect/ModStatusEffects.java` | Custom status-effect registry (gameplay + cosmetic HUD badges). | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/sound/ModSounds.java` | Custom `SoundEvent` registry (`NECROMANCER_SUMMON`, `BONUS_DIAMOND`). | `SYSTEMS.md` |

## Item catalog

| File | Purpose | Doc |
|---|---|---|
| `java/github/rahularora375/famspecial/item/ModItemGroups.java` | Two creative tab registrations (`GEAR_GROUP` / `ARMOR_GROUP`) + `buildItem` / `loreLine` / `EnchantEntry` helpers. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/OldFamItems.java` | 13 legendary diamond pieces (split into `addWeapons` / `addTools` / `addArmor`). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/MistbornItems.java` | Atium Dagger + Mistborn 4-piece armor (day/night swap, night-gated Strength+Speed). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/PacifistItems.java` | Sage's Grace + Pacifist 4-piece armor (heal-on-hit, Regen set bonus). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/PoseidonItems.java` | Trident of Olympus + Poseidon 4-piece armor (Empowered Riptide set bonus). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/FireSerpentItems.java` | Fire Serpent's Wrath + Fire Serpent 4-piece armor (Messmer's Venom set bonus). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/NecromancerItems.java` | Thriller's Edge + Necromancer 4-piece armor (Zombie Reinforcements set bonus). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/KnightRadiantItems.java` | Oathbringer + Shard 4-piece armor (indestructible / regen / Radiant Might / Shardbearing). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/EshEndraNaveshItems.java` | Just Hit Bro pickaxe + Esh-Endra-Navesh 4-piece armor (Haste 4/4 + helmet-gated Bad Omen). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/ShurimaItems.java` | Sun Disc Spear + Shurima 4-piece armor (Shuriman Endurance 4/4 + helmet-gated Sun's Protection, both desert-gated; spear's Emperor's Divide bypasses target KB-resistance). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/entries/ThorItems.java` | Mjolnir + Thor 5-piece set (Thunderhelm + 3 armor + Mjolnir; God of Thunder, Storm's Awakening, Asgardian's Flight). | `ITEMS.md` |
| `java/github/rahularora375/famspecial/item/MjolnirMaceItem.java` | `MaceItem` subclass; right-click charges a dry-land riptide launch gated on `ASGARDIANS_FLIGHT`. | `SYSTEMS.md` |

## Runtime systems

| File | Purpose | Doc |
|---|---|---|
| `java/github/rahularora375/famspecial/item/ArmorEffects.java` | Server tick hub: `BONUSES` dispatch, 243-tick durability regen, main-hand attribute swap, `hasFullSet` helper. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/item/AttackHandlers.java` | `ALLOW_DAMAGE` (Messmer's Venom / Wither-on-hit) + `AttackEntityCallback` (heal-on-hit). | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/item/NecromancerSummon.java` | Event-driven 4/4 Necromancer summon: spawn, target-lock, friendly-fire gate, lifetime cleanup. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/item/ThorEffects.java` | Event-driven Thor handlers: lightning-on-hit roll (Mjolnir mainhand) + Storm's Awakening kill trigger (Thunderhelm) with per-player cooldown. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/item/BlockBreakHandler.java` | `PlayerBlockBreakEvents.AFTER` hook rolling bonus-diamond drops for `BONUS_DIAMOND_CHANCE`-flagged pickaxes. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/loot/ModLootTableModifier.java` | Hooks `LootTableEvents.MODIFY`; delegates to `LegendaryPool` / `MapsPool`. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/loot/LegendaryPool.java` | 4-tier legendary pool (Crazy/S/A/F) + guaranteed-Mending book. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/loot/MapsPool.java` | 4-tier maps pool (jungle temple / ancient city / woodland mansion). | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/loot/LootPoolHelpers.java` | `namedItem(...)` builder using loot functions; supports enchant ranges. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/client/HealthOverlay.java` | Client-only world-space HP bars driven by `SHOWS_ENTITY_HP` helmet flag. | `SYSTEMS.md` |
| `java/github/rahularora375/famspecial/net/VersionHandshake.java` | Login-phase version query on `famspecial:version`; strict-equality kick on mismatch. | `SYSTEMS.md` |

## Mixins

All under `java/github/rahularora375/famspecial/mixin/`. All documented in `MIXINS.md`.

| File | Purpose |
|---|---|
| `GrindstoneScreenHandlerMixin.java` | TAIL-blank grindstone result for `HEALS_TARGET` items. |
| `AnvilScreenHandlerMixin.java` | TAIL-blank anvil result for `HEALS_TARGET` or `BLOCKS_MENDING`+Mending combos. |
| `TridentItemMixin.java` | Dry-land riptide for Poseidon 4/4 (two `@Redirect`s on `isTouchingWaterOrRain`). |
| `LivingEntityMixin.java` | Four `@ModifyReturnValue`s on `modifyAppliedDamage` (Messmer's Flame Aegis / Shardbearing / Undead Resistance / Sun's Protection). |
| `LivingEntityKnockbackMixin.java` | `@ModifyExpressionValue` on `LivingEntity#takeKnockback` `getAttributeValue` invoke; zeroes `KNOCKBACK_RESISTANCE` lookup when attacker has `EMPERORS_DIVIDE` (Sun Disc Spear). |
| `PlayerEntityExhaustionMixin.java` | HEAD-cancel on `PlayerEntity#addExhaustion` while player has `SHURIMAN_ENDURANCE` (Shurima 4/4 saturation lock). |
| `ApplyExhaustionEnchantmentEffectMixin.java` | Cancels Lunge exhaustion when stack has `NO_LUNGE_HUNGER`. |
| `ItemStackMixin.java` | Clamps damage at `maxDamage-1` for `INDESTRUCTIBLE` stacks; plays break sound on transition. |
| `ItemStackAttributeMixin.java` | Cancels attribute modifiers for broken `INDESTRUCTIBLE` stacks. |
| `ItemStackEnchantmentsMixin.java` | Returns empty enchantments from `getEnchantments()` for broken `INDESTRUCTIBLE` stacks (tooltip path). |
| `EnchantmentHelperMixin.java` | Filters Mending out of `getPossibleEntries` for `BLOCKS_MENDING` stacks. |
| `EnchantmentHelperForEachMixin.java` | Cancels both `forEachEnchantment` overloads for broken `INDESTRUCTIBLE` stacks (effect-dispatch path). |
| `MobEntityAccessor.java` | `@Accessor` interface widening `MobEntity.targetSelector` for Necromancer target swap. |
| `LivingEntityEquipMixin.java` | HEAD-inject on `onEquipStack`; calls `ArmorEffects.refreshBonusesFor(player)` once — handles both APPLY and STRIP via the MOD_MANAGED diff, no per-effect enumeration. |
| `ServerPlayNetworkHandlerMixin.java` | TAIL-inject on `onUpdateSelectedSlot`; calls `ArmorEffects.refreshBonusesFor(player)` for the hotbar-swap case. |
| `ServerPlayNetworkHandlerClickSlotMixin.java` | TAIL-inject on `onClickSlot` (survival drag / shift-click / armor-swap-key) AND `onCreativeInventoryAction` (creative inventory tab drag); calls `ArmorEffects.refreshBonusesFor(player)` in each. Creative-tab armor drag does NOT route through `onClickSlot`, so the second hook is load-bearing — without it, creative-mode HUD icons for armor-driven effects lagged up to 4 s. |

## Resources & config

| File | Purpose | Doc |
|---|---|---|
| `resources/fabric.mod.json` | Fabric manifest (main + client entrypoints, mixins pointer, `${version}` from gradle). | `SYSTEMS.md` (handshake section for version flow) |
| `resources/famspecial.mixins.json` | SpongePowered Mixin manifest (16 entries). | `MIXINS.md` |
| `resources/assets/famspecial/sounds.json` | Maps sound event ids to OGG asset paths. | `SYSTEMS.md` |
| `resources/assets/famspecial/sounds/*.ogg` | Custom sound assets (`zombiezzz.ogg`, `lucky.ogg`, `raj_totem.ogg`). | `SYSTEMS.md` |
| `resources/assets/famspecial/lang/en_us.json` | Lang keys for effects, items, item group. | `SYSTEMS.md` |
| `resources/assets/famspecial/textures/mob_effect/*.png` | Byte-for-byte vanilla-icon copies for custom status effects — do not author custom art. | `SYSTEMS.md` |
| `resources/data/famspecial/tags/worldgen/structure/on_ancient_city_maps.json` | Structure tag backing the Ancient City explorer map destination. | `SYSTEMS.md` |
| `gradle.properties` | `mod_version` (filtered into `fabric.mod.json`). Bump for releases. | `CLAUDE.md` (Commands) |
