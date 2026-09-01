# Porting notes: worldgen

Resolves issue #12 (porting notes: worldgen).
The worldgen module is 17 files and 1,903 lines.
It places rocks, ores, mud, and three berry plants during chunk generation.
This is the biggest idiom shift in the port.
The 1.12 module is imperative Java that runs per chunk.
The 1.21 replacement is data: configured features, placed features, and biome modifiers.
Decisions that need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo, package `com.codetaylor.mc.pyrotech.modules.worldgen`.
  The dense coal generator also uses `library/util/FloodFill.java`.
  The cube helpers come from athenaeum `util/BlockHelper.java`
  (github.com/codetaylor/athenaeum).
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths cited below are entries in that jar.
- **NF-DOCS**: docs.neoforged.net, 1.21.1 pages. URLs cited inline.
- **DATA**: the vanilla 1.21.1 data pack, at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`.
- **CORE**: `docs/research/porting-notes-core.md` on branch `research/porting-notes-core`.
- **DEPS**: `docs/research/module-dependencies.md` on branch `research/module-dependencies`.

## Summary

- `IWorldGenerator` and `GameRegistry.registerWorldGenerator` are gone.
  Every generator becomes a configured feature plus a placed feature plus a biome modifier.
- Four generators need a small custom `Feature` class in Java: dense coal, dense redstone,
  dense quartz, and mud. The other seven map onto vanilla feature types.
- The 649-line worldgen config dies. Placement values move into the placed feature JSON.
  Users tune them with a datapack instead of a config file.
- Dimension whitelists become biome tags. Dim 0 becomes `#minecraft:is_overworld`.
  Dim -1 becomes `#minecraft:is_nether`.
- The 1.12 biome name lists translate to 1.21 biome ids. Many 1.12 biomes no longer exist.
  The translation tables are below.
- Recommendation: generate all worldgen JSON with datagen through
  `DatapackBuiltinEntriesProvider`. See the proposal section.

## The 1.12 module at a glance

`ModuleWorldGen` registers one `WorldGenerator` dispatcher with
`GameRegistry.registerWorldGenerator(new WorldGenerator(), 1)`.
The dispatcher runs 11 generators on every new chunk, filtered per dimension:

| generator | places | where |
|---|---|---|
| `WorldGenFossil` | fossil ore veins | overworld, y 40 to 120 |
| `WorldGenLimestone` | limestone veins | overworld, y 8 to 100 |
| `WorldGenDenseCoal` | dense coal ore inside existing coal veins | overworld, y 0 to 32 |
| `WorldGenDenseNetherCoal` | dense nether coal veins in netherrack | nether, y 1 to 127 |
| `WorldGenRocks` | loose stone rocks on the surface | overworld, all biomes |
| `WorldGenDenseRedstoneOre` | dense redstone blocks on cave floors | overworld, y 5 to 25 |
| `WorldGenDenseQuartzOre` | dense quartz blocks on nether cave floors | nether, y 1 to 64 |
| `WorldGenPyroberryBush` | pyroberry bushes | overworld surface, desert |
| `WorldGenGloamberryBush` | gloamberry bushes | overworld surface, forest and taiga |
| `WorldGenFreckleberryPlant` | freckleberry plants | overworld surface, plains to savanna |
| `WorldGenMud` | mud and mud rocks at dirt shorelines | overworld, under and beside water |

All placed blocks belong to core. That matches DEPS: worldgen depends only on core.

## The 1.21 pipeline

1.21 worldgen is three data layers plus optional Java
(NF-SRC `net/minecraft/world/level/levelgen/feature/Feature.java`,
`net/minecraft/world/level/levelgen/placement/PlacedFeature.java`;
NF-DOCS https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier):

- A **configured feature** says what to place. JSON in
  `data/pyrotech/worldgen/configured_feature/`. It names a feature type and its config.
  Vanilla types cover ore veins and plant patches. Custom shapes need a Java `Feature`
  subclass with a config codec, registered under `Registries.FEATURE` with a
  `DeferredRegister` (NF-SRC `net/minecraft/core/registries/Registries.java`, line 149).
- A **placed feature** says where and how often. JSON in
  `data/pyrotech/worldgen/placed_feature/`. It references the configured feature and lists
  placement modifiers. The modifiers run in list order. The vanilla convention is
  count or rarity, then `in_square`, then a height rule, then `biome` (DATA
  `data/minecraft/worldgen/placed_feature/ore_coal_lower.json`).
