import itertools
import json
from pathlib import Path

from refs import fix_model_ref, fix_texture_ref, split_ref

ENTRY_KEYS = {"model", "textures", "submodel", "transform", "custom", "x", "y", "uvlock", "weight"}


class Flagged(Exception):
    def __init__(self, reason):
        self.reason = reason


def is_prop_map(value):
    return (
        isinstance(value, dict)
        and value
        and all(isinstance(child, (dict, list)) for child in value.values())
        and not (ENTRY_KEYS & set(value.keys()))
    )


def merge_entry(base, extra):
    merged = dict(base)
    for key, value in extra.items():
        if key == "textures":
            textures = dict(merged.get("textures", {}))
            textures.update(value)
            merged["textures"] = textures
        elif key == "submodel":
            if not isinstance(value, dict) or ENTRY_KEYS & set(value.keys()):
                raise Flagged("submodel is not a named map; convert by hand")
            submodels = dict(merged.get("submodel", {}))
            submodels.update(value)
            merged["submodel"] = submodels
        else:
            merged[key] = value
    return merged


class WrapperAllocator:
    def __init__(self, out_models_dir, blockstate_name):
        self.dir = Path(out_models_dir) / "block" / "gen" / blockstate_name
        self.prefix = f"pyrotech:block/gen/{blockstate_name}"
        self.by_content = {}
        self.used_names = set()
        self.written = 0

    def wrapper_for(self, model_ref, textures, ctx, source):
        parent = fix_model_ref(model_ref, ctx, source, add_block_prefix=True)
        fixed = {key: fix_texture_ref(value, ctx, source) for key, value in textures.items()}
        content_key = (parent, tuple(sorted(fixed.items())))
        if content_key in self.by_content:
            return self.by_content[content_key]
        base_name = split_ref(model_ref)[1].rsplit("/", 1)[-1]
        name, counter = base_name, 1
        while name in self.used_names:
            counter += 1
            name = f"{base_name}_{counter}"
        self.used_names.add(name)
        self.dir.mkdir(parents=True, exist_ok=True)
        body = {"parent": parent, "textures": fixed}
        (self.dir / f"{name}.json").write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8")
        self.written += 1
        ref = f"{self.prefix}/{name}"
        self.by_content[content_key] = ref
        return ref


def resolve_model(merged, alloc, ctx, source):
    model = merged.get("model")
    if model is None:
        raise Flagged("a state has no model; convert by hand")
    textures = merged.get("textures") or {}
    if textures:
        return alloc.wrapper_for(model, textures, ctx, source)
    return fix_model_ref(model, ctx, source, add_block_prefix=True)


def emit_variant_entry(merged, alloc, ctx, source):
    entry = {"model": resolve_model(merged, alloc, ctx, source)}
    for key in ("x", "y", "uvlock", "weight"):
        if key in merged:
            entry[key] = merged[key]
    return entry


def classify_variants(variants):
    plain, prop_maps, explicit = {}, {}, {}
    for key, value in variants.items():
        if key.startswith("__"):
            continue
        if "=" in key:
            props = dict(pair.split("=", 1) for pair in key.split(","))
            explicit[key] = (props, value)
        elif is_prop_map(value):
            prop_maps[key] = value
        else:
            plain[key] = value
    return plain, prop_maps, explicit


def build_states(plain, prop_maps, explicit):
    if explicit:
        prop_sets = {frozenset(props) for props, _ in explicit.values()}
        if prop_maps or len(prop_sets) != 1:
            raise Flagged("mixes explicit state keys with other variant forms; convert by hand")
        return [(props, [value]) for props, value in explicit.values()]
    if prop_maps:
        names = sorted(prop_maps)
        states = []
        for combo in itertools.product(*(sorted(prop_maps[name]) for name in names)):
            props = dict(zip(names, combo))
            contributions = [prop_maps[name][value] for name, value in props.items()]
            states.append((props, contributions))
        return states
    return [({}, [plain["normal"]])] if "normal" in plain else []


def merge_state(defaults, contributions, source, ctx):
    alternatives = [dict(defaults)]
    rotation_setters = 0
    for contribution in contributions:
        if isinstance(contribution, list):
            if len(alternatives) > 1:
                raise Flagged("two variant lists combine in one state; convert by hand")
            alternatives = [merge_entry(alternatives[0], alt) for alt in contribution]
            if any({"x", "y"} & set(alt) for alt in contribution):
                rotation_setters += 1
        else:
            if {"x", "y"} & set(contribution):
                rotation_setters += 1
            alternatives = [merge_entry(alt, contribution) for alt in alternatives]
    if rotation_setters > 1:
        ctx.warn(source, "two properties set the same rotation; check the merged result")
    return alternatives


def variant_key(props):
    return ",".join(f"{name}={value}" for name, value in sorted(props.items()))


def emit_variants(states, defaults, alloc, ctx, source):
    variants = {}
    for props, contributions in states:
        alternatives = merge_state(defaults, contributions, source, ctx)
        entries = [emit_variant_entry(alt, alloc, ctx, source) for alt in alternatives]
        variants[variant_key(props)] = entries[0] if len(entries) == 1 else entries
    return {"variants": variants}


def is_empty_model(model_ref):
    return model_ref is not None and split_ref(model_ref)[1] in ("_empty", "block/_empty")


