# Porting notes: tech/machine

Resolves issue #20 (porting notes: tech/machine).
Machine is the last hoisting unit of issue #4 (module porting order): 150 files, 18,618 lines (DEPS).
It holds the eight stone and brick tier combustion machines (kiln, oven, sawmill, crucible in two
tiers), the five cog machines (mechanical hopper, mechanical compacting bin, mechanical mulch
spreader, trip hammer, mechanical bellows), the hand bellows, the cog and sawmill blade items, nine
recipe types, a 1,302-line config, and 59 plugin files. This document lists each 1.12 construct and
its 1.21 replacement. Decisions that need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo, package
  `com.codetaylor.mc.pyrotech.modules.tech.machine` (91 non-plugin files), plus
  `src/api/java/com/codetaylor/mc/pyrotech/IAirflowConsumerCapability.java`,
  `library/spi/tile/TileCombustionWorkerBase.java`, `library/spi/tile/TileEntityDataWorkerBase.java`,
  `library/spi/tile/TileCapabilityDelegate.java`,
  `modules/tech/basic/tile/TileCompactingBin.java`, and the 31 crafting recipes under
  `assets/pyrotech/recipes/tech/machine/`. Only `plugin/jei/PluginJEI.java` was read from the
  plugin packages, for the category list.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Every 1.21 class and method named below
  was grepped out of that jar; the line numbers cited are entries in it.
- **TECH-BASIC**: `docs/research/porting-notes-tech-basic.md` on branch
  `research/porting-notes-tech-basic`.
- **BLOOMERY**: `docs/research/porting-notes-bloomery.md` on branch `research/porting-notes-bloomery`.
- **REFRACTORY**: `docs/research/porting-notes-refractory.md` on branch
  `research/porting-notes-refractory`.
- **STORAGE**: `docs/research/porting-notes-storage.md` on branch `research/porting-notes-storage`.
- **PROTOTYPE**: branch `prototype/8-campfire-interaction-sync`,
  `docs/prototypes/campfire-interaction-sync.md`.
- **DEPS**: `docs/research/module-dependencies.md` on branch `research/module-dependencies`.
- **SIGN-OFFS**: the resolution comments on issues #21 (recipe architecture), #22 (core),
  #29 (refractory), #34 (ignition), #35 (tech/basic), #36 (bloomery), and the "Decisions so far"
  section of issue #1.
- **ASSETS**: the converted assets on `main` under `src/main/resources/assets/pyrotech/`.

This document assumes every settled decision above. It applies PROTOTYPE pattern 1 (per-block
if-chain dispatch) and pattern 2 (blockstate properties, else a `SyncedBlockEntity` snapshot).

## Scope correction

Four claims in the ticket text and DEPS do not survive a read of the code.

- **The mulch spreader does not use storage's stash tile.** Its one storage reference is a generic
  type parameter: `InteractionMulch extends InteractionItemStack<TileStash>`
  (`tile/TileMechanicalMulchSpreader.java`, lines 16 and 256 to 257). The interaction never touches
  a `TileStash`; the spreader keeps its own `MulchStackHandler` (lines 313 to 344). This is the same
  copy-paste as the compacting bin worker's `TileSoakingPot` parameter. With the interaction
  framework gone, both parameters vanish and machine has no storage edge at all.
- **DEPS counts one cross-module edge for machine, "basic (22)".** The real edge set, from the
  imports of the 91 non-plugin files, is: tech/basic in 9 files, ignition in 2, refractory in 2,
  tool in 1, bucket in 1, bloomery in 1, storage in 1 (the dead one above). The other 13 tech/basic
  files DEPS counted are plugin code. All of ignition, refractory, tool, bucket, and bloomery are
  reached only from recipe adders, which become datagen.
- **`TileCombustionWorkerBase` is not a tech/basic class.** It lives in
  `library/spi/tile/TileCombustionWorkerBase.java`, over
  `library/spi/tile/TileEntityDataWorkerBase.java`. Its three users are tech/basic's campfire and
  drying rack base and machine's stone worker base. TECH-BASIC inlined it for the campfire, so
  machine is the last caller. Decision 1.
- **The refractory kiln is this unit's brick kiln, as the refractory scope correction said.** The
  lang key `block.pyrotech.brick_kiln` is "Refractory Kiln" (`main`, `en_us.json`, line 172), and
  `init/recipe/StoneKilnRecipesAdd.java` writes the refractory brick smelt at lines 54 to 65. All
  four brick machines are named "Refractory ..." in lang (lines 172 to 175). Confirmed.

## Summary

- The bootstrap collapses into `DeferredRegister` holders for 14 blocks, 14 block items, 15 items,
  12 block entity types, 9 recipe types with 5 serializers, 4 sound events, and three client
  registrations. Machine registers no packets and no capabilities.
- Eight of the 14 blocks are one class, `BlockCombustionWorkerStoneBase`, with four subclasses that
  differ only in particles, a collision box, and the crucible's fluid light. The eight `*Top` tiles
  are capability delegates that exist only so a bellows facing a top half reaches the machine below;
  with airflow a plain core interface they all drop and `newBlockEntity` returns null for `type=top`.
- The four-value `type` property survives, because the converted blockstates key on it. `bottom_lit`
  is written by the block entity as in 1.12; `bottom_dormant` was derived in `getActualState`, which
  1.21 has no equivalent for, so the ticker writes it when the burn time crosses zero.
- Airflow is core's plain `AirflowConsumer` interface (SIGN-OFFS, issue #22 item 7). The bellows
  looks up the block entity in front of it, and its lookup must resolve a bloomery, wither forge, or
  machine top half to the block entity below, because that is what the 1.12 delegate did.
- The nine 1.12 recipe registries become nine `RecipeType`s over five recipe classes: three reused
  from tech/basic (kiln, drying, compacting bin) and two new (sawmill, crucible). The two ovens ride
  the drying recipe shape, which is what makes the drying rack chain edge legal.
- The ovens derive their cook list from vanilla furnace recipes whose result is food, exactly the
  rule TECH-BASIC settled for the campfire. Their whitelist and blacklist are CraftTweaker surface
  and become one item tag.
- Datagen writes 60 own recipes, 206 inherited copies from the eight chain edges machine owns, and
  29 crafting recipes. The 17 `INHERIT_*` and `INHERITED_*` knobs go.
- The three cog data maps in config (hopper transfer, compactor progress, mulcher range and
  attempts) become one item data map that the cog tooltip also reads, so `CogTooltipEventHandler`
  keeps working for third-party cogs.
- The 1,302-line config shrinks to about seven `COMMON` toggles, about nineteen `SERVER` multipliers,
  and four client sound values, with everything else baked, tagged, or in the data map. Thirteen
  gamestages fields die.
- Nine JEI categories return in the shared plugin. Two of them, the ovens, need a synthesized
  furnace-food recipe list as the 1.12 plugin built.

## Module bootstrap

`ModuleTechMachine` follows the CORE replacements: one `register(IEventBus)` hook, `DeferredRegister`
fields as holders, no `ModuleBase`, no `Registry`, no `Injector`, no `@GameRegistry.ObjectHolder`.

| 1.12 construct | 1.21 replacement |
|---|---|
| `setRegistry`, `enableAutoRegistry`, `PACKET_SERVICE`, `TILE_DATA_SERVICE` (`ModuleTechMachine.java`, lines 51 to 55) | Dropped with athenaeum. Sync is the PROTOTYPE `SyncedBlockEntity`; machine sends no payloads (network section). |
| Fourteen CraftTweaker plugins, the JEI plugin, the TOP and Waila IMC messages (lines 59 to 87, 100 to 104, 115 to 119) | Dropped. JEI returns in the shared plugin (assets and JEI section). |
| `RegistryEvent.NewRegistry` creating nine Forge recipe registries and injecting them (`init/RegistryInitializer.java`, lines 13 to 33, 35 to 87, 89 to 130) | Nine `RecipeType` and five `RecipeSerializer` entries in the shared holders (NF-SRC `net/minecraft/core/registries/Registries.java`, lines 181 and 182). |
| `RegistryEvent.Register<SoundEvent>` registering four sounds (lines 122 to 130; `Sounds` holder, lines 310 to 331) | `DeferredRegister.create(Registries.SOUND_EVENT, ...)` (NF-SRC `Registries.java`, line 188) with `SoundEvent.createVariableRangeEvent(id)` (NF-SRC `net/minecraft/sounds/SoundEvent.java`, line 40). `sounds.json` on `main` already has the four entries. |
| `onRegisterRecipesEvent`: nine `apply` calls and eight `registerInherited*` calls (lines 133 to 159) | Datagen (recipes section). The eight inheritance calls become eight chain-table edges. |
| `CogTooltipEventHandler` registered in client pre-init (line 113) | An `@EventBusSubscriber(modid = ..., value = Dist.CLIENT)` static listener on `ItemTooltipEvent` (items section). |
| `BlockInitializer.onRegister`: eleven `registerBlockWithItem`, two `registerBlock` with a custom `ItemBlock`, one more `registerBlockWithItem`, 24 tile classes (`init/BlockInitializer.java`, lines 32 to 84) | `DeferredRegister.Blocks#registerBlock` (NF-SRC `net/neoforged/neoforge/registries/DeferredRegister.java`, line 431) for fourteen blocks and `Items#registerSimpleBlockItem` (line 530) for fourteen block items; the two custom `ItemBlock`s go (cog worker section). Twelve `BlockEntityType`s through `BlockEntityType.Builder.of(factory, blocks...).build(null)` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntityType.java`, lines 337 and 341): kiln over both kilns, oven over both ovens, sawmill over both sawmills, crucible over both crucibles, then hopper, compacting bin, compacting bin worker, mulch spreader, trip hammer, bellows, mechanical bellows, mechanical bellows top. The eight `*Top` delegates and the eight per-tier tile subclasses go; the per-tier numbers come from the block. |
| `ItemInitializer.onRegister`: seven blades and eight cogs, each taking a config durability, the wood cog taking a burn time (`init/ItemInitializer.java`, lines 17 to 32) | `DeferredRegister.Items#registerItem` with `new Item.Properties().durability(n).stacksTo(1)`. The wood cog's 1100-tick burn time becomes a `neoforge:furnace_fuels` entry (config section). |
| `onClientRegister`: block item models, two `_item` model overrides, four `setCustomStateMapper` calls behind `USE_IRON_SKIN`, sixteen TESR bindings (`BlockInitializer.java`, lines 90 to 158, 161 to 180) | Item models load by id. The state mappers have no 1.21 equivalent (decision 4). `EntityRenderersEvent.RegisterRenderers#registerBlockEntityRenderer` (NF-SRC `net/neoforged/neoforge/client/event/EntityRenderersEvent.java`, line 109) for the twelve types that draw anything. |
| `ModuleTechMachine.CREATIVE_TAB` | The fourteen block items and fifteen items join core's tab in `BuildCreativeModeTabContentsEvent` (NF-SRC `net/neoforged/neoforge/event/BuildCreativeModeTabContentsEvent.java`, line 90). |
| `Blocks`, `Items`, `Sounds`, `Registries` holder classes with injected nulls (lines 176 to 358) | `DeferredBlock`, `DeferredItem`, and `DeferredHolder` fields (CORE). |
| `ModuleTechMachine.LOGGER` (line 42), read once by the mulch spreader cog parser | `Pyrotech.LOGGER`. The parser itself goes with the config map. |

## The stone combustion worker

