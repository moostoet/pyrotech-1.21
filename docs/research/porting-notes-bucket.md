# Porting notes: bucket module

Research for issue #13 (porting notes: bucket). The module has 12 Java files
and depends on no other module. It can be the first gameplay module to hoist.

All 1.12 facts come from the `1.12` branch,
`src/main/java/com/codetaylor/mc/pyrotech/modules/bucket/`.
All 1.21 facts come from two primary sources in the local Gradle cache:

- NeoForge 21.1.249 sources jar
  (`net.neoforged:neoforge:21.1.249:sources` in `~/.gradle/caches/modules-2`).
- The patched vanilla 1.21.1 sources that ModDevGradle builds against
  (`~/.gradle/caches/neoformruntime/intermediate_results/sourcesWithNeoForge_*.zip`).

Class names below refer to those sources.

## What the 1.12 module contains

| file | purpose |
|---|---|
| `ModuleBucket` | module shell, item holders, registers furnace recipes |
| `ModuleBucketConfig` | `@Config` annotations, 8 options per bucket tier |
| `item/ItemBucketBase` | all bucket behavior, extends Forge `UniversalBucket` |
| `item/ItemBucketWood/Clay/Stone/Refractory` | thin subclasses that read config |
| `item/ItemBucketClayUnfired`, `item/ItemBucketRefractoryUnfired` | plain items, stack size 1 |
| `init/ItemInitializer` | item and model registration |
| `init/VanillaFurnaceRecipesAdd` | smelting: unfired bucket to fired bucket, 0.1 xp |
| `event/ItemCraftedEventHandler` | takes 1 durability when a bucket is a crafting ingredient |

Assets: 4 Forge blockstate files that drive the dynamic filled-bucket model,
2 unfired item models, 4 crafting recipes, 11 textures, and Patchouli book
entries. The textures and the 2 unfired models are already migrated on `main`.
The 4 blockstate files were not migrated. They cannot be, see the model section.

## How the buckets behave in 1.12

One item per tier holds any fluid. The fluid lives in NBT. Milk is a metadata
variant, not a fluid. Each tier has a hidden "uses" counter in NBT. Draining,
drinking milk, and crafting each cost 1 use. At 0 uses the bucket breaks.

Every second, a filled bucket in an inventory takes the "full container" damage.
If the fluid temperature is at least 450, it also takes the "hot container"
damage, and the holder takes fire damage and ignites. When the bucket breaks
while filled, the fluid source block drops at the player's feet (config).
There is no fill restriction. A wooden bucket can scoop lava. It then destroys
itself within a second and dumps the lava on the player. That is the wooden
bucket's hot-fluid "restriction". Cow milking is allowed for every tier and is
config-gated (default on). A lava-filled bucket is furnace fuel (20000 ticks).
Buckets fill and drain water cauldrons, full levels only.

Config defaults per tier (`ModuleBucketConfig`):

| tier | uses | hot dmg/s | player dmg/s | full dmg/s | empty stack |
|---|---|---|---|---|---|
| wood | 8 | 8 | 2 | 1 | 1 |
| clay | 12 | 4 | 2 | 0 | 4 |
| stone | 16 | 4 | 2 | 0 | 4 |
| refractory | 24 | 0 | 1 | 0 | 4 |

Hot threshold is 450 for all tiers (lava is 1300, water is 300).

## Construct-by-construct mapping

### Base class and registration

| 1.12 | 1.21 |
|---|---|
| `UniversalBucket` base class | Gone. NeoForge 21.1 has no multi-fluid bucket item. Write a plain `Item` subclass and add fluid handling through the item capability. Vanilla `BucketItem` is one item per fluid and does not fit. |
| athenaeum `Registry` + `@GameRegistry.ObjectHolder` | `DeferredRegister.createItems("pyrotech")` with `DeferredItem` holders. |
| `@Config` annotations | `ModConfigSpec` built in a config class, registered with `ModContainer#registerConfig`. |
| `getSubItems` (creative tab entries) | `BuildCreativeModeTabContentsEvent`. Iterate `BuiltInRegistries.FLUID` for source fluids when the show-all option is on. |
| `ItemInitializer.onClientRegister` (model registration) | Not needed. Item models load by item id from `models/item/`. |

