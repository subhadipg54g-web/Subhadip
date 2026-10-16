# Architecture

AFK Progression is split along one deliberate seam: **the AI never talks to Minecraft directly.** It talks to a small interface (`Game`), and one class (`MinecraftGame`) implements that interface on top of the real client. Everything else — about 90 % of the code — is plain Java that can be unit-tested and simulated without launching the game.

```
                   ┌──────────────────────────── Fabric client (client/) ───────────────────────────┐
  Down Arrow ─────▶│ KeybindManager   AFKCommands   HudRenderer   ConfigStore   AFKProgressionClient │
                   └───────────────┬───────────────────────────────────────────▲────────────────────┘
                                   │ toggle / commands                          │ HudModel (read-only)
                                   ▼                                            │
                   ┌──────────────────────────────  AI engine (ai/)  ───────────┴───────────────────┐
                   │  AFKController ── AFKStateManager (OFF/ACTIVE/PAUSED, pause reasons)           │
                   │       │                                                                         │
                   │       ├─ GoalManager / ProgressionManager ── DefaultGoals, Stage, ProgressView  │
                   │       ├─ RequirementPlanner ── RecipeCatalog, Ledger, Plan                      │
                   │       ├─ TaskManager ◀── TaskProviders (CoreProviders + modules)                │
                   │       │      └─ tasks: Wood Mine Craft Smelt Eat Heal Combat Equip Storage …    │
                   │       ├─ NavigationManager ── PathFinder (A*), PathExecutor, LookController     │
                   │       ├─ PlayerNeedsManager ── Hunger, Health, Safety                           │
                   │       ├─ CombatManager / MeleeController / MobCatalog                           │
                   │       ├─ InventoryManager ── EquipmentEvaluator, FoodEvaluator, ContainerOps    │
                   │       └─ ResourceScanner, StationManager, TunnelDigger, ExplorationPlanner      │
                   └───────────────┬───────────────────────────────────────────────────────────────┘
                                   │ Game port (game/Game.java): World · Inv · Controls · Env
                      ┌────────────┴────────────┐
                      ▼                         ▼
             bridge/MinecraftGame          test/SimGame
             (real client, Mojang names)   (headless simulated world)
```

## Layers

| Package | Responsibility | Depends on |
|---|---|---|
| `game` | The port: value types (`Pos`, `Cell`, `ItemInfo`, `MobInfo`, `PlayerState`, `InputState`), static block knowledge (`BlockRules`) and the `Game` interface with its four sub-ports. | nothing |
| `ai` | All behavior. No `net.minecraft` import anywhere. | `game`, `config`, `util` |
| `bridge` | `MinecraftGame` implements the port; `BlockClassifier` and `ItemMapper` translate blocks and stacks into `Cell` flags and `ItemInfo`. | Minecraft, `game` |
| `client` | Key binding, commands, HUD drawing, config file, Fabric lifecycle wiring, fault containment. | Fabric, `ai`, `bridge` |
| `api` | `AFKModule` / `AFKApi`, the supported extension surface. | `ai` |

Because `ai` has no Minecraft dependency, a Minecraft update only touches `bridge/` and `client/`.

## The Game port

| Port | Purpose | Notable detail |
|---|---|---|
| `World` | Cell flags (solid, passable, hazard, climbable, falling, protected…), block ids, hardness, mobs, item drops, resumable block scans. | Reads are cached per tick; unloaded chunks read as "unknown", never as air. |
| `Inv` | Inventory slots, worn armor, hotbar selection, open screen kind, cursor stack, menu slots, and `click(slot, button, kind)`. | `screen()` is derived from the player's container menu, so it is accurate even with no GUI open. |
| `Controls` | Movement keys, look, attack, mine, use, hold-use, respawn. `readInput()` vs `applied()` exposes manual input. | Mining presses the real attack key while the crosshair is on the target; see "Input honesty". |
| `Env` | Remote-server flag, game-paused flag, foreign screens (chat, pause, death, other), chat and action-bar output. | |

## Task arbitration

Each tick, `TaskManager` (throttled to every few ticks, faster when combat is alert) asks every `TaskProvider` for a `TaskProposal` and picks the highest priority. A running task is replaced **only by a strictly higher priority** (with a hysteresis margin below 700), or when it completes, fails or asks to yield. Equal-priority work therefore never thrashes.

