# Module dependencies in the 1.12 source

Supporting analysis for issue #4 (module porting order).
All data comes from the `1.12` branch, read with `git grep` and `git show`.
Counts are files, not import lines, unless stated otherwise.

## Dependency matrix

Each cell counts files in the row module that reference the column module.
Raw counts include false positives. The corrected view follows the table.

| depender \ dependee | core | tech | storage | tool | ignition | hunting | bucket | worldgen |
|---|---|---|---|---|---|---|---|---|
| core     | -   | 3   | 0 | 6 | 0 | 9 | 0 | 0 |
| tech     | 113 | -   | 3 | 2 | 6 | 3 | 2 | 0 |
| storage  | 18  | 3   | - | 0 | 0 | 0 | 0 | 0 |
| tool     | 18  | 10  | 0 | - | 0 | 0 | 0 | 0 |
| ignition | 6   | 3   | 2 | 0 | - | 1 | 0 | 0 |
| hunting  | 14  | 3   | 0 | 0 | 0 | - | 0 | 0 |
| bucket   | 0   | 0   | 0 | 0 | 0 | 0 | - | 0 |
| worldgen | 11  | 0   | 0 | 0 | 0 | 0 | 0 | - |

Two corrections shrink this matrix a lot:

- Every module class declares `MOD_ID = ModPyrotech.MOD_ID`. A reference to
  another module's `MOD_ID` is a string constant, not a dependency. This removes
  all 10 files in tool to tech, 2 of 3 in hunting to tech, 1 of 3 in storage to
  tech, and 2 of 3 in core to tech.
- Much of the coupling sits in `plugin/` subpackages (CraftTweaker, TOP, Waila).
  Those integrations are out of scope for the port. This removes 15 of 18 files
  in storage to core and all 3 in storage to tech.

After correction, the real edges are:

- Every module depends on core. Core provides the registry holders,
  `ItemMaterial`, `BlockRock`, config, and the shared network packets.
- Core has hard back references into hunting (9 files), tool (6), and tech (1
  real: the furnace fuel handler uses `BlockCampfire` and `BlockWorktableStone`).
  These are 1.12 code-organization accidents. The rewrite moves each one into
  the module that owns the behavior.
- Tech depends on storage in three tiles: `TileSoakingPot`,
  `TileMechanicalMulchSpreader` (uses `TileStash`), and `TileTarTankBase`.
- Tech and ignition form a real cycle. Four blocks (campfire, pit kiln,
  bloomery, stone combustion worker) accept `ItemIgniterBase` in their ignite
  paths. Ignition in turn uses `RefractoryIgnitionHelper` from tech/refractory.
- Ignition inherits `TileTankBase` from storage in one tile.
- Hunting borrows tech/basic's network channel in one file
  (`InteractionCarcass` sends `SCPacketNoHunger`).
- Bucket depends on nothing. Worldgen depends only on core. Nothing depends on
  either of them, apart from guarded recipe registration in tech's init code.

## Module sizes

| module | files | lines of Java | athenaeum import lines |
|---|---|---|---|
| tech | 427 | 60,976 | 725 |
| core | 166 | 14,409 | 99 |
| storage | 72 | 8,809 | 204 |
| hunting | 66 | 5,346 | 79 |
| tool | 38 | 2,866 | 11 |
| ignition | 30 | 3,116 | 43 |
| worldgen | 17 | 1,903 | 10 |
| bucket | 12 | 1,760 | 8 |

About 38% of tech (161 files) and 38% of storage (27 files) is `plugin/`
integration code that the port drops or replaces with the JEI plugin.

## Tech is four modules in one

Tech's four subpackages each register as a separate module with their own
config: basic, machine, bloomery, refractory.

| tech submodule | files | lines | depends on siblings |
|---|---|---|---|
| basic | 177 | 29,068 | machine (6), bloomery (3), refractory (2), mostly guarded init code |
| machine | 150 | 18,618 | basic (22) |
| bloomery | 60 | 8,764 | basic (15) |
| refractory | 40 | 4,526 | none |

Basic is the hub. Machine and bloomery build on it. Refractory stands alone
inside tech. Outside modules point at specific submodules only: tool and
hunting at basic, ignition and storage at refractory.

## Shared code outside the modules

- `library/` holds 59 files. The `spi/` part (17 files) is the shared
  inheritance layer: tile worker bases, ignitable block interfaces, JEI
  recipe category bases. Tech consumes 162 of the 219 module files that
  touch `library/`.
- `packer/` (texture atlas, 18 files) and `api/` (1 file) have zero module
  imports. They can be dropped or deferred.

## Athenaeum hotspots

The port drops the athenaeum library, so heavy use means rewrite effort.
Two athenaeum systems dominate:

- `interaction.spi` and `interaction.api`: the multi-part right-click
  interaction framework. 250 import lines, of which 173 are in tech and 39
  in storage.
- `network.tile`: tile entity field sync packets. 175 import lines, of which
  114 are in tech and 31 in storage.

Tool, bucket, and worldgen are nearly athenaeum-free (11 or fewer import
lines each).

## Registration order in 1.12

`ModPyrotech` registers `ModuleCore` first, unconditionally. Then each
optional module behind a config flag, in alphabetical order, not dependency
order. `ModuleCorePost` runs last and only registers ore dictionary entries.
The per-module config flags all default to true. The back references from
core into hunting, tool, and tech are not guarded by those flags, so 1.12
crashes if those modules are compiled out. The flags only skip registration.
