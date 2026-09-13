package com.moostoet.pyrotech.bucket;

/**
 * The four buckets' baked numbers: the 1.12 config defaults for uses, the empty stack
 * size, and the three damage rates per second (bucket sign-off, item 4). What makes a
 * fluid hot is one number for every tier, the shared hot-fluid threshold.
 */
public enum BucketTier {
    WOOD("wood", 8, 1, 8, 2, 1),
    CLAY("clay", 12, 4, 4, 2, 0),
    STONE("stone", 16, 4, 4, 2, 0),
    REFRACTORY("refractory", 24, 4, 0, 1, 0);

    public static final int HOT_TEMPERATURE = 450;

    private final String id;
    private final int uses;
    private final int emptyStackSize;
    private final int hotContainerDamagePerSecond;
    private final int hotPlayerDamagePerSecond;
    private final int fullContainerDamagePerSecond;

    BucketTier(String id, int uses, int emptyStackSize, int hotContainerDamagePerSecond,
               int hotPlayerDamagePerSecond, int fullContainerDamagePerSecond) {
        this.id = id;
        this.uses = uses;
        this.emptyStackSize = emptyStackSize;
        this.hotContainerDamagePerSecond = hotContainerDamagePerSecond;
        this.hotPlayerDamagePerSecond = hotPlayerDamagePerSecond;
        this.fullContainerDamagePerSecond = fullContainerDamagePerSecond;
    }

    /** The 1.12 name, which the item id and the config section carry. */
    public String id() {
        return this.id;
    }

    public int uses() {
        return this.uses;
    }

    public int emptyStackSize() {
        return this.emptyStackSize;
    }

    public int hotContainerDamagePerSecond() {
        return this.hotContainerDamagePerSecond;
    }

    public int hotPlayerDamagePerSecond() {
        return this.hotPlayerDamagePerSecond;
    }

    public int fullContainerDamagePerSecond() {
        return this.fullContainerDamagePerSecond;
    }
}
