package com.moostoet.pyrotech.datagen.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.TechBasicTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Tech/basic's two item tags: the campfire fuels, filled from the logs that burn (tech/basic
 * sign-off, item 5), and the campfire blacklist, which ships empty for datapacks.
 */
public final class TechBasicItemTagsProvider extends ItemTagsProvider {

    public TechBasicItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                     CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Tech Basic " + super.getName();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(TechBasicTags.Items.CAMPFIRE_FUELS).addTag(ItemTags.LOGS_THAT_BURN);
        this.tag(TechBasicTags.Items.CAMPFIRE_BLACKLIST);
    }
}
