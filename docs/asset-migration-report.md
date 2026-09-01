# Asset migration report

This report covers ticket 7 (migrate converted assets into main).
It lists what landed on `main` and what the code port still has to do.
The full conversion detail is in [asset-conversion-report.md](asset-conversion-report.md).

## What landed on main

All files live under `src/main/resources/assets/pyrotech/`.

- 117 blockstates.
- 768 models. 421 are converted 1.12 models. 347 are generated wrapper models under `models/block/gen/`.
- 609 textures and 26 animation `.mcmeta` files.
- 6 lang files. The empty template `en_us.json` was replaced by the converted file (564 keys).
- 18 sound files and `sounds.json`.

Checks done during migration:

- All 919 JSON files parse.
- Every entry in `sounds.json` points at a sound file that exists. No sound file is unreferenced.
- The game boots with no resource errors for the `pyrotech` namespace.

The boot check has a limit. No blocks or items are registered yet.
The game only loads blockstates, models, and textures for registered content.
So the boot only exercises the lang files, `sounds.json`, and the texture `.mcmeta` files.
Each module hoist must verify its own blockstates and models in game.

## Deferred to the code port

Section names below refer to [asset-conversion-report.md](asset-conversion-report.md).

- 12 blockstates were not converted and are not on `main`. Buckets and fluids need new rendering. Slabs, walls, and doors need rebuilds from the 1.21.1 templates. See "Flagged for hand conversion".
- 19 item models converted but need checks. Spears, rods, and tools use item overrides whose predicates may be gone. Shields need a special renderer for `builtin/entity`. See "Models".
- 5 blocks need new item models, because 1.21.1 has no inventory variants. See "Item models to create during hoisting".
- 85 lang keys have guessed registry names that match no model file. Verify each against the 1.12 Java code during hoisting. See "Lang notes".
- `blockstates/torch_backup.json` references three textures that do not exist. Fix during the ignition module.
- The lang key `item.minecraft.arrow` renames the vanilla arrow to "Iron Arrow". This is faithful to 1.12 and is active as of this migration. Confirm it is still wanted during the hunting module.
