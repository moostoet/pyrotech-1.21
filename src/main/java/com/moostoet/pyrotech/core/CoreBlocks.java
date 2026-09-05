package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Core's blocks. Ids are the 1.12 registry names. Hardness and resistance carry the
 * 1.12 values converted to 1.21's scale: a 1.12 {@code setResistance(r)} is {@code r * 0.6}
 * here, and a block that only set hardness has resistance equal to its hardness.
 */
public final class CoreBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Pyrotech.MOD_ID);

    private static final List<DeferredItem<BlockItem>> BLOCK_ITEMS = new ArrayList<>();

    public static final DeferredBlock<Block> CHARCOAL_BLOCK = simple("charcoal_block", rock(MapColor.STONE, 5, 5));
    public static final DeferredBlock<Block> COAL_COKE_BLOCK = simple("coal_coke_block", rock(MapColor.STONE, 5, 5));
    public static final DeferredBlock<Block> CRAFTING_TABLE_TEMPLATE = simple("crafting_table_template",
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(5, 6).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> REFRACTORY_BRICK_BLOCK = simple("refractory_brick_block", rock(MapColor.SAND, 3, 6));
    public static final DeferredBlock<Block> MASONRY_BRICK_BLOCK = simple("masonry_brick_block", rock(MapColor.STONE, 1.5f, 6));
    public static final DeferredBlock<Block> LIMESTONE = simple("limestone", rock(MapColor.STONE, 1.5f, 6));
    public static final DeferredBlock<Block> FOSSIL_ORE = simple("fossil_ore", rock(MapColor.STONE, 3, 3));
    public static final DeferredBlock<Block> DENSE_COAL_ORE = simple("dense_coal_ore", rock(MapColor.STONE, 5, 5));
    public static final DeferredBlock<Block> DENSE_NETHER_COAL_ORE = simple("dense_nether_coal_ore", rock(MapColor.STONE, 10, 10));
    public static final DeferredBlock<Block> PLANKS_TARRED = simple("planks_tarred",
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2, 3));
    public static final DeferredBlock<Block> WOOL_TARRED = simple("wool_tarred",
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).sound(SoundType.WOOL).strength(0.8f));
    public static final DeferredBlock<Block> WOOD_TAR_BLOCK = simple("wood_tar_block",
        BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).sound(SoundType.SLIME_BLOCK).strength(2));

    private CoreBlocks() {
    }

    /** A 1.12 {@code Material.ROCK} block: stone sound, and a pickaxe needed for drops. */
    private static BlockBehaviour.Properties rock(MapColor color, float destroyTime, float resistance) {
        return BlockBehaviour.Properties.of()
            .mapColor(color)
            .sound(SoundType.STONE)
            .strength(destroyTime, resistance)
            .requiresCorrectToolForDrops();
    }

    private static DeferredBlock<Block> simple(String name, BlockBehaviour.Properties properties) {
        DeferredBlock<Block> block = BLOCKS.registerSimpleBlock(name, properties);
        BLOCK_ITEMS.add(CoreItems.ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    static void addToTab(CreativeModeTab.Output output) {
        for (DeferredItem<BlockItem> item : BLOCK_ITEMS) {
            output.accept(item.get());
        }
    }
}
