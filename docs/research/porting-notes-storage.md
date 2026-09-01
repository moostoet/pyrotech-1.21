# Porting notes: storage

Resolves issue #14 (porting notes: storage).
Storage is a hoisting unit from issue #4 (module porting order): 72 files, 8,809 lines.
It holds the stash, the shelf, the crate, the wood rack, the two rock bags, the stone and
refractory tanks, and the stone and refractory faucets. Six of those have a durable stone or
brick twin. 27 of the 72 files are plugin code that drops. `TankFlushRecipe` rehomes here from core.
This document lists each 1.12 construct and its 1.21 replacement.
Decisions that need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo, package `com.codetaylor.mc.pyrotech.modules.storage`,
  plus `modules/core/recipe/TankFlushRecipe.java`, the recipes under
  `assets/pyrotech/recipes/storage/`, and `ModuleStorageConfig.java`.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths cited below are entries in that jar.
- **NF-DOCS**: docs.neoforged.net, 1.21.1 version pages. URLs cited inline.
- **DATA**: the vanilla 1.21.1 data pack, at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`.
- **CORE**: `docs/research/porting-notes-core.md` on branch `research/porting-notes-core`.
- **RECIPES**: `docs/research/recipe-architecture.md` on branch `research/recipe-architecture`.
- **PROTOTYPE**: branch `prototype/8-campfire-interaction-sync`,
  `docs/prototypes/campfire-interaction-sync.md` and the Java under
  `src/main/java/com/moostoet/pyrotech/prototype/campfire/`.
- **HUNTING**: `docs/research/porting-notes-hunting.md` on branch `research/porting-notes-hunting`.
- **BUCKET**: `docs/research/porting-notes-bucket.md` on branch `research/porting-notes-bucket`.
- **ASSETS**: `docs/asset-migration-report.md` and `docs/asset-conversion-report.md` on `main`.
- **DEPS**: `docs/research/module-dependencies.md` on branch `research/module-dependencies`.

This document assumes the recommended answers from CORE's "Decisions for Moos" section.
It applies PROTOTYPE pattern 1 (per-block if-chain dispatch) and pattern 2 (blockstate
properties plus `SyncedBlockEntity`) to every storage block and tile.

## Summary

- The 13 blocks are already one registry entry each in 1.12. Only the bag item's open and
  closed metadata flattens, into a data component. Stone variants become constructor arguments
  on shared classes, so 7 block entity types serve 13 blocks.
- Every inventory becomes a NeoForge `ItemStackHandler` behind `Capabilities.ItemHandler.BLOCK`.
  One hard fact shapes this: `ItemStack.CODEC` refuses counts above 99. The stash, the durable
  shelf and crate, and the bags need a large-stack handler with its own serialization, and the
  bag needs a custom component. The 1.12 phantom-slot hopper hack dies.
- Tanks and faucets move to `FluidTank`, `IFluidHandler`, and `FluidUtil`. The tank's connection
  state becomes the `connection` blockstate property the converted blockstate already expects.
  The two TESRs become `BlockEntityRenderer`s drawing sprites from `IClientFluidTypeExtensions`.
- Interaction is the prototype's if-chain in `useItemOn` and `useWithoutItem`. The shelf, crate,
  and wood rack map hit positions to slots with a small facing rotation of
  `BlockHitResult.getLocation()`.
- Tile sync follows the prototype. `connection` and the bag's `type` are blockstate properties.
  Inventories and fluid stacks ride the `SyncedBlockEntity` snapshot. The faucet's per-tick
  refill of its display tank must not trigger a sync.
- Tank and bag items carry contents as data components copied by the loot table, the shulker box
  pattern. The tank uses `SimpleFluidContent`, exactly as BUCKET decided for buckets.
- The bag auto-pickup handler moves to `ItemEntityPickupEvent.Pre`. Storage registers no
  payloads of its own.
- Contrary to DEPS and the ticket text, no tech or ignition tile extends a storage tile in 1.12.
  What they share is the hot-fluid rule and the large-stack problem. Storage extracts both into
  a small shared package.

## Module bootstrap

`ModuleStorage` follows the CORE replacements: one `register(IEventBus)` hook, `DeferredRegister`
fields as the holders, no `ModuleBase`, no `Registry`, no `@GameRegistry.ObjectHolder`.
`PACKET_SERVICE` and `TILE_DATA_SERVICE` die with athenaeum. The CraftTweaker plugin loop, the
TOP `FMLInterModComms` call, and the Waila IMC message are dropped with the plugin package.

| 1.12 construct | 1.21 replacement |
|---|---|
| `BlockInitializer.onRegister`: 11 `registerBlockWithItem`, 2 tanks with `ItemBlockTank`, 2 bags with `ItemBlockBag` | `DeferredRegister.Blocks#registerBlock` plus `DeferredRegister.Items#registerSimpleBlockItem` for 9 blocks (CORE). The tanks and bags register their own `BlockItem` subclasses through `Items#register`. |
| `RegistryHelper.registerTileEntities` with 13 tile classes, one per block | 7 `BlockEntityType`s. `BlockEntityType.Builder.of(factory, block, stoneBlock).build(null)` accepts several valid blocks (NF-SRC `net/minecraft/world/level/block/entity/BlockEntityType.java`, lines 337 and 341). The stone tiles (`TileStashStone`, `TileShelfStone`, `TileCrateStone`, `TileTankBrick`, `TileBagDurable`, `TileFaucetBrick`) only override config getters, so the block entity reads its limits from its block instead. |
| `onClientRegister`: `ModelRegistrationHelper.registerBlockItemModels`, `registerBlockItemModelForMeta` for the open bag | Item models load by item id. See the asset gap below. |
| `ClientRegistry.bindTileEntitySpecialRenderer` for `TESRTank`, `TESRFaucet`, and six `TESRInteractable` | `EntityRenderersEvent.RegisterRenderers#registerBlockEntityRenderer` (NF-SRC `net/neoforged/neoforge/client/event/EntityRenderersEvent.java`, line 109; NF-DOCS https://docs.neoforged.net/docs/1.21.1/blockentities/ber). |
| `TESRFaucet.updateBlockMatchersFromConfig()` in client post-init | Dropped, see the faucet render cutoff decision. |
| `ModuleStorage.CREATIVE_TAB` | Items join core's tab in `BuildCreativeModeTabContentsEvent` (PROTOTYPE `PrototypeCampfire.addToCreativeTab`). |

Asset gap. `main` has no `models/item/` file for any of the 13 storage block items. ASSETS lists
five blocks that need item models, and none of them is a storage block, because the 1.12 storage
blockstates had no `inventory` variant. Each storage block item still needs a
`models/item/<id>.json` whose parent is its block model. The tank and faucet items point at the
generated wrappers under `models/block/gen/`. The bag needs two, closed and open (see item forms).

## Blocks and their stone variants

All eight block base classes extend athenaeum's `BlockPartialBase`, whose source is not in the
repo. The 1.21 base is `BaseEntityBlock` (NF-SRC `net/minecraft/world/level/block/BaseEntityBlock.java`,
line 14) with `BlockBehaviour.Properties` (NF-SRC `net/minecraft/world/level/block/state/BlockBehaviour.java`,
`mapColor` line 1149, `noOcclusion` line 1165, `sound` line 1185).

| Block | 1.12 material, hardness, resistance, sound, tool | 1.21 properties |
|---|---|---|
| `stash`, `shelf`, `crate` | `WOOD`, 2.0, 5.0, `WOOD` | `mapColor(WOOD).strength(2.0f, 5.0f).sound(WOOD).noOcclusion()` |
| `stash_stone`, `shelf_stone`, `crate_stone` | still `Material.WOOD` and `SoundType.WOOD`, 1.5, 10.0 | `strength(1.5f, 10.0f)`. 1.12 kept the wood sound on the stone variants. Keep it, or switch to `SoundType.STONE` as a cosmetic fix. |
| `wood_rack` | `WOOD`, 0.75, axe level 0, no sound type set | `strength(0.75f)`, block tag `minecraft:mineable/axe` (NF-SRC `net/minecraft/tags/BlockTags.java`, line 139). Level 0 means no `requiresCorrectToolForDrops`. The missing sound type is a 1.12 accident: `BlockWoodRack` calls `super(Material.WOOD)` and never `setSoundType`, unlike every other storage block, `BlockPartialBase` on athenaeum's GitHub master does not set one either, and vanilla 1.12's `Block` constructor defaults to `SoundType.STONE`, so the rack plays stone sounds. `sound(SoundType.WOOD)` (NF-SRC `net/minecraft/world/level/block/SoundType.java`, line 10) is the honest choice, noted here so it is a choice and not a slip. |
| `stone_tank`, `brick_tank` | `ROCK`, 2.0, pickaxe level 0 | `mapColor(STONE).strength(2.0f)`, tag `minecraft:mineable/pickaxe` (line 141). Occluding full cube. |
| `faucet_stone`, `faucet_brick` | `ROCK`, 1.5, 2.5, `STONE`, pickaxe 0 | `strength(1.5f, 2.5f).sound(STONE).noOcclusion()`, pickaxe tag. |
| `bag_simple`, `bag_durable` | `CLOTH`, 0.2, `CLOTH` | `mapColor(WOOL).strength(0.2f).sound(SoundType.WOOL).noOcclusion()` (NF-SRC `net/minecraft/world/level/block/SoundType.java`, line 37; `net/minecraft/world/level/material/MapColor.java`, line 10). |

What differs between a wood block and its durable twin is only hardness, resistance, and the
config numbers below (stack multipliers, bag capacity, tank capacity, hot-fluid tolerance,
faucet rate and limit). Each 1.12 pair is already two registry entries, so nothing flattens
here. In 1.21 one block class per kind takes those numbers as constructor arguments, and the
block entity asks its block for them.

| 1.12 construct | 1.21 replacement |
|---|---|
| athenaeum `Properties.FACING_HORIZONTAL` plus `getStateFromMeta`/`getMetaFromState`/`damageDropped` on every block | `BlockStateProperties.HORIZONTAL_FACING` (NF-SRC `net/minecraft/world/level/block/state/properties/BlockStateProperties.java`, line 54) declared in `createBlockStateDefinition` (NF-DOCS https://docs.neoforged.net/docs/1.21.1/blocks/states). Metadata is gone. The converted blockstates on `main` already key on `facing=`. |
| `getStateForPlacement` reading `placer.getHorizontalFacing().getOpposite()` | `getStateForPlacement(BlockPlaceContext)` with `context.getHorizontalDirection().getOpposite()` (NF-SRC `net/minecraft/world/item/context/UseOnContext.java`, line 70). The faucet uses `getClickedFace()` (line 41) instead, as in 1.12. |
| `BlockShelfBase.TYPE` (back, forward) chosen from `hitX`/`hitZ` in `getStateForPlacement` | `EnumProperty` `type`, chosen from `context.getClickLocation()` (line 45) minus the block position. The converted `shelf.json` blockstate already has `type=back` and `type=forward`. |
| `BlockBagBase.TYPE` (closed, open) chosen from the held item's metadata | Same `EnumProperty`, chosen from the bag item's `open` component (item forms). Converted `bag_simple.json` has `type=closed` and `type=open`. |
| `BlockTankBase.CONNECTION` (none, up, down, both) computed in `getActualState` from two tile booleans | A real blockstate property written by the block entity. `getActualState` does not exist in 1.21. The converted `stone_tank.json` keys on `connection=`. |
| `getBoundingBox` AABBs | `getShape` (NF-SRC `BlockBehaviour.java`, line 361) with `Block.box` (NF-SRC `net/minecraft/world/level/block/Block.java`, line 151). Values: stash 0,0,0 to 16,6,16; wood rack 0,0,0 to 16,14,16; shelf 6 deep against the back or front wall per facing and type; faucet 4,4,10 to 12,10,16 rotated; bag 3,0,5 to 13,10,11 rotated; crate and tank full cubes. |
| `isSideSolid` overrides (stash: down only; crate: not up; shelf: none; tank: all) | Derived from the shape through `isFaceSturdy` (NF-SRC `BlockBehaviour.java`, line 944). Use `forceSolidOn()`/`forceSolidOff()` (lines 1244, 1250) only if a shape gives the wrong answer. |
| `breakBlock` calling `StackHelper.spawnStackHandlerContentsOnTop` | `onRemove` (line 193) guarded by `!state.is(newState.getBlock())`, dropping each slot with `Containers.dropItemStack` (NF-SRC `net/minecraft/world/Containers.java`, line 31), as PROTOTYPE `CampfireBlock.onRemove` does. `dropItemStack` splits a large stack into item entities of 10 to 30 items (lines 39 to 47), which is what the athenaeum helper did. |
| `hasTileEntity`/`createTileEntity` | `EntityBlock#newBlockEntity` (NF-SRC `net/minecraft/world/level/block/EntityBlock.java`, line 15). |
| `getLightValue` on the tank from `Fluid.getLuminosity` | `IBlockExtension#getLightEmission(state, level, pos)` (NF-SRC `net/neoforged/neoforge/common/extensions/IBlockExtension.java`, line 155) reading `FluidType#getLightLevel(FluidStack)` (NF-SRC `net/neoforged/neoforge/fluids/FluidType.java`, line 649). Call `LevelLightEngine#checkBlock` (NF-SRC `net/minecraft/world/level/lighting/LevelLightEngine.java`, line 27) when the fluid changes, as the prototype does for fuel. |
| `addInformation` on tanks, faucets, and the bag item | `Item#appendHoverText` (NF-SRC `net/minecraft/world/item/Item.java`, line 332). The lang keys already exist on `main` (`gui.pyrotech.tooltip.fluid`, `.fluid.capacity`, `.fluid.transfer.rate`, `.fluid.transfer.limit`, `.hot.fluids.true/false`, `.contents.retain.true/false`, `.item.capacity`, `.item.capacity.empty`, `.extended.shift`). |
| Faucet `canPlaceBlockOnSide` (a fluid handler must sit behind) | `canSurvive` (line 345) querying `level.getCapability(Capabilities.FluidHandler.BLOCK, behindPos, facing)` (NF-SRC `net/neoforged/neoforge/common/extensions/ILevelExtension.java`, line 78; `net/neoforged/neoforge/capabilities/Capabilities.java`, line 29). |
| Faucet `neighborChanged`: break when the block behind is air, activate when powered | `neighborChanged` (line 186) with `isEmptyBlock` (NF-SRC `net/minecraft/world/level/LevelReader.java`, line 93), `destroyBlock` (NF-SRC `net/minecraft/world/level/LevelWriter.java`, line 31), and `hasNeighborSignal` (NF-SRC `net/minecraft/world/level/SignalGetter.java`, line 113). |
| Tank `neighborChanged`: connection update, regroup, settle fluids | Same three calls on the block entity from `neighborChanged`. |
| `removedByPlayer`/`harvestBlock`/`getDrops` on tanks and bags, serializing the tile into the drop | Dropped. The loot table copies components from the block entity (item forms). `getCloneItemStack` (NF-SRC `Block.java`, line 447) covers pick-block, as `ShulkerBoxBlock` does (NF-SRC `net/minecraft/world/level/block/ShulkerBoxBlock.java`, line 249). |

Render types (issue #27). `BlockTankBase.getBlockLayer()` returned `SOLID`, so the converted tank
models need no `render_type` field; the default is solid (NF-SRC
`net/neoforged/neoforge/client/model/ExtendedBlockModelDeserializer.java`, line 66; NF-DOCS
https://docs.neoforged.net/docs/1.21.1/resources/client/models/). The `_empty` texture that the
converted `gen/stone_tank/tank_up.json`, `tank_down.json`, and `tank_both.json` assign is only
bound to texture slots no face uses (`tank_up` has zero `#top` faces, `tank_down` zero `#bottom`,
`tank_both` neither), so it never draws. `BlockPartialBase` set the layer for the other blocks
and cannot be read. The pixels say: faucet, crate, shelf, and stash textures have no transparent
pixels, so solid is right. The bag body textures (front, side, top, back, bottom, inside) are 61 to 77 percent fully transparent with black
underneath, so the bag models (`bag_simple.json`, `bag_simple_open.json`, and the durable pair)
need `"render_type": "minecraft:cutout"` (NF-SRC `net/neoforged/neoforge/client/NamedRenderTypeManager.java`,
line 46). The fluid inside tanks and faucets is drawn by the renderers below, not by models.

## Block entities and inventories

| 1.12 construct | 1.21 replacement |
|---|---|
| `TileEntityDataBase` (athenaeum) | PROTOTYPE `SyncedBlockEntity`. |
| `LargeObservableStackHandler`, `ObservableStackHandler`, `addObserver(markDirty)` | `ItemStackHandler` (NF-SRC `net/neoforged/neoforge/items/ItemStackHandler.java`, line 17) with `onContentsChanged` (line 176) calling `setChanged` and `sync()`. |
| `hasCapability`/`getCapability` gated by `ALLOW_AUTOMATION` | `RegisterCapabilitiesEvent#registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> ...)` (NF-SRC `net/neoforged/neoforge/capabilities/RegisterCapabilitiesEvent.java`, line 59; `Capabilities.java`, line 37; NF-DOCS https://docs.neoforged.net/docs/1.21.1/inventories/capabilities). The bag returns its handler only for `Direction.DOWN`, as in 1.12. |
| `getStackLimit` returning `stack.getMaxStackSize() * maxStacks` | Override both `getStackLimit(slot, stack)` (line 129) and `getSlotLimit(slot)` (line 125). The default slot limit is `Item.ABSOLUTE_MAX_STACK_SIZE`, 99 (NF-SRC `Item.java`, line 65). |
| The phantom empty slot (`getSlots()` returns one more than real) on stash, shelf, and crate, from 1.12 issue 380 | Dropped. NeoForge's hopper hook still tests `isFull` by `getCount() < getSlotLimit(slot)` (NF-SRC `net/neoforged/neoforge/items/VanillaInventoryCodeHooks.java`, lines 104 and 189 to 196). With `getSlotLimit` overridden to the real multi-stack limit, a stash holding 64 rocks is not "full" and the hopper inserts. Verify with a hopper in game. |
| `LargeDynamicItemLimitedStackHandler(10, itemCapacity)` on the bag: 10 slots, a total item cap, slot limit `Integer.MAX_VALUE`, extraction from the last filled slot | A `LargeStackHandler` subclass (below) with the same total-count rule in `insertItem` and the same last-slot `extractItem`. |
| Bag `isItemValidForInsertion` parsing whitelist and blacklist strings with `RecipeItemParser` | `stack.is(tag)` against two item tags, see decision 2. |
| Wood rack `insertItem` rejecting non-`logWood` | `isItemValid` returning `stack.is(ItemTags.LOGS)` (NF-SRC `net/minecraft/tags/ItemTags.java`, line 25), with `insertItem` honouring it as the prototype's handlers do. |
| Wood rack `settleStacks` (upper row falls into the row below, `settlingStacks` re-entrancy guard) | Same loop in `onContentsChanged`, server side only. |
| `StackHelper.spawnStackHandlerContentsOnTop` in `dropContents` | `Containers.dropItemStack` per slot (above). |
| `writeToNBT`/`readFromNBT` with `stackHandler.serializeNBT()` | `saveSynced`/`loadSynced` with `serializeNBT(registries)` and `deserializeNBT(registries, tag)` (lines 139 and 155), as PROTOTYPE `CampfireBlockEntity` does. |

Slot counts and limits, from the 1.12 tiles and `ModuleStorageConfig` defaults:

| Tile | Real slots | Per-slot limit | Notes |
|---|---|---|---|
| `TileStash` | 1 | 10 stacks (640 for a 64-stack item) | One item type by construction: every insert targets slot 0. `doItemStackValidation` returns true (a 1.12 `TODO` for a whitelist). |
| `TileStashStone` | 1 | 20 stacks | |
| `TileShelf` | 9 | 1 stack | |
| `TileShelfStone` | 9 | 2 stacks | |
| `TileCrate` | 9 | 1 stack | |
| `TileCrateStone` | 9 | 2 stacks | |
| `TileWoodRack` | 9 | 1 stack | Logs only. |
| `TileBagSimple` | 10 | unbounded per slot, 640 items total | Whitelist. Automation from below only. |
| `TileBagDurable` | 10 | unbounded per slot, 2560 items total | Larger whitelist. |

The 99 problem. `ItemStack.CODEC` validates `count` with `intRange(1, 99)` (NF-SRC
`net/minecraft/world/item/ItemStack.java`, line 107), and `ItemStackHandler#serializeNBT` saves
each slot with `stack.save(provider, tag)` (line 145), which throws on a failed encode (NF-SRC
`net/neoforged/neoforge/common/util/DataComponentUtil.java`, line 24). So a stock handler cannot
save a stash slot holding 640 cobblestone, a durable crate slot holding 128, or a bag slot
holding 500 rocks. Athenaeum's `Large*` handlers existed for the same reason in 1.12 (byte counts).
The `SyncedBlockEntity` snapshot is a `CompoundTag`, so it hits the same limit. The port needs a
`LargeStackHandler extends ItemStackHandler` that overrides `serializeNBT` and `deserializeNBT` to
write each slot as `ItemStack.SINGLE_ITEM_CODEC` (item id plus components, count fixed at 1,
lines 115 to 125) plus a separate `count` int. The network stream codec has no such limit
(`OPTIONAL_STREAM_CODEC` writes the count as a var int, lines 131 to 152), which matters only if a
payload ever carries these stacks. The wood rack and the plain shelf and crate can use a stock
`ItemStackHandler`. See decision 1.

## Tanks and faucets

| 1.12 construct | 1.21 replacement |
|---|---|
| `ObservableFluidTank` (athenaeum), `FluidTank.fill(FluidStack, boolean)` | `FluidTank` (NF-SRC `net/neoforged/neoforge/fluids/capability/templates/FluidTank.java`, line 20) with `onContentsChanged` (line 152). Booleans become `IFluidHandler.FluidAction.EXECUTE`/`SIMULATE` (NF-SRC `net/neoforged/neoforge/fluids/capability/IFluidHandler.java`, line 18). Capacity 4000 (stone) and 8000 (brick). |
| `null` fluid, `FluidStack.amount`, `isFluidEqual` | `FluidStack.EMPTY` and `isEmpty()` (NF-SRC `net/neoforged/neoforge/fluids/FluidStack.java`, lines 144 and 203), `getAmount()`/`copyWithAmount` (lines 430 and 312), `FluidStack.isSameFluidSameComponents` (line 349). |
| The tank's group-spanning `FluidHandler` (`getTankProperties`, `fill` into the group bottom-up, `drain` top-down, refuse a second fluid) | An `IFluidHandler` over the tank group implementing `getTanks`, `getFluidInTank`, `getTankCapacity`, `isFluidValid`, `fill`, `drain` (lines 35 to 103), exposed through `Capabilities.FluidHandler.BLOCK` (line 29) in `RegisterCapabilitiesEvent`. |
| `Tank.fill` override: accept, then if hot and `!canHoldHotFluids` set air, play `ENTITY_ITEM_BREAK`, `FluidUtil.tryPlaceFluid`, send `SCPacketParticleCombust`. The check ignores `doFill`, so a simulated fill also breaks the tank | A `fill` override on the tank; decision 6 settles the simulate case. Temperature is `FluidType#getTemperature(FluidStack)` (NF-SRC `FluidType.java`, line 677), threshold 450, matching BUCKET. Sound `SoundEvents.ITEM_BREAK` (NF-SRC `net/minecraft/sounds/SoundEvents.java`, line 766). Spill with `FluidUtil.tryPlaceFluid(player, level, hand, pos, IFluidHandler, FluidStack)` (NF-SRC `net/neoforged/neoforge/fluids/FluidUtil.java`, line 462). Particles through `ServerLevel#sendParticles` (NF-SRC `net/minecraft/server/level/ServerLevel.java`, line 1258); CORE dropped the packet. |
| The "Empty" NBT tag workaround in `Tank.writeToNBT`/`readFromNBT` | Dropped. `FluidTank#writeToNBT(HolderLookup.Provider, CompoundTag)` and `readFromNBT` (lines 67 and 62) handle the empty case through `FluidStack.OPTIONAL_CODEC` (line 89). |
| `InteractionBucket extends InteractionBucketBase` (bucket fill and drain on any face) | `FluidUtil.interactWithFluidHandler(player, hand, level, pos, side)` (line 56) in `useItemOn`. It works for vanilla buckets and for the bucket module's items through `Capabilities.FluidHandler.ITEM` (line 31). |
| Two `TileDataBoolean`s (up, down), `tryConnectUp`/`tryConnectDown`, `updateConnectionsForPlacement(side)`, `updateConnectionsForNeighborChanged` | The `connection` blockstate property. Placement logic keeps its shape: it needs the clicked face and the neighbours' fluids, so run it from `setPlacedBy` (NF-SRC `Block.java`, line 414) with the face captured in `getStateForPlacement`, or from a `BlockItem#place` override (NF-SRC `net/minecraft/world/item/BlockItem.java`, line 57). Item forms explains why the `place` override is the faithful shape. |
| `tankGroup` list rebuilt by `updateTankGroups` (walk down to the lowest connected tank, then up), `settleFluids` (drain upper tanks into lower ones), `getActualFluidAmount`/`getActualFluidCapacity` | Same algorithm on the block entity, driven by `neighborChanged` and a first-tick check. Group membership uses `state.is(sameBlock)` and the `connection` property instead of tile booleans. |
| `ITickable.update` for the first-tick relight and regroup | `EntityBlock#getTicker` (NF-SRC `EntityBlock.java`, line 18), server side only, as the prototype wires it. |
| `TileFaucetBase.update`: each tick while active, drain `TRANSFER_AMOUNT_PER_TICK` (10 stone, 20 brick) from the fluid handler behind (`facing` side), fill the handler below (`UP` side), stop when the source is empty, the target is full, or `filled` reaches `TRANSFER_LIMIT` (1000 stone, none for brick) | Same tick in a server `BlockEntityTicker`. Look the two handlers up with `BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, serverLevel, pos, side)` (NF-SRC `net/neoforged/neoforge/capabilities/BlockCapabilityCache.java`, line 31; NF-DOCS capabilities page). `FluidUtil.tryFluidTransfer(dest, source, maxAmount, doTransfer)` (line 305) replaces the manual simulate, drain, fill pair. |
| Faucet 1000 mB display `Tank` refilled every tick with the transferred fluid type, so the renderer knows what to draw | Keep a `FluidStack` field for the displayed fluid. Set it only when the fluid type changes, so the snapshot does not sync every tick (see tile sync). |
| Faucet `Tank.fill` hot-fluid check with `TRANSFERS_HOT_FLUIDS` (stone false, brick true) | Same rule as the tank, on the transferred stack. |
| `Interaction.interact` toggling `active` on any click | `useWithoutItem` and `useItemOn` both call `toggleActive()` server side (interaction). |

Rendering. `TESRTank` draws the fluid column of a tank group from the lowest tank, and
`TESRFaucet` draws a stream from the spout down into the block below.

| 1.12 construct | 1.21 replacement |
|---|---|
| `TileEntitySpecialRenderer<T>`, `renderTileEntityFast`, `Tessellator`, `BufferBuilder.pos/color/tex/lightmap` | `BlockEntityRenderer<T>#render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay)` (NF-SRC `net/minecraft/client/renderer/blockentity/BlockEntityRenderer.java`, line 12). Quads go to `bufferSource.getBuffer(Sheets.translucentCullBlockSheet())` (NF-SRC `net/minecraft/client/renderer/Sheets.java`, line 144) through `VertexConsumer.addVertex(pose, x, y, z).setColor(...).setUv(u, v).setLight(packedLight).setOverlay(...).setNormal(...)` (NF-SRC `com/mojang/blaze3d/vertex/VertexConsumer.java`, lines 155, 53, 20, 63, 67, 26). |
| `TextureMap.getAtlasSprite(fluid.getStill(stack).toString())`, `fluid.getFlowing`, `fluid.getColor` | `IClientFluidTypeExtensions.of(fluid)` (NF-SRC `net/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions.java`, line 42) with `getStillTexture(FluidStack)`, `getFlowingTexture(FluidStack)`, `getTintColor(FluidStack)` (lines 294, 311, 277), resolved through `Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)` (NF-SRC `net/minecraft/client/Minecraft.java`, line 2568; `net/minecraft/client/renderer/texture/TextureAtlas.java`, line 30). |
| `sprite.getInterpolatedU(0..16)` | `TextureAtlasSprite#getU(float)`/`getV(float)` (NF-SRC `net/minecraft/client/renderer/texture/TextureAtlasSprite.java`, lines 68 and 86). These take a 0 to 1 fraction; divide the 1.12 pixel values by 16. |
| `world.getCombinedLight(pos, 0)` | The `packedLight` argument, or `LevelRenderer.getLightColor(level, pos)` (NF-SRC `net/minecraft/client/renderer/LevelRenderer.java`, line 3624) for the block below the faucet. |
| `getRenderBoundingBox` on the tank tile, grown to the group height | `IBlockEntityRendererExtension#getRenderBoundingBox(be)` on the renderer (NF-SRC `net/neoforged/neoforge/client/extensions/IBlockEntityRendererExtension.java`, line 20). |
| `shouldRenderInPass` (tank: pass 1, lowest tank only, non-empty) | Gone. The renderer returns early when `connection` is `down` or `both`, or the group is empty. |
| `FLUID_RENDER_CUTOFF` config map parsed into `BlockMetaMatcher`s, default `tconstruct:casting:0` at 1, everything else 15 | Decision 4. |

Fluids from other modules need `IClientFluidTypeExtensions` registered in
`RegisterClientExtensionsEvent#registerFluidType` (NF-SRC
`net/neoforged/neoforge/client/extensions/common/RegisterClientExtensionsEvent.java`, line 100),
which CORE and HUNTING already plan for their fluids. Vanilla water and lava have them.

## Interaction

Every storage block implements athenaeum's `IBlockInteractable`: `collisionRayTrace` is overridden
to run `interactionRayTrace` (a custom sub-box picker), and `onBlockActivated` calls
`interact(EnumType.MouseClick, ...)`, which walks the tile's `IInteraction[]` in order. The tiles
build those arrays from `InteractionItemStack` (per-slot bounds, allowed faces, a render
`Transform`, `doItemStackValidation`, `onInsert`), `InteractionBase`, and `InteractionBucketBase`.
None of that source is in the repo.

PROTOTYPE pattern 1 replaces all of it. `useItemOn` (NF-SRC `BlockBehaviour.java`, line 226) and
`useWithoutItem` (line 222) receive the `BlockHitResult`, whose `getDirection()` and
`getLocation()` (NF-SRC `net/minecraft/world/phys/BlockHitResult.java`, line 46;
`net/minecraft/world/phys/HitResult.java`, line 21) replace the custom ray tracer. Return
`ItemInteractionResult.sidedSuccess` or `PASS_TO_DEFAULT_BLOCK_INTERACTION` (NF-SRC
`net/minecraft/world/ItemInteractionResult.java`, lines 15 and 7). The block entity owns the verbs,
as `insertFood`/`extractFoodTo`/`addLogFrom`/`removeLogTo` do in the prototype.

| Block | 1.12 interactions, in priority order | 1.21 if-chain |
|---|---|---|
| Stash | One `InteractionItemStack` on `UP`, bounds = the stash box, slot 0 | `useItemOn`: face `UP` and hand not empty, `insert(player, held)`. `useWithoutItem`: face `UP`, `extractTo(player)`. |
| Shelf | Nine `ShelfInteraction`s on the tile-relative `NORTH` face, a 3 by 3 grid of 1/3 by 1/3 boxes, z 12/16 to 1 for `back` and 2/16 to 6/16 for `forward` | Convert `getLocation()` to block-local, rotate by `facing` into tile space, take x = floor(localX * 3), y = floor(localY * 3), slot = y * 3 + x. Insert or extract that slot. |
| Crate | Nine slots on `UP`, 3 by 3 in x and z, y 13/16 to 15/16 | Face `UP`, slot = floor(localZ * 3) * 3 + floor(localX * 3) after the facing rotation. |
| Wood rack | Nine slots on any face, 3 wide by 3 high inside x 3/16 to 13/16 and y 4/16 to 14/16; `isEnabled` always true; validation: logs only, and an empty slot rejects while the slot below is empty or still has room; `onInsert` plays `BLOCK_WOOD_PLACE` at 0.75 with a Gaussian pitch | Project the hit onto the rack's front plane after the facing rotation, slot = row * 3 + column. Keep the gravity rule in `isItemValid`. Sound `SoundEvents.WOOD_PLACE` (NF-SRC `SoundEvents.java`, line 1577). |
| Bag | `InteractionInput` on `UP` (enabled only when open, whitelist, `BLOCK_CLOTH_PLACE` at 0.5), then `InteractionToggleOpen` on any face | `useItemOn`: open, face `UP`, item allowed, `insert`; else `toggleOpen()`. `useWithoutItem`: open, face `UP`, not empty, `extractTo(player)`; else `toggleOpen()`. Sound `SoundEvents.WOOL_PLACE` (line 1582). Toggling writes the `type` property. |
| Tank | `InteractionBucket` on any face, whole block | `useItemOn`: `FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())`. |
| Faucet | One `IInteraction` toggling `active` on any face | Both hooks call `faucet.toggleActive()` when `!level.isClientSide`. A bucket clicked on a faucet toggles it instead of filling from it. That is the 1.12 behaviour; keep it. |

Two caveats. First, athenaeum's `InteractionItemStack` decides how many items a click, a
sneak-click, and an empty-hand click move. That code is not in this repo, so the port must
confirm the counts against a 1.12 client during hoisting. The prototype's verbs move one item per
click. Second, the facing rotation of the local hit vector is the "hit-position helper" the
prototype expected to extract once two or three blocks need it. Storage has three. Write it once
in a small shared class and let tech reuse it.

Sneak plus scroll stays. Athenaeum's `CSPacketInteractionMouseWheel` becomes the prototype's
`InputEvent.MouseScrollingEvent` listener (NF-SRC `net/neoforged/neoforge/client/event/InputEvent.java`,
line 136; `getScrollDeltaY` line 166) and one payload. Rather than one payload per block, storage
should add a single `ScrollInteractionPayload(BlockPos, boolean up)` to core's registrar (CORE,
network section) and dispatch to a `ScrollInteractable` interface on the block entity. The stash,
shelf, crate, wood rack, and bag implement it. Tech will reuse it.

The ghost preview (athenaeum's additive render pass) and the count text (`renderSolidPassText`)
move into each block's `BlockEntityRenderer`, which reads `Minecraft.getInstance().hitResult` as
PROTOTYPE describes. The `TESRInteractable` item rendering becomes the same
`ItemRenderer.renderStatic` loop the prototype's `CampfireRenderer` uses, with the 1.12
`Transform` values (stash: 0.75 scale at y 7/16; shelf and crate: 0.20 scale per cell; wood rack:
logs rotated 90 degrees about x). The bag's `BagInteractionInputRenderer` fill plane (a
`minecraft:blocks/gravel` sprite at y = 1/16 + 7/16 times the fill fraction) becomes one quad in
the bag renderer using `Sheets.solidBlockSheet()` (NF-SRC `Sheets.java`, line 132).

## Tile sync

1.12 registers `ITileData[]` per tile with `ModuleStorage.TILE_DATA_SERVICE`. PROTOTYPE pattern 2
splits that into blockstate properties and the `SyncedBlockEntity` snapshot.

| Tile | 1.12 synced fields | Blockstate property | Snapshot (`saveSynced`) |
|---|---|---|---|
| `TileStash` | `TileDataLargeItemStackHandler(stackHandler)` | none | the one large slot |
| `TileShelf` | `TileDataLargeItemStackHandler` (9 slots) | `type` already a property | 9 slots |
| `TileCrate` | `TileDataLargeItemStackHandler` (9 slots) | none | 9 slots |
| `TileWoodRack` | `TileDataItemStackHandler` (9 slots) | none | 9 slots |
| `TileBagBase` | `TileDataLargeItemStackHandler` (10 slots); open state was already the `TYPE` property | `type` (closed, open) | 10 large slots |
| `TileTankBase` | `TileDataFluidTank`, `TileDataBoolean` up, `TileDataBoolean` down | `connection` (none, up, down, both) | the `FluidStack` |
| `TileFaucetBase` | `TileDataFluidTank` (display tank), `TileDataBoolean active` | none | `active` and the displayed `FluidStack` |

The faucet's `active` flag could also be a `BooleanProperty` (NF-SRC
`net/minecraft/world/level/block/state/properties/BooleanProperty.java`, line 19). It does not
select a model, and adding it means hand-editing the converted `faucet_stone.json` and
`faucet_brick.json` blockstates to carry `active=` variants. The snapshot is the smaller change.

`onTileDataUpdate` on the tank (relight when the fluid changed, regroup when a connection
changed) becomes `onSyncedDataUpdate` for the relight, and the client regroups when it sees the
`connection` property change. The renderer can also just walk the column each frame; groups are
short.

Frequency. A running faucet changes its target tank every tick, so the tank calls `sync()` every
tick while being filled. That is one `ClientboundBlockEntityDataPacket` per tick to players
tracking the chunk, carrying one `FluidStack`. Athenaeum also sent one delta per tick, to the
whole dimension, so this is parity or better. The default of no batching holds for the tank. The
faucet is the exception: its 1.12 tick drained and refilled the display tank with the same fluid
every tick. A naive `onContentsChanged` to `sync()` wiring would sync a no-op every tick. Compare
the fluid type first and sync only on change. If profiling ever shows the tank matters, the
prototype's one-line "dirty flag, sync in tick" batching applies.

## Item forms

`ItemBlockTank` and `ItemBlockBag` are `ItemBlock`s carrying contents in NBT. Both follow HUNTING's
carcass pattern: a data component on the stack, `collectImplicitComponents` and
`applyImplicitComponents` on the block entity (NF-SRC `net/minecraft/world/level/block/entity/BlockEntity.java`,
lines 322 and 293; `BaseContainerBlockEntity.java`, lines 160 to 175 as the vanilla model), and a
loot table with `copy_components` from `block_entity` (DATA `data/minecraft/loot_table/blocks/shulker_box.json`;
NF-SRC `net/minecraft/world/level/storage/loot/functions/CopyComponentsFunction.java`, lines 77
and 120; datagen `BlockLootSubProvider#createShulkerBoxDrop` as the template, NF-SRC
`net/minecraft/data/loot/BlockLootSubProvider.java`, line 270; NF-DOCS
https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/lootfunctions).

| 1.12 construct | 1.21 replacement |
|---|---|
| `ItemBlockTank`: fluid in the stack's root tag (`FluidName`, `Amount`, `Empty`), `initCapabilities` returning a `FluidHandlerItemStack` subclass | A `DataComponentType<SimpleFluidContent>` registered through `DeferredRegister.createDataComponents` and `registerComponentType` (NF-SRC `net/neoforged/neoforge/registries/DeferredRegister.java`, lines 649 and 667; NF-DOCS https://docs.neoforged.net/docs/1.21.1/items/datacomponents). `SimpleFluidContent` is the stock component (NF-SRC `net/neoforged/neoforge/fluids/SimpleFluidContent.java`, lines 21 to 29). The capability is `new FluidHandlerItemStack(componentType, stack, capacity)` (NF-SRC `FluidHandlerItemStack.java`, line 34) registered with `RegisterCapabilitiesEvent#registerItem(Capabilities.FluidHandler.ITEM, provider, items)` (line 136). Same shape as BUCKET. |
| `hasContainerItem` true, `getContainerItem` returning a copy drained by 1000 mB | `IItemExtension#hasCraftingRemainingItem(ItemStack)` and `getCraftingRemainingItem(ItemStack)` (NF-SRC `net/neoforged/neoforge/common/extensions/IItemExtension.java`, lines 207 and 193) draining 1000 with `FluidAction.EXECUTE`. |
| `placeBlockAt` calling `readFromItem`, `getDrops` calling `writeToItem`, `HOLDS_CONTENTS_WHEN_BROKEN` (true for both tanks) | `applyImplicitComponents` reads the fluid component into the tank; `collectImplicitComponents` writes it back. The loot table copies the component. The `Empty`-tag merge bug disappears because stacks with different components never merge. |
| `ItemBlockTank.placeBlockAt`: place the block, `readFromItem`, then `updateConnectionsForPlacement(side)` with the clicked face | The order inside `BlockItem#place` matters. It runs `placeBlock` (NF-SRC `BlockItem.java`, line 70), then `updateBlockEntityComponents`, which calls `applyComponentsFromItemStack` on the new block entity (lines 81 and 123), then `setPlacedBy` (line 82). So the fluid is in the tank before `setPlacedBy` runs, but `setPlacedBy` receives no clicked face (NF-SRC `Block.java`, line 414), and `getStateForPlacement` runs before the block entity exists. Keep a `TankBlockItem` and override `place(BlockPlaceContext)`: call `super.place`, and on success run `updateConnectionsForPlacement(context.getClickedFace())` on the placed block entity. That is what `placeBlockAt` did, and it lets the connection logic see both the face and the placed fluid. |
| Tank tooltip: fluid name, amount, capacity, hot-fluid and retain lines | `appendHoverText` reading the component, `FluidStack#getHoverName()` (NF-SRC `FluidStack.java`, line 423). |
| `ItemBlockBag`: `contents` compound holding a serialized `TileBagBase.StackHandler`, `count` int, metadata 0 closed and 1 open, `setMaxStackSize(1)` | Two components: `open` (`DataComponentType<Boolean>`) and `BagContents`, a custom record with a codec. `ItemContainerContents` cannot be used because its slot codec is `ItemStack.CODEC` (NF-SRC `net/minecraft/world/item/component/ItemContainerContents.java`, line 182), capped at 99 per slot, and a bag holds up to 2560 items in 10 slots. `Item.Properties.stacksTo(1)` (NF-SRC `Item.java`, line 452). Give both components defaults with `Item.Properties.component` (line 490). |
| `getDurabilityForDisplay`, `showDurabilityBar`, `getRGBDurabilityForDisplay` (brown, red when full) | `isBarVisible`, `getBarWidth`, `getBarColor` (NF-SRC `Item.java`, lines 188 to 196). |
| `onItemUseFirst` while sneaking: pour the bag into the clicked block's item handler, else spill into the air block in front when open | `IItemExtension#onItemUseFirst(stack, UseOnContext)` still exists (line 111). Target handler through `level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face)`. Spill with `Containers.dropItemStack`. |
| `onItemRightClick` while sneaking with no block hit: toggle open | `Item#use` (line 165), flipping the `open` component. |
| `onItemUse` placing then copying the item handler into the tile | `BlockItem#place` plus `applyImplicitComponents`. Nothing to copy by hand. |
| `addInformation` with the shift-expanded per-item list | `appendHoverText` with `Screen.hasShiftDown()` (NF-SRC `net/minecraft/client/gui/screens/Screen.java`, line 433). |
| Two item models by metadata (`registerBlockItemModelForMeta(..., OPEN, 1)`) | One item model with an `overrides` block on a `pyrotech:open` predicate registered through `ItemProperties.register` (NF-SRC `net/minecraft/client/renderer/item/ItemProperties.java`, line 60) inside `FMLClientSetupEvent.enqueueWork`, as HUNTING does for the spear. See decision 5. |

## Events

| 1.12 handler | 1.21 replacement |
|---|---|
| `EntityItemPickupEventHandler` on `EntityItemPickupEvent`: locate open bags in main hand, off hand, hotbar, and main inventory (four config flags); if any bag accepts part of the stack, first top up existing inventory stacks through `PlayerMainInvWrapper`, then insert into the bags, playing `ENTITY_ITEM_PICKUP` at 0.4, and shrink the entity's stack | `ItemEntityPickupEvent.Pre` (NF-SRC `net/neoforged/neoforge/event/entity/player/ItemEntityPickupEvent.java`, line 56), server only (lines 48 to 55). Run the same steps on `event.getItemEntity().getItem()` (NF-SRC `net/minecraft/world/entity/item/ItemEntity.java`, line 431). If the bags take everything, `setCanPickup(TriState.FALSE)` (line 70; `net/neoforged/neoforge/common/util/TriState.java`) and discard the entity. Otherwise leave `DEFAULT` and let vanilla pick up the rest. `PlayerMainInvWrapper` still exists (NF-SRC `net/neoforged/neoforge/items/wrapper/PlayerMainInvWrapper.java`, line 19). Hands are `getMainHandItem`/`getOffhandItem` (NF-SRC `net/minecraft/world/entity/LivingEntity.java`, lines 2022 and 2026); the hotbar is `Inventory.items` indices 0 to 8 (NF-SRC `net/minecraft/world/entity/player/Inventory.java`, lines 32 and 48). The 1.12 inventory scan started at index 10 and skipped slot 9; the port scans 9 to 35. Sound `SoundEvents.ITEM_PICKUP` (line 767). |
| `EntityItemPickupEvent` fired from Forge's `EntityItem.onCollideWithPlayer` after the `delayBeforeCanPickup` check | `Pre` fires before the delay check: `fireItemPickupPre` runs at `ItemEntity.playerTouch` line 383, and the `pickupDelay == 0` test only runs at line 391 (NF-SRC `ItemEntity.java`). A handler that leaves `canPickup` at `DEFAULT` and moves items into bags itself must test `hasPickUpDelay()` (line 488) first, or open bags vacuum up items the player just dropped. |
| `ConfigChangedEventHandler` re-parsing the faucet cutoff map | Deleted with the map (decision 4). If the map survives, `ModConfigEvent.Reloading` per CORE. |

CORE's own `ItemEntityPickupEvent.Post` listener (the root advancement) is unaffected: `Post` only
fires when part of the item was picked up, so a fully bagged item never triggers it.

## Recipes

Fifteen JSON files under `assets/pyrotech/recipes/storage/`. Thirteen are `minecraft:crafting_shaped`
and two are `pyrotech:tank_flush`. All carry the `modules_enabled` condition, which drops. Material
metas resolve through `ItemMaterial.EnumType` in core: 5 `refractory_brick`, 14 `twine`, 16
`brick_stone`, 17 `clay_lump`, 23 `board_tarred`, 35 `refractory_clay_lump`, 42
`leather_durable_sheet`, 43 `leather_durable_strap`, 44 `leather_durable_cord`.

| Recipe | Pattern and inputs | 1.21 ingredients |
|---|---|---|
| `stash` | `S S` / `SSS`, S `slabWood` | `ItemTags.WOODEN_SLABS` (NF-SRC `ItemTags.java`, line 17) |
| `shelf` | `P-P` three rows, P `plankWood`, `-` `slabWood` | `ItemTags.PLANKS` (line 9), `WOODEN_SLABS` |
| `crate` | `PPP` / `PSP` / `PPP` | `PLANKS`, `WOODEN_SLABS` |
| `stash_durable`, `shelf_durable`, `crate_durable` | `SBS` / `BXB` / `SBS`, S brick_stone, B board_tarred, X the wood block | `pyrotech:brick_stone`, `pyrotech:board_tarred`, the wood block |
| `wood_rack` | `OSO` / `LSL` / `OSO`, O `logWood`, S `slabWood`, L ladder | `ItemTags.LOGS` (line 25), `WOODEN_SLABS`, `minecraft:ladder` |
| `bag_simple` | `sWs` / `WSW` / `sWs`, s twine, W `blockWool`, S stash | `pyrotech:twine`, `ItemTags.WOOL` (line 8), `pyrotech:stash` |
| `bag_durable` | `sCs` / `LSL` / `sLs`, C cord, L sheet, s strap, S stash_stone | hunting's `pyrotech:leather_durable_cord`, `_sheet`, `_strap`, and `pyrotech:stash_stone` |
| `stone_tank` | `BGB` / `G G` / `BGB`, B brick_stone, G `blockGlass` | `Tags.Items.GLASS_BLOCKS`, `c:glass_blocks` (NF-SRC `net/neoforged/neoforge/common/Tags.java`, line 572) |
| `brick_tank` | same, B refractory_brick, G refractory_glass | `pyrotech:refractory_brick`, `pyrotech:refractory_glass` |
| `stone_faucet` | `S S` / `CSC`, S brick_stone, C clay_lump | flat core items |
| `brick_faucet` | same, S refractory_brick, C refractory_clay_lump | flat core items |
| `stone_tank_empty`, `brick_tank_empty` | `pyrotech:tank_flush` with a `tank` item | custom, below |

The thirteen shaped recipes are plain datagen JSON per RECIPES, emitted by a `StorageRecipes`
static class from the shared `RecipeProvider` using vanilla `ShapedRecipeBuilder` (NF-SRC
`net/minecraft/data/recipes/ShapedRecipeBuilder.java`, lines 50, 96, 126). The bag recipe depends
on hunting's durable leather items, so its datagen method waits for hunting or references the
flat ids by string.

`TankFlushRecipe` (1.12 core, rehomed here per CORE) is a `ShapelessRecipes` whose single
ingredient is the tank item, whose output is a fresh tank, and whose `getRemainingItems` returns
all empty. That last override is the point: a plain shapeless recipe would put the drained copy
from `getCraftingRemainingItem` back into the grid and duplicate the tank. So it stays custom: a
`Recipe<CraftingInput>` with one `Ingredient` field and RECIPES' two-codec serializer, matching
exactly one tank item anywhere in the grid, assembling a component-free tank, and returning empty
remainders (`Recipe#getRemainingItems`, cited in HUNTING). `CustomRecipe` with
`SimpleCraftingRecipeSerializer` (NF-SRC `net/minecraft/world/item/crafting/CustomRecipe.java`,
`SimpleCraftingRecipeSerializer.java`) also works if the tank set is hard-coded. One recipe with an
item tag `#pyrotech:tanks` replaces the two 1.12 files.

JEI. Storage had no JEI plugin in 1.12 and exposes nothing new. The shaped recipes and the flush
recipe show up in the crafting category on their own.

## Reusable bases for tech and ignition

What the code shows. No tech or ignition tile extends `TileTankBase` or `TileStash` in 1.12.
`TileLampOil` (ignition) names `TileTankBase` only as the type argument of its own
`InteractionBucket extends InteractionBucketBase<TileTankBase>` (line 199) and owns a separate
tank. `TileSoakingPot` and `TileTarTankBase` extend athenaeum's `TileEntityDataBase` and import
`ModuleStorage` only for `ModuleStorage.PACKET_SERVICE.sendToAllAround(new SCPacketParticleCombust(...))`
(`TileSoakingPot.java` line 609, `TileTarTankBase.java` line 196). `TileMechanicalMulchSpreader`
names `TileStash` only as a type argument (line 257). DEPS and the ticket text overstate these
edges. What the four tiles really share with storage is behaviour copied by hand: a `Tank.fill`
override with a validation step (a temperature check in the two tech tiles and both storage
tanks, a config fuel filter in `TileLampOil`, line 306), the "Empty" tank serialization hack, and
athenaeum's large stack handlers (`MulchStackHandler extends LargeObservableStackHandler`).

What storage should therefore expose, in a shared package rather than under the storage
namespace, since storage is hoisted before tech:

1. `LargeStackHandler extends ItemStackHandler`: the count-above-99 serialization and the
   per-slot multi-stack limits from the inventories section. Storage needs it in four tiles.
   Tech's machines will need it in many.
2. `HotFluidTank extends FluidTank`: a temperature threshold, a "tolerates hot fluids" flag, and
   an `onHotFluid(FluidStack)` callback that the owner uses to break the block, spill, play
   `ITEM_BREAK`, and send particles. Storage tanks and faucets and tech's soaking pot and tar
   tanks run this rule. Ignition's oil lamp needs only the validation hook, which
   `FluidTank#setValidator` (NF-SRC `FluidTank.java`, line 39) already provides.
3. `TankBlockEntity extends SyncedBlockEntity`: the 1.12 `TileTankBase` shape. One `HotFluidTank`,
   the vertical group logic, the item round trip through `SimpleFluidContent`, and the
   `getLightEmission` hook. Only storage's two tanks need the grouping, so keep the group code
   in this class and the tank in `HotFluidTank`, which is what the other modules reach for.
4. A core helper for the combust particles (`sendParticles` of `SMOKE`, `LARGE_SMOKE`, `FLAME`
   in the 1.12 packet's proportions) replacing `SCPacketParticleCombust`, which CORE dropped.
5. The hit-to-slot facing rotation helper and the `ScrollInteractable` payload from the
   interaction section.

The stash class itself needs no reuse hook. The mulch spreader's handler is a large handler with
a filter, which item 1 covers.

## Network packets

Storage declares no packets of its own. `ModuleStorage.PACKET_SERVICE` is used for core's
`SCPacketParticleCombust` (dropped for `ServerLevel#sendParticles`), and athenaeum's tile data
service and mouse wheel packet are replaced by the snapshot and the shared scroll payload.
Storage contributes zero payload types beyond the `ScrollInteractionPayload` it proposes core
owns.

## Config

`ModuleStorageConfig` is 539 lines. Its knobs and 1.12 defaults:

- `ALLOW_AUTOMATION` per block (all true). Drop; register the capability unconditionally.
- `MAX_STACKS`: stash 10, durable stash 20, shelf 1, durable shelf 2, crate 1, durable crate 2.
  Bake as constructor arguments.
- Bags: `MAX_ITEM_CAPACITY` 640 and 2560; four auto-pickup flags (main hand, off hand, hotbar
  true; inventory false); `ITEM_WHITELIST` and `ITEM_BLACKLIST` strings; `ROCK_FILL_TEXTURE_LOCATION`
  `minecraft:blocks/gravel`. Capacities and texture bake. Whitelist is decision 2. Flags are
  decision 3. Two details feed decision 2. `ITEM_BLACKLIST` is dead by default:
  `BlockBagBase.isItemValidForInsertion` consults it only when the whitelist is empty
  (`BlockBagBase.java`, lines 97 to 115), and both whitelists ship non-empty. And
  `DURABLE_ROCK_BAG.ITEM_WHITELIST` is built from the simple list at class-init time with
  `ArrayHelper.combine` (`ModuleStorageConfig.java`, line 192), so once the config file exists,
  editing the simple list no longer changes the durable one. Neither behaviour is worth carrying
  into the tags.
- Tanks: `CAPACITY` 4000 and 8000; `HOT_TEMPERATURE` 450; `HOLDS_HOT_FLUIDS` stone false, brick
  true; `HOLDS_CONTENTS_WHEN_BROKEN` true for both. Bake.
- Faucets: `HOT_TEMPERATURE` 450; `TRANSFERS_HOT_FLUIDS` stone false, brick true; `TRANSFER_LIMIT`
  1000 and unlimited; `TRANSFER_AMOUNT_PER_TICK` 10 and 20; `FLUID_RENDER_CUTOFF` map. Bake the
  numbers. The map is decision 4.
- Thirteen `Stages` fields. Dropped with gamestages.

Whatever survives goes into the one shared `ModConfigSpec` from CORE (`ModConfigSpec.Builder`,
NF-SRC `net/neoforged/neoforge/common/ModConfigSpec.java`, line 300).

## Dropped outright

- `plugin/` (13 CraftTweaker `Zen*` classes, 5 TOP, 9 Waila): 27 files, out of scope per CORE.
  The `ZenDoc*` annotations go with them.
- The gamestages `Stages` hooks on all thirteen tiles.
- `ModuleBase`, `Registry`, `enableAutoRegistry`, `IPacketService`, `ITileDataService`, the
  `modules_enabled` recipe conditions, and the module toggle.
- The phantom empty slot in the stash, shelf, and crate handlers (1.12 issue 380).
- The "Empty" NBT tag workaround in tank and faucet serialization.
- `collisionRayTrace` overrides and `interactionRayTrace`; `getStateFromMeta`, `getMetaFromState`,
  `damageDropped`, `getActualState`; `shouldRenderInPass`; `TileStash.shouldRefresh`.
- `ConfigChangedEventHandler`, `TESRFaucet.updateBlockMatchersFromConfig`, and the
  `BlockMetaMatcher` parsing of block strings.
- `removedByPlayer` and `harvestBlock` tricks that kept the tile alive for `getDrops`.

## Decisions for Moos

1. **Large stacks.** `ItemStack.CODEC` caps saved counts at 99, and `ItemContainerContents` uses
   that codec per slot. Options: a `LargeStackHandler` with its own item-plus-int-count
   serialization and a matching custom bag component (faithful: one slot still holds 640
   cobblestone, hoppers and the renderer see one stack); or split every logical slot into as
   many 99-count sub-slots as needed (stock handlers and `minecraft:container`, but slot counts,
   hopper behaviour, and the count text all change). Recommendation: the custom handler and
   component. It is one small class plus one record, and it is the same problem tech will have.
2. **Bag whitelist.** 1.12 stores item strings with metadata wildcards in config. Options: two
   item tags `#pyrotech:rock_bag_items` and `#pyrotech:durable_rock_bag_items` generated with
   the 1.12 defaults (the flattened rock variants, rock grass, rock netherrack; plus dirt,
   cobblestone, gravel, sandstone, red sandstone, and core's cobblestone variants for the
   durable bag), `ModConfigSpec` string lists, or a data map. Recommendation: item tags. They
   are datapack-tunable, match HUNTING's knife tags, and the blacklist (empty by default) is
   not needed when the whitelist is a tag.
3. **Storage config surface.** Following CORE decision 3, bake every number. The only knobs with
   a gameplay feel are the four bag auto-pickup location flags. Options: bake them too (1.12
   defaults: hands and hotbar yes, main inventory no), or keep the four booleans in the shared
   common config. Recommendation: bake them. Nobody tunes these; the defaults are the design.
4. **Faucet render cutoff.** 1.12 has a config map from block strings to how far the stream
   renders into the block below, defaulting to 15 pixels with one entry for `tconstruct:casting`.
   Options: drop the map and render 15 pixels always; or a block data map
   (`DataMapType` on `Registries.BLOCK`, as HUNTING proposes for its maps) for per-block depth.
   Recommendation: drop it. The one default entry targets a mod that is not in this pack, and
   a data map can be added later without touching gameplay.
5. **Bag item open state.** 1.12 uses item metadata 0 and 1 with two item models. Options: one
   item with an `open` component and an `ItemProperties` predicate driving a model override
   (faithful: one registry id, one recipe result, contents survive toggling); or two items with
   a swap on toggle (simpler models, but the contents component must be copied and recipes and
   tags must name both). Recommendation: one item with the component and the predicate.
6. **Hot fluid on simulated fills.** The 1.12 tank and faucet `fill` overrides run the hot-fluid
   check before looking at `doFill` (`TileTankBase.java` and `TileFaucetBase.java`, inner class
   `Tank`), so a simulated lava fill destroys a stone tank or faucet exactly like a real one. In
   normal play the faucet simulates and executes in the same tick, and the group handler's
   same-fluid guard hides the case where the tank already holds water, so the visible outcome is
   the same either way. Options: keep the 1.12 rule and break on `FluidAction.SIMULATE` too
   (faithful to the letter; any probe or pipe that tests a tank destroys it), or break only on
   `EXECUTE`. Recommendation: break only on `EXECUTE`. No 1.12 mechanic depends on the simulate
   case, and the shared `HotFluidTank` helper stays honest for tech and ignition.
7. **Faucet `active`: blockstate property or synced boolean.** PROTOTYPE's rule is blockstate
   first, and the tile sync section already leans the other way for the faucet. Options: a
   `BooleanProperty` `active` on both faucet blocks (follows the rule and the game syncs it, but
   every start and stop rebuilds the chunk section for a model that does not change, and the
   converted `faucet_stone.json` and `faucet_brick.json` must be hand-edited to carry `active=`
   variants), or a boolean in the `SyncedBlockEntity` snapshot beside the displayed fluid (one
   snapshot per change, no rebuild, no blockstate edits; the renderer reads it the same way it
   reads the fluid). Recommendation: the synced boolean, as the exception the PROTOTYPE rule
   allows for state that selects no model. The tile sync table above assumes it.
8. **Shared fluid package now or later.** PROTOTYPE said to extract `library/` packages only
   from real duplication. For fluids the duplication is already on the 1.12 branch. The
   `Tank.fill` override with a temperature check is copied in six tiles (both storage tanks and
   faucets, tech/basic's barrel and soaking pot, tech/machine's combustion worker fluid-out base,
   tech/refractory's tar tanks), the bucket interaction in six (`TileTankBase`, `TileLampOil`,
   `TileBarrel`, `TileCompostBin`, `TileSoakingPot`, `TileCrucibleBase`), and seven renderers
   draw a fluid box (storage's two, ignition's lamp, tech's barrel, soaking pot, crucible, and
   tar collector). Options: create `library/fluid` during the storage hoist with `HotFluidTank`,
   the `FluidUtil` bucket helper, and the box renderer from the reusable bases section, or keep
   those classes under the storage package and let tickets 17 and 18 pull them out when
   refractory and ignition need them. Recommendation: create it now. Both later notes are
   blocked on this document and should be able to name the classes, and moving code out of
   storage later means touching a hoisted module twice.
