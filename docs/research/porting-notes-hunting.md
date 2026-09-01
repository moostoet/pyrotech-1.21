# Porting notes: hunting

Resolves issue #15 (porting notes: hunting).
Hunting is a hoisting unit from issue #4 (module porting order): 66 files, 5,346 lines.
It holds the mud entity, the carcass and butcher's block, pelts and hides, the knives,
the spears and arrows, the leather kits, and the tannin fluid.
Seven pelt and leather recipe classes rehome here from core.
This document lists each 1.12 construct and its 1.21 replacement.
Decisions that need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo, package `com.codetaylor.mc.pyrotech.modules.hunting`,
  plus the seven recipe classes in `modules/core/recipe/`.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths cited below are entries in that jar.
- **NF-DOCS**: docs.neoforged.net, 1.21.1 version pages. URLs cited inline.
- **DATA**: the vanilla 1.21.1 data pack, at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`.
- **CORE**: `docs/research/porting-notes-core.md` on branch `research/porting-notes-core`.
- **RECIPES**: `docs/research/recipe-architecture.md` on branch `research/recipe-architecture`.

This document assumes the recommended answers from CORE's "Decisions for Moos" section.

## Summary

- The mud entity stays a `Slime` subclass. NeoForge 21.1 still ships the
  `spawnCustomParticles()` hook the mud placement code needs. The custom particle packet dies.
- The spear capability and its three packets collapse into one synced NeoForge data attachment.
  The client join-world reflection hack dies with them.
- The carcass tile keeps its shape as a `BlockEntity`. The item-carries-inventory NBT dance
  becomes the vanilla `minecraft:container` data component plus a loot-table copy function,
  the shulker box pattern.
- Tannin follows the core fluid recipe: one `FluidType`, a `BaseFlowingFluid` pair, a
  `LiquidBlock`. The universal bucket needs an explicit replacement.
- The rehomed pelt recipes split three ways: some become plain datagen JSON, some keep small
  custom recipe classes, and the enchanting ones rewrite against data components.
- The 1.12 string-map configs (drop map, knife efficiency, butcher transforms) have a modern
  data-driven replacement in custom data maps. Whether to use it is flagged for Moos.

## Module bootstrap

The athenaeum `ModuleBase` lifecycle, `enableAutoRegistry`, the `@GameRegistry.ObjectHolder`
holder classes, and the per-module packet service all follow the CORE replacements: one
`register(IEventBus)` hook, `DeferredRegister` fields as the holders, and payloads registered
on core's single registrar. The CraftTweaker, JEI, TOP, and Waila plugin packages (17 files)
are dropped, same as core. The gamestages `Stages` hooks are dropped with athenaeum.

## Items

`ItemPelt` (26 pelts), `ItemHide` (7 hides), `ItemLeatherSheet`, cord, strap, and the durable
variants are plain items with no behavior. They register through `DeferredRegister.Items`
with default `Item.Properties`. The flat 1.12 names (`pelt_cow`, `hide_scraped`, and so on)
are already the registry ids, so lang keys and the migrated item models line up.

| 1.12 construct | 1.21 replacement |
|---|---|
| `ItemHuntersKnife`, `ItemButchersKnife` extend `ItemSword` with per-material `ToolMaterial` and a config durability map | `SwordItem(Tier, Properties)` with `SwordItem.createAttributes(tier, damage, speed)` (NF-SRC `net/minecraft/world/item/SwordItem.java`, lines 20 to 42). The halved attack damage override becomes a halved value in `createAttributes`. Durability bakes into the tier or `Properties.durability(...)`. |
| Bone, flint, stone, obsidian knife materials | `SimpleTier` constants. Tool needs the same materials for its tools. Put one shared holder of `Tier` constants in core so tool and hunting both reach it. |
| `ItemLeatherRepairKit` and friends with durability | Plain items with `Properties.durability(...)`. |
| Config `ALLOW_HUNTERS_KNIFE_REPAIR` gating anvil repair | `isRepairable`/repair-item logic on the item. Survival depends on the config decision below. |
| `ItemBoneArrow`, `ItemFlintArrow` extend `ItemArrow` | `ArrowItem` subclass overriding `createArrow(Level, ItemStack, LivingEntity, @Nullable ItemStack weapon)` (NF-SRC `net/minecraft/world/item/ArrowItem.java`, line 17). `isInfinite` is still a NeoForge extension on the item. |
| Mud spawn egg via `EntityEntryBuilder.egg(...)` | A `DeferredSpawnEggItem` registered as its own item, `pyrotech:mud_spawn_egg`, with the 1.12 colors (NF-SRC `net/neoforged/neoforge/common/DeferredSpawnEggItem.java`, line 32). |

## The mud entity

### Registration

`EntityInitializer.onRegister` and `EntityRegistry.addSpawn` are replaced by four pieces:

1. **Type**: `EntityType.Builder.of(EntityMud::new, MobCategory.MONSTER)` with `sized`,
   `clientTrackingRange`, and `updateInterval`, registered under `Registries.ENTITY_TYPE`
   (CORE, entities section). Copy vanilla slime's builder values. The 1.12 id
   `pyrotech.mud` normalizes to `pyrotech:mud`. The other entity ids normalize the same way:
   `spear`, `flint_arrow`, `bone_arrow`, `hide_scraped_item`.
2. **Attributes**: `EntityAttributeCreationEvent` on the mod bus, with
   `Monster.createMonsterAttributes()`. `Slime.setSize` overwrites max health, speed, and
   attack damage per size at spawn time (NF-SRC `net/minecraft/world/entity/monster/Slime.java`,
   lines 87 to 100), so the supplier only needs the baseline.
3. **Spawn placement**: `RegisterSpawnPlacementsEvent` on the mod bus
   (NF-SRC `net/neoforged/neoforge/event/entity/RegisterSpawnPlacementsEvent.java`, line 42).
   The predicate choice is flagged for Moos below.
4. **Biome spawns**: the config-driven `EntityRegistry.addSpawn` call becomes a
   `neoforge:add_spawns` biome modifier JSON at
   `data/pyrotech/neoforge/biome_modifier/add_mud_spawns.json` with the 1.12 weight 100 and
   count 1 to 3 (NF-SRC `net/neoforged/neoforge/common/world/BiomeModifiers.java`, line 148;
   NF-DOCS https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/).
   Static JSON is fine here; `DatapackBuiltinEntriesProvider` datagen also works.

A spawn-rule nuance to carry over honestly. The 1.12 config listed `swampland` and `river`,
but `EntitySlime.getCanSpawnHere` only passes in swampland (or slime chunks), so the river
entry never spawned anything. The 1.21 equivalent check, `Slime.checkSlimeSpawnRules`, gates on
the `minecraft:allows_surface_slime_spawns` biome tag, which is swamps only (NF-SRC `Slime.java`,
lines 301 to 330). See the decision below.

### Behavior

| 1.12 construct | 1.21 replacement |
|---|---|
| `createInstance()` override so splitting produces muds | Not needed. `Slime.remove` splits via `this.getType().create(this.level())`, so children are already muds (NF-SRC `Slime.java`, lines 202 to 240). |
| `spawnCustomParticles()` as the landing hook | Still exists. NeoForge 21.1 kept the Forge hook (NF-SRC `Slime.java`, lines 146 and 390). Override it, run `spawnMud` server side, and return false. |
| `SCPacketParticleMud` plus `ParticleMud` (a `ParticleBreaking` of the mud rock item) | Dropped. Override `Slime.getParticleType()` to return `new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(rock_mud))`. Vanilla's landing branch then spawns the particles client side with no packet (NF-SRC `Slime.java`, lines 127 and 141 to 152). |
| `setDead()` also placing mud | `remove(RemovalReason)` override, server side, before `super.remove`. |
| `spawnMud` block loop: `BlockHelper.forBlocksInRange`, `Material.GROUND`/`GRASS` checks, `BlockRock.VARIANT` metadata | Plain `BlockPos.betweenClosed` loop. `state.canBeReplaced()` replaces `isReplaceable`; `!state.liquid()` replaces the fluid-block exclusions; `isFaceSturdy(..., Direction.UP)` replaces `isTopSolid`. The material checks become a block test, for example `is(BlockTags.DIRT)`. The rock variant checks become checks against the flat `pyrotech:rock_mud`, `pyrotech:mud`, and `pyrotech:mud_layer` blocks from core. |
| `getDropItem`/`getLootTable` override gating drops to size 1 | Dropped. Vanilla gates slime loot in the loot table itself, with an `entity_properties` condition carrying a `minecraft:slime` size-1 sub-predicate (DATA `data/minecraft/loot_table/entities/slime.json`). The sub-predicate matches any `Slime` subclass. The mud table generates through an `EntityLootSubProvider` at `data/pyrotech/loot_table/entities/mud.json`, dropping 0 to 2 `pyrotech:rock_mud` with the looting bonus, faithful to the 1.12 table. |

### Client

| 1.12 construct | 1.21 replacement |
|---|---|
| `ModelMud` (one 8x8x8 box at y 16) | Exactly `SlimeModel.createOuterBodyLayer()` (NF-SRC `net/minecraft/client/model/SlimeModel.java`). Reuse `SlimeModel`, register a mod `ModelLayerLocation` in `EntityRenderersEvent.RegisterLayerDefinitions` with that mesh. No custom model class. |
| `RenderMud` extends `RenderLiving` with squish scaling | A `MobRenderer<EntityMud, SlimeModel<EntityMud>>` copying `SlimeRenderer` minus the outer layer, with the mud texture (already migrated to `textures/entity/mud/mud.png`). Register in `EntityRenderersEvent.RegisterRenderers`. |

## Spears

### The thrown spear entity

`EntitySpear` extends `EntityArrow`; the 1.21 base is `AbstractArrow`, and vanilla
`ThrownTrident` is the working template (NF-SRC
`net/minecraft/world/entity/projectile/ThrownTrident.java`).

- The constructor takes the pickup stack:
  `AbstractArrow(type, owner, level, pickupItemStack, firedFromWeapon)` (NF-SRC
  `AbstractArrow.java`, lines 112 to 115). That replaces the manual `itemStack` field and its
  NBT read and write.
- The synced `DATA_ITEM_STACK` parameter stays, because the renderer needs the stack client
  side and `pickupItemStack` is not synced. Define it in
  `defineSynchedData(SynchedEntityData.Builder)` with `EntityDataSerializers.ITEM_STACK`.
- `onHit(RayTraceResult)` splits into `onHitEntity(EntityHitResult)` and
  `onHitBlock(BlockHitResult)`.
- The stick-into-entity behavior writes the synced attachment described below instead of the
  capability.
- `RenderSpear` becomes a small `EntityRenderer` that draws the synced stack with the
  `ItemRenderer`, registered in `RegisterRenderers`.

### The spear items

`ItemSpearBase` maps almost one to one onto vanilla `TridentItem` (NF-SRC
`net/minecraft/world/item/TridentItem.java`):

- `getMaxItemUseDuration` becomes `getUseDuration(stack, entity)` returning 72000.
- `EnumAction.BOW` becomes `UseAnim.SPEAR` (or `BOW`, to taste) from `getUseAnimation`.
- `onItemRightClick` becomes `use` calling `player.startUsingItem(hand)`.
- `onPlayerStoppedUsing` becomes `releaseUsing(stack, level, entity, timeLeft)`.
- The Power, Punch, and Flame enchantment reads change to holder lookups through
  `EnchantmentHelper`, or get dropped; spears are not enchantable through survival means.
- The `charging` item property override registers through `ItemProperties.register` inside
  `FMLClientSetupEvent.enqueueWork` (NF-SRC
  `net/minecraft/client/renderer/item/ItemProperties.java`). The migrated spear models on
  `main` already carry the matching `overrides` blocks.
- Per-material config values (durability, velocity, damage, inaccuracy) become constructor
  arguments with the 1.12 defaults, pending the config decision below.

### Spears stuck in entities

The whole sync stack collapses into one NeoForge data attachment:

| 1.12 construct | 1.21 replacement |
|---|---|
| `ISpearEntityData`, `SpearEntityData`, `CapabilitySpear`, `CapabilitySpearProvider`, `CapabilityManager.register` | One `AttachmentType<SpearData>` registered under `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`, where `SpearData` is a small record with a codec. `entity.getData(...)` and `setData(...)` replace the capability lookups. |
| `EntityAttachCapabilitiesEventHandler` | Dropped. Attachments need no attach event. |
| `SCPacketCapabilitySyncSpear`, `CSPacketCapabilitySyncSpearRequest` | Dropped. `AttachmentType.Builder.sync(StreamCodec)` exists in 21.1.249 (NF-SRC `net/neoforged/neoforge/attachment/AttachmentType.java`, lines 250 to 261). Synced attachments push to watching clients on every `setData` and to players that start tracking the entity (NF-SRC `net/neoforged/neoforge/attachment/AttachmentSync.java`, lines 128 and 212). |
| `ClientEntityJoinWorldEventHandler` (reflection into `layerRenderers`, sync request on join) | Dropped. The sync half is automatic. The layer half becomes `EntityRenderersEvent.AddLayers`: iterate the event's entity types, fetch each living renderer with `getRenderer`, and `addLayer(new SpearLayer(...))` (NF-SRC `net/neoforged/neoforge/client/event/EntityRenderersEvent.java`, lines 123 to 178). |
| `LivingDeathEventHandler` dropping stuck spears on death | Unchanged in spirit. `LivingDeathEvent` exists (NF-SRC `net/neoforged/neoforge/event/entity/living/LivingDeathEvent.java`). Read the attachment, spawn the stacks. |

The sync payload only needs the count and seed, same as 1.12; the server keeps the stacks.

## Arrows

`EntityArrowBase` (break on landing, drop stick, shard, and fletching) becomes an
`AbstractArrow` subclass. The landing hook is `onHitBlock(BlockHitResult)` plus the `inGround`
flag (NF-SRC `AbstractArrow.java`, lines 60 and 168 to 213). The material drops reference the
flat core items: `pyrotech:fletching` replaces `ItemMaterial.EnumType.FLETCHING.asStack()`.
The stray 1.12 `System.out.println` in `onHit` does not come along.

The renderers replace the two anonymous `RenderArrow` subclasses with one `ArrowRenderer`
subclass parameterized on texture (NF-SRC
`net/minecraft/client/renderer/entity/ArrowRenderer.java`). Both textures are already migrated.

## The carcass

### Block

`BlockCarcass` extends athenaeum's `BlockPartialBase`. The 1.21 shape:

| 1.12 construct | 1.21 replacement |
|---|---|
| `PropertyDirection FACING` + `getStateFromMeta`/`getMetaFromState` | Extend `HorizontalDirectionalBlock` (or declare `BlockStateProperties.HORIZONTAL_FACING`). Metadata is gone; `createBlockStateDefinition` declares the property, `getStateForPlacement(BlockPlaceContext)` sets it. The migrated blockstate JSON already expects `facing`. |
| `getBoundingBox` with two AABBs | `getShape` returning a `VoxelShape` per facing, built with `Shapes.box`. |
| `Material.GROUND`, `setHardness`, `SoundType.SLIME` | `BlockBehaviour.Properties.of().strength(0.25f).sound(SoundType.SLIME_BLOCK).mapColor(...)` (CORE, blocks section). |
| `hasTileEntity`/`createTileEntity` | Implement `EntityBlock#newBlockEntity`. |
| `onBlockActivated` routing into the interaction framework | `useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)` (NF-SRC `net/minecraft/world/level/block/state/BlockBehaviour.java`, line 226). The interaction framework replacement is below. |
| `removedByPlayer`/`harvestBlock`/`getDrops` keeping the tile alive to serialize the drop | Dropped. Loot tables copy components from the block entity now: the block's loot table uses `CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY)` (NF-SRC `net/minecraft/world/level/storage/loot/functions/CopyComponentsFunction.java`), the shulker box pattern. |

