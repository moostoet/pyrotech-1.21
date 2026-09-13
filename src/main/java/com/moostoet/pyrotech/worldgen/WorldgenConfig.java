package com.moostoet.pyrotech.worldgen;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;

/**
 * Worldgen's config: the eleven {@code ENABLED} toggles, one per generator, in their own
 * file as 1.12 had them. Every other 1.12 worldgen value lives in the generated JSON
 * (worldgen sign-off, item 2). Keys keep their 1.12 names.
 */
public final class WorldgenConfig {

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
    }

    private WorldgenConfig() {
    }

    public static final class Common {

        private final Map<Generator, ModConfigSpec.BooleanValue> enabled = new EnumMap<>(Generator.class);

        Common(ModConfigSpec.Builder builder) {
            for (Generator generator : Generator.values()) {
                builder.push(generator.id());
                this.enabled.put(generator, builder
                    .comment("Set to false to disable this worldgen.")
                    .define("ENABLED", true));
                builder.pop();
            }
        }

        public boolean isEnabled(Generator generator) {
            return this.enabled.get(generator).get();
        }
    }
}
