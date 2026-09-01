# Post-1.12 progression skips: what vanilla added that the 1.12 lists never covered

Resolves issue #32 (post-1.12 vanilla progression skips).
Pyrotech 1.12 guards its early game in two ways. It removes vanilla recipes by id from two config
lists, and it strips crafting tables and furnaces out of every new chunk. Both lists were written
against Minecraft 1.12.2. Vanilla has since added recipes, stations, and structure blocks that
those lists never saw. This note finds every one of them that lets a player past a Pyrotech gate
without the Pyrotech route, confirms how the port drops a vanilla recipe with a datapack override,
and ends with the override files the port should ship. Decisions that need Moos are collected at
the end.

## Sources

- **1.12**: the `1.12` branch of this repo. `ModuleCoreConfig`, `ModuleCore`, and
  `init/recipe/` under `src/main/java/com/codetaylor/mc/pyrotech/modules/core/`, the tech/basic
  recipe classes under `modules/tech/basic/init/recipe/`, and Pyrotech's own recipe JSON under
  `src/main/resources/assets/pyrotech/recipes/`. Paths below are relative to `src/main/java/com/codetaylor/mc/pyrotech/` or to `recipes/`.
- **1.12-DATA**: the Minecraft 1.12.2 client jar from Mojang's version manifest
  (`https://piston-meta.mojang.com/v1/packages/832d95b9f40699d4961394dcf6cf549e65f15dc5/1.12.2.json`,
  client sha1 `0f275bc1547d01fa5f56ba34bdc87d981ee12daf`, verified). Its 432 crafting recipes are
  `assets/minecraft/recipes/*.json`. Smelting recipes were code in 1.12.2 and are not in the jar as
  data.
