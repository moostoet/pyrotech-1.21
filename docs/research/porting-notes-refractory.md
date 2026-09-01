# Porting notes: tech/refractory

Resolves issue #17 (porting notes: tech/refractory).
Refractory is a hoisting unit from issue #4 (module porting order): 40 files, 4,526 lines (DEPS).
It holds the active pile (the burning block a pit burn or refractory burn turns fuel into), the pit
ash block it collapses into, the pit burn recipe, the tar collector and the tar drain in stone and
brick, the wood tar and coal tar fluids, the refractory ignition helper, four event handlers, a
329-line config, and 11 plugin files. This document lists each 1.12 construct and its 1.21
replacement. Decisions that need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo, package `com.codetaylor.mc.pyrotech.modules.tech.refractory`,
  plus its users outside the package (`modules/ignition/item/ItemIgniterBase.java`,
  `modules/ignition/tile/TileIgniter.java`, `modules/tech/basic/tile/TileKilnPit.java`,
  `modules/tech/basic/init/recipe/SoakingPotRecipesAdd.java`,
  `modules/tech/machine/init/recipe/StoneCrucibleRecipesAdd.java`), `library/util/FloodFill.java`,
  `library/util/BlockMetaMatcher.java`, `library/spi/tile/TileBurnableBase.java`,
  `library/FluidInitializerRegistry.java`, and the recipes under `assets/pyrotech/recipes/tech/refractory/`.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths cited below are entries in that jar.
- **NF-DOCS**: docs.neoforged.net, 1.21.1 version pages. URLs cited inline. There is no fluid page
  for 1.21.1 (`/docs/1.21.1/misc/fluids` and the section index both return 404), so every fluid
  claim below cites NF-SRC only.
- **DATA**: the vanilla 1.21.1 data pack and client assets, at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`.
- **CORE**: `docs/research/porting-notes-core.md` on branch `research/porting-notes-core`.
- **STORAGE**: `docs/research/porting-notes-storage.md` on branch `research/porting-notes-storage` (commit 57d143da).
- **RECIPES**: `docs/research/recipe-architecture.md` on branch `research/recipe-architecture`.
- **BUCKET**: `docs/research/porting-notes-bucket.md` on branch `research/porting-notes-bucket`.
- **PROTOTYPE**: branch `prototype/8-campfire-interaction-sync`,
  `docs/prototypes/campfire-interaction-sync.md` and the Java under
  `src/main/java/com/moostoet/pyrotech/prototype/campfire/`.
- **ASSETS**: `docs/asset-migration-report.md` and `docs/asset-conversion-report.md` on `main`.
- **DEPS**: `docs/research/module-dependencies.md` on branch `research/module-dependencies`.

This document assumes the recommended answers from CORE, STORAGE, and RECIPES. It applies
PROTOTYPE pattern 1 (per-block if-chain dispatch) and pattern 2 (blockstate properties, else a
`SyncedBlockEntity` snapshot).

## Scope correction

The ticket text says the package holds "the refractory kiln, tar collector, and tar tanks" and that
"the tar tank builds on storage's tank base". The code says otherwise.

- The 40 files are: `ModuleTechRefractory`, `ModuleTechRefractoryConfig`, four blocks
  (`BlockActivePile`, `BlockPitAsh`, `BlockTarCollector`, `BlockTarDrain`), two fluid blocks
  (`BlockFluidCoalTar`, `BlockFluidWoodTar`), one TESR, four event handlers, three initializers
  plus `BurnPitRecipesAdd`, `PitBurnRecipe` and its builder, nine tiles (`TileActivePile`,
  `TilePitAsh`, stone and brick collectors and drains, and the three `spi` bases
  `TileTarTankBase`, `TileTarCollectorBase`, `TileTarDrainBase`), `RefractoryIgnitionHelper`, and 11
  plugin files (5 JEI, 2 TOP, 3 Waila, 1 CraftTweaker).
- There is no refractory kiln in this package. The "Refractory Kiln" is tech/machine's brick kiln:
  the lang key `gui.pyrotech.jei.category.kiln.brick` is "Refractory Kiln"
  (`main`, `assets/pyrotech/lang/en_us.json` line 462), and its recipes live in
  `modules/tech/machine/init/recipe/` (`StoneKilnRecipesAdd.java` registers the refractory brick
  smelt at lines 54 to 65). The kiln belongs to the tech/machine ticket.
- `TileTarTankBase` does not extend storage's `TileTankBase`. It extends athenaeum's
  `TileEntityDataBase` (`tile/spi/TileTarTankBase.java`, lines 34 to 36). Its only storage reference
  is `ModuleStorage.PACKET_SERVICE.sendToAllAround(new SCPacketParticleCombust(...))` at line 196,
  inside a hand-copied hot-fluid check (lines 179 to 204). STORAGE's "Reusable bases" section
  already found this; this document confirms it.
- The pit burn inputs are core blocks and one vanilla block, not tech/basic blocks:
  `ModuleCore.Blocks.LOG_PILE`, `PILE_WOOD_CHIPS`, `WOOD_TAR_BLOCK` (`ModuleCore.java`, lines
  261, 339, 351) and `Blocks.COAL_BLOCK` (`init/recipe/BurnPitRecipesAdd.java`, lines 25, 46, 64, 85).
  The cross-module edges run the other way: tech/basic and tech/machine read refractory
  (see "Findings for other tickets").

## Summary

- The package is self-contained apart from core, one packet borrowed through storage, and the
  ignition helper that ignition calls. Its four blocks flatten from two metadata variants into
  six registry entries (`tar_collector_stone`, `tar_collector_brick`, `tar_drain_stone`,
  `tar_drain_brick`, `active_pile`, `pit_ash_block`); the lang file on `main` already has the four
  collector and drain names.
- The two fluids leave athenaeum's `FluidInitializerRegistry` for the NeoForge stack: `FluidType`,
  `BaseFlowingFluid.Source` and `.Flowing`, `LiquidBlock`, `BucketItem`, and
  `IClientFluidTypeExtensions`, the same pattern CORE chose for the wines. The 1.12 universal
  bucket becomes one `BucketItem` per fluid.
- The pit burn recipe cannot be a vanilla `RecipeInput` match on an item. It becomes a
  `Recipe<PitBurnRecipeInput>` whose input record wraps a `BlockState`, matched by a
  `BlockPredicate` (block, block tag, or block plus state properties). The flood fill and the
  active-pile mechanics port as they are.
- `RefractoryIgnitionHelper` cannot move to core as CORE wrote: it depends on the recipe type, the
  active pile, and the config. What moves to core is a hook that ignition calls. Refractory
  registers the pit-burn igniter into it. Decision 1 revises CORE's wording.
- The tar collector and drain reuse STORAGE's `HotFluidTank`, its fluid box renderer, and the core
  combust particle helper. The collector's `burning` flag and the fluid ride the `SyncedBlockEntity`
  snapshot, the same choice STORAGE made for the faucet.
- The furnace fuel handler and the collector's per-fluid burn ticks become data. The refractory
  block list becomes the block tag `#pyrotech:refractory`, which core must own because tech/basic's
  pit kiln reads it too.
- The active pile and pit ash need no client sync at all. Both become plain block entities with a
  server ticker.
- Refractory sends no payloads of its own. Its plugin package drops; JEI returns as two categories
  in the shared plugin.

## Module bootstrap

`ModuleTechRefractory` follows the CORE replacements: one `register(IEventBus)` hook, `DeferredRegister`
fields as holders, no `ModuleBase`, no `Registry`, no `@GameRegistry.ObjectHolder`, no `Injector`.

