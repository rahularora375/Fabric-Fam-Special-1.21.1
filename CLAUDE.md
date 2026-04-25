# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Fabric mod for Minecraft **1.21.11** (Java 21, Fabric Loom 1.14). Entrypoint: `github.rahularora375.famspecial.FamSpecial#onInitialize`. Mod id: `famspecial`.

Purpose: adds themed custom gear items (custom names, lore, enchantments, passive effects) into two creative tabs (**Gear** for weapons + tools, **Armor** for sets) and injects a subset of them into vanilla structure chests via three programmatic loot pools (`LegendaryPool` / `MapsPool` / `ThemedSetsPool`). Eleven theme groups: **OldFam**, **Mistborn**, **Pacifist**, **Poseidon**, **FireSerpent**, **Necromancer**, **KnightRadiant**, **EshEndraNavesh**, **Shurima**, **Thor**, **RaidersLegacy**.

## Routing

Detail has been split out of this file to keep always-loaded context small. Load topic files on demand:

- **`ROUTER.md`** — task-to-file rules. Consult first for any non-trivial task.
- **`INDEX.md`** — source-file inventory (every `.java` / resource → topic file). Use when `ROUTER.md` doesn't match cleanly.
- **`ITEMS.md`** — per-theme items, signature-weapon convention, "Adding new items" patterns.
- **`SYSTEMS.md`** — non-item architecture: components, status effects, sounds, item-group helpers, `ArmorEffects`, `AttackHandlers`, `NecromancerSummon`, loot injection, client overlays, networking.
- **`MIXINS.md`** — all 16 SpongePowered mixin entries + `famspecial.mixins.json`.

Do not load all topic files speculatively. Respect `ROUTER.md`.

## Working style

**Default to subagents. Inlining is the exception, not the rule.** Before reading a file or running a grep inline, check the triggers below; if any match, stop and spawn an agent instead.

**Spawn-an-agent triggers** — if ANY match, do not inline:

- About to read 3+ files to answer one question → spawn `Explore` (thoroughness: `"quick"` for file-location lookups, `"medium"` for "how does X work", `"very thorough"` for architecture questions spanning multiple packages).
- About to grep the repo for a keyword where the match location is uncertain → spawn `Explore`.
- Task has 2+ independent chunks (e.g., "update all four theme files to add flag X") → spawn one `general-purpose` agent per chunk **in a single message** so they run in parallel.
- Designing a change that will touch 3+ files or cross subsystem boundaries (item entry + mixin + tooltip, etc.) → spawn `Plan` first; do not start editing until the plan returns.
- User asks a codebase question that would require reading any file I haven't already read this session → spawn `Explore`, not a chain of `Read` calls.

**Background-execution triggers** — set `run_in_background: true` whenever:

- The agent's result doesn't block the very next message I send the user (research that informs a later step, long builds, verification passes, etc.).
- I have other work I can do in the main thread while it runs.

Only keep an agent in the foreground when its output is a hard dependency of my next action (e.g., I need the file list before I can start editing).

**Anti-patterns — these are failures of this rule, not shortcuts:**

- Serial `Read` calls across 3+ files when one `Explore` call would answer the question.
- Inline `Grep` for an uncertain keyword match across the whole repo.
- Spawning an `Explore` agent in the foreground and then sitting idle waiting for it instead of running parallel work or using `run_in_background: true`.
- Justifying inlining with "it's probably quick" — quick work compounds across a session; the cost of a subagent call is one API round-trip, the cost of a bloated main context is session-long.

When in doubt: delegate. Err on the side of more agents, more parallelism, more background execution — never less.

## Commands

Use the Gradle wrapper (on Windows bash, invoke `./gradlew`):

- `./gradlew build` — compile and produce the remapped jar in `build/libs/`
- `./gradlew runClient` — launch a dev Minecraft client with the mod loaded (runtime in `run/`)
- `./gradlew runServer` — launch a dev dedicated server
- `./gradlew genSources` — generate Minecraft source jars for IDE navigation
- `./gradlew clean` — wipe `build/`

No test suite is configured.

Bump `mod_version` in `gradle.properties` for releases; it is filtered into `fabric.mod.json` via `processResources`.