- A **biome modifier** says which biomes get the placed feature and in which generation
  step. JSON in `data/pyrotech/neoforge/biome_modifier/`. Type `neoforge:add_features`
  with `biomes`, `features`, and `step` fields (NF-SRC
  `net/neoforged/neoforge/common/world/BiomeModifiers.java`, lines 29 to 61;
  registry key `neoforge:biome_modifier` in
  `net/neoforged/neoforge/registries/NeoForgeRegistries.java`, line 61).

Generation steps run in a fixed order: `underground_ores` before
`underground_decoration` before `vegetal_decoration`
(NF-SRC `net/minecraft/world/level/levelgen/GenerationStep.java`, lines 32 to 42).
Within one step, features added by a biome modifier run after the biome's vanilla
features, because the modifier appends to the step's list
(NF-SRC `BiomeModifiers.java`, `AddFeaturesBiomeModifier#modify`).
Every placed feature must be defined once and referenced by id.
Do not add vanilla placed features to extra biomes. That can cause a feature order
cycle error (warning in the `AddFeaturesBiomeModifier` javadoc).

## Cross-cutting construct table

| 1.12 construct | 1.21 replacement |
|---|---|
| `GameRegistry.registerWorldGenerator(new WorldGenerator(), 1)` | Nothing. The data files register themselves by existing in the datapack. |
| `WorldGenerator` dispatcher with per-dimension cache | Gone. Biome modifiers route each feature. |
| `IWorldGenFeature`, `isAllowed(dimensionId)` | Gone. The `biomes` field of the biome modifier gates placement. |
| `DIMENSION_WHITELIST = {0}` | `"biomes": "#minecraft:is_overworld"` (DATA `data/minecraft/tags/worldgen/biome/is_overworld.json`). |
| `DIMENSION_WHITELIST = {-1}` | `"biomes": "#minecraft:is_nether"` (five nether biomes in DATA). |
| `DIMENSION_BLACKLIST` | Gone. Datapack users edit the biome tag instead. |
| `ENABLED` config flags | Gone. Datapack users override the biome modifier file with an empty feature list. See decision 2. |
| `CHANCES_TO_SPAWN` (attempts per chunk) | `minecraft:count` placement modifier. Takes an int provider (NF-SRC `placement/CountPlacement.java`). |
| `CHANCE_TO_SPAWN` (fraction of chunks) | `minecraft:rarity_filter` with `chance` = 1/fraction. Placement runs at probability 1/chance (NF-SRC `placement/RarityFilter.java`, line 22). |
| `CLUSTER_FREQUENCY` | Same `rarity_filter` mapping. Rounding flagged in decision 5. |
| `MIN_HEIGHT`/`MAX_HEIGHT`, `VERTICAL_BOUNDS` | `minecraft:height_range` with `minecraft:uniform` between two `absolute` anchors (NF-SRC `placement/HeightRangePlacement.java`; DATA `ore_coal_lower.json`). |
| `world.getHeight(x, z)` surface lookup | `minecraft:heightmap` placement modifier. `WORLD_SURFACE_WG` for plants and rocks, `OCEAN_FLOOR_WG` for mud (NF-SRC `data/worldgen/placement/PlacementUtils.java`, lines 30 to 33). |
| `ALLOWED_BIOMES` string arrays in config | Mod-owned biome tags in `data/pyrotech/tags/worldgen/biome/`. See the biome translation tables. |
| `random.nextInt(16) + 8` chunk offset | `minecraft:in_square` (spreads 0 to 15). The +8 anti-cascading offset is a 1.12 workaround. The placed feature pipeline does not need it. |
| `IRandomIntSupplier` (min to max vein size) | `IntProvider` in codecs where a range survives. The vanilla ore config takes a fixed `size` int. See decision 4. |
| `WorldGenOre` (copy of vanilla `WorldGenMinable`) | The vanilla `minecraft:ore` feature. Same ellipsoid algorithm (NF-SRC `feature/OreFeature.java`, lines 35 to 90, against 1.12 `WorldGenOre.java`, lines 37 to 91). |
| `StonePredicate` (stone, granite, diorite, andesite) | Rule test `minecraft:tag_match` on `minecraft:stone_ore_replaceables`. The tag holds exactly those four blocks (DATA `data/minecraft/tags/block/stone_ore_replaceables.json`). |
| `BlockPredicate(Blocks.NETHERRACK)` | Rule test `minecraft:block_match` on netherrack. Vanilla uses the same rule test for nether ores (NF-SRC `data/worldgen/features/OreFeatures.java`, line 53). |
| `Random` | `RandomSource`, provided by `FeaturePlaceContext`. |
| `world.setBlockState(pos, state, 2 \| 16)` | `level.setBlock(pos, state, 2)` inside a feature. Vanilla features use flag 2 (NF-SRC `feature/SimpleBlockFeature.java`, line 29). |
| `@Config` classes (`ModuleWorldGenConfig`, 649 lines) | Dropped. Values live in the JSON. See decision 2. |

