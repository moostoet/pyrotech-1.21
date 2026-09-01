# Asset conversion report

This report comes from `scripts/asset-conversion/run.sh`.
It covers the conversion of the 1.12 assets to the 1.21.1 formats.

## Summary

| Category | 1.12 files | Converted | Flagged for hand conversion |
|---|---|---|---|
| Blockstates | 129 | 117 | 12 |
| Models | 421 | 421 | 19 (converted, but check the flag) |
| Lang | 6 | 6 | 0 |
| Textures | 609 + 26 mcmeta | all copied | 0 |
| Sounds | 18 + sounds.json | all copied | 0 |

The blockstate conversion wrote 347 generated wrapper models under `models/block/gen/`.
Wrappers replace the Forge 1.12 texture substitution, which vanilla blockstates do not support.
17 blockstates became multipart files. Multipart replaces the Forge 1.12 submodel feature.

Converted lang files: en_us.lang (564 keys), es_es.lang (565 keys), es_mx.lang (564 keys), ru_ru.lang (564 keys), uk_ua.lang (638 keys), zh_cn.lang (564 keys).

## Flagged for hand conversion

### Blockstates (12 files)

- `blockstates/bucket_clay.json`: uses a custom model loader from Forge 1.12; needs new item or fluid rendering
- `blockstates/bucket_refractory.json`: uses a custom model loader from Forge 1.12; needs new item or fluid rendering
- `blockstates/bucket_stone.json`: uses a custom model loader from Forge 1.12; needs new item or fluid rendering
- `blockstates/bucket_wood.json`: uses a custom model loader from Forge 1.12; needs new item or fluid rendering
- `blockstates/fluid.json`: uses a custom model loader from Forge 1.12; needs new item or fluid rendering
- `blockstates/masonry_brick_slab.json`: slabs changed in 1.13 (half=top/bottom became type=top/bottom/double); rebuild from the 1.21.1 slab template
- `blockstates/masonry_brick_wall.json`: wall properties changed in 1.16 (true/false became none/low/tall); rebuild from the 1.21.1 wall template
- `blockstates/material.json`: an item-variants blockstate from 1.12; each variant becomes its own item model during hoisting
- `blockstates/refractory_brick_slab.json`: slabs changed in 1.13 (half=top/bottom became type=top/bottom/double); rebuild from the 1.21.1 slab template
- `blockstates/refractory_brick_wall.json`: wall properties changed in 1.16 (true/false became none/low/tall); rebuild from the 1.21.1 wall template
- `blockstates/refractory_door.json`: door models were redesigned after 1.12; rebuild from the 1.21.1 door template
- `blockstates/stone_door.json`: door models were redesigned after 1.12; rebuild from the 1.21.1 door template

### Models (converted, but they need a check) (19 files)

- `models/item/bone_spear.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/crude_spear.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/flint_spear.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/marshmallow_stick.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/crude_fishing_rod.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/crude_shield.json`: parent builtin/entity needs a special renderer in 1.21.1; has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/crude_shield_blocking.json`: parent builtin/entity needs a special renderer in 1.21.1
- `models/item/tool/durable_shield.json`: parent builtin/entity needs a special renderer in 1.21.1; has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/durable_shield_blocking.json`: parent builtin/entity needs a special renderer in 1.21.1
- `models/item/tool/quartz_axe.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/quartz_hoe.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/quartz_pickaxe.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/quartz_shovel.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/quartz_sword.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/redstone_axe.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/redstone_hoe.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/redstone_pickaxe.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/redstone_shovel.json`: has item overrides; verify the predicates still exist in 1.21.1
- `models/item/tool/redstone_sword.json`: has item overrides; verify the predicates still exist in 1.21.1

## Notes on converted blockstates

- `blockstates/masonry_brick_stairs.json`: stairs: state properties are unchanged since 1.12; verify in game
- `blockstates/refractory_brick_stairs.json`: stairs: state properties are unchanged since 1.12; verify in game

## Item models to create during hoisting

These 1.12 blockstates had an `inventory` variant. Vanilla 1.21.1 has no inventory variants.
Each block needs an item model when its module is hoisted.

- `dense_coal_ore`
- `dense_nether_coal_ore`
- `fossil_ore`
- `refractory_glass`
- `slag_glass`

## Lang notes

The en_us file has 114 block names, 248 item names, and 201 other keys.
Block and item keys follow the modern pattern: `tile.pyrotech.x.y.name` becomes `block.pyrotech.x_y`.
The dots in the old key become underscores in the guessed registry name.

These keys renamed vanilla content in 1.12. Verify each rename is still wanted:
- `item.arrow.name`

