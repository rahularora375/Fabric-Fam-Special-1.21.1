# EFFECTS.md

Audit of every status effect this mod applies to players — both custom-registered (`ModStatusEffects`) and vanilla effects re-applied by mod logic.

## Columns

- **Effect name** — display name
- **Source** — what triggers application
- **Tied to** — `Player` (persists after source removed, beacon-style) / `Armor slot` (gone when piece removed) / `Full set` (gone when set incomplete) / `Main hand` (gone when weapon swapped)
- **Set duration on apply** — ticks the effect is set for when conditions met (20 t = 1 s)
- **Re-check interval** — how often the trigger predicate is polled
- **Event hook** — `Hotbar slot packet` / `Armor slot change` / `Summon fire` / `—` (em-dash if none). Multiple hooks comma-separated.
- **Removed instantly?** — `Yes (event)` / `Yes (next 4-s cycle)` (= MOD_MANAGED) / `No` (natural decay)
- **Mechanic** — `Cosmetic` (HUD badge / particles only) or one-line description of the actual gameplay hook

---

## Table

| Effect name | Source (trigger) | Tied to | Set duration on apply | Re-check interval | Event hook | Removed instantly? | Mechanic |
|---|---|---|---|---|---|---|---|
| Night Strength | Main-hand `NIGHT_STRENGTH` (Obsidian Dagger) + night/Nether/End | Main hand | 400 t (20 s) | 80 t (4 s) | — | No (natural decay ~20 s) | Cosmetic HUD badge (real day/night attribute swap is on the stack itself). Removed from `MOD_MANAGED`; icon re-sourced from vanilla `wither_rose.png` |
| Strength I | Mistborn 4/4 + night/Nether/End | Full set | 400 t (20 s) | 80 t (4 s) | — | No (vanilla, natural decay) | Vanilla Strength I attribute |
| Speed I | Mistborn 4/4 + night/Nether/End | Full set | 400 t (20 s) | 80 t (4 s) | — | No (vanilla, natural decay) | Vanilla Speed I attribute |
| Night Vision | Helmet `GRANTS_NIGHT_VISION` | Armor slot (head) | 400 t (20 s) | 80 t (4 s) | — | No (vanilla, natural decay) | Vanilla night vision |
| Regeneration I | Pacifist 4/4 | Full set | 400 t (20 s) | 80 t (4 s) | — | No (vanilla, natural decay) | Vanilla regen ticks |
| Healer's Vision | Helmet `SHOWS_ENTITY_HP` (Sage's Crown) | Armor slot (head) | 400 t (20 s) | 80 t (4 s) | Armor slot change | **Yes (event)** on armor change; otherwise next cycle | Cosmetic HUD badge (HP bars rendered client-side, component-driven) |
| Sage's Grace | Main-hand `HEALS_TARGET` (Sage's Grace sword) | Main hand | 400 t (20 s) | 80 t (4 s) | Hotbar slot packet, click-slot / creative-drag | **Yes (event)** on hotbar swap or inventory click; otherwise next cycle | Cosmetic HUD badge (heal-on-hit gameplay gated by component in `AttackHandlers`); tooltip reads "Grants Sage's Grace" (renamed from the older "Pacifist Strike" label). Color `0x9FE59F`, icon copied from vanilla `item/golden_apple.png` |
| Water Breathing | Helmet `GRANTS_WATER_BREATHING` (Poseidon) | Armor slot (head) | 400 t (20 s) | 80 t (4 s) | — | No (vanilla, natural decay) | Vanilla water breathing |
| Empowered Riptide | Poseidon 4/4 | Full set | 400 t (20 s) | 80 t (4 s) | Armor slot change | **Yes (event)** on armor change; otherwise next cycle | Cosmetic (dry-land riptide gated by `hasFullSet("poseidon")` in `TridentItemMixin`) |
| Messmer's Flame Aegis | Chest `GRANTS_MESSMERS_FLAME` (Serpent's Embrace) | Armor slot (chest) | 400 t (20 s) | 80 t (4 s) | Armor slot change | **Yes (event)** on armor change; otherwise next cycle | **80% incoming fire-damage reduction** via `LivingEntityMixin` (×0.2) |
| Messmer's Venom (wearer aura) | Fire Serpent 4/4 | Full set | 400 t (20 s) | 80 t (4 s) | — | Yes (next 4-s cycle) | Cosmetic on wearer; read by `AttackHandlers` to inflict Venom on melee targets |
| Tireless Lunge | Main-hand `NO_LUNGE_HUNGER` (Fire Serpent's Wrath) | Main hand | 400 t (20 s) | 80 t (4 s) | Hotbar slot packet | **Yes (event)** on hotbar swap; otherwise next cycle | Cosmetic (Lunge exhaustion skip gated by component) |
| Rotten Muscle | Necromancer 4/4 + summon off cooldown | Full set (+ gate) | 400 t (20 s) | 80 t + event-strip on summon | Summon fire, Armor slot change | **Yes (event)** on summon or armor change; otherwise next cycle | Cosmetic charge-ready indicator + soul particle aura |
| Undead Resistance | Chest `GRANTS_UNDEAD_RESISTANCE` (Smooth Criminal's Vestment) | Armor slot (chest) | 400 t (20 s) | 80 t (4 s) | Armor slot change | **Yes (event)** on armor change; otherwise next cycle | **60% reduction vs undead** via `LivingEntityMixin` (×0.4) + smoke aura |
| Wither Touch | Main-hand `APPLIES_WITHER_ON_HIT` (Thriller's Edge) | Main hand | 400 t (20 s) | 80 t (4 s) | Hotbar slot packet | **Yes (event)** on hotbar swap; otherwise next cycle | Cosmetic (Wither II on-hit gated by component in `AttackHandlers`) |
| Stormlight | Any armor slot `REGENS_DURABILITY` (Knight Radiant) | Armor slot (any) | 400 t (20 s) | 80 t (4 s) | — | Yes (next 4-s cycle) | Cosmetic snowflake aura (durability regen lives in 243-tick block) |
| Radiant Might | Knight Radiant 4/4 | Full set | 400 t (20 s) | 80 t (4 s) | — | No (natural decay ~20 s) | **+2.0 Attack Damage** via chained attribute modifier on the effect. Removed from `MOD_MANAGED` |
| Shardbearing | Main-hand `GRANTS_SHARDBEARING` (Oathbringer) | Main hand | 400 t (20 s) | 80 t (4 s) | — | Yes (next 4-s cycle) | Cosmetic (+7% current-HP bonus damage gated by component in `LivingEntityMixin`) |
| Bad Omen | Esh-Endra-Navesh helmet `GRANTS_OMINOUS` + night/Nether/End | Armor slot (head, gated) | 400 t (20 s) | 80 t (4 s) | — | No (vanilla, natural decay) | Vanilla Bad Omen (raid trigger) |
| Haste I | Esh-Endra-Navesh 4/4 | Full set | 400 t (20 s) | 80 t (4 s) | — | No (vanilla, natural decay) | Vanilla mining/attack speed |
| Shadi Buff | Main-hand `BONUS_DIAMOND_CHANCE` (Just Hit Bro pickaxe) | Main hand | 400 t (20 s) | 80 t (4 s) | — | Yes (next 4-s cycle) | Cosmetic (diamond roll gated by component in `BlockBreakHandler`) |
| **Emperor's Divide** | Main-hand `IGNORES_KB_RESISTANCE` (Sun Disc Spear) | Main hand | 400 t (20 s) | 80 t + per-slot-swap event | Hotbar slot packet | **Yes (event)** on hotbar swap; otherwise next cycle | **Bypasses target KB-resistance** via `LivingEntityKnockbackMixin` |
| **Sun's Protection** | Shurima helmet `GRANTS_SUNS_PROTECTION` + desert-family biome (DESERT / BADLANDS / ERODED_BADLANDS) | Armor slot (head) + biome | 400 t (20 s) | 80 t (4 s) | — | No (natural decay ~20 s) | **20% incoming-damage reduction** (×0.8) via `LivingEntityMixin`. Removed from `MOD_MANAGED` |
| **Shuriman Endurance** | Shurima 4/4 + desert-family biome (DESERT / BADLANDS / ERODED_BADLANDS) | Player (beacon-style) | **4800 t (4 min)**; refresh only when < 4720 t | 80 t (4 s), apply-once-gated | — | No (natural decay when timer expires) | **No saturation loss** — all `PlayerEntity#addExhaustion` cancelled at HEAD via `PlayerEntityExhaustionMixin` |
| **God of Thunder** | Main-hand `LIGHTNING_ON_HIT` (Mjolnir) | Main hand | 400 t (20 s) | 80 t + per-slot-swap event | Hotbar slot packet | **Yes (event)** on hotbar swap; otherwise next cycle | Cosmetic HUD badge; drives **lightning-on-hit roll** (20% clear / 100% thundering) via `ThorEffects.AFTER_DAMAGE` gated on same main-hand flag |
| **Storm's Awakening** | Helmet `TRIGGERS_STORM_AWAKENING` (Thunderhelm) + `!ThorEffects.isStormCooldown` | Armor slot (head, cooldown-gated) | 400 t (20 s) | 80 t (4 s) | — | Yes (next 4-s cycle) | Cosmetic "ability armed" indicator; drives **kill → start 4800-t (~4 min) thunderstorm** via `ThorEffects.AFTER_DEATH` (current trigger chance `1.00f` is a flagged TESTING VALUE pending ship), skipping the kill entirely if `world.isThundering()` is already true (no roll, no cooldown burn). **24000-t (1 MC day)** per-player cooldown only arms on successful trigger; stamped onto the worn Thunderhelm via `STORM_COOLDOWN_END` for tooltip MM:SS countdown (via `grantLineWithCooldown`) |
| **Asgardian's Flight** | Mjolnir (mainhand) + 4 Thor armor | Full set + Main hand | 400 t (20 s) | 80 t (4 s) | Hotbar slot packet, Armor slot change | **Yes (event)** on hotbar swap or armor change; otherwise next cycle | Gates Mjolnir's right-click riptide launch in `MjolnirMaceItem.onStoppedUsing` |

---

## Notable patterns

- **Duration uniformity.** Every BONUSES entry sets 400 t (20 s) except Shuriman Endurance at 4800 t (4 min) with apply-once gating so leaving the desert doesn't reset the timer.
- **MOD_MANAGED has 16 members.** Those vanish within one 4-s dispatch cycle when the trigger drops. Members: `MESSMERS_FLAME_AEGIS`, `MESSMERS_VENOM`, `HEALERS_VISION`, `EMPOWERED_RIPTIDE`, `TIRELESS_LUNGE`, `STORMLIGHT`, `SHARDBEARING`, `ROTTEN_MUSCLE`, `UNDEAD_RESISTANCE`, `WITHER_TOUCH`, `SAGES_GRACE`, `SHADI_BUFF`, `EMPERORS_DIVIDE`, `GOD_OF_THUNDER`, `STORMS_AWAKENING`, `ASGARDIANS_FLIGHT`. **Three custom effects were deliberately removed from `MOD_MANAGED`** and now decay naturally (~20 s after the gate drops): `NIGHT_STRENGTH`, `RADIANT_MIGHT`, `SUNS_PROTECTION`. The 7 vanilla effects (Night Vision, Strength, Speed, Regeneration, Water Breathing, Bad Omen, Haste) and the 1 beacon-style Shuriman Endurance also decay naturally.
- **Instant-apply / instant-strip via `ArmorEffects.refreshBonusesFor(player)`.** Three mixins all call the same helper, which runs the full `BONUSES` walk plus the `MOD_MANAGED` diff on a single player — covering both APPLY and STRIP paths in one pass:
  - `LivingEntityEquipMixin` (HEAD on `onEquipStack`) — every armor-slot mutation.
  - `ServerPlayNetworkHandlerMixin` (TAIL on `onUpdateSelectedSlot`) — hotbar scroll / number-key swap.
  - `ServerPlayNetworkHandlerClickSlotMixin` (TAIL on `onClickSlot` + `onCreativeInventoryAction`) — survival inventory drag / shift-click / armor-swap-key AND creative-tab drag.
  The enumerated hand-rolled strip lists in the equip and slot mixins have been collapsed. Plus the Necromancer summon-fire path in `NecromancerSummon#onAfterDamage` drops `ROTTEN_MUSCLE` directly when the 4/4 summon triggers. The 80-tick `ArmorEffects` tick loop remains the safety-net for slower predicates (time-of-day flip, biome change, cooldown expiry, and any edge-case route not funneling through a tracked mixin hook). **Result**: six previously-effect-gated gameplay paths (`EMPERORS_DIVIDE`, `MESSMERS_FLAME_AEGIS`, `SUNS_PROTECTION`, `MESSMERS_VENOM`, `ASGARDIANS_FLIGHT`, `SHURIMAN_ENDURANCE`) activate within 1 tick of swap-in instead of up to 80. Among all 27 effects, only **7 carry real gameplay hooks** — Messmer's Flame Aegis, Undead Resistance, Sun's Protection, Radiant Might, Emperor's Divide, Shuriman Endurance, Asgardian's Flight — every other custom effect is cosmetic with the actual mechanic gated by a component on the item stack, not by the effect's presence.
- **NIGHT_STRENGTH / RADIANT_MIGHT / SUNS_PROTECTION natural-decay rationale.** These three were previously in `MOD_MANAGED` and diff-stripped within a 4 s cycle after their trigger dropped. They have been moved out — on unequip / set break / biome leave, the bonus trigger simply stops re-upping and the effect decays over its remaining ~20 s duration. Users see a short lingering tail instead of a hard snap. The `MOD_MANAGED` header javadoc in `ArmorEffects.java` notes these three as intentional exceptions.
- **Thor is the first 5/5 set.** The right-click riptide launch on Mjolnir (handled in `MjolnirMaceItem#onStoppedUsing`) is gated on the **Asgardian's Flight** status effect, which itself is applied only while all four `thor` armor pieces are worn AND `THOR_MACE=true` is in the main hand — no water, no rain, no Riptide enchant **required** (but Mjolnir ships with Riptide V and the launch magnitude scales with the stack's Riptide level via `EnchantmentHelper.getTridentSpinAttackStrength`). Asgardian's Flight is the gameplay predicate: `MjolnirMaceItem` checks `player.hasStatusEffect(ASGARDIANS_FLIGHT)` rather than re-deriving the 5/5 gate, so stripping the effect (via any of the three refresh-call mixin paths) short-circuits the launch instantly. The other two Thor-driven cosmetic effects (God of Thunder for Mjolnir mainhand, Storm's Awakening for Thunderhelm-worn-and-off-cooldown) are orthogonal single-piece gates and do not require the 5/5 condition. Storm's Awakening duration is **4800 t (~4 min)**; per-player cooldown is **24000 t (1 MC day)**.