## Feature by feature

### Fossil ore

1.12: 15 veins per chunk, y 40 to 120, vein size 10 to 15, replaces natural stone,
places `ORE_FOSSIL` (flat id `pyrotech:fossil_ore` per the migrated blockstates on `main`).

1.21: vanilla `minecraft:ore` configured feature.
Target: `tag_match` on `minecraft:stone_ore_replaceables`. Size: 13 (see decision 4).
`discard_chance_on_air_exposure`: 0.0
(NF-SRC `feature/configurations/OreConfiguration.java`).
Placement: `count` 15, `in_square`, `height_range` uniform absolute 40 to 120, `biome`.
Biome modifier: `#minecraft:is_overworld`, step `underground_ores`.

### Limestone

Same shape as fossil. 15 veins per chunk, y 8 to 100, size 10 to 20,
places `pyrotech:limestone`.
1.21: `minecraft:ore`, same target tag, size 15.
Placement: `count` 15, `in_square`, `height_range` uniform absolute 8 to 100, `biome`.
Same biome modifier group as fossil.

The 1.12 y bands sit unchanged in the 1.21 world. The overworld now extends to y -64,
so the new deepslate layer gets no pyrotech ore. See decision 3.

### Dense coal

1.12: scans a 4 by 4 grid of columns per chunk between y 0 and 32. At the first coal ore
found in a column, it flood-fills through connected coal ore and converts 3 to 6 blocks
into `pyrotech:dense_coal_ore` (`FloodFill.java`, breadth-first with a visit limit).

No vanilla feature converts existing ore veins. This needs a custom `Feature`:

- Java class with a config codec carrying the scan band and the fill size as an
  `IntProvider`. Registered under `Registries.FEATURE`.
- The feature must run after vanilla coal. Vanilla coal generates in step
  `underground_ores` (DATA `data/minecraft/worldgen/biome/river.json`, feature list index 6).
  Add dense coal in step `underground_decoration`, which runs after.
- Placement: `count` 1 (the feature scans its own chunk internally), `biome`.

Vanilla coal now generates from y 0 to 192 with a peak at 96, plus an upper band
(DATA `placed_feature/ore_coal_lower.json`, `ore_coal_upper.json`).
The 1.12 scan band y 0 to 32 catches only the bottom of that. See decision 3.

### Dense nether coal

1.12: 30 veins per chunk in the nether, y 1 to 127, size 10 to 20, replaces netherrack
only, places `pyrotech:dense_nether_coal_ore`.

1.21: vanilla `minecraft:ore`. Target: `block_match` netherrack. Size 15.
Placement: `count` 30, `in_square`, `height_range` uniform absolute 1 to 127, `biome`.
The nether build limit is still 127, so the band is unchanged.
Biome modifier: `#minecraft:is_nether`, step `underground_ores`.
Vanilla quartz placement is the JSON precedent (DATA `placed_feature/ore_quartz_nether.json`).

### Rocks

1.12: the signature early-game feature. 4 attempts per chunk, no biome restriction.
Each attempt takes the surface height and scans a 9 by 9 by 9 cube (range 4 in
`BlockHelper.forBlocksInCube`). Every air block with solid ground below
(material GROUND, GRASS, or ROCK) gets `pyrotech:rock_stone` at 6.25% chance.
Expected yield: about 5 rocks per attempt on open ground, so about 20 per chunk.

