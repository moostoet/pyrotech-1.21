import json
import zipfile

FOLDER_RENAMES = {"blocks": "block", "items": "item"}


class RefContext:
    def __init__(self, vanilla_jar_path, renames_path):
        self.renames = {"textures": {}, "models": {}}
        if renames_path:
            with open(renames_path) as handle:
                self.renames = json.load(handle)
        self.vanilla_models = set()
        self.vanilla_textures = set()
        if vanilla_jar_path:
            self._load_vanilla_lists(vanilla_jar_path)
        self.warnings = []
        self.mod_texture_refs = set()
        self.mod_model_refs = set()

    def _load_vanilla_lists(self, jar_path):
        with zipfile.ZipFile(jar_path) as jar:
            for name in jar.namelist():
                if name.startswith("assets/minecraft/models/") and name.endswith(".json"):
                    self.vanilla_models.add(name[len("assets/minecraft/models/"):-5])
                elif name.startswith("assets/minecraft/textures/") and name.endswith(".png"):
                    self.vanilla_textures.add(name[len("assets/minecraft/textures/"):-4])

    def warn(self, source, message):
        self.warnings.append((source, message))


def split_ref(ref):
    if ":" in ref:
        namespace, path = ref.split(":", 1)
    else:
        namespace, path = "minecraft", ref
    return namespace, path


def rename_folder(path):
    head, sep, tail = path.partition("/")
    if head in FOLDER_RENAMES:
        return FOLDER_RENAMES[head] + sep + tail
    return path


def fix_texture_ref(ref, ctx, source):
    if ref.startswith("#"):
        return ref
    namespace, path = split_ref(ref.lower())
    path = rename_folder(path)
    if namespace == "minecraft":
        path = ctx.renames["textures"].get(path, path)
        if ctx.vanilla_textures and path not in ctx.vanilla_textures:
            ctx.warn(source, f"vanilla texture missing in 1.21.1: {path}")
    elif namespace == "pyrotech":
        ctx.mod_texture_refs.add((source, path))
    else:
        ctx.warn(source, f"unconvertible {namespace}: texture reference: {ref}")
    return f"{namespace}:{path}"


def fix_model_ref(ref, ctx, source, add_block_prefix=False):
    namespace, path = split_ref(ref.lower())
    if add_block_prefix and not path.startswith(("block/", "item/")):
        path = "block/" + path
    if namespace == "minecraft":
        path = ctx.renames["models"].get(path, path)
        if ctx.vanilla_models and path not in ctx.vanilla_models:
            ctx.warn(source, f"vanilla model missing in 1.21.1: {path}")
    elif namespace == "pyrotech":
        ctx.mod_model_refs.add((source, path))
    else:
        ctx.warn(source, f"unconvertible {namespace}: model reference: {ref}")
    return f"{namespace}:{path}"