`BlockCombustionWorkerStoneBase` (573 lines) and `TileCombustionWorkerStoneBase` (551 lines) carry
all eight tier machines. Three tile subclasses add an input slot, an output handler, and an output
tank.

### The block

| 1.12 construct | 1.21 replacement |
|---|---|
| `PropertyEnum TYPE` with `top`, `bottom`, `bottom_lit`, `bottom_dormant` plus `Properties.FACING_HORIZONTAL`; metadata packs both (`block/spi/BlockCombustionWorkerStoneBase.java`, lines 47, 58 to 61, 465 to 504) | `EnumProperty.create("type", ...)` (NF-SRC `net/minecraft/world/level/block/state/properties/EnumProperty.java`, line 75) with the same four values, and `BlockStateProperties.HORIZONTAL_FACING` (NF-SRC `BlockStateProperties.java`, line 54). The converted blockstates on `main` key on exactly `facing` and `type`, so nothing regenerates. |
| `getActualState` deriving `bottom_dormant` when the tile still has burn time but is not active (lines 519 to 532) | No equivalent hook. The block entity writes the property: on any transition of `burnTimeRemaining > 0` while inactive, `level.setBlock(pos, state.setValue(TYPE, ...), 3)`. Guard on a change so the ticker does not write every tick (decision 2). |
| `Material.ROCK`, `SoundType.STONE`, hardness 2, resistance 15, pickaxe 0 (lines 53 to 57) | `Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(2.0f, 15.0f).requiresCorrectToolForDrops().noOcclusion()` (NF-SRC `BlockBehaviour.java`, lines 1149, 1185, 1195, 1298, 1165) plus `minecraft:mineable/pickaxe`. |
| Top box 1 to 15 px by 8 tall (4 tall for the sawmill); bottom full; `isSideSolid` false on `UP` and the front; never full, cube, opaque, or normal; `UNDEFINED` face shape (lines 49, 123 to 187; `BlockSawmillBase.java`, lines 29 to 41) | `getShape` (NF-SRC `BlockBehaviour.java`, line 725) returning `Block.box(1, 0, 1, 15, 8, 15)` for the top (NF-SRC `net/minecraft/world/level/block/Block.java`, line 151), 4 tall for the sawmill, and a full box for the bottom. `noOcclusion()` covers the rest. |
| `getLightValue` = `STONE_MACHINE_LIGHT_LEVEL` 9 when `bottom_lit` (lines 96 to 104) | `Properties.lightLevel(state -> state.getValue(TYPE) == BOTTOM_LIT ? 9 : 0)` (NF-SRC `BlockBehaviour.java`, line 1190). The crucible overrides it (crucible section). |
| `isFireSource` false, `getFlammability` 0 (lines 193 to 205) | Defaults; nothing to write. |
| `igniteWithAdjacentIgniterBlock`: any half, activate the tile at `pos` (lines 68 to 76) | Core's `IBlockIgnitableAdjacentIgniterBlock` (SIGN-OFFS, issue #34). Note the 1.12 method reads the tile at `pos`, so it only ever fires on the bottom half, where the tile is. |
| `igniteWithIgniterItem`: only when the state is not top and the clicked side equals the facing (lines 78 to 90) | Core's `IBlockIgnitableWithIgniterItem` with the same two tests. Follow-on (A) item 1 is confirmed: the half test is `!isTop(state)` and the side test is `state.getValue(FACING) == facing`. |
| `onBlockActivated`: on a non-top half, an `ItemIgniterBase` in the main hand returns false, else run `interact` (lines 266 to 287, the igniter test at line 281) | `useItemOn` (NF-SRC `BlockBehaviour.java`, line 226): first branch returns `SKIP_DEFAULT_BLOCK_INTERACTION` (NF-SRC `net/minecraft/world/ItemInteractionResult.java`, line 8) for `#pyrotech:igniters` on a non-top half, then the chain. Follow-on (A) item 1 is confirmed at line 281. |
| `collisionRayTrace` re-aiming the top half at `pos.down()`, and `interact` re-dispatching with `hitY + 1` (lines 246 to 261, 358 to 366) | The block resolves the block entity at `pos.below()` for a top half and runs the chain with the hit face and a local position shifted up by one, the same shape BLOOMERY chose. |
| `onBlockPlacedBy` placing the top half; `canPlaceBlockAt` requiring both positions; `getStateForPlacement` facing the placer's opposite (lines 325 to 344, 348 to 355, 497 to 513) | `getStateForPlacement(BlockPlaceContext)` (NF-SRC `Block.java`, line 398) returning null when the block above cannot be replaced, as `DoorBlock` does (NF-SRC `net/minecraft/world/level/block/DoorBlock.java`, line 144), and `setPlacedBy` (NF-SRC `Block.java`, line 414) writing the top, as `DoorBlock` does at lines 164 to 165. |
| `breakBlock`: the top drops the tile's contents plus one machine item at `pos.down()` and clears it; the bottom clears the top and drops contents (lines 288 to 322) | `playerWillDestroy` (NF-SRC `Block.java`, line 467) calling `DoublePlantBlock.preventDropFromBottomPart` (NF-SRC `net/minecraft/world/level/block/DoublePlantBlock.java`, line 120) as `DoorBlock` does at line 128, plus `updateShape` (NF-SRC `BlockBehaviour.java`, line 178) removing the surviving half. Contents drop from the bottom's `onRemove` (line 193) through `Containers.dropItemStack` (NF-SRC `net/minecraft/world/Containers.java`, line 31). |
| `quantityDropped` 0 for the top, `canSilkHarvest` false (lines 237 to 246, 346 to 351) | Loot table shaped like `createSinglePropConditionTable` (NF-SRC `net/minecraft/data/loot/BlockLootSubProvider.java`, line 235) but with two conditions, because the lit and dormant bottoms must also drop: `LootItemBlockStatePropertyCondition.hasBlockStateProperties` (NF-SRC `net/minecraft/world/level/storage/loot/predicates/LootItemBlockStatePropertyCondition.java`, line 53) for `type != top`. |
| `randomDisplayTick`, bottom half: crackle at 10 percent and one smoke plus one flame at the front face, offset 0.25 out (lines 384 to 441) | `Block#animateTick` (NF-SRC `Block.java`, line 277) with `SoundEvents.FURNACE_FIRE_CRACKLE` (NF-SRC `net/minecraft/sounds/SoundEvents.java`, line 581), `ParticleTypes.SMOKE` and `FLAME` (NF-SRC `net/minecraft/core/particles/ParticleTypes.java`, lines 84 and 55). |
| `randomDisplayTickActiveTop` and `randomDisplayTickWorkingTop` hooks on the top half, reading the tile below for active, fuel, and input (lines 366 to 384, 443 to 456) | The same two hooks on the block class, `animateTick` on the top half reading `type` from the state below. Each subclass keeps its own numbers (tier section). |
| `getBlockLayer` SOLID on the base, CUTOUT on all four brick blocks (line 375; `BlockBrickKiln.java` line 44 and three siblings) | `"render_type": "minecraft:cutout"` in the four brick block models (assets section). |
| `IVariant`, `getStateFromMeta`, `getMetaFromState`, `EnumType.fromMeta`, `META_LOOKUP` (lines 465 to 573) | Gone. `EnumProperty` over a `StringRepresentable` enum. |

### The block entity

| 1.12 construct | 1.21 replacement |
|---|---|
| `TileEntityDataWorkerBase` with `active` and one `progress` float synced; `TileCombustionWorkerBase` adding `burnTimeRemaining` synced every 20 ticks and the rain douse; `TileCombustionWorkerStoneBase` adding the fuel handler and `remainingRecipeTimeTicks` synced every 20 ticks (`library/spi/tile/TileEntityDataWorkerBase.java`, lines 28 to 54; `TileCombustionWorkerBase.java`, lines 17 to 35; `tile/spi/TileCombustionWorkerStoneBase.java`, lines 51 to 86) | `SyncedBlockEntity`. Property: `type` carries `active` and the dormant flag. Snapshot: the fuel stack, the input stack, the output stacks, and the fluid, because the interaction renderers read them. Server-only: `burnTimeRemaining`, `remainingRecipeTimeTicks`, `progress`, `airflowBonus`, `dormantCounter`, `interactionCooldown`. Only Waila and TOP read progress, and both are gone. |
| The `ITileWorker` template loop: inactive check, fuel check, `workerDoWork`, then recompute progress (`TileEntityDataWorkerBase.java`, lines 73 to 108) | One server `BlockEntityTicker` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntityTicker.java`, line 9) with the same order, inlined. Decision 1. |
| `combustionGetBurnTimeForFuel` = `StackHelper.getItemBurnTime(fuel) * getFuelBurnTimeModifier(fuel)` (`TileCombustionWorkerStoneBase.java`, lines 209 to 215) | `stack.getBurnTime(null)` (NF-SRC `net/neoforged/neoforge/common/extensions/IItemStackExtension.java`, line 86), which reads the `neoforge:furnace_fuels` data map, times the tier's modifier. |
| `FuelStackHandler(1)`: limit `getFuelSlotSize()` clamped 1 to 64, refuses non-fuel and items with a container item (lines 524 to 551) | `ItemStackHandler(1)` (NF-SRC `net/neoforged/neoforge/items/ItemStackHandler.java`, line 24) with `getSlotLimit` (line 125) returning the tier size and `insertItem` (line 55) applying `stack.getBurnTime(null) > 0 && !stack.hasCraftingRemainingItem()` (NF-SRC `IItemStackExtension.java`, lines 86 and 73). |
| `workerSetActive(true)` only when `hasFuel()`, writing `bottom_lit` and setting a 5-tick `interactionCooldown`; `false` writing `bottom` (lines 220 to 240) | `ignite()` with the same guard, writing the `type` property. The cooldown existed so the click that lit the machine did not also insert fuel; in an if-chain the ignite branch returns before the fuel branch, so it goes. |
| `consumeAirflow(airflow, simulate)` adding `airflow * AIRFLOW_MODIFIER` to `airflowBonus`, returning 0; `hasCapability` and `getCapability` for `ModuleCore.CAPABILITY_AIRFLOW_CONSUMER` (lines 151 to 197) | Core's plain `AirflowConsumer` interface (SIGN-OFFS, issue #22 item 7) implemented by the block entity. No capability registration. Follow-on (F) item 1 is confirmed: `TileCombustionWorkerStoneBase` implements `IAirflowConsumerCapability` at line 46. |
| Airflow drag: `airflowBonus -= airflowBonus * AIRFLOW_DRAG_MODIFIER` each server tick, floored at an epsilon (lines 241 to 262) | Same in the server ticker. |
| `reduceRecipeTime` and `reduceBurnTimeRemaining` each spending one extra tick per whole point of `airflowBonus` and one more at the fractional probability (lines 297 to 331) | Same arithmetic. This is the whole of what a bellows buys: airflow burns fuel faster and finishes recipes faster in the same ratio. |
| `DORMANT_COUNTER` 50: a lit machine with no fuel or no input counts down and stops, unless `KEEP_HEAT` (lines 48, 137 to 140, 265 to 296) | Same, server-only. `shouldKeepHeat()` is the tier's baked flag. |
| Automation: item handler on any horizontal face for fuel, `UP` for input, `DOWN` for output, any non-`UP` face for the fluid tank, all gated by `ALLOW_AUTOMATION` (lines 151 to 185; `TileCombustionWorkerStoneItemInBase.java`, lines 53 to 81; `...ItemInItemOutBase.java`, lines 72 to 100; `...ItemInFluidOutBase.java`, lines 119 to 145) | `RegisterCapabilitiesEvent#registerBlockEntity` (NF-SRC `net/neoforged/neoforge/capabilities/RegisterCapabilitiesEvent.java`, line 59) for `Capabilities.ItemHandler.BLOCK` and `Capabilities.FluidHandler.BLOCK` (NF-SRC `net/neoforged/neoforge/capabilities/Capabilities.java`, lines 37 and 29), returning the handler the side selects, registered unconditionally as STORAGE decided. The gates drop. |
| `shouldRefresh` returning false when the block is unchanged (lines 405 to 417) | Not needed: 1.21 keeps the block entity across a property change of the same block. |
| `getRenderBoundingBox().expand(0, 1, 0)`; `shouldRenderInPass` both passes (lines 419 to 432) | `BlockEntityRenderer#getRenderBoundingBox` and one `render` call (NF-SRC `net/minecraft/client/renderer/blockentity/BlockEntityRenderer.java`, line 12). |
| `InteractionExtinguish` wrapping `InteractionExtinguishable`: deactivate on a douse (lines 481 to 494) | The douse helper IGNITION rehomed into `library/fluid`, called from the chain. |
| `InteractionUseItemToActivateWorker` for flint and steel and for fire charge, both restricted to the tile's `NORTH` side, the fire charge consumed (lines 92 to 93) | Two branches of `useItemOn`, exact item tests, `SoundEvents.FLINTANDSTEEL_USE` (NF-SRC `SoundEvents.java`, line 530). "NORTH" in interaction terms is the tile's front face, resolved through `getTileFacing`; in 1.21 it is `hit.getDirection() == state.getValue(FACING)`. |
| `InteractionFuel` on the front face, whole block, transform 0.5/0.2/0.5 at half scale (lines 94 to 97, 496 to 521) | The fuel branch of the chain, with the transform moved to the renderer. |
| `isExtendedInteraction` claiming the position above (lines 462 to 470); `getTileFacing` reading the property (lines 452 to 460) | The top-half delegation in the block; facing is the state's property. |