### Fluid storage: NBT to data components

1.12 stores the fluid in a `fluids` NBT tag and the uses counter in a
`durability` NBT tag. 1.21 replaces item NBT with data components.

- Register a `DataComponentType<SimpleFluidContent>` for the fluid.
  `SimpleFluidContent` is NeoForge's stock component for one `FluidStack`
  (`net.neoforged.neoforge.fluids.SimpleFluidContent`). Its javadoc says the
  mod must register the component type itself.
- Register a `DataComponentType<Integer>` for the uses counter.
- Data components register through
  `DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "pyrotech")`.

Stacks with different component values do not merge. That matches the 1.12
NBT behavior for partly used empty buckets.

### Fluid capability

1.12 overrides `initCapabilities` and returns a `FluidBucketWrapper` subclass.
1.21 registers capabilities externally:

- `RegisterCapabilitiesEvent#registerItem(Capabilities.FluidHandler.ITEM, provider, items...)`
  (`net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent`, line 136).
- The provider returns an `IFluidHandlerItem`.
- NeoForge's `FluidHandlerItemStack` template already stores the fluid in a
  `SimpleFluidContent` component. Its constructor takes the component type,
  the stack, and a capacity. Subclass it so `drain` also spends 1 use and
  swaps to the broken state at 0 uses. The 1.12 `BucketWrapper` did exactly
  this inside `setFluid`.
- Today's `FluidBucketWrapper` is only for vanilla-style buckets. Its
  `setFluid` swaps to `Items.BUCKET`, so it is not reusable here.

### Picking up fluid: FillBucketEvent is gone

1.12 fills the bucket through `FillBucketEvent` (`onFillBucket`). NeoForge
21.1 has removed that event. The `net.neoforged.neoforge.event.entity.player`
package has no bucket event and no `onBucketUse` hook remains in
`CommonHooks`.

Replacement: do everything inside `Item#use`.

- Ray trace with `getPlayerPOVHitResult` and `ClipContext.Fluid.SOURCE_ONLY`
  when the bucket is empty. Vanilla `BucketItem#use` shows the pattern.
- Pick up with `FluidUtil.tryPickUpFluid(emptyContainer, player, level, pos, side)`.
  It handles both `BucketPickup` blocks (all vanilla fluids) and fluid handler
  blocks, and it plays the fluid's fill sound.
- Copy the stack to count 1 before filling, as the 1.12 handler did, so a
  stack of empty clay buckets fills one at a time.
- Place with `FluidUtil.tryPlaceFluid(player, level, hand, pos, container, fluidStack)`.
  The method now takes the hand. The athenaeum `FluidUtilFix` workaround was
  for a Forge 1.12 bug and is obsolete.
- Both methods return a `FluidActionResult` with the resulting stack, same
  shape as 1.12. Spend 1 use on a successful drain, hand back the empty or
  broken result, and play `SoundEvents.ITEM_BREAK` on break.

### Cauldrons

1.12 hand-rolls water cauldron support (fill from full bucket, drain a full
cauldron). 1.21 has two mechanisms:

- NeoForge `CauldronFluidContent` registers the empty, water, and lava
  cauldrons as fluid handler blocks. `FluidUtil.tryPickUpFluid` and
  `tryPlaceFluid` against the cauldron position then work with no extra code.
  A full-bucket drain only succeeds on a level 3 water cauldron, which matches
  the 1.12 rule.
- Vanilla `CauldronInteraction` maps (`EMPTY`, `WATER`, `LAVA`) key handlers
  by item. This route needs one registration per bucket item and reproduces
  1.12 exactly, including the cauldron stats.

The capability route is less code and also covers lava cauldrons, which 1.12
did not. It skips the `FILL_CAULDRON` and `USE_CAULDRON` stats. See decisions.

### Per-second damage tick

