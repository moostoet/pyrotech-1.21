package com.moostoet.pyrotech.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** The mud block, the mud rock, the radius of the sphere, and the chance of a rock over open ground. */
public record MudConfiguration(BlockState mud, BlockState rock, IntProvider radius, float rockChance)
    implements FeatureConfiguration {

    public static final Codec<MudConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockState.CODEC.fieldOf("mud").forGetter(MudConfiguration::mud),
        BlockState.CODEC.fieldOf("rock").forGetter(MudConfiguration::rock),
        IntProvider.NON_NEGATIVE_CODEC.fieldOf("radius").forGetter(MudConfiguration::radius),
        Codec.floatRange(0, 1).fieldOf("rock_chance").forGetter(MudConfiguration::rockChance)
    ).apply(instance, MudConfiguration::new));
}