- **DATA**: the vanilla 1.21.1 data pack at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`. Recipes are
  `data/minecraft/recipe/*.json` (1,290 files). Structure templates are
  `data/minecraft/structure/**/*.nbt` (1,180 files). Loot tables are `data/minecraft/loot_table/`.
  Tags are `data/minecraft/tags/`.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths and line numbers cited below are
  entries in that jar.
- **NF-DOCS**: docs.neoforged.net, 1.21.1 pages. URLs cited inline.
- **VT**: `docs/research/village-tweaks.md` on branch `research/village-tweaks` (issue #30).
- **CORE**: `docs/research/porting-notes-core.md` on branch `research/porting-notes-core`. Its
  decision 3 and its "Vanilla recipe surgery" table route the removal lists to datapack overrides.

## Summary

- The 1.12 crafting list has 75 ids and the furnace list has one. 63 of the 75 keep their id in
  1.21.1. Twelve were renamed by the 1.13 flattening, and Mojang's own rename map (NF-SRC
  `RecipesFix.java`) gives every new id. One rename is a trap: 1.12 `minecraft:snow` is now
  `minecraft:snow_block`, and 1.21's `minecraft:snow` is the old `snow_layer` recipe, which 1.12
  never removed.
- Six recipes added after 1.12.2 skip a gate from the bare 2x2 inventory grid with no station at
  all: five new plank recipes (cherry, mangrove, bamboo, crimson, warped) and sticks from bamboo.
- Nine more skip a gate from the worktable: five new wooden slab recipes, three new boats, the
  campfire, the soul campfire, the soul torch, the barrel, and the composter. The campfire cooks
  the nine vanilla foods without a furnace. The composter makes bone meal without the anvil. The
  barrel is a chest without the iron ingot.
- Ten stonecutting recipes reproduce the nine gated stone slabs. A stonecutter needs an iron
  ingot, but every village mason's house places one (6 templates).
- Villages and other structures place ten campfires, six stonecutters, sixteen composters, and
  nine barrels beyond the four blocks VT already covers. One post-1.12 chest loot table hands out
  a furnace (`village_snowy_house`) and one hands out barrels (`village_fisher`).
- NeoForge evaluates `neoforge:conditions` before it decodes the recipe body. A stub file carrying
  only a `neoforge:false` condition drops the vanilla recipe with a debug log line and no error,
  for every recipe type, because all types decode through the same `Recipe.CONDITIONAL_CODEC`.
- Recommendation: ship 76 faithful overrides (the translated 1.12 lists) plus 40 post-1.12
  additions, 116 files in `data/minecraft/recipe/`. Fold campfires into the VT chunk scan. Leave
  smoker, blast furnace, stonecutter, crafter, and loom recipes alone.

## What the 1.12 lists gate, and the Pyrotech route past each gate

`VANILLA_CRAFTING_REMOVE` is applied in `ModuleCore.onPostInitializationPreCrT` by casting the
recipe registry to `IForgeRegistryModifiable` and removing each id (1.12 `ModuleCore.java` line
246; `init/recipe/VanillaCraftingRecipesRemove.java` lines 11 to 17). The list is
`ModuleCoreConfig.java` lines 749 to 834. `VANILLA_FURNACE_REMOVE` is applied in the recipe
registration event before Pyrotech adds its own furnace recipes (`ModuleCore.java` lines 176 to
177). It walks the vanilla smelting map and removes every entry whose output item and meta match
the config string (`VanillaFurnaceRecipesRemove.java` lines 22 to 43). Its list is one entry,
`minecraft:brick` (`ModuleCoreConfig.java` lines 741 to 743). Removal is by output, not by id.

The table below groups the 1.12 entries by the gate they enforce and names the Pyrotech route the
player is meant to take instead. The "skip" verdicts in later sections are measured against these
routes.

| Gate | 1.12 entries | Pyrotech 1.12 route |
|---|---|---|
| 3x3 grid | `crafting_table` | The worktable is a wooden slab over a log (`tech/basic/worktable.json`). It gives a 3x3 grid that costs hammer hits and tool damage per craft (`ModuleTechBasicConfig.java` lines 136 to 148). A vanilla table comes later from planks around an iron template that costs 8 iron ingots (`crafting_table.json`, `crafting_table_template.json`). |
| Smelting | `furnace`; furnace list `brick` | A furnace is stone around a furnace core, and 16 cores cost 6 iron ingots and 3 blaze rods (`furnace.json`, `furnace_core.json`). Kilns and ovens have their own recipe registries. Nothing in 1.12 reads the vanilla smelting list except the remover (grep of `getSmeltingList` and `RecipeHelper.inherit` over the 1.12 tree). Brick comes from unfired brick (`VanillaFurnaceRecipesAdd.java` lines 45 to 50). |
| Cooking food | none removed; the furnace gate covers it | The Pyrotech campfire cooks any item whose vanilla furnace output is food (`tech/basic/recipe/CampfireRecipe.java` lines 57 to 64) and burns food left too long (`ModuleCoreConfig` `BURNED_FOOD_HUNGER_EFFECT_DURATION_TICKS`, line 700). |
| Planks | `oak_planks` and the other five | Chopping block, log plus axe. `CompatInitializerWood` harvests every log-to-planks recipe into the compat table that feeds it (`init/CompatInitializerWood.java` lines 64 to 68). |
| Sticks | `stick` | A sapling gives 2 sticks (`stick.json`). Leaves drop sticks (`event/HarvestDropsEventHandler.java` lines 59 to 64). The stone sawmill also makes them (`tech/machine/init/recipe/StoneSawmillRecipesAdd.java` line 35). |
| Wooden slabs | `oak_wooden_slab` and the other five | `CompatInitializerWood` harvests every three-planks-to-slab recipe (lines 73 to 101) for the sawmills (`StoneSawmillRecipesAdd.java` line 30). |
| Stone tools | `stone_axe`, `stone_pickaxe`, `stone_hoe`, `stone_shovel`, `stone_sword` | Pyrotech's own recipes from `pyrotech:material` 16 and sticks (`stone_pickaxe.json`, `stone_axe.json`). |
| Wooden tools | `wooden_axe`, `wooden_hoe`, `wooden_pickaxe`, `wooden_shovel` | Crude tools (`tool/crude_*.json`). |
| Torches | `torch` | Coal only, no charcoal: `torch_vanilla_coal.json` keys `W` to `minecraft:coal` data 0. Fiber and stone torches in ignition (`ignition/torch_fiber.json`, `ignition/torch_stone.json`). |
| Coal | `coal`, `coal_block` | The granite anvil breaks a coal block into 9 coal (`tech/basic/init/recipe/AnvilGraniteRecipesAdd.java` lines 493 to 494). Compacting bins build blocks. |
| Bone meal | `bone_meal_from_bone`, `bone_meal_from_block` | Bone to bone shard with a pickaxe on the anvil (lines 277 to 280), shard to bone meal with a hammer (lines 148 to 151). |
| Stone variants | `andesite`, `granite`, `diorite` | Cobbled variants smelt back to stone (`VanillaFurnaceRecipesAdd.java` lines 17 to 36; `cobbled_andesite.json`). |
| Stone slabs | `stone_slab`, `sandstone_slab`, `cobblestone_slab`, `brick_slab`, `stone_brick_slab`, `nether_brick_slab`, `quartz_slab`, `red_sandstone_slab`, `purpur_slab` | Anvil with a pickaxe, 2 slabs per block (`AnvilGraniteRecipesAdd.java` lines 332 to 415), the brick sawmill (`BrickSawmillRecipesAdd.java`), and a rock-and-mortar recipe (`cobblestone_slab.json`). |
| Compaction and decompression | `clay`, `snow`, `bone_block`, `redstone_block`, `lapis_block`, `redstone`, `lapis_lazuli`, `iron_nugget`, `gold_nugget`, `iron_ingot_from_block`, `gold_ingot_from_block` | Compacting bin (`CompactingBinRecipesAdd.java`: lapis 35 to 40, redstone 42 to 47, clay 122 to 129, snow 131 to 138, bone block 140 to 147). Anvil for the reverse (`AnvilGraniteRecipesAdd.java` lines 55 to 67). |
| Paper | `paper` | Pulp on the crude drying rack (`CrudeDryingRackRecipesAdd.java` lines 45 to 50). |
| Chest | `chest` | Planks around an iron ingot (`chest.json`). |
| Food | `bread`, `cookie`, `cake` | Dough first (`bread_dough.json`), then baked (`VanillaFurnaceRecipesAdd.java` lines 129 to 141). Cake from flour (`cake.json`). |
| Leather | `leather`, `leather_helmet`, `leather_chestplate`, `leather_leggings`, `leather_boots` | Tanning rack, then armor from Pyrotech leather materials (`leather_boots.json`). |
| Other | `book`, `item_frame`, `lead`, `magma_cream`, `arrow`, `shears`, `fire_charge`, `boat` and the other five boats | Own recipes with Pyrotech intermediates. Boats need tarred planks (`boat_oak.json`). |

Note what 1.12 did not gate: `wooden_sword`, stairs, walls, fences, doors, the `iron_block` and
`gold_block` compaction recipes, and every smelting recipe except brick. Those stay untouched in
the port. The verdicts below only count a recipe as a skip when its output belongs to a gated row.

## 1. Recipes added after 1.12.2 that skip a gate

All 1,290 recipes in DATA were indexed by type, output, ingredients, and grid size. Each output was
matched against the 432 crafting recipes in 1.12-DATA by id and by flattened output (see
Verification). The tables list only the recipes whose output belongs to a gated row above and
which did not exist in 1.12.2. Ids are in the `minecraft` namespace.

### A. Craftable in the 2x2 inventory grid, no station needed

These are the worst skips. They need neither a worktable nor a hammer.

| Recipe id | Output | Inputs | 1.12 parallel | Gate skipped |
|---|---|---|---|---|
| `cherry_planks` | 4 cherry planks | 1 `#minecraft:cherry_logs` | `oak_planks` | Planks (chopping block) |
| `mangrove_planks` | 4 mangrove planks | 1 `#minecraft:mangrove_logs` | `oak_planks` | Planks |
| `crimson_planks` | 4 crimson planks | 1 `#minecraft:crimson_stems` | `oak_planks` | Planks |
| `warped_planks` | 4 warped planks | 1 `#minecraft:warped_stems` | `oak_planks` | Planks |
| `bamboo_planks` | 2 bamboo planks | 1 `#minecraft:bamboo_blocks` | `oak_planks` | Planks. The bamboo block itself is 9 bamboo in a 3x3 (`bamboo_block`), so this one needs a worktable one step earlier. |
| `stick_from_bamboo_item` | 1 stick | 2 bamboo, 1 wide 2 tall | `stick` | Sticks |

