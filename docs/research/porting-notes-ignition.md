# Porting notes: ignition

Resolves issue #18 (porting notes: ignition).
Ignition is a hoisting unit from issue #4 (module porting order): 30 files, 3,116 lines (DEPS).
It holds the four igniter items (bow drill, durable bow drill with its spindle, flint and tinder,
matchstick), the powered igniter block in stone and brick with its tile, the fiber and stone
torches, the oil lamp with its tile, renderer, and fuel tank, the lamp oil fluid, a 240-line
config, no event handlers, and 7 plugin files. This document lists each 1.12 construct and its
1.21 replacement. Decisions that need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo, package `com.codetaylor.mc.pyrotech.modules.ignition`,
  plus its users outside the package: the four ignite paths that test `instanceof ItemIgniterBase`
  (`modules/tech/basic/block/BlockCampfire.java`, `modules/tech/basic/block/BlockKilnPit.java`,
  `modules/tech/bloomery/block/BlockBloomery.java`,
  `modules/tech/machine/block/spi/BlockCombustionWorkerStoneBase.java`),
  `modules/tech/refractory/util/RefractoryIgnitionHelper.java`,
  `modules/tech/refractory/init/RegistryInitializer.java`,
  `modules/tech/machine/init/recipe/StoneCrucibleRecipesAdd.java`, `library/spi/block/IBlockIgnitable*.java`,
  `library/spi/interaction/InteractionExtinguishable.java`, `library/util/Util.java`,
  `library/FluidInitializerRegistry.java`, `modules/core/ModuleCoreConfig.java`,
  `modules/core/event/NeighborNotifyEventHandler.java`, `modules/core/item/ItemMaterial.java`, the
  8 recipes under `assets/pyrotech/recipes/ignition/`, and the 4 recipes at the `assets/pyrotech/recipes/`
  root that touch this unit (`bow_drill_durable.json`, `bow_drill_durable_stick.json`,
  `bow_drill_durable_repair.json`, `torch_vanilla_coal.json`). Java paths below are relative to
  `src/main/java/com/codetaylor/mc/pyrotech/`.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths cited below are entries in that jar.
- **NF-DOCS**: docs.neoforged.net, 1.21.1 version pages. URLs cited inline.
- **DATA**: the vanilla 1.21.1 data pack and client assets, at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`.
- **CORE**: `docs/research/porting-notes-core.md` on branch `research/porting-notes-core`.
- **STORAGE**: `docs/research/porting-notes-storage.md` on branch `research/porting-notes-storage`.
- **REFRACTORY**: `docs/research/porting-notes-refractory.md` on branch `research/porting-notes-refractory`.
- **RECIPES**: `docs/research/recipe-architecture.md` on branch `research/recipe-architecture`.
- **BUCKET**: `docs/research/porting-notes-bucket.md` on branch `research/porting-notes-bucket`.
- **HUNTING**: `docs/research/porting-notes-hunting.md` on branch `research/porting-notes-hunting`.
- **TOOL**: `docs/research/porting-notes-tool.md` on branch `research/porting-notes-tool`.
- **PROTOTYPE**: branch `prototype/8-campfire-interaction-sync`,
  `docs/prototypes/campfire-interaction-sync.md`.
- **ASSETS**: `docs/asset-migration-report.md` and `docs/asset-conversion-report.md` on `main`.
- **DEPS**: `docs/research/module-dependencies.md` on branch `research/module-dependencies`.

This document assumes the settled answers from CORE, STORAGE, REFRACTORY, and RECIPES, and the
resolution of issue #22 (core sign-off): the `#pyrotech:igniters` item tag plus a core
`IgniterItem` contract, core's `BlockIgniter` hook with its static registry, core-owned
`#pyrotech:refractory`, no athenaeum, no plugin package, PROTOTYPE patterns 1 and 2. Athenaeum's
own sources (`ModuleBase`, `Registry`, `TileEntityBase`, `TileEntityDataBase`, `BlockPartialBase`,
the `Interaction*` classes) are not in the repo; where a claim depends on them it is marked.

## Scope correction

The ticket text says the tile igniter builds on storage's tank base and that one recipe links to
hunting's tannin fluid. The code says otherwise.

- No ignition tile extends storage's `TileTankBase`. `TileLampOil` extends athenaeum's
  `TileEntityDataBase` (`modules/ignition/tile/TileLampOil.java`, lines 36 to 39) and names
  `TileTankBase` only as the type argument of its `InteractionBucket` (line 199). Its `Tank.fill`
  override is a config fuel filter (lines 306 to 313), not a hot-fluid check. STORAGE's "Reusable
  bases" section found this first; this document confirms it. `TileIgniter` extends athenaeum's
  `TileEntityBase` (`tile/TileIgniter.java`, line 16) and holds no tank at all.
- No ignition recipe consumes tannin. The single hunting reference is `BlockFluidTannin::new`,
  passed as the block factory for the lamp oil fluid (`init/FluidInitializer.java`, line 29).
  `BlockFluidLampOil` (`block/fluid/BlockFluidLampOil.java`, lines 7 to 15) is never referenced.
  Both classes are `BlockFluidClassic` with `Material.WATER` and nothing else, so the swap has no
  effect in game. The edge dies with athenaeum's fluid registry.
- `RefractoryIgnitionHelper` stays in refractory (REFRACTORY decision 1). Ignition calls core's
  `PyrotechIgnition.tryIgnite(level, pos)` in the two places it called the helper.
- The unit's recipes are 11, not 8. Three unconditional recipes at the recipes root
  (`bow_drill_durable.json`, `bow_drill_durable_stick.json`, `bow_drill_durable_repair.json`)
  produce ignition items and belong to this unit's datagen. `torch_vanilla_coal.json` is core's
  (it replaces a removed vanilla recipe; see decision 2).
- `tinder` is not an ignition item. `ItemTinder` is registered by tech/basic
  (`modules/tech/basic/init/ItemInitializer.java`, line 16). The `tinder.json` item model on `main`
  belongs to issue #16.

## Summary

- Ignition registers no event handlers, no packets, and no recipe types. It is four items, two
  igniter blocks with one tile, two torch blocks with one tile, one lamp block with one tile and
  one renderer, one fluid, and a config. Everything sits on core.
- The igniter items keep their 1.12 use loop exactly: right-click starts a timed use, a smoke
  particle plays each tick, and `finishUsingItem` tries three things in order: the clicked block's
  `igniteWithIgniterItem`, fire in the adjacent air, then core's ignition hook. Durability, cooldown,
  and the matchstick's self-consumption ride vanilla 1.21 calls.
- The igniter items must not report `ItemAbilities.FIRESTARTER_LIGHT` (decision 1). No vanilla or
  NeoForge code in 21.1.249 asks for that ability, so the only consumer would be refractory's
  right-click listener, which fires at click time and would skip the igniter's use duration.
- The igniter block flattens into `igniter_stone` and `igniter_brick`. Its tile becomes a server
  ticker that reads the redstone signal from the block it faces and ignites the block behind it.
- The torches keep one block per kind with a five-way `facing` and a three-way `type` property,
  which the converted blockstates on `main` already expect (decision 3). Burn-out and dousing
  ride a small block entity with no ticker; the block's random and scheduled ticks drive it.
- The oil lamp is a `SyncedBlockEntity` with a 2000 mB `FluidTank` whose `setValidator` is the
  fuel filter. The per-fluid burn rate becomes a fluid-keyed data map (decision 7). The lamp keeps
  its fluid across break and place through a `SimpleFluidContent` component, the STORAGE tank
  pattern.
- Lamp oil is a placeable fluid in 1.12 and stays one: `FluidType`, `BaseFlowingFluid` pair,
  `LiquidBlock`, `BucketItem`, `IClientFluidTypeExtensions`. Its block id cannot be `lamp_oil`
  because the lamp already owns that id (decision 8).
- Config shrinks from 21 knobs to the four torch behaviour toggles (decision 5).

## Module bootstrap

`ModuleIgnition` follows the CORE replacements: one `register(IEventBus)` hook, `DeferredRegister`
fields as holders, no `ModuleBase`, no `Registry`, no `@GameRegistry.ObjectHolder`, no `Injector`.

| 1.12 construct | 1.21 replacement |
|---|---|
| `setRegistry`, `enableAutoRegistry` (`ModuleIgnition.java`, lines 43 to 44) | `DeferredRegister.createBlocks` and `createItems` (NF-SRC `net/neoforged/neoforge/registries/DeferredRegister.java`, lines 155 and 142), plus `create(Registries.BLOCK_ENTITY_TYPE, ...)`, `create(Registries.FLUID, ...)`, `create(NeoForgeRegistries.Keys.FLUID_TYPES, ...)` (line 113), and `createDataComponents` (line 169), each registered on the mod bus once (line 315). |
| `PACKET_SERVICE`, `TILE_DATA_SERVICE` (lines 46 to 47) | Dropped with athenaeum. The lamp syncs through PROTOTYPE's `SyncedBlockEntity`. Nothing else syncs. |
| `MinecraftForge.EVENT_BUS.register(this)` (line 49) | Dropped. The module class has no `@SubscribeEvent` methods; the call registered nothing. |
| CraftTweaker plugin loop, JEI plugin, TOP and Waila IMC (lines 52 to 66, 74 to 78, 95 to 99) | Dropped. JEI is the shared plugin's concern (assets section). |
| `FluidInitializer`, `BlockInitializer`, `ItemInitializer` (lines 82 to 87, 104 to 109) | The fluid, block, and item sections below. |
| `BlockInitializer.onRegister`: four `registerBlockWithItem`, four tile classes (`init/BlockInitializer.java`, lines 22 to 36) | `DeferredRegister.Blocks#registerBlock` (line 431) for five blocks (`igniter_stone`, `igniter_brick`, `torch_fiber`, `torch_stone`, `lamp_oil`), `Items#registerSimpleBlockItem` (line 546) for five block items. Three `BlockEntityType`s from `BlockEntityType.Builder.of(factory, blocks...).build(null)` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntityType.java`, lines 337 and 341): one over both igniter blocks, one over both torch blocks, one for the lamp. The stone and fiber tiles only return config values (`tile/TileTorchFiber.java` and `TileTorchStone.java`, lines 9 to 31), so the block entity asks its block, as STORAGE and REFRACTORY do. |
| `BlockInitializer.onClientRegister`: item models, variant item models for the igniter, `bindTileEntitySpecialRenderer(TileLampOil.class, new TESRLampOil())` (lines 39 to 58) | Item models load by id. `EntityRenderersEvent.RegisterRenderers#registerBlockEntityRenderer` for the lamp (NF-SRC `net/neoforged/neoforge/client/event/EntityRenderersEvent.java`, line 109; NF-DOCS https://docs.neoforged.net/docs/1.21.1/blockentities/ber). |
| `ItemInitializer.onRegister`: five `registerItem` (`init/ItemInitializer.java`, lines 13 to 20) | `DeferredRegister.Items#registerItem` for five items. |
| `ModuleIgnition.CREATIVE_TAB` | The nine items join core's tab in `BuildCreativeModeTabContentsEvent` (NF-SRC `net/neoforged/neoforge/event/BuildCreativeModeTabContentsEvent.java`, line 26). The spindle is hidden from the tab and from JEI, as in 1.12 (assets section). |
| `Blocks`, `Items`, `Fluids` holder classes with injected nulls (lines 111 to 172) | The `DeferredBlock`, `DeferredItem`, and `DeferredHolder` fields (CORE). |

