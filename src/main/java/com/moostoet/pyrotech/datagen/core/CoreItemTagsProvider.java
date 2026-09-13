package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.bucket.BucketItems;
import com.moostoet.pyrotech.bucket.item.PyrotechBucketItem;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreFluids;
import com.moostoet.pyrotech.library.fluid.PyrotechFluids;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.core.item.HammerItem;
import com.moostoet.pyrotech.hunting.HuntingFluids;
import com.moostoet.pyrotech.hunting.HuntingTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
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
        // The stone rocks a stone tool recipe accepts (tool sign-off, item 6).
        this.tag(PyrotechTags.Items.ROCKS).add(
            CoreBlocks.ROCK_STONE.get().asItem(),
            CoreBlocks.ROCK_DIORITE.get().asItem(),
            CoreBlocks.ROCK_GRANITE.get().asItem(),
            CoreBlocks.ROCK_ANDESITE.get().asItem(),
            CoreBlocks.ROCK_SANDSTONE.get().asItem(),
            CoreBlocks.ROCK_LIMESTONE.get().asItem(),
            CoreBlocks.ROCK_SANDSTONE_RED.get().asItem());
        // Ignition fills the igniters; the file exists from the start so the tags it feeds resolve.
        this.tag(PyrotechTags.Items.IGNITERS);
        this.tag(PyrotechTags.Items.KNIVES).addTags(HuntingTags.Items.HUNTERS_KNIVES, HuntingTags.Items.BUTCHERS_KNIVES);
        this.tag(PyrotechTags.Items.SHARP_TOOLS).addTags(ItemTags.AXES, ItemTags.SWORDS, PyrotechTags.Items.KNIVES);
        this.tag(PyrotechTags.Items.TWINE).add(
            Items.STRING,
            CoreItems.material(Material.TWINE).get(),
            CoreItems.material(Material.TWINE_DURABLE).get());
        // A tag file has one writer, so every Pyrotech bucket joins c:buckets here: core's four
        // fluid buckets, hunting's tannin bucket, and bucket's four tiers.
        IntrinsicTagAppender<Item> buckets = this.tag(Tags.Items.BUCKETS);
        for (PyrotechFluids.Entry fluid : CoreFluids.ALL) {
            buckets.add(fluid.bucket().get());
        }
        buckets.add(HuntingFluids.TANNIN.bucket().get());
        for (DeferredItem<PyrotechBucketItem> bucket : BucketItems.BUCKETS) {
            buckets.add(bucket.get());
        }

        // The item halves of the shape tags. #minecraft:wooden_doors stays untouched: that
        // is the tag NeoForge's furnace_fuels data map burns.
        this.copy(BlockTags.SLABS, ItemTags.SLABS);
        this.copy(BlockTags.STAIRS, ItemTags.STAIRS);
        this.copy(BlockTags.WALLS, ItemTags.WALLS);
        this.copy(BlockTags.DOORS, ItemTags.DOORS);
        this.copy(Tags.Blocks.GLASS_BLOCKS, Tags.Items.GLASS_BLOCKS);
        this.copy(Tags.Blocks.COBBLESTONES, Tags.Items.COBBLESTONES);
        this.copy(Tags.Blocks.COBBLESTONES_NORMAL, Tags.Items.COBBLESTONES_NORMAL);
    }
}