1.21: vanilla `minecraft:random_patch` wrapping `minecraft:simple_block`
(NF-SRC `feature/configurations/RandomPatchConfiguration.java`;
DATA `configured_feature/patch_berry_bush.json` as the JSON precedent).
`SimpleBlockFeature` only places where the state's `canSurvive` holds
(NF-SRC `feature/SimpleBlockFeature.java`, line 21).
So the ground rule moves onto core's rock block: `canSurvive` checks a block tag,
proposal `#pyrotech:rock_placeable_on` holding dirt, grass block, and the stone family.
1.12 materials are gone in 1.21; tags are the replacement.
Config: `tries` about 45, `xz_spread` 4, `y_spread` 4, tuned to match the 1.12 yield.
Placement: `count` 4, `in_square`, `heightmap` `WORLD_SURFACE_WG`, `biome`.
Biome modifier: `#minecraft:is_overworld`, step `vegetal_decoration`.
The random spread differs slightly from the 1.12 cube scan. See decision 7.

Note: 1.12 samples the surface height at the chunk corner, not at the random position.
On hills the cube can miss the surface and place nothing.
The 1.21 heightmap modifier samples at the actual position. This fixes that quirk,
which slightly raises rock counts in hilly terrain.

### Dense redstone

1.12: 25% of overworld chunks. Picks one spot at y 5 to 25. On cave floors within a
9 by 9 by 9 cube it places 1 to 3 large, then 3 to 5 small, then 6 to 8 rocks variants
of dense redstone (`pyrotech:dense_redstone_ore_large` and friends).
Under each, at 50%, it converts stone into vanilla redstone ore.
Small and rocks variants center on where the last large block landed.

No vanilla feature places block clusters on cave floors this way. Custom `Feature`:

- Config codec carrying the three count ranges and the ore-below chance.
- Floor test: the 1.12 material ROCK check becomes a `#minecraft:base_stone_overworld`
  tag check. Between y 0 and 8 the floor can be deepslate. When converting the block
  below, map stone to redstone ore and deepslate to deepslate redstone ore.
- Placement: `rarity_filter` chance 4 (exactly 25%), `count` 1, `in_square`,
  `height_range` uniform absolute 5 to 25, `biome`.
- Biome modifier: `#minecraft:is_overworld`, step `underground_decoration`.

### Dense quartz

Same pattern as dense redstone, in the nether. 12.5% of chunks, up to 20 attempts to
find a valid cluster spot, y 1 to 64, floors of netherrack, quartz ore below at 50%.
Custom `Feature` sharing code with dense redstone (one class, two configured instances
with different targets is the clean shape).
Placement: `rarity_filter` chance 8 (exactly 12.5%), `count` 1, `in_square`,
`height_range` uniform absolute 1 to 64, `biome`.
Biome modifier: `#minecraft:is_nether`, step `underground_decoration`.

### Pyroberry bush

1.12: desert only. 6% of chunks fire a cluster. Then 4 attempts, each scanning the
9 by 9 by 9 surface cube, placing bushes at 3% per position with age 4 to 6.

1.21: `minecraft:random_patch` of `minecraft:simple_block`.
The age roll becomes a `minecraft:randomized_int_state_provider` over a
`simple_state_provider`, property `age`, values uniform 4 to 6
(NF-SRC `feature/stateproviders/RandomizedIntStateProvider.java`, lines 18 to 22).
Ground rule via the bush block's `canSurvive`, as with rocks.
Config: `tries` about 22, `xz_spread` 4, `y_spread` 4 (about 2.4 expected bushes per
attempt in 1.12; tune in game).
Placement: `rarity_filter` 17, `count` 4, `in_square`, `heightmap` `WORLD_SURFACE_WG`,
`biome`. Vanilla sweet berry placement is the precedent
(DATA `placed_feature/patch_berry_rare.json`).
Biome modifier: `#pyrotech:has_pyroberry_bush`, step `vegetal_decoration`.

### Gloamberry bush

Same mechanics as pyroberry. Forest and taiga biomes, 6% cluster chance, density 3%,
age 4 to 6. Same 1.21 shape.
Biome modifier: `#pyrotech:has_gloamberry_bush`, step `vegetal_decoration`.

### Freckleberry plant

Same mechanics, wider biomes, 7.5% cluster chance, density 10%.
Age: 10% ripe (age 7), else uniform 2 to 6.
That split maps exactly onto `minecraft:weighted_state_provider`
(NF-SRC `feature/stateproviders/WeightedStateProvider.java`):
age 7 at weight 5, ages 2 through 6 at weight 9 each, total 50, so ripe is 5/50 = 10%.
Config: `tries` about 45, `xz_spread` 4, `y_spread` 4.
Placement: `rarity_filter` 13, `count` 4, `in_square`, `heightmap` `WORLD_SURFACE_WG`,
`biome`.
Biome modifier: `#pyrotech:has_freckleberry_plant`, step `vegetal_decoration`.

