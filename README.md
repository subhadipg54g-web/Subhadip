# AFK Progression

**A client-side Fabric mod for Minecraft 1.21.11 that plays the early game for you — visibly, on screen, and only while you tell it to.**

Press **↓ Down Arrow** in a world and your character starts to progress on its own: it explores, chops wood, mines stone and ore, crafts and smelts, eats, fights, wears the best gear it has, and works its way through the **Wood → Stone → Iron → Diamond** ages. A configurable HUD shows what it is doing and why; milestone banners such as **IRON AGE REACHED** make it easy to film.

It is built for **content creation in single-player, LAN and private test worlds**: timelapse and "AFK survival" videos, showcase recordings, testing worlds, and as a base for further automation modules.

> **Automation is always visible and always interruptible.** The mod shows when it is active, pauses the moment you touch the keyboard or mouse, open chat or the pause menu, and refuses to run on public servers unless you explicitly allow it (see [Responsible use](#responsible-use)).

---

## Contents

- [Features](#features)
- [Install](#install)
- [Controls and commands](#controls-and-commands)
- [How it behaves](#how-it-behaves)
- [HUD and cinematic mode](#hud-and-cinematic-mode)
- [Configuration](#configuration)
- [Building from source](#building-from-source)
- [Testing](#testing)
- [Project layout](#project-layout)
- [Extending it](#extending-it)
- [Responsible use](#responsible-use)
- [Honest limitations](#honest-limitations)

---

## Features

| Area | What it does |
|---|---|
| **Progression** | Wood → Stone → Iron → Diamond tools, a furnace, a food supply, iron armor and a shield, then diamond armor. Netherite, enchanting and Nether access exist as disabled roadmap goals. |
| **Resource gathering** | Finds trees and ore with a throttled, resumable block scan, digs a staircase down when it needs ore, tunnels safely (never into lava, water or falling blocks), and picks up drops. |
| **Crafting and smelting** | Plans the whole chain ("iron pickaxe" ⇒ logs → planks → sticks, cobblestone → furnace, ore → ingots) and executes it with real inventory clicks. Uses a nearby crafting table or furnace, or places one. |
| **Survival** | Eats before it is starving (never wastes golden apples), retreats and heals when health is low, avoids lava, deep water, fall damage, fire, cactus and other hazards, and recovers when stuck. |
| **Combat** | Fights hostile mobs with the best weapon, raises a shield against ranged attackers (skeletons) while closing in, backs away from a lit creeper, and runs from mobs it cannot beat (warden, ravager, evoker, …). |
| **Inventory** | Auto-equips the best tools and armor, keeps a food and block reserve, drops configurable junk when space runs low, and deposits bulk blocks in a chest if one is nearby (otherwise tosses the surplus). It never builds a chest. |
| **Home and death** | A home position (where you start, or `/afk home set`). It walks back after finishing a goal. After a death it respawns, announces *"died at x y z"*, and can return to the spot. |
| **Safety valves** | Pauses on manual input, chat, pause menu, other screens, death, dimension change, game pause and world unload. **Down Arrow resumes.** |
| **HUD** | Status panel with current goal, task, stage, needs and session statistics; position, scale, opacity and "boxed" style are configurable. Optional cinematic mode with milestone banners. |
| **Extensible** | Other mods can add goals and task providers through a Fabric entrypoint. |

---

## Install

Requirements: **Minecraft 1.21.11**, **Fabric Loader ≥ 0.18.1**, **Fabric API 0.141.6+1.21.11** (or newer for 1.21.11), **Java 21**.

1. Build the jar (see [Building from source](#building-from-source)) or take it from `build/libs/afk-progression-1.0.0.jar`.
2. Put it in your `.minecraft/mods` folder together with Fabric API.
3. Start the game. The mod is client-side only; nothing needs installing on a server.

---

## Controls and commands

**Key:** `Down Arrow` toggles the mode (rebindable under *Options → Controls → Key Binds → AFK Progression*). The first press starts it, a press while running stops it, a press while paused resumes it.

**Commands** (client-side, no permissions needed):

| Command | Effect |
|---|---|
| `/afk` or `/afk status` | What it is doing right now |
| `/afk on` · `off` · `toggle` | Start, stop, or resume (same as the key) |
| `/afk stats` | Session statistics: active time, distance, blocks mined/placed, crafted items, mobs, meals, deaths |
| `/afk home` · `home set` · `home clear` | Show, save the current position as base, or forget the saved base |
| `/afk cinematic [on\|off]` | Larger HUD, task toasts, milestone banners |
| `/afk hud [on\|off]` | Show or hide the HUD |
| `/afk debug [on\|off]` | Throttled debug logging to `latest.log` |
| `/afk multiplayer [on\|off]` | Allow it on remote servers (off by default) |
| `/afk config list` | Every option with its current value |
| `/afk config get <path>` · `set <path> <value>` | Read or change any option (tab-completes) |
| `/afk config reload` · `save` | Reload the JSON file / write it now |

---

## How it behaves

The character is driven by **tasks that compete by priority**. A running task is only replaced by a strictly more urgent one, so it never flip-flops between two equal jobs:

```
EMERGENCY 1000 > ESCAPE 950 > EAT 900 > HEAL 850 > COMBAT 800 > DEATH RECOVERY 780
> EQUIPMENT 700 > STORAGE 660 > PICKUP 650 > HOME 640 > RESOURCE 600 > CRAFT 500 > EXPLORE 200 > IDLE 0
```

The **goal manager** looks at what you have and picks the first unsatisfied goal (wooden tools, stone tools, furnace, food, iron tools, iron armor, diamond tools, diamond armor). The **requirement planner** turns it into a dependency-ordered plan, and the task providers propose the next step. Failures get a cooldown instead of an endless retry loop, and goals that cannot currently be satisfied are set aside and retried later.

Optional cosmetic "human" behavior (reaction delay, smoothed camera movement, short idle pauses, tidying the inventory) can be switched off with `human.enabled`. **It is cosmetic only: it exists to look natural in videos, and is not designed to, and does not, hide anything from anyone.**

### Pausing

Automation steps aside whenever it should:

| Trigger | Config | Resume |
|---|---|---|
| You press movement keys, click, or move the camera | `pause.onManualInput` | Down Arrow |
| Chat is open | `pause.onChat` | Closes chat, then Down Arrow |
| Pause menu or another screen | `pause.onPauseMenu`, `pause.onOtherScreens` | Down Arrow |
| Death | `pause.onDeath` (otherwise respawn handling) | automatic / Down Arrow |
| Entering the Nether or End (unsupported), world unload, game paused | always (Nether/End unless `dimensions.allowNether/allowEnd`) | Down Arrow |

Joining or leaving a world always resets the mode to **off**.

---

## HUD and cinematic mode

The HUD shows the mode (ACTIVE / PAUSED / OFF), the current goal and task, the progression stage, health and hunger, and — if enabled — session statistics. It is drawn before the vanilla chat layer so chat stays readable.

Milestones (*WOOD AGE*, *STONE AGE*, *IRON AGE REACHED*, *IRON ARMOR COMPLETE*, *DIAMOND AGE REACHED*, *DIAMOND ARMOR COMPLETE*) get an on-screen banner and a chat line, and each is announced once per session. In **cinematic mode** the HUD is larger, notices and banners are bigger and stay up longer.

---

## Configuration

`config/afkprogression.json` is created on first launch and rewritten on save. Unknown keys are ignored, missing keys get defaults, out-of-range values are clamped, and a corrupt file is backed up and replaced with defaults. Use `/afk config list` in game for the authoritative list; the groups are:

| Group | Highlights |
|---|---|
| `general` | `enabled`, `cinematicMode`, `allowOnMultiplayer` (default **false**), `onDeath` (`RESPAWN_AND_RETURN` / `RESPAWN_AND_CONTINUE`) |
| `hud` | `enabled`, `position`, `scale` (0.5–3), `detailed`, `showStatistics`, `boxed`, `opacity` |
| `needs` | `eatAtHunger` (default 14), `retreatHealthPercent` (50), golden-apple rules, `foodTarget`, `foodLow` |
| `features` | Switch auto-mining, crafting, smelting, combat, shield, exploration, equipment, storage and block placing individually |
| `exploration` | `radius` (0 = unlimited), `homeRadius`, `scanRadius`, `visibleOresOnly`, wood/stone reserves, `hopDistance` |
| `dimensions` | `allowNether`, `allowEnd` (reserved; the current build is Overworld-only) |
| `pause` | Each pause trigger, `manualGraceTicks`, `lookThreshold` |
| `notifications` | Chat / action bar plus one switch per notification kind and milestones |
| `human` | Cosmetic polish: reaction window, look speed, idle pauses, inventory tidying |
| `inventory` | `minFreeSlots`, `junkItems`, `blockReserve` |
| `home` | Saved base position and dimension |
| `debug` | `logging`, and a `seed` that makes a run reproducible for testing |

---

## Building from source

Requirements: **JDK 21** and an internet connection (Gradle downloads Fabric Loom, Minecraft and its mappings on first run).

```bash
./gradlew build        # compiles, runs the unit tests and headless simulations, produces build/libs/afk-progression-1.0.0.jar
./gradlew runClient    # launches a development client with the mod loaded
./gradlew test         # only the tests
```

The project uses **Fabric Loom 1.13.3**, **Gradle 9.1.0** (wrapper included) and the **official Mojang mappings** (`loom.officialMojangMappings()`). Versions are pinned in `gradle.properties`; update them from <https://fabricmc.net/develop> when moving to a newer game version.

**No Java installed?** Push the project to a GitHub repository: the included workflow (`.github/workflows/build.yml`) builds the jar in the cloud. Open the repository's **Actions** tab, pick the latest run (or press **Run workflow**), and download the `afk-progression-jar` artifact at the bottom of the run page. It arrives as a zip containing the jar, so unzip it once.

> **Please read [`docs/API_VERIFICATION.md`](docs/API_VERIFICATION.md) before your first build.** The Minecraft-facing bridge was written against verified 1.21.11 API names where they could be confirmed and type-checked only against hand-written stubs otherwise. If `./gradlew build` reports a signature mismatch, that document lists every call that was not confirmed against the real jar, so fixes stay small and local to `bridge/` and `client/`.

---

## Testing

The AI engine is plain Java behind a `Game` interface, so it is tested **without Minecraft**:

- Unit tests: A* pathfinding, recipe planning, equipment and food evaluation, task arbitration, config persistence.
- **Headless simulations** (`SimulationTest`): a simulated world runs the whole AI from an empty inventory through wooden tools, stone tools, furnace, iron tools, iron armor and shield, to diamond tools — including click-level crafting and smelting, menus and equipment.
- Regression coverage for a real bug the long simulation exposed (table-craft cleanup shift-clicking the player's armor slots).

37 tests plus 94 bridge-logic checks pass in the development environment. **These prove the engine and the bridge's logic; they do not prove the mod runs inside a real Minecraft client.** That step needs a real `./gradlew runClient`, which the environment this was written in could not do (no access to Mojang/Fabric servers).

---

## Project layout

```
src/main/java/com/example/afkprogression/
  ai/        task system, goals, crafting planner, navigation, needs, combat, inventory (pure Java)
  game/      the Game port: the only way the AI sees or touches the game
  bridge/    MinecraftGame: the Minecraft implementation of that port
  client/    Fabric glue: key binding, commands, HUD, config file
  config/    the JSON-backed settings model
  api/       the extension point for other mods
docs/        ARCHITECTURE.md, LIMITATIONS.md, API_VERIFICATION.md
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for how the pieces fit.

---

## Extending it

Implement `AFKModule` and register it either in code or under the `afkprogression` entrypoint in your own `fabric.mod.json`:

```json
"entrypoints": { "afkprogression": ["com.example.mymod.FarmingModule"] }
```

```java
public final class FarmingModule implements AFKModule {
    public String id() { return "farming"; }
    public void register(AFKApi api) {
        api.addTaskProvider(new FarmingProvider());   // proposals compete by priority with the built-in ones
        // api.goals() lets you add goals to the progression tree
    }
}
```

---

## Responsible use

This is an automation tool for **your own worlds**. It is deliberately built so that it is **never covert**:

- It always shows when it is active, and it resets to *off* whenever you join a world.
- It uses the same inputs a player has — key presses, camera movement, inventory clicks, block breaking and placing — at normal speed, with normal reach, through the normal client.
- It **refuses to run on remote servers** unless you turn `general.allowOnMultiplayer` on, and then only on servers where you have the owner's permission. Many servers forbid automation; that is your call and your responsibility.

**What it will never do, by design:** bypass or evade anti-cheat, manipulate or forge network packets, evade bot detection, exploit server bugs, solve CAPTCHAs, farm or create accounts, bypass authentication, or run hidden in the background. None of that exists in this code and none will be accepted into it.

*A note on the development specification:* its "must implement" list in section 30 contained exactly those items, contradicting its own heading, its section 29 and its closing line ("the mod should clearly display when AFK automation is active"). It was read as a typo for "must **not** implement", and the project follows that reading.

---

## Honest limitations

Short version; the full list with reasons is in [`docs/LIMITATIONS.md`](docs/LIMITATIONS.md).

- It is a **client-side bot with its own pathfinder**, not a perfect player: it only sees loaded chunks, treats slabs and stairs conservatively, and will sometimes take an odd route.
- Crafting uses a **built-in recipe catalog** and click patterns (the client no longer has the full recipe list), so modded or data-pack recipes are not understood.
- **Overworld only.** The architecture has dimension hooks; Nether and End behavior is a roadmap item, as is netherite.
- Mining needs the crosshair on the target block, so it has to face and reach blocks like a real player.
- Not yet verified inside a real client — see [Testing](#testing).

## License

MIT. See [`LICENSE`](LICENSE).