### Block entity

`TileCarcass` extends athenaeum's `TileEntityDataBase` with tile-data network fields.
The 1.21 shape:

- A plain `BlockEntity` registered with `BlockEntityType.Builder.of(...)` (CORE, block
  entities section). NeoForge still ships `ItemStackHandler`
  (`net/neoforged/neoforge/items/ItemStackHandler.java` in NF-SRC), so the single-slot dynamic
  handler carries over.
- NBT moves to `saveAdditional(CompoundTag, HolderLookup.Provider)` and `loadAdditional`
  (NF-SRC `net/minecraft/world/level/block/entity/BlockEntity.java`, lines 77 and 94).
- The athenaeum tile-data sync (`TileDataFloat`, `TileDataLargeItemStackHandler`) is the
  library system whose replacement is its own prototype ticket per issue #4. The carcass does
  not need to wait for it: it syncs two floats and one slot, and `getUpdateTag` plus
  `level.sendBlockUpdated` on change covers that. If the prototype lands first, use it.
- The carcass item carries its contents as the vanilla `minecraft:container` component
  (`ItemContainerContents`, NF-SRC `net/minecraft/world/item/component/ItemContainerContents.java`)
  instead of a `BlockEntityTag`. The block entity implements `collectImplicitComponents` and
  `applyImplicitComponents` (NF-SRC `BlockEntity.java`, lines 293 to 304) so place-and-break
  round-trips the contents. `ItemBlockCarcass`'s static NBT helpers become component reads on
  the stack; max stack size 1 is `Properties.stacksTo(1)`.

