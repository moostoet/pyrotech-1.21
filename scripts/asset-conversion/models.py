import json
from pathlib import Path

from refs import fix_model_ref, fix_texture_ref


def convert_model(data, ctx, source, flags):
    parent = data.get("parent")
    if parent:
        if parent.startswith("builtin/"):
            if parent != "builtin/generated":
                flags.append(f"parent {parent} needs a special renderer in 1.21.1")
        else:
            data["parent"] = fix_model_ref(parent, ctx, source)
    if "textures" in data:
        data["textures"] = {
            key: fix_texture_ref(value, ctx, source)
            for key, value in data["textures"].items()
        }
    if "overrides" in data:
        flags.append("has item overrides; verify the predicates still exist in 1.21.1")
        for override in data["overrides"]:
            if "model" in override:
                override["model"] = fix_model_ref(override["model"], ctx, source)
    return data


def convert_models(src_dir, out_dir, ctx):
    result = {"converted": 0, "flagged": {}}
    src_root = Path(src_dir)
    for model_file in sorted(src_root.rglob("*.json")):
        rel = model_file.relative_to(src_root)
        source = f"models/{rel}"
        data = json.loads(model_file.read_text(encoding="utf-8"))
        flags = []
        data = convert_model(data, ctx, source, flags)
        out_path = Path(out_dir) / rel
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
        result["converted"] += 1
        if flags:
            result["flagged"][source] = flags
    return result
