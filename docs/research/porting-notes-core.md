# Porting notes: core

Resolves issue #10 (porting notes: core).
Core is the first hoisting unit from issue #4 (module porting order): 166 files, 14,409 lines.
It holds the shared items, rocks and stones, config, network packets, and compat initializers.
This document lists each 1.12 construct in core and its 1.21 replacement.
Decisions that need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo, package `com.codetaylor.mc.pyrotech.modules.core`.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths cited below are entries in that jar.
- **NF-DOCS**: docs.neoforged.net, 1.21.1 version pages. URLs cited inline.
- **DATA**: the vanilla 1.21.1 data pack, at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`.
- **DEPS**: `docs/research/module-dependencies.md` on branch `research/module-dependencies`.

## Summary

- Every registration path collapses into `DeferredRegister` fields wired to the mod event bus.
- Metadata dies. `ItemMaterial` (55 subtypes) and `BlockRock` (variant property) flatten into one
  registry entry per variant. The migrated assets on `main` already assume the flat names.
- The two athenaeum packet services become one set of `CustomPacketPayload` records registered
  under the `pyrotech` namespace with a single version string. Most core packets disappear.
- The igniter cycle between tech and ignition breaks with an item tag plus a small core interface.
- The furnace fuel event handler dissolves into the `neoforge:furnace_fuels` data map.
- The ore dictionary, the runtime JSON compat system, and the module toggle handshake are dropped.

## Module bootstrap

| 1.12 construct | 1.21 replacement |
|---|---|
| `ModPyrotech` + athenaeum `ModuleBase` lifecycle (pre-init, init, post-init) | The existing `@Mod` class `Pyrotech(IEventBus)`. Modules become plain packages with a static `register(IEventBus)` hook called from the constructor. |
| `ModPyrotechConfig.MODULES` toggle map, `SCPacketModuleListRequest` handshake, kick on mismatch | Dropped. Issue #4 removed per-module toggles. NeoForge's own connection checks cover mod matching. |
| `ModuleCorePost` (ore dictionary entries after all modules) | Dropped. Tags load from data, ordering is not a concern. |
| `Registry` + `enableAutoRegistry()` (athenaeum) | `DeferredRegister.createBlocks(MOD_ID)`, `DeferredRegister.createItems(MOD_ID)`, and `DeferredRegister.create(Registries.X, MOD_ID)` for the rest. Each registers on the mod bus once (NF-SRC `net/neoforged/neoforge/registries/DeferredRegister.java`; NF-DOCS https://docs.neoforged.net/docs/1.21.1/concepts/registries/). |
| `@GameRegistry.ObjectHolder` holder classes (`ModuleCore.Blocks`, `.Items`, injected nulls) | The `DeferredBlock`/`DeferredItem` fields returned by `register(...)` are the holders. `@ObjectHolder` no longer exists in NeoForge 21.1 (NF-SRC, no such class). |
| `setRegistryName`, `GameRegistry.register` | Gone. The name string passed to `DeferredRegister#register` is the registry id, namespaced automatically. |
| `ModPyrotech.CREATIVE_TAB` (one `CreativeTabs` instance) | One `CreativeModeTab` registered under `Registries.CREATIVE_MODE_TAB`, built with `CreativeModeTab.builder().title(...).icon(...).displayItems(...)` (NF-SRC `net/minecraft/world/item/CreativeModeTab.java`). |
| `@Mod.EventBusSubscriber` | `@EventBusSubscriber(modid = ...)` from `net.neoforged.fml.common`. Default bus is `Bus.GAME`; mod-bus events need `bus = Bus.MOD` (NF-DOCS https://docs.neoforged.net/docs/1.21.1/concepts/events/). |

## Registration idioms

### Blocks

Register through `DeferredRegister.Blocks#registerBlock(name, ctor, properties)` or
`registerSimpleBlock`. Block items go through `DeferredRegister.Items#registerSimpleBlockItem`
(NF-SRC `DeferredRegister.java`).

Property calls move from setter methods on the block to `BlockBehaviour.Properties.of()` with
`sound(...)`, `strength(destroyTime, explosionResistance)`, `mapColor(...)`, `lightLevel(...)`,
`requiresCorrectToolForDrops()` (NF-SRC `net/minecraft/world/level/block/state/BlockBehaviour.java`).
1.21.1 does not need the `setId` call that later versions require (verified absent in NF-SRC).

Vanilla base classes replace the 1.12 customs:

- `BlockMasonryBrickSlab.Half` and `.Double` (two blocks) become one `SlabBlock` with the
  `SlabType` state. Same for the refractory slab.
- `BlockRefractoryDoor` and `BlockStoneDoor` extend `DoorBlock` with a `BlockSetType`.
  The separate `ItemDoorStone` item class is not needed.
- Walls and stairs extend `WallBlock` and `StairBlock` directly.
- The berry bushes map to the `SweetBerryBushBlock` pattern. `BlockFarmlandMulched` follows
  vanilla `FarmBlock`.

### Items and variant flattening

`ItemMaterial` is one item with 55 metadata subtypes (`setHasSubtypes(true)`, `EnumType.getMeta()`).
Metadata does not exist in 1.21. Each subtype becomes its own registered item.

The flat ids are the 1.12 variant names: `pyrotech:twine`, `pyrotech:board`, `pyrotech:coal_coke`,
and so on. The migrated lang file on `main` already uses these keys
(`item.pyrotech.twine` in `src/main/resources/assets/pyrotech/lang/en_us.json`), so the default
description ids line up with no lang work. `BlockRock` flattens the same way. The migrated
blockstates already carry `rock_stone.json` through `rock_wood_chips.json`.

Consequences of flattening:

- `getSubItems` overrides disappear. The creative tab's `displayItems` generator lists the items.
- `getUnlocalizedName` overrides disappear. The description id derives from the registry name.
- Recipes that accepted "any material meta" switch to item tags.
- Registration is bulk work. A loop over an enum of variant names keeps it short.

Simple items use `Item.Properties`: `stacksTo(int)`, `durability(int)`, `rarity(...)`.
These write vanilla data components (`DataComponents.MAX_STACK_SIZE`, `MAX_DAMAGE`)
(NF-SRC `net/minecraft/world/item/Item.java`, `net/minecraft/core/component/DataComponents.java`).

### Foods

1.12 food items override `ItemFood` behavior. In 1.21 food is data:
`Item.Properties.food(new FoodProperties.Builder().nutrition(...).saturationModifier(...).build())`.
Effects hang off the builder (`effect(MobEffectInstance, chance)`), which covers the wines,
the cocktail, tainted meat, and burned food (NF-SRC `net/minecraft/world/food/FoodProperties.java`).
Items that need behavior beyond data (the cocktail throw) keep a small item class.

### Hammers and tool materials

1.12 hammers extend `ItemTool` with `Item.ToolMaterial` entries created by
`EnumHelper.addToolMaterial` (`ModuleCore.Materials.REDSTONE`, `.QUARTZ`).
`EnumHelper` is gone. The 1.21 equivalent of a tool material is the `Tier` interface
(`SimpleTier` in NeoForge). Only the tool module's redstone and quartz tools use these two
materials, so the `Tier` constants move to the tool module with them.

The hammers themselves are crafting-grid tools, not mining tools. They become plain items with
`durability(...)`. The "damage the tool instead of consuming it" behavior moves onto the item:
NeoForge 21.1 has stack-sensitive `getCraftingRemainingItem(ItemStack)` and
`hasCraftingRemainingItem(ItemStack)` (NF-SRC
`net/neoforged/neoforge/common/extensions/IItemExtension.java`, lines 186 to 208).
With that override in place, the hammer recipes stay plain JSON shapeless recipes.

### Fluids

The four fluids (clay, three wines) leave athenaeum's fluid registry for the NeoForge stack:

- One `FluidType` each, registered under `NeoForgeRegistries.Keys.FLUID_TYPES`.
- `BaseFlowingFluid.Source` and `.Flowing` pairs under `Registries.FLUID`, configured with
  `new BaseFlowingFluid.Properties(type, still, flowing).bucket(...).block(...)`.
- A `LiquidBlock` and a `BucketItem` each.

`NeoForgeMod`'s milk registration is the reference pattern (NF-SRC
`net/neoforged/neoforge/common/NeoForgeMod.java`, lines 482 to 668;
`net/neoforged/neoforge/fluids/BaseFlowingFluid.java`).

