# Village tweaks: replacing the 1.12 chunk scan

Resolves issue #30 (village tweaks: replacing the 1.12 chunk scan).
This is the follow-up that decision 4 of the core porting notes asked for.
The 1.12 core module scans every populated chunk and, when two tweaks are on, removes every
vanilla crafting table and replaces every furnace with cobblestone. Both tweaks default to on.
Both guard progression: a village furnace or crafting table skips Pyrotech's early game.
NeoForge 1.21.1 has no post-populate chunk event. This note finds what replaces the scan.
Decisions that need Moos are collected at the end.

## Sources

- **1.12**: the `1.12` branch of this repo. `PopulateChunkEventHandler` and the `Tweaks` class in
  `ModuleCoreConfig`, both under `src/main/java/com/codetaylor/mc/pyrotech/modules/core/`.
- **NF-SRC**: the decompiled NeoForge 21.1.249 sources this project compiles against, at
  `build/moddev/artifacts/neoforge-21.1.249-sources.jar`. Paths and line numbers cited below are
  entries in that jar.
- **NF-DOCS**: docs.neoforged.net, 1.21.1 version pages. URLs cited inline.
- **DATA**: the vanilla 1.21.1 data pack, at
  `build/moddev/artifacts/neoforge-21.1.249-client-extra-aka-minecraft-resources.jar`.
  Structure templates are `data/minecraft/structure/**/*.nbt` (gzipped NBT). Processor lists are
  `data/minecraft/worldgen/processor_list/*.json`. Template pools are
  `data/minecraft/worldgen/template_pool/**/*.json`.
- **CORE**: `docs/research/porting-notes-core.md` on branch `research/porting-notes-core`.

## Summary

- Vanilla 1.21.1 places the two blocks from 50 of 1,180 structure templates plus one code-built
  piece (the swamp hut). Villages dominate, but igloos, pillager outposts, trail ruins, ancient
  cities, and trial chambers also place them. Shipwrecks do not.
- A datapack can swap the blocks with vanilla `minecraft:rule` processors. No mod processor type
  is needed for that. But processor-list overrides alone reach only 32 of the 50 templates. The
  rest need template-pool overrides (four large files), one template NBT override (igloo), and
  the swamp hut cannot be reached by data at all.
- No NeoForge structure or worldgen hook in 21.1 touches placed blocks. `StructureModifier` only
  edits `StructureSettings`. There is no structure placement event.
- A first-load chunk scan on `ChunkEvent.Load` with `isNewChunk()` is the faithful replacement.
  It catches every source, vanilla or modded, exactly as the 1.12 scan did. Section palette
  checks make it cheap. It maps directly onto two runtime config toggles.
- Recommendation: port the scan. Ship no datapack overrides.

## What the 1.12 scan did

`PopulateChunkEventHandler.on(PopulateChunkEvent.Post)` at `EventPriority.LOWEST` (1.12, lines
19 to 20). It returns early when both toggles are off (lines 22 to 25). It scans the 16 by 16
column starting at chunk origin plus 8, the populate offset, over y 0 to 255 (lines 27 to 38).
A `CRAFTING_TABLE` becomes air with `setBlockToAir` (lines 46 to 51). A `FURNACE` or
`LIT_FURNACE` becomes cobblestone with `setBlockState` (lines 53 to 59). That is 65,536 block
reads per chunk, on every chunk, regardless of what generated in it.

| 1.12 construct | Fact |
|---|---|
| `TWEAKS.REMOVE_VANILLA_CRAFTING_TABLE` | Default `true`. Comment: "When a vanilla crafting table spawns in the world, for example in a village, the table is removed." (1.12 `ModuleCoreConfig` lines 24 to 29). |
| `TWEAKS.REPLACE_VANILLA_FURNACE` | Default `true`. Comment: "When a vanilla furnace spawns in the world, for example in a village, the furnace is replaced with cobblestone." (lines 31 to 36). |
| Scope | Every populated chunk. Any block source: vanilla structures, modded structures, features. |
| Timing | After population of the chunk. Existing chunks are never rescanned. |
| Blocks | Crafting table, furnace, lit furnace. Blast furnace and smoker did not exist in 1.12. |

## Where the two blocks come from in vanilla 1.21.1