The six original plank recipes keep their ids but now take log tags. `#minecraft:oak_logs` holds
`oak_log`, `oak_wood`, `stripped_oak_log`, and `stripped_oak_wood` (DATA
`tags/item/oak_logs.json`). One override per id still covers all four inputs.

Vanilla leaves now drop sticks on their own. All ten `*_leaves.json` block loot tables in DATA
carry a `minecraft:stick` entry (for example `loot_table/blocks/oak_leaves.json` line 129). That
matches the 1.12 handler's intent, so it is not a skip. CORE already noted it.

### B. Needs the 3x3 grid, but nothing else that is gated

A worktable is the first thing a 1.12 player builds. Anything that needs only the worktable plus
free materials is an early-game skip.

| Recipe id | Output | Inputs and grid | 1.12 parallel | Gate skipped |
|---|---|---|---|---|
| `cherry_slab`, `mangrove_slab`, `bamboo_slab`, `crimson_slab`, `warped_slab` | 6 slabs | 3 planks, 3 wide 1 tall | `oak_wooden_slab` | Wooden slabs (sawmill). Planks come from the row above or the chopping block. |
| `cherry_boat`, `mangrove_boat`, `bamboo_raft` | 1 boat | 5 planks, 3 wide 2 tall | `boat` | Boats (tarred planks). |
| `campfire` | 1 campfire | 3 `stick`, 1 `#minecraft:coals`, 3 `#minecraft:logs`, 3x3 | none | Cooking. Cooks the nine `campfire_cooking` recipes with no furnace. See section 2. |
| `soul_campfire` | 1 soul campfire | 3 `stick`, 1 `#minecraft:soul_fire_base_blocks`, 3 `#minecraft:logs`, 3x3 | none | Cooking. Same block entity behaviour. Needs the Nether. |
| `soul_torch` | 4 soul torches | 1 `stick`, 1 coal or charcoal, 1 soul sand or soul soil, 1 wide 3 tall | `torch` | Torches. It accepts charcoal, which `torch_vanilla_coal.json` refuses. Needs the Nether. |
| `barrel` | 1 barrel | 6 `#minecraft:planks`, 2 `#minecraft:wooden_slabs`, 3x3 | `chest` | Chest. 27 slots (NF-SRC `net/minecraft/world/level/block/entity/BarrelBlockEntity.java` line 24) with no iron ingot. |
| `composter` | 1 composter | 7 `#minecraft:wooden_slabs`, 3x3 | none | Bone meal. Fills from plant matter and emits bone meal (NF-SRC `net/minecraft/world/level/block/ComposterBlock.java` lines 287 and 358). |

`#minecraft:coals` is coal and charcoal (DATA `tags/item/coals.json`). `#minecraft:logs` is every
overworld log tag plus crimson and warped stems (DATA `tags/item/logs.json`).

`bamboo_mosaic_slab` (3 bamboo mosaic, 3 wide 1 tall) is the same shape, but bamboo mosaic is
made from two bamboo slabs (`bamboo_mosaic`, 1 wide 2 tall), so it sits behind the bamboo slab
gate. Not counted.

### C. Recipes that only the new stations can run

| Recipes | Type | Station | Reached without a gated item? | Verdict |
|---|---|---|---|---|
| `baked_potato_from_campfire_cooking`, `cooked_beef_from_campfire_cooking`, `cooked_chicken_from_campfire_cooking`, `cooked_cod_from_campfire_cooking`, `cooked_mutton_from_campfire_cooking`, `cooked_porkchop_from_campfire_cooking`, `cooked_rabbit_from_campfire_cooking`, `cooked_salmon_from_campfire_cooking`, `dried_kelp_from_campfire_cooking` | `campfire_cooking` | Campfire, soul campfire | Yes, via row B, a village, or the fisherman trade | Skip of the cooking gate. |
| `stone_slab_from_stone_stonecutting`, `stone_brick_slab_from_stone_stonecutting`, `stone_brick_slab_from_stone_bricks_stonecutting`, `cobblestone_slab_from_cobblestone_stonecutting`, `sandstone_slab_from_sandstone_stonecutting`, `red_sandstone_slab_from_red_sandstone_stonecutting`, `brick_slab_from_bricks_stonecutting`, `nether_brick_slab_from_nether_bricks_stonecutting`, `quartz_slab_from_stonecutting`, `purpur_slab_from_purpur_block_stonecutting` | `stonecutting` | Stonecutter | The recipe needs an iron ingot, but six village templates place the block (section 3) | Skip of the stone slab gate, 2 slabs per block with no hammer. |
| The nine `*_from_smoking` recipes | `smoking` | Smoker | No. The smoker recipe consumes a furnace. | Not a skip on its own. Reachable from a village butcher shop (VT decision 2). |
| The 24 `blasting` recipes | `blasting` | Blast furnace | No. The recipe consumes a furnace and 5 iron ingots. | Not a skip on its own. Same village caveat. |
| 9 `smithing_transform`, 18 `smithing_trim` | `smithing_transform`, `smithing_trim` | Smithing table | n/a | No output is gated. Netherite upgrades and armor trims. |
| The other 240 `stonecutting` recipes | `stonecutting` | Stonecutter | n/a | Outputs are stairs, walls, polished, chiseled, and cut blocks. 1.12 gated none of them. |