### The three variant bases and the top tiles

| 1.12 construct | 1.21 replacement |
|---|---|
| `TileCombustionWorkerStoneItemInBase`: `InputStackHandler(1)` with limit `getInputSlotSize()`, refusing items with no recipe, refusing what `allowInsertInput` refuses, and trimming to `getAllowedRecipeInputQuantity` (`tile/spi/TileCombustionWorkerStoneItemInBase.java`, lines 26 to 47, 134 to 203) | One `ItemStackHandler(1)` on the shared class with those three tests, resolving the recipe through a `RecipeManager.CachedCheck` (NF-SRC `net/minecraft/world/item/crafting/RecipeManager.java`, line 180) over a `SingleRecipeInput` (NF-SRC `net/minecraft/world/item/crafting/SingleRecipeInput.java`, line 5). |
| The input `Observer` recomputing the recipe time only when the slot goes from empty to filled (lines 205 to 229) | Same rule in `onContentsChanged` (NF-SRC `ItemStackHandler.java`, line 176). Getting this wrong restarts a running recipe when a second item is added. |
| Progress particles every 40 client ticks at y 1.625 when active and the input is not empty, gated by core's client flag (lines 112 to 128) | The client ticker with `Level#addParticle` (NF-SRC `net/minecraft/world/level/Level.java`, line 530), reading CORE's client config. |
| `TileCombustionWorkerStoneItemInItemOutBase`: `OutputStackHandler extends LargeDynamicStackHandler(9)`; `allowInsertInput` refuses while slot 0 of the output is filled; `onRecipeComplete` clears the input and inserts every output item; `workerCalculateProgress`; `dropContents` (`tile/spi/TileCombustionWorkerStoneItemInItemOutBase.java`, lines 31 to 66, 106 to 169) | STORAGE's `LargeStackHandler` with nine slots (decision 5), the same three rules, and `onRemove` for the drop. |
| The output extraction interaction emptying slots 1 to 8 into the player before falling through to slot 0 (lines 195 to 242) | One empty-hand branch that walks all nine slots. |
| `TileCombustionWorkerStoneItemInFluidOutBase`: `OutputFluidTank extends ObservableFluidTank(getOutputFluidTankSize())`; `allowInsertInput` refuses a different fluid or a non-empty tank; `getAllowedRecipeInputQuantity` filling one item at a time until the tank refuses (`tile/spi/TileCombustionWorkerStoneItemInFluidOutBase.java`, lines 37 to 113) | STORAGE's `HotFluidTank` in `library/fluid` over `FluidTank` (NF-SRC `net/neoforged/neoforge/fluids/capability/templates/FluidTank.java`, line 25) with `setValidator` (line 39) for the same-fluid rule. The quantity loop ports as written against `fill(stack, FluidAction.SIMULATE)` (line 96). |
| `fillInternal` breaking both halves and placing the fluid when it is hotter than `HOT_TEMPERATURE` and the tier cannot hold hot fluids (lines 302 to 341) | STORAGE's `HotFluidTank` with the tier's temperature and flag, `FluidType#getTemperature(FluidStack)` (NF-SRC `net/neoforged/neoforge/fluids/FluidType.java`, line 677), and `FluidUtil.tryPlaceFluid` (NF-SRC `net/neoforged/neoforge/fluids/FluidUtil.java`, line 462). STORAGE decision 6 (break on `EXECUTE` only) applies. |
| `processAsynchronous()`: true melts the whole stack at once and empties the slot, false melts one item per cycle (lines 151 to 187, 224 to 235) | Same branch, driven by the tier's `ASYNCHRONOUS_OPERATION` toggle (config section). The false path also swaps the input observer so every insert recomputes the time. |
| Eight `Tile*Top` classes extending `TileCapabilityDelegateMachineTop`, which extends `TileCapabilityDelegate(DOWN, UP)` and forwards `hasCapability` and `getCapability` to the block below (`tile/spi/TileCapabilityDelegateMachineTop.java`, lines 6 to 13; `library/spi/tile/TileCapabilityDelegate.java`, lines 10 to 46) | All nine classes drop. `EntityBlock#newBlockEntity` (NF-SRC `net/minecraft/world/level/block/EntityBlock.java`, line 15) returns null for `type=top`. The delegate existed so a bellows or a pipe facing the top half reached the machine; the bellows resolves the half itself (bellows section), and a pipe facing a top half loses its connection. Decision 3. |
| Eight per-tier tile classes overriding only config getters (`tile/TileStoneKiln.java`, lines 14 to 67, and seven siblings) | One block entity class per shape, reading its numbers from the block, as STORAGE does for its stone variants. |

## The kilns, ovens, sawmills, and crucibles

Four block subclasses and four tile shapes sit on the base. The stone and brick tiers of each are
the same class with different numbers.

**Kiln.** `BlockKilnBase` adds one large smoke particle drifting up from the top centre
(`block/spi/BlockKilnBase.java`, lines 15 to 30): `ParticleTypes.LARGE_SMOKE` (NF-SRC
`ParticleTypes.java`, line 77). `TileKilnBase.getRecipeOutput` rolls the failure chance once per
input item; a failure yields one random failure item, or one pit ash per input if the recipe lists
none (`tile/spi/TileKilnBase.java`, lines 14 to 43). That is the pit kiln's rule, so the shared
`KilnRecipe` class serves both.

**Oven.** `BlockOvenBase` adds a facing-dependent collision box, a chimney the player can stand on,
and a smoke plume offset per facing (`block/spi/BlockOvenBase.java`, lines 24 to 47, 50 to 91). The
collision box becomes a second `getShape` box unioned into the bottom half.
`TileStoneOven.getRecipeOutput` multiplies the output count by the input count
(`tile/TileStoneOven.java`, lines 22 to 30), so an oven cooks a whole stack in one cycle.

**Sawmill.** `BlockSawmillBase` shortens the top box to 4 px, damages anything walking on the top
half while the blade spins, and sprays block or item crack particles from the input
(`block/spi/BlockSawmillBase.java`, lines 29 to 60, 106 to 134). The walk damage becomes
`Block#stepOn` (NF-SRC `Block.java`, line 394) with `level.damageSources().generic()` (NF-SRC
`net/minecraft/world/damagesource/DamageSources.java`, line 141). The crack particles become
`ParticleTypes.BLOCK` with a `BlockParticleOption` when the input is a block item and
`ParticleTypes.ITEM` with an `ItemParticleOption` otherwise (NF-SRC `ParticleTypes.java`, lines 15
and 70); the 1.12 metadata lookup goes with flattening.

`TileSawmillBase` (486 lines) adds a one-slot blade handler, a `recipeComplete` sync flag that
drives the completion sound, and the wood chip spawner
(`tile/spi/TileSawmillBase.java`, lines 48 to 72, 120 to 182, 246 to 313).

| 1.12 construct | 1.21 replacement |
|---|---|
| `BladeStackHandler`: limit 1, refuses anything `isValidSawmillBlade` rejects, which is a config string list per tier (lines 425 to 458; `tile/TileBrickSawmill.java`, lines 75 to 83) | `ItemStackHandler(1)` with limit 1 and two item tags, `#pyrotech:sawmill_blades/stone` and `#pyrotech:sawmill_blades/brick` (decision 6). |
| `getRecipe(input)` calling `StoneSawmillRecipe.getRecipe(input, this.getBlade())` (`tile/TileStoneSawmill.java`, lines 96 to 100) | A `SawmillRecipeInput(ItemStack input, ItemStack blade)` record; `matches` tests both. |
| `onRecipeComplete`: set `recipeComplete`, consume one input, insert the output, spawn `woodChips` wood chip blocks, then damage the blade unless it is on the indestructible list, playing `ENTITY_ITEM_BREAK` when it breaks (lines 184 to 240) | Same, with `stack.hurtAndBreak(1, serverLevel, null, item -> ...)` (NF-SRC `net/minecraft/world/item/ItemStack.java`, line 460), the overload that takes no player, and `SoundEvents.ITEM_BREAK` (NF-SRC `SoundEvents.java`, line 766). |
| `trySpawnWoodChips`: shuffle the 3 by 3 by 3 cube around the machine, place a wood chip rock, raise an existing rock to a level-2 pile, or raise a pile below level 8, nudging entities up 2 px each time (lines 120 to 182) | Same loop against core's rock and wood chip pile blocks, with `entity.setPos` in place of `setPositionAndUpdate`. The 1.12 entity nudge exists because the pile grows under their feet. |
| `recipeComplete` synced boolean, read in `onTileDataUpdate` to play one of three completion sounds at 49/49/2 odds (lines 49, 60 to 65, 246 to 283) | A one-shot sound the server plays with `Level#playSound(null, pos, ...)` (NF-SRC `Level.java`, line 433). The flag never needs to reach the client, so it leaves the snapshot. The 2 percent silent case is faithful, not a bug. |
| Client idle sound every 40 ticks while active with a blade, gated by `IDLE_SOUND_ENABLED` (lines 285 to 313) | The client ticker with the same counter, reading the client config. |
| `InteractionBlade` on all four sides and `UP`, box 1 to 15 px by 20 px tall, blade rotated 90 degrees and spinning while active (lines 336 to 415; `client/render/MillInteractionBladeRenderer.java`, lines 39 to 41) | Five chain branches over the same box, and a `BlockEntityRenderer` drawing the blade with `ItemRenderer#renderStatic` (NF-SRC `net/minecraft/client/renderer/entity/ItemRenderer.java`, line 228), spun by `(gameTime % 360 + partialTick) * 64` degrees while lit. |
| Extract or insert a blade while the machine runs deals `ENTITY_DAMAGE_FROM_BLADE` 3 (lines 373 to 393) | The same test in the two branches. |