All 1,180 templates in DATA were decompressed and searched for the block ids (see Verification).
67 templates contain at least one of crafting table, furnace, blast furnace, or smoker.
50 contain a crafting table or a furnace, the two blocks the 1.12 tweaks cover (29 plus 22,
with `igloo/top` in both sets).

| Block | Templates | Where |
|---|---|---|
| `minecraft:crafting_table` | 29 | `igloo/top`; `pillager_outpost/feature_tent1`, `feature_tent2`; 7 under `trail_ruins/`; `trial_chambers/intersection/intersection_2`; 18 village houses (desert 2, savanna 3, taiga 4, plus the 9 zombie copies). |
| `minecraft:furnace` | 22 | `ancient_city/city_center/city_center_2`; `igloo/top`; `trail_ruins/buildings/large_room_1`; 19 village houses (desert weaponsmith, plains weaponsmith, snowy 9, taiga 1, plus 7 zombie copies). |
| `minecraft:blast_furnace` | 9 | Every village armorer house, and two trail ruins rooms. |
| `minecraft:smoker` | 9 | Every village butcher shop. |

Villages in 1.21.1 are jigsaw structures. The five village structures start at
`village/<biome>/town_centers` (DATA `worldgen/structure/village_*.json`, field `start_pool`).
Pillager outpost, trail ruins, ancient city, and trial chambers are jigsaw too. Igloo is not:
`worldgen/structure/igloo.json` has `"type": "minecraft:igloo"` and no pool.

One code-built piece places a crafting table with no template and no processor: the swamp hut
(NF-SRC `net/minecraft/world/level/levelgen/structure/structures/SwampHutPiece.java`, line 74,
`this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), 3, 2, 6, box)`). No other
class under `structures/` references `Blocks.CRAFTING_TABLE`, `FURNACE`, `BLAST_FURNACE`, or
`SMOKER`.

Templates the issue asked about that place neither block: everything under `shipwreck/`,
`ruined_portal/`, `underwater_ruin/`, `woodland_mansion/`, `bastion/`, `end_city/`, `fossil/`,
and `nether_fossils/`. The scan checked all of them.

## Option A: datapack structure processors

### How processors run

A jigsaw element carries a `processors` field that is either a reference to a processor list or
an inline list (NF-SRC `net/minecraft/world/level/levelgen/structure/pools/SinglePoolElement.java`,
line 61, `StructureProcessorType.LIST_CODEC.fieldOf("processors")`). At placement the element
builds `StructurePlaceSettings` and adds, in order: `BlockIgnoreProcessor.STRUCTURE_BLOCK`,
`JigsawReplacementProcessor`, every processor in the element's list, then the projection's
processors (lines 180 to 188). Village houses are `legacy_single_pool_element`, which swaps
`STRUCTURE_BLOCK` for `STRUCTURE_AND_AIR` (NF-SRC `pools/LegacySinglePoolElement.java`, lines 36
to 37). The pool itself has no processor field. Its codec has `fallback` and `elements` only
(NF-SRC `pools/StructureTemplatePool.java`, lines 29 to 35).

`StructureTemplate.processBlockInfos` runs each block through the processor chain. A processor
that returns `null` drops the block. Otherwise the returned info replaces it (NF-SRC
`templatesystem/StructureTemplate.java`, lines 414 to 436). `placeInWorld` then calls
`serverLevel.setBlock(blockpos, blockstate, flags)` on each result (line 261). An `output_state`
of air is placed as air, because `BlockIgnoreProcessor` tests the template's input state, not
the output.

`RuleProcessor` tests each `ProcessorRule` in order and returns the first match's output state
plus its block entity modifier's output (NF-SRC `templatesystem/RuleProcessor.java`, lines 105 to
116). `ProcessorRule` has `input_predicate`, `location_predicate`, optional
`position_predicate`, `output_state`, and optional `block_entity_modifier` defaulting to
`passthrough` (NF-SRC `templatesystem/ProcessorRule.java`, lines 16 to 27).
`minecraft:block_match` tests `state.is(block)`, so it matches a furnace in any `facing` or
`lit` state (NF-SRC `templatesystem/BlockMatchTest.java`, line 22; `RuleTestType.java`, line 9).
The modifier type ids are `clear`, `passthrough`, `append_static`, `append_loot` (NF-SRC
`templatesystem/rule/blockentity/RuleBlockEntityModifierType.java`, lines 8 to 11).

