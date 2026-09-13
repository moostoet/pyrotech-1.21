package com.moostoet.pyrotech.storage;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Storage's config: the four bag auto-pickup toggles, shared by both bags, in their own
 * file (storage sign-off, item 3). Every other 1.12 number bakes into the blocks. Keys keep
 * their 1.12 names.
 */
public final class StorageConfig {

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
    }

    private StorageConfig() {
    }

    public static final class Common {

        public final ModConfigSpec.BooleanValue autoPickupMainHand;
        public final ModConfigSpec.BooleanValue autoPickupOffHand;
        public final ModConfigSpec.BooleanValue autoPickupHotbar;
        public final ModConfigSpec.BooleanValue autoPickupInventory;

        Common(ModConfigSpec.Builder builder) {
            builder.push("storage");
            this.autoPickupMainHand = builder
                .comment("If true, an open rock bag in the player's main hand collects the items it can carry as they are picked up.")
                .define("ALLOW_AUTO_PICKUP_MAINHAND", true);
            this.autoPickupOffHand = builder
                .comment("If true, an open rock bag in the player's off hand collects the items it can carry as they are picked up.")
                .define("ALLOW_AUTO_PICKUP_OFFHAND", true);
            this.autoPickupHotbar = builder
                .comment("If true, an open rock bag in the player's hotbar collects the items it can carry as they are picked up.")
                .define("ALLOW_AUTO_PICKUP_HOTBAR", true);
            this.autoPickupInventory = builder
                .comment("If true, an open rock bag in the player's main inventory collects the items it can carry as they are picked up.")
                .define("ALLOW_AUTO_PICKUP_INVENTORY", false);
            builder.pop();
        }
    }
}
