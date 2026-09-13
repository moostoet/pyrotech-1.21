package com.moostoet.pyrotech.bucket;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bucket's config: the three behaviour toggles per tier, in their own file as 1.12 had
 * them. The other six 1.12 options per tier are baked into {@link BucketTier} (bucket
 * sign-off, item 4). Keys keep their 1.12 names.
 */
public final class BucketConfig {

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
    }

    private BucketConfig() {
    }

    public static final class Common {

        private final Map<BucketTier, Toggles> toggles = new EnumMap<>(BucketTier.class);

        Common(ModConfigSpec.Builder builder) {
            for (BucketTier tier : BucketTier.values()) {
                builder.push("bucket_" + tier.id());
                this.toggles.put(tier, new Toggles(builder));
                builder.pop();
            }
        }

        public boolean showAllBuckets(BucketTier tier) {
            return this.toggles.get(tier).showAllBuckets.get();
        }

        public boolean enableCowMilk(BucketTier tier) {
            return this.toggles.get(tier).enableCowMilk.get();
        }

        public boolean dropSourceOnBreak(BucketTier tier) {
            return this.toggles.get(tier).dropSourceOnBreak.get();
        }
    }

    private static final class Toggles {

        private final ModConfigSpec.BooleanValue showAllBuckets;
        private final ModConfigSpec.BooleanValue enableCowMilk;
        private final ModConfigSpec.BooleanValue dropSourceOnBreak;

        Toggles(ModConfigSpec.Builder builder) {
            this.showAllBuckets = builder
                .comment("Set to true to show all bucket / fluid combinations.")
                .define("SHOW_ALL_BUCKETS", false);
            this.enableCowMilk = builder
                .comment("Set to false to disable milking a cow with the bucket.")
                .define("ENABLE_COW_MILK", true);
            this.dropSourceOnBreak = builder
                .comment("When a filled bucket breaks in a player's inventory, its fluid spills at the player's feet.",
                    "Set to true to leave a flowing puddle that drains away.",
                    "Set to false to leave a permanent source block.")
                .define("DROP_SOURCE_ON_BREAK", true);
        }
    }
}
