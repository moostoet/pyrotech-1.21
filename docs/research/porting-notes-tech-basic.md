# Porting notes: tech/basic

Resolves issue #16 (porting notes: tech/basic).
Tech/basic is the seventh hoisting unit from issue #4 (module porting order): 177 files, 29,068 lines
(DEPS). Of those, 79 files are plugin code (14 CraftTweaker, 1 gamestages, 29 JEI, 12 TOP, 23 Waila)
and 98 files, 20,371 lines, are the mechanics. The unit holds fifteen blocks (three anvils, two
barrels, campfire, chopping block, compacting bin, compost bin, drying rack, pit kiln, soaking pot,
tanning rack, two worktables), fifteen tiles, seven items, five potions, eight event handlers, one
player capability, three packets, five renderers, twelve recipe classes, thirteen recipe adders, and a
1,680-line config. This document lists each 1.12 construct and its 1.21 replacement. Decisions that
need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo, package `com.codetaylor.mc.pyrotech.modules.tech.basic`,
  plus the library classes it extends (`library/spi/tile/TileBurnableBase.java`,
  `TileCombustionWorkerBase.java`, `TileEntityDataWorkerBase.java`, `library/spi/interaction/InteractionExtinguishable.java`,
  `library/InteractionUseItemToActivateWorker.java`, `library/CompostBinRecipeBase.java`,
  `library/CompactingBinRecipeBase.java`, `library/util/Util.java`), its users outside the package
  (`modules/core/event/FurnaceFuelBurnTimeEventHandler.java`, `modules/core/recipe/MarshmallowStickRecipe.java`,
  `modules/core/recipe/ChoppingBlockRecipe.java`, `modules/hunting/tile/InteractionCarcass.java`, and the
  tech/bloomery and tech/machine files listed under findings), the recipe JSON under
  `assets/pyrotech/recipes/tech/basic/` (19 files), and `Reference.java`.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths cited below are entries in that jar.
