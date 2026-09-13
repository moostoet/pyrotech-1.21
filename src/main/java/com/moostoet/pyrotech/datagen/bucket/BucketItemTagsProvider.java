package com.moostoet.pyrotech.datagen.bucket;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.bucket.BucketItems;
import com.moostoet.pyrotech.bucket.item.PyrotechBucketItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.concurrent.CompletableFuture;

/** The {@code c:buckets} tag, which the four Pyrotech buckets join beside core's fluid buckets. */
public final class BucketItemTagsProvider extends ItemTagsProvider {

    public BucketItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                  CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Bucket " + super.getName();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (DeferredItem<PyrotechBucketItem> bucket : BucketItems.BUCKETS) {
            this.tag(Tags.Items.BUCKETS).add(bucket.get());
        }
    }
}
