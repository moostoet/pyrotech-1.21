package com.moostoet.pyrotech.bucket;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.bucket.item.PyrotechBucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** Bucket's six items, in the 1.12 registration order. Ids are the 1.12 registry names. */
public final class BucketItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pyrotech.MOD_ID);

    public static final DeferredItem<PyrotechBucketItem> BUCKET_WOOD = bucket(BucketTier.WOOD);
    public static final DeferredItem<PyrotechBucketItem> BUCKET_CLAY = bucket(BucketTier.CLAY);
    public static final DeferredItem<Item> BUCKET_CLAY_UNFIRED = ITEMS.registerSimpleItem("bucket_clay_unfired",
        new Item.Properties().stacksTo(1));
    public static final DeferredItem<PyrotechBucketItem> BUCKET_STONE = bucket(BucketTier.STONE);
    public static final DeferredItem<Item> BUCKET_REFRACTORY_UNFIRED = ITEMS.registerSimpleItem("bucket_refractory_unfired",
        new Item.Properties().stacksTo(1));
    public static final DeferredItem<PyrotechBucketItem> BUCKET_REFRACTORY = bucket(BucketTier.REFRACTORY);

    public static final List<DeferredItem<PyrotechBucketItem>> BUCKETS =
        List.of(BUCKET_WOOD, BUCKET_CLAY, BUCKET_STONE, BUCKET_REFRACTORY);

    private static final List<DeferredItem<? extends Item>> TAB_ORDER = List.of(
        BUCKET_WOOD, BUCKET_CLAY, BUCKET_CLAY_UNFIRED, BUCKET_STONE, BUCKET_REFRACTORY_UNFIRED, BUCKET_REFRACTORY);

    private BucketItems() {
    }

    private static DeferredItem<PyrotechBucketItem> bucket(BucketTier tier) {
        return ITEMS.registerItem("bucket_" + tier.id(), properties -> new PyrotechBucketItem(tier, properties),
            new Item.Properties().stacksTo(tier.emptyStackSize()));
    }

    static void addToTab(CreativeModeTab.Output output) {
        for (DeferredItem<? extends Item> holder : TAB_ORDER) {
            Item item = holder.get();
            output.accept(item);
            if (item instanceof PyrotechBucketItem bucket && BucketConfig.COMMON.showAllBuckets(bucket.tier())) {
                bucket.addFilledVariants(output);
            }
        }
    }
}