### Mud

1.12: 8 attempts per chunk. Each scans a column top down for dirt with water directly
above (a shoreline or lakebed). Around a hit, in a sphere of radius 2 to 4:

- dirt touching water (above or sideways) becomes `pyrotech:mud`,
- air with solid ground below becomes `pyrotech:rock_mud` at 75%,
- otherwise, when the sky is hidden, the ground becomes mud.

The closest vanilla feature is `minecraft:disk` (clay disks under water, DATA
`configured_feature/disk_clay.json`). It covers the dirt-to-mud part but not the
mud rocks or the sky check. Recommendation: custom `Feature` porting the 1.12 logic,
about 60 lines. See decision 8.
Placement: `count` 8, `in_square`, `heightmap` `OCEAN_FLOOR_WG`,
`block_predicate_filter` on water (the disk placement precedent, DATA
`placed_feature/disk_clay.json`), `biome`.
Biome modifier: `#minecraft:is_overworld`, step `underground_ores`
(the step vanilla disks use, DATA `biome/river.json`).

## Biome translation tables

The 1.12 config lists 1.12 biome ids. Many were renamed in 1.13 and removed in 1.18
(the hills and mutated variants). The faithful mapping:

Pyroberry: `minecraft:desert` stays `minecraft:desert`.

Gloamberry:

| 1.12 id | 1.21 id |
|---|---|
| forest | forest |
| forest_hills | removed, folds into forest |
| birch_forest | birch_forest |
| birch_forest_hills | removed |
| taiga | taiga |
| taiga_hills | removed |
| redwood_taiga | old_growth_pine_taiga |
| redwood_taiga_hills | removed |
| mutated_forest | flower_forest |
| mutated_taiga | removed |
| mutated_birch_forest | old_growth_birch_forest |
| mutated_birch_forest_hills | removed |
| mutated_roofed_forest | removed, folds into dark_forest |
| mutated_redwood_taiga | old_growth_spruce_taiga |
| mutated_redwood_taiga_hills | removed |

Proposed `#pyrotech:has_gloamberry_bush`: forest, flower_forest, birch_forest,
old_growth_birch_forest, dark_forest, taiga, old_growth_pine_taiga,
old_growth_spruce_taiga.
Note: 1.12 listed mutated_roofed_forest but not roofed_forest itself.
Including dark_forest is the closest surviving reading.

Freckleberry: the same renames apply. 1.12 additionally lists plains, mutated_plains
(now sunflower_plains), roofed_forest (now dark_forest), savanna, savanna_rock
(now savanna_plateau), mutated_savanna (now windswept_savanna), and
mutated_savanna_rock (removed).
Proposed `#pyrotech:has_freckleberry_plant`: plains, sunflower_plains, forest,
flower_forest, birch_forest, old_growth_birch_forest, dark_forest, taiga,
old_growth_pine_taiga, old_growth_spruce_taiga, savanna, savanna_plateau,
windswept_savanna.

The vanilla tags `#minecraft:is_forest` and `#minecraft:is_taiga` are close but not
faithful. They add grove and snowy_taiga, which have no 1.12 counterpart in these lists
(DATA `data/minecraft/tags/worldgen/biome/is_forest.json`, `is_taiga.json`).
See decision 6.

## What stays Java

- One `DeferredRegister<Feature<?>>` with three feature types: the dense coal
  flood-fill, the cave-floor cluster (shared by dense redstone and dense quartz),
  and mud. Each is a `Feature<C>` subclass with a config codec and a
  `place(FeaturePlaceContext)` override (NF-SRC `feature/Feature.java`, lines 165, 194).
- The datagen bootstrap (next section).
- Nothing else. `ModuleWorldGen`, the dispatcher, the spi package, `WorldGenOre`,
  and the config class all disappear.

The module lands as roughly 4 Java classes plus data, down from 17 classes.

## Datagen proposal

Issue #1 leaves the worldgen JSON source open: datagen or static files.
Proposal: datagen, through the template's existing `data` run.