Priorities are constants in `TaskPriority`: EMERGENCY 1000 › ESCAPE 950 › EAT 900 › HEAL 850 › COMBAT 800 › DEATH_RECOVERY 780 › EQUIPMENT 700 › STORAGE 660 › PICKUP 650 › HOME 640 › RESOURCE 600 › CRAFT 500 › EXPLORE 200 › IDLE 0.

Interrupted tasks are stopped cleanly (`onStop`) and re-proposed later. Tasks that can be starved by interruptions (travel, food gathering) opt into **cumulative timeouts**: time already spent carries over, so a task that keeps being interrupted still gives up eventually. Failures are recorded on the blackboard with a cooldown key, so a failing job is not retried every tick.

## Goals, planning and crafting

1. `ProgressionManager` evaluates `DefaultGoals` in order and activates the first unsatisfied goal. Goals that cannot currently be met are blocked for a while and retried. Completing a goal sets a "return home" flag and, the first time, fires its milestone.
2. `RequirementPlanner` expands the goal's `Request`s into a `Plan` of `PlanStep`s (gather logs → craft planks → craft sticks → … → smelt iron), using a `Ledger` to account for what the inventory, stations and earlier steps already provide, and reporting unobtainable items instead of looping.
3. `CoreProviders` turns the next plan step into a `WoodTask`, `MineTask`, `CraftTask`, `SmeltTask`, and so on.
4. **Crafting is click-based.** Since 1.21.2 the client no longer has the full recipe list, so `RecipeCatalog` carries the vanilla recipes the progression needs as grids, and `CraftTask` fills the crafting grid with inventory clicks, takes the result, and verifies the inventory. A `CraftTask` first checks which menu is actually open, because slot numbers mean different things per menu (in the player's own menu, slots 5–8 are armor).

## Navigation

`PathFinder` is a bounded A\* over `Cell` flags with moves for walking, jumping up one block, dropping up to three, climbing, and optionally **digging** (never next to lava, never into falling blocks, never straight down). `PathExecutor` follows the path with real key input and detects being stuck; `NavigationManager` re-plans, digs out of boxed-in positions and reports `ARRIVED` / `FAILED`. `LookController` smooths camera movement. Mobs move too fast for A\*, so `MeleeController` steers directly.

## Needs, combat and safety

`PlayerNeedsManager` aggregates `HungerManager` (when and what to eat, `FoodEvaluator`), `HealthManager` (retreat/heal thresholds, golden apple as last resort) and `SafetyManager` (hazards, fall damage, drowning, being stuck). `CombatManager` does throttled threat assessment with `MobCatalog` roles (HOSTILE, NEUTRAL, FOOD, AVOID, IGNORE) and a fight-or-flee decision.

## State, pausing and failure containment

`AFKStateManager` is a small state machine (OFF / ACTIVE / PAUSED) with named pause reasons (manual input, chat, pause menu, other screen, death, unsupported dimension, remote server, game paused…). **Manual input** is detected by comparing the real key state with what the AI last applied, and the camera and hotbar against what it set.

`AFKProgressionClient` wraps every tick in a fault handler: an exception is logged and the tick skipped; five within ten seconds switch the mode off and tell the player. A bug in the AI must never crash the game.

## Input honesty

The bridge only does what a player can do at the keyboard: movement keys, camera, attack/use keys, hotbar selection and inventory clicks via the normal `MultiPlayerGameMode` path. Mining presses the real attack key while the crosshair rests on the target block — vanilla stops a break every tick the key is up, so poking the game mode directly would never finish a block. There is no packet-level code, no timing manipulation and no attempt to look like a different client.

## Extension points

- `AFKModule` (Fabric entrypoint `afkprogression`) can register `TaskProvider`s and add `Goal`s.
- `Goal`, `Stage`, `TaskProvider` and `Notifier` are small interfaces; `DefaultGoals` shows how a goal declares its satisfaction test and requests.
- The `Planned` goals (enchanting, Nether access) are placeholders that show where roadmap work plugs in.

## Testing seams

`SimGame` (in `src/test`) implements `Game` with a block world, trees, ore, mobs, crafting and furnace menus, armor slots and a click model that mirrors vanilla slot numbering. `SimulationTest` drives the full `AFKController` against it. `ConfigStoreTest` covers persistence. A separate check suite for `bridge/` exercises `MinecraftGame` against Minecraft API stubs (see `API_VERIFICATION.md` for what that does and does not prove).
