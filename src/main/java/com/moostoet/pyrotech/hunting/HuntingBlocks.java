package com.moostoet.pyrotech.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.block.ButchersBlock;
import com.moostoet.pyrotech.hunting.block.CarcassBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Hunting's two blocks with their 1.12 numbers: the carcass, {@code Material.GROUND} at
 * hardness 0.25 with the slime sound, and the butcher's block, {@code Material.WOOD} at
 * hardness 1. Both were partial blocks, so neither occludes.
 */
public final class HuntingBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Pyrotech.MOD_ID);

    public static final DeferredBlock<CarcassBlock> CARCASS = BLOCKS.registerBlock("carcass", CarcassBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).sound(SoundType.SLIME_BLOCK).strength(0.25f).noOcclusion());
    /** One to a stack, carrying its contents in vanilla's container component. */
    public static final DeferredItem<BlockItem> CARCASS_ITEM = HuntingItems.ITEMS.registerItem("carcass",
        properties -> new BlockItem(CARCASS.get(), properties), new Item.Properties().stacksTo(1));

    public static final DeferredBlock<ButchersBlock> BUTCHERS_BLOCK = BLOCKS.registerBlock("butchers_block", ButchersBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1).noOcclusion());
    public static final DeferredItem<BlockItem> BUTCHERS_BLOCK_ITEM = HuntingItems.ITEMS.registerSimpleBlockItem(BUTCHERS_BLOCK);

    private HuntingBlocks() {
    }
}
