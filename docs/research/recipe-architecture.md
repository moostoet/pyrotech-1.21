# Recipe architecture: types, codecs, datagen

Research for issue #9 (recipe architecture). Research date: 2026-09-01.

Sources. The 1.12 facts come from the recipe classes on the `1.12` branch.
The 1.21 facts come from the decompiled Minecraft 1.21.1 and NeoForge 21.1.249
sources in the local Gradle cache, and from the
[NeoForge 1.21.1 recipe docs](https://docs.neoforged.net/docs/1.21.1/resources/server/recipes/).
Class names below refer to those sources.

## Proposal in one paragraph

Each machine gets a vanilla `RecipeType` and a `RecipeSerializer`, registered
with two `DeferredRegister`s. Machines that share a field set share one recipe
class, following the vanilla `SingleItemRecipe` pattern where the type and
serializer are constructor arguments. Each serializer holds a `MapCodec` built
with `RecordCodecBuilder` and a `StreamCodec` for network sync. One
`RecipeProvider` subclass generates all recipe JSON into
`src/generated/resources/data/pyrotech/recipe/`. The 1.12 tier inheritance
(pit kiln to stone kiln to brick kiln) becomes a datagen-time transform.

## What the 1.12 machines actually store

Every 1.12 recipe is a plain Java object in a Forge registry. There is no JSON.
Lookup iterates the registry and calls `matches`. The field sets are:

| Machine | 1.12 class | Fields beyond input and output |
|---|---|---|
| Pit kiln | `KilnPitRecipe` | burn time, failure chance, failure items (plain list) |
| Stone and brick kiln | `MachineRecipeBaseKiln` | same three fields as pit kiln |
| Chopping block | `ChoppingBlockRecipe` | chops per axe level (int array), output quantity per axe level (int array), both defaulting from config |
| Drying rack, crude rack | `DryingRackRecipeBase` | dry time |
| Anvil | `AnvilRecipe` | hits, tool type (hammer or pickaxe), allowed anvil tiers (granite, ironclad, obsidian) |
| Soaking pot | `SoakingPotRecipe` | input fluid with amount, campfire required flag, time |
| Stone and brick crucible | `MachineRecipeItemInFluidOutBase` | fluid output with amount, time |
| Bloomery, wither forge | `BloomeryRecipeBase` | burn time, experience, failure chance, weighted failure items, bloom yield min and max, slag item and count, allowed anvil tiers, optional lang key |

File paths on the `1.12` branch, all under
`src/main/java/com/codetaylor/mc/pyrotech/`:

- `modules/tech/basic/recipe/KilnPitRecipe.java`
- `modules/tech/basic/recipe/ChoppingBlockRecipe.java`
- `modules/tech/basic/recipe/spi/DryingRackRecipeBase.java`
- `modules/tech/basic/recipe/AnvilRecipe.java`
- `modules/tech/basic/recipe/SoakingPotRecipe.java`
- `modules/tech/machine/recipe/spi/` (the machine recipe base classes)
- `modules/tech/bloomery/recipe/BloomeryRecipeBase.java`

Three behaviors sit next to the fields and matter for the port:

- Anvil matching needs context beyond the input stack. `AnvilRecipe#matches`
  takes the anvil tier and the tool type.
- Soaking pot matching needs the fluid in the tank. `SoakingPotRecipe#matches`
  takes an item and a `FluidStack`.
- Several machines multiply the recipe time by a config value at query time,
  for example `BASE_RECIPE_DURATION_MODIFIER` in `KilnPitRecipe#getTimeTicks`.

### Tier inheritance in 1.12

Higher tiers copy lower-tier recipes at registration time, with config
modifiers. `StoneKilnRecipesAdd#registerInheritedRecipes` copies every pit kiln
recipe into the stone kiln registry and scales time and failure chance.
`BrickKilnRecipesAdd` does the same from stone to brick. The brick crucible and
the ironclad and obsidian anvils follow the same pattern. The copy runs only
when a config flag is on
(`modules/tech/machine/init/recipe/BrickKilnRecipesAdd.java`).

### The bloom anvil special case

`BloomAnvilRecipe` extends `AnvilRecipe` and is generated at runtime, one per
bloomery recipe. It matches a bloom item by reading a `recipeId` string from
the bloom's NBT and comparing it to its own registry name
(`modules/tech/bloomery/recipe/BloomAnvilRecipe.java`). This is dynamic recipe
registration, which the 1.21 data-driven recipe system does not support.

## The 1.21 recipe system

Facts from the decompiled 1.21.1 sources:

- `Recipe<T extends RecipeInput>` is the core interface. It requires
  `matches`, `assemble`, `canCraftInDimensions`, `getResultItem`,
  `getSerializer`, and `getType` (`net.minecraft.world.item.crafting.Recipe`).
- `RecipeInput` is a tiny interface: `getItem(int)` and `size()`. A custom
  input is any record that implements it and may carry extra context fields.
  Vanilla `SingleRecipeInput` wraps one stack.
- `RecipeSerializer<T>` requires two things: `MapCodec<T> codec()` for JSON
  and `StreamCodec<RegistryFriendlyByteBuf, T> streamCodec()` for network
  sync. Both are mandatory. The server syncs all recipes to the client through
  the stream codec, and JEI reads the synced copy.
- `RecipeManager` loads JSON from `data/<namespace>/recipe/` (singular). It
  parses through NeoForge's conditional codec, so a recipe whose conditions
  fail is skipped. Lookup is `getRecipeFor(type, input, level)`.
  `RecipeManager.createCheck(type)` returns a cached check that remembers the
  last matched recipe. Block entities should hold one, like the vanilla
  furnace does.
- Recipe ids are file paths. `pyrotech:pit_kiln/brick` loads from
  `data/pyrotech/recipe/pit_kiln/brick.json`.

Registration on NeoForge uses two `DeferredRegister`s, one on
`Registries.RECIPE_TYPE` and one on `Registries.RECIPE_SERIALIZER`
([NeoForge recipe docs](https://docs.neoforged.net/docs/1.21.1/resources/server/recipes/)).

## Proposed recipe classes

One class per field set, not one per machine. The type and serializer are
constructor arguments, exactly like vanilla `SingleItemRecipe`, so one class
serves several `RecipeType`s. Package: `com.moostoet.pyrotech.recipe`.

| Class | Recipe types served | Input type |
|---|---|---|
| `KilnRecipe` | `pyrotech:pit_kiln`, `pyrotech:stone_kiln`, `pyrotech:brick_kiln` | `SingleRecipeInput` |
| `DryingRecipe` | `pyrotech:crude_drying_rack`, `pyrotech:drying_rack` | `SingleRecipeInput` |
| `ChoppingBlockRecipe` | `pyrotech:chopping_block` | `SingleRecipeInput` |
| `AnvilRecipe` | `pyrotech:anvil` | custom `AnvilRecipeInput` |
| `SoakingPotRecipe` | `pyrotech:soaking_pot` | custom `SoakingPotRecipeInput` |
| `CrucibleRecipe` | `pyrotech:stone_crucible`, `pyrotech:brick_crucible` | `SingleRecipeInput` |
| `BloomeryRecipe` | `pyrotech:bloomery`, `pyrotech:wither_forge` | `SingleRecipeInput` |

That is 12 recipe types and 12 serializer instances from 7 classes. Later
machines (campfire, ovens, sawmills, compacting bins, barrel, compost bin,
tanning rack, burn pit) reuse these shapes or add their own the same way.

Details per class:

- `KilnRecipe`: `Ingredient input`, `ItemStack result`, `int burnTime`,
  `float failureChance`, `List<ItemStack> failureItems`. The pit kiln and the
  machine kilns have identical fields in 1.12, so one class covers all three.
- `DryingRecipe`: `Ingredient input`, `ItemStack result`, `int dryTime`.
- `ChoppingBlockRecipe`: `Ingredient input`, `ItemStack result`, plus two
  optional int lists (chops and quantity per axe level). When a list is
  absent, the block entity falls back to the config defaults, which preserves
  the 1.12 config behavior.
- `AnvilRecipe`: `Ingredient input`, `ItemStack result`, `int hits`,
  `ToolType tool` (hammer or pickaxe), `Set<AnvilTier> tiers`. Both enums
  implement `StringRepresentable` for the codec.
  `AnvilRecipeInput(ItemStack item, AnvilTier tier, @Nullable ToolType tool)`
  carries the matching context that 1.12 passed as extra arguments.
- `SoakingPotRecipe`: `Ingredient inputItem`,
  `SizedFluidIngredient inputFluid`, `ItemStack result`,
  `boolean campfireRequired`, `int time`.
  `SoakingPotRecipeInput(ItemStack item, FluidStack fluid)`.
  `SizedFluidIngredient` is NeoForge's standard fluid-plus-amount ingredient
  (`net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient`). It also
  allows fluid tags, a superset of the 1.12 exact-fluid match.
- `CrucibleRecipe`: `Ingredient input`, `FluidStack result`, `int meltTime`.
  `assemble` and `getResultItem` return `ItemStack.EMPTY`; the block entity
  and the JEI category read the fluid through a dedicated accessor. This is
  the normal shape for fluid-output recipes.
- `BloomeryRecipe`: `Ingredient input`, `ItemStack result`, `int burnTime`,
  `float experience`, `float failureChance`, `int bloomYieldMin`,
  `int bloomYieldMax`, `int slagCount`, `ItemStack slagItem`,
  `List<FailureItem> failureItems` (a small record of stack and weight),
  `Set<AnvilTier> anvilTiers`, `Optional<String> langKey`.

Runtime config modifiers (the duration multipliers) stay out of the recipe
JSON. The block entity applies them when it reads the recipe, same as 1.12.

## Codec and serializer approach

Each serializer follows the vanilla two-codec pattern
(`SingleItemRecipe.Serializer` and `SimpleCookingSerializer` are the models):

- JSON: `RecordCodecBuilder.mapCodec` with a factory method reference.
  A `RecordCodecBuilder` group takes up to 16 fields, so even the bloomery
  (12 fields) fits in one group.
- Network: `StreamCodec.composite` for classes with up to 6 fields
  (the vanilla limit; NeoForge adds a 7-field overload in
  `NeoForgeStreamCodecs`). The bloomery serializer instead uses
  `StreamCodec.of(writer, reader)` with explicit field writes, the same
  approach vanilla takes in `SimpleCookingSerializer`.

Building blocks, all verified in the 1.21.1 and NeoForge 21.1.249 sources:

| Field kind | Codec | Stream codec |
|---|---|---|
| Item input | `Ingredient.CODEC_NONEMPTY` | `Ingredient.CONTENTS_STREAM_CODEC` |
| Item result | `ItemStack.STRICT_CODEC` | `ItemStack.STREAM_CODEC` |
| Fluid input | `SizedFluidIngredient.FLAT_CODEC` | `SizedFluidIngredient.STREAM_CODEC` |
| Fluid result | `FluidStack.CODEC` | `FluidStack.STREAM_CODEC` |
| Failure item list | `ItemStack.STRICT_CODEC.listOf().optionalFieldOf(..., List.of())` | `ItemStack.STREAM_CODEC` via `ByteBufCodecs.collection` |
| Enums | `StringRepresentable.fromEnum` | `ByteBufCodecs` enum idiom |
| Numbers | `Codec.INT`, `Codec.FLOAT` with `optionalFieldOf` defaults | `ByteBufCodecs.VAR_INT`, `FLOAT` |

A generated pit kiln recipe would look like:

```json
{
  "type": "pyrotech:pit_kiln",
  "ingredient": { "item": "pyrotech:unfired_brick" },
  "result": { "id": "minecraft:brick" },
  "burn_time": 12000,
  "failure_chance": 0.1,
  "failure_items": [
    { "id": "pyrotech:pit_ash" },
    { "id": "pyrotech:pottery_shard" }
  ]
}
```

Registration lives in two holder classes in the recipe package:
`PyrotechRecipeTypes` (a `DeferredRegister<RecipeType<?>>`) and
`PyrotechRecipeSerializers` (a `DeferredRegister<RecipeSerializer<?>>`), both
registered to the mod bus in the `Pyrotech` constructor.

Block entities query recipes through one cached
`RecipeManager.CachedCheck` per block entity, created with
`RecipeManager.createCheck(type)`.

## Datagen layout

The template already has the hook: `PyrotechDatagen` subscribes to
`GatherDataEvent` (`src/main/java/com/moostoet/pyrotech/datagen/PyrotechDatagen.java`),
and the `data` run writes to `src/generated/resources`.

Proposed layout under `com.moostoet.pyrotech.datagen`:

```
datagen/
  PyrotechDatagen.java            (exists; wires the providers)
  PyrotechRecipeProvider.java     (extends RecipeProvider)
  PyrotechLootTableProvider.java  (vanilla LootTableProvider with sub providers)
  recipe/
    TechBasicRecipes.java         (pit kiln, chopping block, drying racks, soaking pot, anvil, ...)
    TechMachineRecipes.java       (stone and brick kiln, crucibles, ...)
    TechBloomeryRecipes.java      (bloomery, wither forge)
  loot/
    PyrotechBlockLoot.java        (extends BlockLootSubProvider)
```

Wiring in `PyrotechDatagen#gatherData`:

```java
DataGenerator generator = event.getGenerator();
PackOutput output = generator.getPackOutput();
var lookup = event.getLookupProvider();
generator.addProvider(event.includeServer(), new PyrotechRecipeProvider(output, lookup));
generator.addProvider(event.includeServer(), new PyrotechLootTableProvider(output, lookup));
```

`PyrotechRecipeProvider` overrides `buildRecipes(RecipeOutput)` and delegates
to one static class per hoisting unit, with one static method per machine.
This mirrors the 1.12 `init/recipe/*RecipesAdd` classes, so porting a machine
means translating one file into one method. Vanilla-style `RecipeBuilder`
subclasses are not needed. The methods construct recipe objects directly and
call `RecipeOutput#accept(id, recipe, null)`. The advancement argument stays
null because the machines are not in the vanilla recipe book.

Recipe ids are `pyrotech:<machine>/<name>`, for example
`pyrotech:pit_kiln/brick`, which lands at
`data/pyrotech/recipe/pit_kiln/brick.json`. This mirrors the per-machine
registries of 1.12 and groups the generated files by machine.

Tier inheritance becomes a datagen transform. The stone kiln method first
emits its own recipes, then maps every pit kiln definition through a transform
that scales time and failure chance by the 1.12 default modifiers, and emits
the result under `pyrotech:stone_kiln/...`. Brick from stone works the same.
The shared definitions live in plain Java lists inside the datagen classes,
so the transform is a `stream().map()` at generation time.

Loot tables follow the vanilla provider chain: `LootTableProvider` takes
`SubProviderEntry` records, and `PyrotechBlockLoot` extends
`BlockLootSubProvider`. Output lands in `data/pyrotech/loot_table/`. The dropt
replacement (loot modifiers) is separate work and not part of this ticket.

One note for later tickets: the 1.12 `VanillaFurnaceRecipesRemove` and
`VanillaCraftingRecipesRemove` behavior maps to overriding the vanilla recipe
id with a JSON whose NeoForge condition is never met. `RecipeManager` skips
recipes with failing conditions, which removes the vanilla recipe.

## Decisions for Moos

1. **Tier inheritance is baked at datagen time.** In 1.12, config flags and
   modifiers control inheritance at runtime. The proposal bakes the default
   modifiers into the generated JSON. Players who want different values edit a
   datapack instead of a config. The alternative keeps one shared recipe type
   per machine family and scales time in the block entity by config. That
   keeps the config knobs but breaks the one-type-per-machine JEI mapping.
   Recommendation: bake it.
2. **Chopping block axe levels need a replacement.** 1.12 indexes the chops
   and quantity arrays by the axe's numeric harvest level. Numeric tool levels
   no longer exist in 1.21.1. Proposal: item tags
   `pyrotech:axe_level_0` through `pyrotech:axe_level_4`, assigned in datagen,
   read by the chopping block block entity. The alternative is a NeoForge
   data map on items. Recommendation: item tags.
3. **The bloom anvil drops its dynamic recipe class.** 1.12 registers one
   `BloomAnvilRecipe` per bloomery recipe at runtime. The 1.21 recipe system
   cannot register recipes dynamically. Proposal: the anvil block entity
   recognizes a bloom item, reads the bloomery recipe id from a
   `pyrotech:bloom` data component, and resolves it with
   `RecipeManager#byKey`. No bloom anvil recipe type exists. The alternative
   emits one bloom anvil JSON per bloomery recipe at datagen time.
   Recommendation: direct lookup in the block entity.
4. **Which runtime config knobs survive.** The duration multipliers
   (`BASE_RECIPE_DURATION_MODIFIER` per machine) and the chopping block
   defaults are runtime config in 1.12. The proposal keeps them as NeoForge
   config values applied in the block entities, on top of the generated
   recipe values. Dropping them would simplify the block entities but change
   moddability compared to 1.12. Recommendation: keep them.