### D. Parallels that are not skips

These look like new routes but each consumes something that is already gated, or produces
something 1.12 never gated.

| Recipe ids | Why it is not a skip |
|---|---|
| `oak_chest_boat` and the other eight chest boats, `bamboo_chest_raft` | Shapeless boat plus chest. The chest is gated. |
| `crafter` | Iron, redstone, a dropper, and a `crafting_table`. The table is gated. |
| `glow_item_frame` | Item frame plus glow ink sac. The frame is gated. |
| `lantern`, `soul_lantern` | A torch or soul torch plus 8 iron nuggets. |
| `smoker`, `blast_furnace` | Both consume a `furnace`. See section 2. |
| `copper_ingot`, `copper_ingot_from_waxed_copper_block`, `raw_iron`, `raw_gold`, `raw_copper` | Block-to-item decompressions that parallel `iron_ingot_from_block`. 1.12 records no reason for removing the two ingot recipes, copper is not a Pyrotech material, and raw ore blocks are only made from raw ore the player already holds. Left for a decision. |
| `charcoal`, `coal_from_smelting_coal_ore`, `stone`, `smooth_stone`, and the food smelting recipes | Furnace recipes. The furnace is gated. 1.12.2 had the same recipes in code. |
| `snow` | This is the 1.12 `snow_layer` recipe (3 snow blocks to 6 layers) under its post-flattening id (NF-SRC `net/minecraft/util/datafix/fixes/RecipesFix.java` line 49). 1.12 never removed it. The 1.12 `snow` recipe is now `snow_block` (line 50). |
| `bone_meal_from_bone_block`, `bone_meal` | Renamed 1.12 entries, not new. See section 4. |

## 2. The post-1.12 stations

Every station's recipe was read from DATA. "Gated inputs" names what the recipe consumes that
1.12 already gates. The last column says whether the block alone gets a player past a gate.

| Station | Recipe id, grid | Ingredients | Gated inputs | What it does (NF-SRC) | Skip on its own? |
|---|---|---|---|---|---|
| Campfire | `campfire`, 3x3 | 3 stick, 1 `#coals`, 3 `#logs` | sticks | Cooks `campfire_cooking` recipes (`block/entity/CampfireBlockEntity.java` line 38) | Yes |
| Soul campfire | `soul_campfire`, 3x3 | 3 stick, 1 soul sand or soil, 3 `#logs` | sticks | Same block entity | Yes, after the Nether |
| Smoker | `smoker`, 3x3 | 1 `furnace`, 4 `#logs` | furnace | `smoking` recipes (`SmokerBlockEntity.java` line 14) | No, consumes a furnace |
| Blast furnace | `blast_furnace`, 3x3 | 1 `furnace`, 5 iron ingot, 3 smooth stone | furnace, iron | `blasting` recipes (`BlastFurnaceBlockEntity.java` line 14) | No, consumes a furnace |
| Stonecutter | `stonecutter`, 3 wide 2 tall | 3 stone, 1 iron ingot | iron | `stonecutting` recipes (`inventory/StonecutterMenu.java` line 177) | Partly. Ten of its recipes reproduce gated slabs. |
| Grindstone | `grindstone`, 3 wide 2 tall | 2 stick, 1 `stone_slab`, 2 `#planks` | sticks, slab, planks | Repair and disenchant | No |
| Smithing table | `smithing_table`, 2 wide 3 tall | 2 iron ingot, 4 `#planks` | iron, planks | Netherite upgrades, trims | No |
| Loom | `loom`, 2x2 | 2 string, 2 `#planks` | planks | Banner patterns | No. It is the only new station that fits the inventory grid. |
| Cartography table | `cartography_table`, 2 wide 3 tall | 2 paper, 4 `#planks` | paper, planks | Maps | No |
| Fletching table | `fletching_table`, 2 wide 3 tall | 2 flint, 4 `#planks` | planks | Job site only | No |
| Composter | `composter`, 3x3 | 7 `#wooden_slabs` | slabs | Bone meal from compostables (`ComposterBlock.java` lines 287 and 358) | Yes |
| Lectern | `lectern`, 3x3 | 4 `#wooden_slabs`, 1 bookshelf | slabs, books | Book display | No |
| Barrel | `barrel`, 3x3 | 6 `#planks`, 2 `#wooden_slabs` | planks, slabs | 27-slot container (`BarrelBlockEntity.java` line 24) | Yes, a chest without iron |
| Crafter | `crafter`, 3x3 | 5 iron ingot, 1 `crafting_table`, 1 dropper, 2 redstone | crafting table, iron | Automated crafting | No, consumes a table |

Two stations are crafted from a furnace: the smoker and the blast furnace. One is crafted from a
crafting table: the crafter. Nothing else in DATA consumes either block. Because the furnace and
crafting table recipes are removed, those three follow the furnace and table gates without any
extra file. The catch is a furnace or table obtained another way. Section 3 lists those ways.

Every station except the loom needs the 3x3 grid, so the worktable is the floor for all of them.

## 3. Structure blocks, chest loot, and one trade

All 1,180 templates in DATA were decompressed and searched for exact block ids (see
Verification). VT covers crafting table (29 templates), furnace (22), blast furnace (9), and
smoker (9); the counts here match VT. The post-1.12 stations:

| Block | Templates | Where |
|---|---|---|
| `minecraft:campfire` | 10 | Taiga village: `taiga_armorer_2`, `taiga_butcher_shop_1`, `taiga_medium_house_4` and its zombie copy, `taiga_decoration_5`, `taiga_decoration_6`. Ancient city: `structures/camp_2`, `camp_3`. Trail ruins: `buildings/large_room_2`, `tower/platform_5`. |
| `minecraft:soul_campfire` | 0 | |
| `minecraft:stonecutter` | 6 | Every village mason's house: desert, plains, savanna, snowy (2), taiga. |
| `minecraft:composter` | 16 | Every village farm (15) and `trail_ruins/buildings/group_hall_2`. |
| `minecraft:barrel` | 9 | Every village fisher cottage (6 with the zombie copy), `trial_chambers/decor/barrel`, `intersection_2`, `intersection_3`. |
| `minecraft:grindstone` | 10 | Village weaponsmiths (8) and two trail ruins rooms. |
| `minecraft:smithing_table` | 7 | Village tool smiths (6) and `trail_ruins/buildings/large_room_1`. |
| `minecraft:loom` | 14 | Village shepherds (7) and seven trail ruins pieces. |
| `minecraft:cartography_table` | 7 | Village cartographers (6) and `trail_ruins/buildings/one_room_4`. |
| `minecraft:fletching_table` | 6 | Every village fletcher house. |
| `minecraft:lectern` | 10 | Village libraries (7) and the three ancient city centers. |
| `minecraft:crafter` | 0 | |

No code-built piece places any of these. Every class under
`net/minecraft/world/level/levelgen/structure/structures/` in NF-SRC was grepped for the block
constants. The only hit is the swamp hut's crafting table (`SwampHutPiece.java` line 74), which
VT already reports.

Only four of the placed blocks matter for progression: the campfire (cooks), the stonecutter
(slabs), the composter (bone meal), and the barrel (storage). The other six do nothing 1.12
gates. Three of the four are villager job sites (stonecutter for masons, composter for farmers,
barrel for fishermen). Replacing them would unemploy those villagers.

Two post-1.12 chest loot tables hand out a gated station block:

| Loot table | Entry | Odds |
|---|---|---|
| `loot_table/chests/village/village_snowy_house.json` | `minecraft:furnace`, weight 1 | Pool weight 53, 3 to 8 rolls. Roughly 5% to 14% of snowy house chests contain a furnace. |
| `loot_table/chests/village/village_fisher.json` | `minecraft:barrel` 1 to 3, weight 1 | Pool weight 11, 1 to 5 rolls. |

The snowy house furnace is a real skip of the furnace gate. The VT chunk scan replaces placed
furnaces but does not open chests. A global loot modifier on that table, or an override of the
file, closes it. CORE already plans a loot modifier for the iron ingot swap, so this is one more
entry there.

Trades are out of scope for this ticket. One is recorded here because it defeats the campfire
removal: the fisherman's second-level trade sells a campfire for two emeralds (NF-SRC
`net/minecraft/world/entity/npc/VillagerTrades.java` line 125 for the profession, line 139 for
the listing). Removing the nine `campfire_cooking` recipes makes a traded campfire cook nothing,
which closes the leak without touching trades.

## 4. Translating the lists to 1.21 datapack overrides

### How NeoForge loads a recipe with conditions

`RecipeManager` is a `SimpleJsonResourceReloadListener` over the directory
`Registries.elementsDirPath(Registries.RECIPE)` (NF-SRC
`net/minecraft/world/item/crafting/RecipeManager.java` lines 36 and 45). That resolves to
`recipe`, singular (NF-SRC `net/minecraft/core/registries/Registries.java` lines 237 and 251 to
253; NF-DOCS: "Recipe data files are located at `data/<namespace>/recipe/<path>.json`",
https://docs.neoforged.net/docs/1.21.1/resources/server/recipes/).

`apply` builds a `ConditionalOps` (line 53), skips paths that start with `_` (line 57), and parses
every file with `Recipe.CONDITIONAL_CODEC` (line 60). A present result is registered by type and
by id (lines 61 to 65). An empty result logs, at debug level, "Skipping loading recipe {} as its
conditions were not met" and moves on (lines 66 to 68). Parse failures are logged as errors and
also move on (lines 69 to 71). Nothing in this path throws.

`Recipe.CONDITIONAL_CODEC` wraps `Recipe.CODEC` with
`ConditionalOps.createConditionalCodecWithConditions` (NF-SRC `Recipe.java` lines 17 to 18).
`Recipe.CODEC` dispatches on the serializer registry (line 17), and every vanilla type registers
there: `crafting_shaped`, `crafting_shapeless`, the special crafting recipes, `smelting`,
`blasting`, `smoking`, `campfire_cooking`, `stonecutting`, `smithing_transform`, `smithing_trim`,
and the decorated pot (NF-SRC `RecipeSerializer.java` lines 10 to 50). So the condition wrapper
sits outside the type dispatch and applies to every recipe type the same way. NF-DOCS agrees:
"Common to all recipe files are the `type` and `neoforge:conditions` properties" (recipes page).

The decoder reads the `neoforge:conditions` key first (NF-SRC
`net/neoforged/neoforge/common/conditions/ConditionalOps.java` line 54 for the key, lines 204 to
209 for the no-conditions path). When the key is present it decodes the condition list, tests
every condition against the context, and returns `Optional.empty()` before it ever calls the
inner codec (lines 211 to 220). The recipe body is decoded only when all conditions pass (lines
222 to 235). A file that holds nothing but a failing condition is therefore valid. It never
reaches the `type` lookup.

`FalseCondition.test` returns `false` and its codec is a unit `MapCodec`, so it takes no fields
(NF-SRC `conditions/FalseCondition.java` lines 13 to 19). It is registered as `neoforge:false`
alongside `and`, `item_exists`, `mod_loaded`, `not`, `or`, `tag_empty`, and `true` (NF-SRC
`net/neoforged/neoforge/common/NeoForgeMod.java` lines 379 to 387; the registry is
`NeoForgeRegistries.CONDITION_SERIALIZERS`, `ICondition.java` lines 33 to 35). NF-DOCS lists the
same eight ids and states "Loading will continue if and only if all conditions pass, otherwise the
data file will be ignored" (https://docs.neoforged.net/docs/1.21.1/resources/server/conditions).
CORE noted the same id and that `neoforge:never` is a later rename.

### Why the mod's file wins over vanilla's

`scanDirectory` asks the resource manager for one `Resource` per id (NF-SRC
`net/minecraft/server/packs/resources/SimpleJsonResourceReloadListener.java` lines 37 to 53).
`FallbackResourceManager.listResources` walks the packs in order and `put`s each hit into a map,
so the pack with the highest index supplies the resource (NF-SRC
`FallbackResourceManager.java` lines 161 to 177). The vanilla pack is inserted at
`Pack.Position.BOTTOM` (NF-SRC `net/minecraft/server/packs/repository/ServerPacksSource.java`
line 35). Mod packs and the aggregate "Mod Data" pack use `Pack.Position.TOP` (NF-SRC
`net/neoforged/neoforge/resource/ResourcePackLoader.java` lines 63 and 204), which
`Position.insert` places after every existing entry (NF-SRC `repository/Pack.java` lines 255 to
265). NF-DOCS: "most data files can be overridden (and thus be removed by replacing them with an
empty file) by a data pack with a higher priority", and "It is currently not possible to disable
mod data packs" (https://docs.neoforged.net/docs/1.21.1/resources/). A user datapack sits above
the mod pack and can put any recipe back by shipping the full recipe under the same id. That is
the 1.21 shape of the 1.12 config list.