Two vanilla rules do the job. No mod processor type is needed for a data-only version:

```json
{
  "processor_type": "minecraft:rule",
  "rules": [
    {
      "input_predicate": { "predicate_type": "minecraft:block_match", "block": "minecraft:crafting_table" },
      "location_predicate": { "predicate_type": "minecraft:always_true" },
      "output_state": { "Name": "minecraft:air" }
    },
    {
      "input_predicate": { "predicate_type": "minecraft:block_match", "block": "minecraft:furnace" },
      "location_predicate": { "predicate_type": "minecraft:always_true" },
      "output_state": { "Name": "minecraft:cobblestone" },
      "block_entity_modifier": { "type": "minecraft:clear" }
    }
  ]
}
```

This is the same shape vanilla uses. `zombie_desert.json` turns doors and torches into air with
`tag_match` and `block_match` rules (DATA `processor_list/zombie_desert.json`).
`mossify_10_percent.json` is a single `random_block_match` rule (DATA
`processor_list/mossify_10_percent.json`).

### What a processor-list override alone reaches

Every pool element that places one of the two blocks was traced to its `processors` value.

| Processor list | Pools that use it | Templates with the two blocks reached |
|---|---|---|
| `minecraft:mossify_10_percent` | `village/plains/houses`, `village/taiga/houses`, `village/taiga/town_centers` | Plains weaponsmith; taiga medium houses 2, 3, 4 and small houses 3, 4 (6 templates). |
| `minecraft:zombie_plains`, `zombie_taiga`, `zombie_savanna`, `zombie_desert`, `zombie_snowy` | The five `village/<biome>/zombie/houses` pools | All 16 zombie village templates with the two blocks, and the zombie pools' references to normal house templates. |
| `minecraft:trail_ruins_houses_archaeology` | 5 `trail_ruins/*` pools | All 8 trail ruins templates with the two blocks. |
| `minecraft:ancient_city_start_degradation` | `ancient_city/city_center` | `city_center_2`. |
| `minecraft:trial_chambers_copper_bulb_degradation` | 6 `trial_chambers/*` pools | `intersection/intersection_2`. |

Overriding those nine processor-list files reaches 32 of the 50 templates. The added rules only
fire on the two blocks, so the other pieces using those lists are unaffected.

### What it does not reach

| Gap | Why | Fix in data |
|---|---|---|
| Desert, savanna, and snowy village houses (15 templates: desert 3, savanna 3, snowy 9) | Every element in `village/desert/houses`, `village/savanna/houses`, and `village/snowy/houses` that places the two blocks has an inline `"processors": {"processors": []}`. There is no list to override (DATA `template_pool/village/desert/houses.json` and siblings; 85 vanilla pool files use inline processors, none reference `minecraft:empty`). | Override the three pool files (29, 32, and 31 elements each) and point each element at a Pyrotech list. |
| Pillager outpost tents (2 templates) | Same inline empty processors in `pillager_outpost/features` (8 elements). | Override that pool file. |
| Igloo `igloo/top` | Placed from code. `IglooPieces.makeSettings` adds only `BlockIgnoreProcessor.STRUCTURE_BLOCK` (NF-SRC `structures/IglooPieces.java`, lines 77 to 84). Not in any pool. | Ship an edited `data/minecraft/structure/igloo/top.nbt`. `StructureTemplateManager` reads templates from the resource manager, so a pack override wins (NF-SRC `templatesystem/StructureTemplateManager.java`, lines 79 and 121 to 127). |
| Swamp hut | `SwampHutPiece` places the table in code. No template, no processor. | None. |
| Other mods' structures | They reference their own pools and lists. | None, unless they happen to reuse a vanilla list. |

Total for a complete vanilla-only data solution: nine processor-list overrides, four
template-pool overrides, one NBT override, and one uncovered structure. Template-pool overrides
are whole-file replacements. Any other mod or datapack that overrides `village/desert/houses`
(village add-on mods do exactly this) collides, and the last pack wins.

### Cost and completeness

