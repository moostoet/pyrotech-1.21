package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.hunting.HuntingBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

/** Hunting's block loot: the carcass drops itself with its contents, the shulker box way; the butcher's block drops itself. */
public final class HuntingBlockLootProvider extends BlockLootSubProvider {

    public HuntingBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        this.add(HuntingBlocks.CARCASS.get(), block -> LootTable.lootTable().withPool(this.applyExplosionCondition(block,
            LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(block)
                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(DataComponents.CONTAINER))))));
        this.dropSelf(HuntingBlocks.BUTCHERS_BLOCK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return HuntingBlocks.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::get).toList();
    }
}
