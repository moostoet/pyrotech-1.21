package com.moostoet.pyrotech.worldgen.feature;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.12 dense redstone and dense quartz generators as one feature. Each attempt
 * picks a spot in the chunk and scans the nine by nine by nine cube around it for air
 * over a floor block. The large variant goes on the first such positions in scan order;
 * once every large block is down, the cube re-centres on the last one, and the small and
 * then the rocks variants go on shuffled positions in it. Each placed block turns the
 * block under it into the matching vanilla ore at {@code ore_chance}. An attempt that
 * places no large block fails, and the next attempt rolls a new spot.
 */
public final class CaveFloorClusterFeature extends Feature<CaveFloorClusterConfiguration> {

    private static final int RANGE = 4;

    public CaveFloorClusterFeature() {
        super(CaveFloorClusterConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<CaveFloorClusterConfiguration> context) {
        CaveFloorClusterConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        WorldGenerationContext heights = new WorldGenerationContext(context.chunkGenerator(), level);
        ChunkPos chunk = new ChunkPos(context.origin());
        BlockPos center = context.origin().atY(config.height().sample(random, heights));
        for (int attempt = 0; attempt < config.attempts(); attempt++) {
            if (attempt > 0) {
                center = new BlockPos(chunk.getMinBlockX() + random.nextInt(16),
                    config.height().sample(random, heights),
                    chunk.getMinBlockZ() + random.nextInt(16));
            }
            if (this.placeCluster(level, random, config, center)) {
                return true;
            }
        }
        return false;
    }

    private boolean placeCluster(WorldGenLevel level, RandomSource random, CaveFloorClusterConfiguration config, BlockPos center) {
        int largeCount = config.large().count().sample(random);
        int largeLeft = largeCount;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        scan:
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    pos.setWithOffset(center, x, y, z);
                    if (this.canPlaceOn(level, pos, config)) {
                        this.placeBlock(level, random, config, pos, config.large().state());
                        largeLeft--;
                        if (largeLeft == 0) {
                            center = pos.immutable();
                            break scan;
                        }
                    }
                }
            }
        }
        if (largeLeft == largeCount) {
            return false;
        }
        this.placeShuffled(level, random, config, center, config.small());
        this.placeShuffled(level, random, config, center, config.rocks());
        return true;
    }

    private void placeShuffled(WorldGenLevel level, RandomSource random, CaveFloorClusterConfiguration config,
                               BlockPos center, CaveFloorClusterConfiguration.Variant variant) {
        int left = variant.count().sample(random);
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RANGE, -RANGE, -RANGE), center.offset(RANGE, RANGE, RANGE))) {
            positions.add(pos.immutable());
        }
        Util.shuffle(positions, random);
        for (BlockPos pos : positions) {
            if (this.canPlaceOn(level, pos, config)) {
                this.placeBlock(level, random, config, pos, variant.state());
                left--;
                if (left == 0) {
                    return;
                }
            }
        }
    }

    private boolean canPlaceOn(WorldGenLevel level, BlockPos pos, CaveFloorClusterConfiguration config) {
        if (!level.isEmptyBlock(pos)) {
            return false;
        }
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        return floor.is(config.floor()) && floor.isFaceSturdy(level, below, Direction.UP);
    }

    private void placeBlock(WorldGenLevel level, RandomSource random, CaveFloorClusterConfiguration config, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        for (OreConfiguration.TargetBlockState target : config.oreBelow()) {
            if (target.target.test(floor, random)) {
                if (random.nextFloat() < config.oreChance()) {
                    level.setBlock(below, target.state, 2);
                }
                return;
            }
        }
    }
}
