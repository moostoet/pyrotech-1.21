import json
import shutil
from pathlib import Path

from refs import rename_folder, split_ref


def copy_textures(src_dir, out_dir):
    src_root = Path(src_dir)
    copied_pngs = set()
    mcmeta_count = 0
    for texture_file in sorted(src_root.rglob("*")):
        if not texture_file.is_file():
            continue
        rel = rename_folder(str(texture_file.relative_to(src_root)))
        target = Path(out_dir) / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(texture_file, target)
        if rel.endswith(".png"):
            copied_pngs.add(rel[:-4])
        elif rel.endswith(".mcmeta"):
            mcmeta_count += 1
    return {"pngs": len(copied_pngs), "mcmeta": mcmeta_count, "paths": copied_pngs}


def copy_sounds(src_assets_dir, out_assets_dir, ctx):
    src_root = Path(src_assets_dir)
    out_root = Path(out_assets_dir)
    ogg_paths = set()
    for sound_file in sorted((src_root / "sounds").rglob("*.ogg")):
        rel = sound_file.relative_to(src_root)
        target = out_root / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(sound_file, target)
        ogg_paths.add(str(rel.relative_to("sounds"))[:-4])
    definitions = json.loads((src_root / "sounds.json").read_text(encoding="utf-8"))
    for event_name, definition in definitions.items():
        for sound in definition.get("sounds", []):
            name = sound["name"] if isinstance(sound, dict) else sound
            namespace, path = split_ref(name)
            if namespace == "pyrotech" and path not in ogg_paths:
                ctx.warn("sounds.json", f"event {event_name} points at a missing file: {path}.ogg")
    (out_root / "sounds.json").write_text(
        json.dumps(definitions, indent=2) + "\n", encoding="utf-8"
    )
    return {"oggs": len(ogg_paths), "events": len(definitions)}
