# Limitations

This mod is a client-side bot built on public client APIs. It is honest about what that can and cannot do.

## What a client-side mod can't do

- **No server knowledge.** It only sees what the client has loaded: nearby chunks, entities in range, the player's own inventory. It cannot know about unloaded terrain, so exploration is local and it never "knows where the diamonds are". There is no x-ray: with `exploration.visibleOresOnly` (default on) it only targets ore exposed to air.
- **No full recipe list.** Since 1.21.2 the client no longer receives every recipe. Crafting uses a built-in catalog of the vanilla recipes the progression needs, applied by clicking items into the grid. Modded and data-pack recipes are not understood, and a server that changes vanilla recipes will make crafts fail (the task then reports "recipe not accepted" and backs off).
- **No server-side power.** It cannot teleport, give items, or set blocks. Everything goes through the same inputs a player has.

## Behavior limits (by design for this version)

| Area | Limitation |
|---|---|
| Dimensions | **Overworld only.** Entering the Nether or End pauses the mode. The config and the architecture have dimension hooks; Nether/End behavior and netherite are roadmap items. |
| Goals | Wood → Stone → Iron → Diamond tools, furnace, food, iron armor + shield, diamond armor. Enchanting and Nether access are disabled placeholders. No farming, villager trading, brewing, bed use or base building. |
| Pathfinding | A custom bounded A\*, not a perfect planner. It only plans through loaded chunks, treats slabs, stairs and other partial blocks conservatively, and can take odd routes or give up on a goal and retry. Doors, trapdoors and fence gates are treated as obstacles (never opened or broken), and boats are not used. |
| Mining | Needs the crosshair on the target block, so it must face and reach it like a player. If the pickaxe tier is too weak for a resource (e.g. iron ore with a wooden pickaxe) the task fails and the goal planner crafts the better tool first. |
| Water and lava | Avoids lava, never digs next to lava, and can swim short stretches, but it is not built for oceans: it does not plan long swims, boats or underwater exploration. |
| Combat | A competent melee fighter with a shield against ranged mobs. It flees from mobs it cannot beat (warden, ravager, evoker, …), does not use bows, potions, or tricks, and can still die. Deaths are handled (respawn, announce, optional return), not prevented. |
| Storage | Drops configured junk, deposits bulk blocks into a chest if one is nearby, else tosses surplus building blocks. It never builds chests, so it will not create a managed "base". |
| Food | Hunts and cooks passive animals; no farming. In food-poor terrain it can run low and will fall back to exploring. |
| Multiplayer | **Blocked by default** (`general.allowOnMultiplayer = false`). Only enable it on servers you own or have explicit permission to automate on. |
| Chunks and performance | Block scans are resumable and budgeted per tick; pathfinding is node-bounded; threat checks are throttled. Very large explorations can still cost frames on weak machines — lower `exploration.scanRadius` if so. |
| "Human-like" behavior | Reaction delay, smoothed look, idle pauses and inventory tidying are cosmetic polish for video. They are not an evasion layer and do not defeat any detection. |

## What it will never do

Anti-cheat bypass, packet manipulation, bot-detection evasion, server-exploit abuse, CAPTCHA solving, account farming, authentication bypass and hidden background operation are **not implemented and are out of scope**. See the README for how the specification's garbled section 30 was interpreted.

## Known rough edges

- Crafting and smelting rely on slot-number conventions of vanilla menus. A mod that adds slots or reorders menus can confuse them (they fail safe and back off).
- If another mod changes the attack/use key bindings or intercepts the game-mode calls used for clicking, mining and eating may stall; the stuck-detection will abandon the task.
- Tasks give up after a time limit and retry later, so progress can look slow in difficult terrain even when nothing is "wrong".
- The simulation covers the AI logic, not Minecraft's real physics. Expect the first real-world run to need tuning of timings (see `API_VERIFICATION.md`).