- **NF-DOCS**: docs.neoforged.net, 1.21.1 version pages. URLs cited inline.
- **DATA**: the vanilla 1.21.1 data pack and client assets, at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`.
- **CORE**: `docs/research/porting-notes-core.md` on branch `research/porting-notes-core`, as amended by
  issue #22.
- **RECIPES**: `docs/research/recipe-architecture.md` on branch `research/recipe-architecture`, as
  amended by issue #21.
- **STORAGE**: `docs/research/porting-notes-storage.md` on branch `research/porting-notes-storage`.
- **REFRACTORY**: `docs/research/porting-notes-refractory.md` on branch `research/porting-notes-refractory`,
  as amended by issue #29.
- **IGNITION**: `docs/research/porting-notes-ignition.md` on branch `research/porting-notes-ignition`, as
  amended by issue #34.
- **HUNTING**: `docs/research/porting-notes-hunting.md` on branch `research/porting-notes-hunting`.
- **TOOL**: `docs/research/porting-notes-tool.md` on branch `research/porting-notes-tool`.
- **BUCKET**: `docs/research/porting-notes-bucket.md` on branch `research/porting-notes-bucket`.
- **SKIPS**: `docs/research/post-1-12-progression-skips.md` on branch `research/post-1-12-progression-skips`,
  as amended by issue #33.
- **DEPS**: `docs/research/module-dependencies.md` on branch `research/module-dependencies`.
- **PROTOTYPE**: branch `prototype/8-campfire-interaction-sync`, `docs/prototypes/campfire-interaction-sync.md`
  and the Java under `src/main/java/com/moostoet/pyrotech/prototype/campfire/`.
- **ASSETS**: `docs/asset-migration-report.md` and `docs/asset-conversion-report.md` on `main`, and the
  converted files under `src/main/resources/assets/pyrotech/`.
- **SIGN-OFFS**: the resolution comments of issues #21, #22, #29, #31, #33, and #34, and the four
  follow-on comments on issue #16.

This document assumes the recommended answers from CORE, RECIPES, STORAGE, REFRACTORY, IGNITION,
HUNTING, TOOL, and BUCKET as the sign-offs amended them. It applies PROTOTYPE pattern 1 (per-block
if-chain dispatch from `useItemOn` and `useWithoutItem`, a shared helper package grown only from real
duplication, sneak plus scroll stacking and the ghost preview kept) and pattern 2 (blockstate
properties where possible, else a full snapshot per change through `SyncedBlockEntity`).

## Scope correction

The ticket text names "the campfire, pit kiln, chopping block, drying rack, worktables, and soaking
pot". The package also holds the three anvils, the barrel, the compacting bin, the compost bin, and the
tanning rack, and the follow-on comments need three corrections.

- The unit has five effects, not six. `PotionInitializer` registers comfort, well fed, resting, well
  rested, and focused (`init/PotionInitializer.java`, lines 10 to 14). `PotionCampfireBase` is the sixth
  file and is only the base class (`potion/PotionCampfireBase.java`, lines 16 to 59).
- The refractory sign-off counts ten tar soaking pot recipes. The code has twelve. `SoakingPotRecipesAdd`
  references `ModuleTechRefractory.Fluids.WOOD_TAR` or `COAL_TAR` at lines 69, 78, 87, 96, 136, 165, 185,
  196, 207, 218, 247, and 258, one per recipe: `durable_leather`, `durable_leather_sheet`,
  `durable_leather_strap`, `durable_leather_cord`, `coal_block`, `living_tar`,
  `tarred_kindling_from_wood_tar`, `wood_tar_block_from_straw`, `twine_durable`, `wool_tarred`,
  `planks_tarred`, and `board_tarred` (`init/recipe/SoakingPotRecipesAdd.java`, lines 64 to 100 and 132 to
  262). All twelve move to refractory's datagen under the last-hoisted-unit rule. Tech/basic keeps
  seventeen.
- The pit kiln has no door logic to share. `TileKilnPit.isRefractoryBlock` only walks the refractory
  block list (`tile/TileKilnPit.java`, lines 441 to 454). The door and double-slab cases live in the
  active pile's `isValidStructureBlock` (REFRACTORY, active pile table). What the pit kiln does with
  refractory blocks: a refractory block passes the enclosure test on any of the five faces regardless
  of solidity and flammability (lines 366 to 373), and each refractory face among the five reduces the
  recipe's failure chance by a fifth, so five refractory faces mean no failure (lines 390 and 421 to 439).
  It is a validity bypass plus a failure bonus, never a requirement.

Three smaller facts the ticket text does not say:

- The worktable recipe registry ships empty. There is no `WorktableRecipesAdd`; only CraftTweaker's
  `ZenWorktable` ever put a `WorktableRecipe` in it (`ModuleTechBasic.java`, lines 155 to 167 list every
  adder; none is a worktable adder). Every worktable craft in 1.12 is a wrapped vanilla crafting recipe
  (`recipe/WorktableRecipe.java`, lines 40 to 58).
- The campfire recipe registry ships one entry, the Patchouli book, guarded by the Patchouli plugin
  module (`init/recipe/CampfireRecipesAdd.java`, lines 18 to 24). Every other campfire cook is derived
  from a furnace recipe with a food result (`recipe/CampfireRecipe.java`, lines 57 to 87).
- Two library classes reach into this module: `library/CompostBinRecipeBase.java` reads
  `ModuleTechBasicConfig.COMPOST_BIN` (lines 57 to 58) and `library/spi/interaction/InteractionExtinguishable.java`
  imports `BlockCampfire` for its bounds (line 8, used at line 61). Both edges die: the compost value
  formula moves into tech/basic's recipe class, and the douse helper takes its bounds from the caller.
- DEPS counts six tech/basic files that reference tech/machine and three that reference tech/bloomery.
  Five of the machine files and two of the bloomery files are plugin code (the CraftTweaker `Zen*`
  classes, `plugin/top/provider/CompactingBinProvider.java`, `plugin/waila/delegate/AnvilProviderDelegate.java`,
  `plugin/jei/wrapper/JEIRecipeWrapperAnvil.java`). The one real bloomery edge is `block/spi/BlockAnvilBase.java`
  (lines 8 to 9, 83 to 94, 108 to 113), covered in the anvil section. There is no real machine edge.

## Summary

- The module bootstrap collapses into `DeferredRegister` holders for fifteen blocks, twenty-two items
  (fifteen block items and seven items), eleven block entity types, five mob effects, eleven recipe
  types with their serializers, one payload, and one client renderer registration, following CORE.
- Every block keeps its interaction set as an if-chain in `useItemOn` and `useWithoutItem`. What 1.12
  synced as tile data splits into blockstate properties (campfire `variant` and `ash`, kiln `variant`,
  chopping block `damage` and `sawdust`, anvil `damage`, compost bin `state` and `compost_value`, soaking
  pot `campfire`, barrel `sealed`, drying rack `stacked`) and `SyncedBlockEntity` snapshots holding
  only what a renderer reads. Recipe progress floats leave the wire entirely: nothing on the client
  reads them once Waila and TOP are gone.
- The campfire derives its cook list from `RecipeType.SMELTING` results that carry `DataComponents.FOOD`,
  never from `campfire_cooking`, with a blacklist tag for bread and cookie, and keeps an explicit
  `pyrotech:campfire` recipe type only for the Patchouli book and datapack additions.
- The five campfire effects become `MobEffect` registrations with infinite-duration instances, which
  makes `CampfireEffectDurationFix` unnecessary, and the focused player capability plus its sync packet
  become one synced data attachment that also carries the resting tick counter the infinite duration
  no longer provides.
- The marshmallow stick's reflection hack and timestamp packet disappear: in 21.1.249 a component
  change on the held stack no longer interrupts item use, and `getUseDuration` receives the entity, so
  client and server agree on the roasting duration without a packet.
- The thirteen recipe adders become datagen: 209 recipes emitted by `TechBasicRecipes` plus 105
  inherited copies inside the unit, twelve tar soaking pot recipes handed to refractory, one campfire
  recipe handed to the book ticket, and four parent chains for machine and bloomery to extend.
- The two custom crafting factories in core whose results are tech/basic items (`marshmallow_stick`,
  `chopping_block`) rehome here and reuse hunting's tool-damaging shapeless serializer.
- The 1,680-line config shrinks to eighteen behaviour toggles plus two counts and one table in
  `COMMON`, thirty-nine multipliers in `SERVER`, one client flag, and everything else baked or moved to
  recipes, tags, and data maps.

## Module bootstrap

`ModuleTechBasic` follows the CORE replacements: one `register(IEventBus)` hook, `DeferredRegister`
fields as holders, no `ModuleBase`, no `Registry`, no `Injector`, no `@GameRegistry.ObjectHolder`.

| 1.12 construct | 1.21 replacement |
|---|---|
| `setRegistry`, `enableAutoRegistry`, `PACKET_SERVICE`, `TILE_DATA_SERVICE` (`ModuleTechBasic.java`, lines 50 to 54) | Dropped with athenaeum. Sync is the PROTOTYPE `SyncedBlockEntity`; payloads go through core's registrar (network section). |
| Fourteen CraftTweaker plugins, the JEI plugin, the gamestages plugin, the TOP and Waila IMC messages (lines 58 to 90, 104 to 108, 130 to 134) | Dropped. JEI returns in the shared plugin (assets and JEI section). |
| `RegistryEvent.NewRegistry` creating twelve Forge recipe registries and injecting them into `Registries` (`init/RegistryInitializer.java`, lines 12 to 85 and 89 to 150) | `RecipeType` and `RecipeSerializer` entries in RECIPES' `PyrotechRecipeTypes` and `PyrotechRecipeSerializers` holders (NF-SRC `net/minecraft/world/item/crafting/RecipeType.java`, line 25; NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/server/recipes/). Eleven types survive; the worktable registry has no type (recipes section). |
| `onRegisterRecipesEvent`: the campfire blacklist, thirteen adders, three inheritance calls (lines 148 to 172) | Datagen (recipes section). The blacklist is an item tag. |
| `RecipeRepeat` handler registered only when `ALLOW_RECIPE_REPEAT` (lines 110 to 112); `TooltipEventHandler.CompostValue` only when `SHOW_COMPOST_VALUE_IN_TOOLTIPS` (lines 136 to 138); six campfire handlers (lines 116 to 121) | `@EventBusSubscriber(modid = ...)` static listeners that read the toggle inside the handler (NF-DOCS https://docs.neoforged.net/docs/1.21.1/concepts/events). See the effects section for the survivors. |
| `CapabilityManager.INSTANCE.register(IFocusedPlayerData.class, ...)` (line 114) | An `AttachmentType` (focused player data section). |
| `BlockInitializer.onRegister`: fourteen blocks with items, the sealed barrel with a stack-size-1 item, fifteen tile classes (`init/BlockInitializer.java`, lines 21 to 69) | `DeferredRegister.Blocks#registerBlock` (NF-SRC `net/neoforged/neoforge/registries/DeferredRegister.java`, line 431) for the blocks and `Items#registerSimpleBlockItem` (line 546) or `registerItem` (line 592) for the block items. `BlockEntityType.Builder.of(factory, blocks...).build(null)` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntityType.java`, line 341) for eleven types: anvil (over three blocks), barrel, campfire, chopping block, compacting bin, compost bin, drying rack (over both rack blocks), pit kiln, soaking pot, tanning rack, worktable (over both worktables). The per-tier subclasses (`TileAnvilGranite` and siblings, `TileWorktableStone`, `TileDryingRackCrude`) only override config getters, so one block entity class per type reads its numbers from the block, as STORAGE does for its stone variants. |
| `onClientRegister`: item model registration per variant and twelve `TESRInteractable` bindings (lines 77 to 144) | Item models load by id. `EntityRenderersEvent.RegisterRenderers#registerBlockEntityRenderer` (NF-SRC `net/neoforged/neoforge/client/event/EntityRenderersEvent.java`, line 109; NF-DOCS https://docs.neoforged.net/docs/1.21.1/blockentities/ber) for every type whose block renders stored items or fluid, which is all eleven. |
| `ItemInitializer.onRegister`: seven items, the stick with the no-creative-tab flag (`init/ItemInitializer.java`, lines 16 to 22) | `DeferredRegister.Items#registerItem` (line 592). The stick stays out of `BuildCreativeModeTabContentsEvent` (NF-SRC `net/neoforged/neoforge/event/BuildCreativeModeTabContentsEvent.java`, line 52). |
| `PotionInitializer`: five potions (`init/PotionInitializer.java`, lines 10 to 14) | `DeferredRegister.create(Registries.MOB_EFFECT, ...)` (effects section). |
| `PacketInitializer`: three client-bound packets (`init/PacketInitializer.java`, lines 12 to 29) | One payload record and two dissolutions (network section). |
| `ModuleTechBasic.CREATIVE_TAB` | The block items and the marshmallow, roasted and burned marshmallows, empty stick, tinder, and barrel lid join core's tab in `BuildCreativeModeTabContentsEvent`. The sealed barrel item (`block/BlockBarrel.java`, lines 188 to 191) and the full stick (`item/ItemMarshmallowStick.java`, lines 173 to 176) stay out. |
| `Potions`, `Blocks`, `Items`, `Registries` holder classes with injected nulls (lines 196 to 354) | The `DeferredHolder`, `DeferredBlock`, and `DeferredItem` fields (CORE). |

## Shared shapes across the unit

Ten blocks repeat the same five idioms. They are stated once here and referenced below.

- **Interaction dispatch.** Every block overrides `collisionRayTrace` and `onBlockActivated` to run
  athenaeum's `interact` (for example `block/spi/BlockAnvilBase.java`, lines 121 to 130). The
  replacement is PROTOTYPE pattern 1: `useItemOn` (NF-SRC `net/minecraft/world/level/block/state/BlockBehaviour.java`,
  line 226) and `useWithoutItem` (line 222), returning `ItemInteractionResult.sidedSuccess`,
  `PASS_TO_DEFAULT_BLOCK_INTERACTION`, or `SKIP_DEFAULT_BLOCK_INTERACTION` (NF-SRC
  `net/minecraft/world/ItemInteractionResult.java`, lines 15, 7, 8). The 1.12 sub-box bounds become a
  face test on `BlockHitResult#getDirection` plus a local-position test on `getLocation`, rotated by
  the block's facing with the hit-to-slot helper STORAGE proposed. The 1.12 priority order is the
  if-chain order.
- **Hunger gate.** The chopping block, compacting bin, anvils, and worktables refuse work below
  `MINIMUM_HUNGER_TO_USE` (3) and send `SCPacketNoHunger` (`tile/TileChoppingBlock.java`, lines 358 to
  364, and the same shape in the other three). In 1.21 the test is `player.getFoodData().getFoodLevel()`
  (NF-SRC `net/minecraft/world/food/FoodData.java`, line 103) and the packet is core's payload sent
  through core's helper (CORE, network section). Exhaustion is `player.causeFoodExhaustion(float)`
  (NF-SRC `net/minecraft/world/entity/player/Player.java`, line 1793).
- **Tool tests and levels.** `getToolClasses(stack).contains("axe")` plus whitelist and blacklist
  arrays (`tile/TileChoppingBlock.java`, lines 377 to 382) becomes `stack.canPerformAction(ItemAbilities.AXE_DIG)`
  (NF-SRC `net/neoforged/neoforge/common/extensions/IItemStackExtension.java`, line 122;
  `net/neoforged/neoforge/common/ItemAbilities.java`, line 23), `SHOVEL_DIG` (line 33) for shovels, and
  `PICKAXE_DIG` (line 28) for pickaxes. `getHarvestLevel(stack, "axe", ...)` (line 471) becomes core's
  `pyrotech:tool_levels` helper (SIGN-OFFS, issue #21 item 2). Hammers are `#pyrotech:hammers` (CORE).
  Tool damage `damageItem(n, player)` becomes `stack.hurtAndBreak(n, player, LivingEntity.getSlotForHand(hand))`
  (NF-SRC `net/minecraft/world/item/ItemStack.java`, line 490; `net/minecraft/world/entity/LivingEntity.java`,
  line 3594).
- **Automation.** `hasCapability` and `getCapability` gated by `ALLOW_AUTOMATION` become
  `RegisterCapabilitiesEvent#registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> ...)`
  (NF-SRC `net/neoforged/neoforge/capabilities/RegisterCapabilitiesEvent.java`, line 59;
  `net/neoforged/neoforge/capabilities/Capabilities.java`, lines 37 and 29), registered unconditionally
  as STORAGE decided. Handlers are `ItemStackHandler` (NF-SRC `net/neoforged/neoforge/items/ItemStackHandler.java`,
  line 24) with `isItemValid` (134), `getSlotLimit` (125), `getStackLimit` (129), and `onContentsChanged`
  (176) calling `sync()`.
- **Progress particles.** `ParticleHelper.spawnProgressParticlesClient` gated by
  `ModuleCoreConfig.CLIENT.SHOW_RECIPE_PROGRESSION_PARTICLES` (`tile/TileBarrel.java`, lines 253 to 262, and
  eight other tiles) stays a client-side check in the block entity's client ticker, reading CORE's client
  config flag and `Level#addParticle` (NF-SRC `net/minecraft/world/level/Level.java`, line 530).
  `SCPacketParticleProgress` sent on hits (`tile/spi/TileAnvilBase.java`, lines 419 to 423) is CORE's
  dropped packet; the server uses `ServerLevel#sendParticles` (NF-SRC `net/minecraft/server/level/ServerLevel.java`,
  line 1258).
- **Contents on break.** `breakBlock` or `harvestBlock` spawning handler contents (`block/BlockCampfire.java`,
  lines 317 to 335) becomes `onRemove` guarded by `!state.is(newState.getBlock())` (NF-SRC `BlockBehaviour.java`,
  line 193) with `Containers.dropItemStack` (NF-SRC `net/minecraft/world/Containers.java`, line 31). The
  `removedByPlayer` and `harvestBlock` tricks that kept the tile alive for `getDrops` (`block/spi/BlockAnvilBase.java`,
  lines 133 to 156) drop: loot tables can copy block entity components (`CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY)`,
  NF-SRC `net/minecraft/world/level/storage/loot/functions/CopyComponentsFunction.java`, lines 77 and 121).

## The campfire

PROTOTYPE already ported the campfire's interaction and sync: variant and ash as properties, three
`ItemStackHandler`s in the snapshot, the if-chain, the scroll payload, the renderer, and fuel-scaled
light. This section records what PROTOTYPE stood in for and what it left out.

| 1.12 construct | 1.21 replacement |
|---|---|
| `VARIANT` normal, lit, ash, item and `ASH` 0 to 8 (`block/BlockCampfire.java`, lines 52 to 53, 432 to 438); `ASH` read from the tile in `getActualState` (lines 419 to 430) | PROTOTYPE's `EnumProperty` and `IntegerProperty` (`CampfireBlock.java`, lines 47 to 48). The `item` value exists only for the converted blockstate; regenerate `campfire.json` without it (PROTOTYPE open question 5, assets section). |
| `Material.WOOD`, hardness 0.5, shovel harvest tool (lines 66 to 70, 261 to 264) | PROTOTYPE's properties (`PrototypeCampfire.java`, lines 32 to 36) plus tag `minecraft:mineable/shovel` (NF-SRC `net/minecraft/tags/BlockTags.java`, line 142). |
| `getSoundType`: wood with fuel, sand as ash, else plant (lines 98 to 117) | `IBlockExtension#getSoundType(state, level, pos, entity)` (NF-SRC `net/neoforged/neoforge/common/extensions/IBlockExtension.java`, line 539) with `SoundType.WOOD`, `SAND`, `GRASS` (NF-SRC `net/minecraft/world/level/block/SoundType.java`, lines 10, 40, 16). |
| `getLightValue` scaled between `MINIMUM_LIGHT_LEVEL` 3 and `MAXIMUM_LIGHT_LEVEL` 11 by fuel out of 8 (lines 124 to 146) | PROTOTYPE's `getLightEmission` (`CampfireBlock.java`, lines 154 to 161) with the 1.12 numbers baked. |
| `getBlockLayer` `CUTOUT_MIPPED`, `doesSideBlockRendering` false (lines 154 to 163) | `"render_type": "minecraft:cutout_mipped"` in the twelve base models, already done on the prototype branch (PROTOTYPE finding); `noOcclusion()` (NF-SRC `BlockBehaviour.java`). Ticket #27. |
| `randomDisplayTick`: crackle 10 percent, four smoke and flame particles, eight large smoke when output is present (lines 166 to 197) | PROTOTYPE's `animateTick` (`CampfireBlock.java`, lines 232 to 248) plus the large-smoke branch it dropped, using `ParticleTypes.LARGE_SMOKE` (NF-SRC `net/minecraft/core/particles/ParticleTypes.java`, line 77) and `SoundEvents.FURNACE_FIRE_CRACKLE` (NF-SRC `net/minecraft/sounds/SoundEvents.java`, line 581), not the vanilla campfire crackle the prototype used. |
| `getBoundingBox`: full when fuelled, tinder box otherwise, ash boxes by level (lines 205 to 243) | PROTOTYPE's `getShape` (`CampfireBlock.java`, lines 163 to 170). |
| `isFireSource` when lit (lines 250 to 253) | `IBlockExtension#isFireSource` (line 700). PROTOTYPE omitted it; fire placed on a lit campfire must persist. |
| `onBlockActivated`: igniters return false, marshmallow sticks in the main hand return false, dead campfires return false, else the six interactions (lines 274 to 299) | `useItemOn` returns `SKIP_DEFAULT_BLOCK_INTERACTION` for `#pyrotech:igniters` (SIGN-OFFS, issue #34 item 1) and for both marshmallow stick items so `Item#use` runs (NF-SRC `net/minecraft/world/item/Item.java`, line 165), `PASS` when the variant is ash. PROTOTYPE's chain (`CampfireBlock.java`, lines 74 to 137) is the 1.12 order: extinguish, food, shovel, flint and steel, log. |
| `InteractionUseItemToActivateWorker(Items.FLINT_AND_STEEL)` and `(Items.FIRE_CHARGE, consume)` (`tile/TileCampfire.java`, lines 158 to 159; `library/InteractionUseItemToActivateWorker.java`, lines 37 to 64) | PROTOTYPE's branch (`CampfireBlock.java`, lines 95 to 109). Exact item tests, not `FIRESTARTER_LIGHT`, as the torches do (IGNITION). |
| `igniteWithIgniterItem` and `igniteWithAdjacentIgniterBlock`, both `workerSetActive(true)` (lines 77 to 90) | Core's `IBlockIgnitableWithIgniterItem` and `IBlockIgnitableAdjacentIgniterBlock` (CORE igniter contract), both calling the block entity's `ignite()`. The powered igniter reaches the campfire through the second (IGNITION). |
| `onEntityWalk`: hot floor damage 1 when lit, not fire immune, no frost walker (lines 302 to 313) | PROTOTYPE's `stepOn` (`CampfireBlock.java`, lines 222 to 230). Frost walker is data-driven in 21.1 and `MagmaBlock#stepOn` tests only `isSteppingCarefully` (NF-SRC `net/minecraft/world/level/block/MagmaBlock.java`, lines 29 to 35); use the same test. |
| `breakBlock`: drop contents, lava particles when lit (lines 317 to 335); `quantityDropped` 0 (lines 338 to 341) | `onRemove` with `dropContents` and `ServerLevel#sendParticles(ParticleTypes.LAVA, ...)` (NF-SRC `ParticleTypes.java`, line 78); `noLootTable()`. Drops stay in code because they depend on the `extinguishedByRain` flag (next row). |
| `canPlaceBlockAt` and `neighborChanged`: solid below or destroy; drop input and output when a soaking pot appears above (lines 348 to 352, 372 to 391) | PROTOTYPE's `canSurvive` and `updateShape` (`CampfireBlock.java`, lines 172 to 184) plus an `UP` branch in `updateShape` that ejects input and output when the neighbour above is a soaking pot. |
| `TileCombustionWorkerBase`: `burnTimeRemaining` synced, rain douse after `TICKS_BEFORE_EXTINGUISHED` 200 when `EXTINGUISHED_BY_RAIN`, `combustionGetInitialBurnTimeRemaining` = tinder burn time 120, `BURN_TIME_TICKS_PER_LOG` 2400 (`library/spi/tile/TileCombustionWorkerBase.java`, lines 104 to 155; `TileCampfire.java`, lines 267 to 298) | PROTOTYPE's server tick (`CampfireBlockEntity.java`, lines 211 to 246) with the 1.12 numbers and the rain check `level.isRainingAt(pos.above())` (NF-SRC `Level.java`, line 1074) counting down 200 ticks. `burnTimeRemaining` is not synced; the client reads nothing from it. The worker base classes are not ported; the campfire is their only tech/basic user and PROTOTYPE inlined them. |
| `combustionOnDeactivatedByRain` sets `extinguishedByRain`, which suppresses the tinder drop on break (lines 294 to 298, 630 to 638) | A boolean in `saveAdditional`, not synced. A campfire lit then doused by rain or water drops no tinder; a never-lit campfire drops tinder; a dead one drops pit ash (lines 632 to 638). |
| `workerConsumeFuel`: no fuel left, `setDead` (variant ash), spit input and output (lines 340 to 364) | PROTOTYPE's `die` (`CampfireBlockEntity.java`, lines 248 to 256). |
| `workerDoWork`: stop at ash 8; five percent per tick chance to turn a flammable block below into fire and stop (lines 414 to 432) | PROTOTYPE stops at ash 8. Add the flammable-floor branch: `state.isFlammable(level, pos.below(), Direction.UP)` (NF-SRC `net/neoforged/neoforge/common/extensions/IBlockStateExtension.java`, line 498) and `level.setBlock(pos.below(), BaseFireBlock.getState(level, pos.below()), 3)` (NF-SRC `net/minecraft/world/level/block/BaseFireBlock.java`, line 41). |
| Effects every 20 ticks between world time 12000 and 23000: players within 15 blocks and inside the fuel-scaled effect radius (1 to 6, lines 569 to 584), with no `EntityMob` within 15 blocks of the player, get comfort (re-added every 200 ticks) and resting, and the campfire joins the player's tracking set (lines 434 to 509) | Not in PROTOTYPE. Same loop in the server ticker: `level.getDayTime() % 24000` (NF-SRC `Level.java`, line 1002), `level.getEntitiesOfClass(Player.class, aabb, predicate)` and `Monster.class`, `addEffect` (NF-SRC `LivingEntity.java`, line 971). The 200-tick re-add is unnecessary with infinite durations (effects section). |
| Cooking: `cookTime` set from the recipe times `FUEL_LEVEL_FOR_FULL_COOK_SPEED` 4 on insert, decremented by fuel count per tick, output inserted at zero (lines 105 to 110, 513 to 528); burned food after `BURNED_FOOD_TICKS` 600 times 4, counted by fuel count, unless the output is already burned food (lines 100 to 103, 530 to 543) | PROTOTYPE's `cook` and `ageOutput` (`CampfireBlockEntity.java`, lines 258 to 285) with 1800 and 600 from the recipe source below and `ModuleCore.Items.BURNED_FOOD` (core) instead of charcoal. |
| Ash: `ASH_CHANCE` 0.25 per tick while burn time is at or below zero (lines 547 to 552) | PROTOTYPE rolls per consumed log (its shortcut list). Roll per tick at zero burn time as 1.12 does. |
| Input handler: one slot, limit 1, refuses items without a recipe or when the output slot is full; fuel handler: eight slots, limit 1, LIFO, `isValidFuel` (lines 977 to 1041) | PROTOTYPE's handlers (`CampfireBlockEntity.java`, lines 30 to 89). Fuel validity is `stack.is(ItemTags.LOGS_THAT_BURN)` (NF-SRC `net/minecraft/tags/ItemTags.java`, line 24), the 1.21 shape of `logWood` since nether stems do not burn; the CraftTweaker whitelist and blacklist become the item tag `#pyrotech:campfire_fuels` that datagen fills with `#minecraft:logs_that_burn` (decision 5). |
| `InteractionLog`: empty-hand click or scroll down removes a log with a 50 percent chance of 1 hot-floor damage when lit; full-hand click or scroll up adds one log with `WOOD_PLACE` (lines 857 to 916) | PROTOTYPE's `addLogFrom` and `removeLogTo` (`CampfireBlockEntity.java`, lines 155 to 183) with `PLAYER_BURN_CHANCE` 0.5 and `PLAYER_LOG_BURN_DAMAGE` 1 baked. |
| `InteractionShovel`: ash minus one, drop one pit ash, damage the shovel, `SAND_BREAK` (lines 939 to 971) | PROTOTYPE's `shovelAsh` with `ItemMaterial.PIT_ASH` (core) and `hurtAndBreak`. Shovel test is `SHOVEL_DIG`. |
| `InteractionExtinguish`: lit and not dead, drain 1000 mB of a `VALID_DOUSING_FLUIDS` fluid from the held container, deactivate, `FIRE_EXTINGUISH` (lines 757 to 770; `library/spi/interaction/InteractionExtinguishable.java`, lines 45 to 89) | The shared douse helper IGNITION found: `FluidUtil.getFluidHandler(stack)` (NF-SRC `net/neoforged/neoforge/fluids/FluidUtil.java`, line 368), drain 1000 with `SIMULATE` then `EXECUTE` (NF-SRC `net/neoforged/neoforge/fluids/capability/IFluidHandler.java`, lines 18 and 103) when the drained fluid `is(#pyrotech:dousing)` (NF-SRC `net/neoforged/neoforge/fluids/FluidStack.java`, line 241), then the block's action and `SoundEvents.FIRE_EXTINGUISH` (line 525). The helper lives in `library/fluid` beside STORAGE's tank helpers and takes the bounds from the caller, which removes its import of `BlockCampfire`. |
| `InteractionFood` on `UP`, validated by `CampfireRecipe.getRecipe`, sneak-click refused (lines 772 to 813) | PROTOTYPE's `insertFood` and `extractFoodTo` (`CampfireBlockEntity.java`, lines 128 to 153). |
| Capability: input on `UP`, output on `DOWN`, fuel elsewhere (lines 226 to 260) | PROTOTYPE's registration (`PrototypeCampfire.java`, lines 63 to 74). |
| `onTileDataUpdate`: relight on fuel, active, or dead changes; block update on ash (lines 693 to 714) | PROTOTYPE's `onSyncedDataUpdate` relight (`CampfireBlockEntity.java`, lines 343 to 346); ash and variant are properties. |
| `CampfireInteractionLogRenderer`: four leaning logs then four flat diagonals, the held log as ghost (`client/render/CampfireInteractionLogRenderer.java`, lines 25 to 126) | PROTOTYPE's `CampfireRenderer` copies the transforms (`CampfireRenderer.java`, lines 12 to 21). The ghost pass is the additive preview PROTOTYPE kept as a pattern but did not draw; it reads `Minecraft.getInstance().hitResult` (NF-SRC `net/minecraft/client/Minecraft.java`, line 355). |

**The cook list.** `CampfireRecipe.getRecipe` caches, checks the custom registry, then asks
`RecipeHelper.hasFurnaceFoodRecipe` and builds a recipe from `FurnaceRecipes.getSmeltingResult` with
`COOK_TIME_TICKS` 1800, filtered by the whitelist or the blacklist (`recipe/CampfireRecipe.java`, lines
37 to 90). The config blacklist defaults to bread and cookie (`ModuleTechBasicConfig.java`, lines 1329 to
1332), which Pyrotech's own smelting recipes make from dough (SKIPS gate table). In 1.21 the block
entity holds a `RecipeManager.CachedCheck` for `RecipeType.SMELTING` (NF-SRC
`net/minecraft/world/item/crafting/RecipeManager.java`, line 83; `RecipeType.java`, line 9), accepts an
input when the matched recipe's `getResultItem` (NF-SRC `net/minecraft/world/item/crafting/AbstractCookingRecipe.java`,
line 57) carries `DataComponents.FOOD` (NF-SRC `net/minecraft/core/component/DataComponents.java`, line 117)
and is not in the item tag `#pyrotech:campfire_blacklist`, and cooks it in the campfire's own time,
not the recipe's 200 ticks. The nine vanilla foods this yields in 21.1.249 are baked potato, cooked
beef, chicken, cod, mutton, porkchop, rabbit, salmon, and dried kelp (DATA `data/minecraft/recipe/*.json`,
the smelting recipes whose result item has food properties; `popped_chorus_fruit` has none, NF-SRC
`net/minecraft/world/item/Items.java`, line 1764). The vanilla `campfire_cooking` type is never read
(SIGN-OFFS, issue #33 item 2). The explicit registry becomes a small `pyrotech:campfire` recipe type
(`Ingredient`, `ItemStack`, `int ticks`) checked first, as 1.12 checks the custom registry first (line
50); its only 1.12 entry is the Patchouli book (findings, book ticket). Decision 4 asks whether to keep
the explicit type at all.

**The marshmallow roasting hook.** The stick items answer `isRoastingBlock` by
`world.getTileEntity(pos) instanceof TileCampfire` (`item/ItemMarshmallowStick.java`, lines 570 to 573),
with no check that the campfire is lit. The port keeps the block-entity test. Whether a lit campfire
should be required is not a 1.12 behaviour and is not proposed.

## The pit kiln

| 1.12 construct | 1.21 replacement |
|---|---|
| `VARIANT` empty, thatch, wood, active, complete (`block/BlockKilnPit.java`, lines 60, 359 to 366); `damageDropped` always empty (lines 169 to 172) | `EnumProperty` with the five values; the converted `kiln_pit.json` on `main` keys on them. The item is always the empty kiln (loot below). |
| `Material.WOOD`, hardness 0.6, shovel tool, sound by variant, bounding box 3/16 when empty else full, thatch collision 10/16, `isFullBlock` and `isSideSolid` for wood, active, complete and for `DOWN` unless empty, `getFlammability` 0 (lines 64 to 140, 185 to 200) | `Properties.of().mapColor(MapColor.WOOD).strength(0.6f)` with `mineable/shovel`; `getSoundType` by variant; `getShape` and `getCollisionShape` (NF-SRC `BlockBehaviour.java`, lines 361 and 365) with `Block.box` (NF-SRC `net/minecraft/world/level/block/Block.java`, line 151); `isFaceSturdy` follows from the shape; `getFlammability` 0 (`IBlockExtension.java`, line 646). |
| `isFireSource` when active (lines 143 to 146) | `IBlockExtension#isFireSource` (line 700). |
| `neighborChanged` calling `setNeedStructureValidation` (lines 175 to 182) | `neighborChanged` (NF-SRC `BlockBehaviour.java`, line 186) doing the same. |
| `igniteWithAdjacentFire`: only from `UP` and only when wood, set active (lines 207 to 219); `igniteWithAdjacentIgniterBlock`: when wood and `Util.canSetFire(above)`, place fire above (lines 222 to 229) | Core's `IBlockIgnitableAdjacentFire` and `IBlockIgnitableAdjacentIgniterBlock`. `canSetFire` (not a liquid, and air or replaceable, `library/util/Util.java`, lines 69 to 79) stays the 1.12 test as the refractory sign-off chose for the collector (SIGN-OFFS, issue #29 item 9). |
| `onBlockActivated`: igniters, flint and steel, and fire charges return false, else interactions (lines 260 to 273) | `useItemOn` returns `SKIP_DEFAULT_BLOCK_INTERACTION` for `#pyrotech:igniters` and for `canPerformAction(FIRESTARTER_LIGHT)` items (NF-SRC `ItemAbilities.java`, line 144), so vanilla flint and steel places fire on top and core's fire-adjacency listener calls `igniteWithAdjacentFire` (SIGN-OFFS, issue #16 comment 2). |
| `breakBlock`: spawn input, outputs, and logs (lines 276 to 296); `canSilkHarvest` false; `quantityDropped` 0 when active or complete; `getDrops` adds one thatch when wood or thatch (lines 306 to 339) | `onRemove` drops the three handlers. A loot table with `LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(...)` (NF-SRC `net/minecraft/world/level/storage/loot/predicates/LootItemBlockStatePropertyCondition.java`, line 53): the kiln item for empty, thatch, and wood; plus one `thatch` (core) for thatch and wood; nothing for active and complete. |
| `TileBurnableBase` parent (`tile/TileKilnPit.java`, line 59; `library/spi/tile/TileBurnableBase.java`, lines 37 to 117) | The shared library base from CORE and REFRACTORY, as a plain `BlockEntity` with a server `BlockEntityTicker`. One stage (`getBurnStages` 1, line 177 to 180). |
| `InputStackHandler` one slot, limit `MAX_STACK_SIZE` 8; `OutputStackHandler` `LargeDynamicStackHandler(9)`; `LogStackHandler` three slots, limit 1 (lines 746 to 786) | Three `ItemStackHandler`s: input limit 8, output nine slots, logs three slots limit 1. No large handler: nine slots of normal stacks hold at most 8 outputs plus 3 ash. All three are in the snapshot; the renderer draws the input and the logs. |
| `progress` synced every 20 ticks (lines 73, 99 to 106, 252) | Server-only. Only Waila and TOP read it. |
| `updateBurnTime`: total = recipe time times `(1 - n) * x + n`, n = `VARIABLE_SPEED_MODIFIER` 0.5, x = (count - 1) / (8 - 1) (lines 205 to 222) | Same arithmetic, with the recipe time from `KilnRecipe#burnTime` times the pit kiln's `BASE_RECIPE_DURATION_MODIFIER` (config section). |
| `onUpdate`: rain on `pos.up()` counts down `TICKS_BEFORE_EXTINGUISHED` 200 when `EXTINGUISHED_BY_RAIN`, then back to thatch, remove fire, inactive (lines 225 to 253) | Same, with `isRainingAt`. |
| `onUpdateValid`: keep fire above when air or replaceable (lines 256 to 275) | `BlockState#isAir` and `canBeReplaced` (NF-SRC `BlockBehaviour.java`, lines 618 and 869), `BaseFireBlock.getState`. |
| `onInvalidDelayExpired`: every input item becomes a random failure item or one pit ash, then complete and clear the fire (lines 285 to 321) | Same. |
| `isStructureValid`: variant wood or active; above air, replaceable, or fire; four horizontals and below pass `isValidStructureBlock` (lines 329 to 363); a refractory block always passes (lines 366 to 373) | Same test with `isFaceSturdy` and `isFlammable` from the base, plus core's refractory test (below). |
| `onAllBurnStagesComplete`: per item, failure chance times `1 - refractoryFaces / 5`, failure item or output; plus 1 to 3 pit ash; complete; clear fire (lines 376 to 419, 421 to 439) | Same. |
| `isRefractoryBlock` walking `ModuleTechRefractory.Registries.REFRACTORY_BLOCK_LIST` behind the module toggle (lines 441 to 454) | `RefractoryBlocks.isRefractory(BlockState)` in core, beside the `#pyrotech:refractory` `TagKey`: `state.is(tag)` (NF-SRC `BlockBehaviour.java`, line 886) or core's refractory slab with `SlabBlock.TYPE == SlabType.DOUBLE` (NF-SRC `net/minecraft/world/level/block/SlabBlock.java`, line 30). Both inputs are core's, so core can own the helper. The active pile adds its door case on top in refractory (REFRACTORY). No edge from tech/basic to refractory remains. |
| `getUpdateTag`, `getUpdatePacket`, `onDataPacket` full-NBT sync (lines 495 to 512) | `SyncedBlockEntity`. |
| `InteractionThatch`: thatch on an empty kiln, one thatch consumed, `GRASS_PLACE` (lines 556 to 603) | `useItemOn` branch: `held.is(core thatch item)` and variant empty. Sound `SoundEvents.GRASS_PLACE` (line 642). |
| `InteractionLog` times three: `logWood` into slot by the top third of the block (`slot * 1/3` in x, y above 2/3), enabled when thatch or wood, `WOOD_PLACE`, all three full sets wood, extract from wood sets thatch (lines 605 to 674) | Branch when variant is thatch or wood and the hit is on the upper third: slot = floor(localX * 3) after facing rotation (the kiln has no facing; use world x). Logs are `#minecraft:logs_that_burn`. |
| `Interaction`: input and output on any face when empty, ghost item at 0.5 scale, empty-hand extract empties every output slot into the player (lines 676 to 740) | Branch when variant is empty: insert with recipe check, or extract all nine output slots then the input, with `Player#addItem` (NF-SRC `Player.java`, line 1906) and a drop fallback. |
| `shouldRefresh` keeping the tile across variant changes (lines 519 to 531) | Not needed. A block entity survives state changes of the same block. |

State split: `variant` on the blockstate; input, logs, and outputs in the snapshot; burn timers,
rain timer, and `active` server-only. The active variant already tells the client the kiln is lit.

## The chopping block

| 1.12 construct | 1.21 replacement |
|---|---|
| `DAMAGE` 0 to 5 and `SAWDUST` 0 to 5, sawdust read from the tile in `getActualState` (`block/BlockChoppingBlock.java`, lines 39 to 40, 176 to 186) | Two `IntegerProperty`s (NF-SRC `net/minecraft/world/level/block/state/properties/IntegerProperty.java`, line 56). `setSawdust` writes the property (`tile/TileChoppingBlock.java`, lines 111 to 118). The converted `chopping_block.json` keys on both. |
| `Material.WOOD`, hardness 0.75, `SoundType.WOOD`, box 6/16, side solid only `DOWN` (lines 44 to 49, 169 to 172, 194 to 197) | `mapColor(WOOD).strength(0.75f).sound(WOOD)`, `getShape` box, `mineable/axe`. |
| `getDrops` serialising the tile into the item with `DAMAGE` as metadata; `ItemChoppingBlock.getMetadata` (lines 102 to 113, 199 to 212) | Loot table `dropSelf` (NF-SRC `net/minecraft/data/loot/BlockLootSubProvider.java`, line 748) with `CopyComponentsFunction(BLOCK_ENTITY)` carrying the input stack, plus a `pyrotech:block_damage` int component written by `collectImplicitComponents` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntity.java`, line 322) and read back in `getStateForPlacement` (NF-SRC `Block.java`, line 398) from `context.getItemInHand()`. The anvils share the component (decision 8). |
| `harvestBlock` spawning contents and resetting sawdust (lines 76 to 95) | `onRemove` drops the input. Sawdust is lost on break, as in 1.12 (the drop line is commented out at line 85). |
| Tile: one-slot handler limit 1 refusing items without a recipe; `recipeProgress`; `sawdust`; `durabilityUntilNextDamage` starting at `CHOPS_PER_DAMAGE` 16 (`tile/TileChoppingBlock.java`, lines 54 to 90, 552 to 578) | Snapshot: the input stack. Server-only: progress, durability. |
| `Interaction` on `UP`: insert with `WOOD_PLACE` (lines 256 to 292) | `useItemOn` branch on `UP` with a `ChoppingBlockRecipe` check; `useWithoutItem` extracts. |
| `InteractionShovel` on any face: sawdust above 0 and (empty hand or shovel); with `REQUIRE_SHOVEL_TO_PICKUP_WOOD_CHIPS` a shovel gives a wood chips rock and takes 1 damage, an empty hand only removes sawdust; else one wood chips rock; `SAND_BREAK`; exhaustion 0.5 (lines 294 to 345) | Same branch reading core's tweak toggle; `SHOVEL_DIG`; `SoundEvents.SAND_BREAK` (line 1190); the wood chips rock is core's flattened `rock_wood_chips` item. |
| `InteractionChop` on `UP`: hunger gate, axe test with lists (lines 347 to 383) | The shared tool tests. |
| `doInteraction`: exhaustion 1.5; durability, damage stages, break at 6 with contents and sawdust spawned; `WOOD_PLACE` at the player; two times `WOOD_CHIPS_CHANCE` to add sawdust, half the chance to place a wood chips rock in a random air neighbour within one block where a rock can sit; recipe progress `1 / chops[level]`; on complete, output count `quantities[level]`, `WOOD_BREAK`; client crack particles (lines 385 to 546) | Same, with `BlockPos.betweenClosed` (NF-SRC `net/minecraft/core/BlockPos.java`, line 397), `level.random`, and `state.canSurvive` on core's rock block. Particles: `ParticleTypes.BLOCK` with `BlockParticleOption` (NF-SRC `ParticleTypes.java`, line 15) on the client side of `useItemOn`. The arrays come from the recipe's optional fields (SIGN-OFFS, issue #21 item 4) and the level from `pyrotech:tool_levels`, with the past-the-end rule. |
| `USES_DURABILITY` toggle (config line 1002) | `COMMON` toggle (config section). |

The five post-1.12 woods: the 1.12 recipe set is generated from every vanilla log-to-planks crafting
recipe with a single exact ingredient (`modules/core/init/CompatInitializerWood.java`, lines 52 to 71),
six in 1.12. In 21.1.249 there are eleven plank recipes, all with tag ingredients (DATA
`data/minecraft/recipe/cherry_planks.json`, `mangrove_planks.json`, `bamboo_planks.json`,
`crimson_planks.json`, `warped_planks.json`, and the six old ones). Datagen writes eleven chopping block
recipes with `Ingredient.of(tag)` (NF-SRC `net/minecraft/world/item/crafting/Ingredient.java`, line 222)
on each wood's log tag and the planks as result (SIGN-OFFS, issue #33 item 1). The output count is
managed by the quantity array (`recipe/ChoppingBlockRecipe.java`, line 56), so bamboo yields 1 to 4
planks by axe level like every other wood, not vanilla's 2. Since the sawmill chain copies these, the
five new woods reach the sawmills for free (findings, machine).

## The anvils

Three blocks share `BlockAnvilBase` and `TileAnvilBase`; the subclasses differ only in material,
hardness, sound, harvest level, and which config category they read (`block/BlockAnvilGranite.java`,
lines 14 to 21; `BlockAnvilIronPlated.java`, lines 14 to 21; `BlockAnvilObsidian.java`, lines 14 to 21;
`tile/TileAnvilGranite.java`, lines 17 to 82, and the two `diff`-identical siblings).

| 1.12 construct | 1.21 replacement |
|---|---|
| `DAMAGE` 0 to 3 (`block/spi/BlockAnvilBase.java`, line 45); `ItemAnvil` metadata = damage; `getDrops` serialising the tile (lines 163 to 174, 271 to 284) | `IntegerProperty` (the converted `anvil_granite.json` and siblings key on `damage=`), the `pyrotech:block_damage` component, and the loot copy described for the chopping block. |
| Granite: rock, 3.0 and 5.0, stone, pickaxe 0; ironclad: iron, 5.0 and 10.0, metal, pickaxe 1; obsidian: rock, 50.0 and 2000.0, stone, pickaxe 0 | Three blocks with those `strength(a, b)` and `sound(...)` values, `mineable/pickaxe`, and `needs_stone_tool` for the ironclad (NF-SRC `BlockTags.java`, line 146). One block class taking the tier and the six numbers below as constructor arguments. |
| Box 6/16, side solid only `DOWN` (lines 47, 232 to 235, 266 to 269) | `getShape` box. |
| `getLightValue` from a held `ItemBlock`'s block default state (lines 59 to 75) | `IBlockExtension#getLightEmission` (line 155) reading `((BlockItem) item).getBlock().defaultBlockState().getLightEmission()` (NF-SRC `BlockBehaviour.java`, line 614). This is how a bloom on an anvil glows. |
| `hasBloom`: the slot holds `ModuleTechBloomery.Items.BLOOM`; `onEntityWalk` hot-floor damage `ModuleTechBloomeryConfig.BLOOM.ENTITY_WALK_DAMAGE`; `randomDisplayTick` flames and crackle (lines 81 to 117, 239 to 258) | The anvil stays bloom-agnostic (SIGN-OFFS, issue #21 item 3). Tech/basic declares a small interface `AnvilHotItem { float walkDamage(); }` that bloomery's bloom item implements; `stepOn` and `animateTick` test `stack.getItem() instanceof AnvilHotItem`. The dependency runs bloomery to basic (decision 7). |
| `TileAnvilBase`: one-slot handler limit 1 accepting only inputs with a recipe for the tier; `recipeProgress`; `durabilityUntilNextDamage`; `recipe`; `tileDataRecipeString` for Waila (`tile/spi/TileAnvilBase.java`, lines 56 to 104, 581 to 610) | Snapshot: the input stack. Server-only: progress, durability, the current `RecipeHolder`. The recipe string dies with Waila. The client resolves the recipe itself for the hit hook (network section). |
| `doInteraction(tool, player, hit)`: type from the tool, recipe by input, tier, and type; progress reset on recipe change; exhaustion per hit; durability countdown, damage stages, at 4 either `onAnvilDurabilityExpired` or break with contents; `STONE_HIT`; `applyDamage` hook or durability minus one; hit reduction = pickaxe harvest level or hammer level; `hits = max(1, recipe.hits - reduction)`; `getModifiedRecipeProgressIncrement` hook with the hammer position (player eye, or the anvil when no player); progress particles; on complete `onRecipeCompleted` hook or extract and output, `STONE_BREAK`, exhaustion; anvil hit packet (lines 309 to 464) | A public `hit(ItemStack tool, @Nullable Player player, Vec3 hit)` returning the outputs, because the trip hammer calls it with a null player (findings, machine). Hit reduction is `pyrotech:tool_levels` for both pickaxes and hammers, indexed into the `HIT_REDUCTION_PER_HAMMER_HARVEST_LEVEL` table for hammers (config). Sounds `STONE_HIT` and `STONE_BREAK` (lines 1370 and 1366). |
| `AnvilRecipe.IExtendedRecipe`: `applyDamage`, `getModifiedRecipeProgressIncrement`, `onRecipeCompleted`, `onAnvilHitClient`, `onAnvilDurabilityExpired`, `allowInheritance` (`recipe/AnvilRecipe.java`, lines 173 to 192) | The same five hooks on a tech/basic interface `ExtendedAnvilRecipe` that the anvil block entity tests with `instanceof` on the matched recipe. `allowInheritance` dies: inheritance is datagen (SIGN-OFFS, issue #21 item 1) and bloomery's recipe is not copied. The bloom-specific accessors the hooks call (`getBloomAnvilExtraDamagePerHit`, `getBloomAnvilExtraDamageChance`, `useDurability`, `getDurabilityUntilNextDamage`, `setDurabilityUntilNextDamage`, `getStackHandler`, lines 150 to 208) stay public on the block entity. |
| `Interaction` on `UP`: insert with `hasRecipe(stack, tier)`, `WOOD_PLACE` at 0.5; extract clears the recipe (lines 475 to 527) | `useItemOn` and `useWithoutItem` branches on `UP`. |
| `InteractionHit` on `UP`: hunger gate; recipe for held tool and input; run `doInteraction`; spawn outputs (lines 529 to 575) | Branch when the held item resolves to a tool type and a recipe matches. |
| `AnvilRecipe.getTypeFromItemStack`: hammer if in `HAMMER_LIST`, else pickaxe if tool class pickaxe and not blacklisted, else pickaxe if whitelisted (lines 65 to 98) | A static resolver on RECIPES' `AnvilRecipe`: `#pyrotech:hammers` first, then `PICKAXE_DIG`. The lists drop. The trip hammer calls this resolver too. |
| Ironclad and obsidian adders with empty own lists and `registerInheritedRecipes` scaling hits by `INHERITED_*_HIT_MODIFIER` 1.0 and skipping extended recipes that refuse inheritance (`init/recipe/AnvilIroncladRecipesAdd.java`, lines 13 to 52; `AnvilObsidianRecipesAdd.java`, lines 13 to 53) | Two edges in RECIPES' chain table at multiplier 1: granite to ironclad, ironclad to obsidian. Both are tech/basic's own chains, so `TechBasicRecipes` emits the 100 copies. |

## The barrel

| 1.12 construct | 1.21 replacement |
|---|---|
| Two blocks, `barrel` and `barrel_sealed`, swapped by `setState` with a `keepInventory` flag and the tile re-validated (`block/BlockBarrel.java`, lines 43 to 83) | One block with a `sealed` `BooleanProperty` (NF-SRC `net/minecraft/world/level/block/state/properties/BooleanProperty.java`, line 19). The lid observer writes the property (`tile/TileBarrel.java`, lines 94 to 108). The block entity survives a property change of its own block, which is the whole reason for the 1.12 dance. Decision 9. |
| `Material.WOOD`, 1.0 and 3.0, wood sound, axe 0 (lines 50 to 58) | `mapColor(WOOD).strength(1.0f, 3.0f).sound(WOOD)`, `mineable/axe`. |
| Unsealed break drops input and lid; sealed break serialises the tile into a stack-size-1 item; the sealed item is hidden from the creative tab (lines 105 to 164, 188 to 191, 255 to 263) | Loot table: `sealed=false` drops self, contents through `onRemove`; `sealed=true` drops self with `CopyComponentsFunction(BLOCK_ENTITY)` carrying the fluid (`SimpleFluidContent`, STORAGE) and the four items (`ItemContainerContents`, NF-SRC `DataComponents.java`, line 216) and `stacksTo(1)` applied by a `pyrotech:sealed` marker component. Placement restores the state from the components in `setPlacedBy` (NF-SRC `Block.java`, line 414). |
| `addInformation`: with shift, the fluid line and the item names from the tile tag; else the shift hint; always the hot-fluids line (lines 198 to 249) | `Item#appendHoverText` (NF-SRC `Item.java`, line 332) reading the components, `Screen.hasShiftDown` (NF-SRC `net/minecraft/client/gui/screens/Screen.java`, line 433). Keys `gui.pyrotech.tooltip.fluid`, `.extended.shift`, `.hot.fluids.false` are on `main` (`en_us.json`, lines 405, 395, 402). |
| `InputFluidTank(1000)`, `fill` refused when sealed, `fillInternal` hot-fluid rule at 450 breaking the block, the "Empty" tag workaround (lines 581 to 665) | STORAGE's `HotFluidTank` (450, not tolerant) in `library/fluid`, `setValidator` refusing fills while sealed. STORAGE decision 6 (break on `EXECUTE` only) applies. |
| `InputStackHandler(4)` limit 1, refused when sealed; `LidStackHandler(1)` limit 1 (lines 535 to 579) | Two `ItemStackHandler`s with the same rules. |
| `update`: unsealed and raining on top, every `RAIN_WATER_FILL_DURATION_TICKS` 20 add 5 mB water below 1000, every `RAIN_WATER_CONVERSION_DURATION_TICKS` 2400 convert any other fluid to water; sealed: progress by `1 / time`, on complete clear the four items and replace the fluid with the output when different (lines 201 to 293) | Same server tick with `Fluids.WATER` (NF-SRC `net/minecraft/world/level/material/Fluids.java`, line 9). |
| `updateRecipe` on every change: `BarrelRecipe.getRecipe(items, fluid)` (lines 295 to 302) | `RecipeManager#getRecipeFor` with a `BarrelRecipeInput(List<ItemStack> items, FluidStack fluid)`. |
| Capability: item and fluid handlers on `UP` only and only when unsealed (lines 315 to 353) | `registerBlockEntity` for both capabilities with the side and state test. |
| `InteractionInputFluid` on `UP` (bucket), `InteractionLid` on `UP` (barrel lid only), four `InteractionInputItem`s on `UP` in a 2 by 2 grid at x and z 2 to 8 or 8 to 14 px, valid only when the fluid is set and `BarrelRecipe.isValidItem(item, fluid)`; the item slots disabled when sealed (lines 121 to 134, 405 to 529) | `useItemOn` on `UP`: `FluidUtil.interactWithFluidHandler(player, hand, level, pos, UP)` (NF-SRC `FluidUtil.java`, line 56) first, then the lid, then the slot by `floor(localX * 2)` and `floor(localZ * 2)`; nothing when sealed. |
| `BarrelFluidRenderer`: one quad inset 2 px at `12 px * fill + 2 px`, still sprite, fluid colour, light from above (`client/render/BarrelFluidRenderer.java`, lines 28 to 111) | STORAGE's fluid box renderer with those numbers, plus the four item transforms at 3/16 scale (lines 445 to 449) and the lid flat on top (lines 415 to 419). |
| `recipeProgress` synced (line 66) | Server-only. |

Snapshot: fluid, four items, lid. Property: `sealed`.

## The compacting bin

| 1.12 construct | 1.21 replacement |
|---|---|
| `Material.WOOD`, 4.0 and 0.5, no properties, side solid only `DOWN` (`block/BlockCompactingBin.java`, lines 31 to 36, 95 to 98) | `mapColor(WOOD).strength(4.0f, 0.5f)`, `mineable/axe`; the converted `compacting_bin.json` has one variant. |
| `breakBlock` spawns slot 0 (lines 58 to 70) | `onRemove` drops the handler. |
| `InputStackHandler extends DynamicStackHandler(1)`: accepts only items whose recipe equals the current recipe, up to `MAX_CAPACITY` 4 times the recipe amount, growing slots; `removeItems` and `extractItem` from the last slot (`tile/TileCompactingBin.java`, lines 445 to 523) | STORAGE's `LargeStackHandler` with one slot whose limit is `4 * recipe.amount` (288 for the 72-flake charcoal block). Insert trims to the room left as 1.12 does. |
| `recipeProgress` synced; `currentRecipe` recomputed on every change and on `onTileDataUpdate` (lines 62 to 96, 127 to 164) | Snapshot: the input stack; the client recomputes the recipe from it for the renderer. Progress server-only. |
| `InteractionInput` on `UP`, whole block, `WOOD_PLACE` at 0.5 (lines 290 to 353) | `useItemOn` branch. |
| `InteractionShovel` on `UP`: hunger gate, a full recipe's worth present, shovel test with lists; per use progress `1 / uses[level]`, on complete spawn output, remove the amount, damage the tool `TOOL_DAMAGE_PER_CRAFT` 1, exhaustion (lines 355 to 439) | Same with `SHOVEL_DIG`, `pyrotech:tool_levels`, and the recipe's optional uses field defaulting to `{4, 3, 2, 1}` (SIGN-OFFS, issue #21 item 4). |
| `CompactingBinInteractionInputRenderer`: the output block scaled 12/16 wide and 1/16 tall at height `fill * 13/16 + 1.5/16`, the count text `total / amount`, the held item as ghost unless full (`client/render/CompactingBinInteractionInputRenderer.java`, lines 26 to 98) | A `BlockEntityRenderer` with `ItemRenderer#renderStatic` (NF-SRC `net/minecraft/client/renderer/entity/ItemRenderer.java`, line 228) and the count text from the storage renderer helper. |

The bloomery adds slag recipes and the machine adds a mechanical tier that inherits (findings).

## The compost bin

| 1.12 construct | 1.21 replacement |
|---|---|
| `PROPERTY_STATE` dry, wet, ready and `PROPERTY_COMPOST_VALUE` 0 to 5, both derived from the tile in `getActualState` (`block/BlockCompostBin.java`, lines 38 to 42, 186 to 230) | Two `IntegerProperty`s written by the block entity when moisture, output presence, or the fill bucket changes; the converted `compost_bin.json` keys on `state=` and `compost_value=`. |
| `Material.WOOD`, 2.0 and 0.5, box 1 to 15 px, side solid only `DOWN` (lines 36, 47 to 58, 66 to 69, 158 to 161) | `strength(2.0f, 0.5f)`, `getShape` box. |
| `breakBlock`: spawn input slot 0, output slot 0, and one dirt rock per stored compost value (lines 114 to 133) | `onRemove` with the same spill; the dirt rock is core's `rock_dirt`. |
| Input `DynamicStackHandler(1)` bounded by total compost value; output `DynamicStackHandler(16)`; `InputFluidTank(1000)` water only, no drain, refuses fills when the bin is empty, fill capped at 1000 mB, and a hand fill places flowing water level 7 in every replaceable horizontal neighbour when it is not raining (`tile/TileCompostBin.java`, lines 927 to 1123) | Input as STORAGE's `LargeStackHandler` (one slot, limit by compost value), output as `ItemStackHandler(16)`, `FluidTank(1000)` with `setValidator(stack -> stack.is(Tags.Fluids.WATER))` (NF-SRC `net/neoforged/neoforge/fluids/capability/templates/FluidTank.java`, line 39; `net/neoforged/neoforge/common/Tags.java`, line 941) and a capability wrapper whose `drain` returns empty. The splash keeps `Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 7)` (NF-SRC `net/minecraft/world/level/block/LiquidBlock.java`, line 53). |
| `update` every 20 ticks: drain when empty; rain adds 20 mB; else evaporate 1 mB per 48 ticks; layer loop with `COMPOST_DURATION_TICKS` 96000, 20 percent faster per active layer above, convert input to stored value past 30 percent progress, emit output at completion, rotate complete empty layers to the top (lines 204 to 358) | Same loop in the server ticker with `isRainingAt`. |
| Sixteen `TileDataFloat` layer progresses, stored value, current recipe output, both handlers, and the tank synced (lines 112 to 131) | Snapshot: the two handlers, the fluid, the stored value, the current output. The layer progresses are server-only; the client particle test needs only active and complete layer counts, which derive from the synced totals (lines 417 to 449 use only totals). |
| `InteractionInput` on `UP`, ghost only; `InteractionShovel` on `UP` taking one output stack with 1 tool damage; `InteractionInputFluid` on `UP` (lines 786 to 921) | Three `useItemOn` branches; the bucket branch is `FluidUtil.interactWithFluidHandler`. |
| `CompostBinRecipeBase.calculateCompostValue`: food gives `(max - min) * (hunger + saturation) / 2 + min` with hunger = heal / 20 and saturation = hunger times modifier times 2 over 20, clamped to `GENERATED_FOOD_COMPOST_VALUE_RANGE` {1, 8}; anything else 1 (`library/CompostBinRecipeBase.java`, lines 52 to 68) | Same formula on `FoodProperties#nutrition` and `saturation` (NF-SRC `net/minecraft/world/food/FoodProperties.java`, lines 17 to 18); note `saturation()` is already nutrition times modifier times 2, so the 1.21 term is `saturation / 20`. |
| `CompostBinRecipe` keyed by `domain_path_meta` registry names, wildcard fallback, output always four mulch except by CraftTweaker (`recipe/CompostBinRecipe.java`, lines 31 to 97; `init/recipe/CompostBinRecipesAdd.java`, lines 117 to 156) | Decision 3: a `pyrotech:compost_bin` recipe type (`Ingredient input`, `int value`, `ItemStack result`) or an item data map. |
| `AUTO_CREATE_RECIPES_FROM_FOOD`: every `ItemFood` in the registry gets a recipe at the computed value (lines 92 to 112) | A runtime fallback in the block entity: an item with `DataComponents.FOOD` and no explicit recipe composts at the formula's value. Datagen cannot enumerate other mods' foods (decision 3). |
| `TooltipEventHandler.CompostValue`: "Compost Value: n" as the second tooltip line (`event/TooltipEventHandler.java`, lines 16 to 35) | `ItemTooltipEvent` (NF-SRC `net/neoforged/neoforge/event/entity/player/ItemTooltipEvent.java`, lines 45 and 52) behind the client flag (config). Key on `main` (`en_us.json`, line 429). |

## The drying racks

| 1.12 construct | 1.21 replacement |
|---|---|
| One block, `VARIANT` crude, normal, stacked plus `FACING`; metadata packs variant and facing; two item variants; `stacked` derived in `getActualState` when a normal rack sits above (`block/BlockDryingRack.java`, lines 46 to 47, 197 to 262, 271 to 313) | Two blocks, `drying_rack_crude` and `drying_rack` (CORE flattening), each with `HORIZONTAL_FACING` (NF-SRC `net/minecraft/world/level/block/state/properties/BlockStateProperties.java`, line 54), and a `stacked` `BooleanProperty` on the normal rack set in `updateShape` for `Direction.UP` (NF-SRC `BlockBehaviour.java`, line 178). The lang keys on `main` already guess these ids (`en_us.json`, lines 117 to 118). Blockstates regenerate (assets). |
| `Material.WOOD`, 0.5 and 5.0, wood sound, axe 0, flammability 150 (lines 55 to 71) | `strength(0.5f, 5.0f).sound(WOOD)`, `mineable/axe`, `getFlammability` 150 (`IBlockExtension.java`, line 646). |
| Placement: facing opposite the clicked face, or the placer's facing for up and down; a crude rack placed against a crude rack copies its facing (lines 108 to 127) | `getStateForPlacement(BlockPlaceContext)` with `context.getClickedFace()` and `getHorizontalDirection()`. |
| Boxes: crude 5 px deep against the facing wall at y 11 to 16; normal 1 to 15 px at y 11 to 12 (lines 49 to 53, 140 to 160) | `getShape` per facing. |
| `hasTileEntity` and `createTileEntity` by variant (lines 167 to 189) | One `BlockEntityType` over both blocks; the block passes slot count (1 or 4) and its config category to the block entity. |
| `TileDryingRackBase extends TileEntityDataWorkerBase`, always active, per-slot `dryTimeTotal` and `dryTimeRemaining`, `partialTicks`, `speed` synced every 20 ticks, input and output handlers limit 1 (`tile/spi/TileDryingRackBase.java`, lines 32 to 80, 141 to 214, 431 to 461) | A `SyncedBlockEntity` with the two handlers and the speed float in the snapshot (the client reads speed only for particles), timers server-only. The worker base is not ported. Uses `ModuleCore.TILE_DATA_SERVICE`, not tech/basic's (line 52); irrelevant after the rewrite. |
| `updateSpeed`: explicit biome speed from the CraftTweaker map; else direct rain `DIRECT_RAIN` -1 when the biome can rain, indirect rain or high humidity 0.25, Nether 2, else 1 plus 0.2 hot, 0.2 dry, -0.2 cold, -0.2 wet; plus 0.2 per fire source within 2 blocks; plus 0.2 when not raining, sky visible, and day time 3000 to 9000; times `SPEED_MODIFIER` (lines 233 to 358) | Same with `Biome#hasPrecipitation` (NF-SRC `net/minecraft/world/level/biome/Biome.java`, line 95), `Tags.Biomes.IS_HOT`, `IS_DRY`, `IS_COLD`, `IS_WET` (NF-SRC `Tags.java`, lines 1062, 1103, 1078, 1099) through `Holder<Biome>#is`, `level.dimension() == Level.NETHER` (NF-SRC `Level.java`, lines 1179 and 82), `BlockTags.FIRE` (NF-SRC `BlockTags.java`, line 97) or `isFireSource` within `BlockPos.betweenClosed`, `canSeeSky` and `getDayTime`. High humidity (1.12 downfall above 0.85) reads `biome.getModifiedClimateSettings().downfall()` (NF-SRC `Biome.java`, lines 438 and 347). The biome map dies with CraftTweaker. |
| `TileDryingRack.update`: `USE_AS_LADDER` climbing by nudging player motion at `CLIMB_SPEED` 0.1 when a normal rack is stacked (`tile/TileDryingRack.java`, lines 110 to 165) | `IBlockExtension#isLadder` (line 181) on the normal rack when stacked above or below, gated by the `USE_AS_LADDER` toggle. Vanilla climbing speed replaces `CLIMB_SPEED` (decision 10). |
| Normal rack: four `Interaction`s on `UP` in a 2 by 2 grid, insert into input or output by which is empty and whether the held item has a recipe (lines 38 to 61, 174 to 206); crude: one interaction on any face against the wall (`TileDryingRackCrude.java` diff, `Interaction` at `AABB_NORTH` rotated by facing) | `useItemOn` and `useWithoutItem` with slot = `floor(localX * 2) + 2 * floor(localZ * 2)` after facing rotation for the normal rack, one slot for the crude. |
| `DryingRackRecipesAdd.registerInheritedRecipes` copying crude recipes scaled by `INHERITED_CRUDE_DRYING_RACK_RECIPE_DURATION_MODIFIER` 1.0 (`init/recipe/DryingRackRecipesAdd.java`, lines 14 to 32) | The crude-to-drying-rack edge of the chain table at multiplier 1 (SIGN-OFFS, issue #21 item 4); `TechBasicRecipes` emits five copies. |

## The soaking pot

| 1.12 construct | 1.21 replacement |
|---|---|
| `FACING_HORIZONTAL` and `PROPERTY_CAMPFIRE`, the latter derived in `getActualState` from a campfire tile below (`block/BlockSoakingPot.java`, lines 38, 132 to 160) | `HORIZONTAL_FACING` plus a `campfire` `BooleanProperty` written in `onPlace` and `updateShape` for `DOWN`; the converted `soaking_pot.json` keys on both. |
| `Material.ROCK`, 3.0 and 5.0, stone, pickaxe 0; box 2 to 14 px by 9 tall, 4 tall over a campfire; side solid only `DOWN` (lines 43 to 55, 167 to 186) | `strength(3.0f, 5.0f).sound(STONE)`, `mineable/pickaxe`, `getShape` by the property. |
| `breakBlock` spawns input and output (lines 76 to 88) | `onRemove`. |
| `InputFluidTank` of `MAX_FLUID_CAPACITY` 4000 with the hot-fluid rule at 450 and `SCPacketParticleCombust` through storage's channel (`tile/TileSoakingPot.java`, lines 579 to 618, 620 to 628) | STORAGE's `HotFluidTank` and the core combust helper. The storage import (line 25) dies. |
| `InputStackHandler(1)` limit `MAX_STACK_SIZE` 8, refused when the output slot is full, when the tank is empty, when no recipe matches item plus fluid, or beyond the count the fluid can pay for (lines 479 to 561); `OutputStackHandler extends LargeObservableStackHandler(1)` at ten stacks (lines 563 to 577) | Input `ItemStackHandler(1)` limit 8 with the same `insertItem`; output as STORAGE's `LargeStackHandler` (one slot, ten stacks). |
| `ejectItemOverfill` when the fluid drops below what the items need (lines 117 to 137) | Same, from the tank's `onContentsChanged`. |
| `update`: first-tick relight; when the recipe requires a campfire, return unless an active campfire is below; progress `1 / time` only when the tank can drain `amount * count`; on complete drain, output times count (lines 257 to 332) | Same server tick reading `level.getBlockEntity(pos.below())` for the campfire's `isLit()`. Relight is a property change. |
| `recipeProgress` synced every 20 ticks (line 95) | Server-only. |
| `InteractionInputFluid` on `UP` (bucket) then `InteractionItem` on `UP`, both with the lowered bounds over a campfire; the item transform drops 5/16 over a campfire (lines 384 to 473) | `useItemOn`: `FluidUtil.interactWithFluidHandler` first, then the item. |
| `SoakingPotFluidRenderer`: quad inset 4 px at `7 px * fill + 1 px`, minus 5 px over a campfire (`client/render/SoakingPotFluidRenderer.java`, lines 29 to 61) | STORAGE's fluid box renderer with those numbers. |
| `SoakingPotRecipe.matches(item, fluid)` by `isFluidEqual` (`recipe/SoakingPotRecipe.java`, lines 85 to 89) | RECIPES' `SoakingPotRecipe` with `SoakingPotRecipeInput(ItemStack, FluidStack)` and `SizedFluidIngredient#test` (NF-SRC `net/neoforged/neoforge/fluids/crafting/SizedFluidIngredient.java`, line 138). Tannin recipes use `SizedFluidIngredient.of(#pyrotech:tannin, n)` (line 107; HUNTING). |

Snapshot: fluid, input, output. Properties: `facing`, `campfire`.

## The tanning rack

| 1.12 construct | 1.21 replacement |
|---|---|
| `FACING_HORIZONTAL`; `Material.WOOD`, 1.0 and 0.2; box 4 to 12 px along the facing axis; `CUTOUT` layer (`block/BlockTanningRack.java`, lines 35 to 43, 100 to 123, 131 to 168) | `HORIZONTAL_FACING`, `strength(1.0f, 0.2f)`, `getShape` per axis, `"render_type": "minecraft:cutout"` in `tanning_rack.json` (ticket #27). |
| `breakBlock` spawns input and output (lines 83 to 95) | `onRemove`. |
| Input and output handlers of one slot, limit 1, input validated by recipe (`tile/TileTanningRack.java`, lines 305 to 341); `recipeProgress` synced; `rainTicks` (lines 39 to 48) | Snapshot: the two stacks. Progress and rain ticks server-only. |
| `update`: only when the sky is visible; rain on top for `RECIPE_RUIN_RAIN_TICKS` 2400 replaces the input with the recipe's rain failure item; else during day time 0 to 12000 progress `1 / time`, on complete output (lines 112 to 165) | Same with `canSeeSky`, `isRainingAt`, `getDayTime`. |
| `InputInteraction` on any face, enabled while the output is empty; `OutputInteraction` extract only (lines 242 to 299) | `useItemOn` inserts when the output is empty; `useWithoutItem` extracts output then input. Both use the north-south box rotated by facing. |
| `TanningRackRecipe`: output, input, nullable rain failure item, time (`recipe/TanningRackRecipe.java`, lines 41 to 88) | A `pyrotech:tanning_rack` recipe class: `Ingredient`, `ItemStack result`, `Optional<ItemStack> rainFailure`, `int time`. |

## The worktables

| 1.12 construct | 1.21 replacement |
|---|---|
| `BlockWorktableBase` with `FACING_HORIZONTAL`, wood 2.0 and 5.0, stone worktable 1.5 and 10.0 (both `Material.WOOD`), side solid on `DOWN`, `EAST`, `WEST`, `SOUTH` (`block/spi/BlockWorktableBase.java`, lines 36 to 41, 150 to 180; `BlockWorktable.java`, line 19; `BlockWorktableStone.java`, line 19) | Two blocks on one class with the numbers as constructor arguments; the converted `worktable.json` and `worktable_stone.json` key on `facing=`. |
| `getDrops` serialising the tile; `harvestBlock` dropping contents then air (lines 61 to 84, 110 to 123) | `dropSelf` and `onRemove` dropping the grid and shelf. The 1.12 serialised drop was never read back (no `ItemBlock` subclass restores it), so nothing is lost. |
| `InputStackHandler(9)` limit `GRID_MAX_STACK_SIZE` 1 (stone 32); `ShelfStackHandler(3)` limit 1 (stone 64); `remainingDurability` synced; `recipeProgress`; `retainedRecipe` id (`tile/TileWorktable.java`, lines 62 to 128, 757 to 793) | Snapshot: grid and shelf. Durability, progress, and the retained recipe id server-only. |
| `InventoryWrapper extends InventoryCrafting` over a dummy `Container` (lines 799 to 852); `WorktableRecipe.getRecipe(inventory, world)` wrapping `CraftingManager.findMatchingRecipe` with whitelist and blacklist (`recipe/WorktableRecipe.java`, lines 40 to 116) | `CraftingInput.of(3, 3, items)` (NF-SRC `net/minecraft/world/item/crafting/CraftingInput.java`, line 32) and `RecipeManager#getRecipeFor(RecipeType.CRAFTING, input, level)` (line 83; `RecipeType.java`, line 8) held as a `RecipeHolder<CraftingRecipe>`. No wrapper class, no cache, no lists (decision 6). |
| `InteractionHammer` on `UP`: sneak plus empty hand clears the grid when `ALLOW_RECIPE_CLEAR`; sneak plus hammer repeats the retained recipe when `ALLOW_RECIPE_REPEAT`, gathering ingredients from the player's inventory and damaging the hammer `RECIPE_REPEAT_TOOL_DAMAGE` 1; else a hammer (or the recipe's tool list) with a non-empty result and the hunger gate advances progress `1 / HITS_PER_CRAFT` (4, stone 2) with `WOOD_HIT`, exhaustion 1 (0.5), progress particles; on complete `firePlayerCraftingEvent`, `getRemainingItems` applied per slot with a spill for mismatches, the result spawned, tool damage 2 (1), durability minus one and break at zero with `ITEM_BREAK` and contents (lines 327 to 664) | Same branch. Crafting completion: `holder.value().assemble(input, registries)` (NF-SRC `net/minecraft/world/item/crafting/Recipe.java`, line 24), `getRemainingItems(input)` (line 33), `ItemStack#onCraftedBy` (NF-SRC `ItemStack.java`, line 687), and `EventHooks.firePlayerCraftingEvent(player, result, container)` (NF-SRC `net/neoforged/neoforge/event/EventHooks.java`, line 926), which wants a `Container`, so wrap the grid in a `TransientCraftingContainer` (NF-SRC `net/minecraft/world/inventory/TransientCraftingContainer.java`, line 20) for that call as `ResultSlot` does (NF-SRC `net/minecraft/world/inventory/ResultSlot.java`, lines 63 to 64). Sounds `WOOD_HIT` and `ITEM_BREAK` (lines 1576 and 766). |
| Nine `InputInteraction`s on `UP` at y 14 to 15 px in thirds, mirrored by facing, refusing hammers and recipe tools, one item per click (lines 673 to 726); three `ShelfInteraction`s on `UP` at y 0 to 5 px in the front third (lines 728 to 751) | `useItemOn` and `useWithoutItem` by local hit: y above 14/16 selects grid slot `(2 - column) + 3 * (2 - row)` after facing rotation; y below 5/16 in the front third selects shelf slot by column. |
| Client crack particles on hit, oak planks for wood and andesite for stone (lines 646 to 671; `TileWorktableStone.java`, lines 60 to 66) | `BlockParticleOption` on the client side of `useItemOn`. |
| `RecipeRepeat.RightClickBlockEventHandler`: for both worktables set `useBlock ALLOW` and `useItem DENY` so a sneaking hammer reaches the block (`event/RecipeRepeat.java`, lines 19 to 39) | `PlayerInteractEvent.RightClickBlock` (NF-SRC `net/neoforged/neoforge/event/entity/player/PlayerInteractEvent.java`, line 163) with `setUseBlock(TriState.TRUE)` and `setUseItem(TriState.FALSE)` (lines 202 and 211). Needed because vanilla skips `useItemOn` for a sneaking player holding an item. Registered unconditionally and tested against the toggle inside. |
| `WorktableRecipe` with tool list, tool damage, and stages, filled only by CraftTweaker (`recipe/WorktableRecipe.java`, lines 226 to 286) | Decision 6: no `pyrotech:worktable` recipe type. |
| `USES_DURABILITY` true (stone false), `DURABILITY` 64 (stone 512) | The toggle stays in `COMMON`; the counts bake. |

## Items

| 1.12 construct | 1.21 replacement |
|---|---|
| `ItemTinder`: `getItemBurnTime` 120; right-click places a campfire in the `NORMAL` variant on the clicked face when `canPlaceBlockAt`, with `GRASS_PLACE` and one tinder consumed (`item/ItemTinder.java`, lines 27 to 73) | A plain `Item` whose `use` (NF-SRC `Item.java`, line 165) ray traces with `getPlayerPOVHitResult` (line 359), tests `level.mayInteract` and the campfire's `canSurvive`, and sets the block. Burn time is a `furnace_fuels` entry written by tech/basic's `DataMapProvider` (NF-SRC `net/neoforged/neoforge/common/data/DataMapProvider.java`, line 139). The `tinder.json` item model on `main` is this ticket's (IGNITION). |
| `ItemBarrelLid`: a plain item (`item/ItemBarrelLid.java`, lines 5 to 9) | `registerSimpleItem`. |
| `ItemMarshmallow extends ItemFood`, always edible, hunger 1, saturation 0.05; `finishUsingItem` adds speed for `MARSHMALLOW_SPEED_DURATION_TICKS` 100 stacked onto an existing speed up to 100; ten-tick cooldown on the four marshmallow items (`item/ItemMarshmallow.java`, lines 30 to 34, 97 to 140) | `Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.05f).alwaysEdible().build())` (NF-SRC `FoodProperties.java`, lines 76, 81, 86, 113), `finishUsingItem` (NF-SRC `Item.java`, line 183) adding `MobEffects.MOVEMENT_SPEED` (NF-SRC `net/minecraft/world/effect/MobEffects.java`, line 16) with `getEffect` (NF-SRC `LivingEntity.java`, line 963), and `player.getCooldowns().addCooldown(item, 10)` (NF-SRC `net/minecraft/world/item/ItemCooldowns.java`, line 43). |
| `ItemMarshmallowRoasted`: stack 1, hunger 2, saturation 0.1, speed 500 stacked to 6000 times potency, where potency decays from 2 to 1 over `ROASTED_MARSHMALLOW_EFFECT_POTENCY_DURATION_TICKS` 600 since `RoastedAtTimestamp`; tooltip "Potency: n%" (`item/ItemMarshmallowRoasted.java`, lines 14 to 42; `ItemMarshmallow.java`, lines 41 to 71, 142 to 145) | Same on a `pyrotech:roasted_at` long component registered with `DeferredRegister.createDataComponents` (NF-DOCS https://docs.neoforged.net/docs/1.21.1/items/datacomponents), `appendHoverText` with the key on `main` (`en_us.json`, line 423), `level.getGameTime()` (NF-SRC `Level.java`, line 998). |
| `ItemMarshmallowBurned`: hunger 1, saturation 0.01, slowness 100 not stacked, server broadcast "ate a burned marshmallow" (`item/ItemMarshmallowBurned.java`, lines 23 to 94) | `MobEffects.MOVEMENT_SLOWDOWN` (line 23); `server.getPlayerList().broadcastSystemMessage(Component.translatable(key, name), false)` (NF-SRC `net/minecraft/server/players/PlayerList.java`, line 798). Keys on `main` (lines 131 to 132). |
| `ItemMarshmallowStickEmpty`: stack 1, durability `MARSHMALLOW_STICK_DURABILITY` 8, sneak plus right-click with marshmallows in the off hand mounts one (`item/ItemMarshmallowStickEmpty.java`, lines 24 to 78) | `Item.Properties().stacksTo(1).durability(8)`; `use` swaps the stack for a full stick carrying the damage and `pyrotech:marshmallow_type` plain. |
| `ItemMarshmallowStick extends ItemFood`: `MarshmallowType` 0, 1, 2 and two timestamps in NBT (lines 182 to 229); `getMaxItemUseDuration` 12000 while a roast timestamp is set else 32; `DRINK` while roasting else `EAT` (lines 100 to 122); unlocalised name by type (lines 126 to 141); `shouldCauseReequipAnimation` by type (lines 144 to 147); `getNBTShareTag` stripping the roast timestamp because a synced NBT change replaced the client stack and interrupted use (lines 151 to 165); a `MethodHandle` setting `activeItemStackUseCount` on the client so the client uses the roast duration before the packet arrives (lines 49 to 69, 280 to 290); the timestamp packet (line 275); `onItemRightClick`: ray trace a campfire within `ROASTING_RANGE_BLOCKS` 2, roll `ROASTING_DURATION_TICKS` 100 with `ROASTING_DURATION_VARIANCE_PERCENTAGE` 0.2, set the roast-by timestamp, start using; else eat; else sneak removes the marshmallow to the off hand, damaging the stick for roasted or burned (lines 237 to 362); `onUsingTick`: stop when the campfire is out of view or range; client flips the type at the timestamps (lines 365 to 407); `onPlayerStoppedUsing`: set roasted or burned with the broadcast (lines 415 to 460); `onItemUseFinish`: eat with the type's effect and return an empty stick, or burn (lines 464 to 545); model predicate `pyrotech:marshmallow_type` (lines 84 to 87) | Three components: `pyrotech:marshmallow_type` (an enum with `StringRepresentable`), `pyrotech:roast_by` and `pyrotech:roasted_at` (longs). `getUseDuration(stack, entity)` (NF-SRC `Item.java`, line 321) returns 12000 when the entity is looking at a campfire block entity within range and the stack is a plain marshmallow, else 32, so client and server agree without a packet; `getUseAnimation` (line 317) with `UseAnim.DRINK` and `EAT` (NF-SRC `net/minecraft/world/item/UseAnim.java`, lines 6 and 5); `onUseTick` (line 126), `releaseUsing` (line 329), `finishUsingItem` (line 183), `stopUsingItem` (NF-SRC `LivingEntity.java`, line 3318). The reflection hack and `getNBTShareTag` die: `updatingUsingItem` keeps the use going while `canContinueUsing` holds, which tests item identity only (NF-SRC `LivingEntity.java`, lines 3137 to 3148; `IItemExtension.java`, lines 572 to 576), so a component write mid-use does not interrupt. `shouldCauseReequipAnimation` stays (`IItemExtension.java`, line 524). Names: `Item#getName(ItemStack)` (NF-SRC `Item.java`, line 339) picking the three keys on `main` (`en_us.json`, lines 111 to 113). Model: `ItemProperties.register(item, "pyrotech:marshmallow_type", ...)` (NF-SRC `net/minecraft/client/renderer/item/ItemProperties.java`, line 60); the converted `marshmallow_stick.json` overrides on that predicate (ASSETS flags it). |
| The core crafting factory `pyrotech:marshmallow_stick`: shapeless `toolSharp` plus `stickWood` giving an empty stick, damaging the sharp tool by one (`modules/core/recipe/MarshmallowStickRecipe.java`, lines 24 to 56; `recipes/tech/basic/marshmallow_stick.json`) | Hunting's `pyrotech:tool_damage_shapeless` serializer (HUNTING decision 3) with `#pyrotech:knives` (hunting's tag) and `#c:rods/wooden` (NF-SRC `Tags.java`, line 720). `toolSharp` was registered by `JsonOreDict` (`modules/core/init/JsonOreDict.java`, line 103), which CORE drops. Rehomed from core (findings). |
| The core crafting factory `pyrotech:chopping_block`: shapeless `logWood` plus `toolAxe`, damaging the axe (`modules/core/recipe/ChoppingBlockRecipe.java`, lines 23 to 57; `recipes/tech/basic/chopping_block.json`) | The same serializer with `#minecraft:logs_that_burn` and `#minecraft:axes` (NF-SRC `ItemTags.java`, line 129). Rehomed from core (findings). |

## The focused player data

`IFocusedPlayerData` holds one double, `remainingBonus`, clamped to
`FOCUSED_MAXIMUM_ACCUMULATED_BONUS` 1.5, and a dirty flag (`capability/FocusedPlayerData.java`, lines 15
to 41). It is attached to every non-fake player in `AttachCapabilitiesEvent` (`event/CampfireFocusEffectEventHandler.java`,
lines 101 to 149), saved as NBT (lines 43 to 56), and pushed to the owning client by
`SCPacketCapabilitySyncFocused` whenever it is dirty and the player has the focused effect (lines 80 to
98; `network/SCPacketCapabilitySyncFocused.java`, lines 17 to 47). The client reads it to draw the
focused bar (`potion/PotionFocused.java`, lines 65 and 88).

| 1.12 construct | 1.21 replacement |
|---|---|
| `CapabilityManager` registration, `@CapabilityInject`, the `Provider`, `AttachCapabilitiesEvent` | One `AttachmentType<CampfireEffectData>` registered on `NeoForgeRegistries.Keys.ATTACHMENT_TYPES` (NF-SRC `net/neoforged/neoforge/registries/NeoForgeRegistries.java`, line 58) with `AttachmentType.builder(...).serialize(codec).sync((holder, to) -> holder == to, streamCodec).build()` (NF-SRC `net/neoforged/neoforge/attachment/AttachmentType.java`, lines 95, 184, 273, 294; NF-DOCS https://docs.neoforged.net/docs/1.21.1/datastorage/attachments). Read with `player.getData(type)` and `setData` (NF-SRC `net/neoforged/neoforge/attachment/IAttachmentHolder.java`, lines 39 and 46). |
| The dirty flag and the packet | `syncData(type)` after a change (NF-SRC `IAttachmentHolder.java`, line 125). The predicate sends only to the owning player, as the 1.12 packet did. |
| Not copied on death: 1.12 registers no `PlayerEvent.Clone` handler, so a respawned player starts at zero | No `copyOnDeath()` (line 224). Faithful. |
| Data shape: `remainingBonus` | A record `CampfireEffectData(double remainingBonus, int restingTicks)`. The second field replaces the elapsed-tick counter that 1.12 derived from the resting effect's duration (effects section). |

## Effects

Five `Potion` subclasses over `PotionCampfireBase`, all beneficial with a black liquid colour and a
custom 18 px icon drawn in the inventory and HUD (`potion/PotionCampfireBase.java`, lines 19 to 46).
The client-side `CampfireEffectDurationFix` marks comfort, resting, and focused as max-duration every
tick so the HUD shows `**:**` (`event/CampfireEffectDurationFix.java`, lines 11 to 39).

| 1.12 construct | 1.21 replacement |
|---|---|
| `registry.registerPotion` times five (`init/PotionInitializer.java`, lines 10 to 14) | `DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID)` with `new MobEffect(MobEffectCategory.BENEFICIAL, 0)` subclasses (NF-SRC `net/minecraft/world/effect/MobEffect.java`, line 55; `MobEffectCategory.java`, line 6; NF-DOCS https://docs.neoforged.net/docs/1.21.1/items/mobeffects). Description ids default to `effect.pyrotech.<name>`; the lang keys on `main` are the 1.12 `pyrotech.effect.comfort`, `.well.fed`, `.resting`, `.well.rested`, `.focused` (`en_us.json`, lines 133 to 137), so either rename the keys or override `getDescriptionId` (line 115). Rename. |
| `renderInventoryEffect` and `renderHUDEffect` drawing `textures/potions/<name>.png`; resting picks `resting`, `resting2`, `resting3` by amplifier and draws a level-up bar; focused draws a bonus bar from the capability (`PotionCampfireBase.java`, lines 27 to 46; `PotionResting.java`, lines 159 to 231; `PotionFocused.java`, lines 51 to 94) | `IClientMobEffectExtensions#renderInventoryIcon` and `renderGuiIcon` (NF-SRC `net/neoforged/neoforge/client/extensions/common/IClientMobEffectExtensions.java`, lines 62 and 94) registered in `RegisterClientExtensionsEvent#registerMobEffect` (NF-SRC `net/neoforged/neoforge/client/extensions/common/RegisterClientExtensionsEvent.java`, line 78). The seven textures are on `main` (`textures/potions/`). The bars read the synced attachment. |
| `Short.MAX_VALUE` durations plus the client duration fix | `MobEffectInstance.INFINITE_DURATION` (NF-SRC `net/minecraft/world/effect/MobEffectInstance.java`, line 30), which the HUD formats as infinite (NF-SRC `net/minecraft/world/effect/MobEffectUtil.java`, lines 17 to 18). The fix handler dies. Consequence: `PotionResting` reads elapsed ticks as `Short.MAX_VALUE - duration` for its regen interval, level-up interval, and bar (`PotionResting.java`, lines 73 to 76, 133, 188, 225), and `PotionFocused.isReady` tests `duration % 20` (line 46); with `INFINITE_DURATION` the duration handed to `shouldApplyEffectTickThisTick` (NF-SRC `MobEffect.java`, line 82) is always -1. The `restingTicks` counter in the attachment replaces it (decision 2). |
| `PotionComfort`: no tick, no curative items (`PotionComfort.java`, lines 33 to 43) | `shouldApplyEffectTickThisTick` false; `fillEffectCures` cleared (NF-SRC `net/neoforged/neoforge/common/extensions/IMobEffectExtension.java`, line 23) so milk does not cure it. Well fed, well rested, and focused keep the default cures, as 1.12 left `getCurativeItems` at its milk default for them. |
| `PotionResting.performEffect` every tick on the server: heal `RESTING_REGEN_HALF_HEARTS` 1 every `RESTING_REGEN_INTERVAL_TICKS` 100 below max health; every `RESTING_LEVEL_UP_INTERVAL_TICKS` 200 raise the amplifier to at most 2; at amplifier 2 grant well rested for `WELL_RESTED_DURATION_TICKS` 6000 when enabled, refresh resting, and when comfort, well rested, and well fed are all present grant focused and add `FOCUSED_ACCUMULATED_BONUS` 0.05 to the capability (`PotionResting.java`, lines 63 to 149) | `applyEffectTick` (NF-SRC `MobEffect.java`, line 74) with `shouldApplyEffectTickThisTick` true, counting `restingTicks` in the attachment; `heal` (NF-SRC `LivingEntity.java`, line 1117); amplifier raise by `addEffect` with a new instance (the 1.12 `addPotionEffect` upgrade semantics are `MobEffectInstance#update`, line 138); `hasEffect` (line 958). |
| `PotionWellRested`: 4 absorption half hearts added on apply and removed on remove (`PotionWellRested.java`, lines 39 to 52) | The vanilla absorption shape: `addAttributeModifier(Attributes.MAX_ABSORPTION, id, 4, ADD_VALUE)` (NF-SRC `MobEffect.java`, line 131; `net/minecraft/world/entity/ai/attributes/Attributes.java`, line 96) plus `onEffectStarted` setting `setAbsorptionAmount(current + 4)` (NF-SRC `MobEffect.java`, line 86; `LivingEntity.java`, line 3109), as `AbsorptionMobEffect` does (NF-SRC `net/minecraft/world/effect/AbsorptionMobEffect.java`, lines 21 to 23). Removal drops the modifier through `LivingEntity#onEffectRemoved` (line 1076) and vanilla clamps absorption to the new maximum. |
| `PotionFocused.performEffect` re-adding itself every 20 ticks to keep it alive (`PotionFocused.java`, lines 38 to 47) | Unnecessary with infinite duration. The effect is removed only by the XP handler when the bonus runs out. |
| `PotionWellFed`: no tick (`PotionWellFed.java`, lines 29 to 32) | `shouldApplyEffectTickThisTick` false. |

The handlers:

| 1.12 handler | 1.21 replacement |
|---|---|
| `CampfireComfortEffectEventHandler.on(RightClickItem)`: for food that is not always edible, when the player cannot eat and has comfort, run the item's right-click on both sides, force `setActiveHand` on `FAIL`, and cancel (`event/CampfireComfortEffectEventHandler.java`, lines 28 to 60 reflection, 63 to 158) | `PlayerInteractEvent.RightClickItem` (NF-SRC `PlayerInteractEvent.java`, line 249): when the stack has `FOOD` whose `canAlwaysEat` (NF-SRC `FoodProperties.java`, line 18) is false, `!player.canEat(false)` (NF-SRC `Player.java`, lines 1809 to 1811), and the player has comfort, call `player.startUsingItem(hand)` (NF-SRC `LivingEntity.java`, line 3192), set the cancellation result to `sidedSuccess`, and cancel. `Item#use` refuses with `fail` in exactly this case (NF-SRC `Item.java`, lines 169 to 174), so the handler runs first. No reflection: the record field is public. |
| `on(LivingEntityUseItemEvent.Finish)`: with comfort, add `heal * COMFORT_HUNGER_MODIFIER` and `saturation * COMFORT_SATURATION_MODIFIER` through `addStats` (lines 161 to 188) | `LivingEntityUseItemEvent.Finish` (NF-SRC `net/neoforged/neoforge/event/entity/living/LivingEntityUseItemEvent.java`, line 115) with `FoodData#eat(int, float)` (NF-SRC `FoodData.java`, line 28), whose second argument is a saturation modifier as in 1.12. |
| `CampfireRestingEffectEventHandler`: `PlayerTickEvent` end phase resets resting to level one when core's `PlayerMovementTracker.getTicksSinceLastMove` is zero; `LivingDamageEvent` resets the victim and strips well rested when absorption hits zero; `AttackEntityEvent` and `LivingHurtEvent` reset the attacker (`event/CampfireRestingEffectEventHandler.java`, lines 21 to 117) | `PlayerTickEvent.Post` (NF-SRC `net/neoforged/neoforge/event/tick/PlayerTickEvent.java`, line 52) reading core's tracker (`modules/core/event/PlayerMovementTracker.java`, lines 50 to 53; CORE keeps it), `LivingDamageEvent.Post` (NF-SRC `net/neoforged/neoforge/event/entity/living/LivingDamageEvent.java`, line 97) for the victim and, through `getSource().getEntity()`, the attacker, and `AttackEntityEvent` (NF-SRC `net/neoforged/neoforge/event/entity/player/AttackEntityEvent.java`, line 27). Resetting also zeroes `restingTicks`. |
| `CampfireWellFedEffectEventHandler`: `Finish` grants well fed for 6000 ticks when comfort is on and saturation is exactly 20; `PlayerTickEvent` start halves exhaustion gains through reflection on `foodExhaustionLevel` (`event/CampfireWellFedEffectEventHandler.java`, lines 28 to 60 reflection, 72 to 147) | `Finish` with `getSaturationLevel` (NF-SRC `FoodData.java`, line 126); `PlayerTickEvent.Pre` (line 39) with `getExhaustionLevel` and `setExhaustion` (lines 122 and 138), both public. The per-player previous-exhaustion map becomes a field on the attachment or stays a static map; static is simpler and 1.12 did it. |
| `CampfireFocusEffectEventHandler.on(PlayerPickupXpEvent)` at highest priority: multiply the orb's `xpValue` by `1 + FOCUSED_BONUS`, subtract `xp / xpToNextLevel` from the bonus, remove focused when the bonus is spent (`event/CampfireFocusEffectEventHandler.java`, lines 34 to 77) | `PlayerXpEvent.XpChange` (NF-SRC `net/neoforged/neoforge/event/entity/player/PlayerXpEvent.java`, line 46), which fires inside `giveExperiencePoints` (NF-SRC `Player.java`, lines 1718 to 1720) and offers `setAmount` (line 58). `PickupXp` (line 29) exposes the orb but `ExperienceOrb.value` has no setter (NF-SRC `net/minecraft/world/entity/ExperienceOrb.java`, line 277 is the getter). Decision 11. `getXpNeededForNextLevel` (NF-SRC `Player.java`, line 1782). |
| `on(PlayerTickEvent)` syncing the dirty capability (lines 80 to 98) | `syncData` at the write sites (attachment section). |
| `on(AttachCapabilitiesEvent)` (lines 101 to 108) | The attachment type's default value; no event. |
| `CampfireEffectTracker`: every 10 server ticks, for each player drop tracked campfires that are gone, inactive, or out of range; outside the 12000 to 23000 window or with no tracked campfire, remove comfort and resting (`event/CampfireEffectTracker.java`, lines 26 to 130) | `ServerTickEvent.Pre` (NF-SRC `net/neoforged/neoforge/event/tick/ServerTickEvent.java`, line 48) over `server.getPlayerList().getPlayers()` (NF-SRC `PlayerList.java`, line 899). The static `Map<UUID, Set<BlockPos>>` stays static; it is transient by design. `removeEffect` (NF-SRC `LivingEntity.java`, line 1035). |
| `CampfireEffectDurationFix` (`event/CampfireEffectDurationFix.java`) | Dropped (infinite durations). |
| `RecipeRepeat` and `TooltipEventHandler` | Covered under the worktable and the compost bin. |

## Recipes and datagen

The twelve 1.12 recipe classes onto the 1.21 classes. RECIPES' table covers the first four; the
remaining shapes are new and follow the same pattern (constructor-argument type and serializer, map
codec plus stream codec, one `RecipeInput`).

| 1.12 class | 1.21 class | Type id | Input |
|---|---|---|---|
| `KilnPitRecipe` (`recipe/KilnPitRecipe.java`, lines 38 to 88) | RECIPES `KilnRecipe` | `pyrotech:pit_kiln` | `SingleRecipeInput` |
| `CrudeDryingRackRecipe`, `DryingRackRecipe`, `DryingRackRecipeBase` (`recipe/spi/DryingRackRecipeBase.java`, lines 14 to 44) | RECIPES `DryingRecipe` | `pyrotech:crude_drying_rack`, `pyrotech:drying_rack` | `SingleRecipeInput` |
| `ChoppingBlockRecipe` (`recipe/ChoppingBlockRecipe.java`, lines 35 to 82) | RECIPES `ChoppingBlockRecipe` with optional `chops` and `quantities` | `pyrotech:chopping_block` | `SingleRecipeInput` |
| `AnvilRecipe` (`recipe/AnvilRecipe.java`, lines 105 to 171) | RECIPES `AnvilRecipe`; `ExtendedAnvilRecipe` interface for the hooks; bloomery's second serializer (SIGN-OFFS, issue #21 item 3) | `pyrotech:anvil` | `AnvilRecipeInput(ItemStack, AnvilTier, ToolType)` |
| `SoakingPotRecipe` (`recipe/SoakingPotRecipe.java`, lines 38 to 89) | RECIPES `SoakingPotRecipe` | `pyrotech:soaking_pot` | `SoakingPotRecipeInput(ItemStack, FluidStack)` |
| `CompactingBinRecipe` over `CompactingBinRecipeBase` (`library/CompactingBinRecipeBase.java`, lines 12 to 53) | New `CompactingBinRecipe`: `Ingredient input`, `ItemStack result`, `int amount`, optional `List<Integer> toolUses` defaulting to `{4, 3, 2, 1}` | `pyrotech:compacting_bin`; machine adds `pyrotech:mechanical_compacting_bin` on the same class | `SingleRecipeInput` |
| `BarrelRecipe` (`recipe/BarrelRecipe.java`, lines 51 to 135) | New `BarrelRecipe`: `List<Ingredient> items` (four), `SizedFluidIngredient fluid`, `FluidStack result`, `int time`; `matches` is the 1.12 bitmask over the four slots (lines 102 to 135) | `pyrotech:barrel` | `BarrelRecipeInput(List<ItemStack>, FluidStack)` |
| `TanningRackRecipe` (`recipe/TanningRackRecipe.java`, lines 41 to 88) | New `TanningRackRecipe`: `Ingredient`, `ItemStack result`, `Optional<ItemStack> rainFailure`, `int time` | `pyrotech:tanning_rack` | `SingleRecipeInput` |
| `CampfireRecipe` (`recipe/CampfireRecipe.java`, lines 179 to 212) | New `CampfireRecipe`: `Ingredient`, `ItemStack result`, `int ticks`; the derived smelting list is not a recipe (campfire section, decision 4) | `pyrotech:campfire` | `SingleRecipeInput` |
| `CompostBinRecipe` over `CompostBinRecipeBase` (`library/CompostBinRecipeBase.java`, lines 15 to 91) | Decision 3: `CompostBinRecipe` (`Ingredient`, `int value`, `ItemStack result`) or an item data map | `pyrotech:compost_bin` | `SingleRecipeInput` |
| `WorktableRecipe` (`recipe/WorktableRecipe.java`) | None (decision 6) | | |

Every 1.12 `removeRecipes` and `RecipeHelper.removeRecipesByOutput` (for example `recipe/AnvilRecipe.java`,
lines 100 to 103) drops; datapacks override recipe files instead (RECIPES). Every `getTimeTicks`
multiplying by a `BASE_RECIPE_DURATION_MODIFIER` (`recipe/KilnPitRecipe.java`, lines 69 to 73, and the
barrel, soaking pot, tanning rack, and two drying racks) moves into the block entity, which multiplies
the plain JSON time by the `SERVER` config value (SIGN-OFFS, issue #21 items 4 and 5).

The thirteen adders become methods of `TechBasicRecipes` under RECIPES' `PyrotechRecipeProvider`,
emitting `pyrotech:<machine>/<name>` ids. Hard-coded groups and counts:

| Adder | Recipes | Notes |
|---|---|---|
| `AnvilGraniteRecipesAdd` (`init/recipe/AnvilGraniteRecipesAdd.java`, lines 26 to 255 hammer, 257 to 517 pickaxe) | 50: 25 hammer, 25 pickaxe | `OreIngredient` inputs become `Tags.Items.CROPS_WHEAT`, `NUGGETS_IRON`, `NUGGETS_GOLD` (NF-SRC `Tags.java`, lines 383, 614, 613) and core tags for `rockLimestone`, `blockFuelCoke`, `blockCharcoal`. `stone_slab` (lines 355 to 363) outputs `minecraft:smooth_stone_slab` (SIGN-OFFS, issue #33 item 5); `brick_stone` (lines 287 to 295) takes `stone_brick_slab`; `sandstone_slab` and `red_sandstone_slab` accept the three sandstone variants each; `quartz_slab` the three quartz blocks; the anvil recipe `G` key is 1.12 stone meta 2, polished granite (`recipes/tech/basic/anvil_granite.json`). Decision 1 covers `stone_slab`. |
| `AnvilIroncladRecipesAdd`, `AnvilObsidianRecipesAdd` | 0 own, 100 inherited | Chain table edges. |
| `BarrelRecipesAdd` (lines 18 to 69) | 4: tannin (hunting-guarded), three wines | Tannin: four `#minecraft:leaves` (NF-SRC `ItemTags.java`, line 46) in water for 12000 ticks. Hunting hoists first, so tech/basic writes it. |
| `CampfireRecipesAdd` (lines 16 to 25) | 1, Patchouli-guarded | Handed to the book ticket (findings). |
| `ChoppingBlockRecipesAdd` (lines 24 to 78) | 0 hard-coded; 11 in 1.21 from the log tags | Chopping block section. |
| `CompactingBinRecipesAdd` (lines 19 to 157) | 16 | `netherrack` 8 (core `rock_netherrack`), `ash_pile` 8, `lapis_block` 9, `redstone_block` 9, `charcoal_block_from_flakes` 72, `gravel` 8 from four rock kinds, `dirt` 8, `mud` 8, `charcoal_block` 9, `sand` 8, `sand_red` 8, `grass` 8 (core `rock_grass`), `clay` 4, `snow` 4, `bone_block` 8, `pile_wood_chips` 8. |
| `CompostBinRecipesAdd` (lines 26 to 115) | 56 explicit plus the food rule | 12 vanilla items, pumpkin at 4, 7 plants, 6 leaves, 6 tall plants, dandelion, 9 flowers, 2 grasses, 6 saplings, 6 Pyrotech items. In 1.21 the leaves, saplings, and flowers collapse to `#minecraft:leaves`, `#minecraft:saplings`, `#minecraft:small_flowers`, `#minecraft:tall_flowers` (NF-SRC `ItemTags.java`, lines 46, 23, 48, 51), which also cover the post-1.12 additions (DATA `data/minecraft/tags/item/small_flowers.json` lists 14). |
| `CrudeDryingRackRecipesAdd` (lines 15 to 51) | 5 | `straw` 14400, `plant_fibers_dried` 9600, `plant_fibers_dried_from_sapling` 12000 on `#minecraft:saplings`, `sponge` 9600 from wet sponge, `paper` 6000. |
| `DryingRackRecipesAdd` (lines 20 to 32) | 0 own, 5 inherited | Chain table edge. |
| `PitKilnRecipesAdd` (lines 24 to 226) | 29 | `bucket_clay` and `bucket_refractory` (bucket items, BUCKET), `clay_shears` (tool item, TOOL), `brick`, `cob`, `charcoal_flakes`, `stone_slab` (cobblestone slab to `smooth_stone_slab`), `stone`, four stone variants from core's cobbled variants, `hardened_clay` (clay to terracotta), 16 glazed terracottas from the coloured terracottas. All at `Reference.PitKiln.DEFAULT_BURN_TIME_TICKS` 16800 and `DEFAULT_FAILURE_CHANCE` 0.33 (`Reference.java`, lines 25 to 26). Bucket and tool hoist before tech/basic, so tech/basic writes all 29. |
| `SoakingPotRecipesAdd` (lines 24 to 309) | 17 own, 12 to refractory | Own: `clay_blasting`, `dough` (`dustWheat` is core's flour item), `hide_tanned`, `hide_small_tanned`, `hide_small_washed`, `hide_washed` (hunting items and `#pyrotech:tannin`), `sponge`, `mud`, `mud_clump`, `flint_clay`, `pulp_from_reeds`, `pulp_from_wood_chips`, `slaked_lime`, `podzol` (coarse dirt), `mossy_stone_bricks`, `mossy_cobblestone`, `white_wool` (any `#minecraft:wool`). Scope correction lists the twelve. |
| `TanningRackRecipesAdd` (lines 16 to 34) | 2, hunting-guarded | `leather` from tanned hide with washed hide as rain failure; `leather_small`. Tech/basic writes them. |
| Crafting JSON (`recipes/tech/basic/*.json`) | 19 | Seventeen shaped or shapeless through `ShapedRecipeBuilder` and `ShapelessRecipeBuilder` (NF-SRC `net/minecraft/data/recipes/ShapedRecipeBuilder.java`, lines 50 and 126; `ShapelessRecipeBuilder.java`, line 45), two through the tool-damage serializer. Tags: `ingotIron` to `#c:ingots/iron` (NF-SRC `Tags.java`, line 593), `plankWood` `#minecraft:planks`, `slabWood` `#minecraft:wooden_slabs`, `stickWood` `#c:rods/wooden`, `logWood` `#minecraft:logs_that_burn`, `milk` `#c:buckets/milk` (line 339), `twine` core's item. `dried_plant_fibers_from_pit_kiln` uncrafts a kiln into two dried fibers. |

Totals: 209 emitted plus 105 inherited copies inside the unit, 12 handed to refractory, 1 to the book
ticket. Tech/basic is the parent side of four cross-unit chains in RECIPES' table: pit kiln to stone
kiln (machine, `modules/tech/machine/init/recipe/StoneKilnRecipesAdd.java`, lines 26 to 36 and 106 to
115), drying rack to stone oven (`StoneOvenRecipesAdd.java`, lines 20 to 43), chopping block to stone
sawmill (`StoneSawmillRecipesAdd.java`, lines 46 to 70), compacting bin to mechanical compacting bin
(`MechanicalCompactingBinRecipesAdd.java`, lines 59 to 64), and the internal crude drying rack to
drying rack and granite to ironclad to obsidian edges.

Placement under the last-hoisted-unit rule: every recipe above whose inputs and results are core,
tool, bucket, hunting, or tech/basic items is tech/basic's. The twelve tar recipes are refractory's.
Bloomery's slag compacting recipes (`modules/tech/bloomery/init/recipe/CompactingBinRecipesAdd.java`,
lines 17 to 43) and machine's three mechanical compacting recipes are theirs (findings).

Tags tech/basic's datagen writes: `#pyrotech:campfire_fuels` (decision 5), `#pyrotech:campfire_blacklist`
(bread, cookie), and the mineable tags for its fifteen blocks. Data maps: `furnace_fuels` with `tinder`
120 and the two `remove` entries CORE assigned here (`campfire`, `worktable_stone`, from
`modules/core/event/FurnaceFuelBurnTimeEventHandler.java`, lines 34 to 41); note that neither item is a
fuel in 21.1 unless tagged, so the removes are no-ops kept for fidelity. Loot: `dropSelf` for the
fifteen blocks with the state conditions and component copies described per block.

## Network payloads

| 1.12 packet | Fate |
|---|---|
| `SCPacketParticleAnvilHit(pos, hitX, hitY, hitZ)`: eight block-crack particles at the hit, then resolve the recipe client-side from the anvil's stack and the held tool and call `onAnvilHitClient` (`network/SCPacketParticleAnvilHit.java`, lines 25 to 93) | One payload record `AnvilHitPayload(BlockPos, Vec3)` on core's registrar with `playToClient` (NF-SRC `net/neoforged/neoforge/network/registration/PayloadRegistrar.java`, line 44), sent with `PacketDistributor.sendToPlayersTrackingChunk` (NF-SRC `net/neoforged/neoforge/network/PacketDistributor.java`, line 113). The crack particles alone could be `sendParticles`, but the bloom hook spawns its own lava particles client-side (`modules/tech/bloomery/recipe/BloomAnvilRecipe.java`, lines 148 to 160), so the client callback stays. Tech/basic's one payload. |
| `SCPacketCapabilitySyncFocused` (`network/SCPacketCapabilitySyncFocused.java`) | Dissolves into the attachment's sync (focused section). |
| `SCPacketMarshmallowStickTimestamp` (`network/SCPacketMarshmallowStickTimestamp.java`, lines 21 to 75) | Dissolves: the component syncs with the stack and does not interrupt use (items section). |
| Athenaeum's mouse-wheel packet | The shared scroll payload STORAGE proposed core owns; the campfire, pit kiln logs, compacting bin, and compost bin dispatch to it. |

## Config

`ModuleTechBasicConfig` is 1,680 lines. The principle from CORE: behaviour toggles in `COMMON`,
multipliers in `SERVER` (SIGN-OFFS, issue #21 item 5), item- and recipe-describing numbers in data
or constants. Fifteen `STAGES_*` fields (lines 26 to 69) die with gamestages. Lines cited are the
field declarations.

| Category | Stays in config | Becomes data | Dies or bakes |
|---|---|---|---|
| `WORKTABLE_COMMON` (75 to 128) | `ALLOW_RECIPE_CLEAR` false, `ALLOW_RECIPE_REPEAT` false, `RECIPE_REPEAT_TOOL_DAMAGE` 1 (`COMMON`) | | `RECIPE_WHITELIST`, `RECIPE_BLACKLIST` die (decision 6). |
| `WORKTABLE`, `STONE_WORKTABLE` (134 to 274) | `USES_DURABILITY` true and false (`COMMON`) | | `HITS_PER_CRAFT` 4 and 2, `TOOL_DAMAGE_PER_CRAFT` 2 and 1, grid and shelf stack sizes 1, 1, 32, 64, `DURABILITY` 64 and 512, exhaustion 1 and 0.5 per hit, 0 per craft, `MINIMUM_HUNGER_TO_USE` 3 bake as constructor arguments. |
| `COMPOST_BIN` (280 to 364) | `COMPOST_DURATION_TICKS` 96000 (`SERVER`, the bin's only duration knob); `SHOW_COMPOST_VALUE_IN_TOOLTIPS` joins CORE's client config | `AUTO_CREATE_RECIPES_FROM_FOOD` becomes the runtime food rule (decision 3) | `ALLOW_AUTOMATION`, `SHOVEL_WHITELIST`, `SHOVEL_BLACKLIST` die; `MAXIMUM_OUTPUT_ITEM_CAPACITY` 16, `COMPOST_VALUE_REQUIRED_PER_OUTPUT_ITEM` 16, `GENERATED_FOOD_COMPOST_VALUE_RANGE` {1, 8}, `ADDITIVE_PERCENTILE_SPEED_MODIFIER_PER_LAYER` 0.2, `MOISTURE_EVAPORATION_RATE_MILLIBUCKETS_PER_TICK` {1, 48} bake. |
| `COMPACTING_BIN` (370 to 462) | | `TOOL_USES_REQUIRED_PER_HARVEST_LEVEL` {4, 3, 2, 1} is the recipe field default; `JEI_HARVEST_LEVEL_ITEM` a JEI constant | `ALLOW_AUTOMATION`, the shovel lists die; `MAX_CAPACITY` 4, `TOOL_DAMAGE_PER_CRAFT` 1, exhaustion 1 and 0, hunger 3 bake. |
| `TANNING_RACK` (469 to 488) | `BASE_RECIPE_DURATION_MODIFIER` 1 (`SERVER`); `RECIPE_RUIN_RAIN_TICKS` 2400 with -1 disabling (`COMMON`) | | |
| `BARREL` (494 to 529) | `BASE_RECIPE_DURATION_MODIFIER` 1 (`SERVER`) | | `ALLOW_AUTOMATION` dies; `RAIN_WATER_FILL_DURATION_TICKS` 20, `RAIN_WATER_CONVERSION_DURATION_TICKS` 2400 bake. |
| `SOAKING_POT` (535 to 581) | `BASE_RECIPE_DURATION_MODIFIER` 1 (`SERVER`) | | `ALLOW_AUTOMATION` dies; `MAX_STACK_SIZE` 8, `MAX_FLUID_CAPACITY` 4000, `HOT_TEMPERATURE` 450, `HOLDS_HOT_FLUIDS` false bake. |
| `ANVIL_COMMON` (587 to 654) | `HIT_REDUCTION_PER_HAMMER_HARVEST_LEVEL` {0, 1, 2, 3} (`COMMON`, per issue #21 item 4) | `JEI_HARVEST_LEVEL_PICKAXE` a JEI constant | `PICKAXE_WHITELIST`, `PICKAXE_BLACKLIST` die (`PICKAXE_DIG`). |
| `GRANITE_ANVIL`, `IRONCLAD_ANVIL`, `OBSIDIAN_ANVIL` (660 to 875) | `USE_DURABILITY` true times three (`COMMON`) | | `ALLOW_AUTOMATION`, `INHERIT_*`, `INHERITED_*_HIT_MODIFIER` die; `HITS_PER_DAMAGE` 64, 256, 2048, `BLOOM_EXTRA_DAMAGE_PER_HIT` 1, 1, 0, `BLOOM_EXTRA_DAMAGE_CHANCE` 0.5, 0.05, 0, exhaustion 0.5, 0.5, 0.25 per hit and 0 per craft, hunger 3 bake as constructor arguments. |
| `CHOPPING_BLOCK` (881 to 1003) | `USES_DURABILITY` true (`COMMON`) | `CHOPS_REQUIRED_PER_HARVEST_LEVEL` {6, 4, 2, 2} and `RECIPE_RESULT_QUANTITY_PER_HARVEST_LEVEL` {1, 2, 3, 4} are recipe field defaults; `JEI_HARVEST_LEVEL_ITEM` a JEI constant | `ALLOW_AUTOMATION`, the axe lists die; `WOOD_CHIPS_CHANCE` 0.05, `CHOPS_PER_DAMAGE` 16, exhaustion 1.5, 0.5, 0, hunger 3 bake. |
| `DryingRackConditionalModifiers` times two (1009 to 1078, 1106, 1160) | All eleven per rack (`SERVER`, per issue #21 item 4): `DIRECT_RAIN` -1, `INDIRECT_RAIN` 0.25, `NETHER` 2, `BASE_DERIVED` 1, `DERIVED_HOT` 0.2, `DERIVED_DRY` 0.2, `DERIVED_COLD` -0.2, `DERIVED_WET` -0.2, `FIRE_SOURCE_BONUS_RANGE` 2, `FIRE_SOURCE_BONUS` 0.2, `DAYTIME` 0.2 | | |
| `CRUDE_DRYING_RACK`, `DRYING_RACK` (1084 to 1161) | `SPEED_MODIFIER` 1.0 and 1.35, `BASE_RECIPE_DURATION_MODIFIER` 1 and 1 (`SERVER`); `USE_AS_LADDER` true (`COMMON`) | | `BIOME_MODIFIERS` (CraftTweaker), `INHERIT_CRUDE_DRYING_RACK_RECIPES`, `INHERITED_*_DURATION_MODIFIER` die; `CLIMB_SPEED` 0.1 dies (decision 10). |
| `PIT_KILN` (1167 to 1212) | `BASE_RECIPE_DURATION_MODIFIER` 1, `VARIABLE_SPEED_MODIFIER` 0.5 (`SERVER`); `EXTINGUISHED_BY_RAIN` true (`COMMON`) | | `MAX_STACK_SIZE` 8, `TICKS_BEFORE_EXTINGUISHED` 200 bake. |
| `CAMPFIRE` (1218 to 1364) | `COOK_TIME_TICKS` 1800 (`SERVER`, the derived recipes' time, which JEI shows); `EXTINGUISHED_BY_RAIN` true (`COMMON`) | `USE_LOG_WOOD_OREDICT`, `CAMPFIRE_FUEL_WHITELIST`, `CAMPFIRE_FUEL_BLACKLIST` become `#pyrotech:campfire_fuels`; `RECIPE_BLACKLIST` becomes `#pyrotech:campfire_blacklist` | `ALLOW_AUTOMATION` dies; `MAXIMUM_LIGHT_LEVEL` 11, `MINIMUM_LIGHT_LEVEL` 3, `BURN_TIME_TICKS_PER_LOG` 2400, `BURNED_FOOD_TICKS` 600, `FUEL_LEVEL_FOR_FULL_COOK_SPEED` 4, `TICKS_BEFORE_EXTINGUISHED` 200, `ASH_CHANCE` 0.25, `PLAYER_BURN_CHANCE` 0.5, `PLAYER_LOG_BURN_DAMAGE` 1, `ENTITY_WALK_BURN_DAMAGE` 1 bake. |
| `CAMPFIRE_MARSHMALLOWS` (1366 to 1511) | `ENABLE_BURNED_MARSHMALLOW_BROADCAST_MESSAGE`, `ENABLE_BURNED_MARSHMALLOW_EAT_BROADCAST_MESSAGE` true (`COMMON`) | hunger and saturation for the three marshmallows become `FoodProperties` | `ROASTING_RANGE_BLOCKS` 2, `ROASTING_DURATION_TICKS` 100, variance 0.2, `ROASTING_BURN_DURATION_TICKS` 20, `MARSHMALLOW_STICK_DURABILITY` 8, the six speed and slow durations, `ROASTED_MARSHMALLOW_EFFECT_POTENCY_DURATION_TICKS` 600 bake. |
| `CAMPFIRE_EFFECTS` (1517 to 1680) | `COMFORT_EFFECT_ENABLED`, `RESTING_EFFECT_ENABLED`, `WELL_FED_EFFECT_ENABLED`, `WELL_RESTED_EFFECT_ENABLED`, `FOCUSED_EFFECT_ENABLED` true (`COMMON`); `COMFORT_SATURATION_MODIFIER` 0.5, `COMFORT_HUNGER_MODIFIER` 0.5, `WELL_FED_EXHAUSTION_MODIFIER` 0.5, `FOCUSED_MAXIMUM_ACCUMULATED_BONUS` 1.5, `FOCUSED_ACCUMULATED_BONUS` 0.05, `FOCUSED_BONUS` 1 (`SERVER`; the client bar divides by the maximum, so it must sync) | | `DEBUG` dies; `EFFECTS_START_TIME` 12000, `EFFECTS_STOP_TIME` 23000, `RESTING_REGEN_INTERVAL_TICKS` 100, `RESTING_REGEN_HALF_HEARTS` 1, `RESTING_LEVEL_UP_INTERVAL_TICKS` 200, `WELL_FED_DURATION_TICKS` 6000, `WELL_RESTED_DURATION_TICKS` 6000, `WELL_RESTED_ABSORPTION_HALF_HEARTS` 4 bake. |

That is eighteen `COMMON` toggles plus two counts (`RECIPE_REPEAT_TOOL_DAMAGE`, `RECIPE_RUIN_RAIN_TICKS`)
and the anvil table, thirty-nine `SERVER` values, and one client flag. `ModConfigSpec.Builder#define`, `defineInRange`, `push`, `pop` (NF-SRC
`net/neoforged/neoforge/common/ModConfigSpec.java`, lines 733, 753, 770, 842, 860; NF-DOCS
https://docs.neoforged.net/docs/1.21.1/misc/config). Decision 12 asks whether the bake list is too
aggressive.

## Dropped outright

- `plugin/`: 79 files. The JEI categories return in the shared plugin (next section); no 1.12 plugin
  code does.
- `ModuleBase`, `Registry`, `Injector`, `IPacketService`, `ITileDataService`, the `modules_enabled`
  recipe condition on all nineteen JSON recipes, the `isModuleEnabled` guards in five adders and the
  pit kiln, the gamestages hooks on every tile.
- `TileEntityDataBase`, `TileEntityDataWorkerBase`, `TileCombustionWorkerBase`, `ITileWorker`
  (`library/spi/tile/`): the campfire and the drying racks are their only tech/basic users and both
  inline their loops. If tech/machine's combustion workers want the base, the machine ticket ports it.
- `InteractionUseItemToActivateWorker`, `InteractionExtinguishable`'s athenaeum half, every
  `IInteraction` subclass, `collisionRayTrace` overrides, `shouldRenderInPass`, `getRenderBoundingBox`,
  `shouldRefresh`, `removedByPlayer` and `harvestBlock` tile-keeping, `getActualState`,
  `getStateFromMeta`, `getMetaFromState`, `damageDropped`, `getSubBlocks`, `getModelName`, `IBlockVariant`.
- `ObservableStackHandler`, `LIFOStackHandler`, `DynamicStackHandler`, `LargeDynamicStackHandler`,
  `LargeObservableStackHandler`, `ObservableFluidTank`, `TickCounter`, the "Empty" tank tag workaround.
- `CampfireRecipe`'s cache, whitelist, blacklist, and `blacklistAll`; `WorktableRecipe` entirely;
  `CompostBinRecipe`'s registry-name lookup; `RecipeHelper.inherit`; `CompatInitializerWood`'s consumer
  in `ChoppingBlockRecipesAdd`.
- The two reflection `MethodHandle`s on `ItemFood.alwaysEdible` and `FoodStats.foodExhaustionLevel`, the
  `activeItemStackUseCount` setter, `CampfireEffectDurationFix`, two of three packets.
- `BlockCompostBin.DEBUG` and `CAMPFIRE_EFFECTS.DEBUG` message spam.
- `ModuleTechBasic.LOGGER`; the port uses `Pyrotech.LOGGER`.

## Assets and JEI

What `main` has for this unit (`src/main/resources/assets/pyrotech/`):

- Blockstates: `anvil_granite.json`, `anvil_iron_plated.json`, `anvil_obsidian.json` (`damage=0..3`),
  `barrel.json` and `barrel_sealed.json` (one variant each), `campfire.json` (158 multipart cases over
  `variant` and `ash`, 98 of them for `variant=item`), `chopping_block.json` (162 cases over `damage` and
  `sawdust`), `compacting_bin.json` (one variant), `compost_bin.json` (33 cases over `state` and
  `compost_value`), `drying_rack.json` (16 cases over `facing` and `variant`), `kiln_pit.json` (five
  variants), `soaking_pot.json` (`campfire` by `facing`), `tanning_rack.json`, `worktable.json`,
  `worktable_stone.json` (`facing`). Fifteen files. None of the twelve hand-conversion blockstates in
  ASSETS belongs to this unit; all twelve are buckets, fluids, slabs, walls, doors, and `material`.
- Block models: the campfire's twelve base models (already carrying `render_type` on the prototype
  branch), kiln pit `_empty`, `_thatch`, `_active`, drying rack, crude, and stacked, chopping block
  core, six bark stages, and two sawdust models, `anvil.json`, `barrel.json` and `barrel_sealed.json`,
  `compacting_bin.json`, `compost_bin.json` and five level models, `soaking_pot.json` and
  `soaking_pot_campfire.json`, `tanning_rack.json`, `worktable.json` and `worktable_stone.json`,
  `tinder.json` (the block-model tinder used by the campfire).
- Item models: `tinder.json`, `marshmallow.json`, `marshmallow_roasted.json`, `marshmallow_burned.json`,
  `marshmallow_stick_empty.json`, `marshmallow_stick.json` with the `pyrotech:marshmallow_type` overrides,
  `marshmallow_on_stick.json`, `marshmallow_roasted_on_stick.json`, `marshmallow_burned_on_stick.json`,
  `barrel_lid.json`. Ten files, all this ticket's.
- Textures: `textures/potions/comfort.png`, `focused.png`, `resting.png`, `resting2.png`, `resting3.png`,
  `well_fed.png`, `well_rested.png`; `textures/gui/jei.png` through `jei13.png` (the category
  backgrounds) and `no_hunger.png`.
- Lang: the fifteen block names (`en_us.json`, lines 115 to 130), the seven item names (106 to 114),
  the two broadcast messages (131 to 132), the five effect names under the 1.12 keys (133 to 137), the
  fourteen JEI category names (471 to 485) and `gui.pyrotech.jei.info.campfire` and `.pit.kiln` (488 to
  489), `gui.pyrotech.tooltip.potency` (423), `.compost.value` (429), the fluid and shift tooltip keys.

Gaps:

- Item models for the fifteen block items: none exist (`models/item/campfire.json` and the other
  fourteen are absent on `main`). 1.12 registered them from blockstate variants
  (`init/BlockInitializer.java`, lines 77 to 130), which the conversion did not turn into item models.
  Each needs `models/item/<block>.json` with the block model as parent; the campfire's points at the
  full-log model the `item` variant used, the kiln at `kiln_pit_empty`, the drying racks at their two
  models, and the chopping block and anvils at their undamaged models with `ItemProperties` overrides
  for `pyrotech:block_damage` (decision 8).
- Blockstates to regenerate (PROTOTYPE open question 5): `campfire.json` without `variant=item`;
  `drying_rack.json` split into `drying_rack_crude.json` (`facing`) and `drying_rack.json` (`facing`,
  `stacked`); `barrel.json` keyed on `sealed` with `barrel_sealed.json` deleted (decision 9).
- Lang: rename the five effect keys to `effect.pyrotech.comfort`, `well_fed`, `resting`, `well_rested`,
  `focused`; the dead `block.pyrotech.barrel_sealed` and `drying_rack_normal` keys follow decisions 9 and
  the drying rack split (`drying_rack` keeps `block.pyrotech.drying_rack`, so `drying_rack_normal` dies).
- Render types (ticket #27): `tanning_rack.json` needs `cutout` (`block/BlockTanningRack.java`, lines
  120 to 123); the campfire's `cutout_mipped` is done on the prototype branch; every other block used
  the 1.12 default solid layer.

JEI. The 1.12 plugin registered fourteen categories: granite, ironclad, and obsidian anvil, barrel,
campfire, chopping block, compacting bin, compost bin, crude drying rack, drying rack, pit kiln,
soaking pot, tanning rack, worktable (`plugin/jei/category/`), with thirteen wrappers, of which the
worktable has a shaped and a shapeless wrapper over the vanilla recipe (`plugin/jei/wrapper/`). The
shared plugin needs the same fourteen over the eleven recipe types plus two synthetic sources: the
campfire category lists the derived smelting foods (one entry per qualifying smelting recipe, with the
campfire's synced `COOK_TIME_TICKS`) beside the explicit `pyrotech:campfire` recipes, and the
worktable category lists every `RecipeType.CRAFTING` recipe with a hammer catalyst, as the 1.12
`JEIRecipeWrapperWorktableShaped` and `Shapeless` did. The chopping block, compacting bin, and anvil
categories show a tool per level from the three `JEI_HARVEST_LEVEL_*` lists baked as constants
(SIGN-OFFS, issue #21 item 2). The campfire and pit kiln info texts are on `main`. Not designed here.

## Findings for other tickets

- **Tech/bloomery** (issue #19): `TileBloom` sends `SCPacketNoHunger` through tech/basic's channel
  (`modules/tech/bloomery/tile/TileBloom.java`, line 198); it becomes core's payload. The tongs read
  and write the anvil's slot (`item/spi/ItemTongsEmptyBase.java`, lines 57 to 71; `ItemTongsFullBase.java`,
  lines 174 to 193); the anvil block entity keeps a public input handler accessor. `BloomAnvilRecipe`
  implements the five hooks and calls `useDurability`, `getDurabilityUntilNextDamage`,
  `setDurabilityUntilNextDamage`, `getBloomAnvilExtraDamageChance`, `getBloomAnvilExtraDamagePerHit`,
  `getStackHandler`, and `getRecipeTier` (`recipe/BloomAnvilRecipe.java`, lines 79 to 175); all stay public
  on the anvil block entity. Its `matches` reads `recipeId` from the bloom's tile tag (lines 46 to 76),
  which is the `pyrotech:bloom` component per issue #21 item 3. `BlockAnvilBase.hasBloom` becomes the
  `AnvilHotItem` interface the bloom item implements (decision 7). `onAnvilHitClient` spawns lava
  particles from the client payload (lines 148 to 160), which is why the anvil hit payload survives.
  Bloomery's datagen writes the slag compacting bin recipes (`init/recipe/CompactingBinRecipesAdd.java`,
  lines 17 to 43) on tech/basic's `CompactingBinRecipe` class. The bloomery recipes carry
  `AnvilRecipe.EnumTier[]` (`recipe/BloomeryRecipeBase.java`, lines 39 and 117), which is RECIPES'
  `AnvilTier` enum.
- **Tech/machine** (issue #20): `TileMechanicalCompactingBin extends TileCompactingBin` overriding only
  `getRecipe` (`modules/tech/machine/tile/TileMechanicalCompactingBin.java`, lines 10 to 15), so the
  compacting bin block entity takes its recipe type as a constructor argument. `BlockMechanicalCompactingBin`
  and `TileMechanicalCompactingBinWorker` read `getInputStackHandler` and `getCurrentRecipe`
  (`block/BlockMechanicalCompactingBin.java`, lines 88 to 89; `tile/TileMechanicalCompactingBinWorker.java`,
  lines 129 to 131). The worker's `InteractionItem` is typed on `TileSoakingPot` by copy-paste (line 219);
  dead, drop it. `TileTripHammer` calls `AnvilRecipe.getTypeFromItemStack`, `AnvilRecipe.getRecipe`, and
  `TileAnvilBase.doInteraction` with a null player and a fixed hit of (0.5, 6/16, 0.5)
  (`tile/TileTripHammer.java`, lines 167 to 213); the anvil's public `hit` and the recipe class's static
  tool resolver serve it. The three mechanical compacting recipes read
  `ModuleTechBasicConfig.COMPACTING_BIN.TOOL_USES_REQUIRED_PER_HARVEST_LEVEL` (`init/recipe/MechanicalCompactingBinRecipesAdd.java`,
  lines 41, 48, 55); the optional recipe field default replaces it. The stone kiln, stone oven, and stone
  sawmill inherit from tech/basic's three types through the chain table; the sawmill copies carry the
  five new woods. The machine ticket decides whether to port `TileCombustionWorkerBase`.
- **Core** (issue #10, hoisting): `MarshmallowStickRecipe` and `ChoppingBlockRecipe` (`modules/core/recipe/`)
  output tech/basic items and rehome here on hunting's tool-damage serializer; CORE's "stay in core"
  row is wrong for these two. `ModuleCoreConfig.FUEL.TINDER_BURN_TIME_TICKS` (line 996) describes
  tech/basic's item, so core's `furnace_fuels` file should not carry `tinder`; tech/basic's does. Core
  owns `RefractoryBlocks.isRefractory` beside the tag (pit kiln section). `PlayerMovementTracker` stays
  in core as CORE said; the resting handler reads it. `ModuleCoreConfig.TWEAKS.REQUIRE_SHOVEL_TO_PICKUP_WOOD_CHIPS`
  (line 67) is read by the chopping block, so it stays a public `COMMON` value.
- **Storage** (issue #14, hoisting): `LargeStackHandler` is used by the compacting bin input, the compost
  bin input, and the soaking pot output; `HotFluidTank` by the barrel and the soaking pot; the fluid box
  renderer by both; the scroll payload by four blocks; the hit-to-slot helper by nine.
- **Ignition** (issue #18, hoisting): the douse helper lives in `library/fluid` and takes its bounds
  from the caller, so it stops importing `BlockCampfire` (`library/spi/interaction/InteractionExtinguishable.java`,
  line 8). The campfire and pit kiln return `SKIP_DEFAULT_BLOCK_INTERACTION` for `#pyrotech:igniters` as
  IGNITION asked.
- **Hunting** (issue #15, hoisting): the `pyrotech:tool_damage_shapeless` serializer gains two users
  outside hunting; keep it in the shared recipe package. `#pyrotech:knives` replaces `toolSharp`. The
  six hunting-guarded soaking pot, barrel, and tanning rack recipes are written by tech/basic's datagen.
- **Tool** (issue #11, hoisting): the chopping block crafting recipe takes `#minecraft:axes`, so
  Pyrotech's axes must join that tag in tool's datagen; the chopping block itself tests `AXE_DIG`, which
  `AxeItem` answers by default.
- **Bucket** (issue #13, hoisting): `bucket_clay` and `bucket_refractory` are pit kiln recipes written
  by tech/basic; bucket's datagen writes none.
- **Refractory** (issue #17, hoisting): twelve tar soaking pot recipes, not ten (scope correction).
- **Patchouli book** (the last ticket): the campfire `book` recipe (`init/recipe/CampfireRecipesAdd.java`,
  lines 18 to 24) turns a vanilla book into Pyrotech's book; the book ticket writes it on the
  `pyrotech:campfire` type if decision 4 keeps the type.
- **Render layers** (issue #27): `tanning_rack.json` cutout.
- **JEI plugin**: fourteen categories, two synthetic sources (assets and JEI section).

## Decisions for Moos

1. **The new stone slab: an anvil entry or unobtainable.** Facts: the 1.12 `stone_slab` recipe (stone, 8
   pickaxe hits, 2 slabs, `AnvilGraniteRecipesAdd.java`, lines 355 to 363) outputs 1.12 stone slab meta
   0, which is `minecraft:smooth_stone_slab` in 1.21, and the pit kiln's `stone_slab` (cobblestone slab
   to stone slab meta 0, `PitKilnRecipesAdd.java`, lines 108 to 117) outputs the same. The stone-textured
   `minecraft:stone_slab` has no 1.12 route and its four vanilla recipes are stubbed (SIGN-OFFS, issue #33
   item 5). Options: add `pyrotech:anvil/stone_slab_rough` (stone, pickaxe, 8 hits, 2 stone slabs) beside
   the smooth one; or leave it unobtainable. Recommendation: add it. The anvil already turns stone into
   cobblestone and every other slab, the block exists in villages and structures, and one more
   datagen line keeps a vanilla building block reachable without touching the smooth slab's fidelity.
2. **Campfire effect durations: infinite plus a counter, or finite plus the client fix.** Facts:
   `MobEffectInstance.INFINITE_DURATION` renders as infinite (NF-SRC `MobEffectUtil.java`, lines 17 to
   18), which is what `CampfireEffectDurationFix` faked every client tick; but `PotionResting` derives
   its elapsed ticks from `Short.MAX_VALUE - duration` (`PotionResting.java`, lines 76, 133, 188, 225) and
   `PotionFocused.isReady` from `duration % 20` (`PotionFocused.java`, line 46), and an infinite instance
   hands `-1` to `shouldApplyEffectTickThisTick`. Options: infinite instances plus a `restingTicks` field
   in the synced attachment; or `Short.MAX_VALUE` instances plus a port of the duration fix on
   `PlayerTickEvent` client side. Recommendation: infinite plus the counter. It deletes a handler, the
   200-tick comfort refresh, and focused's self-refresh, and the attachment already exists for the
   focused bonus.
3. **Compost values: a recipe type or a data map.** Facts: every 1.12 compost recipe outputs four mulch
   (`CompostBinRecipesAdd.java`, lines 117 to 130); the input is really an item to value pair; sixty
   explicit entries plus every food item at runtime (lines 92 to 112); the 1.21 flowers, leaves, and
   saplings are tags. Options: a `pyrotech:compost_bin` recipe type (`Ingredient`, `value`, `result`), one
   JSON per entry, tag ingredients allowed, JEI lists them as recipes, and a runtime rule for untagged
   foods; or an item data map `pyrotech:compost_values` keyed by item or tag with the mulch output fixed
   and the same runtime food rule. Recommendation: the recipe type. It keeps the output configurable as
   CraftTweaker allowed, it is what JEI expects, and it follows RECIPES' one-class-per-shape rule; the
   runtime food fallback reproduces `AUTO_CREATE_RECIPES_FROM_FOOD` for other mods' foods, which datagen
   cannot enumerate.
4. **The explicit campfire recipe type.** Facts: the derived smelting list is the whole 1.12 cook list
   except one Patchouli book recipe (`CampfireRecipesAdd.java`, lines 18 to 24) and CraftTweaker
   additions. Options: keep a `pyrotech:campfire` type checked before the derived list, giving datapacks
   and the book ticket a hook; or derive only and give the book its own smelting-with-food route.
   Recommendation: keep the type. It is one small class on the shared serializer pattern and the book
   needs it.
5. **Campfire and kiln fuel tag: `logs_that_burn` or `logs`.** Facts: `logWood` in 1.12 held every log,
   and no nether wood existed; `#minecraft:logs` adds crimson and warped stems, which vanilla will not
   smelt into charcoal (DATA `data/minecraft/tags/item/logs.json`, `charcoal.json`). Options:
   `#pyrotech:campfire_fuels` filled with `#minecraft:logs_that_burn`, or with `#minecraft:logs`.
   Recommendation: `logs_that_burn`, which is what PROTOTYPE used and what "burns" means; a datapack can
   add the stems.
6. **No worktable recipe type.** Facts: 1.12 shipped zero `WorktableRecipe`s; the type existed for
   CraftTweaker tool-gated recipes (`WorktableRecipe.java`, lines 226 to 286), and the whitelist and
   blacklist config defaulted to empty. Options: drop the type and the lists, wrapping `RecipeType.CRAFTING`
   only; or keep a `pyrotech:worktable` type carrying a nested crafting recipe, a tool ingredient, and a
   tool damage for datapacks. Recommendation: drop. Nothing in 1.12 used it, and a datapack can gate
   a recipe by making its ingredients Pyrotech items.
7. **Keeping the anvil bloom-agnostic: an item interface.** Facts: `BlockAnvilBase` imports bloomery for
   `Items.BLOOM` and `BLOOM.ENTITY_WALK_DAMAGE` to burn walkers and spawn flames (lines 8 to 9, 81 to 117,
   239 to 258), which is the one real basic-to-bloomery edge. Options: a tech/basic interface
   `AnvilHotItem { float walkDamage(); }` the bloom item implements; an item tag `#pyrotech:anvil_hot_items`
   with the damage baked in tech/basic; or leave the edge and hoist bloomery's item stub early.
   Recommendation: the interface. The damage number is bloomery's config in 1.12 and the interface
   lets bloomery keep it.
8. **Damaged anvils and chopping blocks as items.** Facts: 1.12 drops a damaged anvil as an item with
   metadata equal to its damage stage and a matching item model, and places it back at that stage
   (`BlockAnvilBase.java`, lines 163 to 174, 209 to 225, 271 to 284; `BlockChoppingBlock.java`, lines 102 to
   113, 199 to 212). Options: a `pyrotech:block_damage` int component copied by the loot table, read in
   `getStateForPlacement`, and driving an `ItemProperties` model override; or drop the damage on break
   (always the fresh item). Recommendation: the component. It is faithful and four small pieces.
9. **The barrel: one block with `sealed`, or two blocks.** Facts: 1.12 swaps two blocks around one tile
   with a `keepInventory` flag and a re-validate (`BlockBarrel.java`, lines 65 to 83); the converted
   assets and lang on `main` have both `barrel` and `barrel_sealed`. Options: one block with a `sealed`
   property, a regenerated blockstate, and the sealed item as the barrel item carrying components; or two
   blocks sharing one block entity type with the swap ported. Recommendation: one block. It is the
   PROTOTYPE rule, the block entity survives a property change without tricks, and the sealed item's
   distinct name is the only loss.
10. **Drying rack climbing: `isLadder`.** Facts: 1.12 nudges player motion at `CLIMB_SPEED` 0.1 from the
    tile tick when a normal rack is stacked (`TileDryingRack.java`, lines 137 to 165). Options: keep the
    motion hack in the block entity's ticker; or `IBlockExtension#isLadder` (line 181) on the normal rack
    when stacked, gated by `USE_AS_LADDER`, at vanilla climbing speed. Recommendation: `isLadder`. Vanilla
    climbing is what players expect, and the speed knob is not a mechanic anyone tunes.
11. **The focused XP hook.** Facts: 1.12 multiplies the orb's value at highest priority in
    `PlayerPickupXpEvent` (`CampfireFocusEffectEventHandler.java`, lines 34 to 77); in 21.1.249 `PickupXp`
    exposes the orb but `ExperienceOrb.value` has no setter, and Mending repairs from the orb before
    `giveExperiencePoints` (NF-SRC `ExperienceOrb.java`, lines 238 to 254). Options: `PlayerXpEvent.XpChange`
    with `setAmount`, which fires for every `giveExperiencePoints` call including commands and bottles
    and sees the post-Mending remainder; or an access transformer on the orb's value field to keep the
    exact 1.12 hook. Recommendation: `XpChange`. The bonus applies to orb pickups as before, and the two
    differences (commands also boosted, Mending not boosted) are edges nobody plays for.
12. **Config surface: the bake list.** Facts: the table above keeps eighteen toggles, two counts, and
    one table in `COMMON`, thirty-nine multipliers in `SERVER`, and one client flag, and bakes the rest
    (hits per damage, exhaustion, hunger minimums, light levels, timers, capacities). Options: as listed; or keep
    the per-block "hits per damage" and "durability" numbers as config too, since they tune wear.
    Recommendation: as listed. CORE's principle keeps toggles and multipliers; wear counts describe the
    block, and the `USE_DURABILITY` toggles already let a server turn wear off.