**Crucible.** `BlockCrucibleBase.getLightValue` reads the tank's fluid luminosity
(`block/spi/BlockCrucibleBase.java`, lines 27 to 45), which in 1.21 is
`FluidType#getLightLevel(FluidStack)` (NF-SRC `FluidType.java`, line 649) reached from
`IBlockExtension#getLightEmission(state, level, pos)` (NF-SRC
`net/neoforged/neoforge/common/extensions/IBlockExtension.java`, line 155), because the value
depends on the block entity and not on the state. `TileCrucibleBase` adds one bucket interaction on
`UP` over the top box (`tile/spi/TileCrucibleBase.java`, lines 19 to 52), which becomes
`FluidUtil.interactWithFluidHandler(player, hand, level, pos, Direction.UP)` (NF-SRC `FluidUtil.java`,
line 56) as the first branch of the chain. `TileStoneCrucible.reduceRecipeTime` stalls the recipe
whenever the tank cannot take the whole batch (`tile/TileStoneCrucible.java`, lines 112 to 128), so a
full crucible burns fuel without progressing; that is faithful and ports as written.

`CrucibleFluidRenderer` draws one quad inset 2 px at `9 px * fill + 14 px`, still sprite, fluid
colour, lit from the block above (`client/render/CrucibleFluidRenderer.java`, lines 28 to 111). In
1.21 it is STORAGE's fluid box renderer with those numbers.

**What the brick tier changes.** Nothing structural. Per machine: `KEEP_HEAT` true instead of
false, `FUEL_BURN_TIME_MODIFIER` 2.0 instead of 1.0, `FUEL_SLOT_SIZE` 32 instead of 16,
`AIRFLOW_MODIFIER` 1.5 instead of 1.0, `AIRFLOW_DRAG_MODIFIER` 0.01 instead of 0.02, a `CUTOUT`
render layer, the `USE_IRON_SKIN` model swap, and for the crucible `OUTPUT_TANK_SIZE` 8000 instead
of 4000 and `HOLDS_HOT_FLUIDS` true instead of false. The brick sawmill also accepts all seven
blades where the stone sawmill accepts three. Every one of those is a constructor argument or a tag.

## The cog workers

`TileCogWorkerBase` (378 lines) drives five machines. It holds one cog, ticks a counter, and calls
`doWork` when the counter fires and no redstone signal blocks it.

| 1.12 construct | 1.21 replacement |
|---|---|
| `TileEntityDataBase` with a `CogStackHandler(1)` and a `triggered` boolean, both synced (`tile/spi/TileCogWorkerBase.java`, lines 41 to 79) | `SyncedBlockEntity`. Snapshot: the cog stack, because the renderer draws it. `triggered` is a one-shot animation trigger, not state; it becomes a field the sync compares, or a second snapshot boolean. |
| `TickCounter(getUpdateIntervalTicks())`, reset on any cog change (lines 42, 56 to 61) | A plain `int` in the server ticker. `TickCounter` is athenaeum and drops. |
| `update`: return on the client; return with no cog; increment the counter and set `ready`; return while `isPowered()`; on `ready`, `doWork(cog)`, and when the returned damage is at least 0 damage the cog by that much, break it with `ENTITY_ITEM_BREAK` if it runs out, and set `triggered` (lines 144 to 194) | The same loop in a server ticker. `isBlockPowered` is `level.hasNeighborSignal(pos)` (NF-SRC `net/minecraft/world/level/SignalGetter.java`, line 113). Cog damage is `stack.hurtAndBreak(n, serverLevel, null, item -> ...)` (NF-SRC `ItemStack.java`, line 460). A return of -1 means "did nothing", so the counter stays ready; that distinction matters and must survive. |
| `isCogIndestructible(item)` reading a config string list (lines 178 to 180; `ModuleTechMachineConfig.java`, line 253) | The item tag `#pyrotech:indestructible_machine_parts` (decision 6). |
| `ClientRenderData` with `remainingAnimationTime`, `totalAnimationTime`, and an 8-step `cogRotationStage`, advanced on `triggered` and reset when the cog is removed (lines 220 to 249, 348 to 377) | Three fields on the block entity, client-side only, advanced in `onSyncedDataUpdate`. |
| `InteractionCog` on every side over a per-machine box and transform (lines 76 to 98, 267 to 306) | A cog branch in `useItemOn` over the same box, and a `BlockEntityRenderer` drawing the cog. |
| `InteractionCogRenderer`: rotate the cog by `stage * 45` degrees, or by a bounce ease-out from the previous stage while the animation runs (`client/render/InteractionCogRenderer.java`, lines 39 to 53, 106 to 120, 147 to 151) | The same maths in the renderer. The 24-line `bounce` easing function copies over unchanged. |

**Mechanical hopper.** One block, `mechanical_hopper`, with `facing` and a `type` of `down` or
`side`; two more enum values, `down_with_cog` and `side_with_cog`, exist only to pick a ray-trace box
and are never stored (`block/BlockMechanicalHopper.java`, lines 40 to 96, 116 to 141). In 1.21 the
`type` property keeps two values and `getShape` reads the block entity for the cog, which
`getShape(BlockGetter, BlockPos, CollisionContext)` (NF-SRC `BlockBehaviour.java`, line 725) allows.
`TileStoneHopper.doWork` pulls from the item handler above on `DOWN` and pushes into the handler
behind it, moving up to `min(cog durability left, cog transfer amount)` items, damaging the cog once
per operation or once per item depending on `COG_DAMAGE_TYPE`
(`tile/TileStoneHopper.java`, lines 57 to 143). In 1.21 both lookups are
`level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)` (NF-SRC
`net/neoforged/neoforge/common/extensions/ILevelExtension.java`, line 78).

**Mechanical compacting bin.** One block with `type` of `bin` or `machine`, placed as a pair
rotated clockwise from the facing (`block/BlockMechanicalCompactingBin.java`, lines 47 to 57,
249 to 322). `TileMechanicalCompactingBin extends TileCompactingBin` overriding `getRecipe` and
`getInputCapacity` (`tile/TileMechanicalCompactingBin.java`, lines 12 to 22), which corrects
follow-on (D) item 1: it is two overrides, not one, and the class body is lines 9 to 23.
`TileMechanicalCompactingBinWorker.doWork` reads the bin in front of it and drives its recipe
(`tile/TileMechanicalCompactingBinWorker.java`, lines 122 to 156):

- `TileCompactingBin#getCurrentRecipe` (line 131)
- `TileCompactingBin#getInputStackHandler` and its `getTotalItemCount` (line 134)
- `TileCompactingBin#addRecipeProgress` (line 139)
- `TileCompactingBin.InputStackHandler#removeItems` (line 144)
- `TileCompactingBin#resetRecipeProgress` (line 146)

That is five members tech/basic must keep public, not two. The worker's own output is a one-slot
`LargeObservableStackHandler` whose public `insertItem` refuses everything and whose package-private
`insertItemInternal` does the work (lines 244 to 274), so hoppers can pull but not push; in 1.21 that
is a `LargeStackHandler` subclass with `insertItem` returning the stack unchanged. Follow-on (D)
item 2 is confirmed: the worker's `InteractionItem` is typed on `TileSoakingPot` and its validator
returns false (lines 218 to 238). It is dead and drops.

**Mechanical mulch spreader.** One block with `type` of `bin` or `machine`, the machine half having
no block entity and forwarding every interaction to the bin behind it
(`block/BlockMechanicalMulchSpreader.java`, lines 86 to 112, 210 to 226).
`TileMechanicalMulchSpreader.doWork` shuffles a flat square of side `2 * range + 1` one block below
the far side of the machine and turns every mulchable block into core's mulched farmland, up to the
cog's attempt count or the mulch on hand (`tile/TileMechanicalMulchSpreader.java`, lines 134 to 174).
Its `MulchStackHandler` is one slot with a limit of `maxStackSize * CAPACITY` (5 stacks, 320 items),
which is STORAGE's `LargeStackHandler`. Its renderer draws a mulch-coloured quad at
`6/16 * fill + 9/16` and the item count as text
(`client/render/MechanicalMulchSpreaderInteractionMulchRenderer.java`, lines 48 to 49, 101 to 122).

**Trip hammer.** One block, `facing` only, no `type`. `TileTripHammer.doWork` is the only real
cross-unit call in the package (`tile/TileTripHammer.java`, lines 157 to 259):

- refuse unless the block in front holds a `TileAnvilBase` (lines 162 to 169)
- `AnvilRecipe.getTypeFromItemStack(tool)` for the held tool, refuse on null (line 174)
- refuse when the anvil's slot 0 is empty (lines 182 to 187)
- `AnvilRecipe.getRecipe(input, tileAnvil.getRecipeTier(), type)`, refuse on null (line 191)
- `tileAnvil.doInteraction(tool, null, 0.5f, 6f / 16f, 0.5f)` (line 197)
- damage the tool by 1 and break it with a sound (lines 199 to 213)
- an empty result means the anvil broke: return cog damage 1 (lines 215 to 218)
- push the outputs into the item handler clockwise of the facing, spilling on top of the anvil when
  there is none or it is full (lines 220 to 256)

Follow-on (D) item 3 and (E) item 2 are confirmed. TECH-BASIC's public
`hit(ItemStack tool, @Nullable Player player, Vec3 hit)` and the static tool resolver on the shared
`AnvilRecipe` serve every one of these; the trip hammer also needs `getRecipeTier()` and the anvil's
input handler accessor, both already public for the tongs. The trip hammer never reads or writes
anvil wear, so the `damage` property and the `minecraft:block_state` component of SIGN-OFFS issue
#35 are untouched. The tool renderer swings the held tool 90 degrees down over 20 ticks and back
over the rest of the cycle (`client/render/InteractionTripHammerToolRenderer.java`, lines 43 to 64).

**Mechanical bellows.** Two block halves. The top holds a `TileMechanicalBellowsTop extends
TileCogWorkerBase` whose `doWork` sets a `pushing` flag for the down-travel time and returns
`COG_DAMAGE` 8 (`tile/TileMechanicalBellowsTop.java`, lines 82 to 106). Its `isPowered` also tests
the block below (lines 101 to 106), so a signal on either half stops it. The bottom holds a
`TileMechanicalBellows extends TileBellows` whose `shouldProgress` reads the top's flag and whose
push positions are the three blocks in front and to either side
(`tile/TileMechanicalBellows.java`, lines 47 to 77). `breakBlock` removes both halves and drops the
cog at the bottom position (`block/BlockMechanicalBellows.java`, lines 132 to 159); in 1.21 that is
the same `DoorBlock` pattern the machines use.

**Bellows.** `TileBellows` is a plain block entity with one synced `progress` float
(`tile/TileBellows.java`, lines 33 to 48). It descends while a player stands on it and rises
otherwise, lifting the player as it goes, and pushes airflow on every downward tick
(lines 104 to 215).