| 1.12 construct | 1.21 replacement |
|---|---|
| `static { FluidRegistry.enableUniversalBucket(); }` (`ModuleTechRefractory.java`, line 52) | Gone. NeoForge has no universal bucket; each fluid gets its own `BucketItem` (fluids section). |
| `PACKET_SERVICE`, `TILE_DATA_SERVICE` (lines 55 to 66) | Dropped with athenaeum. Sync is the PROTOTYPE `SyncedBlockEntity`. |
| Three game-bus handlers registered in the constructor, the tooltip handler in client pre-init (lines 68 to 71, 120) | `@EventBusSubscriber(modid = ...)` static listeners (NF-DOCS https://docs.neoforged.net/docs/1.21.1/concepts/events/). See the events section for which survive. |
| CraftTweaker plugin loop, JEI plugin registration, TOP and Waila IMC (lines 73 to 87, 101 to 118) | Dropped. JEI is the shared plugin's concern (assets and JEI section). |
| `RegistryEvent.NewRegistry` creating the `pyrotech:pit_recipe` Forge registry (`init/RegistryInitializer.java`, lines 39 to 43) | A `RecipeType` and `RecipeSerializer` in RECIPES' `PyrotechRecipeTypes` and `PyrotechRecipeSerializers` holders (NF-SRC `net/minecraft/core/registries/Registries.java`, lines 181 and 182; NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/server/recipes/). |
| `onRegisterRecipesEvent` calling `BurnPitRecipesAdd.apply` (lines 124 to 128) | Datagen (recipes section). |
| `FluidInitializer.onRegister` / `onClientRegister` (lines 132 to 144) | Fluids section. |
| `BlockInitializer.onRegister`: `registerBlock` for the active pile with no item, pit ash with an `ItemBlock`, `registerBlockWithItem` for collector and drain, six tile classes (`init/BlockInitializer.java`, lines 23 to 40) | `DeferredRegister.Blocks#registerBlock` (NF-SRC `net/neoforged/neoforge/registries/DeferredRegister.java`, line 431) for six blocks, `Items#registerSimpleBlockItem` (line 546) for the four collector and drain items. Three `BlockEntityType`s: `BlockEntityType.Builder.of(factory, blocks...).build(null)` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntityType.java`, lines 337 and 341) for the active pile, the pit ash, and one collector type over both collector blocks plus one drain type over both drain blocks. The stone and brick tiles only override config getters (`tile/TileBrickTarCollector.java`, lines 10 to 31, and the three siblings), so the block entity asks its block for the numbers, as STORAGE does. |
| `onPostInitializationEvent` building `REFRACTORY_BLOCK_LIST` (lines 147 to 151; `RegistryInitializer.java`, lines 56 to 128) | The block tag `#pyrotech:refractory` (active pile section). |
| `onClientRegister`: variant item models, `ClientRegistry.bindTileEntitySpecialRenderer(TileTarCollectorBase.class, new TESRTarCollector())` (`BlockInitializer.java`, lines 46 to 66) | Item models load by id. `EntityRenderersEvent.RegisterRenderers#registerBlockEntityRenderer` (NF-SRC `net/neoforged/neoforge/client/event/EntityRenderersEvent.java`, line 109; NF-DOCS https://docs.neoforged.net/docs/1.21.1/blockentities/ber). |
| `ModuleTechRefractory.CREATIVE_TAB` | The four collector and drain items join core's tab in `BuildCreativeModeTabContentsEvent` (NF-SRC `net/neoforged/neoforge/event/BuildCreativeModeTabContentsEvent.java`, line 52). The active pile has no item. The pit ash item is decision 6. |
| `Fluids`, `Blocks`, `Registries` holder classes with injected nulls (lines 154 to 202) | The `DeferredBlock`, `DeferredItem`, and `DeferredHolder` fields (CORE). |

## Fluids: wood tar and coal tar

1.12 creates both fluids through `FluidInitializerRegistry.createFluid(name, true, fluid ->
fluid.setDensity(3000).setViscosity(6000), BlockFluid*::new)` (`init/FluidInitializer.java`, lines
22 to 42). The library builds `new Fluid(name, still, flowing)` with textures
`pyrotech:blocks/fluid_<name>_still` and `_flow` (`library/FluidInitializerRegistry.java`, lines 59
to 63), registers the block as `pyrotech:fluid.<name>` in the creative tab (line 120), adds a
universal bucket for each fluid (line 152), and maps every fluid block to the single
`blockstates/fluid.json` marker file (lines 178 to 193). The two block classes extend
`BlockFluidClassic` with `Material.WATER` and call `Blocks.FIRE.setFireInfo(this, 100, 5)`
(`block/fluid/BlockFluidWoodTar.java` and `BlockFluidCoalTar.java`, lines 8 to 15). Temperature and
luminosity are the Forge defaults (300 and 0); nothing sets them.

So the 1.12 tar is a placeable, flowing, water-like liquid that burns easily (fire encouragement
100, flammability 5), is heavy and thick (density 3000, viscosity 6000), and is not hot.

| 1.12 construct | 1.21 replacement |
|---|---|
| `Fluid` with density and viscosity (`FluidInitializer.java`, lines 28 and 39) | One `FluidType` each under `NeoForgeRegistries.Keys.FLUID_TYPES` (NF-SRC `net/neoforged/neoforge/registries/NeoForgeRegistries.java`, lines 38 and 53), built with `FluidType.Properties.create().density(3000).viscosity(6000)` (NF-SRC `net/neoforged/neoforge/fluids/FluidType.java`, lines 889, 1062, 1085). Temperature stays at the default; `temperature(int)` is at line 1073 and the default is read by `getTemperature()` at line 189. Light stays 0 (`lightLevel(int)`, line 1049). Add `sound(SoundActions.BUCKET_FILL, ...)` and `BUCKET_EMPTY` (line 1024; NF-SRC `net/neoforged/neoforge/common/SoundActions.java`, lines 19 and 24) so buckets make a noise. `NeoForgeMod`'s milk type is the pattern (NF-SRC `net/neoforged/neoforge/common/NeoForgeMod.java`, lines 660 to 661). |
| `BlockFluidClassic` source and flow in one block (`BlockFluidWoodTar.java`) | `BaseFlowingFluid.Source` and `.Flowing` (NF-SRC `net/neoforged/neoforge/fluids/BaseFlowingFluid.java`, lines 162 and 142) sharing one `BaseFlowingFluid.Properties(type, still, flowing)` (line 186) with `.bucket(...)` (line 192) and `.block(...)` (line 197), registered under `Registries.FLUID` (NF-SRC `Registries.java`, line 152). Milk again is the pattern (NF-SRC `NeoForgeMod.java`, lines 668 to 671). Flow speed and spread stay at the defaults (`slopeFindDistance`, `levelDecreasePerBlock`, `tickRate`, lines 202 to 217) since 1.12 used `BlockFluidClassic` defaults. |
| The fluid block, `Material.WATER`, registry id `pyrotech:fluid.wood_tar` (`FluidInitializerRegistry.java`, line 120) | `new LiquidBlock(fluid, properties)` (NF-SRC `net/minecraft/world/level/block/LiquidBlock.java`, line 66) with the vanilla water chain: `mapColor(...).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY)` (NF-SRC `net/minecraft/world/level/block/Blocks.java`, lines 339 to 352). Id `pyrotech:wood_tar`, matching the fluid id the way `minecraft:water` does. `MapColor.COLOR_BLACK` (NF-SRC `net/minecraft/world/level/material/MapColor.java`, line 36). |
| `Blocks.FIRE.setFireInfo(this, 100, 5)` (`BlockFluidWoodTar.java`, line 14) | Override `getFireSpreadSpeed` to return 100 and `getFlammability` to return 5 on the `LiquidBlock` subclass (NF-SRC `net/neoforged/neoforge/common/extensions/IBlockExtension.java`, lines 685 and 646). The defaults read `FireBlock.getIgniteOdds` and `getBurnOdds` (NF-SRC `net/minecraft/world/level/block/FireBlock.java`, lines 256 and 249), which `FireBlock.setFlammable` (line 331) only fills for vanilla blocks. The argument order matches 1.12 (`setFlammable(block, encouragement, flammability)`). |
| `FluidRegistry.addBucketForFluid` (universal bucket, `FluidInitializerRegistry.java`, line 152) | One `BucketItem(fluid, properties)` each (NF-SRC `net/minecraft/world/item/BucketItem.java`, line 34) with `Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)` (NF-SRC `net/minecraft/world/item/Item.java`, lines 463 and 452). Ids `pyrotech:wood_tar_bucket`, `pyrotech:coal_tar_bucket`. |
| `blockstates/fluid.json` with `forge:fluid` (1.12 assets; ASSETS lists it as not convertible) | Textures come from `IClientFluidTypeExtensions` registered in `RegisterClientExtensionsEvent#registerFluidType` (NF-SRC `net/neoforged/neoforge/client/extensions/common/RegisterClientExtensionsEvent.java`, line 100), returning `getStillTexture` and `getFlowingTexture` (NF-SRC `net/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions.java`, lines 76 and 92), as `ClientNeoForgeMod` does for water at lines 98 to 110. The liquid block still needs a blockstate file and a particle-only model like vanilla's (DATA `assets/minecraft/blockstates/water.json`, `assets/minecraft/models/block/water.json`). |
| Fluid names `fluid.wood_tar`, `tile.fluid.wood_tar.name` (1.12 `lang/en_us.lang`, lines 249 to 252; copied to `main`) | `FluidType#getDescriptionId` defaults to `fluid_type.<namespace>.<path>` (NF-SRC `FluidType.java`, lines 146 to 148), or `Properties.descriptionId(...)` (line 899). `FluidStack#getHoverName` reads it (NF-SRC `net/neoforged/neoforge/fluids/FluidStack.java`, line 423). The old keys on `main` are dead; add `fluid_type.pyrotech.wood_tar`, `block.pyrotech.wood_tar`, `item.pyrotech.wood_tar_bucket` and the coal tar three. |
| No fluid tags in 1.12 | A `FluidTagsProvider` (NF-SRC `net/minecraft/data/tags/FluidTagsProvider.java`, line 22) writing `data/pyrotech/tags/fluid/wood_tar.json` and `coal_tar.json` listing source and flowing, the shape vanilla uses (DATA `data/minecraft/tags/fluid/water.json`). Recipes that accept tar can then use `SizedFluidIngredient.of(tag, amount)` (NF-SRC `net/neoforged/neoforge/fluids/crafting/SizedFluidIngredient.java`, line 107). Nothing requires this; it is the idiom. |

The hot-fluid rule. STORAGE and BUCKET decided that "hot" means `FluidType#getTemperature(FluidStack)`
(NF-SRC `FluidType.java`, line 677) at or above 450. Tar keeps the default temperature of 300, so tar
never triggers the rule. That matches 1.12, where the tar collectors hold tar without breaking
(`ModuleTechRefractoryConfig.java`, line 163: stone collector `HOLDS_HOT_FLUIDS = false` and it still
works). Lava at 1300 breaks the stone collector and drain and is held by the brick ones, exactly as
the config comments say (lines 150 to 156).

Placeability. `BaseFlowingFluid` with a `.block(...)` is placeable and flows like water, as
`BlockFluidClassic` did. `FluidType#isVaporizedOnPlacement` (line 815) stays default (tar is not
water-like in the Nether sense). The drain can pull placed tar back up (drain section).

## The active pile and the pit burn

The mechanic. A player stacks fuel blocks (log piles, coal blocks, tar bales, or a pile of wood
chips) in a pit, lights one, and covers the hole. Ignition flood-fills every connected block that
matches the recipe input and replaces each with an active pile carrying the recipe id
(`util/RefractoryIgnitionHelper.java`, lines 18 to 46). Each active pile then burns through the
recipe's stages, producing an item per stage (or a failure item), pushing fluid down into piles or
collectors below, and finally turns into a pit ash block holding the outputs.

| 1.12 construct | 1.21 replacement |
|---|---|
| `BlockActivePile`: `Material.ROCK`, hardness 2, `SoundType.STONE`, pickaxe 0, `lightValue = 7` (`block/BlockActivePile.java`, lines 27 to 34) | `BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0f).sound(SoundType.STONE).lightLevel(state -> 7).noLootTable()` (NF-SRC `net/minecraft/world/level/block/state/BlockBehaviour.java`, lines 1096, 1149, 1203, 1185, 1190, 1218; `net/minecraft/world/level/block/SoundType.java`, line 28), block tag `minecraft:mineable/pickaxe` (NF-SRC `net/minecraft/tags/BlockTags.java`, line 141; DATA `data/minecraft/tags/block/mineable/pickaxe.json`). Level 0 means no `requiresCorrectToolForDrops`. |
| `neighborChanged` calling `setNeedStructureValidation` (lines 39 to 48) | `neighborChanged` (NF-SRC `BlockBehaviour.java`, line 186) doing the same on the block entity. |
| `randomDisplayTick`: four `SMOKE_NORMAL` particles at y+2 and y+1 (lines 51 to 94) | `animateTick` (NF-SRC `net/minecraft/world/level/block/Block.java`, line 277) with `Level#addParticle(ParticleTypes.SMOKE, ...)` (NF-SRC `net/minecraft/world/level/Level.java`, line 530; `net/minecraft/core/particles/ParticleTypes.java`, line 84). |
| `breakBlock` spawning the nine output slots, `quantityDropped` 0, `canSilkHarvest` false (lines 98 to 127) | `onRemove` guarded by `!state.is(newState.getBlock())` (NF-SRC `BlockBehaviour.java`, line 193) with `Containers.dropItemStack` per slot (NF-SRC `net/minecraft/world/Containers.java`, line 31). `noLootTable()` covers the zero drop (NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/). |
| `isFireSource` true (lines 131 to 134) | `IBlockExtension#isFireSource` (NF-SRC `IBlockExtension.java`, line 700). Fire on top of a pile never burns out, so the pit keeps burning visibly. |
| `hasTileEntity` / `createTileEntity` (lines 137 to 148) | `EntityBlock#newBlockEntity` and `getTicker` (NF-SRC `net/minecraft/world/level/block/EntityBlock.java`, lines 15 and 18), server side only. |

The block entity. `TileActivePile` extends `library/spi/tile/TileBurnableBase` (line 36), which
tech/basic's `TileKilnPit` also extends (`TileKilnPit.java`, line 59). The base owns the tick loop:
revalidate the enclosure every 20 ticks or when a neighbour changes, count invalid ticks up to 100,
then call `onInvalidDelayExpired`; count down `burnTimeTicksPerStage`, call `onBurnStageComplete`
per stage, and `onAllBurnStagesComplete` when `remainingStages` hits zero
(`TileBurnableBase.java`, lines 18, 19, 37 to 92). The default structure test is "side is solid and
not flammable" (lines 113 to 117). The base belongs in the shared `library` package of the port,
hoisted with whichever of tech/basic and refractory comes first. It is 197 lines with no athenaeum
dependency beyond the tile data service constructor argument, which drops.

| 1.12 construct | 1.21 replacement |
|---|---|
| `TileEntityDataBase` parent, `ITickable.update` (`TileBurnableBase.java`, lines 15 to 16) | A plain `BlockEntity` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntity.java`, line 30) with a `BlockEntityTicker` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntityTicker.java`, line 9). The active pile registers no tile data for the network (`tile/TileActivePile.java`, constructor at lines 46 to 51 calls nothing) and nothing on the client reads it, so it does not need `SyncedBlockEntity`. |
| `world.isRemote` guard (line 39) | `level.isClientSide` (NF-SRC `Level.java`, line 113), or wire the ticker server side only in `getTicker`. |
| `isValidStructureBlock` default: `isSideSolid` and not `isFlammable` (lines 113 to 117) | `BlockState#isFaceSturdy(level, pos, direction)` (NF-SRC `BlockBehaviour.java`, line 944) and `!state.isFlammable(level, pos, direction)` (NF-SRC `net/neoforged/neoforge/common/extensions/IBlockStateExtension.java`, line 498). |
| `FluidTank(getMaxFluidLevel())`, `ItemStackHandler(9)`, `recipeKey` (`TileActivePile.java`, lines 42 to 51) | `FluidTank` (NF-SRC `net/neoforged/neoforge/fluids/capability/templates/FluidTank.java`, line 25) of 500 mB, `ItemStackHandler(9)` (NF-SRC `net/neoforged/neoforge/items/ItemStackHandler.java`, line 24), and a `ResourceLocation` recipe id resolved with `RecipeManager#byKey` (NF-SRC `net/minecraft/world/item/crafting/RecipeManager.java`, line 137). No large handler is needed: output stacks never exceed the item's own max stack because `insertItem` walks the nine slots (lines 257 to 262). |
| `getTotalBurnTimeTicks`: recipe time times `REFRACTORY_RECIPE_DURATION_MODIFIER` when the recipe requires refractory, else 1000 default (lines 75 to 90); `PitBurnRecipe.getTimeTicks` times `BASE_RECIPE_DURATION_MODIFIER` (`recipe/PitBurnRecipe.java`, lines 89 to 92) | Same arithmetic in the block entity reading the config (config section). RECIPES decision 4 keeps duration multipliers as runtime config. |
| `onUpdateInvalid`: place fire in every air or replaceable neighbour (lines 127 to 139); `onInvalidDelayExpired`: replace self with fire (lines 320 to 323) | `BlockState#isAir()` and `canBeReplaced()` (NF-SRC `BlockBehaviour.java`, lines 618 and 869), `level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3)` (NF-SRC `Level.java`, line 228). Use `BaseFireBlock.getState(level, pos)` (NF-SRC `net/minecraft/world/level/block/BaseFireBlock.java`, line 41) so soul fire is picked over soul soil. |
| `onBurnStageComplete`, fluid: fill the tank with `getFluidProduced()`, then push down up to `MAX_FLUID_PUSH_DEPTH` 3 blocks into `TileActivePile.fluidTank.fillInternal` or `TileTarCollectorBase.getFluidTank().fillInternal` (lines 150 to 183) | `FluidTank#fill(stack, FluidAction.EXECUTE)` (NF-SRC `FluidTank.java`, line 96; `net/neoforged/neoforge/fluids/capability/IFluidHandler.java`, lines 18 and 81) on the tank reached through `level.getBlockEntity(pos.below(i))` (NF-SRC `Level.java`, line 805; `net/minecraft/core/BlockPos.java`, line 179), `instanceof` against the two block entity classes. `fillInternal` bypassed the drain's `canFill` flag and the collector's sided capability; a direct method on the block entity does the same. |
| `onBurnStageComplete`, items: failure chance plus `(1 - chance) * fill fraction` when `doesFluidLevelAffectFailureChance` (lines 187 to 193); when the recipe does not require refractory, flood-fill the connected active piles and pit ash and check every face against `REFRACTORY_BLOCK_LIST` or a closed refractory door, multiply by `REFRACTORY_FAILURE_MODIFIER` if all valid (lines 195 to 238); clamp to `MIN`/`MAX_FAILURE_CHANCE` (lines 240 to 244); roll with `Util.RANDOM` (lines 246 to 254) | Same code. The flood fill is `library/util/FloodFill.java` ported verbatim (lines 24 to 71: breadth-first, visited set, action returning false stops, `limit` accepted blocks). The refractory test is `state.is(PyrotechTags.REFRACTORY)` (NF-SRC `BlockBehaviour.java`, line 886) plus the door and double slab cases below. Random is `level.random` (NF-SRC `Level.java`, line 107). |
| `onAllBurnStagesComplete`: collect outputs, `setBlockState(PIT_ASH_BLOCK)`, then insert into the new `TilePitAsh` on the next server tick through `minecraftServer.futureTaskQueue` because of a Sponge issue (lines 265 to 317) | `level.setBlock(...)` then `level.getBlockEntity(pos)` directly. The workaround targets Sponge's block capture on 1.12 (comment at lines 281 to 291) and NeoForge has no such capture. If the port wants to keep the deferral, `MinecraftServer#execute` runs immediately on the server thread (NF-SRC `net/minecraft/util/thread/BlockableEventLoop.java`, lines 94 to 100), so the equivalent is `server.tell(new TickTask(server.getTickCount() + 1, runnable))` (NF-SRC `BlockableEventLoop.java`, line 88; `net/minecraft/server/TickTask.java`, line 7; `net/minecraft/server/MinecraftServer.java`, lines 852 and 1391) or `level.scheduleTick(pos, block, 1)` (NF-SRC `net/minecraft/world/level/LevelAccessor.java`, line 52) with the insert in `Block#tick` (NF-SRC `BlockBehaviour.java`, line 387). Recommendation: insert directly. |
| `isValidStructureBlock` on the pile: any refractory-list block is valid; a recipe requiring refractory also accepts a closed refractory door; otherwise refractory or stone doors and the solid-non-flammable default (lines 326 to 354). `isValidDoor`: horizontal faces only, door closed and facing the pile, or open with hinge and facing rotated (lines 356 to 381) | Same logic. `DoorBlock.OPEN`, `FACING`, `HINGE` (NF-SRC `net/minecraft/world/level/block/DoorBlock.java`, lines 45 to 47), `Direction#getCounterClockWise` and `getClockWise` for `rotateYCCW` and `rotateY` (NF-SRC `net/minecraft/core/Direction.java`, lines 239 and 189). CORE makes both doors `DoorBlock` subclasses. |
| `writeToNBT`/`readFromNBT`: `fluidTank`, `recipeKey`, `output` (lines 385 to 403) | `saveAdditional`/`loadAdditional` (NF-SRC `BlockEntity.java`, lines 94 and 77) with `FluidTank#writeToNBT(registries, tag)` (NF-SRC `FluidTank.java`, line 67) and `ItemStackHandler#serializeNBT(registries)` (NF-SRC `ItemStackHandler.java`, line 139). |

The refractory block list. 1.12 builds `REFRACTORY_BLOCK_LIST` from: the active pile and pit ash
block, the brick igniter in all four facings (only if the ignition module is on), the refractory
brick block, refractory glass with wildcard meta, the brick tar collector, the brick tar drain in
all four facings, the double refractory brick slab, and the `REFRACTORY_BRICKS` config strings
(`RegistryInitializer.java`, lines 64 to 108). In 1.21 that is the block tag `#pyrotech:refractory`,
created with `TagKey.create(Registries.BLOCK, ...)` (NF-SRC `net/minecraft/tags/TagKey.java`, line 35;
NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/server/tags/) and written by a
`BlockTagsProvider` (NF-SRC `net/neoforged/neoforge/common/data/BlockTagsProvider.java`, line 18).
Facing no longer matters because tags match blocks, and the brick and stone collectors and drains
are separate blocks. Two cases the tag cannot express stay in code: the double slab is a state of
CORE's one `SlabBlock` (`SlabBlock.TYPE == SlabType.DOUBLE`, NF-SRC
`net/minecraft/world/level/block/SlabBlock.java`, line 30; `state/properties/SlabType.java`, line 8),
and the doors are already special-cased in `isValidDoor`. Tech/basic's pit kiln reads the same
list (`TileKilnPit.java`, lines 441 to 450), so the `TagKey` constant lives in core's tag holder and
each module's datagen adds its own blocks to it. The config string list drops; a datapack extends
the tag.

