package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.core.item.HammerItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.concurrent.CompletableFuture;

public final class CoreItemTagsProvider extends ItemTagsProvider {

    public CoreItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (DeferredItem<HammerItem> hammer : CoreItems.HAMMERS) {
            this.tag(PyrotechTags.Items.HAMMERS).add(hammer.get());
        }
        this.tag(PyrotechTags.Items.STONE_STICKS).add(CoreItems.material(Material.STICK_STONE).get());
        // Ignition fills the igniters and hunting the knives; the files exist from the start so
        // that the tags they feed resolve.
        this.tag(PyrotechTags.Items.IGNITERS);
        this.tag(PyrotechTags.Items.KNIVES);
        this.tag(PyrotechTags.Items.SHARP_TOOLS).addTags(ItemTags.AXES, ItemTags.SWORDS, PyrotechTags.Items.KNIVES);
    }
}
