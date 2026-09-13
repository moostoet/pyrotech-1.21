package com.moostoet.pyrotech.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * The 1.12 mud generator. Its origin is the water over a floor; when that floor is
 * dirt, a sphere around it turns dirt touching water into mud, drops a mud rock on three
 * quarters of the open ground, and turns the ground under the rest into mud where the
 * sky is hidden. The sky test reads the world-surface heightmap, because worldgen runs
 * before the light engine; 1.12 read the sky light.
 */
public final class MudFeature extends Feature<MudConfiguration> {

    public MudFeature() {
        super(MudConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<MudConfiguration> context) {
        MudConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos start = context.origin().below();
        if (!level.getBlockState(start).is(Blocks.DIRT)) {
            return false;
        }
        int radius = config.radius().sample(random);
        for (BlockPos pos : BlockPos.betweenClosed(start.offset(-radius, -radius, -radius), start.offset(radius, radius, radius))) {
            if (pos.distSqr(start) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.DIRT)) {
                if (this.touchesWater(level, pos)) {
                    level.setBlock(pos, config.mud(), 2);
                }
            } else if (state.isAir()) {
                BlockPos below = pos.below();
                if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    if (random.nextFloat() < config.rockChance()) {
                        level.setBlock(pos, config.rock(), 2);
                    } else if (!this.canSeeSky(level, pos)) {
                        level.setBlock(below, config.mud(), 2);
                    }
                }
            }
        }
        return true;
    }

    private boolean touchesWater(WorldGenLevel level, BlockPos pos) {
        if (level.getBlockState(pos.above()).is(Blocks.WATER)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSeeSky(WorldGenLevel level, BlockPos pos) {
        return pos.getY() >= level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
    }
}