## The pit burn recipe

`PitBurnRecipe` holds: a `BlockMetaMatcher` input, an `ItemStack` output, `burnStages`,
`totalBurnTimeTicks`, an optional `FluidStack` produced per stage, `failureChance`, an
`ItemStack[]` of failure items, `requiresRefractoryBlocks`, and `fluidLevelAffectsFailureChance`
(`recipe/PitBurnRecipe.java`, lines 40 to 48). Lookup iterates the registry and calls
`matches(IBlockState)` (lines 23 to 33, 124 to 127). `BlockMetaMatcher` tests block identity and
either a wildcard or `getStateFromMeta(meta) == state` (`library/util/BlockMetaMatcher.java`, lines
22 to 32). Builder defaults: 1 stage, 10 minutes, no fluid, 0 failure, no refractory, no fluid
effect (`recipe/PitBurnRecipeBuilder.java`, lines 31 to 37).

RECIPES did not cover the pit burn. Its table stops at the bloomery and lists "burn pit" among the
later machines that "reuse these shapes or add their own". The pit burn needs its own shape because
its input is a block state, not an item.

The 1.21 shape. `RecipeInput` is `getItem(int)` and `size()` (NF-SRC
`net/minecraft/world/item/crafting/RecipeInput.java`, lines 5 to 8). A custom record
`PitBurnRecipeInput(BlockState state)` implements it. One trap: `RecipeManager#getRecipeFor`
returns empty when `input.isEmpty()` (NF-SRC `RecipeManager.java`, line 97), and the default
`isEmpty` is true when every `getItem` is empty (`RecipeInput.java`, lines 10 to 17). The record
must override `isEmpty()` to return false, or report `size()` 1 with the block's item, which also
gives JEI something to show. Matching uses `Recipe#matches(input, level)` (NF-SRC
`net/minecraft/world/item/crafting/Recipe.java`, line 22) and a `RecipeManager.CachedCheck`
(line 200) is not needed because ignition looks up once per burn.

