# API verification status

**Read this before your first build.** It says exactly how much of the Minecraft-facing code has been checked, and against what.

## The short version

- The **AI engine** (`ai/`, `game/`, `config/`, `util/`) is plain Java. It compiles with the standard JDK and is covered by 37 unit tests and headless simulations.
- The **Minecraft bridge** (`bridge/`) and **Fabric glue** (`client/`) were written against Mojang-mapped 1.21.11 names that were checked in published mapping pages and Fabric documentation. They have been **compiled only against hand-written API stubs** (to prove the code is internally consistent) and exercised by 94 logic checks against those stubs.
- **They have not been compiled against the real 1.21.11 jars, and have never run inside a real client.** The development environment had no route to Mojang's or Fabric's servers, so `./gradlew build` and `./gradlew runClient` could not be run there.

So: the first `./gradlew build` on your machine is the real compile test. If a signature differs, the error will point into `bridge/` or `client/`; the tables below say which calls were confirmed and which were not, so you know where to look first. Nothing in `ai/` should need to change.

## Checked against 1.21.11 mapping pages and Fabric documentation

Names, types and signatures checked against the Mojang-name mapping pages and Fabric API documentation for 1.21.11 (documentation, not the jar itself):

| Area | What the code uses |
|---|---|
| Identifiers | `net.minecraft.resources.Identifier`, `Identifier.fromNamespaceAndPath`, `level.dimension().identifier().getPath()` |
| Key bindings | `KeyMapping.Category.register(Identifier)`, `new KeyMapping(String, InputConstants.Type, int, KeyMapping.Category)`, `KeyBindingHelper.registerKeyBinding` |
| HUD | `HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id, element)` |
| Drawing | `GuiGraphics.fill`, `drawString(Font, String, x, y, color)`, `guiWidth()`, `guiHeight()`, `pose()` returning `Matrix3x2fStack` with `pushMatrix`/`popMatrix` |
| Events | `ClientTickEvents.END_CLIENT_TICK`, `ClientPlayConnectionEvents.JOIN` / `DISCONNECT`, client commands v2 (`ClientCommandManager`, `sendFeedback`, `sendError`) |
| Player | `LocalPlayer.respawn()`, `closeContainer()`, `swing(InteractionHand)`, `displayClientMessage`, `Entity.onGround()` |
| Game mode | `MultiPlayerGameMode.handleInventoryMouseClick(containerId, slot, button, ClickType, player)`, `useItem`, `useItemOn`, `continueDestroyBlock`, `stopDestroyBlock` |
| Menus | `Slot.container`, `Slot.getContainerSlot()`, `Slot.getItem()`, slot layout of `InventoryMenu` (armor 5–8, offhand 45) |
| Items | `DataComponents.FOOD`, `DataComponents.ENCHANTMENTS`, `ItemEnchantments.keySet()/getLevel`, `Holder.getRegisteredName()` |
| Interaction | `InteractionResult.Success.swingSource()`, `SwingSource.CLIENT` |
| Vectors | `net.minecraft.world.phys.Vec3` public `x`, `y`, `z` |

## Confirmed on 1.21.5 – 1.21.9 pages (1.21.11 page not available to check)

These exist with these exact signatures in the closest published versions. They are very unlikely to have changed, but they were not seen on a 1.21.11 page:

| Call | Where | Confirmed signature |
|---|---|---|
| `Creeper.getSwellDir()` | `MinecraftGame.mobInfo` | `public int getSwellDir()` |
| `FoodData.getSaturationLevel()` | `MinecraftGame.player` | `public float getSaturationLevel()` (also `getFoodLevel()`) |
| `IntegratedServer.isPublished()` | `EnvPort.gamePaused` | `public boolean isPublished()` |
| `Screen.isPauseScreen()` | `EnvPort.gamePaused` | `public boolean isPauseScreen()` |
| `Minecraft.hitResult` | `ControlsPort.mine` | `public HitResult hitResult` (also `screen`, `options`, `level`, `player`, `gameMode` as public fields) |
| `LevelHeightAccessor.isOutsideBuildHeight(int)` | `WorldPort`, `Scan` | `default boolean isOutsideBuildHeight(int)` (inherited by `Level`) |
| `LevelReader.hasChunkAt(BlockPos)` | `chunkLoaded` | `default boolean hasChunkAt(BlockPos)` (inherited by `Level`) |
| `EntityGetter.getEntitiesOfClass(Class<T>, AABB, Predicate<? super T>)` | `WorldPort.mobs`, `drops` | `default <T extends Entity> List<T> ...` (inherited by `Level`) |
| `ClientLevel.getEntity(int)` | `WorldPort.mob`, `ControlsPort.attack` | `public Entity getEntity(int)` |
| `Player` | – | extends `Avatar` since 1.21.9; `getInventory()` confirmed |

## Not confirmed against any source

These are assumed from long-standing Mojang names. Each is a single, local call; a compile error here is a one-line fix.

| Call | Where | Fallback if it does not compile |
|---|---|---|
| `Player.getAttackStrengthScale(float)` | `MinecraftGame.player` | Use `1.0f` (attack-cooldown pacing is a minor optimization) |
| `LivingEntity.getHealth()` / `getMaxHealth()` | `MinecraftGame.player`, `mobInfo` | Use `getAttributeValue(Attributes.MAX_HEALTH)` |
| `LivingEntity.getItemBySlot(EquipmentSlot)` | `InvPort.worn` | Read `player.getInventory()` armor/offhand accessors |
| `Mob.isAggressive()` | `MinecraftGame.mobInfo` | Treat hostile-category mobs as aggressive |
| `Options.hideGui` and the key fields (`keyUp`, `keyDown`, `keyLeft`, `keyRight`, `keyJump`, `keySprint`, `keyAttack`, `keyUse`) | `HudRenderer`, `ControlsPort` | Check `Options` for renamed fields |
| `HitResult.Type.BLOCK` / `getType()` | `ControlsPort.mine` | `hit instanceof BlockHitResult` already covers it; drop the type check |
| `Matrix3x2fStack.scale(float)` | `HudRenderer` | Use `scale(float, float)` |

## Cross-version trap: obfuscation

1.21.11 is still obfuscated at runtime (intermediary names under Fabric). The mod never uses reflection by class or method name, so Loom's remapping handles everything; keep it that way when extending the bridge.

## How the stub compile helps (and doesn't)

The stub compile proves that every call in `bridge/` and `client/` uses types and signatures consistent with *the stubs*, which were written from the mapping pages above. It catches typos, wrong argument orders and missing imports. It cannot catch a stub that is wrong about the real API, which is what the real compile is for.

## First-run checklist

1. `./gradlew build` — fix any signature errors using the tables above (all in `bridge/` / `client/`).
2. `./gradlew runClient` and create a **Survival single-player world**.
3. Press **Down Arrow**: the HUD should show ACTIVE and the character should head for trees. If nothing moves, run `/afk debug on` and look at `latest.log`.
4. Things worth watching on the first run, because the simulation cannot judge them: mining speed (crosshair on target), crafting-table opening, furnace loading, eating, and shield use.
5. Report or fix timing constants in `ai/task/impl/*` if a step is abandoned early; each task has a `maxTicks()`.