## Igniter items and the `IgniterItem` contract

`ItemIgniterBase` is the whole mechanic (`item/ItemIgniterBase.java`). Right-click on a block
starts a use (lines 40 to 56). Each tick of the use re-traces; if the trace left the block the use
stops, and on the client a smoke particle spawns at the hit point (lines 60 to 83). When the use
completes, three branches run in order (lines 88 to 141): if the clicked block implements
`IBlockIgnitableWithIgniterItem`, call it and play the flint sound (lines 107 to 114); else if fire
can be set in the block on the clicked side, place `Blocks.FIRE` (lines 116 to 123); else call
`RefractoryIgnitionHelper.igniteBlocks` on the clicked block, guarded by the refractory module
toggle (lines 125 to 136). Every branch damages the item and the item goes on cooldown (line 138).
"Fire can be set" is `Util.canSetFire`: not a liquid, and air or replaceable
(`library/util/Util.java`, lines 69 to 84).

The contract from issue #22 has two halves. `#pyrotech:igniters` answers "is this an igniter"
everywhere a block or recipe asks; ignition's `ItemTagsProvider` adds the bow drill, the durable
spindle, flint and tinder, and the matchstick to it (`ItemTags.create`, NF-SRC
`net/minecraft/tags/ItemTags.java`, line 163; `IntrinsicHolderTagsProvider#tag`,
`net/minecraft/data/tags/IntrinsicHolderTagsProvider.java`, line 64). The bare durable bow drill
body is not an igniter (it has no use behaviour, `item/ItemBowDrillDurable.java`, lines 5 to 14)
and stays out of the tag. `IgniterItem` carries the use behaviour. One Java fact shapes it: a
class method always wins over an interface default, so an interface cannot override `Item#use`,
`onUseTick`, `finishUsingItem`, `getUseAnimation`, or `getUseDuration`. The shape that keeps the
word "interface": `IgniterItem` declares three hooks (`igniterUseDuration(ItemStack)`,
`igniterCooldownTicks()`, `damageIgniter(ItemStack, LivingEntity)`), and a core final class
`IgniterUse` holds static `use`, `tick`, and `finish` that each item's five overrides delegate
to in one line. The alternative is an abstract `IgniterItem extends Item` in core, the 1.12 shape
rehomed. Either satisfies issue #22; this document assumes the interface plus helper.

| 1.12 construct | 1.21 replacement |
|---|---|
| `getItemUseAction` returning `BOW` (lines 32 to 35) | `Item#getUseAnimation` (NF-SRC `net/minecraft/world/item/Item.java`, line 317) returning `UseAnim.BOW` (`net/minecraft/world/item/UseAnim.java`, line 8). |
| `onItemRightClick`: `rayTrace(world, player, false)`, block hit starts the use, else `FAIL` (lines 40 to 56) | `Item#use(Level, Player, InteractionHand)` (line 165) with `getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE)` (line 359; `net/minecraft/world/level/ClipContext.java`, line 70). On `HitResult.Type.BLOCK` (`net/minecraft/world/phys/HitResult.java`, line 25) return `ItemUtils.startUsingInstantly(level, player, hand)` (`net/minecraft/world/item/ItemUtils.java`, lines 10 to 13), else `InteractionResultHolder.fail(stack)` (`net/minecraft/world/InteractionResultHolder.java`, line 32). |
| `onUsingTick`: re-trace, `stopActiveHand` when the trace leaves the block, client smoke at `hitVec` (lines 60 to 83) | `Item#onUseTick(Level, LivingEntity, ItemStack, int)` (line 126). `LivingEntity#releaseUsingItem` (NF-SRC `net/minecraft/world/entity/LivingEntity.java`, line 3303) is the equivalent of `stopActiveHand`: it calls `releaseUsing` then stops. Particle: `level.addParticle(ParticleTypes.SMOKE, hit.getLocation().x, .y, .z, 0, 0, 0)` (`net/minecraft/world/level/Level.java`, line 530; `net/minecraft/core/particles/ParticleTypes.java`, line 84; `net/minecraft/world/phys/HitResult.java`, line 21) when `level.isClientSide` (line 113). |
| `getMaxItemUseDuration` per item | `Item#getUseDuration(ItemStack, LivingEntity)` (line 321). |
| `onItemUseFinish` branch 1: `IBlockIgnitableWithIgniterItem.igniteWithIgniterItem(world, pos, state, facing)` server side, flint sound, damage (lines 107 to 114) | `Item#finishUsingItem(ItemStack, Level, LivingEntity)` (line 183). Same `instanceof` on core's interface with `(Level, BlockPos, BlockState, Direction)`, `hit.getDirection()` (`net/minecraft/world/phys/BlockHitResult.java`, line 46). Sound: `level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F)` (Level.java, line 441; `net/minecraft/sounds/SoundEvents.java`, line 530). Athenaeum's `SoundHelper.playSoundServer` pitch is not in the repo. |
| Branch 2: `Util.canSetFire(world, offset)` then `setBlockState(offset, Blocks.FIRE, 3)` (lines 116 to 123) | Keep the 1.12 test: `state.liquid()` false and `state.isAir() \|\| state.canBeReplaced()` (NF-SRC `net/minecraft/world/level/block/state/BlockBehaviour.java`, lines 629, 618, 869), then `level.setBlock(offset, BaseFireBlock.getState(level, offset), 3)` (Level.java, line 228; `net/minecraft/world/level/block/BaseFireBlock.java`, line 41) so soul fire appears over soul soil. Vanilla `FlintAndSteelItem#useOn` uses `BaseFireBlock.canBePlacedAt` (`net/minecraft/world/item/FlintAndSteelItem.java`, line 38; `BaseFireBlock.java`, lines 179 to 181), which requires air. The 1.12 test also burns tall grass and snow layers, which the vanilla test refuses; keep the 1.12 test for faithful gameplay. The liquid guard matters because `canBeReplaced` is true for water. |
| Branch 3: `RefractoryIgnitionHelper.igniteBlocks(world, pos)` behind `isModuleEnabled(ModuleTechRefractory.class)`, sound, damage, all server side (lines 125 to 136) | `PyrotechIgnition.tryIgnite(level, pos)` (REFRACTORY decision 1). The toggle drops. |
| `getCooldownTracker().setCooldown(this, ticks)` (line 138) | `player.getCooldowns().addCooldown(this, ticks)` (NF-SRC `net/minecraft/world/entity/player/Player.java`, line 2120; `net/minecraft/world/item/ItemCooldowns.java`, line 43). |
| `damageItem`: `stack.damageItem(1, player)` unless creative (`item/ItemBowDrill.java`, lines 41 to 47; `ItemFlintAndTinder.java`, lines 40 to 46) | `stack.hurtAndBreak(1, entity, LivingEntity.getSlotForHand(entity.getUsedItemHand()))` (NF-SRC `net/minecraft/world/item/ItemStack.java`, line 490; LivingEntity.java, lines 3594 and 3133). The creative skip is inside: `hasInfiniteMaterials` at ItemStack.java line 467. The call is a no-op on the client (lines 490 to 497), which matches the 1.12 server-side damage in branch 3 and is harmless in the other two. |
| Matchstick `damageItem`: `stack.shrink(1)` unless creative (`item/ItemMatchstick.java`, lines 33 to 39) | `stack.consume(1, entity)` (ItemStack.java, lines 1089 to 1093), which carries the same creative test. |
| `setMaxDamage(32)`, `setMaxStackSize(1)` (ItemBowDrill.java, lines 24 to 25); flint and tinder 8 (`ItemFlintAndTinder.java`, line 23); spindle 48 (`ItemBowDrillDurableSpindle.java`, line 28); matchstick stack 64 (`ItemMatchstick.java`, line 17) | `Item.Properties().durability(n)` (Item.java, line 456), which writes `MAX_DAMAGE`, `DAMAGE`, and `MAX_STACK_SIZE` 1 (lines 457 to 459; `net/minecraft/core/component/DataComponents.java`, lines 58 to 64). Matchstick: `stacksTo(64)` (line 452). The numbers leave config (config section). |
| `addInformation`: "Durability: %d" when `SHOW_DURABILITY_TOOLTIPS` and undamaged (ItemBowDrill.java, lines 51 to 57; `ModuleCoreConfig.java`, line 848) | `Item#appendHoverText` (line 332) with `Component.translatable("gui.pyrotech.tooltip.durability.full", stack.getMaxDamage())` (NF-SRC `net/minecraft/network/chat/Component.java`, line 159) when `getDamageValue() == 0` (ItemStack.java, line 448). TOOL decision 4 drops the config gate; the key is on `main` (`en_us.json`, line 399). |

Use durations and cooldowns from `ModuleIgnitionConfig` (`ModuleIgnitionConfig.java`): bow drill
20 and 20 (lines 167 and 174), durable bow drill 20 and 20 (lines 188 and 195), flint and tinder
80 and 20 (lines 209 and 216), matchstick 5 and 40 (lines 230 and 237). They become constructor
arguments.