| 1.12 construct | 1.21 replacement |
|---|---|
| `progress` synced every tick with `TileDataFloat(0, 1)` (line 41) | The one field in the snapshot. It changes every tick while the bellows moves, so this is the one block in the unit that would sync at tick rate. Decision 7. |
| `getAirflow()`: `base * 512 * t^5` for the first quarter of the stroke, `base * (4 * (t - 0.75)^3 + 1)` after, doubled (lines 79 to 93) | Same arithmetic. |
| `shouldProgress()`: any `EntityLivingBase` inside the block's box offset up by `1 - progress * 0.5` is pushed to the plate's height; a player within 0.05 of it drives the bellows (lines 186 to 215) | `level.getEntitiesOfClass(LivingEntity.class, aabb)` with the same box and the same nudge. The gamestages branch drops. |
| `getBoundingBox` shrinking with `progress` (`block/BlockBellows.java`, lines 40 to 57) | `getShape` reading the block entity, which `getShape(BlockGetter, BlockPos, CollisionContext)` allows. |
| `pushAirflow(pos, facing)`: `tileEntity.getCapability(CAPABILITY_AIRFLOW_CONSUMER, facing.getOpposite())` then `consumeAirflow(getAirflow(), false)` (lines 150 to 161) | `level.getBlockEntity(target) instanceof AirflowConsumer c` then `c.consumeAirflow(...)`, with the lookup resolving a top half to the block entity below. Follow-on (F) item 1 is confirmed at lines 150 to 173. Three block kinds have top halves that the 1.12 delegate covered: the bloomery, the wither forge, and the eight stone machines. Decision 3. |
| `TESRBellows` drawing a top quad and a bottom quad at `16 px - 8 px * progress`, sides from `bellows_side` (`client/render/TESRBellows.java`, lines 30 to 98) | A `BlockEntityRenderer` with the same numbers over the block atlas sprites. `TESRMechanicalBellows` does the same with two extra arm quads inset 2 px, from `mechanical_bellows_arm` and `crate_stone_top` (`client/render/TESRMechanicalBellows.java`, lines 31 to 34, 42, 232 to 300). |

## The items and the cog tooltip

| 1.12 construct | 1.21 replacement |
|---|---|
| `ItemSawmillBlade(maxUses)` with `setMaxStackSize(1)` and a config durability (`item/ItemSawmillBlade.java`, lines 21 to 25) | `new Item.Properties().stacksTo(1).durability(n)`. Seven items with the 1.12 numbers: stone 32, flint 150, bone 150, iron 500, gold 32, diamond 1500, obsidian 1345. |
| `ItemCog(maxUses)` plus `setBurnTime(1100)` on the wood cog (`item/ItemCog.java`, lines 28 to 45; `init/ItemInitializer.java`, line 25) | Eight items with durabilities 64, 256, 1024, 1024, 4096, 33, 16384, and `(int) (64 * 256 * 0.8968)`. The wood cog's burn time is one `neoforge:furnace_fuels` entry. |
| `getItemEnchantability` returning gold's enchantability for the gold cog and the gold blade (`ItemCog.java`, lines 47 to 55; `ItemSawmillBlade.java`, lines 27 to 35) | `IItemExtension#getEnchantmentValue(ItemStack)`. Neither item is enchantable in 1.12 without a table entry, so this is decorative; keep it or drop it, no gameplay rides on it. |
| `addInformation` on both items: an indestructible line, else a full-durability line gated by core's client flag (`ItemCog.java`, lines 113 to 123; `ItemSawmillBlade.java`, lines 41 to 53) | `Item#appendHoverText` (NF-SRC `net/minecraft/world/item/Item.java`, line 332) reading the same tag and flag. Lang keys `gui.pyrotech.tooltip.durability.indestructible` and `.full` are on `main`. |
| `ItemCog.getTooltip`: with shift, one line per machine the cog works in, naming the machine and its number (`ItemCog.java`, lines 74 to 111) | `appendHoverText` with `Screen.hasShiftDown()` (NF-SRC `net/minecraft/client/gui/screens/Screen.java`, line 433) reading the cog data map. Keys `gui.pyrotech.tooltip.cog.hopper`, `.compactor`, `.spreader` are on `main` (`en_us.json`, lines 412 to 414). |
| `ItemCog.isCog(stack)`: true when the item appears in any of the three config maps (lines 65 to 72) | True when the item has a `pyrotech:cogs` data map entry. |
| `CogTooltipEventHandler`: on `ItemTooltipEvent`, if the item is not an `ItemCog` but `isCog` says it works as one, splice the cog lines in after line 1 (`event/CogTooltipEventHandler.java`, lines 14 to 41) | An `@EventBusSubscriber(value = Dist.CLIENT)` listener on `ItemTooltipEvent` doing the same. It exists so a pack that adds a third-party item to the cog list gets the tooltip; with the data map it still does, so it stays. |
| The eight machine `addInformation` blocks printing input capacity, fuel capacity, fuel modifier, tank capacity, hot-fluid line, and keep-heat line (`block/BlockStoneKiln.java`, lines 43 to 64, and seven siblings) | One `BlockItem` tooltip helper reading the block's baked numbers and the `SERVER` fuel modifier. All eight lang keys are on `main` (`en_us.json`, lines 390 to 394, 402). |

## Recipes and datagen

### The nine recipe types onto the recipe architecture

`MachineRecipeBase` holds `timeTicks`; `MachineRecipeItemInBase` adds an `Ingredient`;
`MachineRecipeItemInItemOutBase` adds an `ItemStack`; `MachineRecipeItemInFluidOutBase` adds a
`FluidStack`; `MachineRecipeBaseKiln` adds a failure chance and failure items;
`MachineRecipeBaseSawmill` adds a blade `Ingredient` and a wood chip count
(`recipe/spi/`, all six files, 22 to 30 lines each).

| 1.12 class | 1.21 class | Type ids | Input |
|---|---|---|---|
| `StoneKilnRecipe`, `BrickKilnRecipe` | TECH-BASIC's `KilnRecipe`, unchanged | `pyrotech:stone_kiln`, `pyrotech:brick_kiln` | `SingleRecipeInput` |
| `StoneOvenRecipe`, `BrickOvenRecipe` | TECH-BASIC's `DryingRecipe` shape (`Ingredient`, `ItemStack`, `int time`) | `pyrotech:stone_oven`, `pyrotech:brick_oven` | `SingleRecipeInput` |
| `StoneSawmillRecipe`, `BrickSawmillRecipe` | New `SawmillRecipe`: `Ingredient input`, `ItemStack result`, `int time`, `Ingredient blade`, `int woodChips` | `pyrotech:stone_sawmill`, `pyrotech:brick_sawmill` | `SawmillRecipeInput(ItemStack input, ItemStack blade)` |
| `StoneCrucibleRecipe`, `BrickCrucibleRecipe` | New `CrucibleRecipe`: `Ingredient input`, `FluidStack result`, `int time` | `pyrotech:stone_crucible`, `pyrotech:brick_crucible` | `SingleRecipeInput` |
| `MechanicalCompactingBinRecipe` over `CompactingBinRecipeBase` | TECH-BASIC's `CompactingBinRecipe`, unchanged | `pyrotech:mechanical_compacting_bin` | `SingleRecipeInput` |

Nine types, five classes, two of them new. That is nine more types than the recipe architecture's
table lists, so the mod-wide count grows (findings). The `FluidStack` result needs
`FluidStack.CODEC` and `FluidStack.STREAM_CODEC` (NF-SRC `net/neoforged/neoforge/fluids/FluidStack.java`,
lines 64 and 115); no crucible input takes a fluid, so `SizedFluidIngredient`
(NF-SRC `net/neoforged/neoforge/fluids/crafting/SizedFluidIngredient.java`, line 117) does not apply
here.

Every static `getRecipe`, `removeRecipes`, and `RecipeHelper.removeRecipesByOutput` on the nine
classes drops; datapacks override recipe files instead.

### The ovens' derived cook list

`StoneOvenRecipe.getRecipe` caches by item, and when the input has a furnace recipe whose result is
food it builds a recipe on the spot at `COOK_TIME_TICKS`, filtered by a whitelist then a blacklist
(`recipe/StoneOvenRecipe.java`, lines 20 to 99; `BrickOvenRecipe.java`, lines 20 to 99, identical).
`RecipeHelper.hasFurnaceFoodRecipe` is the same athenaeum call tech/basic's `CampfireRecipe` makes,
so this is the campfire rule with a different time. The replacement is TECH-BASIC's: derive from
`RecipeType.SMELTING` (NF-SRC `net/minecraft/world/item/crafting/RecipeType.java`, line 9) results
carrying `DataComponents.FOOD` (NF-SRC `net/minecraft/core/component/DataComponents.java`, line 117),
with one item tag for the exceptions, and keep the two explicit types for datapack additions. The
whitelist, the blacklist, and `blacklistAll` are CraftTweaker surface and drop; the tag replaces
them (decision 8).

### The chain table

Machine owns eight of the twelve edges in the recipe provider's chain table (SIGN-OFFS, issue #21
item 1). The 1.12 modifiers, read from the eight `INHERIT_TRANSFORMER` fields:

| Edge | Time | Failure chance | 1.12 source |
|---|---|---|---|
| pit kiln to stone kiln | 0.5 | 0.25 | `init/recipe/StoneKilnRecipesAdd.java`, lines 26 to 36 |
| stone kiln to brick kiln | 1.0 | 0.25 | `init/recipe/BrickKilnRecipesAdd.java`, lines 14 to 24 |
| drying rack to stone oven | 0.25 | none | `init/recipe/StoneOvenRecipesAdd.java`, lines 20 to 27 |
| stone oven to brick oven | 1.0 | none | `init/recipe/BrickOvenRecipesAdd.java`, lines 14 to 21 |
| chopping block to stone sawmill | 1.0 | none | `init/recipe/StoneSawmillRecipesAdd.java`, lines 46 to 73 |
| stone sawmill to brick sawmill | 1.0 | none | `init/recipe/BrickSawmillRecipesAdd.java`, lines 20 to 29 |
| stone crucible to brick crucible | 1.0 | none | `init/recipe/BrickCrucibleRecipesAdd.java`, lines 26 to 33 |
| compacting bin to mechanical compacting bin | verbatim | none | `init/recipe/MechanicalCompactingBinRecipesAdd.java`, lines 25 to 30 |

Follow-on (C) item 1 is confirmed on every number. Two edges are not plain copies:

- **Chopping block to stone sawmill is a fan-out.** `registerSawmillRecipeWood` turns one parent into
  four sawmill recipes, one per blade group: stone at 240 ticks for 1 output and 4 wood chips, flint
  or bone at 160 ticks for 2 and 2, iron or obsidian at 120 ticks for 2 and 1, diamond at 160 ticks
  for 3 and 1 (`StoneSawmillRecipesAdd.java`, lines 80 to 151). The chain table needs a fan-out
  transform for this edge, or the sawmill copies are written by hand. Decision 9.
- **The mechanical compacting bin edge copies the tool-uses array too.** The 1.12 transform passes
  `recipe.getRequiredToolUses()` through (`MechanicalCompactingBinRecipesAdd.java`, lines 25 to 30),
  which is dead weight because the mechanical bin never uses a tool. The optional recipe field of
  SIGN-OFFS issue #21 item 4 covers it; the copies simply omit the field.

### Own recipes, and who writes them