Runtime cost is zero. Coverage is vanilla only, minus the swamp hut. The 1.12 scan covered every
mod's structures. That is the realistic modpack case for Pyrotech, so this is the option's main
weakness.

## Option B: first-load chunk scan

### The hook

`ChunkEvent.Load` is posted on `NeoForge.EVENT_BUS` with `isNewChunk()`. The javadoc: "Check
whether the Chunk is newly generated, and being loaded for the first time. Will only ever return
true on the logical server." (NF-SRC `net/neoforged/neoforge/event/level/ChunkEvent.java`, lines
64 to 73). The server post is in `ChunkStatusTasks.full`:
`new ChunkEvent.Load(levelchunk, !(protochunk instanceof ImposterProtoChunk))` (NF-SRC
`net/minecraft/world/level/chunk/status/ChunkStatusTasks.java`, line 215). A freshly generated
`ProtoChunk` gives `true`. A chunk loaded from disk arrives wrapped in `ImposterProtoChunk` and
gives `false`. The client posts the event with `false` (NF-SRC
`net/minecraft/client/multiplayer/ClientChunkCache.java`, line 127), so the handler also checks
`event.getLevel() instanceof ServerLevel`.

The `full` task runs through `worldGenContext.mainThreadMailBox()` (line 221), so the handler is
on the server thread. Before the post, the task calls `levelchunk.setLoaded(true)` and wraps the
post in NeoForge's `currentlyLoading` bypass, whose comment reads "bypass the future chain when
getChunk is called, this prevents deadlocks" (lines 209 to 218). The event javadoc still warns:
"This event may be called before the underlying LevelChunk is promoted to FULL. You will cause
chunk loading deadlocks if you don't delay your level interactions." (line 47). The safe reading
is: touch this chunk only, never a neighbor.

### Why every structure block is already there

Structure pieces are placed during the `FEATURES` step. `ChunkStatusTasks.generateFeatures`
calls `applyBiomeDecoration` (NF-SRC `ChunkStatusTasks.java`, line 154), which places each
`StructureStart` clipped to the chunk's writable area (NF-SRC
`net/minecraft/world/level/chunk/ChunkGenerator.java`, lines 355 to 357). The status pyramid
gives `FEATURES` a `blockStateWriteRadius(1)` and makes `LIGHT` require every neighbor at
`INITIALIZE_LIGHT` radius 1 (NF-SRC `net/minecraft/world/level/chunk/status/ChunkPyramid.java`,
lines 35 to 43). Steps are sequential per chunk, so by the time a chunk reaches `FULL`, all
eight neighbors have finished `FEATURES`. Nothing generates into the chunk after the event.

This holds for every structure and feature, from any mod, because they all run inside the same
pipeline.

### Making it cheap

The 1.12 scan read 65,536 blocks per chunk. A 1.21 chunk is 384 tall, so a naive loop would read
98,304. Two vanilla checks avoid almost all of it:

- `LevelChunkSection.hasOnlyAir()` is a counter compare (NF-SRC
  `net/minecraft/world/level/chunk/LevelChunkSection.java`, lines 93 to 95).
- `LevelChunkSection.maybeHas(Predicate<BlockState>)` asks the section's palette (line 175).
  `SingleValuePalette` tests one value, `LinearPalette` and `HashMapPalette` iterate the
  palette entries, and `GlobalPalette` returns `true` (NF-SRC
  `net/minecraft/world/level/chunk/SingleValuePalette.java`, line 41; `LinearPalette.java`,
  line 64; `HashMapPalette.java`, line 51; `GlobalPalette.java`, line 26). A section only falls
  back to the global palette above 256 distinct states, which is rare in generated terrain.

So the per-chunk cost is 24 palette checks. A 4,096-block loop runs only in sections whose
palette may contain one of the blocks, which in practice means the sections that hold a
structure. An optional further filter, `chunk.hasAnyStructureReferences()` (NF-SRC
`net/minecraft/world/level/chunk/ChunkAccess.java`, lines 464 to 466), skips chunks no structure
touches, but it would also skip a modded feature that places a table outside a structure. The
palette check is cheap enough that the filter is not needed. Skipping it stays faithful.

### Writing the replacement