How the block side dispatches. In 1.12 each ignitable block's `onBlockActivated` returned `false`
when the held item was an `ItemIgniterBase`, so vanilla fell through to the item's own use
(`BlockCampfire.java`, lines 274 to 280; `BlockKilnPit.java`, lines 260 to 270; `BlockBloomery.java`,
lines 205 to 226; `BlockCombustionWorkerStoneBase.java`, lines 265 to 286; `BlockTorchBase.java`,
lines 218 to 227; `BlockLampOil.java`, lines 144 to 163). In 1.21 the server calls the block's
`useItemOn`, then `useWithoutItem`, then the item's `useOn`, and only then the client sends the
use-item packet that reaches `Item#use` (NF-SRC `net/minecraft/server/level/ServerPlayerGameMode.java`,
lines 340 to 389, and line 306; `net/minecraft/server/network/ServerGamePacketListenerImpl.java`,
line 1182). So each of those blocks returns `ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION`
from `useItemOn` when `stack.is(PyrotechTags.IGNITERS)` (NF-SRC `net/minecraft/world/ItemInteractionResult.java`,
line 8; its `result()` is `PASS`, line 24; `ItemStack#is(TagKey)`, line 339). That skips
`useWithoutItem` and lets `Item#use` run, the 1.12 order. The pit kiln also returned `false` for
vanilla flint and steel and fire charges (`BlockKilnPit.java`, lines 266 to 268); that half becomes
`stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT)` (NF-SRC
`net/neoforged/neoforge/common/extensions/IItemStackExtension.java`, line 122; `net/neoforged/neoforge/common/ItemAbilities.java`,
line 144), which is the one legitimate block-side use of that ability: it lets vanilla
`FlintAndSteelItem#useOn` place fire on top of the kiln (FlintAndSteelItem.java, lines 37 to 41),
and core's fire-adjacency listener turns that into `igniteWithAdjacentFire` (`BlockKilnPit.java`,
lines 207 to 219; `modules/core/event/NeighborNotifyEventHandler.java`, lines 19 to 45).

Why the igniters must not report `FIRESTARTER_LIGHT` (decision 1). Every consumer of the ability
in NF-SRC is a producer passing it into `getToolModifiedState`: `FlintAndSteelItem#useOn` (line 35),
`FireChargeItem#useOn` (`net/minecraft/world/item/FireChargeItem.java`, line 39), and the
dispenser behaviour (`net/minecraft/core/dispenser/DispenseItemBehavior.java`, line 282). The
receiving end, `IBlockExtension#getToolModifiedState` (`net/neoforged/neoforge/common/extensions/IBlockExtension.java`,
lines 778 and 817 to 820), lights vanilla campfires, candles, and candle cakes. Nothing calls
`canPerformAction(FIRESTARTER_LIGHT)`: TNT tests the two items by identity
(`net/minecraft/world/level/block/TntBlock.java`, line 108) and creepers test
`#minecraft:creeper_igniters` (`net/minecraft/world/entity/monster/Creeper.java`, line 227; DATA
`data/minecraft/tags/item/creeper_igniters.json`). So an igniter reporting the ability changes
nothing in vanilla. Its only reader would be refractory's `RightClickBlock` listener (REFRACTORY,
"The ignition helper rehoming"), which runs at click time (NF-SRC
`net/neoforged/neoforge/event/entity/player/PlayerInteractEvent.java`, line 163). For a bow drill
that means the log pile lights the instant it is clicked, before the 20-tick use, with no cooldown
and no durability; then `setUseItem(TriState.TRUE)` (line 211) lets the item's use run anyway and
ignite an already-active pile. In 1.12 the igniter reached the pit burn only through branch 3, after
its use duration. The igniters keep that path and stay off the ability.

## The bow drill and the durable bow drill

Three items and one core material. The bow drill (`bow_drill`) is a 32-durability igniter
(`item/ItemBowDrill.java`). The durable bow drill (`bow_drill_durable`) is a plain body with no use
behaviour and stack size 1 (`item/ItemBowDrillDurable.java`, lines 5 to 14). Fitting a durable
spindle (core material `bow_drill_durable_stick`, `modules/core/item/ItemMaterial.java`, line 123)
to the body gives the spindle item (`bow_drill_durable_spindle`), the 48-durability igniter
(`item/ItemBowDrillDurableSpindle.java`). When the spindle wears out the item turns back into the
body (lines 55 to 69): after the base use, if the stack is empty or its damage equals its max,
play `ENTITY_ITEM_BREAK` and return a fresh `BOW_DRILL_DURABLE`. `damageItem` refuses to damage a
stack already at max (lines 78 to 88), which keeps the swap from ever going past the end.

| 1.12 construct | 1.21 replacement |
|---|---|
| The swap in `onItemUseFinish` plus the max-damage guard in `damageItem` (lines 55 to 69, 78 to 88) | `stack.hurtAndConvertOnBreak(1, BOW_DRILL_DURABLE, entity, slot)` (NF-SRC `ItemStack.java`, line 501). It damages, and when the stack empties it returns a fresh copy of the given item. `hurtAndBreak` breaks at `damage >= maxDamage` (lines 479 to 484), so the swap lands on the same use as in 1.12 (use 48 of 48). The break sound and particles come from `onEquippedItemBroken` (LivingEntity.java, line 3590), replacing the explicit `ENTITY_ITEM_BREAK`. `finishUsingItem` returns the converted stack and `completeUsingItem` puts it in hand (LivingEntity.java, lines 3271 to 3288). |
| `getUnlocalizedName` returning `item.pyrotech.bow.drill.durable` so the spindle item shows "Durable Bow Drill" (lines 40 to 50; 1.12 `lang/en_us.lang`, line 115) | `Item#getDescriptionId()` override (Item.java, line 272) returning `item.pyrotech.bow_drill_durable`, the key `main` already has (`en_us.json`, line 83). `main` has no `bow_drill_durable_spindle` key. |
| `ItemBowDrillDurable`: `setMaxStackSize(1)` | `new Item(new Item.Properties().stacksTo(1))`. No class. |
| JEI blacklist of the spindle (`plugin/jei/PluginJEI.java`, lines 17 to 18) | The shared JEI plugin hides `bow_drill_durable_spindle`. The JEI API is not among this document's sources; the shape is not verified here. |
| Item models `bow_drill.json`, `bow_drill_durable.json` (texture `bow_drill_durable_empty`), `bow_drill_durable_spindle.json` (texture `bow_drill_durable`) | Already on `main` under `models/item/`, `item/handheld` parents. Nothing to do. |

The two body recipes and the spindle recipe live at the 1.12 recipes root and are covered in the
recipes section. The body has no durability, so the "repair" recipe (`bow_drill_durable_repair.json`)
is a plain shapeless craft: body plus spindle stick gives a spindle item at full durability.

## The powered igniter block and tile

`BlockIgniter` is a stone block (`Material.ROCK`, pickaxe 0, hardness 2) with a `variant` of stone
or brick and a horizontal facing (`block/BlockIgniter.java`, lines 37 to 54; the comment at line
162 explains that facing takes two meta bits so at most four variants fit). Placement faces the
player (lines 122 to 136). `neighborChanged` asks whether the block on the facing side powers the
igniter and stores the answer on the tile (lines 89 to 105). The tile ticks while powered
(`tile/TileIgniter.java`, lines 27 to 51): the target is the block on the opposite side; if fire can
be set there, place fire; else if that block is `IBlockIgnitableAdjacentIgniterBlock`, call it; else
call `RefractoryIgnitionHelper.igniteBlocks` on it, with no module guard this time. So a powered
igniter re-places fire every tick and keeps a pit kiln, campfire, bloomery, combustion worker, or
tar collector lit as long as the signal holds. `isPowered` is never written to NBT (lines 19 to 24),
so after a chunk reload the igniter is off until a neighbour changes.

