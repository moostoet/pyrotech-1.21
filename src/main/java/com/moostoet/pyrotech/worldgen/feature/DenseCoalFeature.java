package com.moostoet.pyrotech.worldgen.feature;

import com.moostoet.pyrotech.core.FloodFill;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * The 1.12 dense coal generator. It walks a four by four grid of columns in its chunk,
 * at offsets 2, 6, 10, and 14, up through the scan band, and at the first target block
 * in each column floods through the connected vein, converting a random number of its
 * blocks. It reads only its origin's chunk, never its y (worldgen sign-off, item 12).
 */
public final class DenseCoalFeature extends Feature<DenseCoalConfiguration> {

    public DenseCoalFeature() {
        super(DenseCoalConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<DenseCoalConfiguration> context) {
        DenseCoalConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ChunkPos chunk = new ChunkPos(context.origin());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = 2; x < 16; x += 4) {
            for (int z = 2; z < 16; z += 4) {
                for (int y = config.minY(); y < config.maxY(); y++) {
                    pos.set(chunk.getMinBlockX() + x, y, chunk.getMinBlockZ() + z);
                    if (config.target().test(level.getBlockState(pos), random)) {
                        FloodFill.apply(level, pos,
                            (l, p) -> config.target().test(l.getBlockState(p), random),
                            (l, p) -> {
                                l.setBlock(p, config.state(), 2);
                                return true;
                            },
                            config.size().sample(random));
                        placed = true;
                        break;
                    }
                }
            }
        }
        return placed;
    }
}
