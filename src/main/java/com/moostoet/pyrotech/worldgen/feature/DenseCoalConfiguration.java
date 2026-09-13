package com.moostoet.pyrotech.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * What dense coal converts and where it looks. The scan runs from {@code min_y} up to,
 * but not including, {@code max_y}, as the 1.12 loop did; {@code size} is how many
 * connected target blocks one vein turns into {@code state}.
 */
public record DenseCoalConfiguration(RuleTest target, BlockState state, int minY, int maxY, IntProvider size)
    implements FeatureConfiguration {

    public static final Codec<DenseCoalConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RuleTest.CODEC.fieldOf("target").forGetter(DenseCoalConfiguration::target),
        BlockState.CODEC.fieldOf("state").forGetter(DenseCoalConfiguration::state),
        Codec.INT.fieldOf("min_y").forGetter(DenseCoalConfiguration::minY),
        Codec.INT.fieldOf("max_y").forGetter(DenseCoalConfiguration::maxY),
        IntProvider.POSITIVE_CODEC.fieldOf("size").forGetter(DenseCoalConfiguration::size)
    ).apply(instance, DenseCoalConfiguration::new));
}
