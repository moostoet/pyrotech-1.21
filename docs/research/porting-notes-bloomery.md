# Porting notes: tech/bloomery

Resolves issue #19 (porting notes: tech/bloomery).
Tech/bloomery is the tenth hoisting unit from issue #4 (module porting order): 60 files, 8,764 lines
(DEPS). Of those, 12 files are plugin code (1 CraftTweaker, 3 JEI, 3 TOP, 5 Waila) and 14 are the
tongs subclasses, each a 16-line shell around one config number. The 34 remaining classes hold four
blocks (bloomery, wither forge, bloom, slag heap), four tiles, one item family (slag, seven tongs in
two states, the bloom block item), two event handlers, one particle, one renderer, seven recipe
classes, four recipe adders, the runtime slag generator, and a 612-line config. This document lists
each 1.12 construct and its 1.21 replacement. Decisions that need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo, package `com.codetaylor.mc.pyrotech.modules.tech.bloomery`,
  plus the library classes it uses (`library/spi/block/BlockPileBase.java`, `library/spi/tile/TileCapabilityDelegate.java`,
  `library/spi/tile/ITileContainer.java`, the three `library/spi/block/IBlockIgnitable*.java`,
  `library/JsonInitializer.java`, `library/util/ExperienceHelper.java`, `library/util/Util.java`), the
  airflow interface in the api source set (`src/api/java/com/codetaylor/mc/pyrotech/IAirflowConsumerCapability.java`),
  its users outside the package (`modules/tech/basic/block/spi/BlockAnvilBase.java`,
  `modules/tech/basic/plugin/jei/wrapper/JEIRecipeWrapperAnvil.java`,
  `modules/tech/basic/plugin/waila/delegate/AnvilProviderDelegate.java`,
  `modules/tech/machine/init/recipe/StoneKilnRecipesAdd.java`, `modules/tech/machine/tile/TileBellows.java`,
  `modules/core/init/CompatInitializerOre.java`, `modules/core/ModuleCoreConfig.java`,
  `modules/core/event/LootTableLoadEventHandler.java`, `ModPyrotechConfig.java`, `ModPyrotech.java`,
  `BulkRenderItemSupplier.java`, `ExportDocumentation.java`,
  `modules/plugin/patchouli/processors/FuelBloomeryProcessor.java` and `FuelWitherForgeProcessor.java`),
  the recipe JSON under `assets/pyrotech/recipes/tech/bloomery/` (9 files), the blockstates, models,
  advancements, and `lang/en_us.lang`, and `docs/orecompat.md`.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths cited below are entries in that jar.
- **NF-DATA**: the NeoForge 21.1.249 jar's data (`build/moddev/artifacts/neoforge-21.1.249.jar`), for the
  `c:` tags and the `furnace_fuels` data map.
- **DATA**: the vanilla 1.21.1 data pack and client assets, at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`.
- **CORE**: `docs/research/porting-notes-core.md` on branch `research/porting-notes-core`, as amended by
  issue #22.
- **RECIPES**: `docs/research/recipe-architecture.md` on branch `research/recipe-architecture`, as
  amended by issue #21.
- **TECH-BASIC**: `docs/research/porting-notes-tech-basic.md` on branch `research/porting-notes-tech-basic`,
  as amended by issue #35.
- **REFRACTORY**: `docs/research/porting-notes-refractory.md` on branch `research/porting-notes-refractory`,
  as amended by issue #29.
- **TOOL**: `docs/research/porting-notes-tool.md` on branch `research/porting-notes-tool`.
- **STORAGE**: `docs/research/porting-notes-storage.md` on branch `research/porting-notes-storage`.
- **IGNITION**: `docs/research/porting-notes-ignition.md` on branch `research/porting-notes-ignition`, as
  amended by issue #34.
- **BUCKET**: `docs/research/porting-notes-bucket.md` on branch `research/porting-notes-bucket`.
- **SKIPS**: `docs/research/post-1-12-progression-skips.md` on branch `research/post-1-12-progression-skips`,
  as amended by issue #33.
- **DEPS**: `docs/research/module-dependencies.md` on branch `research/module-dependencies`.
- **PROTOTYPE**: issue #8 and branch `prototype/8-campfire-interaction-sync`,
  `docs/prototypes/campfire-interaction-sync.md`.
- **ASSETS**: `docs/asset-migration-report.md` and `docs/asset-conversion-report.md` on `main`, and the
  converted files under `src/main/resources/assets/pyrotech/`.
- **SIGN-OFFS**: the resolution comments of issues #21, #22, #29, #31, #33, #34, and #35, and the five
  follow-on comments on issue #19.

This document assumes the recommended answers from CORE, RECIPES, TECH-BASIC, REFRACTORY, TOOL,
STORAGE, IGNITION, and BUCKET as the sign-offs amended them. It applies PROTOTYPE pattern 1 (per-block
if-chain dispatch from `useItemOn` and `useWithoutItem`, ghost preview kept) and pattern 2 (blockstate
properties where possible, else a full snapshot per change through `SyncedBlockEntity`).

## Scope correction

The ticket text and DEPS need four corrections.

- The bloomery does not build on tech/basic's combustion worker patterns. `TileBloomery` extends
  athenaeum's `TileEntityDataBase` and implements `ITileInteractable`, `ITickable`, `ITileContainer`,
  and `IAirflowConsumerCapability` (`tile/TileBloomery.java`, lines 62 to 67). It never touches
  `library/spi/tile/TileCombustionWorkerBase.java`, `TileBurnableBase.java`, or
  `TileEntityDataWorkerBase.java` (grep of the package for those names finds nothing). Its fuel model is
  its own: fuel is counted and summed into one burn-time pool that sets a speed, and nothing is burned
  down per tick (block entity section). The only thing it shares with a combustion worker is the airflow
  interface, which it shares with tech/machine's `TileCombustionWorkerStoneBase`
  (`modules/tech/machine/tile/spi/TileCombustionWorkerStoneBase.java`, line 46), not with tech/basic.
  TECH-BASIC did not port the worker bases and left them to the machine ticket; the bloomery needs
  nothing from them.
- DEPS' "bloomery depends on basic (15)" is right as a file count and 12 of the 15 are real. Real:
  `TileBloom` (tech/basic's packet channel, line 198), the two tongs bases (`TileAnvilBase`,
  `item/spi/ItemTongsEmptyBase.java` lines 57 to 71, `ItemTongsFullBase.java` lines 174 to 193),
  `BloomAnvilRecipe` (extends `AnvilRecipe`, line 29), `BloomeryRecipeBase`, `BloomeryRecipe`,
  `WitherForgeRecipe`, and `BloomeryRecipeBuilderBase` (`AnvilRecipe.EnumTier`), `BloomeryRecipesAdd`
  and `WitherForgeRecipesAdd` (`AnvilRecipe` registry), `CompactingBinRecipesAdd`
  (`CompactingBinRecipe`), and `ModuleTechBloomery` (`ModuleTechBasic.Registries`, lines 177 and 188).
  Plugin or string-constant only: `ZenBloomery`, and the two TOP providers reading `ModuleTechBasic.MOD_ID`.
  All 12 real edges survive as edges to tech/basic's `AnvilTier`, `ExtendedAnvilRecipe`, the anvil
  block entity's public accessors, `CompactingBinRecipe`, and core's no-hunger payload.
- The airflow interface is not in the module tree. `IAirflowConsumerCapability` lives in the separate
  api source set (`src/api/java/com/codetaylor/mc/pyrotech/IAirflowConsumerCapability.java`, one method
  at line 18), which DEPS counted as "`api/` (1 file), zero module imports". It has three module users:
  `TileBloomery` (implements it, line 67), `TileBellows` (calls it, `modules/tech/machine/tile/TileBellows.java`
  line 158), and `TileCombustionWorkerStoneBase` (line 46).
- The follow-on on issue #20 says "the slag glass stone kiln recipe written by bloomery's class". Under
  the placement rule (SIGN-OFFS, issue #21 item 6: the datagen of the last-hoisted unit among type,
  ingredients, and result), that recipe's type is machine's `pyrotech:stone_kiln`, and machine hoists
  after bloomery, so tech/machine's class writes it (`modules/tech/machine/init/recipe/StoneKilnRecipesAdd.java`,
  lines 89 to 103). Bloomery writes the vanilla furnace version (`init/recipe/VanillaFurnaceRecipesAdd.java`,
  lines 10 to 18), whose type is vanilla. The chain table copies the kiln recipe to the brick kiln
  either way.

Four smaller facts the ticket text does not say:

- Every bloomery recipe in 1.12 is generated at runtime from the ore compat JSON
  (`init/recipe/BloomeryRecipesAdd.java`, lines 41 to 138). There is no hard-coded bloomery recipe. The
  only hard-coded recipe in the unit is the wither forge's obsidian one (`init/recipe/WitherForgeRecipesAdd.java`,
  lines 48 to 59). With no other mods, the compat data holds `oreIron` and `oreGold`
  (`modules/core/init/CompatInitializerOre.java`, lines 42 to 58: an entry needs an ore and a nugget in
  the ore dictionary; `ModuleCoreConfig.java`, lines 929 to 947 list seventeen ore keys), so vanilla
  Pyrotech 1.12 ships four bloomery recipes and one wither forge recipe (recipes section).
- The slag items and slag heap blocks are generated at runtime too, one pair per compat entry, plus a
  base pair (`init/SlagInitializer.java`, lines 92 to 173 and 200 to 228). Vanilla Pyrotech has three of
  each: plain, iron, gold.
- `TileBloomery.updateAirflow` compares a `Block` to the `GENERATED_PILE_SLAG` map (line 393), which is
  always false, so the graduated slag-heap airflow values at lines 394 to 407 never run. A slag heap in
  front of a bloomery is scored by the generic branch: 0 when its face is solid (a full heap), else 0.5
  (lines 410 to 415). Decision 8 asks which to port.
- `WitherForgeRecipesAdd.INHERIT_TRANSFORMER` copies burn time, failure chance, yield, slag, lang key,
  and failure items, but not experience and not the anvil tiers (lines 28 to 43). Every inherited wither
  forge copy therefore yields 0 experience and accepts all three anvil tiers, while the bloomery original
  yields 0.25 per completion. Decision 8 covers this too.

## Summary

- The module bootstrap collapses into `DeferredRegister` holders for four blocks plus two per-ore slag
  heaps, seven tongs, three slag items, three block entity types, two recipe types with one shared
  serializer, one extra `pyrotech:anvil` serializer, one data component, one particle type, one data map,
  and client registrations for one renderer, one particle provider, and the slag colour handlers.
- The bloomery and wither forge stay one block each with `facing` and the three-value `type` property
  the converted blockstates key on; the top half has no block entity and delegates every use to the
  block entity below. What 1.12 synced as tile data splits into the `type` property (lit) and a
  `SyncedBlockEntity` snapshot holding what the renderer reads: input, output, fuel, fuel count, burn
  time, ash count, airflow. Recipe progress and speed leave the wire.
- Airflow is core's plain interface. The bellows finds the block entity in front of it and calls it;
  no capability and no bloomery-to-machine edge.
- The bloom becomes a plain block entity whose five fields ride the `pyrotech:bloom` component, one
  `pyrotech:bloom_anvil` serializer, one `anvil/bloom` recipe, and an `AnvilHotItem` implementation, as
  the recipe and tech/basic sign-offs settled. Its custom item entity dissolves into a fire-resistant
  item plus `onEntityItemUpdate`.
- The fourteen tongs collapse to seven items with the bloom component marking "full" (decision 3).
- The runtime slag generator and the ore compat JSON die (SIGN-OFFS, issue #22 item 5). Datagen writes
  the iron and gold recipes and the three slag pairs are registered items and blocks (decision 2).
- Every bloomery input becomes a tag: `#c:raw_materials/iron` and `#c:ores/iron` for iron, the gold
  twins for gold, and `#c:obsidians` for the wither forge (decision 1). Copper gets no bloomery recipe;
  the five vanilla decompression recipes SKIPS left open get anvil entries (decision 7).
- The 612-line config shrinks to six toggles in `COMMON`, seventeen values in `SERVER` (the hit counts,
  the hammer tables, and the speed, airflow, and duration multipliers), the fuel modifiers in a data
  map, and everything else baked.

## Module bootstrap

`ModuleTechBloomery` follows the CORE replacements: one `register(IEventBus)` hook, `DeferredRegister`
fields as holders, no `ModuleBase`, no `Registry`, no `Injector`, no `@GameRegistry.ObjectHolder`.

