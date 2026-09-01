import argparse
from pathlib import Path

from blockstates import convert_blockstates
from langs import convert_langs
from models import convert_models
from refs import RefContext
from static_assets import copy_sounds, copy_textures

SCRIPT_DIR = Path(__file__).parent


def validate_mod_refs(ctx, texture_paths, out_models_dir):
    model_paths = {
        str(model_file.relative_to(out_models_dir))[:-5]
        for model_file in Path(out_models_dir).rglob("*.json")
    }
    for source, path in sorted(ctx.mod_texture_refs):
        if path not in texture_paths:
            ctx.warn(source, f"pyrotech texture missing: {path}")
    for source, path in sorted(ctx.mod_model_refs):
        if path not in model_paths:
            ctx.warn(source, f"pyrotech model missing: {path}")


def flagged_section(lines, title, flagged):
    lines.append(f"### {title} ({len(flagged)} files)")
    lines.append("")
    for source, reason in sorted(flagged.items()):
        if isinstance(reason, list):
            reason = "; ".join(reason)
        lines.append(f"- `{source}`: {reason}")
    lines.append("")


def write_report(path, blockstates, models, langs, textures, sounds, ctx):
    lines = ["# Asset conversion report", ""]
    lines.append("This report comes from `scripts/asset-conversion/run.sh`.")
    lines.append("It covers the conversion of the 1.12 assets to the 1.21.1 formats.")
    lines.append("")

    lines.append("## Summary")
    lines.append("")
    lines.append("| Category | 1.12 files | Converted | Flagged for hand conversion |")
    lines.append("|---|---|---|---|")
    bs_total = blockstates["converted"] + len(blockstates["flagged"])
    lines.append(f"| Blockstates | {bs_total} | {blockstates['converted']} | {len(blockstates['flagged'])} |")
    lines.append(f"| Models | {models['converted']} | {models['converted']} | {len(models['flagged'])} (converted, but check the flag) |")
    lang_files = ", ".join(f"{name} ({count} keys)" for name, count in langs["files"])
    lines.append(f"| Lang | {len(langs['files'])} | {len(langs['files'])} | 0 |")
    lines.append(f"| Textures | {textures['pngs']} + {textures['mcmeta']} mcmeta | all copied | 0 |")
    lines.append(f"| Sounds | {sounds['oggs']} + sounds.json | all copied | 0 |")
    lines.append("")
    lines.append(f"The blockstate conversion wrote {blockstates['wrappers']} generated wrapper models under `models/block/gen/`.")
    lines.append("Wrappers replace the Forge 1.12 texture substitution, which vanilla blockstates do not support.")
    lines.append(f"{blockstates['multipart']} blockstates became multipart files. Multipart replaces the Forge 1.12 submodel feature.")
    lines.append("")
    lines.append(f"Converted lang files: {lang_files}.")
    lines.append("")

    lines.append("## Flagged for hand conversion")
    lines.append("")
    flagged_section(lines, "Blockstates", blockstates["flagged"])
    flagged_section(lines, "Models (converted, but they need a check)", models["flagged"])

    if blockstates["notes"]:
        lines.append("## Notes on converted blockstates")
        lines.append("")
        for source, note in sorted(blockstates["notes"].items()):
            lines.append(f"- `{source}`: {note}")
        lines.append("")

    lines.append("## Item models to create during hoisting")
    lines.append("")
    lines.append("These 1.12 blockstates had an `inventory` variant. Vanilla 1.21.1 has no inventory variants.")
    lines.append("Each block needs an item model when its module is hoisted.")
    lines.append("")
    for name in sorted(blockstates["inventory_variants"]):
        lines.append(f"- `{name}`")
    lines.append("")

    lines.append("## Lang notes")
    lines.append("")
    counts = langs["counts"]
    lines.append(f"The en_us file has {counts['block']} block names, {counts['item']} item names, and {counts['other']} other keys.")
    lines.append("Block and item keys follow the modern pattern: `tile.pyrotech.x.y.name` becomes `block.pyrotech.x_y`.")
    lines.append("The dots in the old key become underscores in the guessed registry name.")
    lines.append("")
    if langs["vanilla_overrides"]:
        lines.append("These keys renamed vanilla content in 1.12. Verify each rename is still wanted:")
        for key in langs["vanilla_overrides"]:
            lines.append(f"- `{key}`")
        lines.append("")
    unmatched = langs["unmatched_blocks"] + langs["unmatched_items"]
    if unmatched:
        lines.append(f"For {len(unmatched)} keys, the guessed registry name matches no 1.12 blockstate or item model file.")
        lines.append("The 1.12 Java code holds the real registry names. Check these keys during module hoisting:")
        lines.append("")
        for key in unmatched:
            lines.append(f"- `{key}`")
        lines.append("")
    if langs["duplicates"]:
        lines.append("Duplicate keys (the last value wins):")
        for file_name, key in langs["duplicates"]:
            lines.append(f"- {file_name}: `{key}`")
        lines.append("")

    lines.append("## Warnings")
    lines.append("")
    if ctx.warnings:
        lines.append("Each warning names the converted file and the reference that did not resolve.")
        lines.append("")
        for source, message in sorted(set(ctx.warnings)):
            lines.append(f"- `{source}`: {message}")
    else:
        lines.append("None. Every reference resolved.")
    lines.append("")

    lines.append("## Not converted on purpose")
    lines.append("")
    lines.append("- Recipes, advancements, and loot tables move to Java datagen. The map decided this.")
    lines.append("- The Patchouli book (`patchouli_books/`, 1185 files) has its own ticket later in the porting order.")
    lines.append("")

    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text("\n".join(lines), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description="Convert 1.12 Pyrotech assets to 1.21.1 formats.")
    parser.add_argument("--src", required=True, help="extracted 1.12 assets/pyrotech directory")
    parser.add_argument("--out", required=True, help="output assets/pyrotech directory")
    parser.add_argument("--vanilla-jar", default=None, help="minecraft 1.21.1 client jar, used to validate vanilla references")
    parser.add_argument("--report", required=True, help="path of the coverage report to write")
    args = parser.parse_args()

    src = Path(args.src)
    out = Path(args.out)
    ctx = RefContext(args.vanilla_jar, SCRIPT_DIR / "vanilla_renames.json")
    if not args.vanilla_jar:
        ctx.warn("setup", "no vanilla jar given; vanilla model and texture references were not checked")

    textures = copy_textures(src / "textures", out / "textures")
    sounds = copy_sounds(src, out, ctx)
    models = convert_models(src / "models", out / "models", ctx)
    blockstates = convert_blockstates(src / "blockstates", out / "blockstates", out / "models", ctx)

    block_names = {state_file.stem for state_file in (src / "blockstates").glob("*.json")}
    item_names = {model_file.stem for model_file in (src / "models" / "item").rglob("*.json")}
    langs = convert_langs(src / "lang", out / "lang", block_names, item_names)

    validate_mod_refs(ctx, textures["paths"], out / "models")
    write_report(args.report, blockstates, models, langs, textures, sounds, ctx)

    print(f"blockstates: {blockstates['converted']} converted, {len(blockstates['flagged'])} flagged")
    print(f"models: {models['converted']} converted, {len(models['flagged'])} flagged")
    print(f"wrappers: {blockstates['wrappers']} generated")
    print(f"warnings: {len(set(ctx.warnings))}")
    print(f"report: {args.report}")


if __name__ == "__main__":
    main()