| Adder | Own recipes | Notes |
|---|---|---|
| `StoneKilnRecipesAdd` | 5 | `cobblestone_from_gravel` (gravel to cobblestone, failing to 6 of one of four core rocks), `refractory_brick`, `quicklime` (`dustLimestone` becomes core's limestone dust tag), `glass` (`sand` becomes `#c:sands`, NF-SRC `net/neoforged/neoforge/common/Tags.java`, line 723), `slag_glass`. All at `Reference.StoneKiln.DEFAULT_BURN_TIME_TICKS` and `DEFAULT_FAILURE_CHANCE`. |
| `StoneCrucibleRecipesAdd` | 12 | Four water recipes (ice 1000 mB, snow layer 500, snowball 125, packed ice 2000), six liquid clay recipes, wood tar from the tar bale, lamp oil from lard. |
| `StoneSawmillRecipesAdd` | 12 | Three names (`board` from `#minecraft:wooden_slabs`, `stick` from a board, `board_tarred` from core's tarred planks) times four blade tiers. |
| `BrickSawmillRecipesAdd` | 24 | Twelve stone-cutting names times two blade tiers (iron or obsidian at `INPUT_SLOT_SIZE * 8 * 20` ticks, diamond at half that). The twelve are sandstone, red sandstone, brick, nether brick, quartz, purpur, stone, cobblestone, and stone brick slabs, stone brick from a slab, and core's masonry and refractory brick slabs. |
| `BrickCrucibleRecipesAdd` | 4 | Lava from stone (a nine-way compound ingredient, which becomes `#c:stones` plus `#c:cobblestones` plus core's cobblestone variants), from gravel, from netherrack at half the time, and from a rock at an eighth the amount and time. |
| `MechanicalCompactingBinRecipesAdd` | 3 | `coal_block` from 9 coal, `coal_block_from_pieces` from 72 coal pieces, `coal_coke_block` from 9 coke. |
| `StoneOvenRecipesAdd`, `BrickOvenRecipesAdd`, `BrickKilnRecipesAdd` | 0 each | Inheritance only. |

That is 60 own recipes. Under the placement rule the last-hoisted unit among type, ingredients, and
result is machine for every one of them, because the type is always machine's and machine hoists
last. Three cases the follow-ons raised:

- **Wood tar from the tar bale** (`StoneCrucibleRecipesAdd.java`, lines 110 to 119) is machine's,
  because machine hoists after refractory, which owns the fluid. Follow-on (B) item 2 confirmed.
- **Lamp oil from lard** (lines 121 to 128) is machine's for the same reason against ignition.
  Follow-on (A) item 2 confirmed; the porting order already puts ignition first.
- **Slag glass** (`StoneKilnRecipesAdd.java`, lines 89 to 103) is machine's, because its type is
  machine's. Follow-on (F) item 2 confirmed: the input is `ModuleTechBloomery.Blocks.PILE_SLAG` at
  metadata 0, the plain slag heap, and the failure items are 4 glass shards and 4 plain slag.

### Inherited copy counts

Using the parent counts the sibling notes give (29 pit kiln recipes plus `smooth_stone`, 5 drying
rack, 11 chopping block, 16 compacting bin plus bloomery's 3 slag recipes):

| Edge | Copies |
|---|---|
| pit kiln to stone kiln | 30 |
| stone kiln to brick kiln | 35 (5 own plus 30 inherited) |
| drying rack to stone oven | 5 |
| stone oven to brick oven | 5 |
| chopping block to stone sawmill | 44 (11 parents times 4 blade tiers) |
| stone sawmill to brick sawmill | 56 (12 own plus 44 inherited) |
| stone crucible to brick crucible | 12 |
| compacting bin to mechanical compacting bin | 19 |

206 copies. Ids keep the 1.12 prefix, so a copy of a copy stacks two prefixes:
`pyrotech:brick_sawmill/stone_sawmill/chopping_block/oak_log_tier_0`, and
`pyrotech:stone_oven/drying_rack/crude_drying_rack/straw`. Follow-on (E) item 1 is confirmed: neither
1.12 kiln adder mirrors furnace recipes, so smooth stone reaches the brick kiln only through the
chain, and machine adds nothing for it.

### The 31 crafting JSONs

Fourteen block recipes, eight cogs, seven sawmill blades, and two `_fallback` variants
(`stone_crucible_fallback.json`, `mechanical_compacting_bin_fallback.json`) that fire only when the
storage module is disabled. With modules gone the two fallbacks drop, leaving 29 recipes through
`ShapedRecipeBuilder` and `ShapelessRecipeBuilder`. Every `modules_enabled` condition goes. Ore
dictionary keys map to tags: `plankWood` to `#minecraft:planks` (NF-SRC
`net/minecraft/tags/ItemTags.java`, line 9), `stickWood` to `#c:rods/wooden` (NF-SRC `Tags.java`,
line 720), `slabWood` to `#minecraft:wooden_slabs` (`ItemTags.java`, line 17), `ingotIron` to
`#c:ingots/iron` (`Tags.java`, line 593). Metadata subitems (`pyrotech:material` data 16 and 23)
become core's flattened items.

Machine's datagen total: 60 own recipes, 206 inherited copies, 29 crafting recipes, 14 loot tables,
and the tags of decision 6.

## Network payloads

Machine registers no packets. `ModuleTechMachine.PACKET_SERVICE` exists only to build the tile data
service (`ModuleTechMachine.java`, lines 54 to 55), and no class in the package sends anything
through it. There is no `network` package. Machine contributes zero payload types to core's
registrar. All sync becomes the PROTOTYPE `SyncedBlockEntity` snapshot; the one block that would
sync at tick rate is the bellows (decision 7).

## Config

`ModuleTechMachineConfig` is 1,302 lines holding 137 fields plus 13 `Stages` fields. The principle
from CORE and SIGN-OFFS issue #21 item 5: behaviour toggles in `COMMON`, gameplay multipliers in
`SERVER`, item-describing numbers in data.

| Category and knob | Default | Fate |
|---|---|---|
| Thirteen `STAGES_*` (lines 18 to 54) | null | Die with gamestages. |
| `GENERAL.STONE_MACHINE_LIGHT_LEVEL` (line 69) | 9 | Bake. |
| `SAWMILL_SOUNDS.RECIPE_COMPLETE_SOUND_ENABLED`, `_VOLUME`, `IDLE_SOUND_ENABLED`, `_VOLUME` (lines 84 to 104) | true, 0.75, true, 0.50 | Core's client config. |
| `SAWMILL_BLADES.*_DURABILITY` (lines 120 to 162) | 32, 150, 150, 500, 32, 1500, 1345 | Bake into `Item.Properties#durability`. |
| `SAWMILL_BLADES.INDESTRUCTIBLE_SAWBLADES` (line 168) | empty | Item tag (decision 6). |
| `COGS.*_DURABILITY` (lines 195 to 244) | 64, 256, 1024, 1024, 4096, 33, 16384, and `(int) (64 * 256 * 0.8968)` | Bake. |
| `COGS.INDESTRUCTIBLE_COGS` (line 250) | empty | Same item tag. |
| `STONE_HOPPER.COGS` (line 278) | eight entries, 1 to 64 items | Data map (decision 6). |
| `STONE_HOPPER.COG_DAMAGE_TYPE` (line 299) | `PerItem` | `COMMON` enum. |
| `STONE_HOPPER.TRANSFER_INTERVAL_TICKS` (line 322) | 40 | `SERVER`. |
| `BELLOWS.TRAVEL_TIME_UP_TICKS`, `_DOWN_TICKS` (lines 338, 345) | 10, 100 | Bake. |
| `BELLOWS.BASE_AIRFLOW` (line 351) | 0.1 | `SERVER`. |
| `TRIP_HAMMER.INTERVAL_TICKS` (line 367) | 100, floored at 40 | `SERVER`. |
| `TRIP_HAMMER.COGS` (line 374) | all eight | Data map presence. |
| `MECHANICAL_BELLOWS.TRAVEL_TIME_UP_TICKS`, `_DOWN_TICKS`, `COG_DAMAGE` (lines 409, 417, 429) | 10, 100, 8 | Bake. |
| `MECHANICAL_BELLOWS.BASE_AIRFLOW` (line 423) | 0.1 | `SERVER`. |
| `MECHANICAL_BELLOWS.COGS` (line 436) | all eight | Data map presence. |
| `MECHANICAL_COMPACTING_BIN.COGS` (line 471) | 0.10 to 0.50 progress per cycle | Data map. |
| `MECHANICAL_COMPACTING_BIN.WORK_INTERVAL_TICKS` (line 509) | 40 | `SERVER`. |
| `MECHANICAL_COMPACTING_BIN.INPUT_CAPACITY`, `OUTPUT_CAPACITY` (lines 516, 522) | 16, 64 | Bake. |
| `MECHANICAL_MULCH_SPREADER.COGS` (line 540) | `range;attempts` pairs, 0;1 to 5;8 | Data map, two int fields. |
| `MECHANICAL_MULCH_SPREADER.ALLOW_AUTOMATION` (line 583) | true | Die. |
| `MECHANICAL_MULCH_SPREADER.COG_DAMAGE_TYPE` (line 595) | `PerItem` | `COMMON` enum. |
| `MECHANICAL_MULCH_SPREADER.WORK_INTERVAL_TICKS` (line 601) | 200 | `SERVER`. |
| `MECHANICAL_MULCH_SPREADER.CAPACITY` (line 607) | 5 stacks | Bake. |
| Eight machines: `ALLOW_AUTOMATION` (lines 622, 703, 776, 858, 932, 1031, 1126, 1218) | true | Die (STORAGE's rule). |
| Eight machines: `KEEP_HEAT` | stone false, brick true | Bake. |
| Eight machines: `FUEL_BURN_TIME_MODIFIER` | stone 1.0, brick 2.0 | `SERVER`; the block item tooltip prints it. |
| Eight machines: `INPUT_SLOT_SIZE`, `FUEL_SLOT_SIZE` | 8, and 16 or 32 | Bake. |
| Eight machines: `AIRFLOW_MODIFIER` | stone 1.0, brick 1.5 | `SERVER`. |
| Eight machines: `AIRFLOW_DRAG_MODIFIER` | stone 0.02, brick 0.01 | Bake. |
| Four brick machines: `USE_IRON_SKIN` (lines 697, 852, 1025, 1212) | true | Decision 4. |
| Two ovens: `COOK_TIME_TICKS` (lines 796, 878) | 2400 both | `SERVER`; it is the derived recipes' time, which JEI shows. |
| Two sawmills: `ENTITY_DAMAGE_FROM_BLADE` (lines 966, 1065) | 3 both | Bake. |
| Two sawmills: `DAMAGE_BLADES` (lines 972, 1071) | true both | `COMMON`. |
| Two sawmills: `SAWMILL_BLADES` (lines 979, 1090) | 3 and 7 entries | Two item tags (decision 6). |
| Two crucibles: `OUTPUT_TANK_SIZE` (lines 1160, 1252) | 4000, 8000 | Bake. |
| Two crucibles: `ASYNCHRONOUS_OPERATION` (lines 1167, 1259) | true both | `COMMON`. |
| Two crucibles: `HOT_TEMPERATURE`, `HOLDS_HOT_FLUIDS` (lines 1175, 1182, 1279, 1286) | 450 both; false, true | Bake. |
| Eight `INHERIT_*` flags and nine `INHERITED_*` modifiers | see the chain table | Die (SIGN-OFFS, issue #21 item 1). Follow-on (C) item 1 says twelve mod-wide; machine's own share is 17. |

That leaves seven `COMMON` values (two cog damage types, two `DAMAGE_BLADES`, two
`ASYNCHRONOUS_OPERATION`, and nothing else once the automation flags die), nineteen `SERVER` values
(four work intervals, two base airflows, eight fuel burn modifiers, eight airflow modifiers, and two
oven cook times, counted once per distinct knob), and four client sound values. Machine adds no
`BASE_RECIPE_DURATION_MODIFIER`, confirming follow-on (C) item 2: the machine tier has no duration
multiplier in 1.12. The fuel burn and airflow modifiers are the multipliers worth keeping, and they
belong in the `SERVER` spec. `ModConfigSpec.Builder#define`, `defineInRange`, `push`, and `pop`
(NF-SRC `net/neoforged/neoforge/common/ModConfigSpec.java`, lines 733, 753, 769, 842, 860).

## Dropped outright

- `plugin/`: 59 files. Fourteen CraftTweaker `Zen*` classes, nineteen JEI classes, nine TOP classes,
  seventeen Waila classes. The nine JEI categories return in the shared plugin; none of the 1.12
  code does.
- The eight `Tile*Top` classes and `TileCapabilityDelegateMachineTop`, with
  `library/spi/tile/TileCapabilityDelegate` (decision 3).
- `TileEntityDataBase`, `TileEntityDataWorkerBase`, `TileCombustionWorkerBase`, `ITileWorker`,
  `TickCounter`, `ObservableStackHandler`, `ObservableFluidTank`, `LargeDynamicStackHandler`,
  `LargeObservableStackHandler`, `ArrayHelper`, `AABBHelper`, `FacingHelper`, `StackHelper`,
  `SoundHelper`, `BlockHelper`, `RandomHelper`, `MathConstants`, and the whole interaction framework.
- `IVariant`, `getStateFromMeta`, `getMetaFromState`, `getActualState`, `getBlockLayer`,
  `getBlockFaceShape`, `isFullBlock`, `isFullCube`, `isOpaqueCube`, `isNormalCube`, `shouldRefresh`,
  `shouldRenderInPass`, `FastTESR`.
- `ModelLoader.setCustomStateMapper`, `BrickMachineStateMapper`,
  `BlockInitializer.registerBrickMachineItemModel`, `PROPERTY_STRING_MAPPER`
  (`init/BlockInitializer.java`, lines 119 to 180). Decision 4.
- The `BlockMechanicalCompactingBin.Item` and `BlockMechanicalMulchSpreader.Item` two-block placers
  (decision 10).
- `TileMechanicalCompactingBinWorker.InteractionItem`, the dead `TileSoakingPot`-typed interaction
  (lines 218 to 238), and the `TileStash` type parameter on the mulch spreader's interaction.
- The nine Forge recipe registries, their `Injector` wiring, and every `removeRecipes` and
  `RecipeHelper.removeRecipesByOutput`.
- `StoneOvenRecipe` and `BrickOvenRecipe`'s whitelist, blacklist, `blacklistAll`, and the
  `SMELTING_RECIPES` cache (decision 8).
- `ModuleTechMachineConfig.isCogIndestructible`, `isSawbladeIndestructible`,
  `StoneHopper.getCogTransferAmount`, `MechanicalCompactingBin.getCogRecipeProgress`,
  `MechanicalMulchSpreader.getCogData` and its string parser (decision 6).
- The `Loader.isModLoaded("gamestages")` branch in `TileBellows.shouldProgress` (lines 205 to 210)
  and the thirteen `getStages()` overrides.
- `ModuleTechMachine.LOGGER`.

## Assets and JEI

What `main` already has, all under `src/main/resources/assets/pyrotech/`:

- Blockstates: `stone_kiln.json`, `brick_kiln.json`, `stone_oven.json`, `brick_oven.json`,
  `stone_sawmill.json`, `brick_sawmill.json`, `stone_crucible.json`, `brick_crucible.json`, each a
  multipart on `facing` and `type`, plus `brick_kiln_brick_only.json` and its three siblings;
  `mechanical_hopper.json` (`facing`, `type=down|side`),
  `mechanical_compacting_bin.json` (`facing`, `type=bin|machine`),
  `mechanical_mulch_spreader.json`, `mechanical_bellows.json` (`facing`, `type=bottom|top`),
  `bellows.json` (`facing`), `trip_hammer.json`.
- Block models under `models/block/gen/`: one directory per machine holding the machine model plus
  `stone_machine_lit`, `stone_machine_dormant`, and for the brick tier `brick_machine_lit`; and
  `_empty.json` for the two blocks whose second half draws nothing.
- Item models: `bellows_item.json`, `mechanical_bellows_item.json`, the eight cogs, the seven blades.
- Textures: the `brick_machine_*` set with a `_brick_only` twin for five of them, `bellows_side`,
  `bellows_side_hole`, `bellows_top`, `mechanical_bellows_side`, `mechanical_bellows_arm`, the
  animated `kiln_active_*` set with `.mcmeta`, and the rest of the machine tiles.
- Sounds: `sounds.json` with `sawmill_idle`, `sawmill_active`, `sawmill_active_short_a`,
  `sawmill_active_short_b`, and the four files under `sounds/sawmill/`.
- Lang: the fourteen block names (lines 168 to 181), the fifteen item names (lines 153 to 167), the
  five machine tooltip keys (lines 390 to 394), the three cog tooltip keys (lines 412 to 414), and
  the nine JEI category names (lines 462 to 470).

Gaps and hand work:

- **The eight machine blockstates have no `type=top` case.** In 1.12 the forge blockstate mapped
  `top` to `pyrotech:_empty` (`origin/1.12`, `blockstates/brick_kiln.json`, lines 27 to 29). A
  multipart with no matching case renders nothing, so the visual result is right, but the top half
  has no particle model. Add a `type=top` case pointing at the machine's `_empty` model, as
  `mechanical_bellows.json` and `mechanical_compacting_bin.json` already do for their second halves.
- **Item models for the twelve block items that lack one.** Only the two bellows have `_item` models;
  the other twelve need `models/item/<id>.json` with the block model as parent.
- **Render types (ticket #27).** The four brick machines returned `CUTOUT`
  (`block/BlockBrickKiln.java`, line 44, and three siblings), so
  `models/block/gen/brick_kiln/brick_kiln.json` and the three siblings, plus their four
  `_brick_only` twins, need `"render_type": "minecraft:cutout"`. The four stone machines and the six
  other blocks used the default solid layer.
- **The `_brick_only` set is dead unless decision 4 keeps it.** Four blockstates, four gen model
  directories, and six textures ride on `USE_IRON_SKIN`.
- **The brick machines' `bottom_lit` state stacks two overlays,** `stone_machine_lit` and
  `brick_machine_lit`, which matches the 1.12 forge blockstate exactly. Not an artifact; leave it.
- **Lang.** No key is missing. `block.pyrotech.mechanical_compacting_bin` reads "Mechanical
  Compactor" and `mechanical_mulch_spreader` reads "Mechanical Mulcher", which is what the cog
  tooltip prints.

**JEI.** The 1.12 plugin registers nine categories, one per machine, each with the machine block as
its catalyst (`plugin/jei/PluginJEI.java`, lines 33 to 196): `stone_kiln`, `brick_kiln`,
`stone_oven`, `brick_oven`, `stone_sawmill`, `brick_sawmill`, `stone_crucible`, `brick_crucible`,
`mechanical_compacting_bin`. Two of them need work beyond listing a recipe type:

- The two oven categories synthesize a wrapper per furnace-food recipe at the tier's
  `COOK_TIME_TICKS` and add them beside the registered recipes (lines 55 to 99).
- The two sawmill categories filter the registered recipes down to those whose blade ingredient
  matches an item in the tier's valid blade list (lines 126 to 180), which is why the brick sawmill
  shows recipes the stone sawmill does not.

The kiln categories show the failure items and percentage, as the pit kiln category does. None of
this is designed here.

## Glossary terms

New terms this note needs, for `CONTEXT.md`:

- **Cog**: the consumable gear that drives a mechanical hopper, compactor, mulcher, trip hammer, or
  mechanical bellows. Eight kinds, from wooden to diamond. A cog has durability, loses some on every
  work cycle, and its kind sets how much work one cycle does. A redstone signal stops the machine
  rather than speeding it.
- **Sawmill blade**: the consumable blade a sawmill needs to cut. Seven kinds. The blade decides
  which recipes the sawmill can run, how fast, how many items come out, and how many wood chips
  spill. A stone sawmill accepts only the first three kinds.
- **Combustion worker**: a Pyrotech machine that burns furnace fuel to run recipes: the eight stone
  and brick tier machines, and in a looser sense the campfire and the pit kiln. It holds a fuel
  slot, keeps a burn time, and airflow makes both the burn and the recipe run faster in step.
- **Keep heat**: a brick tier machine stays lit when it runs out of work, where a stone tier machine
  goes out fifty ticks later. A machine that has gone out but still has burn time left shows the
  dormant look.
- **Machine tier**: stone or brick. The brick tier of a machine takes the stone tier's recipes
  unchanged, doubles the value of its fuel, holds twice the fuel, keeps heat, and reads airflow at
  one and a half times. Only the kiln's failure chance changes with the tier.

The existing **airflow** entry already covers the stone combustion worker and needs no edit.

## Findings for other tickets

- **Tech/basic** (issue #16, hoisting): five members of `TileCompactingBin` must stay public for the
  mechanical compacting bin worker, not two: `getCurrentRecipe`, `getInputStackHandler` (and its
  handler's `getTotalItemCount`), `addRecipeProgress`, `removeItems`, and `resetRecipeProgress`
  (`modules/tech/machine/tile/TileMechanicalCompactingBinWorker.java`, lines 131, 134, 139, 144, 146).
  `TileMechanicalCompactingBin` overrides `getRecipe` and `getInputCapacity`, so the compacting bin
  block entity takes both its recipe type and its input capacity as constructor arguments
  (`tile/TileMechanicalCompactingBin.java`, lines 12 to 22). The trip hammer needs `getRecipeTier()`
  public on the anvil beside `hit` and the input handler accessor
  (`tile/TileTripHammer.java`, line 191). Tech/basic's `DryingRecipe` class gains the two oven types,
  so its map codec must carry no drying-rack-only field. Nothing in machine reads
  `TileCombustionWorkerBase`, so tech/basic's choice to inline it stands (decision 1).
- **Tech/bloomery** (issue #19): the bellows lookup must resolve a bloomery or wither forge top half
  to the block entity below, so the bloomery block needs a public helper that answers "which position
  holds my block entity", or the shared lookup tests `type == top` on any block it knows
  (`modules/tech/machine/tile/TileBellows.java`, lines 150 to 161). Bloomery's slag compacting recipes
  are copied into the mechanical compacting bin by machine's chain edge, so bloomery writes only the
  parents.
- **Core** (issue #10): the `AirflowConsumer` interface needs no capability registration and no
  `Storage` or `Factory` inner classes; the 1.12 file has both
  (`src/api/java/com/codetaylor/mc/pyrotech/IAirflowConsumerCapability.java`, lines 20 to 43). Core
  should also own the bellows-side lookup helper that resolves a two-high block's top half, because
  it serves both bloomery and machine. Core's client config gains the four sawmill sound values.
- **Storage** (issue #28, sign-off): `LargeStackHandler` gains four more users in machine (the
  machine output handler at nine slots, the compactor worker output, the mulch spreader, and the
  compacting bin input through tech/basic). `HotFluidTank` gains the two crucibles. The fluid box
  renderer gains the crucible at inset 2 px and level `9 px * fill + 14 px`. No machine tile extends
  a storage tile; the one `TileStash` reference is a dead type parameter
  (`tile/TileMechanicalMulchSpreader.java`, lines 16 and 257).
- **Tool** (issue #24, sign-off): the trip hammer accepts any item the anvil's static tool resolver
  types, so Pyrotech hammers and pickaxes must answer that resolver. Nothing else in machine reads a
  tool level.
- **Ignition** (issue #18): `BlockCombustionWorkerStoneBase` is the last user of `ItemIgniterBase`
  outside ignition (`block/spi/BlockCombustionWorkerStoneBase.java`, lines 12 and 281). With the
  `#pyrotech:igniters` tag that import goes and machine has no ignition edge in code, only in
  datagen.
- **Refractory** (issue #17): `StoneCrucibleRecipesAdd` consumes `WOOD_TAR` in one recipe at lines
  110 to 119, which is machine's datagen under the placement rule.
- **Bucket** (issue #23, sign-off): the crucible's bucket branch is
  `FluidUtil.interactWithFluidHandler`, so the Pyrotech buckets must expose the item fluid handler
  capability for it to work, which BUCKET already planned.
- **Recipe architecture** (issue #9 follow-up): machine adds nine recipe types over five classes, two
  of them new (`SawmillRecipe` with a two-stack input, `CrucibleRecipe` with a `FluidStack` result).
  The chain table needs a fan-out transform for the chopping block to stone sawmill edge, which turns
  one parent into four children (decision 9). The two oven types must share tech/basic's drying
  recipe class for the drying rack edge to be a plain copy.
- **Render layers** (issue #27): four brick machine models plus their four `_brick_only` twins need
  `render_type` cutout. Everything else in machine is solid.
- **JEI plugin** (the map's "Not yet specified"): nine categories, two of which synthesize furnace
  recipes and two of which filter by a blade tag.
- **Sign-off**: verify in game that a bellows in front of a bloomery top half still speeds it up,
  that a trip hammer with a storage block clockwise of it fills that block, that a mechanical
  compactor stops on a redstone signal to either half, that the brick sawmill accepts a gold blade
  and does nothing with it (decision 11), and that a stone crucible melting lava breaks itself.

## Decisions for Moos

1. **Whether to port `TileCombustionWorkerBase`.** TECH-BASIC inlined the worker framework for the
   campfire and left the call to this ticket. Options: port `TileEntityDataWorkerBase` plus
   `TileCombustionWorkerBase` into the shared library for machine's eight machines; or inline the
   loop into machine's one shared block entity. Recommendation: inline. All eight machines already
   share one class, so the framework abstracts over a single caller, and its `workerUpdateInactive`,
   `workerRequiresFuel`, `workerConsumeFuel`, `workerDoWork`, `workerCalculateProgress` split makes
   the fuel and dormant logic harder to read than a forty-line ticker.
2. **How `bottom_dormant` gets written.** 1.12 derives it in `getActualState`, which 1.21 dropped.
   Options: the server ticker writes the property whenever `burnTimeRemaining > 0` and the machine is
   not active, guarded on a change; or drop the dormant look and let a spent-but-warm machine look
   cold. Recommendation: the ticker writes it. It fires at most twice per burn, once when the machine
   goes out with fuel left and once when the fuel runs out, and the dormant overlay is the only cue
   that a stone machine still has heat banked.
3. **How a bellows finds the machine behind a top half.** 1.12 used `TileCapabilityDelegate`, so any
   capability query on a top half forwarded down. Options: a shared core helper
   `resolveMachineBelow(level, pos)` that tests the known two-high blocks; a marker interface
   `UpperHalf { BlockPos base(BlockPos self); }` on the bloomery, wither forge, and machine blocks
   that the helper calls; or keep a real delegate block entity on every top half. Recommendation: the
   marker interface plus one core helper. It is three block classes implementing one method, it costs
   no block entity, and it gives pipes and other mods a documented way to reach the machine. Accepted
   consequence: an item pipe pointed at a top half stops working, where 1.12's delegate forwarded it.
   Say so in the changelog.
4. **The iron skin toggle.** `USE_IRON_SKIN` on each brick machine swaps the whole model set through
   a `ModelLoader.setCustomStateMapper`, which 1.21 has no equivalent for. Options: drop the toggle
   and ship the iron skin only, deleting the four `_brick_only` blockstates, four model directories,
   and six textures; ship the brick-only set as a built-in resource pack the player enables
   (`AddPackFindersEvent#addPackFinders`, NF-SRC
   `net/neoforged/neoforge/event/AddPackFindersEvent.java`, line 71); or drop the toggle and ship the
   brick-only look instead. Recommendation: the built-in resource pack. The assets are already
   converted and on `main`, the pack is about ten lines of code, and it keeps a cosmetic choice
   cosmetic instead of a config value that only takes effect on a restart.
5. **The machine output handler.** 1.12 uses `LargeDynamicStackHandler(9)`, whose slot limit exceeds
   64. A kiln with an input slot of 8 never produces more than 8 items per cycle, and the extraction
   interaction empties every slot on a click, so the large limit is unused headroom. Options:
   STORAGE's `LargeStackHandler` with nine slots; or a plain `ItemStackHandler(9)`. Recommendation:
   `LargeStackHandler`. STORAGE built it, it costs nothing extra, and a datapack recipe with a large
   output would otherwise silently lose items.
6. **Where the cog and blade data live.** 1.12 keeps three cog maps, two blade lists, and two
   indestructible lists in config, and `ItemCog` reads all three maps for its tooltip. Options: one
   item data map `pyrotech:cogs` with four optional fields (`hopper_transfer`,
   `compactor_progress`, `mulcher_range`, `mulcher_attempts`) plus two blade tags
   (`#pyrotech:sawmill_blades/stone`, `#pyrotech:sawmill_blades/brick`) and one
   `#pyrotech:indestructible_machine_parts` tag; or five separate data maps; or bake everything and
   drop third-party cog support. Recommendation: the one data map plus three tags. It is the fewest
   registrations, it keeps the tooltip handler working for pack-added cogs, which is the only reason
   that handler exists, and `compactor_progress` needs a double while the rest need ints, so one map
   with mixed optional fields is simpler than five maps.
7. **The bellows' per-tick sync.** `progress` changes every tick for the 100 ticks of a down stroke
   and the 10 ticks of the up stroke, and the renderer reads it. Options: put it in the
   `SyncedBlockEntity` snapshot and accept one chunk-scoped packet per tick per moving bellows; or
   send nothing and let the client run the same stroke animation from a start timestamp it gets once.
   Recommendation: the timestamp. The stroke is deterministic given its start tick and the two travel
   times, so one sync when the stroke starts and one when it stops reproduces the animation exactly,
   and a bank of bellows stops being the noisiest block in the mod.
8. **The ovens' derived cook list.** Options: derive from `RecipeType.SMELTING` results carrying
   `DataComponents.FOOD`, with one `#pyrotech:oven_blacklist` item tag, keeping
   `pyrotech:stone_oven` and `pyrotech:brick_oven` as explicit types for datapack additions; or reuse
   tech/basic's `#pyrotech:campfire_blacklist` for both; or write every food recipe out at datagen
   time. Recommendation: derive, with its own tag. It is the campfire decision applied to the same
   athenaeum call, and a separate tag matters because the campfire's exceptions, bread and cookie,
   are about early-game gating rather than about what an oven can hold.
9. **The chopping block to stone sawmill edge.** Every other chain edge is one parent to one child;
   this one is one parent to four children with different times, output counts, wood chip counts, and
   blade ingredients. Options: give the chain table an optional fan-out transform for this edge; or
   drop the edge and have machine's provider read the chopping block recipe list directly and emit
   the 44 copies itself. Recommendation: the fan-out transform. Keeping every edge in one table is
   what makes the table worth having, and the transform is a `Function<Recipe, List<Recipe>>` where
   the others are `Function<Recipe, Recipe>`.
10. **The two-block placements.** The mechanical compacting bin and the mulch spreader place a
    partner block through a custom `ItemBlock` that overrides `onItemUse` and calls `placeBlockAt`
    twice. Options: keep a custom `BlockItem` with a `useOn` override; or use `getStateForPlacement`
    returning null when the partner spot is blocked plus `setPlacedBy` placing the partner, which is
    what the stone machines and the bloomery already do. Recommendation: `setPlacedBy`. It deletes
    two classes, it matches the pattern three other blocks in this unit use, and vanilla's door code
    is the reference implementation for the failure cases.
11. **The gold sawmill blade.** The brick sawmill's valid blade list includes
    `sawmill_blade_gold` (`ModuleTechMachineConfig.java`, line 1090), and a player can insert one,
    but no recipe's blade ingredient names it: the four blade groups are stone, flint or bone, iron
    or obsidian, and diamond (`init/recipe/StoneSawmillRecipesAdd.java`, lines 92, 107 to 110,
    125 to 128, 143). A gold blade in a sawmill does nothing. Options: keep it faithful, so gold
    blades stay craftable and useless; or add gold to the stone blade group, giving it the stone
    timings to match its stone-level durability of 32. Recommendation: add it to the stone group.
    The gold cog is deliberately the worst cog and still works, the gold blade's presence in the
    valid list shows the intent, and a craftable item that does nothing reads as a bug to players.
12. **The config surface.** Beyond decision 4, the config lands at seven `COMMON` toggles, nineteen
    `SERVER` multipliers, and four client sound values. Options: accept that table; or also promote
    the eight `AIRFLOW_DRAG_MODIFIER` and eight `KEEP_HEAT` values, which the table bakes.
    Recommendation: accept the table. Drag is a feel constant nobody tunes, and keep-heat is the
    brick tier's headline feature rather than a knob.

## Claims not verified

- Every JEI API call. The 1.12 plugin's `IRecipeCategoryRegistration`, `addRecipeCatalyst`,
  `handleRecipes`, and `addRecipes` are JEI 4 for 1.12; nothing here was checked against a 1.21 JEI
  jar, which is not in this project's dependencies.
- The athenaeum classes named as dropped (`TileEntityDataBase`, `ObservableStackHandler`,
  `LargeDynamicStackHandler`, `LargeObservableStackHandler`, `TickCounter`, `RecipeHelper.inherit`,
  `RecipeHelper.hasFurnaceFoodRecipe`, `StackHelper`, `AABBHelper`, `FacingHelper`, `ArrayHelper`,
  `SoundHelper`, `BlockHelper`, `RandomHelper`, `MathConstants`, `InteractionItemStack`,
  `InteractionBucketBase`, `InteractionExtinguishable`, `Transform`, `IInteractionRenderer`). The
  library is not in this repo; they are described from their use sites only.
- The parent recipe counts used for the copy arithmetic (29 plus 1 pit kiln, 5 drying rack, 11
  chopping block, 16 compacting bin, 3 bloomery slag) come from TECH-BASIC and BLOOMERY, not from a
  fresh count here.
- `SoundEvent.createVariableRangeEvent` is the right constructor for the four sawmill sounds, but the
  1.12 `SoundEvent(ResourceLocation)` carried no range, so whether any of the four wants
  `createFixedRangeEvent` is a play-test question.
- The claim that a multipart blockstate with no matching case renders an empty model rather than the
  missing-model cube. It matches how the converted files were written, but it was not tested in game.
