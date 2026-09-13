package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.block.BerryBushBlock;
import com.moostoet.pyrotech.core.block.StrawBedBlock;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Set;

/**
 * Core's block loot tables: the 1.12 {@code getDrops}, {@code getItemDropped}, and
 * {@code quantityDropped} overrides as data. A block with none of those drops itself.
 * 1.12's silk touch default was "any full cube without a tile", so the full-cube ores and
 * limestone keep it, while the bushes and the small ore sizes refused it in code and the
 * large ore sizes allowed it explicitly. Every block core registers must have a table or
 * be marked {@code noLootTable}, or datagen fails.
 */
public final class CoreBlockLootProvider extends BlockLootSubProvider {

    private final Holder<Enchantment> fortune;

    public CoreBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        this.fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
    }

    @Override
    protected void generate() {
        for (Block block : List.of(
            CoreBlocks.CHARCOAL_BLOCK.get(), CoreBlocks.COAL_COKE_BLOCK.get(), CoreBlocks.CRAFTING_TABLE_TEMPLATE.get(),
            CoreBlocks.REFRACTORY_BRICK_BLOCK.get(), CoreBlocks.MASONRY_BRICK_BLOCK.get(),
            CoreBlocks.PLANKS_TARRED.get(), CoreBlocks.WOOL_TARRED.get(), CoreBlocks.WOOD_TAR_BLOCK.get(),
            CoreBlocks.MASONRY_BRICK_STAIRS.get(), CoreBlocks.REFRACTORY_BRICK_STAIRS.get(),
            CoreBlocks.MASONRY_BRICK_WALL.get(), CoreBlocks.REFRACTORY_BRICK_WALL.get(),
            // 1.12's Material.GLASS blocks were plain Blocks, not BlockGlass, so they drop.
            CoreBlocks.REFRACTORY_GLASS.get(), CoreBlocks.SLAG_GLASS.get(),
            CoreBlocks.COBBLESTONE_ANDESITE.get(), CoreBlocks.COBBLESTONE_DIORITE.get(),
            CoreBlocks.COBBLESTONE_GRANITE.get(), CoreBlocks.COBBLESTONE_LIMESTONE.get(),
            CoreBlocks.MUD.get(), CoreBlocks.COB_WET.get(), CoreBlocks.COB_DRY.get(),
            CoreBlocks.LOG_PILE.get(), CoreBlocks.LIVING_TAR.get(), CoreBlocks.THATCH.get())) {
            this.dropSelf(block);
        }
        for (Block rock : List.of(
            CoreBlocks.ROCK_STONE.get(), CoreBlocks.ROCK_GRANITE.get(), CoreBlocks.ROCK_DIORITE.get(),
            CoreBlocks.ROCK_ANDESITE.get(), CoreBlocks.ROCK_DIRT.get(), CoreBlocks.ROCK_SAND.get(),
            CoreBlocks.ROCK_SANDSTONE.get(), CoreBlocks.ROCK_WOOD_CHIPS.get(), CoreBlocks.ROCK_LIMESTONE.get(),
            CoreBlocks.ROCK_SAND_RED.get(), CoreBlocks.ROCK_SANDSTONE_RED.get(), CoreBlocks.ROCK_MUD.get(),
            CoreBlocks.ROCK_GRASS.get(), CoreBlocks.ROCK_NETHERRACK.get())) {
            this.dropSelf(rock);
        }

        this.add(CoreBlocks.MASONRY_BRICK_SLAB.get(), this::createSlabItemTable);
        this.add(CoreBlocks.REFRACTORY_BRICK_SLAB.get(), this::createSlabItemTable);
        this.add(CoreBlocks.REFRACTORY_DOOR.get(), this::createDoorTable);
        this.add(CoreBlocks.STONE_DOOR.get(), this::createDoorTable);

        this.add(CoreBlocks.LIMESTONE.get(), block -> this.createSingleItemTableWithSilkTouch(block, CoreBlocks.COBBLESTONE_LIMESTONE.get()));
        this.add(CoreBlocks.FOSSIL_ORE.get(), block -> this.createFortuneAddedDrops(block, Items.BONE, 1, 3));
        this.add(CoreBlocks.DENSE_COAL_ORE.get(), block -> this.createFortuneAddedDrops(block, Items.COAL, 8, 15));
        this.add(CoreBlocks.DENSE_NETHER_COAL_ORE.get(), block -> this.createFortuneAddedDrops(block, Items.COAL, 8, 23));

        this.add(CoreBlocks.DENSE_QUARTZ_ORE_LARGE.get(), block -> this.createClusterDrops(block, Material.DENSE_QUARTZ, Items.QUARTZ, 4, 11, true));
        this.add(CoreBlocks.DENSE_QUARTZ_ORE_SMALL.get(), block -> this.createClusterDrops(block, Material.DENSE_QUARTZ, Items.QUARTZ, 2, 4, false));
        this.add(CoreBlocks.DENSE_QUARTZ_ORE_ROCKS.get(), block -> this.createClusterDrops(block, Material.DENSE_QUARTZ, Items.QUARTZ, 1, 2, false));
        this.add(CoreBlocks.DENSE_REDSTONE_ORE_LARGE.get(), block -> this.createClusterDrops(block, Material.DENSE_REDSTONE, Items.REDSTONE, 4, 11, true));
        this.add(CoreBlocks.DENSE_REDSTONE_ORE_SMALL.get(), block -> this.createClusterDrops(block, Material.DENSE_REDSTONE, Items.REDSTONE, 2, 5, false));
        this.add(CoreBlocks.DENSE_REDSTONE_ORE_ROCKS.get(), block -> this.createClusterDrops(block, Material.DENSE_REDSTONE, Items.REDSTONE, 1, 2, false));

        this.add(CoreBlocks.PYROBERRY_BUSH.get(), block -> this.createBushDrops(block, CoreItems.PYROBERRY_SEEDS.get()));
        this.add(CoreBlocks.GLOAMBERRY_BUSH.get(), block -> this.createBushDrops(block, CoreItems.GLOAMBERRY_SEEDS.get()));
        CropBlock freckleberry = CoreBlocks.FRECKLEBERRY_PLANT.get();
        this.add(freckleberry, this.createCropDrops(freckleberry, CoreItems.FRECKLEBERRIES.get(), CoreItems.FRECKLEBERRY_SEEDS.get(),
            LootItemBlockStatePropertyCondition.hasBlockStateProperties(freckleberry)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CropBlock.AGE, freckleberry.getMaxAge()))));

        this.add(CoreBlocks.MUD_LAYER.get(), this.createSingleItemTable(CoreBlocks.ROCK_MUD.get(), ConstantValue.exactly(2)));
        this.dropOther(CoreBlocks.FARMLAND_MULCHED.get(), Items.DIRT);
        this.add(CoreBlocks.STRAW_BED.get(), block -> this.createSinglePropConditionTable(block, StrawBedBlock.PART, BedPart.HEAD));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return CoreBlocks.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::get).toList();
    }

    /**
     * The 1.12 {@code rand.nextInt(max - min + 1) + min + fortune}: a uniform count plus
     * exactly the fortune level, capped at three. Silk touch gives the block itself.
     */
    private LootTable.Builder createFortuneAddedDrops(Block block, Item item, int min, int max) {
        LootPoolSingletonContainer.Builder<?> drops = LootItem.lootTableItem(item)
            .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1), true).when(this.fortuneAtLeast(1)))
            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1), true).when(this.fortuneAtLeast(2)))
            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1), true).when(this.fortuneAtLeast(3)));
        return this.createSilkTouchDispatchTable(block, this.applyExplosionDecay(block, drops));
    }

    /**
     * A dense ore cluster: {@code quantityDropped + rand.nextInt(fortune + 1)} units, each a
     * quarter chance the vanilla item and otherwise the dense material. The count is the
     * pool's rolls, so every unit picks on its own as the 1.12 loop did; the fortune extra
     * is one more pool per fortune level, rolled zero to that level times.
     */
    private LootTable.Builder createClusterDrops(Block block, Material dense, Item vanilla, int min, int max, boolean silkTouch) {
        Item common = CoreItems.material(dense).get();
        LootItemCondition.Builder noSilkTouch = this.doesNotHaveSilkTouch();
        LootTable.Builder table = LootTable.lootTable();
        if (silkTouch) {
            table.withPool(LootPool.lootPool().when(this.hasSilkTouch()).add(LootItem.lootTableItem(block)));
        }
        LootPool.Builder base = this.clusterPool(block, common, vanilla).setRolls(UniformGenerator.between(min, max));
        table.withPool(silkTouch ? base.when(noSilkTouch) : base);
        for (int level = 1; level <= 3; level++) {
            LootItemCondition.Builder fortuneIs = level == 3 ? this.fortuneAtLeast(3) : this.fortuneIs(level);
            LootPool.Builder extra = this.clusterPool(block, common, vanilla)
                .setRolls(UniformGenerator.between(0, level))
                .when(fortuneIs);
            table.withPool(silkTouch ? extra.when(noSilkTouch) : extra);
        }
        return table;
    }

    private LootPool.Builder clusterPool(Block block, Item common, Item vanilla) {
        return LootPool.lootPool()
            .add(this.applyExplosionDecay(block, LootItem.lootTableItem(common).setWeight(3)))
            .add(this.applyExplosionDecay(block, LootItem.lootTableItem(vanilla).setWeight(1)));
    }

    /** One seed while the bush is younger than age 3, one to three sticks from then on. No silk touch. */
    private LootTable.Builder createBushDrops(Block block, ItemLike seeds) {
        LootItemCondition.Builder young = AnyOfCondition.anyOf(this.age(block, 0), this.age(block, 1), this.age(block, 2));
        return this.applyExplosionDecay(block, LootTable.lootTable().withPool(LootPool.lootPool()
            .add(LootItem.lootTableItem(seeds).when(young)
                .otherwise(LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))))));
    }

    private LootItemCondition.Builder age(Block block, int age) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(BerryBushBlock.AGE, age));
    }

    private LootItemCondition.Builder fortuneAtLeast(int level) {
        return this.fortune(MinMaxBounds.Ints.atLeast(level));
    }

    private LootItemCondition.Builder fortuneIs(int level) {
        return this.fortune(MinMaxBounds.Ints.exactly(level));
    }

    private LootItemCondition.Builder fortune(MinMaxBounds.Ints level) {
        return MatchTool.toolMatches(ItemPredicate.Builder.item().withSubPredicate(ItemSubPredicates.ENCHANTMENTS,
            ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(this.fortune, level)))));
    }
}