| 1.12 construct | 1.21 replacement |
|---|---|
| `Material.ROCK`, pickaxe 0, hardness 2 (lines 41 to 43) | `BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f)` (NF-SRC `BlockBehaviour.java`, lines 1149 and 1203; `net/minecraft/world/level/material/MapColor.java`, line 18), tag `minecraft:mineable/pickaxe` (`net/minecraft/tags/BlockTags.java`, line 141). |
| `VARIANT` stone and brick, meta packing, `damageDropped`, `getSubBlocks`, `getModelName` (lines 37, 58 to 86, 140 to 143, 159 to 201) | Two blocks, `igniter_stone` and `igniter_brick`, one class. The lang keys exist (`en_us.json`, lines 86 to 87). The converted `blockstates/igniter.json` keys on `variant=` and must split (assets section). |
| `Properties.FACING_HORIZONTAL`, placement `placer.getHorizontalFacing().getOpposite()` (lines 46, 134 to 135) | `BlockStateProperties.HORIZONTAL_FACING` (NF-SRC `net/minecraft/world/level/block/state/properties/BlockStateProperties.java`, line 54); `getStateForPlacement(BlockPlaceContext)` (NF-SRC `net/minecraft/world/level/block/Block.java`, line 398) with `context.getHorizontalDirection().getOpposite()` (`net/minecraft/world/item/context/UseOnContext.java`, line 70; `net/minecraft/core/Direction.java`, line 169). |
| `neighborChanged`: `world.isSidePowered(pos.offset(facing), facing.getOpposite())` stored on the tile (lines 89 to 105) | `level.hasSignal(pos.relative(facing), facing.getOpposite())` (NF-SRC `net/minecraft/world/level/SignalGetter.java`, line 91; `getSignal` at lines 104 to 108 is the same weak-or-strong test as 1.12 `getRedstonePower`; `net/minecraft/core/BlockPos.java`, line 215). Where the answer lives is decision 4; the recommendation reads it in the ticker every tick, which also fixes the stale-after-reload state. `neighborChanged` is `BlockBehaviour.java`, line 186. |
| `hasTileEntity`, `createTileEntity` (lines 108 to 118); `ITickable.update` (`TileIgniter.java`, line 27) | `EntityBlock#newBlockEntity` and `getTicker` (NF-SRC `net/minecraft/world/level/block/EntityBlock.java`, lines 15 and 18), a `BlockEntityTicker` (`net/minecraft/world/level/block/entity/BlockEntityTicker.java`, line 9) returned only when `!level.isClientSide`. One `BlockEntityType` over both blocks. The tile saves nothing, so no `saveAdditional` and no sync. |
| `update` branch 1: `Util.canSetFire` then `Blocks.FIRE` (lines 42 to 43) | The same replaceable-and-not-liquid test as the item, then `level.setBlock(target, BaseFireBlock.getState(level, target), 3)`. Target is `pos.relative(facing.getOpposite())`. |
| Branch 2: `IBlockIgnitableAdjacentIgniterBlock.igniteWithAdjacentIgniterBlock(world, offset, state, selfFacing)` (lines 45 to 46) | Same call on core's interface (`library/spi/block/IBlockIgnitableAdjacentIgniterBlock.java`, lines 8 to 18). The `facing` argument is the direction of the igniter as seen from the target, unchanged. |
| Branch 3: `RefractoryIgnitionHelper.igniteBlocks(world, offset)` (line 49) | `PyrotechIgnition.tryIgnite(level, target)`. |
| `getBlockLayer` returning `CUTOUT_MIPPED` (lines 153 to 157) | `"render_type": "minecraft:cutout_mipped"` in `models/block/igniter.json` (NF-SRC `net/neoforged/neoforge/client/model/ExtendedBlockModelDeserializer.java`, lines 66 to 67; `net/neoforged/neoforge/client/NamedRenderTypeManager.java`, line 48; NF-DOCS https://docs.neoforged.net/docs/1.21.1/resources/client/models/). Issue #27. |
| Brick igniter in refractory's `REFRACTORY_BLOCK_LIST`, all four facings (`modules/tech/refractory/init/RegistryInitializer.java`, lines 69 to 74) | `igniter_brick` added to `#pyrotech:refractory` by ignition's `BlockTagsProvider` contribution (NF-SRC `net/neoforged/neoforge/common/data/BlockTagsProvider.java`, line 18). Tags ignore facing. |

No comparator, no redstone output, no tooltip. Loot is `dropSelf` for both blocks (NF-SRC
`net/minecraft/data/loot/BlockLootSubProvider.java`, line 748).

## The torches

Two blocks share `BlockTorchBase` (`block/spi/BlockTorchBase.java`), which extends vanilla 1.12
`BlockTorch`. Each has a five-way `FACING` (up plus four walls) inherited from `BlockTorch` and a
`TYPE` of `lit`, `unlit`, `doused`, default `unlit` (lines 38, 46 to 53, 299 to 341). So a crafted
torch places unlit and must be lit. Lighting: an igniter through `igniteWithIgniterItem` (lines 82
to 89), or vanilla flint and steel through the tile's `InteractionUseItemToActivate`
(`tile/spi/TileTorchBase.java`, lines 45, 193 to 238), which refuses while it is raining on the
torch. Dousing: any container holding 1000 mB of a `VALID_DOUSING_FLUIDS` fluid (water by default,
`ModuleCoreConfig.java`, lines 1137 to 1139) through `InteractionExtinguish` (lines 177 to 191),
built on the shared `InteractionExtinguishable` that swallows the fluid into a throwaway tank and
plays `BLOCK_FIRE_EXTINGUISH` (`library/spi/interaction/InteractionExtinguishable.java`, lines 45
to 61, 76 to 89). Rain douses a lit torch on its next update when `EXTINGUISHED_BY_RAIN` is on;
when `BURNS_UP` is on, the torch counts world time since it was lit and turns to air when its
duration (14 minutes plus or minus up to 4, rolled per tile) runs out (lines 39 to 41, 108 to 135).
A lit torch damages entities that touch its box (`BlockTorchBase.java`, lines 185 to 204). A lit or
doused fiber torch drops a stick or dried plant fibers, never the torch; the stone torch always
drops itself (lines 231 to 243; `block/BlockTorchFiber.java`, lines 32 to 40; `BlockTorchStone.java`,
lines 31 to 34).

The vanilla 1.12 `BlockTorch` source is not in the repo. Three things the mod relies on come from
it: random ticks (the first `updateTick` has no other trigger), the wall and floor placement rules,
and the absence of hardness and sound type on the class (the vanilla torch sets those on its
registered instance). STORAGE traced the same class-default pattern for the wood rack.

| 1.12 construct | 1.21 replacement |
|---|---|
| `BlockTorch` parent with five-way `FACING`; `TYPE` enum property (lines 38, 269 to 272) | One block class, two instances (decision 3). `FACING` as `DirectionProperty.create("facing", d -> d != Direction.DOWN)` (NF-SRC `net/minecraft/world/level/block/state/properties/DirectionProperty.java`, line 22), `TYPE` as `EnumProperty.create("type", TorchType.class)` (`EnumProperty.java`, line 75). The converted `torch_fiber.json` and `torch_stone.json` key on exactly `facing=` and `type=` with these values (assets section). |
| Class defaults: no hardness, no sound type, `Material.CIRCUITS` | `Properties.of().instabreak().noCollission().sound(SoundType.WOOD).randomTicks()` (NF-SRC `BlockBehaviour.java`, lines 1199, 1159, 1185, 1208; `net/minecraft/world/level/block/SoundType.java`, line 10). Hardness 0 and no collision match 1.12. The wood sound is a deliberate cosmetic choice; the 1.12 instances played the `Block` default, which STORAGE identified as stone. |
| `getLightValue`: config light when lit, else the class default 0 (lines 96 to 107; `BlockTorchFiber.java`, lines 43 to 46) | `lightLevel(state -> state.getValue(TYPE) == LIT ? light : 0)` (line 1190). Light 9 for both (`ModuleIgnitionConfig.java`, lines 28 and 79) as a constructor argument. |
| `getBoundingBox` per facing (lines 40 to 44, 61 to 75) | `getShape` (line 361) with `Block.box` (Block.java, line 151): up `box(6, 0, 6, 10, 12, 10)`, north `box(5.6, 3.2, 11.2, 10.4, 14, 16)`, south `box(5.6, 3.2, 0, 10.4, 14, 4.8)`, west `box(11.2, 3.2, 5.6, 16, 14, 10.4)`, east `box(0, 3.2, 5.6, 4.8, 14, 10.4)`. |
| `addCollisionBoxToList` only for a null entity; `getCollisionBoundingBox` the torch box (lines 171 to 182) | `noCollission()` plus `getCollisionShape` returning `Shapes.empty()` (line 365; `net/minecraft/world/phys/shapes/Shapes.java`, line 38). Entities pass through, as in 1.12. |
| `BlockTorch` survival and drop rules | `canSurvive` (line 345): `Block.canSupportCenter(level, pos.below(), Direction.UP)` for `up` (Block.java, line 255; `net/minecraft/world/level/block/BaseTorchBlock.java`, lines 40 to 42) and `WallTorchBlock.canSurvive(level, pos, facing)` for walls (`net/minecraft/world/level/block/WallTorchBlock.java`, lines 76 to 79). `updateShape` (line 178) returns air when survival fails (BaseTorchBlock.java, lines 31 to 37; WallTorchBlock.java, line 109). |
| `BlockTorch.getStateForPlacement` (clicked face first, then the other sides) | `getStateForPlacement(BlockPlaceContext)` trying `context.getClickedFace()` when it is not down (UseOnContext.java, line 41), then the remaining horizontals, then up, keeping the first state that `canSurvive`. Vanilla splits this between two blocks and `StandingAndWallBlockItem` over `getNearestLookingDirections` (NF-SRC `net/minecraft/world/item/StandingAndWallBlockItem.java`, lines 29 to 46; `net/minecraft/world/item/context/BlockPlaceContext.java`, line 71); a plain `BlockItem` suffices for one block. |
| `updateTick`: `randomUpdate` on the tile, then `scheduleUpdate(pos, block, (10 + rand.nextInt(10)) * 20)` (lines 114 to 122) | `randomTick` and `tick` (lines 384 and 387) both calling the block entity's `randomUpdate` and `level.scheduleTick(pos, this, (10 + random.nextInt(10)) * 20)` (NF-SRC `net/minecraft/world/level/LevelAccessor.java`, line 52). The first tick still comes from `randomTicks()`; the rest are self-scheduled, as in 1.12. |
| `randomDisplayTick`: smoke and flame at `y + 0.7 + 2/16`, offset 0.17 toward the wall (lines 125 to 144) | `animateTick` (Block.java, line 277) with `ParticleTypes.SMOKE` and `FLAME` (ParticleTypes.java, lines 84 and 55) at the 1.12 offsets. `WallTorchBlock#animateTick` is the vanilla pattern (line 117), with different offsets; keep the 1.12 numbers. |
| `onEntityCollidedWithBlock`: lit, fire damage above 0, torch box intersects the entity box, then `DamageSource.IN_FIRE` (lines 185 to 204) | `entityInside` (line 424). 1.21 calls it for every block inside the entity's bounding box regardless of the block's shape (NF-SRC `net/minecraft/world/entity/Entity.java`, lines 1027 to 1045), so keep the intersection test: `getShape(...).bounds().move(pos).intersects(entity.getBoundingBox())` (`net/minecraft/world/phys/shapes/VoxelShape.java`, line 37; `net/minecraft/world/phys/AABB.java`, lines 317 and 339; Entity.java, line 3026), then `entity.hurt(level.damageSources().inFire(), damage)` (Entity.java, line 1579; Level.java, line 1251; `net/minecraft/world/damagesource/DamageSources.java`, line 85). Fire damage 1 (config lines 63 and 114) as a constructor argument. |
| `onBlockActivated`: igniter returns `false`, else the interaction chain (lines 218 to 227) | `useItemOn` (line 226): `SKIP_DEFAULT_BLOCK_INTERACTION` for `#pyrotech:igniters`; the douse and the flint and steel branches below; else `PASS_TO_DEFAULT_BLOCK_INTERACTION`. |
| `InteractionExtinguish`: lit, held container drains 1000 mB of a dousing fluid, torch doused, `lastTimeStamp` reset, extinguish sound (`TileTorchBase.java`, lines 177 to 191; `InteractionExtinguishable.java`, lines 45 to 89) | `FluidUtil.getFluidHandler(stack)` (NF-SRC `net/neoforged/neoforge/fluids/FluidUtil.java`, line 368), `drain(1000, SIMULATE)` then `EXECUTE` (`net/neoforged/neoforge/fluids/capability/IFluidHandler.java`, line 18) when the drained fluid is in `#pyrotech:dousing` (a core fluid tag replacing `VALID_DOUSING_FLUIDS`, findings), then `setDoused()`, `lastTimeStamp = 0`, `SoundEvents.FIRE_EXTINGUISH` (line 525). This is a shared helper: tech/basic's campfire uses the same `InteractionExtinguishable` (its import at line 8 names `BlockCampfire`). |
| `InteractionUseItemToActivate(Items.FLINT_AND_STEEL)`: exact item test, not raining, `activate()`, flint sound at pitch 0.8 to 1.2 (lines 193 to 238, test at line 214) | `stack.is(Items.FLINT_AND_STEEL)` (ItemStack.java, line 343; `net/minecraft/world/item/Items.java`, line 925), not the ability: a fire charge would otherwise light torches, which 1.12 refused. Sound `FLINTANDSTEEL_USE` at `1.0F, random.nextFloat() * 0.4F + 0.8F`. Whether athenaeum's `InteractionUseItemBase` damaged the flint and steel is not in the repo; vanilla lighting a campfire costs 1 durability (FlintAndSteelItem.java, line 58). Confirm against a 1.12 client during hoisting. |
| `getDrops`: unlit drops the block, else the lit drops; `removedByPlayer` and `harvestBlock` keep the tile alive for it (lines 231 to 261) | A loot table with `LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(TYPE, UNLIT))` (NF-SRC `net/minecraft/world/level/storage/loot/predicates/LootItemBlockStatePropertyCondition.java`, lines 53 and 65; `net/minecraft/advancements/critereon/StatePropertiesPredicate.java`, line 75) for the self drop, and for the fiber torch a second pool of `LootItem.lootTableItem(Items.STICK)` and `lootTableItem(plant_fibers_dried)` at weight 1 each (`net/minecraft/world/level/storage/loot/entries/LootItem.java`, line 47; `LootPoolSingletonContainer.java`, line 117; `LootPool.java`, line 187) for the other two states. Note `doused` takes the lit drops in 1.12: a fiber torch never comes back once lit. The tile-keeping tricks drop. |
| `TileTorchBase` fields `duration`, `lastTimeStamp`; duration rolled in the constructor (lines 33 to 41); NBT (lines 143 to 158) | A plain `BlockEntity` (no ticker, no sync; nothing on the client reads it) with the same two fields rolled in `BlockEntity(pos, state)`, `saveAdditional` and `loadAdditional` (NF-SRC `net/minecraft/world/level/block/entity/BlockEntity.java`, lines 94 and 77). One `BlockEntityType` over both torches; the block supplies duration, variant, and the two toggles. |
| `update` first-tick `checkLightFor`; `readFromNBT` relight; `setWorldCreate` (lines 100 to 106, 157, 161 to 165) | Dropped. Light is a function of the blockstate, and the game relights on state changes (PROTOTYPE pattern 2). |
| `randomUpdate`: `isRainingAt(pos.up())`, `getTotalWorldTime`, `setBlockToAir` (lines 108 to 135) | `level.isRainingAt(pos.above())` (Level.java, line 1074; BlockPos.java, line 161), `level.getGameTime()` (line 998), `level.removeBlock(pos, false)` (line 311). The timestamp quirk is decision 6. |
| `addInformation`: red or green "burns up" and "rain" lines from config (`BlockTorchFiber.java`, lines 55 to 62) | `Block#appendHoverText` (Block.java, line 537); keys on `main` (`en_us.json`, lines 419 to 422). Reads the config toggles of decision 5. |

## The oil lamp

`BlockLampOil` is a glass-sounding stone block (`Material.ROCK`, hardness 1, pickaxe 0,
`SoundType.GLASS`) with a `lit` boolean and a 10 by 16 by 10 box (`block/BlockLampOil.java`, lines
40 to 55). Lit, it gives light 12 (config) and a flame particle (lines 77 to 89, 104 to 113). An
igniter lights it through `igniteWithIgniterItem` (lines 62 to 69). The tile owns a 2000 mB tank
that only accepts fluids named in `ALLOWED_FUEL` (`tile/TileLampOil.java`, lines 99 to 106, 293 to
313; `ModuleIgnitionConfig.java`, lines 135 to 138: pyroberry wine 12 mB per minute, lamp oil 10),
three interactions in order (bucket fill or drain on any face; empty-hand click puts a lit lamp
out; flint and steel lights it, lines 63 to 67, 198 to 287), and a server tick that puts the lamp
out when the tank empties and otherwise burns `mBPerMinute / 1200` per tick, draining whole
millibuckets as they accumulate (lines 122 to 151). `setActive(true)` does nothing on an empty
tank (line 86). The tile serialises into the dropped item (`BlockLampOil.java`, lines 191 to 207)
and vanilla's `ItemBlock` restores it on placement, which is why the tank's NBT carries the
"Empty" workaround (`TileLampOil.java`, lines 315 to 348). A gamestages hook gates the interactions
(lines 193 to 196).

| 1.12 construct | 1.21 replacement |
|---|---|
| `BlockPartialBase`, `Material.ROCK`, hardness 1, pickaxe 0, `SoundType.GLASS` (lines 46 to 51) | `Block implements EntityBlock`, or `BaseEntityBlock` with `getRenderShape` overridden to `MODEL` because its default is `INVISIBLE` (NF-SRC `net/minecraft/world/level/block/BaseEntityBlock.java`, lines 26 to 27). `Properties.of().mapColor(MapColor.STONE).strength(1.0f).sound(SoundType.GLASS).noOcclusion()` (lines 1203, 1185, 1165; SoundType.java, line 34), pickaxe tag. |
| `LIT` property, meta (lines 40, 233 to 250) | `BlockStateProperties.LIT` (BlockStateProperties.java, line 26). The converted `blockstates/lamp_oil.json` keys on `lit=false` and `lit=true`. |
| `AABB` 3,0,3 to 13,16,13 (line 44) | `getShape` returning `Block.box(3, 0, 3, 13, 16, 13)`. |
| `getLightValue` 12 when lit (lines 77 to 89; config line 144) | `lightLevel(state -> state.getValue(LIT) ? 12 : 0)`. |
| `getBlockLayer` `CUTOUT` (lines 97 to 100) | `"render_type": "minecraft:cutout"` in `models/block/lamp_oil.json` and `lamp_oil_lit.json` (NamedRenderTypeManager.java, line 46). Issue #27. |
| `randomDisplayTick`: flame at the block centre, `y + 0.6` (lines 104 to 113) | `animateTick` with `ParticleTypes.FLAME`. |
| `igniteWithIgniterItem` calling `setActive(true)` (lines 62 to 69) | Same on core's interface. |
| `onBlockActivated`: igniter returns `false`, else the chain (lines 144 to 163) | `useItemOn`: `SKIP_DEFAULT_BLOCK_INTERACTION` for igniters; `stack.is(Items.FLINT_AND_STEEL)` lights it with the flint sound (lines 244 to 287); else `FluidUtil.interactWithFluidHandler(player, hand, lamp.getTank())` (FluidUtil.java, line 74), the overload that takes the handler. `useWithoutItem` (line 222): lit and empty hand puts it out with `FIRE_EXTINGUISH` (lines 211 to 242, empty-hand test at 222). |
| No `hasCapability` or `getCapability` on the tile | No `Capabilities.FluidHandler.BLOCK` registration (NF-SRC `net/neoforged/neoforge/capabilities/Capabilities.java`, line 29). Pipes and hoppers never filled a lamp in 1.12; the handler overload above is why none is needed. The held container works through `Capabilities.FluidHandler.ITEM`, which covers BUCKET's items. |
| `TileEntityDataBase`, `TileDataFluidTank` (lines 41, 55 to 59) | PROTOTYPE `SyncedBlockEntity` with the `FluidStack` in `saveSynced`; `FluidTank#onContentsChanged` (NF-SRC `net/neoforged/neoforge/fluids/capability/templates/FluidTank.java`, line 152) calls `setChanged` and `sync()`. `lit` is the blockstate. |
| `Tank extends ObservableFluidTank` with the `fill` fuel filter (lines 293 to 313) | `new FluidTank(2000).setValidator(stack -> LampFuels.ratePerMinute(stack) > 0)` (FluidTank.java, lines 25 and 39). No `HotFluidTank`: the lamp has no hot-fluid rule, exactly as STORAGE item 2 predicted. |
| `ALLOWED_FUEL` map of fluid name to mB per minute (config lines 135 to 138) | A fluid-keyed data map (decision 7). |
| `update`: extinguish on empty, `millibucketsUsed += rate / 1200f`, `drainInternal(used, true)` (lines 122 to 151) | The same loop in a server `BlockEntityTicker`, with `tank.drain(used, FluidAction.EXECUTE)` (FluidTank.java, line 139; `drain` ignores the validator, so `drainInternal` needs no equivalent). Extinguish sound `FIRE_EXTINGUISH` at pitch 0.8 to 1.2. |
| `getDrops` serialising the tile, vanilla `ItemBlock` restoring it, the "Empty" NBT hack (`BlockLampOil.java`, lines 167 to 207; `TileLampOil.java`, lines 315 to 348) | STORAGE's tank pattern: a `DataComponentType<SimpleFluidContent>` (NF-SRC `net/neoforged/neoforge/fluids/SimpleFluidContent.java`, lines 26 to 27 and 38; `DeferredRegister#createDataComponents`, line 169, and `registerComponentType`, line 667), `collectImplicitComponents` and `applyImplicitComponents` on the block entity (BlockEntity.java, lines 322 and 293), and a loot table with `CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY)` (NF-SRC `net/minecraft/world/level/storage/loot/functions/CopyComponentsFunction.java`, line 77; `BlockLootSubProvider#createShulkerBoxDrop`, line 270, as the template). `millibucketsUsed` was also in the item NBT (line 169); it is a sub-millibucket fraction and does not need to survive the round trip. |
| `TESRLampOil`: fluid box inset 3.1 px, bottom at 1.1 px, height `4 px * fill`, still sprite, fluid colour, block light (`client/render/TESRLampOil.java`, lines 26 to 27, 59 to 257) | STORAGE's `library/fluid` box renderer with those numbers, a `BlockEntityRenderer#render` (NF-SRC `net/minecraft/client/renderer/blockentity/BlockEntityRenderer.java`, line 12) registered in `RegisterRenderers`. Sprite and tint from `IClientFluidTypeExtensions.of(fluid)` (`net/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions.java`, lines 42, 294, 277). |
| `getStages` and `STAGES_OIL_LAMP` (`TileLampOil.java`, lines 193 to 196; config lines 12 to 13; `plugin/crafttweaker/ZenOilLamp.java`) | Dropped with gamestages (HUNTING, STORAGE). |

## The lamp oil fluid

What 1.12 registers. `FluidInitializer.createFluid("lamp_oil", true, fluid -> fluid.setDensity(1000).setViscosity(1000), BlockFluidTannin::new)`
(`init/FluidInitializer.java`, lines 25 to 30). The library builds the `Fluid` with textures
`pyrotech:blocks/fluid_lamp_oil_still` and `_flow` (`library/FluidInitializerRegistry.java`, lines
59 to 63), registers the block as `pyrotech:fluid.lamp_oil` with an `ItemBlock` (lines 120 and
145), adds a universal bucket (line 152), and maps the block to the shared `blockstates/fluid.json`
marker (lines 178 to 193). The block is `BlockFluidClassic` with `Material.WATER` and no fire info
(`modules/hunting/block/fluid/BlockFluidTannin.java`, lines 7 to 15). Temperature and light are the
Forge defaults, 300 and 0. So lamp oil is a placeable, flowing, water-like liquid that does not
burn and is not hot. Its only producer is tech/machine's stone crucible: lard gives 125 mB in
12,000 ticks (`modules/tech/machine/init/recipe/StoneCrucibleRecipesAdd.java`, lines 122 to 127).
Its only consumer is the lamp.

The port follows REFRACTORY's tar table line for line, with two differences: density and
viscosity are 1000, and there is no flammability override.

| 1.12 construct | 1.21 replacement |
|---|---|
| `Fluid` with density and viscosity 1000 (line 28) | `FluidType` under `NeoForgeRegistries.Keys.FLUID_TYPES`, `FluidType.Properties.create().density(1000).viscosity(1000)` (NF-SRC `net/neoforged/neoforge/fluids/FluidType.java`, lines 889, 1062, 1085), bucket sounds through `sound(SoundActions.BUCKET_FILL, ...)` and `BUCKET_EMPTY` (line 1024). Temperature stays default (`getTemperature()`, line 189; "hot" is 450 and above), light stays 0 (line 1049). |
| `BlockFluidClassic` source and flow in one block | `BaseFlowingFluid.Source` and `.Flowing` (NF-SRC `net/neoforged/neoforge/fluids/BaseFlowingFluid.java`, lines 161 and 141) over one `BaseFlowingFluid.Properties(type, still, flowing).bucket(...).block(...)` (lines 186, 192, 197), registered under `Registries.FLUID` as `pyrotech:lamp_oil` and `pyrotech:flowing_lamp_oil`. Milk is the pattern (NF-SRC `net/neoforged/neoforge/common/NeoForgeMod.java`, line 668). |
| The fluid block `pyrotech:fluid.lamp_oil`, `Material.WATER` (FluidInitializerRegistry.java, line 120) | `new LiquidBlock(fluid, properties)` (NF-SRC `net/minecraft/world/level/block/LiquidBlock.java`, line 66) with the water chain `replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY)` (NF-SRC `net/minecraft/world/level/block/Blocks.java`, lines 339 to 352). Its id is decision 8: `lamp_oil` is taken by the lamp. |
| No `setFireInfo` | No `getFlammability` or `getFireSpreadSpeed` override. Lamp oil does not burn in 1.12 and should not start burning now. |
| Universal bucket (line 152) | One `BucketItem(fluid, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))` (NF-SRC `net/minecraft/world/item/BucketItem.java`, line 34; Item.java, lines 463 and 452), id `pyrotech:lamp_oil_bucket`, with a `neoforge:fluid_container` item model per BUCKET and REFRACTORY. |
| `blockstates/fluid.json` marker (ASSETS lists it as not convertible) | `IClientFluidTypeExtensions` with `getStillTexture` and `getFlowingTexture` (IClientFluidTypeExtensions.java, lines 76 and 92) registered in `RegisterClientExtensionsEvent#registerFluidType` (NF-SRC `net/neoforged/neoforge/client/extensions/common/RegisterClientExtensionsEvent.java`, line 100), as `ClientNeoForgeMod` does for water (lines 98 to 110). The textures `block/fluid_lamp_oil_still.png` and `_flow.png` with their `.mcmeta` are on `main`. A blockstate and a particle-only model for the liquid block copy vanilla water (DATA `assets/minecraft/blockstates/water.json`, `models/block/water.json`). |
| `fluid.lamp_oil`, `tile.fluid.lamp_oil.name` (`main` `en_us.json`, lines 91 to 92) | Dead keys. Add `fluid_type.pyrotech.lamp_oil` (FluidType.java, line 146), the liquid block key, and `item.pyrotech.lamp_oil_bucket`. |
| No fluid tag | `#pyrotech:lamp_oil` from a `FluidTagsProvider` (NF-SRC `net/minecraft/data/tags/FluidTagsProvider.java`, line 22) listing source and flowing, so the crucible recipe and any future consumer can use `SizedFluidIngredient` on the tag (RECIPES). |

## Events

Ignition has no `event/` package and registers no game-bus listener in 1.12. In 1.21 it needs
only mod-bus listeners, all in its `register(IEventBus)` hook or a client class annotated
`@EventBusSubscriber(modid = ..., bus = Bus.MOD)` (NF-DOCS https://docs.neoforged.net/docs/1.21.1/concepts/events/):

| Purpose | 1.21 listener |
|---|---|
| Lamp renderer | `EntityRenderersEvent.RegisterRenderers` (EntityRenderersEvent.java, line 109). |
| Lamp oil textures | `RegisterClientExtensionsEvent#registerFluidType` (line 100). |
| Creative tab entries | `BuildCreativeModeTabContentsEvent` (line 26). |
| Lamp fuel data map type, if ignition owns it (decision 7) | `RegisterDataMapTypesEvent#register` (NF-SRC `net/neoforged/neoforge/registries/datamaps/RegisterDataMapTypesEvent.java`, line 38). |

The fire-adjacency `NeighborNotifyEvent` path that lights a pit kiln or tar collector from a
neighbouring fire is core's listener (`NeighborNotifyEventHandler.java`, lines 19 to 45; NF-SRC
`net/neoforged/neoforge/event/level/BlockEvent.java`, lines 168 and 183). The right-click listener
for vanilla flint and steel on a log pile is refractory's. Neither touches ignition.

## Recipes and loot

Eleven crafting recipes. Material metas resolve through `ItemMaterial.EnumType`
(`modules/core/item/ItemMaterial.java`): 5 `refractory_brick` (line 80), 10 `flint_shard` (85),
13 `plant_fibers_dried` (88), 16 `brick_stone` (91), 20 `board` (95), 21 `coal_pieces` (96),
39 `leather_strap` (114), 44 `leather_durable_cord` (119), 48 `bow_drill_durable_stick` (123),
54 `clay_blasting` (129). Ore dictionary names follow TOOL and CORE: `stickWood` is
`Tags.Items.RODS_WOODEN` (NF-SRC `net/neoforged/neoforge/common/Tags.java`, line 720), `rock` and
`stickStone` are pyrotech tags, `blockGlassColorless` is `Tags.Items.GLASS_BLOCKS_COLORLESS`
(line 573), `string` is `Tags.Items.STRINGS` (line 786).

| Recipe (1.12 file) | Shape | Inputs | Result |
|---|---|---|---|
| `ignition/bow_drill` | shapeless | `minecraft:bow` (Items.java, line 928), `#c:rods/wooden` | `bow_drill` |
| `ignition/flint_and_tinder` | shapeless | `flint_shard`, `plant_fibers_dried`, `#pyrotech:rocks` | `flint_and_tinder` |
| `ignition/matchstick` | `SSS` / `SBS` / `SSS` | S `#c:rods/wooden`, B `clay_blasting` | 8 `matchstick` |
| `ignition/torch_fiber` | `F` / `S` | F `plant_fibers_dried`, S `#c:rods/wooden` | `torch_fiber` |
| `ignition/torch_stone` | `F` / `S` | F `coal_pieces`, S the stone stick tag | `torch_stone` |
| `ignition/lamp_oil` | ` G ` / `BSB` / ` G ` | G `#c:glass_blocks/colorless`, B `brick_stone`, S `#c:strings` | `lamp_oil` |
| `ignition/igniter_stone` | `AAA` / `rRB` / `AAA` | A `brick_stone`, r redstone (line 759), R redstone block (763), B iron bars (399) | `igniter_stone` |
| `ignition/igniter_brick` | same | A `refractory_brick` | `igniter_brick` |
| root `bow_drill_durable` | ` S\|` / `sL\|` / ` S\|` | `\|` `leather_durable_cord`, S `#c:rods/wooden`, s stone stick, L `leather_strap` | `bow_drill_durable` |
| root `bow_drill_durable_stick` | `-` / `S` / `\|` | `-` `board`, S `leather_strap`, `\|` `#c:rods/wooden` | 2 `bow_drill_durable_stick` |
| root `bow_drill_durable_repair` | shapeless | `bow_drill_durable`, `bow_drill_durable_stick` | `bow_drill_durable_spindle` |

All eleven are `ShapedRecipeBuilder` or `ShapelessRecipeBuilder` calls (NF-SRC
`net/minecraft/data/recipes/ShapedRecipeBuilder.java`, lines 50, 68, 75, 96; `ShapelessRecipeBuilder.java`,
lines 45, 63, 70) in an `IgnitionRecipes` static class under RECIPES' shared `PyrotechRecipeProvider`.
Every builder needs one `unlockedBy` criterion or `save` throws (lines 105, 126, 143);
`RecipeProvider.has(...)` (NF-SRC `net/minecraft/data/recipes/RecipeProvider.java`, lines 705 and
709) on the main input is enough. The `modules_enabled` conditions on the eight ignition files
drop. The two hunting inputs mean the body and spindle recipes reference hunting's flat ids
(HUNTING: `pyrotech:leather_durable_cord`, `pyrotech:leather_strap`).

The vanilla torch. Core's config removes `minecraft:torch` (`ModuleCoreConfig.java`, line 797)
and `torch_vanilla_coal.json` puts back a coal-only version: coal with `data: 0` over `stickWood`
gives 4 torches (root recipe, lines 8 to 20). Charcoal is `minecraft:coal` meta 1 in 1.12, so the
`data: 0` is what refuses it. That recipe stays core's. In 1.21 it is
`ShapedRecipeBuilder.shaped(..., Items.TORCH, 4).define('W', Items.COAL).define('|', Tags.Items.RODS_WOODEN)`
(Items.java, lines 333 and 930), and the removal is the `neoforge:false` stub per CORE. The vanilla
soul torch (DATA `data/minecraft/recipe/soul_torch.json`) is coal or charcoal over a stick over
`#minecraft:soul_fire_base_blocks` (soul sand or soul soil, DATA `data/minecraft/tags/item/soul_fire_base_blocks.json`),
giving 4; issue #33 decision 6 removes it because of the charcoal. The replacement is decision 2.

Loot tables, from an ignition section of RECIPES' `PyrotechBlockLoot`: `dropSelf` for the two
igniters and the stone torch (the stone torch drops itself in every state, `BlockTorchStone.java`,
lines 31 to 34); the state-conditioned table for the fiber torch (torch section); the
component-copying table for the lamp. The liquid block is `noLootTable()`.

Datagen placement per RECIPES: one `IgnitionRecipes` class, one loot section, one `IgnitionTags`
contribution (item tag `#pyrotech:igniters` with four items; block tag `#pyrotech:refractory` with
`igniter_brick`; `mineable/pickaxe` for the three stone blocks; fluid tag `#pyrotech:lamp_oil`),
and the lamp fuel data map (decision 7). No furnace fuels: 1.12 sets no burn time on any ignition
item, and core's fuel handler names none of them.

## Network payloads

Ignition registers no packets. `PACKET_SERVICE` exists only to create the tile data service
(`ModuleIgnition.java`, lines 46 to 47), and the only tile that registers data is `TileLampOil`
(`TileLampOil.java`, lines 41, 55 to 59). The torch and igniter tiles extend `TileEntityBase` and
sync nothing. In 1.21 the lamp's fluid rides the `SyncedBlockEntity` snapshot; the torches and the
igniter need no snapshot because everything the client draws is a blockstate property. Ignition
contributes zero payload types to core's registrar (NF-DOCS https://docs.neoforged.net/docs/1.21.1/networking/payload/).

## Config

`ModuleIgnitionConfig` is 240 lines. Every knob, its 1.12 default, and its proposed fate under the
config principle from issue #22 (behaviour toggles and gameplay multipliers stay; numbers that
describe an item, recipe, or fuel go to data or constants):