### Entities

`EntityEntryBuilder` is gone. `EntityType`s register like any other object:
`EntityType.Builder.of(factory, MobCategory.MISC).sized(w, h).clientTrackingRange(...).build("id")`
under `Registries.ENTITY_TYPE` (NF-SRC `net/minecraft/world/entity/EntityType.java`).

The thrown rocks and the pyroberry cocktail extend `ThrowableItemProjectile`, the snowball base
class (NF-SRC `net/minecraft/world/entity/projectile/ThrowableItemProjectile.java`).
Renderers register in the client event `EntityRenderersEvent.RegisterRenderers`, using vanilla
`ThrownItemRenderer` where the entity just shows its item.
`ItemBook.EntityItemBook` (a custom dropped-item entity) should be replaced by
`Item.Properties` flags where possible; check what it guards during the port.

### Sounds

`SoundInitializer` becomes a `DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID)` with
`SoundEvent.createVariableRangeEvent(id)` per entry (NF-SRC `net/minecraft/sounds/SoundEvent.java`).
The migrated `sounds.json` on `main` already carries the events.

### Block entities

`TileFarmlandMulched` becomes a `BlockEntity` registered with
`BlockEntityType.Builder.of(factory, blocks...).build(null)` under `Registries.BLOCK_ENTITY_TYPE`
(NF-SRC `net/minecraft/world/level/block/entity/BlockEntityType.java`).
`IProgressProvider` stays a plain interface in core.

