package com.moostoet.pyrotech.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

import java.util.List;

/**
 * One cave-floor cluster: how many spots to try, the y band each try rolls, the floor
 * blocks it stands on, the large, small, and rocks variants with their counts, and the
 * vanilla ore that appears under a placed block at {@code ore_chance}.
 */
public record CaveFloorClusterConfiguration(int attempts, HeightProvider height, TagKey<Block> floor,
                                            Variant large, Variant small, Variant rocks,
                                            List<OreConfiguration.TargetBlockState> oreBelow, float oreChance)
    implements FeatureConfiguration {

    public static final Codec<CaveFloorClusterConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("attempts").forGetter(CaveFloorClusterConfiguration::attempts),
        HeightProvider.CODEC.fieldOf("height").forGetter(CaveFloorClusterConfiguration::height),
        TagKey.codec(Registries.BLOCK).fieldOf("floor").forGetter(CaveFloorClusterConfiguration::floor),
        Variant.CODEC.fieldOf("large").forGetter(CaveFloorClusterConfiguration::large),
        Variant.CODEC.fieldOf("small").forGetter(CaveFloorClusterConfiguration::small),
        Variant.CODEC.fieldOf("rocks").forGetter(CaveFloorClusterConfiguration::rocks),
        OreConfiguration.TargetBlockState.CODEC.listOf().fieldOf("ore_below").forGetter(CaveFloorClusterConfiguration::oreBelow),
        Codec.floatRange(0, 1).fieldOf("ore_chance").forGetter(CaveFloorClusterConfiguration::oreChance)
    ).apply(instance, CaveFloorClusterConfiguration::new));

    /** One size of the cluster block and how many of it a cluster places. */
    public record Variant(BlockState state, IntProvider count) {

        public static final Codec<Variant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("state").forGetter(Variant::state),
            IntProvider.POSITIVE_CODEC.fieldOf("count").forGetter(Variant::count)
        ).apply(instance, Variant::new));
    }
}
