package com.moostoet.pyrotech.datagen.tech.basic;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.tech.basic.TechBasicBlocks;
import com.moostoet.pyrotech.tech.basic.TechBasicComponents;
import com.moostoet.pyrotech.tech.basic.block.AnvilBlock;
import com.moostoet.pyrotech.tech.basic.block.BarrelBlock;
import com.moostoet.pyrotech.tech.basic.block.ChoppingBlockBlock;
import com.moostoet.pyrotech.tech.basic.block.PitKilnBlock;
import com.moostoet.pyrotech.tech.basic.block.PitKilnVariant;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Set;

/**
 * Tech/basic's block loot. The chopping block and the anvils carry their wear stage in
 * the vanilla block state component (tech/basic sign-off, item 8). The pit kiln drops
 * itself while unlit, plus its thatch once thatched, and nothing once lit or burned out.
 * The barrel drops plain when open and with everything it holds when sealed (item 9).
 * The campfire has no table: its block entity spills what it holds.
 */
public final class TechBasicBlockLootProvider extends BlockLootSubProvider {

    public TechBasicBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (Block block : List.of(
            TechBasicBlocks.DRYING_RACK_CRUDE.get(), TechBasicBlocks.DRYING_RACK.get(),
            TechBasicBlocks.WORKTABLE.get(), TechBasicBlocks.WORKTABLE_STONE.get(),
            TechBasicBlocks.COMPACTING_BIN.get(), TechBasicBlocks.SOAKING_POT.get(),
            TechBasicBlocks.COMPOST_BIN.get(), TechBasicBlocks.TANNING_RACK.get())) {
            this.dropSelf(block);
        }
        this.add(TechBasicBlocks.CHOPPING_BLOCK.get(), block -> this.singleDrop(block,
            LootItem.lootTableItem(block).apply(CopyBlockState.copyState(block).copy(ChoppingBlockBlock.DAMAGE))));
        for (Block anvil : List.of(TechBasicBlocks.ANVIL_GRANITE.get(), TechBasicBlocks.ANVIL_IRON_PLATED.get(), TechBasicBlocks.ANVIL_OBSIDIAN.get())) {
            this.add(anvil, block -> this.singleDrop(block,
                LootItem.lootTableItem(block).apply(CopyBlockState.copyState(block).copy(AnvilBlock.DAMAGE))));
        }
        this.add(TechBasicBlocks.KILN_PIT.get(), this::createPitKilnDrop);
        this.add(TechBasicBlocks.BARREL.get(), this::createBarrelDrop);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return TechBasicBlocks.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::get).toList();
    }

    private LootTable.Builder createPitKilnDrop(Block block) {
        return LootTable.lootTable()
            .withPool(this.applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block))
                .when(AnyOfCondition.anyOf(variant(block, PitKilnVariant.EMPTY), variant(block, PitKilnVariant.THATCH),
                    variant(block, PitKilnVariant.WOOD)))))
            .withPool(this.applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(CoreBlocks.THATCH.get()))
                .when(AnyOfCondition.anyOf(variant(block, PitKilnVariant.THATCH), variant(block, PitKilnVariant.WOOD)))));
    }

    private static LootItemCondition.Builder variant(Block block, PitKilnVariant variant) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(PitKilnBlock.VARIANT, variant));
    }

    private LootTable.Builder createBarrelDrop(Block block) {
        LootItemCondition.Builder sealed = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(BarrelBlock.SEALED, true));
        return LootTable.lootTable()
            .withPool(this.applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block))
                .when(sealed.invert())))
            .withPool(this.applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                    .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                        .include(TechBasicComponents.BARREL_FLUID.get())
                        .include(DataComponents.CONTAINER)
                        .include(DataComponents.MAX_STACK_SIZE)
                        .include(DataComponents.ITEM_NAME))
                    .apply(CopyBlockState.copyState(block).copy(BarrelBlock.SEALED)))
                .when(sealed)));
    }

    private LootTable.Builder singleDrop(Block block, LootPoolSingletonContainer.Builder<?> entry) {
        return LootTable.lootTable().withPool(this.applyExplosionCondition(block,
            LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(entry)));
    }
}