- `DatapackBuiltinEntriesProvider` takes a `RegistrySetBuilder` with bootstraps for
  `Registries.CONFIGURED_FEATURE`, `Registries.PLACED_FEATURE`, and
  `NeoForgeRegistries.Keys.BIOME_MODIFIERS`, and writes the JSON into
  `src/generated/resources`
  (NF-SRC `net/neoforged/neoforge/common/data/DatapackBuiltinEntriesProvider.java`;
  NF-DOCS https://docs.neoforged.net/docs/1.21.1/concepts/registries/ and
  https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier).
- The biome tags come from a `TagsProvider<Biome>` in the same datagen pass.

Why datagen and not static files:

- The output is about 30 cross-referencing files (11 configured features, 11 placed
  features, about 7 biome modifiers, 3 biome tags, 1 block tag).
  Placed features reference configured features by id. Biome modifiers reference
  placed features by id. Datagen resolves these through holder getters and fails the
  build on a typo. Static JSON fails silently at world load.
- The configured features embed block states of pyrotech blocks. In datagen those are
  code references to the block fields. In static JSON they are strings.
- Recipes and loot already run through datagen per issue #1. One pipeline is simpler.

Static JSON remains the answer for the migrated 1.12 assets. This proposal only covers
the worldgen data, which is new, not migrated. Flagged as decision 1.

## Biome modifier layout

Group placed features that share a biome set and step. Proposal, 7 files:

| file | biomes | step | features |
|---|---|---|---|
| `add_overworld_ores` | `#minecraft:is_overworld` | underground_ores | fossil_ore, limestone |
| `add_mud` | `#minecraft:is_overworld` | underground_ores | mud |
| `add_dense_overworld` | `#minecraft:is_overworld` | underground_decoration | dense_coal, dense_redstone |
| `add_nether_ores` | `#minecraft:is_nether` | underground_ores | dense_nether_coal |
| `add_dense_nether` | `#minecraft:is_nether` | underground_decoration | dense_quartz |
| `add_rocks` | `#minecraft:is_overworld` | vegetal_decoration | rocks |
| `add_plants` | per-plant pyrotech tags | vegetal_decoration | one modifier file per plant, or three files here |

## Decisions for Moos

1. **Worldgen JSON source.** Datagen through `DatapackBuiltinEntriesProvider`
   (recommended) or hand-written static JSON. The proposal section has the reasons.
2. **Config surface.** The 1.12 module has 649 lines of config: toggles, densities,
   heights, biome lists. Recommendation: drop all of it. Every value lives in JSON,
   and datapacks override JSON per file. Keeping runtime toggles would need NeoForge
   conditions on the generated entries and one config flag per feature. Only worth it
   if you want server owners to toggle worldgen without writing a datapack.
3. **Height bands in the taller world.** Recommendation: keep the 1.12 absolute y
   values everywhere (faithful). Consequences to accept: no pyrotech ore in the
   deepslate layer below y 0, and dense coal only converts veins in y 0 to 32 while
   vanilla coal now reaches y 192. Alternative: stretch the bands to the new world
   shape. That changes gameplay balance and is not faithful.
4. **Vein size ranges.** The vanilla ore feature takes one fixed `size`.
   1.12 rolls a range per vein. Recommendation: fixed midpoints
   (fossil 13, limestone 15, dense nether coal 15). The visual difference is noise.
   Alternative: a custom ore feature subclass that keeps the range. Not worth a class.
5. **Rarity rounding.** `rarity_filter` takes an integer chance.
   0.25 and 0.125 map exactly to 4 and 8. The plant frequencies do not:
   0.06 becomes 1/17 (0.0588) and 0.075 becomes 1/13 (0.0769).
   Recommendation: accept 17 and 13.
6. **Biome tags.** Mod-owned tags with the explicit translated lists (recommended,
   faithful) or vanilla `is_forest`/`is_taiga` tags (simpler, adds grove and
   snowy_taiga). Related: whether biomes new since 1.12 (meadow, cherry_grove,
   windswept and snowy variants) should host the plants. Recommendation: not now.
   The tags are datapack-extensible, so pack makers can add them.
7. **Rock density fidelity.** `random_patch` with tuned tries (recommended) or an
   exact custom feature port of the 1.12 cube scan. Rocks gate all early progression,
   so verify the tuned values in game against a 1.12 world before calling it done.
8. **Mud shape.** Custom feature porting the full 1.12 logic (recommended) or an
   approximation with a vanilla mud disk plus a mud-rock patch. The approximation
   loses the mud rocks near water and the under-cover mud. Mud rocks are a gathered
   early resource, so the loss is visible in play.
