package com.moostoet.pyrotech.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.block.AnvilBlock;
import com.moostoet.pyrotech.tech.basic.block.BarrelBlock;
import com.moostoet.pyrotech.tech.basic.block.CampfireBlock;
import com.moostoet.pyrotech.tech.basic.block.ChoppingBlockBlock;
import com.moostoet.pyrotech.tech.basic.block.CompactingBinBlock;
import com.moostoet.pyrotech.tech.basic.block.CompostBinBlock;
import com.moostoet.pyrotech.tech.basic.block.DryingRackBlock;
import com.moostoet.pyrotech.tech.basic.block.PitKilnBlock;
import com.moostoet.pyrotech.tech.basic.block.SoakingPotBlock;
import com.moostoet.pyrotech.tech.basic.block.TanningRackBlock;
import com.moostoet.pyrotech.tech.basic.block.WorktableBlock;
import com.moostoet.pyrotech.tech.basic.item.BarrelItem;
import com.moostoet.pyrotech.tech.basic.recipe.AnvilTier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Tech/basic's fifteen blocks with their 1.12 hardness and resistance (resistance at
 * three fifths, as vanilla's own values map). Every one is a partial block, so none
 * occludes. The block items register in {@link TechBasicItems}' register, in the 1.12
 * tab order.
 */
public final class TechBasicBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Pyrotech.MOD_ID);
    static final List<DeferredItem<? extends BlockItem>> BLOCK_ITEMS = new ArrayList<>();

    public static final DeferredBlock<ChoppingBlockBlock> CHOPPING_BLOCK = BLOCKS.registerBlock("chopping_block", ChoppingBlockBlock::new,
        wood(0.75f, 0));
    public static final DeferredItem<BlockItem> CHOPPING_BLOCK_ITEM = blockItem(CHOPPING_BLOCK);

    public static final DeferredBlock<AnvilBlock> ANVIL_GRANITE = BLOCKS.registerBlock("anvil_granite",
        properties -> new AnvilBlock(AnvilTier.GRANITE, properties),
        stone(3, 3).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> ANVIL_GRANITE_ITEM = blockItem(ANVIL_GRANITE);
    public static final DeferredBlock<AnvilBlock> ANVIL_IRON_PLATED = BLOCKS.registerBlock("anvil_iron_plated",
        properties -> new AnvilBlock(AnvilTier.IRONCLAD, properties),
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(5, 6).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> ANVIL_IRON_PLATED_ITEM = blockItem(ANVIL_IRON_PLATED);
    public static final DeferredBlock<AnvilBlock> ANVIL_OBSIDIAN = BLOCKS.registerBlock("anvil_obsidian",
        properties -> new AnvilBlock(AnvilTier.OBSIDIAN, properties),
        stone(50, 1200).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> ANVIL_OBSIDIAN_ITEM = blockItem(ANVIL_OBSIDIAN);

    public static final DeferredBlock<PitKilnBlock> KILN_PIT = BLOCKS.registerBlock("kiln_pit", PitKilnBlock::new, wood(0.6f, 0));
    public static final DeferredItem<BlockItem> KILN_PIT_ITEM = blockItem(KILN_PIT);

    /** No loot table: the campfire's drops depend on what it holds and how it went out, so the block entity spills them. */
    public static final DeferredBlock<CampfireBlock> CAMPFIRE = BLOCKS.registerBlock("campfire", CampfireBlock::new,
        wood(0.5f, 0).noLootTable());
    public static final DeferredItem<BlockItem> CAMPFIRE_ITEM = blockItem(CAMPFIRE);

    public static final DeferredBlock<DryingRackBlock> DRYING_RACK_CRUDE = BLOCKS.registerBlock("drying_rack_crude",
        properties -> new DryingRackBlock(true, properties), wood(0.5f, 3));
    public static final DeferredItem<BlockItem> DRYING_RACK_CRUDE_ITEM = blockItem(DRYING_RACK_CRUDE);
    public static final DeferredBlock<DryingRackBlock> DRYING_RACK = BLOCKS.registerBlock("drying_rack",
        properties -> new DryingRackBlock(false, properties), wood(0.5f, 3));
    public static final DeferredItem<BlockItem> DRYING_RACK_ITEM = blockItem(DRYING_RACK);

    public static final DeferredBlock<WorktableBlock> WORKTABLE = BLOCKS.registerBlock("worktable",
        properties -> new WorktableBlock(false, properties), wood(2, 3));
    public static final DeferredItem<BlockItem> WORKTABLE_ITEM = blockItem(WORKTABLE);
    public static final DeferredBlock<WorktableBlock> WORKTABLE_STONE = BLOCKS.registerBlock("worktable_stone",
        properties -> new WorktableBlock(true, properties), stone(1.5f, 6));
    public static final DeferredItem<BlockItem> WORKTABLE_STONE_ITEM = blockItem(WORKTABLE_STONE);

    public static final DeferredBlock<CompactingBinBlock> COMPACTING_BIN = BLOCKS.registerBlock("compacting_bin", CompactingBinBlock::new,
        wood(4, 0.3f));
    public static final DeferredItem<BlockItem> COMPACTING_BIN_ITEM = blockItem(COMPACTING_BIN);

    public static final DeferredBlock<SoakingPotBlock> SOAKING_POT = BLOCKS.registerBlock("soaking_pot", SoakingPotBlock::new,
        stone(3, 3).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> SOAKING_POT_ITEM = blockItem(SOAKING_POT);

    public static final DeferredBlock<CompostBinBlock> COMPOST_BIN = BLOCKS.registerBlock("compost_bin", CompostBinBlock::new, wood(2, 0.3f));
    public static final DeferredItem<BlockItem> COMPOST_BIN_ITEM = blockItem(COMPOST_BIN);

    public static final DeferredBlock<BarrelBlock> BARREL = BLOCKS.registerBlock("barrel", BarrelBlock::new, wood(1, 1.8f));
    public static final DeferredItem<BlockItem> BARREL_ITEM = blockItem(BARREL, properties -> new BarrelItem(BARREL.get(), properties));

    public static final DeferredBlock<TanningRackBlock> TANNING_RACK = BLOCKS.registerBlock("tanning_rack", TanningRackBlock::new,
        wood(1, 0.12f));
    public static final DeferredItem<BlockItem> TANNING_RACK_ITEM = blockItem(TANNING_RACK);

    private TechBasicBlocks() {
    }

    private static BlockBehaviour.Properties wood(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(hardness, resistance).noOcclusion();
    }

    private static BlockBehaviour.Properties stone(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(hardness, resistance).noOcclusion();
    }

    private static DeferredItem<BlockItem> blockItem(DeferredBlock<? extends Block> block) {
        return blockItem(block, properties -> new BlockItem(block.get(), properties));
    }

    private static DeferredItem<BlockItem> blockItem(DeferredBlock<? extends Block> block, Function<Item.Properties, ? extends BlockItem> constructor) {
        @SuppressWarnings("unchecked")
        DeferredItem<BlockItem> item = (DeferredItem<BlockItem>) TechBasicItems.ITEMS.registerItem(block.getId().getPath(), constructor);
        BLOCK_ITEMS.add(item);
        return item;
    }
}
