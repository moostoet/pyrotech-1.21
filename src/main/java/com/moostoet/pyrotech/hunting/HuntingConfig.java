package com.moostoet.pyrotech.hunting;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Hunting's config: the three behaviour toggles, in their own file. Every other 1.12
 * number bakes into the items, the blocks, the recipes, or the data maps (hunting sign-off,
 * items 2, 3, and 9). Keys keep their 1.12 names.
 */
public final class HuntingConfig {

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
    }

    private HuntingConfig() {
    }

    public static final class Common {

        public final ModConfigSpec.BooleanValue removeLeatherDrops;
        public final ModConfigSpec.BooleanValue allowHuntersKnifeRepair;
        public final ModConfigSpec.BooleanValue allowButchersKnifeRepair;

        Common(ModConfigSpec.Builder builder) {
            builder.push("hunting");
            this.removeLeatherDrops = builder
                .comment("If true, leather drops will be removed from entity drops.")
                .define("REMOVE_LEATHER_DROPS", true);
            this.allowHuntersKnifeRepair = builder
                .comment("Set to false to disable anvil repair of the hunter's knives.")
                .define("ALLOW_HUNTERS_KNIFE_REPAIR", true);
            this.allowButchersKnifeRepair = builder
                .comment("Set to false to disable anvil repair of the butcher's knives.")
                .define("ALLOW_BUTCHERS_KNIFE_REPAIR", true);
            builder.pop();
        }
    }
}