### What happens downstream of a missing recipe

Vanilla ships a recipe-unlock advancement for every recipe it removes here. Those advancements
reward the recipe by id. `AdvancementRewards.grant` passes the id list to
`ServerPlayer.awardRecipesByKey` (NF-SRC `net/minecraft/advancements/AdvancementRewards.java`
lines 76 to 77), which looks each id up with `byKey(...).stream()` and silently drops the ones
that no longer exist (NF-SRC `net/minecraft/server/level/ServerPlayer.java` lines 1378 to 1383).
No error, no log line. The advancement files can stay.

### The stub

One file per removed recipe, at `src/main/resources/data/minecraft/recipe/<path>.json`:

```json
{
  "neoforge:conditions": [
    { "type": "neoforge:false" }
  ]
}
```

No `type` field is needed, per the decoder order above. Adding one is harmless. The files are
hand-written. NeoForge's `RecipeProvider` cannot emit them, because `ConditionalEncoder` refuses
to encode without a carrier recipe (`ConditionalOps.java` lines 136 to 139). A datagen provider
that wrote the stubs would be a custom `DataProvider`, which is more code than 116 identical
files justify.

### Translating the ids

Mojang's recipe-book fixer maps the pre-flattening recipe ids to their 1.13 names (NF-SRC
`net/minecraft/util/datafix/fixes/RecipesFix.java`). Twelve entries of the 1.12 list are in that
map. Every other entry keeps its id, confirmed by the file existing in DATA under the same name.

| 1.12 id | 1.21.1 id | Source |
|---|---|---|
| `oak_wooden_slab` | `oak_slab` | `RecipesFix.java` line 38 |
| `spruce_wooden_slab` | `spruce_slab` | line 52 |
| `birch_wooden_slab` | `birch_slab` | line 9 |
| `jungle_wooden_slab` | `jungle_slab` | line 28 |
| `acacia_wooden_slab` | `acacia_slab` | line 8 |
| `dark_oak_wooden_slab` | `dark_oak_slab` | line 19 |
| `bone_meal_from_bone` | `bone_meal` | line 14 |
| `bone_meal_from_block` | `bone_meal_from_bone_block` | line 13 |
| `snow` | `snow_block` | line 50. Do not override 1.21 `snow`; that is the old `snow_layer` recipe (line 49). |
| `iron_ingot_from_block` | `iron_ingot_from_iron_block` | line 27 |
| `gold_ingot_from_block` | `gold_ingot_from_gold_block` | line 24 |
| `boat` | `oak_boat` | line 12 |

One entry has a second life. The 1.12 `stone_slab` recipe turned 3 stone into 6 of the slab that
1.14 renamed to `smooth_stone_slab` (NF-SRC `net/minecraft/util/datafix/DataFixers.java` lines
661 to 665 for the block, 675 to 678 for the item). The 1.21 recipe id `stone_slab` still exists
and now makes the newer stone-textured slab from stone. `smooth_stone_slab` makes the old slab
from smooth stone, which is a furnace product. By id the override is `stone_slab`. By output it
is `smooth_stone_slab`. Both are three blocks in a row for six slabs with no hammer. The
recommended list carries both, plus the two stonecutting versions. Listed as a decision.

The furnace list translates by output, since 1.12 matched on output. In DATA exactly one recipe
of any type produces `minecraft:brick`: `brick` (smelting, clay ball). No blasting or campfire
recipe makes it. One file, `brick.json`.

## Recommended removal list

116 files under `data/minecraft/recipe/`. Ids are in the `minecraft` namespace.

**Faithful translation of the 1.12 lists, 76 files.**

- Wooden tools (4): `wooden_axe`, `wooden_hoe`, `wooden_pickaxe`, `wooden_shovel`.
- Planks (6): `oak_planks`, `spruce_planks`, `birch_planks`, `jungle_planks`, `acacia_planks`,
  `dark_oak_planks`.
- Wooden slabs (6): `oak_slab`, `spruce_slab`, `birch_slab`, `jungle_slab`, `acacia_slab`,
  `dark_oak_slab`.