`onUpdate` becomes `inventoryTick(ItemStack, Level, Entity, int, boolean)`.

- Temperature moved from `Fluid` to `FluidType`:
  `stack.getFluid().getFluidType().getTemperature(fluidStack)`
  (`FluidType` lines 189 and 677).
- Player damage: `entity.hurt(level.damageSources().inFire(), amount)`.
- Ignite: `entity.igniteForSeconds(1)` (renamed from `setFire`).
- The break-and-drop-source logic ports as is, using
  `FluidUtil.tryPlaceFluid` at the player position.

### Crafting, fuel, stack size, display

| 1.12 | 1.21 |
|---|---|
| `getContainerItem` + `ItemCraftedEventHandler` | `IItemExtension#getCraftingRemainingItem(ItemStack)` and `hasCraftingRemainingItem(ItemStack)` are stack-sensitive (lines 193 and 207). Return an empty copy with 1 use spent, or `ItemStack.EMPTY` when broken. Delete the event handler. |
| `getItemBurnTime` (lava = 20000) | `IItemExtension#getBurnTime(ItemStack, RecipeType<?>)` (line 619). The `neoforge:furnace_fuels` data map is item-keyed, so it cannot express "only when filled with lava". Keep the override. |
| `getItemStackLimit(stack)` | `IItemExtension#getMaxStackSize(ItemStack)` (line 421). 1 when filled, config value when empty. |
| `showDurabilityBar`, `getDurabilityForDisplay` | `isBarVisible`, `getBarWidth` (0 to 13), `getBarColor`. |
| `addInformation` | `appendHoverText(ItemStack, TooltipContext, List<Component>, TooltipFlag)` with `Component.translatable`. |
| `getItemStackDisplayName` | `getName(ItemStack)` returns a `Component`. Fluid name comes from `FluidStack#getHoverName`. Suggest one generic key per tier with a `%s` argument, plus explicit milk keys. |
| `shouldCauseReequipAnimation` | Same name on `IItemExtension` (line 524). Compare fluid components instead of metadata. |
| `EnumAction.DRINK`, `getMaxItemUseDuration` | `getUseAnimation` returns `UseAnim.DRINK`; `getUseDuration(ItemStack, LivingEntity)` returns 32 when it holds milk. |

### Milk

1.12 models milk as item metadata because 1.12 had no standard milk fluid.
NeoForge 21.1 ships an opt-in milk fluid:

- Call `NeoForgeMod.enableMilkFluid()` in the mod constructor
  (`NeoForgeMod` line 501). This registers `minecraft:milk` still and flowing
  fluids, a fluid type with bucket sounds, client textures, and the `c:milk`
  fluid tag. The vanilla milk bucket becomes its bucket item.
- With that enabled, milk is a normal `FluidStack` in the fluid component.
  The metadata variant, the special creative entries, and the milk branch in
  `createWithFluid` all disappear.
- Drinking: `finishUsingItem` calls
  `entityLiving.removeEffectsCuredBy(EffectCures.MILK)`. That is exactly what
  the patched `MilkBucketItem` does, so the port stays vanilla-consistent.
  The 1.12 `curePotionEffects(milk bucket)` call maps to this.
- Milk has no fluid block, so it cannot be placed in the world. The
  drop-source-on-break path does nothing for milk. Same as 1.12.

### Cow milking

Vanilla `Cow#mobInteract` only accepts `Items.BUCKET`. The 1.21.1 interaction
order in `Player#interactOn` is: `EntityInteract` event, then
`entity.interact` (the cow), then `ItemStack#interactLivingEntity`. The cow
passes on unknown items, so an override of `Item#interactLivingEntity` runs.
That method is the direct heir of 1.12 `itemInteractionForEntity`. Port the
existing logic there: check `Cow`, check the config gate, play
`SoundEvents.COW_MILK`, set the milk fluid component, keep the uses counter.
Note: vanilla refuses to milk baby cows, 1.12 Pyrotech did not check. Match
vanilla and skip babies.

### Wooden bucket restrictions, summarized