def submodel_part(sub_entry, state, alloc, ctx, source):
    if not isinstance(sub_entry, dict):
        raise Flagged("a submodel entry is not a plain object; convert by hand")
    part = dict(sub_entry)
    part["textures"] = {**state.get("textures", {}), **part.get("textures", {})}
    state_rotation = {key: state[key] for key in ("x", "y") if key in state}
    own_rotation = {key: part[key] for key in ("x", "y") if key in part}
    if state_rotation and own_rotation:
        ctx.warn(source, "a submodel has its own rotation on a rotated state; check the result")
    if state_rotation and not own_rotation:
        part.update(state_rotation)
    if "uvlock" in state and "uvlock" not in part:
        part["uvlock"] = state["uvlock"]
    return emit_variant_entry(part, alloc, ctx, source)


def emit_multipart(states, defaults, alloc, ctx, source):
    parts = []
    for props, contributions in states:
        alternatives = merge_state(defaults, contributions, source, ctx)
        submodels = alternatives[0].get("submodel", {})
        if any(alt.get("submodel", {}) != submodels for alt in alternatives):
            raise Flagged("random variants disagree on submodels; convert by hand")
        when = {name: value for name, value in sorted(props.items())}
        base_entries = [
            emit_variant_entry({k: v for k, v in alt.items() if k != "submodel"}, alloc, ctx, source)
            for alt in alternatives
            if not is_empty_model(alt.get("model"))
        ]
        applies = []
        if base_entries:
            applies.append(base_entries[0] if len(base_entries) == 1 else base_entries)
        for sub_name in sorted(submodels):
            applies.append(submodel_part(submodels[sub_name], alternatives[0], alloc, ctx, source))
        for apply in applies:
            part = {"when": when, "apply": apply} if when else {"apply": apply}
            parts.append(part)
    return {"multipart": parts}


def find_item_model_refs(value):
    if isinstance(value, dict):
        for key, child in value.items():
            if key == "model" and isinstance(child, str) and split_ref(child)[1].startswith("item/"):
                yield child
            else:
                yield from find_item_model_refs(child)
    elif isinstance(value, list):
        for child in value:
            yield from find_item_model_refs(child)


def convert_forge_file(name, data, out_models_dir, ctx, result):
    source = f"blockstates/{name}.json"
    text = json.dumps(data)
    if '"custom"' in text or '"transform"' in text:
        raise Flagged("uses a custom model loader from Forge 1.12; needs new item or fluid rendering")
    defaults = {key: value for key, value in data.get("defaults", {}).items() if not key.startswith("__")}
    plain, prop_maps, explicit = classify_variants(data.get("variants", {}))
    if "inventory" in plain:
        result["inventory_variants"][name] = plain.pop("inventory")
    leftover = [key for key in plain if key != "normal"]
    if leftover:
        raise Flagged(f"has non-state variants {leftover}; convert by hand")
    state_data = [defaults, plain, prop_maps, [value for _, value in explicit.values()]]
    if any(find_item_model_refs(state_data)):
        raise Flagged("an item-variants blockstate from 1.12; each variant becomes its own item model during hoisting")
    states = build_states(plain, prop_maps, explicit)
    if not states:
        raise Flagged("defines no block states; convert by hand")
    alloc = WrapperAllocator(out_models_dir, name)
    has_submodels = json.dumps(state_data).find('"submodel"') >= 0
    if has_submodels:
        output = emit_multipart(states, defaults, alloc, ctx, source)
        result["multipart"] += 1
    else:
        output = emit_variants(states, defaults, alloc, ctx, source)
    result["wrappers"] += alloc.written
    return output


def convert_vanilla_file(name, data, ctx, result):
    source = f"blockstates/{name}.json"
    if "multipart" in data:
        raise Flagged("wall properties changed in 1.16 (true/false became none/low/tall); rebuild from the 1.21.1 wall template")
    keys = list(data.get("variants", {}).keys())
    joined = " ".join(keys)
    if "hinge=" in joined:
        raise Flagged("door models were redesigned after 1.12; rebuild from the 1.21.1 door template")
    if "variant=" in joined:
        raise Flagged("slabs changed in 1.13 (half=top/bottom became type=top/bottom/double); rebuild from the 1.21.1 slab template")
    if "shape=" not in joined:
        raise Flagged("unrecognized vanilla-format blockstate; convert by hand")
    variants = {}
    for key, value in data["variants"].items():
        entries = value if isinstance(value, list) else [value]
        fixed = []
        for entry in entries:
            entry = dict(entry)
            entry["model"] = fix_model_ref(entry["model"], ctx, source, add_block_prefix=True)
            fixed.append(entry)
        variants[key] = fixed[0] if len(fixed) == 1 else fixed
    result["notes"][source] = "stairs: state properties are unchanged since 1.12; verify in game"
    return {"variants": variants}


def convert_blockstates(src_dir, out_dir, out_models_dir, ctx):
    result = {
        "converted": 0,
        "multipart": 0,
        "wrappers": 0,
        "flagged": {},
        "notes": {},
        "inventory_variants": {},
    }
    for state_file in sorted(Path(src_dir).glob("*.json")):
        name = state_file.stem
        data = json.loads(state_file.read_text(encoding="utf-8"))
        try:
            if data.get("forge_marker") == 1:
                output = convert_forge_file(name, data, out_models_dir, ctx, result)
            else:
                output = convert_vanilla_file(name, data, ctx, result)
        except Flagged as flag:
            result["flagged"][f"blockstates/{name}.json"] = flag.reason
            continue
        out_path = Path(out_dir) / f"{name}.json"
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")
        result["converted"] += 1
    return result