- Stone tools (5): `stone_axe`, `stone_pickaxe`, `stone_hoe`, `stone_shovel`, `stone_sword`.
- Bone meal (2): `bone_meal`, `bone_meal_from_bone_block`.
- Stone variants (3): `andesite`, `granite`, `diorite`.
- Stone slabs (9): `stone_slab`, `sandstone_slab`, `cobblestone_slab`, `brick_slab`,
  `stone_brick_slab`, `nether_brick_slab`, `quartz_slab`, `red_sandstone_slab`, `purpur_slab`.
- Singles (30): `stick`, `clay`, `snow_block`, `bone_block`, `paper`, `torch`, `coal_block`,
  `coal`, `chest`, `furnace`, `crafting_table`, `redstone_block`, `redstone`, `lapis_block`,
  `lapis_lazuli`, `iron_nugget`, `gold_nugget`, `iron_ingot_from_iron_block`,
  `gold_ingot_from_gold_block`, `fire_charge`, `leather`, `item_frame`, `book`, `lead`,
  `magma_cream`, `arrow`, `bread`, `cookie`, `cake`, `shears`.
- Boats (6): `oak_boat`, `spruce_boat`, `birch_boat`, `jungle_boat`, `acacia_boat`,
  `dark_oak_boat`.
- Leather armor (4): `leather_helmet`, `leather_chestplate`, `leather_leggings`, `leather_boots`.
- Furnace list (1): `brick`.

**Post-1.12 additions, 40 files.**

- Planks (5): `cherry_planks`, `mangrove_planks`, `bamboo_planks`, `crimson_planks`,
  `warped_planks`.
- Wooden slabs (5): `cherry_slab`, `mangrove_slab`, `bamboo_slab`, `crimson_slab`, `warped_slab`.
- Sticks (1): `stick_from_bamboo_item`.
- Boats (3): `cherry_boat`, `mangrove_boat`, `bamboo_raft`.
- Cooking stations (2): `campfire`, `soul_campfire`.
- Campfire cooking (9): `baked_potato_from_campfire_cooking`, `cooked_beef_from_campfire_cooking`,
  `cooked_chicken_from_campfire_cooking`, `cooked_cod_from_campfire_cooking`,
  `cooked_mutton_from_campfire_cooking`, `cooked_porkchop_from_campfire_cooking`,
  `cooked_rabbit_from_campfire_cooking`, `cooked_salmon_from_campfire_cooking`,
  `dried_kelp_from_campfire_cooking`.
- Storage (1): `barrel`.
- Bone meal (1): `composter`.
- Stonecutting slabs (10): `stone_slab_from_stone_stonecutting`,
  `stone_brick_slab_from_stone_stonecutting`, `stone_brick_slab_from_stone_bricks_stonecutting`,
  `cobblestone_slab_from_cobblestone_stonecutting`, `sandstone_slab_from_sandstone_stonecutting`,
  `red_sandstone_slab_from_red_sandstone_stonecutting`, `brick_slab_from_bricks_stonecutting`,
  `nether_brick_slab_from_nether_bricks_stonecutting`, `quartz_slab_from_stonecutting`,
  `purpur_slab_from_purpur_block_stonecutting`.
- Smooth stone slab (2): `smooth_stone_slab`, `smooth_stone_slab_from_smooth_stone_stonecutting`.
- Torches (1): `soul_torch`.

**Left alone, on purpose.** `smoker`, `blast_furnace`, `crafter` (each consumes a gated block),
`stonecutter`, `grindstone`, `smithing_table`, `loom`, `cartography_table`, `fletching_table`,
`lectern` (no gated output), the chest boats, `glow_item_frame`, the lanterns, the copper and raw
ore decompressions, `bamboo_mosaic_slab`, the nine smoking recipes, the 24 blasting recipes, all
smithing recipes, and 1.21 `snow`.

**Outside the recipe folder.** Add `minecraft:campfire` to the VT chunk scan's furnace toggle,
replaced with air. Add a loot modifier or table override that strips `minecraft:furnace` from
`chests/village/village_snowy_house`. Both are decisions below.

The removals open gaps that Pyrotech's own recipes must fill: chopping block entries for the five
new wood types, and boat recipes for cherry, mangrove, and bamboo. That belongs to the recipe
tickets, not to this one.

## Verification

- 1.12 lists: `ModuleCoreConfig.java` lines 734 to 835 on `origin/1.12` were read in full. The
  two applier classes and their call sites in `ModuleCore.java` were read. `git grep` over the
  1.12 tree found no other users of `VANILLA_CRAFTING_REMOVE`, `VANILLA_FURNACE_REMOVE`, or
  `getSmeltingList`, and every `RecipeHelper.inherit` call chains Pyrotech registries only.
- Pyrotech routes: 35 Pyrotech recipe JSONs (26 under `recipes/`, 9 under
  `recipes/tech/basic/`), including every one cited above, and the tech/basic recipe classes
  (`CompactingBinRecipesAdd`, `AnvilGraniteRecipesAdd`, `CrudeDryingRackRecipesAdd`,
  `ChoppingBlockRecipesAdd`, `CampfireRecipesAdd`, `CampfireRecipe`, `CompatInitializerWood`)
  were read from `origin/1.12`.
- 1.21.1 recipes: all 1,290 files under `data/minecraft/recipe/` in DATA were parsed with Python
  into a table of id, type, output, count, ingredients, and grid size. Counts by type:
  634 shaped, 253 shapeless, 250 stonecutting, 70 smelting, 24 blasting, 9 smoking, 9 campfire
  cooking, 18 smithing trim, 9 smithing transform, 14 special crafting.
- Added after 1.12.2: the 432 files under `assets/minecraft/recipes/` in 1.12-DATA were parsed the
  same way. Every 1.21 recipe whose output belongs to a gated row was checked for a 1.12.2 recipe
  with the same id, then for one with the same flattened output (planks and slab metas, `dye` 15
  for bone meal, `boat`, `snow`, `coal` 1 for charcoal). Only rows with neither are reported as
  new. Smelting recipes were code in 1.12.2 and are absent from that jar, so the smelting family
  (brick, charcoal, coal from ore, stone, food) was judged from the 1.12 Pyrotech code that
  depends on them (`VanillaFurnaceRecipesRemove`, `CampfireRecipe` lines 57 to 64) rather than
  from a diff.
