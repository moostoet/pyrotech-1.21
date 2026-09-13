package com.moostoet.pyrotech.datagen.worldgen;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.block.BerryBushBlock;
import com.moostoet.pyrotech.core.block.FreckleberryPlantBlock;
import com.moostoet.pyrotech.worldgen.Generator;
import com.moostoet.pyrotech.worldgen.WorldgenFeatures;
import com.moostoet.pyrotech.worldgen.WorldgenTags;
import com.moostoet.pyrotech.worldgen.WorldgenToggleCondition;
import com.moostoet.pyrotech.worldgen.feature.CaveFloorClusterConfiguration;
import com.moostoet.pyrotech.worldgen.feature.DenseCoalConfiguration;
import com.moostoet.pyrotech.worldgen.feature.MudConfiguration;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * The worldgen data: one configured feature, one placed feature, and one biome modifier
 * per generator, all under the generator's id, with the generator's toggle as a condition
 * on the biome modifier. The values are the 1.12 config's as the worldgen sign-off
 * translated them.
 */
public final class WorldgenRegistryProvider extends DatapackBuiltinEntriesProvider {

    private static final Vec3i BELOW = Direction.DOWN.getNormal();
    private static final int PATCH_SPREAD = 4;

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
        .add(Registries.CONFIGURED_FEATURE, WorldgenRegistryProvider::configuredFeatures)
        .add(Registries.PLACED_FEATURE, WorldgenRegistryProvider::placedFeatures)
        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, WorldgenRegistryProvider::biomeModifiers);

    public WorldgenRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, BUILDER, WorldgenRegistryProvider::toggles, Set.of(Pyrotech.MOD_ID));
    }

    private static void toggles(BiConsumer<ResourceKey<?>, ICondition> conditions) {
        for (Generator generator : Generator.values()) {
            conditions.accept(biomeModifier(generator), new WorldgenToggleCondition(generator));
        }
    }

    private static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest netherrack = new BlockMatchTest(Blocks.NETHERRACK);
        // The 1.12 vein size ranges collapse to their midpoints (sign-off item 4).
        ore(context, Generator.FOSSIL, stone, CoreBlocks.FOSSIL_ORE, 13);
        ore(context, Generator.LIMESTONE, stone, CoreBlocks.LIMESTONE, 15);
        ore(context, Generator.DENSE_NETHER_COAL, netherrack, CoreBlocks.DENSE_NETHER_COAL_ORE, 15);
        register(context, Generator.DENSE_COAL, WorldgenFeatures.DENSE_COAL.get(), new DenseCoalConfiguration(
            new BlockMatchTest(Blocks.COAL_ORE), state(CoreBlocks.DENSE_COAL_ORE), 0, 32, UniformInt.of(3, 6)));

        // The tries reproduce the 1.12 cube-scan yields under the patch's triangular
        // spread (sign-off item 7); every patch tests for air (item 9).
        register(context, Generator.ROCKS, Feature.RANDOM_PATCH, patch(25, Feature.SIMPLE_BLOCK,
            new SimpleBlockConfiguration(BlockStateProvider.simple(CoreBlocks.ROCK_STONE.get())),
            BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesTag(BELOW, WorldgenTags.Blocks.ROCK_PLACEABLE_ON))));
        register(context, Generator.PYROBERRY_BUSH, Feature.RANDOM_PATCH, patch(12, Feature.SIMPLE_BLOCK,
            new SimpleBlockConfiguration(bushAges(CoreBlocks.PYROBERRY_BUSH)), BlockPredicate.ONLY_IN_AIR_PREDICATE));
        register(context, Generator.GLOAMBERRY_BUSH, Feature.RANDOM_PATCH, patch(12, Feature.SIMPLE_BLOCK,
            new SimpleBlockConfiguration(bushAges(CoreBlocks.GLOAMBERRY_BUSH)), BlockPredicate.ONLY_IN_AIR_PREDICATE));
        // A block column of height one places the plant without the crop's survival test,
        // which 1.12 skipped too; the 1.12 ground rule is the predicate.
        register(context, Generator.FRECKLEBERRY_PLANT, Feature.RANDOM_PATCH, patch(40, Feature.BLOCK_COLUMN,
            new BlockColumnConfiguration(List.of(BlockColumnConfiguration.layer(ConstantInt.of(1), freckleberryAges())),
                Direction.UP, BlockPredicate.alwaysTrue(), false),
            BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesTag(BELOW, BlockTags.DIRT),
                BlockPredicate.hasSturdyFace(BELOW, Direction.UP))));

        register(context, Generator.DENSE_REDSTONE, WorldgenFeatures.CAVE_FLOOR_CLUSTER.get(), new CaveFloorClusterConfiguration(
            1, UniformHeight.of(VerticalAnchor.absolute(5), VerticalAnchor.absolute(25)), BlockTags.BASE_STONE_OVERWORLD,
            variant(CoreBlocks.DENSE_REDSTONE_ORE_LARGE, 1, 3),
            variant(CoreBlocks.DENSE_REDSTONE_ORE_SMALL, 3, 5),
            variant(CoreBlocks.DENSE_REDSTONE_ORE_ROCKS, 6, 8),
            List.of(OreConfiguration.target(new BlockMatchTest(Blocks.STONE), Blocks.REDSTONE_ORE.defaultBlockState()),
                OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState())),
            0.5f));
        register(context, Generator.DENSE_QUARTZ, WorldgenFeatures.CAVE_FLOOR_CLUSTER.get(), new CaveFloorClusterConfiguration(
            20, UniformHeight.of(VerticalAnchor.absolute(1), VerticalAnchor.absolute(64)), BlockTags.BASE_STONE_NETHER,
            variant(CoreBlocks.DENSE_QUARTZ_ORE_LARGE, 1, 2),
            variant(CoreBlocks.DENSE_QUARTZ_ORE_SMALL, 3, 5),
            variant(CoreBlocks.DENSE_QUARTZ_ORE_ROCKS, 6, 8),
            List.of(OreConfiguration.target(netherrack, Blocks.NETHER_QUARTZ_ORE.defaultBlockState())),
            0.5f));
        register(context, Generator.MUD, WorldgenFeatures.MUD.get(),
            new MudConfiguration(state(CoreBlocks.MUD), state(CoreBlocks.ROCK_MUD), UniformInt.of(2, 4), 0.75f));
    }

    private static void placedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        // The 1.12 y bands stay absolute (sign-off item 3). Dense coal and the clusters
        // roll their own y and read only the chunk, so they take no height range.
        place(context, features, Generator.FOSSIL,
            CountPlacement.of(15), InSquarePlacement.spread(), band(40, 120), BiomeFilter.biome());
        place(context, features, Generator.LIMESTONE,
            CountPlacement.of(15), InSquarePlacement.spread(), band(8, 100), BiomeFilter.biome());
        place(context, features, Generator.DENSE_COAL, BiomeFilter.biome());
        place(context, features, Generator.DENSE_NETHER_COAL,
            CountPlacement.of(30), InSquarePlacement.spread(), band(1, 127), BiomeFilter.biome());
        // The 1.12 cluster frequencies 0.06 and 0.075 round to one in 17 and one in 13 (item 5).
        place(context, features, Generator.PYROBERRY_BUSH, RarityFilter.onAverageOnceEvery(17), CountPlacement.of(4),
            InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
        place(context, features, Generator.GLOAMBERRY_BUSH, RarityFilter.onAverageOnceEvery(17), CountPlacement.of(4),
            InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
        place(context, features, Generator.FRECKLEBERRY_PLANT, RarityFilter.onAverageOnceEvery(13), CountPlacement.of(4),
            InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
        place(context, features, Generator.ROCKS, CountPlacement.of(4),
            InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
        place(context, features, Generator.DENSE_REDSTONE,
            RarityFilter.onAverageOnceEvery(4), InSquarePlacement.spread(), BiomeFilter.biome());
        place(context, features, Generator.DENSE_QUARTZ,
            RarityFilter.onAverageOnceEvery(8), InSquarePlacement.spread(), BiomeFilter.biome());
        place(context, features, Generator.MUD, CountPlacement.of(8), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID,
            BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)), BiomeFilter.biome());
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderSet<Biome> overworld = biomes.getOrThrow(BiomeTags.IS_OVERWORLD);
        HolderSet<Biome> nether = biomes.getOrThrow(BiomeTags.IS_NETHER);
        // Dense coal runs a step after vanilla's coal; mud takes the step of vanilla's disks.
        modify(context, features, Generator.FOSSIL, overworld, Decoration.UNDERGROUND_ORES);
        modify(context, features, Generator.LIMESTONE, overworld, Decoration.UNDERGROUND_ORES);
        modify(context, features, Generator.DENSE_COAL, overworld, Decoration.UNDERGROUND_DECORATION);
        modify(context, features, Generator.DENSE_NETHER_COAL, nether, Decoration.UNDERGROUND_ORES);
        modify(context, features, Generator.PYROBERRY_BUSH,
            biomes.getOrThrow(WorldgenTags.Biomes.HAS_PYROBERRY_BUSH), Decoration.VEGETAL_DECORATION);
        modify(context, features, Generator.GLOAMBERRY_BUSH,
            biomes.getOrThrow(WorldgenTags.Biomes.HAS_GLOAMBERRY_BUSH), Decoration.VEGETAL_DECORATION);
        modify(context, features, Generator.FRECKLEBERRY_PLANT,
            biomes.getOrThrow(WorldgenTags.Biomes.HAS_FRECKLEBERRY_PLANT), Decoration.VEGETAL_DECORATION);
        modify(context, features, Generator.ROCKS, overworld, Decoration.VEGETAL_DECORATION);
        modify(context, features, Generator.DENSE_REDSTONE, overworld, Decoration.UNDERGROUND_DECORATION);
        modify(context, features, Generator.DENSE_QUARTZ, nether, Decoration.UNDERGROUND_DECORATION);
        modify(context, features, Generator.MUD, overworld, Decoration.UNDERGROUND_ORES);
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
        BootstrapContext<ConfiguredFeature<?, ?>> context, Generator generator, F feature, FC config) {
        context.register(configured(generator), new ConfiguredFeature<>(feature, config));
    }

    private static void ore(BootstrapContext<ConfiguredFeature<?, ?>> context, Generator generator,
                            RuleTest target, DeferredBlock<?> block, int size) {
        register(context, generator, Feature.ORE, new OreConfiguration(target, state(block), size));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> RandomPatchConfiguration patch(
        int tries, F feature, FC config, BlockPredicate predicate) {
        return new RandomPatchConfiguration(tries, PATCH_SPREAD, PATCH_SPREAD, PlacementUtils.filtered(feature, config, predicate));
    }

    /** The 1.12 bush age roll, 4 to 6. */
    private static BlockStateProvider bushAges(DeferredBlock<BerryBushBlock> bush) {
        return new RandomizedIntStateProvider(BlockStateProvider.simple(bush.get()), BerryBushBlock.AGE, UniformInt.of(4, 6));
    }

    /** The 1.12 roll: ripe one time in ten, otherwise uniform over ages 2 to 6, as weights out of 50. */
    private static BlockStateProvider freckleberryAges() {
        FreckleberryPlantBlock plant = CoreBlocks.FRECKLEBERRY_PLANT.get();
        SimpleWeightedRandomList.Builder<BlockState> ages = SimpleWeightedRandomList.builder();
        ages.add(plant.getStateForAge(plant.getMaxAge()), 5);
        for (int age = 2; age <= 6; age++) {
            ages.add(plant.getStateForAge(age), 9);
        }
        return new WeightedStateProvider(ages);
    }

    private static CaveFloorClusterConfiguration.Variant variant(DeferredBlock<?> block, int min, int max) {
        return new CaveFloorClusterConfiguration.Variant(state(block), UniformInt.of(min, max));
    }

    private static BlockState state(DeferredBlock<?> block) {
        return block.get().defaultBlockState();
    }

    private static PlacementModifier band(int minY, int maxY) {
        return HeightRangePlacement.uniform(VerticalAnchor.absolute(minY), VerticalAnchor.absolute(maxY));
    }

    private static void place(BootstrapContext<PlacedFeature> context, HolderGetter<ConfiguredFeature<?, ?>> features,
                              Generator generator, PlacementModifier... modifiers) {
        context.register(placed(generator), new PlacedFeature(features.getOrThrow(configured(generator)), List.of(modifiers)));
    }

    private static void modify(BootstrapContext<BiomeModifier> context, HolderGetter<PlacedFeature> features,
                               Generator generator, HolderSet<Biome> biomes, Decoration step) {
        context.register(biomeModifier(generator), new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes, HolderSet.direct(features.getOrThrow(placed(generator))), step));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(Generator generator) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, id(generator));
    }

    private static ResourceKey<PlacedFeature> placed(Generator generator) {
        return ResourceKey.create(Registries.PLACED_FEATURE, id(generator));
    }

    private static ResourceKey<BiomeModifier> biomeModifier(Generator generator) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, id(generator));
    }

    private static ResourceLocation id(Generator generator) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, generator.id());
    }
}