| 1.12 construct | 1.21 replacement |
|---|---|
| `setRegistry`, `enableAutoRegistry`, `PACKET_SERVICE`, `TILE_DATA_SERVICE` (`ModuleTechBloomery.java`, lines 70 to 74) | Dropped with athenaeum. Sync is the PROTOTYPE `SyncedBlockEntity`; the unit sends no payload of its own (network section). |
| The CraftTweaker plugin, the JEI plugin, the TOP and Waila IMC messages (lines 78 to 92, 136 to 155) | Dropped. JEI returns in the shared plugin (assets and JEI section). |
| `RegistryEvent.NewRegistry` creating `bloomery_recipe` and `wither_forge_recipe` registries and injecting them into `Registries` (lines 96 to 127) | `RecipeType` and `RecipeSerializer` entries in RECIPES' holders: `pyrotech:bloomery` and `pyrotech:wither_forge` on one `BloomeryRecipe` class with the type and serializer as constructor arguments (RECIPES class table; NF-SRC `net/minecraft/world/item/crafting/RecipeType.java`, line 25), plus the `pyrotech:bloom_anvil` serializer for tech/basic's `pyrotech:anvil` type (SIGN-OFFS, issue #21 item 3). |
| `onTextureStitchEvent` registering `blocks/active_pile` and `blocks/ash_block` for the fuel renderer (lines 160 to 165) | Nothing. Both sprites are stitched because block models on `main` reference them (`models/block/active_pile.json`, `ash_block.json`, `bloomery_lit.json`). The renderer fetches them with `Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id)` (NF-SRC `net/minecraft/client/Minecraft.java`, line 2568; `net/minecraft/client/renderer/texture/TextureAtlas.java`, line 30). |
| `onRegisterRecipesEvent`: the furnace recipe, the compat bloomery recipes, the wither forge recipe, the compacting bin recipes behind the tech/basic guard, the inheritance call, and the two bloom anvil registrations (lines 168 to 195) | Datagen (recipes section). The guards die with the module toggles. |
| `onRegister`: blocks, items, the slag generator run as an item registration strategy, the item entity (lines 198 to 209) | `DeferredRegister.Blocks#registerBlock` and `Items#registerSimpleBlockItem` or `registerItem` (NF-SRC `net/neoforged/neoforge/registries/DeferredRegister.java`, lines 46 to 62 document the pattern). The slag generator becomes three explicit registrations (slag section). The item entity is not registered (bloom section). |
| `BlockInitializer.onRegister`: four blocks, the slag heap with `ItemBlockPileSlag`, the bloom with `ItemBlockBloom` registered with the no-creative-tab flag, five tile classes (`init/BlockInitializer.java`, lines 23 to 48) | Four `DeferredBlock`s. `BlockEntityType.Builder.of(factory, blocks...).build(null)` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntityType.java`, line 341) for three types: bloomery over both the bloomery and the wither forge, bloom, slag heap over the three heap blocks. `TileWitherForge` only overrides config getters and three behaviours (block entity section), so one class reads its numbers and its side count from the block, as TECH-BASIC does for the anvils. `TileBloomery.Top` (lines 1251 to 1259) has no replacement: the top half has no block entity. |
| `BlockInitializer.onClientRegister`: item models from blockstates and two `TESRInteractable` bindings (lines 52 to 67) | Item models load by id (assets section). `EntityRenderersEvent.RegisterRenderers#registerBlockEntityRenderer` (NF-SRC `net/neoforged/neoforge/client/event/EntityRenderersEvent.java`, line 109) for the bloomery type. |
| `EntityInitializer`: `EntityItemBloom` with tracker 80, 4, true (`init/EntityInitializer.java`, lines 9 to 15) | Dropped (bloom section, decision 4). |
| `ItemInitializer`: slag, seven empty tongs, seven full tongs with the no-creative-tab flag (`init/ItemInitializer.java`, lines 13 to 30) | Three slag items and seven tongs (tongs section, slag section). |
| `SlagInitializer.initializeSlagModels` and `initializeSlagColors` (`init/SlagInitializer.java`, lines 247 to 300) | `RegisterColorHandlersEvent.Block#register` and `.Item#register` (NF-SRC `net/neoforged/neoforge/client/event/RegisterColorHandlersEvent.java`, lines 65 and 113) with one colour per registered slag (slag section). |
| `ModuleTechBloomery.CREATIVE_TAB` | The bloomery, wither forge, slag heap, three slags, and seven tongs join core's tab in `BuildCreativeModeTabContentsEvent`. The bloom (`block/BlockBloom.java`, lines 72 to 75 return `null`) stays out. |
| `Blocks`, `Items`, `Registries` holder classes with the `GENERATED_*` maps (lines 230 to 335) | The `DeferredHolder` fields. The maps die with the generator. |

## Shared shapes

TECH-BASIC's five shared idioms apply unchanged: interaction dispatch as an if-chain in `useItemOn`
and `useWithoutItem` (NF-SRC `net/minecraft/world/level/block/state/BlockBehaviour.java`, lines 226 and
222) with the 1.12 array order, the hunger gate through core's no-hunger payload, tool tests through
`ItemAbilities` and core's `pyrotech:tool_levels`, contents on break through `onRemove` (line 193)
and `Containers.dropItemStack`, and progress particles from the client ticker. Two more this unit
repeats:

- **Hot floor.** Four blocks damage walkers unless the entity is fire immune, is a `LivingEntity`, and
  lacks frost walker (`block/BlockBloomery.java`, lines 252 to 272; `BlockBloom.java`, lines 101 to 111;
  `BlockPileSlag.java`, lines 172 to 186; tech/basic's anvil). The replacement is `Block#stepOn` (NF-SRC
  `net/minecraft/world/level/block/Block.java`, line 394) with `entity.isSteppingCarefully()` as
  `MagmaBlock` does (NF-SRC `net/minecraft/world/level/block/MagmaBlock.java`, lines 29 to 31),
  `entity.fireImmune()` (NF-SRC `net/minecraft/world/entity/Entity.java`, line 1212), and
  `level.damageSources().hotFloor()` (NF-SRC `net/minecraft/world/damagesource/DamageSources.java`,
  line 105). The bloomery also sets the walker on fire for 4 seconds (line 266): `igniteForSeconds(4)`
  (NF-SRC `Entity.java`, line 539).
- **Flames and crackle.** Three blocks play `FURNACE_FIRE_CRACKLE` at 10 percent and spawn flame
  particles in `randomDisplayTick` (`BlockBloomery.java`, lines 418 to 535; `BlockBloom.java`, lines 205
  to 221; `BlockPileSlag.java`, lines 134 to 164). `Block#animateTick` (NF-SRC `net/minecraft/world/level/block/Block.java`,
  line 277), the PROTOTYPE campfire's hook, with `ParticleTypes.FLAME`,
  `SMOKE`, `LARGE_SMOKE`, `LAVA` (NF-SRC `net/minecraft/core/particles/ParticleTypes.java`, lines 55,
  84, 77, 78) and `SoundEvents.FURNACE_FIRE_CRACKLE` (NF-SRC `net/minecraft/sounds/SoundEvents.java`,
  line 581).

## The bloomery and wither forge blocks

One class, `BlockBloomery`, and a subclass, `BlockWitherForge`, that changes the walk damage source,
the placement sound, the lit light level, and the tile class (`block/BlockWitherForge.java`, lines 25
to 83).

| 1.12 construct | 1.21 replacement |
|---|---|
| `FACING_HORIZONTAL` plus `TYPE` top, bottom, bottom_lit; metadata packs both; `bottom_lit` is derived in `getActualState` from the tile's `active` (`block/BlockBloomery.java`, lines 57, 72 to 75, 399 to 413, 573 to 596, 617 to 658) | `BlockStateProperties.HORIZONTAL_FACING` (NF-SRC `net/minecraft/world/level/block/state/properties/BlockStateProperties.java`, line 54) and an `EnumProperty` `type` with the same three values (NF-SRC `EnumProperty.java`, line 75), written by the block entity when it activates or finishes. The converted `bloomery.json` and `wither_forge.json` on `main` key on exactly `facing` and `type`, so nothing regenerates (decision 6 asks whether to split `type` into `half` and `lit` instead). |
| `Material.ROCK`, `SoundType.STONE`, hardness 2, pickaxe 0 (lines 66 to 71) | `Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(2.0f).requiresCorrectToolForDrops().noOcclusion()` (NF-SRC `BlockBehaviour.java`, lines 1185, 1195, 1298, 1165) plus `minecraft:mineable/pickaxe` (NF-SRC `net/minecraft/tags/BlockTags.java`, line 141). |
| Top box 2 to 14 px by 8 tall; bottom full; `isSideSolid` only `DOWN` on the bottom; never a full or opaque cube; `UNDEFINED` face shape (lines 59 to 64, 129 to 186) | `getShape` (line 361) returning `Block.box(2, 0, 2, 14, 8, 14)` for the top (NF-SRC `Block.java`, line 151) and the two bottom boxes unioned. `noOcclusion()` for the rest. |
| `igniteWithAdjacentIgniterBlock`: bottom half only, `setActive` (lines 91 to 101); `igniteWithIgniterItem`: top half from `UP` only, `setActive` on the tile below (lines 103 to 119) | Core's `IBlockIgnitableAdjacentIgniterBlock` and `IBlockIgnitableWithIgniterItem` (CORE igniter contract, `library/spi/block/*.java` lines 8 to 18) with the same half and side rules, calling the block entity's `ignite()` (SIGN-OFFS, issue #19 follow-on 2). |
| `quantityDropped` 0 for the top (lines 194 to 201); `canSilkHarvest` false (lines 361 to 366) | Loot: `createSinglePropConditionTable(block, TYPE, bottom)` shape, which is what `createDoorTable` does for the lower half (NF-SRC `net/minecraft/data/loot/BlockLootSubProvider.java`, lines 728 to 729 and 235). A lit bottom must also drop, so the condition is `type != top`, two `LootItemBlockStatePropertyCondition` entries (NF-SRC `net/minecraft/world/level/storage/loot/predicates/LootItemBlockStatePropertyCondition.java`, line 53). |
| `onBlockActivated`: on the top half, an `ItemIgniterBase` in the main hand returns false; else `interact` (lines 205 to 226); `interact` on the top half re-dispatches to `pos.down()` with `hitY + 1` (lines 229 to 236) | `useItemOn`: on the top half, `SKIP_DEFAULT_BLOCK_INTERACTION` for `#pyrotech:igniters` (NF-SRC `net/minecraft/world/ItemInteractionResult.java`, line 8; SIGN-OFFS, issue #19 follow-on 2), then resolve the block entity at `pos.below()` and run the chain against it with the hit face and a local position shifted up by one. The bottom half runs the same chain directly. The chain order is the 1.12 array order (block entity section). |
| `neighborChanged` calling `updateAirflow` (lines 241 to 248) | `neighborChanged` (line 186) doing the same on the bottom half. |
| `onEntityWalk`: top half only, when the tile below is active: hot floor damage `ENTITY_WALK_BURN_DAMAGE` 3 and 4 seconds of fire (lines 252 to 277; `BlockWitherForge.java`, lines 25 to 28) | `stepOn` on the top half reading `type == bottom_lit` from the state below (shared shapes). The number bakes (config section). |
| `collisionRayTrace` with two bottom boxes and the top's interaction ray trace (lines 283 to 302) | Dies with the framework. `getShape` gives the hit position. |
| `breakBlock`: the top removes the bottom, drops its contents and one bloomery item; the bottom removes the top and drops contents (lines 305 to 340) | `playerWillDestroy` (NF-SRC `Block.java`, line 467) calling `DoublePlantBlock.preventDropFromBottomPart` as `DoorBlock` does (NF-SRC `net/minecraft/world/level/block/DoorBlock.java`, lines 126 to 131; `DoublePlantBlock.java`, line 120), plus `updateShape` (line 178) removing whichever half is left when the other goes, the `DoorBlock` shape (lines 100 to 105). Contents drop from the bottom's `onRemove`. The item drops from the bottom's loot table, so no `StackHelper.spawnStackOnTop` call. |
| `onBlockPlacedBy`: place the top half above with the same facing when `canPlaceBlockAt` (lines 344 to 357); `canPlaceBlockAt` requires both positions free (lines 370 to 374); `getStateForPlacement` faces the player (lines 601 to 615) | `getStateForPlacement(BlockPlaceContext)` (NF-SRC `Block.java`, line 398) returning null when the block above cannot be replaced, as `DoorBlock` does (line 144); `setPlacedBy` (line 414) setting the top with `level.setBlock(pos.above(), state.setValue(TYPE, top), 3)` (DoorBlock, lines 164 to 165). `BlockWitherForge.onBlockPlacedBy` also plays `ENTITY_WITHER_SPAWN` at 0.4 when `ENABLE_SCARY_SOUNDS` (lines 32 to 46): `level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 0.4f, 1.0f)` (NF-SRC `net/minecraft/world/level/Level.java`, line 441; `SoundEvents.java`, line 1552). |
| `getLightValue` 12 when `bottom_lit`, wither forge 15 (lines 382 to 389; `BlockWitherForge.java`, lines 54 to 61) | `Properties.lightLevel(state -> state.getValue(TYPE) == bottom_lit ? 12 : 0)` (NF-SRC `BlockBehaviour.java`, line 1190), 15 for the wither forge. |
| `randomDisplayTick`, top half: when active, crackle 10 percent, eight smoke, four large smoke, four flame, one lava at 5 percent, at y 4 to 6 px (lines 418 to 460); bottom half: crackle, one smoke and one flame at the front face 0.55 out, plus at 25 percent a second flame and a drip particle (lines 461 to 535) | `animateTick` on both halves with the same numbers. The drip is the custom particle below. |
| `hasTileEntity`, `createTileEntity`: `TileBloomery` on the bottom, `TileBloomery.Top` on the top, a `TileCapabilityDelegate` that forwards `hasCapability` and `getCapability` to the block below (lines 541 to 557; `library/spi/tile/TileCapabilityDelegate.java`, lines 10 to 46) | `EntityBlock#newBlockEntity` returning null for the top half (NF-SRC `net/minecraft/world/level/block/EntityBlock.java`, line 15). The delegate existed so a bellows facing the top half could reach the airflow capability. With airflow a plain interface, the bellows' lookup resolves a top half to the block entity below (findings, machine). |
| `BulkRenderItemSupplier` listing `BlockBloomery` for the two-high render (`BulkRenderItemSupplier.java`, lines 29 to 36) | Dropped with the texture tooling (CORE). |

## The bloomery and wither forge block entities

`TileBloomery` is 1,261 lines. `TileWitherForge` overrides eleven config getters (`tile/TileWitherForge.java`,
lines 28 to 91), the airflow check positions and slag positions (lines 94 to 133), the facing default
(lines 108 to 115), the client drips and the scary sounds (lines 136 to 194), and the recipe type
(lines 201 to 204). One block entity class takes the device's numbers and its side count from the
block.

| 1.12 construct | 1.21 replacement |
|---|---|
| `TileEntityDataBase` parent with ten tile data fields synced: input, output, fuel handlers, `recipeProgress` (every 20 ticks), `speed`, `active`, `fuelCount`, `burnTime`, `airflow`, `ashCount` (`tile/TileBloomery.java`, lines 62 to 67, 83 to 95, 124 to 148) | `SyncedBlockEntity`. Property: `type` for `active`. Snapshot: input, output, fuel, `fuelCount`, `burnTime`, `ashCount`, `airflow`, because the renderer reads all seven (`client/render/BloomeryFuelRenderer.java`, lines 38 to 48, 122, 169 to 170). Server-only: `recipeProgress`, `speed`, `airflowBonus`, `lastBurnTime`, `remainingSlag`, `currentRecipe`, `checkLight`, and the wither forge's `soundTickCounter`. Only Waila and TOP read progress and speed (`plugin/waila/delegate/BloomeryProviderDelegate.java`). |
| `InputStackHandler`: one slot, limit `CAPACITY` (1, wither forge 3), refuses items without a recipe and refuses when the output holds a bloom (lines 1066 to 1095); observer runs `updateRecipe` (lines 110 to 113) | `ItemStackHandler(1)` (NF-SRC `net/neoforged/neoforge/items/ItemStackHandler.java`, line 17) with `getSlotLimit` (line 125) returning the capacity and `insertItem` (line 55) with the two refusals; `onContentsChanged` re-resolves the recipe through a `RecipeManager.CachedCheck` (NF-SRC `net/minecraft/world/item/crafting/RecipeManager.java`, line 180) and calls `sync()`. The capacity bakes (config section). |
| `OutputStackHandler`: one slot, limit 1 (lines 1097 to 1111) | `ItemStackHandler(1)` with limit 1. |
| `FuelStackHandler extends DynamicStackHandler(1)`: refuses fuel while the input is empty, refuses items with a container item, refuses past `FUEL_CAPACITY_ITEMS` 16, refuses non-fuel; with `HAS_SPEED_CAP` refuses when `burnTime` would pass `FUEL_CAPACITY_BURN_TIME`; adds `burnTime * count` and `fuelCount` on insert, and stores the items only when the device is not active; trims a stack to the room left; `extractItem` takes from the last filled slot and subtracts burn time only when not active (lines 1113 to 1245) | STORAGE's `LargeStackHandler` is not needed: 16 items of at most one stack each fit a stock `ItemStackHandler(16)` with limit 1 per slot and a last-slot `extractItem`, the wood rack shape (STORAGE). Fuel validity: `stack.getBurnTime(null) > 0` (NF-SRC `net/neoforged/neoforge/common/extensions/IItemStackExtension.java`, line 86) and `!stack.hasCraftingRemainingItem()` (line 73), which is `hasContainerItem`. The burn-time pool and the active-device rule port as written. |
| `getItemBurnTime`: `StackHelper.getItemBurnTime(stack) * getSpecialFuelBurnTimeModifier(stack)` (lines 297 to 305) | `stack.getBurnTime(null)` reads the `neoforge:furnace_fuels` data map (NF-SRC `net/neoforged/neoforge/registries/datamaps/builtin/NeoForgeDataMaps.java`, line 67), times the modifier from bloomery's data map (config section). |
| `setActive`: only when not active, the input is not empty, `burnTime > 0`, and a recipe matched; clears the stored fuel items and sets `remainingSlag = slagCount * inputCount` (lines 184 to 194) | `ignite()` with the same four tests, setting `type` to `bottom_lit`. Clearing the fuel items is why the fuel handler stores items only while inactive: once lit, the fuel is a number. |
| `updateSpeed`: `speed = SPEED_SCALAR * sqrt(burnTime / FUEL_CAPACITY_BURN_TIME) * (sqrt(airflow - 0.19 * airflow) + 0.1)` (lines 357 to 369) | Same arithmetic, server-only. |
| `updateAirflow`: for each check position (the front block; the wither forge adds the two sides, `TileWitherForge.java` lines 94 to 105) add 1 for air, the dead slag-heap branch, 0 when the block's face toward the device is solid, else 0.5; multiply by `1 - 0.75 * (fuelCount / 16)^2`; add `airflowBonus`; then `updateSpeed` (lines 371 to 429) | Same loop with `state.isAir()` (NF-SRC `BlockBehaviour.java`, line 618) and `state.isFaceSturdy(level, pos, facing.getOpposite())` (line 944). Decision 8 covers the dead branch. Runs on `neighborChanged`, on fuel change, and after a bellows push. |
| `consumeAirflow(airflow, simulate)`: `airflowBonus += airflow * AIRFLOW_MODIFIER`, then `updateAirflow`; returns 0 (lines 432 to 440); `hasCapability`/`getCapability` for `ModuleCore.CAPABILITY_AIRFLOW_CONSUMER` (lines 321 to 337); the interface in `src/api/java/com/codetaylor/mc/pyrotech/IAirflowConsumerCapability.java` (line 18) | Core's plain interface (SIGN-OFFS, issue #22 item 7), one method `float consumeAirflow(float airflow, boolean simulate)`, implemented by the block entity. No capability registration. The bellows (`modules/tech/machine/tile/TileBellows.java`, lines 150 to 173) does `level.getBlockEntity(pos.relative(facing)) instanceof AirflowConsumer` (findings, machine). |
| `update`, client: when active, 50 percent per tick, crackle at 10 percent and a drip at the front (lines 454 to 470, 552 to 597); the wither forge adds drips on both sides (`TileWitherForge.java`, lines 140 to 156) | A client `BlockEntityTicker` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntityTicker.java`, line 9) reading `type` from the state. |
| `update`, server: drag `airflowBonus *= 1 - AIRFLOW_DRAG_MODIFIER` per tick with an epsilon floor; recompute airflow while it is negative (first tick); recompute speed when burn time changed (lines 472 to 489) | Same in the server ticker. |
| Active loop: with no recipe, reset and deactivate; `recipeProgress += speed / recipe.timeTicks`; when progress crosses the next slag interval (`slag = slagCount * count`, intervals of `1 / slag` offset by half), `remainingSlag -= 1` and `createSlag()`; at progress 0.9999: ash `+1` plus one more per remaining fuel item at `ASH_CONVERSION_CHANCE` 0.35, capped at `MAX_ASH_CAPACITY` 16; output `recipe.getUniqueBloomFromOutput(count)`; extract the input, insert the bloom, zero burn time, fuel count, progress, deactivate, recompute airflow (lines 491 to 549) | Same loop. The recipe time is the JSON value times the `SERVER` duration multiplier (config section). |
| `createSlag()`: the front position (lines 599 to 603); the wither forge picks one of three positions per input item (`TileWitherForge.java`, lines 118 to 133). `createSlag(pos)`: if a slag heap of level below 4 stands there, add one slag to its handler and raise its level, molten; else walk down to the first non-replaceable block, destroy the replaceable blocks in between with drops, raise an existing heap below level 8 or place a new level-1 molten heap above it, and insert the recipe's slag item (lines 605 to 702) | Same, with `state.canBeReplaced()` (NF-SRC `BlockBehaviour.java`, line 861), `level.destroyBlock(pos, true)` (NF-SRC `Level.java`, line 317), and the heap block's level helpers (slag section). `addSlagItemToTileEntity` also stamps `lastMolten` with `getGameTime()` (NF-SRC `Level.java`, line 998). |
| `onTileDataUpdate`: re-resolve the recipe on input change; block update and light check on `active` change (lines 709 to 719) | `onSyncedDataUpdate` re-resolves the recipe for the renderer; light and render follow the `type` property. |
| `writeToNBT`/`readFromNBT` with the three handlers and eight fields (lines 727 to 760) | `saveAdditional`/`loadAdditional` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntity.java`, lines 94 and 77) plus `saveSynced`/`loadSynced` for the snapshot fields. |
| `dropContents`: not active, drop the input; active and `DROP_SLAG_WHEN_BROKEN`, drop `remainingSlag` of the recipe's slag item and lose the input; always drop the output and the fuel (lines 777 to 797) | Same from `onRemove`, with `Containers.dropItemStack`. The input is lost when broken while lit, as in 1.12. |
| `isExtendedInteraction`: the position above is part of this tile (lines 817 to 824); `getTileFacing` (lines 827 to 834) | The top-half delegation in `useItemOn` (block section). Facing is the state's property. |
| `InteractionItem` on `UP` in the top's 2 to 14 px box (lines 841 to 848): the tongs' `IInteractionItem` hook | Nothing in the block. The tongs act through `Item#useOn` after the block chain returns `PASS` (tongs section). |
| `InteractionIgnite`: flint and steel or fire charge, not active, `UP`; `setActive`, consume the fire charge, `FLINTANDSTEEL_USE` at random pitch (lines 852 to 891) | The first branch of the chain: exact item tests, as the campfire and torches do (TECH-BASIC campfire row; IGNITION), `SoundEvents.FLINTANDSTEEL_USE` (line 530). Igniter items never reach the chain (block section). |
| `InteractionShovel` on `UP`: with ash and a shovel, `ashCount -= 1`, drop one pit ash, damage the shovel, `SAND_BREAK` (lines 1023 to 1055) | Second branch: `stack.canPerformAction(ItemAbilities.SHOVEL_DIG)` (NF-SRC `net/neoforged/neoforge/common/ItemAbilities.java`, line 33), core's `pit_ash` item, `hurtAndBreak(1, player, slot)` (NF-SRC `net/minecraft/world/item/ItemStack.java`, line 490), `SoundEvents.SAND_BREAK` (line 1190). |
| `InteractionInput` over the input and output handlers on `UP`: enabled when there is no fuel, the device is not active, and there is no ash; validates by recipe; the transform is the upper one (24/16) while both slots are empty, else the lower (5/16) at half scale (lines 895 to 945, 71 to 81) | Third branch: insert with the cached recipe check, or extract the output then the input on an empty hand. The two transforms move to the renderer. |
| `InteractionFuel` on `UP`: enabled while the input is not empty and there is no ash; validates furnace fuel without a container item; renders through `BloomeryFuelRenderer` (lines 949 to 1019) | Fourth branch: insert into the fuel handler; extract from it on an empty hand. |
| `Stages` gamestages hook (lines 805 to 808; `TileWitherForge.java`, lines 212 to 215) | Dropped. |
| `shouldRenderInPass` both passes (lines 767 to 770) | The renderer draws solid and translucent geometry in one `render` call (NF-SRC `net/minecraft/client/renderer/blockentity/BlockEntityRenderer.java`, line 12). |
| Wither forge scary sounds: every `SCARY_SOUND_INTERVAL_TICKS` 800 plus or minus `VARIANCE` 400 while active, a weighted pick (ghast scream 1, ghast ambient 5, ghast warn 5, wither skeleton hurt 5, wither hurt 5, wither skeleton ambient 10, wither ambient 25, wither skeleton step 50) at volume 0.4 to 0.6 and pitch 0.4 to 1.4 (`TileWitherForge.java`, lines 159 to 192) | `SimpleWeightedRandomList.builder().add(sound, weight)...build().getRandomValue(random)` (NF-SRC `net/minecraft/util/random/SimpleWeightedRandomList.java`, lines 23, 46, 35) over `SoundEvents.GHAST_SCREAM`, `GHAST_AMBIENT`, `GHAST_WARN`, `WITHER_SKELETON_HURT`, `WITHER_HURT`, `WITHER_SKELETON_AMBIENT`, `WITHER_AMBIENT`, `WITHER_SKELETON_STEP` (lines 596, 593, 598, 1550, 1546, 1548, 1543, 1551), gated by the `ENABLE_SCARY_SOUNDS` toggle. |

**The fuel renderer.** `BloomeryFuelRenderer.renderSolidPass` draws one quad inset 3 px at
`9 px * max(fuelLevel, ashLevel) + 14 px`, textured with the coal block's top while fuel is stored,
`ash_block` while only ash is stored, else `active_pile`, lit from the block above; when active it draws
the fire block model at 10/16 scale on that quad, and when airflow exceeds 1 a second fire scaled by
`2 * ((0.25 * (airflow + 3) - 1) / (0.25 * (airflow + 3)))`, clamped to 1 (`client/render/BloomeryFuelRenderer.java`,
lines 34 to 155). `renderAdditivePass` draws the held fuel as a ghost when the device is not full and
the held item's burn time fits (lines 163 to 189). In 1.21 this is the bloomery type's `BlockEntityRenderer`:
the quad through the block atlas sprites (bootstrap table), the fire through
`BlockRenderDispatcher#renderSingleBlock(Blocks.FIRE.defaultBlockState(), ...)` (NF-SRC
`net/minecraft/client/renderer/block/BlockRenderDispatcher.java`, line 130), the input, output, and ghost
items through `ItemRenderer#renderStatic` (NF-SRC `net/minecraft/client/renderer/entity/ItemRenderer.java`,
line 228) with the two 1.12 transforms, and the ghost from `Minecraft.getInstance().hitResult` (NF-SRC
`Minecraft.java`, line 355) as PROTOTYPE describes.

**The drip particle.** `ParticleBloomeryDrip` is a copy of the 1.12 lava particle with the lava sprite
(index 49), scale times `rand * 2 + 0.2`, full-bright, a smoke trail that thins with age, gravity 0.015
per tick, and drag 0.999 (`client/particles/ParticleBloomeryDrip.java`, lines 19 to 80). It is spawned
client-side through `effectRenderer.addEffect` (`BlockBloomery.java`, line 491). In 1.21 it is a
`SimpleParticleType` `pyrotech:bloomery_drip` registered under `Registries.PARTICLE_TYPE` (NF-SRC
`net/minecraft/core/particles/SimpleParticleType.java`, line 11; `net/minecraft/core/registries/Registries.java`,
line 175), a `TextureSheetParticle` subclass (NF-SRC `net/minecraft/client/particle/TextureSheetParticle.java`,
line 9) that copies vanilla's `LavaParticle` shape (NF-SRC `net/minecraft/client/particle/LavaParticle.java`:
lifetime at line 20, smoke trail at lines 43 to 48, size at lines 37 to 38, light at line 29) with the
1.12 numbers, a provider registered in `RegisterParticleProvidersEvent#registerSpriteSet` (NF-SRC
`net/neoforged/neoforge/client/event/RegisterParticleProvidersEvent.java`, line 82), a
`particles/bloomery_drip.json` naming the vanilla lava sprite, and `level.addParticle` at the spawn
sites (NF-SRC `Level.java`, line 530). Decision 9 asks whether vanilla's `ParticleTypes.LAVA` is close
enough instead.

## The bloom

The bloom is a block with a tile, an `ItemBlock` subclass with a custom item entity, and a helper
class. Its five fields are `maxIntegrity`, `integrity`, `experiencePerComplete`, `recipeId`, and
`langKey` (`tile/TileBloom.java`, lines 44 to 52; `util/BloomHelper.java`, lines 140 to 155). They
become the `pyrotech:bloom` data component (SIGN-OFFS, issue #21 item 3).

| 1.12 construct | 1.21 replacement |
|---|---|
| `BlockPartialBase`, `Material.ROCK`, hardness 7.5, resistance 30, pickaxe 1, random ticks, no creative tab, box 4 to 12 px by 8 tall (`block/BlockBloom.java`, lines 48 to 63, 72 to 75, 52, 153 to 156) | `Properties.of().mapColor(STONE).strength(7.5f, 30f).requiresCorrectToolForDrops().randomTicks().noOcclusion()` (NF-SRC `BlockBehaviour.java`, lines 1195, 1298, 1208), tags `mineable/pickaxe` and `needs_stone_tool` (NF-SRC `BlockTags.java`, lines 141 and 146), `getShape` box. |
| `getLightValue` 9 (lines 83 to 93) | `lightLevel(state -> 9)`. This is also what the anvil reads through the held block item's default state (TECH-BASIC anvil row). |
| `onEntityWalk`: hot floor `ENTITY_WALK_DAMAGE` 3, no fire (lines 101 to 111) | `stepOn` (shared shapes). The number is the one `AnvilHotItem.walkDamage()` returns (SIGN-OFFS, issue #35 item 7), so the block reads the same constant or config value the item returns (config section). |
| `removedByPlayer` and `harvestBlock` keeping the tile alive for `getDrops`, which drops `BloomHelper.toItemStack(tile)` only when integrity is above 0 (lines 131 to 147, 175 to 189) | Loot table with `CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY)` (NF-SRC `net/minecraft/world/level/storage/loot/functions/CopyComponentsFunction.java`, lines 77 and 121), which copies what `collectImplicitComponents` emits (NF-SRC `BlockEntity.java`, line 322). The integrity test moves to the one code path that destroys a spent bloom: `level.destroyBlock(pos, integrity > 0)` (NF-SRC `Level.java`, line 317). A creative bloom with no data (`TileBloom.java`, lines 188 to 191) mined by hand would drop an empty-data bloom instead of nothing; accepted. |
| `canPlaceBlockAt`: the block below must be side-solid upward (lines 159 to 167) | `canSurvive` (NF-SRC `BlockBehaviour.java`, line 345) with `level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)` (line 944). |
| `updateTick`: `checkFire` then `checkFall`, on random ticks and on the scheduled tick that `onBlockAdded` and `neighborChanged` request at `tickRate` 2 (lines 197 to 201, 229 to 246) | `randomTick` (line 384) and `tick` (line 387) both calling the two checks; `onPlace` (line 190) and `neighborChanged` (line 186) calling `level.scheduleTick(pos, this, 2)` (NF-SRC `net/minecraft/world/level/LevelAccessor.java`, line 52). |
| `checkFall`: when the block below is air or fall-through, spawn an `EntityFallingBlock` carrying `tileEntityData`, whose `entityDropItem` override writes that data into the dropped bloom stack; the world-gen `fallInstantly` path moves the state down (lines 248 to 308) | `FallingBlockEntity.fall(level, pos, state)` (NF-SRC `net/minecraft/world/entity/item/FallingBlockEntity.java`, line 82) with `blockData = saveWithoutMetadata(registries)` (line 62; `BlockEntity.java`, line 112), which vanilla loads back into the landed block entity (lines 182 to 192). The bloom block implements `Fallable` (NF-SRC `net/minecraft/world/level/block/Fallable.java`, line 10) and its `onBrokenAfterFall` (line 14) drops the bloom stack built from `blockData` with the component, with `dropItem = false` (line 56) so vanilla does not drop a plain bloom. `FallingBlock.isFree` (NF-SRC `net/minecraft/world/level/block/FallingBlock.java`, line 58) is `canFallThrough`. |
| `checkFire`: the six neighbours get vanilla's `tryCatchFire` odds (300 sides, 50 below, 250 above, minus 50 in high humidity) with age 0; a flammable block below is set on fire outright; then `BloomHelper.trySpawnFire` at `FIRE_SPAWN_CHANCE_RANDOM` 0.25 (lines 314 to 362) | The same loop copied from `FireBlock.checkBurnOut`, which is private (NF-SRC `net/minecraft/world/level/block/FireBlock.java`, lines 262 to 268), reading `state.getFlammability(level, pos, face)` (NF-SRC `net/neoforged/neoforge/common/extensions/IBlockStateExtension.java`, line 486), `level.isRainingAt` (NF-SRC `Level.java`, line 1074), `BaseFireBlock.getState` with `FireBlock.AGE` (NF-SRC `BaseFireBlock.java`, line 41; `FireBlock.java`, line 36), and `state.onCaughtFire` for the TNT case (NF-SRC `IBlockExtension.java`, line 673). Humidity is `level.getBiome(pos).is(BiomeTags.INCREASED_FIRE_BURNOUT)` (NF-SRC `FireBlock.java`, lines 198 to 199; `net/minecraft/tags/BiomeTags.java`, line 63). Note the bloom's own odds for below are 50, not vanilla's 250. |
| `randomDisplayTick`: crackle 10 percent and four flames within 0.3 of the block centre at y 4 to 6 px (lines 205 to 221) | `animateTick` (shared shapes). |
| `ItemBlockBloom`: stack size 1; `getMaxIntegrity`, `getIntegrity`, `getExperiencePerComplete`, `setIntegrity` reading and writing the `BlockEntityTag` (lines 386 to 448) | `BlockItem` with `stacksTo(1)` (NF-SRC `Item.java`, line 452). The accessors read the `pyrotech:bloom` component through `stack.get(type)` and write with `stack.set(type, record.withIntegrity(n))`. The component is a record `BloomData(int maxIntegrity, int integrity, float experiencePerComplete, ResourceLocation recipeId, List<String> langKeys)` registered with `DeferredRegister.DataComponents#registerComponentType` (NF-SRC `DeferredRegister.java`, line 667) with a persistent codec and a network codec, moved between block entity and stack by `collectImplicitComponents` and `applyImplicitComponents` (NF-SRC `BlockEntity.java`, lines 322 and 293; SIGN-OFFS, issue #21 item 3). |
| `hasCustomEntity` true, `createEntity` returning an `EntityItemBloom` with the thrower's motion and a 40-tick pickup delay (lines 451 to 470); `EntityItemBloom`: never despawns, invulnerable, fire immune, and every 5 client ticks a flame particle with a 10 percent crackle (lines 544 to 596) | Decision 4. Recommended: no entity type. `Item.Properties#fireResistant()` (NF-SRC `Item.java`, line 472) sets `FIRE_RESISTANT` (NF-SRC `DataComponents.java`, line 120), which `ItemEntity#fireImmune` reads (NF-SRC `net/minecraft/world/entity/item/ItemEntity.java`, line 288). `IItemExtension#createEntity` (NF-SRC `IItemExtension.java`, lines 233 and 249) still exists and can return a vanilla `ItemEntity` with `setUnlimitedLifetime()`, `setInvulnerable(true)`, and `setPickUpDelay(40)` (NF-SRC `ItemEntity.java`, lines 492 and 484; `Entity.java`, line 2696). `IItemExtension#onEntityItemUpdate` (line 261), called first in `ItemEntity#tick` (line 132), spawns the client particles. |
| `getItemStackDisplayName`: with one lang key, `"%s Bloom"` of that key; with several `;`-separated keys, fold them so each later key formats the previous one; else the plain name (lines 475 to 522) | `Item#getName(ItemStack)` (NF-SRC `Item.java`, line 339) building `Component.translatable("block.pyrotech.bloom_unique", inner)` from the component's key list. The key on `main` is `block.pyrotech.bloom_unique` (`en_us.json`, line 148). The stored keys become translation keys of 1.21 items (`block.minecraft.iron_ore`), written by datagen into the recipe's optional `lang_key` field (RECIPES). |
| `onUpdate`: every 20 ticks on the server, `IN_FIRE` damage `FIRE_DAMAGE_PER_SECOND` 3 and one second of fire on the carrier (lines 526 to 542) | `Item#inventoryTick` (NF-SRC `Item.java`, line 297) with `level.getGameTime() % 20`, `entity.hurt(level.damageSources().inFire(), 3)` (NF-SRC `DamageSources.java`, line 85), `igniteForSeconds(1)`. |
| `TileBloom`: `recipeProgress` and `integrity` synced, the other three plain, two interactions (`tile/TileBloom.java`, lines 44 to 74) | A plain `BlockEntity`. Nothing on the client reads either synced field (only Waila's `BloomProviderDelegate`), so no `SyncedBlockEntity`. |
| `InteractionItem` on any face: the tongs' hook (lines 152 to 159) | The tongs' `useOn` (tongs section). The block's `useItemOn` returns `PASS_TO_DEFAULT_BLOCK_INTERACTION` for a tongs item. |
| `InteractionHit.allowInteraction`: the held item is a hammer when `HAMMERS.getHammerHarvestLevel(id) > -1` (lines 170 to 183) | `stack.is(#pyrotech:hammers)` (CORE). |
| `doInteraction`: a creative bloom (no recipe id) fails; hunger gate `MINIMUM_HUNGER_TO_USE` 3 with `SCPacketNoHunger` through tech/basic's channel; exhaustion `EXHAUSTION_COST_PER_HIT` 1; `STONE_HIT` at 0.75 and random pitch; `trySpawnFire` at `FIRE_SPAWN_CHANCE_ON_HIT_RAW` 0.1; progress `+= hammerPower / max(1, HAMMER_HITS_REQUIRED)` with the hammer at the player's half eye height; at progress 0.9999: integrity minus one unless fortune saves it, `getRandomOutput` of the recipe resolved by id, XP `experiencePerComplete` spawned, `STONE_BREAK`, `destroyBlock(pos, true)`, and with `BREAKS_BLOCKS` a `1 - (clamp(hardness, 0, 50) / 60)^(1/8)` chance to destroy the block below without drops; client: eight lava particles at the hit (lines 186 to 299) | The hammer branch of `useItemOn`. Hunger through core's payload (SIGN-OFFS, issue #19 follow-on 4). `causeFoodExhaustion` (NF-SRC `net/minecraft/world/entity/player/Player.java`, line 1793). `SoundEvents.STONE_HIT` and `STONE_BREAK` (lines 1370 and 1366). Recipe by `level.getRecipeManager().byKey(id)` (NF-SRC `RecipeManager.java`, line 137). XP through `ExperienceOrb.award(level, Vec3, n)` (NF-SRC `net/minecraft/world/entity/ExperienceOrb.java`, line 148) after the 1.12 fractional roll (`library/util/ExperienceHelper.java`, lines 10 to 34). Hardness is `state.getDestroySpeed(level, pos)` (NF-SRC `BlockBehaviour.java`, line 686). The lava particles are the client side of `useItemOn` with `ParticleTypes.LAVA`. The hit counts and the power table are `SERVER` config (config section). |
| `BloomHelper.shouldReduceIntegrity`: with fortune, keep integrity at `CHANCE_TO_NOT_CONSUME_BLOOM_INTEGRITY_PER_FORTUNE_LEVEL[level]` {0.15, 0.30, 0.45} (lines 35 to 49) | `stack.getEnchantmentLevel(holder)` (NF-SRC `IItemStackExtension.java`, line 162) with the holder from `level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE)` (NF-SRC `net/minecraft/core/RegistryAccess.java`, line 36; `net/minecraft/core/Registry.java`, line 153; `net/minecraft/world/item/enchantment/Enchantments.java`, line 103). Past-the-end rule on the array. |
| `calculateHammerPower`: 0 unless the item is a hammer; distance from the bloom's centre at y 0.25 to the hammer position, `max(0, 1 - (max(1, d) - 1)^3 / 4)`, snapped to 1 above 0.985; times `HAMMER_POWER_MODIFIER_PER_HARVEST_LEVEL[level]` {0.7, 1, 2, 3}; times `PER_EFFICIENCY_LEVEL[eff - 1]` {1.25, 1.5, 1.75, 2, 2.25}; times 1.3 with strength, 0.7 with weakness, 0.1 with mining fatigue (lines 51 to 110) | Same, with the hammer's level from core's `pyrotech:tool_levels` (SIGN-OFFS, issue #21 item 2), `Enchantments.EFFICIENCY` (line 100), and `player.hasEffect(MobEffects.DAMAGE_BOOST)`, `WEAKNESS`, `DIG_SLOWDOWN` (NF-SRC `net/minecraft/world/entity/LivingEntity.java`, line 958; `net/minecraft/world/effect/MobEffects.java`, lines 47, 72, 37). The three arrays and three scalars stay config (config section). Used by the ground hit, the anvil recipe, and Waila. |
| `createBloomAsItemStack`, `toItemStack`, `writeToNBT` (lines 112 to 155) | Constructors of the `BloomData` record and `stack.set(component, data)`; `BlockEntity#saveToItem` (NF-SRC `BlockEntity.java`, line 147) is not needed once the component carries everything. |
| `trySpawnFire`: at the given chance, the nearest player within 2 blocks whose feet block is not air or whose floor is not sturdy is set on fire for 1 or 2 seconds and takes 0 to 4 hot floor damage; then the 5 by 3 by 5 cube around the position is shuffled and the first air block over a sturdy block gets fire (lines 157 to 197) | `level.getNearestPlayer(x, y, z, 2, false)` (NF-SRC `net/minecraft/world/level/EntityGetter.java`, line 107), `BlockPos.betweenClosed` (NF-SRC `net/minecraft/core/BlockPos.java`, line 397) collected and shuffled, `BaseFireBlock.getState`. |
| `getCompareString`: the recipe id for JEI subtypes (lines 273 to 278) | The component's `recipeId` (assets and JEI section). |

Snapshot: none. The bloom's state is the component and the server-only progress.

## The bloom on the anvil

Settled by SIGN-OFFS, issue #21 item 3, and issue #19 follow-ons 3 to 5. This section records what
the 1.12 class does so the port has the numbers.

| 1.12 construct | 1.21 replacement |
|---|---|
| One `BloomAnvilRecipe` registered per bloomery and wither forge recipe under the same id, with the bloom as an NBT ingredient, `HAMMER_HITS_IN_ANVIL_REQUIRED` 8 hits, type `HAMMER`, the recipe's anvil tiers (`init/recipe/BloomeryRecipesAdd.java`, lines 163 to 192; `WitherForgeRecipesAdd.java`, lines 62 to 73) | One datagen recipe `pyrotech:anvil/bloom` on the `pyrotech:bloom_anvil` serializer. Its `matches` reads the `pyrotech:bloom` component, resolves the recipe through `byKey`, checks that the tool is a hammer and the anvil tier is in the recipe's set. Hits come from the `SERVER` config value. |
| `matches`: type must be hammer, item must be the bloom, the `BlockEntityTag.recipeId` must equal this recipe's id (`recipe/BloomAnvilRecipe.java`, lines 46 to 76) | The component read above. `DataComponentIngredient` (NF-SRC `net/neoforged/neoforge/common/crafting/DataComponentIngredient.java`, line 99) is not needed because the match is by code, not by ingredient. |
| `applyDamage`: with `useDurability`, durability minus one and minus `getBloomAnvilExtraDamagePerHit` at `getBloomAnvilExtraDamageChance`; then `trySpawnFire` at `FIRE_SPAWN_CHANCE_ON_HIT_IN_ANVIL` 0.05 (lines 79 to 91) | The `ExtendedAnvilRecipe` hook with the seven public anvil accessors (issue #19 follow-on 4: `useDurability`, `getDurabilityUntilNextDamage`, `setDurabilityUntilNextDamage`, `getBloomAnvilExtraDamageChance`, `getBloomAnvilExtraDamagePerHit`, `getStackHandler`, `getRecipeTier`; `modules/tech/basic/tile/spi/TileAnvilBase.java`, lines 161 to 205). |
| `getModifiedRecipeProgressIncrement`: increment times `calculateHammerPower` (lines 94 to 97) | Same hook. |
| `onRecipeCompleted`: one output per whole unit of progress while the bloom remains, leftover progress kept; each unit rolls `getRandomOutput`, reduces integrity unless fortune saves it, re-inserts the bloom or drops it when spent, and spawns the XP (lines 100 to 145) | Same hook, with the integrity written back into the component. |
| `onAnvilHitClient`: eight lava particles when the slot holds a bloom (lines 148 to 160) | The client handler of tech/basic's `AnvilHitPayload` (TECH-BASIC network table). |
| `onAnvilDurabilityExpired`: destroy the anvil; if the slot held a bloom and the bloom can stand there, place it with its data, else drop it (lines 163 to 198) | Same hook, placing the bloom block and calling `applyComponentsFromItemStack` (NF-SRC `BlockEntity.java`, line 296). |
| `JEIRecipeWrapperAnvil`'s bloom branch listing failure items and drawing `min-max` yield (`modules/tech/basic/plugin/jei/wrapper/JEIRecipeWrapperAnvil.java`, lines 49 to 60, 140 to 147) | Bloomery's JEI plugin synthesizes one anvil entry per bloomery and wither forge recipe (assets and JEI section). |
| `BlockAnvilBase.hasBloom`, `onEntityWalk`, `randomDisplayTick`, `getLightValue` (`modules/tech/basic/block/spi/BlockAnvilBase.java`, lines 59 to 75, 81 to 117, 239 to 258) | `AnvilHotItem { float walkDamage(); }` implemented by the bloom item (SIGN-OFFS, issue #35 item 7); light through the block item's default state (TECH-BASIC). |
| `AnvilProviderDelegate`'s bloom lines (Waila) | Dropped. |

## The tongs

Fourteen items: seven `ItemTongsEmpty*` and seven `ItemTongsFull*`, each pair sharing one config
durability (`item/ItemTongsEmptyBone.java`, lines 12 to 15; `ItemTongsFullBone.java`, lines 12 to 15;
`ModuleTechBloomeryConfig.java`, lines 568 to 610: stone 4, flint 4, bone 4, iron 16, gold 2, obsidian
57, diamond 64). The gold empty tongs override enchantability to gold's 22 (`ItemTongsEmptyGold.java`,
lines 21 to 24). Each empty item holds a supplier of its full twin and back (`item/spi/ItemTongsEmptyBase.java`,
lines 33 to 46; `ItemTongsFullBase.java`, lines 37 to 50).

| 1.12 construct | 1.21 replacement |
|---|---|
| Two items per material, stack 1, `setMaxDamage(durability)` (`ItemTongsEmptyBase.java`, lines 35 to 41; `ItemTongsFullBase.java`, lines 39 to 45) | Decision 3. Recommended: one `TongsItem` per material with `Item.Properties().stacksTo(1).durability(n)` (NF-SRC `Item.java`, lines 452 and 456), where "full" means the stack carries the `pyrotech:bloom` component. |
| Empty `isEnchantable` true, full false (`ItemTongsEmptyBase.java`, lines 48 to 52; `ItemTongsFullBase.java`, lines 53 to 56); gold enchantability 22 | `isEnchantable(stack)` returning `!stack.has(BLOOM)` (NF-SRC `Item.java`, line 355); gold's `getEnchantmentValue` 22 (line 367). |
| Empty `allowInteraction`: an anvil holding a bloom, a bloomery whose output is a bloom, or a bloom block; `doInteraction`: extract the bloom, `createItemTongsFull` merging the bloom's tag into a new full item, shrink the empty, give the full to the player at the current slot; the bloom block is set to air (lines 55 to 99) | `Item#useOn` (NF-SRC `Item.java`, line 153), which runs after the block's `useItemOn` and `useWithoutItem` return without consuming (NF-SRC `net/minecraft/server/level/ServerPlayerGameMode.java`, lines 366 to 389). The three targets: tech/basic's anvil block entity through its public input handler (issue #19 follow-on 4), the bloomery's output handler, and the bloom block. Each block's chain returns `PASS` for a tongs stack: the anvil's insert branch refuses it because no anvil recipe takes tongs, and the bloomery's input branch refuses it for the same reason. The full stack is the same item with the component set; `ItemHandlerHelper.giveItemToPlayer(player, stack, player.getInventory().selected)` (NF-SRC `net/neoforged/neoforge/items/ItemHandlerHelper.java`, line 89; `net/minecraft/world/entity/player/Inventory.java`, line 36). |
| Empty `applyItemDamage` no-op (lines 102 to 105) | Picking up costs nothing; placing costs one durability (next rows). |
| Empty `addInformation`: the full-durability line behind `SHOW_DURABILITY_TOOLTIPS` (lines 109 to 114); full `addInformation`: the bloom's unique name in dark red as line 2 (gold when the tooltip is short), then the durability line (`ItemTongsFullBase.java`, lines 128 to 162) | `appendHoverText` (NF-SRC `Item.java`, line 332). TOOL decision 4 keeps the durability line unconditionally; CORE keeps the client flag. |
| Full `onItemRightClick`: main hand only; ray trace; place the bloom at `pos.offset(side)` when `canPlaceBlockAt`, load the tile from the tag, `createItemTongsEmpty` with damage unless creative, `ITEM_BREAK` when the tongs break (lines 60 to 125) | `Item#use` (NF-SRC `Item.java`, line 165) with `getPlayerPOVHitResult` (line 359), `canSurvive`, `level.setBlock`, `applyComponentsFromItemStack`, then `stack.remove(BLOOM)` and `hurtAndBreak(1, player, slot)`; `SoundEvents.ITEM_BREAK` (line 766) comes from `hurtAndBreak` itself. |
| Full `getItemStackDisplayName` delegating to the empty item (lines 166 to 169) | One item, one name. |
| Full `allowInteraction`: an empty anvil slot; `doInteraction`: insert the bloom from the tag, empty the tongs with damage, give the empty back (lines 172 to 205) | The anvil target in `useOn` when the stack has the component. |
| `BloomHelper.createItemTongsFull` merging the bloom's and the tongs' tags; `createItemTongsEmpty` with `attemptDamageItem(1)` and the tile tag stripped (`util/BloomHelper.java`, lines 210 to 271) | `stack.set(BLOOM, data)` and `stack.remove(BLOOM)`. Enchantments and damage stay on the same stack, which is what the tag merge preserved. |
| `ItemPickupEventHandler`: when a bloom item is picked up and either hand holds empty tongs (main hand first), kill the item entity, cancel the event, and put the full tongs in that hand (`event/ItemPickupEventHandler.java`, lines 29 to 64) | `ItemEntityPickupEvent.Pre` (NF-SRC `net/neoforged/neoforge/event/entity/player/ItemEntityPickupEvent.java`, line 56): set the component on the held stack, `discard()` the entity, `setCanPickup(TriState.FALSE)` (line 70; `net/neoforged/neoforge/common/util/TriState.java`, line 23). The offhand branch is `player.setItemSlot(EquipmentSlot.OFFHAND, stack)` (NF-SRC `Player.java`, line 1895). |
| `PluginJEI` blacklisting the seven full items and the bloom (`plugin/jei/PluginJEI.java`, lines 45 to 54) | With one item per material there is nothing to hide but the bloom (assets and JEI section). |
| Seven crafting recipes ` X `, `SXX`, ` S ` with `S` = stone stick and `X` = the material's shard or brick (`recipes/tech/bloomery/tongs_*.json`; `X` metas 16, 10, 11, 19, 34, 18, 33 are `brick_stone`, `flint_shard`, `bone_shard`, `iron_shard`, `gold_shard`, `diamond_shard`, `obsidian_shard`; `S` meta 27 is `stick_stone`, `modules/core/item/ItemMaterial.java`, lines 91, 85, 86, 94, 109, 93, 108, 102) | Seven `ShapedRecipeBuilder` calls on the flattened core items (CORE). |
| Fourteen item models, seven `_full` (`models/item/tongs_*.json`, `item/handheld`) | Seven models with an `overrides` entry on an `ItemProperties` predicate `pyrotech:full` (NF-SRC `net/minecraft/client/renderer/item/ItemProperties.java`, line 60) pointing at the seven `_full` models on `main` (assets section). |

The tongs read the anvil's slot through a public accessor on the anvil block entity and the bloomery's
output through a public accessor on the bloomery block entity. Both stay public.

## Slag

Slag is one item and one eight-level heap block per ore, plus a plain pair. The pairs come from
`SlagInitializer`, which registers `generated_slag_<ore>` and `generated_pile_slag_<ore>` for every
compat entry (`init/SlagInitializer.java`, lines 160 to 171, 200 to 228), and from the custom slag
JSON, which registers `<name>_custom` pairs (lines 47 to 90). The plain `slag` and `pile_slag` are
registered normally (`init/BlockInitializer.java`, lines 28 to 31; `ItemInitializer.java`, line 15).

| 1.12 construct | 1.21 replacement |
|---|---|
| `JsonSlagList` read from `module.tech.Bloomery.Slag-Custom.json` in the config folder, generating a pair per entry with a `_custom` suffix (`init/JsonSlagList.java`, lines 7 to 64; `SlagInitializer.java`, lines 47 to 90); `docs/orecompat.md` calls it "an artifact from before the ore compat" | Dropped with the runtime JSON system (SIGN-OFFS, issue #22 item 5). It is not the core compat JSON but it is the same mechanism, and it was superseded in 1.12 itself. |
| `initializeSlagFromOreCompat`: one pair per `core.compat.Ore-Custom.json` entry, named from the ore key with `ore` stripped and lowercased, tinted with the entry's `slagColor`, named with the first valid lang key (lines 92 to 173) | Decision 2. Recommended: two registered pairs, `slag_iron` and `pile_slag_iron`, `slag_gold` and `pile_slag_gold`, beside `slag` and `pile_slag`, with the colours from `ModuleCoreConfig.ORE_COMPAT.OREDICT_COLOR_MAP` (`modules/core/ModuleCoreConfig.java`, lines 936 and 934: iron `d8af93`, gold `fcee4b`; plain `676767` from `SlagInitializer.java`, line 286). The other fifteen colour entries (lines 930 to 946) are for other mods' ores and die. |
| `ItemSlag.getItemStackDisplayName`: `"%s Slag"` of the lang key (`item/ItemSlag.java`, lines 19 to 36) | `Item#getName` with `item.pyrotech.slag_unique` (`en_us.json`, line 139) and `block.minecraft.iron_ore`. The plain slag keeps `item.pyrotech.slag` (line 138). |
| `ItemColor` tint index 1 on the slag item (the `slag_overlay` layer, `models/item/slag.json`), tint index 0 on the heap item, `BlockColor` tint index 0 on the heap (`SlagInitializer.java`, lines 271 to 348) | `RegisterColorHandlersEvent.Item#register(ItemColor, items)` and `.Block#register(BlockColor, blocks)` (NF-SRC `RegisterColorHandlersEvent.java`, lines 113 and 65; `net/minecraft/client/color/item/ItemColor.java`, line 9; `client/color/block/BlockColor.java`, line 12) with one constant per registered pair. |
| `BlockPileSlag extends BlockPileBase`: `LEVEL` 0 to 7 (level 8 minus the property), `MOLTEN`, `Material.ROCK`, 1.75 and 40, pickaxe 1, stone sound, random ticks; boxes 2 px per level; side-solid `DOWN` always and all sides at level 8; `isTopSolid` at level 8 (`block/BlockPileSlag.java`, lines 42 to 56, 240 to 261; `library/spi/block/BlockPileBase.java`, lines 29 to 82, 151 to 176) | `IntegerProperty` `level` 0 to 7 and `BooleanProperty` `molten` (NF-SRC `IntegerProperty.java`, line 56; `BooleanProperty.java`, line 19); the converted `pile_slag.json` keys on both. `strength(1.75f, 40f).requiresCorrectToolForDrops().randomTicks().noOcclusion()`, `mineable/pickaxe`, `needs_stone_tool`, `getShape` by level. `isFaceSturdy` follows from the shape. |
| `getLightValue` 6 when molten (lines 76 to 96) | `lightLevel(state -> state.getValue(MOLTEN) ? 6 : 0)`. |
| `updateTick`: a molten heap cools 6,000 ticks after `lastMolten` (lines 104 to 119) | `randomTick` with `level.getGameTime() - lastMolten > 6000`. |
| `getBlockLayer` `CUTOUT` (lines 127 to 130) | `"render_type": "minecraft:cutout"` on the eight `slag_pile_block_*.json` models (issue #27). |
| `randomDisplayTick`: when molten and not covered by a normal cube, four flames within 0.3 at the surface, lava at 5 percent (lines 134 to 164) | `animateTick` with `isSolidRender` for the cover test (NF-SRC `BlockBehaviour.java`, line 704). |
| `onEntityWalk`: `MOLTEN_WALK_DAMAGE` 3 hot floor when molten (lines 172 to 186) | `stepOn` (shared shapes). |
| `harvestBlock`: when molten, `SCPacketParticleLava(pos, level)` to the dimension and a `HARVESTING_PLAYER_FIRE_CHANCE` 0.125 chance of `HARVESTING_PLAYER_FIRE_DURATION_SECONDS` 1 of fire; then `BlockPileBase.harvestBlock` drops one slag from the tile's handler at `level * 2 / 16` and lowers the level or removes the block; `removedByPlayer` in creative lowers the level without a drop; `getDrops` empty (lines 189 to 208; `BlockPileBase.java`, lines 89 to 141) | `IBlockExtension#onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)` (NF-SRC `IBlockExtension.java`, line 240): pop one slag with `Block.popResource` (NF-SRC `Block.java`, line 337) when harvesting, then set the lower level or air, and return false so vanilla does not remove the block. `ServerLevel#sendParticles(ParticleTypes.LAVA, ...)` (NF-SRC `net/minecraft/server/level/ServerLevel.java`, line 1258) replaces the packet (CORE). `noLootTable()` (NF-SRC `BlockBehaviour.java`, line 1218). |
| `ItemBlockPileSlag.placeBlockAt`: a placed heap gets eight of its slag item in the tile (lines 280 to 307); `getItemStackDisplayName` `"%s Slag Heap"` (lines 312 to 329) | `BlockItem#place` (NF-SRC `net/minecraft/world/item/BlockItem.java`, line 57) then fill the handler in `setPlacedBy` (NF-SRC `Block.java`, line 414); `getName` with `block.pyrotech.pile_slag_unique` (`en_us.json`, line 152). The item always places the full block, `getStateForPlacement` returning the default (`BlockPileBase.java`, lines 205 to 208, level 0 which is the full block). |
| `TilePileSlag`: an eight-slot LIFO handler with limit 1 and `lastMolten` (`tile/TilePileSlag.java`, lines 12 to 71) | A plain `BlockEntity` with an `ItemStackHandler(8)` extracting from the last filled slot and a long. Not synced: the tint is per block, not per contents. |

Snapshot: none. Properties: `level`, `molten`.

## Recipes and datagen

The seven 1.12 recipe classes onto the 1.21 classes. RECIPES covers `BloomeryRecipe`.

| 1.12 class | 1.21 class | Type id | Input |
|---|---|---|---|
| `BloomeryRecipe`, `WitherForgeRecipe`, `BloomeryRecipeBase` (`recipe/BloomeryRecipeBase.java`, lines 28 to 74), `BloomeryRecipeBuilderBase`, `BloomeryRecipeBuilder`, `WitherForgeRecipeBuilder` | RECIPES' `BloomeryRecipe`: `Ingredient`, `ItemStack result`, `int burnTime`, `float experience`, `float failureChance`, `int bloomYieldMin`, `int bloomYieldMax`, `int slagCount`, `ItemStack slagItem`, `List<FailureItem>` (stack and weight, lines 231 to 251), `Set<AnvilTier>`, `Optional<String> langKey`; type and serializer as constructor arguments | `pyrotech:bloomery`, `pyrotech:wither_forge` | `SingleRecipeInput` (NF-SRC `net/minecraft/world/item/crafting/SingleRecipeInput.java`, line 5) |
| `BloomAnvilRecipe` (`recipe/BloomAnvilRecipe.java`) | `BloomAnvilRecipe implements ExtendedAnvilRecipe` on the `pyrotech:bloom_anvil` serializer, one instance | `pyrotech:anvil` | tech/basic's `AnvilRecipeInput` |

The recipe class's behaviour: `matches` is `ingredient.test` (lines 180 to 183); `getOutputBloom` is a
bloom at max yield carrying the recipe id, for JEI (lines 86 to 105); `getUniqueBloomFromOutput(count)`
sums one random yield per input item into the integrity and multiplies the experience by the count
(lines 122 to 146; the `replaceAll("\\.slag", "")` at line 139 matches nothing because the ids use
`_slag`); `getRandomOutput` rolls `failureChance`, times `SILK_TOUCH_FAILURE_MODIFIER` 0 with silk
touch, and returns a weighted failure item (empty when the list is empty) or the output (lines 185 to
218); `getLangKeyFrom` takes the first matching stack's name (lines 220 to 229). The `getRecipe` and
`removeRecipes` statics (`recipe/BloomeryRecipe.java`, lines 15 to 30) become the cached check and
datapack overrides (RECIPES). Builder defaults: burn time 21,600, failure chance 0.25, yield 8 to 10,
slag 4 of plain slag, all anvil tiers, experience unset so 0 (`recipe/BloomeryRecipeBuilderBase.java`,
lines 31 to 44); every adder overrides the first four.

The adders become methods of `TechBloomeryRecipes` under RECIPES' `PyrotechRecipeProvider`. Every
recipe 1.12 registers with the default compat data:

| Adder | Recipes | Inputs, outputs, numbers |
|---|---|---|
| `BloomeryRecipesAdd.applyCompatRecipes` (`init/recipe/BloomeryRecipesAdd.java`, lines 41 to 138) | 4: `bloom_from_oreiron`, `bloom_from_oreiron_slag`, `bloom_from_oregold`, `bloom_from_oregold_slag` | Ore bloom (lines 103 to 118): `OreIngredient(oreIron)` to 1 `minecraft:iron_nugget` (the first valid `nuggetIron`, `CompatInitializerOre.java`, lines 84 to 101), 28,800 ticks, experience 0.25, failure 0.25, yield 12 to 15 (`bloomYieldMinMax` default, lines 32 to 33 and 71 to 82), 4 iron slag, failure items plain slag weight 1 and iron slag weight 2, lang key from the compat entry. Slag bloom (lines 121 to 136): the iron slag heap block to 1 iron nugget, 14,400 ticks, experience 0.25, failure 0.25, yield 12 to 15, 2 iron slag, failure items `rock_stone` weight 1 and plain slag weight 2, lang key `<ore>;slag.unique` so the name reads "Iron Slag Bloom". Gold is the same with `minecraft:gold_nugget`. Proposed ids `pyrotech:bloomery/iron`, `bloomery/iron_slag`, `bloomery/gold`, `bloomery/gold_slag`. Inputs become tags (decision 1). |
| `WitherForgeRecipesAdd.apply` (`WitherForgeRecipesAdd.java`, lines 48 to 59) | 1: `bloom_from_obsidian` | `minecraft:obsidian` to 1 `obsidian_shard` (core), 28,800 ticks, experience 0 (never set), failure 0.25 with no failure items so a failed hit yields nothing, yield 8 to 12, 2 plain slag, anvil tier ironclad only, lang key obsidian. Proposed id `pyrotech:wither_forge/obsidian`, input `#c:obsidians` (NF-SRC `net/neoforged/neoforge/common/Tags.java`, line 615; NF-DATA `data/c/tags/item/obsidians.json` holds normal and crying obsidian). |
| `WitherForgeRecipesAdd.registerInheritedRecipes` behind `INHERIT_BLOOMERY_RECIPES` (lines 75 to 83) | 4 copies | The bloomery to wither forge edge of the chain table at multiplier 1 (SIGN-OFFS, issue #21 item 1). The 1.12 transformer drops experience and anvil tiers (scope correction, decision 8). |
| `registerBloomAnvilRecipes` for both registries (`BloomeryRecipesAdd.java`, lines 163 to 192; `WitherForgeRecipesAdd.java`, lines 62 to 73) | 1: `anvil/bloom` | Bloom anvil section. |
| `CompactingBinRecipesAdd` (`CompactingBinRecipesAdd.java`, lines 17 to 43) | 3: `pyrotech_generated_pile_slag_iron_from_pyrotech_generated_slag_iron`, the gold twin, `pyrotech_pile_slag_from_pyrotech_slag` | 8 slag to one heap, on tech/basic's `CompactingBinRecipe` (issue #19 follow-on 4). Proposed ids `pyrotech:compacting_bin/pile_slag`, `pile_slag_iron`, `pile_slag_gold`. The chain table copies them to the mechanical compacting bin. |
| `VanillaFurnaceRecipesAdd` (`VanillaFurnaceRecipesAdd.java`, lines 10 to 18) | 1 | `pile_slag` (the plain heap item) smelts to core's `slag_glass` at 0.1 XP: `SimpleCookingRecipeBuilder.smelting(Ingredient.of(pile_slag), MISC, slag_glass, 0.1f, 200)` (NF-SRC `net/minecraft/data/recipes/SimpleCookingRecipeBuilder.java`, line 93). Bloomery's datagen writes it (scope correction). |
| Crafting JSON (`recipes/tech/bloomery/*.json`) | 9 | `bloomery`: `X X`, `X X`, `XXX` of core's `refractory_brick_block` (7). `wither_forge`: `X X`, `HBH`, `XHX` with `X` = `minecraft:nether_brick` block (4, `minecraft:nether_bricks` in 1.21, DATA `assets/minecraft/blockstates/nether_bricks.json`), `H` = `minecraft:skull` meta 1 (3, `minecraft:wither_skeleton_skull`, DATA `models/item/wither_skeleton_skull.json`), `B` = the bloomery. Seven tongs (tongs section). All through `ShapedRecipeBuilder`. |

Outside the unit, `StoneKilnRecipesAdd` registers `slag_glass`: the plain heap to `slag_glass` at the
stone kiln's default time and failure chance, failure items 4 glass shards and 4 plain slag, behind
the bloomery module guard (`modules/tech/machine/init/recipe/StoneKilnRecipesAdd.java`, lines 89 to
103). Tech/machine's datagen writes it as `pyrotech:stone_kiln/slag_glass` and the chain table copies
it to the brick kiln (scope correction; findings, machine).

**`CompatInitializerOre` and the 1.21 answer.** In 1.12 core wrote `core.compat.Ore-Generated.json`
from `OREDICT_COLOR_MAP` at pre-init, keeping every ore key that had both an `ore*` and a `nugget*`
entry, with the ore items' lang keys and the nugget item strings (`modules/core/init/CompatInitializerOre.java`,
lines 20 to 112), copied it to `Ore-Custom.json` once, and bloomery read the custom file at
registration to generate the slag pairs and the two recipes per ore (`docs/orecompat.md`). The core
sign-off dropped the mechanism and delegated "real cross-mod ore compat" to this ticket (SIGN-OFFS,
issue #22 item 5). The 1.21 answer: datagen writes the iron and gold recipes on `c:` tags, so any mod's
iron or gold ore or raw material that joins `#c:ores/iron` or `#c:raw_materials/iron` blooms into iron
nuggets with iron slag. A new metal needs a datapack that adds one bloomery recipe JSON and one slag
bloom recipe JSON, and either reuses the plain slag or accepts that no per-metal slag exists (decision
2). There is no runtime generation, because the recipe system cannot register at runtime (RECIPES).

**Datagen outputs.** Recipes: the 4 bloomery, 1 wither forge, 4 chain copies, 1 bloom anvil, 3
compacting bin, 1 smelting, and 9 crafting recipes above, 23 files. Tags: `mineable/pickaxe` and
`needs_stone_tool` for the bloom and the three heaps, `mineable/pickaxe` for the bloomery and wither
forge. Data maps: `pyrotech:bloomery_fuels` (config section). Loot: the bloomery and wither forge
bottom-half tables, the bloom's component-copying table, `noLootTable` for the heaps. Advancements:
`bloomery` (parent `pyrotech:refractory_brick`, `inventory_changed` on the bloomery, "Budding Bloom")
and `wither_forge` (parent `pyrotech:enter_the_nether`, "Foul Forge") from `assets/pyrotech/advancements/`
to `data/pyrotech/advancement/` through `InventoryChangeTrigger.TriggerInstance.hasItems` (NF-SRC
`net/minecraft/advancements/critereon/InventoryChangeTrigger.java`, line 63). Lang keys for both are on
`main` (`en_us.json`, lines 544 to 545 and 556 to 557).

## Raw ore and copper

Issue #19 follow-on 1 and SIGN-OFFS, issue #33 item 7 ask what raw ore and copper mean to Pyrotech.
The facts:

- In 1.12 the bloomery's input was the ore block: `new OreIngredient(oreDictKey)` (`BloomeryRecipesAdd.java`,
  line 107), and a mined iron ore dropped itself. Core's tweak did not change ore drops; it swapped
  iron ingots for iron ore in chest loot (`modules/core/ModuleCoreConfig.java`, lines 45 to 57:
  `REPLACE_IRON_INGOTS` with `REPLACE_IRON_INGOTS_WITH` `minecraft:iron_ore`;
  `modules/core/event/LootTableLoadEventHandler.java`), so that found iron also had to go through the
  bloomery.
- In 1.21 an iron or gold ore drops one raw item, more with fortune, and the block itself only with silk
  touch (DATA `data/minecraft/loot_table/blocks/iron_ore.json`, `deepslate_iron_ore.json`, `gold_ore.json`,
  `deepslate_gold_ore.json`). One ore block and one raw item are worth the same in vanilla: each smelts
  to one ingot (DATA `recipe/iron_ingot_from_smelting_iron_ore.json`, `iron_ingot_from_smelting_raw_iron.json`).
  Copper drops 2 to 5 raw copper per block (DATA `copper_ore.json`). Nether gold ore drops 2 to 6 gold
  nuggets, no raw item (DATA `nether_gold_ore.json`).
- The 1.12 yield per ore block: integrity 12 to 15, each hammer completion a nugget at 75 percent or a
  failure item at 25 percent, plus 4 ore slag during the smelt and the failed slag; eight ore slag form
  a heap that blooms again at 12 to 15 (recipes section). Nine nuggets make an ingot
  (DATA `recipe/iron_ingot_from_nuggets.json`, `gold_ingot_from_nuggets.json`, neither in the removal
  list, SKIPS). So one ore block gives roughly one ingot on the first pass and most of a second through
  the slag chain, against exactly one from a vanilla furnace.
- The tags: `#c:ores/iron` is `#minecraft:iron_ores` (iron ore and deepslate iron ore),
  `#c:raw_materials/iron` is raw iron, `#c:nuggets/iron` is the iron nugget, and the gold twins are the
  same shape with `#minecraft:gold_ores` also holding nether gold ore (NF-DATA `data/c/tags/item/ores/iron.json`,
  `raw_materials/iron.json`, `nuggets/iron.json`, `ores/gold.json`; DATA `tags/item/iron_ores.json`,
  `gold_ores.json`; NF-SRC `Tags.java`, lines 658, 709, 614, 657, 708, 613). Copper has `#c:ores/copper`,
  `#c:raw_materials/copper`, and `#c:ingots/copper`, but no nugget tag and no vanilla nugget item
  (NF-DATA `data/c/tags/item/nuggets/` holds only `gold.json` and `iron.json`; NF-SRC `Tags.java`, lines
  654, 707, 591).
- Copper is not a 1.12 material. It sat in `OREDICT_COLOR_MAP` for other mods' ores (line 933) and the
  generator required a nugget, so no 1.12 install bloomed vanilla-less copper. Vanilla 1.21 copper makes
  decorative blocks, the lightning rod, the spyglass, and the brush; none of these is a gate (SKIPS gate
  table).
- The five recipes SKIPS left alone: `raw_iron`, `raw_gold`, `raw_copper` (raw block to 9 raw items,
  DATA `recipe/raw_iron.json`), `copper_ingot`, and `copper_ingot_from_waxed_copper_block` (copper block
  to 9 ingots, DATA `recipe/copper_ingot.json`). The 1.12 removal of `iron_ingot_from_block` and
  `gold_ingot_from_block` sent decompression to the granite anvil (SIGN-OFFS, issue #33 item 7).

Decisions 1 and 7 give the options and recommendations: bloomery inputs `#c:raw_materials/iron`
plus `#c:ores/iron` at the 1.12 yields, no copper bloomery recipe, and the five decompressions removed
with anvil entries added. Decision 10 covers nether gold ore, a post-1.12 gold source SKIPS did not list.

## Network payloads

| 1.12 packet | Fate |
|---|---|
| `SCPacketNoHunger` sent through `ModuleTechBasic.PACKET_SERVICE` (`tile/TileBloom.java`, line 198) | Core's payload through core's helper (CORE network section; issue #19 follow-on 4). |
| `SCPacketParticleLava(pos, level)` sent through `ModuleTechBloomery.PACKET_SERVICE` on molten slag harvest (`block/BlockPileSlag.java`, line 197) | `ServerLevel#sendParticles` (CORE dropped the packet). |
| Athenaeum's tile data service for the two synced tiles | `SyncedBlockEntity` for the bloomery; nothing for the bloom and the heap. |
| Tech/basic's anvil hit payload | Consumed, not owned: bloomery's `onAnvilHitClient` runs in its client handler (TECH-BASIC network table). |

Bloomery owns no payload. The scroll payload STORAGE proposed is not needed: no bloomery interaction
uses the mouse wheel (`TileBloomery.java`, the five interactions at lines 152 to 177 are click-only,
and `InteractionShovel` refuses anything but `MouseClick` at lines 1034 to 1036).

## Config

`ModuleTechBloomeryConfig` is 612 lines in five categories. The principle from CORE: behaviour
toggles in `COMMON`, gameplay multipliers in `SERVER` (SIGN-OFFS, issue #21 item 5), item-describing
numbers in data or constants. The three `STAGES_*` fields (lines 28 to 34) die with gamestages, and the
two `*_FUEL_MODIFIERS` tuple lists (lines 22 to 25) with CraftTweaker. Lines cited are the field
declarations.

| Category | Stays in config | Becomes data | Dies or bakes |
|---|---|---|---|
| `SLAG` (40 to 65) | | | `MOLTEN_WALK_DAMAGE` 3, `HARVESTING_PLAYER_FIRE_CHANCE` 0.125, `HARVESTING_PLAYER_FIRE_DURATION_SECONDS` 1 bake. |
| `BLOOMERY` (71 to 214) | `HAS_SPEED_CAP` false, `DROP_SLAG_WHEN_BROKEN` true (`COMMON`); `SPEED_SCALAR` 2, `AIRFLOW_MODIFIER` 1, `AIRFLOW_DRAG_MODIFIER` 0.005 (`SERVER`); a `BASE_RECIPE_DURATION_MODIFIER` 1 that 1.12 lacks, for parity with the other machines (`SERVER`, decision 5) | `SPECIAL_FUEL_BURN_TIME_MODIFIERS` {living tar 3, coal coke block 3, coal block 1.5} (166 to 170) and `getSpecialFuelBurnTimeModifier` (172 to 207) become an item data map `pyrotech:bloomery_fuels` with two optional doubles, `bloomery` and `wither_forge`, written by bloomery's `DataMapProvider` (NF-SRC `net/neoforged/neoforge/registries/datamaps/DataMapType.java`, line 85; `RegisterDataMapTypesEvent.java`, line 38; `net/neoforged/neoforge/common/data/DataMapProvider.java`, line 98). The tooltip handler and the book read it. | `CAPACITY` 1, `FUEL_CAPACITY_BURN_TIME` 256,000, `FUEL_CAPACITY_ITEMS` 16, `MAX_ASH_CAPACITY` 16, `ASH_CONVERSION_CHANCE` 0.35, `ENTITY_WALK_BURN_DAMAGE` 3 bake as constructor arguments. |
| `WITHER_FORGE` (220 to 389) | `ENABLE_SCARY_SOUNDS` true, `HAS_SPEED_CAP` false, `DROP_SLAG_WHEN_BROKEN` true (`COMMON`); `SPEED_SCALAR` 2, `AIRFLOW_MODIFIER` 1, `AIRFLOW_DRAG_MODIFIER` 0.005, its own duration modifier (`SERVER`) | The `wither_forge` field of the same data map: living tar 6, coal coke block 3, coal block 1.5 (335 to 339) | `INHERIT_BLOOMERY_RECIPES` dies (chain table); `CAPACITY` 3, `SCARY_SOUND_INTERVAL_TICKS` 800, `SCARY_SOUND_INTERVAL_VARIANCE_TICKS` 400, `FUEL_CAPACITY_BURN_TIME` 128,000, `FUEL_CAPACITY_ITEMS` 16, `MAX_ASH_CAPACITY` 16, `ASH_CONVERSION_CHANCE` 0.35, `ENTITY_WALK_BURN_DAMAGE` 3 bake. |
| `BLOOM` (395 to 553) | `BREAKS_BLOCKS` true (`COMMON`); `HAMMER_HITS_REQUIRED` 16, `HAMMER_HITS_IN_ANVIL_REQUIRED` 8, `HAMMER_POWER_MODIFIER_PER_HARVEST_LEVEL` {0.7, 1, 2, 3}, `HAMMER_POWER_MODIFIER_PER_EFFICIENCY_LEVEL` {1.25, 1.5, 1.75, 2, 2.25}, `HAMMER_POWER_MODIFIER_FOR_STRENGTH_EFFECT` 1.3, `WEAKNESS` 0.7, `MINING_FATIGUE` 0.1, `SILK_TOUCH_FAILURE_MODIFIER` 0, `CHANCE_TO_NOT_CONSUME_BLOOM_INTEGRITY_PER_FORTUNE_LEVEL` {0.15, 0.3, 0.45} (`SERVER`; JEI shows the hits, and the hammer power table was settled as config by issue #21 item 4) | | `MINIMUM_HUNGER_TO_USE` 3, `EXHAUSTION_COST_PER_HIT` 1, `FIRE_DAMAGE_PER_SECOND` 3, `ENTITY_WALK_DAMAGE` 3, `FIRE_SPAWN_CHANCE_ON_HIT_RAW` 0.1, `FIRE_SPAWN_CHANCE_ON_HIT_IN_ANVIL` 0.05, `FIRE_SPAWN_CHANCE_RANDOM` 0.25 bake. `ENTITY_WALK_DAMAGE` is one constant read by the bloom block's `stepOn` and returned by `AnvilHotItem.walkDamage()` (SIGN-OFFS, issue #19 follow-on 5). |
| `TONGS` (559 to 611) | | | The seven durabilities bake into `Item.Properties#durability` (TOOL decision 1 pattern). |

That is six `COMMON` toggles, seventeen `SERVER` values (four per device plus the nine bloom
numbers), and one data map. `ModConfigSpec.Builder#define`, `defineInRange`, `defineList` (NF-SRC
`net/neoforged/neoforge/common/ModConfigSpec.java`; TECH-BASIC config section cites the lines).
Decision 5 asks whether the bake list is right.

## Dropped outright

- `plugin/`: 12 files. CraftTweaker (`ZenBloomery`, 494 lines, plus its `ExportDocumentation.java` entry
  at line 25), TOP (`PluginTOP`, `BloomProvider`, `BloomeryProvider`), and Waila (`PluginWaila`, two
  delegates, two providers) are out of scope (issue #1). The Waila lang keys on `main` (`en_us.json`,
  lines 446 to 450) are dead. JEI returns in the shared plugin (next section).
- `ModuleBase`, `Registry`, `Injector`, `IPacketService`, `ITileDataService`, the `modules_enabled`
  condition on all nine JSON recipes, the `isModuleEnabled` guards in `ModuleTechBloomery` (lines 176
  and 185) and `StoneKilnRecipesAdd` (line 90), `ModPyrotechConfig.MODULES` (line 38), the
  `ModPyrotech.registerModule` call (lines 94 to 95), the gamestages hooks on the three tiles.
- `TileEntityDataBase`, `TileCapabilityDelegate`, `TileBloomery.Top`, `ITileContainer`, `IBlockInteractable`,
  `ITileInteractable`, every `IInteraction` subclass, `IInteractionItem` on the tongs, `collisionRayTrace`
  overrides, `shouldRenderInPass`, `removedByPlayer` and `harvestBlock` tile-keeping, `getActualState`,
  `getStateFromMeta`, `getMetaFromState`, `quantityDropped`, `canSilkHarvest`, `IVariant`.
- `ObservableStackHandler`, `DynamicStackHandler`, `LIFOStackHandler`, `TileDataFloat` and friends.
- `JsonSlagList`, `SlagInitializer`, `JsonInitializer`, the `GENERATED_SLAG` and `GENERATED_PILE_SLAG`
  maps, `SLAG_BY_OREDICT`, `SLAG_PILE_BY_OREDICT`, `CompatInitializerOre` on the core side, and the
  `RecipeItemParser` reads in the config's fuel modifier lookup (lines 190 to 202).
- `BloomeryRecipe.getRecipe` and `removeRecipes`, `RecipeHelper.inherit`, `INHERIT_TRANSFORMER`,
  `IngredientHelper.fromStackWithNBT`, `WeightedPicker` (vanilla's list replaces it), `ArrayHelper.getOrLast`
  (a two-line helper the port keeps for the past-the-end rule).
- `EntityItemBloom` and its `EntityEntryBuilder` registration (decision 4), `BlockBloom.tickRate`,
  `BlockFalling.fallInstantly`, the `TextureStitchEvent` handler, `ModuleTechBloomery.LOGGER`,
  `BulkRenderItemSupplier`'s bloomery entry, the seven full tongs items (decision 3), the seven
  `ItemTongsEmpty*` and seven `ItemTongsFull*` classes.
- `FuelBloomeryProcessor` and `FuelWitherForgeProcessor` (`modules/plugin/patchouli/processors/`) are the
  book ticket's; they read the data map instead of the config (findings).

## Assets and JEI

What `main` has for this unit (`src/main/resources/assets/pyrotech/`):

- Blockstates: `bloom.json` (one variant), `bloomery.json` (multipart over `facing` and `type`, with
  the lit state applying three models: the base with lit textures, `bloomery_lit`, and
  `bloomery_lit_lava_top`), `wither_forge.json` (variants over `facing` and `type`, the top half on the
  `_empty` wrapper), `pile_slag.json` (16 variants over `level` and `molten`, the molten ones on `_2`
  wrappers with the `slag_hot` top), `slag_glass.json` (64 connected-texture variants). Five files.
  `slag_glass` is core's block (`modules/core/block/BlockSlagGlass.java`, line 21;
  `modules/core/init/BlockInitializer.java`, line 67), so its connected-texture hand work and its
  missing item model (ASSETS "Item models to create during hoisting") are core's, not bloomery's.
- Block models: `bloom.json`, `bloomery.json`, `bloomery_lit.json`, `bloomery_lit_lava_top.json`, the four
  `gen/bloomery/` wrappers, `wither_forge_lit.json`, `wither_forge_unlit.json`, the three
  `gen/wither_forge/` wrappers, the eight `slag_pile_block_*.json` and sixteen `gen/pile_slag/` wrappers,
  and `models/nether_bloomery.json` at the models root. That last file is the wither forge's source
  model with the same textures as `wither_forge_lit.json` (`models/nether_bloomery.json`, lines 2 to
  16); no blockstate references it. Delete it.
- Item models: `slag.json` (`item/generated`, layer 0 `slag` and layer 1 `slag_overlay`), the seven
  `tongs_<material>.json` and seven `tongs_<material>_full.json` (`item/handheld`). Fifteen files.
- Textures: nine `bloomery_*` block textures, `slag.png`, `slag_hot.png`, `slag_overlay.png`, eleven
  `slag_glass_*` (core's), eight `wither_forge_*`, fourteen tongs item textures, `slag.png` and
  `slag_overlay.png` item textures, and the three book images. `active_pile.png` and `ash_block.png`
  are other units' textures the renderer borrows.
- Lang: `item.pyrotech.slag`, `slag_unique`, the seven `tongs_*`, `block.pyrotech.bloom`, `bloom_unique`,
  `bloomery`, `wither_forge`, `pile_slag`, `pile_slag_unique` (`en_us.json`, lines 138 to 152),
  `gui.pyrotech.tooltip.burn.time.efficiency` and `.modifier` (417 to 418), the two JEI category names
  (486 to 487), the two advancements (544 to 545, 556 to 557).

Gaps the code port owes:

- Item models for the four block items: `bloomery.json`, `wither_forge.json`, `bloom.json`, and
  `pile_slag.json` are absent under `models/item/`. 1.12 registered them from the blockstates
  (`init/BlockInitializer.java`, lines 56 to 61). The bloomery's should point at the unlit bottom model
  and the wither forge's at `wither_forge_unlit`; the bloom's at `block/bloom` (whose `display` block
  carries the 1.12 item transforms, `models/block/bloom.json`, lines 2 to 25); the heap's at
  `slag_pile_block_16`.
- Per-ore slag: `slag_iron.json`, `slag_gold.json` item models on the same two layers, and
  `pile_slag_iron.json`, `pile_slag_gold.json` blockstates that reuse the sixteen `gen/pile_slag/`
  wrappers, plus their item models and the four lang keys `item.pyrotech.slag_iron` and so on if the
  unique-name format is not used (decision 2).
- Tongs: with decision 3, seven `tongs_<material>.json` gain an `overrides` entry on `pyrotech:full`
  pointing at the existing `_full` models; the `_full` files stay as override targets.
- Render types (issue #27): the eight `slag_pile_block_*.json` need `cutout` (`BlockPileSlag.java`,
  lines 127 to 130). The bloomery, wither forge, and bloom used the 1.12 default solid layer, but
  `bloomery_lit.json`, `bloomery_lit_lava_top.json`, and `wither_forge_lit.json` map the vanilla fire and
  lava sprites (`models/block/bloomery_lit.json`, lines 4 to 6; `models/nether_bloomery.json`, lines 9
  and 14), and the fire sprites have transparent pixels. That is the texture-alpha case STORAGE
  reported for the bags; flag the three lit models for a `cutout` check.
- Blockstates: none regenerate if the `type` property is kept (decision 6).
- The `pyrotech:bloomery_drip` particle needs `particles/bloomery_drip.json` naming the vanilla lava
  sprite (decision 9).
- Lang: nothing new beyond the per-ore slag keys above.

JEI. The 1.12 plugin registered two categories, bloomery and wither forge, on one category class
over `jei7.png` (on `main`), with three slots: input, the bloom at max yield, and the slag times its
count (`plugin/jei/JEIRecipeCategoryBloomery.java`, lines 28 to 45 and 84 to 94;
`JEIRecipeWrapperBloomery.java`, lines 24 to 38). It registered a subtype interpreter for the bloom by
recipe id so blooms of different recipes are distinct ingredients (`PluginJEI.java`, lines 32 to 38), and
hid the full tongs and the bloom (lines 45 to 54). The shared plugin needs the two categories over the
two recipe types, a component-aware subtype for the bloom on the `pyrotech:bloom` component, the bloom
hidden, and one synthetic source: one anvil-category entry per bloomery and wither forge recipe showing
the bloom as input, the output plus the failure items, the hits from config, and the yield range, as
`JEIRecipeWrapperAnvil` drew it (SIGN-OFFS, issue #21 item 3). The JEI API calls are not verified here.

## Glossary terms

Terms `CONTEXT.md` does not have yet and this unit needs:

- **Airflow**: the number, from 0 up, that sets how fast a bloomery, wither forge, or stone combustion
  worker runs. An open front face gives 1, a solid one 0, a bellows adds a bonus that decays every
  tick, and stored fuel chokes it. A plain interface every consumer implements; nothing outside the mod
  can push it.
- **Hammer power**: the fraction of a hammer hit a bloom counts, from the hammer's tool level, its
  efficiency level, the player's strength, weakness, and mining fatigue, and the distance from the
  bloom. Below 1 a hit counts for less; above 1 for more.
- **Integrity**: the number of items left in a bloom. Each finished hammering cycle takes one, unless
  fortune saves it, and yields the recipe's output or a failure item. A bloom starts with one random
  yield per input item.
- **Slag heap**: the eight-level pile a bloomery or wither forge builds in front of itself, one slag per
  level, molten for five minutes after the last addition. A heap of a metal's slag blooms again; a heap
  of plain slag fires into slag glass.

## Findings for other tickets

- **Tech/machine** (issue #20): the bellows pushes airflow into `pos.relative(facing)` and the
  mechanical bellows into three positions (`modules/tech/machine/tile/TileBellows.java`, lines 150 to
  173). With airflow a plain core interface the push is `level.getBlockEntity(target) instanceof
  AirflowConsumer`, and the lookup must resolve a bloomery or wither forge top half to the block entity
  below, because 1.12's `TileBloomery.Top` forwarded the capability (`library/spi/tile/TileCapabilityDelegate.java`,
  lines 10 to 46). `TileCombustionWorkerStoneBase` implements the same interface (line 46). The
  `pyrotech:stone_kiln/slag_glass` recipe (`init/recipe/StoneKilnRecipesAdd.java`, lines 89 to 103) is
  machine's datagen under the placement rule, not bloomery's as the issue #20 follow-on says; its
  inputs are the plain slag heap and its failure items 4 glass shards and 4 plain slag. The mechanical
  compacting bin inherits the three slag compacting recipes through the chain table. The trip hammer
  needs nothing from the bloom beyond the anvil's public `hit` (TECH-BASIC).
- **Core** (issue #10, hoisting): `IAirflowConsumerCapability` lives in `src/api/java`, not in a module;
  its one method becomes core's `AirflowConsumer` interface. `slag_glass` is core's block, and core's
  hoist owes its connected-texture blockstate hand work and item model; bloomery only writes the
  smelting recipe that makes it. The `pyrotech:tool_levels` helper gets a fourth reader, the bloom's
  hammer power. Core's removal-stub provider gains five ids if decision 7 holds: `raw_iron`, `raw_gold`,
  `raw_copper`, `copper_ingot`, `copper_ingot_from_waxed_copper_block`.
- **Tech/basic** (issue #16, hoisting): the anvil block entity's public input handler accessor and the
  seven accessors stand (issue #19 follow-on 4); the tongs reach the anvil through `Item#useOn`, so the
  anvil's `useItemOn` must return `PASS` for a stack no anvil recipe accepts rather than consuming the
  click. If decision 7 holds, `TechBasicRecipes` gains five granite anvil pickaxe entries (raw iron,
  raw gold, and raw copper blocks to 9 raw items; copper and waxed copper blocks to 9 ingots) beside its
  iron and gold block entries. The anvil's `getLightEmission` reads the bloom item's block light 9.
- **Progression skips** (issue #33): nether gold ore drops 2 to 6 gold nuggets without a bloomery (DATA
  `loot_table/blocks/nether_gold_ore.json`) and nine nuggets craft an ingot, a post-1.12 gold source the
  note did not list (decision 10).
- **Recipe viewer** (the JEI plugin): two categories, one synthetic anvil source, a component subtype
  for the bloom, and the bloom hidden (assets and JEI section).
- **Render layers** (issue #27): eight `slag_pile_block_*.json` cutout; `bloomery_lit.json`,
  `bloomery_lit_lava_top.json`, and `wither_forge_lit.json` to check for texture alpha.
- **Patchouli book** (the last ticket): `FuelBloomeryProcessor` and `FuelWitherForgeProcessor`
  (`modules/plugin/patchouli/processors/`, lines 15 to 20 each) read the special fuel modifier for the
  `fuel_bloomery` and `fuel_wither_forge` page templates; they read the `pyrotech:bloomery_fuels` data
  map. The book's `bloom/slag.json` entry names the compacting bin recipes by their 1.12 ids
  (`pyrotech_pile_slag_from_pyrotech_slag` and the generated iron and gold ids) and the stone kiln
  `pyrotech:slag_glass`; those become the proposed ids above.
- **Ignition** (issue #18, hoisting): the bloomery takes igniter items on the top half from `UP` and the
  powered igniter on the bottom half from any side (block table); it never tests `FIRESTARTER_LIGHT`.
- **Storage** (issue #14, hoisting): no bloomery tile needs `LargeStackHandler`, `HotFluidTank`, the
  scroll payload, or the hit-to-slot helper; the bloomery's one interaction box is the top's inner
  square on `UP`.

## Decisions for Moos

1. **Bloomery inputs: ore blocks, raw ores, or both.** Facts: 1.12 took ore blocks through the ore
   dictionary (`BloomeryRecipesAdd.java`, line 107); 1.21 ores drop one raw item unless silk-touched
   (DATA `iron_ore.json`), and one raw item equals one ore block in vanilla (both smelt to one ingot);
   the yields are per input item (`BloomeryRecipeBase.java`, lines 122 to 131). Options: `#c:raw_materials/<metal>`
   only; `#c:ores/<metal>` only, which would force silk touch; or an ingredient over both tags at the
   1.12 yield of 12 to 15 per item. Recommendation: both tags, same yield. A raw item is the 1.21 shape
   of "one ore's worth", so the progression cost per ingot matches 1.12, and a silk-touched block blooms
   the same as the raw item it would have dropped. Fortune on raw items is a 1.21 multiplier the
   bloomery cannot see, which is consistent with 1.12 fortune having no effect on ore blocks.
2. **Per-ore slag: registered pairs or a component.** Facts: 1.12 generated a slag item and a heap
   block per compat entry, tinted and named by the entry (`SlagInitializer.java`, lines 160 to 171),
   and the slag bloom recipe takes the ore's heap block as its input (`BloomeryRecipesAdd.java`, line
   125); with vanilla alone there are iron, gold, and plain. Options: register `slag_iron`,
   `slag_gold`, `pile_slag_iron`, `pile_slag_gold` beside the plain pair with baked colours, and let a
   datapack for a new metal reuse the plain slag; or one slag item and one heap block with a
   `pyrotech:slag_ore` component and a per-ore colour data map, matched in recipes through
   `DataComponentIngredient` (NF-SRC `DataComponentIngredient.java`, line 99), tinted through
   `ItemColor` and `BlockColor` reading the component and the block entity. Recommendation: the
   registered pairs. Two extra pairs are cheap, the recipes stay plain item ingredients, and the
   compat system that justified generation is gone. The component route is the extension path if
   cross-mod metals are wanted later.
3. **Tongs: seven items or fourteen.** Facts: each full item differs from its empty twin only in
   enchantability, the right-click placement, the tooltip line, and its JEI hiding (`ItemTongsFullBase.java`,
   lines 53 to 56, 60 to 125, 128 to 162; `PluginJEI.java`, lines 45 to 52); the full item's name
   delegates to the empty one (lines 166 to 169); the bloom's data already rides the tongs' tag
   (`BloomHelper.java`, lines 210 to 234). Options: seven items with the `pyrotech:bloom` component
   marking "full" and an `ItemProperties` predicate switching the model; fourteen items as in 1.12; or
   one item with a material component, the BUCKET shape. Recommendation: seven. It halves the
   registrations and models, keeps the seven crafting recipes and lang keys as they are on `main`,
   and the component already exists. One item with a material component was rejected because the
   seven durabilities and gold's enchantability would then need a second data source, and TOOL bakes
   material differences into separate items.
4. **The bloom's item entity.** Facts: `EntityItemBloom` never despawns, is invulnerable and fire
   immune, and spawns flames every 5 client ticks (`BlockBloom.java`, lines 544 to 596); `createEntity`
   also sets a 40-tick pickup delay (line 467). Options: keep a registered `EntityType` subclass of
   `ItemEntity`; or no entity type, using `fireResistant()` on the item, `createEntity` returning a
   configured vanilla `ItemEntity`, and `onEntityItemUpdate` for the particles (NF-SRC `IItemExtension.java`,
   lines 249 and 261; `ItemEntity.java`, lines 132, 288, 484, 492). Recommendation: no entity type.
   Every 1.12 behaviour has a vanilla or NeoForge hook, and a registered entity type would need its
   own renderer registration and network spawn handling for nothing.
5. **Config surface: the table as listed.** Facts: the config section keeps six toggles and seventeen
   `SERVER` values and bakes the capacities, chances, and damages. 1.12 has no duration multiplier for
   the bloomery, but every other Pyrotech machine keeps one (SIGN-OFFS, issue #21 item 4). Options: as
   tabled, with a new `BASE_RECIPE_DURATION_MODIFIER` per device; as tabled without it; or keep the
   capacities and ash numbers as config too. Recommendation: as tabled with the multiplier. It is one
   knob per device, it matches the glossary's "duration multiplier" for every machine, and the JEI
   category reads it like the others. Capacities describe the block and bake.
6. **Bloomery state shape: `type` or `half` plus `lit`.** Facts: the converted `bloomery.json` and
   `wither_forge.json` key on `facing` and the three-value `type` (top, bottom, bottom_lit), and the
   PROTOTYPE rule regenerates cleaned blockstates only for states that existed for 1.12 rendering.
   `bottom_lit` was derived from the tile (`BlockBloomery.java`, lines 399 to 413) but is a real
   gameplay state (light 12, hot floor, no fuel insertion). Options: keep `type` and the converted
   files; or `DoubleBlockHalf` plus `lit`, with `half=upper,lit=true` states that never occur and two
   regenerated blockstates. Recommendation: keep `type`. Three states are exactly the three the block
   has, and the files on `main` already say so.
7. **Copper and the five decompression recipes.** Facts: copper has no nugget item and no 1.12 recipe;
   raw ore blocks and copper blocks are storage blocks whose 1.12 parallels (`iron_block`, `gold_block`)
   decompress on the granite anvil with 8 pickaxe hits (SIGN-OFFS, issue #33 item 7). Options: no
   bloomery copper recipe and leave the five recipes; no bloomery copper recipe, remove the five, and
   add five granite anvil pickaxe entries (three raw blocks to 9 raw items, two copper blocks to 9
   ingots); or a copper bloomery recipe yielding ingots directly at a lower yield. Recommendation: the
   second. Copper stays a furnace-tier material with nothing gated behind it, and decompression is
   anvil work in Pyrotech, so the raw and copper blocks follow the iron and gold blocks. The stubs are
   core's, the anvil entries tech/basic's.
8. **Three 1.12 slips: port the behaviour or the intent.** Facts: (a) the slag-heap airflow branch is
   dead (`TileBloomery.java`, line 393), so a partial heap in front of the device counts 0.5 and a full
   one 0, where the intent was 1, 0.66, 0.33, 0 by level and nothing above level 4; (b) inherited
   wither forge copies lose experience and anvil tiers (`WitherForgeRecipesAdd.java`, lines 28 to 43);
   (c) `getUniqueBloomFromOutput` strips `.slag` from ids that use `_slag` (`BloomeryRecipeBase.java`,
   line 139), which has no effect. Options: port what 1.12 does; or port the evident intent. Recommendation:
   port the behaviour for (a), because slag heaps form in front of every bloomery and players tuned
   their layouts to the 0.5 they got; port the intent for (b), so the chain copies carry 0.25
   experience and the ore recipes' tiers, because the copies were meant to be the same recipe; drop
   (c).
9. **The drip particle: custom type or vanilla lava.** Facts: 1.12's drip is a lava particle with zero
   initial motion, slow gravity, a smoke trail, and full brightness (`ParticleBloomeryDrip.java`,
   lines 19 to 71); vanilla's `LavaParticle` has gravity 0.75 and a random launch (NF-SRC `LavaParticle.java`,
   lines 13 to 20). Options: a `pyrotech:bloomery_drip` particle type with a copied class; or
   `ParticleTypes.LAVA`. Recommendation: the custom type. It is one class and one JSON, and the look,
   molten drips running down the front, is the bloomery's signature.
10. **Nether gold ore.** Facts: it drops 2 to 6 gold nuggets with fortune (DATA `nether_gold_ore.json`),
    nine nuggets craft an ingot, and neither recipe is on the removal list; it is in `#minecraft:gold_ores`
    and so in `#c:ores/gold`. Options: leave it as a Nether-era gold source; override its loot table to
    drop the block, so it blooms through the gold recipe; or add it to the removal work of issue #33 as
    a loot modifier. Recommendation: leave it. The Nether sits behind obsidian, flint and steel, and a
    bloomery's worth of iron already, and the soul torch precedent (issue #34 item 2) accepted
    Nether-era rewards.