- Id translation: every 1.12 id was looked up in DATA by file name. The 63 hits keep their id.
  The 12 misses were resolved through `RecipesFix.java` and each resolved id was confirmed to
  exist in DATA. The 1.12.2 `snow.json`, `snow_layer.json`, `boat.json`, `bone_meal_from_bone.json`,
  `iron_ingot_from_block.json`, `oak_wooden_slab.json`, and `stone_slab.json` were read to
  confirm their outputs. The stone slab rename was confirmed in `DataFixers.java`.
- 2x2 fit: shaped recipes with a pattern at most 2 wide and 2 tall, and shapeless recipes with at
  most 4 ingredients, were flagged. 356 of 1,290 fit. Only the six in section 1A have a gated
  output.
- Template scan: all 1,180 `.nbt` files under `data/minecraft/structure/` were read with Python
  `gzip` and searched for `minecraft:<block>` followed by a non-identifier byte, so `campfire`
  does not match `soul_campfire` and `furnace` does not match `blast_furnace`. Blocks searched:
  the four VT blocks, campfire, soul campfire, stonecutter, grindstone, smithing table, loom,
  cartography table, fletching table, composter, lectern, barrel, crafter, anvils, brewing stand,
  enchanting table, cauldrons. The VT counts were reproduced exactly.
- Code-built pieces: every class under `structures/` in NF-SRC was grepped for the block
  constants. Only `SwampHutPiece` matched.
- Loot: all 1,178 loot tables in DATA were grepped for the station block ids. Only the two village
  chests and the blocks' own drop tables matched. Pool weights were summed from the JSON.
- NeoForge loading: `RecipeManager`, `Recipe`, `RecipeSerializer`, `RecipeType`, `ConditionalOps`,
  `ICondition`, `FalseCondition`, `NeoForgeMod` (condition registrations),
  `SimpleJsonResourceReloadListener`, `FallbackResourceManager`, `ResourcePackLoader`,
  `ServerPacksSource`, `Pack.Position`, `AdvancementRewards`, and `ServerPlayer` were read at the
  cited lines. The two NF-DOCS pages were fetched and quoted.
- Not verified by running code. No client was launched and no stub file was loaded into a world.
  The first tracer of the core port should place one stub, start a world, and confirm the debug
  line and the missing recipe in the recipe book.

## Decisions for Moos

1. **Scope of the additions.** Options: (a) faithful only, the 76 translated files; (b) faithful
   plus the 40 additions above; (c) faithful plus a narrower set, the 15 wood-family files
   (planks, slabs, bamboo stick, boats) and the two campfires. Recommendation: (b). The 1.12
   lists expressed a design, not a file set, and every addition parallels a row of that design.
2. **Campfire cooking recipes.** Removing `campfire` and `soul_campfire` already stops crafting.
   Options: also remove the nine `campfire_cooking` recipes, or leave them. Recommendation:
   remove them. A village campfire, a fisherman trade, or another mod's campfire then cooks
   nothing, which matches the 1.12 rule that only the Pyrotech campfire cooks early food.
3. **Placed campfires.** Ten templates place one. Options: add `minecraft:campfire` to the VT
   chunk scan (replaced with air, under the furnace toggle), leave them, or a third toggle.
   Recommendation: fold into the furnace toggle, as VT decision 2 does for smokers and blast
   furnaces. If decision 2 removes the cooking recipes, this is belt and braces and can wait.
4. **Placed job-site stations.** Village masons, farmers, and fishermen stand at a stonecutter, a
   composter, and a barrel. Options: replace them in the scan, or leave them and rely on recipe
   removal. Recommendation: leave them. The ten slab recipes are gone, so a village stonecutter
   cuts nothing gated. A village composter trickles bone meal and a village barrel holds items,
   which is a small leak compared with breaking villager professions.
5. **Stone slab pair.** Options: override `stone_slab` only (by id), `smooth_stone_slab` only (by
   output), or both with their stonecutting twins. Recommendation: both, four files. Either slab
   is three blocks in a row for six slabs with no hammer, which is what the 1.12 entry stopped.
6. **Soul torch.** It accepts charcoal, which the Pyrotech torch recipe refuses, but it needs soul
   sand or soul soil from the Nether. Options: remove, or leave as a Nether-era reward.
   Recommendation: remove, and let the ignition module add its own coal-only soul torch recipe if
   wanted. The charcoal rule is the point of the 1.12 torch entry.
7. **Copper and raw ore decompressions.** `copper_ingot`, `copper_ingot_from_waxed_copper_block`,
   `raw_iron`, `raw_gold`, and `raw_copper` parallel `iron_ingot_from_block`, whose reason is not
   recorded in 1.12. Options: extend the removal to them, or leave them. Recommendation: leave
   them. Copper is not a Pyrotech material, and a raw ore block only unpacks what the player
   already mined. Revisit when the bloomery ticket decides what raw ore means to Pyrotech.
8. **Snowy house furnace.** Options: a global loot modifier that strips `minecraft:furnace` from
   `chests/village/village_snowy_house`, an override of that loot table, or leave it as a rare
   find. Recommendation: the loot modifier, next to the iron ingot swap CORE already plans. An
   override of the whole table collides with other mods the same way VT's pool overrides would.
9. **Smoker and blast furnace.** Both consume a furnace, so the furnace gate covers them. Options:
   leave the recipes, or remove them so a found furnace cannot be upgraded. Recommendation:
   leave them. Once a 1.12 player had a furnace, vanilla smelting was open. A faster furnace is
   the same tier.
