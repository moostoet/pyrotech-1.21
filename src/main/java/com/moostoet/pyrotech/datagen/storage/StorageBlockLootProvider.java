package com.moostoet.pyrotech.datagen.storage;

import com.moostoet.pyrotech.storage.StorageBlocks;
import com.moostoet.pyrotech.storage.StorageComponents;
import com.moostoet.pyrotech.storage.block.BagBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Set;

/**
 * Storage's block loot: every block drops itself, and the tanks and bags carry their
 * contents in the dropped item as 1.12 did (storage sign-off, item 9), the bags with their
 * open state too. The stash, shelf, crate, and rack spill their items in code on break.
 */
public final class StorageBlockLootProvider extends BlockLootSubProvider {

    public StorageBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (Block block : List.of(
            StorageBlocks.STASH.get(), StorageBlocks.STASH_STONE.get(),
            StorageBlocks.SHELF.get(), StorageBlocks.SHELF_STONE.get(),
            StorageBlocks.CRATE.get(), StorageBlocks.CRATE_STONE.get(),
            StorageBlocks.WOOD_RACK.get(),
            StorageBlocks.FAUCET_STONE.get(), StorageBlocks.FAUCET_BRICK.get())) {
            this.dropSelf(block);
        }
        this.add(StorageBlocks.STONE_TANK.get(), block -> this.dropWithComponent(block, StorageComponents.TANK_FLUID.get()));
        this.add(StorageBlocks.BRICK_TANK.get(), block -> this.dropWithComponent(block, StorageComponents.TANK_FLUID.get()));
        this.add(StorageBlocks.BAG_SIMPLE.get(), this::createBagDrop);
        this.add(StorageBlocks.BAG_DURABLE.get(), this::createBagDrop);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return StorageBlocks.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::get).toList();
    }

    /** The shulker box pattern: the block with one component copied from its block entity. */
    private LootTable.Builder dropWithComponent(Block block, DataComponentType<?> component) {
        return this.singleDrop(block, LootItem.lootTableItem(block)
            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(component)));
    }

    private LootTable.Builder createBagDrop(Block block) {
        return this.singleDrop(block, LootItem.lootTableItem(block)
            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(StorageComponents.BAG_CONTENTS.get()))
            .apply(CopyBlockState.copyState(block).copy(BagBlock.TYPE)));
    }

    private LootTable.Builder singleDrop(Block block, LootPoolSingletonContainer.Builder<?> entry) {
        return LootTable.lootTable().withPool(this.applyExplosionCondition(block,
            LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(entry)));
    }
}
