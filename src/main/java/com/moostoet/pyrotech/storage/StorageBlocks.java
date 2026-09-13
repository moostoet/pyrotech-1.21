package com.moostoet.pyrotech.storage;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.storage.block.BagBlock;
import com.moostoet.pyrotech.storage.block.CrateBlock;
import com.moostoet.pyrotech.storage.block.FaucetBlock;
import com.moostoet.pyrotech.storage.block.ShelfBlock;
import com.moostoet.pyrotech.storage.block.StashBlock;
import com.moostoet.pyrotech.storage.block.TankBlock;
import com.moostoet.pyrotech.storage.block.WoodRackBlock;
import com.moostoet.pyrotech.storage.item.BagItem;
import com.moostoet.pyrotech.storage.item.TankBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Storage's thirteen blocks, in the 1.12 registration order. Each durable twin is the same
 * class with its 1.12 numbers as constructor arguments. Hardness and resistance are the
 * 1.12 values on 1.21's scale, as core converts them: a 1.12 {@code setResistance(r)} is
 * {@code r * 0.6}, and a block that only set hardness has resistance equal to its hardness.
 */
public final class StorageBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Pyrotech.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pyrotech.MOD_ID);

    private static final List<DeferredItem<? extends BlockItem>> BLOCK_ITEMS = new ArrayList<>();

    public static final DeferredBlock<ShelfBlock> SHELF = register("shelf", properties -> new ShelfBlock(1, properties), wood(2, 3));
    public static final DeferredBlock<ShelfBlock> SHELF_STONE = register("shelf_stone", properties -> new ShelfBlock(2, properties), wood(1.5f, 6));
    public static final DeferredBlock<StashBlock> STASH = register("stash", properties -> new StashBlock(10, properties), wood(2, 3));
    public static final DeferredBlock<StashBlock> STASH_STONE = register("stash_stone", properties -> new StashBlock(20, properties), wood(1.5f, 6));
    public static final DeferredBlock<CrateBlock> CRATE = register("crate", properties -> new CrateBlock(1, properties), wood(2, 3));
    public static final DeferredBlock<CrateBlock> CRATE_STONE = register("crate_stone", properties -> new CrateBlock(2, properties), wood(1.5f, 6));
    // 1.12 set hardness 0.75 and no sound type, so the rack played stone sounds by accident; wood is the honest choice.
    public static final DeferredBlock<WoodRackBlock> WOOD_RACK = register("wood_rack", WoodRackBlock::new, wood(0.75f, 0.75f));

    public static final DeferredBlock<TankBlock> STONE_TANK = BLOCKS.registerBlock("stone_tank",
        properties -> new TankBlock(4000, false, properties), stone(2, 2));
    public static final DeferredBlock<TankBlock> BRICK_TANK = BLOCKS.registerBlock("brick_tank",
        properties -> new TankBlock(8000, true, properties), stone(2, 2));
    public static final DeferredItem<TankBlockItem> STONE_TANK_ITEM = tankItem(STONE_TANK);
    public static final DeferredItem<TankBlockItem> BRICK_TANK_ITEM = tankItem(BRICK_TANK);
    public static final List<DeferredItem<TankBlockItem>> TANK_ITEMS = List.of(STONE_TANK_ITEM, BRICK_TANK_ITEM);

    // The stone faucet moves 10 mB a tick and shuts off after 1000; the brick one 20 mB with no limit.
    public static final DeferredBlock<FaucetBlock> FAUCET_STONE = register("faucet_stone",
        properties -> new FaucetBlock(10, 1000, false, properties), stone(1.5f, 1.5f));
    public static final DeferredBlock<FaucetBlock> FAUCET_BRICK = register("faucet_brick",
        properties -> new FaucetBlock(20, FaucetBlock.NO_LIMIT, true, properties), stone(1.5f, 1.5f));

    public static final DeferredBlock<BagBlock> BAG_SIMPLE = BLOCKS.registerBlock("bag_simple",
        properties -> new BagBlock(640, StorageTags.Items.ROCK_BAG_ITEMS, properties), bag());
    public static final DeferredBlock<BagBlock> BAG_DURABLE = BLOCKS.registerBlock("bag_durable",
        properties -> new BagBlock(2560, StorageTags.Items.DURABLE_ROCK_BAG_ITEMS, properties), bag());
    public static final DeferredItem<BagItem> BAG_SIMPLE_ITEM = bagItem(BAG_SIMPLE);
    public static final DeferredItem<BagItem> BAG_DURABLE_ITEM = bagItem(BAG_DURABLE);
    public static final List<DeferredItem<BagItem>> BAG_ITEMS = List.of(BAG_SIMPLE_ITEM, BAG_DURABLE_ITEM);

    private StorageBlocks() {
    }

    /** 1.12 {@code Material.WOOD} with the wood sound, which the durable twins kept. Partial, so no occlusion. */
    private static BlockBehaviour.Properties wood(float destroyTime, float resistance) {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(destroyTime, resistance)
            .noOcclusion();
    }

    /** 1.12 {@code Material.ROCK} with pickaxe level 0: a pickaxe for drops. The tank's windows and the faucet's spout see through. */
    private static BlockBehaviour.Properties stone(float destroyTime, float resistance) {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .strength(destroyTime, resistance)
            .requiresCorrectToolForDrops()
            .noOcclusion();
    }

    private static BlockBehaviour.Properties bag() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOL)
            .sound(SoundType.WOOL)
            .strength(0.2f)
            .noOcclusion();
    }

    private static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, ? extends B> constructor,
                                                               BlockBehaviour.Properties properties) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, constructor, properties);
        BLOCK_ITEMS.add(ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    private static DeferredItem<TankBlockItem> tankItem(DeferredBlock<TankBlock> block) {
        DeferredItem<TankBlockItem> item = ITEMS.registerItem(block.getId().getPath(),
            properties -> new TankBlockItem(block.get(), properties));
        BLOCK_ITEMS.add(item);
        return item;
    }

    /** One bag to a stack, carrying its contents in a component that an empty bag lacks, so it equals a fresh one. */
    private static DeferredItem<BagItem> bagItem(DeferredBlock<BagBlock> block) {
        DeferredItem<BagItem> item = ITEMS.registerItem(block.getId().getPath(),
            properties -> new BagItem(block.get(), properties),
            new Item.Properties().stacksTo(1));
        BLOCK_ITEMS.add(item);
        return item;
    }

    static void addToTab(BuildCreativeModeTabContentsEvent event) {
        for (DeferredItem<? extends BlockItem> item : BLOCK_ITEMS) {
            event.accept(item.get());
        }
    }
}
