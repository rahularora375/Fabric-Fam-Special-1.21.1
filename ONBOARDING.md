# ONBOARDING.md

Welcome. This is the Fabric **Fam Special** mod for Minecraft 1.21.11 — a themed-gear mod that adds eleven themed sets (custom items, enchantments, passive effects, set bonuses) and injects a subset of them into vanilla structure loot.

This file is the entry point for a new contributor (or a fresh Claude Code session). Read it once, then use the routing files for everything afterward — almost no detail lives here on purpose.

## 1. The 30-second tour

- **Language / build**: Java 21, Fabric Loom 1.14, Gradle wrapper.
- **Mod id**: `famspecial`. Entrypoint: `github.rahularora375.famspecial.FamSpecial#onInitialize`.
- **Surface area**: two creative tabs (**Gear**, **Armor**), eleven theme groups (`OldFam`, `Mistborn`, `Pacifist`, `Poseidon`, `FireSerpent`, `Necromancer`, `KnightRadiant`, `EshEndraNavesh`, `Shurima`, `Thor`, `RaidersLegacy`), three programmatic loot pools (`LegendaryPool` / `MapsPool` / `ThemedSetsPool`), 16 SpongePowered mixins.
- **No test suite.** Verification happens by running the dev client (`./gradlew runClient`).

## 2. First-time setup

1. Install JDK 21 and make sure `java -version` reports 21.
2. From the repo root: `./gradlew genSources` — generates the Minecraft source jar so the IDE can navigate into vanilla code.
3. Open the project in your IDE of choice (IntelliJ recommended; the Fabric Loom plugin auto-detects).
4. Sanity-check: `./gradlew build` produces a remapped jar in `build/libs/`.

## 3. The four commands you actually use

| Command | What it does |
|---|---|
| `./gradlew build` | Compile + remap → `build/libs/famspecial-<version>.jar`. |
| `./gradlew runClient` | Launch a dev Minecraft client with the mod loaded (world data in `run/`). |
| `./gradlew runServer` | Launch a dev dedicated server. |
| `./gradlew clean` | Wipe `build/`. |

On Windows PowerShell, use `.\gradlew.bat`; on Git Bash / WSL, `./gradlew`.

## 4. How the docs are organized (read this carefully)

`CLAUDE.md` stays small on purpose. Detail is split into topic files that you load **on demand**:

- **`ROUTER.md`** — task → topic-file rules. **Consult this first** for any non-trivial task.
- **`INDEX.md`** — every `.java` / resource file → its topic file. Use when `ROUTER.md` doesn't match cleanly.
- **`ITEMS.md`** — per-theme items, signature-weapon convention, "Adding new items" patterns.
- **`SYSTEMS.md`** — non-item architecture: components, status effects, sounds, `ArmorEffects`, `AttackHandlers`, `NecromancerSummon`, loot injection, client overlays, networking.
- **`MIXINS.md`** — all 16 mixin entries + `famspecial.mixins.json`.
- **`EFFECTS.md`** / **`ITEMS_REFERENCE.md`** — per-effect and per-item quick reference.

**Do not load all topic files speculatively.** Pick per `ROUTER.md`; add more only if the first pass turns up a cross-reference you actually need.

## 5. Working style (binding — see `CLAUDE.md` for the full version)

This repo's `CLAUDE.md` mandates a **subagent-first** workflow. Before you reach for `Read` or `Grep` inline, check the spawn-an-agent triggers:

- Reading 3+ files to answer one question → spawn an `Explore` agent.
- Grepping for an uncertain keyword across the repo → spawn an `Explore` agent.
- 2+ independent edits (e.g. "update all four theme files") → spawn one `general-purpose` agent per chunk in **a single message** so they run in parallel.
- Designing a change that touches 3+ files or crosses subsystems → spawn a `Plan` agent first.
- Anything whose result doesn't block the very next message → add `run_in_background: true`.

Inlining `Read`/`Grep` is the exception, not the rule. "It's probably quick" is not a reason — quick work compounds across a session.

## 6. Common tasks — go straight to these

| Task | Load |
|---|---|
| Add an item to an existing theme | `ITEMS.md` (and `SYSTEMS.md` if it touches `ArmorEffects.BONUSES` or `ModComponents`). |
| Add attribute modifiers (+health/speed/armor) | `ITEMS.md` only — renders in the vanilla attributes tooltip. |
| Attack-triggered behavior (heal-on-hit, debuff-on-hit) | `ITEMS.md` + `SYSTEMS.md` (`AttackHandlers`). |
| Edit a mixin / gate a vanilla screen | `MIXINS.md` (+ skim `SYSTEMS.md` `ModComponents` if reading a component flag). |
| Chestplate→elytra merge / Fortune-and-Glory anvil merge | `MIXINS.md` + `SYSTEMS.md` + `ITEMS.md`. |
| New status effect | `SYSTEMS.md` (`ModStatusEffects`). Icons are byte-for-byte vanilla copies — **do not author custom art**. |
| Loot drop rates / map destinations | `SYSTEMS.md` (Loot injection section). **Per user preference: do not add items to loot pools unless explicitly asked.** |
| Tooltip / HUD overlay / client-only render | `SYSTEMS.md` (`FamSpecialClient` + `HealthOverlay`). |
| Enchantment eligibility for a custom item | `SYSTEMS.md` (datapack tag overrides section). Mechanism is datapack tag overrides, **not mixins**. |
| New sound event | `SYSTEMS.md` (`ModSounds`). Register → drop OGG under `assets/famspecial/sounds/` → add `sounds.json` entry. |

If your task isn't here, check `ROUTER.md`. If it still isn't clear, fall back to `INDEX.md`.

## 7. Releasing

1. Bump `mod_version` in `gradle.properties` (it filters into `fabric.mod.json` via `processResources`).
2. `./gradlew build` and verify the remapped jar in `build/libs/`.
3. The client-server version handshake (`net/VersionHandshake.java`) is **strict equality** — players on a mismatched version are kicked at login. Plan version bumps accordingly.

## 8. Where work lives at runtime

- Dev-client world data, logs, configs → `run/` (gitignored).
- Build artifacts → `build/` (gitignored).
- Generated sources (`./gradlew genSources`) → IDE caches; not checked in.

## 9. Conventions worth knowing

- **Custom components** (`ModComponents`) are how runtime systems identify themed gear. New per-item flags get a `DataComponentType` here.
- **`ArmorEffects.BONUSES`** is the central dispatch table for set-bonus effects — `ArmorEffects.refreshBonusesFor(player)` is the one entry point all three equip-mixins call.
- **Tooltip lines must mirror the server-side gate exactly** — use `grantLine(...)` / `buildSetLine(...)` helpers in `FamSpecialClient`.
- **Enchantment eligibility for custom items is done via datapack tag overrides**, not mixins. Mjolnir and Fortune & Glory are the worked examples (see `ROUTER.md` for the full pattern).

That's the whole map. From here on out, `ROUTER.md` is your friend.