### Interaction

The athenaeum interaction framework (`ITileInteractable`, `InteractionUseItemBase`,
`InteractionCarcass`) is a large shared system; tech and storage are its heavy users. Hunting
only needs one interaction: right-click with a knife. The port does not need the framework:

- `useItemOn` on the block checks the hunger gate, the knife check, and calls a
  `chop(player, stack)` method on the block entity.
- The `IInteractionCarcassDelegate` interface stays, in spirit: `TileCarcass` and
  `TileButchersBlock` share the chop loop, so keep a small shared class or interface with the
  same methods, minus the athenaeum types.
- `ALLOWED_KNIVES` (registry-name string lists) becomes item tags: `#pyrotech:hunters_knives`,
  `#pyrotech:butchers_knives`, and a combined `#pyrotech:carcass_knives`. Tag checks are
  `stack.is(tag)`.
- `KNIFE_EFFICIENCY` (item-to-int string map) is flagged for Moos below.
- The no-hunger message packet goes through core's `SCPacketNoHunger` payload helper (CORE,
  network section). That removes hunting's borrow of tech/basic's channel.
- `SCPacketParticleProgress` is dropped for `ServerLevel.sendParticles` (CORE, packets table).
  The client-side crack particles become
  `level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), ...)`.
- Exhaustion is `player.causeFoodExhaustion(amount)`.