`level.setBlock(pos, state, Block.UPDATE_CLIENTS)` routes through `LevelChunk.setBlockState`,
which updates heightmaps and lighting, calls `onRemove` on the old state (which removes the
furnace's block entity), and calls `onPlace` on the new one (NF-SRC
`net/minecraft/world/level/chunk/LevelChunk.java`, lines 239 to 290). `UPDATE_CLIENTS` is flag
2 (NF-SRC `net/minecraft/world/level/block/Block.java`, line 78). Leaving out
`UPDATE_NEIGHBORS` (flag 1) avoids neighbor updates that could reach into a chunk that is not
yet full, which is the deadlock the javadoc warns about. The 1.12 `setBlockToAir` did notify
neighbors. For a table on a house floor the difference is nothing.

### Chunk attachments: available, not needed

`ChunkAccess implements IAttachmentHolder` (NF-SRC `ChunkAccess.java`, line 60, with the holder
at lines 494 to 555). Attachments persist through `ChunkSerializer` (NF-SRC
`net/minecraft/world/level/chunk/storage/ChunkSerializer.java`, lines 214 to 215 and 400 to
401). `AttachmentType`'s javadoc says modifications "should be followed by a call to
ChunkAccess#setUnsaved(boolean)" and that serializable attachments "are copied from a ProtoChunk
to a LevelChunk on promotion" (NF-SRC `net/neoforged/neoforge/attachment/AttachmentType.java`,
lines 50 to 53). NF-DOCS: attachments store data "on block entities, chunks, and entities" and
`setData` marks the chunk dirty automatically
(https://docs.neoforged.net/docs/1.21.1/datastorage/attachments).

A per-chunk "scanned" marker would let the mod also process chunks generated before Pyrotech was
added to a world. The 1.12 scan never did that. `isNewChunk()` alone reproduces 1.12 exactly, so
the attachment is an extension, not a requirement. It is listed as a decision below.

### Cost and completeness

Coverage matches 1.12: every new chunk, every block source, vanilla or modded, including the
swamp hut and igloo. Cost is 24 palette checks per new chunk on the server thread, plus a
section loop where a match is possible. That is far below the 1.12 cost.

## Option C: NeoForge structure and worldgen hooks

Checked in NF-SRC. None of them reach placed blocks.

| Hook | What it can change | Verdict |
|---|---|---|
| `StructureModifier` (`net/neoforged/neoforge/common/world/StructureModifier.java`) | `ModifiableStructureInfo.StructureInfo` wraps only `StructureSettings`. `StructureSettingsBuilder` exposes biomes, spawn overrides, decoration step, terrain adaptation (`StructureSettingsBuilder.java`, lines 134 to 228). | No access to pieces, templates, or processors. |
| `BiomeModifier` | Features, spawns, climate, colors, carvers (NF-DOCS https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier). | Not structures. |
| Events under `net/neoforged/neoforge/event/level/` | `ChunkEvent`, `ChunkDataEvent`, `ChunkWatchEvent`, `ChunkTicketLevelUpdatedEvent`, `LevelEvent`, `BlockEvent`, `AlterGroundEvent`, `BlockGrowFeatureEvent`, `ModifyCustomSpawnersEvent`, and others. | No structure placement event. `ChunkEvent.Load` is the only one that sees a finished new chunk. |
| `net/neoforged/neoforge/common/world/` | Biome and structure modifiers, POI extension, forced chunk tickets, auxiliary light. | Nothing block-level. |
| NeoForge patches in `StructureTemplate`, `StructurePlaceSettings`, `JigsawPlacement`, `StructureStart`, `WorldGenRegion` | `StructureTemplate` adds a `template` parameter to `process` and entity processing. The other four have no NeoForge lines. | No global processor injection point. |

`ChunkDataEvent.Load` fires from `ChunkSerializer.read`, which the javadoc calls async (NF-SRC
`ChunkDataEvent.java`, lines 138 to 140). It fires for chunks read from disk, not new ones. Not
useful here.

## Comparison

| | A: datapack processors | B: first-load scan | C: NeoForge hooks |
|---|---|---|---|
| Reaches vanilla templates | 32 of 50 with list overrides; all 50 with pool and NBT overrides too | All | None |
| Reaches swamp hut | No | Yes | No |
| Reaches other mods' structures | No | Yes | No |
| Runtime cost | None | 24 palette checks per new chunk | n/a |
| Files overridden in `minecraft` namespace | 9 lists, 4 pools, 1 NBT | 0 | n/a |
| Conflict with other packs | High (pool files) | None | n/a |
| Toggle mapping | Pack-level only | Runtime config, exact 1.12 shape | n/a |
| Faithful to 1.12 | Partly | Yes | No |

## Mapping the two toggles

### Runtime config (fits option B, or a mod processor type in option A)

`ModConfigSpec` is built with `ModConfigSpec.Builder` and registered in the mod constructor with
`ModContainer#registerConfig` (NF-DOCS https://docs.neoforged.net/docs/1.21.1/misc/config/).
`ConfigValue.get()` returns a cached value after the first read (NF-SRC
`net/neoforged/neoforge/common/ModConfigSpec.java`, lines 1220 to 1224), so reading two booleans
per chunk is free. Two `BooleanValue`s named after the 1.12 fields, default `true`, in the core
`Tweaks` section that CORE decision 3 already proposes.

Type choice, from NF-DOCS: `COMMON` is "Loaded on both the physical client and physical server"
and read before `FMLCommonSetupEvent`. `SERVER` is read before `ServerAboutToStartEvent`, "Can be
overridden for each world" via `saves/<world>/serverconfig`, and is "Synced across the network to
the client." Worldgen only runs on the server, so either works. CORE decision 3 puts the tweak
toggles in one common config. Keeping these two there is consistent.

If option A were chosen with runtime toggles, a mod-supplied `StructureProcessorType` registered
under `Registries.STRUCTURE_PROCESSOR` (NF-SRC `net/minecraft/core/registries/Registries.java`,
line 194) could read the config inside `processBlock`. The processor-list and pool overrides
would still be needed to attach it. That combines the conflict surface of A with the code of B.

### Datapack toggle (option A)

Two mechanisms were checked. One is unsafe here, one works.

`neoforge:conditions` on a datapack registry entry: `RegistryDataLoader.loadElementFromResource`
wraps every entry codec in `ConditionalOps.createConditionalCodec`, and when the conditions fail
it logs "Skipping loading registry entry {} as its conditions were not met" and registers
nothing (NF-SRC `net/minecraft/resources/RegistryDataLoader.java`, lines 199 to 218). The
resource manager yields one file per id, so a Pyrotech override of `village/desert/houses` with
a false condition removes the vanilla pool instead of falling back to it. Village generation
would break. NF-DOCS confirms the semantics: "Loading will continue if and only if all conditions
pass, otherwise the data file will be ignored."
(https://docs.neoforged.net/docs/1.21.1/resources/server/conditions). Conditions cannot gate
overrides of vanilla files. There is also no built-in config-reading condition; the built-ins
are `true`, `false`, `not`, `and`, `or`, `mod_loaded`, `item_exists`, `tag_empty` (same page).

A separate built-in pack: `AddPackFindersEvent.addPackFinders(packLocation, packType,
packNameDisplay, packSource, alwaysActive, packPosition)` registers a pack found under the mod's
`resources/` folder. The javadoc on `packSource` says it "Controls whether the datapack is
enabled or disabled by default" and on `alwaysActive`: "If false, players have to manually
activate the pack themselves" (NF-SRC `net/neoforged/neoforge/event/AddPackFindersEvent.java`,
lines 61 to 87). This is the only user-facing toggle for data. It is coarse: one pack, one
switch, not two. The automatic mod data pack cannot be disabled at all. NF-DOCS: "It is currently
not possible to disable mod data packs." (https://docs.neoforged.net/docs/1.21.1/resources/).
Two toggles would mean two built-in packs.

## Recommendation

Port the scan. Replace `PopulateChunkEventHandler` with a `ChunkEvent.Load` listener in core:

1. Return unless `event.getLevel() instanceof ServerLevel`, `event.isNewChunk()`, and at least
   one of the two config toggles is on. Same early-out shape as 1.12.
2. For each `LevelChunkSection` in `chunk.getSections()`, skip when `hasOnlyAir()` or when
   `maybeHas(state -> state.is(CRAFTING_TABLE) || state.is(FURNACE))` is false.
3. In the remaining sections, loop the 4,096 positions. Crafting table becomes air. Furnace
   becomes cobblestone. Write with `level.setBlock(pos, state, Block.UPDATE_CLIENTS)`.
4. Two `BooleanValue`s in the common config, default `true`, same names and comments as 1.12.

Ship no `minecraft` namespace overrides for this feature. The data route reaches less, conflicts
with village mods, cannot be toggled per feature, and cannot reach the swamp hut.

The scan is faithful in scope, faithful in timing (new chunks only), faithful in toggles, and
cheaper than the original. If testing ever shows a problem with writing inside the `full` task,
the fallback is to queue the chunk position with `server.execute(...)` and process it on the next
tick, checking `getChunkSource().getChunkNow(x, z)` first. That was not needed in the sources
read for this note and is not part of the recommendation.

## Verification

- Template scan: all 1,180 `.nbt` files under `data/minecraft/structure/` in DATA were read with
  Python `gzip` and searched for the byte strings `minecraft:crafting_table`, `minecraft:furnace`,
  `minecraft:blast_furnace`, and `minecraft:smoker`. `minecraft:furnace` is not a substring of
  `minecraft:blast_furnace`, so the counts do not overlap by accident. 67 templates matched.
  Per-template results are the basis of the tables above.
- Pool tracing: all 186 `template_pool` JSON files were parsed and every element's `location`
  and `processors` recorded. Each matched template was joined to the pools that reference it.
  `igloo/top` is the only matched template referenced by no pool.
- Processor-list inventory: all 40 `processor_list` files were parsed for their
  `processor_type` values. Every list touched above is a `minecraft:rule` list, so appending two
  rules is a well-formed edit.
- Code-built pieces: every class under `structures/` in NF-SRC was grepped for the four block
  constants. Only `SwampHutPiece` matched.
- Chunk pipeline: `ChunkPyramid`, `ChunkStatusTasks`, and `ChunkGenerator.applyBiomeDecoration`
  were read to confirm structures place in `FEATURES` and that `FULL` follows all neighbor
  `FEATURES`. The `ChunkEvent.Load` post site and thread were confirmed in `ChunkStatusTasks`.
- NeoForge hooks: `net/neoforged/neoforge/event/level/` and `net/neoforged/neoforge/common/world/`
  were listed in full. `StructureModifier` and `StructureSettingsBuilder` were read to confirm
  they stop at `StructureSettings`.
- Not verified by running code. No client was launched. The `currentlyLoading` bypass and the
  `UPDATE_CLIENTS` write should be exercised in the first tracer of the core port.

## Decisions for Moos

1. **Mechanism.** Options: (a) first-load chunk scan on `ChunkEvent.Load`; (b) datapack
   processor-list, pool, and NBT overrides; (c) both. Recommendation: (a). It is the faithful
   port, it catches other mods' villages, and it needs no `minecraft` namespace files. (c) adds
   nothing the scan does not already do.
2. **Blast furnace and smoker.** 1.12 predates both. Every vanilla armorer house places a blast
   furnace and every butcher shop places a smoker (9 templates each). Both skip Pyrotech's early
   game the same way a furnace does. Options: leave them, fold them into
   `REPLACE_VANILLA_FURNACE`, or add a third toggle. Recommendation: fold them into
   `REPLACE_VANILLA_FURNACE`, replaced with cobblestone, and say so in the config comment. The
   1.12 comment describes intent, not a block list.
3. **Existing chunks.** The 1.12 scan only touched newly generated chunks. A chunk attachment
   marker could extend the scan to chunks generated before Pyrotech joined a world.
   Recommendation: no. Match 1.12. Adding the mod to an existing world is not a supported path
   for a progression overhaul.
4. **Config type.** `COMMON` (one file, consistent with CORE decision 3) or `SERVER` (per-world
   override, synced). Recommendation: `COMMON`, alongside the other tweak toggles.
5. **Structure-reference filter.** `chunk.hasAnyStructureReferences()` would skip chunks no
   structure touches, at the cost of missing modded features that place the blocks outside a
   structure. Recommendation: do not add it. The palette check already makes the scan cheap, and
   skipping it keeps 1.12's "any source" scope.