| Category and knob | Default | Fate |
|---|---|---|
| `STAGES_OIL_LAMP` (line 13) | none | Drop with gamestages. |
| `FIBER_TORCH.LIGHT_VALUE` (line 28) | 9 | Bake. |
| `FIBER_TORCH.EXTINGUISHED_BY_RAIN` (line 34) | true | Config: `ModConfigSpec.Builder#define(path, true)` (NF-SRC `net/neoforged/neoforge/common/ModConfigSpec.java`, line 733). |
| `FIBER_TORCH.BURNS_UP` (line 40) | true | Config. |
| `FIBER_TORCH.DURATION` (line 47) | 16800 | Bake. |
| `FIBER_TORCH.DURATION_VARIANT` (line 55) | 4800 | Bake. |
| `FIBER_TORCH.FIRE_DAMAGE` (line 63) | 1 | Bake. Zero disabled it; nobody ships zero. |
| `STONE_TORCH.LIGHT_VALUE` (line 79) | 9 | Bake. |
| `STONE_TORCH.EXTINGUISHED_BY_RAIN` (line 85) | true | Config. |
| `STONE_TORCH.BURNS_UP` (line 91) | false | Config. |
| `STONE_TORCH.DURATION`, `DURATION_VARIANT` (lines 98, 106) | 16800, 4800 | Bake (only read when `BURNS_UP` is on). |
| `STONE_TORCH.FIRE_DAMAGE` (line 114) | 1 | Bake. |
| `OIL_LAMP.CAPACITY` (line 129) | 2000 | Bake as the tank capacity. |
| `OIL_LAMP.ALLOWED_FUEL` (lines 135 to 138) | wine 12, lamp oil 10 | Data: the fluid-keyed map of decision 7. |
| `OIL_LAMP.LIGHT_VALUE` (line 144) | 12 | Bake. |
| `IGNITERS.BOW_DRILL_DURABILITY`, `USE_DURATION_TICKS`, `COOLDOWN_TICKS` (lines 160, 167, 174) | 32, 20, 20 | Bake as `durability(32)` and constructor arguments. |
| `IGNITERS.DURABLE_BOW_DRILL_*` (lines 181, 188, 195) | 48, 20, 20 | Bake. |
| `IGNITERS.FLINT_AND_TINDER_*` (lines 202, 209, 216) | 8, 80, 20 | Bake. |
| `IGNITERS.MATCHSTICK_MAX_STACK_SIZE`, `USE_DURATION_TICKS`, `COOLDOWN_DURATION_TICKS` (lines 223, 230, 237) | 64, 5, 40 | Bake. |

