package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.PyrotechTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class CoreBlockTagsProvider extends BlockTagsProvider {

    public CoreBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                 ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // The 1.12 harvest tools and levels.
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            CoreBlocks.CHARCOAL_BLOCK.get(),
            CoreBlocks.COAL_COKE_BLOCK.get(),
            CoreBlocks.CRAFTING_TABLE_TEMPLATE.get(),
            CoreBlocks.REFRACTORY_BRICK_BLOCK.get(),
            CoreBlocks.MASONRY_BRICK_BLOCK.get(),
            CoreBlocks.LIMESTONE.get(),
            CoreBlocks.FOSSIL_ORE.get(),
            CoreBlocks.DENSE_COAL_ORE.get(),
            CoreBlocks.DENSE_NETHER_COAL_ORE.get(),
            CoreBlocks.MASONRY_BRICK_SLAB.get(),
            CoreBlocks.REFRACTORY_BRICK_SLAB.get(),
            CoreBlocks.MASONRY_BRICK_STAIRS.get(),
            CoreBlocks.REFRACTORY_BRICK_STAIRS.get(),
            CoreBlocks.MASONRY_BRICK_WALL.get(),
            CoreBlocks.REFRACTORY_BRICK_WALL.get(),
            CoreBlocks.REFRACTORY_DOOR.get(),
            CoreBlocks.STONE_DOOR.get(),
            // 1.12's Material.GLASS needed no tool, so these two still break by hand and
            // drop; the tag only gives a pickaxe its speed bonus.
            CoreBlocks.REFRACTORY_GLASS.get(),
            CoreBlocks.SLAG_GLASS.get());
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            CoreBlocks.COBBLESTONE_ANDESITE.get(),
            CoreBlocks.COBBLESTONE_DIORITE.get(),
            CoreBlocks.COBBLESTONE_GRANITE.get(),
            CoreBlocks.COBBLESTONE_LIMESTONE.get(),
            CoreBlocks.COB_DRY.get(),
            CoreBlocks.LIVING_TAR.get(),
            CoreBlocks.DENSE_QUARTZ_ORE_LARGE.get(),
            CoreBlocks.DENSE_QUARTZ_ORE_SMALL.get(),
            CoreBlocks.DENSE_QUARTZ_ORE_ROCKS.get(),
            CoreBlocks.DENSE_REDSTONE_ORE_LARGE.get(),
            CoreBlocks.DENSE_REDSTONE_ORE_SMALL.get(),
            CoreBlocks.DENSE_REDSTONE_ORE_ROCKS.get());
        this.tag(BlockTags.MINEABLE_WITH_AXE).add(CoreBlocks.PLANKS_TARRED.get(), CoreBlocks.LOG_PILE.get());
        // The shovel blocks. The wood chip rock and pile, and the ash pile, gate their drops
        // on a shovel in code; the tag only gives the speed.
        this.tag(BlockTags.MINEABLE_WITH_SHOVEL).add(
            CoreBlocks.WOOD_TAR_BLOCK.get(),
            CoreBlocks.MUD.get(),
            CoreBlocks.MUD_LAYER.get(),
            CoreBlocks.COB_WET.get(),
            CoreBlocks.PILE_ASH.get(),
            CoreBlocks.PILE_WOOD_CHIPS.get(),
            CoreBlocks.ROCK_WOOD_CHIPS.get(),
            CoreBlocks.FARMLAND_MULCHED.get());
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(CoreBlocks.LIMESTONE.get(), CoreBlocks.COB_DRY.get());
        this.tag(BlockTags.NEEDS_IRON_TOOL).add(
            CoreBlocks.DENSE_COAL_ORE.get(),
            CoreBlocks.DENSE_QUARTZ_ORE_LARGE.get(),
            CoreBlocks.DENSE_QUARTZ_ORE_SMALL.get(),
            CoreBlocks.DENSE_QUARTZ_ORE_ROCKS.get(),
            CoreBlocks.DENSE_REDSTONE_ORE_LARGE.get(),
            CoreBlocks.DENSE_REDSTONE_ORE_SMALL.get(),
            CoreBlocks.DENSE_REDSTONE_ORE_ROCKS.get());

        // A crop to vanilla: bees visit it and farmland under it stays farmland, as the
        // 1.12 Crop plant type did.
        this.tag(BlockTags.CROPS).add(CoreBlocks.FRECKLEBERRY_PLANT.get());
        this.tag(BlockTags.MAINTAINS_FARMLAND).add(CoreBlocks.FRECKLEBERRY_PLANT.get());

        // The 1.12 netherrack gib spread to Material.ROCK, GROUND, and GRASS full blocks.
        this.tag(PyrotechTags.Blocks.NETHERRACK_SPREADS_TO).addTags(
            BlockTags.DIRT,
            BlockTags.BASE_STONE_OVERWORLD,
            BlockTags.STONE_BRICKS,
            BlockTags.TERRACOTTA,
            Tags.Blocks.COBBLESTONES,
            Tags.Blocks.ORES);
        this.tag(BlockTags.NEEDS_DIAMOND_TOOL).add(CoreBlocks.DENSE_NETHER_COAL_ORE.get());

        // The shape tags. Walls only connect to each other through #minecraft:walls, and
        // slabs, stairs, and doors read theirs in vanilla recipes and behaviour. The doors
        // stay out of #minecraft:wooden_doors, which is where the furnace fuel entry sits.
        this.tag(BlockTags.SLABS).add(CoreBlocks.MASONRY_BRICK_SLAB.get(), CoreBlocks.REFRACTORY_BRICK_SLAB.get());
        this.tag(BlockTags.STAIRS).add(CoreBlocks.MASONRY_BRICK_STAIRS.get(), CoreBlocks.REFRACTORY_BRICK_STAIRS.get());
        this.tag(BlockTags.WALLS).add(CoreBlocks.MASONRY_BRICK_WALL.get(), CoreBlocks.REFRACTORY_BRICK_WALL.get());
        this.tag(BlockTags.DOORS).add(CoreBlocks.REFRACTORY_DOOR.get(), CoreBlocks.STONE_DOOR.get());

        // 1.12 listed the refractory brick, the refractory glass, and the double refractory
        // slab (RegistryInitializer#initializeRefractoryBlocks). The slab is refractory only
        // at type=double, which RefractoryBlocks handles, so it stays out of the tag.
        this.tag(PyrotechTags.Blocks.REFRACTORY).add(
            CoreBlocks.REFRACTORY_BRICK_BLOCK.get(),
            CoreBlocks.REFRACTORY_GLASS.get());
    }
}