### Advancement trigger

`AdvancementTriggers.MOD_ITEM_TRIGGER` (`pyrotech:pickup_mod_item`) backs the root advancement.
1.12 registered it with `CriteriaTriggers.register`. In 1.21 custom triggers extend
`SimpleCriterionTrigger`, provide a `Codec`, and register under `Registries.TRIGGER_TYPE`
(NF-SRC `net/minecraft/advancements/critereon/SimpleCriterionTrigger.java`).
The firing hook is `ItemEntityPickupEvent.Post` (see the events table).
Whether to port the trigger or switch the root advancement to a vanilla criterion is a decision
for Moos (see below). Advancement JSON moves from `assets/pyrotech/advancements/` to
`data/pyrotech/advancement/` (singular, 1.21 layout).

## The shared network channel

Athenaeum gave every module its own `IPacketService` plus a `ITileDataService`.
NeoForge 1.21 has no channel objects at all. `SimpleChannel` does not exist in the 21.1 sources.
Payloads are registered one by one under namespaced ids, grouped by a version string
(NF-DOCS https://docs.neoforged.net/docs/1.21.1/networking/payload/).

The replacement, once, in core:

- Each packet is a record implementing `CustomPacketPayload` with a
  `CustomPacketPayload.Type<T>` constant and a `StreamCodec` built with `StreamCodec.composite`
  and `ByteBufCodecs`.
- One mod-bus listener handles `RegisterPayloadHandlersEvent`. It calls
  `event.registrar("1")` and then `playToClient(...)` or `playToServer(...)` per payload.
  The version string replaces the per-module channel identity. On NeoForge-to-NeoForge
  connections a version mismatch refuses the connection (NF-SRC
  `net/neoforged/neoforge/network/registration/PayloadRegistrar.java`).
- Handlers run on the main thread by default. `IPayloadContext` gives `player()` and
  `enqueueWork(...)`.
- Sending goes through the static `PacketDistributor` methods: `sendToPlayer`,
  `sendToPlayersTrackingChunk`, `sendToServer`
  (NF-SRC `net/neoforged/neoforge/network/PacketDistributor.java`).

Later modules add payloads to the same registrar. Core owns the listener; modules contribute
through the same registration hook they use for registries.

Fates of the ten core packets:

| 1.12 packet | Fate |
|---|---|
| `SCPacketParticleLava`, `...BoneMeal`, `...Combust`, `...Drip`, `...Gloamberry`, `...Progress` | Dropped. `ServerLevel.sendParticles(...)` sends particles to tracking clients without a custom packet (NF-SRC `net/minecraft/server/level/ServerLevel.java`). |
| `SCPacketModuleListRequest`, `CSPacketModuleListResponse` | Dropped with the module toggles. |
| `SCPacketRestartRequired` | Dropped with the runtime compat JSON system. |
| `SCPacketNoHunger` | Ported as a core payload. Hunting sends it through a helper in core, which removes hunting's borrow of tech/basic's channel noted in DEPS. |
| `SCPacketTileData` (athenaeum tile sync) | Not core's problem. The athenaeum tile sync replacement is its own prototype ticket per issue #4. Core only owns the payload registrar it will plug into. |

## The igniter contract

The 1.12 cycle (DEPS): campfire, pit kiln, bloomery, and the stone combustion worker test
`heldItem.getItem() instanceof ItemIgniterBase`, an ignition-module class. `ItemIgniterBase` in
turn calls `RefractoryIgnitionHelper` from tech/refractory. Issue #4 decided a shared igniter
contract in core breaks this cycle.

The 1.21 shape, all in core:

- An item tag `#pyrotech:igniters`, created with `ItemTags.create(...)` and written by the
  datagen `ItemTagsProvider`. Blocks test `stack.is(PyrotechTags.IGNITERS)` instead of
  `instanceof`. Tags also work in recipes and for datapack extension
  (NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/server/tags/).
- The block-side interfaces move from `library/spi/block` into core unchanged in spirit:
  `IBlockIgnitableWithIgniterItem`, `IBlockIgnitableAdjacentFire`,
  `IBlockIgnitableAdjacentIgniterBlock`.
- A small core interface for the item side (working name `IgniterItem`) carrying the shared
  use behavior that `ItemIgniterBase` implements today. Ignition's items implement it.
- `RefractoryIgnitionHelper` (40 lines) moves to core so refractory and ignition both reach it.

Tag checks cover the "is this an igniter" question. The interface covers behavior.
Whether both are wanted, or the tag alone, is flagged for Moos below.

The fire-spread ignition path stays: `NeighborNotifyEventHandler` listens for fire and calls
`IBlockIgnitableAdjacentFire` neighbors. `BlockEvent.NeighborNotifyEvent` still exists in
NeoForge 21.1 (NF-SRC `net/neoforged/neoforge/event/level/BlockEvent.java`).

## Config

1.12 uses the Forge `@Config` annotation system. `ModuleCoreConfig` is 1,141 lines across 16
categories (tweaks, hammers, food, fuel, rocks, plants, recipes, client, ore compat, more).

The 1.21 system is `ModConfigSpec` built with `ModConfigSpec.Builder` and registered in the mod
constructor via `ModContainer.registerConfig(ModConfig.Type.COMMON, spec)`. Values are read
through `ConfigValue#get()` and are only safe after `ModConfigEvent.Loading`
(NF-SRC `net/neoforged/neoforge/common/ModConfigSpec.java`;
NF-DOCS https://docs.neoforged.net/docs/1.21.1/misc/config/).

Not everything should move:

- The fuel category feeds the furnace fuel values. Those become data map entries (next section).
- The ore compat category dies with the ore dictionary.
- The recipes category (vanilla recipe removal lists) becomes datapack overrides.
- The module toggles are already dropped.

How much of the rest survives is a decision for Moos (see below).
`ConfigChangedEventHandler` becomes a `ModConfigEvent.Reloading` listener on the mod bus.

## Furnace fuel: event handler to data map

`FurnaceFuelBurnTimeEventHandler` and `ItemMaterial.getItemBurnTime` set burn times in code from
config values. NeoForge 1.21 moves fuel data into the `neoforge:furnace_fuels` data map:
`data/neoforge/data_maps/item/furnace_fuels.json`, value shape `{"burn_time": <ticks>}`
(NF-SRC `net/neoforged/neoforge/registries/datamaps/builtin/NeoForgeDataMaps.java`;
NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/server/datamaps/builtin).
`DataMapProvider` generates the file in datagen.

Mapping:

- Core items and blocks with burn times (charcoal flakes, straw, coal coke, boards, kindling,
  wood chips, tarred planks, tarred wool, log pile, pyroberries, the tar blocks) get `values`
  entries with the 1.12 default ticks.
- The handler's back reference into tech set campfire and stone worktable burn time to zero.
  Zero means "not a fuel". Those become `remove` entries in tech/basic's own data map file when
  tech/basic is ported. This resolves the one real core-to-tech edge from DEPS.
- `FurnaceFuelBurnTimeEvent` still exists in 21.1 for dynamic cases, but core does not need it
  if the values are static (NF-SRC `net/neoforged/neoforge/event/furnace/FurnaceFuelBurnTimeEvent.java`).

Whether burn times stay user-configurable is flagged for Moos below.

## Ore dictionary and the compat initializers

| 1.12 construct | 1.21 replacement |
|---|---|
| `OreDictionary` entries, `OreDictInitializer`, `JsonOreDict` | Item and block tags. NeoForge 1.21 uses the `c` namespace shared with Fabric (`c:ingots/iron`). Tag files go to `data/<ns>/tags/item/` (singular directory in 1.21.1) and are generated by `ItemTagsProvider`/`BlockTagsProvider` (NF-SRC `net/neoforged/neoforge/common/Tags.java`; NF-DOCS tags page; DATA jar layout). |
| Ore-dict-driven recipe inputs (`OreDictHelper.contains(...)`) | Tag ingredients in recipe JSON, `Ingredient.of(TagKey)` in code. |
| `CompatInitializerOre` (scans oredict at runtime, writes user-editable JSON to the config dir, feeds bloomery slag and machine recipes) | No 1.21 equivalent exists. The consumers are tech modules that come much later in the porting order. Core ships only the `c:` tags. The fate of the user-editable compat feature is a decision for Moos. |
| `CompatInitializerWood` (same pattern for modded wood, feeds sawmill recipes) | Same as above. |
| `RESTART_REQUIRED` flag and kick-on-join when compat JSON was regenerated | Dropped with the runtime JSON system. Datapacks reload with the world. |

`JsonOreDict`'s references to hunting and tool item base classes disappear with it.
The tags each module needs are declared in that module's own datagen.

## Vanilla recipe surgery

| 1.12 construct | 1.21 replacement |
|---|---|
| `VanillaCraftingRecipesRemove` (casts the recipe registry to modifiable, removes by id from a config list) | Override each recipe file with a stub carrying `{"neoforge:conditions": [{"type": "neoforge:false"}]}`. On 1.21.1 the condition id is `neoforge:false` (the `neoforge:never` rename is a later version). Verified on the 1.21.1 conditions page, https://docs.neoforged.net/docs/1.21.1/resources/server/conditions. The config-driven removal list becomes a fixed set of override files, faithful to the 1.12 defaults. |
| `VanillaFurnaceRecipesRemove` | Same override mechanism, on `data/minecraft/recipe/<name>.json`. The recipe data directory is singular in 1.21.1 (DATA jar). |
| `VanillaFurnaceRecipesAdd` (`FurnaceRecipes.instance().addSmeltingRecipe`) | Plain smelting recipes from the datagen `RecipeProvider`. |
| `LootTableLoadEventHandler` (reflection into loot pools, swaps iron ingots for iron ore) | A NeoForge global loot modifier. `LootModifier` subclass, codec registered under `NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS`, generated by `GlobalLootModifierProvider` (NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/glm). This matches the map decision that dropt is replaced by loot tables and loot modifiers. `LootTableLoadEvent` still exists but the reflection hack is not needed. |
| `PopulateChunkEventHandler` (scans every new chunk, removes vanilla crafting tables, replaces furnaces in villages) | No equivalent event scan. 1.21 worldgen is data-driven. The honest options are datapack structure processor edits or dropping the feature. Flagged for Moos. |

## Event handlers

All names verified against NF-SRC.

| 1.12 handler | 1.21 replacement |
|---|---|
| `FurnaceFuelBurnTimeEventHandler` | Data map, see above. The event name survives if ever needed. |
| `EntityLivingDropsEventHandler` (sheep wool removal, then calls hunting's handler) | `LivingDropsEvent` still exists. The wool removal can also be a global loot modifier on the sheep table. The hard-coded call into hunting is deleted. Hunting registers its own `LivingDropsEvent` listener with a later priority. |
| `HarvestDropsEventHandler.Sticks` (sticks from leaves) | Vanilla leaves already drop sticks since 1.14. `HarvestDropsEvent` is gone. Drop the handler. If 1.12 rates must be matched exactly, adjust with a loot modifier. |
| `EntityItemPickupEventHandler` (fires the pickup advancement trigger) | `EntityItemPickupEvent` is gone. Use `ItemEntityPickupEvent.Post` (NF-SRC `net/neoforged/neoforge/event/entity/player/ItemEntityPickupEvent.java`). |
| `LivingEntityUseShearsEventHandler` + `IBlockShearable` | Keep the shape: a `PlayerInteractEvent.RightClickBlock` listener, or better, move the behavior into each block's `useItemOn`. NeoForge's `IShearable` interface exists for entities and blocks (NF-SRC `net/neoforged/neoforge/common/IShearable.java`); implement it where it fits. |
| `NoHungerEventHandler` (hides the hunger bar client side) | Cancel `RenderGuiLayerEvent.Pre` when the layer is `VanillaGuiLayers.FOOD_LEVEL` (NF-SRC `net/neoforged/neoforge/client/event/RenderGuiLayerEvent.java`, `client/gui/VanillaGuiLayers.java`). |
| `StrawBedEventHandler`, `PlayerSleepInStrawBedEvent` | Custom events still extend `Event` and post on `NeoForge.EVENT_BUS`. The bed hooks change: 1.21.1 has `CanPlayerSleepEvent` and `CanContinueSleepingEvent`; the 1.12-era sleep events are gone (NF-SRC `net/neoforged/neoforge/event/entity/player/CanPlayerSleepEvent.java`). |
| `TooltipEventHandler.BurnTime` | `ItemTooltipEvent`, reading burn time from the data map through the item stack. |
| `PlayerLoggedInEventHandler` | Dropped. Both packets it sent are dropped. |
| `PlayerMovementTracker` (`PlayerTickEvent`) | `PlayerTickEvent.Post`. Stays in core; tech/basic's campfire resting effect reads it. |
| `NeighborNotifyEventHandler` (fire ignites adjacent ignitables) | `BlockEvent.NeighborNotifyEvent`, unchanged in spirit. Stays in core with the ignitable interfaces. |
| `ConfigChangedEventHandler` | `ModConfigEvent.Reloading` on the mod bus. |

## Custom crafting recipes and their new homes

The 1.12 `recipe/` package holds special `IRecipe` classes registered through `_factories.json`.
The 1.21 equivalents are `CustomRecipe` subclasses with a `SimpleCraftingRecipeSerializer`, or a
full `Recipe<CraftingInput>` with a `MapCodec` and `StreamCodec` when the recipe carries data.
Serializers register under `Registries.RECIPE_SERIALIZER`
(NF-SRC `net/minecraft/world/item/crafting/CustomRecipe.java`, `RecipeSerializer.java`).
Recipes use `RecipeInput`/`CraftingInput` now, not `Container`.

Two simplifications shrink this package first:

- Recipes whose only trick is "damage the tool in the grid" (hammer recipes,
  `HuntersKnifeShapelessRecipe`, the pelt scraping recipe's knife handling) stop being custom.
  The stack-sensitive `getCraftingRemainingItem(ItemStack)` override on the tool item does it,
  and the recipes become plain datagen JSON.
- Recipes with random or percentage outcomes (tool repair percentage, shearing drop counts)
  stay custom classes.

Rehoming, per issue #4 and this ticket:

| 1.12 file in core | New home |
|---|---|
| `ScrapingPeltRecipe`, `ShearingPeltRecipe`, `ShearingPeltLlamaRecipe`, `LeatherRepairRecipe`, `HuntersKnifeShapelessRecipe` | hunting |
| `BoneToolRepairRecipe`, `FlintToolRepairRecipe` | tool |
| `LeatherArmorDurableUpgradeRecipe`, `LeatherArmorFireProtectionRecipe` | hunting (they consume hunting's durable leather chain) |
| `TankFlushRecipe` | storage |
| `ChoppingBlockRecipe`, `CraftingTableRecipe`, `MarshmallowStickRecipe` | core (they only touch core items) |
| `EntityLivingDropsEventHandler`'s hunting branch | hunting |
| `BlockInitializer`'s `TileCarcass` registration | hunting |
| `FurnaceFuelBurnTimeEventHandler`'s campfire and worktable entries | tech/basic (data map `remove` entries) |
| `ModuleCore.Materials.REDSTONE`, `.QUARTZ` tool materials | tool (as `Tier` constants) |
| `CompatInitializerOre`/`Wood` tech references | die with the compat system, or move to the tech tickets if rebuilt |

After these moves, core references no other module. That matches the target dependency
direction from DEPS: everything depends on core, core depends on nothing.

## Capability

`IAirflowConsumerCapability` registers through the old `CapabilityManager`. Its only consumers
are tech/machine (bellows, stone combustion workers) and tech/bloomery. The 1.21 capability
system (`BlockCapability`, `RegisterCapabilitiesEvent`) exists for cross-mod queries
(NF-DOCS https://docs.neoforged.net/docs/1.21.1/inventories/capabilities). For a mod-internal
contract, a plain Java interface on the block entity is simpler and faster.
Recommendation: plain interface in core. Flagged below because it changes what other mods can see.

## Dropped outright

- `plugin/` (CraftTweaker, TOP, Waila, JEI): integrations are out of scope for the port.
  JEI returns later as its own concern.
- `command/ClientCommandExport`, `ClientCommandLang`, the `BlockRenderer`/`packer` texture
  tooling: 1.12-era dev asset tooling. Datagen and modern tooling replace the need.
  If a client command is ever wanted again, `RegisterClientCommandsEvent` is the hook.
- `Injector` reflection tricks, `EnumHelper`, `ObfuscationReflectionHelper` loot hacks:
  all replaced by the systems above.

## Known asset gap

The migrated assets on `main` carry textures for the material variants
(`textures/item/twine.png` and friends) but no item models for them
(166 item models exist; none cover the 55 material items). The core port must generate flat
item models, one per flattened item. `ModelProvider` datagen or a scripted one-off both work.

## Decisions for Moos

1. **Igniter contract shape.** Options: item tag `#pyrotech:igniters` only, a core `IgniterItem`
   interface only, or both. Recommendation: both. The tag answers "is this an igniter" in blocks,
   recipes, and datapacks. The interface carries the shared use behavior ignition items need.
2. **Fuel burn times.** Bake the 1.12 defaults into the `furnace_fuels` data map (faithful,
   simple, datapack-tunable), or keep them in the mod config with an event handler reading it.
   Recommendation: data map with 1.12 defaults. Users tune via datapack.
3. **Config surface.** The 1.12 core config is 1,141 lines. Which categories survive as a
   NeoForge `ModConfigSpec`? Recommendation: one common config with the gameplay tweak toggles
   (wool drop removal, iron ingot loot swap, shovel for wood chips, sticks from leaves).
   Drop the pure value-tuning categories until someone asks for them.
4. **Village tweaks.** The 1.12 chunk scan that removes crafting tables and replaces furnaces in
   generated structures has no clean 1.21 equivalent. Options: datapack structure processor
   edits, a follow-up ticket, or dropping the feature. Recommendation: follow-up ticket;
   core ships without it. It guards progression, so it should not be forgotten.
5. **Ore and wood compat system.** The user-editable JSON files in the config folder are a 1.12
   pattern with no 1.21 counterpart. Recommendation: drop the mechanism now, rely on `c:` tags,
   and revisit real cross-mod ore compat in the tech/bloomery and tech/machine tickets.
6. **Root advancement trigger.** Port the custom `pickup_mod_item` trigger (small, exact), or
   switch the root advancement to vanilla `inventory_changed` with a tag of mod items.
   Recommendation: port the trigger; the tag variant needs a maintained all-items tag.
7. **Airflow capability.** Plain core interface (recommended) or a NeoForge `BlockCapability`.
   The capability only matters if other mods should push airflow into pyrotech machines.