- Hot fluids: unchanged mechanic, now driven by `FluidType#getTemperature`.
  Still no fill restriction. The wooden bucket scoops lava, burns its holder,
  and destroys itself. All numbers stay config-driven.
- Milk: becomes a real fluid in the component instead of metadata. Drinking
  and stack-size behavior carry over.
- Cow milking: moves from `itemInteractionForEntity` to
  `interactLivingEntity`. The config gate carries over. Default stays on for
  every tier, as in 1.12.

### Models and assets

The 1.12 dynamic bucket model uses `forge_marker` blockstate files with the
`forge:forgebucket` model. That format does not exist in 1.21, which is why
the asset migration skipped those 4 files.

Replacement: one item model JSON per bucket item with NeoForge's dynamic
fluid container loader. Verified in `ClientNeoForgeMod` line 74 and
`DynamicFluidContainerModel.Loader`:

```json
{
  "loader": "neoforge:fluid_container",
  "fluid": "minecraft:empty",
  "textures": {
    "base": "pyrotech:item/wood_bucket_base",
    "fluid": "pyrotech:item/wood_bucket_base",
    "cover": "pyrotech:item/wood_bucket_cover"
  },
  "flip_gas": true
}
```

`fluid` is required. `flip_gas` defaults to false, `cover_is_mask` and
`apply_fluid_luminosity` default to true. The baked model swaps by the fluid
in the item's fluid capability, so the milk texture works automatically once
the milk fluid is enabled. The `fluid` texture acts as a mask over the fluid's
own still texture. These 4 model files are new static JSON, following the
static-assets decision.

### Recipes

All recipes move to Java datagen per the map decisions.

- Shaped recipes: wood bucket (planks + plant fibers), clay unfired
  (3 unfired bricks), stone (3 stone bricks + 3 clay balls), refractory
  unfired (3 unfired refractory bricks). Material metas resolve to: 12 =
  plant_fibers, 24 = unfired_brick, 16 = brick_stone, 9 =
  unfired_refractory_brick (core module `ItemMaterial`).
- The `forge:ore_dict` `plankWood` ingredient becomes the `#minecraft:planks`
  tag.
- The furnace recipes from `VanillaFurnaceRecipesAdd` become two datagen
  smelting recipes, 0.1 xp, replacing the code-side registration.
- The 1.12 `modules_enabled` recipe condition came from athenaeum. See
  decisions.

### Out of scope here

- Patchouli book entries ship with the module's assets but belong to the
  book port, not to this module.
- Other modules consume filled buckets in recipes. Matching a filled bucket
  as an ingredient needs NeoForge's `DataComponentIngredient` or a fluid
  ingredient. That lands with those modules.

## Decisions for Moos

1. Milk representation. Recommended: call `NeoForgeMod.enableMilkFluid()` and
   store milk as a normal fluid. Alternative: copy the 1.12 special-state
   approach. The alternative fights every 1.21 idiom above.
2. Cauldron route. Recommended: NeoForge's cauldron fluid capability, which
   is near-zero code and matches 1.12 for water. Side effects: lava cauldrons
   also work (new), and the two vanilla cauldron stats are not awarded.
   Alternative: explicit `CauldronInteraction` registrations for exact 1.12
   scope and stats.
3. Uses counter. Recommended: a custom integer data component, which keeps
   empty buckets stackable and leaves the vanilla damage system alone.
   Alternative: vanilla `max_damage`/`damage` components, which give the bar
   for free but force stack size 1 and allow Unbreaking.
4. Config surface. Keep all 8 options for each of the 4 tiers, or trim the
   list (for example `SHOW_ALL_BUCKETS`). Also confirm config type. Common
   config fits best: the tooltip and creative entries read it on the client.
5. Recipe gating. 1.12 wraps every recipe in a `modules_enabled` condition.
   Decide whether the port keeps per-module enable flags (NeoForge supports
   custom recipe conditions) or drops module gating.
6. Dispenser support. 1.12 had none. NeoForge's `DispenseFluidContainer`
   would add it in one registration per item. Faithful default: skip.