The input field. Three options, in order of expressiveness:

1. `BlockPredicate` from `net.minecraft.advancements.critereon` (NF-SRC
   `net/minecraft/advancements/critereon/BlockPredicate.java`, record at line 26): an optional
   `HolderSet<Block>` (a block id, a list, or a `#tag`), optional `StatePropertiesPredicate`, optional
   NBT. It has `CODEC` (line 27) and `STREAM_CODEC` (line 35), a `Builder` with `of(Block...)`,
   `of(TagKey<Block>)`, and `setProperties(...)` (lines 85, 95, 105), and it matches a state directly
   through `matchesState` (line 61, private) or `matches(BlockInWorld)` (line 55). This covers the
   1.12 `pile_wood_chips` meta 0 case (the pile's `LEVEL` property, `library/spi/block/BlockPileBase.java`,
   line 29) as `{"blocks": "pyrotech:pile_wood_chips", "state": {"level": "0"}}`.
2. A `HolderSet<Block>` alone through `RegistryCodecs.homogeneousList(Registries.BLOCK)` (NF-SRC
   `net/minecraft/core/RegistryCodecs.java`, line 18) matched with `state.is(holderSet)` (NF-SRC
   `BlockBehaviour.java`, line 894). Simpler JSON, no state matching.
3. A `TagKey<Block>` per recipe (`TagKey.codec(Registries.BLOCK)`, NF-SRC `TagKey.java`, line 21).
   Every recipe then needs its own tag file.

Recommendation: option 1. It is a vanilla codec pair with network sync included, and it is the only
one that reproduces the wood chips recipe faithfully. NeoForge's `BlockTagIngredient` (NF-SRC
`net/neoforged/neoforge/common/crafting/BlockTagIngredient.java`, lines 26 to 41) is not the tool:
it is an item ingredient built from a block tag, for JEI display at most. The worldgen
`BlockPredicate` (NF-SRC `net/minecraft/world/level/levelgen/blockpredicates/BlockPredicate.java`,
line 19) tests a `WorldGenLevel` and is the wrong world type.

The other fields follow RECIPES' table: `ItemStack.STRICT_CODEC` for the result (NF-SRC
`net/minecraft/world/item/ItemStack.java`, line 126), `Codec.INT` and `Codec.FLOAT` with
`optionalFieldOf` defaults, `FluidStack.OPTIONAL_CODEC` for the per-stage fluid (NF-SRC
`FluidStack.java`, line 89), a list of `ItemStack.STRICT_CODEC` for the failure items, two
`Codec.BOOL`. Ten fields fit one `RecordCodecBuilder` group. The stream codec needs the explicit
`StreamCodec.of(writer, reader)` route, as RECIPES chose for the bloomery, because ten fields
exceed the seven-field `composite` (NF-SRC `net/neoforged/neoforge/network/codec/NeoForgeStreamCodecs.java`,
line 136); `BlockPredicate.STREAM_CODEC`, `FluidStack.STREAM_CODEC` (line 123),
`ItemStack.STREAM_CODEC` (line 155), and `ByteBufCodecs.VAR_INT`, `FLOAT`, `BOOL` (NF-SRC
`net/minecraft/network/codec/ByteBufCodecs.java`, lines 90, 108, 45) cover the fields.

A generated recipe:

```json
{
  "type": "pyrotech:pit_burn",
  "input": { "blocks": "pyrotech:log_pile" },
  "result": { "id": "minecraft:charcoal" },
  "burn_stages": 10,
  "burn_time": 9600,
  "fluid": { "id": "pyrotech:wood_tar", "amount": 50 },
  "failure_chance": 0.33,
  "failure_items": [
    { "id": "pyrotech:pit_ash" }, { "id": "pyrotech:pit_ash", "count": 2 }, { "id": "pyrotech:pit_ash", "count": 4 },
    { "id": "pyrotech:charcoal_flakes", "count": 4 }, { "id": "pyrotech:charcoal_flakes", "count": 6 }, { "id": "pyrotech:charcoal_flakes", "count": 8 }
  ],
  "requires_refractory": false,
  "fluid_affects_failure": true
}
```

`assemble` and `getResultItem` return the output (NF-SRC `Recipe.java`, lines 24 and 31). The type
registers as `RecipeType.simple(...)` (NF-SRC `net/minecraft/world/item/crafting/RecipeType.java`,
line 25). The `pyrotech:pit_recipe` Forge registry, `IForgeRegistryModifiable`, `RecipeHelper.removeRecipesByOutput`,
and `PitBurnRecipe.removeRecipes` (lines 35 to 38) all drop; datapacks override recipe files instead.

## Pit ash

| 1.12 construct | 1.21 replacement |
|---|---|
| `BlockPitAsh`: `Material.GROUND`, hardness 0.6, shovel 0, `SoundType.SAND` (`block/BlockPitAsh.java`, lines 26 to 32) | `mapColor(MapColor.COLOR_GRAY).strength(0.6f).sound(SoundType.SAND).noLootTable()` (NF-SRC `MapColor.java`, line 28; `SoundType.java`, line 40), tag `minecraft:mineable/shovel` (NF-SRC `BlockTags.java`, line 142; DATA `data/minecraft/tags/block/mineable/shovel.json`). |
| `quantityDropped` 0, `canSilkHarvest` false, `breakBlock` spawning the nine slots (lines 50 to 77) | `noLootTable()` and `onRemove` with `Containers.dropItemStack`, as for the active pile. |
| `getCreativeTabToDisplayOn` null, and JEI blacklists the item (lines 86 to 89; `plugin/jei/PluginJEI.java`, lines 43 to 44) | Decision 6: no block item. |
| `getUnlocalizedName` returning `tile.pyrotech.pile_ash` (lines 93 to 96), so the block shares core's ash pile name "Ash Pile" (1.12 `en_us.lang`, line 452) | The description id derives from the registry name, so the port needs `block.pyrotech.pit_ash_block` in the lang file. `main` has `block.pyrotech.pile_ash` (line 360) but no `pit_ash_block` key. |
| `TilePitAsh`: `LargeDynamicStackHandler(9)`, `insertItem`, full-NBT update packets (`tile/TilePitAsh.java`, lines 18 to 70) | A plain `BlockEntity` with `ItemStackHandler(9)` and `insertItem` walking slots. No slot exceeds the item's max stack (the active pile's handler already split them), so STORAGE's `LargeStackHandler` is not needed. The update packets served Waila and TOP, which drop; nothing on the client renders the contents, so no `SyncedBlockEntity`. |

## Tar collector and tar drain

Both blocks are `IBlockVariant` blocks with `STONE` (meta 0) and `BRICK` (meta 1)
(`block/BlockTarCollector.java`, lines 265 to 306; `block/BlockTarDrain.java`, lines 212 to 256, whose
comment at line 215 says the two facing bits cap the variants at four). They flatten into four
blocks. The converted blockstates on `main` still key on `variant=`; see assets.

