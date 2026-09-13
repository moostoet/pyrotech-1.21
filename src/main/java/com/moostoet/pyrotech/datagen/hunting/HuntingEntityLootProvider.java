package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.hunting.HuntingEntities;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SlimePredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.stream.Stream;

/**
 * The mud's loot: the 1.12 table, zero to two mud rocks plus up to one per looting level,
 * gated to the smallest size in the table as vanilla gates the slime's, since the 1.12
 * {@code getLootTable} override did the gating in code.
 */
public final class HuntingEntityLootProvider extends EntityLootSubProvider {

    public HuntingEntityLootProvider(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    public void generate() {
        this.add(HuntingEntities.MUD.get(), LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(CoreBlocks.ROCK_MUD.get())
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(0, 2)))
                    .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries, UniformGenerator.between(0, 1))))
                .when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                    EntityPredicate.Builder.entity().subPredicate(SlimePredicate.sized(MinMaxBounds.Ints.exactly(1)))))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return HuntingEntities.ENTITY_TYPES.getEntries().stream().map(holder -> (EntityType<?>) holder.get());
    }
}