For 85 keys, the guessed registry name matches no 1.12 blockstate or item model file.
The 1.12 Java code holds the real registry names. Check these keys during module hoisting:

- `block.pyrotech.igniter_stone`
- `block.pyrotech.igniter_brick`
- `block.pyrotech.drying_rack_crude`
- `block.pyrotech.drying_rack_normal`
- `block.pyrotech.bloom_unique`
- `block.pyrotech.pile_slag_unique`
- `block.pyrotech.tar_collector_stone`
- `block.pyrotech.tar_collector_brick`
- `block.pyrotech.tar_drain_stone`
- `block.pyrotech.tar_drain_brick`
- `block.pyrotech.cobblestone_andesite`
- `block.pyrotech.cobblestone_diorite`
- `block.pyrotech.cobblestone_granite`
- `block.pyrotech.cobblestone_limestone`
- `block.pyrotech.dense_redstone_ore`
- `block.pyrotech.dense_quartz_ore`
- `item.pyrotech.bucket_clay`
- `item.pyrotech.bucket_clay_empty`
- `item.pyrotech.bucket_clay_milk`
- `item.pyrotech.bucket_stone`
- `item.pyrotech.bucket_stone_empty`
- `item.pyrotech.bucket_stone_milk`
- `item.pyrotech.bucket_wood`
- `item.pyrotech.bucket_wood_empty`
- `item.pyrotech.bucket_wood_milk`
- `item.pyrotech.bucket_refractory`
- `item.pyrotech.bucket_refractory_empty`
- `item.pyrotech.bucket_refractory_milk`
- `item.pyrotech.marshmallow_on_stick_roasted`
- `item.pyrotech.marshmallow_on_stick_burned`
- `item.pyrotech.slag_unique`
- `item.pyrotech.straw`
- `item.pyrotech.coal_coke`
- `item.pyrotech.flint_clay_ball`
- `item.pyrotech.refractory_clay_ball`
- `item.pyrotech.refractory_clay_lump`
- `item.pyrotech.unfired_refractory_brick`
- `item.pyrotech.unfired_brick`
- `item.pyrotech.refractory_brick`
- `item.pyrotech.pottery_fragments`
- `item.pyrotech.pottery_shard`
- `item.pyrotech.quicklime`
- `item.pyrotech.slaked_lime`
- `item.pyrotech.flint_shard`
- `item.pyrotech.bone_shard`
- `item.pyrotech.plant_fibers`
- `item.pyrotech.plant_fibers_dried`
- `item.pyrotech.twine`
- `item.pyrotech.charcoal_flakes`
- `item.pyrotech.brick_stone`
- `item.pyrotech.clay_lump`
- `item.pyrotech.iron_shard`
- `item.pyrotech.diamond_shard`
- `item.pyrotech.board`
- `item.pyrotech.coal_pieces`
- `item.pyrotech.board_tarred`
- `item.pyrotech.pulp`
- `item.pyrotech.twine_durable`
- `item.pyrotech.stick_stone`
- `item.pyrotech.dust_limestone`
- `item.pyrotech.kindling`
- `item.pyrotech.kindling_tarred`
- `item.pyrotech.dust_flint`
- `item.pyrotech.glass_shard`
- `item.pyrotech.obsidian_shard`
- `item.pyrotech.gold_shard`
- `item.pyrotech.dense_redstone`
- `item.pyrotech.dense_quartz`
- `item.pyrotech.leather_sheet`
- `item.pyrotech.leather_strap`
- `item.pyrotech.leather_cord`
- `item.pyrotech.leather_durable`
- `item.pyrotech.leather_durable_sheet`
- `item.pyrotech.leather_durable_strap`
- `item.pyrotech.leather_durable_cord`
- `item.pyrotech.leather_small`
- `item.pyrotech.fletching`
- `item.pyrotech.stone_tool_shaft`
- `item.pyrotech.bow_drill_durable_stick`
- `item.pyrotech.lard`
- `item.pyrotech.dough`
- `item.pyrotech.flour`
- `item.pyrotech.bread_dough`
- `item.pyrotech.cookie_dough`
- `item.pyrotech.clay_blasting`

## Warnings

Each warning names the converted file and the reference that did not resolve.

- `blockstates/torch_backup.json`: pyrotech texture missing: block/torch_doused
- `blockstates/torch_backup.json`: pyrotech texture missing: block/torch_off
- `blockstates/torch_backup.json`: pyrotech texture missing: block/torch_on

## Not converted on purpose

- Recipes, advancements, and loot tables move to Java datagen. The map decided this.
- The Patchouli book (`patchouli_books/`, 1185 files) has its own ticket later in the porting order.