| 1.12 construct | 1.21 replacement |
|---|---|
| `BlockTarCollector`: `Material.ROCK`, pickaxe 0, hardness 2 (lines 56 to 62); `BlockTarDrain` the same (lines 51 to 59) | `mapColor(MapColor.STONE).strength(2.0f)` (NF-SRC `MapColor.java`, line 18), pickaxe tag. One block class per kind taking capacity, hot temperature, holds-hot, range, and smoke count as constructor arguments, as STORAGE does for its stone twins. |
| `VARIANT` property and meta packing (`BlockTarCollector.java`, lines 54, 114 to 137; `BlockTarDrain.java`, lines 48 to 49, 79 to 104) | Gone with metadata. The drain keeps `BlockStateProperties.HORIZONTAL_FACING` (NF-SRC `net/minecraft/world/level/block/state/properties/BlockStateProperties.java`, line 54). |
| Drain `getStateForPlacement` facing `placer.getHorizontalFacing().getOpposite()` (lines 121 to 135) | `getStateForPlacement(BlockPlaceContext)` with `context.getHorizontalDirection().getOpposite()` (STORAGE cites NF-SRC `net/minecraft/world/item/context/UseOnContext.java`, line 70). |
| Collector `getBlockFaceShape`: `UNDEFINED` on `UP` (lines 68 to 71) | No equivalent; `BlockFaceShape` is gone. The block stays a full cube in 1.12 (no bounding box override), so `isFaceSturdy` (NF-SRC `BlockBehaviour.java`, line 944) is true on top and fire can sit there, which the collector needs. Drop the override. |
| Collector `getLightValue` from `fluid.getLuminosity` (lines 79 to 96) | `IBlockExtension#getLightEmission(state, level, pos)` (NF-SRC `IBlockExtension.java`, line 155) reading `FluidType#getLightLevel(FluidStack)` (NF-SRC `FluidType.java`, line 649); `LevelLightEngine#checkBlock` (NF-SRC `net/minecraft/world/level/lighting/LevelLightEngine.java`, line 27) when the fluid changes, as STORAGE's tank does. Tar is 0; lava in a brick collector glows. |
| `addInformation`: capacity, hot fluids, and (drain) range tooltips (`BlockTarCollector.java`, lines 100 to 110; `BlockTarDrain.java`, lines 63 to 75; `library/util/Tooltips.java`, lines 16 to 28) | `Block#appendHoverText` (NF-SRC `Block.java`, line 537). The keys `gui.pyrotech.tooltip.fluid.capacity`, `.hot.fluids.true/false`, `.drain` are on `main` (`en_us.json`, lines 401 to 410). |
| `createTileEntity` by variant (`BlockTarCollector.java`, lines 173 to 185; `BlockTarDrain.java`, lines 145 to 157) | `newBlockEntity` on each block returning the shared collector or drain block entity. |
| `igniteWithAdjacentFire`: only from `UP`, only when the tank holds any fluid, `setBurning(true)` (`BlockTarCollector.java`, lines 188 to 201) | Same method on the core interface `IBlockIgnitableAdjacentFire` (CORE moved `library/spi/block/IBlockIgnitableAdjacentFire.java`, lines 8 to 18, into core). |
| `igniteWithAdjacentIgniterBlock`: when fire can be set above and `isFlammable()` (lines 204 to 217) | Same on core's `IBlockIgnitableAdjacentIgniterBlock` (`library/spi/block/IBlockIgnitableAdjacentIgniterBlock.java`, lines 8 to 18). "Fire can be set" was `Util.canSetFire`: not a liquid, and air or replaceable (`library/util/Util.java`, lines 69 to 79). The 1.21 test is `BaseFireBlock.canBePlacedAt(level, pos, direction)` (NF-SRC `BaseFireBlock.java`, line 179), which is stricter (air only, and fire must survive there). Note the difference; the stricter test is fine. |
| `onBlockActivated`: fetch the fluid handler for the clicked face, `FluidUtilFix.interactWithFluidHandler`, return true if the held item has a fluid handler (`BlockTarCollector.java`, lines 221 to 249; `BlockTarDrain.java`, lines 175 to 203) | `useItemOn` (NF-SRC `BlockBehaviour.java`, line 226) calling `FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())` (NF-SRC `net/neoforged/neoforge/fluids/FluidUtil.java`, line 56), returning `ItemInteractionResult.sidedSuccess` or `PASS_TO_DEFAULT_BLOCK_INTERACTION` (NF-SRC `net/minecraft/world/ItemInteractionResult.java`, lines 15 and 7). Because the capability is sided (next rows), a bucket only works on the collector's top face and on the drain's front and back. That is the 1.12 behaviour; keep it. STORAGE's `FluidUtil` bucket helper is this one call. |
| Collector `isFireSource`: `UP` and burning (lines 253 to 263) | `IBlockExtension#isFireSource` reading the block entity. |
| Drain `getBlockLayer` `CUTOUT` (lines 205 to 210) | `"render_type": "minecraft:cutout"` in the converted `models/block/tar_drain.json` (NF-SRC `net/neoforged/neoforge/client/model/ExtendedBlockModelDeserializer.java`, lines 66 to 67; `net/neoforged/neoforge/client/NamedRenderTypeManager.java`, line 46; NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/client/models/). `ItemBlockRenderTypes.setRenderLayer` is `@Deprecated(since = "1.19")` (NF-SRC `net/minecraft/client/renderer/ItemBlockRenderTypes.java`, lines 460 to 461). Ticket #27. |

The tiles. `TileTarTankBase` owns a `Tank` (athenaeum `ObservableFluidTank`) whose observer marks
dirty and relights, a 20-tick `TickCounter`, and the tile data registration for the tank
(`tile/spi/TileTarTankBase.java`, lines 38 to 59). Every 20 ticks it asks the subclass for source
positions and a handler per position, then drains into itself; from a fluid block wrapper it only
takes whole buckets (lines 72 to 121). `Tank.fillInternal` runs the hot-fluid rule after filling:
if the tile cannot hold hot fluids and the fluid's temperature is at or above the tile's threshold,
set the block to air, play `ENTITY_ITEM_BREAK`, `FluidUtil.tryPlaceFluid`, and send the combust
packet (lines 179 to 204). STORAGE decision 6 (break only on `EXECUTE`) and decision 8 (the shared
`library/fluid` package) apply.

