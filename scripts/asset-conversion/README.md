# Asset conversion

These scripts convert the 1.12 Pyrotech assets to the 1.21.1 formats.
They read the assets from the `1.12` branch and write into `src/main/resources/assets/pyrotech`.

## Run

```bash
./scripts/asset-conversion/run.sh
```

The run is repeatable. It deletes the previous output first.
It writes the coverage report to `docs/asset-conversion-report.md`.

## What each part does

- `convert.py`: the entry point. It runs the steps below and writes the report.
- `langs.py`: converts `.lang` files to modern `.json` lang files.
- `blockstates.py`: converts Forge 1.12 blockstates to the vanilla format.
  Texture substitution becomes generated wrapper models under `models/block/gen/`.
  Submodels become multipart blockstates.
- `models.py`: rewrites texture folders (`blocks/` to `block/`, `items/` to `item/`) and checks references.
- `static_assets.py`: copies textures and sounds, and checks `sounds.json`.
- `refs.py`: shared reference rewriting and validation.
- `vanilla_renames.json`: vanilla texture and model names that changed since 1.12 (the 1.13 flattening).

## Validation

When the Minecraft 1.21.1 client jar is in the Gradle cache, the run checks every
vanilla texture and model reference against it. Files the scripts cannot convert
are listed in the report with a reason.
