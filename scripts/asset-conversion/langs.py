import json
import re
from pathlib import Path

PYROTECH_BLOCK = re.compile(r"^tile\.pyrotech\.(?P<name>.+)\.name$")
PYROTECH_ITEM = re.compile(r"^item\.pyrotech\.(?P<name>.+)\.name$")
VANILLA_BLOCK = re.compile(r"^tile\.(?P<name>[^.]+)\.name$")
VANILLA_ITEM = re.compile(r"^item\.(?P<name>[^.]+)\.name$")


def convert_key(key):
    match = PYROTECH_BLOCK.match(key)
    if match:
        return "block.pyrotech." + match.group("name").replace(".", "_"), "block"
    match = PYROTECH_ITEM.match(key)
    if match:
        return "item.pyrotech." + match.group("name").replace(".", "_"), "item"
    match = VANILLA_BLOCK.match(key)
    if match:
        return "block.minecraft." + match.group("name"), "vanilla_override"
    match = VANILLA_ITEM.match(key)
    if match:
        return "item.minecraft." + match.group("name"), "vanilla_override"
    return key, "other"


def parse_lang(text):
    entries = []
    for line in text.splitlines():
        line = line.strip("﻿\r")
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if "=" not in line:
            entries.append((None, line))
            continue
        key, value = line.split("=", 1)
        entries.append((key.strip(), value))
    return entries


def convert_langs(src_dir, out_dir, block_names, item_names):
    result = {
        "files": [],
        "bad_lines": [],
        "duplicates": [],
        "vanilla_overrides": [],
        "unmatched_blocks": [],
        "unmatched_items": [],
        "counts": {"block": 0, "item": 0, "vanilla_override": 0, "other": 0},
    }
    for lang_file in sorted(Path(src_dir).glob("*.lang")):
        entries = parse_lang(lang_file.read_text(encoding="utf-8"))
        converted = {}
        for key, value in entries:
            if key is None:
                result["bad_lines"].append((lang_file.name, value))
                continue
            new_key, category = convert_key(key)
            if new_key in converted:
                result["duplicates"].append((lang_file.name, new_key))
            converted[new_key] = value
            if lang_file.name != "en_us.lang":
                continue
            result["counts"][category] += 1
            if category == "vanilla_override":
                result["vanilla_overrides"].append(key)
            elif category == "block" and new_key.rsplit(".", 1)[1] not in block_names:
                result["unmatched_blocks"].append(new_key)
            elif category == "item" and new_key.rsplit(".", 1)[1] not in item_names:
                result["unmatched_items"].append(new_key)
        out_path = Path(out_dir) / (lang_file.stem + ".json")
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(
            json.dumps(converted, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
        )
        result["files"].append((lang_file.name, len(converted)))
    return result