That leaves four booleans in an `ignition` section of the shared `ModConfigSpec` (`push`/`pop`,
lines 842 and 860). The surface is decision 5. One consequence to name: CORE chose one `COMMON`
config, and NF-DOCS says `COMMON` is not synced to clients while `SERVER` is
(https://docs.neoforged.net/docs/1.21.1/misc/config/). The torch tooltip reads the two toggles, so
on a dedicated server whose file differs from the client's, the tooltip shows the client's values.
1.12's `@Config` had the same limit, so this is faithful; it is noted so it is not mistaken for a
bug later.

## Dropped outright

- `plugin/`: 7 files. `ZenOilLamp` (CraftTweaker), `PluginJEI` (one blacklist line), `PluginTOP`
  with its `OilLampProvider`, `PluginWaila` with `OilLampProviderDelegate` and `OilLampProvider`.
  The lamp's tank readout on hover was TOP and Waila's; nothing in the port shows it.
- `ModuleBase`, `Registry`, `enableAutoRegistry`, `IPacketService`, `ITileDataService`, `Injector`,
  `@GameRegistry.ObjectHolder`, `FMLInterModComms`, the `modules_enabled` recipe conditions, the
  `isModuleEnabled(ModuleTechRefractory.class)` guard in `ItemIgniterBase` (line 129), and
  `ModuleIgnition.LOGGER`.
- `FluidInitializerRegistry`, `BlockFluidClassic`, `BlockFluidLampOil` (dead in 1.12 already),
  the `BlockFluidTannin` factory reference, the universal bucket, `blockstates/fluid.json`.
- `IBlockVariant`, `getStateFromMeta`, `getMetaFromState`, `damageDropped`, `getSubBlocks`,
  `getModelName`, `getBlockLayer`, `collisionRayTrace` and `interactionRayTrace`, `IBlockInteractable`,
  `ITileInteractable`, the `IInteraction[]` arrays, `InteractionBucketBase`, `InteractionUseItemBase`,
  `InteractionBase`.
- `removedByPlayer` and `harvestBlock` on the torches and the lamp; the "Empty" tank NBT hack;
  `ObservableFluidTank`; `setWorldCreate`; the first-tick `checkLightFor` on the torch tile;
  `TileEntitySpecialRenderer` and `renderTileEntityFast`.
- `getUnlocalizedName` on the spindle (replaced by `getDescriptionId`); the spindle's explicit
  `ENTITY_ITEM_BREAK` (vanilla plays it); the `damageItem` max-damage guard (vanilla breaks at max).
- `SHOW_DURABILITY_TOOLTIPS` reads (TOOL decision 4).

## Assets

What `main` has for this unit (all under `src/main/resources/assets/pyrotech/`):

- Blockstates: `igniter.json` keyed on `facing=` and `variant=` with `gen/igniter/igniter` for
  brick and `igniter_2` for stone; `lamp_oil.json` keyed on `lit=`; `torch_fiber.json` and
  `torch_stone.json` keyed on `facing=up|north|south|east|west` and `type=lit|unlit|doused`, with
  `gen/<torch>/torch` for doused, `torch_2` lit, `torch_3` unlit, and the `torch_wall*` trio rotated
  by facing; `torch_backup.json`.
- Block models: `igniter.json` (a full cube with `igniter_back` and `igniter` overlays on north and
  south), `lamp_oil.json` and `lamp_oil_lit.json`, `torch.json` and `torch_wall.json` (the base
  torch shapes, `ambientocclusion` false), and the `gen/` wrappers: `gen/igniter/igniter.json`
  (refractory brick), `igniter_2.json` (masonry brick), six per torch under `gen/torch_fiber/`,
  `gen/torch_stone/`, and `gen/torch_backup/`.
- Item models: `bow_drill.json`, `bow_drill_durable.json`, `bow_drill_durable_spindle.json`,
  `flint_and_tinder.json`, `matchstick.json` (all `item/handheld`). `tinder.json` is tech/basic's.
- Textures: `block/igniter.png`, `igniter_back.png`, `lamp_oil_other.png`, `lamp_oil_side.png`,
  `lamp_oil_top.png`, `torch_fiber_doused.png`, `torch_fiber_off.png`, `torch_fiber_on.png` with
  `.mcmeta`, the stone torch trio the same, `fluid_lamp_oil_still.png` and `_flow.png` with
  `.mcmeta`; `item/bow_drill.png`, `bow_drill_durable.png`, `bow_drill_durable_empty.png`,
  `bow_drill_durable_stick.png`, `flint_and_tinder.png`, `matchstick.png`.
- Lang (`en_us.json`): `item.pyrotech.bow_drill`, `bow_drill_durable`, `flint_and_tinder`,
  `matchstick` (lines 82 to 85), `block.pyrotech.igniter_stone`, `igniter_brick`, `torch_fiber`,
  `torch_stone`, `lamp_oil` (lines 86 to 90), `item.pyrotech.bow_drill_durable_stick` (line 300),
  `gui.pyrotech.tooltip.durability.full` (line 399), the four torch tooltip keys (lines 419 to
  422), and the dead `fluid.lamp_oil` and `tile.fluid.lamp_oil.name` (lines 91 to 92).

Gaps:

- `blockstates/igniter.json` must split into `igniter_brick.json` (pointing at `gen/igniter/igniter`)
  and `igniter_stone.json` (`igniter_2`), facing rotations kept, `variant=` removed. As with
  REFRACTORY's collectors, the `_2` wrapper is the stone variant.
- `torch_backup.json` and `gen/torch_backup/` are dead. No 1.12 block registers as `torch_backup`
  (nothing in the Java tree names it), it keys on `state=` instead of `type=`, and ASSETS flags it
  for three missing textures (`torch_doused`, `torch_off`, `torch_on`). Delete the seven files.
- Item models: none exist for `igniter_stone`, `igniter_brick`, `torch_fiber`, `torch_stone`,
  `lamp_oil`, or `lamp_oil_bucket`. The igniters and lamp take their block model as parent. The
  torches follow vanilla: `item/generated` over the unlit block texture (DATA
  `assets/minecraft/models/item/torch.json`), so `torch_fiber.json` uses `pyrotech:block/torch_fiber_off`.
  The bucket is a `neoforge:fluid_container` model (BUCKET).
- Fluid assets for the liquid block: a blockstate with a `""` variant and a particle-only model,
  copying vanilla water, under whatever id decision 8 picks.
- Lang: `fluid_type.pyrotech.lamp_oil`, the liquid block key, `item.pyrotech.lamp_oil_bucket`.
  `bow_drill_durable_spindle` needs no key if `getDescriptionId` reuses the body's, as 1.12 did.
- Render types (issue #27): `igniter.json` needs `cutout_mipped` (1.12 `getBlockLayer`);
  `lamp_oil.json` and `lamp_oil_lit.json` need `cutout` (1.12 `getBlockLayer`); `torch.json` and
  `torch_wall.json` need `cutout`. The torch base has no `getBlockLayer` in the mod, but the
  textures are torch sprites on transparent ground, and vanilla registers its torches as cutout in
  `ItemBlockRenderTypes` (NF-SRC `net/minecraft/client/renderer/ItemBlockRenderTypes.java`, lines
  93 to 95), which a mod block does not inherit (NF-DOCS models page: absent `render_type` falls
  back to `ItemBlockRenderTypes`, then solid).

JEI. The 1.12 plugin only blacklisted the spindle (`PluginJEI.java`, lines 17 to 18). The shared
plugin should hide `bow_drill_durable_spindle` and add nothing else; the eleven crafting recipes
appear on their own. An info page for the igniters would mirror the Patchouli "verified
fire-starting device" appendix and is optional. Not designed here.

## Findings for other tickets

- **Core** (issue #10): `IgniterItem` cannot supply `Item` overrides as an interface; the shape is
  three hook methods plus a static `IgniterUse` helper, or an abstract base class (igniter items
  section). `VALID_DOUSING_FLUIDS` (`ModuleCoreConfig.java`, lines 1137 to 1139) becomes a core
  fluid tag `#pyrotech:dousing` containing water, and the douse step of `InteractionExtinguishable`
  (drain 1000 mB of a tagged fluid from the held container, then act) becomes a small shared
  helper, since the torches and tech/basic's campfire both use it. The block-side interfaces take
  `(Level, BlockPos, BlockState, Direction)`. `torch_vanilla_coal.json` and the `minecraft:torch`
  removal are core's. The `bow_drill_durable_stick` material item stays in core; its recipe and the
  two that use it are generated by ignition (recipes section).
- **Tech/basic** (issue #16): `BlockCampfire` and `BlockKilnPit` return
  `SKIP_DEFAULT_BLOCK_INTERACTION` for `#pyrotech:igniters`; the pit kiln also for
  `canPerformAction(FIRESTARTER_LIGHT)` (`BlockKilnPit.java`, lines 266 to 268), which lets vanilla
  flint and steel set fire on top and core's fire-adjacency listener light it (lines 207 to 219).
  `BlockCampfire.igniteWithAdjacentIgniterBlock` delegates to the item path (lines 87 to 90).
  `tinder` is tech/basic's item; its model on `main` is that ticket's. The campfire shares the
  douse helper above.
- **Tech/bloomery** (issue #19): `BlockBloomery.onBlockActivated` tests the igniter only on the top
  half (line 220); `igniteWithIgniterItem` only accepts the top half from `UP` (lines 104 to 118).
  Both become the tag test.
- **Tech/machine** (issue #20): `BlockCombustionWorkerStoneBase` tests the igniter only on the
  bottom half (line 281) and `igniteWithIgniterItem` only when the clicked side is the front
  (lines 79 to 90). `StoneCrucibleRecipesAdd` produces lamp oil from lard (lines 122 to 127) and
  references ignition's fluid holder, so ignition hoists before that datagen method or the recipe
  names the fluid by id.
- **Render layers** (issue #27): `igniter.json` cutout_mipped; `lamp_oil.json`, `lamp_oil_lit.json`,
  `torch.json`, `torch_wall.json` cutout.
- **Storage sign-off** (issue #28): the lamp confirms STORAGE item 2 (`setValidator` is enough, no
  `HotFluidTank`) and reuses the box renderer. The `FluidUtil` bucket helper should accept an
  `IFluidHandler` as well as a position, because the lamp registers no block capability.
- **Refractory sign-off** (issue #29): reverse the "igniters should answer `FIRESTARTER_LIGHT`"
  finding (decision 1). The listener keeps the ability test and therefore sees only vanilla items;
  igniters reach the pit burn through `PyrotechIgnition.tryIgnite` after their use duration, as in
  1.12. `igniter_brick` joins `#pyrotech:refractory` from ignition's datagen. The 1.12 item path
  guarded the helper with the module toggle (line 129) and the block path did not (line 49); both
  guards vanish.
- **Hunting sign-off** (issue #26): no ignition recipe consumes tannin; the ticket's "one recipe"
  is the `BlockFluidTannin::new` factory reference, which dies. The durable bow drill recipes
  consume `leather_durable_cord` and `leather_strap`.
- **Tool sign-off** (issue #24): the bow drills are plain `Item`s with `durability(n)`, not
  `TieredItem`s, so TOOL decision 1's tier constraint does not apply; TOOL decision 4 (tooltip
  unconditional) does. `#pyrotech:rocks` and the stone stick tag are shared inputs.
- **Progression skips** (issue #33): decision 2 answers its decision 6.
- **JEI plugin**: hide the spindle; no categories.

## Decisions for Moos

1. **Igniters and `FIRESTARTER_LIGHT`: do not report it.** Options: igniters answer
   `canPerformAction(FIRESTARTER_LIGHT)` so refractory's right-click listener starts pit burns for
   them (the REFRACTORY suggestion); or igniters stay off the ability and reach pit burns only
   through `finishUsingItem` branch 3. Facts: nothing in NF-SRC calls `canPerformAction` with that
   ability (TNT tests item identity, creepers a tag, candles and campfires are lit inside
   `FlintAndSteelItem#useOn`), so the only reader would be the refractory listener, which fires at
   click time. That would light a log pile the instant a bow drill touches it, skipping the 20 to
   80 tick use, the cooldown, and the durability cost, and the item's use would then run on top.
   Recommendation: stay off the ability. The 1.12 mechanic is preserved, and the listener still
   serves vanilla flint and steel and fire charges, which 1.12 allowed on log piles.
2. **Soul torch recipe: add a coal-only one.** Options: ignition's datagen emits
   `pyrotech:soul_torch_vanilla_coal` (coal, `#c:rods/wooden`, `#minecraft:soul_fire_base_blocks`,
   4 soul torches), mirroring core's `torch_vanilla_coal` exactly; or leave the soul torch without
   a recipe. Facts: 1.12 removed `minecraft:torch` only because vanilla accepts charcoal, and put
   back a coal-only copy (`torch_vanilla_coal.json`, `data: 0`); the vanilla soul torch
   (DATA `soul_torch.json`) differs from the torch only by the soul sand or soil under the stick,
   which is Nether-gated and skips nothing. Recommendation: add it, next to the fiber and stone
   torches in ignition's recipe method, or beside `torch_vanilla_coal` in core if Moos prefers the
   removal and the replacement in one place.
3. **Torch block shape: one block, five-way facing.** Options: one block per torch kind with
   `facing` over up and the four walls plus `type`, which is what the converted `torch_fiber.json`
   and `torch_stone.json` on `main` already key on; or vanilla's floor and wall pair with a
   `StandingAndWallBlockItem`, which needs two block entity types per kind, two loot tables, and
   regenerated blockstates. Recommendation: one block. Survival, placement, and shapes are a
   switch on `facing`, and the assets stay untouched.
4. **Powered igniter signal source: read it in the ticker.** Options: keep the 1.12 shape
   (`neighborChanged` stores a transient flag on the block entity, which is off after a chunk
   reload until a neighbour changes); read `hasSignal` from the ticker every tick; or a `powered`
   blockstate property, which needs hand-edited blockstates for a model that does not change.
   Recommendation: the ticker read. It is one cheap call per tick, behaves identically while the
   world is loaded, and removes the reload gap.
5. **Config surface: four toggles.** Options: keep `EXTINGUISHED_BY_RAIN` and `BURNS_UP` for each
   torch in the shared common config and bake the other seventeen knobs; or bake everything.
   Recommendation: the four toggles. They are behaviour switches players and pack makers argue
   about ("does the stone torch burn out"), which is what the issue #22 principle keeps. The
   tooltip reads them client side, faithful to 1.12's unsynced `@Config`.
6. **Rain-douse timestamp: reset it.** Facts: the empty-hand and water douse resets
   `lastTimeStamp` so the doused time is not charged against the torch's life (`TileTorchBase.java`,
   lines 184 to 188, with a comment saying so), but the rain douse in `randomUpdate` does not
   (lines 116 to 117), so a fiber torch relit after a storm loses the storm's length from its
   remaining burn time. Options: byte-faithful, or reset on every douse. Recommendation: reset on
   every douse. The comment shows the intent; the rain path is an omission, not a mechanic.
7. **Lamp fuel data: an ignition-owned fluid data map.** Options: `pyrotech:lamp_fuels` on
   `Registries.FLUID` with `{"millibuckets_per_minute": n}` registered by ignition
   (`DataMapType.builder`, NF-SRC `net/neoforged/neoforge/registries/datamaps/DataMapType.java`,
   line 85; read through `Holder#getData` from `FluidStack#getFluidHolder`, `IWithData.java`, line 23,
   `FluidStack.java`, line 237; file `data/pyrotech/data_maps/fluid/lamp_fuels.json` per NF-DOCS
   https://docs.neoforged.net/docs/1.21.1/resources/server/datamaps/); a third field on REFRACTORY's
   proposed `pyrotech:fluid_fuels` map; or two constants. Recommendation: the separate map. The
   entry doubles as the tank validator, datapacks can add lamp fuels as the 1.12 config invited,
   and it does not couple the lamp to refractory's map, which core would otherwise have to own.
   Ignition's `DataMapProvider` writes wine 12 and lamp oil 10 (NF-SRC
   `net/neoforged/neoforge/common/data/DataMapProvider.java`, lines 98 and 139).
8. **Lamp block id versus liquid block id.** Facts: the lamp is `pyrotech:lamp_oil` in 1.12 and
   every converted asset and lang key on `main` uses that id; REFRACTORY gives each liquid block the
   fluid's id, which would also be `lamp_oil`. Options: keep `lamp_oil` for the lamp and name the
   liquid block `pyrotech:lamp_oil_fluid` (fluid ids stay `lamp_oil` and `flowing_lamp_oil`); or
   rename the lamp to `oil_lamp`, its display name, and free `lamp_oil` for the liquid, at the cost
   of renaming the blockstate, the item model, the lang key, and the recipe result. Recommendation:
   keep `lamp_oil` for the lamp. The liquid block's id is only visible in F3 and commands, and the
   lamp's id is the faithful one.