### The butcher's block

`TileButchersBlock` and `BlockButchersBlock` follow the carcass pattern exactly: same delegate,
same knife tags, plus one input slot holding a carcass item and a transform map applied on
extraction. The `TESRInteractable` renderer that draws the held carcass becomes a small
`BlockEntityRenderer` registered with `registerBlockEntityRenderer` in `RegisterRenderers`.
The transform map (`ITEM_TRANSFORMS` config strings) is part of the data-map decision below.

## Carcass drops on death

`EntityLivingDropsEventHandler` (called from core's handler in 1.12) becomes hunting's own
`LivingDropsEvent` listener at low priority, per CORE's events table. The logic ports as is:

- Remove leather drops, capture listed drops, roll the per-entity drop table, then spawn one
  carcass item whose `minecraft:container` component holds the captured stacks.
- Sheep color switching uses `Sheep.getColor()` returning `DyeColor` (NF-SRC
  `net/minecraft/world/entity/animal/Sheep.java`); `SILVER` is `LIGHT_GRAY` now. Llama
  variants use `Llama.getVariant()` returning the `Llama.Variant` enum (NF-SRC
  `net/minecraft/world/entity/animal/horse/Llama.java`, line 145).
- The drop-map metadata syntax (`pyrotech:material:11;2;0.50`) dies with metadata. The flat
  ids are `pyrotech:bone_shard` (meta 11), `pyrotech:lard` (meta 49), `pyrotech:fletching`
  (meta 46).
- Where the table lives (config strings, data map, or code) is flagged for Moos below.

A loot modifier could express the leather removal, but the capture-and-repack step needs the
whole drop list at once, so a single event listener is the honest port.

## The scraped hide item entity

The in-world washing loop survives as the same pattern:

| 1.12 construct | 1.21 replacement |
|---|---|
| `hasCustomEntity`/`createEntity` on `ItemHideScraped` | Both still exist as NeoForge item extensions (NF-SRC `net/neoforged/neoforge/common/extensions/IItemExtension.java`, lines 233 and 249). |
| `EntityItemHideScraped` extends `EntityItem` | An `ItemEntity` subclass with its own registered `EntityType` (`MobCategory.MISC`). The renderer is vanilla `ItemEntityRenderer`; its generic is fixed to `ItemEntity`, so the registration needs a cast or a two-line provider. |
| Water detection via `BlockFluidBase`/`FluidRegistry.WATER` | `level.getFluidState(pos).is(FluidTags.WATER)`, or simply `this.isInWater()`. |
| `ticksInWater` mirrored into stack NBT every 200 ticks | Store progress on the entity NBT only, and mirror into a `DataComponents.CUSTOM_DATA` component if pickup-and-redrop should keep progress, matching 1.12. |
| `transformedItem` serialized per entity | Keep, or derive from the item (scraped maps to washed, small scraped to small washed) and skip serializing it. Derivation is simpler and equivalent. |
| `lifespan = Integer.MAX_VALUE` | The `lifespan` field is still a NeoForge extension on `ItemEntity`. |

The shift-tooltip on the scraped hides becomes `appendHoverText` with `Screen.hasShiftDown()`.

## The tannin fluid

`FluidInitializer`, athenaeum's `FluidInitializerRegistry`, and `BlockFluidTannin` follow
CORE's fluid section exactly:

- One `FluidType` (density 1000, viscosity 1000) under `NeoForgeRegistries.Keys.FLUID_TYPES`.
- A `BaseFlowingFluid.Source` and `.Flowing` pair under `Registries.FLUID`.
- A `LiquidBlock` for the in-world form. `BlockFluidTannin` (14 lines, no behavior) dies.
- Client textures bind through `IClientFluidTypeExtensions` registered in
  `RegisterClientExtensionsEvent.registerFluidType` on the mod bus (NF-SRC
  `net/neoforged/neoforge/client/extensions/common/RegisterClientExtensionsEvent.java`).
  The `fluid_tannin_still` and `fluid_tannin_flow` textures are already migrated to
  `textures/block/` on `main`.
- 1.12 registered a Forge universal bucket for tannin
  (`FluidRegistry.addBucketForFluid`, via `library/FluidInitializerRegistry.java`, line 152).
  1.21 has no universal bucket. See the decision below.
- Declare a fluid tag `#pyrotech:tannin` in datagen. The tech/basic soaking pot and barrel
  recipes consume tannin later, and RECIPES' `SizedFluidIngredient` matches on tags.

## Pelt and leather recipes rehomed from core

The seven classes from CORE's rehoming table, all athenaeum-era `IRecipe` factories in 1.12.
The 21 recipe JSONs under `assets/pyrotech/recipes/hunting/` (plus the fireproof and durable
armor files) regenerate through the datagen `RecipeProvider` into `data/pyrotech/recipe/`,
per RECIPES. The oredict inputs become tags declared in hunting's datagen:

| 1.12 oredict | 1.21 tag |
|---|---|
| `toolHuntersKnife` | `#pyrotech:hunters_knives` |
| `toolShears` | `Tags.Items.TOOLS_SHEAR` (`c:tools/shear`) |
| `hideScrapeable` | `#pyrotech:scrapeable_hides` |
| `hideSmallScrapeable` | `#pyrotech:small_scrapeable_hides` |
| `kitRepairLeather` | `#pyrotech:leather_repair_kits` |
| `leatherDurable` | `#pyrotech:durable_leather` |

Per class:

- **`ScrapingPeltRecipe`** and **`HuntersKnifeShapelessRecipe`**. Their only trick is damaging
  the knife by a per-recipe amount (scraping 2, fletching 0, both config values). CORE
  proposed the stack-sensitive `getCraftingRemainingItem(ItemStack)` override on the knife.
  That override cannot vary by recipe: it would damage the knife by one fixed amount in every
  recipe. The refinement is one custom serializer, working name
  `pyrotech:tool_damage_shapeless`, a shapeless recipe with a `tool_damage` field whose
  `getRemainingItems` damages stacks in `#pyrotech:hunters_knives`
  (`Recipe#getRemainingItems` still exists, NF-SRC
  `net/minecraft/world/item/crafting/Recipe.java`, line 33). Item damage is
  `stack.hurtAndBreak`-style component math on `DataComponents.DAMAGE` now. See the decision
  below.
- **`ShearingPeltRecipe`** and **`ShearingPeltLlamaRecipe`**. These have a second trick: the
  pelt slot leaves a sheared hide (or llama hide) behind as its remaining item. A remaining
  item cannot come from the pelt item itself, because sheep pelts are also inputs to the
  scraping recipe, which must consume them fully. So shearing keeps a custom class,
  `pyrotech:shearing_pelt`, with an explicit remainder stack field, one serializer serving
  both the sheep and llama variants.
- **`LeatherRepairRecipe`**. Computes the result from the armor's damage and a repair
  percentage, damages the knife and the kit. Stays a custom recipe class with a
  `repair_percentage` field. Damage reads and writes go through `DataComponents.DAMAGE`.
- **`LeatherArmorDurableUpgradeRecipe`** and **`LeatherArmorFireProtectionRecipe`**. Apply
  Unbreaking or Fire Protection plus a dye color. These rewrite against 1.21 systems:
  enchantments are registry holders now, so `matches`/`assemble` resolve
  `Enchantments.UNBREAKING` through the `HolderLookup.Provider` that `assemble` receives.
  Compatibility is `Enchantment.areCompatible(first, second)` (NF-SRC
  `net/minecraft/world/item/enchantment/Enchantment.java`, line 195). The `display.color` NBT
  write becomes a `DataComponents.DYED_COLOR` component (`DyedItemColor`, NF-SRC
  `net/minecraft/world/item/component/DyedItemColor.java`). Both stay `CustomRecipe`-style
  classes with serializers under `Registries.RECIPE_SERIALIZER`.

The `modules_enabled` conditions in every 1.12 recipe JSON die with the module toggles.

## Network packets

| 1.12 packet | Fate |
|---|---|
| `SCPacketParticleMud` | Dropped. `getParticleType()` covers it with no packet. |
| `SCPacketCapabilitySyncSpear`, `CSPacketCapabilitySyncSpearRequest` | Dropped. Attachment sync covers both directions. |
| `SCPacketNoHunger` (borrowed from tech/basic) | Core-owned payload per CORE. Hunting calls the core helper. |

Hunting registers zero payloads of its own.

## Config

`ModuleHuntingConfig` is 564 lines: spear and arrow tuning, carcass and butcher gates, the
drop maps, knife durability maps, and the soak timer. Following CORE decision 3 (minimal
config, bake 1.12 defaults), most values become constants with the 1.12 defaults. The
string-map entries are covered by the data-map decision below. Anything kept configurable
goes into the one shared `ModConfigSpec` from core.

## Dropped outright

- `plugin/` (CraftTweaker, JEI, TOP, Waila): 17 files, out of scope per CORE.
- The gamestages `Stages` integration on both tiles.
- `PacketInitializer` and all three packet classes.
- The capability package (4 files) and both capability event handlers.
- `BulkRenderItemSupplier` hooks and the module toggle machinery.

## Decisions for Moos

1. **Mud spawn rules.** Options: reuse `Slime::checkSlimeSpawnRules` (faithful to observed
   1.12 behavior; the configured river spawns stay dead, because the slime rule only passes
   in swamps), or write a custom predicate gated on a new `#pyrotech:mud_spawn_biomes` biome
   tag (makes river spawns real, and lets datapacks tune biomes). Recommendation: custom
   predicate with the tag, containing swamp and river; decide in passing whether
   `mangrove_swamp` joins it.
2. **The string-map configs.** The drop map (entity to drops), the drop capture list, the
   knife efficiency maps, and the butcher transform map are user-editable config strings in
   1.12. Options: keep them as `ModConfigSpec` string lists (faithful, clunky), bake them
   into code constants (simplest, not tunable), or define custom NeoForge data maps
   (`DataMapType` on `Registries.ENTITY_TYPE` for drops and on `Registries.ITEM` for knife
   efficiency and transforms; NF-SRC
   `net/neoforged/neoforge/registries/datamaps/DataMapType.java`). Data maps are
   datapack-tunable and match the furnace-fuel decision from CORE. Recommendation: data maps,
   generated by `DataMapProvider` with the 1.12 defaults.
3. **Per-recipe knife damage.** Options: one custom `tool_damage_shapeless` serializer
   (faithful; scraping costs 2 durability, fletching 0), or the flat
   `getCraftingRemainingItem` override on the knives (no custom recipe code; every knife
   recipe costs exactly 1 durability, a small balance change). Recommendation: the custom
   serializer. Hunting needs custom recipe classes for shearing and leather repair anyway,
   so the marginal cost is low. This refines CORE's blanket "the override does it" note.
4. **Tannin bucket.** 1.12 exposed tannin through the Forge universal bucket. Options:
   register a `BucketItem` plus a new bucket texture (faithful capability, small art task),
   or ship no vanilla bucket and rely on the bucket module's clay and stone buckets when that
   module lands. Recommendation: register the `BucketItem`; tanks and JEI expect fluids to
   have one, and the texture is a one-off.