| 1.12 construct | 1.21 replacement |
|---|---|
| `TileEntityDataBase`, `TileDataFluidTank` registration, `getUpdateTag`/`getUpdatePacket`/`onDataPacket` (lines 34 to 59, 141 to 157) | PROTOTYPE `SyncedBlockEntity` with the `FluidStack` and the collector's `burning` in `saveSynced`. |
| `Tank extends ObservableFluidTank` with the `fillInternal` hot check (lines 166 to 205) | STORAGE's `HotFluidTank extends FluidTank` (NF-SRC `FluidTank.java`, line 20) with the temperature threshold, the tolerates-hot flag, and the `onHotFluid(FluidStack)` callback. The owner breaks the block with `level.removeBlock` (NF-SRC `Level.java`, line 311), plays `SoundEvents.ITEM_BREAK` (NF-SRC `net/minecraft/sounds/SoundEvents.java`, line 766), spills with `FluidUtil.tryPlaceFluid(null, level, hand, pos, handler, stack)` (NF-SRC `FluidUtil.java`, line 462), and calls the core combust helper (STORAGE item 4) which uses `ServerLevel#sendParticles` (NF-SRC `net/minecraft/server/level/ServerLevel.java`, line 1258). `ModuleStorage.PACKET_SERVICE` and `SCPacketParticleCombust` (line 196) die here. |
| Observer: `markDirty` and `checkLightFor(BLOCK, pos)` (lines 48 to 51) | `FluidTank#onContentsChanged` (NF-SRC `FluidTank.java`, line 152) calling `setChanged`, `sync()`, and `LevelLightEngine#checkBlock`. |
| `TickCounter(20)` and `update` (lines 38 to 81) | An int counter in the server ticker. |
| `collect(source, target)`: simulate-drain the free space, refuse block sources under `Fluid.BUCKET_VOLUME`, then drain and fill (lines 107 to 121) | `FluidUtil.tryFluidTransfer(dest, source, maxAmount, doTransfer)` (NF-SRC `FluidUtil.java`, line 305) for handlers; keep the whole-bucket rule for block sources with `FluidType.BUCKET_VOLUME` (NF-SRC `FluidType.java`, line 68). |
| Collector `hasCapability`/`getCapability`: fluid handler on `UP` only (`tile/spi/TileTarCollectorBase.java`, lines 65 to 82) | `RegisterCapabilitiesEvent#registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> side == Direction.UP ? be.tank : null)` (NF-SRC `net/neoforged/neoforge/capabilities/RegisterCapabilitiesEvent.java`, line 59; `net/neoforged/neoforge/capabilities/Capabilities.java`, line 29; NF-DOCS https://docs.neoforged.net/docs/1.21.1/inventories/capabilities). |
| Collector `getCollectionSourcePositions` empty, `getCollectionSourceFluidHandler` returning an active pile's tank (lines 85 to 101) | The handler method is dead code: with no positions it is never called. The active pile pushes into collectors. Drop it. |
| Collector `update`: first-tick relight; client particles when burning; server: extinguish after 100 ticks under a solid non-flammable block, spread fire to all six neighbours, consume 1 mB per `FLUID_BURN_TICKS` ticks, assert fire above (lines 104 to 162, 249 to 273) | Same loop in the server ticker. The client particle loop (`SMOKE_LARGE` times the smoke count, plus `LAVA` when fire is above, lines 183 to 210) moves to `animateTick` reading the synced `burning` flag, with `ParticleTypes.LARGE_SMOKE` and `LAVA` (NF-SRC `ParticleTypes.java`, lines 77 and 78). `shouldExtinguish` (lines 171 to 178) uses `isFaceSturdy` and `isFlammable`. |
| `tryCatchFire`: a copy of vanilla `BlockFire.tryCatchFire` with chance 100 (flammability roll, 50 percent fire placement unless raining, TNT trigger) plus lighting neighbouring collectors (lines 213 to 247) | Vanilla's `FireBlock#checkBurnOut` is private (NF-SRC `FireBlock.java`, line 262), so the copy stays. Its parts: `state.getFlammability(level, pos, face)` (NF-SRC `IBlockStateExtension.java`, line 486), `level.isRainingAt` (NF-SRC `Level.java`, line 1074), `FireBlock.AGE` (NF-SRC `FireBlock.java`, line 36), and `state.onCaughtFire(level, pos, face, null)` (NF-SRC `IBlockStateExtension.java`, line 510) which `TntBlock` implements (NF-SRC `net/minecraft/world/level/block/TntBlock.java`, line 44), replacing the hard-coded TNT check. |
| `getBurnTicksPerMilliBucket` from the `FLUID_BURN_TICKS` config map keyed by fluid name (lines 286 to 295) | A fluid-keyed data map, decision 4. |
| `setBurning` calling `BlockHelper.notifyBlockUpdate` (lines 50 to 57) | `sync()`; `burning` is a snapshot field, not a blockstate property (decision 5). |
| NBT `burning`, `burnTicksRemaining` (lines 299 to 313) | `saveAdditional`, with `burning` also in `saveSynced`. |
| Drain `fluidTank.setCanFill(false)` (`tile/spi/TileTarDrainBase.java`, line 33) | `FluidTank` has no `canFill`. Expose a wrapper whose `fill` returns 0 through the capability and keep the real tank for collection, or subclass `HotFluidTank` with `fill` returning 0 when called through the capability path. The former is one small class. |
| Drain capability on `FACING` and its opposite (lines 37 to 64) | `registerBlockEntity` with `side == facing \|\| side == facing.getOpposite()` (NF-SRC `Direction.java`, line 169). |
| Drain `getCollectionSourcePositions`: from the block in front, flood-fill a `(2R+1)` by `(2R+1)` square centred `1+R` blocks ahead, accepting positions with a handler, stopping at a fluid source at the start, collecting handler positions that are not fluid sources (lines 67 to 107) | Same code on `FloodFill`. `BlockPos.relative(direction, distance)` (NF-SRC `BlockPos.java`, line 222). |
| Drain `getCollectionSourceFluidHandler`: collector tank, active pile tank, any tile's fluid handler when `ALLOW_TILE_DRAIN`, else a fluid block wrapper when `ALLOW_SOURCE_DRAIN` (lines 117 to 158) | `instanceof` for the two known block entities; `level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null)` (NF-SRC `net/neoforged/neoforge/common/extensions/ILevelExtension.java`, line 78) for other tiles; for source blocks, `FluidBlockWrapper` and `BlockLiquidWrapper` no longer exist. The `wrappers` package holds `BucketPickupHandlerWrapper(player, bucketPickup, level, pos)` (NF-SRC `net/neoforged/neoforge/fluids/capability/wrappers/BucketPickupHandlerWrapper.java`, lines 22 and 30), which drains a `BucketPickup` block such as any `LiquidBlock` (NF-SRC `LiquidBlock.java`, line 40) one bucket at a time. Pass a null player. The two `ALLOW_*` flags bake to true (config section). |
| `TESRTarCollector extends FastTESR`: still sprite, fluid colour, light from the block above, one quad at `2px + 13px * fill` inset 2 px (`client/render/TESRTarCollector.java`, lines 17 to 84) | STORAGE's fluid box renderer: `BlockEntityRenderer#render` (NF-SRC `net/minecraft/client/renderer/blockentity/BlockEntityRenderer.java`, line 12), `IClientFluidTypeExtensions.of(fluid).getStillTexture(stack)` and `getTintColor(stack)` (NF-SRC `IClientFluidTypeExtensions.java`, lines 42, 294, 277), `Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)` (NF-SRC `net/minecraft/client/Minecraft.java`, line 2568; `net/minecraft/client/renderer/texture/TextureAtlas.java`, line 30), `LevelRenderer.getLightColor(level, pos.above())` (NF-SRC `net/minecraft/client/renderer/LevelRenderer.java`, line 3624), quads through `bufferSource.getBuffer(Sheets.translucentCullBlockSheet())` (NF-SRC `net/minecraft/client/renderer/Sheets.java`, line 144) and `VertexConsumer.addVertex(pose, x, y, z).setColor(...).setUv(...).setLight(...).setOverlay(...).setNormal(...)` (NF-SRC `com/mojang/blaze3d/vertex/VertexConsumer.java`, lines 155, 49, 20, 63, 67, 26), UVs from `getU0`/`getV0`/`getU1`/`getV1` (NF-SRC `net/minecraft/client/renderer/texture/TextureAtlasSprite.java`, lines 40 to 82). |

Comparator and redstone: none in 1.12. Neither block overrides `hasComparatorInputOverride`,
`getWeakPower`, or `canConnectRedstone`.

Item forms and loot: the four collector and drain items are plain `BlockItem`s, and 1.12 never
serialized the tank into the item (no `ItemBlock` subclass in the package). The loot tables are
`dropSelf` (NF-SRC `net/minecraft/data/loot/BlockLootSubProvider.java`, line 748), contents lost, as
in 1.12 where `breakBlock` was not overridden on either block. That means a broken collector spills
nothing and loses its tar. Faithful; noted in case Moos wants the STORAGE tank behaviour instead.

## The ignition helper rehoming

CORE wrote that `RefractoryIgnitionHelper` (40 lines) "moves to core so refractory and ignition
both reach it". The helper's body is: look up the pit burn recipe for the block state at `pos`;
if found, flood-fill every neighbour matching the recipe's input, replacing each with
`ModuleTechRefractory.Blocks.ACTIVE_PILE` and calling `TileActivePile.setRecipe(recipe)`, limited
by `ModuleTechRefractoryConfig.GENERAL.MAXIMUM_BURN_SIZE_BLOCKS` (`util/RefractoryIgnitionHelper.java`,
lines 18 to 46). Every one of those is a refractory class. Core cannot depend on them.

Who calls it:

- `ItemIgniterBase.onItemUseFinish` (ignition): after the `IBlockIgnitableWithIgniterItem` branch
  and the "set fire in the adjacent air" branch fail, call `igniteBlocks(world, pos)` on the clicked
  block, guarded by the refractory module toggle (`modules/ignition/item/ItemIgniterBase.java`,
  lines 107 to 136).
- `TileIgniter.update` (ignition): the powered igniter block sets fire in front if it can, else
  calls `igniteWithAdjacentIgniterBlock` on the block in front, else `igniteBlocks` on it
  (`modules/ignition/tile/TileIgniter.java`, lines 42 to 50).
- `NeighborNotifyEventHandler` (refractory): when a fire block notifies neighbours, call
  `igniteBlocks` on each notified side (`event/NeighborNotifyEventHandler.java`, lines 15 to 35).
- `RightClickBlockEventHandler` (refractory): flint and steel or fire charge on any block calls
  `igniteBlocks`, plays the flint sound, and sets `useItem` to `ALLOW`
  (`event/RightClickBlockEventHandler.java`, lines 19 to 43).

What is really shared is a question: "can this position start a pit burn, and if so, do it".
Only refractory can answer it. The shape that keeps the dependency direction from DEPS:

1. A core hook. Core declares an interface `BlockIgniter { boolean tryIgnite(Level, BlockPos); }`
   and a static registry `PyrotechIgnition` with `register(BlockIgniter)` and
   `tryIgnite(level, pos)` that walks the list. Refractory registers one igniter in its
   `register(IEventBus)` hook, holding the flood fill and recipe lookup. Ignition calls
   `PyrotechIgnition.tryIgnite` in both places. Core's own fire-adjacency `NeighborNotifyEvent`
   listener (CORE events table) calls it for each notified side, so refractory's
   `NeighborNotifyEventHandler` disappears.
2. A core event. Core posts a custom `Event` subclass `IgniteBlockEvent(level, pos)` on
   `NeoForge.EVENT_BUS` from the same three call sites (NF-DOCS
   https://docs.neoforged.net/docs/1.21.1/concepts/events/), and refractory subscribes. Same
   dependency direction, and other mods can hook it. Slightly more ceremony per call, and the
   result must travel back through a cancel or a setter.
3. A block interface on the input blocks. Rejected: one input is vanilla `minecraft:coal_block`
   (`BurnPitRecipesAdd.java`, line 64), which cannot implement a Pyrotech interface, and the
   recipe decides what is an input, not the block.
4. Move the recipe type and the active pile into core. Rejected: it drags the burn mechanic,
   the pit ash, and the config out of the unit for a 40-line helper.

Recommendation: option 1. Flagged as decision 1 because it revises CORE's sentence. `FloodFill`
itself moves to the shared `library/util` package regardless; the drain uses it too.

The `RightClickBlockEventHandler` stays a `PlayerInteractEvent.RightClickBlock` listener in
refractory (NF-SRC `net/neoforged/neoforge/event/entity/player/PlayerInteractEvent.java`, line 163)
rather than a `useItemOn` on the input blocks: the inputs are core blocks and a vanilla block, and
the listener keeps refractory logic out of core's `BlockLogPile`. The item test becomes
`stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT)` (NF-SRC
`net/neoforged/neoforge/common/extensions/IItemStackExtension.java`, line 122;
`net/neoforged/neoforge/common/ItemAbilities.java`, line 144), which flint and steel and fire charges
report (lines 162 and 163) and which ignition's igniters can report too. The sound is
`SoundEvents.FLINTANDSTEEL_USE` (NF-SRC `SoundEvents.java`, line 530). `event.setUseItem(TriState.TRUE)`
(line 211; NF-SRC `net/neoforged/neoforge/common/util/TriState.java`) reproduces the 1.12 `ALLOW`,
which let the flint and steel also damage itself through its own `useOn`. Vanilla `FlintAndSteelItem#useOn`
(NF-SRC `net/minecraft/world/item/FlintAndSteelItem.java`, line 30) will then try to place fire next to
the now-active pile, as 1.12 did; that is faithful and mostly harmless because the pile is a fire source.

## Events

| 1.12 handler | 1.21 replacement |
|---|---|
| `FurnaceFuelBurnTimeEventHandler`: any bucket holding a full bucket of wood tar burns 4800 ticks, coal tar 6400 (`event/FurnaceFuelBurnTimeEventHandler.java`, lines 13 to 23; `Util.isFluidBucket`, `library/util/Util.java`, lines 86 to 98) | The two `BucketItem`s go into the `neoforge:furnace_fuels` data map (NF-SRC `net/neoforged/neoforge/registries/datamaps/builtin/NeoForgeDataMaps.java`, line 67; value `FurnaceFuel(int burnTime)`, `FurnaceFuel.java`, line 17; NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/server/datamaps/builtin; NF-SRC jar `data/neoforge/data_maps/item/furnace_fuels.json` for the shape), written by refractory's `DataMapProvider` (NF-SRC `net/neoforged/neoforge/common/data/DataMapProvider.java`, lines 98 and 139). The bucket module's own buckets are BUCKET's `getBurnTime(ItemStack, RecipeType)` override (NF-SRC `net/neoforged/neoforge/common/extensions/IItemExtension.java`, line 619); where it gets the per-fluid number is decision 4. `FurnaceFuelBurnTimeEvent` survives (NF-SRC `net/neoforged/neoforge/event/furnace/FurnaceFuelBurnTimeEvent.java`, line 30) but is not needed. |
| `ItemTooltipEventHandler`: "Valid for refractory structure." on items parsed from a hard-coded list and the client config (`event/ItemTooltipEventHandler.java`, lines 14 to 29; `RegistryInitializer.java`, lines 77 to 79, 93 to 99, 110 to 117) | `ItemTooltipEvent` (NF-SRC `net/neoforged/neoforge/event/entity/player/ItemTooltipEvent.java`, lines 17, 45, 52) testing `stack.is(PyrotechTags.REFRACTORY_ITEMS)`, an item tag copied from the block tag with `ItemTagsProvider#copy` (NF-SRC `net/minecraft/data/tags/ItemTagsProvider.java`, line 74). Key `gui.pyrotech.tooltip.refractory.valid` is on `main` (line 411). The config list drops. |
| `NeighborNotifyEventHandler`: at `LOWEST` priority, if the notifying block is fire, `igniteBlocks` on every notified side (lines 15 to 35) | Folded into core's fire-adjacency listener through the ignition hook (decision 1). `BlockEvent.NeighborNotifyEvent#getNotifiedSides` still exists (NF-SRC `net/neoforged/neoforge/event/level/BlockEvent.java`, lines 168 and 183). |
| `RightClickBlockEventHandler` (lines 19 to 43) | Stays in refractory as described above. |

## Recipes and loot

Four shaped recipes under `assets/pyrotech/recipes/tech/refractory/`, each wrapped in the
`modules_enabled` condition that drops:

| Recipe | Pattern | Input (core `ItemMaterial` meta) | Result |
|---|---|---|---|
| `tar_collector_brick` | `A A` / `A A` / `AAA` | 5, `refractory_brick` | `tar_collector` data 1 |
| `tar_collector_stone` | same | 16, `brick_stone` | `tar_collector` data 0 |
| `tar_drain_brick` | `A A` / `A A` / `A A` | 5 | `tar_drain` data 1 |
| `tar_drain_stone` | same | 16 | `tar_drain` data 0 |

Metas resolve through `ItemMaterial.EnumType` (`modules/core/item/ItemMaterial.java`, lines 80 and 91).
They become four `ShapedRecipeBuilder` calls (NF-SRC `net/minecraft/data/recipes/ShapedRecipeBuilder.java`,
lines 50, 75, 96, 126) in a `TechRefractoryRecipes` static class under RECIPES' shared
`PyrotechRecipeProvider`, with the flat results `pyrotech:tar_collector_brick` and friends. The
result JSON shape is `{"count": 1, "id": "..."}` (DATA `data/minecraft/recipe/furnace.json`).

The four pit burn recipes from `BurnPitRecipesAdd.java`, emitted as `pyrotech:pit_burn/<name>` under
`data/pyrotech/recipe/pit_burn/` per RECIPES:

| Id | Input | Result | Stages | Time | Fluid per stage | Failure | Failure items | Refractory | Fluid affects |
|---|---|---|---|---|---|---|---|---|---|
| `charcoal` (lines 22 to 40) | `pyrotech:log_pile`, any state | charcoal | 10 | 9600 | wood tar 50 | 0.33 | pit ash 1, 2, 4; charcoal flakes 4, 6, 8 | no | yes |
| `charcoal_from_pile_wood_chips` (lines 43 to 58) | `pyrotech:pile_wood_chips`, `level=0` | charcoal flakes | 8 | 7200 | wood tar 25 | 0.33 | pit ash 1, 2, 4 | no | yes |
| `coal_coke` (lines 61 to 79) | `minecraft:coal_block` | coal coke | 10 | 14400 | coal tar 50 | 0.15 | pit ash 1, 2, 4; coal pieces 4, 6, 8 | yes | yes |
| `charcoal_from_wood_tar_block` (lines 82 to 100) | `pyrotech:wood_tar_block`, any state | charcoal | 10 | 6000 | none | 0.15 | pit ash 1, 2, 4; charcoal flakes 4, 6, 8 | no | no |

`level=0` on the wood chips pile is the full pile: `BlockPileBase.LEVEL` runs 0 to 7 and the model
count is `8 - level` (`library/spi/block/BlockPileBase.java`, lines 29 and 63). The log pile is a
`BlockRotatedPillar` with an axis (`modules/core/block/BlockLogPile.java`, lines 15 to 16 and 63),
which the wildcard covers; in 1.21 a `BlockPredicate` with no properties matches every axis.

Loot tables, from a refractory section of RECIPES' `PyrotechBlockLoot`: `dropSelf` for the four
collector and drain blocks; nothing for the active pile and pit ash, which use `noLootTable()`.
The `noDrop()` builder exists (NF-SRC `BlockLootSubProvider.java`, line 681) if an explicit empty
table is preferred. Output lands in `data/pyrotech/loot_table/blocks/` (NF-DOCS loot tables page;
DATA `data/minecraft/loot_table/blocks/coal_block.json` for the vanilla shape).

Datagen placement per RECIPES: one `TechRefractoryRecipes` class, one loot section, one
`TechRefractoryTags` contribution to the shared tag providers (`#pyrotech:refractory`, the item
copy, the two fluid tags, the two mineable tags), and one `DataMapProvider` for the furnace fuels
and the fluid fuel map (decision 4). `GatherDataEvent#includeServer` gates them (NF-SRC
`net/neoforged/neoforge/data/event/GatherDataEvent.java`, line 111).

## Network payloads

Refractory registers no packets of its own. `ModuleTechRefractory.PACKET_SERVICE` exists only to
create the tile data service (`ModuleTechRefractory.java`, lines 65 to 66). The single packet it
sends is core's `SCPacketParticleCombust` through storage's channel (`TileTarTankBase.java`, line
196), which CORE dropped for `ServerLevel#sendParticles` and STORAGE wraps in the core combust helper.
The tile data service is replaced by the `SyncedBlockEntity` snapshot. Refractory contributes zero
payload types to core's registrar (NF-DOCS https://docs.neoforged.net/docs/1.21.1/networking/payload/).

## Config

`ModuleTechRefractoryConfig` is 329 lines. Every knob, its 1.12 default, and its proposed fate:

| Category and knob | Default | Fate |
|---|---|---|
| `CLIENT.VALID_REFRACTORY_TOOLTIP` (line 25) | empty | Drop. The tooltip reads the item tag. |
| `GENERAL.MAXIMUM_BURN_SIZE_BLOCKS` (line 41, range 1 to 512) | 27 | Config. Flood fill limit for ignition and the refractory check. `ModConfigSpec.Builder#defineInRange(path, 27, 1, 512)` (NF-SRC `net/neoforged/neoforge/common/ModConfigSpec.java`, line 770). |
| `REFRACTORY.REFRACTORY_FAILURE_MODIFIER` (line 59) | 0.1 | Config, `defineInRange(path, 0.1, 0, ...)` (line 753). |
| `REFRACTORY.MAX_FAILURE_CHANCE` (line 67) | 0.99 | Config. |
| `REFRACTORY.MIN_FAILURE_CHANCE` (line 75) | 0.01 | Config. |
| `REFRACTORY.ACTIVE_PILE_MAX_FLUID_CAPACITY` (line 85) | 500 | Bake as a constant. JEI's gauge reads it too (`plugin/jei/category/JEIRecipeCategoryPitBurn.java`, line 94). |
| `REFRACTORY.FLUID_BURN_TICKS` (lines 92 to 95) | wood_tar 20, coal_tar 40 | Data: the fluid-keyed data map of decision 4. |
| `REFRACTORY.REFRACTORY_BRICKS` (line 108) | empty | Data: the `#pyrotech:refractory` block tag. |
| `REFRACTORY.BASE_RECIPE_DURATION_MODIFIER` (line 116) | 1 | Config, per RECIPES decision 4. |
| `REFRACTORY.REFRACTORY_RECIPE_DURATION_MODIFIER` (line 124) | 1 | Config, same. |
| `STONE_TAR_COLLECTOR.SMOKE_PARTICLES_PER_TICK`, `CAPACITY`, `HOT_TEMPERATURE`, `HOLDS_HOT_FLUIDS` (lines 141 to 163) | 10, 2000, 450, false | Bake as constructor arguments. |
| `BRICK_TAR_COLLECTOR` same four (lines 180 to 202) | 10, 4000, 450, true | Bake. |
| `STONE_TAR_DRAIN.CAPACITY`, `HOT_TEMPERATURE`, `HOLDS_HOT_FLUIDS`, `RANGE`, `ALLOW_SOURCE_DRAIN`, `ALLOW_TILE_DRAIN` (lines 218 to 252) | 1000, 450, false, 1, true, true | Bake the numbers; drop the two flags (always on). |
| `BRICK_TAR_DRAIN` same six (lines 268 to 302) | 2000, 450, true, 2, true, true | Bake; drop the flags. |
| `FUEL.COAL_TAR_BURN_TIME_TICKS` (line 319) | 6400 | Data: `furnace_fuels` for the bucket item, the fluid map for other containers. |
| `FUEL.WOOD_TAR_BURN_TIME_TICKS` (line 327) | 4800 | Data, same. |

That leaves six values in a `refractory` section of the shared `ModConfigSpec` (`push`/`pop`,
lines 842 and 860; `ModContainer#registerConfig` and `ModConfig.Type.COMMON` per NF-DOCS
https://docs.neoforged.net/docs/1.21.1/misc/config/). The surface is decision 3.

## Dropped outright

- `plugin/`: 11 files. `ZenBurn` (CraftTweaker, 244 lines), `PluginTOP` and its `TankProvider`,
  `PluginWaila` with `TankProviderDelegate` and `TankProvider`, and the five JEI classes. The JEI
  categories return in the shared plugin (assets and JEI section); the 1.12 code does not.
- `ModuleBase`, `Registry`, `enableAutoRegistry`, `IPacketService`, `ITileDataService`,
  `Injector`, `@GameRegistry.ObjectHolder`, `FMLInterModComms`, the `modules_enabled` recipe
  conditions, and the `isModuleEnabled` guards in `ItemIgniterBase` and `RegistryInitializer`.
- `FluidRegistry.enableUniversalBucket`, `FluidInitializerRegistry`, `BlockFluidClassic`,
  `blockstates/fluid.json`, `FluidUtilFix`.
- `BlockMetaMatcher`, `RecipeItemParser`, `ParseResult`, `Util.parseBlockStringWithWildcard`, and
  the `pyrotech:pit_recipe` Forge registry with its `RecipeHelper.removeRecipesByOutput`.
- `getStateFromMeta`, `getMetaFromState`, `damageDropped`, `getSubBlocks`, `getModelName`,
  `IBlockVariant`, `getBlockFaceShape`, `getBlockLayer`, `FastTESR`, `TickCounter`,
  `ObservableFluidTank`, `LargeDynamicStackHandler`, `StackHelper.spawnStackOnTop`.
- The Sponge next-tick workaround in `onAllBurnStagesComplete`.
- The dead collector `getCollectionSourceFluidHandler` path.
- `ModuleTechRefractory.LOGGER`; the port uses `Pyrotech.LOGGER`.

## Assets

What `main` has for this unit (all under `src/main/resources/assets/pyrotech/`):

- Blockstates: `active_pile.json`, `pit_ash_block.json`, `wood_tar_block.json` (core's tar bale,
  not a refractory block), `tar_collector.json` keyed on `variant=brick|stone`, `tar_drain.json`
  keyed on `facing=` and `variant=`.
- Block models: `active_pile.json` (`cube_all` on the animated `active_pile` texture),
  `pit_ash_block.json` (`cube_all` on `ash_block`), `tar_collector.json` (a basin with 2 px walls
  and floor and a centre rib), `tar_drain.json` (a cube with `tar_collector_top` on the back and
  `tar_drain` on the front), and the generated wrappers `gen/tar_collector/tar_collector.json`
  (refractory brick texture) and `tar_collector_2.json` (masonry brick, the stone variant),
  `gen/tar_drain/tar_drain.json` and `tar_drain_2.json` the same way.
- Textures: `block/active_pile.png` plus `.mcmeta`, `block/ash_block.png`, `block/tar_collector_top.png`,
  `block/tar_drain.png`, `block/fluid_wood_tar_still.png`, `_flow.png`, `block/fluid_coal_tar_still.png`,
  `_flow.png`, each with `.mcmeta`, and `block/refractory_brick.png`, `block/masonry_brick.png`.
  `textures/gui/jei5.png` (the burn category background) is present.
- Lang: `block.pyrotech.tar_collector_stone`, `tar_collector_brick`, `tar_drain_stone`,
  `tar_drain_brick` (lines 186 to 189), `gui.pyrotech.jei.category.burn.pit` and `.refractory`
  (lines 472 to 473), `gui.pyrotech.jei.info.burn` and `.refractory` (lines 490 to 491),
  `gui.pyrotech.jei.failure` (line 461), the tooltip keys, and the dead 1.12 fluid keys.

Gaps:

- Blockstates for the flattened blocks. `tar_collector.json` and `tar_drain.json` must split into
  `tar_collector_stone.json` (pointing at `gen/tar_collector/tar_collector_2`), `tar_collector_brick.json`
  (`tar_collector`), `tar_drain_stone.json` and `tar_drain_brick.json` (facing rotations kept,
  `variant=` removed). Watch the texture mapping: the `_2` wrappers are the stone variant.
- Item models: none exist for `tar_collector_stone`, `tar_collector_brick`, `tar_drain_stone`,
  `tar_drain_brick`. Each needs `models/item/<id>.json` with the block model as parent
  (`ItemModelProvider#simpleBlockItem`, NF-SRC `net/neoforged/neoforge/client/model/generators/ItemModelProvider.java`,
  line 54, or static JSON per the map decision). `pit_ash_block` needs one only if it keeps an item.
- Fluid assets: `blockstates/wood_tar.json` and `coal_tar.json` with a `""` variant, and
  particle-only `models/block/wood_tar.json` and `coal_tar.json`, copying vanilla water (DATA
  `assets/minecraft/blockstates/water.json`, `assets/minecraft/models/block/water.json`).
- Bucket models: `models/item/wood_tar_bucket.json` and `coal_tar_bucket.json`. The 1.12 universal
  bucket drew the vanilla bucket with a fluid overlay. NeoForge ships the equivalent as
  `assets/neoforge/models/item/bucket.json` (base `item/bucket`, fluid mask
  `neoforge:item/mask/bucket_fluid`) on the `neoforge:fluid_container` loader BUCKET documents.
  Two static JSON files with `"fluid": "pyrotech:wood_tar"` and `"pyrotech:coal_tar"` reproduce the
  1.12 look with no new textures.
- Lang: `block.pyrotech.pit_ash_block`, `block.pyrotech.active_pile` (only shown by debug tools,
  but the key should exist), `fluid_type.pyrotech.wood_tar`, `fluid_type.pyrotech.coal_tar`,
  `block.pyrotech.wood_tar`, `block.pyrotech.coal_tar`, `item.pyrotech.wood_tar_bucket`,
  `item.pyrotech.coal_tar_bucket`. The 1.12 keys `fluid.wood_tar` and `tile.fluid.wood_tar.name`
  on `main` are dead.
- Render types (ticket #27): `tar_drain.json` needs `"render_type": "minecraft:cutout"` (1.12
  `getBlockLayer` returned `CUTOUT`). `active_pile`, `pit_ash_block`, and `tar_collector` used
  the 1.12 default solid layer; nothing to add.

JEI. The 1.12 plugin registered two categories: "Pit Burning" (`pyrotech.pit.burn`, catalyst dirt,
recipes not requiring refractory) and "Refractory Burning" (`pyrotech.refractory.burn`, catalyst
refractory brick, all recipes), showed the input as the block's item, the output and fluid
multiplied by the stage count, the failure items and percentage, and ingredient info text on the
outputs (`plugin/jei/PluginJEI.java`, lines 46 to 101; `plugin/jei/wrapper/JEIRecipeWrapperPitBurn.java`,
lines 40 to 65). The shared plugin needs those two categories for the `pyrotech:pit_burn` type,
split by `requiresRefractory`. Not designed here.

## Findings for other tickets

- **Core** (issue #10): CORE's "`RefractoryIgnitionHelper` moves to core" becomes "core owns an
  ignition hook; refractory registers into it" (decision 1). Core also owns the `#pyrotech:refractory`
  `TagKey` because tech/basic reads it, and core's datagen adds `refractory_brick_block`,
  `refractory_glass`, and the refractory brick slab to it (double-slab state check stays in code).
  `FloodFill` and `TileBurnableBase` go to the shared `library` package.
- **Ignition** (issue #18): `ItemIgniterBase` and `TileIgniter` call the core hook instead of the
  helper; the module toggle guard drops. The brick igniter block joins `#pyrotech:refractory` in
  ignition's datagen. Igniter items should answer `canPerformAction(FIRESTARTER_LIGHT)` so the
  pit burn right-click listener treats them like flint and steel.
- **Tech/basic** (issue #16): `TileKilnPit` extends `TileBurnableBase` and reads the refractory
  block list (`TileKilnPit.java`, lines 59, 368, 390, 441 to 450); with the tag in core this
  edge disappears. `SoakingPotRecipesAdd` consumes `WOOD_TAR` and `COAL_TAR` fluids in ten
  recipes (`SoakingPotRecipesAdd.java`, lines 64 to 258). Those datagen methods reference
  refractory's fluid holders, so refractory should hoist before tech/basic's soaking pot recipes
  are generated, or the two fluids move to core next to the wines (decision 7).
- **Tech/machine**: the "Refractory Kiln" of the ticket text is the brick kiln in tech/machine,
  and `StoneCrucibleRecipesAdd` consumes `WOOD_TAR` (`StoneCrucibleRecipesAdd.java`, lines 111 to 113).
  Same fluid ordering note.
- **Render layers** (issue #27): `tar_drain.json` needs `render_type` cutout. The other three
  refractory models are solid.
- **Storage** (issue #14): `HotFluidTank`, the fluid box renderer, and the core combust helper
  are consumed here exactly as STORAGE decision 8 proposed. The drain needs a read-only capability
  wrapper on top of `HotFluidTank`; worth adding to `library/fluid` as a one-class helper.
- **Recipe architecture** (issue #9): add `PitBurnRecipe` with `PitBurnRecipeInput(BlockState)` and
  a `BlockPredicate` input to the class table. It is the first recipe whose input is not an item.
- **JEI plugin** (issue #9 follow-up): two categories, `pit_burn` and `refractory_burn`, over one
  recipe type.
- **Bucket** (issue #13): BUCKET's `getBurnTime` override needs the per-fluid furnace value for
  tar; decision 4 gives it a fluid data map to read.
- **Sign-off**: verify in game that a lit log pile under a brick collector fills it, that a stone
  collector breaks on lava, that the drain pulls placed tar back, and that fire above a burning
  collector persists.

## Decisions for Moos

1. **Ignition hook shape (revises CORE).** `RefractoryIgnitionHelper` cannot move to core: it
   needs the pit burn recipe type, the active pile block and block entity, and the burn size
   config. Options: a core `BlockIgniter` interface with a static registry that ignition calls
   and refractory registers into; a core-posted `IgniteBlockEvent` on `NeoForge.EVENT_BUS` that
   refractory listens to; or moving the recipe type and active pile into core. Recommendation:
   the interface and registry. It is the smallest change to CORE's wording, keeps every
   refractory class in refractory, and lets core's fire-adjacency listener drive pit burns so
   refractory's own `NeighborNotifyEventHandler` disappears.
2. **Pit burn input encoding.** Options: vanilla `BlockPredicate` (block or tag plus optional state
   properties, codec and stream codec included); a bare `HolderSet<Block>`; or one block tag per
   recipe. Recommendation: `BlockPredicate`. Only it reproduces the wood chips recipe's `level=0`
   match.
3. **Config surface.** Following CORE decision 3 and RECIPES decision 4, keep six knobs in a
   `refractory` section (`MAXIMUM_BURN_SIZE_BLOCKS`, `REFRACTORY_FAILURE_MODIFIER`,
   `MIN_FAILURE_CHANCE`, `MAX_FAILURE_CHANCE`, `BASE_RECIPE_DURATION_MODIFIER`,
   `REFRACTORY_RECIPE_DURATION_MODIFIER`), bake the eighteen per-block numbers and the active pile
   capacity, drop the two `ALLOW_*` flags and the tooltip list, and move the rest to data.
   Alternative: bake everything. Recommendation: the six knobs. They tune the failure model, which
   is the mechanic players argue about.
4. **Per-fluid fuel data.** 1.12 keys two things by fluid name in config: the collector's ticks per
   mB (`FLUID_BURN_TICKS`) and the furnace burn time of a filled bucket. Options: a custom
   fluid-keyed data map `pyrotech:fluid_fuels` with `collector_ticks_per_mb` and `bucket_burn_time`
   fields (registered in `RegisterDataMapTypesEvent` with `DataMapType.builder`, NF-SRC
   `net/neoforged/neoforge/registries/datamaps/RegisterDataMapTypesEvent.java`, line 21, and
   `DataMapType.java`, line 85; read through `Holder#getData`, `IWithData.java`, line 23, from
   `FluidStack#getFluidHolder`, `FluidStack.java`, line 237); or constants in refractory plus
   `furnace_fuels` entries for the two bucket items only. Recommendation: the data map, with
   the two `BucketItem`s also listed in `furnace_fuels`. It gives BUCKET's `getBurnTime` override
   a single lookup for any container, and datapacks can add burnable fluids as the 1.12 comment
   invited ("Other fluids may be added here", config line 89).
5. **Collector `burning`: blockstate or snapshot.** Options: a `lit` `BooleanProperty`
   (`BlockStateProperties.LIT`, NF-SRC `BlockStateProperties.java`, line 26), which follows the
   PROTOTYPE rule but selects no model and forces hand edits to the converted blockstates; or a
   boolean in the `SyncedBlockEntity` snapshot beside the fluid. Recommendation: the snapshot, the
   same exception STORAGE took for the faucet's `active` (its decision 7).
6. **Pit ash item.** 1.12 registers an `ItemBlock` for the pit ash block but hides it from the
   creative tab and blacklists it in JEI. Options: no item (the block is only ever placed by a
   finished burn), or an item hidden from the tab for `/give`. Recommendation: no item. One less
   model and lang key, and nothing in 1.12 crafts or drops it.
7. **Where the tar fluids live.** Tech/basic's soaking pot and tech/machine's crucible consume
   wood tar and coal tar. Options: keep the fluids in refractory and hoist refractory before those
   datagen methods run; or move the two fluids and their buckets into core beside the wines so no
   tech unit references refractory. Recommendation: keep them in refractory, which produces them,
   and order the hoist. Moving them to core spreads one unit's assets over two for the sake of a
   datagen reference inside one compilation unit.
8. **Collector and drain contents on break.** 1.12 drops the plain block and loses the tar. Options:
   faithful (drop self, lose contents), or adopt STORAGE's tank behaviour (a `SimpleFluidContent`
   component copied by the loot table). Recommendation: faithful. The tar tanks are process
   blocks, not storage, and nothing in the 1.12 book suggests carrying tar in a collector.
